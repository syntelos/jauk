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

import java.util.ArrayList;
import java.util.BitSet;
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
public final class BasicOperations {

    public static Automaton Concatenate(Automaton a1, Automaton a2) {
        if (a1.isSingleton() && a2.isSingleton())
            return BasicAutomata.MakeString(a1.singleton + a2.singleton);
        else if (IsEmpty(a1) || IsEmpty(a2))
            return BasicAutomata.MakeEmpty();
        else {
            boolean deterministic = a1.isSingleton() && a2.isDeterministic();
            /*
             * Deep clone
             */
            a1 = a1.cloneExpanded();
            a2 = a2.cloneExpanded();
            /*
             * Merge transitions
             */
            for (State s : a1.getAcceptStates()) {
                s.accept = false;
                s.addEpsilon(a2.initial);
            }
            a1.deterministic = deterministic;
            a1.clearHashCode();
            a1.checkMinimizeAlways();
            return a1;
        }
    }
    public static Automaton Concatenate(List<Automaton> l) {
        if (l.isEmpty())
            return BasicAutomata.MakeEmptyString();
        else {
            boolean all_singleton = true;
            for (Automaton a : l){
                if (!a.isSingleton()) {
                    all_singleton = false;
                    break;
                }
            }
            if (all_singleton) {
                StringBuilder b = new StringBuilder();
                for (Automaton a : l){
                    b.append(a.singleton);
                }
                return BasicAutomata.MakeString(b.toString());
            }
            else {
                for (Automaton a : l){
                    if (a.isEmpty())
                        return BasicAutomata.MakeEmpty();
                }
                /*
                 */
                Automaton b = l.get(0).cloneExpanded();

                Set<State> ac = b.getAcceptStates();
                boolean first = true;
                for (Automaton a : l){
                    if (first)
                        first = false;
                    else if (!a.isEmptyString()){

                        Automaton aa = a.cloneExpanded();

                        Set<State> ns = aa.getAcceptStates();
                        for (State s : ac) {
                            s.accept = false;
                            s.addEpsilon(aa.initial);
                            if (s.accept)
                                ns.add(s);
                        }
                        ac = ns;
                    }
                }
                b.deterministic = false;
                b.clearHashCode();
                b.checkMinimizeAlways();
                return b;
            }
        }
    }
    public static Automaton Optional(Automaton a) {
        a = a.cloneExpandedIfRequired();
        State s = new State();
        s.addEpsilon(a.initial);
        s.accept = true;
        a.initial = s;
        a.deterministic = false;
        a.clearHashCode();
        a.checkMinimizeAlways();
        return a;
    }
    public static Automaton Repeat(Automaton a) {
        a = a.cloneExpanded();
        State s = new State();
        s.accept = true;
        s.addEpsilon(a.initial);
        for (State p : a.getAcceptStates())
            p.addEpsilon(s);
        a.initial = s;
        a.deterministic = false;
        a.clearHashCode();
        a.checkMinimizeAlways();
        return a;
    }
    public static Automaton Repeat(Automaton a, int min) {
        if (min == 0)
            return Repeat(a);
        else {
            List<Automaton> as = new ArrayList<Automaton>();
            while (min-- > 0){
                as.add(a);
            }
            as.add(Repeat(a));
            return Concatenate(as);
        }
    }
    public static Automaton Repeat(Automaton a, int min, int max) {
        if (min > max)
            return BasicAutomata.MakeEmpty();
        else {
            max -= min;
            a.expandSingleton();
            Automaton b;
            if (min == 0)
                b = BasicAutomata.MakeEmptyString();
            else if (min == 1)
                b = a.clone();
            else {
                List<Automaton> as = new ArrayList<Automaton>();
                while (min-- > 0){
                    as.add(a);
                }
                b = Concatenate(as);
            }
            if (max > 0) {
                Automaton d = a.clone();
                while (--max > 0) {
                    Automaton c = a.clone();
                    for (State p : c.getAcceptStates()){
                        p.addEpsilon(d.initial);
                    }
                    d = c;
                }
                for (State p : b.getAcceptStates()){
                    p.addEpsilon(d.initial);
                }
                b.deterministic = false;
                b.clearHashCode();
                b.checkMinimizeAlways();
            }
            return b;
        }
    }
    public static Automaton Complement(Automaton a) {
        a = a.cloneExpandedIfRequired();
        a.determinize();
        a.totalize();
        for (State p : a.getStates()){
            p.accept = !p.accept;
        }
        a.removeDeadTransitions();
        return a;
    }
    public static Automaton Minus(Automaton a1, Automaton a2) {
        if (a1.isEmpty() || a1 == a2)
            return BasicAutomata.MakeEmpty();
        else if (a2.isEmpty())
            return a1.cloneIfRequired();
        else if (a1.isSingleton()) {
            if (a2.run(a1.singleton))
                return BasicAutomata.MakeEmpty();
            else
                return a1.cloneIfRequired();
        }
        else
            return Intersection(a1, a2.complement());
    }
    public static Automaton Intersection(Automaton a1, Automaton a2) {
        if (a1.isSingleton()) {
            if (a2.run(a1.singleton))
                return a1.cloneIfRequired();
            else
                return BasicAutomata.MakeEmpty();
        }
        else if (a2.isSingleton()) {
            if (a1.run(a2.singleton))
                return a2.cloneIfRequired();
            else
                return BasicAutomata.MakeEmpty();
        }
        else if (a1 == a2)
            return a1.cloneIfRequired();
        else {
            Transition[][] transitions1 = Automaton.GetSortedTransitions(a1.getStates());
            Transition[][] transitions2 = Automaton.GetSortedTransitions(a2.getStates());
            Automaton c = new Automaton();
            LinkedList<StatePair> worklist = new LinkedList<StatePair>();
            LinkedHashMap<StatePair, StatePair> newstates = new LinkedHashMap<StatePair, StatePair>();
            StatePair p = new StatePair(c.initial, a1.initial, a2.initial);
            worklist.add(p);
            newstates.put(p, p);
            while (worklist.size() > 0) {
                p = worklist.removeFirst();
                p.s.accept = p.s1.accept && p.s2.accept;
                Transition[] t1 = transitions1[p.s1.number];
                Transition[] t2 = transitions2[p.s2.number];
                for (int n1 = 0, b2 = 0; n1 < t1.length; n1++) {
                    while (b2 < t2.length && t2[b2].max < t1[n1].min){
                        b2++;
                    }
                    for (int n2 = b2; n2 < t2.length && t1[n1].max >= t2[n2].min; n2++){
                        if (t2[n2].max >= t1[n1].min) {
                            StatePair q = new StatePair(t1[n1].to, t2[n2].to);
                            StatePair r = newstates.get(q);
                            if (r == null) {
                                q.s = new State();
                                worklist.add(q);
                                newstates.put(q, q);
                                r = q;
                            }
                            char min = t1[n1].min > t2[n2].min ? t1[n1].min : t2[n2].min;
                            char max = t1[n1].max < t2[n2].max ? t1[n1].max : t2[n2].max;
                            p.s.transitions.add(new Transition(min, max, r.s));
                        }
                    }
                }
            }
            c.deterministic = a1.deterministic && a2.deterministic;
            c.removeDeadTransitions();
            c.checkMinimizeAlways();
            return c;
        }
    }
    public static boolean SubsetOf(Automaton a1, Automaton a2) {
        if (a1 == a2)
            return true;
        else if (a1.isSingleton()) {
            if (a2.isSingleton())
                return a1.singleton.equals(a2.singleton);
            else
                return a2.run(a1.singleton);
        }
        else {
            a2.determinize();
            Transition[][] transitions1 = Automaton.GetSortedTransitions(a1.getStates());
            Transition[][] transitions2 = Automaton.GetSortedTransitions(a2.getStates());
            LinkedList<StatePair> worklist = new LinkedList<StatePair>();
            LinkedHashSet<StatePair> visited = new LinkedHashSet<StatePair>();
            StatePair p = new StatePair(a1.initial, a2.initial);
            worklist.add(p);
            visited.add(p);
            while (worklist.size() > 0) {
                p = worklist.removeFirst();
                if (p.s1.accept && !p.s2.accept)
                    return false;
                else {
                    Transition[] t1 = transitions1[p.s1.number];
                    Transition[] t2 = transitions2[p.s2.number];
                    for (int n1 = 0, b2 = 0; n1 < t1.length; n1++) {
                        while (b2 < t2.length && t2[b2].max < t1[n1].min){
                            b2++;
                        }
                        int min1 = t1[n1].min, max1 = t1[n1].max;
                        for (int n2 = b2; n2 < t2.length && t1[n1].max >= t2[n2].min; n2++) {
                            if (t2[n2].min > min1)
                                return false;
                            if (t2[n2].max < Character.MAX_VALUE) 
                                min1 = t2[n2].max + 1;
                            else {
                                min1 = Character.MAX_VALUE;
                                max1 = Character.MIN_VALUE;
                            }
                            StatePair q = new StatePair(t1[n1].to, t2[n2].to);
                            if (!visited.contains(q)) {
                                worklist.add(q);
                                visited.add(q);
                            }
                        }
                        if (min1 <= max1)
                            return false;
                    }
                }
            }
            return true;
        }
    }
    public static Automaton Union(Automaton a1, Automaton a2) {
        if ((a1.isSingleton() && a2.isSingleton() && a1.singleton.equals(a2.singleton)) || a1 == a2)
            return a1.cloneIfRequired();
        else if (a1 == a2) {
            a1 = a1.cloneExpanded();
            a2 = a2.cloneExpanded();
        }
        else {
            a1 = a1.cloneExpandedIfRequired();
            a2 = a2.cloneExpandedIfRequired();
        }
        State s = new State();
        s.addEpsilon(a1.initial);
        s.addEpsilon(a2.initial);
        a1.initial = s;
        a1.deterministic = false;
        a1.clearHashCode();
        a1.checkMinimizeAlways();
        return a1;
    }
    public static Automaton Union(Collection<Automaton> l) {
        Set<Integer> ids = new LinkedHashSet<Integer>();
        for (Automaton a : l){
            ids.add(System.identityHashCode(a));
        }
        boolean has_aliases = ids.size() != l.size();
        State s = new State();
        for (Automaton b : l) {
            if (!b.isEmpty()){
                Automaton bb = b;
                if (has_aliases)
                    bb = bb.cloneExpanded();
                else
                    bb = bb.cloneExpandedIfRequired();
                s.addEpsilon(bb.initial);
            }
        }
        Automaton a = new Automaton();
        a.initial = s;
        a.deterministic = false;
        a.clearHashCode();
        a.checkMinimizeAlways();
        return a;
    }
    public static void Determinize(Automaton a) {
        if (a.deterministic || a.isSingleton())
            return;
        else {
            Set<State> initialset = new LinkedHashSet<State>();
            initialset.add(a.initial);
            Determinize(a, initialset);
        }
    }
    protected static void Determinize(Automaton a, Set<State> initialset) {
        char[] points = a.getStartPoints();
        /*
         * Subset construction
         */
        Map<Set<State>, Set<State>> sets = new LinkedHashMap<Set<State>, Set<State>>();
        LinkedList<Set<State>> worklist = new LinkedList<Set<State>>();
        Map<Set<State>, State> newstate = new LinkedHashMap<Set<State>, State>();
        sets.put(initialset, initialset);
        worklist.add(initialset);
        a.initial = new State();
        newstate.put(initialset, a.initial);
        while (worklist.size() > 0) {
            Set<State> s = worklist.removeFirst();
            State r = newstate.get(s);
            for (State q : s){
                if (q.accept) {
                    r.accept = true;
                    break;
                }
            }
            for (int n = 0; n < points.length; n++) {
                Set<State> p = new LinkedHashSet<State>();
                for (State q : s){
                    for (Transition t : q.transitions){
                        if (t.min <= points[n] && points[n] <= t.max)
                            p.add(t.to);
                    }
                }
                if (!sets.containsKey(p)) {
                    sets.put(p, p);
                    worklist.add(p);
                    newstate.put(p, new State());
                }
                State q = newstate.get(p);
                char min = points[n];
                char max;
                if (n + 1 < points.length)
                    max = (char)(points[n + 1] - 1);
                else
                    max = Character.MAX_VALUE;
                r.transitions.add(new Transition(min, max, q));
            }
        }
        a.deterministic = true;
        a.removeDeadTransitions();
    }
    public static void AddEpsilons(Automaton a, Collection<StatePair> pairs) {
        a.expandSingleton();
        LinkedHashMap<State, LinkedHashSet<State>> forward = new LinkedHashMap<State, LinkedHashSet<State>>();
        LinkedHashMap<State, LinkedHashSet<State>> back = new LinkedHashMap<State, LinkedHashSet<State>>();
        for (StatePair p : pairs) {
            LinkedHashSet<State> to = forward.get(p.s1);
            if (to == null) {
                to = new LinkedHashSet<State>();
                forward.put(p.s1, to);
            }
            to.add(p.s2);
            LinkedHashSet<State> from = back.get(p.s2);
            if (from == null) {
                from = new LinkedHashSet<State>();
                back.put(p.s2, from);
            }
            from.add(p.s1);
        }
        /*
         * Calculate epsilon closure
         */
        LinkedList<StatePair> worklist = new LinkedList<StatePair>(pairs);
        LinkedHashSet<StatePair> workset = new LinkedHashSet<StatePair>(pairs);
        while (!worklist.isEmpty()) {
            StatePair p = worklist.removeFirst();
            workset.remove(p);
            LinkedHashSet<State> to = forward.get(p.s2);
            LinkedHashSet<State> from = back.get(p.s1);
            if (to != null) {
                for (State s : to) {
                    StatePair pp = new StatePair(p.s1, s);
                    if (!pairs.contains(pp)) {
                        pairs.add(pp);
                        forward.get(p.s1).add(s);
                        back.get(s).add(p.s1);
                        worklist.add(pp);
                        workset.add(pp);
                        if (from != null) {
                            for (State q : from) {
                                StatePair qq = new StatePair(q, p.s1);
                                if (!workset.contains(qq)) {
                                    worklist.add(qq);
                                    workset.add(qq);
                                }
                            }
                        }
                    }
                }
            }
        }
        /*
         * Add transitions
         */
        for (StatePair p : pairs)
            p.s1.addEpsilon(p.s2);
        a.deterministic = false;
        a.clearHashCode();
        a.checkMinimizeAlways();
    }
    public static boolean IsEmptyString(Automaton a) {
        if (a.isSingleton())
            return a.singleton.length() == 0;
        else
            return a.initial.accept && a.initial.transitions.isEmpty();
    }
    public static boolean IsEmpty(Automaton a) {
        if (a.isSingleton())
            return false;
        return !a.initial.accept && a.initial.transitions.isEmpty();
    }
    public static boolean IsTotal(Automaton a) {
        if (a.isSingleton())
            return false;
        if (a.initial.accept && a.initial.transitions.size() == 1) {
            Transition t = a.initial.transitions.iterator().next();
            return t.to == a.initial && t.min == Character.MIN_VALUE && t.max == Character.MAX_VALUE;
        }
        return false;
    }
    public static boolean Run(Automaton a, String s) {
        if (a.isSingleton())
            return s.equals(a.singleton);
        else if (a.deterministic) {
            State p = a.initial;
            for (int i = 0; i < s.length(); i++) {
                State q = p.step(s.charAt(i));
                if (q == null)
                    return false;
                p = q;
            }
            return p.accept;
        }
        else {
            Set<State> states = a.getStates();
            Automaton.SetStateNumbers(states);
            LinkedList<State> pp = new LinkedList<State>();
            LinkedList<State> pp_other = new LinkedList<State>();
            BitSet bb = new BitSet(states.size());
            BitSet bb_other = new BitSet(states.size());
            pp.add(a.initial);
            ArrayList<State> dest = new ArrayList<State>();
            boolean accept = a.initial.accept;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                accept = false;
                pp_other.clear();
                bb_other.clear();
                for (State p : pp) {
                    dest.clear();
                    p.step(c, dest);
                    for (State q : dest) {
                        if (q.accept)
                            accept = true;
                        if (!bb_other.get(q.number)) {
                            bb_other.set(q.number);
                            pp_other.add(q);
                        }
                    }
                }
                LinkedList<State> tp = pp;
                pp = pp_other;
                pp_other = tp;
                BitSet tb = bb;
                bb = bb_other;
                bb_other = tb;
            }
            return accept;
        }
    }
}
