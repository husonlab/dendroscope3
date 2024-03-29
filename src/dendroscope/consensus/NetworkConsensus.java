/*
 * NetworkConsensus.java Copyright (C) 2023 Daniel H. Huson
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
