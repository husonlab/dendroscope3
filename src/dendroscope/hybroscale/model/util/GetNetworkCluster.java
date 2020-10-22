/*
 *   GetNetworkCluster.java Copyright (C) 2020 Daniel H. Huson
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

import dendroscope.hybroscale.util.graph.MyEdge;
import dendroscope.hybroscale.util.graph.MyNode;
import dendroscope.hybroscale.util.graph.MyPhyloTree;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

public class GetNetworkCluster {

    private HashMap<MyNode, HashSet<MyNode>> nodeToPreds;
    private HashMap<MyNode, HashSet<MyNode>> nodeToRetPreds;

    public Vector<MyPhyloTree> run(MyPhyloTree n) {

        nodeToPreds = new HashMap<MyNode, HashSet<MyNode>>();
        nodeToRetPreds = new HashMap<MyNode, HashSet<MyNode>>();
        for (MyNode v : n.getLeaves())
            cmpPredecessorsRec(v, new HashSet<MyNode>(), new HashSet<MyNode>());

        HashSet<MyNode> rootOfClusters = new HashSet<MyNode>();
        HashSet<MyNode> visited = new HashSet<MyNode>();
        for (MyNode v : n.getLeaves())
            cmpRootOfClustersRec(v, true, rootOfClusters, visited);
        if (!rootOfClusters.contains(n.getRoot()))
            rootOfClusters.add(n.getRoot());

        Vector<MyPhyloTree> networkClusters = new Vector<MyPhyloTree>();
        for (MyNode r : rootOfClusters) {
            MyPhyloTree netCluster = new MyPhyloTree();
            HashMap<MyNode, MyNode> vistedNodes = new HashMap<MyNode, MyNode>();
            cmpNetClusterRec(r, netCluster.getRoot(), vistedNodes, rootOfClusters);
            for (MyNode v : netCluster.getNodes()) {
                if (v.getInDegree() == 0)
                    netCluster.setRoot(v);
            }
            networkClusters.add(netCluster);
        }

        return networkClusters;

    }

    private void cmpNetClusterRec(MyNode v, MyNode vCopy, HashMap<MyNode, MyNode> vistedNodes,
                                  HashSet<MyNode> rootOfClusters) {
        Iterator<MyEdge> it = v.outEdges().iterator();
        while (it.hasNext()) {
            MyNode c = it.next().getTarget();
            if (vistedNodes.containsKey(c)) {
                MyNode cCopy = vistedNodes.get(c);
                vCopy.getOwner().newEdge(vCopy, cCopy);
            } else {
                MyNode cCopy = vCopy.getOwner().newNode(c);
                vCopy.getOwner().newEdge(vCopy, cCopy);
                vistedNodes.put(c, cCopy);
                if (!rootOfClusters.contains(c))
                    cmpNetClusterRec(c, cCopy, vistedNodes, rootOfClusters);
            }
        }
    }

    private void cmpRootOfClustersRec(MyNode v, boolean bC, HashSet<MyNode> rootOfClusters, HashSet<MyNode> visited) {
        if (!visited.contains(v)) {
            visited.add(v);
            boolean bV = isRootOfCluster(v);
            if (!bC && bV)
                rootOfClusters.add(v);
            Iterator<MyEdge> it = v.inEdges().iterator();
            while (it.hasNext()) {
                MyNode p = it.next().getSource();
                cmpRootOfClustersRec(p, bV, rootOfClusters, visited);
            }
        }
    }

    private void cmpPredecessorsRec(MyNode v, HashSet<MyNode> preds, HashSet<MyNode> retPreds) {

        preds.add(v);
        if (!nodeToPreds.containsKey(v))
            nodeToPreds.put(v, preds);
        else
            nodeToPreds.get(v).addAll(preds);
        if (!nodeToRetPreds.containsKey(v))
            nodeToRetPreds.put(v, retPreds);
        else
            nodeToRetPreds.get(v).addAll(retPreds);

        Iterator<MyEdge> it = v.inEdges().iterator();
        while (it.hasNext()) {
            MyNode p = it.next().getSource();
            HashSet<MyNode> predsClone = (HashSet<MyNode>) preds.clone();
            // predsClone.add(v);
            HashSet<MyNode> retPredsClone = (HashSet<MyNode>) retPreds.clone();
            if (v.getInDegree() > 1)
                retPredsClone.add(v);
            cmpPredecessorsRec(p, predsClone, retPredsClone);
        }

    }

    private boolean isRootOfCluster(MyNode v) {
        HashSet<MyNode> preds = nodeToPreds.get(v);
        HashSet<MyNode> retPreds = nodeToRetPreds.get(v);
        for (MyNode r : retPreds) {
            Iterator<MyEdge> it = r.inEdges().iterator();
            while (it.hasNext()) {
                MyNode p = it.next().getSource();
                if (!preds.contains(p))
                    return false;
            }
        }
        return true;
    }

}
