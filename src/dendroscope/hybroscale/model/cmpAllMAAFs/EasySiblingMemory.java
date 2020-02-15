/*
 *   EasySiblingMemory.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.cmpAllMAAFs;

import dendroscope.hybroscale.model.treeObjects.HybridTree;
import dendroscope.hybroscale.model.util.FirstBitComparator;

import java.util.*;

/**
 * @author Beckson
 */
public class EasySiblingMemory {

	private boolean print = true;

	private Vector<String> taxaOrdering;
	private Vector<String> taxonLabels;

	private HashMap<BitSet, HashSet<BitSet>> treeSetToForestSet = new HashMap<BitSet, HashSet<BitSet>>();
	private HashMap<String, Vector<String>> taxontToTaxa = new HashMap<String, Vector<String>>();

	// private BitSet treeSet;
	// private BitSet forestSet;

	@SuppressWarnings("unchecked")
	public EasySiblingMemory(Vector<String> taxaOrdering) {
		this.taxaOrdering = (Vector<String>) taxaOrdering.clone();
		this.taxonLabels = (Vector<String>) taxaOrdering.clone();

		for (String s : this.taxaOrdering) {
			Vector<String> v = new Vector<String>();
			v.add(s);
			taxontToTaxa.put(s, v);
		}
	}

	public EasySiblingMemory(EasyTree t, Vector<HybridTree> forest, Vector<String> taxaOrdering) {
		this.taxaOrdering = taxaOrdering;
		taxonLabels = new Vector<String>();
		for (EasyNode v : t.getLeaves())
			taxonLabels.add(v.getLabel());
	}

	public BitSet getForestSet(Vector<EasyTree> forest) {
		Vector<BitSet> forestSets = new Vector<BitSet>();
		for (EasyTree t : forest) {
			BitSet b = new BitSet(taxaOrdering.size());
			for (EasyNode v : t.getLeaves()) {
				if (taxontToTaxa.containsKey(v.getLabel())) {
					for (String s : taxontToTaxa.get(v.getLabel()))
						b.set(taxaOrdering.indexOf(s));
				} else
					b.set(taxaOrdering.indexOf(v.getLabel()));			
//				for (String s : splitTaxon(v.getLabel()))
//					b.set(taxaOrdering.indexOf(s));			
			}
			forestSets.add(b);
		}
		Collections.sort(forestSets, new FirstBitComparator());
		BitSet b = new BitSet(forestSets.size() * taxaOrdering.size());
		for (BitSet f : forestSets) {
			int bitIndex = f.nextSetBit(0);
			while (bitIndex != -1) {
				b.set(bitIndex + forestSets.indexOf(f) * taxaOrdering.size());
				bitIndex = f.nextSetBit(bitIndex + 1);
			}
		}
		return b;
	}

	private String[] splitTaxon(String taxon) {
		return taxon.split("\\+");
	}

	public BitSet getTreeSet(EasyTree t) {
		BitSet b = new BitSet(taxonLabels.size());
		for (EasyNode v : t.getLeaves()) {
			String label = v.getLabel();
			b.set(taxonLabels.indexOf(label));
		}
		return b;
	}

	public boolean contains(EasyTree t, Vector<EasyTree> forest) {

		BitSet treeSet = getTreeSet(t);
		BitSet forestSet = getForestSet(forest);

		if (treeSetToForestSet.containsKey(treeSet)) {
			for (BitSet b : treeSetToForestSet.get(treeSet)) {
				if (b.size() > forestSet.size()) {
					if (b.equals(forestSet))
						return true;
				} else if (forestSet.equals(b))
					return true;
			}
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	public void addEntry(EasyTree t, Vector<EasyTree> forest) {

		double maxMemory = java.lang.Runtime.getRuntime().maxMemory();
		double totalMemory = java.lang.Runtime.getRuntime().totalMemory();
		double usedMemory = totalMemory - java.lang.Runtime.getRuntime().freeMemory();

		if ((usedMemory / maxMemory) < 0.5) {
			BitSet treeSet = getTreeSet(t);
			BitSet forestSet = getForestSet(forest);

			if (treeSetToForestSet.containsKey(treeSet)) {
				HashSet<BitSet> set = (HashSet<BitSet>) treeSetToForestSet.get(treeSet).clone();
				set.add(forestSet);
				treeSetToForestSet.remove(treeSet);
				treeSetToForestSet.put(treeSet, set);
			} else {
				HashSet<BitSet> set = new HashSet<BitSet>();
				set.add(forestSet);
				treeSetToForestSet.put(treeSet, set);
			}
		}

	}

	public void addTreeLabel(String label) {
		if (!taxonLabels.contains(label))
			taxonLabels.add(label);
	}

	public void addTaxon(String taxon, Vector<String> taxa) {
		Vector<String> v = new Vector<String>();
		if (taxontToTaxa.containsKey(taxa.get(0))) {
			for (String s : taxontToTaxa.get(taxa.get(0)))
				v.add(s);
		} else
			v.add(taxa.get(0));
		if (taxontToTaxa.containsKey(taxa.get(1))) {
			for (String s : taxontToTaxa.get(taxa.get(1)))
				v.add(s);
		} else
			v.add(taxa.get(1));
		taxontToTaxa.put(taxon, v);
	}

	public boolean hasSameLeafSet(EasyTree t1, EasyTree t2) {
		if (t1.getLeaves().size() == t2.getLeaves().size()) {
			BitSet b1 = getTreeSet(t1);
			BitSet b2 = getTreeSet(t2);
			if (b1.equals(b2))
				return true;
		}
		return false;
	}

	public boolean compareTaxa(String s1, String s2) {
		if (taxonLabels.contains(s1) && taxonLabels.contains(s2)) {

			BitSet b1 = new BitSet(taxonLabels.size());
			b1.set(taxonLabels.indexOf(s1));

			BitSet b2 = new BitSet(taxonLabels.size());
			b2.set(taxonLabels.indexOf(s2));

			if (b1.equals(b2))
				return true;
		}
		return false;
	}

	public void freeMemory() {
		treeSetToForestSet = null;
		taxontToTaxa = null;
	}

}
