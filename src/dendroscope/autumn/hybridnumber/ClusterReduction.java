/*
 *   ClusterReduction.java Copyright (C) 2020 Daniel H. Huson
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
package dendroscope.autumn.hybridnumber;

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
    public static final boolean checking = false;

    /**
     * cluster reduce two trees, if possible
     *
     * @param tree1
     * @param tree2
     * @return subtree-reduced trees followed by all reduced subtrees
     */
    public static TreeData[] apply(TreeData tree1, TreeData tree2) throws IOException {
        // setup rooted trees with nodes labeled by taxa ids
        Taxa allTaxa = new Taxa();
        Pair<Root, Root> roots = PreProcess.apply(tree1, tree2, allTaxa);

        Root v1 = roots.getFirst();
        Root v2 = roots.getSecond();

        // run the algorithm
        Pair<Root, Root> pair = apply(v1, v2, new Single<Integer>());

        if (pair != null) {
            List<Root> results = new LinkedList<Root>();
            results.add(v1);
            results.add(v2);
            results.add(pair.getFirst());
            results.add(pair.getSecond());
            // convert data-structures to final trees
            List<TreeData> result = PostProcess.apply(results.toArray(new Root[results.size()]), allTaxa, false);
            return result.toArray(new TreeData[result.size()]);

        } else
            return null;
    }


    /**
     * finds a pair nodes for minimal cluster reduction, if one exists
     *
     * @param v1
     * @param v2
     * @param placeHolderTaxa
     * @return two reduced clusters or null
     */
    public static Pair<Root, Root> apply(Root v1, Root v2, Single<Integer> placeHolderTaxa) {
        String string1 = null;
        String string2 = null;

        if (checking) {
            string1 = v1.toStringFullTreeX();
            string2 = v2.toStringFullTreeX();
        }

        Pair<Root, Root> pair = applyRec(v1, v2, new HashSet<Pair<Node, Node>>(), placeHolderTaxa);
        if (!v1.getTaxa().equals(v2.getTaxa()))
            throw new RuntimeException("Unequal taxon sets: " + Basic.toString(v1.getTaxa()) + " vs " + Basic.toString(v2.getTaxa()));

        // reorder should not be necessary
        // v1.reorderSubTree();
        // v2.reorderSubTree();

        if (checking) {
            try {
                v1.checkTree();
            } catch (RuntimeException ex) {
                System.err.println("DATA A");
                System.err.println(string1 + ";");
                System.err.println(v1.toStringFullTreeX() + ";");
                if (pair != null)
                    System.err.println(pair.getFirst().toStringFullTreeX() + ";");
                throw ex;
            }

            try {
                v2.checkTree();
            } catch (RuntimeException ex) {
                System.err.println("DATA B");
                System.err.println(string2 + ";");
                System.err.println(v2.toStringFullTreeX() + ";");
                if (pair != null)
                    System.err.println(pair.getSecond().toStringFullTreeX() + ";");
                throw ex;
            }
        }

        if (pair != null && !pair.getFirst().getTaxa().equals(pair.getSecond().getTaxa()))
            throw new RuntimeException("Unequal taxon sets: " + Basic.toString(pair.getFirst().getTaxa()) + " vs " + Basic.toString(pair.getSecond().getTaxa()));

        if (pair != null) {
            // reorder should not be necessary
            // pair.getFirst().reorderSubTree();
            // pair.getSecond().reorderSubTree();

            if (checking) {
                try {
                    pair.getFirst().checkTree();
                } catch (RuntimeException ex) {
                    System.err.println("DATA 1");
                    System.err.println(string1 + ";");
                    System.err.println(v1.toStringFullTreeX() + ";");
                    System.err.println(pair.getFirst().toStringFullTreeX() + ";");
                    throw ex;
                }

                try {
                    pair.getSecond().checkTree();
                } catch (RuntimeException ex) {
                    System.err.println("DATA 2");
                    System.err.println(string2 + ";");
                    System.err.println(v2.toStringFullTreeX() + ";");
                    System.err.println(pair.getSecond().toStringFullTreeX() + ";");
                    throw ex;
                }
            }
        }
        return pair;
    }

    /**
     * recursively does the work
     *
     * @param v1
     * @param v2
     * @param compared
     * @param placeHolderTaxa
     * @return two reduced clusters or null
     */
    private static Pair<Root, Root> applyRec(Root v1, Root v2, Set<Pair<Node, Node>> compared, Single<Integer> placeHolderTaxa) {
        Pair<Node, Node> nodePair = new Pair<Node, Node>(v1, v2);
        if (compared.contains(nodePair))
            return null;
        else
            compared.add(nodePair);

        // System.err.println("reduceClusterRec v1=" + Basic.toString((v1.getTaxa()) + " v2=" + Basic.toString(v2.getTaxa());

        BitSet X = v1.getTaxa();
        BitSet Y = v2.getTaxa();

        // recursively process all children
        for (Edge e1 = v1.getFirstOutEdge(); e1 != null; e1 = v1.getNextOutEdge(e1)) {
            Root u1 = (Root) e1.getTarget();
            if (u1.getTaxa().intersects(Y)) {
                Pair<Root, Root> pair = applyRec(u1, v2, compared, placeHolderTaxa);
                if (pair != null)
                    return pair;
            }
        }
        for (Edge e2 = v2.getFirstOutEdge(); e2 != null; e2 = v2.getNextOutEdge(e2)) {
            Root u2 = (Root) e2.getTarget();
            if (u2.getTaxa().intersects(X)) {
                Pair<Root, Root> pair = applyRec(v1, u2, compared, placeHolderTaxa);
                if (pair != null)
                    return pair;
            }
        }

        if (v1.getInDegree() > 0 && v2.getInDegree() > 0) {
            Pair<Set<Node>, Set<Node>> pairOfConnectedComponents = getPairOfSeparatableConnectedComponents(v1, v2);

            if (pairOfConnectedComponents != null) {
                Set<Node> component1 = pairOfConnectedComponents.getFirst();
                Root u1 = v1.newNode();
                BitSet taxa1 = new BitSet();
                for (Node p1 : component1) {
                    u1.deleteEdge(p1.getFirstInEdge());
                    u1.newEdge(u1, p1);
                    taxa1.or(((Root) p1).getTaxa());
                }
                u1.setTaxa(taxa1);
                placeHolderTaxa.set(u1.getTaxa().nextSetBit(0));
                u1.reorderChildren();

                Set<Node> component2 = pairOfConnectedComponents.getSecond();
                Root u2 = v2.newNode();
                BitSet taxa2 = new BitSet();
                for (Node p2 : component2) {
                    v2.deleteEdge(p2.getFirstInEdge());
                    v2.newEdge(u2, p2);
                    taxa2.or(((Root) p2).getTaxa());
                }
                u2.setTaxa(taxa2);
                u2.reorderChildren();

                if (!taxa1.equals(taxa2))
                    throw new RuntimeException("Unequal taxon sets: " + Basic.toString(X) + " vs " + Basic.toString(Y));
                return new Pair<Root, Root>(u1, u2);
            } else if ((v1.getOutDegree() > 1 || v2.getOutDegree() > 1) && X.equals(Y)) // no pair of connected components, but perhaps both nodes give us a component
            {
                if (v1.getInDegree() == 0 || v2.getInDegree() == 0)
                    throw new RuntimeException("Indegree should not be zero");
                Root u1 = v1.newNode();
                u1.setTaxa((BitSet) v1.getTaxa().clone());
                placeHolderTaxa.set(u1.getTaxa().nextSetBit(0));

                List<Edge> toDelete = new LinkedList<Edge>();
                for (Edge e1 = v1.getFirstOutEdge(); e1 != null; e1 = v1.getNextOutEdge(e1)) {
                    u1.newEdge(u1, e1.getTarget());
                    toDelete.add(e1);
                }
                for (Edge e1 : toDelete) {
                    v1.deleteEdge(e1);
                }

                Root u2 = v2.newNode();
                u2.setTaxa((BitSet) v2.getTaxa().clone());
                toDelete.clear();
                for (Edge e2 = v2.getFirstOutEdge(); e2 != null; e2 = v2.getNextOutEdge(e2)) {
                    u2.newEdge(u2, e2.getTarget());
                    toDelete.add(e2);
                }
                for (Edge e2 : toDelete) {
                    v2.deleteEdge(e2);
                }

                return new Pair<Root, Root>(u1, u2);
            }
        }
        return null;
    }

    /**
     * find a pair of separatable bunches of subtrees in both trees.
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
        for (Edge e = v1.getFirstOutEdge(); e != null; e = v1.getNextOutEdge(e)) {
            sets1[i++] = intersectionGraph.newNode(e.getTarget());
        }
        Node[] sets2 = new Node[v2.getOutDegree()];
        i = 0;
        for (Edge e = v2.getFirstOutEdge(); e != null; e = v2.getNextOutEdge(e)) {
            sets2[i++] = intersectionGraph.newNode(e.getTarget());
        }

        {
            int i1 = 0;
            for (Edge e1 = v1.getFirstOutEdge(); e1 != null; e1 = v1.getNextOutEdge(e1)) {
                BitSet A1 = ((Root) e1.getTarget()).getTaxa();
                int i2 = 0;
                for (Edge e2 = v2.getFirstOutEdge(); e2 != null; e2 = v2.getNextOutEdge(e2)) {
                    BitSet A2 = ((Root) e2.getTarget()).getTaxa();

                    if (A1.intersects(A2))
                        intersectionGraph.newEdge(sets1[i1], sets2[i2]);
                    i2++;
                }
                i1++;
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
