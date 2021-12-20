/*
 *  Copyright (C) 2018. Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * GraphIO.java Copyright (C) 2020. Daniel H. Huson
 *
 * (Some code written by other authors, as named in code.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package dendroscope.io;

import jloda.graph.Edge;
import jloda.graph.Graph;
import jloda.graph.Node;
import jloda.util.parse.NexusStreamParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class GraphIO {
    /**
     * writes a graph in jloda format
     *
     * @param w
     * @throws IOException
     */
    public static void write(Writer w, Graph graph) throws IOException {
        Map<Integer, Integer> nodeId2Count = new HashMap<>();
        Map<Integer, Integer> edgeId2Count = new HashMap<>();
        write(w, graph, nodeId2Count, edgeId2Count);
    }

    /**
     * writes a graph in jloda format.  Node-id to node-number and edge-id to edge-number maps are set.
     *
     * @param w
     * @param nodeId2Number after write, maps node-ids to numbers 1..numberOfNodes
     * @param edgeId2Number after write, maps edge-ids to numbers 1..numberOfEdge
     * @throws IOException
     */
    public static void write(Writer w, Graph graph, Map<Integer, Integer> nodeId2Number, Map<Integer, Integer> edgeId2Number) throws IOException {
        w.write("{GRAPH\n");
        w.write("nnodes=" + graph.getNumberOfNodes() + " nedges=" + graph.getNumberOfEdges() + "\n");
        w.write("node.labels\n");
        int count = 0;
        for (Node v : graph.nodes()) {
            nodeId2Number.put(v.getId(), ++count);
            Object info = v.getInfo();
            if (info != null) {
                w.write("" + count + ":'" + info.toString() + "'");
                if (!(info instanceof String))
                    w.write(" [" + info.getClass().getName() + "]");
                w.write("\n");
            }
        }
        w.write("adjacentEdges\n");
        count = 0;
        for (Edge e : graph.edges()) {
            edgeId2Number.put(e.getId(), ++count);
            w.write("" + count + ":" + nodeId2Number.get(e.getSource().getId()) + " " + nodeId2Number.get(e.getTarget().getId()));
            w.write("\n");
        }
        w.write("edge.labels\n");
        for (Edge e : graph.edges()) {
            Object info = e.getInfo();
            if (info != null) {
                w.write("" + edgeId2Number.get(e.getId()) + ":'" + info.toString() + "'");
                if (!(info instanceof String))
                    w.write(" [" + info.getClass().getName() + "]");
                w.write("\n");
            }
        }
        w.write("}\n");
    }

    /**
     * reads a graph in jloda format
     *
     * @param r
     * @throws IOException
     */
    public static void read(Reader r, Graph graph) throws IOException {
        read(r, graph, new Num2NodeArray(), new Num2EdgeArray());
    }

    /**
     * reads a graph in jloda format. Sets node-num to node map and edge-num edge map
     *
     * @param r
     * @param num2node after read, contains mapping of numbers used in file to nodes created in Graph
     * @param num2edge after read, contains mapping of numbers used in file to adjacentEdges created in Graph
     * @throws IOException
     */
    public static void read(Reader r, Graph graph, Num2NodeArray num2node, Num2EdgeArray num2edge) throws IOException {
        graph.clear();

        NexusStreamParser np = new NexusStreamParser(r);
        np.matchRespectCase("{GRAPH\n");
        np.matchRespectCase("nnodes=");
        int nNodes = np.getInt(0, 10000000);

        num2node.set(new Node[nNodes + 1]);
        for (int i = 1; i <= nNodes; i++) {
            num2node.put(i, graph.newNode());
        }

        np.matchRespectCase("nedges=");
        int nEdges = np.getInt(0, 10000000);
        num2edge.set(new Edge[nEdges + 1]);

        if (np.peekMatchRespectCase("node.labels"))
            np.matchRespectCase("node.labels");

        while (!np.peekMatchRespectCase("adjacentEdges")) {
            int vid = np.getInt(1, nNodes);
            np.matchRespectCase(":");
            num2node.get(vid).setInfo(np.getLabelRespectCase());
        }

        np.matchRespectCase("adjacentEdges\n");
        for (int i = 1; i <= nEdges; i++) {
            int eid = np.getInt(1, nEdges);
            np.matchRespectCase(":");
            Node source = num2node.get(np.getInt(1, nNodes));
            Node target = num2node.get(np.getInt(1, nNodes));
            Edge e = graph.newEdge(source, target);
            num2edge.put(eid, e);
        }

        if (np.peekMatchRespectCase("edge.labels")) {
            np.matchRespectCase("edge.labels\n");
            while (!np.peekMatchRespectCase("}")) {
                int eid = np.getInt(1, nEdges);
                np.matchRespectCase(":");
                num2edge.get(eid).setInfo(np.getLabelRespectCase());
            }
        }
        np.matchRespectCase("}");
    }

}
