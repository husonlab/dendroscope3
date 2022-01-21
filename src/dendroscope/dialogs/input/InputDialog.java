/*
 * InputDialog.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.dialogs.input;

import dendroscope.core.Director;
import dendroscope.main.DendroscopeProperties;
import dendroscope.main.Version;
import dendroscope.window.GUIConfiguration;
import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IDirector;
import jloda.swing.director.ProjectManager;
import jloda.swing.util.RememberingComboBox;
import jloda.swing.window.MenuBar;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Command input window
 * Daniel Huson, 5.2011
 */
public class InputDialog extends JFrame implements IDirectableViewer {
    private boolean uptoDate = true;
    private final boolean locked = false;
    private Director dir;
    private CommandManager commandManager;
    final private MenuBar menuBar;
    final private RememberingComboBox inputCBox;
    final private AbstractAction applyAction;
    final private AbstractAction applyToAllAction;

    private static InputDialog instance = null;

    /**
     * constructor
     *
     * @param dir
     */
    public InputDialog(Director dir) {
        this.dir = dir;
        commandManager = dir.getCommandManager();

        setTitle();

        menuBar = new MenuBar(this, GUIConfiguration.getMenuConfiguration(), commandManager);
        setJMenuBar(menuBar);
        DendroscopeProperties.addPropertiesListListener(menuBar.getRecentFilesListener());
        DendroscopeProperties.notifyListChange(ProgramProperties.RECENTFILES);
        ProjectManager.addAnotherWindowWithWindowMenu(dir, menuBar.getWindowMenu());

        setSize(600, 100);
        setLocationRelativeTo(dir.getMainViewer().getFrame());

        inputCBox = new RememberingComboBox();

        inputCBox.addItemsFromString(ProgramProperties.get(ProgramProperties.LASTCOMMAND, ""), "%%%");

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());

        topPanel.add(new JLabel("Enter command(s) to execute:"), BorderLayout.NORTH);

        inputCBox.setAction(getInput());
        topPanel.add(inputCBox, BorderLayout.CENTER);

        getContentPane().add(topPanel);

        AbstractAction closeAction = new AbstractAction("Close") {
            public void actionPerformed(ActionEvent actionEvent) {
                InputDialog.this.setVisible(false);
            }
        };
        closeAction.putValue(AbstractAction.SHORT_DESCRIPTION, "Close this dialog");


        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(new JButton(closeAction));

        applyToAllAction = new AbstractAction("Apply to Every Tree in File") {
            public void actionPerformed(ActionEvent actionEvent) {
                inputCBox.getCurrentText(true);
                String command = getCommand();
                if (command != null && command.length() > 0) {
                    if (!command.startsWith("!"))
                        getDir().executeImmediately("show messagewindow;", getCommandManager());
                    else
                        command = command.substring(1, command.length());
                    if (!command.endsWith(";"))
                        command += ";";
                    command = command.trim();
                    if (command.length() > 0) {
                        ProgramProperties.put(ProgramProperties.LASTCOMMAND, inputCBox.getItemsAsString(20, "%%%"));
                        if (!command.contains("apply-all-"))
                            getDir().execute("apply-all-begin " + command + " apply-all-end;", getCommandManager());
                    }
                }
            }
        };
        applyToAllAction.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply command to all trees in file, each separately. Use with care.");
        bottomPanel.add(new JButton(applyToAllAction));

        applyAction = new AbstractAction("Apply") {
            public void actionPerformed(ActionEvent actionEvent) {
                inputCBox.getCurrentText(true);
                String command = getCommand();
                if (command != null && command.length() > 0) {
                    if (!command.startsWith("!"))
                        getDir().executeImmediately("show messagewindow;", getCommandManager());
                    else
                        command = command.substring(1, command.length());
                    if (!command.endsWith(";"))
                        command += ";";
                    command = command.trim();
                    if (command.length() > 0) {
                        ProgramProperties.put(ProgramProperties.LASTCOMMAND, inputCBox.getItemsAsString(20, "%%%"));
                        getDir().execute(command, getCommandManager());
                    }
                }
            }
        };
        applyAction.putValue(AbstractAction.SHORT_DESCRIPTION, "Apply command to all (selected) trees currently visible.");
        JButton applyButton = new JButton(applyAction);
        bottomPanel.add(applyButton);
        getRootPane().setDefaultButton(applyButton);

        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        getFrame().addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent event) {
                if (getDir().getMainViewer().isLocked())
                    lockUserInput();
                else {
                    updateView(IDirector.ALL);
                    unlockUserInput();
                }
            }
        });
        updateView(IDirector.ALL);
        setVisible(true);
    }

    public boolean isUptoDate() {
        return uptoDate;
    }

    public JFrame getFrame() {
        return this;
    }

    public void updateView(String what) {
        setTitle();
        commandManager.updateEnableState();
    }

    public void lockUserInput() {
        if (!isLocked()) {
            applyAction.setEnabled(false);
            applyToAllAction.setEnabled(false);
            commandManager.setEnableCritical(false);
        }
    }

    public void unlockUserInput() {
        applyAction.setEnabled(true);
        applyToAllAction.setEnabled(true);
        commandManager.setEnableCritical(true);
    }

    public void destroyView() throws CanceledException {
        DendroscopeProperties.removePropertiesListListener(menuBar.getRecentFilesListener());
        setVisible(false);
        dir.removeViewer(this);
        dispose();
    }

    public void setUptoDate(boolean flag) {
        uptoDate = flag;
    }

    /**
     * set the title of the window
     */
    public void setTitle() {
        String newTitle = "Command input  - " + getDir().getDocument().getTitle() + " - " + Version.NAME;

        if (getFrame().getTitle().equals(newTitle) == false) {
            getFrame().setTitle(newTitle);
            ProjectManager.updateWindowMenus();
        }
    }

    public Director getDir() {
        return dir;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * get the current command
     *
     * @return command
     */
    public String getCommand() {
        return inputCBox.getCurrentText(false).trim();
    }

    private AbstractAction input;

    public AbstractAction getInput() {
        AbstractAction action = input;
        if (action != null) return action;

        action = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                updateView(IDirector.ALL);
            }
        };
        action.putValue(AbstractAction.NAME, "Command input");
        action.putValue(AbstractAction.SHORT_DESCRIPTION, "Execute this command");
        return input = action;
    }

    public RememberingComboBox getInputCBox() {
        return inputCBox;
    }

    public static InputDialog getInstance() {
        return instance;
    }

    public static void setInstance(InputDialog instance) {
        InputDialog.instance = instance;
    }

    public void setViewer(Director dir) {
        this.dir = dir;
        this.commandManager = dir.getCommandManager();
        setTitle();
        if (!dir.isLocked() && dir.getMainViewer().isLocked())
            lockUserInput();
    }

    /**
     * is viewer currently locked?
     *
     * @return true, if locked
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * get the name of the class
     *
     * @return class name
     */
    @Override
    public String getClassName() {
        return "InputDialog";
    }
}
