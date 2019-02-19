/**
 * ExploreClusters.java 
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
package dendroscope.progs;

import dendroscope.consensus.Cluster;
import jloda.util.CommandLineOptions;
import jloda.util.UsageException;

import java.util.*;

/**
 * explore the possible combinations of clusters
 * Daniel Huson, Oct 2007
 */
public class ExploreClusters {

    /**
     * explore all combinations
     *
     * @param n
     * @param k
     * @param all
     */
    public void run(int n, int k, boolean all) {
        for (int n1 = (all ? 2 : n); n1 <= n; n1++) {
            System.err.println("====== n=" + n1 + " ========");
            for (int k1 = (k == 0 ? 2 : k); k1 <= (k == 0 ? n : k); k1++) {
                System.err.println("k=" + k1 + ":");
                run(n1, k1);
            }
        }
    }

    /**
     * explore all posible solutions for n and k
     *
     * @param n
     * @param k
     */
    public void run(int n, int k) {
        int count = 0;
        for (int size = 2; size <= n; size++) {
            BitSet cluster = new BitSet();
            for (int t = 1; t <= size; t++)
                cluster.set(t);
            count = makeClustersRec(size, 0, cluster, new Stack(), n, k, count);
        }
    }


    /**
     * recursively generate all sets of k clusters on n taxa.
     * clusters are reduced in that all taxa are separated and all clusters form an incompatibility component
     *
     * @param targetSize
     * @param prev
     * @param cluster
     * @param clusters
     * @param n
     * @param k
     * @return number of solutions found
     */
    private int makeClustersRec(int targetSize, int prev, BitSet cluster, Stack clusters, int n, int k, int count) {
        if (cluster.cardinality() < targetSize) // need to look at all ways of growing cluster
        {
            for (int t = prev + 1; t <= n; t++) {
                if (!cluster.get(t)) {
                    cluster.set(t);
                    count = makeClustersRec(targetSize, t, cluster, clusters, n, k, count);
                    cluster.set(t, false);
                }
            }
        } else // cluster has target size
        {
            clusters.push(cluster.clone());
            if (clusters.size() == k) {
                // check and print
                if (countUsedTaxa(clusters) == n && allSeparated(clusters, n) && oneComponent(clusters)) {
                    System.err.println("(" + (++count) + "): ");
                    for (Iterator it = clusters.iterator(); it.hasNext(); ) {
                        System.err.println("\t" + it.next());
                    }
                    System.err.println(computeH(clusters, n));
                }
            } else // start adding next cluster
            {
                for (int size = targetSize; size <= n; size++) {
                    int firstTaxon = (size == targetSize ? cluster.nextSetBit(0) + 1 : 1);
                    for (int t = firstTaxon; t < n; t++) {
                        BitSet newCluster = new BitSet();
                        newCluster.set(t);
                        count = makeClustersRec(size, t, newCluster, clusters, n, k, count);
                        newCluster.set(t, false);
                    }
                }
            }
            clusters.pop();
        }
        return count;
    }

    private int computeH(Stack clusters, int n) {
        BitSet[] array = (BitSet[]) clusters.toArray(new BitSet[clusters.size()]);

        List triples = new LinkedList();

        for (int i = 0; i < array.length; i++) {
            for (int j = i + 1; j < array.length; j++) {
                Triple triple = computeTriple(array[i], array[j]);
                if (triple != null) {
                    triples.add(triple);
                }
            }
        }

        if (triples.size() > 0) {
            System.err.println("Triples:");
            for (Iterator it = triples.iterator(); it.hasNext(); ) {
                Triple triple = (Triple) it.next();
                System.err.println(triple);
            }
        }

        return 0;
    }


    /**
     * reduces the input clusters using a given choice of optional taxa or taxon groups
     *
     * @param clusters
     * @param choices
     * @return reduced clusters
     */
    private Cluster[] reduceClusters(BitSet[] clusters, Collection choices) {
        Map fixed2optional = new HashMap(); // maps fixed cluster parts to their optional ones

        Set result = new HashSet();

        for (int i = 0; i < clusters.length; i++) {
            BitSet cluster = clusters[i];

            Cluster fixed = new Cluster();
            Cluster optional = new Cluster();

            partition(cluster, choices, fixed, optional);

            if (fixed.cardinality() > 0) {
                BitSet op = (BitSet) fixed2optional.get(fixed);
                if (op == null) {
                    fixed2optional.put(fixed, optional);
                    System.err.println("Mapped: " + cluster + " fixed: " + fixed + " optional: " + optional + " total optional: " + fixed2optional.get(fixed));
                } else
                    op.or(optional);
            }
            // todo: what to do with clusters that equal optional sets?
        }

        for (Iterator it = fixed2optional.keySet().iterator(); it.hasNext(); ) {
            BitSet fixed = (BitSet) it.next();
            BitSet optional = (BitSet) fixed2optional.get(fixed);
            BitSet cluster = Cluster.union(fixed, optional);
            result.add(new Cluster(cluster, 1));
        }
        // add all trivial:
        for (int i = 0; i < clusters.length; i++)
            if (clusters[i].cardinality() == 1 && !result.contains(clusters[i]))
                result.add(clusters[i]);

        return (Cluster[]) result.toArray(new Cluster[result.size()]);
    }

    /**
     * given a cluster and a set of choices of optional taxa or taxon-groups, partitions the cluster
     * into its mandatory and optional parts
     *
     * @param cluster
     * @param choices
     * @param fixed
     * @param optional
     */
    private void partition(BitSet cluster, Collection choices, BitSet fixed, BitSet optional) {
        for (Iterator it = choices.iterator(); it.hasNext(); ) {
            BitSet choice = (BitSet) it.next();
            if (Cluster.contains(cluster, choice))
                optional.or(choice);
        }
        fixed.or(Cluster.setminus(cluster, optional));
    }

    /**
     * count how many different taxa are used by this choice of clusters
     *
     * @param clusters
     * @return number of used taxa
     */
    private int countUsedTaxa(Stack clusters) {
        BitSet taxa = new BitSet();
        for (Iterator it = clusters.iterator(); it.hasNext(); ) {
            taxa.or((BitSet) it.next());
        }
        return taxa.cardinality();
    }

    /**
     * all clusters contained in one incompat. component?
     *
     * @param clusters
     * @return true, if in one component
     */
    private boolean oneComponent(Stack clusters) {
        BitSet[] array = (BitSet[]) clusters.toArray(new BitSet[clusters.size()]);
        boolean[][] incompatible = new boolean[array.length][array.length];
        for (int i = 0; i < array.length; i++) {
            for (int j = i + 1; j < array.length; j++) {
                incompatible[i][j] = incompatible[j][i] = Cluster.incompatible(array[i], array[j]);
            }
        }
        BitSet visited = new BitSet();
        Stack stack = new Stack();
        stack.push(new Integer(0));
        while (stack.size() > 0) {
            int i = ((Integer) stack.pop()).intValue();
            for (int j = 0; j < array.length; j++) {
                if (j != i && incompatible[i][j] && !visited.get(j)) {
                    visited.set(j);
                    stack.push(new Integer(j));
                }
            }
        }
        return visited.cardinality() == array.length;
    }


    /**
     * is each pair of taxa separated by some clusters?
     *
     * @param clusters
     * @param n
     * @return true, if separated
     */
    private boolean allSeparated(Stack clusters, int n) {
        for (int i = 1; i <= n; i++) {
            for (int j = i + 1; j <= n; j++) {
                boolean ko = true;
                for (Iterator it = clusters.iterator(); it.hasNext(); ) {
                    BitSet cluster = (BitSet) it.next();
                    if (cluster.get(i) != cluster.get(j)) {
                        ko = false;
                        break;
                    }
                }
                if (ko)
                    return false;
            }
        }
        return true;
    }


    /**
     * run a command-line program
     *
     * @param args
     * @throws UsageException
     */
    public static void main(String[] args) throws UsageException {
        CommandLineOptions options = new CommandLineOptions(args);
        options.setDescription("explore-clusters - explore clusters");
        int n = options.getOption("-n", "value for n", 4);
        int k = options.getOption("-k", "value for k, if 0, do all", 0);
        boolean all = options.getOption("-a", "do all values from 2 upto n", true, false);
        options.done();

        ExploreClusters exploreClusters = new ExploreClusters();
        exploreClusters.run(n, k, all);
    }

    /**
     * computes the intersection triple, if clusters are incompatible, otherwise null
     *
     * @param cluster1
     * @param cluster2
     * @return intersection triple or null
     */
    public Triple computeTriple(BitSet cluster1, BitSet cluster2) {
        Triple triple = new Triple();

        triple.A = Cluster.setminus(cluster1, cluster2);
        triple.B = Cluster.intersection(cluster1, cluster2);
        triple.C = Cluster.setminus(cluster2, cluster1);
        if (triple.A.cardinality() > 0 && triple.B.cardinality() > 0 && triple.C.cardinality() > 0
                && !triple.A.equals(triple.B) && !triple.A.equals(triple.C) && !triple.B.equals(triple.C))
            return triple;
        else
            return null;
    }

    /**
     * a triple
     * todo: make triple comparable
     */
    class Triple {
        BitSet A;
        BitSet B;
        BitSet C;

        /**
         * is triple hit by H?
         *
         * @param H
         * @return true, if H contains one part of the triple
         */
        boolean isHitBy(BitSet H) {
            return H.equals(A) || H.equals(B) || H.equals(C);
        }

        public String toString() {
            return A.toString() + " | " + B.toString() + " | " + C.toString();
        }


        /**
         * removes H from all parts of the triple. If one part becomes empty, returns null
         *
         * @param H
         * @return reduced triple or null
         */
        public Triple reduce(BitSet H) {
            Triple result = new Triple();
            if (Cluster.contains(A, H)) {
                result.A = Cluster.setminus(A, H);
                if (result.A.cardinality() == 0)
                    return null;
            } else
                result.A = (BitSet) A.clone();
            if (Cluster.contains(B, H)) {
                result.B = Cluster.setminus(B, H);
                if (result.B.cardinality() == 0)
                    return null;
            } else
                result.B = (BitSet) B.clone();
            if (Cluster.contains(C, H)) {
                result.C = Cluster.setminus(C, H);
                if (result.C.cardinality() == 0)
                    return null;
            } else
                result.C = (BitSet) C.clone();
            return result;
        }
    }

}
