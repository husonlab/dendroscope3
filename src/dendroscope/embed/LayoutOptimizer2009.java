/**
 * LayoutOptimizer2009.java 
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
package dendroscope.embed;

import dendroscope.consensus.Cluster;
import dendroscope.consensus.LSATree;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import jloda.util.Pair;
import jloda.util.ProgressListener;

import java.util.*;

/**
 * this algorithm optimizes the layout both for reticulate networks and transfer networks.
 * This is the algorithm described in the 2009 paper on drawing networks and also in the "Phylogenetic Networks" book
 * Daniel Huson, 7.2009
 */
public class LayoutOptimizer2009 implements ILayoutOptimizer {
    /**
     * attempt an optimal topological embedding, based on the guide tree
     *
     * @param tree
     * @param progressListener
     */
    public void apply(PhyloTree tree, ProgressListener progressListener) {
        if (tree.getRoot() == null || tree.getSpecialEdges().size() == 0) {
            tree.getNode2GuideTreeChildren().clear();
            return;
        }
        System.err.println("Computing optimal embedding using (Huson, 2009)");

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
        }

        // first label source and target nodes by numbers representing the different reticulate edges:
        NodeArray<BitSet> node2SpecialSource = new NodeArray<BitSet>(tree);
        NodeArray<BitSet> node2SpecialTarget = new NodeArray<BitSet>(tree);
        int num = 0;
        for (Edge e = tree.getFirstEdge(); e != null; e = e.getNext()) {
            if (tree.isSpecial(e) && tree.getWeight(e) <= 0) {
                num++;
                BitSet sources = node2SpecialSource.get(e.getSource());
                if (sources == null) {
                    sources = new BitSet();
                    node2SpecialSource.put(e.getSource(), sources);
                }
                sources.set(num);
                BitSet targets = node2SpecialTarget.get(e.getTarget());
                if (targets == null) {
                    targets = new BitSet();
                    node2SpecialTarget.put(e.getTarget(), targets);
                }
                targets.set(num);
            }
        }

        // second, extend these map so that each node v is labeled  by the sources and targets
        // that are contained in the subtree    rooted at v

        computeExtendedMapRec(tree.getRoot(), tree.getNode2GuideTreeChildren(), node2SpecialSource, node2SpecialTarget);
        NodeArray<BitSet> node2Special = new NodeArray<BitSet>(tree);
        for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
            BitSet sources = node2SpecialSource.get(v);
            BitSet targets = node2SpecialTarget.get(v);
            BitSet set = null;
            if (sources != null && sources.cardinality() != 0) {
                set = (BitSet) sources.clone();
                if (targets != null && targets.cardinality() != 0) {
                    set.or(targets);
                }
            } else if (targets != null && targets.cardinality() != 0) {
                set = (BitSet) targets.clone();
            }
            if (set != null)
                node2Special.put(v, set);
        }
        node2SpecialSource.clear();
        node2SpecialTarget.clear();

        Map<Node, Integer> node2NumberOfLeavesBelow = new HashMap<Node, Integer>();
        countLeaves(tree.getRoot(), tree.getNode2GuideTreeChildren(), node2NumberOfLeavesBelow);

        // add two dummy nodes to the network. These are used to represent the stuff before and after the current subtree
        Node before = tree.newNode();
        Node after = tree.newNode();

        // process nodes in a pre-order traversal
        optimizeTopologicalEmbedding(tree.getRoot(), before, after, node2NumberOfLeavesBelow, tree.getNode2GuideTreeChildren(), retNode2GuideParent, node2Special);

        // remove dummy nodes
        tree.deleteNode(before);
        tree.deleteNode(after);
    }

    /**
     * optimize the topological embedding of the network
     *
     * @param v
     * @param before
     * @param after
     * @param node2GuideTreeChildren
     * @param node2Special
     */
    private void optimizeTopologicalEmbedding(Node v, Node before, Node after, Map<Node, Integer> node2NumberOfLeavesBelow, NodeArray<List<Node>> node2GuideTreeChildren, NodeArray<Node> retNode2GuideParent, NodeArray<BitSet> node2Special) {
        if (node2GuideTreeChildren.get(v).size() > 1) {
            AttractionMatrix matrix = computeAttractionMatrix(v, before, after, node2GuideTreeChildren, retNode2GuideParent, node2Special);
            //System.err.println("matrix:\n" + matrix.toString());

            List<Node> ordering = node2GuideTreeChildren.get(v);

            /*
            System.err.print("original ordering for v=" + v.getId() + ": ");
            for (Iterator it = ordering.iterator(); it.hasNext();)
                System.err.print(" " + ((Node) it.next()).getId());
            System.err.println();
             */

            ordering.add(0, before);
            ordering.add(after);

            improveOrdering(matrix, ordering, node2NumberOfLeavesBelow);
            // improveOrderingUsingHeaviestChain(matrix, ordering);

            /*
            System.err.print("improved ordering for v=" + v.getId() + ": ");
             for (Iterator it = ordering.iterator(); it.hasNext();)
                 System.err.print(" " + ((Node) it.next()).getId());
             System.err.println();
             */

            ordering.remove(before);
            ordering.remove(after);
        }

        for (Node w : node2GuideTreeChildren.get(v)) {
            optimizeTopologicalEmbedding(w, before, after, node2NumberOfLeavesBelow, node2GuideTreeChildren, retNode2GuideParent, node2Special);
        }
    }


    /**
     * Finds the optimal ordering using branch-and-bound.
     *
     * @param aMatrix
     * @param ordering
     */
    private void improveOrdering(AttractionMatrix aMatrix, List<Node> ordering, Map<Node, Integer> node2NumberOfLeavesBelow) {
        Node[] nodes = ordering.toArray(new Node[ordering.size()]);
        int i = 0;
        for (Node v : ordering) {
            nodes[i++] = v;
        }
        BitSet used = new BitSet();

        /*
         System.err.println("Original ordering:");
        for(int j=0;j<nodes.length;j++)
            System.err.println(nodes[j]); 

        System.err.println("Attractions:\n"+aMatrix);
        */

        int[] bestOrdering = new int[nodes.length];

        improveRec(aMatrix, nodes, used, node2NumberOfLeavesBelow, new int[ordering.size()], 0, 0, bestOrdering, Integer.MAX_VALUE, new int[]{0});

        ordering.clear();
        for (int a : bestOrdering) ordering.add(nodes[a]);

        /*
        System.err.println("Best ordering:");
        for(int j=0;j<bestOrdering.length;j++)
            System.err.println(nodes[bestOrdering[j]]);
                  */
    }

    /**
     * uses branch and bound to find best layout
     *
     * @param aMatrix
     * @param nodes
     * @param used
     * @param currentOrdering
     * @param length
     * @param score
     * @param bestOrdering
     * @param bestScore
     * @return best score
     */
    private int improveRec(AttractionMatrix aMatrix, Node[] nodes, BitSet used, Map<Node, Integer> node2NumberOfLeavesBelow, int[] currentOrdering, int length, int score, int[] bestOrdering, int bestScore, int[] count) {
        if (length == nodes.length) {
            if (score < bestScore) {
                System.arraycopy(currentOrdering, 0, bestOrdering, 0, currentOrdering.length);
                bestScore = score;
            }
        } else {
            int bot = 0;
            int top;
            if (length == 0)
                top = 1; // at very beginning of chain, only allow "before" node to start chain
            else if (length < nodes.length - 1)
                top = nodes.length - 1; // not at end of chain, don't allow "after" node
            else {   // at end of chain, only consider "after" node
                bot = nodes.length - 1;
                top = nodes.length;
            }
            for (int p = bot; p < top; p++) {
                if (count[0] > 100000 && bestScore < Integer.MAX_VALUE)
                    return bestScore;

                if (!used.get(p)) {
                    // determine how much this placement of the node at p will add to score:
                    int add = 0;
                    for (int i = 0; i < length; i++) {
                        int leaves = 0;
                        for (int j = i + 1; j < length; j++) {
                            Node u = nodes[currentOrdering[j]];
                            leaves += node2NumberOfLeavesBelow.get(u);
                        }
                        add += leaves * leaves * aMatrix.get(nodes[currentOrdering[i]], nodes[p]);
                    }

                    // compute penalty for having all reticulation edges going either up or down:
                    int all = 0;
                    int up = 0;
                    for (int q = 0; q < nodes.length; q++) {
                        if (q != p) {
                            all += aMatrix.get(nodes[q], nodes[p]);
                            if (used.get(q))
                                up += aMatrix.get(nodes[q], nodes[p]);
                        }
                    }
                    if (!(all > 0 && (up == 0 || up == all)))  // not all edges up or down, lower penalty
                        add -= 10 * all;

                    if (score + add < bestScore) {
                        currentOrdering[length] = p;
                        used.set(p);
                        bestScore = Math.min(bestScore, improveRec(aMatrix, nodes, used, node2NumberOfLeavesBelow, currentOrdering, length + 1, score + add, bestOrdering, bestScore, count));
                        count[0]++;
                        used.set(p, false);
                    }
                }
            }
        }
        return bestScore;
    }

    /**
     * counts the number of leaves in the guide tree below each node
     *
     * @param v
     * @param node2GuideTreeChildren
     * @param node2NumberOfLeavesBelow
     * @return number of leaves
     */
    private int countLeaves(Node v, NodeArray<List<Node>> node2GuideTreeChildren, Map<Node, Integer> node2NumberOfLeavesBelow) {
        int count = 0;
        List<Node> children = node2GuideTreeChildren.get(v);

        if (children == null || children.size() == 0) {
            count = 1;
        } else {
            for (Node w : children) {
                count += countLeaves(w, node2GuideTreeChildren, node2NumberOfLeavesBelow);
            }
        }
        node2NumberOfLeavesBelow.put(v, count);
        return count;
    }

    /**
     * sets up the  attraction matrix
     *
     * @param v
     * @param node2GuideTreeChildren
     * @param node2Special
     * @return attraction matrix
     */
    private AttractionMatrix computeAttractionMatrix(Node v, Node before, Node after, NodeArray<List<Node>> node2GuideTreeChildren, NodeArray<Node> retNode2GuideParent, NodeArray<BitSet> node2Special) {
        // clear boundary nodes:
        node2Special.put(before, new BitSet());
        node2Special.put(after, new BitSet());
        // compute values for boundary nodes by heading up toward the root
        Node w = v;
        while (true) {
            Node u;
            if (retNode2GuideParent.get(w) != null)
                u = retNode2GuideParent.get(w);
            else if (w.getInDegree() == 1)
                u = w.getFirstInEdge().getSource();
            else
                break;
            boolean isBefore = true;
            List<Node> children = node2GuideTreeChildren.get(u);
            for (Node child : children) {
                if (child == w)
                    isBefore = false;
                else if (isBefore) {
                    BitSet set = node2Special.get(child);
                    if (set != null)
                        (node2Special.get(before)).or(set);
                } else {
                    BitSet set = node2Special.get(child);
                    if (set != null)
                        (node2Special.get(after)).or(set);
                }
            }
            w = u;
        }

        // now setup attraction matrix:
        AttractionMatrix aMatrix = new AttractionMatrix();

        List<Node> children = new LinkedList<Node>();
        children.addAll(node2GuideTreeChildren.get(v));
        children.add(before);
        children.add(after);
        for (Node p : children) {
            for (Node q : children) {
                if (p.getId() < q.getId()) {
                    int count = Cluster.intersection(node2Special.get(p), node2Special.get(q)).cardinality();
                    aMatrix.set(p, q, count);
                }
            }
        }
        return aMatrix;
    }


    /**
     * label every node v by the special edges that have a source or target node (but not both) in the
     * subtree rooted at v
     *
     * @param v
     * @param node2GuideTreeChildren
     * @param node2SpecialSource
     * @param node2SpecialTarget
     */
    private void computeExtendedMapRec(Node v, NodeArray<List<Node>> node2GuideTreeChildren, NodeArray<BitSet> node2SpecialSource, NodeArray<BitSet> node2SpecialTarget) {
        BitSet sources = new BitSet();
        BitSet targets = new BitSet();
        BitSet vSources = node2SpecialSource.get(v);
        if (vSources != null)
            sources.or(vSources);
        BitSet vTargets = node2SpecialTarget.get(v);
        if (vTargets != null)
            targets.or(vTargets);

        List<Node> children = node2GuideTreeChildren.get(v);
        for (Node w : children) {
            computeExtendedMapRec(w, node2GuideTreeChildren, node2SpecialSource, node2SpecialTarget);
            BitSet wSources = node2SpecialSource.get(w);
            if (wSources != null)
                sources.or(wSources);
            BitSet wTargets = node2SpecialTarget.get(w);
            if (wTargets != null)
                targets.or(wTargets);
        }
        BitSet openSources = (BitSet) sources.clone();
        openSources.andNot(targets);
        BitSet openTargets = (BitSet) targets.clone();
        openTargets.andNot(sources);
        node2SpecialSource.put(v, openSources);
        node2SpecialTarget.put(v, openTargets);
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

/**
 * matrix of attraction edges between two subtrees
 */
class AttractionMatrix {
    final private Map<Pair<Node, Node>, Integer> matrix = new HashMap<Pair<Node, Node>, Integer>();

    /**
     * get the number of special edges from subtree of v to subtree of w
     *
     * @param v
     * @param w
     * @return count
     */
    int get(Node v, Node w) {
        Integer value = matrix.get(v.getId() < w.getId() ? new Pair<Node, Node>(v, w) : new Pair<Node, Node>(w, v));
        if (value != null)
            return value;
        else
            return 0;
    }

    /**
     * set the number of edges from subtree of v to subtree of w
     *
     * @param v
     * @param w
     * @param value
     */
    void set(Node v, Node w, int value) {
        matrix.put(v.getId() < w.getId() ? new Pair<Node, Node>(v, w) : new Pair<Node, Node>(w, v), value);
    }

    /**
     * get string
     *
     * @return string
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (Pair<Node, Node> pair : matrix.keySet()) {
            Node v = pair.getFirst();
            Node w = pair.getSecond();
            buf.append(" m(").append(v.getId()).append(",").append(w.getId()).append(")=").append(matrix.get(pair));
        }
        return buf.toString();
    }
}

