package dendroscope.hybroscale.model.attachNetworks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import dendroscope.hybroscale.model.HybridManager;
import dendroscope.hybroscale.model.cmpMinNetworks.NetworkIsomorphismCheck;
import dendroscope.hybroscale.model.parallelization.MyNetPriorThreadPool;
import dendroscope.hybroscale.model.reductionSteps.ReplacementInfo;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.model.util.CheckConstraints;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

public class ReattachNetwork extends Thread {

	private MyPhyloTree n;
	private MyPhyloTree toCopy;
	private MyNode leaf;
	private boolean isClusterNetwork;
	private ReattachClustersPara manager;
	private ReplacementInfo rI;
	private HybridManager hM;
	private CheckConstraints checker;
	private Vector<String> taxaOrdering;
	private int numOfInputTrees;
	private MyNetPriorThreadPool myThreadPool;
	private MyPhyloTree[] trees;

	public ReattachNetwork(MyPhyloTree n, MyPhyloTree toCopy, MyNode leaf, boolean isClusterNetwork,
			ReattachClustersPara manager, ReplacementInfo rI, HybridManager hM, CheckConstraints checker,
			Vector<String> taxaOrdering, int numOfInputTrees, MyNetPriorThreadPool myThreadPool, MyPhyloTree[] trees) {
		this.n = n;
		this.toCopy = toCopy;
		this.leaf = leaf;
		this.isClusterNetwork = isClusterNetwork;
		this.manager = manager;
		this.rI = rI;
		this.hM = hM;
		this.checker = checker;
		this.taxaOrdering = taxaOrdering;
		this.numOfInputTrees = numOfInputTrees;
		this.myThreadPool = myThreadPool;
		this.trees = trees;
	}

	public void run() {

		manager.reportStarting(this);

		// attach root of the attached network
		MyNode vCopy = n.newNode(toCopy.getRoot());
		n.setLabel(vCopy, toCopy.getLabel(toCopy.getRoot()));

		// attaching the generated root
		Iterator<MyEdge> it = leaf.getInEdges();
		while (it.hasNext()) {
			MyEdge e = it.next();
			boolean isSpecial = n.isSpecial(e);
			MyNode parent = e.getSource();
			MyEdge eCopy = n.newEdge(parent, vCopy);

			if (isSpecial) {
				n.setSpecial(eCopy, true);
				n.setWeight(eCopy, 0);
			}

			eCopy.setInfo(e.getInfo());

		}

		// adding attached network
		addNetworkToNetworkRec(vCopy, toCopy.getRoot(), toCopy, n, new ConcurrentHashMap<MyNode, MyNode>(),
				isClusterNetwork, null, null);

		// delete leaf (leaf is now replaced by a resolved network)
		n.deleteNode(leaf);

		if (!rI.containsClusterNetwork()) {

			// System.out.println("****************\n" +
			// n.toMyBracketString() +
			// "\n");
			// System.out.println(n.toMyBracketString());

			// shifting edges down between clusters
			HashMap<MyPhyloTree, MyNode> shiftedDownNetworks = new HashMap<MyPhyloTree, MyNode>();
			HashMap<MyPhyloTree, HashMap<MyEdge, MyNode>> networkToShiftedEdges = new HashMap<MyPhyloTree, HashMap<MyEdge, MyNode>>();

			shiftEdgesDown(n, vCopy, new HashSet<MyEdge>(), new HashMap<MyEdge, MyNode>(), shiftedDownNetworks,
					isClusterNetwork, networkToShiftedEdges);

			// if (shiftedDownNetworks.keySet().size() > 1) {
			// System.out.println("+++++++++++");
			// for (MyPhyloTree net : shiftedDownNetworks.keySet())
			// System.out.println(net.toMyBracketString());
			// System.out.println("+++++++++++");
			// }

			// shifting edges upwards between clusters
			HashMap<MyPhyloTree, MyNode> shiftedUpNetworks = new HashMap<MyPhyloTree, MyNode>();
			for (MyPhyloTree net : shiftedDownNetworks.keySet())
				shiftEdgesUp(net, shiftedDownNetworks.get(net), new HashSet<MyEdge>(), networkToShiftedEdges.get(net),
						shiftedUpNetworks, isClusterNetwork);

			networkToShiftedEdges = null;
			shiftedDownNetworks = null;

			Vector<MyPhyloTree> shiftedNetworks = new Vector<MyPhyloTree>();
			shiftedNetworks.addAll(shiftedUpNetworks.keySet());

//			 if (shiftedUpNetworks.keySet().size() > 1) {
//			 System.out.println("-----------");
//			 for (MyPhyloTree net : shiftedUpNetworks.keySet())
//			 System.out.println(net.toMyBracketString());
//			 System.out.println("-----------");
//			 }

			shiftedUpNetworks = null;

			// System.out.println("\n" + n.toMyBracketString());
			// for (HybridNetwork net : shiftedUpNetworks.keySet())
			// System.out.println(net.toMyBracketString());
			// System.out.println();

			Vector<MyPhyloTree> multipleNetworks = new Vector<MyPhyloTree>();
			for (int i = 0; i < shiftedNetworks.size() - 1; i++) {
				MyPhyloTree net1 = shiftedNetworks.get(i);
				SparseNetwork n1 = new SparseNetwork(net1);
				for (int j = i + 1; j < shiftedNetworks.size(); j++) {
					MyPhyloTree net2 = shiftedNetworks.get(j);
					SparseNetwork n2 = new SparseNetwork(net2);
					if (!net1.equals(net2) && new NetworkIsomorphismCheck().run(n1, n2)) {
						multipleNetworks.add(net1);
						break;
					}
				}
			}

			for (MyPhyloTree net : multipleNetworks)
				shiftedNetworks.remove(net);

			multipleNetworks = null;

			manager.increaseNumOfShiftNets(shiftedNetworks.size() - 1);

			if (checker.doCheckAddTaxa() || checker.doCheckTime()) {
				boolean hasReplacementChar = false;
				for (MyNode v : shiftedNetworks.firstElement().getLeaves()) {
					if (rI.getReplacementLabels().contains(v.getLabel())) {
						hasReplacementChar = true;
						break;
					}
				}
				if (!hasReplacementChar) {
					for (MyPhyloTree net : shiftedNetworks) {
						if (checker.doCheckAddTaxa()) {
							int addTaxaDegree = checker.estimateAddTaxaDegree(new SparseNetwork(net),
									hM.getAddTaxaValue(), false, -1);
							net.setAddTaxaDegree(addTaxaDegree);
							hM.updateAddTaxaValue(addTaxaDegree);
						}
						if (checker.doCheckTime()) {
							int timeConsDegree = checker.estimateTimeConsistencyDegree(new SparseNetwork(net),
									hM.getTimeConsValue(), false, -1);
							net.setTimeDegree(timeConsDegree);
							hM.updateTimeConsValue(timeConsDegree);
						}
					}
				}
			}

			for (MyPhyloTree shiftNet : shiftedNetworks)
				myThreadPool.submit(new NetworkGenerator(new MyPhyloTree(shiftNet), taxaOrdering, rI, numOfInputTrees,
						checker, manager, hM, myThreadPool, trees));

		} else {

			myThreadPool.submit(new NetworkGenerator(new MyPhyloTree(n), taxaOrdering, rI, numOfInputTrees, checker,
					manager, hM, myThreadPool, trees));

		}

		manager.reportFinishing(this);

	}

	private void shiftEdgesDown(MyPhyloTree n, MyNode v, HashSet<MyEdge> visitedEdges,
			HashMap<MyEdge, MyNode> shiftedEdges, HashMap<MyPhyloTree, MyNode> shiftedDownNetworks,
			boolean isClusterNetwork, HashMap<MyPhyloTree, HashMap<MyEdge, MyNode>> networkToShiftedEdges) {

		// System.out.println(n.toMyBracketString()); // + "\n"); +
		// v.getLabel());
		// for (MyEdge e : visitedEdges)
		// System.out.println(e.getSource().getLabel() + "-V->" +
		// e.getTarget().getLabel() + " "
		// + e.getOwner().equals(n));

		MyNode p = v.getInEdges().next().getSource();
		Iterator<MyEdge> it = p.getOutEdges();
		HashSet<MyEdge> shiftingEdges = new HashSet<MyEdge>();
		HashSet<MyNode> sourceNodes = new HashSet<MyNode>();
		while (it.hasNext()) {
			MyEdge e = it.next();
			if (e.getOwner().equals(n) && n.isSpecial(e) && !visitedEdges.contains(e)) {
				HashSet<Integer> treeIndices = (HashSet<Integer>) e.getInfo();
				MyNode s = v;

				while (true) {

					if (getTreeIndexOutDegree(s, treeIndices, shiftedEdges, p) <= 1) {
						sourceNodes.add(s);
						Iterator<MyEdge> itOut = s.getOutEdges();
						while (itOut.hasNext()) {
							MyEdge eOut = itOut.next();
							if (((HashSet<Integer>) eOut.getInfo()).containsAll(treeIndices)
									&& !shiftedEdges.containsKey(eOut))
								s = eOut.getTarget();
						}
					} else if (s.getInDegree() == 1 && s.getOutDegree() > 2 && !s.equals(v)) {
						Iterator<MyEdge> itOut = s.getOutEdges();
						while (itOut.hasNext()) {
							MyEdge eOut = itOut.next();
							boolean addNode = true;
							for (int index : ((HashSet<Integer>) eOut.getInfo())) {
								if (treeIndices.contains(index))
									addNode = false;
							}
							if (addNode)
								sourceNodes.add(s);
						}
						break;
					} else
						break;
				}
				if (!sourceNodes.isEmpty()) {
					shiftingEdges.add(e);
					break;
				}
			}
		}

		if (shiftingEdges.isEmpty()) {
			shiftedDownNetworks.put(n, v);
			networkToShiftedEdges.put(n, shiftedEdges);
		} else {
			HashSet<MyEdge> newVisitedEdges = (HashSet<MyEdge>) visitedEdges.clone();
			newVisitedEdges.add(shiftingEdges.iterator().next());
			shiftEdgesDown(n, v, newVisitedEdges, (HashMap<MyEdge, MyNode>) shiftedEdges.clone(), shiftedDownNetworks,
					isClusterNetwork, networkToShiftedEdges);
		}

		for (MyEdge e : shiftingEdges) {

			for (MyNode x : sourceNodes) {

				Vector<MyEdge[]> edgePairs = new Vector<MyEdge[]>();
				MyEdge[] edgePair = { e, null };
				edgePairs.add(edgePair);
				for (MyEdge eV : visitedEdges) {
					MyEdge[] edgePair2 = { eV, null };
					edgePairs.add(edgePair2);
				}
				for (MyEdge eS : shiftedEdges.keySet()) {
					MyEdge[] edgePair2 = { eS, null };
					edgePairs.add(edgePair2);
				}

				Vector<MyNode[]> nodePairs = new Vector<MyNode[]>();
				MyNode[] nodePair = { x, null };
				nodePairs.add(nodePair);
				MyNode[] nodePair2 = { p, null };
				nodePairs.add(nodePair2);
				MyNode[] nodePair3 = { v, null };
				nodePairs.add(nodePair3);
				for (MyNode vS : shiftedEdges.values()) {
					MyNode[] nodePair4 = { vS, null };
					nodePairs.add(nodePair4);
				}

				MyPhyloTree nCopy = new MyPhyloTree();
				addNetworkToNetworkRec(nCopy.getRoot(), n.getRoot(), n, nCopy, new ConcurrentHashMap<MyNode, MyNode>(),
						isClusterNetwork, edgePairs, nodePairs);

				MyEdge eCopy = edgePairs.get(0)[1];
				HashSet<MyEdge> visitedEdgesCopy = new HashSet<MyEdge>();
				for (int i = 1; i < visitedEdges.size() + 1; i++)
					visitedEdgesCopy.add(edgePairs.get(i)[1]);
				HashMap<MyEdge, MyNode> shiftedEdgesCopy = new HashMap<MyEdge, MyNode>();
				int counter = 3;
				for (int i = visitedEdges.size() + 1; i < edgePairs.size(); i++) {
					shiftedEdgesCopy.put(edgePairs.get(i)[1], nodePairs.get(counter)[1]);
					counter++;
				}
				MyNode xCopy = nodePairs.get(0)[1];
				MyNode pCopy = nodePairs.get(1)[1];
				MyNode vCopy = nodePairs.get(2)[1];

				HashMap<MyPhyloTree, MyNode> newNetworks = new HashMap<MyPhyloTree, MyNode>();
				HashMap<MyPhyloTree, HashSet<MyEdge>> newVisitedEdges = new HashMap<MyPhyloTree, HashSet<MyEdge>>();
				HashMap<MyPhyloTree, HashMap<MyEdge, MyNode>> newShiftedEdges = new HashMap<MyPhyloTree, HashMap<MyEdge, MyNode>>();
				HashMap<MyPhyloTree, MyEdge> networkToShiftedEdge = new HashMap<MyPhyloTree, MyEdge>();
				if (getTreeIndexOutDegree(xCopy, getEdgeIndices(eCopy), shiftedEdgesCopy, pCopy) == 1) {

					MyNode tCopy = eCopy.getTarget();
					HashSet<Integer> eCopyIndices = getEdgeIndices(e);
					// eCopy.getSource().removeOutEdge(eCopy);
					nCopy.deleteEdge(eCopy);
					MyEdge newEdge = nCopy.newEdge(xCopy, tCopy);
					setEdgeIndices(newEdge, (HashSet<Integer>) eCopyIndices.clone());
					nCopy.setSpecial(newEdge, true);
					nCopy.setWeight(newEdge, 0);
					newEdge.setShifted(true);
					shiftedEdgesCopy.put(newEdge, pCopy);
					networkToShiftedEdge.put(nCopy, newEdge);

					if (pCopy.getInDegree() == 1 && pCopy.getOutDegree() == 1) {

						MyNode s = pCopy.getInEdges().next().getSource();
						MyNode t = pCopy.getOutEdges().next().getTarget();
						HashSet<Integer> eDeleteIndices = getEdgeIndices(pCopy.getInEdges().next());
						nCopy.deleteEdge(pCopy.getInEdges().next());
						nCopy.deleteEdge(pCopy.getOutEdges().next());
						nCopy.deleteNode(pCopy);
						newEdge = nCopy.newEdge(s, t);
						setEdgeIndices(newEdge, (HashSet<Integer>) eDeleteIndices.clone());
						shiftedDownNetworks.remove(n);

					}

					newNetworks.put(nCopy, vCopy);
					newVisitedEdges.put(nCopy, visitedEdgesCopy);
					newShiftedEdges.put(nCopy, shiftedEdgesCopy);

				} else if (getTreeIndexOutDegree(xCopy, getEdgeIndices(eCopy), shiftedEdgesCopy, pCopy) > 1) {

					MyEdge midEdge = xCopy.getInEdges().next();
					MyNode midSource = midEdge.getSource();
					HashSet<Integer> midEdgeIndices = getEdgeIndices(midEdge);
					nCopy.deleteEdge(midEdge);
					MyNode midNode = new MyNode(nCopy, xCopy.getLabel());
					MyEdge newEdge = nCopy.newEdge(midSource, midNode);
					setEdgeIndices(newEdge, (HashSet<Integer>) midEdgeIndices.clone());
					newEdge = nCopy.newEdge(midNode, xCopy);
					setEdgeIndices(newEdge, (HashSet<Integer>) midEdgeIndices.clone());

					MyNode tCopy = eCopy.getTarget();
					HashSet<Integer> eCopyIndices = getEdgeIndices(e);
					// eCopy.getSource().removeOutEdge(eCopy);
					nCopy.deleteEdge(eCopy);
					newEdge = nCopy.newEdge(midNode, tCopy);
					setEdgeIndices(newEdge, (HashSet<Integer>) eCopyIndices.clone());
					nCopy.setSpecial(newEdge, true);
					nCopy.setWeight(newEdge, 0);
					newEdge.setShifted(true);
					shiftedEdgesCopy.put(newEdge, pCopy);
					networkToShiftedEdge.put(nCopy, newEdge);

					if (pCopy.getInDegree() == 1 && pCopy.getOutDegree() == 1) {

						MyNode s = pCopy.getInEdges().next().getSource();
						MyNode t = pCopy.getOutEdges().next().getTarget();
						HashSet<Integer> eDeleteIndices = getEdgeIndices(pCopy.getInEdges().next());
						nCopy.deleteEdge(pCopy.getInEdges().next());
						nCopy.deleteEdge(pCopy.getOutEdges().next());
						nCopy.deleteNode(pCopy);
						newEdge = nCopy.newEdge(s, t);
						setEdgeIndices(newEdge, (HashSet<Integer>) eDeleteIndices.clone());

					}

					Vector<MyEdge> moveEdges = new Vector<MyEdge>();
					it = xCopy.getOutEdges();
					while (it.hasNext()) {
						MyEdge eOut = it.next();
						boolean addEdge = true;
						for (int index : ((HashSet<Integer>) eOut.getInfo())) {
							if (eCopyIndices.contains(index))
								addEdge = false;
						}
						if (addEdge)
							moveEdges.add(eOut);
					}

					shiftEdgesRec(midNode, moveEdges, 0, nCopy, newNetworks, vCopy, newVisitedEdges, visitedEdgesCopy,
							newShiftedEdges, shiftedEdgesCopy, networkToShiftedEdge, isClusterNetwork);

				}

				for (MyPhyloTree newNetwork : newNetworks.keySet()) {

					shiftEdgesDown(newNetwork, newNetworks.get(newNetwork), newVisitedEdges.get(newNetwork),
							newShiftedEdges.get(newNetwork), shiftedDownNetworks, isClusterNetwork,
							networkToShiftedEdges);

					MyEdge shiftedEdge = networkToShiftedEdge.get(newNetwork);
					cmpHangingNodes(shiftedEdge.getSource(), getEdgeIndices(shiftedEdge), shiftedEdge, newNetwork,
							newNetworks.get(newNetwork), newVisitedEdges.get(newNetwork),
							newShiftedEdges.get(newNetwork), isClusterNetwork, true, shiftedDownNetworks,
							networkToShiftedEdges);

				}

				shiftedEdgesCopy = null;
				newNetworks = null;
				newVisitedEdges = null;
				newShiftedEdges = null;
				networkToShiftedEdge = null;

			}
		}

	}

	private HashSet<Integer> getEdgeIndices(MyEdge e) {
		return (HashSet<Integer>) e.getInfo();
	}

	private void shiftEdgesRec(MyNode s, Vector<MyEdge> edges, int index, MyPhyloTree n,
			HashMap<MyPhyloTree, MyNode> newNetworks, MyNode v, HashMap<MyPhyloTree, HashSet<MyEdge>> newVisitedNodes,
			HashSet<MyEdge> visitedEdges, HashMap<MyPhyloTree, HashMap<MyEdge, MyNode>> newShiftedEdges,
			HashMap<MyEdge, MyNode> shiftedEdges, HashMap<MyPhyloTree, MyEdge> networkToShiftedEdge,
			boolean isClusterNetwork) {

		if (index < edges.size()) {

			for (int k = index; k < edges.size(); k++) {

				MyEdge shiftEdge = edges.get(k);
				Vector<MyEdge[]> edgePairs = new Vector<MyEdge[]>();
				MyEdge[] edgePair = { shiftEdge, null };
				edgePairs.add(edgePair);
				MyEdge[] edgePair1 = { networkToShiftedEdge.get(n), null };
				edgePairs.add(edgePair1);
				for (MyEdge e : edges) {
					MyEdge[] edgePair2 = { e, null };
					edgePairs.add(edgePair2);
				}
				int k2 = edgePairs.size();
				for (MyEdge e : visitedEdges) {
					MyEdge[] edgePair2 = { e, null };
					edgePairs.add(edgePair2);
				}
				for (MyEdge e : shiftedEdges.keySet()) {
					MyEdge[] edgePair2 = { e, null };
					edgePairs.add(edgePair2);
				}

				Vector<MyNode[]> nodePairs = new Vector<MyNode[]>();
				MyNode[] nodePair = { s, null };
				nodePairs.add(nodePair);
				MyNode[] nodePair2 = { v, null };
				nodePairs.add(nodePair2);
				for (MyNode vS : shiftedEdges.values()) {
					MyNode[] nodePair3 = { vS, null };
					nodePairs.add(nodePair3);
				}

				MyPhyloTree nCopy = new MyPhyloTree();
				addNetworkToNetworkRec(nCopy.getRoot(), n.getRoot(), n, nCopy, new ConcurrentHashMap<MyNode, MyNode>(),
						isClusterNetwork, edgePairs, nodePairs);

				MyEdge shiftEdgeCopy = edgePairs.get(0)[1];
				networkToShiftedEdge.put(nCopy, edgePairs.get(1)[1]);
				Vector<MyEdge> edgesCopy = new Vector<MyEdge>();
				for (int j = 2; j < k2; j++) {
					if (edgePairs.get(j)[1] != null)
						edgesCopy.add(edgePairs.get(j)[1]);
				}
				HashSet<MyEdge> visitedEdgesCopy = new HashSet<MyEdge>();
				for (int j = k2; j < k2 + visitedEdges.size(); j++) {
					if (edgePairs.get(j)[1] != null)
						visitedEdgesCopy.add(edgePairs.get(j)[1]);
				}
				HashMap<MyEdge, MyNode> shiftedEdgesCopy = new HashMap<MyEdge, MyNode>();
				int counter = 2;
				for (int i = k2 + visitedEdges.size(); i < edgePairs.size(); i++) {
					shiftedEdgesCopy.put(edgePairs.get(i)[1], nodePairs.get(counter)[1]);
					counter++;
				}
				MyNode sCopy = nodePairs.get(0)[1];
				MyNode vCopy = nodePairs.get(1)[1];

				HashSet<Integer> shiftEdgeIndices = getEdgeIndices(shiftEdgeCopy);
				MyNode tCopy = shiftEdgeCopy.getTarget();
				nCopy.deleteEdge(shiftEdgeCopy);
				MyEdge newEdge = nCopy.newEdge(sCopy, tCopy);
				setEdgeIndices(newEdge, (HashSet<Integer>) shiftEdgeIndices.clone());
				nCopy.setSpecial(newEdge, true);
				nCopy.setWeight(newEdge, 0);

				newVisitedNodes.put(nCopy, visitedEdgesCopy);
				newNetworks.put(nCopy, vCopy);
				newShiftedEdges.put(nCopy, shiftedEdgesCopy);
				int newIndex = k + 1;
				shiftEdgesRec(sCopy, edgesCopy, newIndex, nCopy, newNetworks, vCopy, newVisitedNodes, visitedEdgesCopy,
						newShiftedEdges, shiftedEdgesCopy, networkToShiftedEdge, isClusterNetwork);

				shiftedEdgesCopy = null;

			}
		}

	}

	private void shiftEdgesUp(MyPhyloTree n, MyNode v, HashSet<MyEdge> visitedEdges,
			HashMap<MyEdge, MyNode> shiftedEdges, HashMap<MyPhyloTree, MyNode> shiftedUpNetworks,
			boolean isClusterNetwork) {

		Iterator<MyEdge> it = v.getOutEdges();
		HashSet<MyEdge> shiftingEdges = new HashSet<MyEdge>();
		HashSet<MyNode> sourceNodes = new HashSet<MyNode>();
		HashMap<MyNode, MyEdge> nodeToEdge = new HashMap<MyNode, MyEdge>();
		while (it.hasNext()) {
			MyEdge e = it.next();
			Boolean[] isSpecial = { true };
			isSpecial(n, e, isSpecial);
			if (e.getOwner().equals(n) && (n.isSpecial(e) || isSpecial[0]) && !e.isShifted()
					&& !visitedEdges.contains(e)) {
				HashSet<Integer> treeIndices = getEdgeIndices(e);
				MyNode s = v;
				while (s.getInDegree() == 1) {
					MyEdge inEdge = s.getInEdges().next();
					s = inEdge.getSource();
					if (s.getInDegree() == 1) {
						if (s.getOutDegree() > 1 && getTreeIndexOutDegree(s, treeIndices, shiftedEdges, v) == 1) {
							sourceNodes.add(s);
						} else if (s.getOutDegree() > 2) {
							sourceNodes.add(s);
							nodeToEdge.put(s, inEdge);
							break;
						} else
							break;
					} else {
						boolean doBreak = true;
						Iterator<MyEdge> itIn = s.getInEdges();
						while (itIn.hasNext()) {
							MyEdge eIn = itIn.next();
							if (getEdgeIndices(eIn).containsAll(treeIndices)) {
								shiftingEdges.add(e);
								s = eIn.getSource();
								sourceNodes.add(s);
								nodeToEdge.put(s, eIn);
								// s = eIn.getSource();
								doBreak = false;
							}
						}
						if (doBreak || getTreeIndexOutDegree(s, treeIndices, shiftedEdges, v) != 1)
							break;
					}
				}
				if (!sourceNodes.isEmpty()) {
					// System.out.println(e.getSource().getLabel() +
					// " -S-> " +
					// e.getTarget().getLabel());
					shiftingEdges.add(e);
					break;
				}
			}
		}

		if (shiftingEdges.isEmpty()) {
			shiftedUpNetworks.put(n, v);
		} else {
			HashSet<MyEdge> newVisitedEdges = (HashSet<MyEdge>) visitedEdges.clone();
			newVisitedEdges.add(shiftingEdges.iterator().next());
			shiftEdgesUp(n, v, newVisitedEdges, shiftedEdges, shiftedUpNetworks, isClusterNetwork);
		}

		for (MyEdge e : shiftingEdges) {

			for (MyNode x : sourceNodes) {

				Vector<MyEdge[]> edgePairs = new Vector<MyEdge[]>();
				MyEdge[] edgePair = { e, null };
				edgePairs.add(edgePair);
				if (nodeToEdge.containsKey(x)) {
					MyEdge[] edgePair2 = { nodeToEdge.get(x), null };
					edgePairs.add(edgePair2);
				}
				for (MyEdge eV : visitedEdges) {
					MyEdge[] edgePair2 = { eV, null };
					edgePairs.add(edgePair2);
				}
				for (MyEdge eS : shiftedEdges.keySet()) {
					MyEdge[] edgePair2 = { eS, null };
					edgePairs.add(edgePair2);
				}

				Vector<MyNode[]> nodePairs = new Vector<MyNode[]>();
				MyNode[] nodePair = { v, null };
				nodePairs.add(nodePair);
				MyNode[] nodePair2 = { x, null };
				nodePairs.add(nodePair2);
				for (MyNode vS : shiftedEdges.values()) {
					MyNode[] nodePair4 = { vS, null };
					nodePairs.add(nodePair4);
				}

				MyPhyloTree nCopy = new MyPhyloTree();
				addNetworkToNetworkRec(nCopy.getRoot(), n.getRoot(), n, nCopy, new ConcurrentHashMap<MyNode, MyNode>(),
						isClusterNetwork, edgePairs, nodePairs);

				MyEdge eCopy = edgePairs.get(0)[1];
				MyNode vCopy = nodePairs.get(0)[1];
				MyNode xCopy = nodePairs.get(1)[1];

				HashMap<MyPhyloTree, MyNode> newNetworks = new HashMap<MyPhyloTree, MyNode>();
				HashMap<MyPhyloTree, HashSet<MyEdge>> newVisitedEdges = new HashMap<MyPhyloTree, HashSet<MyEdge>>();
				HashMap<MyPhyloTree, HashMap<MyEdge, MyNode>> newShiftedEdges = new HashMap<MyPhyloTree, HashMap<MyEdge, MyNode>>();
				HashMap<MyPhyloTree, MyEdge> networkToShiftedEdge = new HashMap<MyPhyloTree, MyEdge>();
				if (getTreeIndexOutDegree(xCopy, getEdgeIndices(eCopy), shiftedEdges, vCopy) == 1) {

					// System.out.println("Case A");

					HashSet<MyEdge> visitedEdgesCopy = new HashSet<MyEdge>();
					for (int i = 1; i < edgePairs.size(); i++)
						visitedEdgesCopy.add(edgePairs.get(i)[1]);
					HashMap<MyEdge, MyNode> shiftedEdgesCopy = new HashMap<MyEdge, MyNode>();
					int counter = 2;
					int offset = nodeToEdge.containsKey(x) ? 2 : 1;
					for (int i = visitedEdges.size() + offset; i < edgePairs.size(); i++) {
						shiftedEdgesCopy.put(edgePairs.get(i)[1], nodePairs.get(counter)[1]);
						counter++;
					}

					MyNode tCopy = eCopy.getTarget();
					HashSet<Integer> eCopyIndices = getEdgeIndices(e);
					nCopy.deleteEdge(eCopy);
					// eCopy.getSource().removeOutEdge(eCopy);
					MyEdge newEdge = nCopy.newEdge(xCopy, tCopy);
					setEdgeIndices(newEdge, (HashSet<Integer>) eCopyIndices.clone());
					nCopy.setSpecial(newEdge, true);
					nCopy.setWeight(newEdge, 0);
					shiftedEdgesCopy.put(newEdge, vCopy);
					networkToShiftedEdge.put(nCopy, newEdge);

					if (vCopy.getInDegree() == 1 && vCopy.getOutDegree() == 1) {

						MyNode s = vCopy.getInEdges().next().getSource();
						MyNode t = vCopy.getOutEdges().next().getTarget();
						HashSet<Integer> eDeleteIndices = getEdgeIndices(vCopy.getInEdges().next());
						nCopy.deleteEdge(vCopy.getInEdges().next());
						nCopy.deleteEdge(vCopy.getOutEdges().next());
						nCopy.deleteNode(vCopy);
						newEdge = nCopy.newEdge(s, t);
						setEdgeIndices(newEdge, (HashSet<Integer>) eDeleteIndices.clone());

						newNetworks.put(nCopy, t);
						newVisitedEdges.put(nCopy, visitedEdgesCopy);
						newShiftedEdges.put(nCopy, shiftedEdgesCopy);
					} else {
						newNetworks.put(nCopy, vCopy);
						newVisitedEdges.put(nCopy, visitedEdgesCopy);
						newShiftedEdges.put(nCopy, shiftedEdgesCopy);
					}

				} else if (nodeToEdge.containsKey(x)) {

					HashSet<MyEdge> visitedEdgesCopy = new HashSet<MyEdge>();
					for (int i = 2; i < edgePairs.size(); i++)
						visitedEdgesCopy.add(edgePairs.get(i)[1]);
					HashMap<MyEdge, MyNode> shiftedEdgesCopy = new HashMap<MyEdge, MyNode>();
					int counter = 2;
					for (int i = visitedEdges.size() + 2; i < edgePairs.size(); i++) {
						shiftedEdgesCopy.put(edgePairs.get(i)[1], nodePairs.get(counter)[1]);
						counter++;
					}

					MyEdge midEdge = edgePairs.get(1)[1];
					MyNode midTarget = midEdge.getTarget();
					nCopy.deleteEdge(midEdge);
					HashSet<Integer> midEdgeIndices = getEdgeIndices(midEdge);
					MyNode midNode = new MyNode(nCopy, xCopy.getLabel());
					MyEdge newEdge = nCopy.newEdge(xCopy, midNode);
					setEdgeIndices(newEdge, (HashSet<Integer>) midEdgeIndices.clone());
					newEdge = nCopy.newEdge(midNode, midTarget);
					setEdgeIndices(newEdge, (HashSet<Integer>) midEdgeIndices.clone());

					Vector<MyEdge> moveEdges = new Vector<MyEdge>();
					Iterator<MyEdge> itOut = xCopy.getOutEdges();
					while (itOut.hasNext()) {
						MyEdge eOut = itOut.next();
						if (eOut.isShifted() && shiftedEdgesCopy.get(eOut) != null
								&& shiftedEdgesCopy.get(eOut).equals(vCopy)) {
							moveEdges.add(eOut);
						}
					}

					if (!moveEdges.isEmpty()) {
						for (MyEdge eMove : moveEdges) {
							HashSet<Integer> eMoveIndices = getEdgeIndices(eMove);
							MyNode t = eMove.getTarget();
							nCopy.deleteEdge(eMove);
							newEdge = nCopy.newEdge(midNode, t);
							nCopy.setSpecial(newEdge, true);
							nCopy.setWeight(newEdge, 0);
							newEdge.setShifted(true);
							setEdgeIndices(newEdge, (HashSet<Integer>) eMoveIndices.clone());
							shiftedEdgesCopy.put(newEdge, midNode);
						}
						MyNode s = xCopy.getInEdges().next().getSource();
						MyNode t = xCopy.getOutEdges().next().getTarget();
						HashSet<Integer> eDeleteIndices = getEdgeIndices(xCopy.getInEdges().next());
						nCopy.deleteEdge(xCopy.getInEdges().next());
						newEdge = nCopy.newEdge(s, t);
						setEdgeIndices(newEdge, (HashSet<Integer>) eDeleteIndices.clone());
					}

					MyNode tCopy = eCopy.getTarget();
					HashSet<Integer> eCopyIndices = getEdgeIndices(e);
					// eCopy.getSource().removeOutEdge(eCopy);
					nCopy.deleteEdge(eCopy);
					newEdge = nCopy.newEdge(midNode, tCopy);
					setEdgeIndices(newEdge, (HashSet<Integer>) eCopyIndices.clone());
					nCopy.setSpecial(newEdge, true);
					nCopy.setWeight(newEdge, 0);
					newEdge.setShifted(true);
					shiftedEdgesCopy.put(newEdge, vCopy);
					networkToShiftedEdge.put(nCopy, newEdge);

					moveEdges = new Vector<MyEdge>();
					itOut = vCopy.getOutEdges();
					while (itOut.hasNext()) {
						MyEdge eOut = itOut.next();
						if (eOut.isShifted() && shiftedEdgesCopy.get(eOut) != null
								&& shiftedEdgesCopy.get(eOut).equals(xCopy)) {
							moveEdges.add(eOut);
						}
					}

					for (MyEdge eMove : moveEdges) {
						HashSet<Integer> eMoveIndices = getEdgeIndices(eMove);
						MyNode t = eMove.getTarget();
						nCopy.deleteEdge(eMove);
						newEdge = nCopy.newEdge(midNode, t);
						nCopy.setSpecial(newEdge, true);
						nCopy.setWeight(newEdge, 0);
						newEdge.setShifted(true);
						setEdgeIndices(newEdge, (HashSet<Integer>) eMoveIndices.clone());
						shiftedEdgesCopy.put(newEdge, midNode);
					}

					if (vCopy.getInDegree() == 1 && vCopy.getOutDegree() == 1) {

						MyNode s = vCopy.getInEdges().next().getSource();
						MyNode t = vCopy.getOutEdges().next().getTarget();
						HashSet<Integer> eDeleteIndices = getEdgeIndices(vCopy.getInEdges().next());
						nCopy.deleteEdge(vCopy.getInEdges().next());
						nCopy.deleteEdge(vCopy.getOutEdges().next());
						// nCopy.deleteNode(vCopy);
						newEdge = nCopy.newEdge(s, t);
						setEdgeIndices(newEdge, (HashSet<Integer>) eDeleteIndices.clone());

						vCopy = t;

					}

					if (nodeToEdge.containsKey(x) && nodeToEdge.get(x).getTarget().getInDegree() > 1) {
						newVisitedEdges.put(nCopy, visitedEdgesCopy);
						newNetworks.put(nCopy, vCopy);
						newShiftedEdges.put(nCopy, shiftedEdgesCopy);
					}

					moveEdges = new Vector<MyEdge>();
					it = xCopy.getOutEdges();
					while (it.hasNext()) {
						MyEdge eOut = it.next();
						boolean addEdge = true;
						for (int index : getEdgeIndices(eOut)) {
							if (eCopyIndices.contains(index))
								addEdge = false;
						}
						if (addEdge)
							moveEdges.add(eOut);
					}

					// System.out.println(vCopy.getLabel()+" "+xCopy.getLabel()+" "+moveEdges.size()+"\n"+nCopy.toMyBracketString());

					shiftEdgesRec(midNode, moveEdges, 0, nCopy, newNetworks, vCopy, newVisitedEdges, visitedEdgesCopy,
							newShiftedEdges, shiftedEdgesCopy, networkToShiftedEdge, isClusterNetwork);

				}

				for (MyPhyloTree newNetwork : newNetworks.keySet()) {

					shiftEdgesUp(newNetwork, newNetworks.get(newNetwork), newVisitedEdges.get(newNetwork),
							newShiftedEdges.get(newNetwork), shiftedUpNetworks, isClusterNetwork);

					MyEdge shiftedEdge = networkToShiftedEdge.get(newNetwork);
					cmpHangingNodes(shiftedEdge.getSource(), getEdgeIndices(shiftedEdge), shiftedEdge, newNetwork,
							newNetworks.get(newNetwork), newVisitedEdges.get(newNetwork),
							newShiftedEdges.get(newNetwork), isClusterNetwork, false, shiftedUpNetworks, null);

					if (shiftedEdge.getTarget().getInDegree() == 1) {
						Iterator<MyEdge> itOut = shiftedEdge.getSource().getOutEdges();
						while (itOut.hasNext()) {
							MyEdge eOut = itOut.next();
							if (eOut.isSpecial()) {
								cmpHangingNodes(eOut.getSource(), getEdgeIndices(eOut), eOut, newNetwork,
										newNetworks.get(newNetwork), newVisitedEdges.get(newNetwork),
										newShiftedEdges.get(newNetwork), isClusterNetwork, false, shiftedUpNetworks,
										null);
							}
						}
					}

				}

				newNetworks = null;
				newVisitedEdges = null;
				newShiftedEdges = null;
				networkToShiftedEdge = null;

			}
		}
	}

	private void setEdgeIndices(MyEdge e, HashSet<Integer> indices) {
		e.setInfo(indices);
	}
	
	private void addEdgeIndices(MyEdge e, HashSet<Integer> indices) {
		HashSet<Integer> eIndices = new HashSet<Integer>();
		eIndices.addAll(indices);
		eIndices.addAll(getEdgeIndices(e));
		e.setInfo(eIndices);
	}

	private void isSpecial(MyPhyloTree n, MyEdge e, Boolean[] b) {
		if (b[0]) {
			if (e.getTarget().getOutDegree() == 0) {
				b[0] = false;
			} else if (e.getOwner().equals(n) && !n.isSpecial(e)) {
				Iterator<MyEdge> it = e.getTarget().getOutEdges();
				while (it.hasNext())
					isSpecial(n, it.next(), b);
			}
		}
	}

	private void cmpHangingNodes(MyNode s, HashSet<Integer> treeIndices, MyEdge shiftedEdge, MyPhyloTree n, MyNode v,
			HashSet<MyEdge> visitedEdges, HashMap<MyEdge, MyNode> shiftedEdges, boolean isClusterNetwork,
			boolean shiftDown, HashMap<MyPhyloTree, MyNode> shiftedNetworks,
			HashMap<MyPhyloTree, HashMap<MyEdge, MyNode>> networkToShiftedEdges) {
		Iterator<MyEdge> it = s.getOutEdges();
		while (it.hasNext()) {
			MyEdge e = it.next();
			boolean isHangingEdge = true;
			for (int index : getEdgeIndices(e)) {
				if (treeIndices.contains(index))
					isHangingEdge = false;
			}
			MyNode h = e.getTarget();
			if (isHangingEdge && h.getInDegree() == 1) {

				Vector<MyEdge[]> edgePairs = new Vector<MyEdge[]>();
				MyEdge[] edgePair0 = { shiftedEdge, null };
				edgePairs.add(edgePair0);
				for (MyEdge eV : visitedEdges) {
					MyEdge[] edgePair2 = { eV, null };
					edgePairs.add(edgePair2);
				}
				for (MyEdge eS : shiftedEdges.keySet()) {
					MyEdge[] edgePair2 = { eS, null };
					edgePairs.add(edgePair2);
				}

				Vector<MyNode[]> nodePairs = new Vector<MyNode[]>();
				MyNode[] nodePair0 = { v, null };
				nodePairs.add(nodePair0);
				MyNode[] nodePair1 = { h, null };
				nodePairs.add(nodePair1);
				for (MyNode vS : shiftedEdges.values()) {
					MyNode[] nodePair4 = { vS, null };
					nodePairs.add(nodePair4);
				}

				MyPhyloTree nCopy = new MyPhyloTree();
				addNetworkToNetworkRec(nCopy.getRoot(), n.getRoot(), n, nCopy, new ConcurrentHashMap<MyNode, MyNode>(),
						isClusterNetwork, edgePairs, nodePairs);

				MyEdge shiftedEdgeCopy = edgePairs.get(0)[1];
				HashSet<MyEdge> visitedEdgesCopy = new HashSet<MyEdge>();
				for (int i = 1; i < visitedEdges.size() + 1; i++)
					visitedEdgesCopy.add(edgePairs.get(i)[1]);
				HashMap<MyEdge, MyNode> shiftedEdgesCopy = new HashMap<MyEdge, MyNode>();
				int counter = 2;
				for (int i = visitedEdges.size() + 1; i < edgePairs.size(); i++) {
					shiftedEdgesCopy.put(edgePairs.get(i)[1], nodePairs.get(counter)[1]);
					counter++;
				}
				MyNode vCopy = nodePairs.get(0)[1];
				MyNode hCopy = nodePairs.get(1)[1];

				HashSet<Integer> shiftedEdgeIndices = getEdgeIndices(shiftedEdgeCopy);
				MyNode t = shiftedEdgeCopy.getTarget();
				nCopy.deleteEdge(shiftedEdgeCopy);
				MyEdge newEdge = nCopy.newEdge(hCopy, t);
				nCopy.setSpecial(newEdge, true);
				nCopy.setWeight(newEdge, 0);
				newEdge.setShifted(true);
				shiftedEdgesCopy.put(newEdge, vCopy);
				setEdgeIndices(newEdge, (HashSet<Integer>) shiftedEdgeIndices.clone());
				addEdgeIndices(hCopy.getInEdges().next(), (HashSet<Integer>) shiftedEdgeIndices.clone());

				if (shiftDown)
					shiftEdgesDown(nCopy, vCopy, visitedEdgesCopy, shiftedEdgesCopy, shiftedNetworks, isClusterNetwork,
							networkToShiftedEdges);
				else
					shiftEdgesUp(nCopy, vCopy, visitedEdgesCopy, shiftedEdgesCopy, shiftedNetworks, isClusterNetwork);

				cmpHangingNodes(h, treeIndices, shiftedEdge, n, v, visitedEdges, shiftedEdges, isClusterNetwork,
						shiftDown, shiftedNetworks, networkToShiftedEdges);

			}
		}
	}

	private int getTreeIndexOutDegree(MyNode v, HashSet<Integer> treeIndices, HashMap<MyEdge, MyNode> shiftedEdges,
			MyNode rootNode) {
		int outDegree = 0;
		Iterator<MyEdge> it = v.getOutEdges();
		while (it.hasNext()) {
			MyEdge e = it.next();
			boolean b = shiftedEdges.containsKey(e) ? !rootNode.equals(shiftedEdges.get(e)) : true;
			// if (b && tM.getEdgeIndices(e).containsAll(treeIndices))
			// outDegree++;
			for (int treeIndex : treeIndices) {
				if (b && ((HashSet<Integer>) e.getInfo()).contains(treeIndex)) {
					outDegree++;
					break;
				}
			}
		}
		return outDegree;
	}

	// simple recursion that adds all nodes and edges of a resolved network
	private void addNetworkToNetworkRec(MyNode vCopy, MyNode v, MyPhyloTree toCopy, MyPhyloTree n,
			ConcurrentHashMap<MyNode, MyNode> created, boolean isClusterNetwork, Vector<MyEdge[]> edgePairs,
			Vector<MyNode[]> nodePairs) {
		Iterator<MyEdge> it = toCopy.getOutEdges(v);
		while (it.hasNext()) {
			MyEdge e = it.next();
			MyNode c = e.getTarget();
			MyNode cCopy;
			MyEdge eCopy;
			if (!created.containsKey(c)) {
				cCopy = n.newNode(c);
				n.setLabel(cCopy, toCopy.getLabel(c));
				created.put(c, cCopy);
				eCopy = n.newEdge(vCopy, cCopy);
				eCopy.setShifted(e.isShifted());
				eCopy.setInfo(e.getInfo());

				if (e.getOwner().equals(toCopy) && toCopy.isSpecial(e)) {
					n.setSpecial(eCopy, true);
					n.setWeight(eCopy, 0);
				}

				addNetworkToNetworkRec(cCopy, c, toCopy, n, created, isClusterNetwork, edgePairs, nodePairs);
			} else {
				cCopy = created.get(c);
				eCopy = n.newEdge(vCopy, cCopy);
				eCopy.setShifted(e.isShifted());
				eCopy.setInfo(e.getInfo());

				if (e.getOwner().equals(toCopy) && toCopy.isSpecial(e)) {
					n.setSpecial(eCopy, true);
					n.setWeight(eCopy, 0);
				}

			}

			if (edgePairs != null) {
				for (MyEdge[] edgePair : edgePairs) {
					if (edgePair[0] != null && edgePair[0].equals(e))
						edgePair[1] = eCopy;
				}
			}

		}

		if (nodePairs != null) {
			for (MyNode[] nodePair : nodePairs) {
				if (nodePair[0] != null && nodePair[0].equals(v))
					nodePair[1] = vCopy;
			}
		}
	}

}
