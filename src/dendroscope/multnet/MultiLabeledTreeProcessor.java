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

package dendroscope.multnet;

import dendroscope.core.TreeData;
import jloda.graph.Node;
import jloda.phylo.PhyloGraphView;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;

import java.util.HashSet;
import java.util.Set;

/**
 * processes a multilabeled tree
 * Daniel Huson, 10.2009
 */
public class MultiLabeledTreeProcessor {
    public static TreeData apply(TreeData tree, String method) {
        PhyloTree network = null;
        if (method.equalsIgnoreCase("cluster")) {
            ClusterNetFromMultree clusternetFromMultree = new ClusterNetFromMultree(tree);
            network = clusternetFromMultree.apply();
            network.setName(tree.getName() + "-multnet-cluster");

        } else if (method.equalsIgnoreCase("levelk")) {
            LevelkNetFromMultree levelKnetFromMultree = new LevelkNetFromMultree(tree);
            network = levelKnetFromMultree.apply();
            network.setName(tree.getName() + "-multnet-levelk");
        } else if (method.equalsIgnoreCase("exact") || method.equalsIgnoreCase("HOLM")) {
            ExactNetFromMultree multibuildnetFromMultree = new ExactNetFromMultree(tree);
            network = multibuildnetFromMultree.apply();
            network.setName(tree.getName() + "-multnet-exact");
        } else if (method.equalsIgnoreCase("contracted")) {
            network = collapseMultiLabeledTaxa(tree);
            network.setName(tree.getName() + "-contracted");
        }
        if (network != null)
            return new TreeData(network);
        else
            return null;
    }

    /**
     * collapse multi-labeled to labeled
     *
     * @param tree0
     * @return single labeled tree
     */
    private static PhyloTree collapseMultiLabeledTaxa(TreeData tree0) {
        MultilabeledTree mulTree = new MultilabeledTree();
        mulTree.copy(tree0);

        PhyloGraphView treeViewer = new PhyloGraphView(mulTree);

        for (Node v = mulTree.getFirstNode(); v != null; v = v.getNext()) {
            String label = mulTree.getLabel(v);
            if (label != null) {
                int pos = label.lastIndexOf(".") + 1;
                if (pos > 0) {
                    String suffix = label.substring(pos);
                    if (Basic.isInteger(suffix) && Basic.parseInt(suffix) > 0) {
                        treeViewer.setSelected(v, true);
                    }
                }
            }
        }

        if (treeViewer.getSelectedNodes().size() > 1) {
            dendroscope.util.PhyloTreeUtils.selectInducedSubNetwork(treeViewer, mulTree, treeViewer.getSelectedNodes(), null);
            if (treeViewer.contractAll(treeViewer.getSelectedEdges())) {
                treeViewer.selectedEdges.clear();
                treeViewer.selectedNodes.clear();
                Set<Node> toDelete = new HashSet<>();
                for (Node v = mulTree.getFirstNode(); v != null; v = v.getNext()) {
                    String label = mulTree.getLabel(v);
                    if (label != null) {
                        label = label.trim();
                        if (label.length() > 0) {
                            int pos = label.lastIndexOf(".") + 1;
                            if (pos > 0) {
                                String suffix = label.substring(pos);
                                if (Basic.isInteger(suffix)) {
                                    if (Basic.parseInt(suffix) > 1) {
                                        toDelete.add(v);
                                    } else if (Basic.parseInt(suffix) == 1) {
                                        treeViewer.setLabel(v, label.substring(0, pos - 1));
                                        mulTree.setLabel(v, treeViewer.getLabel(v));
                                    }
                                }
                            }
                        }
                    }
                }
                for (Node v : toDelete) {
                    mulTree.deleteNode(v);
                }

                mulTree.getNode2GuideTreeChildren().clear();
            }

        }

        return mulTree;
    }
}
