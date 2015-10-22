package dendroscope.hybroscale.controller;

import java.util.Vector;

import dendroscope.hybroscale.model.HybridManager;
import dendroscope.hybroscale.model.HybridManager.Computation;
import dendroscope.hybroscale.model.treeObjects.HybridNetwork;
import dendroscope.hybroscale.util.graph.MyPhyloTree;
import dendroscope.hybroscale.view.HybridView;


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
			for(int i = 0; i< networks.size();i++)
				treeData[i] = networks.get(i);
			controller.reportTrees(treeData);
		}
	}
	
}
