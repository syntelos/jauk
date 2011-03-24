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
    extends Object
{
    public final static Charset UTF8 = Charset.forName("UTF-8");


    private Readable source;

    private CharBuffer buffer;

    private boolean open;

    private int next;


    public Scanner(Resource source){
        this(source.openStream());
    }
    public Scanner(Readable source){
        super();
        if (null != source){
            this.source = source;
            this.buffer = CharBuffer.allocate(0x200);
            this.buffer.limit(0);
            this.open = true;
            this.read();
        }
        else
            throw new IllegalArgumentException();
    }
    public Scanner(InputStream source){
        this(new InputStreamReader(source,UTF8));
    }
    public Scanner(InputStream source, Charset cs){
        this(new InputStreamReader(source,cs));
    }
    public Scanner(File source)
        throws java.io.FileNotFoundException
    {
        this(source,UTF8);
    }
    public Scanner(File source, Charset cs)
        throws java.io.FileNotFoundException
    {
        this(Channels.newReader((new FileInputStream(source).getChannel()),cs.newDecoder(),-1));
    }
    public Scanner(String source){
        this(new StringReader(source));
    }
    public Scanner(ReadableByteChannel source){
        this(Channels.newReader(source,UTF8.newDecoder(),-1));
    }
    public Scanner(ReadableByteChannel source, Charset cs){
        this(Channels.newReader(source,cs.newDecoder(),-1));
    }


    public void close(){
        if (this.open){
            this.open = false;
            if (this.source instanceof Closeable)
                try {
                    ((Closeable)this.source).close();
                }
                catch (IOException ignore){
                }
        }
    }
    public String next(Pattern pattern){

        Match match = this.match(pattern);
        if (null != match)
            return match.group();
        else
            return null;
    }
    public Match match(Pattern pattern){

        CharBuffer buf = this.buffer;

        int next = this.next;
        if (0 != next){
            this.next = 0;
            /*
             * Change buffer state after previous match result has
             * been consumed.
             */
            buf.position(next);
            buf.compact().flip();
        }

        Match match;
        do {
            match = pattern.apply(buf);

            if (match.satisfied()){

                if (match.terminal()){

                    buf = this.read();

                    if (null == buf)
                        return null;
                }
                else {
                    this.next = match.next();

                    return match;
                }
            }
            else if (match.terminal()){
		/*
		 * Buffer is short, expand and retry..
		 */
                buf = this.read();

                if (null == buf)
                    return null;
            }
            else {
                return null;
            }
        }
        while (true);
    }

    private CharBuffer read(){

        CharBuffer buf = this.buffer;

        if (this.open){

            if (buf.limit() == buf.capacity()){
                CharBuffer copier = CharBuffer.allocate(buf.capacity()+0x200);
                copier.put(buf);
                copier.flip();
                buf = copier;
                this.buffer = buf;
            }
            /*
             * Flip to tail for fill
             */
            int tail = buf.limit();
            buf.position(tail);
            buf.limit(buf.capacity());

            try {
                /*
                 * Fill
                 */
                if (-1 == source.read(buf))
                    this.close();
            }
            catch (IOException ioe) {

                this.close();
            }
            /*
             * Flip to head for consumer
             */
            buf.limit(buf.position());
            buf.position(0);

            return buf;
        }
        else
            return null;
    }

}
