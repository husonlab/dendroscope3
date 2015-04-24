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

import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * incremement the first auxiliary parameter
 * Daniel Huson, 7.2010
 */
public class IncrementDrawingAuxiliaryCommand extends CommandBase implements ICommand {
    public IncrementDrawingAuxiliaryCommand() {
        setAutoRepeatInterval(150);
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Increment Auxiliary Parameter";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Increments the  drawing auxiliary parameter";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("AuxPlus16.gif");
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
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }


    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        executeImmediately("auxiliaryparameter change=increment;");
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("auxiliaryparameter change=");
        String change = np.getWordMatchesIgnoringCase("increment decrement");
        np.matchIgnoreCase(";");

        MultiViewer viewer = (MultiViewer) getViewer();

        if (change.equalsIgnoreCase("increment")) {
            for (Iterator<TreeViewer> it = viewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
                TreeViewer treeViewer = it.next();
                String drawerKind = treeViewer.getDrawerKind();
                switch (drawerKind) {
                    case TreeViewer.INNERCIRCULAR_CLADOGRAM:
                        treeViewer.setInnerCircularLength(treeViewer.getInnerCircularLength() + 1);
                        treeViewer.recomputeEmbedding(false, true);
                        treeViewer.fitGraphToWindow();
                        break;
                    case TreeViewer.RADIAL_PHYLOGRAM:
                    case TreeViewer.RADIAL_CLADOGRAM:
                    case TreeViewer.CIRCULAR_CLADOGRAM:
                        if (treeViewer.getRadialAngle() < 100) {
                            treeViewer.setRadialAngle(treeViewer.getRadialAngle() + 1);
                            treeViewer.recomputeEmbedding(false, true);
                        }
                        break;
                    case TreeViewer.RECTANGULAR_PHYLOGRAM:
                    case TreeViewer.CIRCULAR_PHYLOGRAM:
                        treeViewer.setPhylogramPercentOffset(treeViewer.getPhylogramPercentOffset() + 1);
                        treeViewer.recomputeEmbedding(false, true);
                        break;
                }
            }
        } else {
            for (Iterator<TreeViewer> it = viewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
                TreeViewer treeViewer = it.next();
                String drawerKind = treeViewer.getDrawerKind();
                switch (drawerKind) {
                    case TreeViewer.INNERCIRCULAR_CLADOGRAM:
                        if (treeViewer.getInnerCircularLength() > 1) {
                            treeViewer.setInnerCircularLength(treeViewer.getInnerCircularLength() - 1);
                            treeViewer.recomputeEmbedding(false, true);
                            treeViewer.fitGraphToWindow();
                        }
                        break;
                    case TreeViewer.RADIAL_PHYLOGRAM:
                    case TreeViewer.RADIAL_CLADOGRAM:
                    case TreeViewer.CIRCULAR_CLADOGRAM:
                        if (treeViewer.getRadialAngle() > 1) {
                            treeViewer.setRadialAngle(treeViewer.getRadialAngle() - 1);
                            treeViewer.recomputeEmbedding(false, true);
                        }
                        break;
                    case TreeViewer.RECTANGULAR_PHYLOGRAM:
                    case TreeViewer.CIRCULAR_PHYLOGRAM:
                        treeViewer.setPhylogramPercentOffset(treeViewer.getPhylogramPercentOffset() - 1);
                        treeViewer.recomputeEmbedding(false, true);
                        break;
                }
            }
        }
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
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "auxiliaryparameter change={increment|decrement};";
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
