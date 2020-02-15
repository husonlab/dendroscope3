/*
 *   TerminalAlg_Light.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.terminals;

import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyGraph;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TerminalAlg_Light {

	private MyPhyloTree tOne, tTwo;
	private Vector<String> taxaOrdering;
	private int r, labelOffset;
	private boolean success = false;

	private ConcurrentHashMap<BitSet, Integer> failedCutSets = new ConcurrentHashMap<BitSet, Integer>();
	private boolean isStopped = false;

	private int recCounter = 0, termCounter = 0;
	private int caseOne = 0, caseTwo = 0, caseThree = 0, caseFour = 0;
	private long runtime = 0, runtimeOne = 0, runtimeTwo = 0, runtimeThree = 0, runtimeFour = 0, runtimeFive = 0;

	public TerminalAlg_Light(MyPhyloTree tOne, MyPhyloTree tTwo, int r, Vector<String> taxaOrdering) {
		this.tOne = tOne;
		this.tTwo = tTwo;
		this.r = r;
		this.taxaOrdering = (Vector<String>) taxaOrdering.clone();
	}

	public boolean run() {

		long time = System.currentTimeMillis();

		labelOffset = Integer.MAX_VALUE;
		for (String taxon : taxaOrdering) {
			Integer i = Integer.parseInt(taxon);
			labelOffset = i < labelOffset ? i : labelOffset;
		}

		// System.out.println("Trying " + r + "... " + failedCutSets.size());

		MyPhyloTree t1 = copyTree(tOne, new MyNode[2]);
		MyPhyloTree t2 = copyTree(tTwo, new MyNode[2]);
		runRec(t1, t2, r, new BitSet(taxaOrdering.size()));

		runtime = System.currentTimeMillis() - time;

//		long sum = runtimeOne + runtimeTwo + runtimeThree + runtimeFour + runtimeFive;
//		System.out.println("\n" + caseOne + " " + caseTwo + " " + caseThree + " " + caseFour);
//		System.out.println(r + " #RecCalls: " + recCounter + " " + termCounter);
//		System.out.println(runtime + " / " + sum + ": RT1: " + runtimeOne + "s, RT2: " + runtimeTwo + "s, RT3: "
//				+ runtimeThree + "s, RT4: " + runtimeFour + "s, RT5: " + runtimeFive + "s ");

		// System.out.println("Result: " + r + " " +
		// success+" "+failedCutSets.size());

		return success;

	}

	private void runRec(MyPhyloTree t1, MyPhyloTree t2, int i, BitSet cutSet) {

		long time = System.currentTimeMillis();
		BitSet hashSet = getHashSet(t1);
		int lowerBound = failedCutSets.containsKey(hashSet) ? failedCutSets.get(hashSet) : -1;
		runtimeFour += System.currentTimeMillis() - time;

		if (lowerBound >= i)
			termCounter++;

		if (!success && lowerBound < i && !isStopped) {

			recCounter++;

			MyNode[] t1Leaves = new MyNode[taxaOrdering.size()];
			MyNode[] t2Leaves = new MyNode[taxaOrdering.size()];

			// System.out.println("\n");
			// System.out.println(t1 + ";");
			// System.out.println(t2 + ";");

			time = System.currentTimeMillis();
			cmpClusters(t1, t2, t1Leaves, t2Leaves);
			runtimeOne += System.currentTimeMillis() - time;

			time = System.currentTimeMillis();
			collapseST(t1, t2, t1Leaves, t2Leaves);
			runtimeTwo += System.currentTimeMillis() - time;

			// System.out.println(t1 + ";");
			// System.out.println(t2 + ";");

			time = System.currentTimeMillis();
			cmpClusters(t1, t2, t1Leaves, t2Leaves);
			runtimeOne += System.currentTimeMillis() - time;

			time = System.currentTimeMillis();
			hashSet = getHashSet(t1);
			lowerBound = failedCutSets.containsKey(hashSet) ? failedCutSets.get(hashSet) : -1;
			runtimeFour += System.currentTimeMillis() - time;

			if (lowerBound < i) {

				if (t1.getNodes().size() == 1) {
					success = true;
				} else if (i > 0) {

					time = System.currentTimeMillis();
					HashSet<BitSet> primeSet = assessPrimeSet(i, t1, t2, t1Leaves, t2Leaves);
					runtimeThree += System.currentTimeMillis() - time;

					for (BitSet b : primeSet) {

						time = System.currentTimeMillis();

						BitSet leafSet = (BitSet) getLeaf(t1Leaves, b).getInfo();

						MyNode v1 = getLeaf(t1Leaves, b);
						MyNode[] nodePair1 = { v1, null };
						MyPhyloTree t1Copy = copyTree(t1, nodePair1);
						deleteLeafNode(nodePair1[1]);

						MyNode v2 = getLeaf(t2Leaves, b);
						MyNode[] nodePair2 = { v2, null };
						MyPhyloTree t2Copy = copyTree(t2, nodePair2);
						deleteLeafNode(nodePair2[1]);

						int iRec = i - 1;
						BitSet recCutSet = (BitSet) cutSet.clone();
						recCutSet.or(leafSet);

						runtimeFive += System.currentTimeMillis() - time;

						runRec(t1Copy, t2Copy, iRec, recCutSet);

					}
				}

			}

		}

		time = System.currentTimeMillis();
		if (!success && usehash()) {
			hashSet = getHashSet(t1);
			if (!failedCutSets.containsKey(hashSet) || failedCutSets.get(hashSet) < i)
				failedCutSets.put(hashSet, i);
		}
		runtimeFour += System.currentTimeMillis() - time;

	}

	private BitSet getHashSet(MyPhyloTree t) {
		BitSet leafSet = new BitSet(taxaOrdering.size());
		for (MyNode l : t.getLeaves()) {
			int index = Integer.parseInt(l.getLabel()) - labelOffset;
			leafSet.set(index);
		}
		return leafSet;
	}

	public int getTermCounter() {
		return termCounter;
	}

	public int getRecCounter() {
		return recCounter;
	}

	private boolean usehash() {
		double maxMemory = java.lang.Runtime.getRuntime().maxMemory();
		double totalMemory = java.lang.Runtime.getRuntime().totalMemory();
		if ((totalMemory / maxMemory) < 0.9)
			return true;
		return false;
	}

	private void deleteLeafNode(MyNode v) {
		MyEdge e = v.getFirstInEdge();
		MyNode p = e.getSource();
		p.getOwner().deleteEdge(e);
		p.removeOutEdge(e);
		contractNode(p);
	}

	private void contractNode(MyNode v) {
		if (v.getOutDegree() == 1 && v.getInDegree() == 1) {
			MyGraph t = v.getOwner();
			MyEdge eUp = v.getFirstInEdge();
			MyNode p = eUp.getSource();
			MyEdge eDown = v.getFirstOutEdge();
			MyNode c = eDown.getTarget();
			t.deleteEdge(eUp);
			t.deleteEdge(eDown);
			t.newEdge(p, c);
		} else if (v.getOutDegree() == 1 && v.getInDegree() == 0) {
			MyPhyloTree t = (MyPhyloTree) v.getOwner();
			MyEdge eDown = v.getFirstOutEdge();
			MyNode c = eDown.getTarget();
			t.deleteEdge(eDown);
			t.setRoot(c);
		}
	}

	private HashSet<BitSet> assessPrimeSet(int i, MyPhyloTree t1, MyPhyloTree t2, MyNode[] t1Leaves, MyNode[] t2Leaves) {

		HashSet<BitSet> terminalSet = new HashSet<BitSet>();
		for (MyNode v : t1.getLeaves()) {
			if (isTerminalNode(v, t2Leaves))
				terminalSet.add(v.getCluster());
		}

		HashSet<BitSet> primeSet = new HashSet<BitSet>();
		if (terminalSet.size() > 3 * i) {
			caseOne++;
			return primeSet;
		}

		primeSet = searchConflict2Cluster(terminalSet, t1Leaves, t2Leaves);
		if (!primeSet.isEmpty()) {
			caseTwo++;
			return primeSet;
		}

		if (terminalSet.size() > 2 * i) {
			caseThree++;
			Iterator<BitSet> it = terminalSet.iterator();
			while (primeSet.size() <= 2 * i)
				primeSet.add(it.next());
		} else {
			caseFour++;

			HashSet<HashSet<MyNode>> t1Cherries = getCherries(t1);
			HashSet<HashSet<MyNode>> t2Cherries = getCherries(t2);

			BitSet cherrySet = new BitSet(taxaOrdering.size());
			for (HashSet<MyNode> cherry : t2Cherries) {
				for (MyNode c : cherry)
					cherrySet.or(c.getCluster());
			}

			BitSet chosenSet = new BitSet(taxaOrdering.size());
			for (HashSet<MyNode> cherry : t1Cherries) {
				MyNode[] nodePair = new MyNode[2];
				for (MyNode v : cherry) {
					if (isTerminalNode(v, t2Leaves)) {
						nodePair[0] = v;
						if (v.getCluster().intersects(cherrySet))
							break;
					}
				}
				if (nodePair[0] != null) {
					for (MyNode v : cherry) {
						if (!v.equals(nodePair[0])) {
							nodePair[1] = v;
							if (v.getCluster().intersects(cherrySet))
								break;
						}
					}
					chosenSet.set(nodePair[0].getCluster().nextSetBit(0));
					chosenSet.set(nodePair[1].getCluster().nextSetBit(0));
					primeSet.add(nodePair[0].getCluster());
					primeSet.add(nodePair[1].getCluster());
				}
			}

			for (HashSet<MyNode> cherry : t2Cherries) {
				MyNode[] nodePair = new MyNode[2];
				for (MyNode v : cherry) {
					if (isTerminalNode(v, t1Leaves)) {
						nodePair[0] = v;
						if (v.getCluster().intersects(chosenSet))
							break;
					}
				}
				if (nodePair[0] != null) {
					for (MyNode v : cherry) {
						if (!v.equals(nodePair[0])) {
							nodePair[1] = v;
							if (v.getCluster().intersects(chosenSet))
								break;
						}
					}
					primeSet.add(nodePair[0].getCluster());
					primeSet.add(nodePair[1].getCluster());
				}
			}

		}

		return primeSet;
	}

	private HashSet<BitSet> searchConflict2Cluster(HashSet<BitSet> terminalSet, MyNode[] t1Leaves, MyNode[] t2Leaves) {

		HashSet<BitSet> primeSet = new HashSet<BitSet>();
		for (BitSet termCluster : terminalSet) {

			MyNode v1 = getLeaf(t1Leaves, termCluster);
			MyNode p1 = v1.getFirstInEdge().getSource();
			BitSet p1Cluster = p1.getCluster();
			MyNode v2 = getLeaf(t2Leaves, termCluster);
			MyNode p2 = v2.getFirstInEdge().getSource();
			BitSet p2Cluster = p2.getCluster();
			if (p1Cluster.cardinality() == 2 && p2Cluster.cardinality() == 2) {
				BitSet pCluster = (BitSet) p1Cluster.clone();
				pCluster.or(p2Cluster);
				int i = pCluster.nextSetBit(0);
				while (i != -1) {
					BitSet b = new BitSet(taxaOrdering.size());
					b.set(i);
					primeSet.add(b);
					i = pCluster.nextSetBit(i + 1);
				}
				break;
			}

		}
		return primeSet;
	}

	private HashSet<HashSet<MyNode>> getCherries(MyPhyloTree t) {
		HashSet<HashSet<MyNode>> cherries = new HashSet<HashSet<MyNode>>();
		HashSet<MyNode> visited = new HashSet<MyNode>();
		for (MyNode v : t.getLeaves()) {
			MyNode p = v.getFirstInEdge().getSource();
			if (!visited.contains(p)) {
				visited.add(p);
				if (isCherry(p)) {
					HashSet<MyNode> cherry = new HashSet<MyNode>();
					Iterator<MyEdge> it = p.outEdges().iterator();
					while (it.hasNext())
						cherry.add(it.next().getTarget());
					cherries.add(cherry);
				}
			}
		}
		return cherries;
	}

	private boolean isCherry(MyNode v) {
		Iterator<MyEdge> it = v.outEdges().iterator();
		while (it.hasNext()) {
			if (it.next().getTarget().getOutDegree() != 0)
				return false;
		}
		return true;
	}

	private boolean isTerminalNode(MyNode v1, MyNode[] t2Leaves) {
		BitSet b = v1.getCluster();
		MyNode v2 = getLeaf(t2Leaves, b);
		MyNode p1 = v1.getFirstInEdge().getSource();
		MyNode p2 = v2.getFirstInEdge().getSource();
		BitSet b1 = (BitSet) p1.getCluster().clone();
		BitSet b2 = (BitSet) p2.getCluster().clone();
		b1.xor(b);
		b2.xor(b);
		return !b1.intersects(b2);
	}

	private void collapseST(MyPhyloTree t1, MyPhyloTree t2, MyNode[] t1Leaves, MyNode[] t2Leaves) {

		HashSet<BitSet> stSets = cmpSTNodes(t1, t2, t2Leaves);

		for (BitSet stSet : stSets) {

			// this.printCluster(stSet);
			// System.out.println(t1 + ";");
			// System.out.println(t2 + ";");

			HashSet<MyNode> contractedNodes = new HashSet<MyNode>();
			HashSet<MyNode> endNodes = new HashSet<MyNode>();
			int i = stSet.nextSetBit(0);
			while (i != -1) {
				BitSet b = new BitSet(taxaOrdering.size());
				b.set(i);
				contractLeafNode(getLeaf(t1Leaves, b), contractedNodes, endNodes);
				contractLeafNode(getLeaf(t2Leaves, b), contractedNodes, endNodes);
				i = stSet.nextSetBit(i + 1);
			}

			for (MyNode v : endNodes) {
				MyNode w = v.getOwner().newNode();
				w.setInfo(v.getInfo());
				v.setInfo(new BitSet(taxaOrdering.size()));
				w.setLabel(v.getLabel());
				v.setLabel("");
				v.getOwner().newEdge(v, w);
			}

		}

	}

	private void contractLeafNode(MyNode v, HashSet<MyNode> contractedNodes, HashSet<MyNode> endNodes) {
		if (v.getInDegree() != 0) {
			BitSet vSet = (BitSet) v.getInfo();
			MyEdge e = v.getFirstInEdge();
			MyNode p = e.getSource();
			p.getOwner().deleteEdge(e);
			p.removeOutEdge(e);
			p.setLabel(createUniqueLabel(p.getLabel(), v.getLabel()));
			((BitSet) p.getInfo()).or(vSet);
			if (p.getOutDegree() == 0) {
				contractedNodes.add(p);
				if (endNodes.contains(p))
					endNodes.remove(p);
				if (p.getInDegree() > 0)
					contractNodesRec(p.getFirstInEdge().getSource(), contractedNodes, endNodes);
			} else
				contractNodesRec(p, contractedNodes, endNodes);
		}
	}

	private void contractNodesRec(MyNode v, HashSet<MyNode> contractedNodes, HashSet<MyNode> endNodes) {
		Iterator<MyEdge> it = v.outEdges().iterator();
		Vector<MyNode> conChildren = new Vector<MyNode>();
		while (it.hasNext()) {
			MyNode c = it.next().getTarget();
			if (contractedNodes.contains(c))
				conChildren.add(c);
		}
		if (!conChildren.isEmpty()) {
			for (MyNode c : conChildren) {
				contractedNodes.remove(c);
				contractLeafNode(c, contractedNodes, endNodes);
			}
		} else if (conChildren.isEmpty() && v.getOutDegree() != 0)
			endNodes.add(v);
	}

	private String createUniqueLabel(String s1, String s2) {

		if (s1.isEmpty())
			return s2;
		if (s2.isEmpty())
			return s1;
		int c1 = Integer.parseInt(s1);
		int c2 = Integer.parseInt(s2);
		Vector<Integer> vec = new Vector<Integer>();
		vec.add(c1);
		vec.add(c2);
		Collections.sort(vec);

		return vec.firstElement() + "";
	}

	private HashSet<BitSet> cmpSTNodes(MyPhyloTree t1, MyPhyloTree t2, MyNode[] t2Leaves) {

		HashSet<HashSet<MyNode>> stNodeSet = new HashSet<HashSet<MyNode>>();
		HashSet<MyNode> stMulNodes = new HashSet<MyNode>();

		cmpSTNodesRec(t1.getRoot(), stNodeSet, stMulNodes, t2Leaves);

		HashSet<BitSet> stSets = new HashSet<BitSet>();
		for (HashSet<MyNode> stNodes : stNodeSet) {
			BitSet stSet = new BitSet(taxaOrdering.size());
			for (MyNode v : stNodes)
				stSet.or(v.getCluster());
			if (!stSet.isEmpty())
				stSets.add(stSet);
		}

		return stSets;
	}

	private HashSet<MyNode> cmpSTNodesRec(MyNode v, HashSet<HashSet<MyNode>> stNodeSet, HashSet<MyNode> stMulNodes,
										  MyNode[] t2Leaves) {

		if (v.getOutDegree() == 0) {
			HashSet<MyNode> nodes = new HashSet<MyNode>();
			nodes.add(v);
			return nodes;
		} else {

			HashSet<HashSet<MyNode>> subNodeSet = new HashSet<HashSet<MyNode>>();
			Vector<MyNode> stChildren = new Vector<MyNode>();
			Iterator<MyEdge> it = v.outEdges().iterator();
			while (it.hasNext()) {
				MyNode c = it.next().getTarget();
				HashSet<MyNode> nodes = cmpSTNodesRec(c, stNodeSet, stMulNodes, t2Leaves);
				if (!nodes.isEmpty()) {
					subNodeSet.add(nodes);
					stChildren.addAll(nodes);
				}
			}

			boolean isCompatible = false;
			HashSet<HashSet<MyNode>> refNodes = new HashSet<HashSet<MyNode>>();
			BitSet refSet = new BitSet(taxaOrdering.size());
			int size = -1;
			for (int maxSize = v.getOutDegree(); maxSize >= 2; maxSize--) {
				RefinedCluster refCluster = getRefClusters(v, stChildren, maxSize, refSet);
				for (BitSet cluster : refCluster.getAllCluster()) {
					if (!cluster.intersects(refSet) && isCompatibleLCA_Naive(cluster, t2Leaves)) {
						isCompatible = true;
						size = refCluster.getSize();
						refSet.or(cluster);
						refNodes.add(refCluster.getNodes(cluster));
					}
				}

			}

			if (!isCompatible || (isCompatible && size < v.getOutDegree())) {

				for (HashSet<MyNode> subNodes : subNodeSet) {
					HashSet<MyNode> stNodes = new HashSet<MyNode>();
					for (MyNode subNode : subNodes) {
						if (subNode.getOutDegree() > 0 && !subNode.getCluster().intersects(refSet))
							stNodes.add(subNode);
					}
					stNodeSet.add(stNodes);
				}
				if (!refNodes.isEmpty())
					stNodeSet.addAll(refNodes);
				return new HashSet<MyNode>();
			}

			if (v.getInDegree() == 0) {
				HashSet<MyNode> stNodes = new HashSet<MyNode>();
				stNodes.add(v);
				stNodeSet.add(stNodes);
			}

			HashSet<MyNode> nodes = new HashSet<MyNode>();
			nodes.add(v);
			return nodes;

		}
	}

	private RefinedCluster getRefClusters(MyNode v, Vector<MyNode> stChildren, int size, BitSet refSet) {

		Vector<MyNode> children = new Vector<MyNode>();
		for (MyNode c : stChildren) {
			BitSet b = c.getCluster();
			if (!b.intersects(refSet))
				children.add(c);
		}

		RefinedCluster refinedCluster = new RefinedCluster(size);
		getRefClustersRec(children, size, 0, 0, new BitSet(taxaOrdering.size()), new HashSet<MyNode>(), refinedCluster);

		return refinedCluster;
	}

	private void getRefClustersRec(Vector<MyNode> children, int maxSize, int size, int j, BitSet refCluster,
								   HashSet<MyNode> refNodes, RefinedCluster refinedCluster) {
		if (size < maxSize && children.size() - j >= maxSize - size) {
			for (int k = j; k < children.size(); k++) {
				MyNode c = children.get(k);
				BitSet newCluster = (BitSet) refCluster.clone();
				newCluster.or(c.getCluster());
				int newSize = size + 1;
				int newJ = k + 1;
				HashSet<MyNode> newRefNodes = (HashSet<MyNode>) refNodes.clone();
				newRefNodes.add(c);
				getRefClustersRec(children, maxSize, newSize, newJ, newCluster, newRefNodes, refinedCluster);
			}
		} else if (size == maxSize)
			refinedCluster.addCluster(refCluster, refNodes);
	}

	private boolean isCompatibleLCA_Naive(BitSet cluster, MyNode[] t2Leaves) {

		MyNode lca = cmpLCA_naive(cluster, t2Leaves);

		Iterator<MyEdge> it = lca.outEdges().iterator();
		while (it.hasNext()) {
			BitSet childCluster = it.next().getTarget().getCluster();
			BitSet b = (BitSet) childCluster.clone();
			b.and(cluster);
			if (!b.equals(childCluster) && childCluster.intersects(cluster))
				return false;
		}
		return true;
	}

	private MyNode cmpLCA_naive(BitSet cluster, MyNode[] leafArray) {
		MyNode v = leafArray[cluster.nextSetBit(0)];
		BitSet b = (BitSet) v.getCluster().clone();
		b.and(cluster);
		while (!b.equals(cluster)) {
			v = v.getFirstInEdge().getSource();
			b = (BitSet) v.getCluster().clone();
			b.and(cluster);
		}
		return v;
	}

	private void cmpClusters(MyPhyloTree t1, MyPhyloTree t2, MyNode[] t1Leaves, MyNode[] t2Leaves) {
		cmpClustersRec(t1.getRoot(), t1Leaves);
		cmpClustersRec(t2.getRoot(), t2Leaves);
	}

	private BitSet cmpClustersRec(MyNode v, MyNode[] leafArray) {
		BitSet b = new BitSet(taxaOrdering.size());
		if (v.getOutDegree() == 0) {
			int index = Integer.parseInt(v.getLabel()) - labelOffset;
			b.set(index);
		} else {
			Iterator<MyEdge> it = v.outEdges().iterator();
			while (it.hasNext())
				b.or(cmpClustersRec(it.next().getTarget(), leafArray));
		}
		v.setCluster(b);
		if (v.getOutDegree() == 0) {
			int k = b.nextSetBit(0);
			leafArray[k] = v;
		}
		return b;
	}

	private MyNode getLeaf(MyNode[] leafArray, BitSet b) {
		int k = b.nextSetBit(0);
		return leafArray[k];
	}

	private MyPhyloTree copyTree(MyPhyloTree t, MyNode[] nodePair) {
		MyPhyloTree tCopy = new MyPhyloTree();
		tCopy.getRoot().setLabel(t.getRoot().getLabel());
		copyTreeRec(t.getRoot(), tCopy.getRoot(), nodePair);
		return tCopy;
	}

	private void copyTreeRec(MyNode v, MyNode vCopy, MyNode[] nodePair) {
		Iterator<MyEdge> it = v.outEdges().iterator();
		while (it.hasNext()) {
			MyGraph tCopy = vCopy.getOwner();
			MyNode c = it.next().getTarget();
			MyNode cCopy = tCopy.newNode();
			cCopy.setLabel(c.getLabel());
			tCopy.newEdge(vCopy, cCopy);
			copyTreeRec(c, cCopy, nodePair);
		}
		if (nodePair[0] == v) {
			nodePair[1] = vCopy;
		}
		vCopy.setInfo(((BitSet) v.getInfo()).clone());
	}

	public ConcurrentHashMap<BitSet, Integer> getFailedCutSets() {
		return failedCutSets;
	}

	public int getR() {
		return r;
	}

	public void addFailedCutSet(BitSet cutSet, Integer lowerBound) {
		if (usehash()) {
			if (failedCutSets.containsKey(cutSet) && failedCutSets.get(cutSet) < lowerBound) {
				failedCutSets.put(cutSet, lowerBound);
			} else if (!failedCutSets.containsKey(cutSet))
				failedCutSets.put(cutSet, lowerBound);
		}
	}

	// For debugging **********************************************************

	private void printClusters(HashSet<BitSet> clusterSet) {
		for (BitSet b : clusterSet) {
			System.out.println();
			int i = b.nextSetBit(0);
			while (i != -1) {
				System.out.print(taxaOrdering.get(i) + " ");
				i = b.nextSetBit(i + 1);
			}
		}
	}

	private void printCluster(BitSet b) {
		int i = b.nextSetBit(0);
		while (i != -1) {
			System.out.print(taxaOrdering.get(i) + " ");
			i = b.nextSetBit(i + 1);
		}
		System.out.println();
	}

	public void stopExecution() {
		isStopped = true;
	}

}
