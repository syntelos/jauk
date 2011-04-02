/*
 * automaton
 * 
 * Copyright (c) 2001-2011 Anders Moeller
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

import lxl.ArrayList;
import lxl.Collection;
import lxl.List;
import lxl.Map;
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
 */
public class Automaton 
    extends Object
    implements Cloneable
{

    public static final int MINIMIZE_HUFFMAN = 0;
    public static final int MINIMIZE_BRZOZOWSKI = 1;
    public static final int MINIMIZE_HOPCROFT = 2;



    protected int minimization = MINIMIZE_HOPCROFT;
        
    protected State initial;
        
    protected boolean deterministic = true;
        
    protected transient Object info;
        
    protected int hash_code;
        
    protected String singleton;
        

    public Automaton() {
        super();
        this.initial = new State();
    }
    public Automaton(State initial) {
        super();
        if (null != initial){
            this.initial = initial;
        }
        else
            throw new IllegalArgumentException();
    }
    public Automaton(String singleton) {
        this();
        if (null != singleton){
            this.singleton = singleton;
        }
        else
            throw new IllegalArgumentException();
    }
        

        
    public Automaton setMinimization(int algorithm) {
        this.minimization = algorithm;
        return this;
    }
    public Automaton checkMinimizeAlways() {
        //minimize();
        return this;
    }
    public boolean isSingleton() {
        return singleton!=null;
    }
    public String getSingleton() {
        return singleton;
    }
    public Automaton setInitialState(State s) {
        initial = s;
        singleton = null;
        return this;
    }
    public State getInitialState() {
        expandSingleton();
        return initial;
    }
    public boolean isDeterministic() {
        return deterministic;
    }
    public Automaton setDeterministic(boolean deterministic) {
        this.deterministic = deterministic;
        return this;
    }
    public Automaton setInfo(Object info) {
        this.info = info;
        return this;
    }
    public Object getInfo()     {
        return info;
    }
    public Set<State> getStates() {
        expandSingleton();
        Set<State> visited = new Set<State>();
        List<State> worklist = new ArrayList<State>();
        worklist.add(initial);
        visited.add(initial);
        while (worklist.isNotEmpty()) {
            State s = worklist.removeFirst();

            for (Transition t : s){
                if (!visited.contains(t.to)) {
                    visited.add(t.to);
                    worklist.add(t.to);
                }
            }
        }
        return visited;
    }
    public Set<State> getAcceptStates() {
        expandSingleton();
        Set<State> accepts = new Set<State>();
        Set<State> visited = new Set<State>();
        List<State> worklist = new ArrayList<State>();
        worklist.add(initial);
        visited.add(initial);
        while (worklist.isNotEmpty()) {
            State s = worklist.removeFirst();
            if (s.accept)
                accepts.add(s);

            for (Transition t : s){
                if (!visited.contains(t.to)) {
                    visited.add(t.to);
                    worklist.add(t.to);
                }
            }
        }
        return accepts;
    }
    protected Automaton totalize() {
        State s = new State();
        s.add(new Transition(Character.MIN_VALUE, Character.MAX_VALUE, s));
        for (State p : getStates()) {
            int maxi = Character.MIN_VALUE;
            for (Transition t : p.getSortedTransitions(false)) {
                if (t.min > maxi)
                    p.add(new Transition((char)maxi, (char)(t.min - 1), s));
                if (t.max + 1 > maxi)
                    maxi = t.max + 1;
            }
            if (maxi <= Character.MAX_VALUE)
                p.add(new Transition((char)maxi, Character.MAX_VALUE, s));
        }
        return this;
    }
    public Automaton restoreInvariant() {
        return this.removeDeadTransitions();
    }
    public Automaton reduce() {
        if (!this.isSingleton()){

            Set<State> states = getStates();
            SetStateNumbers(states);
            for (State s : states) {
                List<Transition> st = s.getSortedTransitions(true);
                s.resetTransitions();
                State p = null;
                int min = -1, max = -1;
                for (Transition t : st) {
                    if (p == t.to) {
                        if (t.min <= max + 1) {
                            if (t.max > max)
                                max = t.max;
                        } else {
                            if (p != null)
                                s.add(new Transition((char)min, (char)max, p));
                            min = t.min;
                            max = t.max;
                        }
                    } else {
                        if (p != null)
                            s.add(new Transition((char)min, (char)max, p));
                        p = t.to;
                        min = t.min;
                        max = t.max;
                    }
                }
                if (p != null)
                    s.add(new Transition((char)min, (char)max, p));
            }
            clearHashCode();
        }
        return this;
    }
    public char[] getStartPoints() {
        Set<Character> pointset = new Set<Character>();
        for (State s : getStates()) {
            pointset.add(Character.MIN_VALUE);
            for (Transition t : s) {
                pointset.add(t.min);
                if (t.max < Character.MAX_VALUE)
                    pointset.add((char)(t.max + 1));
            }
        }
        char[] points = new char[pointset.size()];
        int n = 0;
        for (Character m : pointset)
            points[n++] = m;
        Arrays.sort(points);
        return points;
    }
    public Set<State> getLiveStates() {
        expandSingleton();
        return getLiveStates(getStates());
    }
    private Set<State> getLiveStates(Set<State> states) {
        Map<State, Set<State>> map = new Map<State, Set<State>>();
        for (State s : states){
            map.put(s, new Set<State>());
        }
        for (State s : states){
            for (Transition t : s){
                map.get(t.to).add(s);
            }
        }
        Set<State> live = new Set<State>(getAcceptStates());
        List<State> worklist = new ArrayList<State>(live);
        while (worklist.isNotEmpty()) {
            State s = worklist.removeFirst();
            for (State p : map.get(s)){
                if (!live.contains(p)) {
                    live.add(p);
                    worklist.add(p);
                }
            }
        }
        return live;
    }
    public Automaton removeDeadTransitions() {
        clearHashCode();
        if (isSingleton())
            return this;
        else {
            Set<State> states = getStates();
            Set<State> live = getLiveStates(states);
            for (State s : states) {
                Set<Transition> st = s.resetTransitions();
                for (Transition t : st){
                    if (live.contains(t.to))
                        s.add(t);
                }
            }
            return this.reduce();
        }
    }
    public Automaton expandSingleton() {
        if (isSingleton()) {
            State p = new State();
            initial = p;
            for (int i = 0; i < singleton.length(); i++) {
                State q = new State();
                p.add(new Transition(singleton.charAt(i), q));
                p = q;
            }
            p.accept = true;
            deterministic = true;
            singleton = null;
        }
	return this;
    }
    public int getNumberOfStates() {
        if (isSingleton())
            return singleton.length() + 1;
        else
            return getStates().size();
    }
    public int getNumberOfTransitions() {
        if (isSingleton())
            return singleton.length();
        else {
            int c = 0;
            for (State s : getStates()){
                c += s.size();
            }
            return c;
        }
    }
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        else if (!(obj instanceof Automaton))
            return false;
        else {
            Automaton a = (Automaton)obj;
            if (isSingleton() && a.isSingleton())
                return singleton.equals(a.singleton);
            else
                return hashCode() == a.hashCode() && subsetOf(a) && a.subsetOf(this);
        }
    }
    public int hashCode() {
        if (hash_code == 0)
            minimize();
        return hash_code;
    }
    protected Automaton recomputeHashCode() {
        hash_code = getNumberOfStates() * 3 + getNumberOfTransitions() * 2;
        if (hash_code == 0)
            hash_code = 1;

        return this;
    }
    protected Automaton clearHashCode() {
        hash_code = 0;
        return this;
    }
    public String toString() {
        StringBuilder b = new StringBuilder();
        if (isSingleton()) {
            b.append("singleton: ");
            for (char c : singleton.toCharArray())
                Transition.appendCharString(c, b);
            b.append("\n");
        } else {
            Set<State> states = getStates();
            SetStateNumbers(states);
            b.append("initial state: ").append(initial.number).append("\n");
            for (State s : states)
                b.append(s.toString());
        }
        return b.toString();
    }
    protected Automaton cloneExpanded() {

        return this.clone().expandSingleton();
    }
    protected Automaton cloneExpandedIfRequired() {

        return this.clone().expandSingleton();
    }
    public Automaton clone() {
        try {
            Automaton a = (Automaton)super.clone();
            if (!isSingleton()) {
                Map<State, State> m = new Map<State, State>();
                Set<State> states = getStates();
                for (State s : states)
                    m.put(s, new State());
                for (State s : states) {
                    State p = m.get(s);
                    p.accept = s.accept;
                    if (s == this.initial){
                        a.initial = p;
                    }
                    for (Transition t : s){
                        p.add(new Transition(t.min, t.max, m.get(t.to)));
                    }
                }
            }
            return a;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
    protected Automaton cloneIfRequired() {

        return this.clone();
    }
    public Automaton concatenate(Automaton a) {
        return BasicOperations.Concatenate(this, a);
    }
    public Automaton optional() {
        return BasicOperations.Optional(this);
    }
    public Automaton repeat() {
        return BasicOperations.Repeat(this);
    }
    public Automaton repeat(int min) {
        return BasicOperations.Repeat(this, min);
    }
    public Automaton repeat(int min, int max) {
        return BasicOperations.Repeat(this, min, max);
    }
    public Automaton complement() {
        return BasicOperations.Complement(this);
    }
    public Automaton minus(Automaton a) {
        return BasicOperations.Minus(this, a);
    }
    public Automaton intersection(Automaton a) {
        return BasicOperations.Intersection(this, a);
    }
    public boolean subsetOf(Automaton a) {
        return BasicOperations.SubsetOf(this, a);
    }
    public Automaton union(Automaton a) {
        return BasicOperations.Union(this, a);
    }
    public Automaton determinize() {
        BasicOperations.Determinize(this);
        return this;
    }
    public Automaton addEpsilons(Collection<StatePair> pairs) {
        BasicOperations.AddEpsilons(this, pairs);
        return this;
    }
    public boolean isEmptyString() {
        return BasicOperations.IsEmptyString(this);
    }
    public boolean isEmpty() {
        return BasicOperations.IsEmpty(this);
    }
    public boolean isTotal() {
        return BasicOperations.IsTotal(this);
    }
    public boolean run(String s) {
        return BasicOperations.Run(this, s);
    }
    public Automaton minimize() {
        MinimizationOperations.Minimize(this);
        return this;
    }
    public Automaton overlap(Automaton a) {
        return SpecialOperations.Overlap(this, a);
    }
    public Automaton singleChars() {
        return SpecialOperations.SingleChars(this);
    }
    public Automaton trim(String set, char c) {
        return SpecialOperations.Trim(this, set, c);
    }
    public Automaton compress(String set, char c) {
        return SpecialOperations.Compress(this, set, c);
    }
    public Automaton subst(Map<Character,Set<Character>> map) {
        return SpecialOperations.Subst(this, map);
    }
    public Automaton subst(char c, String s) {
        return SpecialOperations.Subst(this, c, s);
    }
    public Automaton homomorph(char[] source, char[] dest) {
        return SpecialOperations.Homomorph(this, source, dest);
    }
    public Automaton projectChars(Set<Character> chars) {
        return SpecialOperations.ProjectChars(this, chars);
    }
    public boolean isFinite() {
        return SpecialOperations.IsFinite(this);
    }
    public Set<String> getStrings(int length) {
        return SpecialOperations.GetStrings(this, length);
    }
    public Set<String> getFiniteStrings() {
        return SpecialOperations.GetFiniteStrings(this);
    }
    public Set<String> getFiniteStrings(int limit) {
        return SpecialOperations.GetFiniteStrings(this, limit);
    }
    public String getCommonPrefix() {
        return SpecialOperations.GetCommonPrefix(this);
    }
    public Automaton prefixClose() {
        SpecialOperations.PrefixClose(this);
        return this;
    }
    public Automaton hexCases() {
        return SpecialOperations.HexCases(this);
    }
    public Automaton replaceWhitespace() {
        return SpecialOperations.ReplaceWhitespace(this);
    }
    public Automaton shuffle(Automaton a) {
        return ShuffleOperations.Shuffle(this, a);
    }
        
    protected static void SetStateNumbers(Set<State> states) {
        int number = 0;
        for (State s : states)
            s.number = number++;
    }
    protected static Transition[][] GetSortedTransitions(Set<State> states) {
        SetStateNumbers(states);
        Transition[][] transitions = new Transition[states.size()][];
        for (State s : states)
            transitions[s.number] = s.getSortedTransitionArray(false);
        return transitions;
    }
}
