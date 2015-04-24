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

import dendroscope.embed.LayoutOptimizerManager;
import jloda.gui.commands.ICheckBoxCommand;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * choose embedding algorithm
 * Daniel Huson, 7.2010
 */
public class LayoutOptimizer2009Command extends LayoutNoOptimizerCommand implements ICheckBoxCommand {
    public String getSyntax() {
        return null;
    }

    public boolean isSelected() {
        return multiViewer.getEmbedderName().equalsIgnoreCase(LayoutOptimizerManager.ALGORITHM2009);
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "2009 Algorithm";
    }

    public String getAltName() {
        return "Layout Optimizer 2009";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Optimize embedding of networks using the algorithm\n" +
                "described in (Huson 2009)";
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
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        execute("set layouter=" + LayoutOptimizerManager.ALGORITHM2009 + ";");
    }
}
