/**
 * LayoutOptimizer2008.java 
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
package dendroscope.embed;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloSplitsGraph;
import jloda.phylo.PhyloTree;
import jloda.util.ProgressListener;

import java.util.*;

/**
 * compute optimal ordering for drawing a reticulate network. THis is the algorithm described in
 * the 2008 BMC Evol Bio paper
 * Tobias Kloepper, 2007
 */
public class LayoutOptimizer2008 implements ILayoutOptimizer {

    private static final boolean debug = false;

    /**
     * computes an optimal embedding using tobias' algorithm
     *
     * @param tree
     * @param progressListener
     * @throws Exception
     */
    public void apply(PhyloTree tree, ProgressListener progressListener) {
        if (tree.getRoot() == null || tree.getSpecialEdges().size() == 0) {
            tree.getNode2GuideTreeChildren().clear();
            return;
        }
        System.err.println("Computing optimal embedding using (Kloepper and Huson, 2008)");

        Map<Node, List<Node>> nodes2Orderings = new HashMap<Node, List<Node>>();
        Map rNode2ReticulationNodeData = new HashMap();
        // find root
        Node root = tree.getRoot();
        if (root == null)
            return;

        if (debug) {
            Iterator it2 = tree.nodeIterator();
            while (it2.hasNext()) {
                Node n = (Node) it2.next();
                System.out.print("Node: " + n + "label: " + tree.getLabel(n) + "\tdecendants: ");
                Iterator it3 = n.adjacentNodes().iterator();
                while (it3.hasNext()) {
                    Node d = (Node) it3.next();
                    if (n.getCommonEdge(d).getSource().equals(n)) System.out.print(d + "\t");
                }
                System.out.println();
            }
        }
        // build a dependency graph for the reticulations
        // maps the nodes of the dependency Graph back to the reticulation nodes
        buildReticulationDependency(tree, rNode2ReticulationNodeData);
        // each map has a node as key and a BitSet of the size of taxa as a value
        if (debug) System.out.println("\nstart bottom up:");
        if (debug) System.out.println("\nstart find auxiliary edges:");
        // for the auxiliaryEdges we need to know which is the shortrest reticulation cycle for a reticulation and
        // which node in the cycle is closest to the root. The key is the reticulation edge and the value are two edges.
        // These edges are the startpoints for the path from source of auxiliary edge to p bzw. q.
        createAuxiliaryEdges(tree, root, rNode2ReticulationNodeData);
        // reverse map: each key is a node of the graph and contains as value the set of rNodes for which the auxiliary node connects to.
        Map<Node, Set<Node>> parent2rNodes = new HashMap<Node, Set<Node>>();
        Iterator it = rNode2ReticulationNodeData.keySet().iterator();
        while (it.hasNext()) {
            Node rNode = (Node) it.next();
            ReticulationNodeData rNodeData = (ReticulationNodeData) rNode2ReticulationNodeData.get(rNode);
            Node parent = rNodeData.getParent();
            if (parent2rNodes.get(parent) == null) parent2rNodes.put(parent, new TreeSet<Node>(new NodeComparator()));
            parent2rNodes.get(parent).add(rNode);
        }
        if (debug) System.out.println("\nstart mapping active RNodes:");
        // maps those rNodes to a nodes, that have the node in their path to the GMRCA ( either through p or q)
        Map nodes2rNodes = getActiveRNodesForInEdges(tree, rNode2ReticulationNodeData);
        // start recursion
        if (debug) System.out.println("\nstart top down:");
        recTopDownLabelNodes(root, new HashSet(), parent2rNodes, nodes2rNodes, rNode2ReticulationNodeData, nodes2Orderings);
        if (debug) {
            it = tree.nodeIterator();
            while (it.hasNext()) {
                Node n = (Node) it.next();
                System.out.println("Node: " + n + "\tordered decendants: " + nodes2Orderings.get(n));
            }
        }

        // copy result:
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            tree.getNode2GuideTreeChildren().put(v, nodes2Orderings.get(v));
        }
    }

    /**
     * calculates the set of active reticulations for the inEdges of all nodes.
     *
     * @param graph
     * @param rNode2ReticulationNodeData
     * @return contains as keys nodes and as values Maps which contain the reticulations for which the inEdge is contained
     * in the tree cycle of the reticulations
     */
    private Map getActiveRNodesForInEdges(PhyloSplitsGraph graph, Map rNode2ReticulationNodeData) {
        Map nodes = new HashMap();
        Iterator it = graph.nodeIterator();
        while (it.hasNext()) nodes.put(it.next(), new HashSet());
        it = rNode2ReticulationNodeData.keySet().iterator();
        while (it.hasNext()) {
            Node rNode = (Node) it.next();
            if (debug)
                System.out.println("rNode: " + rNode);
            ReticulationNodeData rNodeData = (ReticulationNodeData) rNode2ReticulationNodeData.get(rNode);
            // find pathes from p and q to parent
            if (!rNodeData.getP().getSource().equals(rNodeData.getParent())) {
                LinkedList path2p = findMinPath2Ancestor(rNodeData.getP().getSource(), rNodeData.getParent(), rNode2ReticulationNodeData);
                rNodeData.setNode2p((Node) path2p.getFirst());
                rNodeData.setPathParent2P(path2p);
                Iterator itPath = path2p.iterator();
                if (debug)
                    System.out.println("found Path between: " + rNodeData.getParent() + "\tand " + rNodeData.getP().getSource() + "\t: " + path2p);
                while (itPath.hasNext()) {
                    Node decendant = (Node) itPath.next();
                    ((HashSet) nodes.get(decendant)).add(rNode);
                }
            } else {
                if (debug)
                    System.out.println("found Path between: " + rNodeData.getParent() + "\tand " + rNodeData.getP().getSource() + "\t which is empty.");
                rNodeData.setNode2p(rNodeData.getParent());
            }
            if (!rNodeData.getQ().getSource().equals(rNodeData.getParent())) {
                LinkedList path2q = findMinPath2Ancestor(rNodeData.getQ().getSource(), rNodeData.getParent(), rNode2ReticulationNodeData);
                rNodeData.setNode2q((Node) path2q.getFirst());
                rNodeData.setPathParent2Q(path2q);
                Iterator itPath = path2q.iterator();
                if (debug)
                    System.out.println("found Path between: " + rNodeData.getParent() + "\tand " + rNodeData.getQ().getSource() + "\t: " + path2q);
                while (itPath.hasNext()) {
                    Node decendant = (Node) itPath.next();
                    ((HashSet) nodes.get(decendant)).add(rNode);
                }
            } else {
                if (debug)
                    System.out.println("found Path between: " + rNodeData.getParent() + "\tand " + rNodeData.getP().getSource() + "\t which is empty.");
                rNodeData.setNode2q(rNodeData.getParent());
            }
        }
        return nodes;
    }

    /**
     * creates the set of auxiliary edges, the information is stored in the ReticulationNodeData of each reticulation.
     *
     * @param graph
     * @param root
     * @param rNode2ReticulationNodeData
     * @throws Exception
     */
    private void createAuxiliaryEdges(PhyloSplitsGraph graph, Node root, Map rNode2ReticulationNodeData) {
        Map node2parent = new HashMap();
        Iterator it = graph.nodeIterator();
        // find first common anceestor
        while (it.hasNext()) {
            Node n = (Node) it.next();
            if (n.getInDegree() == 2) {
                HashSet pathes = new HashSet();
                recFindPathesInOrgGraph2Ancestor(n, root, new LinkedList(), pathes, rNode2ReticulationNodeData);
                // take the first path and iterate through it
                LinkedList startPath = (LinkedList) pathes.iterator().next();
                pathes.remove(startPath);
                Iterator itPath = startPath.iterator();
                // first node is reticulation Node
                Node parent = (Node) itPath.next();
                if (itPath.hasNext())
                    parent = (Node) itPath.next();
                else
                    System.out.println("path too short!"); // should contain at least one more node
                boolean foundParent = false;
                // find the first node in the start path that is contained in all other pathes
                while (!foundParent && parent != null) {
                    Iterator itPathes = pathes.iterator();
                    boolean cont = true;
                    while (cont && itPathes.hasNext()) {
                        if (!((LinkedList) itPathes.next()).contains(parent)) {
                            cont = false;
                        }
                    }
                    if (cont) {
                        foundParent = true;
                        node2parent.put(n, parent);
                    } else
                        parent = (Node) itPath.next();
                }
                if (!foundParent) System.out.println("unable to find Parent for node: " + n);
            }
        }
        it = node2parent.keySet().iterator();
        while (it.hasNext()) {
            Node rNode = (Node) it.next();
            Node parent = (Node) node2parent.get(rNode);
            Edge p = null;
            Edge q = null;
            Iterator it2 = rNode.inEdges().iterator();
            int count = 0;
            while (it2.hasNext()) {
                Edge e = (Edge) it2.next();
                if (p == null)
                    p = e;
                else
                    q = e;
                count++;
            }

            ReticulationNodeData rNodeData = (ReticulationNodeData) rNode2ReticulationNodeData.get(rNode);
            rNodeData.setParent(parent);
            rNodeData.setP(p);
            rNodeData.setQ(q);
        }
    }


    /**
     * @param start
     * @param before
     * @param parent2rNodes
     * @param node2ActiverNodes
     * @param rNode2ReticulationNodeData
     * @throws Exception
     */
    private void recTopDownLabelNodes(Node start, HashSet before, Map parent2rNodes, Map node2ActiverNodes, Map rNode2ReticulationNodeData, Map nodes2Orderings) {
        ArrayList orderedRNodes = new ArrayList();
        if (parent2rNodes.get(start) != null) {
            orderedRNodes = buildLocalWorkFlow((Set) parent2rNodes.get(start), rNode2ReticulationNodeData);
        }
        ArrayList ordList = new ArrayList();
        // if there are no RNodes do not change the ordering
        if (debug)
            System.out.println("orderedRNodes.size() = " + orderedRNodes.size() + "\tactiveRNodes: " + node2ActiverNodes.get(start));
        if (orderedRNodes.size() == 0 && node2ActiverNodes.get(start) == null) {
            Iterator itN = start.adjacentNodes().iterator();
            while (itN.hasNext()) {
                Node n = (Node) itN.next();
                if (n.getInDegree() == 1 && n.getCommonEdge(start).getSource().equals(start))
                    ordList.add(n);
            }
        } else {
            // the list of tree decendants of start
            TreeSet tmp = new TreeSet(new NodeComparator());
            Iterator itN = start.adjacentNodes().iterator();
            while (itN.hasNext()) {
                Node n = (Node) itN.next();
                if (n.getInDegree() == 1 && n.getCommonEdge(start).getSource().equals(start))
                    tmp.add(n);
            }
            ArrayList decendants = new ArrayList();
            decendants.addAll(orderedRNodes);
            decendants.addAll(tmp);
            // gives the distances for the scoring function of the ordering we make this once to save time
            // first are the decendants in order of the ArrayList decendants and last two entries are before and after.
            int[][] nodes2nConections = makeDistances4Edges(start, before, orderedRNodes, decendants, node2ActiverNodes, rNode2ReticulationNodeData);
            // place all reticulation nodes in the order given
            buildOrdListGreedy(orderedRNodes, nodes2nConections, decendants, ordList, rNode2ReticulationNodeData);
        }
        nodes2Orderings.put(start, ordList);
        // recursive downward
        Iterator it = ordList.iterator();
        while (it.hasNext()) {
            Node next = (Node) it.next();
            recTopDownLabelNodes(next, before, parent2rNodes, node2ActiverNodes, rNode2ReticulationNodeData, nodes2Orderings);
            before.add(next);// next has been worked on.
        }

    }


    /**
     * @param orderedRNodes
     * @param nodes2nConections
     * @param decendants
     * @param ordList
     * @param rNode2ReticulationNodeData
     * @return
     */
    private int buildOrdListGreedy(ArrayList orderedRNodes, int[][] nodes2nConections, ArrayList decendants, ArrayList ordList, Map rNode2ReticulationNodeData) {
        Iterator it = orderedRNodes.iterator();
        // crossings is the last number we got as crossing score so it is the overall score we return it for the greedy approach
        // (it is returned because we could use it for a BnB approach)
        int crossings = 0;
        while (it.hasNext()) {
            Node rNode = (Node) it.next();
            ReticulationNodeData rNodeData = (ReticulationNodeData) rNode2ReticulationNodeData.get(rNode);
            Node node2Pathp = rNodeData.getNode2p();
            Node node2Pathq = rNodeData.getNode2q();
            if (debug)
                System.out.println("\nnode: " + rNode + "\tnode2Pathp: " + node2Pathp + "\tnode2Pathq: " + node2Pathq);
            // add p and q to the ordering
            if (!node2Pathp.equals(rNodeData.getParent()) && ordList.indexOf(node2Pathp) == -1) {
                if (debug) System.out.println("adding node: " + node2Pathp);
                int minPosition = 0;
                int minScore = Integer.MAX_VALUE;
                for (int i = 0; i <= ordList.size() && minScore > 0; i++) {
                    ordList.add(i, node2Pathp);
                    int score = getCrossingScore(nodes2nConections, node2Pathp, decendants, ordList, i, rNode2ReticulationNodeData);
                    if (minScore > score) {
                        minScore = score;
                        minPosition = i;
                        crossings = minScore;
                    }
                    ordList.remove(i);
                }
                ordList.add(minPosition, node2Pathp);
                if (debug)
                    System.out.println("minScore: " + minScore + "\tminPosition: " + minPosition + "\tordList: " + ordList);
            }
            if (!node2Pathq.equals(rNodeData.getParent()) && ordList.indexOf(node2Pathq) == -1) {
                if (debug) System.out.println("adding  node: " + node2Pathq);
                int minPosition = 0;
                int minScore = Integer.MAX_VALUE;
                for (int i = 0; i <= ordList.size() && minScore > 0; i++) {
                    ordList.add(i, node2Pathq);
                    int score = getCrossingScore(nodes2nConections, node2Pathq, decendants, ordList, i, rNode2ReticulationNodeData);
                    if (minScore > score) {
                        minScore = score;
                        minPosition = i;
                        crossings = minScore;
                    }
                    ordList.remove(i);
                }
                ordList.add(minPosition, node2Pathq);
                if (debug)
                    System.out.println("minScore: " + minScore + "\tminPosition: " + minPosition + "\tordList: " + ordList);


            }
            if (!ordList.contains(rNode)) {
                // add auxiliary edge  between p and q !
                int startIndex = ordList.indexOf(node2Pathp);
                int stopIndex = ordList.indexOf(node2Pathq);
                if (startIndex > stopIndex) {
                    int tmp = startIndex;
                    startIndex = stopIndex;
                    stopIndex = tmp;
                }
                int minPosition = 0;
                int minScore = Integer.MAX_VALUE;
                if (debug)
                    System.out.println("adding edge: rNode " + rNode + "\t between: " + (startIndex + 1) + "\t" + stopIndex);
                for (int i = startIndex + 1; i <= stopIndex && minScore > 0; i++) {
                    ordList.add(i, rNode);
                    int score = getCrossingScore(nodes2nConections, rNode, decendants, ordList, i, rNode2ReticulationNodeData);
                    if (minScore > score) {
                        minScore = score;
                        minPosition = i;
                        crossings = minScore;
                    }
                    ordList.remove(i);
                }
                ordList.add(minPosition, rNode);
                if (debug)
                    System.out.println("minScore: " + minScore + "\tminPosition: " + minPosition + "\tordList: " + ordList);
            }

        }
        // finally add al those edges that are "real" tree edges with no reticulation depending on it
        it = decendants.iterator();
        while (it.hasNext()) {
            Node n = (Node) it.next();
            if (debug) System.out.println("n: " + n + "\tordList: " + ordList);
            if (ordList.indexOf(n) == -1 && n.getInDegree() == 1) {
                int minPosition = 0;
                int minScore = Integer.MAX_VALUE;
                if (debug) System.out.println("adding edge: Node " + n);
                for (int i = 0; i <= ordList.size() && minScore > 0; i++) {
                    ordList.add(i, n);
                    int score = getCrossingScore(nodes2nConections, n, decendants, ordList, i, rNode2ReticulationNodeData);
                    if (minScore > score) {
                        minScore = score;
                        minPosition = i;
                        crossings = minScore;
                    }
                    ordList.remove(i);
                }
                ordList.add(minPosition, n);
                if (debug)
                    System.out.println("minScore: " + minScore + "\tminPosition: " + minPosition + "\tordList: " + ordList);

            }
        }
        return crossings;
    }

    /**
     * calculates the score for the plasement of 'node' given the scoring matrix nodes2nConnections
     *
     * @param nodes2Connections
     * @param node
     * @param decendants
     * @param ordList
     * @param position
     * @param rNode2ReticulationNodeData
     * @return
     */
    private int getCrossingScore(int[][] nodes2Connections, Node node, ArrayList decendants, ArrayList ordList, int position, Map rNode2ReticulationNodeData) {
        int score = 0;
        int indexP = -1;
        int indexQ = -1;
        if (node.getInDegree() == 2) {
            ReticulationNodeData rnd = (ReticulationNodeData) rNode2ReticulationNodeData.get(node);
            indexP = decendants.indexOf(rnd.getNode2p());
            indexQ = decendants.indexOf(rnd.getNode2q());
        }
        // calculate score of the position:
        if (debug) System.out.println("ordList: " + ordList);
        for (int i = 0; i < ordList.size(); i++) {
            Node n1 = (Node) ordList.get(i);
            int index1 = decendants.indexOf(n1);
            for (int j = i + 1; j < ordList.size(); j++) {
                Node n2 = (Node) ordList.get(j);
                int index2 = decendants.indexOf(n2);
                // if (debug)
                //     System.out.println("indexP: " + indexP + "\t indexQ: " + indexQ + "\t index1: " + index1 + "\tindex2: " + index2 + "\tmatrixValue: " + nodes2Connections[index1][index2]);
                score += (j - i - 1) * nodes2Connections[index1][index2];
                // if (debug)
                //     System.out.println("i: " + i + "\tj: " + j + "\tscore: " + score + "\tadded: " + ((j - i - 1) * nodes2Connections[index1][index2]) + "orderedList: " + ordList);
            }
            // adding score for before
            score += (i) * nodes2Connections[index1][nodes2Connections.length - 2];
            // if (debug)
            //     System.out.println("i: " + i + "\tbefore" + "\tscore: " + score + "\tentry in scoring matrix: " + nodes2Connections[index1][nodes2Connections.length - 2] + "orderedList: " + ordList);

            // adding score for after
            score += (ordList.size() - i) * nodes2Connections[index1][nodes2Connections.length - 1];
            // if (debug)
            //     System.out.println("i: " + i + "\tafter" + "\tscore: " + score + "\tentry in scoring matrix: " + nodes2Connections[index1][nodes2Connections.length - 1] + "orderedList: " + ordList);

        }
        // adding score for before
        return score;
    }

    /**
     * creates a scoring matrix which contains as values the weight of the connections between the subnetworks of two nodes. This is calculated one to save time
     *
     * @param start
     * @param before
     * @param orderedRNodes
     * @param decendants
     * @param nodes2ActiverNodes
     * @param rNode2ReticulationNodeData
     * @return
     */
    private int[][] makeDistances4Edges(Node start, HashSet before, ArrayList orderedRNodes, ArrayList decendants, Map nodes2ActiverNodes, Map rNode2ReticulationNodeData) {
        // last two are the scores for before and after
        int[][] distances = new int[decendants.size() + 2][decendants.size() + 2];
        // first work in the orderedRNodes
        Iterator it = orderedRNodes.iterator();
        while (it.hasNext()) {
            Node rNode = (Node) it.next();
            // first add distance for p and q
            ReticulationNodeData rnd = (ReticulationNodeData) rNode2ReticulationNodeData.get(rNode);

            if (debug) System.out.print("p: " + rnd.getNode2p() + "\tindex: " + decendants.indexOf(rnd.getNode2p()) +
                    "\tq: " + rnd.getNode2q() + "\tindex: " + decendants.indexOf(rnd.getNode2q()) +
                    "\trNode: " + rNode + "\tindex: " + decendants.indexOf(rNode));

            if (!rnd.getNode2p().equals(rnd.getParent()))
                distances[decendants.indexOf(rnd.getNode2p())][decendants.indexOf(rNode)]++;
            if (!rnd.getNode2q().equals(rnd.getParent()))
                distances[decendants.indexOf(rnd.getNode2q())][decendants.indexOf(rNode)]++;
        }
        // set the before and after values, Thisi s done by checking for those rNdoes that are active at start and at a decendant
        // if such a node exists it has a connection either to the before or after part of ordering of the parent of start..
        //
        HashSet startActiveRNodes = (HashSet) nodes2ActiverNodes.get(start);
        it = startActiveRNodes.iterator();
        while (it.hasNext()) {
            Node r = (Node) it.next();
            Iterator it2 = decendants.iterator();
            while (it2.hasNext()) {
                Node dec = (Node) it2.next();
                HashSet decActiveRNodes = (HashSet) nodes2ActiverNodes.get(dec);
                // is the node in the active set of the decendant and already placed?
                if (decActiveRNodes.contains(r) && before.contains(r))
                    distances[decendants.indexOf(dec)][distances.length - 2]++;
                else if (decActiveRNodes.contains(r))
                    distances[decendants.indexOf(dec)][distances.length - 1]++;
            }
        }
        return distances;
    }

    /**
     * find the shortest path from a decendant to an ancestor with respect to the auxiliary edges.
     *
     * @param decendant
     * @param ancestor
     * @param rNode2ReticulationNodeData
     * @return
     */
    private LinkedList findMinPath2Ancestor(Node decendant, Node ancestor, Map rNode2ReticulationNodeData) {
        LinkedList path = new LinkedList();
        recFindPath2Ancestor(decendant, ancestor, path, rNode2ReticulationNodeData);
        return path;
    }

    /**
     * rec find the shortest path from a decendant to an ancestor with respect to the auxiliary edges.
     *
     * @param decendant
     * @param ancestor
     * @param path
     * @param rNode2ReticulationNodeData
     */
    private void recFindPath2Ancestor(Node decendant, Node ancestor, LinkedList path, Map rNode2ReticulationNodeData) {
        // check if this is a recombination node
        if (decendant.equals(ancestor))
            return;
        else if (decendant.getInDegree() == 2) {
            path.addFirst(decendant);
            ReticulationNodeData decRData = (ReticulationNodeData) rNode2ReticulationNodeData.get(decendant);
            if (decRData != null && decRData.getParent() != null) {
                recFindPath2Ancestor(decRData.getParent(), ancestor, path, rNode2ReticulationNodeData);
            }
        } else {
            path.addFirst(decendant);
            Node next = decendant.getFirstInEdge().getSource();
            recFindPath2Ancestor(next, ancestor, path, rNode2ReticulationNodeData);
        }
    }

    /**
     * Calculates all pathes from the decendant to the ancestor in a network the result is saved in 'pathes'
     *
     * @param decendant
     * @param ancestor
     * @param visitedNodes
     * @param pathes
     * @param rNode2ReticulationNodeData
     */
    private void recFindPathesInOrgGraph2Ancestor(Node decendant, Node ancestor, LinkedList visitedNodes, HashSet pathes, Map rNode2ReticulationNodeData) {
        Iterator it = decendant.adjacentNodes().iterator();
        visitedNodes.add(decendant);
        while (it.hasNext()) {
            Node next = (Node) it.next();
            if (decendant.getCommonEdge(next).getSource().equals(next)) {
                if (next.equals(ancestor)) {
                    LinkedList newVisitedNodes = (LinkedList) visitedNodes.clone();
                    newVisitedNodes.add(next);
                    pathes.add(newVisitedNodes);
                } else {
                    LinkedList newVisitedNodes = (LinkedList) visitedNodes.clone();
                    recFindPathesInOrgGraph2Ancestor(next, ancestor, newVisitedNodes, pathes, rNode2ReticulationNodeData);
                }
            }
        }
        // check if this is a recombination node
        if (decendant.getInDegree() == 2) {
            ReticulationNodeData decRData = (ReticulationNodeData) rNode2ReticulationNodeData.get(decendant);
            if (decRData != null && decRData.getParent() != null) {
                if (decRData.getParent().equals(ancestor)) {
                    LinkedList newVisitedNodes = (LinkedList) visitedNodes.clone();
                    newVisitedNodes.add(decRData.getParent());
                    pathes.add(newVisitedNodes);
                } else {
                    LinkedList newVisitedNodes = (LinkedList) visitedNodes.clone();
                    recFindPathesInOrgGraph2Ancestor(decRData.getParent(), ancestor, newVisitedNodes, pathes, rNode2ReticulationNodeData);
                }
            }
        }
    }


    /**
     * Gives back a linked List of the reticulation beeing childs of start. the list is ordered in the sequence the nodes must be added to start
     *
     * @param rNodes
     * @param rNode2ReticulationNodeData
     * @return
     */
    private ArrayList buildLocalWorkFlow(Set rNodes, Map rNode2ReticulationNodeData) {
        PhyloSplitsGraph ordGraph = new PhyloSplitsGraph();
        Iterator it = rNodes.iterator();
        Map rNode2ordNode = new HashMap();
        Map ordNode2rNode = new HashMap();
        while (it.hasNext()) {
            Node rNode = (Node) it.next();
            Node ordNode = ordGraph.newNode();
            rNode2ordNode.put(rNode, ordNode);
            ordNode2rNode.put(ordNode, rNode);
        }
        it = rNodes.iterator();
        while (it.hasNext()) {
            Node rNode = (Node) it.next();
            ReticulationNodeData rNodeData = (ReticulationNodeData) rNode2ReticulationNodeData.get(rNode);
            HashSet dependentRNodes = rNodeData.getDependentRNodes();
            Iterator itDep = dependentRNodes.iterator();
            while (itDep.hasNext()) {
                Node depRNode = (Node) itDep.next();
                if (rNodes.contains(depRNode)) {
                    ordGraph.newEdge((Node) rNode2ordNode.get(rNode), (Node) rNode2ordNode.get(depRNode));
                }
            }
        }
        LinkedList sortedOrdNodes = DFS(ordGraph, true, true);
        ArrayList sortedNodes = new ArrayList();
        it = sortedOrdNodes.iterator();
        while (it.hasNext()) sortedNodes.add(ordNode2rNode.get(it.next()));
        return sortedNodes;
    }


    /**
     * Constructs a graph with nodes are indications of reticulation nodes and an edge between two nodes if the movement of one node has an influence on the edge of a
     * reticulation edge of the other. The information is stored in the ReticulationNodeData objects.
     *
     * @param graph
     * @param rNode2ReticulationNodeData
     */
    private void buildReticulationDependency(PhyloSplitsGraph graph, Map rNode2ReticulationNodeData) {
        Map rNode2DepRetNode = new HashMap();
        PhyloSplitsGraph depRet = new PhyloSplitsGraph();
        // init depRet
        Iterator it = graph.nodeIterator();
        while (it.hasNext()) {
            Node retN = (Node) it.next();
            if (retN.getInDegree() == 2) {
                if (rNode2ReticulationNodeData.get(retN) == null)
                    rNode2ReticulationNodeData.put(retN, new ReticulationNodeData(retN));
                Node depN = depRet.newNode();
                rNode2DepRetNode.put(retN, depN);
                depN.setInfo(retN);
            }
        }
        it = rNode2ReticulationNodeData.keySet().iterator();
        while (it.hasNext()) {
            Node retN = (Node) it.next();
            HashSet dependentTreeNodes = ((ReticulationNodeData) rNode2ReticulationNodeData.get(retN)).getDependentTreeNodes();
            HashSet dependentRetNodes = ((ReticulationNodeData) rNode2ReticulationNodeData.get(retN)).getDependentRNodes();
            HashSet seenNodes = new HashSet();
            seenNodes.add(retN);
            // init toWork
            Vector nodes2Work = new Vector();
            Iterator it2 = retN.adjacentNodes().iterator();
            while (it2.hasNext()) {
                Node n = (Node) it2.next();
                if (n.getCommonEdge(retN).getSource().equals(retN))
                    nodes2Work.add(n);
            }
            while (nodes2Work.size() > 0) {
                Node next = (Node) nodes2Work.remove(0);
                if (!seenNodes.contains(next) && next.getInDegree() == 1) {
                    dependentTreeNodes.add(next);
                    it2 = next.adjacentNodes().iterator();
                    while (it2.hasNext()) {
                        Node toAdd = (Node) it2.next();
                        if (next.getCommonEdge(toAdd).getSource().equals(next)) {
                            nodes2Work.add(toAdd);
                        }
                    }
                } else if (!seenNodes.contains(next) && next.getInDegree() == 2) {
                    dependentRetNodes.add(next);
                    depRet.newEdge((Node) rNode2DepRetNode.get(retN), (Node) rNode2DepRetNode.get(next));
                }
                seenNodes.add(next);
            }
        }
    }


    /**
     * Graph stuff *
     */

    private final Integer white = new Integer(0);
    private final Integer gray = new Integer(1);
    private final Integer black = new Integer(2);
    private int time = -1;

    private LinkedList DFS(PhyloSplitsGraph graph, boolean breakCycles, boolean removeForwardEdges) {
        Map node2Color = new HashMap();
        Map node2Predecessor = new HashMap();
        Map node2time = new HashMap();
        LinkedList sortedNodes = new LinkedList();
        Iterator it = graph.nodeIterator();
        while (it.hasNext()) {
            Node n = (Node) it.next();
            node2Color.put(n, white);
            node2Predecessor.put(n, null);
            // first is discovery, second is finishing time
            node2time.put(n, new int[]{-1, -1});
        }
        time = 0;
        LinkedList sorted = new LinkedList();
        it = graph.nodeIterator();
        while (it.hasNext()) {
            Node n = (Node) it.next();
            if (node2Color.get(n).equals(white))
                sorted.addAll(DFSVisit(n, node2time, node2Color, node2Predecessor, breakCycles, removeForwardEdges));
        }
        while (sorted.size() > 0) {
            Node n = (Node) sorted.removeLast();
            int[] nTimes = (int[]) node2time.get(n);
            //System.out.println("Node: " + n + "\ttimestamps: " + nTimes[0] + "/" + nTimes[1]);
            sortedNodes.add(n);
        }
        return sortedNodes;
    }

    private LinkedList DFSVisit(Node u, Map node2time, Map node2Color, Map node2Predecessor, boolean breakCycles, boolean removeForwardEdges) {
        LinkedList sorted = new LinkedList();
        node2Color.put(u, gray);
        int[] uTimes = (int[]) node2time.get(u);
        uTimes[0] = ++time;
        Iterator it = u.adjacentNodes().iterator();
        HashSet edges2Delete = new HashSet();
        while (it.hasNext()) {
            Node v = (Node) it.next();
            if (v.getCommonEdge(u).getSource().equals(u)) {
                if (node2Color.get(v).equals(white)) {
                    node2Predecessor.put(v, u);
                    sorted.addAll(DFSVisit(v, node2time, node2Color, node2Predecessor, breakCycles, removeForwardEdges));
                } else if (node2Color.get(v).equals(gray) && breakCycles) {
                    edges2Delete.add(u.getCommonEdge(v));
                } else if (node2Color.get(v).equals(black) && removeForwardEdges) {
                    int[] vTimes = (int[]) node2time.get(u);
                    if (uTimes[0] < vTimes[0])
                        edges2Delete.add(u.getCommonEdge(v));
                }
            }
        }
        node2Color.put(u, black);
        sorted.addLast(u);
        uTimes[1] = ++time;
        return sorted;
    }

    class NodeComparator implements Comparator<Node> {

        public int compare(Node n1, Node n2) {
            return n1.getId() - n2.getId();
        }
    }

    class ReticulationNodeData {

        Node rNode;
        Node parent;
        Edge p;
        Edge q;
        Node node2p;
        Node node2q;
        LinkedList pathParent2P;
        LinkedList pathParent2Q;

        HashSet dependentTreeNodes;
        HashSet dependentRNodes;


        public ReticulationNodeData(Node rNode, Node parent) {
            this.rNode = rNode;
            this.parent = parent;
            dependentRNodes = new HashSet();
            dependentTreeNodes = new HashSet();
        }

        public ReticulationNodeData(Node rNode) {
            this.rNode = rNode;
            dependentRNodes = new HashSet();
            dependentTreeNodes = new HashSet();
        }


        public String toString() {
            return "rNode: " + rNode + "\tparent: " + parent + "\np: " + p + "\tnode2p: " + node2p + "\nq: " + q + "\tnode2q: " + node2q +
                    "\ndependent tree Nodes: " + dependentTreeNodes + "\ndpendent Reticulations: " + dependentRNodes;
        }

        public Node getrNode() {
            return rNode;
        }

        public void setrNode(Node rNode) {
            this.rNode = rNode;
        }

        public Node getParent() {
            return parent;
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        public Edge getP() {
            return p;
        }

        public void setP(Edge p) {
            this.p = p;
        }

        public Edge getQ() {
            return q;
        }

        public void setQ(Edge q) {
            this.q = q;
        }

        public HashSet getDependentTreeNodes() {
            return dependentTreeNodes;
        }

        public void setDependentTreeNodes(HashSet dependentTreeNodes) {
            this.dependentTreeNodes = dependentTreeNodes;
        }

        public HashSet getDependentRNodes() {
            return dependentRNodes;
        }

        public void setDependentRNodes(HashSet dependentRNodes) {
            this.dependentRNodes = dependentRNodes;
        }

        public boolean addDependentRNode(Node rNode) {
            if (this.dependentRNodes == null) this.dependentRNodes = new HashSet();
            return this.dependentRNodes.add(rNode);
        }

        public boolean addDependentTreeNode(Node treeNode) {
            if (this.dependentTreeNodes == null) this.dependentTreeNodes = new HashSet();
            return this.dependentTreeNodes.add(treeNode);
        }

        public Node getNode2p() {
            return node2p;
        }

        public void setNode2p(Node node2p) {
            this.node2p = node2p;
        }

        public Node getNode2q() {
            return node2q;
        }

        public void setNode2q(Node node2q) {
            this.node2q = node2q;
        }

        public LinkedList getPathParent2P() {
            return pathParent2P;
        }

        public void setPathParent2P(LinkedList pathParent2P) {
            this.pathParent2P = pathParent2P;
        }

        public LinkedList getPathParent2Q() {
            return pathParent2Q;
        }

        public void setPathParent2Q(LinkedList pathParent2Q) {
            this.pathParent2Q = pathParent2Q;
        }
    }

    /**
     * compute a naive ordering
     *
     * @param tree
     * @param node2ChildrenInNetwork
     */
    public void applyNaiveOrdering(PhyloTree tree, NodeArray node2ChildrenInNetwork) {
        List list = new LinkedList();
        NodeSet seen = new NodeSet(tree);
        list.add(tree.getRoot());
        while (list.size() > 0) {
            Node v = (Node) list.remove(0);
            List nodes = new LinkedList();
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node w = e.getTarget();
                if (!seen.contains(w)) {
                    seen.add(w);
                    nodes.add(w);
                    list.add(w);
                }

            }
            node2ChildrenInNetwork.put(v, nodes);
        }
    }
}

