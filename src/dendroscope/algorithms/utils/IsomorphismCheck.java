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

package dendroscope.algorithms.utils;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.phylo.PhyloTreeUtils;

import java.io.IOException;
import java.util.*;

/**
 * Given two rooted, bifurcating phylogenetic trees T1 and T2, this function
 * checks whether T1 and T2 are isomorphic.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class IsomorphismCheck {

    public Boolean run(PhyloTree tree1, PhyloTree tree2) {

        try {

            PhyloTree t1 = new PhyloTree();
            t1.parseBracketNotation(tree1.toBracketString(), true);
            PhyloTree t2 = new PhyloTree();
            t2.parseBracketNotation(tree2.toBracketString(), true);

            if (!PhyloTreeUtils.areSingleLabeledTreesWithSameTaxa(t1, t2))
                return false;

            if ((PhyloTreeUtils.isBifurcatingTree(t1) && !PhyloTreeUtils
                    .isBifurcatingTree(t2))
                    || (!PhyloTreeUtils.isBifurcatingTree(t1) && PhyloTreeUtils
                    .isBifurcatingTree(t2)))
                return false;

            if (t1.getNumberOfNodes() != t2.getNumberOfNodes())
                return false;

            if (PhyloTreeUtils.isBifurcatingTree(t1)
                    && PhyloTreeUtils.isBifurcatingTree(t2))
                return (treeIsomorphism(t1, t2));

            return (networkIsomorphism(t1, t2));

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;

    }

    private boolean networkIsomorphism(PhyloTree t1, PhyloTree t2) {
        String s1 = getIsoString(t1);
        String s2 = getIsoString(t2);
        return s1.equals(s2);
    }

    public String getIsoString(PhyloTree tree) {

        Vector<String> idString = new Vector<>();
        Hashtable<String, Node> idToNode = new Hashtable<>();
        Vector<Node> idNodes = new Vector<>();

        Vector<String> idSet = new Vector<>();
        for (Node v : tree.computeSetOfLeaves()) {
            idSet.add(tree.getLabel(v));
            idToNode.put(tree.getLabel(v), v);
            idString.add("[" + tree.getLabel(v) + "]");
        }
        Collections.sort(idSet);
        for (String s : idSet)
            idNodes.add(idToNode.get(s));

        Vector<Node> visited = new Vector<>();
        int id = 1;
        while (!idNodes.isEmpty()) {
            Node v = idNodes.firstElement();
            visited.add(v);
            idNodes.remove(0);

            String newID = null;
            Vector<String> pIDs = new Vector<>();
            Iterator<Edge> it = v.getInEdges();
            while (it.hasNext()) {
                Node p = it.next().getSource();
                if (tree.getLabel(p) == null && newID == null) {
                    newID = String.valueOf(id);
                    while (idSet.contains(newID)) {
                        id++;
                        newID = String.valueOf(id);
                    }
                    idSet.add(newID);
                    tree.setLabel(p, newID);
                } else if (tree.getLabel(p) == null && newID != null)
                    tree.setLabel(p, newID);
                pIDs.add(tree.getLabel(p));
                if (!visited.contains(p) && !idNodes.contains(p))
                    idNodes.add(p);
            }
            if (!pIDs.isEmpty()) {
                Collections.sort(pIDs);
                idString.add(pIDs.toString());
            }
        }

        Collections.sort(idString);
        return idString.toString();

    }

    private boolean treeIsomorphism(PhyloTree t1, PhyloTree t2) {
        String s1 = getTreeString(t1);
        String s2 = getTreeString(t2);
        return s1.equals(s2);
    }

    public String getTreeString(PhyloTree t) {
        Set<Set<String>> clusters = dendroscope.util.PhyloTreeUtils.collectAllHardwiredClusters(t);
        Vector<String> treeString = new Vector<>();
        for (Set<String> o : clusters)
            treeString.add(o.toString());
        Collections.sort(treeString);
        return treeString.toString();
    }

}
