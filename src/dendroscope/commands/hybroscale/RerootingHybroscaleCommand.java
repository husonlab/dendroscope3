/*
 *   RerootingHybroscaleCommand.java Copyright (C) 2020 Daniel H. Huson
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
package dendroscope.commands.hybroscale;

import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.core.Director;
import dendroscope.core.Document;
import dendroscope.core.TreeData;
import dendroscope.hybroscale.rerooting.RerootByHNumber;
import dendroscope.hybroscale.rerooting.RerootingView;
import dendroscope.hybroscale.util.graph.MyPhyloTree;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
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
import java.util.Vector;

public class RerootingHybroscaleCommand extends CommandBaseMultiViewer implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {

        np.matchIgnoreCase("compute rerooted-trees method=Albrecht2015");
        boolean showDialog = false;
        if (np.peekMatchIgnoreCase("showdialog=")) {
            np.matchIgnoreCase("showdialog=");
            showDialog = np.getBoolean();
        }
        int availableCores = 1;
        if (np.peekMatchIgnoreCase("numberOfThreads=")) {
            np.matchIgnoreCase("numberOfThreads=");
            availableCores = np.getInt(1, 1000);
        }
        np.matchIgnoreCase(";");

        if (multiViewer.getTreeGrid().getNumberSelectedOrAllViewers() < 2) {
            new Alert(getViewer().getFrame(), "Please select at least two trees!");
            return;
        }

        Iterator<TreeViewer> it0 = multiViewer.getTreeGrid().getSelectedOrAllIterator();
        Vector<PhyloTree> selectedTrees = new Vector<PhyloTree>();
        while (it0.hasNext()) {
            TreeViewer tV = it0.next();
            PhyloTree t = tV.getPhyloTree();
            if (!PhyloTreeUtils.areSingleLabeledTrees(t)) {
                new Alert(getViewer().getFrame(), "The selected tree '" + t.getName() + "' is NOT single-labeled!");
                return;
            } else
                selectedTrees.add(t);
        }

        String[] treeStrings = new String[selectedTrees.size()];
        for (int i = 0; i < selectedTrees.size(); i++)
            treeStrings[i] = selectedTrees.get(i).toBracketString();

        RerootingView view = null;
        RerootByHNumber rerootManager = null;
        if (showDialog)
            view = new RerootingView(getViewer().getFrame(), this, treeStrings, availableCores);
        else
            rerootManager = new RerootByHNumber(null, treeStrings, availableCores, null);

        synchronized (this) {
            this.wait();
        }

        Vector<MyPhyloTree[]> bestRerootedTrees = showDialog ? view.getBestTreeSets() : rerootManager.getBestTreeSets();

        if (bestRerootedTrees != null) {
            TreeData[] treeData = new TreeData[bestRerootedTrees.size() * selectedTrees.size()];
            int counter = 0;
            for (int i = 0; i < bestRerootedTrees.size(); i++) {
                for (int j = 0; j < selectedTrees.size(); j++) {
                    MyPhyloTree bestTree = bestRerootedTrees.get(i)[j];
                    PhyloTree t = new PhyloTree();
                    t.parseBracketNotation(bestTree.toBracketString(), true);
                    t.setName("Rerooted_Tree " + i + "." + j);
                    treeData[counter++] = new TreeData(t);
                }
            }

            if (treeData != null && treeData.length > 0 && treeData[0].getNumberOfNodes() > 0) {

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

                theDoc.appendTrees(treeData);
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
    }

    public String getSyntax() {
        return "compute rerooted-trees method=Albrecht2015 [showdialog={false|true}] [numberOfThreads=number];";
    }

    public void actionPerformed(ActionEvent ev) {
        execute("compute rerooted-trees method=Albrecht2015 showdialog=true;");
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Reroot a set of non-binary trees so as to minimize their hybridization number (Albrecht, 2015)";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getName() {
        return "Reroot by Hybridization Number (Hybroscale)...";
    }

    public String getUndo() {
        return null;
    }

    public boolean isApplicable() {
        // too expensive to check for equal label sets here...
        Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator();
        return it.hasNext() && it.next().getPhyloTree().getSpecialEdges().size() == 0 && it.hasNext()
                && it.next().getPhyloTree().getSpecialEdges().size() == 0
                && ((MultiViewer) getViewer()).getDir().getDocument().getNumberOfTrees() > 0;
    }

    public boolean isCritical() {
        return true;
    }
}
