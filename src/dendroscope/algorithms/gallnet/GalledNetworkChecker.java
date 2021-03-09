/*
 *   GalledNetworkChecker.java Copyright (C) 2020 Daniel H. Huson
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
package dendroscope.algorithms.gallnet;

import dendroscope.consensus.Cluster;
import dendroscope.consensus.Split;
import dendroscope.consensus.SplitSystem;
import dendroscope.consensus.Taxa;
import dendroscope.util.IntegerVariable;
import jloda.graph.*;
import jloda.phylo.PhyloTree;

import java.util.*;

/**
 * check whether a set of clusters is indeed displayed by a given galled network
 * Daniel Huson, 9.2008
 */
public class GalledNetworkChecker {
    private final PhyloTree tree;
    private EdgeArray edge2TaxonSets;


    /**
     * constructors a new galled network checker for the given tree
     *
     * @param tree
     */
    public GalledNetworkChecker(PhyloTree tree) {
        this.tree = tree;
        edge2TaxonSets = null;
    }

    /**
     * computes the edge to taxon sets mapping which is used to determine whether a given edge.
     * Each edge is given a set of taxon sets (as bitsets). The first set is always mandatory
     * and cannot be turned off. All other sets are optional sets that each can be turned completely
     * on or completely off. All sets have pairwise empty intersections
     * represents a given cluster
     *
     * @param tree
     * @return the edge to taxon sets mapping
     */
    private EdgeArray computeEdge2TaxonSets(PhyloTree tree) {
        EdgeArray edge2TaxonSets = new EdgeArray(tree);

        if (tree.getRoot() != null) {
            NodeArray node2taxaBelow = new NodeArray(tree);
            NodeArray node2nodesBelow = new NodeArray(tree);

            NodeArray node2leaves = new NodeArray(tree);
            NodeArray node2reticulate = new NodeArray(tree);

            // compute required node maps
            computeNodeMapsRec(tree.getRoot(), node2taxaBelow, node2nodesBelow, node2leaves, node2reticulate);

            // compute taxon sets for each non-reticulate edge
            for (Edge e = tree.getFirstEdge(); e != null; e = e.getNext()) {
                if (!tree.isSpecial(e)) {
                    BitSet alwaysOn = new BitSet();
                    Set optional = new HashSet();
                    computeAlwaysOnAndOptional(e.getTarget(), node2taxaBelow, node2nodesBelow, node2leaves, node2reticulate, alwaysOn, optional);
                    List edge2sets = new LinkedList();
                    edge2sets.add(alwaysOn);
                    edge2sets.addAll(optional);
                    edge2TaxonSets.put(e, edge2sets);
                }
            }

            // verify that sets are for any given edge all sets have empty intersections
            for (Edge e = tree.getFirstEdge(); e != null; e = e.getNext()) {
                if (!tree.isSpecial(e)) {
                    BitSet seen = new BitSet();
                    for (Object o : ((List) edge2TaxonSets.getValue(e))) {
                        BitSet set = (BitSet) o;
                        if (set.cardinality() > 0 && Cluster.contains(seen, set)) {
                            System.err.println("WARNING: error in GalledNetworkChecker");
                        } else
                            seen.or(set);
                    }
                }
            }
        }
        return edge2TaxonSets;
    }

    /**
     * recursively compute the required node maps
     *
     * @param v
     * @param node2AllTaxaBelow  all taxa on or below v
     * @param node2AllNodesBelow all nodes on or below v
     * @param node2leaves        leaves on or below v reachable by a path of tree edges
     * @param node2reticulate    reticlate  below v reachable by a path of tree edges
     */
    private void computeNodeMapsRec(Node v, NodeArray node2AllTaxaBelow, NodeArray node2AllNodesBelow, NodeArray node2leaves, NodeArray node2reticulate) {
        if (node2AllTaxaBelow.getValue(v) != null)
            return;   // already processed this node

        NodeSet nodesBelow = new NodeSet(tree);
        NodeSet leaves = new NodeSet(tree);
        NodeSet reticulate = new NodeSet(tree);
        BitSet taxaBelow = new BitSet();

        nodesBelow.add(v);

        if (v.getOutDegree() == 0)
            leaves.add(v);

        // todo: no, only below // if(v.getInDegree()>1) reticulate.add(v);

        for (Integer integer : tree.getTaxa(v)) {
            int t = integer;
            taxaBelow.set(t);
        }

        if (taxaBelow.cardinality() > 0 && v.getOutDegree() > 0)
            System.err.println("WARNING: problem in GalledNetworkChecker:Galled network has labeled non-leaf of out-degree="
                    + v.getOutDegree());

        // look at all children:
        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            Node w = e.getTarget();
            computeNodeMapsRec(w, node2AllTaxaBelow, node2AllNodesBelow, node2leaves, node2reticulate);

            nodesBelow.addAll((NodeSet) node2AllNodesBelow.getValue(w));

            if (tree.isSpecial(e)) // w is reticulate node
            {
                reticulate.add(w);
            } else // w is tree node
            {
                leaves.addAll((NodeSet) node2leaves.getValue(w));
                reticulate.addAll((NodeSet) node2reticulate.getValue(w));
            }

            taxaBelow.or((BitSet) node2AllTaxaBelow.getValue(w));
        }
        node2AllTaxaBelow.put(v, taxaBelow);
        node2AllNodesBelow.put(v, nodesBelow);
        node2leaves.put(v, leaves);
        node2reticulate.put(v, reticulate);

        if (!nodesBelow.containsAll(leaves))
            System.err.println("WARNING: error in GalledNetworkChecker: nodes below doesn't contain all leaves");
        if (!nodesBelow.containsAll(reticulate))
            System.err.println("WARNING: error in GalledNetworkChecker: nodes below doesn't contain all reticulate");
    }

    /**
     * for a node v, compute the set of taxa that are always on and compute all sets of optional taxa
     *
     * @param v
     * @param node2taxaBelow
     * @param node2leaves
     * @param node2reticulate
     * @param alwaysOn
     * @param optional
     */
    private void computeAlwaysOnAndOptional(Node v, NodeArray node2taxaBelow, NodeArray node2nodesBelow, NodeArray node2leaves, NodeArray node2reticulate, BitSet alwaysOn, Set optional) {

        NodeSet leaves = (NodeSet) node2leaves.getValue(v);
        for (Node l = leaves.getFirstElement(); l != null; l = leaves.getNextElement(l)) {
            BitSet taxa = (BitSet) node2taxaBelow.getValue(l);
            alwaysOn.or(taxa);
        }

        NodeSet reticulate = (NodeSet) node2reticulate.getValue(v);
        NodeSet belowV = (NodeSet) node2nodesBelow.getValue(v);

        for (Node r = reticulate.getFirstElement(); r != null; r = reticulate.getNextElement(r)) {
            boolean allBelow = true;
            for (Edge in = r.getFirstInEdge(); allBelow && in != null; in = r.getNextInEdge(in)) {
                if (!belowV.contains(in.getSource()))
                    allBelow = false;
            }
            BitSet taxa = (BitSet) node2taxaBelow.getValue(r);

            if (allBelow) // not optional
                alwaysOn.or(taxa);
            else // optional
                optional.add(taxa);
        }
    }

    /**
     * compute a labeling of all nodes by the tree component that they are contained in
     *
     * @return labeling of nodes by tree components
     */
    private NodeIntegerArray labelNodesByTreeComponents() {
        NodeIntegerArray node2component = new NodeIntegerArray(tree);

        IntegerVariable maxComponentNumber = new IntegerVariable(0);

        labelNodesByTreeComponentsRec(tree.getRoot(), null, maxComponentNumber, node2component);
        return node2component;

    }

    /**
     * recursively does the work in a pre-order fashion
     *
     * @param v
     * @param e
     * @param maxComponentNumber
     * @param node2component
     */
    private void labelNodesByTreeComponentsRec(Node v, Edge e, IntegerVariable maxComponentNumber, NodeIntegerArray node2component) {
        if (e == null || tree.isSpecial(e))        // either root or reticulate node, start a new component
            node2component.set(v, maxComponentNumber.increment());
        else            // in same component, keep number
            node2component.set(v, node2component.getValue(e.getSource()));

        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f))  // visit all children
        {
            Node w = f.getTarget();
            if (node2component.getValue(w) == null) // not yet visited
                labelNodesByTreeComponentsRec(w, f, maxComponentNumber, node2component);
        }
    }

    /**
     * determines whether network is a galled network, that is, that every reticulation has a tree cycle for each pair
     * of in-edges
     *
     * @return the number of problems encountered
     */
    public int isGalledNetwork() {
        int problems = 0;
        NodeIntegerArray node2component = labelNodesByTreeComponents();
        for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
            if (v.getInDegree() > 1) {
                int parentComponentNumber = -1;
                for (Edge e = v.getFirstInEdge(); e != null; e = v.getNextInEdge(e)) {
                    if (parentComponentNumber == -1)
                        parentComponentNumber = node2component.getValue(e.getSource());
                    else if (parentComponentNumber != node2component.getValue(e.getSource())) {
                        problems++;
                        System.err.println("WARNING: galled network property does not hold for node: " + v);
                        tree.setLabel(v, "BAD");
                    }
                }
            }
        }
        return problems;
    }

    /**
     * does network contain all clusters
     *
     * @param clusters
     * @return true, if contains all
     */
    public boolean containsAll(Cluster[] clusters) {
        for (Cluster cluster : clusters) {
            if (!contains(cluster)) {
                System.err.println("Check galled tree: not found: " + cluster);
                return false;
            }
        }
        return true;
    }

    /**
     * is the cluster contained in the network?
     *
     * @param cluster
     * @return true, if contained
     */
    public boolean contains(BitSet cluster) {
        for (Edge e = tree.getFirstEdge(); e != null; e = e.getNext()) {
            if (!tree.isSpecial(e) && contains(e, cluster))
                return true;
        }
        return false;
    }

    /**
     * does the given edge represent or "contain" the given cluster
     *
     * @param e
     * @param cluster
     * @return true, if e represents the cluster
     */
    public boolean contains(Edge e, BitSet cluster) {
        if (edge2TaxonSets == null)
            edge2TaxonSets = computeEdge2TaxonSets(tree);

        if (tree.isSpecial(e)) {
            throw new RuntimeException("illegal to method on special edge");
        }
        List taxonSets = (List) edge2TaxonSets.getValue(e);
        BitSet seen = new BitSet();

        boolean first = true;
        for (Object taxonSet : taxonSets) {
            BitSet set = (BitSet) taxonSet;
            if (first) {
                if (!Cluster.contains(cluster, set))
                    return false;
                seen.or(set);
                first = false;
            } else // variable "set" must either be contained in cluster or be disjoint from cluster
            {
                if (Cluster.contains(cluster, set))
                    seen.or(set);
                else if (cluster.intersects(set))
                    return false; // optional set that contains some memebers and some non members
            }
        }
        return Cluster.contains(seen, cluster); // return true, if we saw all taxa
    }

    /*
    public boolean contains (PhyloTree tree)
    {
        return false;
    }
     */

    /**
     * check that galled network contains all splits
     *
     * @param splits
     * @return number found
     */
    public int containsAll(SplitSystem splits, Taxa taxa) {
        int count = 0;
        for (Iterator it = splits.iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();

            if (contains(split.getA()) || contains(split.getB())) {
                count++;
            } else {
                System.err.println("Galled network does not contain split: " + split);
                {
                    BitSet A = split.getA();
                    StringBuffer buf = new StringBuffer();
                    boolean first = true;
                    buf.append("A: ");
                    for (int t = A.nextSetBit(0); t != -1; t = A.nextSetBit(t + 1)) {
                        if (first)
                            first = false;
                        else
                            buf.append("|");
                        buf.append(taxa.getLabel(t));
                    }
                    System.err.println(buf);
                }
                {
                    BitSet B = split.getB();
                    StringBuffer buf = new StringBuffer();
                    boolean first = true;
                    buf.append("B: ");
                    for (int t = B.nextSetBit(0); t != -1; t = B.nextSetBit(t + 1)) {
                        if (first)
                            first = false;
                        else
                            buf.append("|");
                        buf.append(taxa.getLabel(t));
                    }
                    System.err.println(buf);
                }
            }
        }
        return count;
    }
}
