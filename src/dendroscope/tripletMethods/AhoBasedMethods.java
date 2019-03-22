/**
 * AhoBasedMethods.java 
 * Copyright (C) 2019 Daniel H. Huson
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
import jloda.phylo.PhyloTree;
import jloda.swing.director.IDirector;
import jloda.swing.util.Alert;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: scornava
 * Date: Jul 13, 2010
 * Time: 6:50:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class AhoBasedMethods {

    public static void applyAho(Director dir, Document doc, PhyloTree tempTrees[], String param[]) throws Exception {

        //ToDo: [Celine] Can we avoid to copy the trees?

        PhyloTreeTri trees[] = new PhyloTreeTri[tempTrees.length];
        for (int n = 0; n < tempTrees.length; n++) {
            PhyloTreeTri tempTree = new PhyloTreeTri(tempTrees[n]);
            trees[n] = tempTree;
        }

        Taxa allTaxa = trees[0].getTaxa();
        for (int m = 1; m < trees.length; m++) {
            allTaxa.addAll(trees[1].getTaxa());
        }

        HashMap<Integer, String> ID2taxon = new HashMap<>();

        for (int n = 1; n <= allTaxa.size(); n++) {
            ID2taxon.put(n, allTaxa.getLabel(n));
        }

        TripletSet t = new TripletSet();
        t.setNumLeaves(allTaxa.size());

        t.initLookup(allTaxa.size());
        List<TreeData> ahoTree = new LinkedList<>();

        boolean treeFound = false;

        biDAG bignet = t.buildNetwork(0, 0);// it is a tree, so level-0
        if (bignet != null) {
            bignet.resetVisited();
            String networkInNewickFormat = bignet.newickDump(ID2taxon);
            TreeData network = new TreeData();
            network.parseBracketNotation(networkInNewickFormat, true);
            ahoTree.add(network);
            treeFound = true;
        } else {
            System.out.println("// No network found, not even a heuristic network :(");
            new Alert(" No network found, not even a heuristic network.\n");
        }


        if (treeFound) { // If at least one network is found, then we draw it

            Director newDir = Director.newProject(1, 1);
            newDir.getDocument().appendTrees(ahoTree.toArray(new TreeData[ahoTree.size()]));
            newDir.getDocument().setTitle(doc.getTitle() + "-aho");
            MultiViewer newMultiViewer = (MultiViewer) newDir.getMainViewer();
            newMultiViewer.chooseGridSize();
            newMultiViewer.loadTrees(null);
            newMultiViewer.setMustRecomputeEmbedding(true);
            newMultiViewer.updateView(IDirector.ALL);
            newMultiViewer.getFrame().toFront();
        }
    }
}


/*

void build(MyTree * tree,MyNode & nodo,vector < int> toIns, MatrixTriplet & Triplets){
    cout << "id:  " << nodo.getId() << endl;

    int id;
    if(toIns.size()==1) {
        //( nodo).setName((* getNodeUnique(* tree,toIns[0])).getName());
        ( nodo).getInfos().setStid(toIns[0]);
        }
  else  if(toIns.size()==2) {

          id=tree->getNextId();
           MyNode * nodoFiglio1= new MyNode(id);
           id=tree->getNextId();
           MyNode * nodoFiglio2= new MyNode(id);
       //( * nodoFiglio1).setName((* getNodeUnique(* tree,toIns[0])).getName());
        ( * nodoFiglio1).getInfos().setStid(toIns[0]);
        //( * nodoFiglio2).setName((* getNodeUnique(* tree,toIns[1])).getName());
        ( * nodoFiglio2).getInfos().setStid(toIns[1]);
        nodo.addSon( *nodoFiglio1);
         nodo.addSon( *nodoFiglio2);
        }
   else{
        const int num_vert =  toIns.size();
       Graph g(num_vert);
       setEdges(& g, Triplets, toIns);

       std::vector<int> component(num_vertices(g));
        int num = connected_components(g, &component[0]);
        MyNode * nodoFiglio[num];
       if (num)
       {
           cout << "impossibile\n";
       }
       else {
           vector <int> toInsNext[num];
             vector <int> notToInsNext;
              for (unsigned int i=0;i<toIns.size(); i++){
               toInsNext[(component[i])].push_back(toIns[i] );
              }
              for (int i=0;i<num; i++){
                    id=tree->getNextId();
                 nodoFiglio[i]= new MyNode(id);
                 nodo.addSon(*nodoFiglio[i]);
               sort( toInsNext[i].begin(), toInsNext[i].end());
               vector <int> notToInsNext= diff(toIns, toInsNext[i]);
                   build(tree,* nodoFiglio[i],toInsNext[i], Triplets);
           }
          }
   }
}


        int setEdges(Graph  * g,MatrixTriplet & Triplets, vector <int> toIns){
         map<int,int> association;
         int numEdges=0;
         for (unsigned int i=0;i <  toIns.size();i ++)    //posso evitare di fare la corrispondenza????
           association.insert( make_pair(toIns[i], i ) );
        map<int,int>::iterator iter;
        map<int,int>::iterator iter2;
          for(unsigned int i=0;i< toIns.size();i++){
              for(unsigned int j=0;j< toIns.size();j++){
                     for(unsigned int z=0;z< toIns.size();z++){
                         if(Triplets.getValue( toIns[i],toIns[j],toIns[z]) != 0){
                                iter =  association.find(toIns[i]);
                          iter2 =  association.find(toIns[j]);
                          if(!((edge( iter->second, iter2->second, * g)).second)){
                                    add_edge(iter->second, iter2->second, * g);
                                    numEdges++;
                          }
                         }
                     }
              }
          }
          return numEdges;
   }
*/
