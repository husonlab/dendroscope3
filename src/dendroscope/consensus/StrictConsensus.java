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
 * strict consensus
 * Daniel Huson, 7.2007
 */
public class StrictConsensus implements IConsensusTreeMethod {
    public static final String NAME = "Strict";

    /**
     * constructor
     */
    public StrictConsensus() {
        super();
    }

    /**
     * applies the  strict consensus method to obtain a tree
     *
     * @return consensus
     */
    public PhyloTree apply(Document doc, TreeData[] trees) throws CanceledException {
        doc.notifyTasks("Strict consensus", "");
        ZClosure zclosure = new ZClosure();
        zclosure.setOptionFilter(ZClosure.FILTER_STRICT);
        System.err.println("Strict consensus input trees:" + trees.length);

        SplitSystem splits = zclosure.apply(doc.getProgressListener(), trees);
        Taxa taxa = zclosure.getTaxa();

        System.err.println("Strict consensus splits: " + splits.size());
        // System.err.println("OUTGROUP: " + taxa.getLabel(taxa.maxId()));
        PhyloTree tree = splits.createTreeFromSplits(taxa, doc.getProgressListener());
        tree.setName("strict-consensus");
        return tree;
    }
}
