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

package dendroscope.algorithms.utils;

import dendroscope.consensus.Cluster;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * compute a Hasse diagram
 * Daniel Huson, 4.2009
 */
public class HasseDiagram {
    /**
     * construct the Hasse diagram for a set of clusters
     *
     * @param clusters
     */
    public static PhyloTree constructHasse(Cluster[] clusters) {
        // make clusters unique:
        Set<Cluster> set = new HashSet<>();
        Collections.addAll(set, clusters);
        // sort
        clusters = Cluster.getClustersSortedByDecreasingCardinality(set.toArray(new Cluster[set.size()]));

        PhyloTree tree = new PhyloTree();

        // build the diagram
        Node root = tree.newNode();
        tree.setRoot(root);
        tree.setLabel(root, "" + new Cluster(Cluster.extractTaxa(clusters)));
        tree.setInfo(root, new Cluster());

        int[] cardinality = new int[clusters.length];
        Node[] nodes = new Node[clusters.length];

        for (int i = 0; i < clusters.length; i++) {
            cardinality[i] = clusters[i].cardinality();
            nodes[i] = tree.newNode();
            tree.setLabel(nodes[i], "" + clusters[i]);
            tree.setInfo(nodes[i], clusters[i]);
        }

        for (int i = 0; i < clusters.length; i++) {
            BitSet cluster = clusters[i];

            if (nodes[i].getInDegree() == 0) {
                tree.newEdge(root, nodes[i]);
            }

            BitSet covered = new BitSet();

            for (int j = i + 1; j < clusters.length; j++) {
                if (cardinality[j] < cardinality[i]) {
                    BitSet subCluster = clusters[j];
                    if (Cluster.contains(cluster, subCluster) && !Cluster.contains(covered, subCluster)) {
                        tree.newEdge(nodes[i], nodes[j]);
                        covered.or(subCluster);
                        // if (covered.cardinality() == cardinality[i]) break;
                    }
                }
            }
        }
        return tree;
    }
}
