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

package dendroscope.triplets;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeSet;
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

    //todo: extend to networks

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
        NodeSet nodes = (tree).getNodes();
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
                                for (int i3 = 0; i3 < leavesRemaining.size(); i3++) {
                                    (Triplets).addOne(tree.getNodeId(aSon), tree.getNodeId(sonBi), tree.getNodeId(leavesRemaining.get(i3)));
                                    //System.out.println((Triplets).getValue(tree.getNodeId(son.get(i1)),tree.getNodeId(sonBis.get(i2)),tree.getNodeId(leavesRemaining.get(i3))));

                                }
                            }
                        }
                    }
                }
            }
        }
    }


}
