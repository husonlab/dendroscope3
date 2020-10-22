/*
 *   NetworkIsomorphismCheck.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.cmpMinNetworks;

import dendroscope.hybroscale.model.treeObjects.SparseNetEdge;
import dendroscope.hybroscale.model.treeObjects.SparseNetNode;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;

import java.util.*;

/**
 * Given two rooted, bifurcating phylogenetic networks N1 and N2, this function
 * checks whether N1 and N2 are isomorphic.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class NetworkIsomorphismCheck {

    public boolean run(SparseNetwork n1, SparseNetwork n2) {

        SparseNetwork n1Copy = new SparseNetwork(n1);
        SparseNetwork n2Copy = new SparseNetwork(n2);


        if (n1Copy.getNodes().size() != n2Copy.getNodes().size())
            return false;

        if (n1Copy.getNodes().size() < 3) {
            Vector<String> leafLabels = new Vector<String>();
            for (SparseNetNode v : n1Copy.getLeaves())
                leafLabels.add(v.getLabel());

            for (SparseNetNode v : n2Copy.getLeaves()) {
                if (!leafLabels.contains(v.getLabel()))
                    return false;
            }
            return true;
        }

        while (n1Copy.getNodes().size() > 2) {

//			System.out.println("--\n"+n1Copy.getPhyloTree()+";");
//			System.out.println(n2Copy.getPhyloTree()+";\n");

            HashSet<String> n1Cherrys = new HashSet<String>();
            Hashtable<String, Vector<SparseNetNode>> n1Taxa2parent = new Hashtable<String, Vector<SparseNetNode>>();
            Hashtable<SparseNetNode, String> n1parent2edgeLabel = new Hashtable<SparseNetNode, String>();

            // collect all cherries in t1
            // -> a cherry is a sorted string assembled by its taxa
            getCherrys(n1Copy, n1Cherrys, n1Taxa2parent, n1parent2edgeLabel);

            HashSet<String> n2Cherrys = new HashSet<String>();
            Hashtable<String, Vector<SparseNetNode>> n2Taxa2parent = new Hashtable<String, Vector<SparseNetNode>>();
            Hashtable<SparseNetNode, String> n2parent2edgeLabel = new Hashtable<SparseNetNode, String>();

            // collect all cherries in t2
            getCherrys(n2Copy, n2Cherrys, n2Taxa2parent, n2parent2edgeLabel);

            // compare the two cherry sets..
            if (n1Cherrys.size() != n2Cherrys.size()) {
//				System.out.println("Case-1");
//				System.out.println(n1Cherrys);
//				System.out.println(n2Cherrys);
                return false;
            }

            Iterator<String> it = n2Cherrys.iterator();
            while (it.hasNext()) {
                String taxaString = it.next();
                if (!n1Cherrys.contains(taxaString)) {
//					System.out.println("Case-2");
//					System.out.println(n1Cherrys);
//					System.out.println(n2Cherrys);
                    return false;
                } else {
                    Vector<SparseNetNode> parents1 = n1Taxa2parent.get(taxaString);
                    Vector<SparseNetNode> parents2 = n2Taxa2parent.get(taxaString);

                    Vector<String> edgeStrings1 = new Vector<String>();
                    for (SparseNetNode p1 : parents1)
                        edgeStrings1.add(n1parent2edgeLabel.get(p1));

                    Vector<String> edgeStrings2 = new Vector<String>();
                    for (SparseNetNode p2 : parents2)
                        edgeStrings2.add(n2parent2edgeLabel.get(p2));

                    for (String edgeString1 : edgeStrings1) {
                        if (!edgeStrings2.contains(edgeString1)) {
//							System.out.println("Case-3");
//							for(String s : edgeStrings2)
//								System.out.println("|"+edgeString1+"|vs|"+s+"|");
                            return false;
                        }
                    }
                }
            }

            // generate new cherries in both trees
            if (n1Copy.getNodes().size() > 2) {
                replaceCherrys(n1Copy, n1Taxa2parent);
                replaceCherrys(n2Copy, n2Taxa2parent);
            } else {
                Vector<String> leafLabels = new Vector<String>();
                for (SparseNetNode v : n1Copy.getLeaves())
                    leafLabels.add(v.getLabel());
                for (SparseNetNode v : n2Copy.getLeaves()) {
                    if (!leafLabels.contains(v.getLabel())) {
//						System.out.println("Case-4");
                        return false;
                    }
                }
                return true;
            }

        }

        return true;
    }

    private void replaceCherrys(SparseNetwork n, Hashtable<String, Vector<SparseNetNode>> n1Taxa2parent) {

        Iterator<String> it = n1Taxa2parent.keySet().iterator();
        while (it.hasNext()) {
            String taxon = it.next();
            for (SparseNetNode v : n1Taxa2parent.get(taxon)) {
                v.setLabel(taxon);
                v.removeAllChildren();
            }
        }

    }

    private void getCherrys(SparseNetwork n, HashSet<String> cherrys,
                            Hashtable<String, Vector<SparseNetNode>> n1Taxa2parent, Hashtable<SparseNetNode, String> parent2edgeLabels) {
        HashSet<SparseNetNode> leaves = new HashSet<SparseNetNode>();
        for (SparseNetNode v : n.getNodes()) {
            if (v.getOutDegree() == 0 && v.getInDegree() > 0)
                leaves.add(v);
        }
        Iterator<SparseNetNode> it = leaves.iterator();
        Vector<SparseNetNode> parents = new Vector<SparseNetNode>();
        while (it.hasNext()) {
            SparseNetNode v = it.next();
            for (SparseNetEdge eIn : v.getInEdges()) {
                SparseNetNode p = eIn.getSource();
                if (!parents.contains(p) && isCherry(p)) {

                    // collect&sort taxa
                    Vector<String> taxa = new Vector<String>();
                    for (SparseNetEdge e : p.getOutEdges()) {
                        String taxon = e.getTarget().getLabel().replaceAll(" +", "");
                        taxa.add(taxon);
                    }
                    Collections.sort(taxa);

                    // collect&sort edge indices
                    Vector<String> allIndices = new Vector<String>();
                    for (SparseNetEdge e : p.getOutEdges()) {
                        Vector<Integer> indices = new Vector<Integer>();
                        indices.addAll(e.getIndices());
                        Collections.sort(indices);
                        String indexString = "";
                        for (int index : indices)
                            indexString = indexString.concat(String.valueOf(index));
                        allIndices.add(indexString);
                    }
                    Collections.sort(allIndices);

                    // generate cherry-string
                    String taxaString = "";
                    for (String s : taxa)
                        taxaString = taxaString.concat(s + " ");

                    cherrys.add(taxaString);

                    // generate edge-string
                    String edgeString = "";
                    for (String s : allIndices)
                        edgeString = edgeString.concat(s + " ");

                    parents.add(p);
                    if (!n1Taxa2parent.containsKey(taxaString))
                        n1Taxa2parent.put(taxaString, new Vector<SparseNetNode>());
                    // n1Taxa2parent.get(taxaString).add(p);
                    Vector<SparseNetNode> newParents = (Vector<SparseNetNode>) n1Taxa2parent.get(taxaString).clone();
                    newParents.add(p);
                    n1Taxa2parent.put(taxaString, newParents);
                    parent2edgeLabels.put(p, edgeString);

                }
            }
        }
    }

    private boolean isCherry(SparseNetNode p) {
        for (SparseNetEdge e : p.getOutEdges()) {
            if (e.getTarget().getOutDegree() != 0)
                return false;
        }
        return true;
    }
}
