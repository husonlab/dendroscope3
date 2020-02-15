/*
 *   FastGetAgreementForest.java Copyright (C) 2020 Daniel H. Huson
 *
 *   (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import jloda.graph.Edge;
import jloda.graph.Node;

import java.util.*;

public class FastGetAgreementForest {

    private boolean stop = false;
    private int compValue;
    private SiblingMemory sibMem;

    private final HashSet<BitSet> forestSets = new HashSet<>();
    private final Hashtable<String, Vector<String>> taxonToTaxa = new Hashtable<>();
    private final Hashtable<Integer, HashSet<Vector<HybridTree>>> MAAFs = new Hashtable<>();

    private String rootLabel;
    private int k;
    private int recCalls = 0;

    private HybridTree tree1, tree2;

    public Hashtable<Integer, HashSet<Vector<HybridTree>>> run(HybridTree t1,
                                                               HybridTree t2, int forestSize, int compValue) {

        this.compValue = compValue;
        this.tree1 = t1;
        this.tree2 = t2;
        k = forestSize;

        rootLabel = t2.getLabel(t2.getRoot());
        sibMem = new SiblingMemory(t1.getTaxaOrdering());

        if (!(new IsomorphismCheck()).run(t1, t2)) {
            while (MAAFs.size() == 0) {
                System.out.println("searching forest of size " + k + "...");

                sibMem = new SiblingMemory(t1.getTaxaOrdering());

                HybridTree forestTree = new HybridTree(t2, false,
                        t2.getTaxaOrdering());
                HybridTree h1 = new HybridTree(t1, false, t1.getTaxaOrdering());
                HybridTree h2 = new HybridTree(t2, false, t2.getTaxaOrdering());

                initNodeLabelings(forestTree, h1, h2);

                Vector<HybridTree> t2Forest = new Vector<>();
                t2Forest.add(forestTree);

                stop = false;
                computeMAAF(h1, t2Forest, false, 0);

                // System.out.println("#recCalls: " + recCalls);
                recCalls = 0;

                if (MAAFs.size() == 0)
                    k += 1;
            }
        } else {
            Vector<HybridTree> forest = new Vector<>();
            forest.add(new HybridTree(t2, false, t2.getTaxaOrdering()));
            addMAAF(forest, 0);
        }

        // System.out.println();
        // System.out.println("MAAFs");
        // System.out.println("*******************");
        // for (int j : MAAFs.keySet()) {
        // System.out.println(j + " ----------------");
        // for (Vector<HybridTree> vec : MAAFs.get(j)) {
        // System.out.println(" ~~~~~~~~~~~~~~ ");
        // for (HybridTree t : vec)
        // System.out.println(t + ";");
        // System.out.println(" ~~~~~~~~~~~~~~ ");
        // }
        // System.out.println(MAAFs.get(j).size() + " ----------------");
        // }
        // System.out.println("#KEYs: " + MAAFs.size());
        // System.out.println("*******************");
        //
        // if (compValue == 1)
        // System.out.println("Hybrid Number: " + (this.k - 1));
        // else if (compValue == 2)
        // System.out.println("rSPR Distance: " + (this.k - 1));

        return MAAFs;

    }

    @SuppressWarnings("unchecked")
    private void addMAAF(Vector<HybridTree> forest, double weight) {

        BitSet b = sibMem.getForestSet(forest);
        if (!forestSets.contains(b)) {

            forestSets.add(b);

            if (compValue != 2) {
                moveRootTreeToBack(forest);
                for (HybridTree t : forest)
                    t.update();
            }

            int index = forest.size() + (int) weight;
            if (MAAFs.containsKey(index)) {
                HashSet<Vector<HybridTree>> set = (HashSet<Vector<HybridTree>>) MAAFs
                        .get(index).clone();
                MAAFs.remove(index);
                set.add(forest);
                MAAFs.put(index, set);
            } else {
                HashSet<Vector<HybridTree>> set = new HashSet<>();
                set.add(forest);
                MAAFs.put(index, set);
            }

            if (compValue == 2)
                stop = true;

            if (compValue == 1 && index == k)
                stop = true;

        }

    }

    private void moveRootTreeToBack(Vector<HybridTree> forest) {

        int index = 0;
        for (HybridTree t : forest) {
            if (t.computeSetOfLeaves().size() > 1) {
                Iterator<Edge> it = t.getRoot().outEdges().iterator();
                String l1 = t.getLabel(it.next().getTarget());
                String l2 = t.getLabel(it.next().getTarget());
                if (l1.equals("rho") || l2.equals("rho")) {
                    index = forest.indexOf(t);
                    break;
                }
            }
        }

        HybridTree rootTree = forest.get(index);
        forest.remove(rootTree);
        forest.add(forest.size(), rootTree);

    }

    private void initNodeLabelings(HybridTree t1, HybridTree t2, HybridTree t3) {
        int i = 0;
        Vector<String> t1Taxa = getTaxa(t1);
        for (Node v : t1.nodes()) {
            if (v.getOutDegree() != 0 && v.getInDegree() != 0) {
                while (t1Taxa.contains(String.valueOf(i)))
                    i++;
                t1.setLabel(v, String.valueOf(i));
                i++;
            }
        }
        Vector<String> t2Taxa = getTaxa(t1);
        for (Node v : t2.nodes()) {
            if (v.getOutDegree() != 0 && v.getInDegree() != 0) {
                while (t2Taxa.contains(String.valueOf(i)))
                    i++;
                t2.setLabel(v, String.valueOf(i));
                i++;
            }
        }
        Vector<String> t3Taxa = getTaxa(t1);
        for (Node v : t3.nodes()) {
            if (v.getOutDegree() != 0 && v.getInDegree() != 0) {
                while (t3Taxa.contains(String.valueOf(i)))
                    i++;
                t3.setLabel(v, String.valueOf(i));
                i++;
            }
        }
    }

    private Vector<String> getTaxa(HybridTree t) {
        Vector<String> taxa = new Vector<>();
        for (Node v : t.computeSetOfLeaves())
            taxa.add(t.getLabel(v));
        return taxa;
    }

    private void computeMAAF(HybridTree t1, Vector<HybridTree> forest,
                             boolean newComponents, double weight) {

        // System.out.println(t1 + ";");

        if (!sibMem.contains(t1, forest) && t1.computeSetOfLeaves().size() > 2 && !stop) {

            if (forest.size() <= k) {

                Siblings sibs = new Siblings();
                sibs.init(t1, forest);

                // System.out.println("");
                // System.out.println("--------------");
                // System.out.println("Forests");
                // for (HybridTree t : forest)
                // System.out.println(t + ";");
                // System.out.println("--------------");
                // System.out.println("Trees");
                // System.out.println(t1 + ";");
                // System.out.println(t2 + ";");
                // System.out.println("--------------");
                // System.out.println(sibs.getSiblingsOfT1());
                // System.out.println(sibs.getSiblingsOfForest());
                // System.out.println("--------------");

                // System.out.println("1. removeNodes");
                removeSingletons(t1, forest, sibs);
                // System.out.println(newT1 + ";");

                if (!sibMem.contains(t1, forest) && t1.computeSetOfLeaves().size() != 2) {

                    double kPrime = 0;
                    // if (newComponents)
                    // kPrime = new ForestApproximation(taxonToTaxa).run(
                    // tree1, tree2, t1, forest, compValue);
                    if (Math.floor(kPrime / 3) < k) {

                        recCalls++;
                        sibMem.addEntry(t1, forest);
                        sibs.updateSiblings(t1, forest, true);
                        Vector<String> t1Sib = sibs.popSibling();
                        // System.out.println(t1Sib);

                        Node v1 = sibs.getForestLeaf(t1Sib.get(0));
                        HybridTree t = (HybridTree) v1.getOwner();
                        String s = t.getLabel(getNeighbour(v1));

                        if (sibMem.compareTaxa(s, t1Sib.get(1))) {
                            // System.out.println("1. contracting " +
                            // t1Sib);
                            contractSib(t1, forest, t1Sib, sibs);
                            computeMAAF(t1, forest, false, weight);
                        } else if (forest.size() < k) {
                            // System.out.println("2. cutting " + t1Sib);
                            cutEdges(t1, sibs, t1Sib, forest, weight);
                        }
                    } else
                        System.out.println("ABORT: " + forest.size());

                } else if (t1.computeSetOfLeaves().size() == 2 && forest.size() <= k
                        && !stop) {
                    // reattachTrees(forest);
                    if (compValue != 2) {
                        for (HybridTree t : forest)
                            extractTree(t);
                        (new CuttingCycles()).run(forest, tree1, tree2);
                    }
                    if (forest.size() == k && !stop)
                        addMAAF(forest, weight);
                }

            }
        } else if (t1.computeSetOfLeaves().size() == 2 && forest.size() <= k && !stop) {
            // reattachTrees(forest);
            if (compValue != 2) {
                for (HybridTree t : forest)
                    extractTree(t);
                (new CuttingCycles()).run(forest, tree1, tree2);
            }
            if (forest.size() == k && !stop)
                addMAAF(forest, weight);
        }

    }

    private void removeSingletons(HybridTree t1, Vector<HybridTree> forest,
                                  Siblings sibs) {

        HashSet<String> forestSingeltons = new HashSet<>();
        for (HybridTree t : forest) {
            if (!Objects.equals(t.getLabel(t.getRoot()), "rho"))
                forestSingeltons.add(t.getLabel(t.getRoot()));
        }

        // System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~");
        // System.out.println(forestSingeltons);
        // for (Node v : t1.computeSetOfLeaves())
        // System.out.println(t1.getLabel(v));
        // System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~");

        Vector<String> deletedTaxa = new Vector<>();
        boolean isActive = true;
        while (isActive) {
            isActive = false;
            for (String s : forestSingeltons) {
                Node v1 = sibs.getT1Leaf(s);
                if (v1 != null && !deletedTaxa.contains(s)) {
                    removeNode(t1, v1);
                    deletedTaxa.add(s);
                    // isActive = true;
                }
            }
        }

    }

    private void removeNode(HybridTree t, Node v) {
        if (t.computeSetOfLeaves().size() > 2) {
            Edge e = v.getFirstInEdge();
            Node p = e.getSource();
            t.deleteEdge(e);
            t.deleteNode(v);
            removeNeedlessNode(t, p);
        }
    }

    private void removeNeedlessNode(HybridTree t, Node v) {
        if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
            Edge inEdge = v.getFirstInEdge();
            Node pP = inEdge.getSource();
            Edge outEdge = v.getFirstOutEdge();
            Node c = outEdge.getTarget();
            t.deleteEdge(inEdge);
            t.deleteEdge(outEdge);
            t.deleteNode(v);
            t.newEdge(pP, c);
        } else if (v.getInDegree() == 0 && v.getOutDegree() == 1) {
            Edge outEdge = v.getFirstOutEdge();
            Node c = outEdge.getTarget();
            t.deleteEdge(outEdge);
            t.deleteNode(v);
            t.setRoot(c);
        }
    }

    private void cutEdges(HybridTree t1, Siblings sibs, Vector<String> t1Sib,
                          Vector<HybridTree> forest, double weight) {

        String taxa1 = t1Sib.get(0);
        String taxa2 = t1Sib.get(1);

        HybridTree owner1 = (HybridTree) sibs.getForestLeaf(taxa1).getOwner();
        HybridTree owner2 = (HybridTree) sibs.getForestLeaf(taxa2).getOwner();

        int h1 = getHeight(sibs.getForestLeaf(taxa1));
        int h2 = getHeight(sibs.getForestLeaf(taxa2));

        Vector<HybridTree> forestCopy;
        Node cuttingNode;
        if (!owner1.equals(owner2)) {
            // System.out.println("case-1");
            forestCopy = copyForest(forest);
            cuttingNode = getCuttingNode(forestCopy, sibs, taxa1);
            cutForest(cuttingNode, forestCopy, copyTree(t1), sibs, t1Sib,
                    weight);

            cuttingNode = getCuttingNode(forest, sibs, taxa2);
            cutForest(cuttingNode, forest, t1, sibs, t1Sib, weight);
        } else {

            String onlyPendant = null;
            if (owner1.equals(owner2)) {
                if (h1 > h2)
                    onlyPendant = getOnlyPendant(sibs.getForestLeaf(taxa1),
                            sibs.getForestLeaf(taxa2), owner1);
                else
                    onlyPendant = getOnlyPendant(sibs.getForestLeaf(taxa2),
                            sibs.getForestLeaf(taxa1), owner1);
            }

            if (onlyPendant != null && compValue != 0) {
                if (compValue == 1) {
                    forestCopy = copyForest(forest);
                    cuttingNode = getCuttingPendant(forestCopy, sibs,
                            onlyPendant);
                    cutForest(cuttingNode, forestCopy, copyTree(t1), sibs,
                            t1Sib, weight);

                    cuttingNode = getCuttingNode(forest, sibs, taxa2);
                    cutForest(cuttingNode, forest, t1, sibs, t1Sib, weight);
                } else {
                    // System.out.println("case-2");
                    // cutForest(onlyPendant, copyForest(forest), copyTree(t1),
                    // sibs, t1Sib);
                    cuttingNode = getCuttingPendant(forest, sibs, onlyPendant);
                    cutForest(cuttingNode, forest, t1, sibs, t1Sib, weight);
                }
                // else {
                // forestCopy = copyForest(forest);
                // cuttingNode = getCuttingPendant(forestCopy, sibs,
                // onlyPendant);
                // cutForest(cuttingNode, forestCopy, copyTree(t1), sibs,
                // t1Sib);
                //
                // forestCopy = copyForest(forest);
                // cuttingNode = getCuttingNode(forestCopy, sibs, taxa1);
                // cutForest(cuttingNode, forestCopy, copyTree(t1), sibs,
                // t1Sib);
                //
                // forestCopy = copyForest(forest);
                // cuttingNode = getCuttingNode(forestCopy, sibs, taxa2);
                // cutForest(cuttingNode, forestCopy, copyTree(t1), sibs,
                // t1Sib);
                // }
            } else {
                // System.out.println("Pendants: " + pendants);
                forestCopy = copyForest(forest);
                Vector<Node> pendants = getPendantNodes(taxa1, taxa2, sibs,
                        forestCopy);
                cutForest(pendants, forestCopy, copyTree(t1), sibs, t1Sib,
                        weight);

                forestCopy = copyForest(forest);
                cuttingNode = getCuttingNode(forestCopy, sibs, taxa1);
                cutForest(cuttingNode, forestCopy, copyTree(t1), sibs, t1Sib,
                        weight);

                cuttingNode = getCuttingNode(forest, sibs, taxa2);
                cutForest(cuttingNode, forest, t1, sibs, t1Sib, weight);
            }

            // cutForest(pendants, copyForest(forest), t1, t2, sibs);
            // cutForest(taxa1, copyForest(forest), t1, t2, sibs);
            // cutForest(taxa2, copyForest(forest), t1, t2, sibs);

        }

    }

    private Node getCuttingNode(Vector<HybridTree> forest, Siblings sibs,
                                String taxa) {
        sibs.updateTaxa(forest);
        return sibs.getForestLeaf(taxa);
    }

    private Node getCuttingPendant(Vector<HybridTree> forest, Siblings sibs,
                                   String taxa) {
        sibs.updateTaxa(forest);
        return getNeighbour(sibs.getForestLeaf(taxa));
    }

    private Vector<HybridTree> copyForest(Vector<HybridTree> forest) {
        Vector<HybridTree> v = new Vector<>();
        for (HybridTree t : forest)
            v.add(copyTree(t));
        return v;
    }

    private HybridTree copyTree(HybridTree t) {
        return new HybridTree(t, false, t.getTaxaOrdering());
    }

    private void cutForest(Node v, Vector<HybridTree> forest, HybridTree t1,
                           Siblings sibs, Vector<String> t1Sib, double weight) {

        sibs.updateTaxa(t1);
        HybridTree t = (HybridTree) v.getOwner();
        double newWeight = weight;

        if (t.computeSetOfLeaves().size() > 1) {
            Node p = v.getFirstInEdge().getSource();

            if (compValue != 2 && !t.getLabel(p).equals(rootLabel)) {
                HybridTree subtree = t.getSubtree(v, false);

                // if(t.getWeight(v.getFirstInEdge())!=0){
                // System.out.println();
                // System.out.println("Weight: "+t.getWeight(v.getFirstInEdge()));
                // }

                if (compValue != 2)
                    newWeight += t.getWeight(v.getFirstInEdge()) - 1;

                t.deleteSubtree(v, null, false);
                forest.add(subtree);
                removeNeedlessNode(t, p);

                computeMAAF(t1, forest, true, newWeight);

            } else if (compValue == 2) {
                HybridTree subtree = t.getSubtree(v, false);

                if (compValue != 2)
                    newWeight += t.getWeight(v.getFirstInEdge()) - 1;

                t.deleteSubtree(v, null, false);
                forest.add(subtree);
                removeNeedlessNode(t, p);

                computeMAAF(t1, forest, true, newWeight);
            }

        }

    }

    private void cutForest(Vector<Node> nodes, Vector<HybridTree> forest,
                           HybridTree t1, Siblings sibs, Vector<String> t1Sib, double weight) {

        double newWeight = weight;
        for (Node v : nodes) {
            HybridTree t = (HybridTree) v.getOwner();

            if (t.computeSetOfLeaves().size() > 1) {
                Node p = v.getFirstInEdge().getSource();
                if (compValue != 2 && !t.getLabel(p).equals(rootLabel)) {
                    HybridTree subtree = t.getSubtree(v, false);

                    if (compValue != 2)
                        newWeight += t.getWeight(v.getFirstInEdge()) - 1;

                    t.deleteSubtree(v, null, false);
                    forest.add(subtree);
                    removeNeedlessNode(t, p);
                } else if (compValue == 2) {
                    HybridTree subtree = t.getSubtree(v, false);

                    if (compValue != 2)
                        newWeight += t.getWeight(v.getFirstInEdge()) - 1;

                    t.deleteSubtree(v, null, false);
                    forest.add(subtree);
                    removeNeedlessNode(t, p);
                }
            }

        }

        sibs.updateTaxa(t1);
        contractSib(t1, forest, t1Sib, sibs);
        computeMAAF(t1, forest, true, newWeight);

    }

    private Node getNeighbour(Node v) {

        Node p = v.getFirstInEdge().getSource();
        Iterator<Edge> it = p.outEdges().iterator();

        Node c = it.next().getTarget();
        if (!c.equals(v))
            return c;

        return it.next().getTarget();
    }

    private String getOnlyPendant(Node v1, Node v2, HybridTree t) {

        Node p1 = v1.getFirstInEdge().getSource();
        Node p2 = v2.getFirstInEdge().getSource();

        if (p1.getInDegree() == 1) {
            Node v = p1.getFirstInEdge().getSource();
            if (v.equals(p2))
                return t.getLabel(v1);
        }

        return null;
    }

    private Vector<Node> getPendantNodes(String taxa1, String taxa2,
                                         Siblings sibs, Vector<HybridTree> forest) {

        sibs.updateTaxa(forest);
        Vector<Node> pendantNodes = new Vector<>();
        Node v1 = sibs.getForestLeaf(taxa1);
        Node v2 = sibs.getForestLeaf(taxa2);

        // HybridTree t = (HybridTree) v1.getOwner();

        Vector<Node> upperNodesV1 = getUpperNodes(v1);
        Vector<Node> upperNodesV2 = getUpperNodes(v2);

        Node pendant = v1;
        Node p = pendant.getFirstInEdge().getSource();
        while (!upperNodesV2.contains(p)) {
            pendantNodes.add(getNeighbour(pendant));
            pendant = p;
            p = pendant.getFirstInEdge().getSource();
        }

        pendant = v2;
        p = pendant.getFirstInEdge().getSource();
        while (!upperNodesV1.contains(p)) {
            pendantNodes.add(getNeighbour(pendant));
            pendant = p;
            p = pendant.getFirstInEdge().getSource();
        }

        return pendantNodes;
    }

    private Vector<Node> getUpperNodes(Node v) {
        Vector<Node> upperNodes = new Vector<>();
        while (v.getInDegree() == 1) {
            v = v.getFirstInEdge().getSource();
            upperNodes.add(v);
        }
        return upperNodes;
    }

    private void contractSib(HybridTree t1, Vector<HybridTree> forest,
                             Vector<String> t1Sib, Siblings sibs) {

        contractLeaves(t1, t1Sib, sibs);
        sibs.updateTaxa(t1, forest);

    }

    private void contractLeaves(HybridTree t1, Vector<String> t1Sib,
                                Siblings sibs) {

        // contracting t1

        Node v1 = sibs.getT1Leaf(t1Sib.get(0));
        Node v2 = sibs.getT1Leaf(t1Sib.get(1));

        Vector<String> taxa = new Vector<>();
        taxa.add(t1.getLabel(v1));
        taxa.add(t1.getLabel(v2));
        Collections.sort(taxa);

        Edge e = v1.getFirstInEdge();
        Node p = e.getSource();

        t1.deleteEdge(e);
        t1.deleteNode(v1);

        e = v2.getFirstInEdge();
        t1.deleteEdge(e);
        t1.deleteNode(v2);

        String s = "";
        s = s.concat(taxa.get(0));
        s = s.concat("+");
        s = s.concat(taxa.get(1));

        // putNodeToLeaves(s, taxa.get(0), taxa.get(1));

        t1.setLabel(p, s);
        sibMem.addTreeLabel(s);

        // setting forest label

        Node f1 = sibs.getForestLeaf(t1Sib.get(0));
        Edge eF = f1.getFirstInEdge();
        Node fP = eF.getSource();
        HybridTree f = (HybridTree) fP.getOwner();
        f.setLabel(fP, s);

        f.deleteEdge(eF);
        f.deleteNode(f1);

        Node f2 = sibs.getForestLeaf(t1Sib.get(1));
        eF = f2.getFirstInEdge();

        f.deleteEdge(eF);
        f.deleteNode(f2);

        taxonToTaxa.put(s, taxa);
        sibMem.addTaxon(s, taxa);

    }

    private int getHeight(Node v) {
        int h = 0;
        if (v.getInDegree() != 0) {
            Node p = v.getFirstInEdge().getSource();
            h = 1;
            while (p.getInDegree() != 0) {
                p = p.getFirstInEdge().getSource();
                h++;
            }
            return h;
        }
        return h;
    }

    private void extractTree(HybridTree t) {
        for (Node v : t.computeSetOfLeaves()) {
            if (taxonToTaxa.containsKey(t.getLabel(v)))
                extractNode(t, v);
        }

    }

    private void extractNode(HybridTree t, Node v) {
        Node v1 = t.newNode();
        t.setLabel(v1, taxonToTaxa.get(t.getLabel(v)).get(0));
        t.newEdge(v, v1);
        if (taxonToTaxa.containsKey(t.getLabel(v1)))
            extractNode(t, v1);
        Node v2 = t.newNode();
        t.setLabel(v2, taxonToTaxa.get(t.getLabel(v)).get(1));
        t.newEdge(v, v2);
        if (taxonToTaxa.containsKey(t.getLabel(v2)))
            extractNode(t, v2);
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

}
