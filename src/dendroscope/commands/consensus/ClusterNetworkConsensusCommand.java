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

import dendroscope.consensus.ComputeNetworkConsensus;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.Alert;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

public class ClusterNetworkConsensusCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    public void actionPerformed(ActionEvent ev) {
        String result;

        if (((MultiViewer) getViewer()).getTreeGrid().getNumberSelectedOrAllViewers() <= 2)
            result = "0";
        else {
            double threshold = ProgramProperties.get("ConsensusThreshold", 20d);
            result = JOptionPane.showInputDialog(getViewer().getFrame(), "Enter percent threshold for network consensus construction", "" + threshold);
        }
        if (result != null) {
            float threshold = Basic.parseFloat(result);
            if (threshold >= 0) {
                ProgramProperties.put("ConsensusThreshold", threshold);
                execute("compute consensus method=" + ComputeNetworkConsensus.CLUSTER_NETWORK + " threshold='" + threshold + "';");
            } else
                new Alert(getViewer().getFrame(), "Number >=0 expected, got: '" + result + "'");
        }
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Compute a consensus cluster-network for the given trees";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getName() {
        return "Cluster Network Consensus...";
    }

    public String getUndo() {
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
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
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
}
