/*
 * ShowReplaceDialogCommand.java Copyright (C) 2023 Daniel H. Huson
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

import jloda.swing.commands.ICommand;
import jloda.swing.director.IViewerWithFindToolBar;
import jloda.swing.find.FindToolBar;
import jloda.swing.util.Alert;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * command Daniel Huson, 5.2012
 */
public class ShowReplaceDialogCommand extends CommandBaseMultiViewer implements ICommand {

    public boolean isSelected() {
        if (!(getViewer() instanceof IViewerWithFindToolBar))
            return false;
        IViewerWithFindToolBar viewerWithFindToolBar = (IViewerWithFindToolBar) getViewer();
        return viewerWithFindToolBar.isShowFindToolBar() && viewerWithFindToolBar.getSearchManager().getFindDialogAsToolBar().isShowReplaceBar();
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Replace...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Open the replace toolbar";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Replace16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    /**
     * parses the given command and executes it
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("show replacetoolbar=");
        boolean show = np.getBoolean();
        np.matchIgnoreCase(";");

        if (getViewer() instanceof IViewerWithFindToolBar) {
            IViewerWithFindToolBar viewer = (IViewerWithFindToolBar) getViewer();
            viewer.setShowFindToolBar(show);
            FindToolBar findToolBar = viewer.getSearchManager().getFindDialogAsToolBar();
            findToolBar.setShowReplaceBar(show);
        } else {
            new Alert(getViewer().getFrame(), "Find/Replace not implemented for this type of window");
        }
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return false;
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "show replacetoolbar={true|false};";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return getViewer() instanceof IViewerWithFindToolBar;
    }

    /**
     * action to be performed
     *
	 */
    @Override
    public void actionPerformed(ActionEvent ev) {
        executeImmediately("show replacetoolbar=" + (!isSelected() + ";"));
    }
}



