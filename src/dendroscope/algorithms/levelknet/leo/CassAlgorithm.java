/*
 * CassAlgorithm.java Copyright (C) 2022
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
package dendroscope.algorithms.levelknet.leo;

import jloda.util.CanceledException;
import jloda.util.progress.ProgressListener;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Stack;
import java.util.Vector;

// todo: use these two dummy versions when used as stand-alone
//class CanceledException extends Exception{}
//class ProgressListener { public void checkForCancel() throws CanceledException {} public void incrementProgress() throws CanceledException {}}


/**
 * The Cass algorithm  for computing in minimum level network
 * Leo van Iersel, 2012
 */
public class CassAlgorithm {
    public static final boolean PRINT_EXTRA_INFO = false;
    public static final int DUMMY_NUMBER = 9999;
    private static final int MAX_CIRCLE_SIZE = 5;
    public static int FOUND = 0; // number of networks found

    public static final Vector stringTaxa = new Vector();
    public static String leafShape = "circle";

    /**
     * stand alone program
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        boolean printClusters = false; // label each edge by the cluster its represents
        boolean colourEdges = false; // colour reticulation edges red
        boolean isOnlyOne = false; // compute only on network?
        boolean checkTrees = false; // only construct networks that display the trees?
        ClusterSet clusterSet = new ClusterSet();

        BufferedReader br;

        if (args.length == 0)  // read from console
        {
            br = new BufferedReader(new InputStreamReader(System.in));
        } else   // read from file
        {
            String fileName = args[0];
            br = new BufferedReader(new FileReader(fileName));
        }

        String record;
        while ((record = br.readLine()) != null) {
            if (record.length() == 0 || record.startsWith("//") || record.startsWith("#")) {
                continue; //! ignore comments  and empty lines
            } else if (record.startsWith("."))  // end of input
                break;
            String[] clusterData = record.split(" ");
            Vector cluster = addCluster(clusterData);
            clusterSet.addCluster(cluster);
        }

        for (int a = 1; a < args.length; a++) {
            String option1 = args[a];
            if (option1.equals("--printclusters")) {
                printClusters = true;
            }
            if (option1.equals("--colouredges")) {
                colourEdges = true;
            }
        }

        Vector networks = minSL(clusterSet, null, isOnlyOne, checkTrees);
        if (networks.size() > 0) {
            ((DiGraph) networks.elementAt(0)).printDiGraph(printClusters, colourEdges);
        } else {
            System.err.println("No network found.");
        }
    }

    private static Vector addCluster(String[] cluster) {
        Vector vecCluster = new Vector();
        for (String aCluster : cluster) {
            Integer taxon;
            if (aCluster.length() > CassAlgorithm.MAX_CIRCLE_SIZE) {
                CassAlgorithm.leafShape = "box";
            }
            if (stringTaxa.contains(aCluster)) {
                taxon = stringTaxa.indexOf(aCluster) + 1;
            } else {
                taxon = stringTaxa.size() + 1;
                stringTaxa.add(aCluster);
            }
            vecCluster.add(taxon);
        }
        return vecCluster;
    }

    public static Vector minSL(ClusterSet CS, ProgressListener progressListener, boolean isOnlyOne, boolean checkTrees) throws CanceledException {
        int k = 0;
        FOUND = 0;
        Vector output = new Vector();
        boolean found = false;

        // System.err.println("Collapsing");
        // collapse all maximal ST-sets
        Vector op = CS.collapse();
        ClusterSet collapsed = (ClusterSet) op.elementAt(0);
        Vector collapsedTaxa = (Vector) op.elementAt(1);
        Vector collapsedTaxaSets = (Vector) op.elementAt(2);

        while (!found) {
            System.err.println("Searching level " + k);
            progressListener.incrementProgress();
            Vector networks = SL(collapsed, k, progressListener, isOnlyOne, checkTrees);
            for (int j = 0; j < networks.size(); j++) {
                DiGraph colG = (DiGraph) networks.elementAt(j);
                DiGraph G = uncollapse(colG, CS, collapsedTaxa, collapsedTaxaSets);
                G.cleanDiGraph();

                // check if network displays all clusters/trees, should not be necessary
                // this has to be done before postprocessing, since postprocessing does not update all attributes
                // boolean disp = G.displays(CS, k, checkTrees);

                // remove dummy leaves
                G.remDummyLeaves();
                G.cleanDiGraph();

                // contract edges if possible
                for (int i = 0; i < k; i++) {
                    // might have to repeat this a couple of times before everything has been cleaned up
                    G = G.postprocess();
                    G.cleanDiGraph();
                }

                //if(disp) {
                found = true;
                output.add(G);
                //} else {
                //    System.err.println("Incorrect network returned");
                //}
            }
            k++;
        }
        return output;
    }

    public static final int FASE1 = 0;
    public static final int FASE2 = 1;

    static class StackObject {
        // the StackObject is a snapshot of the data in either fase 1 (collapse, remove leaf steps)
        //                                              or fase 2 (hang leaf back, decollapse steps)

        Vector taxa; // all the taxa.... don't really need this
        int step; // the step we're at in the current fase
        int type; // either fase 1 or fase 2
        final ClusterSet[] CS; // the cluster sets after 0,1,2,... "collapse, remove leaf"-steps
        final Vector[] collapsedTaxaSets; // the sets of taxa we collapsed in step 0,1,2,...
        final Vector[] collapsedTaxa; // the taxa we collapsed them into in step 0,1,2,...
        final Integer[] removedLeaves; // the leaf we've removed in step 0,1,2,...

        // only for fase 2
        DiGraph network; // the solution in the current step of the second fase

        public StackObject(int k) {
            this.step = 0;
            CS = new ClusterSet[k + 1];
            collapsedTaxa = new Vector[k + 1];
            collapsedTaxaSets = new Vector[k + 1];
            removedLeaves = new Integer[k + 1];
        }
    }

    private static Vector SL(ClusterSet CS, int k, ProgressListener progressListener, boolean isOnlyOne, boolean checkTrees) throws CanceledException {
        // int count = 0;

        // in this vector we will put all valid networks
        Vector networks = new Vector();

        if (k == 0) {
            // we try to create a tree
            // construct a tree containing precisely those clusters in CSp
            DiGraph T = buildTree(CS);
            if (T != null) {
                networks.add(T);
            }
            return networks;
        }

        Stack stack = new Stack();

        StackObject so = new StackObject(k);
        so.taxa = CS.taxa;
        so.type = FASE1;
        so.CS[0] = CS;
        so.collapsedTaxa[0] = null;
        so.collapsedTaxaSets[0] = null;
        so.removedLeaves[0] = null;

        stack.push(so);

        while (stack.size() != 0) {

            StackObject top = (StackObject) stack.peek();
            stack.pop();

            if (top.type == FASE1) {
                if (top.step == k) {
                    // build a tree and start the second fase
                    // construct a tree containing precisely those clusters in top.CS[k]
                    DiGraph T = buildTree(top.CS[k]);
                    if (T != null) {
                        // add the tree to the stackobject
                        // change the fase
                        top.type = FASE2;
                        top.network = T;
                        // put the stackobject back on the stack
                        stack.push(top);
                    }
                    continue;
                }

                // collapse all maximal ST-sets
                // IN THE FIRST STEP THERE IS NO NEED TO COLLAPSE
                Vector output = top.CS[top.step].collapse();
                ClusterSet collapsed = (ClusterSet) output.elementAt(0);
                Vector collapsedTaxa = (Vector) output.elementAt(1);
                Vector collapsedTaxaSets = (Vector) output.elementAt(2);

                // loop through all taxa
                for (int i = 0; i < collapsed.taxa.size() + 1; i++) {
                    ClusterSet CSp;
                    Integer x;
                    if (i == collapsed.taxa.size()) {
                        if (top.step == 0) {
                            // always remove a taxon in the first step
                            break;
                        }
                        // don't remove a leaf
                        // do not collapse
                        CSp = top.CS[top.step];
                        x = CassAlgorithm.DUMMY_NUMBER;
                    } else {
                        // remove the taxon with index i
                        x = collapsed.taxa.elementAt(i);
                        CSp = collapsed.remLeaf(x);
                    }
                    // create a new stackobject
                    StackObject virgin = new StackObject(k);
                    virgin.taxa = top.taxa;
                    virgin.type = FASE1;
                    virgin.step = top.step + 1;
                    for (int c = 0; c < virgin.step; c++) {
                        virgin.CS[c] = top.CS[c];
                        virgin.collapsedTaxa[c] = top.collapsedTaxa[c];
                        virgin.collapsedTaxaSets[c] = top.collapsedTaxaSets[c];
                        virgin.removedLeaves[c] = top.removedLeaves[c];
                    }
                    virgin.CS[virgin.step] = CSp;
                    virgin.collapsedTaxa[virgin.step] = collapsedTaxa;
                    virgin.collapsedTaxaSets[virgin.step] = collapsedTaxaSets;
                    virgin.removedLeaves[virgin.step] = x;
                    stack.push(virgin);
                }
            } else {
                // FASE 2
                if (top.step == 0) {
                    // a solution has been found

                    // we remove the root only if it has outdegree 1 and after removal the network still displays all clusters
                    // NOTE: if we keep an outdegree-1 root, the output network might not be simple, but I don't think that matters
                    if (top.network.outdeg == 1) {
                        if (top.network.children.elementAt(0).displays(top.CS[0], k, checkTrees)) {
                            top.network = top.network.children.elementAt(0);
                            top.network.indeg = 0;
                        }
                    }

                    // output this solution
                    networks.add(top.network);
                    FOUND++;
                    if (FOUND == 1) {
                        System.err.println("Network found");
                    }
                    if (isOnlyOne) {
                        return networks;
                    } else {
                        if (FOUND == 1) {
                            System.err.println("Starting search for alternative solutions");
                        }
                        continue;
                    }
                }
                // number all edges
                int[] num = new int[1];
                num[0] = 0;
                int e = top.network.numberEdges(num);
                top.network.cleanDiGraph();
                Integer x = top.removedLeaves[top.step];
                // if there are 2 cherries, we should only really sub cherries, unless the last leaf we removed was a dummy leaf, because then we haven't collapsed properly
                // if there are more cherries we can stop, unless the last leaf we removed was a dummy leaf, because then we haven't collapsed properly
                // if there's 1 cherry, we should sub that one and one arb other edge, unless the last leaf we removed was a dummy leaf, because then we haven't collapsed properly
                Vector cherries = top.network.findCherries();
                top.network.cleanDiGraph();
                if (cherries.size() > 2 && x != CassAlgorithm.DUMMY_NUMBER) {
                    continue;
                }
                Vector cherry1 = new Vector();
                if (cherries.size() > 0) {
                    cherry1 = (Vector) cherries.elementAt(0);
                }
                Vector cherry2 = new Vector();
                if (cherries.size() > 1) {
                    cherry2 = (Vector) cherries.elementAt(1);
                }
                // loop through each pair of edges
                for (int e1 = 0; e1 < e; e1++) {
                    Integer edge1 = e1;
                    // skip this edge if it is not a cherry-edge and there are two cherries
                    if (cherries.size() == 2 && !cherry1.contains(edge1) && !cherry2.contains(edge1) && x != CassAlgorithm.DUMMY_NUMBER) {
                        continue;
                    }
                    for (int e2 = e1; e2 < e; e2++) {
                        Integer edge2 = e2;
                        // if there is a cherry we have to hit it
                        if (cherries.size() > 0 && !cherry1.contains(edge1) && !cherry1.contains(edge2) && x != CassAlgorithm.DUMMY_NUMBER) {
                            continue;
                        }
                        // if there are two cherries we have to hit both of them
                        if (cherries.size() == 2 && !cherry2.contains(edge1) && !cherry2.contains(edge2) && x != CassAlgorithm.DUMMY_NUMBER) {
                            continue;
                        }
                        // if e1 = e2 we subdivide this edge twice
                        // copy top.network into a new DiGraph G
                        DiGraph G = top.network.cloneDiGraph();
                        top.network.cleanDiGraph();
                        // create a new reticulation
                        DiGraph r = new DiGraph();
                        r.indeg = 2;
                        // hang this reticulation below e1 and e2
                        G.hangBelowEdges(r, e1, e2);
                        G.cleanDiGraph();
                        // create a new leaf x below this reticulation
                        r.children.add(new DiGraph(x));
                        r.children.elementAt(0).indeg = 1;
                        r.outdeg = 1;
                        // if G has cherries at this point we can forget about it
                        // but it shouldn't really have cherries if we're carefull
                        if (x != CassAlgorithm.DUMMY_NUMBER) {
                            // ONLY UNCOLAPSE IF WE HAVEN'T ADDED A DUMMY LEAF
                            // OTHERWISE THE UNCOLLAPSE FUNCTION WON'T WORK PROPERLY
                            uncollapse(G, top.CS[top.step - 1], top.collapsedTaxa[top.step], top.collapsedTaxaSets[top.step]);
                        }
                        // check if the network displays all clusters
                        // (if we just added a dummy leaf, there is no need to check this)
                        if (x == DUMMY_NUMBER || G.displays(top.CS[top.step - 1], k, checkTrees)) {
                            // create new stackobject
                            StackObject virgin = new StackObject(k);
                            virgin.taxa = top.taxa;
                            virgin.step = top.step - 1;
                            virgin.type = FASE2;
                            for (int c = 0; c < top.step; c++) {
                                virgin.CS[c] = top.CS[c];
                                virgin.collapsedTaxa[c] = top.collapsedTaxa[c];
                                virgin.collapsedTaxaSets[c] = top.collapsedTaxaSets[c];
                                virgin.removedLeaves[c] = top.removedLeaves[c];
                            }
                            virgin.network = G;
                            stack.push(virgin);
                        }
                    }
                    //if (progressListener != null)
                    //    progressListener.checkForCancel();
                }
            }
            /*
            if (count < 80) {
                System.err.print(".");
                count++;
            } else {
                System.err.println();
                count = 0;
            }
            */

            if (progressListener != null)
                progressListener.checkForCancel();
        }
        return networks;
    }

    private static DiGraph buildTree(ClusterSet CS) {
        // find the maximal clusters
        Vector maxClusters = new Vector(0);
        for (int i = 0; i < CS.clusterVec.size(); i++) {
            Vector cluster1 = (Vector) CS.clusterVec.elementAt(i);
            boolean max = true;
            for (int j = 0; j < CS.clusterVec.size(); j++) {
                Vector cluster2 = (Vector) CS.clusterVec.elementAt(j);
                int rel = ClusterSet.getRelation(cluster1, cluster2);
                if (rel == 3) {
                    max = false;
                }
                if (rel == 2) {
                    return null;
                }
            }
            if (max) {
                maxClusters.add(cluster1);
            }
        }

        // create a vertex
        DiGraph G = new DiGraph(maxClusters.size());

        // the maximal clusters become children of the root
        for (int c = 0; c < maxClusters.size(); c++) {
            Vector cluster = (Vector) maxClusters.elementAt(c);
            if (cluster.size() == 1) {
                Integer label = (Integer) cluster.elementAt(0);
                G.outdeg++;
                DiGraph leaf = new DiGraph(label);
                leaf.indeg = 1;
                G.children.add(leaf);
            } else {
                ClusterSet restricted = CS.restrict(cluster);
                G.outdeg++;
                DiGraph H = buildTree(restricted);
                H.indeg = 1;
                if (H != null) {
                    G.children.add(H);
                } else {
                    return null;
                }
            }
        }
        return G;
    }

    private static DiGraph uncollapse(DiGraph D, ClusterSet CS, Vector collapsedTaxa, Vector collapsedTaxaSets) {
        // we only uncollapse the last step!
        for (int i = 0; i < collapsedTaxa.size(); i++) {
            Integer x = (Integer) collapsedTaxa.elementAt(i);
            Vector cluster = (Vector) collapsedTaxaSets.elementAt(i);
            if (cluster.size() > 1) {
                // uncollapse this taxon
                ClusterSet restrictedCS = CS.restrict(cluster);
                DiGraph G = buildTree(restrictedCS);
                // replace the leaf by the subtree
                replaceLeafBySubtree(x, D, G);
                D.cleanDiGraph();
            }
        }
        return D;
    }

    private static DiGraph replaceLeafBySubtree(int x, DiGraph D, DiGraph G) {
        D.visited = true;
        for (int c = 0; c < D.outdeg; c++) {
            DiGraph child = D.children.elementAt(c);
            if (child.label == x) {
                if (G.outdeg == 1) {
                    D.children.setElementAt(G.children.elementAt(0), c);
                } else {
                    D.children.setElementAt(G, c);
                }
            } else {
                if (!D.children.elementAt(c).visited) {
                    D.children.setElementAt(replaceLeafBySubtree(x, D.children.elementAt(c), G), c);
                }
            }
        }
        return D;
    }
}


