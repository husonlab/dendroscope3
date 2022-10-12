/*
 * NewCommand.java Copyright (C) 2022 Daniel H. Huson
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
import dendroscope.core.TreeData;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirector;
import jloda.swing.util.ResourceManager;
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
        return ResourceManager.getIcon("sun/New16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    /**
     * parses the given command and executes it
     */

    public void apply(NexusStreamParser np) throws Exception {
    }

    /**
     * action to be performed
     *
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
			newMultiViewer.getDir().getDocument().appendTrees(trees.toArray(new TreeData[0]));
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
