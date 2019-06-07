/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import jloda.graph.Edge;
import jloda.graph.Node;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

/**
 * This function checks whether a set of subtrees of a rooted, bifurcating
 * phylogenetic tree is a legal forest.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class CheckAgreementForest {

    private final HashSet<BitSet> illegalTrees = new HashSet<>();
    private HybridTree rT;

    public boolean run(HybridTree t2, GetAgreementForest gAf,
                       Vector<Node> rootNodes, HybridTree t1) {

        for (Node v : rootNodes) {
            BitSet set = getTaxa(t1, rootNodes, v);
            if (set.cardinality() > 2) {
                // restrict tree t to the leaf-set of f
                rT = (new RestrictTree()).run(t2, t1, rootNodes, v, gAf);

                boolean b1 = (new FastIsomorphismCheck().run(t1, rootNodes, v,
                        rT));
                // boolean b2 = new IsomorphismCheck().run(forest.get(rootNodes
                // .indexOf(v)), rT);

                // check whether the restricted tree and t are isomorphic
                if (!b1) {
                    reportIllegalTree(v, rootNodes, gAf);
                    illegalTrees.add(set);
                    //System.out.println("IsomorphismCheck "+false);
                    return false;
                }
            }
        }

        // check whether all trees of the forest are node-disjoint in t

        boolean b1 = (new FastNodeDisjointCheck().run(t1, rootNodes, t2));
        // boolean b2 = new NodeDisjointCheck().run(t2,forest);

        if (!b1)
            return false;

        // all tests positive - returning true
        return true;
    }

    public HashSet<BitSet> getIllegalTrees() {
        return illegalTrees;
    }

    public HybridTree getrT() {
        return rT;
    }

    private void reportIllegalTree(Node v, Vector<Node> rootNodes,
                                   GetAgreementForest gAf) {
        BitSet b = new BitSet(gAf.getSortedEdges().size());
        if (v.getInDegree() == 1) {
            Edge e = v.getFirstInEdge();
            getEdgeSetRec(e, b, rootNodes, gAf);
            gAf.reportIllegalTree(gAf.getSortedEdges().indexOf(e), b);
        }
    }

    private void getEdgeSetRec(Edge e, BitSet b, Vector<Node> rootNodes,
                               GetAgreementForest gAf) {
        Node v = e.getTarget();
        Iterator<Edge> it = v.outEdges().iterator();
        while (it.hasNext()) {
            Edge out = it.next();
            Node t = out.getTarget();
            if (rootNodes.contains(t))
                b.set(gAf.getSortedEdges().indexOf(out));
            else
                getEdgeSetRec(out, b, rootNodes, gAf);
        }
    }

    private BitSet getTaxa(HybridTree t1, Vector<Node> rootNodes, Node root) {

        Vector<String> fTaxa = new Vector<>();
        if (root.getOutDegree() != 0)
            initRec(t1, root, rootNodes, fTaxa);
        else
            fTaxa.add(t1.getLabel(root));

        BitSet treeSet = new BitSet(t1.getTaxaOrdering().size());
        for (String s : fTaxa)
            treeSet.set(t1.getTaxaOrdering().indexOf(s));

        return treeSet;
    }

    private void initRec(HybridTree t1, Node v, Vector<Node> rootNodes,
                         Vector<String> fTaxa) {
        Iterator<Edge> it = v.outEdges().iterator();
        while (it.hasNext()) {
            Edge e = it.next();
            Node t = e.getTarget();
            if (!rootNodes.contains(t)) {
                if (t.getOutDegree() == 0) {
                    fTaxa.add(t1.getLabel(t));
                } else
                    initRec(t1, t, rootNodes, fTaxa);
            }

        }
    }
}
