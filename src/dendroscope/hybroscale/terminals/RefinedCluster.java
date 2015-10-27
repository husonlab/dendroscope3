package dendroscope.hybroscale.terminals;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import dendroscope.hybroscale.util.graph.MyNode;

public class RefinedCluster {
	
	private HashMap<BitSet,HashSet<MyNode>> clusterToNode = new HashMap<BitSet,HashSet<MyNode>>();
	private int size;
	
	public RefinedCluster(int size) {
		this.size = size;
	}

	public void addCluster(BitSet cluster, HashSet<MyNode> hashSet){
		clusterToNode.put(cluster, hashSet);
	}
	
	public Set<BitSet> getAllCluster(){
		return clusterToNode.keySet();
	}
	
	public HashSet<MyNode> getNodes(BitSet cluster){
		return clusterToNode.get(cluster);
	}
	
	public boolean isEmpty(){
		return clusterToNode.isEmpty();
	}

	public int getSize() {
		return size;
	}
	
}
