package dendroscope.hybroscale.model.cmpMinNetworks;

import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.model.treeObjects.SparseNetEdge;
import dendroscope.hybroscale.model.treeObjects.SparseNetNode;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;

import java.util.*;

public class NetworkToTreeAdaptor {

	private Vector<SparseNetEdge> treeEdges;
	private Vector<String> taxaOrdering;
	private HashMap<SparseNetNode, BitSet> nodeToCluster;

	private int counter = 0;


	public void run(SparseNetwork nCopy, HybridTree t, Vector<SparseNetEdge> treeEdges, Vector<String> taxaOrdering) {

		this.treeEdges = treeEdges;
		this.taxaOrdering = taxaOrdering;
		nodeToCluster = new HashMap<SparseNetNode, BitSet>();
		
		for(SparseNetNode v : nCopy.getNodes())
			nodeToCluster.put(v, new BitSet(taxaOrdering.size()));
		initClusters(nCopy.getRoot());
		
		resolveNetwork(nCopy, nCopy.getRoot(), t);

	}

	private void resolveNetwork(SparseNetwork nCopy, SparseNetNode vNet, HybridTree t) {
		
		if (vNet.getOutDegree() > 2 && !nodeToCluster.get(vNet).isEmpty()) {
			
			HashMap<BitSet, Vector<SparseNetNode>> childBuckets = new HashMap<BitSet, Vector<SparseNetNode>>();
			MyNode vTree = t.getClusterToNode().get(nodeToCluster.get(vNet));

			if (vTree != null) {

                Iterator<MyEdge> it = vTree.outEdges().iterator();
				while (it.hasNext()) {
					BitSet cTreeCluster = t.getNodeToCluster().get(it.next().getTarget());
					childBuckets.put(cTreeCluster, new Vector<SparseNetNode>());
				}

				// filling buckets
				for (SparseNetEdge e : getOutTreeEdges(vNet)) {
					SparseNetNode cNet = e.getTarget();
					BitSet cNetCluster = nodeToCluster.get(cNet);
					if (!cNetCluster.isEmpty()) {
						for (BitSet bucket : childBuckets.keySet()) {
							BitSet b = (BitSet) bucket.clone();
							b.and(cNetCluster);
							if (b.equals(cNetCluster))
								childBuckets.get(bucket).add(cNet);
						}
					}
				}

				SparseNetwork nTemp1 = new SparseNetwork(nCopy);

				// refining network
				for (BitSet bucket : childBuckets.keySet()) {
					Vector<SparseNetNode> children = childBuckets.get(bucket);
					if (children.size() > 1) {
						SparseNetNode newNode = new SparseNetNode(vNet, nCopy, "");
						SparseNetEdge e1 = newNode.getInEdges().get(0);
						treeEdges.add(e1);
						nodeToCluster.put(newNode, bucket);

						for (SparseNetNode cNet : children) {
							SparseNetEdge e = getInTreeEdges(cNet);
							vNet.removeOutEdge(e);
							boolean b = treeEdges.remove(e);
							SparseNetEdge e2 = newNode.addChild(cNet);
							e2.addIndices((HashSet<Integer>) e.getIndices().clone());
							e1.addIndices((HashSet<Integer>) e.getIndices().clone());
							if (b)
								treeEdges.add(e2);
						}

					}
				}

				for (SparseNetEdge e : getOutTreeEdges(vNet))
					resolveNetwork(nCopy, e.getTarget(), t);

			} else if (getInTreeEdges(vNet).getSource().getInDegree() == 1) {

				vTree = t.findLCA(nodeToCluster.get(vNet));

				Iterator<MyEdge> it = vTree.getOutEdges();
				while (it.hasNext()) {
					BitSet cTreeCluster = t.getNodeToCluster().get(it.next().getTarget());
					childBuckets.put(cTreeCluster, new Vector<SparseNetNode>());
				}

				// filling buckets
				for (SparseNetEdge e : getOutTreeEdges(vNet)) {
					SparseNetNode cNet = e.getTarget();
					BitSet cNetCluster = nodeToCluster.get(cNet);
					for (BitSet bucket : childBuckets.keySet()) {
						BitSet b = (BitSet) bucket.clone();
						b.and(cNetCluster);
						if (b.equals(cNetCluster))
							childBuckets.get(bucket).add(cNet);
					}
				}

				// refining network
				Vector<SparseNetEdge> newEdges = new Vector<SparseNetEdge>();
				SparseNetNode vNetParent = getInTreeEdges(vNet).getSource();
				for (BitSet bucket : childBuckets.keySet()) {
					Vector<SparseNetNode> children = childBuckets.get(bucket);
					for (SparseNetNode cNet : children) {
						SparseNetEdge e = getInTreeEdges(cNet);
						vNet.removeOutEdge(e);
						boolean b = treeEdges.remove(e);
						SparseNetEdge e2 = vNetParent.addChild(cNet);
						e2.addIndices((HashSet<Integer>) e.getIndices().clone());
						newEdges.add(e2);
						if (b)
							treeEdges.add(e2);
					}
				}
				if (vNet.getOutDegree() == 0)
					vNetParent.removeOutEdge(getInTreeEdges(vNet));
				else if (vNet.getOutDegree() == 1) {
					SparseNetNode cNet = vNet.getOutEdges().get(0).getTarget();
					SparseNetEdge e = getInTreeEdges(cNet);
					vNet.removeOutEdge(e);
					boolean b = treeEdges.remove(e);
					SparseNetEdge e2 = vNetParent.addChild(cNet);
					e2.addIndices((HashSet<Integer>) e.getIndices().clone());
					newEdges.add(e2);
					if (b)
						treeEdges.add(e2);
					vNetParent.removeOutEdge(getInTreeEdges(vNet));
				}

				resolveNetwork(nCopy, vNetParent, t);
				
			} else {
				Vector<SparseNetEdge> newEdges = new Vector<SparseNetEdge>();
				for (SparseNetEdge e : vNet.getOutEdges())
					newEdges.add(e);
				for (SparseNetEdge e : newEdges)
					resolveNetwork(nCopy, e.getTarget(), t);
			}
		} else {
			Vector<SparseNetEdge> newEdges = new Vector<SparseNetEdge>();
			for (SparseNetEdge e : vNet.getOutEdges())
				newEdges.add(e);
			for (SparseNetEdge e : newEdges)
				resolveNetwork(nCopy, e.getTarget(), t);
		}

	}

	private SparseNetEdge getInTreeEdges(SparseNetNode v) {
		if (v.getInDegree() == 1)
			return v.getInEdges().get(0);
		for (SparseNetEdge e : v.getInEdges()) {
			if (treeEdges.contains(e))
				return e;
		}
		return null;
	}

	public Vector<SparseNetEdge> getOutTreeEdges(SparseNetNode v) {
		Vector<SparseNetEdge> outEdges = new Vector<SparseNetEdge>();
		for (SparseNetEdge e : v.getOutEdges()) {
			if (e.getTarget().getInDegree() > 1 && treeEdges.contains(e))
				outEdges.add(e);
			else if (e.getTarget().getInDegree() == 1 && e.getTarget().getOutDegree() != 1)
				outEdges.add(e);
			else if (e.getTarget().getInDegree() == 1 && e.getTarget().getOutDegree() == 1
					&& treeEdges.contains(e.getTarget().getOutEdges().get(0)))
				outEdges.add(e);
		}
		return outEdges;
	}

	private BitSet initClusters(SparseNetNode v) {

		if (v.getLabel() == null || v.getLabel().equals(""))
			v.setLabel(String.valueOf(++counter));

		if (v.getOutDegree() == 0) {
			BitSet cluster = new BitSet(taxaOrdering.size());
			cluster.set(taxaOrdering.indexOf(v.getLabel()));
			nodeToCluster.put(v, cluster);
			return cluster;
		} else {
			BitSet cluster = new BitSet(taxaOrdering.size());
			for (SparseNetEdge e : getOutTreeEdges(v))
				cluster.or(initClusters(e.getTarget()));
			nodeToCluster.put(v, cluster);
			return cluster;
		}
	}

}
