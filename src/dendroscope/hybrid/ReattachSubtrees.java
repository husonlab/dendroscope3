/**
 * ReattachSubtrees.java 
 * Copyright (C) 2019 Daniel H. Huson
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

import java.util.Iterator;

/**
 * This method replaces distinct leaves of a resolved network by rooted,
 * bifurcating phylogenetic trees.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class ReattachSubtrees {

    public HybridNetwork run(HybridNetwork n,
                             ReplacementInfo rI) {

        for (Node taxon : n.computeSetOfLeaves()) {
            String label = n.getLabel(taxon);
            // checking if label replaces a subtree
            if (rI.getLabelToSubtree().containsKey(label)) {
                // getting tree replaced by the label
                PhyloTree p = rI.getLabelToSubtree().get(label);
                // replacing label through tree
                addTreeToNetwork(p, n, taxon);
            }
        }

        return n;
    }

    private void addTreeToNetwork(PhyloTree p, HybridNetwork n, Node taxon) {
        Node vCopy = n.newNode(p.getRoot());
        n.setLabel(vCopy, p.getLabel(p.getRoot()));
        addTreeToNetworkRec(vCopy, p.getRoot(), p, n);

        // attaching tree p to network n
        // -> connect all in-edges of taxon to the root of the tree
        Iterator<Edge> it = taxon.getInEdges();
        while (it.hasNext()) {
            Edge e = it.next();
            boolean isSpecial = n.isSpecial(e);
            Node parent = e.getSource();
            n.deleteEdge(e);
            Edge eCopy = n.newEdge(parent, vCopy);
            if (isSpecial) {
                n.setSpecial(eCopy, true);
                n.setWeight(eCopy, 0);
            }
        }

        //delete taxon (taxon is now replaced by a common binary tree)
        n.deleteNode(taxon);

        n.initTaxaOrdering();
        n.update();
    }

    @SuppressWarnings("unchecked")
    private void addTreeToNetworkRec(Node vCopy, Node v, PhyloTree p,
                                     HybridNetwork n) {
        for (Edge e : v.outEdges()) {
            Node c = e.getTarget();
            Node cCopy = n.newNode(c);
            n.setLabel(cCopy, p.getLabel(c));
            n.newEdge(vCopy, cCopy);
            addTreeToNetworkRec(cCopy, c, p, n);
        }
    }
}
