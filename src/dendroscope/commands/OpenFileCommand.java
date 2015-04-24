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

import dendroscope.core.Director;
import dendroscope.core.Document;
import dendroscope.io.IOFormat;
import dendroscope.io.IOManager;
import dendroscope.io.nexml.Nexml;
import dendroscope.main.DendroscopeProperties;
import jloda.gui.ChooseFileDialog;
import jloda.gui.commands.ICommand;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
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
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Open16.gif");
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
                    jloda.util.ProgramProperties.put(ProgramProperties.OPENFILE, file.getAbsolutePath());
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
        String openCommand = "open file='" + fileName + "';";

        if (!ProgramProperties.isUseGUI() || multiViewer.getTreeGrid().getCurrentTrees().cardinality() == 0) {
            multiViewer.getTreeGrid().setShowScrollBars(false);
            if (!ProgramProperties.isUseGUI()) {
                multiViewer.getTreeGrid().setSize(800, 600);
                executeImmediately("set window width=800 height=600;");
                executeImmediately("zoom what=fit;");
            }

            File file = new File(fileName);
            Document doc = getDir().getDocument();
            doc.setFile(file);
            IOFormat format = IOManager.createIOFormatForFile(file);
            if (format instanceof Nexml) {
                ((Nexml) format).setConnectors(doc.getConnectors());
            }
            doc.setTrees(format.read(file));
            System.err.println("Trees loaded: " + doc.getTrees().length);
            multiViewer.chooseGridSize();
            multiViewer.loadTrees(null);
            multiViewer.setMustRecomputeEmbedding(true);
            multiViewer.setMustRecomputeCoordinates(false);
            DendroscopeProperties.addRecentFile(file);
        } else {
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
