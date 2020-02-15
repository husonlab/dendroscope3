/*
 *   UncollapseSelectionCommand.java Copyright (C) 2020 Daniel H. Huson
 *
 *   (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
        return KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
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
