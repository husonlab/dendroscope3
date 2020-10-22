/*
 *   HybridNetwork.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.treeObjects;

import dendroscope.hybroscale.model.reductionSteps.ReplacementInfo;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

import java.util.*;

/**
 * This class represents a resolved network.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class HybridNetwork extends MyPhyloTree {

    private String altLabel;

    // node-ordering produced by an in-order tree walk
    private Vector<MyNode> postOrderNodes = new Vector<MyNode>();

    // taxon-labelings sorted lexicographically
    private Vector<String> taxaOrdering;
    // all containing clusters, represented by a bit-set
    // -> according to the taxaOrdering, a bit denotes whether the taxon is
    // contained in the cluster
    private HashSet<BitSet> clusterSet = new HashSet<BitSet>();

    private HashMap<MyNode, BitSet> nodeToCluster = new HashMap<MyNode, BitSet>();
    private HashMap<BitSet, MyNode> clusterToNode = new HashMap<BitSet, MyNode>();

    public HashMap<Vector<String>, Integer> taxaPairToWeight = new HashMap<Vector<String>, Integer>();
    public HashMap<MyEdge, Integer> edgeToWeight = new HashMap<MyEdge, Integer>();

    private String replacementCharacter;
    private boolean isResolved = true;

    @SuppressWarnings("unchecked")
    public HybridNetwork(MyPhyloTree t, boolean rootTree, Vector<String> taxaOrdering) {
        super(copy(t));
        if (rootTree)
            addOutgroup();
        this.taxaOrdering = (Vector<String>) taxaOrdering.clone();
    }

    @SuppressWarnings("unchecked")
    public HybridNetwork(HybridNetwork n, boolean rootTree, Vector<String> taxaOrdering) {
        super(copy(n));
        if (rootTree)
            addOutgroup();
        this.taxaOrdering = (Vector<String>) taxaOrdering.clone();
        if (n.getAltLabel() != null)
            altLabel = n.getAltLabel();
        if (n.getReplacementCharacter() != null)
            replacementCharacter = n.getReplacementCharacter();

        this.isResolved = n.isResolved();

        Iterator<Vector<String>> it = n.getTaxaPairToWeight().keySet().iterator();
        while (it.hasNext()) {
            Vector<String> key = it.next();
            int value = n.getTaxaPairToWeight().get(key);
            this.taxaPairToWeight.put(key, value);
        }

        this.taxaPairToWeight = (HashMap<Vector<String>, Integer>) n.getTaxaPairToWeight().clone();

    }

    @SuppressWarnings("unchecked")
    public HybridNetwork(MyPhyloTree t, boolean rootTree, Vector<String> taxaOrdering, boolean isResolved)
            throws Exception {
        super(copy(t));
        if (rootTree)
            addOutgroup();
        this.taxaOrdering = (Vector<String>) taxaOrdering.clone();
    }

    // public void initTaxaOrdering() {
    // taxaOrdering.clear();
    // Iterator<MyNode> it = getLeaves().iterator();
    // while (it.hasNext())
    // taxaOrdering.add(getLabel(it.next()));
    // Collections.sort(taxaOrdering);
    // }

    // adds outgroup 'rho' to the network
    private void addOutgroup() {
        Iterator<MyNode> it = getLeaves().iterator();
        while (it.hasNext()) {
            MyNode v = it.next();
            if (getLabel(v).equals("rho")) {
                altLabel = changeLabel("rho'");
                setLabel(v, altLabel);
            }
        }
        MyNode rho = newNode();
        setLabel(rho, "rho");
        MyNode newR = newNode();
        MyNode r = getRoot();
        newEdge(newR, rho);
        newEdge(newR, r);
        setRoot(newR);
    }

    // find unique labeling for a node v in the network
    protected String changeLabel(String label) {
        Iterator<MyNode> it = getLeaves().iterator();
        while (it.hasNext()) {
            MyNode l = it.next();
            if (this.getLabel(l).equals(label)) {
                String newLabel = label.concat("'");
                changeLabel(newLabel);
                break;
            }
        }
        return label;
    }

    private void resetLabelings() {
        Iterator<MyNode> it = postOrderWalk();
        while (it.hasNext()) {
            MyNode v = it.next();
            if (v.getOutDegree() != 0)
                this.setLabel(v, null);
        }
    }

    public void update() {
        resetLabelings();
        clusterSet.clear();
        nodeToCluster.clear();
        initClusters();
    }

    private void initClusters() {
        Vector<MyNode> visited = new Vector<MyNode>();
        initClustersRec(getRoot(), visited, new Hashtable<MyNode, Vector<String>>());
    }

    private Vector<String> initClustersRec(MyNode v, Vector<MyNode> visited,
                                           Hashtable<MyNode, Vector<String>> nodeToLabel) {

        Vector<String> vTaxa = new Vector<String>();
        BitSet cluster = new BitSet(taxaOrdering.size());
        Iterator<MyNode> it = getSuccessors(v);
        if (v.getOutDegree() != 0) {
            while (it.hasNext()) {
                MyNode child = it.next();
                if (!visited.contains(child)) {
                    visited.add(child);
                    Vector<String> cTaxa = initClustersRec(child, visited, nodeToLabel);
                    for (String l : cTaxa) {
                        if (!vTaxa.contains(l))
                            vTaxa.add(l);
                    }
                    nodeToLabel.put(v, vTaxa);
                } else {
                    Vector<String> cTaxa = nodeToLabel.get(child);
                    for (String l : cTaxa)
                        vTaxa.add(l);
                }
            }
            Vector<String> labels = new Vector<String>();
            for (String l : vTaxa) {
                int bitIndex = taxaOrdering.indexOf(l);
                cluster.set(bitIndex);
                if (getLabel(v) == null)
                    setLabel(v, l);
                else if (!labels.contains(l)) {
                    setLabel(v, getLabel(v) + "+" + l);
                    labels.add(l);
                }
            }
        } else {
            vTaxa.add(getLabel(v));
            int bitIndex = taxaOrdering.indexOf(getLabel(v));
            if (bitIndex < 0) {
                System.out.println("-1: " + getLabel(v) + "\n" + this + ";");
                for (String s : taxaOrdering)
                    System.out.print(s + " ");
                System.out.println();
                System.out.println(getLabel(v).length());
            }
            cluster.set(bitIndex);
        }

        if (v.getOutDegree() >= 2 || v.getOutDegree() == 0) {
            nodeToCluster.put(v, cluster);
            clusterToNode.put(cluster, v);
            clusterSet.add(cluster);
        }
        return vTaxa;
    }

    // performs a post order walk through this resolved network
    public Iterator<MyNode> postOrderWalk() {
        postOrderNodes.clear();
        Vector<MyNode> visited = new Vector<MyNode>();
        postOrderWalkRec(getRoot(), visited);
        return postOrderNodes.iterator();
    }

    private void postOrderWalkRec(MyNode v, Vector<MyNode> visited) {
        Iterator<MyNode> it = getSuccessors(v);
        while (it.hasNext()) {
            MyNode child = it.next();
            if (!visited.contains(child)) {
                visited.add(child);
                postOrderWalkRec(child, visited);
            }
        }
        postOrderNodes.add(v);
    }

    // returns the subtree under node v
    public HybridNetwork getSubtree(MyNode v, boolean doUpdate) {
        MyPhyloTree sT = new MyPhyloTree();
        // subtree is copied into sT
        if (contains(v)) {
            MyNode vCopy = sT.newNode(v);
            sT.setLabel(vCopy, getLabel(v));
            sT.setRoot(vCopy);
            createSubtreeRec(v, vCopy, sT, new Hashtable<MyNode, MyNode>());
        }
        // network is created out of sT
        HybridNetwork h = new HybridNetwork(sT, false, taxaOrdering);
        if (doUpdate)
            h.update();
        return h;
    }

    private void createSubtreeRec(MyNode v, MyNode vCopy, MyPhyloTree t, Hashtable<MyNode, MyNode> created) {
        Iterator<MyEdge> it = getOutEdges(v);
        while (it.hasNext()) {
            MyEdge e = it.next();
            MyNode c = e.getTarget();
            MyNode cCopy;
            if (!created.containsKey(c)) {
                cCopy = t.newNode(c);
                t.setLabel(cCopy, getLabel(c));
                created.put(c, cCopy);
                MyEdge eCopy = t.newEdge(vCopy, cCopy);
                if (isSpecial(e)) {
                    t.setSpecial(eCopy, true);
                    t.setWeight(eCopy, 0);
                }
                createSubtreeRec(c, cCopy, t, created);
            } else {
                cCopy = created.get(c);
                MyEdge eCopy = t.newEdge(vCopy, cCopy);
                if (isSpecial(e)) {
                    t.setSpecial(eCopy, true);
                    t.setWeight(eCopy, 0);
                }
            }
        }
    }

    public boolean contains(MyNode v) {
        Iterator<MyNode> it = postOrderWalk();
        while (it.hasNext()) {
            if (it.next().equals(v))
                return true;
        }
        return false;
    }

    public Iterator<MyNode> getSuccessors(MyNode v) {
        Iterator<MyEdge> it = v.outEdges().iterator();
        Vector<MyNode> successors = new Vector<MyNode>();
        while (it.hasNext()) {
            successors.add(it.next().getTarget());
        }
        return successors.iterator();
    }

    // deletes outgroup 'rho'
    public void removeOutgroup() {

        Iterator<MyNode> it = getLeaves().iterator();
        while (it.hasNext()) {
            MyNode v = it.next();
            if (getLabel(v).equals("rho")) {
                MyEdge e = (MyEdge) v.getFirstInEdge();
                MyNode p = e.getSource();
                deleteEdge(e);
                deleteNode(v);
                e = (MyEdge) p.getFirstOutEdge();
                MyNode c = e.getTarget();
                deleteEdge(e);
                deleteNode(p);
                setRoot(c);
                break;
            }
        }
        if (altLabel != null) {
            it = getLeaves().iterator();
            while (it.hasNext()) {
                MyNode v = it.next();
                if (getLabel(v).equals(altLabel))
                    setLabel(v, "rho");
            }
        }
        update();

    }

    // all labelings of each inner-nodes are set to null
    public void clearLabelings() {
        Iterator<MyNode> it = postOrderWalk();
        while (it.hasNext()) {
            MyNode v = it.next();
            if (v.getOutDegree() != 0)
                this.setLabel(v, null);
        }
    }

    // replaces subtrees displaying a distinct set of cluster by a unique
    // taxon-labeling
    @SuppressWarnings("unchecked")
    public Vector<String> replaceClusters(Vector<BitSet> clusters, ReplacementInfo rI) throws Exception {
        Vector<String> taxa = (Vector<String>) taxaOrdering.clone();
        for (BitSet cluster : clusters) {
            MyNode v = getClusterToNode().get(cluster);
            String label = changeLabel(taxa.get(cluster.nextSetBit(0)) + "'");
            taxa.add(label);

            MyPhyloTree subtree = getSubtree(v, true);

            MyNode newV = newNode();
            setLabel(newV, label);
            deleteSubtree(v, newV, false);

            Iterator<MyNode> it = subtree.getLeaves().iterator();
            Vector<String> l = new Vector<String>();
            while (it.hasNext()) {
                l.add(subtree.getLabel(it.next()));
            }

            rI.putLabelToSubtree(label, subtree);
        }
        update();
        return taxa;
    }

    public String getTaxa(int index) {
        if (taxaOrdering.size() == 0)
            initClusters();
        if (index < taxaOrdering.size())
            return taxaOrdering.get(index);
        else
            return null;
    }

    // replaces a subtree under node v by a new node newV
    public void deleteSubtree(MyNode v, MyNode newV, boolean doUpdate) {
        MyNode p = ((MyEdge) v.getFirstInEdge()).getSource();
        deleteSubtreeRec(v);
        if (newV != null) {
            newEdge(p, newV);
            if (!taxaOrdering.contains(newV.getLabel()))
                taxaOrdering.add(newV.getLabel());
        }
        if (doUpdate)
            update();
    }

    private void deleteSubtreeRec(MyNode v) {
        Iterator<MyNode> it = getSuccessors(v);
        if (v.getOutDegree() != 0) {
            while (it.hasNext()) {
                MyNode child = it.next();
                deleteSubtreeRec(child);
            }
        }
        deleteEdge((MyEdge) v.getFirstInEdge());
        deleteNode(v);
    }

    // finds the smallest node containing a distinct cluster
    public MyNode findLCA(BitSet cluster) {
        Iterator<MyNode> it = postOrderWalk();
        BitSet lcaCluster = null;
        MyNode lca = null;
        while (it.hasNext()) {
            MyNode v = it.next();
            BitSet vCluster = nodeToCluster.get(v);
            if (vCluster != null) {
                BitSet b = (BitSet) vCluster.clone();
                b.or(cluster);
                if (b.equals(vCluster)) {
                    if (lcaCluster == null) {
                        lca = v;
                        lcaCluster = vCluster;
                    } else if (vCluster.cardinality() < lcaCluster.cardinality()) {
                        lca = v;
                        lcaCluster = vCluster;
                    }
                }
            }
        }
        return lca;
    }

    public Vector<MyNode> getLeaves(MyNode v) {
        Vector<MyNode> leaves = new Vector<MyNode>();
        Iterator<MyNode> it = getSuccessors(v);
        while (it.hasNext()) {
            MyNode l = it.next();
            if (l.getOutDegree() == 0)
                leaves.add(l);
        }
        return leaves;
    }

    // returns true if the network is bicombining
    private boolean isBicombining() {
        Iterator<MyNode> it = nodeIterator();
        while (it.hasNext()) {
            MyNode v = it.next();
            if (!(getRoot().equals(v) || v.getInDegree() == 2 || v.getInDegree() == 1))
                return false;
        }
        return true;
    }

    // returns the edge weight produced by the chain reduction step
    public int getEdgeWeight(MyEdge e) {
        MyNode s = e.getSource();
        MyNode t = e.getTarget();
        Vector<MyNode> sChilds = getLeaves(s);
        Vector<MyNode> tChilds = getLeaves(t);
        for (MyNode v : sChilds) {
            for (MyNode w : tChilds) {
                Vector<String> taxaPair = new Vector<String>();
                taxaPair.add(getLabel(v));
                taxaPair.add(getLabel(w));
                Collections.sort(taxaPair);
                if (taxaPairToWeight.containsKey(taxaPair))
                    return taxaPairToWeight.get(taxaPair);
            }
        }
        return 0;
    }

    public int getEdgeWeight(Vector<String> v1Labels, Vector<String> v2Labels) {
        for (String l1 : v1Labels) {
            for (String l2 : v2Labels) {
                Vector<String> taxaPair = new Vector<String>();
                taxaPair.add(l1);
                taxaPair.add(l2);
                Collections.sort(taxaPair);
                if (taxaPairToWeight.containsKey(taxaPair))
                    return taxaPairToWeight.get(taxaPair);
            }
        }
        return 0;
    }

    public Vector<String> getTaxaOrdering() {
        if (taxaOrdering.size() == 0)
            initClusters();
        return taxaOrdering;
    }

    public HashSet<BitSet> getClusterSet() {
        if (clusterSet.size() == 0)
            initClusters();
        return clusterSet;
    }

    public HashMap<MyNode, BitSet> getNodeToCluster() {
        if (nodeToCluster.keySet().size() == 0)
            initClusters();
        return nodeToCluster;
    }

    public HashMap<BitSet, MyNode> getClusterToNode() {
        if (clusterToNode.keySet().size() == 0)
            initClusters();
        return clusterToNode;
    }

    public void setReplacementCharacter(String replacementCharacter) {
        this.replacementCharacter = replacementCharacter;
    }

    public String getReplacementCharacter() {
        return replacementCharacter;
    }

    public String getAltLabel() {
        return altLabel;
    }

    public HashMap<Vector<String>, Integer> getTaxaPairToWeight() {
        return taxaPairToWeight;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public void setAltLabel(String altLabel) {
        this.altLabel = altLabel;
    }

    public void setTaxaPairToWeight(HashMap<Vector<String>, Integer> taxaPairToWeight) {
        this.taxaPairToWeight = taxaPairToWeight;
    }

    public void setEdgeToWeight(HashMap<MyEdge, Integer> edgeToWeight) {
        this.edgeToWeight = edgeToWeight;
    }

    public void setTaxaOrdering(Vector<String> taxaOrdering) {
        this.taxaOrdering = taxaOrdering;
    }

}
