package dendroscope.hybroscale.model.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import dendroscope.hybroscale.model.treeObjects.SparseNetEdge;
import dendroscope.hybroscale.model.treeObjects.SparseNetNode;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyGraph;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

public class CheckTimeConsistency {

	private SparseNetwork nInput;

	private HashMap<SparseNetNode, MyNode> sparseNodeToNode;;
	private int heuDegree = Integer.MAX_VALUE;

	private Vector<MyNode> nodeOrdering;
	private boolean checkSuccesful = false;

	public int run(MyPhyloTree network, int border, boolean heuristicMode, boolean storeNetworkInfo, int maxNodes) {

		if (network.getTimeDegree() != null) {
			int timeDegree = network.getTimeDegree();
			return timeDegree <= border ? timeDegree : -1;
		}

		int result = 0;
		for (MyPhyloTree net : new GetNetworkCluster().run(network)) {

			heuDegree = maxNodes;
			checkSuccesful = false;
			sparseNodeToNode = new HashMap<SparseNetNode, MyNode>();
			SparseNetwork n = copyNetwork(net);

			assingUniqueLabeling(n);
			nInput = new SparseNetwork(n);

			HashMap<SparseNetNode, MyNode> nodeToGraphNode = new HashMap<SparseNetNode, MyNode>();
			HashMap<MyNode, Vector<SparseNetNode>> graphNodeToNode = new HashMap<MyNode, Vector<SparseNetNode>>();
			MyGraph g = initGraph(n, nodeToGraphNode, graphNodeToNode);
			refineGraph(g, graphNodeToNode);

			int timeConsValue = -1;
			for (int k = 0; k <= border; k++) {
				HashSet<MyEdge> visitedEdges = new HashSet<MyEdge>();
				Vector<MyNode> visitedNodes = new Vector<MyNode>();
				Vector<MyNode> nextNodes = getNextNodes(g, visitedNodes, visitedEdges, graphNodeToNode, heuristicMode);
				for (MyNode nextNode : nextNodes) {
					int newMaxK = k;
					checkConsistency(g, nextNode, newMaxK, (Vector<MyNode>) visitedNodes.clone(),
							(HashSet<MyEdge>) visitedEdges.clone(), heuristicMode, graphNodeToNode);
					if (checkSuccesful) {
						if (storeNetworkInfo) {
							// assignTimeStamps(graphNodeToNode);
							// net.setTimeDegree(k);
						}
						timeConsValue = k;
						break;
					}
				}
				if (timeConsValue != -1)
					break;
			}

			if (timeConsValue == -1)
				return -1;
			else {
				result += timeConsValue;
				border -= timeConsValue;
			}

		}

		if (result != -1)
			network.setTimeDegree(result);

		return result;
	}

	private void assignTimeStamps(HashMap<MyNode, Vector<SparseNetNode>> graphNodeToNode) {
		int i = 0;
		for (MyNode vG : nodeOrdering) {
			boolean entered = false;
			for (SparseNetNode vN : graphNodeToNode.get(vG)) {
				if (vN.getOutDegree() != 0) {
					entered = true;
					MyNode v = sparseNodeToNode.get(vN);
					String label = v.getLabel();
					if (label == null)
						label = "";
					if (!label.isEmpty())
						label = label.concat("_TS" + i);
					else
						label = "TS" + i;
					v.setLabel(label);
				}
			}
			if (entered)
				i++;
		}
	}

	private void checkConsistency(MyGraph g, MyNode v, int maxK, Vector<MyNode> visitedNodes,
			HashSet<MyEdge> visitedEdges, boolean heuristicMode, HashMap<MyNode, Vector<SparseNetNode>> graphNodeToNode) {
		visitedNodes.add(v);
		int weight = getCurInDegree(v, visitedEdges);
		maxK = maxK - weight;
		if (maxK >= 0 && visitedNodes.size() == g.getNodes().size()) {
			nodeOrdering = (Vector<MyNode>) visitedNodes.clone();
			checkSuccesful = true;
		} else if (maxK >= 0 && !checkSuccesful) {
			Iterator<MyEdge> it = v.getOutEdges();
			while (it.hasNext()) {
				MyEdge e = it.next();
				visitedEdges.add(e);
			}
			Vector<MyNode> nextNodes = getNextNodes(g, visitedNodes, visitedEdges, graphNodeToNode, heuristicMode);
			for (MyNode nextNode : nextNodes) {
				int newMaxK = maxK;
				checkConsistency(g, nextNode, newMaxK, (Vector<MyNode>) visitedNodes.clone(),
						(HashSet<MyEdge>) visitedEdges.clone(), heuristicMode, graphNodeToNode);
			}
		}
	}

	private Vector<MyNode> getNextNodes(MyGraph g, Vector<MyNode> visitedNodes, HashSet<MyEdge> visitedEdges,
			HashMap<MyNode, Vector<SparseNetNode>> graphNodeToNode, boolean heuristicMode) {
		Vector<MyNode> nextNodes = new Vector<MyNode>();
		for (MyNode v : g.getNodes()) {
			if (!visitedNodes.contains(v))
				nextNodes.add(v);
		}

		Collections.sort(nextNodes, new NextNodeComparator(visitedEdges, graphNodeToNode));

		if (heuristicMode) {
			Vector<MyNode> nextHeuNodes = new Vector<MyNode>();
			int k = nextNodes.size() > heuDegree ? heuDegree : nextNodes.size();
			for (int i = 0; i < k; i++)
				nextHeuNodes.add(nextNodes.get(i));
			return nextHeuNodes;
		}

		return nextNodes;
	}

	private class NextNodeComparator implements Comparator<MyNode> {

		private HashSet<MyEdge> visitedEdges;
		private HashMap<MyNode, Vector<SparseNetNode>> graphNodeToNode;

		public NextNodeComparator(HashSet<MyEdge> visitedEdges, HashMap<MyNode, Vector<SparseNetNode>> graphNodeToNode) {
			this.visitedEdges = visitedEdges;
			this.graphNodeToNode = graphNodeToNode;
		}

		@Override
		public int compare(MyNode v1, MyNode v2) {
			int d1 = getCurDegree(v1, visitedEdges);
			int d2 = getCurDegree(v2, visitedEdges);
			if (d2 < d1)
				return 1;
			else if (d2 > d1)
				return -1;
			return 0;
		}

	}

	private int getCurDegree(MyNode v, HashSet<MyEdge> visitedEdges) {
		int curInDegree = 0;
		Iterator<MyEdge> it = v.getInEdges();
		while (it.hasNext()) {
			if (!visitedEdges.contains(it.next()))
				curInDegree++;
		}
		return curInDegree;
	}

	private int getNumberOfHNodes(MyNode vG, HashMap<MyNode, Vector<SparseNetNode>> graphNodeToNode) {
		int numberOfHNodes = 0;
		for (SparseNetNode v : graphNodeToNode.get(vG)) {
			if (v.getInDegree() > 1) {
				numberOfHNodes++;
			}
		}
		return numberOfHNodes;
	}

	private int getCurInDegree(MyNode v, HashSet<MyEdge> visitedEdges) {
		int curInDegree = 0;
		Iterator<MyEdge> it = v.getInEdges();
		while (it.hasNext()) {
			if (!visitedEdges.contains(it.next()))
				curInDegree++;
		}
		return curInDegree;
	}

	public void refineGraph(MyGraph g, HashMap<MyNode, Vector<SparseNetNode>> graphNodeToNode) {
		boolean hasChanged = false;
		Vector<MyNode> leaves = new Vector<MyNode>();
		for (MyNode v : g.getNodes()) {
			if (v.getOutDegree() == 0 && v.getInDegree() == 1 && graphNodeToNode.get(v).size() == 1)
				leaves.add(v);
		}
		if (!leaves.isEmpty()) {
			for (MyNode v : leaves) {
				MyNode p = v.getInEdges().next().getSource();
				p.setLabel(p.getLabel() + " " + v.getLabel());
				removeNode(g, v);
			}
			hasChanged = true;
		}
		MyNode supNode = null;
		for (MyNode v : g.getNodes()) {
			if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
				MyNode p = v.getInEdges().next().getSource();
				MyNode c = v.getOutEdges().next().getTarget();
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
			refineGraph(g, graphNodeToNode);
	}

	private MyGraph initGraph(SparseNetwork n, HashMap<SparseNetNode, MyNode> nodeToGraphNode,
			HashMap<MyNode, Vector<SparseNetNode>> graphNodeToNode) {
		MyGraph g = new MyGraph();
		MyNode rootG = g.newNode();
		rootG.setLabel(n.getRoot().getLabel());
		nodeToGraphNode.put(n.getRoot(), rootG);
		graphNodeToNode.put(rootG, new Vector<SparseNetNode>());
		graphNodeToNode.get(rootG).add(n.getRoot());
		initGraphRec(g, n.getRoot(), nodeToGraphNode, graphNodeToNode);
		return g;
	}

	private void initGraphRec(MyGraph g, SparseNetNode v, HashMap<SparseNetNode, MyNode> nodeToGraphNode,
			HashMap<MyNode, Vector<SparseNetNode>> graphNodeToNode) {

		for (SparseNetEdge eOut : v.getOutEdges()) {
			SparseNetNode c = eOut.getTarget();
			if (c.getInDegree() > 1) {

				MyNode vG = nodeToGraphNode.get(v);
				MyNode cG = nodeToGraphNode.containsKey(c) ? nodeToGraphNode.get(c) : g.newNode();

				if (!vG.equals(cG)) {

					if (!nodeToGraphNode.containsKey(c))
						cG.setLabel(c.getLabel());

					transferEdges(g, vG, cG);
					removeNode(g, vG);
					nodeToGraphNode.remove(v);

					Vector<SparseNetNode> xNodes = (Vector<SparseNetNode>) graphNodeToNode.get(vG).clone();
					for (SparseNetNode x : xNodes) {
						nodeToGraphNode.put(x, cG);
						if (!graphNodeToNode.containsKey(cG))
							graphNodeToNode.put(cG, new Vector<SparseNetNode>());
						graphNodeToNode.get(cG).add(x);
					}

					cG.setLabel(cG.getLabel() + " " + vG.getLabel());

					if (!nodeToGraphNode.containsKey(c)) {
						nodeToGraphNode.put(c, cG);
						if (!graphNodeToNode.containsKey(cG))
							graphNodeToNode.put(cG, new Vector<SparseNetNode>());
						graphNodeToNode.get(cG).add(c);
						initGraphRec(g, c, nodeToGraphNode, graphNodeToNode);
					}

				}
			} else {
				MyNode vG = nodeToGraphNode.get(v);
				MyNode cG = g.newNode();
				cG.setLabel(c.getLabel());
				g.newEdge(vG, cG);
				nodeToGraphNode.put(c, cG);
				if (!graphNodeToNode.containsKey(cG))
					graphNodeToNode.put(cG, new Vector<SparseNetNode>());
				graphNodeToNode.get(cG).add(c);
				initGraphRec(g, c, nodeToGraphNode, graphNodeToNode);
			}
		}
	}

	private void removeNode(MyGraph g, MyNode v) {
		Vector<MyEdge> toDelete = new Vector<MyEdge>();
		Iterator<MyEdge> it = v.getInEdges();
		while (it.hasNext())
			toDelete.add(it.next());
		it = v.getOutEdges();
		while (it.hasNext())
			toDelete.add(it.next());
		for (MyEdge e : toDelete) {
			g.deleteEdge(e);
		}
		g.deleteNode(v);
	}

	private void transferEdges(MyGraph g, MyNode v1, MyNode v2) {
		Iterator<MyEdge> it = v1.getInEdges();
		Vector<MyNode> sourceNodes = new Vector<MyNode>();
		while (it.hasNext()) {
			MyNode s = it.next().getSource();
			sourceNodes.add(s);
		}
		for (MyNode s : sourceNodes) {
			if (!s.equals(v2))
				g.newEdge(s, v2);
		}
		it = v1.getOutEdges();
		Vector<MyNode> targetNodes = new Vector<MyNode>();
		while (it.hasNext()) {
			MyNode t = it.next().getTarget();
			targetNodes.add(t);
		}
		for (MyNode t : targetNodes) {
			if (!t.equals(v2))
				g.newEdge(v2, t);
		}
	}

	private boolean supressNode(MyGraph g, MyNode v) {
		if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
			MyNode p = v.getInEdges().next().getSource();
			MyNode c = v.getOutEdges().next().getTarget();
			g.newEdge(p, c);
			this.removeNode(g, v);
			return true;
		}
		return false;
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

		sparseNodeToNode.put(vCopy, v);
		visited.put(v, vCopy);
		Iterator<MyEdge> it = v.getOutEdges();
		while (it.hasNext()) {
			MyNode c = it.next().getTarget();
			if (visited.containsKey(c)) {
				SparseNetNode cCopy = visited.get(c);
				vCopy.addChild(cCopy);
			} else {
				SparseNetNode cCopy = new SparseNetNode(vCopy, nCopy, t.getLabel(c));
				if (c.getOutDegree() != 0)
					copyNetworkRec(t, nCopy, c, cCopy, visited);
			}
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

	public void printGraph(MyGraph g) {
		System.out.println("**********\nPrinting Consistency Graph\n" + nInput.getPhyloTree().toMyBracketString()
				+ "\n#Nodes: " + g.getNodes().size());
		for (MyNode v : g.getNodes()) {
			Iterator<MyEdge> it = v.getInEdges();
			while (it.hasNext()) {
				MyEdge e = it.next();
				System.out.println(e.getSource().getLabel() + " -> " + e.getTarget().getLabel());
			}
		}
		System.out.println("**********");
	}

	public String outputGraph(MyGraph g) {
		StringBuffer buf = new StringBuffer("");
		buf.append("**********\nPrinting Consistency Graph\n" + nInput.getPhyloTree().toMyBracketString()
				+ "\n#Nodes: " + g.getNodes().size() + "\n");
		for (MyNode v : g.getNodes()) {
			Iterator<MyEdge> it = v.getInEdges();
			while (it.hasNext()) {
				MyEdge e = it.next();
				buf.append(e.getSource().getLabel() + " -> " + e.getTarget().getLabel() + "\n");
			}
		}
		buf.append("**********\n");
		return buf.toString();
	}

}
