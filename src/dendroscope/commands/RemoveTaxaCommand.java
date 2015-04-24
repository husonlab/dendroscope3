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

import dendroscope.core.Document;
import dendroscope.embed.LayoutOptimizerManager;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.graph.NodeSet;
import jloda.gui.commands.ICommand;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * command Daniel Huson, 6.2010
 */
public class RemoveTaxaCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Delete Taxa";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Delete all selected taxa in the selected trees";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Delete16.gif");
    }

    /**
     * gets the accelerator key to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, Toolkit
                .getDefaultToolkit().getMenuShortcutKeyMask());
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("remove taxa=");
        boolean selected = false;
        Set<String> labels = new HashSet<>();

        if (np.peekMatchIgnoreCase("selected")) {
            np.matchIgnoreCase("selected;");
            selected = true;
        } else {
            labels.addAll(np.getTokensRespectCase(null, ";"));
        }

        Document doc = getDir().getDocument();

        for (Iterator<TreeViewer> it = ((MultiViewer) getViewer()).getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();

            NodeSet nodes;
            if (selected) {
                nodes = treeViewer.getSelectedNodes();
            } else {
                nodes = treeViewer.findLabeledNodes(labels);
            }
            if (nodes.size() > 0) {
                //   doc.getTree(multiViewer.getTreeGrid().getNumberOfViewerInDocument(treeViewer)).syncViewer2Data(treeViewer, treeViewer.isDirty());
                if (treeViewer.removeTaxa(nodes)) {
                    treeViewer.getPhyloTree().getNode2GuideTreeChildren().clear();
                    LayoutOptimizerManager.apply(multiViewer.getEmbedderName(), treeViewer.getPhyloTree());
                    treeViewer.setDirty(true);
                    treeViewer.recomputeEmbedding(true, true);
                    treeViewer.resetLabelPositions(true);
                    treeViewer.getScrollPane().invalidate();
                    treeViewer.getScrollPane().repaint();
                    doc.getTree(multiViewer.getTreeGrid().getNumberOfViewerInDocument(treeViewer)).syncViewer2Data(treeViewer, treeViewer.isDirty());
                }
            }
        }
    }


    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        executeImmediately("remove taxa=selected;");
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
    @Override
    public String getSyntax() {
        return "remove taxa={<selected>|names};";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return multiViewer.getTreeGrid().getSelectedNodesIterator().hasNext();
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
