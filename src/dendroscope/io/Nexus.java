/**
 * Nexus.java 
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
package dendroscope.io;

import dendroscope.core.TreeData;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.NotOwnerException;
import jloda.util.parse.NexusStreamParser;
import jloda.util.parse.NexusStreamTokenizer;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * read and write trees in Nexus format
 * Daniel Huson, 7.2007
 */
public class Nexus extends IOBase implements IOFormat {
    public final static String DESCRIPTION = "Nexus File (*.nex,*.nxs, *.nexus)";
    public final static String EXTENSION = ".nex";
    public final static String NAME = "Nexus";

    private boolean partial = false; // does this block contain trees on subsets of the taxa?
    private boolean rooted = false; // are the trees rooted?
    private boolean rootedGloballySet = false; // if, true, this overrides [&R] statment
    final private Map<String, String> translate = new HashMap<>(); // maps node labels to taxa


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
     * does this look like the first line of the a file of the correct type?
     *
     * @param aLine
     * @return true, if correct type of string
     */
    public boolean isCorrectType(String aLine) {
        return aLine != null && aLine.toLowerCase().startsWith("#nexus");
    }

    /**
     * read trees
     *
     * @param r0
     * @return trees
     * @throws java.io.IOException
     */
    public TreeData[] read(Reader r0) throws IOException {
        final List treesList = new LinkedList(); // list of phylotrees
        TreeData[] trees;

        try (BufferedReader r = new BufferedReader(r0)) {
            NexusStreamParser np = new NexusStreamParser(r);

            np.matchIgnoreCase("#nexus");

            // skip all non-tree blocks:
            while (true) {
                np.matchAnyTokenIgnoreCase("begin beginblock");
                if (np.peekMatchIgnoreCase("trees"))
                    break;
                String name = np.getWordFileNamePunctuation();
                np.matchRespectCase(";");

                System.err.print("Skipping  NEXUS block '" + name + "': ");
                while (true) {
                    while (np.peekMatchAnyTokenIgnoreCase("end endblock") == false) {
                        np.nextToken();
                        if (np.ttype == NexusStreamTokenizer.TT_EOF)
                            throw new IOException("line " + np.lineno() +
                                    ": Unexpected EOF while skipping block");
                    }
                    np.matchAnyTokenIgnoreCase("end endblock");
                    if (np.peekMatchRespectCase(";")) {
                        np.matchRespectCase(";");
                        if (np.peekMatchAnyTokenIgnoreCase("begin beginblock"))
                            break;
                        np.nextToken();
                        if (np.ttype == NexusStreamParser.TT_EOF) // EOF ok
                            break;
                    }
                }
            }

            np.matchIgnoreCase("trees;");

            if (np.peekMatchIgnoreCase("properties")) {
                List<String> tokens = np.getTokensLowerCase("properties", ";");
                if (np.findIgnoreCase(tokens, "no partialtrees"))
                    partial = false;
                if (np.findIgnoreCase(tokens, "partialtrees=no"))
                    partial = false;
                if (np.findIgnoreCase(tokens, "partialtrees=yes"))
                    partial = true;
                if (np.findIgnoreCase(tokens, "partialtrees"))
                    partial = true;
                if (np.findIgnoreCase(tokens, "rooted=yes")) {
                    rooted = true;
                    rootedGloballySet = true;
                }
                if (np.findIgnoreCase(tokens, "rooted=no")) {
                    rooted = false;
                    rootedGloballySet = true;
                }
                if (tokens.size() != 0)
                    throw new IOException("line " + np.lineno() + ": `" + tokens + "' unexpected in PROPERTIES");
            }

            if (np.peekMatchIgnoreCase("translate")) {
                np.matchIgnoreCase("translate");
                while (!np.peekMatchIgnoreCase(";")) {
                    String nodelabel = np.getWordRespectCase();
                    String taxlabel = np.getWordRespectCase();
                    translate.put(nodelabel, taxlabel);

                    if (!np.peekMatchIgnoreCase(";"))
                        np.matchIgnoreCase(",");
                }
                np.matchIgnoreCase(";");
            }

            while (np.peekMatchIgnoreCase("tree")) {
                np.matchIgnoreCase("tree");
                if (np.peekMatchRespectCase("*"))
                    np.matchRespectCase("*"); // don't know why PAUP puts this star in the file....

                String name = np.getWordRespectCase();
                name = name.replaceAll("[ \t\b]+", "_");
                name = name.replaceAll("[:;,]+", ".");
                name = name.replaceAll("\\[", "(");
                name = name.replaceAll("\\]", ")");
                if (name.length() == 0 || name.equals("tree"))
                    name = createNewTreeName();

                np.matchIgnoreCase("=");
                StringBuilder buf = new StringBuilder();
                while (!np.peekMatchIgnoreCase(";"))
                    buf.append(np.getWordRespectCase());
                np.matchIgnoreCase(";");
                TreeData tree = new TreeData(PhyloTree.valueOf(buf.toString(), true));
                addTree(name, tree, treesList);

                /*
                np.pushPunctuationCharacters(NexusStreamTokenizer.SEMICOLON_PUNCTUATION);
                try {
                    String tmp = np.getWordRespectCase();
                    TreeData tree = new TreeData(PhyloTree.valueOf(tmp, true));
                    addTree(name, tree, treesList);
                } catch (Exception ex) {
                    Basic.caught(ex);
                    np.popPunctuationCharacters();
                    throw new IOException("line " + np.lineno() +
                            ": Add tree failed: " + ex.getMessage());
                }
                np.popPunctuationCharacters();
                np.matchIgnoreCase(";");
                */
            }
            np.matchEndBlock();
            trees = (TreeData[]) treesList.toArray(new TreeData[treesList.size()]);
        }
        return trees;
    }


    /**
     * write trees
     *
     * @param w0
     * @param trees
     * @throws java.io.IOException
     */
    public void write(Writer w0, TreeData[] trees) throws IOException {
        try (BufferedWriter w = new BufferedWriter(w0)) {
            w.write("#NEXUS\n");
            w.write("BEGIN trees;\n");
            for (TreeData tree : trees) {
                String name = tree.getName();
                if (name != null)
                    w.write("TREE '" + name + "' = ");
                else
                    w.write("TREE '" + createNewTreeName() + "' = ");
                tree.setHideCollapsedSubTreeOnWrite(true);
                tree.write(w, true);
                tree.setHideCollapsedSubTreeOnWrite(false);
                w.write(";\n");
            }
            w.write("END;\n");

        }
    }

    /**
     * Adds a tree to the list of trees. If this is called to add the first
     * tree to the trees block, then the tree nodes must be labeled with
     * taxon names or integers 1..ntax. If this is not the case, then use
     * the other addTree method described below. Subsequent trees can be
     * added by this method regardless of which labels are used for nodes,
     * as long as they are compatible with the initial translation table.
     *
     * @param name      the name of the tree
     * @param tree      the phylogenetic tree
     * @param treesList
     */
    private void addTree(String name, TreeData tree, List<TreeData> treesList)
            throws IOException, NotOwnerException {

        // apply translation, if necessary
        if (translate != null) {
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                String label = tree.getLabel(v);
                if (label != null) {
                    String newLabel = translate.get(label);
                    if (newLabel != null)
                        tree.setLabel(v, newLabel);
                }
            }
        }
        treesList.add(tree);
        tree.setName(name);
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
                    if (extension.equalsIgnoreCase("nex")
                            || extension.equalsIgnoreCase("nxs")
                            || extension.equalsIgnoreCase("nexus"))
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
}

