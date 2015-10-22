package dendroscope.hybroscale.model.util;

import java.util.Vector;

import dendroscope.hybroscale.model.reductionSteps.ReplacementInfo;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;

public class CheckConstraints {

	private boolean checkTime = false, checkAddTaxa = false, checkLevel = false;
	private boolean reportBestNetworks = false;

	private int bestTimeDegree = Integer.MAX_VALUE;

	private ReplacementInfo rI;
	private Vector<String> taxaOrdering;

	private String constraints;

	public CheckConstraints(String constraints, Vector<String> taxaOrdering, ReplacementInfo rI) {
		this.constraints = constraints;
		this.taxaOrdering = taxaOrdering;
		this.rI = rI;
		parseConstraints(constraints);
	}

	private void parseConstraints(String constraints) {
		for (String s : constraints.split("\n")) {
			if (s.contains("time")) {
				checkTime = true;
			} else if (s.contains("add-taxa")) {
				checkAddTaxa = true;
			} else if (s.contains("level-k")) {
				checkLevel = true;
			}
		}
	}

	public boolean forNegConstraints(SparseNetwork n) {
		return true;
	}

	public int estimateTimeConsistencyDegree(SparseNetwork n, int bestTimeDegree, boolean heuristicMode, int maxNodes) {
		if (checkTime)
			return new CheckTimeConsistency().run(n.getPhyloTree(), bestTimeDegree, heuristicMode, false, maxNodes);
		return Integer.MAX_VALUE;
	}
	
	public int estimateAddTaxaDegree(SparseNetwork n, int bestAddTaxaValue, boolean heuristicMode, int maxNodes) {
		if (checkAddTaxa)
			return new CheckAddTaxaEdge().run(n.getPhyloTree(), bestAddTaxaValue, false, true);
		return Integer.MAX_VALUE;
	}
	
	public int estimateLevelDegree(SparseNetwork n, int bestLevel) {
		if (checkLevel)
			return new CheckLevel().run(n.getPhyloTree(), bestLevel);
		return Integer.MAX_VALUE;
	}

	public String getNegBadConstraints() {
		String text = "";
		return text;
	}

	public void setCheckTime(boolean checkTime) {
		this.checkTime = checkTime;
	}

	public boolean doCheckTime() {
		return checkTime;
	}
	
	public boolean doCheckAddTaxa() {
		return checkAddTaxa;
	}
	
	public boolean doCheckLevel() {
		return checkLevel;
	}

	public boolean reportBestNetworks() {
		return reportBestNetworks;
	}

	public int getBestTimeDegree() {
		return bestTimeDegree;
	}

	public CheckConstraints copy() {
		return new CheckConstraints(constraints, taxaOrdering, rI);
	}

}
