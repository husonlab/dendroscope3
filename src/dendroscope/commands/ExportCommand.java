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

import dendroscope.io.IOFormat;
import dendroscope.io.IOManager;
import dendroscope.io.nexml.Nexml;
import dendroscope.main.Dendroscope;
import jloda.gui.ChooseFileDialog;
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
import java.io.File;

/**
 * command Daniel Huson, 6.2010
 */
public class ExportCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Export...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Export trees or networks to other file formats";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Export16.gif");
    }

    /**
     * gets the accelerator key to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK);
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
    }

    /**
     * action to be performed
     *
     * @param event
     */
    public void actionPerformed(ActionEvent event) {
        if (!Dendroscope.getApplication().go(null, false, false)) {
            new Alert(getViewer().getFrame(), "Unavailable: please register program to unlock");
            return;
        }

        String lastSaveFormat = ProgramProperties.get(ProgramProperties.SAVEFORMAT, Nexml.NAME);

        String format = (String) JOptionPane.showInputDialog(getViewer().getFrame(), "Output format:", "Choose output format",
                JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(),
                IOManager.getAvailableFormats(), lastSaveFormat);

        if (format == null) {
            return; // must have canceled
        }

        IOFormat formatter = IOManager.createIOFormatForName(format);
        if (!format.equalsIgnoreCase("newick-no-weights"))
            format = formatter.getName();

        String lastSaveDir = ProgramProperties.get(ProgramProperties.SAVEFILE, "");
        if (lastSaveDir.length() != 0)
            lastSaveDir = (new File(lastSaveDir)).getParent();
        else if (multiViewer.getDir().getDocument().getFile() != null) {
            lastSaveDir = multiViewer.getDir().getDocument().getFile().getParent();
        }

        String name;
        if (multiViewer.getDir().getDocument().getFile() != null) {
            name = multiViewer.getDir().getDocument().getFile().getName();
        } else
            name = "Untitled";

        File lastOpenFile = null;
        if (lastSaveDir != null)
            lastOpenFile = new File(lastSaveDir, Basic.replaceFileSuffix(name, formatter.getExtension()));

        File file = ChooseFileDialog.chooseFileToSave(multiViewer.getFrame(), lastOpenFile, IOManager.getFileFilter(), IOManager.getFilenameFilter(), event, "Export trees or networks", formatter.getExtension());

        if (file != null) {
            ProgramProperties.put(ProgramProperties.SAVEFILE, file);
            ProgramProperties.put(ProgramProperties.SAVEFORMAT, formatter.getName());
            execute("save format=" + format + " file='" + file.getPath() + "';");
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
    public String getSyntax() {
        return null;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return multiViewer.getDir().getDocument().getNumberOfTrees() > 0;
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

