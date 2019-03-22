/**
 * ComputeRootedSPRDistanceCommand.java 
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
package dendroscope.commands.hybrid;

import dendroscope.hybrid.Controller;
import dendroscope.hybrid.View;
import dendroscope.window.TreeViewer;
import jloda.phylo.PhyloTree;
import jloda.phylo.PhyloTreeUtils;
import jloda.swing.util.Alert;
import jloda.swing.util.Message;
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
