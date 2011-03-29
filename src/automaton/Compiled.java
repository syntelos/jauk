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

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Set;

/**
 * @author Anders Møller
 * @author John Pritchard
 */
public class Compiled
    extends Object
    implements jauk.Pattern
{

    protected final int size;
    protected final boolean[] accept;
    protected final int initial;
    protected final int[] transitions; // delta(state,c) = transitions[state*points.length + getCharClass(c)]
    protected final char[] points;     // char interval start points
    protected final int[] classmap;    // map from char number to class class


    public Compiled(Automaton a) {
        this(a, true);
    }
    public Compiled(Automaton a, boolean tableize) {
        super();
        a.determinize();
        this.points = a.getStartPoints();
        Set<State> states = a.getStates();
        Automaton.setStateNumbers(states);
        this.initial = a.initial.number;
        this.size = states.size();
        this.accept = new boolean[size];

        this.transitions = new int[this.size * this.points.length];
        for (int n = 0; n < this.transitions.length; n++){
            this.transitions[n] = -1;
        }

        for (State s : states) {
            int n = s.number;
            this.accept[n] = s.accept;
            for (int c = 0; c < this.points.length; c++) {
                State q = s.step(this.points[c]);
                if (q != null)
                    this.transitions[n * this.points.length + c] = q.number;
            }
        }
        if (tableize){
            this.classmap = new int[Character.MAX_VALUE - Character.MIN_VALUE + 1];
            int i = 0;
            for (int j = 0; j <= Character.MAX_VALUE - Character.MIN_VALUE; j++) {
                if (i + 1 < this.points.length && j == this.points[i + 1])
                    i++;
                this.classmap[j] = i;
            }
        }
        else
            this.classmap = null;
    }


    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("initial state: ").append(initial).append("\n");
        for (int i = 0; i < size; i++) {
            b.append("state " + i);
            if (accept[i])
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
    public final int getSize() {
        return this.size;
    }
    public final boolean isAccept(int state) {
        if (-1 == state)
            return false;
        else
            return this.accept[state];
    }
    public final int getInitialState() {
        return this.initial;
    }
    public final char[] getCharIntervals() {
        return this.points.clone();
    }
    protected final int getCharClass(char c) {
        return SpecialOperations.FindIndex(c, this.points);
    }
    public final int step(int state, char c) {
        if (-1 == state)
            return -1;
        else if (this.classmap == null)
            return this.transitions[state * this.points.length + getCharClass(c)];
        else
            return this.transitions[state * this.points.length + this.classmap[c - Character.MIN_VALUE]];
    }
    /**
     * @return Last offset in match (inclusive), or negative one.
     */
    public final int run(CharSequence s, int ofs) {
        final int len = s.length();
        int p = this.initial;
        int end = -1;
        for (; ofs < len; ofs++) {

            p = this.step(p, s.charAt(ofs));
            if (p == -1)
                break;
            else {
                if (this.accept[p])
                    end = ofs;
            }
        }
        return end;
    }
    public Match apply(CharSequence s)  {
        return new Match(s, this);
    }
    public Match apply(CharSequence s, int start)  {
        return new Match(s,this,start);
    }
}
