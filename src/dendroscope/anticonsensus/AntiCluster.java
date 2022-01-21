/*
 * AntiCluster.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.anticonsensus;

import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.util.*;

/**
 * representing anti-clusters
 * thomas bonfert, 10.2009
 */

public class AntiCluster {

    private final PhyloTree tree;
    private final Vector<PhyloTree> P_1;
    private final Vector<PhyloTree> P_2;
    private HashSet<Node> witness;
    private final HashSet<Node> nodes;
    private final Node ancestor;
    private final ArrayList<String> taxa;
    private String concatenatedTaxa;

    public AntiCluster(PhyloTree t, Node ancestor) {
        this.tree = t;
        this.ancestor = ancestor;
        this.nodes = new HashSet<>();
        this.P_1 = new Vector<>();
        this.P_2 = new Vector<>();
        this.witness = new HashSet<>();
        this.taxa = new ArrayList<>();
        this.concatenatedTaxa = "";
    }

    public void add(Node n) {
        this.nodes.add(n);
        addSorted(this.taxa, this.tree.getLabel(n));
    }

    private void addSorted(ArrayList<String> taxa, String taxon) {
        Comparator comparator = new Comparator() {
            public int compare(Object o1, Object o2) {
                String taxon1 = (String) o1;
                String taxon2 = (String) o2;
                return taxon1.compareTo(taxon2);
            }
        };
        int insertionPoint = Collections.binarySearch(taxa, taxon, comparator);
        if (insertionPoint < 0) insertionPoint = -1 * insertionPoint - 1;
        taxa.add(insertionPoint, taxon);
    }

    public void setConcatenatedTaxa() {

        for (String aTaxa : this.taxa) {
            this.concatenatedTaxa += aTaxa;
        }
    }


    public String getConcatenatedTaxa() {
        return this.concatenatedTaxa;
    }

    public void addTreeToP1(PhyloTree t) {
        this.P_1.add(t);
    }

    public void addTreeToP2(PhyloTree t) {
        this.P_2.add(t);
    }

    public void setWitness(HashSet<Node> nodes) {
        this.witness = nodes;
    }

    public boolean removeTreeFromP2(PhyloTree t) {
        return (this.P_2.remove(t));
    }

    public HashSet<Node> getNodes() {
        return this.nodes;
    }

    public Node getAncestor() {
        return this.ancestor;
    }

    public Vector<PhyloTree> getP1() {
        return this.P_1;
    }

    public Vector<PhyloTree> getP2() {
        return this.P_2;
    }

    public HashSet<Node> getWitness() {
        return this.witness;
    }

    public PhyloTree getTree() {
        return this.tree;
    }

    public boolean equals(AntiCluster cluster) {
        return (this.getConcatenatedTaxa().compareTo(cluster.getConcatenatedTaxa()) == 0);
    }


}
