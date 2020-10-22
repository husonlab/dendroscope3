/*
 *   CheckAddTaxaNode.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.util;

import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

public class CheckAddTaxaNode {

    private MyPhyloTree result;
    private HashSet<MyNode> trivialNodes;

    public MyPhyloTree getResult() {
        return result;
    }

    public int run(MyPhyloTree n, int border, boolean storeNetworkInfo) {

        trivialNodes = new HashSet<MyNode>();
        HashMap<MyNode, HashSet<MyNode>> nodeToPreds = new HashMap<MyNode, HashSet<MyNode>>();
        assingPreds(n.getRoot(), new HashSet<MyNode>(), nodeToPreds);

        resolveTrivialInteractions(n, nodeToPreds);

        Vector<MyNode> retNodes = new Vector<MyNode>();
        for (MyNode v : n.getNodes()) {
            if (v.getInDegree() > 1 && !trivialNodes.contains(v))
                retNodes.add(v);
        }

        for (int k = trivialNodes.size(); k <= border; k++) {
            HashSet<Vector<MyNode>> allSubsets = new HashSet<Vector<MyNode>>();
            getAllSubsets(0, retNodes, k - trivialNodes.size(), new Vector<MyNode>(), allSubsets);
            for (Vector<MyNode> subset : allSubsets) {
                clearEdges(n);
                setEdges(subset);
                if (checkTree(n)) {
                    if (storeNetworkInfo) {
                        n.setAddTaxaDegree(k);
                        setEdges(subset);
                    } else
                        clearEdges(n);
                    return k;
                }
            }
        }

        return -1;

    }

    private boolean checkTree(MyPhyloTree net) {
        MyPhyloTree n = new MyPhyloTree(net);
        HashSet<MyEdge> criticalEdges = new HashSet<MyEdge>();
        for (MyEdge e : n.getEdges()) {
            if (e.isCritical())
                criticalEdges.add(e);
        }
        for (MyEdge e : criticalEdges) {
            MyNode s = e.getSource();
            MyNode t = e.getTarget();
            s.removeOutEdge(e);
            MyNode x = n.newNode();
            n.newEdge(s, x);
            n.newEdge(x, t);
        }
        CheckTimeConsistency checker = new CheckTimeConsistency();
        int k = checker.run(n, 0, false, true, -1);
        if (k != 0)
            return false;
        return true;
    }

    private void setEdges(Vector<MyNode> subset) {
        for (MyNode v : subset) {
            Iterator<MyEdge> it = v.inEdges().iterator();
            while (it.hasNext())
                it.next().setCritical(true);
        }
        for (MyNode v : trivialNodes) {
            Iterator<MyEdge> it = v.inEdges().iterator();
            while (it.hasNext())
                it.next().setCritical(true);
        }
    }

    private void clearEdges(MyPhyloTree n) {
        for (MyEdge e : n.getEdges()) {
            e.setCritical(false);
        }
    }

    private void getAllSubsets(int index, Vector<MyNode> retNodes, int border, Vector<MyNode> subset,
                               HashSet<Vector<MyNode>> allSubsets) {
        if (subset.size() < border) {
            for (int i = index; i < retNodes.size(); i++) {
                MyNode v = retNodes.get(i);
                if (!subset.contains(v)) {
                    Vector<MyNode> newSubset = (Vector<MyNode>) subset.clone();
                    newSubset.add(v);
                    int newIndex = i + 1;
                    getAllSubsets(newIndex, retNodes, border, newSubset, allSubsets);
                }
            }
        } else
            allSubsets.add(subset);

    }

    private void resolveTrivialInteractions(MyPhyloTree n, HashMap<MyNode, HashSet<MyNode>> nodeToPreds) {
        HashSet<MyNode> retNodes = new HashSet<MyNode>();
        for (MyNode v : n.getNodes()) {
            if (v.getInDegree() > 1)
                retNodes.add(v);
        }
        HashSet<MyEdge> toResolve = new HashSet<MyEdge>();
        for (MyNode v : retNodes) {
            Vector<MyNode> parentNodes = new Vector<MyNode>();
            Vector<MyEdge> parentEdges = new Vector<MyEdge>();
            Iterator<MyEdge> it = v.inEdges().iterator();
            while (it.hasNext()) {
                MyEdge e = it.next();
                parentNodes.add(e.getSource());
                parentEdges.add(e);
            }
            for (MyNode p1 : parentNodes) {
                for (MyNode p2 : parentNodes) {
                    if (nodeToPreds.get(p1).contains(p2))
                        toResolve.add(parentEdges.get(parentNodes.indexOf(p2)));
                }
            }
        }
        for (MyEdge e : toResolve)
            trivialNodes.add(e.getTarget());

    }

    private void assingPreds(MyNode v, HashSet<MyNode> preds,
                             HashMap<MyNode, HashSet<MyNode>> nodeToPreds) {
        boolean isCritical = false;
        Iterator<MyEdge> it = v.outEdges().iterator();
        while (it.hasNext()) {
            MyEdge e = it.next();
            if (v.getOwner().isSpecial(e)) {
                nodeToPreds.put(v, (HashSet<MyNode>) preds.clone());
                isCritical = true;
                break;
            }
        }
        if (isCritical)
            preds.add(v);
        it = v.outEdges().iterator();
        while (it.hasNext()) {
            MyEdge e = it.next();
            assingPreds(e.getTarget(), (HashSet<MyNode>) preds.clone(), nodeToPreds);
        }
    }

}
