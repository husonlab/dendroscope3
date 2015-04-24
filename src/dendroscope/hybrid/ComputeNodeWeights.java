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

import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.graph.Edge;
import jloda.graph.Graph;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class ComputeNodeWeights {

    private final Hashtable<BitSet, Double> nodeToNumber = new Hashtable<>();
    private final Hashtable<Node, BitSet> nodeToCluster = new Hashtable<>();

    private final Vector<HybridNetwork> networks;
    private final double size;
    private final Vector<String> taxaOrdering;
    private MultiViewer mV;

    public ComputeNodeWeights(Vector<HybridNetwork> networks) {
        this.networks = networks;
        size = networks.size();
        taxaOrdering = networks.firstElement().getTaxaOrdering();
    }

    public void showOccurrences() {

        if (nodeToNumber.size() == 0)
            computeOcurrences();

        Iterator<TreeViewer> it = mV.getTreeGrid().getIterator();
        while (it.hasNext()) {

            TreeViewer viewer = it.next();
            Graph g = viewer.getGraph();
            PhyloTree n = (PhyloTree) g;

            for (Node v : n.getNodes()) {
                if (v.getInDegree() == 2) {
                    BitSet b = computeCluster(n, v);
                    double perc = (nodeToNumber.get(b) / size) * 100.0;
                    int occ = (int) Math.round(perc);
                    n.setLabel(v, occ + "%");
                }
            }

            viewer.resetViews();
            viewer.repaint();
        }

    }

    public void hideOccurrences() {
        Iterator<TreeViewer> it = mV.getTreeGrid().getIterator();
        while (it.hasNext()) {

            TreeViewer viewer = it.next();
            Graph g = viewer.getGraph();
            PhyloTree n = (PhyloTree) g;

            for (Node v : n.getNodes()) {
                if (nodeToCluster.containsKey(v)) {
                    n.setLabel(v, "");
                }
            }

            viewer.resetViews();
            viewer.repaint();
        }
    }

    private void computeOcurrences() {

        for (HybridNetwork n : networks) {

            Iterator<Node> it = n.nodeIterator();
            while (it.hasNext()) {
                Node v = it.next();
                if (v.getInDegree() == 2) {
                    BitSet cluster = computeCluster(n, v);
                    if (nodeToNumber.containsKey(cluster)) {
                        double num = nodeToNumber.get(cluster) + 1.0;
                        nodeToNumber.remove(cluster);
                        nodeToNumber.put(cluster, num);
                    } else
                        nodeToNumber.put(cluster, 1.0);
                }
            }

        }

    }

    // private void computeOcurrences() {
    //
    // Iterator<TreeViewer> itV = mV.getTreeGrid().getIterator();
    // while (itV.hasNext()) {
    //
    // TreeViewer viewer = itV.next();
    // Graph g = viewer.getGraph();
    // PhyloTree n = (PhyloTree) g;
    //
    // Iterator<Node> it = n.nodeIterator();
    // while (it.hasNext()) {
    // Node v = it.next();
    // if (v.getInDegree() == 2) {
    // BitSet cluster = computeCluster(n, v);
    // if (nodeToNumber.containsKey(cluster)) {
    // double num = nodeToNumber.get(cluster) + 1;
    // nodeToNumber.remove(cluster);
    // nodeToNumber.put(cluster, num);
    // } else
    // nodeToNumber.put(cluster, 1.0);
    // }
    // }
    //
    // }
    //
    // }

    private BitSet computeCluster(PhyloTree n, Node v) {

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
            while (it.hasNext()) {
                Node c = it.next().getTarget();
                if (c.getInDegree() == 1)
                    computeClusterRec(n, c, cluster);
            }
        }
    }

    public void setMultiViewer(MultiViewer mV) {
        this.mV = mV;
    }

}
