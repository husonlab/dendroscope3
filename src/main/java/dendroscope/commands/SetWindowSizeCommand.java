/*
 * SetWindowSizeCommand.java Copyright (C) 2023 Daniel H. Huson
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
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.Alert;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * command
 * Daniel Huson, 6.2010
 */
public class SetWindowSizeCommand extends CommandBase implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Set Window Size...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set size (width x height) of windows";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Preferences16.gif");
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
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set window");
        java.util.List<String> tokens = np.getTokensLowerCase(null, ";");
        final JFrame frame = getViewer().getFrame();
        int width = np.findIgnoreCase(tokens, "width=", 0, Integer.MAX_VALUE, frame.getWidth());
        int height = np.findIgnoreCase(tokens, "height=", 0, Integer.MAX_VALUE, frame.getHeight());
        int x = np.findIgnoreCase(tokens, "x=", Integer.MIN_VALUE, Integer.MAX_VALUE, frame.getLocation().x);
        int y = np.findIgnoreCase(tokens, "y=", Integer.MIN_VALUE, Integer.MAX_VALUE, frame.getLocation().y);
        if (tokens.size() != 0)
            throw new IOException("Additional tokens: " + tokens);
        if (x != frame.getLocation().x || y != frame.getLocation().y)
            frame.setLocation(x, y);
        if (width != frame.getWidth() || height != frame.getHeight()) {
            frame.setSize(width, height);
            if (getViewer() instanceof MultiViewer) {
                MultiViewer multiViewer = (MultiViewer) getViewer();
                multiViewer.getTreeGrid().setGridSize(multiViewer.getTreeGrid().getRows(), multiViewer.getTreeGrid().getCols());
                multiViewer.getTreeGrid().revalidate();
            }
        }
    }

    /**
     * action to be performed
     *
	 */
    public void actionPerformed(ActionEvent ev) {
        String original = getViewer().getFrame().getWidth() + " x " + getViewer().getFrame().getHeight();
        String result = JOptionPane.showInputDialog(getViewer().getFrame(), "Set window size (width x height):", original);
        if (result != null && !result.equals(original)) {
            int height = 0;
            int width = 0;
            StringTokenizer st = new StringTokenizer(result, "x ");
            try {
                if (st.hasMoreTokens())
                    width = Integer.parseInt(st.nextToken());
                if (st.hasMoreTokens())
                    height = Integer.parseInt(st.nextToken());
                if (st.hasMoreTokens())
                    throw new NumberFormatException("Unexcepted characters at end of string");
                executeImmediately("set window width=" + width + " height=" + height + ";");
            } catch (NumberFormatException e) {
                new Alert("Input error: " + e.getMessage());
            }
        }
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return false;
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    public String getSyntax() {
        return "set window [width=<number>] [height=<number>] [x=<number>] [y=<number>];";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return true;
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
