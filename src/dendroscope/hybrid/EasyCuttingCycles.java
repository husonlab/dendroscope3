/**
 * EasyCuttingCycles.java 
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
package dendroscope.hybrid;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class EasyCuttingCycles {

    private Vector<String> taxaOrdering;
    private final Hashtable<EasyNode, BitSet> t1NodeToCluster = new Hashtable<>();
    private final Hashtable<EasyNode, BitSet> t2NodeToCluster = new Hashtable<>();

    public void run(Vector<EasyTree> forest, EasyTree t1, EasyTree t2,
                    Vector<String> taxaOrdering) {

        this.taxaOrdering = taxaOrdering;
        initCluster(t1, t1NodeToCluster);
        initCluster(t2, t2NodeToCluster);

        // System.out.println("BEGIN - cutting cycles... "+forest.size());

        // System.out.println("Acyclic Check:");
        EasyTree[] cyclePair = getCycle(forest, t1, t2);
        while (cyclePair != null) {

            // System.out.println();
            // System.out.println(cyclePair[0].getPhyloTree());
            // System.out.println(cyclePair[1].getPhyloTree());

            if (cyclePair[0].getLeaves().size() <= cyclePair[1].getLeaves()
                    .size())
                cutCyclePair(cyclePair[0], forest, t1, t2);
            else
                cutCyclePair(cyclePair[1], forest, t1, t2);

            cyclePair = getCycle(forest, t1, t2);
        }

        // System.out.println("END - cutting cycles... "+forest.size());
    }

    private void cutCyclePair(EasyTree t, Vector<EasyTree> forest, EasyTree t1,
                              EasyTree t2) {
        EasyNode root = t.getRoot();
        EasyNode c1 = root.getChildren().get(0);
        EasyNode c2 = root.getChildren().get(1);
        EasyTree s1 = t.pruneSubtree(c1);
        EasyTree s2 = t.pruneSubtree(c2);
        forest.remove(t);
        forest.add(s1);
        forest.add(s2);
    }

    public EasyTree[] getCycle(Vector<EasyTree> forest, EasyTree t1, EasyTree t2) {

        Hashtable<EasyTree, BitSet> treeToLCA = new Hashtable<>();
        Hashtable<BitSet, EasyTree> LCAtoTree = new Hashtable<>();
        Vector<BitSet> LCAclusters = new Vector<>();

        for (EasyTree f : forest) {
            if (f.getNodes().size() != 1) {

                BitSet fCluster = getRootCluster(f);

                // finding the node in t1 representing the root of f
                EasyNode v1 = findLCA(t1, fCluster, t1NodeToCluster);
                BitSet v1Cluster = t1NodeToCluster.get(v1);

                LCAclusters.add(v1Cluster);
                LCAtoTree.put(v1Cluster, f);

                // finding the node in t2 representing the root of f
                EasyNode v2 = findLCA(t2, fCluster, t2NodeToCluster);
                BitSet v2Cluster = t2NodeToCluster.get(v2);
                treeToLCA.put(f, v2Cluster);
            }
        }

        // create pairs, first tree is a subtree of the second tree in t1
        Vector<EasyTree[]> pairs = new Vector<>();
        for (int i = 0; i < LCAclusters.size() - 1; i++) {
            BitSet b1 = LCAclusters.get(i);
            for (int j = i + 1; j < LCAclusters.size(); j++) {
                BitSet b2 = LCAclusters.get(j);
                BitSet test1 = (BitSet) b1.clone();
                test1.and(b2);
                BitSet test2 = (BitSet) b2.clone();
                test2.and(b1);
                if (test1.equals(b1)) {
                    EasyTree[] pair = {LCAtoTree.get(b1), LCAtoTree.get(b2)};
                    pairs.add(pair);
                } else if (test2.equals(b2)) {
                    EasyTree[] pair = {LCAtoTree.get(b2), LCAtoTree.get(b1)};
                    pairs.add(pair);
                }
            }
        }

        // check if each pairs holds in t2
        for (EasyTree[] pair : pairs) {
            BitSet b1 = treeToLCA.get(pair[0]);
            BitSet b2 = treeToLCA.get(pair[1]);
            BitSet test = (BitSet) b1.clone();
            test.and(b2);
            if (test.cardinality() != 0 && !test.equals(b1))
                return pair;
        }

        return null;
    }

    private EasyNode findLCA(EasyTree t, BitSet cluster,
                             Hashtable<EasyNode, BitSet> nodeToCluster) {
        Iterator<EasyNode> it = t.postOrderWalk();
        while (it.hasNext()) {
            EasyNode v = it.next();
            BitSet b1 = nodeToCluster.get(v);
            BitSet b2 = (BitSet) cluster.clone();
            b2.and(b1);
            if (b2.equals(cluster))
                return v;
        }
        return null;
    }

    private Hashtable<EasyNode, BitSet> initCluster(EasyTree t,
                                                    Hashtable<EasyNode, BitSet> nodeToCluster) {
        initClusterRec(t.getRoot(), new Vector<String>(), nodeToCluster);
        return null;
    }

    private Vector<String> initClusterRec(EasyNode v, Vector<String> taxa,
                                          Hashtable<EasyNode, BitSet> nodeToCluster) {
        BitSet b = new BitSet(taxaOrdering.size());
        if (v.getOutDegree() == 0)
            taxa.add(v.getLabel());
        else {
            Vector<String> v1 = initClusterRec(v.getChildren().get(0),
                    new Vector<String>(), nodeToCluster);
            Vector<String> v2 = initClusterRec(v.getChildren().get(1),
                    new Vector<String>(), nodeToCluster);
            for (String s : v1)
                taxa.add(s);
            for (String s : v2)
                taxa.add(s);
        }
        for (String s : taxa)
            b.set(taxaOrdering.indexOf(s));
        nodeToCluster.put(v, b);
        return taxa;
    }

    private BitSet getRootCluster(EasyTree t) {
        BitSet cluster = new BitSet(taxaOrdering.size());
        for (EasyNode v : t.getLeaves())
            cluster.set(taxaOrdering.indexOf(v.getLabel()));
        return cluster;
    }

}
