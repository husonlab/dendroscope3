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
import dendroscope.core.TreeData;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.gui.commands.ICommand;
import jloda.gui.director.IDirector;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * open a new document
 * Daniel Huson, 5.10
 */
public class NewCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "New...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Open a new document. Any selected trees are put in it";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/New16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */

    public void apply(NexusStreamParser np) throws Exception {
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        Director newDir = Director.newProject(1, 1);
        MultiViewer newMultiViewer = (MultiViewer) newDir.getMainViewer();

        if (multiViewer.getTreeGrid().getNumberOfPanels() > 1 && multiViewer.getTreeGrid().getNumberOfSelectedViewers() > 0) {
            java.util.List<TreeData> trees = new LinkedList<>();

            for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedIterator(); it.hasNext(); ) {
                TreeViewer viewer = it.next();
                int treeId = multiViewer.getTreeGrid().getNumberOfViewerInDocument(viewer);
                TreeData treeData = viewer.getDoc().getTree(treeId);
                treeData.syncViewer2Data(viewer, viewer.isDirty());
                treeData.setName(viewer.getName());
                TreeData newTree = (TreeData) treeData.clone();
                newTree.setDirty(false);
                trees.add(newTree);
            }
            newMultiViewer.getDir().getDocument().appendTrees(trees.toArray(new TreeData[trees.size()]));
            BitSet which = new BitSet();
            for (int i = 0; i < trees.size(); i++)
                which.set(i);
            newMultiViewer.loadTrees(which);
            newMultiViewer.chooseGridSize();
            newMultiViewer.setMustRecomputeEmbedding(true);
            newMultiViewer.setMustRecomputeCoordinates(true);
            newDir.execute("toFront;", newMultiViewer.getCommandManager());
        } else
            newMultiViewer.updateView(IDirector.ALL);
    }

    public static void makeNewDocument() {
        Director newDir = Director.newProject(1, 1);
        MultiViewer newMultiViewer = (MultiViewer) newDir.getMainViewer();
        newDir.execute("toFront;", newMultiViewer.getCommandManager());
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */

    public String getSyntax() {
        return null; // no command line version
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
