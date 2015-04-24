/**
 * Copyright 2015, Daniel Huson
 *
 *(Some files contain contributions from other authors, who are then mentioned separately)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
