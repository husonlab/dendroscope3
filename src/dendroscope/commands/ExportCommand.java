/*
 * ExportCommand.java Copyright (C) 2022 Daniel H. Huson
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

import dendroscope.io.IOFormat;
import dendroscope.io.IOManager;
import dendroscope.io.nexml.Nexml;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.util.FileUtils;
import jloda.util.ProgramProperties;
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
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    /**
     * gets the accelerator key to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK);
    }

    /**
     * parses the given command and executes it
     *
	 */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
    }

    /**
     * action to be performed
     *
	 */
    public void actionPerformed(ActionEvent event) {
        String lastSaveFormat = ProgramProperties.get(ProgramProperties.SAVEFORMAT, Nexml.NAME);

        String format = (String) JOptionPane.showInputDialog(getViewer().getFrame(), "Output format:", "Choose output format",
                JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(), IOManager.getAvailableFormats(), lastSaveFormat);

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
			lastOpenFile = new File(lastSaveDir, FileUtils.replaceFileSuffix(name, formatter.getExtension()));

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

