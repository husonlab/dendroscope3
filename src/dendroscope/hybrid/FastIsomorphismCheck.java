/**
 * FastIsomorphismCheck.java 
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
 * Given two rooted, bifurcating phylogenetic trees T1 and T2, this function
 * checks whether T1 and T2 are isomorphic.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class FastIsomorphismCheck {

    private final Vector<Node> currentN1Leaves = new Vector<>();
    private int n1NodeSize;
    private Node root;

    @SuppressWarnings("unchecked")
    public boolean run(HybridTree n1, Vector<Node> roots, Node r, HybridTree n2) {

        HybridTree n1Mod = new HybridTree(n1, false, (Vector<String>) n1
                .getTaxaOrdering().clone());
        HybridTree n2Mod = new HybridTree(n2, false, (Vector<String>) n2
                .getTaxaOrdering().clone());
        n1Mod.update();
        n2Mod.update();

        Vector<Node> rootNodes = initRootNodes(n1, roots, r, n1Mod);

        initBlockeNodes(n1Mod, rootNodes);
        init(n1Mod, rootNodes);


        if (n1NodeSize != n2Mod.getNumberOfNodes())
            return false;

        while (n2Mod.getNumberOfNodes() > 3) {

            HashSet<String> t1Cherrys = new HashSet<>();
            Hashtable<String, Node> t1Taxa2parent = new Hashtable<>();

            // collect all cherries in t1
            // -> a cherry is a sorted string assembled by its taxon labelings
            getN1Cherrys(n1Mod, t1Cherrys, t1Taxa2parent, rootNodes);

            HashSet<String> t2Cherrys = new HashSet<>();
            Hashtable<String, Node> t2Taxa2parent = new Hashtable<>();

            // collect all cherries in t2
            getN2Cherrys(n2Mod, t2Cherrys, t2Taxa2parent);

            // compare the two cherry sets..
            if (t1Cherrys.size() != t2Cherrys.size())
                return false;

            for (String t2Cherry : t2Cherrys) {
                if (!t1Cherrys.contains(t2Cherry)) {
                    return false;
                }
            }

            // generate new cherries in both trees
            if (n1Mod.getNumberOfNodes() > 3) {
                replaceN1Cherrys(n1Mod, t1Taxa2parent);
                replaceN2Cherrys(n2Mod, t2Taxa2parent);
            } else
                return true;

            currentN1Leaves.clear();
            init(n1Mod, rootNodes);

        }

        return true;
    }

    private void initBlockeNodes(HybridTree n, Vector<Node> rootNodes) {
        Vector<Node> blockedNodes = new Vector<>();
        Iterator<Node> it = n.postOrderWalk();
        while (it.hasNext()) {
            Node v = it.next();
            if (rootNodes.contains(v) && v.getInDegree() == 1) {
                Node p = v.getInEdges().next().getSource();
                if (blockedNodes.contains(p))
                    rootNodes.add(p);
                else
                    blockedNodes.add(p);
            }
        }
    }

    private Vector<Node> initRootNodes(HybridTree n1, Vector<Node> roots,
                                       Node r, HybridTree n1Mod) {
        Vector<Node> rootNodes = new Vector<>();

        HashSet<BitSet> rootClusters = new HashSet<>();
        for (Node v : roots)
            rootClusters.add(n1.getNodeToCluster().get(v));

        for (Node v : n1Mod.getNodes()) {
            BitSet b = n1Mod.getNodeToCluster().get(v);
            if (b.equals(n1.getNodeToCluster().get(r)))
                root = v;
            else if (rootClusters.contains(b))
                rootNodes.add(v);
        }

        return rootNodes;
    }

    private void init(HybridTree n1Mod, Vector<Node> rootNodes) {
        n1NodeSize = 1;
        initRec(root, rootNodes);
    }

    private void initRec(Node v, Vector<Node> rootNodes) {
        Iterator<Edge> it = v.getOutEdges();
        while (it.hasNext()) {
            Edge e = it.next();
            Node t = e.getTarget();
            if (!rootNodes.contains(t)) {
                if (!hasRootChild(v, rootNodes))
                    n1NodeSize++;
                if (t.getOutDegree() == 0)
                    currentN1Leaves.add(t);
                else
                    initRec(t, rootNodes);
            }

        }
    }

    private void replaceN1Cherrys(HybridTree n,
                                  Hashtable<String, Node> taxa2parent) {
        for (String taxon : taxa2parent.keySet()) {
            Node v = taxa2parent.get(taxon);
            Node newV = n.newNode();
            n.setLabel(newV, taxon);
            n.deleteSubtree(v, newV, true);
        }
    }

    private void replaceN2Cherrys(HybridTree n,
                                  Hashtable<String, Node> taxa2parent) {
        for (String taxon : taxa2parent.keySet()) {
            Node v = taxa2parent.get(taxon);
            Node newV = n.newNode();
            n.setLabel(newV, taxon);
            n.deleteSubtree(v, newV, true);
        }
    }

    private void getN1Cherrys(HybridTree n, HashSet<String> cherrys,
                              Hashtable<String, Node> taxa2parent, Vector<Node> rootNodes) {

        Vector<Node> parents = new Vector<>();
        Hashtable<Node, Node> nodeToChilds = new Hashtable<>();

        for (Node v : currentN1Leaves) {

//			System.out.println("Leaf " + n.getLabel(v));

            Node p = v.getInEdges().next().getSource();

            while (hasRootChild(p, rootNodes))
                p = p.getInEdges().next().getSource();

//			System.out.println("P " + n.getLabel(p));

            if (!parents.contains(p) && isCherry(p)) {

                Vector<String> taxa = new Vector<>();
                Iterator<Edge> it2 = p.getOutEdges();

                // collect taxa
                while (it2.hasNext()) {
                    Node child = getChild(it2.next().getTarget(), rootNodes);
                    taxa.add(n.getLabel(child));
                }

                // sort taxas lexicographically
                Collections.sort(taxa);

                // generate cherry-string
                String taxaString = "";
                for (String s : taxa)
                    taxaString = taxaString.concat(s);
                cherrys.add(taxaString);

                parents.add(p);
                taxa2parent.put(taxaString, p);

            } else if (nodeToChilds.containsKey(p)) {
                Node child = nodeToChilds.get(p);
                Vector<String> taxa = new Vector<>();
                taxa.add(n.getLabel(v));
                taxa.add(n.getLabel(child));

                // sort taxas lexicographically
                Collections.sort(taxa);

                // generate cherry-string
                String taxaString = "";
                for (String s : taxa)
                    taxaString = taxaString.concat(s);
                cherrys.add(taxaString);

                parents.add(p);
                taxa2parent.put(taxaString, p);

            } else
                nodeToChilds.put(p, v);
        }

    }

    private Node getChild(Node child, Vector<Node> rootNodes) {
        while (hasRootChild(child, rootNodes)) {
            Iterator<Edge> it = child.getOutEdges();
            while (it.hasNext()) {
                Node v = it.next().getTarget();
                if (!rootNodes.contains(v))
                    child = v;
            }
        }
        return child;
    }

    private boolean hasRootChild(Node p, Vector<Node> rootNodes) {
        Iterator<Edge> it = p.getOutEdges();
        while (it.hasNext()) {
            if (rootNodes.contains(it.next().getTarget()))
                return true;
        }
        return false;
    }

    private void getN2Cherrys(HybridTree n, HashSet<String> cherrys,
                              Hashtable<String, Node> taxa2parent) {
        Iterator<Node> it = n.computeSetOfLeaves().iterator();
        Vector<Node> parents = new Vector<>();
        while (it.hasNext()) {
            Node v = it.next();
            Node p = v.getInEdges().next().getSource();
            if (!parents.contains(p) && isCherry(p)) {

                Vector<String> taxa = new Vector<>();
                Iterator<Edge> it2 = p.getOutEdges();

                // collect taxa
                while (it2.hasNext())
                    taxa.add(n.getLabel(it2.next().getTarget()));

                // sort taxas lexicographically
                Collections.sort(taxa);

                // generate cherry-string
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
        Iterator<Edge> it = p.getOutEdges();
        while (it.hasNext()) {
            Node v = it.next().getTarget();
            if (v.getOutDegree() != 0)
                return false;
        }
        return true;
    }

}
