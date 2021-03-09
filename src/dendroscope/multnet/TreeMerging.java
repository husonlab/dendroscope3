/*
 *   TreeMerging.java Copyright (C) 2020 Daniel H. Huson
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
package dendroscope.multnet;

import dendroscope.core.TreeData;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.util.Iterator;

/**
 * merges the actual inputed trees to a single one. for this purpose, a new root node will be generated.
 * this node is connected with the roots of all trees we want to merge.
 */
public class TreeMerging {

    private final TreeData[] trees;

    public TreeMerging(TreeData[] trees) {
        this.trees = trees;
    }

    public PhyloTree apply() {
        PhyloTree t = new PhyloTree();
        Node root = t.newNode();
        t.setRoot(root);
        //add all trees to a single graph.
        for (TreeData tmpTree : this.trees) {
            t.add(tmpTree);
        }
        //now connect the old roots with the new one.
        Iterator<Node> nodesIt = t.nodes().iterator();
        while (nodesIt.hasNext()) {
            Node n = nodesIt.next();
            if (n.getInDegree() == 0 && !n.equals(root)) {
                t.newEdge(root, n);
            }
        }
        return t;
    }
}
