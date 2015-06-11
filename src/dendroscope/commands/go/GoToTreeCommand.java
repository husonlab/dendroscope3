/**
 * GoToTreeCommand.java 
 * Copyright (C) 2015 Daniel H. Huson
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
package dendroscope.commands.go;

import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.window.MultiViewer;
import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

/**
 * move to a named
 * Daniel Huson, 5.2010
 */
public class GoToTreeCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Go to Tree...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Go to a specific tree";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/navigation/Forward16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("go tree=");
        String which = np.getWordRespectCase().toLowerCase();
        switch (which) {
            case "first":
                multiViewer.goToTree(0);
                break;
            case "next":
                multiViewer.goNextTree();
                break;
            case "nextpage": {
                int t = multiViewer.getTreeGrid().getNumberOfViewerInDocument(multiViewer.getTreeGrid().getTreeViewer(0, 0))
                        + multiViewer.getTreeGrid().getNumberOfPanels() + 1;
                multiViewer.goToTree(Math.min(getDir().getDocument().getNumberOfTrees(), t));
                break;
            }
            case "prev":
                multiViewer.goPreviousTree();
                break;
            case "prevpage": {
                int t = multiViewer.getTreeGrid().getNumberOfViewerInDocument(multiViewer.getTreeGrid().getTreeViewer(0, 0))
                        - multiViewer.getTreeGrid().getNumberOfPanels() + 1;
                multiViewer.goToTree(Math.max(1, t));
                break;
            }
            case "last":
                multiViewer.goToTree(getDir().getDocument().getNumberOfTrees());
                break;
            default:
                if (!Basic.isInteger(which))
                    throw new IOException("go tree=" + which + " illegal specification, expected: first, prev, prevpage, next, nextpage, last or <number>");
                multiViewer.goToTree(Integer.parseInt(which));
                break;
        }
        multiViewer.setMustRecomputeEmbedding(true);
        // multiViewer.setMustRecomputeCoordinates(true) ;
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        String current = "" + multiViewer.getTreeGrid().getNumberOfViewerInDocument(multiViewer.getTreeGrid().getViewerByRank(0));
        current = JOptionPane.showInputDialog(multiViewer.getFrame(), "Specify tree (first,next,prev,last or number)", current);
        if (current != null)
            execute("go tree=" + current + ";");
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */

    public String getSyntax() {
        return "go tree={first,next,nextpage,prev,prevpage,last,<number>};";
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
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
        return "go prev;";
    }
}
