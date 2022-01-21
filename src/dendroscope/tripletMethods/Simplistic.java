/*
 * Simplistic.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.tripletMethods;

import dendroscope.consensus.Taxa;
import dendroscope.core.Director;
import dendroscope.core.Document;
import dendroscope.core.TreeData;
import dendroscope.window.MultiViewer;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;
import jloda.swing.director.IDirector;
import jloda.swing.util.Alert;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: scornava
 * Date: Jul 12, 2010
 * Time: 12:41:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class Simplistic {

    public static boolean BUILD_ALL = false;
    public static boolean SIMPLE = false;
    public static int BEGIN_LEV = 1;
    public static int SN_TO_SPLIT = 0;    //! how many SN-sets should be split
    public static int MAXLEV = -1;
    public static boolean stopAtThisKIfSuccess = false;


    public static void apply(Director dir, Document doc, PhyloTree tempTrees[], String param[]) throws Exception {


        SIMPLE = Boolean.parseBoolean(param[0]);
        BUILD_ALL = Boolean.parseBoolean(param[1]);
        stopAtThisKIfSuccess = Boolean.parseBoolean(param[2]);
        BEGIN_LEV = Integer.parseInt(param[3]);
        SN_TO_SPLIT = Integer.parseInt(param[4]);
        MAXLEV = Integer.parseInt(param[5]);


        /*System.out.println(BUILD_ALL);
        System.out.println(SIMPLE);
        System.out.println(stopAtThisKIfSuccess);
        System.out.println(BEGIN_LEV);
        System.out.println(SN_TO_SPLIT);
        System.out.println(MAXLEV);*/


        //ToDo: [Celine] Can we avoid to copy the trees?

        PhyloTreeTri[] trees = new PhyloTreeTri[tempTrees.length];
        for (int n = 0; n < tempTrees.length; n++) {
            PhyloTreeTri tempTree = new PhyloTreeTri(tempTrees[n]);
            trees[n] = tempTree;
        }

        Taxa allTaxa = trees[0].getAllTaxa();
        for (int m = 1; m < trees.length; m++) {
            allTaxa.addAll(trees[1].getAllTaxa());
        }

        HashMap<String, Integer> taxon2ID = new HashMap<>();
        HashMap<Integer, String> ID2taxon = new HashMap<>();

        for (int n = 1; n <= allTaxa.size(); n++) {
            taxon2ID.put(allTaxa.getLabel(n), n);
            ID2taxon.put(n, allTaxa.getLabel(n));
        }

        TripletSet t = new TripletSet();
        t.setNumLeaves(allTaxa.size());

        t.initLookup(allTaxa.size());

        for (PhyloTreeTri tree : trees) {
            //System.out.println( "Tree " + m);
            Map<Node, Integer> nodesToId = new HashMap<>();
            NodeSet leaves = (tree).computeSetOfLeaves();
            for (Node leaf : leaves) {
                tree.setNodeId(leaf, taxon2ID.get(tree.getLabel(leaf)));
            }
            TripletUtils.setClade(tree);
            TripletUtils.setCladeAbove(tree);
            TripletUtils.setTriplets(tree, t);
        }
        t.setFinalised(true);

        /*System.out.println( "print list:");
        t.printList();*/

        System.out.println(t.getTripVec().size() + " triplets " + " for " + allTaxa.size() + " taxa");

        int[] errorTrip = new int[3];

        List<TreeData> simplisticNetwork = new LinkedList<>();

        /*not useful anymore
        if(!t.isClosed(errorTrip))
        {
            System.out.println("ERROR: there are leaves in the triplet set that are unused;");
            System.out.println("Leaf "+errorTrip[0]+" does not appear in any triple (and maybe this is true for other leaves.)");
            System.out.println("Please ensure that the leaf range is precisely of the form [1...n] where all leaves in that interval appear in some triplet.");
            System.exit(0);
        }  */

        System.out.println("Leaf interval is closed 1..n, good.");

        if (!t.isDense(errorTrip)) {
            System.out.println("ERROR: Not a dense input set.");
            System.out.println("{" + errorTrip[0] + "," + errorTrip[1] + "," + errorTrip[2] + "} is missing (and maybe more.)");
            new Alert("The method works only for dense triplet set.\nThe given set is not dense.\n{" + errorTrip[0] + "," + errorTrip[1] + "," + errorTrip[2] + "} is missing (and maybe more.)\n");
            //System.exit(0);

        } else {

            System.out.println("Triplets are dense, good.");

            System.out.println("Attempting to build HEURISTIC network.");


            //int max = t.getNumLeaves() - 1;

            if (Simplistic.MAXLEV == -1) {
                Simplistic.MAXLEV = t.getNumLeaves() - 1; //default value
            }

            boolean networkFound = false; // do we need to draw the network? True if we find at least one network.
            boolean stopSearching = false; // true if stopAtThisKIfSuccess is true and we find  find at least one network.

            //if (Simplistic.MAXLEV > max) max = Simplistic.MAXLEV; //no, i want to be able to stop before

            System.out.println("max......" + Simplistic.MAXLEV);

            if (Simplistic.SIMPLE) {
                for (int level = Simplistic.BEGIN_LEV; level <= Simplistic.MAXLEV && (!stopSearching); level++) {
                    System.out.println("// Trying to build simple level " + level + " networks.");
                    Vector sol = t.buildSimpleNetworks(level);
                    if (sol != null) {
                        Enumeration e = sol.elements();
                        while (e.hasMoreElements()) {
                            BiDAG b = (BiDAG) e.nextElement();
                            b.resetVisited();    //! does this work if we have ripped nodes out???
                            String networkInNewickFormat = b.newickDump(ID2taxon);
                            TreeData network = new TreeData();
                            network.parseBracketNotation(networkInNewickFormat, true);
                            simplisticNetwork.add(network);
                            networkFound = true;
                        }
                        if (Simplistic.BUILD_ALL) {
                            if (sol.size() == 1) System.out.println("// ENUM: There was thus only one solution");
                            else
                                System.out.println("// ENUM: There were thus multiple (maybe isomorphic) solutions, " + sol.size() + " in total.");
                        }
                        if (stopAtThisKIfSuccess)
                            stopSearching = true;
                        //System.exit(0);
                    } else {
                        System.out.println("No simple network exists for this level.\n");
                        //new Alert("No simple network exists for this level.\n");

                    }
                }
                if (!networkFound) {
                    System.out.println("No simple network exists.\n");
                    new Alert("No simple network exists.\n");
                }
            } else {

                for (int level = Simplistic.BEGIN_LEV; level <= Simplistic.MAXLEV && (!stopSearching); level++) {
                    System.out.println("// Trying to build a level " + level + " network.");
                    BiDAG bignet = t.buildNetwork(0, level);
                    if (bignet != null) {
                        bignet.resetVisited();
                        String networkInNewickFormat = bignet.newickDump(ID2taxon);
                        TreeData network = new TreeData();
                        network.parseBracketNotation(networkInNewickFormat, true);
                        simplisticNetwork.add(network);
                        networkFound = true;

                        if (stopAtThisKIfSuccess)
                            stopSearching = true;

                    } else {
                        System.out.println("No heuristic network found for this level.\n");
                        //new Alert(" No heuristic network found for this level. \n");
                        //new Alert("No simple network exists for this level.\n");

                    }

                }
                if (!networkFound) {
                    System.out.println("// No network found, not even a heuristic network :(");
                    new Alert(" No network found, not even a heuristic network. Try to increase the maximal level. \n");
                }
            }

            if (networkFound) { // If at least one network is found, then we draw it

                Director newDir = Director.newProject(1, 1);
                newDir.getDocument().appendTrees(simplisticNetwork.toArray(new TreeData[simplisticNetwork.size()]));
                newDir.getDocument().setTitle(doc.getTitle() + "-simplistic");
                MultiViewer newMultiViewer = (MultiViewer) newDir.getMainViewer();
                newMultiViewer.chooseGridSize();
                newMultiViewer.loadTrees(null);
                newMultiViewer.setMustRecomputeEmbedding(true);
                newMultiViewer.updateView(IDirector.ALL);
                newMultiViewer.getFrame().toFront();
            }
        }
    }
}

