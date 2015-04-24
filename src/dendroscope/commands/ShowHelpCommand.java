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

import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.ResourceManager;
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
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/About16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
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
            System.out.println("Available commands (context=" + Basic.getShortName(getViewer().getClass()).toLowerCase() + ", keyword(s)=" + Basic.toString(keywords, ",") + "):");
            System.out.println(getCommandManager().getUsage(keywords));
        }
    }

    /**
     * action to be performed
     *
     * @param ev
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
