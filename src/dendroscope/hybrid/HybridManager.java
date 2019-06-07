/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import dendroscope.core.TreeData;
import jloda.phylo.PhyloTree;

import java.util.Vector;

/**
 * Manages the computation of hybrid networks.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class HybridManager {

    private final Vector<TreeData> treeData = new Vector<>();
    private final Hybrid hybrid;

    public HybridManager(PhyloTree[] trees, View view, Controller c, View.Computation compValue, boolean caMode) {
        hybrid = new Hybrid(trees, view, c, compValue, caMode);
    }

    /**
     * computes a hybrid network gram using the named method
     *
     * @return Hybrid
     */
    public void computeHybrid() {
        Vector<HybridNetwork> networks = hybrid.getNetworks();
        if (networks.size() > 0) {
            System.out.println("Networks: " + networks.size());
            for (HybridNetwork n : networks)
                treeData.add(new TreeData(n));
        }
    }

    public TreeData[] getTreeData() {
        return treeData.toArray(new TreeData[treeData.size()]);
    }

    public Hybrid getHybrid() {
        return hybrid;
    }

}
