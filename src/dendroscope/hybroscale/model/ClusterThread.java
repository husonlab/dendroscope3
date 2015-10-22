package dendroscope.hybroscale.model;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import dendroscope.hybroscale.model.HybridManager.Computation;
import dendroscope.hybroscale.model.parallelization.MyNetPriorThreadPool;
import dendroscope.hybroscale.model.parallelization.MyThreadPool;
import dendroscope.hybroscale.model.reductionSteps.ReplacementInfo;
import dendroscope.hybroscale.model.treeObjects.HybridNetwork;
import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.model.treeObjects.SparseNetEdge;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.model.util.CheckConstraints;
import dendroscope.hybroscale.util.ClusterManager;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;
import dendroscope.hybroscale.view.HybridView;

/**
 * @author Benjamin Albrecht, 6.2012
 */
public class ClusterThread extends Thread {

	private int level;

	private MyNetPriorThreadPool netExec;
	private HybridTree[] trees;
	private ReplacementInfo rI;
	private MyThreadPool threadPool;
	private Vector<SparseNetwork> maxForests = new Vector<SparseNetwork>();
	private Vector<HybridNetwork> networks = new Vector<HybridNetwork>();
	private Vector<Integer> isomorphVector = new Vector<Integer>();
	private Hashtable<Integer, Vector<Integer>> isomorphMapping = new Hashtable<Integer, Vector<Integer>>();
	private Boolean isSubCluster;
	private ExhaustiveSearch eS;
	private HybridNetwork clustertNetwork;
	private long timeStart;
	private int sumOfCalls = 0;
	private Vector<String> taxaOrdering;
	private Integer maxK;

	private Vector<HybridTree> forest = new Vector<HybridTree>();
	private int hybridNumber = -1, edgeNumber = -1;
	private Computation compValue;
	private int lowerBound = 0;
	private boolean speedUp;
	private CheckConstraints checker;
	boolean hasNoResult = false, verbose = false;

	private Vector<HybridView> views;

	public ClusterThread(HybridTree[] clusterTrees, ReplacementInfo rI, MyThreadPool myPool, Boolean isSubCluster, Computation compValue, MyNetPriorThreadPool netExec, Integer maxK,
			Vector<HybridView> views, Vector<String> taxaOrdering, boolean speedUp, CheckConstraints checker, boolean verbose) {

		trees = new HybridTree[clusterTrees.length];
		for (int i = 0; i < clusterTrees.length; i++) {
			trees[i] = new HybridTree(clusterTrees[i], false, taxaOrdering);
			trees[i].update();
		}

		this.taxaOrdering = taxaOrdering;
		this.rI = rI;
		this.threadPool = myPool;
		this.isSubCluster = isSubCluster;
		this.compValue = compValue;
		this.netExec = netExec;
		this.maxK = maxK;
		this.views = views;
		this.speedUp = speedUp;
		this.checker = checker;
		this.verbose = verbose;

		for (HybridView view : views)
			view.addClusterThread(this);

	}

	public void run() {
		try {

			if(verbose){
				System.out.println("> Running cluster thread for tree set: ");
				for (HybridTree t : trees)
					printTree(t);
			}
			
			eS = new ExhaustiveSearch(trees, threadPool, this, compValue, lowerBound, netExec, maxK, views,
					taxaOrdering, speedUp, checker, verbose);
			eS.run();		
			eS.join();
			
			eS.stopThread();
			eS.interrupt();

			if (eS.hasNoResult()) {
				for (HybridView view : views)
					view.setDetails(this,
							"No result computed - please check constraints:\n" + checker.getNegBadConstraints());
				hasNoResult = true;
			}
			
			if(verbose){
				if(compValue == Computation.EDGE_NETWORK)
					System.out.println("Result: "+maxForests.size()+ " network(s) with hybridization number "+hybridNumber+" computed");
				else
					System.out.println("Result: hybridization number "+hybridNumber+" computed");
			}

			if (compValue == Computation.EDGE_NETWORK) {

				// computing all networks out of the MAAFs...
				if (maxForests.size() != 0) {

					if (isSubCluster) {
						// transforming sparse networks to hybrid networks
						for (SparseNetwork s : maxForests) {
							performEdgeMapping(s);
							HybridNetwork n = new HybridNetwork(s.getPhyloTree(), false, taxaOrdering);
							n.setReplacementCharacter(trees[0].getReplacementCharacter());
							networks.add(n);
						}
						for (HybridNetwork n : networks)
							rI.addNetwork(n, false);
					} else {
						for (SparseNetwork n : maxForests)
							performEdgeMapping(n);
					}

				} else if (maxForests.isEmpty()) {

					MyPhyloTree clusterNetwork = new ClusterManager(trees).run();

					if (isSubCluster) {
						HybridNetwork n = new HybridNetwork(clusterNetwork, false, taxaOrdering);
						n.setReplacementCharacter(trees[0].getReplacementCharacter());
						for (MyEdge e : n.getEdges()){
							HashSet<Integer> indices = new HashSet<Integer>();
							indices.add(-1);
							e.setInfo(indices);
						}
						networks.add(n);
						rI.addNetwork(n, true);
					} else {
						SparseNetwork net = new SparseNetwork(clusterNetwork);
						for (SparseNetEdge e : net.getEdges())
							e.setEdgeIndex(new HashSet<Integer>());
						maxForests.add(net);
					}

				}

			}

			for (HybridView view : views)
				view.finishClusterThread(this);

		} catch (Exception e) {
			e.printStackTrace();
		}		
		
//		maxForests = null;
		eS = null;
		
	}

	private void performEdgeMapping(SparseNetwork n) {
		if (!isomorphVector.isEmpty()) {

			for (SparseNetEdge e : n.getEdges()) {

				Vector<Integer> newIndices = new Vector<Integer>();
				Vector<Integer> isoVector = (Vector<Integer>) isomorphVector.clone();
				for (int index : e.getIndices()) {
					int newIndex = index;
					for (int i : isoVector) {
						if (i <= newIndex)
							newIndex = newIndex + 1;
					}
					newIndices.add(newIndex);
				}

				e.getIndices().clear();
				e.addIndices(newIndices);

				newIndices = new Vector<Integer>();
				for (int index : e.getIndices()) {
					newIndices.add(index);
					if (isomorphMapping.containsKey(index))
						newIndices.addAll(isomorphMapping.get(index));
				}
				e.getIndices().clear();
				e.addIndices(newIndices);

			}

		}
	}

	public Vector<SparseNetwork> getMaxForests() {
		return maxForests;
	}

	public boolean hasNoResult() {
		return hasNoResult;
	}

	public boolean stopThread() {
		if (eS != null) {
			eS.stopThread();
			return true;
		}
		return false;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public HybridNetwork getClusterNetwork() {
		return clustertNetwork;
	}

	public long getTimeStart() {
		return timeStart;
	}

	public void setTimeStart(long timeStart) {
		this.timeStart = timeStart;
	}

	public void setHybridNumber(int hybridNumber) {
		this.hybridNumber = hybridNumber;
	}

	public void setEdgeNumber(int edgeNumber) {
		this.edgeNumber = edgeNumber;
	}

	public int getHybridNumber() {
		return hybridNumber;
	}

	public int getEdgeNumber() {
		return edgeNumber;
	}

	public void setCompValue(Computation compValue) {
		this.compValue = compValue;
	}

	public void setLowerBound(int lowerBound) {
		this.lowerBound = lowerBound;
	}

	public Vector<HybridTree> getForest() {
		return forest;
	}

	public void setSumOfCalls(int sumOfCalls) {
		this.sumOfCalls = sumOfCalls;
	}

	public int getSumOfCalls() {
		return sumOfCalls;
	}

	public void setTrees(HybridTree[] trees) {
		this.trees = trees;
	}

	public void setIsoVector(Vector<Integer> isomorphVector) {
		this.isomorphVector = isomorphVector;
	}

	public void setIsoMapping(Hashtable<Integer, Vector<Integer>> isomorphMapping) {
		this.isomorphMapping = isomorphMapping;
	}

	public Vector<Integer> getIsoVector() {
		return isomorphVector;
	}

	public HybridTree[] getTrees() {
		return trees;
	}

	public Vector<HybridNetwork> getNetworks() {
		return networks;
	}

	public CheckConstraints getChecker() {
		return checker;
	}
	
	private void printTree(HybridTree t) {
		MyPhyloTree tPrint = new MyPhyloTree(t);
		for(MyNode v : tPrint.getNodes()){
			if(v.getOutDegree() != 0)
				v.setLabel(null);;
		}
		rI.addLeafLabels(tPrint);
		System.out.println(tPrint.toBracketString());
	}

}
