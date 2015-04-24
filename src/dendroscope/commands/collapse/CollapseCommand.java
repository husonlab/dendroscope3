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

public class CollapseCommand extends CommandBaseMultiViewer implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("collapse what=");
        String what = np.getWordMatchesIgnoringCase("selected complement");
        np.matchIgnoreCase(";");

        if (what.equalsIgnoreCase("selected")) {
            for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
                TreeViewer viewer = it.next();
                if (viewer.collapseSelectedNodes(true, true)) {
                    viewer.setDirty(true);
                    viewer.recomputeEmbedding(false, true);
                }
            }
        } else if (what.equalsIgnoreCase("complement") || what.equalsIgnoreCase("all")) {
            for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
                TreeViewer viewer = it.next();
                if (viewer.collapseComplement()) {
                    viewer.setDirty(true);
                    viewer.recomputeEmbedding(false, true);
                }
            }
        }
    }

    public String getSyntax() {
        return "collapse what={selected|complement};";
    }

    public void actionPerformed(ActionEvent ev) {
        execute("collapse what=selected;");
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    public String getDescription() {
        return "Collapse the selected nodes";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("CollapseTree16.gif");
    }

    public String getName() {
        return "Collapse";
    }

    public String getUndo() {
        return null;
    }

    public boolean isApplicable() {
        return multiViewer.getTreeGrid().getSelectedNodesIterator().hasNext() && multiViewer.getDir().getDocument().getNumberOfTrees() > 0;
    }

    public boolean isCritical() {
        return true;
    }
}
