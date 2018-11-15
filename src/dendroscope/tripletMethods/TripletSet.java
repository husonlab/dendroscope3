/**
 * TripletSet.java 
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
package dendroscope.tripletMethods;

import java.util.Enumeration;
import java.util.Stack;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: scornava
 * Date: Jun 24, 2010
 * Time: 3:54:26 PM
 * To change this template use File | Settings | File Templates.
 */
class TripletSet {
    private Vector tripVec;
    private int numLeaves;
    private boolean finalised;
    private boolean lookup[][][];

    public TripletSet() {
        tripVec = new Vector();
        numLeaves = 0;
        finalised = false;
    }

    public Vector getTripVec() {
        return tripVec;
    }

    public void setTripVec(Vector tripVec) {
        this.tripVec = tripVec;
    }

    public int getNumLeaves() {
        return numLeaves;
    }

    public void setNumLeaves(int numLeaves) {
        this.numLeaves = numLeaves;
    }

    public boolean isFinalised() {
        return finalised;
    }

    public void setFinalised(boolean finalised) {
        this.finalised = finalised;
    }

    public boolean[][][] getLookup() {
        return lookup;
    }

    public void setLookup(boolean[][][] lookup) {
        this.lookup = lookup;
    }

    public void initLookup(int size) {
        this.lookup = new boolean[size + 1][size + 1][size + 1];
    }

    public Enumeration elements() {
        return tripVec.elements();
    }


    //! This is currently very slooow.
    public boolean isDense(int errorTrip[]) {
        for (int x = 1; x <= numLeaves; x++)
            for (int y = 1; y <= numLeaves; y++)
                for (int z = 1; z <= numLeaves; z++) {
                    if ((x == y) || (x == z) || (z == y)) continue;
                    if (!containsTriplet(x, y, z) && !containsTriplet(x, z, y) && !containsTriplet(z, y, x)) {
                        errorTrip[0] = x;
                        errorTrip[1] = y;
                        errorTrip[2] = z;
                        return false;
                    }
                }
        return true;
    }


    public void addTriplet(int a, int b, int c) {
        if (finalised == true) {
            System.out.println("ERROR#2: Major error: tried adding triplet to finalised triplet set.");
            System.exit(0);
        }

        int swap;

        //! swap a and b around, if necessary...

        if (a > b) {
            swap = a;
            a = b;
            b = swap;
        }

        if (this.containsTriplet(a, b, c)) return;

        //! What is the highest leaf seen so far?
        // celine: not used anymore since numLeaves has to be known before to construct the Map<String, Integer> taxon2ID

        int highest;

        if ((a > b) && (a > c)) highest = a;
        else if ((b > a) && (b > c)) highest = b;
        else
            highest = c;

        if (highest > numLeaves) {
            numLeaves = highest;
        }


        int myTriplet[] = new int[3];
        myTriplet[0] = a;
        myTriplet[1] = b;
        myTriplet[2] = c;

        tripVec.add(myTriplet);
    }


    public boolean containsTripletFast(int a, int b, int c) {
        return (lookup[a][b][c] || lookup[b][a][c]);
    }

    public void addTripletInMatrix(int a, int b, int c) {

        lookup[a][b][c] = true;
        lookup[b][a][c] = true;
    }


    public void addTripletInList(int a, int b, int c) {


        int swap;

        //! swap a and b around, if necessary...

        if (a > b) {
            swap = a;
            a = b;
            b = swap;
        }

        if (this.containsTripletFast(a, b, c)) return;

        int myTriplet[] = new int[3];
        myTriplet[0] = a;
        myTriplet[1] = b;
        myTriplet[2] = c;

        tripVec.add(myTriplet);
    }


    public boolean containsTriplet(int a, int b, int c) {
        int swap;

        if (finalised == true) {
            return (lookup[a][b][c] || lookup[b][a][c]);
        }

        //! swap a and b around, if necessary...

        if (a > b) {
            swap = a;
            a = b;
            b = swap;
        }

        Enumeration e = tripVec.elements();
        while (e.hasMoreElements()) {
            int triplet[] = (int[]) e.nextElement();
            if ((triplet[0] == a) && (triplet[1] == b) && (triplet[2] == c)) return true;
        }

        return false;
    }


    //! After calling this, look-up is miles faster, BUT you can't change the set anymore!!!

    public void finaliseAndOptimise() {
        finalised = true;

        lookup = new boolean[numLeaves + 1][numLeaves + 1][numLeaves + 1];

        Enumeration e = this.tripVec.elements();

        while (e.hasMoreElements()) {
            int t[] = (int[]) e.nextElement();

            int x = t[0];
            int y = t[1];
            int z = t[2];

            lookup[x][y][z] = true;
            lookup[y][x][z] = true;
        }

    }


    //! Not sure whether this is a correct implementation, try and verify the
    //! correctness of this...I think it should be approximately cubic time...
    //! Ah I now understand why this works. At any one iteration, X cup Z is
    //! the current SN set, and X cup Z contains all leaves which were
    //! added because of triplets of the form x1 * | x2

    public KSet FastComputeSN(int x, int y) {
        int n = numLeaves;

        KSet X = new KSet(n);
        KSet Z = new KSet(n);

        X.addLeaf(x);
        Z.addLeaf(y);

        while (Z.empty() == false) {
            int z = Z.getFirstElement();

            for (int a = 1; a <= n; a++) {
                if (X.containsLeaf(a) == false) continue;

                for (int c = 1; c <= n; c++) {
                    if (X.containsLeaf(c) || Z.containsLeaf(c)) continue;

                    if (this.containsTriplet(a, c, z) || this.containsTriplet(z, c, a)) {
                        Z.addLeaf(c);

                        //! I think their algorithm mistakenly implies that we can 'break' here
                    }

                }
                X.addLeaf(z);
                Z.removeLeaf(z);
            }

        }
        return X;
    }

    //! Returns a vector of KSets, one KSet per leaf. This to
    //! retain compatibility with computeCTBRs routine from
    //! KSet

    public Vector<KSet> getLeaves() {
        Vector<KSet> v = new Vector<>();

        for (int x = 1; x <= this.numLeaves; x++) {
            KSet k = new KSet(this.numLeaves);
            k.addLeaf(x);
            v.addElement(k);
        }

        return v;
    }


    //! ------------------------------------------------------
    //! returns a tree of KSets.
    //! specifically, returns the KSet at the root (the set of all leaves)

    public KSet buildSNTree() {
        //System.out.println("Constructing SN-sets.");

        if (this.numLeaves == 0) return null;    //! there are no SN-sets at all!

        Vector sn = this.getSNSets();

        //! Ok, so now we have a vector containing all SN-sets.
        //! We need to build an intelligent data-structure out of them, to make
        //! everything nice and fast.

        //System.out.println("Ranking the SN-sets in terms of size.");

        //! snrank begins at [1], goes up until [this.numLeaves]

        Vector snrank[] = new Vector[this.numLeaves + 1];
        for (int x = 1; x < snrank.length; x++) {
            snrank[x] = new Vector();
        }

        //! This makes a ranking based on the number of leaves...
        for (int x = 0; x < sn.size(); x++) {
            KSet k = (KSet) sn.elementAt(x);
            int l = k.size();
            snrank[l].add(k);
        }

        //System.out.println("Finishing ranking, now putting in linear order...");

        //! ---------------------------------------------
        //! This creates an SN-set array which has
        //! elements of increasing size...

        KSet ordered[] = new KSet[sn.size()];

        int at = 0;
        for (int x = 1; x < snrank.length; x++) {
            Enumeration e = snrank[x].elements();
            while (e.hasMoreElements()) {
                ordered[at++] = (KSet) e.nextElement();
            }
        }

        //! Hopefully this is correct...
        KSet trivial = ordered[ordered.length - 1];

        //! -----------------------------------------------

        //System.out.println("Constructing SN-tree: first the parent relationship");

        doom:
        for (int l = 0; l < ordered.length - 1; l++)        //! the trivial SN-set is not a subset of anything
            for (int r = (l + 1); r < ordered.length; r++) {
                KSet s = ordered[l];
                KSet t = ordered[r];

                if (s.size() == t.size()) continue;

                if (s.isSubsetOf(t)) {
                    s.setParent(t);
                    continue doom;
                }
            }

        //! -----------------------------------------------
        //! Now we can build the child relationship...

        //System.out.println("Building the SN child relationship...");

        for (int x = 0; x < sn.size(); x++) {
            KSet k = (KSet) sn.elementAt(x);

            KSet p = k.getParent();

            if (p == null) continue;

            p.addChild(k);
        }

        return trivial;
    }

    //! ----------------------------------------------------------------------------

    private final static int TBR_OBJECT = 0;
    private final static int EDGE_OBJECT = 1;

    class StackObject {
        //for both TBRs and edges
        Vector<KSet> list;
        int busyWith;
        int type;

        //! int createdMustHits;

        // for only TBRs
        TripletSet base;
        KSet builtTBR;    //! The TBR that brought us here, labels are correct?
        int bmap[];

        //! for edges only...
        dagExplore netBase;
        int alsoBusyWith;

        public StackObject() {
            this.busyWith = -1;
            this.alsoBusyWith = 0;
            //! this.createdMustHits = 0;
        }
    }

    //! --------------------------------------------------------------------------
    //! Here we go!! Here we go!! Here we go!!
    //! We assume that we specify the upper limit on the level with the 'k' parameter...
    //! It would perhaps be more clever to remove the k parameter but first things first...
    //! So many potential boundary errors...

    //! this returns null if there were no strict simple level-k solutions...

    public Vector buildSimpleNetworks(int k) {
        Vector solutions = null;

        int numTrips = this.tripVec.size();
        float maxpercentage = (float) 0.0;

        Stack<StackObject> stack = new Stack<>();

        Vector<KSet> leaves = this.getLeaves();    //! Is a vector of KSets, each containing a single leaf...

        StackObject so = new StackObject();
        so.list = leaves;
        so.type = TBR_OBJECT;
        so.base = this;        //! Triplets...
        so.builtTBR = null;    //! we didn't remove anything to get here...
        so.bmap = null;        //! likewise

        stack.push(so);

        while (stack.size() != 0) {
            //Simplistic.doublereport("Stack size="+stack.size());

            StackObject top = stack.peek();

            if (top.type == TBR_OBJECT) {
                top.busyWith++;

                if (top.busyWith >= top.list.size()) {
                    //! Then we've done everything we need to here, we drop down the stack
                    // Simplistic.doublereport("Popping stack.");
                    stack.pop();
                    continue;
                }

                //! get the object 'busyWith', do something with it
                //! unless we are really high up...

                KSet ctbr = top.list.elementAt(top.busyWith);

                //! TODO: count the number of cherries in the ctbr...

                //Simplistic.doublereport("Preparing to remove this CTBR");


                TripletSet trip = top.base;

                //! So we're gonna rrrrip ctbr out of trip...

                int leavesLeft = trip.numLeaves - ctbr.size();

                int backmap[] = new int[leavesLeft + 1];
                //! backmap will tell me what the numbers of the remaining leaves, used to be

                TripletSet nextLevel;

                //! int TBRmustHits = 0;

                if (ctbr.size() == 0) {
                    //! We can put some optimisation in here because nothing changes at all

                    nextLevel = trip;    //! I think this is safe....
                    for (int f = 1; f < backmap.length; f++) backmap[f] = f;

                    //! TBRmustHits = 1;			//! dummy leaf adds one mustHit
                } else {
                    //! System.out.println("cbtr has "+ctbr.size()+" leaves.");
                    //! TBRmustHits = ctbr.countCherries();
                    nextLevel = trip.subtractCTBR(ctbr, backmap);
                    nextLevel.finaliseAndOptimise();    //! for speed
                }

                //! System.out.println("// TBR has "+TBRmustHits+" mustHits.");

                if (stack.size() < k) {
                    StackObject penthouse = new StackObject();

                    //Simplistic.doublereport("Preparing penthouse.");

                    Vector<KSet> cand;

                    if (ctbr.size() != 0) {
                        KSet freshTree = nextLevel.buildSNTree();
                        cand = new Vector<>();

                        if (freshTree != null) freshTree.computeCandidateTBRs(cand);

                        /*
                                  if( cand.size() == 0 )
                                      {
                                      //! There should always at least be leaves...or should there???!? CHECK THIS
                                      System.out.println("We have a problem, there were no CTBRs...");
                                      System.exit(0);
                                      }
                                  */

                        //! Add the empty CTBR
                        //! the cloning thing here is just to make sure that the maxLeaf thing is the right size

                        KSet emptyCTBR;

                        if (cand.size() != 0) emptyCTBR = cand.elementAt(0).emptyClone();
                        else {
                            emptyCTBR = new KSet(nextLevel.getNumLeaves());    //! not sure whether this makes sense...
                        }

                        //! System.out.println("Adding the empty CTBR");
                        cand.addElement(emptyCTBR);
                    } else {
                        cand = top.list;    //! Same set of CTBRs!!
                        //! System.out.println("// Empty CBTR, bypassing...");
                    }

                    penthouse.base = nextLevel;
                    penthouse.list = cand;
                    penthouse.type = TBR_OBJECT;
                    penthouse.builtTBR = ctbr;        //! Is a KSet / CTBR
                    penthouse.bmap = backmap;

                    //! penthouse.createdMustHits = top.createdMustHits + TBRmustHits;
                    //! System.out.println("MustHits was"+top.createdMustHits+", is now "+penthouse.createdMustHits+" (ctbr had "+ctbr.size()+" leaves");

                    stack.push(penthouse);

                } else {
                    if (stack.size() > k) {
                        //! The stack should be exactly k-hoog at this point
                        System.out.println("Houston, we have a problem...");
                        System.exit(0);
                    }
                    //! So stack.size == k
                    //! So now we have to build the tree and switch into
                    //! edge stack objects, zuuucht

                    //Simplistic.doublereport("Time to try building the tree...");

                    biDAG mytree;

                    if (leavesLeft > 2) {
                        //Simplistic.doublereport("tree leaves > 2");
                        mytree = nextLevel.buildTree();
                    } else if (leavesLeft == 2) {
                        //Simplistic.doublereport("tree leaves = 2");
                        mytree = biDAG.cherry12();
                    } else if (leavesLeft == 1) {
                        //Simplistic.doublereport("tree leaves = 1");
                        //! network consisting of one leaf...hmmm
                        mytree = new biDAG();
                        mytree.data = 1;
                    } else {
                        //! dummy node
                        //Simplistic.doublereport("tree leaves = 0");
                        mytree = new biDAG();
                        mytree.isDummy = true;    //! it's a dummy leaf!!!
                    }

                    //! need to consider what we will do if we get 'small' trees

                    if (mytree != null) {
                        //! So we got a tree............

                        //! System.out.println("We got a tree!");

                        //! add the fake root

                        biDAG dummyRoot = new biDAG();
                        dummyRoot.child1 = mytree;    //! note that child2 stays null
                        mytree.parent = dummyRoot;

                        //! get all the edges and shove them onto the stack

                        dagExplore dexp = new dagExplore(dummyRoot);

                        //! We need to hit all the mustHit clusters. Each
                        //! TBR can hit 2, but each TBR might add some back in as
                        //! well....

                        //! int lbTBRcontribution = top.createdMustHits + TBRmustHits;

                        //! System.out.println("lower bound TBR contribution: "+lbTBRcontribution);

                        Vector edges = dexp.getEdges();

                        StackObject edgeob = new StackObject();
                        edgeob.type = EDGE_OBJECT;
                        edgeob.list = edges;
                        edgeob.netBase = dexp;

                        //! I think we need this too...
                        edgeob.builtTBR = ctbr;
                        edgeob.bmap = backmap;

                        //! edgeob.createdMustHits = lbTBRcontribution;

                        stack.push(edgeob);
                    }

                }

            } else {
                //! top.type == EDGE_OBJECT

                //! So. We need to consider all pairs of edges, including pairs
                //! that are the same.

                //Simplistic.doublereport("Top of stack is an EDGE_OBJECT, top.list.size() is "+top.list.size());

                boolean popped = false;

                top.busyWith++;
                if (top.busyWith >= top.list.size()) {
                    top.alsoBusyWith++;

                    if (top.alsoBusyWith >= top.list.size()) {
                        //Simplistic.doublereport("Popping EDGE_OBJECT");
                        stack.pop();
                        popped = true;
                    }
                    top.busyWith = top.alsoBusyWith;    //! IF EVERYTHING BREAKS SET TO 0
                }

                if (!popped) {
                    //Simplistic.doublereport("busyWith, alsoBusyWith = "+top.busyWith+","+top.alsoBusyWith);
                    //! Let's hang some TBRs!!

                    dagExplore netBase = top.netBase;
                    Vector edges = top.list;    //! should be the same as netBase's list!

                    biDAG firstEdge[] = (biDAG[]) edges.elementAt(top.busyWith);
                    biDAG secondEdge[] = (biDAG[]) edges.elementAt(top.alsoBusyWith);

                    if ((firstEdge[1].isDummy) && (firstEdge[0].parent != null) && (firstEdge[0].secondParent == null)) {
                        //Simplistic.doublereport("Skipping, no need to subdivide already subdivided dummy edge [first edge]");
                        continue;
                    }

                    if ((secondEdge[1].isDummy) && (secondEdge[0].parent != null) && (secondEdge[0].secondParent == null)) {
                        //Simplistic.doublereport("Skipping, no need to subdivide already subdivided dummy edge [second edge]");
                        continue;
                    }

                    int height = stack.size();
                    int index = (2 * (k + 1)) - height - 1; //! -1 because the stack indexes from 0

                    int tripIndex = index - 1;    //! This is where the triplets are that we will
                    //! compare with;


                    //! --------------------------------------------------------------
                    //! This bit of code can be removed if it doesn't help

                    //! int TBRaccum = (2*k) - height + 1;
                    //! int TBRaccumIndex = TBRaccum - 1;
                    //! StackObject Accum = (StackObject) stack.elementAt(TBRaccumIndex);
                    //! int lbHits = Accum.createdMustHits;
                    //! --------------------------------------------------------------


                    StackObject lookback = stack.elementAt(index);

                    TripletSet oldTrips = stack.elementAt(tripIndex).base;

                    KSet findTheTree = lookback.builtTBR;

                    int switchback[] = lookback.bmap;    //! lets us relabel stuff

                    //! Need to clone the biDAG. This is a very annoying
                    //! thing to do, I must improve this later.

                    //! This should simultaneously relabel the leaves too using the backmap...
                    biDAG newNodes[] = netBase.clonebiDAG(switchback);    //! So we clone the netBase...

                    biDAG newDAG = newNodes[0];

                    biDAG edge1tail = newNodes[firstEdge[0].nodeNum];
                    biDAG edge1head = newNodes[firstEdge[1].nodeNum];

                    biDAG edge2tail = newNodes[secondEdge[0].nodeNum];
                    biDAG edge2head = newNodes[secondEdge[1].nodeNum];

                    //! buildTreeFromCTBR returns a dummy leaf if it was an emptyCTBR...

                    //! this following line needs to be optimised...is sloow
                    biDAG treeToHang = findTheTree.buildTreeFromCTBR();    //! labels should be OK

                    //! if( treeToHang.isDummy ) System.out.println("Hanging a CTBR.");

                    biDAG leave1;
                    biDAG leave2;

                    if ((edge1tail == edge2tail) && (edge1head == edge2head)) {
                        //! System.out.println("Subdividing a single edge.");

                        //! Then we are subdividing one edge
                        leave1 = biDAG.subdivide(edge1tail, edge1head);

                        leave2 = biDAG.subdivide(edge1tail, leave1);    //! experimental, check it works!
                    } else {
                        //! We are subdividing two edges

                        leave1 = biDAG.subdivide(edge1tail, edge1head);

                        leave2 = biDAG.subdivide(edge2tail, edge2head);
                    }

                    biDAG recomb = new biDAG();
                    leave1.child2 = recomb;
                    leave2.child2 = recomb;
                    recomb.parent = leave1;
                    recomb.secondParent = leave2;

                    recomb.child1 = treeToHang;
                    treeToHang.parent = recomb;

                    //! So the root of the new network is newDAG. I haven't removed
                    //! dummy nodes, I will do this only at the end...

                    dagExplore exploreAgain;

                    if (stack.size() == (2 * k)) {
                        //! If we've reached a height of 2k, we're ready to check the triplets!

                        //! newDAG has a fake root so we need to move down a little

                        newDAG = newDAG.child1;
                        newDAG.parent = null;        //! I think this should work...

                        exploreAgain = new dagExplore(newDAG);

                        boolean ok = true;

                        Vector dummies = exploreAgain.getDummies();

                        //! We loop through the dummy nodes, if there is a dummy node whose parent is
                        //! a recomb node we know that it is not a valid solution. Otherwise we
                        //! delete the leaf and suppress its parent. If suppress causes multi-edges
                        //! we also abort.

                        for (int d = 0; d < dummies.size(); d++) {
                            biDAG dummyLeaf = (biDAG) dummies.elementAt(d);
                            if ((dummyLeaf.child1 != null) || (dummyLeaf.child2 != null) || (dummyLeaf.secondParent != null)) {
                                System.out.println("Something went wrong with dummy suppression.");
                                System.exit(0);
                            }

                            biDAG p = dummyLeaf.parent;
                            if ((p.parent != null) && (p.secondParent != null)) {
                                ok = false;    //! dummy leaf with a recombination parent, doomed!
                                //! System.out.println("Suppressed a recombination leaf, forget it.");
                                break;
                            }

                            biDAG grandp = p.parent;

                            biDAG sibling = null;

                            if (p.child1 == dummyLeaf) {
                                sibling = p.child2;
                            } else if (p.child2 == dummyLeaf) {
                                sibling = p.child1;
                            } else {
                                System.out.println("Big error.");
                                System.exit(0);
                            }

                            biDAG parentSibling = null;

                            if (grandp.child1 == p) {
                                parentSibling = grandp.child2;
                            } else if (grandp.child2 == p) {
                                parentSibling = grandp.child1;
                            } else {
                                System.out.println("More error...");
                                System.exit(0);
                            }

                            if (parentSibling == sibling) {
                                //! multiedge !!
                                //! System.out.println("Created a multi-edge, aborting...");
                                ok = false;
                                break;
                            }

                            if (sibling.parent == p) {
                                sibling.parent = grandp;
                            } else if (sibling.secondParent == p) {
                                sibling.secondParent = grandp;
                            } else {
                                System.out.println("Not again...");
                                System.exit(0);
                            }

                            if (grandp.child1 == p) {
                                grandp.child1 = sibling;
                            } else if (grandp.child2 == p) {
                                grandp.child2 = sibling;
                            } else {
                                System.out.println("Again it goes wrong.");
                                System.exit(0);
                            }

                            //! Done!

                        }

                        if (ok) {
                            if (newDAG.isBiconnected()) {
                                boolean isConsistent = true;

                                //! I need a new explorer because i removed all the
                                //! dummy nodes

                                exploreAgain = new dagExplore(newDAG);

                                int numCon = 0;

                                for (int t = 0; t < tripVec.size(); t++) {
                                    int trip[] = (int[]) tripVec.elementAt(t);
                                    if (!exploreAgain.consistent(trip[0], trip[1], trip[2])) {
                                        isConsistent = false;
                                        break;
                                    } else numCon++;
                                }

                                if (isConsistent) {
                                    //! System.out.println("// We found a simple level-"+k+" network consistent with all the triplets!");
                                    if (solutions == null) solutions = new Vector();
                                    solutions.addElement(newDAG);
                                    if (Simplistic.BUILD_ALL == false) return solutions;
                                }

                            }
                        } //! end if ok


                    }    //! end stack.size == (2*l)
                    else {
                        //! push it onto the stack...

                        exploreAgain = new dagExplore(newDAG);

                        //! int stillToHit = exploreAgain.num_mustHits;
                        //! int stillToAdd = (2*k)-stack.size();	//! check this

                        //! Check whether exploreAgain is consistent with oldTrips.
                        //! Does consistency checking work with dummy nodes present?
                        //! I think so.... :o

                        boolean proceed = true;

                        Vector zoom = oldTrips.tripVec;

                        for (int spin = 0; spin < zoom.size(); spin++) {
                            int trip[] = (int[]) zoom.elementAt(spin);
                            if (!exploreAgain.consistent(trip[0], trip[1], trip[2])) {
                                proceed = false;
                                //! System.out.println("Premature departure: early non-consistency detected.");
                                break;
                            }
                        }


                        if (proceed) {
                            StackObject pushnetwork = new StackObject();
                            pushnetwork.type = EDGE_OBJECT;
                            pushnetwork.list = exploreAgain.getEdges();
                            pushnetwork.netBase = exploreAgain;

                            //Simplistic.doublereport("About to push a new EDGE_OBJ onto the stack.");
                            stack.push(pushnetwork);
                        }

                    }


                } // end if(!popped)

            } //! end else

        }


        return solutions;    //! null if there are no solutions
    }


    //! Eliminates duplicates...
    //! Returns also the trivial set I think...
    //! Gives every SN-set a unique index, starting at 0

    private Vector getSNSets() {
        Vector temp = new Vector();

        int n = numLeaves;

        //! Build all singleton SN-sets i.e. of the form SN(x)
        //! These are obviously all disjoint...

        int index = 0;

        for (int x = 1; x <= n; x++) {
            KSet K = new KSet(n);

            //! SNSet indices begin at 0!
            K.setID(index++);

            K.addLeaf(x);

            temp.add(K);

            /*if(Simplistic.DEBUG)
            {
                System.out.println("SN set "+(index-1));
            }  */
        }

        //! Build all sets of the form SN(x,y)
        //!

        for (int x = 1; x <= n; x++) {
            for (int y = (x + 1); y <= n; y++) {
                KSet K = FastComputeSN(x, y);

                boolean alreadyIn = false;

                for (int scan = 0; scan < temp.size(); scan++) {
                    KSet comp = (KSet) temp.elementAt(scan);
                    if (K.sameAs(comp)) {
                        alreadyIn = true;
                        break;
                    }
                }

                if (!alreadyIn) {
                    K.setID(index++);
                    temp.add(K);

                    /*if(Simplistic.DEBUG)
                    {
                        System.out.println("SN set "+(index-1));
                    }*/

                }
            }
        }

        return temp;
    }


    //! Hmmm, this will be interesting. I've hardcoded this (instead of using
    //! the standard internal routines) for speed...be careful if elsewhere internal
    //! subroutines of TripletSet are changed!!

    //! remember: we assume that this. uses every leaf 1 <= leaf <= numLeaves

    //! somehow I am unhappy about this routine, keep an eye on it for bugs...

    public TripletSet subtractCTBR(KSet ctbr, int backmap[]) {
        if (ctbr.getMaxSize() != numLeaves) {
            System.out.println("The CTBR and TripletSet do not match.");
            System.exit(0);
        }

        int sub = ctbr.size();

        int remains = numLeaves - sub;

        if (backmap.length != remains + 1) {
            System.out.println("Backmap[] is broken.");
            System.exit(0);
        }

        //! not sure numLeaves+1 here is necessary but who cares
        int forwardmap[] = new int[numLeaves + 1];

        int at = 1;
        for (int x = 1; x <= numLeaves; x++) {
            if (!ctbr.containsLeaf(x)) {
                backmap[at] = x;
                forwardmap[x] = at;
                at++;
            }
        }

        if (at != remains + 1) {
            System.out.println("Something went very wrong.");
            System.exit(0);
        }

        TripletSet t = new TripletSet();

        for (int scan = 0; scan < tripVec.size(); scan++) {
            int trip[] = (int[]) tripVec.elementAt(scan);

            int x = trip[0];
            int y = trip[1];
            int z = trip[2];

            if (ctbr.containsLeaf(x) || ctbr.containsLeaf(y) || ctbr.containsLeaf(z)) continue;

            int xp = forwardmap[x];
            int yp = forwardmap[y];
            int zp = forwardmap[z];

            int fall[] = new int[3];

            fall[0] = xp;
            fall[1] = yp;
            fall[2] = zp;

            //! No need to arrange the order of the elements:  x < y => xp < yp
            //! check this, but it should be fine...

            t.tripVec.addElement(fall);
        }

        t.numLeaves = remains;

        return t;
    }


    private AhoGraph buildAhoGraph() {
        //! Cycle through all triplets...

        AhoGraph aho = new AhoGraph(numLeaves);

        Enumeration e = tripVec.elements();
        while (e.hasMoreElements()) {
            int t[] = (int[]) e.nextElement();

            aho.addEdge(t[0], t[1]);
        }

        return aho;
    }


    public biDAG buildTree() {
        biDAG papa = new biDAG();

        //! Build the Aho AhoGraph...
        AhoGraph g = buildAhoGraph();

        //! Split into two cliques...

        int partition[] = g.getDoubleClique();

        if (partition == null) return null;

        /*
          if(tripletMethods.DEBUG)
              {
              System.out.println("Had the double-clique structure:");
              for(int k=1; k<partition.length; k++ )
                  {
                  System.out.print(partition[k]+" ");
                  }
              System.out.println();
              }
          */

        //! The partition will have only values LEFT and RIGHT...

        //! At some point we have to stop the recursion. We finish once
        //! one of the two cliques has size <= 2;

        int lCount = 0;
        int rCount = 0;

        //! The 0 element of these guys will not be used...
        int leftList[] = new int[partition.length];
        int rightList[] = new int[partition.length];

        //! The 0 element of these guys will not be used...
        int leftBackMap[] = new int[partition.length];
        int rightBackMap[] = new int[partition.length];

        //! Note here that we begin at 1
        for (int scan = 1; scan < partition.length; scan++) {
            if (partition[scan] == AhoGraph.LEFT) {
                lCount++;

                leftList[lCount] = scan;

                //! This is the new leaf numbering of leaf scan...

                leftBackMap[scan] = lCount;
            } else if (partition[scan] == AhoGraph.RIGHT) {
                rCount++;
                rightList[rCount] = scan;
                rightBackMap[scan] = rCount;
            } else
                System.out.println("ERROR#23");
        }

        //! -------------------------

        biDAG leftChild;
        biDAG rightChild;

        //! Here it gets messy...

        if (lCount == 1) {
            //! In this case it's just a simple leaf...

            leftChild = new biDAG();

            leftChild.data = leftList[1];

            /*
               if(tripletMethods.DEBUG)
                   {
                   System.out.println("Setting only data as "+leftList[1]);
                   }
               */

        } else if (lCount == 2) {
            leftChild = new biDAG();

            //! Here it's a cherry...
            biDAG gchild1 = new biDAG();
            biDAG gchild2 = new biDAG();

            leftChild.child1 = gchild1;
            leftChild.child2 = gchild2;

            gchild1.data = leftList[1];
            gchild2.data = leftList[2];

            /*
               if(tripletMethods.DEBUG)
                   {
                   System.out.println("Setting gchild1 as "+leftList[1]);
                   System.out.println("Setting gchild2 as "+leftList[2]);
                   }
               */

            gchild1.parent = leftChild;
            gchild2.parent = leftChild;

        } else {
            /*
               if( tripletMethods.DEBUG )
                   {
                   System.out.println("Recursing left...");
                   }
               */

            //! and here we recurse...
            //! get all the triplets in the LEFT partition...
            //! and continue...

            TripletSet leftBranch = new TripletSet();

            Enumeration e = tripVec.elements();
            while (e.hasMoreElements()) {
                int v[] = (int[]) e.nextElement();

                //! We want all 3 of its elements to be in the left
                //! partition...so for each element check whether it

                if ((partition[v[0]] == AhoGraph.RIGHT) || (partition[v[1]] == AhoGraph.RIGHT) || (partition[v[2]] == AhoGraph.RIGHT))
                    continue;

                //! the backscan variable ensures that the indices are correct...i.e. in
                //! the range [1...leftCount]

                leftBranch.addTriplet(leftBackMap[v[0]], leftBackMap[v[1]], leftBackMap[v[2]]);
            }

            leftChild = leftBranch.buildTree();

            if (leftChild == null) return null;

            /*
               if( tripletMethods.DEBUG ) System.out.println("Past left recursion");
               */

            //! Fix labelling in the tree...very crude but zucht let's get it working first...
            leftChild.treeFixLeaves(leftList);
        }

        //! and now the right branch...

        if (rCount == 1) {
            //! In this case it's just a simple leaf...

            rightChild = new biDAG();
            rightChild.data = rightList[1];

            /*
               if(tripletMethods.DEBUG)
                   {
                   System.out.println("Setting only data as"+rightList[1]);
                   }
               */

        } else if (rCount == 2) {
            rightChild = new biDAG();

            //! Here it's a cherry...
            biDAG gchild1 = new biDAG();
            biDAG gchild2 = new biDAG();

            rightChild.child1 = gchild1;
            rightChild.child2 = gchild2;

            gchild1.data = rightList[1];
            gchild2.data = rightList[2];

            gchild1.parent = rightChild;
            gchild2.parent = rightChild;

            /*
               if(tripletMethods.DEBUG)
                   {
                   System.out.println("Setting gchild1 as "+rightList[1]);
                   System.out.println("Setting gchild2 as "+rightList[2]);
                   }
               */

        } else {

            /*
               if( tripletMethods.DEBUG )
                   {
                   System.out.println("Recursing...");
                   }
               */

            //! and here we recurse...
            //! get all the triplets in the RIGHT partition...
            //! and continue...

            TripletSet rightBranch = new TripletSet();

            Enumeration e = tripVec.elements();
            while (e.hasMoreElements()) {
                int v[] = (int[]) e.nextElement();

                //! We want all 3 of its elements to be in the left
                //! partition...so for each element check whether it
                //! is in the LEFT set...

                if ((partition[v[0]] == AhoGraph.LEFT) || (partition[v[1]] == AhoGraph.LEFT) || (partition[v[2]] == AhoGraph.LEFT))
                    continue;

                //! the backscan variable ensures that the indices are correct...i.e. in
                //! the range [1...rightCount]

                rightBranch.addTriplet(rightBackMap[v[0]], rightBackMap[v[1]], rightBackMap[v[2]]);
            }

            rightChild = rightBranch.buildTree();

            if (rightChild == null) return null;

            /*
               if( tripletMethods.DEBUG )
                   {
                   System.out.println("Past recursive step!");
                   }
               */

            //! And now fix the leaf numbering...
            rightChild.treeFixLeaves(rightList);
        }

        papa.child1 = leftChild;
        papa.child2 = rightChild;
        leftChild.parent = papa;
        rightChild.parent = papa;

        return papa;
    }


    public biDAG buildNetwork(int depth, int levelToTry) {
        System.out.println("Entering buildNetwork, recursion level " + depth);

        //System.out.println("Constructing the maximum SN-sets...");

        KSet sntree = this.buildSNTree();

        Vector msn = sntree.getChildren();    //! That should be thje maximum SN-sets...check this...

        //! Is there a problem if msn is 'small' ?

        System.out.println("...done. There are " + msn.size() + " maximum SN-sets.");

        //! for( int x=0; x<msn.size(); x++ ) ((KSet) msn.elementAt(x)).dump();

        biDAG root = null;
        Vector mstar;

        boolean success = false;

        if (msn.size() == 2) {
            System.out.println("There were only 2 maximum SN-sets.");
            root = biDAG.cherry12();
            mstar = msn;
            success = true;
        } else    //! there are at least 3 maximum SN-sets
        {
            mstar = msn;

            TripletSet treal = this.buildTPrime(mstar);
            treal.finaliseAndOptimise();

            int nprime = treal.getNumLeaves();
            int toplev;

            //if( Simplistic.MAXLEV > (nprime-1) ) toplev = Simplistic.MAXLEV; else toplev = nprime-1;  //no, i want to be able to stop before


            System.out.println("// There are " + nprime + " maximum SN-sets at recursion level " + depth + ".");
            //for( int b=0; b<mstar.size(); b++ ) ((KSet) mstar.elementAt(b)).dump();

            chains:
            for (int level = levelToTry; level <= levelToTry; level++) {
                mstar = msn;

                Vector result = treal.buildSimpleNetworks(level);

                if (result != null) {
                    success = true;
                    System.out.println("// Found a" + " consistent " + "simple level-" + level + " network!");
                    root = (biDAG) result.elementAt(0);
                    //break;
                } else {
                    System.out.println("// Couldn't find a simple level-" + level + " network.");
                    if (Simplistic.SN_TO_SPLIT > 0) {
                        System.out.println("// Attempting to split maximal SN-sets before moving on to level " + (level + 1) + ".");
                        //! Let's try splitting some SN-sets.....

                        for (int split = 1; split <= Simplistic.SN_TO_SPLIT; split++) {
                            mstar = msn;

                            //! Build a set of all size-split subsets of
                            //! the maximum subsets, try each one (zucht...)

                            if (split > mstar.size()) break;

                            System.out.println("// Trying to split all combinations of " + split + " maximum SN-sets.");

                            CombinationGenerator cg = new CombinationGenerator(mstar.size(), split);

                            nextcom:
                            while (cg.hasMore()) {
                                mstar = msn;

                                int gc[] = cg.getNext();
                                for (int aGc2 : gc) {
                                    if (((KSet) mstar.elementAt(aGc2)).size() == 1)
                                        continue nextcom;    // can't be split...
                                }

                                for (int aGc1 : gc) {
                                    System.out.println(aGc1 + ",");
                                }
                                System.out.println("\n");

                                Vector newVec = new Vector();

                                //! First add the unsplit SN-sets in...

                                outerfor:
                                for (int roll = 0; roll < mstar.size(); roll++) {
                                    for (int aGc : gc) {
                                        if (aGc == roll) {
                                            continue outerfor;
                                        }
                                    }
                                    newVec.addElement(mstar.elementAt(roll));
                                }

                                for (int aGc : gc) {
                                    KSet p = (KSet) mstar.elementAt(aGc);
                                    Vector c = p.getChildren();
                                    for (int y = 0; y < c.size(); y++) {
                                        newVec.addElement(c.elementAt(y));
                                    }
                                }


                                System.out.println("Split SN-sets are now: ");
                                for (int l = 0; l < newVec.size(); l++) {
                                    KSet p = (KSet) newVec.elementAt(l);
                                }

                                mstar = newVec;

                                TripletSet tsplit = this.buildTPrime(mstar);
                                tsplit.finaliseAndOptimise();

                                int nsplit = tsplit.getNumLeaves();

                                System.out.println("// After splitting there are " + nsplit + " maximum SN-sets");

                                Vector splitresult = tsplit.buildSimpleNetworks(level);
                                if (splitresult != null) {
                                    success = true;
                                    System.out.println("// Found a simple level-" + level + " network!");
                                    root = (biDAG) splitresult.elementAt(0);
                                    break chains;
                                }
                            }

                            //! Don't forget to adjust mstar if it works...
                        }
                        System.out.println("// Couldn't find a simple level-" + level + " subnetwork, even after splitting.");

                    } //! end SN_TO_SPLIT > 0

                }
            }
        }

        if (!success) return null;    //! This should never need to happen...

        //! Now do the funky stuff....
        //! root will contain a network with as many leaves as maximum SN-sets...
        //! i.e. the size of mstar...

        biDAG leafToNode[] = new biDAG[mstar.size() + 1];

        //! This assumes that 'visited' is clean...

        root.resetVisited();

        root.getDAGLeafMap(leafToNode);

        //! Now leadToNode[1] points to the biDAG containing element 1, and so on...
        //! ---------------------------------------------------------------------------
        //! Not sure if we need to do this, the only thing that could get hurt is
        //! the printing out of the DAG, but I think it's important to do it anyway...
        //! ---------------------------------------------------------------------------

        root.resetVisited();

        //! xl -> expand leaf...
        for (int xl = 1; xl <= mstar.size(); xl++) {
            KSet subLeaves = (KSet) mstar.elementAt(xl - 1);
            int numSubLeaves = subLeaves.size();

            int backMap[] = new int[numSubLeaves + 1];

            biDAG subroot;

            System.out.println("Looking inside maxsn set " + xl);

            if (numSubLeaves > 2) {
                System.out.println("Ah, this has more than 3 leaves...");
                TripletSet recurse = this.induceTripletSet(subLeaves, backMap);

                recurse.finaliseAndOptimise();

                subroot = recurse.buildNetwork(depth + 1, levelToTry);
                if (subroot == null) return null;
            } else {
                if (numSubLeaves == 1) {
                    System.out.println("Ah, this is a single leaf...");
                    //! Simply fix what's already there.
                    biDAG tweak = leafToNode[xl];
                    int leafNum = subLeaves.getFirstElement();
                    System.out.println("Replacing leaf " + tweak.data + " with " + leafNum);
                    tweak.data = leafNum;
                } else

                {
                    //! numSubLeaves == 2

                    System.out.println("Ah, this has two leaves.");
                    System.out.println("Entering danger zone");

                    biDAG tweak = leafToNode[xl];

                    int pick[] = subLeaves.getFirstTwoElements();

                    biDAG branch = biDAG.cherry12();

                    branch.child1.data = pick[0];
                    branch.child2.data = pick[1];

                    branch.parent = tweak.parent;

                    if (tweak.parent == null) System.out.println("Null pointer discovered...");

                    if (tweak.parent.child1 == tweak) tweak.parent.child1 = branch;
                    else if (tweak.parent.child2 == tweak) tweak.parent.child2 = branch;
                    else {
                        System.out.println("Mega problem, please report to programmer");
                        System.exit(0);
                    }

                    System.out.println("Leaving danger zone?");

                }
                continue;    //! no need to do the rest...
            }

            //! That subbranch worked, so fix the leaves and then graft back

            System.out.println("Still at recursion depth " + depth);

            subroot.dagFixLeaves(backMap);

            //! Is this necessary??? Might it be dangerous even???
            subroot.resetFixLeaves();

            biDAG knoop = leafToNode[xl];

            subroot.parent = knoop.parent;

            if (knoop.parent.child1 == knoop) knoop.parent.child1 = subroot;
            else if (knoop.parent.child2 == knoop) knoop.parent.child2 = subroot;
            else {
                System.out.println("BL2: That really shouldn't have happened");
                System.exit(0);
            }

            System.out.println("Ending iteration.");
        }


        return root;
    }

    public TripletSet buildTPrime(Vector maxSN) {
        if (maxSN.size() == 2) return null;

        int nprime = maxSN.size();

        KSet fastSN[] = new KSet[nprime + 1];

        //! We'll build a lookup-table here to make the mapping fast
        //! Maps {1,n} -> {1,n'}

        int map[] = new int[this.numLeaves + 1];

        int index = 1;

        Enumeration e = maxSN.elements();
        while (e.hasMoreElements()) {
            fastSN[index] = (KSet) e.nextElement();
            for (int scan = 1; scan <= numLeaves; scan++) {
                if (fastSN[index].containsLeaf(scan)) map[scan] = index;
            }
            index++;
        }

        //! Ok, the maxSN sets are now in the array fastSN, we don't
        //! use the zero element...

        //! Simply iterate through all the triplets in 'this', and induce the corresponding set
        //! of new triplets...

        TripletSet tprime = new TripletSet();

        Enumeration f = tripVec.elements();
        while (f.hasMoreElements()) {
            int t[] = (int[]) f.nextElement();

            int a = map[t[0]];
            int b = map[t[1]];
            if (b == a) continue;

            int c = map[t[2]];
            if ((c == a) || (c == b)) continue;

            tprime.addTriplet(map[t[0]], map[t[1]], map[t[2]]);
        }

        return tprime;
    }

    //! Give it an array of which leaves you want to do the inducing
    //! backMap says what the original labellings of the new leaves were...

    public TripletSet induceTripletSet(KSet k, int backMap[]) {
        TripletSet ts = new TripletSet();

        boolean in[] = new boolean[numLeaves + 1];

        int leafMap[] = new int[numLeaves + 1];
        int mapCount = 1;
        //! This will map {1...numLeaves} -> {1...size(KSet)}

        for (int x = 1; x <= numLeaves; x++) {
            in[x] = k.containsLeaf(x);
            if (in[x]) {
                leafMap[x] = mapCount;

                backMap[mapCount] = x;

                mapCount++;
            }
        }

        Enumeration e = this.tripVec.elements();

        while (e.hasMoreElements()) {
            int t[] = (int[]) e.nextElement();

            if (in[t[0]] && in[t[1]] && in[t[2]]) {
                ts.addTriplet(leafMap[t[0]], leafMap[t[1]], leafMap[t[2]]);
            }

        }

        return ts;
    }

    void printMatrix() {

        for (int i = 0; i < numLeaves; i++) {
            for (int j = i; j < numLeaves; j++) {
                for (int z = 0; z < numLeaves; z++) {
                    if (this.containsTripletFast(i, j, z)) {
                        System.out.println(i + "," + j + "|" + z);
                    }
                }
            }
        }
    }

    void printList() {

        Enumeration e = tripVec.elements();
        //tripl
        while (e.hasMoreElements()) {
            int t[] = (int[]) e.nextElement();
            System.out.println(t[0] + "," + t[1] + "|" + t[2]);

        }
    }


}



