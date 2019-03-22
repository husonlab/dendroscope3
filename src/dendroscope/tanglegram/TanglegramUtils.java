/*
 * GeneralMethods.java
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
package dendroscope.tanglegram;


import dendroscope.consensus.Split;
import dendroscope.consensus.SplitSystem;
import dendroscope.consensus.Taxa;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;

import java.util.*;

/**
 * stores methods that are of general use and not only applicable for tanglegrams
 * <p/>
 * Celine Scornavacca, Franziska Zickmann 7.2010
 */
public class TanglegramUtils {
    /**
     * computes the number of crossings
     *
     * @return crossingNum
     */

    public static int computeCrossingNum(List<String> v1, List<String> v2) {
        int crossingNum = 0;

        Map<String, Integer> v2_m = new HashMap<>();
        for (int i = 0; i < v2.size(); i++)
            v2_m.put(v2.get(i), i);

        for (int i = 0; i < v1.size() - 1; i++) {
            String first_v1 = v1.get(i);
            if (v2_m.get(first_v1) != null) {    // otherwise null pointer exc with if taxa sets unequal
                int first_v2 = v2_m.get(first_v1);
                for (int j = i + 1; j < v1.size(); j++) {
                    String second_v1 = v1.get(j);
                    if (v2_m.get(second_v1) != null) {
                        int second_v2 = v2_m.get(second_v1);
                        if ((i < j && first_v2 > second_v2) || (i > j && first_v2 < second_v2))
                            crossingNum++;
                    }

                }
            }

        }
        return crossingNum;

    }


    /**
     * compute the crossings in case of many-to-many connections and/or different taxon sets
     *
     * @param v1
     * @param v2
     * @param taxCon
     * @return
     */

    public static int compCrossingsMany2Many(List<String> v1, List<String> v2, Map<String, List<String>> taxCon) {
        int crossingNum = 0;

        Map<String, Integer> v2_m = new HashMap<>();
        for (int i = 0; i < v2.size(); i++) {
            v2_m.put(v2.get(i), i);
        }

        for (int i = 0; i < v1.size() - 1; i++) {
            String first_v1 = v1.get(i);
            List<String> connectList1 = taxCon.get(first_v1);
            if (connectList1 != null) {
                for (String partner1 : connectList1) {
                    if (v2_m.get(partner1) != null) {
                        int first_v2 = v2_m.get(partner1);
                        for (int j = i + 1; j < v1.size(); j++) {
                            String second_v1 = v1.get(j);
                            List<String> connectList2 = taxCon.get(second_v1);
                            if (connectList2 != null) {
                                for (String partner2 : connectList2) {
                                    if (v2_m.get(partner2) != null) {
                                        int second_v2 = v2_m.get(partner2);
                                        if ((i < j && first_v2 > second_v2) || (i > j && first_v2 < second_v2)) {
                                            crossingNum++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return crossingNum;
    }

    /**
     * get the order of taxa concerning the lsa tree
     *
     * @param tree
     * @param v
     * @param leavesList
     * @return
     */

    public static void getLsaOrderRec(PhyloTree tree, Node v, List<String> leavesList) {
        if (tree.computeSetOfLeaves().contains(v)) {
            leavesList.add(tree.getLabel(v));
        } else {
            List<Node> lsaChildren = tree.getNode2GuideTreeChildren().get(v);
            for (Node w : lsaChildren) {
                getLsaOrderRec(tree, w, leavesList);
            }
        }
    }


    public static List<String> adaptLSAorder(Node v, Node w, List<String> originalOrder, Map<Node, List<String>> node2leavesBelow) {

        final List<String> taxaBelowV = new LinkedList<>();
        TanglegramUtils.getLsaOrderRec((PhyloTree) v.getOwner(), v, taxaBelowV);

        //System.err.println("            taxOrderGen v" + taxaBelowV.toString());

        final List<String> taxaBelowW = new LinkedList<>();
        TanglegramUtils.getLsaOrderRec((PhyloTree) w.getOwner(), w, taxaBelowW);

        if ((taxaBelowV.size() != 0) && (taxaBelowW.size() != 0)) {
            //System.err.println("            taxOrderGen w" + taxOrderGen.toString());


            //System.err.println("originalOrder.size() " + originalOrder.size());

            List<String> newOrder = new LinkedList<>();
            List<String> list1 = node2leavesBelow.get(v);
            List<String> list2 = node2leavesBelow.get(w);

            // now fill the new list with the old one, but change places of the taxa below v and w

            boolean foundFirstSet = false;
            boolean foundSecondSet = false;
            boolean firstEncounter1 = true;
            boolean firstEncounter2 = true;

            for (String currTax : originalOrder) {
                if (list1.contains(currTax)) {
                    foundFirstSet = true;
                } else if (list2.contains(currTax)) {
                    foundSecondSet = true;
                } else {
                    newOrder.add(currTax);
                }

                if (foundFirstSet && firstEncounter1) {
                    // now add all taxa of the second and instead of the first set
                    newOrder.addAll(list2);
                    firstEncounter1 = false;
                }
                if (foundSecondSet && firstEncounter2) {
                    // now add all taxa of the first and instead of the second set
                    newOrder.addAll(list1);
                    firstEncounter2 = false;
                }
            }
            return newOrder;
            //System.err.println("newOrder.size() " + newOrder.size());
        } else
            return originalOrder;

    }

    /*
     This function finds the min and max values w.r.t. the getLsaOrderRec associated to the leaves below each node
     */


    private static void getMinMaxInLSATree(PhyloTree tree, Node v, HashMap<String, Integer> orderInLsaTree, HashMap<Node, Integer> minInLsaTree, HashMap<Node, Integer> maxInLsaTree) {

        /*for (Edge edge =v.getFirstOutEdge();edge !=null;edge = v.getNextOutEdge(edge)){
            getMinMaxInLSATree(tree,edge.getTarget(),orderInLsaTree, minInLsaTree,maxInLsaTree);
        }*/
        List<Node> lsaChildren = tree.getNode2GuideTreeChildren().get(v);
        for (Node node : lsaChildren)
            getMinMaxInLSATree(tree, node, orderInLsaTree, minInLsaTree, maxInLsaTree);

        if (v.getOutDegree() == 0) {
            minInLsaTree.put(v, orderInLsaTree.get(tree.getLabel(v)));
            maxInLsaTree.put(v, orderInLsaTree.get(tree.getLabel(v)));
            //System.err.println(" leaf " + tree.getLabel(v) + " " + orderInLsaTree.get(tree.getLabel(v)));
        } else {
            int min = Integer.MAX_VALUE;
            int max = -1;
            for (Node son : lsaChildren) {
                if (minInLsaTree.get(son) < min)
                    min = minInLsaTree.get(son);
                if (maxInLsaTree.get(son) > max)
                    max = maxInLsaTree.get(son);

            }
            minInLsaTree.put(v, min);
            maxInLsaTree.put(v, max);
            //System.err.println(" int " + " " + min + " " + max);
        }
    }

    /**
     * get the split system for a tanglegram, use taxon IDs of the Map from all trees
     *
     * @return split system for this tanglegram
     */

    public static SplitSystem getSplitSystem(Set<Set<String>> clusters, Map<String, Integer> taxon2ID) {
        final SplitSystem splitSys = new SplitSystem();
        final BitSet activeTaxa = new BitSet();
        for (String s : taxon2ID.keySet()) {
            activeTaxa.set(taxon2ID.get(s));
        }
        for (Set<String> currCluster : clusters) {
            //System.err.println("currCluster " + currCluster);

            final BitSet siteA = new BitSet();

            for (String aCurrCluster : currCluster) {
                if (taxon2ID.get(aCurrCluster) != null)
                    siteA.set(taxon2ID.get(aCurrCluster));
            }

            // todo: this is surely a bug:
            final BitSet complement = (BitSet) activeTaxa.clone();
            complement.andNot(siteA);
            final Split split = new Split(siteA, complement);

            //System.err.println("split " + split);

            if (!splitSys.contains(split)) {
                splitSys.addSplit(split);
            }
        }
        return splitSys;
    }

    /**
     * initialize new matrix in first call, after that only add distances when called with new split system
     *
     * @param D
     * @param ntax
     * @param splits
     * @return new distance matrix
     */

    public static double[][] setMatForDiffSys(double[][] D, int ntax, SplitSystem splits, boolean firstTree) {
        if (D == null) {
            int max_num_nodes = 3 * ntax - 5;
            D = new double[max_num_nodes][max_num_nodes];
            for (int i = 1; i <= ntax; i++)
                for (int j = i + 1; j <= ntax; j++) {
                    double weight = 0;
                    for (Iterator it = splits.iterator(); it.hasNext(); ) {
                        Split split = (Split) it.next();
                        if (split.separates(i, j))
                            weight++;
                    }
                    if (firstTree) {
                        D[i][j] = D[j][i] = (1000 * weight);
                    } else {
                        D[i][j] = D[j][i] = weight;
                    }
                }
        } else {
            for (int i = 1; i <= ntax; i++)
                for (int j = i + 1; j <= ntax; j++) {
                    double weight = 0;
                    for (Iterator it = splits.iterator(); it.hasNext(); ) {
                        Split split = (Split) it.next();
                        if (split.separates(i, j))
                            weight++;
                    }
                    D[i][j] = D[j][i] = D[i][j] + weight;
                }
        }
        return D;
    }

    /**
     * calculates the subtree T(v).
     */

    public static PhyloTree getSubtree(PhyloTree srcTree, Node v) {
        PhyloTree newTree = new PhyloTree();
        Node newNode = newTree.newNode();
        newTree.setRoot(newNode);
        getSubTreeRec(srcTree, newTree, v, newNode);
        return newTree;
    }

    /**
     * recursively does the work
     *
     * @param oldTree
     * @param newTree
     * @param vOld
     * @param vNew
     */
    private static void getSubTreeRec(PhyloTree oldTree, PhyloTree newTree, Node vOld, Node vNew) {
        newTree.setLabel(vNew, oldTree.getLabel(vOld));
        newTree.setInfo(vNew, oldTree.getInfo(vOld));
        for (Edge eOld = vOld.getFirstOutEdge(); eOld != null; eOld = vOld.getNextOutEdge(eOld)) {
            Node wOld = eOld.getTarget();
            Node wNew = newTree.newNode();
            Edge eNew = newTree.newEdge(vNew, wNew);
            newTree.setLabel(eNew, oldTree.getLabel(eOld));
            newTree.setInfo(eNew, oldTree.getInfo(eOld));
            getSubTreeRec(oldTree, newTree, wOld, wNew);
        }
    }

    /**
     * deletes the subtree T(v), including v and the incoming edges to v.
     */

    public static void deleteSubtree(PhyloTree tree, Node v) {
        NodeSet collapsedNodes = getDescendingNodes(tree, v);
        //delete the in edges of v.
        for (Edge e : v.inEdges()) {
            tree.deleteEdge(e);
        }
        for (Node toDel : collapsedNodes) {
            for (Edge e : toDel.outEdges()) {
                tree.deleteEdge(e);
            }
            tree.deleteNode(toDel);
        }

    }

    /**
     * calculates all descending nodes of v, including v.
     */

    public static NodeSet getDescendingNodes(PhyloTree tree, Node v) {
        NodeSet nodes = new NodeSet(tree);
        nodes.add(v);
        getDescendingNodesRec(tree, v, nodes);
        return nodes;
    }

    public static NodeSet getDescendingNodesRec(PhyloTree tree, Node v, NodeSet nodes) {
        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            Node w = f.getTarget();
            nodes.add(w);
            NodeSet tmpSet = getDescendingNodesRec(tree, w, nodes);
            for (Node aTmpSet : tmpSet) {
                nodes.add(aTmpSet);
            }
        }
        return nodes;
    }

    /**
     * get the taxon set of a tree/network plus fake taxa for DC
     *
     * @param tree
     * @return
     */
    public static Taxa getTaxaForTanglegram(PhyloTree tree) {
        Taxa taxa = new Taxa();
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            if (v.getOutDegree() == 0 && tree.getLabel(v) != null) {
                taxa.add(tree.getLabel(v));
            } else if (v.getOutDegree() == 0 && tree.getLabel(v) == null) {
                tree.setLabel(v, "null" + tree.getId(v) + tree.getId(v.getFirstInEdge()));
            }
        }
        return taxa;
    }

    /**
     * optimizes layout along the LSA tree
     *
     * @param tree
     * @param otherOrder
     * @param treeNum
     * @param taxConMap1
     * @param taxConMap2
     */
    public static void lsaOptimization(PhyloTree tree, List<String> otherOrder, int treeNum, Map<String, List<String>> taxConMap1, Map<String, List<String>> taxConMap2) {
        final Map<Node, List<String>> node2LsaLeavesBelow = new HashMap<>();
        final List<String> lsaOrderInLastOpti = new LinkedList<>();
        lsaOptimizationRec(tree, tree.getRoot(), otherOrder, treeNum, taxConMap1, taxConMap2, node2LsaLeavesBelow, lsaOrderInLastOpti);
    }

    /**
     * does swaps along the lsa tree (but only adapts order when doing a swap to reduce time consumption)
     *
     * @param tree
     * @param v
     * @param otherOrder
     * @param treeNum
     * @param taxConMap1 can be simply assigned null, only important for host parasite
     * @param taxConMap2
     */


    private static void lsaOptimizationRec(PhyloTree tree, Node v, List<String> otherOrder, int treeNum, Map<String, List<String>> taxConMap1,
                                           Map<String, List<String>> taxConMap2, Map<Node, List<String>> node2LsaLeavesBelow, List<String> lsaOrderInLastOpti) {
        for (Edge e : v.outEdges()) {
            Node next = e.getOpposite(v);
            lsaOptimizationRec(tree, next, otherOrder, treeNum, taxConMap1, taxConMap2, node2LsaLeavesBelow, lsaOrderInLastOpti);
        }

        // this is only to make it accessible by everyone:

        if (tree.getRoot().equals(v)) {
            lsaOrderInLastOpti.clear();
            getLsaOrderRec(tree, v, lsaOrderInLastOpti);
        }

        lsaOrderInLastOpti.clear();
        TanglegramUtils.getLsaOrderRec(tree, v, lsaOrderInLastOpti);


        // now check if we should swap somewhere to optimize

        List<Node> lsaChildren = tree.getNode2GuideTreeChildren().get(v);

        if (lsaChildren.size() > 1) {

            //System.err.println("lsaChildren " + lsaChildren.size());

            final List<String> currentBestOrder = new LinkedList<>(lsaOrderInLastOpti);

            boolean stopBecauseLoop = false;
            boolean swapped;
            do {
                swapped = false;

                for (int o = 0; o < lsaChildren.size() - 1; o++) {
                    //System.err.println("o " + o);
                    //System.err.println("currentBestOrder " + currentBestOrder.toString());
                    int crossingBefore;

                    if (taxConMap1 == null) {
                        crossingBefore = TanglegramUtils.computeCrossingNum(lsaOrderInLastOpti, otherOrder);
                    } else {
                        if (treeNum == 0) {
                            crossingBefore = TanglegramUtils.compCrossingsMany2Many(lsaOrderInLastOpti, otherOrder, taxConMap1);
                        } else {
                            crossingBefore = TanglegramUtils.compCrossingsMany2Many(lsaOrderInLastOpti, otherOrder, taxConMap2);
                        }
                    }

                    Node temp1 = lsaChildren.get(o);
                    Node temp2 = lsaChildren.get(o + 1);
                    lsaChildren.set(o, temp2);
                    lsaChildren.set(o + 1, temp1);

                    lsaOrderInLastOpti = TanglegramUtils.adaptLSAorder(temp1, temp2, lsaOrderInLastOpti, node2LsaLeavesBelow);

                    //System.err.println("lsaOrderInLastOpti " + lsaOrderInLastOpti.toString());


                    if (currentBestOrder.equals(lsaOrderInLastOpti)) {
                        stopBecauseLoop = true;
                        break;
                    }

                    int crossingAfter;

                    if (taxConMap1 == null) {
                        crossingAfter = TanglegramUtils.computeCrossingNum(lsaOrderInLastOpti, otherOrder);
                    } else {
                        if (treeNum == 0) {
                            crossingAfter = TanglegramUtils.compCrossingsMany2Many(lsaOrderInLastOpti, otherOrder, taxConMap1);
                        } else {
                            crossingAfter = TanglegramUtils.compCrossingsMany2Many(lsaOrderInLastOpti, otherOrder, taxConMap2);
                        }
                    }


                    if (crossingBefore < crossingAfter) {
                        temp1 = lsaChildren.get(o);
                        temp2 = lsaChildren.get(o + 1);
                        lsaChildren.set(o, temp2);
                        lsaChildren.set(o + 1, temp1);

                        lsaOrderInLastOpti = TanglegramUtils.adaptLSAorder(temp1, temp2, lsaOrderInLastOpti, node2LsaLeavesBelow);

                    } else {
                        swapped = true;
                        if (crossingBefore > crossingAfter) {

                            currentBestOrder.clear();
                            currentBestOrder.addAll(lsaOrderInLastOpti);
                        }
                    }
                }

            } while (lsaChildren.size() != 2 && !stopBecauseLoop && swapped);
        }

        final List<String> newOrderTaxList = new LinkedList<>();
        getLsaOrderRec(tree, v, newOrderTaxList);

        if (newOrderTaxList.size() == 0) {                   // can happen if two reti children that assigned to other nodes
            for (Edge e : v.outEdges()) {
                Node next = e.getOpposite(v);
                final List<String> tempOrder = new LinkedList<>();
                getLsaOrderRec(tree, next, tempOrder);
                newOrderTaxList.addAll(tempOrder);
            }
            int newOrdSize = newOrderTaxList.size();
            int encounterOrd = 0;
            for (int i = 0; i < lsaOrderInLastOpti.size(); i++) {      // obtained newOrdList via network structure, have to adapt it to lsa ordering
                if (encounterOrd != 1) {
                    String tax = lsaOrderInLastOpti.get(i);
                    if (newOrderTaxList.contains(tax)) {
                        encounterOrd++;
                    }
                    if (encounterOrd == 1) {
                        newOrderTaxList.clear();
                        for (int j = 0; j < newOrdSize; j++) {
                            newOrderTaxList.add(lsaOrderInLastOpti.get(i + j));
                        }
                    }
                }
            }
        }

        node2LsaLeavesBelow.put(v, newOrderTaxList);   // assigns the order of taxa below v
    }
}


