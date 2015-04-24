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

package dendroscope.commands.compute;

import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.util.DistanceMethods;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.gui.Message;
import jloda.gui.commands.ICommand;
import jloda.phylo.PhyloTree;
import jloda.util.Alert;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ComputeNetworkProperties extends CommandBaseMultiViewer
        implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        List<PhyloTree> trees = new LinkedList<>();
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid()
                .getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            PhyloTree tree = viewer.getPhyloTree();
            trees.add(tree);
        }

        if (trees.size() == 1) {
            new Message(getViewer().getFrame(), "Tree-child property: "
                    + DistanceMethods.hasTreeChildProperty(trees.get(0)) + "\n" + "Tree-sibling property: "
                    + DistanceMethods.hasTreeSiblingProperty(trees.get(0)) + "\n" + "Time-consistent property: "
                    + DistanceMethods.hasTimeConsistentProperty(trees.get(0)));
            System.out.println("Tree-child property: "
                    + DistanceMethods.hasTreeChildProperty(trees.get(0)) + "\n" + "Tree-sibling property: "
                    + DistanceMethods.hasTreeSiblingProperty(trees.get(0)) + "\n" + "Time-consistent property: "
                    + DistanceMethods.hasTimeConsistentProperty(trees.get(0)));
        } else {
            new Alert(getViewer().getFrame(),
                    "Calculation of properties requires only one network.");
        }
    }

    public String getSyntax() {
        return "compute distance method=networkProperties;";
    }

    public void actionPerformed(ActionEvent ev) {
        execute(getSyntax());
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Calculate properties of one network";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getName() {
        return "Network Properties...";
    }

    public String getUndo() {
        return null;
    }

    public boolean isApplicable() {
        return multiViewer.getTreeGrid().getNumberSelectedOrAllViewers() == 1
                && ((MultiViewer) getViewer()).getDir().getDocument()
                .getNumberOfTrees() > 0;
    }

    public boolean isCritical() {
        return true;
    }

}
