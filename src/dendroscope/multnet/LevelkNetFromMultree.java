/*
 *   LevelkNetFromMultree.java Copyright (C) 2020 Daniel H. Huson
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
package dendroscope.multnet;

import dendroscope.algorithms.levelknet.LevelKNetwork;
import dendroscope.window.MultiViewer;
import jloda.phylo.PhyloTree;
import jloda.swing.util.ProgressDialog;
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
