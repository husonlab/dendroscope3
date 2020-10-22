/*
 *   EasySiblings.java Copyright (C) 2020 Daniel H. Huson
 *
 *   (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dendroscope.hybroscale.model.cmpAllMAAFs;

import java.util.*;

public class EasySiblings {

    private Hashtable<String, EasyNode> t1TaxaToNode = new Hashtable<String, EasyNode>();
    private Hashtable<String, EasyNode> forestTaxaToNode = new Hashtable<String, EasyNode>();

    private Vector<Vector<Vector<String>>> siblingsOfT1 = new Vector<Vector<Vector<String>>>();
    private int maxSize;

    public void init(EasyTree t1, Vector<EasyTree> forest, int maxSize) {
        initTaxaToNode(t1, t1TaxaToNode);
        for (EasyTree t : forest)
            initTaxaToNode(t, forestTaxaToNode);
        this.maxSize = maxSize;
    }

    private void getSiblingPairs(EasyTree t, Vector<Vector<Vector<String>>> siblings) {

        // System.out.println("-------------------------");

        if (t.getLeaves().size() >= 2) {
            Iterator<EasyNode> it = t.getLeaves().iterator();
            HashSet<EasyNode> parents = new HashSet<EasyNode>();
            while (it.hasNext() && siblings.size() < maxSize) {
                EasyNode v = it.next();
                EasyNode p = v.getParent();

                if (!parents.contains(p) && isCherry(p)) {
                    parents.add(p);

                    Vector<Vector<String>> taxaPairs = new Vector<Vector<String>>();
                    for (int i = 0; i < p.getChildren().size() - 1; i++) {
                        for (int j = i + 1; j < p.getChildren().size(); j++) {
                            Vector<String> taxa = new Vector<String>();
                            taxa.add(p.getChildren().get(i).getLabel());
                            taxa.add(p.getChildren().get(j).getLabel());
                            Collections.sort(taxa);
                            taxaPairs.add(taxa);
                        }
                    }
                    siblings.add(taxaPairs);
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

    public void updateTaxa(EasyTree t1, Vector<EasyTree> forest) {
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

    public void updateSiblings(EasyTree t1, Vector<EasyTree> forest, boolean sortSiblings) {
        siblingsOfT1.clear();
        getSiblingPairs(t1, siblingsOfT1);
    }

    public Vector<Vector<Vector<String>>> getSiblingsOfT1() {
        return siblingsOfT1;
    }

    private void initTaxaToNode(EasyTree t, Hashtable<String, EasyNode> taxaToNode) {
        for (EasyNode v : t.getLeaves())
            taxaToNode.put(v.getLabel(), v);
    }

    public EasyNode getT1Leaf(String s) {
        return t1TaxaToNode.get(s);
    }

    public EasyNode getForestLeaf(String s) {
        return forestTaxaToNode.get(s);
    }

    public void removeT1Leaf(String s) {
        t1TaxaToNode.remove(s);
    }

    public void removeForestLeaves(EasyTree t) {
        for (EasyNode v : t.getLeaves())
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

    @SuppressWarnings("unchecked")
    public Vector<Vector<String>> popSibling() {
        Vector<Vector<String>> siblingPair = (Vector<Vector<String>>) siblingsOfT1.iterator().next().clone();
        siblingsOfT1.remove(siblingPair);
        return siblingPair;
    }

    public Vector<Vector<Vector<String>>> getAllSiblings() {
        return siblingsOfT1;
    }

    public int getNumOfSiblings() {
        return siblingsOfT1.size();
    }

    public void clear() {
        t1TaxaToNode.clear();
        forestTaxaToNode.clear();
        siblingsOfT1.clear();
    }

}
