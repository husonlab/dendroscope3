package dendroscope.hybroscale.model.treeObjects;

import java.util.Iterator;
import java.util.Vector;

import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

/**
 * This class represents a rooted, bifurcating phylogenetic tree.
 * 
 * @author Benjamin Albrecht, 6.2010
 */

public class HybridTree extends HybridNetwork {

	public HybridTree(MyPhyloTree t, boolean rootTree, Vector<String> taxaOrdering) {
		super(t, rootTree, taxaOrdering);
	}

	public HybridTree(HybridNetwork t, boolean rootTree, Vector<String> taxaOrdering) {
		super(t, rootTree, taxaOrdering);
	}

	// returns subtree under node v
	public HybridTree getSubtree(MyNode v, boolean doUpdate) {
		MyPhyloTree sT = new MyPhyloTree();
		if (contains(v)) {
			MyNode vCopy = sT.newNode(v);
			sT.setLabel(vCopy, getLabel(v));
			sT.setRoot(vCopy);
			createSubtreeRec(v, vCopy, sT);
		}
		HybridTree newTree = new HybridTree(sT, false, super.getTaxaOrdering());
		if (doUpdate)
			newTree.update();

		Iterator<Vector<String>> it = this.getTaxaPairToWeight().keySet().iterator();
		while (it.hasNext()) {
			Vector<String> key = it.next();
			int value = this.getTaxaPairToWeight().get(key);
			newTree.taxaPairToWeight.put(key, value);
		}

		return newTree;
	}

	public void deleteNode(MyNode v) {
		MyNode p = v.getInDegree() != 0 ? v.getInEdges().next().getSource() : null;
		super.deleteNode(v);
		if (p != null && p.getInDegree() == 1 && p.getOutDegree() == 1) {
			MyNode s = p.getInEdges().next().getSource();
			MyNode t = p.getOutEdges().next().getTarget();
			t.setSolid(p.isSolid());
			s.removeOutEdge(p.getInEdges().next());
			t.removeInEdge(p.getOutEdges().next());
			newEdge(s, t);
		}
	}

	@SuppressWarnings("unchecked")
	private void createSubtreeRec(MyNode v, MyNode vCopy, MyPhyloTree t) {
		Iterator<MyEdge> it = getOutEdges(v);
		while (it.hasNext()) {
			MyEdge e = it.next();
			MyNode c = e.getTarget();
			MyNode cCopy;
			cCopy = t.newNode(c);
			t.setLabel(cCopy, getLabel(c));
			t.newEdge(vCopy, cCopy);
			createSubtreeRec(c, cCopy, t);
		}
	}

}
