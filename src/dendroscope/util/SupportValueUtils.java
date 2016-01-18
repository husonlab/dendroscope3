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
import jloda.graph.EdgeArray;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;

/**
 * Utils for support values
 * Daniel Huson, 1.2016
 */
public class SupportValueUtils {
    /**
     *does this tree have internal node labels
     * @param tree
     * @return true, if some internal nodes have labels
     */
    public static String getInternalNodeLabelIfPresent(PhyloTree tree) {
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            if (v.getInDegree() > 0 && v.getOutDegree() > 0) { // is internal node and not root
                String label = tree.getLabel(v);
                if (label != null)
                    return label;
            }
        }
        return null;
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
     * set edge labels from internal node labels
     * @param tree
     * @return edge labels
     */
    public static EdgeArray<String> setEgeLabelsFromInternalNodeLabels(PhyloTree tree) {
        final EdgeArray<String> edgeLabels = new EdgeArray<>(tree);
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            if (v.getInDegree() == 1 && v.getOutDegree() > 0) {
                edgeLabels.set(v.getFirstInEdge(), tree.getLabel(v));
            }
        }
        return edgeLabels;
    }

    /**
     * set internal node labels from edge labels
     * @param tree
     */
    public static void setInternalNodeLabelsFromEdgeLabels(PhyloTree tree, EdgeArray<String> edgeLabels) {
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            if (v.getInDegree() == 1 && v.getOutDegree() > 0) {
                final Edge e = v.getFirstInEdge();
                tree.setLabel(v, edgeLabels.get(e));
            } else if (v.getInDegree() == 0) // root node
                tree.setLabel(v, null);
        }
    }
}
