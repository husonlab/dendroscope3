/*
 *   LCA_Query.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.util.lcaQueries;

import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

public class LCA_Query {

    private MyPhyloTree t;

    private Vector<MyNode> nodeArray;
    private Vector<Integer> depthArray;
    private HashMap<MyNode, Integer> nodeToIndex;
    private Integer[][] minMatrix;

    public LCA_Query(MyPhyloTree t) {
        this.t = t;
        init();
    }

    public MyNode cmpLCA(HashSet<MyNode> nodes) {
        Iterator<MyNode> it = nodes.iterator();
        MyNode lca = it.next();
        while (it.hasNext()) {
            MyNode v = it.next();
            lca = cmpLCA(lca, v);
        }
        return lca;
    }

    private MyNode cmpLCA(MyNode v1, MyNode v2) {
        int pos1 = nodeToIndex.get(v1);
        int pos2 = nodeToIndex.get(v2);
        if (pos1 < pos2)
            return nodeArray.get(minMatrix[pos1][pos2]);
        return nodeArray.get(minMatrix[pos2][pos1]);
    }

    private void init() {

        depthArray = new Vector<Integer>();
        nodeArray = new Vector<MyNode>();
        nodeToIndex = new HashMap<MyNode, Integer>();
        initRec(t.getRoot(), 0);

        int size = depthArray.size();
        int logSize = (int) Math.ceil(Math.log(size) / Math.log(2));
        minMatrix = new Integer[size][size];
        initMatrix();
    }

    private void initMatrix() {
        for (int i = 0; i < depthArray.size(); i++) {
            int min = i;
            for (int j = i; j < depthArray.size(); j++) {
                min = depthArray.get(j) < depthArray.get(min) ? j : min;
                minMatrix[i][j] = min;
            }
        }
    }

    private void initRec(MyNode v, int depth) {

        if (v.getOutDegree() == 0) {
            depthArray.add(depth);
            nodeArray.add(v);
            nodeToIndex.put(v, depthArray.size() - 1);
        } else {
            int pos = -1;
            Iterator<MyEdge> it = v.outEdges().iterator();
            while (it.hasNext()) {
                int newDepth = depth + 1;
                initRec(it.next().getTarget(), newDepth);
                depthArray.add(depth);
                nodeArray.add(v);
                if (pos == -1)
                    pos = depthArray.size() - 1;
            }

            depthArray.remove(depthArray.size() - 1);
            nodeArray.remove(nodeArray.size() - 1);
            nodeToIndex.put(v, pos);
        }

    }

}
