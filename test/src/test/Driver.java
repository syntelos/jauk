package test;

import jauk.Re;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Read and execute test description plain text file.
 * 
 * File format
 * 
 * Plain text lines, first line a regular expression string (input to
 * {@link jauk.Re}), additional line(s) regexp test source filenames
 * named *.src (input to {@link test.Source}).
 * 
 * Operation
 * 
 * For each test source file, *.src, a file named
 * <code>"&lt;basename&gt;.tgt"</code> contains the expected correct
 * match result from the application of the regular expression to the
 * source file.
 */
public class Driver
    extends Object
{
    public static class Report
	extends Object
    {
	public int error, correct;
    }


    private final File file;

    private final Re re;

    private final Source[] src;

    private final Target[] tgt;

    private final int count;


    public Driver(File file)
	throws IOException
    {
	this(file,new InputStreamReader(new FileInputStream(file),"UTF-8"));
    }
    private Driver(File file, Reader reader)
	throws IOException
    {
	this(file,new BufferedReader(reader));
    }
    private Driver(File file, BufferedReader reader)
	throws IOException
    {
	super();
	this.file = file;
	final File dir = file.getParentFile();
	try {
	    this.re = new Re(ReadLine(reader));
	    Source[] sources = null;
	    Target[] targets = null;
	    String line;
	    while (null != (line = ReadLine(reader))){
		if (0 < line.length()){
		    Source src = new Source(dir,line);
		    sources = Source.Add(sources,src);
		    targets = Target.Add(targets,new Target(src));
		}
	    }
	    this.src = sources;
	    this.tgt = targets;
	    if (null == sources)
		this.count = 0;
	    else
		this.count = sources.length;
	}
	finally {
	    reader.close();
	}
    }


    public Driver.Report run()
	throws IOException
    {

	Driver.Report report = new Driver.Report();

	for (int idx = 0; idx < this.count; idx++){
	    Source src = this.src[idx];
	    Target tgt = this.tgt[idx];
	    try {
		String result = src.next(this.re);

		if (tgt.equals(result)){
		    System.out.printf("Correct in '%s'%n",this.file.getPath());
		    report.correct += 1;
		}
		else {
		    if (null == result)
			result = "<null>";

		    System.out.printf("Error in '%s': %s%n",this.file.getPath(),result);
		    report.error += 1;
		}
	    }
	    finally {
		src.close();
		tgt.close();
	    }
	}
	return report;
    }


    public final static String ReadLine(BufferedReader reader)
	throws IOException
    {
	do {
	    String line = reader.readLine();
	    if (null != line){
		if (0 < line.length()){
		    if ('#' == line.charAt(0))
			continue;
		    else
			return line;
		}
		else
		    continue;
	    }
	    else
		return null;
	}
	while (true);
    }
}
