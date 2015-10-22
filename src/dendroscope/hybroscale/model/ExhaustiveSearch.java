package dendroscope.hybroscale.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import dendroscope.hybroscale.model.HybridManager.Computation;
import dendroscope.hybroscale.model.parallelization.MyNetPriorThreadPool;
import dendroscope.hybroscale.model.parallelization.MyThreadPool;
import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.model.treeObjects.SparseNetEdge;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.model.util.CheckConstraints;
import dendroscope.hybroscale.model.util.IsomorphismCheck;
import dendroscope.hybroscale.terminals.TerminalManager;
import dendroscope.hybroscale.view.HybridView;

/**
 * This function computes all maximum acyclic agreement forests of two rooted,
 * bifurcating phylogenetic trees.
 * 
 * @author Benjamin Albrecht, 6.2010
 */

public class ExhaustiveSearch extends Thread {

	private MyNetPriorThreadPool netExec;
	private ClusterThread clusterThread;
	private MyThreadPool threadPool;
	private Boolean isNotified = false;
	private HybridTree[] trees;
	private Vector<String> taxaOrdering;

	private Hashtable<Integer, Vector<SparseNetwork>> edgesToMAAFs = new Hashtable<Integer, Vector<SparseNetwork>>();
	private Integer[] retResults;

	private Integer[] recCalls;
	private int sumOfCalls = 0;

	private Vector<Future<?>> futures = new Vector<Future<?>>();
	private Thread reportProgress;
	private HashSet<Vector<HybridTree>> acyclicSetOfForests = new HashSet<Vector<HybridTree>>();
	private Vector<Runnable> threads = new Vector<Runnable>();
	private Vector<Integer> progress = new Vector<Integer>();
	private boolean isStopped = false;
	private boolean stop = false;
	private boolean verbose = false;
	private HybridManager.Computation compValue;
	private int fixedLowerBound = 0;
	private Integer maxK;
	private Vector<HybridView> views;
	private boolean speedUp;
	private CheckConstraints checker;

	public boolean hasFinished = false;
	public boolean subissionCompl = false;

	public ExhaustiveSearch(HybridTree[] trees, MyThreadPool threadPool, ClusterThread clusterThread,
			HybridManager.Computation compValue, int lowerBound, MyNetPriorThreadPool netExec, Integer maxK,
			Vector<HybridView> views, Vector<String> taxaOrdering, boolean speedUp, CheckConstraints checker, boolean verbose) {
		this.threadPool = threadPool;
		this.clusterThread = clusterThread;
		this.trees = trees;
		this.compValue = compValue;
		this.fixedLowerBound = lowerBound;
		this.netExec = netExec;
		this.maxK = maxK;
		this.views = views;
		this.taxaOrdering = taxaOrdering;
		this.speedUp = speedUp;
		this.checker = checker;
		this.verbose = verbose;
	}

	public void run() {

		try {

			// remove isomorphic trees
			Vector<HybridTree> uniqueTrees = new Vector<HybridTree>();
			Vector<Integer> isoVector = new Vector<Integer>();
			Hashtable<Integer, Vector<Integer>> isoMapping = new Hashtable<Integer, Vector<Integer>>();
			for (int i = 0; i < trees.length; i++) {
				uniqueTrees.add(trees[i]);
				for (int j = i + 1; j < trees.length; j++) {
					if ((new IsomorphismCheck()).run(trees[i], trees[j], taxaOrdering)) {
						uniqueTrees.remove(trees[i]);
						isoMapping.put(j, new Vector<Integer>());
						isoMapping.get(j).add(i);
						// System.out.println(clusterThread+"A: "+isoMapping);
						if (isoMapping.containsKey(i)) {
							isoMapping.get(j).addAll(isoMapping.get(i));
							isoMapping.remove(i);
							// System.out.println(clusterThread+"B: "+isoMapping);
						}
						// System.out.println(clusterThread+"isoVector: "+i);
						isoVector.add(i);
						break;
					}
				}
			}

			trees = new HybridTree[uniqueTrees.size()];
			int index = 0;
			for (HybridTree t : uniqueTrees)
				trees[index++] = t;

			clusterThread.setTrees(trees);
			clusterThread.setIsoVector(isoVector);
			clusterThread.setIsoMapping(isoMapping);

			if (trees.length == 1) {

				// all trees are isomorphic - return first one
				SparseNetwork n = new SparseNetwork(trees[0]);
				for (SparseNetEdge e : n.getEdges())
					e.addIndex(0);
				clusterThread.getMaxForests().add(n);
				clusterThread.setHybridNumber(0);
				clusterThread.setEdgeNumber(0);

				for (HybridView view : views) {
					view.setProgress(clusterThread, 100);
					view.setDetails(clusterThread, " Network with hybrid number " + 0 + " computed. ");
				}

			} else {

				// computing lower bound for parameter k
				int approximation = 0;
				if (trees.length >= 2 && fixedLowerBound == 0) {
					for (int i = 0; i < trees.length - 1; i++) {
						for (int j = i + 1; j < trees.length; j++) {

							int upperBound, lowerBound = 0;
							TerminalManager tM = new TerminalManager(trees[i], trees[j], taxaOrdering, netExec,
									lowerBound, maxK);
							tM.run();
							lowerBound = tM.getResult();
							tM = null;

							approximation = approximation < lowerBound ? lowerBound : approximation;
						}
					}
				} else
					approximation = fixedLowerBound;

				// initializing result array
				int numOfEdges = trees[0].getNumberOfEdges() > 2 ? trees[0].getNumberOfEdges() - 2 : 1;

				if (maxK == null)
					retResults = new Integer[numOfEdges * trees.length];
				else {
					maxK = maxK > numOfEdges * trees.length ? numOfEdges * trees.length : maxK;
					maxK = maxK < 0 ? -1 : maxK;
					retResults = new Integer[maxK + 1];
				}
				recCalls = new Integer[retResults.length];
				for (int i = 0; i < recCalls.length; i++)
					recCalls[i] = 0;
				for (int i = 0; i < retResults.length; i++) {
					if (i < approximation) {
						retResults[i] = 0;
						progress.add(100);
					} else
						retResults[i] = -1;
				}

				clusterThread.setTimeStart(System.currentTimeMillis());

				if (!stop && (trees.length > 2 || compValue == Computation.EDGE_NETWORK)) {

					// creating forest thread for each k
					int border = maxK != null ? maxK + 1 : numOfEdges;

					int priorityCounter = 0;
					for (int k = approximation; k < border; k++) {

						HybridTree[] treeCopies = new HybridTree[trees.length];
						for (int j = 0; j < trees.length; j++) {
							treeCopies[j] = new HybridTree(trees[j], false, taxaOrdering);
							treeCopies[j].update();
						}

						ForestThread t = new ForestThread(this, treeCopies, k, compValue, netExec, taxaOrdering,
								speedUp, checker, verbose);
						int priority = priorityCounter;
						t.setMyPriority(priority);
						priorityCounter += 100;
						progress.add(0);
						threads.add(t);

						if (trees.length == 2)
							break;

					}

					reportProgress = reportProgress();
					for (HybridView view : views)
						view.setStatus(clusterThread, "Computing Networks...");
					reportProgress.start();

					if (maxK == null || approximation < border) {

						for (Runnable t : threads) {
							futures.add(threadPool.runTask(t));
							if (stop || isStopped)
								break;
							else
								sleep(500);
						}

						subissionCompl = true;

						if (!stop && !isStopped) {
							// sleep(2000);
							if (!isNotified) {
								synchronized (this) {
									this.wait();
								}
							}
						}

					}

				} else {
					clusterThread.setHybridNumber(approximation);
					clusterThread.setEdgeNumber(approximation);
					for (HybridView view : views) {
						view.setProgress(clusterThread, 100);
						view.setDetails(clusterThread, "Network with reticulation number " + (approximation)
								+ " computed! ");
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// System.out.println(this + " FINISHED");

		if (reportProgress != null)
			reportProgress.interrupt();
		hasFinished = true;

	}

	public synchronized void reportReticulationResult(ConcurrentHashMap<Integer, Vector<SparseNetwork>> result, int k,
			ForestThread forestThread, int calls) {

		// System.out.println(this+"+++++++++++ "+k+" "+forestThread.getMyPriority()+" "+result.size());

		recCalls[k] = calls;

		// storing computed networks
		if ((result.size() == 0 && retResults[k] == -1)
				|| (result.size() != 0 && result.get(result.keySet().iterator().next()).isEmpty())) {
			retResults[k] = 0;

		} else if (result.size() > 0) {
			for (int key : result.keySet()) {
				if (!edgesToMAAFs.containsKey(key))
					edgesToMAAFs.put(key, result.get(key));
				else
					edgesToMAAFs.get(key).addAll(result.get(key));
				// System.out.println(result.get(key).size());
			}
			retResults[k] = 1;
		}

		// for (HybridTree t : trees)
		// System.out.println(t + ";");
		// System.out.println(" -> " + this + " " + k + " " + new Date());
		// System.out.println("retResults[" + k + "]: " + retResults[k]);
		// for (int i = 0; i < retResults.length; i++)
		// System.out.print(retResults[i] + " ");
		// System.out.println();

		result.clear();
		result = null;
		sumOfCalls = 0;

		for (int i = 0; i < retResults.length; i++) {
			sumOfCalls += recCalls[i];
			if (isNotified) {
				break;
			} else if (!isStopped && retResults[i] == -1) {
				break;
			} else if ((!isStopped && retResults[i] == 1) || (i == retResults.length - 1 && retResults[i] == 0)) {

				// reporting result to cluster thread
				if (edgesToMAAFs.containsKey(i)) {
					Iterator<SparseNetwork> it = edgesToMAAFs.get(i).iterator();
					while (it.hasNext())
						clusterThread.getMaxForests().add(it.next());
					clusterThread.setSumOfCalls(sumOfCalls);
					clusterThread.setHybridNumber(i);
					clusterThread.setEdgeNumber(i);
				} else {
					clusterThread.setSumOfCalls(sumOfCalls);
					clusterThread.setHybridNumber(-1);
					clusterThread.setEdgeNumber(-1);
				}

				// stopping computation of this cluster
				isNotified = true;
				isStopped = true;
				for (Runnable fT : threads)
					((ForestThread) fT).stopThread();
				threadPool.removeThreads(threads);

				for (HybridView view : views) {
					view.setProgress(clusterThread, 100);
					if (compValue == Computation.EDGE_NETWORK) {
						int numOfNets = edgesToMAAFs.containsKey(i) ? edgesToMAAFs.get(i).size() : 0;
						view.setDetails(clusterThread, numOfNets + " network(s) with reticulation number " + (i)
								+ " computed! ");
					} else {
						view.setDetails(clusterThread, "Network with reticulation number " + (i) + " computed! ");
					}
				}

				synchronized (this) {
					this.notify();
					this.notifyAll();
				}
				break;
			}

		}

		if (retResults[retResults.length - 1] == 0) {
			isNotified = true;
			isStopped = true;
			threadPool.removeThreads(threads);
			synchronized (this) {
				this.notify();
				this.notifyAll();
			}
		}

	}

	public Thread reportProgress() {
		Thread t = new Thread(new Thread() {
			public void run() {
				Long time = System.currentTimeMillis();
				while (!isStopped && !views.isEmpty()) {
					for (HybridView view : views) {
						int i = 1;
						while (!isStopped && retResults[i] != -1)
							i++;
						view.setProgress(clusterThread, getProcent(i, retResults.length - 1));
						String progress = " Searching for networks with reticulation number " + (i) + "/"
								+ (retResults.length - 2) + ". (" + ((System.currentTimeMillis() - time) / 1000) + "s)";
						String result = "\n---\nPreliminary Results:";

						int k = 0;
						while (k < threads.size()
								&& (((ForestThread) threads.get(k)).getCurrentNumberOfNetworks() == null || ((ForestThread) threads
										.get(k)).getCurrentNumberOfNetworks() == 0))
							k++;
						String currentResult = k < threads.size() ? "\n"
								+ ((ForestThread) threads.get(k)).getCurrentNumberOfNetworks()
								+ " unreduced minimal network(s) computed so far." : "";

						view.setDetails(clusterThread, progress + result + currentResult);
						if (retResults[i] != -1) {
							i++;
							time = System.currentTimeMillis();
						}
					}
					try {
						sleep(500);
					} catch (InterruptedException e) {
						// e.printStackTrace();
					}
				}

			}

			private int getProcent(double i, double j) {
				return (int) Math.round((i / j) * 100);
			}

		});
		return t;
	}

	public void reportProgress(int k, int i) {
		progress.set(k, i);
	}

	public HashSet<Vector<HybridTree>> getAcyclicSetOfForests() {
		return acyclicSetOfForests;
	}

	public int getSumOfCalls() {
		return sumOfCalls;
	}

	public void stopNextThreads(ForestThread t) {
		int index = threads.indexOf(t) + 1;
		for (int i = index; i < threads.size(); i++)
			((ForestThread) threads.get(i)).stopThread();
	}

	public void stopThread() {
		stop = true;
		for (Runnable t : threads) {
			((ForestThread) t).stopThread();
		}
		if (reportProgress != null)
			reportProgress.interrupt();
		if (retResults != null) {
			for (int i = 1; i < retResults.length; i++) {
				if (retResults[i] != 0)
					clusterThread.setEdgeNumber(i);
			}
		}
		isStopped = true;
		threadPool.removeThreads(threads);
		synchronized (this) {
			this.notify();
			this.notifyAll();
		}
	}

	public Vector<ForestThread> getThreads() {
		Vector<ForestThread> forestThreads = new Vector<ForestThread>();
		for (Runnable fT : threads)
			forestThreads.add((ForestThread) fT);
		return forestThreads;
	}

	public void reportFinishing(ForestThread t) {
		if (subissionCompl && !isStopped) {
			threads.remove(t);
			threadPool.removeThread(t);
			t = null;
		}
	}

	public boolean hasNoResult() {
		if (retResults == null)
			return false;
		for (int i = 0; i < retResults.length; i++) {
			if (retResults[i] != 0)
				return false;
		}
		return true;
	}

}
