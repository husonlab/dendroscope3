/*
 *   RefinedFastApproxHNumber.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.cmpAllMAAFs;

import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.model.treeObjects.SparseTree;
import dendroscope.hybroscale.model.treeObjects.SparseTreeNode;
import dendroscope.hybroscale.util.graph.MyNode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

public class RefinedFastApproxHNumber {

    private boolean stop = false;
    private int k;

    private Hashtable<String, Vector<String>> taxonToTaxa = new Hashtable<String, Vector<String>>();
    private HashSet<Vector<EasyTree>> MAAFs = new HashSet<Vector<EasyTree>>();

    private int recCalls = 0;

    private HybridTree t1Restricted, t2Restricted;

    private Vector<String> t1Taxa, t2Taxa;
    private Vector<SparseTree> t2Components, t1Components;

    public int run(HybridTree t1, HybridTree t2, Vector<String> taxaOrdering, int k) {

        // System.out.println(" *************************************  " +
        // forestSize);

        this.k = k;

        t1Taxa = new Vector<String>();
        for (MyNode leaf : t1.getLeaves())
            t1Taxa.add(leaf.getLabel());
        t2Taxa = new Vector<String>();
        for (MyNode leaf : t2.getLeaves())
            t2Taxa.add(leaf.getLabel());

        t1Components = new Vector<SparseTree>();
        t2Components = new Vector<SparseTree>();

        if (!(new EasyIsomorphismCheck()).run(new EasyTree(t1), new EasyTree(t2))) {

            // Restricting both trees to an equal set of taxa **************

            t2Restricted = new HybridTree(t2, false, taxaOrdering);
            Vector<MyNode> obsoleteNodes = new Vector<MyNode>();
            for (MyNode leaf : t2Restricted.getLeaves()) {
                if (!t1Taxa.contains(leaf.getLabel()))
                    obsoleteNodes.add(leaf);
            }
            for (MyNode v : obsoleteNodes) {
                t2Restricted.deleteNode(v);
                t2Components.add(new SparseTree(new SparseTreeNode(null, null, v.getLabel())));
            }

            t1Restricted = new HybridTree(t1, false, taxaOrdering);
            obsoleteNodes = new Vector<MyNode>();
            for (MyNode leaf : t1Restricted.getLeaves()) {
                if (!t2Taxa.contains(leaf.getLabel()))
                    obsoleteNodes.add(leaf);
            }
            for (MyNode v : obsoleteNodes) {
                t1Restricted.deleteNode(v);
                t1Components.add(new SparseTree(new SparseTreeNode(null, null, v.getLabel())));
            }

            // *************************************************************

            if (!(new EasyIsomorphismCheck()).run(new EasyTree(t1Restricted), new EasyTree(t2Restricted))) {

                EasyTree forestTree = new EasyTree(t2Restricted);
                EasyTree h1 = new EasyTree(t1Restricted);
                EasyTree h2 = new EasyTree(t2Restricted);

                initNodeLabelings(forestTree, h1, h2);

                Vector<EasyTree> t2Forest = new Vector<EasyTree>();
                t2Forest.add(forestTree);

                stop = false;
                computeMAAF(h1, t2Forest, false);

            } else {

                Vector<EasyTree> forest = new Vector<EasyTree>();
                forest.add(new EasyTree(t2Restricted));
                MAAFs.add(forest);
            }

        } else {
            Vector<EasyTree> forest = new Vector<EasyTree>();
            forest.add(new EasyTree(t2));
            MAAFs.add(forest);
        }

        if (!MAAFs.isEmpty())
            return MAAFs.iterator().next().size() - 1;

        return Integer.MAX_VALUE;

    }

    private void initNodeLabelings(EasyTree forestTree, EasyTree h1, EasyTree h2) {
        int i = 0;
        Vector<String> t1Taxa = getTaxa(forestTree);
        for (EasyNode v : forestTree.getNodes()) {
            if (v.getOutDegree() != 0) {
                while (t1Taxa.contains(String.valueOf(i)))
                    i++;
                forestTree.setLabel(v, String.valueOf(i));
                i++;
            }
        }
        Vector<String> t2Taxa = getTaxa(forestTree);
        for (EasyNode v : h1.getNodes()) {
            if (v.getOutDegree() != 0) {
                while (t2Taxa.contains(String.valueOf(i)))
                    i++;
                h1.setLabel(v, String.valueOf(i));
                i++;
            }
        }
        Vector<String> t3Taxa = getTaxa(forestTree);
        for (EasyNode v : h2.getNodes()) {
            if (v.getOutDegree() != 0) {
                while (t3Taxa.contains(String.valueOf(i)))
                    i++;
                h2.setLabel(v, String.valueOf(i));
                i++;
            }
        }
    }

    private Vector<String> getTaxa(EasyTree forestTree) {
        Vector<String> taxa = new Vector<String>();
        for (EasyNode v : forestTree.getLeaves())
            taxa.add(v.getLabel());
        return taxa;
    }

    private void computeMAAF(EasyTree h, Vector<EasyTree> forest, boolean newComponents) {
        recCalls++;

        boolean debug = false;

        if (forest.size() <= k) {

            if (h.getLeaves().size() > 2 && !stop) {

                EasySiblings sibs = new EasySiblings();
                sibs.init(h, forest, Integer.MAX_VALUE);

                boolean validCut = removeSingletons(h, forest, sibs, debug);

                if (h.getLeaves().size() != 2 && validCut) {

                    if (debug) {
                        System.out.println("\n------ New Recursive Call");
                        System.out.println("T1: " + h.getPhyloTree() + ";");
                        for (EasyTree t : forest)
                            System.out.println(t.getPhyloTree().toBracketString());
                    }

                    sibs.updateSiblings(h, forest, true);
                    Vector<Vector<Vector<String>>> t1Siblings = sibs.getAllSiblings();
                    Vector<String> t1Sib = getOptimalSibling(t1Siblings, sibs, debug);

                    sibs.init(h, forest, Integer.MAX_VALUE);

                    EasyNode v1 = sibs.getForestLeaf(t1Sib.get(0));

                    boolean contractSibling = false;
                    for (EasyNode c : v1.getParent().getChildren()) {
                        if (c.getLabel().equals(t1Sib.get(1)))
                            contractSibling = true;
                    }

                    if (contractSibling) {
                        if (debug)
                            System.out.println("Contracting: " + t1Sib);

                        EasyTree hCopy = copyTree(h);
                        Vector<EasyTree> forestCopy = copyForest(forest);
                        EasySiblings sibsCopy = new EasySiblings();
                        sibsCopy.init(hCopy, forestCopy, Integer.MAX_VALUE);
                        contractSib(hCopy, forestCopy, t1Sib, sibsCopy);
                        computeMAAF(hCopy, forestCopy, false);

                    } else {

                        if (debug)
                            System.out.println("Cutting: " + t1Sib);

                        cutEdges(copyTree(h), sibs, t1Sib, copyForest(forest), debug);
                    }

                } else if (h.getLeaves().size() == 2 && !stop) {

                    MAAFs.add(forest);
                    stop = true;
                }

                sibs.clear();

            } else if (h.getLeaves().size() == 2 && !stop) {

                MAAFs.add(forest);
                stop = true;

            }
        }

    }

    private Vector<String> getOptimalSibling(Vector<Vector<Vector<String>>> t1Siblings, EasySiblings sibs, boolean debug) {

        Vector<Vector<String>> sibsSameComp = new Vector<Vector<String>>();
        Vector<String> diffSib = null;
        for (Vector<Vector<String>> sibCluster : t1Siblings) {
            boolean allDifferent = true;
            Vector<EasyTree> owners = new Vector<EasyTree>();
            Vector<String> taxa = new Vector<String>();
            for (Vector<String> sib : sibCluster) {

                if (debug)
                    System.out.println("Sibs: " + sib.get(0) + " " + sib.get(1));

                EasyNode v1 = sibs.getForestLeaf(sib.get(0));
                EasyNode v2 = sibs.getForestLeaf(sib.get(1));
                if (v1.getOwner().equals(v2.getOwner()))
                    sibsSameComp.add(sib);
                for (EasyNode c : v1.getParent().getChildren()) {
                    if (c.getLabel().equals(v2.getLabel()))
                        return sib;
                }

                if (!owners.contains(v1.getOwner())) {
                    taxa.add(sib.get(0));
                    owners.add(v1.getOwner());
                } else if (!taxa.contains(sib.get(0)))
                    allDifferent = false;
                if (!owners.contains(v2.getOwner())) {
                    taxa.add(sib.get(1));
                    owners.add(v2.getOwner());
                } else if (!taxa.contains(sib.get(0)))
                    allDifferent = false;

            }
            if (allDifferent)
                diffSib = sibCluster.firstElement();
        }

        if (diffSib != null)
            return diffSib;

        int maxHeight = -1;
        Vector<Vector<String>> optSibs = new Vector<Vector<String>>();
        for (Vector<String> sib : sibsSameComp) {

            EasyNode v1 = sibs.getForestLeaf(sib.get(0));
            EasyNode v2 = sibs.getForestLeaf(sib.get(1));
            EasyNode lca = cmpLCA(v1, v2);
            int height = getHeight(lca);

            if (height == maxHeight)
                optSibs.add(sib);
            else if (height > maxHeight) {
                maxHeight = height;
                optSibs.clear();
                optSibs.add(sib);
            }

        }

        for (Vector<String> sib : optSibs) {

            EasyNode v1 = sibs.getForestLeaf(sib.get(0));
            EasyNode v2 = sibs.getForestLeaf(sib.get(1));
            EasyNode lca = cmpLCA(v1, v2);

            boolean b = true;
            for (EasyNode c : lca.getChildren()) {
                if (c.getLabel().equals(sib.get(0)) || c.getLabel().equals(sib.get(1)))
                    b = false;
            }
            if (b)
                return sib;

        }

        return optSibs.firstElement();

    }

    private EasyNode cmpLCA(EasyNode v1, EasyNode v2) {
        EasyNode p = v1.getParent();
        Vector<EasyNode> v1Preds = new Vector<EasyNode>();
        while (p != null) {
            v1Preds.add(p);
            p = p.getParent();
        }
        p = v2.getParent();
        while (p != null) {
            if (v1Preds.contains(p))
                return p;
            p = p.getParent();
        }
        return null;
    }

    private boolean removeSingletons(EasyTree h1, Vector<EasyTree> t2Forest, EasySiblings sibs, boolean debug) {

        HashSet<String> forestSingeltons = new HashSet<String>();
        for (EasyTree t : t2Forest) {
            if (t.getLeaves().size() == 1 && t.getRoot().getLabel() != "rho")
                forestSingeltons.add(t.getRoot().getLabel());
        }

        Vector<String> deletedTaxa = new Vector<String>();
        boolean isActive = true;
        while (isActive) {
            isActive = false;
            for (String s : forestSingeltons) {
                EasyNode v1 = sibs.getT1Leaf(s);
                if (v1 != null && !deletedTaxa.contains(s)) {
                    if (v1.isSolid())
                        return false;
                    removeNode(h1, v1, sibs, debug);
                    deletedTaxa.add(s);
                }
            }
        }

        return true;

    }

    private void removeNode(EasyTree t, EasyNode v, EasySiblings sibs, boolean debug) {
        if (t.getLeaves().size() > 2) {
            EasyNode p = v.getParent();
            if (debug)
                System.out.println("Removing: " + v.getLabel() + " ");
            t.deleteNode(v);
            if (p != null && p.getOutDegree() == 1)
                p.restrict();
        }
    }

    private void cutEdges(EasyTree h12, EasySiblings sibs, Vector<String> t1Sib, Vector<EasyTree> t2Forest,
                          boolean debug) {

        EasyNode v1 = sibs.getForestLeaf(t1Sib.get(0));
        EasyNode v2 = sibs.getForestLeaf(t1Sib.get(1));
        int h1 = this.getHeight(v1);
        int h2 = this.getHeight(v2);

        String taxa1 = h1 < h2 ? t1Sib.get(0) : t1Sib.get(1);
        String taxa2 = h1 < h2 ? t1Sib.get(1) : t1Sib.get(0);

        EasyTree owner1 = (EasyTree) sibs.getForestLeaf(taxa1).getOwner();
        EasyTree owner2 = (EasyTree) sibs.getForestLeaf(taxa2).getOwner();

        EasyNode cuttingNode;
        if (!owner1.equals(owner2)) {

            cuttingNode = getCuttingNode(t2Forest, sibs, taxa1);
            cuttingNode.setInfo(2);
            cutForest(cuttingNode, t2Forest, h12, sibs, t1Sib, debug, false);

            cuttingNode = getCuttingNode(t2Forest, sibs, taxa2);
            cuttingNode.setInfo(2);
            cutForest(cuttingNode, t2Forest, h12, sibs, t1Sib, debug, false);

            computeMAAF(h12, t2Forest, true);

        } else {

            Vector<Vector<EasyNode>> pendantSet = getPendantNodes(taxa1, sibs, t2Forest);
            if (pendantSet.size() != 0)
                cutPendants(pendantSet, t2Forest, h12, sibs, t1Sib, debug);

            pendantSet = getPendantNodes(taxa2, sibs, t2Forest);
            if (pendantSet.size() != 0)
                cutPendants(pendantSet, t2Forest, h12, sibs, t1Sib, debug);

            computeMAAF(h12, t2Forest, true);

        }

    }

    private EasyNode getCuttingNode(Vector<EasyTree> forestCopy, EasySiblings sibs, String taxa) {
        sibs.updateTaxa(forestCopy);
        return sibs.getForestLeaf(taxa);
    }

    private Vector<EasyTree> copyForest(Vector<EasyTree> forest) {
        Vector<EasyTree> forestCopy = new Vector<EasyTree>();
        for (EasyTree t : forest) {
            EasyTree tCopy = new EasyTree(t);
            tCopy.getRoot().setInfo(t.getRoot().getInfo());
            forestCopy.add(tCopy);
        }
        return forestCopy;
    }

    private EasyTree copyTree(EasyTree t) {
        return new EasyTree(t);
    }

    private void cutForest(EasyNode v, Vector<EasyTree> forest, EasyTree t1, EasySiblings sibs, Vector<String> t1Sib,
                           boolean debug, boolean sameOwner) {

        // sibs.updateTaxa(t1);
        EasyTree t = (EasyTree) v.getOwner();

        if (t.getLeaves().size() > 1) {
            Vector<EasyNode> neighbours = getNeighbours(v);
            if (neighbours.size() != 1 || !neighbours.get(0).getLabel().equals("rho")) {

                if (neighbours.size() == 1) {
                    neighbours.get(0).setAddedNode(false);
                    // if (check(neighbours.get(0).getLabel()))
                    // System.out.println("2)" + neighbours.get(0).getLabel() +
                    // " " +
                    // neighbours.get(0).getOwner().getPhyloTree() + ";");
                }

                String label = v.getLabel();
                EasyTree subtree = t.pruneSubtree(v);
                subtree.getRoot().setLabel(label);

                forest.add(0, subtree);

            }

        }

    }

    private void cutPendants(Vector<Vector<EasyNode>> pendantSet, Vector<EasyTree> forest, EasyTree t1,
                             EasySiblings sibs, Vector<String> t1Sib, boolean debug) {

        for (Vector<EasyNode> pendants : pendantSet) {
            Vector<EasyTree> subtrees = new Vector<EasyTree>();

            boolean cutPendants = true;
            if (pendants.size() > 1) {
                EasyNode p = pendants.firstElement().getParent();
                Vector<EasyNode> neighbours = getNeighbours(p);
                if (neighbours.size() == 1 && neighbours.get(0).getLabel().equals("rho"))
                    cutPendants = false;
            }

            if (cutPendants) {
                for (EasyNode v : pendants) {

                    EasyTree t = (EasyTree) v.getOwner();

                    if (t.getLeaves().size() > 1) {
                        Vector<EasyNode> neighbours = getNeighbours(v);
                        if (neighbours.size() != 1 || !neighbours.get(0).getLabel().equals("rho")) {

                            String label = v.getLabel();
                            EasyTree subtree = t.pruneSubtree(v);
                            subtree.getRoot().setLabel(label);
                            subtrees.add(subtree);

                        }
                    }
                }
                if (subtrees.size() == 1) {
                    forest.add(0, subtrees.firstElement());
                } else {
                    EasyNode root = new EasyNode(null, null, cmpUniqueLabel(forest));
                    EasyTree t = new EasyTree(root);
                    for (EasyTree subtree : subtrees)
                        t.getRoot().addSubtree(subtree);

                    forest.add(0, t);
                }
            }
        }
        sibs.updateTaxa(t1);

    }

    private String cmpUniqueLabel(Vector<EasyTree> forest) {
        HashSet<String> labelings = new HashSet<String>();
        for (EasyTree c : forest) {
            for (EasyNode v : c.getNodes())
                labelings.add(v.getLabel());
        }
        int i = 0;
        while (labelings.contains(String.valueOf(i)))
            i++;
        return String.valueOf(i);
    }

    private Vector<EasyNode> getNeighbours(EasyNode v) {
        Vector<EasyNode> neighbours = new Vector<EasyNode>();
        for (EasyNode c : v.getParent().getChildren()) {
            if (!c.equals(v))
                neighbours.add(c);
        }
        return neighbours;
    }

    private Vector<Vector<EasyNode>> getPendantNodes(String taxa, EasySiblings sibs, Vector<EasyTree> forestCopy) {

        sibs.updateTaxa(forestCopy);
        Vector<Vector<EasyNode>> pendantSet = new Vector<Vector<EasyNode>>();
        EasyNode v = sibs.getForestLeaf(taxa);

        if (v.getInDegree() != 0) {

            Vector<EasyNode> v1Set = new Vector<EasyNode>();
            v1Set.add(v);
            pendantSet.add(v1Set);

            if (v.getParent().getInDegree() != 0)
                pendantSet.add(getNeighbours(v));
        }

        return pendantSet;
    }

    private void contractSib(EasyTree t, Vector<EasyTree> forest, Vector<String> t1Sib, EasySiblings sibs) {
        contractLeaves(t, t1Sib, sibs);
        sibs.updateTaxa(t, forest);
    }

    private void contractLeaves(EasyTree h1, Vector<String> t1Sib, EasySiblings sibs) {

        EasyNode v1 = sibs.getT1Leaf(t1Sib.get(0));
        EasyNode v2 = sibs.getT1Leaf(t1Sib.get(1));
        EasyNode p = v1.getParent();

        // contracting h1
        Vector<String> taxa = new Vector<String>();
        taxa.add(v1.getLabel());
        taxa.add(v2.getLabel());
        Collections.sort(taxa);
        String s = "";
        s = s.concat(taxa.get(0));
        s = s.concat("+");
        s = s.concat(taxa.get(1));

        boolean isMultiNode = false;
        if (p.getOutDegree() == 2) {
            h1.deleteNode(v1);
            h1.deleteNode(v2);
            h1.setLabel(p, s);
        } else {
            isMultiNode = true;
            h1.deleteNode(v2);
            h1.setLabel(v1, s);
        }

        // contracting forest
        EasyNode f1 = sibs.getForestLeaf(t1Sib.get(0));
        EasyNode f2 = sibs.getForestLeaf(t1Sib.get(1));
        EasyNode fP = f1.getParent();
        EasyTree f = (EasyTree) f1.getOwner();

        EasyNode[] contractedNodes = {f1, f2};

        // System.out.println(t1Sib.get(0)+" "+t1Sib.get(1));
        // System.out.println(h1.getPhyloTree()+";");
        // System.out.println(f.getPhyloTree()+";\n"+fP.getLabel()+" "+fP.getOutDegree()+"\n");

        if (fP.getOutDegree() == 2) {
            f.deleteNode(f1);
            f.deleteNode(f2);
            f.setLabel(fP, s);
            fP.setContractedNodes(contractedNodes);
            // if(check(fP.getLabel()) && !isMultiNode)
            // System.out.println("95) "+fP.getLabel()+"\n"+f.getPhyloTree()+";");
        } else {
            f.deleteNode(f1);
            f.deleteNode(f2);
            EasyNode fC = new EasyNode(fP, f, s);
            fC.setContractedNodes(contractedNodes);
            fC.setAddedNode(isMultiNode);
            // if(check(fC.getLabel()) && !isMultiNode)
            // System.out.println("96) "+fC.getLabel()+"\n"+f.getPhyloTree()+";");
        }

        taxonToTaxa.put(s, taxa);

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

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public Vector<String> getTaxa(EasyNode v) {
        Vector<String> taxa = new Vector<String>();
        for (EasyNode c : v.getChildren()) {
            if (c.getOutDegree() == 0) {
                taxa.add(c.getLabel());
            }
        }
        return taxa;
    }

    public int getRecCalls() {
        return recCalls;
    }
}
