/*
 * MultiLabeledTreeProcessor.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.multnet;

import dendroscope.core.TreeData;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.swing.graphview.PhyloGraphView;
import jloda.util.NumberUtils;

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
                    if (NumberUtils.isInteger(suffix) && NumberUtils.parseInt(suffix) > 0) {
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
                                if (NumberUtils.isInteger(suffix)) {
                                    if (NumberUtils.parseInt(suffix) > 1) {
                                        toDelete.add(v);
                                    } else if (NumberUtils.parseInt(suffix) == 1) {
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

                mulTree.getLSAChildrenMap().clear();
            }

        }

        return mulTree;
    }
}
