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

package dendroscope.autumn;

import dendroscope.consensus.Cluster;
import dendroscope.consensus.Taxa;
import dendroscope.consensus.Utilities;
import jloda.graph.Graph;
import jloda.phylo.PhyloTree;
import jloda.util.Pair;

import java.io.IOException;
import java.util.BitSet;

/**
 * pre process input trees
 * Daniel Huson, 4.2011
 */
public class PreProcess {
    /**
     * apply preprocessing to two trees
     *
     * @param tree1
     * @param tree2
     * @param allTaxa
     * @return
     * @throws IOException
     */
    static public Pair<Root, Root> apply(PhyloTree tree1, PhyloTree tree2, Taxa allTaxa) throws IOException {

        if (tree1.getRoot() == null || tree2.getRoot() == null) {
            throw new IOException("Pre-processing failed, at least one of the trees is empty or unrooted");
        }

        Utilities.extractTaxa(1, tree1, allTaxa);
        Utilities.extractTaxa(2, tree2, allTaxa);

        Root root1 = Root.createACopy(new Graph(), tree1, allTaxa);
        root1.reorderSubTree();
        Root root2 = Root.createACopy(new Graph(), tree2, allTaxa);
        root2.reorderSubTree();

        return new Pair<Root, Root>(root1, root2);
    }

    /**
     * apply preprocessing to one tree
     *
     * @param tree1
     * @param allTaxa
     * @param mustHaveSameTaxa
     * @return
     * @throws IOException
     */
    static public Root apply(PhyloTree tree1, Taxa allTaxa, boolean mustHaveSameTaxa) throws IOException {

        if (tree1.getRoot() == null) {
            throw new IOException("Pre-processing failed, tree is empty or unrooted");
        }

        BitSet taxa = (BitSet) allTaxa.getBits().clone();

        BitSet taxa1 = Utilities.extractTaxa(1, tree1, allTaxa);
        if (mustHaveSameTaxa && taxa.cardinality() > 0 && !Cluster.contains(taxa, taxa1))
            throw new IOException("Pre-processing failed, trees has additional taxa");

        Root root1 = Root.createACopy(new Graph(), tree1, allTaxa);
        root1.reorderSubTree();
        return root1;
    }
}
