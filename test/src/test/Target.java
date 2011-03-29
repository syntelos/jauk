package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

/**
 * @see Driver
 * @see Source
 */
public class Target
    extends jauk.Scanner
{

    public final File file;
    public final int number;

    public Target(Source source, int num)
        throws IOException
    {
        this(Translate(source,num),num);
    }
    private Target(File file, int num)
        throws IOException
    {
        super(file);
        this.file = file;
        this.number = num;
    }


    public final static Target[] Add(Target[] list, Target item){
        if (null == item)
            return list;
        else if (null == list)
            return new Target[]{item};
        else {
            final int len = list.length;
            Target[] copier = new Target[len+1];
            System.arraycopy(list,0,copier,0,len);
            copier[len] = item;
            return copier;
        }
    }
    public final static File Translate(Source source, int num){
        String path = source.file.getPath();
        if (path.endsWith(".src")){
            path = path.substring(0,path.length()-3)+"tgt"+num;
            return new File(path);
        }
        else
            throw new IllegalArgumentException(path);
    }
}
