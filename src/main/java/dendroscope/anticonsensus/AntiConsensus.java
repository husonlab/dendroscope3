/*
 * AntiConsensus.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.anticonsensus;

import dendroscope.core.TreeData;
import dendroscope.util.LCA_LSACalculation;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;

import java.util.*;

/**
 * this class calculates the anti-consensus for a given set of trees
 * <p/>
 * thomas bonfert, 11.2009
 */

public class AntiConsensus {

    private final TreeData[] trees;
    private final Vector<AntiCluster> antiClusters;

    public AntiConsensus(TreeData[] trees) {
        this.trees = trees;
        this.antiClusters = new Vector<>();
    }

    public AntiCluster[] apply() {
        long startTime = new Date().getTime();
        try {
            //check if all inputed trees are on the same set of taxa
            if (!checkTaxasets())
                throw new Exception("ERROR: trees are not on the same set of taxa");
            //determine all clusters of each tree
            Vector<AntiClusterSet> clusters = new Vector<>(this.trees.length);
            Vector<Hashtable<String, Node>> leaves = new Vector<>(this.trees.length);
            Vector<LCA_LSACalculation> lcaCalculations = new Vector<>(this.trees.length);
            for (int i = 0; i < this.trees.length; i++) {
                PhyloTree tmpTree = this.trees[i];
                tmpTree.setName(Integer.toString(i));
                clusters.add(determineClusters(tmpTree));
                leaves.add(collectLeaves(tmpTree));
                lcaCalculations.add(new LCA_LSACalculation(tmpTree));
            }
            long seconds = (new Date().getTime() - startTime);
            System.err.println("Preprocessing required " + seconds / 1000.0 + " seconds");
            //now iterate over all trees
            for (int i = 0; i < trees.length; i++) {
                PhyloTree tree_1 = this.trees[i];
                LCA_LSACalculation lcaCalculation_1 = lcaCalculations.get(i);
                AntiClusterSet clusterSet = clusters.get(i);
                //iterate over all clusters contained in the current tree
                Iterator<AntiCluster> clusterSetIt = clusterSet.getClusters();
                while (clusterSetIt.hasNext()) {
                    AntiCluster cluster = clusterSetIt.next();
                    cluster.addTreeToP1(tree_1);
                    Node v_T1 = calculateV_T(lcaCalculation_1, cluster.getNodes(), true);
                    Node v_T1_Plus = calculateV_T_Plus(v_T1);
                    HashSet<Node> witness = getLeavesBelow(v_T1_Plus);
                    witness.removeAll(cluster.getNodes());
                    cluster.setWitness(witness);
                    for (int j = 0; j < this.trees.length; j++) {
                        PhyloTree tree_2 = this.trees[j];
                        if (!tree_2.equals(tree_1)) {
                            HashSet<Node> witness_T2 = getNodesInOtherTree(tree_1, cluster.getWitness(), leaves, j);
                            HashSet<Node> cluster_T2 = getNodesInOtherTree(tree_1, cluster.getNodes(), leaves, j);
                            LCA_LSACalculation lcaCalculation_2 = lcaCalculations.get(j);
                            Node lcaWitness_T2 = lcaCalculation_2.getLca(witness_T2, false);
                            AntiClusterSet clusterSet_T2 = clusters.get(j);
                            boolean isCluster = clusterSet_T2.contains(cluster);
                            Node v_T2 = calculateV_T(lcaCalculation_2, cluster_T2, isCluster);
                            Node lca = lcaCalculation_2.getLCA(lcaWitness_T2, v_T2);
                            if (!(lca.equals(lcaWitness_T2) || lca.equals(v_T2))) {
                                cluster.addTreeToP2(tree_2);
                            } else {
                                if (!isCluster) {
                                    cluster.getP2().clear();
                                    cluster.getWitness().clear();
                                    removeNeedlessCluster(clusters, cluster);
                                    break;
                                }
                                cluster.addTreeToP1(tree_2);
                                Node v_T2_Plus = calculateV_T_Plus(v_T2);
                                HashSet<Node> leavesBelow = getLeavesBelow(v_T2_Plus);
                                leavesBelow.removeAll(cluster_T2);
                                witness_T2.addAll(leavesBelow);
                                cluster.setWitness(getNodesInOtherTree(tree_2, witness_T2, leaves, i));
                                Vector<PhyloTree> q = cluster.getP2();
                                boolean processNewCluster = false;
                                while (!q.isEmpty()) {
                                    PhyloTree tree_3 = q.get(0);
									int treeNumber = Integer.parseInt(tree_3.getName());
                                    q.remove(0);
                                    HashSet<Node> witness_T3 = getNodesInOtherTree(tree_1, cluster.getWitness(), leaves, treeNumber);
                                    HashSet<Node> cluster_T3 = getNodesInOtherTree(tree_1, cluster.getNodes(), leaves, treeNumber);
                                    LCA_LSACalculation lcaCalculation_3 = lcaCalculations.get(treeNumber);
                                    AntiClusterSet clusterSet_T3 = clusters.get(treeNumber);
                                    isCluster = clusterSet_T3.contains(cluster);
                                    Node lcaWitness_T3 = lcaCalculation_3.getLca(witness_T3, false);
                                    Node v_T3 = calculateV_T(lcaCalculation_3, cluster_T3, isCluster);
                                    Node v_T3_Plus = calculateV_T_Plus(v_T3);
                                    lca = lcaCalculation_3.getLCA(lcaWitness_T3, v_T3);
                                    if (lcaWitness_T3.getId() == 1 || v_T3.getId() == 1 || lca.getId() != 1) {
                                        if (!clusters.get(treeNumber).contains(cluster)) {
                                            processNewCluster = true;
                                            break;
                                        }
                                        cluster.addTreeToP1(tree_3);
                                        cluster.removeTreeFromP2(tree_3);
                                        leavesBelow = getLeavesBelow(v_T3_Plus);
                                        leavesBelow.removeAll(cluster_T3);
                                        witness_T3.addAll(leavesBelow);
                                        cluster.setWitness(getNodesInOtherTree(tree_3, witness_T3, leaves, i));
                                        q = cluster.getP2();
                                    }
                                }
                                if (processNewCluster) {
                                    cluster.getP2().clear();
                                    cluster.getWitness().clear();
                                    removeNeedlessCluster(clusters, cluster);
                                    break;
                                }
                            }

                        }
                    }
                    //the current cluster is an anti-cluster
                    if (cluster.getP2().size() > 0) {
                        antiClusters.add(cluster);

                        //this can be deleted
                        System.out.println("found the following anti-cluster: ");
                        System.out.println("cluster: " + cluster.getConcatenatedTaxa());
                        System.out.println("trees in P1: " + cluster.getP1().size());
                        Vector<PhyloTree> p1 = cluster.getP1();
                        for (PhyloTree aP1 : p1) {
                            System.out.println(aP1.getName());
                        }
                        System.out.println("trees in P2: " + cluster.getP2().size());
                        Vector<PhyloTree> p2 = cluster.getP2();
                        for (PhyloTree aP2 : p2) {
                            System.out.println(aP2.getName());
                        }
                        System.out.println("witness:");
                        HashSet<Node> tmpWitness = cluster.getWitness();
                        for (Node tmpNode : tmpWitness) {
                            System.out.println(tree_1.getLabel(tmpNode));
                        }
                        System.out.println();
                    }
                }
            }
            seconds = (new Date().getTime() - startTime);
            System.err.println("Algorithm required " + seconds / 1000.0 + " seconds");
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

		return antiClusters.toArray(new AntiCluster[0]);
    }

    private boolean checkTaxasets() {
        for (int i = 0; i < this.trees.length; i++) {
            for (int j = i + 1; j < this.trees.length; j++) {
                PhyloTree tmpTree_1 = this.trees[i];
                PhyloTree tmpTree_2 = this.trees[j];
                NodeSet set1 = tmpTree_1.computeSetOfLeaves();
                NodeSet set2 = tmpTree_2.computeSetOfLeaves();
                for (Node tmpNode_1 : set1) {
                    Iterator<Node> setIt2 = set2.iterator();
                    boolean contains = false;
                    while (setIt2.hasNext()) {
                        Node tmpNode_2 = setIt2.next();
                        if (tmpTree_1.getLabel(tmpNode_1).equals(tmpTree_2.getLabel(tmpNode_2))) {
                            contains = true;
                            break;
                        }
                    }
                    if (!contains)
                        return false;
                }
            }
        }
        return true;
    }

    private AntiClusterSet determineClusters(PhyloTree tree) {
        AntiClusterSet clusterSet = new AntiClusterSet();
        for (Node n = tree.getFirstNode(); n != null; n = tree.getNextNode(n)) {
            if (!n.equals(tree.getRoot())) {
                AntiCluster cluster = new AntiCluster(tree, n);
                if (n.getOutDegree() == 0)
                    cluster.add(n);
                else
                    determineClusterRec(n, cluster);
                cluster.setConcatenatedTaxa();
                clusterSet.add(cluster);
            }
        }
        return clusterSet;
    }

    private void determineClusterRec(Node n, AntiCluster cluster) {
        for (Edge f = n.getFirstOutEdge(); f != null; f = n.getNextOutEdge(f)) {
            Node w = f.getTarget();
            if (w.getOutDegree() == 0) {
                cluster.add(w);
            } else
                determineClusterRec(w, cluster);
        }
    }

    private void removeNeedlessCluster(Vector<AntiClusterSet> clusters, AntiCluster cluster) {
        for (AntiClusterSet tmpSet : clusters) {
            tmpSet.remove(cluster);
        }
    }

    private Hashtable<String, Node> collectLeaves(PhyloTree tree) {
        Hashtable<String, Node> leaves = new Hashtable<>();
        for (Node n : tree.computeSetOfLeaves()) {
            leaves.put(tree.getLabel(n), n);
        }
        return leaves;
    }

    /**
     * calculates v_T(C) for any set of nodes
     *
     * @return v_T(nodes)
     */
    private Node calculateV_T(LCA_LSACalculation lcaCalculation, HashSet<Node> nodes, boolean isCluster) {
        if (isCluster) {
            Node returnValue = lcaCalculation.getLca(nodes, false);
            if (returnValue.getFirstInEdge() != null)
                returnValue = returnValue.getFirstInEdge().getSource();
            return returnValue;
        } else
            return (lcaCalculation.getLca(nodes, false));

    }

    private Node calculateV_T_Plus(Node v_T) {
        if (v_T.getOutDegree() >= 3)
            return v_T;
        else if (v_T.getFirstInEdge() != null)
            return v_T.getFirstInEdge().getSource();

        else
            return v_T;
    }

    /**
     * calculates all descending leave nodes of v.
     */
    public HashSet<Node> getLeavesBelow(Node v) {
        HashSet<Node> nodes = new HashSet<>();
        if (v.getOutDegree() == 0) {
            nodes.add(v);
            return nodes;
        } else {
            getLeaveNodesRec(v, nodes);
            return nodes;
        }
    }

    private void getLeaveNodesRec(Node v, HashSet<Node> nodes) {
        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            Node w = f.getTarget();
            if (w.getOutDegree() == 0)
                nodes.add(w);
            else
                getLeaveNodesRec(w, nodes);
        }
    }


    /**
     * the given nodes have to be leaves in the given tree.
     * the function collects these nodes in the tree with index 'treeIndex'
     *
	 */

    private HashSet<Node> getNodesInOtherTree(PhyloTree tree, HashSet<Node> nodes, Vector<Hashtable<String, Node>> leaves,
                                              int treeIndex) {

        HashSet<Node> newNodes = new HashSet<>();
        Hashtable<String, Node> leaveNodes = leaves.get(treeIndex);
        for (Node nextNode : nodes) {
            newNodes.add(leaveNodes.get(tree.getLabel(nextNode)));
        }
        return newNodes;
    }
}
