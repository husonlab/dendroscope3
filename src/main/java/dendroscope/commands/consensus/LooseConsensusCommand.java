/*
 * LooseConsensusCommand.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.commands.consensus;

import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.consensus.LooseConsensus;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.swing.commands.ICommand;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

public class LooseConsensusCommand extends CommandBaseMultiViewer implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {
    }

    public String getSyntax() {
        return null;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        MultiViewer multiViewer = (MultiViewer) getViewer();
        int count = 0;
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            if (treeViewer.getPhyloTree().getNumberReticulateEdges() > 0)
				return false;
            count++;
        }
        return count > 1;
    }

    public boolean isCritical() {
        return true;
    }

    public void actionPerformed(ActionEvent ev) {
        execute("compute consensus method=" + LooseConsensus.NAME + ";");
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return null;
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getName() {
        return "Loose Consensus...";
    }

    public String getUndo() {
        return null;
    }
}
