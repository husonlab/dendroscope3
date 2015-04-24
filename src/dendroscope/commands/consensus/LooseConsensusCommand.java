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

package dendroscope.commands.consensus;

import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.consensus.LooseConsensus;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.gui.commands.ICommand;
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
            if (treeViewer.getPhyloTree().getSpecialEdges().size() > 0)
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
