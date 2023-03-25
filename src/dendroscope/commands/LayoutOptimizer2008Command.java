/*
 *   LayoutOptimizer2008Command.java Copyright (C) 2023 Daniel H. Huson
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
import jloda.swing.commands.ICheckBoxCommand;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * choose embedding algorithm
 * Daniel Huson, 7.2010
 */
public class LayoutOptimizer2008Command extends LayoutNoOptimizerCommand implements ICheckBoxCommand {
    public String getSyntax() {
        return null;
    }

    public boolean isSelected() {
        return multiViewer.getEmbedderName().equalsIgnoreCase(LayoutOptimizerManager.ALGORITHM2008);
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "2008 Algorithm";
    }

    public String getAltName() {
        return "Layout Optimizer 2008";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Optimize embedding of networks using the algorithm\n" +
                "described in (Kloepper and Huson 2008)";
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
	 */
    @Override
    public void actionPerformed(ActionEvent ev) {
        execute("set layouter=" + LayoutOptimizerManager.ALGORITHM2008 + ";");
    }
}
