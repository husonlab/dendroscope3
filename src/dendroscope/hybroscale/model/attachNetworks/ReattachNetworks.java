package dendroscope.hybroscale.model.attachNetworks;

import java.util.Vector;

import dendroscope.hybroscale.model.HybridManager;
import dendroscope.hybroscale.model.parallelization.MyNetPriorThreadPool;
import dendroscope.hybroscale.model.reductionSteps.ReplacementInfo;
import dendroscope.hybroscale.model.treeObjects.HybridNetwork;
import dendroscope.hybroscale.util.graph.MyPhyloTree;
import dendroscope.hybroscale.view.HybridView;

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
