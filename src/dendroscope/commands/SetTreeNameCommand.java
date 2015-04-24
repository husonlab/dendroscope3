/**
 * Copyright 2015, Daniel Huson
 *
 *(Some files contain contributions from other authors, who are then mentioned separately)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package dendroscope.commands;

import dendroscope.core.Document;
import dendroscope.window.TreeViewer;
import jloda.gui.commands.ICommand;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * show or hide borders
 * Daniel Huson, 6.2010
 */
public class SetTreeNameCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Set Tree Name...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set the name of a tree or network";
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
     * @param np
     * @throws java.io.IOException
     */

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set name=");
        String name = np.getWordFileNamePunctuation();
        String template = name.contains("%t") ? name : null;
        int treeId = -1;
        Document doc = multiViewer.getDir().getDocument();
        if (np.peekMatchIgnoreCase("treeId")) {
            np.matchIgnoreCase("treeId=");
            treeId = np.getInt(0, doc.getNumberOfTrees());
        }
        np.matchIgnoreCase(";");

        if (treeId != -1) {
            doc.setDocumentIsDirty(true);
            for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getIterator(); it.hasNext(); ) {
                TreeViewer treeViewer = it.next();
                if (multiViewer.getTreeGrid().getNumberOfViewerInDocument(treeViewer) == treeId) {
                    if (template != null)
                        doc.setName(template.replaceAll("%t", "" + (treeId + 1)), treeId);
                    else
                        doc.setName(name, treeId);
                    treeViewer.setName(doc.getName(treeId));
                    treeViewer.setDirty(true);
                    if (multiViewer.getTreeGrid().isShowBorders())
                        treeViewer.getScrollPane().setBorder(BorderFactory.createTitledBorder(treeViewer.getName() + (treeViewer.isDirty() ? "*" : "")));
                    break;
                }
            }
        } else {
            for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
                TreeViewer treeViewer = it.next();
                treeId = multiViewer.getTreeGrid().getNumberOfViewerInDocument(treeViewer);
                if (template != null)
                    doc.setName(template.replaceAll("%t", "" + treeId), treeId);
                else
                    doc.setName(name, treeId);
                treeViewer.setName(doc.getName(treeId));
                treeViewer.setDirty(true);
                doc.setDocumentIsDirty(true);
                if (multiViewer.getTreeGrid().isShowBorders())
                    treeViewer.getScrollPane().setBorder(BorderFactory.createTitledBorder(treeViewer.getName() + (treeViewer.isDirty() ? "*" : "")));
            }
        }
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        String template = null;
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            if (treeViewer.getPhyloTree().getNumberOfNodes() > 0) {
                String original = treeViewer.getName() != null ? treeViewer.getName() : "";

                String name = "";
                if (template == null) {
                    name = JOptionPane.showInputDialog(getViewer().getFrame(), "Name for tree:", original);
                    if (name == null)
                        break;
                    name = name.trim();
                    if (name.contains("%t")) {
                        template = name;
                        System.err.println("Name contains template %t, will replace by tree number for each tree");
                    }
                }
                if (template != null) {
                    name = template;
                }
                if (original == null && name.length() > 0 || original != null && !name.equals(original)) {
                    executeImmediately("set name='" + name + "' treeId=" + multiViewer.getTreeGrid().getNumberOfViewerInDocument(treeViewer) + ";");
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

    public String getSyntax() {
        return "set name=<name> [treeId=<tree-number>];";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return multiViewer.getTreeGrid().getNumberSelectedOrAllViewers() > 0;
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
