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
import lxl.Set;

import java.io.Serializable;
import java.util.Arrays;

/** 
 * {@link Automaton} state. 
 * 
 * @see Automaton
 * @author Anders Møller
 */
public class State 
    extends Object
    implements Comparable<State>, Iterable<Transition>
{


    private volatile static int next_id;



    private String name;

    public final int id;

    public boolean accept;

    private Set<Transition> transitions;
        
    public int number;
        


        
    /** 
     * Initially, the new state is a reject state. 
     */
    public State() {
        this("");
    }
    public State(State s) {
        this((null != s)?(s.name):(""));
    }
    public State(String name){
        super();
        this.id = next_id++;
        this.transitions = new Set<Transition>();
        if (null == name || 1 > name.length())
            this.name = null;
        else
            this.name = name;
    }
    public State(boolean accept){
        this();
        this.accept = accept;
    }
    public State(String name, boolean accept){
        this(name);
        this.accept = accept;
    }
        

    public String name(){
        if (null != this.name)
            return this.name ;
        else
            return String.valueOf(this.number);
    }
    protected final Set<Transition> resetTransitions() {
        Set<Transition> old = this.transitions;
        this.transitions = new Set<Transition>();
        return old;
    }
    public Set<Transition> getTransitions(){
        return transitions;
    }
    public State addTransition(Transition t){
        transitions.add(t);
        return this;
    }
    public State setAccept(boolean accept){
        this.accept = accept;
        return this;
    }
    public boolean isAccept() {
        return accept;
    }
    /** 
     * Performs lookup in transitions, assuming determinism. 
     * @param c character to look up
     * @return destination state, null if no matching outgoing transition
     * @see #step(char, Collection)
     */
    public State step(char c) {
        for (Transition t : transitions){
            if (t.min <= c && c <= t.max){

                return t.to;
            }
        }
        return null;
    }
    /** 
     * Performs lookup in transitions, allowing nondeterminism.
     * @param c character to look up
     * @param dest collection where destination states are stored
     * @see #step(char)
     */
    public void step(char c, Collection<State> dest) {
        for (Transition t : transitions){
            if (t.min <= c && c <= t.max)
                dest.add(t.to);
        }
    }
    protected boolean isNotEmpty(){

        return (!this.transitions.isEmpty());
    }
    protected boolean isEmpty(){

        return this.transitions.isEmpty();
    }
    protected int size(){

        return this.transitions.size();
    }
    protected State add(Transition t) {

        this.transitions.add(t);

        return this;
    }
    protected Transition first(){

        return this.iterator().next();
    }
    protected State set( Set<Transition> transitions){

        this.transitions = transitions;

        return this;
    }
    protected State addEpsilon(State to) {
        if (to.accept)
            this.accept = true;

        for (Transition t : to){
            this.transitions.add(t);
        }
        return this;
    }
    /**
     * Returns sorted array of outgoing transitions.
     * @param to_first Order by (to, min, reverse max); otherwise (min, reverse max, to)
     */
    protected Transition[] getSortedTransitionArray(boolean to_first) {
        Transition[] e = this.transitions.toArray(Transition.class);
        if (null != e){
            Arrays.sort(e, new TransitionComparator(to_first));
            return e;
        }
        else
            return new Transition[0];
    }
    /**
     * Returns sorted list of outgoing transitions.
     * @param to_first Order by (to, min, reverse max); otherwise (min, reverse max, to)
     */
    public List<Transition> getSortedTransitions(boolean to_first){
        return new ArrayList(this.getSortedTransitionArray(to_first));
    }
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("state ").append(name);
        if (accept)
            b.append(" [accept]");
        else
            b.append(" [reject]");
        b.append(":\n");
        for (Transition t : transitions)
            b.append("  ").append(t.toString()).append("\n");
        return b.toString();
    }
    public int compareTo(State that) {
        if (this.id == that.id)
            return 0;
        else if (this.id > that.id)
            return 1;
        else
            return -1;
    }
    public java.util.Iterator<Transition> iterator(){
        return this.transitions.iterator();
    }
}
