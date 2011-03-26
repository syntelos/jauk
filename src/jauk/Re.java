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

import automaton.Automaton;
import automaton.BasicAutomata;
import automaton.NamedAutomata;

/**
 * Use Anders M&oslash;ller's Automaton with the builtin named
 * automata from the Automaton package.
 */
public class Re
    extends automaton.RegExp
    implements Pattern
{

    public Re(String re){
	super(re);
    }
    public Re(NamedAutomata map, String re){
	super(map,re);
    }
}
