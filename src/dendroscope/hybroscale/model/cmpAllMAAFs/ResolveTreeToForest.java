/*
 *   ResolveTreeToForest.java Copyright (C) 2020 Daniel H. Huson
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

import dendroscope.hybroscale.model.cmpMinNetworks.NetworkIsomorphismCheck;
import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.model.treeObjects.SparseTree;
import dendroscope.hybroscale.model.treeObjects.SparseTreeNode;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;
import dendroscope.hybroscale.util.lcaQueries.LCA_Query;

import java.util.*;

public class ResolveTreeToForest {

    private Vector<String> taxaOrdering;
    private Vector<BitSet> compClusters = new Vector<BitSet>();
    private HybridTree tInit;
    private Vector<SparseTree> forest;

    private LCA_Query lcaQuery;
    private HashMap<BitSet, MyNode> setToLeaf;
    private HybridTree h;

    public Vector<HybridTree> run(Vector<SparseTree> forest, HybridTree h, HybridTree t, boolean debug,
                                  Vector<String> taxaOrdering) {

        this.taxaOrdering = taxaOrdering;
        this.tInit = t;
        this.forest = forest;

        this.h = h;
        lcaQuery = new LCA_Query(h);
        setToLeaf = new HashMap<BitSet, MyNode>();
        for (MyNode l : h.getLeaves())
            setToLeaf.put(getLeafSet(l), l);

        HybridTree tCopy = new HybridTree(t, false, taxaOrdering);
        tCopy.update();
        for (SparseTree c : forest)
            compClusters.add(getTreeCluster(c));

        Vector<HybridTree> outputTrees = new Vector<HybridTree>();
        resolveTreeRec(tCopy, outputTrees);

        Vector<MyPhyloTree> multipleTrees = new Vector<MyPhyloTree>();
        for (int i = 0; i < outputTrees.size() - 1; i++) {
            MyPhyloTree tree1 = outputTrees.get(i);
            SparseNetwork t1 = new SparseNetwork(tree1);
            for (int j = i + 1; j < outputTrees.size(); j++) {
                MyPhyloTree tree2 = outputTrees.get(j);
                SparseNetwork t2 = new SparseNetwork(tree2);
                if (!tree1.equals(tree2) && new NetworkIsomorphismCheck().run(t1, t2))
                    multipleTrees.add(tree2);
            }
        }

        for (MyPhyloTree mulTree : multipleTrees)
            outputTrees.remove(mulTree);

        return outputTrees;

    }

    private BitSet getLeafSet(MyNode leaf) {
        BitSet b = new BitSet(taxaOrdering.size());
        b.set(taxaOrdering.indexOf(leaf.getLabel()));
        return b;
    }

    private boolean isCompCluster(BitSet cluster) {
        BitSet b = (BitSet) cluster.clone();
        for (BitSet bC : compClusters) {
            if (bC.equals(b))
                return true;
            else if (b.intersects(bC)) {
                BitSet bCopy = (BitSet) b.clone();
                bCopy.or(bC);
                if (bCopy.equals(b))
                    b.xor(bC);
            }
        }
        return false;
    }

    private void resolveTreeRec(HybridTree t, Vector<HybridTree> outputTrees) {

        boolean isResolved = true;
        for (MyNode v : t.getNodes()) {

            boolean alreadyResolved = v.getInfo() != null;

            if (v.getOutDegree() > 2 && !alreadyResolved) {
                Iterator<MyEdge> it = v.outEdges().iterator();
                int counter = 0;
                while (it.hasNext()) {
                    MyNode c = it.next().getTarget();
                    BitSet bC = new BitSet(taxaOrdering.size());
                    getTreeCluster(c, bC);
                    // if (compClusters.contains(bC))
                    if (isCompCluster(bC))
                        counter++;
                }
                if (counter > 1) {
                    isResolved = false;
                    Vector<HybridTree> resolvedTrees = resolveNode(v, t);
                    for (HybridTree resNet : resolvedTrees)
                        resolveTreeRec(resNet, outputTrees);

                    break;
                }
            }
        }
        if (isResolved) {
            t.update();
            outputTrees.add(t);
        }

    }

    private Vector<HybridTree> resolveNode(MyNode v, HybridTree t) {

        Vector<HybridTree> subtrees = new Vector<HybridTree>();
        Vector<MyEdge> compEdges = new Vector<MyEdge>();
        Vector<MyEdge> toDelete = new Vector<MyEdge>();

        Iterator<MyEdge> it = v.outEdges().iterator();
        Vector<MyEdge> outEdges = new Vector<MyEdge>();
        while (it.hasNext())
            outEdges.add(it.next());
        Collections.sort(outEdges, new EdgeComparator());

        // while (it.hasNext()) {
        for (MyEdge outEdge : outEdges) {
            // MyEdge outEdge = it.next();
            BitSet bC = new BitSet(taxaOrdering.size());
            getTreeCluster(outEdge.getTarget(), bC);
            // if (compClusters.contains(bC) && subtrees.size() <
            // v.getOutDegree() - 1) {
            if (isCompCluster(bC)) {
                compEdges.add(outEdge);
                if (subtrees.size() < v.getOutDegree() - 1) {
                    subtrees.add(t.getSubtree(outEdge.getTarget(), true));
                    toDelete.add(outEdge);
                }
            }
        }

        for (MyEdge e : toDelete)
            t.deleteEdge(e);
        Vector<MyNode> nodes = new Vector<MyNode>();
        it = v.outEdges().iterator();
        while (it.hasNext()) {
            MyEdge e = it.next();
            MyNode w = e.getTarget();
            if (compEdges.contains(e))
                w.setInfo("1");
            else
                w.setInfo("2");
            nodes.add(w);
        }
        if (v.getOutDegree() > 1) {
            v.setInfo("1");
            nodes.add(v);
        } else
            removeOneNode(v, t);

        Vector<HybridTree> resolvedTrees = new Vector<HybridTree>();
        addSubtreesRec(t, nodes, subtrees, 0, resolvedTrees);

        for (HybridTree tRes : resolvedTrees) {
            for (MyNode vRes : tRes.getNodes())
                removeOneNode(vRes, tRes);
        }

        return resolvedTrees;
    }

    public class EdgeComparator implements Comparator<MyEdge> {

        @Override
        public int compare(MyEdge e1, MyEdge e2) {
            int outDeg1 = e1.getTarget().getOutDegree();
            int outDeg2 = e2.getTarget().getOutDegree();
            if (outDeg1 > outDeg2)
                return -1;
            else if (outDeg1 < outDeg2)
                return 1;
            return 0;
        }

    }

    private void removeOneNode(MyNode v, HybridTree t) {
        if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
            MyNode p = v.getFirstInEdge().getSource();
            MyNode c = v.getFirstOutEdge().getTarget();
            t.deleteEdge(v.getFirstInEdge());
            t.deleteEdge(v.getFirstOutEdge());
            t.newEdge(p, c);
        }
    }

    private void addSubtreesRec(HybridTree t, Vector<MyNode> nodes, Vector<HybridTree> subtrees, int index,
                                Vector<HybridTree> resolvedTrees) {
        if (index < subtrees.size()) {
            for (MyNode sib : nodes) {

                Vector<MyNode[]> nodePairs = new Vector<MyNode[]>();
                MyNode[] nodePair = {sib, null};
                nodePairs.add(nodePair);
                for (MyNode sibling : nodes) {
                    if (!sibling.equals(sib)) {
                        MyNode[] nodeSibPair = {sibling, null};
                        nodePairs.add(nodeSibPair);
                    }
                }

                HybridTree tCopy = copyTree(t, nodePairs);

                Vector<MyNode> nodesCopy = new Vector<MyNode>();
                MyNode sibCopy = nodePairs.get(0)[1];
                MyNode pCopy = sibCopy.getFirstInEdge().getSource();
                tCopy.deleteEdge(sibCopy.getFirstInEdge());
                MyNode xCopy = tCopy.newNode();
                MyNode rootCopy = tCopy.newNode();
                tCopy.newEdge(pCopy, xCopy);
                tCopy.newEdge(xCopy, sibCopy);
                tCopy.newEdge(xCopy, rootCopy);
                addSubtree(subtrees.get(index).getRoot(), rootCopy, tCopy);
                xCopy.setInfo("1");
                sibCopy.setInfo("1");
                nodesCopy.add(xCopy);
                nodesCopy.add(rootCopy);

                for (MyNode[] pair : nodePairs)
                    nodesCopy.add(pair[1]);
                int newIndex = index + 1;
                addSubtreesRec(tCopy, nodesCopy, subtrees, newIndex, resolvedTrees);

                if ((subtrees.get(index).getNodes().size() == 1 && sibCopy.getOutDegree() != 0 && sibCopy.getInfo() != null)
                        || (sib.getOutDegree() != 0 && sib.getInfo() != null && sib.getInfo().equals("2"))) {

                    HashSet<MyNode> leafSet1 = new HashSet<MyNode>();
                    getTreeChildren(subtrees.get(index).getRoot(), leafSet1);
                    HashSet<MyNode> leafSet2 = new HashSet<MyNode>();
                    getTreeChildren(sibCopy, leafSet2);

                    // MyNode lca1 = lcaQuery.cmpLCA(leafSet1);
                    // lca1 = lca1.getOutDegree() == 0 ?
                    // lca1.getFirstInEdge().getSource() : lca1;
                    // MyNode p1 = lca1.getFirstInEdge().getSource();

                    MyNode lca1 = lcaQuery.cmpLCA(leafSet1);
                    BitSet c1 = getTreeCluster(new SparseTree(subtrees.get(index)));
                    BitSet inserted = getTreeCluster(new SparseTree(t));
                    inserted.or(c1);
                    BitSet b = (BitSet) h.getNodeToCluster().get(lca1).clone();
                    b.and(inserted);
                    while (b.equals(c1)) {
                        lca1 = lca1.getFirstInEdge().getSource();
                        b = (BitSet) h.getNodeToCluster().get(lca1).clone();
                        b.and(inserted);
                    }

                    MyNode lca2 = lcaQuery.cmpLCA(leafSet2);
                    lca2 = lca2.getOutDegree() == 0 ? lca2.getFirstInEdge().getSource() : lca2;

                    if (lca1.equals(lca2)) {

                        Vector<MyNode> nodes2Copy = new Vector<MyNode>();
                        HybridTree t2Copy = copyTree(t, nodePairs);

                        rootCopy = t2Copy.newNode();
                        sibCopy = nodePairs.get(0)[1];
                        t2Copy.newEdge(sibCopy, rootCopy);
                        addSubtree(subtrees.get(index).getRoot(), rootCopy, t2Copy);
                        nodes2Copy.add(sibCopy);
                        nodes2Copy.add(rootCopy);
                        sibCopy.setInfo("1");

                        // System.out.println(t+";\n"+t2Copy+";\n");

                        for (MyNode[] pair : nodePairs)
                            nodes2Copy.add(pair[1]);
                        newIndex = index + 1;
                        addSubtreesRec(t2Copy, nodes2Copy, subtrees, newIndex, resolvedTrees);

                    }

                }

            }
        } else
            resolvedTrees.add(t);

    }

    private void getTreeChildren(MyNode v, HashSet<MyNode> leafSet) {
        if (v.getOutDegree() == 0)
            leafSet.add(setToLeaf.get(this.getLeafSet(v)));
        else {
            Iterator<MyEdge> it = v.outEdges().iterator();
            while (it.hasNext())
                getTreeChildren(it.next().getTarget(), leafSet);
        }
    }

    private void addSubtree(MyNode v, MyNode vCopy, HybridTree tCopy) {
        vCopy.setLabel(v.getLabel());
        Iterator<MyEdge> it = v.outEdges().iterator();
        while (it.hasNext()) {
            MyNode c = it.next().getTarget();
            MyNode cCopy = tCopy.newNode();
            tCopy.newEdge(vCopy, cCopy);
            addSubtree(c, cCopy, tCopy);
        }
    }

    private void getTreeCluster(MyNode v, BitSet b) {
        if (v.getOutDegree() == 0)
            b.set(taxaOrdering.indexOf(v.getLabel()));
        Iterator<MyEdge> it = v.outEdges().iterator();
        while (it.hasNext())
            getTreeCluster(it.next().getTarget(), b);
    }

    private BitSet getTreeCluster(SparseTree c) {
        BitSet cluster = new BitSet(taxaOrdering.size());
        for (SparseTreeNode v : c.getLeaves())
            cluster.set(taxaOrdering.indexOf(v.getLabel()));
        return cluster;
    }

    private HybridTree copyTree(HybridTree t, Vector<MyNode[]> nodePairs) {
        MyNode v = t.getRoot();
        HybridTree tCopy = new HybridTree(new MyPhyloTree(), false, taxaOrdering);
        MyNode root = new MyNode(tCopy, v.getLabel());
        root.setLabel(v.getLabel());
        tCopy.setRoot(root);
        copyTreeRec(t, tCopy, v, root, nodePairs);
        return tCopy;
    }

    private void copyTreeRec(MyPhyloTree t, MyPhyloTree tCopy, MyNode v, MyNode vCopy, Vector<MyNode[]> nodePairs) {
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
            copyTreeRec(t, tCopy, c, cCopy, nodePairs);
            eCopy.setInfo(e.getInfo());
            eCopy.setLabel(e.getLabel());
        }
    }

}
