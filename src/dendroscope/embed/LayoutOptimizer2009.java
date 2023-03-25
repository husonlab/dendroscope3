/*
 *   LayoutOptimizer2009.java Copyright (C) 2023 Daniel H. Huson
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

import dendroscope.consensus.Cluster;
import dendroscope.consensus.LSATree;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import jloda.util.Pair;
import jloda.util.progress.ProgressListener;

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
	 */
    public void apply(PhyloTree tree, ProgressListener progressListener) {
        if (tree.getRoot() == null || tree.getNumberReticulateEdges() == 0) {
			tree.getLSAChildrenMap().clear();
			return;
		}
        System.err.println("Computing optimal embedding using (Huson, 2009)");

        boolean isTransferNetwork = isTransferNetwork(tree);

		var retNode2GuideParent = new NodeArray<Node>(tree);

        if (isTransferNetwork) {
			tree.getLSAChildrenMap().clear();
            for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
                List<Node> children = new LinkedList<Node>();
                for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
					if (!tree.isReticulateEdge(e) || tree.getWeight(e) > 0) {
						children.add(e.getTarget());
						retNode2GuideParent.put(e.getTarget(), e.getSource());
					}

				}
				tree.getLSAChildrenMap().put(v, children);

			}
		} else // must be combining network
		{
			LSATree.computeNodeLSAChildrenMap(tree, retNode2GuideParent); // maps reticulate nodes to lsa nodes
		}

		// first label source and target nodes by numbers representing the different reticulate edges:
		var node2SpecialSource = new NodeArray<BitSet>(tree);
		var node2SpecialTarget = new NodeArray<BitSet>(tree);
		int num = 0;
		for (var e : tree.edges()) {
			if (tree.isReticulateEdge(e) && tree.getWeight(e) <= 0) {
				num++;
				var sources = node2SpecialSource.get(e.getSource());
				if (sources == null) {
					sources = new BitSet();
					node2SpecialSource.put(e.getSource(), sources);
				}
				sources.set(num);
				var targets = node2SpecialTarget.get(e.getTarget());
				if (targets == null) {
					targets = new BitSet();
					node2SpecialTarget.put(e.getTarget(), targets);
				}
				targets.set(num);
			}
		}

		// second, extend these map so that each node v is labeled  by the sources and targets
		// that are contained in the subtree    rooted at v

		computeExtendedMapRec(tree.getRoot(), tree.getLSAChildrenMap(), node2SpecialSource, node2SpecialTarget);
		var node2Special = new NodeArray<BitSet>(tree);
		for (var v : tree.nodes()) {
			var sources = node2SpecialSource.get(v);
			var targets = node2SpecialTarget.get(v);
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

		var node2NumberOfLeavesBelow = new HashMap<Node, Integer>();
		countLeaves(tree.getRoot(), tree.getLSAChildrenMap(), node2NumberOfLeavesBelow);

		// add two dummy nodes to the network. These are used to represent the stuff before and after the current subtree
		var before = tree.newNode();
		var after = tree.newNode();

		// process nodes in a pre-order traversal
		optimizeTopologicalEmbedding(tree.getRoot(), before, after, node2NumberOfLeavesBelow, tree.getLSAChildrenMap(), retNode2GuideParent, node2Special);

		// remove dummy nodes
		tree.deleteNode(before);
		tree.deleteNode(after);
	}

    /**
     * optimize the topological embedding of the network
     *
	 */
    private void optimizeTopologicalEmbedding(Node v, Node before, Node after, Map<Node, Integer> node2NumberOfLeavesBelow, NodeArray<List<Node>> node2GuideTreeChildren, NodeArray<Node> retNode2GuideParent, NodeArray<BitSet> node2Special) {
        if (node2GuideTreeChildren.get(v).size() > 1) {
			var matrix = computeAttractionMatrix(v, before, after, node2GuideTreeChildren, retNode2GuideParent, node2Special);
            //System.err.println("matrix:\n" + matrix.toString());

			var ordering = node2GuideTreeChildren.get(v);

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

		for (var w : node2GuideTreeChildren.get(v)) {
			optimizeTopologicalEmbedding(w, before, after, node2NumberOfLeavesBelow, node2GuideTreeChildren, retNode2GuideParent, node2Special);
		}
    }


    /**
     * Finds the optimal ordering using branch-and-bound.
     *
	 */
    private void improveOrdering(AttractionMatrix aMatrix, List<Node> ordering, Map<Node, Integer> node2NumberOfLeavesBelow) {
		var nodes = ordering.toArray(new Node[0]);
		var i = 0;
		for (Node v : ordering) {
			nodes[i++] = v;
		}
		var used = new BitSet();

        /*
         System.err.println("Original ordering:");
        for(int j=0;j<nodes.length;j++)
            System.err.println(nodes[j]); 

        System.err.println("Attractions:\n"+aMatrix);
        */

		var bestOrdering = new int[nodes.length];

		improveRec(aMatrix, nodes, used, node2NumberOfLeavesBelow, new int[ordering.size()], 0, 0, bestOrdering, Integer.MAX_VALUE, new int[]{0});

		ordering.clear();
		for (var a : bestOrdering)
			ordering.add(nodes[a]);

        /*
        System.err.println("Best ordering:");
        for(int j=0;j<bestOrdering.length;j++)
            System.err.println(nodes[bestOrdering[j]]);
                  */
	}

    /**
     * uses branch and bound to find best layout
     *
     * @return best score
     */
    private int improveRec(AttractionMatrix aMatrix, Node[] nodes, BitSet used, Map<Node, Integer> node2NumberOfLeavesBelow, int[] currentOrdering, int length, int score, int[] bestOrdering, int bestScore, int[] count) {
        if (length == nodes.length) {
            if (score < bestScore) {
                System.arraycopy(currentOrdering, 0, bestOrdering, 0, currentOrdering.length);
                bestScore = score;
            }
        } else {
			final int bot;
			final int top;
			if (length == 0) {
				bot = 0;
				top = 1; // at very beginning of chain, only allow "before" node to start chain
			} else if (length < nodes.length - 1) {
				bot = 0;
				top = nodes.length - 1; // not at end of chain, don't allow "after" node
			} else {   // at end of chain, only consider "after" node
				bot = nodes.length - 1;
				top = nodes.length;
			}
			for (var p = bot; p < top; p++) {
				if (count[0] > 100000 && bestScore < Integer.MAX_VALUE)
					return bestScore;

				if (!used.get(p)) {
					// determine how much this placement of the node at p will add to score:
					var add = 0;
					for (var i = 0; i < length; i++) {
						var leaves = 0;
						for (var j = i + 1; j < length; j++) {
							Node u = nodes[currentOrdering[j]];
							leaves += node2NumberOfLeavesBelow.get(u);
						}
						add += leaves * leaves * aMatrix.get(nodes[currentOrdering[i]], nodes[p]);
					}

					// compute penalty for having all reticulation edges going either up or down:
					var all = 0;
					var up = 0;
					for (var q = 0; q < nodes.length; q++) {
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
     * @return number of leaves
     */
    private int countLeaves(Node v, NodeArray<List<Node>> node2GuideTreeChildren, Map<Node, Integer> node2NumberOfLeavesBelow) {
		int count = 0;
		var children = node2GuideTreeChildren.get(v);

        if (children == null || children.size() == 0) {
            count = 1;
        } else {
			for (var w : children) {
				count += countLeaves(w, node2GuideTreeChildren, node2NumberOfLeavesBelow);
			}
        }
        node2NumberOfLeavesBelow.put(v, count);
        return count;
    }

    /**
     * sets up the  attraction matrix
     *
     * @return attraction matrix
     */
    private AttractionMatrix computeAttractionMatrix(Node v, Node before, Node after, NodeArray<List<Node>> node2GuideTreeChildren, NodeArray<Node> retNode2GuideParent, NodeArray<BitSet> node2Special) {
        // clear boundary nodes:
        node2Special.put(before, new BitSet());
        node2Special.put(after, new BitSet());
        // compute values for boundary nodes by heading up toward the root
		var w = v;
        while (true) {
			Node u;
			if (retNode2GuideParent.get(w) != null)
				u = retNode2GuideParent.get(w);
			else if (w.getInDegree() == 1)
				u = w.getFirstInEdge().getSource();
			else
				break;
			var isBefore = true;
			var children = node2GuideTreeChildren.get(u);
			for (var child : children) {
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

		var children = new LinkedList<>(node2GuideTreeChildren.get(v));
		children.add(before);
		children.add(after);
		for (var p : children) {
			for (var q : children) {
				if (p.getId() < q.getId()) {
					var count = Cluster.intersection(node2Special.get(p), node2Special.get(q)).cardinality();
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
	 */
    private void computeExtendedMapRec(Node v, NodeArray<List<Node>> node2GuideTreeChildren, NodeArray<BitSet> node2SpecialSource, NodeArray<BitSet> node2SpecialTarget) {
		var sources = new BitSet();
		var targets = new BitSet();
		var vSources = node2SpecialSource.get(v);
		if (vSources != null)
			sources.or(vSources);
		var vTargets = node2SpecialTarget.get(v);
		if (vTargets != null)
			targets.or(vTargets);

		for (var w : node2GuideTreeChildren.get(v)) {
			computeExtendedMapRec(w, node2GuideTreeChildren, node2SpecialSource, node2SpecialTarget);
			var wSources = node2SpecialSource.get(w);
			if (wSources != null)
				sources.or(wSources);
			var wTargets = node2SpecialTarget.get(w);
			if (wTargets != null)
				targets.or(wTargets);
		}
		var openSources = (BitSet) sources.clone();
        openSources.andNot(targets);
		var openTargets = (BitSet) targets.clone();
        openTargets.andNot(sources);
        node2SpecialSource.put(v, openSources);
        node2SpecialTarget.put(v, openTargets);
    }

    /**
     * does network look like a transfer network?
     *
     * @return true, if is transfer network
     */
    public static boolean isTransferNetwork(PhyloTree tree) {
		var isTransferNetwork = false;
		for (Edge e : tree.reticulateEdges()) {
			if (tree.getWeight(e) != 0) {
				isTransferNetwork = true;
				break;
			}
		}
		return isTransferNetwork;
	}


	/**
	 * matrix of attraction edges between two subtrees
	 */
	private static class AttractionMatrix {
		final private Map<Pair<Node, Node>, Integer> matrix = new HashMap<Pair<Node, Node>, Integer>();

		/**
		 * get the number of special edges from subtree of v to subtree of w
		 *
		 * @return count
		 */
		int get(Node v, Node w) {
			Integer value = matrix.get(v.getId() < w.getId() ? new Pair<>(v, w) : new Pair<>(w, v));
			return Objects.requireNonNullElse(value, 0);
		}

		/**
		 * set the number of edges from subtree of v to subtree of w
		 *
		 */
		void set(Node v, Node w, int value) {
			matrix.put(v.getId() < w.getId() ? new Pair<>(v, w) : new Pair<>(w, v), value);
		}

		/**
		 * get string
		 *
		 * @return string
		 */
		public String toString() {
			var buf = new StringBuilder();
			for (var pair : matrix.keySet()) {
				var v = pair.getFirst();
				var w = pair.getSecond();
				buf.append(" m(").append(v.getId()).append(",").append(w.getId()).append(")=").append(matrix.get(pair));
			}
			return buf.toString();
		}
	}
}

