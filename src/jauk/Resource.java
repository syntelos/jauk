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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * Typed string for file and classpath resource.
 */
public class Resource
    extends Object
    implements java.lang.Comparable<Resource>
{

    public final File file;

    public final String path;


    public Resource(File file){
        super();
        if (null != file && file.isFile()){
            this.path = file.getPath();
            this.file = file;
        }
        else
            throw new IllegalArgumentException();
    }
    public Resource(File dir, String path){
        super();
        if (null != dir){
            if (null != path && 0 != path.length()){
                this.path = path;
                this.file = new File(dir,path);
            }
            else
                throw new IllegalArgumentException(path);
        }
        else
            throw new IllegalArgumentException();
    }
    public Resource(String path){
        super();
        this.file = null;
        if (null != path && 0 != path.length())
            this.path = path;
        else
            throw new IllegalArgumentException(path);
    }
    public Resource(String path, String prefix, String suffix){
        super();
        this.file = null;
        if (null != path && 0 != path.length()){

            if (null != suffix && (!path.endsWith(suffix)))
                path += suffix;

            if (null != prefix && (!path.startsWith(prefix)))
                path = (prefix + path);

            this.path = path;
        }
        else
            throw new IllegalArgumentException(path);
    }
    public Resource(File dir, Resource resource){
        super();
        if (null != dir && null != resource){
            String path = resource.path;
            if ('/' == path.charAt(0))
                path = path.substring(1);

            this.path = path;

            String test = dir.getName()+'/';
            if (path.startsWith(test))
                path = path.substring(test.length());

            this.file = new File(dir,path);
        }
        else
            throw new IllegalArgumentException();
    }


    public String getPath(){
        return this.path;
    }
    public InputStream openStream(){
        if (null != this.file){
            try {
                return new FileInputStream(this.file);
            }
            catch (IOException exc){
                return null;
            }
        }
        else
            return this.getClass().getResourceAsStream(this.path);
    }
    public OutputStream openOutput(){
        if (null != this.file){
            try {
                return new FileOutputStream(this.file);
            }
            catch (IOException exc){
                return null;
            }
        }
        else
            return null;
    }
    public Enumeration<URL> scanfor() throws IOException {
        return Thread.currentThread().getContextClassLoader().getResources(this.path);
    }
    public boolean hasFile(){
        return (null != this.file);
    }
    public File getFile(){
        return this.file;
    }
    public File getParentFile(){
        if (null != this.file)
            return this.file.getParentFile();
        else
            throw new IllegalStateException();
    }
    public long getLastModified(){
        File file = this.file;
        if (null != file)
            return file.lastModified();
        else
            return System.currentTimeMillis();
    }
    public File filein(File dir){
        return new File(dir,this.path);
    }
    public boolean dropTouch(){
        if (null != this.file){
            try {
                this.openOutput().close();
                return true;
            }
            catch (Exception err){
            }
        }
        return false;
    }
    public String toString(){
        return this.path;
    }
    public int hashCode(){
        return this.path.hashCode();
    }
    public boolean equals(Object that){
        if (this == that)
            return true;
        else if (null != that)
            return this.path.equals(that.toString());
        else
            return false;
    }
    public int compareTo(Resource that){
        return this.path.compareTo(that.getPath());
    }
}
