/*
 * SetPropertyCommand.java Copyright (C) 2022 Daniel H. Huson
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

import jloda.swing.commands.ICommand;
import jloda.util.NumberUtils;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * the set property command
 * Daniel Huson, 8.2010
 */
public class SetPropertyCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Set Property...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set a property";
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
    @Override
    public void apply(NexusStreamParser np) throws IOException {
        np.matchIgnoreCase("set prop");
        String label = np.getWordRespectCase();
        np.matchIgnoreCase("=");
        String value = np.getWordRespectCase();
        if (NumberUtils.isBoolean(value)) {
            ProgramProperties.put(label, Boolean.parseBoolean(value));
        } else if (NumberUtils.isInteger(value)) {
            ProgramProperties.put(label, Integer.parseInt(value));
        } else if (NumberUtils.isFloat(value)) {
            ProgramProperties.put(label, Float.parseFloat(value));
        } else
            ProgramProperties.put(label, value);
        np.matchIgnoreCase(";");
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
        return "set prop <name>=<value>;";
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
     * action to be performed
     *
	 */
    @Override
    public void actionPerformed(ActionEvent ev) {
        String line = JOptionPane.showInputDialog("Enter property", "name=value");
        if (line != null)
            execute("set prop " + line + ";");

    }
}
