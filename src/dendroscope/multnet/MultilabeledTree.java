/*
 *   MultilabeledTree.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.multnet;

import dendroscope.consensus.SplitSystem;
import dendroscope.consensus.Taxa;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;

import java.util.HashMap;

/**
 * This class represents MUL-trees
 *
 * @author thomas bonfert, 6.2009
 */

public class MultilabeledTree extends PhyloTree {


    protected HashMap<Integer, Multiset> allMultisets;
    protected HashMap<Integer, Integer> allNodeHeights;


    public MultilabeledTree(PhyloTree t) {
        super();
        this.copy(t);
        this.adaptLabeling();
        this.allMultisets = collectAllMultisets();
        this.allNodeHeights = collectAllNodeHeights();

    }

    public MultilabeledTree() {
        super();
    }

    /**
     * searches for labels which occur more than once and changes these labels back from "a.1", "a.2",..., to "a", etc.
     */
    public void adaptLabeling() {
        NodeSet leaveSet = this.computeSetOfLeaves();
        for (Node n : leaveSet) {
            String label = this.getLabel(n);
            //maybe we've found a multilabeled node.
            if (label.contains(".") && !n.equals(leaveSet.getLastElement())) {
                //check if we have an integer behind the last "."
                try {
                    int i = Integer.parseInt(label.substring(label.lastIndexOf(".") + 1));
                    Node secondNode = leaveSet.getNextElement(n);
                    while (secondNode != null) {
                        String secondLabel = this.getLabel(secondNode);
                        if (secondLabel.contains(".")) {
                            if (label.substring(0, label.lastIndexOf(".")).equals
                                    (secondLabel.substring(0, secondLabel.lastIndexOf(".")))) {
                                this.setLabel(n, label.substring(0, label.lastIndexOf(".")));
                                this.setLabel(secondNode, label.substring(0, label.lastIndexOf(".")));
                            }
                        }
                        secondNode = leaveSet.getNextElement(secondNode);
                    }
                }
                //the node  was not multi-labeled.
                catch (Exception ignored) {
				}
            }
        }
    }


    /**
     * makes a depth-first search starting at node v.
     * if the multi-labeled tree has no reticulation, the nested labels of v and all descending nodes of v will be calculated.
     * otherwise the multiset for v and for the descending nodes will be calculated. these multisets can contain
     * nested labels of descending nodes of v several times because of the reticulation edges. -> could be used to calculate
     * the nested labeled distance between networks and multi-labeled trees.
     */
    public Multiset getMultisets(Node v) {
        Multiset clusterSet = new Multiset();
        getMultisetsRec(v, clusterSet);
        return clusterSet;
    }

    private Multiset getMultisetsRec(Node v, Multiset clusterSet) {
        //reached a leave
        if (this.computeSetOfLeaves().contains(v)) {
            Multiset cluster = new Multiset();
            cluster.setConcatenatedElements(this.getLabel(v));
            cluster.add(this.getLabel(v));
            clusterSet.addSorted(cluster);
            return cluster;
        }

		boolean hasNormalEdge = false;
        for (Edge e : v.inEdges()) {
            if (!this.isReticulateEdge(e)) {
				hasNormalEdge = true;
				break;
			}
        }
        if (v.getOutDegree() == 1 && !hasNormalEdge) {
            Node w = this.getTarget(v.getFirstOutEdge());
            return (getMultisetsRec(w, clusterSet));
        }

        //recursion
        else {
            Multiset cluster = new Multiset();
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                Node w = f.getTarget();
                cluster.addSorted(getMultisetsRec(w, clusterSet));
            }
            cluster.setConcatenatedElements();
            clusterSet.addSorted(cluster);
            return cluster;
        }
    }

    /**
     * calculates for every node in the tree its nested label.
     */
    protected HashMap<Integer, Multiset> collectAllMultisets() {
        HashMap<Integer, Multiset> allMultisets = new HashMap<>(this.getNumberOfNodes());
        collectAllMultisetsRec(this.getRoot(), allMultisets);
        return allMultisets;
    }

    private Multiset collectAllMultisetsRec(Node v, HashMap<Integer, Multiset> allMultisets) {
        //reached a leave
        if (this.computeSetOfLeaves().contains(v)) {
            Multiset cluster = new Multiset();
            cluster.setConcatenatedElements(this.getLabel(v));
            cluster.add(this.getLabel(v));
            allMultisets.put(this.getId(v), cluster);
            return cluster;
        }
        //recursion
        else {
            Multiset cluster = new Multiset();
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                Node w = f.getTarget();
                cluster.addSorted(collectAllMultisetsRec(w, allMultisets));
            }
            cluster.setConcatenatedElements();
            allMultisets.put(this.getId(v), cluster);
            return cluster;
        }
    }

    /**
     * collects the height of every node in the tree
     */
    protected HashMap<Integer, Integer> collectAllNodeHeights() {
        HashMap<Integer, Integer> heights = new HashMap<>();
        collectAllNodeHeightsRec(this.getRoot(), heights);
        return heights;
    }

    private Integer collectAllNodeHeightsRec(Node v, HashMap<Integer, Integer> heights) {
        if (this.computeSetOfLeaves().contains(v)) {
            heights.put(v.getId(), 0);
            return 0;
        }
        //recursion
        else {
            int maxHeight = 0;
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                Node w = f.getTarget();
                int currentHeight = 1 + collectAllNodeHeightsRec(w, heights);
                if (currentHeight > maxHeight) maxHeight = currentHeight;
            }
            int heightV = maxHeight;
            heights.put(v.getId(), heightV);
            return heightV;
        }
    }


    public Taxa getAllTaxa() {
        Taxa taxa = new Taxa();
        for (Node leave : this.computeSetOfLeaves()) {
            taxa.add(this.getLabel(leave));
        }
        return taxa;
    }

    public SplitSystem getSplitSystem() {
        return (new SplitSystem(getAllTaxa(), this));
    }


    /**
     * calculates the subtree T(v).
     */
    public MultilabeledTree getSubtree(Node v) {
        MultilabeledTree subTree = new MultilabeledTree();
        NodeSet subtreeNodes = getDescendingNodes(v);
        NodeSet collapsedNodes = new NodeSet(this);
        PhyloTree newTree = dendroscope.util.PhyloTreeUtils.extractInducedSubtree(this,
                subtreeNodes, collapsedNodes, true);
        subTree.copy(newTree);
        System.out.println();
        return subTree;
    }

    /**
     * deletes the subtree T(v), including v and the incoming edges to v.
     */
    public void deleteSubtree(Node v) {
        NodeSet collapsedNodes = getDescendingNodes(v);
        //delete the in edges of v.
        for (Edge e : v.inEdges()) {
            this.deleteEdge(e);
        }
        for (Node toDel : collapsedNodes) {
            for (Edge e : toDel.outEdges()) {
                this.deleteEdge(e);
            }
            this.deleteNode(toDel);
        }
    }

    /**
     * calculates all descending nodes of v, including v.
     */
    public NodeSet getDescendingNodes(Node v) {
        NodeSet nodes = new NodeSet(this);
        nodes.add(v);
        getDescendingNodesRec(v, nodes);
        return nodes;
    }

    private void getDescendingNodesRec(Node v, NodeSet nodes) {
        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            Node w = f.getTarget();
            nodes.add(w);
            getDescendingNodesRec(w, nodes);
        }
    }

    /**
     * calculates all ascending nodes of v, excluding v.
     */
    public NodeSet getAscendingNodes(Node v) {
        NodeSet nodes = new NodeSet(this);
        getAscendingNodesRec(v, nodes);
        return nodes;
    }

    private void getAscendingNodesRec(Node v, NodeSet nodes) {
        Edge e = v.getFirstInEdge();
        Node w = e.getSource();
        if (!w.equals(this.getRoot())) {
            nodes.add(w);
            getAscendingNodesRec(w, nodes);
        }
    }

    /**
     * simply returns the multiset of for node n.
     * only use this method with a network generated by the exact method.
     */
    public Multiset getMultiset(Node n) {
        return this.allMultisets.get(n.getId());
    }

    public int getHeight(Node n) {
        return this.allNodeHeights.get(n.getId());
    }

    public int getNumberOfSpecialNodes() {
        int numberOfSpecialNodes = 0;
        for (Node v = this.getFirstNode(); v != null; v = v.getNext())
            if (v.getInDegree() > 1)
                numberOfSpecialNodes++;
        return numberOfSpecialNodes;
    }

    /**
     * add a formal outgroup
     */
    public void addOutgroup() {
        Node v = newNode();
        setLabel(v, "outgroup");
        newEdge(getRoot(), v);
    }
}
