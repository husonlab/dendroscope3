package dendroscope.hybroscale.util;

import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

public class ClusterManager {

	private MyPhyloTree[] trees;
	private Vector<String> taxaOrdering;

	public ClusterManager(MyPhyloTree[] trees) {
		this.trees = trees;
		taxaOrdering = new Vector<String>();
		for (MyNode leaf : trees[0].getLeaves())
			taxaOrdering.add(leaf.getLabel());
		Collections.sort(taxaOrdering);
	}

	public MyPhyloTree run() {

		MyPhyloTree clusterNet = new MyPhyloTree();

		Vector<BitSet> clusterSet = computeClusters();
		BitSet rootCluster = new BitSet(taxaOrdering.size());
		for (int i = 0; i < taxaOrdering.size(); i++)
			rootCluster.set(i);
		if (!clusterSet.contains(rootCluster))
			clusterSet.add(rootCluster);

		HashMap<MyNode, BitSet> nodeToCluster = new HashMap<MyNode, BitSet>();
		HashMap<BitSet, MyNode> clusterToNode = new HashMap<BitSet, MyNode>();
		for (BitSet cluster : clusterSet) {
			MyNode v = clusterNet.newNode();
			nodeToCluster.put(v, cluster);
			clusterToNode.put(cluster, v);
			if (cluster.cardinality() == 1)
				v.setLabel(taxaOrdering.get(cluster.nextSetBit(0)));
			if (cluster.equals(rootCluster))
				clusterNet.setRoot(v);
		}

		Collections.sort(clusterSet, new ClusterComparator());			
		for (BitSet cluster : clusterSet) {
			if (!cluster.equals(rootCluster)) {
				Vector<MyNode> markedNodes = new Vector<MyNode>();
				Stack<BitSet> stack = new Stack<BitSet>();
				stack.push(rootCluster);
				markedNodes.add(clusterToNode.get(rootCluster));
				while (!stack.isEmpty()) {
					MyNode v = clusterToNode.get(stack.pop());
					boolean isBelow = false;
					Iterator<MyEdge> it = v.getOutEdges();
					while (it.hasNext()) {
						MyNode w = it.next().getTarget();
						BitSet wCluster = nodeToCluster.get(w);
						BitSet clusterCopy = (BitSet) cluster.clone();
						clusterCopy.and(wCluster);
						if (clusterCopy.equals(cluster) && cluster.cardinality() < wCluster.cardinality()) {
							isBelow = true;
							if (!markedNodes.contains(w)){
								stack.push(wCluster);
								markedNodes.add(w);
							}
						}
					}
					if (!isBelow) 
						clusterNet.newEdge(v, clusterToNode.get(cluster));	
				}
			}
		}
		
		Vector<MyNode> retNodes = new Vector<MyNode>();
		for(MyNode v : clusterNet.getNodes()){
			if(v.getInDegree() > 1)
				retNodes.add(v);
		}
		for(MyNode v : retNodes){
			MyNode vPrime = clusterNet.newNode();
			Vector<MyEdge> inEdges = new Vector<MyEdge>();
			Iterator<MyEdge> it = v.getInEdges();
			while(it.hasNext())
				inEdges.add(it.next());
			for(MyEdge e : inEdges){
				clusterNet.newEdge(e.getSource(), vPrime);
				clusterNet.deleteEdge(e);
			}
			clusterNet.newEdge(vPrime, v);
		}

		return clusterNet;
	}

	public class ClusterComparator implements Comparator<BitSet> {
		@Override
		public int compare(BitSet b1, BitSet b2) {
			if (b1.cardinality() > b2.cardinality())
				return -1;
			else if (b1.cardinality() < b2.cardinality())
				return 1;
			return 0;
		}
	}

	private Vector<BitSet> computeClusters() {
		Vector<BitSet> clusterSet = new Vector<BitSet>();
		for (MyPhyloTree t : trees)
			cmpClustersRec(t.getRoot(), clusterSet);
		return clusterSet;
	}

	private BitSet cmpClustersRec(MyNode v, Vector<BitSet> clusterSet) {
		if (v.getOutDegree() == 0) {
			BitSet b = new BitSet(taxaOrdering.size());
			b.set(taxaOrdering.indexOf(v.getLabel()));
			if (!clusterSet.contains(b))
				clusterSet.add(b);
			return b;
		} else {
			BitSet b = new BitSet(taxaOrdering.size());
			Iterator<MyEdge> it = v.getOutEdges();
			while (it.hasNext()) {
				MyNode child = it.next().getTarget();
				b.or(cmpClustersRec(child, clusterSet));
			}
			if (!clusterSet.contains(b))
				clusterSet.add(b);
			return b;
		}
	}
}