/*
 *   TerminalAlg.java Copyright (C) 2020 Daniel H. Huson
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
import dendroscope.hybroscale.util.lcaQueries.LCA_Query_LogN;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TerminalAlg {

	private MyPhyloTree tOne, tTwo;
	private Vector<String> taxaOrdering;
	private HashMap<String, Integer> taxonToIndex = new HashMap<String, Integer>();
	private int r;
	private boolean success = false;

	private ConcurrentHashMap<BitSet, Integer> failedCutSets = new ConcurrentHashMap<BitSet, Integer>();
	private boolean isStopped = false;

	private int recCounter = 0, termCounter = 0;
	private int caseOne = 0, caseTwo = 0, caseThree = 0, caseFour = 0;
	private long runtimeOne = 0, runtimeTwo = 0;

	public TerminalAlg(MyPhyloTree tOne, MyPhyloTree tTwo, int r, Vector<String> taxaOrdering) {
		this.tOne = tOne;
		this.tTwo = tTwo;
		this.r = r;
		this.taxaOrdering = (Vector<String>) taxaOrdering.clone();
		for (String taxon : taxaOrdering)
			taxonToIndex.put(taxon, taxaOrdering.indexOf(taxon));
	}

	public boolean run() {

		// System.out.println("Trying " + r + "... " + failedCutSets.size());

		MyPhyloTree t1 = copyTree(tOne, new MyNode[2]);
		MyPhyloTree t2 = copyTree(tTwo, new MyNode[2]);
		runRec(t1, t2, r, new BitSet(taxaOrdering.size()));

//		System.out.println(caseOne + " " + caseTwo + " " + caseThree + " " + caseFour);
//		System.out.println("RT1: " + runtimeOne + "s, RT2: " + runtimeTwo + "s ");

		// System.out.println("Result: " + r + " " +
		// success+" "+failedCutSets.size());

		return success;

	}

	private void runRec(MyPhyloTree t1, MyPhyloTree t2, int i, BitSet cutSet) {

		// int lowerBound = failedCutSets.containsKey(cutSet) ?
		// failedCutSets.get(cutSet) : -1;

		BitSet hashSet = getHashSet(t1);
		int lowerBound = failedCutSets.containsKey(hashSet) ? failedCutSets.get(hashSet) : -1;

		if (lowerBound >= i)
			termCounter++;

		if (!success && lowerBound < i && !isStopped) {

			recCounter++;

			HashMap<MyNode, BitSet> nodeToCluster = new HashMap<MyNode, BitSet>();
			HashMap<BitSet, MyNode> t1ClusterToNode = new HashMap<BitSet, MyNode>();
			HashMap<BitSet, MyNode> t2ClusterToNode = new HashMap<BitSet, MyNode>();
			HashSet<BitSet> clusterSet = new HashSet<BitSet>();

			System.out.println("\n");
			System.out.println(t1 + ";");
			System.out.println(t2 + ";");

			cmpClusters(t1, t2, nodeToCluster, t1ClusterToNode, t2ClusterToNode);

			collapseST(t1, t2, nodeToCluster, t1ClusterToNode, t2ClusterToNode, clusterSet);

			System.out.println(t1 + ";");
			System.out.println(t2 + ";");

			cmpClusters(t1, t2, nodeToCluster, t1ClusterToNode, t2ClusterToNode);

			hashSet = getHashSet(t1);
			lowerBound = failedCutSets.containsKey(hashSet) ? failedCutSets.get(hashSet) : -1;

			if (lowerBound < i) {

				if (t1.getNodes().size() == 1) {
					success = true;
				} else if (i > 0) {

					HashSet<BitSet> primeSet = assessPrimeSet(i, t1, t2, nodeToCluster, t1ClusterToNode,
							t2ClusterToNode);

					for (BitSet b : primeSet) {

						BitSet leafSet = (BitSet) t1ClusterToNode.get(b).getInfo();

						MyNode v1 = t1ClusterToNode.get(b);
						MyNode[] nodePair1 = { v1, null };
						MyPhyloTree t1Copy = copyTree(t1, nodePair1);
						deleteLeafNode(nodePair1[1]);

						MyNode v2 = t2ClusterToNode.get(b);
						MyNode[] nodePair2 = { v2, null };
						MyPhyloTree t2Copy = copyTree(t2, nodePair2);
						deleteLeafNode(nodePair2[1]);

						int iRec = i - 1;
						BitSet recCutSet = (BitSet) cutSet.clone();
						recCutSet.or(leafSet);

						runRec(t1Copy, t2Copy, iRec, recCutSet);

					}
				}

			}

		}

		if (!success && usehash()) {
			hashSet = getHashSet(t1);
			if (!failedCutSets.containsKey(hashSet) || failedCutSets.get(hashSet) < i)
				failedCutSets.put(hashSet, i);
		}

	}

	private BitSet getHashSet(MyPhyloTree t) {
		BitSet leafSet = new BitSet(taxaOrdering.size());
		for (MyNode l : t.getLeaves()) {
			int index = taxonToIndex.get(l.getLabel());
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

	private HashSet<BitSet> assessPrimeSet(int i, MyPhyloTree t1, MyPhyloTree t2,
										   HashMap<MyNode, BitSet> nodeToCluster, HashMap<BitSet, MyNode> t1ClusterToNode,
										   HashMap<BitSet, MyNode> t2ClusterToNode) {

		HashSet<BitSet> terminalSet = new HashSet<BitSet>();
		for (MyNode v : t1.getLeaves()) {
			if (isTerminalNode(v, nodeToCluster, t2ClusterToNode))
				terminalSet.add(nodeToCluster.get(v));
		}

		HashSet<BitSet> primeSet = new HashSet<BitSet>();
		if (terminalSet.size() > 3 * i) {
			caseOne++;
			return primeSet;
		}

		primeSet = searchConflict2Cluster(terminalSet, nodeToCluster, t1ClusterToNode, t2ClusterToNode);
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

			HashSet<HashSet<MyNode>> t1Cherries = getCherries(t1, nodeToCluster);
			HashSet<HashSet<MyNode>> t2Cherries = getCherries(t2, nodeToCluster);

			BitSet cherrySet = new BitSet(taxaOrdering.size());
			for (HashSet<MyNode> cherry : t2Cherries) {
				for (MyNode c : cherry)
					cherrySet.or(nodeToCluster.get(c));
			}

			BitSet chosenSet = new BitSet(taxaOrdering.size());
			for (HashSet<MyNode> cherry : t1Cherries) {
				MyNode[] nodePair = new MyNode[2];
				for (MyNode v : cherry) {
					if (isTerminalNode(v, nodeToCluster, t2ClusterToNode)) {
						nodePair[0] = v;
						if (nodeToCluster.get(v).intersects(cherrySet))
							break;
					}
				}
				if (nodePair[0] != null) {
					for (MyNode v : cherry) {
						if (!v.equals(nodePair[0])) {
							nodePair[1] = v;
							if (nodeToCluster.get(v).intersects(cherrySet))
								break;
						}
					}
					chosenSet.set(nodeToCluster.get(nodePair[0]).nextSetBit(0));
					chosenSet.set(nodeToCluster.get(nodePair[0]).nextSetBit(0));
					primeSet.add(nodeToCluster.get(nodePair[0]));
					primeSet.add(nodeToCluster.get(nodePair[1]));
				}
			}

			for (HashSet<MyNode> cherry : t2Cherries) {
				MyNode[] nodePair = new MyNode[2];
				for (MyNode v : cherry) {
					if (isTerminalNode(v, nodeToCluster, t1ClusterToNode)) {
						nodePair[0] = v;
						if (nodeToCluster.get(v).intersects(chosenSet))
							break;
					}
				}
				if (nodePair[0] != null) {
					for (MyNode v : cherry) {
						if (!v.equals(nodePair[0])) {
							nodePair[1] = v;
							if (nodeToCluster.get(v).intersects(chosenSet))
								break;
						}
					}
					primeSet.add(nodeToCluster.get(nodePair[0]));
					primeSet.add(nodeToCluster.get(nodePair[1]));
				}
			}

		}

		return primeSet;
	}

	private HashSet<BitSet> searchConflict2Cluster(HashSet<BitSet> terminalSet, HashMap<MyNode, BitSet> nodeToCluster,
												   HashMap<BitSet, MyNode> t1ClusterToNode, HashMap<BitSet, MyNode> t2ClusterToNode) {

		HashSet<BitSet> primeSet = new HashSet<BitSet>();
		for (BitSet termCluster : terminalSet) {

			MyNode v1 = t1ClusterToNode.get(termCluster);
			MyNode p1 = v1.getFirstInEdge().getSource();
			BitSet p1Cluster = nodeToCluster.get(p1);
			MyNode v2 = t2ClusterToNode.get(termCluster);
			MyNode p2 = v2.getFirstInEdge().getSource();
			BitSet p2Cluster = nodeToCluster.get(p2);
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

	private HashSet<HashSet<MyNode>> getCherries(MyPhyloTree t, HashMap<MyNode, BitSet> nodeToCluster) {
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

	private boolean isTerminalNode(MyNode v1, HashMap<MyNode, BitSet> nodeToCluster,
								   HashMap<BitSet, MyNode> t2ClusterToNode) {
		BitSet b = nodeToCluster.get(v1);
		MyNode v2 = t2ClusterToNode.get(b);
		MyNode p1 = v1.getFirstInEdge().getSource();
		MyNode p2 = v2.getFirstInEdge().getSource();
		BitSet b1 = (BitSet) nodeToCluster.get(p1).clone();
		BitSet b2 = (BitSet) nodeToCluster.get(p2).clone();
		b1.xor(b);
		b2.xor(b);
		return !b1.intersects(b2);
	}

	private void collapseST(MyPhyloTree t1, MyPhyloTree t2, HashMap<MyNode, BitSet> nodeToCluster,
							HashMap<BitSet, MyNode> t1ClusterToNode, HashMap<BitSet, MyNode> t2ClusterToNode, HashSet<BitSet> clusterSet) {

		HashSet<BitSet> stSets = cmpSTNodes(t1, t2, nodeToCluster, clusterSet, t2ClusterToNode);

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
				contractLeafNode(t1ClusterToNode.get(b), contractedNodes, endNodes);
				contractLeafNode(t2ClusterToNode.get(b), contractedNodes, endNodes);
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

	private HashSet<BitSet> cmpSTNodes(MyPhyloTree t1, MyPhyloTree t2, HashMap<MyNode, BitSet> nodeToCluster,
									   HashSet<BitSet> clusterSet, HashMap<BitSet, MyNode> t2ClusterToNode) {

		LCA_Query_LogN rmqQuery_logN = new LCA_Query_LogN(t2);
		HashSet<HashSet<MyNode>> stNodeSet = new HashSet<HashSet<MyNode>>();
		HashSet<MyNode> stMulNodes = new HashSet<MyNode>();

		cmpSTNodesRec(t1.getRoot(), nodeToCluster, stNodeSet, stMulNodes, t2ClusterToNode, rmqQuery_logN);

		HashSet<BitSet> stSets = new HashSet<BitSet>();
		for (HashSet<MyNode> stNodes : stNodeSet) {
			BitSet stSet = new BitSet(taxaOrdering.size());
			for (MyNode v : stNodes)
				stSet.or(nodeToCluster.get(v));
			if (!stSet.isEmpty())
				stSets.add(stSet);
		}

		return stSets;
	}

	private HashSet<MyNode> cmpSTNodesRec(MyNode v, HashMap<MyNode, BitSet> nodeToCluster,
										  HashSet<HashSet<MyNode>> stNodeSet, HashSet<MyNode> stMulNodes, HashMap<BitSet, MyNode> t2ClusterToNode,
										  LCA_Query_LogN rmqQuery_logN) {

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
				HashSet<MyNode> nodes = cmpSTNodesRec(c, nodeToCluster, stNodeSet, stMulNodes, t2ClusterToNode,
						rmqQuery_logN);
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
				RefinedCluster refCluster = getRefClusters(v, stChildren, nodeToCluster, maxSize, refSet);
				for (BitSet cluster : refCluster.getAllCluster()) {
					if (!cluster.intersects(refSet)
							&& isCompatibleLCA(cluster, t2ClusterToNode, rmqQuery_logN, nodeToCluster)) {
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
						if (subNode.getOutDegree() > 0 && !nodeToCluster.get(subNode).intersects(refSet))
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

	private RefinedCluster getRefClusters(MyNode v, Vector<MyNode> stChildren, HashMap<MyNode, BitSet> nodeToCluster,
										  int size, BitSet refSet) {

		Vector<MyNode> children = new Vector<MyNode>();
		for (MyNode c : stChildren) {
			BitSet b = nodeToCluster.get(c);
			if (!b.intersects(refSet))
				children.add(c);
		}

		RefinedCluster refinedCluster = new RefinedCluster(size);
		getRefClustersRec(children, nodeToCluster, size, 0, 0, new BitSet(taxaOrdering.size()), new HashSet<MyNode>(),
				refinedCluster);

		return refinedCluster;
	}

	private void getRefClustersRec(Vector<MyNode> children, HashMap<MyNode, BitSet> nodeToCluster, int maxSize,
								   int size, int j, BitSet refCluster, HashSet<MyNode> refNodes, RefinedCluster refinedCluster) {
		if (size < maxSize && children.size() - j >= maxSize - size) {
			for (int k = j; k < children.size(); k++) {
				MyNode c = children.get(k);
				BitSet newCluster = (BitSet) refCluster.clone();
				newCluster.or(nodeToCluster.get(c));
				int newSize = size + 1;
				int newJ = k + 1;
				HashSet<MyNode> newRefNodes = (HashSet<MyNode>) refNodes.clone();
				newRefNodes.add(c);
				getRefClustersRec(children, nodeToCluster, maxSize, newSize, newJ, newCluster, newRefNodes,
						refinedCluster);
			}
		} else if (size == maxSize)
			refinedCluster.addCluster(refCluster, refNodes);
	}

	private boolean isCompatibleLCA(BitSet cluster, HashMap<BitSet, MyNode> t2ClusterToNode,
									LCA_Query_LogN rmqQuery_logN, HashMap<MyNode, BitSet> nodeToCluster) {
		int i = cluster.nextSetBit(0);
		HashSet<MyNode> t2Nodes = new HashSet<MyNode>();
		while (i != -1) {
			BitSet leafCluster = new BitSet(taxaOrdering.size());
			leafCluster.set(i);
			t2Nodes.add(t2ClusterToNode.get(leafCluster));
			i = cluster.nextSetBit(i + 1);
		}
		MyNode lca = rmqQuery_logN.cmpLCA(t2Nodes);

		Iterator<MyEdge> it = lca.outEdges().iterator();
		while (it.hasNext()) {
			BitSet childCluster = nodeToCluster.get(it.next().getTarget());
			BitSet b = (BitSet) childCluster.clone();
			b.and(cluster);
			if (!b.equals(childCluster) && childCluster.intersects(cluster))
				return false;
		}
		return true;
	}

	private void cmpClusters(MyPhyloTree t1, MyPhyloTree t2, HashMap<MyNode, BitSet> nodeToCluster,
							 HashMap<BitSet, MyNode> t1ClusterToNode, HashMap<BitSet, MyNode> t2ClusterToNode) {
		cmpClustersRec(t1.getRoot(), nodeToCluster, t1ClusterToNode);
		cmpClustersRec(t2.getRoot(), nodeToCluster, t2ClusterToNode);
	}

	private BitSet cmpClustersRec(MyNode v, HashMap<MyNode, BitSet> nodeToCluster, HashMap<BitSet, MyNode> clusterToNode) {
		BitSet b = new BitSet(taxaOrdering.size());
		if (v.getOutDegree() == 0) {
			int index = taxonToIndex.get(v.getLabel());
			b.set(index);
			// b.set(taxaOrdering.indexOf(v.getLabel()));
		} else {
			Iterator<MyEdge> it = v.outEdges().iterator();
			while (it.hasNext())
				b.or(cmpClustersRec(it.next().getTarget(), nodeToCluster, clusterToNode));
		}
		nodeToCluster.put(v, b);
		clusterToNode.put(b, v);
		return b;
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
		if (nodePair[0] == v)
			nodePair[1] = vCopy;
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
