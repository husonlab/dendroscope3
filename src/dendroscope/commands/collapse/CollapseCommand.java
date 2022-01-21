/*
 * CollapseCommand.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dendroscope.commands.collapse;

import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.window.TreeViewer;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
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
        return KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
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
