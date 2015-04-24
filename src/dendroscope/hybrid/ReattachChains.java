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

package dendroscope.hybrid;

import jloda.graph.Edge;
import jloda.graph.Node;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class ReattachChains {

    public void run(HybridNetwork n, ReplacementInfo rI, TreeMarker tM) {


        Hashtable<String, Node> labelToNode = new Hashtable<>();
        for (Node l : n.computeSetOfLeaves()) {
            labelToNode.put(n.getLabel(l), l);
        }

        Hashtable<String, Vector<String>> startLabelToChain = rI
                .getStartLabelToChain();
        Hashtable<String, String> startLabelToEndLabel = rI
                .getStartLabelToEndLabel();
        insertChain(n, labelToNode, startLabelToChain, startLabelToEndLabel, tM);

        n.initTaxaOrdering();
        n.update();

    }

    private void insertChain(HybridNetwork n,
                             Hashtable<String, Node> labelToNode,
                             Hashtable<String, Vector<String>> startLabelToChain,
                             Hashtable<String, String> startLabelToEndLabel, TreeMarker tM) {

        Vector<Node> startNodes = new Vector<>();
        for (Node v : n.computeSetOfLeaves()) {
            if (startLabelToChain.containsKey(n.getLabel(v)))
                startNodes.add(v);
        }

        for (Node start : startNodes) {
            Node end = labelToNode.get(startLabelToEndLabel.get(n
                    .getLabel(start)));

            boolean b = true;
            Node p = start.getInEdges().next().getSource();
            if (p.getOutDegree() == 2) {
                Edge e = getChainEdge(n, p, n.getLabel(start));
                Node c = e.getTarget();
                if (n.getLabel(c).equals(n.getLabel(end))) {
                    insertNormalChain(n, start, startLabelToChain.get(n
                            .getLabel(start)));
                    b = false;
                } else {
                    Iterator<Node> childIt = n.getSuccessors(c);
                    while (childIt.hasNext()) {
                        Node x = childIt.next();
                        if (n.getLabel(x).equals(n.getLabel(end))) {
                            insertNormalChain(n, start, startLabelToChain.get(n
                                    .getLabel(start)));
                            b = false;
                            break;
                        }
                    }
                }
            }
            if (b)
                insertHybridChain(n, start, end, startLabelToChain.get(n
                        .getLabel(start)), tM);
        }

    }

    private Vector<Node> insertNormalChain(HybridNetwork n, Node start,
                                           Vector<String> chain) {

        Vector<Node> newLeaves = new Vector<>();
        Node p = start.getInEdges().next().getSource();

        for (int i = chain.size() - 2; i > 0; i--) {

            String label = chain.get(i);
            Edge e = getChainEdge(n, p, chain.get(i + 1));
            Node target = e.getTarget();
            n.deleteEdge(e);

            Node newP = n.newNode();
            Node leaf = n.newNode();
            n.setLabel(leaf, label);
            newLeaves.add(leaf);
            n.newEdge(p, newP);
            n.newEdge(newP, target);
            n.newEdge(newP, leaf);

            p = newP;
        }

        return newLeaves;
    }

    @SuppressWarnings("unchecked")
    private void insertHybridChain(HybridNetwork n, Node start, Node end,
                                   Vector<String> chain, TreeMarker tM) {
        Edge inEdge = start.getInEdges().next();
        Node p = inEdge.getSource();

        Vector<Node> newLeaves = new Vector<>();

        Node newP = n.newNode();
        Node leaf = n.newNode();
        n.setLabel(leaf, chain.get(1));
        newLeaves.add(leaf);
        Vector<String> chainCopy = (Vector<String>) chain.clone();
        chainCopy.remove(0);
        n.deleteEdge(inEdge);

        n.newEdge(p, newP);
        n.newEdge(newP, start);
        n.newEdge(newP, leaf);

        for (Node v : insertNormalChain(n, start, chainCopy))
            newLeaves.add(v);

        insertReticulations(n, end, newLeaves, tM);
    }

    private void insertReticulations(HybridNetwork n, Node end,
                                     Vector<Node> newLeaves, TreeMarker tM) {

        int i = 0;
        for (Node v : newLeaves) {
            Edge inEdge = end.getInEdges().next();
            Node p = inEdge.getSource();
            n.deleteEdge(inEdge);
            Node newP = n.newNode();
            n.newEdge(p, newP);
            n.newEdge(newP, end);
            insertReticulation(n, newP, v, tM);
            i++;
        }

    }

    private void insertReticulation(HybridNetwork n, Node newP, Node v, TreeMarker tM) {

        n.initTaxaOrdering();
        n.update();

        Edge inEdge = v.getInEdges().next();
        Node p = inEdge.getSource();

        n.deleteEdge(inEdge);

        Node ret = n.newNode();

        Edge r1 = n.newEdge(newP, ret);
        n.setSpecial(r1, true);
        n.setWeight(r1, 0);

        Edge r2 = n.newEdge(p, ret);
        n.setSpecial(r2, true);
        n.setWeight(r2, 0);

        tM.insertT1Edge(r2);

        n.newEdge(ret, v);
    }

    private Edge getChainEdge(HybridNetwork n, Node v, String label) {
        Iterator<Edge> it = v.getOutEdges();
        while (it.hasNext()) {
            Edge e = it.next();
            Node c = e.getTarget();
            if (!n.getLabel(c).equals(label)) {
                return e;
            }
        }
        return null;
    }

}
