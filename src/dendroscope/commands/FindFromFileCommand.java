/**
 * FindFromFileCommand.java 
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

import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.swing.commands.ICommand;
import jloda.swing.find.EdgeLabelSearcher;
import jloda.swing.find.ISearcher;
import jloda.swing.find.NodeLabelSearcher;
import jloda.swing.find.SearchManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * command Daniel Huson, 6.2010
 */
public class FindFromFileCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return null;
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Find all occurrences of a text that is input from a file";
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
        np.matchIgnoreCase("find searchfile=");
        String fileName = np.getWordFileNamePunctuation();
        String target = "nodes";
        if (np.peekMatchIgnoreCase("target")) {
            np.matchIgnoreCase("target=");
            target = np.getWordMatchesIgnoringCase("nodes edges");
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
        File file = new File(fileName);

        if (!file.exists() || !file.canRead())
            throw new IOException("Can't open file to read: " + file.getPath());

        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            SearchManager searchManager = new SearchManager(new ISearcher[]{new NodeLabelSearcher(viewer), new EdgeLabelSearcher(viewer)});
            searchManager.runFindFromFile(file, target, regex, wholewords, respectcase);
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
        return true;
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "find searchfile=<text> [target={nodes|edges}] [regex={true|false}] [wholewords={true|false}] [respectcase={true|false}];";
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



