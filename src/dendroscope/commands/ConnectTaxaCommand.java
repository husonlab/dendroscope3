/**
 * ConnectTaxaCommand.java 
 * Copyright (C) 2015 Daniel H. Huson
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
import dendroscope.window.TreeGrid;
import dendroscope.window.TreeViewer;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * connect taxa in different viewers
 * Daniel Huson, 6.2010
 */
public class ConnectTaxaCommand extends CommandBase implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Connect Taxa";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Connect all taxa of the same name in different trees or networks";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Match16.gif");
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
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        TreeGrid treeGrid = ((MultiViewer) getViewer()).getTreeGrid();
        np.matchIgnoreCase("connect");
        np.matchIgnoreCase("what=");
        String what = np.getWordMatchesIgnoringCase("taxa");
        List<Integer> panels = new LinkedList<>();
        if (np.peekMatchIgnoreCase("panels")) {
            np.matchIgnoreCase("panels=");
            while (!np.peekMatchIgnoreCase(";")) {
                panels.add(np.getInt(0, treeGrid.getNumberOfPanels()));
            }
        }
        np.matchIgnoreCase(";");

        for (Integer panel1 : panels) {
            for (Integer panel2 : panels) {
                if (panel2 > panel1) {
                    if (what.equalsIgnoreCase("taxa"))
                        treeGrid.connectorAllTaxa(treeGrid.getViewerByRank(panel1), treeGrid.getViewerByRank((panel2)));
                }
            }
        }
        treeGrid.repaint();
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        String command = "connect what=taxa panels=";
        for (Iterator<TreeViewer> it = ((MultiViewer) getViewer()).getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            int id = ((MultiViewer) getViewer()).getTreeGrid().getRankOfViewer(treeViewer);
            command += " " + id;
        }
        command += ";";
        execute(command);
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
        return "connect what=taxa panels=<panel-ids>;";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return ((MultiViewer) getViewer()).getTreeGrid().getNumberSelectedOrAllViewers() > 1 && ((MultiViewer) getViewer()).getDir().getDocument().getNumberOfTrees() > 0;
    }
}
