/*
 * ClusterNetwork.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.algorithms.clusternet;

import dendroscope.consensus.*;
import dendroscope.util.RerootingUtils;
import jloda.graph.*;
import jloda.phylo.PhyloTree;
import jloda.util.ProgramProperties;
import jloda.util.StringUtils;
import jloda.util.Triplet;

import java.util.*;
import java.util.stream.Collectors;

/**
 * constructs a cluster network from splits
 * Daniel Huson, 9.2007
 */
public class ClusterNetwork {
    private final Taxa taxa;
    private final SplitSystem splits;
    private PhyloTree tree;

    /**
     * constructor
     *
	 */
    public ClusterNetwork(Taxa taxa, SplitSystem splits) {
        this.taxa = taxa;
        this.splits = splits;
        tree = new PhyloTree();
    }

    /**
     * Computes the cluster network. Treats the last named taxon as the outgroup
     *
     * @return tree from splits
     */
    public PhyloTree apply() {
        System.err.println("Constructing cluster network (taxa=" + taxa.size() + ", splits=" + splits.size() + "):");

        tree = new PhyloTree();
        if (!splits.isFullSplitSystem(taxa)) {
            System.err.println("Warning: ClusterNetwork.apply(): not full split system");
            return tree;
        }

        /*
        System.err.println("Taxa:");
        for (int t = 1; t <= taxa.size(); t++) {
            System.err.println("t=" + t + ": " + taxa.getLabel(t));
        }
        */

        var root = tree.newNode();
        tree.setRoot(root);

        //   determine the clusters
        var list = new ArrayList<Cluster>();
        var outGroupTaxonId = taxa.maxId();
        for (var i = 1; i <= splits.size(); i++) {
            var split = splits.getSplit(i);
            var A = split.getPartNotContainingTaxon(outGroupTaxonId);
            //  System.err.println("A=" + A + " taxa=" + taxa);
            if (A.cardinality() < taxa.size() - 1)
                list.add(new Cluster(A, split.getWeight()));
        }
        var clusters = list.toArray(new Cluster[0]);

        var additionalEdges = new ArrayList<Triplet<Cluster, Cluster, Boolean>>();

        var node2weight = new NodeDoubleArray(tree);
        var node2confidence = new NodeDoubleArray(tree);

        constructHasse(taxa, tree, root, clusters, node2weight, node2confidence, additionalEdges, taxa.size());

        convertHasseToClusterNetwork(tree, node2weight);

        // computeConfidenceOnReticulate(tree);

        for (var e : tree.edges()) {
            if (ProgramProperties.get("scaleconfidence", false) && tree.isReticulateEdge(e)) {
                tree.setWeight(e, tree.getConfidence(e));
            }
        }

        //if (!optionOptimize) // only simply if we haven't optimized
        //simplify(root);

        {
            var numberOfSpecialNodes = 0;
            for (var v : tree.nodes()) {
                if (v.getInDegree() > 1)
                    numberOfSpecialNodes++;
                System.err.println("Special nodes: " + numberOfSpecialNodes);
            }
        }

        for (var v : tree.nodes()) {
            if (v != root && v.getDegree() == 1 && tree.getLabel(v) == null)
                throw new RuntimeException("unlabeled leaf");
        }
        return tree;
    }

    /**
     * construct the Hasse diagram for a set of clusters. If any taxa > maxTaxonId are found, these are
     * used to compute the diagram, but are not added as labels to the nodes.
     *
	 */
    public static void constructHasse(Taxa taxa, PhyloTree tree, Node root, Cluster[] clusters, NodeDoubleArray node2weight,
                                      NodeDoubleArray node2confidence, List<Triplet<Cluster, Cluster, Boolean>> additionalEdges, int maxTaxonId) {
        clusters = Cluster.getClustersSortedByDecreasingCardinality(clusters);
        var nodes = new Node[clusters.length];
        var node2id = new NodeIntArray(tree);
        var additionalEdgeSources = new NodeSet(tree);

        for (var i = 0; i < clusters.length; i++) {
            nodes[i] = tree.newNode();
            node2id.set(nodes[i], i);
            if (node2weight != null)
                node2weight.put(nodes[i], clusters[i].getWeight());
            if (node2confidence != null)
                node2confidence.put(nodes[i], clusters[i].getConfidence());
            //System.err.println("cluster: " + clusters[i] + " confidence: " + clusters[i].getConfidence());
        }

        var newEdges = new EdgeSet(tree);

        var stack = new Stack<Node>();
        var visited = new NodeSet(tree);
        for (var i = 0; i < clusters.length; i++) {
            var node = nodes[i];
            var cluster = clusters[i];
            visited.clear();
            stack.push(root);
            visited.add(root);
            while (stack.size() > 0) {
                var v = stack.pop();
                var isBelow = false;
                for (var e : v.outEdges()) {
                    if (newEdges.contains(e)) {
                        var w = e.getTarget();
                        var j = node2id.getInt(w);
                        if (Cluster.contains(clusters[j], cluster)) {
                            isBelow = true;
                            if (!visited.contains(w)) {
                                visited.add(w);
                                stack.push(w);
                            }
                        }
                    }
                    }
                if (!isBelow) {
                    Edge e = tree.newEdge(v, node);
                    newEdges.add(e);
                }
            }
        }

        // set labels:
        stack.push(root);
        visited.clear();
        visited.add(root);
        while (stack.size() > 0) {
            var v = stack.pop();
            var cluster = (BitSet) clusters[node2id.getInt(v)].clone();

            for (var e : v.outEdges()) {
                if (newEdges.contains(e)) {
                    var w = e.getTarget();
                    cluster = Cluster.setminus(cluster, clusters[node2id.getInt(w)]);
                    if (!visited.contains(w)) {
                        stack.push(w);
                        visited.add(w);
                    }
                }
            }
            for (var t = cluster.nextSetBit(0); t != -1 && t <= maxTaxonId; t = cluster.nextSetBit(t + 1)) {
                tree.addTaxon(v, t);
                if (tree.getLabel(v) == null)
                    tree.setLabel(v, taxa.getLabel(t));
                else
                    tree.setLabel(v, tree.getLabel(v) + "," + taxa.getLabel(t));
                tree.setInfo(v, tree.getLabel(v));
            }
        }

        if (additionalEdges != null && additionalEdges.size() > 0) {
            var seen = new HashSet<Cluster>();

            for (var add : additionalEdges) {
                var first = add.getFirst();
                var second = add.getSecond();
                var up = add.getThird();
                var v = findNode(first, clusters, nodes, maxTaxonId);
                var w = findNode(second, clusters, nodes, maxTaxonId);

                if (w.getInDegree() == 1 && additionalEdgeSources.contains(w.getFirstInEdge().getSource()))
                    w = w.getFirstInEdge().getSource();

                System.err.println("Creating additional edge: " + StringUtils.toString(first) + " -> " + StringUtils.toString(second) + "; up=" + up);

                if (up) {
                    if (additionalEdgeSources.contains(v.getFirstInEdge().getSource())) {
                        tree.newEdge(v.getFirstInEdge().getSource(), w);
                    } else {
                        System.err.println("Inserting new node, source node has indegree=" + v.getInDegree());

                        var u = tree.newNode();
                        additionalEdgeSources.add(u);
                        var toDelete = new ArrayList<Edge>();
                        for (var f : v.inEdges()) {
                            tree.newEdge(f.getSource(), u);
                            toDelete.add(f);
                        }
                        tree.newEdge(u, v);
                        tree.newEdge(u, w);
                        for (var g : toDelete)
                            tree.deleteEdge(g);

                    }
                } else
                    tree.newEdge(v, w);

                if (seen.contains(second))
                    System.err.println("WARNING: multiple additional edges to cluster: " + second);
                else
                    seen.add(second);
            }
        }
    }

    /**
     * find node that represents a given cluster, or null
     *
     * @return node or null
     */
    private static Node findNode(Cluster cluster, Cluster[] clusters, Node[] nodes, int maxTaxonId) {
        for (var i = 0; i < clusters.length; i++) {
            if (Cluster.equals(clusters[i], cluster, maxTaxonId))
                return nodes[i];
        }
        return null;
    }


    /**
     * extend the Hasse diagram to obtain a cluster network
     *
	 */
    public static void convertHasseToClusterNetwork(PhyloTree tree, NodeDoubleArray node2weight) {
        // split every node that has indegree>1 and outdegree!=1
        var nodes = tree.nodeStream().collect(Collectors.toCollection(ArrayList::new));

        for (var v : nodes) {
            if (v.getInDegree() > 1) {
                var w = tree.newNode();
                var toDelete = new ArrayList<Edge>();
                for (var e : v.inEdges()) {
                    var u = e.getSource();
                    var f = tree.newEdge(u, w);
                    tree.setWeight(f, 0); // special edges have zero weight
                    tree.setReticulate(f, true);
                    toDelete.add(e);
                }
                var f = tree.newEdge(w, v);
                if (node2weight != null) {
                    if (node2weight.getDouble(v) == -1)  // todo: fix code so that is ok for node to have only special edges
                    {
                        tree.setWeight(f, 0);
                    } else {
                        tree.setWeight(f, node2weight.getDouble(v));
                    }
                }

                for (var aToDelete : toDelete) {
                    tree.deleteEdge(aToDelete);
                }
                //treeView.setLocation(w, treeView.getLocation(v).getX(), treeView.getLocation(v).getY() + 0.1);
            } else if (v.getInDegree() == 1) {
                var e = v.getFirstInEdge();
                if (node2weight != null)
                    tree.setWeight(e, node2weight.getDouble(v));
            }
        }
        var root = tree.getRoot();
        if (root.getDegree() == 2 && tree.getLabel(root) == null)  // is midway root, must divide both weights by two
        {
            var e = root.getFirstAdjacentEdge();
            var f = root.getLastAdjacentEdge();
            var weight = 0.5 * (tree.getWeight(e) + tree.getWeight(f));
            var a = RerootingUtils.computeAverageDistanceToALeaf(tree, e.getOpposite(root));
            var b = RerootingUtils.computeAverageDistanceToALeaf(tree, f.getOpposite(root));
            var na = 0.5 * (b - a + weight);
            if (na >= weight)
                na = 0.95 * weight;
            else if (na <= 0)
                na = 0.05 * weight;
            var nb = weight - na;
            tree.setWeight(e, na);
            tree.setWeight(f, nb);
        }

        // todo:    the algorithm sometimes leaves an additional edge at the root
        if (root.getOutDegree() == 1) {
            root = tree.getRoot().getFirstOutEdge().getTarget();
            tree.deleteNode(tree.getRoot());
            tree.setRoot(root);
        }
    }

    /**
     * compute confidence of reticulate edges as max confidence on all paths to lsa
     *
	 */
    private static void computeConfidenceOnReticulate(PhyloTree tree) {
        var reticulate2lsa = new NodeArray<Node>(tree);
        (new LSATree()).computeReticulation2LSA(tree, reticulate2lsa);

        var averageConfidenceBelow = new NodeDoubleArray(tree);
        var countBelow = tree.newNodeIntArray();
        computeConfidenceBelowRec(tree, tree.getRoot(), averageConfidenceBelow, countBelow);
        for (var v : tree.nodes()) {
            if (countBelow.getInt(v) > 0)
                averageConfidenceBelow.put(v, averageConfidenceBelow.getDouble(v) / countBelow.getInt(v));
        }
        for (var v : tree.nodes()) {
            var lsa = reticulate2lsa.get(v);
            if (lsa != null) {
                //System.err.println("node v=" + v);
                //System.err.println("lsa=" + lsa);
                var e2AverageConfidence = new HashMap<Edge, Double>();
                for (var e : v.inEdges()) {
                    // get all edges between e and lsa
                    var stack = new Stack<Edge>();
                    stack.push(e);
                    var seen = new HashSet<Edge>();
                    seen.add(e);
                    while (stack.size() > 0) {
                        var f = stack.pop();
                        var w = f.getSource();
                        if (w != lsa) {
                            for (var g : w.inEdges()) {
                                if (!seen.contains(g)) {
                                    seen.add(g);
                                    stack.push(g);
                                }
                            }
                        }
                    }
                    e2AverageConfidence.put(e, computeAverageConfidence(tree, seen));
                }
                var sum = 0.0;
                for (var x : e2AverageConfidence.values()) {
                    sum += (x);
                }
                for (var e : e2AverageConfidence.keySet()) {
                    var value = e2AverageConfidence.get(e);
                    var confidence = (sum == 0 ? 0 : (averageConfidenceBelow.getDouble(e.getTarget()) * value) / sum);
                }
            }
        }
    }

    /**
     * recursively compute the confidence and count below each node
     *
	 */
    private static void computeConfidenceBelowRec(PhyloTree tree, Node v, NodeDoubleArray confidenceBelow, NodeIntArray countBelow) {
        var confidence = 0.0;
        var count = 0;
        for (var e : v.outEdges()) {
            if (!tree.isReticulateEdge(e)) {
                confidence += tree.getConfidence(e);
                count++;
            }
            var w = e.getTarget();
            if (countBelow.get(w) == null)
                computeConfidenceBelowRec(tree, w, confidenceBelow, countBelow);
            confidence += confidenceBelow.getDouble(w);
            count += countBelow.getInt(w);
        }
        confidenceBelow.put(v, confidence);
        countBelow.set(v, count);
    }

    /**
     * computes the average confidence of a collection of edges. Uses only non-special edges
     *
     * @return average confidence
     */
    private static double computeAverageConfidence(PhyloTree tree, Collection<Edge> edges) {
        var sum = 0.0;
        var count = 0;
        for (var e : edges) {
            if (!tree.isReticulateEdge(e)) {
                sum += tree.getConfidence(e);
                count++;
            }
        }
        return (count == 0 ? 0 : sum / count);
    }


    /**
     * determines the partitioning of taxa defined by the node v
     *
     * @return partitioning of taxa
     */
    public static Partition getPartitionDefinedByNode(PhyloTree tree, Taxa taxa, Node v) {
        var node2component = new NodeIntArray(tree);
        var componentNumber = 0;

        node2component.set(v, -1); // always avoid this node
        for (var e : v.adjacentEdges()) {
            var w = e.getOpposite(v);
            if (node2component.getInt(w) == 0) {
                componentNumber++;
                node2component.set(w, componentNumber);
                getTwoConnectedComponentRec(w, componentNumber, node2component);
            }
        }

        final var partition = new BitSet[componentNumber];
        for (var p = 0; p < componentNumber; p++)
            partition[p] = new BitSet();
        for (var w : tree.nodes()) {
            var label = tree.getLabel(w);
            if (label != null) {
                int t = taxa.indexOf(label);
                if (t > 0) {
                    int n = node2component.getInt(w);
                    if (n > 0)
                        partition[n - 1].set(t);
                }
            }
        }
        var count = 0;
        for (var p = 0; p < componentNumber; p++) {
            count += partition[p].cardinality();
        }
        if (count == taxa.size())
            return new Partition(partition);
        else
            return null;
    }

    /**
     * gets the partitioning of taxa defined by an edge in the graph
     *
     * @return partitioning defined by edge
     */
    public static Partition getPartitionDefinedByEdge(PhyloTree tree, Taxa taxa, Edge e0) {
        var node2component = new NodeIntArray(tree);
        var componentNumber = 0;

        var source = e0.getSource();
        var target = e0.getTarget();

        node2component.set(source, ++componentNumber);

        var first = true;
        for (var e : source.adjacentEdges()) {
            if (e != e0) {
                var w = e.getOpposite(source);
                if (node2component.getInt(w) == 0) {
                    if (first)
                        first = false;
                    else
                        componentNumber++;
                    node2component.set(w, componentNumber);
                    getTwoConnectedComponentRec(w, componentNumber, node2component);
                }
            }
        }

        if (node2component.getInt(target) == 0) {
            node2component.set(target, ++componentNumber);
            first = true;
            for (var e : target.adjacentEdges()) {
                var w = e.getOpposite(target);
                if (node2component.getInt(w) == 0) {
                    if (first)
                        first = false;
                    else
                        componentNumber++;
                    node2component.set(w, componentNumber);
                    getTwoConnectedComponentRec(w, componentNumber, node2component);
                }
            }
        }

        var partition = new BitSet[componentNumber];
        for (var p = 0; p < componentNumber; p++)
            partition[p] = new BitSet();
        for (var w : tree.nodes()) {
            var label = tree.getLabel(w);
            if (label != null) {
                var t = taxa.indexOf(label);
                if (t > 0) {
                    int n = node2component.getInt(w);
                    if (n > 0)
                        partition[n - 1].set(t);
                }
            }
        }
        var count = 0;
        for (var p = 0; p < componentNumber; p++) {
            count += partition[p].cardinality();
        }
        if (count == taxa.size())
            return new Partition(partition);
        else
            return null;
    }

    /**
     * recursively do the work
     *
	 */
    private static void getTwoConnectedComponentRec(Node v, int componentNumber, NodeIntArray node2component) {
        for (var f : v.adjacentEdges()) {
            var w = f.getOpposite(v);
            if (node2component.getInt(w) == 0) {
                node2component.set(w, componentNumber);
                getTwoConnectedComponentRec(w, componentNumber, node2component);
            }
        }
    }
}

