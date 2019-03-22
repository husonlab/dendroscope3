/**
 * ComputeHybridizationNetwork.java 
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
package dendroscope.autumn.hybridnetwork;

import dendroscope.autumn.PostProcess;
import dendroscope.autumn.PreProcess;
import dendroscope.autumn.Refine;
import dendroscope.autumn.Root;
import dendroscope.autumn.hybridnumber.ComputeHybridNumber;
import dendroscope.consensus.Cluster;
import dendroscope.consensus.Taxa;
import dendroscope.core.TreeData;
import jloda.graph.Edge;
import jloda.swing.util.ProgramProperties;
import jloda.util.*;
import org.apache.commons.collections.map.LRUMap;

import java.io.IOException;
import java.util.*;

/**
 * computes minimal hybridization networks for two multifurcating trees
 * Daniel Huson, 4.2011
 */
public class ComputeHybridizationNetwork {
    private final static int LARGE = 10000;
    public static final boolean checking = false;
    public boolean verbose = false;

    private int numberOfLookups = 0;

    private final LRUMap lookupTable = new LRUMap(1000000);

    private long nextTime = 0;
    private long waitTime = 1000;

    private ProgressListener progressListener;

    /**
     * computes the hybrid number for two multifurcating trees
     *
     * @param tree1
     * @param tree2
     * @param hybridizationNumber
     * @return reduced trees
     */
    public static TreeData[] apply(TreeData tree1, TreeData tree2, ProgressListener progressListener, Single<Integer> hybridizationNumber) throws IOException, CanceledException {
        int upperBound = ComputeHybridNumber.apply(tree1, tree2, progressListener);

        ComputeHybridizationNetwork computeHybridizationNetwork = new ComputeHybridizationNetwork();
        computeHybridizationNetwork.progressListener = progressListener;
        return computeHybridizationNetwork.run(tree1, tree2, upperBound, hybridizationNumber);
    }

    /**
     * run the algorithm
     *
     * @param tree1
     * @param tree2
     * @param hybridizationNumber
     * @return reduced trees
     */
    private TreeData[] run(TreeData tree1, TreeData tree2, int upperBound, Single<Integer> hybridizationNumber) throws IOException, CanceledException {
        verbose = ProgramProperties.get("verbose-HL", false);
        Taxa allTaxa = new Taxa();
        Pair<Root, Root> roots = PreProcess.apply(tree1, tree2, allTaxa);
        Root root1 = roots.getFirst();
        Root root2 = roots.getSecond();

        if (root1.getOutDegree() == 1 && root1.getFirstOutEdge().getTarget().getOutDegree() > 0) {
            Root tmp = (Root) root1.getFirstOutEdge().getTarget();
            root1.deleteNode();
            root1 = tmp;
        }

        if (root2.getOutDegree() == 1 && root2.getFirstOutEdge().getTarget().getOutDegree() > 0) {
            Root tmp = (Root) root2.getFirstOutEdge().getTarget();
            root2.deleteNode();
            root2 = tmp;
        }

        BitSet onlyTree1 = Cluster.setminus(root1.getTaxa(), root2.getTaxa());
        BitSet onlyTree2 = Cluster.setminus(root2.getTaxa(), root1.getTaxa());

        if (root1.getTaxa().cardinality() == onlyTree1.cardinality())
            throw new IOException("None of the taxa in second tree are contained in first tree");
        if (root2.getTaxa().cardinality() == onlyTree2.cardinality())
            throw new IOException("None of the taxa in first tree are contained in second tree");

        if (onlyTree1.cardinality() > 0) {
            System.err.println("Killing all taxa only present in first tree: " + onlyTree1.cardinality());
            for (int t = onlyTree1.nextSetBit(0); t != -1; t = onlyTree1.nextSetBit(t + 1)) {
                RemoveTaxon.apply(root1, 1, t);
            }
        }

        if (onlyTree2.cardinality() > 0) {
            System.err.println("Killing all taxa only present in second tree: " + onlyTree2.cardinality());
            for (int t = onlyTree2.nextSetBit(0); t != -1; t = onlyTree2.nextSetBit(t + 1)) {
                RemoveTaxon.apply(root2, 2, t);
            }
        }

        // run the refine algorithm
        System.err.println("Computing common refinement of both trees");
        Refine.apply(root1, root2);

        if (tree1.getRoot() == null || tree2.getRoot() == null) {
            throw new IOException("Can't compute hybridization networks, at least one of the trees is empty or unrooted");
        }

        // we maintain both trees in lexicographic order for ease of comparison
        root1.reorderSubTree();
        root2.reorderSubTree();

        System.err.println("Computing hybridization networks using Autumn algorithm (Autumn algorithm, Huson and Linz, 2016)...");
        progressListener.setTasks("Computing hybridization networks", "(Unknown how long this will really take)");
        progressListener.setMaximum(20);
        progressListener.setProgress(0);
        long startTime = System.currentTimeMillis();
        nextTime = startTime + waitTime;
        Set<Root> result = new TreeSet<>(new NetworkComparator());
        int h = computeRec(root1, root2, false, getAllAliveTaxa(root1, root2), upperBound, result, ">");


        fixOrdering(result);

        if (false) {
            Collection<Root> maafs = MAAFUtils.computeAllMAAFs(result);
            System.err.println("MAAFs before:");
            for (Root root : maafs) {
                System.err.println(root.toStringNetworkFull());
            }
        }
        int numberOfDuplicatesRemoved = MAAFUtils.removeDuplicateMAAFs(result, false);
        if (numberOfDuplicatesRemoved > 0)
            System.err.println("MAAF duplicates removed: " + numberOfDuplicatesRemoved);
        if (false) {
            Collection<Root> maafs = MAAFUtils.computeAllMAAFs(result);
            System.err.println("MAAFs after:");
            for (Root root : maafs) {
                System.err.println(root.toStringNetworkFull());
            }
        }

        fixOrdering(result);

        BitSet missingTaxa = Cluster.union(onlyTree1, onlyTree2);
        if (missingTaxa.cardinality() > 0) {
            System.err.println("Reattaching killed taxa: " + missingTaxa.cardinality());
            for (Root r : result) {
                for (int t = missingTaxa.nextSetBit(0); t != -1; t = missingTaxa.nextSetBit(t + 1)) {
                    RemoveTaxon.unapply(r, t);
                }
            }
        }

        System.err.println("Hybridization number: " + h);
        hybridizationNumber.set(h);
        System.err.println("Total networks: " + result.size());
        System.err.println("Time: " + ((System.currentTimeMillis() - startTime) / 1000) + " secs");

        System.err.println("(Size lookup table: " + lookupTable.size() + ", number of times used: " + numberOfLookups + ")");
        lookupTable.clear();
        System.gc();

        if (false) {
            System.err.println("Networks:");
            for (Root root : result) {
                System.err.println(root.toStringNetworkFull());
            }
        }

        System.gc();

        List<TreeData> list = PostProcess.apply(result.toArray(new Root[result.size()]), allTaxa, false);
        return list.toArray(new TreeData[list.size()]);
    }

    /**
     * this is called between recursive calls of the algorithm to cache networks already computed
     *
     * @param root1
     * @param root2
     * @param isReduced
     * @param candidateHybrids
     * @param k
     * @param totalResults
     * @param depth
     * @return cached networks or newly computed networks
     * @throws java.io.IOException
     * @throws jloda.util.CanceledException
     */
    private int cacheComputeRec(Root root1, Root root2, boolean isReduced, BitSet candidateHybrids, int k, Collection<Root> totalResults, String depth) throws IOException, CanceledException {
        if (true) // use caching
        {
            String key = root1.toStringTree() + root2.toStringTree() + (candidateHybrids != null ? Basic.toString(candidateHybrids) : "");
            Pair<Integer, Collection<Root>> cachedResults = (Pair<Integer, Collection<Root>>) lookupTable.get(key);
            if (cachedResults != null) {
                totalResults.addAll(cachedResults.getSecond());

                if (cachedResults.getFirst() <= k) {
                    numberOfLookups++;
                    totalResults.addAll(cachedResults.getSecond());
                    return cachedResults.getFirst();
                }

                return cachedResults.getFirst();
            } else {
                TreeSet<Root> newResults = new TreeSet<Root>(new NetworkComparator());
                int h = computeRec(root1, root2, isReduced, candidateHybrids, k, newResults, depth);

                if (h > 0)
                    lookupTable.put(key, new Pair<Integer, Collection<Root>>(h, newResults));
                totalResults.addAll(newResults);
                return h;
            }
        } else {
            return computeRec(root1, root2, isReduced, candidateHybrids, k, totalResults, depth);
        }
    }

    /**
     * recursively compute the hybrid number
     *
     * @param root1
     * @param root2
     * @param isReduced    @return hybrid number
     * @param k
     * @param totalResults
     */
    private int computeRec(Root root1, Root root2, boolean isReduced, BitSet candidateHybridsOriginal, int k, Collection<Root> totalResults, String depth) throws IOException, CanceledException {
        if (verbose) {
            System.err.println(depth + "---------- ComputeRec:");
            System.err.println(depth + "Tree1: " + root1.toStringFullTreeX());
            System.err.println(depth + "Tree2: " + root2.toStringFullTreeX());
        }

        if (System.currentTimeMillis() > nextTime) {
            progressListener.incrementProgress();
            nextTime += waitTime;
            waitTime *= 1.5;
        } else
            progressListener.checkForCancel();

        // root1.reorderSubTree();
        //  root2.reorderSubTree();
        if (checking) {
            root1.checkTree();
            root2.checkTree();
            if (!root2.getTaxa().equals(root1.getTaxa()))
                throw new RuntimeException("Unequal taxon sets: X=" + Basic.toString(root1.getTaxa()) + " vs " + Basic.toString(root2.getTaxa()));
        }

        if (!isReduced) {
            // 1. try to perform a subtree reduction:
            {
                final Single<Integer> placeHolderTaxon = new Single<Integer>();
                List<Pair<Root, Root>> reducedSubtreePairs = new LinkedList<Pair<Root, Root>>();

                switch (SubtreeReduction.apply(root1, root2, reducedSubtreePairs, placeHolderTaxon)) {
                    case ISOMORPHIC:
                        Root isomorphicTree = MergeIsomorphicInducedTrees.apply(root1, root2);
                        if (verbose) {
                            System.err.println(depth + "Trees are isomorphic");
                            System.err.println(depth + "Isomorphic tree: " + isomorphicTree.toStringFullTreeX());
                        }
                        totalResults.add(isomorphicTree);
                        return 0; // two trees are isomorphic, no hybrid node needed
                    case REDUCED:  // a reduction was performed, cannot maintain lexicographical ordering in removal loop below
                        List<Root> subTrees = new LinkedList<Root>();
                        for (Pair<Root, Root> pair : reducedSubtreePairs) {
                            subTrees.add(MergeIsomorphicInducedTrees.apply(pair.getFirst(), pair.getSecond()));
                        }
                        if (verbose) {
                            System.err.println(depth + "Trees are reducible:");
                            System.err.println(depth + "Tree1-reduced: " + root1.toStringFullTreeX());
                            System.err.println(depth + "Tree2-reduced: " + root2.toStringFullTreeX());
                            for (Root root : subTrees) {
                                System.err.println(depth + "Merged reduced subtree: " + root.toStringFullTreeX());
                            }
                        }

                        BitSet candidateHybrids;
                        if (false)
                            candidateHybrids = getAllAliveTaxa(root1, root2);  // need to reconsider all possible hybrids
                        else {
                            candidateHybrids = (BitSet) candidateHybridsOriginal.clone();
                            candidateHybrids.set(placeHolderTaxon.get(), true);
                        }

                        Collection<Root> currentResults = new TreeSet<Root>(new NetworkComparator());

                        int h = cacheComputeRec(root1, root2, false, candidateHybrids, k, currentResults, depth + " >");
                        List<Root> merged = MergeNetworks.apply(currentResults, subTrees);
                        if (verbose) {
                            for (Root r : merged) {
                                System.err.println(depth + "Result-merged: " + r.toStringNetworkFull());
                            }
                        }
                        totalResults.addAll(fixOrdering(merged));
                        return h;
                    case IRREDUCIBLE:
                        if (verbose)
                            System.err.println(depth + "Trees are subtree-irreducible");
                        break;
                }
            }

            // 2. try to perform a cluster reduction:
            {
                final Single<Integer> placeHolderTaxon = new Single<Integer>();
                Pair<Root, Root> clusterTrees = ClusterReduction.apply(root1, root2, placeHolderTaxon);

                if (clusterTrees != null) {
                    Set<Root> resultBottomPair = new TreeSet<Root>(new NetworkComparator());
                    int h = cacheComputeRec(clusterTrees.getFirst(), clusterTrees.getSecond(), true, candidateHybridsOriginal, k, resultBottomPair, depth + " >");

                    // for the top pair, we should reconsider the place holder in the top pair as a possible place holder
                    BitSet candidateHybrids = (BitSet) candidateHybridsOriginal.clone();

                    candidateHybrids.set(placeHolderTaxon.get(), true);

                    Set<Root> resultTopPair = new TreeSet<Root>(new NetworkComparator());
                    h += cacheComputeRec(root1, root2, false, candidateHybrids, k - h, resultTopPair, depth + " >");

                    Set<Root> currentResults = new TreeSet<Root>(new NetworkComparator());

                    for (Root r : resultBottomPair) {
                        currentResults.addAll(MergeNetworks.apply(resultTopPair, Arrays.asList(r)));
                    }
                    if (verbose) {
                        System.err.println(depth + "Cluster reduction applied::");
                        System.err.println(depth + "Tree1-reduced: " + root1.toStringFullTreeX());
                        System.err.println(depth + "Tree2-reduced: " + root2.toStringFullTreeX());
                        System.err.println(depth + "Subtree-1:     " + clusterTrees.getFirst().toStringFullTreeX());
                        System.err.println(depth + "Subtree-2:     " + clusterTrees.getSecond().toStringFullTreeX());

                        for (Root r : resultBottomPair) {
                            System.err.println(depth + "Results for reduced-trees: " + r.toStringNetworkFull());
                        }

                        for (Root r : resultTopPair) {
                            System.err.println(depth + "Results for sub-trees: " + r.toStringNetworkFull());
                        }

                        for (Root r : currentResults) {
                            System.err.println(depth + "Merged cluster-reduced networks: " + r.toStringNetworkFull());
                        }
                    }
                    totalResults.addAll(currentResults);
                    clusterTrees.getFirst().deleteSubTree();
                    clusterTrees.getSecond().deleteSubTree();

                    return h;
                }
            }
        } else {
            if (verbose)
                System.err.println(depth + "Trees are already reduced");
        }

        if (k <= 0) // 1, if only interested in number or in finding only one network, 0 else
            return LARGE;

        int hBest = LARGE;
        List<Root> leaves1 = getAllAliveLeaves(root1);

        /*
        if (leaves1.size() <= 2) // try 2 rather than one...
        {
            totalResults.add(MergeNetworks.apply(root1,root2)); // todo: this needs to be fixed
            return 0;
        }
        */

        for (Root leaf2remove : leaves1) {
            BitSet taxa2remove = leaf2remove.getTaxa();
            if (taxa2remove.cardinality() != 1)
                throw new IOException(depth + "Leaf taxa cardinality: " + taxa2remove.cardinality());

            int hybridTaxon = taxa2remove.nextSetBit(0);

            if (candidateHybridsOriginal.get(hybridTaxon)) {
                if (verbose) {
                    System.err.println(depth + "Removing: " + hybridTaxon);
                    System.err.println(depth + "candidateHybrids: " + Basic.toString(candidateHybridsOriginal));
                    System.err.println(depth + "Tree1: " + root1.toStringFullTreeX());
                    System.err.println(depth + "Tree2: " + root2.toStringFullTreeX());
                }

                Root root1x = root1.copySubNetwork();
                Root root2x = root2.copySubNetwork();
                RemoveTaxon.apply(root1x, 1, hybridTaxon);
                RemoveTaxon.apply(root2x, 2, hybridTaxon);    // now we keep removed taxa as separate sets

                if (verbose) {
                    System.err.println(depth + "Tree1-x: " + root1x.toStringFullTreeX());
                    System.err.println(depth + "Tree2-x: " + root2x.toStringFullTreeX());
                }

                Refine.apply(root1x, root2x);

                if (verbose) {
                    System.err.println(depth + "Tree1-x-refined: " + root1x.toStringFullTreeX());
                    System.err.println(depth + "Tree2-x-refined: " + root2x.toStringFullTreeX());
                }

                Collection<Root> currentResults = new TreeSet<Root>(new NetworkComparator());
                candidateHybridsOriginal.set(hybridTaxon, false);

                int h = cacheComputeRec(root1x, root2x, false, candidateHybridsOriginal, k - 1, currentResults, depth + " >") + 1;
                candidateHybridsOriginal.set(hybridTaxon, true);

                if (h < k)
                    k = h;

                // System.err.println("Subproblem with " + Basic.toString(taxa2remove) + " removed, h=" + h);

                if (h < hBest && h <= k) {
                    hBest = h;
                    totalResults.clear();
                }
                if (h == hBest && h <= k) {
                    if (verbose) {
                        for (Root r : currentResults) {
                            System.err.println(depth + "Result: " + r.toStringNetworkFull());
                        }
                    }

                    // add the hybrid node:
                    currentResults = copyAll(currentResults);
                    AddHybridNode.apply(currentResults, hybridTaxon);
                    totalResults.addAll(fixOrdering(currentResults));
                }
                root1x.deleteSubTree();
                root2x.deleteSubTree();
            }
        }
        return hBest;
    }

    /**
     * get all alive leaves below the given root
     *
     * @param root
     * @return leaves
     */
    private List<Root> getAllAliveLeaves(Root root) {
        List<Root> leaves = new LinkedList<Root>();
        if (root.getTaxa().cardinality() > 0) {
            if (root.getOutDegree() == 0)
                leaves.add(root);
            else {
                Queue<Root> queue = new LinkedList<Root>();
                queue.add(root);
                while (queue.size() > 0) {
                    root = queue.poll();
                    for (Edge e = root.getFirstOutEdge(); e != null; e = root.getNextOutEdge(e)) {
                        Root w = (Root) e.getTarget();
                        if (w.getTaxa().cardinality() > 0) {
                            if (w.getOutDegree() == 0)
                                leaves.add(w);
                            else
                                queue.add(w);
                        }
                    }
                }
            }
        }
        return leaves;
    }

    /**
     * gets all alive taxa. Checks that both trees have the same set of alive taxa
     *
     * @param root1
     * @param root2
     * @return all alive taxa
     * @throws IOException
     */
    public BitSet getAllAliveTaxa(Root root1, Root root2) throws IOException {
        if (!root1.getTaxa().equals(root2.getTaxa()))
            throw new IOException("Trees have different sets of alive taxa: " + Basic.toString(root1.getTaxa()) + " vs "
                    + Basic.toString(root2.getTaxa()));
        return (BitSet) root1.getTaxa().clone();
    }

    /**
     * reorder the children in all networks
     *
     * @param networks
     */
    private Collection<Root> fixOrdering(Collection<Root> networks) {
        for (Root root : networks) {
            // if (verbose)
            //    System.err.println("Orig ordering: " + root.toStringNetworkFull());
            root.reorderNetwork();
            // if (verbose)
            //     System.err.println("New ordering: " + root.toStringNetworkFull());
        }
        return networks;
    }

    private static Collection<Root> copyAll(Collection<Root> list) {
        TreeSet<Root> copy = new TreeSet<Root>(new NetworkComparator());
        for (Root r : list) {
            copy.add(r.copySubNetwork());
        }
        return copy;
    }
}
