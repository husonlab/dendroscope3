/*
 *   TreeToForestAdaptor.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.cmpAllMAAFs;

import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.model.treeObjects.SparseTree;
import dendroscope.hybroscale.model.treeObjects.SparseTreeNode;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

import java.util.*;

public class TreeToForestAdaptor {

    private Vector<String> taxaOrdering;
    // private BitSet insertedSet;

    private HybridTree t;
    private Vector<SparseTree> orderedForest;

    public Vector<HybridTree> runRec(Vector<SparseTree> forest, HybridTree t, boolean debug, Vector<String> taxaOrdering) {

        this.t = t;
        this.taxaOrdering = taxaOrdering;

//		Vector<EasyTree> easyForest = new Vector<EasyTree>();
//		for (SparseTree c : forest)
//			easyForest.add(new EasyTree(c.getPhyloTree()));
//		Vector<EasyTree> acyclicOrder = new FastAcyclicCheck().run(easyForest, new EasyTree(t), null, taxaOrdering,
//				null, false, false);
//		orderedForest = new Vector<SparseTree>();
//		for (EasyTree c : acyclicOrder)
//			orderedForest.add(new SparseTree(c.getPhyloTree()));

        orderedForest = new Vector<SparseTree>();
        for (SparseTree c : forest)
            orderedForest.add(new SparseTree(c.getPhyloTree()));

        HybridTree tF = new HybridTree(orderedForest.lastElement().getPhyloTree(), false, taxaOrdering);
        tF.update();
        updateInsertedSet(tF);

        HashSet<MyNode> compRoots = new HashSet<MyNode>();
        Vector<HybridTree> trees = new Vector<HybridTree>();

        addComponentsRec(orderedForest.size() - 2, tF, trees, compRoots);

        return trees;

    }

    private void addComponentsRec(int i, HybridTree tF, Vector<HybridTree> trees, HashSet<MyNode> compRoots) {

        if (i >= 0) {

            BitSet insertedSet = updateInsertedSet(tF);
            SparseTree c = orderedForest.get(i);

            BitSet cCluster = getTreeCluster(c);
            MyNode v = t.findLCA(cCluster);

            BitSet sibCluster = (BitSet) t.getNodeToCluster().get(v).clone();
            sibCluster.and(insertedSet);
            while (sibCluster.isEmpty()) {
                v = v.getFirstInEdge().getSource();
                sibCluster = (BitSet) t.getNodeToCluster().get(v).clone();
                sibCluster.and(insertedSet);
            }

            MyNode vSib = tF.findLCA(sibCluster);
            MyNode vT = t.findLCA(sibCluster);
            BitSet b = (BitSet) t.getNodeToCluster().get(vT).clone();
            b.and(cCluster);

            if (!b.equals(cCluster) || compRoots.contains(vSib)) {

                // if (b.equals(cCluster) && vSib.getOutDegree() != 0){ // &&
                // c.getLeaves().size() == 1) {
                //
                // //
                // System.out.println(" >"+c.getPhyloTree()+" "+vSib.getLabel());
                //
                // Vector<MyNode[]> nodePairs = new Vector<MyNode[]>();
                // MyNode[] nodePair = { vSib, null };
                // nodePairs.add(nodePair);
                // for (MyNode compRoot : compRoots) {
                // MyNode[] nodePair2 = { compRoot, null };
                // nodePairs.add(nodePair2);
                // }
                //
                // HybridTree tFCopy = new HybridTree(new MyPhyloTree(), false,
                // taxaOrdering);
                // tFCopy.getRoot().setLabel(tF.getRoot().getLabel());
                // copyTreeRec(tFCopy, tF.getRoot(), tFCopy.getRoot(),
                // nodePairs, new Vector<MyEdge[]>());
                //
                // MyNode vSibCopy = nodePairs.get(0)[1];
                // HashSet<MyNode> compRootsCopy = new HashSet<MyNode>();
                // for (int k = 1; k < nodePairs.size(); k++)
                // compRootsCopy.add(nodePairs.get(k)[1]);
                //
                // MyNode v3 = tFCopy.newNode();
                // tFCopy.newEdge(vSibCopy, v3);
                // HybridTree subtree = new HybridTree(c.getPhyloTree(), false,
                // taxaOrdering);
                // addSubtree(subtree.getRoot(), v3, tFCopy);
                // compRootsCopy.add(v3);
                // tFCopy.update();
                //
                // //
                // System.out.println(tF.toMyBracketString()+"\n"+tFCopy.toMyBracketString());
                //
                // int newI = i - 1;
                // addComponentsRec(newI, tFCopy, trees, compRootsCopy);
                //
                // }

                HybridTree tFCopy = new HybridTree(new MyPhyloTree(), false, taxaOrdering);
                tFCopy.getRoot().setLabel(tF.getRoot().getLabel());
                Vector<MyNode[]> nodePairs = new Vector<MyNode[]>();
                MyNode[] nodePair = {vSib, null};
                nodePairs.add(nodePair);
                for (MyNode compRoot : compRoots) {
                    if (compRoot.getOwner().equals(tF)) {
                        MyNode[] nodePairC = {compRoot, null};
                        nodePairs.add(nodePairC);
                    }
                }
                copyTreeRec(tFCopy, tF.getRoot(), tFCopy.getRoot(), nodePairs, null);
                MyNode vSibCopy = nodePairs.get(0)[1];

                MyNode v1 = vSibCopy.getFirstInEdge().getSource();
                tFCopy.deleteEdge(vSibCopy.getFirstInEdge());
                MyNode v2 = tFCopy.newNode();
                MyNode v3 = tFCopy.newNode();
                tFCopy.newEdge(v1, v2);
                tFCopy.newEdge(v2, vSibCopy);
                tFCopy.newEdge(v2, v3);
                HybridTree subtree = new HybridTree(c.getPhyloTree(), false, taxaOrdering);
                addSubtree(subtree.getRoot(), v3, tFCopy);
                compRoots.add(v3);
                for (int k = 1; k < nodePairs.size(); k++) {
                    compRoots.add(nodePairs.get(k)[1]);
                }
                tFCopy.update();

                int newI = i - 1;
                addComponentsRec(newI, tFCopy, trees, compRoots);

            }
//			 else {

            if (b.equals(cCluster)) {

                MyNode v3 = tF.newNode();
                tF.newEdge(vSib, v3);
                HybridTree subtree = new HybridTree(c.getPhyloTree(), false, taxaOrdering);
                addSubtree(subtree.getRoot(), v3, tF);
                compRoots.add(v3);
                tF.update();

                Iterator<MyEdge> it = v.outEdges().iterator();
                Vector<BitSet> sibClusters = new Vector<BitSet>();
                while (it.hasNext()) {
                    BitSet cluster = (BitSet) t.getNodeToCluster().get(it.next().getTarget()).clone();
                    cluster.and(insertedSet);
                    if (!cluster.isEmpty())
                        sibClusters.add(cluster);
                }
                HashMap<HybridTree, HashSet<MyNode>> treeToRoots = new HashMap<HybridTree, HashSet<MyNode>>();
                addToChildren(tF, vSib, v3, sibClusters, insertedSet, compRoots, treeToRoots);

                for (HybridTree tFRec : treeToRoots.keySet()) {
                    int newI = i - 1;
                    addComponentsRec(newI, tFRec, trees, treeToRoots.get(tFRec));
                }

                int newI = i - 1;
                addComponentsRec(newI, tF, trees, compRoots);

            }

            // int newI = i - 1;
            // addComponentsRec(newI, tF, trees, compRoots);

        } else
            trees.add(tF);

    }

    private void addToChildren(HybridTree tF, MyNode p, MyNode v, Vector<BitSet> sibClusters, BitSet insertedSet,
                               HashSet<MyNode> compRoots, HashMap<HybridTree, HashSet<MyNode>> treeToRoots) {
        Iterator<MyEdge> it = p.outEdges().iterator();
        while (it.hasNext()) {
            MyNode c = it.next().getTarget();
            if (c != v) {
                Iterator<MyEdge> itC = c.outEdges().iterator();
                boolean isSourceNode = true;
                while (itC.hasNext()) {
                    MyNode cC = itC.next().getTarget();
                    BitSet cCluster = tF.getNodeToCluster().get(cC);
                    cCluster.and(insertedSet);
                    if (!sibClusters.contains(cCluster)) {
                        isSourceNode = false;
                        break;
                    }
                }
                if (isSourceNode) {
                    Vector<MyNode[]> nodePairs = new Vector<MyNode[]>();
                    MyNode[] pPair = {p, null};
                    nodePairs.add(pPair);
                    MyNode[] cPair = {c, null};
                    nodePairs.add(cPair);
                    MyNode[] vPair = {v, null};
                    nodePairs.add(vPair);
                    for (MyNode r : compRoots) {
                        MyNode[] rootPair = {r, null};
                        nodePairs.add(rootPair);
                    }
                    Vector<MyEdge[]> edgePairs = new Vector<MyEdge[]>();
                    MyEdge[] edgePair = {v.getFirstInEdge(), null};
                    edgePairs.add(edgePair);

                    HybridTree tFCopy = new HybridTree(new MyPhyloTree(), false, taxaOrdering);
                    tFCopy.getRoot().setLabel(tF.getRoot().getLabel());
                    copyTreeRec(tFCopy, tF.getRoot(), tFCopy.getRoot(), nodePairs, edgePairs);

                    MyNode cCopy = nodePairs.get(1)[1];
                    MyNode vCopy = nodePairs.get(2)[1];
                    HashSet<MyNode> compRootsCopy = new HashSet<MyNode>();
                    for (int i = 3; i < nodePairs.size(); i++)
                        compRootsCopy.add(nodePairs.get(i)[1]);
                    MyEdge eCopy = edgePairs.get(0)[1];
                    tFCopy.deleteEdge(eCopy);
                    // System.out.println(eCopy.getSource().getLabel()+" -> "+eCopy.getTarget().getLabel()+" "+eCopy.getOwner().equals(tFCopy));
                    tFCopy.deleteEdge(eCopy);

                    // System.out.println(tFCopy+";");

                    // tFCopy.deleteNode(vCopy);
                    tFCopy.newEdge(cCopy, vCopy);

                    // System.out.println("\n"+tF+";\n"+tFCopy+";");

                    // treeToRoots.put(tFCopy, compRootsCopy);
                }
            }
        }
    }

    private void copyTreeRec(HybridTree tCopy, MyNode v, MyNode vCopy, Vector<MyNode[]> nodePairs,
                             Vector<MyEdge[]> edgePairs) {
        // System.out.println(tCopy+";");
        for (MyNode[] nodePair : nodePairs) {
            if (nodePair[0].equals(v))
                nodePair[1] = vCopy;
        }
        Iterator<MyEdge> it = v.outEdges().iterator();
        while (it.hasNext()) {
            MyEdge e = it.next();
            MyNode c = e.getTarget();
            MyNode cCopy = tCopy.newNode(c);
            MyEdge eCopy = tCopy.newEdge(vCopy, cCopy);
            copyTreeRec(tCopy, c, cCopy, nodePairs, edgePairs);
            eCopy.setInfo(e.getInfo());
            eCopy.setLabel(e.getLabel());
            if (edgePairs != null) {
                for (MyEdge[] edgePair : edgePairs) {
                    if (edgePair[0].equals(e))
                        edgePair[1] = eCopy;
                }
            }
        }
    }

    public HybridTree run(Vector<SparseTree> forest, HybridTree t, boolean debug, Vector<String> taxaOrdering) {

        Vector<EasyTree> easyForest = new Vector<EasyTree>();
        for (SparseTree c : forest) {
            easyForest.add(new EasyTree(c.getPhyloTree()));
        }
        Vector<EasyTree> acyclicOrder = new FastAcyclicCheck().run(easyForest, new EasyTree(t), null, taxaOrdering,
                null, false, true);

        Vector<SparseTree> orderedForest = new Vector<SparseTree>();
        for (EasyTree c : acyclicOrder)
            orderedForest.add(new SparseTree(c.getPhyloTree()));

        this.taxaOrdering = taxaOrdering;
        BitSet insertedSet = new BitSet(taxaOrdering.size());
        t.update();

        HybridTree tF = new HybridTree(orderedForest.lastElement().getPhyloTree(), false, taxaOrdering);
        tF.update();
        insertedSet = updateInsertedSet(tF);

        HashSet<MyNode> compRoots = new HashSet<MyNode>();
        for (int i = orderedForest.size() - 2; i >= 0; i--) {

            SparseTree c = orderedForest.get(i);

            BitSet cCluster = getTreeCluster(c);
            MyNode v = t.findLCA(cCluster);

            BitSet sibCluster = (BitSet) t.getNodeToCluster().get(v).clone();
            sibCluster.and(insertedSet);
            while (sibCluster.isEmpty()) {
                v = v.getFirstInEdge().getSource();

                if (!t.getNodeToCluster().containsKey(v)) {
                    System.out.println("Error!");
                }

                sibCluster = (BitSet) t.getNodeToCluster().get(v).clone();
                sibCluster.and(insertedSet);
            }

            MyNode vSib = tF.findLCA(sibCluster);
            MyNode vT = t.findLCA(sibCluster);
            BitSet b = (BitSet) t.getNodeToCluster().get(vT).clone();
            b.and(cCluster);
            if (!b.equals(cCluster) || compRoots.contains(vSib)) {
                // System.out.println("A");
                MyNode v1 = vSib.getFirstInEdge().getSource();
                tF.deleteEdge(vSib.getFirstInEdge());
                MyNode v2 = tF.newNode();
                MyNode v3 = tF.newNode();
                tF.newEdge(v1, v2);
                tF.newEdge(v2, vSib);
                tF.newEdge(v2, v3);
                HybridTree subtree = new HybridTree(c.getPhyloTree(), false, taxaOrdering);
                addSubtree(subtree.getRoot(), v3, tF);
                compRoots.add(v3);
            } else {
                // System.out.println("B");
                MyNode v3 = tF.newNode();
                tF.newEdge(vSib, v3);
                HybridTree subtree = new HybridTree(c.getPhyloTree(), false, taxaOrdering);
                addSubtree(subtree.getRoot(), v3, tF);
                compRoots.add(v3);
            }

            tF.update();
            insertedSet = updateInsertedSet(tF);

            if (debug)
                System.out.println("tF: " + tF + ";");

        }

        return tF;
    }

    private void addSubtree(MyNode v, MyNode vCopy, HybridTree t) {
        vCopy.setLabel(v.getLabel());
        Iterator<MyEdge> it = v.outEdges().iterator();
        while (it.hasNext()) {
            MyNode c = it.next().getTarget();
            MyNode cCopy = t.newNode();
            t.newEdge(vCopy, cCopy);
            addSubtree(c, cCopy, t);
        }
    }

    private BitSet getTreeCluster(SparseTree c) {
        BitSet cluster = new BitSet(taxaOrdering.size());
        for (SparseTreeNode v : c.getLeaves())
            cluster.set(taxaOrdering.indexOf(v.getLabel()));
        return cluster;
    }

    private BitSet updateInsertedSet(HybridTree tF) {
        BitSet insertedSet = new BitSet(taxaOrdering.size());
        for (MyNode v : tF.getLeaves())
            insertedSet.set(taxaOrdering.indexOf(v.getLabel()));
        return insertedSet;
    }

}
