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

    private boolean checkInDegrees(PhyloTree t) {
        boolean b = true;
        for (Node v : t.getNodes()) {
            if (v.getInDegree() == 0) {
                if (!t.getRoot().equals(v))
                    b = false;
            } else if (v.getInDegree() != 1)
                b = false;
        }
        return b;
    }

    private boolean checkOutDegrees(PhyloTree t) {
        boolean b = true;
        for (Node v : t.getNodes()) {
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
