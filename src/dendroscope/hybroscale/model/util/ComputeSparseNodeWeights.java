package dendroscope.hybroscale.model.util;

import dendroscope.hybroscale.model.treeObjects.SparseNetEdge;
import dendroscope.hybroscale.model.treeObjects.SparseNetNode;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;

import java.util.*;

public class ComputeSparseNodeWeights {

	private Hashtable<BitSet, Integer> clusterToNumber = new Hashtable<BitSet, Integer>();
	private Hashtable<SparseNetNode, BitSet> nodeToCluster = new Hashtable<SparseNetNode, BitSet>();
	private Hashtable<SparseNetwork, Integer> networkToOcc = new Hashtable<SparseNetwork, Integer>();

	private Vector<SparseNetwork> networks;
	private Vector<String> taxaOrdering;
	private int numberOfTrees;

	public ComputeSparseNodeWeights(Vector<SparseNetwork> networks, int numberOfTrees, Vector<String> taxaOrdering) {
		this.networks = networks;
		this.taxaOrdering = taxaOrdering;
		this.numberOfTrees = numberOfTrees;
	}

	public void computeOcurrences() {

		int networksFactor = 0;
		for (SparseNetwork n : networks) {
			Vector<BitSet> hybridClusters = new Vector<BitSet>();
			int netFactor = 1;
			Iterator<SparseNetNode> it = n.getNodes().iterator();
			while (it.hasNext()) {
				SparseNetNode v = it.next();
				if (v.getInDegree() > 1) {
					BitSet cluster = computeReticulationCluster(n, v);
					hybridClusters.add(cluster);
					netFactor *= v.getInDegree() - 1;
				}
			}
			for (BitSet cluster : hybridClusters) {
				if (clusterToNumber.containsKey(cluster)) {
					int num = clusterToNumber.get(cluster) + netFactor;
					clusterToNumber.remove(cluster);
					clusterToNumber.put(cluster, num);
				} else
					clusterToNumber.put(cluster, netFactor);
			}
			networksFactor += netFactor;
		}

		for (SparseNetwork n : networks) {
			int sumOcc = 0;
			Iterator<SparseNetNode> it = n.getNodes().iterator();
			while (it.hasNext()) {
				SparseNetNode v = it.next();
				if (v.getInDegree() > 1) {
					if (getNodeWeight(n, v) != null) {
						double w = getNodeWeight(n, v);
						double s = networksFactor;
						double occProc = (w / s) * 100;
						occProc = Math.round(occProc * 100 / 100.);
						v.setLabel(occProc + "%");
						sumOcc += Math.round(occProc);
					}
				}
			}
			networkToOcc.put(n, sumOcc);
		}
		
		Collections.sort(networks, new NetworkSorter());
	}

	private BitSet computeReticulationCluster(SparseNetwork n, SparseNetNode v) {

		Vector<BitSet> clusters = new Vector<BitSet>();
		for (int treeIndex = 0; treeIndex < numberOfTrees; treeIndex++) {
			BitSet cluster = new BitSet(taxaOrdering.size());
			computeClusterRec(n, v, cluster, treeIndex);
			clusters.add(cluster);
		}

		BitSet b = new BitSet(clusters.size() * taxaOrdering.size());
		for (BitSet c : clusters) {
			int bitIndex = c.nextSetBit(0);
			while (bitIndex != -1) {
				b.set(bitIndex + clusters.indexOf(c) * taxaOrdering.size());
				bitIndex = c.nextSetBit(bitIndex + 1);
			}
		}

		nodeToCluster.put(v, b);

		return b;
	}

	private void computeClusterRec(SparseNetwork n, SparseNetNode v, BitSet cluster, int treeIndex) {
		if (v.getOutDegree() == 0)
			cluster.set(taxaOrdering.indexOf(v.getLabel()));
		else {
            Iterator<SparseNetEdge> it = v.outEdges().iterator();
			while (it.hasNext()) {
				SparseNetEdge e = it.next();
				HashSet<Integer> indices = e.getIndices();
				if (indices.contains(treeIndex)) {
					SparseNetNode c = e.getTarget();
					computeClusterRec(n, c, cluster, treeIndex);
				}
			}
		}
	}

	private Integer getNodeWeight(SparseNetwork n, SparseNetNode v) {
		if (nodeToCluster.containsKey(v)) {
			BitSet b = nodeToCluster.get(v);
			if (clusterToNumber.containsKey(b))
				return clusterToNumber.get(b);
		}
		return null;
	}

	public Integer getNetWeight(SparseNetwork n) {
		return networkToOcc.get(n);
	}

	public class NetworkSorter implements Comparator<SparseNetwork> {

		@Override
		public int compare(SparseNetwork t1, SparseNetwork t2) {
			double occ1 = networkToOcc.get(t1);
			double occ2 = networkToOcc.get(t2);
			if (occ1 > occ2)
				return -1;
			else if (occ1 < occ2)
				return 1;
			return 0;
		}

	}

}
