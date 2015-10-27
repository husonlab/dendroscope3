package dendroscope.hybroscale.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import dendroscope.hybroscale.model.HybridManager.Computation;
import dendroscope.hybroscale.model.cmpMinNetworks.DFSManager;
import dendroscope.hybroscale.model.parallelization.MyNetPriorThreadPool;
import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.model.util.CheckConstraints;
import dendroscope.hybroscale.util.graph.MyNode;

public class ForestThread extends Thread implements Runnable {

	private int myPriority;
	private boolean stop = false;

	private MyNetPriorThreadPool netExec;
	private ExhaustiveSearch exSearch;
	private HybridTree[] trees;
	int edgeChoice;
	private Computation compValue;
	private ConcurrentHashMap<Integer, Vector<SparseNetwork>> edgesToMAAFs;
	private DFSManager dfsManager;
	private int calls = 0;
	private Vector<String> taxaOrdering;
	private boolean speedUp, verbose = false;
	private CheckConstraints checker;

	public ForestThread(ExhaustiveSearch exSearch, HybridTree[] trees, int edgeChoice, Computation compValue,
			MyNetPriorThreadPool netExec, Vector<String> taxaOrdering, boolean speedUp, CheckConstraints checker, boolean verbose) {
		this.exSearch = exSearch;
		this.trees = trees;
		this.edgeChoice = edgeChoice;
		this.compValue = compValue;
		this.netExec = netExec;
		this.taxaOrdering = taxaOrdering;
		this.speedUp = speedUp;
		this.checker = checker;
		this.verbose = verbose;
	}

	public void run() {
		if (!stop) {
			try {
				
//				System.out.println("++++ Forest "+edgeChoice+" "+this);
				dfsManager = new DFSManager(trees, edgeChoice, compValue, netExec, taxaOrdering, speedUp, checker, verbose);
				edgesToMAAFs = (ConcurrentHashMap<Integer, Vector<SparseNetwork>>) dfsManager.run();
				calls = dfsManager.getCalls();

				exSearch.reportReticulationResult(edgesToMAAFs, edgeChoice, this, calls);			
				exSearch.reportFinishing(this);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
	}
	
	public int getResult(){
		if(edgesToMAAFs == null)
			return -1;
		if(edgesToMAAFs.size() == 0)
			return 0;
		return 1;
	}

	public int getMyPriority() {
		return myPriority;
	}

	public void setMyPriority(int myPriority) {
		this.myPriority = myPriority;
	}

	public void stopThread() {
		stop = true;
		if (dfsManager != null) {
			reportCurrentResult();
			dfsManager.stopManager();
		}
	}

	private void reportCurrentResult() {
		edgesToMAAFs = dfsManager.getNumberToNetworks();
		calls = dfsManager.getCalls();
		if (edgesToMAAFs != null)
				exSearch.reportReticulationResult(edgesToMAAFs, edgeChoice, this, calls);
	}

	private void printMAAFs(ConcurrentHashMap<Integer, HashSet<Vector<HybridTree>>> numberToForests) {
		HashSet<Vector<HybridTree>> forests = numberToForests.get(5);
		if (forests != null) {
			Vector<String> MAAFs = new Vector<String>();
			for (Vector<HybridTree> maaf : forests) {
				Vector<String> MAAF = new Vector<String>();
				for (HybridTree comp : maaf) {
					Vector<String> c = new Vector<String>();
					for (MyNode v : comp.getLeaves())
						c.add(comp.getLabel(v));
					Collections.sort(c);
					MAAF.add(c.toString());
				}
				Collections.sort(MAAF);
				MAAFs.add(MAAF.toString());
			}
			Collections.sort(MAAFs);
			for (String s : MAAFs)
				System.out.println(MAAFs.indexOf(s) + ": " + s);
		}
	}

	public Integer getCurrentNumberOfNetworks() {
		return dfsManager != null ? this.dfsManager.getCurrentNumberOfNetworks() : null;
	}
	
	public void freeMemory(){
		dfsManager.freeMemory();
		dfsManager = null;
		taxaOrdering = null;
		edgesToMAAFs = null;
	}

}
