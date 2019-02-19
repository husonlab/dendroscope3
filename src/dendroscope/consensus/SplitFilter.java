/**
 * SplitFilter.java 
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
package dendroscope.consensus;

import jloda.phylo.Distortion;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;

import java.io.IOException;
import java.util.BitSet;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * filters splits
 * Daniel Huson, 7.2007
 */
public class SplitFilter {
    /**
     * filter away all non-compatible splits in a greedy fashion
     *
     * @param progressListener
     * @param splits
     * @return compatible splits
     */
    public static SplitSystem applyGreedyCompatibleFilter(ProgressListener progressListener, SplitSystem splits) throws CanceledException {
        progressListener.setSubtask("greedy compatible filter");
        // System.err.print("Greedy Compatible filter: "+splits.size());
        SortedSet<Split> sorted = new TreeSet<>(Split.createWeightComparator());
        sorted.addAll(splits.asList());

        SplitSystem result = new SplitSystem();

        for (Split split : sorted) {
            boolean ok = true;
            for (Iterator it2 = result.iterator(); it2.hasNext(); ) {
                Split split2 = (Split) it2.next();
                if (!split.isCompatible(split2)) {
                    ok = false;
                    break;
                }
            }
            if (ok)
                result.addSplit(split);
        }
        // System.err.println(" -> "+result.size());
        return result;
    }

    /**
     * filter away all partial splits
     *
     * @param progressListener
     * @param taxa
     * @param splits
     * @return all full splits
     */
    public static SplitSystem applyRemovePartialFilter(ProgressListener progressListener, Taxa taxa, SplitSystem splits) throws CanceledException {
        progressListener.setSubtask("remove partial splits");

        SplitSystem result = new SplitSystem();
        for (Iterator it = splits.iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();
            if (split.getTaxa().equals(taxa.getBits()))
                result.addSplit(split);
        }
        return result;
    }

    /**
     * todo: needs testing
     * filter the splits by total score. All splits are removed whose total distortion score exceeds the value of maxDistortion
     *
     * @param progressListener
     * @param taxa
     * @param tree2taxa
     * @param trees
     * @param splits
     * @param maxDistortion
     * @return filtered splits
     */
    public static SplitSystem filterByTotalScore(ProgressListener progressListener, Taxa taxa, BitSet[] tree2taxa, PhyloTree[] trees, SplitSystem splits, int maxDistortion) throws CanceledException {
        progressListener.setTasks("Filter by total distortion", "Processing splits");
        progressListener.setMaximum(splits.size());

        for (PhyloTree tree1 : trees) Utilities.setNode2Taxa(taxa, tree1);

        SplitSystem result = new SplitSystem();

        System.err.println("Filtering splits:");
        for (Iterator it = splits.iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();
            int totalScore = 0;
            BitSet A = split.getA();
            BitSet B = split.getB();
            for (int t = 0; t <= trees.length; t++) {
                BitSet treeTaxa = tree2taxa[t];
                BitSet treeTaxaAndA = (BitSet) (treeTaxa.clone());
                treeTaxaAndA.and(A);
                BitSet treeTaxaAndB = (BitSet) (treeTaxa.clone());
                treeTaxaAndB.and(B);

                if (treeTaxaAndA.cardinality() > 1 && treeTaxaAndB.cardinality() > 1) {
                    try {
                        PhyloTree tree = trees[t];
                        totalScore += Distortion.computeDistortionForSplit(tree, A, B);
                    } catch (IOException ex) {
                        Basic.caught(ex);
                    }
                }
            }
            if (totalScore <= maxDistortion)
                result.addSplit(split);
            progressListener.incrementProgress();
        }
        for (PhyloTree tree : trees) Utilities.clearNode2Taxa(tree);

        return result;
    }

    /**
     * filters splits by distortion, allowing a split to pass only if there exists minNumberTrees for which
     * the distortion of the split is at most maxDistortion
     *
     * @param progressListener
     * @param taxa
     * @param tree2taxa
     * @param trees
     * @param splits
     * @param maxDistortion
     * @param minNumberTrees
     * @param allTrivial
     * @return filtered splits
     * @throws CanceledException
     */
    public static SplitSystem filterByDistortion(ProgressListener progressListener, Taxa taxa, BitSet[] tree2taxa, PhyloTree[] trees, SplitSystem splits, int maxDistortion,
                                                 int minNumberTrees, boolean allTrivial) throws CanceledException {
        progressListener.setTasks("Filter by distortion", "Processing splits");
        progressListener.setMaximum(splits.size());

        for (PhyloTree tree1 : trees) Utilities.setNode2Taxa(taxa, tree1);

        SplitSystem result = new SplitSystem();

        System.err.println("Filtering splits:");
        for (Iterator it = splits.iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();
            BitSet A = split.getA();
            BitSet B = split.getB();
            int count = 0;
            for (int t = 0; t < trees.length; t++) {
                BitSet treeTaxa = tree2taxa[t];
                BitSet treeTaxaAndA = (BitSet) (treeTaxa.clone());
                treeTaxaAndA.and(A);
                BitSet treeTaxaAndB = (BitSet) (treeTaxa.clone());
                treeTaxaAndB.and(B);

                if (treeTaxaAndA.cardinality() > 1 && treeTaxaAndB.cardinality() > 1) {
                    try {
                        PhyloTree tree = trees[t];
                        int score = Distortion.computeDistortionForSplit(tree, A, B);
                        //System.err.print(" " + score);
                        if (score <= maxDistortion)
                            count++;
                        if (count + (trees.length - t) < minNumberTrees)
                            break; // no hope to get above threshold
                    } catch (IOException e) {
                        Basic.caught(e);
                    }
                } else if ((A.cardinality() == 1 || B.cardinality() == 1)
                        && treeTaxaAndB.cardinality() > 0 && treeTaxaAndB.cardinality() > 0) {
                    count++; // is confirmed split
                    //System.err.print(" +");
                } else {
                    //System.err.print(" .");
                }
            }
            //System.err.println(" sum=" + count);
            if ((allTrivial && (A.cardinality() == 1 || B.cardinality() == 1))
                    || count >= minNumberTrees) {
                Split newSplit = (Split) split.clone();
                newSplit.setConfidence((float) count / (float) trees.length);
                result.addSplit(newSplit);
            }
            progressListener.incrementProgress();
        }

        for (PhyloTree tree : trees) Utilities.clearNode2Taxa(tree);

        return result;
    }

    /**
     * Filters splits by the majority rule. Keeps only those splits that appear in more than 50% of the relevant trees.
     * If the input are partial trees, then there is no guarantee that the splits will be compatible
     *
     * @param progressListener
     * @param tree2taxa
     * @param tree2splits
     * @param splits
     * @return majority splits
     */
    public static SplitSystem filterByMajority(ProgressListener progressListener, BitSet[] tree2taxa, SplitSystem[] tree2splits, SplitSystem splits) throws CanceledException {
        progressListener.setTasks("Filter by majority consensus", "Processing splits");
        progressListener.setMaximum(splits.size());

        SplitSystem result = new SplitSystem();

        for (Iterator it = splits.iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();

            int countPossible = 0;
            int countContainedIn = 0;
            for (int t = 0; t < tree2taxa.length; t++) {
                if (split.getA().intersects(tree2taxa[t]) && split.getB().intersects(tree2taxa[t])) {
                    countPossible++;
                    Split induced = split.getInduced(tree2taxa[t]);
                    if (tree2splits[t].contains(induced))
                        countContainedIn++;
                }
            }
            if (2 * countContainedIn > countPossible)
                result.addSplit(split);
            progressListener.incrementProgress();
        }
        return result;
    }

    /**
     * Filters splits by the majority rule. Keeps only those splits that appear in all the relevant trees.
     * If the input are partial trees, then there is no guarantee that the splits will be compatible
     *
     * @param progressListener
     * @param tree2taxa
     * @param tree2splits
     * @param splits
     * @return strict majority splits
     */
    public static SplitSystem filterByStrict(ProgressListener progressListener, BitSet[] tree2taxa, SplitSystem[] tree2splits, SplitSystem splits) throws CanceledException {
        progressListener.setTasks("Filter by strict consensus", "Processing splits");
        progressListener.setMaximum(splits.size());

        SplitSystem result = new SplitSystem();

        for (Iterator it = splits.iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();

            boolean ok = true;
            for (int t = 0; t < tree2taxa.length; t++) {
                if (split.getA().intersects(tree2taxa[t]) && split.getB().intersects(tree2taxa[t])) {
                    Split induced = split.getInduced(tree2taxa[t]);
                    if (!tree2splits[t].contains(induced)) {
                        ok = false;
                        break;
                    }
                }
            }
            if (ok)
                result.addSplit(split);
            progressListener.incrementProgress();
        }
        return result;
    }

    /**
     * filter by loose consensus
     *
     * @param progressListener
     * @param tree2taxa
     * @param tree2splits
     * @param splits
     * @return loose consensus splis
     * @throws CanceledException
     */
    public static SplitSystem filterByLoose(ProgressListener progressListener, BitSet[] tree2taxa, SplitSystem[] tree2splits, SplitSystem splits) throws CanceledException {
        progressListener.setTasks("Filter by loose consensus", "Processing splits");
        progressListener.setMaximum(splits.size());

        SplitSystem result = new SplitSystem();

        for (Iterator it = splits.iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();

            boolean ok = true;
            for (int t = 0; ok && t < tree2taxa.length; t++) {
                if (split.getA().intersects(tree2taxa[t]) && split.getB().intersects(tree2taxa[t])) {
                    Split induced = split.getInduced(tree2taxa[t]);
                    for (Iterator it2 = tree2splits[t].iterator(); it2.hasNext(); ) {
                        if (!induced.isCompatible((Split) it2.next())) {
                            ok = false;
                            break;
                        }
                    }
                }
            }
            if (ok)
                result.addSplit(split);
            progressListener.incrementProgress();
        }
        return result;
    }

    /**
     * Filters splits by the majority rule. Keeps only those splits that appear in more than 50% of the relevant trees.
     * If the input are partial trees, then there is no guarantee that the splits will be compatible
     *
     * @param progressListener
     * @param tree2taxa
     * @param tree2splits
     * @param splits
     * @return majority splits
     */
    public static SplitSystem filterByPrecentThreshold(ProgressListener progressListener, BitSet[] tree2taxa, SplitSystem[] tree2splits, SplitSystem splits, double percentThreshold) throws CanceledException {
        progressListener.setTasks("Filter by percent threshold (" + percentThreshold + " %)", "Processing splits");
        progressListener.setMaximum(splits.size());

        SplitSystem result = new SplitSystem();

        for (Iterator it = splits.iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();

            int countPossible = 0;
            int countContainedIn = 0;
            for (int t = 0; t < tree2taxa.length; t++) {
                if (split.getA().intersects(tree2taxa[t]) && split.getB().intersects(tree2taxa[t])) {
                    countPossible++;
                    Split induced = split.getInduced(tree2taxa[t]);
                    if (tree2splits[t].contains(induced))
                        countContainedIn++;
                }
            }
            if (countPossible > 0 && 100.0 * ((double) countContainedIn / (double) countPossible) > percentThreshold) {
                result.addSplit(split);
                //System.err.println("Adding split (possible="+countPossible+", containedIn: "+countContainedIn+"): <"+split+">");
            }
            progressListener.incrementProgress();
        }
        return result;
    }
}
