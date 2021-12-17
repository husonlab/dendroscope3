/*
 *   LayoutNoOptimizerCommand.java Copyright (C) 2020 Daniel H. Huson
 *
 *   (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dendroscope.commands;

import dendroscope.embed.LayoutOptimizerManager;
import dendroscope.window.TreeViewer;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.util.StringUtils;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * choose embedding algorithm
 * Daniel Huson, 7.2010
 */
public class LayoutNoOptimizerCommand extends CommandBaseMultiViewer implements ICheckBoxCommand {
    public boolean isSelected() {
        return multiViewer.getEmbedderName().equalsIgnoreCase(LayoutOptimizerManager.UNOPTIMIZED);
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set layouter=");
        String embedder = np.getWordMatchesIgnoringCase(LayoutOptimizerManager.getEmbedderNames());
        np.matchIgnoreCase(";");
        multiViewer.setEmbedderName(embedder);
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setDirty(true);
			treeViewer.getPhyloTree().getLSAChildrenMap().clear(); // this will force recomputation of embedding
			LayoutOptimizerManager.apply(embedder, treeViewer.getPhyloTree());
        }
        multiViewer.getCommandManager().updateEnableState();
        multiViewer.setMustRecomputeEmbedding(true);
        multiViewer.setMustRecomputeCoordinates(true);
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
		return "set layouter={" + StringUtils.toString(LayoutOptimizerManager.getEmbedderNames(), "|") + "};";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return getDir().getDocument().getNumberOfTrees() > 0;
    }


    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "No Optimization";
    }

    public String getAltName() {
        return "Layout Optimizer None";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Choose layout optimizer for networks";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return null;
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
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        execute("set layouter=" + LayoutOptimizerManager.UNOPTIMIZED + ";");
    }
}
