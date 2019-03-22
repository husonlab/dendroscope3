package dendroscope.hybroscale.model.util;

import dendroscope.hybroscale.model.treeObjects.HybridNetwork;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

import java.util.*;

public class ComputeNodeWeights {

	private Hashtable<BitSet, Integer> clusterToNumber = new Hashtable<BitSet, Integer>();
	private Hashtable<MyNode, BitSet> nodeToCluster = new Hashtable<MyNode, BitSet>();
	private Hashtable<MyPhyloTree, Double> networkToOcc = new Hashtable<MyPhyloTree, Double>();

	private Vector<HybridNetwork> networks;
	private Vector<String> taxaOrdering;
	private int numberOfTrees;

	public ComputeNodeWeights(Vector<HybridNetwork> networks, int numberOfTrees, Vector<String> taxaOrdering) {
		this.networks = networks;
		this.taxaOrdering = taxaOrdering;
		this.numberOfTrees = numberOfTrees;
	}

	public void computeOcurrences() {
		
		int networksFactor = 0;
		for (HybridNetwork n : networks) {
			Vector<BitSet> hybridClusters = new Vector<BitSet>();
			int netFactor = 1;
			Iterator<MyNode> it = n.getNodes().iterator();
			while (it.hasNext()) {
				MyNode v = it.next();
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

		for (HybridNetwork n : networks) {
			double sumOcc = 0;
			Iterator<MyNode> it = n.nodeIterator();
			while (it.hasNext()) {
				MyNode v = it.next();
				if (v.getInDegree() > 1 && !isClusterNode(v)) {
					if (getWeight(n, v) != null) {
						double w = getWeight(n, v);
						double s = networksFactor;
						double occProc = (w / s) * 100;
						occProc = Math.round(occProc * 100) / 100.;
						v.setLabel((int) Math.floor(occProc) + "%");
						sumOcc += occProc;
					}
				}
			}
			networkToOcc.put(n, sumOcc);
		}
		
//		Collections.sort(networks, new NetworkSorter());
	}

	private boolean isClusterNode(MyNode v) {
        MyEdge outEdge = v.getFirstOutEdge();
		if (((HashSet<Integer>) outEdge.getInfo()).contains(-1))
			return true;
		return false;
	}

	private BitSet computeReticulationCluster(MyPhyloTree n, MyNode v) {

		Vector<BitSet> clusters = new Vector<BitSet>();
		for (int treeIndex = 0; treeIndex < numberOfTrees; treeIndex++) {
			BitSet cluster = new BitSet(taxaOrdering.size());
			computeClusterRec(n, v, cluster, treeIndex);
			clusters.add(cluster);
		}

		BitSet retCluster = new BitSet(clusters.size() * taxaOrdering.size());
		for (BitSet c : clusters) {
			int bitIndex = c.nextSetBit(0);
			while (bitIndex != -1) {
				retCluster.set(bitIndex + clusters.indexOf(c) * taxaOrdering.size());
				bitIndex = c.nextSetBit(bitIndex + 1);
			}
		}

		nodeToCluster.put(v, retCluster);

		return retCluster;
	}

	private void computeClusterRec(MyPhyloTree n, MyNode v, BitSet cluster, int treeIndex) {
		if (v.getOutDegree() == 0)
			cluster.set(taxaOrdering.indexOf(n.getLabel(v)));
		else {
			Iterator<MyEdge> it = n.getOutEdges(v);
			while (it.hasNext()) {
				MyEdge e = it.next();
				HashSet<Integer> indices = (HashSet<Integer>) e.getInfo();
				if (indices.contains(treeIndex)) {
					MyNode c = e.getTarget();
					computeClusterRec(n, c, cluster, treeIndex);
				}
			}
		}
	}

	public Integer getWeight(HybridNetwork n, MyNode v) {
		if (nodeToCluster.containsKey(v)) {
			BitSet b = nodeToCluster.get(v);
			if (clusterToNumber.containsKey(b))
				return clusterToNumber.get(b);
		}
		return null;
	}

	public class NetworkSorter implements Comparator<MyPhyloTree> {

		@Override
		public int compare(MyPhyloTree t1, MyPhyloTree t2) {
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
