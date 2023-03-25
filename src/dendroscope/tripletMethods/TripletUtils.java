/*
 *   TripletUtils.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.tripletMethods;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.util.Iterator;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: scornava
 * Date: Jun 21, 2010
 * Time: 10:27:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class TripletUtils {

    /**
     * calculates all leaf nodes of the subtree of v, including v.
     */
    public static Vector<Node> getClade(PhyloTree tree, Node v) {
        Vector<Node> nodes = new Vector<>();
        if (v.getOutDegree() == 0)
            nodes.add(v);
        getCladeRec(tree, v, nodes);
        return nodes;
    }

    private static void getCladeRec(PhyloTree tree, Node v, Vector<Node> nodes) {
        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            Node w = f.getTarget();
            if (w.getOutDegree() == 0)
                nodes.add(w);
            getCladeRec(tree, w, nodes);
        }
    }


    /**
     * this function associates to the node v the clade below  (it updates nodesToClade)
     */

    static void setClade(PhyloTreeTri tree, Node node) {
        Vector<Node> leaves = getClade(tree, node);
        tree.setCladeNode(node, leaves);
    }

    /**
     * this function associates to each node of the tree its clade (it updates nodesToClade)
     */


    static void setClade(PhyloTreeTri tree) {
        Vector<Node> nodes = (tree).getAllNodes();
        for (Node node : nodes) {
            setClade(tree, node);
        }
    }

    /**
     * this function associates to the node v the "clade" above  (it updates nodesToCladeAbove)
     */

    static void setCladeAbove(PhyloTreeTri tree, Node node) {
        Vector<Node> sons = tree.getSons(node);
        Vector<Node> aboveNode = tree.getCladeNodeAbove(node);
        for (Node nodeSon : sons) {
            Iterator<Node> nodeIt2 = sons.iterator();
            Vector<Node> tempAboveSon = new Vector<>();
            while (nodeIt2.hasNext()) {
                Node nodeSon2 = nodeIt2.next();
                if (nodeSon2 != nodeSon) {
                    tempAboveSon.addAll(tree.getCladeNode(nodeSon2));
                }

            }
            tempAboveSon.addAll(aboveNode);
            tree.setCladeNodeAbove(nodeSon, tempAboveSon);
            setCladeAbove(tree, nodeSon);
        }

    }


    /**
     * this function associates to each node of the tree the "clade" above(it updates nodesToCladeAbove)
     */

    static void setCladeAbove(PhyloTreeTri tree) {
        tree.getNodesToCladeAbove().put(tree.getRoot(), new Vector<Node>());
        setCladeAbove(tree, tree.getRoot());
    }

    /*this function computes the set of triplet of a tree;*/

    static void setTriplets(PhyloTreeTri tree, TripletMatrix Triplets) {
        Vector<Node> innerNodes = (tree).getInnerNodes();
        for (Node node : innerNodes) {
            if ((node.getInDegree() != 0) && (node.getOutDegree() != 0)) { // not the root nor leaf
                Vector<Node> leavesRemaining = tree.getCladeNodeAbove(node);
                Vector<Node> sons = tree.getSons(node);
                for (int j = 0; j < sons.size() - 1; j++) {
                    Vector<Node> son = tree.getCladeNode(sons.get(j));
                    for (int z = j + 1; z < sons.size(); z++) {
                        Vector<Node> sonBis = tree.getCladeNode(sons.get(z));
                        for (Node aSon : son) {
                            for (Node sonBi : sonBis) {
                                for (Node aLeavesRemaining : leavesRemaining) {
                                    (Triplets).addOne(tree.getNodeId(aSon), tree.getNodeId(sonBi), tree.getNodeId(aLeavesRemaining));
                                    //System.out.println((Triplets).get(tree.getNodeId(son.get(i1)),tree.getNodeId(sonBis.get(i2)),tree.getNodeId(leavesRemaining.get(i3))));

                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static void setTriplets(PhyloTreeTri tree, TripletSet Triplets) {
        Vector<Node> innerNodes = (tree).getInnerNodes();
        for (Node node : innerNodes) {
            if ((node.getInDegree() != 0) && (node.getOutDegree() != 0)) { // not the root nor leaf
                Vector<Node> leavesRemaining = tree.getCladeNodeAbove(node);
                Vector<Node> sons = tree.getSons(node);
                for (int j = 0; j < sons.size() - 1; j++) {
                    Vector<Node> son = tree.getCladeNode(sons.get(j));
                    for (int z = j + 1; z < sons.size(); z++) {
                        Vector<Node> sonBis = tree.getCladeNode(sons.get(z));
                        for (Node aSon : son) {
                            for (Node sonBi : sonBis) {
                                for (Node aLeavesRemaining : leavesRemaining) {
                                    int l1 = tree.getNodeId(aSon);
                                    int l2 = tree.getNodeId(sonBi);
                                    int l3 = tree.getNodeId(aLeavesRemaining);
                                    if (!Triplets.containsTripletFast(l1, l2, l3)) {
                                        (Triplets).addTripletInList(l1, l2, l3);  //before adding in the list because it adds a triplet
                                        //in the list iff it is not already in the matrix
                                        (Triplets).addTripletInMatrix(l1, l2, l3);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


}
