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
import lxl.List;
import lxl.Set;

/**
 * @author Anders Møller
 */
public final class MinimizationOperations
{
    public static void Minimize(Automaton a) {
        if (!a.isSingleton()) {
            switch (a.minimization) {
            case Automaton.MINIMIZE_HUFFMAN:
                MinimizeHuffman(a);
                a.recomputeHashCode();
                break;
            case Automaton.MINIMIZE_BRZOZOWSKI:
                MinimizeBrzozowski(a);
                a.recomputeHashCode();
                break;
            case Automaton.MINIMIZE_HOPCROFT:
                MinimizeHopcroft(a);
                a.recomputeHashCode();
                break;
            default:
                break;
            }
        }
    }
    /** 
     * Minimizes the given automaton using Huffman's algorithm. 
     */
    public static void MinimizeHuffman(Automaton a) {
        a.determinize();
        a.totalize();
        Set<State> ss = a.getStates();
        Transition[][] transitions = new Transition[ss.size()][];
        State[] states = ss.toArray(State.class);
        int stateslen = ((null != states)?(states.length):(0));
        boolean[][] mark = new boolean[stateslen][stateslen];
        List<List<Set<IntPair>>> triggers = new ArrayList<List<Set<IntPair>>>();
        for (int n1 = 0; n1 < stateslen; n1++) {
            List<Set<IntPair>> v = new ArrayList<Set<IntPair>>();
            initialize(v, stateslen);
            triggers.add(v);
        }
        // initialize marks based on acceptance status and find transition arrays
        for (int n1 = 0; n1 < stateslen; n1++) {
            states[n1].number = n1;
            transitions[n1] = states[n1].getSortedTransitionArray(false);
            for (int n2 = n1 + 1; n2 < stateslen; n2++)
                if (states[n1].accept != states[n2].accept)
                    mark[n1][n2] = true;
        }
        // for all pairs, see if states agree
        for (int n1 = 0; n1 < stateslen; n1++)
            for (int n2 = n1 + 1; n2 < stateslen; n2++)
                if (!mark[n1][n2]) {
                    if (statesAgree(transitions, mark, n1, n2))
                        addTriggers(transitions, triggers, n1, n2);
                    else
                        markPair(mark, triggers, n1, n2);
                }
        // assign equivalence class numbers to states
        int numclasses = 0;
        for (int n = 0; n < stateslen; n++)
            states[n].number = -1;
        for (int n1 = 0; n1 < stateslen; n1++)
            if (states[n1].number == -1) {
                states[n1].number = numclasses;
                for (int n2 = n1 + 1; n2 < stateslen; n2++)
                    if (!mark[n1][n2])
                        states[n2].number = numclasses;
                numclasses++;
            }
        // make a new state for each equivalence class
        State[] newstates = new State[numclasses];
        for (int n = 0; n < numclasses; n++)
            newstates[n] = new State();
        // select a class representative for each class and find the new initial
        // state
        for (int n = 0; n < stateslen; n++) {
            newstates[states[n].number].number = n;
            if (states[n] == a.initial)
                a.initial = newstates[states[n].number];
        }
        // build transitions and set acceptance
        for (int n = 0; n < numclasses; n++) {
            State s = newstates[n];
            s.accept = states[s.number].accept;
            for (Transition t : states[s.number])
                s.add(new Transition(t.min, t.max, newstates[t.to.number]));
        }
        a.removeDeadTransitions();
    }
    public static void MinimizeBrzozowski(Automaton a) {
        if (a.isSingleton())
            return;
        BasicOperations.Determinize(a, SpecialOperations.Reverse(a));
        BasicOperations.Determinize(a, SpecialOperations.Reverse(a));
    }
    public static void MinimizeHopcroft(Automaton a) {
        a.determinize();
        Set<Transition> tr = a.initial.getTransitions();
        if (tr.size() == 1) {
            Transition t = tr.iterator().next();
            if (t.to == a.initial && t.min == Character.MIN_VALUE && t.max == Character.MAX_VALUE)
                return;
        }
        a.totalize();
	/*
	 * Make arrays for numbered states and effective alphabet
	 */
        Set<State> ss = a.getStates();
        State[] states = new State[ss.size()];
        int number = 0;
        for (State q : ss) {
            states[number] = q;
            q.number = number++;
        }
        char[] sigma = a.getStartPoints();
	/*
	 * Initialize structure
	 */
        List<List<List<State>>> reverse = new ArrayList<List<List<State>>>();
        for (int q = 0; q < states.length; q++) {
            List<List<State>> v = new ArrayList<List<State>>();
            initialize(v, sigma.length);
            reverse.add(v);
        }
        boolean[][] reverse_nonempty = new boolean[states.length][sigma.length];
        List<List<State>> partition = new ArrayList<List<State>>();
        initialize(partition, states.length);
        int[] block = new int[states.length];
        StateList[][] active = new StateList[states.length][sigma.length];
        StateListNode[][] active2 = new StateListNode[states.length][sigma.length];
        List<IntPair> pending = new ArrayList<IntPair>();
        boolean[][] pending2 = new boolean[sigma.length][states.length];
        List<State> split = new ArrayList<State>();
        boolean[] split2 = new boolean[states.length];
        List<Integer> refine = new ArrayList<Integer>();
        boolean[] refine2 = new boolean[states.length];
        List<List<State>> splitblock = new ArrayList<List<State>>();
        initialize(splitblock, states.length);
        for (int q = 0; q < states.length; q++) {
            splitblock.set(q, new ArrayList<State>());
            partition.set(q, new ArrayList<State>());
            for (int x = 0; x < sigma.length; x++) {
                reverse.get(q).set(x, new ArrayList<State>());
                active[q][x] = new StateList();
            }
        }
	/*
	 * Find initial partition and reverse edges
	 */
        for (int q = 0; q < states.length; q++) {
            State qq = states[q];
            int j;
            if (qq.accept)
                j = 0;
            else
                j = 1;
            partition.get(j).add(qq);
            block[qq.number] = j;
            for (int x = 0; x < sigma.length; x++) {
                char y = sigma[x];
                State p = qq.step(y);
                reverse.get(p.number).get(x).add(qq);
                reverse_nonempty[p.number][x] = true;
            }
        }
	/*
	 * Initialize active sets
	 */
        for (int j = 0; j <= 1; j++){
            for (int x = 0; x < sigma.length; x++){
                for (State qq : partition.get(j)){
                    if (reverse_nonempty[qq.number][x])
                        active2[qq.number][x] = active[j][x].add(qq);
		}
	    }
	}
	/*
	 * Initialize pending
	 */
        for (int x = 0; x < sigma.length; x++) {
            int a0 = active[0][x].size;
            int a1 = active[1][x].size;
            int j;
            if (a0 <= a1)
                j = 0;
            else
                j = 1;
            pending.add(new IntPair(j, x));
            pending2[x][j] = true;
        }
	/*
	 * Process pending until fixed point
	 */
        int k = 2;
        while (!pending.isEmpty()) {
            IntPair ip = pending.removeFirst();
            int p = ip.n1;
            int x = ip.n2;
            pending2[x][p] = false;
	    /*
	     * Find states that need to be split off their blocks
	     */
            for (StateListNode m = active[p][x].first; m != null; m = m.next){
                for (State s : reverse.get(m.q.number).get(x)){
                    if (!split2[s.number]) {
                        split2[s.number] = true;
                        split.add(s);
                        int j = block[s.number];
                        splitblock.get(j).add(s);
                        if (!refine2[j]) {
                            refine2[j] = true;
                            refine.add(j);
                        }
                    }
		}
	    }
	    /*
	     * Refine blocks
	     */
            for (int j : refine) {
                if (splitblock.get(j).size() < partition.get(j).size()) {
                    List<State> b1 = partition.get(j);
                    List<State> b2 = partition.get(k);
                    for (State s : splitblock.get(j)) {
                        b1.remove(s);
                        b2.add(s);
                        block[s.number] = k;
                        for (int c = 0; c < sigma.length; c++) {
                            StateListNode sn = active2[s.number][c];
                            if (sn != null && sn.sl == active[j][c]) {
                                sn.remove();
                                active2[s.number][c] = active[k][c].add(s);
                            }
                        }
                    }
		    /*
		     * Update pending
		     */
                    for (int c = 0; c < sigma.length; c++) {
                        int aj = active[j][c].size;
                        int ak = active[k][c].size;
                        if (!pending2[c][j] && 0 < aj && aj <= ak) {
                            pending2[c][j] = true;
                            pending.add(new IntPair(j, c));
                        } else {
                            pending2[c][k] = true;
                            pending.add(new IntPair(k, c));
                        }
                    }
                    k++;
                }
                for (State s : splitblock.get(j)){
                    split2[s.number] = false;
		}
                refine2[j] = false;
                splitblock.get(j).clear();
            }
            split.clear();
            refine.clear();
        }
	/*
	 * Make a new state for each equivalence class, set initial state
	 */
        State[] newstates = new State[k];
        for (int n = 0; n < newstates.length; n++) {
            State s = new State();
            newstates[n] = s;
            for (State q : partition.get(n)) {
                if (q == a.initial)
                    a.initial = s;
                s.accept = q.accept;
                s.number = q.number; // select representative
                q.number = n;
            }
        }
	/*
	 * Build transitions and set acceptance
	 */
        for (int n = 0; n < newstates.length; n++) {
            State s = newstates[n];
            s.accept = states[s.number].accept;
            for (Transition t : states[s.number])
                s.add(new Transition(t.min, t.max, newstates[t.to.number]));
        }
        a.removeDeadTransitions();
    }

    private static boolean statesAgree(Transition[][] transitions, boolean[][] mark, int n1, int n2) {
        Transition[] t1 = transitions[n1];
        Transition[] t2 = transitions[n2];
        for (int k1 = 0, k2 = 0; k1 < t1.length && k2 < t2.length;) {
            if (t1[k1].max < t2[k2].min)
                k1++;
            else if (t2[k2].max < t1[k1].min)
                k2++;
            else {
                int m1 = t1[k1].to.number;
                int m2 = t2[k2].to.number;
                if (m1 > m2) {
                    int t = m1;
                    m1 = m2;
                    m2 = t;
                }
                if (mark[m1][m2])
                    return false;
                if (t1[k1].max < t2[k2].max)
                    k1++;
                else
                    k2++;
            }
        }
        return true;
    }
    private static void addTriggers(Transition[][] transitions, List<List<Set<IntPair>>> triggers, int n1, int n2) {
        Transition[] t1 = transitions[n1];
        Transition[] t2 = transitions[n2];
        for (int k1 = 0, k2 = 0; k1 < t1.length && k2 < t2.length;) {
            if (t1[k1].max < t2[k2].min)
                k1++;
            else if (t2[k2].max < t1[k1].min)
                k2++;
            else {
                if (t1[k1].to != t2[k2].to) {
                    int m1 = t1[k1].to.number;
                    int m2 = t2[k2].to.number;
                    if (m1 > m2) {
                        int t = m1;
                        m1 = m2;
                        m2 = t;
                    }
                    if (triggers.get(m1).get(m2) == null)
                        triggers.get(m1).set(m2, new Set<IntPair>());
                    triggers.get(m1).get(m2).add(new IntPair(n1, n2));
                }
                if (t1[k1].max < t2[k2].max)
                    k1++;
                else
                    k2++;
            }
        }
    }
    private static void markPair(boolean[][] mark, List<List<Set<IntPair>>> triggers, int n1, int n2) {
        mark[n1][n2] = true;
        if (triggers.get(n1).get(n2) != null) {
            for (IntPair p : triggers.get(n1).get(n2)) {
                int m1 = p.n1;
                int m2 = p.n2;
                if (m1 > m2) {
                    int t = m1;
                    m1 = m2;
                    m2 = t;
                }
                if (!mark[m1][m2])
                    markPair(mark, triggers, m1, m2);
            }
        }
    }
    private static <T> void initialize(List<T> list, int size) {

        for (int i = 0; i < size; i++)
            list.add(null);
    }
        
        
    static class IntPair
        extends Object
        implements Comparable<IntPair>
    {

        final int n1, n2, h;

        IntPair(int n1, int n2) {
            super();
            this.n1 = n1;
            this.n2 = n2;
            this.h = (n1+(n2<<16));
        }


        public int hashCode(){
            return this.h;
        }
        public boolean equals(Object tha){
            if (this == tha)
                return true;
            else if (tha instanceof IntPair){
                IntPair that = (IntPair)tha;
                return (this.n1 == that.n1 && this.n2 == that.n2);
            }
            else
                return false;
        }
        public int compareTo(IntPair that){
            if (this == that || (this.n1 == that.n1 && this.n2 == that.n2))
                return 0;
            else if (this.n1 < that.n1 || this.n2 < that.n2)
                return -1;
            else
                return 1;
        }
    }

    static class StateList {
                
        int size;

        StateListNode first, last;

        StateListNode add(State q) {
            return new StateListNode(q, this);
        }
    }

    static class StateListNode {
                
        State q;

        StateListNode next, prev;

        StateList sl;

        StateListNode(State q, StateList sl) {
            this.q = q;
            this.sl = sl;
            if (sl.size++ == 0)
                sl.first = sl.last = this;
            else {
                sl.last.next = this;
                prev = sl.last;
                sl.last = this;
            }
        }

        void remove() {
            sl.size--;
            if (sl.first == this)
                sl.first = next;
            else
                prev.next = next;
            if (sl.last == this)
                sl.last = prev;
            else
                next.prev = prev;
        }
    }
}
