/*
 *   HybridLauncher.java Copyright (C) 2020 Daniel H. Huson
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

import dendroscope.hybroscale.model.HybridManager;
import dendroscope.hybroscale.model.HybridManager.Computation;
import dendroscope.hybroscale.model.treeObjects.HybridNetwork;
import dendroscope.hybroscale.util.graph.MyPhyloTree;
import dendroscope.hybroscale.view.HybridView;

import java.util.Vector;


public class HybridLauncher extends Thread {

	private HybridView view;
	private HybroscaleController controller;
	private MyPhyloTree[] trees;
	private Computation computation;
	private int cores;
	private Integer maxK;
	private HybridManager hybridManager;
	private boolean upperBound;
	private String constraints;

	public HybridLauncher(MyPhyloTree[] trees, HybridView view, HybroscaleController controller,
						  Computation computation, int cores, Integer maxK, boolean upperBound, String constraints) {
		this.trees = trees;
		this.view = view;
		this.controller = controller;
		this.computation = computation;
		this.cores = cores;
		this.maxK = maxK;
		this.trees = trees;
		this.view = view;
		this.controller = controller;
		this.upperBound = upperBound;
		this.constraints = constraints;
	}

	public void run() {
		hybridManager = new HybridManager(trees, computation, cores, maxK, upperBound, constraints, true, false);
		hybridManager.addObserver(this);
		hybridManager.addView(view);
		hybridManager.run();
	}

	public void stopComputation(){
		if(hybridManager != null)
			hybridManager.stopThreads();
	}

	public void interrupt(){
		if(hybridManager != null)
			hybridManager.stopThreads();
		super.interrupt();
	}

	public void update(Object info) {
		if(info instanceof Vector<?>){
			Vector<HybridNetwork> networks = (Vector<HybridNetwork>) info;
			MyPhyloTree[] treeData = new MyPhyloTree[networks.size()];
			for(int i = 0; i< networks.size(); i++)
				treeData[i] = networks.get(i);
			controller.reportTrees(treeData);
		}
	}

}
