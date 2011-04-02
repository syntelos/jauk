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
 * A transition, which belongs to a source state, consists of a
 * Unicode character interval and a destination state.
 * 
 * @see State
 * @author Anders Møller
 */
public class Transition 
    extends Object
    implements Comparable<Transition>
{
    /*
     * CLASS INVARIANT: min &lt;= max
     */
    public char min;
    public final char max;
    public State to;
        

    public Transition(char c, State to){
        super();
        this.min = this.max = c;
        this.to = to;
    }
    public Transition(char min, char max, State to){
        super();
        if (max < min) {
            char t = max;
            max = min;
            min = t;
        }
        this.min = min;
        this.max = max;
        this.to = to;
    }

    public char getMin() {
        return min;
    }
    public char getMax() {
        return max;
    }
    public State getDest() {
        return to;
    }
    public int hashCode() {
        return min * 2 + max * 3;
    }
    public boolean equals(Object tha) {
        if (this == tha)
            return true;
        else if (tha instanceof Transition) {
            Transition that = (Transition)tha;
            return (that.min == this.min && that.max == this.max && that.to == this.to);
        }
        else
            return false;
    }
    public int compareTo(Transition that){
        if (this.equals(that))
            return 0;
        else if (this.min < that.min || this.max < that.max)
            return -1;
        else
            return 1;
    }
    public String toString() {
        StringBuilder b = new StringBuilder();
        appendCharString(min, b);
        if (min != max) {
            b.append("-");
            appendCharString(max, b);
        }
        b.append(" -> ").append(to.number);
        return b.toString();
    }
    private void appendDot(StringBuilder b) {
        b.append(" -> ").append(to.number).append(" [label=\"");
        appendCharString(min, b);
        if (min != max) {
            b.append("-");
            appendCharString(max, b);
        }
        b.append("\"]\n");
    }

        
    static void appendCharString(char c, StringBuilder b) {
        if (c >= 0x21 && c <= 0x7e && c != '\\' && c != '"')
            b.append(c);
        else {
            b.append("\\u");
            String s = Integer.toHexString(c);
            if (c < 0x10)
                b.append("000").append(s);
            else if (c < 0x100)
                b.append("00").append(s);
            else if (c < 0x1000)
                b.append("0").append(s);
            else
                b.append(s);
        }
    }
}
