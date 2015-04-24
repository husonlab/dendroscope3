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

package dendroscope.commands.autumn;

import dendroscope.autumn.hybridnetwork.ComputeHybridizationNetwork;
import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.core.Director;
import dendroscope.core.Document;
import dendroscope.core.TreeData;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.gui.Message;
import jloda.gui.commands.ICommand;
import jloda.gui.director.IDirector;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.Single;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * computes the hybrid number for two multifurcating trees
 * Daniel Huson, 4.2011
 */
public class ComputeHybridizationNetworksCommand extends CommandBaseMultiViewer implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator();
        TreeData tree1 = getDir().getDocument().getTree(multiViewer.getTreeGrid().getNumberOfViewerInDocument(it.next()));
        TreeData tree2 = getDir().getDocument().getTree(multiViewer.getTreeGrid().getNumberOfViewerInDocument(it.next()));

        Single<Integer> hybridizationNumber = new Single<>();

        TreeData[] trees = ComputeHybridizationNetwork.apply(tree1, tree2, getDir().getDocument().getProgressListener(), hybridizationNumber);
        getDir().getDocument().getProgressListener().close();

        if (trees.length > 0 && trees[0].getNumberOfNodes() > 0) {
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

            theDoc.appendTrees(trees);
            theDoc.setTitle(Basic.replaceFileSuffix(multiViewer.getDir().getDocument().getTitle(), "-hybrid"));
            theMultiViewer.chooseGridSize();
            theMultiViewer.loadTrees(null);
            theMultiViewer.setMustRecomputeEmbedding(true);
            theDir.setDirty(true);
            theDir.getDocument().setDocumentIsDirty(true);
            theMultiViewer.updateView(IDirector.ALL);
            // theMultiViewer.getCommandManager().execute("select edges;set labelcolor=null;deselect edges;");
            theMultiViewer.getFrame().toFront();
            new Message(theMultiViewer.getFrame(), "Hybridization number: " + hybridizationNumber.get() + "\nNumber of networks: " + trees.length);
        }
    }

    public void actionPerformed(ActionEvent ev) {
        execute(getSyntax());
    }

    public String getSyntax() {
        return "compute hybridization-network method=Autumn;";
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Compute minimum hybridization networks for two multifurcating trees with overlapping taxon sets (Huson, in preparation)";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getName() {
        return "Hybridization Networks...";
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
