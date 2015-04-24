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

import dendroscope.core.Document;
import dendroscope.io.nexml.Nexml;
import jloda.util.Alert;
import jloda.util.Basic;
import jloda.util.TextFileFilter;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * manages input and output of data
 * Daniel Huson, 2008
 */
public class IOManager {
    /**
     * save the document
     *
     * @param doc
     * @param formatName
     * @param file
     */
    public void saveDocument(Document doc, String formatName, File file) {
        IOFormat format = createIOFormatForName(formatName);

        try {
            format.write(file, doc.getTrees());
        } catch (IOException e) {
            Basic.caught(e);
            new Alert("File NOT saved: " + e.getMessage());
        }
    }

    public void readDocument(Document doc, String formatName, File file) {
        IOFormat format = createIOFormatForName(formatName);

        try {

            doc.setTrees(format.read(file));
        } catch (IOException e) {
            Basic.caught(e);
            new Alert("Error opening file: " + e.getMessage());
        }
    }

    /**
     * gets an IOFormat object of the named type
     *
     * @param name
     * @return IOFormat object
     */
    public static IOFormat createIOFormatForName(String name) {
        if (name.equalsIgnoreCase(Nexus.NAME))
            return new Nexus();
        else if (name.equalsIgnoreCase(Dendro.NAME))
            return new Dendro();
        else if (name.equalsIgnoreCase(Nexml.NAME))
            return new Nexml();
        else
            return new Newick();
    }

    /**
     * gets an IOFormat object of file type
     *
     * @param file
     * @return IOFormat object
     */
    public static IOFormat createIOFormatForFile(File file) throws IOException {
        if (!file.exists())
            throw new IOException("File not found: " + file.getPath());
        if (!file.canRead())
            throw new IOException("Can't read file: " + file.getPath());

        IOFormat format = new Dendro();
        if (format.isCorrectFileType(file))
            return format;
        format = new Nexus();
        if (format.isCorrectFileType(file))
            return format;
        format = new Newick();
        if (format.isCorrectFileType(file))
            return format;
        format = new Nexml();
        if (format.isCorrectFileType(file))
            return format;
        return null;
    }

    /**
     * gets an IOFormat object of the correct type
     *
     * @param aLine
     * @return IOFormat object
     */
    public static IOFormat createIOFormatForFile(String aLine) {
        IOFormat format = new Dendro();
        if (format.isCorrectType(aLine))
            return format;
        format = new Nexus();
        if (format.isCorrectType(aLine))
            return format;
        format = new Newick();
        if (format.isCorrectType(aLine))
            return format;
        format = new Nexml();
        if (format.isCorrectType(aLine))
            return format;
        return null;
    }

    /**
     * gets all available formats
     *
     * @return formats as objects
     */
    public static String[] getAvailableFormats() {
        return new String[]{Newick.NAME, Nexus.NAME, Dendro.NAME, Nexml.NAME, Newick.NAME + "-no-weights", "TEXT"};
    }

    /**
     * gets a file name filter that accepts all supported file formats
     *
     * @return file name filter
     */
    public static FilenameFilter getFilenameFilter() {
        return new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (new Dendro()).getFilenameFilter().accept(dir, name)
                        || (new Newick()).getFilenameFilter().accept(dir, name)
                        || (new Nexus()).getFilenameFilter().accept(dir, name)
                        || (new Nexml()).getFilenameFilter().accept(dir, name)
                        || (new TextFileFilter()).accept(dir, name);
            }
        };
    }

    /**
     * gets a file filter that accepts all supported formats
     *
     * @return file filter
     */
    public static FileFilter getFileFilter() {
        return new FileFilter() {
            public boolean accept(File pathname) {
                return (new Dendro()).getFileFilter().accept(pathname)
                        || (new Newick()).getFileFilter().accept(pathname)
                        || (new Nexus()).getFileFilter().accept(pathname)
                        || (new Nexml()).getFileFilter().accept(pathname)
                        || (new TextFileFilter()).accept(pathname);
            }

            /**
             * The description of this filter. For example: "JPG and GIF Images"
             *
             * @see javax.swing.filechooser.FileView#getName
             */
            @Override
            public String getDescription() {
                return "Dendro, Nexus, Newick, NeXML or text file (*.dendro,nexus,newick,nexml,txt,text)";

            }
        };
    }
}
