package dendroscope.hybroscale.model.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

public class CheckAddTaxaEdge {

	private MyPhyloTree result;
	private HashSet<MyEdge> trivialEdges;

	public MyPhyloTree getResult() {
		return result;
	}

	public int run(MyPhyloTree net, int border, boolean storeNetworkInfo, boolean smartMode) {

		int result = 0;
		for (MyPhyloTree n : new GetNetworkCluster().run(net)) {

			trivialEdges = new HashSet<MyEdge>();

			if (smartMode) {
				HashMap<MyNode, HashSet<MyNode>> nodeToPreds = new HashMap<MyNode, HashSet<MyNode>>();
				assingPreds(n.getRoot(), new HashSet<MyNode>(), nodeToPreds);
				cmpTrivialInteractions(n, nodeToPreds);
			}

			Vector<MyEdge> retEdges = new Vector<MyEdge>();
			for (MyNode v : n.getNodes()) {
				if (v.getInDegree() > 1) {
					Iterator<MyEdge> it = v.getInEdges();
					while (it.hasNext()) {
						MyEdge e = it.next();
						if (!trivialEdges.contains(e))
							retEdges.add(e);
					}
				}
			}

			int addTaxaDegree = -1;
			for (int k = trivialEdges.size(); k <= border; k++) {
				HashSet<Vector<MyEdge>> allSubsets = new HashSet<Vector<MyEdge>>();
				getAllSubsets(0, retEdges, k - trivialEdges.size(), new Vector<MyEdge>(), allSubsets);
				for (Vector<MyEdge> subset : allSubsets) {
					clearEdges(n);
					setEdges(subset);
					if (checkTree(n)) {
						if (storeNetworkInfo) {
							n.setAddTaxaDegree(k);
							setEdges(subset);
						} else
							clearEdges(n);
						addTaxaDegree = k;
						break;
					}
				}
				if (addTaxaDegree != -1)
					break;
			}

			if (addTaxaDegree == -1)
				return -1;
			else {
				result += addTaxaDegree;
				border -= addTaxaDegree;
			}

		}
		
		if(result != -1)
			net.setAddTaxaDegree(result);
		
		return result;

	}

	private boolean checkTree(MyPhyloTree net) {
		MyPhyloTree n = new MyPhyloTree(net);
		n.setTimeDegree(null);
		HashSet<MyEdge> criticalEdges = new HashSet<MyEdge>();
		for (MyEdge e : n.getEdges()) {
			if (e.isCritical())
				criticalEdges.add(e);
		}
		for (MyEdge e : criticalEdges) {
			MyNode s = e.getSource();
			MyNode t = e.getTarget();
			s.removeOutEdge(e);
			MyNode x = n.newNode();
			n.newEdge(s, x);
			n.newEdge(x, t);
		}
		CheckTimeConsistency checker = new CheckTimeConsistency();
		int k = checker.run(n, 0, false, true, -1);
		if (k != 0)
			return false;
		return true;
	}

	private void setEdges(Vector<MyEdge> subset) {
		for (MyEdge e : subset)
			e.setCritical(true);
		for (MyEdge e : trivialEdges)
			e.setCritical(true);
	}

	private void clearEdges(MyPhyloTree n) {
		for (MyEdge e : n.getEdges()) {
			e.setCritical(false);
		}
	}

	private void getAllSubsets(int index, Vector<MyEdge> retEdges, int border, Vector<MyEdge> subset,
			HashSet<Vector<MyEdge>> allSubsets) {
		if (subset.size() < border) {
			for (int i = index; i < retEdges.size(); i++) {
				MyEdge e = retEdges.get(i);
				if (!subset.contains(e)) {
					Vector<MyEdge> newSubset = (Vector<MyEdge>) subset.clone();
					newSubset.add(e);
					int newIndex = i + 1;
					getAllSubsets(newIndex, retEdges, border, newSubset, allSubsets);
				}
			}
		} else
			allSubsets.add(subset);

	}

	private void cmpTrivialInteractions(MyPhyloTree n, HashMap<MyNode, HashSet<MyNode>> nodeToPreds) {
		HashSet<MyNode> retNodes = new HashSet<MyNode>();
		for (MyNode v : n.getNodes()) {
			if (v.getInDegree() > 1) {
				retNodes.add(v);
			}
		}
		HashSet<MyEdge> toResolve = new HashSet<MyEdge>();
		for (MyNode v : retNodes) {
			Vector<MyNode> parentNodes = new Vector<MyNode>();
			Vector<MyEdge> parentEdges = new Vector<MyEdge>();
			Iterator<MyEdge> it = v.getInEdges();
			while (it.hasNext()) {
				MyEdge e = it.next();
				parentNodes.add(e.getSource());
				parentEdges.add(e);
			}
			for (MyNode p1 : parentNodes) {
				for (MyNode p2 : parentNodes) {
					if (nodeToPreds.get(p1).contains(p2)) {
						if (ancestorCheck(p1, p2))
							toResolve.add(parentEdges.get(parentNodes.indexOf(p2)));
					}
				}
			}
		}
		for (MyEdge e : toResolve)
			trivialEdges.add(e);

	}

	private boolean ancestorCheck(MyNode p1, MyNode p2) {
		Vector<MyNode> implicitAncestors = new Vector<MyNode>();
		cmpImplicitAncestorsRec(true, p1, implicitAncestors);
		return !implicitAncestors.contains(p2);
	}

	private void cmpImplicitAncestorsRec(boolean b, MyNode v, Vector<MyNode> implicitAncestors) {
		if (v.getInDegree() > 1 || b) {
			Iterator<MyEdge> it = v.getInEdges();
			while (it.hasNext()) {
				MyEdge e = it.next();
				MyNode a = e.getSource();
				implicitAncestors.add(a);
				cmpImplicitAncestorsRec(v.getInDegree() != 1, a, implicitAncestors);
			}
		}
	}

	private void assingPreds(MyNode v, HashSet<MyNode> preds, HashMap<MyNode, HashSet<MyNode>> nodeToPreds) {
		boolean isCritical = false;
		Iterator<MyEdge> it = v.getOutEdges();
		while (it.hasNext()) {
			MyEdge e = it.next();
			if (v.getOwner().isSpecial(e)) {
				if (!nodeToPreds.containsKey(v))
					nodeToPreds.put(v, (HashSet<MyNode>) preds.clone());
				else
					nodeToPreds.get(v).addAll(preds);
				isCritical = true;
				break;
			}
		}
		if (isCritical)
			preds.add(v);
		it = v.getOutEdges();
		while (it.hasNext()) {
			MyEdge e = it.next();
			assingPreds(e.getTarget(), (HashSet<MyNode>) preds.clone(), nodeToPreds);
		}
	}

	public HashSet<MyEdge> getTrivialEdges() {
		return trivialEdges;
	}

}
