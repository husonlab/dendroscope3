/*
 *   ConvertNexmlDocToTreeData.java Copyright (C) 2020 Daniel H. Huson
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
package dendroscope.io.nexml;

import dendroscope.core.Connectors;
import dendroscope.core.TreeData;
import jloda.phylo.PhyloTree;
import jloda.swing.graphview.EdgeView;
import jloda.swing.graphview.NodeView;
import jloda.util.Basic;
import jloda.util.NumberUtils;
import jloda.util.Pair;
import org.nexml.model.*;

import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.*;

/**
 * converts a nexml document to tree data
 * Daniel Huson, 2.2011
 */
public class ConvertNexmlDocToTreeData {

    public static TreeData[] apply(Document document, Connectors connectors) {

        final List<TreeData> trees = new LinkedList<>();

        TreeBlock metaBlock = null;
        final Map<String, Pair<jloda.graph.Node, TreeData>> src2tarNode = new HashMap<>();

        for (TreeBlock treeBlock : document.getTreeBlockList()) {
            if (treeBlock.getLabel().equals("InterTreeConnectors")) {
                metaBlock = treeBlock;
                continue;
            }
            for (Network network : treeBlock) {
                // src2tarNode.clear();
                final Set<Node> nodes = network.getNodes();

                Node root = null;
                if (network instanceof Tree) {
                    root = ((Tree) network).getRoot();
                }
                if (root == null) {
                    String rootId = getFirstString(network.getAnnotationValues("root"));
                    if (rootId != null) {
                        for (Node p : nodes) {
                            if (p.getId().equals(rootId)) {
                                root = p;
                                break;
                            }
                        }
                    }
                }
                if (root == null) {
                    for (Node p : nodes) {
                        if (network.getInNodes(p).size() == 0) {
                            root = p;
                            break;
                        }
                    }
                }
                TreeData treeData = new TreeData();
                treeData.setName(network.getLabel());
                NodeView defaultNodeView = null;
                EdgeView defaultEdgeView = null;
                if (getFirstBoolean(network.getAnnotationValues("embellished"))) {
                    treeData.setupAdditional();
                    String nodeViewString = getFirstString(network.getAnnotationValues("default_node_format"));
                    if (nodeViewString != null) {
                        defaultNodeView = new NodeView();
                        try {
                            defaultNodeView.read(nodeViewString, null);
                        } catch (IOException e) {
                            Basic.caught(e);
                        }
                    }
                    String edgeViewString = getFirstString(network.getAnnotationValues("default_edge_format"));
                    if (edgeViewString != null) {
                        defaultEdgeView = new EdgeView();
                        try {
                            defaultEdgeView.read(edgeViewString, null);
                        } catch (IOException e) {
                            Basic.caught(e);
                        }
                    }
                }
                if (root != null)
                    copyRec((Network<Edge>) network, root, treeData, src2tarNode, defaultNodeView, defaultEdgeView);
                else {
                    System.err.println("Skipped unrooted tree or network: " + network.getLabel());
                    continue;
                }

                System.err.println(((PhyloTree) treeData.getRoot().getOwner()).toBracketString());

                if (treeData.hasAdditional()) {
                    treeData.setDrawerKind(getFirstString(network.getAnnotationValues("drawer")));
                    treeData.setToScale(getFirstBoolean(network.getAnnotationValues("toscale")));
                    treeData.setRadialLabels(getFirstBoolean(network.getAnnotationValues("radiallabels")));
                    treeData.setSparseLabels(getFirstBoolean(network.getAnnotationValues("sparselabels")));
                    String collapsed = getFirstString(network.getAnnotationValues("collapsed"));
                    if (collapsed != null) {
                        final Set<String> ids = new HashSet<>(Arrays.asList(collapsed.split(" ")));
                        for (Node p : nodes) {
                            if (ids.contains(p.getId())) {
                                jloda.graph.Node v = src2tarNode.get(p.getId()).getFirst();
                                if (v != null)
                                    treeData.getCollapsedNodes().add(v);
                            }
                        }

                    }
                    final String trans = getFirstString(network.getAnnotationValues("trans"));
                    if (trans != null) {
                        try {
                            treeData.getTrans().read(new StringReader(trans));
                        } catch (IOException e) {
                            Basic.caught(e);
                        }
                    }
                }

                trees.add(treeData);
            }
        }
        if (metaBlock != null) {
            for (Network metaTree : metaBlock) {
                Set<Edge> edges = metaTree.getEdges();
                for (Edge e : edges) {
                    Node p = e.getSource();
                    Node q = e.getTarget();
                    int rgb = Color.GRAY.getRGB();
                    String label = e.getLabel();
                    if (label != null)
                        rgb = NumberUtils.parseInt(e.getLabel());
                    Color color = new Color(rgb);
                    String nodeLabel1 = p.getLabel();
                    String nodeLabel2 = q.getLabel();
                    Pair<jloda.graph.Node, TreeData> pair1 = src2tarNode.get(nodeLabel1);
                    Pair<jloda.graph.Node, TreeData> pair2 = src2tarNode.get(nodeLabel2);
                    if (pair1 != null && pair2 != null) {
                        connectors.add(pair1.getSecond().getName(), pair2.getSecond().getName(),
                                pair1.getSecond().getLabel(pair1.getFirst()), pair2.getSecond().getLabel(pair2.getFirst()), color);
                    }

                }
            }
        }

        return trees.toArray(new TreeData[0]);
    }

    /**
     * recursively copy src to target
     *
     * @param network
     * @param p
     * @param treeData
     * @param src2tarNode
     */
    private static void copyRec(Network<Edge> network, Node p, TreeData treeData, Map<String, Pair<jloda.graph.Node, TreeData>> src2tarNode,
                                NodeView defaultNodeView, EdgeView defaultEdgeView) {
        if (src2tarNode.get(p.getId()) == null) {
            jloda.graph.Node v = treeData.newNode();
            if (treeData.getRoot() == null)
                treeData.setRoot(v);
            String label = p.getLabel().trim();
            treeData.setLabel(v, label.length() > 0 ? p.getLabel() : null);
            src2tarNode.put(p.getId(), new Pair<>(v, treeData));
            if (treeData.hasAdditional()) {
                String format = (getFirstString(p.getAnnotationValues("format")));
                if (format != null) {
                    try {
                        NodeView nv = new NodeView();
                        nv.read(format, defaultNodeView);
                        treeData.setNV(v, nv);
                    } catch (IOException ex) {
                        Basic.caught(ex);
                    }
                }
            }
            final Set<Node> nodes = new TreeSet<>((o1, o2) -> {
                int id1 = NumberUtils.parseInt(o1.getId());
                int id2 = NumberUtils.parseInt(o2.getId());
                return Integer.compare(id1, id2);
            });
            nodes.addAll(network.getOutNodes(p));
            for (Node q : nodes) {
                copyRec(network, q, treeData, src2tarNode, defaultNodeView, defaultEdgeView);
            }
            for (Node q : nodes) {
                final Edge edge = network.getEdge(p, q);
                jloda.graph.Node w = src2tarNode.get(q.getId()).getFirst();
                jloda.graph.Edge e = treeData.newEdge(v, w);
                String eLabel = edge.getLabel().trim();
                treeData.setLabel(e, eLabel.length() > 0 ? eLabel : null);
                if (edge.getLength() != null)
                    treeData.setWeight(e, edge.getLength().doubleValue());
                if (getFirstBoolean(edge.getAnnotationValues("special")))
                    treeData.setSpecial(e, true);
                if (treeData.hasAdditional()) {
                    String format = (getFirstString(edge.getAnnotationValues("format")));
                    if (format != null) {
                        try {
                            EdgeView ev = new EdgeView();
                            ev.read(format, defaultEdgeView);
                            treeData.setEV(e, ev);
                        } catch (IOException ex) {
                            Basic.caught(ex);
                        }
                    }
                }
            }
        }
    }

    /**
     * get the first boolean value found, or false
     *
     * @param values
     * @return first boolean value found
     */
    public static boolean getFirstBoolean(Set<Object> values) {
        if (values != null) {
            for (Object obj : values) {
                if (obj instanceof Boolean)
                    return (Boolean) obj;
            }
        }
        return false;
    }

    /**
     * get the first string found, or null
     *
     * @param values
     * @return first string found
     */
    public static String getFirstString(Set<Object> values) {
        if (values != null) {
            for (Object obj : values) {
                if (obj instanceof String)
                    return (String) obj;
            }
        }
        return null;
    }
}
