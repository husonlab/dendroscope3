/*
 *   ReattachUniqueTaxa.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.attachNetworks;

import dendroscope.hybroscale.model.reductionSteps.ReplacementInfo;
import dendroscope.hybroscale.model.treeObjects.HybridNetwork;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

import java.util.*;

public class ReattachUniqueTaxa {

	private HashMap<Integer, Vector<String>> treeIndexToUniqueTaxa;
	private MyPhyloTree[] phyloTrees;
	private Vector<String> taxaOrdering;
	private ReplacementInfo rI;

	private HashMap<Integer, Vector<MyNode>> indexToPostorderNodes;
	private HashMap<Integer, HashMap<MyNode, BitSet>> indexToCluster;
	private HashMap<Integer, HashMap<String, BitSet>> indexToAttachingCluster;

	public ReattachUniqueTaxa(MyPhyloTree[] phyloTrees, ReplacementInfo rI, Vector<String> taxaOrdering) {
		this.phyloTrees = phyloTrees;
		this.taxaOrdering = taxaOrdering;
		this.rI = rI;
		this.treeIndexToUniqueTaxa = rI.getTreeIndexToUniqueTaxa();
		for (Vector<String> taxa : treeIndexToUniqueTaxa.values()) {
			for (String taxon : taxa) {
				if (!taxaOrdering.contains(taxon))
					taxaOrdering.add(taxon);
			}
		}
		cmpAttachingClusters();
	}

	private void cmpAttachingClusters() {

		indexToAttachingCluster = new HashMap<Integer, HashMap<String, BitSet>>();
		indexToCluster = new HashMap<Integer, HashMap<MyNode, BitSet>>();
		indexToPostorderNodes = new HashMap<Integer, Vector<MyNode>>();

		for (int index = 0; index < phyloTrees.length; index++) {

			if (treeIndexToUniqueTaxa.containsKey(index)) {

				HashMap<MyNode, BitSet> nodeToCluster = new HashMap<MyNode, BitSet>();
				HashMap<BitSet, MyNode> clusterToNode = new HashMap<BitSet, MyNode>();
				Vector<MyNode> postOrderNodes = new Vector<MyNode>();
				MyPhyloTree t = phyloTrees[index];
				updateTreeClusters(t.getRoot(), nodeToCluster, clusterToNode, postOrderNodes);
				indexToCluster.put(index, nodeToCluster);
				indexToPostorderNodes.put(index, postOrderNodes);
				indexToAttachingCluster.put(index, new HashMap<String, BitSet>());

				Vector<String> uniqueTaxa = treeIndexToUniqueTaxa.get(index);
				BitSet inserted = new BitSet(taxaOrdering.size());
				inserted.set(0, taxaOrdering.size(), true);
				for (String s : uniqueTaxa)
					inserted.set(taxaOrdering.indexOf(s), false);

				for (String taxon : uniqueTaxa) {
					BitSet vCluster = new BitSet(taxaOrdering.size());
					vCluster.set(taxaOrdering.indexOf(taxon));
					MyNode v = clusterToNode.get(vCluster);
					BitSet sibCluster = new BitSet(taxaOrdering.size());
					while (sibCluster.isEmpty()) {
						MyNode p = v.getFirstInEdge().getSource();
						Iterator<MyEdge> it = p.outEdges().iterator();
						while (it.hasNext()) {
							MyNode sib = it.next().getTarget();
							if (!sib.equals(v))
								sibCluster.or(nodeToCluster.get(sib));
						}
						sibCluster.and(inserted);
						v = p;
					}
					indexToAttachingCluster.get(index).put(taxon, sibCluster);
					inserted.set(taxaOrdering.indexOf(taxon));
				}

			}

		}
	}

	private BitSet updateTreeClusters(MyNode v, HashMap<MyNode, BitSet> nodeToCluster,
									  HashMap<BitSet, MyNode> clusterToNode, Vector<MyNode> postOrderNodes) {
		BitSet b = new BitSet(taxaOrdering.size());
		if (v.getOutDegree() == 0){
			b.set(taxaOrdering.indexOf(v.getLabel()));
		}else {
			Iterator<MyEdge> it = v.outEdges().iterator();
			while (it.hasNext())
				b.or(updateTreeClusters(it.next().getTarget(), nodeToCluster, clusterToNode, postOrderNodes));
		}
		postOrderNodes.add(v);
		nodeToCluster.put(v, b);
		clusterToNode.put(b, v);
		return (BitSet) b.clone();
	}

	public void run(HybridNetwork n) {

		for (int index = 0; index < phyloTrees.length; index++) {

			if (treeIndexToUniqueTaxa.containsKey(index)) {

				HashMap<BitSet, MyNode> clusterToNode = new HashMap<BitSet, MyNode>();
				updateNetworkClusters(n.getRoot(), clusterToNode, index);
				Vector<String> uniqueTaxa = treeIndexToUniqueTaxa.get(index);
				for (String taxon : uniqueTaxa) {

					// System.out.println("Adding: " + taxon +" "+index+
					// "\n" + n.toMyBracketString() + "\n" +
					// phyloTrees[index] + ";");
					BitSet sibCluster = indexToAttachingCluster.get(index).get(taxon);

					// System.out.println(sibCluster);
					// int i = sibCluster.nextSetBit(0);
					// while(i != -1){
					// System.out.println(taxaOrdering.get(i));
					// i = sibCluster.nextSetBit(i+1);
					// }

					MyNode s = clusterToNode.get(sibCluster);
					MyNode v = n.newNode();
					v.setLabel(taxon);
					insertUnqiueTaxon(v, s, sibCluster, n, index, clusterToNode);
					clusterToNode.clear();
					updateNetworkClusters(n.getRoot(), clusterToNode, index);
					// System.out.println(n.toMyBracketString()+";");

				}

			}
		}

	}

	private void insertUnqiueTaxon(MyNode v, MyNode s, BitSet sibCluster, HybridNetwork n, int index,
								   HashMap<BitSet, MyNode> clusterToNode) {
		BitSet topCluster = new BitSet(taxaOrdering.size());
		topCluster.set(taxaOrdering.indexOf(v.getLabel()));
		topCluster.or(sibCluster);
		MyNode lcaTop = getLCA(topCluster, index);
		MyNode lcaS = getLCA(sibCluster, index);
		if (lcaTop.equals(lcaS)) {
			// System.out.println("Case-A");
			MyEdge e = n.newEdge(s, v);
			addEdge(e, index);
		} else {
			if (s.getInDegree() == 1) {
				// System.out.println("Case-B");
				MyEdge eDel = s.getFirstInEdge();
				MyNode p = eDel.getSource();
				HashSet<Integer> eIndices = (HashSet<Integer>) getEdgeIndices(eDel).clone();
				n.deleteEdge(eDel);
				MyNode x = n.newNode();
				MyEdge e = n.newEdge(p, x);
				addEdge(e, (HashSet<Integer>) eIndices.clone());
				addEdge(e, index);
				e = n.newEdge(x, s);
				addEdge(e, (HashSet<Integer>) eIndices.clone());
				e = n.newEdge(x, v);
				addEdge(e, index);
			} else {
				// System.out.println("Case-C");
				MyNode root = n.newNode();
				MyNode r = n.getRoot();
				n.setRoot(root);
				MyEdge e = n.newEdge(root, r);
				for (int i = 0; i < phyloTrees.length; i++)
					addEdge(e, i);
				e = n.newEdge(root, v);
				addEdge(e, index);
			}
		}
		for (MyEdge e : n.getEdges()) {
			if (getEdgeIndices(e) == null)
				System.out.println("Error!");
		}
	}

	private HashSet<Integer> getEdgeIndices(MyEdge e) {
		return (HashSet<Integer>) e.getInfo();
	}

	private void addEdge(MyEdge e, int index) {
		if (e.getInfo() == null)
			e.setInfo(new HashSet<Integer>());
		((HashSet<Integer>) e.getInfo()).add(index);
	}

	private void addEdge(MyEdge e, HashSet<Integer> indices) {
		if (e.getInfo() == null)
			e.setInfo(new HashSet<Integer>());
		((HashSet<Integer>) e.getInfo()).addAll(indices);
	}

	public void run(MyPhyloTree n) {

		for (int index = 0; index < phyloTrees.length; index++) {

			if (treeIndexToUniqueTaxa.containsKey(index)) {

				HashMap<BitSet, MyNode> clusterToNode = new HashMap<BitSet, MyNode>();
				try {
					updateNetworkClusters(n.getRoot(), clusterToNode, index);
				} catch (Exception e) {
					e.printStackTrace();
				}

				Vector<String> uniqueTaxa = treeIndexToUniqueTaxa.get(index);
				for (String taxon : uniqueTaxa) {

					BitSet sibCluster = indexToAttachingCluster.get(index).get(taxon);

					MyNode s = clusterToNode.get(sibCluster);

					MyNode v = n.newNode();
					v.setLabel(taxon);
					insertUnqiueTaxon(v, s, sibCluster, n, index, clusterToNode);
					clusterToNode.clear();
					updateNetworkClusters(n.getRoot(), clusterToNode, index);

				}

			}
		}

	}

	private void insertUnqiueTaxon(MyNode v, MyNode s, BitSet sibCluster, MyPhyloTree n, int index,
								   HashMap<BitSet, MyNode> clusterToNode) {
		BitSet topCluster = new BitSet(taxaOrdering.size());
		topCluster.set(taxaOrdering.indexOf(v.getLabel()));
		topCluster.or(sibCluster);
		MyNode lcaTop = getLCA(topCluster, index);
		MyNode lcaS = getLCA(sibCluster, index);
		if (lcaTop.equals(lcaS)) {
			// System.out.println("Case-A");
			MyEdge e = n.newEdge(s, v);
			addEdge(e, index);
		} else {
			if (s.getInDegree() == 1) {
				// System.out.println("Case-B");
				MyEdge eDel = s.getFirstInEdge();
				MyNode p = eDel.getSource();
				HashSet<Integer> eIndices = (HashSet<Integer>) getEdgeIndices(eDel).clone();
				n.deleteEdge(eDel);
				MyNode x = n.newNode();
				MyEdge e = n.newEdge(p, x);
				addEdge(e, (HashSet<Integer>) eIndices.clone());
				addEdge(e, index);
				e = n.newEdge(x, s);
				addEdge(e, (HashSet<Integer>) eIndices.clone());
				e = n.newEdge(x, v);
				addEdge(e, index);
			} else {
				// System.out.println("Case-C");
				MyNode root = n.newNode();
				MyNode r = n.getRoot();
				n.setRoot(root);
				MyEdge e = n.newEdge(root, r);
				for (int i = 0; i < phyloTrees.length; i++)
					addEdge(e, i);
				e = n.newEdge(root, v);
				addEdge(e, index);
			}
		}
	}

	private MyNode getLCA(BitSet cluster, int index) {
		for (MyNode v : indexToPostorderNodes.get(index)) {
			BitSet vCluster = indexToCluster.get(index).get(v);
			BitSet b = (BitSet) cluster.clone();
			b.and(vCluster);
			if (b.equals(cluster))
				return v;
		}
		return null;
	}

	private BitSet updateNetworkClusters(MyNode v, HashMap<BitSet, MyNode> clusterToNode, int index) {
		BitSet b = new BitSet(taxaOrdering.size());
		if (v.getOutDegree() == 0) {
//			System.out.println("Leaf: " + v.getLabel());
			b.set(taxaOrdering.indexOf(v.getLabel()));
		} else {
			Iterator<MyEdge> it = v.outEdges().iterator();
			while (it.hasNext()) {
				MyEdge e = it.next();
				if (getEdgeIndices(e).contains(index)) // || e.getTarget().getInDegree() == 1)
					b.or(updateNetworkClusters(e.getTarget(), clusterToNode, index));
			}
		}
		if (v.getInDegree() < 2 && !clusterToNode.containsKey(b)) {
			clusterToNode.put((BitSet) b.clone(), v);
//			System.out.println(v.getLabel() + " -> " + b);
		}
		return b;
	}

}
