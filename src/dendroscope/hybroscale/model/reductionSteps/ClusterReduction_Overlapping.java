/*
 *   ClusterReduction_Overlapping.java Copyright (C) 2020 Daniel H. Huson
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

import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This function replaces a minimum common cluster of two rooted, bifurcating
 * phylogenetic trees by a unique taxon labeling.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class ClusterReduction_Overlapping {

    private ConcurrentHashMap<String, HybridTree[]> labelToCluster = new ConcurrentHashMap<String, HybridTree[]>();
    private ConcurrentHashMap<HybridTree[], Integer> clusterToLevel = new ConcurrentHashMap<HybridTree[], Integer>();
    private int maxLevel = 0;
    private String label;
    private Vector<String> taxaOrdering;

    private HashMap<Integer, BitSet> indexToTaxa = new HashMap<Integer, BitSet>();

    public HybridTree[] run(HybridTree[] trees, ReplacementInfo rI, Vector<String> taxaOrdering) throws Exception {

//		System.out.println("---");

        this.taxaOrdering = taxaOrdering;
        for (int i = 0; i < trees.length; i++) {
            BitSet taxaSet = new BitSet(taxaOrdering.size());
            for (MyNode leaf : trees[i].getLeaves())
                taxaSet.set(taxaOrdering.indexOf(leaf.getLabel()));
            indexToTaxa.put(i, taxaSet);
        }

        BitSet lcaCluster = minimumCommonCluster(trees);

        HybridTree[] clusterTrees = new HybridTree[trees.length];
        if (lcaCluster == null)
            return null;
        else {
            // replace minimal common cluster, replacement is captured in
            // ReplacementInfo

            label = taxaOrdering.get(lcaCluster.nextSetBit(0)) + "'";
            taxaOrdering.add(label);
            for (int i = 0; i < trees.length; i++) {
                BitSet treeCluster = (BitSet) lcaCluster.clone();
                treeCluster.and(indexToTaxa.get(i));
                clusterTrees[i] = replaceNode(trees[i], treeCluster, rI);
            }

            setLevel(trees);
            labelToCluster.put(label, trees);

            // return subtrees displaying the minimal cluster of each tree
            return clusterTrees;
        }

    }

    // finding minimal cluster by a simple post order walk
    private BitSet minimumCommonCluster(HybridTree[] trees) {
        Vector<MyNode> levelNodes = getNodesByLevel(trees[0]);

        Vector<BitSet> commonClusters = new Vector<BitSet>();
        for (MyNode v : levelNodes) {

            if (v.getOutDegree() != 0) {

                BitSet cluster = trees[0].getNodeToCluster().get(v);

                BitSet lcaCluster = (BitSet) cluster.clone();
                for (int i = 1; i < trees.length; i++) {
                    BitSet treeCluster = (BitSet) cluster.clone();
                    treeCluster.and(indexToTaxa.get(i));
                    MyNode lca = trees[i].findLCA(treeCluster);
                    lcaCluster.or(trees[i].getNodeToCluster().get(lca));
                }

                boolean b = true;
                for (int i = 0; i < trees.length; i++) {
                    BitSet treeCluster = (BitSet) lcaCluster.clone();
                    treeCluster.and(indexToTaxa.get(i));
                    if (!trees[i].getClusterSet().contains(treeCluster)
                            || trees[i].getRoot().equals(trees[i].getClusterToNode().get(treeCluster))) {
                        b = false;
                        break;
                    }
                }
                if (b)
                    commonClusters.add(lcaCluster);
            }
        }

        if (!commonClusters.isEmpty()) {
            BitSet minCluster = null;
            for (BitSet cluster : commonClusters) {
                if ((minCluster == null || cluster.cardinality() < minCluster.cardinality()) && cluster.cardinality() > 3)
                    minCluster = cluster;
            }
            return minCluster;
        }

        return null;
    }

    private Vector<MyNode> getNodesByLevel(HybridTree t) {
        HashMap<MyNode, Integer> nodeToLevel = new HashMap<MyNode, Integer>();
        insertNodesRec(t.getRoot(), 0, nodeToLevel);
        Vector<MyNode> nodes = new Vector<MyNode>();
        for (MyNode v : nodeToLevel.keySet())
            nodes.add(v);
        Collections.sort(nodes, new LevelCompararotor(nodeToLevel));
        return nodes;
    }

    public class LevelCompararotor implements Comparator<MyNode> {

        HashMap<MyNode, Integer> nodeToLevel;

        public LevelCompararotor(HashMap<MyNode, Integer> nodeToLevel) {
            this.nodeToLevel = nodeToLevel;
        }

        @Override
        public int compare(MyNode o1, MyNode o2) {
            int l1 = nodeToLevel.get(o1);
            int l2 = nodeToLevel.get(o2);
            if (l1 > l2)
                return -1;
            if (l1 < l2)
                return 1;
            return 0;
        }

    }

    private void insertNodesRec(MyNode v, int level, HashMap<MyNode, Integer> nodeToLevel) {
        nodeToLevel.put(v, level);
        Iterator<MyEdge> it = v.outEdges().iterator();
        while (it.hasNext()) {
            int newLevel = level + 1;
            insertNodesRec(it.next().getTarget(), newLevel, nodeToLevel);
        }
    }

    private HybridTree replaceNode(HybridTree t, BitSet cluster, ReplacementInfo rI) throws Exception {

        // modify input tree
        t.setTaxaOrdering(taxaOrdering);
        MyNode v = t.getClusterToNode().get(cluster);
        HybridTree subtree = t.getSubtree(v, true);

        MyNode newV = t.newNode();
        t.setLabel(newV, label);
        t.deleteSubtree(v, newV, true);

        // keep replacement in mind for reattachment...
        rI.addClusterLabel(label);

        // return subtree displaying the minimal cluster
        HybridTree newTree = new HybridTree(subtree, true, taxaOrdering);
        newTree.setReplacementCharacter(label);
        newTree.update();

        removeOneNodes(t);
        removeOneNodes(newTree);

        return newTree;

    }

    private void removeOneNodes(HybridTree t) {
        for (MyNode v : t.getNodes()) {
            if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
                MyNode p = v.getFirstInEdge().getSource();
                MyNode c = v.getFirstOutEdge().getTarget();
                t.deleteEdge(v.getFirstInEdge());
                t.deleteEdge(v.getFirstOutEdge());
                t.deleteNode(v);
                t.newEdge(p, c);
                removeOneNodes(t);
                break;
            }
        }
    }

    private void setLevel(HybridTree[] cluster) {
        HybridTree t = cluster[0];
        for (MyNode v : t.getLeaves()) {
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

    public String getLabel() {
        return label;
    }

    public Vector<String> getTaxaOrdering() {
        return taxaOrdering;
    }

}
