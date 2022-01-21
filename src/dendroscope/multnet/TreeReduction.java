/*
 * TreeReduction.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.multnet;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.util.HashMap;
import java.util.Iterator;

/*
 * tries to delete redundant informations in the given tree. given node v we collect all children n_1,n_2,...,n_l of v and
 * check wheter a pair of T(n_1),T(n_2),...,T(n_l) is isomorphic. in this case we delete one of these subtrees.
 */

public class TreeReduction {

    private final PhyloTree tree;

    public TreeReduction(PhyloTree t) {
        this.tree = t;
    }

    public MultilabeledTree apply() {
        MultilabeledTree reducedTree = new MultilabeledTree(this.tree);
        applyRec(reducedTree, reducedTree.getRoot());
        return reducedTree;
    }

    private void applyRec(MultilabeledTree reducedTree, Node n) {
        HashMap<Integer, HeightList> childrenHeightlists = new HashMap<>();
        Iterator<Edge> childrenIt = n.outEdges().iterator();
        while (childrenIt.hasNext()) {
            Node target = childrenIt.next().getTarget();
            int targetHeight = reducedTree.getHeight(target);
            HeightList targetList = childrenHeightlists.get(targetHeight);
            if (targetList == null) {
                targetList = new HeightList();
                childrenHeightlists.put(targetHeight, targetList);
            }
            targetList.addSorted(target, reducedTree.getMultiset(target));
        }

        for (HeightList h : childrenHeightlists.values()) {
            while (!h.isEmpty()) {
                Node v = (Node) h.get(0);
                //now check if the current heightlist contains isomorphs to T(t_max).
                for (int i = 1; i < h.size(); i++) {
                    Node w = (Node) h.get(i);
                    //found isomorph subtrees
                    if (reducedTree.getMultiset(v).equals(reducedTree.getMultiset(w))) {
                        Node w_father = w.getFirstInEdge().getSource();
                        reducedTree.deleteSubtree(w);
                        checkTreeStructure(reducedTree, w_father);
                        h.remove(w);
                    } else break;
                }
                applyRec(reducedTree, v);
                h.remove(v);
            }
        }
    }

    //checks if we have generated a node n with out- and indegree 1. in this case we want to delete this node.
    private void checkTreeStructure(MultilabeledTree t, Node n) {
        if (n.getInDegree() == 1 && n.getOutDegree() == 1) {
            Edge inEdge = n.getFirstInEdge();
            Edge outEdge = n.getFirstOutEdge();
            Node source = inEdge.getSource();
            Node target = outEdge.getTarget();

            t.deleteEdge(inEdge);
            t.deleteEdge(outEdge);
            t.deleteNode(n);

            t.newEdge(source, target);
        }
    }

}
