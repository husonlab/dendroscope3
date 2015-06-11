/**
 * biDAG.java 
 * Copyright (C) 2015 Daniel H. Huson
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
package dendroscope.tripletMethods;

import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: scornava
 * Date: Jul 12, 2010
 * Time: 12:41:40 PM
 * To change this template use File | Settings | File Templates.
 */


//! ----------------------
//! The root has no parent;
//! Leaves have no children, but do have data;
//! Nothing more than that...very general...can have cycles...
//! Is considered a leaf if


class biDAG {
    public biDAG parent;
    public biDAG child1, child2;
    public int data;
    public biDAG secondParent;
    public final int auxDat;
    public boolean visited;

    public boolean fixVisit;

    public boolean isDummy;

    final KSet snset;

    public int nodeNum;

    public boolean newickVisit;
    public int newickRecNum;
    public boolean newickRecVisit;


//! These are things we need for the undirected
//! DFS which we use for testing biconnectivity

    private biDAG adj[];
    private int degree;
    private biDAG pred;
    private int colour;
    private int discovery;
    private int low;

    private final static int WHITE = 0;
    private final static int GREY = 1;

    public biDAG() {
        parent = child1 = child2 = null;
        data = 0;
        secondParent = null;
        auxDat = 0;
        visited = false;
        snset = null;
        isDummy = false;

        newickVisit = false;
        newickRecNum = -1;
        newickRecVisit = false;

        pred = null;
        colour = WHITE;
    }

//! ----------------------------------------------------------------------
//! This is used for the undirected DFS...

    public void buildAdjacency() {
        adj = new biDAG[3];

        int at = 0;

        if (parent != null) adj[at++] = parent;
        if (secondParent != null) adj[at++] = secondParent;
        if (child1 != null) adj[at++] = child1;
        if (child2 != null) adj[at++] = child2;

        if (at == 4) {
            System.out.println("Node with degree 4?!?!?");
            System.exit(0);
        }
        degree = at;
    }

//! ----------------------------------------------------------------------

    public boolean isBiconnected() {
        //! Nou dit wordt leuk...
        //! remember: we are dealing here with UNDIRECTED graph

        int time[] = new int[1];
        time[0] = -1;

        //! I assume that 'this' is the root

        return this.biconVisit(time);
    }

    private boolean biconVisit(int time[]) {
        //! System.out.println("Inside node "+this);
        //! if( secondParent != null ) System.out.println("I am recombination node.");

        this.buildAdjacency();

        time[0]++;
        colour = GREY;

        discovery = time[0];
        low = time[0];

        //! System.out.println("I am discovered at time "+discovery);

        int dfsdegree = 0;

        for (int x = 0; x < degree; x++) {
            biDAG next = adj[x];

            if (next == pred) {
                //! System.out.println("Not entering that neighbour: is our DFS parent.");
                continue;    //! to ensure we don't retrace our steps
            }

            if (next.colour == WHITE) {
                //! System.out.println("Neighbour is white.");

                if (next.isLeafNode()) {
                    //! System.out.println("Not entering that neighbour: is a leaf.");
                    continue;    //! don't bother with leaves...
                }

                next.pred = this;

                dfsdegree++;
                if ((dfsdegree > 1) && (this.parent == null)) {
                    //! System.out.println("Root is articulation node.");
                    return false;
                }
                //! Root is only allowed
                //! to have degree 1 in DFS tree

                boolean result = next.biconVisit(time);
                if (result == false) return false;                //! exit the recursion zsm

                if (next.low < this.low) {
                    this.low = next.low;
                    //! System.out.println("Updating low of "+this+" to "+this.low);
                }

                if ((next.low >= this.discovery) && (this.parent != null)) {
                    //! System.out.println("A subtree rooted at "+this+" can't escape.");
                    return false;
                }
            } else {
                //! next.colour == GREY, so already visited...
                //! System.out.println("Neighbour is grey.");
                //! System.out.println("(Neighbour was "+next+")");

                if (next.discovery < this.low) this.low = next.discovery;

                //! System.out.println("Back edge: updating low of "+this+" to "+this.low);
            }

        }
        return true;
    }


    public void resetFixLeaves() {
        if (fixVisit = false) return;

        fixVisit = false;
        if (child1 != null) child1.resetFixLeaves();
        if (child2 != null) child2.resetFixLeaves();
    }

    public void dagFixLeaves(int map[]) {
        if (fixVisit) return;
        fixVisit = true;

        if (this.isLeafNode()) {
            data = map[data];
        } else {
            if (child1 != null) child1.dagFixLeaves(map);
            if (child2 != null) child2.dagFixLeaves(map);
        }
    }

//! We assume that dagMap has indexing space for 1...l where l
//! is the number of leaves. ALSO ASSUMES THAT 'VISITED' is UNUSED/RESET

    public void getDAGLeafMap(biDAG[] dagMap) {
        if (visited == true) return;

        if (data != 0) {
            dagMap[data] = this;
        }
        visited = true;

        if (child1 != null) child1.getDAGLeafMap(dagMap);
        if (child2 != null) child2.getDAGLeafMap(dagMap);
    }


    public void treeFixLeaves(int map[]) {
        //! Changes the leaf numberings from l to map[l];
        //! Note that we assume that l>=1;

        if (data != 0) {
            if ((child1 != null) || (child2 != null)) {
                System.out.println("Error 4");
            }
            data = map[data];
        } else {
            child1.treeFixLeaves(map);
            child2.treeFixLeaves(map);
        }

    }

    public static biDAG cherry12() {
        //! Returns a cherry, using 3 nodes,
        //! with leaf numbers 1,2.

        biDAG p = new biDAG();
        biDAG c1 = new biDAG();
        biDAG c2 = new biDAG();

        p.child1 = c1;
        p.child2 = c2;

        c1.parent = p;
        c1.data = 1;

        c2.parent = p;
        c2.data = 2;

        return p;
    }

//! this will leave child2 empty for other fun...

    public static biDAG subdivide(biDAG edgetail, biDAG edgehead) {
        biDAG subd = new biDAG();

        subd.parent = edgetail;
        subd.secondParent = null;

        subd.child1 = edgehead;
        subd.child2 = null;

        if (edgetail.child1 == edgetail.child2) {
            System.out.println("Uh oh, multi-edge...");
            System.exit(0);
        }

        if (edgetail.child1 == edgehead) {
            edgetail.child1 = subd;
        } else if (edgetail.child2 == edgehead) {
            edgetail.child2 = subd;
        } else {
            System.out.println("Mega mess up.");
            System.exit(0);
        }

        if (edgehead.parent == edgehead.secondParent) {
            System.out.println("Another multi-edge problem...");
            System.exit(0);
        }

        if (edgehead.parent == edgetail) {
            edgehead.parent = subd;
        } else if (edgehead.secondParent == edgetail) {
            edgehead.secondParent = subd;
        } else {
            System.out.println("Mega mess up II.");
            System.out.println("Edgetail: " + edgetail);
            System.out.println("Edgehead: " + edgehead);
            System.out.println(edgehead.parent);
            System.out.println(edgehead.secondParent);
        }

        return subd;
    }


    public void dump() {
        if (data != 0) System.out.print(data);
        else {
            if ((child1 == null) || (child2 == null)) {
                System.out.println("Error 5");
                if (child1 == null) System.out.println("child1 is null");
                if (child2 == null) System.out.println("child2 is null");
                System.exit(0);
            }
            System.out.print("[");
            child1.dump();
            System.out.print(",");
            child2.dump();
            System.out.print("]");
        }
    }

    public void resetVisited() {
        //Simplistic.doublereport("In resetVisited()");

        if (visited == false) return;

        visited = false;

        if (child1 != null) {
            child1.resetVisited();
        }
        if (child2 != null) {
            child2.resetVisited();
        }
    }

    public String newickDump(HashMap<Integer, String> ID2taxon) {
        int counter[] = new int[1];
        counter[0] = 1;

        numberRecombNodes(counter);

        String network = this.doNewick(ID2taxon) + ";";

        System.out.println(network);
        return network;
    }

//! ---------------------------------------------------

    public void numberRecombNodes(int counter[]) {
        if (newickRecVisit == true) return;

        newickRecVisit = true;

        if (child1 != null) {
            child1.numberRecombNodes(counter);
        }

        if (child2 != null) {
            child2.numberRecombNodes(counter);
        }

        if ((parent != null) && (secondParent != null)) {
            newickRecNum = counter[0];
            counter[0]++;
        }
    }

//! --------------------------------------------------------

    public String doNewick(HashMap<Integer, String> ID2taxon) {
        this.newickVisit = true;

        if (this.isLeafNode()) {
            String leafName = ID2taxon.get(this.data);
            return (leafName + "");
        }

        //! Ok, so it's either a leaf node or a recomb node...

        String lstring = null;
        String rstring = null;

        if (child1 != null) {
            if (child1.newickVisit == true) {
                lstring = "#H" + child1.newickRecNum;
            } else
                lstring = child1.doNewick(ID2taxon);
        }

        if (child2 != null) {
            if (child2.newickVisit == true) {
                rstring = "#H" + child2.newickRecNum;
            } else
                rstring = child2.doNewick(ID2taxon);
        }

        boolean benRecomb = ((parent != null) && (secondParent != null));

        if ((child1 != null) && (child2 != null)) {
            return ("(" + lstring + "," + rstring + ")");
        } else if (benRecomb) {
            return ("(" + lstring + ")#H" + newickRecNum);
        } else
            System.out.println("FOUT!");

        return ("BOOM!");

    }


//! This is hacked, might not work so well...

    public boolean isLeafNode() {
        return (this.child1 == null) && (this.child2 == null);
    }


}
