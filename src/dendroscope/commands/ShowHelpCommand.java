/*
 * ShowHelpCommand.java Copyright (C) 2022 Daniel H. Huson
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
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.StringUtils;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * close the window
 * Daniel Huson, 6.2010
 */
public class ShowHelpCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Command-line Syntax";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Shows command-line syntax";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/About16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    public String getSyntax() {
        return "help [keyword(s)];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("help");
        Set<String> keywords = new HashSet<>();
        while (!np.peekMatchIgnoreCase(";")) {
            keywords.add(np.getWordRespectCase().toLowerCase());
        }
        np.matchIgnoreCase(";");
        if (keywords.size() == 0) {
            System.out.println("Available commands (context=" + Basic.getShortName(getViewer().getClass()).toLowerCase() + "):");
            System.out.println(getCommandManager().getUsage(getViewer().getFrame().getJMenuBar()));
        } else {
			System.out.println("Available commands (context=" + Basic.getShortName(getViewer().getClass()).toLowerCase() + ", keyword(s)=" + StringUtils.toString(keywords, ",") + "):");
			System.out.println(getCommandManager().getUsage(keywords));
        }
    }

    /**
     * action to be performed
     *
	 */
    public void actionPerformed(ActionEvent ev) {
        executeImmediately("show messagewindow;");
        execute(getSyntax());
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
