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
        boolean result = false;
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(file));
            if (isCorrectType(r.readLine()))
                result = true;
        } catch (Exception e) {
        }
        try {
            if (r != null)
                r.close();
        } catch (IOException e) {
        }
        return result;
    }

    /**
     * does this look like the first line of a file of the correct type
     *
     * @param aLine
     */
    public boolean isCorrectType(String aLine) {
        return aLine != null && aLine.toLowerCase().startsWith("<?xml");
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
