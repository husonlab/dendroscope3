package dendroscope.hybroscale.model.cmpMinNetworks;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import dendroscope.hybroscale.model.HybridManager.Computation;
import dendroscope.hybroscale.model.treeObjects.HybridNetwork;
import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.model.treeObjects.SparseNetEdge;
import dendroscope.hybroscale.model.treeObjects.SparseNetNode;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.model.treeObjects.SparseTree;
import dendroscope.hybroscale.model.treeObjects.SparseTreeNode;
import dendroscope.hybroscale.model.util.RetEdgeComparator;
import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;

public class DFSComputeUpdatedNetworks extends Thread {

	private HybridTree[] trees;
	private HybridTree t1;
	private int firstIndex;
	private Vector<SparseTree> forest;
	private int forestSize;
	private int insertIndex;
	private HashSet<Integer> currIndices, lastIndices;
	private Computation compValue;
	private SparseTree[] inputTrees;
	private HybridTree[] hybridTrees;
	private DFSNetworkReporter reporter;
	private int myPriority;

	private Vector<ConcurrentHashMap<Integer, BitSet>> clusterConstraints;
	private Vector<ConcurrentHashMap<BitSet, Vector<SparseNetNode>>> clusterToNode;
	private Vector<ConcurrentHashMap<SparseNetNode, BitSet>> nodeToCluster;
	private Vector<HashSet<BitSet>> clusters;
	private ConcurrentHashMap<Integer, BitSet> indexToInsertedSet;
	private ConcurrentHashMap<String, Integer> taxonToIndex;
	private Vector<String> taxaOrdering;
	private BitSet compSet;
	private boolean isStoppped = false;
	private ConcurrentHashMap<Integer, Vector<String>> indexToTaxa, prevTreeToTaxa;

	private boolean speedUp;
	private boolean debug = false;
	private boolean doContainmentCheck = false;

	private SparseNetwork n;
	private Vector<SparseNetEdge> treeEdges;

	private int counter = 0;
	private Vector<SparseNetwork> repNetworks = new Vector<SparseNetwork>();
	private Vector<SparseNetwork> recNetworks = new Vector<SparseNetwork>();
	private Vector<String> recInfos = new Vector<String>();

	boolean relevant = false;

	private String log = "";

	public DFSComputeUpdatedNetworks(Vector<SparseTree> forest, SparseNetwork n, HybridTree[] hTreePair,
			Vector<SparseNetEdge> treeEdges, int treeInsertIndex, Vector<String> taxaOrdering, Computation compValue,
			SparseTree[] inputTrees, HybridTree[] hybridTrees, boolean heuristicMode, boolean speedUp, boolean debug,
			DFSNetworkReporter reporter, int myPriority) {

		this.n = n;
		this.treeEdges = treeEdges;
		this.firstIndex = 0;
		this.trees = hTreePair;
		this.t1 = hTreePair[firstIndex];
		this.taxaOrdering = (Vector<String>) taxaOrdering.clone();
		this.debug = debug;
		this.forest = forest;
		this.insertIndex = treeInsertIndex;
		this.compValue = compValue;
		this.inputTrees = inputTrees;
		this.hybridTrees = hybridTrees;
		this.speedUp = speedUp;
		this.reporter = reporter;
		this.myPriority = myPriority;

		this.forestSize = 0;
		for (SparseTree c : forest) {
			if (c.getInfo().isEmpty())
				this.forestSize++;
		}

		uniqueNodeLabels(n);

	}

	public void run() {

//		log = log.concat("Forest:\n");
//		for (SparseTree c : forest)
//			log = log.concat(c.getPhyloTree() + ";\n");
//		log = log.concat("---\n");

		try {

			if (singleNodeCheck(n))
				System.out.println("OneNode-Begin: " + n.getPhyloTree() + ";");

			this.debug = false;

			lastIndices = new HashSet<Integer>();
			for (int i = 0; i < insertIndex; i++)
				lastIndices.add(i);

			taxonToIndex = new ConcurrentHashMap<String, Integer>();
			for (String taxon : taxaOrdering)
				taxonToIndex.put(taxon, taxaOrdering.indexOf(taxon));

			indexToTaxa = new ConcurrentHashMap<Integer, Vector<String>>();
			for (int i = 0; i < trees.length; i++) {
				indexToTaxa.put(i, new Vector<String>());
				for (MyNode leaf : trees[i].getLeaves())
					indexToTaxa.get(i).add(leaf.getLabel());
			}

			prevTreeToTaxa = new ConcurrentHashMap<Integer, Vector<String>>();
			// for (int i = 0; i < insertIndex; i++) {
			for (int i = 0; i < insertIndex + 1; i++) {
				prevTreeToTaxa.put(i, new Vector<String>());
				for (MyNode leaf : hybridTrees[i].getLeaves())
					prevTreeToTaxa.get(i).add(leaf.getLabel());
			}

			// prevTreeContainmentCheck(n,"Begin");

			if (debug) {
				System.out.println("*******");
				System.out.println("First Index: " + firstIndex);
				System.out.println(t1);
				System.out.println(n.getPhyloTree() + ";");
			}

			// initialize fields
			clusterToNode = new Vector<ConcurrentHashMap<BitSet, Vector<SparseNetNode>>>();
			clusters = new Vector<HashSet<BitSet>>();
			nodeToCluster = new Vector<ConcurrentHashMap<SparseNetNode, BitSet>>();
			clusterConstraints = new Vector<ConcurrentHashMap<Integer, BitSet>>();
			for (int i = 0; i < trees.length; i++) {
				clusterToNode.add(new ConcurrentHashMap<BitSet, Vector<SparseNetNode>>());
				nodeToCluster.add(new ConcurrentHashMap<SparseNetNode, BitSet>());
				clusters.add(new HashSet<BitSet>());
				clusterConstraints.add(new ConcurrentHashMap<Integer, BitSet>());
			}

			compSet = new BitSet(taxaOrdering.size());
			indexToInsertedSet = new ConcurrentHashMap<Integer, BitSet>();
			BitSet b = new BitSet(taxaOrdering.size());
			for (int i = forest.size() - 2; i >= 0; i--) {
				SparseTree comp = forest.get(i);
				BitSet compCluster = new BitSet(taxaOrdering.size());
				for (SparseTreeNode leaf : comp.getLeaves()) {
					b.set(taxaOrdering.indexOf(leaf.getLabel()));
					compCluster.set(taxaOrdering.indexOf(leaf.getLabel()));
				}
				compSet.or(compCluster);
				if (compCluster.cardinality() > 1) {
					int j = compCluster.nextSetBit(0);
					while (j != -1) {
						clusterConstraints.get(1).put(j, (BitSet) compCluster.clone());
						j = compCluster.nextSetBit(j + 1);
					}
				}
			}
			BitSet inserted = new BitSet(taxaOrdering.size());
			for (SparseTreeNode leaf : forest.lastElement().getLeaves())
				inserted.set(taxaOrdering.indexOf(leaf.getLabel()), true);
			indexToInsertedSet.put(1, inserted);

			BitSet insertedFirstIndex = new BitSet(taxaOrdering.size());
			for (MyNode leaf : t1.getLeaves())
				insertedFirstIndex.set(taxaOrdering.indexOf(leaf.getLabel()), true);
			indexToInsertedSet.put(firstIndex, insertedFirstIndex);

			// compute first network
			SparseNetwork newN = n;
			for (SparseNetEdge e : newN.getEdges()) {
				if (treeEdges.contains(e)) {
					HashSet<Integer> v = new HashSet<Integer>();
					v.add(firstIndex);
					e.addEdgeIndices(v);
				} else {
					HashSet<Integer> v = new HashSet<Integer>();
					v.add(-1);
					e.addEdgeIndices(v);
				}
			}
			updateNetwork(newN, null, null);

			// adding component to network
			computeNetworksRec(newN, forest.size() - 2);

			reporter.reportFinishing();
			freeMemory();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void computeNetworksRec(SparseNetwork newNet, int i) {

		if (i >= 0 && !isStoppped) {

			// updating inserted set
			indexToInsertedSet.put(1, new BitSet());
			for (int k = forest.size() - 1; k > i; k--) {
				SparseTree comp = forest.get(k);
				BitSet insertedSet = (BitSet) indexToInsertedSet.get(1).clone();
				BitSet compCluster = getTreeCluster(comp);
				insertedSet.or(compCluster);
				indexToInsertedSet.remove(1);
				indexToInsertedSet.put(1, insertedSet);
			}

//			log = log.concat("Adding: " + forest.get(i).getPhyloTree() + "\n" + newNet.getPhyloTree() + ";\n");

			// System.out.println("Adding: " + forest.get(i).getPhyloTree() +
			// "\n" + newNet.getPhyloTree() + ";");

			// adding new component
			// System.out.println("Adding " +
			// forest.get(i).getPhyloTree().toMyBracketString());
			SparseTree comp = forest.get(i);
			Vector<Integer> indices = new Vector<Integer>();
			indices.add(1);

			Vector<SparseNetwork> nextNetworks = addComponent(newNet, (Vector<Integer>) indices.clone(), firstIndex,
					comp, null, null, new BitSet(), (Vector<Integer>) indices.clone(), null, null, null);

			int nextI = i - 1;
			for (SparseNetwork nextNet : nextNetworks) {
				computeNetworksRec(nextNet, nextI);
			}

			nextNetworks = null;

		} else if (!isStoppped && prevTreeContainmentCheck(newNet)) {

			indexToInsertedSet.put(1, new BitSet());
			for (int k = forest.size() - 1; k >= 0; k--) {
				SparseTree comp = forest.get(k);
				BitSet insertedSet = (BitSet) indexToInsertedSet.get(1).clone();
				BitSet compCluster = getTreeCluster(comp);
				insertedSet.or(compCluster);
				indexToInsertedSet.remove(1);
				indexToInsertedSet.put(1, insertedSet);
			}

			if (compValue == Computation.EDGE_NUMBER && insertIndex == inputTrees.length - 1 && i == 0)
				isStoppped = true;

			// update final edge-mapping for each computed network

			uniqueNodeLabels(newNet);
			BitSet rootCluster = (BitSet) indexToInsertedSet.get(1).clone();
			updateEdges(newNet, rootCluster, firstIndex, 1);
			Vector<SparseNetwork> modNetworks = new Vector<SparseNetwork>();

			if (singleNodeCheck(newNet))
				System.out.println("OneNode-Mid: " + newNet.getPhyloTree() + ";");

			// System.out.println("> "+newNet.getPhyloTree() + ";");
			// System.out.println("> "+newNet.getPhyloTree() + ";");

			modT2Edges(newNet, modNetworks, new Vector<String>(), new Vector<String>(), new Vector<String>());

			// System.out.println(">> "+newNet.getPhyloTree() + ";");

			for (SparseNetwork modNet : modNetworks) {

				for (SparseNetEdge e : modNet.getEdges()) {
					if (modNet.isSpecial(e) && e.getEdgeIndex().contains(1)) {
						if (e.getTarget().getOutEdges().get(0).getEdgeIndex().contains(1))
							e.addIndex(insertIndex);
					}
				}

				Vector<String> t1Taxa = new Vector<String>();
				for (MyNode leaf : trees[1].getLeaves())
					t1Taxa.add(leaf.getLabel());
				for (SparseNetNode leaf : modNet.getLeaves()) {
					if (t1Taxa.contains(leaf.getLabel()))
						updateRootPath(insertIndex, leaf, modNet);
				}

				boolean makesSense = true;
				for (SparseNetNode v : modNet.getNodes()) {
					if (v.getInDegree() > 1) {
						Vector<SparseNetNode> parents = new Vector<SparseNetNode>();
						for (SparseNetEdge e : v.getInEdges()) {
							SparseNetNode p = e.getSource().getInEdges().get(0).getSource();
							if (parents.contains(p))
								makesSense = false;
							else
								parents.add(p);
						}
					}
				}

				if (makesSense) {

					if (speedUp || insertIndex == hybridTrees.length - 1) {

						Vector<SparseNetwork> updatedNetworks = new UpdateEmbeddings().run(hybridTrees, insertIndex,
								modNet, taxaOrdering);

						// System.out.println("\n"+modNet.getPhyloTree().toMyBracketString());
						// for (SparseNetwork upNet : updatedNetworks)
						// System.out.println(upNet.getPhyloTree().toMyBracketString());

						for (SparseNetwork upNet : updatedNetworks) {

							Vector<SparseNetwork> shiftNetworks = new Vector<SparseNetwork>();
							shiftNetworks.add(upNet);
							// if (speedUp)
							// switchNewRetEdges(shiftNetworks, upNet);

							for (SparseNetwork shiftNet : shiftNetworks) {

								if (new CountRetNumber().getNumber(shiftNet, compValue) == new CountRetNumber()
										.getNumber(n, compValue) + forestSize - 1) {

									modEdgesCurrTrees(shiftNet);

									boolean alreadyReported = false;
									for (SparseNetwork repNet : repNetworks) {
										if (new NetworkIsomorphismCheck().run(repNet, shiftNet)) {
											alreadyReported = true;
											break;
										}
									}

									// prevTreeContainmentCheck(modNet,"END");

									if (!alreadyReported && !isStoppped && prevTreeContainmentCheck(shiftNet)) {
										repNetworks.add(shiftNet);

										if (singleNodeCheck(shiftNet))
											System.out.println("OneNode-End: " + upNet.getPhyloTree() + ";");

										// parentThread.reportNewNetworkThreads(shiftNet,
										// trees, hybridTrees);
										reporter.reportNewNetworkThreads(shiftNet, trees, hybridTrees, log);

										// isStoppped = true;

										counter++;
									}

								}

							}

							shiftNetworks = null;

						}

						updatedNetworks = null;

					} else {

						modEdgesCurrTrees(modNet);

						boolean alreadyReported = false;
						for (SparseNetwork repNet : repNetworks) {
							if (new NetworkIsomorphismCheck().run(repNet, modNet)) {
								alreadyReported = true;
								break;
							}
						}

						// prevTreeContainmentCheck(modNet,"END");

						if (!alreadyReported && !isStoppped && prevTreeContainmentCheck(modNet)) {
							repNetworks.add(modNet);

							if (singleNodeCheck(modNet))
								System.out.println("OneNode-End: " + modNet.getPhyloTree() + ";");

							// parentThread.reportNewNetworkThreads(modNet,
							// trees, hybridTrees);
							reporter.reportNewNetworkThreads(modNet, trees, hybridTrees, log);

							// isStoppped = true;

							counter++;
						}

					}

				}

			}

			modNetworks = null;

		}

	}

	private void updateRootPath(int index, SparseNetNode v, SparseNetwork n) {
		if (v.getInDegree() != 0) {
			SparseNetEdge e = null;
			if (v.getInDegree() == 1) {
				e = v.getInEdges().get(0);
				e.addIndex(index);
			} else if (v.getInDegree() > 1) {
				for (SparseNetEdge eIn : v.getInEdges()) {
					if (eIn.getIndices().contains(index)) {
						e = eIn;
						break;
					}
				}
			}
			if (e != null)
				updateRootPath(index, e.getSource(), n);
		}
	}

	private void modT2Edges(SparseNetwork n, Vector<SparseNetwork> modNetworks, Vector<String> visitedNodes,
			Vector<String> sequence, Vector<String> shiftedNodes) {

		modT2EdgesRecDown(n, 1);
		modT2EdgesRecDown(n, 0);
		modT2EdgesRecUp(n, 1);
		modT2EdgesRecUp(n, 0);
		modT2Edges01(n);

		modNetworks.add(n);

		// System.out.println(n.getPhyloTree() + ";" + " " + sequence.size());
		// for(String s : sequence)
		// System.out.println(s);

		for (SparseNetNode multiNode : n.getNodes()) {
			if (multiNode.getOutDegree() > 2) {
				Vector<SparseNetEdge> t1Edges = new Vector<SparseNetEdge>();
				Vector<SparseNetNode> sourceNodes = new Vector<SparseNetNode>();
				for (SparseNetEdge e : multiNode.getOutEdges()) {

					BitSet b0 = new BitSet(taxaOrdering.size());
					getClusterByIndex(e.getTarget(), n, b0, 0);
					BitSet b1 = new BitSet(taxaOrdering.size());
					getClusterByIndex(e.getTarget(), n, b1, 1);

					if (!b1.isEmpty() && b0.isEmpty())
						t1Edges.add(e);
					else if (((!b1.isEmpty() && !b0.isEmpty()) || (!b0.isEmpty() && shiftedNodes.contains(multiNode
							.getLabel()))) && e.getTarget().getOutDegree() > 1) {
						SparseNetNode s = e.getTarget();
						for (SparseNetEdge e2 : s.getOutEdges()) {
							BitSet b = new BitSet(taxaOrdering.size());
							getClusterByIndex(e2.getTarget(), n, b, 1);
							if (b.isEmpty()) {
								sourceNodes.add(s);
								break;
							}
						}
					} else if (b1.isEmpty() && !b0.isEmpty())
						sourceNodes.add(e.getTarget());

				}

				for (SparseNetEdge t1Edge : t1Edges) {
					for (SparseNetNode sourceNode : sourceNodes) {

						int counter0 = 0;
						for (SparseNetEdge e2 : sourceNode.getOutEdges()) {
							if (e2.getEdgeIndex().contains(0)) {
								counter0++;
							}
						}

						if (counter0 > 1 && !isDraggedUpNode1(sourceNode) && !isDraggedDownNode(sourceNode, 0)) {

							// copying network
							Vector<SparseNetNode[]> nodePairs = new Vector<SparseNetNode[]>();
							SparseNetNode[] nodePair = { sourceNode, null };
							nodePairs.add(nodePair);
							Vector<SparseNetEdge[]> edgePairs = new Vector<SparseNetEdge[]>();
							SparseNetEdge[] edgePair = { t1Edge, null };
							edgePairs.add(edgePair);
							SparseNetwork nCopy = copyNetwork(n, nodePairs, edgePairs);
							SparseNetNode sCopy = nodePairs.get(0)[1];
							SparseNetEdge eCopy = edgePairs.get(0)[1];
							SparseNetNode tCopy = eCopy.getTarget();

							BitSet b = new BitSet(taxaOrdering.size());
							getClusterByIndex(eCopy.getTarget(), nCopy, b, 1);
							Vector<BitSet> childNetSets = new Vector<BitSet>();
							childNetSets.add((BitSet) b.clone());
							for (SparseNetEdge e : sCopy.getOutEdges()) {
								if (e.getEdgeIndex().contains(1)) {
									BitSet bC = new BitSet(taxaOrdering.size());
									getClusterByIndex(e.getTarget(), nCopy, bC, 1);
									b.or(bC);
									if (!bC.isEmpty())
										childNetSets.add(bC);
								}
							}

							boolean treeCheck = true;
							if (childNetSets.size() > 1) {
								MyNode lca = trees[1].findLCA(b);
								Iterator<MyEdge> it = lca.getOutEdges();
								Vector<BitSet> childTreeSets = new Vector<BitSet>();
								while (it.hasNext()) {
									MyNode c = it.next().getTarget();
									BitSet cCluster = trees[1].getNodeToCluster().get(c);
									childTreeSets.add(cCluster);
								}
								for (BitSet bNet : childNetSets) {
									if (!childTreeSets.contains(bNet)) {
										treeCheck = false;
									}
								}
							}

							BitSet b1 = new BitSet(taxaOrdering.size());
							getClusterByIndex(multiNode, n, b1, 1);
							BitSet b2 = new BitSet(taxaOrdering.size());
							getClusterByIndex(sourceNode, n, b2, 1);
							getClusterByIndex(t1Edge.getTarget(), n, b2, 1);

							// System.out.println((!treeCheck &&
							// !isDraggedDownNode0(multiNode))+" "+!b1.equals(b2)+" "+shiftedNodes.contains(multiNode.getLabel()));

							// if(b1.equals(b2))
							// System.out.println(multiNode.getLabel()+" "+n.getPhyloTree()+";");

							// if ((treeCheck && isContractNodePrevTree(sCopy,
							// tCopy, true)) || !treeCheck) {

							if ((!treeCheck && !isDraggedDownNode(multiNode, 0)) || !b1.equals(b2)
									|| shiftedNodes.contains(multiNode.getLabel())) {

								HashMap<SparseNetwork, SparseNetNode> draggedNetworks = new HashMap<SparseNetwork, SparseNetNode>();
								Vector<SparseNetEdge> shiftEdges = new Vector<SparseNetEdge>();
								if (treeCheck) {

									eCopy.getSource().removeOutEdge(eCopy);
									SparseNetEdge newEdge = sCopy.addChild(tCopy);
									newEdge.addEdgeIndex(1);
									sCopy.getInEdges().get(0).addEdgeIndex(1);

									Vector<SparseNetNode> draggingNodes = new Vector<SparseNetNode>();
									collectDraggingNodes1(newEdge, draggingNodes);

									for (SparseNetNode vDragged : draggingNodes)
										switchT1EdgesRec(nCopy, sCopy, vDragged, draggingNodes, sCopy, draggedNetworks,
												new SparseTree(trees[1]), "2", 0, null, null, "");

								} else {

									for (SparseNetEdge e : sCopy.getOutEdges()) {
										BitSet bOne = new BitSet(taxaOrdering.size());
										getClusterByIndex(e.getTarget(), nCopy, bOne, 1);
										if (bOne.isEmpty())
											shiftEdges.add(e);
									}

									SparseNetEdge eXCopy = sCopy.getInEdges().get(0);
									HashSet<Integer> eIndices = (HashSet<Integer>) eXCopy.getEdgeIndex().clone();
									SparseNetNode vCopy = eXCopy.getSource();
									vCopy.removeOutEdge(eXCopy);
									SparseNetNode xCopy = new SparseNetNode(vCopy, nCopy, sCopy.getLabel());
									xCopy.addChild(sCopy);
									xCopy.getInEdges().get(0).setEdgeIndex(eIndices);
									xCopy.getOutEdges().get(0).setEdgeIndex(eIndices);
									for (SparseNetEdge e : shiftEdges) {
										SparseNetNode t = e.getTarget();
										e.getSource().removeOutEdge(e);
										SparseNetEdge newEdge = xCopy.addChild(t);
										newEdge.addEdgeIndex(0);
									}

									eCopy.getSource().removeOutEdge(eCopy);
									SparseNetEdge newEdge = xCopy.addChild(tCopy);
									newEdge.addEdgeIndex(1);

									// dragOneEdgeDown(sCopy, nCopy);

									removeOneNode(sCopy, nCopy);

									// System.out.println(nCopy.getPhyloTree()+";");
									// for(SparseNetEdge e :
									// nCopy.getEdges()){
									// if(e.getEdgeIndex().contains(1))
									// System.out.println(e.getSource().getLabel()+"->"+e.getTarget().getLabel());
									// }

									Vector<SparseNetNode> draggingNodes = new Vector<SparseNetNode>();
									collectDraggingNodes1(newEdge, draggingNodes);
									for (SparseNetNode vDragged : draggingNodes)
										switchT1EdgesRec(nCopy, xCopy, vDragged, draggingNodes, vDragged,
												draggedNetworks, new SparseTree(trees[1]), "2", 0, null, null, "");

									draggingNodes = null;

								}

								Vector<String> sequenceCopy = (Vector<String>) sequence.clone();
								sequenceCopy.add(multiNode.getLabel() + "-" + sourceNode.getLabel() + "-"
										+ t1Edge.getTarget().getLabel());

								Collections.sort(sequenceCopy);
								String sequenceString = "";
								for (String s : sequenceCopy)
									sequenceString = sequenceString.concat(s + " ");

								Vector<String> visitedNodesClone = (Vector<String>) visitedNodes.clone();
								if (treeCheck || !shiftEdges.isEmpty()) {
									Vector<String> shiftedNodesCopy = (Vector<String>) shiftedNodes.clone();
									shiftedNodesCopy.add(sourceNode.getLabel());
									shiftedNodesCopy.add(multiNode.getLabel());
									visitedNodes.add(sequenceString);

									// System.out.println("\n"+n.getPhyloTree()+";\n"+nCopy.getPhyloTree()+";");

									updateNetwork(nCopy, null, null);
									modT2EdgesRecDown(nCopy, 1);
									modT2EdgesRecDown(nCopy, 0);
									modT2EdgesRecUp(nCopy, 1);
									modT2EdgesRecUp(nCopy, 0);
									modT2Edges01(n);

									boolean alreadyReported = false;
									for (SparseNetwork repNet : modNetworks) {
										if (new NetworkIsomorphismCheck().run(repNet, nCopy)) {
											alreadyReported = true;
											break;
										}
									}

									if (!alreadyReported && prevTreeContainmentCheck(nCopy)) {

										// System.out.println("> "+n.getPhyloTree()
										// + ";\n" + nCopy.getPhyloTree() +
										// ";");
										modT2Edges(nCopy, modNetworks, visitedNodes, sequenceCopy, shiftedNodesCopy);
									}

								}

								if (!draggedNetworks.isEmpty()) {

									Vector<String> sequenceCopy2 = (Vector<String>) sequence.clone();
									sequenceCopy2.add(multiNode.getLabel() + "-" + sourceNode.getLabel() + "-"
											+ t1Edge.getTarget().getLabel() + "D");
									Collections.sort(sequenceCopy2);
									String sequenceString2 = "";
									for (String s : sequenceCopy2)
										sequenceString2 = sequenceString2.concat(s + " ");

									for (SparseNetwork draggedNet : draggedNetworks.keySet()) {

										Vector<String> shiftedNodesCopy = (Vector<String>) shiftedNodes.clone();
										shiftedNodesCopy.add(sourceNode.getLabel());
										shiftedNodesCopy.add(multiNode.getLabel());
										visitedNodesClone.add(sequenceString2);

										updateNetwork(draggedNet, null, null);
										modT2EdgesRecDown(draggedNet, 1);
										modT2EdgesRecDown(draggedNet, 0);
										modT2EdgesRecUp(draggedNet, 1);
										modT2EdgesRecUp(draggedNet, 0);
										updateNetwork(draggedNet, null, null);

										boolean alreadyReported = false;
										for (SparseNetwork repNet : modNetworks) {
											if (new NetworkIsomorphismCheck().run(repNet, draggedNet)) {
												alreadyReported = true;
												break;
											}
										}

										if (!alreadyReported && prevTreeContainmentCheck(draggedNet)) {

											// System.out.println(">"+draggedNet.getPhyloTree()+";");
											modT2Edges(draggedNet, modNetworks,
													(Vector<String>) visitedNodesClone.clone(),
													(Vector<String>) sequenceCopy2.clone(), shiftedNodesCopy);
										}

									}

									draggedNetworks = null;
								}
							}
						}
					}
				}
			}
		}
	}

	private void modT2EdgesRecDown(SparseNetwork n, int treeIndex) {
		Vector<SparseNetNode> nodes = new Vector<SparseNetNode>();
		for (SparseNetNode v : n.getNodes())
			nodes.add(v);
		for (SparseNetNode v : nodes) {
			if (dragOneEdgeDown(v, n, treeIndex)) {
				updateNetwork(n, null, null);
				modT2EdgesRecDown(n, treeIndex);
				break;
			}
		}
	}

	private void modT2EdgesRecUp(SparseNetwork n, int treeIndex) {
		Vector<SparseNetNode> nodes = new Vector<SparseNetNode>();
		for (SparseNetNode v : n.getNodes())
			nodes.add(v);
		for (SparseNetNode v : nodes) {
			if (dragOneEdgeUp(v, n, treeIndex)) {
				updateNetwork(n, null, null);
				modT2EdgesRecUp(n, treeIndex);
				break;
			}
		}
	}

	private void modT2Edges01(SparseNetwork n) {
		Vector<SparseNetNode> nodes = new Vector<SparseNetNode>();
		for (SparseNetNode v : n.getNodes())
			nodes.add(v);
		for (SparseNetNode v : nodes) {
			if (v.getInDegree() == 1 && v.getOutDegree() == 2 && isDraggedDownNode(v, 1) && isDraggedDownNode(v, 0)) {
				updateNetwork(n, null, null);
				SparseNetNode vP = v.getInEdges().get(0).getSource();
				if (isContractNodeCurrTrees(vP, v, insertIndex, false, false, null, false)) {
					Vector<SparseNetEdge> dragEdges = new Vector<SparseNetEdge>();
					for (SparseNetEdge eOut : v.getOutEdges())
						dragEdges.add(eOut);

					// System.out.println(">01:" + v.getLabel());
					// System.out.println(n.getPhyloTree() + ";");

					for (SparseNetEdge eDrag : dragEdges) {
						HashSet<Integer> eDragIndices = (HashSet<Integer>) eDrag.getEdgeIndex().clone();
						SparseNetNode t = eDrag.getTarget();
						v.removeOutEdge(eDrag);
						SparseNetEdge newEdge = vP.addChild(t);
						newEdge.setEdgeIndex(eDragIndices);
					}
					vP.removeOutEdge(v.getInEdges().get(0));

					// System.out.println(n.getPhyloTree() + ";");
					// for (int i = 0; i < this.insertIndex; i++)
					// System.out.println(hybridTrees[i] + ";");

					modT2Edges01(n);
					break;
				}
			}
		}
	}

	private void modEdgesCurrTrees(SparseNetwork n) {

		Vector<SparseNetNode> nodes = new Vector<SparseNetNode>();
		cmpTopDownNodes(n.getRoot(), nodes);

		SparseNetEdge eContract = null;
		for (SparseNetNode v : nodes) {
			if (v.getInDegree() == 1 && v.getOutDegree() > 1) {
				for (SparseNetEdge e : v.getOutEdges()) {
					if (e.getTarget().getOutDegree() > 1
							&& isContractNodeCurrTrees(v, e.getTarget(), insertIndex + 1, false, false, null, false)) {
						eContract = e;
						break;
					}
				}
			}
			if (eContract != null)
				break;
		}
		if (eContract != null) {

			SparseNetNode s = eContract.getSource();
			HashMap<SparseNetNode, HashSet<Integer>> nodeToIndices = new HashMap<SparseNetNode, HashSet<Integer>>();
			Vector<SparseNetEdge> toDelete = new Vector<SparseNetEdge>();
			for (SparseNetEdge e : eContract.getTarget().getOutEdges()) {
				nodeToIndices.put(e.getTarget(), (HashSet<Integer>) e.getIndices().clone());
				toDelete.add(e);
			}
			s.removeOutEdge(eContract);
			for (SparseNetEdge e : toDelete)
				e.getSource().removeOutEdge(e);
			for (SparseNetNode t : nodeToIndices.keySet()) {
				SparseNetEdge newEdge = s.addChild(t);
				newEdge.addIndices((HashSet<Integer>) nodeToIndices.get(t).clone());
			}

			modEdgesCurrTrees(n);

		}

	}

	private void cmpTopDownNodes(SparseNetNode v, Vector<SparseNetNode> nodes) {
		if (!nodes.contains(v)) {
			nodes.add(v);
			for (SparseNetEdge e : v.getOutEdges())
				cmpTopDownNodes(e.getTarget(), nodes);
		}
	}

	private boolean isContractNodeCurrTrees(SparseNetNode p, SparseNetNode v, int lastIndex, boolean shiftMode,
			boolean speedUp, Integer checkIndex, boolean debug) {

		// String log = "";
		// log = log.concat("---- p: " + p.getLabel() + " v: " + v.getLabel() +
		// "\n");

		Vector<Integer> indices = new Vector<Integer>();
		if (checkIndex != null)
			indices.add(checkIndex);
		else {
			for (int index = 0; index < lastIndex; index++)
				indices.add(index);
		}

		for (int index : indices) {
			// log = log.concat(index + "\n" + hybridTrees[index] + ";\n" +
			// p.getOwner().getPhyloTree() + ";\n");
			Vector<BitSet> netClusters = new Vector<BitSet>();
			BitSet cluster = new BitSet(taxaOrdering.size());
			int counterA = 0;
			for (SparseNetEdge eOut : p.getOutEdges()) {
				if ((!n.isSpecial(eOut) || eOut.getIndices().contains(index)) && !eOut.getTarget().equals(v)) {
					BitSet b = new BitSet(taxaOrdering.size());
					getClusterByPrevTree(eOut.getTarget(), v.getOwner(), b, index, false);
					if (!b.isEmpty()) {
						// log = log.concat("1-Adding: " + b + "\n");
						netClusters.add(b);
						counterA++;
					}
					cluster.or(b);
				}
			}
			if (speedUp && p.getOutDegree() == 0) {
				BitSet b = new BitSet(taxaOrdering.size());
				getClusterByPrevTree(p, v.getOwner(), b, index, false);
				if (!b.isEmpty()) {
					// log = log.concat("1-Adding: " + b + "\n");
					netClusters.add(b);
					counterA++;
				}
				cluster.or(b);
			}
			getClusterByPrevTree(v, p.getOwner(), cluster, index, false);
			int counterB = 0;
			for (SparseNetEdge eOut : v.getOutEdges()) {
				if (!n.isSpecial(eOut) || eOut.getIndices().contains(index)) {
					BitSet b = new BitSet(taxaOrdering.size());
					getClusterByPrevTree(eOut.getTarget(), v.getOwner(), b, index, false);
					if (!b.isEmpty()) {
						// log = log.concat("2-Adding: " + b + "\n");
						netClusters.add(b);
						counterB++;
					}
				}
			}
			// if (netCluster.size() >= 1 && counterB >= 1 && counterA > 0) {
			if (netClusters.size() > 0 && (counterA > 0) && (counterB > 1 || shiftMode)) {
				MyNode lca = hybridTrees[index].findLCA(cluster);
				Iterator<MyEdge> it = lca.getOutEdges();
				// log = log.concat("LCA " + lca.getLabel());
				Vector<BitSet> treeClusters = new Vector<BitSet>();
				while (it.hasNext())
					treeClusters.add(hybridTrees[index].getNodeToCluster().get(it.next().getTarget()));
				int match = 0, mismatch = 0;
				for (BitSet netCluster : netClusters) {
					// if (!treeClusters.contains(netCluster)) {
					// if (debug)
					// System.out.println(log);
					// return false;
					// }
					for (BitSet treeCluster : treeClusters) {
						BitSet b = (BitSet) netCluster.clone();
						b.or(treeCluster);
						if (!b.equals(netCluster) && treeCluster.intersects(netCluster)) {
							// log = log.concat("Case-1 \n");
							// if (debug)
							// System.out.println(log);
							return false;
						} else if (!treeCluster.intersects(netCluster)) {
							// log = log.concat("mismatch: " + treeCluster + " "
							// + netCluster + "\n");
							mismatch++;
						}
						if (netCluster.equals(treeCluster))
							match++;
					}
				}
				// if (shiftMode && match == netClusters.size() && mismatch > 0)
				// {
				if (shiftMode && match == netClusters.size() && mismatch > match * (match - 1)) {
					// log = log.concat("Case-2 " + mismatch + "\n");
					// if (debug)
					// System.out.println(log);
					return false;
				}
			}
		}
		// if (debug)
		// System.out.println(log);
		return true;
	}

	private boolean dragOneEdgeDown(SparseNetNode v, SparseNetwork n, int treeIndex) {
		if (v.getOutDegree() == 2) {
			SparseNetEdge e = null, e1 = null;
			for (SparseNetEdge eOut : v.getOutEdges()) {
				BitSet b0 = new BitSet(taxaOrdering.size());
				getClusterByIndex(eOut.getTarget(), n, b0, 0);
				BitSet b1 = new BitSet(taxaOrdering.size());
				getClusterByIndex(eOut.getTarget(), n, b1, 1);
				if (!b1.isEmpty() && !b0.isEmpty())
					e = eOut;
				else if ((treeIndex == 1 && b0.isEmpty() && !b1.isEmpty())
						|| (treeIndex == 0 && b1.isEmpty() && !b0.isEmpty()))
					e1 = eOut;
			}
			if (e != null && e1 != null && e.getTarget().getOutDegree() != 0) {
				SparseNetNode t = e.getTarget();

				if (t.getOutDegree() > 1 && isDraggedUpNodePrevTree(e1.getTarget(), t, true, false)) {

					// if(!isDraggedUpNodePrevTree(e1.getTarget(), t, true,
					// false))
					// isDraggedUpNodePrevTree(e1.getTarget(), t, true, true);

					Vector<BitSet> netCluster = new Vector<BitSet>();
					BitSet cluster = new BitSet(taxaOrdering.size());
					getClusterByIndex(e1.getTarget(), n, cluster, treeIndex);
					netCluster.add((BitSet) cluster.clone());
					getClusterByIndex(t, n, cluster, treeIndex);
					for (SparseNetEdge eOut : t.getOutEdges()) {
						BitSet b = new BitSet(taxaOrdering.size());
						getClusterByIndex(eOut.getTarget(), n, b, treeIndex);
						if (!b.isEmpty())
							netCluster.add(b);
					}
					boolean check = true;
					MyNode lca = trees[treeIndex].findLCA(cluster);
					Iterator<MyEdge> it = lca.getOutEdges();
					while (it.hasNext()) {
						BitSet b = trees[treeIndex].getNodeToCluster().get(it.next().getTarget());
						if (!netCluster.contains(b))
							check = false;
					}
					if (check) {

						// System.out.println(">Down:" + v.getLabel() + " " +
						// t.getLabel() + " " + treeIndex + " "
						// + lca.getLabel() + " " +
						// isDraggedUpNodePrevTree(e1.getTarget(), t, true));
						// System.out.println(n.getPhyloTree() + ";");

						SparseNetNode t1 = e1.getTarget();
						v.removeOutEdge(e1);
						SparseNetEdge newEdge = t.addChild(t1);
						newEdge.addEdgeIndex(treeIndex);
						removeOneNode(v, n);

						// System.out.println(n.getPhyloTree() + ";");

						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean dragOneEdgeUp(SparseNetNode v, SparseNetwork n, int treeIndex) {
		if (v.getOutDegree() == 2 && v.getInDegree() == 1) {
			SparseNetEdge e1 = null;
			for (SparseNetEdge eOut : v.getOutEdges()) {
				BitSet b0 = new BitSet(taxaOrdering.size());
				getClusterByIndex(eOut.getTarget(), n, b0, 0);
				BitSet b1 = new BitSet(taxaOrdering.size());
				getClusterByIndex(eOut.getTarget(), n, b1, 1);
				if ((treeIndex == 1 && b0.isEmpty() && !b1.isEmpty())
						|| (treeIndex == 0 && b1.isEmpty() && !b0.isEmpty()))
					e1 = eOut;
			}
			SparseNetNode p = v.getInEdges().get(0).getSource();
			if (e1 != null && p.getInDegree() == 1
					&& isContractNodeCurrTrees(p, v, insertIndex, false, false, null, false)) {

				Vector<BitSet> netCluster = new Vector<BitSet>();
				BitSet cluster = new BitSet(taxaOrdering.size());
				getClusterByIndex(e1.getTarget(), n, cluster, treeIndex);
				BitSet mask = new BitSet(taxaOrdering.size());
				mask.set(0, taxaOrdering.size());
				mask.xor(cluster);
				netCluster.add((BitSet) cluster.clone());
				for (SparseNetEdge eOut : p.getOutEdges()) {
					BitSet b = new BitSet(taxaOrdering.size());
					getClusterByIndex(eOut.getTarget(), n, b, treeIndex);
					b.and(mask);
					if (!b.isEmpty())
						netCluster.add(b);
				}
				boolean check = true;
				getClusterByIndex(p, n, cluster, treeIndex);
				MyNode lca = trees[treeIndex].findLCA(cluster);

				Iterator<MyEdge> it = lca.getOutEdges();
				Vector<BitSet> treeCluster = new Vector<BitSet>();
				while (it.hasNext()) {
					BitSet b = trees[treeIndex].getNodeToCluster().get(it.next().getTarget());
					treeCluster.add(b);
					if (!netCluster.contains(b)) {
						check = false;
						break;
					}
				}

				if (check) {

					Vector<SparseNetEdge> dragEdges = new Vector<SparseNetEdge>();
					dragEdges.add(e1);
					boolean draggedUpNode = treeIndex == 1 ? isDraggedUpNode1(p) : isDraggedUpNode0(p);
					if (draggedUpNode) {
						for (SparseNetEdge eOut : v.getOutEdges()) {
							BitSet b0 = new BitSet(taxaOrdering.size());
							getClusterByIndex(eOut.getTarget(), n, b0, 0);
							BitSet b1 = new BitSet(taxaOrdering.size());
							getClusterByIndex(eOut.getTarget(), n, b1, 1);
							if (!b1.isEmpty() && !b0.isEmpty())
								dragEdges.add(eOut);
						}
					}

					// System.out.println(">Up:" + v.getLabel() + " " +
					// p.getLabel() + " " + treeIndex + " "
					// + dragEdges.size() + " " + isContractNodePrevTree(p, v,
					// true));
					// System.out.println(n.getPhyloTree() + ";");

					for (SparseNetEdge eDrag : dragEdges) {
						HashSet<Integer> eDragIndices = (HashSet<Integer>) eDrag.getEdgeIndex().clone();
						SparseNetNode t1 = eDrag.getTarget();
						v.removeOutEdge(eDrag);
						SparseNetEdge newEdge = p.addChild(t1);
						newEdge.setEdgeIndex(eDragIndices);
					}
					removeOneNode(v, n);
					if (v.getOutDegree() == 0 && v.getInDegree() != 0) {
						SparseNetNode vP = v.getInEdges().get(0).getSource();
						vP.removeOutEdge(v.getInEdges().get(0));
					}

					// System.out.println(n.getPhyloTree() + ";");
					// for (int i = 0; i < insertIndex; i++)
					// System.out.println(hybridTrees[i] + ";");
					// System.out.println();

					return true;
				}
			}
		}
		return false;
	}

	private void collectDraggingNodes1(SparseNetEdge e, Vector<SparseNetNode> draggingNodes) {
		if (e.getEdgeIndex().contains(1) && !e.getEdgeIndex().contains(0)) {
			if (e.getTarget().getOutDegree() > 1) {
				draggingNodes.add(e.getTarget());
				for (SparseNetEdge eOut : e.getTarget().getOutEdges())
					collectDraggingNodes1(eOut, draggingNodes);
			}
		}
	}

	private boolean isDraggedUpNode1(SparseNetNode v) {
		Vector<BitSet> childNetSets = new Vector<BitSet>();
		BitSet cluster = new BitSet(taxaOrdering.size());
		for (SparseNetEdge e : v.getOutEdges()) {
			if (e.getEdgeIndex().contains(1)) {
				BitSet b = new BitSet(taxaOrdering.size());
				getClusterByIndex(e.getTarget(), v.getOwner(), b, 1);
				if (!b.isEmpty())
					childNetSets.add(b);
				cluster.or(b);
			}
		}
		if (childNetSets.size() > 1) {
			MyNode lca = trees[1].findLCA(cluster);
			Iterator<MyEdge> it = lca.getOutEdges();
			while (it.hasNext()) {
				BitSet childSet = (BitSet) trees[1].getNodeToCluster().get(it.next().getTarget()).clone();
				childSet.and(indexToInsertedSet.get(1));
				if (!childSet.isEmpty() && !childNetSets.contains(childSet)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isDraggedUpNode0(SparseNetNode v) {
		Vector<BitSet> childNetSets = new Vector<BitSet>();
		BitSet cluster = new BitSet(taxaOrdering.size());
		for (SparseNetEdge e : v.getOutEdges()) {
			if (e.getEdgeIndex().contains(0)) {
				BitSet b = new BitSet(taxaOrdering.size());
				getClusterByIndex(e.getTarget(), v.getOwner(), b, 0);
				childNetSets.add(b);
				cluster.or(b);
			}
		}
		if (childNetSets.size() > 1) {
			MyNode lca = trees[0].findLCA(cluster);
			Iterator<MyEdge> it = lca.getOutEdges();
			Vector<BitSet> childTreeSets = new Vector<BitSet>();
			while (it.hasNext()) {
				BitSet childSet = (BitSet) trees[0].getNodeToCluster().get(it.next().getTarget()).clone();
				childTreeSets.add(childSet);
			}
			for (BitSet netCluster : childNetSets) {
				if (!childTreeSets.contains(netCluster) && !netCluster.isEmpty())
					return true;
			}
		}
		return false;
	}

	private boolean isDraggedUpNodePrevTree(SparseNetNode v1, SparseNetNode v2, boolean dragMode, boolean debug) {

		// String log = "";
		// log = log.concat("---- v1: " + v1.getLabel() + " v2: " +
		// v2.getLabel() + "\n");

		for (int prevIndex = 0; prevIndex < insertIndex; prevIndex++) {
			// log = log.concat(prevIndex + "\n" + hybridTrees[prevIndex] +
			// ";\n" + v1.getOwner().getPhyloTree() + ";\n");
			Vector<BitSet> netClusters = new Vector<BitSet>();
			BitSet cluster = new BitSet(taxaOrdering.size());
			getClusterByPrevTree(v1, v1.getOwner(), cluster, prevIndex, false);
			if (!cluster.isEmpty()) {
				// log = log.concat("Adding1: " + cluster + "\n");
				netClusters.add((BitSet) cluster.clone());
			}
			getClusterByPrevTree(v2, v1.getOwner(), cluster, prevIndex, false);
			int counter = 0;
			for (SparseNetEdge eOut : v2.getOutEdges()) {
				BitSet b = new BitSet(taxaOrdering.size());
				getClusterByPrevTree(eOut.getTarget(), v1.getOwner(), b, prevIndex, false);
				if (!b.isEmpty()) {
					// log = log.concat("Adding2: " + b);
					netClusters.add(b);
					counter++;
				}
			}
			if ((netClusters.size() > 1 && counter > 1) || !dragMode) {
				// MyNode lca = hybridTrees[prevIndex].findLCA(cluster);
				// Iterator<MyEdge> it = lca.getOutEdges();
				// while (it.hasNext()) {
				// BitSet b =
				// hybridTrees[prevIndex].getNodeToCluster().get(it.next().getTarget());
				// if (!netCluster.contains(b)) {
				// if (debug)
				// System.out.println(log);
				// return false;
				// }
				// }
				MyNode lca = hybridTrees[prevIndex].findLCA(cluster);
				Iterator<MyEdge> it = lca.getOutEdges();
				// log = log.concat("LCA " + lca.getLabel());
				Vector<BitSet> treeClusters = new Vector<BitSet>();
				while (it.hasNext())
					treeClusters.add(hybridTrees[prevIndex].getNodeToCluster().get(it.next().getTarget()));
				for (BitSet netCluster : netClusters) {
					// if (!treeClusters.contains(netCluster)) {
					// if (debug)
					// System.out.println(log);
					// return false;
					// }
					for (BitSet treeCluster : treeClusters) {
						BitSet b = (BitSet) netCluster.clone();
						b.or(treeCluster);
						if (!b.equals(netCluster) && treeCluster.intersects(netCluster)) {
							// if (debug)
							// System.out.println(log);
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	private boolean isDraggedDownNode(SparseNetNode v, int treeIndex) {
		BitSet netCluster = new BitSet(taxaOrdering.size());
		for (SparseNetEdge e : v.getOutEdges()) {
			if (e.getEdgeIndex().contains(treeIndex)) {
				BitSet b = new BitSet(taxaOrdering.size());
				getClusterByIndex(e.getTarget(), v.getOwner(), b, treeIndex);
				netCluster.or(b);
			}
		}
		MyNode lca = trees[treeIndex].findLCA(netCluster);

		BitSet treeCluster = (BitSet) trees[treeIndex].getNodeToCluster().get(lca).clone();
		BitSet b = (BitSet) netCluster.clone();
		b.or(treeCluster);
		if (!b.equals(netCluster))
			return true;
		return false;
	}

	private Vector<SparseNetwork> addComponent(SparseNetwork n, Vector<Integer> indices, int t1Index, SparseTree f,
			SparseNetNode vRet, BitSet sibCluster, BitSet justInserted, Vector<Integer> justIndices,
			Vector<SparseNetNode> vXCandidates, ConcurrentHashMap<SparseNetwork, SparseNetNode> netToRetNode,
			SparseNetNode[] nodePair) {

		// doubleEdge(n, "1");

		if (debug) {
			System.out.println("\nAdding Component: " + f.getRoot().getLabel());
			System.out.println(n.getPhyloTree() + ";");
		}

		updateNetwork(n, null, null);

		Vector<HybridTree> txTrees = new Vector<HybridTree>();
		for (int txIndex : indices) {
			if (debug) {
				System.out.println(txIndex + "Tree:");
				System.out.println(n.getPhyloTree() + ";");
			}
			txTrees.add(trees[txIndex]);
		}

		// compute parent of the node representing f in Tx
		BitSet r = getTreeCluster(f);
		Vector<MyNode> lxNodes = new Vector<MyNode>();
		for (int i = 0; i < indices.size(); i++)
			lxNodes.add(txTrees.get(i).findLCA(r));

		BitSet forestSet = (BitSet) getTreeCluster(f).clone();

		// all taxa inserted - bit set consists of 1s
		BitSet insertedT1Set = getNetworkCluster(n);

		// computing target node
		// **********************************************************

		if (f.getInfo() == "2")
			vRet = new SparseNetNode(null, n, f.getRoot().getLabel());

		ConcurrentHashMap<Integer, SparseNetNode> intToTargetNode = new ConcurrentHashMap<Integer, SparseNetNode>();
		int rhoIndex = taxonToIndex.get("rho");

		if (vRet == null) {

			Vector<MyNode> t1LCA = new Vector<MyNode>();
			t1LCA.add(t1.findLCA(r));
			BitSet v1Set = findChildNode(t1LCA, t1, t1Index, forestSet, insertedT1Set);

			// if there is no edge, f is attached to the out-edge of the root
			// int rhoIndex = taxaOrdering.indexOf("rho");
			BitSet b1 = new BitSet(taxaOrdering.size());
			b1.set(0, taxaOrdering.size());
			b1.set(rhoIndex, false);
			v1Set = (v1Set == null) ? b1 : v1Set;

			SparseNetNode vT = clusterToNode.get(t1Index).get(v1Set).get(0);

			// if (vRet == null) {
			int i = 1;
			SparseNetEdge e0 = null, e1 = null;
			while (e1 == null) {
				for (SparseNetEdge e : vT.getInEdges()) {

					// BitSet bE0 = new BitSet(taxaOrdering.size());
					// getClusterByIndex(e.getTarget(), n, bE0, 0);

					// BitSet bE1 = new BitSet(taxaOrdering.size());
					// getClusterByIndex(e.getTarget(), n, bE1, 1, null, false);

					// if(!bE1.isEmpty()){
					if (e.getEdgeIndex().contains(1)) {
						for (SparseNetEdge eOut : vT.getOutEdges()) {
							if (eOut.getEdgeIndex().contains(1))
								e1 = e;
						}
						if (vT.getOutDegree() == 0)
							e1 = e;
					} else if (f.getNodes().size() == 1) {
						for (SparseNetEdge eOut : vT.getOutEdges()) {
							BitSet bE0 = new BitSet(taxaOrdering.size());
							getClusterByIndex(eOut.getTarget(), n, bE0, 0);
							if (!checkConstraints(bE0, 1))
								e1 = e;
						}
					}
					// if(!bE0.isEmpty()){
					if (e.getEdgeIndex().contains(0)) {
						e0 = e;
					}
				}
				if (e1 == null) {
					if (vT.getInDegree() == 1 && vT.getOutDegree() != 1) {
						if (debug)
							System.out.println("vT: " + vT.getLabel());
						intToTargetNode.put(i, vT);
						i++;
					}
					vT = e0.getSource();
					e0 = null;
					e1 = null;
				}
			}

		}

		// ********************************************************************************

		// computing source nodes
		// *********************************************************

		Vector<BitSet> vXSets = new Vector<BitSet>();
		if (sibCluster == null) {
			for (int i = 0; i < indices.size(); i++) {

				// computing source nodes corresponding to index
				MyNode lx = lxNodes.get(i);
				HybridTree tx = txTrees.get(i);
				int txIndex = indices.get(i);
				BitSet tXSet = (BitSet) indexToInsertedSet.get(txIndex).clone();
				// if (debug) {
				// System.out.println("txIndex: " + txIndex + " insertedSet: " +
				// tXSet);
				// System.out.println(indexToInsertedSet.get(txIndex) + " " +
				// getTreeCluster(f));
				// }
				BitSet vXCluster = findChildNode(getSibling(lx, tx, tXSet), tx, txIndex, forestSet, tXSet);
				vXSets.add(vXCluster);
			}

			for (int i = 0; i < indices.size(); i++) {
				BitSet bX = (BitSet) indexToInsertedSet.get(indices.get(i)).clone();
				bX.set(rhoIndex, false);
				BitSet vXSet = vXSets.get(i);
				vXSet = (vXSet == null) ? bX : vXSet;
			}
		} else
			vXSets.add(sibCluster);

		// ********************************************************************************

		// build all possible edge pairs
		Vector<Vector<Integer>> v1Indices = new Vector<Vector<Integer>>();
		for (int i = 0; i < indices.size(); i++) {
			v1Indices.add(new Vector<Integer>());
			if (vRet == null) {
				for (int key : intToTargetNode.keySet())
					v1Indices.get(i).add(key);
			} else
				v1Indices.get(i).add(0);
		}

		// System.out.println("vX nodes: ");
		Vector<Vector<Integer>> vXIndices = new Vector<Vector<Integer>>();
		boolean emptySet = false;
		for (int i = 0; i < indices.size(); i++) {

			vXIndices.add(new Vector<Integer>());
			int txIndex = indices.get(i);
			BitSet vXSet = vXSets.get(i);

			for (int j = 0; j < clusterToNode.get(txIndex).get(vXSet).size(); j++) {
				SparseNetNode vX = clusterToNode.get(txIndex).get(vXSet).get(j);
				if (vXCandidates == null || vXCandidates.contains(vX)) {
					if (debug)
						System.out.println("txIndex: " + txIndex + " vX: " + vX.getLabel());
					vXIndices.get(i).add(j);
				}
			}

			if (vXIndices.get(i).isEmpty())
				emptySet = true;

		}

		Vector<Vector<Integer>> pairs = computePairs(v1Indices, vXIndices);
		Vector<Vector<Integer>> toRemove = new Vector<Vector<Integer>>();
		for (Vector<Integer> pair : pairs) {
			// SparseNetNode vR = (vRet == null) ?
			// clusterToNode.get(t1Index).get(v1Set).get(pair.get(0)) : vRet;
			SparseNetNode vR = (vRet == null) ? intToTargetNode.get(pair.get(0)) : vRet;
			for (int i = 0; i < indices.size(); i++) {

				vXIndices.add(new Vector<Integer>());
				int txIndex = indices.get(i);
				BitSet vXSet = vXSets.get(i);
				SparseNetNode vX = clusterToNode.get(txIndex).get(vXSet).get(pair.get(i + indices.size()));

				// checking if vX is in the subtree of vR (cycle!!!)
				Vector<SparseNetNode> subNodes = new Vector<SparseNetNode>();
				getSubnodes(n, vR, subNodes);
				boolean hasCycle = subNodes.contains(vX);

				if (hasCycle || vR.equals(vX)) {
					toRemove.add(pair);
					break;
				}

			}
		}

		for (Vector<Integer> pair : toRemove)
			pairs.removeElement(pair);

		// attach f to all possible edges
		Vector<SparseNetwork> newNetworks = new Vector<SparseNetwork>();
		if (!emptySet) {

			for (Vector<Integer> pair : pairs) {

				if (debug) {
					System.out.println("New Pair:");
					for (int index : pair)
						System.out.print(index + " | ");
					System.out.println();
				}

				Vector<SparseNetNode[]> nodePairs = new Vector<SparseNetNode[]>();
				// SparseNetNode vR = (vRet == null) ?
				// clusterToNode.get(t1Index).get(v1Set).get(pair.get(0)) :
				// vRet;
				SparseNetNode vR = (vRet == null) ? intToTargetNode.get(pair.get(0)) : vRet;
				SparseNetNode[] vRPair = { vR, null };
				nodePairs.add(vRPair);
				if (nodePair != null)
					nodePairs.add(nodePair);

				// wrongLeafCheck(n, "1");

				SparseNetwork nCopy = copyNetwork(n, nodePairs, null);
				updateNetwork(nCopy, null, null);

				// adding component
				SparseNetNode retNode = null;
				ConcurrentHashMap<SparseNetNode, Vector<Integer>> sourceNodeToIndices = new ConcurrentHashMap<SparseNetNode, Vector<Integer>>();

				SparseNetNode v1 = nodePairs.get(0)[1];
				if (netToRetNode != null)
					netToRetNode.put(nCopy, nodePairs.get(1)[1]);

				// computing help node
				Iterator<SparseNetEdge> it = nCopy.getRoot().getOutEdges().iterator();
				SparseNetNode v = it.next().getTarget();
				if (v.getLabel().equals("rho"))
					v = it.next().getTarget();

				// log = log.concat("\n");

				if (f.getInfo() == "2")
					retNode = new SparseNetNode(null, nCopy, f.getRoot().getLabel());
				else {
					// computing target node v1
					if (v1.getLabel().equals("rho") || v1.equals(nCopy.getRoot()))
						v1 = v;
					if (v1.getInDegree() == 1 && v1.getInEdges().get(0).getEdgeIndex().isEmpty()) {
						retNode = v1;
					} else {
						retNode = insertTargetNode(nCopy, v1, indices);
					}
				}

				// log =
				// log.concat(nCopy.getPhyloTree().toMyBracketString()+"\n");

				// computing source nodes vX
				for (int i = 0; i < indices.size(); i++) {
					BitSet vXSet = vXSets.get(i);
					int txIndex = indices.get(i);

					SparseNetNode vX = clusterToNode.get(txIndex).get(vXSet).get(pair.get(i + indices.size()));
					if (vX.getLabel().equals("rho") || vX.equals(nCopy.getRoot()))
						vX = v;
					if (!sourceNodeToIndices.containsKey(vX))
						sourceNodeToIndices.put(vX, new Vector<Integer>());
					sourceNodeToIndices.get(vX).add(txIndex);
				}

				// for (SparseNetNode vX : sourceNodeToIndices.keySet()) {
				// System.out.println(vX.getLabel()+"\n"+nCopy.getPhyloTree()+";");
				SparseNetNode vX = sourceNodeToIndices.keys().nextElement();
				SparseNetNode sX = insertSourceNode(nCopy, retNode, vX, sourceNodeToIndices.get(vX), f);
				// System.out.println(nCopy.getPhyloTree()+";\n");
				// }

				// doubleEdge(nCopy, "2");

				if (sX != null) {

					// log =
					// log.concat(nCopy.getPhyloTree().toMyBracketString()+"\n");

					// remove empty in-edges
					Vector<SparseNetEdge> emptyEdges = new Vector<SparseNetEdge>();
					for (SparseNetEdge e : retNode.getInEdges()) {
						if (e.getEdgeIndex().isEmpty())
							emptyEdges.add(e);
					}
					for (SparseNetEdge e : emptyEdges) {
						e.getSource().removeOutEdge(e);
						removeOneNode(e.getSource(), nCopy);
					}
					if (!emptyEdges.isEmpty()) {
						if (retNode.getInDegree() == 1)
							removeOneNode(retNode.getInEdges().get(0).getSource(), nCopy);
					}

					// prevTreeContainmentCheckVerbose(nCopy,
					// "1)******************\n"+log);

					HashMap<SparseNetwork, SparseNetNode> draggedNetworks = new HashMap<SparseNetwork, SparseNetNode>();
					SparseNetNode switchNode = netToRetNode != null ? netToRetNode.get(nCopy) : null;

					dragEdgeUp1(draggedNetworks, nCopy, retNode, f, switchNode, netToRetNode);
					dragEdgeDown(draggedNetworks, nCopy, retNode, f, switchNode, netToRetNode, false,
							!justInserted.isEmpty());
					dragEdgeUp2(draggedNetworks, nCopy, retNode, f, switchNode, netToRetNode);

					Vector<SparseNetwork> switchedNetworks = new Vector<SparseNetwork>();
					if (justInserted.isEmpty()) {
						// assessing ordering for incoming edges
						for (SparseNetEdge e : retNode.getInEdges()) {
							if (e.getEdgeIndex().contains(firstIndex))
								e.getSource().setOrder(retNode.getInDegree() - 1);
							else
								e.getSource().setOrder(0);
						}
						switchT2Edges(switchedNetworks, nCopy, retNode, 0, getTreeCluster(f), justIndices);
					}
					// if (justInserted.isEmpty() && speedUp) {
					// // assessing ordering for incoming edges
					// switchNewRetEdge(switchedNetworks, nCopy, retNode,
					// getTreeCluster(f), justIndices);
					// // switchRetEdgeDown(switchedNetworks, nCopy, retNode,
					// // getTreeCluster(f), justIndices);
					// }

					if (justInserted.isEmpty()) {
						// assessing ordering for incoming edges
						for (SparseNetwork nDrag : draggedNetworks.keySet()) {
							SparseNetNode retDragNode = draggedNetworks.get(nDrag);
							for (SparseNetEdge e : retDragNode.getInEdges()) {
								if (e.getEdgeIndex().contains(firstIndex))
									e.getSource().setOrder(retDragNode.getInDegree() - 1);
								else
									e.getSource().setOrder(0);
							}
							switchT2Edges(switchedNetworks, nDrag, retDragNode, 0, getTreeCluster(f), justIndices);
						}
					}
					// if (justInserted.isEmpty() && speedUp) {
					// // assessing ordering for incoming edges
					// for (SparseNetwork nDrag : draggedNetworks.keySet()) {
					// SparseNetNode retDragNode = draggedNetworks.get(nDrag);
					// switchNewRetEdge(switchedNetworks, nDrag, retDragNode,
					// getTreeCluster(f), justIndices);
					// // switchRetEdgeDown(switchedNetworks, nDrag,
					// // retDragNode, getTreeCluster(f), justIndices);
					// }
					// }

					if (switchedNetworks.isEmpty()) {
						newNetworks.add(nCopy);
						for (SparseNetwork draggedNetwork : draggedNetworks.keySet())
							newNetworks.add(draggedNetwork);
					}

					newNetworks.addAll(switchedNetworks);

					// for (SparseNetwork newNet : newNetworks) {
					// // System.out.println(">>" + newNet.getPhyloTree() +
					// // ";");
					// if (justInserted.isEmpty()) {
					// updateNetwork(newNet, indices, getTreeCluster(f));
					// if (!containmentCheck(1, clusters.get(1),
					// getTreeCluster(f), indices)) {
					// System.out.println("Normal Stuff... ");
					// for (HybridTree h : trees)
					// System.out.println(h + ";");
					// System.out.println(n.getPhyloTree() + ";");
					// System.out.println(newNet.getPhyloTree() + ";");
					// System.out.println("vR: " + vR.getLabel() + " " +
					// f.getPhyloTree() + ";");
					// System.out.println(indexToInsertedSet.get(1) + " & " +
					// justInserted);
					// for (SparseTree c : forest)
					// System.out.println(c.getPhyloTree() + ";");
					// }
					// }
					// }

					// resetting clusters to initial network n
					updateNetwork(n, null, null);

				}
			}
		}

		if (debug) {
			System.out.println();
			for (SparseNetwork newNetwork : newNetworks) {
				System.out.println(">> " + newNetwork.getPhyloTree() + ";");
			}
		}

		return newNetworks;

	}

	private void updateNetwork(SparseNetwork n, Vector<Integer> indices, BitSet justInserted) {
		clusters.get(firstIndex).clear();
		nodeToCluster.get(firstIndex).clear();
		clusterToNode.get(firstIndex).clear();
		BitSet inserted = (BitSet) indexToInsertedSet.get(firstIndex).clone();
		updateEdges(n, inserted, firstIndex, firstIndex);
		updateClusters(n.getRoot(), firstIndex, firstIndex, n, inserted);

		for (int treeIndex = 0; treeIndex < trees.length; treeIndex++) {
			if (treeIndex != firstIndex) {
				clusters.get(treeIndex).clear();
				clusterToNode.get(treeIndex).clear();
				nodeToCluster.get(treeIndex).clear();
				inserted = (BitSet) indexToInsertedSet.get(treeIndex).clone();
				if (justInserted != null && indices.contains(treeIndex))
					inserted.or(justInserted);
				updateEdges(n, inserted, firstIndex, treeIndex);
				updateClusters(n.getRoot(), treeIndex, firstIndex, n, inserted);
			}
		}

	}

	private void dragEdgeUp1(HashMap<SparseNetwork, SparseNetNode> outputNetworks, SparseNetwork n,
			SparseNetNode retNode, SparseTree f, SparseNetNode switchNode,
			ConcurrentHashMap<SparseNetwork, SparseNetNode> netToRetNode) {
		SparseNetEdge eRet = null;
		for (SparseNetEdge e : retNode.getInEdges()) {
			if (e.getEdgeIndex().contains(1)) {
				eRet = e;
				break;
			}
		}
		if (eRet.getSource().getOutDegree() == 1 && eRet.getSource().getInDegree() == 1)
			eRet = eRet.getSource().getInEdges().get(0);
		SparseNetNode pTmp = eRet.getSource();

		if (pTmp.getOutDegree() == 2) {

			SparseNetEdge eTree = pTmp.getOutEdges().get(0);
			eTree = eTree.equals(eRet) ? pTmp.getOutEdges().get(1) : eTree;

			if (!isDraggedDownNode(eTree.getTarget(), 0) && !isDraggedUpNode1(eTree.getTarget())) {

				Vector<SparseNetNode> draggingNodes = new Vector<SparseNetNode>();
				draggingNodes.add(pTmp);
				collectDraggingNodes0(eRet, draggingNodes);

				for (SparseNetNode p : draggingNodes) {
					if (p.getOutDegree() == 2)
						switchT1EdgesRec(n, eTree.getTarget(), p, draggingNodes, retNode, outputNetworks, f, "1", 0,
								switchNode, netToRetNode, "");
				}
			}

		} else if (pTmp.getOutDegree() > 2) { // && !f.getInfo().isEmpty()) {

			// System.out.println("-> " + n.getPhyloTree() + ";");

			Vector<SparseNetNode[]> nodePairs = new Vector<SparseNetNode[]>();
			SparseNetNode[] nodePair = { retNode, null };
			SparseNetNode[] nodePair1 = { pTmp, null };
			nodePairs.add(nodePair);
			nodePairs.add(nodePair1);
			Vector<SparseNetEdge[]> edgePairs = new Vector<SparseNetEdge[]>();
			SparseNetEdge[] edgePair = { eRet, null };
			edgePairs.add(edgePair);
			SparseNetwork nCopy = copyNetwork(n, nodePairs, edgePairs);

			SparseNetNode retNodeCopy = nodePairs.get(0)[1];
			SparseNetNode pCopy = nodePairs.get(1)[1];
			SparseNetEdge eRetCopy = edgePairs.get(0)[1];

			Vector<SparseNetEdge> shiftEdges = new Vector<SparseNetEdge>();
			boolean uniqueOneEdge = false;
			for (SparseNetEdge e : pCopy.getOutEdges()) {
				// BitSet b0 = new BitSet(taxaOrdering.size());
				// getClusterByIndex(e.getTarget(), nCopy, b0, 0);
				// BitSet b1 = new BitSet(taxaOrdering.size());
				// getClusterByIndex(e.getTarget(), nCopy, b1, 1);
				// if (!b0.isEmpty() && b1.isEmpty())
				// shiftEdges.add(e);
				if (e.getEdgeIndex().contains(0) && !e.getEdgeIndex().contains(1)) {
					// BitSet b0 = new BitSet(taxaOrdering.size());
					// getClusterByIndex(e.getTarget(), n, b0, 1);
					// boolean uniqueTaxa = true;
					// int index = b0.nextSetBit(0);
					// while (index != -1 && uniqueTaxa) {
					// if (indexToTaxa.get(0).contains(taxaOrdering.get(index)))
					// uniqueTaxa = false;
					// index = b0.nextSetBit(index + 1);
					// }
					// if (uniqueTaxa)
					shiftEdges.add(e);
				} else {
					BitSet b0 = new BitSet(taxaOrdering.size());
					getClusterByIndex(e.getTarget(), n, b0, 0);
					if (b0.isEmpty())
						uniqueOneEdge = true;
				}
				// else if (e.getEdgeIndex().contains(1) &&
				// !e.getEdgeIndex().contains(0)
				// && !(e.getTarget().getInDegree() == 1 &&
				// e.getTarget().getOutDegree() == 1))
				// uniqueOneEdge = true;
			}

			SparseNetNode pPCopy = pCopy.getInEdges().get(0).getSource();
			pPCopy.removeOutEdge(pCopy.getInEdges().get(0));
			SparseNetNode xCopy = new SparseNetNode(pPCopy, nCopy, pCopy.getLabel());
			xCopy.addChild(pCopy);

			for (SparseNetEdge e : shiftEdges) {
				SparseNetNode tCopy = e.getTarget();
				pCopy.removeOutEdge(e);
				xCopy.addChild(tCopy);
			}

			if (pCopy.getOutDegree() == 2 && uniqueOneEdge) {

				// System.out.println("-->\n" + n.getPhyloTree() + ";\n" +
				// nCopy.getPhyloTree() + ";");
				// System.out.println("pCopy: " + pCopy.getLabel() + " " +
				// pCopy.getOutDegree());

				SparseNetEdge eTreeCopy = pCopy.getOutEdges().get(0);
				eTreeCopy = eTreeCopy.equals(eRetCopy) ? pCopy.getOutEdges().get(1) : eTreeCopy;

				if (!isDraggedDownNode(eTreeCopy.getTarget(), 0) && !isDraggedUpNode1(eTreeCopy.getTarget())) {

					Vector<SparseNetNode> draggingNodes = new Vector<SparseNetNode>();
					draggingNodes.add(pCopy);
					collectDraggingNodes0(eRetCopy, draggingNodes);

					for (SparseNetNode p : draggingNodes) {
						if (p.getOutDegree() == 2) {
							// System.out.println(xCopy.getLabel()+"\n"+n.getPhyloTree()+";\n"+nCopy.getPhyloTree()+";");
							switchT1EdgesRec(nCopy, eTreeCopy.getTarget(), pCopy, null, retNodeCopy, outputNetworks, f,
									"1.1", 0, switchNode, netToRetNode, "");
						}
					}
				}

			}

		}
	}

	private void dragEdgeDown(HashMap<SparseNetwork, SparseNetNode> outputNetworks, SparseNetwork n,
			SparseNetNode retNode, SparseTree f, SparseNetNode switchNode,
			ConcurrentHashMap<SparseNetwork, SparseNetNode> draggedSwitchedNetworks, boolean recCall, boolean debug) {
		SparseNetEdge eRet = null;
		for (SparseNetEdge e : retNode.getInEdges()) {
			if (e.getEdgeIndex().contains(1)) {
				eRet = e;
				break;
			}
		}

		if (retNode.getInDegree() > 1)
			eRet = eRet.getSource().getInEdges().get(0);

		// if(debug)
		// System.out.println("-----------DraggingDown " + retNode.getLabel() +
		// "\n" + n.getPhyloTree() + ";");

		if (eRet.getSource().getInDegree() == 1) {

			Vector<SparseNetNode> draggingNodes = new Vector<SparseNetNode>();

			SparseNetNode v = eRet.getSource().getInEdges().get(0).getSource();
			if (retNode.getInDegree() == 1 && f.getInfo().isEmpty())
				v = eRet.getSource();

			SparseNetNode sTmp = eRet.getSource();
			draggingNodes.add(sTmp);
			if (retNode.getInDegree() == 1 && retNode.getOutDegree() != 0) {
				draggingNodes.clear();
				collectDraggingNodes0(eRet, draggingNodes);
			}

			for (SparseNetNode s : draggingNodes) {

				BitSet b1 = new BitSet(taxaOrdering.size());
				getClusterByIndex(s, n, b1, 1);
				b1.and(indexToInsertedSet.get(1));

				BitSet b0 = new BitSet(taxaOrdering.size());
				getClusterByIndex(s, n, b0, 0);

				if ((!b1.isEmpty() || retNode.getInDegree() == 1) && (!isDraggedUpNode0(v) || b0.isEmpty() || recCall)) {

					int i = 0;
					for (SparseNetEdge e : s.getOutEdges()) {
						BitSet bCheck = new BitSet(taxaOrdering.size());
						getClusterByIndex(e.getTarget(), n, bCheck, 0);
						if (e.getEdgeIndex().contains(0) || !bCheck.isEmpty())
							i++;
					}

					if (i > 1 && !recCall) {

						Vector<SparseNetNode[]> nodePairs = new Vector<SparseNetNode[]>();
						SparseNetNode[] nodePair = { switchNode, null };
						nodePairs.add(nodePair);
						for (SparseNetNode dragNode : draggingNodes) {
							SparseNetNode[] dragPair = { dragNode, null };
							nodePairs.add(dragPair);
						}
						Vector<SparseNetEdge[]> edgePairs = new Vector<SparseNetEdge[]>();
						SparseNetEdge[] edgePair = { eRet, null };
						edgePairs.add(edgePair);
						SparseNetwork nCopy = copyNetwork(n, nodePairs, edgePairs);
						SparseNetEdge eRetCopy = edgePairs.get(0)[1];
						SparseNetNode tCopy = eRetCopy.getTarget();
						SparseNetNode sCopy = eRetCopy.getSource();
						SparseNetNode vCopy = eRetCopy.getSource().getInEdges().get(0).getSource();
						HashSet<Integer> eIndices = (HashSet<Integer>) sCopy.getInEdges().get(0).getEdgeIndex().clone();
						vCopy.removeOutEdge(sCopy.getInEdges().get(0));
						SparseNetNode xCopy = new SparseNetNode(vCopy, nCopy, sCopy.getLabel());
						xCopy.addChild(sCopy);
						xCopy.getInEdges().get(0).setEdgeIndex(eIndices);
						xCopy.getOutEdges().get(0).setEdgeIndex(eIndices);
						sCopy.removeOutEdge(eRetCopy);
						xCopy.addChild(tCopy);
						switchT1EdgesRec(nCopy, vCopy, xCopy, null, xCopy.getOutEdges().get(0).getTarget(),
								outputNetworks, f, "2", 0, nodePairs.get(0)[1], draggedSwitchedNetworks, "");
					} else {
						switchT1EdgesRec(n, v, s, null, retNode, outputNetworks, f, "2", 0, switchNode,
								draggedSwitchedNetworks, "");
					}

				}

			}
		}
	}

	private void collectDraggingNodes0(SparseNetEdge e, Vector<SparseNetNode> draggingNodes) {
		if (e.getTarget().getOutDegree() > 1) {
			draggingNodes.add(e.getTarget());
			for (SparseNetEdge eOut : e.getTarget().getOutEdges())
				collectDraggingNodes0(eOut, draggingNodes);
		}
	}

	private void switchT1EdgesRec(SparseNetwork n, SparseNetNode v, SparseNetNode s, Vector<SparseNetNode> sourceNodes,
			SparseNetNode retNode, HashMap<SparseNetwork, SparseNetNode> outputNetworks, SparseTree f, String info,
			int startIndex, SparseNetNode switchNode, ConcurrentHashMap<SparseNetwork, SparseNetNode> netToRetNode,
			String log) {
		Vector<SparseNetEdge> switchingEdges = new Vector<SparseNetEdge>();

		if (v.getOutDegree() > 2 || (v.getOutDegree() == 2 && sourceNodes != null)) {

			for (int i = startIndex; i < v.getOutEdges().size(); i++) {
				SparseNetEdge e = v.getOutEdges().get(i);

				BitSet b = new BitSet(taxaOrdering.size());
				getClusterByIndex(e.getTarget(), n, b, 0);
				BitSet inserted = (BitSet) indexToInsertedSet.get(1).clone();
				inserted.or(getTreeCluster(f));

				BitSet b3 = (BitSet) b.clone();
				b.and(indexToInsertedSet.get(1));

				// TODO
				if ((info != "3" || !b3.intersects(getTreeCluster(f)) || true)
						&& (isDraggedUpNodePrevTree(e.getTarget(), s, true, false) || !info.equals("2"))
						&& (isContractNodeCurrTrees(s, v, insertIndex, false, false, null, false) || info.equals("2"))) {

					BitSet b1 = new BitSet(taxaOrdering.size());
					getClusterByIndex(e.getTarget(), n, b1, 1);
					BitSet b0 = new BitSet(taxaOrdering.size());
					getClusterByIndex(e.getTarget(), n, b0, 0);
					if (!e.getEdgeIndex().contains(1) || b1.isEmpty()) {
						if (!e.getTarget().equals(s))
							// addSwitchingEdges(e, switchingEdges);
							switchingEdges.add(e);
					} else if (info == "1.1" && isDraggedUpNode1(s) && e.getEdgeIndex().contains(0)) {
						if (!e.getTarget().equals(s))
							switchingEdges.add(e);
					}
				}

			}

			// if(info == "2")
			// System.out.println("----"+retNode.getLabel()+" v: "+v.getLabel()+" s: "+s.getLabel()+" "+switchingEdges.size()+"\n"+n.getPhyloTree()+";");

			int counter = 0;
			for (SparseNetEdge e : switchingEdges) {
				if (v.getOutEdges().contains(e))
					counter++;
			}

			if (counter < v.getOutDegree()) {
				for (SparseNetEdge e : switchingEdges) {

					// copying relevant nodes
					Vector<SparseNetNode[]> nodePairs = new Vector<SparseNetNode[]>();
					SparseNetNode[] pairOne = { v, null };
					SparseNetNode[] pairTwo = { s, null };
					SparseNetNode[] pairThree = { retNode, null };
					SparseNetNode[] pairFour = { switchNode, null };
					nodePairs.add(pairOne);
					nodePairs.add(pairTwo);
					nodePairs.add(pairThree);
					nodePairs.add(pairFour);

					if (sourceNodes != null) {
						for (SparseNetNode source : sourceNodes) {
							SparseNetNode[] nodePair = { source, null };
							nodePairs.add(nodePair);
						}
					} else {
						SparseNetNode[] nodePair = { s, null };
						nodePairs.add(nodePair);
					}

					// copying relevant edges
					Vector<SparseNetEdge[]> edgePairs = new Vector<SparseNetEdge[]>();
					SparseNetEdge[] ePairOne = { e, null };
					edgePairs.add(ePairOne);

					// copying network
					SparseNetwork nCopy = copyNetwork(n, nodePairs, edgePairs);

					// switching edge
					SparseNetNode vCopy = nodePairs.get(0)[1];
					SparseNetNode sCopy = nodePairs.get(1)[1];

					// modify network?
					BitSet bSCopy = new BitSet(taxaOrdering.size());
					getClusterByIndex(sCopy, nCopy, bSCopy, 0);
					BitSet bECopy = new BitSet(taxaOrdering.size());
					getClusterByIndex(e.getTarget(), nCopy, bECopy, 1);

					SparseNetNode retNodeCopy = nodePairs.get(2)[1];
					SparseNetEdge eCopy = edgePairs.get(0)[1];
					SparseNetNode tCopy = eCopy.getTarget();
					HashSet<Integer> edgeIndices = (HashSet<Integer>) eCopy.getEdgeIndex().clone();
					SparseNetNode eSourceNode = eCopy.getSource();
					eCopy.getSource().removeOutEdge(eCopy);
					SparseNetEdge eNew = sCopy.addChild(tCopy);
					eNew.setEdgeIndex((HashSet<Integer>) edgeIndices.clone());

					boolean recCall = true;
					if (vCopy.getOutDegree() == 1) {
						removeOneNode(vCopy, nCopy);
						recCall = false;
					} else if (vCopy.getOutDegree() == 0) {
						SparseNetNode vPCopy = vCopy.getInEdges().get(0).getSource();
						vPCopy.removeOutEdge(vCopy.getInEdges().get(0));
						recCall = false;
					}
					removeOneNode(eSourceNode, nCopy);

					// if(info == "3"){
					// System.out.println(">> " + info + " v: " + v.getLabel() +
					// " s: " + s.getLabel() + " retNode: "
					// + retNode.getLabel()+" "+switchingEdges.size() + "\n" +
					// n.getPhyloTree() + ";\n" + nCopy.getPhyloTree() + "; ");
					// }

					outputNetworks.put(nCopy, retNodeCopy);
					SparseNetNode switchNodeCopy = nodePairs.get(3)[1];

					if (netToRetNode != null && switchNodeCopy != null) {
						netToRetNode.put(nCopy, switchNodeCopy);
					}

					if (recCall) {
						Vector<SparseNetNode> sourceNodesCopy = new Vector<SparseNetNode>();
						for (int i = 4; i < nodePairs.size(); i++)
							sourceNodesCopy.add(nodePairs.get(i)[1]);
						for (SparseNetNode sourceCopy : sourceNodesCopy) {
							switchT1EdgesRec(nCopy, vCopy, sourceCopy, sourceNodesCopy, retNodeCopy, outputNetworks, f,
									info, v.getOutEdges().indexOf(e), switchNodeCopy, netToRetNode, log);
						}
					}

				}

			}

		}

	}

	private void dragEdgeUp2(HashMap<SparseNetwork, SparseNetNode> outputNetworks, SparseNetwork n,
			SparseNetNode retNode, SparseTree f, SparseNetNode switchNode,
			ConcurrentHashMap<SparseNetwork, SparseNetNode> draggedSwitchedNetworks) {
		SparseNetNode c = retNode.getOutDegree() != 0 ? retNode.getOutEdges().get(0).getTarget() : retNode;
		if (retNode.getInDegree() == 2 && c.getOutDegree() > 1) {
			SparseNetEdge eT1 = null;
			for (SparseNetEdge e : retNode.getInEdges()) {
				if (e.getEdgeIndex().contains(0)) {
					eT1 = e;
					break;
				}
			}

			if (eT1 != null) {

				// copying relevant nodes and edges
				Vector<SparseNetNode[]> nodePairs = new Vector<SparseNetNode[]>();
				SparseNetNode[] pairOne = { c, null };
				SparseNetNode[] pairTwo = { retNode, null };
				SparseNetNode[] pairThree = { switchNode, null };
				nodePairs.add(pairOne);
				nodePairs.add(pairTwo);
				nodePairs.add(pairThree);
				Vector<SparseNetEdge[]> edgePairs = new Vector<SparseNetEdge[]>();
				SparseNetEdge[] ePairOne = { eT1, null };
				edgePairs.add(ePairOne);
				SparseNetwork nCopy = this.copyNetwork(n, nodePairs, edgePairs);
				SparseNetEdge eT1Copy = edgePairs.get(0)[1];

				// inserting source node
				SparseNetNode x3 = eT1Copy.getSource();
				SparseNetNode x1 = x3.getInEdges().get(0).getSource();
				x1.removeOutEdge(x3.getInEdges().get(0));
				SparseNetNode x2 = new SparseNetNode(x1, nCopy, x1.getLabel());
				x2.addChild(x3);
				x2.getInEdges().get(0).addEdgeIndex(0);
				x2.getOutEdges().get(0).addEdgeIndex(0);

				SparseNetNode cCopy = nodePairs.get(0)[1];
				SparseNetNode retNodeCopy = nodePairs.get(1)[1];
				SparseNetNode switchNodeCopy = nodePairs.get(2)[1];
				switchT1EdgesRec(nCopy, cCopy, x2, null, retNodeCopy, outputNetworks, f, "3", 0, switchNodeCopy,
						draggedSwitchedNetworks, "");
			}

		}
	}

	private void switchT2Edges(Vector<SparseNetwork> outputNetworks, SparseNetwork n, SparseNetNode retNode,
			int edgeIndex, BitSet justInserted, Vector<Integer> justIndices) {

		updateNetwork(n, justIndices, justInserted);
		SparseNetNode vR = retNode;

		if (vR != null && vR.getInDegree() > 1) {

			Collections.sort(vR.getInEdges(), new RetEdgeComparator());
			for (int eIndex = edgeIndex; eIndex < vR.getInEdges().size(); eIndex++) {
				vR = retNode;
				SparseNetEdge eR1 = vR.getInEdges().get(eIndex);

				HashSet<Integer> eR1Indices = (HashSet<Integer>) eR1.getEdgeIndex().clone();
				SparseNetEdge eR2 = eR1.getSource().getInEdges().get(0);
				SparseNetNode p = eR2.getSource();

				// BitSet eRIndexSet = new BitSet(trees.length);
				// for (int index : eR1.getEdgeIndex())
				// eRIndexSet.set(index);

				if (p.getOutDegree() == 2) {

					SparseNetEdge eIn = eR2;

					for (SparseNetEdge eSib2 : p.getOutEdges()) {

						vR = retNode;
						if (eSib2 != eIn) {

							// SparseNetEdge eSib2 =
							// !p.getOutEdges().get(0).equals(eIn) ?
							// p.getOutEdges().get(0) : p
							// .getOutEdges().get(1);

							if ((eSib2.getTarget().getInDegree() == 1 && eSib2.getTarget().getOutDegree() == 1)
									|| (!eSib2.getEdgeIndex().isEmpty() && !eSib2.getEdgeIndex().contains(firstIndex))) {

								// System.out.println(">" + n.getPhyloTree() +
								// ";");
								// System.out.println(eSib2.getSource().getLabel()
								// + " " + eSib2.getTarget().getLabel());
								// System.out.println(eIn.getSource().getLabel()
								// + " " + eIn.getTarget().getLabel());

								SparseNetNode vSib;
								SparseNetEdge eSib1;
								if (eSib2.getTarget().getInDegree() == 1 && eSib2.getTarget().getOutDegree() == 1) {
									eSib1 = eSib2.getTarget().getOutEdges().get(0);
									vSib = eSib1.getTarget();
								} else {
									eSib1 = eSib2;
									vSib = eSib2.getTarget();
								}

								HashSet<Integer> eSib1Indices = (HashSet<Integer>) eSib2.getEdgeIndex().clone();

								if (vSib != vR
										&& isContractNodeCurrTrees(vR, eSib2.getTarget(), insertIndex, true, false,
												null, false)) {

									// if (vSib != vR && doContractPrevTree(vR,
									// eSib2.getTarget(), false)) {

									// collecting possible siblings
									Vector<Integer> commonIndices = new Vector<Integer>();
									for (int index : eSib1Indices) {
										if (eR1Indices.contains(index) && index != firstIndex && index != -1)
											commonIndices.add(index);
									}

									// checking for sibling pair
									// ***************************************************************

									Vector<Integer> toRemove = new Vector<Integer>();
									ConcurrentHashMap<Integer, BitSet[]> indexToSibSet = new ConcurrentHashMap<Integer, BitSet[]>();
									for (int index : commonIndices) {

										BitSet bRet = new BitSet(taxaOrdering.size());
										getClusterByIndex(vR, n, bRet, index);
										BitSet bSib = new BitSet(taxaOrdering.size());
										getClusterByIndex(vSib, n, bSib, index);
										BitSet retSet = (BitSet) bRet.clone();
										retSet.or(bSib);
										BitSet[] sets = { bSib, retSet };
										indexToSibSet.put(index, sets);

										HybridTree t = trees[index];
										BitSet inserted = (BitSet) indexToInsertedSet.get(index).clone();
										inserted.or(justInserted);
										MyNode v1 = t.getClusterToNode().get(bRet);
										if (v1 == null) {
											for (BitSet cluster : t.getClusterToNode().keySet()) {
												BitSet cCopy = (BitSet) cluster.clone();
												cCopy.and(inserted);
												if (cCopy.equals(bRet)) {
													v1 = t.getClusterToNode().get(cluster);
													bRet = cluster;
													break;
												}
											}
										}
										MyNode v2 = t.getClusterToNode().get(bSib);
										if (v2 == null) {
											for (BitSet cluster : t.getClusterToNode().keySet()) {
												BitSet cCopy = (BitSet) cluster.clone();
												cCopy.and(inserted);
												if (cCopy.equals(bSib)) {
													v2 = t.getClusterToNode().get(cluster);
													bSib = cluster;
													break;
												}
											}
										}

										inserted = (BitSet) indexToInsertedSet.get(index).clone();
										inserted.or(justInserted);
										if (v1 != null && v2 != null) {

											MyNode p1 = v1;
											// BitSet p1Set = (BitSet)
											// bRet.clone();
											BitSet bRetCopy = (BitSet) bRet.clone();
											bRetCopy.and(inserted);
											BitSet p1Set = (BitSet) bRetCopy.clone();
											while (p1Set.equals(bRetCopy) && p1.getInDegree() != 0) {
												p1 = p1.getInEdges().next().getSource();
												p1Set = (BitSet) t.getNodeToCluster().get(p1).clone();
												p1Set.and(inserted);
											}

											MyNode p2 = v2;
											BitSet bSetCopy = (BitSet) bSib.clone();
											bSetCopy.and(inserted);
											BitSet p2Set = (BitSet) bSetCopy.clone();
											while (p2Set.equals(bSetCopy) && p2.getInDegree() != 0) {
												p2 = p2.getInEdges().next().getSource();
												p2Set = (BitSet) t.getNodeToCluster().get(p2).clone();
												p2Set.and(inserted);
											}

											if (!p1.equals(p2) || p1.getOutDegree() > 2) {
												toRemove.add(index);
											}

										} else {
											toRemove.add(index);
										}

									}

									// ***************************************************************

									// removing non-siblings
									for (int index : toRemove)
										commonIndices.removeElement(index);

									if (!commonIndices.isEmpty()) {

										for (int txIndex : commonIndices) {

											if (debug)
												System.out.println("Common Index: " + txIndex);

											// copying network
											Vector<SparseNetEdge[]> edgePairs = new Vector<SparseNetEdge[]>();
											SparseNetEdge[] pairOne = { eR1, null };
											SparseNetEdge[] pairTwo = { eSib1, null };
											edgePairs.add(pairOne);
											edgePairs.add(pairTwo);
											SparseNetwork nCopy = copyNetwork(n, null, edgePairs);

											SparseNetEdge eSib1Copy, eR1Copy;
											eSib1Copy = pairTwo[1];
											eR1Copy = pairOne[1];

											SparseNetEdge eSib2Copy = eSib1 != eSib2 ? eSib1Copy.getSource()
													.getInEdges().get(0) : null;
											SparseNetEdge eR2Copy = eR1Copy.getSource().getInEdges().get(0);

											SparseNetNode vSibCopy = eSib1Copy.getTarget();
											SparseNetNode vRetCopy = eR1Copy.getTarget();

											// removing sibling structure
											// *********************************************

											if (txIndex == firstIndex)
												currIndices = (HashSet<Integer>) eSib1.getIndices().clone();

											if (eSib1Indices.size() > 1 || eSib2Copy == null) {
												HashSet<Integer> eSib1IndicesCopy = (HashSet<Integer>) eSib1Copy
														.getEdgeIndex().clone();
												eSib1IndicesCopy.remove(txIndex);
												eSib1Copy.setEdgeIndex(eSib1IndicesCopy);
												if (eSib2Copy != null) {
													HashSet<Integer> eSib2IndicesCopy = (HashSet<Integer>) eSib2Copy
															.getEdgeIndex().clone();
													eSib2IndicesCopy.remove(txIndex);
													eSib2Copy.setEdgeIndex(eSib2IndicesCopy);
												}
											} else {
												eSib1Copy.getSource().removeOutEdge(eSib1Copy);
												if (eSib2Copy != null) {
													eSib2Copy.getSource().removeOutEdge(eSib2Copy);
													removeOneNode(eSib2Copy.getSource(), nCopy);
												}
											}

											// ************************************************************************

											BitSet bSib = new BitSet(taxaOrdering.size());
											getClusterByIndex(vSibCopy, nCopy, bSib, txIndex);
											BitSet bRet = new BitSet(taxaOrdering.size());
											getClusterByIndex(vRetCopy, nCopy, bRet, txIndex);
											HybridTree t = trees[txIndex];
											MyNode rootSib = t.getClusterToNode().get(bSib);
											BitSet inserted = (BitSet) indexToInsertedSet.get(txIndex).clone();
											inserted.or(justInserted);
											if (rootSib == null) {
												for (BitSet cluster : t.getClusterToNode().keySet()) {
													BitSet cCopy = (BitSet) cluster.clone();
													cCopy.and(inserted);
													if (cCopy.equals(bSib)) {
														rootSib = t.getClusterToNode().get(cluster);
														break;
													}
												}
											}
											SparseTree f;
											BitSet sibCluster = bRet;
											if (rootSib != null)
												f = new SparseTree(t.getSubtree(rootSib, true));
											else {
												sibCluster = new BitSet(taxaOrdering.size());
												BitSet b = (BitSet) bSib.clone();
												b.or(bRet);
												for (BitSet cluster : t.getClusterToNode().keySet()) {
													BitSet cCopy = (BitSet) cluster.clone();
													cCopy.and(inserted);
													if (cCopy.equals(b)) {
														rootSib = t.getClusterToNode().get(cluster);
														break;
													}
												}
												f = new SparseTree(t.getSubtree(rootSib, true));
												Vector<SparseTreeNode> toDelete = new Vector<SparseTreeNode>();
												for (SparseTreeNode c : f.getRoot().getChildren()) {
													BitSet bC = new BitSet(taxaOrdering.size());
													getTreeClusterRec(c, bC);
													if (!bC.intersects(bSib))
														toDelete.add(c);
												}
												for (SparseTreeNode c : toDelete)
													c.delete();

												sibCluster = bRet;
											}

											SparseNetNode vRet = vSibCopy;
											if (vSibCopy.getOutDegree() == 1)
												vRet = vSibCopy.getOutEdges().get(0).getTarget();

											if (vSibCopy.getInDegree() == 1 && vSibCopy.getOutDegree() == 1) {
												removeOneNode(vSibCopy.getInEdges().get(0).getSource(), nCopy);
												removeOneNode(vSibCopy, nCopy);
											}

											// Re-inserting component f
											// ***********************************************

											Vector<Integer> txIndices = new Vector<Integer>();
											txIndices.add(txIndex);

											BitSet tmpInserted = (BitSet) indexToInsertedSet.get(txIndex).clone();
											inserted = (BitSet) indexToInsertedSet.get(txIndex).clone();
											if (justIndices.contains(txIndex))
												inserted.or(justInserted);
											BitSet treeCluster = getTreeCluster(f);
											treeCluster.and(inserted);
											inserted.xor(treeCluster);
											indexToInsertedSet.remove(txIndex);
											indexToInsertedSet.put(txIndex, inserted);

											Vector<SparseNetNode> candidates = new Vector<SparseNetNode>();
											if (txIndex != firstIndex)
												getSubnodes(nCopy, vRetCopy, candidates);
											else
												candidates.add(vRetCopy.getOutEdges().get(0).getTarget());

											if (debug) {
												System.out.println("vRet: " + vRet.getLabel());
												System.out.println("vRetCopy: " + vRetCopy.getLabel());
												System.out.println(nCopy.getPhyloTree() + ";");
												System.out.println(f.getPhyloTree() + "; ");
											}

											SparseNetNode[] nodePair = { vRetCopy, null };
											ConcurrentHashMap<SparseNetwork, SparseNetNode> netToRetNode = new ConcurrentHashMap<SparseNetwork, SparseNetNode>();

											Vector<SparseNetwork> switchedNetworks = new Vector<SparseNetwork>();
											switchedNetworks.addAll(addComponent(nCopy, txIndices, firstIndex, f, vRet,
													sibCluster, justInserted, justIndices, candidates, netToRetNode,
													nodePair));

											indexToInsertedSet.remove(txIndex);
											indexToInsertedSet.put(txIndex, tmpInserted);

											// ************************************************************************

											for (SparseNetwork switchedNetwork : switchedNetworks) {

												// System.out.println(">");
												// System.out.println(n.getPhyloTree().toMyBracketString());
												// System.out.println(switchedNetwork.getPhyloTree().toMyBracketString());

												SparseNetNode txSibNode = netToRetNode.get(switchedNetwork);

												int newStartIndex = 0;
												switchT2Edges(outputNetworks, switchedNetwork, txSibNode,
														newStartIndex, justInserted, justIndices);

											}

										}
									}
								}
							}
						}
					}
				}
			}

		}

		outputNetworks.add(n);
	}

	private boolean removeOneNode(SparseNetNode v, SparseNetwork n) {
		if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
			SparseNetNode p = v.getInEdges().get(0).getSource();
			if (p.getInDegree() == 1 || (!n.isSpecial(v.getInEdges().get(0)) && !n.isSpecial(v.getOutEdges().get(0)))) {
				SparseNetNode c = v.getOutEdges().get(0).getTarget();
				HashSet<Integer> newIndices = (HashSet<Integer>) v.getInEdges().get(0).getEdgeIndex().clone();
				p.removeOutEdge(v.getInEdges().get(0));
				v.removeOutEdge(v.getOutEdges().get(0));
				SparseNetEdge e = p.addChild(c);
				e.setEdgeIndex(newIndices);
				return true;
			}
		}
		return false;
	}

	private boolean singleNodeCheck(SparseNetwork n) {
		Vector<String> t1Taxa = new Vector<String>();
		for (MyNode v : t1.getLeaves())
			t1Taxa.add(v.getLabel());
		for (SparseNetNode v : n.getNodes()) {
			if (v.getInDegree() == 1 && v.getOutDegree() == 1) {
				SparseNetEdge e = v.getOutEdges().get(0);
				SparseNetNode t = e.getTarget();
				if (!n.isSpecial(e) && (t.getOutDegree() != 0 || t1Taxa.contains(t.getLabel())))
					return true;
			}
		}
		return false;
	}

	// private boolean wrongLeafCheck(SparseNetwork n, String s) {
	// for (SparseNetNode v : n.getLeaves()) {
	// if (!taxaOrdering.contains(v.getLabel())) {
	// System.out.println(s + "\n" + n.getPhyloTree() + ";");
	// return true;
	// }
	// }
	// return false;
	// }

	private boolean leafCheck(SparseNetNode v, int txIndex) {
		if (v.getInDegree() != 0) {
			SparseNetEdge eX = null, e1 = null;
			for (SparseNetEdge e : v.getInEdges()) {
				if (e.getEdgeIndex().contains(txIndex))
					eX = e;
				if (e.getEdgeIndex().contains(firstIndex))
					e1 = e;
			}
			if (eX != null)
				return leafCheck(eX.getSource(), txIndex);
			else if (e1 != null)
				return leafCheck(e1.getSource(), txIndex);
			else {
				// System.out.println("v: " + v.getLabel() + ", -: " +
				// v.getInDegree() + ", +: " + v.getOutDegree());
				return false;
			}
		} else
			return true;
	}

	private Vector<Vector<Integer>> computePairs(Vector<Vector<Integer>> v1Indices, Vector<Vector<Integer>> vXIndices) {
		Vector<Vector<Integer>> pairs = new Vector<Vector<Integer>>();
		Vector<Vector<Integer>> indices = (Vector<Vector<Integer>>) v1Indices.clone();
		indices.addAll(vXIndices);
		for (int k : indices.get(0)) {
			Vector<Integer> v = new Vector<Integer>();
			v.add(k);
			pairs.add(v);
		}
		computePairsRec(1, pairs, indices);
		return pairs;
	}

	private void computePairsRec(int i, Vector<Vector<Integer>> pairs, Vector<Vector<Integer>> indices) {
		if (i < indices.size()) {
			Vector<Vector<Integer>> newPairs = new Vector<Vector<Integer>>();
			Vector<Integer> vNumbers = indices.get(i);
			for (int k = 0; k < vNumbers.size(); k++) {
				for (Vector<Integer> pair : pairs) {
					if (k < vNumbers.size() - 1) {
						Vector<Integer> pairCopy = (Vector<Integer>) pair.clone();
						pairCopy.add(vNumbers.get(k));
						newPairs.add(pairCopy);
					} else
						pair.add(vNumbers.get(k));
				}
			}
			pairs.addAll(newPairs);
			int newI = i + 1;
			computePairsRec(newI, pairs, indices);
		}
	}

	private Vector<MyNode> getSibling(MyNode v, HybridNetwork n, BitSet insertedSet) {
		Vector<MyNode> siblings = new Vector<MyNode>();

		BitSet b = (BitSet) n.getNodeToCluster().get(v).clone();
		b.and(insertedSet);
		MyNode p = v;
		while (b.isEmpty()) {
			p = p.getInEdges().next().getSource();
			b = (BitSet) n.getNodeToCluster().get(p).clone();
			b.and(insertedSet);
		}
		// p = v.getInEdges().next().getSource();

		if (p.getOutDegree() > 1) {
			Iterator<MyEdge> it = p.getOutEdges();
			while (it.hasNext()) {
				MyNode sib = it.next().getTarget();
				if (sib != v)
					siblings.add(sib);
			}
			return siblings;
		}
		return null;
	}

	private BitSet findChildNode(Vector<MyNode> nodes, HybridNetwork t, int treeIndex, BitSet forestSet,
			BitSet insertedSet) {

		BitSet childSet = new BitSet(taxaOrdering.size());
		for (MyNode v : nodes) {
			childSet.or(t.getNodeToCluster().get(v));
		}
		// BitSet childSet = (BitSet) t.getNodeToCluster().get(v).clone();
		if (debug)
			System.out.println("ChildSet: " + childSet);
		childSet.and(insertedSet);
		if (debug)
			System.out.println("InsertedSet: " + insertedSet + " " + childSet);
		BitSet siblingSet = checkNetworkCluster(childSet, treeIndex, forestSet);
		if (debug)
			System.out.println("SiblingSet: " + siblingSet);

		if (siblingSet != null && siblingSet.cardinality() != 0)
			return siblingSet;
		else {
			MyNode v = nodes.firstElement();
			if (v.getInDegree() != 0) {
				MyNode p = v.getInEdges().next().getSource();
				// return findChildNode(getSibling(p, t), t, treeIndex,
				// forestSet, insertedSet);
				return findChildNode(getSibling(p, t, insertedSet), t, treeIndex, forestSet, insertedSet);
			} else
				return null;
		}
	}

	private BitSet checkNetworkCluster(BitSet childSet, int treeIndex, BitSet forestSet) {
		if (childSet.cardinality() != 0) {
			HashSet<BitSet> clusterSet = clusters.get(treeIndex);
			for (BitSet b : clusterSet) {
				if (b != null) {
					if (b.equals(childSet))
						return b;
				}
			}
		}
		return null;
	}

	private SparseNetNode insertTargetNode(SparseNetwork n, SparseNetNode vR, Vector<Integer> txIndices) {

		log = log.concat("retNode: " + vR.getLabel() + "\n" + n.getPhyloTree() + ";\n");
		// System.out.println("retNode: " + vR.getLabel() + "\n" +
		// n.getPhyloTree() + ";");

		SparseNetNode p = vR.getInEdges().iterator().next().getSource();
		if (p.getInDegree() > 1)
			return p;

		// split in-edge of vR by inserting retNode
		SparseNetNode retNode = new SparseNetNode(null, n, vR.getLabel());

		SparseNetEdge eR = null;
		boolean isSpecial = false;
		if (vR.getInDegree() == 1)
			eR = vR.getInEdges().iterator().next();
		else {
			isSpecial = true;
			Iterator<SparseNetEdge> it = vR.getInEdges().iterator();
			while (it.hasNext()) {
				eR = it.next();
				if (eR.getEdgeIndex().contains(txIndices.firstElement()))
					break;
			}
		}

		SparseNetNode sR = eR.getSource();
		HashSet<Integer> eRIndices = (HashSet<Integer>) eR.getEdgeIndex().clone();

		sR.removeOutEdge(eR);

		SparseNetNode sR2 = new SparseNetNode(null, n, vR.getLabel());
		SparseNetEdge newEdge = sR.addChild(sR2);
		newEdge.setEdgeIndex(eRIndices);

		newEdge = sR2.addChild(retNode);
		newEdge.setEdgeIndex(eRIndices);

		HashSet<Integer> newIndices = (HashSet<Integer>) eRIndices.clone();
		for (int txIndex : txIndices) {
			if (!newIndices.contains(txIndex))
				newIndices.add(txIndex);
		}

		if (!isSpecial) {
			newEdge = retNode.addChild(vR);
			newEdge.setEdgeIndex(newIndices);
		} else {
			SparseNetNode x = new SparseNetNode(null, n, vR.getLabel());
			newEdge = retNode.addChild(x);
			newEdge.setEdgeIndex((HashSet<Integer>) newIndices.clone());
			newEdge = x.addChild(vR);
			newEdge.setEdgeIndex((HashSet<Integer>) newIndices.clone());
		}

		// System.out.println(n.getPhyloTree() + ";");

		return retNode;
	}

	private SparseNetNode insertSourceNode(SparseNetwork n, SparseNetNode retNode, SparseNetNode vX,
			Vector<Integer> txIndices, SparseTree f) {

		// log = log.concat("1" + f.getPhyloTree() + ";vX: " + vX.getLabel() +
		// " retNode:" + retNode.getLabel() + "\n"
		// + n.getPhyloTree() + ";\n");

		// System.out.println("1" + f.getPhyloTree() + ";vX: " + vX.getLabel() +
		// " retNode:" + retNode.getLabel() + "\n"
		// + n.getPhyloTree() + ";");

		SparseNetNode pX = vX.getInEdges().get(0).getSource();
		// System.out.println(isMultifurcatingNode(pX, txIndices.get(0), f) +
		// " "
		// + !isMultifurcatingNode(vX, txIndices.get(0), f));

		if (isMultifurcatingNode(pX, txIndices.get(0), f) && !isMultifurcatingNode(vX, txIndices.get(0), f))
			return null;

		Iterator<SparseNetEdge> it = retNode.getInEdges().iterator();
		while (it.hasNext()) {
			SparseNetEdge e = it.next();
			HashSet<Integer> eIndices = (HashSet<Integer>) e.getEdgeIndex().clone();
			for (int index : txIndices)
				eIndices.remove(index);
			e.setEdgeIndex((HashSet<Integer>) eIndices.clone());

			BitSet b1 = new BitSet(taxaOrdering.size());
			getClusterByIndex(e.getSource(), n, b1, 1);
			while (b1.isEmpty()) {
				SparseNetEdge e1 = null;
				for (SparseNetEdge eIn : e.getSource().getInEdges()) {
					if (eIn.getEdgeIndex().contains(1))
						e1 = eIn;
				}
				if (e1 != null) {
					HashSet<Integer> e1Indices = (HashSet<Integer>) e1.getEdgeIndex().clone();
					for (int index : txIndices)
						e1Indices.remove(index);
					e1.setEdgeIndex((HashSet<Integer>) e1Indices.clone());
					e = e1;
					getClusterByIndex(e.getSource(), n, b1, 1);
				} else
					break;
			}

		}

		SparseNetNode x;
		if (!isMultifurcatingNode(vX, txIndices.get(0), f)) {
			// if (!b.equals(txRetCluster) || !legalNode) {

			// split in-edge of vX by inserting node x
			SparseNetEdge eX = null;
			if (vX.getInDegree() == 1)
				eX = vX.getInEdges().iterator().next();
			else {
				it = vX.getInEdges().iterator();
				while (it.hasNext()) {
					eX = it.next();
					if (eX.getEdgeIndex().contains(txIndices.firstElement()))
						break;
				}
			}

			HashSet<Integer> eXIndices = (HashSet<Integer>) eX.getEdgeIndex().clone();

			SparseNetNode sX = eX.getSource();
			sX.removeOutEdge(eX);

			x = new SparseNetNode(null, n, retNode.getLabel());
			SparseNetEdge newEdge = sX.addChild(x);

			HashSet<Integer> newIndices = (HashSet<Integer>) eXIndices.clone();
			for (int txIndex : txIndices) {
				if (!newIndices.contains(txIndex))
					newIndices.add(txIndex);
			}
			newEdge.setEdgeIndex(newIndices);

			newEdge = x.addChild(vX);
			newEdge.setEdgeIndex(eXIndices);

		} else
			x = vX;

		SparseNetNode cX = null;

		if (retNode.getInDegree() != 0) {
			cX = new SparseNetNode(null, n, retNode.getLabel());
			SparseNetEdge newEdge = x.addChild(cX);
			HashSet<Integer> cXIndices = new HashSet<Integer>();
			cXIndices.addAll(txIndices);
			newEdge.setEdgeIndex((HashSet<Integer>) cXIndices.clone());
		} else
			cX = x;

		// adding reticulate edge
		SparseNetEdge e = cX.addChild(retNode);
		HashSet<Integer> xIndices = new HashSet<Integer>();
		xIndices.addAll(txIndices);
		e.setEdgeIndex((HashSet<Integer>) xIndices.clone());

		if (txIndices.get(0).equals(firstIndex))
			e.addIndices((HashSet<Integer>) currIndices.clone());
		else
			e.addIndex(insertIndex);
		for (SparseNetEdge retEdge : e.getTarget().getInEdges()) {
			if (retEdge.getIndices().isEmpty())
				retEdge.addIndices((HashSet<Integer>) lastIndices.clone());
		}

		// System.out.println(n.getPhyloTree() + ";");

		return cX;
	}

	private boolean isMultifurcatingNode(SparseNetNode v, int treeIndex, SparseTree f) {

		// System.out.println("vX: " + v.getLabel());

		if (v.getInDegree() > 1)
			return false;
		if (v.getOutDegree() == 0)
			return false;
		for (SparseNetEdge e : v.getOutEdges()) {
			if (n.isSpecial(e))
				return false;
		}

		int inDegree = 0;
		Iterator<SparseNetEdge> itIn = v.getInEdges().iterator();
		while (itIn.hasNext()) {
			if (itIn.next().getEdgeIndex().contains(treeIndex))
				inDegree++;
		}
		int outDegree = 0;
		Iterator<SparseNetEdge> itOut = v.getOutEdges().iterator();
		while (itOut.hasNext()) {
			BitSet bChild = new BitSet(taxaOrdering.size());
			SparseNetEdge eOut = itOut.next();
			getClusterByIndex(eOut.getTarget(), n, bChild, treeIndex);
			if (eOut.getEdgeIndex().contains(treeIndex) && !bChild.isEmpty())
				outDegree++;
		}
		if (inDegree == 1 && outDegree == 1)
			return true;

		BitSet txRetCluster = getTreeCluster(f);
		BitSet txVxCluster = new BitSet(taxaOrdering.size());
		getClusterByIndex(v, n, txVxCluster, treeIndex);
		// System.out.println("1) " + txVxCluster);
		txVxCluster.and(indexToInsertedSet.get(treeIndex));
		if (txVxCluster.isEmpty())
			return true;
		// System.out.println("2) " + txVxCluster);
		MyNode lca = trees[1].findLCA(txVxCluster);
		lca = lca.getOutDegree() == 0 ? lca.getInEdges().next().getSource() : lca;
		Iterator<MyEdge> it = lca.getOutEdges();
		// System.out.println("LCA: " + lca.getLabel() + "\n" + trees[1] + ";");
		while (it.hasNext()) {
			MyNode c = it.next().getTarget();
			BitSet b = (BitSet) trees[1].getNodeToCluster().get(c).clone();
			BitSet inserted = (BitSet) indexToInsertedSet.get(treeIndex).clone();
			inserted.or(txRetCluster);
			b.and(inserted);
			if (b.equals(txRetCluster))
				return true;
		}
		return false;
	}

	private SparseNetwork copyNetwork(SparseNetwork n, Vector<SparseNetNode[]> nodePairs,
			Vector<SparseNetEdge[]> edgePairs) {
		SparseNetwork nCopy = new SparseNetwork(new SparseNetNode(null, null, n.getRoot().getLabel()));
		nCopy.getRoot().setOwner(nCopy);
		copyNetworkRec(nCopy.getRoot(), n.getRoot(), nCopy, n, new Vector<SparseNetNode>(),
				new ConcurrentHashMap<SparseNetNode, SparseNetNode>(), nodePairs, edgePairs);
		return nCopy;
	}

	private void copyNetworkRec(SparseNetNode vCopy, SparseNetNode v, SparseNetwork nCopy, SparseNetwork n,
			Vector<SparseNetNode> visited, ConcurrentHashMap<SparseNetNode, SparseNetNode> nodeToCopy,
			Vector<SparseNetNode[]> nodePairs, Vector<SparseNetEdge[]> edgePairs) {
		Iterator<SparseNetEdge> it = v.getOutEdges().iterator();
		while (it.hasNext()) {
			SparseNetEdge e = it.next();
			SparseNetNode c = e.getTarget();
			SparseNetNode cCopy;
			SparseNetEdge eCopy;
			if (nodeToCopy.containsKey(c)) {
				cCopy = nodeToCopy.get(c);
				eCopy = vCopy.addChild(cCopy);
			} else {
				cCopy = new SparseNetNode(vCopy, nCopy, c.getLabel());
				if (c.getOrder() != null)
					cCopy.setOrder(c.getOrder());
				eCopy = cCopy.getInEdges().firstElement();
			}
			if (edgePairs != null) {
				for (SparseNetEdge[] edgePair : edgePairs) {
					if (e.equals(edgePair[0]))
						edgePair[1] = eCopy;
				}
			}
			if (n.isSpecial(e)) {
				nodeToCopy.put(c, cCopy);
				eCopy.addIndices((HashSet<Integer>) e.getIndices().clone());
			}
			HashSet<Integer> treeIndices = (HashSet<Integer>) e.getEdgeIndex().clone();
			eCopy.setEdgeIndex(treeIndices);
			if (!visited.contains(c)) {
				visited.add(c);
				copyNetworkRec(cCopy, c, nCopy, n, visited, nodeToCopy, nodePairs, edgePairs);
			}
		}
		if (nodePairs != null) {
			for (SparseNetNode[] nodePair : nodePairs) {
				if (v.equals(nodePair[0]))
					nodePair[1] = vCopy;
			}
		}
	}

	private void updateEdges(SparseNetwork n, BitSet b, int t1Index, int txIndex) {
		Vector<String> leaves = new Vector<String>();
		int i = b.nextSetBit(0);
		while (i != -1) {
			leaves.add(taxaOrdering.get(i));
			i = b.nextSetBit(i + 1);
		}
		for (SparseNetNode v : n.getLeaves()) {
			if (leaves.contains(v.getLabel()))
				updateEdgesRec(n, v, t1Index, txIndex);
		}
	}

	private void updateEdgesRec(SparseNetwork n, SparseNetNode v, int t1Index, int txIndex) {

		// System.out.println(txIndex + " " + v.getLabel());

		if (v.getInDegree() != 0) {
			Iterator<SparseNetEdge> it = v.getInEdges().iterator();
			SparseNetEdge e1 = null, ex = null;
			while (it.hasNext()) {
				SparseNetEdge e = it.next();
				if (e.getEdgeIndex().contains(t1Index))
					e1 = e;
				if (e.getEdgeIndex().contains(txIndex))
					ex = e;
			}
			if (ex == null && e1 == null && v.getInDegree() == 1)
				e1 = v.getInEdges().get(0);
			SparseNetNode p;
			if (ex != null)
				p = ex.getSource();
			else {
				HashSet<Integer> indices = (HashSet<Integer>) e1.getEdgeIndex().clone();
				// edgeToIndices.remove(e1);
				indices.add(txIndex);
				e1.setEdgeIndex(indices);
				// edgeToIndices.get(e1).add(txIndex);
				p = e1.getSource();
			}
			updateEdgesRec(n, p, t1Index, txIndex);
		}
	}

	private HashSet<String> updateClusters(SparseNetNode v, int treeIndex, int t1Index, SparseNetwork n,
			BitSet insertedSet) {
		HashSet<String> vTaxa = new HashSet<String>();
		BitSet cluster = new BitSet(taxaOrdering.size());
		if (v.getOutDegree() != 0) {
			for (SparseNetEdge e : v.getOutEdges()) {
				if (e.getEdgeIndex().contains(treeIndex)) {
					SparseNetNode child = e.getTarget();
					HashSet<String> cTaxa = updateClusters(child, treeIndex, t1Index, n, insertedSet);
					for (String l : cTaxa) {
						if (!vTaxa.contains(l))
							vTaxa.add(l);
					}
				}
			}
			for (String l : vTaxa) {
				int bitIndex = taxonToIndex.get(l);
				// int bitIndex = taxaOrdering.indexOf(l);
				cluster.set(bitIndex);
			}
		} else {
			// if(!taxonToIndex.containsKey(v.getLabel())){
			// System.out.println("---\n"+trees[0].toMyBracketString());
			// System.out.println(trees[1].toMyBracketString());
			// System.out.println(n.getPhyloTree().toMyBracketString()+"\n---");
			// }
			int bitIndex = taxonToIndex.get(v.getLabel());
			// int bitIndex = taxaOrdering.indexOf(v.getLabel());
			if (insertedSet.get(bitIndex)) {
				vTaxa.add(v.getLabel());
				cluster.set(bitIndex);
			}
		}
		boolean isReachable = false;
		Iterator<SparseNetEdge> it = v.getInEdges().iterator();
		while (it.hasNext() && !isReachable) {
			if (it.next().getEdgeIndex().contains(treeIndex))
				isReachable = true;
		}
		if (isReachable
				&& (v.getOutDegree() >= 2 || v.getOutDegree() == 0 || (v.getOutDegree() == 1 && v.getInDegree() == 1))) {

			if (!clusterToNode.get(treeIndex).containsKey(cluster))
				clusterToNode.get(treeIndex).put(cluster, new Vector<SparseNetNode>());

			clusterToNode.get(treeIndex).get(cluster).add(v);
			nodeToCluster.get(treeIndex).put(v, cluster);
			if (!clusters.get(treeIndex).contains(cluster))
				clusters.get(treeIndex).add(cluster);

			Vector<SparseNetNode> hangNodes = new Vector<SparseNetNode>();
			getHangingNode(v, treeIndex, t1Index, n, insertedSet, hangNodes, cluster);
			for (SparseNetNode h : hangNodes) {
				clusterToNode.get(treeIndex).get(cluster).add(h);
				nodeToCluster.get(treeIndex).put(h, (BitSet) cluster.clone());
			}

		}
		return vTaxa;
	}

	private void getHangingNode(SparseNetNode v, int treeIndex, int t1Index, SparseNetwork n, BitSet insertedSet,
			Vector<SparseNetNode> hangingNodes, BitSet cluster) {

		boolean globalConstraintCheck = false;
		if (v.getOutDegree() > 2) {
			globalConstraintCheck = true;
			for (SparseNetEdge e1 : v.getOutEdges()) {
				if (e1.getEdgeIndex().contains(1)) {
					SparseNetNode h = e1.getTarget();
					BitSet b = new BitSet(taxaOrdering.size());
					getClusterByIndex(h, n, b, 1);
					b.and(indexToInsertedSet.get(1));
					if (!b.equals(cluster)) {
						globalConstraintCheck = false;
						break;
					}
				}
			}
		} else
			globalConstraintCheck = true;

		if (globalConstraintCheck) {
			for (SparseNetEdge e1 : v.getOutEdges()) {

				SparseNetNode h = e1.getTarget();
				if (!n.isSpecial(e1) && !e1.getEdgeIndex().contains(treeIndex)) {

					boolean leafCheckOne = true;
					if (h.getOutDegree() == 0) {
						int bitIndex = taxonToIndex.get(h.getLabel());
						leafCheckOne = !insertedSet.get(bitIndex);
					}
					if (leafCheckOne) {

						BitSet b = new BitSet(taxaOrdering.size());
						getClusterByIndex(h, n, b, t1Index);
						boolean constraintCheck = checkConstraints(b, treeIndex);

						if (constraintCheck) {
							boolean isHangingNode = true;
							for (SparseNetEdge e2 : h.getOutEdges()) {
								if (e2.getEdgeIndex().contains(treeIndex)) {
									isHangingNode = false;
									break;
								}
							}
							if (isHangingNode) {
								hangingNodes.add(h);
								getHangingNode(h, treeIndex, t1Index, n, insertedSet, hangingNodes, cluster);
							}
						}
					}
				}
			}
		}
	}

	private boolean checkConstraints(BitSet b, int txIndex) {
		int i = b.nextSetBit(0);
		while (i != -1) {
			if (clusterConstraints.get(txIndex).containsKey(i)) {
				BitSet con = clusterConstraints.get(txIndex).get(i);
				BitSet conCheck = (BitSet) con.clone();
				conCheck.and(b);
				if (!conCheck.equals(con))
					return false;
			}
			i = b.nextSetBit(i + 1);
		}
		return true;
	}

	private void uniqueNodeLabels(SparseNetwork n) {
		Vector<String> nodeLabels = new Vector<String>();
		for (String label : taxaOrdering)
			nodeLabels.add(label);
		int k = 0;
		for (SparseNetNode v : n.getNodes()) {
			if (v.getOutDegree() != 0) {
				String l = k + "";
				while (nodeLabels.contains(l)) {
					k++;
					l = k + "";
				}
				v.setLabel(l);
				nodeLabels.add(l);
			}
		}
	}

	private void getClusterByIndex(SparseNetNode v, SparseNetwork n, BitSet b, int txIndex) {
		if (v.getOutDegree() == 0 && indexToTaxa.get(txIndex).contains(v.getLabel())) {
			// if (v.getOutDegree() == 0){
			b.set(taxonToIndex.get(v.getLabel()));
			// b.set(taxaOrdering.indexOf(v.getLabel()));
		} else {
			Iterator<SparseNetEdge> it = v.getOutEdges().iterator();
			while (it.hasNext()) {
				SparseNetEdge e = it.next();
				if (e.getEdgeIndex().contains(txIndex))
					getClusterByIndex(e.getTarget(), n, b, txIndex);
			}
		}
	}

	private void getClusterByPrevTree(SparseNetNode v, SparseNetwork n, BitSet b, int index, boolean debug) {
		if (v.getOutDegree() == 0 && prevTreeToTaxa.get(index).contains(v.getLabel())) {
			b.set(taxonToIndex.get(v.getLabel()));
		} else {
			Iterator<SparseNetEdge> it = v.getOutEdges().iterator();
			while (it.hasNext()) {
				SparseNetEdge e = it.next();
				if (e.getIndices().contains(index) || e.getTarget().getInDegree() <= 1)
					getClusterByPrevTree(e.getTarget(), n, b, index, debug);
				else if (debug)
					System.out.println(e.getSource().getLabel() + " -> " + e.getTarget().getLabel() + " "
							+ e.getIndices().size());
			}
		}
	}

	private BitSet getTreeCluster(SparseTree t) {
		BitSet b = new BitSet(taxaOrdering.size());
		for (SparseTreeNode v : t.getLeaves()) {
			b.set(taxonToIndex.get(v.getLabel()));
			// b.set(taxaOrdering.indexOf(v.getLabel()));
		}
		return b;
	}

	private void getTreeClusterRec(SparseTreeNode v, BitSet b) {
		if (v.getOutDegree() == 0)
			b.set(taxaOrdering.indexOf(v.getLabel()));
		else {
			for (SparseTreeNode c : v.getChildren())
				getTreeClusterRec(c, b);
		}
	}

	private void getTreeClusterRec(MyNode v, BitSet b) {
		if (v.getOutDegree() == 0)
			b.set(taxaOrdering.indexOf(v.getLabel()));
		else {
			Iterator<MyEdge> it = v.getOutEdges();
			while (it.hasNext())
				getTreeClusterRec(it.next().getTarget(), b);
		}
	}

	private BitSet getNetworkCluster(SparseNetwork n) {
		BitSet b = new BitSet(taxaOrdering.size());
		for (SparseNetNode v : n.getLeaves()) {
			b.set(taxonToIndex.get(v.getLabel()));
		}
		return b;
	}

	private void getSubnodes(SparseNetwork n, SparseNetNode v, Vector<SparseNetNode> subNodes) {
		Iterator<SparseNetEdge> it = v.getOutEdges().iterator();
		subNodes.add(v);
		while (it.hasNext()) {
			getSubnodes(n, it.next().getTarget(), subNodes);
		}
	}

	private boolean prevTreeContainmentCheck(SparseNetwork n) {
		for (int i = 0; i < insertIndex; i++) {
			HashSet<BitSet> treeClusters = hybridTrees[i].getClusterSet();
			HashSet<BitSet> netClusters = new HashSet<BitSet>();
			for (SparseNetNode v : n.getNodes()) {
				BitSet cluster = new BitSet(taxaOrdering.size());
				getClusterByPrevTree(v, n, cluster, i, false);
				if (!cluster.isEmpty())
					netClusters.add(cluster);
			}
			for (BitSet treeCluster : treeClusters) {
				if (!netClusters.contains(treeCluster))
					return false;
			}
		}
		return true;
	}

	private boolean prevTreeContainmentCheckVerbose(SparseNetwork n, String s) {

		for (int i = 0; i < insertIndex; i++) {
			HashSet<BitSet> treeClusters = hybridTrees[i].getClusterSet();
			HashSet<BitSet> netClusters = new HashSet<BitSet>();
			for (SparseNetNode v : n.getNodes()) {
				BitSet cluster = new BitSet(taxaOrdering.size());
				getClusterByPrevTree(v, n, cluster, i, false);
				if (!cluster.isEmpty())
					netClusters.add(cluster);
			}
			for (BitSet treeCluster : treeClusters) {
				if (!netClusters.contains(treeCluster)) {
					System.out.println("ERROR - Tree not contained: " + i + " / " + insertIndex + " | " + s);
					System.out.println(n.getPhyloTree() + ";");
					System.out.println(hybridTrees[i] + ";\n----");
					for (int j = 0; j < insertIndex; j++)
						System.out.println(hybridTrees[j] + ";");
					int index = treeCluster.nextSetBit(0);
					while (index != -1) {
						System.out.print(taxaOrdering.get(index) + " ");
						index = treeCluster.nextSetBit(index + 1);
					}
					System.out.println();
					return false;
				}
			}
		}
		return true;
	}

	private boolean containmentCheck(int txIndex, HashSet<BitSet> netClusters, BitSet justInserted,
			Vector<Integer> indices) {

		if (doContainmentCheck == false)
			return true;

		HashSet<BitSet> clusters = (HashSet<BitSet>) trees[txIndex].getClusterSet().clone();
		HashSet<BitSet> treeClusters = new HashSet<BitSet>();
		BitSet inserted = (BitSet) indexToInsertedSet.get(txIndex).clone();
		if (indices != null && indices.contains(txIndex) && justInserted != null)
			inserted.or(justInserted);
		for (BitSet b : clusters) {
			BitSet b2 = (BitSet) b.clone();
			b2.and(inserted);
			treeClusters.add(b2);
		}

		for (BitSet bNet : netClusters) {
			if (!bNet.isEmpty() && !treeClusters.contains(bNet)) {
				System.out.println("\nContainmentCheck FAILED!");
				System.out.println(treeClusters);
				System.out.println(netClusters);
				return false;
			}
		}
		return true;

	}

	public void freeMemory() {
		trees = null;
		t1 = null;
		forest = null;
		currIndices = null;
		lastIndices = null;
		compValue = null;
		inputTrees = null;
		hybridTrees = null;
		clusterConstraints = null;
		clusterToNode = null;
		nodeToCluster = null;
		clusters = null;
		indexToInsertedSet = null;
		taxonToIndex = null;
		taxaOrdering = null;
		indexToTaxa = null;
		prevTreeToTaxa = null;
		repNetworks = null;
	}

	public int getTreeIndex() {
		return insertIndex;
	}

	public void setStop(boolean b) {
		this.isStoppped = b;
	}

	public int getMyPriority() {
		return myPriority;
	}

}
