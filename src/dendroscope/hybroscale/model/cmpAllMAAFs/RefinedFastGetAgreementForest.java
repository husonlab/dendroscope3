package dendroscope.hybroscale.model.cmpAllMAAFs;

import java.util.*;

import dendroscope.hybroscale.model.HybridManager.Computation;
import dendroscope.hybroscale.model.cmpMinNetworks.DFSManager;
import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.model.treeObjects.SparseTree;
import dendroscope.hybroscale.model.treeObjects.SparseTreeNode;
import dendroscope.hybroscale.model.util.IsomorphismCheck;
import dendroscope.hybroscale.util.graph.MyNode;

public class RefinedFastGetAgreementForest {

	private boolean stop = false;
	private Computation compValue;
	private EasySiblingMemory easySibMem;

	private HashMap<BitSet, Vector<HybridTree[]>> forestSets = new HashMap<BitSet, Vector<HybridTree[]>>();
	private Vector<Vector<SparseTree>> MAAFs = new Vector<Vector<SparseTree>>();

	private String rootLabel;
	private int k;
	private int recCalls = 0;

	private HybridTree tree1, tree2;
	private HybridTree t1Restricted, t2Restricted;

	private Vector<String> taxaOrdering;
	private Vector<String> t1Taxa, t2Taxa;
	private Vector<SparseTree> t2Components, t1Components;
	public Vector<Vector<SparseTree>> run(HybridTree t1, HybridTree t2, int forestSize, Computation compValue,
			Vector<String> taxaOrdering, DFSManager manager) {

//		 System.out.println(" *************************************  " +
//		 forestSize);

		this.compValue = compValue;
		this.tree1 = t1;
		this.tree2 = t2;
		this.k = forestSize;
		this.taxaOrdering = taxaOrdering;

		t1Taxa = new Vector<String>();
		for (MyNode leaf : t1.getLeaves())
			t1Taxa.add(leaf.getLabel());
		t2Taxa = new Vector<String>();
		for (MyNode leaf : t2.getLeaves())
			t2Taxa.add(leaf.getLabel());

		t1Components = new Vector<SparseTree>();
		t2Components = new Vector<SparseTree>();

		rootLabel = t2.getLabel(t2.getRoot());
		easySibMem = new EasySiblingMemory(taxaOrdering);

		if (!(new EasyIsomorphismCheck()).run(new EasyTree(t1), new EasyTree(t2))) {

			easySibMem = new EasySiblingMemory(taxaOrdering);

			// Restricting both trees to an equal set of taxa **************

			t2Restricted = new HybridTree(t2, false, taxaOrdering);
			Vector<MyNode> obsoleteNodes = new Vector<MyNode>();
			for (MyNode leaf : t2Restricted.getLeaves()) {
				if (!t1Taxa.contains(leaf.getLabel()))
					obsoleteNodes.add(leaf);
			}
			for (MyNode v : obsoleteNodes) {
				t2Restricted.deleteNode(v);
				t2Components.add(new SparseTree(new SparseTreeNode(null, null, v.getLabel())));
			}

			t1Restricted = new HybridTree(t1, false, taxaOrdering);
			obsoleteNodes = new Vector<MyNode>();
			for (MyNode leaf : t1Restricted.getLeaves()) {
				if (!t2Taxa.contains(leaf.getLabel()))
					obsoleteNodes.add(leaf);
			}
			for (MyNode v : obsoleteNodes) {
				t1Restricted.deleteNode(v);
				t1Components.add(new SparseTree(new SparseTreeNode(null, null, v.getLabel())));
			}

			// *************************************************************

			if (!(new EasyIsomorphismCheck()).run(new EasyTree(t1Restricted), new EasyTree(t2Restricted))) {

				EasyTree forestTree = new EasyTree(t2Restricted);
				EasyTree h1 = new EasyTree(t1Restricted);
				EasyTree h2 = new EasyTree(t2Restricted);

				initNodeLabelings(forestTree, h1, h2);

				Vector<EasyTree> t2Forest = new Vector<EasyTree>();
				t2Forest.add(forestTree);

				stop = false;
				computeMAAF(h1, t2Forest, false);

			} else {

				Vector<EasyTree> forest = new Vector<EasyTree>();
				forest.add(new EasyTree(t2Restricted));
				addMAAF(forest, tree1, tree2);
			}

		} else {
			Vector<EasyTree> forest = new Vector<EasyTree>();
			forest.add(new EasyTree(t2));
			addMAAF(forest, tree1, tree2);
		}
		
		if(manager != null)
			manager.increaseAGCounter(recCalls);
		
		forestSets = null;
		easySibMem.freeMemory();
		easySibMem = null;
		
		return MAAFs;

	}

	private void addMAAF(Vector<EasyTree> forest, HybridTree t1, HybridTree t2) {

		BitSet b = easySibMem.getForestSet(forest);
		if (!forestSets.containsKey(b)) {

			forestSets.put(b, new Vector<HybridTree[]>());
			HybridTree[] hPair = { t1, t2 };
			forestSets.get(b).add(hPair);
			
			Vector<SparseTree> convForest = convertForest(copyForest(forest));
			for (SparseTree c : t1Components)
				convForest.add(0, new SparseTree(c.getPhyloTree(), "1"));
			for (SparseTree c : t2Components)
				convForest.add(0, new SparseTree(c.getPhyloTree(), "2"));

			MAAFs.add(convForest);

			if (compValue == Computation.EDGE_NUMBER)
				stop = true;

		} else {

			boolean uniqueHPair = true;
			for (HybridTree[] hPair : forestSets.get(b)) {
				if (new IsomorphismCheck().run(t1, hPair[0], taxaOrdering)
						&& new IsomorphismCheck().run(t2, hPair[1], taxaOrdering)) {
					uniqueHPair = false;
					break;
				}
			}
			
			if (uniqueHPair) {
				HybridTree[] hPair = { t1, t2 };
				Vector<SparseTree> convForest = convertForest(copyForest(forest));
				MAAFs.add(convForest);

				for (SparseTree c : t1Components)
					convForest.add(0, new SparseTree(c.getPhyloTree(), "1"));
				for (SparseTree c : t2Components)
					convForest.add(0, new SparseTree(c.getPhyloTree(), "2"));

				forestSets.get(b).add(hPair);

			}
		}

	}

	private void initNodeLabelings(EasyTree forestTree, EasyTree h1, EasyTree h2) {
		int i = 0;
		Vector<String> t1Taxa = getTaxa(forestTree);
		for (EasyNode v : forestTree.getNodes()) {
			if (v.getOutDegree() != 0) {
				while (t1Taxa.contains(String.valueOf(i)))
					i++;
				forestTree.setLabel(v, String.valueOf(i));
				i++;
			}
		}
		Vector<String> t2Taxa = getTaxa(forestTree);
		for (EasyNode v : h1.getNodes()) {
			if (v.getOutDegree() != 0) {
				while (t2Taxa.contains(String.valueOf(i)))
					i++;
				h1.setLabel(v, String.valueOf(i));
				i++;
			}
		}
		Vector<String> t3Taxa = getTaxa(forestTree);
		for (EasyNode v : h2.getNodes()) {
			if (v.getOutDegree() != 0) {
				while (t3Taxa.contains(String.valueOf(i)))
					i++;
				h2.setLabel(v, String.valueOf(i));
				if (v.equals(h2.getRoot()))
					rootLabel = String.valueOf(i);
				i++;
			}
		}
	}

	private Vector<String> getTaxa(EasyTree forestTree) {
		Vector<String> taxa = new Vector<String>();
		for (EasyNode v : forestTree.getLeaves())
			taxa.add(v.getLabel());
		return taxa;
	}

	private void computeMAAF(EasyTree h, Vector<EasyTree> forest, boolean newComponents) {
		recCalls++;

		boolean debug = false;

		if (!easySibMem.contains(h, forest) && h.getLeaves().size() > 2 && !stop) {

			if (forest.size() <= k) {

				EasySiblings sibs = new EasySiblings();
				sibs.init(h, forest, 1);

				boolean validCut = removeSingletons(h, forest, sibs, debug);

				if (!easySibMem.contains(h, forest) && h.getLeaves().size() != 2 && validCut) {

					easySibMem.addEntry(h, forest);

					sibs.updateSiblings(h, forest, true);
					Vector<Vector<String>> t1Siblings = sibs.popSibling();

					for (Vector<String> t1Sib : t1Siblings) {

						sibs.init(h, forest, 1);

						if (debug) {
							System.out.println("\n------ New Recursive Call");
							System.out.println("T1: " + h.getPhyloTree() + ";");
							for (EasyTree t : forest)
								System.out.println(t.getPhyloTree().toBracketString());
							System.out.println(t1Sib.get(0));
						}

						EasyNode v1 = sibs.getForestLeaf(t1Sib.get(0));

						boolean contractSibling = false;
						for (EasyNode c : v1.getParent().getChildren()) {
							if (easySibMem.compareTaxa(c.getLabel(), t1Sib.get(1)))
								contractSibling = true;
						}

						if (contractSibling) {
							if (debug)
								System.out.println("contracting " + t1Sib);

							EasyTree hCopy = copyTree(h);
							Vector<EasyTree> forestCopy = copyForest(forest);
							EasySiblings sibsCopy = new EasySiblings();
							sibsCopy.init(hCopy, forestCopy, 1);
							contractSib(hCopy, forestCopy, t1Sib, sibsCopy);
							computeMAAF(hCopy, forestCopy, false);

							break;

						} else if (forest.size() < k) {
							if (debug)
								System.out.println("2. cutting " + t1Sib);
							cutEdges(copyTree(h), sibs, t1Sib, copyForest(forest), debug);
						} else if (debug)
							System.out.println("3. Nothing\n");

					}

				} else if (h.getLeaves().size() == 2 && forest.size() <= k && !stop) {

					// System.out.println();
					for (EasyTree t : forest) {
						extractTree(t);
						// System.out.println(t.getPhyloTree() + ";");
					}

					Vector<Vector<EasyTree>> refinedForests = refiningForest(forest);

					if (!stop && !refinedForests.isEmpty()) {
						for (Vector<EasyTree> refinedForest : refinedForests) {

							Vector<SparseTree> convForest = convertForest(refinedForest);
							HybridTree hT1 = new HybridTree(t1Restricted, false, taxaOrdering);
							hT1.update();
							HybridTree hT2 = new HybridTree(t2Restricted, false, taxaOrdering);
							hT2.update();
							HybridTree aT1 = new TreeToForestAdaptor().run(convForest, hT1, false, taxaOrdering);
							aT1.update();
							HybridTree aT2 = new TreeToForestAdaptor().run(convForest, hT2, false, taxaOrdering);
							aT2.update();
							addMAAF(refinedForest, aT1, aT2);
						}
					}
				}

				sibs.clear();

			}
		} else if (h.getLeaves().size() == 2 && forest.size() <= k && !stop) {

			// System.out.println();
			for (EasyTree t : forest) {
				extractTree(t);
				// System.out.println(t.getPhyloTree() + ";");
			}

			Vector<Vector<EasyTree>> refinedForests = refiningForest(forest);

			if (!stop && !refinedForests.isEmpty()) {

				for (Vector<EasyTree> refinedForest : refinedForests) {
					Vector<SparseTree> convForest = convertForest(refinedForest);
					HybridTree hT1 = new HybridTree(t1Restricted, false, taxaOrdering);
					hT1.update();
					HybridTree hT2 = new HybridTree(t2Restricted, false, taxaOrdering);
					hT2.update();
					HybridTree aT1 = new TreeToForestAdaptor().run(convForest, hT1, false, taxaOrdering);
					aT1.update();
					HybridTree aT2 = new TreeToForestAdaptor().run(convForest, hT2, false, taxaOrdering);
					aT2.update();
					addMAAF(refinedForest, aT1, aT2);
				}
			}
		}

	}

	private Vector<Vector<EasyTree>> refiningForest(Vector<EasyTree> forest) {
		
		Vector<SparseTree> sparseForest = new Vector<SparseTree>();
		for (EasyTree c : forest)
			sparseForest.add(new SparseTree(c.getPhyloTree()));
		HybridTree aT1 = new TreeToForestAdaptor().run(sparseForest, tree1, false, taxaOrdering);
		aT1.update();
		HybridTree aT2 = new TreeToForestAdaptor().run(sparseForest, tree2, false, taxaOrdering);
		aT2.update();
		CycleRefinement cR = new CycleRefinement(new EasyTree(aT1), new EasyTree(aT2), taxaOrdering, tree1, tree2);
		
//		CycleRefinement cR = new CycleRefinement(new EasyTree(tree1), new EasyTree(tree2), taxaOrdering);
		return cR.run(forest, k, false);
		
	}

	private boolean removeSingletons(EasyTree h1, Vector<EasyTree> t2Forest, EasySiblings sibs, boolean debug) {

		HashSet<String> forestSingeltons = new HashSet<String>();
		for (EasyTree t : t2Forest) {
			if (t.getLeaves().size() == 1 && t.getRoot().getLabel() != "rho")
				forestSingeltons.add(t.getRoot().getLabel());
		}

		Vector<String> deletedTaxa = new Vector<String>();
		boolean isActive = true;
		while (isActive) {
			isActive = false;
			for (String s : forestSingeltons) {
				EasyNode v1 = sibs.getT1Leaf(s);
				if (v1 != null && !deletedTaxa.contains(s)) {
					if (v1.isSolid())
						return false;
					removeNode(h1, v1, sibs, debug);
					deletedTaxa.add(s);
				}
			}
		}

		return true;

	}

	private void removeNode(EasyTree t, EasyNode v, EasySiblings sibs, boolean debug) {
		if (t.getLeaves().size() > 2) {
			EasyNode p = v.getParent();
			if (debug)
				System.out.println("Removing: " + v.getLabel() + " ");
			t.deleteNode(v);
			if (p != null && p.getOutDegree() == 1) {
				if (p.getChildren().get(0).getOutDegree() == 0) {
					EasyNode c = sibs.getForestLeaf(p.getChildren().get(0).getLabel());
					c.setAddedNode(false);
					// if (check(c.getLabel()))
					// System.out.println("1)" + c.getLabel() + " " +
					// c.getOwner().getPhyloTree() + ";");
				}
				p.restrict();
			}
		}
	}

	private void cutEdges(EasyTree h12, EasySiblings sibs, Vector<String> t1Sib, Vector<EasyTree> t2Forest,
			boolean debug) {

		String taxa1 = t1Sib.get(0);
		String taxa2 = t1Sib.get(1);

		EasyTree owner1 = (EasyTree) sibs.getForestLeaf(taxa1).getOwner();
		EasyTree owner2 = (EasyTree) sibs.getForestLeaf(taxa2).getOwner();

		Vector<EasyTree> forestCopy;
		EasyNode cuttingNode;
		if (!owner1.equals(owner2)) {

			if (debug) {
				for (String l : taxaOrdering) {
					if (sibs.getForestLeaf(l) != null)
						System.out.println(l + " -> " + sibs.getForestLeaf(l).getOwner().getPhyloTree() + ";");
				}
				System.out.println("Case-A\n" + owner1.getPhyloTree() + ";\n" + owner2.getPhyloTree() + ";\n" + taxa1
						+ " " + taxa2);
			}

			forestCopy = copyForest(t2Forest);
			cuttingNode = getCuttingNode(forestCopy, sibs, taxa1);
			cuttingNode.setInfo(2);
			cutForest(cuttingNode, forestCopy, copyTree(h12), sibs, t1Sib, debug, false);

			cuttingNode = getCuttingNode(t2Forest, sibs, taxa2);
			cuttingNode.setInfo(2);
			cutForest(cuttingNode, t2Forest, h12, sibs, t1Sib, debug, false);

		} else {

			if (debug)
				System.out.println("Case-C");

			forestCopy = copyForest(t2Forest);
			Vector<Vector<EasyNode>> pendantSet = getPendantNodes(taxa1, taxa2, sibs, forestCopy);

			for (Vector<EasyNode> pendants : pendantSet) {
				for (EasyNode p : pendants)
					p.setInfo(1);
			}
			if (pendantSet.size() != 0)
				cutPendants(pendantSet, forestCopy, copyTree(h12), sibs, t1Sib, debug);

			forestCopy = copyForest(t2Forest);
			cuttingNode = getCuttingNode(forestCopy, sibs, taxa1);
			cuttingNode.setInfo(2);
			cutForest(cuttingNode, forestCopy, copyTree(h12), sibs, t1Sib, debug, true);

			cuttingNode = getCuttingNode(t2Forest, sibs, taxa2);
			cuttingNode.setInfo(2);
			cutForest(cuttingNode, t2Forest, h12, sibs, t1Sib, debug, true);

		}

	}

	private EasyNode getCuttingNode(Vector<EasyTree> forestCopy, EasySiblings sibs, String taxa) {
		sibs.updateTaxa(forestCopy);
		return sibs.getForestLeaf(taxa);
	}

	private Vector<EasyNode> getSinglePendant(Vector<EasyTree> forestCopy, EasySiblings sibs, String taxa) {
		sibs.updateTaxa(forestCopy);
		Vector<EasyNode> pendants = getNeighbours(sibs.getForestLeaf(taxa));
		return pendants;
	}

	private Vector<EasyTree> copyForest(Vector<EasyTree> forest) {
		Vector<EasyTree> forestCopy = new Vector<EasyTree>();
		for (EasyTree t : forest) {
			EasyTree tCopy = new EasyTree(t);
			tCopy.getRoot().setInfo(t.getRoot().getInfo());
			forestCopy.add(tCopy);
		}
		return forestCopy;
	}

	private EasyTree copyTree(EasyTree t) {
		return new EasyTree(t);
	}

	private void cutForest(EasyNode v, Vector<EasyTree> forest, EasyTree t1, EasySiblings sibs, Vector<String> t1Sib,
			boolean debug, boolean sameOwner) {

		// sibs.updateTaxa(t1);
		EasyTree t = (EasyTree) v.getOwner();

		if (t.getLeaves().size() > 1) {
			Vector<EasyNode> neighbours = getNeighbours(v);
			if (neighbours.size() != 1 || !neighbours.get(0).getLabel().equals("rho")) {

				if (neighbours.size() == 1) {
					neighbours.get(0).setAddedNode(false);
					// if (check(neighbours.get(0).getLabel()))
					// System.out.println("2)" + neighbours.get(0).getLabel() +
					// " " +
					// neighbours.get(0).getOwner().getPhyloTree() + ";");
				}

				String label = v.getLabel();
				EasyTree subtree = t.pruneSubtree(v);
				subtree.getRoot().setLabel(label);

				forest.add(0, subtree);
				if (debug) {
					System.out.println("Forest-A");
					for (EasyTree c : forest)
						System.out.println(c.getPhyloTree() + ";");
					System.out.println();
				}

				computeMAAF(t1, forest, true);

			}

		}

	}

	private void cutPendants(Vector<Vector<EasyNode>> pendantSet, Vector<EasyTree> forest, EasyTree t1,
			EasySiblings sibs, Vector<String> t1Sib, boolean debug) {

		EasyNode sib1 = sibs.getForestLeaf(t1Sib.get(0));
		EasyNode sib2 = sibs.getForestLeaf(t1Sib.get(1));
		int h1 = getHeight(sib1);
		int h2 = getHeight(sib2);

		for (Vector<EasyNode> pendants : pendantSet) {
			Vector<EasyTree> subtrees = new Vector<EasyTree>();
			for (EasyNode v : pendants) {
				EasyTree t = (EasyTree) v.getOwner();
				if (t.getLeaves().size() > 1) {
					EasyNode p = v.getParent();
					Vector<EasyNode> neighbours = getNeighbours(v);
					if (neighbours.size() != 1 || !neighbours.get(0).getLabel().equals("rho")) {

						String label = v.getLabel();
						EasyTree subtree = t.pruneSubtree(v);
						t.getRoot().setLabel(label);
						subtrees.add(subtree);

					}
				}
			}
			if (subtrees.size() == 1) {
				forest.add(0, subtrees.firstElement());
			} else {
				EasyNode root = new EasyNode(null, null, cmpUniqueLabel(forest));
				EasyTree t = new EasyTree(root);
				for (EasyTree subtree : subtrees)
					t.getRoot().addSubtree(subtree);

				forest.add(0, t);
			}
		}
		sibs.updateTaxa(t1);

		if (getHeight(sib1) < h1) {
			sib1.setAddedNode(false);
			// System.out.println("1) "+sib1.getLabel()+" "+sib1.isAddedNode());
			// System.out.println(sib1.getLabel()+" "+sib2.getLabel()+"\n"+sib1.getOwner().getPhyloTree()+";");
			// if (check(sib1.getLabel()))
			// System.out.println("3)" + sib1.getLabel() + " " +
			// sib1.getOwner().getPhyloTree() + ";");
		}
		if (getHeight(sib2) < h2) {
			sib2.setAddedNode(false);
			// System.out.println("2) "+sib2.getLabel()+" "+sib2.isAddedNode());
			// System.out.println(sib1.getLabel() + " " + sib2.getLabel() + "\n"
			// + sib2.getOwner().getPhyloTree() + ";");
			// if (check(sib2.getLabel()))
			// System.out.println("4)" + sib2.getLabel() + " " +
			// sib2.getOwner().getPhyloTree() + ";");
		}

		contractSib(t1, forest, t1Sib, sibs);

		if (debug) {
			System.out.println("Forest-B");
			for (EasyTree c : forest)
				System.out.println(c.getPhyloTree() + ";");
			System.out.println(t1.getPhyloTree() + ";\n");
		}

		computeMAAF(t1, forest, true);

	}

	// private boolean check(String label) {
	// if (label.length() == 7 && label.contains("d") && label.contains("i") &&
	// label.contains("j") && label.contains("g") )
	// return true;
	// return false;
	// }

	private String cmpUniqueLabel(Vector<EasyTree> forest) {
		HashSet<String> labelings = new HashSet<String>();
		for (EasyTree c : forest) {
			for (EasyNode v : c.getNodes())
				labelings.add(v.getLabel());
		}
		int i = 0;
		while (labelings.contains(String.valueOf(i)))
			i++;
		return String.valueOf(i);
	}

	private Vector<EasyNode> getNeighbours(EasyNode v) {
		Vector<EasyNode> neighbours = new Vector<EasyNode>();
		for (EasyNode c : v.getParent().getChildren()) {
			if (!c.equals(v))
				neighbours.add(c);
		}
		return neighbours;
	}

	private String getOnlyPendant(EasyNode v1, EasyNode v2, EasyTree owner1) {

		EasyNode p1 = v1.getParent();
		EasyNode p2 = v2.getParent();

		if (p1.getInDegree() == 1) {
			EasyNode v = p1.getParent();
			if (v.equals(p2))
				return v1.getLabel();
		}

		return null;
	}

	private Vector<Vector<EasyNode>> getPendantNodes(String taxa1, String taxa2, EasySiblings sibs,
			Vector<EasyTree> forestCopy) {

		sibs.updateTaxa(forestCopy);
		Vector<Vector<EasyNode>> pendantSet = new Vector<Vector<EasyNode>>();
		EasyNode v1 = sibs.getForestLeaf(taxa1);
		EasyNode v2 = sibs.getForestLeaf(taxa2);

		Vector<EasyNode> upperNodesV1 = getUpperNodes(v1);
		Vector<EasyNode> upperNodesV2 = getUpperNodes(v2);

		EasyNode pendant = v1;
		EasyNode p = pendant.getParent();
		while (!upperNodesV2.contains(p)) {
			pendantSet.add(getNeighbours(pendant));
			pendant = p;
			p = pendant.getParent();
		}

		pendant = v2;
		p = pendant.getParent();
		while (!upperNodesV1.contains(p)) {
			pendantSet.add(getNeighbours(pendant));
			pendant = p;
			p = pendant.getParent();
		}

		return pendantSet;
	}

	private Vector<EasyNode> getUpperNodes(EasyNode v1) {
		Vector<EasyNode> upperNodes = new Vector<EasyNode>();
		while (v1.getInDegree() == 1) {
			v1 = v1.getParent();
			upperNodes.add(v1);
		}
		return upperNodes;
	}

	private void contractSib(EasyTree t, Vector<EasyTree> forest, Vector<String> t1Sib, EasySiblings sibs) {
		contractLeaves(t, t1Sib, sibs);
		sibs.updateTaxa(t, forest);
	}

	private void contractLeaves(EasyTree h1, Vector<String> t1Sib, EasySiblings sibs) {

		EasyNode v1 = sibs.getT1Leaf(t1Sib.get(0));
		EasyNode v2 = sibs.getT1Leaf(t1Sib.get(1));
		EasyNode p = v1.getParent();

		// contracting h1
		Vector<String> taxa = new Vector<String>();
		taxa.add(v1.getLabel());
		taxa.add(v2.getLabel());
		Collections.sort(taxa);
		String s = "";
		s = s.concat(taxa.get(0));
		s = s.concat("+");
		s = s.concat(taxa.get(1));

		boolean isMultiNode = false;
		if (p.getOutDegree() == 2) {
			h1.deleteNode(v1);
			h1.deleteNode(v2);
			h1.setLabel(p, s);
			easySibMem.addTreeLabel(s);
		} else {
			isMultiNode = true;
			h1.deleteNode(v2);
			h1.setLabel(v1, s);
			easySibMem.addTreeLabel(s);
		}

		// contracting forest
		EasyNode f1 = sibs.getForestLeaf(t1Sib.get(0));
		EasyNode f2 = sibs.getForestLeaf(t1Sib.get(1));
		EasyNode fP = f1.getParent();
		EasyTree f = (EasyTree) f1.getOwner();

		EasyNode[] contractedNodes = { f1, f2 };

		// System.out.println(t1Sib.get(0)+" "+t1Sib.get(1));
		// System.out.println(h1.getPhyloTree()+";");
		// System.out.println(f.getPhyloTree()+";\n"+fP.getLabel()+" "+fP.getOutDegree()+"\n");

		if (fP.getOutDegree() == 2) {
			f.deleteNode(f1);
			f.deleteNode(f2);
			f.setLabel(fP, s);
			fP.setContractedNodes(contractedNodes);
			// if(check(fP.getLabel()) && !isMultiNode)
			// System.out.println("95) "+fP.getLabel()+"\n"+f.getPhyloTree()+";");
		} else {
			f.deleteNode(f1);
			f.deleteNode(f2);
			EasyNode fC = new EasyNode(fP, f, s);
			fC.setContractedNodes(contractedNodes);
			fC.setAddedNode(isMultiNode);
			// if(check(fC.getLabel()) && !isMultiNode)
			// System.out.println("96) "+fC.getLabel()+"\n"+f.getPhyloTree()+";");
		}

//		taxonToTaxa.put(s, taxa);
		easySibMem.addTaxon(s, taxa);

	}

	private int getHeight(EasyNode v) {
		int h = 0;
		if (v.getInDegree() != 0) {
			EasyNode p = v.getParent();
			h = 1;
			while (p.getInDegree() != 0) {
				p = p.getParent();
				h++;
			}
			return h;
		}
		return h;
	}

	@SuppressWarnings("unchecked")
	private void extractTree(EasyTree t) {
		Vector<EasyNode> leaveSet = (Vector<EasyNode>) t.getLeaves().clone();
		for (EasyNode leaf : leaveSet) {
			if (leaf.getContractedNodes() != null)
				extractNode(t, leaf);
		}
	}

	private void extractNode(EasyTree t, EasyNode v) {
		EasyNode v1 = v.getContractedNodes()[0];
		EasyNode v2 = v.getContractedNodes()[1];
		v.addChild(v1);
		v.addChild(v2);

		Iterator<EasyNode> it = ((Vector<EasyNode>) v1.getChildren().clone()).iterator();
		while (it.hasNext())
			v1.removeChild(it.next());
		it = ((Vector<EasyNode>) v2.getChildren().clone()).iterator();
		while (it.hasNext())
			v2.removeChild(it.next());

		if (v.isAddedNode() && v.getParent() != null) {
			EasyNode p = v.getParent();
			p.removeChild(v);
			p.addChild(v1);
			p.addChild(v2);
		}

		if (v1.getContractedNodes() != null)
			extractNode(t, v1);
		if (v2.getContractedNodes() != null)
			extractNode(t, v2);
	}

	private Vector<SparseTree> convertForest(Vector<EasyTree> forest) {
		Vector<SparseTree> convForest = new Vector<SparseTree>();
		for (EasyTree c : forest) {
			SparseTree t = new SparseTree(convertTree(c), c.getInfo());
			removeOneNodes(t);
			convForest.add(t);
		}
		return convForest;
	}

	private void removeOneNodes(SparseTree t) {
		Iterator<SparseTreeNode> it = t.getNodes().iterator();
		while (it.hasNext())
			removeOneNode(it.next(), t);
	}

	private void removeOneNode(SparseTreeNode v, SparseTree t) {
		if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
			SparseTreeNode p = v.getParent();
			SparseTreeNode c = v.getChildren().firstElement();
			t.deleteNode(v);
			p.addChild(c);
		}
	}

	private HybridTree convertTree(EasyTree t) {
		HybridTree h = new HybridTree(t.getPhyloTree(), false, taxaOrdering);
		h.update();
		return h;
	}

	public void setStop(boolean stop) {
		this.stop = stop;
	}

	public Vector<String> getTaxa(EasyNode v) {
		Vector<String> taxa = new Vector<String>();
		for (EasyNode c : v.getChildren()) {
			if (c.getOutDegree() == 0) {
				taxa.add(c.getLabel());
			}
		}
		return taxa;
	}

	public int getRecCalls() {
		return recCalls;
	}
}
