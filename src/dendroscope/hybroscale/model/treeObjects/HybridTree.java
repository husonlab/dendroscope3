/*
 *   HybridTree.java Copyright (C) 2020 Daniel H. Huson
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

import java.util.Iterator;
import java.util.Vector;

/**
 * This class represents a rooted, bifurcating phylogenetic tree.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class HybridTree extends HybridNetwork {

    public HybridTree(MyPhyloTree t, boolean rootTree, Vector<String> taxaOrdering) {
        super(t, rootTree, taxaOrdering);
    }

    public HybridTree(HybridNetwork t, boolean rootTree, Vector<String> taxaOrdering) {
        super(t, rootTree, taxaOrdering);
    }

    // returns subtree under node v
    public HybridTree getSubtree(MyNode v, boolean doUpdate) {
        MyPhyloTree sT = new MyPhyloTree();
        if (contains(v)) {
            MyNode vCopy = sT.newNode(v);
            sT.setLabel(vCopy, getLabel(v));
            sT.setRoot(vCopy);
            createSubtreeRec(v, vCopy, sT);
        }
        HybridTree newTree = new HybridTree(sT, false, super.getTaxaOrdering());
        if (doUpdate)
            newTree.update();

        Iterator<Vector<String>> it = this.getTaxaPairToWeight().keySet().iterator();
        while (it.hasNext()) {
            Vector<String> key = it.next();
            int value = this.getTaxaPairToWeight().get(key);
            newTree.taxaPairToWeight.put(key, value);
        }

        return newTree;
    }

    public void deleteNode(MyNode v) {
        MyNode p = v.getInDegree() != 0 ? v.getFirstInEdge().getSource() : null;
        super.deleteNode(v);
        if (p != null && p.getInDegree() == 1 && p.getOutDegree() == 1) {
            MyNode s = p.getFirstInEdge().getSource();
            MyNode t = p.getFirstOutEdge().getTarget();
            t.setSolid(p.isSolid());
            s.removeOutEdge(p.getFirstInEdge());
            t.removeInEdge(p.getFirstOutEdge());
            newEdge(s, t);
        }
    }

    @SuppressWarnings("unchecked")
    private void createSubtreeRec(MyNode v, MyNode vCopy, MyPhyloTree t) {
        Iterator<MyEdge> it = getOutEdges(v);
        while (it.hasNext()) {
            MyEdge e = it.next();
            MyNode c = e.getTarget();
            MyNode cCopy;
            cCopy = t.newNode(c);
            t.setLabel(cCopy, getLabel(c));
            t.newEdge(vCopy, cCopy);
            createSubtreeRec(c, cCopy, t);
        }
    }

}
