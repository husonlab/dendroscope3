/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;

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
        for (Edge e : taxon.inEdges()) {
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
    private void addTreeToNetworkRec(Node vCopy, Node v, PhyloTree p, HybridNetwork n) {
        for (Edge e : v.outEdges()) {
            Node c = e.getTarget();
            Node cCopy = n.newNode(c);
            n.setLabel(cCopy, p.getLabel(c));
            n.newEdge(vCopy, cCopy);
            addTreeToNetworkRec(cCopy, c, p, n);
        }
    }
}
