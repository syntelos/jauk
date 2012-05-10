/*
 * Update
 * Copyright (C) 2009 John Pritchard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Accept source and target path expressions to update.  One or more
 * files in the source path are copied to one or more locations in the
 * target path.
 * 
 * Perform GIT or SVN file management as available and found at the
 * target location.
 * 
 * Perform version management for <code>"name-VERSION.jar"</code>
 * files.  Old versions are deleted, new versions are added on git and
 * subversion repositories -- for a numeric VERSION.  The VERSION
 * pattern is <code>[0-9.]+</code>.  The managed file name pattern is
 * very highly defined in order to prevent unintended consequences of
 * the feature under various applications.
 * 
 * @author jdp
 */
public class Update 
    extends Object
    implements java.io.FileFilter
{

    public static void usage(){
        System.err.println("Usage");
        System.err.println();
        System.err.println("  Update source[':'source]* target[':'target]*");
        System.err.println();
        System.err.println("Description");
        System.err.println();
        System.err.println("  Copy one or more source files into the");
        System.err.println("  target directories.  Delete old versions.");
        System.err.println();
        System.err.println("  For no targets found on the cmd line, exit");
        System.err.println("  silently.");
        System.err.println();
    }
    public static void main(String[] argv){
        if (1 < argv.length){

            File[] sources = Source(argv[0]);

            if (null != sources){

                for (File src: sources){

                    File[] targets = Target(src,argv);

                    if (null != targets){
			if (Debug){
			    try {
				for (File tgt: targets){
				    /*
				     * Copy source to target
				     */
				    System.out.printf("+ copy '%s' to '%s' in '%s'%n",src.getPath(),tgt.getName(),tgt.getParentFile().getPath());
				    /*
				     * Delete old versions
				     */
				    File[] deletes = ListDeletes(src,tgt);
				    if (null != deletes){

					for (File del: deletes){

					    if (DeleteFile(del))
						System.out.printf("Deleted %s\n",del.getPath());
					    else
						System.out.printf("Failed to delete %s\n",del.getPath());
					}
				    }
				    /*
				     * Add new versions
				     */
				    if (AddFile(tgt))
					System.out.printf("Added %s\n",tgt.getPath());
				    else
					System.out.printf("Modified %s\n",tgt.getPath());
				}
			    }
			    catch (Exception exc){
				exc.printStackTrace();
				System.exit(1);
			    }
			}
			else {
			    final long srclen = src.length();
			    try {
				FileChannel source = new FileInputStream(src).getChannel();
				try {
				    for (File tgt: targets){
					/*
					 * Copy source to target
					 */
					FileChannel target = new FileOutputStream(tgt).getChannel();
					try {
					    source.transferTo(0L,srclen,target);
					}
					finally {
					    target.close();
					}
					/*
					 * Delete old versions
					 */
					File[] deletes = ListDeletes(src,tgt);
					if (null != deletes){

					    for (File del: deletes){

						if (DeleteFile(del))
						    System.out.printf("Deleted %s\n",del.getPath());
						else
						    System.out.printf("Failed to delete %s\n",del.getPath());
					    }
					}
					/*
					 * Add new versions
					 */
					if (AddFile(tgt))
					    System.out.printf("Added %s\n",tgt.getPath());
					else
					    System.out.printf("Modified %s\n",tgt.getPath());
				    }
				}
				finally {
				    source.close();
				}
			    }
			    catch (Exception exc){
				exc.printStackTrace();
				System.exit(1);
			    }
			}
                    }
                }
                System.exit(0);
            }
            else {
                System.err.printf("Source file(s) not found in '%s'\n",argv[0]);
                System.exit(1);
            }
        }
        else {
            usage();
            System.exit(1);
        }
    }


    private final static boolean Debug = (null != System.getProperty("Debug"));


    private final static String Svn, Git, UserHome;
    static {
        String svn = null, git = null, rm = null;
        try {
            StringTokenizer strtok = new StringTokenizer(System.getenv("PATH"),File.pathSeparator);
	    File chk;
            while (strtok.hasMoreTokens()){
                String pel = strtok.nextToken();
                chk = new File(pel,"svn");
                if (chk.isFile() && chk.canExecute()){
                    svn = chk.getPath();
		    if (null != git && null != rm)
			break;
                }
                else {
                    chk = new File(pel,"svn.exe");
                    if (chk.isFile() && chk.canExecute()){
                        svn = chk.getPath();
                        if (null != git && null != rm)
                            break;
                    }
                }
		chk = new File(pel,"git");
                if (chk.isFile() && chk.canExecute()){
                    git = chk.getPath();
		    if (null != svn && null != rm)
			break;
                }
                else {
                    chk = new File(pel,"git.exe");
                    if (chk.isFile() && chk.canExecute()){
                        git = chk.getPath();
                        if (null != svn && null != rm)
                            break;
                    }
                }
            }
        }
        catch (Exception exc){
            throw new InternalError();
        }
        Svn = svn;
        Git = git;
	UserHome = System.getProperty("user.home");
    }
    private final static File UserHomeDir = new File(UserHome);

    public final static boolean HaveSvn = (null != Svn);
    public final static boolean HaveGit = (null != Git);

    static {
        if (HaveSvn){
	    if (HaveGit){
		if (Debug)
		    System.err.println("Using git and subversion.  Using Debug (Dry Run).");
		else
		    System.err.println("Using git and subversion.");
	    }
	    else {
		if (Debug)
		    System.err.println("Using subversion but not git.  Using Debug (Dry Run).");
		else
		    System.err.println("Using subversion but not git.");
	    }
	}
	else if (HaveGit){
	    if (Debug)
		System.err.println("Using git but not subversion.  Using Debug (Dry Run).");
	    else
		System.err.println("Using git but not subversion.");
	}
        else {
	    if (Debug)
		System.err.println("Not using either of subversion or git.  Using Debug (Dry Run).");
	    else
		System.err.println("Not using either of subversion or git.");
	}
    }

    private final static Runtime RT = Runtime.getRuntime();

    private final static boolean DeleteFile(File file)
        throws java.io.IOException,
               java.lang.InterruptedException
    {
	if (file.isFile()){
	    if (IsSvnRepo(file)){
		if (SvnContains(file))
		    return SvnDelete(file);
		else if (Debug){
		    System.out.printf("+ delete %s in %s%n",file.getName(),file.getParentFile().getPath());
		    return true;
		}
		else
		    return file.delete();
	    }
	    else if (GitContains(file))
		return GitDelete(file);
	    else if (Debug){
		System.out.printf("+ delete %s in %s%n",file.getName(),file.getParentFile().getPath());
		return true;
	    }
	    else
		return file.delete();
	}
	else
	    return false;
    }
    private final static boolean SvnDelete(File file)
        throws java.io.IOException,
               java.lang.InterruptedException
    {
        if (Update.HaveSvn){
            String[] cmd = new String[]{
                Update.Svn, "delete", "--force", file.getName()
            };

            if (Debug){
                System.out.printf("+ svn delete --force %s in %s%n",file.getName(),file.getParentFile().getPath());

                return true;
            }
            else {
                Process p = RT.exec(cmd,ENV,file.getParentFile());
                if (0 == p.waitFor()){

                    return true;
                }
                else {
                    System.out.printf("| svn delete --force %s in %s%n",file.getName(),file.getParentFile().getPath());
                    Copy(p.getInputStream(),System.out);
                    Copy(p.getErrorStream(),System.err);
                    return false;
                }
            }
        }
        else
            return false;
    }
    private final static boolean GitDelete(File file)
        throws java.io.IOException,
               java.lang.InterruptedException
    {
        if (Update.HaveGit){
            String[] cmd = new String[]{
                Update.Git, "rm", "-f", file.getName()
            };

            if (Debug){
                System.out.printf("+ git rm -f %s in %s%n",file.getName(),file.getParentFile().getPath());

                return true;
            }
            else {
                Process p = RT.exec(cmd,ENV,file.getParentFile());
                if (0 == p.waitFor()){

                    return true;
                }
                else {
                    System.out.printf("| git rm -f %s in %s%n",file.getName(),file.getParentFile().getPath());
                    Copy(p.getInputStream(),System.out);
                    Copy(p.getErrorStream(),System.err);
                    return false;
                }
            }
        }
        else
            return false;
    }
    private final static boolean AddFile(File file)
        throws java.io.IOException,
               java.lang.InterruptedException
    {
	if (Update.HaveSvn && IsSvnRepo(file)){
	    if (SvnContains(file))
		return false;
	    else
		return SvnAdd(file);
	}
	else if (Update.HaveGit && IsGitRepo(file)){
	    if (GitContains(file))
		return false;
	    else
		return GitAdd(file);
	}
	else
	    return false;
    }
    private final static boolean SvnAdd(File file)
        throws java.io.IOException,
               java.lang.InterruptedException
    {
        if (Update.HaveSvn){
            String[] cmd = new String[]{
                Update.Svn, "add", file.getName()
            };

            if (Debug){
                System.out.printf("+ svn add %s in %s%n",file.getName(),file.getParentFile().getPath());

                return true;
            }
            else {
                Process p = RT.exec(cmd,ENV,file.getParentFile());
                if (0 == p.waitFor()){

                    return true;
                }
                else {
                    System.out.printf("| svn add %s in %s%n",file.getName(),file.getParentFile().getPath());
                    Copy(p.getInputStream(),System.out);
                    Copy(p.getErrorStream(),System.err);
                    return false;
                }
            }
        }
        else
            return false;
    }
    private final static boolean GitAdd(File file)
        throws java.io.IOException,
               java.lang.InterruptedException
    {
        if (Update.HaveGit){
            String[] cmd = new String[]{
                Update.Git, "add", file.getName()
            };

            if (Debug){
                System.out.printf("+ git add %s in %s%n",file.getName(),file.getParentFile().getPath());

                return true;
            }
            else {
                Process p = RT.exec(cmd,ENV,file.getParentFile());
                if (0 == p.waitFor()){

                    return true;
                }
                else {
                    System.out.printf("| git add %s in %s%n",file.getName(),file.getParentFile().getPath());
                    Copy(p.getInputStream(),System.out);
                    Copy(p.getErrorStream(),System.err);
                    return false;
                }
            }
        }
        else
            return false;
    }
    private final static boolean IsSvnRepo(File file){
	File dir = new File(file.getParentFile(),".svn");
	return (dir.isDirectory());
    }
    private final static boolean IsGitRepo(File file){
	if (null != UserHome && file.getPath().startsWith(UserHome)){
	    File parent = file.getParentFile();
	    while (null != parent && (!parent.equals(UserHomeDir))){
		File dir = new File(parent,".git");
		if (dir.isDirectory())
		    return true;
		else {
		    parent = parent.getParentFile();
		}
	    }
	}
	return false;
    }
    private final static boolean SvnContains(File file)
        throws java.io.IOException,
               java.lang.InterruptedException
    {
        if (Update.HaveSvn){
            String[] cmd = new String[]{
                Update.Svn, "diff", file.getName()
            };

            Process p = RT.exec(cmd,ENV,file.getParentFile());
            if (0 == p.waitFor()){

                return true;
            }
            else {
                return false;
            }
        }
        else
            return false;
    }
    private final static boolean GitContains(File file)
        throws java.io.IOException,
               java.lang.InterruptedException
    {
        if (Update.HaveGit){
            String[] cmd = new String[]{
                Update.Git, "ls-files", "--others"
            };

            Process p = RT.exec(cmd,ENV,file.getParentFile());
            if (0 == p.waitFor()){
                /*
                 * Is a git repo
                 */
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                Copy(p.getInputStream(),buf);
                if (Contains(buf,file.getName()))
                    return false;
                else
                    return true;
            }
            else {
                /*
                 * Not a git repo
                 */
                return false;
            }
        }
        else
            return false;
    }
    private final static File[] Source(String path){
        File[] list = null;
        String[] files = path.split(":");
        for (int ccc = 0, ccz = files.length; ccc < ccz; ccc++){

            File tgt = new File(files[ccc]);

            if (tgt.isFile()){
                list = Add(list,tgt);
            }
        }
        return list;
    }
    private final static File[] Target(File src, String[] argv){
        File[] list = null;
        for (int cc = 1, count = argv.length; cc < count; cc++){
            String[] files = argv[cc].split(":");
            for (int ccc = 0, ccz = files.length; ccc < ccz; ccc++){

		String tgts = files[ccc];
                File tgt;

		if (tgts.startsWith("${user.home}/"))
		    tgt = new File(UserHomeDir,tgts.substring(13));
		else
		    tgt = new File(tgts);

                if (tgt.isDirectory()){
                    tgt = new File(tgt,src.getName());
                }
		
		if (!tgt.getAbsolutePath().equals(src.getAbsolutePath()))
		    list = Add(list,tgt);
            }
        }
        return list;
    }
    private final static Pattern DeletesBasenameRe = Pattern.compile("-([kK][0-9]{2}[A-L][0-9]{2}|[0-9.]+)\\.jar");

    private final static File[] ListDeletes(File src, File tgt){
	File tgtd = tgt.getParentFile();
	if (null != tgtd){
	    String name = src.getName();
	    String basename = Basename(name);
	    if (name.equals(basename)){
		if (Debug)
		    System.out.printf("+ listing deletes, basename (%s) == name (%s)%n",basename,name);
		return null;
	    }
	    else
		return tgtd.listFiles(new Update(basename,name));
	}
	return null;
    }
    private final static String Basename(String name){
        String[] s = DeletesBasenameRe.split(name,0);
        if (null != s && 0 < s.length)
            return s[0];
        else
            throw new IllegalArgumentException(name);
    }
    private final static void Copy(InputStream in, OutputStream out){
        try {
            try {
                byte[] iob = new byte[128];
                int read;
                while (0 < (read = in.read(iob,0,128)))
                    out.write(iob,0,read);
            }
            finally {
                in.close();
            }
        }
        catch (IOException exc){
        }
    }
    public final static File[] Add(File[] list, File item){
        if (null == item)
            return list;
        else if (null == list)
            return new File[]{item};
        else {
            int len = list.length;
            File[] copier = new File[len+1];
            System.arraycopy(list,0,copier,0,len);
            copier[len] = item;
            return copier;
        }
    }
    private final static boolean Contains(ByteArrayOutputStream stdout, String name){
	return Contains(stdout.toByteArray(),name);
    }
    private final static boolean Contains(byte[] stdout, String name){
	if (null != stdout && 0 < stdout.length)
	    return Contains(new String(stdout,0,0,stdout.length),name);
	else
	    return false;
    }
    private final static boolean Contains(String stdout, String name){
	StringTokenizer strtok = new StringTokenizer(stdout,"\r\n");
	while (strtok.hasMoreTokens()){
	    if (name.equals(strtok.nextToken()))
		return true;
	}
	return false;
    }
    private final static String[] ENV = new String[0];


    /**
     * Delete files filter
     */
    public final String include, exclude;


    public Update(String include, String exclude){
        super();
        this.include = include;
        this.exclude = exclude;
	if (Debug)
	    System.out.printf("+ list deletes filter include (%s) exclude (%s)%n",include,exclude);
    }

    public boolean accept(File file){
	if (file.isFile()){
	    final String name = file.getName();
	    /*
	     * Files to delete
	     */
	    if (name.startsWith(this.include)){
		/*
		 */
		if (DeletesBasenameRe.matcher(name.substring(this.include.length())).matches()){
		    /*
		     */
		    if (!name.equals(this.exclude)){
			if (Debug)
			    System.out.printf("+ list deletes filter (%s)%n",file.getPath());

			return true;
		    }
		}
	    }
	}
	return false;
    }
}
