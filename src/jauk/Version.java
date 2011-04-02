package jauk;

public final class Version
    extends Object
{
    public final static String Name = "jauk";
    public final static int Major   =  0;
    public final static int Minor   =  6;
    public final static int Build   =  6;


    public final static String Number = String.valueOf(Major)+'.'+String.valueOf(Minor);

    public final static String Full = Name+'-'+Number;

    private Version(){
        super();
    }
}
