/*
 * IsomorphismCheck.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * IsomorphismCheck.java Copyright (C) 2022
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dendroscope.algorithms.utils;

import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.io.IOException;
import java.util.*;

/**
 * Given two rooted, bifurcating phylogenetic trees T1 and T2, this function
 * checks whether T1 and T2 are isomorphic.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class IsomorphismCheck {
	public Boolean run(PhyloTree tree1, PhyloTree tree2) {

		try {

			PhyloTree t1 = new PhyloTree();
			t1.parseBracketNotation(tree1.toBracketString(), true);
			PhyloTree t2 = new PhyloTree();
			t2.parseBracketNotation(tree2.toBracketString(), true);

			if (!areSingleLabeledTreesWithSameTaxa(t1, t2))
				return false;

			var isBifurcatingTree1 = !t1.isReticulated() && t1.isBifurcating();
			var isBifurcatingTree2 = !t2.isReticulated() && t2.isBifurcating();

			if (isBifurcatingTree1 != isBifurcatingTree2)
				return false;

			if (t1.getNumberOfNodes() != t2.getNumberOfNodes())
				return false;

			if (isBifurcatingTree1)
				return treeIsomorphism(t1, t2);
			else
				return (networkIsomorphism(t1, t2));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}

	private boolean networkIsomorphism(PhyloTree t1, PhyloTree t2) {
		String s1 = getIsoString(t1);
		String s2 = getIsoString(t2);
		return s1.equals(s2);
	}

	public String getIsoString(PhyloTree tree) {

		Vector<String> idString = new Vector<>();
		Hashtable<String, Node> idToNode = new Hashtable<>();
		Vector<Node> idNodes = new Vector<>();

		Vector<String> idSet = new Vector<>();
		for (Node v : tree.computeSetOfLeaves()) {
			idSet.add(tree.getLabel(v));
			idToNode.put(tree.getLabel(v), v);
			idString.add("[" + tree.getLabel(v) + "]");
		}
		Collections.sort(idSet);
		for (String s : idSet)
			idNodes.add(idToNode.get(s));

		Vector<Node> visited = new Vector<>();
		int id = 1;
		while (!idNodes.isEmpty()) {
			Node v = idNodes.firstElement();
			visited.add(v);
			idNodes.remove(0);

			String newID = null;
			Vector<String> pIDs = new Vector<>();
			for (Node p : v.parents()) {
				if (tree.getLabel(p) == null && newID == null) {
					newID = String.valueOf(id);
					while (idSet.contains(newID)) {
						id++;
						newID = String.valueOf(id);
					}
					idSet.add(newID);
					tree.setLabel(p, newID);
				} else if (tree.getLabel(p) == null && newID != null)
					tree.setLabel(p, newID);
				pIDs.add(tree.getLabel(p));
				if (!visited.contains(p) && !idNodes.contains(p))
					idNodes.add(p);
			}
			if (!pIDs.isEmpty()) {
				Collections.sort(pIDs);
				idString.add(pIDs.toString());
			}
		}

		Collections.sort(idString);
		return idString.toString();

	}

	private boolean treeIsomorphism(PhyloTree t1, PhyloTree t2) {
		String s1 = getTreeString(t1);
		String s2 = getTreeString(t2);
		return s1.equals(s2);
	}

	public String getTreeString(PhyloTree t) {
		Set<Set<String>> clusters = dendroscope.util.PhyloTreeUtils.collectAllHardwiredClusters(t);
		Vector<String> treeString = new Vector<>();
		for (Set<String> o : clusters)
			treeString.add(o.toString());
		Collections.sort(treeString);
		return treeString.toString();
	}


	/**
	 * determines whether the two trees are single-labeled trees on the same
	 * taxon sets
	 *
	 * @return true, if ok
	 */
	public static boolean areSingleLabeledTreesWithSameTaxa(PhyloTree tree1, PhyloTree tree2) {
		var labels1 = new HashSet<String>();

		if (tree1.getNumberReticulateEdges() > 0 || tree2.getNumberReticulateEdges() > 0)
			return false;

		for (var v : tree1.nodes()) {
			if (v.getOutDegree() == 0) {
				if (labels1.contains(tree1.getLabel(v)))
					return false; // not single labeled
				else
					labels1.add(tree1.getLabel(v));
			}
		}

		var labels2 = new HashSet<String>();
		for (var v : tree2.nodes()) {
			if (v.getOutDegree() == 0) {
				if (!labels1.contains(tree2.getLabel(v)))
					return false; // not present in first tree
				if (labels2.contains(tree2.getLabel(v)))
					return false; // not single labeled
				else
					labels2.add(tree2.getLabel(v));
			}
		}
		return labels1.size() == labels2.size();
	}
}
