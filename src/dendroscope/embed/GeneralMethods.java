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

package dendroscope.embed;


import dendroscope.consensus.Split;
import dendroscope.consensus.SplitSystem;
import dendroscope.consensus.Taxa;
import dendroscope.util.LCA_LSACalculation;
import dendroscope.window.TreeViewer;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;

import java.awt.geom.Point2D;
import java.io.*;
import java.util.*;

/**
 * stores methods that are of general use and not only applicable for tanglegrams
 * <p/>
 * Celine Scornavacca, Franziska Zickmann 7.2010
 */
public class GeneralMethods {

    private final Map<Node, List<String>> node2LsaLeavesBelow = new HashMap<>();
    private List<String> lsaOrderInLastOpti = new LinkedList<>();
    boolean debug = false;

    /**
     * map taxa from one tree to the other
     *
     * @param args
     */

    public static Map<String, List<String>> getConnections(String[] args) {
        String fileName = args[0];
        BufferedReader br = null;
        Map<String, List<String>> taxaConnections = new HashMap<>();
        try {
            br = new BufferedReader(new FileReader(new File(fileName)));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                String[] conArr1 = parts[0].split(",");
                String[] conArr2 = parts[1].split(",");
                List<String> conList = new LinkedList<>();
                Collections.addAll(conList, conArr2);
                for (String name1 : conArr1) {
                    if (taxaConnections.keySet().contains(name1)) {
                        List<String> connections = new LinkedList<>();
                        connections.addAll(taxaConnections.get(name1));
                        for (String current : conList) {
                            if (!connections.contains(current)) {
                                connections.add(current);
                            }
                        }
                        taxaConnections.put(name1, connections);
                    } else {
                        taxaConnections.put(name1, conList);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return taxaConnections;
    }

    /**
     * get the connections from tree2 to tree1
     *
     * @param taxCon
     * @param tree2
     * @return
     */
    public static Map<String, List<String>> getReverseConnections(Map<String, List<String>> taxCon, PhyloTree tree2) {
        Map<String, List<String>> taxConnTree2 = new HashMap<>();

        for (Node node : tree2.computeSetOfLeaves()) {
            String leafName = tree2.getLabel(node);
            List<String> connected = new LinkedList<>();

            for (String currTax : taxCon.keySet()) {
                if (taxCon.get(currTax).contains(leafName)) {
                    connected.add(currTax);
                }
            }

            taxConnTree2.put(leafName, connected);
        }

        return taxConnTree2;
    }

    /**
     * check if inner nodes lay over another and adapt drawing
     *
     * @return true if changes has occured
     */

    public static boolean checkForCollisionsAndRemove(TreeViewer viewer) {
        System.err.println("checkForCollisionsAndRemove");
        boolean changes = false;

        Map<Node, Point2D> innerNodes2Loc = new HashMap<>();
        Map<Node, Point2D> allNodes2Loc = new HashMap<>();
        List<Node> forbiddenNodes = new LinkedList<>();
        NodeSet leaveSet = viewer.getPhyloTree().computeSetOfLeaves();

        Iterator<Node> allNodesIt = viewer.getPhyloTree().nodeIterator();
        while (allNodesIt.hasNext()) {      // fill the hash maps and list with nodes with reticulate edges
            Node v = allNodesIt.next();
            allNodes2Loc.put(v, viewer.getLocation(v));
            if (!leaveSet.contains(v)) {
                innerNodes2Loc.put(v, viewer.getLocation(v));
            }
            Iterator<Edge> outEdgesIt = v.getOutEdges();
            int found = 0;
            while (outEdgesIt.hasNext() && found == 0) {
                Edge e = outEdgesIt.next();
                if (viewer.getPhyloTree().isSpecial(e)) {
                    forbiddenNodes.add(v);
                    found = 1;
                }
            }
        }

        changes = checkForCollisionsAndRemoveRec(viewer, innerNodes2Loc, allNodes2Loc, forbiddenNodes, viewer.getPhyloTree().getRoot(), changes);
        if (changes) {
            for (Node currentNode : innerNodes2Loc.keySet()) {
                viewer.setLocation(currentNode, innerNodes2Loc.get(currentNode));
            }
        }

        return changes;
    }


    /**
     * recursively check all inner nodes for collisions with others
     *
     * @param viewer
     * @param innerNodes2Loc
     * @param allNodes2Loc
     * @param forbiddenNodes
     * @param v
     * @param changes
     * @return true if changes has been made
     */

    public static boolean checkForCollisionsAndRemoveRec(TreeViewer viewer, Map<Node, Point2D> innerNodes2Loc, Map<Node, Point2D> allNodes2Loc, List<Node> forbiddenNodes, Node v, boolean changes) {
        for (Node current : allNodes2Loc.keySet()) {
            boolean allowChange = false;
            if (!v.equals(current) && innerNodes2Loc.get(v).getY() == allNodes2Loc.get(current).getY()) {
                if (!forbiddenNodes.contains(v) && !forbiddenNodes.contains(current)) {
                    allowChange = true;
                } else if (!viewer.getPhyloTree().areAdjacent(v, current)) {
                    allowChange = true;
                }
                if (allowChange) {
                    System.out.println("change of node: " + viewer.getPhyloTree().getId(v));
                    double newX = (innerNodes2Loc.get(v).getX() - 0.3);
                    double newY = (innerNodes2Loc.get(v).getY() + 0.3);
                    innerNodes2Loc.get(v).setLocation(newX, newY);          // update coordinates
                    allNodes2Loc.get(v).setLocation(newX, newY);
                    changes = true;
                    checkForCollisionsAndRemoveRec(viewer, innerNodes2Loc, allNodes2Loc, forbiddenNodes, viewer.getPhyloTree().getRoot(), changes); // check all over again
                }

            }
        }
        // if arrived here, no collisions has been found with v or v is forbidden, so check the next node:
        Iterator<Node> getNextNodeIt = innerNodes2Loc.keySet().iterator();
        int stop = 0;
        Node nextNode = null;
        while (getNextNodeIt.hasNext() && stop == 0) {
            Node currNode = getNextNodeIt.next();
            if (v.equals(currNode)) {
                stop = 1;
                if (getNextNodeIt.hasNext()) {
                    nextNode = getNextNodeIt.next();
                }
            }
        }
        if (nextNode != null) {
            checkForCollisionsAndRemoveRec(viewer, innerNodes2Loc, allNodes2Loc, forbiddenNodes, nextNode, changes);
        }
        return changes;
    }

    /**
     * get the bottom up order of nodes + assign get(ID) to the info of each node (if not turned off)
     *
     * @param tree
     * @param wantInfo
     * @return
     */
    public static List<Node> postOrderTraversal(PhyloTree tree, boolean wantInfo) {

        List<Node> postOrder = new LinkedList<>();
        Stack<Node> doneNodes = new Stack<>();
        postOrder = postOrderRec(tree, tree.getRoot(), postOrder, doneNodes, wantInfo);
        return postOrder;
    }

    public static List<Node> postOrderRec(PhyloTree tree, Node v, List<Node> postOrder, Stack<Node> doneNodes, boolean wantInfo) {

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            Node w = e.getTarget();
            if (!doneNodes.contains(w)) {
                postOrder = postOrderRec(tree, w, postOrder, doneNodes, wantInfo);
            }
        }

        if (wantInfo) {
            List<Integer> infoNode = new LinkedList<>();

            infoNode.add(tree.getId(v));
            tree.setInfo(v, infoNode);
        }
        postOrder.add(v);
        doneNodes.add(v);

        return postOrder;
    }

    /**
     * get the order of taxa for a network/tree
     *
     * @param tree
     * @return
     */

    public static List<String> getTaxOrder(PhyloTree tree) {

        List<String> taxOrder = new LinkedList<>();
        Stack<Node> doneNodes = new Stack<>();
        taxOrder = getTaxOrderRec(tree, tree.getRoot(), taxOrder, doneNodes);
        //System.err.println();
        return taxOrder;
    }

    public static List<String> getTaxOrderRec(PhyloTree tree, Node v, List<String> taxOrder, Stack<Node> doneNodes) {

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            Node w = e.getTarget();
            if (!doneNodes.contains(w)) {
                if (tree.computeSetOfLeaves().contains(w)) {
                    taxOrder.add(tree.getLabel(w));
                    //System.err.print(" " + tree.getLabel(w));
                    doneNodes.add(w);
                } else {
                    taxOrder = getTaxOrderRec(tree, w, taxOrder, doneNodes);
                }
            }
        }

        return taxOrder;
    }

    /**
     * get the order of taxa that are below a certain node
     *
     * @param tree
     * @param v
     * @param order
     * @return
     */
    public static List<String> getOrderOfAssignedTaxRec(PhyloTree tree, Node v, List<String> order) {

        if (tree.computeSetOfLeaves().contains(v) && tree.getLabel(v) != null) {
            order.add(tree.getLabel(v));
        } else {
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node w = e.getTarget();
                getOrderOfAssignedTaxRec(tree, w, order);
            }
        }

        return order;
    }

    /**
     * swap children of internal nodes at random allowed positions (allowed: networks are still embedable)
     *
     * @param tree
     * @return
     */

    public static PhyloTree swapRandom(PhyloTree tree) {

        Stack<Node> doneNodes = new Stack<>();
        tree = swapRandomRec(tree, tree.getRoot(), doneNodes);

        return tree;
    }

    public static PhyloTree swapRandomRec(PhyloTree tree, Node v, Stack<Node> doneNodes) {

        if (v.getOutDegree() > 1) {
            Iterator<Edge> outEdgesIt = v.getOutEdges();
            boolean foundRetiChild = false;
            while (outEdgesIt.hasNext() && !foundRetiChild) {
                Edge currEdge = outEdgesIt.next();
                if (tree.getSpecialEdges().contains(currEdge)) {
                    foundRetiChild = true;
                }
            }
            if (!foundRetiChild) {   // nextNode is a node where it is allowed to swap

                double randNum = Math.random();
                if (randNum < 0.5) {
                    tree = swapChildren(tree, v);
                }
            }
        }

        doneNodes.add(v);

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            Node w = e.getTarget();
            if (!doneNodes.contains(w)) {
                tree = swapRandomRec(tree, w, doneNodes);
            }
        }

        return tree;
    }

    /**
     * swap the children of a node
     *
     * @param tree
     * @param v
     * @return
     */
    public static PhyloTree swapChildren(PhyloTree tree, Node v) {

        List<Edge> newEdgeOrder = new LinkedList<>();
        if (v.getFirstInEdge() != null) {
            newEdgeOrder.add(v.getFirstInEdge());
        }
        for (Edge e = v.getLastOutEdge(); e != null; e = v.getPrevOutEdge(e)) {
            newEdgeOrder.add(e);
        }
        tree.rearrangeAdjacentEdges(v, newEdgeOrder);

        return tree;
    }


    /**
     * computes the number of crossings
     *
     * @return crossingNum
     */

    public static Integer computeCrossingNum(List<String> v1, List<String> v2) {


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

    public static Integer compCrossingsMany2Many(List<String> v1, List<String> v2, Map<String, List<String>> taxCon) {

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
     * returns all permutations of a given array
     *
     * @param nodeArr
     * @return
     */

    public static List<Node[]> permute(Node[] nodeArr) {
        //System.err.println("given order: " + Arrays.toString(nodeArr));
        List<Node[]> permList = new LinkedList<>();
        int length = nodeArr.length;
        permuteRec(nodeArr, length, permList);
        if (permList.size() != 0) {
            permList.remove(permList.size() - 1);  // the original order is not given back
        }
        return permList;
    }

    private static List<Node[]> permuteRec(Node[] a, int n, List<Node[]> permList) {
        if (n == 1) {
            //System.err.println("permutation:" +  Arrays.toString(a));
            Node[] newPerm = a.clone();
            permList.add(newPerm);
            return permList;
        }
        for (int i = 0; i < n; i++) {
            swap(a, i, n - 1);
            permuteRec(a, n - 1, permList);
            swap(a, i, n - 1);
        }

        return permList;
    }

    // swap the characters at indices i and j

    private static void swap(Node[] a, int i, int j) {
        Node c;
        c = a[i];
        a[i] = a[j];
        a[j] = c;
    }


    /**
     * get the order of taxa concerning the lsa tree
     *
     * @param tree
     * @param v
     * @param leavesList
     * @return
     */

    public static List<String> getLsaOrderRec(PhyloTree tree, Node v, List<String> leavesList) {
        if (tree.computeSetOfLeaves().contains(v)) {
            leavesList.add(tree.getLabel(v));
        } else {
            List<Node> lsaChildren = tree.getNode2GuideTreeChildren().get(v);
            for (Node w : lsaChildren) {
                leavesList = getLsaOrderRec(tree, w, leavesList);
            }
        }

        return leavesList;
    }


    public static List<String> adaptLSAorder(Node v, Node w, List<String> originalOrder, Map<Node, List<String>> node2leavesBelow) {


        List<String> taxaBelowV = new LinkedList<>();
        taxaBelowV = GeneralMethods.getLsaOrderRec((PhyloTree) v.getOwner(), v, taxaBelowV);

        //System.err.println("            taxOrderGen v" + taxaBelowV.toString());

        List<String> taxaBelowW = new LinkedList<>();
        taxaBelowV = GeneralMethods.getLsaOrderRec((PhyloTree) w.getOwner(), w, taxaBelowW);

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

    /*
    this function computes the inner crossing number of a PhyloTree
    */


    public static int getInnerCrossing(PhyloTree tree) {

        int upperBoundNumberOfInnerCrossing = 0;

        List<String> order = new LinkedList<>();
        order = GeneralMethods.getLsaOrderRec(tree, tree.getRoot(), order); //the order in the LSA Tree
        HashMap<String, Integer> orderInLsaTree = new HashMap<>();
        HashMap<Node, Integer> minInLsaTree = new HashMap<>();
        HashMap<Node, Integer> maxInLsaTree = new HashMap<>();

        int o = 0;
        for (String temp : order) {
            orderInLsaTree.put(temp, o);
            o++;
        }
        getMinMaxInLSATree(tree, tree.getRoot(), orderInLsaTree, minInLsaTree, maxInLsaTree);
        for (Edge edge = tree.getFirstEdge(); edge != null; edge = tree.getNextEdge(edge)) {
            if (tree.isSpecial(edge)) {
                Node source = edge.getSource();
                Node target = edge.getTarget();
                int minS = minInLsaTree.get(source);
                int maxS = maxInLsaTree.get(source);
                int minT = minInLsaTree.get(target);
                int maxT = maxInLsaTree.get(target);
                //System.err.println(minS + " " + maxS + " " +minT +  " " + maxT);
                /*if(!(maxS==maxT && minS ==minT)){
                    if(maxS < minT)
                        upperBoundNumberOfInnerCrossing += (Math.abs(minT - maxS) +1);
                    else
                        upperBoundNumberOfInnerCrossing += (Math.abs(minS - maxT) +1);
                }*/
                upperBoundNumberOfInnerCrossing += (Math.min(Math.abs(minT - maxS), Math.abs(minS - maxT)) - 1);
                //System.err.println("upperBoundNumberOfInnerCrossing "+ upperBoundNumberOfInnerCrossing);
            }
        }
        return upperBoundNumberOfInnerCrossing;

    }


    /**
     * get the LSA tree after the DivCon reconstruction - have to make sure that all changes made
     * are taken into account (find right order of lsa children)
     *
     * @param current
     * @param lsaChildren
     * @param lsaNewOrder
     * @param doneNodes
     */

    public static List<Node> getLsaChildrenOrderRec(Node current, List<Node> lsaChildren, List<Node> lsaNewOrder, Stack<Node> doneNodes) {

        // recursive traversal through the tree, check for the current tested node if it is part of lsa children of
        // a certain node v, if yes, add it to the list of reordered lsa children

        if (!doneNodes.contains(current) && lsaChildren.contains(current)) {
            lsaNewOrder.add(current);
        }

        if (lsaChildren.size() != lsaNewOrder.size()) {
            for (Edge e = current.getFirstOutEdge(); e != null; e = current.getNextOutEdge(e)) {
                Node next = e.getOpposite(current);
                getLsaChildrenOrderRec(next, lsaChildren, lsaNewOrder, doneNodes);
            }
        }
        doneNodes.add(current);

        return lsaNewOrder;
    }

    /**
     * get the split system for a tanglegram, use taxon IDs of the Map from all trees
     *
     * @return split system for this tanglegram
     */

    public static SplitSystem getSplitSystem(Set<Set<String>> clusters, Map<String, Integer> taxon2ID) {
        SplitSystem splitSys = new SplitSystem();


        BitSet activeTaxa = new BitSet();
        for (String s : taxon2ID.keySet()) {
            activeTaxa.set(taxon2ID.get(s));
        }
        for (Set<String> currCluster : clusters) {
            //System.err.println("currCluster " + currCluster);

            BitSet siteA = new BitSet();

            for (String aCurrCluster : currCluster) {
                siteA.set(taxon2ID.get(aCurrCluster));
            }

            // todo: this is surely a bug:
            BitSet complement = (BitSet) activeTaxa.clone();
            complement.andNot(siteA);
            Split split = new Split(siteA, complement);

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
     * add a subtree to an existing tree at a certain node
     *
     * @param tree
     * @param subtree
     * @param vTree
     * @param vSub
     * @param info2node
     * @return
     */

    public static PhyloTree addSubtreeRec(PhyloTree tree, PhyloTree subtree, Node vTree, Node vSub, Map<Integer, Node> info2node) {
        tree.setLabel(vTree, subtree.getLabel(vSub));
        tree.setInfo(vTree, subtree.getInfo(vSub));
        info2node.put(((List<Integer>) vTree.getInfo()).get(0), vTree);
        for (Edge e = vSub.getFirstOutEdge(); e != null; e = vSub.getNextOutEdge(e)) {
            Node wSub = e.getTarget();
            Node wTree = tree.newNode();
            Edge eNew = tree.newEdge(vTree, wTree);
            tree.setLabel(eNew, subtree.getLabel(e));
            tree.setInfo(eNew, subtree.getInfo(e));
            addSubtreeRec(tree, subtree, wTree, wSub, info2node);
        }
        return tree;
    }

    /**
     * deletes the subtree T(v), including v and the incoming edges to v.
     */

    public static void deleteSubtree(PhyloTree tree, Node v) {
        NodeSet collapsedNodes = getDescendingNodes(tree, v);
        //delete the in edges of v.
        Iterator<Edge> edgeIt = tree.getInEdges(v);
        while (edgeIt.hasNext()) {
            tree.deleteEdge(edgeIt.next());
        }
        for (Node toDel : collapsedNodes) {
            edgeIt = tree.getOutEdges(toDel);
            while (edgeIt.hasNext()) {
                tree.deleteEdge(edgeIt.next());
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
     * find a node with a certain label
     *
     * @param tree
     * @param label
     * @return target node
     */
    public static Node getNode(PhyloTree tree, String label) {
        //System.err.println("Label of searched node: " + label);
        Node target = null;
        Iterator<Node> nodeIt = tree.nodeIterator();
        while (nodeIt.hasNext()) {
            Node current = nodeIt.next();
            if (tree.getLabel(current) != null && tree.getLabel(current).equals(label)) {
                target = current;
            }
        }
        return target;
    }

    /**
     * find a node with a certain Id
     *
     * @param tree
     * @param nodeId
     * @return target node
     */
    public static Node getNode(PhyloTree tree, Integer nodeId) {

        Node target = null;
        Iterator<Node> nodeIt = tree.nodeIterator();
        while (nodeIt.hasNext()) {
            Node current = nodeIt.next();
            if (tree.getId(current) == (nodeId)) {
                target = current;
            }
        }
        return target;
    }

    /**
     * swap right and left child of a node and its descendants, only works for trees!
     *
     * @param tree
     * @param v
     * @return
     */
    public static PhyloTree reverseSubtreeRec(PhyloTree tree, Node v) {

        List<Edge> newEdgeOrder = new LinkedList<>();
        if (v.getFirstInEdge() != null) {
            newEdgeOrder.add(v.getFirstInEdge());
        }
        for (Edge e = v.getLastOutEdge(); e != null; e = v.getPrevOutEdge(e)) {
            newEdgeOrder.add(e);
        }
        tree.rearrangeAdjacentEdges(v, newEdgeOrder);
        Iterator<Edge> edgeIt = v.getOutEdges();
        while (edgeIt.hasNext()) {
            Node w = edgeIt.next().getTarget();
            tree = reverseSubtreeRec(tree, w);
        }

        return tree;
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


    public void lsaOptimizationRec(PhyloTree tree, Node v, List<String> otherOrder, int treeNum, Map<String, List<String>> taxConMap1, Map<String, List<String>> taxConMap2) {

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            Node next = e.getOpposite(v);
            lsaOptimizationRec(tree, next, otherOrder, treeNum, taxConMap1, taxConMap2);
        }

        // this is only to make it accessible by everyone:

        if (tree.getRoot().equals(v)) {
            List<String> taxOrderGen = new LinkedList<>();
            lsaOrderInLastOpti = GeneralMethods.getLsaOrderRec(tree, v, taxOrderGen);
        }

        List<String> taxOrderGen = new LinkedList<>();
        lsaOrderInLastOpti.clear();
        lsaOrderInLastOpti = GeneralMethods.getLsaOrderRec(tree, v, taxOrderGen);


        // now check if we should swap somewhere to optimize

        List<Node> lsaChildren = tree.getNode2GuideTreeChildren().get(v);

        if (lsaChildren.size() > 1) {

            //System.err.println("lsaChildren " + lsaChildren.size());

            List<String> currentBestOrder = new LinkedList<>();

            currentBestOrder.clear();
            for (String tax1 : lsaOrderInLastOpti) {
                currentBestOrder.add(tax1);
            }

            boolean stopBecauseLoop = false;
            boolean swapped;
            do {
                swapped = false;

                for (int o = 0; o < lsaChildren.size() - 1; o++) {
                    //System.err.println("o " + o);
                    //System.err.println("currentBestOrder " + currentBestOrder.toString());

                    List<String> newOrderList = new LinkedList<>();

                    int crossingBefore;

                    if (taxConMap1 == null) {
                        crossingBefore = GeneralMethods.computeCrossingNum(lsaOrderInLastOpti, otherOrder);
                    } else {
                        if (treeNum == 0) {
                            crossingBefore = GeneralMethods.compCrossingsMany2Many(lsaOrderInLastOpti, otherOrder, taxConMap1);
                        } else {
                            crossingBefore = GeneralMethods.compCrossingsMany2Many(lsaOrderInLastOpti, otherOrder, taxConMap2);
                        }
                    }


                    Node temp1 = lsaChildren.get(o);
                    Node temp2 = lsaChildren.get(o + 1);
                    lsaChildren.set(o, temp2);
                    lsaChildren.set(o + 1, temp1);


                    lsaOrderInLastOpti = GeneralMethods.adaptLSAorder(temp1, temp2, lsaOrderInLastOpti, node2LsaLeavesBelow);


                    //System.err.println("lsaOrderInLastOpti " + lsaOrderInLastOpti.toString());


                    if (currentBestOrder != null && currentBestOrder.equals(lsaOrderInLastOpti)) {
                        stopBecauseLoop = true;
                        break;
                    }

                    int crossingAfter;

                    if (taxConMap1 == null) {
                        crossingAfter = GeneralMethods.computeCrossingNum(lsaOrderInLastOpti, otherOrder);
                    } else {
                        if (treeNum == 0) {
                            crossingAfter = GeneralMethods.compCrossingsMany2Many(lsaOrderInLastOpti, otherOrder, taxConMap1);
                        } else {
                            crossingAfter = GeneralMethods.compCrossingsMany2Many(lsaOrderInLastOpti, otherOrder, taxConMap2);
                        }
                    }


                    if (crossingBefore < crossingAfter) {
                        temp1 = lsaChildren.get(o);
                        temp2 = lsaChildren.get(o + 1);
                        lsaChildren.set(o, temp2);
                        lsaChildren.set(o + 1, temp1);

                        lsaOrderInLastOpti = GeneralMethods.adaptLSAorder(temp1, temp2, lsaOrderInLastOpti, node2LsaLeavesBelow);

                    } else {
                        swapped = true;
                        if (crossingBefore > crossingAfter) {

                            currentBestOrder.clear();
                            for (String tax : lsaOrderInLastOpti) {
                                currentBestOrder.add(tax);
                            }

                        }
                    }
                }

            } while (lsaChildren.size() != 2 && !stopBecauseLoop && swapped);

        }

        List<String> newOrderTaxList = new LinkedList<>();
        newOrderTaxList = GeneralMethods.getLsaOrderRec(tree, v, newOrderTaxList);

        if (newOrderTaxList.size() == 0) {                   // can happen if two reti children that assigned to other nodes
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node next = e.getOpposite(v);
                List<String> tempOrder = new LinkedList<>();
                tempOrder = GeneralMethods.getLsaOrderRec(tree, next, tempOrder);
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


    /**
     * returns an integer number in an intervall [low,high]
     *
     * @param low
     * @param high
     * @return
     */

    public static int getRandomNumber(int low, int high) {

        double decNum = Math.random();

        return (int) Math.round((decNum * (high - low)) + low);

    }


    public static String[] MixUpArray(String inarray[]) {
        int iSize = inarray.length;
        String outarray[] = new String[iSize];
        boolean inserted;
        for (String anInarray : inarray) {
            inserted = false;
            while (!inserted) {
                int randomPosition = getRandomNumber(0, iSize - 1);
                if (outarray[randomPosition] == null) {
                    outarray[randomPosition] = anInarray;
                    inserted = true;

                }
            }
        }

        return outarray;
    }

    public static void adaptTreeToLsa(PhyloTree tree, List<Node> postOrder) {

        for (Node current : postOrder) {
            List<Node> lsaChildren = tree.getNode2GuideTreeChildren().get(current);

            List<Edge> newEdgeOrder = new LinkedList<>();
            if (current.getFirstInEdge() != null) {
                newEdgeOrder.add(current.getFirstInEdge());
            }

            for (Node thisLsa : lsaChildren) {
                for (Edge e = current.getFirstOutEdge(); e != null; e = current.getNextOutEdge(e)) {
                    if (e.getOpposite(current).equals(thisLsa)) {
                        newEdgeOrder.add(e);
                    }
                }
            }
            tree.rearrangeAdjacentEdges(current, newEdgeOrder);
        }
    }


    /**
     * this function prints the ILP formulation of the tanglegram problems (to solve with lp_solve)
     * in the file print. Use only when tree1 and tree2 are bonary trees.
     *
     * @param tree1
     * @param tree2
     * @param print
     * @return
     */

    static void printlILPFormulation(PhyloTree tree1, PhyloTree tree2, PrintStream print) {

        LCA_LSACalculation LCA1 = new LCA_LSACalculation(tree1, false);
        LCA_LSACalculation LCA2 = new LCA_LSACalculation(tree2, false);

        int numberInnerNodeTree1 = tree1.getNumberOfNodes() - tree1.getNumberOfLeaves();
        int numberInnerNodeTree2 = tree2.getNumberOfNodes() - tree2.getNumberOfLeaves();

        int k_par[][] = new int[numberInnerNodeTree1 + 1][numberInnerNodeTree2 + 1];
        int k_crossing[][] = new int[numberInnerNodeTree1 + 1][numberInnerNodeTree2 + 1];

        NodeSet leaves1_temp = tree1.computeSetOfLeaves();


        Node[] leaves1 = new Node[leaves1_temp.size()];
        int tempI = 0;
        Iterator<Node> tempIt = leaves1_temp.iterator();
        while (tempIt.hasNext()) {
            leaves1[tempI++] = tempIt.next();
        }


        NodeSet leaves2 = tree2.computeSetOfLeaves();

        Map<Node, Integer> nodeToInt1 = new HashMap<>();
        Map<Node, Integer> nodeToInt2 = new HashMap<>();

        NodeSet nodes1 = tree1.getNodes();
        NodeSet nodes2 = tree2.getNodes();

        tempIt = nodes1.iterator();
        int tempIndex = 1;
        while (tempIt.hasNext()) {
            Node tempNode = tempIt.next();
            if (tempNode.getOutDegree() != 0) {
                nodeToInt1.put(tempNode, tempIndex);
                //tree1.setLabel(tempNode,Integer.toString(tempIndex));
                tempIndex++;
            }
        }

        tempIt = nodes2.iterator();
        tempIndex = 1;
        while (tempIt.hasNext()) {
            Node tempNode = tempIt.next();
            if (tempNode.getOutDegree() != 0) {
                nodeToInt2.put(tempNode, tempIndex);
                //tree2.setLabel(tempNode,Integer.toString(tempIndex));
                tempIndex++;
            }
        }


        for (int it1 = 0; it1 < leaves1.length - 1; it1++) {
            Node a = leaves1[it1];
            String label_a = tree1.getLabel(a);

            Iterator<Node> it3 = leaves2.iterator();
            int index_b = -1;
            int temp = 0;
            Node b = null;
            Node tempNode;
            while (it3.hasNext()) {
                tempNode = it3.next();
                if (tree2.getLabel(tempNode).equalsIgnoreCase(label_a)) {
                    b = tempNode;
                    index_b = temp;
                    break;
                }
                temp++;

            }
            if (index_b != -1) {
                for (int it2 = it1 + 1; it2 < leaves1.length; it2++) {
                    Node c = leaves1[it2];
                    String label_c = tree1.getLabel(c);
                    Node[] temp1 = new Node[2];
                    temp1[0] = a;
                    temp1[1] = c;
                    Node lca1 = LCA1.getLca(temp1, false);

                    int index_d = -1;
                    temp = 0;
                    Node d = null;
                    it3 = leaves2.iterator();
                    while (it3.hasNext()) {
                        tempNode = it3.next();
                        if (tree2.getLabel(tempNode).equalsIgnoreCase(label_c)) {
                            d = tempNode;
                            index_d = temp;
                            break;
                        }
                        temp++;

                    }

                    if (index_d != -1) {
                        temp1[0] = b;
                        temp1[1] = d;
                        Node lca2 = LCA2.getLca(temp1, false);
                        int indexTree1 = nodeToInt1.get(lca1);
                        int indexTree2 = nodeToInt2.get(lca2);
                        if (index_b < index_d) {
                            k_par[indexTree1][indexTree2]++;
                        } else if (index_b > index_d) {
                            k_crossing[indexTree1][indexTree2]++;
                        }
                    }
                }
            }
        }
        print.print("min: ");

        for (int v = 1; v <= numberInnerNodeTree1; v++) {
            for (int w = 1; w <= numberInnerNodeTree2; w++) {

                if (k_par[v][w] != 0)
                    print.print(Integer.toString(k_par[v][w]) + "y_u" + Integer.toString(v) + "_w" + Integer.toString(w) + "+ ");
                if (k_crossing[v][w] != 0)
                    print.print(Integer.toString(k_crossing[v][w]) + "-" + Integer.toString(k_crossing[v][w]) + " y_u" + Integer.toString(v) + "_w" + Integer.toString(w) + " +");
            }
        }
        print.println("0;");

        print.print("C1: ");


        for (int v = 1; v <= numberInnerNodeTree1; v++) {
            for (int w = 1; w <= numberInnerNodeTree2; w++) {
                print.println("y_u" + Integer.toString(v) + "_w" + Integer.toString(w) + "<= 2-x_u" + Integer.toString(v) + "-x_w" + Integer.toString(w) + ";");
                print.println("y_u" + Integer.toString(v) + "_w" + Integer.toString(w) + "<= x_u" + Integer.toString(v) + "+x_w" + Integer.toString(w) + ";");
                print.println("y_u" + Integer.toString(v) + "_w" + Integer.toString(w) + ">= -x_u" + Integer.toString(v) + "+x_w" + Integer.toString(w) + ";");
                print.println("y_u" + Integer.toString(v) + "_w" + Integer.toString(w) + ">= x_u" + Integer.toString(v) + "-x_w" + Integer.toString(w) + ";");

            }
        }

        print.print("bin  ");
        for (int v = 1; v <= numberInnerNodeTree1; v++) {
            for (int w = 1; w <= numberInnerNodeTree2; w++) {
                print.print("y_u" + Integer.toString(v) + "_w" + Integer.toString(w) + ", ");
            }
        }
        for (int v = 1; v <= numberInnerNodeTree1; v++)
            print.print("x_u" + Integer.toString(v) + ", ");

        for (int w = 1; w <= numberInnerNodeTree2 - 1; w++)
            print.print("x_w" + Integer.toString(w) + ", ");
        print.print("x_w" + Integer.toString(numberInnerNodeTree2) + ";");
    }

}


