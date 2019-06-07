/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import jloda.graph.Node;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Given two rooted, bifurcating phylogenetic trees T1 and T2 and a set of
 * subtrees F of T1, this functions tests whether there exist an acyclic
 * ordering of F which holds for both tree.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class AcyclicCheck {

    Vector<HybridTree> illegalTrees = new Vector<>();

    public boolean run(Vector<HybridTree> forest, HybridTree t1, HybridTree t2) {

        Hashtable<HybridTree, BitSet> treeToLCA = new Hashtable<>();
        Hashtable<BitSet, HybridTree> LCAtoTree = new Hashtable<>();
        Vector<BitSet> LCAclusters = new Vector<>();

        for (HybridTree f : forest) {
            if (f.getNumberOfNodes() != 1) {
                BitSet fCluster = f.getNodeToCluster().get(f.getRoot());

                // finding the node in t1 representing the root of f
                Node v1 = t1.findLCA(fCluster);
                BitSet v1Cluster = t1.getNodeToCluster().get(v1);

                LCAclusters.add(v1Cluster);
                LCAtoTree.put(v1Cluster, f);

                // finding the node in t2 representing the root of f
                Node v2 = t2.findLCA(fCluster);
                BitSet v2Cluster = t2.getNodeToCluster().get(v2);
                treeToLCA.put(f, v2Cluster);
            }
        }

        // create pairs, first tree is a subtree of the second tree in t1
        Vector<HybridTree[]> pairs = new Vector<>();
        for (int i = 0; i < LCAclusters.size() - 1; i++) {
            BitSet b1 = LCAclusters.get(i);
            for (int j = i + 1; j < LCAclusters.size(); j++) {
                BitSet b2 = LCAclusters.get(j);
                BitSet test1 = (BitSet) b1.clone();
                test1.and(b2);
                BitSet test2 = (BitSet) b2.clone();
                test2.and(b1);
                if (test1.equals(b1)) {
                    HybridTree[] pair = {LCAtoTree.get(b1), LCAtoTree.get(b2)};
                    pairs.add(pair);
                } else if (test2.equals(b2)) {
                    HybridTree[] pair = {LCAtoTree.get(b2), LCAtoTree.get(b1)};
                    pairs.add(pair);
                }
            }
        }

        // check if each pairs holds in t2
        for (HybridTree[] pair : pairs) {
            BitSet b1 = treeToLCA.get(pair[0]);
            BitSet b2 = treeToLCA.get(pair[1]);
            BitSet test = (BitSet) b1.clone();
            test.and(b2);
            if (test.cardinality() != 0 && !test.equals(b1))
                return false;
        }

        return true;
    }

}
