package dendroscope.hybroscale.model.cmpMinNetworks;

import dendroscope.hybroscale.model.HybridManager.Computation;
import dendroscope.hybroscale.model.cmpAllMAAFs.AcyclicOrderingChecker;
import dendroscope.hybroscale.model.cmpAllMAAFs.RefinedFastGetAgreementForest;
import dendroscope.hybroscale.model.cmpAllMAAFs.ResolveTreeToForest;
import dendroscope.hybroscale.model.cmpAllMAAFs.TreeToForestAdaptor;
import dendroscope.hybroscale.model.parallelization.MyNetPriorThreadPool;
import dendroscope.hybroscale.model.treeObjects.*;
import dendroscope.hybroscale.model.util.CheckConstraints;
import dendroscope.hybroscale.terminals.TerminalManager;
import dendroscope.hybroscale.util.graph.MyNode;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class DFSUpdateNetworkThread extends Thread {

	private SparseNetwork n;
	private Vector<SparseNetEdge> reticulateEdges;
	private BitSet edgeSet, solidEdgeSet;
	private SparseTree[] trees;
	private int treeIndex, k, myPriority;
	private Vector<String> taxaOrdering;
	private DFSManager manager;
	private Computation compValue;
	private Vector<Integer> treeMapping;
	private MyNetPriorThreadPool myThreadPool;
	private boolean stop = false;
	private HybridTree[] hybridTrees;
	private RefinedFastGetAgreementForest cmpAF;
	private NetworkMemory netMem;

	private boolean debug = false;
	private boolean heuristicMode, speedUp;
	private SparseTree tPrime;

	int counter = 0;
	String log = "0";

	private CheckConstraints checker;

	public DFSUpdateNetworkThread(int k, SparseNetwork n, Vector<SparseNetEdge> reticulateEdges, BitSet edgeSet,
			BitSet solidEdgeSet, SparseTree[] trees, HybridTree[] hybridTrees, int treeIndex,
			Vector<String> taxaOrdering, int myPriority, DFSManager manager, Computation compValue,
			Vector<Integer> treeMapping, boolean heuristicMode, MyNetPriorThreadPool myThreadPool, boolean speedUp,
			CheckConstraints checker, NetworkMemory netMem) {
		this.k = k;
		this.n = n;
		this.reticulateEdges = reticulateEdges;
		this.edgeSet = edgeSet;
		this.solidEdgeSet = solidEdgeSet;
		this.trees = trees;
		this.hybridTrees = hybridTrees;
		this.treeIndex = treeIndex;
		this.taxaOrdering = taxaOrdering;
		this.myPriority = myPriority;
		this.manager = manager;
		this.compValue = compValue;
		this.treeMapping = treeMapping;
		this.heuristicMode = heuristicMode;
		this.speedUp = speedUp;
		this.checker = checker == null ? null : checker.copy();
		this.myThreadPool = myThreadPool;
		this.netMem = netMem;
	}

	public void run() {

		// System.out.println("+++ START k: "+k+" | "+this);
		HybridTree h1 = null;
		try {

			if (debug) {
				System.out.println("\nUpdating Network... " + treeIndex + "\n" + n.getPhyloTree() + ";");
				System.out.println("EdgeSet: " + edgeSet);
			}

			// extracting t from n
			tPrime = extractTree(false);

			if (tPrime.getLeaves().size() < n.getLeaves().size())
				System.out.println("Error: \n" + tPrime.getPhyloTree() + ";\n" + n.getPhyloTree().toMyBracketString());

			// computing (maximum) acyclic agreement forests
			h1 = new HybridTree(tPrime.getPhyloTree(), false, taxaOrdering);
			h1.update();
			HybridTree h2 = new HybridTree(trees[treeIndex].getPhyloTree(), false, taxaOrdering);
			h2.update();

			Vector<Vector<SparseTree>> MAAFs = new Vector<Vector<SparseTree>>();
			int border = k - n.getRetNumber();

			int lowerBound = 0;
			if (trees.length != 2) {
				// int upperBound = new RefinedFastApproxHNumber().run(h1, h2,
				// taxaOrdering, 4 * border);
				// lowerBound = (int) (Math.ceil((double) upperBound / 4.));
				TerminalManager tM = new TerminalManager(h1, h2, taxaOrdering, lowerBound, border);
				tM.run();
				lowerBound = tM.getResult();
			} else
				lowerBound = k;

			for (int minK = lowerBound; minK <= border; minK++) {
				Computation compMAAF = Computation.EDGE_NETWORK;
				if (compValue == Computation.EDGE_NUMBER && treeIndex == trees.length - 1)
					compMAAF = Computation.EDGE_NUMBER;
				cmpAF = new RefinedFastGetAgreementForest();
				MAAFs = cmpAF.run(h1, h2, minK, compMAAF, taxaOrdering, manager);
				if (!MAAFs.isEmpty())
					break;
			}
			cmpAF = null;

			MAAFs = new AcyclicOrderingChecker().run(h1, h2, MAAFs);

			// adding each agreement forest to the network
			for (Vector<SparseTree> MAAF : MAAFs) {

				Vector<SparseTree> t2MAAF = new Vector<SparseTree>();
				Vector<SparseTree> t1MAAF = new Vector<SparseTree>();
				int hSize = 0;
				for (SparseTree t : MAAF) {
					if (t.getInfo().isEmpty()) {
						t2MAAF.add(t);
						t1MAAF.add(t);
						hSize++;
					} else if (t.getInfo() == "2")
						t2MAAF.add(t);
					else if (t.getInfo() == "1")
						t1MAAF.add(t);
				}

				if ((n.getRetNumber() + hSize - 1 < k) && !stop) {

					// adding agreement forest to the so far computed network
					Vector<SparseNetEdge> treeEdges = new Vector<SparseNetEdge>();
					for (SparseNetEdge e : reticulateEdges) {
						int index = reticulateEdges.indexOf(e);
						if (edgeSet.get(index) == true)
							treeEdges.add(e);
					}

					// System.out.println("\nForest:");
					// for (SparseTree c : MAAF)
					// System.out.println(c.getPhyloTree().toMyBracketString() +
					// " " + c.getInfo());
					// System.out.println(n.getPhyloTree() + ";");

					Vector<HybridTree> tFor1Vec = new TreeToForestAdaptor().runRec(t1MAAF, h1, false, taxaOrdering);
					Vector<HybridTree> tFor2Vec = new TreeToForestAdaptor().runRec(t2MAAF, h2, false, taxaOrdering);

					for (HybridTree tFor1 : tFor1Vec) {
						for (HybridTree tFor2 : tFor2Vec) {

							// System.out.println(h1 + ";\n" + tFor1 + ";");
							// System.out.println(h2 + ";\n" + tFor2 + ";");
							// System.out.println(h2 + ";\n" + tFor2 + ";");

							Vector<HybridTree> resolvedH1Trees = new ResolveTreeToForest().run(t1MAAF, h1, tFor1,
									false, taxaOrdering);
							Vector<HybridTree> resolvedH2Trees = new ResolveTreeToForest().run(t2MAAF, h2, tFor2,
									false, taxaOrdering);

							for (HybridTree resH1 : resolvedH1Trees) {
								for (HybridTree resH2 : resolvedH2Trees) {

									ConcurrentHashMap<SparseNetEdge, SparseNetEdge> edgeToEdgeCopy = new ConcurrentHashMap<SparseNetEdge, SparseNetEdge>();
									SparseNetwork nCopy = copyNetwork(n, treeEdges, null, edgeToEdgeCopy);
									Vector<SparseNetEdge> treeEdgesCopy = new Vector<SparseNetEdge>();
									for (SparseNetEdge eCopy : edgeToEdgeCopy.values())
										treeEdgesCopy.add(eCopy);

									if (hasOneNode(nCopy))
										System.out.println("ONE NODE-1: \n" + n.getPhyloTree() + ";\n"
												+ nCopy.getPhyloTree() + ";");

									HybridTree resH1Copy = new HybridTree(resH1, false, resH1.getTaxaOrdering());
									resH1Copy.update();
									HybridTree resH2Copy = new HybridTree(resH2, false, resH2.getTaxaOrdering());
									resH2Copy.update();

									HybridTree[] hTreesCopy = { resH1Copy, resH2Copy };

									NetworkToTreeAdaptor netApp = new NetworkToTreeAdaptor();
									netApp.run(nCopy, hTreesCopy[0], treeEdgesCopy, taxaOrdering);

									if (hasOneNode(nCopy))
										System.out.println("ONE NODE-2: \n" + n.getPhyloTree() + ";\n"
												+ nCopy.getPhyloTree());

									// System.out.println(hTreesCopy[0] + ";");
									// System.out.println("\n"+hTreesCopy[1] +
									// ";");
									// System.out.println(nCopy.getPhyloTree() +
									// ";\n");

									HybridTree[] newHybridTrees = new HybridTree[trees.length];
									for (int i = 0; i < trees.length; i++) {
										newHybridTrees[i] = new HybridTree(trees[i].getPhyloTree(), false, taxaOrdering);
										newHybridTrees[i].update();
									}

									DFSNetworkReporter reporter = new DFSNetworkReporter(n, k, treeIndex,
											compValue, manager, trees, checker, taxaOrdering, treeMapping,
											myThreadPool, hybridTrees, speedUp, stop, debug, heuristicMode, netMem);
									DFSComputeUpdatedNetworks cmpNetworks = new DFSComputeUpdatedNetworks(t2MAAF,
											nCopy, hTreesCopy, treeEdgesCopy, treeIndex, taxaOrdering, compValue,
											trees, newHybridTrees, heuristicMode, speedUp, false, reporter, myPriority);
									// cmpNetworks.run();
									myThreadPool.submit(cmpNetworks);

								}
							}
						}
					}
				}

			}

			MAAFs = null;

		} catch (Exception e) {
			e.printStackTrace();
		}

		reticulateEdges = null;
		edgeSet = null;
		solidEdgeSet = null;
		trees = null;
		hybridTrees = null;
		taxaOrdering = null;
		manager = null;
		compValue = null;
		treeMapping = null;
		myThreadPool = null;

	}

	private SparseTree extractTree(boolean debug) {

		Vector<SparseNetEdge> nonTreeEdges = new Vector<SparseNetEdge>();
		Vector<SparseNetEdge> solidTreeEdges = new Vector<SparseNetEdge>();
		for (SparseNetEdge e : reticulateEdges) {
			int index = reticulateEdges.indexOf(e);
			if (edgeSet.get(index) == false)
				nonTreeEdges.add(e);
			if (solidEdgeSet != null && solidEdgeSet.get(index) == true)
				solidTreeEdges.add(e);
		}

		ConcurrentHashMap<SparseNetEdge, SparseNetEdge> edgeToEdgeCopy = new ConcurrentHashMap<SparseNetEdge, SparseNetEdge>();
		SparseNetwork nCopy = copyNetwork(n, nonTreeEdges, solidTreeEdges, edgeToEdgeCopy);

		if (debug)
			System.out.println("nCopy: " + nCopy.getPhyloTree() + ";");

		for (SparseNetEdge e : edgeToEdgeCopy.keySet()) {
			SparseNetEdge e1 = edgeToEdgeCopy.get(e);
            SparseNetEdge e2 = e1.getSource().inEdges().iterator().next();
			SparseNetNode s1 = e1.getSource();
			SparseNetNode s2 = e2.getSource();

			s2.removeOutEdge(e2);
			s1.removeOutEdge(e1);
		}
		for (SparseNetNode leaf : nCopy.getNodes())
			removeNonLeaf(leaf);
		removeOneNodes(nCopy);

		return new SparseTree(nCopy);
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
        Iterator<SparseNetEdge> it = v.outEdges().iterator();
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
                eCopy = cCopy.inEdges().iterator().next();
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

	private void removeNonLeaf(SparseNetNode v) {
		if (v.getOutDegree() == 0 && !taxaOrdering.contains(v.getLabel())) {
			ConcurrentHashMap<SparseNetNode, SparseNetEdge> nodeToEdge = new ConcurrentHashMap<SparseNetNode, SparseNetEdge>();
            for (SparseNetEdge e : v.inEdges())
				nodeToEdge.put(e.getSource(), e);
			for (SparseNetNode s : nodeToEdge.keySet()) {
				s.removeOutEdge(nodeToEdge.get(s));
				if (s.getOutDegree() == 0)
					removeNonLeaf(s);
			}
		}
	}

	public boolean hasOneNode(SparseNetwork n) {
		for (SparseNetNode v : n.getNodes()) {
            if (v.getInDegree() == 1 && v.getOutDegree() == 1 && !n.isSpecial(v.outEdges().iterator().next()))
				return true;
		}
		return false;
	}

	public boolean hasOneNode(HybridTree t) {
		for (MyNode v : t.getNodes()) {
			if (v.getInDegree() == 1 && v.getOutDegree() == 1)
				return true;
		}
		return false;
	}

	private void removeOneNodes(SparseNetwork n) {
		Iterator<SparseNetNode> it = n.getNodes().iterator();
		while (it.hasNext())
			removeOneNode(it.next(), n);
	}

	private void removeOneNode(SparseNetNode v, SparseNetwork n) {
		if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
            SparseNetNode p = v.inEdges().iterator().next().getSource();
            if (p.getInDegree() == 1 || (!n.isSpecial(v.inEdges().iterator().next()) && !n.isSpecial(v.outEdges().iterator().next()))) {
                boolean isSolid = v.inEdges().iterator().next().isSolid();
                isSolid = isSolid ? isSolid : v.outEdges().iterator().next().isSolid();
                HashSet<Integer> indices = (HashSet<Integer>) v.outEdges().iterator().next().getIndices().clone();
                SparseNetNode c = v.outEdges().iterator().next().getTarget();
                p.removeOutEdge(v.inEdges().iterator().next());
                v.removeOutEdge(v.outEdges().iterator().next());
				SparseNetEdge e = p.addChild(c);
				e.addIndices(indices);
				e.setSolid(isSolid);
			}
		}
	}

	public int getMyPriority() {
		return myPriority;
	}

	public SparseNetwork getNetwork() {
		return n;
	}

	public int getTreeIndex() {
		return treeIndex;
	}

	public SparseTree[] getTrees() {
		return trees;
	}

	public void setStop(boolean b) {
		stop = b;
		if (b && cmpAF != null)
			cmpAF.setStop(true);
	}

}
