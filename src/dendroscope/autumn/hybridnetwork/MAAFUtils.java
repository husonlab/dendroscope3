/*
 *   MAAFUtils.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.autumn.hybridnetwork;

import dendroscope.autumn.Root;
import jloda.graph.Edge;
import jloda.graph.Graph;
import jloda.graph.Node;

import java.util.*;

/**
 * utilities for MAAFs
 * Daniel Huson, 10.2011
 */
public class MAAFUtils {
    /**
     * if there is more than one network for the same MAAF, removes all but one of them
     *
     * @return number of networks removed
     */
    public static int removeDuplicateMAAFs(Set<Root> networks, boolean verbose) {
		LinkedList<Root> input = new LinkedList<Root>(networks);

        Set<Root> maafs = new TreeSet<Root>(new NetworkComparator());

        int count = 0;
        Map<Root, Root> maaf2extendedMaaf = new TreeMap<Root, Root>(new NetworkComparator());
        Map<Root, Root> maaf2network = new TreeMap<Root, Root>(new NetworkComparator());
        for (Root network : input) {
            Root maaf = computeMAAF(network, false);
            if (!maafs.contains(maaf)) {
                maafs.add(maaf);
                maaf2extendedMaaf.put(maaf, computeMAAF(network, true));
                maaf2network.put(maaf, network);
            } else {
                networks.remove(network); // remove, as has same maaf as a previous network
                if (verbose) {
                    System.err.println("Two networks have the same MAAF, but different extended MAAFs:");
                    System.err.println("Networks:");
                    System.err.println(maaf2network.get(maaf).toStringNetworkFull());
                    System.err.println(network.toStringNetworkFull());
                    System.err.println("ex-MAAFs:");
                    System.err.println(maaf2extendedMaaf.get(maaf).toStringNetworkFull());
                    System.err.println(computeMAAF(network, true).toStringNetworkFull());
                }
                count++;
            }
        }
        return count;
    }

    /**
     * computes all maafs as trees
     *
     * @return MAAFs  (each as a tree in which an auxiliary root is connected to all subtrees)
     */
    public static Collection<Root> computeAllMAAFs(Set<Root> networks) {
		LinkedList<Root> input = new LinkedList<Root>(networks);

        LinkedList<Root> maafs = new LinkedList<Root>();

        for (Root network : input) {
            Root MAAF = computeMAAF(network, false);
            maafs.add(MAAF);
        }
        return maafs;
    }


    /**
     * computes the MAAF for the given network. Note that all components of the MAAF are attached below an auxiliary root node
     *
     * @param extendedMAAF if true, compute extended MAAF in which all unlabeled nodes of outdegree 0 are kept
     * @return MAAF
     */
    public static Root computeMAAF(Root network, boolean extendedMAAF) {
        Graph graph = network.copySubNetwork().getOwner(); // copy of original network

        // remove all reticulate edges:
        LinkedList<Edge> edgesToDelete = new LinkedList<Edge>();
        for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
            if (e.getTarget().getInDegree() > 1)
                edgesToDelete.add(e);
        }
        for (Edge e : edgesToDelete) {
            graph.deleteEdge(e);
        }

        // remove all divertices:
        LinkedList<Node> toDelete = new LinkedList<Node>();
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            if (v.getInDegree() == 1 && v.getOutDegree() == 1)
                toDelete.add(v);
        }
        for (Node v : toDelete) {
            Node u = v.getFirstInEdge().getSource();
            Node w = v.getFirstOutEdge().getTarget();
            graph.newEdge(u, w);
            graph.deleteNode(v);
        }

        // remove all unlabeled nodes of in-degree 1
        // also remove all unlabeled nodes of out-degree 1, unless we want to compute an extended MAAF
        do {
            toDelete.clear();
            for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
                if ((v.getInDegree() == 0 && v.getOutDegree() == 1) || !extendedMAAF && (v.getOutDegree() == 0 && ((Root) v).getTaxa().cardinality() != 1))
                    toDelete.add(v);
            }
            for (Node v : toDelete) {
                graph.deleteNode(v);
            }

        }
        while (toDelete.size() > 0);

        // remove all divertices:
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            if (v.getInDegree() == 1 && v.getOutDegree() == 1)
                toDelete.add(v);
        }
        for (Node v : toDelete) {
            Node u = v.getFirstInEdge().getSource();
            Node w = v.getFirstOutEdge().getTarget();
            graph.newEdge(u, w);
            graph.deleteNode(v);
        }


        // make a new root and attach all components to the root:
        Root aNode = (Root) graph.getFirstNode();
        if (aNode != null) {
            Root maaf = aNode.newNode();
            for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
                if (v != maaf && v.getInDegree() == 0) {
                    maaf.newEdge(maaf, v);
                    maaf.getTaxa().or(((Root) v).getTaxa());
                }
            }
            maaf.reorderNetwork();
            return maaf;
        } else {
            // this should never happen!
            return null;
        }
    }
}
