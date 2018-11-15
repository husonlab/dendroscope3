/**
 * Hybrid.java 
 * Copyright (C) 2018 Daniel H. Huson
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
import jloda.phylo.PhyloTree;

import java.util.Collections;
import java.util.HashSet;
import java.util.Vector;

/**
 * This function manages the computation of all minimal networks displaying two
 * rooted, bifurcating phylogenetic trees.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class Hybrid {

    private final Vector<HybridNetwork> networks = new Vector<>();
    private boolean isReady = false;
    private final MyThreadPool myPool = new MyThreadPool();
    private final Vector<ClusterThread> distanceThreads = new Vector<>();
    private final Vector<ClusterThread> hybridThreads = new Vector<>();
    private final TreeMarker tM = new TreeMarker();
    private static long timeStart = 0;
    private int hybridNumber = 0;
    private int rSPRDistance = 0;
    private boolean isAborted = false;

    public Hybrid(PhyloTree[] trees, final View view, Controller controller,
                  View.Computation compValue, boolean caMode) {
        try {
            if (new CheckTrees().run(trees)) {

                controller.setMyPool(myPool);
                if (controller.getPoolSize() != -1)
                    myPool.setSize(controller.getPoolSize());

                final long time = System.currentTimeMillis();
                timeStart = time;

                Thread timeThread = new Thread(new Thread() {
                    public void run() {
                        while (!isReady) {
                            if (view != null)
                                view.updateTime(System.currentTimeMillis() - time);
                            try {
                                sleep(500);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                });
                timeThread.start();

                ReplacementInfo rI = new ReplacementInfo();

                // replacing each leaf labeling by a unique number
                // rI.replaceLabels(trees[0], trees[1]);

                HybridTree t1 = new HybridTree(trees[0], true, null);
                HybridTree t2 = new HybridTree(trees[1], true, null);

                // replacing common subtrees...
                System.err.println("ReplaceCommonSubtrees");
                (new SubtreeReduction()).run(t1, t2, rI);

                // replacing max common chains
                Vector<Vector<Node>> t1Chains = (new FindMaxChains()).run(t1);
                Vector<Vector<Node>> t2Chains = (new FindMaxChains()).run(t2);
//                (new ReplaceMaxCommonChains()).run(t1, t1Chains, t2, t2Chains,
//                       rI);

                // getting common minimal clusters...
                System.err.println("GetMinCommonCluster");
                ClusterReduction cR = new ClusterReduction();
                HybridTree[] minClusterTrees = cR.run(t1, t2, rI);
                // HybridTree[] minClusterTrees = null;

                // computing all networks representing the minimal clusters...
                while (minClusterTrees != null) {
                    if (compValue == View.Computation.rSPR_DISTANCE) {
                        ClusterThread cT = new ClusterThread(minClusterTrees, rI, view, myPool, true, tM, View.Computation.rSPR_DISTANCE, this, caMode);
                        cT.setLevel(cR.getLevel(minClusterTrees));
                        distanceThreads.add(0, cT);
                    } else {
                        hybridThreads.add(new ClusterThread(minClusterTrees, rI, view, myPool, true, tM, compValue, this, caMode));
                    }

                    System.err.println("ClusterReduction");
                    minClusterTrees = cR.run(t1, t2, rI);

                }

                HashSet<Vector<HybridTree>> sprForests = new HashSet<>();
                if (compValue == View.Computation.rSPR_DISTANCE) {
                    // dSPR-Distance
                    // ****************************************************
                    for (int i = 0; i <= cR.getMaxLevel(); i++) {
                        for (ClusterThread cT : distanceThreads) {
                            if (cT.getLevel() == i) {
                                cT.setCompValue(compValue);
                                cT.start();
                                // cT.join();
                                // if (hybridThreads.size() != 0)
                                // hybridThreads.get(distanceThreads.indexOf(cT))
                                // .setLowerBound(cT.getHybridNumber());
                                // System.err.println("rSPRDistance: "+cT.getHybridNumber());
                            }
                        }
                        for (ClusterThread cT : distanceThreads) {
                            if (cT.getLevel() == i)
                                cT.join();
                        }
                    }
                    // for (ClusterThread cT : distanceThreads)
                    // cT.join();
                    for (ClusterThread cT : distanceThreads) {
                        rSPRDistance += cT.getHybridNumber();
                        sprForests.add(cT.getForest());
                    }

                    // ******************************************************
                }

                // Hybrid
                // ******************************************************
                if (compValue != View.Computation.rSPR_DISTANCE) {
                    // starting each cluster thread...
                    for (ClusterThread cT : hybridThreads) {
                        cT.setCompValue(compValue);
                        cT.start();
                        // cT.join(); //COMMENT OUT - ONLY FOR DEBUG!
                    }
                    // waiting for each cluster thread...
                    for (ClusterThread cT : hybridThreads)
                        cT.join();
                    for (ClusterThread cT : hybridThreads) {
                        // System.err.println("HybridNumber: "
                        // + cT.getHybridNumber());
                        hybridNumber += cT.getHybridNumber();
                    }
                }
                // *******************************************************

                // computing all MAAFs...

                HybridTree[] clusterTrees = {t1, t2};

                ClusterThread cTDistance = null;
                if (compValue == View.Computation.rSPR_DISTANCE)
                    cTDistance = new ClusterThread(clusterTrees, rI, view,
                            myPool, false, tM, View.Computation.rSPR_DISTANCE, this, caMode);

                ClusterThread cTHybrid = null;
                if (compValue != View.Computation.rSPR_DISTANCE) {
                    HybridTree[] clusterTreesCopy = {
                            new HybridTree(t1, false, t1.getTaxaOrdering()),
                            new HybridTree(t2, false, t2.getTaxaOrdering())};
                    cTHybrid = new ClusterThread(clusterTreesCopy, rI, view,
                            myPool, false, tM, compValue, this, caMode);
                }

                if (compValue == View.Computation.rSPR_DISTANCE) {
                    // dSPR-Distance
                    // ****************************************************
                    cTDistance.run();
                    cTDistance.join();
                    if (cTHybrid != null)
                        cTHybrid.setLowerBound(cTDistance.getHybridNumber());

                    // System.err.println("rSPRDistance: "
                    // + cTDistance.getHybridNumber());

                    rSPRDistance += cTDistance.getHybridNumber();
                    sprForests.add(cTDistance.getForest());
                    // ****************************************************
                }

                // Hybrid
                // ****************************************************
                if (compValue != View.Computation.rSPR_DISTANCE) {
                    cTHybrid.setCompValue(compValue);
                    cTHybrid.run();
                    cTHybrid.join();
                    hybridNumber += cTHybrid.getHybridNumber();

                    // System.err.println("HybridNumber: "
                    // + cTHybrid.getHybridNumber());
                }
                // ****************************************************

                // no forest thread in use, all MAAFs computed
                myPool.shutDown();
                isReady = true;

                if (compValue == View.Computation.NETWORK) {

                    int numOfNets = getNumOfNetworks(rI);

                    HashSet<Vector<HybridTree>> maxForests = cTHybrid
                            .getMaxForests();

                    if (maxForests.size() != 0) {
                        for (Vector<HybridTree> forest : maxForests) {

                            HybridNetwork n = (new ComputeHybridNetwork()).run(
                                    forest, t1, t2, rI, tM);

                            ReattachNetworks rN = new ReattachNetworks(n, rI,
                                    tM, numOfNets, view);
                            // controller.showCreatingTreesFrame(rN);
                            rN.start();
                            rN.join();
                            Vector<HybridNetwork> v = rN.getNetworks();
                            // controller.hideCreatingFrames();

                            for (HybridNetwork h : v)
                                networks.add(h);
                        }
                    } else {
                        HybridNetwork n = cTHybrid.getClusterNetwork();

                        ReattachNetworks rN = new ReattachNetworks(n, rI, tM,
                                numOfNets, view);
                        // controller.showCreatingTreesFrame(rN);
                        rN.start();
                        rN.join();
                        Vector<HybridNetwork> v = rN.getNetworks();
                        // controller.hideCreatingFrames();

                        for (HybridNetwork h : v)
                            networks.add(h);
                    }

                    for (ClusterThread t : hybridThreads)
                        t.interrupt();

                    isReady = true;
                    if (view != null)
                        view.reportNetworks(numOfNets, hybridNumber);

                    NetworkComparator nC = new NetworkComparator(networks);
                    Collections.sort(networks, nC);

                    // for (HybridNetwork h : networks)
                    // System.err.println("Weight: "+nC.getWeight(h));

                    tM.setNetworks(networks);
                    tM.initT1Edges();
                    controller.setTreeMarker(tM);

                    controller.setNodeWeights(new ComputeNodeWeights(networks));

                    System.out.println("Hybridization number: " + hybridNumber);
                } else if (compValue == View.Computation.HYBRID_NUMBER) {
                    if (view != null)
                        view.reportNumber(hybridNumber);
                    controller.setHybridNumber(hybridNumber);
                    // if (!isAborted)
                    // view.createInfoFrame("Result", "Hybridization Number: "
                    // + hybridNumber);
                    // else
                    // view.createInfoFrame("Result",
                    // "Hybridization Number: >=" + hybridNumber);
                    System.out.println("Hybridization number: " + hybridNumber);
                } else {
                    rSPRDistance = new ComputeDistance().run(sprForests, rI);
                    if (view != null)
                        view.reportDistance(rSPRDistance);
                    controller.setrSPRDistance(rSPRDistance);
                    // if (!isAborted)
                    // view.createInfoFrame("Result", "rSPR Distance: "
                    // + rSPRDistance + ".");
                    // else
                    // view.createInfoFrame("Result", "rSPR Distance: >="
                    // + rSPRDistance + ".");
                    System.out.println("rSPR distance: " + rSPRDistance);
                }

                Long runtime = System.currentTimeMillis() - time;
                Long seconds = runtime / 1000;
                System.err.println("Runtime: " + seconds + " seconds ");
                stop();

            } else {
                view.createInfoFrame("Error", "Hybrid Trees are not resolved!");
                throw new Exception("Hybrid Trees are not legal!");
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private int getNumOfNetworks(ReplacementInfo rI) {
        int i = 1;
        for (String s : rI.getLabelToNetworks().keySet())
            i *= rI.getLabelToNetworks().get(s).size();
        return i;
    }

    public void stop() {
        myPool.shutDown();
        for (ClusterThread cT : distanceThreads)
            cT.stopThread();
        for (ClusterThread cT : hybridThreads)
            cT.stopThread();
        isReady = true;
    }

    public Vector<HybridNetwork> getNetworks() {
        for (HybridNetwork n : networks) {
            for (Node v : n.getNodes())
                n.setInfo(v, null);
        }
        return networks;
    }

    public static long getTimeStart() {
        return timeStart;
    }

    @SuppressWarnings("static-access")
    public void setTimeStart(long timeStart) {
        this.timeStart = timeStart;
    }

    public void setAborted(boolean isAborted) {
        this.isAborted = isAborted;
    }

    public int getHybridNumber() {
        return hybridNumber;
    }

    public int getrSPRDistance() {
        return rSPRDistance;
    }

    public boolean isAborted() {
        return isAborted;
    }
}
