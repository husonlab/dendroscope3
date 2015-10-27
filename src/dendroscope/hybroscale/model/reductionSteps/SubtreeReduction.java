package dendroscope.hybroscale.model.reductionSteps;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Vector;

import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;

/**
 * This function replaces all common subtrees of two two rooted, bifurcating
 * phylogenetic trees by a unique taxon labeling.
 * 
 * @author Benjamin Albrecht, 6.2010
 */

public class SubtreeReduction {

	private Vector<String> taxaOdering;

	public Vector<String> run(HybridTree[] trees, ReplacementInfo rI, Vector<String> taxaOrdering) throws Exception {

		this.taxaOdering = taxaOrdering;

		Vector<BitSet> commonSubtrees = new Vector<BitSet>();
		Vector<MyNode> commonNodes = new Vector<MyNode>();

		Iterator<MyNode> it = trees[0].postOrderWalk();
		while (it.hasNext()) {
			MyNode v = it.next();
			if (v.getOutDegree() != 0) {

				Vector<MyNode> children = new Vector<MyNode>();
				Iterator<MyNode> it2 = trees[0].getSuccessors(v);
				while (it2.hasNext())
					children.add(it2.next());

				BitSet b = new BitSet(children.size());
				for (MyNode c : children)
					b.set(children.indexOf(c), commonNodes.contains(c));

				// true if node l is the root of a common subtree
				Boolean bAll = b.cardinality() == children.size() ? true : false;
				// true if node v is the root of a common subtree
				Boolean bV = true;
				Vector<BitSet> t0Clusters = new Vector<BitSet>();
				cmpClusters(v, t0Clusters);
				for (int i = 1; i < trees.length; i++) {
					// if
					// (!trees[i].getClusterSet().contains(trees[0].getNodeToCluster().get(v)))
					// {
					// bV = false;
					// break;
					// }
					if (!trees[i].getClusterSet().contains(trees[0].getNodeToCluster().get(v))) {
						bV = false;
						break;
					} else {
						Vector<BitSet> tiClusters = new Vector<BitSet>();
						cmpClusters(trees[i].getClusterToNode().get(trees[0].getNodeToCluster().get(v)), tiClusters);
						if (t0Clusters.size() != tiClusters.size()) {
							bV = false;
							break;
						} else {
							for (BitSet t0Cluster : t0Clusters) {
								if (!tiClusters.contains(t0Cluster)) {
									bV = false;
									break;
								}
							}
						}
					}

				}

				if (bAll) {
					if (!bV) {
						for (MyNode c : children)
							addSubtree(trees[0].getNodeToCluster().get(c), commonSubtrees, taxaOrdering.size());
					} else if (trees[0].getRoot().equals(v)) {
						addSubtree(trees[0].getNodeToCluster().get(v), commonSubtrees, taxaOrdering.size());
					} else if (bV)
						commonNodes.add(v);
				} else {
					for (MyNode c : children) {
						if (b.get(children.indexOf(c)))
							addSubtree(trees[0].getNodeToCluster().get(c), commonSubtrees, taxaOrdering.size());
					}
				}
			} else {
				// at the beginning each leaf is a possible root of a common
				// subtree
				commonNodes.add(v);
			}
		}

		for (HybridTree t : trees)
			taxaOrdering = t.replaceClusters(commonSubtrees, rI);

		return taxaOrdering;

	}

	private BitSet cmpClusters(MyNode v, Vector<BitSet> clusters) {
		if (v.getOutDegree() == 0) {
			BitSet b = new BitSet(taxaOdering.size());
			b.set(taxaOdering.indexOf(v.getLabel()));
			clusters.add(b);
			return b;
		} else {
			BitSet b = new BitSet(taxaOdering.size());
			Iterator<MyEdge> it = v.getOutEdges();
			while (it.hasNext())
				b.or(cmpClusters(it.next().getTarget(), clusters));
			clusters.add(b);
			return b;
		}
	}

	private void addSubtree(BitSet b, Vector<BitSet> v, int size) {
		// common subtree must not be a single leaf or the whole tree
		if (b.cardinality() > 1 && b.cardinality() < size && !v.contains(b))
			v.add(b);
	}
}
