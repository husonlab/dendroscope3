/**
 * ClusterReduction.java 
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
package dendroscope.autumn.hybridnetwork;

import dendroscope.autumn.PostProcess;
import dendroscope.autumn.PreProcess;
import dendroscope.autumn.Root;
import dendroscope.consensus.Taxa;
import dendroscope.core.TreeData;
import jloda.graph.Edge;
import jloda.graph.Graph;
import jloda.graph.Node;
import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.Single;

import java.io.IOException;
import java.util.*;

/**
 * given two multifurcating, refined, subtree-reduce trees, performs a mininum cluster reduction
 * Daniel Huson, 4.2011
 */
public class ClusterReduction {
    public static boolean checking = false;

    /**
     * cluster-reduce two trees, if possible
     *
     * @param tree1
     * @param tree2
     * @param merge
     * @return subtree-reduced trees followed by all reduced subtrees
     */
    public static TreeData[] apply(TreeData tree1, TreeData tree2, Set<String> selectedLabels, boolean merge) throws IOException {
        // setup rooted trees with nodes labeled by taxa ids
        Taxa allTaxa = new Taxa();
        Pair<Root, Root> roots = PreProcess.apply(tree1, tree2, allTaxa);

        Root root1 = roots.getFirst();
        Root root2 = roots.getSecond();

        if (selectedLabels != null) {
            BitSet selectedTaxa = new BitSet();
            for (String label : selectedLabels) {
                int id = allTaxa.indexOf(label);
                if (id != -1) {
                    selectedTaxa.set(id);
                    RemoveTaxon.apply(root1, 1, id);
                    RemoveTaxon.apply(root2, 2, id);
                }
            }
        }

        // run the algorithm
        Pair<Root, Root> pair = apply(root1, root2, new Single<Integer>());

        if (pair != null) {
            List<Root> results = new LinkedList<Root>();
            results.add(root1);
            results.add(root2);
            results.add(pair.getFirst());
            results.add(pair.getSecond());


            if (merge) {// for debugging purposes, we then merge the common subtrees back into the main tree
                Root newRoot1 = root1.copySubNetwork();
                Root newRoot2 = root2.copySubNetwork();
                List<Root> allMerged = new LinkedList<Root>();

                List<Root> merged1 = MergeNetworks.apply(Arrays.asList(newRoot1), Arrays.asList(pair.getFirst()));
                List<Root> merged2 = MergeNetworks.apply(Arrays.asList(newRoot2), Arrays.asList(pair.getSecond()));
                results.addAll(merged1);
                results.addAll(merged2);
            }

            // convert data-structures to final trees
            List<TreeData> result = PostProcess.apply(results.toArray(new Root[results.size()]), allTaxa, true);
            return result.toArray(new TreeData[result.size()]);
        } else
            return null;
    }

    /**
     * finds a pair nodes for minimal cluster reduction, if one exists
     *
     * @param v1
     * @param v2
     * @return two reduced clusters or null
     */
    public static Pair<Root, Root> apply(Root v1, Root v2, Single<Integer> placeHolder) {

        Pair<Root, Root> pair = applyRec(v1.getTaxa(), v1, v2, new HashSet<Pair<Node, Node>>(), placeHolder);
        if (!v1.getTaxa().equals(v2.getTaxa()))
            throw new RuntimeException("Unequal taxon sets: " + Basic.toString(v1.getTaxa()) + " vs " + Basic.toString(v2.getTaxa()));

        // reorder should not be necessary
        // v1.reorderSubTree();
        // v2.reorderSubTree();


        if (pair != null && !pair.getFirst().getTaxa().equals(pair.getSecond().getTaxa()))
            throw new RuntimeException("Unequal taxon sets: " + Basic.toString(pair.getFirst().getTaxa()) + " vs " + Basic.toString(pair.getSecond().getTaxa()));

        return pair;
    }

    /**
     * recursively does the work
     *
     * @param root1
     * @param root2
     * @param compared
     * @param placeHolder
     * @return two reduced clusters or null
     */
    private static Pair<Root, Root> applyRec(BitSet taxa, Root root1, Root root2, Set<Pair<Node, Node>> compared, Single<Integer> placeHolder) {
        Pair<Node, Node> nodePair = new Pair<Node, Node>(root1, root2);
        if (compared.contains(nodePair))
            return null;
        else
            compared.add(nodePair);

        // System.err.println("reduceClusterRec v1=" + Basic.toString((v1.getTaxa()) + " v2=" + Basic.toString(v2.getTaxa());

        BitSet X = root1.getTaxa();
        BitSet Y = root2.getTaxa();

        // recursively process all children
        for (Edge e1 = root1.getFirstOutEdge(); e1 != null; e1 = root1.getNextOutEdge(e1)) {
            Root u1 = (Root) e1.getTarget();
            if (u1.getTaxa().intersects(Y)) {
                Pair<Root, Root> pair = applyRec(taxa, u1, root2, compared, placeHolder);
                if (pair != null)
                    return pair;
            }
        }
        for (Edge e2 = root2.getFirstOutEdge(); e2 != null; e2 = root2.getNextOutEdge(e2)) {
            Root u2 = (Root) e2.getTarget();
            if (u2.getTaxa().intersects(X)) {
                Pair<Root, Root> pair = applyRec(taxa, root1, u2, compared, placeHolder);
                if (pair != null)
                    return pair;
            }
        }

        // in the code above we did not find a pair below any of the children, now see if v1 and v2 have a pair of clusters below them:

        // don't want either cluster to contain all taxa in the tree:
        if (root1.getTaxa().equals(taxa) || root2.getTaxa().equals(taxa) || root1.getInDegree() == 0 || root2.getInDegree() == 0)
            return null;

        if (!isBranchingNode(root1) || !isBranchingNode(root2))  // should both be branching nodes
            return null;

        Pair<Set<Node>, Set<Node>> pairOfConnectedComponents = getPairOfSeparatableConnectedComponents(root1, root2);

        if (pairOfConnectedComponents != null) // has pair of connected components, one below each root
        {
            Set<Node> component1 = pairOfConnectedComponents.getFirst();
            Root subTreeRoot1 = root1.newNode();
            BitSet taxa1 = new BitSet();
            BitSet removedTaxa1 = new BitSet();
            boolean allChildren1 = (component1.size() == root1.getOutDegree());

            for (Node a1 : component1) {
                Edge f = subTreeRoot1.newEdge(subTreeRoot1, a1);
                f.setInfo(a1.getFirstInEdge().getInfo());
                subTreeRoot1.deleteEdge(a1.getFirstInEdge());
                taxa1.or(((Root) a1).getTaxa());
                removedTaxa1.or(((Root) a1).getRemovedTaxa());
            }
            subTreeRoot1.setTaxa(taxa1);
            subTreeRoot1.setRemovedTaxa(removedTaxa1);
            subTreeRoot1.reorderChildren();

            if (allChildren1) {  // cluster is below one node, add extra root edge
                Root tmp = subTreeRoot1.newNode();
                tmp.setTaxa(subTreeRoot1.getTaxa());
                tmp.setRemovedTaxa(subTreeRoot1.getRemovedTaxa());
                Edge f = tmp.newEdge(tmp, subTreeRoot1);
                f.setInfo(1);
                subTreeRoot1 = tmp;
            }

            int taxon = taxa1.nextSetBit(0);

            Root placeHolder1;
            if (allChildren1)
                placeHolder1 = root1;
            else {
                placeHolder1 = root1.newNode();
                Edge f = root1.newEdge(root1, placeHolder1);
                f.setInfo(1);
            }
            placeHolder1.getTaxa().set(taxon);
            // placeHolder1.getRemovedTaxa().or(removedTaxa1);
            placeHolder.set(taxon);

            root1.reorderChildren();

            // remove all taxa of cluster, except first, from all nodes above
            Root up1 = root1;
            while (up1 != null) {
                up1.getTaxa().andNot(taxa1);
                up1.getTaxa().set(taxon);
                up1.getRemovedTaxa().andNot(removedTaxa1);
                //up1.reorderChildren();
                if (up1.getInDegree() > 0)
                    up1 = (Root) up1.getFirstInEdge().getSource();
                else
                    up1 = null;
            }

            Set<Node> component2 = pairOfConnectedComponents.getSecond();
            Root subTreeRoot2 = root2.newNode();
            BitSet taxa2 = new BitSet();
            BitSet removedTaxa2 = new BitSet();
            boolean allChildren2 = (component2.size() == root2.getOutDegree());

            for (Node a2 : component2) {
                root2.deleteEdge(a2.getFirstInEdge());
                Edge f = root2.newEdge(subTreeRoot2, a2);
                f.setInfo(2);
                taxa2.or(((Root) a2).getTaxa());
                removedTaxa2.or(((Root) a2).getRemovedTaxa());
            }
            subTreeRoot2.setTaxa(taxa2);
            subTreeRoot2.setRemovedTaxa(removedTaxa2);
            subTreeRoot2.reorderChildren();

            if (allChildren2) { // cluster is below one node, add extra root edge
                Root tmp = subTreeRoot2.newNode();
                tmp.setTaxa(subTreeRoot2.getTaxa());
                tmp.setRemovedTaxa(subTreeRoot2.getRemovedTaxa());
                Edge f = tmp.newEdge(tmp, subTreeRoot2);
                f.setInfo(2);
                subTreeRoot2 = tmp;
            }

            Root placeHolder2;
            if (allChildren2)
                placeHolder2 = root2;
            else {
                placeHolder2 = root2.newNode();
                Edge f = root2.newEdge(root2, placeHolder2);
                f.setInfo(2);
            }
            placeHolder2.getTaxa().set(taxon);

            // placeHolder2.getRemovedTaxa().or(removedTaxa2);
            root2.reorderChildren();

            // remove all taxa of cluster, except first, from all nodes above
            Root up2 = root2;
            while (up2 != null) {
                up2.getTaxa().andNot(taxa2);
                up2.getTaxa().set(taxon);
                up2.getRemovedTaxa().andNot(removedTaxa2);
                //up2.reorderChildren();
                if (up2.getInDegree() > 0)
                    up2 = (Root) up2.getFirstInEdge().getSource();
                else
                    up2 = null;
            }

            if (!taxa1.equals(taxa2))
                throw new RuntimeException("Unequal taxon sets: " + Basic.toString(X) + " vs " + Basic.toString(Y));

            return new Pair<Root, Root>(subTreeRoot1, subTreeRoot2);
        } else if (X.equals(Y)) // no pair of connected components, but perhaps both nodes give us a component
        {
            if (root1.getInDegree() == 0 || root2.getInDegree() == 0)
                throw new RuntimeException("Indegree should not be zero");

            Root subTreeRoot1 = root1.newNode();
            BitSet taxa1 = root1.getTaxa();
            BitSet removedTaxa1 = root1.getRemovedTaxa();
            subTreeRoot1.setTaxa(taxa1);
            subTreeRoot1.setRemovedTaxa(root1.getRemovedTaxa());
            List<Edge> toDelete = new LinkedList<Edge>();
            for (Edge e1 = root1.getFirstOutEdge(); e1 != null; e1 = root1.getNextOutEdge(e1)) {
                Edge f = subTreeRoot1.newEdge(subTreeRoot1, e1.getTarget());
                f.setInfo(1);
                toDelete.add(e1);
            }
            for (Edge e1 : toDelete) {
                root1.deleteEdge(e1);
            }

            root1.getRemovedTaxa().clear();

            // remove all taxa of cluster, except first, from all nodes above
            int t1 = taxa1.nextSetBit(0);
            Root up1 = root1;
            while (up1 != null) {
                up1.getTaxa().andNot(taxa1);
                up1.getTaxa().set(t1);
                up1.getRemovedTaxa().andNot(removedTaxa1);
                //up1.reorderChildren();
                if (up1.getInDegree() > 0)
                    up1 = (Root) up1.getFirstInEdge().getSource();
                else
                    up1 = null;
            }
            placeHolder.set(t1);

            Root subTreeRoot2 = root2.newNode();
            BitSet taxa2 = root2.getTaxa();
            BitSet removedTaxa2 = root2.getRemovedTaxa();
            subTreeRoot2.setTaxa(taxa2);
            subTreeRoot2.setRemovedTaxa(root2.getRemovedTaxa());
            toDelete.clear();
            for (Edge e2 = root2.getFirstOutEdge(); e2 != null; e2 = root2.getNextOutEdge(e2)) {
                Edge f = subTreeRoot2.newEdge(subTreeRoot2, e2.getTarget());
                f.setInfo(2);
                toDelete.add(e2);
            }
            for (Edge e2 : toDelete) {
                root2.deleteEdge(e2);
            }

            root2.getRemovedTaxa().clear();

            // remove all taxa of cluster, except first, from all nodes above
            int t2 = taxa2.nextSetBit(0);
            Root up2 = root2;
            while (up2 != null) {
                up2.getTaxa().andNot(taxa2);
                up2.getTaxa().set(t2);
                up2.getRemovedTaxa().andNot(removedTaxa2);
                //up2.reorderChildren();
                if (up2.getInDegree() > 0)
                    up2 = (Root) up2.getFirstInEdge().getSource();
                else
                    up2 = null;
            }

            {  // cluster is below one node, add extra root edge
                Root tmp = subTreeRoot1.newNode();
                tmp.setTaxa(subTreeRoot1.getTaxa());
                tmp.setRemovedTaxa(subTreeRoot1.getRemovedTaxa());
                Edge f = tmp.newEdge(tmp, subTreeRoot1);
                f.setInfo(1);
                subTreeRoot1 = tmp;
            }

            {  // cluster is below one node, add extra root edge
                Root tmp = subTreeRoot2.newNode();
                tmp.setTaxa(subTreeRoot2.getTaxa());
                tmp.setRemovedTaxa(subTreeRoot2.getRemovedTaxa());
                Edge f = tmp.newEdge(tmp, subTreeRoot2);
                f.setInfo(2);
                subTreeRoot2 = tmp;
            }

            return new Pair<Root, Root>(subTreeRoot1, subTreeRoot2);
        }

        return null;
    }

    /**
     * is this a branching node, i.e. does it have at least two children with unremoved taxa?
     *
     * @param v
     * @return true, if branching node
     */
    private static boolean isBranchingNode(Root v) {
        boolean foundOne = false;
        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            Root w = (Root) e.getTarget();
            if (w.getTaxa().cardinality() > 0) {
                if (foundOne)
                    return true;
                else foundOne = true;
            }
        }
        return false;
    }

    /**
     * returns the next branching node
     *
     * @param v
     * @return next branching node
     */
    private static Root nextBranchingNode(Root v) {
        while (v != null) {
            Root one = null;
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Root w = (Root) e.getTarget();
                if (w.getTaxa().cardinality() > 0) {
                    if (one != null)
                        return v; // found a second child with taxa, v is a branching node
                    else one = w;
                }
            }
            v = one; // found only one child with taxa, move to it.
        }
        return null;
    }


    /**
     * find a pair of separable bunches of subtrees in both trees.
     *
     * @param v1
     * @param v2
     * @return
     */
    private static Pair<Set<Node>, Set<Node>> getPairOfSeparatableConnectedComponents(Node v1, Node v2) {
        // compute intersection graph:
        Graph intersectionGraph = new Graph();
        Node[] sets1 = new Node[v1.getOutDegree()];
        int i = 0;
        for (Edge e1 = v1.getFirstOutEdge(); e1 != null; e1 = v1.getNextOutEdge(e1)) {
            Root w1 = (Root) e1.getTarget();
            if (w1.getTaxa().cardinality() > 0)
                sets1[i++] = intersectionGraph.newNode(w1);
        }
        Node[] sets2 = new Node[v2.getOutDegree()];
        i = 0;
        for (Edge e2 = v2.getFirstOutEdge(); e2 != null; e2 = v2.getNextOutEdge(e2)) {
            Root w2 = (Root) e2.getTarget();
            if (w2.getTaxa().cardinality() > 0)
                sets2[i++] = intersectionGraph.newNode(w2);
        }

        {
            int i1 = 0;
            for (Edge e1 = v1.getFirstOutEdge(); e1 != null; e1 = v1.getNextOutEdge(e1)) {
                Root w1 = (Root) e1.getTarget();
                BitSet A1 = w1.getTaxa();
                if (A1.cardinality() > 0) {
                    int i2 = 0;
                    for (Edge e2 = v2.getFirstOutEdge(); e2 != null; e2 = v2.getNextOutEdge(e2)) {
                        Root w2 = (Root) e2.getTarget();
                        BitSet A2 = w2.getTaxa();
                        if (A2.cardinality() > 0) {
                            if (A1.intersects(A2))
                                intersectionGraph.newEdge(sets1[i1], sets2[i2]);
                            i2++;
                        }
                    }
                    i1++;
                }
            }
        }

        // System.err.println("----- Intersection graph:\n"+intersectionGraph.toString());

        // find a component that contains at least three nodes and has same taxa in both components
        for (Node a = intersectionGraph.getFirstNode(); a != null; a = a.getNext()) {
            if (a.getDegree() > 1) {
                Pair<Set<Node>, Set<Node>> nodesInComponent = getNodesInComponent(a, sets1, sets2);
                BitSet G = new BitSet();
                BitSet H = new BitSet();
                for (Node x : nodesInComponent.getFirst())
                    G.or(((Root) x).getTaxa());
                for (Node x : nodesInComponent.getSecond())
                    H.or(((Root) x).getTaxa());
                if (G.equals(H))      // have same taxa in both trees, return it!
                    return nodesInComponent;
            }
        }
        return null;
    }

    /**
     * get all tree nodes in a connected component of the  intersection graph
     *
     * @param a
     * @param sets1
     * @param sets2
     * @return
     */
    private static Pair<Set<Node>, Set<Node>> getNodesInComponent(Node a, Node[] sets1, Node[] sets2) {
        Set<Node> seen = new HashSet<Node>();
        Stack<Node> stack = new Stack<Node>();
        stack.push(a);
        seen.add(a);
        while (stack.size() > 0) {
            a = stack.pop();
            for (Edge e = a.getFirstAdjacentEdge(); e != null; e = a.getNextAdjacentEdge(e)) {
                Node b = e.getOpposite(a);
                if (!seen.contains(b)) {
                    seen.add(b);
                    stack.add(b);
                }
            }
        }
        Set<Node> result1 = new HashSet<Node>();
        for (Node c : sets1) {
            if (seen.contains(c))
                result1.add((Node) c.getInfo());
        }
        Set<Node> result2 = new HashSet<Node>();
        for (Node c : sets2) {
            if (seen.contains(c))
                result2.add((Node) c.getInfo());
        }
        return new Pair<Set<Node>, Set<Node>>(result1, result2);
    }
}
