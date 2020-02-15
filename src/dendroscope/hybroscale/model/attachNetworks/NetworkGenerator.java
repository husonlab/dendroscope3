/*
 *   NetworkGenerator.java Copyright (C) 2020 Daniel H. Huson
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
import dendroscope.hybroscale.model.util.CheckConstraints;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;
import dendroscope.hybroscale.util.lcaQueries.LCA_Query;

import java.util.*;

public class NetworkGenerator extends Thread {

	private MyPhyloTree n;
	private Vector<String> taxaOrdering;
	private boolean stop = false;
	private ReplacementInfo rI;
	private int numOfInputTrees;
	private CheckConstraints checker;
	private ReattachClustersPara manager;
	private HybridManager hM;
	private MyNetPriorThreadPool myThreadPool;

	private MyPhyloTree[] trees;
	private Vector<LCA_Query> lcaQueries = new Vector<LCA_Query>();
	private Vector<HashMap<BitSet, MyNode>> setToLeaves = new Vector<HashMap<BitSet, MyNode>>();

	public NetworkGenerator(MyPhyloTree n, Vector<String> taxaOrdering, ReplacementInfo rI, int numOfInputTrees,
							CheckConstraints checker, ReattachClustersPara manager, HybridManager hM,
							MyNetPriorThreadPool myThreadPool, MyPhyloTree[] trees) {
		this.n = n;
		this.taxaOrdering = taxaOrdering;
		this.rI = rI;
		this.checker = checker;
		this.manager = manager;
		this.hM = hM;
		this.myThreadPool = myThreadPool;
		this.numOfInputTrees = numOfInputTrees;

		this.trees = trees;
		for (MyPhyloTree t : trees) {
			lcaQueries.add(new LCA_Query(t));
			HashMap<BitSet, MyNode> setToLeaf = new HashMap<BitSet, MyNode>();
			for (MyNode l : t.getLeaves())
				setToLeaf.put(getLeafSet(l), l);
			setToLeaves.add(setToLeaf);
		}

	}

	public void run() {

		manager.reportStarting(this);

		try {

			Iterator<MyNode> it = n.getLeaves().iterator();
			MyNode leaf = null;
			while (it.hasNext()) {
				MyNode l = it.next();
				if (rI.getReplacementLabels().contains(l.getLabel()))
					leaf = l;
			}

			Vector<MyPhyloTree> recNetworks = new Vector<MyPhyloTree>();
			if (leaf != null) {

				BitSet b = getLeafSet(leaf);
				String label = leaf.getLabel();

				// getting all networks representing a minimal common
				// cluster
				Vector<HybridNetwork> clusterNetworks = rI.getLabelToNetworks().get(label);

				// every so far computed network gets attached by all
				// different networks representing the minimal cluster
				// producing a bigger set of networks

				for (HybridNetwork cN : clusterNetworks) {

					if (stop)
						break;

					MyPhyloTree newN = new MyPhyloTree(n);

					myThreadPool.submit(new ReattachNetwork(newN, cN, getClusterToNode(newN, b), rI
							.isClusterNetwork(cN), manager, rI, hM, checker, taxaOrdering, numOfInputTrees,
							myThreadPool, trees));

				}

			} else if (!stop) {

				// re-attaching all common subtrees...
				// System.out.println("ReattachSubtrees");
				ReattachSubtrees rS = new ReattachSubtrees();
				rS.run(n, rI, numOfInputTrees);
				rS = null;

				removeOutgroup(n);
				if (!containsRedundantNodeUp(n) && !containsRedundantNodeDown(n)) {
					clearLabelings(n);
					manager.reportNetwork(n);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
			manager.setStop(true);
		}

		manager.reportFinishing(this);

	}

	private MyNode getClusterToNode(MyPhyloTree n, BitSet b1) {
		for (MyNode v : n.getLeaves()) {
			BitSet b2 = getLeafSet(v);
			if (b2.equals(b1))
				return v;
		}
		return null;
	}

	private BitSet getLeafSet(MyNode leaf) {
		BitSet b = new BitSet(taxaOrdering.size());

		String mappedLeaf = leaf.getLabel();
		if (rI.getLabelToNumber().containsKey(mappedLeaf))
			mappedLeaf = rI.getLabelToNumber().get(leaf.getLabel());

		if (taxaOrdering.contains(mappedLeaf))
			b.set(taxaOrdering.indexOf(mappedLeaf));

		return b;
	}

	public void clearLabelings(MyPhyloTree n) {
		for (MyNode v : n.getNodes()) {
			if (v.getOutDegree() != 0)
				v.setLabel(null);
		}
	}

	public void removeOutgroup(MyPhyloTree n) {
		for (MyNode v : n.getLeaves()) {
			if (v.getLabel().equals("rho")) {
				MyEdge e = (MyEdge) v.getFirstInEdge();
				MyNode p = e.getSource();
				n.deleteEdge(e);
				n.deleteNode(v);
				e = (MyEdge) p.getFirstOutEdge();
				MyNode c = e.getTarget();
				n.deleteEdge(e);
				n.deleteNode(p);
				n.setRoot(c);
				break;
			}
		}
	}

	private boolean containsRedundantNodeUp(MyPhyloTree n) {
		for (MyNode v : n.getNodes()) {
			Vector<MyEdge> retEdges = getRetEdges(v);
			Vector<MyEdge> treeEdges = getTreeEdges(v);
			if (v.getInDegree() == 1 && !retEdges.isEmpty() && treeEdges.size() == 1) {

				boolean isAllRedundant = true;
				for (MyEdge eRet : retEdges) {
					MyNode p = v.getFirstInEdge().getSource();

					boolean isRedundant = false;
					if (p != null && p.getInDegree() < 2 && isRedundant(eRet, p, true))
						isRedundant = true;

					if (!isRedundant) {
						isAllRedundant = false;
						break;
					}

				}
				if (isAllRedundant)
					return true;

			}
		}
		return false;
	}

	private boolean containsRedundantNodeDown(MyPhyloTree n) {
		for (MyNode v : n.getNodes()) {
			Vector<MyEdge> retEdges = getRetEdges(v);
			Vector<MyEdge> treeEdges = getTreeEdges(v);
			if (!retEdges.isEmpty() && treeEdges.size() == 1) {

				MyEdge eTree = treeEdges.firstElement();

				boolean isAllRedundant = true;
				for (MyEdge eRet : retEdges) {
					MyNode c = eTree.getTarget();

					boolean isRedundant = false;
					if (isRedundant(eRet, c, false))
						isRedundant = true;

					if (!isRedundant) {
						isAllRedundant = false;
						break;
					}

				}
				if (isAllRedundant)
					return true;

			}
		}
		return false;
	}

	private Vector<MyEdge> getRetEdges(MyNode v) {
		Vector<MyEdge> retEdges = new Vector<MyEdge>();
		Iterator<MyEdge> it = v.outEdges().iterator();
		while (it.hasNext()) {
			MyEdge e = it.next();
			if (e.getTarget().getInDegree() > 1)
				retEdges.add(e);
		}
		return retEdges;
	}

	private Vector<MyEdge> getTreeEdges(MyNode v) {
		Vector<MyEdge> treeEdges = new Vector<MyEdge>();
		Iterator<MyEdge> it = v.outEdges().iterator();
		while (it.hasNext()) {
			MyEdge e = it.next();
			if (e.getTarget().getInDegree() == 1)
				treeEdges.add(e);
		}
		return treeEdges;
	}

	private boolean isRedundant(MyEdge eRet, MyNode v, boolean upwards) {
		HashSet<Integer> eIndices = (HashSet<Integer>) eRet.getInfo();
		for (int eIndex : eIndices) {

			HashSet<MyNode> leafSet1 = new HashSet<MyNode>();
			getTreeChildren(eRet.getSource(), eIndex, leafSet1);
			HashSet<MyNode> leafSet2 = new HashSet<MyNode>();
			getTreeChildren(v, eIndex, leafSet2);

			if (upwards || !leafSet2.isEmpty()) {
				LCA_Query lcaQuery = lcaQueries.get(eIndex);
				MyNode lca1 = lcaQuery.cmpLCA(leafSet1);
				MyNode lca2 = lcaQuery.cmpLCA(leafSet2);
				if (!lca1.equals(lca2))
					return false;
			}

		}
		return true;
	}

	private void getTreeChildren(MyNode v, int eIndex, HashSet<MyNode> leafSet) {
		if (v.getOutDegree() == 0)
			leafSet.add(setToLeaves.get(eIndex).get(getLeafSet(v)));
		else {
			Iterator<MyEdge> it = v.outEdges().iterator();
			while (it.hasNext()) {
				MyEdge e = it.next();
				if (e.getTarget().getInDegree() == 1)
					getTreeChildren(e.getTarget(), eIndex, leafSet);
				else {
					HashSet<Integer> eIndices = (HashSet<Integer>) e.getInfo();
					if (eIndices.contains(eIndex))
						getTreeChildren(e.getTarget(), eIndex, leafSet);
				}
			}
		}
	}

}