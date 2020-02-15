/*
 *   EasyTree.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.cmpAllMAAFs;

import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

import java.util.Iterator;
import java.util.Vector;

public class EasyTree {

	private boolean treeChanged = true;

	private String info = "";
	private EasyNode root;
	private Vector<EasyNode> postOrderNodes = new Vector<EasyNode>();
	private Vector<EasyNode> leaves = new Vector<EasyNode>();

	public EasyTree() {
		root = new EasyNode(null, this, null);
	}

	public EasyTree(EasyNode root) {
		this.root = root;
		postOrderWalk();
		for (EasyNode v : postOrderNodes) {
			v.setOwner(this);
			if (v.getOutDegree() == 0 && !leaves.contains(v))
				leaves.add(v);
		}
	}

	public EasyTree(MyPhyloTree t) {
		copy(t);
	}

	public EasyTree(MyPhyloTree t, String info) {
		copy(t);
		this.info = info;
	}

	public EasyTree(EasyTree t) {
		EasyNode v = t.getRoot();
		root = new EasyNode(null, this, v.getLabel());
		root.setInfo(v.getInfo());
		root.setAddedNode(v.isAddedNode());
		root.setContractedNodes(v.getContractedNodes());
		copyTreeRec(t, v, root);
		info = t.getInfo();
	}

	private void copyTreeRec(EasyTree t, EasyNode v, EasyNode vCopy) {
		for (EasyNode c : v.getChildren()) {
			EasyNode cCopy = new EasyNode(vCopy, this, c.getLabel());
			cCopy.setInfo(c.getInfo());
			cCopy.setAddedNode(c.isAddedNode());
			cCopy.setContractedNodes(c.getContractedNodes());
			if (c.getOutDegree() != 0)
				copyTreeRec(t, c, cCopy);
			cCopy.setSolid(c.isSolid());
		}
	}

	private void copy(MyPhyloTree t) {
		MyNode v = t.getRoot();
		root = new EasyNode(null, this, t.getLabel(v));
		if (t.getLabel(v) != null)
			root.setLabel(t.getLabel(v));
		copyTreeRec(t, v, root);
	}

	/* copy the tree t starting with node v */

	public void copy(MyPhyloTree t, MyNode v) {

		root = new EasyNode(null, this, t.getLabel(v));
		if (t.getLabel(v) != null)
			root.setLabel(t.getLabel(v));
		copyTreeRec(t, v, root);
	}

	private void copyTreeRec(MyPhyloTree t, MyNode v, EasyNode vCopy) {
		Iterator<MyEdge> it = v.outEdges().iterator();
		while (it.hasNext()) {
			MyNode c = it.next().getTarget();
			EasyNode cCopy = new EasyNode(vCopy, this, t.getLabel(c));
			if (c.getOutDegree() != 0)
				copyTreeRec(t, c, cCopy);
			cCopy.setSolid(c.isSolid());
		}
	}

	public EasyNode getRoot() {
		return root;
	}

	public Iterator<EasyNode> postOrderWalk() {
		if (treeChanged) {
			postOrderNodes.clear();
			initPostOrderNodesRec(root, new Vector<EasyNode>());
			treeChanged = false;
		}
		return postOrderNodes.iterator();
	}

	private void initPostOrderNodesRec(EasyNode v, Vector<EasyNode> visited) {
		for (EasyNode child : v.getChildren()) {
			if (!visited.contains(child)) {
				visited.add(child);
				initPostOrderNodesRec(child, visited);
			}
		}
		postOrderNodes.add(v);
	}

	public Vector<EasyNode> getLeaves() {
		return leaves;
	}

	public Vector<EasyNode> getNodes() {
		if (treeChanged) {
			postOrderNodes.clear();
			initPostOrderNodesRec(root, new Vector<EasyNode>());
			treeChanged = false;
		}
		return postOrderNodes;
	}

	public void setLabel(EasyNode v, String s) {
		v.setLabel(s);
	}

	public void restrictNode(EasyNode v) {
		v.restrict();
	}

	public void deleteNode(EasyNode v) {

		EasyNode p = v.getParent();
		v.delete();
		if (p != null) {
			p.removeChild(v);
			if (p.getOutDegree() == 0)
				leaves.add(p);
			v.setParent(null);
		} else
			root = null;
		treeChanged = true;
	}

	public void removeLeaf(EasyNode v) {
		leaves.remove(v);
	}

	public void addLeaf(EasyNode v) {
		if (!leaves.contains(v))
			leaves.add(v);
	}

	public void addLabel(EasyNode v, String s) {
		v.setLabel(s);
	}

	public EasyTree pruneSubtree(EasyNode v) {
		EasyNode root = new EasyNode(null, null, v.getLabel());
		root.setInfo(v.getInfo());
		root.setContractedNodes(v.getContractedNodes());
		root.setAddedNode(v.isAddedNode());
		EasyNode p = v.getParent();

		// compute pruned subtree
		copyNode(v, root);
		EasyTree eT = new EasyTree(root);

		// delete subtree
		v.delete();
		if (p != null) {
			p.removeChild(v);
			if (p.getOutDegree() == 1)
				p.restrict();
		}

		return eT;
	}

	private void copyNode(EasyNode v, EasyNode vCopy) {
		for (EasyNode c : v.getChildren()) {
			EasyNode cCopy = new EasyNode(vCopy, null, c.getLabel());
			cCopy.setInfo(c.getInfo());
			cCopy.setContractedNodes(c.getContractedNodes());
			cCopy.setAddedNode(c.isAddedNode());
			copyNode(c, cCopy);
		}
	}

	public MyPhyloTree getPhyloTree() {
		MyPhyloTree t = new MyPhyloTree();
		if (root != null) {
			MyNode rootCopy = t.newNode();
			t.setLabel(rootCopy, root.getLabel());
			rootCopy.setInfo(root.getInfo());
			getPhyloTreeRec(t, root, rootCopy);
			t.setRoot(rootCopy);
		}
		return t;
	}

	private void getPhyloTreeRec(MyPhyloTree t, EasyNode v, MyNode vCopy) {
		for (EasyNode c : v.getChildren()) {
			MyNode cCopy = t.newNode();
			t.setLabel(cCopy, c.getLabel());
			cCopy.setInfo(c.getInfo());
			t.newEdge(vCopy, cCopy);
			getPhyloTreeRec(t, c, cCopy);
		}
	}

	public void setRoot(EasyNode v) {
		root = v;
	}

	public void setTreeChanged(boolean treeChanged) {
		this.treeChanged = treeChanged;
	}

	/*
	 * copy the tree t starting with the node v without copying the reticulate
	 * nodes
	 */

	public void copyNoRet(MyPhyloTree t, MyNode v) {

		root = new EasyNode(null, this, t.getLabel(v));
		if (t.getLabel(v) != null)
			root.setLabel(t.getLabel(v));
		copyTreeRecNoRet(t, v, root, false);

	}

	/* copy the tree t rec without copying the reticulate nodes */

	private void copyTreeRecNoRet(MyPhyloTree t, MyNode v, EasyNode vCopy, boolean check) {
		Iterator<MyEdge> it = v.outEdges().iterator();
		if (!check || v.getInDegree() == 1) {
			while (it.hasNext()) {
				MyNode c = it.next().getTarget();
				if (c.getInDegree() == 1) {
					EasyNode cCopy = new EasyNode(vCopy, this, t.getLabel(c));
					if (c.getOutDegree() != 0)
						copyTreeRecNoRet(t, c, cCopy, true);
				}
			}
		}

	}

	public String getInfo() {
		return info;
	}

}
