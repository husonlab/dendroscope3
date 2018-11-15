/**
 * SaveAsCommand.java 
 * Copyright (C) 2018 Daniel H. Huson
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

import dendroscope.core.Document;
import dendroscope.io.IOManager;
import dendroscope.io.nexml.Nexml;
import jloda.gui.ChooseFileDialog;
import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

/**
 * The save as command
 * Daniel Huson, 7.2010
 */
public class SaveAsCommand extends SaveCommand implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Save As...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Save the current document under a new name";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/SaveAs16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return getDir().getDocument().getNumberOfTrees() > 0;
    }

    /**
     * action to be performed
     *
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        final boolean inAskToSave = (event.getActionCommand() != null && event.getActionCommand().equals("askToSave"));
        Document doc = getDir().getDocument();
        Nexml formatter = new Nexml();
        formatter.setConnectors(doc.getConnectors());

        File lastOpenFile = doc.getFile();

        if (lastOpenFile == null) {
            lastOpenFile = new File(ProgramProperties.get(ProgramProperties.SAVEFILE, ""), doc.getTitle());
        }
        lastOpenFile = new File(lastOpenFile.getParent(), Basic.replaceFileSuffix(lastOpenFile.getName(), formatter.getExtension()));

        File file = ChooseFileDialog.chooseFileToSave(multiViewer.getFrame(), lastOpenFile, IOManager.getFileFilter(), IOManager.getFilenameFilter(), event, "Save document", formatter.getExtension());

        if (file != null) {
            ProgramProperties.put(ProgramProperties.SAVEFILE, file.getPath());
            execute("save format=" + formatter.getName() + " file='" + file.getPath() + "';");
        }
    }

    void replyUserHasCanceledInAskToSave(ActionEvent event) {
        ((Boolean[]) event.getSource())[0] = Boolean.TRUE;
    }
}
