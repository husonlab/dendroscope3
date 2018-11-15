/**
 * OrderNetworksByHybridTaxa.java 
 * Copyright (C) 2018 Daniel H. Huson
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
package dendroscope.commands.autumn;

import dendroscope.core.Director;
import dendroscope.core.Document;
import dendroscope.core.TreeData;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.gui.director.IDirector;
import jloda.phylo.PhyloTree;
import jloda.util.Alert;
import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;

/**
 * order nodes by hybrid taxa
 * Daniel Huson, 1.2012
 */
public class OrderNetworksByHybridTaxa extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("orderNetworks");
        boolean order = true;
        if (np.peekMatchIgnoreCase("mode")) {
            np.matchIgnoreCase("mode=");
            order = (np.getWordMatchesIgnoringCase("order filter").equalsIgnoreCase("order"));
        }
        Set<String> noHybrid = new HashSet<>();
        if (np.peekMatchIgnoreCase("noHybrid")) {
            np.matchIgnoreCase("noHybrid=");
            while (true) {
                noHybrid.add(np.getWordRespectCase());
                if (!np.peekMatchIgnoreCase(","))
                    break;
                else
                    np.matchIgnoreCase(",");
            }
        }
        Set<String> noRecentHybrid = new HashSet<>();
        if (np.peekMatchIgnoreCase("noRecentHybrid")) {
            np.matchIgnoreCase("noRecentHybrid=");
            while (true) {
                noRecentHybrid.add(np.getWordRespectCase());
                if (!np.peekMatchIgnoreCase(","))
                    break;
                else
                    np.matchIgnoreCase(",");
            }
        }

        Set<String> hybrid = new HashSet<>();
        if (np.peekMatchIgnoreCase("hybrid")) {
            np.matchIgnoreCase("hybrid=");
            while (true) {
                hybrid.add(np.getWordRespectCase());
                if (!np.peekMatchIgnoreCase(","))
                    break;
                else
                    np.matchIgnoreCase(",");
            }
        }
        Set<String> recentHybrid = new HashSet<>();
        if (np.peekMatchIgnoreCase("recentHybrid")) {
            np.matchIgnoreCase("recentHybrid=");
            while (true) {
                recentHybrid.add(np.getWordRespectCase());
                if (!np.peekMatchIgnoreCase(","))
                    break;
                else
                    np.matchIgnoreCase(",");
            }
        }

        List<Pair<Integer, TreeViewer>> list = new LinkedList<>();

        MultiViewer multiViewer = (MultiViewer) getViewer();

        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            int score = computeScore(noHybrid, noRecentHybrid, hybrid, recentHybrid, treeViewer.getPhyloTree(), order);
            Pair<Integer, TreeViewer> pair = new Pair<>(score, treeViewer);
            list.add(pair);
        }
        Pair<Integer, TreeViewer>[] array = new Pair[list.size()];
        int count = 0;
        for (Pair<Integer, TreeViewer> pair : list) {
            array[count++] = pair;
        }


        List<TreeViewer> list2 = new LinkedList<>();
        if (order) {
            Arrays.sort(array);
            for (Pair<Integer, TreeViewer> pair : array) {
                list2.add(pair.getSecond());
            }

        } else // filter
        {
            for (Pair<Integer, TreeViewer> pair : array) {
                if (pair == null)
                    break;
                if (pair.getFirst() == 1) // 1 means that filter criteria were met
                    list2.add(pair.getSecond());
            }
        }

        if (list2.size() == 0) {
            new Alert(getViewer().getFrame(), "No matching networks found");
            return;
        }

        TreeViewer[] result = list2.toArray(new TreeViewer[list2.size()]);

        Document originalDocument = ((Director) getDir()).getDocument();
        Director theDir;
        MultiViewer theMultiViewer;
        Document theDoc;


        if (ProgramProperties.isUseGUI()) {
            theDir = Director.newProject(1, 1);
            theMultiViewer = (MultiViewer) theDir.getViewerByClass(MultiViewer.class);
            theDoc = theDir.getDocument();
        } else // in commandline mode we recycle the existing document:
        {
            theDir = (Director) getDir();
            theMultiViewer = (MultiViewer) getViewer();
            theDoc = theDir.getDocument();
            theDoc.setTrees(new TreeData[0]);
        }

        theDoc.setTitle(Basic.getFileBaseName(originalDocument.getTitle()) + "-" + (order ? "ordered" : "filtered"));
        BitSet which = new BitSet();
        for (int i = 0; i < result.length; i++) {
            theDoc.appendTree(originalDocument.getName(multiViewer.getTreeGrid().getNumberOfViewerInDocument(result[i])), result[i].getPhyloTree(), i);
            // System.err.println("tree[" + i + "] in doc: " + theDoc.getTree(i).toBracketString());
            which.set(i);
        }

        theMultiViewer.loadTrees(which);

        theMultiViewer.getFrame().toFront();
        theDir.setDirty(true);
        theDir.getDocument().setDocumentIsDirty(true);

        theMultiViewer.chooseGridSize();
        theMultiViewer.recomputeEmbedding();
        theMultiViewer.updateView(IDirector.ALL);
        theMultiViewer.setMustRecomputeEmbedding(true);
        theMultiViewer.setMustRecomputeCoordinates(true);
        theMultiViewer.getTreeGrid().repaint();

    }

    /**
     * compute score to reflect how well the given network matches the given sets of hybrid vs non-hybrid species
     *
     * @param noHybrid       should not be hybrids
     * @param noRecentHybrid should not be recent hybrids
     * @param hybrid         should be hybrid
     * @param recentHybrid   should be recent hybrid
     * @param tree
     * @param order          if true, return value "smaller is better", else return 0 for something violated, 1 for everything ok
     * @return
     */
    private int computeScore(Set<String> noHybrid, Set<String> noRecentHybrid, Set<String> hybrid, Set<String> recentHybrid, PhyloTree tree, boolean order) {
        int score = 0;
        if (tree.getRoot() == null)
            return 0;

        Stack<Node> stack = new Stack<>();
        Set<Node> seen = new HashSet<>();
        Set<Node> nodeIsBelowHybrid = new HashSet<>();
        stack.push(tree.getRoot());
        while (stack.size() > 0) {
            Node v = stack.pop();
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node w = e.getTarget();
                if (!seen.contains(w)) {
                    seen.add(w);
                    stack.push(w);
                    if (w.getInDegree() > 1 || (nodeIsBelowHybrid.contains(v) && v.getInDegree() > 1)) {
                        String label = tree.getLabel(w);
                        if (label != null) {
                            if (recentHybrid.contains(label))
                                score++;
                            if (noRecentHybrid.contains(label)) {
                                if (!order) // filter
                                    return 0;
                                score--;
                            }
                            if (hybrid.contains(label))
                                score++;
                            if (noHybrid.contains(label)) {
                                if (!order) // filter
                                    return 0;
                                score--;
                            }
                        }
                        nodeIsBelowHybrid.add(w);
                    } else if (nodeIsBelowHybrid.contains(v)) {
                        String label = tree.getLabel(w);
                        if (label != null) {
                            if (hybrid.contains(label))
                                score++;
                            if (noHybrid.contains(label)) {
                                if (!order) // filter
                                    return 0;
                                score--;
                            }
                        }
                    }
                }
            }
        }
        if (order)
            return -score;
        else // filter
            return (score == hybrid.size() + recentHybrid.size() ? 1 : 0);
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "orderNetworks [mode=<order|filter>] [noHybrid=<name,...>] [noRecent_hybrid=<name,...>] [hybrid=<name,...>] [recentHybrid=<name,...>]";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return null;
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Order or filter networks by whether specified taxa appear or do not appear below reticulate nodes";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return null;
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return true;
    }
}
