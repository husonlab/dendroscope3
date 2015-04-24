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

package dendroscope.commands.select;


/**
 * Created by IntelliJ IDEA.
 * User: scornava
 * Date: 6/1/11
 * Time: 5:29 PM
 * To change this template use File | Settings | File Templates.
 */


import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.graph.Node;
import jloda.gui.commands.ICommand;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * close the window
 * Daniel Huson, 6.2010
 */
public class SelectLSASubnetworkCommand extends CommandBaseMultiViewer implements ICommand {

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Select LSA Induced Network";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Select subtree or subnetwork induced by the LSA of selected nodes";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("SelectInduced16.gif");
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
        np.matchIgnoreCase("select LSA induced network;");
        for (Iterator<TreeViewer> it = ((MultiViewer) getViewer()).getTreeGrid().getSelectedNodesIterator(); it.hasNext(); ) {

            TreeViewer viewer = it.next();
            Node subnetworkRoot = dendroscope.util.PhyloTreeUtils.selectLSAInducedSubNetwork(viewer, viewer.getPhyloTree(), viewer.getSelectedNodes(), viewer.getCollapsedNodes());
            viewer.selectSubTreeRec(subnetworkRoot, true, true);
            viewer.repaint();

        }


    }


    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        executeImmediately("select LSA induced network;");
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
        return "select LSA induced network;";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return ((MultiViewer) getViewer()).getTreeGrid().getSelectedNodesIterator().hasNext();
    }

    public String getUndo() {
        return null;
    }

}
