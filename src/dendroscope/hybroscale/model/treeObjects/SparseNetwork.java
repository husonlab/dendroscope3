/*
 *   SparseNetwork.java Copyright (C) 2020 Daniel H. Huson
 *
 *   (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dendroscope.hybroscale.model.treeObjects;

import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class SparseNetwork {

    private SparseNetNode root;
    private int retNumber;
    private Vector<SparseNetEdge> edges = new Vector<SparseNetEdge>();
    private Vector<SparseNetNode> leaves = new Vector<SparseNetNode>();
    private Object info;

    public SparseNetwork() {
        root = new SparseNetNode(null, this, null);
    }

    public SparseNetwork(SparseNetNode root) {
        this.root = root;
        root.setOwner(this);
        leaves.add(root);
    }

    public SparseNetwork(MyPhyloTree t) {
        copy(t);
    }

    public SparseNetwork(SparseNetwork t) {
        SparseNetNode v = t.getRoot();
        root = new SparseNetNode(null, this, v.getLabel());
        copyTreeRec(t, v, root, new Hashtable<SparseNetNode, SparseNetNode>());
    }

    private void copyTreeRec(SparseNetwork t, SparseNetNode v,
                             SparseNetNode vCopy, Hashtable<SparseNetNode, SparseNetNode> visited) {
        visited.put(v, vCopy);
        for (SparseNetEdge e : v.outEdges()) {
            SparseNetNode c = e.getTarget();
            if (visited.containsKey(c)) {
                SparseNetNode cCopy = visited.get(c);
                SparseNetEdge eCopy = vCopy.addChild(cCopy);
                eCopy.addIndices((HashSet<Integer>) e.getIndices().clone());
                eCopy.addEdgeIndices((HashSet<Integer>) e.getEdgeIndex().clone());
            } else {
                SparseNetNode cCopy = new SparseNetNode(vCopy, this,
                        c.getLabel());
                SparseNetEdge eCopy = cCopy.inEdges().iterator().next();
                eCopy.addIndices((HashSet<Integer>) e.getIndices().clone());
                eCopy.addEdgeIndices((HashSet<Integer>) e.getEdgeIndex().clone());
                if (c.getOutDegree() != 0)
                    copyTreeRec(t, c, cCopy, visited);
            }
        }
    }

    private void copy(MyPhyloTree t) {
        MyNode v = t.getRoot();
        root = new SparseNetNode(null, this, t.getLabel(v));
        if (t.getLabel(v) != null)
            root.setLabel(t.getLabel(v));
        copyTreeRec(t, v, root, new Hashtable<MyNode, SparseNetNode>());
    }

    private void copyTreeRec(MyPhyloTree t, MyNode v, SparseNetNode vCopy,
                             Hashtable<MyNode, SparseNetNode> visited) {
        visited.put(v, vCopy);
        Iterator<MyEdge> it = v.outEdges().iterator();
        while (it.hasNext()) {
            MyEdge e = it.next();
            MyNode c = e.getTarget();
            if (visited.containsKey(c)) {
                SparseNetNode cCopy = visited.get(c);
                vCopy.addChild(cCopy, e.getInfo());
            } else {
                SparseNetNode cCopy = new SparseNetNode(vCopy, this,
                        t.getLabel(c), e.getInfo());
                if (c.getOutDegree() != 0)
                    copyTreeRec(t, c, cCopy, visited);
            }
        }
    }

    public void removeLeaf(SparseNetNode v) {
        if (leaves.contains(v))
            leaves.remove(v);
    }

    public void addLeaf(SparseNetNode v) {
        if (!leaves.contains(v))
            leaves.add(v);
    }

    public void addLabel(SparseNetNode v, String s) {
        v.setLabel(s);
    }

    public MyPhyloTree getPhyloTree() {
        MyPhyloTree t = new MyPhyloTree();
        if (root != null) {
            MyNode rootCopy = t.newNode();
            if (root.getLabel() != null)
                t.setLabel(rootCopy, root.getLabel());
            getPhyloTreeRec(t, root, rootCopy,
                    new Hashtable<SparseNetNode, MyNode>());
            t.setRoot(rootCopy);
        }
        return t;
    }

    private void getPhyloTreeRec(MyPhyloTree t, SparseNetNode v, MyNode vCopy, Hashtable<SparseNetNode, MyNode> visited) {
        visited.put(v, vCopy);
        for (SparseNetEdge eOut : v.outEdges()) {
            SparseNetNode c = eOut.getTarget();
            MyEdge newEdge = null;
            if (visited.containsKey(c)) {
                MyNode cCopy = visited.get(c);
                if (c.getLabel() != null)
                    t.setLabel(cCopy, c.getLabel());
                newEdge = t.newEdge(vCopy, cCopy);
                Iterator<MyEdge> it = cCopy.inEdges().iterator();
                while (it.hasNext()) {
                    MyEdge e = it.next();
                    t.setSpecial(e, true);
                    t.setWeight(e, 0);
                }
            } else {
                MyNode cCopy = t.newNode();
                if (c.getLabel() != null)
                    t.setLabel(cCopy, c.getLabel());
                newEdge = t.newEdge(vCopy, cCopy);
                getPhyloTreeRec(t, c, cCopy, visited);
            }
            newEdge.setInfo(eOut.getIndices());
        }
    }

    public void removeEdge(SparseNetEdge e) {
        edges.remove(e);
    }

    public boolean isSpecial(SparseNetEdge e) {
        if (e.getTarget().getInDegree() > 1)
            return true;
        return false;
    }

    public void addEdges(SparseNetEdge e) {
        edges.add(e);
    }

    public Vector<SparseNetEdge> getEdges() {
        return edges;
    }

    public HashSet<SparseNetNode> getNodes() {
        HashSet<SparseNetNode> myNodes = new HashSet<SparseNetNode>();
        myNodes.add(root);
        for (SparseNetEdge e : edges) {
            SparseNetNode v = e.getTarget();
            myNodes.add(v);
        }
        return myNodes;
    }

    public Vector<SparseNetNode> getLeaves() {
        return leaves;
    }

    public SparseNetNode getRoot() {
        return root;
    }

    public void setRoot(SparseNetNode v) {
        root = v;
    }

    public void setLabel(SparseNetNode v, String s) {
        v.setLabel(s);
    }

    public int getRetNumber() {
        return retNumber;
    }

    public void setNumber(int retNumber) {
        this.retNumber = retNumber;
    }

    public Object getInfo() {
        return info;
    }

    public void setInfo(Object info) {
        this.info = info;
    }

}
