package dendroscope.hybroscale.model.cmpMinNetworks;

import dendroscope.hybroscale.model.HybridManager.Computation;
import dendroscope.hybroscale.model.treeObjects.HybridNetwork;
import dendroscope.hybroscale.model.treeObjects.SparseNetNode;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.util.graph.MyNode;

public class CountRetNumber {

	public int getNumber(HybridNetwork n, Computation compValue) {
		if(compValue.equals(Computation.EDGE_NETWORK) || compValue.equals(Computation.EDGE_NUMBER))
			return getReticulationNumber(n);
		return getHybridizationNumber(n);
	}
	
	public int getNumber(SparseNetwork n, Computation compValue) {
		if(compValue.equals(Computation.EDGE_NETWORK) || compValue.equals(Computation.EDGE_NUMBER))
			return getReticulationNumber(n);
		return getHybridizationNumber(n);
	}
	
	public int getHybridizationNumber(SparseNetwork n) {
		int hNumber = 0;
		for(SparseNetNode v : n.getNodes()){
			if(v.getInDegree() > 1)
				hNumber++;
		}
		return hNumber;
	}
	
	public int getHybridizationNumber(HybridNetwork h) {
		int hNumber = 0;
		for(MyNode v : h.getNodes()){
			if(v.getInDegree() > 1)
				hNumber++;
		}
		return hNumber;
	}
	
	public int getReticulationNumber(SparseNetwork n) {
		int retNumber = computeWithModeTwo(n);
		return retNumber;
	}

	public int getReticulationNumber(HybridNetwork h) {
		int retNumber = computeWithModeTwo(new SparseNetwork(h));
		return retNumber;
	}

	private int computeWithModeTwo(SparseNetwork n) {
		int counter = 0;
		for (SparseNetNode v : n.getNodes()) {
			if (v.getInDegree() >= 2)
				counter += v.getInDegree() - 1;
		}
		return counter;
	}

}
