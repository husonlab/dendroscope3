/**
 * LCA_LSACalculation.java 
 * Copyright (C) 2018 Daniel H. Huson
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
package dendroscope.util;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * this class calculates the lca for every pair of nodes in a given tree T.
 * thomas bonfert, celine scornavacca 10.2009
 */

public class LCA_LSACalculation {

    // nodes visited in an Euler Tour of T
    private final Vector<Node> eulerTour;
    // levels of the nodes visited in the Euler Tour, whereas levels[i] is the level of node eulerTour[i]
    private final Vector<Integer> levels;
    // index of the first occurence of node i in E
    private final int[] firstOccurences;
    //preprocessing matrix
    private final int[][] matrixRMQ;
    private final double log2 = Math.log(2);

    public LCA_LSACalculation(PhyloTree tree) {
        this.eulerTour = new Vector<Node>();
        this.levels = new Vector<Integer>();
        this.firstOccurences = new int[tree.getNumberOfNodes()];
        //filling the arrays and the preprocessing matrix for RMQs.
        int numNodes = doEulerTour(tree, this.eulerTour, this.levels, this.firstOccurences, false);
        this.matrixRMQ = preprocessRMQ(tree, numNodes);
    }

    /*
    if computeLSA is true, we compute the LCA on the LSA tree, i.e. the LSA
     */
    public LCA_LSACalculation(PhyloTree tree, boolean computeLSA) {
        this.eulerTour = new Vector<Node>();
        this.levels = new Vector<Integer>();
        //this.firstOccurences = new int[tree.getNumberOfNodes()];
        this.firstOccurences = new int[tree.computeMaxId()];
        //filling the arrays and the preprocessing matrix for RMQs.
        int numNodes = doEulerTour(tree, this.eulerTour, this.levels, this.firstOccurences, computeLSA);
        //System.err.println("numNodes " + numNodes);
        this.matrixRMQ = preprocessRMQ(tree, numNodes);
    }

    /**
     * filling the vectors in a depth-first search
     */
    private int doEulerTour(PhyloTree t, Vector<Node> eulerTour, Vector<Integer> levels, int[] firstOccurences, boolean computeLSA) {
        Node root = t.getRoot();
        int currentLevel = 0;
        int numNodesEnd = 0;
        numNodesEnd = doEulerTourRec(t, root, currentLevel, eulerTour, levels, firstOccurences, computeLSA, numNodesEnd);
        return numNodesEnd;
    }

    /**
     * does the work recursively
     */
    private int doEulerTourRec(PhyloTree t, Node n, int currentLevel, Vector<Node> eulerTour, Vector<Integer> levels, int[] firstOccurences, boolean computeLSA, int numNodes) {
        if (t.getNode2GuideTreeChildren() == null)
            System.err.println("errore");
        if (computeLSA) {
            while (t.getNode2GuideTreeChildren().get(n).size() == 1) {
                n = t.getNode2GuideTreeChildren().get(n).get(0);
            }
        }
        eulerTour.add(n);
        levels.add(currentLevel);
        firstOccurences[n.getId() - 1] = eulerTour.size() - 1;
        currentLevel++;
        numNodes++;


        if (!computeLSA) {
            for (Edge e = n.getFirstOutEdge(); e != null; e = n.getNextOutEdge(e)) {
                numNodes = doEulerTourRec(t, e.getTarget(), currentLevel, eulerTour, levels, firstOccurences, computeLSA, numNodes);
                eulerTour.add(n);
                levels.add(currentLevel - 1);
            }
        } else {
            List<Node> LSAChildren = t.getNode2GuideTreeChildren().get(n);
            for (Iterator<Node> it = LSAChildren.iterator(); it.hasNext(); ) {
                Node LSASon = it.next();
                numNodes = doEulerTourRec(t, LSASon, currentLevel, eulerTour, levels, firstOccurences, computeLSA, numNodes);
                eulerTour.add(n);
                levels.add(currentLevel - 1);
            }
        }
        return numNodes;
    }

    /**
     * fills the preprocessing matrix, which needs O(n log n) space.
     */
    private int[][] preprocessRMQ(PhyloTree tree, int numNodes) {
        int n = 2 * numNodes - 1; //when it is used for the LSA  numNodes!= numNodes of the all graph
        int[][] m = new int[n][(int) Math.ceil(Math.log(n) / this.log2)];
        for (int i = 0; i < n; i++) {
            m[i][0] = i;
        }
        for (int j = 1; 1 << j <= n; j++) {
            for (int i = 0; i + (1 << j) - 1 < n; i++) {
                if (this.levels.get(m[i][j - 1]) < this.levels.get(m[i + (1 << (j - 1))][j - 1])) {
                    m[i][j] = m[i][j - 1];
                } else {
                    m[i][j] = m[i + (1 << (j - 1))][j - 1];
                }
            }
        }
        return m;
    }


    /**
     * calculates the RMQ of the subarray levels[i,j].
     *
     * @param i
     * @param j
     * @return RMQ_L(i, j)
     */
    public int getRMQ(int i, int j) {
        int k = 0;
        if (j != i)
            k = (int) Math.floor(Math.log(Math.abs(j - i)) / this.log2); //bug of thomas. He forgot the abs...
        else
            k = 0;  //if they are the same node
        int pow = pow(2, k);
        if (this.levels.get(this.matrixRMQ[i][k]) <= this.levels.get(this.matrixRMQ[j - pow + 1][k]))
            return this.matrixRMQ[i][k];

        else
            return this.matrixRMQ[j - pow + 1][k];
    }

    /**
     * calculates base^exponent for two integer values.
     * is quite faster than the java internal function.
     */
    private static int pow(int base, int exponent) {
        int result = 1;
        for (int x = base; exponent > 0; x *= x, exponent >>= 1) {
            if ((exponent & 1) == 1) result *= x;
        }
        return result;
    }


    /**
     * calculates the LCA of two given nodes a and b.
     *
     * @param a
     * @param b
     * @return LCA(a, b)
     */

    public Node getLCA(Node a, Node b) {
        //Todo: comment this out for a moment for method getEmbedableOrder
        if (a.equals(b)) {
            int id = a.getId();
            int firstOc = this.firstOccurences[id - 1];
            //System.err.println("first occurence: " + firstOc);
            return this.eulerTour.get(getRMQ(this.firstOccurences[id - 1], this.firstOccurences[id - 1]));
            //return a;
        } else {
            int id1 = a.getId();
            int id2 = b.getId();

            if (id1 < id2)
                return this.eulerTour.get(getRMQ(this.firstOccurences[id1 - 1], this.firstOccurences[id2 - 1]));
            else
                return this.eulerTour.get(getRMQ(this.firstOccurences[id2 - 1], this.firstOccurences[id1 - 1]));
        }
    }


    public Node getLca(Node[] nodesToArray, boolean computeLSA) {

        int length = nodesToArray.length;
        int lengthInit = nodesToArray.length;
        while (length > 1) {
            Node a = nodesToArray[length - 2];
            Node b = nodesToArray[length - 1];
            Node tmpNode = getLCA(a, b);
            nodesToArray[length - 1] = null;
            nodesToArray[length - 2] = tmpNode;
            length--;

        }
        if (lengthInit == 1) {
            if (!computeLSA)
                return nodesToArray[0]; // LCA of a node is the node itself
            if (nodesToArray[0].getInDegree() == 1)
                return nodesToArray[0].getFirstInEdge().getSource();
            else if (nodesToArray[0].getInDegree() == 0)
                return nodesToArray[0]; // LSA of the root  is the root itself   
            else {
                Node[] fathers = new Node[nodesToArray[0].getInDegree()];
                int i = 0;
                for (Edge e = nodesToArray[0].getFirstInEdge(); e != null; e = nodesToArray[0].getNextInEdge(e)) {
                    fathers[i] = e.getSource();
                    i++;
                }
                return getLca(fathers, computeLSA);
            }
        }
        return nodesToArray[0];
    }


    public Node getLca(HashSet<Node> nodes, boolean computeLSA) {
        Object[] nodesToArray = nodes.toArray();
        return getLca((Node[]) nodesToArray, computeLSA);

    }


    public Node getLca(NodeSet nodes, boolean computeLSA) {
        Node[] nodesToArray = new Node[nodes.size()];
        int i = 0;
        for (Iterator<Node> it = nodes.iterator(); it.hasNext(); ) {
            nodesToArray[i] = it.next();
            i++;
        }
        return getLca(nodesToArray, computeLSA);
    }

}
