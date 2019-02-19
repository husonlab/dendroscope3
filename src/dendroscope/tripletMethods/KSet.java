/**
 * KSet.java 
 * Copyright (C) 2019 Daniel H. Huson
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
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: scornava
 * Date: Jul 12, 2010
 * Time: 12:41:36 PM
 * To change this template use File | Settings | File Templates.
 */
class KSet {
    private final int leafMax;
    private final boolean[] array;
    private int leafCount;

    private int id;
    private KSet parent;
    private final Vector children;

    private int isCTBR;
    private final static int UNKNOWN = 0;
    private final static int NO = -1;
    private final static int YES = 1;

    public void setID(int idval) {
        this.id = idval;
    }

    public int getID() {
        return id;
    }

    public void setParent(KSet p) {
        this.parent = p;
    }

    public KSet getParent() {
        return parent;
    }

    public KSet emptyClone() {
        return new KSet(this.leafMax);
    }

    //! Should I check to see if it's unique?
    public void addChild(KSet p) {
        children.add(p);
    }

    public int countChildren() {
        return children.size();
    }

    public Vector getChildren() {
        return children;
    }

    //! Only call this when the SN-tree rooted at this. is binary

    public int countCherries() {
        int numC = this.children.size();

        if (numC == 0) return 0;

        if (numC != 2) {
            System.out.println("THIS SHOULD NEVER HAPPEN!");
            System.exit(0);
        }

        KSet ch1 = (KSet) children.elementAt(0);
        KSet ch2 = (KSet) children.elementAt(1);

        int tot = ch1.countCherries() + ch2.countCherries();

        if ((ch1.leafCount == 1) && (ch2.leafCount == 1)) tot++;

        return tot;
    }

    public KSet(int leaves) {
        this.leafMax = leaves;
        array = new boolean[leafMax];
        children = new Vector();
    }

    public void addLeaf(int l) {
        if (array[l - 1] == false) leafCount++;
        array[l - 1] = true;
    }

    public void removeLeaf(int l) {
        if (array[l - 1] == true) leafCount--;
        array[l - 1] = false;
    }

    public boolean containsLeaf(int l) {
        return l <= leafMax && array[l - 1];
    }

    //! Returns true if this is a subset of 'two'

    public boolean isSubsetOf(KSet two) {
        if (this.leafCount > two.leafCount) return false;

        for (int x = 0; x < array.length; x++) {
            if ((array[x] == true) && (two.array[x] == false)) return false;
        }
        return true;
    }

    public int size() {
        return leafCount;
    }

    public boolean empty() {
        return (leafCount == 0);
    }

    public int getFirstElement() {
        for (int x = 0; x < array.length; x++) {
            if (array[x] == true) return (x + 1);
        }

        System.out.println("ERROR#1: Set was empty!");
        return -1;
    }

    public int[] getFirstTwoElements() {
        if (leafCount < 2) return null;

        int vec[] = new int[2];
        int at = 0;

        for (int x = 0; x < array.length; x++) {
            if (array[x]) {
                vec[at++] = (x + 1);
                if (at == 2) return vec;
            }

        }
        return null;
    }


    public boolean sameAs(KSet second) {
        int smaller, bigger;
        boolean longer[];

        if (this.leafCount != second.leafCount) return false;

        if (leafMax < second.leafMax) {
            smaller = leafMax;
            bigger = second.leafMax;
            longer = second.array;
        } else {
            smaller = second.leafMax;
            bigger = leafMax;
            longer = this.array;
        }

        for (int x = 0; x < smaller; x++) {
            if (array[x] != second.array[x]) return false;
        }

        for (int x = smaller; x < bigger; x++) {
            if (longer[x]) return false;
        }

        return true;
    }


    public int getMaxSize() {
        return leafMax;
    }


    //! After this has been done, the isCTBR flags for the
    //! KSets in the SN-tree will be correct. The beautiful thing
    //! is that the structure of the SN-tree under a CTBR exactly
    //! represents the tree that it builds. Also nice: each
    //! KSet is equal to the set of leaves underneath it.

    //! returns the set of CTBRs in the Vector ctbrs, which is assumed
    //! to be empty at the beginning.

    //! Note that it DOES NOT return the empty CBTR, this has to be
    //! handled explicitly as a special case.

    public void computeCandidateTBRs(Vector ctbrs) {
        if (isCTBR != UNKNOWN) {
            System.out.println("Error: computeCandidateTBRs has been called more than once!");
            System.exit(0);
        }

        int numKids = children.size();

        if (numKids == 0) {
            isCTBR = YES;
            ctbrs.addElement(this);
            return;
        }

        Enumeration e = children.elements();

        boolean kidsAreGood = true;

        while (e.hasMoreElements()) {
            KSet kid = (KSet) e.nextElement();

            kid.computeCandidateTBRs(ctbrs);

            if (kid.isCTBR == NO) kidsAreGood = false;
        }

        if (numKids != 2) {
            isCTBR = NO;
            return;
        }

        isCTBR = kidsAreGood ? YES : NO;

        if (isCTBR == YES) {
            ctbrs.addElement(this);
        }

    }

    //! private int leafMax;
    //! private boolean array[];
    //! private int leafCount;

    public KSet negateKSet() {
        KSet neg = new KSet(leafMax);

        for (int x = 1; x <= array.length; x++) {
            if (this.containsLeaf(x)) {
                neg.removeLeaf(x);
            } else {
                neg.addLeaf(x);
            }
        }
        return neg;
    }


    public biDAG buildTreeFromCTBR() {
        //! System.out.println("Building tree from CTBR");

        if (leafCount == 0) {
            //! This is an emptyTBR, return a dummy biDAG

            //! System.out.println("Returning a dummy biDAG");

            biDAG b = new biDAG();
            b.data = 0;
            b.isDummy = true;

            return b;
        }

        biDAG node = new biDAG();

        int kids = children.size();

        if ((kids != 0) && (kids != 2)) {
            System.out.println("Something went very very wrong with the CTBRs.");
        }
        if (kids == 2) {
            KSet ch1 = (KSet) children.elementAt(0);
            KSet ch2 = (KSet) children.elementAt(1);

            biDAG lchild = ch1.buildTreeFromCTBR();
            biDAG rchild = ch2.buildTreeFromCTBR();

            node.child1 = lchild;
            node.child2 = rchild;

            lchild.parent = node;
            rchild.parent = node;
        } else {
            //! a leaf...
            node.data = this.getFirstElement();
            //! System.out.println("Node data="+node.data);
        }

        return node;
    }


}
