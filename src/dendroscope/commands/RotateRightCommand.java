/**
 * Copyright 2015, Daniel Huson
 *
 *(Some files contain contributions from other authors, who are then mentioned separately)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package dendroscope.commands;

import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.gui.commands.ICommand;
import jloda.util.ResourceManager;

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
