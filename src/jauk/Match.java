/*
 * Jauk
 * Copyright (C) 2011 John Pritchard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package jauk;

/**
 * Match from pattern.
 */
public interface Match
{

    /**
     * Pattern is satisfied
     */
    public boolean satisfied();
    /**
     * Input has been consumed
     */
    public boolean terminal();
    /**
     * Last index in pattern plus one.
     */
    public int next();
    /**
     * First index in matched character sequence
     */
    public int start();
    /**
     * As the "end" argument to CharSequence subSequence: index
     * trailing matched character sequence (exclusive tail)
     */
    public int end();
    /**
     * Matched character sequence as string
     */
    public String group();
    /**
     * @return Line number before group (exclusive) -- (counting newline
     * characters).
     */
    public int lnoX();
    /**
     * @return Line number in group (inclusive) -- (counting newline
     * characters).
     */
    public int lnoN();
    /**
     * Internal buffer
     */
    public CharSequence buffer();
    /**
     * Apply pattern <i>into</i> this string (in "search to")
     */
    public Match search(Pattern pattern);
    /**
     * Apply pattern <i>onto</i> this string (in "match on")
     */
    public Match match(Pattern pattern);
    /**
     * @return This string minus the argument substring, otherwise
     * this
     */
    public Match subtract(Match substring);

    /**
     * {@link Scanner} internal matching history.
     * 
     * @author jdp
     */
    public final class Stack {

        private Match[] stack;

        private int head;


        public Stack(){
            super();
        }


        /**
         * @return Current buffer position from stack head employed by
         * {@link Scanner}, default zero for empty stack
         * @see #head()
         */
        public int position(){
            if (null != this.stack)
                return this.current().next();
            else
                return 0;
        }
        /**
         * @return Previous buffer position from <code>peek(+1)</code>,
         * default zero
         * @see #head()
         */
        public int previous(){
            Match previous = this.peek(1);
            if (null != this.stack)
                return this.current().next();
            else
                return 0;
        }
        /**
         * The value of {@link #current()} is identical to the value
         * of <code>peek(0)</code>.
         * 
         * @return Stack head, otherwise null for empty stack
         */
        public Match current(){
            if (null != this.stack)
                return this.stack[this.head];
            else
                return null;
        }
        /**
         * @return Present value of head (stack array pointer-index):
         * default zero, incremented by {@link #pop}, reset to zero by
         * {@link #push(jauk.Match)}
         */
        public int head(){

            return this.head;
        }
        /**
         * @param peek Relative offset from head, see {@link #peek(int)}
         * @return Peek offset addresses match
         */
        public boolean valid(int peek){

            final int idx = (this.head + peek);

            return (null != this.stack && -1 < idx && idx < this.stack.length);
        }
        /**
         * The value of {@link #current()} is identical to the value
         * of <code>peek(0)</code>.
         * 
         * @param ofs Relative to head (default zero, incremented by
         * {@link #pop}, reset to zero by {@link #push(jauk.Match)})
         * 
         * @return Null for invalid offset
         */
        public Match peek(int ofs){

            final int idx = (this.head + ofs);

            if (null != this.stack && -1 < idx && idx < this.stack.length)

                return this.stack[idx];
            else
                return null;
        }
        /**
         * Replace head
         * 
         * @see #replace(int,jauk.Match)
         */
        public Match replace(Match r)
            throws java.lang.IllegalArgumentException
        {
            return this.replace(0,r);
        }
        /**
         * @param ofs Relative offset from head.  See {@link
         * #peek(int)} and {@link #valid(int)}
         * 
         * @param r New match set to replace existing match set
         * 
         * @return New match set
         * 
         * @exception java.lang.IllegalArgumentException For null or
         * empty match set, or invalid offset
         */
        public Match replace(int ofs, Match r)
            throws java.lang.IllegalArgumentException
        {

            if (null != r && r.satisfied()){

                final int idx = (this.head + ofs);

                if (null != this.stack && -1 < idx && idx < this.stack.length)

                    return (this.stack[idx] = r);
                else
                    throw new IllegalArgumentException();
            }
            else
                throw new IllegalArgumentException();
        }
        /**
         * Increment head pointer if possible, otherwise no effect.
         * @return The current match set.
         */
        public Match pop(){

            if (null != this.stack && (this.head+1) < this.stack.length){

                this.head += 1;
            }
            return this.current();
        }
        /**
         * Stack list insert to head.  Reset head to zero.
         * 
         * @param push A non empty match
         *
         * @exception java.lang.IllegalArgumentException For null or
         * empty (unsatisfied) argument
         */
        public Match push(Match push)
            throws java.lang.IllegalArgumentException
        {

            if (null == push || (!push.satisfied()))

                throw new IllegalArgumentException();

            else if (null == this.stack){

                this.stack = new Match[]{push};

                this.head = 0;

                return push;
            }
            else {
                final int len = this.stack.length;
                final Match[] copier = new Match[len+1];
                System.arraycopy(stack,0,copier,1,len);
                copier[0] = push;

                this.stack = copier;

                this.head = 0;

                return push;
            }
        }
        public void clear(){
            this.stack = null;
            this.head = 0;
        }
    }
}
