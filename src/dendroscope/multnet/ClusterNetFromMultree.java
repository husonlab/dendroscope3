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

package dendroscope.multnet;

import dendroscope.algorithms.clusternet.ClusterNetwork;
import jloda.phylo.PhyloTree;

/**
 * constructs a clusternetwork for a given multi-labeled tree.
 *
 * @author thomas bonfert, 6.2009
 */
public class ClusterNetFromMultree {

    private final PhyloTree tree;
    private ClusterNetwork clusterNetwork;

    public ClusterNetFromMultree(PhyloTree tree) {
        this.tree = tree;
    }

    public PhyloTree apply() {
        MultilabeledTree mulTree = new MultilabeledTree();
        mulTree.copy(this.tree);
        //adding a pseudo root as outgroup. this outgroup is used in the class 'ClusterNetwork' to determine the clusters of the
        //splitsystem.

        mulTree.addOutgroup();
        mulTree.adaptLabeling();
        this.clusterNetwork = new ClusterNetwork(mulTree.getTaxa(), mulTree.getSplitSystem());
        return (this.clusterNetwork.apply());
    }
}
