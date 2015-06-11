/**
 * UnlockEdgeLengthCommand.java 
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

import dendroscope.window.TreeViewer;
import jloda.gui.commands.ICheckBoxCommand;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * unlock reshape edges command Daniel Huson, 6.2010
 */
public class UnlockEdgeLengthCommand extends CommandBaseMultiViewer implements ICheckBoxCommand {
    public boolean isSelected() {
        boolean hasOne = false;
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            if (viewer.isUnlockEdgeLengths())
                hasOne = true;
            else
                return false;
        }
        return hasOne;
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Unlock Edge Lengths";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Allow user to reshape tree by dragging nodes or internal edge points";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Empty16.gif");
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
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set unlockedgelengths=");
        boolean unlockedgelengths = np.getBoolean();
        np.matchIgnoreCase(";");

        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            viewer.setUnlockEdgeLengths(unlockedgelengths);
        }
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        execute("set unlockedgelengths=" + !isSelected() + ";");

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
    @Override
    public String getSyntax() {
        return "set unlockedgelengths={true|false};";
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
     * gets the command needed to undo this command
     *
     * @return undo command
     */
    public String getUndo() {
        return null;
    }
}

