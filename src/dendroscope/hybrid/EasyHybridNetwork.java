/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import jloda.graph.Edge;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;

public class EasyHybridNetwork extends EasyTree {

    private final String altLabel;
    private final Vector<String> taxaOrdering;
    protected Hashtable<Vector<String>, Integer> taxaPairToWeight;
    private final String replacementCharacter;
    protected Hashtable<Edge, Integer> edgeToWeight = new Hashtable<>();

    public EasyHybridNetwork(HybridTree t) {
        super(t);
        this.altLabel = t.getAltLabel();
        this.taxaOrdering = t.getTaxaOrdering();
        this.taxaPairToWeight = t.getTaxaPairToWeight();
        this.replacementCharacter = t.getReplacementCharacter();
        this.edgeToWeight = t.edgeToWeight;
    }

    public EasyHybridNetwork(EasyTree t, String altLabel,
                             Vector<String> taxaOrdering,
                             Hashtable<Vector<String>, Integer> taxaPairToWeight,
                             String replacementCharacter) {
        super(t);
        this.altLabel = altLabel;
        this.taxaOrdering = taxaOrdering;
        this.replacementCharacter = replacementCharacter;
    }

    public EasyHybridNetwork pruneSubtree(EasyNode v) {
        EasyTree eT = super.pruneSubtree(v);
        return new EasyHybridNetwork(eT, altLabel, taxaOrdering,
                taxaPairToWeight, replacementCharacter);
    }

    public int getWeight(EasyNode v) {
        EasyNode p = v.getParent();
        Vector<EasyNode> pChilds = getNodeLeaves(p);
        Vector<EasyNode> vChilds = getNodeLeaves(v);
        for (EasyNode a : pChilds) {
            for (EasyNode b : vChilds) {
                Vector<String> taxaPair = new Vector<>();
                taxaPair.add(a.getLabel());
                taxaPair.add(b.getLabel());
                Collections.sort(taxaPair);
                if (taxaPairToWeight.containsKey(taxaPair))
                    return taxaPairToWeight.get(taxaPair);
            }
        }
        return 0;
    }

    public Vector<EasyNode> getNodeLeaves(EasyNode v) {
        Vector<EasyNode> leaves = new Vector<>();
        for (EasyNode c : v.getChildren()) {
            if (c.getOutDegree() == 0)
                leaves.add(c);
        }
        return leaves;
    }

    public HybridTree getHybridTree() {
        HybridTree t = new HybridTree(super.getPhyloTree(), false, taxaOrdering);
        t.setReplacementCharacter(replacementCharacter);
        t.setTaxaPairToWeight(taxaPairToWeight);
        t.setAltLabel(altLabel);
        t.setEdgeToWeight(edgeToWeight);
        t.setRoot(t.getRoot());
        return t;
    }

    public String getAltLabel() {
        return altLabel;
    }

    public Vector<String> getTaxaOrdering() {
        return taxaOrdering;
    }

    public Hashtable<Vector<String>, Integer> getTaxaPairToWeight() {
        return taxaPairToWeight;
    }

    public String getReplacementCharacter() {
        return replacementCharacter;
    }

}
