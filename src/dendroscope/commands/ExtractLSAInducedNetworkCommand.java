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

package dendroscope.commands;

import dendroscope.core.Director;
import dendroscope.core.Document;
import dendroscope.embed.LayoutOptimizerManager;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.gui.commands.ICommand;
import jloda.gui.director.IDirector;
import jloda.phylo.PhyloTree;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * close the window
 * Celine Scornavacca, 7.2010
 */
public class ExtractLSAInducedNetworkCommand extends CommandBaseMultiViewer implements ICommand {

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Extract LSA Induced Network...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Extract subtree or subnetwork rooted at the LSA of the selected nodes";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("SelectInduced16.gif");
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
        np.matchIgnoreCase(getSyntax());

        String[] names = new String[multiViewer.getTreeGrid().getNumberSelectedOrAllViewers()];
        List<PhyloTree> subNetworks = new LinkedList<>();

        int numbNetworksSelected = 0;
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            PhyloTree temp = viewer.getPhyloTree();
            PhyloTree newTree = dendroscope.util.PhyloTreeUtils.extractLSAInducedSubNetwork(viewer.getPhyloTree(),
                    viewer.getSelectedNodes(), viewer.getCollapsedNodes(), true);
            subNetworks.add(newTree);
            names[numbNetworksSelected] = viewer.getName();
            numbNetworksSelected++;

        }

        int rows = Math.min(4, (int) (Math.sqrt(subNetworks.size())));
        int cols = subNetworks.size() / rows;


        Director newDir = Director.newProject(rows, cols);
        MultiViewer newMultiViewer = (MultiViewer) newDir.getViewerByClass(MultiViewer.class);
        newMultiViewer.setEmbedderName(LayoutOptimizerManager.UNOPTIMIZED); // don't change best common embedding
        Document newDoc = newDir.getDocument();
        BitSet which = new BitSet();
        for (int i = 0; i < subNetworks.size(); i++) {
            newDoc.appendTree(names[i], subNetworks.get(i), i);
            // System.err.println("tree[" + i + "] in doc: " + newDoc.getTree(i).toBracketString());
            which.set(i);
        }

        newMultiViewer.loadTrees(which);

        newMultiViewer.setMustRecomputeEmbedding(true);
        newMultiViewer.updateView(IDirector.ALL);
        newMultiViewer.getFrame().toFront();


        newMultiViewer.getTreeGrid().repaint();
    }


    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        executeImmediately(getSyntax());
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
        return "Extract LSA induced network ;";
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
