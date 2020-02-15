/*
 *   NetworkComparator.java Copyright (C) 2020 Daniel H. Huson
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

import java.util.BitSet;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Vector;

public class NetworkComparator implements Comparator<HybridNetwork> {

    private final Hashtable<BitSet, Integer> nodeToOcc = new Hashtable<>();
    private final Hashtable<HybridNetwork, Integer> networkToWeight = new Hashtable<>();
    private final Hashtable<Node, BitSet> nodeToCluster = new Hashtable<>();
    private final Vector<String> taxaOrdering;

    public NetworkComparator(Vector<HybridNetwork> networks) {
        taxaOrdering = networks.firstElement().getTaxaOrdering();
        initTable(networks);
    }

    public int compare(HybridNetwork arg0, HybridNetwork arg1) {
        if (networkToWeight.get(arg0) < networkToWeight.get(arg1))
            return 1;
        else if (networkToWeight.get(arg0) > networkToWeight.get(arg1))
            return -1;
        return 0;
    }

    private void initTable(Vector<HybridNetwork> networks) {
        for (HybridNetwork network : networks) {
            for (Node v : network.nodes()) {
                if (v.getInDegree() == 2)
                    computeCluster(network, v);
            }
        }
        for (HybridNetwork n : networks) {
            for (Node v : n.nodes()) {
                if (v.getInDegree() == 2) {
                    BitSet b = nodeToCluster.get(v);
                    if (nodeToOcc.containsKey(b)) {
                        int occ = nodeToOcc.get(b) + 1;
                        nodeToOcc.remove(b);
                        nodeToOcc.put(b, occ);
                    } else
                        nodeToOcc.put(b, 1);
                }
            }
        }
        for (HybridNetwork n : networks) {
            int weight = 0;
            for (Node v : n.nodes()) {
                if (v.getInDegree() == 2) {
                    BitSet b = nodeToCluster.get(v);
                    weight += nodeToOcc.get(b);
                }
            }
            networkToWeight.put(n, weight);
        }
    }

    private BitSet computeCluster(HybridNetwork n, Node v) {

        BitSet cluster = new BitSet(taxaOrdering.size());
        computeClusterRec(n, v, cluster);
        nodeToCluster.put(v, cluster);

        return cluster;
    }

    private void computeClusterRec(PhyloTree n, Node v, BitSet cluster) {
        if (v.getOutDegree() == 0)
            cluster.set(taxaOrdering.indexOf(n.getLabel(v)));
        else if (nodeToCluster.containsKey(v))
            cluster.or(nodeToCluster.get(v));
        else {
            for (Edge e : v.outEdges()) {
                computeClusterRec(n, e.getTarget(), cluster);
            }
        }
    }

    public Integer getWeight(HybridNetwork n) {
        return networkToWeight.get(n);
    }
}
