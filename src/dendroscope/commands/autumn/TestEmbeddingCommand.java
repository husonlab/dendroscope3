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

package dendroscope.commands.autumn;

import dendroscope.embed.EmbedderForOrderPrescribedNetwork;
import dendroscope.embed.LayoutOptimizerManager;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.graph.Node;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.Alert;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * tests the new embedding algorithm
 * Daniel Huson, 6.2011
 */
public class TestEmbeddingCommand extends CommandBase implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Test Embedding";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Test new embedding algorithm";
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
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return false;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return true;
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        MultiViewer multiViewer = (MultiViewer) getViewer();

        multiViewer.setEmbedderName(LayoutOptimizerManager.UNOPTIMIZED);
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            // treeViewer.setDirty(true);
            // treeViewer.getPhyloTree().getNode2GuideTreeChildren().clear();
            try {
                Map<Node, Float> node2pos = EmbedderForOrderPrescribedNetwork.setupAlphabeticalOrdering(treeViewer);
                System.err.println("Ordering:");
                for (Node v = treeViewer.getPhyloTree().getFirstNode(); v != null; v = v.getNext()) {
                    if (node2pos.get(v) != null)
                        System.err.println(treeViewer.getLabel(v) + " -> " + node2pos.get(v));
                }
                EmbedderForOrderPrescribedNetwork.apply(treeViewer, node2pos);
            } catch (IOException e) {
                Basic.caught(e);
                new Alert(getViewer().getFrame(), "Exception: " + e.getMessage());
            }
        }
        multiViewer.setMustRecomputeEmbedding(true);
        multiViewer.setMustRecomputeCoordinates(true);
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "test-embedding;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        execute(getSyntax());
    }
}
