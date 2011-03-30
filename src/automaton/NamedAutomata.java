/*
 * automaton
 * 
 * Copyright (c) 2001-2011 Anders Moeller
 * Copyright (c) 2011 John Pritchard
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package automaton;

/**
 * Named automata.
 * @see RegExp
 * @author John Pritchard
 */
public interface NamedAutomata {

    /**
     * @return Known name, able to return.
     */
    public boolean isAutomaton(String name);
    /**
     * @return Must be a non null, cloneable automaton.  Otherwise
     * throw an illegal argument exception.
     */
    public Automaton getAutomaton(String name);


    /**
     * An implementation of Named Automata permitting additional
     * scopes as inferior or superior.
     */
    public static class Basic
        extends java.util.HashMap<String,Automaton>
        implements NamedAutomata
    {
        protected final NamedAutomata map;
        protected final boolean mapSuperior;


        public Basic(){
            this(null,true);
        }
        public Basic(NamedAutomata map, boolean superior){
            super();
            if (null != map){
                this.map = map;
                this.mapSuperior = superior;
            }
            else {
                this.map = null;
                this.mapSuperior = false;
            }
        }
        public Basic(NamedAutomata map, boolean superior, String[][] fill){
            this(map,superior);
            if (null != fill){
                for (String[] nvpair: fill){
                    if (null != nvpair && 2 == nvpair.length){
                        String name = nvpair[0];
                        String regx = nvpair[1];
                        this.put(name,new RegExp(regx).toAutomaton());
                    }
                }
            }
        }
        public Basic(NamedAutomata map, boolean superior, Object[][] fill){
            this(map,superior);
            if (null != fill){
                for (Object[] nvpair: fill){
                    if (null != nvpair && 2 == nvpair.length){
                        String name = (String)nvpair[0];
                        Automaton aut = (Automaton)nvpair[1];
                        this.put(name,aut);
                    }
                }
            }
        }


        public boolean isAutomaton(String name){
            if (null != this.map)
                return (this.containsKey(name) || this.map.isAutomaton(name));
            else
                return this.containsKey(name);
        }
        public Automaton getAutomaton(String name){
            if (null != this.map){

                if (this.mapSuperior){
                    if (this.map.isAutomaton(name))
                        return this.map.getAutomaton(name);
                    else {
                        final Automaton automaton = this.get(name);
                        if (null != automaton)
                            return automaton;
                        else
                            throw new IllegalArgumentException(String.format("Automaton named '%s' not found.",name));
                    }
                }
                else {
                    final Automaton automaton = this.get(name);
                    if (null != automaton)
                        return automaton;
                    else
                        return this.map.getAutomaton(name);
                }
            }
            else {
                final Automaton automaton = this.get(name);
                if (null != automaton)
                    return automaton;
                else
                    throw new IllegalArgumentException(String.format("Automaton named '%s' not found.",name));
            }
        }
    }
    /**
     * An implementation of Named Automata with a few of the automata
     * defined in the original automaton package.
     */
    public static class Builtin
        extends Basic
    {
        /*
         * Assign
         */
        public final static NamedAutomata Instance = new Builtin();
        /*
         * Self referencing operatations
         */
        static {
            ((Builtin)Instance).init();
        }
        /**
         * Provoke class initialization
         */
        public final static NamedAutomata Init(){
            return Instance;
        }


        public Builtin(){
            super();
        }


        protected void init(){

            this.put("Extender",(new RegExp("[\u3031-\u3035\u309D-\u309E\u30FC-\u30FE\u00B7\u02D0\u02D1\u0387\u0640\u0E46\u0EC6\u3005]")).toAutomaton());
            this.put("CombiningChar",(new RegExp("[\u0300-\u0345\u0360-\u0361\u0483-\u0486\u0591-\u05A1\u05A3-\u05B9\u05BB-\u05BD\u05C1-\u05C2\u064B-\u0652" +
                                                 "\u06D6-\u06DC\u06DD-\u06DF\u06E0-\u06E4\u06E7-\u06E8\u06EA-\u06ED\u0901-\u0903\u093E-\u094C\u0951-\u0954" +
                                                 "\u0962-\u0963\u0981-\u0983\u09C0-\u09C4\u09C7-\u09C8\u09CB-\u09CD\u09E2-\u09E3\u0A40-\u0A42\u0A47-\u0A48" +
                                                 "\u0A4B-\u0A4D\u0A70-\u0A71\u0A81-\u0A83\u0ABE-\u0AC5\u0AC7-\u0AC9\u0ACB-\u0ACD\u0B01-\u0B03\u0B3E-\u0B43" +
                                                 "\u0B47-\u0B48\u0B4B-\u0B4D\u0B56-\u0B57\u0B82-\u0B83\u0BBE-\u0BC2\u0BC6-\u0BC8\u0BCA-\u0BCD\u0C01-\u0C03" +
                                                 "\u0C3E-\u0C44\u0C46-\u0C48\u0C4A-\u0C4D\u0C55-\u0C56\u0C82-\u0C83\u0CBE-\u0CC4\u0CC6-\u0CC8\u0CCA-\u0CCD" +
                                                 "\u0CD5-\u0CD6\u0D02-\u0D03\u0D3E-\u0D43\u0D46-\u0D48\u0D4A-\u0D4D\u0E34-\u0E3A\u0E47-\u0E4E\u0EB4-\u0EB9" +
                                                 "\u0EBB-\u0EBC\u0EC8-\u0ECD\u0F18-\u0F19\u0F71-\u0F84\u0F86-\u0F8B\u0F90-\u0F95\u0F99-\u0FAD\u0FB1-\u0FB7" +
                                                 "\u20D0-\u20DC\u302A-\u302F\u05BF\u05C4\u0670\u093C\u094D\u09BC\u09BE\u09BF\u09D7\u0A02\u0A3C\u0A3E\u0A3F" +
                                                 "\u0ABC\u0B3C\u0BD7\u0D57\u0E31\u0EB1\u0F35\u0F37\u0F39\u0F3E\u0F3F\u0F97\u0FB9\u20E1\u3099\u309A]")).toAutomaton());
            this.put("Digit",(new RegExp("[\u0030-\u0039\u0660-\u0669\u06F0-\u06F9\u0966-\u096F\u09E6-\u09EF\u0A66-\u0A6F\u0AE6-\u0AEF\u0B66-\u0B6F" + 
                                         "\u0BE7-\u0BEF\u0C66-\u0C6F\u0CE6-\u0CEF\u0D66-\u0D6F\u0E50-\u0E59\u0ED0-\u0ED9\u0F20-\u0F29]")).toAutomaton());
            this.put("Ideographic",(new RegExp("[\u4E00-\u9FA5\u3021-\u3029\u3007]")).toAutomaton());
            this.put("BaseChar",(new RegExp("[\u0041-\u005A\u0061-\u007A\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u00FF\u0100-\u0131\u0134-\u013E\u0141-\u0148" + 
                                            "\u014A-\u017E\u0180-\u01C3\u01CD-\u01F0\u01F4-\u01F5\u01FA-\u0217\u0250-\u02A8\u02BB-\u02C1\u0388-\u038A" +
                                            "\u038E-\u03A1\u03A3-\u03CE\u03D0-\u03D6\u03E2-\u03F3\u0401-\u040C\u040E-\u044F\u0451-\u045C\u045E-\u0481" +
                                            "\u0490-\u04C4\u04C7-\u04C8\u04CB-\u04CC\u04D0-\u04EB\u04EE-\u04F5\u04F8-\u04F9\u0531-\u0556\u0561-\u0586" +
                                            "\u05D0-\u05EA\u05F0-\u05F2\u0621-\u063A\u0641-\u064A\u0671-\u06B7\u06BA-\u06BE\u06C0-\u06CE\u06D0-\u06D3" +
                                            "\u06E5-\u06E6\u0905-\u0939\u0958-\u0961\u0985-\u098C\u098F-\u0990\u0993-\u09A8\u09AA-\u09B0\u09B6-\u09B9" +
                                            "\u09DC-\u09DD\u09DF-\u09E1\u09F0-\u09F1\u0A05-\u0A0A\u0A0F-\u0A10\u0A13-\u0A28\u0A2A-\u0A30\u0A32-\u0A33" +
                                            "\u0A35-\u0A36\u0A38-\u0A39\u0A59-\u0A5C\u0A72-\u0A74\u0A85-\u0A8B\u0A8F-\u0A91\u0A93-\u0AA8\u0AAA-\u0AB0" +
                                            "\u0AB2-\u0AB3\u0AB5-\u0AB9\u0B05-\u0B0C\u0B0F-\u0B10\u0B13-\u0B28\u0B2A-\u0B30\u0B32-\u0B33\u0B36-\u0B39" +
                                            "\u0B5C-\u0B5D\u0B5F-\u0B61\u0B85-\u0B8A\u0B8E-\u0B90\u0B92-\u0B95\u0B99-\u0B9A\u0B9E-\u0B9F\u0BA3-\u0BA4" +
                                            "\u0BA8-\u0BAA\u0BAE-\u0BB5\u0BB7-\u0BB9\u0C05-\u0C0C\u0C0E-\u0C10\u0C12-\u0C28\u0C2A-\u0C33\u0C35-\u0C39" +
                                            "\u0C60-\u0C61\u0C85-\u0C8C\u0C8E-\u0C90\u0C92-\u0CA8\u0CAA-\u0CB3\u0CB5-\u0CB9\u0CE0-\u0CE1\u0D05-\u0D0C" +
                                            "\u0D0E-\u0D10\u0D12-\u0D28\u0D2A-\u0D39\u0D60-\u0D61\u0E01-\u0E2E\u0E32-\u0E33\u0E40-\u0E45\u0E81-\u0E82" +
                                            "\u0E87-\u0E88\u0E94-\u0E97\u0E99-\u0E9F\u0EA1-\u0EA3\u0EAA-\u0EAB\u0EAD-\u0EAE\u0EB2-\u0EB3\u0EC0-\u0EC4" +
                                            "\u0F40-\u0F47\u0F49-\u0F69\u10A0-\u10C5\u10D0-\u10F6\u1102-\u1103\u1105-\u1107\u110B-\u110C\u110E-\u1112" +
                                            "\u1154-\u1155\u115F-\u1161\u116D-\u116E\u1172-\u1173\u11AE-\u11AF\u11B7-\u11B8\u11BC-\u11C2\u1E00-\u1E9B" +
                                            "\u1EA0-\u1EF9\u1F00-\u1F15\u1F18-\u1F1D\u1F20-\u1F45\u1F48-\u1F4D\u1F50-\u1F57\u1F5F-\u1F7D\u1F80-\u1FB4" +
                                            "\u1FB6-\u1FBC\u1FC2-\u1FC4\u1FC6-\u1FCC\u1FD0-\u1FD3\u1FD6-\u1FDB\u1FE0-\u1FEC\u1FF2-\u1FF4\u1FF6-\u1FFC" +
                                            "\u212A-\u212B\u2180-\u2182\u3041-\u3094\u30A1-\u30FA\u3105-\u312C\uAC00-\uD7A3" +
                                            "\u0386\u038C\u03DA\u03DC\u03DE\u03E0\u0559\u06D5\u093D\u09B2\u0A5E\u0A8D\u0ABD\u0AE0\u0B3D\u0B9C\u0CDE\u0E30\u0E84\u0E8A" +
                                            "\u0E8D\u0EA5\u0EA7\u0EB0\u0EBD\u1100\u1109\u113C\u113E\u1140\u114C\u114E\u1150\u1159\u1163\u1165\u1167\u1169\u1175\u119E" +
                                            "\u11A8\u11AB\u11BA\u11EB\u11F0\u11F9\u1F59\u1F5B\u1F5D\u1FBE\u2126\u212E]")).toAutomaton());
            /*
             * Essential unicode character classes
             */
            this.put("Letter",(new RegExp("<BaseChar>|<Ideographic>")).toAutomaton());

            this.put("Char",(new RegExp("[\t\n\r\u0020-\uD7FF\ue000-\ufffd]|[\uD800-\uDBFF][\uDC00-\uDFFF]")).toAutomaton());
            /*
             * Whitespace (+ related)
             */
            this.put("_",(new RegExp("[ \t\n\r]*")).toAutomaton());

            this.put("S",(new RegExp("[ \t\n\r]")).toAutomaton());

            this.put("Newline",(new RegExp("[\n\r]")).toAutomaton());

            this.put("Line",(new RegExp("[^\n\r]*")).toAutomaton());
            /*
             * Convenience
             */
            this.put("CComment",(new RegExp("/\\*(~(\"\\*/\"))*\\*/")).toAutomaton());
            /*
             * ASCII (unicode basic & common)
             */
            this.put("UpAlpha",(new RegExp("[A-Z]")).toAutomaton());
            this.put("LowAlpha",(new RegExp("[a-z]")).toAutomaton());
            this.put("Alpha",(new RegExp("<LowAlpha>|<UpAlpha>")).toAutomaton());
            this.put("AlphaNum",(new RegExp("<Alpha>|<Digit>")).toAutomaton());
            this.put("Digit",(new RegExp("[0-9]")).toAutomaton());
            /*
             * Date & Time
             */
            this.put("Z",(new RegExp("[-+](<00-13>:<00-59>|14:00)|Z")).toAutomaton());
            this.put("Y",(new RegExp("(<Digit>{4,})&~(0000)")).toAutomaton());
            this.put("M",(new RegExp("<01-12>")).toAutomaton());
            this.put("D",(new RegExp("<01-31>")).toAutomaton());
            this.put("T",(new RegExp("<00-23>:<00-59>:<00-59>|24:00:00")).toAutomaton());

            this.put("Duration",(new RegExp("<_>(-?P(((<Digit>+Y)?(<Digit>+M)?(<Digit>+D)?(T(((<Digit>+H)?(<Digit>+M)?(<Digit>+(\\.<Digit>+)?S)?)&~()))?)&~()))<_>")).toAutomaton());
            this.put("DateTime",(new RegExp("<_>(-?<Y>-<M>-<Digit>T<T>(\\.<Digit>+)?<Z>?)<_>")).toAutomaton());
            this.put("Time",(new RegExp("<_>(<T>(\\.<Digit>+)?<Z>?)<_>")).toAutomaton());
            this.put("Date",(new RegExp("<_>(-?<Y>-<M>-<D><Z>?)<_>")).toAutomaton());
            this.put("YearMonth",(new RegExp("<_>(-?<Y>-<M><Z>?)<_>")).toAutomaton());
            this.put("Year",(new RegExp("<_>(-?<Y><Z>?)<_>")).toAutomaton());
            this.put("MonthDay",(new RegExp("<_>(--<M>-<D><Z>?)<_>")).toAutomaton());
            this.put("Day",(new RegExp("<_>(--<D><Z>?)<_>")).toAutomaton());
            this.put("Month",(new RegExp("<_>(--<M><Z>?)<_>")).toAutomaton());
            /*
             * Data Strings
             */
            this.put("Hex",(new RegExp("<Digit>|[a-f]|[A-F]")).toAutomaton());

            this.put("B64",(new RegExp("[A-Za-z0-9+/]")).toAutomaton());
            this.put("B16",(new RegExp("[AEIMQUYcgkosw048]")).toAutomaton());
            this.put("B04",(new RegExp("[AQgw]")).toAutomaton());
            this.put("B04S",(new RegExp("<B04> ?")).toAutomaton());
            this.put("B16S",(new RegExp("<B16> ?")).toAutomaton());
            this.put("B64S",(new RegExp("<B64> ?")).toAutomaton());

            this.put("HexBinary",(new RegExp("<_>([0-9a-fA-F]{2}*)<_>")).toAutomaton());
            this.put("B64Binary",(new RegExp("<_>(((<B64S><B64S><B64S><B64S>)*((<B64S><B64S><B64S><B64>)|(<B64S><B64S><B16S>=)|(<B64S><B04S>= ?=)))?)<_>")).toAutomaton());
            /*
             * Logical & Numeric Values
             */
            this.put("Boolean",(new RegExp("<_>(true|false|1|0)<_>")).toAutomaton());
            this.put("Decimal",(new RegExp("<_>([-+]?<Digit>+(\\.<Digit>+)?)<_>")).toAutomaton());
            this.put("Float",(new RegExp("<_>([-+]?<Digit>+(\\.<Digit>+)?([Ee][-+]?<Digit>+)?|INF|-INF|NaN)<_>")).toAutomaton());
            this.put("Integer",(new RegExp("<_>[-+]?[0-9]+<_>")).toAutomaton());

            this.put("NonPositiveInteger",(new RegExp("<_>(0+|-<Digit>+)<_>")).toAutomaton());
            this.put("NegativeInteger",(new RegExp("<_>(-[1-9]<Digit>*)<_>")).toAutomaton());
            this.put("NonNegativeInteger",(new RegExp("<_>(<Digit>+)<_>")).toAutomaton());
            this.put("PositiveInteger",(new RegExp("<_>([1-9]<Digit>*)<_>")).toAutomaton());

            final Basic Numeric = new Basic(this,true,new Object[][]{
                    {"UNSIGNEDLONG", BasicAutomata.MakeMaxInteger("18446744073709551615")},
                    {"UNSIGNEDINT", BasicAutomata.MakeMaxInteger("4294967295")},
                    {"UNSIGNEDSHORT", BasicAutomata.MakeMaxInteger("65535")},
                    {"UNSIGNEDBYTE", BasicAutomata.MakeMaxInteger("255")},
                    {"LONG", BasicAutomata.MakeMaxInteger("9223372036854775807")},
                    {"LONG_NEG", BasicAutomata.MakeMaxInteger("9223372036854775808")},
                    {"INT", BasicAutomata.MakeMaxInteger("2147483647")},
                    {"INT_NEG", BasicAutomata.MakeMaxInteger("2147483648")},
                    {"SHORT", BasicAutomata.MakeMaxInteger("32767")},
                    {"SHORT_NEG", BasicAutomata.MakeMaxInteger("32768")},
                    {"BYTE", BasicAutomata.MakeMaxInteger("127")},
                    {"BYTE_NEG", BasicAutomata.MakeMaxInteger("128")}
                });

            this.put("ULong",(new RegExp(Numeric,"<_><UNSIGNEDLONG><_>")).toAutomaton());
            this.put("UInt",(new RegExp(Numeric,"<_><UNSIGNEDINT><_>")).toAutomaton());
            this.put("UShort",(new RegExp(Numeric,"<_><UNSIGNEDSHORT><_>")).toAutomaton());
            this.put("UByte",(new RegExp(Numeric,"<_><UNSIGNEDBYTE><_>")).toAutomaton());
            this.put("Long",(new RegExp(Numeric,"<_>(<LONG>|-<LONG_NEG>)<_>")).toAutomaton());
            this.put("Int",(new RegExp(Numeric,"<_>(<INT>|-<INT_NEG>)<_>")).toAutomaton());
            this.put("Short",(new RegExp(Numeric,"<_>(<SHORT>|-<SHORT_NEG>)<_>")).toAutomaton());
            this.put("Byte",(new RegExp(Numeric,"<_>(<BYTE>|-<BYTE_NEG>)<_>")).toAutomaton());
        }
    }
}
