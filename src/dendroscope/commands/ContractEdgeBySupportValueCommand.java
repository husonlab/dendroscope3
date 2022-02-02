/*
 * ContractEdgeBySupportValueCommand.java Copyright (C) 2022 Daniel H. Huson
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
import dendroscope.embed.LayoutOptimizerManager;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.Message;
import jloda.util.NumberUtils;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * contract edges by support value Daniel Huson, 11.2011
 */
public class ContractEdgeBySupportValueCommand extends CommandBase implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Contract Low Support Edges";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Contract all edges whose support is low";
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
        np.matchIgnoreCase("contractEdges minSupport=");
        double minSupport = np.getDouble();
        np.matchIgnoreCase(";");

        final Document doc = ((Director) getDir()).getDocument();
        final MultiViewer multiViewer = (MultiViewer) getViewer();

        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            final TreeViewer treeViewer = it.next();

            final Set<Edge> toDelete = new HashSet<>();

            for (Edge e = treeViewer.getGraph().getFirstEdge(); e != null; e = e.getNext()) {
                final Node w = e.getTarget();
                if (w.getDegree() > 1 && NumberUtils.isDouble(treeViewer.getLabel(w))) {
                    final double value = NumberUtils.parseDouble(treeViewer.getLabel(w));
                    {
                        if (value < minSupport)
                            toDelete.add(e);
                    }
                }
            }
            final int count = toDelete.size();
            if (count > 0 && treeViewer.contractAll(toDelete)) {
                treeViewer.getPhyloTree().getLSAChildrenMap().clear();
				LayoutOptimizerManager.apply(multiViewer.getEmbedderName(), treeViewer.getPhyloTree());
                treeViewer.setDirty(true);
                treeViewer.recomputeEmbedding(true, true);
                treeViewer.resetLabelPositions(true);
                treeViewer.getScrollPane().invalidate();
                treeViewer.getScrollPane().repaint();
                doc.getTree(multiViewer.getTreeGrid().getNumberOfViewerInDocument(treeViewer)).syncViewer2Data(treeViewer, treeViewer.isDirty());

                if (count > 0) {
					Message.show(multiViewer.getFrame(), "Number of edges contracted: " + count);
                }
            }
        }
    }


    /**
     * action to be performed
     *
	 */
    public void actionPerformed(ActionEvent ev) {
        final MultiViewer viewer = (MultiViewer) getViewer();
        double minSupport = ProgramProperties.get("CollapseEdgeThreshold", 75.0);

        final String result = JOptionPane.showInputDialog(viewer.getFrame(), "Enter minimum support threshold:", String.format("%.2f", minSupport));
        if (result != null && NumberUtils.isDouble(result)) {
            minSupport = NumberUtils.parseDouble(result);
            ProgramProperties.put("CollapseEdgeThreshold", minSupport);
            execute(String.format("contractEdges minSupport=%.2f;", minSupport));
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
        return "contractEdges minSupport=<number>;";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        final Document doc = ((Director) getDir()).getDocument();
        return doc.getNumberOfTrees() > 0;
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
