package dendroscope.hybroscale.model.treeObjects;

import java.util.Vector;

public class SparseTreeNode {

    private Vector<SparseTreeNode> children = new Vector<SparseTreeNode>();

    private SparseTreeNode parent;
    private SparseTree owner;
    private String label;
    
    private boolean solid = false;
    
    public SparseTreeNode(SparseTreeNode parent, SparseTree owner, String label) {
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

    public Vector<SparseTreeNode> getChildren() {
        return children;
    }

    public SparseTreeNode getParent() {
        return parent;
    }

    public String getLabel() {
        return label;
    }

    public SparseTree getOwner() {
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

    public void addChild(SparseTreeNode v) {
        children.add(v);
        v.setParent(this);
    }

    public void removeChild(SparseTreeNode v) {
        children.remove(v);
        v.setParent(null);
    }

    public void delete() {
        if (children.size() == 0)
            owner.removeLeaf(this);
        for (SparseTreeNode c : children)
            c.delete();   
    }

    public void setLabel(String s) {
        label = s;
    }

    public void setParent(SparseTreeNode p) {
        parent = p;
    }

    public void restrict() {
        if (parent != null) {
            SparseTreeNode p = parent;
            p.removeChild(this);
            for (SparseTreeNode c : children) {
                p.addChild(c);
                c.setParent(p);
            }
            if (p.getOutDegree() == 0)
                owner.addLeaf(p);
        } else if (children.size() == 1) {
            children.firstElement().setParent(null);
            owner.setRoot(children.firstElement());
        }
    }

    public void setOwner(SparseTree t) {
        owner = t;
    }

	public void setSolid(boolean b) {
		solid = true;
	}

	public boolean isSolid() {
		return solid;
	}

}
