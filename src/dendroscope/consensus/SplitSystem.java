/*
 * SplitSystem.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.consensus;

import dendroscope.algorithms.clusternet.ClusterNetwork;
import dendroscope.algorithms.gallnet.ComputeGalledNetwork;
import dendroscope.algorithms.levelknet.LevelKNetwork;
import dendroscope.main.DendroscopeProperties;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import jloda.util.progress.ProgressListener;
import jloda.util.progress.ProgressSilent;

import javax.swing.*;
import java.io.IOException;
import java.util.*;

/**
 * a collection of splits
 * Daniel Huson, 6.2007
 */
public class SplitSystem {
    private int nsplits;

    final private Map<Integer, Split> index2split;
    final private Map<Split, Integer> split2index;

    /**
     * constructor
     */
    public SplitSystem() {
        nsplits = 0;
        index2split = new HashMap<>();
        split2index = new HashMap<>();
    }

    /**
     * constructs a set of splits from a tree
     *
	 */
    public SplitSystem(Taxa allTaxa, PhyloTree tree) {
        this();
        splitsFromTreeRec(tree.getRoot(), tree, allTaxa, allTaxa.getBits(), new NodeArray<BitSet>(tree), this);
    }

    /**
     * get size
     *
     * @return number of splits
     */
    public int size() {
        return nsplits;
    }

    /**
     * add a split
     *
     * @return index
     */
    public int addSplit(Split split) {
        nsplits++;
        index2split.put(nsplits, split);
        split2index.put(split, nsplits);

        return nsplits;
    }

    /**
     * gets a split
     *
     * @return split with given index
     */
    public Split getSplit(int index) {
        return index2split.get(index);
    }

    /**
     * gets the index of the split, if present, otherwise -1
     *
     * @return index or -1
     */
    public int indexOf(Split split) {
        Integer index = split2index.get(split);
        if (index == null)
            return -1;
        else
            return index;
    }

    /**
     * determines whether given split is already contained in the system
     *
     * @return true, if contained
     */
    public boolean contains(Split split) {
		return split2index.containsKey(split);
    }

    /**
     * gets a string representation
     *
     * @return string
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Splits (").append(nsplits).append("):\n");
        for (Iterator it = iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();
            buf.append(split).append("\n");
        }
        return buf.toString();
    }

    /**
     * clear
     */
    public void clear() {
        nsplits = 0;
        index2split.clear();
        split2index.clear();
    }

    /**
     * gets an iterator over all splits
     *
     * @return iterator
     */
    public Iterator<Split> iterator() {
        return split2index.keySet().iterator();
    }


    /**
     * given a phylogenetic tree and a set of taxa, returns all splits found in the tree.
     * Assumes the last taxon is an outgroup
     *
     * @return splits
     */
    public static SplitSystem getSplitsFromTree(Taxa allTaxa, BitSet activeTaxa, PhyloTree tree) {
        SplitSystem splits = new SplitSystem();

        splitsFromTreeRec(tree.getRoot(), tree, allTaxa, activeTaxa, new NodeArray<BitSet>(tree), splits);
        return splits;
    }

    /**
     * recursively extract splits from tree. Also works for cluster networks.
     *
     * @return taxa
     */
    private static BitSet splitsFromTreeRec(Node v, PhyloTree tree, Taxa allTaxa, BitSet activeTaxa, NodeArray<BitSet> reticulateNode2Taxa, SplitSystem splits) {
        BitSet e_taxa = new BitSet();

        if (tree.getLabel(v) != null) {
            var taxon = allTaxa.indexOf(tree.getLabel(v));
            if (taxon > -1)
                e_taxa.set(taxon);
        }

        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            Node w = f.getTarget();
            BitSet f_taxa;
            if (!tree.isReticulateEdge(f) || reticulateNode2Taxa.get(w) == null)
				f_taxa = splitsFromTreeRec(w, tree, allTaxa, activeTaxa, reticulateNode2Taxa, splits);
			else
				f_taxa = reticulateNode2Taxa.get(w);

			if (!tree.isReticulateEdge(f)) {
				BitSet complement = (BitSet) activeTaxa.clone();
				complement.andNot(f_taxa);
				Split split = new Split(f_taxa, complement, tree.getWeight(f));
				if (!splits.contains(split))
					splits.addSplit(split);
				else if (v == tree.getRoot() && v.getOutDegree() == 2) // is root split
				{
					Split prevSplit = splits.get(split);
					prevSplit.setWeight(prevSplit.getWeight() + split.getWeight());
				}
            } else
                reticulateNode2Taxa.put(w, f_taxa);

            e_taxa.or(f_taxa);
        }
        return e_taxa;
    }

    /**
     * gets a tree or network from a set of splits. Treats the last taxon as an outgroup.
     *
     * @return tree (or network)
     */
    public PhyloTree createTreeFromSplits(Taxa taxa, ProgressListener progressListener) {
        if (progressListener == null)
            progressListener = new ProgressSilent();

        return createTreeFromSplits(taxa, true, progressListener);
    }

    /**
     * gets a tree or network from a set of splits. Uses the last taxon listed in taxa as an outgroup
     *
     * @return tree or network
     */
    public PhyloTree createTreeFromSplits(Taxa taxa, boolean askToOptimize, ProgressListener progressListener) {
        if (progressListener == null)
            progressListener = new ProgressSilent();

        PhyloTree tree;
        if (taxa.size() == 0)
            return new PhyloTree();
        if (containsPairWithAllFourIntersections()) {
            System.err.println("not compatible");
            String MINIMAL_NETWORK = "Minimal Level-k Network";
            String CLUSTER_NETWORK = "Cluster Network";
            String GALLED_NETWORK = "Galled Network";
            int choice = -1;
            String[] options;
            if (DendroscopeProperties.ALLOW_MINIMAL_NETWORKS)
                options = new String[]{MINIMAL_NETWORK, GALLED_NETWORK, CLUSTER_NETWORK};
            else
                options = new String[]{GALLED_NETWORK, CLUSTER_NETWORK};

            String defaultChoice = DendroscopeProperties.ALLOW_MINIMAL_NETWORKS ? MINIMAL_NETWORK : CLUSTER_NETWORK;

            if (DendroscopeProperties.ALLOW_OPTIMIZE_NETWORKS && askToOptimize)
                choice = JOptionPane.showOptionDialog(null, "Choose network type:", "Choose network algorithm - Dendroscope",
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(), options, defaultChoice);
            if (choice >= 0 && options[choice].equals(MINIMAL_NETWORK)) {
                LevelKNetwork network = new LevelKNetwork(taxa, this);
                network.setComputeOnlyOne(true);
                network.setCheckTrees(true);
                tree = network.apply(progressListener).get(0);
                tree.setName("minimum-network");
            } else if (choice >= 0 && options[choice].equals(GALLED_NETWORK)) {
                ComputeGalledNetwork network = new ComputeGalledNetwork(taxa, this);
                tree = network.apply(progressListener);
                tree.setName("galled-network");
            } else {
                ClusterNetwork network = new ClusterNetwork(taxa, this);
                tree = network.apply();
                tree.setName("cluster-network");
            }
        } else {
            tree = new PhyloTree();
            NodeArray<BitSet> node2taxa = new NodeArray<>(tree); // map each node to the taxa beyond it

            Set<Split> usedSplits = new HashSet<>();
            Node center = tree.newNode();
            tree.setRoot(center);
            BitSet centerTaxa = new BitSet();

            // setup leaf nodes
            for (Iterator it = taxa.iterator(); it.hasNext(); ) {
                String name = (String) it.next();
                int t = taxa.indexOf(name);
                if (t != taxa.maxId()) {  // this condition drops the __outgroup__
                    Node v = tree.newNode();
                    Edge e = tree.newEdge(center, v);
                    centerTaxa.set(t);
                    tree.setLabel(v, name);
                    BitSet set = new BitSet();
                    set.set(t);
                    node2taxa.put(v, set);
                    Split split = getTrivial(t);
                    if (split != null) {
                        tree.setWeight(e, split.getWeight());
                        usedSplits.add(split);
                    }
                }
            }
            node2taxa.put(center, centerTaxa);

            // process all non-trivial splits
            for (Iterator it = iterator(); it.hasNext(); ) {
                Split split = (Split) it.next();
                if (!usedSplits.contains(split)) {
                    if (split.getSplitSize() == 1)
                        System.err.println("problem: " + split);
                    else
                        processSplit(center, node2taxa, taxa.maxId(), split, tree);
                }
            }
            tree.setName("");
        }
        return tree;
    }

    /**
     * inserts a split into a tree
     *
	 */
    public void processSplit(Node v, NodeArray<BitSet> node2taxa, int outGroupTaxonId, Split split, PhyloTree tree) {
        BitSet partB = split.getPartNotContainingTaxon(outGroupTaxonId);
        double weight = split.getWeight();

        boolean done = false;

        while (!done) {
            List<Edge> edgesToPush = new LinkedList<>();
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                Node w = f.getTarget();
                BitSet nodeSet = node2taxa.get(w);
                if (nodeSet.intersects(partB))
                    edgesToPush.add(f);
            }
            if (edgesToPush.size() == 1) // need to move down tree
            {
                Edge f = edgesToPush.get(0);
                v = f.getTarget();
            } else if (edgesToPush.size() > 1) { // more than one subtree contains taxa from the set, time to split
                Node u = tree.newNode();
                node2taxa.put(u, partB);
                Edge h = tree.newEdge(v, u);
                tree.setWeight(h, weight);

                for (Edge f : edgesToPush) {
                    Node w = f.getTarget();
                    Edge g = tree.newEdge(u, w);
                    tree.setWeight(g, tree.getWeight(f));
                }
                for (Edge f : edgesToPush) {
                    tree.deleteEdge(f);
                }
                done = true;
            } else {
                throw new RuntimeException("0 taxa in splitsToTreeRec");
            }
        }
    }

    /**
     * returns a trivial split separating index from all other taxa, if it exists
     *
     * @return trivial split, if it exists
     */
    public Split getTrivial(int taxonIndex) {
        for (Iterator it = iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();

            if (split.getA().cardinality() == 1 && split.getA().get(taxonIndex)
                    || split.getB().cardinality() == 1 && split.getB().get(taxonIndex))
                return split;
        }
        return null;
    }

    /**
     * add all given splits that are not already present (as splits, ignoring weights etc)
     *
     * @return number actually added
     */
    public int addAll(SplitSystem splits) {
        int count = 0;
        for (Iterator it = splits.iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();
			if (!split2index.containsKey(split)) {
				addSplit(split);
				count++;
			}
        }
        return count;
    }

    /**
     * get all splits in a new list
     *
     * @return list of splits
     */
    public List<Split> asList() {
        List<Split> result = new LinkedList<>();
        for (Iterator<Split> it = iterator(); it.hasNext(); ) {
            result.add(it.next());
        }
        return result;
    }

    /**
     * returns the splits as an array
     *
     * @return array of splits
     */
    public Split[] asArray() {
        Split[] result = new Split[size()];
        int count = 0;
        for (Iterator it = iterator(); it.hasNext(); ) {
            result[count++] = (Split) it.next();
        }
        return result;
    }

    /**
     * if the given split A|B is contained in this split system, return it.
     * This is useful because A|B might have a weight etc in the split system
     *
     * @return split
     */
    public Split get(Split split) {
        Integer index = split2index.get(split);
        if (index != null)
            return getSplit(index);
        else
            return null;

    }

    /**
     * determines whether these splits contains a pair with all four intersections
     *
	 */
    public boolean containsPairWithAllFourIntersections() {
        for (int s = 1; s <= size(); s++) {
            Split S = getSplit(s);
            for (int t = s + 1; t <= size(); t++) {
                Split T = getSplit(t);
                if (S.getA().intersects(T.getA()) && S.getA().intersects(T.getB())
                        && S.getB().intersects(T.getA()) && S.getB().intersects(T.getB()))
                    return true;
            }
        }
        return false;
    }

    /**
     * returns true, if all splits contain all taxa
     *
     * @return true, if full splits
     */
    public boolean isFullSplitSystem(Taxa taxa) {
        BitSet bits = taxa.getBits();

        for (Iterator it = iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();
            if (!split.getTaxa().equals(bits))
                return false;
        }
        return true;
    }

    /**
     * gets the splits as binary sequences in fastA format
     *
     * @return splits in fastA format
     */
    public String toStringAsBinarySequences(Taxa taxa) {
        StringBuilder buf = new StringBuilder();
        for (Iterator it = taxa.iterator(); it.hasNext(); ) {
            String name = (String) it.next();
            int t = taxa.indexOf(name);
            buf.append("> ").append(name).append("\n");

            for (int s = 1; s <= size(); s++) {
                Split split = getSplit(s);
                if (split.getA().get(t))
                    buf.append("1");
                else
                    buf.append("0");
            }
            buf.append("\n");
        }
        return buf.toString();
    }

    /**
     * returns the heaviest trivial split or null
     *
     * @return heaviest trivial split
     */
    public Split getHeaviestTrivialSplit() {
        Split result = null;
        for (int s = 1; s <= size(); s++) {
            Split split = getSplit(s);
            if (split.getSplitSize() == 1) {
                if (result == null)
                    result = split;
                else if (split.getWeight() > result.getWeight())
                    result = split;
            }
        }
        return result;
    }

    /**
     * delete all taxa listed
     *
     * @param taxa   taxa are removed from here
     * @return split set with taxa delated
     */
    public SplitSystem deleteTaxa(List<String> labels, Taxa taxa) {
        for (String label : labels) {
            taxa.remove(label);
        }
        SplitSystem result = new SplitSystem();

        for (Iterator it = iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();
            Split induced = split.getInduced(taxa.getBits());
            if (result.contains(induced)) {
                Split other = result.get(induced);
                other.setWeight(other.getWeight() + induced.getWeight());
            } else if (induced.getSplitSize() > 0) // make sure that is a proper split
                result.addSplit(induced);
        }
        return result;
    }

    /**
     * test program
     *
	 */
    public static void main(String[] args) throws IOException, CanceledException {
        Taxa taxa = new Taxa();
        taxa.add("a");
        taxa.add("b");
        taxa.add("c");
        taxa.add("d");
        taxa.add("e");

        SplitSystem splitSystem1;
        PhyloTree tree1 = new PhyloTree();
        tree1.parseBracketNotation("(((a,b),c),(d,e));", true);
        splitSystem1 = new SplitSystem(taxa, tree1);


        SplitSystem splitSystem2;
        PhyloTree tree2 = new PhyloTree();
        tree2.parseBracketNotation("((a,c),(d,(b,e)));", true);
        splitSystem2 = new SplitSystem(taxa, tree2);

        System.err.println("1: " + splitSystem1);
        System.err.println("2: " + splitSystem2);

        splitSystem1.addAll(splitSystem2);

        System.err.println("new: " + splitSystem1);

        int[] ordering = NeighborNetCycle.getNeighborNetOrdering(null, taxa.size(), NeighborNetCycle.setupMatrix(taxa.size(), splitSystem1));

        System.err.println("Ordering:");
        for (int value : ordering) {
            System.err.print(" " + value);
        }
        System.err.println();
    }
}
