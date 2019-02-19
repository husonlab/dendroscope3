/**
 * ClusterNetFromMultree.java 
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
