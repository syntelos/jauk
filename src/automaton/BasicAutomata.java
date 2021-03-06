/*
 * automaton
 * 
 * Copyright (c) 2001-2011 Anders Moeller
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
import lxl.Collection;
import lxl.Set;

import java.util.Arrays;

/**
 * @author Anders Møller
 */
public final class BasicAutomata {

    private static Automaton WS;

    public static Automaton Whitespace() {
        if (null == WS)
            WS = MakeCharSet(" \t\n\r").repeat().minimize();
        return WS;
    }
        
    public static Automaton MakeEmpty(boolean accept){

        return (new Automaton(new State(accept)));
    }
    public static Automaton MakeEmptyString() {

        return (new Automaton(""));
    }
    public static Automaton MakeAnyString()     {

        Automaton a = new Automaton(new State(true));

        a.initial.add(new Transition(Character.MIN_VALUE, Character.MAX_VALUE, a.initial));

        return a;
    }
    public static Automaton MakeAnyChar() {

        return MakeCharRange(Character.MIN_VALUE, Character.MAX_VALUE);
    }
    public static Automaton MakeChar(char c) {

        return (new Automaton(Character.toString(c)));
    }
    public static Automaton MakeCharRange(char min, char max) {
        if (min == max)
            return MakeChar(min);
        else {
            Automaton a = new Automaton();

            if (min <= max)
                a.initial.add(new Transition(min, max, new State(true)));

            return a;
        }
    }
    public static Automaton MakeCharSet(String set) {
        if (set.length() == 1)
            return MakeChar(set.charAt(0));
        else {
            Automaton a = new Automaton();
            State s1 = a.initial;
            State s2 = new State(true);

            for (int i = 0; i < set.length(); i++){
                s1.add(new Transition(set.charAt(i), s2));
            }
            return a.reduce();
        }
    }
    private static State anyOfRightLength(String x, int n) {
        State s = new State();
        if (x.length() == n)
            s.setAccept(true);
        else
            s.addTransition(new Transition('0', '9', anyOfRightLength(x, n + 1)));
        return s;
    }
    private static State atLeast(String x, int n, Collection<State> initials, boolean zeros) {
        State s = new State();
        if (x.length() == n)
            s.setAccept(true);
        else {
            if (zeros)
                initials.add(s);
            char c = x.charAt(n);
            s.addTransition(new Transition(c, atLeast(x, n + 1, initials, zeros && c == '0')));
            if (c < '9')
                s.addTransition(new Transition((char)(c + 1), '9', anyOfRightLength(x, n + 1)));
        }
        return s;
    }
    private static State atMost(String x, int n) {
        State s = new State();
        if (x.length() == n)
            s.setAccept(true);
        else {
            char c = x.charAt(n);
            s.addTransition(new Transition(c, atMost(x, (char)n + 1)));
            if (c > '0')
                s.addTransition(new Transition('0', (char)(c - 1), anyOfRightLength(x, n + 1)));
        }
        return s;
    }
    private static State between(String x, String y, int n, Collection<State> initials, boolean zeros) {
        State s = new State();
        if (x.length() == n)
            s.setAccept(true);
        else {
            if (zeros)
                initials.add(s);
            char cx = x.charAt(n);
            char cy = y.charAt(n);
            if (cx == cy)
                s.addTransition(new Transition(cx, between(x, y, n + 1, initials, zeros && cx == '0')));
            else { // cx<cy
                s.addTransition(new Transition(cx, atLeast(x, n + 1, initials, zeros && cx == '0')));
                s.addTransition(new Transition(cy, atMost(y, n + 1)));
                if (cx + 1 < cy)
                    s.addTransition(new Transition((char)(cx + 1), (char)(cy - 1), anyOfRightLength(x, n + 1)));
            }
        }
        return s;
    }
    public static Automaton MakeInterval(int min, int max, int digits) throws IllegalArgumentException {
        Automaton a = new Automaton();
        String x = Integer.toString(min);
        String y = Integer.toString(max);
        if (min > max || (digits > 0 && y.length() > digits))
            throw new IllegalArgumentException();
        int d;
        if (digits > 0)
            d = digits;
        else
            d = y.length();
        StringBuilder bx = new StringBuilder();
        for (int i = x.length(); i < d; i++)
            bx.append('0');
        bx.append(x);
        x = bx.toString();
        StringBuilder by = new StringBuilder();
        for (int i = y.length(); i < d; i++)
            by.append('0');
        by.append(y);
        y = by.toString();
        Collection<State> initials = new ArrayList<State>();
        a.initial = between(x, y, 0, initials, digits <= 0);
        if (digits <= 0) {
            ArrayList<StatePair> pairs = new ArrayList<StatePair>();
            for (State p : initials)
                if (a.initial != p)
                    pairs.add(new StatePair(a.initial, p));
            a.addEpsilons(pairs);
            a.initial.addTransition(new Transition('0', a.initial));
            a.deterministic = false;
        }
        else
            a.deterministic = true;
        a.checkMinimizeAlways();
        return a;
    }
    public static Automaton MakeString(String s) {

        return (new Automaton(s));
    }
    public static Automaton MakeStringUnion(CharSequence... strings) {
        if (strings.length == 0)
            return MakeEmpty(false);
        else {
            Arrays.sort(strings, StringUnionOperations.LEXICOGRAPHIC_ORDER);

            return (new Automaton(StringUnionOperations.build(strings)).reduce().recomputeHashCode());
        }
    }
    public static Automaton MakeMaxInteger(String n) {
        int i = 0;
        while (i < n.length() && n.charAt(i) == '0'){
            i++;
        }
        StringBuilder b = new StringBuilder();
        b.append("0*(0|");
        if (i < n.length()){
            b.append("[0-9]{1," + (n.length() - i - 1) + "}|");
        }
        MaxInteger(n.substring(i), 0, b);
        b.append(")");
        return (new RegExp(b.toString())).toAutomaton().minimize();
    }
    private static void MaxInteger(String n, int i, StringBuilder b) {
        b.append('(');
        if (i < n.length()) {
            char c = n.charAt(i);
            if (c != '0')
                b.append("[0-" + (char)(c-1) + "][0-9]{" + (n.length() - i - 1) + "}|");
            b.append(c);
            MaxInteger(n, i + 1, b);
        }
        b.append(')');
    }
    public static Automaton MakeMinInteger(String n) {
        int i = 0;
        while (i + 1 < n.length() && n.charAt(i) == '0')
            i++;
        StringBuilder b = new StringBuilder();
        b.append("0*");
        minInteger(n.substring(i), 0, b);
        b.append("[0-9]*");
        return (new RegExp(b.toString())).toAutomaton().minimize();
    }
    private static void minInteger(String n, int i, StringBuilder b) {
        b.append('(');
        if (i < n.length()) {
            char c = n.charAt(i);
            if (c != '9')
                b.append("[" + (char)(c+1) + "-9][0-9]{" + (n.length() - i - 1) + "}|");
            b.append(c);
            minInteger(n, i + 1, b);
        }
        b.append(')');
    }
    public static Automaton MakeTotalDigits(int i) {
        return ((new RegExp("[ \t\n\r]*[-+]?0*([0-9]{0," + i + "}|((([0-9]\\.*){0," + i + "})&@\\.@)0*)[ \t\n\r]*")).toAutomaton()).minimize();
    }
    public static Automaton MakeFractionDigits(int i) {
        return ((new RegExp("[ \t\n\r]*[-+]?[0-9]+(\\.[0-9]{0," + i + "}0*)?[ \t\n\r]*")).toAutomaton()).minimize();
    }
    public static Automaton MakeIntegerValue(String value) {
        boolean minus = false;
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c == '-')
                minus = true;
            if (c >= '1' && c <= '9')
                break;
            i++;
        }
        StringBuilder b = new StringBuilder();
        b.append(value.substring(i));
        if (b.length() == 0)
            b.append("0");
        Automaton s;
        if (minus)
            s = MakeChar('-');
        else
            s = MakeChar('+').optional();
        Automaton ws = Whitespace();
        return (ws.concatenate(s.concatenate(MakeChar('0').repeat()).concatenate(MakeString(b.toString()))).concatenate(ws)).minimize();
    }
    public static Automaton MakeDecimalValue(String value) {
        boolean minus = false;
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c == '-')
                minus = true;
            if ((c >= '1' && c <= '9') || c == '.')
                break;
            i++;
        }
        StringBuilder b1 = new StringBuilder();
        StringBuilder b2 = new StringBuilder();
        int p = value.indexOf('.', i);
        if (p == -1)
            b1.append(value.substring(i));
        else {
            b1.append(value.substring(i, p));
            i = value.length() - 1;
            while (i > p) {
                char c = value.charAt(i);
                if (c >= '1' && c <= '9')
                    break;
                i--;
            }
            b2.append(value.substring(p + 1, i + 1));
        }
        if (b1.length() == 0)
            b1.append("0");
        Automaton s;
        if (minus)
            s = MakeChar('-');
        else
            s = MakeChar('+').optional();
        Automaton d;
        if (b2.length() == 0)
            d = MakeChar('.').concatenate(MakeChar('0').repeat(1)).optional();
        else
            d = MakeChar('.').concatenate(MakeString(b2.toString())).concatenate(MakeChar('0').repeat());
        Automaton ws = Whitespace();
        return (ws.concatenate(s.concatenate(MakeChar('0').repeat()).concatenate(MakeString(b1.toString())).concatenate(d)).concatenate(ws)).minimize();
    }
    public static Automaton MakeStringMatcher(String s) {
        Automaton a = new Automaton();
        State[] states = new State[s.length() + 1];
        states[0] = a.initial;
        for (int i = 0; i < s.length(); i++){
            states[i+1] = new State();
        }
        State f = states[s.length()];
        f.accept = true;
        f.add(new Transition(Character.MIN_VALUE, Character.MAX_VALUE, f));
        for (int i = 0; i < s.length(); i++) {
            Set<Character> done = new Set<Character>();
            char c = s.charAt(i);
            states[i].add(new Transition(c, states[i+1]));
            done.add(c);
            for (int j = i; j >= 1; j--) {
                char d = s.charAt(j-1);
                if (!done.contains(d) && s.substring(0, j-1).equals(s.substring(i-j+1, i))) {
                    states[i].add(new Transition(d, states[j]));
                    done.add(d);
                }
            }
            char[] da = new char[done.size()];
            int h = 0;
            for (char w : done)
                da[h++] = w;
            Arrays.sort(da);
            int from = Character.MIN_VALUE;
            int k = 0;
            while (from <= Character.MAX_VALUE) {
                while (k < da.length && da[k] == from) {
                    k++;
                    from++;
                }
                if (from <= Character.MAX_VALUE) {
                    int to = Character.MAX_VALUE;
                    if (k < da.length) {
                        to = da[k]-1;
                        k++;
                    }
                    states[i].add(new Transition((char)from, (char)to, states[0]));
                    from = to+2;
                }
            }
        }
        a.deterministic = true;
        return a;
    }
}
