/**
 * ExhaustiveSearch.java 
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
package dendroscope.hybrid;

import jloda.graph.Node;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.Future;

/**
 * This function computes all maximum acyclic agreement forests of two rooted,
 * bifurcating phylogenetic trees.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class ExhaustiveSearch extends Thread {

    private final ReplacementInfo rI;
    private final ClusterThread clusterThread;
    private final MyThreadPool threadPool;
    private Boolean isNotified = false;
    private final HybridTree t1;
    private final HybridTree t2;

    private final Hashtable<Integer, HashSet<Vector<HybridTree>>> numberToForests = new Hashtable<>();
    private Integer[] threadResults;

    private final HashSet<Vector<HybridTree>> acyclicSetOfForests = new HashSet<>();
    private final Vector<ForestThread> threads = new Vector<>();
    private final Thread reportProgress = reportProgress();
    private boolean isStopped = false;
    private boolean stop = false;
    private final View view;
    private final View.Computation compValue;
    private int lowerBound = 0;

    private final GetIllegalTrees gIt = new GetIllegalTrees();
    private final boolean caMode;

    public ExhaustiveSearch(HybridTree t1, HybridTree t2,
                            MyThreadPool threadPool, ClusterThread clusterThread, View view,
                            View.Computation compValue, ReplacementInfo rI, int lowerBound, boolean caMode) {
        this.threadPool = threadPool;
        this.clusterThread = clusterThread;
        this.t1 = t1;
        this.t2 = t2;
        this.view = view;
        this.compValue = compValue;
        this.rI = rI;
        this.lowerBound = lowerBound;
        this.caMode = caMode;
    }

    public void run() {

        // if (compValue == 2) {
        // removePrunedLeaves(t1);
        // removePrunedLeaves(t2);
        // }

        try {
            // trees are isomorphic - nothing to be done...
            if ((new IsomorphismCheck()).run(t1, t2)) {

                Vector<HybridTree> acyclicForests = new Vector<>();
                acyclicForests.add(t1);
                acyclicSetOfForests.add(acyclicForests);

                HashSet<Vector<HybridTree>> maxForests = clusterThread
                        .getMaxForests();

                maxForests.add(acyclicForests);

                if (view != null) {
                    view.setProgress(clusterThread, 100);
                    if (compValue != View.Computation.rSPR_DISTANCE)
                        view.setDetails(clusterThread,
                                " Network with hybrid number " + 0
                                        + " computed. ");
                    else
                        view.setDetails(clusterThread,
                                0 + " rSPR-operations computed. ");
                }

                // trees not isomorphic - searching for forests...
            } else {
                int numOfEdges = t1.getNumberOfEdges() - 2;
                int approximation;
                if (lowerBound == 0 && !caMode) {
                    Vector<EasyTree> v = new Vector<>();
                    v.add(new EasyTree(t2));
                    double kPrime = (new EasyForestApproximation(
                            new Hashtable<String, Vector<String>>(),
                            t1.getTaxaOrdering())).run(new EasyTree(t1),
                            new EasyTree(t2), new EasyTree(t1), v, compValue);
                    approximation = (int) (kPrime / 3.0);
                    // System.out.println(" -- kPrime: " + kPrime);
                    // System.out.println(" -- approximation: "
                    // + (int) (kPrime / 3.0));
                } else {
                    approximation = lowerBound;
                    // System.out.println(" ---- UpperBound != 0: "+approximation);
                }

                // init thread results to not reported (-1)
                threadResults = new Integer[numOfEdges];
                threadResults[0] = 0;

                for (int i = 1; i < numOfEdges; i++) {
                    if (i <= approximation) {
                        threadResults[i] = 0;
                    } else
                        threadResults[i] = -1;
                }

                clusterThread.setTimeStart(System.currentTimeMillis());

                if (!stop) {
                    // try forests with ascending size
                    // compute all forests of size i

                    Hashtable<Integer, Vector<BitSet>> indexToIllegalCombi;
                    Vector<String> dummyLabels = new Vector<>();
                    if (caMode) {
                        if (caMode && compValue == View.Computation.rSPR_DISTANCE) {
                            for (Node v : t1.computeSetOfLeaves()) {
                                if (rI.getPrunedLabels().contains(
                                        t1.getLabel(v))) {
                                    dummyLabels.add(t1.getLabel(v));
                                    t1.deleteSubtree(v, null, true);
                                }
                            }
                            for (Node v : t2.computeSetOfLeaves()) {
                                if (rI.getPrunedLabels().contains(
                                        t2.getLabel(v)))
                                    t2.deleteSubtree(v, null, true);
                            }
                        }
                        indexToIllegalCombi = gIt.run(t1, view, clusterThread);
                    } else
                        indexToIllegalCombi = new Hashtable<>();

                    for (int i = approximation; i < numOfEdges; i++) {

                        HybridTree t1Copy = new HybridTree(t1, false, null);
                        t1Copy.update();
                        HybridTree t2Copy = new HybridTree(t2, false, null);
                        t2Copy.update();

                        ForestThread t = new ForestThread(this, t1Copy, t2Copy,
                                i, numOfEdges, compValue, rI, caMode);

                        if (caMode) {
                            t.setIndexToIllegalCombi(indexToIllegalCombi);
                            t.setDummyLabels(dummyLabels);
                        }
                        threads.add(t);
                    }

                    if (view != null)
                        view.setStatus(clusterThread, "Computing Forests...");

                    for (ForestThread t : threads) {
                        Future<?> f = threadPool.runTask(t);
                    }

                    if (view != null)
                        reportProgress.start();

                    sleep(500);
                    if (!isNotified)
                        synchronized (this) {
                            this.wait();
                        }

                }

            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @SuppressWarnings("unchecked")
    public synchronized void reportResult(
            Hashtable<Integer, HashSet<Vector<HybridTree>>> result,
            int edgeChoice, ForestThread forestThread) {

        if (result.size() == 0 && threadResults[edgeChoice] == -1)
            threadResults[edgeChoice] = 0;
        else {
            for (Integer h : result.keySet()) {
                if (numberToForests.containsKey(h)) {
                    HashSet<Vector<HybridTree>> set = numberToForests.get(h);
                    HashSet<Vector<HybridTree>> newSet = (HashSet<Vector<HybridTree>>) set
                            .clone();
                    numberToForests.remove(h);
                    for (Vector<HybridTree> forest : result.get(h))
                        newSet.add(forest);
                    numberToForests.put(h, newSet);
                } else {
                    HashSet<Vector<HybridTree>> set = new HashSet<>();
                    for (Vector<HybridTree> forest : result.get(h))
                        set.add(forest);
                    numberToForests.put(h, set);
                }

                if (numberToForests.containsKey(edgeChoice))
                    threadResults[edgeChoice] = 1;
                else
                    threadResults[edgeChoice] = 0;

            }
        }

        for (int i = 1; i < threadResults.length; i++) {
            if (isNotified)
                break;
            else if (!isStopped && threadResults[i] == -1)
                break;
            else if (!isStopped && threadResults[i] == 1) {

                // reporting result to cluster thread
                HashSet<Vector<HybridTree>> finalResult = numberToForests
                        .get(i);
                HashSet<Vector<HybridTree>> maxForests = clusterThread
                        .getMaxForests();
                for (Vector<HybridTree> aFinalResult : finalResult) maxForests.add(aFinalResult);

                isNotified = true;
                isStopped = true;
                reportProgress.interrupt();

                if (view != null) {
                    view.setProgress(clusterThread, 100);
                    if (compValue != View.Computation.rSPR_DISTANCE)
                        view.setDetails(clusterThread,
                                " Network with hybrid number " + (i - 1)
                                        + " computed. ");
                    else
                        view.setDetails(clusterThread, (i - 1)
                                + " rSPR-operations computed. ");
                }

                threadPool.removeThreads(threads);

                synchronized (this) {
                    this.notify();
                    this.notifyAll();
                }

                break;
            }
        }

    }

    public Thread reportProgress() {
        return new Thread(new Thread() {
            public void run() {
                int i = 1;
                Long time = System.currentTimeMillis();
                while (!isStopped && threadResults[i] != -1)
                    i++;
                while (i < threadResults.length && !isStopped) {
                    view.setProgress(clusterThread,
                            getProcent(i, threadResults.length - 1));
                    if (compValue != View.Computation.rSPR_DISTANCE) {
                        view.setDetails(
                                clusterThread,
                                " Searching for a network with hybrid number "
                                        + (i - 1)
                                        + "/"
                                        + (threadResults.length - 2)
                                        + ". ("
                                        + ((System.currentTimeMillis() - time) / 1000)
                                        + "s)");
                    } else {
                        view.setDetails(
                                clusterThread,
                                " Searching for "
                                        + (i - 1)
                                        + "/"
                                        + (threadResults.length - 2)
                                        + " rSPR-operations. ("
                                        + ((System.currentTimeMillis() - time) / 1000)
                                        + "s)");
                    }
                    if (threadResults[i] != -1) {
                        i++;
                        time = System.currentTimeMillis();
                    }
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        // e.printStackTrace();
                    }
                }
            }

            private int getProcent(double i, double j) {
                return (int) Math.round((i / j) * 100);
            }
        });
    }


    public HashSet<Vector<HybridTree>> getAcyclicSetOfForests() {
        return acyclicSetOfForests;
    }

    public void stopThread() {
        stop = true;
        reportProgress.stop();
        threadPool.removeThreads(threads);
        for (ForestThread t : threads)
            t.stopThread();
        synchronized (this) {
            this.notify();
        }
    }

    public Vector<ForestThread> getThreads() {
        return threads;
    }

}
