/*
 * MUL2NetworkLevelKCommand.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.commands.mul;

import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * command Daniel Huson, 6.2010
 */
public class MUL2NetworkLevelKCommand extends MUL2NetworkCommand implements ICommand {
    public String getSyntax() {
        return null;
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "MUL to Network, Level-k-based...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Compute a network from a multi-labeled tree using a level-k-based algorithm";
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
     * action to be performed
     *
	 */
    public void actionPerformed(ActionEvent ev) {
        execute("compute mult2net method=levelk;");


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



