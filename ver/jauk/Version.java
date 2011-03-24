package jauk;

public final class Version
    extends Object
{
    public final static String Name = "@VersionName@";
    public final static int Major   =  @VersionMajor@;
    public final static int Minor   =  @VersionMinor@;
    public final static int Build   =  @VersionBuild@;


    public final static String Number = String.valueOf(Major)+'.'+String.valueOf(Minor);

    public final static String Full = Name+'-'+Number;

    private Version(){
        super();
    }
}
