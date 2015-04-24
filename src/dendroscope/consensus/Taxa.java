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

package dendroscope.consensus;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * maintains the taxa associated with a tree or network
 * Daniel Huson, 6.2007
 */
public class Taxa {
    final Map<String, Integer> name2index;
    final Map<Integer, String> index2name;
    final BitSet bits;
    int ntax;

    /**
     * constructor
     */
    public Taxa() {
        name2index = new HashMap<>();
        index2name = new HashMap<>();
        bits = new BitSet();
        ntax = 0;
    }

    /**
     * get the t-th taxon (numbered 1-size)
     *
     * @param t
     * @return name of t-th taxon
     */
    public String getLabel(int t) {
        return index2name.get(t);

    }

    /**
     * index of the named taxon, or -1
     *
     * @param name
     * @return index or -1
     */
    public int indexOf(String name) {
        Integer index = name2index.get(name);
        if (index == null)
            return -1;
        else
            return index;

    }

    /**
     * add the named taxon
     *
     * @param name
     * @return the index of the taxon
     */
    public int add(String name) {
        if (!name2index.keySet().contains(name)) {
            ntax++;
            bits.set(ntax);
            Integer index = ntax;
            index2name.put(index, name);
            name2index.put(name, index);
            return ntax;
        } else
            return name2index.get(name);
    }

    /**
     * does this taxa object contain the named taxon?
     *
     * @param name
     * @return true, if contained
     */
    public boolean contains(String name) {
        return indexOf(name) != -1;
    }

    /**
     * get the number of taxa
     *
     * @return number of taxa
     */
    public int size() {
        return bits.cardinality();
    }

    /**
     * gets the maximal defined taxon id
     *
     * @return max id
     */
    public int maxId() {
        int t = -1;
        while (true) {
            int s = bits.nextSetBit(t + 1);
            if (s == -1)
                return t;
            else
                t = s;
        }
    }

    /**
     * clear all taxa
     */
    public void clear() {
        ntax = 0;
        bits.clear();
        index2name.clear();
        name2index.clear();
    }

    /**
     * gets the complement to bit set A
     *
     * @param A
     * @return complement
     */
    public BitSet getComplement(BitSet A) {
        BitSet result = new BitSet();

        for (int t = 1; t <= ntax; t++) {
            if (!A.get(t))
                result.set(t);
        }
        return result;

    }

    /**
     * add all taxa.
     *
     * @param taxa
     * @return set of indices
     */
    public void addAll(Taxa taxa) {
        for (Iterator it = taxa.iterator(); it.hasNext(); ) {
            String name = (String) it.next();
            add(name);
        }
    }

    /**
     * gets string representation
     *
     * @return string
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Taxa (").append(size()).append("):\n");
        for (Iterator it = iterator(); it.hasNext(); ) {
            String name = (String) it.next();
            buf.append(name).append("\n");
        }
        return buf.toString();
    }

    /**
     * gets an iterator over all taxon names
     *
     * @return iterator
     */
    public Iterator<String> iterator() {
        return name2index.keySet().iterator();
    }

    /**
     * gets the bits of this set
     *
     * @return bits
     */
    public BitSet getBits() {
        return (BitSet) bits.clone();
    }

    /**
     * remove this taxon
     *
     * @param name
     */
    public void remove(String name) {
        Integer tt = name2index.get(name);
        if (tt != null) {
            name2index.keySet().remove(name);
            index2name.remove(tt);
            ntax--;
            bits.set(tt, false);
        }
    }
}
