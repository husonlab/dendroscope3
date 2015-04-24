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

package dendroscope.commands.hybrid;

import dendroscope.hybrid.Controller;
import dendroscope.hybrid.View;
import dendroscope.window.TreeViewer;
import jloda.gui.Message;
import jloda.phylo.PhyloTree;
import jloda.phylo.PhyloTreeUtils;
import jloda.util.Alert;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

public class ComputeRootedSPRDistanceCommand extends ComputeHybridCommand {
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("compute rspr-distance method=SAH2011");
        boolean showDialog = false;
        if (np.peekMatchIgnoreCase("showdialog=")) {
            np.matchIgnoreCase("showdialog=");
            showDialog = np.getBoolean();
        }
        int numberOfThreads = -1;
        if (np.peekMatchIgnoreCase("numberOfThreads=")) {
            np.matchIgnoreCase("numberOfThreads=");
            numberOfThreads = np.getInt(1, 1000);
        }
        np.matchIgnoreCase(";");

        if (multiViewer.getTreeGrid().getNumberSelectedOrAllViewers() != 2) {
            new Alert(getViewer().getFrame(), "Wrong number of trees: " + multiViewer.getTreeGrid().getNumberSelectedOrAllViewers());
            return;
        }

        Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator();
        PhyloTree[] treeArray = new PhyloTree[]{it.next().getPhyloTree(), it.next().getPhyloTree()};

        if (!PhyloTreeUtils.areSingleLabeledTreesWithSameTaxa(treeArray[0], treeArray[1])) {
            new Alert(getViewer().getFrame(), "Trees have different label sets");
            return;
        }

        if (!PhyloTreeUtils.isBifurcatingTree(treeArray[0])) {
            new Alert(getViewer().getFrame(), "Not a bifurcating tree: first");
            return;
        }
        if (!PhyloTreeUtils.isBifurcatingTree(treeArray[1])) {
            new Alert(getViewer().getFrame(), "Not a bifurcating tree: second");
            return;
        }

        Controller controller = new Controller(treeArray, this);
        if (numberOfThreads > 0)
            controller.setCores(numberOfThreads);

        if (showDialog) {
            View view = new View(getViewer().getFrame(), controller, View.Computation.rSPR_DISTANCE);
            controller.setView(view);
            view.run();
            synchronized (this) {
                this.wait();
            }
        } else { // non-gui mode
            controller.run(View.Computation.rSPR_DISTANCE, false);
            synchronized (this) {
                this.wait();
            }
            if (controller.getrSPRDistance() != -1)
                new Message(getViewer().getFrame(), "rSPR distance: " + controller.getrSPRDistance());
        }
    }

    public void actionPerformed(ActionEvent ev) {
        execute("compute rspr-distance method=SAH2011 showdialog=true;");
    }

    public String getSyntax() {
        return "compute rspr-distance method=SAH2011 [showdialog={false|true}] [numberOfThreads=number];";
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Compute the rSPR distance for two selected rooted binary trees";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getName() {
        return "rSPR Distance (Binary Trees)...";
    }
}
