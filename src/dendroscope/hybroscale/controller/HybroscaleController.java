/*
 *   HybroscaleController.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.controller;

import dendroscope.hybroscale.model.HybridManager.Computation;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;
import dendroscope.hybroscale.view.ConstraintWindow;
import dendroscope.hybroscale.view.HybridView;
import dendroscope.hybroscale.view.ProcWindow;

import javax.swing.*;
import java.util.Collections;
import java.util.Vector;

public class HybroscaleController {

    private HybridView hView;
    private ProcWindow pW;
    private ConstraintWindow cW;
    private HybridLauncher algorithm;
    private int poolSize = -1;
    private MyPhyloTree[] trees;
    private Object syncObject;

    private int edgeNumber = -1;
    private int hybridNumber = -1;
    private MyPhyloTree[] networks;

    public HybroscaleController(String[] newickStrings, Object syncObject, JFrame mainFrame, Computation comp,
                                int cores, boolean showView) {

        if (showView)
            this.hView = new HybridView(mainFrame, this, comp, cores, null);
        this.pW = new ProcWindow(hView, this);

        trees = new MyPhyloTree[newickStrings.length];
        for (int i = 0; i < newickStrings.length; i++) {
            String s = newickStrings[i];
            MyPhyloTree t = new MyPhyloTree();
            t.parseBracketNotation(s);
            trees[i] = t;
        }

        Vector<String> taxa = new Vector<String>();
        for (MyPhyloTree t : trees) {
            for (MyNode l : t.getLeaves()) {
                if (!taxa.contains(l.getLabel()))
                    taxa.add(l.getLabel());
            }
        }
        Collections.sort(taxa);

        if (hView != null)
            this.cW = new ConstraintWindow(hView, taxa);
        this.syncObject = syncObject;
        poolSize = Runtime.getRuntime().availableProcessors() - 1;

    }

    public void run(Computation compValue, int cores, Integer maxK, boolean upperBound) {
        algorithm = new HybridLauncher(trees, hView, this, compValue, poolSize, maxK, upperBound, cW.getConstraints());
        algorithm.start();
    }

    public void stop(boolean close) {
        if (algorithm != null && algorithm.isAlive())
            algorithm.stopComputation();
        else {
            if (algorithm != null && algorithm.isAlive())
                algorithm.interrupt();
            hView.setVisible(false);
            pW.setVisible(false);
            pW.dispose();
            hView.dispose();
            synchronized (syncObject) {
                syncObject.notify();
            }
        }
    }

    public void setView(HybridView view) {
        this.hView = view;
    }

    public void showProcWindow() {
        pW.pack();
        pW.setAlwaysOnTop(true);
        pW.setVisible(true);
    }

    public void showConstraintWindow() {
        cW.pack();
        cW.setAlwaysOnTop(true);
        cW.setVisible(true);
    }

    public void disableConstraintWindow() {
        cW.disableAdding();
    }

    public void setCores(int num) {
        poolSize = num;
        hView.updateProc(num);
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void reportTrees(MyPhyloTree[] networks) {
        this.networks = networks;
        synchronized (syncObject) {
            syncObject.notify();
        }
    }

    public void setHybridNumber(int hybridNumber) {
        this.hybridNumber = hybridNumber;
    }

    public MyPhyloTree[] getNetworks() {
        return networks;
    }

    public String[] getNetworkStrings() {
        if (networks != null) {
            String[] networkStrings = new String[networks.length];
            for (int i = 0; i < networks.length; i++)
                networkStrings[i] = networks[i].toMyBracketString();
            return networkStrings;
        }
        return null;
    }

    public void setEdgeNumber(int edgeNumber) {
        this.edgeNumber = edgeNumber;
    }

    public int getHybridNumber() {
        return hybridNumber;
    }

    public int getEdgeNumber() {
        return edgeNumber;
    }

    public int getNumberOfTrees() {
        return trees.length;
    }

    public HybridView getHybridView() {
        return hView;
    }

}
