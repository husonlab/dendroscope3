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
