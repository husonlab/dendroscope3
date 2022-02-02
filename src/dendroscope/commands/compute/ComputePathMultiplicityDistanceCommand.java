/*
 * ComputePathMultiplicityDistanceCommand.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.commands.compute;

import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.util.DistanceMethods;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.phylo.PhyloTree;
import jloda.swing.commands.ICommand;
import jloda.swing.util.Alert;
import jloda.swing.util.Message;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * compute hardwired distance Celine Scornavacca, 7.2010
 */
public class ComputePathMultiplicityDistanceCommand extends
        CommandBaseMultiViewer implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        List<PhyloTree> trees = new LinkedList<>();
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid()
                .getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            PhyloTree tree = viewer.getPhyloTree();
            trees.add(tree);
        }

        if (trees.size() == 2) {
            double distance = DistanceMethods.computePathMultiplicityDistance(trees);
            Message.show(getViewer().getFrame(), "Path multiplicity distance: "
												 + distance);
            System.out.println("Path multiplicity distance: " + distance);
        } else {
            new Alert(getViewer().getFrame(),
                    "Distance calculation requires two trees or networks\n");
        }
    }

    public String getSyntax() {
        return "compute distance method=pathMultiplicity;";
    }

    public void actionPerformed(ActionEvent ev) {
        execute(getSyntax());
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Calculate path multiplicity distance between two trees or networks";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getName() {
        return "Path Multiplicity Distance...";
    }

    public String getUndo() {
        return null;
    }

    public boolean isApplicable() {
        return multiViewer.getTreeGrid().getNumberSelectedOrAllViewers() == 2
                && ((MultiViewer) getViewer()).getDir().getDocument()
                .getNumberOfTrees() > 0;
    }

    public boolean isCritical() {
        return true;
    }
}
