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

            } catch (Exception e) {

            }

            System.out.println(STree.getLabel(STree.getRoot()));
            NodeSet leaves = STree.computeSetOfLeaves();
            Iterator<Node> it = leaves.iterator();
            while (it.hasNext()) {
                Node current = it.next();
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
            } catch (Exception e) {

            }

            System.out.println(STree.getNumberOfLeaves());

        }
    }

    private void checkAndGo(Node current) {
        int degree = current.getOutDegree();
        if (degree == 1) {
            Edge in = current.getInEdges().next();
            Edge out = current.getOutEdges().next();

            Node father = in.getSource();
            Node son = out.getTarget();

            STree.deleteEdge(in);
            STree.deleteEdge(out);
            STree.deleteNode(current);
            STree.newEdge(father, son);

            checkAndGo(son);
        } else if (degree == 2) {
            Iterator<Edge> it = current.getOutEdges();
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


