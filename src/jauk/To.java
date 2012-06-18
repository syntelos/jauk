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
 * Simple substring operators
 */
public class To
    extends Object
    implements Pattern
{
    private final char ch;
    private final String source;


    public To(String substring){
        super();
        if (null != substring && 0 < substring.length()){
            this.source = substring;
            this.ch = 0;
        }
        else
            throw new IllegalArgumentException();
    }
    public To(char ch){
        super();
        this.ch = ch;
        this.source = null;
    }


    public boolean matches(CharSequence target){

	return this.match(target).satisfied();
    }
    public Match match(CharSequence target){
        return this.match(target,0);
    }
    public Match match(CharSequence target, int targeti){
        return this.match(target,targeti,1);
    }
    public Match match(CharSequence target, int targeti, int lno){
        final int targetl = target.length();
        if (0 < targetl){
            if (null != this.source){
                final int sourcel = this.source.length();

                match:
                for (int sourcei = 0, ti = targeti; sourcei < sourcel; sourcei++, ti++){

                    if ((ti >= targetl)||(this.source.charAt(sourcei) != target.charAt(ti))){
                        /*
                         * Empty substring
                         */
                        return new Simple(target,targeti,targeti,lno);
                    }
                }
                return new Simple(target,targeti,(targeti+sourcel),lno);
            }
            else {

                if (this.ch == target.charAt(targeti)){

                    return new Simple(target,targeti,(targeti+1),lno);
                }
            }
        }
        /*
         * Empty substring
         */
        return new Simple(target,targeti,targeti,lno);
    }
    public Match search(CharSequence target){
        return this.search(target,0);
    }
    public Match search(CharSequence target, int targeti){
        return this.search(target,targeti,1);
    }
    public Match search(CharSequence target, int targeti, int lno){
        final int targetl = target.length();
        if (0 < targetl){
            if (null != this.source){
                final int sourcel = this.source.length();
                int sourcei = 0;
                char sourcec = this.source.charAt(sourcei);
                search:
                for (; targeti < targetl; targeti++){
                    if (sourcec == target.charAt(targeti)){
                        find:
                        for (int ti = targeti; ti < targetl; ti++){
                            sourcei += 1;
                            if (sourcei < sourcel){
                                sourcec = this.source.charAt(sourcei);
                                if (sourcec == target.charAt(ti))
                                    continue find;
                                else
                                    break search;
                            }
                            else {
                                return new Simple(target,targeti,(targeti+sourcel),lno);
                            }
                        }
                    }
                }
            }
            else {
                search:
                for (int ti = targeti; ti < targetl; ti++){

                    if (this.ch == target.charAt(ti)){

                        return new Simple(target,targeti,(ti+1),lno);
                    }
                }
            }
        }
        /*
         * Empty substring
         */
        return new Simple(target,targeti,targeti,lno);
    }
}
