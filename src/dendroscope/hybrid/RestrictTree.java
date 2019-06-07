/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import jloda.graph.Edge;
import jloda.graph.Node;

import java.util.Collections;
import java.util.Vector;

/**
 * This function restricts a rooted, bifurcating phylogenetic tree to the
 * leaf-set of a second rooted, bifurcating phylogenetic tree.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class RestrictTree {

    public HybridTree run(HybridTree t2, HybridTree t1, Vector<Node> rootNodes, Node root, GetAgreementForest gAf) {

        HybridTree rT = new HybridTree(t2, false, t2.getTaxaOrdering());

        // compute leaf-set of f
        Vector<String> fTaxa = getTaxa(t1, rootNodes, root);

        if (fTaxa.size() == 0)
            return rT;

        if (gAf.getLeavesToTree(fTaxa) == null) {

            // compute intersection between leaf-set of f and t
            Vector<Node> leavesNotContained = new Vector<>();
            for (Node v : rT.computeSetOfLeaves()) {
                if (!fTaxa.contains(rT.getLabel(v)))
                    leavesNotContained.add(v);
            }

            // remove all leaves not contained in f (-> see method in class
            // HybridTree)
            for (Node v : leavesNotContained)
                rT.removeLeafNode(v);

            gAf.putLeavesToTree(fTaxa, new HybridTree(rT, false, null));
            return rT;

        } else
            return new HybridTree(gAf.getLeavesToTree(fTaxa), false, null);

    }

    private Vector<String> getTaxa(HybridTree t1, Vector<Node> rootNodes,
                                   Node root) {

        Vector<String> fTaxa = new Vector<>();
        initRec(t1, root, rootNodes, fTaxa);

        Collections.sort(fTaxa);

        return fTaxa;
    }

    private void initRec(HybridTree t1, Node v, Vector<Node> rootNodes, Vector<String> fTaxa) {

        for (Edge e : v.outEdges()) {
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
