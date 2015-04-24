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

import dendroscope.embed.LayoutOptimizerManager;
import dendroscope.window.TreeViewer;
import jloda.gui.commands.ICheckBoxCommand;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * choose embedding algorithm
 * Daniel Huson, 7.2010
 */
public class LayoutNoOptimizerCommand extends CommandBaseMultiViewer implements ICheckBoxCommand {
    public boolean isSelected() {
        return multiViewer.getEmbedderName().equalsIgnoreCase(LayoutOptimizerManager.UNOPTIMIZED);
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set layouter=");
        String embedder = np.getWordMatchesIgnoringCase(LayoutOptimizerManager.getEmbedderNames());
        np.matchIgnoreCase(";");
        multiViewer.setEmbedderName(embedder);
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setDirty(true);
            treeViewer.getPhyloTree().getNode2GuideTreeChildren().clear(); // this will force recomputation of embedding
            LayoutOptimizerManager.apply(embedder, treeViewer.getPhyloTree());
        }
        multiViewer.getCommandManager().updateEnableState();
        multiViewer.setMustRecomputeEmbedding(true);
        multiViewer.setMustRecomputeCoordinates(true);
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
        return "set layouter={" + Basic.toString(LayoutOptimizerManager.getEmbedderNames(), "|") + "};";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return getDir().getDocument().getNumberOfTrees() > 0;
    }


    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "No Optimization";
    }

    public String getAltName() {
        return "Layout Optimizer None";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Choose layout optimizer for networks";
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
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        execute("set layouter=" + LayoutOptimizerManager.UNOPTIMIZED + ";");
    }
}
