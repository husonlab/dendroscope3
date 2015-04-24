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
