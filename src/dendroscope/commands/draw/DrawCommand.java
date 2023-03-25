/*
 * DrawCommand.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.commands.draw;

import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.window.TreeViewer;
import jloda.swing.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * the draw command
 * Daniel Huson, 6.2010
 */
public abstract class DrawCommand extends CommandBaseMultiViewer {
    /**
     * get command-line syntax. First two tokens are used to identify the command
     *
     * @return usage
     */
    public String getSyntax() {
        return "set drawer=<drawer-name>;";
    }

    /**
     * action to be performed
     *
	 */
    public void actionPerformed(ActionEvent ev) {
        String[] drawers = new String[]{TreeViewer.RECTANGULAR_PHYLOGRAM,
                TreeViewer.RECTANGULAR_CLADOGRAM,
                TreeViewer.SLANTED_CLADOGRAM,
                TreeViewer.RADIAL_PHYLOGRAM,
                TreeViewer.RADIAL_CLADOGRAM,
                TreeViewer.CIRCULAR_CLADOGRAM,
                TreeViewer.CIRCULAR_PHYLOGRAM,
                TreeViewer.INNERCIRCULAR_CLADOGRAM};

        String drawer = (String) JOptionPane.showInputDialog(multiViewer.getFrame(), "Set drawer", "Set drawer", JOptionPane.QUESTION_MESSAGE,
                ProgramProperties.getProgramIcon(), drawers, drawers[0]);
        if (drawer != null)
            execute("set drawer=" + drawer + ";");
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
        return getDir().getDocument().getNumberOfTrees() > 0;
    }

    /**
     * parses the given command and executes it
     */
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set drawer=");
        String drawerKind = np.getWordMatchesIgnoringCase
                (TreeViewer.RECTANGULAR_PHYLOGRAM + " "
                 + TreeViewer.RECTANGULAR_CLADOGRAM + " "
                 + TreeViewer.SLANTED_CLADOGRAM + " "
                 + TreeViewer.RADIAL_PHYLOGRAM + " "
                 + TreeViewer.RADIAL_CLADOGRAM + " "
                 + TreeViewer.CIRCULAR_CLADOGRAM + " "
                 + TreeViewer.CIRCULAR_PHYLOGRAM + " "
                 + TreeViewer.INNERCIRCULAR_CLADOGRAM);
        np.matchIgnoreCase(";");

        boolean showScaleBar = (drawerKind.equals(TreeViewer.RECTANGULAR_PHYLOGRAM)
                || drawerKind.equals(TreeViewer.RADIAL_PHYLOGRAM)
                || drawerKind.equals(TreeViewer.CIRCULAR_PHYLOGRAM));

        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            viewer.setShowScaleBar(showScaleBar);
            viewer.setDrawerKind(drawerKind);
            viewer.recomputeEmbedding(true, true);
            viewer.resetLabelPositions(true);
            viewer.getScrollPane().invalidate();
        }
    }
}
