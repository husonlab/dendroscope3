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

package dendroscope.hybrid;

import java.util.*;


/**
 * @author Beckson
 */
public class EasySiblingMemory {

    private final Vector<String> taxaOrdering;
    private final Vector<String> taxonLabels;

    private final Hashtable<BitSet, HashSet<BitSet>> treeSetToForestSet = new Hashtable<>();
    private final Hashtable<String, Vector<String>> taxontToTaxa = new Hashtable<>();

//	private BitSet treeSet;
//	private BitSet forestSet;

    @SuppressWarnings("unchecked")
    public EasySiblingMemory(Vector<String> taxaOrdering) {
        this.taxaOrdering = taxaOrdering;
        this.taxonLabels = (Vector<String>) taxaOrdering.clone();
    }

    public EasySiblingMemory(EasyTree t, Vector<HybridTree> forest, Vector<String> taxaOrdering) {
        this.taxaOrdering = taxaOrdering;
        taxonLabels = new Vector<>();
        for (EasyNode v : t.getLeaves())
            taxonLabels.add(v.getLabel());
    }

    public BitSet getForestSet(Vector<EasyTree> forest) {
        Vector<BitSet> forestSets = new Vector<>();
        for (EasyTree t : forest) {
            BitSet b = new BitSet(taxaOrdering.size());
            for (EasyNode v : t.getLeaves()) {
                if (taxontToTaxa.containsKey(v.getLabel())) {
                    for (String s : taxontToTaxa.get(v.getLabel()))
                        b.set(taxaOrdering.indexOf(s));
                } else
                    b.set(taxaOrdering.indexOf(v.getLabel()));
            }
            forestSets.add(b);
        }
        Collections.sort(forestSets, new FirstBitComparator());
        BitSet b = new BitSet(forestSets.size() * taxaOrdering.size());
        for (BitSet f : forestSets) {
            int bitIndex = f.nextSetBit(0);
            while (bitIndex != -1) {
                b.set(bitIndex + forestSets.indexOf(f) * taxaOrdering.size());
                bitIndex = f.nextSetBit(bitIndex + 1);
            }
        }
        return b;
    }

    public BitSet getTreeSet(EasyTree t) {
        BitSet b = new BitSet(taxonLabels.size());
        for (EasyNode v : t.getLeaves()) {
            String label = v.getLabel();
            b.set(taxonLabels.indexOf(label));
        }
        return b;
    }

    public boolean contains(EasyTree t, Vector<EasyTree> forest) {

        BitSet treeSet = getTreeSet(t);
        BitSet forestSet = getForestSet(forest);

        if (treeSetToForestSet.containsKey(treeSet)) {
            for (BitSet b : treeSetToForestSet.get(treeSet)) {
                if (b.size() > forestSet.size()) {
                    if (b.equals(forestSet))
                        return true;
                } else if (forestSet.equals(b))
                    return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public void addEntry(EasyTree t, Vector<EasyTree> forest) {

        BitSet treeSet = getTreeSet(t);
        BitSet forestSet = getForestSet(forest);

        if (treeSetToForestSet.containsKey(treeSet)) {
            HashSet<BitSet> set = (HashSet<BitSet>) treeSetToForestSet.get(
                    treeSet).clone();
            set.add(forestSet);
            treeSetToForestSet.remove(treeSet);
            treeSetToForestSet.put(treeSet, set);
        } else {
            HashSet<BitSet> set = new HashSet<>();
            set.add(forestSet);
            treeSetToForestSet.put(treeSet, set);
        }

    }

    public void addTreeLabel(String label) {
        if (!taxonLabels.contains(label))
            taxonLabels.add(label);
    }

    public void addTaxon(String taxon, Vector<String> taxa) {
        Vector<String> v = new Vector<>();
        if (taxontToTaxa.containsKey(taxa.get(0))) {
            for (String s : taxontToTaxa.get(taxa.get(0)))
                v.add(s);
        } else
            v.add(taxa.get(0));
        if (taxontToTaxa.containsKey(taxa.get(1))) {
            for (String s : taxontToTaxa.get(taxa.get(1)))
                v.add(s);
        } else
            v.add(taxa.get(1));
        taxontToTaxa.put(taxon, v);
    }

    public boolean hasSameLeafSet(EasyTree t1, EasyTree t2) {
        if (t1.getLeaves().size() == t2.getLeaves().size()) {
            BitSet b1 = getTreeSet(t1);
            BitSet b2 = getTreeSet(t2);
            if (b1.equals(b2))
                return true;
        }
        return false;
    }

    public Hashtable<String, Vector<String>> getTaxontToTaxa() {
        return taxontToTaxa;
    }

    public boolean compareTaxa(String s1, String s2) {
        if (taxonLabels.contains(s1) && taxonLabels.contains(s2)) {

            BitSet b1 = new BitSet(taxonLabels.size());
            b1.set(taxonLabels.indexOf(s1));

            BitSet b2 = new BitSet(taxonLabels.size());
            b2.set(taxonLabels.indexOf(s2));

            if (b1.equals(b2))
                return true;
        }
        return false;
    }

}
