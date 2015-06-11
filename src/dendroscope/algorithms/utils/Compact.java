/**
 * Compact.java 
 * Copyright (C) 2015 Daniel H. Huson
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
package dendroscope.algorithms.utils;

import dendroscope.consensus.Cluster;
import dendroscope.consensus.Taxa;
import jloda.util.Pair;
import jloda.util.Triplet;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * methods for compacting and uncompacting clusters
 * Daniel Huson, 4.2009
 */
public class Compact {
    static public final boolean DEBUG = false;

    /**
     * compact clusters by identifying non-distinquishable taxa
     *
     * @param clusters
     * @return a map that can be used to uncompact clusters
     */
    public static Map compactClusters(Cluster[] clusters) {
        Map mapBack = new HashMap();
        // System.err.println("original clusters:"); Cluster.print(clusters);

        int maxTax = 0;
        BitSet allTaxa = new BitSet();
        for (Cluster cluster2 : clusters) {
            for (int t = cluster2.nextSetBit(0); t >= 0; t = cluster2.nextSetBit(t + 1)) {
                if (t > maxTax)
                    maxTax = t;
                allTaxa.set(t);
            }
        }
        boolean[][] separated = new boolean[maxTax + 1][maxTax + 1];

        for (Cluster cluster1 : clusters) {
            BitSet complement = Cluster.setminus(allTaxa, cluster1);
            for (int t1 = cluster1.nextSetBit(0); t1 != -1; t1 = cluster1.nextSetBit(t1 + 1)) {
                for (int t2 = complement.nextSetBit(0); t2 != -1; t2 = complement.nextSetBit(t2 + 1))
                    separated[t1][t2] = separated[t2][t1] = true;
            }
        }

        int[] taxon2rep = new int[maxTax + 1];
        // map every taxon to the smallest one that it is not separated from:
        for (int t1 = allTaxa.nextSetBit(0); t1 != -1; t1 = allTaxa.nextSetBit(t1 + 1)) {
            int smallest = t1;
            for (int t2 = allTaxa.nextSetBit(0); t2 != -1; t2 = allTaxa.nextSetBit(t2 + 1))
                if (!separated[t1][t2] && t2 < smallest)
                    smallest = t2;
            taxon2rep[t1] = smallest;
        }

        BitSet redundant = new BitSet();
        for (int t1 = allTaxa.nextSetBit(0); t1 != -1; t1 = allTaxa.nextSetBit(t1 + 1)) {
            if (taxon2rep[t1] != t1)
                redundant.set(t1); // taxa that are redundant
        }

        // setup map back
        for (int t1 = allTaxa.nextSetBit(0); t1 != -1; t1 = allTaxa.nextSetBit(t1 + 1)) {
            Integer n = taxon2rep[t1];
            BitSet preImage = (BitSet) mapBack.get(n);
            if (preImage == null) {
                preImage = new BitSet();
                mapBack.put(n, preImage);
            }
            preImage.set(t1);
        }

        // compact all clusters:
        for (Cluster cluster : clusters) {
            cluster.andNot(redundant);

        }

        // System.err.println("compact clusters:"); Cluster.print(clusters);
        return mapBack;
    }

    /**
     * uncompact reticulations
     *
     * @param set
     * @param mapBack
     */
    public static void uncompactReticulations(BitSet set, Map mapBack, Taxa taxa, int maxTaxonId) {
        BitSet newTaxa = new BitSet();
        for (int t = set.nextSetBit(0); t != -1 && t <= maxTaxonId; t = set.nextSetBit((t + 1))) {
            BitSet orig = (BitSet) mapBack.get(t);
            newTaxa.or(orig);
            if (DEBUG) {
                System.err.print("Reticulation t=" + t + " covering taxa: ");
                for (int o = orig.nextSetBit(0); o != -1; o = orig.nextSetBit(o + 1))
                    System.err.print(" " + taxa.getLabel(o) + ",");
                System.err.println();
            }
        }
        set.clear();
        set.or(newTaxa);
    }


    /**
     * uncompact clusters
     *
     * @param clusters
     * @param mapBack
     */
    public static void uncompactClusters(Cluster[] clusters, Map mapBack, int maxTaxonId) {
        for (Cluster cluster : clusters) {
            uncompactCluster(cluster, mapBack, maxTaxonId);
        }
    }

    /**
     * uncompact pairs of bit sets
     *
     * @param pairs
     * @param mapBack
     */
    public static void uncompactPairs(List pairs, Map mapBack, int maxTaxonId) {
        for (Object pair1 : pairs) {
            Pair pair = (Pair) pair1;
            uncompactCluster((BitSet) pair.getFirst(), mapBack, maxTaxonId);
            uncompactCluster((BitSet) pair.getSecond(), mapBack, maxTaxonId);
        }
    }

    /**
     * uncompact triplets of clusters
     *
     * @param triplets
     * @param mapBack
     */
    public static void uncompactTriplets(List triplets, Map mapBack, int maxTaxonId) {
        for (Object triplet1 : triplets) {
            Triplet triplet = (Triplet) triplet1;
            if (triplet.getFirst() instanceof Cluster) {
                /*
              System.err.println("XXXXX: "+triplet.getFirst());
              Cluster tmp=new Cluster();
              tmp.set(1);tmp.set(2); tmp.set(3);tmp.set(4);tmp.set(7);

              if(Cluster.equals(tmp,(Cluster) triplet.getFirst()))
              {
                  System.err.print("UNCOMPACTING: "+tmp);
              uncompactCluster((Cluster) triplet.getFirst(), mapBack, maxTaxonId);
              System.err.println(" -> "+ triplet.getFirst());
              }
              else  */
                uncompactCluster((Cluster) triplet.getFirst(), mapBack, maxTaxonId);

            }
            if (triplet.getSecond() instanceof Cluster)
                uncompactCluster((Cluster) triplet.getSecond(), mapBack, maxTaxonId);
            if (triplet.getThird() instanceof Cluster)
                uncompactCluster((Cluster) triplet.getThird(), mapBack, maxTaxonId);
        }
    }

    /**
     * uncompact a clusters
     *
     * @param orig
     * @param mapBack
     */
    public static void uncompactCluster(BitSet orig, Map mapBack, int maxTaxonId) {
        if (orig != null) {
            BitSet set = new BitSet();
            for (int t = orig.nextSetBit(0); t != -1; t = orig.nextSetBit(t + 1)) {
                BitSet origTaxa = (BitSet) mapBack.get(t);
                if (origTaxa != null)
                    set.or(origTaxa);
                else if (t > maxTaxonId)
                    set.set(t); // assuming that t is a new additional artifical taxon used to guide the Hasse diagram algorithm
            }
            // System.err.println("map "+orig+" - > "+set);
            orig.clear();
            orig.or(set);
        }
    }
}
