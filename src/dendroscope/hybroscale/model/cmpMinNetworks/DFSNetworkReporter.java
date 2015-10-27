package dendroscope.hybroscale.model.cmpMinNetworks;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import dendroscope.hybroscale.model.HybridManager.Computation;
import dendroscope.hybroscale.model.parallelization.MyNetPriorThreadPool;
import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.model.treeObjects.SparseNetEdge;
import dendroscope.hybroscale.model.treeObjects.SparseNetNode;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.model.treeObjects.SparseTree;
import dendroscope.hybroscale.model.treeObjects.SparseTreeNode;
import dendroscope.hybroscale.model.util.CheckConstraints;
import dendroscope.hybroscale.util.graph.MyNode;

public class DFSNetworkReporter {

	private int k, treeIndex;
	private Computation compValue;
	private DFSManager manager;
	private SparseTree[] trees;
	private CheckConstraints checker;
	private Vector<String> taxaOrdering;
	private Vector<Integer> treeMapping;
	private MyNetPriorThreadPool myThreadPool;
	private HybridTree[] hybridTrees;
	private boolean speedUp;
	private NetworkMemory netMem;

	private HashMap<DFSUpdateNetworkThread, Integer[]> netToDegrees = new HashMap<DFSUpdateNetworkThread, Integer[]>();

	private boolean stop = false, debug = false, heuristicMode = false;

	public DFSNetworkReporter(SparseNetwork lastNetwork, int k, int treeIndex, Computation compValue,
			DFSManager manager, SparseTree[] trees, CheckConstraints checker, Vector<String> taxaOrdering,
			Vector<Integer> treeMapping, MyNetPriorThreadPool myThreadPool, HybridTree[] hybridTrees, boolean speedUp,
			boolean stop, boolean debug, boolean heuristicMode, NetworkMemory netMem) {
		
		this.k = k;
		this.treeIndex = treeIndex;
		this.compValue = compValue;
		this.manager = manager;
		this.trees = trees;
		this.checker = checker.copy();
		this.taxaOrdering = taxaOrdering;
		this.treeMapping = treeMapping;
		this.myThreadPool = myThreadPool;
		this.hybridTrees = hybridTrees;
		this.speedUp = speedUp;
		this.stop = stop;
		this.debug = debug;
		this.heuristicMode = heuristicMode;
		this.netMem = netMem;
		
	}

	public void reportNewNetworkThreads(SparseNetwork newNetwork, HybridTree[] treePair, HybridTree[] repHybridTrees, String log) {

		int newNumber = new CountRetNumber().getNumber(newNetwork, compValue);
		newNetwork.setNumber(newNumber);
		
//		System.out.println(newNetwork.getPhyloTree()+";");

		if(!processNetwork(newNetwork))
			return;
			
		if (newNumber < k && !stop) {

			SparseNetwork newNetworkCopy = copyNetwork(newNetwork, null, null, null);
			newNetworkCopy.setNumber(newNumber);

			Vector<SparseNetEdge> newReticulateEdges = new Vector<SparseNetEdge>();
			for (SparseNetEdge e : newNetworkCopy.getEdges()) {
				if (newNetworkCopy.isSpecial(e))
					newReticulateEdges.add(e);
			}

			SparseNetwork newNetworkWithoutOneNodes = new SparseNetwork(newNetwork);
			removeOneNodes(newNetworkWithoutOneNodes);

			int timeDegree = Integer.MAX_VALUE;
			if (manager.doCheckTime()
					&& (treeIndex == trees.length - 1 || manager.getBestTimeConsistentValue() < Integer.MAX_VALUE)) {
				timeDegree = checker.estimateTimeConsistencyDegree(newNetworkWithoutOneNodes,
						manager.getBestTimeConsistentValue(), true, Integer.MAX_VALUE);
				timeDegree = timeDegree == -1 ? Integer.MAX_VALUE : timeDegree;
			}

			int addTaxaDegree = Integer.MAX_VALUE;
			if (manager.doCheckAddTaxa()
					&& (treeIndex == trees.length - 1 || manager.getBestAddTaxaValue() < Integer.MAX_VALUE)) {
				addTaxaDegree = checker.estimateAddTaxaDegree(newNetworkWithoutOneNodes, manager.getBestAddTaxaValue(),
						true, Integer.MAX_VALUE);
				addTaxaDegree = addTaxaDegree == -1 ? Integer.MAX_VALUE : addTaxaDegree;
			}

			int levelDegree = Integer.MAX_VALUE;
			if (manager.doCheckLevel()
					&& (treeIndex == trees.length - 1 || manager.getBestLevelValue() < Integer.MAX_VALUE)) {
				levelDegree = checker.estimateLevelDegree(newNetworkWithoutOneNodes, manager.getBestLevelValue());
				levelDegree = levelDegree == -1 ? Integer.MAX_VALUE : levelDegree;
			}

			boolean nothingToCheck = !manager.doCheckTime() && !manager.doCheckAddTaxa() && !manager.doCheckLevel();

			if (treeIndex < trees.length - 1
					&& (checker == null || nothingToCheck || (checker.forNegConstraints(newNetworkWithoutOneNodes)
							&& (manager.doCheckTime() && timeDegree <= manager.getBestTimeConsistentValue())
							|| (manager.doCheckAddTaxa() && addTaxaDegree <= manager.getBestAddTaxaValue()) || (manager
							.doCheckLevel() && levelDegree <= manager.getBestLevelValue())))) {

				HashSet<BitSet> newEdgeSets = new ComputeEdgeSets(newNetworkCopy, newReticulateEdges, heuristicMode,
						treeIndex, null).run();

				for (BitSet newEdgeSet : newEdgeSets) {

					if (debug)
						System.out.println("newEdgeSet: " + newEdgeSet);

					BitSet newEdgeSetClone = (BitSet) newEdgeSet.clone();
					int newTreeIndex = treeIndex + 1;
					int newPriority = (newNumber + (2 * newTreeIndex)) * 10;

					prevTreeContainmentCheck(newNetworkCopy, "0-treeIndex: " + treeIndex, treeIndex);
						

					DFSUpdateNetworkThread newThread = new DFSUpdateNetworkThread(k, newNetworkCopy,
							newReticulateEdges, newEdgeSetClone, null, trees, repHybridTrees, newTreeIndex,
							taxaOrdering, newPriority, manager, compValue, treeMapping, heuristicMode, myThreadPool,
							speedUp, checker, netMem);

					if (!manager.isStopped)
						myThreadPool.submit(newThread);

				}

				newEdgeSets = null;

			} else if (newNumber < k
					&& (checker == null || nothingToCheck || (checker.forNegConstraints(newNetworkWithoutOneNodes)
							&& (manager.doCheckTime() && timeDegree <= manager.getBestTimeConsistentValue())
							|| (manager.doCheckAddTaxa() && addTaxaDegree <= manager.getBestAddTaxaValue()) || (manager
							.doCheckLevel() && levelDegree <= manager.getBestLevelValue())))) {

				manager.setBestTimeConsistentValue(timeDegree);
				manager.setBestAddTaxaValue(addTaxaDegree);
				manager.setBestLevelValue(levelDegree);

				int newTreeIndex = treeIndex + 1;
				int newPriority = (newNumber - (2 * newTreeIndex)) * 10;

				removeOneNodes(newNetworkCopy);
				updateEdgeIndices(newNetworkCopy);

				if(!prevTreeContainmentCheck(newNetworkCopy, " 1-treeIndex: " + newTreeIndex, newTreeIndex)){
					System.out.println(log+"\n");
				}

				mapIndices(newNetworkCopy);

				DFSUpdateNetworkThread newThread = new DFSUpdateNetworkThread(k, newNetworkCopy, newReticulateEdges,
						new BitSet(), null, trees, hybridTrees, newTreeIndex, taxaOrdering, newPriority, manager,
						compValue, treeMapping, heuristicMode, myThreadPool, speedUp, checker, netMem);

				if (compValue != Computation.EDGE_NETWORK)
					manager.reportNetworks(newThread, timeDegree, addTaxaDegree, levelDegree);
				else {
					Integer[] degrees = { timeDegree, addTaxaDegree, levelDegree };
					netToDegrees.put(newThread, degrees);
				}

			}

			newNetwork = null;
			newNetworkWithoutOneNodes = null;

		}

	}

	private boolean processNetwork(SparseNetwork recNet) {
		
		SparseNetwork n = new SparseNetwork(recNet);
		removeOneNodes(n);
		updateEdgeIndices(n);
		mapIndices(n);
		
		Vector<Integer> upcomingIndices = new Vector<Integer>();
		for (int i = treeIndex + 1; i < trees.length; i++)
			upcomingIndices.add(treeMapping.get(i));
		
		BitSet netSet = netMem.getNetworkSet(n);
		n.setInfo(netSet);
		if (netMem.containsNetwork(n, upcomingIndices))
			return false;
		netMem.addNetwork(n, upcomingIndices);
		
		return true;
	}

	public void reportFinishing() {
		if (compValue == Computation.EDGE_NETWORK) {
			for (DFSUpdateNetworkThread t : netToDegrees.keySet()) {
				int timeDegree = netToDegrees.get(t)[0];
				int addTaxaDegree = netToDegrees.get(t)[1];
				int levelDegree = netToDegrees.get(t)[2];
				manager.reportNetworks(t, timeDegree, addTaxaDegree, levelDegree);
			}
		}
	}

	private SparseNetwork copyNetwork(SparseNetwork n, Vector<SparseNetEdge> nonTreeEdges,
			Vector<SparseNetEdge> solidTreeEdges, ConcurrentHashMap<SparseNetEdge, SparseNetEdge> edgeToEdgeCopy) {
		SparseNetwork nCopy = new SparseNetwork(new SparseNetNode(null, null, n.getRoot().getLabel()));
		nCopy.getRoot().setOwner(nCopy);
		copyNetworkRec(nCopy.getRoot(), n.getRoot(), nCopy, n, new Vector<SparseNetNode>(),
				new ConcurrentHashMap<SparseNetNode, SparseNetNode>(), nonTreeEdges, solidTreeEdges, edgeToEdgeCopy);
		return nCopy;
	}

	private void copyNetworkRec(SparseNetNode vCopy, SparseNetNode v, SparseNetwork nCopy, SparseNetwork n,
			Vector<SparseNetNode> visited, ConcurrentHashMap<SparseNetNode, SparseNetNode> nodeToCopy,
			Vector<SparseNetEdge> nonTreeEdges, Vector<SparseNetEdge> solidTreeEdges,
			ConcurrentHashMap<SparseNetEdge, SparseNetEdge> edgeToEdgeCopy) {
		Iterator<SparseNetEdge> it = v.getOutEdges().iterator();
		while (it.hasNext()) {
			SparseNetEdge e = it.next();
			SparseNetNode c = e.getTarget();
			SparseNetNode cCopy;
			SparseNetEdge eCopy;
			if (nodeToCopy.containsKey(c)) {
				cCopy = nodeToCopy.get(c);
				eCopy = vCopy.addChild(cCopy);
			} else {
				cCopy = new SparseNetNode(vCopy, nCopy, c.getLabel());
				if (c.getOrder() != null)
					cCopy.setOrder(c.getOrder());
				eCopy = cCopy.getInEdges().firstElement();
			}
			if (nonTreeEdges != null && nonTreeEdges.contains(e))
				edgeToEdgeCopy.put(e, eCopy);
			if (n.isSpecial(e)) {
				nodeToCopy.put(c, cCopy);
				eCopy.addIndices((HashSet<Integer>) e.getIndices().clone());
			}
			if (solidTreeEdges != null && solidTreeEdges.contains(e))
				eCopy.setSolid(true);
			if (!visited.contains(c)) {
				visited.add(c);
				copyNetworkRec(cCopy, c, nCopy, n, visited, nodeToCopy, nonTreeEdges, solidTreeEdges, edgeToEdgeCopy);
			}
		}
	}

	private void removeOneNodes(SparseNetwork n) {
		Iterator<SparseNetNode> it = n.getNodes().iterator();
		while (it.hasNext())
			removeOneNode(it.next(), n);
	}

	private void removeOneNode(SparseNetNode v, SparseNetwork n) {
		if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
			SparseNetNode p = v.getInEdges().get(0).getSource();
			if (p.getInDegree() == 1 || (!n.isSpecial(v.getInEdges().get(0)) && !n.isSpecial(v.getOutEdges().get(0)))) {
				boolean isSolid = v.getInEdges().get(0).isSolid();
				isSolid = isSolid ? isSolid : v.getOutEdges().get(0).isSolid();
				HashSet<Integer> indices = (HashSet<Integer>) v.getOutEdges().get(0).getIndices().clone();
				SparseNetNode c = v.getOutEdges().get(0).getTarget();
				p.removeOutEdge(v.getInEdges().get(0));
				v.removeOutEdge(v.getOutEdges().get(0));
				SparseNetEdge e = p.addChild(c);
				e.addIndices(indices);
				e.setSolid(isSolid);
			}
		}
	}

	private void mapIndices(SparseNetwork newNet) {
		if (treeMapping == null || treeMapping.isEmpty())
			return;
		for (SparseNetEdge e : newNet.getEdges()) {
			HashSet<Integer> newIndices = new HashSet<Integer>();
			for (int index : e.getIndices())
				newIndices.add(treeMapping.get(index));
			e.getIndices().clear();
			e.addIndices(newIndices);
		}
		for (SparseNetEdge e : newNet.getEdges()) {
			if (newNet.isSpecial(e)) {
				Vector<Integer> wrongIndices = new Vector<Integer>();
				for (int index : e.getIndices())
					if (!e.getTarget().getOutEdges().get(0).getIndices().contains(index)
							&& e.getTarget().getOutDegree() == 1)
						wrongIndices.add(index);
				for (int wrongIndex : wrongIndices) {
					e.removeIndex(wrongIndex);
					removeWrongIndicesRec(e.getSource(), wrongIndex);
				}
			}
		}
	}

	private void removeWrongIndicesRec(SparseNetNode v, int wrongIndex) {
		if (v.getInDegree() == 1) {
			boolean removeIndex = true;
			for (SparseNetEdge e : v.getOutEdges()) {
				if (e.getIndices().contains(wrongIndex))
					removeIndex = false;
			}
			if (removeIndex) {
				SparseNetEdge e = v.getInEdges().get(0);
				e.removeIndex(wrongIndex);
				removeWrongIndicesRec(e.getSource(), wrongIndex);
			}
		}
	}

	private void updateEdgeIndices(SparseNetwork newNet) {
		for (int index = 0; index < trees.length; index++) {
			Vector<String> treeTaxa = new Vector<String>();
			for (SparseTreeNode leaf : trees[index].getLeaves())
				treeTaxa.add(leaf.getLabel());
			for (SparseNetNode leaf : newNet.getLeaves()) {
				if (treeTaxa.contains(leaf.getLabel()))
					updateRootPath(index, leaf, newNet);
			}
		}
	}

	private void updateRootPath(int index, SparseNetNode v, SparseNetwork newNet) {
		if (v.getInDegree() != 0) {
			SparseNetEdge e = null;
			if (v.getInDegree() == 1) {
				e = v.getInEdges().get(0);
				e.addIndex(index);
			} else if (v.getInDegree() > 1) {
				for (SparseNetEdge eIn : v.getInEdges()) {
					if (eIn.getIndices().contains(index)) {
						e = eIn;
						break;
					}
				}
			}
			if (e != null)
				updateRootPath(index, e.getSource(), newNet);
		}
	}

	private boolean prevTreeContainmentCheck(SparseNetwork n, String s, int treeIndex) {

		for (int i = 0; i < treeIndex; i++) {		
			
			if (hybridTrees[i] != null) {

				Hashtable<Integer, Vector<String>> prevTreeToTaxa = new Hashtable<Integer, Vector<String>>();
				prevTreeToTaxa.put(i, new Vector<String>());
				for (MyNode leaf : hybridTrees[i].getLeaves())
					prevTreeToTaxa.get(i).add(leaf.getLabel());

				HashSet<BitSet> treeClusters = hybridTrees[i].getClusterSet();
				HashSet<BitSet> netClusters = new HashSet<BitSet>();
				for (SparseNetNode v : n.getNodes()) {
					BitSet cluster = new BitSet(taxaOrdering.size());
					getClusterByPrevTree(v, n, cluster, i, prevTreeToTaxa, false);
					if (!cluster.isEmpty())
						netClusters.add(cluster);
				}
				for (BitSet treeCluster : treeClusters) {
					if (!netClusters.contains(treeCluster)) {
						System.out.println("ERROR - Tree not contained: " + s);
						System.out.println(n.getPhyloTree().toMyBracketString());
						System.out.println(hybridTrees[i] + ";");
						for (int j = 0; j < treeIndex; j++)
							System.out.println(hybridTrees[j] + ";");
						int index = treeCluster.nextSetBit(0);
						while (index != -1) {
							System.out.print(taxaOrdering.get(index) + " ");
							index = treeCluster.nextSetBit(index + 1);
						}
						System.out.println();
						return false;
					}
				}
			}
		}
		return true;
	}

	private void getClusterByPrevTree(SparseNetNode v, SparseNetwork n, BitSet b, int index,
			Hashtable<Integer, Vector<String>> prevTreeToTaxa, boolean debug) {
		if (v.getOutDegree() == 0 && prevTreeToTaxa.get(index).contains(v.getLabel())) {
			b.set(taxaOrdering.indexOf(v.getLabel()));
		} else {
			Iterator<SparseNetEdge> it = v.getOutEdges().iterator();
			while (it.hasNext()) {
				SparseNetEdge e = it.next();
				if (e.getIndices().contains(index) || e.getTarget().getInDegree() <= 1)
					getClusterByPrevTree(e.getTarget(), n, b, index, prevTreeToTaxa, debug);
				else if (debug)
					System.out.println(e.getSource().getLabel() + " -> " + e.getTarget().getLabel() + " "
							+ e.getIndices().size());
			}
		}
	}

}
