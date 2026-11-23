/*
 * Num2NodeArray.java Copyright (C) 2023 Daniel H. Huson
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

import jloda.graph.Node;

/**
 * A wrapper class for an array mapping numbers to nodes
 * Daniel Huson, 1.2007
 */
public class Num2NodeArray {
    Node[] array;

    /**
     * default constructor
     */
    public Num2NodeArray() {
        array = new Node[0];
    }

    /**
     * default constructor
     *
     * @param n number of nodes (nodes are  numbered 1..n+1)
     */
    public Num2NodeArray(int n) {
        array = new Node[n + 1];
    }

    /**
     * wrapper constructor
     *
	 */
    public Num2NodeArray(Node[] array) {
        this.array = array;
    }

    /**
     * sets the wrapped array
     *
	 */
    public void set(Node[] array) {
        this.array = array;
    }

    /**
     * sets the i-th entry. Assumes the wrapped array has already been constructed or set
     *
	 */
    public void put(int i, Node v) {
        array[i] = v;
    }

    /**
     * gets the -th entry
     *
     * @return node at position i of array
     */
    public Node get(int i) {
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
     * @return node array
     */
    public Node[] get() {
        return array;
    }

    /**
     * erase and resize  to hold (0,1,...,n)
     *
	 */
    public void clear(int n) {
        array = new Node[n + 1];
    }
}

