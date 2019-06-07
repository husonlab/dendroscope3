/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import dendroscope.core.TreeData;
import jloda.phylo.PhyloTree;

public class Algorithm extends Thread {

    private final View view;
    private final Controller controller;
    private final PhyloTree[] trees;
    private final View.Computation compValue;
    private final boolean caMode;

    public Algorithm(PhyloTree[] trees, View view, Controller controller, View.Computation compValue, boolean caMode) {
        super();
        this.compValue = compValue;
        this.trees = trees;
        this.view = view;
        this.controller = controller;
        this.caMode = caMode;
    }

    public void run() {
        HybridManager hybridManager = new HybridManager(trees, view, controller, compValue, caMode);
        hybridManager.computeHybrid();
        TreeData[] treeData = hybridManager.getTreeData();
        controller.printTrees(treeData);
    }
}
