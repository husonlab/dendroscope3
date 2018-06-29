/**
 * Nexml.java 
 * Copyright (C) 2015 Daniel H. Huson
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
package dendroscope.io.nexml;

import dendroscope.core.Connectors;
import dendroscope.core.TreeData;
import dendroscope.io.IOBase;
import dendroscope.io.IOFormat;
import jloda.util.Basic;
import org.nexml.model.Document;
import org.nexml.model.DocumentFactory;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

/**
 * interface to NEXML format
 * Daniel Huson, 2.2011
 */
public class Nexml extends IOBase implements IOFormat {
    public final static String DESCRIPTION = "NeXML File (*.xml,*nexml)";
    public final static String EXTENSION = ".nexml";
    public final static String NAME = "NeXML";

    private final static String TAG = "<?xml";


    private Connectors connectors;

    public Connectors getConnectors() {
        return connectors;
    }

    public void setConnectors(Connectors connectors) {
        this.connectors = connectors;
    }

    /**
     * read trees
     *
     * @param reader
     * @return trees
     * @throws java.io.IOException
     */
    @Override
    public TreeData[] read(final Reader reader) throws IOException {
        InputStream stream = new InputStream() {
            public int read() throws IOException {
                return reader.read();
            }
        };
        Document document = DocumentFactory.safeParse(stream);
        if (document == null)
            throw new IOException("readNexml: failed to parse input");
        return ConvertNexmlDocToTreeData.apply(document, connectors);
    }

    /**
     * write trees
     *
     * @param writer
     * @param trees
     * @throws java.io.IOException
     */
    @Override
    public void write(Writer writer, TreeData[] trees) throws IOException {
        try {
            Document document = ConvertTreeDataToNexmlDoc.apply(trees, connectors);
            writer.write(document.getXmlString());
            writer.flush();
        } catch (ParserConfigurationException e) {
            Basic.caught(e);
            throw new IOException("Write FAILED: " + e.getMessage());
        }
    }

    @Override
    public boolean accept(File file) {
        if (file != null) {
            if (file.isDirectory()) return true;
            // Get the file extension
            try {
                String extension = getExtension(file);
                if (extension != null)
                    if (extension.equalsIgnoreCase("xml")
                            || extension.equalsIgnoreCase("nexml"))
                        return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * does this look like a file of the correct type?
     *
     * @param file
     * @return true, if correct type of file
     */
    public boolean isCorrectFileType(File file) {
        try {
            return isCorrectType(Basic.toString(Basic.getFirstBytesFromFile(file, TAG.length())));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * does this look like the first line of the a file of the correct type?
     *
     * @param aLine
     * @return true, if correct type of string
     */
    public boolean isCorrectType(String aLine) {
        return aLine != null && aLine.startsWith(TAG);
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
}
