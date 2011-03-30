package test;

import java.io.File;

/**
 * Invoke in a directory containing "*.txt".
 */
public class Main
    extends Object
    implements java.io.FileFilter, 
               java.lang.Iterable<File>, 
               java.util.Iterator<File>
{
    public final File[] list;
    public final int length;
    private int index;


    public Main(File dir){
        super();
	if (dir.isDirectory()){
	    this.list = dir.listFiles(this);
	    if (null == this.list)
		this.length = 0;
	    else
		this.length = this.list.length;
	}
	else if (dir.isFile()){
	    this.list = new File[]{dir};
	    this.length = 1;
	}
	else
	    throw new IllegalArgumentException(dir.getPath());
    }


    public boolean accept(File file){
        if (file.isFile()){
            return (file.getName().endsWith(".txt"));
        }
        return false;
    }
    public boolean hasNext(){
        return (this.index < this.length);
    }
    public File next(){
        if (this.index < this.length)
            return this.list[this.index++];
        else
            throw new java.util.NoSuchElementException();
    }
    public void remove(){
        throw new UnsupportedOperationException();
    }
    public java.util.Iterator<File> iterator(){
        return this;
    }



    public static void main(String[] argv){
        File dir = new File(System.getProperty("user.dir"));
        if (0 < argv.length){
            File test = new File(argv[0]);
            if (test.exists())
                dir = test;
            else {
                System.err.printf("Directory not found '%s'.%n",test.getPath());
                System.exit(1);
            }
        }
        try {
            int correct = 0, error = 0;

            Main main = new Main(dir);
            for (File test : main){
                Driver driver = new Driver(test);
                try {
                    Driver.Report report = driver.run();
                    correct += report.correct;
                    error += report.error;
                }
                catch (Exception exc){
                    exc.printStackTrace();
                    System.exit(1);
                }
            }
            System.out.printf("Failures %d, Successes %d%n",error,correct);
            System.exit(error);
        }
        catch (Exception exc){
            exc.printStackTrace();
            System.exit(1);
        }
    }
}
