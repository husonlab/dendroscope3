/**
 * CassAlgorithm.java 
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
package dendroscope.algorithms.levelknet.cass;

import dendroscope.algorithms.levelknet.leo.ClusterSet;
import dendroscope.algorithms.levelknet.leo.DiGraph;
import dendroscope.consensus.Cluster;
import dendroscope.util.DistanceMethods;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;

import java.util.*;

/**
 * new version of the cass algorithm
 * Leo van Iersel, 2012
 */
public class CassAlgorithm {
    /**
     * constructor
     */
    public CassAlgorithm() {

    }

    /**
     * run the algorithm and return all networks obtained for the given set of clusters
     *
     * @param clusters
     * @param networks         all computed networks are returned here
     * @param computeOnlyOne
     * @param progressListener @return number of reticulations
     * @throws CanceledException
     */
    public int apply(Cluster[] clusters, List<PhyloTree> networks, boolean computeOnlyOne, boolean checkTrees, ProgressListener progressListener) throws Exception {

            System.err.println("Clusters:");
            for (Cluster cluster : clusters) {
                for (int t = cluster.nextSetBit(0); t != -1; t = cluster.nextSetBit(t + 1))
                    System.err.print(" " + t);
                System.err.println();
            }
            System.err.println(".");

        ClusterSet clusterSet = new ClusterSet();
        // convert clusters to Leo-clusters and add to cluster set.
        for (Cluster cluster : clusters) {
            clusterSet.addCluster(makeCluster(cluster), cluster.getTreeNumbers());
        }

        // run the minimization algorithm
        Vector leoNetworks = dendroscope.algorithms.levelknet.leo.CassAlgorithm.minSL(clusterSet, progressListener, computeOnlyOne, checkTrees);

        // convert DiGraphs to phylotrees
        PhyloTree tree = null;
        DiGraph diGraph;
        BitSet taxa;
        for (int i = 0; i < leoNetworks.size(); i++) {
            diGraph = (DiGraph) leoNetworks.elementAt(i);
            tree = makePhyloTree(diGraph);
            //System.err.println("Component network:");
            //System.err.println(tree.toString());
            taxa = Cluster.extractTaxa(clusters);
            tree.getRoot().setInfo(new Cluster(taxa));

            check(taxa, tree);

            // check if the tree is already in networks
            boolean isNew = true;
            for (PhyloTree network : networks) {
                List<PhyloTree> twoNetworks = new LinkedList<>();
                twoNetworks.add(tree);
                twoNetworks.add(network);
                // we see two networks as being equal if they have 0 nested labels distance
                double d = DistanceMethods.computeNestedLabelsDistance(twoNetworks);
                if (d < 0.001) {
                    isNew = false;
                }
            }

            if (isNew) {
                networks.add(tree);
                System.err.println("Component network:");
                System.err.println(tree.toString());
            }
        }

        int level = 0;
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            if (v.getInDegree() > 1)
                level += (v.getInDegree() - 1);
        }
        return level;

    }

    /**
     * make a Leo cluster from a cluster
     *
     * @param cluster
     * @return Leo-cluster
     */
    private Vector makeCluster(Cluster cluster) {
        Vector<Integer> vecCluster = new Vector<>();
        for (int taxon = cluster.nextSetBit(0); taxon != -1; taxon = cluster.nextSetBit(taxon + 1)) {
            vecCluster.add(taxon);
        }
        return vecCluster;
    }

    /**
     * make a phylo tree from a Leo DiGraph
     *
     * @param diGraph
     * @return phylo tree
     */
    private PhyloTree makePhyloTree(DiGraph diGraph) {
        PhyloTree tree = new PhyloTree();

        //System.err.println("Network computed by Cass algorithm:");
        //System.err.println(diGraph.toString());
        Map<DiGraph, Node> mapDiGraphNode2TreeNode = new HashMap<>();
        processNodes(diGraph, tree, mapDiGraphNode2TreeNode);
        diGraph.cleanDiGraph();

        processArcs(diGraph, tree, mapDiGraphNode2TreeNode);
        diGraph.cleanDiGraph();

        for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
            if (v.getInDegree() == 0)
                tree.setRoot(v);
        }

        for (Edge e = tree.getFirstEdge(); e != null; e = tree.getNextEdge(e)) {
            tree.setSpecial(e, e.getTarget().getInDegree() > 1);
        }
        return tree;
    }

    /**
     * copy nodes from DiGraph to PhyloTree
     *
     * @param diGraphNode
     * @param tree
     * @param mapDiGraphNode2TreeNode mapping of DiGraph nodes to PhyloTree nodes
     */
    private void processNodes(DiGraph diGraphNode, PhyloTree tree, Map<DiGraph, Node> mapDiGraphNode2TreeNode) {
        if (mapDiGraphNode2TreeNode.get(diGraphNode) == null) {
            Node v = tree.newNode();
            mapDiGraphNode2TreeNode.put(diGraphNode, v);
            int taxon = diGraphNode.label;
            if (taxon > 0)    // this is a leaf
            {
                Cluster cluster = new Cluster();
                cluster.set(taxon);
                v.setInfo(cluster);
                tree.setLabel(v, "" + cluster);
                //System.err.println("Leaf:     " + v);
            } else // internal node
            {
                //System.err.println("Internal: " + v);
                for (int c = 0; c < diGraphNode.outdeg; c++) {
                    processNodes(diGraphNode.children.elementAt(c), tree, mapDiGraphNode2TreeNode);
                }
            }
        }
    }

    /**
     * copy arcs from DiGraph to PhyloTree
     *
     * @param diGraphNode
     * @param tree
     * @param mapDiGraphNode2TreeNode
     */
    private void processArcs(DiGraph diGraphNode, PhyloTree tree, Map mapDiGraphNode2TreeNode) {
        Stack<DiGraph> stack = new Stack<>();
        Set<DiGraph> seen = new HashSet<>();

        stack.push(diGraphNode);
        seen.add(diGraphNode);

        while (stack.size() > 0) {
            diGraphNode = stack.pop();
            Node v = (Node) mapDiGraphNode2TreeNode.get(diGraphNode);
            for (int c = 0; c < diGraphNode.outdeg; c++) {
                Node w = (Node) mapDiGraphNode2TreeNode.get(diGraphNode.children.elementAt(c));
                tree.newEdge(v, w);
                //System.err.println("Edge: " + v + " -> " + w);
                if (!seen.contains(diGraphNode.children.elementAt(c))) {
                    stack.push(diGraphNode.children.elementAt(c));
                    seen.add(diGraphNode.children.elementAt(c));
                }
            }
        }
    }

    /**
     * performs some sanity checks on the network
     *
     * @param taxa
     * @param tree
     */
    private void check(BitSet taxa, PhyloTree tree) throws Exception {
        BitSet seen = new BitSet();
        StringBuilder buf = new StringBuilder();
        for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
            Cluster cluster = (Cluster) v.getInfo();
            if (v.getInDegree() == 0 && v != tree.getRoot()) {
                buf.append("\nAdditional root node: ").append(v);
            }
            if (v.getOutDegree() == 0 && (cluster == null || cluster.cardinality() == 0)) {
                buf.append("\nUnlabeled leaf node: ").append(v);
            }
            if (v.getOutDegree() > 0 && v != tree.getRoot() && cluster != null && cluster.cardinality() > 0) {
                buf.append("\nLabeled internal node: ").append(v);
            }
            if (v == tree.getRoot() && (cluster == null || !cluster.equals(taxa))) {
                buf.append("\nRoot not labeled by all taxa");
            }
            if (cluster != null) {
                if (Cluster.intersection(seen, cluster).cardinality() > 0) {
                    buf.append("\nMultiple occurrence of taxa in: ").append(cluster).append(" (already saw: ").append(seen).append(")");
                }
                if (v != tree.getRoot())
                    seen.or(cluster);
            }
        }
        if (Cluster.setminus(taxa, seen).cardinality() > 0) {
            buf.append("\nMissing taxa: ").append(Cluster.setminus(taxa, seen));
        }
        if (Cluster.setminus(seen, taxa).cardinality() > 0) {
            buf.append("\nAdditional taxa: ").append(Cluster.setminus(seen, taxa));
        }
        if (buf.toString().length() > 0) {
            String message = "Error(s) in component computed by Cass algorithm:" + buf.toString() + "\n";
            System.err.println(message);
            throw new Exception(message);
        }
    }
}
