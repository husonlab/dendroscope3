/*
 *   CollapseComplementCommand.java Copyright (C) 2023 Daniel H. Huson
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

import dendroscope.window.TreeViewer;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;

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
        return KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK);
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
            if (viewer.getPhyloTree().getNumberReticulateEdges() > 0)
				return false;
        }
        return multiViewer.getTreeGrid().getSelectedNodesIterator().hasNext() && multiViewer.getDir().getDocument().getNumberOfTrees() > 0;
    }

}
