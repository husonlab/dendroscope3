/**
 * ReplaceCommand.java 
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
package dendroscope.commands;

import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.gui.commands.ICommand;
import jloda.gui.find.EdgeLabelSearcher;
import jloda.gui.find.ISearcher;
import jloda.gui.find.NodeLabelSearcher;
import jloda.gui.find.SearchManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * command Daniel Huson, 6.2010
 */
public class ReplaceCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Replace...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Replace all occurrences of a text";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return null;
    }

    /**
     * gets the accelerator key to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        if (np.peekMatchIgnoreCase("replace searchtext")) {
            np.matchIgnoreCase("replace searchtext=");
            String searchText = np.getWordFileNamePunctuation();
            np.matchIgnoreCase("replacetext=");
            String replaceText = np.getWordFileNamePunctuation();

            String target = "nodes";
            if (np.peekMatchIgnoreCase("target")) {
                np.matchIgnoreCase("target=");
                target = np.getWordMatchesIgnoringCase("Nodes Edges");
            }
            boolean all = true;
            if (np.peekMatchIgnoreCase("all")) {
                np.matchIgnoreCase("all=");
                all = np.getBoolean();
            }
            boolean regex = false;
            if (np.peekMatchIgnoreCase("regex")) {
                np.matchIgnoreCase("regex=");
                regex = np.getBoolean();
            }
            boolean wholewords = false;
            if (np.peekMatchIgnoreCase("wholewords")) {
                np.matchIgnoreCase("wholewords=");
                wholewords = np.getBoolean();
            }
            boolean respectcase = false;
            if (np.peekMatchIgnoreCase("respectcase")) {
                np.matchIgnoreCase("respectcase=");
                respectcase = np.getBoolean();
            }
            np.matchIgnoreCase(";");
            for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
                TreeViewer viewer = it.next();
                SearchManager searchManager = new SearchManager(new ISearcher[]{new NodeLabelSearcher(viewer), new EdgeLabelSearcher(viewer)});
                if (searchManager.runFindReplace(searchText, replaceText, target, all, regex, wholewords, respectcase)) {
                    viewer.setDirty(true);
                    viewer.repaint();
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
    }

    /**
     * is this a critical command that can only be executed when no other
     * command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return false;
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "replace searchtext=<text> replacetext=<text> [target={nodes|edges}] [all={true|false}] [regex={true|false}] [wholewords={true|false}] [respectcase={true|false}];";
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
        return null;
    }
}



