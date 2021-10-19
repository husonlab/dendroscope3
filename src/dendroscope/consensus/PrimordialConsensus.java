/*
 *   PrimordialConsensus.java Copyright (C) 2020 Daniel H. Huson
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

import dendroscope.algorithms.clusternet.ClusterNetwork;
import dendroscope.core.Document;
import dendroscope.core.TreeData;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeDoubleArray;
import jloda.phylo.PhyloTree;
import jloda.util.CanceledException;
import jloda.util.StringUtils;
import jloda.util.Triplet;
import jloda.util.progress.ProgressListener;

import java.io.IOException;
import java.util.*;

/**
 * computes the primordial consensus supertree of a set of gene trees
 * Based on an algorithm proposed by Mike Steel
 * Daniel Huson, 7.2012
 */
public class PrimordialConsensus implements IConsensusTreeMethod {
    public static final String NAME = "Primordial";
    private static final boolean verbose = false;

    private final static QuartetTopology unresolved = new QuartetTopology(0, 0, 0, 0);  // just used to signal that topology is unresolved

    /**
     * compute the consensus
     *
     * @param trees
     * @return consensus tree
     */
    public PhyloTree apply(Document doc, TreeData[] trees) throws CanceledException, IOException {
        ProgressListener progressListener = doc.getProgressListener();

        progressListener.setSubtask("Addressing taxa in trees");
        progressListener.setMaximum(trees.length);
        progressListener.setProgress(0);

        // compute address of all taxa in all trees:
        TaxId2Address[] taxId2Address = new TaxId2Address[trees.length];
        Taxa taxa = new Taxa();
        for (int t = 0; t < trees.length; t++) {
            PhyloTree tree = trees[t];
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                if (v.getOutDegree() == 0) {
                    String name = tree.getLabel(v);
                    if (!taxa.contains(name))
                        taxa.add(name);
                }
            }
            taxId2Address[t] = new TaxId2Address();
            computeLabel2AddressRec(tree, taxa, tree.getRoot(), "", taxId2Address[t]);
            progressListener.incrementProgress();
        }
        if (verbose)
            System.err.println("Total number of taxa: " + taxa.size());

        if (verbose) {
            for (int i = 1; i <= taxa.size(); i++)
                System.err.println(taxa.getLabel(i) + " -> " + i);
        }

        progressListener.setSubtask("Computing active rooted triples");
        progressListener.setMaximum(trees.length);
        progressListener.setProgress(taxa.size());

        // computing all active triplets:
        Map<FourTaxa, QuartetTopology> four2topology = new HashMap<>();

        SortedSet<Triplet<Integer, Integer, Integer>> missingTriplets = new TreeSet<>();

        BitSet taxaBits = taxa.getBits();
        for (int a = taxaBits.nextSetBit(0); a != -1; a = taxaBits.nextSetBit(a + 1)) {
            for (int b = taxaBits.nextSetBit(a + 1); b != -1; b = taxaBits.nextSetBit(b + 1)) {
                for (int c = taxaBits.nextSetBit(b + 1); c != -1; c = taxaBits.nextSetBit(c + 1)) {
                    QuartetTopology topology = computeTopology(a, b, c, taxId2Address);
                    if (topology == null) {
                        missingTriplets.add(new Triplet<>(a, b, c));
                    } else if (topology != unresolved) {
                        FourTaxa four = new FourTaxa(0, a, b, c);
                        four2topology.put(four, topology);
                        if (verbose)
                            System.err.println("Four: " + StringUtils.toString(four) + " Quartet: " + topology);
                    } else {
                        FourTaxa four = new FourTaxa(0, a, b, c);
                        if (verbose)
                            System.err.println("Four: " + StringUtils.toString(four) + " Quartet: unresolved");
                    }
                }
            }
            progressListener.incrementProgress();
        }
        if (verbose)
            System.err.println("Total number of rooted triples: " + four2topology.size());

        if (missingTriplets.size() > 0) {
            int[] counts = new int[taxa.size() + 1];
            System.err.println("The following triplets are not present in any input tree:");
            for (Triplet<Integer, Integer, Integer> triplet : missingTriplets) {
                SortedSet<String> labels = new TreeSet<>();
                counts[triplet.getFirst()]++;
                counts[triplet.getSecond()]++;
                counts[triplet.getThird()]++;
                if (missingTriplets.size() < 100) {
                    labels.add(taxa.getLabel(triplet.getFirst()));
                    labels.add(taxa.getLabel(triplet.getSecond()));
                    labels.add(taxa.getLabel(triplet.getThird()));
                }
                Iterator<String> it = labels.iterator();
                if (it != null && it.hasNext())
                    System.err.println(String.format("%s, %s, %s", it.next(), it.next(), it.next()));
            }
            int worst = 0;
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] > counts[worst])
                    worst = i;
            }
            System.err.println("Worst taxon is " + taxa.getLabel(worst) + ", contained in " + counts[worst] + " of " + missingTriplets.size() + " missing triplets");
            throw new IOException("Primordial consensus not applicable. There are " + missingTriplets.size() + " missing triplets, worst taxon is: " + taxa.getLabel(worst)
                    + " (in " + counts[worst] + " missing triplets)");
        }

        progressListener.setSubtask("Building clusters");
        progressListener.setMaximum(trees.length);
        progressListener.setProgress(taxa.size());

        // build splits from the triplets:
        LinkedList<Split> splits = new LinkedList<>();
        LinkedList<Split> nextSplits = new LinkedList<>();

        BitSet addedTaxa = new BitSet();
        // process the first quartet:
        QuartetTopology initialTopology = four2topology.values().iterator().next();
        addedTaxa.or(initialTopology.getAll());
        splits.add(initialTopology.asSplit());
        splits.add(initialTopology.asSplit(0));
        splits.add(initialTopology.asSplit(1));
        splits.add(initialTopology.asSplit(2));
        splits.add(initialTopology.asSplit(3));

        if (verbose) {
            System.err.println("Initial splits:");
            for (Split split : splits) {
                System.err.println(split);
            }
        }

        taxaBits.andNot(addedTaxa); // don't loop over the taxa already processed
        for (int x = taxaBits.nextSetBit(1); x != -1; x = taxaBits.nextSetBit(x + 1)) {
            if (verbose)
                System.err.println("Adding: " + x);
            for (Split split : splits) {
                // try one side:
                {
                    boolean ok = true;
                    for (int a = split.getA().nextSetBit(0); ok && a != -1; a = split.getA().nextSetBit(a + 1)) {
                        for (int b1 = split.getB().nextSetBit(0); ok && b1 != -1; b1 = split.getB().nextSetBit(b1 + 1)) {
                            for (int b2 = split.getB().nextSetBit(b1 + 1); ok && b2 != -1; b2 = split.getB().nextSetBit(b2 + 1)) {
                                FourTaxa four = new FourTaxa(x, a, b1, b2);
                                QuartetTopology topology = four2topology.get(four);
                                if (verbose)
                                    System.err.println("Four: " + StringUtils.toString(four) + " Quartet: " + topology);
                                if (topology == null || !topology.separates(a, b1, b2))
                                    ok = false;
                            }
                            if (b1 == 0)
                                break;
                        }
                        if (a == 0)
                            break;
                    }
                    if (ok) {
                        BitSet A = (BitSet) split.getA().clone();
                        BitSet B = (BitSet) split.getB().clone();
                        A.set(x);
                        Split newSplit = new Split(A, B);
                        if (verbose)
                            System.err.println("Extending " + split + " to " + newSplit);
                        nextSplits.add(newSplit);
                    }
                }
                // try the other side:
                {
                    boolean ok = true;
                    for (int a1 = split.getA().nextSetBit(0); ok && a1 != -1; a1 = split.getA().nextSetBit(a1 + 1)) {
                        for (int a2 = split.getA().nextSetBit(a1 + 1); ok && a2 != -1; a2 = split.getA().nextSetBit(a2 + 1)) {
                            for (int b = split.getB().nextSetBit(0); ok && b != -1; b = split.getB().nextSetBit(b + 1)) {
                                QuartetTopology topology = four2topology.get(new FourTaxa(x, b, a1, a2));
                                if (topology == null || !topology.separates(b, a1, a2))
                                    ok = false;
                                if (b == 0)
                                    break;
                            }
                        }
                        if (a1 == 0)
                            break;
                    }
                    if (ok) {
                        BitSet A = (BitSet) split.getA().clone();
                        BitSet B = (BitSet) split.getB().clone();
                        B.set(x);
                        Split newSplit = new Split(A, B);
                        if (verbose)
                            System.err.println("Extending " + split + " to " + newSplit);
                        nextSplits.add(newSplit);
                    }
                }
            }

            Split newSplit = new Split();
            BitSet xSet = new BitSet();
            xSet.set(x);
            newSplit.set((BitSet) addedTaxa.clone(), xSet);
            if (verbose)
                System.err.println("Adding " + newSplit);
            nextSplits.add(newSplit);

            LinkedList<Split> tmp = splits;
            splits = nextSplits;
            nextSplits = tmp;
            nextSplits.clear();
            addedTaxa.set(x);
            progressListener.incrementProgress();
        }
        if (verbose)
            System.err.println("Total number of clusters: " + splits.size());

        if (verbose) {
            for (Split split : splits) {
                System.err.println("Split: " + split);
            }
        }
        // convert to clusters:
        Set<Cluster> clusters = new HashSet<>();
        for (Split split : splits) {
            Cluster cluster;
            if (split.getA().get(0))
                cluster = new Cluster(split.getB());
            else
                cluster = new Cluster(split.getA());
            if (cluster.cardinality() < taxa.size()) {
                clusters.add(cluster);
            }
        }

        // add all trivial clusters:
        for (int t = 1; t <= taxa.size(); t++) {
            Cluster cluster = new Cluster();
            cluster.set(t);
            if (!clusters.contains(cluster))
                clusters.add(cluster);
        }

        List<Triplet<Cluster, Cluster, Boolean>> additionalEdges = new LinkedList<>();
        PhyloTree tree = new PhyloTree();
        NodeDoubleArray node2weight = new NodeDoubleArray(tree);
        NodeDoubleArray node2confidence = new NodeDoubleArray(tree);
        Node root = tree.newNode();
        tree.setRoot(root);

        ClusterNetwork.constructHasse(taxa, tree, root, clusters.toArray(new Cluster[clusters.size()]), node2weight, node2confidence, additionalEdges, taxa.size());

        ClusterNetwork.convertHasseToClusterNetwork(tree, node2weight);
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            if (tree.getNumberOfTaxa(v) > 0) {
                tree.setLabel(v, taxa.getLabel(tree.getTaxa(v).iterator().next()));
            }
        }

        return tree;
    }


    /**
     * determines which triplet topology is the majority topology   and returns it as a quartet (including 0 as formal root)
     *
     * @param a
     * @param b
     * @param c
     * @param taxId2Address
     * @return majority topology
     */
    private QuartetTopology computeTopology(int a, int b, int c, TaxId2Address[] taxId2Address) {
        int count_ab_c = 0;
        int count_ac_b = 0;
        int count_bc_a = 0;
        boolean seen = false;

        for (TaxId2Address map : taxId2Address) {
            String addressA = map.get(a);
            String addressB = map.get(b);
            String addressC = map.get(c);
            if (addressA == null || addressB == null || addressC == null)  // taxon not present in this tree
                continue;
            seen = true;

            int prefixLengthAB = getCommonPrefix(new String[]{addressA, addressB}).length();
            int prefixLengthAC = getCommonPrefix(new String[]{addressA, addressC}).length();
            int prefixLengthBC = getCommonPrefix(new String[]{addressB, addressC}).length();
            if (prefixLengthAB > prefixLengthAC)
                count_ab_c++;
            else if (prefixLengthAC > prefixLengthBC)
                count_ac_b++;
            else if (prefixLengthBC > prefixLengthAC)
                count_bc_a++;
        }

        if (count_ab_c > count_ac_b && count_ab_c > count_bc_a)
            return new QuartetTopology(a, b, c, 0);
        else if (count_ac_b > count_ab_c && count_ac_b > count_bc_a)
            return new QuartetTopology(a, c, b, 0);
        else if (count_bc_a > count_ab_c && count_bc_a > count_ac_b)
            return new QuartetTopology(b, c, a, 0);
        if (!seen)
            return null;
        return unresolved;
    }

    /**
     * computes the label2address mapping
     *
     * @param v
     * @param path
     */
    private void computeLabel2AddressRec(PhyloTree tree, Taxa taxa, Node v, String path, TaxId2Address taxId2Address) {
        if (v.getOutDegree() == 0) {
            Integer taxId = taxa.indexOf(tree.getLabel(v));
            taxId2Address.put(taxId, path);
        } else {
            char count = 0;
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                count++;
                computeLabel2AddressRec(tree, taxa, f.getTarget(), path + count, taxId2Address);
            }
        }
    }

    /**
     * given a set of addresses, returns the common prefix
     *
     * @param addresses
     * @return prefix
     */
    public String getCommonPrefix(String[] addresses) {
        if (addresses.length == 0)
            return "";
        else if (addresses.length == 1)
            return addresses[0];

        for (int i = 0; ; i++) {
            int alive = 0;
            String first = null;
            for (String address : addresses) {
                if (i < address.length()) {
                    alive++;
                    if (first == null)
                        first = address;
                    else {
                        if (first.charAt(i) != address.charAt(i))
                            return address.substring(0, i);
                    }
                }
            }
            if (alive < 2) {
                assert first != null;
                if (i > 0)
                    return first.substring(0, i - 1);
                else
                    return "";
            }
        }
    }

    /**
     * gets the sorted triplet
     *
     * @param a
     * @param b
     * @param c
     * @return sorted triplet
     */
    private Triplet<Integer, Integer, Integer> getSortedTriplet(int a, int b, int c) {
        int min = Math.min(a, Math.min(b, c));
        int max = Math.max(a, Math.max(b, c));
        int mid = a;
        if (mid == min || mid == max)
            mid = c;
        if (mid == min || mid == max)
            mid = b;
        return new Triplet<>(min, mid, max);
    }

    class TaxId2Address extends HashMap<Integer, String> {
    }

    class FourTaxa extends BitSet {
        public FourTaxa(int a, int b, int c, int d) {
            super();
            set(a);
            set(b);
            set(c);
            set(d);
        }
    }

    static class QuartetTopology {
        final private Integer[] data;
        final private BitSet all;
        final private Split split;

        public QuartetTopology(int a, int b, int c, int d) {
            data = new Integer[]{a, b, c, d};

            all = new BitSet();
            for (int x : data)
                all.set(x);

            BitSet A = new BitSet();
            A.set(data[0]);
            A.set(data[1]);
            BitSet B = new BitSet();
            B.set(data[2]);
            B.set(data[3]);
            split = new Split(A, B);
        }

        public String toString() {
            return data[0] + " " + data[1] + " | " + data[2] + " " + data[3];
        }

        public int get(int i) {
            return data[i];
        }

        public int hashCode() {
            return Arrays.hashCode(data);
        }

        public BitSet getAll() {
            return (BitSet) all.clone();
        }

        public Split asSplit() {
            return (Split) split.clone();
        }

        public Split asSplit(int i) {
            BitSet A = new BitSet();
            A.set(data[i]);
            BitSet B = getAll();
            B.set(data[i], false);
            return new Split(A, B);
        }

        /**
         * does split separate a from b1 and b2?
         *
         * @param a
         * @param b1
         * @param b2
         * @return true, if split separates a from b1, b2
         */
        public boolean separates(int a, int b1, int b2) {
            return (split.getA().get(a) != split.getA().get(b1)) && (split.getA().get(b1) == split.getA().get(b2));
        }
    }
}
