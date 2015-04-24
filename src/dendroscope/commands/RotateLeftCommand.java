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
import jloda.graphview.ScrollPaneAdjuster;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Iterator;

/**
 * rotate left
 * Daniel Huson, 6.2010
 */
public class RotateLeftCommand extends CommandBase implements ICommand {
    /**
     * constructor
     */
    public RotateLeftCommand() {
        setAutoRepeatInterval(250);
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Rotate Left";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Rotate left";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("RotateLeft16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK);
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("rotate angle=");
        double value = np.getDouble();
        np.matchIgnoreCase(";");

        for (Iterator<TreeViewer> it = ((MultiViewer) getViewer()).getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            ScrollPaneAdjuster spa = new ScrollPaneAdjuster(treeViewer.getScrollPane(), treeViewer.trans);
            treeViewer.trans.composeAngle(treeViewer.trans.getFlipH() == treeViewer.trans.getFlipV() ? -value : value);
            if (treeViewer.trans.getLockXYScale() == false) {
                treeViewer.trans.setCoordinateRect(treeViewer.getGraphDrawer().getBBox());
                treeViewer.fitGraphToWindow();
                treeViewer.setDirty(true);
                spa.adjust(true, true);
                treeViewer.resetLabelPositions(true);
            }
        }
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        boolean lockXY = false;
        for (Iterator<TreeViewer> it = ((MultiViewer) getViewer()).getTreeGrid().getSelectedOrAllIterator(); !lockXY && it.hasNext(); ) {
            TreeViewer viewer = it.next();
            if (viewer.trans.getLockXYScale())
                lockXY = true;
        }

        if (lockXY)
            executeImmediately("rotate angle= " + (0.02 * Math.PI) + ";");
        else if ((ev.getModifiers() & ActionEvent.SHIFT_MASK) == 0)
            executeImmediately("rotate angle= " + (0.5 * Math.PI) + ";");
        else
            executeImmediately("rotate angle= " + (0.25 * Math.PI) + ";");
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
        return "rotate angle=<angle>;";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return ((MultiViewer) getViewer()).getDir().getDocument().getNumberOfTrees() > 0;
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
