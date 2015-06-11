/**
 * ShowNodeLabelsCommand.java 
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
package dendroscope.commands;

import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.gui.commands.ICheckBoxCommand;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * command Daniel Huson, 6.2010
 */
public class ShowNodeLabelsCommand extends CommandBaseMultiViewer implements ICheckBoxCommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Show Node Labels";
    }

    /**
     * is selected?
     *
     * @return true, if selected
     */
    public boolean isSelected() {
        boolean result = false;
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedNodesIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            NodeSet selected = treeViewer.getSelectedNodes();
            try {
                for (Node v = selected.getFirstElement(); v != null; v = selected.getNextElement(v)) {
                    if (!treeViewer.getLabelVisible(v)) {
                        result = false;
                        break;
                    } else result = true;
                }
            } catch (Exception ex) {
                // ignore
            }
        }
        return result;
    }


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Show labels for selected nodes";
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
     * gets the accelerator key to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("show nodelabels=");
        boolean show = np.getBoolean();
        np.matchIgnoreCase(";");
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            NodeSet selected = viewer.getSelectedNodes();
            if (selected.size() == 0)
                viewer.showNodeLabels(null, show);
            else
                viewer.showNodeLabels(selected, show);
            viewer.repaint();
        }
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        execute("show nodelabels=" + (!isSelected()) + ";");
    }

    /**
     * is this a critical command that can only be executed when no other
     * command is running?
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
        return "show nodelabels={true|false};";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return ((MultiViewer) getViewer()).getTreeGrid().getSelectedNodesIterator().hasNext();
    }

    /**
     * gets the command needed to undo this command
     *
     * @return undo command
     */
    public String getUndo() {
        return null;
    }
}
