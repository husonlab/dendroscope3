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

import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * select no panels
 * Daniel Huson, 5.2010
 */
public class SelectNoPanelsCommand extends SelectPanelsCommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */

    public String getName() {
        return "No Panels";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */

    public String getDescription() {
        return "Deselect all panels";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */

    public ImageIcon getIcon() {
        return super.getIcon();
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */

    public KeyStroke getAcceleratorKey() {
        return super.getAcceleratorKey();
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */

    public void apply(NexusStreamParser np) throws Exception {
        super.apply(np);
    }

    /**
     * action to be performed
     *
     * @param ev
     */

    public void actionPerformed(ActionEvent ev) {
        execute("select panels=none;");
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */

    public String getSyntax() {
        return null;
    }
}
