package dendroscope.hybroscale.model.cmpAllMAAFs;

import dendroscope.hybroscale.model.cmpMinNetworks.NetworkIsomorphismCheck;
import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.model.treeObjects.SparseTree;
import dendroscope.hybroscale.model.treeObjects.SparseTreeNode;
import dendroscope.hybroscale.util.graph.MyNode;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Vector;

public class AcyclicOrderingChecker {

	public Vector<Vector<SparseTree>> run(HybridTree h1, HybridTree h2, Vector<Vector<SparseTree>> MAAFs) {

		for (Vector<SparseTree> MAAF : MAAFs)
			adjustOrdering(MAAF, h1, h2);

		// return MAAFs;
		Vector<Vector<SparseTree>> newTaxaOrderings = new Vector<Vector<SparseTree>>();
		for (Vector<SparseTree> MAAF : MAAFs)
			cmpNewOrderings(MAAF, h1, h2, newTaxaOrderings);
		return filterMAAFs(newTaxaOrderings);

	}

	private Vector<Vector<SparseTree>> filterMAAFs(Vector<Vector<SparseTree>> newTaxaOrderings) {

		Vector<Vector<SparseTree>> multipleMAAFs = new Vector<Vector<SparseTree>>();
		for (int i = 0; i < newTaxaOrderings.size() - 1; i++) {
			for (int j = i + 1; j < newTaxaOrderings.size(); j++) {
				if (compareMAAFs(newTaxaOrderings.get(i), newTaxaOrderings.get(j)))
					multipleMAAFs.add(newTaxaOrderings.get(j));
			}
		}

		for (Vector<SparseTree> mulMAAF : multipleMAAFs)
			newTaxaOrderings.remove(mulMAAF);

		return newTaxaOrderings;
	}

	private boolean compareMAAFs(Vector<SparseTree> MAAF1, Vector<SparseTree> MAAF2) {

		if (MAAF1.size() != MAAF2.size())
			return false;
		else {
			for (int i = 0; i < MAAF1.size(); i++) {
				SparseNetwork c1 = new SparseNetwork(MAAF1.get(i).getPhyloTree());
				SparseNetwork c2 = new SparseNetwork(MAAF2.get(i).getPhyloTree());
				if (!new NetworkIsomorphismCheck().run(c1, c2))
					return false;
			}
		}

		return true;
	}

	private void cmpNewOrderings(Vector<SparseTree> MAAF, HybridTree h1, HybridTree h2,
			Vector<Vector<SparseTree>> newTaxaOrderings) {

//		 System.out.println("\nForest:");
//		 for (SparseTree c : MAAF)
//		 System.out.println(c.getPhyloTree().toMyBracketString() + " " +
//		 c.getInfo());

		HybridTree[] trees = { h1, h2 };

		HashMap<Integer, Vector<SparseTree>> switchingGroups = new HashMap<Integer, Vector<SparseTree>>();
		Vector<SparseTree> selectedComponents = new Vector<SparseTree>();
		for (int i = MAAF.size() - 1; i >= 0; i--) {

			SparseTree c1 = MAAF.get(i);

			if (!selectedComponents.contains(c1)) {

				Vector<SparseTree> switchingGroup = new Vector<SparseTree>();
				switchingGroup.add(c1);

				for (int j = i - 1; j >= 0; j--) {

					SparseTree c2 = MAAF.get(j);

					Boolean[] doSwitch = new Boolean[2];
					int counter = 0;
					for (HybridTree t : trees) {

						MyNode lca1 = t.findLCA(getTreeSet(c1, t.getTaxaOrdering()));
                        lca1 = lca1.getOutDegree() == 0 ? lca1.getFirstInEdge().getSource() : lca1;
						BitSet b1 = t.getNodeToCluster().get(lca1);
                        MyNode p1 = lca1.getInDegree() != 0 ? lca1.getFirstInEdge().getSource() : null;

						MyNode lca2 = t.findLCA(getTreeSet(c2, t.getTaxaOrdering()));
                        lca2 = lca2.getOutDegree() == 0 ? lca2.getFirstInEdge().getSource() : lca2;
						BitSet b2 = t.getNodeToCluster().get(lca2);
                        MyNode p2 = lca2.getInDegree() != 0 ? lca2.getFirstInEdge().getSource() : null;

						boolean notTwoSingletons = c1.getNodes().size() > 1 || c2.getNodes().size() > 1;
						if (b1.equals(b2)
								|| (notTwoSingletons && ((p1 != null && p1.equals(lca2)) || (p2 != null && p2
										.equals(lca1))))) {
							// System.out.println("TRUE: "+c1.getPhyloTree()+" "+c2.getPhyloTree());
							doSwitch[counter] = true;
						}
						if (isSubcomponent(c1, c2, t) || isSubcomponent(c2, c1, t)) {
							// System.out.println("FALSE: "+c1.getPhyloTree()+" "+c2.getPhyloTree());
							doSwitch[counter] = false;
						}

						counter++;

					}

					if (((doSwitch[0] == null || doSwitch[0]) && (doSwitch[1] == null || doSwitch[1]))
							&& (doSwitch[0] != null || doSwitch[1] != null)) {
						switchingGroup.add(c2);
						selectedComponents.add(c2);
					}

				}

				if (switchingGroup.size() > 1) {
					int minIndex = Integer.MAX_VALUE;
					for (SparseTree c : switchingGroup) {
						int index = MAAF.indexOf(c);
						minIndex = index < minIndex ? index : minIndex;
					}
					switchingGroups.put(minIndex, switchingGroup);
				}

			}

		}

//		 if (switchingGroups.size() > 0) {
//		 System.out.println();
//		 for (Vector<SparseTree> f : switchingGroups.values()) {
//		 for (SparseTree c : f)
//		 System.out.println(c.getPhyloTree() + ";");
//		 System.out.println();
//		 }
//		 System.out.println("--");
//		 }

		if (switchingGroups.size() == 0)
			newTaxaOrderings.add(MAAF);
		else {

			Vector<Vector<SparseTree>> recMAAFs = new Vector<Vector<SparseTree>>();
			recMAAFs.add(MAAF);

			for (int key : switchingGroups.keySet()) {
				Vector<Vector<SparseTree>> newMAAFs = new Vector<Vector<SparseTree>>();
				for (Vector<SparseTree> recMAAF : recMAAFs) {

					Vector<SparseTree> MAAFClone = new Vector<SparseTree>();
					for (SparseTree c : recMAAF)
						MAAFClone.add(new SparseTree(c));

					Vector<Integer> insertIndices = new Vector<Integer>();
					insertIndices.add(key);

					for (SparseTree c : switchingGroups.get(key))
						removeComponent(c, MAAFClone, h1.getTaxaOrdering());

					generateTaxaAllOrderingsRec(0, insertIndices, switchingGroups, MAAFClone, newMAAFs, h1, h2);

				}
				// recMAAFs = newMAAFs;
				recMAAFs.clear();
				recMAAFs.addAll(newMAAFs);
			}

			newTaxaOrderings.addAll(recMAAFs);

		}

	}

	private void removeComponent(SparseTree c1, Vector<SparseTree> MAAF, Vector<String> taxaOrdering) {
		BitSet b1 = getTreeSet(c1, taxaOrdering);
		SparseTree c = null;
		for (SparseTree c2 : MAAF) {
			BitSet b2 = getTreeSet(c2, taxaOrdering);
			if (b1.equals(b2))
				c = c2;
		}
		if (c != null)
			MAAF.remove(c);
	}

	private void generateTaxaAllOrderingsRec(int i, Vector<Integer> insertIndices,
			HashMap<Integer, Vector<SparseTree>> switchingGroups, Vector<SparseTree> MAAF,
			Vector<Vector<SparseTree>> newTaxaOrderings, HybridTree h1, HybridTree h2) {
		if (i < insertIndices.size()) {
			Vector<Vector<Integer>> allOrderings = new Vector<Vector<Integer>>();
			generateAllOrderingsRec(new Vector<Integer>(), switchingGroups.get(insertIndices.get(i)).size(),
					allOrderings);
			for (Vector<Integer> ordering : allOrderings) {

				Vector<SparseTree> MAAFClone = new Vector<SparseTree>();
				for (SparseTree c : MAAF)
					MAAFClone.add(new SparseTree(c));

				Vector<SparseTree> treeOrdering = new Vector<SparseTree>();
				for (int j : ordering)
					treeOrdering.add(switchingGroups.get(insertIndices.get(i)).get(j));

				MAAFClone.addAll(insertIndices.get(i), treeOrdering);

				int newI = i + 1;
				generateTaxaAllOrderingsRec(newI, insertIndices, switchingGroups, MAAFClone, newTaxaOrderings, h1, h2);
			}
		} else {

			// System.out.println("\n+++");
			// for (SparseTree c : MAAF) {
			// System.out.println(c.getPhyloTree() + ";");
			// }

			adjustOrdering(MAAF, h1, h2);

			// for (SparseTree c : MAAF) {
			// System.out.println(c.getPhyloTree() + ";");
			// }
			// System.out.println("+++");

			newTaxaOrderings.add(MAAF);
		}

	}

	private void generateAllOrderingsRec(Vector<Integer> v, int k, Vector<Vector<Integer>> allOrderings) {
		if (v.size() < k) {
			for (int i = 0; i < k; i++) {
				if (!v.contains(i)) {
					Vector<Integer> vClone = (Vector<Integer>) v.clone();
					vClone.add(i);
					generateAllOrderingsRec(vClone, k, allOrderings);
				}
			}
		} else
			allOrderings.add(v);
	}

	private void adjustOrdering(Vector<SparseTree> MAAF, HybridTree h1, HybridTree h2) {

		HybridTree[] trees = { h1, h2 };
		Vector<SparseTree[]> switchPairs = new Vector<SparseTree[]>();

		for (int i = MAAF.size() - 1; i >= 0; i--) {

			SparseTree c1 = MAAF.get(i);

			for (int j = i - 1; j >= 0; j--) {

				SparseTree c2 = MAAF.get(j);

				Boolean[] doSwitch = new Boolean[2];
				int counter = 0;
				for (HybridTree t : trees) {

					if (isSubcomponent(c1, c2, t))
						doSwitch[counter] = true;

					if (isSubcomponent(c2, c1, t))
						doSwitch[counter] = false;

					counter++;

				}

				if (((doSwitch[0] == null || doSwitch[0]) && (doSwitch[1] == null || doSwitch[1]))
						&& (doSwitch[0] != null || doSwitch[1] != null)) {
					SparseTree[] switchPair = { c1, c2 };
					switchPairs.add(switchPair);

				}

			}

		}

		for (SparseTree[] pair : switchPairs) {

			int index1 = MAAF.indexOf(pair[0]);
			int index2 = MAAF.indexOf(pair[1]);

			if (index2 < index1) {

				// boolean doShift = true;
				// for (int k = index1 - 1; k > index2; k--) {
				// if (isIllegalShift(pair[0], MAAF.get(k), trees)) {
				// doShift = false;
				// break;
				// }
				// }
				//
				// if (doShift) {
				//
				// System.out.println("---");
				// System.out.println(pair[0].getPhyloTree() + " vs " +
				// pair[1].getPhyloTree() + "\n---");
				// for (SparseTree c : MAAF)
				// System.out.println(c.getPhyloTree() + ";");
				//
				// MAAF.remove(pair[0]);
				// MAAF.add(index2, pair[0]);
				//
				// for (SparseTree c : MAAF)
				// System.out.println(c.getPhyloTree() + ";");
				// System.out.println("---");
				//
				// adjustOrdering(MAAF, h1, h2);
				//
				// }

				Vector<SparseTree> subComponents = new Vector<SparseTree>();
				for (int k = index2 + 1; k < index1; k++) {
					if (isSubcomponent(MAAF.get(k), pair[0], h1) || isSubcomponent(MAAF.get(k), pair[0], h2))
						subComponents.add(MAAF.get(k));
				}

				// System.out.println("---");
				// System.out.println(pair[0].getPhyloTree() + " vs " +
				// pair[1].getPhyloTree() + "\n---");
				// for (SparseTree c : MAAF)
				// System.out.println(c.getPhyloTree() + ";");

				MAAF.remove(pair[0]);
				MAAF.add(index2, pair[0]);

				for (SparseTree c : subComponents) {
					MAAF.remove(c);
					MAAF.add(index2, c);
				}

				// for (SparseTree c : MAAF)
				// System.out.println(c.getPhyloTree() + ";");
				// System.out.println("---");

				// adjustOrdering(MAAF, h1, h2);

			}

		}

	}

	private boolean isSubcomponent(SparseTree c2, SparseTree c1, HybridTree t) {

		if (c1.getNodes().size() > 1 || c2.getNodes().size() > 1) {

			MyNode lca1 = t.findLCA(getTreeSet(c1, t.getTaxaOrdering()));
			MyNode lca2 = t.findLCA(getTreeSet(c2, t.getTaxaOrdering()));

			BitSet b1 = t.getNodeToCluster().get(lca1);
			BitSet b2 = t.getNodeToCluster().get(lca2);

			BitSet c1Set = getTreeSet(c1, t.getTaxaOrdering());

			if (!lca1.equals(lca2) && b1.intersects(b2) && b1.cardinality() > b2.cardinality()) {
				MyNode v = lca2;
				while (v != lca1) {
					BitSet b = t.getNodeToCluster().get(v);
					if (b.intersects(c1Set))
						return true;
                    v = v.getFirstInEdge().getSource();
				}
			}

		}

		return false;
	}

	private BitSet getTreeSet(SparseTree t, Vector<String> taxaOrdering) {
		BitSet b = new BitSet();
		for (SparseTreeNode l : t.getLeaves())
			b.set(taxaOrdering.indexOf(l.getLabel()));
		return b;
	}

}
