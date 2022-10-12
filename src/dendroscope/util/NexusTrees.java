/*
 * NexusTrees.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.util;

import dendroscope.core.TreeData;
import jloda.graph.Node;
import jloda.graph.NotOwnerException;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import jloda.util.parse.NexusStreamTokenizer;

import java.io.*;
import java.util.*;

/**
 * I/O for Nexus trees block
 * Daniel Huson, 2.2007
 */
public class NexusTrees {
    final public static String NAME = "Trees";
    private int ntrees = 0; // number of trees
    private boolean partial = false; // does this block contain trees on subsets of the taxa?
    private boolean rooted = false; // are the trees rooted?
    private boolean rootedGloballySet = false; // if, true, this overrides [&R] statment
    final private Vector trees = new Vector(); // list of phylotrees
    final private Map translate = new HashMap(); // maps node labels to taxa


    /**
     * constructor
     */
    public NexusTrees() {
    }

    /**
     * clears all the data associated with this trees block
     */
    public void clear() {
        translate.clear();
        trees.clear();
        ntrees = 0;
        partial = false;
        rooted = false;
        rootedGloballySet = false;
    }

    /**
     * is the given file a Nexus file?
     *
     * @return true, if file exists, is readable and is a nexus file
     */
    public static boolean isNexusFile(File file) {
        boolean result = false;
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(file));
            String aLine = r.readLine();
            if (aLine != null && aLine.toLowerCase().startsWith("#nexus"))
                result = true;

        } catch (Exception ignored) {
        }
        try {
            if (r != null)
                r.close();
        } catch (IOException ignored) {
        }
        return result;
    }

    public void read(Reader r) throws IOException {
         NexusStreamParser np = new NexusStreamParser(r);

        np.matchIgnoreCase("#nexus");

        // skip all non-tree blocks:
        while (true) {
            np.matchAnyTokenIgnoreCase("begin beginblock");
            if (np.peekMatchIgnoreCase(NAME))
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
            System.err.println("done");
        }

        np.matchIgnoreCase(NAME + ";");

        if (np.peekMatchIgnoreCase("properties")) {
            List tokens = np.getTokensLowerCase("properties", ";");
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
            List taxlabels = new ArrayList();
            np.matchIgnoreCase("translate");
            while (!np.peekMatchIgnoreCase(";")) {
                String nodelabel = np.getWordRespectCase();
                String taxlabel = np.getWordRespectCase();
// if we have a translate and have to detect the Tasa use the taxlabels
                taxlabels.add(taxlabel);
                translate.put(nodelabel, taxlabel);

                if (!np.peekMatchIgnoreCase(";"))
                    np.matchIgnoreCase(",");
            }
            np.matchIgnoreCase(";");
        }

        while (np.peekMatchIgnoreCase("tree")) {
            np.matchIgnoreCase("tree");
            String name = np.getWordRespectCase();
            name = name.replaceAll("[ \t\b]+", "_");
            name = name.replaceAll("[:;,]+", ".");
            name = name.replaceAll("\\[", "(");
            name = name.replaceAll("\\]", ")");

            np.matchIgnoreCase("=");
            np.pushPunctuationCharacters(NexusStreamTokenizer.SEMICOLON_PUNCTUATION);
            try {
                TreeData tree = new TreeData(PhyloTree.valueOf(np.getWordRespectCase()));
                addTree(name, tree);
            } catch (Exception ex) {
                Basic.caught(ex);
                np.popPunctuationCharacters();
                throw new IOException("line " + np.lineno() +
                        ": Add tree failed: " + ex.getMessage());
            }
            np.popPunctuationCharacters();
            np.matchIgnoreCase(";");
        }
        np.matchEndBlock();
    }

    /**
     * Returns the i-th tree
     *
     * @param i the number of the tree
     * @return the i-th tree
     */
    public TreeData getTree(int i) {
        return (TreeData) trees.elementAt(i - 1);
    }

    /**
     * Returns the i-th tree name.
     * Trees are numbered 1 to ntrees
     *
     * @param i the number of the tree
     * @return the i-th tree name
     */
    public String getName(int i) {
        return ((TreeData) trees.elementAt(i - 1)).getName();
    }

    /**
     * sets the i-th tree name
     *
     */
    public void setName(int i, String name) {
        ((TreeData) trees.elementAt(i - 1)).setName(name);
    }

    /**
     * Returns the nexus flag [&R] indicating whether the tree should be considered
     * as rooted
     *
     * @return String  Returns [&R] if rooted, and "" otherwise.
     */
    public String getFlags(int i) {
        if (getTree(i).getRoot() != null)
            return "[&R]";
        else
            return "";
    }

    /**
     * Adds a tree to the list of trees. If this is called to add the first
     * tree to the trees block, then the tree nodes must be labeled with
     * taxon names or integers 1..ntax. If this is not the case, then use
     * the other addTree method described below. Subsequent trees can be
     * added by this method regardless of which labels are used for nodes,
     * as long as they are compatible with the initial translation table.
     *
     * @param name the name of the tree
     * @param tree the phylogenetic tree
     */
    public void addTree(String name, TreeData tree)
            throws IOException, NotOwnerException {

        // apply translation, if necessary
        if (translate != null) {
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                String label = tree.getLabel(v);
                if (label != null) {
                    String newLabel = (String) translate.get(label);
                    if (newLabel != null)
                        tree.setLabel(v, newLabel);
                }
            }
        }
        ntrees++;
        trees.setSize(ntrees);
        trees.add(ntrees - 1, tree);
        tree.setName(name);
    }

    public int getNtrees() {
        return ntrees;
    }

    public void setNtrees(int ntrees) {
        this.ntrees = ntrees;
    }

    public boolean isPartial() {
        return partial;
    }

    public boolean isRooted() {
        return rooted;
    }

    public Vector getTrees() {
        return trees;
    }

    public Map getTranslate() {
        return translate;
    }

    /**
     * show the usage of this block
     *
     * @param ps the print stream
     */
    public static void showUsage(PrintStream ps) {
        ps.println("BEGIN " + NexusTrees.NAME + ";");
        ps.println("[PROPERTIES PARTIALTREES={YES|NO} ROOTED={YES|NO};]");
        ps.println("[TRANSLATE");
        ps.println("    nodeLabel1 taxon1,");
        ps.println("    nodeLabel2 taxon2,");
        ps.println("    ...");
        ps.println("    nodeLabelN taxonN");
        ps.println(";]");
        ps.println("[TREE name1 = tree1-in-Newick-format;]");
        ps.println("[TREE name2 = tree2-in-Newick-format;]");
        ps.println("...");
        ps.println("[TREE nameM = treeM-in-Newick-format;]");
        ps.println("END;");
    }

    /**
     * writes a single tree in nexus format
     *
     */
    public static void writeNexus(Writer w, PhyloTree tree) throws IOException {
        w.write("#NEXUS\n");
        w.write("BEGIN " + NexusTrees.NAME + ";");
        w.write("TREE tree=");
        tree.write(w, true);
        w.write(";\n");
        w.write("END;\n");
    }

    /**
     * writes trees in nexus format
     *
     */
    public static void writeNexus(Writer w, TreeData[] trees) throws IOException {
        w.write("#NEXUS\n");
        w.write("BEGIN " + NexusTrees.NAME + ";");
        for (int t = 0; t < trees.length; t++) {
            String name = trees[t].getName();
            if (name != null)
                w.write("TREE " + name + " = ");
            else
                w.write("TREE tree" + t + " = ");
            trees[t].write(w, true);
            w.write(";\n");
        }
        w.write("END;\n");
    }
}
