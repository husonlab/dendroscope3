/*
 * ListSelectedTaxaCommand.java Copyright (C) 2022 Daniel H. Huson
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

import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;

/**
 * List all selected taxa
 * Daniel Huson, 6.2010
 */
public class ListSelectedTaxaCommand extends CommandBase implements ICommand {

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "List Selected Taxa";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "List all selected taxa";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Empty16.gif");
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
	 */
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("list taxa=selected;");
        executeImmediately("show messageWindow;");
        List<String> taxaLabels = new LinkedList<>();
        for (Iterator<TreeViewer> it = ((MultiViewer) getViewer()).getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();

            Node root = viewer.getPhyloTree().getRoot();
            if (root != null) {
                Set<Node> seen = new HashSet<>();
                Stack<Node> stack = new Stack<>();
                seen.add(root);
                stack.add(root);
                while (stack.size() > 0) {
                    Node v = stack.pop();
                    if (viewer.getSelected(v)) {
                        String label = viewer.getLabel(v);
                        if (label != null) {
                            label = label.trim();
                            if (label.length() > 0) {
                                taxaLabels.add(0, label);
                            }
                        }
                    }

                    for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                        Node w = e.getTarget();
                        if (!seen.contains(w)) {
                            seen.add(w);
                            stack.add(w);
                        }
                    }

                }
            }
            viewer.repaint();
        }

        System.out.println("Selected taxa:");
        for (String label : taxaLabels)
            System.out.println(label);
    }

    /**
     * action to be performed
     *
	 */
    public void actionPerformed(ActionEvent ev) {
        executeImmediately("list taxa=selected;");
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
        return "list taxa=selected;";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return ((MultiViewer) getViewer()).getDir().getDocument().getNumberOfTrees() > 0;
    }

    public String getUndo() {
        return null;
    }

}
