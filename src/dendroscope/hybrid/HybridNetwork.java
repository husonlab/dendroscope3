/**
 * HybridNetwork.java 
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

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.util.*;

/**
 * This class represents a resolved network.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class HybridNetwork extends PhyloTree {

    private String altLabel;

    // node-ordering produced by an in-order tree walk
    private final Vector<Node> postOrderNodes = new Vector<>();

    // taxon-labelings sorted lexicographically
    private Vector<String> taxaOrdering;
    // all containing clusters, represented by a bit-set
    // -> according to the taxaOrdering, a bit denotes whether the taxon is
    // contained in the cluster
    private final HashSet<BitSet> clusterSet = new HashSet<>();

    private final Hashtable<Node, BitSet> nodeToCluster = new Hashtable<>();
    private final Hashtable<BitSet, Node> clusterToNode = new Hashtable<>();

    protected Hashtable<Vector<String>, Integer> taxaPairToWeight = new Hashtable<>();
    protected Hashtable<Edge, Integer> edgeToWeight = new Hashtable<>();

    private String replacementCharacter;
    private boolean isResolved = true;

    @SuppressWarnings("unchecked")
    public HybridNetwork(PhyloTree t, boolean rootTree,
                         Vector<String> taxaOrdering) {
        this.copy(t);
        if (rootTree)
            addOutgroup();
        if (taxaOrdering == null) {
            this.taxaOrdering = new Vector<>();
            initTaxaOrdering();
        } else
            this.taxaOrdering = (Vector<String>) taxaOrdering.clone();
    }

    @SuppressWarnings("unchecked")
    public HybridNetwork(HybridNetwork n, boolean rootTree,
                         Vector<String> taxaOrdering) {
        this.copy(n);
        if (rootTree)
            addOutgroup();
        if (taxaOrdering == null) {
            this.taxaOrdering = new Vector<>();
            initTaxaOrdering();
        } else
            this.taxaOrdering = (Vector<String>) taxaOrdering.clone();
        if (n.getAltLabel() != null)
            altLabel = n.getAltLabel();
        if (n.getReplacementCharacter() != null)
            replacementCharacter = n.getReplacementCharacter();

        this.isResolved = n.isResolved();

        for (Vector<String> key : n.getTaxaPairToWeight().keySet()) {
            int value = n.getTaxaPairToWeight().get(key);
            this.taxaPairToWeight.put(key, value);
        }

        this.taxaPairToWeight = (Hashtable<Vector<String>, Integer>) n
                .getTaxaPairToWeight().clone();

    }

    @SuppressWarnings("unchecked")
    public HybridNetwork(PhyloTree t, boolean rootTree,
                         Vector<String> taxaOrdering, boolean isResolved) throws Exception {
        this.copy(t);
        if (rootTree)
            addOutgroup();
        if (taxaOrdering == null) {
            this.taxaOrdering = new Vector<>();
            initTaxaOrdering();
        } else
            this.taxaOrdering = (Vector<String>) taxaOrdering.clone();
        this.isResolved = isResolvedCheck();
    }

    protected void initTaxaOrdering() {
        taxaOrdering.clear();
        for (Node node : computeSetOfLeaves()) taxaOrdering.add(getLabel(node));
        Collections.sort(taxaOrdering);
    }

    // adds outgroup 'rho' to the network
    private void addOutgroup() {
        for (Node v : computeSetOfLeaves()) {
            if (getLabel(v).equals("rho")) {
                altLabel = changeLabel("rho'");
                setLabel(v, altLabel);
            }
        }
        Node rho = newNode();
        setLabel(rho, "rho");
        Node newR = newNode();
        Node r = getRoot();
        newEdge(newR, rho);
        newEdge(newR, r);
        setRoot(newR);
    }

    // find unique labeling for a node v in the network
    protected String changeLabel(String label) {
        for (Node l : computeSetOfLeaves()) {
            if (this.getLabel(l).equals(label)) {
                String newLabel = label.concat("'");
                changeLabel(newLabel);
                break;
            }
        }
        return label;
    }

    private void resetLabelings() {
        Iterator<Node> it = postOrderWalk();
        while (it.hasNext()) {
            Node v = it.next();
            if (v.getOutDegree() != 0)
                this.setLabel(v, null);
        }
    }

    public void update() {
        resetLabelings();
        clusterSet.clear();
        nodeToCluster.clear();
        clusterToNode.clear();
        initClusters();
    }

    private void initClusters() {
        Vector<Node> visited = new Vector<>();
        initClustersRec(getRoot(), visited,
                new Hashtable<Node, Vector<String>>());
    }

    private Vector<String> initClustersRec(Node v, Vector<Node> visited,
                                           Hashtable<Node, Vector<String>> nodeToLabel) {
        Vector<String> vTaxa = new Vector<>();
        BitSet cluster = new BitSet(taxaOrdering.size());
        Iterator<Node> it = getSuccessors(v);
        if (v.getOutDegree() != 0) {
            while (it.hasNext()) {
                Node child = it.next();
                if (!visited.contains(child)) {
                    visited.add(child);
                    Vector<String> cTaxa = initClustersRec(child, visited,
                            nodeToLabel);
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
            Vector<String> labels = new Vector<>();
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
            cluster.set(bitIndex);
        }

        if (v.getOutDegree() == 2 || v.getOutDegree() == 0) {
            nodeToCluster.put(v, cluster);
            clusterToNode.put(cluster, v);
            clusterSet.add(cluster);
        }
        return vTaxa;
    }

    // performs a post order walk through this resolved network
    public Iterator<Node> postOrderWalk() {
        postOrderNodes.clear();
        Vector<Node> visited = new Vector<>();
        postOrderWalkRec(getRoot(), visited);
        return postOrderNodes.iterator();
    }

    private void postOrderWalkRec(Node v, Vector<Node> visited) {
        Iterator<Node> it = getSuccessors(v);
        while (it.hasNext()) {
            Node child = it.next();
            if (!visited.contains(child)) {
                visited.add(child);
                postOrderWalkRec(child, visited);
            }
        }
        postOrderNodes.add(v);
    }

    // returns the subtree under node v
    public HybridNetwork getSubtree(Node v, boolean doUpdate) {
        PhyloTree sT = new PhyloTree();
        // subtree is copied into sT
        if (contains(v)) {
            Node vCopy = sT.newNode(v);
            sT.setLabel(vCopy, getLabel(v));
            sT.setRoot(vCopy);
            createSubtreeRec(v, vCopy, sT, new Hashtable<Node, Node>());
        }
        // network is created out of sT
        HybridNetwork h = new HybridNetwork(sT, false, taxaOrdering);
        if (doUpdate)
            h.update();
        return h;
    }

    private void createSubtreeRec(Node v, Node vCopy, PhyloTree t, Hashtable<Node, Node> created) {
        for (Edge e : v.outEdges()) {
            Node c = e.getTarget();
            Node cCopy;
            if (!created.containsKey(c)) {
                cCopy = t.newNode(c);
                t.setLabel(cCopy, getLabel(c));
                created.put(c, cCopy);
                Edge eCopy = t.newEdge(vCopy, cCopy);
                if (isSpecial(e)) {
                    t.setSpecial(eCopy, true);
                    t.setWeight(eCopy, 0);
                }
                createSubtreeRec(c, cCopy, t, created);
            } else {
                cCopy = created.get(c);
                Edge eCopy = t.newEdge(vCopy, cCopy);
                if (isSpecial(e)) {
                    t.setSpecial(eCopy, true);
                    t.setWeight(eCopy, 0);
                }
            }
        }
    }

    public boolean contains(Node v) {
        Iterator<Node> it = postOrderWalk();
        while (it.hasNext()) {
            if (it.next().equals(v))
                return true;
        }
        return false;
    }

    public Iterator<Node> getSuccessors(Node v) {
        Iterator<Edge> it = v.getOutEdges();
        Vector<Node> successors = new Vector<>();
        while (it.hasNext()) {
            successors.add(it.next().getTarget());
        }
        return successors.iterator();
    }

    // deletes outgroup 'rho'
    public void removeOutgroup() {
        for (Node v : computeSetOfLeaves()) {
            if (getLabel(v).equals("rho")) {
                Edge e = v.getInEdges().next();
                Node p = e.getSource();
                deleteEdge(e);
                deleteNode(v);
                e = p.getOutEdges().next();
                Node c = e.getTarget();
                deleteEdge(e);
                deleteNode(p);
                setRoot(c);
                break;
            }
        }
        if (altLabel != null) {
            for (Node v : computeSetOfLeaves()) {
                if (getLabel(v).equals(altLabel))
                    setLabel(v, "rho");
            }
        }
        initTaxaOrdering();
        update();
    }

    // all labelings of each inner-nodes are set to null
    public void clearLabelings() {
        Iterator<Node> it = postOrderWalk();
        while (it.hasNext()) {
            Node v = it.next();
            if (v.getOutDegree() != 0)
                this.setLabel(v, null);
        }
    }

    // replaces subtrees displaying a distinct set of cluster by a unique
    // taxon-labeling
    @SuppressWarnings("unchecked")
    public void replaceClusters(Vector<BitSet> clusters, ReplacementInfo rI)
            throws Exception {
        Vector<String> taxa = (Vector<String>) taxaOrdering.clone();
        for (BitSet cluster : clusters) {
            Node v = getClusterToNode().get(cluster);
            String label = changeLabel(taxa.get(cluster.nextSetBit(0)) + "'");

            PhyloTree subtree = getSubtree(v, true);

            Node newV = newNode();
            setLabel(newV, label);
            deleteSubtree(v, newV, false);

            rI.putLabelToSubtree(label, subtree);
        }
        initTaxaOrdering();
        update();
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
    public void deleteSubtree(Node v, Node newV, boolean doUpdate) {
        Node p = v.getInEdges().next().getSource();
        deleteSubtreeRec(v);
        if (newV != null) {
            newEdge(p, newV);
            initTaxaOrdering();
        }
        if (doUpdate)
            update();
    }

    private void deleteSubtreeRec(Node v) {
        Iterator<Node> it = getSuccessors(v);
        if (v.getOutDegree() != 0) {
            while (it.hasNext()) {
                Node child = it.next();
                deleteSubtreeRec(child);
            }
        }
        deleteEdge(v.getInEdges().next());
        deleteNode(v);
    }

    // finds the smallest node containing a distinct cluster
    public Node findLCA(BitSet cluster) {
        Iterator<Node> it = postOrderWalk();
        while (it.hasNext()) {
            Node v = it.next();
            BitSet vCluster = nodeToCluster.get(v);
            BitSet b = (BitSet) vCluster.clone();
            b.or(cluster);
            if (b.equals(vCluster))
                return v;
        }
        return null;
    }

    // returns true if the network is resolved
    private boolean isResolvedCheck() {
        if (isBifurcating() && isBicombining())
            return true;
        System.err.println("ERROR: Network not resolved: " + isBifurcating()
                + " " + isBicombining());
        return false;
    }

    public Vector<Node> getLeaves(Node v) {
        Vector<Node> leaves = new Vector<>();
        Iterator<Node> it = getSuccessors(v);
        while (it.hasNext()) {
            Node l = it.next();
            if (l.getOutDegree() == 0)
                leaves.add(l);
        }
        return leaves;
    }

    // returns true if the network is bicombining
    private boolean isBicombining() {
        Iterator<Node> it = nodeIterator();
        while (it.hasNext()) {
            Node v = it.next();
            if (!(getRoot().equals(v) || v.getInDegree() == 2 || v
                    .getInDegree() == 1))
                return false;
        }
        return true;
    }

    // returns the edge weight produced by the chain reduction step
    public int getEdgeWeight(Edge e) {
        Node s = e.getSource();
        Node t = e.getTarget();
        Vector<Node> sChilds = getLeaves(s);
        Vector<Node> tChilds = getLeaves(t);
        for (Node v : sChilds) {
            for (Node w : tChilds) {
                Vector<String> taxaPair = new Vector<>();
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
                Vector<String> taxaPair = new Vector<>();
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

    public Hashtable<Node, BitSet> getNodeToCluster() {
        if (nodeToCluster.keySet().size() == 0)
            initClusters();
        return nodeToCluster;
    }

    public Hashtable<BitSet, Node> getClusterToNode() {
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

    public Hashtable<Vector<String>, Integer> getTaxaPairToWeight() {
        return taxaPairToWeight;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public void setAltLabel(String altLabel) {
        this.altLabel = altLabel;
    }

    public void setTaxaOrdering(Vector<String> taxaOrdering) {
        this.taxaOrdering = taxaOrdering;
    }

    public void setTaxaPairToWeight(
            Hashtable<Vector<String>, Integer> taxaPairToWeight) {
        this.taxaPairToWeight = taxaPairToWeight;
    }

    public void setEdgeToWeight(Hashtable<Edge, Integer> edgeToWeight) {
        this.edgeToWeight = edgeToWeight;
    }

}
