/**
 * PasteCommand.java 
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
package dendroscope.commands;

import dendroscope.core.TreeData;
import dendroscope.io.IOFormat;
import dendroscope.io.IOManager;
import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.StringReader;

/**
 * command Daniel Huson, 6.2010
 */
public class PasteCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Paste";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Paste";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Paste16.gif");
    }

    /**
     * gets the accelerator key to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("paste;");

        Transferable transf = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(DataFlavor.getTextPlainUnicodeFlavor());
        DataFlavor[] flavors = transf.getTransferDataFlavors();
        for (DataFlavor flavor : flavors) {
            // System.err.println("flavor["+i+"]: "+flavors[i]);
            if (flavor.getRepresentationClass().equals(String.class)) {
                try {
                    String string = (String) transf.getTransferData(flavor);
                    IOFormat format = IOManager.createIOFormatForFile(string);
                    if (format != null) {
                        TreeData[] trees = format.read(new StringReader(string));
                        if (trees.length > 0) {
                            multiViewer.addTrees(trees);
                            break;
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Add tree failed: " + ex);
                    Basic.caught(ex);
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
        Action textPaste = null;

        DefaultEditorKit kit = new DefaultEditorKit();
        Action[] defActions = kit.getActions();
        for (Action defAction : defActions) {
            if (defAction.getValue(Action.NAME) == DefaultEditorKit.pasteAction) {
                textPaste = defAction;
                break;
            }
        }
        final Action textPasteFinal = textPaste;
        if (multiViewer.getFrame().isActive() || textPasteFinal == null) {
            execute("paste;");
        } else
            textPasteFinal.actionPerformed(ev);
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
        return "paste;";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return true;
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



