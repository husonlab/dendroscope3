/*
 *   ComputeClusterNetwork.java Copyright (C) 2020 Daniel H. Huson
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

import dendroscope.algorithms.clusternet.ClusterNetwork;
import dendroscope.consensus.SplitSystem;
import dendroscope.consensus.Taxa;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;

public class ComputeClusterNetwork {

    public HybridNetwork run(HybridTree t1, HybridTree t2) throws Exception {

        if ((new IsomorphismCheck()).run(t1, t2))
            return t1;

        Taxa taxa = new Taxa();
        for (Node node : t1.computeSetOfLeaves()) {
            String label = t1.getLabel(node);
            if (!label.equals("rho"))
                taxa.add(label);
        }
        taxa.add("rho");

        SplitSystem s1 = new SplitSystem(taxa, t1);
        SplitSystem s2 = new SplitSystem(taxa, t2);

        s1.addAll(s2);

        PhyloTree clusterNetwork = (new ClusterNetwork(taxa, s1)).apply();

        HybridNetwork n = new HybridNetwork(clusterNetwork, false, null, false);
        n.update();

        return n;
    }

}
