/**
 * HybridTree.java 
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
package dendroscope.hybrid;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.util.Collections;
import java.util.Iterator;
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
        Iterator<Edge> it = getOutEdges(v);
        while (it.hasNext()) {
            Edge e = it.next();
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
        Node p = v.getInEdges().next().getSource();
        deleteEdge(v.getInEdges().next());
        deleteNode(v);
        if (getRoot().equals(p)) {
            Node c = p.getOutEdges().next().getTarget();

            deleteEdge(p.getOutEdges().next());
            deleteNode(p);

            setRoot(c);
        } else {
            Node pP = p.getInEdges().next().getSource();
            Edge e = p.getOutEdges().next();
            Node c = e.getTarget();

            deleteEdge(p.getInEdges().next());
            deleteEdge(p.getOutEdges().next());
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
            Node p = v.getInEdges().next().getSource();
            Node c = getLeaves(p).firstElement();

            Iterator<Edge> it = p.getOutEdges();
            while (it.hasNext()) {
                Edge e = it.next();
                if (!edgesToBeRemoved.contains(e))
                    edgesToBeRemoved.add(e);
            }
            edgesToBeRemoved.add(p.getInEdges().next());

            nodesToBeRemoved.add(p);
            nodesToBeRemoved.add(c);

            v = p;
        }

        Node start = v.getInEdges().next().getSource();

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
