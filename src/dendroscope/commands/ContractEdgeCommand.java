/*
 *   ContractEdgeCommand.java Copyright (C) 2020 Daniel H. Huson
 *
 *   (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dendroscope.commands;

import dendroscope.core.Document;
import dendroscope.embed.LayoutOptimizerManager;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.graph.Edge;
import jloda.swing.commands.ICommand;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * command Daniel Huson, 11.2011
 */
public class ContractEdgeCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Contract Edges";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Contract all selected edges";
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
        return KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, Toolkit
                .getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK);
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        final Document doc = getDir().getDocument();
        for (Iterator<TreeViewer> it = ((MultiViewer) getViewer()).getTreeGrid().getSelectedEdgesIterator(); it.hasNext(); ) {
            final TreeViewer treeViewer = it.next();

            final Set<Edge> edges = new HashSet<>();
            edges.addAll(treeViewer.getSelectedEdges());

            if (edges.size() > 0 && edges.size() < treeViewer.getGraph().getNumberOfEdges()) {
                //   doc.getTree(multiViewer.getTreeGrid().getNumberOfViewerInDocument(treeViewer)).syncViewer2Data(treeViewer, treeViewer.isDirty());
                if (treeViewer.contractAll(edges)) {
                    treeViewer.getPhyloTree().getLSAChildrenMap().clear();
					LayoutOptimizerManager.apply(multiViewer.getEmbedderName(), treeViewer.getPhyloTree());
                    treeViewer.setDirty(true);
                    treeViewer.recomputeEmbedding(true, true);
                    treeViewer.resetLabelPositions(true);
                    treeViewer.getScrollPane().invalidate();
                    treeViewer.getScrollPane().repaint();
                    doc.getTree(multiViewer.getTreeGrid().getNumberOfViewerInDocument(treeViewer)).syncViewer2Data(treeViewer, treeViewer.isDirty());
                }
            }
        }
    }


    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        executeImmediately(getSyntax());
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
        return "remove edges=selected;";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return multiViewer.getTreeGrid().getSelectedEdgesIterator().hasNext();
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
