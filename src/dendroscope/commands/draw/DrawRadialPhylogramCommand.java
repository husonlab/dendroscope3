/*
 * DrawRadialPhylogramCommand.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.commands.draw;

import dendroscope.window.TreeViewer;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.util.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Iterator;

public class DrawRadialPhylogramCommand extends DrawCommand implements ICheckBoxCommand {
    /**
     * get command-line syntax
     *
     * @return usage
     */
    public String getSyntax() {
        return "set drawer=" + TreeViewer.RADIAL_PHYLOGRAM + ";";
    }

    /**
     * this is currently selected?
     *
     * @return selected
     */
    public boolean isSelected() {
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            if (viewer.getDrawerKind().equals(TreeViewer.RADIAL_PHYLOGRAM))
                return true;
        }
        return false;
    }


    /**
     * get the name to be used as a menu label
     *
     * @return name
     */

    public String getName() {
        return "Draw Radial Phylogram";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */

    public String getDescription() {
        return "Draw tree or network as radial phylogram";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("RadialPhylogram16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_7, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    /**
     * action to be performed
     *
	 */

    public void actionPerformed(ActionEvent ev) {
        execute("set drawer=" + TreeViewer.RADIAL_PHYLOGRAM + ";");
    }
}
