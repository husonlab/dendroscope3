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

package dendroscope.dtl;

import jloda.phylo.PhyloTree;

import java.io.FileInputStream;
import java.io.InputStreamReader;


public class DTL {

    /**
     * @param args
     */
    public static void main(String[] args) {

        long time = System.currentTimeMillis();


        PhyloTree STree = new PhyloTree();
        STree.setAllowMultiLabeledNodes(true);

        PhyloTree GTree = new PhyloTree();
        GTree.setAllowMultiLabeledNodes(true);

        String dupCostS = new String();
        String transCostS = new String();
        String lossCostS = new String();
        String sTreePath = "";
        String gTreePath = "";
        try {
            sTreePath = args[0].trim();
            gTreePath = args[1].trim();
            // String sTreePath =
            // "/home/wojtek/firstTests/size100_strees/STREE_" + i + ".sim";
            // String sTreePath = "/home/wojtek/firstTests/STREE_" + i + ".txt";
            // String gTreePath =
            // "/home/wojtek/firstTests/size100_gtree_x_paramset_0_0/GTREE_"
            // + j + ":STREE_" + i + ":PARAMSET_0_0.sim";
            dupCostS = args[2].trim();
            transCostS = args[3].trim();
            lossCostS = args[4].trim();

            FileInputStream fstreamS = new FileInputStream(sTreePath);
            InputStreamReader inS = new InputStreamReader(fstreamS);
            STree.read(inS, true);
            FileInputStream fstreamG = new FileInputStream(gTreePath);
            InputStreamReader inG = new InputStreamReader(fstreamG);
            GTree.read(inG, true);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
            System.out.println(e.toString());
            System.out.print("Wrong path? File read error");
        }


        int dupCost = Integer.parseInt(dupCostS);
        int transCost = Integer.parseInt(transCostS);
        int lossCost = Integer.parseInt(lossCostS);
        // int dupCost = cost[k][0];
        // int transCost = cost[k][1];
        // int lossCost = cost[k][2];

        //System.out.println(STree.toBracketString());
        //System.out.println(GTree.toBracketString());

        Structure current = new Structure(dupCost, transCost, lossCost, STree,
                GTree);

        // version 1 = takes a normal speciestree
        // version 2 = takes .sim speciestree

        int version = 2;
        int DTLcost = current.getCost(version);

        String jeden = ("DTL cost for: " + gTreePath + ", " + sTreePath + ", "
                + dupCost + "," + transCost + "," + lossCost + "\n");
        String dwa = Integer.toString(DTLcost);
        dwa = dwa + "\n";
        System.out.print(jeden);
        System.out.print(dwa);


        Long runtime = System.currentTimeMillis() - time;
        Long seconds = runtime / 1000;

        System.out.println("time " + seconds);

        //result.append(jeden);
        //result.append(dwa);

        /*
          try {
              FileWriter fstream = new FileWriter(file);
              BufferedWriter out = new BufferedWriter(fstream);
              out.write(result.toString());
              out.close();
          } catch (Exception e) {

          }
          */
    }

    /**
     * @param STree, GTree, event costs
     */
    public static int apply(PhyloTree STree, PhyloTree GTree, int dupCost, int transCost, int lossCost) {

        PhyloTree STreeCopy = (PhyloTree) STree.clone();
        PhyloTree GTreeCopy = (PhyloTree) GTree.clone();

        Structure current = new Structure(dupCost, transCost, lossCost, STreeCopy, GTreeCopy);


        int version = 2;
        int DTLcost = current.getCost(version);


        return DTLcost;

    }

}
