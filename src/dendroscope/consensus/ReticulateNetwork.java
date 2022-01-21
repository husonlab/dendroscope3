/*
 * ReticulateNetwork.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.consensus;

import jloda.graph.Edge;
import jloda.swing.util.ProgressDialog;
import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.progress.ProgressListener;

import java.util.*;

/**
 * compute a reticulate network from clusters
 * Daniel Huson, 9.2007
 */
public class ReticulateNetwork {
    private Stack bestSolution;
    private ProgressListener progressListener;

    /**
     * applies the algorithm. Given a set of clusters, computes a minimal set of reduced clusters such that
     * the cluster diagram for the reduced clusters equals a minimal reticulate network for the input clusters
     *
     * @param clusters
     * @return reduced clusters
     */
    public Cluster[] apply(Cluster[] clusters, List<Edge> additionalEdges) {
        int totalChoices = 0;

        this.progressListener = new ProgressDialog("Computing reduced network", "Initializing", null);
        progressListener.setMaximum(-1);

        boolean[][] incompatible = new boolean[clusters.length][clusters.length];
        for (int i = 0; i < clusters.length; i++) {
            for (int j = i + 1; j < clusters.length; j++)
                incompatible[i][j] = incompatible[j][i] = Cluster.incompatible(clusters[i], clusters[j]);
        }
        BitSet[] components = computeComponents(incompatible);
        System.err.println("Components: " + components.length);
        for (BitSet component1 : components) System.err.println(component1);

        Set<Cluster> result = new HashSet<>();
        int predictedNumberReticulations = 0;

        for (int i = 0; i < components.length; i++) {
            BitSet comp = components[i];
            if (comp.cardinality() == 1)   // trivial component
            {
                int c = comp.nextSetBit(0);
                result.add(clusters[c]);
            } else {
                Cluster[] component = new Cluster[comp.cardinality()];
                int pos = 0;
                for (int c = comp.nextSetBit(0); c >= 0; c = comp.nextSetBit(c + 1)) {
                    component[pos++] = clusters[c];
                }
                Map mapBack = new HashMap();

                compactClusters(component, mapBack);
                component = addAllTrivial(component);

                int oldSize = component.length;
                List<Edge> newAdditionalEdges = new LinkedList<>();
                List newChoices = new LinkedList();
                component = apply2component(component, newAdditionalEdges, newChoices);
                System.err.println("Reduced component number " + i + ": " + oldSize + " -> " + component.length);
                System.err.println("Choices component: " + newChoices.size());
                totalChoices += newChoices.size();
                predictedNumberReticulations += getPredictedNumberReticulations(newChoices);

                component = removeAllTrivial(component);
                uncompactClusters(component, mapBack);
                uncompactPairs(newAdditionalEdges, mapBack);

                System.err.println("Original additional edges:");
                for (Edge newAdditionalEdge : newAdditionalEdges) {
                    System.err.println(newAdditionalEdge);

                }
                additionalEdges.addAll(newAdditionalEdges);

                Collections.addAll(result, component);
            }

        }
        if (progressListener != null)
            progressListener.close();

        Cluster[] resultClusters = result.toArray(new Cluster[result.size()]);
        computeWeights(clusters, resultClusters);

        System.err.println("Number of choices: " + totalChoices);
        System.err.println("Predicted number reticulations: " + predictedNumberReticulations);

        return resultClusters;
    }

    /**
     * for a given list of choices, determine the predicted number of reticulations needed in the super-soft
     * wired network
     *
     * @param choices
     * @return predicted number of reticulations
     */
    private int getPredictedNumberReticulations(List choices) {
        BitSet taxa = new BitSet();
        for (Object choice : choices) {
            taxa.or((BitSet) choice);
        }
        return taxa.cardinality();
    }

    /**
     * applies the algorithm. Given a set of clusters, computes a minimal set of reduced clusters such that
     * the cluster diagram for the reduced clusters equals a minimal reticulate network for the input clusters
     *
     * @param inputClusters
     * @return reduced clusters
     */
    private Cluster[] apply2component(Cluster[] inputClusters, List AdditionalEdges, List choices) {
        System.err.println("Input clusters:");
        printClusters(inputClusters);

        List triples = new LinkedList();

        for (int i = 0; i < inputClusters.length; i++) {
            for (int j = i + 1; j < inputClusters.length; j++) {
                Triple triple = computeTriple(inputClusters[i], inputClusters[j]);
                if (triple != null) {
                    triples.add(triple);
                }
            }
        }

        if (triples.size() > 0) {
            System.err.println("Triples:");
            for (Object triple1 : triples) {
                Triple triple = (Triple) triple1;
                System.err.println(triple);
            }

            bestSolution = null;
            Set allChoices = computeAllChoices(triples);
            System.err.println("Choices:");
            for (Object allChoice : allChoices) {
                System.err.println(allChoice);
            }

            if (progressListener != null)
                progressListener.setTasks("Processing component", "Branch-and-bound");
            branchAndBoundRec(triples, allChoices, new Stack());

            if (bestSolution == null) {
                if (progressListener != null && progressListener.isUserCancelled()) {
                    System.err.println("User canceled before result found");
                    progressListener.setUserCancelled(false);
                } else
                    System.err.println("Branch-and-bound search incomplete, nothing to do");
                return inputClusters;
            }

            System.err.println("Optimal choices: " + bestSolution.size());
            for (Object aBestSolution : bestSolution) {
                System.err.println(aBestSolution);
            }

            Cluster[] result = reduceClusters(inputClusters, bestSolution);

            System.err.println("Reduced clusters:");
            printClusters(result);

            pushUpChoices(result, bestSolution);

            System.err.println("Pushed-up clusters");
            printClusters(result);

            AdditionalEdges.addAll(computeAdditionalEdges(inputClusters, result, bestSolution));

            System.err.println("Additonal edges:");
            for (Object AdditionalEdge : AdditionalEdges) {
                System.err.println(AdditionalEdge);
            }

            choices.addAll(bestSolution);

            return result;
        } else // no incompatibities, do nothing
        {
            System.err.println("All clusters compatible, nothing to do");
            return inputClusters;
        }
    }

    /**
     * compute all additional edges. These are pairs consisting of the root and any optional taxon cluster that is
     * contained only in one chain of nested taxa. (This is an on/off type choice that needs to be attached
     * to the root). Or it can consist of the root and a reticulation node that needs to be made "turn-offable"
     *
     * @param reducedClusters
     * @param choices
     * @return additional edges. Each pair consists of a source cluster and a target cluster
     */
    private List computeAdditionalEdges(Cluster[] origClusters, Cluster[] reducedClusters, Collection choices) {
        List result = new LinkedList();

        int countSingleStuck = 0;
        int countMultipleStuck = 0;

        Cluster allOptional = new Cluster();
        for (Object choice2 : choices) {
            BitSet choice = (BitSet) choice2;
            allOptional.or(choice);
        }

        Cluster allTaxa = new Cluster();

        boolean[][] contains = new boolean[reducedClusters.length][reducedClusters.length];
        boolean[][] compatible = new boolean[reducedClusters.length][reducedClusters.length];
        for (int i = 0; i < reducedClusters.length; i++) {
            allTaxa.or(reducedClusters[i]);
            for (int j = 0; j < reducedClusters.length; j++) {
                if (i != j) {
                    if (Cluster.contains(reducedClusters[i], reducedClusters[j])) {
                        contains[i][j] = true; // cluster i contains j
                    }
                    compatible[i][j] = compatible[j][i] = !Cluster.incompatible(reducedClusters[i], reducedClusters[j]);
                }
            }
        }
        boolean[][] containsDirect = new boolean[reducedClusters.length][reducedClusters.length];
        for (int i = 0; i < reducedClusters.length; i++) {
            for (int j = 0; j < reducedClusters.length; j++) {
                if (i != j && contains[i][j]) {
                    boolean ok = true;
                    for (int k = 0; ok && k < reducedClusters.length; k++) {
                        if (k != i && k != j) {
                            if (contains[i][k] && contains[k][j])
                                ok = false;
                        }
                    }
                    containsDirect[i][j] = ok; // does i contain j directly (without any k such that i contains k contains j)
                }
            }
        }


        for (Object choice1 : choices) {
            BitSet choice = (BitSet) choice1;
            int c = -1;
            for (int i = 0; i < reducedClusters.length; i++)  // determine cluster that equals choice
            {
                if (choice.equals(reducedClusters[i])) {
                    c = i;
                    break;
                }
            }

            if (c != -1) // cluster exists that equals choice
            {
                // first determine whether choice c is "single stuck"
                //   c is "single stuck" if the set of all clusters containing c is compatible
                boolean singleStuck = true;

                for (int i = 0; singleStuck && i < reducedClusters.length; i++) {
                    if (i != c && contains[i][c]) {
                        for (int j = i + 1; singleStuck && j < reducedClusters.length; j++) {
                            if (j != c && contains[j][c]) {
                                if (!compatible[i][j])
                                    singleStuck = false;
                            }
                        }
                    }
                }
                // also need to check that we do indeed need to turn the taxon off
                if (singleStuck) {
                    boolean needToTurnOff = false;
                    for (int io = 0; !needToTurnOff && io < origClusters.length; io++) {
                        Cluster origCluster = origClusters[io];
                        if (!Cluster.contains(origCluster, choice)) {
                            int r = getReduced(reducedClusters, choices, origCluster);
                            if (contains[r][c])
                                needToTurnOff = true;
                        }
                    }
                    if (!needToTurnOff)
                        singleStuck = false;
                }

                if (singleStuck) {
                    result.add(new Pair(allTaxa, choice));
                    countSingleStuck++;
                } else // check whether the choice is multi-stuck
                // first determine the coalescent cluster oc(c) for the choice c
                // then see whether there is any original cluster H that does not contain the choice c but is mapped
                // to a reduced cluster R that contains oc(c)
                {
                    int co = getCoalescent(contains, containsDirect, compatible, reducedClusters, c);
                    if (co != -1) {
                        BitSet coalescent = reducedClusters[co];

                        for (Cluster origCluster : origClusters) {
                            int r = getReduced(reducedClusters, choices, origCluster);
                            if (r != -1) {
                                if (!Cluster.contains(origCluster, choice) && Cluster.contains(reducedClusters[r], coalescent)) {
                                    // multi-stuck
                                    result.add(new Pair(allTaxa, choice));
                                    countMultipleStuck++;
                                }
                            } else
                                System.err.println("WARNING: reducedCluster=null");
                        }
                    }
                }
            }
        }
        System.err.println("Added single-stuck edges: " + countSingleStuck);
        System.err.println("Added multiple-stuck edges: " + countMultipleStuck);
        return result;
    }

    /**
     * given an original cluster, determines which reduced cluster it maps to. This is uniquely defined.
     * todo: instead of computing this, we should remember it when computing the reduced clusters
     *
     * @param reducedClusters
     * @param choices
     * @param origCluster
     * @return reduced cluster that orig cluster maps to
     */
    private int getReduced(Cluster[] reducedClusters, Collection choices, Cluster origCluster) {
        for (int i = 0; i < reducedClusters.length; i++) {
            if (Cluster.contains(reducedClusters[i], origCluster)) {
                if (Cluster.equals(reducedClusters[i], origCluster))
                    return i;
                BitSet set = new BitSet();
                set.or(origCluster);
                for (Object choice1 : choices) {
                    BitSet choice = (BitSet) choice1;
                    if (Cluster.contains(reducedClusters[i], choice)) {
                        set.or(choice);
                        if (Cluster.equals(set, reducedClusters[i]))
                            return i;
                    }
                }
            }
        }
        throw new RuntimeException("can't find reduced cluster for: " + origCluster);
        //return -1;
    }

    /**
     * gets the coalescent cluster for an optional cluster. This  this the largest cluster oc above c
     * that is directly above two different clusters p and q, both containing c, that are incompatible.
     *
     * @param contains
     * @param reducedClusters
     * @param c
     * @return
     */
    private int getCoalescent(boolean[][] contains, boolean[][] containsDirect, boolean[][] compatible,
                              Cluster[] reducedClusters, int c) {
        int co = -1;
        for (int i = 0; i < reducedClusters.length; i++) {
            if (i != c && contains[c][i]) {
                for (int p = 0; p < reducedClusters.length; p++) {
                    if (p == c || containsDirect[i][p]) {
                        for (int q = p + 1; q < reducedClusters.length; q++) {
                            if ((q == c || containsDirect[i][q]) && !compatible[p][q]
                                    && (co == -1 || Cluster.contains(reducedClusters[i], reducedClusters[co])))
                                co = i;
                        }
                    }
                }
            }
        }
        return co;
    }

    /**
     * determines incompatibity components
     *
     * @param incompatible
     * @return all incompatibity components
     */
    private BitSet[] computeComponents(boolean[][] incompatible) {
        int[] componentNumber = new int[incompatible.length];
        for (int i = 0; i < componentNumber.length; i++)
            componentNumber[i] = -1;

        int number = 0;
        for (int i = 0; i < incompatible.length; i++) {
            if (componentNumber[i] == -1)
                computeComponentsRec(incompatible, i, number++, componentNumber);
        }

        System.err.println("Components: " + number);

        BitSet[] components = new BitSet[number];
        for (int i = 0; i < components.length; i++)
            components[i] = new BitSet();

        for (int c = 0; c < incompatible.length; c++) {
            components[componentNumber[c]].set(c);
        }
        return components;
    }

    /**
     * recursively does the work
     *
     * @param incompatible
     * @param i
     * @param number
     * @param componentNumber
     */
    private void computeComponentsRec(boolean[][] incompatible, int i, int number, int[] componentNumber) {
        componentNumber[i] = number;
        for (int j = 0; j < incompatible.length; j++) {
            if (i != j && incompatible[i][j] && componentNumber[j] == -1) {
                computeComponentsRec(incompatible, j, number, componentNumber);
            }
        }
    }


    /**
     * reduces the input clusters using a given choice of optional taxa or taxon groups
     *
     * @param clusters
     * @param choices
     * @return reduced clusters
     */
    private Cluster[] reduceClusters(Cluster[] clusters, Collection choices) {
        Map fixed2optional = new HashMap(); // maps fixed cluster parts to their optional ones

        Set result = new HashSet();

        for (Cluster cluster : clusters) {
            Cluster fixed = new Cluster();
            Cluster optional = new Cluster();

            partition(cluster, choices, fixed, optional);

            if (fixed.cardinality() > 0) {
                BitSet op = (BitSet) fixed2optional.get(fixed);
                if (op == null) {
                    fixed2optional.put(fixed, optional);
                    System.err.println("Mapped: " + cluster + " fixed: " + fixed + " optional: " + optional + " total optional: " + fixed2optional.get(fixed));
                } else
                    op.or(optional);
            }
            // todo: what to do with clusters that equal optional sets?
        }

        for (Object o : fixed2optional.keySet()) {
            BitSet fixed = (BitSet) o;
            BitSet optional = (BitSet) fixed2optional.get(fixed);
            BitSet cluster = Cluster.union(fixed, optional);
            result.add(new Cluster(cluster, 1));
        }
        // add all trivial:
        for (Cluster cluster : clusters)
            if (cluster.cardinality() == 1 && !result.contains(cluster))
                result.add(cluster);

        return (Cluster[]) result.toArray(new Cluster[result.size()]);
    }

    /**
     * if a cluster lies below a cluster consisting of the cluster plus an optional set of taxa,
     * pushes the optional set into all containing clusters. This removes a superfluous reticulation node from the network
     *
     * @param clusters
     * @param choices
     */
    private void pushUpChoices(Cluster[] clusters, Collection choices) {
        BitSet[] containedIn = new BitSet[clusters.length];
        BitSet[] contains = new BitSet[clusters.length];
        for (int i = 0; i < clusters.length; i++) {
            containedIn[i] = new BitSet(); // all clusters that cluster i is contained in
            contains[i] = new BitSet();     // all clusters that cluster i contains
        }

        for (int i = 0; i < clusters.length; i++) {
            for (int j = 0; j < clusters.length; j++)
                if (i != j) {
                    if (Cluster.contains(clusters[j], clusters[i])) {
                        containedIn[i].set(j);
                        contains[j].set(i);
                    }
                }
        }

        for (int i = 0; i < clusters.length; i++) {
            BitSet containers = minimalContainingClusters(i, contains, containedIn);
            if (containers.cardinality() > 0) {
                for (int c = containers.nextSetBit(0); c != -1; c = containers.nextSetBit(c + 1)) {
                    BitSet optionalTaxa = determineSeparatingOptionalTaxa(clusters[i], clusters[c], choices);
                    if (optionalTaxa != null) {
                        pushUp(clusters, containedIn[i], optionalTaxa);
                    }
                }
            }
        }
    }

    /**
     * determines a set of optional taxa that separates a cluster from a containing cluster
     *
     * @param cluster
     * @param largerCluster
     * @param choices
     * @return separating cluster or null
     */
    private BitSet determineSeparatingOptionalTaxa(Cluster cluster, Cluster largerCluster, Collection choices) {
        BitSet result = new BitSet();
        BitSet covered = new BitSet();
        covered.or(cluster);
        for (Object choice1 : choices) {
            BitSet choice = (BitSet) choice1;
            if (Cluster.contains(largerCluster, choice) && Cluster.intersection(cluster, choice).cardinality() == 0) {
                covered.or(choice);
                result.or(choice);
            }
            if (covered.equals(largerCluster))
                return result;
        }
        return null;
    }

    /**
     * determine all minimal clusters that contain cluster i
     *
     * @param i
     * @param contains
     * @param containedIn
     * @return all minimal clusters that contain i
     */
    private BitSet minimalContainingClusters(int i, BitSet[] contains, BitSet[] containedIn) {
        BitSet result = new BitSet();

        for (int c = containedIn[i].nextSetBit(0); c != -1; c = containedIn[i].nextSetBit(c + 1)) // for each clusters c that i is contained in
        {
            // check that c does not contain any other cluster that contains i:
            boolean ok = true;
            for (int d = containedIn[i].nextSetBit(0); ok && d != -1; d = containedIn[i].nextSetBit(d + 1)) // for each clusters d that contain i
            {
                if (d != c && contains[c].get(d))
                    ok = false;
            }
            if (ok)
                result.set(c);
        }
        return result;
    }

    /**
     * push up optional taxa into all containing clusters
     *
     * @param clusters
     * @param containers
     * @param optionalTaxa
     */
    private void pushUp(Cluster[] clusters, BitSet containers, BitSet optionalTaxa) {
        for (int c = containers.nextSetBit(0); c != -1; c = containers.nextSetBit(c + 1)) {
            {
                clusters[c].or(optionalTaxa);
            }
        }
    }

    /**
     * given a cluster and a set of choices of optional taxa or taxon-groups, partitions the cluster
     * into its mandatory and optional parts
     *
     * @param cluster
     * @param choices
     * @param fixed
     * @param optional
     */
    private void partition(Cluster cluster, Collection choices, BitSet fixed, BitSet optional) {
        for (Object choice1 : choices) {
            BitSet choice = (BitSet) choice1;
            if (Cluster.contains(cluster, choice))
                optional.or(choice);
        }
        fixed.or(Cluster.setminus(cluster, optional));
    }

    /**
     * recursively use branch-and-bound to find optimal solution
     *
     * @param triples
     * @param remainingChoices
     * @param currentSolution
     * @return true, if algorithm should be terminated
     */
    private boolean branchAndBoundRec(List triples, Set remainingChoices, Stack currentSolution) {
        BitSet[] choices = orderChoices(triples, remainingChoices);

        for (BitSet choice : choices) {
            List reduced = removeHitTriples(triples, choice);

            if (progressListener != null)
                try {
                    progressListener.setProgress(-1);
                } catch (CanceledException e) {
                    return true;
                }

            if (bestSolution == null || currentSolution.size() + 1 < bestSolution.size()) {
                currentSolution.push(choice);
                if (reduced.size() == 0) // found a solution
                {
                    bestSolution = (Stack) currentSolution.clone();
                    /*
                System.err.println("FOUND:");
                for (Iterator it = currentSolution.iterator(); it.hasNext();)
                    System.err.println(it.next());
                    */
                    if (progressListener != null) {
                        progressListener.setSubtask("Current best solution: " + bestSolution.size());
                        try {
                            progressListener.checkForCancel();
                        } catch (CanceledException e) {
                            return true;
                        }
                    }
                } else {
                    int increment = 1;          // todo: need to use lower bound here!
                    // int gain=triples.size()-reduced.size();
                    // int increment=Math.max(1,(gain>0?reduced.size()/gain:0)); // need at least reduced.size()/gain additional choices
                    if (bestSolution == null || currentSolution.size() + increment < bestSolution.size()) {
                        remainingChoices.remove(choice);
                        if (branchAndBoundRec(reduced, remainingChoices, currentSolution))
                            return true;
                        remainingChoices.add(choice);
                    }
                }
                currentSolution.pop();
            }
        }
        return false;
    }

    /**
     * returns all choices in order of decreasing order of triple hits
     *
     * @param triples
     * @param choices
     * @return remaining choices in decreasing order of triples hit
     */
    private BitSet[] orderChoices(List triples, Set choices) {
        var choice2countChoice = new HashMap<BitSet, Pair<Integer, BitSet>>();

        for (Object triple1 : triples) {
            Triple triple = (Triple) triple1;
            for (Object choice : choices) {
                BitSet c = (BitSet) choice;

				if (triple.isHitBy(c)) {
					var pair = choice2countChoice.get(c);
					if (pair == null)
						choice2countChoice.put(c, new Pair<Integer, BitSet>(1, c));
					else
						pair.setFirst(pair.getFirst() + 1);
				}
			}
		}

		var sorted = new TreeSet<Pair<Integer, BitSet>>((p1, p2) -> {
			if (p1.getFirst() > p2.getFirst())
				return -1;
			else if (p1.getFirst() < p2.getFirst())
				return 1;
			BitSet cluster1 = p1.getSecond();
			BitSet cluster2 = p2.getSecond();
			int t1 = cluster1.nextSetBit(0);
			int t2 = cluster2.nextSetBit(0);
			while (true) {
				if (t1 < t2)
					return -1;
				else if (t1 > t2)
					return 1;
				t1 = cluster1.nextSetBit(t1 + 1);
				t2 = cluster2.nextSetBit(t2 + 1);
				if (t1 == -1 && t2 > -1)
					return -1;
				else if (t1 > -1 && t2 == -1)
					return 1;
				else if (t1 == -1 && t2 == -1)
					return 0;
			}
		});
		sorted.addAll(choice2countChoice.values());

		var result = new BitSet[sorted.size()];
		int i = 0;
		for (var pair : sorted) {
			result[i++] = pair.getSecond();
		}
		return result;
	}


    /**
     * computes the intersection triple, if clusters are incompatible, otherwise null
     *
     * @param cluster1
     * @param cluster2
     * @return intersection triple or null
     */
    public Triple computeTriple(BitSet cluster1, BitSet cluster2) {
        Triple triple = new Triple();

        triple.A = Cluster.setminus(cluster1, cluster2);
        triple.B = Cluster.intersection(cluster1, cluster2);
        triple.C = Cluster.setminus(cluster2, cluster1);
        if (triple.A.cardinality() > 0 && triple.B.cardinality() > 0 && triple.C.cardinality() > 0
                && !triple.A.equals(triple.B) && !triple.A.equals(triple.C) && !triple.B.equals(triple.C))
            return triple;
        else
            return null;
    }

    /**
     * compute the is of all possible choices for "hitters"
     *
     * @param triples
     * @return all choices of hitters
     */
    private Set computeAllChoices(List triples) {
        Set choices = new HashSet();

        BitSet taxa = new BitSet();
        for (Object triple1 : triples) {
            Triple triple = (Triple) triple1;

            taxa.or(triple.A);
            taxa.or(triple.B);
            taxa.or(triple.C);
        }
        for (int i = taxa.nextSetBit(0); i != -1; i = taxa.nextSetBit(i + 1)) {
            Cluster iSet = new Cluster();
            iSet.set(i);
            choices.add(iSet);
        }
        return choices;
    }

    /**
     * remove all triples hit by choice
     *
     * @param triples
     * @param choice
     * @return remaining triples
     */
    private List removeHitTriples(List triples, BitSet choice) {
        List result = new LinkedList();

        for (Object triple1 : triples) {
            Triple triple = (Triple) triple1;
            Triple reduced = triple.reduce(choice);
            if (reduced != null)
                result.add(reduced);
        }
        // todo: check result for multiple copies of the same reduced triple

        return result;
    }

    /**
     * compact clusters by identifying no-distinquishable taxa
     *
     * @param clusters
     * @param mapBack  this is used to uncompact clusters
     */
    private void compactClusters(Cluster[] clusters, Map mapBack) {
        System.err.println("original clusters:");
        printClusters(clusters);

        int maxTax = 0;
        BitSet allTaxa = new BitSet();
        for (Cluster cluster2 : clusters) {
            for (int t = cluster2.nextSetBit(0); t >= 0; t = cluster2.nextSetBit(t + 1)) {
                if (t > maxTax)
                    maxTax = t;
                allTaxa.set(t);
            }
        }
        boolean[][] separated = new boolean[maxTax + 1][maxTax + 1];

        for (Cluster cluster1 : clusters) {
            BitSet complement = Cluster.setminus(allTaxa, cluster1);
            for (int t1 = cluster1.nextSetBit(0); t1 != -1; t1 = cluster1.nextSetBit(t1 + 1)) {
                for (int t2 = complement.nextSetBit(0); t2 != -1; t2 = complement.nextSetBit(t2 + 1))
                    separated[t1][t2] = separated[t2][t1] = true;
            }
        }

        int[] taxon2rep = new int[maxTax + 1];
        // map every taxon to the smallest one that it is not separated from:
        for (int t1 = allTaxa.nextSetBit(0); t1 != -1; t1 = allTaxa.nextSetBit(t1 + 1)) {
            int smallest = t1;
            for (int t2 = allTaxa.nextSetBit(0); t2 != -1; t2 = allTaxa.nextSetBit(t2 + 1))
                if (!separated[t1][t2] && t2 < smallest)
                    smallest = t2;
            taxon2rep[t1] = smallest;
        }

        BitSet redundant = new BitSet();
        for (int t1 = allTaxa.nextSetBit(0); t1 != -1; t1 = allTaxa.nextSetBit(t1 + 1)) {
            if (taxon2rep[t1] != t1)
                redundant.set(t1); // taxa that are redundant
        }

        // setup map back
        for (int t1 = allTaxa.nextSetBit(0); t1 != -1; t1 = allTaxa.nextSetBit(t1 + 1)) {
            Integer n = taxon2rep[t1];
            BitSet preImage = (BitSet) mapBack.get(n);
            if (preImage == null) {
                preImage = new BitSet();
                mapBack.put(n, preImage);
            }
            preImage.set(t1);
        }

        // compact all clusters:
        for (Cluster cluster : clusters) {
            cluster.andNot(redundant);

        }

        System.err.println("compact clusters:");
        printClusters(clusters);
    }

    /**
     * uncompact clusters
     *
     * @param clusters
     * @param mapBack
     */
    private void uncompactClusters(Cluster[] clusters, Map mapBack) {
        for (Cluster cluster : clusters) {
            uncompactCluster(cluster, mapBack);
        }
    }

    /**
     * uncompact pairs of clusters
     *
     * @param pairs
     * @param mapBack
     */
    private void uncompactPairs(List pairs, Map mapBack) {
        for (Object pair1 : pairs) {
            Pair pair = (Pair) pair1;
            uncompactCluster((Cluster) pair.getFirst(), mapBack);
            uncompactCluster((Cluster) pair.getSecond(), mapBack);
        }
    }

    /**
     * uncompact pairs of clusters
     *
     * @param orig
     * @param mapBack
     */
    private void uncompactCluster(BitSet orig, Map mapBack) {
        BitSet set = new BitSet();
        for (int t = orig.nextSetBit(0); t != -1; t = orig.nextSetBit(t + 1)) {
            BitSet origTaxa = (BitSet) mapBack.get(t);
            if (origTaxa != null)
                set.or(origTaxa);
        }
        orig.clear();
        orig.or(set);
    }

    /**
     * need to add all trivial clusters to a component. These will be removed again before uncompacting
     *
     * @param clusters
     * @return clusters including all trivial ones
     */
    private Cluster[] addAllTrivial(Cluster[] clusters) {
        List result = new LinkedList();

        BitSet taxa = new BitSet();
        for (Cluster cluster1 : clusters) {
            result.add(cluster1);
            taxa.or(cluster1);
        }

        for (int t = taxa.nextSetBit(0); t != -1; t = taxa.nextSetBit(t + 1)) {
            Cluster cluster = new Cluster();
            cluster.set(t);
            result.add(cluster);
        }
        return (Cluster[]) result.toArray(new Cluster[result.size()]);
    }

    /**
     * remove all trivial clusters
     *
     * @param clusters
     * @return non-trivial clusters
     */
    private Cluster[] removeAllTrivial(Cluster[] clusters) {
        List result = new LinkedList();
        for (Cluster cluster : clusters) {
            if (cluster.cardinality() > 1)
                result.add(cluster);
        }
        return (Cluster[]) result.toArray(new Cluster[result.size()]);
    }

    /**
     * computes weights for all output clusters from the input clusters
     *
     * @param iClusters
     * @param oClusters
     */
    private void computeWeights(Cluster[] iClusters, Cluster[] oClusters) {
        int[] mapIn2Out = new int[iClusters.length]; // maps each input cluster to its output cluster

        for (int ic = 0; ic < iClusters.length; ic++) {
            Cluster iCluster = iClusters[ic];
            int smallest = Integer.MAX_VALUE;
            for (int oc = 0; oc < oClusters.length; oc++) {
                Cluster oCluster = oClusters[oc];
                if (oCluster.cardinality() < smallest && Cluster.contains(oCluster, iCluster)) {
                    mapIn2Out[ic] = oc;
                    smallest = oCluster.cardinality();
                }
            }
        }
        double[] oWeights = new double[oClusters.length];
        int[] oCounts = new int[oClusters.length];

        for (int ic = 0; ic < iClusters.length; ic++) {
            Cluster iCluster = iClusters[ic];
            int oc = mapIn2Out[ic];
            if (oc < Integer.MAX_VALUE) {
                oWeights[oc] += iCluster.getWeight();
                oCounts[oc]++;
            } else {
                System.err.println("WARNING: unmapped input cluster: " + iCluster);
            }
        }
        for (int oc = 0; oc < oClusters.length; oc++) {
            Cluster oCluster = oClusters[oc];
            if (oCounts[oc] > 0) {
                oCluster.setWeight(oWeights[oc] / oCounts[oc]);
            } else {
                System.err.println("WARNING: zero-weight output cluster: " + oCluster);
                oCluster.setWeight(0);
            }
        }
    }


    /**
     * prints a set of weighted clusters
     *
     * @param clusters
     */
    public void printClusters(Cluster[] clusters) {
        for (Cluster cluster : clusters) {
            //System.err.println(clusters[i] + ": " + clusters[i].getWeight());
            System.err.println(cluster);

        }
    }

    /**
     * a triple
     * todo: make triple comparable
     */
    class Triple {
        BitSet A;
        BitSet B;
        BitSet C;

        /**
         * is triple hit by H?
         *
         * @param H
         * @return true, if H contains one part of the triple
         */
        boolean isHitBy(BitSet H) {
            return H.equals(A) || H.equals(B) || H.equals(C);
        }

        public String toString() {
            return A.toString() + " | " + B.toString() + " | " + C.toString();
        }


        /**
         * removes H from all parts of the triple. If one part becomes empty, returns null
         *
         * @param H
         * @return reduced triple or null
         */
        public Triple reduce(BitSet H) {
            Triple result = new Triple();
            if (Cluster.contains(A, H)) {
                result.A = Cluster.setminus(A, H);
                if (result.A.cardinality() == 0)
                    return null;
            } else
                result.A = (BitSet) A.clone();
            if (Cluster.contains(B, H)) {
                result.B = Cluster.setminus(B, H);
                if (result.B.cardinality() == 0)
                    return null;
            } else
                result.B = (BitSet) B.clone();
            if (Cluster.contains(C, H)) {
                result.C = Cluster.setminus(C, H);
                if (result.C.cardinality() == 0)
                    return null;
            } else
                result.C = (BitSet) C.clone();
            //return this;
            return result; // todo: this is if we allow a part to be covered by multiple choices
        }
    }
}
