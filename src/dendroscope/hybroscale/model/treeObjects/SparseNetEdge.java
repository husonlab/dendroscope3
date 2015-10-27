package dendroscope.hybroscale.model.treeObjects;

import java.util.HashSet;
import java.util.Vector;

public class SparseNetEdge {
	
	private HashSet<Integer> edgeIndex = new HashSet<Integer>();
	private HashSet<Integer> indices = new HashSet<Integer>();
	private SparseNetNode source, target;
	
	private boolean solid = false;

	public SparseNetEdge(SparseNetNode source, SparseNetNode target) {
		this.source = source;
		this.target = target;
	}

	public SparseNetNode getSource() {
		return source;
	}

	public SparseNetNode getTarget() {
		return target;
	}
	
	public void addIndices(HashSet<Integer> newIndices) {
		indices.addAll(newIndices);
	}
	
	public void addIndices(Vector<Integer> newIndices) {
		indices.addAll(newIndices);
	}
	
	public void addIndex(int newIndex) {
		indices.add(newIndex);
	}
	
	public void removeIndex(int index) {
		indices.remove(index);
	}
	
	public HashSet<Integer> getIndices() {
		return indices;
	}
	
	public void removeEdgeIndex(int index){
		edgeIndex.remove(index);
	}
	
	public void addEdgeIndex(int index){
		edgeIndex.add(index);
	}
	
	public void addEdgeIndices(HashSet<Integer> indices){
		edgeIndex.addAll(indices);
	}

	public HashSet<Integer> getEdgeIndex() {
		return edgeIndex;
	}

	public void setEdgeIndex(HashSet<Integer> edgeIndex) {
		this.edgeIndex = edgeIndex;
	}

	public String toMyString() {
		return getSource().getLabel()+" -> "+getTarget().getLabel();
	}

	public void setSolid(boolean b) {
		solid = b;
	}

	public boolean isSolid() {
		return solid;
	}

}
