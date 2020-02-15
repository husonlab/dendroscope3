/*
 *   MySparsePhyloTree.java Copyright (C) 2020 Daniel H. Huson
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

import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class MySparsePhyloTree extends MySparseGraph {

	private String info;
	private String name;
	private MySparseNode root;

	public MySparsePhyloTree() {
		root = new MySparseNode(this, null);
//		addLeaf(root);
	}

	public MySparsePhyloTree(MySparseNode root) {
		this.root = root;
		root.setOwner(this);
//		addLeaf(root);
	}

	public MySparsePhyloTree copy(MyPhyloTree t) {
		return new MySparsePhyloTree(t);
	}

	public MySparsePhyloTree(MyPhyloTree t) {
		MyNode v = t.getRoot();
		root = new MySparseNode(this, v.getLabel());
		root.setLabel(v.getLabel());
		root.setInfo(v.getInfo());
//		addLeaf(root);
		copyTreeRec(t, v, root);
		name = t.getName();
		info = t.getInfo();
	}

	private void copyTreeRec(MyPhyloTree t, MyNode v, MySparseNode vCopy) {
		Iterator<MyEdge> it = v.outEdges().iterator();
		while(it.hasNext()){
			MyNode c = it.next().getTarget();
			MySparseNode cCopy = newNode(c);
			newEdge(vCopy, cCopy);
			copyTreeRec(t, c, cCopy);
		}
	}

	public Vector<MySparseNode> getNodes() {
		Vector<MySparseNode> nodes = new Vector<MySparseNode>();
		getNodesRec(root, nodes);
		return nodes;
	}

	private void getNodesRec(MySparseNode v, Vector<MySparseNode> nodes) {
		if (!nodes.contains(v)) {
			nodes.add(v);
			for (MySparseNode c : v.getChildren())
				getNodesRec(c, nodes);
		}
	}

	private void getLeavesRec(MySparseNode v, Vector<MySparseNode> nodes) {
		if (v.getOutDegree() != 0) {
			for (MySparseNode c : v.getChildren())
				getLeavesRec(c, nodes);
		}else
			nodes.add(v);
	}

	public Vector<MySparseNode> getLeaves() {
		Vector<MySparseNode> leaves = new Vector<MySparseNode>();
		getLeavesRec(root, leaves);
		return leaves;
	}

	public int getNumberOfLeaves() {
		return getLeaves().size();
	}

	public MySparseNode getRoot() {
		return root;
	}

	public void setRoot(MySparseNode v) {
		root = v;
	}

	public String toBracketString() {
		return root.toNewick("", new Vector<MySparseNode>(), new Hashtable<MySparseNode, String>()) + ";";
	}

	public String toString() {
		return root.toNewick("", new Vector<MySparseNode>(), new Hashtable<MySparseNode, String>());
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

}
