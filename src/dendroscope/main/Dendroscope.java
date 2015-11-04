/**
 * Dendroscope.java 
 * Copyright (C) 2015 Daniel H. Huson
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
package dendroscope.main;

import dendroscope.core.Director;
import dendroscope.core.Document;
import dendroscope.window.MultiViewer;
import jloda.gui.About;
import jloda.gui.director.ProjectManager;
import jloda.phylo.PhyloTree;
import jloda.util.ArgsOptions;
import jloda.util.Basic;
import jloda.util.ProgramProperties;

import javax.swing.*;
import java.io.*;

/**
 * Dendroscope main program
 * Daniel Huson, 1.2007
 */
public class Dendroscope {
    protected static Dendroscope application;

    /**
     * launches Dendroscope
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        PhyloTree.setWarnMultiLabeled(false);
        About.setVersionStringOffset(20, 20);
        About.setAbout("resources.images", "splash.jpg", Version.SHORT_DESCRIPTION, JDialog.DISPOSE_ON_CLOSE);

        if (System.getProperty("mrj.version") == null)
            System.setProperty("mrj.version", "3.1"); // browser launcher requires that this is set

        try {
            //run application
            application = new Dendroscope();
            application.parseArguments(args);

        } catch (Throwable th) {
            //catch any exceptions and the like that propagate up to the top level
            System.err.println("Dendroscope fatal error:" + "\n" + th.toString());
            Basic.caught(th);
        }
    }


    /**
     * parse commandline arguments
     *
     * @param args
     * @throws Exception
     */
    public void parseArguments(String args[]) throws Exception {
        ProgramProperties.setProgramName(Version.SHORT_DESCRIPTION);

        final ArgsOptions options = new ArgsOptions(args, this, "Program for rooted phylogenetic trees and networks");
        options.setVersion(Version.SHORT_DESCRIPTION);

        options.comment("Mode:");
        ProgramProperties.setUseGUI(!options.getOption("-g", "--commandLineMode", "Run program in command-line mode", false) && !options.isDoHelp());
        if (!ProgramProperties.isUseGUI())
            Basic.sendSystemErrToSystemOut();

        options.comment("Commands:");
        final String initCommand = options.getOption("-x", "execute", "Command to execute at startup (do not use for multiple commands)", "");
        final String commandFileName = options.getOption("-c", "commandFile", "File of commands to execute in command-line mode", "");

        options.comment("Configuration:");
        final boolean quitOnExceptionInNonGUIMode = options.getOption("-E", "quitOnException", "Quit if exception thrown in command-line mode", false);
        String defaultPreferenceFile;
        if (ProgramProperties.isMacOS())
            defaultPreferenceFile = System.getProperty("user.home") + "/Library/Preferences/Dendroscope.def";
        else
            defaultPreferenceFile = System.getProperty("user.home") + File.separator + ".Dendroscope.def";
        final String propertiesFile = options.getOption("-p", "propertiesFile", "Properties file", defaultPreferenceFile);

        final boolean showMessageWindow = !options.getOption("+w", "hideMessageWindow", "Hide message window", false);
        final boolean showVersion = options.getOption("-V", "version", "Show version string", false);
        final boolean silentMode = options.getOption("-S", "silentMode", "Silent mode", false);
        Basic.setDebugMode(options.getOption("-d", "debug", "Debug mode", false));
        final boolean showSplash = (!options.getOption("+s", "hideSplash", "Hide startup splash screen", false)) && ProgramProperties.isUseGUI();
        options.done();

        if (silentMode) {
            Basic.hideSystemErr();
            Basic.hideSystemOut();
        }
        if (showVersion) {
            System.err.println(ProgramProperties.getProgramVersion());
            System.err.println("Java version: " + System.getProperty("java.version"));
        }

        DendroscopeProperties.initializeProperties(propertiesFile);

        if (ProgramProperties.isUseGUI())  // run in GUI mode
        {
            System.setProperty("user.dir", System.getProperty("user.home"));
            if (showSplash)
                About.getAbout().showAbout();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        Director dir = Director.newProject(1, 1);
                        MultiViewer multiViewer = (MultiViewer) dir.getViewerByClass(MultiViewer.class);

                        if (showMessageWindow)
                            multiViewer.getCommandManager().execute("show messageWindow;");

                        DendroscopeProperties.notifyListChange(ProgramProperties.RECENTFILES);
                        dir.getMainViewer().updateView(Director.ALL);
                        if (initCommand != null && initCommand.length() > 0)
                            dir.execute(initCommand + ";", dir.getMainViewer().getCommandManager());
                    } catch (Exception e) {
                        Basic.caught(e);
                    }
                }
            });
        } else // non-gui mode
        {
            final Document doc = new Document();
            final Director dir = new Director(doc);
            dir.setID(ProjectManager.getNextID());
            final MultiViewer multiViewer = new MultiViewer(dir, 1, 1);
            ProjectManager.addProject(dir, multiViewer);

            // need to set up size of figure. Don't quite know how this all works...
            /*
            Dimension size = new Dimension(500, 500);
            multiViewer.getFrame().setSize(size);
            TreeViewer viewer = multiViewer.getTreeGrid().getViewerByRank(0);
            viewer.setSize(size);
            viewer.setPreferredSize(size);
            viewer.getScrollPane().setSize(size);
            viewer.trans.setCoordinateRect(0, 0, size.width, size.height);
            multiViewer.getFrame().setSize(size);
            viewer.trans.fireHasChanged();
            */

            dir.executeImmediately("version;");

            if (initCommand != null && initCommand.length() > 0) {
                boolean ok = dir.executeImmediately(initCommand + ";");
                if (!ok && quitOnExceptionInNonGUIMode) {
                    System.err.println("Quit on exception");
                    System.exit(1);
                }
            }

            final LineNumberReader inp;
            final boolean fromFile = (commandFileName.length() > 0);
            if (fromFile)
                inp = new LineNumberReader(new BufferedReader(new FileReader(commandFileName)));
            else
                inp = new LineNumberReader(new BufferedReader(new InputStreamReader(System.in)));
            boolean inMultiLineMode = false;
            String command = "";
            while (true) // process commands from standard input:
            {
                if (!fromFile) {
                    if (!inMultiLineMode)
                        System.out.print("DENDROSCOPE> ");
                    else
                        System.out.print("+ ");
                    System.out.flush();
                }
                try {
                    String aline = inp.readLine();
                    if (aline == null)
                        break;
                    if (aline.equals("\\")) {
                        inMultiLineMode = !inMultiLineMode;
                    } else
                        command += aline;
                    if (!inMultiLineMode && command.length() > 0) {
                        final String finalCommand = command;
                        SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                dir.executeImmediately(finalCommand + ";", multiViewer.getCommandManager());
                            }
                        });
                        System.out.flush();
                        System.err.flush();
                        command = "";
                    }
                } catch (Exception ex) {
                    System.err.println(command + ": failed");
                    Basic.caught(ex);
                    command = "";
                    if (quitOnExceptionInNonGUIMode) {
                        System.err.println("Quit on exception");
                        System.exit(1);
                    }
                }
            }
        }
    }

    static public Dendroscope getApplication() {
        return application;
    }
}
