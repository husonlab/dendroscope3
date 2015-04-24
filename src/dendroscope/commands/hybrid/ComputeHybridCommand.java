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
import jloda.gui.commands.ICommand;
import jloda.gui.director.IDirector;
import jloda.phylo.PhyloTree;
import jloda.phylo.PhyloTreeUtils;
import jloda.util.Alert;
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
        return it.hasNext() && it.next().getPhyloTree().getSpecialEdges().size() == 0 && it.hasNext() && it.next().getPhyloTree().getSpecialEdges().size() == 0
                && ((MultiViewer) getViewer()).getDir().getDocument().getNumberOfTrees() > 0;
    }

    public boolean isCritical() {
        return true;
    }
}
