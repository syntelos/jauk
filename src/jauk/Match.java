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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package jauk;

/**
 * Match from pattern.
 */
public interface Match
{
    /**
     * Pattern is satisfied
     */
    public boolean satisfied();
    /**
     * Input has been consumed
     */
    public boolean terminal();
    /**
     * Last index in pattern plus one.
     */
    public int next();
    /**
     * First index in matched character sequence
     */
    public int start();
    /**
     * Last index in matched character sequence
     */
    public int end();
    /**
     * Matched character sequence as string
     */
    public String group();
}
