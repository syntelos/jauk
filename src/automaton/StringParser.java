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

/**
 * Abstraction from under {@link RegExp}.
 * 
 * @author Anders Møller
 * @author John Pritchard
 */
public class StringParser
    extends Object
{
    protected final String string;

    protected final int length;

    protected int pos;

    protected int mark = -1;


    public StringParser(String s){
        super();
        this.string = s;
        if (null != s)
            this.length = s.length();
        else
            this.length = 0;
    }


    public final boolean peek(String s) {
        return (this.more() && (s.indexOf(this.string.charAt(this.pos)) != -1));
    }
    public final boolean match(char c) {
        if (this.pos >= this.length)
            return false;
        else if (this.string.charAt(this.pos) == c) {
            this.pos++;
            return true;
        }
        else
            return false;
    }
    public final boolean more() {
        return (this.pos < this.length);
    }
    public final char next() throws IllegalArgumentException {
        if (!this.more())
            throw new IllegalArgumentException("unexpected end-of-string");
        else
            return this.string.charAt(this.pos++);
    }
    public final String substring(int start, int end){
        return this.string.substring(start,end);
    }
    public final String substring(){
	if (-1 != this.mark)
	    return this.string.substring(this.mark,pos-1);
	else
	    return this.string.substring(0,pos-1);
    }
    public final StringParser reset(){
	this.mark = -1;
	this.pos = 0;
	return this;
    }
    public final StringParser mark(){
	this.mark = this.pos;
	return this;
    }
}
