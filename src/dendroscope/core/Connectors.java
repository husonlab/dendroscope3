/*
 *   Connectors.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.core;

import dendroscope.window.Connector;
import dendroscope.window.TreeGrid;
import dendroscope.window.TreeViewer;
import jloda.graph.Node;
import jloda.util.Pair;
import jloda.util.Triplet;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * keeps track of inter-tree connectors
 * Daniel Huson, 2.2011
 */
public class Connectors {
    private final Map<Pair<String, String>, List<Triplet<String, String, Color>>> treePair2ListOfNodePairs = new HashMap<Pair<String, String>, List<Triplet<String, String, Color>>>();

    /**
     * sync all connectors from tree grid
     *
	 */
    public void syncCurrentViewersToDocument(TreeGrid treeGrid) {
        for (int i = 0; i < treeGrid.getNumberOfPanels(); i++) {
            String treeName1 = treeGrid.getViewerByRank(i).getName();
            if (treeName1 != null) {
                for (int j = i + 1; j < treeGrid.getNumberOfPanels(); j++) {
                    String treeName2 = treeGrid.getViewerByRank(j).getName();
                    if (treeName2 != null) {
                        treePair2ListOfNodePairs.remove(new Pair<String, String>(treeName1, treeName2));
                    }
                }
            }
        }

        int count = 0;
        for (Connector connector : treeGrid.getConnectors()) {
            if (connector.isValid()) {
				String treeName1 = connector.getTreeViewer1().getName();
				String treeName2 = connector.getTreeViewer2().getName();
				String nodeName1 = connector.getTreeViewer1().getLabel(connector.getV1());
				String nodeName2 = connector.getTreeViewer2().getLabel(connector.getV2());
				Pair<String, String> key = new Pair<String, String>(treeName1, treeName2);
				List<Triplet<String, String, Color>> list = treePair2ListOfNodePairs.computeIfAbsent(key, k -> new LinkedList<Triplet<String, String, Color>>());
				list.add(new Triplet<String, String, Color>(nodeName1, nodeName2, connector.getColor()));
				count++;
			}
        }

    }

    /**
     * sync all connectors from document to tree grid
     *
	 */
    public void syncDocumentToCurrentViewers(TreeGrid treeGrid) {
        treeGrid.getConnectors().clear();

        int count = 0;
        if (treePair2ListOfNodePairs.size() > 0) {
            for (int i = 0; i < treeGrid.getNumberOfPanels(); i++) {
                String treeName1 = treeGrid.getViewerByRank(i).getName();
                TreeViewer treeViewer1 = treeGrid.getViewerByRank(i);
                for (int j = i + 1; j < treeGrid.getNumberOfPanels(); j++) {
                    String treeName2 = treeGrid.getViewerByRank(j).getName();
                    TreeViewer treeViewer2 = treeGrid.getViewerByRank(j);
                    List<Triplet<String, String, Color>> list = treePair2ListOfNodePairs.get(new Pair<String, String>(treeName1, treeName2));
                    if (list != null) {
                        Map<String, Node> name2node1 = treeViewer1.computeName2NodeMap();
                        Map<String, Node> name2node2 = treeViewer2.computeName2NodeMap();
                        for (Triplet<String, String, Color> one : list) {
                            Node v = name2node1.get(one.getFirst());
                            Node w = name2node2.get(one.getSecond());
                            if (v != null && w != null) {
                                treeGrid.getConnectors().add(new Connector(treeViewer1, v, treeViewer2, w, one.getThird()));
                                count++;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * access all connectors
     *
     * @return access
     */
    public Map<Pair<String, String>, List<Triplet<String, String, Color>>> getTreePair2ListOfNodePairs() {
        return treePair2ListOfNodePairs;
    }

    /**
     * add a connector
     *
	 */
    public void add(String treeName1, String treeName2, String nodeName1, String nodeName2, Color color) {
		Pair<String, String> key = new Pair<String, String>(treeName1, treeName2);
		List<Triplet<String, String, Color>> list = treePair2ListOfNodePairs.computeIfAbsent(key, k -> new LinkedList<Triplet<String, String, Color>>());
		list.add(new Triplet<String, String, Color>(nodeName1, nodeName2, color));
	}
}
