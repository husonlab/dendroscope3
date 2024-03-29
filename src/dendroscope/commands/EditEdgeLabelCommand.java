/*
 * EditEdgeLabelCommand.java Copyright (C) 2023 Daniel H. Huson
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

import dendroscope.window.TreeViewer;
import jloda.graph.Edge;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * edit all selected edge labels
 * Daniel Huson, 8.2010
 */
public class EditEdgeLabelCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Edit Edge Label...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Edit the edge label";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Command16.gif");
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
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("edit edgelabels;");

        boolean ok = true;
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedEdgesIterator(); ok && it.hasNext(); ) {
            TreeViewer viewer = it.next();
            int numToEdit = viewer.getSelectedEdges().size();
            boolean changed = false;
            for (Edge e : viewer.getSelectedEdges()) {
                if (numToEdit > 5) {
                    int result = JOptionPane.showConfirmDialog(getViewer().getFrame(), "There are " + numToEdit +
                            " more selected labels, edit next?", "Question", JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.NO_OPTION)
                        ok = false;
                }
                numToEdit--;
                String label = viewer.getLabel(e);
                label = JOptionPane.showInputDialog(getViewer().getFrame(), "Edit Edge Label:", label);
                if (label != null && !label.equals(viewer.getLabel(e))) {
                    /*
                    label = label.trim();
                    label = label.replaceAll("[ \\(\\),:]", "_");

                    if (label.length() > 0) {
                    */
                    if (label.length() == 0)
                        label = null;
                    viewer.getPhyloTree().setLabel(e, label);
                    viewer.setLabel(e, label);
                    /*
                    } else {
                        viewer.getPhyloTree().setLabel(v, null);
                        viewer.setLabel(v, null);
                    }
                    */
                    changed = true;
                }
                if (changed) {
                    viewer.setDirty(true);
                    viewer.repaint();
                }
                if (!ok)
                    break;
            }
        }
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
    @Override
    public String getSyntax() {
        return "edit edgelabels;";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return multiViewer.getTreeGrid().getSelectedEdgesIterator().hasNext();
    }

    /**
     * action to be performed
     *
	 */
    @Override
    public void actionPerformed(ActionEvent ev) {
        executeImmediately("edit edgelabels;");
    }
}
