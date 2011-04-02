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

/**
 * Pair of {@link State states}.
 * 
 * @author Anders Møller
 */
public final class StatePair
    extends Object
    implements Comparable<StatePair>
{
    protected State s;
    protected final State s1;
    protected final State s2;
    private final int hashCode;

        
    protected StatePair(State s, State s1, State s2) {
        super();
        this.s = s;
        this.s1 = s1;
        this.s2 = s2;
        this.hashCode = (this.s1.hashCode() + this.s2.hashCode());
    }
    protected StatePair(State s1, State s2) {
        super();
        this.s1 = s1;
        this.s2 = s2;
        this.hashCode = (this.s1.hashCode() + this.s2.hashCode());
    }
        

    public State getFirstState() {
        return s1;
    }
    public State getSecondState() {
        return s2;
    }
    public int hashCode() {
        return this.hashCode;
    }
    public boolean equals(Object tha) {
        if (this == tha)
            return true;
        else if (tha instanceof StatePair) {
            StatePair that = (StatePair)tha;

            return (that.s1 == this.s1 && that.s2 == this.s2);
        }
        else
            return false;
    }
    public int compareTo(StatePair that){
        if (this.equals(that))
            return 0;
        else if ((0 > this.s1.compareTo(that.s1))||(0 > this.s2.compareTo(that.s2)))
            return -1;
        else
            return 1;
    }
}
