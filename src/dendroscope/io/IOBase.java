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
     * @param file
     * @return trees
     * @throws java.io.IOException
     */
    public TreeData[] read(File file) throws IOException {
        return read(new FileReader(file));
    }

    /**
     * read trees
     *
     * @param r0
     * @return trees
     * @throws IOException
     */
    public abstract TreeData[] read(Reader r0) throws IOException;

    /**
     * writes trees to a file
     *
     * @param file
     * @param trees
     * @throws java.io.IOException
     */
    public void write(File file, TreeData[] trees) throws IOException {
        write(new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), "UTF-8")), trees);
    }

    /**
     * write trees
     *
     * @param w0
     * @param trees
     * @throws java.io.IOException
     */
    public abstract void write(Writer w0, TreeData[] trees) throws IOException;

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
