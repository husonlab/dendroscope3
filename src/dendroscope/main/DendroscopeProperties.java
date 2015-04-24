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

import jloda.export.ExportImageDialog;
import jloda.util.ProgramProperties;
import jloda.util.PropertiesListListener;
import jloda.util.ResourceManager;

import java.io.File;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * manages dendroscope properties, in cooperation with jloda.util.Properties
 *
 * @author huson
 *         Date: 11-Nov-2004
 */
public class DendroscopeProperties {
    public static boolean ALLOW_SPLITPANE = true;
    public static final boolean ALLOW_CONSENSUS = true;
    public static final boolean ALLOW_IMAGES = true;
    public static final boolean ALLOW_OPTIMIZE_NETWORKS = true;
    public static final boolean ALLOW_MINIMAL_NETWORKS = true;
    public static final boolean ALLOW_TANGLEGRAM = true;
    public static final boolean ALLOW_MULTIVIEWER = true;

    public static final String LASTTREE = "LastTree";
    public static final String IMAGE_DIRECTORY = "TaxonImageDirectory";
    public static final String IMAGE_HEIGHT = "TaxonImageHeight";
    public static final String TAXONOMY_PROFILE_FILE = "TaxonomyProfileFile";
    public static final String MULTIVIEWER_ROWS = "ViewerRows";
    public static final String MULTIVIEWER_COLS = "ViewerCols";

    /**
     * constructor
     */
    public DendroscopeProperties() {
    }

    /**
     * sets the program properties
     *
     * @param propertiesFile
     */
    public static void initializeProperties(String propertiesFile) {
        ProgramProperties.setProgramIcon(ResourceManager.getIcon("Dendroscope32.png"));
        ProgramProperties.setProgramVersion(Version.SHORT_DESCRIPTION);
        ProgramProperties.setProgramName(Version.NAME);
        ProgramProperties.setPropertiesFileName(propertiesFile);

        // first set all necessary defaults:
        ProgramProperties.put(ProgramProperties.OPENFILE, System.getProperty("user.dir"));
        ProgramProperties.put(ProgramProperties.SAVEFILE, System.getProperty("user.dir"));
        ProgramProperties.put(ProgramProperties.EXPORTFILE, System.getProperty("user.dir"));

        ProgramProperties.put(ProgramProperties.RECENTFILES, "");
        ProgramProperties.put(ProgramProperties.MAXRECENTFILES, 20);
        ProgramProperties.put(ExportImageDialog.GRAPHICSFORMAT, ".eps");
        ProgramProperties.put(ExportImageDialog.GRAPHICSDIR, System.getProperty("user.dir"));

        // then read in file to override defaults:
        ProgramProperties.load(propertiesFile);

        if (!ProgramProperties.get("Version", "").equals(Version.SHORT_DESCRIPTION)) {
            // System.err.println("Version has changed, resetting path to initialization files");
            // Version has changed, reset paths to taxonomy
            ProgramProperties.put("Version", Version.SHORT_DESCRIPTION);
            // make sure we find the initialization files:
        }

        if (ProgramProperties.get(ProgramProperties.SHOWVERSIONINTITLE, true))
            ProgramProperties.setProgramTitle(ProgramProperties.getProgramVersion());
        else
            ProgramProperties.setProgramTitle(ProgramProperties.getProgramName());
    }

    protected static void initializeToolBar() {
        ProgramProperties.put(ProgramProperties.TOOLBARITEMS, "Open...;Save;Print...;Export Image...;|;Format...;|;Expand Vertical;Contract Vertical;"
                + "Expand Horizontal;Contract Horizontal;|;Rotate Left;Rotate Right;|;Zoom To Fit;Fully Contract;Fully Expand;|;Toggle Magnifier;|;" +
                "Rectangular Phylogram;Rectangular Cladogram;Slanted Cladogram;Circular Phylogram;Circular Cladogram;Inner Circular Cladogram;Radial Phylogram;Radial Cladogram;|;Find/Replace...;" +
                "|;Ladderize Left;Ladderize Right;Ladderize Random;|;Reroot;Swap Subtree;");
    }

    /**
     * setup the initial menu
     */
    protected static void initializeMenu() {
        ProgramProperties.put("MenuBar.main", "File;Edit;Select;Options;Tree;View;Window;");
        ProgramProperties.put("Menu.File", "File;New...;" + (ALLOW_MULTIVIEWER ? "New MultiViewer...;" : "") + "|;Open...;@OpenRecent;|;Save;Save As...;Export...;|;Duplicate...;|;Export Image...;Print...;|;Close;Quit;");
        ProgramProperties.put("Menu.OpenRecent", "Recent Files;");
        ProgramProperties.put("Menu.Edit", "Edit;Copy;Paste;|;Find/Replace...;Find Again;|;Reroot;Swap Subtrees;Rotate Subtrees;Reorder Subtrees...;" + (ALLOW_TANGLEGRAM ? "@ChangeLeafOrder;" : "") + "|;Delete Taxa;|;Unlock Edge Lengths;|;Format...;");
        ProgramProperties.put("Menu.ChangeLeafOrder", "Change Leaf Order;Best for current tree...;Circular Order from Combined Trees...;Set Order...;");
        ProgramProperties.put("Menu.Select", "Select;Select All;Select Nodes;Select Edges;|;From Previous Window;|;Deselect All;Deselect Nodes;Deselect Edges;|;" +
                "Select Labeled Nodes;Select Leaves;Select Subtree;Select Induced Tree;Select Induced Network;Select Spanned Edges;|;Select Root;Select Non-Terminal;" + (ALLOW_CONSENSUS ? "Select Special;" : "") + "|;Invert Selection;|;Scroll To Selection;|;List Selected Taxa;");
        ProgramProperties.put("Menu.Options", "Options;" +
                "Collapse;Uncollapse;Uncollapse Subtree;|;Collapse Complement;Collapse At Level...;|;" +
                "Extract Subtree...;" + (ALLOW_IMAGES ? "|;Load Taxon Images...;Set Image Size...;@ImagePosition;" : "")
                + (ALLOW_CONSENSUS ? "|;Strict Consensus...;Loose Consensus...;Majority Consensus...;LSA Consensus...;Network Consensus...;" : "")
                + "|;Network for Multi-Labeled Tree...;");
        ProgramProperties.put("Menu.ImagePosition", "Image Position;North;South;East;West;Radial;");
        ProgramProperties.put("Menu.Tree", "Tree;Draw Rectangular Phylogram;Draw Rectangular Cladogram;|;Draw Slanted Cladogram;|;" +
                "Draw Circular Phylogram;Draw Circular Cladogram;Draw Inner Circular Cladogram;|;Draw Radial Phylogram;Draw Radial Cladogram;|;" +
                "Previous Tree;Next Tree;|;" +
                "Ladderize Left;Ladderize Right;Ladderize Random;");
        ProgramProperties.put("Menu.View", "View;Zoom To Fit;Fully Contract;Fully Expand;|;Use Magnifier;Magnify All Mode;|;Show Node Labels;Hide Node Labels;|;Show Edge Weights;Show Edge Labels;Hide Edge Labels;|;Sparse Labels;Radial Labels;|;Reposition Labels;");
        ProgramProperties.put("Menu.Window", "Window;About...;How to Cite...;Website...;|;Register...;|;Set Window Size...;|;Command-line Syntax;Execute Command...;Add Tree or Network...;|;Message Window...;");
    }

    /**
     * add a file to the recent files list
     *
     * @param file
     */
    public static void addRecentFile(File file) {
        int maxRecentFiles = ProgramProperties.get(ProgramProperties.MAXRECENTFILES, 20);
        StringTokenizer st = new StringTokenizer(ProgramProperties.get(ProgramProperties.RECENTFILES, ""), ";");
        int count = 1;
        java.util.List<String> recentFiles = new LinkedList<String>();
        String pathName = file.getAbsolutePath();
        recentFiles.add(pathName);
        while (st.hasMoreTokens()) {
            String next = st.nextToken();
            if (pathName.equals(next) == false) {
                recentFiles.add(next);
                if (++count == maxRecentFiles)
                    break;
            }
        }
        StringBuilder buf = new StringBuilder();
        for (String recentFile : recentFiles) buf.append(recentFile).append(";");
        ProgramProperties.put(ProgramProperties.RECENTFILES, buf.toString());
        notifyListChange(ProgramProperties.RECENTFILES);
    }

    /**
     * clears the list of recent files
     */
    public static void clearRecentFiles() {
        String str = ProgramProperties.get(ProgramProperties.RECENTFILES, "");
        if (str.length() != 0) {
            ProgramProperties.put(ProgramProperties.RECENTFILES, "");
            notifyListChange(ProgramProperties.RECENTFILES);
        }
    }

    static final java.util.List<PropertiesListListener> propertieslistListeners = new LinkedList<PropertiesListListener>();

    /**
     * notify listeners that list of values for the given name has changed
     *
     * @param name such as RecentFiles
     */
    public static void notifyListChange(String name) {
        java.util.List<String> list = new LinkedList<String>();
        StringTokenizer st = new StringTokenizer(ProgramProperties.get(name, ""), ";");
        while (st.hasMoreTokens()) {
            list.add(st.nextToken());
        }
        for (PropertiesListListener listener : propertieslistListeners) {
            if (listener.isInterested(name))
                listener.hasChanged(list);
        }
    }

    /**
     * add recent file listener
     *
     * @param listener
     */
    public static void addPropertiesListListener(PropertiesListListener listener) {
        if (propertieslistListeners.contains(listener) == false)
            propertieslistListeners.add(listener);
    }

    /**
     * remove recent file listener
     *
     * @param listener
     */
    public static void removePropertiesListListener(PropertiesListListener listener) {
        propertieslistListeners.remove(listener);
    }

    /**
     * gets the version of the program
     *
     * @return version
     */
    public static String getVersion() {
        if (Dendroscope.getApplication() == null || Dendroscope.getApplication().go(null, false, false))
            return Version.SHORT_DESCRIPTION;
        else {
            return Version.SHORT_DESCRIPTION + " - unregistered copy";
        }
    }

}


