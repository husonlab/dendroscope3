/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Vector;

public class Simulator {

    private final int numOfLeaves;
    private final int numOfTrees;
    private final int rSPRmoves;
    private final int lcaDist;

    private final Vector<HybridTree> t1Trees = new Vector<>();
    private final Vector<HybridTree> t2Trees = new Vector<>();

    public Simulator(int numOfLeaves, int numOfTrees, int rSPRmoves, int lcaDist) {
        this.numOfLeaves = numOfLeaves;
        this.numOfTrees = numOfTrees;
        this.rSPRmoves = rSPRmoves;
        this.lcaDist = lcaDist;
    }

    public void run() {
        generateT1Trees();
        generateT2Trees();
    }

    private void generateT2Trees() {
        for (HybridTree t1 : t1Trees) {
            HybridTree t2 = switchSubtrees(new HybridTree(t1, false, null));
            while (!sameLeafSet(t1, t2))
                t2 = switchSubtrees(new HybridTree(t1, false, null));
            t2Trees.add(t2);
        }
    }

    // private HybridTree switchSubtrees(HybridTree t) {
    // for (int i = 0; i < rSPRmoves; i++) {
    // Vector<Node> nodePair = getRandomSwitchNodes(t, collectFirstNodes(t),
    // collectSecondNodes(t));
    // Node v1 = nodePair.get(0);
    // Node v2 = nodePair.get(1);
    //
    // Node p1 = v1.getFirstInEdge().getSource();
    // t.deleteEdge(v1.getFirstInEdge());
    // Node c = p1.getFirstOutEdge().getTarget();
    // t.deleteEdge(p1.getFirstOutEdge());
    // Node pP = p1.getFirstInEdge().getSource();
    // t.deleteEdge(p1.getFirstInEdge());
    // t.newEdge(pP, c);
    // t.deleteNode(p1);
    //
    // Node p2 = v2.getFirstInEdge().getSource();
    // Node v = t.newNode();
    // t.deleteEdge(v2.getFirstInEdge());
    // t.newEdge(p2, v);
    // t.newEdge(v, v2);
    // t.newEdge(v, v1);
    //
    // t.update();
    // }
    // return t;
    // }

    private HybridTree switchSubtrees(HybridTree t) {
        for (int i = 0; i < rSPRmoves; i++) {

            Node v1 = getRandomNode(collectFirstNodes(t));

            Node p1 = v1.getFirstInEdge().getSource();
            t.deleteEdge(v1.getFirstInEdge());
            Node c = p1.getFirstOutEdge().getTarget();
            t.deleteEdge(p1.getFirstOutEdge());
            Node pP = p1.getFirstInEdge().getSource();
            t.deleteEdge(p1.getFirstInEdge());
            t.newEdge(pP, c);
            t.deleteNode(p1);

            Node v2 = getRandomNode(getRandom2ndNode(t, pP));

            Node p2 = v2.getFirstInEdge().getSource();
            Node v = t.newNode();
            t.deleteEdge(v2.getFirstInEdge());
            t.newEdge(p2, v);
            t.newEdge(v, v2);
            t.newEdge(v, v1);

            t.update();
        }
        // System.out.println("is correct? : " + isCorrect(t));
        return t;
    }

    private Vector<Node> getRandom2ndNode(HybridTree t, Node v1) {
        int dist = 0;
        Vector<Node> nodes = new Vector<>();
        while (dist < lcaDist) {
            if (v1.getInDegree() == 1) {
                v1 = v1.getFirstInEdge().getSource();
                if (v1.getInDegree() == 1) {
                    nodes.add(v1);
                    getNodesBelow(v1, nodes);
                    dist++;
                } else
                    break;
            } else
                break;
        }
        return nodes;
    }

    private void getNodesBelow(Node v1, Vector<Node> nodes) {
        Iterator<Edge> it = v1.outEdges().iterator();
        while (it.hasNext()) {
            Node c = it.next().getTarget();
            if (!nodes.contains(c) && c.getInDegree() == 1) {
                nodes.add(c);
                getNodesBelow(c, nodes);
            }
        }
    }

    private Vector<Node> collectFirstNodes(HybridTree t) {
        Vector<Node> nodes = new Vector<>();
        for (Node v : t.nodes()) {
            if (v.getInDegree() == 1) {
                Node p = v.getFirstInEdge().getSource();
                if (p.getInDegree() == 1) {
                    Node pP = p.getFirstInEdge().getSource();
                    if (pP.getInDegree() == 1) {
                        Node pPp = pP.getFirstInEdge().getSource();
                        if (pPp.getInDegree() == 1)
                            nodes.add(v);
                    }
                }
            }
        }
        return nodes;
    }

    private Vector<Node> collectSecondNodes(HybridTree t) {
        Vector<Node> nodes = new Vector<>();
        for (Node v : t.nodes()) {
            if (v.getInDegree() == 1)
                nodes.add(v);
        }
        return nodes;
    }

    private void generateT1Trees() {
        for (int i = 0; i < numOfTrees; i++) {
            HybridTree t = generateRandomTree();
            t.update();
            t1Trees.add(t);
        }
    }

    private HybridTree generateRandomTree() {
        PhyloTree t = new PhyloTree();
        Vector<Node> leaves = new Vector<>();
        for (int i = 0; i < numOfLeaves; i++) {
            Node v = t.newNode();
            t.setLabel(v, i + "");
            leaves.add(v);
        }
        createInnerNodes(t, leaves);
        return new HybridTree(t, false, null);
    }

    private void createInnerNodes(PhyloTree t, Vector<Node> nodes) {
        while (nodes.size() != 1) {
            Node v1 = getRandomNode(nodes);
            nodes.remove(v1);
            Node v2 = getRandomNode(nodes);
            nodes.remove(v2);

            Node v = t.newNode();
            t.newEdge(v, v1);
            t.newEdge(v, v2);
            nodes.add(v);
        }
        Node root = nodes.firstElement();
        t.setRoot(root);
    }

    private Vector<Node> getRandomSwitchNodes(HybridTree t,
                                              Vector<Node> firstNodes, Vector<Node> secondNodes) {

        boolean search = true;
        Vector<Node> v = new Vector<>();

        while (search) {
            search = false;
            v.clear();
            v.add(getRandomNode(firstNodes));
            v.add(getRandomNode(secondNodes));
            BitSet b1 = t.getNodeToCluster().get(v.get(0));
            BitSet b2 = t.getNodeToCluster().get(v.get(1));
            BitSet b;
            if (b1.cardinality() < b2.cardinality()) {
                b = (BitSet) b1.clone();
                b.and(b2);
                if (b.equals(b1))
                    search = true;
            } else {
                b = (BitSet) b2.clone();
                b.and(b1);
                if (b.equals(b2))
                    search = true;

            }
            Node p = v.get(0).getFirstInEdge().getSource();
            if (p.equals(v.get(1)))
                search = true;
        }

        return v;
    }

    private Node getRandomNode(Vector<Node> nodes) {
        double d = Math.random() * (nodes.size() - 1);
        int index = (int) Math.round(d);
        return nodes.get(index);
    }

    public Vector<HybridTree> getT1Trees() {
        return t1Trees;
    }

    public Vector<HybridTree> getT2Trees() {
        return t2Trees;
    }

    public boolean sameLeafSet(HybridTree t1, HybridTree t2) {
        Vector<String> t1Taxa = new Vector<>();
        for (Node v : t1.computeSetOfLeaves())
            t1Taxa.add(t1.getLabel(v));

        for (Node v : t2.computeSetOfLeaves()) {
            if (!t1Taxa.contains(t2.getLabel(v)))
                return false;
        }
        return true;
    }

    private boolean isCorrect(HybridTree t) {
        for (Node v : t.nodes()) {
            if (!(((v.getInDegree() == 0 && v.equals(t.getRoot())) && v
                    .getOutDegree() == 2)
                    || (v.getInDegree() == 1 && v.getOutDegree() == 2) || (v
                    .getInDegree() == 1 && v.getOutDegree() == 0)))
                return false;
        }
        return true;
    }

}
