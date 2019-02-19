/**
 * UncollapseSubtreeCommand.java 
 * Copyright (C) 2019 Daniel H. Huson
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

import jloda.gui.commands.ICommand;
import jloda.util.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class UncollapseSubtreeCommand extends UncollapseSelectionCommand implements ICommand {

    public String getSyntax() {
        return null;
    }


    public void actionPerformed(ActionEvent ev) {
        execute("uncollapse what=subtree;");
    }


    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK);
    }


    public String getDescription() {
        return "completely uncollapse a collapsed subtree";
    }


    public ImageIcon getIcon() {
        return ResourceManager.getIcon("UncollapseSubTree16.gif");
    }


    public String getName() {
        return "Uncollapse Subtree";
    }

    public boolean isApplicable() {
        return multiViewer.getTreeGrid().getSelectedNodesIterator().hasNext();
    }

    public boolean isCritical() {
        return true;
    }
}
