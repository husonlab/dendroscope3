/*
 *   ListDistancesFromRoot.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.commands.compute;

import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.window.TreeViewer;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.swing.commands.ICommand;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * computes the distance from each taxon to the root and reports it
 */
public class ListDistancesFromRoot extends CommandBaseMultiViewer implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        executeImmediately("show messageWindow;");

        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedNodesIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            System.err.println("Distance taxa to root for '" + viewer.getName() + "':");
            PhyloTree tree = viewer.getPhyloTree();
            boolean special = false;
            for (Node v : viewer.getSelectedNodes()) {
                String label = viewer.getLabel(v);
                if (label != null && label.trim().length() > 0) {
                    Node w = v;
                    int edges = 0;
                    double distance = 0;
                    while (w.getInDegree() > 0) {
                        if (!special && w.getInDegree() > 1)
                            special = true;
                        Edge e = w.getFirstInEdge();
                        edges++;
                        distance += tree.getWeight(e);
                        w = e.getSource();
                    }
                    System.out.printf("%s: %d %g%s%n", label, edges, distance, (special ? " (warning: contains reticulation in path)" : ""));
                }
            }
        }
    }

    public String getSyntax() {
        return "list distanceFromRoot;";
    }

    public void actionPerformed(ActionEvent ev) {
        execute(getSyntax());
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Lists the distance to root for all selected taxa";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getName() {
        return "Distance To Root...";
    }

    public String getUndo() {
        return null;
    }

    public boolean isApplicable() {
        return multiViewer.getTreeGrid().getSelectedNodesIterator().hasNext();
    }

    public boolean isCritical() {
        return true;
    }
}
