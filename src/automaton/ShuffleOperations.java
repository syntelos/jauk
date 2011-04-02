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

import java.util.Arrays;

/**
 * @author Anders Møller
 */
public final class ShuffleOperations {
        
    public static Automaton Shuffle(Automaton a1, Automaton a2) {
        a1.determinize();
        a2.determinize();
        Transition[][] transitions1 = Automaton.GetSortedTransitions(a1.getStates());
        Transition[][] transitions2 = Automaton.GetSortedTransitions(a2.getStates());
        Automaton c = new Automaton();
        List<StatePair> worklist = new ArrayList<StatePair>();
        Map<StatePair, StatePair> newstates = new Map<StatePair, StatePair>();
        State s = new State();
        c.initial = s;
        StatePair p = new StatePair(s, a1.initial, a2.initial);
        worklist.add(p);
        newstates.put(p, p);
        while (worklist.isNotEmpty()) {
            p = worklist.removeFirst();
            p.s.accept = p.s1.accept && p.s2.accept;
            Transition[] t1 = transitions1[p.s1.number];
            for (int n1 = 0; n1 < t1.length; n1++) {
                StatePair q = new StatePair(t1[n1].to, p.s2);
                StatePair r = newstates.get(q);
                if (r == null) {
                    q.s = new State();
                    worklist.add(q);
                    newstates.put(q, q);
                    r = q;
                }
                p.s.add(new Transition(t1[n1].min, t1[n1].max, r.s));
            }
            Transition[] t2 = transitions2[p.s2.number];
            for (int n2 = 0; n2 < t2.length; n2++) {
                StatePair q = new StatePair(p.s1, t2[n2].to);
                StatePair r = newstates.get(q);
                if (r == null) {
                    q.s = new State();
                    worklist.add(q);
                    newstates.put(q, q);
                    r = q;
                }
                p.s.add(new Transition(t2[n2].min, t2[n2].max, r.s));
            }
        }
        c.deterministic = false;
        c.removeDeadTransitions();
        c.checkMinimizeAlways();
        return c;
    }

    private static void add(Character suspend_shuffle, Character resume_shuffle, 
                            List<ShuffleConfiguration> pending, Set<ShuffleConfiguration> visited, 
                            ShuffleConfiguration c, int i1, Transition t1, Transition t2, char min, char max)
    {
        final char HIGH_SURROGATE_BEGIN = '\uD800'; 
        final char HIGH_SURROGATE_END = '\uDBFF'; 
        if (suspend_shuffle != null && min <= suspend_shuffle && suspend_shuffle <= max && min != max) {
            if (min < suspend_shuffle)
                add(suspend_shuffle, resume_shuffle, pending, visited, c, i1, t1, t2, min, (char)(suspend_shuffle - 1));
            add(suspend_shuffle, resume_shuffle, pending, visited, c, i1, t1, t2, suspend_shuffle, suspend_shuffle);
            if (suspend_shuffle < max)
                add(suspend_shuffle, resume_shuffle, pending, visited, c, i1, t1, t2, (char)(suspend_shuffle + 1), max);
        } else if (resume_shuffle != null && min <= resume_shuffle && resume_shuffle <= max && min != max) {
            if (min < resume_shuffle)
                add(suspend_shuffle, resume_shuffle, pending, visited, c, i1, t1, t2, min, (char)(resume_shuffle - 1));
            add(suspend_shuffle, resume_shuffle, pending, visited, c, i1, t1, t2, resume_shuffle, resume_shuffle);
            if (resume_shuffle < max)
                add(suspend_shuffle, resume_shuffle, pending, visited, c, i1, t1, t2, (char)(resume_shuffle + 1), max);
        } else if (min < HIGH_SURROGATE_BEGIN && max >= HIGH_SURROGATE_BEGIN) {
            add(suspend_shuffle, resume_shuffle, pending, visited, c, i1, t1, t2, min, (char)(HIGH_SURROGATE_BEGIN - 1));
            add(suspend_shuffle, resume_shuffle, pending, visited, c, i1, t1, t2, HIGH_SURROGATE_BEGIN, max);
        } else if (min <= HIGH_SURROGATE_END && max > HIGH_SURROGATE_END) {
            add(suspend_shuffle, resume_shuffle, pending, visited, c, i1, t1, t2, min, HIGH_SURROGATE_END);
            add(suspend_shuffle, resume_shuffle, pending, visited, c, i1, t1, t2, (char)(HIGH_SURROGATE_END + 1), max);
        } else {
            ShuffleConfiguration nc = new ShuffleConfiguration(c, i1, t1.to, t2.to, min);
            if (suspend_shuffle != null && min == suspend_shuffle) {
                nc.shuffle_suspended = true;
                nc.suspended1 = i1;
            } else if (resume_shuffle != null && min == resume_shuffle)
                nc.shuffle_suspended = false;
            if (min >= HIGH_SURROGATE_BEGIN && min <= HIGH_SURROGATE_BEGIN) {
                nc.shuffle_suspended = true;
                nc.suspended1 = i1;
                nc.surrogate = true;
            }
            if (!visited.contains(nc)) {
                pending.add(nc);
                visited.add(nc);
            }
        }
    }

    protected final static class ShuffleConfiguration
        extends Object
        implements Comparable<ShuffleConfiguration>
    {
                
        ShuffleConfiguration prev;
        State[] ca_states;
        State a_state;
        char min;
        int hash;
        boolean shuffle_suspended;
        boolean surrogate;
        int suspended1;

                
        private ShuffleConfiguration(){
            super();
        }
        protected ShuffleConfiguration(Collection<Automaton> ca, Automaton a) {
            super();
            ca_states = new State[ca.size()];
            int i = 0;
            for (Automaton a1 : ca)
                ca_states[i++] = a1.getInitialState();
            a_state = a.getInitialState();
            computeHash();
        }
        protected ShuffleConfiguration(ShuffleConfiguration c, int i1, State s1, char min) {
            super();
            prev = c;
            ca_states = c.ca_states.clone();
            a_state = c.a_state;
            ca_states[i1] = s1;
            this.min = min;
            computeHash();
        }
        protected ShuffleConfiguration(ShuffleConfiguration c, int i1, State s1, State s2, char min) {
            super();
            prev = c;
            ca_states = c.ca_states.clone();
            a_state = c.a_state;
            ca_states[i1] = s1;
            a_state = s2;
            this.min = min;
            if (!surrogate) {
                shuffle_suspended = c.shuffle_suspended;
                suspended1 = c.suspended1;
            }
            computeHash();
        }


	public boolean equals(Object tha) {
            if (this == tha)
                return true;
            else if (tha instanceof ShuffleConfiguration) {
                ShuffleConfiguration that = (ShuffleConfiguration)tha;
                return (this.shuffle_suspended == that.shuffle_suspended &&
                        this.surrogate == that.surrogate &&
                        this.suspended1 == that.suspended1 &&
                        Arrays.equals(this.ca_states, that.ca_states) &&
                        this.a_state == that.a_state);
            }
            else
                return false;
        }
	public int hashCode() {
            return hash;
        }
        public int compareTo(ShuffleConfiguration that){
            if (this == that)
                return 0;
            else {
                int thisH = this.hash;
                int thatH = that.hash;
                if (thisH == thatH)
                    return 0;
                else if (thisH < thatH)
                    return 1;
                else
                    return -1;
            }
        }
        private void computeHash() {
            this.hash = 0;

            for (int i = 0; i < ca_states.length; i++){
                this.hash ^= ca_states[i].hashCode();
            }
            this.hash ^= (a_state.hashCode() * 100);

            if (shuffle_suspended || surrogate){
                this.hash += suspended1;
            }
        }
    }
}
