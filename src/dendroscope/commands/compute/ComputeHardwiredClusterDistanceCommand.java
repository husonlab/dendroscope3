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
import dendroscope.io.DistanceOutput;
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

/**
 * compute hardwired distance
 * Celine Scornavacca, 7.2010
 */
public class ComputeHardwiredClusterDistanceCommand extends CommandBaseMultiViewer implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        List<PhyloTree> trees = new LinkedList<>();
        List<String> names = new LinkedList<>();
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            PhyloTree tree = viewer.getPhyloTree();
            names.add(viewer.getName());
            trees.add(tree);
        }

        if (trees.size() == 2) {
            double distance = DistanceMethods.computeHardwiredClusterDistance(trees)[0][1];
            new Message(getViewer().getFrame(), "Hardwired cluster distance: " + distance);
            System.out.println("Hardwired cluster distance: " + distance);
        } else if (trees.size() > 2) {
            String[] namesArray = names.toArray(new String[names.size()]);
            double[][] distances = DistanceMethods.computeHardwiredClusterDistance(trees);
            System.out.println("[You can paste the following lines straight into SplitsTree]");
            System.out.print(DistanceOutput.toNexusString(distances, namesArray));
            new Message(getViewer().getFrame(), "Hardwired cluster distances:\n" + DistanceOutput.toSimpleString(distances, namesArray));

        } else {
            new Alert(getViewer().getFrame(), "Distance calculation requires two trees or networks\n");
        }
    }


    public String getSyntax() {
        return "compute distance method=hardwired;";
    }


    public void actionPerformed(ActionEvent ev) {
        execute(getSyntax());
    }


    public KeyStroke getAcceleratorKey() {
        return null;
    }


    public String getDescription() {
        return "Calculate hardwired cluster distance between two trees or networks";
    }

    public ImageIcon getIcon() {
        return null;
    }


    public String getName() {
        return "Hardwired Cluster Distance...";
    }


    public String getUndo() {
        return null;
    }


    public boolean isApplicable() {
        return multiViewer.getTreeGrid().getNumberSelectedOrAllViewers() >= 2 && ((MultiViewer) getViewer()).getDir().getDocument().getNumberOfTrees() > 0;
    }


    public boolean isCritical() {
        return true;
    }
}
