package dendroscope.hybroscale.util.sparseGraph;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

public class MySparseNode {

	private MySparseNode parent;
	private Vector<MySparseNode> children = new Vector<MySparseNode>();

	private BitSet cluster;
	private MySparseGraph owner;
	private Object info;
	private String label;

	public MySparseNode(MySparseGraph owner, String label) {
		this.owner = owner;
		this.label = label;
	}
	

	public void setParent(MySparseNode p) {
		parent = p;
	}
	
	public MySparseNode getParent() {
		return parent;
	}

	public void addChild(MySparseNode v) {
		children.add(v);
	}

	public Vector<MySparseNode> getChildren() {
		return children;
	}
	
	public void removeChild(MySparseNode v) {
		children.remove(v);
	}

	public MySparseGraph getOwner() {
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

	public String getLabel() {
		return label;
	}

	public void setLabel(String s) {
		label = s;
	}

	public void setOwner(MySparsePhyloTree t) {
		owner = t;
	}

	public Object getInfo() {
		return info;
	}

	public void setInfo(Object info) {
		this.info = info;
	}

	public BitSet getCluster() {
		return cluster;
	}

	public void setCluster(BitSet cluster) {
		this.cluster = cluster;
	}

	public String toNewick(String newickString, Vector<MySparseNode> visitedNodes, Hashtable<MySparseNode, String> nodeToHTag) {
		if (!visitedNodes.contains(this)) {
			visitedNodes.add(this);
			if (children.isEmpty() && parent == null)
				return "(" + getLabelString() + ")";
			else if (children.isEmpty()) {
				return getLabelString();
			} else {
				String subString = "(";
				int counter = 0;
				for (MySparseNode c : children) {
					counter++;
					String hTag = "";
					if (nodeToHTag.containsKey(c))
						hTag = nodeToHTag.get(c) + ":0.0";
					if (counter == children.size())
						subString = subString.concat(c.toNewick(newickString, visitedNodes, nodeToHTag) + hTag + ")");
					else
						subString = subString.concat(c.toNewick(newickString, visitedNodes, nodeToHTag) + hTag + ",");
				}
				return subString + getLabelString();
			}
		} else
			return getLabelString();
	}

	private String getLabelString() {
		if (label == null)
			return "";
		return label;
	}

}
