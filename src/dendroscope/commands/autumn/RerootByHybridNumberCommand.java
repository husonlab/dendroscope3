/**
 * RerootByHybridNumberCommand.java 
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
package dendroscope.commands.autumn;

import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.core.Director;
import dendroscope.core.Document;
import dendroscope.core.TreeData;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.gui.Message;
import jloda.gui.commands.ICommand;
import jloda.gui.director.IDirector;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * reroots two trees by hybrid number
 * Daniel Huson, 4.2011
 */
public class RerootByHybridNumberCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        Document doc = getDir().getDocument();

        Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator();
        PhyloTree tree1 = doc.getTree(multiViewer.getTreeGrid().getNumberOfViewerInDocument(it.next()));
        PhyloTree tree2 = doc.getTree(multiViewer.getTreeGrid().getNumberOfViewerInDocument(it.next()));

        tree1 = (PhyloTree) tree1.clone();
        tree2 = (PhyloTree) tree2.clone();

        // int h = ComputeHybridNumber.applyWithRerooting(tree1, tree2, getDir().getDocument().getProgressListener());
        int h = dendroscope.autumn.hybridnumber.RerootByHybridNumber.apply(tree1, tree2, getDir().getDocument().getProgressListener());

        getDir().getDocument().getProgressListener().close();

        if (h < Integer.MAX_VALUE) {
            new Message(getViewer().getFrame(), "Best hybridization number is: " + h);

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

            theDoc.appendTrees(new TreeData[]{new TreeData(tree1), new TreeData(tree2)});
            theDoc.setTitle(Basic.replaceFileSuffix(multiViewer.getDir().getDocument().getTitle(), "-rerooted"));
            theMultiViewer.chooseGridSize();
            theMultiViewer.loadTrees(null);
            theMultiViewer.setMustRecomputeEmbedding(true);
            theDir.setDirty(true);
            theDir.getDocument().setDocumentIsDirty(true);
            theMultiViewer.updateView(IDirector.ALL);
            // theMultiViewer.getCommandManager().execute("select edges;set labelcolor=null;deselect edges;");
            theMultiViewer.getFrame().toFront();
        }
    }

    public void actionPerformed(ActionEvent ev) {
        execute(getSyntax());
    }

    public String getSyntax() {
        return "rerootby method=min-hybridization-number;";
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Reroot two trees so as to minimize their hybridization number (tree multifurcating, on overlapping taxon sets) (Autumn algorithm, Huson and Linz, 2016)";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getName() {
        return "Reroot by Hybridization Number...";
    }

    public boolean isApplicable() {
        // too expensive to check for equal label sets here...
        Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator();
        return it.hasNext() && it.next().getPhyloTree().getSpecialEdges().size() == 0 && it.hasNext() && it.next().getPhyloTree().getSpecialEdges().size() == 0
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
