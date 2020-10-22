/*
 *   EasyFastGetAgreementForest.java Copyright (C) 2020 Daniel H. Huson
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

import java.util.*;

public class EasyFastGetAgreementForest {

    // ******************************************************
    // Booleans for Debugging
    // ******************************************************
    // reports informations of each recursive call?
    private boolean debugMode = false;
    // size of the MAAFs-Search whose debug information should be printed
    private int debugForestSize = 14;
    // report computed MAAFs at the end?
    private final boolean printMAAFs = false;
    // use new recursive call?
    private final boolean addNewRecCall = true;
    // ******************************************************
    // if you have any questions: 01799783535
    // ******************************************************

    private boolean stop = false;
    private View.Computation compValue;
    private EasySiblingMemory easySibMem;

    private final HashSet<BitSet> forestSets = new HashSet<>();
    private final Hashtable<String, Vector<String>> taxonToTaxa = new Hashtable<>();
    private final Hashtable<Integer, HashSet<Vector<HybridTree>>> MAAFs = new Hashtable<>();

    private String rootLabel;
    private int k;
    private int recCalls = 0;

    private HybridTree tree1, tree2;
    private EasyTree eT1, eT2;

    private Vector<String> taxaOrdering;

    private ReplacementInfo rI;
    private final Vector<EasyTree> singletons = new Vector<>();

    public Hashtable<Integer, HashSet<Vector<HybridTree>>> run(HybridTree t1,
                                                               HybridTree t2, int forestSize, View.Computation compValue,
                                                               ReplacementInfo rI) {

        this.compValue = compValue;
        this.tree1 = t1;
        this.tree2 = t2;
        this.eT1 = new EasyTree(t1);
        this.eT2 = new EasyTree(t2);
        this.taxaOrdering = t1.getTaxaOrdering();
        this.k = forestSize;
        this.rI = rI;

        // max = getMax(eT2);
        rootLabel = t2.getLabel(t2.getRoot());
        easySibMem = new EasySiblingMemory(t1.getTaxaOrdering());

        if (!(new EasyIsomorphismCheck()).run(eT1, eT2)) {
            // while (MAAFs.size() == 0) {
            // System.out.println("searching forest of size " + k + "...");

            easySibMem = new EasySiblingMemory(t1.getTaxaOrdering());

            EasyTree forestTree = new EasyTree(t2);
            EasyTree h1 = new EasyTree(t1);
            EasyTree h2 = new EasyTree(t2);

            if (compValue == View.Computation.rSPR_DISTANCE) {
                removeDummyNodes(forestTree);
                removeDummyNodes(h1);
                removeDummyNodes(h2);
            }

            initNodeLabelings(forestTree, h1, h2);

            Vector<EasyTree> t2Forest = new Vector<>();
            t2Forest.add(forestTree);

            stop = false;
            computeMAAF(h1, t2Forest, false, 0);

            // System.out.println("#recCalls: " + recCalls);
            recCalls = 0;

            // if (MAAFs.size() == 0)
            // k += 1;
            // }
        } else {
            Vector<EasyTree> forest = new Vector<>();
            forest.add(new EasyTree(t2));
            addMAAF(forest, 0);
        }

        if (printMAAFs) {
            System.out.println();
            System.out.println("MAAFs");
            System.out.println("*******************");
            for (int j : MAAFs.keySet()) {
                System.out.println(j + " ----------------");
                for (Vector<HybridTree> vec : MAAFs.get(j)) {
                    System.out.println(" ~~~~~~~~~~~~~~ ");
                    for (HybridTree t : vec)
                        System.out.println(t + ";");
                    System.out.println(" ~~~~~~~~~~~~~~ ");
                }
                System.out
                        .println(MAAFs.get(j).size() + " MAAFs computed ----");
            }
            System.out.println("*******************");
        }

        // if (compValue == 1)
        // System.out.println("Hybrid Number: " + (this.k - 1));
        // else if (compValue == 2)
        // System.out.println("rSPR Distance: " + (this.k - 1));

        return MAAFs;

    }

    @SuppressWarnings("unchecked")
    private void addMAAF(Vector<EasyTree> forest, double weight) {

        // moveRootTreeToBack(forest);
        // boolean addForest = true;
        // if (compValue == 2) {
        // addForest = isMinimumForest(forest);
        // }

        BitSet b = easySibMem.getForestSet(forest);
        if (!forestSets.contains(b)) {

            forestSets.add(b);
            if (compValue == View.Computation.rSPR_DISTANCE) {
                addDummyNodes(forest);
                moveRootTreeToBack(forest);
            }

            Vector<HybridTree> convForest = convertForest(copyForest(forest));

            if (compValue != View.Computation.rSPR_DISTANCE) {

                for (HybridTree t : convForest)
                    t.update();

                HybridTree t = convForest.lastElement();
                t.setAltLabel(tree2.getAltLabel());
                t.taxaPairToWeight = tree2.getTaxaPairToWeight();
                t.edgeToWeight = tree2.edgeToWeight;
                t.setReplacementCharacter(tree2.getReplacementCharacter());

            } else
                MAAFs.clear();

            int index;
            if (compValue != View.Computation.rSPR_DISTANCE)
                index = forest.size() + (int) weight;
            else
                index = forest.size() - singletons.size();

            if (MAAFs.containsKey(index)) {
                HashSet<Vector<HybridTree>> set = (HashSet<Vector<HybridTree>>) MAAFs
                        .get(index).clone();
                MAAFs.remove(index);
                set.add(convForest);
                MAAFs.put(index, set);
            } else {
                HashSet<Vector<HybridTree>> set = new HashSet<>();
                set.add(convForest);
                MAAFs.put(index, set);
            }

            if (compValue == View.Computation.rSPR_DISTANCE) {
                if (forest.lastElement().getLeaves().size() == 1)
                    stop = true;
            }

            if (compValue == View.Computation.HYBRID_NUMBER && index == k)
                stop = true;

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
            }
        }

        EasyTree rootTree = forest.get(index);
        forest.remove(rootTree);
        forest.add(forest.size(), rootTree);

    }

    private void initNodeLabelings(EasyTree forestTree, EasyTree h1, EasyTree h2) {
        int i = 0;
        Vector<String> t1Taxa = getTaxa(forestTree);
        for (EasyNode v : forestTree.getLeaves()) {
            if (v.getOutDegree() != 0 && v.getInDegree() != 0) {
                while (t1Taxa.contains(String.valueOf(i)))
                    i++;
                forestTree.setLabel(v, String.valueOf(i));
                i++;
            }
        }
        Vector<String> t2Taxa = getTaxa(forestTree);
        for (EasyNode v : h1.getNodes()) {
            if (v.getOutDegree() != 0 && v.getInDegree() != 0) {
                while (t2Taxa.contains(String.valueOf(i)))
                    i++;
                h1.setLabel(v, String.valueOf(i));
                i++;
            }
        }
        Vector<String> t3Taxa = getTaxa(forestTree);
        for (EasyNode v : h2.getNodes()) {
            if (v.getOutDegree() != 0 && v.getInDegree() != 0) {
                while (t3Taxa.contains(String.valueOf(i)))
                    i++;
                h2.setLabel(v, String.valueOf(i));
                i++;
            }
        }
    }

    private Vector<String> getTaxa(EasyTree forestTree) {
        Vector<String> taxa = new Vector<>();
        for (EasyNode v : forestTree.getLeaves())
            taxa.add(v.getLabel());
        return taxa;
    }

    private void computeMAAF(EasyTree h, Vector<EasyTree> forest,
                             boolean newComponents, double weight) {

        // System.out.println(h.getPhyloTree() + ";");

        Vector<String> sing = new Vector<>();
        for (EasyTree t : forest)
            sing.add(t.getPhyloTree().toString());
        boolean debug = false;

        if (!easySibMem.contains(h, forest) && h.getLeaves().size() > 2
                && !stop) {

            if (forest.size() <= k) {

                EasySiblings sibs = new EasySiblings();
                sibs.init(h, forest);

                // System.out.println("1. removeNodes");
                removeSingletons(h, forest, sibs);
                // System.out.println(newT1 + ";");

                if (!easySibMem.contains(h, forest)
                        && h.getLeaves().size() != 2) {

                    double kPrime = 0;
                    // Brunch&Bound Approach - only activated for the
                    // computation of the rSPR distance
                    // if (newComponents)
                    if (compValue == View.Computation.rSPR_DISTANCE
                            && newComponents)
                        kPrime = new EasyForestApproximation(taxonToTaxa,
                                taxaOrdering).run(eT1, eT2, h, forest,
                                compValue);
                    if ((int) (kPrime / 3.0) <= k) {

                        recCalls++;
                        easySibMem.addEntry(h, forest);

                        sibs.updateSiblings(h, forest, true);
                        Vector<String> t1Sib = sibs.popSibling();

                        if (debug)
                            System.out.println("T1: " + h.getPhyloTree() + ";");

                        EasyNode v1 = sibs.getForestLeaf(t1Sib.get(0));
                        String s = getNeighbour(v1).getLabel();

                        if (easySibMem.compareTaxa(s, t1Sib.get(1))) {

                            EasyTree hCopy = copyTree(h);
                            Vector<EasyTree> forestCopy = copyForest(forest);
                            EasySiblings sibsCopy = new EasySiblings();
                            sibsCopy.init(hCopy, forestCopy);

                            contractSib(hCopy, forestCopy, t1Sib, sibsCopy);

                            if ((compValue == View.Computation.NETWORK || compValue == View.Computation.HYBRID_NUMBER)
                                    && addNewRecCall) {

                                Vector<EasyTree> exForest = copyForest(forestCopy);
                                EasyTree comp = null;
                                for (EasyTree t : exForest) {
                                    if (t.getRoot().getLabel().contains(t1Sib.get(0))) {
                                        comp = t;
                                        extractTree(comp);
                                    }
                                    extractTree(t);
                                }


                                if (new FastAcyclicCheck().run(exForest, eT1,
                                        eT2, taxaOrdering, comp, true) == null) {
                                    if (debug)
                                        System.out.println("1. cutting "
                                                + t1Sib);
                                    cutEdges(h, sibs, t1Sib, forest, weight,
                                            debug);
                                }
                            }

                            computeMAAF(hCopy, forestCopy, false, weight);

                        } else if (forest.size() < k) {
                            if (debug)
                                System.out.println("3. cutting " + t1Sib);
                            cutEdges(h, sibs, t1Sib, forest, weight, debug);
                        }
                    }

                } else if (h.getLeaves().size() == 2 && forest.size() <= k
                        && !stop) {

                    for (EasyTree t : forest)
                        extractTree(t);

                    if (compValue != View.Computation.rSPR_DISTANCE) {
                        Vector<EasyTree> acyclicOrder = new FastAcyclicCheck()
                                .run(forest, eT1, eT2, taxaOrdering, null, debug);
                        if (forest.size() == k && !stop && acyclicOrder != null)
                            addMAAF(acyclicOrder, weight);
                    } else if (forest.size() == k && !stop)
                        addMAAF(forest, weight);
                }

            }
        } else if (h.getLeaves().size() == 2 && forest.size() <= k && !stop) {
            // System.err.println("-> report forest " + forest);
            // reattachTrees(forest);
            for (EasyTree t : forest)
                extractTree(t);
            // Vector<HybridTree> convForest =
            // convertForest(copyForest(forest));
            // if (compValue != 2)
            // (new CuttingCycles()).run(convForest, tree1, tree2);
            // if (convForest.size() == k && !stop)
            // addMAAF(convForest, weight);
            if (compValue != View.Computation.rSPR_DISTANCE) {
                // (new EasyCuttingCycles()).run(forest, eT1, eT2,
                // taxaOrdering);
                Vector<EasyTree> acyclicOrder = new FastAcyclicCheck().run(
                        forest, eT1, eT2, taxaOrdering, null, debug);
                if (forest.size() == k && !stop && acyclicOrder != null)
                    addMAAF(acyclicOrder, weight);
            } else if (forest.size() == k && !stop)
                addMAAF(forest, weight);
        }

    }

    private void removeSingletons(EasyTree h1, Vector<EasyTree> t2Forest,
                                  EasySiblings sibs) {

        HashSet<String> forestSingeltons = new HashSet<>();
        for (EasyTree t : t2Forest) {
            if (!Objects.equals(t.getRoot().getLabel(), "rho"))
                forestSingeltons.add(t.getRoot().getLabel());
        }

        Vector<String> deletedTaxa = new Vector<>();
        boolean isActive = true;
        while (isActive) {
            isActive = false;
            for (String s : forestSingeltons) {
                EasyNode v1 = sibs.getT1Leaf(s);
                if (v1 != null && !deletedTaxa.contains(s)) {
                    removeNode(h1, v1);
                    deletedTaxa.add(s);
                }
            }
        }

    }

    private void removeNode(EasyTree t, EasyNode v) {
        if (t.getLeaves().size() > 2) {
            EasyNode p = v.getParent();
            t.deleteNode(v);
            if (p != null)
                p.restrict();
        }
    }

    private void cutEdges(EasyTree h12, EasySiblings sibs,
                          Vector<String> t1Sib, Vector<EasyTree> t2Forest, double weight,
                          boolean debug) {

        String taxa1 = t1Sib.get(0);
        String taxa2 = t1Sib.get(1);

        EasyTree owner1 = sibs.getForestLeaf(taxa1).getOwner();
        EasyTree owner2 = sibs.getForestLeaf(taxa2).getOwner();

        int h1 = getHeight(sibs.getForestLeaf(taxa1));
        int h2 = getHeight(sibs.getForestLeaf(taxa2));

        Vector<EasyTree> forestCopy;
        EasyNode cuttingNode;
        if (!owner1.equals(owner2)) {

            forestCopy = copyForest(t2Forest);
            cuttingNode = getCuttingNode(forestCopy, sibs, taxa1);
            cutForest(cuttingNode, forestCopy, copyTree(h12), sibs, t1Sib,
                    weight, debug);

            cuttingNode = getCuttingNode(t2Forest, sibs, taxa2);
            cutForest(cuttingNode, t2Forest, h12, sibs, t1Sib, weight, debug);
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

            if (onlyPendant != null && compValue != View.Computation.NETWORK) {
                if (compValue == View.Computation.HYBRID_NUMBER) {
                    forestCopy = copyForest(t2Forest);
                    cuttingNode = getCuttingPendant(forestCopy, sibs,
                            onlyPendant);
                    cutForest(cuttingNode, forestCopy, copyTree(h12), sibs,
                            t1Sib, weight, debug);

                    cuttingNode = getCuttingNode(t2Forest, sibs, taxa2);
                    cutForest(cuttingNode, t2Forest, h12, sibs, t1Sib, weight,
                            debug);
                } else {
                    // cutForest(onlyPendant, copyForest(forest), copyTree(t1),
                    // sibs, t1Sib);

                    if (rI.getPrunedLabels().contains(taxa1)) {
                        forestCopy = copyForest(t2Forest);
                        cuttingNode = getCuttingNode(forestCopy, sibs, taxa1);
                        cutForest(cuttingNode, forestCopy, copyTree(h12), sibs,
                                t1Sib, weight, debug);
                    }

                    if (rI.getPrunedLabels().contains(taxa2)) {
                        forestCopy = copyForest(t2Forest);
                        cuttingNode = getCuttingNode(forestCopy, sibs, taxa2);
                        cutForest(cuttingNode, forestCopy, copyTree(h12), sibs,
                                t1Sib, weight, debug);
                    }

                    cuttingNode = getCuttingPendant(t2Forest, sibs, onlyPendant);
                    cutForest(cuttingNode, t2Forest, h12, sibs, t1Sib, weight,
                            debug);

                }
            } else {
                forestCopy = copyForest(t2Forest);
                Vector<EasyNode> pendants = getPendantNodes(taxa1, taxa2, sibs,
                        forestCopy);
                if (pendants.size() != 0)
                    cutForest(pendants, forestCopy, copyTree(h12), sibs, t1Sib,
                            weight, debug);

                forestCopy = copyForest(t2Forest);
                cuttingNode = getCuttingNode(forestCopy, sibs, taxa1);
                cutForest(cuttingNode, forestCopy, copyTree(h12), sibs, t1Sib,
                        weight, debug);

                cuttingNode = getCuttingNode(t2Forest, sibs, taxa2);
                cutForest(cuttingNode, t2Forest, h12, sibs, t1Sib, weight,
                        debug);
            }

        }

    }

    private EasyNode getCuttingNode(Vector<EasyTree> forestCopy,
                                    EasySiblings sibs, String taxa) {
        sibs.updateTaxa(forestCopy);
        return sibs.getForestLeaf(taxa);
    }

    private EasyNode getCuttingPendant(Vector<EasyTree> forestCopy,
                                       EasySiblings sibs, String taxa) {
        sibs.updateTaxa(forestCopy);
        return getNeighbour(sibs.getForestLeaf(taxa));
    }

    private Vector<EasyTree> copyForest(Vector<EasyTree> t2Forest) {
        Vector<EasyTree> v = new Vector<>();
        for (EasyTree t : t2Forest)
            v.add(copyTree(t));
        return v;
    }

    private EasyTree copyTree(EasyTree t) {
        return new EasyTree(t.getPhyloTree());
    }

    private void cutForest(EasyNode v, Vector<EasyTree> forestCopy,
                           EasyTree t1, EasySiblings sibs, Vector<String> t1Sib,
                           double weight, boolean debug) {

        sibs.updateTaxa(t1);
        EasyTree t = v.getOwner();
        double newWeight = weight;

        if (t.getLeaves().size() > 1) {
            EasyNode p = v.getParent();

            if (compValue != View.Computation.rSPR_DISTANCE
                    && !p.getLabel().equals(rootLabel)) {
                EasyTree subtree = t.pruneSubtree(v);

                if (v.getInDegree() == 1)
                    newWeight += tree1.getEdgeWeight(getTaxa(v), getTaxa(p));

                forestCopy.add(subtree);
                if (debug) {
                    System.out.println("Forest");
                    for (EasyTree c : forestCopy)
                        System.out.println(c.getPhyloTree());
                    System.out.println();
                }
                computeMAAF(t1, forestCopy, true, newWeight);

            } else if (compValue == View.Computation.rSPR_DISTANCE) {
                EasyTree subtree = t.pruneSubtree(v);

                forestCopy.add(subtree);
                computeMAAF(t1, forestCopy, true, newWeight);
            }

        }

    }

    private void cutForest(Vector<EasyNode> nodes, Vector<EasyTree> forest,
                           EasyTree t1, EasySiblings sibs, Vector<String> t1Sib,
                           double weight, boolean debug) {

        // if (compValue == View.Computation.NETWORK) {
        // for (EasyNode v2 : nodes) {
        // if (v2.getOutDegree() == 0) {
        // EasyNode v1 = sibs.getT1Leaf(v2.getLabel());
        // if (v1 != null && v1.getInDegree() != 0) {
        // EasyNode w1 = getNeighbour(v1);
        // if (w1 != null && w1.getOutDegree() == 0) {
        // EasyTree t1Copy = copyTree(t1);
        // Vector<EasyTree> forestCopy = copyForest(forest);
        // EasySiblings sibsCopy = new EasySiblings();
        // sibsCopy.init(t1Copy, forestCopy);
        // EasyNode cuttingNode = getCuttingNode(forestCopy,
        // sibsCopy, w1.getLabel());
        // if (cuttingNode != null)
        // cutForest(cuttingNode, forestCopy, t1Copy,
        // sibsCopy, new Vector<String>(), weight,
        // debug);
        // }
        // }
        // }
        // }
        // }

        double newWeight = weight;
        for (EasyNode v : nodes) {
            EasyTree t = v.getOwner();

            if (t.getLeaves().size() > 1) {
                EasyNode p = v.getParent();
                if (compValue != View.Computation.rSPR_DISTANCE
                        && !p.getLabel().equals(rootLabel)) {

                    if (v.getInDegree() == 1)
                        newWeight += tree1
                                .getEdgeWeight(getTaxa(v), getTaxa(p));

                    EasyTree subtree = t.pruneSubtree(v);
                    forest.add(subtree);

                } else if (compValue == View.Computation.rSPR_DISTANCE) {
                    EasyTree subtree = t.pruneSubtree(v);
                    forest.add(subtree);
                }
            }
        }

        sibs.updateTaxa(t1);
        contractSib(t1, forest, t1Sib, sibs);
        if (debug) {
            System.out.println("Forest");
            for (EasyTree c : forest)
                System.out.println(c.getPhyloTree());
            System.out.println();
        }
        computeMAAF(t1, forest, true, newWeight);

    }

    private EasyNode getNeighbour(EasyNode v) {

        EasyNode p = v.getParent();
        Iterator<EasyNode> it = p.getChildren().iterator();

        EasyNode c = it.next();
        if (!c.equals(v))
            return c;

        return it.next();
    }

    private String getOnlyPendant(EasyNode v1, EasyNode v2, EasyTree owner1) {

        EasyNode p1 = v1.getParent();
        EasyNode p2 = v2.getParent();

        if (p1.getInDegree() == 1) {
            EasyNode v = p1.getParent();
            if (v.equals(p2))
                return v1.getLabel();
        }

        return null;
    }

    private Vector<EasyNode> getPendantNodes(String taxa1, String taxa2,
                                             EasySiblings sibs, Vector<EasyTree> forestCopy) {

        sibs.updateTaxa(forestCopy);
        Vector<EasyNode> pendantNodes = new Vector<>();
        EasyNode v1 = sibs.getForestLeaf(taxa1);
        EasyNode v2 = sibs.getForestLeaf(taxa2);

        Vector<EasyNode> upperNodesV1 = getUpperNodes(v1);
        Vector<EasyNode> upperNodesV2 = getUpperNodes(v2);

        EasyNode pendant = v1;
        EasyNode p = pendant.getParent();
        while (!upperNodesV2.contains(p)) {
            pendantNodes.add(getNeighbour(pendant));
            pendant = p;
            p = pendant.getParent();
        }

        pendant = v2;
        p = pendant.getParent();
        while (!upperNodesV1.contains(p)) {
            pendantNodes.add(getNeighbour(pendant));
            pendant = p;
            p = pendant.getParent();
        }

        return pendantNodes;
    }

    private Vector<EasyNode> getUpperNodes(EasyNode v1) {
        Vector<EasyNode> upperNodes = new Vector<>();
        while (v1.getInDegree() == 1) {
            v1 = v1.getParent();
            upperNodes.add(v1);
        }
        return upperNodes;
    }

    private void contractSib(EasyTree t, Vector<EasyTree> forest,
                             Vector<String> t1Sib, EasySiblings sibs) {

        contractLeaves(t, t1Sib, sibs);
        sibs.updateTaxa(t, forest);

    }

    private void contractLeaves(EasyTree h1, Vector<String> t1Sib,
                                EasySiblings sibs) {

        // contracting t1
        EasyNode v1 = sibs.getT1Leaf(t1Sib.get(0));
        EasyNode v2 = sibs.getT1Leaf(t1Sib.get(1));
        EasyNode p = v1.getParent();

        Vector<String> taxa = new Vector<>();
        taxa.add(v1.getLabel());
        taxa.add(v2.getLabel());
        Collections.sort(taxa);

        h1.deleteNode(v1);
        h1.deleteNode(v2);

        String s = "";
        s = s.concat(taxa.get(0));
        s = s.concat("+");
        s = s.concat(taxa.get(1));

        h1.setLabel(p, s);
        easySibMem.addTreeLabel(s);

        // setting forest label

        EasyNode f1 = sibs.getForestLeaf(t1Sib.get(0));
        EasyNode f2 = sibs.getForestLeaf(t1Sib.get(1));
        EasyNode fP = f1.getParent();
        EasyTree f = f1.getOwner();

        f.deleteNode(f1);
        f.deleteNode(f2);

        f.setLabel(fP, s);

        taxonToTaxa.put(s, taxa);
        easySibMem.addTaxon(s, taxa);

    }

    private int getHeight(EasyNode v) {
        int h = 0;
        if (v.getInDegree() != 0) {
            EasyNode p = v.getParent();
            h = 1;
            while (p.getInDegree() != 0) {
                p = p.getParent();
                h++;
            }
            return h;
        }
        return h;
    }

    @SuppressWarnings("unchecked")
    private void extractTree(EasyTree t) {
        Vector<EasyNode> leaveSet = (Vector<EasyNode>) t.getLeaves().clone();
        for (EasyNode v : leaveSet) {
            if (taxonToTaxa.containsKey(v.getLabel()))
                extractNode(t, v);
        }
    }

    private void extractNode(EasyTree t, EasyNode v) {
        EasyNode v1 = new EasyNode(v, t, taxonToTaxa.get(v.getLabel()).get(0));
        EasyNode v2 = new EasyNode(v, t, taxonToTaxa.get(v.getLabel()).get(1));
        if (taxonToTaxa.containsKey(v1.getLabel()))
            extractNode(t, v1);
        if (taxonToTaxa.containsKey(v2.getLabel()))
            extractNode(t, v2);
    }

    private Vector<HybridTree> convertForest(Vector<EasyTree> forest) {
        Vector<HybridTree> convForest = new Vector<>();
        for (EasyTree t : forest) {
            HybridTree h = convertTree(t);
            convForest.add(h);
        }
        return convForest;
    }

    private HybridTree convertTree(EasyTree t) {
        HybridTree h = new HybridTree(t.getPhyloTree(), false,
                tree1.getTaxaOrdering());
        h.update();
        return h;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public Vector<String> getTaxa(EasyNode v) {
        Vector<String> taxa = new Vector<>();
        for (EasyNode c : v.getChildren()) {
            if (c.getOutDegree() == 0) {
                taxa.add(c.getLabel());
            }
        }
        return taxa;
    }

    // private int getMax(EasyTree t) {
    // int max = 0;
    // for (EasyNode v : t.computeSetOfLeaves()) {
    // if (rI.getPrunedLabels().contains(v.getLabel()))
    // max++;
    // }
    // return max;
    // }
    //
    // private boolean isMinimumForest(Vector<EasyTree> f) {
    // int prunedLabels = 0;
    // for (EasyTree t : f) {
    // if (t.computeSetOfLeaves().size() == 1) {
    // String label = t.computeSetOfLeaves().firstElement().getLabel();
    // if (rI.getPrunedLabels().contains(label))
    // prunedLabels++;
    // }
    // }
    // if (prunedLabels > min) {
    // min = prunedLabels;
    // return true;
    // } else if (prunedLabels == min) {
    // if (f.lastElement().computeSetOfLeaves().size() == 1) {
    // if (min == max)
    // stop = true;
    // return true;
    // }
    // }
    //
    // return false;
    // }

    private void addDummyNodes(Vector<EasyTree> forest) {
        // System.out.println("Before");
        // for(EasyTree t : forest)
        // System.out.println(t.getPhyloTree());
        for (EasyTree t : singletons)
            forest.add(t);
        // System.out.println("After");
        // for(EasyTree t : forest)
        // System.out.println(t.getPhyloTree());
    }

    private void removeDummyNodes(EasyTree t) {
        Vector<String> labels = new Vector<>();
        for (EasyTree eT : singletons)
            labels.add(eT.getRoot().getLabel());

        Hashtable<String, EasyNode> labelToDummy = new Hashtable<>();
        for (EasyNode v : t.getLeaves()) {
            String label = v.getLabel();
            if (rI.getPrunedLabels().contains(label))
                labelToDummy.put(label, v);
        }
        for (String l : labelToDummy.keySet()) {
            EasyTree s = t.pruneSubtree(labelToDummy.get(l));
            if (!labels.contains(s.getRoot().getLabel()))
                singletons.add(s);
        }
    }
}
