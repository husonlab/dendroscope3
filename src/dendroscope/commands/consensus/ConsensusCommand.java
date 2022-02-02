/*
 * ConsensusCommand.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.commands.consensus;

import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.consensus.*;
import dendroscope.core.Director;
import dendroscope.core.Document;
import dendroscope.core.TreeData;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.phylo.PhyloTree;
import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirector;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

public class ConsensusCommand extends CommandBaseMultiViewer implements ICommand {
    public void apply(NexusStreamParser np) throws Exception {
		np.matchIgnoreCase("compute");
		np.matchIgnoreCase("consensus method=");
		String which = np.getWordMatchesIgnoringCase(StrictConsensus.NAME + " " + LooseConsensus.NAME + " " + MajorityConsensus.NAME + " "
													 + ComputeNetworkConsensus.LEVEL_K_NETWORK + " " + ComputeNetworkConsensus.CLUSTER_NETWORK + " " + ComputeNetworkConsensus.GALLED_NETWORK + " "
													 + Distortion1Consensus.NAME + " " + LSATree.NAME + " " + PrimordialConsensus.NAME);
		// System.err.println("which: " + which);
		IConsensusTreeMethod consensusMethod;
		float threshold;

		switch (which) {
			case MajorityConsensus.NAME -> consensusMethod = new MajorityConsensus();
			case LooseConsensus.NAME -> consensusMethod = new LooseConsensus();
			case Distortion1Consensus.NAME -> consensusMethod = new Distortion1Consensus();
			case PrimordialConsensus.NAME -> consensusMethod = new PrimordialConsensus();
			case LSATree.NAME -> consensusMethod = new LSATree();
			case ComputeNetworkConsensus.LEVEL_K_NETWORK, ComputeNetworkConsensus.CLUSTER_NETWORK, ComputeNetworkConsensus.GALLED_NETWORK -> {
				np.matchIgnoreCase("threshold=");
				threshold = (float) np.getDouble(0, 100);
				consensusMethod = new ComputeNetworkConsensus(which, threshold);
			}
			default ->
// strict consensus is default:
					consensusMethod = new StrictConsensus();
		}

		boolean computeOnlyOne = false;
		if (np.peekMatchIgnoreCase("one-only=")) {
			np.matchIgnoreCase("one-only=");
			computeOnlyOne = np.getBoolean();
		}

		boolean checkTrees = false;
		if (np.peekMatchIgnoreCase("check-trees=")) {
			np.matchIgnoreCase("check-trees=");
            checkTrees = np.getBoolean();
        }

        if (which.equals(ComputeNetworkConsensus.LEVEL_K_NETWORK)) {
            ((ComputeNetworkConsensus) consensusMethod).setComputeOnlyOne(computeOnlyOne);
            ((ComputeNetworkConsensus) consensusMethod).setCheckTrees(checkTrees);
        }

        np.matchIgnoreCase(";");

        Vector<TreeData> trees = new Vector<>();
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            PhyloTree tree = (PhyloTree) viewer.getPhyloGraph();
            if (tree.getNumberOfNodes() != 0) {
                trees.add(new TreeData(tree));
            }
        }
        List<PhyloTree> result = new LinkedList<>();

        if (consensusMethod instanceof ComputeNetworkConsensus)
			result.addAll(((ComputeNetworkConsensus) consensusMethod).applyAll(getDir().getDocument(), trees.toArray(new TreeData[0])));
        else
			result.add(consensusMethod.apply(getDir().getDocument(), trees.toArray(new TreeData[0])));

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

        for (PhyloTree tree : result) {
            theDoc.appendTree(tree);
        }
        theDoc.setTitle(multiViewer.getDir().getDocument().getTitle() + "-" + which.toLowerCase());
        theMultiViewer.loadTrees(null);
        theMultiViewer.setMustRecomputeEmbedding(true);
        theMultiViewer.updateView(IDirector.ALL);
        theMultiViewer.getFrame().toFront();

        TreeViewer treeViewer = theMultiViewer.getTreeGrid().getTreeViewer(0, 0);
        treeViewer.setDirty(true);
        theDoc.setDocumentIsDirty(true);

        theMultiViewer.updateView(IDirector.TITLE);
    }

    public String getSyntax() {
        return "compute consensus method={" + StrictConsensus.NAME + "|" + LooseConsensus.NAME + "|" + MajorityConsensus.NAME + "|"
                + ComputeNetworkConsensus.LEVEL_K_NETWORK + "|" + ComputeNetworkConsensus.CLUSTER_NETWORK + "|" + ComputeNetworkConsensus.GALLED_NETWORK + "|"
                + Distortion1Consensus.NAME + "|" + LSATree.NAME + "|" + PrimordialConsensus.NAME + " [threshold=<number>]};";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        MultiViewer multiViewer = (MultiViewer) getViewer();
        int count = 0;
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            if (treeViewer.getPhyloTree().getNumberReticulateEdges() > 0)
				return false;
            count++;
        }
        return count > 1;
    }

    public boolean isCritical() {
        return true;
    }

    public void actionPerformed(ActionEvent ev) {
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return null;
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getName() {
        return "Compute consensus";
    }

    public String getUndo() {
        return null;
    }
}
