/**
 * Copyright 2015, Daniel Huson
 *
 *(Some files contain contributions from other authors, who are then mentioned separately)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package dendroscope.consensus;

import dendroscope.core.Document;
import dendroscope.core.TreeData;
import jloda.graph.*;
import jloda.phylo.PhyloTree;
import jloda.util.CanceledException;

import java.util.*;

/**
 * computes the LSA tree from a reticulate network
 * Daniel Huson, 12.2007
 */

/**
 * Computes the lsa consensus
 * Daniel Huson, 7.2007
 */
public class LSATree implements IConsensusTreeMethod {
    public static final String NAME = "LSAtree";

    /**
     * constructor
     */
    public LSATree() {
        super();
    }

    /**
     * applies the  distortion-1 consensus method to obtain a tree
     *
     * @return consensus
     */
    public PhyloTree apply(Document doc, TreeData[] trees) throws CanceledException {
        doc.notifyTasks("LSA consensus", "");
        ZClosure zclosure = new ZClosure();
        System.err.println("LSA consensus input trees:" + trees.length);

        SplitSystem splits = zclosure.apply(doc.getProgressListener(), trees);
        Taxa taxa = zclosure.getTaxa();

        System.err.println("LSA consensus splits: " + splits.size());
        PhyloTree network = splits.createTreeFromSplits(taxa, false, doc.getProgressListener());

        PhyloTree tree = computeLSA(network);
        tree.setName("lsa-consensus");
        return tree;
    }

    /**
     * given a reticulate network, returns the LSA tree
     *
     * @param network
     * @return LSA tree
     */
    public static PhyloTree computeLSA(PhyloTree network) {
        PhyloTree tree = (PhyloTree) network.clone();

        if (tree.getRoot() != null) {
            // first we compute the reticulate node to lsa node mapping:
            LSATree lsaTree = new LSATree();
            NodeArray<Node> reticulation2LSA = new NodeArray<>(tree);
            lsaTree.computeReticulation2LSA(tree, reticulation2LSA);

            NodeDoubleArray reticulation2LSAEdgeLength = lsaTree.computeReticulation2LSAEdgeLength(tree);

            // check that all reticulation nodes have a LSA:
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                if (v.getInDegree() >= 2) {
                    Node lsa = reticulation2LSA.get(v);
                    if (lsa == null)
                        System.err.println("WARNING: no LSA found for node: " + v);
                }
            }

            List<Edge> toDelete = new LinkedList<>();
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                Node lsa = reticulation2LSA.get(v);

                if (lsa != null) {
                    for (Edge e = v.getFirstInEdge(); e != null; e = v.getNextInEdge(e))
                        toDelete.add(e);
                    Edge e = tree.newEdge(lsa, v);
                    tree.setWeight(e, reticulation2LSAEdgeLength.getValue(v));
                    // System.err.println("WEIGHT: " + (float) reticulation2LSAEdgeLength.getValue(v));
                    // tree.setLabel(v,tree.getLabel(v)!=null?tree.getLabel(v)+"/"+(float)tree.getWeight(e):""+(float)tree.getWeight(e));
                }
            }
            for (Edge e : toDelete)
                tree.deleteEdge(e);

            boolean changed = true;
            while (changed) {
                changed = false;
                List<Node> falseLeaves = new LinkedList<>();
                for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                    if (v.getInDegree() == 1 && v.getOutDegree() == 0 && (tree.getLabel(v) == null || tree.getLabel(v).length() == 0))
                        falseLeaves.add(v);
                }
                if (falseLeaves.size() > 0) {
                    for (Node u : falseLeaves)
                        tree.deleteNode(u);
                    changed = true;
                }

                List<Node> divertices = new LinkedList<>();
                for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                    if (v.getInDegree() == 1 && v.getOutDegree() == 1 && v != tree.getRoot() && (tree.getLabel(v) == null || tree.getLabel(v).length() == 0))
                        divertices.add(v);
                }
                if (divertices.size() > 0) {
                    for (Node u : divertices)
                        tree.delDivertex(u);
                    changed = true;
                }
            }
        }

        // make sure special attribute is set correctly:
        for (Edge e = tree.getFirstEdge(); e != null; e = e.getNext()) {
            boolean shouldBe = e.getTarget().getInDegree() > 1;
            if (shouldBe != tree.isSpecial(e)) {
                System.err.println("WARNING: bad special state, fixing (to: " + shouldBe + ") for e=" + e);
                tree.setSpecial(e, shouldBe);
            }
        }
        // making sure leaves have labels:

        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            {
                if (v.getOutDegree() == 0 && (tree.getLabel(v) == null || tree.getLabel(v).trim().length() == 0)) {
                    System.err.println("WARNING: adding label to naked leaf: " + v);
                    tree.setLabel(v, "V" + v.getId());
                }
            }
        }
        //System.err.println("tree: " + tree.toBracketString());
        return tree;
    }

    /**
     * given a reticulate network, returns the LSA tree
     *
     * @param network
     * @return LSA tree
     */
    public static PhyloTree computeLSATreeKeeping(PhyloTree network) {
        PhyloTree tree = (PhyloTree) network.clone();

        if (tree.getRoot() != null) {
            // first we compute the reticulate node to lsa node mapping:
            LSATree lsaTree = new LSATree();
            NodeArray reticulation2LSA = new NodeArray(tree);
            lsaTree.computeReticulation2LSA(tree, reticulation2LSA);

            NodeDoubleArray reticulation2LSAEdgeLength = lsaTree.computeReticulation2LSAEdgeLength(tree);

            // check that all reticulation nodes have a LSA:
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                if (v.getInDegree() >= 2) {
                    Node lsa = (Node) reticulation2LSA.get(v);
                    if (lsa == null)
                        System.err.println("WARNING: no LSA found for node: " + v);
                }
            }

            List toDelete = new LinkedList();
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                Node lsa = (Node) reticulation2LSA.get(v);

                if (lsa != null) {
                    for (Edge e = v.getFirstInEdge(); e != null; e = v.getNextInEdge(e))
                        toDelete.add(e);
                    Edge e = tree.newEdge(lsa, v);
                    tree.setWeight(e, reticulation2LSAEdgeLength.getValue(v));
                    // System.err.println("WEIGHT: " + (float) reticulation2LSAEdgeLength.getValue(v));
                    // tree.setLabel(v,tree.getLabel(v)!=null?tree.getLabel(v)+"/"+(float)tree.getWeight(e):""+(float)tree.getWeight(e));
                }
            }
            for (Object aToDelete : toDelete) tree.deleteEdge((Edge) aToDelete);

            boolean changed = true;
            while (changed) {
                changed = false;
                List falseLeaves = new LinkedList();
                for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                    if (v.getInDegree() == 1 && v.getOutDegree() == 0 && (tree.getLabel(v) == null || tree.getLabel(v).length() == 0))
                        falseLeaves.add(v);
                }
                if (falseLeaves.size() > 0) {
                    for (Object falseLeave : falseLeaves) tree.deleteNode((Node) falseLeave);
                    changed = true;
                }

                List divertices = new LinkedList();
                for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                    if (v.getInDegree() == 1 && v.getOutDegree() == 1 && v != tree.getRoot() && (tree.getLabel(v) == null || tree.getLabel(v).length() == 0))
                        divertices.add(v);
                }
                if (divertices.size() > 0) {
                    for (Object divertice : divertices) tree.delDivertex((Node) divertice);
                    changed = true;
                }
            }
        }

        // make sure special attribute is set correctly:
        for (Edge e = tree.getFirstEdge(); e != null; e = e.getNext()) {
            boolean shouldBe = e.getTarget().getInDegree() > 1;
            if (shouldBe != tree.isSpecial(e)) {
                System.err.println("WARNING: bad special state, fixing (to: " + shouldBe + ") for e=" + e);
                tree.setSpecial(e, shouldBe);
            }
        }
        // making sure leaves have labels:

        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            {
                if (v.getOutDegree() == 0 && (tree.getLabel(v) == null || tree.getLabel(v).trim().length() == 0)) {
                    System.err.println("WARNING: adding label to naked leaf: " + v);
                    tree.setLabel(v, "V" + v.getId());
                }
            }
        }
        //System.err.println("tree: " + tree.toBracketString());
        return tree;
    }


    /**
     * given a reticulate network, returns a mapping of each node to a list of its children in the LSA tree
     *
     * @param tree
     */
    public static NodeArray computeLSAOrdering(PhyloTree tree) {
        NodeArray<Node> reticulation2LSA = new NodeArray<>(tree);
        computeLSAOrdering(tree, reticulation2LSA);
        return reticulation2LSA;
    }

    /**
     * given a reticulate network, returns a mapping of each node to a list of its children in the LSA tree
     *
     * @param tree
     * @param reticulation2LSA is returned here
     */
    public static void computeLSAOrdering(PhyloTree tree, NodeArray<Node> reticulation2LSA) {
        tree.getNode2GuideTreeChildren().clear();

        if (tree.getRoot() != null) {
            // first we compute the reticulate node to lsa node mapping:
            LSATree lsaTree = new LSATree();
            lsaTree.computeReticulation2LSA(tree, reticulation2LSA);

            for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                List<Node> children = new LinkedList<>();
                for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                    if (!tree.isSpecial(e))
                        children.add(e.getTarget());
                }
                tree.getNode2GuideTreeChildren().set(v, children);
            }
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                Node lsa = reticulation2LSA.get(v);
                if (lsa != null)
                    tree.getNode2GuideTreeChildren().get(lsa).add(v);
            }
        }
    }


    /**
     * recursively determine the number of edges between a reticulation and its lsa
     *
     * @param tree
     * @param reticulation2LSA
     * @return number of edges between a reticulation and its lsa
     */
    private static NodeIntegerArray computeReticulationSize(PhyloTree tree, NodeArray reticulation2LSA) {
        NodeIntegerArray rSize = new NodeIntegerArray(tree);

        for (Node r = tree.getFirstNode(); r != null; r = r.getNext()) {
            Node lsa = (Node) reticulation2LSA.get(r);
            if (lsa != null) {
                System.err.println("lsa: " + lsa + " r: " + r);
                EdgeSet visited = new EdgeSet(tree);
                computeReticulationSizeRec(r, lsa, visited);
                rSize.set(r, visited.size());
            }
        }

        return rSize;
    }

    /**
     * recursively count edges from r upto lsa
     *
     * @param r
     * @param lsa
     * @param visited
     */
    private static void computeReticulationSizeRec(Node r, Node lsa, EdgeSet visited) {
        for (Edge e = r.getFirstInEdge(); e != null; e = r.getNextInEdge(e)) {
            if (!visited.contains(e) && e.getSource() != lsa) {
                visited.add(e);
                computeReticulationSizeRec(e.getSource(), lsa, visited);
            }
        }
    }


    NodeArray<BitSet> ret2PathSet;
    NodeArray<EdgeArray<BitSet>> ret2Edge2PathSet;
    NodeArray<Node> reticulation2LSA;
    NodeArray<Set<Node>> node2below;

    /**
     * compute the reticulate node to lsa node mapping
     *
     * @param network
     * @param reticulation2LSA
     */
    public void computeReticulation2LSA(PhyloTree network, NodeArray<Node> reticulation2LSA) {
        reticulation2LSA.clear();
        ret2PathSet = new NodeArray<>(network);
        ret2Edge2PathSet = new NodeArray<>(network);
        this.reticulation2LSA = reticulation2LSA;
        node2below = new NodeArray<>(network); // set of reticulation nodes below a given node

        computeReticulation2LSARec(network, network.getRoot());
    }

    /**
     * recursively compute the mapping of reticulate nodes to their lsa nodes
     *
     * @param tree
     * @param v
     */
    private void computeReticulation2LSARec(PhyloTree tree, Node v) {
        if (v.getInDegree() > 1) // this is a reticulate node, add paths to node and incoming edges
        {
            // setup new paths for this node:
            EdgeArray<BitSet> edge2PathSet = new EdgeArray<>(tree);
            ret2Edge2PathSet.set(v, edge2PathSet);
            BitSet pathsForR = new BitSet();
            ret2PathSet.set(v, pathsForR);
            //  assign a different path number to each in-edge:
            int pathNum = 0;
            for (Edge e = v.getFirstInEdge(); e != null; e = v.getNextInEdge(e)) {
                pathNum++;
                pathsForR.set(pathNum);
                BitSet pathsForEdge = new BitSet();
                pathsForEdge.set(pathNum);
                edge2PathSet.set(e, pathsForEdge);
            }
        }

        Set<Node> reticulationsBelow = new HashSet<>(); // set of all reticulate nodes below v
        node2below.set(v, reticulationsBelow);

        // visit all children and determine all reticulations below this node
        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            Node w = f.getTarget();
            if (node2below.get(w) == null) // if haven't processed child yet, do it:
                computeReticulation2LSARec(tree, w);
            reticulationsBelow.addAll(node2below.get(w));
            if (w.getInDegree() > 1)
                reticulationsBelow.add(w);
        }

        // check whether this is the lsa for any of the reticulations below v
        // look at all reticulations below v:
        List<Node> toDelete = new LinkedList<>();
        for (Node r : reticulationsBelow) {
            // determine which paths from the reticulation lead to this node
            EdgeArray edge2PathSet = ret2Edge2PathSet.get(r);
            BitSet paths = new BitSet();
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                BitSet eSet = (BitSet) edge2PathSet.get(f);
                if (eSet != null)
                    paths.or(eSet);

            }
            BitSet alive = ret2PathSet.get(r);
            if (paths.equals(alive)) // if the set of paths equals all alive paths, v is lsa of r
            {
                reticulation2LSA.set(r, v);
                toDelete.add(r); // don't need to consider this reticulation any more
            }
        }
        // don't need to consider reticulations for which lsa has been found:
        for (Node u : toDelete)
            reticulationsBelow.remove(u);

        // all paths are pulled up the first in-edge"
        if (v.getInDegree() >= 1) {
            for (Node r : reticulationsBelow) {
                // determine which paths from the reticulation lead to this node
                EdgeArray<BitSet> edge2PathSet = ret2Edge2PathSet.get(r);

                BitSet newSet = new BitSet();

                for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                    BitSet pathSet = edge2PathSet.get(e);
                    if (pathSet != null)
                        newSet.or(pathSet);
                }
                edge2PathSet.set(v.getFirstInEdge(), newSet);
            }
        }
        // open new paths on all additional in-edges:
        if (v.getInDegree() >= 2) {
            for (Node r : reticulationsBelow) {
                BitSet existingPathsForR = ret2PathSet.get(r);

                EdgeArray<BitSet> edge2PathSet = ret2Edge2PathSet.get(r);
                // start with the second in edge:
                for (Edge e = v.getNextInEdge(v.getFirstInEdge()); e != null; e = v.getNextInEdge(e)) {
                    BitSet pathsForEdge = new BitSet();
                    int pathNum = existingPathsForR.nextClearBit(1);
                    existingPathsForR.set(pathNum);
                    pathsForEdge.set(pathNum);
                    edge2PathSet.set(e, pathsForEdge);
                }
            }
        }
    }

    NodeArray<NodeDoubleArray> ret2Node2Length;
    NodeDoubleArray ret2length;

    /**
     * computes the reticulation 2 lsa edge length map, after running the lsa computation
     *
     * @param tree
     * @return mapping from reticulation nodes to the edge lengths
     */
    private NodeDoubleArray computeReticulation2LSAEdgeLength(PhyloTree tree) {
        ret2Node2Length = new NodeArray<>(tree);
        ret2length = new NodeDoubleArray(tree);

        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            if (v.getInDegree() > 1)
                ret2Node2Length.set(v, new NodeDoubleArray(tree));
            // if(v.getOutDegree()>0) tree.setLabel(v,""+v.getId());
        }

        computeReticulation2LSAEdgeLengthRec(tree, tree.getRoot(), new NodeSet(tree));
        return ret2length;
    }

    /**
     * recursively does the work
     *
     * @param tree
     * @param v
     * @param visited
     */
    private void computeReticulation2LSAEdgeLengthRec(PhyloTree tree, Node v, NodeSet visited) {
        if (!visited.contains(v)) {
            visited.add(v);

            Set<Node> reticulationsBelow = new HashSet<>();

            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                computeReticulation2LSAEdgeLengthRec(tree, f.getTarget(), visited);
                reticulationsBelow.addAll(node2below.get(f.getTarget()));
            }

            reticulationsBelow.removeAll(node2below.get(v)); // because reticulations mentioned here don't hve v as LSA

            for (Node r : reticulationsBelow) {
                NodeDoubleArray node2Dist = ret2Node2Length.get(r);
                double length = 0;
                for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                    Node w = f.getTarget();
                    length += node2Dist.getValue(w);
                    if (!tree.isSpecial(f))
                        length += tree.getWeight(f);
                }
                if (v.getOutDegree() > 0)
                    length /= v.getOutDegree();
                node2Dist.set(v, length);
                if (reticulation2LSA.get(r) == v)
                    ret2length.set(r, length);
            }
        }
    }
}
