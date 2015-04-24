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

package dendroscope.commands.collapse;

import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.window.TreeViewer;
import jloda.gui.commands.ICommand;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Iterator;

public class UncollapseSelectionCommand extends CommandBaseMultiViewer implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("uncollapse what=");
        String what = np.getWordMatchesIgnoringCase("selected all subtree");
        np.matchIgnoreCase(";");

        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedNodesIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            if (viewer.getPhyloTree().getRoot() != null) {
                if (what.equalsIgnoreCase("selected")) {
                    if (viewer.collapseSelectedNodes(false, false)) {
                        viewer.setDirty(true);
                        viewer.recomputeEmbedding(false, true);
                    }
                } else {
                    boolean originalSelectionState = true;
                    if (what.equalsIgnoreCase("all")) {
                        originalSelectionState = viewer.getSelected(viewer.getPhyloTree().getRoot());
                        if (originalSelectionState == false)
                            viewer.setSelected(viewer.getPhyloTree().getRoot(), true);
                    }
                    if (viewer.collapseSelectedNodes(false, true)) {
                        viewer.setDirty(true);
                        viewer.recomputeEmbedding(false, true);
                    }
                    if (what.equalsIgnoreCase("all")) {
                        if (originalSelectionState == false)
                            viewer.setSelected(viewer.getPhyloTree().getRoot(), false);
                    }

                }
            }
        }
    }


    public String getSyntax() {
        return "uncollapse what={selected|all|subtree};";
    }


    public void actionPerformed(ActionEvent ev) {
        execute("uncollapse what=selected;");
    }


    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }


    public String getDescription() {
        return "Uncollapse all selected nodes";
    }


    public ImageIcon getIcon() {
        return ResourceManager.getIcon("UncollapseTree16.gif");
    }


    public String getName() {
        return "Uncollapse";
    }


    public String getUndo() {
        return null;
    }


    public boolean isApplicable() {
        return multiViewer.getTreeGrid().getSelectedNodesIterator().hasNext();
    }


    public boolean isCritical() {
        return true;
    }

}
