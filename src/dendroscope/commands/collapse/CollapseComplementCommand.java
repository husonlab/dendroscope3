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

import dendroscope.window.TreeViewer;
import jloda.gui.commands.ICommand;
import jloda.util.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Iterator;

public class CollapseComplementCommand extends CollapseCommand implements ICommand {
    public String getSyntax() {
        return null;
    }

    public void actionPerformed(ActionEvent ev) {
        execute("collapse what=complement;");
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK);
    }

    public String getDescription() {
        return "Collapse the complement of the current selected nodes";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("UncollapseSubTree16.gif");
    }

    public String getName() {
        return "Collapse Complement";
    }

    public String getUndo() {
        return null;
    }

    public boolean isApplicable() {
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            if (viewer.getPhyloTree().getSpecialEdges().size() > 0)
                return false;
        }
        return multiViewer.getTreeGrid().getSelectedNodesIterator().hasNext() && multiViewer.getDir().getDocument().getNumberOfTrees() > 0;
    }

}
