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
    public void write(File file, TreeData[] trees) throws IOException;

    /**
     * write trees
     *
     * @param w
     * @param trees
     * @throws IOException
     */
    public void write(Writer w, TreeData[] trees) throws IOException;

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
