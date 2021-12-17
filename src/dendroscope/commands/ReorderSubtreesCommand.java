/*
 *   ReorderSubtreesCommand.java Copyright (C) 2020 Daniel H. Huson
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

import dendroscope.window.TreeViewer;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ReorderListDialog;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * command Daniel Huson, 6.2010
 */
public class ReorderSubtreesCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Reorder Subtrees...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Reorder of subtrees below node";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("RotateSubtree16.gif");
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
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        // todo: this is broken!

        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedNodesIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();

            int numToEdit = viewer.getSelectedNodes().size();

            for (Node v : viewer.getSelectedNodes()) {
                numToEdit--;
                if (v.getOutDegree() > 0 && !viewer.getCollapsedNodes().contains(v)) {
                    if (numToEdit > 5) {
                        int result = JOptionPane.showConfirmDialog(getViewer().getFrame(), "There are " + numToEdit +
                                " more selected nodes, reorder next?", "Question", JOptionPane.YES_NO_OPTION);
                        if (result == JOptionPane.NO_OPTION)
                            return;
                    }


                    class EdgeListElement {
                        EdgeListElement(Edge e, String s) {
                            this.edge = e;
                            this.label = s;
                        }

                        Edge edge;
                        String label;

                        public String toString() {
                            return label;
                        }
                    }
                    java.util.List<EdgeListElement> original = new LinkedList<>();

                    int count = 0;
                    for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                        count++;
                        String label;
                        if (viewer.getLabel(e.getTarget()) != null)
                            label = viewer.getLabel(e.getTarget()) + " [" + count + "]";
                        else
                            label = "Subtree [" + count + "]";
                        original.add(new EdgeListElement(e, label));
                    }

                    ReorderListDialog dialog = new ReorderListDialog("Reorder subtrees", false);
                    java.util.List newOrder = dialog.show(original);

                    if (newOrder == null)
                        continue;

                    System.err.println("original: " + original);
                    System.err.println("new: " + newOrder);

                    if (newOrder.size() == original.size()) {
                        java.util.List<Edge> edges = new LinkedList<>();
                        for (Object aNewOrder : newOrder) {
                            edges.add(((EdgeListElement) aNewOrder).edge);
                        }
                        v.rearrangeAdjacentEdges(edges);
						viewer.getPhyloTree().getLSAChildrenMap().put(v, null);
						viewer.recomputeEmbedding(false, true);
                        viewer.setDirty(true);
                    }
                }
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
        return null;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return multiViewer.getTreeGrid().getNumberSelectedOrAllViewers() == 1 && multiViewer.getTreeGrid().getSelectedNodesIterator().hasNext();
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
