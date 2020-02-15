/*
 *   UpdateEmbeddings.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.cmpMinNetworks;

import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.model.treeObjects.SparseNetEdge;
import dendroscope.hybroscale.model.treeObjects.SparseNetNode;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.util.graph.MyNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UpdateEmbeddings {

	private Vector<String> taxaOrdering;
	private HashMap<Integer, Vector<String>> treeToTaxa;
	private HybridTree[] addedTrees;

	public Vector<SparseNetwork> run(HybridTree[] addedTrees, int treeIndex, SparseNetwork n,
									 Vector<String> taxaOrdering) {

		this.taxaOrdering = taxaOrdering;
		this.treeToTaxa = new HashMap<Integer, Vector<String>>();
		this.addedTrees = addedTrees;

		for (int index = 0; index <= treeIndex; index++) {
			Vector<String> taxa = new Vector<String>();
			HybridTree t = addedTrees[index];
			for (MyNode v : t.getLeaves())
				taxa.add(v.getLabel());
			treeToTaxa.put(index, taxa);
		}

		Vector<SparseNetEdge> reticulateEdges = new Vector<SparseNetEdge>();
		for (SparseNetEdge e : n.getEdges()) {
			if (n.isSpecial(e))
				reticulateEdges.add(e);
		}

		// computing all possible edge mappings for each tree
		HashMap<Integer, HashSet<BitSet>> indexToEdgeSets = new HashMap<Integer, HashSet<BitSet>>();
		HashSet<BitSet> allEdgeSets = new ComputeEdgeSets(n, reticulateEdges, false, -1, null).run();
		for (int index = 0; index <= treeIndex; index++) {
			for (BitSet edgeSet : allEdgeSets) {

				Vector<SparseNetEdge> chosenEdges = new Vector<SparseNetEdge>();
				int i = edgeSet.nextSetBit(0);
				while (i != -1) {
					chosenEdges.add(reticulateEdges.get(i));
					i = edgeSet.nextSetBit(i + 1);
				}

				HashSet<BitSet> netClusters = new HashSet<BitSet>();
				cmpClustersRec(index, n.getRoot(), chosenEdges, netClusters);

				HybridTree t = addedTrees[index];
				boolean displaysTree = true;
				for (BitSet treeCluster : t.getClusterSet()) {
					if (!treeCluster.isEmpty() && !netClusters.contains(treeCluster)) {
						displaysTree = false;
						break;
					}
				}

				if (displaysTree) {
					if (!indexToEdgeSets.containsKey(index))
						indexToEdgeSets.put(index, new HashSet<BitSet>());
					indexToEdgeSets.get(index).add((BitSet) edgeSet.clone());
				}

			}

		}

		// distributing all edge mappings in all possible ways
		Vector<SparseNetwork> updatedNetworks = new Vector<SparseNetwork>();
		Vector<Integer> treeIndices = new Vector<Integer>();
		for (int key : indexToEdgeSets.keySet())
			treeIndices.add(key);
		addIndicesRec(n, reticulateEdges, 0, treeIndices, indexToEdgeSets, updatedNetworks);

		// remove empty edges
		Vector<SparseNetwork> finalNetworks = new Vector<SparseNetwork>();
		for (SparseNetwork upNet : updatedNetworks) {
			if (removeEmptyEdges(upNet))
				finalNetworks.add(upNet);
		}

		return finalNetworks;
	}

	private boolean removeEmptyEdges(SparseNetwork upNet) {

		Vector<SparseNetEdge> toDelete = new Vector<SparseNetEdge>();
		for (SparseNetEdge e : upNet.getEdges()) {
			if (e.getTarget().getInDegree() > 1 && e.getIndices().isEmpty())
				toDelete.add(e);
		}
		for (SparseNetEdge e : toDelete) {

			// System.out.println(">\n"+upNet.getPhyloTree().toMyBracketString());
			// System.out.println(e.getSource().getLabel()+" "+e.getTarget().getLabel());

			if (e.getSource().getInDegree() == 1) {

				SparseNetNode retNode = e.getTarget();
				SparseNetNode x = e.getSource();
				SparseNetEdge eX = x.getInEdges().get(0);
				SparseNetNode p = eX.getSource();

				x.removeOutEdge(e);
				p.removeOutEdge(eX);

				Vector<SparseNetNode> potIsolatedNodes = new Vector<SparseNetNode>();
				potIsolatedNodes.add(p);
				if (retNode.getInDegree() == 1) {
					potIsolatedNodes.add(retNode);
					for (SparseNetEdge eIn : retNode.getInEdges()) {
						potIsolatedNodes.add(eIn.getSource());
						potIsolatedNodes.add(eIn.getSource().getInEdges().get(0).getSource());
					}
				}

				for (SparseNetNode v : potIsolatedNodes)
					removeIsolatedNode(v, upNet);
			}
			// System.out.println(upNet.getPhyloTree()+";");
		}

		for (SparseNetNode v : upNet.getNodes()) {
			if (v.getInDegree() > 1) {
				SparseNetNode c = v.getOutEdges().get(0).getTarget();
				if (c.getInDegree() == 1 && c.getOutDegree() == 1)
					return false;
			}
		}

		return true;
	}

	private void removeIsolatedNode(SparseNetNode v, SparseNetwork n) {
		if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
			SparseNetNode p = v.getInEdges().get(0).getSource();
			if (p.getInDegree() == 1 || (!n.isSpecial(v.getInEdges().get(0)) && !n.isSpecial(v.getOutEdges().get(0)))) {
				HashSet<Integer> indices = (HashSet<Integer>) v.getOutEdges().get(0).getIndices().clone();
				SparseNetNode c = v.getOutEdges().get(0).getTarget();
				p.removeOutEdge(v.getInEdges().get(0));
				v.removeOutEdge(v.getOutEdges().get(0));
				SparseNetEdge e = p.addChild(c);
				e.addIndices(indices);
			}
		}
	}

	private void addIndicesRec(SparseNetwork n, Vector<SparseNetEdge> retEdges, int i, Vector<Integer> treeIndices,
							   HashMap<Integer, HashSet<BitSet>> indexToEdgeSets, Vector<SparseNetwork> updatedNetworks) {
		if (i < treeIndices.size()) {
			int treeIndex = treeIndices.get(i);
			for (BitSet b : indexToEdgeSets.get(treeIndex)) {

				Vector<SparseNetEdge[]> edgePairs = new Vector<SparseNetEdge[]>();
				for (SparseNetEdge e : retEdges) {
					SparseNetEdge[] edgePair = { e, null };
					edgePairs.add(edgePair);
				}

				SparseNetwork nCopy = copyNetwork(n, edgePairs);
				Vector<SparseNetEdge> retEdgesCopy = new Vector<SparseNetEdge>();
				for (SparseNetEdge[] edgePair : edgePairs)
					retEdgesCopy.add(edgePair[1]);

				for (SparseNetEdge e : nCopy.getEdges())
					e.getIndices().remove(treeIndex);
				int bitIndex = b.nextSetBit(0);
				while (bitIndex != -1) {
					retEdgesCopy.get(bitIndex).addIndex(treeIndex);
					bitIndex = b.nextSetBit(bitIndex + 1);
				}
				updateIndices(nCopy, addedTrees[treeIndex], treeIndex);
				for (SparseNetEdge e : retEdgesCopy) {
					SparseNetEdge eX = e.getSource().getInEdges().get(0);
					if (!eX.getIndices().contains(treeIndex))
						e.removeIndex(treeIndex);
				}

				int newI = i + 1;
				addIndicesRec(nCopy, retEdgesCopy, newI, treeIndices, indexToEdgeSets, updatedNetworks);

			}

		} else
			updatedNetworks.add(n);

	}

	private void updateIndices(SparseNetwork n, HybridTree t, int treeIndex) {
		Vector<String> taxa = new Vector<String>();
		for (MyNode leaf : t.getLeaves())
			taxa.add(leaf.getLabel());
		for (SparseNetNode leaf : n.getLeaves()) {
			if (taxa.contains(leaf.getLabel()))
				updateIndicesRec(leaf, treeIndex);
		}
	}

	private void updateIndicesRec(SparseNetNode v, int treeIndex) {
		if (v.getInDegree() == 1) {
			v.getInEdges().get(0).addIndex(treeIndex);
			updateIndicesRec(v.getInEdges().get(0).getSource(), treeIndex);
		} else if (v.getInDegree() > 1) {
			SparseNetEdge eIn = null;
			for (SparseNetEdge e : v.getInEdges()) {
				if (e.getIndices().contains(treeIndex)) {
					eIn = e;
					break;
				}
			}
			updateIndicesRec(eIn.getSource(), treeIndex);
		}
	}

	private BitSet cmpClustersRec(int index, SparseNetNode v, Vector<SparseNetEdge> reticulateEdges,
								  HashSet<BitSet> clusters) {
		if (v.getOutDegree() == 0 && treeToTaxa.get(index).contains(v.getLabel())) {
			BitSet b = new BitSet(taxaOrdering.size());
			b.set(taxaOrdering.indexOf(v.getLabel()));
			clusters.add((BitSet) b.clone());
			return b;
		} else {
			Iterator<SparseNetEdge> it = v.outEdges().iterator();
			BitSet b = new BitSet(taxaOrdering.size());
			while (it.hasNext()) {
				SparseNetEdge e = it.next();
				if (e.getTarget().getInDegree() < 2 || reticulateEdges.contains(e))
					b.or(cmpClustersRec(index, e.getTarget(), reticulateEdges, clusters));
			}
			clusters.add((BitSet) b.clone());
			return b;
		}

	}

	private SparseNetwork copyNetwork(SparseNetwork n, Vector<SparseNetEdge[]> edgePairs) {
		SparseNetwork nCopy = new SparseNetwork(new SparseNetNode(null, null, n.getRoot().getLabel()));
		nCopy.getRoot().setOwner(nCopy);
		copyNetworkRec(nCopy.getRoot(), n.getRoot(), nCopy, n, new Vector<SparseNetNode>(),
				new ConcurrentHashMap<SparseNetNode, SparseNetNode>(), edgePairs);
		return nCopy;
	}

	private void copyNetworkRec(SparseNetNode vCopy, SparseNetNode v, SparseNetwork nCopy, SparseNetwork n,
								Vector<SparseNetNode> visited, ConcurrentHashMap<SparseNetNode, SparseNetNode> nodeToCopy,
								Vector<SparseNetEdge[]> edgePairs) {
		Iterator<SparseNetEdge> it = v.getOutEdges().iterator();
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
				eCopy = cCopy.getInEdges().firstElement();
			}
			if (edgePairs != null) {
				for (SparseNetEdge[] edgePair : edgePairs) {
					if (e.equals(edgePair[0]))
						edgePair[1] = eCopy;
				}
			}
			if (n.isSpecial(e)) {
				nodeToCopy.put(c, cCopy);
				eCopy.addIndices((HashSet<Integer>) e.getIndices().clone());
			}
			HashSet<Integer> treeIndices = (HashSet<Integer>) e.getEdgeIndex().clone();
			eCopy.setEdgeIndex(treeIndices);
			if (!visited.contains(c)) {
				visited.add(c);
				copyNetworkRec(cCopy, c, nCopy, n, visited, nodeToCopy, edgePairs);
			}
		}
	}

}
