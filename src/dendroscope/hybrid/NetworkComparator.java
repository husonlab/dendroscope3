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

import java.util.*;

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
        for (HybridNetwork n : networks) {
            for (Node v : n.getNodes()) {
                if (v.getInDegree() == 2)
                    computeCluster(n, v);
            }
        }
        for (HybridNetwork n : networks) {
            for (Node v : n.getNodes()) {
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
            for (Node v : n.getNodes()) {
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
            Iterator<Edge> it = n.getOutEdges(v);
            while (it.hasNext())
                computeClusterRec(n, it.next().getTarget(), cluster);
        }
    }

    public Integer getWeight(HybridNetwork n) {
        return networkToWeight.get(n);
    }
}
