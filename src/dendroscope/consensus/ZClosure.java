/*
 * ZClosure.java Copyright (C) 2022 Daniel H. Huson
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

import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.progress.ProgressCmdLine;
import jloda.util.progress.ProgressListener;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;

/**
 * computes the Z-closure of a set of splits
 * Daniel Huson, 7.2007
 */
public class ZClosure {
    final int optionSeed = 666;
    final int optionNumberOfRuns = 10;
    Random rand = null;

    Taxa taxa;

    // edge weight options:
    private String optionEdgeWeights = TREESIZEWEIGHTEDMEAN;
    static final String AVERAGERELATIVE = "AverageRelative";
    static final String MEAN = "Mean";
    static final String TREESIZEWEIGHTEDMEAN = "TreeSizeWeightedMean";
    static final String SUM = "Sum";
    static final String MIN = "Min";
    static final String NONE = "None";

    // filter options:
    private String optionFilter = FILTER_NONE;
    private int optionMinNumberTrees = 1;
    private int optionMaxDistortionScore = 0;
    private double optionPercentThreshold = 0;


    public static final String FILTER_NONE = "None";
    public static final String FILTER_TOTAL_SCORE = "TotalScore";
    public static final String FILTER_DISTORTION = "Distortion";
    public static final String FILTER_MAJORITY = "Majority";
    public static final String FILTER_STRICT = "Strict";
    public static final String FILTER_LOOSE = "Loose";

    public static final String FILTER_PERCENT_THRESHOLD = "PercentThreshold";


    /**
     * applies Z-closure to a collection of trees.
     *
     * @param trees            @return full splits obtained by Z-closure
     */
    public SplitSystem apply(ProgressListener progressListener, PhyloTree[] trees) throws CanceledException {
        this.rand = new Random(this.optionSeed);
        taxa = new Taxa();
        BitSet[] tree2taxa = new BitSet[trees.length];
        SplitSystem[] tree2splits = new SplitSystem[trees.length];
        final SplitSystem inputSplits = new SplitSystem();
        boolean inputSplitsAreAllFull = Utilities.getSplitsFromTrees(progressListener, trees, taxa, tree2taxa, tree2splits, inputSplits);  // also sets tree2taxa and tree2splits

        System.err.println("Input splits from trees: " + inputSplits.size());

        SplitSystem splits = new SplitSystem();
        splits.addAll(inputSplits);

        if (!inputSplitsAreAllFull) {
            final SplitSystem finalSplits = splits;
            progressListener.setSubtask("Computing Z-closure");
            progressListener.setMaximum(optionNumberOfRuns);
            progressListener.setProgress(0);

            // parallel implementation!
            ExecutorService executor = Executors.newCachedThreadPool();
            try {
                progressListener.setCancelable(false);

                int numberOfThreads = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), this.optionNumberOfRuns));
                final SynchronousQueue<Integer> queue = new SynchronousQueue<>();
                final CountDownLatch countDownLatch = new CountDownLatch(this.optionNumberOfRuns);
                final ProgressCmdLine progress = new ProgressCmdLine();

                for (int i = 1; i <= numberOfThreads; i++) {
					executor.execute(() -> {
						while (true) {
							try {
								int i1 = queue.take(); // ignore the value

								List<Split> splitsInRandomOrder = inputSplits.asList();
								Collections.shuffle(splitsInRandomOrder, rand);
								SplitSystem result = null;
								try {
									result = computeClosure(progress, splitsInRandomOrder);
								} catch (CanceledException ignored) {
								}
								synchronized (finalSplits) {
									finalSplits.addAll(result);
								}
							} catch (InterruptedException e) {
								return;
							} finally {
								countDownLatch.countDown();
							}
						}
					});
                }

                for (int i = 1; i <= this.optionNumberOfRuns; i++) {
                    try {
                        queue.put(i);
                        if (i == 1)
                            progressListener.setCancelable(true);
                        progressListener.setProgress(i);
                    } catch (InterruptedException e) {
                        Basic.caught(e);
                    }
                }
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    Basic.caught(e);
                }
            } catch (CanceledException ex) {
                System.err.println("CANCELED");
                progressListener.setUserCancelled(false);
            } finally {
                executor.shutdownNow();
            }
            progressListener.setProgress(-1);

            System.err.println("After z-closure: " + finalSplits.size());
            //System.err.println("Partial splits after z-closure: " + splits);

            splits = SplitFilter.applyRemovePartialFilter(progressListener, taxa, finalSplits);
            System.err.println("After removal of partial splits: " + splits.size());
            //System.err.println("Full splits:\n" + splits.toStringAsBinarySequences(taxa));
        }

        // add all missing trivial splits
        int totalTrivialAdded = 0;
        for (Iterator it = taxa.iterator(); it.hasNext(); ) {
            int index = taxa.indexOf((String) it.next());
            BitSet A = new BitSet();
            A.set(index);
            BitSet B = (BitSet) taxa.getBits().clone();
            B.set(index, false);

            Split split = new Split(A, B);
            if (!splits.contains(split)) {
                splits.addSplit(split);
                totalTrivialAdded++;
            }
        }
        if (totalTrivialAdded > 0)
            System.err.println("Trivial added: " + totalTrivialAdded);

        if (getOptionEdgeWeights().equals(AVERAGERELATIVE) == true) {
            setWeightAverageReleativeLength(progressListener, tree2taxa, tree2splits, splits);
        } else if (!getOptionEdgeWeights().equals(NONE)) {
            setWeightsConfidences(progressListener, tree2taxa, tree2splits, splits);
        }

        // apply filtering, if requested:
        switch (getOptionFilter()) {
            case FILTER_TOTAL_SCORE:
                splits = SplitFilter.filterByTotalScore(progressListener, taxa, tree2taxa, trees, splits, getOptionMaxDistortionScore());
                break;
            case FILTER_DISTORTION:
                splits = SplitFilter.filterByDistortion(progressListener, taxa, tree2taxa, trees, splits, getOptionMaxDistortionScore(), getOptionMinNumberTrees(), true);
                break;
            case FILTER_MAJORITY:
                splits = SplitFilter.filterByMajority(progressListener, tree2taxa, tree2splits, splits);
                break;
            case FILTER_STRICT:
                splits = SplitFilter.filterByStrict(progressListener, tree2taxa, tree2splits, splits);
                break;
            case FILTER_LOOSE:
                splits = SplitFilter.filterByLoose(progressListener, tree2taxa, tree2splits, splits);
                break;
            case FILTER_PERCENT_THRESHOLD:
                splits = SplitFilter.filterByPrecentThreshold(progressListener, tree2taxa, tree2splits, splits, getOptionPercentThreshold());
                break;
        }
        return splits;
    }

    /**
     * computes the "in place" Z-closure
     *
     * @param progressListener the progressListenerument
	 */
    private SplitSystem computeClosure(ProgressListener progressListener, Collection<Split> inputSplits) throws CanceledException {
        Set<Integer> seniorSplits = new HashSet<>();
        Set<Integer> activeSplits = new HashSet<>();
        Set<Integer> newSplits = new HashSet<>();

        // setup "in place" array
        Split[] splits = new Split[inputSplits.size()];
        {
            int pos = 0;
            for (Split inputSplit : inputSplits) {
                splits[pos] = inputSplit;
                seniorSplits.add(pos);
                progressListener.setProgress(-1);
                pos++;
            }
        }

        // init:
        {
            for (int pos1 = 0; pos1 < splits.length; pos1++) {

                for (int pos2 = pos1 + 1; pos2 < splits.length; pos2++) {
                    Split ps1 = splits[pos1];
                    Split ps2 = splits[pos2];
                    Split qs1 = new Split();
                    Split qs2 = new Split();
                    if (applyZRule(ps1, ps2, qs1, qs2)) {
                        splits[pos1] = qs1;
                        splits[pos2] = qs2;
                        newSplits.add(pos1);
                        newSplits.add(pos2);
                    }
                    progressListener.setProgress(-1);
                }
            }
        }

        // main loop:
        {
            while (newSplits.size() != 0) {
                seniorSplits.addAll(activeSplits);
                activeSplits = newSplits;
                newSplits = new HashSet<>();

                for (Integer seniorSplit : seniorSplits) {

                    for (Integer activeSplit : activeSplits) {
                        Split ps1 = splits[(seniorSplit)];
                        Split ps2 = splits[(activeSplit)];
                        Split qs1 = new Split();
                        Split qs2 = new Split();
                        if (applyZRule(ps1, ps2, qs1, qs2)) {
                            splits[(seniorSplit)] = qs1;
                            splits[(activeSplit)] = qs2;
                            newSplits.add((seniorSplit));
                            newSplits.add((activeSplit));
                        }
                        progressListener.setProgress(-1);
                    }
                }
                for (Integer pos1 : activeSplits) {
                    for (Integer pos2 : activeSplits) {
                        Split ps1 = splits[pos1];
                        Split ps2 = splits[pos2];
                        Split qs1 = new Split();
                        Split qs2 = new Split();
                        if (applyZRule(ps1, ps2, qs1, qs2)) {
                            splits[pos1] = qs1;
                            splits[pos2] = qs2;
                            newSplits.add(pos1);
                            newSplits.add(pos2);
                        }
                        progressListener.setProgress(-1);
                    }
                }
            }
        }

        SplitSystem result = new SplitSystem();
        for (Integer pos1 : seniorSplits) {
            result.addSplit(splits[pos1]);
        }
        for (Integer pos1 : activeSplits) {
            result.addSplit(splits[pos1]);
        }
        return result;
    }

    /**
     * apply the Z rule, if applicable. That is, replaces A1/B1 and A2/B2
     * by  A1/(B1uB2)  and (A1uA2)/B2
     *
     * @param ps1 input partial split 1
     * @param ps2 input partial split 2
     * @param qs1 output 1
     * @param qs2 output2
     * @return true, if rule was applied and resulting splits differ from the original
     * ones
     */
    public static boolean applyZRule(Split ps1, Split ps2, Split qs1, Split qs2) {
        for (int i = 0; i <= 1; i++) {
            BitSet A1 = ps1.getSide(i);
            BitSet B1 = ps1.getSide(1 - i);
            for (int j = 0; j <= 1; j++) {
                BitSet A2 = ps2.getSide(j);
                BitSet B2 = ps2.getSide(1 - j);

                if (A1.intersects(A2) && A2.intersects(B1) && B1.intersects(B2) && A1.intersects(B2) == false) {
                    BitSet B1uB2 = getUnion(B1, B2);
                    qs1.set(A1, B1uB2);

                    BitSet A1uA2 = getUnion(A1, A2);
                    qs2.set(A1uA2, B2);
                    // System.err.println("[" + i + "," + j + "] " + ps1 + " + " + ps2 + " -> " + qs1 + " + " + qs2);
                    return !((ps1.equals(qs1) && ps2.equals(qs2)) || (ps1.equals(qs2) && ps2.equals(qs1)));
                }
            }
        }
        return false;
    }

    /**
     * gets the union of two bit sets
     *
     * @return union
     */
    public static BitSet getUnion(BitSet a, BitSet b) {
        BitSet result = (BitSet) a.clone();
        result.or(b);
        return result;
    }


    /**
     * set the weight to the mean weight of all projections of this split and confidence to
     * the count of trees containing a projection of the split
     *
     * @param tree2splits i-th component contains all partial splits of i-th tree
     * @param tree2taxa   taxa contained in i-th tree
     * @param splits      full splits computed by z-closure
     */
    private void setWeightsConfidences(ProgressListener progressListener, BitSet[] tree2taxa,
                                       SplitSystem[] tree2splits, SplitSystem splits) throws CanceledException {
        for (Iterator it = splits.iterator(); it.hasNext(); ) {
            Split current = (Split) it.next();
            progressListener.setProgress(-1);

            double min = 1000000;
            double sum = 0;
            double weighted = 0;
            double confidence = 0;
            int total = 0;
            for (int t = 0; t < tree2splits.length; t++) {
                Split projection = current.getInduced(tree2taxa[t]);
                if (projection != null)  // split cuts support set of tree t
                {
                    if (tree2splits[t].contains(projection)) {
                        double cur = tree2splits[t].get(projection).getWeight();
                        weighted += tree2taxa[t].cardinality() * cur;
                        if (cur < min)
                            min = cur;
                        sum += cur;
                        confidence += tree2taxa[t].cardinality() *
                                tree2splits[t].get(projection).getConfidence();
                    }
                    total += tree2taxa[t].cardinality();
                }
            }

            double value = 1;
            switch (getOptionEdgeWeights()) {
                case MIN:
                    value = min;
                    break;
                case MEAN:
                    value = weighted / total;
                    break;
                case TREESIZEWEIGHTEDMEAN:
                    value = sum / total;
                    break;
                case SUM:
                    value = sum;
                    break;
            }
            current.setWeight(value);
            current.setConfidence(total);
        }
    }

    /**
     * sets the weight of a split in the network as the average relative length of the edge
     * in the input trees
     *
	 */
    private void setWeightAverageReleativeLength(ProgressListener progressListener, BitSet[] tree2taxa,
                                                 SplitSystem[] tree2splits, SplitSystem splits) throws
            CanceledException {
        // compute average of weights and num of edges for each input tree
        float[] averageWeight = new float[tree2splits.length];
        int[] numEdges = new int[tree2splits.length];

        for (int t = 0; t < tree2splits.length; t++) {
            numEdges[t] = tree2splits[t].size();
            float sum = 0;
            for (Iterator it = tree2splits[t].iterator(); it.hasNext(); ) {
                Split ps = (Split) it.next();
                sum += ps.getWeight();
            }
            if (numEdges[t] > 0)
                averageWeight[t] = sum / numEdges[t];
            else
                averageWeight[t] = 0;
        }

        // consider each network split in turn:
        for (Iterator it = splits.iterator(); it.hasNext(); ) {
            Split current = (Split) it.next();
            progressListener.setProgress(-1);

            BitSet activeTrees = new BitSet(); // trees that contain projection of
            // current split

            for (int t = 0; t < tree2splits.length; t++) {
                Split projection = current.getInduced(tree2taxa[t]);
                if (projection != null && tree2splits[t].contains(projection)) {
                    activeTrees.set(t);
                }
            }

            float weight = 0;
            for (int t = activeTrees.nextSetBit(1); t >= 0; t = activeTrees.nextSetBit(t + 1)) {
                Split projection = current.getInduced(tree2taxa[t]);

                weight += tree2splits[t].get(projection).getWeight()
                        / averageWeight[t];
            }
            weight /= activeTrees.cardinality();
            current.setWeight(weight);
        }
    }

    public String getOptionEdgeWeights() {
        return optionEdgeWeights;
    }

    public void setOptionEdgeWeights(String optionEdgeWeights) {
        this.optionEdgeWeights = optionEdgeWeights;
    }

    /**
     * return the possible choices for optionEdgeWeights
     *
     * @return list of choices
     */
    public List<String> selectionOptionEdgeWeights(ProgressListener progressListener) {
        List<String> list = new LinkedList<>();
        list.add(AVERAGERELATIVE);
        list.add(MEAN);
        list.add(TREESIZEWEIGHTEDMEAN);
        list.add(SUM);
        list.add(MIN);
        list.add(NONE);
        return list;
    }


    public String getOptionFilter() {
        return optionFilter;
    }

    public void setOptionFilter(String optionFilter) {
        this.optionFilter = optionFilter;
    }

    /**
     * return the possible choices for optionEdgeWeights
     *
     * @return list of choices
     */
    public List<String> selectionOptionFilter(ProgressListener progressListener) {
        List<String> list = new LinkedList<>();
        list.add(FILTER_NONE);
        list.add(FILTER_TOTAL_SCORE);
        list.add(FILTER_DISTORTION);
        list.add(FILTER_MAJORITY);
        list.add(FILTER_LOOSE);
        list.add(FILTER_STRICT);

        return list;
    }

    public int getOptionMinNumberTrees() {
        return optionMinNumberTrees;
    }

    public void setOptionMinNumberTrees(int optionMinNumberTrees) {
        this.optionMinNumberTrees = optionMinNumberTrees;
    }

    public int getOptionMaxDistortionScore() {
        return optionMaxDistortionScore;
    }

    public void setOptionMaxDistortionScore(int optionMaxDistortionScore) {
        this.optionMaxDistortionScore = optionMaxDistortionScore;
    }

    public double getOptionPercentThreshold() {
        return optionPercentThreshold;
    }

    public void setOptionPercentThreshold(double optionPercentThreshold) {
        this.optionPercentThreshold = optionPercentThreshold;
    }

    /**
     * gets the set of taxa generated by the apply method
     *
     * @return taxa
     */
    public Taxa getTaxa() {
        return taxa;
    }

}
