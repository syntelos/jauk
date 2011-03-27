/*
 * automaton
 *
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
 * Based on AutomatonMatcher by John Gibson, and RunAutomaton by
 * Anders M&oslash;ller.
 * 
 * @author John Pritchard
 */
public class Matcher 
    extends Object
    implements jauk.Match
{

    protected final CharSequence chars;

    protected final int start;

    protected final int end;


    public Matcher(CharSequence chars, Compiler automaton) {
	super();
	if (null != chars && null != automaton){
	    this.chars = chars;

	    int end = -1;
	    int p = automaton.getInitialState();
	    final int l = this.chars.length();
	    {
		for (int i = 0; i < l; end = i += 1) {

		    p = automaton.step(p, this.chars.charAt(i));
		    if (-1 == p) {
			end = -1;
			break;
		    }
		}
	    }
	    if (-1 == end){
		this.start = -1;
		this.end = end;
	    }
	    else if (automaton.isAccept(p)) {
		this.start = 0;
		this.end = end;
	    }
	    else {
		this.start = -1;
		this.end = -1;
	    }
	}
	else
	    throw new IllegalArgumentException();
    }


    public boolean satisfied(){
	return (this.end > this.start)&&(-1 < this.start);
    }
    public boolean terminal(){
	return (this.end+1 == this.chars.length());
    }
    public int next(){
	return (this.end+1);
    }
    public int start() {
        return this.start;
    }
    public int end() {
        return this.end;
    }
    public String group(){
        return this.chars.subSequence(this.start, this.end).toString();
    }
}
