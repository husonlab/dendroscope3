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

package dendroscope.commands;

import dendroscope.core.Director;
import dendroscope.core.Document;
import dendroscope.core.TreeData;
import dendroscope.embed.EmbedderForOrderPrescribedNetwork;
import dendroscope.embed.EmbeddingOptimizerNNet;
import dendroscope.embed.GeneralMethods;
import dendroscope.embed.LayoutOptimizerManager;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.graph.Node;
import jloda.gui.commands.ICommand;
import jloda.gui.director.IDirector;
import jloda.phylo.PhyloTree;
import jloda.util.Alert;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.ProgressListener;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/**
 * tanglegram using neighbor-net command
 * Daniel Huson & Celine Scornavacca, 7.2010
 */
public class TanglegramNeighborNetCommand extends CommandBaseMultiViewer implements ICommand {
    private final boolean verbose = false;

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Tanglegram...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Compute a tanglegram for two trees or networks using (Scornavacca,Zickmann and Huson, 2011)";
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
        return KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
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
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "compute tanglegram method=nnet;";
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return multiViewer.getTreeGrid().getNumberSelectedOrAllViewers() == 2 && getDir().getDocument().getNumberOfTrees() > 0;
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        execute(getSyntax());
    }


    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());

        PhyloTree[] trees = new PhyloTree[multiViewer.getTreeGrid().getNumberSelectedOrAllViewers()];
        String[] names = new String[multiViewer.getTreeGrid().getNumberSelectedOrAllViewers()];

        int t = 0;
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            trees[t] = (PhyloTree) viewer.getPhyloTree().clone();
            names[t] = viewer.getName();
            t++;
        }


        ProgressListener progressListener = multiViewer.getDir().getDocument().getProgressListener();
        progressListener.setTasks("Compute tanglegram", "Initialization");

        long timeBef;
        long timeAfter;
        timeBef = System.currentTimeMillis();

        EmbeddingOptimizerNNet optimizer = new EmbeddingOptimizerNNet();
        optimizer.apply(trees, progressListener, false, false);   // choose best common embedding

        timeAfter = System.currentTimeMillis();
        long timeNeed = timeAfter - timeBef;

        List<String>[] orderFin = new LinkedList[2];
        orderFin[0] = new LinkedList<>();
        orderFin[1] = new LinkedList<>();

        //todo: check these changes
        orderFin[0] = optimizer.getFirstOrder();
        orderFin[1] = optimizer.getSecondOrder();

        int bestScore = GeneralMethods.computeCrossingNum(orderFin[0], orderFin[1]);
        if (verbose) {
            System.err.println("--------------------------");
            System.err.println("Actual number of crossings in the current embedding: " + bestScore);

        }
        System.err.println("Final order of the taxa in the trees (lsa): ");
        System.err.println(orderFin[0]);
        System.err.println(orderFin[1]);


        if (timeNeed > 60000) {
            System.err.println("Time needed for computation: " + timeNeed / 60000 + " min.");
        } else if (timeNeed > 1000) {
            System.err.println("Time needed for computation: " + timeNeed / 1000 + " s.");
        } else {
            System.err.println("Time needed for computation: " + timeNeed + " ms.");
        }
        System.err.println("--------------------------");

        int rows = Math.min(4, (int) (Math.sqrt(trees.length)));
        int cols = trees.length / rows;


        Director theDir;
        MultiViewer theMultiViewer;
        Document theDoc;

        if (ProgramProperties.isUseGUI()) {
            theDir = Director.newProject(rows, cols);
            theMultiViewer = (MultiViewer) theDir.getViewerByClass(MultiViewer.class);
            theDoc = theDir.getDocument();
        } else // in commandline mode we recycle the existing document:
        {
            theDir = getDir();
            theMultiViewer = (MultiViewer) getViewer();
            theDoc = theDir.getDocument();
            theDoc.setTrees(new TreeData[0]);
        }

        theMultiViewer.setEmbedderName(LayoutOptimizerManager.UNOPTIMIZED); // don't change best common embedding

        theDoc.setTitle(Basic.getFileBaseName(getDir().getDocument().getTitle()) + "-tanglegram");
        BitSet which = new BitSet();
        for (int i = 0; i < trees.length; i++) {
            theDoc.appendTree(names[i], trees[i], i);
            // System.err.println("tree[" + i + "] in doc: " + theDoc.getTree(i).toBracketString());
            which.set(i);
        }

        theMultiViewer.loadTrees(which);

        theMultiViewer.getFrame().toFront();

        for (int i = 0; i < Math.min(trees.length, orderFin.length); i++) {
            TreeViewer treeViewer = theMultiViewer.getTreeGrid().getViewerByRank(i);
            treeViewer.setDrawerKind(TreeViewer.RECTANGULAR_CLADOGRAM);
            treeViewer.setDirty(true);

            //  Map<Node, Float> node2pos = EmbedderForOrderPrescribedNetwork.setupAlphabeticalOrdering(treeViewer);

            try {
                Map<Node, Float> node2pos = EmbedderForOrderPrescribedNetwork.setupOrderingFromNames(treeViewer, orderFin[i]);
                EmbedderForOrderPrescribedNetwork.apply(treeViewer, node2pos);
            } catch (Exception ex) {
                Basic.caught(ex);
                new Alert(theMultiViewer.getFrame(), "Exception: " + ex.getMessage());
            }
            if (i > 0) {
                treeViewer.trans.setFlipH(true);
                treeViewer.flipNodeLabels(true, false);
                if (treeViewer.getGraphDrawer() != null)
                    treeViewer.trans.setCoordinateRect(treeViewer.getGraphDrawer().getBBox());
                else
                    treeViewer.trans.setCoordinateRect(new Rectangle(0, 0, 500, 500));
            }
        }

        for (int i = 0; i < theMultiViewer.getTreeGrid().getNumberOfPanels(); i++) {
            for (int j = i + 1; j < theMultiViewer.getTreeGrid().getNumberOfPanels(); j++) {
                theMultiViewer.getTreeGrid().connectorAllTaxa(theMultiViewer.getTreeGrid().getViewerByRank(i), theMultiViewer.getTreeGrid().getViewerByRank(j));
            }
        }
        theDir.setDirty(true);
        theDir.getDocument().setDocumentIsDirty(true);

        theMultiViewer.recomputeEmbedding();
        theMultiViewer.updateView(IDirector.ALL);
        theMultiViewer.setMustRecomputeEmbedding(true);
        theMultiViewer.setMustRecomputeCoordinates(true);
        theMultiViewer.getTreeGrid().repaint();
    }
}
