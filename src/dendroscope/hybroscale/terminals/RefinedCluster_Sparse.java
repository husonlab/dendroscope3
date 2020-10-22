/*
 *   RefinedCluster_Sparse.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.terminals;

import dendroscope.hybroscale.util.sparseGraph.MySparseNode;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

public class RefinedCluster_Sparse {

    private HashMap<BitSet, Vector<MySparseNode>> clusterToNode = new HashMap<BitSet, Vector<MySparseNode>>();
    private int size;

    public RefinedCluster_Sparse(int size) {
        this.size = size;
    }

    public void addCluster(BitSet cluster, Vector<MySparseNode> hashSet) {
        clusterToNode.put(cluster, hashSet);
    }

    public Set<BitSet> getAllCluster() {
        return clusterToNode.keySet();
    }

    public Vector<MySparseNode> getNodes(BitSet cluster) {
        return clusterToNode.get(cluster);
    }

    public boolean isEmpty() {
        return clusterToNode.isEmpty();
    }

    public int getSize() {
        return size;
    }

}
