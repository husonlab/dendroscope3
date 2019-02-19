/**
 * AddHybridNode.java 
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
package dendroscope.autumn.hybridnetwork;

import dendroscope.autumn.Root;
import jloda.graph.Edge;
import jloda.graph.NodeSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * adds a hybrid node to all given networks
 * Daniel Huson, 5.2011
 */
public class AddHybridNode {
    /**
     * add hybrid node to all given networks
     *
     * @param networks
     * @param taxon
     */
    public static void apply(Collection<Root> networks, int taxon) {
        Root[] roots = networks.toArray(new Root[networks.size()]); // need to use array as networks will change
        networks.clear();
        for (Root root : roots) {
            // System.err.println("TRYING: " + root.toStringNetworkFull());
            Root hybrid = root.newNode();
            hybrid.getTaxa().set(taxon);
            Set<Root> toDelete = new HashSet<Root>();
            applyRec(root, hybrid, taxon, new NodeSet(root.getOwner()), toDelete);

            for (Root v : toDelete) {
                if (v.getInDegree() != 1 || v.getOutDegree() != 1)
                    throw new RuntimeException("v must have indeg=outdeg=1");
                Edge eIn = v.getFirstInEdge();
                Edge eOut = v.getFirstOutEdge();
                Root u = (Root) eIn.getSource();
                Root w = (Root) eOut.getTarget();
                Edge f = v.newEdge(u, w);
                f.setInfo(eOut.getInfo());
                v.deleteNode();
            }
            if (hybrid.getInDegree() != 2)
                throw new RuntimeException("AddHybridNode (taxon=" + taxon + "): bad indegree: " + hybrid.getInDegree());
            Edge e1 = hybrid.getFirstInEdge();
            Edge e2 = hybrid.getLastInEdge();
            if (!(e1.getInfo() instanceof Integer) || !(e1.getInfo() instanceof Integer) || e1.getInfo().equals(e2.getInfo()))
                throw new RuntimeException("AddHybridNode (taxon=" + taxon + "): inedges have wrong labels: " + e1.getInfo() + " and " + e1.getInfo());
            networks.add(root);
        }
    }

    /**
     * recursively does the work
     *
     * @param v
     * @param hybrid
     * @param taxon
     * @param toDelete
     */
    private static void applyRec(Root v, Root hybrid, int taxon, NodeSet visited, Set<Root> toDelete) {
        if (v.getRemovedTaxa().get(taxon) && !visited.contains(v)) {
            visited.add(v);
            v.getRemovedTaxa().set(taxon, false);
            v.getTaxa().set(taxon);
            if (v.getOutDegree() == 0) {
                Integer treeId = (Integer) v.getFirstInEdge().getInfo();
                if (treeId == null)
                    throw new RuntimeException("Reticulation in-edge without tree-id");
                Edge f = v.newEdge(v, hybrid);
                f.setInfo(treeId);
                if (v.getInDegree() == 1)
                    toDelete.add(v);
            } else {
                for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                    applyRec((Root) e.getTarget(), hybrid, taxon, visited, toDelete);
                }
            }
        }
    }
}
