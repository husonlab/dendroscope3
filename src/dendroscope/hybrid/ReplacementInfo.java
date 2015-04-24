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

import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

public class ReplacementInfo {

    private Vector<String> prunedLabels = new Vector<>();

    //storing all cluster networks caused be an abortion
    private final Vector<HybridNetwork> clusterNetworks = new Vector<>();

    // storing labels for each leaf
    private final Hashtable<String, String> numberToLabel = new Hashtable<>();

    // storing labels replacing a common subtree
    private final Hashtable<String, PhyloTree> labelToSubtree = new Hashtable<>();

    protected final Hashtable<String, Vector<String>> startLabelToChain = new Hashtable<>();
    protected final Hashtable<String, String> startLabelToEndLabel = new Hashtable<>();

    // storing labels replacing a common cluster
    private final HashSet<String> replacementLabels = new HashSet<>();

    // storing labels replacing a set of networks representing a minimal common
    // cluster
    // REMARK: a label can replace more than one network if several MAAFs exist
    // representing a minimal cluster
    private final Hashtable<String, Vector<HybridNetwork>> labelToNetworks = new Hashtable<>();

    public void putLabelToSubtree(String l, PhyloTree t) {
        if (!labelToSubtree.containsKey(l))
            labelToSubtree.put(l, t);
    }

    public void putStartLabelToChain(String startLabel,
                                     Vector<String> chainLabels) {
        if (!startLabelToChain.containsKey(startLabel))
            startLabelToChain.put(startLabel, chainLabels);
    }

    public void putStartLabelToEndLabel(String startLabel, String endLabel) {
        if (!startLabelToEndLabel.containsKey(startLabel))
            startLabelToEndLabel.put(startLabel, endLabel);
    }

    public Hashtable<String, PhyloTree> getLabelToSubtree() {
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
            Vector<HybridNetwork> v = (Vector<HybridNetwork>) labelToNetworks
                    .get(repChar).clone();
            labelToNetworks.remove(repChar);
            v.add(n);
            labelToNetworks.put(repChar, v);
        } else {
            Vector<HybridNetwork> v = new Vector<>();
            v.add(n);
            labelToNetworks.put(repChar, v);
        }

        if (isClusterNetwork)
            clusterNetworks.add(n);
    }

    public boolean isClusterNetwork(HybridNetwork n) {
        return clusterNetworks.contains(n);
    }

    public Hashtable<String, Vector<HybridNetwork>> getLabelToNetworks() {
        return labelToNetworks;
    }

    public Hashtable<String, Vector<String>> getStartLabelToChain() {
        return startLabelToChain;
    }

    public Hashtable<String, String> getStartLabelToEndLabel() {
        return startLabelToEndLabel;
    }

    public void replaceLabels(PhyloTree t1, PhyloTree t2) {
        int i = 0;
        Hashtable<String, String> leafToNum = new Hashtable<>();
        numberToLabel.clear();
        for (Node v : t1.computeSetOfLeaves()) {
            String label = t1.getLabel(v);
            numberToLabel.put("" + i, label);
            leafToNum.put(label, "" + i);
            t1.setLabel(v, "" + i);
            i++;
        }
        for (Node v : t2.computeSetOfLeaves()) {
            String label = t2.getLabel(v);
            t2.setLabel(v, leafToNum.get(label));
        }
    }

    public void addLeafLabels(HybridNetwork n) {
        for (Node v : n.computeSetOfLeaves()) {
            String label = n.getLabel(v);
            if (numberToLabel.containsKey(label))
                n.setLabel(v, numberToLabel.get(label));
        }
    }

    public void clearNumberToLabels() {
        numberToLabel.clear();
    }

    public Vector<String> getPrunedLabels() {
        return prunedLabels;
    }

    public void setPrunedLabels(Vector<String> prunedLabels) {
        this.prunedLabels = prunedLabels;
    }
}
