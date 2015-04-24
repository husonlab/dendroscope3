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
 * Computes a network consensus
 * Daniel Huson, 8.2007
 */
public class NetworkConsensus implements IConsensusTreeMethod {
    public static final String NAME = "Network";

    double optionPercentThreshold = 0;

    /**
     * constructor
     */
    public NetworkConsensus() {
        super();
    }

    /**
     * constructor
     */
    public NetworkConsensus(double percentThreshold) {
        super();
        this.optionPercentThreshold = percentThreshold;
    }

    /**
     * applies the  strict consensus method to obtain a tree
     *
     * @return consensus
     */
    public PhyloTree apply(Document doc, TreeData[] trees) throws CanceledException {
        doc.notifyTasks("Consensus super network", "");
        ZClosure zclosure = new ZClosure();
        zclosure.setOptionFilter(ZClosure.FILTER_PERCENT_THRESHOLD);
        zclosure.setOptionPercentThreshold(getOptionPercentThreshold());
        System.err.println("Consensus super network input trees:" + trees.length);
        SplitSystem splits = zclosure.apply(doc.getProgressListener(), trees);
        Taxa taxa = zclosure.getTaxa();
        System.err.println("Consensus super network splits: " + splits.size());

        PhyloTree tree = splits.createTreeFromSplits(taxa, doc.getProgressListener());

        /*
        todo: this seems broken

        if (taxa.size() < 1000) {
            System.err.println("Using Neighbor-Net to compute ordering of leaves");
            NeighborNetCycle nnc = new NeighborNetCycle();
            int[] ordering = nnc.getNeighborNetOrdering(doc, taxa, splits);
            if (ordering != null)
                Utilities.applyOrderingToTree(taxa, ordering, tree);
        }
        */

        if (getOptionPercentThreshold() > 0)
            tree.setName("consensus-" + getOptionPercentThreshold() + "-" + tree.getName());
        return tree;
    }

    public double getOptionPercentThreshold() {
        return optionPercentThreshold;
    }

    public void setOptionPercentThreshold(double optionPercentThreshold) {
        this.optionPercentThreshold = optionPercentThreshold;
    }
}
