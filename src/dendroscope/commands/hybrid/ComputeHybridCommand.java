/*
 *   ComputeHybridCommand.java Copyright (C) 2020 Daniel H. Huson
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
package dendroscope.commands.hybrid;

import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.core.Director;
import dendroscope.core.Document;
import dendroscope.core.TreeData;
import dendroscope.hybrid.Controller;
import dendroscope.hybrid.View;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.graph.Edge;
import jloda.phylo.PhyloTree;
import jloda.phylo.PhyloTreeUtils;
import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirector;
import jloda.swing.util.Alert;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

public class ComputeHybridCommand extends CommandBaseMultiViewer implements ICommand {


    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("compute hybridization-network method=ASCH2011");
        boolean showDialog = false;
        if (np.peekMatchIgnoreCase("showdialog=")) {
            np.matchIgnoreCase("showdialog=");
            showDialog = np.getBoolean();
        }
        int numberOfThreads = -1;
        if (np.peekMatchIgnoreCase("numberOfThreads=")) {
            np.matchIgnoreCase("numberOfThreads=");
            numberOfThreads = np.getInt(1, 1000);
        }
        np.matchIgnoreCase(";");

        if (multiViewer.getTreeGrid().getNumberSelectedOrAllViewers() != 2) {
            return;
        }

        Iterator<TreeViewer> it0 = multiViewer.getTreeGrid().getSelectedOrAllIterator();
        PhyloTree[] treeArray = new PhyloTree[]{it0.next().getPhyloTree(), it0.next().getPhyloTree()};

        if (!PhyloTreeUtils.areSingleLabeledTreesWithSameTaxa(treeArray[0], treeArray[1])) {
            new Alert(getViewer().getFrame(), "Trees have different label sets");
            return;
        }

        if (!PhyloTreeUtils.isBifurcatingTree(treeArray[0])) {
            new Alert(getViewer().getFrame(), "Not a bifurcating tree: first");
            return;
        }
        if (!PhyloTreeUtils.isBifurcatingTree(treeArray[1])) {
            new Alert(getViewer().getFrame(), "Not a bifurcating tree: second");
            return;
        }

        Controller controller = new Controller(treeArray, this);
        if (numberOfThreads > 0)
            controller.setCores(numberOfThreads);

        if (showDialog) {
            View view = new View(getViewer().getFrame(), controller, View.Computation.NETWORK);
            controller.setView(view);
            view.run();
        } else { // non-gui mode
            controller.run(View.Computation.NETWORK, false);
        }

        synchronized (this) {
            this.wait();
        }

        TreeData[] trees = controller.getTreeData();
        if (trees != null && trees.length > 0 && trees[0].getNumberOfNodes() > 0) {
            for (int i = 0; i < trees.length; i++) {
                trees[i].setName("[" + (i + 1) + "]");
                for (Edge e = trees[i].getFirstEdge(); e != null; e = e.getNext()) {
                    if (e.getInfo() != null && !e.getInfo().equals("0"))
                        trees[i].setLabel(e, e.getInfo().toString());
                }
            }

            Director theDir;
            MultiViewer theMultiViewer;
            Document theDoc;

            if (ProgramProperties.isUseGUI()) {
                theDir = Director.newProject(1, 1);
                theMultiViewer = (MultiViewer) theDir.getViewerByClass(MultiViewer.class);
                theDoc = theDir.getDocument();
            } else // in commandline mode we recycle the existing document:
            {
                theDir = getDir();
                theMultiViewer = (MultiViewer) getViewer();
                theDoc = theDir.getDocument();
                theDoc.setTrees(new TreeData[0]);
            }

            if (showDialog)
                controller.setMultiViewer(theMultiViewer);

            theDoc.appendTrees(trees);
            theDoc.setTitle(Basic.replaceFileSuffix(multiViewer.getDir().getDocument().getTitle(), "-hybrid"));
            theMultiViewer.chooseGridSize();
            theMultiViewer.loadTrees(null);
            for (Iterator<TreeViewer> it = theMultiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
                TreeViewer viewer = it.next();
                viewer.setDirty(true);
            }

            theMultiViewer.setMustRecomputeEmbedding(true);
            theDir.setDirty(true);
            theDir.getDocument().setDocumentIsDirty(true);
            theMultiViewer.updateView(IDirector.ALL);
            theMultiViewer.getFrame().toFront();
        }
    }


    public String getSyntax() {
        return "compute hybridization-network method=ASCH2011 [showdialog={false|true}] [numberOfThreads=number];";
    }


    public void actionPerformed(ActionEvent ev) {
        execute("compute hybridization-network method=ASCH2011 showdialog=true;");
    }


    public KeyStroke getAcceleratorKey() {
        return null;
    }


    public String getDescription() {
        return "Compute all minimum hybridization networks for two binary trees using (Albrecht, Scornavacca, Cenci, and Huson, 2011)";
    }

    public ImageIcon getIcon() {
        return null;
    }


    public String getName() {
        return "Hybridization Networks (Binary Trees)...";
    }


    public String getUndo() {
        return null;
    }

    public boolean isApplicable() {
        // too expensive to check for equal label sets here...
        Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator();
        return it.hasNext() && it.next().getPhyloTree().getNumberSpecialEdges() == 0 && it.hasNext() && it.next().getPhyloTree().getNumberSpecialEdges() == 0
                && ((MultiViewer) getViewer()).getDir().getDocument().getNumberOfTrees() > 0;
    }

    public boolean isCritical() {
        return true;
    }
}
