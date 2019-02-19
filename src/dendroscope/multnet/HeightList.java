/**
 * HeightList.java 
 * Copyright (C) 2019 Daniel H. Huson
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
package dendroscope.multnet;

import jloda.graph.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 * A HeightList contains only nodes of the same height. The elements are always sorted by their nested labels.
 * For finding the insertion point of a new element we use binary search. -> O(log n) for inserting a new node.
 *
 * @author thomas bonfert, 6.2009
 */

public class HeightList extends ArrayList {

    final HashMap<Integer, Multiset> multisets;

    public HeightList() {
        super();
        this.multisets = new HashMap<>();
    }


    public void addSorted(Node n, Multiset multiset) {
        this.multisets.put(n.getId(), multiset);
        final class MultisetComparator implements Comparator {
            final HashMap<Integer, Multiset> multisets;

            public MultisetComparator(HashMap<Integer, Multiset> multisets) {
                super();
                this.multisets = multisets;
            }

            public int compare(Object o1, Object o2) {
                Node n1 = (Node) o1;
                Node n2 = (Node) o2;

                String concatenatedElements1 = this.multisets.get(n1.getId()).getConcatenatedElements();
                String concatenatedElements2 = this.multisets.get(n2.getId()).getConcatenatedElements();
                return concatenatedElements1.compareTo(concatenatedElements2);
            }
        }

        MultisetComparator comparator = new MultisetComparator(this.multisets);
        int insertionPoint = Collections.binarySearch(this, n, comparator);
        if (insertionPoint < 0) insertionPoint = -1 * insertionPoint - 1;
        this.add(insertionPoint, n);
    }

}
