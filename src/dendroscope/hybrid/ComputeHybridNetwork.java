/**
 * Copyright 2015, Daniel Huson
 *
 *(Some files contain contributions from other authors, who are then mentioned separately)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package dendroscope.hybrid;

import jloda.graph.Edge;
import jloda.graph.Node;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * Given two rooted, bifurcating phylogenetic trees T1 and T2 and a minimal
 * acyclic agreement forest of T1 and T2, this functions computes a network
 * displaying T1 and T2 with a minimal number of reticulate nodes.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class ComputeHybridNetwork {

	// private Hashtable<HybridTree, Integer> componentToDepth = new
	// Hashtable<HybridTree, Integer>();
	// private Vector<HybridTree> sortedForest = new Vector<HybridTree>();

	private final Vector<Edge> t1Edges = new Vector<>();

	private final Hashtable<BitSet, Node> t1ClusterToNode = new Hashtable<>();
	private final Hashtable<BitSet, Node> t2ClusterToNode = new Hashtable<>();

	private final Vector<BitSet> t1Cluster = new Vector<>();
	private final Vector<BitSet> t2Cluster = new Vector<>();

	public HybridNetwork run(Vector<HybridTree> forest, HybridTree t1,
							 HybridTree t2, ReplacementInfo rI, TreeMarker tM) throws Exception {

		HybridNetwork newN = new HybridNetwork(forest.lastElement(), false,
				t1.getTaxaOrdering());

		updateClusters(true, newN);
		updateClusters(false, newN);
		newN.update();

		// setting the taxon-labeling which is the replacement character for the
		// network
		if (t1.getReplacementCharacter() != null)
			newN.setReplacementCharacter(t1.getReplacementCharacter());

		// System.out.println();
		// for (HybridTree f : forest)
		// System.out.println(f + ";");

		for (int i = forest.size() - 2; i >= 0; i--) {
			HybridTree f = forest.get(i);
			// sortedForest = sortForest(forest, t1, t2);
			// for (HybridTree f : sortedForest) {

			// System.out.println();
			BitSet mask = new BitSet(newN.getTaxaOrdering().size());
			for (int j = 0; j <= i; j++)
				mask.or(forest.get(j).getNodeToCluster()
						.get(forest.get(j).getRoot()));

			// attach tree f to the so far computed network...
			BitSet r = f.getNodeToCluster().get(f.getRoot());

			// compute parent of the node representing f in t1
			Node l1 = t1.findLCA(r);
			Node p1 = l1.getInEdges().next().getSource();

			// compute parent of the node representing f in t2
			Node l2 = t2.findLCA(r);
			Node p2 = l2.getInEdges().next().getSource();

			// System.out.println("l1: " + t1.getLabel(l1));
			// System.out.println("l2: " + t2.getLabel(l2));
			// System.out.println("p1: " + t1.getLabel(p1));
			// System.out.println("p2: " + t2.getLabel(getSibling(l2, t2)));

			// find Node whose in-edge is attached by the reticulate edge
			Node v1 = findChildNode(getSibling(l1, t1), t1, newN, true, mask);
			Node v2 = findChildNode(getSibling(l2, t2), t2, newN, false, mask);

			// if no edge is found, f is attached to the out-edge of the root
			Iterator<Edge> it = newN.getRoot().getOutEdges();
			Node v = it.next().getTarget();
			if (newN.getLabel(v).equals("rho"))
				v = it.next().getTarget();

			if (v1 == null || newN.getLabel(v1).equals("rho")
					|| v1.equals(newN.getRoot()))
				v1 = v;
			if (v2 == null || newN.getLabel(v2).equals("rho")
					|| v2.equals(newN.getRoot()))
				v2 = v;

			// System.out.println(newN);
			// System.out.println("Root-Label: " + f.getLabel(f.getRoot()));
			// System.out.println("v1: " + newN.getLabel(v1));
			// System.out.println("v2: " + newN.getLabel(v2));

			// perform attachment of tree f
			insertReticulateEdges(newN, v1, v2, f, tM);
			newN.update();

			t1ClusterToNode.clear();
			t2ClusterToNode.clear();
			t1Cluster.clear();
			t2Cluster.clear();
			updateClusters(true, newN);
			updateClusters(false, newN);

		}

		newN.update();
		return newN;
	}

	private Node getSibling(Node v, HybridNetwork t) {
		Node p = v.getInEdges().next().getSource();
		Iterator<Edge> it = p.getOutEdges();
		Node sibling = it.next().getTarget();
		if (sibling.equals(v))
			sibling = it.next().getTarget();
		return sibling;
	}

	private Node findChildNode(Node v, HybridNetwork t, HybridNetwork newN,
							   boolean isTree1, BitSet mask) {

		// searching for first node in n whose subtree contains all leaves in f
		// Iterator<Edge> it = p.getOutEdges();
		// BitSet c = t.getNodeToCluster().get(it.next().getTarget());
		// if (c.equals(r))
		// c = t.getNodeToCluster().get(it.next().getTarget());
		// System.out.println();
		// System.out.println("v: " + t.getLabel(v));
		BitSet c = t.getNodeToCluster().get(v);
		// System.out.println("vC: " + c);

		// check whether the subtree of p in n contains all leaves in f
		Node sibling = checkNetworkCluster(c, newN, isTree1, mask);
		BitSet b = (BitSet) mask.clone();
		b.or(c);
		// System.out.println(mask);
		// System.out.println(c);
		if (!b.equals(mask) && sibling != null) {
			// System.out.println("TRUE: "
			// + t.getLabel(t.getClusterToNode().get(c)));
			return sibling;
		} else {
			// System.out.println("FALSE: " + t.getLabel(v));
			// System.out.println(c);
			if (v.getInDegree() != 0) {
				Node p = v.getInEdges().next().getSource();
				return findChildNode(p, t, newN, isTree1, mask);
			} else
				return null;
		}
	}

	// returns true if cluster c is subset of a cluster in the network n
	private Node checkNetworkCluster(BitSet c, HybridNetwork newN,
									 boolean isTree1, BitSet mask) {
		Vector<BitSet> clusterSet;
		if (isTree1)
			clusterSet = t1Cluster;
		else
			clusterSet = t2Cluster;

		for (BitSet b : clusterSet) {
			if (b != null) {
				BitSet cluster = (BitSet) b.clone();
				cluster.or(mask);
				BitSet a = (BitSet) cluster.clone();
				a.or(c);
				// System.out.println("checking label: ");
				// System.out.println("label: " + newN.getLabel(v));
				// System.out.println(a);
				// System.out.println(c);
				// System.out.println(cluster);
				if (a.equals(cluster)) {
					if (isTree1)
						return t1ClusterToNode.get(b);
					else
						return t2ClusterToNode.get(b);
				}
			}
		}
		return null;
	}

	private void insertReticulateEdges(HybridNetwork n, Node v1, Node v2,
									   HybridTree f, TreeMarker tM) {

		Edge e = v1.getInEdges().next();
		boolean isSpecial = n.isSpecial(e);
		Node t = e.getTarget();
		Node s = e.getSource();
		n.deleteEdge(e);

		// adding tree f to network, v is the root of the added tree f
		Node v = addTreeToNetwork(f, n);
		Node retNode = n.newNode();

		// split in-edge of v1 by inserting node x1
		Node x1 = n.newNode();
		n.newEdge(s, x1);
		Edge newE = n.newEdge(x1, t);
		if (isSpecial)
			n.setSpecial(newE, true);

		// adding 1st reticulate edge
		Edge e1 = n.newEdge(x1, retNode);
		n.setSpecial(e1, true);
		n.setWeight(e1, 0);
		tM.insertT1Edge(e1);
		t1Edges.add(e1);

		e = v2.getInEdges().next();
		isSpecial = n.isSpecial(e);
		t = e.getTarget();
		s = e.getSource();
		n.deleteEdge(e);

		// split in-edge of v2 by inserting node x2
		Node x2 = n.newNode();
		n.newEdge(s, x2);
		newE = n.newEdge(x2, t);
		if (isSpecial)
			n.setSpecial(newE, true);

		// adding 2nd reticulate edge
		Edge e2 = n.newEdge(x2, retNode);
		n.setSpecial(e2, true);
		n.setWeight(e2, 0);

		// attach generated tree f to the network
		n.newEdge(retNode, v);
		n.update();

	}

	// adding the binary tree f to the resolved network n
	private Node addTreeToNetwork(HybridTree f, HybridNetwork n) {
		Node vCopy = n.newNode(f.getRoot());
		n.setLabel(vCopy, f.getLabel(f.getRoot()));
		addTreeToNetworkRec(vCopy, f.getRoot(), f, n);
		return vCopy;
	}

	private void addTreeToNetworkRec(Node vCopy, Node v, HybridNetwork f,
									 HybridNetwork n) {
		Iterator<Node> it = f.getSuccessors(v);
		while (it.hasNext()) {
			Node c = it.next();
			Node cCopy = n.newNode(c);
			n.setLabel(cCopy, f.getLabel(c));
			n.newEdge(vCopy, cCopy);
			addTreeToNetworkRec(cCopy, c, f, n);
		}
	}

	private void updateClusters(boolean isTree1, HybridNetwork newN) {
		updateClustersRec(newN.getRoot(), isTree1, newN);
	}

	private Vector<String> updateClustersRec(Node v, boolean isTree1,
											 HybridNetwork newN) {
		Vector<String> vTaxa = new Vector<>();
		BitSet cluster = new BitSet(newN.getTaxaOrdering().size());
		Iterator<Edge> it = v.getOutEdges();
		if (v.getOutDegree() != 0) {
			while (it.hasNext()) {
				Edge e = it.next();
				if (!newN.isSpecial(e) || (t1Edges.contains(e) && isTree1)
						|| (!t1Edges.contains(e) && !isTree1)) {
					Node child = e.getTarget();
					Vector<String> cTaxa = updateClustersRec(child, isTree1,
							newN);
					for (String l : cTaxa) {
						if (!vTaxa.contains(l))
							vTaxa.add(l);
					}
				}
			}
			for (String l : vTaxa) {
				int bitIndex = newN.getTaxaOrdering().indexOf(l);
				cluster.set(bitIndex);
			}
		} else {
			vTaxa.add(newN.getLabel(v));
			int bitIndex = newN.getTaxaOrdering().indexOf(newN.getLabel(v));
			cluster.set(bitIndex);
		}
		if (v.getOutDegree() == 2 || v.getOutDegree() == 0) {
			if (isTree1) {
				if (!t1Cluster.contains(cluster)) {
					t1ClusterToNode.put(cluster, v);
					t1Cluster.add(cluster);
				}
			} else {
				if (!t2Cluster.contains(cluster)) {
					t2ClusterToNode.put(cluster, v);
					t2Cluster.add(cluster);
				}
			}
		}
		return vTaxa;
	}

}
