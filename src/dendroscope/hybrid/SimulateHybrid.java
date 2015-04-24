/**
 * Copyright 2015, Daniel Huson
 *
 *(Some files contain contributions from other authors, who are then mentioned separately)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package dendroscope.hybrid;

import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.util.HashSet;
import java.util.Vector;

public class SimulateHybrid extends Thread {

    private final Vector<HybridNetwork> networks = new Vector<>();
    private final boolean computeLowerBound = false;
    private final MyThreadPool myPool = new MyThreadPool();
    private final Vector<ClusterThread> distanceThreads = new Vector<>();
    private final Vector<ClusterThread> hybridThreads = new Vector<>();
    private final TreeMarker tM = new TreeMarker();
    private double timeStart;
    private int hybridNumber = 0;
    private int rSPRDistance = 0;
    private int maxNumber = 0;
    private double runtime;

    private final PhyloTree[] trees;
    private final View.Computation compValue;

    private final boolean caMode;

    public SimulateHybrid(PhyloTree[] trees, View.Computation compValue, boolean caMode) {
        this.compValue = compValue;
        this.trees = trees;
        this.caMode = caMode;
    }

    public void run() {
        try {
            if (new CheckTrees().run(trees)) {

                timeStart = System.currentTimeMillis();

                ReplacementInfo rI = new ReplacementInfo();

                // replacing each leaf labeling by a unique number
                // rI.replaceLabels(trees[0], trees[1]);

                HybridTree t1 = new HybridTree(trees[0], true, null);
                HybridTree t2 = new HybridTree(trees[1], true, null);

                // replacing common subtrees...
                // System.out.println("ReplaceCommonSubtrees");
                (new SubtreeReduction()).run(t1, t2, rI);

                // replacing max common chains
                Vector<Vector<Node>> t1Chains = (new FindMaxChains()).run(t1);
                Vector<Vector<Node>> t2Chains = (new FindMaxChains()).run(t2);
                (new ReplaceMaxCommonChains()).run(t1, t1Chains, t2, t2Chains,
                        rI);

                // getting common minimal clusters...
                // System.out.println("GetMinCommonCluster");
                ClusterReduction cR = new ClusterReduction();
                HybridTree[] minClusterTrees = cR.run(t1,
                        t2, rI);

                // computing all networks representing the minimal clusters...
                while (minClusterTrees != null) {
                    if (computeLowerBound || compValue == View.Computation.rSPR_DISTANCE) {
                        ClusterThread cT = new ClusterThread(minClusterTrees,
                                rI, null, myPool, true, tM, View.Computation.rSPR_DISTANCE, null, caMode);
                        cT.setLevel(cR.getLevel(minClusterTrees));
                        distanceThreads.add(cT);
                    }
                    if (compValue != View.Computation.rSPR_DISTANCE) {
                        HybridTree[] minClusterTreesCopy = {
                                new HybridTree(minClusterTrees[0], false,
                                        minClusterTrees[0].getTaxaOrdering()),
                                new HybridTree(minClusterTrees[1], false,
                                        minClusterTrees[1].getTaxaOrdering())};
                        hybridThreads.add(new ClusterThread(
                                minClusterTreesCopy, rI, null, myPool, true,
                                tM, compValue, null, caMode));
                    }

                    // System.out.println("ClusterReduction");
                    minClusterTrees = cR.run(t1, t2, rI);
                }

                HashSet<Vector<HybridTree>> sprForests = new HashSet<>();
                if (computeLowerBound || compValue == View.Computation.rSPR_DISTANCE) {
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
                                // System.out.println("rSPRDistance: "+cT.getHybridNumber());
                            }
                        }
                        for (ClusterThread cT : distanceThreads) {
                            if (cT.getLevel() == i)
                                cT.join();
                        }
                    }
                    for (ClusterThread cT : distanceThreads) {
                        rSPRDistance += cT.getHybridNumber();
                        sprForests.add(cT.getForest());
                        if (cT.getHybridNumber() > maxNumber)
                            maxNumber = cT.getHybridNumber();
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
                    }
                    // waiting for each cluster thread...
                    for (ClusterThread cT : hybridThreads)
                        cT.join();
                    for (ClusterThread cT : hybridThreads) {
                        hybridNumber += cT.getHybridNumber();
                        if (cT.getHybridNumber() > maxNumber)
                            maxNumber = cT.getHybridNumber();
                    }
                }
                // *******************************************************

                // computing all MAAFs...

                HybridTree[] clusterTrees = {t1, t2};

                ClusterThread cTDistance = null;
                if (computeLowerBound || compValue == View.Computation.rSPR_DISTANCE)
                    cTDistance = new ClusterThread(clusterTrees, rI, null,
                            myPool, false, tM, View.Computation.rSPR_DISTANCE, null, caMode);

                ClusterThread cTHybrid = null;
                if (compValue != View.Computation.rSPR_DISTANCE) {
                    HybridTree[] clusterTreesCopy = {
                            new HybridTree(t1, false, t1.getTaxaOrdering()),
                            new HybridTree(t2, false, t2.getTaxaOrdering())};
                    cTHybrid = new ClusterThread(clusterTreesCopy, rI, null,
                            myPool, false, tM, compValue, null, caMode);
                }

                if (computeLowerBound || compValue == View.Computation.rSPR_DISTANCE) {
                    // dSPR-Distance
                    // ****************************************************
                    cTDistance.run();
                    cTDistance.join();
                    if (cTHybrid != null)
                        cTHybrid.setLowerBound(cTDistance.getHybridNumber());
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
                }
                // ****************************************************

                // no forest thread in use, all MAAFs computed
                myPool.shutDown();
                stopThreads();
                runtime = (System.currentTimeMillis() - timeStart) / 1000;
                // System.out.println("Runtime: " + runtime);

                if (compValue == View.Computation.NETWORK) {

                    int numOfNets = getNumOfNetworks(rI);

                    HashSet<Vector<HybridTree>> maxForests = cTHybrid
                            .getMaxForests();

                    if (maxForests.size() != 0) {
                        for (Vector<HybridTree> forest : maxForests) {
                            // computing all MAAFs...
                            System.out.println("ComputeHybridNetwork");
                            HybridNetwork n = (new ComputeHybridNetwork()).run(
                                    forest, t1, t2, rI, tM);

                            // re-attaching minimal clusters...
                            System.out.println("ReattachNetworks");
                            ReattachNetworks rN = new ReattachNetworks(n, rI,
                                    tM, numOfNets, null);
                            rN.start();
                            rN.join();
                            Vector<HybridNetwork> v = rN.getNetworks();

                            for (HybridNetwork h : v)
                                networks.add(h);
                        }
                    } else {
                        HybridNetwork n = cTHybrid.getClusterNetwork();

                        // re-attaching minimal clusters...
                        System.out.println("ReattachNetworks");
                        ReattachNetworks rN = new ReattachNetworks(n, rI, tM,
                                numOfNets, null);
                        rN.start();
                        rN.join();
                        Vector<HybridNetwork> v = rN.getNetworks();

                        for (HybridNetwork h : v)
                            networks.add(h);
                    }

                    for (ClusterThread t : hybridThreads)
                        t.interrupt();

                    tM.setNetworks(networks);
                    tM.initT1Edges();

                } else if (compValue == View.Computation.HYBRID_NUMBER)
                    System.out.println("The hybridization number is "
                            + hybridNumber + ".");
                else {
                    rSPRDistance = new ComputeDistance().run(sprForests, rI);
                    System.out.println("rSPR Distance: " + rSPRDistance);
                }

            } else
                throw new Exception("Hybrid Trees are not legal!");
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

    public void stopThreads() {
        myPool.shutDown();
        for (ClusterThread cT : distanceThreads)
            cT.stopThread();
        for (ClusterThread cT : hybridThreads)
            cT.stopThread();
    }

    public Vector<HybridNetwork> getNetworks() {
        return networks;
    }

    public double getRuntime() {
        return runtime;
    }

    public Integer getClusterSize() {
        if (compValue == View.Computation.rSPR_DISTANCE)
            return distanceThreads.size();
        return hybridThreads.size();
    }

    public Integer getMaxNumber() {
        return maxNumber;
    }

    public int getHybridNumber() {
        return hybridNumber;
    }

    public int getdSPRDistance() {
        return rSPRDistance;
    }

}
