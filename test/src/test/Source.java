package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

/**
 * @see Driver
 */
public class Source
    extends jauk.Scanner
{
    public final File file;


    public Source(File dir, String file)
	throws IOException
    {
	this(Translate(dir,file));
    }
    private Source(File file)
	throws IOException
    {
	super(file);
	this.file = file;
    }


    public final static Source[] Add(Source[] list, Source item){
	if (null == item)
	    return list;
	else if (null == list)
	    return new Source[]{item};
	else {
	    final int len = list.length;
	    Source[] copier = new Source[len+1];
	    System.arraycopy(list,0,copier,0,len);
	    copier[len] = item;
	    return copier;
	}
    }
    public final static File Translate(File dir, String file){
	File test = new File(file);
	if (test.isFile()){
	    File testdir = test.getParentFile();
	    if (null != testdir){
		if (testdir.equals(dir))
		    return test;
	    }
	    else if (null == dir){
		return test;
	    }
	}
	return new File(dir,file);
    }
}
