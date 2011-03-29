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
 * <h3>File format</h3>
 * 
 * The plain text "test/{driver}.txt" file describes a test sequence
 * set.  The format of the driver file is outlined in the following.
 * 
 * <pre>
 * 
 *   optional '#' line comments
 * 
 *   separator string line
 * 
 *   ordered list of one or more regular expression lines
 * 
 *   separator string line
 * 
 *   one or more source text file names
 * 
 * <pre>
 * 
 * Any line may begin with the pound character '#', and be ignored.
 * 
 * The separator string is one or more characters to be reflected at
 * the end of the enclosed list of regular expressions.  
 * 
 * The binary character pairs including brackets and parentheses are
 * reflected in their respective opposite or inverted character.
 * Other strings are reflected in a literal copy -- to mark the end of
 * the list of regular expressions.
 * 
 * Each regular expression in this list may be indented by whitespace
 * which will be trimmed.
 * 
 * Following the sequence list of regular expressions, a test source
 * filename named "*.src" is identified.
 * 
 * Implied by the number of regular expressions in the list and the
 * test source filename are one or more test target files.  Each
 * implied target file is named "*.tgt{N}" for {N} an integer between
 * zero and nine (inclusive).  The numbering of targets corresponds
 * with the list of regular expressions, from zero to nine.
 * 
 * <h3>Operation</h3>
 * 
 * Each regular expression is applied in sequence to test source file.
 * Each result must be byte - identical with the corresponding target
 * file.
 * 
 * <h3>For example</h3>
 * 
 * <pre>
 * #
 * # ex.txt
 * #
 * %
 * &lt;CComment&gt;
 * [^{]*{[^}]*}
 * %
 * ex.src
 * #
 * # requires ex.tgt0 and ex.tgt1
 * #
 * </pre>
 */
public class Driver
    extends Object
{
    public static class Report
        extends Object
    {
        public int error, correct;
    }


    private final String listBegin, listEnd;

    private final File file;

    private final Re[] re;

    private final Source src;

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
            this.listBegin = ReadLine(reader);
            this.listEnd = ListEnd(this.listBegin);

            Re[] re = null;
            String line;
            while (null != (line = ReadLine(reader))){
                if (this.listEnd.equals(line))
                    break;
                else {
                    try {
                        re = Re.Add(re,new Re(line));
                    }
                    catch (RuntimeException exc){
                        throw new IllegalStateException(file.getPath(),exc);
                    }
                }
            }

            if (null != re){
                final int count = re.length;

                final Source src = new Source(dir,ReadLine(reader));

                Target[] tgt = new Target[count];
                for (int cc = 0; cc < count; cc++){

                    tgt[cc] = new Target(src,cc);
                }
                this.count = count;
                this.re = re;
                this.src = src;
                this.tgt = tgt;
            }
            else {
                this.count = 0;
                this.re = null;
                this.src = null;
                this.tgt = null;
            }
        }
        finally {
            reader.close();
        }
    }


    public Driver.Report run()
        throws IOException
    {
        Driver.Report report = new Driver.Report();
        final Source src = this.src;
        for (int idx = 0; idx < this.count; idx++){
            Re re = this.re[idx];
            Target tgt = this.tgt[idx];
            String result = src.next(re);

            if (tgt.equals(result)){
                System.out.printf("Success for tgt%d from '%s'%n",idx,this.file.getPath());
                report.correct += 1;
            }
            else {
                if (null == result)
                    result = "<null>";

                System.out.printf("Failure for tgt%d from '%s': %s%n",idx,this.file.getPath(),result);
                report.error += 1;
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

                line = line.trim();

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
    public final static String ListEnd(String listBegin){
        if (1 == listBegin.length()){
            switch(listBegin.charAt(0)){
            case '[':
                return "]";
            case ']':
                return "[";
            case '{':
                return "}";
            case '}':
                return "{";
            case '(':
                return ")";
            case ')':
                return "(";
            case '<':
                return ">";
            case '>':
                return "<";
            }
        }
        return listBegin;
    }
}
