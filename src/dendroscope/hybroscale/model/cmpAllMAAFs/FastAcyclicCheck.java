/*
 *   FastAcyclicCheck.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.cmpAllMAAFs;

import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyGraph;
import dendroscope.hybroscale.util.graph.MyNode;

import java.util.*;

public class FastAcyclicCheck {

	private Vector<String> taxaOrdering;

	private Hashtable<EasyTree, HashSet<EasyTree>> compToUpper = new Hashtable<EasyTree, HashSet<EasyTree>>();
	private Hashtable<Integer, MyNode> levelToNode = new Hashtable<Integer, MyNode>();
	private Hashtable<MyNode, Integer> nodeToLevel = new Hashtable<MyNode, Integer>();

	private Hashtable<EasyTree, MyNode> compToNode = new Hashtable<EasyTree, MyNode>();
	private Hashtable<MyNode, EasyTree> nodeToComp = new Hashtable<MyNode, EasyTree>();
	private Vector<EasyTree> acyclicOrder = new Vector<EasyTree>();

	public Vector<EasyTree> run(Vector<EasyTree> forest, EasyTree t1, EasyTree t2, Vector<String> taxaOrdering,
								EasyTree comp, boolean debug, boolean checkCycle) {

		this.taxaOrdering = taxaOrdering;

		Hashtable<EasyNode, BitSet> t1NodeToCluster = new Hashtable<EasyNode, BitSet>();
		Hashtable<EasyNode, BitSet> t2NodeToCluster = new Hashtable<EasyNode, BitSet>();

		moveRootTreeToBack(forest);

		initCluster(t1, t1NodeToCluster);
		if (t2 != null)
			initCluster(t2, t2NodeToCluster);
		MyGraph g = new MyGraph();

		for (EasyTree t : forest) {
			if (t.getNodes().size() != 1) {
				MyNode v = g.newNode();
				g.setInfo(v, t.getRoot().getLabel());
				compToNode.put(t, v);
				nodeToComp.put(v, t);
			}
		}

		if (g.getNodes().size() == 1) {

			for (EasyTree c : forest)
				acyclicOrder.add(c);

			return acyclicOrder;
		}

		addEdges(t1, g, t1NodeToCluster, forest, debug);
		if (t2 != null)
			addEdges(t2, g, t2NodeToCluster, forest, debug);

		boolean isAcyclic = !checkCycle || !hasCycle(forest, g);

		if (isAcyclic) {

			compToUpper.put(forest.lastElement(), new HashSet<EasyTree>());
			acyclicOrder.add(forest.lastElement());
			Vector<MyNode> border = new Vector<MyNode>();
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

	private boolean hasCycle;

	public boolean checkCycle(Vector<EasyTree> forest, EasyTree t1, EasyTree t2, Vector<String> taxaOrdering,
							  EasyTree comp, boolean debug) {

		hasCycle = false;
		this.taxaOrdering = taxaOrdering;
		Hashtable<EasyNode, BitSet> t1NodeToCluster = new Hashtable<EasyNode, BitSet>();
		Hashtable<EasyNode, BitSet> t2NodeToCluster = new Hashtable<EasyNode, BitSet>();

		moveRootTreeToBack(forest);

		initCluster(t1, t1NodeToCluster);
		initCluster(t2, t2NodeToCluster);
		MyGraph g = new MyGraph();

		for (EasyTree t : forest) {
			if (t.getNodes().size() != 1) {
				MyNode v = g.newNode();
				g.setInfo(v, t.getRoot().getLabel());
				compToNode.put(t, v);
				nodeToComp.put(v, t);
			}
		}

		addEdges(t1, g, t1NodeToCluster, forest, debug);
		addEdges(t2, g, t2NodeToCluster, forest, debug);

		MyNode start = compToNode.get(comp);
		checkCycleRec(g, start, start, new Vector<MyEdge>());

		return hasCycle;
	}

	public void checkCycleRec(MyGraph g, MyNode v, MyNode start, Vector<MyEdge> visited) {
		Iterator<MyEdge> it = v.outEdges().iterator();
		while (it.hasNext()) {
			MyEdge e = it.next();
			MyNode w = e.getTarget();
			if (!visited.contains(e)) {
				visited.add(e);
				if (w.equals(start))
					hasCycle = true;
				else if (!hasCycle)
					checkCycleRec(g, w, start, visited);
			}
		}
	}

	private boolean hasCycle(Vector<EasyTree> forest, MyGraph g) {

		// init levels
		MyNode start = compToNode.get(forest.lastElement());
		initLevels(start, 0, new Vector<MyNode>());

		// search for scc
		for (int i = g.getNumberOfNodes() - 1; i >= 0; i--) {
			MyNode v = levelToNode.get(i);
			Iterator<MyEdge> it = v.inEdges().iterator();
			while (it.hasNext()) {
				MyNode w = it.next().getSource();
				if (nodeToLevel.get(w) < i)
					return true;
			}
		}
		return false;
	}

	private int initLevels(MyNode v, int level, Vector<MyNode> visited) {
		visited.add(v);
		Iterator<MyEdge> it = v.outEdges().iterator();
		int value = level;
		while (it.hasNext()) {
			MyNode w = it.next().getTarget();
			if (!visited.contains(w))
				value = initLevels(w, value, visited) + 1;
		}
		levelToNode.put(value, v);
		nodeToLevel.put(v, value);
		return value;
	}

	private void computeOrder(Vector<MyNode> border) {
		Vector<MyNode> newBorder = new Vector<MyNode>();
		for (MyNode v : border) {
			Iterator<MyEdge> it = v.outEdges().iterator();
			while (it.hasNext()) {
				MyNode w = it.next().getTarget();
				if (!acyclicOrder.contains(nodeToComp.get(w))) {
					acyclicOrder.add(0, nodeToComp.get(w));
					newBorder.add(w);
				} else
					changeOrder(v, w);
				HashSet<EasyTree> upperNodes = cloneSet(compToUpper.get(nodeToComp.get(v)));
				upperNodes.add(nodeToComp.get(v));
				if (compToUpper.containsKey(nodeToComp.get(w)))
					upperNodes.addAll(compToUpper.get(nodeToComp.get(w)));
				compToUpper.put(nodeToComp.get(w), upperNodes);
			}
		}
		if (newBorder.size() != 0)
			computeOrder(newBorder);
	}

	private void changeOrder(MyNode v, MyNode w) {
		int posV = acyclicOrder.indexOf(nodeToComp.get(v));
		int posW = acyclicOrder.indexOf(nodeToComp.get(w));
		if (posV < posW) {
			for (int i = posV + 1; i < posW; i++) {
				EasyTree comp = acyclicOrder.get(i);
				HashSet<EasyTree> upperComp = compToUpper.get(comp);
				if (upperComp.contains(nodeToComp.get(w))) {
					acyclicOrder.remove(comp);
					acyclicOrder.add(posV, comp);
					posV++;
				}
			}
			acyclicOrder.remove(nodeToComp.get(w));
			acyclicOrder.add(posV, nodeToComp.get(w));
		}
	}

	private HashSet<EasyTree> cloneSet(HashSet<EasyTree> vec) {
		HashSet<EasyTree> newVec = new HashSet<EasyTree>();
		for (EasyTree t : vec)
			newVec.add(t);
		return newVec;
	}

	private Vector<EasyTree> cloneVector(Vector<EasyTree> vec) {
		Vector<EasyTree> newVec = new Vector<EasyTree>();
		for (EasyTree t : vec)
			newVec.add(t);
		return newVec;
	}

	private void addEdges(EasyTree t, MyGraph g, Hashtable<EasyNode, BitSet> tNodeToCluster, Vector<EasyTree> forest2,
						  boolean debug) {

		Hashtable<EasyTree, BitSet> compToCluster = new Hashtable<EasyTree, BitSet>();
		Vector<EasyTree> forest = new Vector<EasyTree>();

		for (MyNode v : nodeToComp.keySet()) {
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

		for (int i = 0; i < forest.size(); i++) {
			EasyTree f1 = forest.get(i);
			BitSet b1 = compToCluster.get(f1);
			for (int j = i + 1; j < forest.size(); j++) {
				EasyTree f2 = forest.get(j);
				BitSet b2 = compToCluster.get(f2);
				if (!b1.equals(b2)) {
					BitSet b = (BitSet) b1.clone();
					b.or(b2);
					if (b.equals(b1)) {
						addEdge(compToNode.get(f1), compToNode.get(f2), g);
						// System.out.println(f1.getPhyloTree()+" -> "+f2.getPhyloTree());
					} else {
						b.clear();
						b = (BitSet) b2.clone();
						b.or(b1);
						if (b.equals(b2)) {
							addEdge(compToNode.get(f2), compToNode.get(f1), g);
							// System.out.println(f2.getPhyloTree()+" -> "+f1.getPhyloTree());
						}
					}
				}
			}
		}
	}

	private void addEdge(MyNode v1, MyNode v2, MyGraph g) {
		Iterator<MyEdge> it = v1.outEdges().iterator();
		boolean add = true;
		while (it.hasNext()) {
			MyNode v = it.next().getTarget();
			if (v.equals(v2))
				add = false;
		}
		if (add)
			g.newEdge(v1, v2);
	}

	private EasyNode findLCA(EasyTree t, BitSet cluster, Hashtable<EasyNode, BitSet> nodeToCluster) {
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

	private void initCluster(EasyTree t, Hashtable<EasyNode, BitSet> nodeToCluster) {
		initClusterRec(t.getRoot(), new Vector<String>(), nodeToCluster);
	}

	private Vector<String> initClusterRec(EasyNode v, Vector<String> taxa, Hashtable<EasyNode, BitSet> nodeToCluster) {
		BitSet b = new BitSet(taxaOrdering.size());
		if (v.getOutDegree() == 0)
			taxa.add(v.getLabel());
		else {
			for (EasyNode c : v.getChildren())
				taxa.addAll(initClusterRec(c, new Vector<String>(), nodeToCluster));
		}
		for (String s : taxa)
			b.set(taxaOrdering.indexOf(s));
		nodeToCluster.put(v, b);
		return taxa;
	}

	private BitSet getRootCluster(EasyTree t) {
		BitSet cluster = new BitSet(taxaOrdering.size());
		// for (EasyNode v : t.getLeaves()){
		// cluster.set(taxaOrdering.indexOf(v.getLabel()));
		// }
		for (EasyNode v : t.getNodes()) {
			if (v.getOutDegree() == 0)
				cluster.set(taxaOrdering.indexOf(v.getLabel()));
		}
		return cluster;
	}

	private void moveRootTreeToBack(Vector<EasyTree> forest) {
		int index = 0;
		for (EasyTree t : forest) {
			for (EasyNode v : t.getLeaves()) {
				if (v.getLabel().equals("rho")) {
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
