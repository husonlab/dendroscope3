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
