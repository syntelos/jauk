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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        
    protected boolean deterministic;
        
    protected transient Object info;
        
    protected int hash_code;
        
    protected String singleton;
        

    public Automaton() {
        super();
        this.initial = new State();
        this.deterministic = true;
        this.singleton = null;
    }
        

        
    public void setMinimization(int algorithm) {
        this.minimization = algorithm;
    }
    public void checkMinimizeAlways() {
        //minimize();
    }
    public boolean isSingleton() {
        return singleton!=null;
    }
    public String getSingleton() {
        return singleton;
    }
    public void setInitialState(State s) {
        initial = s;
        singleton = null;
    }
    public State getInitialState() {
        expandSingleton();
        return initial;
    }
    public boolean isDeterministic() {
        return deterministic;
    }
    public void setDeterministic(boolean deterministic) {
        this.deterministic = deterministic;
    }
    public void setInfo(Object info) {
        this.info = info;
    }
    public Object getInfo()     {
        return info;
    }
    public Set<State> getStates() {
        expandSingleton();
        Set<State> visited = new LinkedHashSet<State>();
        LinkedList<State> worklist = new LinkedList<State>();
        worklist.add(initial);
        visited.add(initial);
        while (worklist.size() > 0) {
            State s = worklist.removeFirst();
            Collection<Transition> tr = s.transitions;
            for (Transition t : tr)
                if (!visited.contains(t.to)) {
                    visited.add(t.to);
                    worklist.add(t.to);
                }
        }
        return visited;
    }
    public Set<State> getAcceptStates() {
        expandSingleton();
        LinkedHashSet<State> accepts = new LinkedHashSet<State>();
        LinkedHashSet<State> visited = new LinkedHashSet<State>();
        LinkedList<State> worklist = new LinkedList<State>();
        worklist.add(initial);
        visited.add(initial);
        while (worklist.size() > 0) {
            State s = worklist.removeFirst();
            if (s.accept)
                accepts.add(s);
            for (Transition t : s.transitions)
                if (!visited.contains(t.to)) {
                    visited.add(t.to);
                    worklist.add(t.to);
                }
        }
        return accepts;
    }
    protected void totalize() {
        State s = new State();
        s.transitions.add(new Transition(Character.MIN_VALUE, Character.MAX_VALUE, s));
        for (State p : getStates()) {
            int maxi = Character.MIN_VALUE;
            for (Transition t : p.getSortedTransitions(false)) {
                if (t.min > maxi)
                    p.transitions.add(new Transition((char)maxi, (char)(t.min - 1), s));
                if (t.max + 1 > maxi)
                    maxi = t.max + 1;
            }
            if (maxi <= Character.MAX_VALUE)
                p.transitions.add(new Transition((char)maxi, Character.MAX_VALUE, s));
        }
    }
    public void restoreInvariant() {
        removeDeadTransitions();
    }
    public void reduce() {
        if (isSingleton())
            return;
        Set<State> states = getStates();
        setStateNumbers(states);
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
                            s.transitions.add(new Transition((char)min, (char)max, p));
                        min = t.min;
                        max = t.max;
                    }
                } else {
                    if (p != null)
                        s.transitions.add(new Transition((char)min, (char)max, p));
                    p = t.to;
                    min = t.min;
                    max = t.max;
                }
            }
            if (p != null)
                s.transitions.add(new Transition((char)min, (char)max, p));
        }
        clearHashCode();
    }
    public char[] getStartPoints() {
        Set<Character> pointset = new LinkedHashSet<Character>();
        for (State s : getStates()) {
            pointset.add(Character.MIN_VALUE);
            for (Transition t : s.transitions) {
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
        LinkedHashMap<State, Set<State>> map = new LinkedHashMap<State, Set<State>>();
        for (State s : states)
            map.put(s, new LinkedHashSet<State>());
        for (State s : states)
            for (Transition t : s.transitions)
                map.get(t.to).add(s);
        Set<State> live = new LinkedHashSet<State>(getAcceptStates());
        LinkedList<State> worklist = new LinkedList<State>(live);
        while (worklist.size() > 0) {
            State s = worklist.removeFirst();
            for (State p : map.get(s))
                if (!live.contains(p)) {
                    live.add(p);
                    worklist.add(p);
                }
        }
        return live;
    }
    public void removeDeadTransitions() {
        clearHashCode();
        if (isSingleton())
            return;
        Set<State> states = getStates();
        Set<State> live = getLiveStates(states);
        for (State s : states) {
            Set<Transition> st = s.transitions;
            s.resetTransitions();
            for (Transition t : st)
                if (live.contains(t.to))
                    s.transitions.add(t);
        }
        reduce();
    }
    public Automaton expandSingleton() {
        if (isSingleton()) {
            State p = new State();
            initial = p;
            for (int i = 0; i < singleton.length(); i++) {
                State q = new State();
                p.transitions.add(new Transition(singleton.charAt(i), q));
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
        return getStates().size();
    }
    public int getNumberOfTransitions() {
        if (isSingleton())
            return singleton.length();
        int c = 0;
        for (State s : getStates())
            c += s.transitions.size();
        return c;
    }
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Automaton))
            return false;
        Automaton a = (Automaton)obj;
        if (isSingleton() && a.isSingleton())
            return singleton.equals(a.singleton);
        return hashCode() == a.hashCode() && subsetOf(a) && a.subsetOf(this);
    }
    public int hashCode() {
        if (hash_code == 0)
            minimize();
        return hash_code;
    }
    void recomputeHashCode() {
        hash_code = getNumberOfStates() * 3 + getNumberOfTransitions() * 2;
        if (hash_code == 0)
            hash_code = 1;
    }
    void clearHashCode() {
        hash_code = 0;
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
            setStateNumbers(states);
            b.append("initial state: ").append(initial.number).append("\n");
            for (State s : states)
                b.append(s.toString());
        }
        return b.toString();
    }
    Automaton cloneExpanded() {

        return this.clone().expandSingleton();
    }
    Automaton cloneExpandedIfRequired() {

        return this.clone().expandSingleton();
    }
    public Automaton clone() {
        try {
            Automaton a = (Automaton)super.clone();
            if (!isSingleton()) {
                LinkedHashMap<State, State> m = new LinkedHashMap<State, State>();
                Set<State> states = getStates();
                for (State s : states)
                    m.put(s, new State());
                for (State s : states) {
                    State p = m.get(s);
                    p.accept = s.accept;
                    if (s == initial)
                        a.initial = p;
                    for (Transition t : s.transitions)
                        p.transitions.add(new Transition(t.min, t.max, m.get(t.to)));
                }
            }
            return a;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
    Automaton cloneIfRequired() {

        return clone();
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
    public void determinize() {
        BasicOperations.Determinize(this);
    }
    public void addEpsilons(Collection<StatePair> pairs) {
        BasicOperations.AddEpsilons(this, pairs);
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
    public void prefixClose() {
        SpecialOperations.PrefixClose(this);
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
        
    protected static void setStateNumbers(Set<State> states) {
        int number = 0;
        for (State s : states)
            s.number = number++;
    }
    protected static Transition[][] getSortedTransitions(Set<State> states) {
        setStateNumbers(states);
        Transition[][] transitions = new Transition[states.size()][];
        for (State s : states)
            transitions[s.number] = s.getSortedTransitionArray(false);
        return transitions;
    }
}
