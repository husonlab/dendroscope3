/**
 * RotateRightCommand.java 
 * Copyright (C) 2019 Daniel H. Huson
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
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * rotate left
 * Daniel right, 6.2010
 */
public class RotateRightCommand extends RotateLeftCommand implements ICommand {
    public String getSyntax() {
        return null;
    }

    /**
     * constructor
     */
    public RotateRightCommand() {
        setAutoRepeatInterval(250);
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    @Override
    public String getName() {
        return "Rotate Right";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    @Override
    public String getDescription() {
        return "Rotate right";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    @Override
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("RotateRight16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    @Override
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
        boolean lockXY = false;
        for (Iterator<TreeViewer> it = ((MultiViewer) getViewer()).getTreeGrid().getSelectedOrAllIterator(); !lockXY && it.hasNext(); ) {
            TreeViewer viewer = it.next();
            if (viewer.trans.getLockXYScale())
                lockXY = true;
        }

        if (lockXY)
            executeImmediately("rotate angle= " + (-0.02 * Math.PI) + ";");
        else if ((ev.getModifiers() & ActionEvent.SHIFT_MASK) == 0)
            executeImmediately("rotate angle= " + (-0.5 * Math.PI) + ";");
        else
            executeImmediately("rotate angle= " + (-0.25 * Math.PI) + ";");
    }

    /**
     * gets the command needed to undo this command
     *
     * @return undo command
     */
    @Override
    public String getUndo() {
        return null;
    }
}
