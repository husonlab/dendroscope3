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

import java.util.*;

public class ForestApproximation {

    private double kPrime;
    private final Hashtable<String, Vector<String>> taxonToTaxa;

    public ForestApproximation(Hashtable<String, Vector<String>> taxonToTaxa) {
        if (taxonToTaxa != null)
            this.taxonToTaxa = taxonToTaxa;
        else
            this.taxonToTaxa = new Hashtable<>();
    }

    public double run(HybridTree tree1, HybridTree tree2, HybridTree t1,
                      Vector<HybridTree> forest, int compValue) {

        kPrime = forest.size() - 1;

        if (forest.size() != 1 || !(new IsomorphismCheck()).run(t1, forest.firstElement())) {

            HybridTree t1Copy = new HybridTree(t1, false, t1.getTaxaOrdering());
            Vector<HybridTree> forestCopy = copyForest(forest);

            Siblings sibs = new Siblings();
            sibs.init(t1Copy, forestCopy);
            computeMAAF(t1Copy, forestCopy, sibs);

            if (compValue != 2) {
                for (HybridTree f : forestCopy) {
                    extractTree(f);
                    f.update();
                }
                int size = forestCopy.size();
                (new CuttingCycles()).run(forestCopy, tree1, tree2);
                kPrime += forestCopy.size() - size;
            }

        }

//		 System.out.println("kPrime: " + kPrime);
//		 System.out.println("approx drSPR: " + kPrime / 3);

        return kPrime;

    }

    private void computeMAAF(HybridTree t1, Vector<HybridTree> forest,
                             Siblings sibs) {

        if (t1.computeSetOfLeaves().size() > 2) {

            removeSingletons(t1, forest, sibs);

            if (t1.computeSetOfLeaves().size() > 2) {

//				if (sibs.getNumOfSiblings() == 0)
                sibs.updateSiblings(t1, forest, true);

                Vector<String> t1Sib = sibs.popSibling();
                Node v1 = sibs.getForestLeaf(t1Sib.get(0));
                HybridTree t = (HybridTree) v1.getOwner();
                String s = t.getLabel(getNeighbour(v1));

                if (s.equals(t1Sib.get(1))) {
                    // System.out.println("Contract Phase " + kPrime);
                    contractSib(t1, forest, t1Sib, sibs);
                    computeMAAF(t1, forest, sibs);
                } else {
                    kPrime += 3;
                    // System.out.println("Cutting Phase " + kPrime);
                    cutEdges(t1, sibs, t1Sib, forest);
                }

            } else if (!(new IsomorphismCheck()).run(t1, forest.firstElement())) {
                kPrime += 3;
                // System.out.println(t1);
                // System.out.println(forest.firstElement());
                // System.out.println("Finish Phase " + kPrime);
            }

        } else if (!(new IsomorphismCheck()).run(t1, forest.firstElement())) {
            kPrime += 3;
            // System.out.println(t1);
            // System.out.println(forest.firstElement());
            // System.out.println("Finish Phase " + kPrime);
        }

    }

    private void removeSingletons(HybridTree t1, Vector<HybridTree> forest,
                                  Siblings sibs) {

        HashSet<String> forestSingeltons = new HashSet<>();
        for (HybridTree t : forest) {
            if (!Objects.equals(t.getLabel(t.getRoot()), "rho"))
                forestSingeltons.add(t.getLabel(t.getRoot()));
        }

        Vector<String> deletedTaxa = new Vector<>();
        boolean isActive = true;
        while (isActive) {
            isActive = false;
            for (String s : forestSingeltons) {
                Node v1 = sibs.getT1Leaf(s);
                if (v1 != null && !deletedTaxa.contains(s)) {
                    removeNode(t1, v1);
                    sibs.removeT1Leaf(s);
                    deletedTaxa.add(s);
//					isActive = true;
                }
            }
        }
    }

    private void removeNode(HybridTree t, Node v) {
        if (t.computeSetOfLeaves().size() > 2) {
            Edge e = v.getInEdges().next();
            Node p = e.getSource();
            t.deleteEdge(e);
            t.deleteNode(v);
            removeNeedlessNode(t, p);
        }
    }

    private void removeNeedlessNode(HybridTree t, Node v) {
        if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
            Edge inEdge = v.getInEdges().next();
            Node pP = inEdge.getSource();
            Edge outEdge = v.getOutEdges().next();
            Node c = outEdge.getTarget();
            t.deleteEdge(inEdge);
            t.deleteEdge(outEdge);
            t.deleteNode(v);
            t.newEdge(pP, c);
        } else if (v.getInDegree() == 0 && v.getOutDegree() == 1) {
            Edge outEdge = v.getOutEdges().next();
            Node c = outEdge.getTarget();
            t.deleteEdge(outEdge);
            t.deleteNode(v);
            t.setRoot(c);
        }
    }

    private void cutEdges(HybridTree t1, Siblings sibs, Vector<String> t1Sib,
                          Vector<HybridTree> forest) {

        String taxa1 = t1Sib.get(0);
        String taxa2 = t1Sib.get(1);

        HybridTree owner1 = (HybridTree) sibs.getForestLeaf(taxa1).getOwner();
        HybridTree owner2 = (HybridTree) sibs.getForestLeaf(taxa2).getOwner();

        int h1 = getHeight(sibs.getForestLeaf(taxa1));
        int h2 = getHeight(sibs.getForestLeaf(taxa2));

        Node onlyPendant = null;
        if (owner1.equals(owner2)) {
            if (h1 > h2)
                onlyPendant = getOnlyPendant(sibs.getForestLeaf(taxa1), sibs
                        .getForestLeaf(taxa2), owner1);
            else
                onlyPendant = getOnlyPendant(sibs.getForestLeaf(taxa2), sibs
                        .getForestLeaf(taxa1), owner1);
        }

        Vector<Node> v = new Vector<>();

        if (!owner1.equals(owner2))
            v.add(sibs.getForestLeaf(taxa1));
        else if (onlyPendant == null) {

            Node neighbour;
            if (h1 >= h2)
                neighbour = getNeighbour(sibs.getForestLeaf(taxa1));
            else
                neighbour = getNeighbour(sibs.getForestLeaf(taxa2));

            v.add(sibs.getForestLeaf(taxa1));
            v.add(sibs.getForestLeaf(taxa2));
            v.add(neighbour);
        } else
            v.add(onlyPendant);

        cutForest(v, forest, t1, sibs);

    }

    private Node getOnlyPendant(Node v1, Node v2, HybridTree t) {

        Node p1 = v1.getInEdges().next().getSource();
        Node p2 = v2.getInEdges().next().getSource();

        if (p1.getInDegree() == 1) {
            Node v = p1.getInEdges().next().getSource();
            if (v.equals(p2)) {
                Iterator<Edge> it = p1.getOutEdges();
                Node p = it.next().getTarget();
                if (v1.equals(p))
                    p = it.next().getTarget();
                return p;
            }

        }

        return null;
    }

    private void cutForest(Vector<Node> nodes, Vector<HybridTree> forest,
                           HybridTree t1, Siblings sibs) {

        for (Node v : nodes) {

            HybridTree t = (HybridTree) v.getOwner();

            if (v.getInDegree() != 0) {
                Node p = v.getInEdges().next().getSource();
                HybridTree subtree = t.getSubtree(v, false);
                sibs.putForestLeaves(subtree);

                if (subtree.getNodes().size() == 1) {
                    String label = subtree.getLabel(subtree.getFirstNode());
                    if (sibs.getT1Leaf((label)) != null) {
                        removeNode(t1, sibs.getT1Leaf(label));
                        sibs.removeT1Leaf(label);
                    }
                }

                t.deleteSubtree(v, null, false);
                forest.add(subtree);
                removeNeedlessNode(t, p);
            }

        }

        computeMAAF(t1, forest, sibs);
    }

    private Node getNeighbour(Node v) {

        Node p = v.getInEdges().next().getSource();
        Iterator<Edge> it = p.getOutEdges();
        Node c = it.next().getTarget();
        if (!c.equals(v))
            return c;

        return it.next().getTarget();
    }

    private void contractSib(HybridTree t1, Vector<HybridTree> forest,
                             Vector<String> t1Sib, Siblings sibs) {

        contractT1Leaves(t1, t1Sib, sibs);

    }

    private void contractT1Leaves(HybridTree t1, Vector<String> t1Sib,
                                  Siblings sibs) {

        Node v1 = sibs.getT1Leaf(t1Sib.get(0));
        Node v2 = sibs.getT1Leaf(t1Sib.get(1));

        Vector<String> taxa = new Vector<>();
        taxa.add(t1.getLabel(v1));
        taxa.add(t1.getLabel(v2));
        Collections.sort(taxa);

        Edge e = v1.getInEdges().next();
        Node p = e.getSource();

        t1.deleteEdge(e);
        sibs.removeT1Leaf(t1.getLabel(v1));
        t1.deleteNode(v1);

        e = v2.getInEdges().next();
        t1.deleteEdge(e);
        sibs.removeT1Leaf(t1.getLabel(v2));
        t1.deleteNode(v2);

        String s = "";
        s = s.concat(taxa.get(0));
        s = s.concat("+");
        s = s.concat(taxa.get(1));

        sibs.putT1Leaf(s, p);
        t1.setLabel(p, s);

        Node f1 = sibs.getForestLeaf(t1Sib.get(0));
        Edge eF = f1.getInEdges().next();
        Node fP = eF.getSource();
        HybridTree f = (HybridTree) fP.getOwner();

        f.deleteEdge(eF);
        sibs.removeForestLeaf(f.getLabel(f1));
        f.deleteNode(f1);

        f.setLabel(fP, s);
        sibs.putForestLeaf(s, fP);

        Node f2 = sibs.getForestLeaf(t1Sib.get(1));
        eF = f2.getInEdges().next();

        f.deleteEdge(eF);
        sibs.removeForestLeaf(f.getLabel(f2));
        f.deleteNode(f2);

        taxonToTaxa.put(s, taxa);
    }

    private int getHeight(Node v) {
        int h = 0;
        if (v.getInDegree() != 0) {
            Node p = v.getInEdges().next().getSource();
            h = 1;
            while (p.getInDegree() != 0) {
                p = p.getInEdges().next().getSource();
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

    private Vector<HybridTree> copyForest(Vector<HybridTree> forest) {
        Vector<HybridTree> v = new Vector<>();
        for (HybridTree t : forest)
            v.add(new HybridTree(t, false, t.getTaxaOrdering()));
        return v;
    }

}
