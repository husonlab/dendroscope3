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

import java.util.*;

public class EasyForestApproximation {

    private final Vector<String> taxaOrdering;
    private double kPrime;
    private final Hashtable<String, Vector<String>> taxonToTaxa;

    @SuppressWarnings("unchecked")
    public EasyForestApproximation(
            Hashtable<String, Vector<String>> taxonToTaxa,
            Vector<String> taxaOrdering) {
        this.taxaOrdering = taxaOrdering;
        this.taxonToTaxa = (Hashtable<String, Vector<String>>) taxonToTaxa
                .clone();
    }

    public double run(EasyTree tree1, EasyTree tree2, EasyTree t1,
                      Vector<EasyTree> forest, View.Computation compValue) {

        kPrime = forest.size() - 1;

        EasyTree t1Copy = new EasyTree(t1);
        Vector<EasyTree> forestCopy = copyForest(forest);

        if (forestCopy.size() != 1
                || !(new EasyIsomorphismCheck()).run(t1Copy,
                forestCopy.firstElement())) {

            EasySiblings sibs = new EasySiblings();
            sibs.init(t1Copy, forestCopy);
            computeMAAF(t1Copy, forestCopy, sibs);

            if (forestCopy.firstElement().getNodes().size() != 1) {
                if (!(new EasyIsomorphismCheck()).run(t1Copy,
                        forestCopy.firstElement())) {
                    kPrime += 3;
                }
            }

            if (compValue != View.Computation.rSPR_DISTANCE) {
                int size = forestCopy.size();
                for (EasyTree t : forestCopy)
                    extractTree(t);
                (new EasyCuttingCycles()).run(forestCopy, tree1, tree2,
                        taxaOrdering);
                kPrime += forestCopy.size() - size;
            }

        }

        // System.out.println("kPrime: " + kPrime);
        // System.out.println("approx drSPR: " + kPrime / 3);

        return kPrime;

    }

    private void computeMAAF(EasyTree t, Vector<EasyTree> forest,
                             EasySiblings sibs) {

        if (t.getLeaves().size() > 2) {

            removeSingletons(t, forest, sibs);

            if (t.getLeaves().size() > 2) {

                sibs.updateSiblings(t, forest, true);
                Vector<String> t1Sib = sibs.popSibling();

                EasyNode v1 = sibs.getForestLeaf(t1Sib.get(0));
                String s = getNeighbour(v1).getLabel();

                if (s.equals(t1Sib.get(1))) {
                    contractSib(t, forest, t1Sib, sibs);
                    computeMAAF(t, forest, sibs);
                } else {
                    cutEdges(t, sibs, t1Sib, forest);
                    // kPrime += 3;
                }

            }

        }

        // else {
        // for (EasyTree v : forest)
        // System.out.println(v.getPhyloTree());
        // }

    }

    private void removeSingletons(EasyTree t1Copy, Vector<EasyTree> forestCopy,
                                  EasySiblings sibs) {

        HashSet<String> forestSingeltons = new HashSet<>();
        for (EasyTree t : forestCopy) {
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
                    removeNode(t1Copy, v1);
                    sibs.removeT1Leaf(s);
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

    private void cutEdges(EasyTree t, EasySiblings sibs, Vector<String> t1Sib,
                          Vector<EasyTree> forest) {

        String taxa1 = t1Sib.get(0);
        String taxa2 = t1Sib.get(1);

        EasyTree owner1 = sibs.getForestLeaf(taxa1).getOwner();
        EasyTree owner2 = sibs.getForestLeaf(taxa2).getOwner();

        int d1 = getRootDistance(sibs.getForestLeaf(taxa1));
        int d2 = getRootDistance(sibs.getForestLeaf(taxa2));

        if (d1 == d2 && sibs.getForestLeaf(taxa1).getInDegree() != 0
                && sibs.getForestLeaf(taxa2).getInDegree() != 0) {
            d1 = getRootDistance(sibs.getForestLeaf(taxa1).getParent());
            d2 = getRootDistance(sibs.getForestLeaf(taxa2).getParent());
        }

        // EasyNode onlyPendant = null;
        // if (owner1.equals(owner2)) {
        // if (d1 > d2)
        // onlyPendant = getOnlyPendant(sibs.getForestLeaf(taxa1),
        // sibs.getForestLeaf(taxa2), owner1);
        // else
        // onlyPendant = getOnlyPendant(sibs.getForestLeaf(taxa2),
        // sibs.getForestLeaf(taxa1), owner1);
        // }

        boolean cut_b_only;
        if (d1 < d2)
            cut_b_only = cutOnlyB(sibs.getForestLeaf(taxa1),
                    sibs.getForestLeaf(taxa2));
        else
            cut_b_only = cutOnlyB(sibs.getForestLeaf(taxa2),
                    sibs.getForestLeaf(taxa1));

        Vector<EasyNode> v = new Vector<>();

        // // if (!owner1.equals(owner2))
        // // v.add(sibs.getForestLeaf(taxa1));
        // if (onlyPendant == null) {
        //
        // // EasyNode neighbour = null;
        // // if (h1 >= h2)
        // // neighbour = getNeighbour(sibs.getForestLeaf(taxa1));
        // // else
        // // neighbour = getNeighbour(sibs.getForestLeaf(taxa2));
        //
        // v.add(sibs.getForestLeaf(taxa1));
        // v.add(sibs.getForestLeaf(taxa2));
        // // v.add(neighbour);
        //
        // } else {
        // v.add(onlyPendant);
        // // if (h1 > h2)
        // // v.add(sibs.getForestLeaf(taxa2));
        // // else
        // // v.add(sibs.getForestLeaf(taxa1));
        // }

        if (!cut_b_only)
            v.add(sibs.getForestLeaf(taxa1));
        kPrime++;

        EasyNode neighbour;
        if (d1 > d2)
            neighbour = getNeighbour(sibs.getForestLeaf(taxa1));
        else
            neighbour = getNeighbour(sibs.getForestLeaf(taxa2));

        if (owner1.equals(owner2) && neighbour != null)
            v.add(neighbour);
        kPrime++;

        if (!cut_b_only)
            v.add(sibs.getForestLeaf(taxa2));
        kPrime++;

        cutForest(v, forest, t, sibs);

    }

    private boolean cutOnlyB(EasyNode v1, EasyNode v2) {

        if (v1.getInDegree() == 1) {
            EasyNode p1 = v1.getParent();
            if (v2.getInDegree() == 1) {
                EasyNode p2 = v2.getParent();
                if (p2.getInDegree() == 1) {
                    EasyNode pp2 = p2.getParent();
                    if (p1.equals(pp2))
                        return true;
                }
            }
        }

        return false;
    }

    private EasyNode getOnlyPendant(EasyNode v1, EasyNode v2, EasyTree t) {

        EasyNode p1 = v1.getParent();
        EasyNode p2 = v2.getParent();

        if (p1.getInDegree() == 1) {
            EasyNode v = p1.getParent();
            if (v.equals(p2)) {
                Iterator<EasyNode> it = p1.getChildren().iterator();
                EasyNode p = it.next();
                if (v1.equals(p))
                    p = it.next();
                return p;
            }

        }

        return null;
    }

    private void cutForest(Vector<EasyNode> nodes, Vector<EasyTree> forest,
                           EasyTree t1, EasySiblings sibs) {

        for (EasyNode v : nodes) {

            EasyTree t = v.getOwner();

            if (v.getInDegree() != 0) {
                EasyTree subtree = t.pruneSubtree(v);
                sibs.putForestLeaves(subtree);
                // kPrime++;

                if (subtree.getNodes().size() == 1) {
                    String label = subtree.getNodes().firstElement().getLabel();
                    if (sibs.getT1Leaf((label)) != null) {
                        removeNode(t1, sibs.getT1Leaf(label));
                        sibs.removeT1Leaf(label);
                    }
                }
                forest.add(subtree);
            }

        }
        computeMAAF(t1, forest, sibs);
    }

    private EasyNode getNeighbour(EasyNode v) {
        EasyNode p = v.getParent();
        Iterator<EasyNode> it = p.getChildren().iterator();
        EasyNode c = it.next();
        if (!c.getLabel().equals(v.getLabel()))
            return c;
        return it.next();
    }

    private void contractSib(EasyTree t, Vector<EasyTree> forest,
                             Vector<String> t1Sib, EasySiblings sibs) {

        contractT1Leaves(t, t1Sib, sibs);
        sibs.updateTaxa(t, forest);

    }

    private void contractT1Leaves(EasyTree t, Vector<String> t1Sib,
                                  EasySiblings sibs) {

        // contracting t1
        EasyNode v1 = sibs.getT1Leaf(t1Sib.get(0));
        EasyNode v2 = sibs.getT1Leaf(t1Sib.get(1));
        EasyNode p = v1.getParent();

        Vector<String> taxa = new Vector<>();
        taxa.add(v1.getLabel());
        taxa.add(v2.getLabel());
        Collections.sort(taxa);

        t.deleteNode(v1);
        t.deleteNode(v2);

        String s = "";
        s = s.concat(taxa.get(0));
        s = s.concat("+");
        s = s.concat(taxa.get(1));

        t.setLabel(p, s);

        // setting forest label

        EasyNode f1 = sibs.getForestLeaf(t1Sib.get(0));
        EasyNode f2 = sibs.getForestLeaf(t1Sib.get(1));
        EasyNode fP = f1.getParent();
        EasyTree f = f1.getOwner();

        f.deleteNode(f1);
        f.deleteNode(f2);

        f.setLabel(fP, s);
        taxonToTaxa.put(s, taxa);
    }

    private int getRootDistance(EasyNode easyNode) {
        int h = 0;
        if (easyNode.getInDegree() != 0) {
            EasyNode p = easyNode.getParent();
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

    private Vector<EasyTree> copyForest(Vector<EasyTree> t2Forest) {
        Vector<EasyTree> v = new Vector<>();
        for (EasyTree t : t2Forest)
            v.add(copyTree(t));
        return v;
    }

    private EasyTree copyTree(EasyTree t) {
        return new EasyTree(t.getPhyloTree());
    }
}
