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

package dendroscope.tripletMethods;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: scornava
 * Date: Jul 12, 2010
 * Time: 12:41:39 PM
 * To change this template use File | Settings | File Templates.
 */
class dagExplore {
    //! We need these constants for the dynamic programming...
    public static final int UNKNOWN = 0;
    public static final int YES = 1;
    public static final int NO = -1;

    public int num_mustHits;    //! the number of edge-disjoint edge subsets that
    //! simply have to be subdivided. cherries + dummy leaves
    public int num_leaves;
    public int num_nodes;
    public int num_edges;

    public Vector nodes;
    public Vector edges;
    public Vector leaves;
    public Vector dummy_nodes;

    public biDAG nodearray[];    //! maps nodeNums -> biDAGs
    public int leafarray[];        //! maps leafNums -> leafNums

    public boolean adjmatrix[][];

    //! for the algorithm of pawel
    public int join[][];
    public int cons[][][];

    public int desc[][];

    public final biDAG root;

//! ------------ methods --------------

    public dagExplore(biDAG network) {
        this.root = network;

        //! If this is not necessary then it will return
        //! immediately without wasting time.

        root.resetVisited();

        initiateExploration();
    }

//! ---------------------------------------------------------------------------

//! Using the information contained in the dagExplore, this clones the
//! biDAG that it represents.
//! changeLabels is a backmap that adjusts the labels (to make room for the ctbr that will be inserted)


    public biDAG[] clonebiDAG(int changeLabels[]) {
        biDAG newNodes[] = new biDAG[num_nodes];

        if (root.nodeNum != 0) {
            System.out.println("I expected the root to have number 0...");
            System.exit(0);
        }

        for (int x = 0; x < num_nodes; x++) {
            newNodes[x] = new biDAG();
        }

        for (int x = 0; x < num_nodes; x++) {
            biDAG oldGuy = nodearray[x];
            biDAG newGuy = newNodes[x];

            //! Only copy over necessar stuff...BE CAREFUL HERE

            newGuy.data = oldGuy.data;
            newGuy.isDummy = oldGuy.isDummy;
            newGuy.nodeNum = oldGuy.nodeNum;

            if (oldGuy.parent != null) {
                newGuy.parent = newNodes[oldGuy.parent.nodeNum];
            }
            if (oldGuy.child1 != null) {
                newGuy.child1 = newNodes[oldGuy.child1.nodeNum];
            }
            if (oldGuy.child2 != null) {
                newGuy.child2 = newNodes[oldGuy.child2.nodeNum];
            }
            if (oldGuy.secondParent != null) {
                newGuy.secondParent = newNodes[oldGuy.secondParent.nodeNum];
            }

            //! This simultaneously relabels the leaves toooo....
            if (oldGuy.data != 0) {
                newGuy.data = changeLabels[oldGuy.data];
                //! System.out.println("Mapped leaf "+oldGuy.data+" to "+newGuy.data);
            }

            //! That should be enough...
        }


        //! This should be the root......
        return newNodes;
    }


    public Vector getEdges() {
        return edges;
    }

    public Vector getDummies() {
        return dummy_nodes;
    }

    private void initiateExploration() {
        nodes = new Vector();
        edges = new Vector();
        leaves = new Vector();
        dummy_nodes = new Vector();

        dagExplore.scan(root, nodes, edges, leaves, dummy_nodes);

        num_nodes = nodes.size();
        num_edges = edges.size();
        num_leaves = leaves.size();

        nodearray = new biDAG[num_nodes];

        //! leafarray maps leaf numbers to node numbers...
        //! and leaves start at 1, hence the +1

        leafarray = new int[num_leaves + 1];

        //! this is a DIRECTED adjacency matrix...
        adjmatrix = new boolean[num_nodes][num_nodes];

        //! Here we use -1 because we want node numberings to start at 0
        int id = num_nodes - 1;

        for (int x = 0; x < nodes.size(); x++) {
            biDAG b = (biDAG) nodes.elementAt(x);

            //! reverse postorder numbering: topological sort!
            b.nodeNum = id--;

            //! System.out.println("Assigning number "+b.nodeNum+" to biDAG"+b);

            if ((b.secondParent == null) && (b.parent != null) && (b.child1 != null) && (b.child2 != null)) {
                biDAG c1 = b.child1;
                biDAG c2 = b.child2;

                //! See if this is the root of a cherry...
                if (c1.isLeafNode() && (!c1.isDummy) && c2.isLeafNode() && (!c2.isDummy)) num_mustHits++;
            } else if (b.isLeafNode() && b.isDummy) {
                biDAG p = b.parent;

                //! This is a dummy leaf that has a recomb node as parent, it has to be subdivided...
                if ((p.parent != null) && (p.secondParent != null)) num_mustHits++;
            }

            //! nodearray maps nodeNums to biDAGs...
            nodearray[b.nodeNum] = b;
        }

        for (int x = 0; x < edges.size(); x++) {
            biDAG e[] = (biDAG[]) edges.elementAt(x);

            //! System.out.println("dagExplore found edge "+e[0]+" -> "+e[1]);

            if ((e[1].parent != e[0]) && (e[1].secondParent != e[0])) {
                System.out.println("Catastrophic error in dagEncode.");
                System.exit(0);
            }

            int tail = e[0].nodeNum;
            int head = e[1].nodeNum;

            adjmatrix[tail][head] = true;
        }

        for (int x = 0; x < leaves.size(); x++) {
            biDAG b = (biDAG) leaves.elementAt(x);
            leafarray[b.data] = b.nodeNum;
        }

        cons = new int[num_nodes][num_nodes][num_nodes];
        join = new int[num_nodes][num_nodes];
        desc = new int[num_nodes][num_nodes];

        //! if necessary we can add all kinds of other look-up matrices, Vectors and so on...
    }

//! x and y are nodes, not leaves
//! asks: "is x a descendant of y?"
//! again uses memoisation

    public boolean isDescendant(int x, int y) {
        if (x == y) desc[x][y] = YES;

        if (desc[x][y] == YES) return true;
        if (desc[x][y] == NO) return false;

        //! So descendancy relation is not yet known for x, y
        //! Let's compute it and store the answer...

        biDAG X = nodearray[x];

        biDAG p1 = X.parent;

        if (p1 == null) {
            //! x is the root, y is not equal to x,
            //! so x cannot be a descendant of y

            desc[x][y] = NO;
            return false;
        }

        int xprime = p1.nodeNum;

        if (isDescendant(xprime, y)) {
            desc[x][y] = YES;
            return true;
        }

        biDAG p2 = X.secondParent;

        if (p2 == null) {
            desc[x][y] = NO;
            return false;
        }

        xprime = p2.nodeNum;

        if (isDescendant(xprime, y)) {
            desc[x][y] = YES;
            return true;
        }

        desc[x][y] = NO;
        return false;
    }


//! join is only internally used, so we assume the ints that you
//! give it refer to nodes, not leaves.

// "where join(x, z) is a predicate stating that N contains t != x and internally vertex-disjoint
// paths t ? x and t ? z."

    public boolean isJoin(int x, int z) {
        if (x == z) return false;    //! I think that's OK...or not? need to think about that...

        if (join[x][z] == YES) return true;
        if (join[x][z] == NO) return false;

        //! So it is not yet defined. Let's compute it...
        //! this requires some thinking...

        if (isDescendant(x, z)) {
            join[x][z] = YES;
            return true;
        }

        //! So x is not a descendant of z. In this case, the only chance of a join is if there
        //! is an explicit ^ shape with t distinct from both x and z

        biDAG Z = nodearray[z];

        biDAG p1 = Z.parent;

        if (p1 == null) {
            //! then z is the root. In this case x is not a descendant of z, and there
            //! can be no 't' higher than z, so the answer is FALSE.

            join[x][z] = NO;
            return false;
        }

        int zprime = p1.nodeNum;

        if (isJoin(x, zprime)) {
            join[x][z] = YES;
            return true;
        }

        biDAG p2 = Z.secondParent;

        if (p2 == null) {
            join[x][z] = NO;
            return false;
        }

        zprime = p2.nodeNum;

        if (isJoin(x, zprime)) {
            join[x][z] = YES;
            return true;
        }

        join[x][z] = NO;
        return false;
    }

//! this is the internal consistency checker: it
//! assumes that x, y, z refer to nodes not leaves...

    private boolean con(int x, int y, int z) {
        if ((x == y) || (x == z) || (y == z)) {
            cons[x][y][z] = NO;
            return false;
        }

        if (cons[x][y][z] == NO) return false;
        if (cons[x][y][z] == YES) return true;

        //! So cons[x][y][z] is UNKNOWN, we need to compute it!

        //! Remember, the node numberings are also the position
        //! in the topological sort. T'sort begins at 0.

        if ((x < z) && (y < z)) {
            biDAG Z = nodearray[z];

            biDAG p1 = Z.parent;

            if (p1 == null) {
                cons[x][y][z] = NO;
                return false;
            }

            int zprime = p1.nodeNum;

            if (con(x, y, zprime)) {
                cons[x][y][z] = YES;
                return true;
            }

            biDAG p2 = Z.secondParent;

            if (p2 == null) {
                cons[x][y][z] = NO;
                return false;
            }

            zprime = p2.nodeNum;

            if (con(x, y, zprime)) {
                cons[x][y][z] = YES;
                return true;
            }

            cons[x][y][z] = NO;
            return false;
        }


        if ((x < y) && (z < y)) {
            if (adjmatrix[x][y] && isJoin(x, z)) {
                cons[x][y][z] = YES;
                return true;
            }

            biDAG Y = nodearray[y];

            biDAG p1 = Y.parent;

            if (p1 == null) {
                //! Is this correct?

                cons[x][y][z] = NO;
                return false;
            }

            int yprime = p1.nodeNum;

            if ((yprime != x) && (yprime != z)) {
                if (con(x, yprime, z)) {
                    cons[x][y][z] = YES;
                    return true;
                }

            }

            biDAG p2 = Y.secondParent;

            if (p2 == null) {
                cons[x][y][z] = NO;
                return false;
            }

            yprime = p2.nodeNum;

            if ((yprime != x) && (yprime != z)) {
                if (con(x, yprime, z)) {
                    cons[x][y][z] = YES;
                    return true;
                }
            }

            cons[x][y][z] = NO;
            return false;
        }

        if ((y < x) && (z < x)) {
            if (adjmatrix[y][x] && isJoin(y, z)) {
                cons[x][y][z] = YES;
                return true;
            }

            biDAG X = nodearray[x];

            biDAG p1 = X.parent;

            if (p1 == null) {
                //! Is this correct?

                cons[x][y][z] = NO;
                return false;
            }

            int xprime = p1.nodeNum;

            if ((xprime != z) && (xprime != y)) {
                if (con(xprime, y, z)) {
                    cons[x][y][z] = YES;
                    return true;
                }

            }

            biDAG p2 = X.secondParent;

            if (p2 == null) {
                cons[x][y][z] = NO;
                return false;
            }

            xprime = p2.nodeNum;

            if ((xprime != z) && (xprime != y)) {
                if (con(xprime, y, z)) {
                    cons[x][y][z] = YES;
                    return true;
                }
            }

            cons[x][y][z] = NO;
            return false;
        }

        System.out.println("Shouldn't actually get here...");

        cons[x][y][z] = NO;
        return false;
    }

//! this is the external consistency checker:
//! x, y and z refer to LEAVES !!

    public boolean consistent(int x, int y, int z) {
        if ((x == 0) || (y == 0) || (z == 0)) {
            System.out.println("Leaf with 0 number?");
            System.exit(0);
        }

        if ((x == y) || (x == z) || (z == y)) return false;

        //! System.out.println("Internal: "+leafarray[x] + " " +leafarray[y] + "|" + leafarray[z] );

        return (con(leafarray[x], leafarray[y], leafarray[z]));
    }

    public static void scan(biDAG b, Vector nodes, Vector edges, Vector leaves, Vector dummy_nodes) {
        if (b.visited == true) return;

        //Simplistic.doublereport("Visiting node "+b);

        b.visited = true;

        if (b.child1 != null) {
            biDAG myedge[] = new biDAG[2];
            myedge[0] = b;
            myedge[1] = b.child1;

            if ((myedge[1].parent != myedge[0]) && (myedge[1].secondParent != myedge[0])) {
                System.out.println("I DON'T UNDERSTAND!");
                System.exit(0);
            }


            edges.addElement(myedge);

            dagExplore.scan(b.child1, nodes, edges, leaves, dummy_nodes);
        }

        if (b.child2 != null) {
            biDAG myedge[] = new biDAG[2];
            myedge[0] = b;
            myedge[1] = b.child2;

            if ((myedge[1].parent != myedge[0]) && (myedge[1].secondParent != myedge[0])) {
                System.out.println("I DON'T UNDERSTAND EITHER!");
                System.exit(0);
            }

            edges.addElement(myedge);

            dagExplore.scan(b.child2, nodes, edges, leaves, dummy_nodes);
        }

        //! so it does not get added as a leaf...REMEMBER THIS!!!

        if (b.isLeafNode() && (!b.isDummy)) leaves.addElement(b);

        if (b.isDummy) dummy_nodes.addElement(b);

        //! post order...
        nodes.addElement(b);
    }

}
