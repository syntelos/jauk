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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Anders Møller
 */
public final class SpecialOperations {


    public static Set<State> Reverse(Automaton a) {
        // reverse all edges
        LinkedHashMap<State, LinkedHashSet<Transition>> m = new LinkedHashMap<State, LinkedHashSet<Transition>>();
        Set<State> states = a.getStates();
        Set<State> accept = a.getAcceptStates();
        for (State r : states) {
            m.put(r, new LinkedHashSet<Transition>());
            r.accept = false;
        }
        for (State r : states)
            for (Transition t : r.getTransitions())
                m.get(t.to).add(new Transition(t.min, t.max, r));
        for (State r : states)
            r.transitions = m.get(r);
        // make new initial+final states
        a.initial.accept = true;
        a.initial = new State();
        for (State r : accept)
            a.initial.addEpsilon(r); // ensures that all initial states are reachable
        a.deterministic = false;
        return accept;
    }
    public static Automaton Overlap(Automaton a1, Automaton a2) {
        Automaton b1 = a1.cloneExpanded();
        b1.determinize();
        AcceptToAccept(b1);
        Automaton b2 = a2.cloneExpanded();
        Reverse(b2);
        b2.determinize();
        AcceptToAccept(b2);
        Reverse(b2);
        b2.determinize();
        return b1.intersection(b2).minus(BasicAutomata.MakeEmptyString());
    }
    private static void AcceptToAccept(Automaton a) {
        State s = new State();
        for (State r : a.getAcceptStates())
            s.addEpsilon(r);
        a.initial = s;
        a.deterministic = false;
    }
    public static Automaton SingleChars(Automaton a) {
        Automaton b = new Automaton();
        State s = new State();
        b.initial = s;
        State q = new State();
        q.accept = true;
        if (a.isSingleton()) 
            for (int i = 0; i < a.singleton.length(); i++)
                s.transitions.add(new Transition(a.singleton.charAt(i), q));
        else
            for (State p : a.getStates())
                for (Transition t : p.transitions)
                    s.transitions.add(new Transition(t.min, t.max, q));
        b.deterministic = true;
        b.removeDeadTransitions();
        return b;
    }
    public static Automaton Trim(Automaton a, String set, char c) {
        a = a.cloneExpandedIfRequired();
        State f = new State();
        AddSetTransitions(f, set, f);
        f.accept = true;
        for (State s : a.getStates()) {
            State r = s.step(c);
            if (r != null) {
                // add inner
                State q = new State();
                AddSetTransitions(q, set, q);
                AddSetTransitions(s, set, q);
                q.addEpsilon(r);
            }
            // add postfix
            if (s.accept)
                s.addEpsilon(f);
        }
        // add prefix
        State p = new State();
        AddSetTransitions(p, set, p);
        p.addEpsilon(a.initial);
        a.initial = p;
        a.deterministic = false;
        a.removeDeadTransitions();
        a.checkMinimizeAlways();
        return a;
    }
    private static void AddSetTransitions(State s, String set, State p) {
        for (int n = 0; n < set.length(); n++)
            s.transitions.add(new Transition(set.charAt(n), p));
    }
    public static Automaton Compress(Automaton a, String set, char c) {
        a = a.cloneExpandedIfRequired();
        for (State s : a.getStates()) {
            State r = s.step(c);
            if (r != null) {
                // add inner
                State q = new State();
                AddSetTransitions(q, set, q);
                AddSetTransitions(s, set, q);
                q.addEpsilon(r);
            }
        }
        // add prefix
        a.deterministic = false;
        a.removeDeadTransitions();
        a.checkMinimizeAlways();
        return a;
    }
    public static Automaton Subst(Automaton a, Map<Character, Set<Character>> map) {
        if (map.isEmpty())
            return a.cloneIfRequired();
        Set<Character> ckeys = new TreeSet<Character>(map.keySet());
        char[] keys = new char[ckeys.size()];
        int j = 0;
        for (Character c : ckeys)
            keys[j++] = c;
        a = a.cloneExpandedIfRequired();
        for (State s : a.getStates()) {
            Set<Transition> st = s.transitions;
            s.resetTransitions();
            for (Transition t : st) {
                int index = FindIndex(t.min, keys);
                while (t.min <= t.max) {
                    if (keys[index] > t.min) {
                        char m = (char)(keys[index] - 1);
                        if (t.max < m)
                            m = t.max;
                        s.transitions.add(new Transition(t.min, m, t.to));
                        if (m + 1 > Character.MAX_VALUE)
                            break;
                        t.min = (char)(m + 1);
                    }
                    else if (keys[index] < t.min) {
                        char m;
                        if (index + 1 < keys.length)
                            m = (char)(keys[++index] - 1);
                        else
                            m = Character.MAX_VALUE;
                        if (t.max < m)
                            m = t.max;
                        s.transitions.add(new Transition(t.min, m, t.to));
                        if (m + 1 > Character.MAX_VALUE)
                            break;
                        t.min = (char)(m + 1);
                    }
                    else { // found t.min in substitution map
                        for (Character c : map.get(t.min))
                            s.transitions.add(new Transition(c, t.to));
                        if (t.min + 1 > Character.MAX_VALUE)
                            break;
                        t.min++;
                        if (index + 1 < keys.length && keys[index + 1] == t.min)
                            index++;
                    }
                }
            }
        }
        a.deterministic = false;
        a.removeDeadTransitions();
        a.checkMinimizeAlways();
        return a;
    }
    static int FindIndex(char c, char[] points) {
        int a = 0;
        int b = points.length;
        while (b - a > 1) {
            int d = (a + b) >>> 1;
            if (points[d] > c)
                b = d;
            else if (points[d] < c)
                a = d;
            else
                return d;
        }
        return a;
    }
    public static Automaton Subst(Automaton a, char c, String s) {
        a = a.cloneExpandedIfRequired();
        Set<StatePair> epsilons = new LinkedHashSet<StatePair>();
        for (State p : a.getStates()) {
            Set<Transition> st = p.transitions;
            p.resetTransitions();
            for (Transition t : st)
                if (t.max < c || t.min > c)
                    p.transitions.add(t);
                else {
                    if (t.min < c)
                        p.transitions.add(new Transition(t.min, (char)(c - 1), t.to));
                    if (t.max > c)
                        p.transitions.add(new Transition((char)(c + 1), t.max, t.to));
                    if (s.length() == 0)
                        epsilons.add(new StatePair(p, t.to));
                    else {
                        State q = p;
                        for (int i = 0; i < s.length(); i++) {
                            State r;
                            if (i + 1 == s.length())
                                r = t.to;
                            else
                                r = new State();
                            q.transitions.add(new Transition(s.charAt(i), r));
                            q = r;
                        }
                    }
                }
        }
        a.addEpsilons(epsilons);
        a.deterministic = false;
        a.removeDeadTransitions();
        a.checkMinimizeAlways();
        return a;
    }
    public static Automaton Homomorph(Automaton a, char[] source, char[] dest) {
        a = a.cloneExpandedIfRequired();
        for (State s : a.getStates()) {
            Set<Transition> st = s.transitions;
            s.resetTransitions();
            for (Transition t : st) {
                int min = t.min;
                while (min <= t.max) {
                    int n = FindIndex((char)min, source);
                    char nmin = (char)(dest[n] + min - source[n]);
                    int end = (n + 1 == source.length) ? Character.MAX_VALUE : source[n + 1] - 1;
                    int length;
                    if (end < t.max)
                        length = end + 1 - min;
                    else
                        length = t.max + 1 - min;
                    s.transitions.add(new Transition(nmin, (char)(nmin + length - 1), t.to));
                    min += length;
                }
            }
        }
        a.deterministic = false;
        a.removeDeadTransitions();
        a.checkMinimizeAlways();
        return a;
    }
    public static Automaton ProjectChars(Automaton a, Set<Character> chars) {
        Character[] c = chars.toArray(new Character[chars.size()]);
        char[] cc = new char[c.length];
        boolean normalchars = false;
        for (int i = 0; i < c.length; i++)
            if (c[i] == null)
                normalchars = true;
            else
                cc[i] = c[i];
        Arrays.sort(cc);
        if (a.isSingleton()) {
            for (int i = 0; i < a.singleton.length(); i++) {
                char sc = a.singleton.charAt(i);
                if (!(normalchars && (sc <= '\udfff' || sc >= '\uf900') || Arrays.binarySearch(cc, sc) >= 0))
                    return BasicAutomata.MakeEmpty();
            }
            return a.cloneIfRequired();
        } else {
            LinkedHashSet<StatePair> epsilons = new LinkedHashSet<StatePair>();
            a = a.cloneExpandedIfRequired();
            for (State s : a.getStates()) {
                LinkedHashSet<Transition> new_transitions = new LinkedHashSet<Transition>();
                for (Transition t : s.transitions) {
                    boolean addepsilon = false;
                    if (t.min < '\uf900' && t.max > '\udfff') {
                        int w1 = Arrays.binarySearch(cc, t.min > '\ue000' ? t.min : '\ue000');
                        if (w1 < 0) {
                            w1 = -w1 - 1;
                            addepsilon = true;
                        }
                        int w2 = Arrays.binarySearch(cc, t.max < '\uf8ff' ? t.max : '\uf8ff');
                        if (w2 < 0) {
                            w2 = -w2 - 2;
                            addepsilon = true;
                        }
                        for (int w = w1; w <= w2; w++) {
                            new_transitions.add(new Transition(cc[w], t.to));
                            if (w > w1 && cc[w - 1] + 1 != cc[w])
                                addepsilon = true;
                        }
                    }
                    if (normalchars) {
                        if (t.min <= '\udfff')
                            new_transitions.add(new Transition(t.min, t.max < '\udfff' ? t.max : '\udfff', t.to));
                        if (t.max >= '\uf900')
                            new_transitions.add(new Transition(t.min > '\uf900' ? t.min : '\uf900', t.max, t.to));
                    } else if (t.min <= '\udfff' || t.max >= '\uf900')
                        addepsilon = true;
                    if (addepsilon)
                        epsilons.add(new StatePair(s, t.to));
                }
                s.transitions = new_transitions;
            }
            a.reduce();
            a.addEpsilons(epsilons);
            a.removeDeadTransitions();
            a.checkMinimizeAlways();
            return a;
        }
    }
    public static boolean IsFinite(Automaton a) {
        if (a.isSingleton())
            return true;
        return IsFinite(a.initial, new LinkedHashSet<State>(), new LinkedHashSet<State>());
    }
    private static boolean IsFinite(State s, LinkedHashSet<State> path, LinkedHashSet<State> visited) {
        path.add(s);
        for (Transition t : s.transitions)
            if (path.contains(t.to) || (!visited.contains(t.to) && !IsFinite(t.to, path, visited)))
                return false;
        path.remove(s);
        visited.add(s);
        return true;
    }
    public static Set<String> GetStrings(Automaton a, int length) {
        LinkedHashSet<String> strings = new LinkedHashSet<String>();
        if (a.isSingleton() && a.singleton.length() == length)
            strings.add(a.singleton);
        else if (length >= 0)
            GetStrings(a.initial, strings, new StringBuilder(), length);
        return strings;
    }
    private static void GetStrings(State s, Set<String> strings, StringBuilder path, int length) {
        if (length == 0) {
            if (s.accept)
                strings.add(path.toString());
        } else 
            for (Transition t : s.transitions)
                for (int n = t.min; n <= t.max; n++) {
                    path.append((char)n);
                    GetStrings(t.to, strings, path, length - 1);
                    path.deleteCharAt(path.length() - 1);
                }
    }
    public static Set<String> GetFiniteStrings(Automaton a) {
        LinkedHashSet<String> strings = new LinkedHashSet<String>();
        if (a.isSingleton())
            strings.add(a.singleton);
        else if (!GetFiniteStrings(a.initial, new LinkedHashSet<State>(), strings, new StringBuilder(), -1))
            return null;
        return strings;
    }
    public static Set<String> GetFiniteStrings(Automaton a, int limit) {
        LinkedHashSet<String> strings = new LinkedHashSet<String>();
        if (a.isSingleton()) {
            if (limit > 0)
                strings.add(a.singleton);
            else
                return null;
        } else if (!GetFiniteStrings(a.initial, new LinkedHashSet<State>(), strings, new StringBuilder(), limit))
            return null;
        return strings;
    }
    private static boolean GetFiniteStrings(State s, LinkedHashSet<State> pathstates, LinkedHashSet<String> strings, StringBuilder path, int limit) {
        pathstates.add(s);
        for (Transition t : s.transitions) {
            if (pathstates.contains(t.to))
                return false;
            for (int n = t.min; n <= t.max; n++) {
                path.append((char)n);
                if (t.to.accept) {
                    strings.add(path.toString());
                    if (limit >= 0 && strings.size() > limit)
                        return false;
                }
                if (!GetFiniteStrings(t.to, pathstates, strings, path, limit))
                    return false;
                path.deleteCharAt(path.length() - 1);
            }
        }
        pathstates.remove(s);
        return true;
    }
    public static String GetCommonPrefix(Automaton a) {
        if (a.isSingleton())
            return a.singleton;
        StringBuilder b = new StringBuilder();
        LinkedHashSet<State> visited = new LinkedHashSet<State>();
        State s = a.initial;
        boolean done;
        do {
            done = true;
            visited.add(s);
            if (!s.accept && s.transitions.size() == 1) {
                Transition t = s.transitions.iterator().next();
                if (t.min == t.max && !visited.contains(t.to)) {
                    b.append(t.min);
                    s = t.to;
                    done = false;
                }
            }
        } while (!done);
        return b.toString();
    }
    public static void PrefixClose(Automaton a) {
        for (State s : a.getStates())
            s.setAccept(true);
        a.clearHashCode();
        a.checkMinimizeAlways();
    }
    public static Automaton HexCases(Automaton a) {
        Map<Character,Set<Character>> map = new LinkedHashMap<Character,Set<Character>>();
        for (char c1 = 'a', c2 = 'A'; c1 <= 'f'; c1++, c2++) {
            Set<Character> ws = new LinkedHashSet<Character>();
            ws.add(c1);
            ws.add(c2);
            map.put(c1, ws);
            map.put(c2, ws);
        }
        Automaton ws = BasicAutomata.Whitespace();
        return ws.concatenate(a.subst(map)).concatenate(ws);            
    }
    public static Automaton ReplaceWhitespace(Automaton a) {
        Map<Character,Set<Character>> map = new LinkedHashMap<Character,Set<Character>>();
        Set<Character> ws = new LinkedHashSet<Character>();
        ws.add(' ');
        ws.add('\t');
        ws.add('\n');
        ws.add('\r');
        map.put(' ', ws);
        return a.subst(map);
    }
}
