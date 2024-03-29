/*
 * ExpandVerticalCommand.java Copyright (C) 2023 Daniel H. Huson
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
import jloda.swing.commands.ICommand;
import jloda.swing.graphview.GraphView;
import jloda.swing.graphview.ScrollPaneAdjuster;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * expand vertically
 * Daniel Huson, 8.2010
 */
public class ExpandVerticalCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Expand Vertical";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Expand view vertically";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("ExpandVertical16.gif");
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
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("expand direction=");
        var direction = np.getWordMatchesIgnoringCase("vertical horizontal");
        np.matchIgnoreCase(";");

        for (var it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            var viewer = it.next();
            if (direction.equalsIgnoreCase("horizontal")) {
                var scale = 1.2 * viewer.trans.getScaleX();
                if (scale <= GraphView.XMAX_SCALE) {
                    var spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
                    viewer.trans.composeScale(1.2, 1);
                    spa.adjust(true, false);
                }
            } else {
                var scale = 2 * viewer.trans.getScaleY();
                if (scale <= GraphView.YMAX_SCALE) {
                    var spa = new ScrollPaneAdjuster(viewer.getScrollPane(), viewer.trans);
                    viewer.trans.composeScale(1, 1.2);
                    spa.adjust(false, true);
                }
            }
        }
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
    @Override
    public String getSyntax() {
        return "expand direction={vertical|horizontal};";
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
     * action to be performed
     *
	 */
    @Override
    public void actionPerformed(ActionEvent ev) {
        executeImmediately("expand direction=vertical;");
    }
}
