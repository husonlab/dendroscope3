package dendroscope.hybroscale.model.treeObjects;

import java.util.HashSet;
import java.util.Vector;

public class SparseNetNode {

	private Vector<SparseNetEdge> inEdges = new Vector<SparseNetEdge>();
	private Vector<SparseNetEdge> outEdges = new Vector<SparseNetEdge>();

	private SparseNetwork owner;
	private String label;
	private Integer order;

	public SparseNetNode(SparseNetNode parent, SparseNetwork owner, String label) {
		this.owner = owner;
		this.label = label;
		if (owner != null) {
			owner.addLeaf(this);
		}
		if (parent != null) 
			parent.addChild(this);	
		if (owner != null && parent != null) {
			if (owner.getLeaves().contains(parent)) 
				owner.removeLeaf(parent);	
		}
	}
	
	public SparseNetNode(SparseNetNode parent, SparseNetwork owner, String label, Object info) {
		this.owner = owner;
		this.label = label;
		if (owner != null) {
			owner.addLeaf(this);
		}
		if (parent != null) 
			parent.addChild(this, info);	
		if (owner != null && parent != null) {
			if (owner.getLeaves().contains(parent)) 
				owner.removeLeaf(parent);	
		}
	}
	
	public void removeOutEdge(SparseNetEdge e) {
		SparseNetNode child = e.getTarget();
		outEdges.remove(e);
		child.removeInEdge(e);
		owner.removeEdge(e);
	}
	
	public void removeAllChildren() {
		for(SparseNetEdge e : outEdges){
			SparseNetNode child = e.getTarget();
			child.removeInEdge(e);
			owner.removeEdge(e);
		}
		outEdges.clear();
	}
	
	private void removeInEdge(SparseNetEdge e) {
		inEdges.remove(e);
	}

	public SparseNetEdge addChild(SparseNetNode child) {
		SparseNetEdge e = new SparseNetEdge(this, child);
		outEdges.add(e);
		owner.addEdges(e);
		child.addParent(this, e);
		if(owner.getLeaves().contains(this))
			owner.removeLeaf(this);
		return e;
	}
	
	public SparseNetEdge addChild(SparseNetNode child, Object info) {
		SparseNetEdge e = new SparseNetEdge(this, child);
		if(info instanceof HashSet<?>)
			e.addEdgeIndices((HashSet<Integer>) info);
		outEdges.add(e);
		owner.addEdges(e);
		child.addParent(this, e);
		if(owner.getLeaves().contains(this))
			owner.removeLeaf(this);
		return e;
	}

	private SparseNetEdge addParent(SparseNetNode parent, SparseNetEdge e) {
		inEdges.add(e);
		return e;
	}
	
	public SparseNetwork getOwner() {
		return owner;
	}
	
	public int getOutDegree() {
		return outEdges.size();
	}

	public int getInDegree() {
		return inEdges.size();
	}
	
	public Vector<SparseNetEdge> getOutEdges() {
		return outEdges;
	}

	public Vector<SparseNetEdge> getInEdges() {
		return inEdges;
	}

	public String getLabel() {
		return label;
	}
	
	public void setLabel(String s) {
		label = s;
	}

	public void setOwner(SparseNetwork t) {
		owner = t;
	}

	public Integer getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

}
