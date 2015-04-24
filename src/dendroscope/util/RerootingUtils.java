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

package dendroscope.util;

import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.graph.*;
import jloda.graphview.EdgeView;
import jloda.phylo.PhyloTree;
import jloda.util.Alert;
import jloda.util.Triplet;

import javax.swing.*;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * rerooting methods
 * Daniel Huson, 4.2008
 */
public class RerootingUtils {

    /**
     * reroot tree
     *
     * @return true, if rerooted
     */
    public static boolean reroot(TreeViewer viewer) {
        PhyloTree tree = viewer.getPhyloTree();
        if (viewer.getNumberSelectedEdges() == 1) {
            Edge e = viewer.getSelectedEdges().getFirstElement();
            if ((e.getSource() == tree.getRoot() || e.getTarget() == tree.getRoot()) && tree.getRoot().getDegree() == 2)
                return false; // no need to reroot

            if (tree.getNumberSpecialEdges() > 0) {
                if (tree.isSpecial(e))
                    return false; // can't root in special edge
                // todo: bugs need fixing
                if (JOptionPane.showConfirmDialog(MultiViewer.getLastActiveFrame(), "Rerooting networks has major bugs, try anyway?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION)
                        != JOptionPane.YES_OPTION)
                    return false;
                if (belowSpecialEdge(tree, e.getSource())) {
                    System.err.println("WARNING: Can't reroot below a reticulation");
/*                

                    Taxa taxa = new Taxa();
                    try {
                        Utilities.extractTaxa(0, tree, taxa);
                    } catch (IOException ex) {
                        Basic.caught(ex);
                    }
                    dendroscope.consensus.Partition partition = ClusterNetwork.getPartitionDefinedByEdge(tree, taxa, e);


                    if (partition != null && partition.size() > 0) {
                        int result = JOptionPane.showConfirmDialog(viewer.getFrame(), "Rerooting here will change the topology of the network, proceed?");
                        if (result == JOptionPane.YES_OPTION) {
                            SplitSystem splits = SplitSystem.getSplitsFromTree(taxa, taxa.getBits(), tree);
                            ClusterNetwork clusterNetwork = new ClusterNetwork(taxa, splits);
                            tree.copy(clusterNetwork.apply());
                            return true;
                        } else
                            return false;
                    } else
                    */

                    return false;
                }
            }

            // not under a special node, reroot in simple way
            tree.setRoot(e);
            tree.redirectEdgesAwayFromRoot();

            Node root = tree.getRoot();
            if (root.getDegree() == 2 && tree.getLabel(root) == null) {
                Edge ea = root.getFirstAdjacentEdge();
                Edge eb = root.getLastAdjacentEdge();
                double weight = tree.getWeight(ea) + tree.getWeight(eb);
                double a = tree.computeAverageDistanceToALeaf(ea.getOpposite(root));
                double b = tree.computeAverageDistanceToALeaf(eb.getOpposite(root));
                double na = 0.5 * (b - a + weight);
                if (na >= weight)
                    na = 0.95 * weight;
                else if (na <= 0)
                    na = 0.05 * weight;
                double nb = weight - na;
                tree.setWeight(ea, na);
                tree.setWeight(eb, nb);
            }

            if (viewer.getShowEdgeWeights() && tree.getRoot() != null) {
                Edge f = tree.getRoot().getFirstAdjacentEdge();
                viewer.setLabel(f, "" + tree.getWeight(f));
                viewer.setLabelVisible(f, true);
                Edge g = tree.getRoot().getLastAdjacentEdge();
                viewer.setLabel(g, "" + tree.getWeight(g));
                viewer.setLabelVisible(g, true);
            }
            return true;
        } else if (viewer.getNumberSelectedNodes() == 1) {
            Node v = viewer.getSelectedNodes().getFirstElement();
            if (v == tree.getRoot())
                return false;
            if (tree.getNumberSpecialEdges() > 0) {
                // v is source of a special edge, can't use it as root
                for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e))
                    if (tree.isSpecial(e))
                        return false;

                // todo: bugs need fixing
                if (JOptionPane.showConfirmDialog(MultiViewer.getLastActiveFrame(), "Rerooting networks has major bugs, try anyway?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION)
                        != JOptionPane.YES_OPTION)
                    return false;


                if (belowSpecialEdge(tree, v)) {
                    System.err.println("WARNING: Can't reroot below a reticulation");
                    /*
                    Taxa taxa = new Taxa();
                    try {
                        Utilities.extractTaxa(0, tree, taxa);
                    } catch (IOException e) {
                        Basic.caught(e);
                    }
                    dendroscope.consensus.Partition partition = ClusterNetwork.getPartitionDefinedByNode(tree, taxa, v);
                    if (partition != null && partition.size() > 0) {
                        int result = JOptionPane.showConfirmDialog(viewer.getFrame(), "Rerooting here will change the topology of the network, proceed?");
                        if (result == JOptionPane.YES_OPTION) {
                            SplitSystem splits = SplitSystem.getSplitsFromTree(taxa, taxa.getBits(), tree);
                            ClusterNetwork clusterNetwork = new ClusterNetwork(taxa, splits);
                            tree.copy(clusterNetwork.apply());
                            return true;
                        } else
                            return false;
                    } else
                    */
                    return false;
                }
            }
            Node root = tree.getRoot();
            if (tree.getDegree(root) == 2 && tree.getLabel(root) == null) {
                Edge g = tree.delDivertex(root);
                viewer.setDirection(g, EdgeView.UNDIRECTED);
                if (viewer.getShowEdgeWeights() == true) {
                    viewer.setLabel(g, "" + tree.getWeight(g));
                    viewer.setLabelVisible(g, true);
                }
            }
            tree.setRoot(v);
            tree.redirectEdgesAwayFromRoot();
            return true;
        }
        return false;

    }

    /**
     * is this node below a special edge?
     *
     * @param tree
     * @param v
     * @return true, if not contained in the same special-edge component as the root
     */
    private static boolean belowSpecialEdge(PhyloTree tree, Node v) {
        NodeSet nodes = tree.getSpecialComponent(tree.getRoot());
        return !nodes.contains(v);
    }


    /**
     * reroot a tree by outgroup. Find the node or edge middle point so that tree is optimally rooted for
     * the given outgroup  labels
     *
     * @param viewer
     * @param outgroupLabels
     * @return true, if tree was rerooted
     */
    public static boolean rerootByOutgroup(TreeViewer viewer, Set outgroupLabels) {
        PhyloTree tree = viewer.getPhyloTree();

        if (tree.getRoot() == null)
            return false;

        if (tree.getSpecialEdges().size() > 0) {
            new Alert("Reroot by outgroup: not implemented for network");
            return false;
        }

        int totalOutgroup = 0;
        int totalNodes = tree.getNumberOfNodes();

        // compute number of outgroup taxa for each node
        NodeIntegerArray node2NumberOutgroup = new NodeIntegerArray(tree);
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            if (tree.getLabel(v) != null && outgroupLabels.contains(tree.getLabel(v))) {
                node2NumberOutgroup.set(v, node2NumberOutgroup.getValue(v) + 1);
                totalOutgroup++;
            }
        }

        System.err.println("total outgroup " + totalOutgroup + " total nodes " + totalNodes);

        EdgeIntegerArray edge2OutgroupBelow = new EdgeIntegerArray(tree); // how many outgroup taxa below this edge?
        EdgeIntegerArray edge2NodesBelow = new EdgeIntegerArray(tree);  // how many nodes below this edge?
        NodeIntegerArray node2OutgroupBelow = new NodeIntegerArray(tree); // how many outgroup taxa below this multifurcation?
        NodeIntegerArray node2NodesBelow = new NodeIntegerArray(tree);     // how many nodes below this multifurcation (including this?)

        rerootByOutgroupRec(tree.getRoot(), null, node2NumberOutgroup, edge2OutgroupBelow, edge2NodesBelow, node2OutgroupBelow, node2NodesBelow, totalNodes, totalOutgroup);

        // find best edge for rooting

        Edge bestEdge = null;
        int outgroupBelowBestEdge = 0;
        int nodesBelowBestEdge = 0;

        for (Edge e = tree.getFirstEdge(); e != null; e = e.getNext()) {
            int outgroupBelowE = edge2OutgroupBelow.getValue(e);
            int nodesBelowE = edge2NodesBelow.getValue(e);
            if (outgroupBelowE < 0.5 * totalOutgroup) {
                outgroupBelowE = totalOutgroup - outgroupBelowE;
                nodesBelowE = totalNodes - nodesBelowE;
            }
            if (bestEdge == null || outgroupBelowE > outgroupBelowBestEdge || (outgroupBelowE == outgroupBelowBestEdge && nodesBelowE < nodesBelowBestEdge)) {
                bestEdge = e;
                outgroupBelowBestEdge = outgroupBelowE;
                nodesBelowBestEdge = nodesBelowE;
            }
            //tree.setLabel(e,""+outgroupBelowE+" "+nodesBelowE);
        }

        // try to find better node for rooting:

        Node bestNode = null;
        int outgroupBelowBestNode = outgroupBelowBestEdge;
        int nodesBelowBestNode = nodesBelowBestEdge;

        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            int outgroupBelowV = node2OutgroupBelow.getValue(v);
            int nodesBelowV = node2NodesBelow.getValue(v);
            if (outgroupBelowV > 0 && (outgroupBelowV > outgroupBelowBestNode || (outgroupBelowV == outgroupBelowBestNode && nodesBelowV < nodesBelowBestNode))) {
                bestNode = v;
                outgroupBelowBestNode = outgroupBelowV;
                nodesBelowBestNode = nodesBelowV;
                // System.err.println("node score: "+outgroupBelowV+" "+nodesBelowV);
            }
        }
        Node oldRoot = tree.getRoot();
        if (bestNode != null && bestNode != tree.getRoot()) {
            tree.setRoot(bestNode);
            tree.redirectEdgesAwayFromRoot();
        } else if (bestEdge != null) {
            tree.setRoot(bestEdge);
            tree.redirectEdgesAwayFromRoot();
            Node root = tree.getRoot();
            if (root.getDegree() == 2 && tree.getLabel(root) == null) {
                Edge ea = root.getFirstAdjacentEdge();
                Edge eb = root.getLastAdjacentEdge();
                double weight = tree.getWeight(ea) + tree.getWeight(eb);
                double a = tree.computeAverageDistanceToALeaf(ea.getOpposite(root));
                double b = tree.computeAverageDistanceToALeaf(eb.getOpposite(root));
                double na = 0.5 * (b - a + weight);
                if (na >= weight)
                    na = 0.95 * weight;
                else if (na <= 0)
                    na = 0.05 * weight;
                double nb = weight - na;
                tree.setWeight(ea, na);
                tree.setWeight(eb, nb);
            }
            if (viewer.getShowEdgeWeights() && tree.getRoot() != null) {
                Edge f = tree.getRoot().getFirstAdjacentEdge();
                viewer.setLabel(f, "" + tree.getWeight(f));
                viewer.setLabelVisible(f, true);
                Edge g = tree.getRoot().getLastAdjacentEdge();
                viewer.setLabel(g, "" + tree.getWeight(g));
                viewer.setLabelVisible(g, true);
            }
        } else return false;
        // if root was changed, remove old root of degree two, if necessary
        if (oldRoot.getOwner() != null && tree.getRoot() != oldRoot) {
            if (tree.getDegree(oldRoot) == 2 && tree.getLabel(oldRoot) == null) {
                Edge g = tree.delDivertex(oldRoot);
                viewer.setDirection(g, EdgeView.UNDIRECTED);
                if (viewer.getShowEdgeWeights()) {
                    viewer.setLabel(g, "" + tree.getWeight(g));
                    viewer.setLabelVisible(g, true);
                }
            }
        }
        return true;
    }

    /**
     * recursively determine the best place to root the tree for the given outgroup
     *
     * @param v
     * @param e
     * @param node2NumberOutgroup
     * @param edge2OutgroupBelow
     * @param edge2NodesBelow
     * @param node2OutgroupBelow
     * @param node2NodesBelow
     * @param totalNodes
     * @param totalOutgroup
     */
    private static void rerootByOutgroupRec(Node v, Edge e, NodeIntegerArray node2NumberOutgroup, EdgeIntegerArray edge2OutgroupBelow,
                                            EdgeIntegerArray edge2NodesBelow, NodeIntegerArray node2OutgroupBelow, NodeIntegerArray node2NodesBelow, int totalNodes, int totalOutgroup) {
        int outgroupBelowE = node2NumberOutgroup.getValue(v);
        int nodesBelowE = 1; // including v

        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            rerootByOutgroupRec(f.getTarget(), f, node2NumberOutgroup, edge2OutgroupBelow, edge2NodesBelow, node2OutgroupBelow, node2NodesBelow, totalNodes, totalOutgroup);
            outgroupBelowE += edge2OutgroupBelow.getValue(f);
            nodesBelowE += edge2NodesBelow.getValue(f);
        }
        if (e != null) {
            edge2NodesBelow.set(e, nodesBelowE);
            edge2OutgroupBelow.set(e, outgroupBelowE);
        }

        // if v is a multifurcation then we may need to use it as root
        if (v.getOutDegree() > 2) // multifurcation
        {
            int outgroupBelowV = outgroupBelowE + node2NumberOutgroup.getValue(v);

            if (outgroupBelowV == totalOutgroup) // all outgroup taxa lie below here
            {
                // count nodes below in straight-forward way
                node2OutgroupBelow.set(v, outgroupBelowV);

                int nodesBelowV = 1;
                for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                    if (edge2OutgroupBelow.getValue(f) > 0)
                        nodesBelowV += edge2NodesBelow.getValue(f);
                }
                node2NodesBelow.set(v, nodesBelowV);
            } else // outgroupBelowE<totalOutgroup, i.e. some outgroup nodes lie above e
            {
                // count nodes below in parts not containing outgroup taxa and then subtract appropriately

                boolean keep = false;
                int nodesBelowV = 0;
                for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                    if (edge2OutgroupBelow.getValue(f) > 0)
                        keep = true;   // need to have at least one node below that contains outgroup taxa
                    else
                        nodesBelowV += edge2NodesBelow.getValue(f);
                }
                if (keep) {
                    node2OutgroupBelow.set(v, totalOutgroup);
                    node2NodesBelow.set(v, totalNodes - nodesBelowV);
                }
            }
        }
    }

    /**
     * reroots tree using midpoint rooting
     *
     * @param viewer
     * @return true, if tree rerooted
     */
    public static boolean rerootByMidpoint(TreeViewer viewer) {
        PhyloTree tree = viewer.getPhyloTree();

        SortedSet<Triplet<Edge, Float, Float>> rankedMidpointRootings = getRankedMidpointRootings(tree);

        Triplet<Edge, Float, Float> best = rankedMidpointRootings.first();
        Edge e = best.get1();
        Node v = e.getSource();
        Node w = e.getTarget();
        float a = best.get2();
        float b = best.get3();
        float weight = (float) tree.getWeight(e);
        float halfOfTotal = (a + b + weight) / 2;

        Node oldDivertex = tree.getOutDegree(tree.getRoot()) == 2 ? tree.getRoot() : null;

        if (halfOfTotal <= a) {
            if (tree.getRoot() == v)
                return false;
            tree.setRoot(v);
        } else if (halfOfTotal >= a + weight) {
            if (tree.getRoot() == w)
                return false;
            tree.setRoot(w);
        } else {
            float vpWeight = halfOfTotal - a;
            float pwWeight = a + weight - halfOfTotal;
            Node p = tree.newNode();
            tree.setRoot(p);
            Edge vp = tree.newEdge(v, p);
            tree.setWeight(vp, vpWeight);
            Edge pw = tree.newEdge(p, w);
            tree.setWeight(pw, pwWeight);
            tree.deleteEdge(e);
        }
        tree.redirectEdgesAwayFromRoot();
        if (oldDivertex != null)
            tree.delDivertex(oldDivertex);
        return true;
    }

    /**
     * gets all mid-point rootings edges ranked by increasing level of imbalance (absolute difference of distances of
     * source and target to furthest leaf without through e)
     *
     * @return collection of triplets: edge,
     */
    public static SortedSet<Triplet<Edge, Float, Float>> getRankedMidpointRootings(final PhyloTree tree) {

        EdgeArray<Float> maxBottomUpDistance = new EdgeArray<Float>(tree);
        EdgeArray<Float> maxTopDownDistance = new EdgeArray<Float>(tree);

        for (Edge e = tree.getRoot().getFirstOutEdge(); e != null; e = tree.getRoot().getNextOutEdge(e))
            computeMaxBottomUpDistance(tree, e, maxBottomUpDistance);
        computeMaxTopDownDistanceRec(tree, tree.getRoot(), maxBottomUpDistance, maxTopDownDistance);

        SortedSet<Triplet<Edge, Float, Float>> result = new TreeSet<Triplet<Edge, Float, Float>>(new Comparator<Triplet<Edge, Float, Float>>() {
            public int compare(Triplet<Edge, Float, Float> a, Triplet<Edge, Float, Float> b) {
                float compare = Math.abs(a.get2() - a.get3()) - Math.abs(b.get2() - b.get3());
                if (compare < 0)
                    return -1;
                else if (compare > 0)
                    return 1;
                else if (a.get1().getId() < b.get1().getId())
                    return -1;
                else if (a.get1().getId() > b.get1().getId())
                    return 1;
                else
                    return 0;
            }
        });
        for (Edge e = tree.getFirstEdge(); e != null; e = tree.getNextEdge(e)) {
            Triplet<Edge, Float, Float> triplet = new Triplet<Edge, Float, Float>(e, maxTopDownDistance.get(e), maxBottomUpDistance.get(e));
            result.add(triplet);
        }
        if (false) {
            System.err.println("Ranking:");
            for (Triplet<Edge, Float, Float> triplet : result) {
                System.err.println(triplet);
            }
        }
        return result;
    }

    /**
     * compute the midpoint score for all edges
     *
     * @param tree
     * @return midpoint scores
     */
    public static EdgeArray<Float> getMidpointScores(PhyloTree tree) {
        EdgeArray<Float> maxBottomUpDistance = new EdgeArray<Float>(tree);
        EdgeArray<Float> maxTopDownDistance = new EdgeArray<Float>(tree);
        for (Edge e = tree.getRoot().getFirstOutEdge(); e != null; e = tree.getRoot().getNextOutEdge(e))
            computeMaxBottomUpDistance(tree, e, maxBottomUpDistance);
        computeMaxTopDownDistanceRec(tree, tree.getRoot(), maxBottomUpDistance, maxTopDownDistance);

        EdgeArray<Float> scores = new EdgeArray<Float>(tree);
        for (Edge e = tree.getRoot().getFirstOutEdge(); e != null; e = tree.getRoot().getNextOutEdge(e)) {
            scores.set(e, Math.abs(maxBottomUpDistance.get(e) - maxTopDownDistance.get(e)));
        }
        return scores;
    }

    /**
     * compute the midpoint score a given root node
     *
     * @param tree
     * @param root
     * @return midpoint score
     */
    public static float getMidpointScore(PhyloTree tree, Node root) {
        SortedSet<Double> distances = new TreeSet<Double>();

        for (Edge e = root.getFirstOutEdge(); e != null; e = root.getNextOutEdge(e)) {
            distances.add(computeMaxDistanceRec(tree, e.getTarget(), e) + tree.getWeight(e));
        }
        double first = distances.last();
        distances.remove(distances.last());
        double second = distances.last();
        return (float) Math.abs(first - second);
    }

    /**
     * compute the maximum distance from v to a leaf in a tree, avoiding edge f
     *
     * @param tree
     * @param v
     * @param f
     * @return max distance
     */
    private static float computeMaxDistanceRec(PhyloTree tree, Node v, Edge f) {
        float dist = 0;
        for (Edge e = v.getFirstAdjacentEdge(); e != null; e = v.getNextAdjacentEdge(e)) {
            if (e != f) {
                dist = Math.max(dist, computeMaxDistanceRec(tree, e.getOpposite(v), e) + (float) tree.getWeight(e));
            }
        }
        return dist;
    }


    /**
     * bottom up calculation of max down distance
     *
     * @param tree
     * @param e
     * @param maxDownDistance
     * @return distance down (including length of e)
     */
    private static float computeMaxBottomUpDistance(PhyloTree tree, Edge e, EdgeArray<Float> maxDownDistance) {
        Node w = e.getTarget();
        float depth = 0;
        for (Edge f = w.getFirstOutEdge(); f != null; f = w.getNextOutEdge(f)) {
            depth = Math.max(computeMaxBottomUpDistance(tree, f, maxDownDistance), depth);
        }
        maxDownDistance.set(e, depth);
        return depth + (float) tree.getWeight(e);
    }

    /**
     * recursively compute best topdown distance
     *
     * @param tree
     * @param v
     * @param maxDownDistance
     * @param maxUpDistance
     */
    private static void computeMaxTopDownDistanceRec(PhyloTree tree, Node v, EdgeArray<Float> maxDownDistance, EdgeArray<Float> maxUpDistance) {
        float bestUp;
        Edge inEdge = v.getFirstInEdge();
        if (inEdge != null)
            bestUp = maxUpDistance.get(inEdge) + (float) tree.getWeight(inEdge);
        else
            bestUp = 0;

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            float best = bestUp;
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                if (f != e) {
                    best = Math.max(best, maxDownDistance.get(f) + (float) tree.getWeight(f));
                }
            }
            maxUpDistance.set(e, best);
        }
        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            computeMaxTopDownDistanceRec(tree, e.getTarget(), maxDownDistance, maxUpDistance);
        }
    }
}
