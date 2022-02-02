/*
 * ComputeGalledNetwork.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.algorithms.gallnet;

import dendroscope.algorithms.clusternet.ClusterNetwork;
import dendroscope.algorithms.utils.Compact;
import dendroscope.consensus.Cluster;
import dendroscope.consensus.Split;
import dendroscope.consensus.SplitSystem;
import dendroscope.consensus.Taxa;
import dendroscope.util.IntegerVariable;
import jloda.graph.Node;
import jloda.graph.NodeDoubleArray;
import jloda.phylo.PhyloTree;
import jloda.swing.util.Alert;
import jloda.swing.util.ProgressDialog;
import jloda.util.StringUtils;
import jloda.util.progress.ProgressListener;

import java.util.*;

/**
 * computes an optimal network from a collection of clusters
 * Daniel Huson, 10.2007
 */
public class ComputeGalledNetwork {
    private final Taxa taxa;
    private final SplitSystem splits;
    private PhyloTree tree;

    private final boolean DEBUG = false; // when this is true, report lots of stuff

    /**
     * constructor
     *
	 */
    public ComputeGalledNetwork(Taxa taxa, SplitSystem splits) {
        this.taxa = taxa;
        this.splits = splits;
        tree = new PhyloTree();
    }

    /**
     * apply the optimal galled network algorithm. Treats the last named taxon as an outgroup
     *
     * @return optimal network from splits
     */
    public PhyloTree apply(ProgressListener progressListener) {
        long startTime = new Date().getTime();

        progressListener.setCancelable(false);
        progressListener.setTasks("Computing optimal galled network", "initalization");
        progressListener.setMaximum(-1);

        if (progressListener instanceof ProgressDialog) {
            ((ProgressDialog) progressListener).setCloseOnCancel(false);
        }

        System.err.println("Constructing optimal galled  network (taxa=" + taxa.size() + ", splits=" + splits.size() + "):");

        tree = new PhyloTree();
        if (!splits.isFullSplitSystem(taxa)) {
            System.err.println("Warning: not full split system");
            return tree;
        }

        if (DEBUG) {
            System.err.println("Taxa:");
            for (int t = 1; t <= taxa.size(); t++) {
                System.err.println("t=" + t + ": " + taxa.getLabel(t));
            }
        }

        Node root = tree.newNode();
        tree.setRoot(root);

        //   determine the clusters
        List list = new LinkedList();
        int outGroupTaxonId = taxa.maxId();
        for (int i = 1; i <= splits.size(); i++) {
            Split split = splits.getSplit(i);
            BitSet A = split.getPartNotContainingTaxon(outGroupTaxonId);
            if (A.cardinality() < taxa.size() - 1)
                list.add(new Cluster(A, split.getWeight()));
        }
		Cluster[] clusters = (Cluster[]) list.toArray(new Cluster[0]);

        System.err.println("Number of clusters: " + clusters.length);

        List additionalEdges = new LinkedList();// additional edges need to convert soft-wired into semi-softwired

        IntegerVariable additionalTaxonId = new IntegerVariable(taxa.size() + 1);

        if (progressListener instanceof ProgressDialog) {
            ((ProgressDialog) progressListener).setCancelButtonText("Skip");
        }

        Cluster[] result = applyToPartition(progressListener, clusters, additionalEdges, taxa.maxId(), additionalTaxonId);

        if (progressListener instanceof ProgressDialog) {
            ((ProgressDialog) progressListener).resetCancelButtonText();
        }

        NodeDoubleArray node2weight = new NodeDoubleArray(tree);
        NodeDoubleArray node2confidence = new NodeDoubleArray(tree);

        if (DEBUG)
            System.err.println("Constructing Hasse Diagram");
        ClusterNetwork.constructHasse(taxa, tree, root, result, node2weight, node2confidence, additionalEdges, taxa.maxId());

        if (DEBUG)
            System.err.println("Converting Hasse Diagram to Cluster Network...");
        ClusterNetwork.convertHasseToClusterNetwork(tree, node2weight);
        /*
        for(Edge e=tree.getFirstEdge();e!=null;e=tree.getNextEdge(e))
        {
            if(e.getTarget().getInDegree()>1)
                tree.setReticulated(e,true);
         }
                */

        {
            int numberOfSpecialNodes = 0;
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext())
                if (v.getInDegree() > 1)
                    numberOfSpecialNodes++;
            System.err.println("Number of reticulations: " + numberOfSpecialNodes);
        }

        GalledNetworkChecker checker = new GalledNetworkChecker(tree);
        System.err.println("Verifying that computed network is a galled network:");
        int problems = checker.isGalledNetwork();
        System.err.println("Verified: " + (problems == 0 ? "ok" : problems + " problems"));
        if (problems == 0) {
            System.err.println("Verifying that galled network represents all splits:");
            int count = checker.containsAll(splits, taxa);
            System.err.println("Verified: " + count + " of " + splits.size());
            if (count < splits.size())
                new Alert(null, "Internal error: computed network is not galled network or does not represent all clusters in the input");
        } else {
            new Alert(null, "Internal error: computed network is not a galled network");
        }

        long seconds = (new Date().getTime() - startTime);
        System.err.println("Algorithm required " + seconds / 1000.0 + " seconds");
        //System.err.println("Algorithm required " + (seconds / 60000) + " minutes");

        return tree;
    }

    /**
     * compute one part of partitioning below root
     *
     * @return extended clusters below root
     */
    private Cluster[] applyToPartition(ProgressListener progressListener, Cluster[] clusters, List additionalEdges, int maxTaxonId,
                                       IntegerVariable additionaTaxonId) {
        progressListener.setTasks("Computing optimal galled network", "initialization");
        progressListener.setMaximum(-1);

        // compute incompatibility graph:
        boolean[][] incompatible = new boolean[clusters.length][clusters.length];
        for (int i = 0; i < clusters.length; i++) {
            for (int j = i + 1; j < clusters.length; j++) {
                incompatible[i][j] = incompatible[j][i] = Cluster.incompatible(clusters[i], clusters[j]);
            }
        }

        int totalReticulate = 0; // total number of reticulate nodes

        BitSet[] components = computeComponents(incompatible);
        int numberOfNonTrivialComponents = 0;
        for (BitSet component : components)
            if (component.cardinality() > 1)
                numberOfNonTrivialComponents++;

        BitSet[] allTaxaInComponent = new BitSet[components.length];

        if (DEBUG)
            System.err.println("Non-trivial incompatibility components: " + numberOfNonTrivialComponents);
        /*
        for (int c = 0; c < components.length; c++)
            System.err.println(components[c]);
        */

        // System.err.println("Clusters before running components:"); Cluster.print(clusters);

        Set result = new HashSet();

        BitSet[] additionalTaxa = new BitSet[components.length];
        // For each component, a list of pairs, each consisting of a compact cluster and a set of additional taxa
        // that needs to be added to each occurrence of the uncompacted cluster in all other components

        int nontrivialComponentNumber = 0;
        // process each non-trivial incompatibility component
        for (int n = 0; n < components.length; n++) {
            BitSet comp = components[n];
            additionalTaxa[n] = new BitSet();
            if (comp.cardinality() == 1)   // trivial component
            {
                int c = comp.nextSetBit(0);
                result.add(clusters[c]);
                // System.err.println("Trivial component: " + clusters[c]);
                Cluster[] clustersInComponent = new Cluster[comp.cardinality()];
                clustersInComponent[0] = clusters[c];
                allTaxaInComponent[n] = Cluster.extractTaxa(clustersInComponent);
            } else {
                nontrivialComponentNumber++;
                if (DEBUG)
                    System.err.println("== Processing non-trivial component " + nontrivialComponentNumber + ":");

                Cluster[] clustersInComponent = new Cluster[comp.cardinality()];
                int pos = 0;
                for (int c = comp.nextSetBit(0); c >= 0; c = comp.nextSetBit(c + 1)) {
                    clustersInComponent[pos++] = clusters[c];
                }
                allTaxaInComponent[n] = Cluster.extractTaxa(clustersInComponent);

                if (DEBUG) {
                    BitSet componentTaxa = allTaxaInComponent[n];
                    System.err.println("Taxa in component: " + componentTaxa.cardinality());
                    for (int t = componentTaxa.nextSetBit(0); t != -1; t = componentTaxa.nextSetBit(t + 1))
                        System.err.print(" " + taxa.getLabel(t) + ",");
                    System.err.println();
                }

                // identify node separated taxa and produce the backward mapping from single taxa to multiple taxa:
                Map mapBack = Compact.compactClusters(clustersInComponent);

                if (DEBUG) {
                    System.err.println("Compacted clusters:");
                    for (Cluster cluster : clustersInComponent) {
                        System.err.println(cluster.toString());
                    }
                }

                BitSet componentTaxa = Cluster.extractTaxa(clustersInComponent);
                if (DEBUG) {
                    System.err.println("Taxa in compacted component: " + componentTaxa.cardinality());
                    for (int t = componentTaxa.nextSetBit(0); t != -1; t = componentTaxa.nextSetBit(t + 1)) {
                        System.err.print(" " + taxa.getLabel(t));
                        BitSet other = (BitSet) mapBack.get(t);
                        if (other.cardinality() == 1)
                            System.err.print(",");
                        else {
                            boolean first = true;
                            for (int o = other.nextSetBit(0); o != -1; o = other.nextSetBit(o + 1)) {
                                if (first) {
                                    System.err.print("(");
                                    first = false;
                                } else
                                    System.err.print(", ");
                                System.err.print(taxa.getLabel(o));
                            }
                            System.err.print("),");
                        }
                    }
                    System.err.println();
                }

                int oldSize = clustersInComponent.length;
                List newAdditionalEdges = new LinkedList();
                BitSet reticulations = new BitSet();
                clustersInComponent = (new Apply2Component()).apply(progressListener, clustersInComponent, newAdditionalEdges, additionalTaxa[n], reticulations, additionaTaxonId);
                if (DEBUG) {
                    System.err.println("Reduced component number " + n + ": " + oldSize + " -> " + clustersInComponent.length);
                    System.err.println("Reticulations in component: " + reticulations.cardinality());
                }

                totalReticulate += reticulations.cardinality();  // count compacted reticulations only

                Compact.uncompactReticulations(reticulations, mapBack, taxa, maxTaxonId);

                Compact.uncompactClusters(clustersInComponent, mapBack, maxTaxonId);
                Compact.uncompactTriplets(newAdditionalEdges, mapBack, maxTaxonId);
                additionalEdges.addAll(newAdditionalEdges);
                result.addAll(Arrays.asList(clustersInComponent));
            }
            result.add(new Cluster(Cluster.union(allTaxaInComponent[n], additionalTaxa[n])));
        }

		Cluster[] resultClusters = (Cluster[]) result.toArray(new Cluster[0]);

        // System.err.println("Clusters before adding markers:");  Cluster.print(clusters);

        // add all additional marker taxa to all clusters not contained in the same component in which
        // the marker cluster originated
        for (int n = 0; n < components.length; n++) {
            if (additionalTaxa[n] != null && additionalTaxa[n].cardinality() > 0) {
                for (Cluster resultCluster : resultClusters) {
                    if (Cluster.contains(resultCluster, allTaxaInComponent[n])) {
                        resultCluster.or(additionalTaxa[n]);
                    }
                }
            }
        }

        for (int n = 0; n < components.length; n++) {
            if (additionalTaxa[n] != null && additionalTaxa[n].cardinality() > 0) {
                boolean foundIn = false;
                boolean found = false;
                for (Cluster resultCluster : resultClusters) {
                    if (Cluster.contains(resultCluster, Cluster.union(allTaxaInComponent[n], additionalTaxa[n])))
                        foundIn = true;
                    if (Cluster.equals(resultCluster, Cluster.union(allTaxaInComponent[n], additionalTaxa[n])))
                        found = true;
                }
                if (!found)
                    System.err.println("Component not equal to any cluster: " + StringUtils.toString(allTaxaInComponent[n]));
                if (!foundIn)
                    System.err.println("Component not contained in any cluster: " + StringUtils.toString(allTaxaInComponent[n]));
            }
        }

        progressListener.close();

        // remove all additional, only for debugging!
        /*
        for(int i=0;i<resultClusters.length;i++)
        {
        Cluster cluster=resultClusters[i];
        // remove all additional taxa
        for(int t=cluster.nextSetBit(maxTaxonId+1);t!= -1;t=cluster.nextSetBit(t+1) )
            cluster.set(t,false);
        }
        */

        // System.err.println("Clusters after adding markers:"); Cluster.print(resultClusters);


        computeWeights(clusters, resultClusters);

        // System.err.println("Number of reticulations: " + totalReticulate);

        return resultClusters;
    }


    /**
     * computes weights for all output clusters from the input clusters
     *
	 */
    private void computeWeights(Cluster[] iClusters, Cluster[] oClusters) {
        int[] mapIn2Out = new int[iClusters.length]; // maps each input cluster to its output cluster

        for (int ic = 0; ic < iClusters.length; ic++) {
            Cluster iCluster = iClusters[ic];
            int smallest = Integer.MAX_VALUE;
            for (int oc = 0; oc < oClusters.length; oc++) {
                Cluster oCluster = oClusters[oc];
                if (oCluster.cardinality() < smallest && Cluster.contains(oCluster, iCluster)) {
                    mapIn2Out[ic] = oc;
                    smallest = oCluster.cardinality();
                }
            }
        }
        double[] oWeights = new double[oClusters.length];
        int[] oCounts = new int[oClusters.length];

        for (int ic = 0; ic < iClusters.length; ic++) {
            Cluster iCluster = iClusters[ic];
            int oc = mapIn2Out[ic];
            if (oc < Integer.MAX_VALUE) {
                oWeights[oc] += iCluster.getWeight();
                oCounts[oc]++;
            } else {
                System.err.println("WARNING: unmapped input cluster: " + iCluster);
            }
        }
        for (int oc = 0; oc < oClusters.length; oc++) {
            Cluster oCluster = oClusters[oc];
            if (oCounts[oc] > 0) {
                oCluster.setWeight(oWeights[oc] / oCounts[oc]);
            } else {
                System.err.println("WARNING: zero-weight output cluster: " + oCluster);
                oCluster.setWeight(0);
            }
        }
    }

    /**
     * determines incompatibity components
     *
     * @return all incompatibity components
     */
    private static BitSet[] computeComponents(boolean[][] incompatible) {
        int[] componentNumber = new int[incompatible.length];
		Arrays.fill(componentNumber, -1);

        int number = 0;
        for (int i = 0; i < incompatible.length; i++) {
            if (componentNumber[i] == -1)
                computeComponentsRec(incompatible, i, number++, componentNumber);
        }

        BitSet[] components = new BitSet[number];
        for (int i = 0; i < components.length; i++)
            components[i] = new BitSet();

        for (int c = 0; c < incompatible.length; c++) {
            components[componentNumber[c]].set(c);
        }
        return components;
    }

    /**
     * recursively does the work
     *
	 */
    private static void computeComponentsRec(boolean[][] incompatible, int i, int number, int[] componentNumber) {
        componentNumber[i] = number;
        for (int j = 0; j < incompatible.length; j++) {
            if (i != j && incompatible[i][j] && componentNumber[j] == -1) {
                computeComponentsRec(incompatible, j, number, componentNumber);
            }
        }
    }
}
