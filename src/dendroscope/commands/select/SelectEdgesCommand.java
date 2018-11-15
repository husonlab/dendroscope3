/**
 * SelectEdgesCommand.java 
 * Copyright (C) 2018 Daniel H. Huson
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
package dendroscope.commands.select;

import dendroscope.core.Director;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.graph.Edge;
import jloda.graph.EdgeSet;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * Select all edges
 * Daniel Huson, 6.2010
 */
public class SelectEdgesCommand extends CommandBase implements ICommand {

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Select Edges";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Select edges";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("SelectEdges16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
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
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("select edges=");
        String what = np.getWordMatchesIgnoringCase("all none short long");
        double threshold = 0;
        if (what.equals("short") || what.equals("long")) {
            np.matchIgnoreCase("threshold=");
            threshold = np.getDouble(0, 1000);
        }
        np.matchIgnoreCase(";");

        for (Iterator<TreeViewer> it = ((MultiViewer) getViewer()).getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            final TreeViewer viewer = it.next();
            switch (what) {
                case "all":
                    viewer.selectAllEdges(true);
                    break;
                case "none":
                    viewer.selectAllEdges(false);
                    break;
                case "short": {
                    final EdgeSet toSelect = new EdgeSet(viewer.getPhyloTree());
                    for (Edge e = viewer.getGraph().getFirstEdge(); e != null; e = e.getNext()) {
                        if (viewer.getPhyloTree().getWeight(e) < threshold)
                            toSelect.add(e);
                    }
                    viewer.setSelected(toSelect, true);
                    break;
                }
                case "long": {
                    final EdgeSet toSelect = new EdgeSet(viewer.getPhyloTree());
                    for (Edge e = viewer.getGraph().getFirstEdge(); e != null; e = e.getNext()) {
                        if (viewer.getPhyloTree().getWeight(e) >= threshold)
                            toSelect.add(e);
                    }
                    viewer.setSelected(toSelect, true);
                    break;
                }
            }
            viewer.repaint();
        }
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        executeImmediately("select edges=all;");
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
     * get command-line usage description
     *
     * @return usage
     */

    public String getSyntax() {
        return "select edges={all|none|long|short} [threshold=<number>];";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return ((Director) getDir()).getDocument().getNumberOfTrees() > 0;
    }

    public String getUndo() {
        return null;
    }

}
