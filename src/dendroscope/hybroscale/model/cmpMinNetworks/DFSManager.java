package dendroscope.hybroscale.model.cmpMinNetworks;

import dendroscope.hybroscale.model.HybridManager.Computation;
import dendroscope.hybroscale.model.cmpAllMAAFs.RefinedFastApproxHNumber;
import dendroscope.hybroscale.model.parallelization.MyNetPriorThreadPool;
import dendroscope.hybroscale.model.treeObjects.*;
import dendroscope.hybroscale.model.util.CheckConstraints;
import dendroscope.hybroscale.model.util.ComputeSparseNodeWeights;
import dendroscope.hybroscale.model.util.HSumComparator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class DFSManager {

	private HybridTree[] trees;
	private int k;
	private Computation compValue;
	private Vector<String> taxaOrdering;

	private CountDownLatch countDownLatch = new CountDownLatch(1);
	private MyNetPriorThreadPool myThreadPool;
	boolean isStopped = false, verbose = false;
	private Integer[][] hNumberMatrix;
	private ConcurrentHashMap<SparseTree[], Vector<Integer>> orderingToTreeIndices = new ConcurrentHashMap<SparseTree[], Vector<Integer>>();
	private ConcurrentHashMap<Integer, Vector<SparseNetwork>> numberToNetworks = new ConcurrentHashMap<Integer, Vector<SparseNetwork>>();
	private Vector<SparseNetwork> minNetworks = new Vector<SparseNetwork>();
	private ConcurrentLinkedQueue<SparseNetwork> minNetworksInQueue = new ConcurrentLinkedQueue<SparseNetwork>();
	private NetworkMemory netMem;

	private boolean heuristicMode = false, speedUp = false;

	private ConcurrentHashMap<SparseNetwork, Integer> netToTimeDegree = new ConcurrentHashMap<SparseNetwork, Integer>();
	private ConcurrentHashMap<SparseNetwork, Integer> netToAddTaxaDegree = new ConcurrentHashMap<SparseNetwork, Integer>();
	private ConcurrentHashMap<SparseNetwork, Integer> netToLevelDegree = new ConcurrentHashMap<SparseNetwork, Integer>();
	private int bestTimeConsistentValue = Integer.MAX_VALUE, bestAddTaxaValue = Integer.MAX_VALUE,
			bestLevelValue = Integer.MAX_VALUE;
	private boolean checkTime = false, checkAddTaxa = false, checkLevel = false;;
	private int agCounter = 0;
	private CheckConstraints checker;

	public DFSManager(HybridTree[] trees, int k, Computation compValue, MyNetPriorThreadPool myThreadPool,
			Vector<String> taxaOrdering, boolean speedUp, CheckConstraints checker, boolean verbose) {

		this.trees = trees;
		this.k = k + 1;
		this.compValue = compValue;
		this.myThreadPool = myThreadPool;
		this.taxaOrdering = taxaOrdering;
		this.speedUp = speedUp;
		this.checker = checker;
		this.verbose = verbose;

		netMem = new NetworkMemory(taxaOrdering);
		checkTime = checker == null ? false : checker.doCheckTime();
		checkAddTaxa = checker == null ? false : checker.doCheckAddTaxa();
		checkLevel = checker == null ? false : checker.doCheckLevel();

		hNumberMatrix = new Integer[trees.length][trees.length];

	}

	public ConcurrentHashMap<Integer, Vector<SparseNetwork>> run() {

		Thread infoThread = getInfoThread();
		// infoThread.start();

		if (verbose)
			System.out.println("Searching for networks with hybridization number " + (k - 1));

		// System.out.println("********************************** " + (k - 1) +
		// " " + this + " isStopped: " + isStopped
		// + " #Trees: " + trees.length + " " + new Date());

		if (!isStopped) {

			for (SparseTree[] sparseTrees : cmpAllTreeOrderings()) {

				HybridTree[] hybridTrees = new HybridTree[trees.length];
				for (int i = 0; i < sparseTrees.length; i++)
					hybridTrees[i] = new HybridTree(sparseTrees[i].getPhyloTree(), false, taxaOrdering);
				// HybridTree[] hybridTrees = trees;
				
				SparseNetwork n = new SparseNetwork(sparseTrees[0].getPhyloTree());
				DFSUpdateNetworkThread thread = new DFSUpdateNetworkThread(k, n, new Vector<SparseNetEdge>(),
						new BitSet(), null, sparseTrees, hybridTrees, 1, taxaOrdering, 0, this, compValue,
						orderingToTreeIndices.get(sparseTrees), heuristicMode, myThreadPool, speedUp, checker,
						netMem);
				myThreadPool.submit(thread);

				if (speedUp)
					break;

			}

			Thread stoppingThread = getStoppingThread();
			stoppingThread.run();

			try {
				countDownLatch.await();
			} catch (Exception e) {
				e.printStackTrace();
				stopManager();
			}

		}

		// netMem.printStatistics();
		netMem.freeMemory();
		infoThread.interrupt();

		minNetworks = queueToVector(minNetworksInQueue);
		minNetworksInQueue = null;

		if (!minNetworks.isEmpty()) {
			if (doCheckTime() || doCheckAddTaxa() || doCheckLevel()) {
				Vector<SparseNetwork> bestTimeNetworks = new Vector<SparseNetwork>();
				for (SparseNetwork n : minNetworks) {
					if (doCheckAddTaxa() && !doCheckTime() && !doCheckLevel()
							&& netToAddTaxaDegree.get(n) <= bestAddTaxaValue)
						bestTimeNetworks.add(n);
					else if (!doCheckAddTaxa() && doCheckTime() && !doCheckLevel()
							&& netToTimeDegree.get(n) <= bestTimeConsistentValue)
						bestTimeNetworks.add(n);
					else if (!doCheckAddTaxa() && !doCheckTime() && doCheckLevel()
							&& netToLevelDegree.get(n) <= bestLevelValue)
						bestTimeNetworks.add(n);
					else if (doCheckAddTaxa() && doCheckTime() && !doCheckLevel()
							&& netToAddTaxaDegree.get(n) <= bestAddTaxaValue
							&& netToTimeDegree.get(n) <= bestTimeConsistentValue)
						bestTimeNetworks.add(n);
					else if (doCheckAddTaxa() && !doCheckTime() && doCheckLevel()
							&& netToAddTaxaDegree.get(n) <= bestAddTaxaValue
							&& netToLevelDegree.get(n) <= bestLevelValue)
						bestTimeNetworks.add(n);
					else if (!doCheckAddTaxa() && doCheckTime() && doCheckLevel()
							&& netToTimeDegree.get(n) <= bestTimeConsistentValue
							&& netToLevelDegree.get(n) <= bestLevelValue)
						bestTimeNetworks.add(n);
					else if (netToAddTaxaDegree.get(n) <= bestAddTaxaValue
							&& netToTimeDegree.get(n) <= bestTimeConsistentValue
							&& netToLevelDegree.get(n) <= bestLevelValue)
						bestTimeNetworks.add(n);
				}
				minNetworks.clear();
				minNetworks.addAll(bestTimeNetworks);
			}
			numberToNetworks.put(k - 1, minNetworks);
		}

		if (numberToNetworks.containsKey(k - 1) && compValue != Computation.EDGE_NUMBER && (trees.length >= 2)) {
			if (verbose)
				System.out.println("Filtering " + numberToNetworks.get(k - 1).size() + " networks...");
			reduceNetworksParallel(numberToNetworks.get(k - 1));
		}

		stopManager();

		return numberToNetworks;
	}

	private Vector<SparseNetwork> queueToVector(ConcurrentLinkedQueue<SparseNetwork> queue) {
		Vector<SparseNetwork> vector = new Vector<SparseNetwork>();
		vector.addAll(queue);
		return vector;
	}

	public void reportNetworks(DFSUpdateNetworkThread thread, int timeDegree, int addTaxaDegree, int levelDegree) {

		SparseNetwork n = thread.getNetwork();

		if (thread.getTreeIndex() == trees.length && !isStopped) {
			if (n.getRetNumber() == (k - 1)) {
				minNetworksInQueue.add(n);
				netToTimeDegree.put(n, timeDegree);
				netToAddTaxaDegree.put(n, addTaxaDegree);
				netToLevelDegree.put(n, levelDegree);
				if (compValue == Computation.EDGE_NUMBER && !isStopped)
					stopManager();
			}
		}

	}

	public void stopManager() {
		if (!isStopped) {
			isStopped = true;
			myThreadPool.stopCurrentExecution();
			minNetworks = queueToVector(minNetworksInQueue);
			countDownLatch.countDown();
		}
	}

	public Thread getStoppingThread() {
		Thread t = new Thread(new Thread() {
			public void run() {
				while (!isStopped) {
					// System.out.println("Stopping: "+k+" "+myThreadPool.getQueue().isEmpty()+" "+myThreadPool.getActiveCount());
					try {
						sleep(1000);
						if (myThreadPool.getQueue().isEmpty() && myThreadPool.getActiveCount() == 0)
							stopManager();
					} catch (Exception e) {
						e.printStackTrace();
						stopManager();
					}
				}
			}
		});
		return t;
	}

	public Thread getInfoThread() {
		Thread t = new Thread(new Thread() {
			public void run() {
				while (!isStopped) {
					try {
						sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						// e.printStackTrace();
					}
					System.out.println("********************************** " + (k - 1) + " " + this + " isStopped: "
							+ isStopped + " #Trees: " + trees.length + " " + new Date());
					if (minNetworksInQueue != null)
						System.out.println("#Networks: " + minNetworksInQueue.size());
					System.out.println("ActiveCount: " + myThreadPool.getActiveCount());
					System.out.println("CompletedCount: " + myThreadPool.getCompletedTaskCount());
					System.out.println("Queue Size: " + myThreadPool.getQueue().size());
					System.out.println("---");

				}
			}
		});
		return t;
	}

	private Vector<SparseTree[]> cmpAllTreeOrderings() {

		Vector<Vector<Integer>> diffOrderings = new Vector<Vector<Integer>>();
		for (int i = 0; i < trees.length - 1; i++) {
			for (int j = i + 1; j < trees.length; j++) {
				Vector<Integer> ordering = new Vector<Integer>();
				ordering.add(i);
				ordering.add(j);
				Vector<Vector<Integer>> allOrderings = new Vector<Vector<Integer>>();
				allOrderings.add(ordering);
				compAllOrderings(allOrderings);
				diffOrderings.addAll(allOrderings);
			}
		}

		for (int i = 0; i < trees.length - 1; i++) {
			for (int j = i + 1; j < trees.length; j++) {
				if (true) {
					int upperBound = new RefinedFastApproxHNumber().run(trees[i], trees[j], taxaOrdering,
							Integer.MAX_VALUE);
					int lowerBound = (int) (Math.ceil((double) upperBound / 4.));
					hNumberMatrix[i][j] = lowerBound;
					hNumberMatrix[j][i] = lowerBound;
				}
			}
		}

		ConcurrentHashMap<Vector<Integer>, Vector<Integer>> orderingToMaxima = new ConcurrentHashMap<Vector<Integer>, Vector<Integer>>();
		for (Vector<Integer> ordering : diffOrderings) {
			Vector<Integer> maxima = new Vector<Integer>();
			for (int i = 1; i < ordering.size(); i++) {
				int lastIndex = ordering.get(i);
				int max = Integer.MIN_VALUE;
				for (int j = 0; j < i; j++) {
					int currIndex = ordering.get(j);
					max = hNumberMatrix[lastIndex][currIndex] > max ? hNumberMatrix[lastIndex][currIndex] : max;
				}
				maxima.add(max);
			}
			orderingToMaxima.put(ordering, maxima);
		}
		Collections.sort(diffOrderings, new HSumComparator(orderingToMaxima));

		Vector<SparseTree[]> allTreeOrderings = new Vector<SparseTree[]>();
		for (Vector<Integer> ordering : diffOrderings) {

			SparseTree[] treeOrdering = new SparseTree[trees.length];
			int i = 0;
			for (int treeIndex : ordering) {
				treeOrdering[i] = new SparseTree(trees[treeIndex]);
				i++;
			}

			orderingToTreeIndices.put(treeOrdering, ordering);
			allTreeOrderings.add(treeOrdering);
		}

		return allTreeOrderings;

	}

	private void compAllOrderings(Vector<Vector<Integer>> allOrderings) {
		if (allOrderings.firstElement().size() < trees.length) {
			Vector<Vector<Integer>> newOrderings = new Vector<Vector<Integer>>();
			for (Vector<Integer> ordering : allOrderings) {
				for (int i = 0; i < trees.length; i++)
					if (!ordering.contains(i)) {
						Vector<Integer> newOrdering = (Vector<Integer>) ordering.clone();
						newOrdering.add(i);
						newOrderings.add(newOrdering);
					}
			}
			allOrderings.clear();
			allOrderings.addAll(newOrderings);
			compAllOrderings(allOrderings);
		}
	}

	private void reduceNetworksParallel(Vector<SparseNetwork> networks) {

		ComputeSparseNodeWeights cSnW = new ComputeSparseNodeWeights(networks, trees.length, taxaOrdering);
		cSnW.computeOcurrences();
		ConcurrentHashMap<Integer, Vector<SparseNetwork>> occToNetworks = new ConcurrentHashMap<Integer, Vector<SparseNetwork>>();

		for (SparseNetwork n : networks) {
			// int occ = 1;
			int occ = (int) Math.round(cSnW.getNetWeight(n));
			if (!occToNetworks.containsKey(occ))
				occToNetworks.put(occ, new Vector<SparseNetwork>());
			Vector<SparseNetwork> netsCopy = (Vector<SparseNetwork>) occToNetworks.get(occ).clone();
			netsCopy.add(n);
			occToNetworks.put(occ, netsCopy);
		}

		countDownLatch = new CountDownLatch(occToNetworks.size());
		Vector<ReductionThread> redThreads = new Vector<ReductionThread>();
		Vector<Integer> occValues = new Vector<Integer>();
		for (int key : occToNetworks.keySet())
			occValues.add(key);
		Collections.sort(occValues);
		for (int i = occValues.size() - 1; i >= 0; i--) {
			Vector<SparseNetwork> nets = occToNetworks.get(occValues.get(i));
			ReductionThread t = new ReductionThread(nets);
			redThreads.add(t);
		}

		if (trees.length >= 2) {
			try {
				for (ReductionThread t : redThreads)
					myThreadPool.submit(t);
				countDownLatch.await();
			} catch (Exception e) {
				stopManager();
				e.printStackTrace();
			}
		}

		networks.clear();
		for (ReductionThread t : redThreads) {
			for (SparseNetwork n : t.getNets())
				networks.add(n);
		}

	}

	public class ReductionThread implements Runnable {

		Vector<SparseNetwork> nets;

		public ReductionThread(Vector<SparseNetwork> nets) {
			this.nets = nets;
		}

		@Override
		public void run() {
			Vector<Integer> multipleIndices = new Vector<Integer>();
			for (int i = 0; i < nets.size() - 1; i++) {
				if (!multipleIndices.contains(i)) {
					for (int j = i + 1; j < nets.size(); j++) {
						if (!multipleIndices.contains(j)) {
							SparseNetwork n1 = nets.get(i);
							SparseNetwork n2 = nets.get(j);
							if (new NetworkIsomorphismCheck().run(n1, n2)) {
								// checkIsoEdges(n1, n2);
								multipleIndices.add(j);
							}
						}
					}
				}
			}
			Collections.sort(multipleIndices);
			for (int i = multipleIndices.size() - 1; i >= 0; i--)
				nets.removeElementAt(multipleIndices.get(i));
			countDownLatch.countDown();
		}

		private void checkIsoEdges(SparseNetwork n1, SparseNetwork n2) {
			HashMap<String, HashSet<String>> allN1Labels = new HashMap<String, HashSet<String>>();
			for (SparseNetNode v : n1.getNodes()) {
				if (v.getInDegree() > 1) {
					Vector<String> labels = new Vector<String>();
                    for (SparseNetEdge e : v.inEdges())
						labels.add(e.getIndices().toString());
					Collections.sort(labels);
					String s = "";
					for (String l : labels)
						s = s.concat(l);
					if (!allN1Labels.containsKey(v.getLabel()))
						allN1Labels.put(v.getLabel(), new HashSet<String>());
					allN1Labels.get(v.getLabel()).add(s);
				}
			}
			HashMap<String, HashSet<String>> allN2Labels = new HashMap<String, HashSet<String>>();
			for (SparseNetNode v : n2.getNodes()) {
				if (v.getInDegree() > 1) {
					Vector<String> labels = new Vector<String>();
                    for (SparseNetEdge e : v.inEdges())
						labels.add(e.getIndices().toString());
					Collections.sort(labels);
					String s = "";
					for (String l : labels)
						s = s.concat(l);
					if (!allN2Labels.containsKey(v.getLabel()))
						allN2Labels.put(v.getLabel(), new HashSet<String>());
					allN2Labels.get(v.getLabel()).add(s);
				}
			}
			for (String l : allN1Labels.keySet()) {
				for (String s1 : allN1Labels.get(l)) {
					if (!allN2Labels.containsKey(l)) {
						System.out.println("Wrong label: " + l + "\n" + n1.getPhyloTree().toMyBracketString() + "\n"
								+ n2.getPhyloTree().toMyBracketString() + "\n");
					}
					if (!allN2Labels.get(l).contains(s1)) {
						System.out.println("Wrong edge: " + s1 + "\n" + n1.getPhyloTree().toMyBracketString() + "\n"
								+ n2.getPhyloTree().toMyBracketString() + "\n");
					}
				}
			}
		}

		public Vector<SparseNetwork> getNets() {
			return nets;
		}

	}

	private void reduceNetworks(Vector<SparseNetwork> networks) {

		Vector<Integer> multipleIndices = new Vector<Integer>();
		for (int i = 0; i < networks.size() - 1; i++) {
			if (!multipleIndices.contains(i)) {
				for (int j = i + 1; j < networks.size(); j++) {
					if (!multipleIndices.contains(j)) {
						SparseNetwork n1 = networks.get(i);
						SparseNetwork n2 = networks.get(j);
						if (new NetworkIsomorphismCheck().run(n1, n2))
							multipleIndices.add(j);
					}
				}
			}
		}
		Collections.sort(multipleIndices);

		for (int i = multipleIndices.size() - 1; i >= 0; i--)
			networks.removeElementAt(multipleIndices.get(i));

	}

	public void increaseAGCounter(int recCounter) {
		agCounter += recCounter;
	}

	public ConcurrentHashMap<Integer, Vector<SparseNetwork>> getNumberToNetworks() {
		if (minNetworksInQueue != null)
			minNetworks = queueToVector(minNetworksInQueue);
		numberToNetworks.put(k - 1, minNetworks);
		return numberToNetworks;
	}

	public int getCurrentNumberOfNetworks() {
		if (minNetworksInQueue != null)
			return minNetworksInQueue.size();
		return minNetworks.size();
	}

	public int getBestTimeConsistentValue() {
		return bestTimeConsistentValue;
	}

	public int getBestAddTaxaValue() {
		return bestAddTaxaValue;
	}

	public int getBestLevelValue() {
		return bestLevelValue;
	}

	public void setBestTimeConsistentValue(int value) {
		if (value < bestTimeConsistentValue)
			bestTimeConsistentValue = value;
	}

	public void setBestAddTaxaValue(int value) {
		if (value < bestAddTaxaValue)
			this.bestAddTaxaValue = value;
	}

	public void setBestLevelValue(int value) {
		if (value < bestLevelValue)
			this.bestLevelValue = value;
	}

	public boolean doCheckTime() {
		return checkTime;
	}

	public boolean doCheckAddTaxa() {
		return checkAddTaxa;
	}

	public boolean doCheckLevel() {
		return checkLevel;
	}

	public int getCalls() {
		return agCounter;
	}

	public void freeMemory() {
		numberToNetworks = null;
		netToTimeDegree = null;
		netToAddTaxaDegree = null;
		netToLevelDegree = null;
		minNetworks = null;
	}

}
