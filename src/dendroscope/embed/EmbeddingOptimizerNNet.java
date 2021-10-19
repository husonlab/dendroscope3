/*
 *   EmbeddingOptimizerNNet.java Copyright (C) 2020 Daniel H. Huson
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
package dendroscope.embed;

import dendroscope.consensus.LSATree;
import dendroscope.consensus.NeighborNetCycle;
import dendroscope.consensus.SplitSystem;
import dendroscope.consensus.Taxa;
import dendroscope.tanglegram.TanglegramUtils;
import dendroscope.util.PhyloTreeUtils;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.IteratorUtils;
import jloda.util.progress.ProgressListener;

import java.util.*;

/**
 * compute an optimal embedding using the Neighbor-net heuristic
 * Daniel Huson, Celine Scornavacca 7.2010
 */
public class EmbeddingOptimizerNNet implements ILayoutOptimizer {
    public final boolean DEBUG = false;
    public final boolean DEBUG_time = false;
    public boolean Heuristic_only_rho = true;
    public final boolean printILP = false;

    private List<String> firstOrder = null;
    private List<String> secondOrder = null;

    /**
     * apply the embedding algorithm to a single tree
     *
     * @param tree
     * @param progressListener
     */
    public void apply(PhyloTree tree, ProgressListener progressListener) throws CanceledException {

        if (printILP) {
            int tempIndex = 1;
            for (Node v : tree.nodes()) {
                if (v.getOutDegree() != 0) {
                    tree.setLabel(v, Integer.toString(tempIndex));
                    tempIndex++;
                }
            }
        }

        if (tree.getRoot() == null || tree.getNumberSpecialEdges() == 0) {
            tree.getNode2GuideTreeChildren().clear();
            return;
        }
        //System.err.println("Computing optimal embedding using circular-ordering algorithm");
        apply(new PhyloTree[]{tree}, progressListener, false, true);
    }

    /**
     * apply the embedding algorithm to a whole set of trees
     *
     * @param trees
     * @param progressListener
     * @param useFastAlignmentHeuristic
     */
    public int apply(PhyloTree[] trees, ProgressListener progressListener, boolean shortestPath, boolean useFastAlignmentHeuristic) throws CanceledException {
        if (progressListener != null) {
            progressListener.setSubtask("Neighbor-net heuristic");
            progressListener.setCancelable(false);
            progressListener.setMaximum(-1);
            progressListener.setProgress(-1);
        }
        //System.err.println("Computing optimal embedding using circular-ordering algorithm");
        final Map<String, Integer> taxon2Id = new HashMap<String, Integer>();
        final Map<Integer, String> id2Taxon = new HashMap<Integer, String>();

        long timeBef = System.currentTimeMillis();
        long timeStart = System.currentTimeMillis();
        long timeNeeded = 0;

        Vector<Node> dummyLeaves = new Vector<Node>();
        int count = 1;
        int idRho = 0;

        // add formal root nodes
        for (PhyloTree tree : trees) {
            Node dummyLeaf = new Node(tree);
            Node newRoot = new Node(tree);
            dummyLeaves.add(dummyLeaf);
            tree.newEdge(newRoot, tree.getRoot());
            tree.newEdge(newRoot, dummyLeaf);
            tree.setRoot(newRoot);
            tree.setLabel(dummyLeaf, "rho****");

            Taxa tax = TanglegramUtils.getTaxaForTanglegram(tree);
            for (Iterator<String> taxIt = tax.iterator(); taxIt.hasNext(); ) {
                String taxName = taxIt.next();
                if (!taxon2Id.keySet().contains(taxName)) {
                    taxon2Id.put(taxName, count);
                    id2Taxon.put(count, taxName);
                    if (taxName.equals("rho****"))
                        idRho = count;
                    count++;
                }
            }
        }

        int[] circularOrdering;
        //if(!useFastAlignmentHeuristic)
        //    shortestPath =true;
        if (shortestPath)
            circularOrdering = computerCircularOrderingShortestPathMatrix(trees, taxon2Id, id2Taxon);
        else
            circularOrdering = computerCircularOrderingHardwiredMatrix(trees, taxon2Id, id2Taxon);

        if (DEBUG_time) {
            timeNeeded = System.currentTimeMillis() - timeBef;
            timeBef = System.currentTimeMillis();
            System.err.println(" breakpoint 1:" + timeNeeded);
        }

        int[] bestOrdering = getLinearOrderingId(circularOrdering, idRho);

        if (!useFastAlignmentHeuristic && trees.length == 2) {
            if (progressListener != null)
                progressListener.setCancelable(true);

            int[][] circularOrderingPair = new int[2][];
            circularOrderingPair[0] = circularOrdering;
            circularOrderingPair[1] = new int[circularOrdering.length];
            int lenghtCircOrdering = circularOrdering.length;


            for (int i = 1; i < lenghtCircOrdering; i++)
                circularOrderingPair[1][i] = circularOrderingPair[0][lenghtCircOrdering - i];


            if (DEBUG) {
                //System.err.println("Best score: " + bestScore);
                System.err.print("Best bestOrderingFinal: ");
                for (int ii = 1; ii < bestOrdering.length; ii++) {
                    System.err.print(" " + id2Taxon.get(bestOrdering[ii]));
                }
                System.err.println();
            }


            Vector<PhyloTree>[] forests;
            forests = new Vector[2];
            forests[0] = new Vector<>();
            forests[1] = new Vector<>();
            Vector<List<String>>[] tempOrder = new Vector[2];
            tempOrder[0] = new Vector<>();
            tempOrder[1] = new Vector<>();

            List<String>[] newOrder = new List[2];
            newOrder[0] = new LinkedList<>();
            newOrder[1] = new LinkedList<>();

            if (DEBUG_time) {
                timeNeeded = System.currentTimeMillis() - timeBef;
                timeBef = System.currentTimeMillis();
                System.err.println(" breakpoint 2:" + timeNeeded);
            }

            List<String> currOrderingListNew = new LinkedList<String>();
            for (int i = 1; i < bestOrdering.length; i++) {
                currOrderingListNew.add(id2Taxon.get(bestOrdering[i]));
            }


            if (!currOrderingListNew.contains("rho****"))
                currOrderingListNew.add(0, "rho****");

            int best = Integer.MAX_VALUE;
            int swapTourNew = 0;
            while (swapTourNew < 5) {
                //System.err.println("swapTourNew " +swapTourNew);
                if (swapTourNew != 0) {
                    forests[0].clear();
                    forests[1].clear();
                    currOrderingListNew = newOrder[swapTourNew % 2];
                    tempOrder[0].clear();
                    tempOrder[1].clear();
                }

                //System.err.println("currOrderingListNew:" + currOrderingListNew.toString());
                swapTourNew++;

                for (int s = 0; s < 2; s++) {
                    //System.err.println("tree " +s);

                    for (Iterator<Node> itN = trees[s].nodes().iterator(); itN.hasNext(); ) {
                        Node node = itN.next();

                        if (node.getInDegree() > 1) {
                            var tempP = copyNoRet(trees[s], node);
                            LSATree.computeLSAOrdering(tempP);
                            forests[s].add(tempP);
                        }
                    }
                    var tempP = copyNoRet(trees[s], trees[s].getRoot());
                    LSATree.computeLSAOrdering(tempP);
                    forests[s].add(tempP);

                    for (int a = 0; a < forests[s].size(); a++) {
                        TanglegramUtils.lsaOptimization(forests[s].get(a), currOrderingListNew, 0, null, null);
                        final List<String> tempOrd = new ArrayList<>();
                        TanglegramUtils.getLsaOrderRec(forests[s].get(a), forests[s].get(a).getRoot(), tempOrd);

                        //todo : some trees have "?" as leaves (ex paper) solve this problem
                        //for now we do


                        int sizeBefore = tempOrd.size();
                        for (int ind = 0; ind < sizeBefore; ind++) {
                            if (tempOrd.get(ind) == null) {
                                tempOrd.remove(tempOrd.get(ind));
                                sizeBefore--;
                                ind--;
                            }

                        }

                        tempOrder[s].add(tempOrd);
                    }

                    List<String> bestOrdForNow = new LinkedList<String>();
                    for (int ss = 0; ss < tempOrder[s].get(0).size(); ss++) {
                        bestOrdForNow.add(tempOrder[s].get(0).get(ss));
                    }

                    //System.err.println("tempOrder[s].get(0) " + tempOrder[s].get(0).toString());

                    for (int a = 1; a < tempOrder[s].size(); a++) {
                        List<String> copy = new LinkedList<String>();
                        List<String> bestOrdTemp = new LinkedList<String>();
                        List<String> toInsert = tempOrder[s].get(a);

                        //not needed, it is seems to work the same without adding the rest of the order
                        //List<String> rest = new LinkedList<String>();

                        //for (int ss=a+1;ss<tempOrder[s].size();ss++)
                        //    rest.addAll(tempOrder[s].get(ss));

                        //System.err.println("rest " + rest.toString());

                        int lastOneInsOfThisTree = 0; //to avoid to mes up the ordering in the tree

                        //System.err.println("toInsert " + toInsert.toString());

                        for (int sss = 0; sss < toInsert.size(); sss++) {

                            int min = Integer.MAX_VALUE;

                            String stringToInsert = toInsert.get(sss);

                            if (stringToInsert.equalsIgnoreCase("rho****")) {
                                bestOrdForNow.add(0, "rho****");
                                lastOneInsOfThisTree = 1;
                            } else {
                                //if(stringToInsert.equalsIgnoreCase("t47") || stringToInsert.equalsIgnoreCase("t48")){
                                //System.err.println("stringToInsert " + stringToInsert);
                                //    System.err.println("toInsert " + toInsert.toString());
                                ///}

                                for (int p = lastOneInsOfThisTree; p < bestOrdForNow.size() + 1; p++) {  //to avoid to mes up the ordering in the tree
                                    for (String aBestOrdForNow : bestOrdForNow) copy.add(aBestOrdForNow);
                                    copy.add(p, stringToInsert);
                                    int taxaSetsThatAgree = 0;

                                    //check if is not overlapping!

                                    List<List<String>> alreadyInsertedSets = new LinkedList<List<String>>();
                                    List<String> alreadyInsertedForThisSet = new LinkedList<String>();

                                    for (int aa = 0; aa < a; aa++) {
                                        alreadyInsertedSets.add(tempOrder[s].get(aa)); //taxa of the trees of F(N) already inserted
                                    }

                                    for (int index = 0; index < sss + 1; index++) {
                                        alreadyInsertedForThisSet.add(toInsert.get(index));   //taxa of this tree  already inserted
                                    }

                                    if (alreadyInsertedSets.size() != 0 && sss != 0) {
                                        for (int in = 0; in < alreadyInsertedSets.size(); in++) {
                                            int inf = 0;
                                            int sup = 0;

                                            int indexPastY = -1;
                                            boolean trySupLoop = true;

                                            for (int ind = 0; ind < alreadyInsertedSets.get(in).size(); ind++) {
                                                String x = alreadyInsertedSets.get(in).get(ind); //it ensure a<b<c<d as in the paper
                                                if (copy.indexOf(x) > indexPastY) {
                                                    for (int inde = 0; inde < alreadyInsertedForThisSet.size(); inde++) {  //only against the newly inserted
                                                        String y = alreadyInsertedForThisSet.get(inde);
                                                        if (copy.indexOf(x) < copy.indexOf(y)) {
                                                            inf++;
                                                            indexPastY = copy.indexOf(y);
                                                            inde = alreadyInsertedForThisSet.size(); //it ensure a<b<c<d as in the paper
                                                        }
                                                        if (inf == 2) {
                                                            ind = alreadyInsertedSets.get(in).size(); //uneusefull to continue
                                                            trySupLoop = false;
                                                        }
                                                    }
                                                }
                                            }

                                            int indexPastX = -1;

                                            if (trySupLoop) {
                                                for (int inde = 0; inde < alreadyInsertedForThisSet.size(); inde++) {  //only against the newly inserted

                                                    String y = alreadyInsertedForThisSet.get(inde);
                                                    if (copy.indexOf(y) > indexPastX) {
                                                        for (int ind = 0; ind < alreadyInsertedSets.get(in).size(); ind++) {
                                                            String x = alreadyInsertedSets.get(in).get(ind); //it ensure a<b<c<d as in the paper
                                                            if (copy.indexOf(y) < copy.indexOf(x)) {
                                                                sup++;
                                                                indexPastX = copy.indexOf(x);
                                                                ind = alreadyInsertedSets.get(in).size(); //it ensure a<b<c<d as in the paper
                                                            }
                                                            if (sup == 2)
                                                                inde = alreadyInsertedForThisSet.size(); //uneusefull to continue
                                                        }
                                                    }
                                                }
                                            }
                                            if (inf > 1 || sup > 1) {
                                                in = alreadyInsertedSets.size(); //it is enough one taxa set that does not agree
                                            } else {
                                                taxaSetsThatAgree++;
                                            }
                                        }
                                    } else if (sss == 0) {
                                        taxaSetsThatAgree = alreadyInsertedSets.size(); //the first is always good
                                    }
                                    if (taxaSetsThatAgree == alreadyInsertedSets.size()) {
                                        int value = TanglegramUtils.computeCrossingNum(copy, currOrderingListNew);
                                        if (value <= min) {
                                            min = value;
                                            bestOrdTemp.clear();
                                            for (int in = 0; in < bestOrdForNow.size() + 1; in++) {  //I do not want to copy rest
                                                bestOrdTemp.add(copy.get(in));

                                            }
                                            lastOneInsOfThisTree = p + 1;  //to avoid to mes up the ordering in the tree
                                        }
                                    }
                                    copy.clear();
                                }

                                bestOrdForNow.clear();
                                for (String aBestOrdTemp : bestOrdTemp) bestOrdForNow.add(aBestOrdTemp);
                            }
                        }
                    }
                    newOrder[s] = bestOrdForNow;
                    if (DEBUG) {
                        if (newOrder[s].size() != currOrderingListNew.size()) {
                            System.err.println("\n\nERROR newOrder Partial\n\n");
                            System.err.println(" bestEnd  " + newOrder[s].toString());

                            System.err.println(currOrderingListNew.size() + " currOrderingListNew  " + currOrderingListNew.toString());
                            for (String str : currOrderingListNew) {
                                if (!newOrder[s].contains(str)) {
                                    System.err.println(" missing  " + str);

                                }
                            }
                        }
                    }
                }
                int score = TanglegramUtils.computeCrossingNum(newOrder[0], newOrder[1]);
                if (score == best)
                    break;
            }

            LSATree.computeLSAOrdering(trees[0]);
            LSATree.computeLSAOrdering(trees[1]);

            int finalScore = TanglegramUtils.computeCrossingNum(newOrder[0], newOrder[1]);   // the two orderings for Daniel

            // get rid of dummy leaves
            for (Node v : dummyLeaves) {
                PhyloTree tree = (PhyloTree) v.getOwner();
                for (Node w = tree.getFirstNode(); w != null; w = w.getNext()) {
                    List<Node> children = tree.getNode2GuideTreeChildren().get(w);
                    if (children != null)
                        children.remove(v);
                }
                Node root = v.getFirstAdjacentEdge().getOpposite(v);
                tree.deleteNode(v);
                v = root;
                root = v.getFirstAdjacentEdge().getOpposite(v);
                tree.deleteNode(v);
                tree.setRoot(root);
            }
            newOrder[0].remove(0);
            newOrder[1].remove(0);

            firstOrder = new LinkedList<String>();
            firstOrder.addAll(newOrder[0]);

            secondOrder = new LinkedList<String>();
            secondOrder.addAll(newOrder[1]);

            /*int t = 0;

            List<String>[] orderEmbeddeding = new LinkedList[2];
            orderEmbeddeding[0] = new LinkedList<String>();
            orderEmbeddeding[1] = new LinkedList<String>();

            for (PhyloTree tree : trees) {
                if (newOrder[t].size() != tree.countLeaves())
                    System.err.println("\n\nERROR order in tree number " + t);
                final NodeArray<BitSet> taxaBelow = new NodeArray<BitSet>(tree);
                for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                    if (v.getOutDegree() == 0) {
                        //int id = taxon2Id.get(tree.getLabel(v));
                        for (int z = 0; z < newOrder[t].size(); z++)
                            if (newOrder[t].get(z).equalsIgnoreCase(tree.getLabel(v))) {
                                BitSet below = new BitSet();
                                below.set(z);
                                taxaBelow.set(v, below);
                                break;
                            }
                    }
                }
                computeTaxaBelowRec(tree.getRoot(), taxaBelow);
                //if (DEBUG)
                //    System.err.println("tree[" + i + "] before: " + tree.toBracketString());
                rotateTreeByTaxaBelow(tree, taxaBelow);
                //if (DEBUG)
                //    System.err.println("tree[" + i + "] after: " + tree.toBracketString());
                (new LayoutUnoptimized()).apply(tree, null);

                //todo: check these changes
                if (tree.getNumberSpecialEdges() != 0)
                    orderEmbeddeding[t] = GeneralMethods.getLsaOrderRec(tree, tree.getRoot(), orderEmbeddeding[t]);
                else
                    orderEmbeddeding[t] = newOrder[t];

                System.err.print("");
                t++;

            }

            */
            System.err.println("The minimal crossing number found is " + finalScore);
            System.err.println("Order of the taxa in the trees: ");
            System.err.println(newOrder[0]);
            System.err.println(newOrder[1]);


            /*firstOrder.addAll(orderEmbeddeding[0]);
            secondOrder.addAll(orderEmbeddeding[1]);
            */
            return finalScore; //in case we actually compute the layout, we return -1
        } else {  // use fast alignment heuristic or number of trees !=2
            useFastAlignmentHeuristic(trees, circularOrdering, bestOrdering, idRho, id2Taxon, taxon2Id, dummyLeaves);
            return -1; //in case we actually compute the layout, we return -1
        }
    }

    /**
     * fast heuristic that tries to rotate trees so that they match the given ordering
     *
     * @param trees
     * @param circularOrdering
     * @param bestOrdering
     * @param idRho
     * @param id2Taxon
     * @param taxon2Id
     * @param dummyLeaves
     */
    private void useFastAlignmentHeuristic(PhyloTree[] trees, int[] circularOrdering, int[] bestOrdering, int idRho, Map<Integer, String> id2Taxon,
                                           Map<String, Integer> taxon2Id, Vector<Node> dummyLeaves) {

        bestOrdering = getLinearOrderingId(circularOrdering, idRho);
        int s = 0;
        long timeBef = System.currentTimeMillis();

        if (DEBUG_time) {
            long timeNeeded = timeBef = System.currentTimeMillis() - timeBef;
            timeBef = System.currentTimeMillis();
            System.err.println(" breakpoint fastHeuristic:" + timeNeeded);
        }

        for (PhyloTree tree : trees) {
            Node rho = dummyLeaves.get(s);
            Node oldRoot = tree.getRoot();
            Node newRoot = tree.getOpposite(oldRoot, tree.getRoot().getFirstOutEdge());

            Edge e = tree.getRoot().getFirstOutEdge();
            Edge e1 = rho.getFirstInEdge();
            tree.deleteEdge(e);
            tree.deleteEdge(e1);
            tree.setRoot(newRoot);

            tree.deleteNode(rho);
            tree.deleteNode(oldRoot);
            s++;
        }

        for (PhyloTree tree : trees) {
            final NodeArray<BitSet> taxaBelow = new NodeArray<BitSet>(tree);
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                if (v.getOutDegree() == 0) {
                    int id = taxon2Id.get(tree.getLabel(v));
                    for (int z = 1; z <= bestOrdering.length; z++)
                        if (bestOrdering[z] == id) {
                            BitSet below = new BitSet();
                            below.set(z);
                            taxaBelow.put(v, below);
                            break;
                        }
                }
            }
            computeTaxaBelowRec(tree.getRoot(), taxaBelow);
            rotateTreeByTaxaBelow(tree, taxaBelow);
            (new LayoutUnoptimized()).apply(tree, null);
            System.err.print("");
        }
    }


    /**
     * gets the linear ordering starting at id idRho and excluding idRho
     *
     * @param circularOrdering
     * @param idRho
     * @return linear ordering
     */
    private int[] getLinearOrderingId(int[] circularOrdering, int idRho) {
        int start = 0;

        for (int src = 1; src < circularOrdering.length; src++) {
            if (circularOrdering[src] == idRho)
                start = src;
        }

        int[] ordering = new int[circularOrdering.length - 1];
        int tar = 1;
        for (int src = start + 1; src < circularOrdering.length; src++) {
            ordering[tar++] = circularOrdering[src];
        }
        for (int src = 1; src < start; src++) {
            ordering[tar++] = circularOrdering[src];
        }

        return ordering;
    }


    /**
     * gets the linear ordering starting at start plus reversal
     *
     * @param circularOrdering
     * @param start
     * @return linear ordering
     */
    private int[][] getLinearOrderingRev(int[] circularOrdering, int start, int idRho) {
        int[][] ordering = new int[2][];
        int lenghtCircOrdering = circularOrdering.length;
        ordering[0] = new int[lenghtCircOrdering];
        ordering[1] = new int[lenghtCircOrdering];
        int tar = 1;
        //System.err.println("start " + start);
        boolean idRhoEncounter = false;
        int idRhoPos = 0;
        for (int src = start; src < circularOrdering.length; src++) {
            //System.err.println("src " + src + " " + circularOrdering[src]+ " ");
            ordering[0][tar] = circularOrdering[src];
            if (!idRhoEncounter) {
                //System.err.println("tar " + tar  + " src " + src + " " + circularOrdering[src]+ " ");
                ordering[1][tar] = circularOrdering[src];
            } else {
                //System.err.println(lenghtCircOrdering + " " + tar + " " + idRhoPos +" " +(lenghtCircOrdering - tar + idRhoPos ));
                //ordering[1][tar] = circularOrdering[lenghtCircOrdering - tar + idRhoPos ];
                ordering[1][lenghtCircOrdering - tar + idRhoPos] = ordering[0][tar];

            }
            if (circularOrdering[src] == idRho) {
                idRhoEncounter = true;
                idRhoPos = tar;
                //System.err.println("found " + tar)  ;
            }
            tar++;
        }
        for (int src = 1; src < start; src++) {
            ordering[0][tar] = circularOrdering[src];
            if (!idRhoEncounter)
                ordering[1][tar] = circularOrdering[src];
            else {
                //System.err.println(lenghtCircOrdering + " " + tar + " " + idRhoPos +" " +(lenghtCircOrdering - tar + idRhoPos ));
                ordering[1][lenghtCircOrdering - tar + idRhoPos] = ordering[0][tar];
            }

            if (circularOrdering[src] == idRho) {
                idRhoEncounter = true;
                idRhoPos = tar;
                //System.err.println("found " + tar) ;
            }
            tar++;
        }
        return ordering;
    }

    /**
     * recursively extends the taxa below map from leaves to all nodes
     *
     * @param v
     * @param taxaBelow
     */
    public static void computeTaxaBelowRec(Node v, NodeArray<BitSet> taxaBelow) {
        if (v.getOutDegree() > 0 && taxaBelow.get(v) == null) {
            BitSet below = new BitSet();

            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node w = e.getTarget();
                computeTaxaBelowRec(w, taxaBelow);
                below.or(taxaBelow.get(w));
            }
            taxaBelow.put(v, below);
        }
    }

    /**
     * rotates all out edges so as to sort by the taxa-below sets
     *
     * @param tree
     * @param taxaBelow
     */
    public static void rotateTreeByTaxaBelow(PhyloTree tree, final NodeArray<BitSet> taxaBelow) {
        for (Node v0 = tree.getFirstNode(); v0 != null; v0 = v0.getNext()) {
            if (v0.getOutDegree() > 0) {
                final Node sourceNode = v0;

                /*
                System.err.println("Source node: " +sourceNode+" "+tree.getLabel(v0));

                System.err.println("original order:");
                for (Edge e = v0.getFirstOutEdge(); e != null; e = v0.getNextOutEdge(e)) {
                    Node w=e.getOpposite(v0) ;
                    System.err.println(w +" "+tree.getLabel(w)+ " value: " + (Basic.toString(taxaBelow.get(w))));
                }
                */

                SortedSet<Edge> adjacentEdges = new TreeSet<Edge>(new Comparator<Edge>() {
                    public int compare(Edge e, Edge f) {
                        if (e.getSource() == sourceNode && f.getSource() == sourceNode) // two out edges
                        {
                            Node v = e.getTarget();
                            Node w = f.getTarget();

                            // lexicographically smaller is smaller
                            BitSet taxaBelowV = taxaBelow.get(v);
                            BitSet taxaBelowW = taxaBelow.get(w);

                            int i = taxaBelowV.nextSetBit(0);
                            int j = taxaBelowW.nextSetBit(0);
                            while (i != -1 && j != -1) {
                                if (i < j)
                                    return -1;
                                else if (i > j)
                                    return 1;
                                i = taxaBelowV.nextSetBit(i + 1);
                                j = taxaBelowW.nextSetBit(j + 1);
                            }
                            if (i == -1 && j != -1)
                                return -1;
                            else if (i != -1 && j == -1)
                                return 1;

                        } else if (e.getTarget() == sourceNode && f.getSource() == sourceNode)
                            return -1;
                        else if (e.getSource() == sourceNode && f.getTarget() == sourceNode)
                            return 1;
                        // no else here!
                        if (e.getId() < f.getId())
                            return -1;
                        else if (e.getId() > f.getId())
                            return 1;
                        else
                            return 0;
                    }
                });

                for (Edge e = v0.getFirstAdjacentEdge(); e != null; e = v0.getNextAdjacentEdge(e)) {
                    adjacentEdges.add(e);
                }
                List<Edge> list = new LinkedList<Edge>();
                list.addAll(adjacentEdges);
                v0.rearrangeAdjacentEdges(list);
            }
        }
    }

    /**
     * compute a circular ordering using neighbor net
     *
     * @param trees
     * @param taxon2ID
     * @param id2Taxon
     * @return
     */
    //working
    public int[] computerCircularOrderingHardwiredMatrix(PhyloTree[] trees, Map<String, Integer> taxon2ID, Map<Integer, String> id2Taxon) {
        if (taxon2ID.size() > 2) {

            double[][] distMat = null;

            Taxa[] taxaTrees = new Taxa[2];
            if (trees.length == 2) {
                taxaTrees[0] = TanglegramUtils.getTaxaForTanglegram(trees[0]);
                taxaTrees[1] = TanglegramUtils.getTaxaForTanglegram(trees[1]);
            }

            Taxa[] taxaNotInTrees = new Taxa[2];
            PhyloTree[] newTrees = new PhyloTree[2];


            if (trees.length == 2 && (taxaTrees[0].size() != taxon2ID.size() || taxaTrees[1].size() != taxon2ID.size())) {

                for (int s = 0; s < 2; s++) {

                    newTrees[s] = (PhyloTree) trees[s].clone();
                    taxaNotInTrees[java.lang.Math.abs(s - 1)] = new Taxa();


                    for (Iterator<String> it = taxaTrees[s].iterator(); it.hasNext(); ) {
                        String taxon = it.next();
                        boolean contains = (taxaTrees[java.lang.Math.abs(s - 1)]).contains(taxon);
                        if (!contains) {
                            taxaNotInTrees[java.lang.Math.abs(s - 1)].add(taxon);
                        }
                    }
                    //if(DEBUG_Partial)
                    //System.err.println("taxaNotInTrees[" + java.lang.Math.abs(s-1) + "] " + taxaNotInTrees[java.lang.Math.abs(s-1)].toString());
                }

                // we restrict the trees to the common taxa

                for (int s = 0; s < 2; s++) {
                    for (Iterator<String> it = taxaNotInTrees[java.lang.Math.abs(s - 1)].iterator(); it.hasNext(); ) {
                        String taxon = it.next();


                        Node toDelete = null;
                        for (Iterator<Node> itN = newTrees[s].nodes().iterator(); itN.hasNext(); ) {
                            Node node = itN.next();
                            if (node.getOutDegree() == 0 && newTrees[s].getLabel(node) == taxon) {
                                toDelete = node;
                                break;
                            }
                        }
                        newTrees[s].deleteNode(toDelete);
                        //System.err.println("taxon " + taxon + " " + newTrees[s].toBracketString());


                    }
                    boolean weird = true;  //todo : problem with delete!
                    while (weird) {
                        weird = false;
                        for (Iterator<Node> itN = newTrees[s].nodes().iterator(); itN.hasNext(); ) {
                            Node node = itN.next();
                            if (node.getOutDegree() == 0 && newTrees[s].getLabel(node) == null) {
                                newTrees[s].deleteNode(node);
                                weird = true;
                            }
                        }
                    }

                }

                // we extract the clusters from the modified trees

                Set<Set<String>> clustersAll = PhyloTreeUtils.collectAllHardwiredClusters(newTrees[0]);
                clustersAll.addAll(PhyloTreeUtils.collectAllHardwiredClusters(newTrees[1]));

                SplitSystem sys = TanglegramUtils.getSplitSystem(clustersAll, taxon2ID);
                distMat = TanglegramUtils.setMatForDiffSys(distMat, taxon2ID.size(), sys, false);
            } else {

// create a new distance matrix and update it for every split system induced by the given networks
                for (PhyloTree tree : trees) {
                    //System.err.println("tree ");

                    Set<Set<String>> clusters = PhyloTreeUtils.collectAllHardwiredClusters(tree);
                    SplitSystem sys = TanglegramUtils.getSplitSystem(clusters, taxon2ID);

                    distMat = TanglegramUtils.setMatForDiffSys(distMat, taxon2ID.size(), sys, false);
                }
            }

            // get the order using NN

            int ntax = taxon2ID.size();
            try {
                int[] ordering = NeighborNetCycle.getNeighborNetOrdering(null, ntax, distMat);
                if (trees.length == 2) {
                    // we restrict the ordering to the common taxa. If solution zero exist, we will find it

                    if (taxaNotInTrees[0] != null && taxaNotInTrees[1] != null) {
                        int takeAway = taxaNotInTrees[0].size() + taxaNotInTrees[1].size();
                        int[] newOrdering = new int[ordering.length - takeAway];

                        int index = 0;
                        for (int i : ordering) {
                            if (!taxaNotInTrees[0].contains(id2Taxon.get(i)) && !taxaNotInTrees[1].contains(id2Taxon.get(i))) {
                                newOrdering[index] = i;
                                index++;
                            }

                        }

                        return newOrdering;
                    } else
                        return ordering;
                } else
                    return ordering;


            } catch (CanceledException e) {
                Basic.caught(e);
                return null; // can't happen
            }
        } else {
            int[] ordering = new int[taxon2ID.size() + 1];
            for (int i = 1; i <= taxon2ID.size(); i++)
                ordering[i] = i;
            return ordering;
        }
    }

    /**
     * compute a circular ordering using neighbor net
     *
     * @param trees
     * @param taxon2ID
     * @param id2Taxon
     * @return
     */
    public int[] computerCircularOrderingShortestPathMatrix(PhyloTree[] trees, Map<String, Integer> taxon2ID, Map<Integer, String> id2Taxon) {

        if (taxon2ID.size() > 2) {
            int count = 1;

// take taxa of all networks, assign a new ID and store both in a Map (allows use of networks
// with different taxa sets)

            for (PhyloTree tree : trees) {
                Taxa tax = TanglegramUtils.getTaxaForTanglegram(tree);
                for (Iterator<String> taxIt = tax.iterator(); taxIt.hasNext(); ) {
                    String taxName = taxIt.next();
                    if (!taxon2ID.keySet().contains(taxName)) {
                        taxon2ID.put(taxName, count);
                        id2Taxon.put(count, taxName);
                        count++;
                        if (DEBUG)
                            System.err.print(taxName + " = " + taxon2ID.get(taxName) + " , ");
                    }
                }
            }


            int max_num_nodes = 3 * taxon2ID.size() - 5; // why???????????????? why not????

            double[][] distMat = new double[max_num_nodes][max_num_nodes]; //initialize to zeros


            for (PhyloTree tree : trees) {
                PhyloTreeUtils.computeNumberNodesInTheShortestPath(tree, taxon2ID, distMat);
            }

            int numBTrees = trees.length;

            for (int ii = 0; ii < distMat.length; ii++) {
                for (int jj = 0; jj < distMat.length; jj++)
                    distMat[ii][jj] /= numBTrees;
            }

            if (DEBUG) {
                for (int ii = 1; ii <= taxon2ID.size(); ii++) {
                    System.err.print(id2Taxon.get(ii) + " ");
                }
                System.err.println();
                for (int ii = 1; ii <= taxon2ID.size(); ii++) {
                    System.err.print(id2Taxon.get(ii) + "\t");
                    for (int jj = 1; jj < taxon2ID.size(); jj++)
                        System.err.print(" " + distMat[ii][jj]);
                    System.err.println();
                }
            }

            // get the order using NN

            int ntax = taxon2ID.size();
            try {
                return NeighborNetCycle.getNeighborNetOrdering(null, ntax, distMat);
            } catch (CanceledException e) {
                Basic.caught(e);
                return null; // can't happen
            }

        } else {
            int[] ordering = new int[taxon2ID.size()];
            int i = 0;
            for (String key : taxon2ID.keySet()) {
                ordering[i++] = taxon2ID.get(key);
            }
            return ordering;
        }
    }

    /**
     * gets the list of taxa for the first network, if computed
     *
     * @return list of taxa or null
     */
    public List<String> getFirstOrder() {
        return firstOrder;
    }

    /**
     * gets the list of taxa for the second network, if computed
     *
     * @return list of taxa or null
     */
    public List<String> getSecondOrder() {
        return secondOrder;
    }

    private static PhyloTree copyNoRet(PhyloTree src, Node v) {
        var tar = new PhyloTree();
        var nodes = src.newNodeSet();
        var queue = new LinkedList<Node>();
        queue.add(v);
        nodes.add(v);
        while (queue.size() > 0) {
            var w = queue.removeFirst();
            if (w.getInDegree() <= 1 || w == v) {
                nodes.add(w);
                queue.addAll(IteratorUtils.asList(w.children()));
            }
        }
        var src2tar = src.extract(nodes, null, tar);
        tar.setRoot(src2tar.get(v));
        for (var s : src2tar.keys()) {
            if (src.getLabel(s) != null)
                tar.setLabel(src2tar.get(s), src.getLabel(s));
        }
        return tar;
    }
}
