/*
 * ShowScrollBarsCommand.java Copyright (C) 2022 Daniel H. Huson
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

import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * show or hide individual scrollbars
 * Daniel Huson, 6.2010
 */
public class ShowScrollBarsCommand extends CommandBaseMultiViewer implements ICheckBoxCommand {
    /**
     * this is currently selected?
     *
     * @return selected
     */
    public boolean isSelected() {
        return multiViewer.getTreeGrid().isShowScrollBars();
    }

    /**
     * set the selected status of this command
     *
	 */
    public void setSelected(boolean selected) {
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Show Scroll Bars";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "show or hide scroll bars";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("ScrollBars16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    /**
     * parses the given command and executes it
     *
	 */

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("show scrollbars=");
        boolean show = np.getBoolean();
        np.matchIgnoreCase(";");
        MultiViewer viewer = (MultiViewer) getViewer();
        viewer.getTreeGrid().setShowScrollBars(show);
    }

    /**
     * action to be performed
     *
	 */
    public void actionPerformed(ActionEvent ev) {
        execute("show scrollbars=" + (!isSelected()) + ";");
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
     * get command-line usage description
     *
     * @return usage
     */

    public String getSyntax() {
        return "show scrollbars={true|false};";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        MultiViewer viewer = (MultiViewer) getViewer();
        TreeViewer treeViewer = viewer.getTreeGrid().getIterator().next();

        return treeViewer != null && (treeViewer.getWidth() > 300 || treeViewer.getHeight() > 200);
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
