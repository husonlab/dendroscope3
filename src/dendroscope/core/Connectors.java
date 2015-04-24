/**
 * Copyright 2015, Daniel Huson
 *
 *(Some files contain contributions from other authors, who are then mentioned separately)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * @param treeGrid
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
                List<Triplet<String, String, Color>> list = treePair2ListOfNodePairs.get(key);
                if (list == null) {
                    list = new LinkedList<Triplet<String, String, Color>>();
                    treePair2ListOfNodePairs.put(key, list);
                }
                list.add(new Triplet<String, String, Color>(nodeName1, nodeName2, connector.getColor()));
                count++;
            }
        }

    }

    /**
     * sync all connectors from document to tree grid
     *
     * @param treeGrid
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
     * @param treeName1
     * @param treeName2
     * @param nodeName1
     * @param nodeName2
     * @param color
     */
    public void add(String treeName1, String treeName2, String nodeName1, String nodeName2, Color color) {
        Pair<String, String> key = new Pair<String, String>(treeName1, treeName2);
        List<Triplet<String, String, Color>> list = treePair2ListOfNodePairs.get(key);
        if (list == null) {
            list = new LinkedList<Triplet<String, String, Color>>();
            treePair2ListOfNodePairs.put(key, list);
        }
        list.add(new Triplet<String, String, Color>(nodeName1, nodeName2, color));
    }
}
