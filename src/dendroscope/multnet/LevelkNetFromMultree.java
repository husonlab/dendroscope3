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

package dendroscope.multnet;

import dendroscope.algorithms.levelknet.LevelKNetwork;
import dendroscope.window.MultiViewer;
import jloda.gui.ProgressDialog;
import jloda.phylo.PhyloTree;
import jloda.util.ProgressListener;

import java.util.List;

/**
 * This class calculates a level-k network from a given MUL-tree by the help of leo's algorithm.
 *
 * @author thomas bonfert, 6.2009
 */

public class LevelkNetFromMultree {

    private final PhyloTree tree;

    public LevelkNetFromMultree(PhyloTree tree) {
        this.tree = tree;
    }

    public PhyloTree apply() {
        ProgressListener progressListener = new ProgressDialog("Computing level-k network", "Initializing", MultiViewer.getLastActiveFrame());
        try {
            MultilabeledTree mulTree = new MultilabeledTree();
            mulTree.copy(this.tree);
            mulTree.addOutgroup();
            mulTree.adaptLabeling();
            LevelKNetwork levelKNetwork = new LevelKNetwork(mulTree.getTaxa(), mulTree.getSplitSystem());
            levelKNetwork.setComputeOnlyOne(true);
            List<PhyloTree> results = levelKNetwork.apply(progressListener);
            if (results.size() > 0)
                return results.get(0);
            else
                return null;
        } finally {
            progressListener.close();
        }
    }
}
