/*
 *   LevelKNetwork.java Copyright (C) 2023 Daniel H. Huson
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

/*
 * LevelKNetwork.java Copyright (C) 2022
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dendroscope.algorithms.levelknet;

import dendroscope.algorithms.clusternet.ClusterNetwork;
import dendroscope.algorithms.levelknet.cass.CassAlgorithm;
import dendroscope.algorithms.utils.Compact;
import dendroscope.algorithms.utils.HasseDiagram;
import dendroscope.consensus.Cluster;
import dendroscope.consensus.Split;
import dendroscope.consensus.SplitSystem;
import dendroscope.consensus.Taxa;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import jloda.swing.util.Alert;
import jloda.swing.util.ProgressDialog;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.StringUtils;
import jloda.util.progress.ProgressListener;

import java.util.*;

/**
 * compute a level-k network from a set of clusters
 * Daniel Huson, 4.2009. 1.2012
 */
public class LevelKNetwork {
	private final Taxa taxa;
	private final SplitSystem splits;

	private boolean computeOnlyOne = false;
	private boolean checkTrees = false;

	private final boolean DEBUG = false; // when this is true, report lots of stuff

	/**
	 * constructor
	 *
	 */
	public LevelKNetwork(Taxa taxa, SplitSystem splits) {
		this.taxa = taxa;
		this.splits = splits;
	}

	/**
	 * apply the optimal galled network algorithm. Treats the last named taxon as an outgroup
	 *
	 * @return optimal network from splits
	 */
	public List<PhyloTree> apply(ProgressListener progressListener) {

		long startTime = new Date().getTime();

		progressListener.setCancelable(false);
		progressListener.setTasks("Computing minimal network", "initalization");
		progressListener.setMaximum(-1);

		if (progressListener instanceof ProgressDialog) {
			((ProgressDialog) progressListener).show();
			((ProgressDialog) progressListener).setCloseOnCancel(false);
		}

		System.err.println("Constructing optimal level-k network (taxa=" + taxa.size() + ", splits=" + splits.size() + "):");

		if (!splits.isFullSplitSystem(taxa)) {
			System.err.println("Warning: not full split system");
			return new LinkedList<>();
		}

		if (DEBUG) {
			System.err.println("Taxa:");
			for (int t = 1; t <= taxa.size(); t++) {
				System.err.println("t=" + t + ": " + taxa.getLabel(t));
			}
		}
		//   determine the clusters
		List<Cluster> list = new LinkedList<>();
		int outGroupTaxonId = taxa.maxId();
		for (int i = 1; i <= splits.size(); i++) {
			Split split = splits.getSplit(i);
			BitSet A = split.getPartNotContainingTaxon(outGroupTaxonId);
			if (A.cardinality() < taxa.size() - 1)
				list.add(new Cluster(A, split.getWeight(), split.getConfidence(), i, split.getTreeNumbers()));
		}
		Cluster[] clusters = list.toArray(new Cluster[0]);

		System.err.println("Clusters: " + clusters.length);

		// compute incompatibility graph:
		boolean[][] incompatible = new boolean[clusters.length][clusters.length];
		for (int i = 0; i < clusters.length; i++) {
			for (int j = i + 1; j < clusters.length; j++) {
				incompatible[i][j] = incompatible[j][i] = Cluster.incompatible(clusters[i], clusters[j]);
			}
		}

		BitSet[] components = computeComponents(incompatible);
		BitSet[] component2taxa = new BitSet[components.length];
		Cluster[][] component2clusters = new Cluster[components.length][];
		Cluster[] component2representative = new Cluster[components.length];

		System.err.println("Components: " + components.length);

		int numberOfNonTrivialComponents = 0;
		for (int n = 0; n < components.length; n++) {
			component2taxa[n] = new BitSet();
			component2clusters[n] = new Cluster[components[n].cardinality()];
			int count = 0;
			for (int c = components[n].nextSetBit(0); c != -1; c = components[n].nextSetBit(c + 1)) {
				component2taxa[n].or(clusters[c]);
				component2clusters[n][count++] = clusters[c];
			}
			if (components[n].cardinality() > 1) {
				numberOfNonTrivialComponents++;
				component2representative[n] = new Cluster(component2taxa[n], -1);
			} else
				component2representative[n] = component2clusters[n][0];
		}
		System.err.println("Non-trivial components=" + numberOfNonTrivialComponents);

		PhyloTree originalBackboneTree = HasseDiagram.constructHasse(component2representative);

		// compute map of clusters to nodes in backbone tree
		Map<BitSet, Node> taxa2node = new HashMap<>();
		for (Node v = originalBackboneTree.getFirstNode(); v != null; v = originalBackboneTree.getNextNode(v)) {
			Cluster cluster = (Cluster) originalBackboneTree.getInfo(v);
			if (cluster != null && cluster.cardinality() > 0)
				taxa2node.put(cluster, v);
		}

		// set the weights of the backbone tree:

		for (int n = 0; n < component2taxa.length; n++) {
			BitSet taxa = component2taxa[n];
			if (taxa != null && taxa.cardinality() > 0) {
				Node v = taxa2node.get(taxa);
				if (v != null) {
					if (component2clusters[n].length == 1) {
						Cluster cluster = component2clusters[n][0];
						Edge e = v.getFirstInEdge();
						originalBackboneTree.setWeight(e, cluster.getWeight());
					} else
						originalBackboneTree.setLabel(v, originalBackboneTree.getLabel(v) + "<" + component2clusters[n].length + ">");
				}
			}
		}

		// process all non-trivial components
		progressListener.setMaximum(5L * (component2clusters.length + 1));
		progressListener.setCancelable(false);
		if (progressListener instanceof ProgressDialog) {
			((ProgressDialog) progressListener).setCancelButtonText("Skip");
		}

		int nontrivialComponentNumber = 0;
		Map<Integer, List<PhyloTree>> component2networks = new HashMap<>();

		for (int n = 0; n < component2clusters.length; n++) {
			if (component2clusters[n].length > 1) {
				nontrivialComponentNumber++;
				try {
					progressListener.setProgress(5L * n);
					progressListener.setTasks("Minimal network", "Processing component " + nontrivialComponentNumber);
					List<PhyloTree> result = processComponent(component2taxa[n], component2clusters[n], progressListener, nontrivialComponentNumber);
					component2networks.put(n, result);
				} catch (Exception e) {
					Basic.caught(e);
					new Alert(null, "Cass algorithm failed: " + e.getMessage());
				}
			}
		}

		List<PhyloTree> networks = new LinkedList<>();
		networks.add(originalBackboneTree);
		for (Integer n : component2networks.keySet()) {
			BitSet taxaX = component2taxa[n];
			Cluster[] clustersX = component2clusters[n];
			List<PhyloTree> networksX = component2networks.get(n);
			networks = plantComponents(taxaX, clustersX, networksX, networks);
		}


		for (PhyloTree network : networks) {
			var nodeWeight = network.newNodeDoubleArray();
			for (var v : network.nodes()) {
				nodeWeight.put(v, 1.0);
			}
			ClusterNetwork.convertHasseToClusterNetwork(network, nodeWeight);
			// convert taxon ids back to taxon labels:
			List<Node> toDelete = new LinkedList<>();
			for (Node v = network.getFirstNode(); v != null; v = network.getNextNode(v)) {
				network.setLabel(v, null);
				if (v.getOutDegree() == 0) {
					BitSet set = (BitSet) v.getInfo();
					if (set != null && set.cardinality() > 0) {
						String label = "";
						for (int t = set.nextSetBit(0); t != -1; t = set.nextSetBit(t + 1)) {
							network.addTaxon(v, t);
							if (label.length() > 0)
								label += ",";
							label += taxa.getLabel(t);
						}
						network.setLabel(v, label);
					} else
						toDelete.add(v);
				}
			}
			for (Node v : toDelete) {
				network.deleteNode(v);
			}

			for (Edge e = network.getFirstEdge(); e != null; e = e.getNext()) {
				if (e.getTarget().getInDegree() > 1) {
					network.setWeight(e, 0);
					network.setReticulate(e, true);
				}
			}
			System.err.println("Network: " + network + ";");
		}

		long seconds = (new Date().getTime() - startTime);
		System.err.println("Algorithm required " + seconds / 1000.0 + " seconds");
		return networks;
	}


	/**
	 * determines incompatibility components
	 *
	 * @return all incompatibity components
	 */
	public static BitSet[] computeComponents(boolean[][] incompatible) {
		int[] componentNumber = new int[incompatible.length];
		Arrays.fill(componentNumber, -1);

		int number = 0;
		for (int i = 0; i < incompatible.length; i++) {
			if (componentNumber[i] == -1)
				computeComponentsRec(incompatible, i, number++, componentNumber);
		}

		BitSet[] components = new BitSet[number];
		for (int i = 0; i < components.length; i++)
			components[i] = new BitSet();

		for (int c = 0; c < incompatible.length; c++) {
			components[componentNumber[c]].set(c);
		}
		return components;
	}

	/**
	 * recursively does the work
	 *
	 */
	private static void computeComponentsRec(boolean[][] incompatible, int i, int number, int[] componentNumber) {
		componentNumber[i] = number;
		for (int j = 0; j < incompatible.length; j++) {
			if (i != j && incompatible[i][j] && componentNumber[j] == -1) {
				computeComponentsRec(incompatible, j, number, componentNumber);
			}
		}
	}


	/**
	 * process an individual component in the incompatibility graph
	 *
	 */
	private List<PhyloTree> processComponent(BitSet taxa, Cluster[] clusters, ProgressListener progressListener, int componentNumber) throws Exception {
		System.err.println("Processing incompatibility component on " + taxa.cardinality() + " taxa and " + clusters.length + "  clusters");

		// try to compute level-k solution for component:
		List<PhyloTree> componentNetworks = new LinkedList<>();
		try {
			progressListener.setCancelable(true);

			System.err.println("Running new implementation of Cass algorithm:");
			CassAlgorithm cassAlgorithm = new CassAlgorithm();
			int level = cassAlgorithm.apply(clusters, componentNetworks, isComputeOnlyOne(), isCheckTrees(), progressListener);
			System.err.println("done (level=" + level + ")");

		} catch (CanceledException ex) {
			System.err.println("User canceled calculation of minimal network on compoonent " + componentNumber + ", will construct cluster network");
			progressListener.setCancelable(false);
			progressListener.setUserCancelled(false);
			if (progressListener instanceof ProgressDialog) {
				((ProgressDialog) progressListener).show();
			}
		}

		if (componentNetworks.size() == 0) // solve component using level-k network failed, construct cluster network
		{
			Set<Cluster> set = new HashSet<>(Arrays.asList(clusters));
			// add all trivial clusters:
			BitSet compactTaxa = Cluster.extractTaxa(clusters);

			for (int t = compactTaxa.nextSetBit(0); t != -1; t = compactTaxa.nextSetBit(t + 1)) {
				Cluster cluster = new Cluster();
				cluster.set(t);
				set.add(cluster);
			}
			componentNetworks.add(HasseDiagram.constructHasse(set.toArray(new Cluster[0])));
		}
		return componentNetworks;
	}

	/**
	 * plant the networks for components into all backbone networks
	 *
	 * @return all new backbone networks
	 */
	private List<PhyloTree> plantComponents(BitSet taxa, Cluster[] clusters, List<PhyloTree> componentNetworks, List<PhyloTree> backboneNetworks) {
		List<PhyloTree> result = new LinkedList<>();

		Map mapBack = Compact.compactClusters(clusters);


		for (PhyloTree backboneNetwork0 : backboneNetworks) {
			for (PhyloTree componentTree : componentNetworks) {
				PhyloTree backboneNetwork = (PhyloTree) backboneNetwork0.clone();

				Node v = getNodeInBackboneNetwork(backboneNetwork, taxa);

				// determine all children below v
				Node[] bottom = new Node[v.getOutDegree()];
				int count = 0;
				List<Edge> toDelete = new LinkedList<>();
				for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
					bottom[count++] = e.getTarget();
					toDelete.add(e);
				}

				for (Edge e : toDelete) {
					backboneNetwork.deleteEdge(e);
				}


				for (Node u = componentTree.getFirstNode(); u != null; u = componentTree.getNextNode(u)) {
					Cluster cluster = (Cluster) u.getInfo();
					if (cluster != null)
						Compact.uncompactCluster(cluster, mapBack, Integer.MAX_VALUE);
				}

				// copy component into graph:
				NodeArray<Node> src2target = new NodeArray<>(componentTree);
				for (Node src = componentTree.getFirstNode(); src != null; src = componentTree.getNextNode(src)) {
					if (src == componentTree.getRoot())
						src2target.put(src, v);
					else {
						Node tar = backboneNetwork.newNode();
						src2target.put(src, tar);
						tar.setInfo(src.getInfo());
						backboneNetwork.setLabel(tar, componentTree.getLabel(src));
					}
				}
				for (Node src = componentTree.getFirstNode(); src != null; src = componentTree.getNextNode(src)) {
					for (Edge e = src.getFirstOutEdge(); e != null; e = src.getNextOutEdge(e)) {
						Edge f = backboneNetwork.newEdge(src2target.get(e.getSource()), src2target.get(e.getTarget()));
						backboneNetwork.setWeight(f, componentTree.getWeight(e));
						backboneNetwork.setLabel(f, componentTree.getLabel(e));
					}
				}
				// find all leaves of the solution, these will give the top nodes
				Node[] top = getAllLeavesBelow(v);

				// compute a mapping from each taxon to the set of top nodes that contain them
				BitSet seen = new BitSet();
				Map<Integer, Set<Node>> taxon2top = new HashMap<>();
				for (Node u : top) {
					BitSet topSet = (BitSet) backboneNetwork.getInfo(u);
					if (topSet == null)
						throw new RuntimeException("Unlabeled leaf encountered: " + u);
					for (int t = topSet.nextSetBit(0); t != -1; t = topSet.nextSetBit(t + 1)) {
						seen.set(t);
						Set<Node> topNodes = taxon2top.computeIfAbsent(t, k -> new HashSet<>());
						topNodes.add(u);
					}
				}
				if (!seen.equals(taxa))
					throw new RuntimeException("Leaves of component do not mention all taxa, missing: " + Cluster.setminus(taxa, seen));

				// attach all bottom children to top leaves:
				for (Node bn : bottom) {
					BitSet bottomSet = (BitSet) backboneNetwork.getInfo(bn);
					for (int t = bottomSet.nextSetBit(0); t != -1; t = bottomSet.nextSetBit(t + 1)) {
						Set<Node> topNodes = taxon2top.get(t);
						for (Node tn : topNodes) {
							if (tn.getEdgeTo(bn) == null) {
								backboneNetwork.newEdge(tn, bn);
							}
						}
					}
				}

				// reduce all top nodes that have indegree=outdegree=1
				for (Node u : top) {
					if (u.getInDegree() == 1 && u.getOutDegree() == 1) {
						Edge e = u.getFirstOutEdge();
						Edge f = backboneNetwork.newEdge(u.getFirstInEdge().getSource(), u.getFirstOutEdge().getTarget());
						backboneNetwork.setWeight(f, backboneNetwork.getWeight(e));
						backboneNetwork.deleteNode(u);
					}
				}
				result.add(backboneNetwork);
			}
		}
		return result;
	}

	/**
	 * get the node that corresponds to the givent taxon set in the backbone network
	 *
	 */
	private Node getNodeInBackboneNetwork(PhyloTree backboneNetwork, BitSet taxa) {
		for (Node v = backboneNetwork.getFirstNode(); v != null; v = v.getNext()) {
			Cluster cluster = (Cluster) backboneNetwork.getInfo(v);
			if (cluster != null && cluster.equals(taxa)) {
				return v;
			}
		}
		throw new RuntimeException("Taxa not found in backbone network: " + StringUtils.toString(taxa));
	}

	/**
	 * gets all leaves below the given node
	 *
	 * @return all leaves below v
	 */
	private Node[] getAllLeavesBelow(Node v) {
		List<Node> list = new LinkedList<>();

		Stack<Node> stack = new Stack<>();
		stack.push(v);
		while (stack.size() > 0) {
			v = stack.pop();
			if (v.getOutDegree() == 0)
				list.add(v);
			else for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
				Node w = e.getTarget();
				if (e == w.getFirstInEdge()) // to ensure that we consider each node only once
					stack.add(w);
			}
		}
		return list.toArray(new Node[0]);
	}

	public boolean isComputeOnlyOne() {
		return computeOnlyOne;
	}

	public void setComputeOnlyOne(boolean computeOnlyOne) {
		this.computeOnlyOne = computeOnlyOne;
	}

	public void setCheckTrees(boolean ct) {
		this.checkTrees = ct;
	}

	public boolean isCheckTrees() {
		return checkTrees;
	}
}
