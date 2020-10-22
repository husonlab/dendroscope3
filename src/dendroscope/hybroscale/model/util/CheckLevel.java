/*
 *   CheckLevel.java Copyright (C) 2020 Daniel H. Huson
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

import dendroscope.hybroscale.model.treeObjects.SparseNetEdge;
import dendroscope.hybroscale.model.treeObjects.SparseNetNode;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

import java.util.Vector;

public class CheckLevel {

    private int border;
    private boolean stop = false;

    public int run(MyPhyloTree network, int border) {

        this.border = border;

        SparseNetwork n = new SparseNetwork(network);
        Vector<SparseNetNode> retNodes = new Vector<SparseNetNode>();
        for (SparseNetNode v : n.getNodes()) {
            if (v.getInDegree() > 1)
                retNodes.add(v);
        }

        if (retNodes.isEmpty()) {
            network.setLevel(0);
            return 0;
        }

        int maxLevel = -1;
        for (SparseNetNode v : retNodes) {
            int level = cmpLevel(v);
            maxLevel = level > maxLevel ? level : maxLevel;
            if (stop)
                return -1;
        }

        network.setLevel(maxLevel);

        return maxLevel;
    }

    private int cmpLevel(SparseNetNode v) {

        Vector<Vector<SparseNetEdge>> allUndirectedCycles = new Vector<Vector<SparseNetEdge>>();
        for (SparseNetEdge e : v.getInEdges()) {
            Vector<SparseNetEdge> visitedEdges = new Vector<SparseNetEdge>();
            visitedEdges.add(e);
            findUndirectedCycleRec(e.getSource(), v, visitedEdges, allUndirectedCycles);
        }

        int maxLevel = -1;

        if (!stop) {
            for (Vector<SparseNetEdge> undirectedCycle : allUndirectedCycles) {
                int level = 0;
                Vector<SparseNetNode> retNodes = new Vector<SparseNetNode>();
                Vector<SparseNetNode> mulNodes = new Vector<SparseNetNode>();
                for (SparseNetEdge e : undirectedCycle) {
                    if (e.getTarget().getInDegree() > 1) {
                        level++;
                        if (!retNodes.contains(e.getTarget()))
                            retNodes.add(e.getTarget());
                        else if (!mulNodes.contains(e.getTarget()))
                            mulNodes.add(e.getTarget());
                    }
                }
                maxLevel = level - mulNodes.size() > maxLevel ? level - mulNodes.size() : maxLevel;
            }
        }

        return maxLevel;
    }

    private void findUndirectedCycleRec(SparseNetNode w, SparseNetNode v, Vector<SparseNetEdge> visitedEdges,
                                        Vector<Vector<SparseNetEdge>> allUndirectedCycles) {
        if (w.equals(v)) {
            allUndirectedCycles.add(visitedEdges);
            if (getCycleCosts(visitedEdges) > border)
                stop = true;
        }
        if (!stop) {
            for (SparseNetEdge e : w.getInEdges()) {
                if (!visitedEdges.contains(e)) {
                    Vector<SparseNetEdge> visitedEdgesCopy = (Vector<SparseNetEdge>) visitedEdges.clone();
                    visitedEdgesCopy.add(e);
                    findUndirectedCycleRec(e.getSource(), v, visitedEdgesCopy, allUndirectedCycles);
                }
            }
            for (SparseNetEdge e : w.getOutEdges()) {
                if (!visitedEdges.contains(e)) {
                    Vector<SparseNetEdge> visitedEdgesCopy = (Vector<SparseNetEdge>) visitedEdges.clone();
                    visitedEdgesCopy.add(e);
                    findUndirectedCycleRec(e.getTarget(), v, visitedEdgesCopy, allUndirectedCycles);
                }
            }
        }
    }

    private int getCycleCosts(Vector<SparseNetEdge> undirectedCycle) {
        int level = 0;
        Vector<SparseNetNode> retNodes = new Vector<SparseNetNode>();
        Vector<SparseNetNode> mulNodes = new Vector<SparseNetNode>();
        for (SparseNetEdge e : undirectedCycle) {
            if (e.getTarget().getInDegree() > 1) {
                level++;
                if (!retNodes.contains(e.getTarget()))
                    retNodes.add(e.getTarget());
                else if (!mulNodes.contains(e.getTarget()))
                    mulNodes.add(e.getTarget());
            }
        }
        return level - mulNodes.size();
    }

}
