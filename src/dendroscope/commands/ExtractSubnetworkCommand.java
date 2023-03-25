/*
 * ExtractSubnetworkCommand.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.commands;

import dendroscope.core.Director;
import dendroscope.core.Document;
import dendroscope.embed.LayoutOptimizerManager;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.phylo.PhyloTree;
import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirector;
import jloda.swing.util.Alert;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * extract subpart of a network
 * Celine Scornavacca 7.2010
 */
public class ExtractSubnetworkCommand extends CommandBaseMultiViewer implements ICommand {

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Extract Subnetwork...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Extract subtree or subnetwork below node or edge";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("SelectSubtree16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    /**
     * parses the given command and executes it
     */
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        String[] names = new String[multiViewer.getTreeGrid().getNumberSelectedOrAllViewers()];
        List<PhyloTree> subNetworks = new LinkedList<>();


        int numbNetworksSelected = 0;

        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();

            int numberSelectedEgdesOrNodes = viewer.getSelectedNodes().size() + viewer.getSelectedEdges().size();
            if (numberSelectedEgdesOrNodes == 1) {
                if (viewer.getSelectedEdges().size() == 1)
                    viewer.getSelectedNodes().add(viewer.getSelectedEdges().getFirstElement().getTarget()); //if an edge is selected, select the target
                // node of the egde.
                PhyloTree newTree = dendroscope.util.PhyloTreeUtils.extractSubNetwork(viewer.getPhyloTree(),
                        viewer.getSelectedNodes(), viewer.getCollapsedNodes(), true);
                subNetworks.add(newTree);
                names[numbNetworksSelected] = viewer.getName();
                numbNetworksSelected++;
            } else {
                new Alert("In the selected network number " + (numbNetworksSelected + 1) + " there are more than \none edge/node selected. Subtnetwork not computed.");
            }

        }
        if (subNetworks.size() > 0) {
            int rows = Math.min(4, (int) (Math.sqrt(subNetworks.size())));
            int cols = subNetworks.size() / rows;


            Director newDir = Director.newProject(rows, cols);
            MultiViewer newMultiViewer = (MultiViewer) newDir.getViewerByClass(MultiViewer.class);
            newMultiViewer.setEmbedderName(LayoutOptimizerManager.UNOPTIMIZED); // don't change best common embedding
            Document newDoc = newDir.getDocument();
            BitSet which = new BitSet();
            for (int i = 0; i < subNetworks.size(); i++) {
                newDoc.appendTree(names[i], subNetworks.get(i), i);
                which.set(i);
            }

            newMultiViewer.loadTrees(which);

            newMultiViewer.setMustRecomputeEmbedding(true);
            newMultiViewer.updateView(IDirector.ALL);
            newMultiViewer.getFrame().toFront();


            newMultiViewer.getTreeGrid().repaint();
        }
    }

    /**
     * action to be performed
     *
	 */
    public void actionPerformed(ActionEvent ev) {
        executeImmediately(getSyntax());
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */

    public String getSyntax() {
        return "extract subnetwork;";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return ((MultiViewer) getViewer()).getDir().getDocument().getNumberOfTrees() > 0;
    }

    public String getUndo() {
        return null;
    }

}
