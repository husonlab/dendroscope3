package dendroscope.hybroscale.model.cmpAllMAAFs;

import java.util.Iterator;
import java.util.Vector;

public class EasyNode {

	private Vector<EasyNode> children = new Vector<EasyNode>();

	private EasyNode parent;
	private EasyTree owner;
	private String label;

	private Object info;
	private EasyNode[] contractedNodes;
	
	private boolean solid = false;
	private boolean isAddedNode = false;

	public EasyNode(EasyNode parent, EasyTree owner, String label) {
		this.parent = parent;
		this.owner = owner;
		this.label = label;
		if (owner != null) {
			owner.addLeaf(this);
			owner.setTreeChanged(true);
		}
		if (parent != null)
			this.parent.addChild(this);
		if (owner != null && parent != null) {
			if (owner.getLeaves().contains(parent)) {
				owner.removeLeaf(parent);
				owner.setTreeChanged(true);
			}
		}
	}

	public Vector<EasyNode> getChildren() {
		return children;
	}

	public EasyNode getParent() {
		return parent;
	}

	public String getLabel() {
		return label;
	}

	public EasyTree getOwner() {
		return owner;
	}

	public int getOutDegree() {
		return children.size();
	}

	public int getInDegree() {
		if (parent != null)
			return 1;
		return 0;
	}

	public void addChild(EasyNode v) {
		// v.getOwner().setTreeChanged(true);
		children.add(v);
		v.setParent(this);
	}

	public void addSubtree(EasyTree t) {
		owner.removeLeaf(this);
		for (EasyNode l : t.getLeaves())
			owner.addLeaf(l);
		children.add(t.getRoot());
		t.getRoot().setParent(this);
		owner.setTreeChanged(true);
		for(EasyNode v : owner.getNodes())
			v.setOwner(owner);
	}

	public void removeChild(EasyNode v) {
		children.remove(v);
		v.setParent(null);
	}

	public void delete() {
		if (children.size() == 0)
			owner.removeLeaf(this);
		for (EasyNode c : children)
			c.delete();
	}

	public void setLabel(String s) {
		label = s;
	}

	public void setParent(EasyNode p) {
		parent = p;
	}

	public void restrict() {
		if (parent != null) {
			EasyNode p = parent;
			p.removeChild(this);
			for (EasyNode c : children) {
				p.addChild(c);
				c.setParent(p);
				c.setSolid(solid);
			}
			if (p.getOutDegree() == 0)
				owner.addLeaf(p);
		} else if (children.size() == 1) {
			children.firstElement().setParent(null);
			owner.setRoot(children.firstElement());
		}
	}

	public void setOwner(EasyTree t) {
		owner = t;
	}

	public Object getInfo() {
		return info;
	}

	public void setInfo(Object info) {
		this.info = info;
	}

	public EasyNode[] getContractedNodes() {
		return contractedNodes;
	}

	public void setContractedNodes(EasyNode[] contractedNodes) {
		if (contractedNodes != null) {
			for (EasyNode v : contractedNodes) {
				Iterator<EasyNode> it = ((Vector<EasyNode>) v.getChildren().clone()).iterator();
				while (it.hasNext())
					v.removeChild(it.next());
			}
		}
		this.contractedNodes = contractedNodes;
	}

	public void setAddedNode(boolean b) {
		isAddedNode = b;
	}

	public boolean isAddedNode() {
		return isAddedNode;
	}
	
	public boolean isSolid() {
		return solid;
	}
	
	public void setSolid(boolean solid) {
		this.solid = solid;
	}

}
