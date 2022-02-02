/*
 * CopyEdgeLabelCommand.java Copyright (C) 2022 Daniel H. Huson
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
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * copy all selected edge labels
 * Daniel Huson, 8.2010
 */
public class CopyEdgeLabelCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Copy Edge Labels";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Copy the labels of all selected edges";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Copy16.gif");
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
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        StringBuilder buf = new StringBuilder();

        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedEdgesIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            boolean first = true;
            for (Edge v : viewer.getSelectedEdges()) {
                String label = viewer.getLabel(v);
                if (label != null && label.trim().length() > 0) {
                    label = label.trim();
                    if (first == true)
                        first = false;
                    else
                        buf.append("\t");
                    buf.append(label);
                }
            }
            if (!first)
                buf.append("\n");
        }
        if (buf.toString().length() > 0) {
            StringSelection selection = new StringSelection(buf.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        }
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
    @Override
    public String getSyntax() {
        return "copy what=edgelabels;";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return multiViewer.getTreeGrid().getSelectedEdgesIterator().hasNext() && ((MultiViewer) getViewer()).getDir().getDocument().getNumberOfTrees() > 0;
    }

    /**
     * action to be performed
     *
	 */
    @Override
    public void actionPerformed(ActionEvent ev) {
        executeImmediately(getSyntax());
    }
}
