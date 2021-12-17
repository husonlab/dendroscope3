/*
 *   Apply2Component.java Copyright (C) 2020 Daniel H. Huson
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
package dendroscope.algorithms.gallnet;

import dendroscope.consensus.Cluster;
import dendroscope.util.IntegerVariable;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;
import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.StringUtils;
import jloda.util.Triplet;
import jloda.util.progress.ProgressListener;

import java.util.*;

/**
 * computes an optimal softwired network for a component of clusters that separate all taxa
 * Daniel Huson and Phillippe Gambette 10.2007 -2008
 */
public class Apply2Component {
    ProgressListener progressListener;
    BitSet bestChoice;

    private final boolean DEBUG = false; // when this is true, report lots of stuff

    /**
     * apply the algorithm to a single incompatibility compontent of separated taxa
     *
     * @param clusters
     * @param additionalEdges
     * @param reticulate
     * @return extended set of clusters whose Hasse network is a minimal  softwired network for the set of input clusters
     */
    public Cluster[] apply(ProgressListener progressListener, Cluster[] clusters, List<Triplet> additionalEdges,
                           BitSet additionalTaxa, BitSet reticulate, IntegerVariable additionalTaxonId) {
        this.progressListener = progressListener;
        progressListener.setCancelable(false);
        progressListener.setMaximum(-1);

        // determine all triples:

        List<Triple> triples = new LinkedList<>();

        for (int i = 0; i < clusters.length; i++) {
            for (int j = i + 1; j < clusters.length; j++) {
                Triple triple = Triple.computeIncompatibilityTriple(clusters[i], clusters[j]);
                if (triple != null) {
                    triples.add(triple);
                }
            }
        }
        if (triples.size() == 0) // everything is compatible, nothing to do
            return clusters;

        // sort triples so that ones with largest sets come first
        Triple[] array = triples.toArray(new Triple[triples.size()]);
        Arrays.sort(array, Triple.getComparator());
        triples = Arrays.asList(array);

        /*
        if (DEBUG) {
            System.err.println("Triples:" + triples.size());
            for (Triple triple : triples) {
                System.err.println(triple);
            }
        }
        */

        // find optimal set of reticulate taxa:

        progressListener.setTasks("Minimal reticulate set", "Searching for initial solution");

        bestChoice = null;

        findMinimalHittingSet(triples, clusters);

        // System.err.println("Reticulate nodes: " + bestChoice.cardinality());

        if (DEBUG) {
            System.err.println("Reticulate nodes: " + bestChoice);
            if (Cluster.containsAtLeastOne(bestChoice, clusters))
                System.err.println("R induces at least one cluster");
        }

        reticulate.or(bestChoice);

        {
            boolean reticulateOk = verifyReticulateTaxa(bestChoice, clusters);
            if (!reticulateOk)
                System.err.println("Verification of reticulation set failed on: " + StringUtils.toString(bestChoice));
        }

        // compute backbone tree:
        Map<Cluster, BitSet> backbone2clusters = new HashMap<>(); // maps each backbone cluster indices of original clusters that map to it
        PhyloTree backbone = computeBackbone(clusters, reticulate, backbone2clusters);
        NodeArray<BitSet> backboneNodes2Clusters = computeBackboneNodes2Clusters(backbone, backbone2clusters);

        if (DEBUG) {
            System.err.println("Backbone:");
            print(backbone);
        }
        // add optional reticulate taxa to the backbone tree:
        addReticulateToBackboneAndPushUp(backbone, clusters, backbone2clusters);
        if (DEBUG) {
            System.err.println("Extended backbone:");
            print(backbone);
        }

        Cluster[] maximalOptionalClusters = computeMaximalOptionalClusters(clusters, reticulate);
        if (DEBUG) {
            System.err.println("Maximal optional clusters:");
            Cluster.print(maximalOptionalClusters);
        }

        BitSet maximalReticulate = Cluster.extractTaxa(maximalOptionalClusters);

        // set of all nodes in backbone tree for which some reticulate chain ends
        NodeSet[] attachmentNodes = new NodeSet[maximalReticulate.length()];
        NodeSet[] usedAttachmentNodes = new NodeSet[maximalReticulate.length()];
        for (int t = maximalReticulate.nextSetBit(0); t != -1; t = maximalReticulate.nextSetBit(t + 1)) {
            attachmentNodes[t] = new NodeSet(backbone);
            usedAttachmentNodes[t] = new NodeSet(backbone);

            attachmentNodes[t].add(backbone.getRoot()); // always allow the root as one possibility
            computeAttachmentNodes(backbone, t, attachmentNodes[t]);
        }

        // process maximal clusters in B:

        Node[] maximalOptionalCluster2AttachmentNode = solveMinimalAttachmentProblem(backbone, attachmentNodes, usedAttachmentNodes, maximalOptionalClusters);
        // todo: comment out the next command to turn off additional taxa:
        pushMaximalClustersUp(maximalOptionalClusters, maximalOptionalCluster2AttachmentNode, additionalTaxa, additionalTaxonId);

        // System.err.println("Modified backbone:"); print(backbone);

        Set<Cluster> result = extractClusters(backbone);
        result.addAll(Arrays.asList(maximalOptionalClusters));

        // todo: comment out the next command to turn off additional edges:
        // determine which additional edges we need to ensure that every reticulate node has degree >=2
        determineAdditionalEdges(reticulate, backbone, clusters, backboneNodes2Clusters, additionalEdges);

        if (DEBUG) {
            System.err.println("Clusters after adding maximal optional to modified backbone:");
            Cluster.print(result.toArray(new Cluster[result.size()]));
        }

        return result.toArray(new Cluster[result.size()]);
    }

    /**
     * verifies that set of reticulate taxa    actually resolves all incompatibilities
     *
     * @param reticulate
     * @param clusters
     * @return true, if no problems found
     */
    private boolean verifyReticulateTaxa(BitSet reticulate, Cluster[] clusters) {
        boolean allOk = true;
        for (int i = 0; i < clusters.length; i++) {
            for (int j = i + 1; j < clusters.length; j++) {
                boolean ok = false;

                BitSet set = Cluster.setminus(clusters[i], clusters[j]);
                if (set.cardinality() == 0 || Cluster.contains(reticulate, set))
                    ok = true;
                if (!ok) {
                    set = Cluster.setminus(clusters[j], clusters[i]);
                    if (set.cardinality() == 0 || Cluster.contains(reticulate, set))
                        ok = true;
                }
                if (!ok) {
                    set = Cluster.intersection(clusters[j], clusters[i]);
                    if (set.cardinality() == 0 || Cluster.contains(reticulate, set))
                        ok = true;
                }
                if (!ok) {
                    System.err.println("WARNING: error in reticulate set, doesn't solve imcompatibility between "
                                       + StringUtils.toString(clusters[i]) + " and " + StringUtils.toString(clusters[j]));
                    allOk = false;
                }
            }
        }
        return allOk;
    }

    /**
     * computes the mapping from nodes in the backbone tree to original clusters
     *
     * @param backbone
     * @param backbone2clusters
     * @return mapping from nodes to bits indicating original clusters
     */
    private NodeArray<BitSet> computeBackboneNodes2Clusters(PhyloTree backbone, Map backbone2clusters) {
        NodeArray<BitSet> node2clusters = new NodeArray<>(backbone);
        for (Node v = backbone.getFirstNode(); v != null; v = v.getNext()) {
            if (v.getInfo() != null)
                node2clusters.put(v, (BitSet) backbone2clusters.get(v.getInfo()));
        }
        return node2clusters;

    }

    /**
     * determines the set of additional edges that are needed
     *
     * @param reticulateTaxa
     * @param backbone
     * @param clusters
     * @param backboneNodes2OriginalClusters
     * @param additionalEdges
     */
    private void determineAdditionalEdges(BitSet reticulateTaxa, PhyloTree backbone, Cluster[] clusters,
                                          NodeArray backboneNodes2OriginalClusters, List<Triplet> additionalEdges) {
        if (backbone.getRoot() == null)
            return; // should never happen
        // for each reticulate taxon, determine whether it needs an addtional edge to turn it off
        // this is true, if it does not apear in two different children of the root
        // in case, we descend to find that last node v such that all original clusters associated
        // with the node contain the taxon and it is not true that v has exactly one child with that property
        for (int t = reticulateTaxa.nextSetBit(0); t != -1; t = reticulateTaxa.nextSetBit(t + 1)) {
            Node root = backbone.getRoot();
            boolean found = false;

            // check that all clusters at the root node contain t, otherwise must attach above root
            boolean mustAttachRoot = false;
            BitSet oClusters = (BitSet) backboneNodes2OriginalClusters.get(root);
            if (oClusters != null) {
                for (int c = oClusters.nextSetBit(0); c != -1; c = oClusters.nextSetBit(c + 1)) {
                    // System.err.println("Original cluster: " + clusters[c]);
                    if (!clusters[c].get(t)) {
                        mustAttachRoot = true;   // not all clusters  contain t, don't descend
                        break;
                    }
                }
            }
            if (mustAttachRoot) {
                Cluster taxonCluster = new Cluster();
                taxonCluster.set(t);
                Cluster backboneCluster = new Cluster((BitSet) root.getInfo(), 0);
                Triplet<Cluster, Cluster, Boolean> triplet = new Triplet<>(backboneCluster, taxonCluster, Boolean.TRUE);
                additionalEdges.add(triplet);
                if (DEBUG) {
                    System.err.println("Additional edge(up): " + backboneCluster + " -> " + taxonCluster);
                }
            } else {
                boolean ok = false;
                for (Edge e = root.getFirstOutEdge(); !ok && e != null; e = root.getNextOutEdge(e)) {
                    Node u = e.getTarget();
                    Cluster backboneCluster = (Cluster) u.getInfo();
                    if (backboneCluster.get(t)) // t somwhere below u
                    {
                        if (!found)
                            found = true;
                        else
                            ok = true;
                    }
                }
                if (ok)
                    continue; // root has two different children for which t lies below, no additional edge necessary

                // do decend down to best attachment place
                Node v = root;
                while (true) { // until we break from the loop
                    Node nextV = null;   //  next child to move to
                    boolean okToDescend = true;  // is ok to go down
                    // look at all children:
                    for (Edge e = v.getFirstOutEdge(); okToDescend && e != null; e = v.getNextOutEdge(e)) {
                        Node u = e.getTarget();
                        Cluster backboneCluster = (Cluster) u.getInfo();

                        if (backboneCluster.get(t)) {
                            if (nextV == null) {
                                nextV = u;

                                oClusters = (BitSet) backboneNodes2OriginalClusters.get(u);

                                for (int c = oClusters.nextSetBit(0); c != -1; c = oClusters.nextSetBit(c + 1)) {

                                    if (!clusters[c].get(t))
                                        okToDescend = false;   // not all clusters  contain t, don't descend
                                }
                            } else
                                okToDescend = false; // v has more than one child with t below,don't descend
                        }
                    }
                    if (nextV != null && okToDescend) {
                        v = nextV;
                    } else {
                        Cluster taxonCluster = new Cluster();
                        taxonCluster.set(t);
                        Cluster backboneCluster = new Cluster((BitSet) v.getInfo(), 0);
                        Triplet triplet = new Triplet(backboneCluster, taxonCluster, Boolean.FALSE);
                        if (DEBUG)
                            System.err.println("Additional edge: " + backboneCluster + " -> " + taxonCluster);
                        additionalEdges.add(triplet);
                        break;
                    }
                }
            }
        }
    }


    /**
     * determine attachment position for maximal optional clusters
     *
     * @param backbone
     * @param attachmentNodes
     * @param usedAttachmentNodes
     * @param maximalOptionalClusters
     * @return the best assignment
     */
    private Node[] solveMinimalAttachmentProblem(PhyloTree backbone, NodeSet[] attachmentNodes, NodeSet[] usedAttachmentNodes, Cluster[] maximalOptionalClusters) {

        progressListener.setTasks("Minimal attachment problem", "Searching for initial solution");
        progressListener.setMaximum(-1);

        Node[] currentAssignment = new Node[maximalOptionalClusters.length];
        int currentScore = 0;
        Node[] bestAssignment = new Node[maximalOptionalClusters.length];
        IntegerVariable bestScore = new IntegerVariable(Integer.MAX_VALUE);
        try {
            progressListener.setProgress(-1);
            computeAttachmentForMaximalOptionalClustersRec(backbone, attachmentNodes, usedAttachmentNodes, maximalOptionalClusters, 0, currentAssignment, currentScore, bestAssignment, bestScore);
        } catch (CanceledException e) {
            System.err.println("Aborted search for minimal attachment solution");
            progressListener.setUserCancelled(false);
        }
        progressListener.setCancelable(false);

        //    System.err.print("Best loss score: " + bestScore + " best assignments: ");
        //    for (int i = 0; i < bestAssignment.length; i++)
        //        System.err.print(" " + bestAssignment[i]);
        //    System.err.println();

        return bestAssignment;
    }

    /**
     * use branch and bound to compute optimal attachment nodes for all maximal optional clusters
     *
     * @param topTree
     * @param attachmentNodes
     * @param usedAttachmentNodes
     * @param maximalOptionalClusters
     * @param i
     * @param currentAssignment
     * @param currentScore
     * @param bestAssignment
     */
    private void computeAttachmentForMaximalOptionalClustersRec(PhyloTree topTree, NodeSet[] attachmentNodes, NodeSet[] usedAttachmentNodes,
                                                                Cluster[] maximalOptionalClusters, int i, Node[] currentAssignment,
                                                                int currentScore, Node[] bestAssignment, IntegerVariable bestScore) throws CanceledException {
        if (i < maximalOptionalClusters.length) {
            Cluster cluster = maximalOptionalClusters[i];
            NodeSet union = new NodeSet(topTree);
            for (int t = cluster.nextSetBit(0); t >= 0; t = cluster.nextSetBit(t + 1))
                union.addAll(attachmentNodes[t]);

            for (Node v = union.getFirstElement(); v != null; v = union.getNextElement(v)) {
                BitSet backboneCluster = (Cluster) v.getInfo();

                if (backboneCluster != null && backboneCluster.intersects(cluster)) {
                    Map<Integer, Node> id2UsedAttachmentNode = new HashMap<>();
                    int loss = computeLoss(topTree.getRoot(), v, cluster, attachmentNodes, usedAttachmentNodes, id2UsedAttachmentNode);
                    if (currentScore + loss < bestScore.getValue()) {
                        currentAssignment[i] = v;
                        for (Integer id : id2UsedAttachmentNode.keySet()) {
                            Node w = id2UsedAttachmentNode.get(id);
                            usedAttachmentNodes[id].add(w);
                        }
                        computeAttachmentForMaximalOptionalClustersRec(topTree, attachmentNodes, usedAttachmentNodes, maximalOptionalClusters, i + 1, currentAssignment, currentScore + loss, bestAssignment, bestScore);
                        currentAssignment[i] = null;
                        for (Integer id : id2UsedAttachmentNode.keySet()) {
                            Node w = id2UsedAttachmentNode.get(id);
                            usedAttachmentNodes[id].remove(w);
                        }
                    }
                }
            }
            progressListener.checkForCancel();

        } else if (currentScore < bestScore.getValue()) {
            bestScore.setValue(currentScore);

            System.arraycopy(currentAssignment, 0, bestAssignment, 0, currentAssignment.length);

            progressListener.setCancelable(true);
            progressListener.setTasks("Minimal attachment problem", "Current best score: " + bestScore.getValue());
        }
    }

    /**
     * computes the loss associated with attaching the given optional cluster to the node v in the tree
     *
     * @param v
     * @param taxId2attachmentNode maps taxid to node used
     * @return loss
     */
    private int computeLoss(Node root, Node v, Cluster cluster, NodeSet[] attachmentNodes, NodeSet[] usedAttachmentNodes, Map<Integer, Node> taxId2attachmentNode) {
        int count = 0;
        while (v != null) {
            for (int i = cluster.nextSetBit(0); i != -1; i = cluster.nextSetBit(i + 1)) {   // go up to the first attachment node for i
                if (attachmentNodes[i].contains(v) && v != root) {
                    if (!usedAttachmentNodes[i].contains(v)) {
                        taxId2attachmentNode.put(i, v);
                        count++;
                    }
                }
            }
            if (v.getInDegree() > 0)
                v = v.getFirstInEdge().getSource();
            else
                v = null;
        }

        return cluster.cardinality() - count;
    }

    /**
     * push up maximal optional cluster.  Also push up new taxon Id to make sure that cluster only gets one connection
     *
     * @param maximalOptionalClusters
     * @param maximalOptionalCluster2AttachmentNode
     */
    private void pushMaximalClustersUp(Cluster[] maximalOptionalClusters, Node[] maximalOptionalCluster2AttachmentNode,
                                       BitSet additionalTaxa, IntegerVariable additionaTaxonId) {
        for (int i = 0; i < maximalOptionalClusters.length; i++) {
            Node v = maximalOptionalCluster2AttachmentNode[i];
            Cluster cluster = maximalOptionalClusters[i];
            int newTaxonId = (additionaTaxonId.increment());
            cluster.set(newTaxonId);
            additionalTaxa.set(newTaxonId);

            while (v != null) {
                BitSet backboneCluster = (Cluster) v.getInfo();
                backboneCluster.or(cluster);
                if (v.getInDegree() > 0)
                    v = v.getFirstInEdge().getSource();
                else
                    v = null;
            }
        }
    }

    /**
     * greedily find a minimal reticulate set
     *
     * @param tripleClusters
     * @param current
     * @param level
     * @param hide
     * @return
     */
    private BitSet greedyMinimalHittingSet(BitSet[] tripleClusters, BitSet current, int level, boolean hide) {
        progressListener.setCancelable(false);
        while (level < tripleClusters.length / 3) {
            if ((!hide) && (level % 100 == 0))
                progressListener.setTasks("Searching for minimum reticulate set - greedily", level + "/" + (tripleClusters.length / 3) + " incompatibilities treated");

            boolean solvesIncompatibility = false;
            int j = 0;
            int bestSize = current.size() + tripleClusters[3 * level].size() + 1; //Lowest number of reticulate nodes to add so far
            int bestJ = 0; //part of the triple where this local optimum has been found
            while (!solvesIncompatibility && (j < 3)) {
                BitSet intersection = (BitSet) current.clone();
                BitSet union = (BitSet) current.clone();
                intersection.and(tripleClusters[3 * level + j]);
                union.or(tripleClusters[3 * level + j]);
                //System.err.println("current incompatibility triple: "+tripleClusters[3*level+j]+" - bestsize found so far: "+bestSize+" - size with this choice: "+union.cardinality());
                if (tripleClusters[3 * level + j].equals(intersection)) {
                    solvesIncompatibility = true;
                } else {
                    if (union.cardinality() < bestSize) {
                        bestJ = j;
                        bestSize = union.cardinality();
                    }
                }
                j++;
                intersection.clear();
                union.clear();
            }
            if (!solvesIncompatibility) {
                BitSet union = (BitSet) current.clone();
                union.or(tripleClusters[3 * level + bestJ]);
                //System.err.println("Greedily chose "+union);
                current = union;
                level++;
            } else {
                //System.err.println("Greedily kept the current one");
                level++;
            }
        }
        return current;
    }


    /**
     * use an FPT algorithm to find sets of reticulate taxa of minimum size
     *
     * @param triples
     */
    private void findMinimalHittingSet(Collection<Triple> triples, Cluster[] clusters) {
        SortedSet<Pair<BitSet, Integer>> candidates = new TreeSet<>(new Comparator<Pair<BitSet, Integer>>() {
            public int compare(Pair<BitSet, Integer> p1, Pair<BitSet, Integer> p2) {
                BitSet s1 = p1.getFirst();
                BitSet s2 = p2.getFirst();
                if (s1.cardinality() < s2.cardinality())
                    return -1;
                else if (s1.cardinality() > s2.cardinality())
                    return 1;
                else if (p1.getSecond() > p2.getSecond())
                    return -1;
                else if (p1.getSecond() < p2.getSecond())
                    return 1;
                else if (s1.equals(s2)) {
                    return 0;
                } else {
                    int b1 = s1.nextSetBit(0);
                    int b2 = s2.nextSetBit(0);
                    while (b1 == b2) {
                        b1 = s1.nextSetBit(b1 + 1);
                        b2 = s2.nextSetBit(b2 + 1);
                    }
                    if (b1 < b2)
                        return 1;
                    else
                        return -1;
                }
            }
        });//Candidates found so far

        //Those SortedSets contain candidate sets of reticulate taxa
        //sorted by increasing size, then decreasing "level"
        // (i.e. nb of incompatibilities they solve).

        if (DEBUG)
            System.err.println("Triples:");
        BitSet[] tripleClusters = new BitSet[3 * triples.size()];
        int maxLevel = triples.size();
        int solutionSize = maxLevel;
        int nbtriple = 0;
        for (Triple currentTriple : triples) {
            if (DEBUG)
                System.err.println(currentTriple);
            tripleClusters[3 * nbtriple] = currentTriple.getA();
            tripleClusters[3 * nbtriple + 1] = currentTriple.getB();
            tripleClusters[3 * nbtriple + 2] = currentTriple.getC();
            nbtriple++;
        }

        SortedSet<BitSet> solutions = new TreeSet<>(new Comparator<BitSet>() {
            public int compare(BitSet set1, BitSet set2) {
                if (set1.cardinality() < set2.cardinality())
                    return -1;
                else if (set1.cardinality() > set2.cardinality())
                    return 1;
                else {
                    int i1 = set1.nextSetBit(0);
                    int i2 = set2.nextSetBit(0);
                    while (i1 != -1) {
                        if (i1 < i2)
                            return -1;
                        else if (i1 > i2)
                            return 1;
                        i1 = set1.nextSetBit(i1 + 1);
                        i2 = set2.nextSetBit(i2 + 1);
                    }
                    return 0;
                }
            }
        });
        candidates.add(new Pair<>(new BitSet(), 0));

        BitSet greedySolution = greedyMinimalHittingSet(tripleClusters, new BitSet(), 0, false);
        try {
            progressListener.setCancelable(true);
            progressListener.setMaximum(greedySolution.cardinality());
            progressListener.setProgress(0);
            progressListener.setTasks("Searching for minimum reticulate set", "(step 0/" + greedySolution.cardinality() + ")");
        } catch (CanceledException e) {

        }
        int currentMaxLevel;
        int previousMinSize;
        int minSize = 0;
        while (candidates.size() > 0) {
            //System.err.println("Nb of candidates: "+candidates.size());
            //if minCandidates is empty, transfer candidates of smallest size from candidates
            var currentPair = candidates.first();
            candidates.remove(currentPair);
			BitSet maxLevelCluster = (BitSet) (currentPair.getFirst()).clone();
            previousMinSize = minSize;
			minSize = maxLevelCluster.cardinality();
			currentMaxLevel = currentPair.getSecond();
            try {
                if (previousMinSize != minSize) {
                    progressListener.setTasks("Searching for minimum reticulate set", "(step " + minSize + "/" + greedySolution.cardinality() + ")");
                    progressListener.setProgress(minSize);
                }
                if (DEBUG)
                    System.err.println("Current candidate " + maxLevelCluster + " (level" + currentMaxLevel + "), size:"
                            + maxLevelCluster.cardinality() + " (" + candidates.size() + "candidate sets:" + candidates + ")");

                progressListener.checkForCancel();
                if ((maxLevelCluster.cardinality()) > solutionSize) {
                    //System.err.println("Those candidates are useless:");
                    while (!candidates.isEmpty()) {
                        if (DEBUG)
							System.err.println("- " + (candidates.first()).getFirst());
                        candidates.remove(candidates.first());
                    }
                } else {
                    //Does this set solve the next incompatibility?
                    boolean solvesIncompatibility = false;
                    for (int j = 0; !solvesIncompatibility && j < 3; j++) {
                        BitSet intersection = (BitSet) maxLevelCluster.clone();
                        intersection.and(tripleClusters[3 * currentMaxLevel + j]);
                        if (tripleClusters[3 * currentMaxLevel + j].equals(intersection)) {
                            solvesIncompatibility = true;
                        }
                    }
                    if (solvesIncompatibility) {
                        if (DEBUG)
                            System.err.println(maxLevelCluster + " solves incompatibility " + tripleClusters[3 * currentMaxLevel]
                                    + "|" + tripleClusters[3 * currentMaxLevel + 1] + "|" + tripleClusters[3 * currentMaxLevel + 2]);
                        if (currentMaxLevel + 1 == maxLevel) {
                            if (DEBUG)
                                System.err.println("Added to list of possible solutions");
                            solutions.add(maxLevelCluster);
                            if (maxLevelCluster.cardinality() < solutionSize) {
                                solutionSize = maxLevelCluster.cardinality();
                            }
                        } else {
                            candidates.add(new Pair<>(maxLevelCluster, currentMaxLevel + 1));
                        }
                    } else {
                        if (DEBUG)
                            System.err.println(maxLevelCluster + " doesn't solve incompatibility " + tripleClusters[3 * currentMaxLevel]
                                    + "|" + tripleClusters[3 * currentMaxLevel + 1] + "|" + tripleClusters[3 * currentMaxLevel + 2]);

                        for (int j = 0; j < 3; j++) {
                            BitSet newCandidate = (BitSet) maxLevelCluster.clone();
                            newCandidate.or(tripleClusters[3 * currentMaxLevel + j]);
                            if (currentMaxLevel + 1 == maxLevel) {
                                if (DEBUG)
                                    System.err.println("Add a new candidate: " + newCandidate + " of level " + (currentMaxLevel + 1));
                                solutions.add(newCandidate);
                                if (newCandidate.cardinality() < solutionSize) {
                                    solutionSize = newCandidate.cardinality();
                                }
                            } else {
                                candidates.add(new Pair<>(newCandidate, currentMaxLevel + 1));
                                if (DEBUG)
                                    System.err.println("New candidate: " + newCandidate + " of level " + (currentMaxLevel + 1));
                            }
                            if (DEBUG)
                                System.err.println(newCandidate + ":" + (currentMaxLevel + 1));
                        }
                    }
                }
            } catch (CanceledException e) {
                System.err.println("Aborted findMinimalHittingSet");
                candidates.clear();
                progressListener.setUserCancelled(false);
                progressListener.setCancelable(false);
                progressListener.setMaximum(-1);
                progressListener.setUserCancelled(false);
                progressListener.setCancelable(false);
                if (solutions.isEmpty()) {
                    System.err.println("Used greedy improvement of the solution found so far");
                    bestChoice = greedyMinimalHittingSet(tripleClusters, maxLevelCluster, currentMaxLevel, false);
                    solutions.add(bestChoice);
                }
            }
        }
        // System.err.println("Solutions for sets of reticulate nodes: "+solutions);
        Iterator solutionIt = solutions.iterator();
        bestChoice = (BitSet) solutionIt.next();

        if (DEBUG)
            System.err.println("Best solution: " + StringUtils.toString(bestChoice));

        while (solutionIt.hasNext()) {
            boolean keep = false;
            BitSet reticulateTaxa = (BitSet) solutionIt.next();
            //System.err.println("Consider solution "+reticulateTaxa);
            if (!keep && reticulateTaxa.cardinality() == bestChoice.cardinality()) {
                boolean bestIsComplementToAllClusters = !Cluster.intersectsAll(bestChoice, clusters);
                boolean currentIsComplementToAllClusters = !Cluster.intersectsAll(reticulateTaxa, clusters);

                if (bestIsComplementToAllClusters && !currentIsComplementToAllClusters)
                    keep = true;

                if (!keep && !bestIsComplementToAllClusters && !currentIsComplementToAllClusters) {
                    int bestNotContained = Cluster.numberOfTaxaNotContainedInAllClusters(bestChoice, clusters);
                    int currentNotContained = Cluster.numberOfTaxaNotContainedInAllClusters(reticulateTaxa, clusters);
                    if (currentNotContained > bestNotContained)
                        keep = true;
                }
            }
            if (keep) {
                bestChoice = reticulateTaxa;
                //System.err.println(reticulateTaxa+" better than previous solution!");
            } else {
                //System.err.println(reticulateTaxa+" not better than previous solution!");
            }
        }

    }


    /**
     * compute the set of maximal optional clusters
     *
     * @param clusters
     * @param reticulate
     * @return all maximal fully optional clusters
     */
    private Cluster[] computeMaximalOptionalClusters(Cluster[] clusters, BitSet reticulate) {
        Set<Cluster> list = new HashSet<>();

        for (Cluster cluster : clusters) {
            if (Cluster.contains(reticulate, cluster)) {
                list.add(cluster);
            }
        }

        Cluster[] optional = list.toArray(new Cluster[list.size()]);
        BitSet isContained = new BitSet();
        for (int i = 0; i < optional.length; i++) {
            for (int j = 0; j < optional.length; j++)
                if (i != j) {
                    if (Cluster.contains(optional[i], optional[j]))
                        isContained.set(j);
                }
        }
        Cluster[] result = new Cluster[optional.length - isContained.cardinality()];
        int count = 0;
        for (int i = 0; i < optional.length; i++)
            if (!isContained.get(i))
                result[count++] = (Cluster) optional[i].clone();
        return result;
    }

    /**
     * computes the backbone Hasse diagram
     *
     * @param clusters
     * @param reticulate
     * @param backbone2clusters this gets set to the backbone to clusters map
     * @return backbone Hasse diagram
     */
    private PhyloTree computeBackbone(Cluster[] clusters, BitSet reticulate, Map<Cluster, BitSet> backbone2clusters) {
        for (int i = 0; i < clusters.length; i++) {
            Cluster fixed = (Cluster) clusters[i].clone();
            fixed.andNot(reticulate);
            if (fixed.cardinality() > 0) {
                BitSet preimage = backbone2clusters.get(fixed);
                if (preimage == null) {
                    preimage = new BitSet();
                    backbone2clusters.put(fixed, preimage);
                }
                preimage.set(i);
            }
        }
        Cluster[] backbone = backbone2clusters.keySet().toArray(new Cluster[backbone2clusters.keySet().size()]);
        return constructHasse(backbone);
    }

    /**
     * construct the Hasse diagram for a set of clusters
     *
     * @param clusters0
     */
    public PhyloTree constructHasse(Cluster[] clusters0) {
        BitSet taxa = Cluster.extractTaxa(clusters0);
        Cluster[] clusters = Cluster.getClustersSortedByDecreasingCardinality(clusters0);
        boolean rootClusterIsPresent = (clusters.length > 0 && clusters[0].equals(taxa));
        PhyloTree tree = new PhyloTree();
        Node root = tree.newNode();
        tree.setRoot(root);
        if (!rootClusterIsPresent) {
            tree.setLabel(root, "[" + Cluster.extractTaxa(clusters) + "]");
            tree.setInfo(root, new Cluster());
        }

        int[] cardinality = new int[clusters.length];
        Node[] nodes = new Node[clusters.length];

        for (int i = 0; i < clusters.length; i++) {
            cardinality[i] = clusters[i].cardinality();
            if (i == 0 && rootClusterIsPresent)
                nodes[i] = root;
            else
                nodes[i] = tree.newNode();
            tree.setLabel(nodes[i], "" + clusters[i]);
            tree.setInfo(nodes[i], clusters[i]);
        }

        for (int i = 0; i < clusters.length; i++) {
            BitSet cluster = clusters[i];

            if (nodes[i].getInDegree() == 0 && (i > 0 || !rootClusterIsPresent)) {
                tree.newEdge(root, nodes[i]);
            }

            BitSet covered = new BitSet();

            for (int j = i + 1; j < clusters.length; j++) {
                if (cardinality[j] < cardinality[i]) {
                    BitSet subCluster = clusters[j];
                    if (Cluster.contains(cluster, subCluster) && !Cluster.contains(covered, subCluster)) {
                        tree.newEdge(nodes[i], nodes[j]);
                        covered.or(subCluster);
                        // if (covered.cardinality() == cardinality[i]) break;
                    }
                }
            }
        }
        return tree;
    }

    /**
     * add elements of all clusters to the backbone clusters that represent them, pushing up all elements, too
     *
     * @param backbone
     * @param backbone2clusters maps each cluster in the backbone tree or the bitset of original clusters
     */
    private void addReticulateToBackboneAndPushUp(PhyloTree backbone, Cluster[] clusters, Map backbone2clusters) {
        for (Node v = backbone.getFirstNode(); v != null; v = v.getNext()) {
            BitSet backboneCluster = (Cluster) v.getInfo();
            //System.err.println("Backbone cluster: "+backboneCluster);
            if (backboneCluster != null) {
                BitSet preimage = (BitSet) backbone2clusters.get(backboneCluster);
                //System.err.println("--> Preimage: "+preimage);

                if (preimage != null) {
                    for (int i = preimage.nextSetBit(0); i != -1; i = preimage.nextSetBit(i + 1))
                        backboneCluster.or(clusters[i]);
                }
            }
        }
        if (backbone.getRoot() != null)
            pushupRec(backbone.getRoot());
    }

    /**
     * extract all clusters from the backbone
     *
     * @param backbone
     * @return clusters
     */
    private Set<Cluster> extractClusters(PhyloTree backbone) {
        Set<Cluster> clusters = new HashSet<>();

        Stack<Node> stack = new Stack<>();
        stack.push(backbone.getRoot());
        while (!stack.isEmpty()) {
            Node v = stack.pop();
            Cluster cluster = (Cluster) v.getInfo();
            clusters.add(cluster);
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                stack.push(e.getTarget());
            }
        }

        return clusters;
    }

    /**
     * recursively push up all taxa so that we have that any parent contains all the taxa of its children
     *
     * @param v
     */
    private void pushupRec(Node v) {
        BitSet vSet = (BitSet) v.getInfo();
        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            Node w = e.getTarget();
            pushupRec(w);
            BitSet wSet = (BitSet) w.getInfo();
            vSet.or(wSet);
        }
    }


    /**
     * determines all minimal attachment nodes in backbone for taxon t
     *
     * @param backbone
     * @param t
     * @return minimal attachment nodes
     */
    private void computeAttachmentNodes(PhyloTree backbone, int t, NodeSet attachmentNodes) {
        Stack<Node> stack = new Stack<>();
        stack.push(backbone.getRoot());

        while (!stack.isEmpty()) {
            Node v = stack.pop();

            boolean foundBelow = false;
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node w = e.getTarget();
                BitSet wTaxa = (BitSet) backbone.getInfo(w);
                if (wTaxa.get(t)) {
                    stack.push(w);
                    foundBelow = true;
                }
            }
            if (!foundBelow) {
                attachmentNodes.add(v);
//                System.err.println("Attachment node for t=" + t + ": " + v.getId());
            }
        }
    }

    /**
     * print the backbone network:
     *
     * @param backbone
     */
    private void print(PhyloTree backbone) {
        Node root = backbone.getRoot();
        List<Node> list = new LinkedList<>();
        Set<Node> seen = new HashSet<>();
        list.add(root);
        seen.add(root);
        while (!list.isEmpty()) {
            Node v = list.remove(0);
            System.err.print("Node: " + v.getId());
            if (v.getOutDegree() > 0)
                System.err.print(" adj:");
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node w = e.getTarget();
                if (!seen.contains(w)) {
                    System.err.print(" " + w.getId());
                    list.add(w);
                    seen.add(w);
                }
            }
            System.err.println(" cluster: " + backbone.getInfo(v) + "");
        }
    }
}
