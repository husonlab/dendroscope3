/**
 * DiGraph.java 
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
package dendroscope.algorithms.levelknet.leo;

import java.util.Collection;
import java.util.Vector;

/**
 * a DiGraph or DiGraph node
 * Leo van Iersel, 2012
 */

public class DiGraph {

    public static final int DUMMY_NUMBER = 9999;
    static int MAX_RET = 0; // for printing purposes
    public Vector<DiGraph> children;
    private DiGraph clone;
    public Integer label;
    public final int[] edgenumber;
    public int retnumber;
    public final int[] retside; // 0 = this is the left parent of the reticulation, 1 = right parent, 2 = this child is not a reticulation
    public Vector<Vector<Integer>> clusters[];
    public int number;
    public int outdeg;
    public int indeg;
    public boolean visited;
    public final boolean[] switched;
    private static final int MAX_DEGREE = 2; // maximum degree if no higher maximum degree is specified

    public DiGraph() {
        children = new Vector<>();
        outdeg = 0;
        indeg = 0;
        label = 0;
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

    public DiGraph(int maxout) {
        children = new Vector<>();
        outdeg = 0;
        indeg = 0;
        label = 0;
        edgenumber = new int[maxout];
        retnumber = 0;
        retside = new int[maxout];
        switched = new boolean[maxout];
        clusters = new Vector[maxout];
        for (int d = 0; d < maxout; d++) {
            clusters[d] = new Vector<>();
            edgenumber[d] = 0;
            retside[d] = 2; // not a reticulation
            switched[d] = true; // all edges are switched on by default
        }
        number = 0;
        visited = false;
    }

    public DiGraph(Integer l) {
        children = new Vector();
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
        // outgoing edges are cherry-edges if there are at least 2 and all of them are leaf-edges
        boolean cher = true;
        if (outdeg < 2) {
            cher = false;
        }
        for (int c = 0; c < outdeg; c++) {
            if (children.elementAt(c).outdeg != 0) {
                cher = false;
            }
        }
        if (cher) {
            // this is a cherry
            final Vector<Integer> cherry = new Vector<>();
            for (int i = 0; i < outdeg; i++)
                cherry.add(edgenumber[i]);
            output.add(cherry);
            return output;
        }

        // find cherry-edges recursively
        for (int c = 0; c < outdeg; c++) {
            Collection recVec = children.elementAt(c).findCherries();
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
            if (children.elementAt(c).label == DUMMY_NUMBER) {
                // this child is a dummy leaf
                // for now, we just remove the dummy leaf
                // we can suppress this vertex later
                children.removeElementAt(c);
                for (int d = c + 1; d < outdeg; d++) {
                    clusters[d - 1] = clusters[d];
                    retside[d - 1] = retside[d];
                    edgenumber[d - 1] = edgenumber[d];
                    switched[d - 1] = switched[d];
                }
                outdeg--;
            } else {
                c++;
            }
        }
        for (int d = 0; d < outdeg; d++) {
            children.elementAt(d).remDummyLeaves();
        }
    }

    public DiGraph postprocess() {
        if (visited) {
            return this;
        }
        if (children.size() != outdeg) {
            System.err.println("Wrong outdegree detected");
        }
        visited = true;
        for (int c = 0; c < outdeg; c++) {
            DiGraph child = children.elementAt(c);
            if (child.outdeg == 1 && child.indeg == 1) {
                // suppress this child
                children.setElementAt(child.children.elementAt(0), c);
                c--;
                continue;
            }
            if (child.outdeg == 1 && child.children.elementAt(0).outdeg == 1) {
                // this is an edge between two reticulations
                // point directly to the child of the child
                child.indeg--;
                child.children.elementAt(0).indeg++;
                children.setElementAt(child.children.elementAt(0), c);
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
            if (suppress && child.indeg == 1 && child.outdeg > 0) {
                // this is a tree-edge that doesn't represent any nontrivial clusters and doesn't lead to a leaf
                // suppress this edge

                // create a longer clusters array
                Vector[] clustersNew = new Vector[outdeg + child.outdeg - 1];
                System.arraycopy(clusters, 0, clustersNew, 0, outdeg);
                int index = outdeg;

                for (int cc = 1; cc < child.outdeg; cc++) {
                    outdeg++;
                    children.add(child.children.elementAt(cc));
                    clustersNew[index] = child.clusters[cc];
                    index++;
                }
                children.setElementAt(child.children.elementAt(0), c);
                clustersNew[c] = child.clusters[0];

                // update clusters
                // other variables are NOT updated
                clusters = clustersNew;
                c--;
            }
        }
        for (int c = 0; c < outdeg; c++) {
            children.elementAt(c).postprocess();
        }
        return this;
    }

    public DiGraph cloneDiGraph() {
        if (visited) {
            return clone;
        }
        visited = true;
        DiGraph G = new DiGraph(edgenumber.length);
        G.outdeg = outdeg;
        G.indeg = indeg;
        G.label = label;
        // G.retnumber = retnumber;
        System.arraycopy(edgenumber, 0, G.edgenumber, 0, outdeg);
        //for (int d = 0; d < outdeg; d++) {
        // G.clusters[d] = clusters[d];
        // G.edgenumber[d] = edgenumber[d];
        // G.retside[d] = retside[d];
        // G.switched[d] = switched[d];
        // }

        // G.number = number;
        G.children = new Vector();
        for (int c = 0; c < outdeg; c++) {
            G.children.add(children.elementAt(c).cloneDiGraph());
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
            n += children.elementAt(c).numberEdges(num);
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
            if (children.elementAt(c).indeg > 1) {
                // this child is a reticulation
                if (children.elementAt(c).visited) {
                    retside[c] = 1;
                } else {
                    retside[c] = 0;
                    children.elementAt(c).retnumber = num[0];
                    num[0]++;
                }
            } else {
                retside[c] = 2;
            }
            children.elementAt(c).numberRetEdges(num);
        }
    }

    public boolean displays(ClusterSet CS, int k, boolean checkfortrees) {
        if (checkfortrees) {
            return displaysTrees(CS, k);
        } else {
            return displays(CS, k);
        }
    }

    public boolean displays(ClusterSet CS, int k) {
        boolean disp = true;
        // number all reticulation edges
        int num[] = new int[1];
        num[0] = 0;
        numberRetEdges(num);
        cleanDiGraph();
        // update all clusters
        boolean[] retOn = new boolean[k];
        clearClusters();
        loopRet(CS, k - 1, retOn);
        switchAllOn();
        cleanDiGraph();
        // check if all clusters are displayed
        for (int c = 0; c < CS.clusterVec.size(); c++) {
            Vector cluster = (Vector) CS.clusterVec.elementAt(c);
            if (!clusterDisplayed(cluster)) {
                disp = false;
            }
            cleanDiGraph();
        }
        return disp;
    }

    public boolean displaysTrees(ClusterSet CS, int k) {

        Vector allTreeNums = CS.updateAllTreeNums();
        // number all reticulation edges
        int num[] = new int[1];
        num[0] = 0;
        numberRetEdges(num);
        cleanDiGraph();

        // loop through all trees
        for (int t = 0; t < allTreeNums.size(); t++) {
            int treenum = (Integer) allTreeNums.elementAt(t);

            // recursively loop through parent maps and check if at least one of the parent maps displays the tree
            boolean[] retOn = new boolean[k];
            if (!displaysTree(CS, k - 1, retOn, treenum)) {
                // update all clusters
                retOn = new boolean[k];
                clearClusters();
                loopRet(CS, k - 1, retOn);
                switchAllOn();
                cleanDiGraph();
                return false;
            }
        }
        // update all clusters
        boolean[] retOn = new boolean[k];
        clearClusters();
        loopRet(CS, k - 1, retOn);
        switchAllOn();
        cleanDiGraph();
        return true;
    }

    public boolean displaysTree(ClusterSet CS, int i, boolean[] retOn, int tree) {
        if (i < 0) {
            // retOn has been filled out
            // switch the reticulation edges on/off based on retOn
            switchRet(retOn);
            cleanDiGraph();
            // label the edges by the clusters that come from the specified tree
            clearClusters();
            updateClustersTree(CS, tree);
            return displaysClustersTree(CS, tree);
        } else {
            // fill out the i-th position of retOn and recurse
            retOn[i] = true;
            boolean out1 = displaysTree(CS, i - 1, retOn, tree);
            retOn[i] = false;
            boolean out2 = displaysTree(CS, i - 1, retOn, tree);
            return out1 | out2;
        }
    }

    public Vector updateClustersTree(ClusterSet CS, int tree) {
        // returns the cluster displayed by the incoming edge
        Vector cluster = new Vector();
        if (outdeg == 0) {
            // this is a leaf
            // return a singleton cluster
            if (label != DUMMY_NUMBER) {
                cluster.add(label);
            }
            return cluster;
        }
        for (int c = 0; c < outdeg; c++) {
            if (!switched[c]) {
                // this reticulation edge has not been switched on
                continue;
            }
            Vector childCluster = children.elementAt(c).updateClustersTree(CS, tree);
            childCluster = ClusterSet.sortCluster(childCluster);
            // check that this is not a reticulation edge
            // abd that this cluster is contained in CS and comes from the specified tree
            if (children.elementAt(c).indeg < 2 & CS.contains(childCluster, tree)) {
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

    public void switchAllOn() {
        // switches all edges on
        if (visited) {
            return;
        }
        visited = true;
        for (int c = 0; c < outdeg; c++) {
            switched[c] = true;
            children.elementAt(c).switchAllOn();
        }
    }

    public boolean displaysClustersTree(ClusterSet CS, int tree) {
        for (int c = 0; c < CS.clusterVec.size(); c++) {
            Vector cluster = (Vector) CS.clusterVec.elementAt(c);

            // singletons are always displayed
            if (cluster.size() < 2) continue;

            // only check clusters that come from the given tree
            Vector treeNums = (Vector) CS.treeNumberVec.elementAt(c);
            if (!treeNums.contains(tree)) continue;

            boolean cd = clusterDisplayed(cluster);
            cleanDiGraph();
            if (!cd) {
                return false;
            }
        }
        return true;
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
            if (children.elementAt(c).clusterDisplayed(cluster)) {
                // the cluster is displayed by some edge reachable form this vertex
                return true;
            }
        }
        return false;
    }

    public void clearClusters() {
        // resets all clusters
        for (int c = 0; c < outdeg; c++) {
            clusters[c].removeAllElements();
            children.elementAt(c).clearClusters();
        }
    }

    public void loopRet(ClusterSet CS, int i, boolean[] retOn) {
        if (i < 0) {
            // retOn has been filled out
            // switch the reticulation edges on/off based on retOn
            switchRet(retOn);
            cleanDiGraph();
            // update clusters
            updateClusters(CS);
            cleanDiGraph();
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
                switched[c] = retOn[children.elementAt(c).retnumber];
            }
            if (retside[c] == 1) {
                // right parent is on the right side of the reticulation
                switched[c] = !retOn[children.elementAt(c).retnumber];
            }
            children.elementAt(c).switchRet(retOn);
        }
    }

    public Vector updateClusters(ClusterSet CS) {
        // returns the cluster displayed by the incoming edge
        Vector cluster = new Vector();
        if (outdeg == 0) {
            // this is a leaf
            // return a singleton cluster
            if (label != DUMMY_NUMBER) {
                cluster.add(label);
            }
            return cluster;
        }
        for (int c = 0; c < outdeg; c++) {
            if (!switched[c]) {
                // this reticulation edge has not been switched on
                continue;
            }
            Vector childCluster = children.elementAt(c).updateClusters(CS);
            childCluster = ClusterSet.sortCluster(childCluster);
            // check if this is not a reticulation edge
            // and that this cluster is not yet in the cluster set of the edge
            // and that this cluster is contained in CS
            if (children.elementAt(c).indeg < 2 & !clusters[c].contains(childCluster) & CS.contains(childCluster)) {
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
                v.children.add(w);
                v.children.add(r);
                v.indeg = 1;
                v.outdeg = 2;
                w.children.add(children.elementAt(d));
                w.children.add(r);
                w.indeg = 1;
                w.outdeg = 2;
                children.setElementAt(v, d);
                // no need to recurse
            } else {
                // if this edge is either equal to e1 or to e2, but not to both, we subdivide it once
                if (edgenumber[d] == e1 || edgenumber[d] == e2) {
                    // subdivide this edge by a new vertex v
                    v = new DiGraph();
                    v.children.add(children.elementAt(d));
                    v.indeg = 1;
                    // and add an edge from v to r
                    v.children.add(r);
                    v.outdeg = 2;
                    children.setElementAt(v, d);
                    if (v.children.elementAt(0).outdeg > 0) {
                        v.children.elementAt(0).hangBelowEdges(r, e1, e2);
                    }
                }
            }
            if (edgenumber[d] != e2 && edgenumber[d] != e1) {
                children.elementAt(d).hangBelowEdges(r, e1, e2);
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
        if (label != 0) {
            // this is a leaf
            number = label;
            String taxon = (String) CassAlgorithm.stringTaxa.elementAt(number - 1);
            System.out.println(number + " [shape=" + CassAlgorithm.leafShape + ", width=0.3, label=" + taxon + "];");
        } else {
            // this is an internal vertex
            if (CassAlgorithm.PRINT_EXTRA_INFO) {
                number = num[0];
                System.out.println(number + " [shape=circle, label=\"" + indeg + "," + outdeg + "\"];");
                for (int c = 0; c < outdeg; c++) {
                    num[0]++;
                    children.elementAt(c).printNode(num);
                }
            } else {
                number = num[0];
                System.out.println(number + " [shape=point];");
                for (int c = 0; c < outdeg; c++) {
                    num[0]++;
                    children.elementAt(c).printNode(num);
                }
            }
        }
    }

    public void printArcs(boolean printClusters, boolean colourEdges) {
        for (int c = 0; c < outdeg; c++) {
            // create edgelabel containing all clusters displayed by this edge
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
                        Integer taxon = (Integer) cluster.elementAt(i);
                        String taxonString = (String) CassAlgorithm.stringTaxa.elementAt(taxon - 1);
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
            if (!printClusters) {
                edgeLabel = "";
            }
            String edgeColour = "black";
            if (children.elementAt(c).indeg > 1 && colourEdges) {
                edgeColour = "red";
            }

            System.out.println(number + " -> " + children.elementAt(c).number + "[label=\"" + edgeLabel + "\", color=" + edgeColour + "]");
        }
        visited = true;
        for (int c = 0; c < outdeg; c++) {
            if (!children.elementAt(c).visited) {
                children.elementAt(c).printArcs(printClusters, colourEdges);
            }
        }
    }

    public void cleanDiGraph() {
        visited = false;
        number = 0;
        MAX_RET = 0;
        for (int c = 0; c < outdeg; c++) {
            children.elementAt(c).cleanDiGraph();
        }
    }

    public void setIndegrees() {
        for (int c = 0; c < children.size(); c++) {
            DiGraph child = children.elementAt(c);
            child.indeg++;
            if (!child.visited) {
                child.visited = true;
                child.setIndegrees();
            }
        }
    }

    public void resetIndegrees() {
        indeg = 0;
        for (int c = 0; c < children.size(); c++) {
            children.elementAt(c).resetIndegrees();
        }
    }
/*
    public String toString() {
        String output;
        // returns eNewick string of the network
        if (label.intValue() != 0) {
            return label.toString();
        }

        String childString1 = ((DiGraph) children.elementAt(0)).toString();

        if (indeg > 1) {
            // reticulation
            if (visited) {
                output = "#H" + number;
            } else {
                MAX_RET++;
                number = MAX_RET;
                visited = true;
                output = "(" + childString1 + ")" + "#H" + number;
            }
            return output;
        }

        output = "(" + childString1;

        for (int i = 1; i < children.size(); i++) {
            output += "," + ((DiGraph) children.elementAt(i)).toString();
        }
        output += ")";
        if (indeg == 0) {
            output += "root;"; // root labelling for http://darwin.uvigo.es/software/nettest/
        }
        return output;

    }
    */
}
