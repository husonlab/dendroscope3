/*
 * Dendroscope.java Copyright (C) 2023 Daniel H. Huson
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
import jloda.phylo.PhyloTree;
import jloda.swing.director.ProjectManager;
import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ResourceManager;
import jloda.swing.window.About;
import jloda.util.Basic;
import jloda.util.Pair;
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
	 */
    public static void main(String[] args) throws Exception {
        ResourceManager.getClassLoadersAndRoots().add(new Pair<>(Dendroscope.class, "dendroscope.resources"));

        PhyloTree.WARN_HAS_MULTILABELS = false;
        About.setVersionStringOffset(20, 20);
        About.setAbout("Dendroscope3-splash.png", Version.SHORT_DESCRIPTION, JDialog.DISPOSE_ON_CLOSE);

        try {
            //run application
            application = new Dendroscope();
            application.parseArguments(args);

        } catch (Throwable th) {
            //catch any exceptions and the like that propagate up to the top level
			System.err.println("Dendroscope fatal error:" + "\n" + th);
			Basic.caught(th);
        }
    }


    /**
     * parse commandline arguments
     *
	 */
    public void parseArguments(String[] args) throws Exception {
        ResourceManager.insertResourceRoot(Dendroscope.class);
        Basic.startCollectionStdErr();

        ProgramProperties.setProgramName(Version.NAME);
        ProgramProperties.setProgramVersion(Version.SHORT_DESCRIPTION);

        final var options = new ArgsOptions(args, this, "Program for rooted phylogenetic trees and networks");
        options.setAuthors("Daniel H. Huson, with some contributions from other authors");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2023  Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.\n" +
                           "This is free software, licensed under the terms of the GNU General Public License, Version 3.");
        options.setVersion(ProgramProperties.getProgramVersion());

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
        final boolean silentMode = options.getOption("-S", "silentMode", "Silent mode", false);
        Basic.setDebugMode(options.getOption("-d", "debug", "Debug mode", false));
        final boolean showSplash = (!options.getOption("+s", "hideSplash", "Hide startup splash screen", false)) && ProgramProperties.isUseGUI();

        ProgramProperties.setConfirmQuit(options.getOption("-q", "confirmQuit", "Confirm quit on exit", ProgramProperties.isConfirmQuit()));
        options.done();

        System.err.println("Java version: " + System.getProperty("java.version"));
        if (silentMode) {
            Basic.stopCollectingStdErr();
            Basic.hideSystemErr();
            Basic.hideSystemOut();
        }

        DendroscopeProperties.initializeProperties(propertiesFile);

        if (ProgramProperties.isUseGUI())  // run in GUI mode
		{
			System.setProperty("user.dir", System.getProperty("user.home"));
			if (showSplash)
				About.getAbout().showAbout();
			SwingUtilities.invokeLater(() -> {
				try {
					final Director dir = Director.newProject(1, 1);
					final MultiViewer multiViewer = (MultiViewer) dir.getViewerByClass(MultiViewer.class);

					if (showMessageWindow)
						multiViewer.getCommandManager().execute("show messageWindow;");
					else
						System.err.println(Basic.stopCollectingStdErr());

					DendroscopeProperties.notifyListChange(ProgramProperties.RECENTFILES);
					dir.getMainViewer().updateView(Director.ALL);
					if (initCommand != null && initCommand.length() > 0)
						dir.execute(initCommand + ";", dir.getMainViewer().getCommandManager());
				} catch (Exception e) {
					Basic.caught(e);
				}
			});
        } else // non-gui mode
        {
            System.err.println(Basic.stopCollectingStdErr());

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
						SwingUtilities.invokeAndWait(() -> dir.executeImmediately(finalCommand + ";", multiViewer.getCommandManager()));
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
