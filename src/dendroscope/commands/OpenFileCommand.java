/**
 * OpenFileCommand.java 
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

import dendroscope.core.Director;
import dendroscope.core.Document;
import dendroscope.core.TreeData;
import dendroscope.io.IOFormat;
import dendroscope.io.IOManager;
import dendroscope.io.nexml.Nexml;
import dendroscope.main.DendroscopeProperties;
import dendroscope.util.SupportValueUtils;
import jloda.swing.commands.ICommand;
import jloda.swing.director.ProjectManager;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * open file
 * Daniel Huson, 5.2010
 */
public class OpenFileCommand extends CommandBaseMultiViewer implements ICommand {

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Open...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Opens a file";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Open16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());

    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        File lastOpenFile = ProgramProperties.getFile(ProgramProperties.OPENFILE);

        getDir().notifyLockInput();
        Collection<File> files = ChooseFileDialog.chooseFilesToOpen(getViewer().getFrame(), lastOpenFile, IOManager.getFileFilter(), IOManager.getFilenameFilter(), ev, "Open document");
        getDir().notifyUnlockInput();

        if (files != null && files.size() > 0) {
            StringBuilder buf = new StringBuilder();
            for (File file : files) {
                if (file != null && file.exists() && file.canRead()) {
                    ProgramProperties.put(ProgramProperties.OPENFILE, file.getAbsolutePath());
                    buf.append("open file='").append(file.getPath()).append("';");
                }
            }
            execute(buf.toString());
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
        np.matchIgnoreCase("open file=");
        String fileName = np.getWordFileNamePunctuation();
        np.matchIgnoreCase(";");

        if (!ProgramProperties.isUseGUI() || multiViewer.getTreeGrid().getCurrentTrees().cardinality() == 0) {
            multiViewer.getTreeGrid().setShowScrollBars(false);
            if (!ProgramProperties.isUseGUI()) {
                multiViewer.getTreeGrid().setSize(800, 600);
                executeImmediately("set window width=800 height=600;");
                executeImmediately("zoom what=fit;");
            }

            final File file = new File(fileName);
            Document doc = getDir().getDocument();
            doc.setFile(file);
            final IOFormat format = IOManager.createIOFormatForFile(file);
            if (format == null)
                throw new IOException("Unknown format in file: " + fileName);
            if (format instanceof Nexml) {
                ((Nexml) format).setConnectors(doc.getConnectors());
            }
            doc.setTrees(format.read(file));
            if (ProgramProperties.isUseGUI() && doc.getTrees().length > 0) {
                String internalLabel = SupportValueUtils.getInternalNodeLabelIfPresent(doc.getTree(0));
                if (internalLabel != null) {
                    final String[] choices = new String[]{"Interpret as node labels", "Interpret as edge labels (such as bootstrap values)", "Delete"};
                    final String choice = (String) JOptionPane.showInputDialog(getViewer().getFrame(), "Internal nodes have labels (example: '" + internalLabel + "'),\nhow should they be interpreted?",
                            "How to interpret internal node numbers", JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(), choices, choices[0]);
                    if (choice == null) {
                        System.err.println("USER CANCELED");
                        if (ProjectManager.getNumberOfProjects() > 1) {
                            ProjectManager.removeProject(multiViewer.getDir());
                            multiViewer.getDir().close();
                        } else {
                            doc.setFile("Untitled", true);
                            doc.setTrees(new TreeData[0]);
                        }
                        return;
                    }
                    if (choice.equals(choices[1])) { // interpret as support values
                        doc.setInternalNodeLabelsAreEdgeLabels(true);
                    } else
                        doc.setInternalNodeLabelsAreEdgeLabels(false);

                    if (choice.equals(choices[2])) { // delete
                        SupportValueUtils.deleteAllInternalNodes(doc.getTrees());
                        doc.setDocumentIsDirty(true);
                    }
                }
            }
            System.err.println("Trees loaded: " + doc.getTrees().length);
            multiViewer.chooseGridSize();
            multiViewer.loadTrees(null);
            multiViewer.setMustRecomputeEmbedding(true);
            multiViewer.setMustRecomputeCoordinates(false);
            DendroscopeProperties.addRecentFile(file);
        } else {
            String openCommand = "open file='" + fileName + "';";
            Director theDir = Director.newProject(1, 1);
            theDir.executeImmediately(openCommand, theDir.getMainViewer().getCommandManager());
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
        return "open file=<filename>;";
    }
}
