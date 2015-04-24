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

import dendroscope.core.TreeData;
import dendroscope.window.MultiViewer;
import jloda.phylo.PhyloTree;

import java.awt.*;

public class Controller {

    private View view;
    private final ProcWindow pW;
    private Algorithm algorithm;
    private MyThreadPool myPool;
    private int poolSize = -1;
    private TreeData[] treeData;
    private final PhyloTree[] trees;
    private final Object syncObject;

    private ComputeNodeWeights computeNodeWeights;
    private TreeMarker tM;
    private MultiViewer mV;

    private int hybridNumber = -1;
    private int rSPRDistance = -1;

    public Controller(PhyloTree[] trees, Object syncObject) {
        this.pW = new ProcWindow(this);
        this.trees = trees;
        this.syncObject = syncObject;
    }

    public void run(View.Computation compValue, boolean caMode) {
        algorithm = new Algorithm(trees, view, this, compValue, caMode);
        algorithm.start();
    }

    public void stop() {
        if (myPool != null)
            myPool.shutDown();
        if (algorithm != null && algorithm.isAlive())
            algorithm.interrupt();
        view.setVisible(false);
        pW.setVisible(false);
        pW.dispose();
        view.dispose();

        synchronized (syncObject) {
            syncObject.notify();
        }

    }

    public void setView(View view) {
        this.view = view;
    }

    public void showProcWindow() {
        pW.pack();

        pW.setLocation(
                (Toolkit.getDefaultToolkit().getScreenSize().width / 2) - (pW.getWidth() / 2),
                (Toolkit.getDefaultToolkit().getScreenSize().height / 2) - (pW.getHeight() / 2));

        pW.setAlwaysOnTop(true);

        pW.setVisible(true);
    }

    public void setCores(int num) {
        poolSize = num;
        view.updateProc(num);
    }

    public void setMyPool(MyThreadPool myPool) {
        this.myPool = myPool;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void printTrees(TreeData[] treeData) {
        this.treeData = treeData;

        synchronized (syncObject) {
            syncObject.notify();
        }

    }

    public TreeData[] getTreeData() {
        return treeData;
    }

    public void markTrees(boolean treeT1) {
        tM.markReticulateEdges(treeT1, mV);
    }

    public void selectCommonEdges() {
        tM.markTreeEdges(mV);
    }

    public void unmarkTrees() {
        tM.unmark(mV);
    }

    public void setMultiViewer(MultiViewer mV) {
        this.mV = mV;
        view.enableMarkingTrees();
        tM.init(mV);
        computeNodeWeights.setMultiViewer(mV);
    }

    public void setTreeMarker(TreeMarker tM) {
        this.tM = tM;
    }

    public void showNodeOcc() {
        computeNodeWeights.showOccurrences();
    }

    public void hideNodeOcc() {
        computeNodeWeights.hideOccurrences();
    }

    public void setNodeWeights(ComputeNodeWeights computeNodeWeights) {
        this.computeNodeWeights = computeNodeWeights;
    }

    public void setHybridNumber(int hybridNumber) {
        this.hybridNumber = hybridNumber;
    }

    public void setrSPRDistance(int rSPRDistance) {
        this.rSPRDistance = rSPRDistance;
    }

    public int getHybridNumber() {
        return hybridNumber;
    }

    public int getrSPRDistance() {
        return rSPRDistance;
    }

}
