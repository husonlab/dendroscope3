/*
 *   LCA_Query_LogN_Sparse.java Copyright (C) 2020 Daniel H. Huson
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

import dendroscope.hybroscale.util.sparseGraph.MySparseNode;
import dendroscope.hybroscale.util.sparseGraph.MySparsePhyloTree;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class LCA_Query_LogN_Sparse {

    private MySparsePhyloTree t;

    private Vector<MySparseNode> nodeArray;
    private Vector<Integer> depthArray;
    private HashMap<MySparseNode, Integer> nodeToIndex;
    private Integer[][] minMatrix;

    public LCA_Query_LogN_Sparse(MySparsePhyloTree t) {
        this.t = t;
        init();
    }

    public MySparseNode cmpLCA(Vector<MySparseNode> nodes) {
        Iterator<MySparseNode> it = nodes.iterator();
        MySparseNode lca = it.next();
        while (it.hasNext()) {
            MySparseNode v = it.next();
            lca = cmpLCA(lca, v);
        }
        return lca;
    }

    private MySparseNode cmpLCA(MySparseNode v1, MySparseNode v2) {
        int l = nodeToIndex.get(v1) < nodeToIndex.get(v2) ? nodeToIndex.get(v1) : nodeToIndex.get(v2);
        int r = nodeToIndex.get(v2) > nodeToIndex.get(v1) ? nodeToIndex.get(v2) : nodeToIndex.get(v1);
        int h = (int) Math.floor(Math.log(r - l + 1) / Math.log(2));
        int m1 = minMatrix[l][h];
        int m2 = minMatrix[r - (int) Math.pow(2, h) + 1][h];
        if (depthArray.get(m1) < depthArray.get(m2))
            return nodeArray.get(m1);
        return nodeArray.get(m2);
    }

    private void init() {
        depthArray = new Vector<Integer>();
        nodeArray = new Vector<MySparseNode>();
        nodeToIndex = new HashMap<MySparseNode, Integer>();
        initRec(t.getRoot(), 0);

        int size = depthArray.size();
        int logSize = (int) Math.ceil(Math.log(size) / Math.log(2));
        minMatrix = new Integer[size][logSize];
        initMatrix();
    }

    private void initMatrix() {
        for (int i = 0; i < minMatrix.length; i++)
            minMatrix[i][0] = i;
        for (int j = 1; j < minMatrix[0].length; j++) {
            for (int i = 0; i < minMatrix.length; i++) {
                if (i + (int) Math.pow(2, j) - 1 < depthArray.size()) {
                    int pos1 = minMatrix[i][j - 1];
                    int pos2 = minMatrix[i + (int) Math.pow(2, j - 1)][j - 1];
                    int min = depthArray.get(pos1) < depthArray.get(pos2) ? pos1 : pos2;
                    minMatrix[i][j] = min;
                }
            }
        }
    }

    private void initRec(MySparseNode v, int depth) {

        if (v.getOutDegree() == 0) {
            depthArray.add(depth);
            nodeArray.add(v);
            nodeToIndex.put(v, depthArray.size() - 1);
        } else {
            int pos = -1;
            for (MySparseNode c : v.getChildren()) {
                int newDepth = depth + 1;
                initRec(c, newDepth);
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
