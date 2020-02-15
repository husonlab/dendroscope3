/*
 *   ClusterThread.java Copyright (C) 2020 Daniel H. Huson
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

/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import java.util.HashSet;
import java.util.Vector;

/**
 * @author Beckson
 */
public class ClusterThread extends Thread {

    private int level;

    private boolean isStopped = false;
    private final HybridTree t1;
    private final HybridTree t2;
    private final ReplacementInfo rI;
    private final Hybrid hybrid;
    private View view;
    private final MyThreadPool threadPool;
    private final HashSet<Vector<HybridTree>> maxForests = new HashSet<>();
    private final Boolean insert;
    private ExhaustiveSearch eS;
    private HybridNetwork clustertNetwork;
    private final TreeMarker tM;
    private long timeStart;

    private Vector<HybridTree> forest = new Vector<>();
    private int hybridNumber = 0;
    private View.Computation compValue;
    private int lowerBound = 0;

    private final boolean caMode;

    public ClusterThread(HybridTree[] minClusterTrees, ReplacementInfo rI,
                         View view, MyThreadPool myPool, Boolean insert, TreeMarker tM,
                         View.Computation compValue, Hybrid hybrid, boolean caMode) {

        t1 = new HybridTree(minClusterTrees[0], false, null);
        t1.update();
        t2 = new HybridTree(minClusterTrees[1], false, null);
        t2.update();

        this.hybrid = hybrid;
        this.rI = rI;
        this.view = view;
        if (view != null)
            view.addClusterThread(this);
        this.threadPool = myPool;
        this.insert = insert;
        this.tM = tM;
        this.compValue = compValue;

        this.caMode = caMode;
    }

    public void run() {
        try {

            eS = new ExhaustiveSearch(t1, t2, threadPool, this, view,
                    compValue, rI, lowerBound, caMode);
            eS.run();
            eS.join();

            // System.out.println();
            // HybridTree x1 = new HybridTree(t1, false, t1.getTaxaOrdering());
            // x1.clearLabelings();
            // HybridTree x2 = new HybridTree(t2, false, t2.getTaxaOrdering());
            // x2.clearLabelings();
            // System.out.println(x1 + ";");
            // System.out.println(x2 + ";");

            if (maxForests.size() != 0)
                hybridNumber = maxForests.iterator().next().size() - 1;

            // System.out.println("Computed Cluster Distance: " + hybridNumber);
            // for (HybridTree component : maxForests.iterator().next())
            // System.out.println(component);

            if (compValue == View.Computation.rSPR_DISTANCE && maxForests.size() != 0) {
                forest = maxForests.iterator().next();
                // forest = minimumForest();

                // System.out.println("RC: "+t1.getReplacementCharacter());
                // System.out.println("MAF:");
                // for (HybridTree component : forest)
                // System.out.println(component);

                if (forest.lastElement().computeSetOfLeaves().size() == 1) {
                    if (t1.getReplacementCharacter() != null)
                        rI.getPrunedLabels().add(t1.getReplacementCharacter());
                }
            }
            if (compValue == View.Computation.NETWORK) {

                // computing all networks out of the MAAFs...

                if (!isStopped && insert && (maxForests.size() != 0)) {
                    for (Vector<HybridTree> forest : maxForests) {
                        HybridNetwork n = (new ComputeHybridNetwork()).run(
                                forest, t1, t2, rI, tM);
                        // HybridNetwork n = (new ComputeClearHybridNetwork())
                        // .run(forest, t1, t2, rI, tM);
                        rI.addNetwork(n, false);
                    }
                    if (view != null)
                        view.finishClusterThread(this);
                } else if (isStopped && maxForests.size() == 0) {
                    clustertNetwork = (new ComputeClusterNetwork()).run(t1, t2);
                    clustertNetwork.update();
                    if (insert) {
                        clustertNetwork.setReplacementCharacter(t1
                                .getReplacementCharacter());
                        rI.addNetwork(clustertNetwork, true);
                    }
                    if (view != null)
                        view.stopClusterThread(this);
                } else if (!insert) {
                    if (view != null)
                        view.finishClusterThread(this);
                }

            } else if (view != null)
                view.finishClusterThread(this);

            if (compValue != View.Computation.rSPR_DISTANCE) {
                if (view != null)
                    view.setDetails(this, " Network with hybrid number " + (hybridNumber) + " computed");
            } else {
                if (view != null)
                    view.setDetails(this, (hybridNumber) + " rSPR-operations computed");
            }

        } catch (Exception e) {
            if (view != null) {
                view.stopClusterThread(this);
                view.setProgress(this, -1);
            }
            e.printStackTrace();
        }
    }

    // private Vector<HybridTree> minimumForest() {
    // Vector<HybridTree> minForest = maxForests.iterator().next();
    // ;
    // int min = 0;
    // for (Vector<HybridTree> f : maxForests) {
    // int prunedLabels = 0;
    // for (HybridTree t : f) {
    // if (t.computeSetOfLeaves().size() == 1) {
    // String label = t.getLabel(t.computeSetOfLeaves().getFirstElement());
    // if (rI.getPrunedLabels().contains(label))
    // prunedLabels++;
    // }
    // }
    // if (prunedLabels > min) {
    // minForest = f;
    // min = prunedLabels;
    // } else if (prunedLabels == min) {
    // if (f.lastElement().computeSetOfLeaves().size() == 1)
    // minForest = f;
    // }
    // }
    // return minForest;
    // }

    public HashSet<Vector<HybridTree>> getMaxForests() {
        return maxForests;
    }

    public void stopThread() {
        if (hybrid != null)
            hybrid.setAborted(true);
        isStopped = true;
        if (eS != null)
            eS.stopThread();
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public HybridNetwork getClusterNetwork() {
        return clustertNetwork;
    }

    public long getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(long timeStart) {
        this.timeStart = timeStart;
    }

    public int getHybridNumber() {
        return hybridNumber;
    }

    public HybridTree getT1() {
        return t1;
    }

    public HybridTree getT2() {
        return t2;
    }

    public int getMax() {
        return t1.computeSetOfLeaves().size();
    }

    public void setCompValue(View.Computation compValue) {
        this.compValue = compValue;
    }

    public void setView(View view) {
        this.view = view;
    }

    public void setLowerBound(int lowerBound) {
        this.lowerBound = lowerBound;
    }

    public Vector<HybridTree> getForest() {
        return forest;
    }

}
