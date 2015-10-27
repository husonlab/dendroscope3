package dendroscope.hybroscale.terminals;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import dendroscope.hybroscale.util.sparseGraph.MySparseNode;

public class RefinedCluster_Sparse {
	
	private HashMap<BitSet,Vector<MySparseNode>> clusterToNode = new HashMap<BitSet,Vector<MySparseNode>>();
	private int size;
	
	public RefinedCluster_Sparse(int size) {
		this.size = size;
	}

	public void addCluster(BitSet cluster, Vector<MySparseNode> hashSet){
		clusterToNode.put(cluster, hashSet);
	}
	
	public Set<BitSet> getAllCluster(){
		return clusterToNode.keySet();
	}
	
	public Vector<MySparseNode> getNodes(BitSet cluster){
		return clusterToNode.get(cluster);
	}
	
	public boolean isEmpty(){
		return clusterToNode.isEmpty();
	}

	public int getSize() {
		return size;
	}
	
}
