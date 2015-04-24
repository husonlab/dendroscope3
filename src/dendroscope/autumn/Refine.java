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
import dendroscope.core.TreeData;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.util.Basic;
import jloda.util.Pair;

import java.io.IOException;
import java.util.*;

/**
 * refine two trees
 * Daniel Huson, 4.2011
 */
public class Refine {
    /**
     * refine two trees
     *
     * @param tree1
     * @param tree2
     * @return refined trees
     */
    public static TreeData[] apply(TreeData tree1, TreeData tree2) throws IOException {
        // setup rooted trees with nodes labeled by taxa ids
        Taxa allTaxa = new Taxa();
        Pair<Root, Root> roots = PreProcess.apply(tree1, tree2, allTaxa);
        Root v1 = roots.getFirst();
        Root v2 = roots.getSecond();

        apply(v1, v2);

        v1.reorderSubTree();
        v2.reorderSubTree();

        // convert data-structures to final trees
        List<TreeData> result = PostProcess.apply(new Root[]{v1, v2}, allTaxa, false);
        return result.toArray(new TreeData[result.size()]);

    }

    /**
     * recursively does the work
     *
     * @param root1
     * @param root2
     */
    public static void apply(Root root1, Root root2) {
        applyRec(root1, root2, new HashSet<Pair<Root, Root>>());
        if (!root1.getTaxa().equals(root2.getTaxa()))
            throw new RuntimeException("Unequal taxon sets: " + Basic.toString(root1.getTaxa()) + " vs " + Basic.toString(root2.getTaxa()));
    }

    /**
     * refines two rooted trees with respect to each other
     *
     * @param v1
     * @param v2
     */
    private static void applyRec(Root v1, Root v2, Set<Pair<Root, Root>> compared) {
        Pair<Root, Root> pair = new Pair<Root, Root>(v1, v2);

        if (compared.contains(pair))
            return;
        else
            compared.add(pair);

        BitSet X = v1.getTaxa();
        BitSet Y = v2.getTaxa();

        if (X.cardinality() == 1 || Y.cardinality() == 1 || !X.intersects(Y))
            return; // doesn't apply

        // System.err.println("Refining with v1=" + Basic.toString(X) + "  v2=" + Basic.toString(Y));

        if (Cluster.contains(X, Y) && !X.equals(Y))  // X contains Y
        {
            // System.err.println("X contains Y");

            BitSet taxa1 = new BitSet();
            BitSet removedTaxa1 = new BitSet();
            LinkedList<Root> toPushDown = new LinkedList<Root>();
            int count = 0;
            for (Edge e1 = v1.getFirstOutEdge(); e1 != null; e1 = v1.getNextOutEdge(e1)) {
                Root w1 = (Root) e1.getTarget();
                if (Y.intersects(w1.getTaxa())) {
                    taxa1.or(w1.getTaxa());
                    removedTaxa1.or(w1.getRemovedTaxa());
                    toPushDown.add(w1);
                    count++;
                }
            }
            if (count > 1 && taxa1.equals(Y)) {
                Set<Root> needsReordering1 = new HashSet<Root>();
                // push down nodes
                Root u = v1.newNode();
                u.setTaxa(taxa1);
                u.setRemovedTaxa(removedTaxa1);
                Edge f = v1.newEdge(v1, u);
                f.setInfo(1);
                needsReordering1.add(v1);
                needsReordering1.add(u);
                for (Root w : toPushDown) {
                    needsReordering1.add((Root) w.getFirstInEdge().getSource());
                    w.deleteEdge(w.getFirstInEdge());
                    f = v1.newEdge(u, w);
                    f.setInfo(1);
                }
                //   System.err.println("Refined " + Basic.toString(Y));
                v1 = u;
                for (Root v : needsReordering1) {
                    v.reorderChildren();
                }
            }
        }
        if (Cluster.contains(Y, X) && !X.equals(Y))  // Y contains X
        {
            //   System.err.println("Y contains X");

            BitSet taxa2 = new BitSet();
            BitSet removedTaxa2 = new BitSet();
            LinkedList<Node> toPushDown = new LinkedList<Node>();
            int count = 0;
            for (Edge e2 = v2.getFirstOutEdge(); e2 != null; e2 = v2.getNextOutEdge(e2)) {
                Root w2 = (Root) e2.getTarget();
                if (X.intersects(w2.getTaxa())) {
                    taxa2.or(w2.getTaxa());
                    removedTaxa2.or(w2.getRemovedTaxa());
                    toPushDown.add(w2);
                    count++;
                }
            }
            if (count > 1 && taxa2.equals(X)) {
                Set<Root> needsReordering2 = new HashSet<Root>();
                // push down nodes
                Root u = v2.newNode();
                u.setTaxa(taxa2);
                u.setRemovedTaxa(removedTaxa2);
                Edge f = v2.newEdge(v2, u);
                f.setInfo(2);
                needsReordering2.add(v2);
                needsReordering2.add(u);
                for (Node w : toPushDown) {
                    needsReordering2.add((Root) w.getFirstInEdge().getSource());
                    v2.deleteEdge(w.getFirstInEdge());
                    f = v2.newEdge(u, w);
                    f.setInfo(2);
                }
                //    System.err.println("Refined " + Basic.toString(X));
                v2 = u;
                for (Root v : needsReordering2) {
                    v.reorderChildren();
                }
            }
        }

        for (Edge e1 = v1.getFirstOutEdge(); e1 != null; e1 = v1.getNextOutEdge(e1)) {
            Root w1 = (Root) e1.getTarget();
            if (w1.getTaxa().intersects(Y)) {
                applyRec(w1, v2, compared);
            }
        }
        for (Edge e2 = v2.getFirstOutEdge(); e2 != null; e2 = v2.getNextOutEdge(e2)) {
            Root w2 = (Root) e2.getTarget();
            if (w2.getTaxa().intersects(X)) {
                applyRec(v1, w2, compared);
            }
        }
    }
}
