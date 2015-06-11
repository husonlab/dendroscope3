/**
 * LoadTaxonImageCommand.java 
 * Copyright (C) 2015 Daniel H. Huson
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

import dendroscope.main.DendroscopeProperties;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.gui.commands.ICommand;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Iterator;

/**
 * command Daniel Huson, 4.2011
 */
public class LoadTaxonImageCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Load Taxon Images...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Load taxon images from a directory and apply to trees";
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
     * gets the accelerator key to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("load imagedir=");
        String directoryName = np.getWordFileNamePunctuation();
        np.matchIgnoreCase(";");

        for (Iterator<TreeViewer> it = ((MultiViewer) getViewer()).getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            viewer.getNodeImageManager().loadImagesFromDirectory(new File(directoryName));
            viewer.getNodeImageManager().applyImagesToNodes();
            viewer.getNodeImageManager().setDefaultImageHeight(ProgramProperties.get(DendroscopeProperties.IMAGE_HEIGHT, 50));
        }
    }

    /**
     * action to be performed
     *
     * @param event
     */
    public void actionPerformed(ActionEvent event) {
        File lastOpenFile = ProgramProperties.getFile(DendroscopeProperties.IMAGE_DIRECTORY);

        File file = null;


        if (ProgramProperties.isMacOS() && (event != null && (event.getModifiers() & Event.SHIFT_MASK) == 0)) {
            //Use native file dialog on mac
            java.awt.FileDialog dialog = new java.awt.FileDialog(getViewer().getFrame(), "Open image directory", java.awt.FileDialog.LOAD);
            dialog.setFilenameFilter(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return true;
                }
            });
            if (lastOpenFile != null) {
                dialog.setDirectory(lastOpenFile.getPath());
                //dialog.setFile(fileName);
            }
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            dialog.setVisible(true);
            System.setProperty("apple.awt.fileDialogForDirectories", "false");

            if (dialog.getFile() != null) {
                file = new File(dialog.getDirectory(), dialog.getFile());
            } else
                return;
        } else {
            JFileChooser chooser = new JFileChooser(lastOpenFile);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (lastOpenFile != null)
                chooser.setSelectedFile(lastOpenFile);
            chooser.setAcceptAllFileFilterUsed(true);

            int result = chooser.showOpenDialog(getViewer().getFrame());
            if (result == JFileChooser.APPROVE_OPTION) {
                file = chooser.getSelectedFile();
            }
        }
        if (file != null) {
            if (!file.isDirectory())
                file = file.getParentFile();
        }


        if (file != null) {
            if (file.exists() && file.isDirectory()) {
                ProgramProperties.put(DendroscopeProperties.IMAGE_DIRECTORY, file.getAbsolutePath());
                execute("load imagedir='" + file.getPath() + "';");
            }
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
        return "load imagedir=<directory>;";
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
     * gets the command needed to undo this command
     *
     * @return undo command
     */
    public String getUndo() {
        return null;
    }
}
