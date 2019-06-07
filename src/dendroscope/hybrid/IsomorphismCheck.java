/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import jloda.graph.Node;

import java.util.*;

/**
 * Given two rooted, bifurcating phylogenetic trees T1 and T2, this function
 * checks whether T1 and T2 are isomorphic.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class IsomorphismCheck {

    @SuppressWarnings("unchecked")
    public boolean run(HybridTree n1, HybridTree n2) {

        HybridTree n1Mod = new HybridTree(n1, false, (Vector<String>) n1
                .getTaxaOrdering().clone());
        HybridTree n2Mod = new HybridTree(n2, false, (Vector<String>) n2
                .getTaxaOrdering().clone());

        if (n1Mod.getNumberOfNodes() != n2Mod.getNumberOfNodes())
            return false;

        while (n1Mod.getNumberOfNodes() > 3) {

            HashSet<String> t1Cherrys = new HashSet<>();
            Hashtable<String, Node> t1Taxa2parent = new Hashtable<>();

            //collect all cherries in t1
            //-> a cherry is a sorted string assembled by its taxon labelings
            getCherrys(n1Mod, t1Cherrys, t1Taxa2parent);

            HashSet<String> t2Cherrys = new HashSet<>();
            Hashtable<String, Node> t2Taxa2parent = new Hashtable<>();

            //collect all cherries in t2
            getCherrys(n2Mod, t2Cherrys, t2Taxa2parent);

            //compare the two cherry sets..
            if (t1Cherrys.size() != t2Cherrys.size())
                return false;

            for (String t2Cherry : t2Cherrys) {
                if (!t1Cherrys.contains(t2Cherry))
                    return false;
            }

            //generate new cherries in both trees
            if (n1Mod.getNumberOfNodes() > 3) {
                replaceCherrys(n1Mod, t1Taxa2parent);
                replaceCherrys(n2Mod, t2Taxa2parent);
            } else
                return true;

        }

        return true;
    }

    private void replaceCherrys(HybridTree n,
                                Hashtable<String, Node> taxa2parent) {
        for (String taxon : taxa2parent.keySet()) {
            Node v = taxa2parent.get(taxon);
            Node newV = n.newNode();
            n.setLabel(newV, taxon);
            n.deleteSubtree(v, newV, true);
        }
    }

    private void getCherrys(HybridTree n, HashSet<String> cherrys,
                            Hashtable<String, Node> taxa2parent) {
        Iterator<Node> it = n.computeSetOfLeaves().iterator();
        Vector<Node> parents = new Vector<>();
        while (it.hasNext()) {
            Node v = it.next();
            Node p = v.getFirstInEdge().getSource();
            if (!parents.contains(p) && isCherry(p)) {

                Vector<String> taxa = new Vector<>();

                for (Node c : p.children()) {
                    taxa.add(n.getLabel(c));
                }

                //sort taxas lexicographically
                Collections.sort(taxa);

                //generate cherry-string
                String taxaString = "";
                for (String s : taxa)
                    taxaString = taxaString.concat(s);
                cherrys.add(taxaString);

                parents.add(p);
                taxa2parent.put(taxaString, p);
            }
        }
    }

    private boolean isCherry(Node p) {
        for (Node c : p.children()) {
            if (c.getOutDegree() != 0)
                return false;
        }
        return true;
    }
}
