/*
 * Utilities.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.embed;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import jloda.util.Pair;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * utilities
 * Daniel Huson, 7.2010
 */
public class Utilities {

    /**
     * compute the shortest path distances between taxa
     *
     * @param taxon2Id numbering assumed to be 1...ntax
     * @return ntax+1 x ntax+1 distance matricx
     */
    public static double[][] computeDistances(PhyloTree tree, int ntax, Map<String, Integer> taxon2Id) {
        // compute additional maps:
        Node[] id2node = new Node[ntax + 1];
        Set<String> taxaPresent = new HashSet<String>();
        for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
            if (v.getOutDegree() == 0) {
                String taxon = tree.getLabel(v);
                int id = taxon2Id.get(taxon);
                id2node[id] = v;
                taxaPresent.add(taxon);
            }
        }

        double[][] dist = new double[ntax + 1][ntax + 1];

        for (String taxon : taxaPresent) {
            int iFirst = taxon2Id.get(taxon);
            Node vFirst = id2node[iFirst];

            NodeArray<Pair<Integer, Integer>> distFromFirst = new NodeArray<Pair<Integer, Integer>>(tree);
            computeDistFirst2AncestorRec(tree.getRoot(), vFirst, distFromFirst);
            computeDistBestAncestor2OtherRec(tree, tree.getRoot(), distFromFirst);

            for (Node vOther = tree.getFirstNode(); vOther != null; vOther = tree.getNextNode(vOther)) {
                if (vOther.getOutDegree() == 0) {
                    Integer id = taxon2Id.get(tree.getLabel(vOther));
                }
            }

            for (int iOther = 1; iOther <= ntax; iOther++) {
                Node vOther = id2node[iOther];
                if (vOther != null && vOther != vFirst) {
                    Pair<Integer, Integer> pair = distFromFirst.get(vOther);
                    if (pair != null) {
                        dist[iOther][iFirst] = dist[iFirst][iOther] = pair.getFirst() + pair.getSecond();
                    }
                }
            }
        }
        return dist;
    }

    /**
     * in a post-order traversal, computes the distance from the first node to all its ancestors
     *
     * @return true, if first node is below v
     */
    private static boolean computeDistFirst2AncestorRec(Node v, Node first, NodeArray<Pair<Integer, Integer>> distFromFirst) {
        boolean isBelow = false;

        if (v == first) {
            isBelow = true;
            Pair<Integer, Integer> pairV = new Pair<Integer, Integer>();
            pairV.setFirst(0);
            pairV.setSecond(0);
            distFromFirst.put(v, pairV);
        } else {
            Pair<Integer, Integer> pairV = new Pair<Integer, Integer>();
            pairV.setFirst(Integer.MAX_VALUE);
            pairV.setSecond(Integer.MAX_VALUE);
            distFromFirst.put(v, pairV);

            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node w = e.getTarget();
                if (computeDistFirst2AncestorRec(w, first, distFromFirst)) {
                    isBelow = true;
                    Pair<Integer, Integer> pairW = distFromFirst.get(w);
                    if (pairW.getFirst() + 1 < pairV.getFirst())
                        pairV.setFirst(pairW.getFirst() + 1);
                }
            }
        }
        return isBelow;
    }

    /**
     * in a pre-order traversal, compute the distance from the best ancestor of first to the given node v
     *
	 */
    private static void computeDistBestAncestor2OtherRec(PhyloTree tree, Node v, NodeArray<Pair<Integer, Integer>> distFromFirst) {
        Node bestParent = null;

        for (Edge e = v.getFirstInEdge(); e != null; e = v.getNextInEdge(e)) {
            Node w = e.getSource();
            Pair<Integer, Integer> pairW = distFromFirst.get(w);
            if (pairW.getSecond() == Integer.MAX_VALUE)
                return; // parents have not yet all been processed
            if (bestParent == null)
                bestParent = w;
            else {
                Pair<Integer, Integer> pairBest = distFromFirst.get(bestParent);
                if (pairW.getFirst() < pairBest.getFirst()
                        || (pairW.getFirst() == pairBest.getFirst() && pairW.getSecond() > pairBest.getSecond()))
                    bestParent = w;
            }
        }

        Pair<Integer, Integer> pairV = distFromFirst.get(v);
        if (bestParent == null) {
            if (pairV.getFirst() != Integer.MAX_VALUE)
                pairV.setSecond(0);
        } else {
            Pair<Integer, Integer> pairBest = distFromFirst.get(bestParent);
            if (pairBest.getFirst() < pairV.getFirst()) {
                pairV.setFirst(pairBest.getFirst());
                pairV.setSecond(pairBest.getSecond() + 1);
            } else if (pairV.getFirst() != Integer.MAX_VALUE)
                pairV.setSecond(0);
        }

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            Node w = e.getTarget();
            computeDistBestAncestor2OtherRec(tree, w, distFromFirst);
        }
        // if (v.getOutDegree() > 0) tree.setLabel(v, distFromFirst.get(v).toString());
    }

    /**
     * add the first matrix to the second
     *
	 */
    public static void add(double[][] first, double[][] second) {
        for (int i = 0; i < first.length; i++) {
            for (int j = 0; j < first[i].length; j++)
                second[i][j] += first[i][j];
        }
    }
}
