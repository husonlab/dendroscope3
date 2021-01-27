/*
 *   Newick.java Copyright (C) 2020 Daniel H. Huson
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
import jloda.util.Basic;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * read and write trees in Newick format
 * Daniel Huson, 7.2007
 */
public class Newick extends IOBase implements IOFormat {
    public static final String DESCRIPTION = "Newick File (*.tree, *.tre, *.new, *.newick)";
    public static final String NAME = "Newick";
    public static final String EXTENSION = ".tree";

    private boolean saveEdgeWeights = true;

    /**
     * does this look like a file of the correct type?
     *
     * @param file
     * @return true, if correct type of file
     */
    public boolean isCorrectFileType(File file) {
        try {
            return isCorrectType(Basic.toString(Basic.getFirstBytesFromFile(file, 1)));
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
        return aLine != null && aLine.startsWith("(");
    }

    /**
     * read trees
     *
     * @param r0
     * @return trees
     * @throws IOException
     */
    public TreeData[] read(Reader r0) throws IOException {
        boolean warned = false;
        try (BufferedReader r = new BufferedReader(r0)) {
            final List<TreeData> list = new LinkedList<>();
            final StringBuilder buf = new StringBuilder();
            String aLine;
            while ((aLine = r.readLine()) != null) {
                aLine = aLine.trim();
                if (!aLine.endsWith(";")) {
                    buf.append(aLine);
                } else // got a whole tree
                {
                    buf.append(aLine);
                    final TreeData tmpTreeData = new TreeData();
                    tmpTreeData.setName(createNewTreeName());
                    if (!warned && buf.toString().contains("#")) {
                        System.err.println("Input contains the special character '#', will try to interpret as extended-Newick");
                        warned = true;
                    }
                    tmpTreeData.parseBracketNotation(buf.toString(), true);
                    buf.delete(0, buf.length());
                    list.add(tmpTreeData);
                }
            }
            if (buf.length() > 0) {
                TreeData tree = new TreeData();
                tree.parseBracketNotation(buf.toString(), true);
                tree.setName(createNewTreeName());
                buf.delete(0, buf.length());
                list.add(tree);
            }
            return list.toArray(new TreeData[0]);
        }
    }


    /**
     * write trees
     *
     * @param w0
     * @param trees
     * @throws java.io.IOException
     */
    public void write(Writer w0, boolean internalNodeLabelsAreEdgeLabels, TreeData[] trees) throws IOException {
        try (BufferedWriter w = new BufferedWriter(w0)) {
            for (TreeData tree : trees) {
                tree.setHideCollapsedSubTreeOnWrite(true);
                tree.write(w, saveEdgeWeights, true);
                tree.setHideCollapsedSubTreeOnWrite(false);
                w.write(";\n");
            }
            System.err.println("written " + trees.length + " trees");
        }
    }

    /**
     * do we accept this file?
     *
     * @param file
     * @return true, if correct ending
     */
    public boolean accept(File file) {
        if (file != null) {
            if (file.isDirectory()) return true;
            // Get the file extension
            try {
                String extension = getExtension(file);
                if (extension != null)
                    if (extension.equalsIgnoreCase("new")
                            || extension.equalsIgnoreCase("tre")
                            || extension.equalsIgnoreCase("tree")
                            || extension.equalsIgnoreCase("newick"))
                        return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
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

    public boolean isSaveEdgeWeights() {
        return saveEdgeWeights;
    }

    public void setSaveEdgeWeights(boolean saveEdgeWeights) {
        this.saveEdgeWeights = saveEdgeWeights;
    }
}
