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

package dendroscope.consensus;

import dendroscope.core.Document;
import dendroscope.core.TreeData;
import jloda.phylo.PhyloTree;
import jloda.util.CanceledException;

/**
 * Computes the distortion1 consensus
 * Daniel Huson, 7.2007
 */
public class Distortion1Consensus implements IConsensusTreeMethod {
    public static final String NAME = "Distortion1";

    /**
     * constructor
     */
    public Distortion1Consensus() {
        super();
    }

    /**
     * applies the  distortion-1 consensus method to obtain a tree
     *
     * @return consensus
     */
    public PhyloTree apply(Document doc, TreeData[] trees) throws CanceledException {
        doc.notifyTasks("Distortion1 consensus", "");
        ZClosure zclosure = new ZClosure();
        zclosure.setOptionFilter(ZClosure.FILTER_DISTORTION);
        zclosure.setOptionMaxDistortionScore(1);
        zclosure.setOptionMinNumberTrees(trees.length);
        System.err.println("Distortion1 consensus input trees:" + trees.length);

        SplitSystem splits = zclosure.apply(doc.getProgressListener(), trees);
        Taxa taxa = zclosure.getTaxa();

        System.err.println("Distortion1 consensus splits: " + splits.size());
        PhyloTree tree = splits.createTreeFromSplits(taxa, doc.getProgressListener());
        tree.setName("distortion1-consensus");

        /*
        if (taxa.size() < 1000) {
            // todo: this seems broken...
            System.err.println("Using Neighbor-Net to compute ordering of leaves");
            NeighborNetCycle nnc = new NeighborNetCycle();
            int[] ordering = nnc.getNeighborNetOrdering(doc, taxa, splits);
            if (ordering != null)
                Utilities.applyOrderingToTree(taxa, ordering, tree);
        }
        */
        return tree;
    }
}
