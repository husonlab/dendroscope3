/*
 *   NetworkMemory.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.cmpMinNetworks;

import dendroscope.hybroscale.model.treeObjects.SparseNetEdge;
import dendroscope.hybroscale.model.treeObjects.SparseNetNode;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;

import java.util.*;

public class NetworkMemory {

	private Vector<SparseNetwork> cachedNetworks = new Vector<SparseNetwork>();
	private HashMap<String, Vector<Integer>> treeIndicesToNetworkIndices = new HashMap<String, Vector<Integer>>();

	private Vector<String> taxaOrdering;

	private double netCounter = 0, bitCounter = 0, counter = 0;

	public NetworkMemory(Vector<String> taxaOrdering) {
		this.taxaOrdering = taxaOrdering;
	}

	public void addNetwork(SparseNetwork n, Vector<Integer> treeIndices) {

		try {
			if (storeNetwork() && cachedNetworks != null) {
				cachedNetworks.add(n);
				String indexID = getIndexID(treeIndices);
				if (!treeIndicesToNetworkIndices.containsKey(indexID))
					treeIndicesToNetworkIndices.put(indexID, new Vector<Integer>());
				treeIndicesToNetworkIndices.get(indexID).add(cachedNetworks.size() - 1);
			}
		} catch (Exception e) {

		}

	}

	private boolean storeNetwork() {

		double maxMemory = java.lang.Runtime.getRuntime().maxMemory();
		double totalMemory = java.lang.Runtime.getRuntime().totalMemory();
		double usedMemory = totalMemory - java.lang.Runtime.getRuntime().freeMemory();

		if ((usedMemory / maxMemory) < 0.5)
			return true;
		return false;

	}

	public boolean containsNetwork(SparseNetwork n, Vector<Integer> treeIndices) {

		try {
			String indexID = getIndexID(treeIndices);
			if (treeIndicesToNetworkIndices != null && treeIndicesToNetworkIndices.containsKey(indexID)) {
				Vector<Integer> networkPointer = (Vector<Integer>) treeIndicesToNetworkIndices.get(indexID).clone();
				for (int i : networkPointer) {

					counter++;
					SparseNetwork nCached = cachedNetworks.get(i);

					BitSet bN = (BitSet) n.getInfo();
					BitSet bCached = (BitSet) nCached.getInfo();

					if (bN.equals(bCached)) {
						bitCounter++;
						if (new NetworkIsomorphismCheck().run(n, nCached)) {
							netCounter++;
							return true;
						}
					}
				}
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	private String getIndexID(Vector<Integer> indices) {
		String indexID = "";
		for (int i : indices)
			indexID = indexID.concat(i + ":");
		return indexID;
	}

	public void freeMemory() {
		cachedNetworks = null;
		treeIndicesToNetworkIndices = null;
	}

	public BitSet getNetworkSet(SparseNetwork n) {

		Vector<SparseNetNode> retNodes = new Vector<SparseNetNode>();
		for (SparseNetNode v : n.getNodes()) {
			if (v.getInDegree() > 1)
				retNodes.add(v);
		}

		Vector<BitSet> leafSets = new Vector<BitSet>();
		for (SparseNetNode v : retNodes) {
			BitSet bV = new BitSet(taxaOrdering.size());
			cmpLeafSet(v, bV);
			leafSets.add(bV);
		}
		Collections.sort(leafSets, new BitSetComparator());

		BitSet netSet = new BitSet(leafSets.size() * taxaOrdering.size());
		for (int i = 0; i < leafSets.size(); i++) {
			for (int j = 0; j < taxaOrdering.size(); j++) {
				netSet.set((taxaOrdering.size() * i) + j, leafSets.get(i).get(j));
			}
		}

		return netSet;
	}

	private void cmpLeafSet(SparseNetNode v, BitSet bV) {
		if (v.getOutDegree() == 0)
			bV.set(taxaOrdering.indexOf(v.getLabel()));
		else {
			for (SparseNetEdge e : v.outEdges())
				cmpLeafSet(e.getTarget(), bV);
		}
	}

	public class BitSetComparator implements Comparator<BitSet> {

		@Override
		public int compare(BitSet b1, BitSet b2) {
			if (b1.equals(b2))
				return 0;
			BitSet xor = (BitSet) b1.clone();
			xor.xor(b2);
			int firstSetBit = xor.length() - 1;
			return b2.get(firstSetBit) ? 1 : -1;
		}

	}

	public void printStatistics() {
		System.out.println("Statstics: " + counter + " / " + bitCounter + " / " + netCounter);
		System.out.println("BitEfficency: " + (1. - (bitCounter / counter)));
	}

}
