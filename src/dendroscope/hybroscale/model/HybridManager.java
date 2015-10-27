package dendroscope.hybroscale.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import dendroscope.hybroscale.controller.HybridLauncher;
import dendroscope.hybroscale.model.attachNetworks.ReattachNetworks;
import dendroscope.hybroscale.model.attachNetworks.ReattachUniqueTaxa;
import dendroscope.hybroscale.model.parallelization.MyNetPriorThreadPool;
import dendroscope.hybroscale.model.parallelization.MyThreadPool;
import dendroscope.hybroscale.model.reductionSteps.ClusterReduction_Overlapping;
import dendroscope.hybroscale.model.reductionSteps.ReplacementInfo;
import dendroscope.hybroscale.model.reductionSteps.SubtreeReduction;
import dendroscope.hybroscale.model.treeObjects.HybridNetwork;
import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.model.util.CheckConstraints;
import dendroscope.hybroscale.model.util.ComputeNodeWeightsSparse;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;
import dendroscope.hybroscale.view.HybridView;

public class HybridManager extends Thread {

	private Vector<HybridView> views = new Vector<HybridView>();
	private Vector<HybridLauncher> observers = new Vector<HybridLauncher>();

	private Vector<MyPhyloTree> networks = new Vector<MyPhyloTree>();
	private MyThreadPool myPool = new MyThreadPool();
	private MyNetPriorThreadPool myNetPool = new MyNetPriorThreadPool();
	private Vector<ClusterThread> clusterThreads = new Vector<ClusterThread>();
	private int hybridNumber = 0, edgeNumber = 0, addTaxaValue = -1, timeConsValue = -1, levelDegree = -1;
	private int maxNumber = 0;
	private int numOfNets;
	private int cores;
	private int recCalls = 0;
	private double runtime;

	private MyPhyloTree[] phyloTrees;
	private Vector<String> taxaOrdering;
	private Computation compValue;
	private ReattachNetworks rN;

	private boolean stop = false, hasFinished = false, doClusterReduction, verbose = false;
	private Integer maxK;
	private boolean speedUp;
	private String constraints;

	private Thread timeThread;

	public enum Computation {
		EDGE_NETWORK, EDGE_NUMBER
	}

	public HybridManager(MyPhyloTree[] phyloTrees, Computation computation, int cores, Integer maxK, boolean speedUp,
			String constraints, boolean doClusterReduction, boolean verbose) {
		this.compValue = computation;
		this.phyloTrees = new MyPhyloTree[phyloTrees.length];
		for (int i = 0; i < phyloTrees.length; i++)
			this.phyloTrees[i] = new MyPhyloTree(phyloTrees[i]);
		this.maxK = maxK;
		this.cores = cores;
		this.speedUp = speedUp;
		this.constraints = constraints;
		this.doClusterReduction = doClusterReduction;
		this.verbose = verbose;
		myPool.setSize(1);
		myNetPool.setSize(cores);
	}

	public void run() {
		try {

			if (verbose)
				System.out.println("------------------------------------------------------");

			final double time = System.currentTimeMillis();
			timeThread = new TimeThread();
			timeThread.start();

			ReplacementInfo rI = new ReplacementInfo();

			// replacing each leaf labeling by a unique number
			MyPhyloTree[] uniquePhyloTrees = new MyPhyloTree[phyloTrees.length];
			for (int i = 0; i < phyloTrees.length; i++) {
				uniquePhyloTrees[i] = new MyPhyloTree(phyloTrees[i]);
				for (MyNode v : uniquePhyloTrees[i].getLeaves()) {
					if (v.getLabel() == "rho")
						v.setLabel("rho'");
				}
				for (MyNode v : uniquePhyloTrees[i].getNodes()) {
					if (v.getOutDegree() != 0)
						v.setLabel("");
				}
			}
			
			rI.removeUniqueTaxa(uniquePhyloTrees);
			rI.replaceLabelsByNumber(uniquePhyloTrees);

			this.taxaOrdering = new Vector<String>();
			for (int i = 0; i < uniquePhyloTrees.length; i++) {
				for (MyNode leaf : uniquePhyloTrees[i].getLeaves()) {
					if (!taxaOrdering.contains(leaf.getLabel()))
						taxaOrdering.add(leaf.getLabel());
				}
			}
			taxaOrdering.add("rho");
			Collections.sort(taxaOrdering);

			HybridTree[] trees = new HybridTree[uniquePhyloTrees.length];
			for (int i = 0; i < uniquePhyloTrees.length; i++)
				trees[i] = new HybridTree(uniquePhyloTrees[i], true, taxaOrdering);

			// Reduction Steps *********************************************

			// running subtree reduction
			taxaOrdering = new SubtreeReduction().run(trees, rI, taxaOrdering);

			// running cluster reduction
			// ClusterReduction cR = new ClusterReduction();
			ClusterReduction_Overlapping cR = new ClusterReduction_Overlapping();
			HybridTree[] minClusterTrees = null;

			if (doClusterReduction)
				minClusterTrees = cR.run(trees, rI, taxaOrdering);
			while (minClusterTrees != null) {
				if (cR.getTaxaOrdering() != null)
					taxaOrdering = cR.getTaxaOrdering();
				CheckConstraints clusterChecker = new CheckConstraints(constraints, taxaOrdering, rI);
				clusterThreads.add(new ClusterThread(minClusterTrees, rI, myPool, true, compValue, myNetPool, maxK,
						views, taxaOrdering, speedUp, clusterChecker, verbose));
				minClusterTrees = cR.run(trees, rI, taxaOrdering);
			}

			if (cR.getTaxaOrdering() != null)
				taxaOrdering = cR.getTaxaOrdering();

			// *************************************************************

			// Running cluster threads ************************************

			// running each cluster thread in parallel
			for (ClusterThread cT : clusterThreads) {
				cT.setCompValue(compValue);
				cT.start();
				cT.join();
				if (maxK != null) {
					cT.join();
					maxK = cT.getEdgeNumber() < 0 ? -1 : maxK - cT.getEdgeNumber();
					if (maxK < 0)
						break;
				}
			}

			// waiting for each cluster thread
			for (ClusterThread cT : clusterThreads)
				cT.join();

			for (ClusterThread cT : clusterThreads) {
				if (cT.hasNoResult()) {
					for (HybridView view : views)
						view.showProblematicConstraints(cT.getChecker().getNegBadConstraints());
				}
			}

			// collecting results from each cluster thread
			for (ClusterThread cT : clusterThreads) {
				hybridNumber += cT.getHybridNumber();
				edgeNumber += cT.getEdgeNumber();
				recCalls += cT.getSumOfCalls();
			}

			// running root cluster thread
			HybridTree[] clusterTreesCopy = new HybridTree[trees.length];
			for (int i = 0; i < trees.length; i++)
				clusterTreesCopy[i] = new HybridTree(trees[i], false, taxaOrdering);
			CheckConstraints clusterChecker = new CheckConstraints(constraints, taxaOrdering, rI);
			ClusterThread cTHybrid = new ClusterThread(clusterTreesCopy, rI, myPool, false, compValue, myNetPool, maxK,
					views, taxaOrdering, speedUp, clusterChecker, verbose);
			cTHybrid.setCompValue(compValue);
			cTHybrid.run();

			// waiting for root cluster thread
			cTHybrid.join();

			// collecting results from root cluster thread
			hybridNumber += cTHybrid.getHybridNumber();
			edgeNumber += cTHybrid.getEdgeNumber();
			recCalls += cTHybrid.getSumOfCalls();
			if (maxK != null) {
				maxK = cTHybrid.getEdgeNumber() < 0 ? -1 : maxK - cTHybrid.getEdgeNumber();
			}

			// ****************************************************

			// Computing networks / Hybrid Number *****************
			if (compValue == Computation.EDGE_NETWORK && (maxK == null || maxK >= 0)) {

				numOfNets = getNumOfNetworks(rI);
				int numOfShiftNets = 0;

				Vector<SparseNetwork> maxForests = cTHybrid.getMaxForests();
				if (maxForests.size() != 0) {

					// computing root networks
					Vector<HybridNetwork> allNetworks = new Vector<HybridNetwork>();
					for (SparseNetwork s : maxForests)
						allNetworks.add(new HybridNetwork(s.getPhyloTree(), false, taxaOrdering));

					if (clusterChecker.doCheckAddTaxa()) {
						addTaxaValue = 0;
						setAddTaxaValue(allNetworks.firstElement(), rI);
					}

					if (clusterChecker.doCheckTime()) {
						timeConsValue = 0;
						setTimeConsValue(allNetworks.firstElement(), rI);
					}

					if (clusterChecker.doCheckLevel()) {
						levelDegree = 0;
						setLevelValue(allNetworks.firstElement(), rI);
					}

					if (verbose)
						System.out.println("> Reattaching cluster networks");

					// attaching networks to root networks
					for (HybridNetwork n : allNetworks) {

						rN = new ReattachNetworks(n, rI, numOfNets, numOfShiftNets, uniquePhyloTrees.length, views,
								cores, myNetPool, taxaOrdering, constraints, this, verbose, doClusterReduction, phyloTrees);
						rN.start();
						rN.join();
						networks.addAll(rN.getNetworks());

					}

				} else {

					// compute networks
					HybridNetwork n = cTHybrid.getClusterNetwork();
					rN = new ReattachNetworks(n, rI, numOfNets, numOfShiftNets, uniquePhyloTrees.length, views, cores,
							myNetPool, taxaOrdering, constraints, this, verbose, doClusterReduction, phyloTrees);
					rN.start();
					rN.join();

					for (MyPhyloTree network : rN.getNetworks()) {
						taxaOrdering = rI.addLeafLabels(network, taxaOrdering);
						networks.add(network);
					}

				}

				if (clusterChecker.doCheckAddTaxa() || clusterChecker.doCheckTime()) {
					Vector<MyPhyloTree> filteredNetworks = new Vector<MyPhyloTree>();
					for (MyPhyloTree net : networks) {
						if (clusterChecker.doCheckAddTaxa() && net.getAddTaxaDegree() == addTaxaValue)
							filteredNetworks.add(net);
						if (clusterChecker.doCheckTime() && net.getTimeDegree() == timeConsValue)
							filteredNetworks.add(net);
					}
					networks.clear();
					networks.addAll(filteredNetworks);
				}

				for (ClusterThread t : clusterThreads)
					t.interrupt();
				
				for (MyPhyloTree n : networks)
					rI.addLeafLabels(n, taxaOrdering);
				
				// reattach unique taxa to each network
				ReattachUniqueTaxa reattachUniqueTaxa = new ReattachUniqueTaxa(phyloTrees, rI, taxaOrdering);
				if (!rI.containsClusterNetwork()) {
					for (MyPhyloTree n : networks)
						reattachUniqueTaxa.run(n);
				}

				// computing weight for each hybrid node
				new ComputeNodeWeightsSparse(networks, trees.length, taxaOrdering).computeOcurrences();

				numOfNets = networks.size();
				for (HybridView view : views)
					view.reportRetNetworks(numOfNets, hybridNumber);

				notifyObserver(networks);

			} else if ((maxK == null || maxK >= 0)) {
				notifyObserver(edgeNumber);
				for (HybridView view : views)
					view.reportEdgeNumber(hybridNumber);
			} else {
				hybridNumber = -1;
				edgeNumber = -1;
			}

			runtime = (System.currentTimeMillis() - time) / 1000;

		} catch (Exception e) {
			System.err.println("ERROR: Computation aborted!");
			e.printStackTrace();
		}

		if (verbose)
			System.out.println("------------------------------------------------------");

		myPool.shutDown();
		myNetPool.forceShutDown();
		hasFinished = true;
		stopThreads();

	}

	private void setTimeConsValue(HybridNetwork n, ReplacementInfo rI) {
		CheckConstraints clusterChecker = new CheckConstraints(constraints, taxaOrdering, rI);
		timeConsValue += clusterChecker.estimateTimeConsistencyDegree(new SparseNetwork(n), Integer.MAX_VALUE, false,
				-1);
		for (String repLabel : rI.getReplacementLabels())
			timeConsValue += clusterChecker.estimateTimeConsistencyDegree(new SparseNetwork(rI.getLabelToNetworks()
					.get(repLabel).firstElement()), Integer.MAX_VALUE, false, -1);
	}

	private void setAddTaxaValue(HybridNetwork n, ReplacementInfo rI) {
		CheckConstraints clusterChecker = new CheckConstraints(constraints, taxaOrdering, rI);
		addTaxaValue += clusterChecker.estimateAddTaxaDegree(new SparseNetwork(n), Integer.MAX_VALUE, false, -1);
		for (String repLabel : rI.getReplacementLabels())
			addTaxaValue += clusterChecker.estimateAddTaxaDegree(new SparseNetwork(rI.getLabelToNetworks()
					.get(repLabel).firstElement()), Integer.MAX_VALUE, false, -1);
	}

	private void setLevelValue(HybridNetwork n, ReplacementInfo rI) {
		CheckConstraints clusterChecker = new CheckConstraints(constraints, taxaOrdering, rI);
		int level = clusterChecker.estimateLevelDegree(new SparseNetwork(n), Integer.MAX_VALUE);
		levelDegree += level;
		for (String repLabel : rI.getReplacementLabels()) {
			level = clusterChecker.estimateLevelDegree(new SparseNetwork(rI.getLabelToNetworks().get(repLabel)
					.firstElement()), Integer.MAX_VALUE);
			levelDegree += level;
		}
	}

	public class TimeThread extends Thread {
		final double time = System.currentTimeMillis();

		public void run() {
			while (!hasFinished) {
				for (HybridView view : views)
					view.updateTime((long) (System.currentTimeMillis() - time));
				try {
					sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
				}
			}
		}
	}

	private void notifyObserver(Object info) {
		for (HybridLauncher obs : observers)
			obs.update(info);
	}

	public void addObserver(HybridLauncher launcher) {
		observers.add(launcher);
	}

	public void addView(HybridView view) {
		views.add(view);
	}

	public int getNumOfNetworks(ReplacementInfo rI) {
		int i = 1;
		for (String s : rI.getLabelToNetworks().keySet())
			i *= rI.getLabelToNetworks().get(s).size();
		return i;
	}

	public void stopThreads() {
		if (rN != null) {
			rN.stopThread();
		} else {
			stop = true;
			for (ClusterThread cT : clusterThreads)
				cT.stopThread();
			myNetPool.forceShutDown();
			myPool.shutDown();
			if (timeThread != null)
				timeThread.interrupt();
		}
	}

	public void updateAddTaxaValue(int value) {
		if (value >= 0)
			addTaxaValue = value < addTaxaValue ? value : addTaxaValue;
	}

	public void updateTimeConsValue(int value) {
		if (value >= 0)
			timeConsValue = value < timeConsValue ? value : timeConsValue;
	}

	public Vector<MyPhyloTree> getNetworks() {
		return networks;
	}

	public int getNumOfNets() {
		return numOfNets;
	}

	public void clear() {
		networks.clear();
		clusterThreads.clear();
	}

	public double getRuntime() {
		return runtime;
	}

	public Integer getClusterSize() {
		return clusterThreads.size();
	}

	public Integer getMaxNumber() {
		return maxNumber;
	}

	public int getHybridNumber() {
		return hybridNumber;
	}

	public int getEdgeNumber() {
		return edgeNumber;
	}

	public int getAddTaxaValue() {
		return addTaxaValue;
	}

	public int getTimeConsValue() {
		return timeConsValue;
	}

	public int getLevelValue() {
		return levelDegree;
	}

	public int getRecCalls() {
		return recCalls;
	}

	public MyPhyloTree[] getPhyloTrees() {
		return phyloTrees;
	}

	public int getCores() {
		return cores;
	}

}
