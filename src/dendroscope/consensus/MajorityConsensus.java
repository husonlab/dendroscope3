/**
 * MajorityConsensus.java 
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
package dendroscope.consensus;

import dendroscope.core.Document;
import dendroscope.core.TreeData;
import jloda.phylo.PhyloTree;
import jloda.util.CanceledException;

/**
 * Computes the majority consensus
 * Daniel Huson, 7.2007
 */
public class MajorityConsensus implements IConsensusTreeMethod {
    public static final String NAME = "Majority";

    /**
     * constructor
     */
    public MajorityConsensus() {
        super();
    }

    /**
     * applies the  strict consensus method to obtain a tree
     *
     * @return consensus
     */
    public PhyloTree apply(Document doc, TreeData[] trees) throws CanceledException {
        doc.notifyTasks("Majority consensus", "");
        ZClosure zclosure = new ZClosure();
        zclosure.setOptionFilter(ZClosure.FILTER_MAJORITY);
        System.err.println("Majority consensus input trees:" + trees.length);

        SplitSystem splits = zclosure.apply(doc.getProgressListener(), trees);
        Taxa taxa = zclosure.getTaxa();

        System.err.println("Majority consensus splits: " + splits.size());
        PhyloTree tree = splits.createTreeFromSplits(taxa, doc.getProgressListener());
        tree.setName("majority-consensus");
        return tree;
    }
}
