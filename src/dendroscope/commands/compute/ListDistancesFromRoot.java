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

package dendroscope.commands.compute;

import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.window.TreeViewer;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.gui.commands.ICommand;
import jloda.phylo.PhyloTree;
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
                    System.out.println(String.format("%s: %d %g%s", label, edges, distance, (special ? " (warning: contains reticulation in path)" : "")));
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
