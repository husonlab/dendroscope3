/*
 *   IOFormat.java Copyright (C) 2020 Daniel H. Huson
 *
 *   (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dendroscope.io;

import dendroscope.core.TreeData;

import java.io.*;


/**
 * interface for input-output classe
 * Daniel Huson, 7.2007
 */
public interface IOFormat {
    /**
     * does this look like a file of the correct type?
     *
     * @param file
     * @return true, if correct type of file
     */
    public boolean isCorrectFileType(File file);

    /**
     * does this look like the first line of a file of the correct type
     *
     * @param aLine
     */
    public boolean isCorrectType(String aLine);

    /**
     * reads trees from a file
     *
     * @param file
     * @return trees or null
     * @throws IOException
     */
    public TreeData[] read(File file) throws IOException;

    /**
     * read trees
     *
     * @param r
     * @return trees or null
     * @throws IOException
     */
    public TreeData[] read(Reader r) throws IOException;

    /**
     * writes trees to a file
     *
     * @param file
     * @param trees
     * @throws IOException
     */
    public void write(File file, boolean internalNodeLabelsAreEdgeLabels, TreeData[] trees) throws IOException;

    /**
     * write trees
     *
     * @param w
     * @param trees
     * @throws IOException
     */
    public void write(Writer w, boolean internalNodeLabelsAreEdgeLabels, TreeData[] trees) throws IOException;

    /**
     * gets a description of the file type
     *
     * @return description
     */
    public String getDescription();

    /**
     * gets the default file extension of this format
     *
     * @return extension
     */
    public String getExtension();

    /**
     * gets the format name
     *
     * @return name
     */
    public String getName();

    /**
     * gets the filename filter
     *
     * @return file name filter
     */
    public FilenameFilter getFilenameFilter();

    /**
     * get the file filter
     *
     * @return file filter
     */
    public FileFilter getFileFilter();

    /**
     * get the file filter
     *
     * @return file filter
     */
    public javax.swing.filechooser.FileFilter getJFileFilter();
}
