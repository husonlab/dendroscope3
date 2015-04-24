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

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class EasySiblings {

    private final Hashtable<String, EasyNode> t1TaxaToNode = new Hashtable<>();
    private final Hashtable<String, EasyNode> forestTaxaToNode = new Hashtable<>();

    private final Hashtable<String, Vector<String>> taxaToSiblingPair = new Hashtable<>();
    private final HashSet<Vector<String>> siblingsOfT1 = new HashSet<>();

    public void init(EasyTree t1, Vector<EasyTree> forest) {

        initTaxaToNode(t1, t1TaxaToNode);

        for (EasyTree t : forest)
            initTaxaToNode(t, forestTaxaToNode);

    }

    private void getSiblingPairs(EasyTree t,
                                 HashSet<Vector<String>> siblings, boolean onlyOne) {

//		System.out.println("-------------------------");

        if (t.getLeaves().size() >= 2) {
            Iterator<EasyNode> it = t.getLeaves().iterator();
            HashSet<EasyNode> parents = new HashSet<>();
            while (it.hasNext()) {
                EasyNode v = it.next();
                EasyNode p = v.getParent();

                if (!parents.contains(p) && isCherry(p)) {

                    parents.add(p);

                    Vector<String> taxa = new Vector<>();

                    // collecting taxa
                    for (EasyNode c : p.getChildren())
                        taxa.add(c.getLabel());

//					System.out.println("Taxa: "+taxa);
                    taxaToSiblingPair.put(taxa.get(0), taxa);
                    taxaToSiblingPair.put(taxa.get(1), taxa);
                    siblings.add(taxa);

                    if (onlyOne)
                        break;

                }
            }
        }
    }

    private boolean isCherry(EasyNode v) {
        for (EasyNode c : v.getChildren()) {
            if (c.getOutDegree() != 0)
                return false;
        }
        return true;
    }

    public void updateTaxa(EasyTree t1,
                           Vector<EasyTree> forest) {

        t1TaxaToNode.clear();
        forestTaxaToNode.clear();

        initTaxaToNode(t1, t1TaxaToNode);

        for (EasyTree t : forest)
            initTaxaToNode(t, forestTaxaToNode);
    }

    public void updateTaxa(EasyTree t1) {
        t1TaxaToNode.clear();
        initTaxaToNode(t1, t1TaxaToNode);
    }

    public void updateTaxa(Vector<EasyTree> forest) {
        forestTaxaToNode.clear();
        for (EasyTree t : forest)
            initTaxaToNode(t, forestTaxaToNode);
    }

    public void updateSiblings(EasyTree t1,
                               Vector<EasyTree> forest, boolean onlyOne) {
        siblingsOfT1.clear();
        getSiblingPairs(t1, siblingsOfT1, onlyOne);
    }

    public HashSet<Vector<String>> getSiblingsOfT1() {
        return siblingsOfT1;
    }

    private void initTaxaToNode(EasyTree t1,
                                Hashtable<String, EasyNode> t1TaxaToNode2) {
        for (EasyNode v : t1.getLeaves())
            t1TaxaToNode2.put(v.getLabel(), v);
    }

    public EasyNode getT1Leaf(String s) {
        return t1TaxaToNode.get(s);
    }

    public EasyNode getForestLeaf(String s) {
        return forestTaxaToNode.get(s);
    }

    public void removeT1Leaf(String s) {
        t1TaxaToNode.remove(s);
        if (taxaToSiblingPair.containsKey(s))
            siblingsOfT1.remove(taxaToSiblingPair.get(s));
    }

    public void putT1Leaf(String s, EasyNode v) {
        t1TaxaToNode.put(s, v);
    }

    public void removeForestLeaves(EasyTree t) {
        for (EasyNode v : t.getNodes())
            forestTaxaToNode.remove(v.getLabel());
    }

    public void removeForestLeaf(String s) {
        forestTaxaToNode.remove(s);
    }

    public void putForestLeaves(EasyTree t) {
        for (EasyNode v : t.getNodes()) {
            forestTaxaToNode.remove(v.getLabel());
            forestTaxaToNode.put(v.getLabel(), v);
        }
    }

    public void putForestLeaf(String s, EasyNode v) {
        forestTaxaToNode.put(s, v);
    }

    @SuppressWarnings("unchecked")
    public Vector<String> popSibling() {
        Vector<String> siblingPair = (Vector<String>) siblingsOfT1.iterator()
                .next().clone();
        siblingsOfT1.remove(siblingPair);
        taxaToSiblingPair.remove(siblingPair.get(0));
        taxaToSiblingPair.remove(siblingPair.get(1));
        return siblingPair;
    }

    public int getNumOfSiblings() {
        return siblingsOfT1.size();
    }

}
