/**
 * GetAgreementForest.java 
 * Copyright (C) 2019 Daniel H. Huson
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
package dendroscope.hybrid;

import jloda.graph.Edge;
import jloda.graph.Node;

import java.util.*;

/**
 * This function computes all maximum acyclic agreement forests of a rooted,
 * bifurcating phylogenetic tree with a distinct size 'i'.
 *
 * @author Benjamin Albrecht, 6.2010
 */

/**
 * @author Benjamin Albrecht
 */
public class GetAgreementForest {

    private Vector<Edge> sortedEdges;
    private final Hashtable<Integer, Vector<BitSet>> newIndexToIllegalCombi = new Hashtable<>();
    private final HashSet<BitSet> newIllegalTrees = new HashSet<>();

    private Hashtable<Integer, Vector<BitSet>> indexToIllegalCombi;
    private final Hashtable<Vector<String>, HybridTree> leavesToTree = new Hashtable<>();
    private final HashSet<BitSet> illegalTrees = new HashSet<>();
    private boolean stopped = false;
    private int proc = 0;

    private boolean isStarted = false;
    private int numOfEdgeCombis = 0;
    private int numOfForestCombis = 0;

    private Vector<String> dummyLabels;

    boolean print = false;

    @SuppressWarnings("unchecked")
    public Hashtable<Integer, HashSet<Vector<HybridTree>>> run(HybridTree t1,
                                                               HybridTree t2, int numOfEdges,
                                                               Hashtable<Integer, Vector<BitSet>> indexToIllegalCombi,
                                                               View.Computation compVal, Vector<String> dummyLabels) throws Exception {

        this.indexToIllegalCombi = indexToIllegalCombi;
        this.dummyLabels = dummyLabels;

        HashSet<Vector<BitSet>> leafSets = new HashSet<>();
        Hashtable<Integer, HashSet<Vector<HybridTree>>> numberToForests = new Hashtable<>();

        // storing all edges sorted by its heights in the tree from the leaves
        // up to the root
        // -> necessary for having the right order when constructing a forest
        // out of an edge combination
        // -> necessary for having the right order when re-attaching each tree
        // of a forest to a resolved network
        sortedEdges = new Vector<>();
        Iterator<Node> it = t1.postOrderWalk();
        while (it.hasNext()) {
            Node v = it.next();
            if (v.getInDegree() == 1) {
                Edge e = v.getInEdges().next();
                if (!e.getSource().equals(t1.getRoot()))
                    sortedEdges.add(e);
                else if (compVal == View.Computation.rSPR_DISTANCE && t1.getLabel(e.getTarget()).equals("rho"))
                    sortedEdges.add(e);
            }
        }

        // collecting leaf-labelings...
        Vector<String> leafLabels = new Vector<>();
        it = t1.computeSetOfLeaves().iterator();
        while (it.hasNext())
            leafLabels.add(t1.getLabel(it.next()));

        // System.out.println("Computing Edge Combis - Start");
        // choosing each unsorted combination of edges with size 'numOfEdges'
        HashSet<BitSet> edgeCombis = chooseEdges(sortedEdges, numOfEdges, t1, compVal);
        // System.out.println("Computing Edge Combis - Finish");

        numOfEdgeCombis = edgeCombis.size();

        isStarted = true;
        // numOfEdgeCombis = edgeCombis.size();

        double i = 0;

        // constructing a forest out of each edge combination
        for (BitSet edgeSet : edgeCombis) {

            Vector<Integer> combi = new Vector<>();
            for (int index = 0; index < edgeSet.size(); index++) {
                if (edgeSet.get(index) == true)
                    combi.add(index);
            }

            i++;
            double size = edgeCombis.size();
            proc = (int) Math.round((i / size) * 100.0);

            if (stopped)
                break;

            // collecting the roots of each tree of the forest
            Vector<Node> rootNodes = new Vector<>();
            for (int index : combi)
                rootNodes.add(sortedEdges.get(index).getTarget());
            rootNodes.add(t1.getRoot());

            // checking if forest contains an illegal tree
            Hashtable<Node, Vector<String>> nodeToTaxa = new Hashtable<>();
            Vector<BitSet> treeSets = new Vector<>();
            for (Node v : rootNodes) {
                BitSet b = new BitSet(t1.getTaxaOrdering().size());
                Vector<String> taxa = getTaxa(t1, rootNodes, v);
                nodeToTaxa.put(v, taxa);
                for (String s : taxa)
                    b.set(t1.getTaxaOrdering().indexOf(s));
                treeSets.add(b);
            }

            Vector<BitSet> taxaSet = new Vector<>();
            boolean containsIllegalTree = false;
            for (BitSet treeSet : treeSets) {
                taxaSet.add(treeSet);
                if (illegalTrees.contains(treeSet)
                        || treeSet.cardinality() == 0) {
                    containsIllegalTree = true;
                    break;
                }
            }

            // generating vector of bit sets that is unique for each forest
            Collections.sort(taxaSet, new TaxaComparator());

            // checking if forest contains an illegal tree or has been computed
            // so far
            if (!containsIllegalTree && !leafSets.contains(taxaSet)) {

                leafSets.add(taxaSet);

                // checking the leaves of each tree of the forest
                boolean isLegal = true;
                for (Node v : rootNodes) {
                    Vector<String> taxa = nodeToTaxa.get(v);
                    if (taxa.size() == 0) {
                        isLegal = false;
                        break;
                    }
                    if (taxa.size() == 1 && taxa.get(0).equals("rho")
                            && compVal != View.Computation.rSPR_DISTANCE) {
                        isLegal = false;
                        break;
                    }
                }

                // checking if all trees of the forest are correct
                if (isLegal) {

                    numOfForestCombis++;

                    CheckAgreementForest cAf = new CheckAgreementForest();
                    if (cAf.run(t2, this, rootNodes, t1)) {

                        int h = combi.size();
                        if (compVal != View.Computation.rSPR_DISTANCE) {
                            for (int index : combi) {
                                Edge e = sortedEdges.get(index);
                                h += t1.getEdgeWeight(e);
                            }
                        }

                        HybridTree tCopy = new HybridTree(t1, false,
                                t1.getTaxaOrdering());
                        tCopy.update();

                        Vector<EasyTree> easyForest = new Vector<>();
                        for (int index : combi) {

                            Edge e = sortedEdges.get(index);

                            BitSet targetCluster = t1.getNodeToCluster().get(
                                    e.getTarget());
                            Node target = tCopy.getClusterToNode().get(
                                    targetCluster);

                            HybridTree subtree = tCopy
                                    .getSubtree(target, false);

                            // resolving each tree of the forest, such that
                            // each tree is a binary rooted tree
                            resolveTree(subtree, leafLabels);
                            easyForest.add(new EasyTree(subtree));

                            tCopy.deleteSubtree(target, null, false);

                        }
                        // resolving each tree of the forest, such that each
                        // tree is a binary rooted tree
                        resolveTree(tCopy, leafLabels);
                        easyForest.add(new EasyTree(tCopy));

                        Vector<EasyTree> acyclicOrder = null;
                        if (compVal != View.Computation.rSPR_DISTANCE)
                            acyclicOrder = new FastAcyclicCheck().run(
                                    easyForest, new EasyTree(t1), new EasyTree(
                                            t2), t1.getTaxaOrdering(), null, false);

                        if (compVal == View.Computation.rSPR_DISTANCE || acyclicOrder != null) {

                            Vector<HybridTree> forest = new Vector<>();
                            if (compVal != View.Computation.rSPR_DISTANCE) {
                                for (EasyTree eT : acyclicOrder) {
                                    HybridTree t = new HybridTree(
                                            eT.getPhyloTree(), false,
                                            t1.getTaxaOrdering());
                                    t.update();
                                    forest.add(t);
                                }
                            } else {
                                numberToForests.clear();
                                addDummyNodes(easyForest);
                                moveRootTreeToBack(easyForest);
                                for (EasyTree eT : easyForest) {
                                    HybridTree t = new HybridTree(
                                            eT.getPhyloTree(), false,
                                            t1.getTaxaOrdering());
                                    t.update();
                                    forest.add(t);
                                }
                            }

                            if (numberToForests.containsKey(h)) {
                                HashSet<Vector<HybridTree>> set = numberToForests
                                        .get(h);
                                HashSet<Vector<HybridTree>> newSet = (HashSet<Vector<HybridTree>>) set
                                        .clone();
                                numberToForests.remove(h);
                                newSet.add(forest);
                                numberToForests.put(h, newSet);
                            } else {
                                HashSet<Vector<HybridTree>> set = new HashSet<>();
                                set.add(forest);
                                numberToForests.put(h, set);
                            }

                            if (compVal == View.Computation.HYBRID_NUMBER && h == numOfEdges)
                                stopped = true;

                            if (compVal == View.Computation.rSPR_DISTANCE
                                    && forest.lastElement().computeSetOfLeaves().size() == 1)
                                stopped = true;

                        }

                    } else {
                        for (BitSet treeSet : cAf.getIllegalTrees()) {
                            illegalTrees.add(treeSet);
                            newIllegalTrees.add(treeSet);
                        }
                    }
                }

            }
        }

        return numberToForests;

    }

    private void addDummyNodes(Vector<EasyTree> forest) {
        for (String label : dummyLabels) {
            EasyTree eT = new EasyTree(new EasyNode(null, null, label));
            forest.add(eT);
        }
    }

    private void moveRootTreeToBack(Vector<EasyTree> forest) {

        int index = 0;
        for (EasyTree t : forest) {
            if (t.getLeaves().size() > 1) {
                Iterator<EasyNode> it = t.getRoot().getChildren().iterator();
                String l1 = it.next().getLabel();
                String l2 = it.next().getLabel();
                if (l1.equals("rho") || l2.equals("rho")) {
                    index = forest.indexOf(t);
                    break;
                }
            } else if (t.getRoot().getLabel().equals("rho")) {
                index = forest.indexOf(t);
                break;
            }
        }

        EasyTree rootTree = forest.get(index);
        forest.remove(rootTree);
        forest.add(forest.size(), rootTree);

    }

    @SuppressWarnings("unchecked")
    public HashSet<BitSet> getIllegalTrees() {
        HashSet<BitSet> newIllegalTreesCopy = (HashSet<BitSet>) newIllegalTrees
                .clone();
        newIllegalTrees.clear();
        return newIllegalTreesCopy;
    }

    public void setIllegalTrees(HashSet<BitSet> set) {
        for (BitSet b : set) {
            if (!illegalTrees.contains(b))
                illegalTrees.add(b);
        }
    }

    private Vector<String> getTaxa(HybridTree t1, Vector<Node> rootNodes,
                                   Node root) {

        Vector<String> fTaxa = new Vector<>();

        if (root.getOutDegree() != 0) {
            initRec(t1, root, rootNodes, fTaxa);
            Collections.sort(fTaxa);
        } else
            fTaxa.add(t1.getLabel(root));

        return fTaxa;
    }

    private void initRec(HybridTree t1, Node v, Vector<Node> rootNodes,
                         Vector<String> fTaxa) {
        Iterator<Edge> it = v.getOutEdges();
        while (it.hasNext()) {
            Edge e = it.next();
            Node t = e.getTarget();
            if (!rootNodes.contains(t)) {
                if (t.getOutDegree() == 0) {
                    fTaxa.add(t1.getLabel(t));
                } else
                    initRec(t1, t, rootNodes, fTaxa);
            }

        }
    }

    private boolean resolveTree(HybridTree f, Vector<String> leafLabels) {

        // removing all illegal leaves
        boolean toCorrect = true;
        while (toCorrect) {
            toCorrect = false;
            for (Node v : f.computeSetOfLeaves()) {
                if (!leafLabels.contains(f.getLabel(v))) {
                    resolveTreeRec(v, f, leafLabels);
                    toCorrect = true;
                    break;
                }
            }
        }

        // removing all nodes with only one in- and out-edge
        Iterator<Node> it = f.postOrderWalk();
        while (it.hasNext()) {
            Node v = it.next();
            if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
                Edge eP = v.getInEdges().next();
                Edge eC = v.getOutEdges().next();
                Node p = eP.getSource();
                Node c = eC.getTarget();
                f.deleteEdge(eP);
                f.deleteEdge(eC);
                f.deleteNode(v);
                f.newEdge(p, c);
            }
        }

        // checking if tree is empty, if so then regarded forest is illegal
        if (f.getNumberOfNodes() == 0)
            return false;

        // resolving the root such that it has in-degree 0 and out-degree 2
        setRoot(f);
        if (f.getRoot().getOutDegree() == 1) {
            Node v = f.getRoot();
            Edge e = v.getOutEdges().next();
            Node c = e.getTarget();
            f.deleteEdge(e);
            f.deleteNode(v);
            f.setRoot(c);
        }

        // checking if tree consists only of the outgroup 'rho'
        if (f.getLabel(f.getRoot()) != null) {
            if (f.getLabel(f.getRoot()).equals("rho"))
                return false;
        }

        f.update();

        return true;

    }

    private void resolveTreeRec(Node v, HybridTree f, Vector<String> leafLabels) {
        if (!leafLabels.contains(f.getLabel(v))) {
            if (v.getDegree() == 0)
                f.deleteNode(v);
            else if (v.getOutDegree() == 0 && v.getInDegree() == 1) {
                Edge e = v.getInEdges().next();
                Node p = e.getSource();
                f.deleteEdge(e);
                f.deleteNode(v);
                if (p.getOutDegree() == 0 && p.getInDegree() == 1)
                    resolveTreeRec(p, f, leafLabels);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private HashSet<BitSet> chooseEdges(Vector<Edge> sortedEdges,
                                        int numOfEdges, HybridTree t, View.Computation compVal) {

        HashSet<BitSet> edgeChoice = new HashSet<>();

        // generate edge combinations consisting of one edge
        for (Edge e : sortedEdges) {
            // edges adjacent to the root are not chosen
            if (!e.getSource().equals(t.getRoot()) || compVal == View.Computation.rSPR_DISTANCE) {
                BitSet b = new BitSet(sortedEdges.size());
                b.set(sortedEdges.indexOf(e));
                edgeChoice.add(b);
            }
        }

        // computing each edge combinations with size 'i'
        for (int i = 1; i < numOfEdges; i++) {
            HashSet<BitSet> newEdgeCombis = computeNewEdgeChose(edgeChoice,
                    sortedEdges, t);
            edgeChoice.clear();
            edgeChoice = (HashSet<BitSet>) newEdgeCombis.clone();
            newEdgeCombis.clear();
        }

        HashSet<BitSet> toRemove = new HashSet<>();
        for (BitSet set : edgeChoice) {
            boolean isLegal = true;
            int setIndex = set.nextSetBit(0);
            while (setIndex != -1) {
                if (!isLegalCombination(set, setIndex)) {
                    isLegal = false;
                    break;
                }
                setIndex = set.nextSetBit(setIndex + 1);
            }

            if (!isLegal)
                toRemove.add(set);
        }

        for (BitSet set : toRemove)
            edgeChoice.remove(set);

        return edgeChoice;
    }

    private HashSet<BitSet> computeNewEdgeChose(HashSet<BitSet> edgeChose,
                                                Vector<Edge> sortedEdges, HybridTree t) {

        HashSet<BitSet> newEdgeChose = new HashSet<>();

        for (BitSet b : edgeChose) {
            for (Edge e : sortedEdges) {
                if (!b.get(sortedEdges.indexOf(e))
                        && !e.getSource().equals(t.getRoot())) {

                    // adding edge such that all so far added edges are sorted
                    // after its position of a post order tree walk
                    BitSet bNew = (BitSet) b.clone();
                    int index = sortedEdges.indexOf(e);

                    // adding the computed edge-combination if it is a new one
                    if (!newEdgeChose.contains(bNew)) {
                        bNew.set(index);
                        newEdgeChose.add(bNew);
                    }
                }

            }
        }
        return newEdgeChose;
    }

    private boolean isLegalCombination(BitSet bNew, int index) {
        if (indexToIllegalCombi.containsKey(index)) {
            for (BitSet b : indexToIllegalCombi.get(index)) {
                BitSet test = (BitSet) b.clone();
                test.and(bNew);
                if (test.equals(b))
                    return false;
                else if (b.cardinality() > bNew.cardinality())
                    return true;
            }
        }
        return true;
    }

    private boolean setRoot(HybridTree n) {
        Iterator<Node> it = n.postOrderWalk();
        while (it.hasNext()) {
            Node v = it.next();
            if (v.getInDegree() == 0) {
                n.setRoot(v);
                return true;
            }
        }
        return false;
    }

    public void printTree(HybridTree t) {

        Iterator<Node> it = t.computeSetOfLeaves().iterator();
        Vector<String> taxa = new Vector<>();
        while (it.hasNext())
            taxa.add(t.getLabel(it.next()));
        Collections.sort(taxa);

        System.out.println("Taxa: " + taxa);

    }

    public void putLeavesToTree(Vector<String> leaves, HybridTree t) {
        if (!leavesToTree.containsKey(leaves))
            leavesToTree.put(leaves, t);
    }

    public HybridTree getLeavesToTree(Vector<String> leaves) {
        return leavesToTree.get(leaves);
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public int getProc() {
        return proc;
    }

    public Hashtable<Vector<String>, HybridTree> getLeavesToTree() {
        return leavesToTree;
    }

    public Vector<Edge> getSortedEdges() {
        return sortedEdges;
    }

    @SuppressWarnings("unchecked")
    public Hashtable<Integer, Vector<BitSet>> getNewIllegalTrees() {
        Hashtable<Integer, Vector<BitSet>> newIndexToIllegalCombiCopy = (Hashtable<Integer, Vector<BitSet>>) newIndexToIllegalCombi
                .clone();
        newIndexToIllegalCombi.clear();
        return newIndexToIllegalCombiCopy;
    }

    @SuppressWarnings("unchecked")
    public void reportIllegalTree(int index, BitSet b) {
        if (newIndexToIllegalCombi.containsKey(index)) {
            Vector<BitSet> v = (Vector<BitSet>) newIndexToIllegalCombi.get(
                    index).clone();
            if (!v.contains(b)) {
                newIndexToIllegalCombi.remove(index);
                v.add(b);
                newIndexToIllegalCombi.put(index, v);
            }
        } else {
            Vector<BitSet> v = new Vector<>();
            v.add(b);
            newIndexToIllegalCombi.put(index, v);
        }
    }

    public boolean isStarted() {
        return isStarted;
    }

    public int getNumOfEdgeCombis() {
        return numOfEdgeCombis;
    }

    public int getNumOfForestCombis() {
        return numOfForestCombis;
    }

}
