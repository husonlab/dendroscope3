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

import dendroscope.core.TreeData;
import dendroscope.io.IOFormat;
import dendroscope.io.IOManager;
import dendroscope.main.DendroscopeProperties;
import dendroscope.main.Version;
import dendroscope.util.NewickInputDialog;
import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.StringReader;

/**
 * command Daniel Huson, 6.2010
 */
public class AddTreeOrNetworkCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Enter Trees or Networks...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Enter a tree in Newick or network in extended Newick format";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Command16.gif");
    }

    /**
     * gets the accelerator key to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK);
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("add tree=");

        try {
            IOFormat format = IOManager.createIOFormatForName("newick");
            //System.err.println(stringArgument);
            TreeData[] trees = format.read(new StringReader(np.getWordFileNamePunctuation().replaceAll(";", ";\n")));
            np.matchIgnoreCase(";");

            if (trees.length > 0) {
                multiViewer.addTrees(trees);
            }

        } catch (Exception ex) {
            System.err.println("Add tree failed: " + ex);
            Basic.caught(ex);
        }
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        boolean edgeLengths = true;
        String string = ProgramProperties.get(DendroscopeProperties.LASTTREE, "").replaceAll(";", ";\n");
        NewickInputDialog newickInputDialog = new NewickInputDialog(getViewer().getFrame(), Version.NAME, string, edgeLengths);

        String tree = newickInputDialog.getString();
        if (tree != null) {
            ProgramProperties.put(DendroscopeProperties.LASTTREE, tree);
            execute("add tree='" + tree + "';");
        }
    }

    /**
     * is this a critical command that can only be executed when no other
     * command is running?
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
        return "add tree=<Newick>;";
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


