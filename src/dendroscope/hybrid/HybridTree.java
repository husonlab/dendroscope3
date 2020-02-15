/*
 *   HybridTree.java Copyright (C) 2020 Daniel H. Huson
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
import jloda.phylo.PhyloTree;

import java.util.Collections;
import java.util.Vector;

/**
 * This class represents a rooted, bifurcating phylogenetic tree.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class HybridTree extends HybridNetwork {

    public HybridTree(PhyloTree t, boolean rootTree, Vector<String> taxaOrdering) {
        super(t, rootTree, taxaOrdering);
    }

    public HybridTree(HybridNetwork t, boolean rootTree, Vector<String> taxaOrdering) {
        super(t, rootTree, taxaOrdering);
    }

    // returns subtree under node v
    public HybridTree getSubtree(Node v, boolean doUpdate) {
        PhyloTree sT = new PhyloTree();
        if (contains(v)) {
            Node vCopy = sT.newNode(v);
            sT.setLabel(vCopy, getLabel(v));
            sT.setRoot(vCopy);
            createSubtreeRec(v, vCopy, sT);
        }
        HybridTree newTree = new HybridTree(sT, false, super.getTaxaOrdering());
        if (doUpdate)
            newTree.update();

        for (Vector<String> key : this.getTaxaPairToWeight().keySet()) {
            int value = this.getTaxaPairToWeight().get(key);
            newTree.taxaPairToWeight.put(key, value);
        }

        return newTree;
    }

    @SuppressWarnings("unchecked")
    private void createSubtreeRec(Node v, Node vCopy, PhyloTree t) {
        for (Edge e : v.outEdges()) {
            Node c = e.getTarget();
            Node cCopy;
            cCopy = t.newNode(c);
            t.setLabel(cCopy, getLabel(c));
            t.newEdge(vCopy, cCopy);
            createSubtreeRec(c, cCopy, t);
        }
    }

    // removes leaf from tree
    public void removeLeafNode(Node v) {
        Node p = v.getFirstInEdge().getSource();
        deleteEdge(v.getFirstInEdge());
        deleteNode(v);
        if (getRoot().equals(p)) {
            Node c = p.getFirstOutEdge().getTarget();

            deleteEdge(p.getFirstOutEdge());
            deleteNode(p);

            setRoot(c);
        } else {
            Node pP = p.getFirstInEdge().getSource();
            Node c = p.getFirstOutEdge().getTarget();

            deleteEdge(p.getFirstInEdge());
            deleteEdge(p.getFirstOutEdge());
            deleteNode(p);

            newEdge(pP, c);
        }
        update();
    }

    public void replaceCommonChain(Node v, Vector<String> chainLabels) {

        Node end = v;

        Vector<Edge> edgesToBeRemoved = new Vector<>();
        Vector<Node> nodesToBeRemoved = new Vector<>();

        for (int i = 1; i < chainLabels.size() - 1; i++) {
            Node p = v.getFirstInEdge().getSource();
            Node c = getLeaves(p).firstElement();

            for (Edge e : p.outEdges()) {
                if (!edgesToBeRemoved.contains(e))
                    edgesToBeRemoved.add(e);
            }
            edgesToBeRemoved.add(p.getFirstInEdge());

            nodesToBeRemoved.add(p);
            nodesToBeRemoved.add(c);

            v = p;
        }

        Node start = v.getFirstInEdge().getSource();

        int edgeWeight = chainLabels.size() - 2;
        for (Edge e : edgesToBeRemoved) {
            if (edgeToWeight.containsKey(e))
                edgeWeight += edgeToWeight.get(e);
            deleteEdge(e);
        }
        for (Node n : nodesToBeRemoved)
            deleteNode(n);

        Edge e = newEdge(start, end);
        edgeToWeight.put(e, edgeWeight);

        Vector<String> taxaPair = new Vector<>();
        taxaPair.add(chainLabels.firstElement());
        taxaPair.add(chainLabels.lastElement());
        Collections.sort(taxaPair);
        taxaPairToWeight.put(taxaPair, edgeWeight);

        initTaxaOrdering();
        update();
    }
}
