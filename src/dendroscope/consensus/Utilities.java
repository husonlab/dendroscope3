/*
 *   Utilities.java Copyright (C) 2020 Daniel H. Huson
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
package dendroscope.consensus;

import dendroscope.core.TreeData;
import dendroscope.window.MultiViewer;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeIntArray;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;
import jloda.swing.util.Alert;
import jloda.swing.util.Message;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.progress.ProgressListener;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;

/**
 * some utilities
 * Daniel Huson, 8.2007
 */
public class Utilities {
    /**
     * compute the total set of splits
     *
     * @param trees
     * @param allTaxa     total set of taxa will be returned here, if non-null
     * @param tree2taxa   tree-to-taxon mapping will be returned here, if non-null
     * @param tree2splits tree-to-splits mapping will be return here, if non-null
     * @param allSplits   all splits are returned here
     * @return true, if all trees have the same taxa
     */
    public static boolean getSplitsFromTrees(ProgressListener progressListener, PhyloTree[] trees, Taxa allTaxa, BitSet[] tree2taxa, SplitSystem[] tree2splits, SplitSystem allSplits) throws CanceledException {
        progressListener.setSubtask("Get splits from trees");

        Component owner = MultiViewer.getLastActiveFrame();

        if (allTaxa == null)
            allTaxa = new Taxa();
        if (tree2taxa == null)
            tree2taxa = new BitSet[trees.length];
        if (tree2splits == null)
            tree2splits = new SplitSystem[trees.length];

        allTaxa.clear();

        // determine all sets of taxa:
        boolean equalSets = true;

        for (int i = 0; i < trees.length; i++) {
            try {
                tree2taxa[i] = extractTaxa(i, trees[i], allTaxa);
                if (i > 0 && !tree2taxa[i].equals(tree2taxa[0])) {
                    BitSet treeIMinusTree0 = Cluster.setminus(tree2taxa[i], tree2taxa[0]);
                    BitSet tree0MinusTreeI = Cluster.setminus(tree2taxa[0], tree2taxa[i]);
                    int diff = treeIMinusTree0.cardinality() + tree0MinusTreeI.cardinality();
                    if (equalSets) {
                        new Message(owner, "Unequal taxon content: For example: tree[" + (i + 1) + "] differs from tree[1] by " + diff + " taxa\n" +
                                "Will use Z-closure to extend trees to full taxon set");
                        equalSets = false;
                    }
                    System.err.println("--- Taxa contained in tree[1], not contained in tree[" + (i + 1) + "]:");
                    for (int t = treeIMinusTree0.nextSetBit(0); t != -1; t = treeIMinusTree0.nextSetBit(t + 1)) {
                        System.err.println(allTaxa.getLabel(t));
                    }
                    System.err.println("--- Taxa contained in tree[" + (i + 1) + "], not contained in tree[1]:");
                    for (int t = tree0MinusTreeI.nextSetBit(0); t != -1; t = tree0MinusTreeI.nextSetBit(t + 1)) {
                        System.err.println(allTaxa.getLabel(t));
                    }
                }
            } catch (IOException e) {
                Basic.caught(e);
                new Alert(owner, "Internal error: " + e.getMessage());
            }
        }

        // add an artifical outgroup:
        boolean warned = false;
        int outgroupId = allTaxa.add("__outgroup__");

        for (int i = 0; i < trees.length; i++) {
            if (trees[i].getNumberReticulateEdges() == 0) {
				tree2taxa[i].set(outgroupId);
				tree2splits[i] = SplitSystem.getSplitsFromTree(allTaxa, tree2taxa[i], trees[i]);

				for (Iterator it = tree2splits[i].iterator(); it.hasNext(); ) {
					Split split = (Split) it.next();
					double weight = split.getWeight();
					int p = allSplits.indexOf(split);
					if (p == -1) {
						allSplits.addSplit(split);
					} else
                        split = allSplits.getSplit(p);
                    split.addTreeNumber(i);
                    split.addToWeightList(weight);
                }
            } else {
                System.err.println("Tree[" + i + "] is network, can't extract splits");
                if (!warned) {
                    new Alert(owner, "Input contains one or more networks, these will be skipped");
                    warned = true;
                }
                tree2taxa[i].set(outgroupId);
                tree2splits[i] = new SplitSystem();
            }
        }
        //System.err.println("compute splits:");
        //System.err.println(allTaxa);
        //System.err.println(allSplits);

        // add outgroup
        BitSet oA = (BitSet) allTaxa.getBits().clone();
        oA.set(outgroupId, false);
        BitSet oB = new BitSet();
        oB.set(outgroupId);
        allSplits.addSplit(new Split(oA, oB, 0));
        // System.err.println("A=" + oA + " B=" + oB);

        return equalSets;
    }

    /**
     * extract all taxa from the given tree and add all new ones to the set of all taxa
     *
     * @param i
     * @param tree
     * @param allTaxa
     * @return the bits of all taxa found in the tree
     */
    public static BitSet extractTaxa(int i, PhyloTree tree, Taxa allTaxa) throws IOException {
        BitSet taxaBits = new BitSet();
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            String name = tree.getLabel(v);
            if (name != null && name.length() > 0 && !PhyloTree.isBootstrapValue(name)) {
                int index = allTaxa.indexOf(name);
                if (index == -1)
                    index = allTaxa.add(name);
                if (taxaBits.get(index))
                    throw new IOException("tree[" + i + "]: contains multiple copies of label: " + name);
                else
                    taxaBits.set(index);
            } else if (v.getOutDegree() == 0)
                throw new IOException("tree [" + i + "]: leaf has invalid label: " + name);
        }
        return taxaBits;
    }

    /**
     * sets the node to taxa and taxon to node maps
     *
     * @param tree
     * @param taxa
     */
    public static void setNode2Taxa(Taxa taxa, PhyloTree tree) {
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            String label = tree.getLabel(v);
            if (label != null) {
                int t = taxa.indexOf(label);
                if (t != -1) {
                    tree.addTaxon(v, t);
                }
            }
        }
    }

    /**
     * clear node to taxa and taxa to node maps
     *
     * @param tree
     */
    public static void clearNode2Taxa(PhyloTree tree) {
        tree.clearTaxa();
    }

    /**
     * applies a given cyclic ordering to a tree
     *
     * @param taxa     taxa, must have indices 1 .. ntax
     * @param ordering cells 1 .. ntax must contain each of the numbers 1 .. ntax
     * @param tree
     */
    public static void applyOrderingToTree(Taxa taxa, int[] ordering, PhyloTree tree) {
        System.err.println("Applying circular ordering to tree");
        int[] orderingInv = new int[ordering.length];
        for (int t = 1; t < ordering.length; t++) {
            orderingInv[ordering[t]] = t;
        }
        NodeIntArray numbering = new NodeIntArray(tree);

        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            String label = tree.getLabel(v);
            if (label != null) {
                int t = taxa.indexOf(label);
                if (t > 0) {
                    numbering.set(v, orderingInv[t]);
                }
            }
        }

        applyOrderingToTreeRec(tree.getRoot(), numbering);
    }

    /**
     * recursively does the work. Assumes that all edges are directed away from the root
     *
     * @param v
     * @param numbering
     */
    private static void applyOrderingToTreeRec(Node v, NodeIntArray numbering) {
        int min;

        if (numbering.get(v) != null)
            min = numbering.get(v);
        else
            min = Integer.MAX_VALUE;

        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            Node w = f.getTarget();
            applyOrderingToTreeRec(w, numbering);
            min = Math.min(min, numbering.get(w));
        }
        java.util.List<Edge> newOrder = orderEdges(v, numbering);
        v.rearrangeAdjacentEdges(newOrder);

        numbering.set(v, min);
    }

    /**
     * order the edges
     *
     * @param v
     * @param numbering
     * @return edges ordered by numbering of nodes
     */
    private static List<Edge> orderEdges(Node v, NodeIntArray numbering) {
        {
            SortedSet<Pair<Integer, Edge>> sorted = new TreeSet<>(new Comparator<Pair<Integer, Edge>>() {
                public int compare(Pair<Integer, Edge> o1, Pair<Integer, Edge> o2) {
                    return o1.compareTo(o2);
                }
            });

            for (Edge e = v.getFirstAdjacentEdge(); e != null; e = v.getNextAdjacentEdge(e)) {
                Node w = e.getOpposite(v);
                sorted.add(new Pair<>(numbering.getInt(w), e));
            }
            List<Edge> result = new LinkedList<>();
            for (Pair<Integer, Edge> pair : sorted) {
                result.add(pair.getSecond());
            }
            return result;
        }
    }

    /**
     * extract the most abundent root partition that is compatible with all given splits
     *
     * @param taxa
     * @param splits
     * @param trees
     * @return root partition
     */
    public static Partition extractRootPartition(Taxa taxa, SplitSystem splits, TreeData[] trees) {
        List<Partition> list = new LinkedList<>();

        for (TreeData tree : trees) {
            Partition partition = Utilities.getRootPartition(taxa, tree);
            if (partition.compatibleWithAllSplits(splits)) {
                boolean found = false;
                for (Iterator it = list.iterator(); !found && it.hasNext(); ) {
                    Partition previous = (Partition) it.next();
                    if (previous.equals(partition)) {
                        previous.setWeight(previous.getWeight() + partition.getWeight());
                        found = true;
                    }
                }
                if (!found) {
                    list.add(partition);
                }
            }
        }
        if (list.size() > 0) {
            Partition[] array = list.toArray(new Partition[list.size()]);
            Arrays.sort(array, Partition.getComparatorByDescendingWeight());

            System.err.println("root partition: " + array[0]);

            return array[0];
        } else // nothing suitable found, use the heaviest trivial split
        {
            Split rootSplit = splits.getHeaviestTrivialSplit();
            Partition partition = new Partition();
            partition.addBlock(rootSplit.getA());
            partition.addBlock(rootSplit.getB());

            System.err.println("root partition (default): " + partition);

            return partition;
        }

    }

    /**
     * computes the root partition of a tree (or network)
     *
     * @param taxa
     * @param tree
     * @return parition
     */
    private static Partition getRootPartition(Taxa taxa, PhyloTree tree) {
        Partition partition = new Partition();

        NodeSet seen = new NodeSet(tree);
        Stack<Node> stack = new Stack<>();

        Node root = tree.getRoot();
        boolean first = true;
        for (Edge e = root.getFirstOutEdge(); e != null; e = root.getNextOutEdge(e)) {
            Node w = e.getTarget();
            if (!seen.contains(w)) {
                BitSet block = new BitSet();
                if (first) {
                    if (tree.getLabel(root) != null) {
                        int t = taxa.indexOf(tree.getLabel(root));
                        if (t != -1)
                            block.set(t); // if root is labeled, add it to first block
                    }
                    first = false;
                }
                stack.push(w);
                seen.add(w);
                while (!stack.isEmpty()) {
                    Node u = stack.pop();
                    if (tree.getLabel(u) != null) {
                        int t = taxa.indexOf(tree.getLabel(u));
                        if (t != -1)
                            block.set(t);
                    }
                    for (Edge f = u.getFirstOutEdge(); f != null; f = u.getNextOutEdge(f)) {
                        Node z = f.getTarget();
                        if (!seen.contains(z)) {
                            seen.add(z);
                            stack.push(z);
                        }
                    }
                }
                partition.addBlock(block);
            }
        }
        partition.setWeight(1);
        return partition;
    }
}
