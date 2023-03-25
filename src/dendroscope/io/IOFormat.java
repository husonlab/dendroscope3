/*
 *   IOFormat.java Copyright (C) 2023 Daniel H. Huson
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
	 * @return true, if correct type of file
	 */
	boolean isCorrectFileType(File file);

	/**
	 * does this look like the first line of a file of the correct type
	 */
	boolean isCorrectType(String aLine);

    /**
	 * reads trees from a file
	 *
	 * @return trees or null
	 */
	TreeData[] read(File file) throws IOException;

	/**
	 * read trees
	 *
	 * @return trees or null
	 */
	TreeData[] read(Reader r) throws IOException;

	/**
	 * writes trees to a file
	 *
	 */
	void write(File file, boolean internalNodeLabelsAreEdgeLabels, TreeData[] trees) throws IOException;

	/**
	 * write trees
	 *
	 */
	void write(Writer w, boolean internalNodeLabelsAreEdgeLabels, TreeData[] trees) throws IOException;

	/**
	 * gets a description of the file type
	 *
	 * @return description
	 */
	String getDescription();

	/**
	 * gets the default file extension of this format
	 *
	 * @return extension
	 */
	String getExtension();

	/**
	 * gets the format name
	 *
	 * @return name
	 */
	String getName();

	/**
	 * gets the filename filter
	 *
	 * @return file name filter
	 */
	FilenameFilter getFilenameFilter();

	/**
	 * get the file filter
	 *
	 * @return file filter
	 */
	FileFilter getFileFilter();

	/**
     * get the file filter
	 *
	 * @return file filter
	 */
	javax.swing.filechooser.FileFilter getJFileFilter();
}
