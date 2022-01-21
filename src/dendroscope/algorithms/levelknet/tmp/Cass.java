/*
 * Cass.java Copyright (C) 2022
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
package dendroscope.algorithms.levelknet.tmp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Stack;
import java.util.Vector;

/**
 * Leos Cass algorithm
 * Leo 2009
 */
public class Cass {

    public static final boolean PRINT_EXTRA_INFO = false;
    public static final int DUMMY_NUMBER = 9999;
    public static boolean EDGE_LABELS = true;
    public static final boolean PRINT_ALL_ARCS = true;
    private static final int MAX_CIRCLE_SIZE = 5;

    private static boolean printClusters = false; // label each edge by the cluster its represents
    private static boolean colourEdges = false; // colour reticulation edges red
    public static final Vector<String> stringTaxa = new Vector<>();
    public static String leafShape = "circle";

    public static void main(String[] args) {
        ClusterSet c = new ClusterSet();

        if (args.length == 0)
            args = new String[]{"/Users/huson/in.txt"};

        if (args.length == 0) {
            System.out.println("Terminating: No input file specified.");
            System.exit(0);
        } else {
            String fileName = args[0];
            int recCount = 0;

            try {
                FileReader fr = new FileReader(fileName);
                BufferedReader br = new BufferedReader(fr);

                String record = "";
                while ((record = br.readLine()) != null) {
                    if (record.startsWith("//")) {
                        continue; //! ignore comments
                    }
                    recCount++;
                    String[] clusterData = record.split(" ");
//
//                    if ((clusterData.length < 2)) {
//                        System.out.println("Read line: " + record);
//                        System.out.println("Malformed line: each line should consist of at least two space-delimited integers.");
//
//                        throw new IOException();
//                    }

                    Vector cluster = addCluster(clusterData);
                    c.addCluster(cluster);
                }
            } catch (IOException e) {
                // catch possible io errors from readLine()
                System.out.println("Problem reading file " + fileName + ": " + e.getMessage());
                System.exit(0);
            }
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

        DiGraph N = c.minSL();
        if (N != null) {
            N.printDiGraph(printClusters, colourEdges);
        } else {
            System.out.println("No network found.");
        }
    }

    public static Vector addCluster(String[] cluster) {
        Vector<Integer> vecCluster = new Vector<>();
        for (String aCluster : cluster) {
            int taxon;
            if (aCluster.length() > Cass.MAX_CIRCLE_SIZE) {
                Cass.leafShape = "box";
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
}

class ClusterSet {

    public Vector clusterVec; // vectors of integers
    public Vector taxa; // integers
    public Vector taxaInTaxa; // vectors of integers
    public int numClusters;

    public ClusterSet() {
        clusterVec = new Vector();
        taxa = new Vector();
        taxaInTaxa = new Vector();
        numClusters = 0;
    }

    public static Vector<Integer> sortCluster(Vector<Integer> cluster) {
        for (int i = 0; i < cluster.size() - 1; i++) {
            for (int j = i + 1; j < cluster.size(); j++) {
                int x = cluster.elementAt(i);
                Integer y = cluster.elementAt(j);
                if (x > y) {
                    // swap
                    Integer x2 = x;
                    cluster.set(i, y);
                    cluster.set(j, x2);
                }
            }
        }
        return cluster;
    }

    public void printClusterSet() {
        for (int c = 0; c < numClusters; c++) {
            Vector cluster = (Vector) clusterVec.elementAt(c);
            String clusterString = "";
            for (int i = 0; i < cluster.size(); i++) {
                int x = (Integer) cluster.elementAt(i);
                clusterString = clusterString + x;
            }
            System.out.println(clusterString);
        }
        System.out.println("--------------------");
    }

    public ClusterSet remLeaf(Object x) {
        ClusterSet CS = new ClusterSet();
        for (int c = 0; c < numClusters; c++) {
            Vector cluster = (Vector) clusterVec.elementAt(c);
            Vector cluster2 = new Vector(0);
            for (int i = 0; i < cluster.size(); i++) {
                Object y = cluster.elementAt(i);
                if (y != x) {
                    cluster2.add(y);
                }
            }
            if (cluster2.size() > 0 && !CS.contains(cluster2)) {
                CS.addCluster(cluster2);
            }
        }
        return CS;
    }

    public DiGraph buildTree() {
        // create a root
        DiGraph G = new DiGraph();
        // find the maximal clusters
        Vector maxClusters = new Vector(0);
        for (int i = 0; i < numClusters; i++) {
            Vector cluster1 = (Vector) clusterVec.elementAt(i);
            boolean max = true;
            for (int j = 0; j < numClusters; j++) {
                Vector cluster2 = (Vector) clusterVec.elementAt(j);
                int rel = getRelation(cluster1, cluster2);
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
        // the maximal clusters become children of the root
        for (int c = 0; c < maxClusters.size(); c++) {
            Vector cluster = (Vector) maxClusters.elementAt(c);
            if (cluster.size() == 1) {
                int[] label = new int[1];
                label[0] = (Integer) cluster.elementAt(0);
                G.outdeg++;
                G.children[c] = new DiGraph(label);
                G.children[c].indeg = 1;
            } else {
                ClusterSet CS = this.restrict(cluster);
                G.outdeg++;
                DiGraph H = CS.buildTree();
                if (H != null) {
                    G.children[c] = H;
                } else {
                    return null;
                }
                G.children[c].indeg = 1;
            }
        }
        return G;
    }

    public boolean contains(Vector cluster) {
        return clusterVec.contains(cluster);
    }

    public DiGraph minSL() {
        int k = 0;
        boolean found = false;
        DiGraph G = new DiGraph();
        while (!found) {
            System.out.println("// Searching for networks with level " + k);
            Vector networks = SL(k);
            if (networks.size() > 0) {
                G = (DiGraph) networks.elementAt(0);
                G.remDummyLeaves();
                G.cleanDiGraph();
                for (int i = 0; i < 2 * k; i++) {
                    // might have to repeat this a couple of times before everything has been cleaned up
                    G = G.postprocess();
                    G.cleanDiGraph();
                }
                found = true;
            }
            k++;
        }
        return G;
    }

    public static final int FASE1 = 0;
    public static final int FASE2 = 1;

    class StackObject {
        // the StackObject is a snapshot of the data in either fase 1 (collapse, remove leaf steps)
        //                                              or fase 2 (hang leaf back, decollapse steps)

        Vector taxa; // all the taxa.... don't really need this
        int step; // the step we're at in the current fase
        int type; // either fase 1 or fase 2
        final ClusterSet[] CS; // the cluster sets after 0,1,2,... "collapse, remove leaf"-steps
        final Vector[] collapsedTaxaSets; // the sets of taxa we collapsed in step 0,1,2,...
        final Vector[] collapsedTaxa; // the taxa we collapsed them into in step 0,1,2,...
        final Object[] removedLeaves; // the leaf we've removed in step 0,1,2,...

        // only for fase 2
        DiGraph network; // the solution in the current step of the second fase

        public StackObject(int k) {
            this.step = 0;
            CS = new ClusterSet[k + 1];
            collapsedTaxa = new Vector[k + 1];
            collapsedTaxaSets = new Vector[k + 1];
            removedLeaves = new Object[k + 1];
        }
    }

    public Vector SL(int k) {

        // in this vector we will put all valid networks
        Vector<DiGraph> networks = new Vector<>();

        // return an empty vector if there are no leaves left
        if (taxa.size() == 0) {
            return networks;
        }

        if (k == 0) {
            // we try to create a tree
            // construct a tree containing precisely those clusters in CSp
            DiGraph T = buildTree();
            if (T != null) {
                networks.add(T);
            }
            return networks;
        }

        Stack<StackObject> stack = new Stack<>();

        StackObject so = new StackObject(k);
        so.taxa = taxa;
        so.type = FASE1;
        so.CS[0] = this; // the current cluster set
        so.collapsedTaxa[0] = null;
        so.collapsedTaxaSets[0] = null;
        so.removedLeaves[0] = null;

        stack.push(so);

        while (stack.size() != 0) {

            StackObject top = stack.pop();

            if (top.type == FASE1) {
                if (top.step == k) {
                    // build a tree and start the second fase
                    // construct a tree containing precisely those clusters in top.CS[k]
                    DiGraph T = top.CS[k].buildTree();
                    if (T != null) {
                        // add the tree to the stackobject
                        // change the fase
                        top.type = FASE2;
                        top.network = T;
                        // put the stackobject back on the stack
                        stack.push(top);
                    }
                }

                // collapse all maximal ST-sets
                Vector output = top.CS[top.step].collapse();
                ClusterSet collapsed = (ClusterSet) output.elementAt(0);
                Vector collapsedTaxa = (Vector) output.elementAt(1);
                Vector collapsedTaxaSets = (Vector) output.elementAt(2);

                // loop through all taxa
                for (int i = 0; i < collapsed.taxa.size() + 1; i++) {

                    ClusterSet CSp;
                    Object x;
                    if (i == collapsed.taxa.size()) {
                        // don't remove a leaf
                        // do not collapse
                        CSp = top.CS[top.step];
                        x = Cass.DUMMY_NUMBER;
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
                    // output this solution
                    networks.add(top.network);
                    return networks;
                }
                // number all edges
                int[] num = new int[1];
                num[0] = 0;
                int e = top.network.numberEdges(num);
                top.network.cleanDiGraph();
                if (Cass.PRINT_EXTRA_INFO) {
                    top.network.printDiGraph(false, true);
                }
                int[] xint = new int[1];
                xint[0] = (Integer) top.removedLeaves[top.step];
                // if there are 2 cherries, we should only really sub cherries, unless the last leaf we removed was a dummy leaf, because then we haven't collapsed properly
                // if there are more cherries we can stop, unless the last leaf we removed was a dummy leaf, because then we haven't collapsed properly
                // if there's 1 cherry, we should sub that one and one arb other edge, unless the last leaf we removed was a dummy leaf, because then we haven't collapsed properly
                Vector cherries = top.network.findCherries();
                top.network.cleanDiGraph();
                if (cherries.size() > 2 && xint[0] != Cass.DUMMY_NUMBER) {
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
                    // skip this edge if it is not a cherry-edge and there are two cherries
                    if (cherries.size() == 2 && !cherry1.contains((int) (e1)) && !cherry2.contains((int) (e1)) && xint[0] != Cass.DUMMY_NUMBER) {
                        continue;
                    }
                    for (int e2 = e1; e2 < e; e2++) {
                        // if there is a cherry we have to hit it
                        if (cherries.size() > 0 && !cherry1.contains((int) (e1)) && !cherry1.contains((int) (e2)) && xint[0] != Cass.DUMMY_NUMBER) {
                            continue;
                        }
                        // if there are two cherries we have to hit both of them
                        if (cherries.size() == 2 && !cherry2.contains((int) (e1)) && !cherry2.contains((int) (e2)) && xint[0] != Cass.DUMMY_NUMBER) {
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
                        r.children[0] = new DiGraph(xint);
                        r.children[0].indeg = 1;
                        r.outdeg = 1;
                        if (Cass.PRINT_EXTRA_INFO) {
                            G.printDiGraph(false, true);
                        }
                        // if G has cherries at this point we can forget about it
                        // but it shouldn't really have cherries if we're carefull
                        if (xint[0] != Cass.DUMMY_NUMBER) {
                            // ONLY UNCOLAPSE IF WE HAVEN'T ADDED A DUMMY LEAF
                            // OTHERWISE THE UNCOLLAPSE FUNCTION WON'T WORK PROPERLY
                            G.uncollapse(top.CS[top.step - 1], top.collapsedTaxa[top.step], top.collapsedTaxaSets[top.step]);
                        }
                        // check if the network displays all clusters
                        if (Cass.PRINT_EXTRA_INFO) {
                            G.printDiGraph(false, true);
                        }
                        if (G.displays(top.CS[top.step - 1], k)) {
                            if (Cass.PRINT_EXTRA_INFO) {
                                G.printDiGraph(false, true);
                            }

                            // contract some edges
                            G = G.postprocess();
                            G.cleanDiGraph();

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
                }
            }
            System.gc();
        }
        System.gc();
        return networks;
    }

    public ClusterSet cloneClusterSet() {
        ClusterSet CS = new ClusterSet();
        CS.clusterVec = (Vector) clusterVec.clone();
        CS.numClusters = numClusters;
        CS.taxa = (Vector) taxa.clone();
        CS.taxaInTaxa = (Vector) taxaInTaxa.clone();
        return CS;
    }

    public Vector collapse() {
        Vector output = new Vector();
        ClusterSet CS = cloneClusterSet();
        Vector collapsedTaxa = new Vector();
        Vector collapsedTaxaSets = new Vector();
        boolean suc = true;
        while (suc) {
            suc = false;
            // collapse two leaves
            int i = 0;
            int j = 1;
            while (i < CS.taxa.size() - 1) {
                Object x = CS.taxa.elementAt(i);
                Object y = CS.taxa.elementAt(j);
                if (!CS.separated(x, y)) {
                    int ix = collapsedTaxa.indexOf(x);
                    int iy = collapsedTaxa.indexOf(y);
                    Vector setx;
                    Vector sety;
                    if (ix != -1) {
                        setx = (Vector) collapsedTaxaSets.elementAt(ix);
                    } else {
                        setx = new Vector();
                        setx.add(x);
                    }
                    if (iy != -1) {
                        sety = (Vector) collapsedTaxaSets.elementAt(iy);
                    } else {
                        sety = new Vector();
                        sety.add(y);
                    }
                    setx.addAll(sety);
                    if (ix != -1) {
                        // shouldn't be necessary
                        collapsedTaxaSets.setElementAt(setx, ix);
                    } else {
                        collapsedTaxa.add(x);
                        collapsedTaxaSets.add(setx);
                    }
                    if (iy != -1) {
                        collapsedTaxaSets.removeElementAt(iy);
                        collapsedTaxa.removeElementAt(iy);
                    }
                    // collapse x and y
                    suc = true;
                    Vector z1 = (Vector) CS.taxaInTaxa.elementAt(i);
                    Vector z2 = (Vector) z1.clone();
                    Vector z3 = (Vector) CS.taxaInTaxa.elementAt(j);
                    Collection z4 = (Collection) z3.clone();
                    z2.addAll(z4);
                    CS.taxaInTaxa.set(i, z2);
                    CS.taxa.removeElementAt(j);
                    CS.taxaInTaxa.removeElementAt(j);
                    // remove y from each cluster
                    int c = 0;
                    while (c < CS.numClusters) {
                        Vector cluster = (Vector) CS.clusterVec.elementAt(c);
                        if (cluster.contains(y)) {
                            // remove y from cluster
                            Vector cluster2 = (Vector) cluster.clone();
                            cluster2.remove(y);
                            if (cluster2.size() == 0 || CS.contains(cluster2)) {
                                // remove cluster
                                CS.clusterVec.removeElementAt(c);
                                CS.numClusters--;
                            } else {
                                CS.clusterVec.set(c, cluster2);
                                c++;

                            }


                        } else {
                            c++;
                        }

                    }
                } else {
                    j++;
                }

                if (j > CS.taxa.size() - 1) {
                    i++;
                    j =
                            i + 1;
                }
            }
        }
        output.add(CS);
        output.add(collapsedTaxa);
        output.add(collapsedTaxaSets);
        return output;
    }

    private boolean separated(Object x, Object y) {
        boolean sep = false;
        for (int c = 0; c <
                numClusters; c++) {
            Vector cluster = (Vector) clusterVec.elementAt(c);
            if (cluster.size() == 1) {
                continue;
            }

            boolean a = cluster.contains(x);
            boolean b = cluster.contains(y);
            if ((a && !b) || (b & !a)) {
                sep = true;
            }

        }
        return sep;
    }

    public ClusterSet restrict(Vector cluster) {
        ClusterSet C = new ClusterSet();
        for (int c = 0; c < numClusters; c++) {
            Vector cluster2 = (Vector) clusterVec.elementAt(c);
            cluster2 = ClusterSet.sortCluster(cluster2);
            if (ClusterSet.getRelation(cluster, cluster2) == 4) {
                C.addCluster(cluster2);
            }
        }
        return C;
    }

    public void addCluster(Vector cluster) {
        int[] intcluster = new int[cluster.size()];
        for (int i = 0; i < cluster.size(); i++) {
            intcluster[i] = (Integer) cluster.elementAt(i);
        }
        this.addCluster(intcluster);
    }

    public void addCluster(int cluster[]) {
        Vector vecCluster = new Vector();
        for (int aCluster : cluster) {
            vecCluster.add(aCluster);
            Vector singleton = new Vector(0);
            singleton.add(aCluster);
            if (!taxa.contains((int) (aCluster))) {
                clusterVec.add(singleton);
                taxa.add(aCluster);
                taxaInTaxa.add(singleton);
                numClusters++;
            }
        }
        vecCluster = ClusterSet.sortCluster(vecCluster);
        if (!clusterVec.contains(vecCluster)) {
            clusterVec.add(vecCluster);
            numClusters++;
        }
    }

    private static int getRelation(Vector c1, Vector c2) {
        // 1 = disjoint
        // 2 = incompatible
        // 3 = c1 strictly included in c2
        // 4 = c2 strictly included in c1
        // 5 = clusters are identical
        if (c1.equals(c2)) {
            return 5;
        }
        if (c1.containsAll(c2)) {
            return 4;
        }
        if (c2.containsAll(c1)) {
            return 3;
        }
        for (int i = 0; i < c1.size(); i++) {
            if (c2.contains(c1.elementAt(i))) {
                return 2;
            }
        }
        return 1;
    }
}

class DiGraph {

    public final DiGraph[] children;
    private DiGraph clone;
    public final int[] label;
    public final int[] edgenumber;
    public int retnumber;
    public final int[] retside; // 0 = this is the left parent of the reticulation, 1 = right parent, 2 = this child is not a reticulation
    public final Vector[] clusters;
    public int number;
    public int outdeg;
    public int indeg;
    public boolean visited;
    public final boolean[] switched;
    private static final int MAX_DEGREE = 30;

    public DiGraph() {
        children = new DiGraph[MAX_DEGREE];
        outdeg = 0;
        indeg = 0;
        label = new int[1];
        label[0] = 0;
        edgenumber = new int[MAX_DEGREE];
        retnumber = 0;
        retside = new int[MAX_DEGREE];
        switched = new boolean[MAX_DEGREE];
        clusters = new Vector[MAX_DEGREE];
        for (int d = 0; d < MAX_DEGREE; d++) {
            clusters[d] = new Vector();
            edgenumber[d] = 0;
            retside[d] = 2; // not a reticulation
            switched[d] = true; // all edges are switched on by default
        }
        number = 0;
        visited = false;
    }

    public DiGraph(int l[]) {
        children = new DiGraph[0];
        outdeg = 0;
        indeg = 0;
        label = l;
        edgenumber = new int[0];
        retnumber = 0;
        clusters = new Vector[0];
        retside = new int[0];
        switched = new boolean[0];
        number = 0;
        visited = false;
    }

    public Vector findCherries() {
        Vector output = new Vector();
        if (visited) {
            return output;
        }
        visited = true;
// we should not count leaf-edges below the root as cherry-edges, because they could be the result of previous contraction-deltion steps
//        if (indeg == 0) {
//            // this is the root
//            if (outdeg == 1 && children[0].outdeg > 0) {
//                // the leaf-edges of the only child of the root count as cherry-edges
//                Vector rootCherry = new Vector();
//                for (int c = 0; c < children[0].outdeg; c++) {
//                    if (children[0].children[c].outdeg == 0) {
//                        // this child of the root is a leaf
//                        // this counts as a cherry edge
//                        rootCherry.add(children[0].edgenumber[c]);
//                    }
//                }
//                if (rootCherry.size() > 0) {
//                    output.add(rootCherry);
//                }
//            } else {
//                // the leaf-edges leaving the root count as cherry-edges
//                Vector rootCherry = new Vector();
//                for (int c = 0; c < outdeg; c++) {
//                    if (children[c].outdeg == 0) {
//                        // this child of the root is a leaf
//                        // this counts as a cherry edge
//                        rootCherry.add(edgenumber[c]);
//                    }
//                }
//                if (rootCherry.size() > 0) {
//                    output.add(rootCherry);
//                }
//            }
//        } else {

        // outgoing edges are cherry-edges if there are at least 2 and all of them are leaf-edges
        boolean cher = true;
        if (outdeg < 2) {
            cher = false;
        }
        for (int c = 0; c < outdeg; c++) {
            if (children[c].outdeg != 0) {
                cher = false;
            }
        }
        if (cher) {
            // this is a cherry
            Vector cherry = new Vector();
            for (int c = 0; c < outdeg; c++) {
                cherry.add(edgenumber[c]);
            }
            output.add(cherry);
            return output;
        }

        // find cherry-edges recursively
        for (int c = 0; c < outdeg; c++) {
            Collection recVec = children[c].findCherries();
            output.addAll(recVec);
        }
        return output;
    }

    public void remDummyLeaves() {
        if (visited) {
            return;
        }
        visited = true;
        int c = 0;
        while (c < outdeg) {
            if (children[c].label[0] == Cass.DUMMY_NUMBER) {
                // this child is a dummy leaf
                // for now, we just remove the dummy leaf
                // we can suppress this vertex later
                System.arraycopy(children, c + 1, children, c + 1 - 1, outdeg - (c + 1));
                outdeg--;
            } else {
                c++;
            }
        }
        for (int d = 0; d < outdeg; d++) {
            children[d].remDummyLeaves();
        }
    }

    public DiGraph postprocess() {
        if (indeg == 0 && outdeg == 1) {
            // this root has outdegree 1
            // delete it
            children[0].indeg = 0;
            return children[0];
        }
        if (visited) {
            return this;
        }
        visited = true;
        for (int c = 0; c < outdeg; c++) {
            if (children[c].outdeg == 1 && children[c].indeg == 1) {
                // suppress this child
                children[c].indeg--;
                children[c].children[0].indeg++;
                children[c] = children[c].children[0];
                c--;
                continue;
            }
            if (children[c].outdeg == 1 && children[c].children[0].outdeg == 1) {
                // this is an edge between two reticulations
                // point directly to the child of the child
                children[c].indeg--;
                children[c].children[0].indeg++;
                children[c] = children[c].children[0];
                c--;
                continue;
                // if this results into a vertex with indegree and outdegree 1 it will be suppressed later (I hope)
            }
            boolean suppress = true;
            for (int i = 0; i < clusters[c].size(); i++) {
                Vector cluster = (Vector) clusters[c].elementAt(i);
                if (cluster.size() > 1) {
                    suppress = false;
                }
            }
            if (suppress && children[c].indeg == 1 && children[c].outdeg > 0) {
                // this is a tree-edge that doesn't represent any nontrivial clusters and doesn't lead to a leaf
                // suppress this edge
                for (int cc = 1; cc < children[c].outdeg; cc++) {
                    outdeg++;
                    children[outdeg - 1] = children[c].children[cc];
                }
                children[c] = children[c].children[0];
                c--;
            }
        }
        for (int c = 0; c < outdeg; c++) {
            children[c].postprocess();
        }
        return this;
    }

    public DiGraph cloneDiGraph() {
        if (visited) {
            return clone;
        }
        visited = true;
        DiGraph G = new DiGraph();
        G.outdeg = outdeg;
        G.indeg = indeg;
        G.label[0] = label[0];
        G.retnumber = retnumber;
        for (int d = 0; d < outdeg; d++) {
            G.clusters[d] = clusters[d];
            G.edgenumber[d] = edgenumber[d];
            G.retside[d] = retside[d];
            G.switched[d] = switched[d];
        }
        G.number = number;
        for (int c = 0; c < outdeg; c++) {
            G.children[c] = children[c].cloneDiGraph();
        }
        clone = G;
        return G;
    }

    public int numberEdges(int[] num) {
        // returns the number of edges reachable from this vertex, excluding edges already visited
        if (visited) {
            return 0;
        }
        visited = true;
        int n = 0;
        for (int c = 0; c < outdeg; c++) {
            edgenumber[c] = num[0];
            num[0]++;
            n += children[c].numberEdges(num);
            n++;
        }
        return n;
    }

    public void numberRetEdges(int[] num) {
        if (visited) {
            return;
        }
        visited = true;
        for (int c = 0; c < outdeg; c++) {
            if (children[c].indeg > 1) {
                // this child is a reticulation
                if (children[c].visited) {
                    retside[c] = 1;
                } else {
                    retside[c] = 0;
                    children[c].retnumber = num[0];
                    num[0]++;
                }
            } else {
                retside[c] = 2;
            }
            children[c].numberRetEdges(num);
        }
    }

    public boolean displays(ClusterSet CS, int k) {
        boolean disp = true;
        // number all reticulation edges
        int[] num = new int[1];
        num[0] = 0;
        numberRetEdges(num);
        cleanDiGraph();
        if (Cass.PRINT_EXTRA_INFO) {
            printDiGraph(false, true);
        }
        boolean[] retOn = new boolean[k];
        // update all clusters
        clearClusters();
        cleanDiGraph();
        loopRet(CS, k - 1, retOn);
        switchAllOn();
        cleanDiGraph();
        if (Cass.PRINT_EXTRA_INFO) {
            printDiGraph(true, true);
        }
        // check if all clusters are displayed
        for (int c = 0; c < CS.numClusters; c++) {
            Vector cluster = (Vector) CS.clusterVec.elementAt(c);
            if (!clusterDisplayed(cluster)) {
                disp = false;
            }
            cleanDiGraph();
        }
        return disp;
    }

    public void switchAllOn() {
        // switches all edges on
        if (visited) {
            return;
        }
        for (int c = 0; c < outdeg; c++) {
            switched[c] = true;
            children[c].switchAllOn();
        }
    }

    public boolean clusterDisplayed(Vector cluster) {
        // returns true if this cluster is displayed by the digraph and false otherwise
        if (visited) {
            return false;
        }
        visited = true;
        if (cluster.size() == 1) {
            return true;
        }
        for (int c = 0; c < outdeg; c++) {
            if (clusters[c].contains(cluster)) {
                // the cluster is displayed by this edge
                return true;
            }
            if (children[c].clusterDisplayed(cluster)) {
                // the cluster is displayed by some edge reachable form this vertex
                return true;
            }
        }
        return false;
    }

    public void clearClusters() {
        // resets all clusters
        if (visited) {
            return;
        }
        visited = true;
        for (int c = 0; c < outdeg; c++) {
            clusters[c] = new Vector();
            children[c].clearClusters();
        }
    }

    public void loopRet(ClusterSet CS, int i, boolean[] retOn) {
        if (i < 0) {
            // retOn has been filled out
            // switch the reticulation edges on/off based on retOn
            switchRet(retOn);
            cleanDiGraph();
            if (Cass.PRINT_EXTRA_INFO) {
                printDiGraph(false, true);
            }
            // update clusters
            updateClusters(CS);
            cleanDiGraph();
            if (Cass.PRINT_EXTRA_INFO) {
                printDiGraph(false, true);
            }
        } else {
            // fill out the i-th position of retOn and recurse
            retOn[i] = true;
            loopRet(CS, i - 1, retOn);
            retOn[i] = false;
            loopRet(CS, i - 1, retOn);
        }
    }

    public void switchRet(boolean[] retOn) {
        if (visited) {
            return;
        }
        visited = true;
        for (int c = 0; c < outdeg; c++) {
            if (retside[c] == 0) {
                // this parent is on the left side of the reticulation
                switched[c] = retOn[children[c].retnumber];
            }
            if (retside[c] == 1) {
                // right parent is on the right side of the reticulation
                switched[c] = !retOn[children[c].retnumber];
            }
            children[c].switchRet(retOn);
        }
    }

    public Vector updateClusters(ClusterSet CS) {
        // returns the cluster displayed by the incoming edge
        Vector cluster = new Vector();
        if (outdeg == 0) {
            // this is a leaf
            // return a singleton cluster
            int lab = label[0];
            if (lab != Cass.DUMMY_NUMBER) {
                cluster.add(lab);
            }
            return cluster;
        }
        for (int c = 0; c < outdeg; c++) {
            if (!switched[c]) {
                // this reticulation edge has not been switched on
                continue;
            }
            Vector childCluster = children[c].updateClusters(CS);
            childCluster = ClusterSet.sortCluster(childCluster);
            // check if this cluster is contained in CS
            // and not yet in the cluster set of thid edge
            // and that this is not a reticulation edge
            if (CS.contains(childCluster) && !clusters[c].contains(childCluster) && children[c].indeg < 2) {
                // store this cluster as one of the clusters displayed by this outgoing edge
                clusters[c].add(childCluster);
            }
            // add the elements to cluster displayed by the incoming edge
            for (int i = 0; i < childCluster.size(); i++) {
                cluster.add(childCluster.elementAt(i));
            }
        }
        return cluster;
    }

    public void hangBelowEdges(DiGraph r, int e1, int e2) {
        if (visited) {
            return;
        }
        visited = true;
        DiGraph v;
        DiGraph w;
        for (int d = 0; d < outdeg; d++) {
            // if e1=e2 is this edge then we subdivide it twice
            if (edgenumber[d] == e1 && edgenumber[d] == e2) {
                // subdivide this edge (this,children[d]) to this -> v -> w -> children[d]
                // and add edges v -> r and w -> r
                v = new DiGraph();
                w = new DiGraph();
                v.children[0] = w;
                v.children[1] = r;
                v.indeg = 1;
                v.outdeg = 2;
                w.children[0] = children[d];
                w.children[1] = r;
                w.indeg = 1;
                w.outdeg = 2;
                children[d] = v;
                if (Cass.PRINT_EXTRA_INFO) {
                    this.printDiGraph(false, true);
                }
                // no need to recurse
            } else {
                // if this edge is either equal to e1 or to e2, but not to both, we subdivide it once
                if (edgenumber[d] == e1 || edgenumber[d] == e2) {
                    // subdivide this edge by a new vertex v
                    v = new DiGraph();
                    v.children[0] = children[d];
                    v.indeg = 1;
                    // and add an edge from v to r
                    v.children[1] = r;
                    v.outdeg = 2;
                    children[d] = v;
                    if (Cass.PRINT_EXTRA_INFO) {
                        this.printDiGraph(false, true);
                    }
                    if (v.children[0].outdeg > 0) {
                        v.children[0].hangBelowEdges(r, e1, e2);
                    }
                }
            }
            if (edgenumber[d] != e2 && edgenumber[d] != e1) {
                children[d].hangBelowEdges(r, e1, e2);
            }
        }
    }

    public void printDiGraph(boolean printClusters, boolean colourEdges) {
        System.out.println("strict digraph G {");
        int[] num = new int[1];
        num[0] = 1000;
        this.printNode(num);
        this.printArcs(printClusters, colourEdges);
        System.out.println("}");
        this.cleanDiGraph();
    }

    public void printNode(int num[]) {
        if (number != 0) {
            return; // already visited
        }
        if (label[0] > 0) {
            // this is a leaf
            number = label[0];
            String taxon = Cass.stringTaxa.elementAt(number - 1);
            System.out.println(number + " [shape=" + Cass.leafShape + ", width=0.3, label=" + taxon + "];");
        } else {
            // this is an internal vertex
            if (indeg > 1 && Cass.PRINT_EXTRA_INFO) {
                // this is a reticulation
                number = num[0];
                System.out.println(number + " [shape=circle, label=\"" + retnumber + "\"];");
                for (int c = 0; c < outdeg; c++) {
                    num[0]++;
                    children[c].printNode(num);
                }
            } else {
                number = num[0];
                System.out.println(number + " [shape=point];");
                for (int c = 0; c < outdeg; c++) {
                    num[0]++;
                    children[c].printNode(num);
                }
            }
        }
    }

    public void printArcs(boolean printClusters, boolean colourEdges) {
        for (int c = 0; c < outdeg; c++) {
            if (switched[c] || Cass.PRINT_ALL_ARCS) {
                // create edgelabel containing of all clusters displayed by this edge
                String edgeLabel = "";
                boolean notEmpty = false;
                for (int j = 0; j < clusters[c].size(); j++) {
                    Vector cluster = (Vector) clusters[c].elementAt(j);
                    if (cluster.size() > 1) {
                        if (notEmpty) {
                            edgeLabel = edgeLabel + ", ";
                        }
                        String clusterString = "[";
                        for (int i = 0; i < cluster.size(); i++) {
                            int taxon = (Integer) cluster.elementAt(i);
                            String taxonString = Cass.stringTaxa.elementAt(taxon - 1);
                            clusterString = clusterString + taxonString;
                            if (i < cluster.size() - 1) {
                                clusterString = clusterString + ",";
                            } else {
                                clusterString = clusterString + "]";
                            }
                        }
                        edgeLabel = edgeLabel + clusterString;
                        notEmpty = true;
                    }
                }
                if (notEmpty) {
                    edgeLabel = "{" + edgeLabel + "}";
                }
                if (Cass.PRINT_EXTRA_INFO && !printClusters) {
                    edgeLabel = "" + edgenumber[c];
                }
                if (!Cass.PRINT_EXTRA_INFO && !printClusters) {
                    edgeLabel = "";
                }
                String edgeColour = "black";
                if (children[c].indeg > 1 && colourEdges) {
                    edgeColour = "red";
                }

                System.out.println(number + " -> " + children[c].number + "[label=\"" + edgeLabel + "\", color=" + edgeColour + "]");
            }
        }
        visited = true;
        for (int c = 0; c < outdeg; c++) {
            if (!children[c].visited) {
                children[c].printArcs(printClusters, colourEdges);
            }
        }
    }

    public void cleanDiGraph() {
        if (visited) {
            visited = false;
            number = 0;
            for (int c = 0; c < outdeg; c++) {
                children[c].cleanDiGraph();
            }
        }
    }

    public void uncollapse(ClusterSet CS, Vector collapsedTaxa, Vector collapsedTaxaSets) {
        // we only uncollapse the last step!
        for (int i = 0; i < collapsedTaxa.size(); i++) {
            Object x = collapsedTaxa.elementAt(i);
            Vector cluster = (Vector) collapsedTaxaSets.elementAt(i);
            if (cluster.size() > 1) {
                // uncollapse this taxon
                ClusterSet restrictedCS = CS.restrict(cluster);
                DiGraph G = restrictedCS.buildTree();
                int intx = (Integer) x;
                // replace the leaf by the subtree
                replaceLeafBySubtree(intx, G);
                cleanDiGraph();
            }
        }
    }

    public void replaceLeafBySubtree(int x, DiGraph G) {
        visited = true;
        for (int c = 0; c < outdeg; c++) {
            if (children[c].label[0] == x) {
                if (G.outdeg == 1) {
                    children[c] = G.children[0];
                } else {
                    children[c] = G;
                }
            } else {
                if (!children[c].visited) {
                    children[c].replaceLeafBySubtree(x, G);
                }
            }
        }
    }
}
