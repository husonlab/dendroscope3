/**
 * LayoutOptimizerDist.java 
 * Copyright (C) 2019 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package dendroscope.embed;

import dendroscope.consensus.Taxa;
import dendroscope.tanglegram.TanglegramUtils;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;
import jloda.util.Pair;
import jloda.util.ProgressListener;

import java.util.*;

/**
 * compute an optimal embedding using a simple distance heuristic
 * Daniel Huson, 7.2010
 * todo: the scoring function must be changed to reflect the number of crossing produced by reticulate edges
 */
public class LayoutOptimizerDist implements ILayoutOptimizer {
    public final boolean DEBUG = false;

    /**
     * apply the embedding algorithm to a single tree
     *
     * @param tree
     * @param progressListener
     */
    public void apply(PhyloTree tree, ProgressListener progressListener) {
        if (tree.getRoot() == null || tree.getSpecialEdges().size() == 0) {
            tree.getNode2GuideTreeChildren().clear();
            return;
        }
        System.err.println("Computing optimal embedding using distance-based algorithm");
        apply(new PhyloTree[]{tree});
    }

    /**
     * apply the embedding algorithm to a whole set of trees
     *
     * @param trees
     */
    public void apply(PhyloTree[] trees) {
        System.err.println("Computing optimal embedding using circular-ordering algorithm");
        final Map<String, Integer> taxon2Id = new HashMap<String, Integer>();
        final Map<Integer, String> id2Taxon = new HashMap<Integer, String>();

        {
            int id = 0;
            for (PhyloTree tree : trees) {
                Taxa tax = TanglegramUtils.getTaxaForTanglegram(tree);
                for (Iterator<String> taxIt = tax.iterator(); taxIt.hasNext(); ) {
                    String taxName = taxIt.next();
                    if (!taxon2Id.keySet().contains(taxName)) {
                        taxon2Id.put(taxName, id);
                        id2Taxon.put(id, taxName);
                        id++;
                        // System.err.print(taxName + " = " + taxon2ID.get(taxName) + " , ");
                    }
                }
            }
        }

        int bestScore = Integer.MAX_VALUE;
        int[] bestOrdering = null;

        for (Integer firstTaxon : id2Taxon.keySet()) {
            //todo: enable next line for order starting with "a"
            // if (!id2Taxon.get(firstTaxon).equals("a")) continue;

            int[] ordering = computeBestOrderingForGivenFirstTaxon(taxon2Id.size(), firstTaxon, trees, taxon2Id, id2Taxon);

            if (DEBUG) {
                System.err.println("----------\nstart=" + id2Taxon.get(firstTaxon));
                System.err.print("Ordering: ");
                for (int i = 1; i < ordering.length; i++) {
                    System.err.print(" " + id2Taxon.get(ordering[i]));
                }
                System.err.println();
            }

            int totalScore = 0;
            for (PhyloTree oTree : trees) {
                final PhyloTree tree = (PhyloTree) oTree.clone();
                final NodeArray<BitSet> taxaBelow = new NodeArray<BitSet>(tree);
                final NodeArray<Integer> leaf2taxonId = new NodeArray<Integer>(tree);

                for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                    if (v.getOutDegree() == 0) {
                        int id = taxon2Id.get(tree.getLabel(v));
                        leaf2taxonId.put(v, id);
                        //    if (DEBUG)
                        //        System.err.println("Ordering: " + Basic.toString(ordering));

                        for (int z = 1; z <= ordering.length; z++) {
                            if (ordering[z] == id) {
                                BitSet below = new BitSet();
                                below.set(z);
                                taxaBelow.put(v, below);
                                //  if (DEBUG)
                                //      System.err.println("set " + tree.getLabel(v) + ": id=" + id + " low=" + z);
                                break;
                            }
                        }
                    }
                }
                computeTaxaBelowRec(tree.getRoot(), taxaBelow);
                rotateTreeByTaxaBelow(tree, taxaBelow);
                if (DEBUG)
                    System.err.println("tree: " + tree.toBracketString());

                totalScore += computeScoreRec(tree.getRoot(), leaf2taxonId, new NodeSet(tree), new int[]{0});
            }
            if (DEBUG)
                System.err.println("Score: " + totalScore);

            // if the new ordering is better, or just as good, but more similar to the original ordering, then we keep it
            if (totalScore < bestScore || (totalScore == bestScore &&
                    computeSimilarityScore(trees[0], ordering, taxon2Id, id2Taxon) < computeSimilarityScore(trees[0], bestOrdering, taxon2Id, id2Taxon))) {
                bestScore = totalScore;
                bestOrdering = ordering;
            }
        }
        if (DEBUG) {
            System.err.println("Best score: " + bestScore);
            System.err.print("Best ordering: ");
            for (int i = 1; i < bestOrdering.length; i++) {
                System.err.print(" " + id2Taxon.get(bestOrdering[i]));
            }
            System.err.println();
        }

        for (int i = 0; i < trees.length; i++) {
            PhyloTree tree = trees[i];
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
            if (DEBUG)
                System.err.println("tree[" + i + "] before: " + tree.toBracketString());
            rotateTreeByTaxaBelow(tree, taxaBelow);
            if (DEBUG)
                System.err.println("tree[" + i + "] after: " + tree.toBracketString());
        }

        (new LayoutUnoptimized()).apply(trees[0], null);
    }

    /**
     * computes a score that measures how different the current layout of the tree is from the given ordering
     *
     * @param tree
     * @param ordering
     * @param taxon2Id
     */
    private int computeSimilarityScore(PhyloTree tree, int[] ordering, Map<String, Integer> taxon2Id,
                                       Map<Integer, String> id2taxon) {
        int[] rank = new int[ordering.length];
        for (int i = 0; i < ordering.length; i++)
            rank[ordering[i]] = i;

        int value = computeSimilarityScoreRec(tree.getRoot(), tree, new NodeSet(tree), rank, taxon2Id, new int[]{0});
        System.err.println("Sim score for " + id2taxon.get(ordering[1]) + ": " + value);
        return value;
    }

    /**
     * recursively computes the similarity score
     *
     * @param v
     * @param tree
     * @param seen
     * @param rank
     * @param taxon2Id
     * @param leafCount
     * @return score
     */
    private int computeSimilarityScoreRec(Node v, PhyloTree tree, NodeSet seen, int[] rank, Map<String, Integer> taxon2Id, int[] leafCount) {
        int score = 0;
        if (v.getOutDegree() == 0) {
            int id = taxon2Id.get(tree.getLabel(v));
            score = Math.abs(leafCount[0] - rank[id]);
            leafCount[0]++;
        } else {
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node w = e.getTarget();
                if (!seen.contains(w)) {
                    seen.add(w);
                    score += computeSimilarityScoreRec(w, tree, seen, rank, taxon2Id, leafCount);
                }
            }
        }
        return score;
    }


    /**
     * given the first taxon, computes an ordering for all the rest
     *
     * @param ntax
     * @param firstTaxon
     * @param trees
     * @param taxon2Id
     * @param id2Taxon
     * @return ordering of taxa
     */
    private int[] computeBestOrderingForGivenFirstTaxon(int ntax, Integer firstTaxon, PhyloTree[] trees, Map<String, Integer> taxon2Id, Map<Integer, String> id2Taxon) {
        Pair<Integer, Integer>[] taxon2distFirstTotal = new Pair[ntax];  // distance from first to common parent, distance from parent to other
        for (int i = 0; i < taxon2distFirstTotal.length; i++) {
            taxon2distFirstTotal[i] = new Pair<Integer, Integer>();
            taxon2distFirstTotal[i].setFirst(0);
            taxon2distFirstTotal[i].setSecond(0);
        }


        for (PhyloTree tree : trees) {
            Node first = findNode(tree, id2Taxon.get(firstTaxon));
            NodeArray<Pair<Integer, Integer>> distFromFirst = new NodeArray<Pair<Integer, Integer>>(tree);
            computeDistFirst2AncestorRec(tree.getRoot(), first, distFromFirst);
            computeDistBestAncestor2OtherRec(tree, tree.getRoot(), distFromFirst);

            Pair<Integer, Integer>[] taxon2distFirstTree = new Pair[ntax];
            for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
                if (v.getOutDegree() == 0) {
                    Integer id = taxon2Id.get(tree.getLabel(v));
                    taxon2distFirstTree[id] = distFromFirst.get(v);
                    // System.err.println(id2Taxon.get(id)+": "+id+": "+distFromFirst.get(v));
                }
            }

            for (int i = 0; i < taxon2distFirstTotal.length; i++) {
                taxon2distFirstTotal[i].setFirst(taxon2distFirstTotal[i].getFirst() + taxon2distFirstTree[i].getFirst());
                taxon2distFirstTotal[i].setSecond(taxon2distFirstTotal[i].getSecond() + taxon2distFirstTree[i].getSecond());
            }
        }

        if (DEBUG)
            System.err.println("Unsorted:");
        SortedSet<Pair<Pair<Integer, Integer>, Integer>> sorted = new TreeSet<Pair<Pair<Integer, Integer>, Integer>>((new Pair<Pair<Integer, Integer>, Integer>()));
        for (int i = 0; i < taxon2distFirstTotal.length; i++) {
            Pair<Pair<Integer, Integer>, Integer> distFromFirstForTaxon = new Pair<Pair<Integer, Integer>, Integer>();
            distFromFirstForTaxon.setFirst(taxon2distFirstTotal[i]);
            distFromFirstForTaxon.setSecond(i);
            sorted.add(distFromFirstForTaxon);
            if (DEBUG)
                System.err.println(" " + id2Taxon.get(distFromFirstForTaxon.getSecond()) + ": " + distFromFirstForTaxon);
        }

        if (DEBUG)
            System.err.println("Sorted:");
        int[] ordering = new int[ntax + 1];
        int place = 1;
        for (Pair<Pair<Integer, Integer>, Integer> distFromFirstForTaxon : sorted) {
            ordering[place++] = distFromFirstForTaxon.getSecond();
            if (DEBUG)
                System.err.println(" " + id2Taxon.get(distFromFirstForTaxon.getSecond()) + ": " + distFromFirstForTaxon);
        }
        return ordering;
    }

    /**
     * in a post-order traversal, computes the distance from the first node to all its ancestors
     *
     * @param v
     * @param first
     * @param distFromFirst
     * @return true, if first node is below v
     */
    private boolean computeDistFirst2AncestorRec(Node v, Node first, NodeArray<Pair<Integer, Integer>> distFromFirst) {
        boolean isBelow = false;

        if (v == first) {
            isBelow = true;
            Pair<Integer, Integer> pairV = new Pair<Integer, Integer>();
            pairV.setFirst(0);
            pairV.setSecond(0);
            distFromFirst.put(v, pairV);
        } else {
            Pair<Integer, Integer> pairV = new Pair<Integer, Integer>();
            pairV.setFirst(Integer.MAX_VALUE);
            pairV.setSecond(Integer.MAX_VALUE);
            distFromFirst.put(v, pairV);

            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node w = e.getTarget();
                if (computeDistFirst2AncestorRec(w, first, distFromFirst)) {
                    isBelow = true;
                    Pair<Integer, Integer> pairW = distFromFirst.get(w);
                    if (pairW.getFirst() + 1 < pairV.getFirst())
                        pairV.setFirst(pairW.getFirst() + 1);
                }
            }
        }
        return isBelow;
    }

    /**
     * in a pre-order traversal, compute the distance from the best ancestor of first to the given node v
     *
     * @param v
     * @param distFromFirst
     */
    private void computeDistBestAncestor2OtherRec(PhyloTree tree, Node v, NodeArray<Pair<Integer, Integer>> distFromFirst) {
        Node bestParent = null;

        for (Edge e = v.getFirstInEdge(); e != null; e = v.getNextInEdge(e)) {
            Node w = e.getSource();
            Pair<Integer, Integer> pairW = distFromFirst.get(w);
            if (pairW.getSecond() == Integer.MAX_VALUE)
                return; // parents have not yet all been processed
            if (bestParent == null)
                bestParent = w;
            else {
                Pair<Integer, Integer> pairBest = distFromFirst.get(bestParent);
                if (pairW.getFirst() < pairBest.getFirst()
                        || (pairW.getFirst() == pairBest.getFirst() && pairW.getSecond() > pairBest.getSecond()))
                    bestParent = w;
            }
        }

        Pair<Integer, Integer> pairV = distFromFirst.get(v);
        if (bestParent == null) {
            if (pairV.getFirst() != Integer.MAX_VALUE)
                pairV.setSecond(0);
        } else {
            Pair<Integer, Integer> pairBest = distFromFirst.get(bestParent);
            if (pairBest.getFirst() < pairV.getFirst()) {
                pairV.setFirst(pairBest.getFirst());
                pairV.setSecond(pairBest.getSecond() + 1);
            } else if (pairV.getFirst() != Integer.MAX_VALUE)
                pairV.setSecond(0);
        }

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            Node w = e.getTarget();
            computeDistBestAncestor2OtherRec(tree, w, distFromFirst);
        }
        // todo: next lines labels internal nodes by the pairs:
        // if (v.getOutDegree() > 0) tree.setLabel(v, distFromFirst.get(v).toString());
    }

    /**
     * finds the node for the named taxon
     *
     * @param tree
     * @param name
     * @return node or null
     */
    private Node findNode(PhyloTree tree, String name) {
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            if (v.getOutDegree() == 0 && tree.getLabel(v).equals(name))
                return v;
        }
        return null;
    }

    /**
     * computes the score of the given embedding
     *
     * @param v
     * @param leaf2taxonId
     * @param leafPos
     * @return score for subtree rooted at v
     */
    private int computeScoreRec(Node v, NodeArray<Integer> leaf2taxonId, NodeSet visited, int[] leafPos) {
        visited.add(v);
        if (v.getOutDegree() == 0) {
            int value = Math.abs((++leafPos[0]) - leaf2taxonId.get(v));
            /* if(DEBUG) {
                PhyloTree tree=(PhyloTree)v.getOwner();                
            System.err.println("-------taxon="+tree.getLabel(v));
            System.err.println("leafpos="+leafPos[0]);
            System.err.println("taxabelow="+ Basic.toString(taxaBelow.get(v)));
            System.err.println("Score: "+value);
            }
            */
            return value;
        } else {
            int score = 0;
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node w = e.getTarget();
                if (!visited.contains(w))
                    score += computeScoreRec(w, leaf2taxonId, visited, leafPos);
            }
            return score;
        }
    }


    /**
     * recursively extends the taxa below map from leaves to all nodes
     *
     * @param v
     * @param taxaBelow
     */
    private void computeTaxaBelowRec(Node v, NodeArray<BitSet> taxaBelow) {
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
    private void rotateTreeByTaxaBelow(PhyloTree tree, final NodeArray<BitSet> taxaBelow) {
        for (Node v0 = tree.getFirstNode(); v0 != null; v0 = v0.getNext()) {
            if (v0.getOutDegree() > 0) {
                final Node sourceNode = v0;

                /*
                if(tree.getLabel(v0)==null)
                        tree.setLabel(v0,Basic.toString(taxaBelow.get(v0)));
                else
                    tree.setLabel(v0,tree.getLabel(v0)+"/"+Basic.toString(taxaBelow.get(v0)));
                */

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

                /*
                System.err.println("New order:");
                for (Edge e = v0.getFirstOutEdge(); e != null; e = v0.getNextOutEdge(e)) {
                    Node w=e.getOpposite(v0) ;
                    System.err.println(w +" "+tree.getLabel(w)+ " value: " + (Basic.toString(taxaBelow.get(w))));
                }
                */
            }
        }
    }
}
