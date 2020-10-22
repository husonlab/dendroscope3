/*
 *   RefinedCluster.java Copyright (C) 2020 Daniel H. Huson
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

import dendroscope.hybroscale.util.graph.MyNode;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class RefinedCluster {

    private HashMap<BitSet, HashSet<MyNode>> clusterToNode = new HashMap<BitSet, HashSet<MyNode>>();
    private int size;

    public RefinedCluster(int size) {
        this.size = size;
    }

    public void addCluster(BitSet cluster, HashSet<MyNode> hashSet) {
        clusterToNode.put(cluster, hashSet);
    }

    public Set<BitSet> getAllCluster() {
        return clusterToNode.keySet();
    }

    public HashSet<MyNode> getNodes(BitSet cluster) {
        return clusterToNode.get(cluster);
    }

    public boolean isEmpty() {
        return clusterToNode.isEmpty();
    }

    public int getSize() {
        return size;
    }

}
