package dendroscope.hybroscale.model.cmpMinNetworks;

import dendroscope.hybroscale.model.HybridManager.Computation;
import dendroscope.hybroscale.model.treeObjects.*;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Vector;

public class CheckReEmbedding {

	private Vector<Integer> treeMapping;

	public Vector<Object[]> run(SparseNetwork n, int curIndex, SparseTree[] trees, Vector<Integer> treeMap,
			Vector<String> taxaOrdering, int k, Computation compValue) {

		Vector<Object[]> results = new Vector<Object[]>();
		this.treeMapping = (Vector<Integer>) treeMap.clone();

		HashSet<Integer> switchIndices = checkNetwork(n, curIndex);
		for (int switchIndex : switchIndices) {

			if (treeMapping.get(switchIndex) < treeMapping.get(curIndex)) {

				SparseNetwork nCopy = new SparseNetwork(n);
				Vector<SparseNetEdge> shrinkedEdges = deleteRetEdges(nCopy, switchIndex, curIndex);

				if (new CountRetNumber().getNumber(nCopy, compValue) < k
						&& n.getEdges().size() > nCopy.getEdges().size()) {

					SparseTree[] newTrees = new SparseTree[trees.length];
					for (int i = 0; i < trees.length; i++)
						newTrees[i] = new SparseTree(trees[i]);
					newTrees[curIndex] = new SparseTree(trees[switchIndex]);
					newTrees[switchIndex] = new SparseTree(trees[curIndex]);

					Vector<Integer> newTreeMapping = (Vector<Integer>) treeMapping.clone();
					int newIndex;
					newTreeMapping.removeElementAt(curIndex);
					newIndex = treeMapping.get(switchIndex);
					newTreeMapping.add(curIndex, newIndex);
					newTreeMapping.removeElementAt(switchIndex);
					newIndex = treeMapping.get(curIndex);
					newTreeMapping.add(switchIndex, newIndex);

					HybridTree[] newHybridTrees = new HybridTree[trees.length];
					for (int i = 0; i < newTrees.length; i++) {
						newHybridTrees[i] = new HybridTree(newTrees[i].getPhyloTree(), false, taxaOrdering);
						newHybridTrees[i].update();
					}

					Vector<SparseNetEdge> retEdges = new Vector<SparseNetEdge>();
					for (SparseNetEdge e : nCopy.getEdges()) {
						if (nCopy.isSpecial(e))
							retEdges.add(e);
					}
					HashSet<BitSet> uniqueEdgeSets = new ComputeEdgeSets(nCopy, retEdges, false, curIndex, switchIndex)
							.run();
					Vector<BitSet> edgeSets = new Vector<BitSet>();
					for (BitSet b : uniqueEdgeSets)
						edgeSets.add(b);

					Vector<Integer> toDelete = new Vector<Integer>();
					Vector<BitSet> solidEdgeSets = new Vector<BitSet>();
					for (BitSet edgeSet : edgeSets) {
						BitSet solidEdgeSet = new BitSet(retEdges.size());
						int index = edgeSet.nextSetBit(0);
						while (index != -1) {
							SparseNetEdge retEdge = retEdges.get(index);
							if (retEdge.getIndices().contains(switchIndex) && retEdge.getIndices().size() == 1
									&& !shrinkedEdges.contains(retEdge))
								solidEdgeSet.set(index);
							index = edgeSet.nextSetBit(index + 1);
						}
						solidEdgeSets.add(solidEdgeSet);
						if (solidEdgeSet.cardinality() == 0)
							toDelete.add(edgeSets.indexOf(edgeSet));
					}

					for (int i = toDelete.size() - 1; i >= 0; i--) {
						edgeSets.removeElementAt(toDelete.get(i));
						solidEdgeSets.removeElementAt(toDelete.get(i));
					}

					boolean isUnique = true;

					Object[] result = { nCopy, newTrees, newTreeMapping, newHybridTrees, retEdges, edgeSets,
							solidEdgeSets };
					results.add(result);

				}
			}
		}

		return results;

	}

	private HashSet<Integer> checkNetwork(SparseNetwork n, int curIndex) {
		HashSet<Integer> switchIndices = new HashSet<Integer>();

		// for (SparseNetEdge e : n.getEdges()) {
		// if (e.getTarget().getInDegree() > 1 &&
		// e.getIndices().contains(curIndex) && e.getIndices().size() == 1) {
		for (int i = 0; i < curIndex; i++)
			// if (treeMapping.get(curIndex) > 1 && treeMapping.get(i) <
			// treeMapping.get(curIndex)) {
			// if (treeMapping.get(curIndex) > 1 && treeMapping.get(i) <
			// treeMapping.get(curIndex)){
			// if (treeMapping.get(i) < treeMapping.get(curIndex)){
			switchIndices.add(i);
		// }
		// }
		// if (!switchIndices.isEmpty())
		// break;
		// }

		return switchIndices;
	}

	// private HashSet<Integer> checkNetwork(SparseNetwork n, int curIndex,
	// Vector<Integer> treeMapping) {
	// HashSet<Integer> switchIndices = new HashSet<Integer>();
	//
	// for (SparseNetEdge e : n.getEdges()) {
	// if (e.getTarget().getInDegree() > 1 && e.getIndices().contains(curIndex)
	// && e.getIndices().size() == 1) {
    // for (SparseNetEdge eIn : e.getTarget().inEdges().iterator()) {
	// if (eIn != e) {
    // SparseNetNode p = eIn.getSource().inEdges().iterator().get(0).getSource();
    // for (SparseNetEdge e2 : p.outEdges().iterator()) {
	// if (e2.getTarget() != eIn.getSource() && e2.getTarget().getInDegree() ==
	// 1
	// && e2.getTarget().getOutDegree() == 1) {
    // SparseNetEdge e1 = e2.getTarget().outEdges().iterator().get(0);
	// for (int e1Index : e1.getIndices()) {
	// if (eIn.getIndices().contains(e1Index)
	// && treeMapping.get(e1Index) < treeMapping.get(curIndex))
	// switchIndices.add(e1Index);
	// }
	// }
	// }
	// }
	// }
	// }
	// }
	//
	// return switchIndices;
	// }

	private Vector<SparseNetEdge> deleteRetEdges(SparseNetwork n, int switchIndex, int curIndex) {

		Vector<SparseNetEdge> shrinkedEdges = new Vector<SparseNetEdge>();
		Vector<SparseNetEdge> toDelete = new Vector<SparseNetEdge>();
		for (SparseNetEdge e : n.getEdges()) {
			if (e.getIndices().contains(switchIndex)) {
				e.getIndices().remove(switchIndex);
				shrinkedEdges.add(e);
			}
			if (e.getIndices().contains(curIndex)) {
				e.getIndices().remove(curIndex);
				e.getIndices().add(switchIndex);
			}
			if (e.getTarget().getInDegree() > 1 && e.getIndices().isEmpty()) {
				toDelete.add(e);
				shrinkedEdges.remove(e);
			}
		}

		for (SparseNetEdge e : toDelete) {

			SparseNetNode retNode = e.getTarget();
			SparseNetNode x = e.getSource();
            SparseNetEdge eX = x.inEdges().iterator().next();
			SparseNetNode p = eX.getSource();

			x.removeOutEdge(e);
			p.removeOutEdge(eX);

			Vector<SparseNetNode> potIsolatedNodes = new Vector<SparseNetNode>();
			potIsolatedNodes.add(p);
			if (retNode.getInDegree() == 1) {
				potIsolatedNodes.add(retNode);
                for (SparseNetEdge eIn : retNode.inEdges()) {
					potIsolatedNodes.add(eIn.getSource());
                    potIsolatedNodes.add(eIn.getSource().inEdges().iterator().next().getSource());
				}
			}

			for (SparseNetNode v : potIsolatedNodes)
				removeIsolatedNode(v, n);

		}

		return shrinkedEdges;
	}

	private void removeIsolatedNode(SparseNetNode v, SparseNetwork n) {
		if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
            SparseNetNode p = v.inEdges().iterator().next().getSource();
            if (p.getInDegree() == 1 || (!n.isSpecial(v.inEdges().iterator().next()) && !n.isSpecial(v.outEdges().iterator().next()))) {
                HashSet<Integer> indices = (HashSet<Integer>) v.outEdges().iterator().next().getIndices().clone();
                SparseNetNode c = v.outEdges().iterator().next().getTarget();
                p.removeOutEdge(v.inEdges().iterator().next());
                v.removeOutEdge(v.outEdges().iterator().next());
				SparseNetEdge e = p.addChild(c);
				e.addIndices(indices);
			}
		}
	}

}
