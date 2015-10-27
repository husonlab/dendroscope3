package dendroscope.hybroscale.util.sparseGraph;

import dendroscope.hybroscale.util.graph.MyNode;

public class MySparseGraph {
	
	public MySparseNode newNode(MyNode v) {
		MySparseNode vCopy = new MySparseNode(this, v.getLabel());
		vCopy.setInfo(v.getInfo());

		return vCopy;
	}

	public MySparseNode newNode() {
		MySparseNode newNode = new MySparseNode(this, null);
		return newNode;
	}

	public void newEdge(MySparseNode v, MySparseNode w) {
		v.addChild(w);
		w.setParent(v);
	}

	public void deleteNode(MySparseNode v) {
		if (v.getOwner().equals(this)) {
			for(MySparseNode c : v.getChildren())
				c.setParent(null);
			MySparseNode p = v.getParent();
			if(p != null)
				p.removeChild(v);
			v.setParent(null);
		} else
			throw new RuntimeException("Wrong Owner Exception");
	}

	public void setLabel(MySparseNode v, String label) {
		if (v.getOwner().equals(this))
			v.setLabel(label);
		else
			throw new RuntimeException("Wrong Owner Exception");
	}

	public void setInfo(MySparseNode v, Object info) {
		if (v.getOwner().equals(this))
			v.setInfo(info);
		else
			throw new RuntimeException("Wrong Owner Exception");
	}

}
