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

import dendroscope.main.DendroscopeProperties;
import jloda.gui.commands.ICommand;
import jloda.util.Alert;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Set the grid size of the multi-window
 * Daniel Huson, 5.2010
 */
public class SetGridCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Set Grid...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set the tree grid dimensions";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Grid16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK);
    }

    /**
     * get command-line syntax. First two tokens are used to identify the command
     *
     * @return usage
     */
    public String getSyntax() {
        return "set grid=<rows x cols>;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {

        if ((ev.getModifiers() & ActionEvent.SHIFT_MASK) != 0)  // if shift is pressed, auto select
        {
            if (multiViewer.getTreeGrid().getNumberOfPanels() > 1)
                executeImmediately("set grid=1x1;");
            else
                executeImmediately("set grid=0x0;");
            return;
        }

        int rows = ProgramProperties.get(DendroscopeProperties.MULTIVIEWER_ROWS, 3);
        int cols = ProgramProperties.get(DendroscopeProperties.MULTIVIEWER_COLS, 3);

        String gridSize = JOptionPane.showInputDialog(getViewer().getFrame(), "Set dimensions of grid (rows x cols):", rows + "x" + cols);
        if (gridSize != null && gridSize.length() > 0) {
            String[] tokens = gridSize.split("x");
            if (tokens.length == 2 && Basic.isInteger(tokens[0].trim()) && Basic.isInteger(tokens[1].trim())) {
                rows = Integer.parseInt(tokens[0].trim());
                cols = Integer.parseInt(tokens[1].trim());
                if (rows > 0 && cols > 0) {
                    if (rows * cols > 64) {
                        int result = JOptionPane.showConfirmDialog(getViewer().getFrame(), "The number of panels is very large, proceed?");
                        if (result != JOptionPane.OK_OPTION)
                            return;
                    }
                    executeImmediately("set grid=" + rows + "x" + cols + ";");

                    ProgramProperties.put(DendroscopeProperties.MULTIVIEWER_ROWS, rows);
                    ProgramProperties.put(DendroscopeProperties.MULTIVIEWER_COLS, cols);

                }
            } else
                new Alert(multiViewer.getFrame(), "Failed to set grid, couldn't parse: " + gridSize);
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
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return multiViewer != null;
    }

    /**
     * parses the given command and executes it. Do not call this directly. This is called from the director in a separate thread
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set grid=");

        String str = "";
        while (!str.contains("x") || str.endsWith("x"))
            str += np.getWordRespectCase();
        String[] tokens = str.split("x");
        int rows = Integer.parseInt(tokens[0]);
        int cols = Integer.parseInt(tokens[1]);
        if (rows == 0 || cols == 0)
            multiViewer.chooseGridSize();
        else
            multiViewer.getTreeGrid().setGridSize(rows, cols);
        multiViewer.setMustRecomputeEmbedding(true);
        np.matchIgnoreCase(";");
    }

    /**
     * gets the command needed to undo this command. This is called just before the apply command.
     *
     * @return undo command
     */
    public String getUndo() {
        int rows = multiViewer.getTreeGrid().getRows();
        int cols = multiViewer.getTreeGrid().getCols();
        return "set grid=" + rows + " x " + cols + ";";
    }
}
