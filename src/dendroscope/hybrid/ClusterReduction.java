/**
 * ClusterReduction.java 
 * Copyright (C) 2018 Daniel H. Huson
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
package dendroscope.hybrid;

import jloda.graph.Node;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * This function replaces a minimum common cluster of two rooted, bifurcating
 * phylogenetic trees by a unique taxon labeling.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class ClusterReduction {

    private final Hashtable<String, HybridTree[]> labelToCluster = new Hashtable<>();
    private final Hashtable<HybridTree[], Integer> clusterToLevel = new Hashtable<>();
    private int maxLevel = 0;
    private String label;

    public HybridTree[] run(HybridTree t1, HybridTree t2, ReplacementInfo rI)
            throws Exception {

        BitSet cluster = minimumCommonCluster(t1, t2);

        HybridTree[] trees = new HybridTree[2];
        if (cluster == null)
            return null;
        else if (cluster.equals(t1.getNodeToCluster().get(t1.getRoot())))
            return null;
        else {
            // replace minimal common cluster, replacement is captured in
            // ReplacementInfo
            trees[0] = replaceNode(t1, cluster, rI);
            trees[1] = replaceNode(t2, cluster, rI);

            setLevel(trees);
            labelToCluster.put(label, trees);

            // return subtrees displaying the minimal cluster of each tree
            return trees;
        }

    }

    // finding minimal cluster by a simple post order walk
    private BitSet minimumCommonCluster(HybridTree t1, HybridTree t2) {
        Iterator<Node> it = t2.postOrderWalk();
        while (it.hasNext()) {
            Node v = it.next();
            if (v.getOutDegree() != 0) {
                BitSet cluster = t2.getNodeToCluster().get(v);
                if (t1.getClusterSet().contains(cluster))
                    return cluster;
            }
        }
        return null;
    }

    private HybridTree replaceNode(HybridTree t, BitSet cluster,
                                   ReplacementInfo rI) throws Exception {

        // modify input tree
        Node v = t.getClusterToNode().get(cluster);
        label = t.getTaxa(cluster.nextSetBit(0)) + "'";
        HybridTree subtree = t.getSubtree(v, true);

        Node newV = t.newNode();
        t.setLabel(newV, label);
        t.deleteSubtree(v, newV, true);

        // keep replacement in mind for reattachment...
        rI.addClusterLabel(label);

        // return subtree displaying the minimal cluster
        HybridTree newTree = new HybridTree(subtree, true, null);
        newTree.setReplacementCharacter(label);
        newTree.update();

        return newTree;

    }

    private void setLevel(HybridTree[] cluster) {
        HybridTree t = cluster[0];
        for (Node v : t.computeSetOfLeaves()) {
            String label = t.getLabel(v);
            if (labelToCluster.containsKey(label)) {
                int level = clusterToLevel.get(labelToCluster.get(label)) + 1;
                if (level > maxLevel)
                    maxLevel = level;
                clusterToLevel.put(cluster, level);
            }
        }
        if (!clusterToLevel.containsKey(cluster))
            clusterToLevel.put(cluster, 0);
    }

    public int getLevel(HybridTree[] cluster) {
        return clusterToLevel.get(cluster);
    }

    public int getMaxLevel() {
        return maxLevel;
    }

}
