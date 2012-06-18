/*
 * Jauk
 * Copyright (C) 2011 John Pritchard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the Lesser GNU General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package jauk;

/**
 * Stateless producer of matches.
 */
public interface Pattern {

    /**
     * Used within the automaton package to communicate application
     * request through pattern into match
     */
    public enum Op {
        Match, Search;
    }



    public boolean matches(CharSequence string);

    public Match match(CharSequence string);

    public Match match(CharSequence string, int offset);

    public Match match(CharSequence string, int offset, int lno);

    public Match search(CharSequence string);

    public Match search(CharSequence string, int offset);

    public Match search(CharSequence string, int offset, int lno);
}
