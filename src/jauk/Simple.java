/*
 * Jauk Examples
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
package jauk;

/**
 * A simple {@link Match} 
 * 
 * @see automaton.Match
 */
public class Simple
    extends Object
    implements Match
{
    private final CharSequence string;
    private final int start, end;


    /**
     * @param string Source string
     * @param start Substring head offset inclusive
     * @param end Substring tail offset exclusive
     */
    public Simple(CharSequence string, int start, int end){
	super();
	this.string = string;
	this.start = start;
	this.end = end;
    }


    public boolean satisfied(){
	return (this.end > this.start)&&(-1 < this.start);
    }
    public boolean terminal(){
	return (this.end == string.length());
    }
    public int next(){
	return this.end;
    }
    public int start(){
	return this.start;
    }
    public int end(){
	return this.end;
    }
    public String group(){
	if (this.end > this.start)
	    return this.string.subSequence(this.start,this.end).toString();
	else
	    return null;
    }
}
