/**
 * DistanceMethods.java 
 * Copyright (C) 2018 Daniel H. Huson
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

import dendroscope.algorithms.utils.IsomorphismCheck;
import jloda.graph.Edge;
import jloda.graph.Graph;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.io.IOException;
import java.util.*;

/**
 * compute different distances between two networks: - Hardwired Cluster
 * Distance - Path Multiplicity Distance - Nested-Labels Distance - Tripartition
 * Distance - Displayed Trees Distance - Softwired Distance - Subnetwork
 * Distance
 */

public class DistanceMethods {

    // **********************************************************************************
    // Hardwired Cluster Distance
    // **********************************************************************************

    public static double[][] computeHardwiredClusterDistance(List<PhyloTree> trees) {
        PhyloTree[] array = trees.toArray(new PhyloTree[trees.size()]);
        Set<Set<String>>[] clusters = (Set<Set<String>>[]) new HashSet[trees.size()];
        for (int i = 0; i < array.length; i++) {
            clusters[i] = PhyloTreeUtils.collectAllHardwiredClusters(array[i]);
        }

        double[][] distance = new double[trees.size()][array.length];

        for (int i = 0; i < array.length; i++) {
            for (int j = i + 1; j < array.length; j++) {
                Set<Set<String>> common = new HashSet<Set<String>>();
                for (Set<String> cluster : clusters[i]) {
                    if (clusters[j].contains(cluster))
                        common.add(cluster);
                }
                distance[i][j] = distance[j][i] = (clusters[i].size() + clusters[j].size() - 2 * common.size()) / 2;
            }
        }

        return distance;
    }

    // **********************************************************************************
    // Path Multiplicity Distance
    // **********************************************************************************

    public static double computePathMultiplicityDistance(List<PhyloTree> trees) {

        Vector<String> taxa = new Vector<String>();
        for (Node v : trees.get(0).computeSetOfLeaves())
            taxa.add(trees.get(0).getLabel(v));
        Collections.sort(taxa);

        HashSet<String> n1PathVectors = collectPathMultiplicityVectors(
                trees.get(0), taxa);
        HashSet<String> n2PathVectors = collectPathMultiplicityVectors(
                trees.get(1), taxa);

        HashSet<String> diffVectors = new HashSet<String>();
        for (String pV : n1PathVectors) {
            if (!n2PathVectors.contains(pV))
                diffVectors.add(pV);
        }
        for (String pV : n2PathVectors) {
            if (!n1PathVectors.contains(pV))
                diffVectors.add(pV);
        }

        return ((double) diffVectors.size()) / 2;
    }

    private static int numOfPaths = 0;

    private static HashSet<String> collectPathMultiplicityVectors(PhyloTree t,
                                                                  Vector<String> taxa) {
        HashSet<String> pathVectors = new HashSet<String>();
        for (Node v : t.getNodes()) {
            if (v.getOutDegree() != 0) {
                String s = "";
                for (String taxon : taxa) {
                    numOfPaths = 0;
                    computeMultiplicity(t, v, taxon);
                    s = s.concat(String.valueOf(numOfPaths));
                }
                pathVectors.add(s);
            }
        }
        return pathVectors;
    }

    private static void computeMultiplicity(PhyloTree t, Node v, String taxon) {
        Iterator<Edge> it = v.getOutEdges();
        while (it.hasNext()) {
            Node w = it.next().getTarget();
            if (t.getLabel(w) != null && t.getLabel(w).equals(taxon))
                numOfPaths++;
            else
                computeMultiplicity(t, w, taxon);
        }
    }

    // **********************************************************************************
    // Nested-Labels Distance
    // **********************************************************************************

    public static double computeNestedLabelsDistance(List<PhyloTree> trees) {

        HashSet<String> n1MultiSet = collectNestedLabels(trees.get(0));
        HashSet<String> n2MultiSet = collectNestedLabels(trees.get(1));

        HashSet<String> diffLabels = new HashSet<String>();
        for (String nL : n1MultiSet) {
            if (!n2MultiSet.contains(nL))
                diffLabels.add(nL);
        }
        for (String nL : n2MultiSet) {
            if (!n1MultiSet.contains(nL))
                diffLabels.add(nL);
        }

        return ((double) diffLabels.size()) / 2;
    }

    private static final Hashtable<Node, String> nodeToLabel = new Hashtable<Node, String>();

    private static HashSet<String> collectNestedLabels(PhyloTree t) {

        nodeToLabel.clear();
        for (Node v : t.computeSetOfLeaves())
            nodeToLabel.put(v, t.getLabel(v));

        computeNestedLabels(t.getRoot());

        HashSet<String> multiSet = new HashSet<String>();
        for (String s : nodeToLabel.values())
            multiSet.add(s);

        return multiSet;
    }

    private static String computeNestedLabels(Node v) {
        if (!nodeToLabel.containsKey(v)) {

            Iterator<Edge> it = v.getOutEdges();
            Vector<String> childLabels = new Vector<String>();
            while (it.hasNext()) {
                Node c = it.next().getTarget();
                if (nodeToLabel.containsKey(c))
                    childLabels.add(nodeToLabel.get(c));
                else
                    childLabels.add(computeNestedLabels(c));
            }

            String label = "{";
            Collections.sort(childLabels);
            for (int i = 0; i < childLabels.size() - 1; i++)
                label = label.concat(childLabels.get(i) + ",");
            label = label.concat(childLabels.get(childLabels.size() - 1) + "}");

            nodeToLabel.put(v, label);
            return label;
        } else
            return nodeToLabel.get(v);
    }

    // **********************************************************************************
    // Tripartition Distance
    // **********************************************************************************

    public static double computeTripartitionDistance(List<PhyloTree> trees) {

        Vector<String> taxa = new Vector<String>();
        for (Node v : trees.get(0).computeSetOfLeaves())
            taxa.add(trees.get(0).getLabel(v));

        HashSet<String> n1Tripartitions = collectTripartitions(trees.get(0),
                taxa);
        HashSet<String> n2Tripartitions = collectTripartitions(trees.get(1),
                taxa);

        HashSet<String> diffTripartitions = new HashSet<String>();
        for (String trip : n1Tripartitions) {
            if (!n2Tripartitions.contains(trip))
                diffTripartitions.add(trip);
        }
        for (String trip : n2Tripartitions) {
            if (!n1Tripartitions.contains(trip))
                diffTripartitions.add(trip);
        }

        return ((double) diffTripartitions.size()) / 2;
    }

    private static final Hashtable<String, HashSet<HashSet<Edge>>> taxonToPaths = new Hashtable<String, HashSet<HashSet<Edge>>>();

    @SuppressWarnings("unchecked")
    private static HashSet<String> collectTripartitions(PhyloTree t,
                                                        Vector<String> taxa) {

        HashSet<String> tripartitions = new HashSet<String>();
        taxonToPaths.clear();

        initPathTable(t.getRoot(), new HashSet<Edge>(), t);

        Iterator<Edge> it = t.edgeIterator();
        while (it.hasNext()) {
            Edge e = it.next();
            if (!t.isSpecial(e) && e.getTarget().getOutDegree() != 0) {
                Vector<String> setA = new Vector<String>();
                Vector<String> setB = new Vector<String>();
                Vector<String> setC = (Vector<String>) taxa.clone();
                for (String taxon : taxonToPaths.keySet()) {
                    boolean b = false;
                    for (HashSet<Edge> path : taxonToPaths.get(taxon)) {
                        if (path.contains(e)) {
                            if (setC.contains(taxon)) {
                                setC.remove(taxon);
                                setA.add(taxon);
                            }
                        } else
                            b = true;
                    }
                    if (setA.contains(taxon) && b) {
                        setA.remove(taxon);
                        setB.add(taxon);
                    }
                }

                String trip = "(";
                if (setA.size() != 0) {
                    Collections.sort(setA);
                    trip = trip.concat("{");
                    for (int i = 0; i < setA.size() - 1; i++)
                        trip = trip.concat(setA.get(i) + ",");
                    trip = trip.concat(setA.get(setA.size() - 1) + "}");
                } else
                    trip = trip.concat("{}");
                if (setB.size() != 0) {
                    Collections.sort(setB);
                    trip = trip.concat("{");
                    for (int i = 0; i < setB.size() - 1; i++)
                        trip = trip.concat(setB.get(i) + ",");
                    trip = trip.concat(setB.get(setB.size() - 1) + "}");
                } else
                    trip = trip.concat("{}");
                if (setC.size() != 0) {
                    Collections.sort(setC);
                    trip = trip.concat("{");
                    for (int i = 0; i < setC.size() - 1; i++)
                        trip = trip.concat(setC.get(i) + ",");
                    trip = trip.concat(setC.get(setC.size() - 1) + "}");
                } else
                    trip = trip.concat("{}");
                trip = trip.concat(")");

                tripartitions.add(trip);
            }
        }

        return tripartitions;
    }

    @SuppressWarnings("unchecked")
    private static void initPathTable(Node v, HashSet<Edge> path, PhyloTree t) {
        if (v.getOutDegree() != 0) {
            Iterator<Edge> it = v.getOutEdges();
            while (it.hasNext()) {
                Edge e = it.next();
                HashSet<Edge> pathCopy = (HashSet<Edge>) path.clone();
                if (!t.isSpecial(e) && e.getTarget().getOutDegree() != 0)
                    pathCopy.add(e);
                initPathTable(e.getTarget(), pathCopy, t);
            }
        } else {
            String taxon = t.getLabel(v);
            if (!taxonToPaths.containsKey(taxon)) {
                HashSet<HashSet<Edge>> set = new HashSet<HashSet<Edge>>();
                set.add(path);
                taxonToPaths.put(taxon, set);
            } else {
                HashSet<HashSet<Edge>> set = (HashSet<HashSet<Edge>>) taxonToPaths
                        .get(taxon).clone();
                taxonToPaths.remove(taxon);
                if (!set.contains(path))
                    set.add(path);
                taxonToPaths.put(taxon, set);
            }
        }
    }

    // **********************************************************************************
    // Displayed Trees Distance
    // **********************************************************************************

    public static double computeDisplayedTreesDistance(List<PhyloTree> trees) {

        HashSet<PhyloTree> n1DisplayesTrees = collectDisplayedTrees(trees
                .get(0));
        HashSet<PhyloTree> n2DisplayesTrees = collectDisplayedTrees(trees
                .get(1));

        Set<String> n1TreeStrings = computeTreeStrings(n1DisplayesTrees);
        Set<String> n2TreeStrings = computeTreeStrings(n2DisplayesTrees);

        HashSet<String> diffTrees = new HashSet<String>();
        for (String s1 : n1TreeStrings) {
            if (!n2TreeStrings.contains(s1))
                diffTrees.add(s1);
        }
        for (String s2 : n2TreeStrings) {
            if (!n1TreeStrings.contains(s2))
                diffTrees.add(s2);
        }

        return ((double) diffTrees.size()) / 2;
    }

    private static Set<String> computeTreeStrings(HashSet<PhyloTree> trees) {
        Set<String> treeStrings = new HashSet<String>();
        for (PhyloTree t : trees) {
            Set<Set<String>> clusters = PhyloTreeUtils.collectAllHardwiredClusters(t);
            treeStrings.add(clusters.toString());
        }
        return treeStrings;
    }

    private static final HashSet<PhyloTree> displayedTrees = new HashSet<PhyloTree>();

    @SuppressWarnings("unchecked")
    private static HashSet<PhyloTree> collectDisplayedTrees(PhyloTree t) {

        displayedTrees.clear();

        int id = 0;
        for (Node v : t.getNodes()) {
            if (v.getInDegree() > 1) {
                v.setInfo(id);
                id++;
                Iterator<Edge> it = v.getInEdges();
                for (int i = 0; i < v.getInDegree(); i++)
                    it.next().setInfo(i);
            }
        }

        try {
            computeTrees(0, t);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return (HashSet<PhyloTree>) displayedTrees.clone();
    }

    private static void computeTrees(int id, PhyloTree t) throws IOException {

        Node r = getNodeFromID(t, id);

        if (r != null) {
            int inDegree = r.getInDegree();
            for (int i = 0; i < inDegree; i++) {

                PhyloTree tCopy = new PhyloTree();
                tCopy.copy(t);
                for (Node v : tCopy.getNodes()) {
                    if (v.getInDegree() == 0)
                        tCopy.setRoot(v);
                }

                Node rCopy = getNodeFromID(tCopy, id);
                Edge treeEdge = null;

                Iterator<Edge> it = rCopy.getInEdges();
                while (it.hasNext()) {
                    Edge e = it.next();
                    if (!e.getInfo().equals(i))
                        tCopy.deleteEdge(e);
                    else
                        treeEdge = e;
                }

                if (treeEdge != null) {
                    tCopy.setSpecial(treeEdge, false);
                    tCopy.setWeight(treeEdge, 1.0);
                }
                int newID = id + 1;
                computeTrees(newID, tCopy);

            }
        } else {
            refineTree(t);
            displayedTrees.add(t);
        }

    }

    private static Node getNodeFromID(PhyloTree t, int id) {
        for (Node v : t.getNodes()) {
            if (v.getInfo() != null && v.getInfo().equals(id))
                return v;
        }
        return null;
    }

    private static void refineTree(PhyloTree t) {
        for (Node v : t.getNodes()) {
            if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
                removeNode(t, v);
                refineTree(t);
                break;
            } else if (v.getInDegree() == 1 && v.getOutDegree() == 0
                    && t.getLabel(v) == null) {
                removeNode(t, v);
                refineTree(t);
                break;
            } else if (v.getInDegree() == 0 && v.getOutDegree() == 1
                    && t.getLabel(v) == null) {
                removeNode(t, v);
                refineTree(t);
                break;
            }
        }
    }

    private static void removeNode(PhyloTree t, Node v) {
        if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
            Node v1 = v.getInEdges().next().getSource();
            Node v2 = v.getOutEdges().next().getTarget();
            t.deleteEdge(v.getInEdges().next());
            t.deleteEdge(v.getOutEdges().next());
            t.deleteNode(v);
            t.newEdge(v1, v2);
        } else if (v.getInDegree() == 1 && v.getOutDegree() == 0
                && t.getLabel(v) == null) {
            Node v1 = v.getInEdges().next().getSource();
            t.deleteEdge(v.getInEdges().next());
            t.deleteNode(v);
            removeNode(t, v1);
        } else if (v.getInDegree() == 0 && v.getOutDegree() == 1
                && t.getLabel(v) == null) {
            Node c = v.getOutEdges().next().getTarget();
            t.deleteEdge(v.getOutEdges().next());
            t.deleteNode(v);
            t.setRoot(c);
            removeNode(t, v);
        }

    }

    // **********************************************************************************
    // Softwired Distance
    // **********************************************************************************

    public static double computeSoftwiredDistance(List<PhyloTree> trees) {

        HashSet<PhyloTree> n1DisplayesTrees = collectDisplayedTrees(trees
                .get(0));
        HashSet<PhyloTree> n2DisplayesTrees = collectDisplayedTrees(trees
                .get(1));

        HashSet<String> n1Clusters = new HashSet<String>();
        for (PhyloTree t : n1DisplayesTrees) {
            Set<Set<String>> clusters = PhyloTreeUtils
                    .collectAllHardwiredClusters(t);
            for (Set<String> o : clusters)
                n1Clusters.add(o.toString());
        }

        HashSet<String> n2Clusters = new HashSet<String>();
        for (PhyloTree t : n2DisplayesTrees) {
            Set<Set<String>> clusters = PhyloTreeUtils.collectAllHardwiredClusters(t);
            for (Set<String> o : clusters)
                n2Clusters.add(o.toString());
        }

        HashSet<String> diffClusters = new HashSet<String>();
        for (String c : n1Clusters) {
            if (!n2Clusters.contains(c))
                diffClusters.add(c);
        }
        for (String c : n2Clusters) {
            if (!n1Clusters.contains(c))
                diffClusters.add(c);
        }

        return ((double) diffClusters.size()) / 2;
    }

    // **********************************************************************************
    // Subnetwork Distance
    // **********************************************************************************

    public static double computeSubnetworkDistance(List<PhyloTree> trees) {

        HashSet<PhyloTree> n1Subnetworks = collectSubnetworks(trees.get(0));
        HashSet<PhyloTree> n2Subnetworkst = collectSubnetworks(trees.get(1));

        HashSet<String> n1IsoStrings = computeIsoStrings(n1Subnetworks);
        HashSet<String> n2IsoStrings = computeIsoStrings(n2Subnetworkst);

        HashSet<String> diffIsoStrings = new HashSet<String>();
        for (String s1 : n1IsoStrings) {
            if (!n2IsoStrings.contains(s1))
                diffIsoStrings.add(s1);
        }
        for (String s2 : n2IsoStrings) {
            if (!n1IsoStrings.contains(s2))
                diffIsoStrings.add(s2);
        }
        return ((double) diffIsoStrings.size()) / 2;
    }

    private static final Hashtable<Node, Node> nodeToCopy = new Hashtable<Node, Node>();

    public static HashSet<PhyloTree> collectSubnetworks(PhyloTree t) {
        HashSet<PhyloTree> multiSet = new HashSet<PhyloTree>();
        for (Node v : t.getNodes()) {

            PhyloTree subnet = new PhyloTree();
            nodeToCopy.clear();

            Node vCopy = subnet.newNode(v);
            subnet.setLabel(vCopy, t.getLabel(v));
            nodeToCopy.put(v, vCopy);
            subnet.setRoot(vCopy);

            computeSubnetwork(v, t, vCopy, subnet);

            refineTree(subnet);
            multiSet.add(subnet);
        }
        return multiSet;
    }

    private static void computeSubnetwork(Node v, PhyloTree t, Node vCopy,
                                          PhyloTree subnet) {
        Iterator<Edge> it = v.getOutEdges();
        while (it.hasNext()) {
            Node c = it.next().getTarget();
            if (nodeToCopy.containsKey(c)) {
                Node cCopy = nodeToCopy.get(c);
                subnet.newEdge(vCopy, cCopy);
                Iterator<Edge> itChild = cCopy.getInEdges();
                while (itChild.hasNext()) {
                    Edge e = itChild.next();
                    subnet.setSpecial(e, true);
                    subnet.setWeight(e, 0.0);
                }
            } else {
                Node cCopy = subnet.newNode(c);
                subnet.setLabel(cCopy, t.getLabel(c));
                subnet.newEdge(vCopy, cCopy);
                nodeToCopy.put(c, cCopy);
                computeSubnetwork(c, t, cCopy, subnet);
            }
        }

    }

    private static HashSet<String> computeIsoStrings(HashSet<PhyloTree> trees) {
        HashSet<String> isoStrings = new HashSet<String>();
        for (PhyloTree t : trees)
            isoStrings.add(new IsomorphismCheck().getIsoString(t));
        return isoStrings;
    }

    // **********************************************************************************
    // Galled tree - Galled network - Level-k network
    // **********************************************************************************

    private static boolean isGalledTree;
    private static boolean isGalledNetwork;

    private static Hashtable<Node, HashSet<BitSet>> retToBiComponents;
    private static Vector<Edge> edgeOrder;

    public static Object[] computeTreeDefinitions(PhyloTree tree) {

        isGalledTree = true;
        isGalledNetwork = true;
        retToBiComponents = new Hashtable<Node, HashSet<BitSet>>();

        edgeOrder = new Vector<Edge>();
        Iterator<Edge> it = tree.edgeIterator();
        while (it.hasNext())
            edgeOrder.add(it.next());

        for (Node v : tree.getNodes()) {
            if (v.getInDegree() > 1)
                collectBiComponents(v);
        }

        int levelK = 1;
        for (Node r1 : retToBiComponents.keySet()) {
            int k = 1;
            for (Node r2 : retToBiComponents.keySet()) {
                boolean isContained = false;
                if (!r1.equals(r2)) {
                    for (BitSet b1 : retToBiComponents.get(r1)) {
                        for (BitSet b2 : retToBiComponents.get(r2)) {
                            BitSet b = (BitSet) b1.clone();
                            b.and(b2);
                            if (b.cardinality() > 0) {
                                isGalledTree = false;
                                isContained = true;
                                break;
                            }
                        }
                        if (isContained)
                            break;
                    }
                }
                if (isContained)
                    k++;
            }
            if (k > levelK)
                levelK = k;
        }

        Object[] definitions = {isGalledTree, isGalledNetwork, levelK};
        return definitions;
    }

    private static void collectBiComponents(Node v) {
        Vector<Edge> inEdges = new Vector<Edge>();
        Iterator<Edge> it = v.getInEdges();
        while (it.hasNext())
            inEdges.add(it.next());
        for (int i = 0; i < inEdges.size() - 1; i++) {
            for (int j = i + 1; j < inEdges.size(); j++)
                computeBiComponentsStepOne(new Vector<Edge>(), inEdges.get(i),
                        inEdges.get(j), v);
        }
    }

    private static void computeBiComponentsStepOne(Vector<Edge> e1Edges,
                                                   Edge e1, Edge e2, Node v) {
        e1Edges.add(e1);
        Node s = e1.getSource();
        if (s.getInDegree() == 0)
            computeBiComponentsStepTwo(e1Edges, e2, new HashSet<Edge>(), v);
        else if (s.getInDegree() > 1) {
            isGalledTree = false;
            isGalledNetwork = false;
            Iterator<Edge> it = s.getInEdges();
            while (it.hasNext()) {
                Vector<Edge> e1EdgesCopy = (Vector<Edge>) e1Edges.clone();
                computeBiComponentsStepOne(e1EdgesCopy, it.next(), e2, v);
            }
        } else
            computeBiComponentsStepOne(e1Edges, s.getInEdges().next(), e2, v);
    }

    private static void computeBiComponentsStepTwo(Vector<Edge> e1Edges,
                                                   Edge e2, HashSet<Edge> e2Edges, Node v) {
        if (!e1Edges.contains(e2)) {
            e2Edges.add(e2);
            Node s = e2.getSource();
            if (s.getInDegree() == 0) {
                HashSet<Edge> biComponent = new HashSet<Edge>();
                biComponent.addAll(e1Edges);
                biComponent.addAll(e2Edges);
                putRetToComponent(v, biComponent);
            } else if (s.getInDegree() > 1) {
                isGalledTree = false;
                isGalledNetwork = false;
                Iterator<Edge> it = s.getInEdges();
                while (it.hasNext()) {
                    HashSet<Edge> e2EdgesCopy = (HashSet<Edge>) e2Edges.clone();
                    computeBiComponentsStepTwo(e1Edges, it.next(), e2EdgesCopy,
                            v);
                }
            } else
                computeBiComponentsStepTwo(e1Edges, s.getInEdges().next(),
                        e2Edges, v);
        } else {
            int index = e1Edges.indexOf(e2);
            HashSet<Edge> biComponent = new HashSet<Edge>();
            biComponent.addAll(e2Edges);
            biComponent.addAll(e1Edges.subList(0, index));
            putRetToComponent(v, biComponent);
        }

    }

    private static void putRetToComponent(Node v, HashSet<Edge> biComponent) {
        BitSet b = new BitSet(edgeOrder.size());
        for (Edge e : biComponent)
            b.set(edgeOrder.indexOf(e));
        if (retToBiComponents.containsKey(v)) {
            HashSet<BitSet> set = retToBiComponents.get(v);
            set.add(b);
        } else {
            HashSet<BitSet> set = new HashSet<BitSet>();
            set.add(b);
            retToBiComponents.put(v, set);
        }
    }

    // **********************************************************************************
    // Tree-child Property
    // **********************************************************************************

    public static boolean hasTreeChildProperty(PhyloTree t) {
        boolean hasTreeChildProp = true;
        for (Node v : t.getNodes()) {
            if (v.getOutDegree() != 0) {
                boolean hasProp = false;
                Iterator<Edge> it = v.getOutEdges();
                while (it.hasNext()) {
                    Node c = it.next().getTarget();
                    if (c.getInDegree() == 1)
                        hasProp = true;
                }
                if (!hasProp) {
                    hasTreeChildProp = false;
                    break;
                }
            }
        }
        return hasTreeChildProp;
    }

    // **********************************************************************************
    // Tree-sibling Property
    // **********************************************************************************

    public static boolean hasTreeSiblingProperty(PhyloTree t) {
        boolean hasTreeSibProp = true;
        for (Node v : t.getNodes()) {
            if (v.getInDegree() > 1) {
                boolean hasProp = false;
                Iterator<Edge> it = v.getInEdges();
                while (it.hasNext()) {
                    Edge e1 = it.next();
                    Node p = e1.getSource();
                    Iterator<Edge> pOut = p.getOutEdges();
                    while (pOut.hasNext()) {
                        Edge e2 = pOut.next();
                        if (!e2.equals(e1)) {
                            Node s = e2.getTarget();
                            if (s.getInDegree() == 1)
                                hasProp = true;
                        }
                    }
                }
                if (!hasProp) {
                    hasTreeSibProp = false;
                    break;
                }
            }
        }
        return hasTreeSibProp;
    }

    // **********************************************************************************
    // Time-consistent Property
    // **********************************************************************************

    private static boolean isTimeConsistent;

    public static Boolean hasTimeConsistentProperty(PhyloTree tree) {

        isTimeConsistent = false;

        try {

            PhyloTree t = new PhyloTree();
            t.parseBracketNotation(tree.toBracketString(), true);

            HashSet<HashSet<Node>> retSets = new HashSet<HashSet<Node>>();
            Vector<Node> reticulations = new Vector<Node>();
            for (Node v : t.getNodes()) {
                if (v.getInDegree() > 1)
                    reticulations.add(v);
            }
            while (!reticulations.isEmpty()) {
                HashSet<Node> retSet = new HashSet<Node>();
                collectRetSet(reticulations.firstElement(), reticulations,
                        retSet);
                retSets.add(retSet);
            }

            Graph g = computeDependencyGraph(retSets, t);
            if (hasSelfCycle)
                isTimeConsistent = false;
            else
                isTimeConsistent = !hasDirectedCycle(g);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return isTimeConsistent;

    }

    private static Hashtable<Integer, Node> levelToNode;
    private static Hashtable<Node, Integer> nodeToLevel;
    private static boolean hasSelfCycle;

    private static boolean hasDirectedCycle(Graph g) {

        levelToNode = new Hashtable<Integer, Node>();
        nodeToLevel = new Hashtable<Node, Integer>();

        Node start = g.newNode();
        Iterator<Node> itNodes = g.nodeIterator();
        while (itNodes.hasNext()) {
            Node v = itNodes.next();
            if (!v.equals(start))
                g.newEdge(start, v);
        }

        initLevels(start, 0, new Vector<Node>());

        for (int i = g.getNumberOfNodes() - 1; i >= 0; i--) {
            Node v = levelToNode.get(i);
            Iterator<Edge> it = v.getInEdges();
            while (it.hasNext()) {
                Node w = it.next().getSource();
                if (nodeToLevel.get(w) <= i)
                    return true;
            }
        }

        return false;
    }

    private static int initLevels(Node v, int level, Vector<Node> visited) {
        visited.add(v);
        Iterator<Edge> it = v.getOutEdges();
        int value = level;
        while (it.hasNext()) {
            Node w = it.next().getTarget();
            if (!visited.contains(w))
                value = initLevels(w, value, visited) + 1;
        }
        levelToNode.put(value, v);
        nodeToLevel.put(v, value);
        return value;
    }

    private static Graph computeDependencyGraph(HashSet<HashSet<Node>> retSets,
                                                PhyloTree t) {
        Graph g = new Graph();

        int id = 1;
        Hashtable<Integer, HashSet<Node>> idToRetSet = new Hashtable<Integer, HashSet<Node>>();
        Hashtable<HashSet<Node>, Node> retSetToNode = new Hashtable<HashSet<Node>, Node>();
        for (HashSet<Node> retSet : retSets) {
            Node v = g.newNode();
            retSetToNode.put(retSet, v);
            idToRetSet.put(id, retSet);
            for (Node r : retSet)
                r.setInfo(id);
            id++;
        }

        createEdges(t.getRoot(), g, idToRetSet, retSetToNode, (Integer) t
                .getRoot().getInfo());

        return g;
    }

    private static void createEdges(Node v, Graph g,
                                    Hashtable<Integer, HashSet<Node>> idToRetSet,
                                    Hashtable<HashSet<Node>, Node> retSetToNode, Integer lastID) {
        Iterator<Edge> it = v.getOutEdges();
        while (it.hasNext()) {
            Node c = it.next().getTarget();
            if (c.getInfo() != null) {
                if (lastID != null) {
                    Node v1 = retSetToNode.get(idToRetSet.get(lastID));
                    Node v2 = retSetToNode.get(idToRetSet.get(c.getInfo()));
                    if (c.getInDegree() == 1) {
                        if (!v1.equals(v2) && g.findDirectedEdge(v1, v2) == null)
                            g.newEdge(v1, v2);
                        else if (v1.equals(v2))
                            hasSelfCycle = true;
                    }
                }
                createEdges(c, g, idToRetSet, retSetToNode,
                        (Integer) c.getInfo());
            } else
                createEdges(c, g, idToRetSet, retSetToNode, lastID);
        }
    }

    private static void collectRetSet(Node v, Vector<Node> reticulations,
                                      HashSet<Node> reticulationSet) {
        if (v.getInDegree() > 1) {
            reticulations.remove(v);
            reticulationSet.add(v);
            Iterator<Edge> it = v.getInEdges();
            while (it.hasNext()) {
                Node p = it.next().getSource();
                reticulationSet.add(p);
                collectRetSet(p, reticulations, reticulationSet);
                Iterator<Edge> pIt = p.getOutEdges();
                while (pIt.hasNext()) {
                    Node c = pIt.next().getTarget();
                    if (!c.equals(v))
                        collectRetSet(c, reticulations, reticulationSet);
                }
            }
            it = v.getOutEdges();
            while (it.hasNext()) {
                Node c = it.next().getTarget();
                collectRetSet(c, reticulations, reticulationSet);
            }
        }
    }

}
