/*
 *   ReattachClustersPara.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.attachNetworks;

import dendroscope.hybroscale.model.HybridManager;
import dendroscope.hybroscale.model.parallelization.MyNetPriorThreadPool;
import dendroscope.hybroscale.model.reductionSteps.ReplacementInfo;
import dendroscope.hybroscale.model.treeObjects.HybridNetwork;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.model.util.CheckConstraints;
import dendroscope.hybroscale.util.graph.MyPhyloTree;
import dendroscope.hybroscale.view.HybridView;

import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This method replaces distinct leaves of a resolved network by other resolved
 * networks producing a set of new resolved networks.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class ReattachClustersPara {

	private AtomicInteger activeThreads = new AtomicInteger(0), numOfNetworks = new AtomicInteger(0);
	private int numOfNets, numOfInputTrees, numOfShiftNets;
	private Vector<MyPhyloTree> networks = new Vector<MyPhyloTree>();
	private Vector<SparseNetwork> sparseNetworks = new Vector<SparseNetwork>();
	private boolean stop = false;
	private Vector<HybridView> views;
	private ReplacementInfo rI;
	private CheckConstraints checker;
	private HybridManager hM;

	private MyNetPriorThreadPool myThreadPool;
	private CountDownLatch countDownLatch;
	private Vector<String> taxaOrdering;
	private boolean verbose;
	private int progress = 0, tmpProgress = 0;
	private HybridNetwork n;
	private MyPhyloTree[] trees;

	public ReattachClustersPara(HybridNetwork n, ReplacementInfo rI, int numOfNets, int numOfShiftNets,
								int numOfInputTrees, Vector<HybridView> views, int cores, MyNetPriorThreadPool myThreadPool,
								Vector<String> taxaOrdering, String constraints, HybridManager hM, boolean verbose, MyPhyloTree[] trees) {
		this.n = n;
		this.numOfNets = numOfNets;
		this.numOfShiftNets = numOfShiftNets;
		this.numOfInputTrees = numOfInputTrees;
		this.views = views;
		this.rI = rI;
		this.checker = new CheckConstraints(constraints, taxaOrdering, rI);
		this.hM = hM;
		this.myThreadPool = myThreadPool;
		this.taxaOrdering = taxaOrdering;
		this.verbose = verbose;
		this.trees = trees;
	}

	public Vector<MyPhyloTree> run() {

		// generating several networks by adding each combination of all
		// networks representing a minimal common cluster replaced before
		// REMARK: a taxon can replace more than one network since one MAAF
		// represents several networks and there can be more than one MAAF

		countDownLatch = new CountDownLatch(1);
		myThreadPool
				.submit(new NetworkGenerator(n, taxaOrdering, rI, numOfInputTrees, checker, this, hM, myThreadPool, trees));

		getStoppingThread().start();
		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			setStop(true);
			e.printStackTrace();
		}

		if (checker.doCheckAddTaxa() || checker.doCheckTime()) {
			Vector<MyPhyloTree> filteredNetworks = new Vector<MyPhyloTree>();
			for (MyPhyloTree net : networks) {
				if (checker.doCheckAddTaxa() && net.getAddTaxaDegree() == null)
					net.setAddTaxaDegree(hM.getAddTaxaValue());
				if (checker.doCheckAddTaxa() && net.getAddTaxaDegree() != -1)
					filteredNetworks.add(net);
				if (checker.doCheckTime() && net.getTimeDegree() == null)
					net.setTimeDegree(hM.getTimeConsValue());
				if (checker.doCheckTime() && net.getTimeDegree() != -1)
					filteredNetworks.add(net);
			}
			networks.clear();
			networks.addAll(filteredNetworks);
		}

		if(verbose)
			System.out.println("Result: "+networks.size()+" networks computed");

		return networks;

	}

	public void reportStarting(Thread t) {
		activeThreads.incrementAndGet();
	}

	public void reportFinishing(Thread t) {
		activeThreads.decrementAndGet();
	}

	public synchronized void reportNetwork(MyPhyloTree n) {

		numOfNetworks.incrementAndGet();

		networks.add(new MyPhyloTree(n));
		progress = (int) Math.floor(((double) (networks.size() - numOfShiftNets) / (numOfNets + 1.)) * 100.);
		progress = progress < 0 ? 0 : progress;
		if (verbose && progress % 10 == 0 && progress != tmpProgress) {
			System.out.println(progress + "% (" + networks.size() + ") of all networks calculated...");
			tmpProgress = progress;
		}

		for (HybridView view : views)
			view.setInfo(progress + "% (" + networks.size() + ") of all networks calculated...");

	}

	public Thread getStoppingThread() {
		Thread t = new Thread(new Thread() {
			public void run() {
				while (!stop) {
					try {
						sleep(1000);
						if (activeThreads.compareAndSet(0, 0))
							stopReattachment();
					} catch (Exception e) {
						e.printStackTrace();
						stopReattachment();
					}
				}
			}

		});
		return t;
	}

	private void stopReattachment() {
		stop = true;
		if (countDownLatch != null)
			countDownLatch.countDown();
		myThreadPool.stopCurrentExecution();
	}

	public void setStop(boolean stop) {
		if (stop)
			stopReattachment();
	}

	public Vector<MyPhyloTree> getNetworks() {
		return networks;
	}

	public void increaseNumOfShiftNets(int n) {
		numOfShiftNets += n;
	}

	public int getNumOfShiftNets() {
		return numOfShiftNets;
	}

}
