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

package dendroscope.main;

import dendroscope.core.Director;
import dendroscope.core.Document;
import dendroscope.window.MultiViewer;
import jloda.gui.About;
import jloda.gui.director.ProjectManager;
import jloda.phylo.PhyloTree;
import jloda.util.Alert;
import jloda.util.ArgsOptions;
import jloda.util.Basic;
import jloda.util.ProgramProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.StringTokenizer;

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
        final boolean showSplash = !options.getOption("+s", "hideSplash", "Hide startup splash screen", true) && ProgramProperties.isUseGUI();
        options.done();

        if (silentMode) {
            Basic.hideSystemErr();
            Basic.hideSystemOut();
        }
        if (showVersion)
            System.err.println(ProgramProperties.getProgramVersion());

        DendroscopeProperties.initializeProperties(propertiesFile);
        DendroscopeProperties.initializeToolBar();
        DendroscopeProperties.initializeMenu();

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
                        go(multiViewer.getFrame(), true, false);

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
            if (!go(null, false, false)) {
                System.err.println("No license - please obtain license to use");
                System.exit(0);
            }

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

    // EVERYTHING BELOW HERE IS REGISTRATION CODE
    // at startup: ask=true show=false
    // in action: ask=true show=true
    // to check status: ask=false show=false

    /**
     * check for go
     *
     * @param parent
     * @param ask
     * @param show
     */
    public boolean go(JFrame parent, boolean ask, boolean show) {
        final String programName = "Dendroscope";
        final String url = "www.Dendroscope.org";

        boolean ok = false;
        try {
            String s = jloda.util.ProgramProperties.get("User", "");
            StringTokenizer tok = new StringTokenizer(s, ";");
            String n = tok.nextToken();
            String a = tok.nextToken();
            String e = tok.nextToken();
            String k = tok.nextToken();

            long sum = 10000000;
            for (int i = 0; i < programName.length(); i++)
                sum += (7 + i) * programName.charAt(i);
            for (int i = 0; i < n.length(); i++)
                sum += (70 + i) * n.charAt(i);
            for (int i = 0; i < a.length(); i++)
                sum += (70 + i) * a.charAt(i);
            for (int i = 0; i < e.length(); i++)
                sum += (700 + i) * e.charAt(i);
            if (k.equals(Long.toString(sum)) == false)
                throw new Exception();
            ok = true;
            if (!ask && !show)
                return true;
        } catch (Exception ex) {
            if (ask == false && !show)
                return false;
        }
        if (ask && !ok) {
            final JTextField name;
            final JTextField address;
            final JTextField email;
            final JTextField key;

            // registration dialog
            final JDialog frame = new JDialog(parent);
            frame.setModal(true);
            frame.setTitle("Registration for " + programName);

            if (parent != null) {
                frame.setLocationRelativeTo(parent);

            }

            frame.setSize(350, 300);
            Basic.centerDialogOnScreen(frame);
            frame.getContentPane().setLayout(new BorderLayout());
            JPanel main = new JPanel();
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
            main.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));


            frame.getContentPane().add(main, BorderLayout.CENTER);

            JPanel panel = new JPanel();
            panel.setBorder(BorderFactory.createEtchedBorder());
            GridLayout grid = new GridLayout();
            grid.setRows(4);
            grid.setColumns(1);
            panel.setLayout(grid);
            panel.add(new JLabel("Please register your copy of the program."));
            panel.add(new JLabel("A key is freely available from:"));
            panel.add(new JLabel(url));
            panel.add(new JLabel(" "));
            main.add(panel);

            JPanel middle = new JPanel();
            middle.setBorder(BorderFactory.createEtchedBorder());
            grid = new GridLayout();
            grid.setColumns(2);
            grid.setRows(5);
            middle.setLayout(grid);
            middle.add(new JLabel("Name:"));
            name = new JTextField();
            name.setPreferredSize(new Dimension(150, 20));
            middle.add(name);

            middle.add(new JLabel("Institute:"));
            address = new JTextField();
            middle.add(address);

            middle.add(new JLabel("Email:"));
            email = new JTextField();
            middle.add(email);


            middle.add(new JLabel(" "));
            middle.add(new JLabel(" "));

            middle.add(new JLabel("Key:"));
            key = new JTextField();
            middle.add(key);

            main.add(middle);


            JPanel buttons = new JPanel();
            buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
            JButton button = new JButton("Later");
            button.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent actionEvent) {
                    frame.setVisible(false);
                    frame.dispose();
                }
            });
            buttons.add(button);
            button = new JButton("Register now");
            button.addActionListener(new AbstractAction() {
                public void actionPerformed(ActionEvent actionEvent) {
                    try {
                        if (key.getText().trim().length() == 0)
                            throw new Exception("No key supplied");
                        else if (name.getText().trim().length() == 0)
                            throw new Exception("No name supplied");
                        else if (address.getText().trim().length() == 0)
                            throw new Exception("No address supplied");
                        else if (email.getText().trim().length() == 0)
                            throw new Exception("No email supplied");

                        String n = name.getText().trim();
                        String a = address.getText().trim();
                        String e = email.getText().trim();
                        String k = key.getText().trim();

                        long sum = 10000000;
                        for (int i = 0; i < programName.length(); i++)
                            sum += (7 + i) * programName.charAt(i);
                        for (int i = 0; i < n.length(); i++)
                            sum += (70 + i) * n.charAt(i);
                        for (int i = 0; i < a.length(); i++)
                            sum += (70 + i) * a.charAt(i);
                        for (int i = 0; i < e.length(); i++)
                            sum += (700 + i) * e.charAt(i);
                        if (k.equals(Long.toString(sum)) == false)
                            throw new Exception("Key doesn't match name, institute and email");
                        jloda.util.ProgramProperties.put("User",
                                name.getText() + ";" + address.getText().trim() + ";" + email.getText().trim() + ";" + key.getText().trim() + ";");
                        frame.setVisible(false);
                        frame.dispose();
                    } catch (Exception ex) {
                        new Alert(frame, "Registration failed: " + ex.getMessage() + "."
                                + "\nPlease obtain a key from\nwww.Dendroscope.org\nto unlock all features.");
                    }

                }
            });
            buttons.add(button);

            JPanel bottom = new JPanel();
            bottom.setBorder(BorderFactory.createEtchedBorder());
            bottom.setLayout(new BorderLayout());
            bottom.add(buttons, BorderLayout.EAST);
            main.add(bottom);
            frame.getContentPane().validate();

            frame.setVisible(true);
        } else if (show && ok) {
            try {
                String s = jloda.util.ProgramProperties.get("User", "");
                StringTokenizer tok = new StringTokenizer(s, ";");
                String n = tok.nextToken();
                String a = tok.nextToken();
                String e = tok.nextToken();
                tok.nextToken();
                new Alert(parent, "Registered to:\n"
                        + n + "\n" + a + "\n" + e + "\n");
            } catch (Exception ex) {
            }
        }
        return ok;
    }

}
