/*
 * ClusterSet.java Copyright (C) 2022
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
import java.util.List;
import java.util.Vector;

/**
 * a set of clusters
 * Leo j.j.v. Iersel, 2012
 */
public class ClusterSet {

    public Vector<Vector<Integer>> clusterVec; // the clusters
    public Vector<Integer> taxa; // all taxa
    public Vector<Vector<Integer>> taxaInTaxa; // the taxa that have been collapsed into each taxon
    public Vector<Vector<Vector<Integer>>> treeNumberVec; // the numbers of the trees that each cluster is in
    public Vector<Integer> allTreeNums; // all the numbers of the trees, over all clusters

    public ClusterSet() {
        clusterVec = new Vector();
        taxa = new Vector();
        taxaInTaxa = new Vector();
        treeNumberVec = new Vector();
        allTreeNums = new Vector();
    }

    public Vector updateAllTreeNums() {
        allTreeNums = new Vector<>();
        for (int c = 0; c < clusterVec.size(); c++) {
            Vector vec = treeNumberVec.elementAt(c);
            for (int i = 0; i < vec.size(); i++) {
                Integer x = (Integer) vec.elementAt(i);
                if (!allTreeNums.contains(x)) {
                    allTreeNums.add(x);
                }
            }
        }
        return allTreeNums;
    }

    public static Vector sortCluster(Vector<Integer> cluster) {
        for (int i = 0; i < cluster.size() - 1; i++) {
            for (int j = i + 1; j < cluster.size(); j++) {
                Integer x = cluster.elementAt(i);
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

    public void print() {
        System.err.println("Cluster Set:");
        for (int c = 0; c < clusterVec.size(); c++) {
            Vector cluster = (Vector) clusterVec.elementAt(c);
            System.err.println(cluster.toString());
        }
    }

    /*
    public ClusterSet remLeaf(Integer x) {
        ClusterSet CS = this.cloneClusterSet();
        //System.err.println("Removing element " + x);
        Vector clusters = CS.clusterVec;
        Vector treeNums = CS.treeNumberVec;
        int index = CS.taxa.indexOf(x);
        CS.taxa.removeElementAt(index);
        CS.taxaInTaxa.removeElementAt(index);
        for (int c = 0; c < clusters.size(); c++) {
            //CS.print();
            Vector cluster = (Vector) clusters.elementAt(c);
            cluster.removeElement(x);
            if(cluster.size() == 0) {
                clusters.removeElementAt(c);
                treeNums.removeElementAt(c);
                c--;
                continue;
            }
            int i = clusters.indexOf(cluster);
            int j = clusters.lastIndexOf(cluster);
            if(i!=c){
                // merge the two copies of the cluster
                Vector treesc = (Vector<Integer>) treeNums.elementAt(c);
                Vector treesi = (Vector<Integer>) treeNums.elementAt(i);
                for(int t = 0; t < treesc.size(); t++) {
                    Integer treenum = (Integer) treesc.elementAt(t);
                    if(!treesi.contains(treenum)) {
                        treesi.add(treenum);
                    }
                }
                clusters.removeElementAt(c);
                treeNums.removeElementAt(c);
                c--;
                continue;
            }
            if (c!=j) {
                // merge the two copies of the cluster
                Vector treesc = (Vector<Integer>) treeNums.elementAt(c);
                Vector treesj = (Vector<Integer>) treeNums.elementAt(j);
                for(int t = 0; t < treesc.size(); t++) {
                    Integer treenum = (Integer) treesc.elementAt(t);
                    if(!treesj.contains(treenum)) {
                        treesj.add(treenum);
                    }
                }
                clusters.removeElementAt(c);
                treeNums.removeElementAt(c);
                c--;
            }
        }
        //CS.print();
        return CS;
    }
    */

    public ClusterSet remLeaf(Integer x) {
        ClusterSet CS = new ClusterSet();
        for (int c = 0; c < clusterVec.size(); c++) {
            Vector cluster = (Vector) clusterVec.elementAt(c);
            Vector cluster2 = new Vector(0);
            for (int i = 0; i < cluster.size(); i++) {
                Integer y = (Integer) cluster.elementAt(i);
                if (!y.equals(x)) {
                    cluster2.add(y);
                }
            }
            if (cluster2.size() == 0) {
                continue;
            }
            CS.addCluster(cluster2, (Vector) treeNumberVec.elementAt(c));
        }
        return CS;
    }

    public boolean contains(Vector cluster) {
        return clusterVec.contains(cluster);
    }

    public boolean contains(Vector cluster, int tree) {
        if (!clusterVec.contains(cluster)) {
            return false;
        }
        int index = clusterVec.indexOf(cluster);
        Vector treeNumbers = (Vector) treeNumberVec.elementAt(index);
        return treeNumbers.contains(tree);
    }

    public ClusterSet cloneClusterSet() {
        ClusterSet CS = new ClusterSet();
        CS.clusterVec = (Vector) clusterVec.clone();
        CS.treeNumberVec = (Vector) treeNumberVec.clone();
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
                Integer x = CS.taxa.elementAt(i);
                Integer y = CS.taxa.elementAt(j);
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
                    while (c < CS.clusterVec.size()) {
                        Vector cluster = (Vector) CS.clusterVec.elementAt(c);
                        if (cluster.contains(y)) {
                            // remove y from cluster
                            Vector cluster2 = (Vector) cluster.clone();
                            cluster2.remove(y);
                            if (cluster2.size() == 0 | CS.contains(cluster2)) {
                                // remove cluster
                                CS.clusterVec.removeElementAt(c);
                                CS.treeNumberVec.removeElementAt(c);
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
                    j = i + 1;
                }
            }
        }
        output.add(CS);
        output.add(collapsedTaxa);
        output.add(collapsedTaxaSets);
        return output;
    }

    public boolean separated(Integer x, Integer y) {
        boolean sep = false;
        for (int c = 0; c < clusterVec.size(); c++) {
            Vector cluster = (Vector) clusterVec.elementAt(c);
            if (cluster.size() == 1) {
                continue;
            }
            boolean a = cluster.contains(x);
            boolean b = cluster.contains(y);
            if ((a && !b) || (b && !a)) {
                sep = true;
            }
        }
        return sep;
    }

    public ClusterSet restrict(Vector cluster) {
        ClusterSet C = new ClusterSet();
        for (int c = 0; c < clusterVec.size(); c++) {
            Vector cluster2 = (Vector) clusterVec.elementAt(c);
            if (ClusterSet.getRelation(cluster, cluster2) == 4) {
                C.addCluster(cluster2, (Vector) treeNumberVec.elementAt(c));
            }
        }
        return C;
    }

    // only for stand alone version
    public void addCluster(Vector cluster) {
        for (int i = 0; i < cluster.size(); i++) {
            Vector singleton = new Vector(0);
            Integer taxon = (Integer) cluster.elementAt(i);
            singleton.add(taxon);
            if (!taxa.contains(taxon)) {
                clusterVec.add(singleton);
                treeNumberVec.add(new Vector());
                taxa.add(taxon);
                taxaInTaxa.add(singleton);
            }
        }
        cluster = ClusterSet.sortCluster(cluster);
        if (!clusterVec.contains(cluster)) {
            clusterVec.add(cluster);
            treeNumberVec.add(new Vector());
        }
    }

    public void addCluster(Vector cluster, List<Integer> treeNumbers) {

        // add tree numbers to allTreeNumbers
        Vector numVec = new Vector();
        for (Integer treeNumber : treeNumbers) {
            Integer treenum = treeNumber;
            numVec.add(treenum);
            if (!allTreeNums.contains(treenum)) {
                allTreeNums.add(treenum);
            }
        }

        for (int i = 0; i < cluster.size(); i++) {
            Vector singleton = new Vector(0);
            Integer taxon = (Integer) cluster.elementAt(i);
            singleton.add(taxon);
            if (!taxa.contains(taxon)) {
                clusterVec.add(singleton);
                treeNumberVec.add(new Vector());
                taxa.add(taxon);
                taxaInTaxa.add(singleton);
            }
        }
        cluster = ClusterSet.sortCluster(cluster);
        if (!clusterVec.contains(cluster)) {
            // add the cluster
            clusterVec.add(cluster);
            treeNumberVec.add(numVec);
        } else {
            // only add the tree numbers of the new cluster to the existing cluster
            int index = clusterVec.indexOf(cluster);
            Vector treenums = (Vector) treeNumberVec.elementAt(index);
            for (int i = 0; i < numVec.size(); i++) {
                Integer treenum = (Integer) numVec.elementAt(i);
                if (!treenums.contains(treenum)) {
                    treenums.add(treenum);
                }
            }
        }

    }

    /*
    public void addCluster(Integer cluster[]) {
        Vector vecCluster = new Vector();
        for (int i = 0; i < cluster.length; i++) {
            vecCluster.add(cluster[i]);
            Vector singleton = new Vector(0);
            singleton.add(cluster[i]);
            if (!taxa.contains(cluster[i])) {
                clusterVec.add(singleton);
                taxa.add(cluster[i]);
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
    */

    public static int getRelation(Vector c1, Vector c2) {
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
