/*
 *   LayoutUnoptimized.java Copyright (C) 2020 Daniel H. Huson
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
package dendroscope.embed;

import dendroscope.consensus.LSATree;
import jloda.graph.*;
import jloda.phylo.PhyloTree;
import jloda.util.ProgressListener;

import java.util.*;


/**
 * this algorithm set the LSA children to reflect the trivial embedding
 * Daniel Huson, 7.2009
 */
public class LayoutUnoptimized implements ILayoutOptimizer {
    /**
     * compute standard embedding
     *
     * @param tree
     * @param progressListener
     */
    public void apply(PhyloTree tree, ProgressListener progressListener) {
        if (tree.getRoot() == null || tree.getSpecialEdges().size() == 0) {
            tree.getNode2GuideTreeChildren().clear();
            return; // if this is a tree, don't need LSA guide tree
        }

        //System.err.println("Maintaining current embedding");

        boolean isTransferNetwork = isTransferNetwork(tree);

        NodeArray<Node> retNode2GuideParent = new NodeArray<Node>(tree);

        if (isTransferNetwork) {
            tree.getNode2GuideTreeChildren().clear();
            for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
                List<Node> children = new LinkedList<Node>();
                for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                    if (!tree.isSpecial(e) || tree.getWeight(e) > 0) {
                        children.add(e.getTarget());
                        retNode2GuideParent.put(e.getTarget(), e.getSource());
                    }

                }
                tree.getNode2GuideTreeChildren().put(v, children);

            }
        } else // must be combining network
        {
            LSATree.computeLSAOrdering(tree, retNode2GuideParent); // maps reticulate nodes to lsa nodes

            // compute preorder numbering of all nodes
            NodeIntegerArray ordering = new NodeIntegerArray(tree);
            computePreOrderNumberingRec(tree, tree.getRoot(), new NodeSet(tree), ordering, 0);
            reorderLSAChildren(tree, ordering);
        }
    }

    /**
     * recursively compute the pre-ordering numbering of all nodes below v
     *
     * @param v
     * @param visited
     * @param ordering
     * @param number
     * @return last number assigned
     */
    private int computePreOrderNumberingRec(PhyloTree tree, Node v, NodeSet visited, NodeIntegerArray ordering, int number) {
        if (!visited.contains(v)) {
            visited.add(v);
            ordering.set(v, ++number);

            // todo: use this to label by order:
            if (false) {
                if (tree.getLabel(v) == null)
                    tree.setLabel(v, "o" + number);
                else
                    tree.setLabel(v, tree.getLabel(v) + "_o" + number);
            }

            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node w = e.getTarget();
                number = computePreOrderNumberingRec(tree, w, visited, ordering, number);
            }
        }
        return number;
    }

    /**
     * reorder LSA children of each node to reflect the topological embedding of the network
     *
     * @param tree
     * @param ordering
     */
    private void reorderLSAChildren(PhyloTree tree, final NodeIntegerArray ordering) {
        // System.err.println("------ v="+v);
        for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
            List<Node> children = tree.getNode2GuideTreeChildren().getValue(v);
            if (children != null) {
                /*
                System.err.println("LSA children old:");
                for(Node u:children) {
                    System.err.println(" "+u+" order: "+ordering.get(u));
                }
                */
                SortedSet<Node> sorted = new TreeSet<Node>(new Comparator<Node>() {

                    public int compare(Node v1, Node v2) {
                        if (ordering.get(v1) < ordering.get(v2))
                            return -1;
                        else if (ordering.get(v1) > ordering.get(v2))
                            return 1;
                        if (v1.getId() != v2.getId())
                            System.err.println("ERROR in sort");
                        // different nodes must have different ordering values!
                        return 0;
                    }
                });
                sorted.addAll(children);
                List<Node> list = new LinkedList<Node>();
                list.addAll(sorted);
                tree.getNode2GuideTreeChildren().put(v, list);
                /*
                System.err.println("LSA children new:");
                 for(Node u: list) {
                     System.err.println(u+" order: "+ordering.get(u));
                 }
                System.err.println();
                */
            }
        }
    }

    /**
     * does network look like a transfer network?
     *
     * @param tree
     * @return true, if is transfer network
     */
    public static boolean isTransferNetwork(PhyloTree tree) {
        boolean isTransferNetwork = false;
        for (Edge e : tree.getSpecialEdges()) {
            if (tree.getWeight(e) != 0) {
                isTransferNetwork = true;
                break;
            }
        }
        return isTransferNetwork;
    }
}

