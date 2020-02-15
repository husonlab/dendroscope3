/*
 *   ReattachNetworks.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.attachNetworks;

import dendroscope.hybroscale.model.HybridManager;
import dendroscope.hybroscale.model.parallelization.MyNetPriorThreadPool;
import dendroscope.hybroscale.model.reductionSteps.ReplacementInfo;
import dendroscope.hybroscale.model.treeObjects.HybridNetwork;
import dendroscope.hybroscale.util.graph.MyPhyloTree;
import dendroscope.hybroscale.view.HybridView;

import java.util.Vector;

public class ReattachNetworks extends Thread {

	private ReattachClustersPara rCpS;
	private ReattachClustersRec rCr;
	private boolean doClusterReduction;

	public ReattachNetworks(HybridNetwork n, ReplacementInfo rI, int numOfNets, int numOfShiftNets,
							int numOfInputTrees, Vector<HybridView> views, int cores, MyNetPriorThreadPool myThreadPool,
							Vector<String> taxaOrdering, String constraints, HybridManager hM, boolean verbose, boolean doClusterReduction, MyPhyloTree[] trees) {
		rCpS = new ReattachClustersPara(n, rI, numOfNets, numOfShiftNets, numOfInputTrees, views, cores, myThreadPool,
				taxaOrdering, constraints, hM, verbose, trees);
		rCr = new ReattachClustersRec(n, rI, numOfNets, numOfShiftNets, numOfInputTrees, views, cores, taxaOrdering,
				constraints, hM);
		this.doClusterReduction = doClusterReduction;
	}

	@Override
	public void run() {
		if(!doClusterReduction)
			rCr.run();
		else
			rCpS.run();
	}

	public Vector<MyPhyloTree> getNetworks() {
		if(!doClusterReduction)
			return rCr.getNetworks();
		return rCpS.getNetworks();
	}

	public void stopThread() {
		rCr.setStop(true);
		rCpS.setStop(true);
		interrupt();
	}

	public int getNumOfShiftNets() {
		if(!doClusterReduction)
			return rCr.getNumOfShiftNets();
		return rCpS.getNumOfShiftNets();
	}

}
