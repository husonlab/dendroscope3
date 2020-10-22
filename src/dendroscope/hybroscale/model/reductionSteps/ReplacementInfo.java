/*
 *   ReplacementInfo.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.reductionSteps;

import dendroscope.hybroscale.model.treeObjects.HybridNetwork;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class ReplacementInfo {

    private HashMap<Integer, Vector<String>> treeIndexToUniqueTaxa = new HashMap<Integer, Vector<String>>();

    private Vector<String> prunedLabels = new Vector<String>();

    // storing all cluster networks caused be an abortion
    private Vector<HybridNetwork> clusterNetworks = new Vector<HybridNetwork>();

    // storing labels for each leaf
    private ConcurrentHashMap<String, String> numberToLabel = new ConcurrentHashMap<String, String>();
    private ConcurrentHashMap<String, String> labelToNumber = new ConcurrentHashMap<String, String>();

    // storing labels replacing a common subtree
    private ConcurrentHashMap<String, MyPhyloTree> labelToSubtree = new ConcurrentHashMap<String, MyPhyloTree>();

    protected ConcurrentHashMap<String, Vector<String>> startLabelToChain = new ConcurrentHashMap<String, Vector<String>>();
    protected ConcurrentHashMap<String, String> startLabelToEndLabel = new ConcurrentHashMap<String, String>();

    // storing labels replacing a common cluster
    private HashSet<String> replacementLabels = new HashSet<String>();

    // storing labels replacing a set of networks representing a minimal common
    // cluster
    // REMARK: a label can replace more than one network if several MAAFs exist
    // representing a minimal cluster
    private Hashtable<String, Vector<HybridNetwork>> labelToNetworks = new Hashtable<String, Vector<HybridNetwork>>();

    public void putLabelToSubtree(String l, MyPhyloTree t) {
        if (!labelToSubtree.containsKey(l))
            labelToSubtree.put(l, t);
    }

    public void putStartLabelToChain(String startLabel, Vector<String> chainLabels) {
        if (!startLabelToChain.containsKey(startLabel))
            startLabelToChain.put(startLabel, chainLabels);
    }

    public void putStartLabelToEndLabel(String startLabel, String endLabel) {
        if (!startLabelToEndLabel.containsKey(startLabel))
            startLabelToEndLabel.put(startLabel, endLabel);
    }

    public ConcurrentHashMap<String, MyPhyloTree> getLabelToSubtree() {
        return labelToSubtree;
    }

    public void addClusterLabel(String l) {
        if (!replacementLabels.contains(l))
            replacementLabels.add(l);
    }

    public HashSet<String> getReplacementLabels() {
        return replacementLabels;
    }

    @SuppressWarnings("unchecked")
    public void addNetwork(HybridNetwork n, boolean isClusterNetwork) {

        n.removeOutgroup();
        String repChar = n.getReplacementCharacter();

        // REMARK: a label can replace more than one network if several MAAFs
        // exist representing a minimal cluster
        if (labelToNetworks.containsKey(repChar)) {
            Vector<HybridNetwork> v = (Vector<HybridNetwork>) labelToNetworks.get(repChar).clone();
            labelToNetworks.remove(repChar);
            v.add(n);
            labelToNetworks.put(repChar, v);
        } else {
            Vector<HybridNetwork> v = new Vector<HybridNetwork>();
            v.add(n);
            labelToNetworks.put(repChar, v);
        }

        if (isClusterNetwork)
            clusterNetworks.add(n);
    }

    public boolean isClusterNetwork(HybridNetwork n) {
        if (clusterNetworks.contains(n))
            return true;
        return false;
    }

    public Hashtable<String, Vector<HybridNetwork>> getLabelToNetworks() {
        return labelToNetworks;
    }

    public ConcurrentHashMap<String, Vector<String>> getStartLabelToChain() {
        return startLabelToChain;
    }

    public ConcurrentHashMap<String, String> getStartLabelToEndLabel() {
        return startLabelToEndLabel;
    }

    public void replaceLabelsByNumber(MyPhyloTree[] trees) {

        HashSet<String> labels = new HashSet<String>();
        for (MyPhyloTree t : trees) {
            for (MyNode v : t.getLeaves())
                labels.add(t.getLabel(v));
        }

        int i = 0;
        Hashtable<String, String> leafToUnique = new Hashtable<String, String>();
        numberToLabel.clear();
        for (MyPhyloTree t : trees) {
            for (MyNode v : t.getLeaves()) {
                String label = t.getLabel(v);
                if (!leafToUnique.containsKey(label)) {
                    String newLabel = "" + i;
                    while (labels.contains(newLabel)) {
                        i++;
                        newLabel = "" + i;
                    }
                    numberToLabel.put("" + i, label);
                    labelToNumber.put(label, "" + i);
                    leafToUnique.put(label, "" + i);
                    t.setLabel(v, "" + i);
                    i++;
                } else
                    t.setLabel(v, leafToUnique.get(label));
            }
        }
    }

    public Vector<String> addLeafLabels(HybridNetwork n, Vector<String> taxaOrdering) {
        for (MyNode v : n.getLeaves()) {
            String label = n.getLabel(v);
            if (numberToLabel.containsKey(label)) {
                n.setLabel(v, numberToLabel.get(label));
                if (!taxaOrdering.contains(v.getLabel()))
                    taxaOrdering.add(v.getLabel());
            }
        }
        return taxaOrdering;
    }

    public void addLeafLabels(MyPhyloTree t) {
        for (MyNode v : t.getLeaves()) {
            String label = v.getLabel();
            if (numberToLabel.containsKey(label))
                v.setLabel(numberToLabel.get(label));
        }
    }

    public Vector<String> addLeafLabels(MyPhyloTree n, Vector<String> taxaOrdering) {
        for (MyNode v : n.getLeaves()) {
            String label = n.getLabel(v);
            if (numberToLabel.containsKey(label)) {
                n.setLabel(v, numberToLabel.get(label));
                if (!taxaOrdering.contains(v.getLabel()))
                    taxaOrdering.add(v.getLabel());
            }
        }
        return taxaOrdering;
    }

    public void clearNumberToLabels() {
        numberToLabel.clear();
    }

    public ConcurrentHashMap<String, String> getLabelToNumber() {
        return labelToNumber;
    }

    public ConcurrentHashMap<String, String> getNumberToLabel() {
        return numberToLabel;
    }

    public Vector<String> getPrunedLabels() {
        return prunedLabels;
    }

    public void setPrunedLabels(Vector<String> prunedLabels) {
        this.prunedLabels = prunedLabels;
    }

    public void removeUniqueTaxa(MyPhyloTree[] phyloTrees) {
        HashMap<String, Vector<MyNode>> taxaToUniqueLeaves = new HashMap<String, Vector<MyNode>>();
        HashMap<MyPhyloTree, Integer> treeToIndex = new HashMap<MyPhyloTree, Integer>();
        for (int i = 0; i < phyloTrees.length; i++) {
            MyPhyloTree t = phyloTrees[i];
            treeToIndex.put(t, i);
            for (MyNode v : t.getLeaves()) {
                if (!taxaToUniqueLeaves.containsKey(v.getLabel()))
                    taxaToUniqueLeaves.put(v.getLabel(), new Vector<MyNode>());
                taxaToUniqueLeaves.get(v.getLabel()).add(v);
            }
        }

        for (Vector<MyNode> uniqueLeaves : taxaToUniqueLeaves.values()) {
            if (uniqueLeaves.size() == 1) {
                MyNode leaf = uniqueLeaves.firstElement();
                MyPhyloTree t = (MyPhyloTree) leaf.getOwner();
                if (!treeIndexToUniqueTaxa.containsKey(treeToIndex.get(t)))
                    treeIndexToUniqueTaxa.put(treeToIndex.get(t), new Vector<String>());
                treeIndexToUniqueTaxa.get(treeToIndex.get(t)).add(leaf.getLabel());
                MyEdge e = leaf.getFirstInEdge();
                MyNode p = e.getSource();
                t.deleteEdge(e);
                removeOneNode(p, t);
            }
        }

    }

    public HashMap<Integer, Vector<String>> getTreeIndexToUniqueTaxa() {
        return treeIndexToUniqueTaxa;
    }

    private void removeOneNode(MyNode v, MyPhyloTree t) {
        if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
            MyNode p = v.getFirstInEdge().getSource();
            MyNode c = v.getFirstOutEdge().getTarget();
            t.deleteEdge(v.getFirstInEdge());
            t.deleteEdge(v.getFirstOutEdge());
            t.newEdge(p, c);
        } else if (v.getInDegree() == 0 && v.getOutDegree() == 1) {
            MyNode c = v.getFirstOutEdge().getTarget();
            t.deleteEdge(v.getFirstOutEdge());
            t.setRoot(c);
        }
    }

    public boolean containsClusterNetwork() {
        return !clusterNetworks.isEmpty();
    }

}
