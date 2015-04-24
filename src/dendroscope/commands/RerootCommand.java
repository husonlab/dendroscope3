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
import dendroscope.util.RerootingUtils;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.gui.commands.ICommand;
import jloda.phylo.PhyloTree;
import jloda.util.Alert;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import java.util.Set;

/**
 * command Daniel Huson, 6.2010
 */
public class RerootCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Reroot";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Reroot the tree or network using the selected node, edge or taxa";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Reroot16.gif");
    }

    /**
     * gets the accelerator key to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK);
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

        boolean warned = false;
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();

            boolean changed = false;
            if (treeViewer.getSelectedNodeLabelsNotInternalNumbers().size() > 0) {
                PhyloTree tree = treeViewer.getPhyloTree();
                Set selectedLabels = treeViewer.getSelectedNodeLabels();
                if (tree.getSpecialEdges().size() > 0) {
                    if (!warned) {
                        warned = true;
                        new Alert(getViewer().getFrame(), "Reroot by outgroup: not implemented for networks");
                    }
                } else
                    changed = RerootingUtils.rerootByOutgroup(treeViewer, selectedLabels);
            } else
                changed = RerootingUtils.reroot(treeViewer);
            if (changed) {
                treeViewer.getPhyloTree().getNode2GuideTreeChildren().clear();
                LayoutOptimizerManager.apply(multiViewer.getEmbedderName(), treeViewer.getPhyloTree());
                treeViewer.setDirty(true);
                treeViewer.recomputeEmbedding(true, true);
                treeViewer.resetLabelPositions(true);
                treeViewer.getScrollPane().invalidate();
                treeViewer.getScrollPane().repaint();
                getDir().getDocument().getTree(multiViewer.getTreeGrid().getNumberOfViewerInDocument(treeViewer)).syncViewer2Data(treeViewer, treeViewer.isDirty());
            }
        }

    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        execute(getSyntax());

    }

    /**
     * is this a critical command that can only be executed when no other
     * command is running?
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
        return "reroot;";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return ((MultiViewer) getViewer()).getDir().getDocument().getNumberOfTrees() > 0;
    }

    /**
     * gets the command needed to undo this command
     *
     * @return undo command
     */
    public String getUndo() {
        return null;
    }
}


