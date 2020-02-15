/*
 *   RestrictTree.java Copyright (C) 2020 Daniel H. Huson
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
