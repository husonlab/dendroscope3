/*
 *   CheckAddTaxa.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.util;

import dendroscope.hybroscale.model.treeObjects.SparseNetEdge;
import dendroscope.hybroscale.model.treeObjects.SparseNetNode;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyGraph;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

import java.util.*;

public class CheckAddTaxa {

	private MyGraph g;
	private SparseNetwork n, nInput;
	private HashMap<SparseNetEdge, MyEdge> sparseEdgeToEdge;
	private HashMap<MyEdge, SparseNetEdge> horizontalEdgeToSparseEdge;
	private HashMap<SparseNetEdge, MyEdge> sparseEdgeToHorizontalEdge;;
	private HashMap<SparseNetEdge, Vector<MyEdge>> edgeToGraphEdges;
	private HashMap<SparseNetNode, HashSet<MyNode>> nodeToCriticalNodes;
	private HashMap<MyEdge, SparseNetEdge> graphEdgeToEdge;
	private int heuDegree = Integer.MAX_VALUE;

	private HashSet<SparseNetEdge> criticalEdges;
	private HashSet<MyEdge> horizontalEdges;
	private HashSet<MyEdge> trivialEdgeOrdering;
	private HashSet<MyEdge> edgeOrdering;
	private boolean checkSuccesful = false;

	public int run(MyPhyloTree network, int border, boolean storeNetworkInfo, int maxNodes) {

		heuDegree = maxNodes;

		if (network.getTimeDegree() != null) {
			int addTaxaDegree = network.getAddTaxaDegree();
			return addTaxaDegree <= border ? addTaxaDegree : -1;
		}

		sparseEdgeToEdge = new HashMap<SparseNetEdge, MyEdge>();
		horizontalEdgeToSparseEdge = new HashMap<MyEdge, SparseNetEdge>();
		sparseEdgeToHorizontalEdge = new HashMap<SparseNetEdge, MyEdge>();
		edgeToGraphEdges = new HashMap<SparseNetEdge, Vector<MyEdge>>();
		graphEdgeToEdge = new HashMap<MyEdge, SparseNetEdge>();
		n = copyNetwork(network);

		assingUniqueLabeling(n);
		nInput = new SparseNetwork(n);

		// check trivial interactions *******************************

		HashMap<SparseNetNode, HashSet<SparseNetNode>> nodeToPreds = new HashMap<SparseNetNode, HashSet<SparseNetNode>>();
		assingPreds(n.getRoot(), new HashSet<SparseNetNode>(), nodeToPreds);
		trivialEdgeOrdering = new HashSet<MyEdge>();
		// resolveTrivialInteractions(nodeToPreds);
		// border -= trivialEdgeOrdering.size();

		// check non-trivial interactions ***************************

		HashMap<SparseNetNode, MyNode> nodeToGraphNode = new HashMap<SparseNetNode, MyNode>();
		HashMap<MyNode, SparseNetNode> graphNodeToNode = new HashMap<MyNode, SparseNetNode>();
		criticalEdges = new HashSet<SparseNetEdge>();
		horizontalEdges = new HashSet<MyEdge>();
		nodeToCriticalNodes = new HashMap<SparseNetNode, HashSet<MyNode>>();

		g = initGraph(n, nodeToGraphNode, graphNodeToNode);
		// refineGraph(graphNodeToNode);

		printGraph();
		for (SparseNetEdge e : criticalEdges)
			System.out.println(e.getSource().getLabel() + "->" +
					e.getTarget().getLabel());

		if (g.getNodes().isEmpty())
			return 0;

		for (int k = 0; k <= border; k++) {

			// System.out.println(k + " -------------------------------- ");

			HashSet<MyEdge> visitedEdges = new HashSet<MyEdge>();
			Vector<MyNode> visitedNodes = new Vector<MyNode>();
			HashSet<MyEdge> countingEdges = new HashSet<MyEdge>();
			Vector<MyNode> nextNodes = getNextNodes(visitedNodes, visitedEdges, graphNodeToNode);
			for (MyNode nextNode : nextNodes) {
				int newMaxK = k;
				checkConsistency(nextNode, newMaxK, (Vector<MyNode>) visitedNodes.clone(),
						(HashSet<MyEdge>) visitedEdges.clone(), (HashSet<MyEdge>) countingEdges.clone(),
						graphNodeToNode);
				if (checkSuccesful) {
					if (storeNetworkInfo) {
						network.setAddTaxaDegree(k);
						for (MyEdge e : trivialEdgeOrdering)
							e.setCritical(true);
						for (MyEdge e : edgeOrdering) {
							e.setCritical(true);
						}
					}
					return k;
				}
			}
		}

		// ********************************************************

		return -1;
	}

	private void checkConsistency(MyNode v, int maxK, Vector<MyNode> visitedNodes, HashSet<MyEdge> visitedEdges,
								  HashSet<MyEdge> countingEdges, HashMap<MyNode, SparseNetNode> graphNodeToNode) {
		if (getCommonDegree(v, visitedEdges) == 0)
			visitedNodes.add(v);
		// int weight = getNodeWeight(v, graphNodeToNode, countingEdges,
		// visitedEdges);
		int weight = getWeight(v, countingEdges, visitedEdges, visitedNodes, graphNodeToNode);
		maxK = maxK - weight;
		if (maxK >= 0 && visitedNodes.size() == g.getNodes().size()) {
			edgeOrdering = countingEdges;
			checkSuccesful = true;
		} else if (maxK >= 0 && !checkSuccesful) {
			// Iterator<MyEdge> it = v.outEdges().iterator();
			// while (it.hasNext()) {
			// MyEdge e = it.next();
			// visitedEdges.add(e);
			// }
			Vector<MyNode> nextNodes = getNextNodes(visitedNodes, visitedEdges, graphNodeToNode);
			for (MyNode nextNode : nextNodes) {
				int newMaxK = maxK;
				checkConsistency(nextNode, newMaxK, (Vector<MyNode>) visitedNodes.clone(),
						(HashSet<MyEdge>) visitedEdges.clone(), (HashSet<MyEdge>) countingEdges.clone(),
						graphNodeToNode);
			}
		}
	}

	private int getWeight(MyNode vG, HashSet<MyEdge> countingEdges, HashSet<MyEdge> visitedEdges,
						  Vector<MyNode> visitedNodes, HashMap<MyNode, SparseNetNode> graphNodeToNode) {
		int counter = 0;
		// System.out.println(" ********** " + vG.getLabel() + " - SIZE: " +
		// countingEdges.size() + " InDeg: "
		// + vG.getInDegree());

		if (getCommonInDegree(vG, visitedEdges) == 0) {
			// System.out.println("A");
			Iterator<MyEdge> it = vG.outEdges().iterator();
			while (it.hasNext())
				visitedEdges.add(it.next());
		} else if (getCurInDegree(vG, visitedEdges) != 0) {
			// System.out.println("B");
			Iterator<MyEdge> itIn = vG.inEdges().iterator();
			while (itIn.hasNext()) {
				MyEdge eG = itIn.next();
				if (!horizontalEdges.contains(eG) && !visitedEdges.contains(eG)) {

					MyNode sG = eG.getSource();
					SparseNetNode s = graphNodeToNode.get(sG);
					for (SparseNetEdge e : s.getOutEdges()) {
						if (n.isSpecial(e) && !countingEdges.contains(sparseEdgeToEdge.get(e))){
							for (SparseNetEdge eIn : e.getTarget().getInEdges()) {
								countingEdges.add(sparseEdgeToEdge.get(eIn));
								if (sparseEdgeToHorizontalEdge.containsKey(eIn))
									visitedEdges.add(sparseEdgeToHorizontalEdge.get(eIn));
							}
							counter++;
						}
					}

					Iterator<MyEdge> itOut = sG.outEdges().iterator();
					while (itOut.hasNext()) {
						MyEdge eSG = itOut.next();
						if (!horizontalEdges.contains(eSG) && !visitedEdges.contains(eSG))
							visitedEdges.add(eSG);
					}

				}

			}

		}

		if (getCurDegree(vG, visitedEdges) == 0 && getCurHorizontalInDegree(vG, visitedEdges) == 1) {
			// System.out.println("C");
			Iterator<MyEdge> it = vG.outEdges().iterator();
			while (it.hasNext())
				visitedEdges.add(it.next());
			it = vG.inEdges().iterator();
			while (it.hasNext())
				visitedEdges.add(it.next());
		} else {
			// System.out.println("D");
			Iterator<MyEdge> it = vG.outEdges().iterator();
			while (it.hasNext()) {
				MyEdge eG = it.next();
				if (horizontalEdges.contains(eG) && !visitedEdges.contains(eG)) {
					SparseNetEdge e = horizontalEdgeToSparseEdge.get(eG);
					for (SparseNetEdge eIn : e.getTarget().getInEdges()) {
						countingEdges.add(sparseEdgeToEdge.get(eIn));
						if (sparseEdgeToHorizontalEdge.containsKey(eIn))
							visitedEdges.add(sparseEdgeToHorizontalEdge.get(eIn));
					}
					counter++;
				}
			}
			it = vG.inEdges().iterator();
			while (it.hasNext()) {
				MyEdge eG = it.next();
				if (horizontalEdges.contains(eG) && !visitedEdges.contains(eG)) {
					SparseNetEdge e = horizontalEdgeToSparseEdge.get(eG);
					for (SparseNetEdge eIn : e.getTarget().getInEdges()) {
						countingEdges.add(sparseEdgeToEdge.get(eIn));
						if (sparseEdgeToHorizontalEdge.containsKey(eIn))
							visitedEdges.add(sparseEdgeToHorizontalEdge.get(eIn));
					}
				}
			}
		}

		// System.out.println(counter);

		return counter;
	}

	private int getCurInDegree(MyNode v, HashSet<MyEdge> visitedEdges) {
		int curInDegree = 0;
		Iterator<MyEdge> it = v.inEdges().iterator();
		while (it.hasNext()) {
			MyEdge e = it.next();
			if (!visitedEdges.contains(e) && !horizontalEdges.contains(e))
				curInDegree++;
		}
		return curInDegree;
	}

	private int getCurDegree(MyNode v, HashSet<MyEdge> visitedEdges) {
		int curDegree = 0;
		Iterator<MyEdge> it = v.inEdges().iterator();
		while (it.hasNext()) {
			MyEdge e = it.next();
			if (!visitedEdges.contains(e) && !horizontalEdges.contains(e))
				curDegree++;
		}
		it = v.outEdges().iterator();
		while (it.hasNext()) {
			MyEdge e = it.next();
			if (!visitedEdges.contains(e) && !horizontalEdges.contains(e))
				curDegree++;
		}
		return curDegree;
	}

	private int getCurHorizontalInDegree(MyNode v, HashSet<MyEdge> visitedEdges) {
		int curInDegree = 0;
		Iterator<MyEdge> it = v.inEdges().iterator();
		while (it.hasNext()) {
			MyEdge e = it.next();
			if (horizontalEdges.contains(e) && !visitedEdges.contains(e))
				curInDegree++;
		}
		return curInDegree;
	}

	private int getCommonInDegree(MyNode v, HashSet<MyEdge> visitedEdges) {
		int commomInDegree = 0;
		Iterator<MyEdge> it = v.inEdges().iterator();
		while (it.hasNext()) {
			MyEdge e = it.next();
			if (!visitedEdges.contains(e))
				commomInDegree++;
		}
		return commomInDegree;
	}

	private int getCommonDegree(MyNode v, HashSet<MyEdge> visitedEdges) {
		int commonDegree = 0;
		Iterator<MyEdge> it = v.inEdges().iterator();
		while (it.hasNext()) {
			MyEdge e = it.next();
			if (!visitedEdges.contains(e))
				commonDegree++;
		}
		it = v.outEdges().iterator();
		while (it.hasNext()) {
			MyEdge e = it.next();
			if (!visitedEdges.contains(e))
				commonDegree++;
		}
		return commonDegree;
	}

	private Vector<MyNode> getNextNodes(Vector<MyNode> visitedNodes, HashSet<MyEdge> visitedEdges,
										HashMap<MyNode, SparseNetNode> graphNodeToNode) {
		Vector<MyNode> nextNodes = new Vector<MyNode>();
		for (MyNode v : g.getNodes()) {
			if (!visitedNodes.contains(v))
				nextNodes.add(v);
		}

		Collections.sort(nextNodes, new NextNodeComparator(visitedEdges));

		return nextNodes;
	}

	private class NextNodeComparator implements Comparator<MyNode> {

		private HashSet<MyEdge> visitedEdges;

		public NextNodeComparator(HashSet<MyEdge> visitedEdges) {
			this.visitedEdges = visitedEdges;
		}

		@Override
		public int compare(MyNode v1, MyNode v2) {
			int d1 = getCurInDegree(v1, visitedEdges);
			int d2 = getCurInDegree(v2, visitedEdges);
			if (d2 < d1)
				return 1;
			else if (d2 > d1)
				return -1;
			return 0;
		}

	}

	public void refineGraph(HashMap<MyNode, SparseNetNode> graphNodeToNode) {
		boolean hasChanged = false;
		Vector<MyNode> leaves = new Vector<MyNode>();
		for (MyNode v : g.getNodes()) {
			if (v.getOutDegree() == 0 && v.getInDegree() == 1)
				leaves.add(v);
		}
		if (!leaves.isEmpty()) {
			for (MyNode v : leaves) {
				MyNode p = v.getFirstInEdge().getSource();
				p.setLabel(p.getLabel() + " " + v.getLabel());
				removeNode(g, v);
			}
			hasChanged = true;
		}
		MyNode supNode = null;
		for (MyNode v : g.getNodes()) {
			if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
				MyNode p = v.getFirstInEdge().getSource();
				MyNode c = v.getFirstOutEdge().getTarget();
				if (!p.equals(c)) {
					supNode = v;
					break;
				}
			}
		}
		if (supNode != null) {
			supressNode(g, supNode);
			hasChanged = true;
		}
		if (hasChanged)
			refineGraph(graphNodeToNode);
	}

	private boolean supressNode(MyGraph g, MyNode v) {
		if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
			MyNode p = v.getFirstInEdge().getSource();
			MyNode c = v.getFirstOutEdge().getTarget();
			g.newEdge(p, c);
			this.removeNode(g, v);
			return true;
		}
		return false;
	}

	private void removeNode(MyGraph g, MyNode v) {
		Vector<MyEdge> toDelete = new Vector<MyEdge>();
		Iterator<MyEdge> it = v.inEdges().iterator();
		while (it.hasNext())
			toDelete.add(it.next());
		it = v.outEdges().iterator();
		while (it.hasNext())
			toDelete.add(it.next());
		for (MyEdge e : toDelete) {
			g.deleteEdge(e);
		}
		g.deleteNode(v);
	}

	private MyGraph initGraph(SparseNetwork n, HashMap<SparseNetNode, MyNode> nodeToGraphNode,
							  HashMap<MyNode, SparseNetNode> graphNodeToNode) {
		MyGraph g = new MyGraph();
		initGraphRec(g, n.getRoot(), null, new Vector<MyNode>(), nodeToGraphNode, graphNodeToNode);
//		 addAllHorizontalEdges(n, nodeToGraphNode, g);
		return g;
	}

	private void addAllHorizontalEdges(SparseNetwork n2, HashMap<SparseNetNode, MyNode> nodeToGraphNode, MyGraph g) {
		for (SparseNetNode v : n.getNodes()) {
			for (SparseNetEdge e : v.getOutEdges()) {
				if (n2.isSpecial(e)) {
					SparseNetNode vRet = e.getTarget();
					for (SparseNetEdge eIn : vRet.getInEdges()) {
						SparseNetNode s = eIn.getSource();
						if (!s.equals(v)) {
							MyNode vG = nodeToGraphNode.get(v);
							MyNode sG = nodeToGraphNode.get(s);
							MyEdge hEdge = g.newEdge(vG, sG);
							horizontalEdges.add(hEdge);
							horizontalEdgeToSparseEdge.put(hEdge, eIn);
							sparseEdgeToHorizontalEdge.put(eIn, hEdge);
						}
					}
				}
			}
		}
	}

	private void addHorizontalEdges(SparseNetNode v, HashMap<SparseNetNode, MyNode> nodeToGraphNode, HashMap<MyNode, SparseNetNode> graphNodeToNode, MyGraph g, HashSet<SparseNetNode> visitedNodes, Vector<MyNode> criticalNodes) {
		for (SparseNetEdge e : v.getOutEdges()) {
			if (n.isSpecial(e)) {
				SparseNetNode vRet = e.getTarget();
				if(!nodeToCriticalNodes.containsKey(vRet))
					nodeToCriticalNodes.put(vRet, new HashSet<MyNode>());
				nodeToCriticalNodes.get(vRet).addAll(criticalNodes);
				for (SparseNetEdge eIn : vRet.getInEdges()) {
					SparseNetNode s = eIn.getSource();
					if (!s.equals(v) && !visitedNodes.contains(s)) {
						visitedNodes.add(s);
						MyNode vG = nodeToGraphNode.get(v);
						if(!nodeToGraphNode.containsKey(s)){
							MyNode sG = g.newNode();
							sG.setLabel(s.getLabel());
							nodeToGraphNode.put(s, sG);
							graphNodeToNode.put(sG, s);
						}
						MyNode sG = nodeToGraphNode.get(s);
						if (!existHoriozontalEdge(vG, sG)) {
							MyEdge hEdge = g.newEdge(vG, sG);
							horizontalEdges.add(hEdge);
							horizontalEdgeToSparseEdge.put(hEdge, eIn);
							sparseEdgeToHorizontalEdge.put(eIn, hEdge);
						}
						addHorizontalEdges(s, nodeToGraphNode, graphNodeToNode, g, visitedNodes, criticalNodes);
					}
				}
			}
		}
	}

	private boolean existHoriozontalEdge(MyNode vG, MyNode sG) {
		Iterator<MyEdge> it = vG.outEdges().iterator();
		while (it.hasNext()) {
			MyEdge e = it.next();
			if (horizontalEdges.contains(e) && e.getTarget().equals(sG))
				return true;
		}
		return false;
	}

	private void initGraphRec(MyGraph g, SparseNetNode v, SparseNetNode lastNode, Vector<MyNode> criticalNodes,
							  HashMap<SparseNetNode, MyNode> nodeToGraphNode, HashMap<MyNode, SparseNetNode> graphNodeToNode) {

		boolean isCriticalNode = false;
		for (SparseNetEdge e : v.getOutEdges()) {
			if (v.getOwner().isSpecial(e))
				isCriticalNode = true;
		}

		if (isCriticalNode) {
			if (!nodeToGraphNode.containsKey(v)) {
				MyNode vG = g.newNode();
				vG.setLabel(v.getLabel());
				nodeToGraphNode.put(v, vG);
				graphNodeToNode.put(vG, v);
			}
			MyNode vG = nodeToGraphNode.get(v);
			if (!criticalNodes.contains(vG)){
				criticalNodes.add(vG);
				addHorizontalEdges(v, nodeToGraphNode, graphNodeToNode, g, new HashSet<SparseNetNode>(), criticalNodes);
			}
		} else if (v.getInDegree() > 1) {
			HashMap<SparseNetNode, SparseNetEdge> sourceNodeToEdge = new HashMap<SparseNetNode, SparseNetEdge>();
			for (SparseNetEdge e : v.getInEdges()) {
				SparseNetNode s = e.getSource();
				sourceNodeToEdge.put(s, e);
				if (!nodeToGraphNode.containsKey(s)) {
					MyNode sG = g.newNode();
					sG.setLabel(s.getLabel());
					nodeToGraphNode.put(s, sG);
					graphNodeToNode.put(sG, s);
				}
			}
			for (SparseNetNode s : sourceNodeToEdge.keySet()) {
				MyNode sG = nodeToGraphNode.get(s);
				for (MyNode cG : criticalNodes) {
					if (criticalNodes.contains(sG) && !cG.equals(sG)) {
						criticalEdges.add(sourceNodeToEdge.get(s));
						MyEdge eG = existsEdge(cG, sG);
						SparseNetEdge sourceEdge = sourceNodeToEdge.get(s);
						if (!edgeToGraphEdges.containsKey(sourceEdge))
							edgeToGraphEdges.put(sourceEdge, new Vector<MyEdge>());
						if (eG == null) {
							MyEdge newEdge = g.newEdge(cG, sG);
							edgeToGraphEdges.get(sourceEdge).add(newEdge);
							graphEdgeToEdge.put(newEdge, sourceEdge);
						} else {
							graphEdgeToEdge.put(eG, sourceEdge);
							edgeToGraphEdges.get(sourceEdge).add(eG);
						}
					}
				}
			}
		}

		for (SparseNetEdge e : v.getOutEdges()) {
			SparseNetNode c = e.getTarget();
			initGraphRec(g, c, v, (Vector<MyNode>) criticalNodes.clone(), nodeToGraphNode, graphNodeToNode);
		}

	}

	private MyEdge existsEdge(MyNode sG, MyNode tG) {
		Iterator<MyEdge> it = sG.outEdges().iterator();
		while (it.hasNext()) {
			MyEdge e = it.next();
			if (!horizontalEdges.contains(e) && e.getTarget().equals(tG))
				return e;
		}
		return null;
	}

	private void resolveTrivialInteractions(HashMap<SparseNetNode, HashSet<SparseNetNode>> nodeToPreds) {
		HashSet<SparseNetNode> retNodes = new HashSet<SparseNetNode>();
		for (SparseNetNode v : n.getNodes()) {
			if (v.getInDegree() > 1)
				retNodes.add(v);
		}
		HashSet<SparseNetEdge> toResolve = new HashSet<SparseNetEdge>();
		for (SparseNetNode v : retNodes) {
			Vector<SparseNetNode> parentNodes = new Vector<SparseNetNode>();
			Vector<SparseNetEdge> parentEdges = new Vector<SparseNetEdge>();
			for (SparseNetEdge e : v.getInEdges()) {
				parentNodes.add(e.getSource());
				parentEdges.add(e);
			}
			for (SparseNetNode p1 : parentNodes) {
				for (SparseNetNode p2 : parentNodes) {
					if (nodeToPreds.get(p1).contains(p2))
						toResolve.add(parentEdges.get(parentNodes.indexOf(p2)));
				}
			}
		}
		for (SparseNetEdge e : toResolve) {
			trivialEdgeOrdering.add(sparseEdgeToEdge.get(e));
			SparseNetNode s = e.getSource();
			SparseNetNode t = e.getTarget();
			s.removeOutEdge(e);
			SparseNetNode x = new SparseNetNode(s, n, s.getLabel());
			x.addChild(t);
		}
	}

	private void assingPreds(SparseNetNode v, HashSet<SparseNetNode> preds,
							 HashMap<SparseNetNode, HashSet<SparseNetNode>> nodeToPreds) {
		boolean isCritical = false;
		for (SparseNetEdge e : v.getOutEdges()) {
			if (v.getOwner().isSpecial(e)) {
				nodeToPreds.put(v, preds);
				isCritical = true;
				break;
			}
		}
		if (isCritical)
			preds.add(v);
		for (SparseNetEdge e : v.getOutEdges())
			assingPreds(e.getTarget(), (HashSet<SparseNetNode>) preds.clone(), nodeToPreds);
	}

	private SparseNetwork copyNetwork(MyPhyloTree t) {
		MyNode root = t.getRoot();
		SparseNetNode rootCopy = new SparseNetNode(null, null, t.getLabel(root));
		SparseNetwork nCopy = new SparseNetwork(rootCopy);
		copyNetworkRec(t, nCopy, root, rootCopy, new Hashtable<MyNode, SparseNetNode>());
		return nCopy;
	}

	private void copyNetworkRec(MyPhyloTree t, SparseNetwork nCopy, MyNode v, SparseNetNode vCopy,
								Hashtable<MyNode, SparseNetNode> visited) {
		visited.put(v, vCopy);
		Iterator<MyEdge> it = v.getOutEdges();
		while (it.hasNext()) {
			MyEdge e = it.next();
			MyNode c = e.getTarget();
			SparseNetEdge eCopy = null;
			if (visited.containsKey(c)) {
				SparseNetNode cCopy = visited.get(c);
				eCopy = vCopy.addChild(cCopy);
			} else {
				SparseNetNode cCopy = new SparseNetNode(vCopy, nCopy, t.getLabel(c));
				eCopy = cCopy.getInEdges().get(0);
				if (c.getOutDegree() != 0)
					copyNetworkRec(t, nCopy, c, cCopy, visited);
			}
			sparseEdgeToEdge.put(eCopy, e);
		}
	}

	private void assingUniqueLabeling(SparseNetwork n) {
		Vector<String> labels = new Vector<String>();
		int i = 0;
		for (SparseNetNode v : n.getNodes()) {
			String l = i + "";
			while (labels.contains(l))
				l = (i++) + "";
			labels.add(l);
			v.setLabel(l);
		}
	}

	public void printGraph() {
		System.out.println("**********\nPrinting Consistency Graph\n" + nInput.getPhyloTree().toMyBracketString()
				+ "\n#Nodes: " + g.getNodes().size());
		for (MyNode v : g.getNodes()) {
			Iterator<MyEdge> it = v.inEdges().iterator();
			System.out.println("Node: " + v.getLabel() + " Indeg: " + v.getInDegree());
			while (it.hasNext()) {
				MyEdge e = it.next();
				if (horizontalEdges.contains(e))
					System.out.println(e.getSource().getLabel() + "-H->" + e.getTarget().getLabel() + " ");
				else
					System.out.println(e.getSource().getLabel() + "-V->" + e.getTarget().getLabel() + " ");
			}
		}
		System.out.println("**********");
	}

}
