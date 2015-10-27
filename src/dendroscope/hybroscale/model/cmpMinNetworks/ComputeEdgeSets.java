package dendroscope.hybroscale.model.cmpMinNetworks;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import dendroscope.hybroscale.model.treeObjects.SparseNetEdge;
import dendroscope.hybroscale.model.treeObjects.SparseNetNode;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;

public class ComputeEdgeSets {

	private SparseNetwork n;
	private Vector<SparseNetEdge> reticulateEdges;

	private boolean heuristicMode;
	private int treeInsertIndex;
	private Integer singleIndex;

	public ComputeEdgeSets(SparseNetwork n, Vector<SparseNetEdge> reticulateEdges, boolean heuristicMode,
			int treeInsertIndex, Integer singleIndex) {
		this.n = n;
		this.reticulateEdges = reticulateEdges;
		this.heuristicMode = heuristicMode;
		this.treeInsertIndex = treeInsertIndex;
		this.singleIndex = singleIndex;
	}

	public HashSet<BitSet> run() {

		Vector<SparseNetNode> retNodes = new Vector<SparseNetNode>();
		for (SparseNetNode v : n.getNodes()) {
			if (v.getInDegree() > 1)
				retNodes.add(v);
		}

		Vector<BitSet> allEdgeSets = new Vector<BitSet>();
		allEdgeSets.add(new BitSet(reticulateEdges.size()));

		allEdgeSets = computeAllEdgeSets(allEdgeSets, retNodes, 0);

		shrinkEdgeSets(n, allEdgeSets);
		HashSet<BitSet> uniqueEdgeSets = new HashSet<BitSet>();
		Vector<BitSet> toRemove = new Vector<BitSet>();
		for (BitSet edgeSet : allEdgeSets) {
			if (uniqueEdgeSets.contains(edgeSet))
				toRemove.remove(edgeSet);
			uniqueEdgeSets.add(edgeSet);
		}

		if (heuristicMode) {
			for (BitSet edgeSet : toRemove)
				allEdgeSets.remove(edgeSet);
			int border = treeInsertIndex > 1 ? 3 : 8;
			Vector<BitSet> limitedNEdgeSets = new Vector<BitSet>();
			if (uniqueEdgeSets.size() > border) {
				while (limitedNEdgeSets.size() < border) {
					int randIndex = (int) Math.random() * allEdgeSets.size();
					limitedNEdgeSets.add(allEdgeSets.get(randIndex));
					allEdgeSets.remove(randIndex);
				}
				uniqueEdgeSets.clear();
				uniqueEdgeSets.addAll(limitedNEdgeSets);
			}
		}
		
		if(singleIndex != null)
			filterSingleSets(uniqueEdgeSets);
		
		return uniqueEdgeSets;
	}

	private void filterSingleSets(HashSet<BitSet> uniqueEdgeSets) {
		BitSet singleSet = new BitSet(reticulateEdges.size());
		for(SparseNetEdge e : reticulateEdges){
			if(e.getIndices().contains(singleIndex) && e.getIndices().size() == 1)
				singleSet.set(reticulateEdges.indexOf(e));
		}
		
		Vector<BitSet> toRemove = new Vector<BitSet>();
		for(BitSet b : uniqueEdgeSets){
			BitSet bCheck = (BitSet) singleSet.clone();
			bCheck.and(b);
			if(bCheck.isEmpty())
				toRemove.add(b);
		}
		
		for(BitSet b : toRemove)
			uniqueEdgeSets.remove(b);
	}

	private Vector<BitSet> computeAllEdgeSets(Vector<BitSet> allEdgeSets, Vector<SparseNetNode> retNodes, int nodeIndex) {
		if (nodeIndex < retNodes.size()) {
			Vector<BitSet> newAllEdgeSets = new Vector<BitSet>();
			for (BitSet edgeSet : allEdgeSets) {
				Vector<SparseNetEdge> retEdges = retNodes.get(nodeIndex).getInEdges();
				for (SparseNetEdge e : retEdges) {
					BitSet set = (BitSet) edgeSet.clone();
					int bitIndex = reticulateEdges.indexOf(e);
					set.set(bitIndex);
					newAllEdgeSets.add(set);
				}
			}
			return computeAllEdgeSets(newAllEdgeSets, retNodes, nodeIndex + 1);
		} else
			return allEdgeSets;
	}

	private void shrinkEdgeSets(SparseNetwork n, Vector<BitSet> allEdgeSets) {

		Hashtable<Integer, Vector<Integer>> edgeIndexToSet = new Hashtable<Integer, Vector<Integer>>();
		for (SparseNetEdge e : reticulateEdges) {

			int edgeIndex = reticulateEdges.indexOf(e);

			Vector<SparseNetEdge> leafEdges = new Vector<SparseNetEdge>();
			computeLeafEdges(n, e, leafEdges);

			if (leafEdges.isEmpty()) {

				Vector<SparseNetEdge> bottomEdges = new Vector<SparseNetEdge>();
				computeBottomEdges(n, e, bottomEdges);

				Vector<Integer> bottomIndices = new Vector<Integer>();
				for (SparseNetEdge bottomEdge : bottomEdges) {
					int bottomIndex = reticulateEdges.indexOf(bottomEdge);
					bottomIndices.add(bottomIndex);
				}

				edgeIndexToSet.put(edgeIndex, bottomIndices);

			}
		}

		for (BitSet edgeSet : allEdgeSets)
			resetBits(edgeSet, edgeIndexToSet);

	}

	private void resetBits(BitSet edgeSet, Hashtable<Integer, Vector<Integer>> edgeIndexToSet) {
		boolean isModified = false;
		int i = edgeSet.nextSetBit(0);
		while (i != -1) {
			if (edgeIndexToSet.containsKey(i)) {
				boolean removeEdgeChoice = true;
				for (int j : edgeIndexToSet.get(i)) {
					if (edgeSet.get(j)) {
						removeEdgeChoice = false;
						break;
					}
				}
				if (removeEdgeChoice) {
					edgeSet.set(i, false);
					isModified = true;
				}
			}
			i = edgeSet.nextSetBit(i + 1);
		}
		if (isModified)
			resetBits(edgeSet, edgeIndexToSet);
	}

	private void computeBottomEdges(SparseNetwork n, SparseNetEdge e, Vector<SparseNetEdge> bottomEdges) {
		for (SparseNetEdge eOut : e.getTarget().getOutEdges()) {
			if (n.isSpecial(eOut))
				bottomEdges.add(eOut);
			else
				computeBottomEdges(n, eOut, bottomEdges);
		}
	}

	private void computeLeafEdges(SparseNetwork n, SparseNetEdge e, Vector<SparseNetEdge> leafEdges) {
		if (e.getTarget().getOutDegree() == 0)
			leafEdges.add(e);
		else {
			for (SparseNetEdge eOut : e.getTarget().getOutEdges()) {
				if (!n.isSpecial(eOut))
					computeLeafEdges(n, eOut, leafEdges);
			}
		}
	}

}
