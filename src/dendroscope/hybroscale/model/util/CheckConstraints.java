/*
 *   CheckConstraints.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.util;

import dendroscope.hybroscale.model.reductionSteps.ReplacementInfo;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;

import java.util.Vector;

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
