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

package dendroscope.hybrid;

import jloda.graph.Edge;
import jloda.graph.Node;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

/**
 * Given a set of subtrees F of a rooted, bifurcating phylogenetic tree T, this
 * function checks whether all F are node disjoint in T.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class FastNodeDisjointCheck {

    HybridTree t1;
    HybridTree t2;
    final Vector<String> t1roots1 = new Vector<>();
    final Vector<String> t2roots = new Vector<>();
    HashSet<BitSet> tVCluster;
    final Vector<Vector<String>> leaves = new Vector<>();
    Vector<Vector<String>> nodes = new Vector<>();

    public boolean run(HybridTree t1, Vector<Node> roots, HybridTree t2) {

        HashSet<BitSet> testSet = new HashSet<>();

        Vector<Node> rootNodes = new Vector<>();

        for (Node v : roots) {
            while (this.hasRootChild(v, roots)) {
                boolean b = true;
                Vector<Node> vec = t1.getLeaves(v);
                for (Node c : vec) {
                    if (!roots.contains(c)) {
                        v = c;
                        b = false;
                    }
                }
                if (b)
                    break;
            }
            rootNodes.add(v);
        }

        for (Node r : rootNodes) {

            t1roots1.add(t1.getLabel(r));

            if (r.getOutDegree() != 0) {

                // compute subtree under v
                // HybridTree tV = t.getSubnetwork(v, true);
                // collect all cluster in tV appearing from each leaf of f up to
                // the
                // root
                // HashSet<BitSet> tVCluster = collectClusters(tV, f);
                HashSet<BitSet> tVCluster = collectClusters(t1, t2, roots,
                        r);

                this.t1 = t1;
                this.t2 = t2;

                // check whether collected cluster has already been collected by
                // another tree
                for (BitSet s : tVCluster) {
                    if (testSet.contains(s)) {
                        return false;
                    } else
                        testSet.add(s);
                }

            }

        }

        // all collected clusters are disjunct, forest is node-disjoint to t
        return true;

    }

    private HashSet<BitSet> collectClusters(HybridTree t1, HybridTree t2,
                                            Vector<Node> roots, Node t1Root) {

        HashSet<BitSet> s = new HashSet<>();
        Vector<Node> t1Leaves = getTaxa(t1, roots, t1Root);

        Vector<String> fTaxa = new Vector<>();
        for (Node v : t1Leaves)
            fTaxa.add(t1.getLabel(v));

        this.leaves.add(fTaxa);

        Vector<String> vec = new Vector<>();

        BitSet cluster = new BitSet();
        for (Node v : t2.computeSetOfLeaves()) {
            if (fTaxa.contains(t2.getLabel(v)))
                cluster.or(t2.getNodeToCluster().get(v));
        }

        Node t2Root = t2.findLCA(cluster);
        this.t2roots.add(t2.getLabel(t2Root));

        for (Node v : t2.computeSetOfLeaves()) {
            // check if leaf is contained in tree f
            if (fTaxa.contains(t2.getLabel(v))) {
                s.add(t2.getNodeToCluster().get(v));
                vec.add(t2.getLabel(v) + " ");
                // collect clusters up to the root
                while (!v.equals(t2Root)) {
                    v = v.getInEdges().next().getSource();
                    vec.add(t2.getLabel(v) + " ");
                    s.add(t2.getNodeToCluster().get(v));
                }
            }
            vec.add(" | ");
        }

        s.add(t2.getNodeToCluster().get(t2Root));

        return s;
    }

    private Vector<Node> getTaxa(HybridTree t1, Vector<Node> rootNodes,
                                 Node root) {

        Vector<Node> fTaxa = new Vector<>();
        initRec(t1, root, rootNodes, fTaxa);

        return fTaxa;
    }

    private void initRec(HybridTree t1, Node v, Vector<Node> rootNodes,
                         Vector<Node> fTaxa) {
        Iterator<Edge> it = v.getOutEdges();
        while (it.hasNext()) {
            Edge e = it.next();
            Node t = e.getTarget();
            if (!rootNodes.contains(t)) {
                if (t.getOutDegree() == 0) {
                    fTaxa.add(t);
                } else
                    initRec(t1, t, rootNodes, fTaxa);
            }

        }
    }

    private boolean hasRootChild(Node p, Vector<Node> rootNodes) {
        Iterator<Edge> it = p.getOutEdges();
        while (it.hasNext()) {
            if (rootNodes.contains(it.next().getTarget()))
                return true;
        }
        return false;
    }

}
