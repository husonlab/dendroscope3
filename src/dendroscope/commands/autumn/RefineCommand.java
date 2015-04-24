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

import dendroscope.autumn.Refine;
import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.core.Director;
import dendroscope.core.TreeData;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.gui.commands.ICommand;
import jloda.gui.director.IDirector;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

/**
 * files two trees
 * Daniel Huson, 4.2011
 */
public class RefineCommand extends CommandBaseMultiViewer implements ICommand {
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

        TreeData[] newTrees = Refine.apply(tree1, tree2);
        if (newTrees != null) {
            for (int i = 0; i < newTrees.length; i++)
                newTrees[i].setName("[" + (i + 1) + "]");

            if (newTrees.length > 0 && newTrees[0].getNumberOfNodes() > 0) {
                Director newDir = Director.newProject(1, 1);
                newDir.getDocument().appendTrees(newTrees);
                newDir.getDocument().setTitle(Basic.replaceFileSuffix(getDir().getDocument().getTitle(), "-refined"));
                MultiViewer newMultiViewer = (MultiViewer) newDir.getMainViewer();
                newMultiViewer.chooseGridSize();
                newMultiViewer.loadTrees(null);
                newMultiViewer.setMustRecomputeEmbedding(true);
                newMultiViewer.updateView(IDirector.ALL);
                newMultiViewer.getFrame().toFront();
                newDir.getDocument().setDocumentIsDirty(true);
            }
        }
    }

    public void actionPerformed(ActionEvent ev) {
        execute(getSyntax());
    }

    public String getSyntax() {
        return "experimental what=refine;";
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Refine two multifurcating trees";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getName() {
        return "Refine...";
    }

    public boolean isApplicable() {
        if (multiViewer.getTreeGrid().getNumberSelectedOrAllViewers() != 2)
            return false;
        Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator();
        // too expensive to check for equal label sets here...
        return it.next().getPhyloTree().getSpecialEdges().size() == 0 && it.next().getPhyloTree().getSpecialEdges().size() == 0
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
