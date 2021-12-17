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
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeIntArray;
import jloda.graph.NodeSet;
import jloda.phylo.LSAUtils;
import jloda.phylo.PhyloTree;
import jloda.util.Counter;
import jloda.util.IteratorUtils;
import jloda.util.StringUtils;
import jloda.util.progress.ProgressListener;

import java.util.*;
import java.util.stream.Collectors;


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
        if (tree.getRoot() == null || tree.getNumberSpecialEdges() == 0) {
            tree.getLSAChildrenMap().clear();
            return; // if this is a tree, don't need LSA guide tree
        }

        //System.err.println("Maintaining current embedding");

        boolean isTransferNetwork = isTransferNetwork(tree);

        if (isTransferNetwork) {
            tree.getLSAChildrenMap().clear();
            for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
                List<Node> children = new LinkedList<Node>();
                for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                    if (!tree.isSpecial(e) || tree.getWeight(e) > 0) {
                        children.add(e.getTarget());
                    }

                }
                tree.getLSAChildrenMap().put(v, children);

            }
        } else // must be combining network
        {
            if (false) {
                System.err.println("++++++++ Nodes 0:");
                tree.preorderTraversal(tree.getRoot(), v -> true, v -> {
                    if (v.isLeaf())
                        System.err.println(v + " " + tree.getLabel(v));
                    else {
                        System.err.println("Node:" + v.getId());
                        System.err.println("Children:" + StringUtils.toString(v.childrenStream().map(w -> w.getId()).collect(Collectors.toList()), " "));
                        System.err.println("LSA Chld:" + StringUtils.toString(IteratorUtils.asStream(tree.lsaChildren(v)).map(w -> w.getId()).collect(Collectors.toList()), " "));
                    }
                });
            }
            LSATree.computeNodeLSAChildrenMap(tree); // maps reticulate nodes to lsa nodes

            if (true) {
                System.err.println("before reorder:");
                System.err.println("network: " + tree.toBracketString(false));
                System.err.println("LSAtree: " + jloda.phylo.LSAUtils.getLSATree(tree).toBracketString(false));
            }
            // compute preorder numbering of all nodes
            var ordering = new NodeIntArray(tree);
            computePreOrderNumberingRec(tree, tree.getRoot(), new NodeSet(tree), ordering, 0);
            reorderLSAChildren(tree, ordering);

            if (false) {
                System.err.println("++++++++ Nodes 3:");
                LSAUtils.preorderTraversalLSA(tree, tree.getRoot(), v -> {
                    if (v.isLeaf())
                        System.err.println(v.getId() + " " + tree.getLabel(v));
                    else {
                        System.err.println("Node:" + v.getId());
                        System.err.println("Children:" + StringUtils.toString(v.childrenStream().map(w -> w.getId()).collect(Collectors.toList()), " "));
                        System.err.println("LSA Chld:" + StringUtils.toString(IteratorUtils.asStream(tree.lsaChildren(v)).map(w -> w.getId()).collect(Collectors.toList()), " "));
                    }
                });
            }

            {
                if (false) {
                    var counter = new Counter(0);
                    System.err.println("Leaves:");
                    LSAUtils.preorderTraversalLSA(tree, tree.getRoot(), v -> {
                        if (v.isLeaf())
                            System.err.println(tree.getLabel(v) + ": " + counter.incrementAndGet());
                    });
                }
            }

            if (true) {
                System.err.println("network: " + tree.toBracketString(false));
                System.err.println("LSAtree: " + jloda.phylo.LSAUtils.getLSATree(tree).toBracketString(false));

                var pos = new Counter(0);
                System.err.println("Traversal:");
                LSAUtils.preorderTraversalLSA(tree, tree.getRoot(), v -> {
                    System.err.println("node: " + v.getId() + " (pos: " + pos.incrementAndGet() + ")");
                    System.err.println("Children: " + StringUtils.toString(v.childrenStream().map(w -> w.getId()).collect(Collectors.toList()), " "));
                    System.err.println("LSA Chd: " + StringUtils.toString(IteratorUtils.asStream(tree.lsaChildren(v)).map(w -> w.getId()).collect(Collectors.toList()), " "));
                    if (tree.getLabel(v) != null)
                        System.err.println("taxon: " + tree.getLabel(v));
                });
            }

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
    private int computePreOrderNumberingRec(PhyloTree tree, Node v, NodeSet visited, NodeIntArray ordering, int number) {
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
    private void reorderLSAChildren(PhyloTree tree, final NodeIntArray ordering) {
        // System.err.println("------ v="+v);
        for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
            List<Node> children = tree.getLSAChildrenMap().get(v);
            if (children != null) {
                if (false) {
                    System.err.println("LSA children old for v=" + v.getId() + ":");
                    for (Node u : children) {
                        System.err.println(" " + u.getId() + " order: " + ordering.get(u));
                    }
                }
                SortedSet<Node> sorted = new TreeSet<Node>(new Comparator<Node>() {

                    public int compare(Node v1, Node v2) {
                        if (ordering.getInt(v1) < ordering.getInt(v2))
                            return -1;
                        else if (ordering.getInt(v1) > ordering.getInt(v2))
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
                tree.getLSAChildrenMap().put(v, list);
                if (false) {
                    System.err.println("LSA children new for v=" + v.getId() + ":");
                    for (Node u : children) {
                        System.err.println(" " + u.getId() + " order: " + ordering.get(u));
                    }
                }
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
        for (Edge e : tree.specialEdges()) {
            if (tree.getWeight(e) != 0) {
                isTransferNetwork = true;
                break;
            }
        }
        return isTransferNetwork;
    }
}

