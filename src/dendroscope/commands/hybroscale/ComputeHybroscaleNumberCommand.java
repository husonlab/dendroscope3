/**
 * ComputeHybridNumberCommand.java 
 * Copyright (C) 2015 Daniel H. Huson
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
package dendroscope.commands.hybroscale;

import dendroscope.hybroscale.controller.HybroscaleController;
import dendroscope.hybroscale.model.HybridManager.Computation;
import dendroscope.window.TreeViewer;
import jloda.phylo.PhyloTree;
import jloda.phylo.PhyloTreeUtils;
import jloda.util.Alert;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.Vector;


public class ComputeHybroscaleNumberCommand extends ComputeHybroscaleCommand {
    public void apply(NexusStreamParser np) throws Exception {
    	
        np.matchIgnoreCase("compute hybridization-number method=Albrecht2015");
        boolean showDialog = false;
        if (np.peekMatchIgnoreCase("showdialog=")) {
            np.matchIgnoreCase("showdialog=");
            showDialog = np.getBoolean();
        }
        int availableCores = 1;
        if (np.peekMatchIgnoreCase("numberOfThreads=")) {
            np.matchIgnoreCase("numberOfThreads=");
            availableCores = np.getInt(1, 1000);
        }
        np.matchIgnoreCase(";");

        if (multiViewer.getTreeGrid().getNumberSelectedOrAllViewers() < 2) {
            new Alert(getViewer().getFrame(), "Please select at least two trees!");
            return;
        }

		Iterator<TreeViewer> it0 = multiViewer.getTreeGrid().getSelectedOrAllIterator();
		Vector<PhyloTree> selectedTrees = new Vector<PhyloTree>();
		while (it0.hasNext()) {
			TreeViewer tV = it0.next();
			PhyloTree t = tV.getPhyloTree();
			if (!PhyloTreeUtils.areSingleLabeledTrees(t)) {
				new Alert(getViewer().getFrame(), "The selected tree '" + t.getName() + "' is NOT single-labeled!");
				return;
			} else
				selectedTrees.add(t);
		}

		String[] treeStrings = new String[selectedTrees.size()];
		for (int i = 0; i < selectedTrees.size(); i++)
			treeStrings[i] = selectedTrees.get(i).toBracketString();

		HybroscaleController controller = new HybroscaleController(treeStrings, this, getViewer().getFrame(),
				Computation.EDGE_NUMBER, availableCores, showDialog);

		if (!showDialog)
			controller.run(Computation.EDGE_NETWORK, availableCores, null, false);

		synchronized (this) {
			this.wait();
		}

    }

    public void actionPerformed(ActionEvent ev) {
        execute("compute hybridization-number method=Albrecht2015 showdialog=true;");
    }

    public String getSyntax() {
        return "compute hybridization-number method=Albrecht2015 [showdialog={false|true}] [numberOfThreads=number];";
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Compute the hybridization number for a set of non-binary trees using (Albrecht, Scornavacca, Cenci, and Huson, 2011)";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getName() {
        return "Hybridization Number (Hybroscale)...";
    }

}
