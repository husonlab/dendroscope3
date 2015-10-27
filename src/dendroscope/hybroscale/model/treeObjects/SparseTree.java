package dendroscope.hybroscale.model.treeObjects;

import java.util.Iterator;
import java.util.Vector;

import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

public class SparseTree {

	private boolean treeChanged = true;
	
	private String info = "";
	private SparseTreeNode root;
	private Vector<SparseTreeNode> postOrderNodes = new Vector<SparseTreeNode>();
	private Vector<SparseTreeNode> leaves = new Vector<SparseTreeNode>();
    private boolean isCommonCherry = false;

	public SparseTree() {
		root = new SparseTreeNode(null, this, null);
	}

	public SparseTree(SparseTreeNode root) {
		this.root = root;
		postOrderWalk();
		for (SparseTreeNode v : postOrderNodes) {
			v.setOwner(this);
			if (v.getOutDegree() == 0)
				leaves.add(v);
		}
	}

	public SparseTree(MyPhyloTree t) {
		copy(t);
	}
	
	public SparseTree(MyPhyloTree t, String info) {
		copy(t);
		this.info = info;
	}

	public SparseTree(SparseTree t) {
		SparseTreeNode v = t.getRoot();
		root = new SparseTreeNode(null, this, v.getLabel());
		copyTreeRec(t, v, root);
	}

	private void copyTreeRec(SparseTree t, SparseTreeNode v, SparseTreeNode vCopy) {
		for (SparseTreeNode c : v.getChildren()) {
			SparseTreeNode cCopy = new SparseTreeNode(vCopy, this, c.getLabel());
			if (c.getOutDegree() != 0)
				copyTreeRec(t, c, cCopy);
		}
	}
	
	public SparseTree(SparseNetwork n) {
		
		for (SparseNetNode v : n.getNodes()) {
			if (v.getInDegree() > 1) 
				System.err.println("WARNING: this is no network " + n.getPhyloTree() + ";");
		}
		
		SparseNetNode v = n.getRoot();
		root = new SparseTreeNode(null, this, v.getLabel());
		copyNetworkRec(n, v, root);
	}

	private void copyNetworkRec(SparseNetwork n, SparseNetNode v, SparseTreeNode vCopy) {
		for (SparseNetEdge e : v.getOutEdges()) {
			SparseNetNode c = e.getTarget();
			SparseTreeNode cCopy = new SparseTreeNode(vCopy, this, c.getLabel());
			if(e.isSolid())
				cCopy.setSolid(true);
			if (c.getOutDegree() != 0)
				copyNetworkRec(n, c, cCopy);
		}
	}

	private void copy(MyPhyloTree t) {
		MyNode v = t.getRoot();
		root = new SparseTreeNode(null, this, t.getLabel(v));
		if (t.getLabel(v) != null)
			root.setLabel(t.getLabel(v));
		copyTreeRec(t, v, root);
	}

	/* copy the tree t starting with node v */

	public void copy(MyPhyloTree t, MyNode v) {
		root = new SparseTreeNode(null, this, t.getLabel(v));
		if (t.getLabel(v) != null)
			root.setLabel(t.getLabel(v));
		copyTreeRec(t, v, root);
	}

	private void copyTreeRec(MyPhyloTree t, MyNode v, SparseTreeNode vCopy) {
		Iterator<MyEdge> it = v.getOutEdges();
		while (it.hasNext()) {
			MyNode c = it.next().getTarget();
			SparseTreeNode cCopy = new SparseTreeNode(vCopy, this, t.getLabel(c));
			if (c.getOutDegree() != 0)
				copyTreeRec(t, c, cCopy);
		}
	}

	public SparseTreeNode getRoot() {
		return root;
	}

	public Iterator<SparseTreeNode> postOrderWalk() {
		if (treeChanged) {
			postOrderNodes.clear();
			initPostOrderNodesRec(root, new Vector<SparseTreeNode>());
			treeChanged = false;
		}
		return postOrderNodes.iterator();
	}

	private void initPostOrderNodesRec(SparseTreeNode v, Vector<SparseTreeNode> visited) {
		for (SparseTreeNode child : v.getChildren()) {
			if (!visited.contains(child)) {
				visited.add(child);
				initPostOrderNodesRec(child, visited);
			}
		}
		postOrderNodes.add(v);
	}

	public Vector<SparseTreeNode> getLeaves() {
		return leaves;
	}

	public Vector<SparseTreeNode> getNodes() {
		if (treeChanged) {
			postOrderNodes.clear();
			initPostOrderNodesRec(root, new Vector<SparseTreeNode>());
			treeChanged = false;
		}
		return postOrderNodes;
	}

	public void setLabel(SparseTreeNode v, String s) {
		v.setLabel(s);
	}

	public void restrictNode(SparseTreeNode v) {
		v.restrict();
	}

	public void deleteNode(SparseTreeNode v) {

		SparseTreeNode p = v.getParent();
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

	public void removeLeaf(SparseTreeNode v) {
		leaves.remove(v);
	}

	public void addLeaf(SparseTreeNode v) {
		if (!leaves.contains(v))
			leaves.add(v);
	}

	public void addLabel(SparseTreeNode v, String s) {
		v.setLabel(s);
	}

	public void replaceLeaf(SparseTreeNode leaf, SparseTree t) {
		SparseTreeNode p = leaf.getParent();
		if (p != null) {
			p.getChildren().remove(leaf);
			p.addChild(t.getRoot());
			t.getRoot().setParent(p);
		}else
			setRoot(t.getRoot());	
		leaves.remove(leaf);
		for (SparseTreeNode v : t.getLeaves())
			addLeaf(v);
		treeChanged = true;
	}

	public SparseTree pruneSubtree(SparseTreeNode v) {
		SparseTreeNode root = new SparseTreeNode(null, null, v.getLabel());
		SparseTreeNode p = v.getParent();

		// compute pruned subtree
		copyNode(v, root);
		SparseTree eT = new SparseTree(root);

		// delete subtree
		v.delete();
		if (p != null) {
			p.removeChild(v);
			p.restrict();
		}

		return eT;
	}

	private void copyNode(SparseTreeNode v, SparseTreeNode vCopy) {
		for (SparseTreeNode c : v.getChildren()) {
			SparseTreeNode cCopy = new SparseTreeNode(vCopy, null, c.getLabel());
			copyNode(c, cCopy);
		}
	}

	public MyPhyloTree getPhyloTree() {
		MyPhyloTree t = new MyPhyloTree();
		if (root != null) {
			MyNode rootCopy = t.newNode();
			if (root.getLabel() != null)
				t.setLabel(rootCopy, root.getLabel());
			getPhyloTreeRec(t, root, rootCopy);
			t.setRoot(rootCopy);
		}
		return t;
	}

	private void getPhyloTreeRec(MyPhyloTree t, SparseTreeNode v, MyNode vCopy) {
		for (SparseTreeNode c : v.getChildren()) {
			MyNode cCopy = t.newNode();
			if (c.getLabel() != null)
				t.setLabel(cCopy, c.getLabel());
			cCopy.setSolid(c.isSolid());
			t.newEdge(vCopy, cCopy);
			getPhyloTreeRec(t, c, cCopy);
		}
	}

	public void setRoot(SparseTreeNode v) {
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

		root = new SparseTreeNode(null, this, t.getLabel(v));
		if (t.getLabel(v) != null)
			root.setLabel(t.getLabel(v));
		copyTreeRecNoRet(t, v, root, false);

	}

	/* copy the tree t rec without copying the reticulate nodes */
	private void copyTreeRecNoRet(MyPhyloTree t, MyNode v, SparseTreeNode vCopy,
			boolean check) {
		Iterator<MyEdge> it = v.getOutEdges();
		if (!check || v.getInDegree() == 1) {
			while (it.hasNext()) {
				MyNode c = it.next().getTarget();
				if (c.getInDegree() == 1) {
					SparseTreeNode cCopy = new SparseTreeNode(vCopy, this,
							t.getLabel(c));
					if (c.getOutDegree() != 0)
						copyTreeRecNoRet(t, c, cCopy, true);
				}
			}
		}

	}

//	@Override
//	public boolean equals(Object obj) {
//		SparseTree t;
//		if (obj instanceof SparseTree)
//			t = (SparseTree) obj;
//		else
//			return false;
//		return (new SparseIsomorphismCheck()).run(this, t);
//	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
	
	public boolean isCommonCherry() {
		return isCommonCherry;
	}

	public void setCommonCherry(boolean isCommonCherry) {
		this.isCommonCherry = isCommonCherry;
	}
	
	public String getInfo(){
		return info;
	}
	
}
