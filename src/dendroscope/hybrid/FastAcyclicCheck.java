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

package dendroscope.hybrid;

import jloda.graph.Edge;
import jloda.graph.Graph;
import jloda.graph.Node;

import java.util.*;

public class FastAcyclicCheck {

    private Vector<String> taxaOrdering;

    private final Hashtable<EasyTree, Vector<EasyTree>> compToUpper = new Hashtable<>();
    private final Hashtable<Integer, Node> levelToNode = new Hashtable<>();
    private final Hashtable<Node, Integer> nodeToLevel = new Hashtable<>();

    private final Hashtable<EasyTree, Node> compToNode = new Hashtable<>();
    private final Hashtable<Node, EasyTree> nodeToComp = new Hashtable<>();
    private final Vector<EasyTree> acyclicOrder = new Vector<>();

    public Vector<EasyTree> run(Vector<EasyTree> forest, EasyTree t1,
                                EasyTree t2, Vector<String> taxaOrdering, EasyTree comp,
                                boolean debug) {

        this.taxaOrdering = taxaOrdering;
        Hashtable<EasyNode, BitSet> t1NodeToCluster = new Hashtable<>();
        Hashtable<EasyNode, BitSet> t2NodeToCluster = new Hashtable<>();

        moveRootTreeToBack(forest);

        initCluster(t1, t1NodeToCluster);
        initCluster(t2, t2NodeToCluster);
        Graph g = new Graph();

        for (EasyTree t : forest) {
            if (t.getNodes().size() != 1) {
                Node v = g.newNode();
                g.setInfo(v, t.getRoot().getLabel());
                compToNode.put(t, v);
                nodeToComp.put(v, t);
            }
        }

        addEdges(t1, g, t1NodeToCluster, comp, forest, debug);
        addEdges(t2, g, t2NodeToCluster, comp, forest, debug);

        boolean isAcyclic = !hasCycle(forest, g);

        if (isAcyclic) {

            compToUpper.put(forest.lastElement(), new Vector<EasyTree>());
            acyclicOrder.add(forest.lastElement());
            Vector<Node> border = new Vector<>();
            border.add(compToNode.get(forest.lastElement()));

            computeOrder(border);

            for (EasyTree f : forest) {
                if (!acyclicOrder.contains(f))
                    acyclicOrder.add(0, f);
            }

            return acyclicOrder;
        } else
            return null;

    }

    private boolean hasCycle(Vector<EasyTree> forest, Graph g) {

        // init levels
        Node start = compToNode.get(forest.lastElement());
        initLevels(start, 0, new Vector<Node>());

        // search for scc
        for (int i = g.getNumberOfNodes() - 1; i >= 0; i--) {
            Node v = levelToNode.get(i);
            Iterator<Edge> it = v.getInEdges();
            while (it.hasNext()) {
                Node w = it.next().getSource();
                if (nodeToLevel.get(w) < i)
                    return true;
            }
        }
        return false;
    }

    private int initLevels(Node v, int level, Vector<Node> visited) {
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

    private void computeOrder(Vector<Node> border) {
        Vector<Node> newBorder = new Vector<>();
        for (Node v : border) {
            Iterator<Edge> it = v.getOutEdges();
            while (it.hasNext()) {
                Node w = it.next().getTarget();
                if (!acyclicOrder.contains(nodeToComp.get(w))) {
                    acyclicOrder.add(0, nodeToComp.get(w));
                    Vector<EasyTree> upperNodes = cloneVector(compToUpper
                            .get(nodeToComp.get(v)));
                    upperNodes.add(nodeToComp.get(v));
                    compToUpper.put(nodeToComp.get(w), upperNodes);
                    newBorder.add(w);
                } else {
                    changeOrder(v, w);
                }
            }
        }
        if (newBorder.size() != 0)
            computeOrder(newBorder);
    }

    private void changeOrder(Node v, Node w) {
        int posV = acyclicOrder.indexOf(nodeToComp.get(v));
        int posW = acyclicOrder.indexOf(nodeToComp.get(w));
        if (posV < posW) {
            for (int i = posV + 1; i < posW; i++) {
                EasyTree comp = acyclicOrder.get(i);
                Vector<EasyTree> upperComp = compToUpper.get(comp);
                if (upperComp.contains(nodeToComp.get(w))) {
                    acyclicOrder.remove(comp);
                    acyclicOrder.add(posV, comp);
                }
            }
            acyclicOrder.remove(nodeToComp.get(w));
            acyclicOrder.add(posV, nodeToComp.get(w));
        }
    }

    private Vector<EasyTree> cloneVector(Vector<EasyTree> vec) {
        Vector<EasyTree> newVec = new Vector<>();
        for (EasyTree t : vec)
            newVec.add(t);
        return newVec;
    }

    private void addOrderedEdges(Vector<EasyTree> forest, EasyTree t,
                                 Hashtable<EasyNode, BitSet> nodeToCluster, Graph g) {
        Hashtable<EasyTree, Integer> componentToDepth = new Hashtable<>();
        for (EasyTree f : forest) {
            EasyNode lca = findLCA(t, getRootCluster(f), nodeToCluster);
            componentToDepth.put(f, getDepth(lca));
        }
        Collections.sort(forest, new ForestComparator(componentToDepth));
        for (int i = forest.size() - 1; i > 0; i--)
            g.newEdge(compToNode.get(forest.get(i)),
                    compToNode.get(forest.get(i - 1)));
    }

    private int getDepth(EasyNode v) {
        int depth = 0;
        while (v.getInDegree() != 0) {
            v = v.getParent();
            depth++;
        }
        return depth;
    }

    private void addEdges(EasyTree t, Graph g,
                          Hashtable<EasyNode, BitSet> tNodeToCluster, EasyTree comp,
                          Vector<EasyTree> forest2, boolean debug) {

        Hashtable<EasyTree, BitSet> compToCluster = new Hashtable<>();
        Vector<EasyTree> forest = new Vector<>();

        for (Node v : nodeToComp.keySet()) {
            EasyTree f = nodeToComp.get(v);
            forest.add(f);

            BitSet fCluster = getRootCluster(f);

            // finding the node in t1 representing the root of f
            EasyNode lca = findLCA(t, fCluster, tNodeToCluster);
            if (f.getNodes().size() == 1)
                lca = lca.getParent();
            BitSet lcaCluster = tNodeToCluster.get(lca);
            compToCluster.put(f, lcaCluster);

        }

//		if (comp == null) {
        for (int i = 0; i < forest.size(); i++) {
            EasyTree f1 = forest.get(i);
            BitSet b1 = compToCluster.get(f1);
            // if(debug)
            // System.out.println(f1.getPhyloTree()+" "+b1);
            for (int j = i + 1; j < forest.size(); j++) {
                EasyTree f2 = forest.get(j);
                BitSet b2 = compToCluster.get(f2);
                // if(debug)
                // System.out.println(f2.getPhyloTree()+" "+b2);
                BitSet b = (BitSet) b1.clone();
                b.or(b2);
                // if(debug)
                // System.out.println(b);
                if (b.equals(b1))
                    addEdge(compToNode.get(f1), compToNode.get(f2), g);
                else {
                    b.clear();
                    b = (BitSet) b2.clone();
                    b.or(b1);
                    if (b.equals(b2)) {
                        addEdge(compToNode.get(f2), compToNode.get(f1), g);
                    }
                }
            }
        }
//		} 
//		else {
//			for (int i = 0; i < forest.size(); i++) {
//				EasyTree f1 = forest.get(i);
//				BitSet b1 = compToCluster.get(f1);
//				// if(debug)
//				// System.out.println(f1.getPhyloTree()+" "+b1);
//				if (f1.equals(comp) || f1.equals(forest2.lastElement())) {
//					for (int j = 0; j < forest.size(); j++) {
//						EasyTree f2 = forest.get(j);
//						if (!f1.equals(f2)) {
//							BitSet b2 = compToCluster.get(f2);
//							// if(debug)
//							// System.out.println(f2.getPhyloTree()+" "+b2);
//							BitSet b = (BitSet) b1.clone();
//							b.or(b2);
//							// if(debug)
//							// System.out.println(b);
//							if (b.equals(b1))
//								addEdge(compToNode.get(f1), compToNode.get(f2),
//										g);
//							else {
//								b.clear();
//								b = (BitSet) b2.clone();
//								b.or(b1);
//								if (b.equals(b2)) {
//									addEdge(compToNode.get(f2),
//											compToNode.get(f1), g);
//								}
//							}
//						}
//					}
//				}
//			}
//
//		}

    }

    private void addEdge(Node v1, Node v2, Graph g) {
        Iterator<Edge> it = v1.getOutEdges();
        boolean add = true;
        while (it.hasNext()) {
            Node v = it.next().getTarget();
            if (v.equals(v2))
                add = false;
        }
        if (add)
            g.newEdge(v1, v2);
    }

    private EasyNode findLCA(EasyTree t, BitSet cluster,
                             Hashtable<EasyNode, BitSet> nodeToCluster) {
        Iterator<EasyNode> it = t.postOrderWalk();
        while (it.hasNext()) {
            EasyNode v = it.next();
            BitSet b1 = nodeToCluster.get(v);
            BitSet b2 = (BitSet) cluster.clone();
            b2.and(b1);
            if (b2.equals(cluster))
                return v;
        }
        return null;
    }

    private void initCluster(EasyTree t,
                             Hashtable<EasyNode, BitSet> nodeToCluster) {
        initClusterRec(t.getRoot(), new Vector<String>(), nodeToCluster);
    }

    private Vector<String> initClusterRec(EasyNode v, Vector<String> taxa,
                                          Hashtable<EasyNode, BitSet> nodeToCluster) {
        BitSet b = new BitSet(taxaOrdering.size());
        if (v.getOutDegree() == 0)
            taxa.add(v.getLabel());
        else {
            Vector<String> v1 = initClusterRec(v.getChildren().get(0),
                    new Vector<String>(), nodeToCluster);
            Vector<String> v2 = initClusterRec(v.getChildren().get(1),
                    new Vector<String>(), nodeToCluster);
            for (String s : v1)
                taxa.add(s);
            for (String s : v2)
                taxa.add(s);
        }
        for (String s : taxa)
            b.set(taxaOrdering.indexOf(s));
        nodeToCluster.put(v, b);
        return taxa;
    }

    private BitSet getRootCluster(EasyTree t) {
        BitSet cluster = new BitSet(taxaOrdering.size());
        for (EasyNode v : t.getLeaves())
            cluster.set(taxaOrdering.indexOf(v.getLabel()));
        return cluster;
    }

    private void moveRootTreeToBack(Vector<EasyTree> forest) {

        int index = 0;
        for (EasyTree t : forest) {
            if (t.getLeaves().size() > 1) {
                Iterator<EasyNode> it = t.getRoot().getChildren().iterator();
                String l1 = it.next().getLabel();
                String l2 = it.next().getLabel();
                if (l1.equals("rho") || l2.equals("rho")) {
                    index = forest.indexOf(t);
                    break;
                }
            }
        }

        EasyTree rootTree = forest.get(index);
        forest.remove(rootTree);
        forest.add(forest.size(), rootTree);

    }
}
