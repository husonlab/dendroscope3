/*
 * Num2EdgeArray.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dendroscope.io;

import jloda.graph.Edge;

/**
 * A wrapper class for an array mapping numbers to adjacentEdges
 * Daniel Huson, 1.2007
 */
public class Num2EdgeArray {
    Edge[] array;

    /**
     * default constructor
     */
    public Num2EdgeArray() {
        array = new Edge[0];
    }

    /**
     * default constructor
     *
     * @param n number of adjacentEdges (adjacentEdges are  numbered 1..n+1)
     */
    public Num2EdgeArray(int n) {
        array = new Edge[n + 1];
    }

    /**
     * wrapper constructor
     *
     * @param array
     */
    public Num2EdgeArray(Edge[] array) {
        this.array = array;
    }

    /**
     * sets the wrapped array
     *
     * @param array
     */
    public void set(Edge[] array) {
        this.array = array;
    }

    /**
     * sets the i-th entry. Assumes the wrapped array has already been constructed or set
     *
     * @param i
     * @param v
     */
    public void put(int i, Edge v) {
        array[i] = v;
    }

    /**
     * gets the -th entry
     *
     * @param i
     * @return edge at position i of array
     */
    public Edge get(int i) {
        return array[i];
    }

    /**
     * gets the length of the array
     *
     * @return length
     */
    public int length() {
        return array.length;
    }

    /**
     * gets the wrapped array
     *
     * @return edge array
     */
    public Edge[] get() {
        return array;
    }

    /**
     * erase and resize  to hold (0,1,...,n)
     *
     * @param n
     */
    public void clear(int n) {
        array = new Edge[n + 1];
    }
}
