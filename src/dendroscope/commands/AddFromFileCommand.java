/**
 * AddFromFileCommand.java 
 * Copyright (C) 2019 Daniel H. Huson
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
import dendroscope.core.TreeData;
import dendroscope.io.IOFormat;
import dendroscope.io.IOManager;
import dendroscope.io.nexml.Nexml;
import dendroscope.main.DendroscopeProperties;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * add trees from a file to the existing document
 * Daniel Huson, 5.2010
 */
public class AddFromFileCommand extends CommandBaseMultiViewer implements ICommand {

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Add From File...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Add trees or networks from one or more files to the current document";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Open16.gif");
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
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        File lastOpenFile = ProgramProperties.getFile(ProgramProperties.OPENFILE);

        Collection<File> files = ChooseFileDialog.chooseFilesToOpen(getViewer().getFrame(), lastOpenFile, IOManager.getFileFilter(), IOManager.getFilenameFilter(), ev, "Open file(s) to add");
        if (files != null) {
            String command = "add file=";
            boolean first = true;
            for (File file : files) {
                if (file.exists() && file.canRead()) {
                    if (first)
                        first = false;
                    else
                        command += ",";
                    lastOpenFile = file;
                    ProgramProperties.put(ProgramProperties.OPENFILE, lastOpenFile.getAbsolutePath());
                    command += "'" + file.getPath() + "'";
                }
            }
            if (!first)
                execute(command + ";");
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
        return true;
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */

    public void apply(NexusStreamParser np) throws Exception {
        List<File> files = new LinkedList<>();
        np.matchIgnoreCase("add");
        boolean first = true;
        while (!np.peekMatchIgnoreCase(";")) {
            if (first) {
                np.matchIgnoreCase("file=");
                first = false;
            } else
                np.matchIgnoreCase(",");
            String fileName = np.getWordFileNamePunctuation();
            files.add(new File(fileName));
        }
        np.matchIgnoreCase(";");

        Document doc = getDir().getDocument();
        int current;
        if (multiViewer.getTreeGrid().getNumberOfSelectedViewers() > 0)
            current = multiViewer.getTreeGrid().getSelected().get(multiViewer.getTreeGrid().getSelected().size() - 1);
        else
            current = doc.getNumberOfTrees() - 1;
        doc.setCurrent(current);

        List<TreeData> allTrees = new LinkedList<>();

        int count = 0;
        for (File file : files) {
            if (++count >= files.size() - 3)
                DendroscopeProperties.addRecentFile(file); // just remember last three files added

            IOFormat format = IOManager.createIOFormatForFile(file);
            if (format instanceof Nexml) {
                ((Nexml) format).setConnectors(doc.getConnectors());
            }
            allTrees.addAll(Arrays.asList(format.read(file)));
        }
        TreeData[] trees = allTrees.toArray(new TreeData[allTrees.size()]);

        if (trees.length > 0) {
            multiViewer.addTrees(trees);
            multiViewer.getDir().getDocument().setDocumentIsDirty(true);
        }
    }

    /**
     * gets the command needed to undo this command
     *
     * @return undo command
     */
    public String getUndo() {
        return null;
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */

    public String getSyntax() {
        return "add file=<filename>[,<filename>...";
    }
}
