/*
 * SubtreeReductionCommand.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.commands.autumn;

import dendroscope.autumn.hybridnetwork.SubtreeReduction;
import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.core.Director;
import dendroscope.core.TreeData;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirector;
import jloda.util.FileUtils;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * performs a subtree reduction on two trees
 * Daniel Huson, 4.2011
 */
public class SubtreeReductionCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * parses the given command and executes it
     *
	 */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

		Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator();
		TreeViewer viewer1 = it.next();
		Set<String> selectedLabels = new HashSet<>(viewer1.getSelectedNodeLabels());
		TreeData tree1 = getDir().getDocument().getTree(multiViewer.getTreeGrid().getNumberOfViewerInDocument(viewer1));
        TreeViewer viewer2 = it.next();
        selectedLabels.addAll(viewer2.getSelectedNodeLabels());
        TreeData tree2 = getDir().getDocument().getTree(multiViewer.getTreeGrid().getNumberOfViewerInDocument(viewer2));

        TreeData[] newTrees = SubtreeReduction.apply(tree1, tree2, selectedLabels, true);

        if (newTrees != null) {
            for (int i = 0; i < newTrees.length; i++)
                newTrees[i].setName("[" + (i + 1) + "]");

            if (newTrees.length > 0 && newTrees[0].getNumberOfNodes() > 0) {
                Director newDir = Director.newProject(1, 1);
				newDir.getDocument().appendTrees(newTrees);
				newDir.getDocument().setTitle(FileUtils.replaceFileSuffix(getDir().getDocument().getTitle(), "-subtree-reduction"));
                MultiViewer newMultiViewer = (MultiViewer) newDir.getMainViewer();
                newMultiViewer.chooseGridSize();
                newMultiViewer.loadTrees(null);
                newMultiViewer.setMustRecomputeEmbedding(true);
                newMultiViewer.updateView(IDirector.ALL);
                newMultiViewer.getFrame().toFront();
                newDir.getDocument().setDocumentIsDirty(true);
            }
        }
    }

    public void actionPerformed(ActionEvent ev) {
        execute(getSyntax());
    }

    public String getSyntax() {
        return "experimental what=subtree-reduction;";
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Apply all possible subtree reductions to two refined multifurcating trees";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getName() {
        return "Subtree Reduction...";
    }

    public boolean isApplicable() {
		if (multiViewer.getTreeGrid().getNumberSelectedOrAllViewers() != 2)
			return false;
		Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator();
		// too expensive to check for equal label sets here...
		return it.next().getPhyloTree().getNumberReticulateEdges() == 0 && it.next().getPhyloTree().getNumberReticulateEdges() == 0
			   && ((MultiViewer) getViewer()).getDir().getDocument().getNumberOfTrees() > 0;
	}

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }
}
