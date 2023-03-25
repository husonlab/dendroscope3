/*
 *   Dendro.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.io;

import dendroscope.core.TreeData;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.swing.graphview.EdgeView;
import jloda.swing.graphview.GraphView;
import jloda.swing.graphview.NodeView;
import jloda.util.Basic;
import jloda.util.FileUtils;
import jloda.util.Pair;
import jloda.util.StringUtils;
import jloda.util.parse.NexusStreamParser;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * input and output of trees in dendro format
 * Daniel Huson, 7.2007
 */
public class Dendro extends IOBase implements IOFormat {
    public final static String DESCRIPTION = "Dendroscope File (*.den, *.dendro)";
    public final static String EXTENSION = ".dendro";
    public final static String NAME = "Dendro";

    private final static String TAG = "#DENDROSCOPE";

    /**
     * does this look like a file of the correct type?
     *
     * @return true, if correct type of file
     */
    public boolean isCorrectFileType(File file) {
        try {
			return isCorrectType(StringUtils.toString(FileUtils.getFirstBytesFromFile(file, TAG.length())));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * does this look like the first line of the a file of the correct type?
     *
     * @return true, if correct type of string
     */
    public boolean isCorrectType(String aLine) {
        return aLine != null && aLine.startsWith(TAG);
    }

    /**
     * read trees
     *
     * @return trees
     */
    public TreeData[] read(Reader r) throws IOException {
        List<TreeData> trees = new LinkedList<>();
        try {
            final NexusStreamParser np = new NexusStreamParser(r);
            np.matchRespectCase(TAG);
            while (np.peekNextToken() != NexusStreamParser.TT_EOF) {
                trees.add(read(np));
            }
        } catch (IOException ex) {
            Basic.caught(ex);
        } finally {
            if (r != null)
                r.close();
        }
        return trees.toArray(new TreeData[0]);
    }

    /**
     * read a single tree
     *
     * @return tree
     */
    private TreeData read(NexusStreamParser np) throws IOException {
        TreeData tree = new TreeData();
        Num2NodeArray num2node = null;
        Num2EdgeArray num2edge = null;

        if (np.peekMatchIgnoreCase("{GRAPH")) { // only for backward compatiblity. This format is no longer written
            num2node = new Num2NodeArray();
            num2edge = new Num2EdgeArray();
            tree.setName(createNewTreeName());
            String str = np.getTokensFileNamePunctuation(null, "}") + "}";
            GraphIO.read(new StringReader(str), tree, num2node, num2edge);
        }
        if (np.peekMatchIgnoreCase("{NETWORK")) {
            num2node = new Num2NodeArray();
            num2edge = new Num2EdgeArray();
            tree.setName(createNewTreeName());
            //String str = np.getTokensFileNamePunctuation(null, "}") + "}";
            //read(tree, new StringReader(str), num2node, num2edge);
            read(tree, np, num2node, num2edge);

        } else {
            np.matchRespectCase("{TREE");
            if (np.peekMatchIgnoreCase("("))
                tree.setName(createNewTreeName()); // backward compatibility: no name given
            else
                tree.setName(np.getWordRespectCase());

            String str = np.getTokensFileNamePunctuation(null, ";");
            tree.parseBracketNotation(str, true);
            np.matchRespectCase("}");
        }

        if (np.peekMatchRespectCase("{LSA")) {
            tree.setupLSA();
            np.matchRespectCase("{LSA");
            while (!np.peekMatchRespectCase("}")) {
                int vid = np.getInt(1, tree.getNumberOfNodes());
                Node v = num2node.get(vid);
                List<Node> order = new LinkedList<Node>();
				tree.getLSAChildrenMap().put(v, order);
				np.matchRespectCase(":");

                while (!np.peekMatchRespectCase(";")) {
                    int wid = np.getInt(1, tree.getNumberOfNodes());
                    Node w = num2node.get(wid);
                    order.add(w);
                }
                np.matchRespectCase(";");
            }
            np.matchRespectCase("}");
        } else
            tree.clearLSA();

        if (np.peekMatchRespectCase("{GRAPHVIEW") || np.peekMatchRespectCase("{JLODA.GRAPHVIEW")) {
            if (np.peekMatchRespectCase("{GRAPHVIEW"))
                np.matchRespectCase("{GRAPHVIEW");
            else
                np.matchRespectCase("{JLODA.GRAPHVIEW"); // for backward compatibility

            tree.setupAdditional(); // prepare for additional data

            if (num2node == null) // have read in a tree with annotations
            {
                num2node = new Num2NodeArray();
                num2edge = new Num2EdgeArray();
                setNum2NodeEdgeArray(tree.getRoot(), num2node, num2edge);
            }

            np.matchRespectCase("nnodes = " + tree.getNumberOfNodes() + " nedges = " + tree.getNumberOfEdges());

            np.matchRespectCase("nodes");
            NodeView prevNV = GraphView.defaultNodeView;
            while (!np.peekMatchRespectCase("edges")) {
                int vid = np.getInt(1, tree.getNumberOfNodes());
                Node v = num2node.get(vid);
                NodeView nv = new NodeView();
                nv.read(np, np.getTokensRespectCase(":", ";"), prevNV);
                tree.setNV(v, nv);
                tree.setLabel(v, nv.getLabel());
                v.setInfo(nv.getLabel());
                prevNV = nv;
            }

            np.matchRespectCase("edges");
            EdgeView prevEV = GraphView.defaultEdgeView;
            while (!np.peekMatchRespectCase("}")) {
                int eid = np.getInt(1, tree.getNumberOfEdges());
                Edge e = num2edge.get(eid);
                EdgeView ev = new EdgeView();
                ev.read(np, np.getTokensRespectCase(":", ";"), prevEV);
                tree.setEV(e, ev);
                tree.setLabel(e, ev.getLabel());
                e.setInfo(ev.getLabel());
                prevEV = ev;
            }
            np.matchRespectCase("}");

            if (np.peekMatchRespectCase("{DENDRO") || np.peekMatchRespectCase("{DENDROSCOPE")) {
                if (np.peekMatchRespectCase("{DENDRO"))
                    np.matchRespectCase("{DENDRO");
                else
                    np.matchRespectCase("{DENDROSCOPE");
                // match dendroscope for backward compatibility

                if (np.peekMatchRespectCase("root")) {
                    np.matchRespectCase("root=");
                    tree.setRoot(num2node.get(np.getInt(1, tree.getNumberOfNodes())));
                }
                if (np.peekMatchRespectCase("drawer")) {
                    np.matchRespectCase("drawer=");
                    tree.setDrawerKind(np.getLabelRespectCase());
                }
                if (np.peekMatchRespectCase("toscale")) {
                    np.matchRespectCase("toscale=");
                    tree.setToScale(np.getBoolean());
                }
                // for backward compatibility:
                if (np.peekMatchRespectCase("rotatelabels")) {
                    np.matchRespectCase("rotatelabels=");
                    tree.setRadialLabels(np.getBoolean());
                }
                if (np.peekMatchRespectCase("radiallabels")) {
                    np.matchRespectCase("radiallabels=");
                    tree.setRadialLabels(np.getBoolean());
                }

                if (np.peekMatchIgnoreCase("collapsed")) {
                    np.matchRespectCase("collapsed=");
                    int n = np.getInt(0, tree.getNumberOfNodes());
                    for (int i = 1; i <= n; i++) {
                        int num = np.getInt(1, tree.getNumberOfNodes());
                        tree.getCollapsedNodes().add(num2node.get(num));
                    }
                }
                if (np.peekMatchIgnoreCase("trans")) {
                    np.matchRespectCase("trans=");
                    tree.getTrans().read(np);
                }
                np.matchRespectCase("}");
            }
        }

        return tree;
    }


    /**
     * write trees
     *
	 */
    public void write(Writer w0, boolean internalNodeLabelsAreEdgeLabels, TreeData[] trees) throws IOException {

        if (trees != null) {
            try (BufferedWriter w = new BufferedWriter(w0)) {
                w.write("#DENDROSCOPE\n");
                for (TreeData tree : trees) {
                    write(w, tree);
                }
            }
        }
    }

    /**
     * write a single tree
     *
	 */
    private void write(BufferedWriter w, TreeData tree) throws IOException {
		var nodeId2Number = new HashMap<Integer, Integer>();
		var edgeId2Number = new HashMap<Integer, Integer>();

		if (tree.getNumberReticulateEdges() > 0) {
			write(tree, w, nodeId2Number, edgeId2Number); // write as network
		} else {
			w.write("{TREE '" + tree.getName() + "'\n");

			tree.write(w, new PhyloTree.NewickOutputFormat(true, true, false, false, false), nodeId2Number, edgeId2Number);
			w.write(";\n");
			w.write("}\n");
		}

        if (tree.getNumberReticulateEdges() > 0 && !tree.getLSAChildrenMap().isEmpty()) {
            w.write("{LSA\n");
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                List<Node> order = tree.getLSAChildrenMap().get(v);
                if (order != null && order.size() > 0) {
                    w.write(nodeId2Number.get(v.getId()) + ":");
                    for (Node u : order) {
                        w.write(" " + nodeId2Number.get(u.getId()));
                    }
                    w.write(";\n");
                }
            }
            w.write("}\n");
        }

        if (tree.hasAdditional()) {
            w.write("{GRAPHVIEW\n");
            w.write("nnodes=" + tree.getNumberOfNodes() + " nedges=" + tree.getNumberOfEdges() + "\n");
            w.write("nodes\n");
            NodeView prevNV = null;
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                w.write(nodeId2Number.get(v.getId()) + ":");
                tree.getNV(v).write(w, prevNV);
                prevNV = tree.getNV(v);
            }
            w.write("edges\n");
            EdgeView prevEV = null;
            for (Edge e = tree.getFirstEdge(); e != null; e = e.getNext()) {
                w.write((edgeId2Number.get(e.getId())) + ":");
                tree.getEV(e).write(w, prevEV);
                prevEV = tree.getEV(e);
            }
            w.write("}\n");

            w.write("{DENDRO\n");
            if (tree.getRoot() != null)
                w.write("root=" + nodeId2Number.get(tree.getRoot().getId()) + "\n");

            w.write("drawer=" + tree.getDrawerKind() + "\n");
            w.write("toscale=" + tree.isToScale() + "\n");
            w.write("radiallabels=" + tree.isRadialLabels() + "\n");
            if (tree.getCollapsedNodes() != null) {
                w.write("collapsed=" + tree.getCollapsedNodes().size());
                for (Node v : tree.getCollapsedNodes()) {
                    w.write(" " + v.getId());
                }
            } else
                w.write("collapsed=0");
            w.write("\ntrans=" + tree.getTrans().toString() + "\n");
            w.write("}\n");
        }
        w.flush();
    }

    /**
     * do we accept this file?
     *
     * @return true, if correct ending
     */
    public boolean accept(File file) {
        if (file != null) {
            if (file.isDirectory()) return true;
            // Get the file extension
            try {
                String extension = getExtension(file);
                if (extension != null)
                    if (extension.equalsIgnoreCase("den")
                            || extension.equalsIgnoreCase("dendro"))
                        return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * gets a description of the file type
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * gets the default file extension of this format
     *
     * @return extension
     */
    public String getExtension() {
        return EXTENSION;
    }

    /**
     * gets the format name
     *
     * @return name
     */
    public String getName() {
        return NAME;
    }

    /**
     * writes a graph in jloda format.  Node-id to node-number and edge-id to edge-number maps are set.
     *
     * @param nodeId2Number after write, maps node-ids to numbers 1..numberOfNodes
     * @param edgeId2Number after write, maps edge-ids to numbers 1..numberOfEdge
     */
    private void write(PhyloTree tree, Writer w, Map<Integer, Integer> nodeId2Number, Map<Integer, Integer> edgeId2Number) throws IOException {
        w.write("{NETWORK\n");
        w.write("nnodes=" + tree.getNumberOfNodes() + " nedges=" + tree.getNumberOfEdges() + "\n");
        w.write("node.labels\n");
        int count = 0;
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            nodeId2Number.put(v.getId(), ++count);
            Object info = tree.getLabel(v);
            if (info == null)
                info = v.getInfo();
            if (info != null) {
                String string = info.toString();
                if (string != null && string.length() > 0) {
                    w.write("" + count + ":'" + string + "'");
                    if (!(info instanceof String))
                        w.write(" [" + info.getClass().getName() + "]");
                    w.write("\n");
                }
            }
        }
        w.write("edges\n");
        count = 0;
        for (Edge e = tree.getFirstEdge(); e != null; e = e.getNext()) {
            edgeId2Number.put(e.getId(), ++count);
            w.write("" + count + ":" + nodeId2Number.get(e.getSource().getId()) + " " +
                    nodeId2Number.get(e.getTarget().getId()) + " " + tree.getWeight(e));
			if (tree.isReticulateEdge(e))
				w.write(" s");
            w.write("\n");
        }
        w.write("edge.labels\n");
        for (Edge e = tree.getFirstEdge(); e != null; e = e.getNext()) {
            Object info = tree.getLabel(e);
            if (info == null)
                info = e.getInfo();

            if (info != null) {
                String string = info.toString();
                if (string != null && string.length() > 0) {
                    w.write("" + edgeId2Number.get(e.getId()) + ":'" + string + "'");
                    if (!(info instanceof String))
                        w.write(" [" + info.getClass().getName() + "]");
                    w.write("\n");
                }
            }
        }
        w.write("}\n");
    }

    /**
     * reads a graph in jloda format. Sets node-num to node map and edge-num edge map
     *
     * @param num2node after read, contains mapping of numbers used in file to nodes created in Graph
     * @param num2edge after read, contains mapping of numbers used in file to edges created in Graph
     */
    private void read(PhyloTree tree, NexusStreamParser np, Num2NodeArray num2node, Num2EdgeArray num2edge) throws IOException {
        tree.clear();
        np.matchRespectCase("{NETWORK\n");
        np.matchRespectCase("nnodes=");
        int nNodes = np.getInt(0, 10000000);

        num2node.set(new Node[nNodes + 1]);
        for (int i = 1; i <= nNodes; i++) {
            num2node.put(i, tree.newNode());
        }

        np.matchRespectCase("nedges=");
        int nEdges = np.getInt(0, 10000000);
        num2edge.set(new Edge[nEdges + 1]);

        if (np.peekMatchRespectCase("node.labels"))
            np.matchRespectCase("node.labels");

        while (!np.peekMatchRespectCase("edges")) {
            int vid = np.getInt(1, nNodes);
            np.matchRespectCase(":");
            Node u = num2node.get(vid);
            String label = np.getLabelRespectCase();
            u.setInfo(label);
            tree.setLabel(u, label);
        }

        np.matchRespectCase("edges");
        for (int i = 1; i <= nEdges; i++) {
            int eid = np.getInt(1, nEdges);
            np.matchRespectCase(":");
            Node source = num2node.get(np.getInt(1, nNodes));
            Node target = num2node.get(np.getInt(1, nNodes));
            Edge e = tree.newEdge(source, target);
            tree.setWeight(e, np.getDouble());
            num2edge.put(eid, e);
            if (np.peekMatchIgnoreCase("s")) {
				np.matchIgnoreCase("s");
				tree.setReticulate(e, true);
            }
        }

        if (np.peekMatchRespectCase("edge.labels")) {
            np.matchRespectCase("edge.labels");
            while (!np.peekMatchRespectCase("}")) {
                int eid = np.getInt(1, nEdges);
                np.matchRespectCase(":");
                Edge f = num2edge.get(eid);
                String label = np.getLabelRespectCase();
                tree.setLabel(f, label);
                f.setInfo(label);
            }
        }
        np.matchRespectCase("}");
    }

    /**
     * sets the number 2 node and number 2 edge maps
     *
	 */
    private static void setNum2NodeEdgeArray(Node root, Num2NodeArray num2node, Num2EdgeArray num2edge) {
        num2node.clear(root.getOwner().getNumberOfNodes());
        num2edge.clear(root.getOwner().getNumberOfEdges());
        setNum2NodeEdgeArrayRec(root, null, new Pair<>(0, 0), num2node, num2edge);
    }

    /**
     * recursively do the work
     *
	 */
    private static void setNum2NodeEdgeArrayRec(Node v, Edge e, Pair<Integer, Integer> nodeNumberEdgeNumber, Num2NodeArray num2node, Num2EdgeArray num2edge) {
        var nodes = nodeNumberEdgeNumber.getFirst() + 1;
        nodeNumberEdgeNumber.setFirst(nodes);
        num2node.put(nodes, v);
        for (Edge f = v.getFirstAdjacentEdge(); f != null; f = v.getNextAdjacentEdge(f))
            if (f != e) {
                var edges = nodeNumberEdgeNumber.getSecond() + 1;
                nodeNumberEdgeNumber.setSecond(edges);
                num2edge.put(edges, f);
                if (((PhyloTree) v.getOwner()).okToDescendDownThisEdgeInTraversal(f, v))
                    setNum2NodeEdgeArrayRec(f.getOpposite(v), f, nodeNumberEdgeNumber, num2node, num2edge);
            }
    }

}
