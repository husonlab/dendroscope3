/*
 *   TerminalAlg_Sparse.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.terminals;

import dendroscope.hybroscale.util.graph.MyPhyloTree;
import dendroscope.hybroscale.util.lcaQueries.LCA_Query_LogN_Sparse;
import dendroscope.hybroscale.util.sparseGraph.MySparseGraph;
import dendroscope.hybroscale.util.sparseGraph.MySparseNode;
import dendroscope.hybroscale.util.sparseGraph.MySparsePhyloTree;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TerminalAlg_Sparse {

    private MyPhyloTree tOne, tTwo;
    private Vector<String> taxaOrdering;
    private int r, labelOffset;
    private boolean success = false;

    private ConcurrentHashMap<BitSet, Integer> failedCutSets = new ConcurrentHashMap<BitSet, Integer>();
    private boolean isStopped = false;

    public TerminalAlg_Sparse(MyPhyloTree tOne, MyPhyloTree tTwo, int r, Vector<String> taxaOrdering) {
        this.tOne = tOne;
        this.tTwo = tTwo;
        this.r = r;
        this.taxaOrdering = (Vector<String>) taxaOrdering.clone();
    }

    public boolean run() {

        labelOffset = Integer.MAX_VALUE;
        for (String taxon : taxaOrdering) {
            Integer i = Integer.parseInt(taxon);
            labelOffset = i < labelOffset ? i : labelOffset;
        }

//		 System.out.println("Trying " + r + "... " + failedCutSets.size());

        MySparsePhyloTree t1 = new MySparsePhyloTree(tOne);
        MySparsePhyloTree t2 = new MySparsePhyloTree(tTwo);

        runRec(t1, t2, r, new BitSet(taxaOrdering.size()));

        // System.out.println("Result: " + r + " " +
        // success+" "+failedCutSets.size());

        return success;

    }

    private void runRec(MySparsePhyloTree t1, MySparsePhyloTree t2, int i, BitSet cutSet) {

        BitSet hashSet = getHashSet(t1);
        int lowerBound = failedCutSets.containsKey(hashSet) ? failedCutSets.get(hashSet) : -1;

        if (!success && lowerBound < i && !isStopped) {

            MySparseNode[] t1Leaves = new MySparseNode[taxaOrdering.size()];
            MySparseNode[] t2Leaves = new MySparseNode[taxaOrdering.size()];

//			System.out.println("\n");
//			System.out.println(t1 + ";");
//			System.out.println(t2 + ";");

            cmpClusters(t1, t2, t1Leaves, t2Leaves);
            collapseST(t1, t2, t1Leaves, t2Leaves);

//			System.out.println(t1 + ";");
//			System.out.println(t2 + ";");

            cmpClusters(t1, t2, t1Leaves, t2Leaves);

            hashSet = getHashSet(t1);
            lowerBound = failedCutSets.containsKey(hashSet) ? failedCutSets.get(hashSet) : -1;

            if (lowerBound < i) {

                if (t1.getNodes().size() == 1) {
                    success = true;
                } else if (i > 0) {

                    Vector<BitSet> primeSet = assessPrimeSet(i, t1, t2, t1Leaves, t2Leaves);

                    for (BitSet b : primeSet) {

                        BitSet leafSet = (BitSet) getLeaf(t1Leaves, b).getInfo();

                        MySparseNode v1 = getLeaf(t1Leaves, b);
                        MySparseNode[] nodePair1 = {v1, null};
                        MySparsePhyloTree t1Copy = copyTree(t1, nodePair1);
                        deleteLeafNode(nodePair1[1]);

                        MySparseNode v2 = getLeaf(t2Leaves, b);
                        MySparseNode[] nodePair2 = {v2, null};
                        MySparsePhyloTree t2Copy = copyTree(t2, nodePair2);
                        deleteLeafNode(nodePair2[1]);

                        int iRec = i - 1;
                        BitSet recCutSet = (BitSet) cutSet.clone();
                        recCutSet.or(leafSet);

                        runRec(t1Copy, t2Copy, iRec, recCutSet);

                    }
                }

            }

        }

        if (!success && usehash()) {
            hashSet = getHashSet(t1);
            if (!failedCutSets.containsKey(hashSet) || failedCutSets.get(hashSet) < i)
                failedCutSets.put(hashSet, i);
        }

    }

    private BitSet getHashSet(MySparsePhyloTree t1) {
        BitSet leafSet = new BitSet(taxaOrdering.size());
        for (MySparseNode l : t1.getLeaves()) {
            int index = Integer.parseInt(l.getLabel()) - labelOffset;
            leafSet.set(index);
        }
        return leafSet;
    }

    private boolean usehash() {
        double maxMemory = java.lang.Runtime.getRuntime().maxMemory();
        double totalMemory = java.lang.Runtime.getRuntime().totalMemory();
        double usedMemory = totalMemory - java.lang.Runtime.getRuntime().freeMemory();
        if ((usedMemory / maxMemory) < 0.5)
            return true;
        return false;
    }

    private void deleteLeafNode(MySparseNode v) {
        MySparseNode p = v.getParent();
        p.getOwner().deleteNode(v);
        contractNode(p);
    }

    private void contractNode(MySparseNode v) {
        if (v.getOutDegree() == 1 && v.getInDegree() == 1) {
            MySparseGraph t = v.getOwner();
            MySparseNode p = v.getParent();
            MySparseNode c = v.getChildren().firstElement();
            t.deleteNode(v);
            t.newEdge(p, c);
        } else if (v.getOutDegree() == 1 && v.getInDegree() == 0) {
            MySparsePhyloTree t = (MySparsePhyloTree) v.getOwner();
            MySparseNode c = v.getChildren().firstElement();
            t.deleteNode(v);
            t.setRoot(c);
        }
    }

    private Vector<BitSet> assessPrimeSet(int i, MySparsePhyloTree t1, MySparsePhyloTree t2, MySparseNode[] t1Leaves,
                                          MySparseNode[] t2Leaves) {

        Vector<BitSet> terminalSet = new Vector<BitSet>();
        for (MySparseNode v : t1.getLeaves()) {
            if (isTerminalNode(v, t2Leaves))
                terminalSet.add(v.getCluster());
        }

        Vector<BitSet> primeSet = new Vector<BitSet>();
        if (terminalSet.size() > 3 * i) {
            return primeSet;
        }

        primeSet = searchConflict2Cluster(terminalSet, t1Leaves, t2Leaves);
        if (!primeSet.isEmpty()) {
            return primeSet;
        }

        if (terminalSet.size() > 2 * i) {
            Iterator<BitSet> it = terminalSet.iterator();
            while (primeSet.size() <= 2 * i)
                primeSet.add(it.next());
        } else {

            Vector<Vector<MySparseNode>> t1Cherries = getCherries(t1);
            Vector<Vector<MySparseNode>> t2Cherries = getCherries(t2);

            BitSet cherrySet = new BitSet(taxaOrdering.size());
            for (Vector<MySparseNode> cherry : t2Cherries) {
                for (MySparseNode c : cherry)
                    cherrySet.or(c.getCluster());
            }

            BitSet chosenSet = new BitSet(taxaOrdering.size());
            for (Vector<MySparseNode> cherry : t1Cherries) {
                MySparseNode[] nodePair = new MySparseNode[2];
                for (MySparseNode v : cherry) {
                    if (isTerminalNode(v, t2Leaves)) {
                        nodePair[0] = v;
                        if (v.getCluster().intersects(cherrySet))
                            break;
                    }
                }
                if (nodePair[0] != null) {
                    for (MySparseNode v : cherry) {
                        if (!v.equals(nodePair[0])) {
                            nodePair[1] = v;
                            if (v.getCluster().intersects(cherrySet))
                                break;
                        }
                    }
                    chosenSet.set(nodePair[0].getCluster().nextSetBit(0));
                    chosenSet.set(nodePair[1].getCluster().nextSetBit(0));
                    primeSet.add(nodePair[0].getCluster());
                    primeSet.add(nodePair[1].getCluster());
                }
            }

            for (Vector<MySparseNode> cherry : t2Cherries) {
                MySparseNode[] nodePair = new MySparseNode[2];
                for (MySparseNode v : cherry) {
                    if (isTerminalNode(v, t1Leaves)) {
                        nodePair[0] = v;
                        if (v.getCluster().intersects(chosenSet))
                            break;
                    }
                }
                if (nodePair[0] != null) {
                    for (MySparseNode v : cherry) {
                        if (!v.equals(nodePair[0])) {
                            nodePair[1] = v;
                            if (v.getCluster().intersects(chosenSet))
                                break;
                        }
                    }
                    if (!nodePair[0].getCluster().intersects(chosenSet))
                        primeSet.add(nodePair[0].getCluster());
                    if (!nodePair[1].getCluster().intersects(chosenSet))
                        primeSet.add(nodePair[1].getCluster());
                }
            }

        }

        return primeSet;
    }

    private Vector<BitSet> searchConflict2Cluster(Vector<BitSet> terminalSet, MySparseNode[] t1Leaves,
                                                  MySparseNode[] t2Leaves) {

        Vector<BitSet> primeSet = new Vector<BitSet>();
        for (BitSet termCluster : terminalSet) {

            MySparseNode v1 = getLeaf(t1Leaves, termCluster);
            MySparseNode p1 = v1.getParent();
            BitSet p1Cluster = p1.getCluster();
            MySparseNode v2 = getLeaf(t2Leaves, termCluster);
            MySparseNode p2 = v2.getParent();
            BitSet p2Cluster = p2.getCluster();
            if (p1Cluster.cardinality() == 2 && p2Cluster.cardinality() == 2) {
                BitSet pCluster = (BitSet) p1Cluster.clone();
                pCluster.or(p2Cluster);
                int i = pCluster.nextSetBit(0);
                while (i != -1) {
                    BitSet b = new BitSet(taxaOrdering.size());
                    b.set(i);
                    primeSet.add(b);
                    i = pCluster.nextSetBit(i + 1);
                }
                break;
            }

        }
        return primeSet;
    }

    private Vector<Vector<MySparseNode>> getCherries(MySparsePhyloTree t) {
        Vector<Vector<MySparseNode>> cherries = new Vector<Vector<MySparseNode>>();
        Vector<MySparseNode> visited = new Vector<MySparseNode>();
        for (MySparseNode v : t.getLeaves()) {
            MySparseNode p = v.getParent();
            if (!visited.contains(p)) {
                visited.add(p);
                if (isCherry(p)) {
                    Vector<MySparseNode> cherry = new Vector<MySparseNode>();
                    for (MySparseNode c : p.getChildren())
                        cherry.add(c);
                    cherries.add(cherry);
                }
            }
        }
        return cherries;
    }

    private boolean isCherry(MySparseNode v) {
        for (MySparseNode c : v.getChildren()) {
            if (c.getOutDegree() != 0)
                return false;
        }
        return true;
    }

    private boolean isTerminalNode(MySparseNode v1, MySparseNode[] t2Leaves) {
        BitSet b = v1.getCluster();
        MySparseNode v2 = getLeaf(t2Leaves, b);
        MySparseNode p1 = v1.getParent();
        MySparseNode p2 = v2.getParent();
        BitSet b1 = (BitSet) p1.getCluster().clone();
        BitSet b2 = (BitSet) p2.getCluster().clone();
        b1.xor(b);
        b2.xor(b);
        return !b1.intersects(b2);
    }

    private void collapseST(MySparsePhyloTree t1, MySparsePhyloTree t2, MySparseNode[] t1Leaves, MySparseNode[] t2Leaves) {

        Vector<BitSet> stSets = cmpSTNodes(t1, t2, t2Leaves);

        for (BitSet stSet : stSets) {

            // this.printCluster(stSet);
            // System.out.println(t1 + ";");
            // System.out.println(t2 + ";");

            Vector<MySparseNode> contractedNodes = new Vector<MySparseNode>();
            Vector<MySparseNode> endNodes = new Vector<MySparseNode>();
            int i = stSet.nextSetBit(0);
            while (i != -1) {
                BitSet b = new BitSet(taxaOrdering.size());
                b.set(i);
                contractLeafNode(getLeaf(t1Leaves, b), contractedNodes, endNodes);
                contractLeafNode(getLeaf(t2Leaves, b), contractedNodes, endNodes);
                i = stSet.nextSetBit(i + 1);
            }

            for (MySparseNode v : endNodes) {
                MySparseNode w = v.getOwner().newNode();
                w.setInfo(v.getInfo());
                v.setInfo(new BitSet(taxaOrdering.size()));
                w.setLabel(v.getLabel());
                v.setLabel("");
                v.getOwner().newEdge(v, w);
            }

        }

    }

    private void contractLeafNode(MySparseNode v, Vector<MySparseNode> contractedNodes, Vector<MySparseNode> endNodes) {
        if (v.getInDegree() != 0) {
            BitSet vSet = (BitSet) v.getInfo();
            MySparseNode p = v.getParent();
            p.setLabel(createUniqueLabel(p.getLabel(), v.getLabel()));
            p.getOwner().deleteNode(v);
            ((BitSet) p.getInfo()).or(vSet);
            if (p.getOutDegree() == 0) {
                if (!contractedNodes.contains(p))
                    contractedNodes.add(p);
                endNodes.remove(p);
                if (p.getInDegree() > 0)
                    contractNodesRec(p.getParent(), contractedNodes, endNodes);
            } else
                contractNodesRec(p, contractedNodes, endNodes);
        }
    }

    private void contractNodesRec(MySparseNode v, Vector<MySparseNode> contractedNodes, Vector<MySparseNode> endNodes) {
        Vector<MySparseNode> conChildren = new Vector<MySparseNode>();
        for (MySparseNode c : v.getChildren()) {
            if (contractedNodes.contains(c))
                conChildren.add(c);
        }
        if (!conChildren.isEmpty()) {
            for (MySparseNode c : conChildren) {
                contractedNodes.remove(c);
                contractLeafNode(c, contractedNodes, endNodes);
            }
        } else if (conChildren.isEmpty() && v.getOutDegree() != 0 && !endNodes.contains(v))
            endNodes.add(v);
    }

    private String createUniqueLabel(String s1, String s2) {

        if (s1.isEmpty())
            return s2;
        if (s2.isEmpty())
            return s1;
        int c1 = Integer.parseInt(s1);
        int c2 = Integer.parseInt(s2);
        Vector<Integer> vec = new Vector<Integer>();
        vec.add(c1);
        vec.add(c2);
        Collections.sort(vec);

        return vec.firstElement() + "";
    }

    private Vector<BitSet> cmpSTNodes(MySparsePhyloTree t1, MySparsePhyloTree t2, MySparseNode[] t2Leaves) {

        LCA_Query_LogN_Sparse rmqQuery = null;
//		LCA_Query_LogN_Sparse rmqQuery = new LCA_Query_LogN_Sparse(t2);

        Vector<Vector<MySparseNode>> stNodeSet = new Vector<Vector<MySparseNode>>();
        Vector<MySparseNode> stMulNodes = new Vector<MySparseNode>();

        cmpSTNodesRec(t1.getRoot(), stNodeSet, stMulNodes, t2Leaves, rmqQuery);

        Vector<BitSet> stSets = new Vector<BitSet>();
        for (Vector<MySparseNode> stNodes : stNodeSet) {
            BitSet stSet = new BitSet(taxaOrdering.size());
            for (MySparseNode v : stNodes)
                stSet.or(v.getCluster());
            if (!stSet.isEmpty())
                stSets.add(stSet);
        }

        return stSets;
    }

    private Vector<MySparseNode> cmpSTNodesRec(MySparseNode v, Vector<Vector<MySparseNode>> stNodeSet,
                                               Vector<MySparseNode> stMulNodes, MySparseNode[] t2Leaves, LCA_Query_LogN_Sparse rmqQuery) {

        if (v.getOutDegree() == 0) {
            Vector<MySparseNode> nodes = new Vector<MySparseNode>();
            nodes.add(v);
            return nodes;
        } else {

            Vector<Vector<MySparseNode>> subNodeSet = new Vector<Vector<MySparseNode>>();
            Vector<MySparseNode> stChildren = new Vector<MySparseNode>();
            for (MySparseNode c : v.getChildren()) {
                Vector<MySparseNode> nodes = cmpSTNodesRec(c, stNodeSet, stMulNodes, t2Leaves, rmqQuery);
                if (!nodes.isEmpty()) {
                    subNodeSet.add(nodes);
                    stChildren.addAll(nodes);
                }
            }

            boolean isCompatible = false;
            Vector<Vector<MySparseNode>> refNodes = new Vector<Vector<MySparseNode>>();
            BitSet refSet = new BitSet(taxaOrdering.size());
            int size = -1;
            for (int maxSize = v.getOutDegree(); maxSize >= 2; maxSize--) {
                RefinedCluster_Sparse refCluster = getRefClusters(v, stChildren, maxSize, refSet);
                for (BitSet cluster : refCluster.getAllCluster()) {
                    if (!cluster.intersects(refSet) && isCompatibleLCA_Naive(cluster, t2Leaves)) {
//					if (!cluster.intersects(refSet) && isCompatibleLCA(cluster, t2Leaves, rmqQuery)) {
                        isCompatible = true;
                        size = refCluster.getSize();
                        refSet.or(cluster);
                        refNodes.add(refCluster.getNodes(cluster));
                    }
                }

            }

            if (!isCompatible || (isCompatible && size < v.getOutDegree())) {

                for (Vector<MySparseNode> subNodes : subNodeSet) {
                    Vector<MySparseNode> stNodes = new Vector<MySparseNode>();
                    for (MySparseNode subNode : subNodes) {
                        if (subNode.getOutDegree() > 0 && !subNode.getCluster().intersects(refSet))
                            stNodes.add(subNode);
                    }
                    stNodeSet.add(stNodes);
                }
                if (!refNodes.isEmpty())
                    stNodeSet.addAll(refNodes);
                return new Vector<MySparseNode>();
            }

            if (v.getInDegree() == 0) {
                Vector<MySparseNode> stNodes = new Vector<MySparseNode>();
                stNodes.add(v);
                stNodeSet.add(stNodes);
            }

            Vector<MySparseNode> nodes = new Vector<MySparseNode>();
            nodes.add(v);
            return nodes;

        }
    }

    private RefinedCluster_Sparse getRefClusters(MySparseNode v, Vector<MySparseNode> stChildren, int size,
                                                 BitSet refSet) {

        Vector<MySparseNode> children = new Vector<MySparseNode>();
        for (MySparseNode c : stChildren) {
            BitSet b = c.getCluster();
            if (!b.intersects(refSet))
                children.add(c);
        }

        RefinedCluster_Sparse refinedCluster = new RefinedCluster_Sparse(size);
        getRefClustersRec(children, size, 0, 0, new BitSet(taxaOrdering.size()), new Vector<MySparseNode>(),
                refinedCluster);

        return refinedCluster;
    }

    private void getRefClustersRec(Vector<MySparseNode> children, int maxSize, int size, int j, BitSet refCluster,
                                   Vector<MySparseNode> hashSet, RefinedCluster_Sparse refinedCluster) {
        if (size < maxSize && children.size() - j >= maxSize - size) {
            for (int k = j; k < children.size(); k++) {
                MySparseNode c = children.get(k);
                BitSet newCluster = (BitSet) refCluster.clone();
                newCluster.or(c.getCluster());
                int newSize = size + 1;
                int newJ = k + 1;
                Vector<MySparseNode> newRefNodes = (Vector<MySparseNode>) hashSet.clone();
                newRefNodes.add(c);
                getRefClustersRec(children, maxSize, newSize, newJ, newCluster, newRefNodes, refinedCluster);
            }
        } else if (size == maxSize)
            refinedCluster.addCluster(refCluster, hashSet);
    }

    private boolean isCompatibleLCA_Naive(BitSet cluster, MySparseNode[] t2Leaves) {

        MySparseNode lca = cmpLCA_naive(cluster, t2Leaves);

        for (MySparseNode c : lca.getChildren()) {
            BitSet childCluster = c.getCluster();
            BitSet b = (BitSet) childCluster.clone();
            b.and(cluster);
            if (!b.equals(childCluster) && childCluster.intersects(cluster))
                return false;
        }
        return true;
    }

    private MySparseNode cmpLCA_naive(BitSet cluster, MySparseNode[] t2Leaves) {
        MySparseNode v = t2Leaves[cluster.nextSetBit(0)];
        BitSet b = (BitSet) v.getCluster().clone();
        b.and(cluster);
        while (!b.equals(cluster)) {
            v = v.getParent();
            b = (BitSet) v.getCluster().clone();
            b.and(cluster);
        }
        return v;
    }

    private boolean isCompatibleLCA(BitSet cluster, MySparseNode[] t2Leaves, LCA_Query_LogN_Sparse rmqQuery) {
        int i = cluster.nextSetBit(0);
        Vector<MySparseNode> t2Nodes = new Vector<MySparseNode>();
        while (i != -1) {
            BitSet leafCluster = new BitSet(taxaOrdering.size());
            leafCluster.set(i);
            t2Nodes.add(getLeaf(t2Leaves, leafCluster));
            i = cluster.nextSetBit(i + 1);
        }
        MySparseNode lca = rmqQuery.cmpLCA(t2Nodes);

        for (MySparseNode c : lca.getChildren()) {
            BitSet childCluster = c.getCluster();
            BitSet b = (BitSet) childCluster.clone();
            b.and(cluster);
            if (!b.equals(childCluster) && childCluster.intersects(cluster))
                return false;
        }
        return true;
    }

    private void cmpClusters(MySparsePhyloTree t1, MySparsePhyloTree t2, MySparseNode[] t1Leaves,
                             MySparseNode[] t2Leaves) {
        cmpClustersRec(t1.getRoot(), t1Leaves);
        cmpClustersRec(t2.getRoot(), t2Leaves);
    }

    private BitSet cmpClustersRec(MySparseNode v, MySparseNode[] t1Leaves) {
        BitSet b = new BitSet(taxaOrdering.size());
        if (v.getOutDegree() == 0) {
            int index = Integer.parseInt(v.getLabel()) - labelOffset;
            b.set(index);
        } else {
            for (MySparseNode c : v.getChildren())
                b.or(cmpClustersRec(c, t1Leaves));
        }
        v.setCluster(b);
        if (v.getOutDegree() == 0) {
            int k = b.nextSetBit(0);
            t1Leaves[k] = v;
        }
        return b;
    }

    private MySparseNode getLeaf(MySparseNode[] t2Leaves, BitSet b) {
        int k = b.nextSetBit(0);
        return t2Leaves[k];
    }

    private MySparsePhyloTree copyTree(MySparsePhyloTree t, MySparseNode[] nodePair) {
        MySparsePhyloTree tCopy = new MySparsePhyloTree();
        tCopy.getRoot().setLabel(t.getRoot().getLabel());
        copyTreeRec(t.getRoot(), tCopy.getRoot(), nodePair);
        return tCopy;
    }

    private void copyTreeRec(MySparseNode v, MySparseNode vCopy, MySparseNode[] nodePair) {
        for (MySparseNode c : v.getChildren()) {
            MySparseGraph tCopy = vCopy.getOwner();
            MySparseNode cCopy = tCopy.newNode();
            cCopy.setLabel(c.getLabel());
            tCopy.newEdge(vCopy, cCopy);
            copyTreeRec(c, cCopy, nodePair);
        }
        if (nodePair[0] == v) {
            nodePair[1] = vCopy;
        }
        vCopy.setInfo(((BitSet) v.getInfo()).clone());
    }

    public ConcurrentHashMap<BitSet, Integer> getFailedCutSets() {
        return failedCutSets;
    }

    public int getR() {
        return r;
    }

    public void addFailedCutSet(BitSet cutSet, Integer lowerBound) {
        if (usehash()) {
            if (failedCutSets.containsKey(cutSet) && failedCutSets.get(cutSet) < lowerBound) {
                failedCutSets.put(cutSet, lowerBound);
            } else if (!failedCutSets.containsKey(cutSet))
                failedCutSets.put(cutSet, lowerBound);
        }
    }

    // For debugging **********************************************************

    private void printClusters(HashSet<BitSet> clusterSet) {
        for (BitSet b : clusterSet) {
            System.out.println();
            int i = b.nextSetBit(0);
            while (i != -1) {
                System.out.print(taxaOrdering.get(i) + " ");
                i = b.nextSetBit(i + 1);
            }
        }
    }

    private void printCluster(BitSet b) {
        int i = b.nextSetBit(0);
        while (i != -1) {
            System.out.print(taxaOrdering.get(i) + " ");
            i = b.nextSetBit(i + 1);
        }
        System.out.println();
    }

    public void stopExecution() {
        isStopped = true;
    }

    public void freeMemory() {
        failedCutSets = null;
    }

}
