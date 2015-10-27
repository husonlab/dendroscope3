package dendroscope.hybroscale.model.util;

import java.util.Vector;

import dendroscope.hybroscale.model.treeObjects.SparseNetEdge;
import dendroscope.hybroscale.model.treeObjects.SparseNetNode;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

public class CheckLevel {

	private int border;
	private boolean stop = false;

	public int run(MyPhyloTree network, int border) {

		this.border = border;

		SparseNetwork n = new SparseNetwork(network);
		Vector<SparseNetNode> retNodes = new Vector<SparseNetNode>();
		for (SparseNetNode v : n.getNodes()) {
			if (v.getInDegree() > 1)
				retNodes.add(v);
		}

		if (retNodes.isEmpty()) {
			network.setLevel(0);
			return 0;
		}

		int maxLevel = -1;
		for (SparseNetNode v : retNodes) {
			int level = cmpLevel(v);
			maxLevel = level > maxLevel ? level : maxLevel;
			if (stop)
				return -1;
		}

		network.setLevel(maxLevel);

		return maxLevel;
	}

	private int cmpLevel(SparseNetNode v) {

		Vector<Vector<SparseNetEdge>> allUndirectedCycles = new Vector<Vector<SparseNetEdge>>();
		for (SparseNetEdge e : v.getInEdges()) {
			Vector<SparseNetEdge> visitedEdges = new Vector<SparseNetEdge>();
			visitedEdges.add(e);
			findUndirectedCycleRec(e.getSource(), v, visitedEdges, allUndirectedCycles);
		}

		int maxLevel = -1;

		if (!stop) {
			for (Vector<SparseNetEdge> undirectedCycle : allUndirectedCycles) {
				int level = 0;
				Vector<SparseNetNode> retNodes = new Vector<SparseNetNode>();
				Vector<SparseNetNode> mulNodes = new Vector<SparseNetNode>();
				for (SparseNetEdge e : undirectedCycle) {
					if (e.getTarget().getInDegree() > 1) {
						level++;
						if (!retNodes.contains(e.getTarget()))
							retNodes.add(e.getTarget());
						else if (!mulNodes.contains(e.getTarget()))
							mulNodes.add(e.getTarget());
					}
				}
				maxLevel = level - mulNodes.size() > maxLevel ? level - mulNodes.size() : maxLevel;
			}
		}

		return maxLevel;
	}

	private void findUndirectedCycleRec(SparseNetNode w, SparseNetNode v, Vector<SparseNetEdge> visitedEdges,
			Vector<Vector<SparseNetEdge>> allUndirectedCycles) {
		if (w.equals(v)) {
			allUndirectedCycles.add(visitedEdges);
			if (getCycleCosts(visitedEdges) > border)
				stop = true;
		}
		if (!stop) {
			for (SparseNetEdge e : w.getInEdges()) {
				if (!visitedEdges.contains(e)) {
					Vector<SparseNetEdge> visitedEdgesCopy = (Vector<SparseNetEdge>) visitedEdges.clone();
					visitedEdgesCopy.add(e);
					findUndirectedCycleRec(e.getSource(), v, visitedEdgesCopy, allUndirectedCycles);
				}
			}
			for (SparseNetEdge e : w.getOutEdges()) {
				if (!visitedEdges.contains(e)) {
					Vector<SparseNetEdge> visitedEdgesCopy = (Vector<SparseNetEdge>) visitedEdges.clone();
					visitedEdgesCopy.add(e);
					findUndirectedCycleRec(e.getTarget(), v, visitedEdgesCopy, allUndirectedCycles);
				}
			}
		}
	}

	private int getCycleCosts(Vector<SparseNetEdge> undirectedCycle) {
		int level = 0;
		Vector<SparseNetNode> retNodes = new Vector<SparseNetNode>();
		Vector<SparseNetNode> mulNodes = new Vector<SparseNetNode>();
		for (SparseNetEdge e : undirectedCycle) {
			if (e.getTarget().getInDegree() > 1) {
				level++;
				if (!retNodes.contains(e.getTarget()))
					retNodes.add(e.getTarget());
				else if (!mulNodes.contains(e.getTarget()))
					mulNodes.add(e.getTarget());
			}
		}
		return level - mulNodes.size();
	}

}
