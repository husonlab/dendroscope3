package dendroscope.hybroscale.model.attachNetworks;

import dendroscope.hybroscale.model.reductionSteps.ReplacementInfo;
import dendroscope.hybroscale.model.treeObjects.HybridNetwork;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

import java.util.HashSet;
import java.util.Iterator;

/**
 * This method replaces distinct leaves of a resolved network by rooted,
 * bifurcating phylogenetic trees.
 * 
 * @author Benjamin Albrecht, 6.2010
 */

public class ReattachSubtrees {

	public HybridNetwork run(HybridNetwork n, ReplacementInfo rI, int numOfInputTrees) {

		Iterator<MyNode> it = n.getLeaves().iterator();
		while (it.hasNext()) {
			MyNode taxon = it.next();
			String label = n.getLabel(taxon);
			// checking if label replaces a subtree
			if (rI.getLabelToSubtree().containsKey(label)) {
				// getting tree replaced by the label
				MyPhyloTree p = rI.getLabelToSubtree().get(label);
				// replacing label through tree
				addTreeToNetwork(p, n, taxon, numOfInputTrees);
			}
		}

		return n;
	}

	private void addTreeToNetwork(MyPhyloTree p, HybridNetwork n, MyNode taxon, int numOfInputTrees) {
		MyNode vCopy = n.newNode(p.getRoot());
		n.setLabel(vCopy, p.getLabel(p.getRoot()));
		addTreeToNetworkRec(vCopy, p.getRoot(), p, n, numOfInputTrees);

		// attaching tree p to network n
		// -> connect all in-edges of taxon to the root of the tree
        Iterator<MyEdge> it = taxon.inEdges().iterator();
		while (it.hasNext()) {
			MyEdge e = it.next();
			boolean isSpecial = n.isSpecial(e);
			MyNode parent = e.getSource();
			MyEdge eCopy = n.newEdge(parent, vCopy);
			if (isSpecial) {
				n.setSpecial(eCopy, true);
				n.setWeight(eCopy, 0);
			}
			
			HashSet<Integer> edgeIndices = new HashSet<Integer>();
			for (int i = 0; i < numOfInputTrees; i++)
				edgeIndices.add(i);
			eCopy.setInfo(edgeIndices);
			
		}

		// delete taxon (taxon is now replaced by a common binary tree)
		n.deleteNode(taxon);

		n.update();
	}

	@SuppressWarnings("unchecked")
	private void addTreeToNetworkRec(MyNode vCopy, MyNode v, MyPhyloTree p, HybridNetwork n, int numOfInputTrees) {
		Iterator<MyEdge> it = p.getOutEdges(v);

		HashSet<Integer> edgeIndices = new HashSet<Integer>();
		for (int i = 0; i < numOfInputTrees; i++)
			edgeIndices.add(i);

		while (it.hasNext()) {
			MyNode c = it.next().getTarget();
			MyNode cCopy = n.newNode(c);
			n.setLabel(cCopy, p.getLabel(c));
			MyEdge newEdge = n.newEdge(vCopy, cCopy);
			newEdge.setInfo(edgeIndices);
			addTreeToNetworkRec(cCopy, c, p, n, numOfInputTrees);
		}
		
	}
	
	public MyPhyloTree run(MyPhyloTree n, ReplacementInfo rI, int numOfInputTrees) {
		
		Iterator<MyNode> it = n.getLeaves().iterator();
		while (it.hasNext()) {
			MyNode taxon = it.next();
			String label = n.getLabel(taxon);
			// checking if label replaces a subtree
			if (rI.getLabelToSubtree().containsKey(label)) {
				// getting tree replaced by the label
				MyPhyloTree p = rI.getLabelToSubtree().get(label);
				// replacing label through tree
				addTreeToNetwork(p, n, taxon, numOfInputTrees);
			}
		}

		return n;
	}

	private void addTreeToNetwork(MyPhyloTree p, MyPhyloTree n, MyNode taxon, int numOfInputTrees) {
		
		MyNode vCopy = n.newNode(p.getRoot());
		n.setLabel(vCopy, p.getLabel(p.getRoot()));
		addTreeToNetworkRec(vCopy, p.getRoot(), p, n, numOfInputTrees);

		// attaching tree p to network n
		// -> connect all in-edges of taxon to the root of the tree
        Iterator<MyEdge> it = taxon.inEdges().iterator();
		while (it.hasNext()) {
			MyEdge e = it.next();
			boolean isSpecial = n.isSpecial(e);
			MyNode parent = e.getSource();
			MyEdge eCopy = n.newEdge(parent, vCopy);
			if (isSpecial) {
				n.setSpecial(eCopy, true);
				n.setWeight(eCopy, 0);
			}
			
			HashSet<Integer> edgeIndices = new HashSet<Integer>();
			for (int i = 0; i < numOfInputTrees; i++)
				edgeIndices.add(i);
			eCopy.setInfo(edgeIndices);
			
		}

		// delete taxon (taxon is now replaced by a common binary tree)
		n.deleteNode(taxon);

	}

	@SuppressWarnings("unchecked")
	private void addTreeToNetworkRec(MyNode vCopy, MyNode v, MyPhyloTree p, MyPhyloTree n, int numOfInputTrees) {
		Iterator<MyEdge> it = p.getOutEdges(v);

		HashSet<Integer> edgeIndices = new HashSet<Integer>();
		for (int i = 0; i < numOfInputTrees; i++)
			edgeIndices.add(i);

		while (it.hasNext()) {
			MyNode c = it.next().getTarget();
			MyNode cCopy = n.newNode(c);
			n.setLabel(cCopy, p.getLabel(c));
			MyEdge newEdge = n.newEdge(vCopy, cCopy);
			newEdge.setInfo(edgeIndices);
			addTreeToNetworkRec(cCopy, c, p, n, numOfInputTrees);
		}
		
	}
	
}
