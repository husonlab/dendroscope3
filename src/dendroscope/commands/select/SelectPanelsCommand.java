/*
 * SelectPanelsCommand.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.commands.select;

import dendroscope.commands.CommandBaseMultiViewer;
import jloda.swing.commands.ICommand;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * select all panels
 * Daniel Huson, 5.2010
 */
public class SelectPanelsCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Select Panels";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Select panels";
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
     * parses the given command and executes it
     *
	 */

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("select panels");
        String what;
        if (np.peekMatchIgnoreCase("=")) {
            np.matchIgnoreCase("=");
            what = np.getWordMatchesIgnoringCase("all none invert");
        } else
            what = "all";
        np.matchIgnoreCase(";");

        switch (what) {
            case "all":
                multiViewer.getTreeGrid().selectAllPanels(true);
                // select all
                break;
            case "none":
                multiViewer.getTreeGrid().selectAllPanels(false);

                break;
            default:
// must be invert

                multiViewer.getTreeGrid().selectPanelsInvert();
                break;
        }
    }

    /**
     * action to be performed
     *
	 */
    public void actionPerformed(ActionEvent ev) {
        String choice = (String) JOptionPane.showInputDialog(multiViewer.getFrame(), "Select", "Selection", JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(), new String[]{"All", "None", "Invert"}, "All");
        if (choice != null) {
            execute("select panels=" + choice + ";");
        }
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */

    public String getSyntax() {
        return "select panels={all|none|invert};";
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
