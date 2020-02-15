/*
 *   ReattachClustersRec.java Copyright (C) 2020 Daniel H. Huson
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
import dendroscope.hybroscale.model.cmpMinNetworks.NetworkIsomorphismCheck;
import dendroscope.hybroscale.model.reductionSteps.ReplacementInfo;
import dendroscope.hybroscale.model.treeObjects.HybridNetwork;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.model.util.CheckConstraints;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;
import dendroscope.hybroscale.view.HybridView;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This method replaces distinct leaves of a resolved network by other resolved
 * networks producing a set of new resolved networks.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class ReattachClustersRec {

	private int numOfNets, numOfShiftNets;
	private Vector<MyPhyloTree> networks = new Vector<MyPhyloTree>();
	private boolean stop = false;
	private int numOfInputTrees;
	private Vector<HybridView> views;
	private ReplacementInfo rI;
	private HybridManager hM;
	private CheckConstraints checker;
	private Vector<String> taxaOrdering;
	private HybridNetwork n;

	public ReattachClustersRec(HybridNetwork n, ReplacementInfo rI, int numOfNets, int numOfShiftNets,
							   int numOfInputTrees, Vector<HybridView> views, int cores, Vector<String> taxaOrdering, String constraints,
							   HybridManager hM) {

		this.n = n;
		this.numOfNets = numOfNets;
		this.numOfShiftNets = numOfShiftNets;
		this.numOfInputTrees = numOfInputTrees;
		this.views = views;
		this.rI = rI;
		this.checker = new CheckConstraints(constraints, taxaOrdering, rI);
		this.hM = hM;
		this.taxaOrdering = taxaOrdering;
	}

	public Vector<MyPhyloTree> run() {

		// generating several networks by adding each combination of all
		// networks representing a minimal common cluster replaced before
		// REMARK: a taxon can replace more than one network since one MAAF
		// represents several networks and there can be more than one MAAF
		generateNetworksRec(n, taxaOrdering);

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

		return networks;
	}

	private void generateNetworksRec(HybridNetwork n, Vector<String> taxaOrdering) {

		Iterator<MyNode> it = n.getLeaves().iterator();
		boolean hasReplacmentLabel = false;
		while (it.hasNext()) {

			if (stop)
				break;

			MyNode leaf = it.next();
			BitSet b = n.getNodeToCluster().get(leaf);
			String label = n.getLabel(leaf);

			// checking if leaf replaces a minimal common clusters
			if (rI.getReplacementLabels().contains(label)) {

				hasReplacmentLabel = true;

				// getting all networks representing a minimal common
				// cluster
				Vector<HybridNetwork> clusterNetworks = rI.getLabelToNetworks().get(label);

				// every so far computed network gets attached by all
				// different networks representing the minimal cluster
				// producing a bigger set of networks
				for (HybridNetwork cN : clusterNetworks) {

					if (stop)
						break;

					HybridNetwork newN = new HybridNetwork(n, false, taxaOrdering);
					newN.update();

					// attach network representing the minimal cluster
					Vector<HybridNetwork> newNetworks = reattachNetwork(newN, cN, newN.getClusterToNode().get(b),
							rI.isClusterNetwork(cN));

					for (HybridNetwork newNetwork : newNetworks)
						generateNetworksRec(newNetwork, taxaOrdering);

					newNetworks = null;

					// generateNetworksRec(newN, rI, tM, taxaOrdering);

				}

				break;
			}
		}

		// all cluster replaced
		if (!hasReplacmentLabel && !stop) {

			// re-attaching all common subtrees...
			// System.out.println("ReattachSubtrees");
			(new ReattachSubtrees()).run(n, rI, numOfInputTrees);

			taxaOrdering = rI.addLeafLabels(n, taxaOrdering);
			n.setTaxaOrdering(taxaOrdering);
			n.removeOutgroup();
			n.clearLabelings();

			networks.add(n);
			// countDownLatch.countDown();

			int progress = (int) Math.round(((double) (networks.size() - numOfShiftNets) / (numOfNets + 1.)) * 100.);
			for (HybridView view : views)
				view.setInfo(progress + "% of all hybridization networks computed");

		}
	}

	private Vector<HybridNetwork> reattachNetwork(HybridNetwork n, HybridNetwork toCopy, MyNode leaf,
												  boolean isClusterNetwork) {

		// attach root of the attached network
		MyNode vCopy = n.newNode(toCopy.getRoot());
		n.setLabel(vCopy, toCopy.getLabel(toCopy.getRoot()));

		// attaching the generated root
		Iterator<MyEdge> it = leaf.inEdges().iterator();
		while (it.hasNext()) {
			MyEdge e = it.next();
			boolean isSpecial = n.isSpecial(e);
			MyNode parent = e.getSource();
			MyEdge eCopy = n.newEdge(parent, vCopy);

			if (isSpecial) {
				n.setSpecial(eCopy, true);
				n.setWeight(eCopy, 0);
			}

			eCopy.setInfo(e.getInfo());

		}

		// adding attached network
		addNetworkToNetworkRec(vCopy, toCopy.getRoot(), toCopy, n, new ConcurrentHashMap<MyNode, MyNode>(),
				isClusterNetwork, null, null);

		// delete leaf (leaf is now replaced by a resolved network)
		n.deleteNode(leaf);

		if (!rI.containsClusterNetwork()) {

			// System.out.println("****************\n" + n.toMyBracketString() +
			// "\n");
			// System.out.println(n.toMyBracketString());

			// shifting edges down between clusters
			HashMap<HybridNetwork, MyNode> shiftedDownNetworks = new HashMap<HybridNetwork, MyNode>();
			HashMap<HybridNetwork, HashMap<MyEdge, MyNode>> networkToShiftedEdges = new HashMap<HybridNetwork, HashMap<MyEdge, MyNode>>();

			shiftEdgesDown(n, vCopy, new HashSet<MyEdge>(), new HashMap<MyEdge, MyNode>(), shiftedDownNetworks,
					isClusterNetwork, networkToShiftedEdges);

			// System.out.println("+++++++++++");
			// for (HybridNetwork net : shiftedDownNetworks.keySet())
			// System.out.println(net.toMyBracketString());
			// System.out.println("+++++++++++");

			// shifting edges upwards between clusters
			HashMap<HybridNetwork, MyNode> shiftedUpNetworks = new HashMap<HybridNetwork, MyNode>();
			for (HybridNetwork net : shiftedDownNetworks.keySet())
				shiftEdgesUp(net, shiftedDownNetworks.get(net), new HashSet<MyEdge>(), networkToShiftedEdges.get(net),
						shiftedUpNetworks, isClusterNetwork);

			networkToShiftedEdges = null;
			shiftedDownNetworks = null;

			Vector<HybridNetwork> shiftedNetworks = new Vector<HybridNetwork>();
			shiftedNetworks.addAll(shiftedUpNetworks.keySet());

			shiftedUpNetworks = null;

			// System.out.println("-----------");
			// for (HybridNetwork net : shiftedUpNetworks.keySet())
			// System.out.println(net.toMyBracketString());
			// System.out.println("-----------");

			// System.out.println("\n" + n.toMyBracketString());
			// for (HybridNetwork net : shiftedUpNetworks.keySet())
			// System.out.println(net.toMyBracketString());
			// System.out.println();

			Vector<HybridNetwork> multipleNetworks = new Vector<HybridNetwork>();
			for (int i = 0; i < shiftedNetworks.size() - 1; i++) {
				HybridNetwork net1 = shiftedNetworks.get(i);
				SparseNetwork n1 = new SparseNetwork(net1);
				for (int j = i + 1; j < shiftedNetworks.size(); j++) {
					HybridNetwork net2 = shiftedNetworks.get(j);
					SparseNetwork n2 = new SparseNetwork(net2);
					if (!net1.equals(net2) && new NetworkIsomorphismCheck().run(n1, n2)) {
						multipleNetworks.add(net1);
						break;
					}
				}
			}

			for (HybridNetwork net : multipleNetworks)
				shiftedNetworks.remove(net);

			multipleNetworks = null;

			numOfShiftNets += shiftedNetworks.size() - 1;

			if (checker.doCheckAddTaxa() || checker.doCheckTime()) {
				boolean hasReplacementChar = false;
				for (MyNode v : shiftedNetworks.firstElement().getLeaves()) {
					if (rI.getReplacementLabels().contains(v.getLabel())) {
						hasReplacementChar = true;
						break;
					}
				}
				if (!hasReplacementChar) {
					for (HybridNetwork net : shiftedNetworks) {
						if (checker.doCheckAddTaxa()) {
							int addTaxaDegree = checker.estimateAddTaxaDegree(new SparseNetwork(net),
									hM.getAddTaxaValue(), false, -1);
							net.setAddTaxaDegree(addTaxaDegree);
							hM.updateAddTaxaValue(addTaxaDegree);
						}
						if (checker.doCheckTime()) {
							int timeConsDegree = checker.estimateTimeConsistencyDegree(new SparseNetwork(net),
									hM.getTimeConsValue(), false, -1);
							net.setTimeDegree(timeConsDegree);
							hM.updateTimeConsValue(timeConsDegree);
						}
					}
				}
			}

			return shiftedNetworks;

		} else {

			Vector<HybridNetwork> shiftedNetworks = new Vector<HybridNetwork>();
			n.update();
			shiftedNetworks.add(n);

			return shiftedNetworks;

		}

	}

	private void shiftEdgesDown(HybridNetwork n, MyNode v, HashSet<MyEdge> visitedEdges,
								HashMap<MyEdge, MyNode> shiftedEdges, HashMap<HybridNetwork, MyNode> shiftedDownNetworks,
								boolean isClusterNetwork, HashMap<HybridNetwork, HashMap<MyEdge, MyNode>> networkToShiftedEdges) {

		// System.out.println(n.toMyBracketString() + "\n" + v.getLabel());
		// for (MyEdge e : visitedEdges)
		// System.out.println(e.getSource().getLabel() + "-V->" +
		// e.getTarget().getLabel() + " "
		// + e.getOwner().equals(n));

		MyNode p = v.getFirstInEdge().getSource();
		Iterator<MyEdge> it = p.outEdges().iterator();
		HashSet<MyEdge> shiftingEdges = new HashSet<MyEdge>();
		HashSet<MyNode> sourceNodes = new HashSet<MyNode>();
		while (it.hasNext()) {
			MyEdge e = it.next();
			if (e.getOwner().equals(n) && n.isSpecial(e) && !visitedEdges.contains(e)) {
				HashSet<Integer> treeIndices = getEdgeIndices(e);
				MyNode s = v;

				while (true) {

					if (getTreeIndexOutDegree(s, treeIndices, shiftedEdges, p) <= 1) {
						sourceNodes.add(s);
						Iterator<MyEdge> itOut = s.outEdges().iterator();
						while (itOut.hasNext()) {
							MyEdge eOut = itOut.next();
							if (getEdgeIndices(eOut).containsAll(treeIndices) && !shiftedEdges.containsKey(eOut))
								s = eOut.getTarget();
						}
					} else if (s.getInDegree() == 1 && s.getOutDegree() > 2 && !s.equals(v)) {
						Iterator<MyEdge> itOut = s.outEdges().iterator();
						while (itOut.hasNext()) {
							MyEdge eOut = itOut.next();
							boolean addNode = true;
							for (int index : getEdgeIndices(eOut)) {
								if (treeIndices.contains(index))
									addNode = false;
							}
							if (addNode)
								sourceNodes.add(s);
						}
						break;
					} else
						break;
				}
				if (!sourceNodes.isEmpty()) {
					shiftingEdges.add(e);
					break;
				}
			}
		}

		if (shiftingEdges.isEmpty()) {
			n.update();
			shiftedDownNetworks.put(n, v);
			networkToShiftedEdges.put(n, shiftedEdges);
		} else {
			HashSet<MyEdge> newVisitedEdges = (HashSet<MyEdge>) visitedEdges.clone();
			newVisitedEdges.add(shiftingEdges.iterator().next());
			shiftEdgesDown(n, v, newVisitedEdges, (HashMap<MyEdge, MyNode>) shiftedEdges.clone(), shiftedDownNetworks,
					isClusterNetwork, networkToShiftedEdges);
		}

		for (MyEdge e : shiftingEdges) {

			for (MyNode x : sourceNodes) {

				Vector<MyEdge[]> edgePairs = new Vector<MyEdge[]>();
				MyEdge[] edgePair = { e, null };
				edgePairs.add(edgePair);
				for (MyEdge eV : visitedEdges) {
					MyEdge[] edgePair2 = { eV, null };
					edgePairs.add(edgePair2);
				}
				for (MyEdge eS : shiftedEdges.keySet()) {
					MyEdge[] edgePair2 = { eS, null };
					edgePairs.add(edgePair2);
				}

				Vector<MyNode[]> nodePairs = new Vector<MyNode[]>();
				MyNode[] nodePair = { x, null };
				nodePairs.add(nodePair);
				MyNode[] nodePair2 = { p, null };
				nodePairs.add(nodePair2);
				MyNode[] nodePair3 = { v, null };
				nodePairs.add(nodePair3);
				for (MyNode vS : shiftedEdges.values()) {
					MyNode[] nodePair4 = { vS, null };
					nodePairs.add(nodePair4);
				}

				HybridNetwork nCopy = new HybridNetwork(new MyPhyloTree(), false, n.getTaxaOrdering());
				addNetworkToNetworkRec(nCopy.getRoot(), n.getRoot(), n, nCopy, new ConcurrentHashMap<MyNode, MyNode>(),
						isClusterNetwork, edgePairs, nodePairs);

				MyEdge eCopy = edgePairs.get(0)[1];
				HashSet<MyEdge> visitedEdgesCopy = new HashSet<MyEdge>();
				for (int i = 1; i < visitedEdges.size() + 1; i++)
					visitedEdgesCopy.add(edgePairs.get(i)[1]);
				HashMap<MyEdge, MyNode> shiftedEdgesCopy = new HashMap<MyEdge, MyNode>();
				int counter = 3;
				for (int i = visitedEdges.size() + 1; i < edgePairs.size(); i++) {
					shiftedEdgesCopy.put(edgePairs.get(i)[1], nodePairs.get(counter)[1]);
					counter++;
				}
				MyNode xCopy = nodePairs.get(0)[1];
				MyNode pCopy = nodePairs.get(1)[1];
				MyNode vCopy = nodePairs.get(2)[1];

				HashMap<HybridNetwork, MyNode> newNetworks = new HashMap<HybridNetwork, MyNode>();
				HashMap<HybridNetwork, HashSet<MyEdge>> newVisitedEdges = new HashMap<HybridNetwork, HashSet<MyEdge>>();
				HashMap<HybridNetwork, HashMap<MyEdge, MyNode>> newShiftedEdges = new HashMap<HybridNetwork, HashMap<MyEdge, MyNode>>();
				HashMap<HybridNetwork, MyEdge> networkToShiftedEdge = new HashMap<HybridNetwork, MyEdge>();
				if (getTreeIndexOutDegree(xCopy, getEdgeIndices(eCopy), shiftedEdgesCopy, pCopy) == 1) {

					MyNode tCopy = eCopy.getTarget();
					HashSet<Integer> eCopyIndices = getEdgeIndices(e);
					// eCopy.getSource().removeOutEdge(eCopy);
					nCopy.deleteEdge(eCopy);
					MyEdge newEdge = nCopy.newEdge(xCopy, tCopy);
					addEdgeIndices(newEdge, (HashSet<Integer>) eCopyIndices.clone());
					nCopy.setSpecial(newEdge, true);
					nCopy.setWeight(newEdge, 0);
					newEdge.setShifted(true);
					shiftedEdgesCopy.put(newEdge, pCopy);
					networkToShiftedEdge.put(nCopy, newEdge);

					if (pCopy.getInDegree() == 1 && pCopy.getOutDegree() == 1) {

						MyNode s = pCopy.getFirstInEdge().getSource();
						MyNode t = pCopy.getFirstOutEdge().getTarget();
						HashSet<Integer> eDeleteIndices = getEdgeIndices(pCopy.getFirstInEdge());
						nCopy.deleteEdge(pCopy.getFirstInEdge());
						nCopy.deleteEdge(pCopy.getFirstOutEdge());
						nCopy.deleteNode(pCopy);
						newEdge = nCopy.newEdge(s, t);
						addEdgeIndices(newEdge, (HashSet<Integer>) eDeleteIndices.clone());
						shiftedDownNetworks.remove(n);

					}

					// System.out.println(n.toMyBracketString() + "\n" +
					// nCopy.toMyBracketString());
					newNetworks.put(nCopy, vCopy);
					newVisitedEdges.put(nCopy, visitedEdgesCopy);
					newShiftedEdges.put(nCopy, shiftedEdgesCopy);

				} else if (getTreeIndexOutDegree(xCopy, getEdgeIndices(eCopy), shiftedEdgesCopy, pCopy) > 1) {

					MyEdge midEdge = xCopy.getFirstInEdge();
					MyNode midSource = midEdge.getSource();
					HashSet<Integer> midEdgeIndices = getEdgeIndices(midEdge);
					nCopy.deleteEdge(midEdge);
					MyNode midNode = new MyNode(nCopy, xCopy.getLabel());
					MyEdge newEdge = nCopy.newEdge(midSource, midNode);
					addEdgeIndices(newEdge, (HashSet<Integer>) midEdgeIndices.clone());
					newEdge = nCopy.newEdge(midNode, xCopy);
					addEdgeIndices(newEdge, (HashSet<Integer>) midEdgeIndices.clone());

					MyNode tCopy = eCopy.getTarget();
					HashSet<Integer> eCopyIndices = getEdgeIndices(e);
					// eCopy.getSource().removeOutEdge(eCopy);
					nCopy.deleteEdge(eCopy);
					newEdge = nCopy.newEdge(midNode, tCopy);
					addEdgeIndices(newEdge, (HashSet<Integer>) eCopyIndices.clone());
					nCopy.setSpecial(newEdge, true);
					nCopy.setWeight(newEdge, 0);
					newEdge.setShifted(true);
					shiftedEdgesCopy.put(newEdge, pCopy);
					networkToShiftedEdge.put(nCopy, newEdge);

					if (pCopy.getInDegree() == 1 && pCopy.getOutDegree() == 1) {

						MyNode s = pCopy.getFirstInEdge().getSource();
						MyNode t = pCopy.getFirstOutEdge().getTarget();
						HashSet<Integer> eDeleteIndices = getEdgeIndices(pCopy.getFirstInEdge());
						nCopy.deleteEdge(pCopy.getFirstInEdge());
						nCopy.deleteEdge(pCopy.getFirstOutEdge());
						nCopy.deleteNode(pCopy);
						newEdge = nCopy.newEdge(s, t);
						addEdgeIndices(newEdge, (HashSet<Integer>) eDeleteIndices.clone());

					}

					Vector<MyEdge> moveEdges = new Vector<MyEdge>();
					it = xCopy.outEdges().iterator();
					while (it.hasNext()) {
						MyEdge eOut = it.next();
						boolean addEdge = true;
						for (int index : getEdgeIndices(eOut)) {
							if (eCopyIndices.contains(index))
								addEdge = false;
						}
						if (addEdge)
							moveEdges.add(eOut);
					}

					shiftEdgesRec(midNode, moveEdges, 0, nCopy, newNetworks, vCopy, newVisitedEdges, visitedEdgesCopy,
							newShiftedEdges, shiftedEdgesCopy, networkToShiftedEdge, isClusterNetwork);

				}

				for (HybridNetwork newNetwork : newNetworks.keySet()) {

					newNetwork.update();
					shiftEdgesDown(newNetwork, newNetworks.get(newNetwork), newVisitedEdges.get(newNetwork),
							newShiftedEdges.get(newNetwork), shiftedDownNetworks, isClusterNetwork,
							networkToShiftedEdges);

					MyEdge shiftedEdge = networkToShiftedEdge.get(newNetwork);
					cmpHangingNodes(shiftedEdge.getSource(), getEdgeIndices(shiftedEdge), shiftedEdge, newNetwork,
							newNetworks.get(newNetwork), newVisitedEdges.get(newNetwork),
							newShiftedEdges.get(newNetwork), isClusterNetwork, true, shiftedDownNetworks,
							networkToShiftedEdges);

				}

			}
		}

	}

	private HashSet<Integer> getEdgeIndices(MyEdge e) {
		return (HashSet<Integer>) e.getInfo();
	}

	private void addEdgeIndices(MyEdge newEdge, HashSet<Integer> indices) {
		newEdge.setInfo(indices);
	}

	private void shiftEdgesRec(MyNode s, Vector<MyEdge> edges, int index, HybridNetwork n,
							   HashMap<HybridNetwork, MyNode> newNetworks, MyNode v,
							   HashMap<HybridNetwork, HashSet<MyEdge>> newVisitedNodes, HashSet<MyEdge> visitedEdges,
							   HashMap<HybridNetwork, HashMap<MyEdge, MyNode>> newShiftedEdges, HashMap<MyEdge, MyNode> shiftedEdges,
							   HashMap<HybridNetwork, MyEdge> networkToShiftedEdge, boolean isClusterNetwork) {

		if (index < edges.size()) {

			for (int k = index; k < edges.size(); k++) {

				MyEdge shiftEdge = edges.get(k);
				Vector<MyEdge[]> edgePairs = new Vector<MyEdge[]>();
				MyEdge[] edgePair = { shiftEdge, null };
				edgePairs.add(edgePair);
				MyEdge[] edgePair1 = { networkToShiftedEdge.get(n), null };
				edgePairs.add(edgePair1);
				for (MyEdge e : edges) {
					MyEdge[] edgePair2 = { e, null };
					edgePairs.add(edgePair2);
				}
				int k2 = edgePairs.size();
				for (MyEdge e : visitedEdges) {
					MyEdge[] edgePair2 = { e, null };
					edgePairs.add(edgePair2);
				}
				for (MyEdge e : shiftedEdges.keySet()) {
					MyEdge[] edgePair2 = { e, null };
					edgePairs.add(edgePair2);
				}

				Vector<MyNode[]> nodePairs = new Vector<MyNode[]>();
				MyNode[] nodePair = { s, null };
				nodePairs.add(nodePair);
				MyNode[] nodePair2 = { v, null };
				nodePairs.add(nodePair2);
				for (MyNode vS : shiftedEdges.values()) {
					MyNode[] nodePair3 = { vS, null };
					nodePairs.add(nodePair3);
				}

				HybridNetwork nCopy = new HybridNetwork(new MyPhyloTree(), false, n.getTaxaOrdering());
				addNetworkToNetworkRec(nCopy.getRoot(), n.getRoot(), n, nCopy, new ConcurrentHashMap<MyNode, MyNode>(),
						isClusterNetwork, edgePairs, nodePairs);

				MyEdge shiftEdgeCopy = edgePairs.get(0)[1];
				networkToShiftedEdge.put(nCopy, edgePairs.get(1)[1]);
				Vector<MyEdge> edgesCopy = new Vector<MyEdge>();
				for (int j = 2; j < k2; j++) {
					if (edgePairs.get(j)[1] != null)
						edgesCopy.add(edgePairs.get(j)[1]);
				}
				HashSet<MyEdge> visitedEdgesCopy = new HashSet<MyEdge>();
				for (int j = k2; j < k2 + visitedEdges.size(); j++) {
					if (edgePairs.get(j)[1] != null)
						visitedEdgesCopy.add(edgePairs.get(j)[1]);
				}
				HashMap<MyEdge, MyNode> shiftedEdgesCopy = new HashMap<MyEdge, MyNode>();
				int counter = 2;
				for (int i = k2 + visitedEdges.size(); i < edgePairs.size(); i++) {
					shiftedEdgesCopy.put(edgePairs.get(i)[1], nodePairs.get(counter)[1]);
					counter++;
				}
				MyNode sCopy = nodePairs.get(0)[1];
				MyNode vCopy = nodePairs.get(1)[1];

				HashSet<Integer> shiftEdgeIndices = getEdgeIndices(shiftEdgeCopy);
				MyNode tCopy = shiftEdgeCopy.getTarget();
				nCopy.deleteEdge(shiftEdgeCopy);
				MyEdge newEdge = nCopy.newEdge(sCopy, tCopy);
				addEdgeIndices(newEdge, (HashSet<Integer>) shiftEdgeIndices.clone());
				nCopy.setSpecial(newEdge, true);
				nCopy.setWeight(newEdge, 0);

				newVisitedNodes.put(nCopy, visitedEdgesCopy);
				newNetworks.put(nCopy, vCopy);
				newShiftedEdges.put(nCopy, shiftedEdgesCopy);
				int newIndex = k + 1;
				shiftEdgesRec(sCopy, edgesCopy, newIndex, nCopy, newNetworks, vCopy, newVisitedNodes, visitedEdgesCopy,
						newShiftedEdges, shiftedEdgesCopy, networkToShiftedEdge, isClusterNetwork);

			}
		}

	}

	private void shiftEdgesUp(HybridNetwork n, MyNode v, HashSet<MyEdge> visitedEdges,
							  HashMap<MyEdge, MyNode> shiftedEdges, HashMap<HybridNetwork, MyNode> shiftedUpNetworks,
							  boolean isClusterNetwork) {

		Iterator<MyEdge> it = v.outEdges().iterator();
		HashSet<MyEdge> shiftingEdges = new HashSet<MyEdge>();
		HashSet<MyNode> sourceNodes = new HashSet<MyNode>();
		HashMap<MyNode, MyEdge> nodeToEdge = new HashMap<MyNode, MyEdge>();
		while (it.hasNext()) {
			MyEdge e = it.next();
			Boolean[] isSpecial = { true };
			isSpecial(n, e, isSpecial);
			if (e.getOwner().equals(n) && (n.isSpecial(e) || isSpecial[0]) && !e.isShifted()
					&& !visitedEdges.contains(e)) {
				HashSet<Integer> treeIndices = getEdgeIndices(e);
				MyNode s = v;
				while (s.getInDegree() == 1) {
					MyEdge inEdge = s.getFirstInEdge();
					s = inEdge.getSource();
					if (s.getInDegree() == 1) {
						if (s.getOutDegree() > 1 && getTreeIndexOutDegree(s, treeIndices, shiftedEdges, v) == 1) {
							sourceNodes.add(s);
						} else if (s.getOutDegree() > 2) {
							sourceNodes.add(s);
							nodeToEdge.put(s, inEdge);
							break;
						} else
							break;
					} else {
						boolean doBreak = true;
						Iterator<MyEdge> itIn = s.inEdges().iterator();
						while (itIn.hasNext()) {
							MyEdge eIn = itIn.next();
							if (getEdgeIndices(eIn).containsAll(treeIndices)) {
								shiftingEdges.add(e);
								s = eIn.getSource();
								sourceNodes.add(s);
								nodeToEdge.put(s, eIn);
								// s = eIn.getSource();
								doBreak = false;
							}
						}
						if (doBreak || getTreeIndexOutDegree(s, treeIndices, shiftedEdges, v) != 1)
							break;
					}
				}
				if (!sourceNodes.isEmpty()) {
					// System.out.println(e.getSource().getLabel() + " -S-> " +
					// e.getTarget().getLabel());
					shiftingEdges.add(e);
					break;
				}
			}
		}

		if (shiftingEdges.isEmpty()) {
			n.update();
			shiftedUpNetworks.put(n, v);
		} else {
			HashSet<MyEdge> newVisitedEdges = (HashSet<MyEdge>) visitedEdges.clone();
			newVisitedEdges.add(shiftingEdges.iterator().next());
			shiftEdgesUp(n, v, newVisitedEdges, shiftedEdges, shiftedUpNetworks, isClusterNetwork);
		}

		for (MyEdge e : shiftingEdges) {

			for (MyNode x : sourceNodes) {

				Vector<MyEdge[]> edgePairs = new Vector<MyEdge[]>();
				MyEdge[] edgePair = { e, null };
				edgePairs.add(edgePair);
				if (nodeToEdge.containsKey(x)) {
					MyEdge[] edgePair2 = { nodeToEdge.get(x), null };
					edgePairs.add(edgePair2);
				}
				for (MyEdge eV : visitedEdges) {
					MyEdge[] edgePair2 = { eV, null };
					edgePairs.add(edgePair2);
				}
				for (MyEdge eS : shiftedEdges.keySet()) {
					MyEdge[] edgePair2 = { eS, null };
					edgePairs.add(edgePair2);
				}

				Vector<MyNode[]> nodePairs = new Vector<MyNode[]>();
				MyNode[] nodePair = { v, null };
				nodePairs.add(nodePair);
				MyNode[] nodePair2 = { x, null };
				nodePairs.add(nodePair2);
				for (MyNode vS : shiftedEdges.values()) {
					MyNode[] nodePair4 = { vS, null };
					nodePairs.add(nodePair4);
				}

				HybridNetwork nCopy = new HybridNetwork(new MyPhyloTree(), false, n.getTaxaOrdering());
				addNetworkToNetworkRec(nCopy.getRoot(), n.getRoot(), n, nCopy, new ConcurrentHashMap<MyNode, MyNode>(),
						isClusterNetwork, edgePairs, nodePairs);

				MyEdge eCopy = edgePairs.get(0)[1];
				MyNode vCopy = nodePairs.get(0)[1];
				MyNode xCopy = nodePairs.get(1)[1];

				HashMap<HybridNetwork, MyNode> newNetworks = new HashMap<HybridNetwork, MyNode>();
				HashMap<HybridNetwork, HashSet<MyEdge>> newVisitedEdges = new HashMap<HybridNetwork, HashSet<MyEdge>>();
				HashMap<HybridNetwork, HashMap<MyEdge, MyNode>> newShiftedEdges = new HashMap<HybridNetwork, HashMap<MyEdge, MyNode>>();
				HashMap<HybridNetwork, MyEdge> networkToShiftedEdge = new HashMap<HybridNetwork, MyEdge>();

				if (getTreeIndexOutDegree(xCopy, getEdgeIndices(eCopy), shiftedEdges, vCopy) == 1) {

					// System.out.println("Case A");

					HashSet<MyEdge> visitedEdgesCopy = new HashSet<MyEdge>();
					for (int i = 1; i < edgePairs.size(); i++)
						visitedEdgesCopy.add(edgePairs.get(i)[1]);
					HashMap<MyEdge, MyNode> shiftedEdgesCopy = new HashMap<MyEdge, MyNode>();
					int counter = 2;
					int offset = nodeToEdge.containsKey(x) ? 2 : 1;
					for (int i = visitedEdges.size() + offset; i < edgePairs.size(); i++) {
						shiftedEdgesCopy.put(edgePairs.get(i)[1], nodePairs.get(counter)[1]);
						counter++;
					}

					MyNode tCopy = eCopy.getTarget();
					HashSet<Integer> eCopyIndices = getEdgeIndices(e);
					nCopy.deleteEdge(eCopy);
					// eCopy.getSource().removeOutEdge(eCopy);
					MyEdge newEdge = nCopy.newEdge(xCopy, tCopy);
					addEdgeIndices(newEdge, (HashSet<Integer>) eCopyIndices.clone());
					nCopy.setSpecial(newEdge, true);
					nCopy.setWeight(newEdge, 0);
					shiftedEdgesCopy.put(newEdge, vCopy);
					networkToShiftedEdge.put(nCopy, newEdge);

					if (vCopy.getInDegree() == 1 && vCopy.getOutDegree() == 1) {

						MyNode s = vCopy.getFirstInEdge().getSource();
						MyNode t = vCopy.getFirstOutEdge().getTarget();
						HashSet<Integer> eDeleteIndices = getEdgeIndices(vCopy.getFirstInEdge());
						nCopy.deleteEdge(vCopy.getFirstInEdge());
						nCopy.deleteEdge(vCopy.getFirstOutEdge());
						nCopy.deleteNode(vCopy);
						newEdge = nCopy.newEdge(s, t);
						addEdgeIndices(newEdge, (HashSet<Integer>) eDeleteIndices.clone());

						newNetworks.put(nCopy, t);
						newVisitedEdges.put(nCopy, visitedEdgesCopy);
						newShiftedEdges.put(nCopy, shiftedEdgesCopy);
					} else {
						newNetworks.put(nCopy, vCopy);
						newVisitedEdges.put(nCopy, visitedEdgesCopy);
						newShiftedEdges.put(nCopy, shiftedEdgesCopy);
					}

				} else if (nodeToEdge.containsKey(x)) {

					HashSet<MyEdge> visitedEdgesCopy = new HashSet<MyEdge>();
					for (int i = 2; i < edgePairs.size(); i++)
						visitedEdgesCopy.add(edgePairs.get(i)[1]);
					HashMap<MyEdge, MyNode> shiftedEdgesCopy = new HashMap<MyEdge, MyNode>();
					int counter = 2;
					for (int i = visitedEdges.size() + 2; i < edgePairs.size(); i++) {
						shiftedEdgesCopy.put(edgePairs.get(i)[1], nodePairs.get(counter)[1]);
						counter++;
					}

					MyEdge midEdge = edgePairs.get(1)[1];
					MyNode midTarget = midEdge.getTarget();
					nCopy.deleteEdge(midEdge);
					HashSet<Integer> midEdgeIndices = getEdgeIndices(midEdge);
					MyNode midNode = new MyNode(nCopy, xCopy.getLabel());
					MyEdge newEdge = nCopy.newEdge(xCopy, midNode);
					addEdgeIndices(newEdge, (HashSet<Integer>) midEdgeIndices.clone());
					newEdge = nCopy.newEdge(midNode, midTarget);
					addEdgeIndices(newEdge, (HashSet<Integer>) midEdgeIndices.clone());

					Vector<MyEdge> moveEdges = new Vector<MyEdge>();
					Iterator<MyEdge> itOut = xCopy.outEdges().iterator();
					while (itOut.hasNext()) {
						MyEdge eOut = itOut.next();
						if (eOut.isShifted() && shiftedEdgesCopy.get(eOut) != null
								&& shiftedEdgesCopy.get(eOut).equals(vCopy)) {
							moveEdges.add(eOut);
						}
					}

					if (!moveEdges.isEmpty()) {
						for (MyEdge eMove : moveEdges) {
							HashSet<Integer> eMoveIndices = getEdgeIndices(eMove);
							MyNode t = eMove.getTarget();
							nCopy.deleteEdge(eMove);
							newEdge = nCopy.newEdge(midNode, t);
							nCopy.setSpecial(newEdge, true);
							nCopy.setWeight(newEdge, 0);
							newEdge.setShifted(true);
							addEdgeIndices(newEdge, (HashSet<Integer>) eMoveIndices.clone());
							shiftedEdgesCopy.put(newEdge, midNode);
						}
						MyNode s = xCopy.getFirstInEdge().getSource();
						MyNode t = xCopy.getFirstOutEdge().getTarget();
						HashSet<Integer> eDeleteIndices = getEdgeIndices(xCopy.getFirstInEdge());
						nCopy.deleteEdge(xCopy.getFirstInEdge());
						newEdge = nCopy.newEdge(s, t);
						addEdgeIndices(newEdge, (HashSet<Integer>) eDeleteIndices.clone());
					}

					MyNode tCopy = eCopy.getTarget();
					HashSet<Integer> eCopyIndices = getEdgeIndices(e);
					// eCopy.getSource().removeOutEdge(eCopy);
					nCopy.deleteEdge(eCopy);
					newEdge = nCopy.newEdge(midNode, tCopy);
					addEdgeIndices(newEdge, (HashSet<Integer>) eCopyIndices.clone());
					nCopy.setSpecial(newEdge, true);
					nCopy.setWeight(newEdge, 0);
					newEdge.setShifted(true);
					shiftedEdgesCopy.put(newEdge, vCopy);
					networkToShiftedEdge.put(nCopy, newEdge);

					moveEdges = new Vector<MyEdge>();
					itOut = vCopy.outEdges().iterator();
					while (itOut.hasNext()) {
						MyEdge eOut = itOut.next();
						if (eOut.isShifted() && shiftedEdgesCopy.get(eOut) != null
								&& shiftedEdgesCopy.get(eOut).equals(xCopy)) {
							moveEdges.add(eOut);
						}
					}

					for (MyEdge eMove : moveEdges) {
						HashSet<Integer> eMoveIndices = getEdgeIndices(eMove);
						MyNode t = eMove.getTarget();
						nCopy.deleteEdge(eMove);
						newEdge = nCopy.newEdge(midNode, t);
						nCopy.setSpecial(newEdge, true);
						nCopy.setWeight(newEdge, 0);
						newEdge.setShifted(true);
						addEdgeIndices(newEdge, (HashSet<Integer>) eMoveIndices.clone());
						shiftedEdgesCopy.put(newEdge, midNode);
					}

					if (vCopy.getInDegree() == 1 && vCopy.getOutDegree() == 1) {

						MyNode s = vCopy.getFirstInEdge().getSource();
						MyNode t = vCopy.getFirstOutEdge().getTarget();
						HashSet<Integer> eDeleteIndices = getEdgeIndices(vCopy.getFirstInEdge());
						nCopy.deleteEdge(vCopy.getFirstInEdge());
						nCopy.deleteEdge(vCopy.getFirstOutEdge());
						// nCopy.deleteNode(vCopy);
						newEdge = nCopy.newEdge(s, t);
						addEdgeIndices(newEdge, (HashSet<Integer>) eDeleteIndices.clone());

						vCopy = t;

					}

					if (nodeToEdge.containsKey(x) && nodeToEdge.get(x).getTarget().getInDegree() > 1) {
						// System.out.println(n.toMyBracketString()+"\n"+nCopy.toMyBracketString());
						newVisitedEdges.put(nCopy, visitedEdgesCopy);
						newNetworks.put(nCopy, vCopy);
						newShiftedEdges.put(nCopy, shiftedEdgesCopy);
						// shiftEdgesUp2(nCopy, vCopy, tM, shiftedUpNetworks,
						// isClusterNetwork);
					}

					moveEdges = new Vector<MyEdge>();
					it = xCopy.outEdges().iterator();
					while (it.hasNext()) {
						MyEdge eOut = it.next();
						boolean addEdge = true;
						for (int index : getEdgeIndices(eOut)) {
							if (eCopyIndices.contains(index))
								addEdge = false;
						}
						if (addEdge)
							moveEdges.add(eOut);
					}

					// System.out.println(vCopy.getLabel()+" "+xCopy.getLabel()+" "+moveEdges.size()+"\n"+nCopy.toMyBracketString());

					shiftEdgesRec(midNode, moveEdges, 0, nCopy, newNetworks, vCopy, newVisitedEdges, visitedEdgesCopy,
							newShiftedEdges, shiftedEdgesCopy, networkToShiftedEdge, isClusterNetwork);

				}

				for (HybridNetwork newNetwork : newNetworks.keySet()) {

					newNetwork.update();
					shiftEdgesUp(newNetwork, newNetworks.get(newNetwork), newVisitedEdges.get(newNetwork),
							newShiftedEdges.get(newNetwork), shiftedUpNetworks, isClusterNetwork);

					MyEdge shiftedEdge = networkToShiftedEdge.get(newNetwork);
					cmpHangingNodes(shiftedEdge.getSource(), getEdgeIndices(shiftedEdge), shiftedEdge, newNetwork,
							newNetworks.get(newNetwork), newVisitedEdges.get(newNetwork),
							newShiftedEdges.get(newNetwork), isClusterNetwork, false, shiftedUpNetworks, null);

				}

			}
		}
	}

	private void isSpecial(HybridNetwork n, MyEdge e, Boolean[] b) {
		if (b[0]) {
			if (e.getTarget().getOutDegree() == 0) {
				b[0] = false;
			} else if (e.getOwner().equals(n) && !n.isSpecial(e)) {
				Iterator<MyEdge> it = e.getTarget().outEdges().iterator();
				while (it.hasNext())
					isSpecial(n, it.next(), b);
			}
		}
	}

	private void cmpHangingNodes(MyNode s, HashSet<Integer> treeIndices, MyEdge shiftedEdge, HybridNetwork n, MyNode v,
								 HashSet<MyEdge> visitedEdges, HashMap<MyEdge, MyNode> shiftedEdges, boolean isClusterNetwork,
								 boolean shiftDown, HashMap<HybridNetwork, MyNode> shiftedNetworks,
								 HashMap<HybridNetwork, HashMap<MyEdge, MyNode>> networkToShiftedEdges) {
		Iterator<MyEdge> it = s.outEdges().iterator();
		while (it.hasNext()) {
			MyEdge e = it.next();
			boolean isHangingEdge = true;
			for (int index : getEdgeIndices(e)) {
				if (treeIndices.contains(index))
					isHangingEdge = false;
			}
			MyNode h = e.getTarget();
			if (isHangingEdge && h.getInDegree() == 1) {

				Vector<MyEdge[]> edgePairs = new Vector<MyEdge[]>();
				MyEdge[] edgePair0 = { shiftedEdge, null };
				edgePairs.add(edgePair0);
				for (MyEdge eV : visitedEdges) {
					MyEdge[] edgePair2 = { eV, null };
					edgePairs.add(edgePair2);
				}
				for (MyEdge eS : shiftedEdges.keySet()) {
					MyEdge[] edgePair2 = { eS, null };
					edgePairs.add(edgePair2);
				}

				Vector<MyNode[]> nodePairs = new Vector<MyNode[]>();
				MyNode[] nodePair0 = { v, null };
				nodePairs.add(nodePair0);
				MyNode[] nodePair1 = { h, null };
				nodePairs.add(nodePair1);
				for (MyNode vS : shiftedEdges.values()) {
					MyNode[] nodePair4 = { vS, null };
					nodePairs.add(nodePair4);
				}

				HybridNetwork nCopy = new HybridNetwork(new MyPhyloTree(), false, n.getTaxaOrdering());
				addNetworkToNetworkRec(nCopy.getRoot(), n.getRoot(), n, nCopy, new ConcurrentHashMap<MyNode, MyNode>(),
						isClusterNetwork, edgePairs, nodePairs);

				MyEdge shiftedEdgeCopy = edgePairs.get(0)[1];
				HashSet<MyEdge> visitedEdgesCopy = new HashSet<MyEdge>();
				for (int i = 1; i < visitedEdges.size() + 1; i++)
					visitedEdgesCopy.add(edgePairs.get(i)[1]);
				HashMap<MyEdge, MyNode> shiftedEdgesCopy = new HashMap<MyEdge, MyNode>();
				int counter = 2;
				for (int i = visitedEdges.size() + 1; i < edgePairs.size(); i++) {
					shiftedEdgesCopy.put(edgePairs.get(i)[1], nodePairs.get(counter)[1]);
					counter++;
				}
				MyNode vCopy = nodePairs.get(0)[1];
				MyNode hCopy = nodePairs.get(1)[1];

				HashSet<Integer> shiftedEdgeIndices = getEdgeIndices(shiftedEdgeCopy);
				MyNode t = shiftedEdgeCopy.getTarget();
				nCopy.deleteEdge(shiftedEdgeCopy);
				MyEdge newEdge = nCopy.newEdge(hCopy, t);
				nCopy.setSpecial(newEdge, true);
				nCopy.setWeight(newEdge, 0);
				newEdge.setShifted(true);
				shiftedEdgesCopy.put(newEdge, vCopy);
				addEdgeIndices(newEdge, (HashSet<Integer>) shiftedEdgeIndices.clone());

				if (shiftDown)
					shiftEdgesDown(nCopy, vCopy, visitedEdgesCopy, shiftedEdgesCopy, shiftedNetworks, isClusterNetwork,
							networkToShiftedEdges);
				else
					shiftEdgesUp(nCopy, vCopy, visitedEdgesCopy, shiftedEdgesCopy, shiftedNetworks, isClusterNetwork);

				cmpHangingNodes(h, treeIndices, shiftedEdge, n, v, visitedEdges, shiftedEdges, isClusterNetwork,
						shiftDown, shiftedNetworks, networkToShiftedEdges);

			}
		}
	}

	private int getTreeIndexOutDegree(MyNode v, HashSet<Integer> treeIndices, HashMap<MyEdge, MyNode> shiftedEdges,
									  MyNode rootNode) {
		int outDegree = 0;
		Iterator<MyEdge> it = v.outEdges().iterator();
		while (it.hasNext()) {
			MyEdge e = it.next();
			boolean b = shiftedEdges.containsKey(e) ? !rootNode.equals(shiftedEdges.get(e)) : true;
			// if (b && tM.getEdgeIndices(e).containsAll(treeIndices))
			// outDegree++;
			for (int treeIndex : treeIndices) {
				if (b && getEdgeIndices(e).contains(treeIndex)) {
					outDegree++;
					break;
				}
			}
		}
		return outDegree;
	}

	// simple recursion that adds all nodes and edges of a resolved network
	private void addNetworkToNetworkRec(MyNode vCopy, MyNode v, HybridNetwork toCopy, HybridNetwork n,
										ConcurrentHashMap<MyNode, MyNode> created, boolean isClusterNetwork, Vector<MyEdge[]> edgePairs,
										Vector<MyNode[]> nodePairs) {
		Iterator<MyEdge> it = toCopy.getOutEdges(v);
		while (it.hasNext()) {
			MyEdge e = it.next();
			MyNode c = e.getTarget();
			MyNode cCopy;
			MyEdge eCopy;
			if (!created.containsKey(c)) {
				cCopy = n.newNode(c);
				n.setLabel(cCopy, toCopy.getLabel(c));
				created.put(c, cCopy);
				eCopy = n.newEdge(vCopy, cCopy);
				eCopy.setShifted(e.isShifted());
				eCopy.setInfo(e.getInfo());

				if (e.getOwner().equals(toCopy) && toCopy.isSpecial(e)) {
					n.setSpecial(eCopy, true);
					n.setWeight(eCopy, 0);
				}

				addNetworkToNetworkRec(cCopy, c, toCopy, n, created, isClusterNetwork, edgePairs, nodePairs);
			} else {
				cCopy = created.get(c);
				eCopy = n.newEdge(vCopy, cCopy);
				eCopy.setShifted(e.isShifted());
				eCopy.setInfo(e.getInfo());

				if (e.getOwner().equals(toCopy) && toCopy.isSpecial(e)) {
					n.setSpecial(eCopy, true);
					n.setWeight(eCopy, 0);
				}

			}

			if (edgePairs != null) {
				for (MyEdge[] edgePair : edgePairs) {
					if (edgePair[0] != null && edgePair[0].equals(e))
						edgePair[1] = eCopy;
				}
			}

		}

		if (nodePairs != null) {
			for (MyNode[] nodePair : nodePairs) {
				if (nodePair[0] != null && nodePair[0].equals(v))
					nodePair[1] = vCopy;
			}
		}
	}

	public void setStop(boolean stop) {
		this.stop = stop;
	}

	public Vector<MyPhyloTree> getNetworks() {
		return networks;
	}

	public int getNumOfShiftNets() {
		return numOfShiftNets;
	}

}
