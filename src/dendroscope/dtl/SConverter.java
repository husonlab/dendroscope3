/*
 * SConverter.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.dtl;


import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;

import java.io.*;
import java.util.Iterator;

public class SConverter {

    final PhyloTree STree;

    public SConverter() {
        STree = new PhyloTree();
        STree.setAllowMultiLabeledNodes(true);
    }

    public void getOriginalTree(String[] path) {
        for (int i = 0; i < path.length; i++) {
            try {
                FileInputStream fstreamS = new FileInputStream(path[i]);
                InputStreamReader inS = new InputStreamReader(fstreamS);
                STree.read(inS, true);

            } catch (Exception ignored) {

            }

            System.out.println(STree.getLabel(STree.getRoot()));
            NodeSet leaves = STree.computeSetOfLeaves();
            for (Node current : leaves) {
                String label = STree.getLabel(current);
                Node root = STree.getRoot();
                if (label.matches("LABEL")) {
                    Edge temp = current.getFirstInEdge();
                    // delete the leave
                    Node father = temp.getSource();
                    STree.deleteEdge(temp);
                    STree.deleteNode(current);
                }
            }
            Node root = STree.getRoot();
            // checkAndGo for deleting node with degree 1
            checkAndGo(root);

            try {
                File file = new File("/home/wojtek/firstTests/STREE_" + (i + 1)
                                     + ".txt");
                boolean success = file.createNewFile();
                if (success) { // File did not exist and was created
                    FileWriter fstream = new FileWriter(file);
                    BufferedWriter out = new BufferedWriter(fstream);
                    out.write(STree.toBracketString());
                    out.close();
                } else { // File already exists
                    System.out.println("not happy!!!");
                }
            } catch (Exception ignored) {

            }

            System.out.println(STree.countLeaves());

        }
    }

    private void checkAndGo(Node current) {
        int degree = current.getOutDegree();
        if (degree == 1) {
            Edge in = current.getFirstInEdge();
            Edge out = current.getFirstOutEdge();

            Node father = in.getSource();
            Node son = out.getTarget();

            STree.deleteEdge(in);
            STree.deleteEdge(out);
            STree.deleteNode(current);
            STree.newEdge(father, son);

            checkAndGo(son);
        } else if (degree == 2) {
            Iterator<Edge> it = current.outEdges().iterator();
            Edge first = it.next();
            checkAndGo(first.getTarget());
            Edge second = it.next();
            checkAndGo(second.getTarget());
            System.out.println("tell me!222222222222222222222222222222222!!!!");
        } else if (degree == 0) {
            System.out.println("tell me!00000000000000000000000000000000!!!!");
        } else {
            System.out.println("tell me!!!!!");
        }

    }
}


