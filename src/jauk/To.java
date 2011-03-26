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
 * Match the source substring up to and including a target substring.
 */
public class To
    extends Object
    implements Pattern
{
    private final char ch;
    private final String st;


    public To(String substring){
	super();
	if (null != substring && 0 < substring.length()){
	    this.st = substring;
	    this.ch = 0;
	}
	else
	    throw new IllegalArgumentException();
    }
    public To(char ch){
	super();
	this.ch = ch;
	this.st = null;
    }


    public Match apply(CharSequence string){
	final int strlen = string.length();
	if (0 < strlen){
	    if (null != this.st){
		final int stl = this.st.length();
		int sti = 0;
		char stc = this.st.charAt(sti);
		search:
		for (int si = 0; si < strlen; si++){
		    if (stc == string.charAt(si)){
			find:
			for (int fi = si; fi < strlen; fi++){
			    sti += 1;
			    if (sti < stl){
				stc = this.st.charAt(sti);
				if (stc == string.charAt(fi))
				    continue find;
				else
				    break search;
			    }
			    else {
				return new Simple(string,si,(si+stl-1));
			    }
			}
		    }
		}
	    }
	    else {
		search:
		for (int si = 0; si < strlen; si++){
		    if (this.ch == string.charAt(si))
			return new Simple(string,si,(si+1));
		}
	    }
	}
	return new Simple(string,0,0);
    }
}
