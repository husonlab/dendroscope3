/*
 *   MySparseNode.java Copyright (C) 2020 Daniel H. Huson
 *
 *   (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dendroscope.hybroscale.util.sparseGraph;

import java.util.BitSet;
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
