/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import jloda.graph.Edge;
import jloda.graph.Node;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class Siblings {

    private final Hashtable<String, Node> t1TaxaToNode = new Hashtable<>();
    private final Hashtable<String, Node> forestTaxaToNode = new Hashtable<>();

    private final Hashtable<String, Vector<String>> taxaToSiblingPair = new Hashtable<>();
    private final HashSet<Vector<String>> siblingsOfT1 = new HashSet<>();

    public void init(HybridTree t1, Vector<HybridTree> forest) {

        initTaxaToNode(t1, t1TaxaToNode);

        for (HybridTree t : forest)
            initTaxaToNode(t, forestTaxaToNode);

    }

    private void getSiblingPairs(HybridTree t,
                                 HashSet<Vector<String>> siblings, boolean onlyOne) {
        if (t.computeSetOfLeaves().size() >= 2) {
            Iterator<Node> it = t.computeSetOfLeaves().iterator();
            HashSet<Node> parents = new HashSet<>();
            while (it.hasNext()) {
                Node v = it.next();
                Node p = v.getFirstInEdge().getSource();
                if (!parents.contains(p) && isCherry(p)) {

                    parents.add(p);

                    Vector<String> taxa = new Vector<>();
                    Iterator<Edge> it2 = p.outEdges().iterator();

                    // collecting taxa
                    while (it2.hasNext())
                        taxa.add(t.getLabel(it2.next().getTarget()));

                    taxaToSiblingPair.put(taxa.get(0), taxa);
                    taxaToSiblingPair.put(taxa.get(1), taxa);
                    siblings.add(taxa);

                    if (onlyOne)
                        break;

                }
            }
        }
    }

    private boolean isCherry(Node p) {
        for (Edge e : p.outEdges()) {
            if (e.getTarget().getOutDegree() != 0)
                return false;
        }
        return true;
    }

    public void updateTaxa(HybridTree t1, Vector<HybridTree> forest) {

        t1TaxaToNode.clear();
        forestTaxaToNode.clear();

        initTaxaToNode(t1, t1TaxaToNode);

        for (HybridTree t : forest)
            initTaxaToNode(t, forestTaxaToNode);
    }

    public void updateTaxa(HybridTree t1) {
        t1TaxaToNode.clear();
        initTaxaToNode(t1, t1TaxaToNode);
    }

    public void updateTaxa(Vector<HybridTree> forest) {
        forestTaxaToNode.clear();
        for (HybridTree t : forest)
            initTaxaToNode(t, forestTaxaToNode);
    }

    public void updateSiblings(HybridTree t1, Vector<HybridTree> forest,
                               boolean onlyOne) {
        siblingsOfT1.clear();
        getSiblingPairs(t1, siblingsOfT1, onlyOne);
    }

    public HashSet<Vector<String>> getSiblingsOfT1() {
        return siblingsOfT1;
    }

    private void initTaxaToNode(HybridTree t, Hashtable<String, Node> taxaToNode) {
        for (Node v : t.computeSetOfLeaves())
            taxaToNode.put(t.getLabel(v), v);
    }

    public Node getT1Leaf(String s) {
        return t1TaxaToNode.get(s);
    }

    public Node getForestLeaf(String s) {
        return forestTaxaToNode.get(s);
    }

    public void removeT1Leaf(String s) {
        t1TaxaToNode.remove(s);
        if (taxaToSiblingPair.containsKey(s))
            siblingsOfT1.remove(taxaToSiblingPair.get(s));
    }

    public void putT1Leaf(String s, Node v) {
        t1TaxaToNode.put(s, v);
    }

    public void removeForestLeaves(HybridTree t) {
        for (Node v : t.nodes())
            forestTaxaToNode.remove(t.getLabel(v));
    }

    public void removeForestLeaf(String s) {
        forestTaxaToNode.remove(s);
    }

    public void putForestLeaves(HybridTree t) {
        for (Node v : t.nodes()) {
            forestTaxaToNode.remove(t.getLabel(v));
            forestTaxaToNode.put(t.getLabel(v), v);
        }
    }

    public void putForestLeaf(String s, Node v) {
        forestTaxaToNode.put(s, v);
    }

    @SuppressWarnings("unchecked")
    public Vector<String> popSibling() {
        Vector<String> siblingPair = (Vector<String>) siblingsOfT1.iterator()
                .next().clone();
        siblingsOfT1.remove(siblingPair);
        taxaToSiblingPair.remove(siblingPair.get(0));
        taxaToSiblingPair.remove(siblingPair.get(1));
        return siblingPair;
    }

    public int getNumOfSiblings() {
        return siblingsOfT1.size();
    }

}
