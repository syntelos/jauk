/*
 * automaton
 * 
 * Copyright (c) 2001-2011 Anders Moeller
 * Copyright (c) 2011 John Pritchard
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package automaton;

import lxl.Set;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;

/**
 * @author Anders Møller
 * @author John Pritchard
 */
public class Compiled
    extends Object
    implements jauk.Pattern
{

    protected final int size;
    protected final State[] accept;
    protected final int initial, terminal, classmaplen;
    protected final int[] transitions; // delta(state,c) = transitions[state * points.length + ClassMap[c]]
    protected final char[] points;     // char interval start points
    protected final int[] classmap;    // map from char number to class class


    public Compiled(Automaton a, boolean index) {
        super();
        a.determinize();
        this.points = a.getStartPoints();
        Set<State> states = a.getStates();
        Automaton.SetStateNumbers(states);
        this.initial = a.initial.number;
        /*
         */
        this.size = states.size();
        this.accept = new State[size];
        {
            this.transitions = new int[this.size * this.points.length];
            Arrays.fill(this.transitions,-1);
        }
        final int pointslen = this.points.length;

        this.terminal = (pointslen-1);

        for (State s : states) {
            int n = s.number;

            this.accept[n] = s;

            final int nofs = (n * pointslen);

            for (int c = 0; c < pointslen; c++) {
                State q = s.step(this.points[c]);
                if (q != null){
                    this.transitions[nofs + c] = q.number;
                }
            }
        }

        if (index){
            /*
             * Index points, equivalent to
             *   SpecialOperations.FindIndex(c, this.points);
             * for char c.
             */
            int classmaplen = 0;
            for (int c = 0, ix = 0; c <= Character.MAX_VALUE; c++) {

                if (ix < this.terminal){
                    if (c == this.points[ix + 1])
                        ix++;
                }
                else {
                    classmaplen = c;
                    break;
                }
            }
            int[] classmap = new int[classmaplen];
            for (int c = 0, ix = 0; c < classmaplen; c++) {

                if (ix < this.terminal){
                    if (c == this.points[ix + 1])
                        ix++;

                    classmap[c] = ix;
                }
            }
            this.classmap = classmap;
            this.classmaplen = classmaplen;
        }
        else {
            this.classmap = null;
            this.classmaplen = 0;
        }
    }


    public final int step(int state, char c) {

        final int row = (state * this.points.length);

        int t;

        if (this.classmap == null)
            t = this.transitions[row + SpecialOperations.FindIndex(c, this.points)];

        else if (c < this.classmaplen)
            t = this.transitions[row + this.classmap[c]];
        else
            t = this.transitions[row + this.terminal];

        if (Automaton.Trace){
            if (-1 < t){
                System.err.printf("step <%c %s> -> <%s>%n",c,this.accept[state].name(),this.accept[t].name());
            }
            else
                System.err.printf("step <%c %s> -> <>%n",c,this.accept[state].name());
        }
        return t;
    }
    public final int[] run(Op op, CharSequence s, int ofs) {
        switch(op){
        case Match:
            return new int[]{
                ofs,
                this.runMatch(s,ofs)
            };
        case Search:
            return this.runSearch(s,ofs);
        default:
            throw new Error(op.name());
        }
    }
    /**
     * @return Last offset in match (inclusive), or negative one.
     */
    public final int runMatch(CharSequence s, int ofs) {
        final int len = s.length();
        int p = this.initial;
        int end = -1;
        for (; ofs < len; ofs++) {

            p = this.step(p, s.charAt(ofs));
            if (p == -1){
                if (Automaton.Trace){
                    System.err.printf("match <%c> => <%d>%n",s.charAt(ofs),end);
                }
                return end;
            }
            else if (this.accept[p].accept){

                end = ofs;
            }
            else if (-1 != end){
                if (Automaton.Trace){
                    System.err.printf("match <%c %s> => <%d>%n",s.charAt(ofs),this.accept[p].name(),end);
                }
                return end;
            }
        }
        return end;
    }
    /**
     * @return First and last offsets (inclusive) in match, or null
     */
    public final int[] runSearch(CharSequence s, int ofs) {
        final int len = s.length();
        int p = this.initial;
        int min = -1, max = -1;
        for (; ofs <= len; ofs++) {

            if (this.accept[p].accept){
                if (-1 == min){
                    min = ofs;
                }
                max = ofs;
            }
            else if (ofs == len)
                break;

            p = this.step(p, s.charAt(ofs));
            if (p == -1){
                break;
            }
        }
        /*
         */
        if (-1 != min && -1 != max){

            return new int[]{min,max};
        }
        else
            return null;
    }
    public boolean matches(CharSequence string){

	return this.match(string).terminal();
    }
    public Match match(CharSequence s)  {

        return new Match(Op.Match, s, this);
    }
    public Match match(CharSequence s, int start)  {

        return new Match(Op.Match, s,this,start);
    }
    public Match match(CharSequence s, int start, int lno)  {

        return new Match(Op.Match, s,this,start,lno);
    }
    public Match search(CharSequence s)  {

        return new Match(Op.Search, s, this);
    }
    public Match search(CharSequence s, int start)  {

        return new Match(Op.Search, s,this,start);
    }
    public Match search(CharSequence s, int start, int lno)  {

        return new Match(Op.Search, s,this,start,lno);
    }
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("initial state: ").append(initial).append("\n");
        for (int i = 0; i < size; i++) {
            b.append("state " + accept[i].name());
            if (accept[i].accept)
                b.append(" [accept]:\n");
            else
                b.append(" [reject]:\n");
            for (int j = 0; j < points.length; j++) {
                int k = transitions[i * points.length + j];
                if (k != -1) {
                    char min = points[j];
                    char max;
                    if (j + 1 < points.length)
                        max = (char)(points[j + 1] - 1);
                    else
                        max = Character.MAX_VALUE;
                    b.append(" ");
                    Transition.appendCharString(min, b);
                    if (min != max) {
                        b.append("-");
                        Transition.appendCharString(max, b);
                    }
                    b.append(" -> ").append(k).append("\n");
                }
            }
        }
        return b.toString();
    }
}
