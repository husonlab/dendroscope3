/*
 * ClusterNetwork.java Copyright (C) 2022 Daniel H. Huson
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

        Node root = tree.newNode();
        tree.setRoot(root);

        //   determine the clusters
        List<Cluster> list = new LinkedList<>();
        int outGroupTaxonId = taxa.maxId();
        for (int i = 1; i <= splits.size(); i++) {
            Split split = splits.getSplit(i);
            BitSet A = split.getPartNotContainingTaxon(outGroupTaxonId);
            //  System.err.println("A=" + A + " taxa=" + taxa);
            if (A.cardinality() < taxa.size() - 1)
                list.add(new Cluster(A, split.getWeight()));
        }
        Cluster[] clusters = list.toArray(new Cluster[0]);

        List<Triplet<Cluster, Cluster, Boolean>> additionalEdges = new LinkedList<>();

        NodeDoubleArray node2weight = new NodeDoubleArray(tree);
        NodeDoubleArray node2confidence = new NodeDoubleArray(tree);

        constructHasse(taxa, tree, root, clusters, node2weight, node2confidence, additionalEdges, taxa.size());

        convertHasseToClusterNetwork(tree, node2weight);

        // computeConfidenceOnReticulate(tree);

        for (Edge e = tree.getFirstEdge(); e != null; e = e.getNext()) {
            if (ProgramProperties.get("scaleconfidence", false) && tree.isReticulatedEdge(e)) {
                tree.setWeight(e, tree.getConfidence(e));
            }
        }

        //if (!optionOptimize) // only simply if we haven't optimized
        //simplify(root);

        {
            int numberOfSpecialNodes = 0;
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext())
                if (v.getInDegree() > 1)
                    numberOfSpecialNodes++;
            System.err.println("Special nodes: " + numberOfSpecialNodes);
        }

        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
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
        Node[] nodes = new Node[clusters.length];
        NodeIntArray node2id = new NodeIntArray(tree);
        NodeSet additionalEdgeSources = new NodeSet(tree);

        for (int i = 0; i < clusters.length; i++) {
            nodes[i] = tree.newNode();
            node2id.set(nodes[i], i);
            if (node2weight != null)
                node2weight.put(nodes[i], clusters[i].getWeight());
            if (node2confidence != null)
                node2confidence.put(nodes[i], clusters[i].getConfidence());
            //System.err.println("cluster: " + clusters[i] + " confidence: " + clusters[i].getConfidence());
        }

        EdgeSet newEdges = new EdgeSet(tree);

        Stack<Node> stack = new Stack<>();
        NodeSet visited = new NodeSet(tree);
        for (int i = 0; i < clusters.length; i++) {
            Node node = nodes[i];
            Cluster cluster = clusters[i];
            visited.clear();
            stack.push(root);
            visited.add(root);
            while (stack.size() > 0) {
                Node v = stack.pop();
                boolean isBelow = false;
                for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e))
                    if (newEdges.contains(e)) {
                        {
                            Node w = e.getTarget();
                            int j = node2id.getInt(w);
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
            Node v = stack.pop();
            BitSet cluster = (BitSet) clusters[node2id.getInt(v)].clone();

            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                if (newEdges.contains(e)) {
                    Node w = e.getTarget();
                    cluster = Cluster.setminus(cluster, clusters[node2id.getInt(w)]);
                    if (!visited.contains(w)) {
                        stack.push(w);
                        visited.add(w);
                    }
                }
            }
            for (int t = cluster.nextSetBit(0); t != -1 && t <= maxTaxonId; t = cluster.nextSetBit(t + 1)) {
                tree.addTaxon(v, t);
                if (tree.getLabel(v) == null)
                    tree.setLabel(v, taxa.getLabel(t));
                else
                    tree.setLabel(v, tree.getLabel(v) + "," + taxa.getLabel(t));
                tree.setInfo(v, tree.getLabel(v));
            }
        }

        if (additionalEdges != null && additionalEdges.size() > 0) {
            Set<Cluster> seen = new HashSet<>();

            for (Triplet<Cluster, Cluster, Boolean> add : additionalEdges) {
                Cluster first = add.getFirst();
                Cluster second = add.getSecond();
                boolean up = add.getThird();
                Node v = findNode(first, clusters, nodes, maxTaxonId);
                Node w = findNode(second, clusters, nodes, maxTaxonId);

                if (w.getInDegree() == 1 && additionalEdgeSources.contains(w.getFirstInEdge().getSource()))
                    w = w.getFirstInEdge().getSource();

				System.err.println("Creating additional edge: " + StringUtils.toString(first) + " -> " + StringUtils.toString(second) + "; up=" + up);

                if (up) {
                    if (additionalEdgeSources.contains(v.getFirstInEdge().getSource())) {
                        tree.newEdge(v.getFirstInEdge().getSource(), w);
                    } else {
                        System.err.println("Inserting new node, source node has indegree=" + v.getInDegree());

                        Node u = tree.newNode();
                        additionalEdgeSources.add(u);
                        List<Edge> toDelete = new LinkedList<>();
                        for (Edge f = v.getFirstInEdge(); f != null; f = v.getNextInEdge(f)) {
                            tree.newEdge(f.getSource(), u);
                            toDelete.add(f);
                        }
                        tree.newEdge(u, v);
                        tree.newEdge(u, w);
                        for (Edge aToDelete : toDelete) tree.deleteEdge(aToDelete);

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
        for (int i = 0; i < clusters.length; i++) {
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
        List<Node> nodes = new LinkedList<>();
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext())
            nodes.add(v);

        for (Node v : nodes) {
            if (v.getInDegree() > 1) {
                Node w = tree.newNode();
                List<Edge> toDelete = new LinkedList<>();
                for (var e : v.inEdges()) {
                    Node u = e.getSource();
                    Edge f = tree.newEdge(u, w);
                    tree.setWeight(f, 0); // special edges have zero weight
                    tree.setReticulated(f, true);
                    toDelete.add(e);
                }
                Edge f = tree.newEdge(w, v);
                if (node2weight != null) {
                    if (node2weight.getDouble(v) == -1)  // todo: fix code so that is ok for node to have only special edges
                    {
                        tree.setWeight(f, 0);
                    } else {
                        tree.setWeight(f, node2weight.getDouble(v));
                    }
                }

                for (Edge aToDelete : toDelete) {
                    tree.deleteEdge(aToDelete);
                }
                //treeView.setLocation(w, treeView.getLocation(v).getX(), treeView.getLocation(v).getY() + 0.1);
            } else if (v.getInDegree() == 1) {
                Edge e = v.getFirstInEdge();
                if (node2weight != null)
                    tree.setWeight(e, node2weight.getDouble(v));
            }
        }
        Node root = tree.getRoot();
        if (root.getDegree() == 2 && tree.getLabel(root) == null)  // is midway root, must divide both weights by two
        {
            Edge e = root.getFirstAdjacentEdge();
            Edge f = root.getLastAdjacentEdge();
            double weight = 0.5 * (tree.getWeight(e) + tree.getWeight(f));
            double a = RerootingUtils.computeAverageDistanceToALeaf(tree, e.getOpposite(root));
            double b = RerootingUtils.computeAverageDistanceToALeaf(tree, f.getOpposite(root));
            double na = 0.5 * (b - a + weight);
            if (na >= weight)
                na = 0.95 * weight;
            else if (na <= 0)
                na = 0.05 * weight;
            double nb = weight - na;
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
        NodeArray<Node> reticulate2lsa = new NodeArray<>(tree);
        (new LSATree()).computeReticulation2LSA(tree, reticulate2lsa);

        var averageConfidenceBelow = new NodeDoubleArray(tree);
        var countBelow = tree.newNodeIntArray();
        computeConfidenceBelowRec(tree, tree.getRoot(), averageConfidenceBelow, countBelow);
        for (var v : tree.nodes()) {
            if (countBelow.getInt(v) > 0)
                averageConfidenceBelow.put(v, averageConfidenceBelow.getDouble(v) / countBelow.getInt(v));
        }
        for (var v : tree.nodes()) {
            Node lsa = reticulate2lsa.get(v);
            if (lsa != null) {
                //System.err.println("node v=" + v);
                //System.err.println("lsa=" + lsa);
                Map<Edge, Double> e2AverageConfidence = new HashMap<>();
                for (Edge e = v.getFirstInEdge(); e != null; e = v.getNextInEdge(e)) {
                    // get all edges between e and lsa
                    Stack<Edge> stack = new Stack<>();
                    stack.push(e);
                    Set<Edge> seen = new HashSet<>();
                    seen.add(e);
                    while (stack.size() > 0) {
                        Edge f = stack.pop();
                        Node w = f.getSource();
                        if (w != lsa) {
                            for (Edge g = w.getFirstInEdge(); g != null; g = w.getNextInEdge(g)) {
                                if (!seen.contains(g)) {
                                    seen.add(g);
                                    stack.push(g);
                                }
                            }
                        }
                    }
                    e2AverageConfidence.put(e, computeAverageConfidence(tree, seen));
                }
                double sum = 0;
                for (Double x : e2AverageConfidence.values()) {
                    sum += (x);
                }
                for (Edge e : e2AverageConfidence.keySet()) {
                    double value = e2AverageConfidence.get(e);
                    double confidence = (sum == 0 ? 0 : (averageConfidenceBelow.getDouble(e.getTarget()) * value) / sum);
                }
            }
        }
    }

    /**
     * recursively compute the confidence and count below each node
     *
	 */
    private static void computeConfidenceBelowRec(PhyloTree tree, Node v, NodeDoubleArray confidenceBelow, NodeIntArray countBelow) {
        double confidence = 0;
        int count = 0;
        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            if (!tree.isReticulatedEdge(e)) {
                confidence += tree.getConfidence(e);
                count++;
            }
            Node w = e.getTarget();
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
        double sum = 0;
        int count = 0;
        for (Edge e : edges) {
            if (!tree.isReticulatedEdge(e)) {
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
        NodeIntArray node2component = new NodeIntArray(tree);
        int componentNumber = 0;

        node2component.set(v, -1); // always avoid this node
        for (Edge e = v.getFirstAdjacentEdge(); e != null; e = v.getNextAdjacentEdge(e)) {
            Node w = e.getOpposite(v);
            if (node2component.getInt(w) == 0) {
                componentNumber++;
                node2component.set(w, componentNumber);
                getTwoConnectedComponentRec(w, componentNumber, node2component);
            }
        }

        final BitSet[] partition = new BitSet[componentNumber];
        for (int p = 0; p < componentNumber; p++)
            partition[p] = new BitSet();
        for (Node w = tree.getFirstNode(); w != null; w = w.getNext()) {
            String label = tree.getLabel(w);
            if (label != null) {
                int t = taxa.indexOf(label);
                if (t > 0) {
                    int n = node2component.getInt(w);
                    if (n > 0)
                        partition[n - 1].set(t);
                }
            }
        }
        int count = 0;
        for (int p = 0; p < componentNumber; p++) {
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
        NodeIntArray node2component = new NodeIntArray(tree);
        int componentNumber = 0;

        Node source = e0.getSource();
        Node target = e0.getTarget();

        node2component.set(source, ++componentNumber);

        boolean first = true;
        for (Edge e = source.getFirstAdjacentEdge(); e != null; e = source.getNextAdjacentEdge(e)) {
            if (e != e0) {
                Node w = e.getOpposite(source);
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
            for (Edge e = target.getFirstAdjacentEdge(); e != null; e = target.getNextAdjacentEdge(e)) {
                Node w = e.getOpposite(target);
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

        BitSet[] partition = new BitSet[componentNumber];
        for (int p = 0; p < componentNumber; p++)
            partition[p] = new BitSet();
        for (Node w = tree.getFirstNode(); w != null; w = w.getNext()) {
            String label = tree.getLabel(w);
            if (label != null) {
                int t = taxa.indexOf(label);
                if (t > 0) {
                    int n = node2component.getInt(w);
                    if (n > 0)
                        partition[n - 1].set(t);
                }
            }
        }
        int count = 0;
        for (int p = 0; p < componentNumber; p++) {
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
        for (Edge f = v.getFirstAdjacentEdge(); f != null; f = v.getNextAdjacentEdge(f)) {
            Node w = f.getOpposite(v);
            if (node2component.getInt(w) == 0) {
                node2component.set(w, componentNumber);
                getTwoConnectedComponentRec(w, componentNumber, node2component);
            }
        }
    }
}

