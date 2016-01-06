/**
 * SupportValueUtils.java
 * Copyright (C) 2015 Daniel H. Huson
 * <p/>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dendroscope.util;

import dendroscope.core.TreeData;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;

/**
 * Utils for support values
 * Daniel Huson, 1.2016
 */
public class SupportValueUtils {
    /**
     * are all internal nodes labeled by numbers?
     * @param tree
     * @return true, if all internal nodes labeled by numbers
     */
    public static boolean isInternalNodesLabeledByNumbers(PhyloTree tree) {
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            if (v.getInDegree() > 0 && v.getOutDegree() > 0) { // is internal node and not root
                String label = tree.getLabel(v);
                if (label == null || !Basic.isFloat(label))
                    return false;
            }
        }
        return true;
    }

    /**
     * delete all internal nodes
     * @param trees
     */
    public static void deleteAllInternalNodes(TreeData[] trees) {
        for (PhyloTree tree : trees) {
            for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
                if (v.getInDegree() > 0 && v.getOutDegree() > 0) {
                    tree.setLabel(v, null);
                }
            }
        }
    }

    /**
     * parse node labels and interpret as edge support values
     * @param tree
     */
    public static void setEdgeConfidencesFromNodeLabels(PhyloTree tree) {
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            if (v.getInDegree() == 1) {
                final Edge e = v.getFirstInEdge();
                if (v.getOutDegree() == 0) {
                    tree.setConfidence(e, 100.0);
                } else
                    tree.setConfidence(e, Basic.parseDouble(tree.getLabel(v)));
            }
        }
    }

    /**
     * set internal node labels as edge support
     * @param tree
     */
    public static void setNodeLabelsFromEdgeConfidences(PhyloTree tree) {
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            if (v.getInDegree() == 1) {
                final Edge e = v.getFirstInEdge();
                if (v.getOutDegree() > 0) {
                    tree.setLabel(v, "" + tree.getConfidence(e));
                }
            } else if (v.getInDegree() == 0) // root node
                tree.setLabel(v, null);
        }
    }
}
