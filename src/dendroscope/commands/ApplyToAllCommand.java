/*
 * ApplyToAllCommand.java Copyright (C) 2022 Daniel H. Huson
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
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeGrid;
import dendroscope.window.TreeViewer;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * command Daniel Huson, 6.2010
 */
public class ApplyToAllCommand extends CommandBase implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return null;
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Apply a command to all trees in the file";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return null;
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
	 */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("apply-all-begin");
        String commands = np.getTokensStringRespectCase("apply-all-end");
        np.matchIgnoreCase(";");

        MultiViewer multiViewer = (MultiViewer) getViewer();
        TreeGrid treeGrid = multiViewer.getTreeGrid();
        Director dir = multiViewer.getDir();
        Document doc = dir.getDocument();

        executeImmediately("select panels=all;");
        executeImmediately("deselect all;");

        int originalPosition = treeGrid.getNumberOfViewerInDocument(treeGrid.getIterator().next());
        // move to first tree in file:
        if (originalPosition != 1) {
            executeImmediately("go tree=first;");
            dir.notifyUpdateViewer(Director.ALL);
        }

        int delta = treeGrid.getNumberOfPanels();
        int pos = 1;
        while (pos <= doc.getNumberOfTrees()) {
            if (pos > 1) {
                executeImmediately("go tree=" + pos + ";");
                dir.notifyUpdateViewer(Director.ALL);
            }
            int top = Math.min(doc.getNumberOfTrees(), pos + delta - 1);
            System.err.println("Applying to trees " + pos + " to " + top);
            for (Iterator<TreeViewer> it = treeGrid.getIterator(); it.hasNext(); ) {
                TreeViewer treeViewer = it.next();
                if (treeViewer.getPhyloTree().getNumberOfNodes() > 0) {
                    TreeData treeData = doc.getTree(treeGrid.getNumberOfViewerInDocument(treeViewer));
                    if (!treeViewer.isDirty() && !treeData.hasAdditional()) {
                        treeViewer.setDrawerKind(TreeViewer.RECTANGULAR_CLADOGRAM);
                        treeViewer.recomputeEmbedding(true, true);
                        treeViewer.getScrollPane().invalidate();
                    }
                }
            }
            dir.notifyUpdateViewer(Director.ALL);
            executeImmediately(commands);
            executeImmediately("deselect all;");
            pos += delta;
        }

        if (multiViewer.getTreeGrid().getCurrentTrees().nextSetBit(1) != originalPosition) {
            multiViewer.goToTree(originalPosition);
        }
        multiViewer.setMustRecomputeEmbedding(true);
    }

    /**
     * action to be performed
     *
	 */
    public void actionPerformed(ActionEvent ev) {
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
        return "apply-all-begin <command>;[<command>;...] apply-all-end;";
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
