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

import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * bring window to front
 * Daniel Huson, 2.2011
 */
public class ToFrontCommand extends CommandBase implements ICommand {
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        if (getViewer() != null) {
            getViewer().getFrame().setVisible(true);
            getViewer().getFrame().toFront();
        } else if (getParent() != null && getParent() instanceof Window) {
            ((Window) getParent()).setVisible(true);
            ((Window) getParent()).toFront();
        }
    }


    public boolean isApplicable() {
        return getViewer() != null || (getParent() != null && getParent() instanceof Window);
    }

    public boolean isCritical() {
        return false;
    }

    public String getSyntax() {
        return "tofront;";
    }

    public void actionPerformed(ActionEvent event) {
        execute(getSyntax());
    }

    public String getName() {
        return "To Front";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getDescription() {
        return "Bring window to front";
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

}

