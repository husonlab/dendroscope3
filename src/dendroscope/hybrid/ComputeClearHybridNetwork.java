/*
 *   ComputeClearHybridNetwork.java Copyright (C) 2020 Daniel H. Huson
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

/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import jloda.graph.Edge;
import jloda.graph.Node;

import java.util.*;

/**
 * Given two rooted, bifurcating phylogenetic trees T1 and T2 and a minimal
 * acyclic agreement forest of T1 and T2, this functions computes a network
 * displaying T1 and T2 with a minimal number of reticulate nodes.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class ComputeClearHybridNetwork {

    private final Hashtable<HybridTree, Node[]> componentToNodes = new Hashtable<>();

    private final Hashtable<HybridTree, Integer> componentToDepth = new Hashtable<>();
    private Vector<HybridTree> sortedForest = new Vector<>();

    private final Vector<Vector<String>> pairs = new Vector<>();

    public HybridNetwork run(Vector<HybridTree> forest, HybridTree t1,
                             HybridTree t2, ReplacementInfo rI, TreeMarker tM) throws Exception {

        HybridNetwork n = new HybridNetwork(t2, false, t2.getTaxaOrdering());
        n.update();

        // sortedForest = sortForest(forest, t1);

        // find target nodes of attaching-edge
        for (int i = forest.size() - 2; i >= 0; i--) {

            // for (HybridTree f : sortedForest) {

            HybridTree f = forest.get(i);
            BitSet r = f.getNodeToCluster().get(f.getRoot());

            // System.out.println();
            // System.out.println(f + ";");
            // insertComponent(f, r, t1, n, tM);

            // compute parent of the node representing f in t1
            Node v1 = n.findLCA(r);

            System.out.println();
            System.out.println(f + ";");
            System.out.println("lca " + n.getLabel(v1));

            // compute parent of the node representing f in t2
            Node l2 = t1.findLCA(r);

            // find Node whose in-edge is attached by the reticulate edge
            Node neighbour = getNeighbour(l2, t1);

            // if (!isPair(l2, neighbour, t1)) {

            Node v2 = findDeepestNode(neighbour, t1, n, n.getNodeToCluster()
                    .get(v1));

            // System.out.println(t1.getLabel(l2));
            // System.out.println(n.getLabel(v2));

            // if no edge is found, f is attached to the out-edge of the
            // root
            Iterator<Edge> it = n.getRoot().outEdges().iterator();
            Node v = it.next().getTarget();
            if (n.getLabel(v).equals("rho"))
                v = it.next().getTarget();
            if (v2 == null || n.getLabel(v2).equals("rho")
                    || v2.equals(n.getRoot()))
                v2 = v;

            // insertReticulateEdges(n, v1, v2, tM);

            Node[] sourceNodes = {v1, v2};
            componentToNodes.put(f, sourceNodes);

            // // System.out.println("putting " + n.getLabel(v2) + " -> "
            // // + t1.getLabel(l2));
            componentToDepth.put(f, getDepth(l2));

            // } else {
            // BitSet newR = t1.getNodeToCluster().get(
            // l2.getFirstInEdge().getSource());
            // compueSourceNodes(f, newR, t1, n, tM);
            // }

            sortedForest.add(f);

        }

        // Collections.sort(sortedForest, new
        // ForestComparator(componentToDepth));
        insertReticulations(n, tM);

        // setting the taxon-labeling which is the replacement character for the
        // network
        if (t1.getReplacementCharacter() != null)
            n.setReplacementCharacter(t1.getReplacementCharacter());

        n.update();
        return n;
    }

    private Vector<HybridTree> sortForest(Vector<HybridTree> forest,
                                          HybridTree t) {

        sortedForest = new Vector<>();

        for (int i = forest.size() - 2; i >= 0; i--) {
            HybridTree f = forest.get(i);

            BitSet r = f.getNodeToCluster().get(f.getRoot());
            Node v = t.findLCA(r);

            int depth = getDepth(v);
            componentToDepth.put(f, depth);
            sortedForest.add(f);
        }

        // Collections.sort(sortedForest, new
        // ForestComparator(componentToDepth));

        return sortedForest;
    }

    private void insertComponent(HybridTree f, BitSet r, HybridTree t1,
                                 HybridNetwork n, TreeMarker tM) {

        Node v1 = n.findLCA(r);

        // System.out.println();
        // System.out.println(f + ";");
        // System.out.println(n.getLabel(v1));

        // compute parent of the node representing f in t2
        Node l2 = t1.findLCA(r);

        // System.out.println("v1 " + n.getLabel(v1));
        // System.out.println("l2 " + t1.getLabel(l2));

        // find Node whose in-edge is attached by the reticulate edge
        Node neighbour = getNeighbour(l2, t1);

        // if (!isPair(l2, neighbour, t1)) {

        Node v2 = findDeepestNode(neighbour, t1, n, n.getNodeToCluster()
                .get(v1));

        // System.out.println(t1.getLabel(l2));
        // System.out.println(n.getLabel(v2));

        // if no edge is found, f is attached to the out-edge of the
        // root
        Iterator<Edge> it = n.getRoot().outEdges().iterator();
        Node v = it.next().getTarget();
        if (n.getLabel(v).equals("rho"))
            v = it.next().getTarget();
        if (v2 == null || n.getLabel(v2).equals("rho")
                || v2.equals(n.getRoot()))
            v2 = v;

        // System.out.println(n.getLabel(v1));
        // System.out.println(n.getLabel(v2));
        // System.out.println(n + ";");

        // insertReticulateEdges(n, v1, v2, tM);

        Node[] sourceNodes = {v1, v2};
        componentToNodes.put(f, sourceNodes);

        // // System.out.println("putting " + n.getLabel(v2) + " -> "
        // // + t1.getLabel(l2));
        // componentToDepth.put(f, getDepth(l2));

        // } else {
        // BitSet newR = t1.getNodeToCluster().get(
        // l2.getFirstInEdge().getSource());
        // insertComponent(f, newR, t1, n, tM);
        // }

    }

    private int getDepth(Node v) {
        int depth = 0;
        while (v.getInDegree() != 0) {
            v = v.getFirstInEdge().getSource();
            depth++;
        }
        return depth;
    }

    private Node getNeighbour(Node v, HybridTree t) {
        Node p = v.getFirstInEdge().getSource();
        Iterator<Edge> it = p.outEdges().iterator();
        Node c = it.next().getTarget();

        if (c.equals(v))
            c = it.next().getTarget();
        return c;
    }

    private boolean isPair(Node v1, Node v2, HybridTree t) {
        Vector<String> pair = new Vector<>();
        pair.add(t.getLabel(v1));
        pair.add(t.getLabel(v2));
        Collections.sort(pair);
        if (pairs.contains(pair))
            return true;
        else {
            pairs.add(pair);
            return false;
        }
    }

    private Node findDeepestNode(Node v, HybridTree t1, HybridNetwork n,
                                 BitSet b) {

        // searching for deepest node in t1 whose subtree contains the leaf-set
        // of v
        BitSet vCluster = t1.getNodeToCluster().get(v);
        BitSet cluster = getDeepestCluster(vCluster, n);

        BitSet a = (BitSet) b.clone();
        a.or(cluster);
        if (a.equals(b)) {
            // System.out.println("Rejected: "
            // + n.getLabel(n.getClusterToNode().get(cluster)));
            return findDeepestNode(v.getFirstInEdge().getSource(), t1, n, b);
        } else
            return n.getClusterToNode().get(cluster);

    }

    // returns deepest cluster in t1 for which c is subset holds
    private BitSet getDeepestCluster(BitSet c, HybridNetwork n) {
        Iterator<Node> it = n.postOrderWalk();
        while (it.hasNext()) {
            Node v = it.next();
            if (v.getOutDegree() != 1) {
                BitSet b = n.getNodeToCluster().get(v);
                BitSet a = (BitSet) b.clone();
                a.or(c);
                if (a.equals(b))
                    return b;
            }
        }
        return null;
    }

    private void insertReticulations(HybridNetwork n, TreeMarker tM) {

        for (HybridTree f : sortedForest) {

            Node[] nodes = componentToNodes.get(f);

            System.out.println();
            System.out.println(f + ";");
            System.out.println(n.getLabel(nodes[0]));
            System.out.println(n.getLabel(nodes[1]));

            insertReticulateEdges(n, nodes[0], nodes[1], tM);
            System.out.println(n + ";");
            n.update();

        }

        n.update();

    }

    private void insertReticulateEdges(HybridNetwork n, Node v1, Node v2,
                                       TreeMarker tM) {

        Edge e = v1.getFirstInEdge();
        Node s = e.getSource();
        Node t = e.getTarget();
        n.deleteEdge(e);

        // adding reticulate node
        Node retNode = n.newNode();
        n.newEdge(retNode, t);

        // adding 1st reticulate edge
        Edge e1 = n.newEdge(s, retNode);
        n.setSpecial(e1, true);
        n.setWeight(e1, 0);
        // tM.insertT1Edge(e1);

        e = v2.getFirstInEdge();
        boolean isSpecial = n.isSpecial(e);
        t = e.getTarget();
        s = e.getSource();
        n.deleteEdge(e);

        // split in-edge of v2 by inserting node x2
        Node x = n.newNode();
        n.newEdge(s, x);
        Edge newE = n.newEdge(x, t);
        if (isSpecial)
            n.setSpecial(newE, true);

        // adding 2nd reticulate edge
        Edge e2 = n.newEdge(x, retNode);
        n.setSpecial(e2, true);
        n.setWeight(e2, 0);
        tM.insertT1Edge(e2);

        // System.out.println("e1: " + n.getLabel(e1.getSource()) + " -> "
        // + n.getLabel(e1.getTarget()));
        // System.out.println("e2: " + n.getLabel(e2.getSource()) + " -> "
        // + n.getLabel(e2.getTarget()));
    }

}
