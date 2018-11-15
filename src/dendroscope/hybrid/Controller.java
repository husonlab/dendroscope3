/**
 * Controller.java 
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
