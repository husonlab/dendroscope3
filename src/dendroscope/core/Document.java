/*
 *   Document.java Copyright (C) 2020 Daniel H. Huson
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
package dendroscope.core;

import dendroscope.util.NexusTrees;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressCmdLine;
import jloda.util.ProgressListener;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * the document associated with a viewer
 * Daniel Huson, 1.2007
 */
public class Document {
    private TreeData[] trees;  // list of tree obtained in input
    private int current; // index of current tree
    private String title = "Untitled";
    private final Connectors connectors = new Connectors();

    private boolean documentIsDirty;    // is any tree dirty?

    private boolean internalNodeLabelsAreEdgeLabels = false;

    private ProgressListener progressListener;
    private File file;

    /**
     * constructor
     */
    public Document() {
        trees = new TreeData[0];
        current = -1;
        documentIsDirty = false;
        progressListener = new ProgressCmdLine(); // for efficienty, allow only one
        file = null;
    }

    /**
     * is any tree dirty
     *
     * @return true, if dirty
     */
    public boolean isDocumentIsDirty() {
        return documentIsDirty;
    }

    /**
     * is any tree dirty
     *
     * @param documentIsDirty
     */
    public void setDocumentIsDirty(boolean documentIsDirty) {
        this.documentIsDirty = documentIsDirty;
    }

    /**
     * notifys the progress listener the progress
     *
     * @param current step number
     */
    public void notifySetProgress(int current) throws CanceledException {
        if (progressListener != null) {
            progressListener.setProgress(current);
        }
    }

    public void notifySubtask(String subtask) throws CanceledException {
        if (progressListener != null) {
            progressListener.setSubtask(subtask);
        }
    }

    /**
     * set the task and subtask name
     *
     * @param task
     * @param subtask
     */
    public void notifyTasks(String task, String subtask) throws CanceledException {
        progressListener.setTasks(task, subtask);
    }

    /**
     * set the task and subtask name
     *
     * @param max
     */
    public void notifySetMaximumProgress(int max) throws CanceledException {
        progressListener.setMaximum(max);
    }

    /**
     * get the set progress listener
     *
     * @return progress listener
     */
    public ProgressListener getProgressListener() {
        return progressListener;
    }

    /**
     * set the progress listener
     *
     * @param progressListener
     */
    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * get file for this document
     *
     * @return file
     */
    public File getFile() {
        return file;
    }

    /**
     * set file for this document
     *
     * @param file
     */
    public void setFile(File file) {
        this.file = file;
        if (file != null) {
            Basic.getFileWithNewUniqueName(file.getName()); // load this name
            setTitle(file.getName());
        }
    }

    /**
     * sets the file to the named one, modifying the name to make it unique, if desired
     *
     * @param name
     * @param unique
     */
    public void setFile(String name, boolean unique) {
        if (unique) {
            file = Basic.getFileWithNewUniqueName(name);
        } else {
            file = new File(name);
        }
        setTitle(file.getName());
    }

    /**
     * load a set of trees in Nexus format
     *
     * @param file
     * @throws IOException
     */
    public void loadNexus(File file) throws IOException {
        NexusTrees treesBlock = new NexusTrees();

        this.file = file;
        setTitle(file.getName());
        current = -1;

        treesBlock.read(new FileReader(file));

        trees = new TreeData[treesBlock.getNtrees()];
        for (int t = 1; t <= treesBlock.getNtrees(); t++) {
            trees[t - 1] = treesBlock.getTree(t);
        }
        System.err.println("Trees loaded: " + trees.length);
    }


    /**
     * save the current trees to a file
     *
     * @param file
     * @throws IOException
     */
    public void save(File file) throws IOException {
        setTitle(file.getName());
        BufferedWriter w = new BufferedWriter(new FileWriter(file));
        for (int i = 0; i < trees.length; i++) {
            w.write(trees[i].toString() + "\n");
        }
        w.close();
        setDocumentIsDirty(false);
    }

    /**
     * add a tree  to the current file of trees
     *
     * @param newTree
     * @return index of tree
     */
    public int appendTree(PhyloTree newTree) {
        return appendTree(createNewTreeName(), newTree, current);
    }


    /**
     * add a tree to the list after the given position
     *
     * @param name
     * @param newTree
     * @return index of tree
     */
    public int appendTree(String name, PhyloTree newTree, int pos) {
        setDocumentIsDirty(true);
        if (trees == null || trees.length == 0) {
            trees = new TreeData[1];
            trees[0] = new TreeData(name, newTree);
            current = 0;
            return 0;
        } else {
            if (pos > current)
                pos = current;
            TreeData[] newTrees = new TreeData[trees.length + 1];
            for (int i = 0; i <= pos; i++)
                newTrees[i] = trees[i];
            newTrees[pos + 1] = new TreeData(name, newTree);
            for (int i = pos + 1; i < trees.length; i++) {
                newTrees[i + 1] = trees[i];
                newTrees[i + 1].setName(trees[i].getName());
            }
            trees = newTrees;
            return pos + 1;
        }
    }

    /**
     * add a tree to the list after the given position
     *
     * @param name
     * @param newTree
     * @return index of tree
     */
    public int appendTreeWithoutCopy(String name, TreeData newTree, int pos) {
        setDocumentIsDirty(true);
        if (trees == null || trees.length == 0) {
            trees = new TreeData[1];
            trees[0] = newTree;
            current = 0;
            return 0;
        } else {
            if (pos > current)
                pos = current;
            TreeData[] newTrees = new TreeData[trees.length + 1];
            for (int i = 0; i <= pos; i++)
                newTrees[i] = trees[i];
            newTrees[pos + 1] = newTree;
            for (int i = pos + 1; i < trees.length; i++) {
                newTrees[i + 1] = trees[i];
                newTrees[i + 1].setName(trees[i].getName());
            }
            trees = newTrees;
            return pos + 1;
        }
    }

    /**
     * append a set of trees after the current tree
     *
     * @param newTrees
     * @return position of first new tree
     */
    public int appendTrees(TreeData[] newTrees) {
        return appendTrees(newTrees, current);
    }

    /**
     * append a list of trees to the list after the given position
     *
     * @param newTrees, must be non-zero
     * @return index of first appended tree
     */
    public int appendTrees(TreeData[] newTrees, int pos) {
        setDocumentIsDirty(true);
        if (trees == null || trees.length == 0) {
            trees = new TreeData[newTrees.length];
            System.arraycopy(newTrees, 0, trees, 0, newTrees.length);
            current = 0;
            return 0;
        } else {
            if (pos > current)
                pos = current;
            TreeData[] allTrees = new TreeData[trees.length + newTrees.length];
            Set<String> treeLabels = new HashSet<String>();
            for (TreeData tree : trees) {
                if (tree.getName() != null)
                    treeLabels.add(tree.getName());
            }
            for (int i = 0; i <= pos; i++) {
                allTrees[i] = trees[i];
            }
            for (int i = 0; i < newTrees.length; i++) {
                int newIndex = pos + 1 + i;
                allTrees[newIndex] = newTrees[i];
                if (newTrees[i].getName() == null || treeLabels.contains(newTrees[i].getName())) {
                    newTrees[i].setName("[" + (newIndex + 1) + "]");
                    treeLabels.add(newTrees[i].getName());
                }
            }
            for (int i = pos + 1; i < trees.length; i++) {
                allTrees[newTrees.length + i] = trees[i];
            }

            trees = allTrees;
            return pos + 1;
        }
    }

    /**
     * returns number of tree in file
     *
     * @return tree number
     */
    public int getCurrent() {
        return current;
    }

    /**
     * gets the number of trees
     *
     * @return number of trees
     */
    public int getNumberOfTrees() {
        return trees == null ? 0 : trees.length;
    }

    /**
     * gets the document title
     *
     * @return title
     */
    public String getTitle() {
        return title;
    }

    /**
     * sets the documents title
     *
     * @param title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isInternalNodeLabelsAreEdgeLabels() {
        return internalNodeLabelsAreEdgeLabels;
    }

    public void setInternalNodeLabelsAreEdgeLabels(boolean internalNodeLabelsAreEdgeLabels) {
        this.internalNodeLabelsAreEdgeLabels = internalNodeLabelsAreEdgeLabels;
    }

    /**
     * gets the name of the current tree
     *
     * @return name of current tree
     */
    public String getName() {
        return getName(current);
    }

    /**
     * gets the name of the current tree
     *
     * @return name of t-th tree
     */
    public String getName(int t) {
        String prefix = "[" + (t + 1) + "]";

        String name = "";
        if (t >= 0 && trees != null && t < trees.length && trees[t] != null)
            name = trees[t].getName();
        name = prefix + removePrefix(name);
        return name;
    }

    /**
     * remove prefix of form [digits]
     *
     * @param name
     * @return name without prefix
     */
    private String removePrefix(String name) {
        if (name != null && name.startsWith("[")) {
            for (int pos = 1; pos < name.length(); pos++) {
                if (name.charAt(pos) == ']')   // has just been [digits], so is a valid prefix that we can remove
                    return name.substring(pos + 1, name.length());
                if (!Character.isDigit(name.charAt(pos)))
                    break;
            }
        }
        return name; // no valid prefix [digits] found, return original string
    }

    /**
     * sets the name of the t-th tree
     *
     * @param name
     */
    public void setName(String name, int t) {
        if (t >= 0 && trees != null && t < trees.length)
            trees[t].setName(name);
    }

    /**
     * set the name for the current tree
     *
     * @param name
     */
    public void setName(String name) {
        setName(name, current);
    }

    /**
     * gets the t-th tree
     *
     * @param t
     * @return tree
     */
    public TreeData getTree(int t) {
        if (t >= 0 && t < trees.length)
            return trees[t];
        else
            return null;
    }

    /**
     * gets the current tree
     *
     * @return current tree
     */
    public TreeData getCurrentTree() {
        return getTree(current);
    }

    /**
     * gets the trees
     *
     * @return trees
     */
    public TreeData[] getTrees() {
        return trees;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    int countNewNames = 0;

    /**
     * creates a new unique tree name
     *
     * @return new unique tree name
     */
    public String createNewTreeName() {
        return "Tree" + (++countNewNames);
    }

    /**
     * sets the trees to the given array
     *
     * @param newTrees
     */
    public void setTrees(TreeData[] newTrees) {
        if (newTrees == null) {
            trees = new TreeData[0];
            current = -1;
        } else {
            trees = newTrees;
            current = 0;
            //System.err.println("Set trees: "+newTrees.length);
        }
    }

    /**
     * gets the inter-tree connectors associated with this document
     *
     * @return connectors
     */
    public Connectors getConnectors() {
        return connectors;
    }
}
