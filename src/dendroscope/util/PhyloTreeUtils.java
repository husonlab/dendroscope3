/*
 * PhyloTreeUtils.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.util;

import jloda.graph.*;
import jloda.graph.algorithms.Dijkstra;
import jloda.phylo.PhyloTree;
import jloda.swing.graphview.PhyloGraphView;

import java.util.*;

/**
 * phylo tree utilities
 * Daniel Huson, Celine Scornavacca 2.2008
 */


//todo: can we delete extractInducedSubtree? Is it the same as extractInducedSubnetwork???

public class PhyloTreeUtils {
    /**
     * extract the subtree induced by a selection of nodes.
     *
     * @return induced subtree   or null, if not possible
     */
    public static PhyloTree extractInducedSubtree(PhyloTree tree, NodeSet selected, NodeSet collapsed, boolean reduce) {
        if (selected.size() == 0)
            return tree;
        var node2NumberInducedChildren = new NodeIntArray(tree);
        findInducedSubnetworkRec(tree.getRoot(), node2NumberInducedChildren, new NodeSet(tree), collapsed, selected);
        Node subtreeRoot = null;
        if (reduce)
            subtreeRoot = findSubtreeNetworkRec(tree, tree.getRoot(), node2NumberInducedChildren, selected);
        if (subtreeRoot == null || subtreeRoot.getOutDegree() == 0 || (collapsed != null && collapsed.contains(subtreeRoot)))
            subtreeRoot = tree.getRoot();
        var newTree = new PhyloTree();
        newTree.setRoot(newTree.newNode());
        newTree.setLabel(newTree.getRoot(), tree.getLabel(subtreeRoot));
        createInducedSubnetworkRec(subtreeRoot, newTree.getRoot(), tree, new NodeSet(tree), newTree,
                tree.newNodeArray(), node2NumberInducedChildren);
        if (reduce) {
            var divertices = new LinkedList<Node>();
            for (Node v = newTree.getFirstNode(); v != null; v = v.getNext()) {
                if (v.getInDegree() == 1 && v.getOutDegree() == 1)
                    divertices.add(v);
            }
            for (var v : divertices) {
                newTree.delDivertex(v);
            }
        }
        for (Edge e : newTree.edges()) {
            if (e.getTarget().getInDegree() > 1)
                newTree.setReticulate(e, true);
        }
        return newTree;
    }

    /**
     * extract the subnetwork induced by a selection of nodes.
     *
     * @return induced subnetwork   or null, if not possible
     */
    public static PhyloTree extractInducedSubnetwork(PhyloTree network, NodeSet selected, NodeSet collapsed, boolean reduce) {
        if (selected.size() == 0)
            return network;

        var node2NumberInducedChildren = network.newNodeIntArray();
        findInducedSubnetworkRec(network.getRoot(), node2NumberInducedChildren, network.newNodeSet(), collapsed, selected);

        Node subtreeRoot;
        if (network.getNumberReticulateEdges() == 0) { //tree
            subtreeRoot = findSubtreeNetworkRec(network, network.getRoot(), node2NumberInducedChildren, selected);
        } else {
            var LSA = new LCA_LSACalculation(network, true);
            subtreeRoot = LSA.getLca(selected, true);
        }

        if (subtreeRoot == null || subtreeRoot.getOutDegree() == 0 || (collapsed != null && collapsed.contains(subtreeRoot)))
            subtreeRoot = network.getRoot();

        var newTree = new PhyloTree();
        newTree.setRoot(newTree.newNode());
        newTree.setLabel(newTree.getRoot(), network.getLabel(subtreeRoot));
        createInducedSubnetworkRec(subtreeRoot, newTree.getRoot(), network, new NodeSet(network), newTree,
                network.newNodeArray(), node2NumberInducedChildren);
        if (reduce) {
            var divertices = new LinkedList<Node>();
            for (Node v : newTree.nodes()) {
                if (v.getInDegree() == 1 && v.getOutDegree() == 1)
                    divertices.add(v);
            }
            for (var v : divertices) {
                newTree.delDivertex(v);
            }
        }
        for (var e : newTree.edges()) {
            if (e.getTarget().getInDegree() > 1)
				newTree.setReticulate(e, true);
        }
        return newTree;

    }

    /**
     * extract the subnetwork induced by a selection of nodes.
     *
     * @return induced subnetwork   or null, if not possible
     */
    public static PhyloTree extractLSAInducedSubNetwork(PhyloTree network, NodeSet selected, NodeSet collapsed, boolean reduce) {
        //System.err.println("size sel" + selected.size());
        if (selected.size() == 0)
            return network;

        var node2NumberInducedChildren = new NodeIntArray(network);
        findInducedSubnetworkRec(network.getRoot(), node2NumberInducedChildren, new NodeSet(network), collapsed, selected);

        Node subtreeRoot;
        if (network.getNumberReticulateEdges() == 0) { //tree
            subtreeRoot = findSubtreeNetworkRec(network, network.getRoot(), node2NumberInducedChildren, selected);
        } else {
            LCA_LSACalculation LSA = new LCA_LSACalculation(network, true);
            subtreeRoot = LSA.getLca(selected, true);

        }

        if (subtreeRoot == null || subtreeRoot.getOutDegree() == 0 || (collapsed != null && collapsed.contains(subtreeRoot)))
            subtreeRoot = network.getRoot();

        return getSubnetwork(network, subtreeRoot);
    }

    /**
     * extract the subnetwork induced by a selection of nodes.
     *
     * @return induced subnetwork   or null, if not possible
     */
    public static PhyloTree extractSubNetwork(PhyloTree network, NodeSet selected, NodeSet collapsed, boolean reduce) {
        if (selected.size() == 0)
            return network;
        return getSubnetwork(network, selected.getFirstElement());
    }


    /**
     * selects the subnetwork rooted at the LSA of the given selection of nodes
     *
	 */
    public static Node selectLSAInducedSubNetwork(PhyloGraphView viewer, PhyloTree network, NodeSet selected, NodeSet collapsed) {
        var node2NumberInducedChildren = new NodeIntArray(network);

        Node subtreeRoot;
        if (network.getNumberReticulateEdges() == 0) { //tree
            findInducedSubnetworkRec(network.getRoot(), node2NumberInducedChildren, new NodeSet(network), collapsed, selected);
            subtreeRoot = findSubtreeNetworkRec(network, network.getRoot(), node2NumberInducedChildren, selected);
        } else {
            var LSA = new LCA_LSACalculation(network, true);
            subtreeRoot = LSA.getLca(selected, true);
        }

        if (subtreeRoot == null || subtreeRoot.getOutDegree() == 0 || (collapsed != null && collapsed.contains(subtreeRoot)))
            subtreeRoot = network.getRoot();

        return subtreeRoot;

    }

    /**
     * selects the subnetwork induced by the given selection of nodes
     *
	 */
    public static void selectInducedSubNetwork(PhyloGraphView viewer, PhyloTree network, NodeSet selected, NodeSet collapsed) {
        var node2NumberInducedChildren = new NodeIntArray(network);
        findInducedSubnetworkRec(network.getRoot(), node2NumberInducedChildren, new NodeSet(network), collapsed, selected);

        Node subtreeRoot;
        if (network.getNumberReticulateEdges() == 0) { //tree
            subtreeRoot = findSubtreeNetworkRec(network, network.getRoot(), node2NumberInducedChildren, selected);
        } else {
            var LSA = new LCA_LSACalculation(network, true);
            subtreeRoot = LSA.getLca(selected, true);
        }

        if (subtreeRoot == null || subtreeRoot.getOutDegree() == 0 || (collapsed != null && collapsed.contains(subtreeRoot)))
            subtreeRoot = network.getRoot();

        selectInducedSubnetworkRec(viewer, subtreeRoot, network, new NodeSet(network), node2NumberInducedChildren);
        viewer.setSelected(subtreeRoot, true);
    }


    /**
     * find the subnetwork induced by a set of selected nodes
     *
	 */
    private static void findInducedSubnetworkRec(Node v, NodeIntArray node2NumberInducedChildren, NodeSet visited, NodeSet collapsed, NodeSet selected) {
        int count = 0;
        if (collapsed == null || !collapsed.contains(v)) {
            for (var w : v.children()) {
                if (!visited.contains(w)) {
                    visited.add(w);
                    findInducedSubnetworkRec(w, node2NumberInducedChildren, visited, collapsed, selected);
                }
                if (node2NumberInducedChildren.getInt(w) != 0)
                    count++;
            }
        }
        if (count != 0)
            node2NumberInducedChildren.set(v, count);
        else if (selected.contains(v))
            node2NumberInducedChildren.set(v, -1); // is selected
    }

    /**
     * find root of induced subtree. this is first node that has two or more induced children
     *
     * @return root
     */
    private static Node findSubtreeNetworkRec(PhyloTree tree, Node v, NodeIntArray node2NumberInducedChildren, NodeSet selected) {
        if (node2NumberInducedChildren.getInt(v) > 1 || selected.contains(v))
            return v;
        for (var f : v.outEdges()) {
            if (tree.okToDescendDownThisEdgeInTraversal(f, v)) {
                var w = f.getTarget();
                if (node2NumberInducedChildren.getInt(w) != 0) {
                    var u = findSubtreeNetworkRec(tree, w, node2NumberInducedChildren, selected);
                    if (u != null)
                        return u;
                }
            }
        }
        return null;
    }

    /**
     * select the induced subtree
     *
	 */
    private static void selectInducedSubnetworkRec(PhyloGraphView viewer, Node v, PhyloTree tree, NodeSet visited, NodeIntArray node2NumberInducedChildren) {
        for (var f : v.outEdges()) {
            var w = f.getTarget();
            if (node2NumberInducedChildren.getInt(w) != 0) {
                viewer.setSelected(w, true);
                viewer.setSelected(f, true);
                if (!visited.contains(w)) {
                    visited.add(w);
                    selectInducedSubnetworkRec(viewer, w, tree, visited, node2NumberInducedChildren);
                }
            }
        }
    }


    /**
     * create the induced subtree
     *
	 */
    private static void createInducedSubnetworkRec(Node v, Node newV, PhyloTree tree, NodeSet visited, PhyloTree newTree, NodeArray<Node> old2new,
                                                   NodeIntArray node2NumberInducedChildren) {
        for (var f : v.outEdges()) {
            var w = f.getTarget();
            if (node2NumberInducedChildren.getInt(w) != 0) {
                Node newW;
                if (old2new.get(w) != null)
                    newW = old2new.get(w);
                else {
                    newW = newTree.newNode();
                    old2new.put(w, newW);
                }

                if (tree.getLabel(w) != null && tree.getLabel(w).length() > 0)
                    newTree.setLabel(newW, tree.getLabel(w));
                var newF = newTree.newEdge(newV, newW);
                if (tree.getLabel(f) != null && tree.getLabel(f).length() > 0)
                    newTree.setLabel(newF, tree.getLabel(f));
                newTree.setWeight(newF, tree.getWeight(f));
                if (!visited.contains(w)) {
                    visited.add(w);
                    createInducedSubnetworkRec(w, newW, tree, visited, newTree, old2new, node2NumberInducedChildren);
                }
            }
        }
    }

    /**
     * calculates the subnetwork T(v).
     */

    public static PhyloTree getSubnetwork(PhyloTree srcTree, Node v) {
        var newTree = new PhyloTree();
        var newNode = newTree.newNode();
        newTree.setRoot(newNode);
        var visited = new LinkedList<Node>();
        var visitedNew = new LinkedList<Node>();
        getSubnetworkRec(srcTree, newTree, v, newNode, visited, visitedNew);
        return newTree;
    }

    /**
     * recursively does the work
     *
	 */
    private static void getSubnetworkRec(PhyloTree oldTree, PhyloTree newTree, Node vOld, Node vNew, List<Node> visited, List<Node> visitedNew) {
        newTree.setLabel(vNew, oldTree.getLabel(vOld));
        newTree.setInfo(vNew, oldTree.getInfo(vOld));
        for (Edge eOld : vOld.outEdges()) {
            var wOld = eOld.getTarget();
            Node wNew = null;
            var isVisited = false;

			if (oldTree.isReticulateEdge(eOld)) {
				isVisited = visited.contains(wOld);
				if (isVisited) {
					for (var temp : visitedNew) {
						if (newTree.getInfo(temp).equals(wOld.getId()))
							wNew = temp;
					}
				} else {
					wNew = newTree.newNode();
					visited.add(wOld);
					if (oldTree.getInfo(wOld) == null) {
                        newTree.setInfo(wNew, wOld.getId());
                        oldTree.setInfo(wOld, wOld.getId());
					} else {
						newTree.setInfo(wNew, oldTree.getInfo(wOld));
					}
					visitedNew.add(wNew);
				}
			} else {
				wNew = newTree.newNode();
			}

			var eNew = newTree.newEdge(vNew, wNew);
			if (oldTree.isReticulateEdge(eOld)) {
				newTree.setReticulate(eNew, true);
				newTree.setWeight(eNew, 0);
			}

			newTree.setLabel(eNew, oldTree.getLabel(eOld));
			newTree.setInfo(eNew, oldTree.getInfo(eOld));

			if (!isVisited)
				getSubnetworkRec(oldTree, newTree, wOld, wNew, visited, visitedNew);
		}
    }

    /**
     * collects all clusters contained in the tree.
     */
    public static Set<Set<String>> collectAllHardwiredClusters(PhyloTree tree) {
        var clusters = new HashSet<Set<String>>();
        collectAllHardwiredClustersRec(tree, tree.getRoot(), clusters);
        return clusters;
    }

    public static Set<String> collectAllHardwiredClustersRec(PhyloTree tree, Node v, Set<Set<String>> clusters) {
        //reached a leave
        if (v.getOutDegree() == 0) {
            var set = new HashSet<String>();
            set.add(tree.getLabel(v));
            clusters.add(set);
            return set;
        }
        //recursion
        else {
            TreeSet<String> set = new TreeSet<String>();
            for (Edge f : v.outEdges()) {
                var w = f.getTarget();
                set.addAll(collectAllHardwiredClustersRec(tree, w, clusters));
            }
            clusters.add(set);
            return set;
        }
    }


    /**
     * compute the number of nodes present in the shortest path for each pair of leaves, considering the graph undirected
     *
	 */
    public static void computeNumberNodesInTheShortestPath(final PhyloTree graph, Map<String, Integer> taxon2ID, double[][] distMatrix) {
        var leaves = graph.computeSetOfLeaves();
        var distMatrixTemp = new int[graph.getNumberOfNodes()][graph.getNumberOfNodes()];   // temp matrix

        var idTemp = new NodeIntArray(graph);   //temp id used to fill distMatrixTemp
        var nN = 0;

        for (var tempNode : graph.nodes()) {
            idTemp.set(tempNode, nN);     //set temp id
            nN++;

        }

            /* Find the shortest path between source and and all other nodes of the DiRECTED PN.
            The values are saved in distMatrixTemp, dist is used to order the priorityQueue.
            */

        for (var source : graph.nodes()) {
            var idS = idTemp.getInt(source);
            //System.out.println("idS" + idS);

            //NodeArray predecessor = new NodeArray(graph);
            var dist = graph.newNodeDoubleArray();

            for (var v : graph.nodes()) {
                dist.put(v, 10000.0);
                var idV = idTemp.getInt(v);
                distMatrixTemp[idS][idV] = 10000;
                //predecessor.set(v, null);
            }
                dist.put(source, 0.0);
                distMatrixTemp[idS][idS] = 0;

            var priorityQueue = Dijkstra.newFullQueue(graph, dist);

                while (priorityQueue.isEmpty() == false) {
                    var size = priorityQueue.size();
                    var u = priorityQueue.first();

                    priorityQueue.remove(u);

                    if (priorityQueue.size() != size - 1)
                        throw new RuntimeException("remove u=" + u + " failed: size=" + size);

                    var idU = idTemp.getInt(u);
                    for (Edge e : u.outEdges()) {
                        var v = graph.getOpposite(u, e);
                        var idV = idTemp.getInt(v);
                        if (distMatrixTemp[idS][idV] > distMatrixTemp[idS][idU] + 1) {
                            //System.out.println("enter");
                            // priorty of v changes, so must re-and to queue:
                            priorityQueue.remove(v);
                            distMatrixTemp[idS][idV] = distMatrixTemp[idS][idU] + 1;
                            dist.put(v, distMatrixTemp[idS][idU] + 1.0);
                            priorityQueue.add(v);
                            //predecessor.set(v, u);
                        }
                    }
                }
        }


            /* Find the shortest path between each pair of leaves (l1,l2) as the shortest
            dist(l1,intNode)+ dist(l2,intNode), where intNode is an internal node.
            The values are saved in distMatrix.
            */

        for (var source : leaves) {
            var idS = idTemp.getInt(source);
            int labelS = taxon2ID.get(graph.getLabel(source)); // Franzi uses values from 1 to n, me from 0 to n-1
            leaves.remove(source);
            for (Node target : leaves) {
                if (source != target) {
                    var idT = idTemp.getInt(target);
                    var labelT = taxon2ID.get(graph.getLabel(target)); // Same as above

                    //System.out.println("labelS " + labelS+ " labelT " + labelT);
                    var min = 1000000;
                    for (var node : graph.nodes()) {
                        var nodeID = idTemp.getInt(node);
                        if (distMatrixTemp[nodeID][idS] + distMatrixTemp[nodeID][idT] < min)
                            min = distMatrixTemp[nodeID][idS] + distMatrixTemp[nodeID][idT];
                    }

                    distMatrix[labelS][labelT] += min;   //update distMatrix
                    distMatrix[labelT][labelS] += min;
                }
                }
            }
    }

}

