/**
 * SubtreeReduction.java 
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

import jloda.graph.Node;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Vector;

/**
 * This function replaces all common subtrees of two two rooted, bifurcating
 * phylogenetic trees by a unique taxon labeling.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class SubtreeReduction {

    public void run(HybridTree t1, HybridTree t2, ReplacementInfo rI) throws Exception {

        Vector<BitSet> commonSubtrees = new Vector<>();
        Vector<Node> commonNodes = new Vector<>();

        Iterator<Node> it = t2.postOrderWalk();
        while (it.hasNext()) {
            Node v = it.next();
            if (v.getOutDegree() == 2) {
                Iterator<Node> it2 = t2.getSuccessors(v);
                Node l = it2.next();
                Node r = it2.next();

                // true if node l is the root of a common subtree
                Boolean bL = commonNodes.contains(l);
                // true if node r is the root of a common subtree
                Boolean bR = commonNodes.contains(r);
                // true if node v is the root of a common subtree
                Boolean bV = t1.getClusterSet().contains(
                        t2.getNodeToCluster().get(v));

                if (bL && bR) {
                    if (!bV) {
                        addSubtree(t2.getNodeToCluster().get(l),
                                commonSubtrees, t1.getTaxaOrdering().size());
                        addSubtree(t2.getNodeToCluster().get(r),
                                commonSubtrees, t1.getTaxaOrdering().size());
                    } else if (t2.getRoot().equals(v)) {
                        addSubtree(t2.getNodeToCluster().get(v),
                                commonSubtrees, t1.getTaxaOrdering().size());
                    } else if (bV)
                        commonNodes.add(v);
                } else if (bL && !bR)
                    addSubtree(t2.getNodeToCluster().get(l), commonSubtrees, t1
                            .getTaxaOrdering().size());
                else if (!bL && bR)
                    addSubtree(t2.getNodeToCluster().get(r), commonSubtrees, t1
                            .getTaxaOrdering().size());
            } else {
                // at the beginning each leaf is a possible root of a common
                // subtree
                commonNodes.add(v);
            }
        }

        t1.replaceClusters(commonSubtrees, rI);
        t2.replaceClusters(commonSubtrees, rI);

    }

    private void addSubtree(BitSet b, Vector<BitSet> v, int size) {
        // common subtree must not be a single leaf or the whole tree
        if (b.cardinality() > 1 && b.cardinality() < size && !v.contains(b)) {
            v.add(b);
        }
    }
}
