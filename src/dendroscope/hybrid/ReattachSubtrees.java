/**
 * Copyright 2015, Daniel Huson
 *
 *(Some files contain contributions from other authors, who are then mentioned separately)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        Iterator<Edge> it = p.getOutEdges(v);
        while (it.hasNext()) {
            Node c = it.next().getTarget();
            Node cCopy = n.newNode(c);
            n.setLabel(cCopy, p.getLabel(c));
            n.newEdge(vCopy, cCopy);
            addTreeToNetworkRec(cCopy, c, p, n);
        }
    }
}
