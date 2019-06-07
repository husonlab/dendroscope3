/**
 * DendroscopeProperties.java 
 * Copyright (C) 2019 Daniel H. Huson
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

import jloda.swing.export.ExportImageDialog;
import jloda.swing.util.ResourceManager;
import jloda.util.ProgramProperties;
import jloda.util.PropertiesListListener;

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
    public static final boolean ALLOW_IMAGES = true;
    public static final boolean ALLOW_OPTIMIZE_NETWORKS = true;
    public static final boolean ALLOW_MINIMAL_NETWORKS = true;

    public static final String LASTTREE = "LastTree";
    public static final String IMAGE_DIRECTORY = "TaxonImageDirectory";
    public static final String IMAGE_HEIGHT = "TaxonImageHeight";
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
            return Version.SHORT_DESCRIPTION;
    }

}


