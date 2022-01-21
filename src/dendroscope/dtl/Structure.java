/*
 * Structure.java Copyright (C) 2022 Daniel H. Huson
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

import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

public class Structure {
    final PhyloTree geneTree;
    final PhyloTree speciesTree;
    PhyloTree subdivision;
    final int dupCost;
    final int transCost;
    final int lossCost;
    int[][] costMatrix;
    Event[][] eventMatrix;
    final Vector<Vector<Node>> subLayers;
    final HashMap<Integer, Integer> leavesAs;

    public Structure(int dupCost, int transCost, int lossCost, PhyloTree STree, PhyloTree GTree) {

        this.speciesTree = STree;
        this.geneTree = GTree;
        this.dupCost = dupCost;
        this.transCost = transCost;
        this.lossCost = lossCost;
        this.subLayers = new Vector<Vector<Node>>();
        this.leavesAs = new HashMap<Integer, Integer>();
    }

    //associate Leaves from genetree and speciestree
    //the association are stored in a Hashmap, called leavesAs
    @SuppressWarnings("unchecked")
    public void assoTrees() {
        NodeSet sLeaves = this.subdivision.computeSetOfLeaves();
        Iterator<Node> sL = sLeaves.iterator();
        HashMap<String, Integer> tempS = new HashMap<String, Integer>();
        while (sL.hasNext()) {
            Node current = sL.next();
            String label = this.subdivision.getLabel(current);
            int sId = current.getId();
            tempS.put(label, sId);
        }

        NodeSet gLeaves = this.geneTree.computeSetOfLeaves();
        Iterator<Node> gL = gLeaves.iterator();
        while (gL.hasNext()) {
            Node current = gL.next();
            String label = this.geneTree.getLabel(current);
            int sId = tempS.get(label);
            int gId = current.getId();
            leavesAs.put(gId, sId);
        }
    }


    private void convertSimToSub() {
        NodeSet leaves = this.speciesTree.computeSetOfLeaves();
        Iterator<Node> it = leaves.iterator();
        while (it.hasNext()) {
            Node current = it.next();
            String label = this.speciesTree.getLabel(current);
            if (label.contains("LABEL")) {
                Edge temp = current.getFirstInEdge();
                // delete the leave
                this.speciesTree.deleteEdge(temp);
                this.speciesTree.deleteNode(current);
            }
        }
        StringReader sT = new StringReader(this.speciesTree.toBracketString());
        try {
            this.subdivision = new PhyloTree();
            this.subdivision.read(sT, true);
        } catch (Exception e) {
            System.out.println("read problem");
            e.printStackTrace();
            System.err.println(e);
            System.out.println(e.toString());
        }
        this.createSubLayers();
    }

    private void createSubLayers() {
        //System.out.println(this.subdivision.toBracketString());
        NodeSet leaves = this.subdivision.computeSetOfLeaves();
        Vector<Node> subLayer = new Vector<Node>();
        Node root = this.subdivision.getRoot();
        Vector<Node> currentLayer = new Vector<Node>();
        currentLayer.add(root);
        while (currentLayer.size() != 0) {

            Iterator<Node> it = currentLayer.iterator();
            Vector<Node> nextLayer = new Vector<Node>();
            while (it.hasNext()) {
                Node current = it.next();
                LinkedList<Node> sons = this.getChildren(current);
                for (int i = 0; i < sons.size(); i++) {
                    nextLayer.add(sons.get(i));
                }
            }
            this.subLayers.add(0, currentLayer);
            currentLayer = nextLayer;
        }
        /*
          for(int i=0;i<this.subLayers.size();i++){
              Vector<Node> a = this.subLayers.elementAt(i);

              for(int j=0;j<a.size();j++){
                  Node x = a.elementAt(j);
                  System.out.println(x.getId() + " --- " + x.getOutDegree());
              }
              System.out.println(a.size() + "-------------------------------");
          }
          */
    }

    //convert speciecTree
    @SuppressWarnings("unchecked")
    private void convertToSubdivision() {
        subdivision = (PhyloTree) speciesTree.clone();
        //System.out.println(subdivision.toBracketString());
        //I'm always keeping nextNodeList in the right order, according to the Label in the Node (ordered queue)
        NodeSet currentSet = this.subdivision.computeSetOfLeaves();
        LinkedList<Edge> nextEdgeList = new LinkedList<Edge>();
        Iterator<Node> ic = currentSet.iterator();
        Node firstLeave = ic.next();
        nextEdgeList.addFirst(firstLeave.getFirstInEdge());
        Vector<Node> firstLayer = new Vector<Node>();
        firstLayer.add(firstLeave);
        while (ic.hasNext()) {
            Node x = ic.next();
            firstLayer.add(x);
            Edge inEdge = x.getFirstInEdge();
            boolean smallest = true;
            for (int i = nextEdgeList.size() - 1; i > -1; i--) {
                String label1 = subdivision.getLabel(nextEdgeList.get(i).getSource());
                String label2 = subdivision.getLabel(inEdge.getSource());
                float float1 = Float.parseFloat(label1);
                float float2 = Float.parseFloat(label2);
                if (float1 <= float2) {
                    nextEdgeList.add(i + 1, inEdge);
                    smallest = false;
                    break;
                }
            }
            if (smallest) {
                nextEdgeList.add(0, inEdge);
            }
        }

        this.subLayers.add(firstLayer);
        //for the next Element in the nextNodeList; until list empty; create a new subLayer
        while (nextEdgeList.size() > 0) {
            Vector<Node> subLayer = new Vector<Node>();
            Edge currentE = nextEdgeList.getFirst();
            Node currentN = currentE.getSource();
            subLayer.addElement(currentN);
            nextEdgeList.removeFirst();

            //remove the second edge to the source node
            if (nextEdgeList.getFirst().getSource().equals(currentN)) {
                nextEdgeList.removeFirst();
            }
            //creating new edges and adding them to the next node list, also adding nodes to subLayer
            for (int i = 0; i < nextEdgeList.size(); i++) {
                Edge newEdge = addSubNode(nextEdgeList.get(i), subLayer);
                nextEdgeList.remove(i);
                nextEdgeList.add(i, newEdge);
            }

            this.subLayers.add(subLayer);
            Edge inEdge = currentN.getFirstInEdge();
            if (inEdge != null) {
                Node father = inEdge.getSource();
                for (int i = nextEdgeList.size() - 1; i > -1; i--) {
                    String label1 = subdivision.getLabel(nextEdgeList.get(i).getSource());
                    String label2 = subdivision.getLabel(father);

                    if (Float.parseFloat(label1) <= Float.parseFloat(label2)) {
                        nextEdgeList.add(i + 1, inEdge);
                        break;
                    }
                }
            }
        }

        //System.out.println(subdivision.toBracketString());
    }

    //return children of a node
    private LinkedList<Node> getChildren(Node father) {
        LinkedList<Node> list = new LinkedList<Node>();
        for (Node c : father.children()) {
            list.add(c);
        }
        return list;
    }

    //use in convertToSub to create an extra Node for one subLayer
    private Edge addSubNode(Edge x, Vector<Node> subLayer) {
        Node father = x.getSource();
        Node son = x.getTarget();
        this.subdivision.deleteEdge(x);
        Node between = new Node(this.subdivision, "new");
        subLayer.add(between);
        subdivision.newEdge(between, son);
        return subdivision.newEdge(father, between);
    }

    //tree reconciliation
    public int getCost(int version) {
        if (version == 1) {
            this.convertToSubdivision();
            this.assoTrees();
        } else if (version == 2) {
            this.convertSimToSub();
            this.assoTrees();
        }
        //Generate cost matrix
        int subSize = this.subdivision.getNumberOfNodes();
        int genSize = this.geneTree.getNumberOfNodes();
        //init cost Matrix
        this.costMatrix = new int[genSize][subSize];
        for (int i = 0; i < this.costMatrix.length; i++) {
            for (int j = 0; j < this.costMatrix[i].length; j++) {
                this.costMatrix[i][j] = 999999999;
            }
        }
        //this.eventMatrix = new Event[genSize][subSize];

        Node geneRoot = this.geneTree.getRoot();
        this.traversal(geneRoot);
        recon(geneRoot);
        Node speciesRoot = this.speciesTree.getRoot();
        //showMatrix();
        return this.costMatrix[geneRoot.getId() - 1][speciesRoot.getId() - 1];
    }

    //recursion on gennodes
    private void traversal(Node geneNode) {
        Iterator<Edge> it = geneNode.outEdges().iterator();
        if (it.hasNext()) {
            Edge first = (Edge) it.next();
            traversal(first.getTarget());
            recon(first.getTarget());
        }
        if (it.hasNext()) {
            Edge second = (Edge) it.next();
            traversal(second.getTarget());
            recon(second.getTarget());
        }

    }

    //the loops of the algorithm
    private void recon(Node current) {
        int uId = current.getId();
        int degree = current.getOutDegree();

        Iterator<Vector<Node>> it = this.subLayers.iterator();
        // going over generated timeLayers
        while (it.hasNext()) {
            Vector<Node> subLayer = it.next();
            if (subLayer.size() > 1) {
                // bestreceiver
                int[] bestReceiver1 = new int[2];
                int[] bestReceiver2 = new int[2];
                if (degree == 2) {
                    LinkedList<Node> children = getChildren(current);
                    int u1Id = children.getFirst().getId();
                    int u2Id = children.getLast().getId();
                    // TODO: für 2 kinder
                    int[] temp = this.bestReceiverT(subLayer, u1Id, u2Id);
                    bestReceiver1[0] = temp[0];
                    bestReceiver1[1] = temp[1];
                    bestReceiver2[0] = temp[2];
                    bestReceiver2[1] = temp[3];
                    //bestReceiver1 = this.bestReceiver(subLayer, u1Id);
                    //bestReceiver2 = this.bestReceiver(subLayer, u2Id);
                }

                // cost
                Iterator<Node> it2 = subLayer.iterator();
                while (it2.hasNext()) {
                    Node speciesNode = it2.next();
                    int xId = speciesNode.getId();
                    int a = -1;
                    if (leavesAs.containsKey(uId)) {
                        a = leavesAs.get(uId);
                        if (a == xId) {
                            costMatrix[uId - 1][xId - 1] = 0;
                        } else {
                            this.computeCostForEdges(current, speciesNode,
                                    bestReceiver1, bestReceiver2, true);
                        }
                    } else {
                        this.computeCostForEdges(current, speciesNode,
                                bestReceiver1, bestReceiver2, true);
                    }
                }

                int[] bestReceiver = this.bestReceiver(subLayer, uId);
                it2 = subLayer.iterator();
                while (it2.hasNext()) {
                    Node speciesNode = it2.next();

                    int xId = speciesNode.getId();
                    int a = -1;
                    if (leavesAs.containsKey(uId)) {
                        a = leavesAs.get(uId);
                        if (a == xId) {
                            costMatrix[uId - 1][xId - 1] = 0;
                        } else {
                            tl(current, speciesNode, bestReceiver[0],
                                    bestReceiver[1]);
                        }
                    } else {
                        tl(current, speciesNode, bestReceiver[0],
                                bestReceiver[1]);
                    }
                }
            } else {
                Iterator<Node> it2 = subLayer.iterator();
                while (it2.hasNext()) {
                    Node speciesNode = it2.next();
                    this.computeCostForEdges(current, speciesNode, new int[1], new int[1], false);
                }
            }
        }
    }

    //compute cost for a pair of species and genenode
    private void computeCostForEdges(Node u, Node x, int[] u1Receiver, int[] u2Receiver, boolean transfer) {
        int degreeU = u.getOutDegree();
        int uId = u.getId();
        int degreeX = x.getOutDegree();
        int xId = x.getId();
        int cost = 214748364;
        int costD;
        int costT;
        int cost0;
        int costSL;
        if (degreeU == 2) {
            LinkedList<Node> children = this.getChildren(u);
            Node u1 = children.getFirst();
            Node u2 = children.getLast();
            int u1Id = u1.getId();
            int u2Id = u2.getId();
            //2 - 2
            if (degreeX == 2) {
                LinkedList<Node> childrenX = this.getChildren(x);
                Node x1 = childrenX.getFirst();
                Node x2 = childrenX.getLast();
                int x1Id = x1.getId();
                int x2Id = x2.getId();
                //u1,x1,u2,x2
                int costU1X1U2X2 = this.costMatrix[u1Id - 1][x1Id - 1] + this.costMatrix[u2Id - 1][x2Id - 1];
                int costU1X2U2X1 = this.costMatrix[u1Id - 1][x2Id - 1] + this.costMatrix[u2Id - 1][x1Id - 1];

                if (costU1X1U2X2 < costU1X2U2X1) {
                    cost = costU1X1U2X2;
                } else {
                    cost = costU1X2U2X1;
                }
            }
            //duplication
            costD = this.costMatrix[u1Id - 1][xId - 1] + this.costMatrix[u2Id - 1][xId - 1] + this.dupCost;
            if (costD < cost) {
                cost = costD;

            }
            // Transfer costs
            if (transfer == true) {
                int bestRU1 = u1Receiver[0];
                if (xId == u1Receiver[0]) {
                    bestRU1 = u1Receiver[1];
                }
                int bestRU2 = u2Receiver[0];
                if (xId == u2Receiver[0]) {
                    bestRU2 = u2Receiver[1];
                }
                int costT1 = this.costMatrix[u1Id - 1][xId - 1]
                        + this.costMatrix[u2Id - 1][bestRU2 - 1];
                int costT2 = this.costMatrix[u1Id - 1][bestRU1 - 1]
                        + this.costMatrix[u2Id - 1][xId - 1];
                if (costT1 < costT2) {
                    costT = costT1 + this.transCost;
                } else {
                    costT = costT2 + this.transCost;
                }
                if (costT < cost) {
                    cost = costT;
                }
            }
        }
        if (degreeX == 1) {
            LinkedList<Node> childrenX = this.getChildren(x);
            Node x1 = childrenX.getFirst();
            int x1Id = x1.getId();
            cost0 = this.costMatrix[uId - 1][x1Id - 1];
            if (cost0 < cost) {
                cost = cost0;
            }
        } else if (degreeX == 2) {
            LinkedList<Node> childrenX = this.getChildren(x);
            Node x1 = childrenX.getFirst();
            Node x2 = childrenX.getLast();
            int x1Id = x1.getId();
            int x2Id = x2.getId();
            int cost1 = this.costMatrix[uId - 1][x1Id - 1];
            int cost2 = this.costMatrix[uId - 1][x2Id - 1];
            if (cost1 < cost2) {
                costSL = cost1 + this.lossCost;
            } else {
                costSL = cost2 + this.lossCost;
            }
            if (costSL < cost) {
                cost = costSL;
            }
        }
        //Min of Costs... costS,costD,costT,cost0,costSL
        this.costMatrix[uId - 1][xId - 1] = cost;
        //showMatrix();
    }

    //compute costs for transfer-loss events
    private void tl(Node u, Node spec, int bestId, int secondId) {
        int uId = u.getId();
        if (uId == bestId) {
            bestId = secondId;
        }
        int costTL = this.costMatrix[uId - 1][bestId - 1] + this.transCost + this.lossCost;
        int xId = spec.getId();
        if (this.costMatrix[uId - 1][xId - 1] > costTL) {
            this.costMatrix[uId - 1][xId - 1] = costTL;
        }
    }

    //bestreceiver
    private int[] bestReceiverT(Vector<Node> subLayer, int u1Id, int u2Id) {

        Iterator<Node> it2 = subLayer.iterator();
        //bestreceiver
        int[] result = new int[4];
        Node initSpecNode = it2.next();
        int iid = initSpecNode.getId();

        //init u1
        int bestR = this.costMatrix[u1Id - 1][iid - 1];
        int secondR = 2147483647;
        result[0] = iid;
        result[1] = iid;
        //init u2
        int bestR2 = this.costMatrix[u2Id - 1][iid - 1];
        int secondR2 = 2147483647;
        result[2] = iid;
        result[3] = iid;

        while (it2.hasNext()) {
            Node speciesNode = it2.next();
            int id = speciesNode.getId();
            int cost = this.costMatrix[u1Id - 1][id - 1];
            if (cost < bestR) {
                secondR = bestR;
                bestR = cost;
                result[1] = result[0];
                result[0] = id;
            } else if (cost < secondR) {
                secondR = cost;
                result[1] = id;
            }
            int cost2 = this.costMatrix[u2Id - 1][id - 1];
            if (cost2 < bestR2) {
                secondR2 = bestR2;
                bestR2 = cost2;
                result[3] = result[2];
                result[2] = id;
            } else if (cost2 < secondR2) {
                secondR2 = cost2;
                result[3] = id;
            }
        }
        return result;
    }

    private int[] bestReceiver(Vector<Node> subLayer, int uId) {
        Iterator<Node> it2 = subLayer.iterator();
        //bestreceiver
        int[] result = new int[2];
        Node initSpecNode = it2.next();
        int iid = initSpecNode.getId();

        int bestR = this.costMatrix[uId - 1][iid - 1];
        int secondR = 2147483647;
        result[0] = iid;
        result[1] = iid;
        while (it2.hasNext()) {
            Node speciesNode = it2.next();
            int id = speciesNode.getId();
            int cost = this.costMatrix[uId - 1][id - 1];
            if (cost < bestR) {
                secondR = bestR;
                bestR = cost;
                result[1] = result[0];
                result[0] = id;
            } else if (cost < secondR) {
                secondR = cost;
                result[1] = id;
            }
        }

        return result;
    }

    //system.out.pr..... matrix
    private void showMatrix() {
        for (int i = 0; i < this.costMatrix.length; i++) {
            System.out.println();
            for (int j = 0; j < this.costMatrix[i].length; j++) {
                System.out.print(this.costMatrix[i][j] + " ");
            }
        }
    }

    private void showMatrixRow(int i) {
        for (int j = 0; j < this.costMatrix[i].length; j++)
            System.out.print(this.costMatrix[i][j] + " ");
    }
}
