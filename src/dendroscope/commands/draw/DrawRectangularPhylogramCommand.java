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

package dendroscope.commands.draw;

import dendroscope.window.TreeViewer;
import jloda.gui.commands.ICheckBoxCommand;
import jloda.util.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Iterator;

/**
 * set the retangular phylogram drawer
 * Daniel Huson, DATE
 */
public class DrawRectangularPhylogramCommand extends DrawCommand implements ICheckBoxCommand {
    /**
     * get command-line syntax
     *
     * @return usage
     */
    public String getSyntax() {
        return "set drawer=" + TreeViewer.RECTANGULAR_PHYLOGRAM + ";";
    }

    /**
     * this is currently selected?
     *
     * @return selected
     */
    public boolean isSelected() {
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            if (viewer.getDrawerKind().equals(TreeViewer.RECTANGULAR_PHYLOGRAM))
                return true;
        }
        return false;
    }

    /**
     * set the selected status of this command
     *
     * @param selected
     */
    public void setSelected(boolean selected) {
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */

    public String getName() {
        return "Draw Rectangular Phylogram";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */

    public String getDescription() {
        return "Draw tree or network as rectangular phylogram";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("RectangularPhylogram16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_1,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    /**
     * action to be performed
     *
     * @param ev
     */

    public void actionPerformed(ActionEvent ev) {
        execute("set drawer=" + TreeViewer.RECTANGULAR_PHYLOGRAM + ";");
    }
}
