/*
 *   TransferVisualization.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.consensus;

import dendroscope.window.TreeViewer;
import jloda.graph.*;
import jloda.phylo.PhyloTree;
import jloda.swing.graphview.EdgeView;
import jloda.swing.graphview.Transform;
import jloda.swing.util.Alert;
import jloda.util.StringUtils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.*;

/**
 * the anticonsensus method of adding transfer edges to a consensus tree
 * Daniel Huson, 6.2008
 */
public class TransferVisualization {
    final PhyloTree consensusTree; // input consensus tree
    final BitSet cladeA; // input clade A
    final BitSet cladeB;  // input clade B

    double threshold = 0;

    // list of horizontal edges computed so far
    final List horizontalEdges = new LinkedList();

    final Taxa taxa = new Taxa();
    final Vector id2node = new Vector();

    /**
     * constructor
     *
	 */
    public TransferVisualization(PhyloTree consensusTree) {
        this.consensusTree = consensusTree;
        // setup id2node and taxa:
        id2node.setSize(consensusTree.getNumberOfNodes() + 1);

        for (Node v = consensusTree.getFirstNode(); v != null; v = v.getNext()) {
            String label = consensusTree.getLabel(v);
            if (label != null && label.length() > 0) {
                int id = taxa.add(label);
                id2node.set(id, v);
            }
        }
        this.cladeA = new BitSet(); // input clade A
        this.cladeB = new BitSet();  // input clade B
    }

    /**
     * constructor
     *
	 */
    public TransferVisualization(PhyloTree consensusTree, Collection cladeANames, Collection cladeBNames) throws Exception {
        this(consensusTree);
        setCladesAandB(cladeANames, cladeBNames);
    }

    /**
     * constructor
     *
	 */
    public TransferVisualization(PhyloTree consensusTree, Collection cladeANames, Collection cladeBNames, double threshold) throws Exception {
        this(consensusTree, cladeANames, cladeBNames);
        setThreshold(threshold);
    }

    /**
     * process a gene tree, name is applied to horizontal edge found
     *
     * @return number of transfer edges identified
     */
    public int applyToTree(PhyloTree geneTree, String name) throws Exception {
        int count = 0;

        if (cladeA.cardinality() == 0)
            throw new Exception("cladeA is empty");
        if (cladeB.cardinality() == 0)
            throw new Exception("cladeB is empty");


        BitSet setC = new BitSet(); // is a clade in gene tree, only set in consensus tree
        BitSet siblingCladeInAOfC = new BitSet();

        Vector id2nodeInGeneTree = computeId2Node(geneTree, taxa);
        Node lsaCladeAInGeneTree = findLSA(geneTree, id2nodeInGeneTree, cladeA);

        findTransferCladeCRec(geneTree, taxa, lsaCladeAInGeneTree, cladeA, cladeB, setC, siblingCladeInAOfC, id2nodeInGeneTree,
                lsaCladeAInGeneTree);

        // todo: apply threshold

        if (setC.cardinality() > 0) {
            if (siblingCladeInAOfC.cardinality() == 0) {
                new Alert("Couldn't determine sibling clade");
                siblingCladeInAOfC.or(cladeA);
            }
            count++;

            TransferEdge hEdge = new TransferEdge(findLSA(consensusTree, id2node, siblingCladeInAOfC),
                    findTargetNodes(consensusTree, setC), name);
            horizontalEdges.add(hEdge);
        }

        return count;
    }


    /**
     * look for maximal subset C of B such that C is clade with LSA(C) >= LSA(A), in gene tree
     *
     * @return all taxa on or below v
     */
    private BitSet findTransferCladeCRec(PhyloTree geneTree, Taxa taxa, Node v, BitSet cladeA, BitSet cladeB, BitSet cladeC,
                                         BitSet siblingCladeInAOfC, Vector id2nodeInGeneTree, Node lsaCladeAInGeneTree) {
        BitSet below = new BitSet();

        String label = geneTree.getLabel(v);
        if (label != null && label.length() > 0 && taxa.indexOf(label) != -1)
            below.set(taxa.indexOf(label));

        BitSet[] belowE = new BitSet[v.getOutDegree()];
        int ie = 0;
        int possibleC = -1;
        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e), ie++) {
            belowE[ie] = findTransferCladeCRec(geneTree, taxa, e.getTarget(), cladeA, cladeB, cladeC, siblingCladeInAOfC, id2nodeInGeneTree,
                    lsaCladeAInGeneTree);
            if (belowE[ie] == null)
                return null; // have found cladeC, return
            below.or(belowE[ie]);

            if (Cluster.contains(cladeB, belowE[ie]))
                possibleC = ie;
        }

        if (possibleC >= 0) {
            BitSet BminusC = new BitSet();
            BminusC.or(cladeB);
            BminusC.andNot(belowE[possibleC]);
            // must check that lsa(A) and lsa(BminusC) are incompariable
            Node lsaBminusC = findLSA(geneTree, id2nodeInGeneTree, BminusC);
            if (!areComparable(geneTree, lsaCladeAInGeneTree, lsaBminusC)) {
                cladeC.clear();
                cladeC.or(belowE[possibleC]);
                siblingCladeInAOfC.clear();
                for (int i = 0; i < belowE.length; i++) {
                    if (i != possibleC) {
                        siblingCladeInAOfC.or(belowE[i]);
                    }
                }
                siblingCladeInAOfC.and(cladeA);
            }
        }

        return below;
    }

    /**
     * are two nodes comparable in the given tree?
     *
     * @return true, if one is ancestor of the other
     */
    private boolean areComparable(PhyloTree tree, Node a, Node b) {

        if (a == b)
            return true;

        NodeSet above = new NodeSet(tree);

        Stack stack = new Stack();
        stack.push(a);
        above.add(a);
        while (stack.size() > 0) {
            Node v = (Node) stack.pop();
            for (Edge e = v.getFirstInEdge(); e != null; e = v.getNextInEdge(e)) {
                Node w = e.getSource();
                if (!above.contains(w)) {
                    if (w == b)
                        return true;
                    stack.push(w);
                    above.add(w);
                }
            }
        }
        stack.push(b);
        above.add(a);
        while (stack.size() > 0) {
            Node v = (Node) stack.pop();
            for (Edge e = v.getFirstInEdge(); e != null; e = v.getNextInEdge(e)) {
                Node w = e.getSource();
                if (!above.contains(w)) {
                    if (w == a)
                        return true;
                    stack.push(w);
                    above.add(w);
                }
            }
        }
        return false;
    }

    /**
     * compute the resulting network from the set of compute clusters
     *
     * @return networm, if transfers were identified, null, else
     */
    public PhyloTree computeResult() {
        PhyloTree tree = new PhyloTree();
        NodeArray oldNode2NewNode = new NodeArray(consensusTree);
        EdgeArray oldEdge2NewEdge = new EdgeArray(consensusTree);

        tree.copy(consensusTree, oldNode2NewNode, oldEdge2NewEdge);

        int countTransfers = 0;

        for (Object horizontalEdge : horizontalEdges) {
            TransferEdge hEdge = (TransferEdge) horizontalEdge;
            Node v = (Node) oldNode2NewNode.get(hEdge.getSource());

            String label;
            String name = hEdge.getLabel();

            if (name != null && name.length() > 0)
                label = "<TS" + (++countTransfers) + "-" + name + ">";
            else
                label = "<TS" + (++countTransfers) + ">";
            if (tree.getLabel(v) != null)
                tree.setLabel(v, tree.getLabel(v) + label);
            else
                tree.setLabel(v, label);


            int countTargets = 0;
            for (int i = 0; i < hEdge.getTargets().length; i++) {
                {
                    Node w = (Node) oldNode2NewNode.get(hEdge.getTargets()[i]);

                    if (name != null && name.length() > 0)
                        label = "<TT" + (countTransfers) + "." + (++countTargets) + "-" + name + ">";
                    else
                        label = "<TT" + (countTransfers) + "." + (++countTargets) + ">";
                    if (tree.getLabel(w) != null)
                        tree.setLabel(w, tree.getLabel(w) + label);
                    else
                        tree.setLabel(w, label);
                }

            }
        }
        return tree;
    }

    /**
     * get the threshold
     *
     * @return threshold
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * set the treshold
     *
	 */
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    /**
     * set clades A and B from lists of taxon names. These two sets must be clades in the consensus tree
     *
	 */
    public void setCladesAandB(Collection cladeANames, Collection cladeBNames) throws Exception {
        cladeA.clear();
        for (Object cladeAName : cladeANames) {
            String label = (String) cladeAName;
            int t = taxa.indexOf(label);
            if (t == -1)
                throw new Exception("Unknown taxon in namesA: " + label);
            cladeA.set(t);
        }
        cladeB.clear();
        for (Object cladeBName : cladeBNames) {
            String label = (String) cladeBName;
            int t = taxa.indexOf(label);
            if (t == -1)
                throw new Exception("Unknown taxon in namesB: " + label);
            cladeB.set(t);
        }

        if (cladeA.intersects(cladeB))
            throw new Exception("clades A and B have non-empty intersection");

        NodeSet nodesA = getNodesForTaxa(consensusTree, id2node, cladeA);
        NodeSet nodesB = getNodesForTaxa(consensusTree, id2node, cladeB);

        Node lsaCladeA = findLSA(consensusTree, nodesA);

        if (!isClade(lsaCladeA, cladeA))
            throw new Exception("cladeANames is not a clade");

        Node lsaCladeB = findLSA(consensusTree, nodesB);

        if (!isClade(lsaCladeB, cladeB))
            throw new Exception("cladeBNames is not a clade");

        if (areComparable(consensusTree, lsaCladeA, lsaCladeB))
            throw new Exception("cladeA and claseB are nested, not allowed!");

        System.err.println("Clade A: " + cladeA.cardinality());

        System.err.println("Clade B: " + cladeB.cardinality());

    }

    /**
     * if the root has out-degree two, use it to define the two clades A and B
     *
	 */
    public void setCladesAandBViaRoot() throws Exception {
        if (consensusTree.getRoot() != null && consensusTree.getRoot().getOutDegree() == 2) {
            Set cladeANames = new HashSet();
            findLabelsBelow(consensusTree, consensusTree.getRoot().getFirstOutEdge().getTarget(), cladeANames);
            Set cladeBNames = new HashSet();
            findLabelsBelow(consensusTree, consensusTree.getRoot().getLastOutEdge().getTarget(), cladeBNames);

            setCladesAandB(cladeANames, cladeBNames);
        } else
            throw new Exception("failed to set A and B via root");
    }

    /**
     * find all labels that can be found on or below the given node
     *
	 */
    private void findLabelsBelow(PhyloTree tree, Node v, Set labels) {
        Stack stack = new Stack();
        stack.push(v);
        while (stack.size() > 0) {
            v = (Node) stack.pop();
            String label = tree.getLabel(v);
            if (label != null && label.length() > 0)
                labels.add(label);
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
				if (tree.okToDescendDownThisEdgeInTraversal(e, v)) {
					stack.push(e.getTarget());
				}
            }
        }
    }

    /**
     * determines whether the given set of taxa is a clade with the given root
     *
     * @return true, if clade with root
     */
    private boolean isClade(Node root, BitSet setOfTaxa) {
        NodeSet seen = new NodeSet(consensusTree);
        Stack stack = new Stack();
        stack.push(root);
        seen.add(root);
        while (stack.size() > 0) {
            Node v = (Node) stack.pop();
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node w = e.getTarget();
                if (!seen.contains(w)) {
                    stack.push(w);
                    seen.add(w);
                }
            }
            String label = consensusTree.getLabel(v);
            if (label != null && label.length() > 0) {
                int t = taxa.indexOf(label);
                if (t != -1 && !setOfTaxa.get(t))
                    return false;
            }
        }
        return true;
    }

    /**
     * sets the set of nodes labeled by the given set of taxa
     *
     * @return nodes
     */
    static public NodeSet getNodesForTaxa(PhyloTree tree, Vector id2node, BitSet taxa) {
        NodeSet result = new NodeSet(tree);

        for (int t = taxa.nextSetBit(0); t != -1; t = taxa.nextSetBit(t + 1))
            result.add((Node) id2node.get(t));

        return result;
    }

    /**
     * gets the lowest single ancestor of a set of nodes in a tree
     *
     * @return LSA of taxa
     */
    static public Node findLSA(PhyloTree tree, NodeSet nodes) {
        if (tree.getRoot() != null && nodes.size() > 0) {
            EdgeSet edges = new EdgeSet(tree); // edges on paths to elements of nodes
            for (Node v = nodes.getFirstElement(); v != null; v = nodes.getNextElement(v)) {
                markPathEdgesRec(v, edges);
            }
            Node v = tree.getRoot();
            while (true) {
                int count = 0;
                Node w = null;
                for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                    if (edges.contains(e)) {
                        count++;
                        w = e.getTarget();
                    }
                }
                if (count != 1)
                    return v;
                else
                    v = w;
            }
        }
        return null;
    }

    /**
     * find the LSA for a set of taxa
     *
     * @return LSA or null
     */
    private Node findLSA(PhyloTree tree, Vector id2nodeInGeneTree, BitSet set) {
        NodeSet nodeSet = new NodeSet(tree);
        for (int t = set.nextSetBit(0); t != -1; t = set.nextSetBit(t + 1)) {
            nodeSet.add((Node) id2nodeInGeneTree.get(t));
        }
        return findLSA(tree, nodeSet);

    }

    /**
     * find all nodes in tree that are above elements of the given set only, maximally
     *
     * @return nodes above setC
     */
    private Node[] findTargetNodes(PhyloTree tree, BitSet setC) {
        Set nodes = new HashSet();

        findTargetNodesRec(tree, tree.getRoot(), setC, nodes);
		return (Node[]) nodes.toArray(new Node[0]);
    }

    /**
     * recursively does the work
     *
     * @return true, if all labels below like in the given set
     */
    private boolean findTargetNodesRec(PhyloTree tree, Node v, BitSet setC, Set targets) {
        boolean ok = true;  // ok if all labeled nodes on or below v are labeled by elements of the given set

        String label = tree.getLabel(v);
        if (label != null && label.length() > 0 && taxa.indexOf(label) != -1)
            if (!setC.get(taxa.indexOf(label)))
                ok = false; // not labeled by element of set

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            Node w = e.getTarget();
            if (!findTargetNodesRec(tree, w, setC, targets))
                ok = false;
        }
        if (ok) // this node is ok, i.e. all taxa below are elements of the set, to remove childern form list of targets
        {
            targets.add(v);
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node w = e.getTarget();
                targets.remove(w);
            }
        }
        return ok;
    }

    /**
     * compute the set of all edges on a path from the root to some node in the given set of nodes
     *
	 */
    private static void markPathEdgesRec(Node v, EdgeSet edges) {
        for (Edge edge = v.getFirstInEdge(); edge != null; edge = v.getNextInEdge(edge)) {
            if (edges.contains(edge))
                break; // must have visited v already
            else {
                edges.add(edge);
                markPathEdgesRec(edge.getSource(), edges);
            }
        }
    }

    /**
     * compute the taxon-id to node mapping for a tree
     *
     * @return mapping
     */
    private Vector<Node> computeId2Node(PhyloTree tree, Taxa taxa) {
        Vector<Node> result = new Vector<>();
        result.setSize(tree.getNumberOfNodes() + 1);
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            String label = tree.getLabel(v);
            if (label != null && label.length() > 0 && taxa.indexOf(label) != -1)
                result.set(taxa.indexOf(label), v);
        }
        return result;
    }

    /**
     * this is used to draw the transfer
     *
	 */
    public static void paint(Graphics2D gc, TreeViewer viewer, PhyloTree tree) {
        Transform trans = viewer.trans;

        Map transfer2sources = new HashMap();
        Map transfer2targets = new HashMap();

        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            List labels = getTransferSources(tree.getLabel(v));
            if (labels != null) {
                for (Object label1 : labels) {
                    String label = (String) label1;
                    List list = (List) transfer2sources.get(label);
                    if (list == null) {
                        list = new LinkedList();
                        transfer2sources.put(label, list);
                    }
                    list.add(v);
                }
            }
            labels = getTransferTargets(tree.getLabel(v));
            if (labels != null) {
                for (Object label1 : labels) {
                    String label = (String) label1;
                    List list = (List) transfer2targets.get(label);
                    if (list == null) {
                        list = new LinkedList();
                        transfer2targets.put(label, list);
                    }
                    list.add(v);
                }
            }
        }
        for (Object o : transfer2sources.keySet()) {
            String label = (String) o;
            List sources = (List) transfer2sources.get(label);
            if (sources != null && sources.size() > 0) {
                List targets = (List) transfer2targets.get(label);

                if (targets != null && targets.size() > 0) {
                    for (Object source : sources) {
                        Node v = (Node) source;
                        Point2D loc;
                        double x = 0;
                        double y = 0;

                        for (Object target1 : targets) {
                            Node w = (Node) target1;
                            loc = viewer.getLocation(w);
                            x += loc.getX();
                            y += loc.getY();

                        }
                        x /= targets.size();
                        y /= targets.size();

                        Point a = trans.w2d(viewer.getLocation(v));
                        Point b = trans.w2d(x, y);
                        gc.setStroke(new BasicStroke(4));
                        gc.setColor(Color.MAGENTA)
                        ;

                        gc.drawLine(a.x, a.y, b.x, b.y);
                        EdgeView.drawArrowHead(gc, a, b);

                        gc.setStroke(new BasicStroke(1));
                        gc.setColor(Color.MAGENTA);

                        for (Object target : targets) {
                            Node w = (Node) target;
                            Point c = trans.w2d(viewer.getLocation(w));
                            gc.drawLine(b.x, b.y, c.x, c.y);
                            EdgeView.drawArrowHead(gc, b, c);


                        }
                    }
                }
            }
        }
    }

    /**
     * extracts all the transfer source labels in a node label
     *
     * @return list of labels
     */
    private static List getTransferSources(String string) {
        if (string == null || string.length() == 0)
            return null;

        List result = new LinkedList();

        int i = 0;

        while ((i = string.indexOf("<TS", i)) != -1) {
            int j = string.indexOf("-", i);
            if (j != -1) {
                int k = string.indexOf(">", j);
                if (k != -1) {
                    String label = string.substring(j + 1, k);
                    result.add(label);
                }
            }
            i++;
        }
        return result;

    }

    /**
     * extracts all the transfer target labels in a node label
     *
     * @return list of labels
     */
    private static List getTransferTargets(String string) {
        if (string == null || string.length() == 0)
            return null;

        List result = new LinkedList();

        int i = 0;

        while ((i = string.indexOf("<TT", i)) != -1) {
            int j = string.indexOf("-", i);
            if (j != -1) {
                int k = string.indexOf(">", j);
                if (k != -1) {
                    String label = string.substring(j + 1, k);
                    result.add(label);
                }
            }
            i++;
        }
        return result;

    }
}

class TransferEdge {
    Node source;
    Node[] targets;
    String label;

    public TransferEdge(Node source, Node[] targets, String label) {
        this.source = source;
        this.targets = targets;
        this.label = label;
    }

    public String toString() {
		return "Transfer-edge (" + label + "): " + source + " to " + StringUtils.toString(targets, ",");
    }

    public Node getSource() {
        return source;
    }

    public void setSource(Node source) {
        this.source = source;
    }

    public Node[] getTargets() {
        return targets;
    }

    public void setTargets(Node[] targets) {
        this.targets = targets;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
