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

package dendroscope.autumn.hybridnumber;

import dendroscope.autumn.Root;
import dendroscope.consensus.Cluster;
import jloda.graph.Edge;
import jloda.graph.Graph;
import jloda.util.Basic;

import java.util.BitSet;

/**
 * copy a tree with given taxa removed
 * Daniel Huson, 5.2011
 */
public class CopyWithTaxaRemoved {
    /**
     * creates a copy of the subtree below this node, with all given taxa removed
     *
     * @param taxa2remove
     * @return copy with named taxa
     */
    public static Root apply(Root v, BitSet taxa2remove) {
        if (v.getTaxa().equals(taxa2remove))
            return null; // removal of all taxa produces empty tree

        Root newRoot = new Root(new Graph());

        applyRec(v, newRoot, taxa2remove);
        newRoot.reorderSubTree();
        if (false) {
            try {
                newRoot.checkTree();
            } catch (RuntimeException ex) {
                System.err.println("Orig: " + v.toStringFullTreeX());
                System.err.println("To remove: " + Basic.toString(taxa2remove));
                System.err.println("New: " + newRoot.toStringFullTreeX());
                throw ex;
            }
        }
        return newRoot;
    }

    /**
     * recursively makes a copy
     *
     * @param v1
     * @param v2
     * @param taxa2remove
     */
    private static void applyRec(Root v1, Root v2, BitSet taxa2remove) {
        BitSet taxa = new BitSet();
        taxa.or(v1.getTaxa());
        taxa = Cluster.setminus(taxa, taxa2remove);
        v2.setTaxa(taxa);
        for (Edge e1 = v1.getFirstOutEdge(); e1 != null; e1 = v1.getNextOutEdge(e1)) {
            Root w1 = (Root) e1.getTarget();
            if (!taxa2remove.equals(w1.getTaxa())) {
                Root w2 = v2.newNode();
                v2.newEdge(v2, w2);
                applyRec(w1, w2, taxa2remove);
            }
        }
        // found leaf, if it was one of a pair, delete its sibling
        if (v2.getOutDegree() == 1) {
            if (v2.getInDegree() == 1) {
                v2.newEdge(v2.getFirstInEdge().getSource(), v2.getFirstOutEdge().getTarget());
                v2.deleteNode();
            } else // v2.getInDegree()==0
            {
                Root w2 = (Root) v2.getFirstOutEdge().getTarget();
                for (Edge e = w2.getFirstOutEdge(); e != null; e = w2.getNextOutEdge(e)) {
                    Root u2 = (Root) e.getTarget();
                    v2.newEdge(v2, u2);
                }
                w2.deleteNode();
            }
        }
    }

}
