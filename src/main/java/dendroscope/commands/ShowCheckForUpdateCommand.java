/*
 * ShowCheckForUpdateCommand.java Copyright (C) 2026 Daniel H. Huson
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


import dendroscope.main.Version;
import javafx.application.Platform;
import jloda.fx.service.UpdateService;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirector;
import jloda.swing.director.ProjectManager;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.CountDownLatch;

/**
 * check for updates
 * <p>
 * Releases are published on GitHub. The installers bundle the github-updater library, which provides the
 * UpdateService that compares the running version with the manifest of the latest release and offers to download
 * and install a newer one. When the program is not run from an installer (for example from Maven or an IDE) no such
 * service is present and this command is disabled.
 * <p>
 * Daniel Huson, 6.2010, 11.2026
 */
public class ShowCheckForUpdateCommand extends CommandBase implements ICommand {
    final public static String NAME = "Check For Updates...";

    private static UpdateService updateService;

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
        return ResourceManager.getIcon("sun/About16.gif");
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
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
    }


    public void actionPerformed(ActionEvent ev) {
        try {
            // the update dialog is a JavaFX dialog, so the JavaFX toolkit must be running in this Swing program
            ensureJavaFXToolkit();
            getUpdateService().checkForUpdates(null, Version.HOME_URL, Version.NAME, Version.VERSION);
        } catch (Exception ex) {
            Basic.caught(ex);
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
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        if (getUpdateService().disabledProperty().get())
            return false;
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

    /**
     * the update service of the github-updater library, if it is on the class or module path, otherwise a disabled one
     */
    private static UpdateService getUpdateService() {
        if (updateService == null)
            updateService = UpdateService.get();
        return updateService;
    }

    /**
     * starts the JavaFX toolkit, unless it is already running
     */
    private static void ensureJavaFXToolkit() {
        Platform.setImplicitExit(false); // closing the update dialog must not shut the toolkit down
        try {
            var latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await();
        } catch (IllegalStateException ignored) {
            // toolkit is already running
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
