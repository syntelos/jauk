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

import lxl.ArrayList;
import lxl.List;
import lxl.Set;

import java.io.IOException;

/**
 * @author Anders Møller
 * @author John Pritchard
 */
public class RegExp
    extends StringParser
    implements Context, jauk.Pattern
{

    enum Kind {
        REGEXP_UNION,
            REGEXP_CONCATENATION,
            REGEXP_INTERSECTION,
            REGEXP_OPTIONAL,
            REGEXP_REPEAT,
            REGEXP_REPEAT_MIN,
            REGEXP_REPEAT_MINMAX,
            REGEXP_COMPLEMENT,
            REGEXP_CHAR,
            REGEXP_CHAR_RANGE,
            REGEXP_ANYCHAR,
            REGEXP_EMPTY,
            REGEXP_STRING,
            REGEXP_ANYSTRING,
            REGEXP_AUTOMATON,
            REGEXP_INTERVAL
            }
        

    public static final int INTERSECTION = 0x0001;
    public static final int COMPLEMENT = 0x0002;
    public static final int EMPTY = 0x0004;
    public static final int ANYSTRING = 0x0008;
    public static final int AUTOMATON = 0x0010;
    public static final int INTERVAL = 0x0020;
    public static final int ALL = 0xffff;
    public static final int NONE = 0x0000;

    /*
     * Provoke initialization 
     */
    static {
        NamedAutomata.Builtin.Init();
    }
    public final static void Init(){
    }


    private Kind kind;
    private RegExp exp1, exp2;
    private String s;
    private char c;
    private int min, max, digits;
    private char from, to;
    private int flags;
    private Context context;
    private Compiled compiled;


    public RegExp(String s){
        this(null, s, ALL);
    }
    public RegExp(Context context, String s){
        this(context,s,ALL);
    }
    public RegExp(Context context, String s, int flags){
        super(s);
        this.flags = flags;
        this.context = context;
        {
            RegExp e;
            if (s.length() == 0)
                e = MakeString(this,"");
            else {
                e = parseUnionExp();
                if (this.more())
                    throw new IllegalArgumentException("end-of-string expected at position " + pos);
            }
            this.kind = e.kind;
            this.exp1 = e.exp1;
            this.exp2 = e.exp2;
            this.s = e.s;
            this.c = e.c;
            this.min = e.min;
            this.max = e.max;
            this.digits = e.digits;
            this.from = e.from;
            this.to = e.to;
        }
    }
    /**
     * Context copy for subexpressions 
     */
    private RegExp(RegExp p){
        super(null);
        this.context = p.context;
    }


    public Compiled compile(){
        if (null == this.compiled)
            this.compiled = new Compiled(this.toAutomaton(),this.compileForTime());

        return this.compiled;
    }
    public Match apply(CharSequence s)  {

        return new Match(s, this.compile());
    }
    public Match apply(CharSequence s, int offset)  {

        return new Match(s, this.compile(), offset);
    }
    public Automaton toAutomaton(){
        return toAutomaton(true);
    }
    public Automaton toAutomaton(boolean minimize){

        switch (this.kind) {
        case REGEXP_UNION:{
            List<Automaton> list = new ArrayList<Automaton>();
            FindLeaves(exp1, Kind.REGEXP_UNION, list, minimize);
            FindLeaves(exp2, Kind.REGEXP_UNION, list, minimize);
            return BasicOperations.Union(list).minimize();
	}
        case REGEXP_CONCATENATION:{
            List<Automaton> list = new ArrayList<Automaton>();
            FindLeaves(exp1, Kind.REGEXP_CONCATENATION, list, minimize);
            FindLeaves(exp2, Kind.REGEXP_CONCATENATION, list, minimize);
            return BasicOperations.Concatenate(list).minimize();
	}
        case REGEXP_INTERSECTION:
            return exp1.toAutomaton(minimize).intersection(exp2.toAutomaton(minimize)).minimize();
        case REGEXP_OPTIONAL:
            return exp1.toAutomaton(minimize).optional().minimize();
        case REGEXP_REPEAT:
            return exp1.toAutomaton(minimize).repeat().minimize();
        case REGEXP_REPEAT_MIN:
            return exp1.toAutomaton(minimize).repeat(min).minimize();
        case REGEXP_REPEAT_MINMAX:
            return exp1.toAutomaton(minimize).repeat(min, max).minimize();
        case REGEXP_COMPLEMENT:
            return exp1.toAutomaton(minimize).complement().minimize();
        case REGEXP_CHAR:
            return BasicAutomata.MakeChar(c);
        case REGEXP_CHAR_RANGE:
            return BasicAutomata.MakeCharRange(from, to);
        case REGEXP_ANYCHAR:
            return BasicAutomata.MakeAnyChar();
        case REGEXP_EMPTY:
            return BasicAutomata.MakeEmpty(true);
        case REGEXP_STRING:
            return BasicAutomata.MakeString(s);
        case REGEXP_ANYSTRING:
            return BasicAutomata.MakeAnyString();
        case REGEXP_AUTOMATON:
            return this.getAutomaton(s).clone();
        case REGEXP_INTERVAL:
            return BasicAutomata.MakeInterval(min, max, digits);
	default:
	    throw new Error(this.kind.name());
        }
    }
    /**
     * @see Context
     */
    public boolean compileForTime(){
        if (null != this.context)
            return this.context.compileForTime();
        else
            return true;
    }
    /**
     * @see NamedAutomata
     */
    public boolean isAutomaton(String name){
        if (null != this.context)
            return this.context.isAutomaton(name);
        else
            return NamedAutomata.Builtin.Instance.isAutomaton(name);
    }
    /**
     * @see NamedAutomata
     */
    public Automaton getAutomaton(String name){
        if (null != this.context)
            return this.context.getAutomaton(name);
        else 
            return NamedAutomata.Builtin.Instance.getAutomaton(name);
    }
    public String toString() {
        return toStringBuilder(new StringBuilder()).toString();
    }
    protected StringBuilder toStringBuilder(StringBuilder b) {
        switch (kind) {
        case REGEXP_UNION:
            b.append("(");
            exp1.toStringBuilder(b);
            b.append("|");
            exp2.toStringBuilder(b);
            b.append(")");
            break;
        case REGEXP_CONCATENATION:
            exp1.toStringBuilder(b);
            exp2.toStringBuilder(b);
            break;
        case REGEXP_INTERSECTION:
            b.append("(");
            exp1.toStringBuilder(b);
            b.append("&");
            exp2.toStringBuilder(b);
            b.append(")");
            break;
        case REGEXP_OPTIONAL:
            b.append("(");
            exp1.toStringBuilder(b);
            b.append(")?");
            break;
        case REGEXP_REPEAT:
            b.append("(");
            exp1.toStringBuilder(b);
            b.append(")*");
            break;
        case REGEXP_REPEAT_MIN:
            b.append("(");
            exp1.toStringBuilder(b);
            b.append("){").append(min).append(",}");
            break;
        case REGEXP_REPEAT_MINMAX:
            b.append("(");
            exp1.toStringBuilder(b);
            b.append("){").append(min).append(",").append(max).append("}");
            break;
        case REGEXP_COMPLEMENT:
            b.append("~(");
            exp1.toStringBuilder(b);
            b.append(")");
            break;
        case REGEXP_CHAR:
            b.append("\\").append(c);
            break;
        case REGEXP_CHAR_RANGE:
            b.append("[\\").append(from).append("-\\").append(to).append("]");
            break;
        case REGEXP_ANYCHAR:
            b.append(".");
            break;
        case REGEXP_EMPTY:
            b.append("#");
            break;
        case REGEXP_STRING:
            b.append("\"").append(s).append("\"");
            break;
        case REGEXP_ANYSTRING:
            b.append("@");
            break;
        case REGEXP_AUTOMATON:
            b.append("<").append(s).append(">");
            break;
        case REGEXP_INTERVAL:
            String s1 = Integer.toString(min);
            String s2 = Integer.toString(max);
            b.append("<");
            if (digits > 0)
                for (int i = s1.length(); i < digits; i++)
                    b.append('0');
            b.append(s1).append("-");
            if (digits > 0)
                for (int i = s2.length(); i < digits; i++)
                    b.append('0');
            b.append(s2).append(">");
            break;
        }
        return b;
    }
    public Set<String> getIdentifiers() {

        return this.getIdentifiers(new Set<String>());
    }
    protected Set<String> getIdentifiers(Set<String> set) {
        switch (kind) {
        case REGEXP_UNION:
        case REGEXP_CONCATENATION:
        case REGEXP_INTERSECTION:
            exp1.getIdentifiers(set);
            exp2.getIdentifiers(set);
            break;
        case REGEXP_OPTIONAL:
        case REGEXP_REPEAT:
        case REGEXP_REPEAT_MIN:
        case REGEXP_REPEAT_MINMAX:
        case REGEXP_COMPLEMENT:
            exp1.getIdentifiers(set);
            break;
        case REGEXP_AUTOMATON:
            set.add(s);
            break;
        default:
            break;
        }
        return set;
    }
    private boolean check(int flag) {
        return (flags & flag) != 0;
    }
    protected final RegExp parseUnionExp() throws IllegalArgumentException {
        RegExp e = parseInterExp();
        if (match('|'))
            e = MakeUnion(e, parseUnionExp());
        return e;
    }
    protected final RegExp parseInterExp() throws IllegalArgumentException {
        RegExp e = parseConcatExp();
        if (check(INTERSECTION) && match('&'))
            e = MakeIntersection(e, parseInterExp());
        return e;
    }
    protected final RegExp parseConcatExp() throws IllegalArgumentException {
        RegExp e = parseRepeatExp();
        if (more() && !peek(")|") && (!check(INTERSECTION) || !peek("&")))
            e = MakeConcatenation(e, parseConcatExp());
        return e;
    }
    protected final RegExp parseRepeatExp() throws IllegalArgumentException {
        RegExp e = parseComplExp();
        while (peek("?*+{")) {
            if (match('?'))
                e = MakeOptional(e);
            else if (match('*'))
                e = MakeRepeat(e);
            else if (match('+'))
                e = MakeRepeat(e, 1);
            else if (match('{')) {
                int start = pos;
                while (peek("0123456789"))
                    next();
                if (start == pos)
                    throw new IllegalArgumentException("integer expected at position " + pos);
                else {
                    int n = Integer.parseInt(this.substring(start, pos));
                    int m = -1;
                    if (match(',')) {
                        start = pos;
                        while (peek("0123456789"))
                            next();
                        if (start != pos)
                            m = Integer.parseInt(this.substring(start, pos));
                    }
                    else {
                        m = n;
                    }
                    if (!match('}'))
                        throw new IllegalArgumentException("expected '}' at position " + pos);
                    else if (m == -1)
                        e = MakeRepeat(e, n);
                    else
                        e = MakeRepeat(e, n, m);
                }
            }
        }
        return e;
    }
    protected final RegExp parseComplExp() throws IllegalArgumentException {
        if (check(COMPLEMENT) && match('~'))
            return MakeComplement(parseComplExp());
        else
            return parseCharClassExp();
    }
    protected final RegExp parseCharClassExp() throws IllegalArgumentException {
        if (match('[')) {
            boolean negate = false;
            if (match('^'))
                negate = true;
            RegExp e = parseCharClasses();
            if (negate)
                e = MakeIntersection(MakeAnyChar(this), MakeComplement(e));
            if (!match(']'))
                throw new IllegalArgumentException("expected ']' at position " + pos);
            return e;
        } else
            return parseSimpleExp();
    }
    protected final RegExp parseCharClasses() throws IllegalArgumentException {
        RegExp e = parseCharClass();
        while (more() && !peek("]"))
            e = MakeUnion(e, parseCharClass());
        return e;
    }
    protected final RegExp parseCharClass() throws IllegalArgumentException {
        char c = parseCharExp();
        if (match('-'))
            if (peek("]"))
                return MakeUnion(MakeChar(this,c), MakeChar(this,'-'));
            else
                return MakeCharRange(this, c, parseCharExp());
        else
            return MakeChar(this,c);
    }
    protected final RegExp parseSimpleExp() throws IllegalArgumentException {
        if (match('.'))
            return MakeAnyChar(this);
        else if (check(EMPTY) && match('#'))
            return MakeEmpty(this);
        else if (check(ANYSTRING) && match('@'))
            return MakeAnyString(this);
        else if (match('"')) {
            int start = pos;
            while (more() && !peek("\""))
                next();
            if (!match('"'))
                throw new IllegalArgumentException("expected '\"' at position " + pos);
            else
                return MakeString(this,this.substring(start, pos - 1));
        }
        else if (match('(')) {
            if (match(')'))
                return MakeString(this,"");
            else {
                RegExp e = parseUnionExp();
                if (!match(')'))
                    throw new IllegalArgumentException("expected ')' at position " + pos);
                else
                    return e;
            }
        }
        else if ((check(AUTOMATON) || check(INTERVAL)) && match('<')) {
            int start = pos;
            while (more() && !peek(">")){
                next();
            }
            if (!match('>'))
                throw new IllegalArgumentException("expected '>' at position " + pos);
            else {
                String s = this.substring(start, pos - 1);
                int i = s.indexOf('-');
                if (i == -1) {
                    if (!check(AUTOMATON))
                        throw new IllegalArgumentException("interval syntax error at position " + (pos - 1));
                    else
                        return MakeAutomaton(this,s);
                }
                else {
                    if (!check(INTERVAL))
                        throw new IllegalArgumentException("illegal identifier at position " + (pos - 1));
                    try {
                        if (i == 0 || i == s.length() - 1 || i != s.lastIndexOf('-'))
                            throw new NumberFormatException();
                        String smin = s.substring(0, i);
                        String smax = s.substring(i + 1, s.length());
                        int imin = Integer.parseInt(smin);
                        int imax = Integer.parseInt(smax);
                        int digits;
                        if (smin.length() == smax.length())
                            digits = smin.length();
                        else
                            digits = 0;
                        if (imin > imax) {
                            int t = imin;
                            imin = imax;
                            imax = t;
                        }
                        return MakeInterval(this, imin, imax, digits);
                    }
                    catch (NumberFormatException e) {
                        throw new IllegalArgumentException("interval syntax error at position " + (pos - 1));
                    }
                }
            }
        }
        else
            return MakeChar(this,parseCharExp());
    }
    protected final char parseCharExp() throws IllegalArgumentException {
        match('\\');
        return next();
    }

    private static void FindLeaves(RegExp exp, Kind kind, List<Automaton> list, boolean minimize)
    {
        if (exp.kind == kind) {
            FindLeaves(exp.exp1, kind, list, minimize);
            FindLeaves(exp.exp2, kind, list, minimize);
        }
	else
            list.add(exp.toAutomaton(minimize));
    }
    protected static RegExp MakeUnion(RegExp exp1, RegExp exp2) {
        RegExp r = new RegExp(exp1);
        r.kind = Kind.REGEXP_UNION;
        r.exp1 = exp1;
        r.exp2 = exp2;
        return r;
    }
    protected static RegExp MakeConcatenation(RegExp exp1, RegExp exp2) {
        if ((exp1.kind == Kind.REGEXP_CHAR || exp1.kind == Kind.REGEXP_STRING) && 
            (exp2.kind == Kind.REGEXP_CHAR || exp2.kind == Kind.REGEXP_STRING))
            {
                return MakeString(exp1, exp2);
            }
        else {
            RegExp r = new RegExp(exp1);
            r.kind = Kind.REGEXP_CONCATENATION;
            if (exp1.kind == Kind.REGEXP_CONCATENATION && 
                (exp1.exp2.kind == Kind.REGEXP_CHAR || exp1.exp2.kind == Kind.REGEXP_STRING) && 
                (exp2.kind == Kind.REGEXP_CHAR || exp2.kind == Kind.REGEXP_STRING))
                {
                    r.exp1 = exp1.exp1;
                    r.exp2 = MakeString(exp1.exp2, exp2);
                }
            else if ((exp1.kind == Kind.REGEXP_CHAR || exp1.kind == Kind.REGEXP_STRING) && 
                     exp2.kind == Kind.REGEXP_CONCATENATION && 
                     (exp2.exp1.kind == Kind.REGEXP_CHAR || exp2.exp1.kind == Kind.REGEXP_STRING))
                {
                    r.exp1 = MakeString(exp1, exp2.exp1);
                    r.exp2 = exp2.exp2;
                }
            else {
                r.exp1 = exp1;
                r.exp2 = exp2;
            }
            return r;
        }
    }
    private static RegExp MakeString(RegExp exp1, RegExp exp2) {
        StringBuilder b = new StringBuilder();
        if (exp1.kind == Kind.REGEXP_STRING)
            b.append(exp1.s);
        else
            b.append(exp1.c);
        if (exp2.kind == Kind.REGEXP_STRING)
            b.append(exp2.s);
        else
            b.append(exp2.c);
        return MakeString(exp1,b.toString());
    }
    protected static RegExp MakeIntersection(RegExp exp1, RegExp exp2) {
        RegExp r = new RegExp(exp1);
        r.kind = Kind.REGEXP_INTERSECTION;
        r.exp1 = exp1;
        r.exp2 = exp2;
        return r;
    }
    protected static RegExp MakeOptional(RegExp exp) {
        RegExp r = new RegExp(exp);
        r.kind = Kind.REGEXP_OPTIONAL;
        r.exp1 = exp;
        return r;
    }
    protected static RegExp MakeRepeat(RegExp exp) {
        RegExp r = new RegExp(exp);
        r.kind = Kind.REGEXP_REPEAT;
        r.exp1 = exp;
        return r;
    }
    protected static RegExp MakeRepeat(RegExp exp, int min) {
        RegExp r = new RegExp(exp);
        r.kind = Kind.REGEXP_REPEAT_MIN;
        r.exp1 = exp;
        r.min = min;
        return r;
    }
    protected static RegExp MakeRepeat(RegExp exp, int min, int max) {
        RegExp r = new RegExp(exp);
        r.kind = Kind.REGEXP_REPEAT_MINMAX;
        r.exp1 = exp;
        r.min = min;
        r.max = max;
        return r;
    }
    protected static RegExp MakeComplement(RegExp exp) {
        RegExp r = new RegExp(exp);
        r.kind = Kind.REGEXP_COMPLEMENT;
        r.exp1 = exp;
        return r;
    }
    protected static RegExp MakeChar(RegExp context, char c) {
        RegExp r = new RegExp(context);
        r.kind = Kind.REGEXP_CHAR;
        r.c = c;
        return r;
    }
    protected static RegExp MakeCharRange(RegExp context, char from, char to) {
        RegExp r = new RegExp(context);
        r.kind = Kind.REGEXP_CHAR_RANGE;
        r.from = from;
        r.to = to;
        return r;
    }
    protected static RegExp MakeAnyChar(RegExp context) {
        RegExp r = new RegExp(context);
        r.kind = Kind.REGEXP_ANYCHAR;
        return r;
    }
    protected static RegExp MakeEmpty(RegExp context) {
        RegExp r = new RegExp(context);
        r.kind = Kind.REGEXP_EMPTY;
        return r;
    }
    protected static RegExp MakeString(RegExp context, String s) {
        RegExp r = new RegExp(context);
        r.kind = Kind.REGEXP_STRING;
        r.s = s;
        return r;
    }
    protected static RegExp MakeAnyString(RegExp context) {
        RegExp r = new RegExp(context);
        r.kind = Kind.REGEXP_ANYSTRING;
        return r;
    }
    protected static RegExp MakeAutomaton(RegExp context, String s) {
        RegExp r = new RegExp(context);
        r.kind = Kind.REGEXP_AUTOMATON;
        r.s = s;
        return r;
    }
    protected static RegExp MakeInterval(RegExp context, int min, int max, int digits) {
        RegExp r = new RegExp(context);
        r.kind = Kind.REGEXP_INTERVAL;
        r.min = min;
        r.max = max;
        r.digits = digits;
        return r;
    }
}
