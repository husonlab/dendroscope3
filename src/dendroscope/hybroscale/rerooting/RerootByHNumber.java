/*
 *   RerootByHNumber.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.rerooting;

import dendroscope.hybroscale.model.HybridManager;
import dendroscope.hybroscale.model.HybridManager.Computation;
import dendroscope.hybroscale.model.attachNetworks.ReattachSubtrees;
import dendroscope.hybroscale.model.reductionSteps.ReplacementInfo;
import dendroscope.hybroscale.model.reductionSteps.SubtreeReduction;
import dendroscope.hybroscale.model.treeObjects.HybridNetwork;
import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.*;

public class RerootByHNumber extends Thread {

	private int minHNumber;
	private Vector<MyPhyloTree[]> bestTreeSets;
	private Vector<String> taxaOrdering;
	private ReplacementInfo rI;
	private CountDownLatch countDownLatch;
	private Vector<HNumberThread> hThreads;
	private HashMap<Thread, Future<?>> threadToFuture;

	private MyPhyloTree[] trees;
	private int cores;
	private Integer upperBound;
	private RerootingView view;

	private boolean isInterrupted = false;

	public RerootByHNumber(RerootingView view, String[] treeStrings, int cores, Integer upperBound) {
		super();
		this.view = view;
		this.cores = cores;
		this.upperBound = upperBound;

		trees = new MyPhyloTree[treeStrings.length];
		for (int i = 0; i < treeStrings.length; i++) {
			MyPhyloTree t = new MyPhyloTree();
			t.parseBracketNotation(treeStrings[i]);
			trees[i] = t;
		}

	}

	public void run() {

		try {

			minHNumber = upperBound != null ? upperBound : Integer.MAX_VALUE;
			bestTreeSets = new Vector<MyPhyloTree[]>();
			rI = new ReplacementInfo();

			taxaOrdering = new Vector<String>();
			for (MyPhyloTree t : trees) {
				for (MyNode l : t.getLeaves()) {
					if (!taxaOrdering.contains(l.getLabel()))
						taxaOrdering.add(l.getLabel());
				}
			}
			HybridTree[] hTrees = new HybridTree[trees.length];
			for (int i = 0; i < trees.length; i++)
				hTrees[i] = new HybridTree(trees[i], false, taxaOrdering);
			taxaOrdering = new SubtreeReduction().run(hTrees, rI, taxaOrdering);
			for (int i = 0; i < trees.length; i++) {
				trees[i] = hTrees[i];
				for (MyNode v : trees[i].getNodes()) {
					if (v.getOutDegree() != 0)
						v.setLabel("");
				}
			}

			Vector<Vector<MyEdge>> treeEdges = new Vector<Vector<MyEdge>>();
			for (MyPhyloTree t : trees)
				treeEdges.add(t.getEdges());

			Vector<MyEdge> invalidEdges = cmpInvalidEdges();
			Vector<Vector<MyEdge>> allEdgeCombis = cmpAllEdgeCombis(treeEdges, invalidEdges);

			hThreads = new Vector<HNumberThread>();
			for (int i = 0; i < allEdgeCombis.size(); i++) {
				Vector<MyEdge> edgeCombi = allEdgeCombis.get(i);
				hThreads.add(new HNumberThread(edgeCombi));
			}

			countDownLatch = new CountDownLatch(hThreads.size());
			ThreadPoolExecutor executor = new ThreadPoolExecutor(cores, cores, Integer.MAX_VALUE, TimeUnit.SECONDS,
					new LinkedBlockingQueue<Runnable>());

			threadToFuture = new HashMap<Thread, Future<?>>();
			for (HNumberThread hThread : hThreads) {
				Future<?> f = executor.submit(hThread);
				threadToFuture.put(hThread, f);
			}

			countDownLatch.await();
			executor.shutdown();

			if(!isInterrupted)
				processBestTrees();

		} catch (Exception e) {

		}

		if (view != null)
			view.reportResult();

	}

	private void processBestTrees(){
		for (MyPhyloTree[] treeSet : bestTreeSets) {
			for (int i = 0; i < treeSet.length; i++) {
				HybridNetwork n = new HybridNetwork(treeSet[i], false, taxaOrdering);
				treeSet[i] = new ReattachSubtrees().run(n, rI, bestTreeSets.size() * trees.length);
				for (MyNode v : treeSet[i].getNodes()) {
					if (v.getOutDegree() != 0)
						v.setLabel("");
				}
			}
		}
	}

	public class HNumberThread extends Thread implements Runnable {

		private MyPhyloTree[] rootedTrees;
		private HybridManager hM;

		public HNumberThread(Vector<MyEdge> edgeCombi) {
			this.rootedTrees = reRootTrees(edgeCombi);
		}

		public void run() {

			hM = new HybridManager(rootedTrees, Computation.EDGE_NUMBER, 1, minHNumber, false, "", true, false);
			hM.start();
			try {
				hM.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			reportResult(hM.getHybridNumber(), rootedTrees);
			countDownLatch.countDown();
			stopComp();

			int progress = (int) (100 - ((countDownLatch.getCount() * 100) / hThreads.size()));
			if (view != null) {
				view.setProgress(progress);
				view.setInfo("So far, " + bestTreeSets.size() + " tree set(s) with hybridization number " + minHNumber
						+ " computed...");
			}

			// System.out.println(progress);

		}

		public void stopComp() {
			if (hM != null)
				hM.stopThreads();
			hM = null;
		}

	}

	private synchronized void reportResult(int hybridNumber, MyPhyloTree[] treeSet) {
		if (hybridNumber >= 0 && hybridNumber < minHNumber) {
			minHNumber = hybridNumber;
			bestTreeSets.clear();
			bestTreeSets.add(treeSet);
		} else if (hybridNumber == minHNumber)
			bestTreeSets.add(treeSet);
	}

	private Vector<MyEdge> cmpInvalidEdges() {
		Vector<MyEdge> invalidEdges = new Vector<MyEdge>();
		for (MyPhyloTree t : trees) {
			MyNode r = t.getRoot();
			if (r.getOutDegree() == 2)
				invalidEdges.add(r.getFirstOutEdge());
		}
		return invalidEdges;
	}

	private MyPhyloTree[] reRootTrees(Vector<MyEdge> edgeCombi) {

		MyPhyloTree[] rootedTrees = new MyPhyloTree[trees.length];
		for (int i = 0; i < edgeCombi.size(); i++) {

			MyEdge e = edgeCombi.get(i);
			e.setInfo("R");
			MyPhyloTree tR = new MyPhyloTree(trees[i]);
			e.setInfo(null);

			MyEdge splitEdge = null;
			for (MyEdge eR : tR.getEdges()) {
				if (eR.getInfo() != null && eR.getInfo().equals("R")) {
					splitEdge = eR;
					break;
				}
			}

			MyNode r = tR.getRoot();
			MyNode v = insertNode(splitEdge, tR);
			reRootTrees(v, tR);
			tR.setRoot(v);
			supressNode(r, tR);

			rootedTrees[i] = tR;

		}

		return rootedTrees;
	}

	private void reRootTrees(MyNode v, MyPhyloTree t) {
		Vector<MyNode> modNodes = new Vector<MyNode>();
		MyNode p = v;
		while (p.getInDegree() != 0) {
			modNodes.add(p);
			p = p.getFirstInEdge().getSource();
		}
		for (MyNode y : modNodes) {
			MyEdge e = y.getFirstInEdge();
			MyNode x = e.getSource();
			t.deleteEdge(e);
			t.newEdge(y, x);
		}
	}

	private void supressNode(MyNode v, MyPhyloTree t) {
		if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
			MyEdge e1 = v.getFirstInEdge();
			MyNode p = e1.getSource();
			MyEdge e2 = v.getFirstOutEdge();
			MyNode c = e2.getTarget();
			t.deleteEdge(e1);
			t.deleteEdge(e2);
			t.newEdge(p, c);
		}
	}

	private MyNode insertNode(MyEdge e, MyPhyloTree t) {
		MyNode p = e.getSource();
		MyNode c = e.getTarget();
		t.deleteEdge(e);
		MyNode x = t.newNode();
		t.newEdge(p, x);
		t.newEdge(x, c);
		return x;
	}

	private Vector<Vector<MyEdge>> cmpAllEdgeCombis(Vector<Vector<MyEdge>> treeEdges, Vector<MyEdge> invalidEdges) {

		Vector<Vector<MyEdge>> allEdgeCombis = new Vector<Vector<MyEdge>>();
		int curTreeIndex = 0;
		Vector<MyEdge> edgeCombi = new Vector<MyEdge>();

		cmpAllEdgeCombisRec(edgeCombi, treeEdges, curTreeIndex, allEdgeCombis, invalidEdges);

		return allEdgeCombis;
	}

	private void cmpAllEdgeCombisRec(Vector<MyEdge> edgeCombi, Vector<Vector<MyEdge>> treeEdges, int curTreeIndex,
									 Vector<Vector<MyEdge>> allEdgeCombis, Vector<MyEdge> invalidEdges) {
		if (curTreeIndex < trees.length) {
			for (MyEdge e : treeEdges.get(curTreeIndex)) {
				if (!invalidEdges.contains(e)) {
					Vector<MyEdge> edgeCombiClone = (Vector<MyEdge>) edgeCombi.clone();
					edgeCombiClone.add(e);
					int newTreeIndex = curTreeIndex + 1;
					cmpAllEdgeCombisRec(edgeCombiClone, treeEdges, newTreeIndex, allEdgeCombis, invalidEdges);
				}
			}
		} else
			allEdgeCombis.add(edgeCombi);
	}

	public int getMinHNumber() {
		return minHNumber;
	}

	public Vector<MyPhyloTree[]> getBestTreeSets() {
		return bestTreeSets;
	}

	public void stopComp() {
		isInterrupted = true;
		if (hThreads != null) {
			for (HNumberThread thread : hThreads)
				thread.stopComp();
			processBestTrees();
		}
	}

}
