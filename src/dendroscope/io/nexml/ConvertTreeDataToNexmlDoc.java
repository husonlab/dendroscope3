/*
 *   ConvertTreeDataToNexmlDoc.java Copyright (C) 2020 Daniel H. Huson
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
import jloda.graph.NodeArray;
import jloda.swing.graphview.EdgeView;
import jloda.swing.graphview.NodeView;
import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.Triplet;
import org.nexml.model.*;

import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * converts tree data to nexml document
 * Daniel Huson, 2.2011
 */
public class ConvertTreeDataToNexmlDoc {
    public static final URI NAMESPACE_URI;

    static {
        URI uri = null;
        try {
            uri = new URI("");
        } catch (URISyntaxException e) {
            Basic.caught(e);
        }
        NAMESPACE_URI = uri;
    }

    /**
     * convert the given tree data to Nexml format
     *
     * @param treeDataArray
     * @param connectors
     * @return nexml document
     * @throws ParserConfigurationException
     */
    public static Document apply(boolean internalNodeLabelsAreEdgeLabels, TreeData[] treeDataArray, Connectors connectors) throws ParserConfigurationException {
        final Document document = DocumentFactory.createDocument();
        final OTUs otus = document.createOTUs();
        final Map<String, OTU> label2otu = new HashMap<>();
        final TreeBlock treeBlock = document.createTreeBlock(otus);

        Map<String, Node> globalSrc2Target = new HashMap<>();

        for (TreeData treeData : treeDataArray) {
            Tree<FloatEdge> network = treeBlock.createFloatTree();
            network.setLabel(treeData.getName());
            NodeArray<Node> src2tarNode = new NodeArray<>(treeData);

            jloda.graph.Node root = treeData.getRoot();
            if (root == null) {
                for (jloda.graph.Node v = treeData.getFirstNode(); v != null; v = v.getNext()) {
                    if (v.getInDegree() == 0) {
                        root = v;
                        break;
                    }
                }
            }

            if (root != null) {
                NodeView defaultNodeFormat = null;
                EdgeView defaultEdgeFormat = null;
                if (treeData.hasAdditional()) {
                    if (treeData.getFirstNode() != null) {
                        defaultNodeFormat = treeData.getNV(treeData.getFirstNode());
                        network.addAnnotationValue("default_node_format", NAMESPACE_URI, defaultNodeFormat.toString(null, false, false));
                    }
                    if (treeData.getFirstEdge() != null) {
                        defaultEdgeFormat = treeData.getEV(treeData.getFirstEdge());
                        network.addAnnotationValue("default_edge_format", NAMESPACE_URI, defaultEdgeFormat.toString(null, false, false));
                    }
                }
                final BiFunction<jloda.graph.Node, String, OTU> getOtu = (v, s) -> {
                    if (!internalNodeLabelsAreEdgeLabels || v.getOutDegree() == 0 || !Basic.isDouble(s)) {
                        if (!label2otu.containsKey(s)) {
                            final OTU otu = otus.createOTU();
                            otu.setLabel(s);
                            label2otu.put(s, otu);
                        }
                        return label2otu.get(s);
                    } else
                        return null;
                };

                copyRec(getOtu, treeData, root, network, src2tarNode, defaultNodeFormat, defaultEdgeFormat);
            } else {
                System.err.println("Skipped unrooted tree or network: " + treeData.getName());
                continue;
            }

            if (connectors != null && connectors.getTreePair2ListOfNodePairs().size() > 0) {
                for (jloda.graph.Node v = treeData.getFirstNode(); v != null; v = v.getNext()) {
                    String name = treeData.getLabel(v);
                    if (name != null && name.length() > 0)
                        globalSrc2Target.put(treeData.getName() + " " + name, src2tarNode.getValue(v));
                }
            }

            Node p = src2tarNode.getValue(root);
            if (p != null) {
                p.setRoot(true);
                network.addAnnotationValue("root", NAMESPACE_URI, p.getId());
            }

            if (treeData.hasAdditional()) {
                network.addAnnotationValue("embellished", NAMESPACE_URI, Boolean.TRUE);
                network.addAnnotationValue("drawer", NAMESPACE_URI, treeData.getDrawerKind());
                network.addAnnotationValue("toscale", NAMESPACE_URI, treeData.isToScale());
                network.addAnnotationValue("sparselabels", NAMESPACE_URI, treeData.isSparseLabels());
                network.addAnnotationValue("radiallabels", NAMESPACE_URI, treeData.isRadialLabels());
                if (treeData.getCollapsedNodes() != null) {
                    StringBuilder buf = new StringBuilder();
                    boolean first = true;
                    for (jloda.graph.Node v : treeData.getCollapsedNodes()) {
                        if (first)
                            first = false;
                        else
                            buf.append(" ");
                        buf.append(src2tarNode.getValue(v).getId());
                    }
                    network.addAnnotationValue("collapsed", NAMESPACE_URI, buf.toString());
                }
                network.addAnnotationValue("trans", NAMESPACE_URI, treeData.getTrans().toString());
            }
            // save connectors here, if present:
            if (connectors != null && connectors.getTreePair2ListOfNodePairs().size() > 0) {
                TreeBlock metaTreeBlock = document.createTreeBlock(document.createOTUs());
                metaTreeBlock.setLabel("InterTreeConnectors");
                Tree<IntEdge> metaTree = metaTreeBlock.createIntTree();
                metaTree.setLabel("InterTreeConnectors");
                for (Pair<String, String> pair : connectors.getTreePair2ListOfNodePairs().keySet()) {
                    java.util.List<Triplet<String, String, Color>> list = connectors.getTreePair2ListOfNodePairs().get(pair);
                    for (Triplet<String, String, Color> one : list) {
                        Node refA = globalSrc2Target.get(pair.getFirst() + " " + one.getFirst());
                        Node refB = globalSrc2Target.get(pair.getSecond() + " " + one.getSecond());
                        if (refA != null && refB != null) {
                            String first = refA.getId();
                            String second = refB.getId();
                            Color color = one.getThird();
                            if (first != null && second != null) {
                                Node a = metaTree.createNode();
                                a.setLabel(first);
                                Node b = metaTree.createNode();
                                b.setLabel(second);
                                Edge e = metaTree.createEdge(a, b);
                                e.setLabel("" + color.getRGB());
                            }
                        }
                    }
                }
            }

        }
        return document;
    }

    /**
     * recursively copy src to target
     *
     * @param treeData
     * @param v
     * @param network
     * @param src2tarNode
     */
    private static void copyRec(BiFunction<jloda.graph.Node, String, OTU> getOTU, TreeData treeData, jloda.graph.Node v, Network network, NodeArray<Node> src2tarNode, NodeView defaultNodeView, EdgeView defaultEdgeView) {
        if (src2tarNode.getValue(v) == null) {
            Node p = network.createNode();
            src2tarNode.put(v, p);
            String label = treeData.getLabel(v);
            if (label != null) {
                p.setLabel(label);
                final OTU otu = getOTU.apply(v, label);
                if (otu != null)
                    p.setOTU(otu);
            }
            if (treeData.hasAdditional()) {
                p.addAnnotationValue("format", NAMESPACE_URI, treeData.getNV(v).toString(defaultNodeView, true, false));
            }
            for (jloda.graph.Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                copyRec(getOTU, treeData, e.getTarget(), network, src2tarNode, defaultNodeView, defaultEdgeView);
            }
            for (jloda.graph.Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node q = src2tarNode.getValue(e.getTarget());
                Edge edge = network.createEdge(p, q);
                label = treeData.getLabel(e);
                if (label != null)
                    edge.setLabel(label);
                edge.setLength(treeData.getWeight(e));
                if (treeData.isSpecial(e)) {
                    edge.addAnnotationValue("special", NAMESPACE_URI, true);
                }
                if (treeData.hasAdditional())
                    edge.addAnnotationValue("format", NAMESPACE_URI, treeData.getEV(e).toString(defaultEdgeView, true, false));
            }
        }
    }
}
