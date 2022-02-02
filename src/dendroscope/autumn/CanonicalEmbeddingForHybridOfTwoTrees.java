/*
 * CanonicalEmbeddingForHybridOfTwoTrees.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.autumn;

import dendroscope.consensus.Taxa;
import dendroscope.consensus.Utilities;
import dendroscope.util.DistanceMethods;
import dendroscope.window.TreeViewer;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Pair;
import jloda.util.Single;

import java.io.IOException;
import java.util.*;

/**
 * reorders a rooted phylogenetic network canonically, assuming the reticulate edges are labeled by 1 and 2
 * to indicate source trees
 * Daniel Huson, 6.2011
 */
public class CanonicalEmbeddingForHybridOfTwoTrees {

    /**
     * reorder the network below this node so that all children are in lexicographic order
     */
    public static void apply(PhyloTree tree, Taxa allTaxa) throws IOException {
        if (tree.getRoot() != null && allTaxa.size() > 0) {
            Map<Node, Integer> order = new HashMap<Node, Integer>();
            Single<Integer> postOrderNumber = new Single<Integer>(1);
            order.put(tree.getRoot(), postOrderNumber.get());
            computePostOrderNumberingRec(tree, tree.getRoot(), allTaxa, order, postOrderNumber);
            reorderNetworkChildrenRec(tree, tree.getRoot(), order);
        }
    }

    /**
     * computes a post-order numbering of all nodes, avoiding edges that are only contained in tree2
     *
     * @return taxa below
     */
    private static BitSet computePostOrderNumberingRec(PhyloTree tree, Node v, Taxa allTaxa, final Map<Node, Integer> order, Single<Integer> postOrderNumber) throws IOException {
        final BitSet taxaBelow = new BitSet();

        if (v.getOutDegree() == 0) {
            taxaBelow.set(allTaxa.indexOf(tree.getLabel(v)));
        } else {
			SortedSet<Pair<BitSet, Node>> child2TaxaBelow = new TreeSet<Pair<BitSet, Node>>((pair1, pair2) -> {
				int t1 = pair1.getFirst().nextSetBit(0);
				int t2 = pair2.getFirst().nextSetBit(0);
				if (t1 < t2)
					return -1;
				else if (t1 > t2)
					return 1;

				int id1 = pair1.getSecond().getId();
				int id2 = pair2.getSecond().getId();

				return Integer.compare(id1, id2);
			});

			// first visit the children:
			for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
				Node w = e.getTarget();
                String treeId = tree.getLabel(e);
                if (w.getInDegree() > 1 && treeId == null)
                    throw new IOException("Node has two in-edges, one not labeled");
                if (w.getInDegree() == 1 || treeId.equals("1")) {
                    if (w.getInDegree() == 2 && treeId != null && !treeId.equals("1"))
                        throw new IOException("Node has two in-edges, but chosen one is not labeled 1");

                    BitSet childTaxa = computePostOrderNumberingRec(tree, w, allTaxa, order, postOrderNumber);
                    child2TaxaBelow.add(new Pair<BitSet, Node>(childTaxa, w));

                } else {
                    if (w.getInDegree() < 2)
                        throw new IOException("Node has only one in edge, which is labeled 2");
                    if (w.getInDegree() == 2 && (tree.getLabel(w.getFirstInEdge()).equals("2") && tree.getLabel(w.getLastInEdge()).equals("2")))
                        throw new IOException("Node has two in edges, both labeled 2");
                }
            }
            for (Pair<BitSet, Node> pair : child2TaxaBelow) {
                postOrderNumber.set(postOrderNumber.get() + 1);
                order.put(pair.getSecond(), postOrderNumber.get());
                taxaBelow.or(pair.getFirst());
            }
        }
        return taxaBelow;
    }

    /**
     * recursively reorders the network using the post-order numbering computed above
     */
    private static void reorderNetworkChildrenRec(PhyloTree tree, Node v, final Map<Node, Integer> order) {
		List<Edge> children = new LinkedList<Edge>();

		for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
			Node w = e.getTarget();
			String treeId = tree.getLabel(e);
			if (w.getInDegree() == 1 || treeId == null || !treeId.equals("2"))
				reorderNetworkChildrenRec(tree, w, order);
			children.add(e);
		}

		Edge[] array = children.toArray(new Edge[0]);
		Arrays.sort(array, (e1, e2) -> {
			Integer rank1 = order.get(e1.getTarget());
			Integer rank2 = order.get(e2.getTarget());

			if (rank1 == null)  // dead node
				rank1 = Integer.MAX_VALUE;
			if (rank2 == null)  // dead node
				rank2 = Integer.MAX_VALUE;

			if (rank1 < rank2)
				return -1;
			else if (rank1 > rank2)
				return 1;
			else if (e1.getId() < e2.getId())
				return -1;
			else if (e1.getId() > e2.getId())
				return 1;
			else
				return 0;
		});
		List<Edge> list = new LinkedList<Edge>(Arrays.asList(array));
        if (v.getInDegree() > 0)
            list.add(v.getFirstInEdge());
        v.rearrangeAdjacentEdges(list);
    }

    /**
     * does pairwise comparison of all hybrid networks
     *
     */
    public static void compareAllTrees(TreeViewer[] treeViewers) throws IOException {
        Taxa allTaxa = new Taxa();

        for (TreeViewer treeViewer : treeViewers) {
            PhyloTree tree = treeViewer.getPhyloTree();
            Utilities.extractTaxa(1, tree, allTaxa);
        }
        for (TreeViewer treeViewer : treeViewers) {
            PhyloTree tree = treeViewer.getPhyloTree();
            apply(tree, allTaxa);
        }

        for (TreeViewer treeViewer : treeViewers) {
            PhyloTree tree = treeViewer.getPhyloTree();
            boolean found = false;
            for (Node v = tree.getFirstNode(); !found && v != null; v = v.getNext()) {
                if (v.getOutDegree() > 1) {
                    boolean hasReticulate = false;
                    for (Edge e = v.getFirstOutEdge(); !found && e != null; e = v.getNextOutEdge(e)) {
                        if (tree.isReticulatedEdge(e)) {
                            if (!hasReticulate)
                                hasReticulate = true;
                            else
                                found = true;
                        }
                    }
                }
            }
            if (found)
                System.err.println(treeViewer.getName() + ": has node with multiple reticulate out edges");
        }

		StringBuilder nexus = new StringBuilder();
        nexus.append("#nexus\nbegin taxa;\ndimensions ntax=").append(treeViewers.length).append(";\nend;\n");
        nexus.append("begin distances;\nformat labels triangle=upper no diagonal;\n");
        nexus.append("matrix\n");

        int count = 0;
        int countIdentical = 0;
        for (int i = 0; i < treeViewers.length; i++) {
            TreeViewer treeViewer1 = treeViewers[i];
            PhyloTree tree1 = treeViewer1.getPhyloTree();
            String newick1 = tree1.toBracketString();
            nexus.append("'").append(treeViewer1.getName()).append("'");

            for (int j = i + 1; j < treeViewers.length; j++) {
                TreeViewer treeViewer2 = treeViewers[j];
                PhyloTree tree2 = treeViewer2.getPhyloTree();
                String newick2 = tree2.toBracketString();

                if (newick1.equals(newick2)) {
                    System.out.println("Hybrid networks are identical: " + i + " vs " + j);
                    System.out.println("Network " + i + ": " + newick1);
                    System.out.println("Network " + j + ": " + newick2);
                    countIdentical++;
                }
                count++;

                List<PhyloTree> list = new LinkedList<PhyloTree>();
                list.add(tree1);
                list.add(tree2);
                float dist = (float) DistanceMethods.computeHardwiredClusterDistance(list)[0][1];
                nexus.append(" ").append(dist);
            }
            nexus.append("\n");
        }
        nexus.append(";\nend;\n");
        System.err.println("Pairs compared: " + count + " (identical: " + countIdentical + ")");

		System.err.println(nexus);
    }
}
