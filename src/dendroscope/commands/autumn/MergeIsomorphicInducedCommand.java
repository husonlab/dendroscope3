/*
 *   MergeIsomorphicInducedCommand.java Copyright (C) 2020 Daniel H. Huson
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
package dendroscope.commands.autumn;

import dendroscope.autumn.PostProcess;
import dendroscope.autumn.PreProcess;
import dendroscope.autumn.Root;
import dendroscope.autumn.hybridnetwork.AddHybridNode;
import dendroscope.autumn.hybridnetwork.MergeIsomorphicInducedTrees;
import dendroscope.autumn.hybridnetwork.RemoveTaxon;
import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.consensus.Taxa;
import dendroscope.core.Director;
import dendroscope.core.TreeData;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirector;
import jloda.swing.util.Message;
import jloda.util.FileUtils;
import jloda.util.Pair;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;

/**
 * files two trees
 * Daniel Huson, 4.2011
 */
public class MergeIsomorphicInducedCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        Set<String> selectedLabels = new HashSet<>();

        Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator();
        TreeViewer viewer1 = it.next();
        selectedLabels.addAll(viewer1.getSelectedNodeLabels());
        TreeData tree1 = getDir().getDocument().getTree(multiViewer.getTreeGrid().getNumberOfViewerInDocument(viewer1));
        TreeViewer viewer2 = it.next();
        selectedLabels.addAll(viewer2.getSelectedNodeLabels());
        TreeData tree2 = getDir().getDocument().getTree(multiViewer.getTreeGrid().getNumberOfViewerInDocument(viewer2));

        Taxa allTaxa = new Taxa();
        Pair<Root, Root> roots = PreProcess.apply(tree1, tree2, allTaxa);
        Root root1 = roots.getFirst();
        Root root2 = roots.getSecond();

        BitSet selectedTaxa = new BitSet();
        for (String label : selectedLabels) {
            int id = allTaxa.indexOf(label);
            if (id != -1) {
                selectedTaxa.set(id);
                RemoveTaxon.apply(root1, 1, id);
                RemoveTaxon.apply(root2, 2, id);
            }
        }

        System.err.println("Tree1: " + root1.toStringFullTreeX());
        System.err.println("Tree2: " + root2.toStringFullTreeX());

        Root result = MergeIsomorphicInducedTrees.apply(root1, root2);
        if (result != null)
            System.err.println("Merged: " + result.toStringFullTreeX());

        if (result != null) {
            List<Root> list = new LinkedList<>();
            list.add(result);

            List<TreeData> resultTrees = new LinkedList<>();
            resultTrees.addAll(PostProcess.apply(new Root[]{result}, allTaxa, false));

            for (int t = selectedTaxa.nextSetBit(0); t != -1; t = selectedTaxa.nextSetBit(t + 1)) {
                AddHybridNode.apply(list, t);
            }
            resultTrees.addAll(PostProcess.apply(new Root[]{result}, allTaxa, false));

            /*
            if (allTaxa.contains("t2")) {
                allTaxa.add("t2xx");
                allTaxa.add("t2yy");
                allTaxa.add("t2zz");

                PhyloTree subTree = new PhyloTree();
                subTree.parseBracketNotation("((t2,t2xx),(t2yy,t2zz));", true);
                Root subRoot = Root.createACopy(new Graph(), subTree, allTaxa);
                List<Root> subTrees = new LinkedList<Root>();
                subTrees.add(subRoot);
                List<Root> merged = MergeNetworks.apply(list, subTrees);
                resultTrees.addAll(PostProcess.apply(merged.toArray(new Root[merged.size()]), allTaxa, true));
            }

            if (allTaxa.contains("t4")) {
                allTaxa.add("t4xx");
                allTaxa.add("t4yy");

                PhyloTree subTree = new PhyloTree();
                subTree.parseBracketNotation("((t4,(t4xx,t4yy)));", true);
                Root subRoot = Root.createACopy(new Graph(), subTree, allTaxa);
                List<Root> subTrees = new LinkedList<Root>();
                subTrees.add(subRoot);
                List<Root> merged = MergeNetworks.apply(list, subTrees);
                resultTrees.addAll(PostProcess.apply(merged.toArray(new Root[merged.size()]), allTaxa, true));
            }
            */

            if (resultTrees.size() > 0) {
                for (int i = 0; i < resultTrees.size(); i++)
                    resultTrees.get(i).setName("[" + (i + 1) + "]");

                if (resultTrees.get(0).getNumberOfNodes() > 0) {
                    Director newDir = Director.newProject(1, 1);
					newDir.getDocument().appendTrees(resultTrees.toArray(new TreeData[resultTrees.size()]));
					newDir.getDocument().setTitle(FileUtils.replaceFileSuffix(getDir().getDocument().getTitle(), "-merged"));
                    MultiViewer newMultiViewer = (MultiViewer) newDir.getMainViewer();
                    newMultiViewer.chooseGridSize();
                    newMultiViewer.loadTrees(null);
                    newMultiViewer.setMustRecomputeEmbedding(true);
                    newMultiViewer.updateView(IDirector.ALL);
                    newMultiViewer.getFrame().toFront();
                    newDir.getDocument().setDocumentIsDirty(true);
                }
            }
        } else
            new Message(getViewer().getFrame(), "Trees not reduced-isomorphic");
    }

    public void actionPerformed(ActionEvent ev) {
        execute(getSyntax());
    }

    public String getSyntax() {
        return "experimental what=merged;";
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Merge two reduced-isomorphic trees";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getName() {
        return "Merge Isomorphic Induced...";
    }

    public boolean isApplicable() {
        if (multiViewer.getTreeGrid().getNumberSelectedOrAllViewers() != 2)
            return false;
        Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator();
        // too expensive to check for equal label sets here...
        return it.next().getPhyloTree().getNumberSpecialEdges() == 0 && it.next().getPhyloTree().getNumberSpecialEdges() == 0
                && ((MultiViewer) getViewer()).getDir().getDocument().getNumberOfTrees() > 0;
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }
}
