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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;


/**
 * A verbatum scanner does the I/O and just the I/O.  Drives {@link
 * Pattern} and {@link Match}.
 * 
 * @author jdp
 */
public class Scanner
    extends java.io.Reader
    implements CharSequence, Readable, Closeable
{
    public final static Charset UTF8 = Charset.forName("UTF-8");


    private final CharBuffer buffer;

    private final int length;

    private final Match.Stack stack = new Match.Stack();


    public Scanner(Resource source)
        throws IOException
    {
        this(source.openStream());
    }
    public Scanner(Readable source)
        throws IOException
    {
        super();
        if (null != source){
            CharBuffer buffer = CharBuffer.allocate(0x200);
            try {
                while (0 < source.read(buffer)){

                    if (buffer.limit() == buffer.capacity()){
                        /*
                         * Flip to head for copy
                         */
                        buffer.flip();

                        CharBuffer copier = CharBuffer.allocate(buffer.capacity()+0x200);
                        copier.put(buffer);
                        buffer = copier;
                    }
                }

		buffer.flip();

                this.buffer = buffer;
                this.length = buffer.limit();
            }
            finally {
                if (source instanceof Closeable){
                    try {
                        ((Closeable)source).close();
                    }
                    catch (IOException ignore){
                    }
                }
            }
        }
        else
            throw new IllegalArgumentException();
    }
    public Scanner(InputStream source)
        throws IOException
    {
        this(new InputStreamReader(source,UTF8));
    }
    public Scanner(InputStream source, Charset cs)
        throws IOException
    {
        this(new InputStreamReader(source,cs));
    }
    public Scanner(File source)
        throws IOException
    {
        this(source,UTF8);
    }
    public Scanner(File source, Charset cs)
        throws IOException
    {
        this(Channels.newReader((new FileInputStream(source).getChannel()),cs.newDecoder(),-1));
    }
    public Scanner(String source)
        throws IOException
    {
        this(new StringReader(source));
    }
    public Scanner(ReadableByteChannel source)
        throws IOException
    {
        this(Channels.newReader(source,UTF8.newDecoder(),-1));
    }
    public Scanner(ReadableByteChannel source, Charset cs)
        throws IOException
    {
        this(Channels.newReader(source,cs.newDecoder(),-1));
    }


    public void revert(){

        this.stack.pop();
    }
    /**
     * @return Match history, including the scanner's current position
     * in the input at the head of the match history stack
     */
    public Match.Stack stack(){

        return this.stack;
    }
    public String next(Pattern pattern){

        Match match = this.match(pattern);
        if (null != match)
            return match.group();
        else
            return null;
    }
    public Match match(Pattern pattern){

        Match match = pattern.match(this.buffer,this.stack.position(),this.currentLine());

        if (match.satisfied())

            return this.stack.push(match);
        else 
            return null;
    }
    public Match search(Pattern pattern){

        Match match = pattern.search(this.buffer,this.stack.position(),this.currentLine());

        if (match.satisfied())

            return this.stack.push(match);
        else 
            return null;
    }
    public boolean isEmpty(){

        return (this.stack.position() >= this.length);
    }
    public boolean isNotEmpty(){

        return (this.stack.position() < this.length);
    }
    public int previousLine(){

        Match previous = this.stack.peek(1);
        if (null != previous)
            return previous.lnoN();
        else
            return 0;
    }
    public String previousCapture(){

        Match previous = this.stack.peek(1);
        if (null != previous)
            return previous.group();
        else
            return null;
    }
    public int currentLine(){

        Match current = this.stack.peek(0);
        if (null != current)
            return current.lnoN();
        else
            return 1;
    }
    public String currentCapture(){

        Match current = this.stack.peek(0);
        if (null != current)
            return current.group();
        else
            return null;
    }
    /**
     * Analytical presentation as for "unrecognized input" errors:
     * control characters are represented in their character escape
     * form (e.g. "\t" or "\n"), or control form (e.g. "^A" or "^Z");
     * the future source string up to the next line terminal (carriage
     * return or new line) is returned.
     */
    public String nextCapture(){
        StringBuilder nextCapture = new StringBuilder();
        int analysis = 0;
        for (int p = this.stack.position(); p < this.length; p++){
            char ch = this.buffer.charAt(p);
            switch(ch){
            case '\u0000':
                nextCapture.append("^@");
                break;
            case '\u0001':
                nextCapture.append("^A");
                break;
            case '\u0002':
                nextCapture.append("^B");
                break;
            case '\u0003':
                nextCapture.append("^C");
                break;
            case '\u0004':
                nextCapture.append("^D");
                break;
            case '\u0005':
                nextCapture.append("^E");
                break;
            case '\u0006':
                nextCapture.append("^F");
                break;
            case '\u0007':
                nextCapture.append("\\a");
                break;
            case '\b':
                nextCapture.append("\\b");
                break;
            case '\t':
                nextCapture.append("\\t");
                break;
            case '\n':
                if (analysis < nextCapture.length())
                    return nextCapture.toString();
                else {
                    analysis += 1;
                    nextCapture.append("\\n");
                }
                break;
            case '\u000B':
                nextCapture.append("\\v");
                break;
            case '\f':
                nextCapture.append("\\f");
                break;
            case '\r':
                if (analysis < nextCapture.length())
                    return nextCapture.toString();
                else {
                    analysis += 1;
                    nextCapture.append("\\r");
                }
                break;
            case '\u000E':
                nextCapture.append("^N");
                break;
            case '\u000F':
                nextCapture.append("^O");
                break;
            case '\u0010':
                nextCapture.append("^P");
                break;
            case '\u0011':
                nextCapture.append("^Q");
                break;
            case '\u0012':
                nextCapture.append("^R");
                break;
            case '\u0013':
                nextCapture.append("^S");
                break;
            case '\u0014':
                nextCapture.append("^T");
                break;
            case '\u0015':
                nextCapture.append("^U");
                break;
            case '\u0016':
                nextCapture.append("^V");
                break;
            case '\u0017':
                nextCapture.append("^W");
                break;
            case '\u0018':
                nextCapture.append("^X");
                break;
            case '\u0019':
                nextCapture.append("^Y");
                break;
            case '\u001A':
                nextCapture.append("^Z");
                break;
            case '\u001B':
                nextCapture.append("\\e");
                break;
            case '\u001C':
                nextCapture.append("^\\");
                break;
            case '\u001D':
                nextCapture.append("^]");
                break;
            case '\u001E':
                nextCapture.append("^^");
                break;
            case '\u001F':
                nextCapture.append("^_");
                break;
            case '\u007F':
                nextCapture.append("^?");
                break;
            default:
                nextCapture.append(ch);
                break;
            }
        }
        return nextCapture.toString();
    }
    /*
     * Additional utility for instances of this class, not compatible
     * with concurrent/mixed matching, etc..
     */
    public int length(){
        return this.length;
    }
    public char charAt(int idx){
        return this.buffer.charAt(idx);
    }
    public CharSequence subSequence(int start, int end){
        return this.buffer.subSequence(start,end);
    }
    public String toString(){
        return this.buffer.toString();
    }
    public int read() throws IOException {
        try {
            return this.buffer.get();
        }
        catch (java.nio.BufferUnderflowException eof){
            return -1;
        }
    }
    public int read(char[] buf, int ofs, int len)
        throws IOException
    {
        int rem = this.buffer.remaining();
        if (len <= rem){
            this.buffer.get(buf,ofs,len);
            return len;
        }
        else {
            this.buffer.get(buf,ofs,rem);
            return rem;
        }
    }
    public long skip(long n) throws IOException {
        final long newp = (this.buffer.position() + n);
        final int clamp = (int)Math.min(this.length,newp);
        final int start = this.buffer.position();
        this.buffer.position(clamp);
        final int end = this.buffer.position();
        return (end-start);
    }
    public boolean ready(){
        return true;
    }
    public boolean markSupported(){
        return true;
    }
    public void mark(int m)
        throws IOException
    {
        this.buffer.mark();
    }
    public void reset()
        throws IOException
    {
        this.buffer.reset();
    }
    public void close()
        throws IOException
    {
        this.stack.clear();
        /*
         * Deterministic "flip" succeeds in all use cases
         */
        this.buffer.limit(this.length);
        /*
         * Clear mark, too..
         */
        this.buffer.position(0);
    }
    public boolean equals(CharSequence that){
        if (null == that)
            return (0 == this.length);
        else if (that.length() == this.length){
            for (int idx = 0; idx < this.length; idx++){
                if (this.charAt(idx) != that.charAt(idx))
                    return false;
            }
            return true;
        }
        return false;
    }
}
