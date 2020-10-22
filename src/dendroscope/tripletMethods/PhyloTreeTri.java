/*
 *   PhyloTreeTri.java Copyright (C) 2020 Daniel H. Huson
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
package dendroscope.tripletMethods;


import dendroscope.consensus.Taxa;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;


/**
 * This class represents trees for triplet-based methods
 *
 * @author celine scornavacca, 6.2010
 */

public class PhyloTreeTri extends PhyloTree {

    public PhyloTreeTri() {
        super();
    }

    public PhyloTreeTri(PhyloTree tree) {
        this();
        setName(tree.getName());
        copy(tree);
    }

    private Map<Node, Integer> nodesToId = new HashMap<>();
    private Map<Node, Vector<Node>> nodesToCladeAbove = new HashMap<>();
    private Map<Node, Vector<Node>> nodesToClade = new HashMap<>();


    public void setNodesToId(Map<Node, Integer> nodesToId) {
        this.nodesToId = nodesToId;
    }

    public void setNodesToCladeAbove(Map<Node, Vector<Node>> nodesToCladeAbove) {
        this.nodesToCladeAbove = nodesToCladeAbove;
    }

    public void setNodesToClade(Map<Node, Vector<Node>> nodesToClade) {
        this.nodesToClade = nodesToClade;
    }


    public Map<Node, Integer> getNodesToId() {
        return nodesToId;
    }

    public Map<Node, Vector<Node>> getNodesToCladeAbove() {
        return nodesToCladeAbove;
    }

    public Map<Node, Vector<Node>> getNodesToClade() {
        return nodesToClade;
    }


    /**
     * get the taxa set for a tanglegram
     *
     * @return taxa
     */

    public Taxa getTaxa() {        //DUPLI
        Taxa taxa = new Taxa();
        for (Node leave : this.computeSetOfLeaves()) {
            taxa.add(this.getLabel(leave));
        }
        return taxa;
    }

    /*set the HashMap associating a node to its label */

    public void setNodeId(Node node, int id) {
        nodesToId.put(node, id);
    }


    /*get the label associated to a node from the HashMap
     * @return   node id
     * */


    public int getNodeId(Node node) {
        return nodesToId.get(node);
    }



    /*set the HashMap associating a node to the clade above it */

    public void setCladeNodeAbove(Node node, Vector<Node> nodes) {
        nodesToCladeAbove.put(node, nodes);
    }


    /*get the clade associated to a node from the HashMap
     * @return   the clade above the node
     * */


    public Vector<Node> getCladeNodeAbove(Node node) {
        return nodesToCladeAbove.get(node);
    }


    /*set the HashMap associating a node to the clade below it */

    public void setCladeNode(Node node, Vector<Node> nodes) {
        nodesToClade.put(node, nodes);
    }


    /*get the clade associated to a node from the HashMap
     * @return   the clade below the node
     * */


    public Vector<Node> getCladeNode(Node node) {
        return nodesToClade.get(node);
    }


    /**
     * get all nodes with outdegree not 0
     *
     * @return all inner nodes
     */

    public Vector<Node> getInnerNodes() {
        Vector<Node> nodes = new Vector<>();
        for (Node v = getFirstNode(); v != null; v = getNextNode(v)) {
            if (v.getOutDegree() != 0)
                nodes.add(v);
        }
        return nodes;
    }


    /**
     * get all nodes
     *
     * @return all  nodes
     */

    public Vector<Node> getAllNodes() {
        Vector<Node> nodes = new Vector<>();
        for (Node v = getFirstNode(); v != null; v = getNextNode(v))
            nodes.add(v);
        return nodes;
    }


    /**
     * get all sons of a node
     *
     * @return all  sons of a node
     */

    public Vector<Node> getSons(Node v) {
        Vector<Node> sons = new Vector<>();
        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            Node w = f.getTarget();
            sons.add(w);
        }
        return sons;
    }


}
