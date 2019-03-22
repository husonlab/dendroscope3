/**
 * CheckTrees.java 
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
package dendroscope.hybrid;

import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.util.Iterator;
import java.util.Vector;

/**
 * Given two Graphs, this method checks whether both trees are bifurcating and
 * have the same leaf-set.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class CheckTrees {

    public boolean run(PhyloTree[] trees) {
        if (twoTrees(trees)) {
            if (bifurcatingTrees(trees[0], trees[1])
                    && sameLeafset(trees[0], trees[1]))
                return true;
        }
        return false;
    }

    private boolean twoTrees(PhyloTree[] trees) {
        return trees.length == 2;
    }

    private boolean bifurcatingTrees(PhyloTree t1, PhyloTree t2) {
        boolean t1Degree = (checkInDegrees(t1) && checkOutDegrees(t1));
        boolean t2Degree = (checkInDegrees(t2) && checkOutDegrees(t2));
        return (t1Degree && t2Degree);
    }

    private boolean checkInDegrees(PhyloTree tree) {
        boolean b = true;
        for (Node v : tree.nodes()) {
            if (v.getInDegree() == 0) {
                if (!tree.getRoot().equals(v))
                    b = false;
            } else if (v.getInDegree() != 1)
                b = false;
        }
        return b;
    }

    private boolean checkOutDegrees(PhyloTree t) {
        boolean b = true;
        for (Node v : t.nodes()) {
            if (v.getOutDegree() != 0 && v.getOutDegree() != 2)
                b = false;
        }
        return b;
    }

    private boolean sameLeafset(PhyloTree t1, PhyloTree t2) {
        Vector<String> t1Leafset = new Vector<>();
        Iterator<Node> it = t1.computeSetOfLeaves().iterator();
        while (it.hasNext())
            t1Leafset.add(t1.getLabel(it.next()));
        it = t2.computeSetOfLeaves().iterator();
        while (it.hasNext()) {
            Node v = it.next();
            if (!t1Leafset.contains(t2.getLabel(v)))
                return false;
        }
        return true;
    }

}
