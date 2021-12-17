/*
 *   MainTriplets.java Copyright (C) 2020 Daniel H. Huson
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
package dendroscope.triplets;

import dendroscope.consensus.Taxa;
import dendroscope.io.Newick;
import jloda.graph.Node;
import jloda.graph.NodeSet;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 * This class encode triplets
 *
 * @author celine scornavacca, 6.2010
 */

public class MainTriplets {

    public static void main(String[] args) throws Exception {
        String file1 = "/Users/scornava/Documents/workspaceSVN/dendroscope/src/dendroscope/triplets/tree1.txt";
        String file2 = "/Users/scornava/Documents/workspaceSVN/dendroscope/src/dendroscope/triplets/tree2.txt";


        Newick newick = new Newick();

        String StringTree1 = (newick.read(new File(file1))[0]).toBracketString();
        String StringTree2 = (newick.read(new File(file2))[0]).toBracketString();


        PhyloTreeTri[] Trees = new PhyloTreeTri[2];
        Trees[0] = new PhyloTreeTri();
        Trees[0].parseBracketNotation(StringTree1, true);
        Trees[1] = new PhyloTreeTri();
        Trees[1].parseBracketNotation(StringTree2, true);

        Taxa allTaxa = Trees[0].getAllTaxa();
        for (int m = 1; m < Trees.length; m++) {
            allTaxa.addAll(Trees[1].getAllTaxa());
        }

        /*for (int n=1;n<= allTaxa.size();n++) {
            System.out.println(allTaxa.getLabel(n));
        }*/

        Map<String, Integer> taxon2ID = new HashMap<>();

        for (int n = 1; n <= allTaxa.size(); n++) {
            taxon2ID.put(allTaxa.getLabel(n), n - 1);
        }

        TripletMatrix matrix = new TripletMatrix();
        matrix.setDim(allTaxa.size());

        for (int m = 0; m < Trees.length; m++) {
            System.out.println("Tree " + m);
            Map<Node, Integer> nodesToId = new HashMap<>();
            NodeSet leaves = (Trees[m]).computeSetOfLeaves();
            for (Node leaf : leaves) {
                Trees[m].setNodeId(leaf, taxon2ID.get(Trees[m].getLabel(leaf)));
            }
            TripletUtils.setClade(Trees[m]);
            TripletUtils.setCladeAbove(Trees[m]);
            TripletUtils.setTriplets(Trees[m], matrix);
        }
        matrix.print();

        //TreeViewer tempV= new TreeViewer();
        //tempV.fireEdgeLabelAAAAAA();


    }

}                  
