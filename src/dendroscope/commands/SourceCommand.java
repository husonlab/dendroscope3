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

import dendroscope.io.IOManager;
import jloda.gui.ChooseFileDialog;
import jloda.gui.commands.ICommand;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * read commands from source
 * Daniel Huson, 4.2011
 */
public class SourceCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Source";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Reads commands from a source file";
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
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("source file=");
        String fileName = np.getWordFileNamePunctuation();
        np.matchIgnoreCase(";");

        BufferedReader r = new BufferedReader(new FileReader(fileName));
        StringBuilder buf = new StringBuilder();
        String aLine;
        while ((aLine = r.readLine()) != null) {
            buf.append(aLine).append("\n");
        }
        r.close();
        ProgramProperties.put("SourceFile", fileName);
        executeImmediately(buf.toString());
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
     * get command-line syntax. First two tokens are used to identify the command
     *
     * @return usage
     */
    public String getSyntax() {
        return "source file=<filename>;";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        String lastOpenFile = ProgramProperties.get("SourceFile", "commands.txt");
        File file = ChooseFileDialog.chooseFileToOpen(getViewer().getFrame(), new File(lastOpenFile), IOManager.getFileFilter(), IOManager.getFilenameFilter(), ev, "Open command source file");
        if (file != null)
            execute("source file='" + file.getPath() + "';");
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
        return true;
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
