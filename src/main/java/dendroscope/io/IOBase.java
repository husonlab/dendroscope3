/*
 * IOBase.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.io;

import dendroscope.core.TreeData;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * base class for  io classes
 * Daniel Huson, 7.2007
 */
public abstract class IOBase extends javax.swing.filechooser.FileFilter implements FileFilter, FilenameFilter {
    int countNewNames = 0;

    // over write these constants
    public final String DESCRIPTION = "no description";
    public final String EXTENSION = ".txt";
    public final String NAME = "no name";

    /**
     * reads trees from a file
     *
     * @return trees
	 */
    public TreeData[] read(File file) throws IOException {
        return read(new FileReader(file));
    }

    /**
     * read trees
     *
     * @return trees
	 */
    public abstract TreeData[] read(Reader r0) throws IOException;

    /**
     * writes trees to a file
     *
	 */
    public void write(File file, boolean internalNodeLabelsAreEdgeLabels, TreeData[] trees) throws IOException {
        write(new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)), internalNodeLabelsAreEdgeLabels, trees);
    }

    /**
     * write trees
     *
	 */
    public abstract void write(Writer w0, boolean internalNodeLabelsAreEdgeLabels, TreeData[] trees) throws IOException;

    /**
     * creates a new unique tree name
     *
     * @return new unique tree name
     */
    public String createNewTreeName() {
        return "Tree" + (++countNewNames);
    }

    /**
     * @param f the file the extension is to be found
     * @return the extension as string (i.e. the substring beginning after the
     * last ".")
     */
    public static String getExtension(File f) {
        if (f != null) {
            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            if (i > 0 && i < filename.length() - 1) {
                return filename.substring(i + 1).toLowerCase();
            }
        }
        return null;
    }

    public boolean accept(File file, String string) {
        if (string != null)
            return accept(new File(file, string));
        else
            return accept(file);
    }

    /**
     * gets a description of the file type
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * gets the default file extension of this format
     *
     * @return extension
     */
    public String getExtension() {
        return EXTENSION;
    }

    /**
     * gets the format name
     *
     * @return name
     */
    public String getName() {
        return NAME;
    }

    /**
     * gets the filename filter
     *
     * @return file name filter
     */
    public FilenameFilter getFilenameFilter() {
        return this;
    }

    /**
     * get the file filter
     *
     * @return file filter
     */
    public FileFilter getFileFilter() {
        return this;
    }

    /**
     * get the file filter
     *
     * @return file filter
     */
    public javax.swing.filechooser.FileFilter getJFileFilter() {
        return this;
    }

}
