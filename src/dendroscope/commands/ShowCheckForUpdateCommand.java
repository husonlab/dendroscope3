package dendroscope.commands;
/**
 * ShowCheckForUpdateCommand.java
 * Copyright (C) 2015 Daniel H. Huson
 * <p/>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


import com.install4j.api.launcher.ApplicationLauncher;
import com.install4j.api.update.ApplicationDisplayMode;
import com.install4j.api.update.UpdateChecker;
import com.install4j.api.update.UpdateDescriptor;
import com.install4j.api.update.UpdateDescriptorEntry;
import dendroscope.window.MultiViewer;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.gui.director.IDirector;
import jloda.gui.director.ProjectManager;
import jloda.util.Alert;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * show the message window
 * Daniel Huson, 6.2010
 */
public class ShowCheckForUpdateCommand extends CommandBase implements ICommand {
    final public static String NAME = "Check For Updates...";

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return NAME;
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Check for updates";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/About16.gif");
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
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {

        ApplicationDisplayMode applicationDisplayMode = ProgramProperties.isUseGUI() ? ApplicationDisplayMode.GUI : ApplicationDisplayMode.CONSOLE;
        UpdateDescriptor updateDescriptor;
        try {
            updateDescriptor = UpdateChecker.getUpdateDescriptor("http://www-ab.informatik.uni-tuebingen.de/data/software/dendroscope/download/updates.xml", applicationDisplayMode);
        } catch (Exception e) {
            Basic.caught(e);
            new Alert(MultiViewer.getLastActiveFrame(), "Installed version is up-to-date");
            return;
        }
        if (updateDescriptor.getEntries().length > 0) {
            if (!ProgramProperties.isUseGUI()) {
                UpdateDescriptorEntry entry = updateDescriptor.getEntries()[0];
                new Alert(MultiViewer.getLastActiveFrame(), "New version available: " + entry.getNewVersion()
                        + "\nPlease download from: http://www-ab.informatik.uni-tuebingen.de/data/software/dendroscope/download/");
                return;
            }
        } else {
            new Alert(MultiViewer.getLastActiveFrame(), "Installed version is up-to-date");
            return;
        }


        // This will return immediately if you call it from the EDT,
// otherwise it will block until the installer application exits
        ApplicationLauncher.launchApplicationInProcess("1691242312", null, new ApplicationLauncher.Callback() {
            public void exited(int exitValue) {
                //TODO add your code here (not invoked on event dispatch thread)
            }

            public void prepareShutdown() {
                ProgramProperties.store();
            }
        }, ApplicationLauncher.WindowMode.FRAME, null);
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
        for (IDirector dir : ProjectManager.getProjects()) {
            if (dir.getDirty())
                return false;
        }
        return true;
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return null;
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