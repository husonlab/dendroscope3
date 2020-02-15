/*
 *   Test.java Copyright (C) 2020 Daniel H. Huson
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

/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import jloda.phylo.PhyloTree;

public class Test {

    public static void main(String[] args) {
        try {

            double time = System.currentTimeMillis();

            // Simple Example
            // PhyloTree t1 = new PhyloTree();
            // t1.parseBracketNotation("(z,((a,b),(((g,h),x),y)))", true);
            //
            // PhyloTree t2 = new PhyloTree();
            // t2.parseBracketNotation("(z,(a,(b,(x,(y,(g,h))))))", true);

            // Example on page 250
            // PhyloTree t1 = new PhyloTree();
            // t1.parseBracketNotation("((((a,b),c),d),(e,f))", true);
            //
            // PhyloTree t2 = new PhyloTree();
            // t2.parseBracketNotation("(((a,(b,(c,d))),f),e)", true);
            //
            // HybridNetwork n1 = new HybridNetwork(t1, true, null);
            // HybridNetwork n2 = new HybridNetwork(t2, true, null);
            // ReplacementInfo rI = new ReplacementInfo();
            // (new SubtreeReduction()).run(n1, n2, rI);

            // Example on page 250 - contains a cycle
            // PhyloTree t1 = new PhyloTree();
            // t1.parseBracketNotation("((a,(b,(e,(c,d)))),f)", true);
            //
            // PhyloTree t2 = new PhyloTree();
            // t2.parseBracketNotation("(a,(f,(d,(c,(b,e)))))", true);

            // Example on page 254 - big example
            // PhyloTree t1 = new PhyloTree();
            // t1
            // .parseBracketNotation(
            // "(anomochloa,(pharus,(((chusquea,(pariana,eremitis)),(oryza,(lygeum,(triticum,(glycerias,meelicaa))))),(danthoniop,(pennisetum,(miscanthus,zea))))))",
            // true);
            //
            // PhyloTree t2 = new PhyloTree();
            // t2
            // .parseBracketNotation(
            // "(anomochloa,(pharus,(chusquea,((danthoniop,(pennisetum,(miscanthus,zea))),((pariana,eremitis),(oryza,(meelicaa,(glycerias,(triticum,lygeum)))))))))",
            // true);

            // PhyloTree[] trees = { t1, t2 };
            // new Hybrid(trees);

            // PhyloTree t1 = new PhyloTree();
            // t1.parseBracketNotation("(z,((a,b),(((g,h),x),y)))", true);
            // HybridNetwork h1 = new HybridNetwork(t1,true);
            // System.out.println("h1: "+h1);
            //
            // PhyloTree t2 = new PhyloTree();
            // t2.parseBracketNotation("(a,(z,b))", true);
            // HybridNetwork h2 = new HybridNetwork(t2,false);
            //
            // HybridNetwork h3 = (new RestrictTree()).restrict(h1, h2);
            // System.out.println("h2: "+h2);
            // System.out.println("h3: "+h3);
            // System.out.println((new IsomorphismCheck()).test(h3, h2));

            // Example on page 250 - contains no cycle
            // PhyloTree t1 = new PhyloTree();
            // t1.parseBracketNotation("((e,f),(d,(c,(a,b))))", true);
            // HybridNetwork h1 = new HybridNetwork(t1,true,null);
            // h1.update();
            //
            // PhyloTree t6 = new PhyloTree();
            // t6.parseBracketNotation("(e,(f,(a,(b,(c,d)))))", true);
            // HybridNetwork h6 = new HybridNetwork(t6,true,null);
            // h6.update();
            //
            // PhyloTree t2 = new PhyloTree();
            // t2.parseBracketNotation("(a,d)", true);
            // HybridNetwork h2 = new
            // HybridNetwork(t2,false,h1.getTaxaOrdering());
            //
            // PhyloTree t3 = new PhyloTree();
            // t3.parseBracketNotation("(b)", true);
            // HybridNetwork h3 = new
            // HybridNetwork(t3,false,h1.getTaxaOrdering());
            //
            // PhyloTree t4 = new PhyloTree();
            // t4.parseBracketNotation("(c)", true);
            // HybridNetwork h4 = new
            // HybridNetwork(t4,false,h1.getTaxaOrdering());
            //
            // PhyloTree t5 = new PhyloTree();
            // t5.parseBracketNotation("(e,f)", true);
            // HybridNetwork h5 = new
            // HybridNetwork(t5,true,h1.getTaxaOrdering());
            //
            // HybridNetwork[] forest = {h2,h3,h4,h5};
            //
            // System.out.println((new AcyclicCheck()).test(forest, h1, h6));
            // System.out.println((new NodeDisjointCheck()).check(h1, forest));

            // Example on page 250 - contains a cycle
            // PhyloTree t1 = new PhyloTree();
            // t1.parseBracketNotation("(f,(a,(b,(e,(c,d)))))", true);
            // HybridNetwork h1 = new HybridNetwork(t1,true,null);
            // h1.update();
            //
            // PhyloTree t2 = new PhyloTree();
            // t2.parseBracketNotation("(a,(f,(d,(c,(b,e)))))", true);
            // HybridNetwork h2 = new HybridNetwork(t2,true,null);
            // h2.update();
            //
            // PhyloTree t3 = new PhyloTree();
            // t3.parseBracketNotation("(a,f)", true);
            // HybridNetwork h3 = new
            // HybridNetwork(t3,true,h1.getTaxaOrdering());
            //
            // PhyloTree t4 = new PhyloTree();
            // t4.parseBracketNotation("(e,b)", true);
            // HybridNetwork h4 = new
            // HybridNetwork(t4,false,h1.getTaxaOrdering());
            //
            // PhyloTree t5 = new PhyloTree();
            // t5.parseBracketNotation("(c,d)", true);
            // HybridNetwork h5 = new
            // HybridNetwork(t5,false,h1.getTaxaOrdering());
            //
            // HybridNetwork[] forest = {h3,h4,h5};
            //
            // System.out.println((new AcyclicCheck()).test(forest, h1, h2));

            // GetAgreementForest Test
            // PhyloTree t1 = new PhyloTree();
            // t1.parseBracketNotation("((((a,b),c),d),(e,f))", true);
            // HybridNetwork h1 = new HybridNetwork(t1,true,null);
            // h1.update();
            //
            // PhyloTree t2 = new PhyloTree();
            // t2.parseBracketNotation("(((a,(b,(c,d))),f),e)", true);
            // HybridNetwork h2 = new HybridNetwork(t2,true,null);
            // h2.update();
            //
            // new ExhaustiveSearch().run(h1, h2);

            // MinHybridNetwork
            // PhyloTree t1 = new PhyloTree();
            // t1.parseBracketNotation("(f,(a,(b,(e,(c,d)))))", true);
            // HybridNetwork h1 = new HybridNetwork(t1, true, null);
            // h1.update();
            //
            // PhyloTree t2 = new PhyloTree();
            // t2.parseBracketNotation("(a,(f,(d,(c,(b,e)))))", true);
            // HybridNetwork h2 = new HybridNetwork(t2, true, null);
            // h2.update();
            //
            // PhyloTree t3 = new PhyloTree();
            // t3.parseBracketNotation("(a,f)", true);
            // HybridNetwork h3 = new HybridNetwork(t3, true,
            // h1.getTaxaOrdering());
            //
            // PhyloTree t4 = new PhyloTree();
            // t4.parseBracketNotation("(e,b)", true);
            // HybridNetwork h4 = new HybridNetwork(t4, false,
            // h1.getTaxaOrdering());
            //
            // PhyloTree t5 = new PhyloTree();
            // t5.parseBracketNotation("(c,d)", true);
            // HybridNetwork h5 = new HybridNetwork(t5, false,
            // h1.getTaxaOrdering());
            //
            // Vector<HybridNetwork> forest = new Vector<HybridNetwork>();
            // forest.add(h5);
            // forest.add(h4);
            // forest.add(h3);
            //
            // (new ComputeHybridNetwork()).run(forest, h1, h2);

            // PhyloTree t1 = new PhyloTree();
            // t1.parseBracketNotation("((((((((r,a),b),c),d),e),f),g),(s,(t,(u,v))))",
            // true);
            //
            // PhyloTree t2 = new PhyloTree();
            // t2.parseBracketNotation("((((((((r,a),b),c),g),d),e),f),(s,(t,(u,v))))",
            // true);

            // PhyloTree t1 = new PhyloTree();
            // t1
            // .parseBracketNotation(
            // "(gynerium,(micraira,((((miscanthus,zea),(pennisetum,panicum)),thysanolae),((centropodi,((danthonia,(merxmuel_r,(merxmuel_m,(((((sporobolus,spartina),eragrostis),(stipagrost,aristida)),(austrodant,karoochloa)),arundo)))),(phragmites,(molinia,(amphipogon,((ampelodesm,(((piptatheru,anisopogon),(((nassella,stipa),((nardus,lygeum),(brachypodi,((triticum,bromus),avena)))),((glycerias,melicaa),brachyelyt))),diarrhena)),(((leersia,oryza),ehrharta),(chusquea,lithachne)))))))),(chasmanthi,(pharus,(joinvillea,streptocha)))))));",
            // true);
            //
            // PhyloTree t2 = new PhyloTree();
            // t2
            // .parseBracketNotation(
            // "(spartina,(sporobolus,(eragrostis,((((((((((miscanthus,zea),(pennisetum,panicum)),gynerium),thysanolae),chasmanthi),((phragmites,molinia),(arundo,amphipogon))),((danthonia,(austrodant,karoochloa)),merxmuel_m)),((stipagrost,aristida),((((leersia,oryza),ehrharta),((chusquea,lithachne),(brachyelyt,((nardus,lygeum),((((glycerias,melicaa),(nassella,(piptatheru,(stipa,ampelodesm)))),(diarrhena,(brachypodi,((triticum,bromus),avena)))),anisopogon))))),(pharus,(streptocha,joinvillea))))),(centropodi,merxmuel_r)),micraira))));",
            // true);
            //
            // System.out.println(t1.toBracketString()+";");
            // System.out.println(t2.toBracketString()+";");
            //
            // System.out.println((new
            // JoshInterleaveSnappy().run(t1.toBracketString()+";",
            // t2.toBracketString()+";")));

            // //phyb_ITS
            // String s1 =
            // "(miscanthus,(zea,((pennisetum,panicum),(thysanolae,(((danthonia,((sporobolus,eragrostis),aristida)),(phragmites,(molinia,(((anisopogon,((nassella,((nardus,lygeum),(brachypodi,((triticum,bromus),avena)))),(glycerias,melicaa))),diarrhena),((oryza,ehrharta),(chusquea,lithachne)))))),(chasmanthi,(pharus,(joinvillea,streptocha))))))));";
            // String s2 =
            // "(miscanthus,(zea,((pennisetum,panicum),(((chasmanthi,thysanolae),((sporobolus,eragrostis),(molinia,phragmites))),((danthonia,aristida),(((chusquea,lithachne),(((anisopogon,(((((triticum,bromus),avena),diarrhena),brachypodi),(nassella,(glycerias,melicaa)))),(nardus,lygeum)),(oryza,ehrharta))),(pharus,(streptocha,joinvillea))))))));";

            // waxy_ITS
            // String s1 =
            // "(austrodant,(karoochloa,((((miscanthus,zea),pennisetum),((melicaa,((triticum,lygeum),glycerias)),oryza)),((centropodi,merxmuel_r),(merxmuel_m,(chusquea,pharus))))));";
            // String s2 =
            // "(miscanthus,(zea,(pennisetum,((centropodi,((merxmuel_r,(merxmuel_m,(austrodant,karoochloa))),(((lygeum,triticum),(glycerias,melicaa)),(oryza,chusquea)))),pharus))));";

            // // phyb_rbcl
            // String s2 =
            // "(miscanthus,(zea,(pennisetum,(((chasmanthi,thysanolae),(eragrostis,(molinia,phragmites))),((danthonia,aristida),((((chusquea,pseudosasa),lithachne),(((triticum,bromus),avena),oryza)),(puelia,(anomochloa,(flagellari,joinvillea)))))))));";
            // String s1 =
            // "(miscanthus,((pennisetum,((chasmanthi,thysanolae),(((eragrostis,(danthonia,aristida)),(phragmites,molinia)),(((triticum,bromus),avena),(puelia,((lithachne,((chusquea,pseudosasa),oryza)),(anomochloa,(joinvillea,flagellari)))))))),zea));";

            // //rbcl_waxy
            // String s1 =
            // "(miscanthus,((pennisetum,(((centropodi,merxmuel_r),((austrodant,karoochloa),merxmuel_m)),(triticum,((chusquea,oryza),anomochloa)))),zea));";
            // String s2 =
            // "(austrodant,(karoochloa,((((miscanthus,zea),pennisetum),(triticum,oryza)),((centropodi,merxmuel_r),(merxmuel_m,(chusquea,anomochloa))))));";

            // //ndhf_phyB
            // String s1 =
            // "(sporobolus,(eragrostis,(((((((miscanthus,zea),(pennisetum,panicum)),(danthoniop,thysanolae)),chasmanthi),(phragmites,molinia)),danthonia),(aristida,((((oryza,ehrharta),streptogyn),((chusquea,(pseudosasa,(buergersio,((olyra,lithachne),(pariana,eremitis))))),((nardus,lygeum),((((glycerias,melicaa),nassella),(diarrhena,(brachypodi,((triticum,bromus),avena)))),anisopogon)))),(puelia,(pharus,((streptocha,anomochloa),(joinvillea,flagellari)))))))));";
            // String s2 =
            // "(joinvillea,(((streptocha,anomochloa),((((((((zea,miscanthus),(pennisetum,panicum)),danthoniop),((chasmanthi,thysanolae),((sporobolus,eragrostis),(molinia,phragmites)))),(danthonia,aristida)),(streptogyn,(((chusquea,pseudosasa),((olyra,lithachne),(buergersio,(pariana,eremitis)))),(((anisopogon,(((((triticum,bromus),avena),diarrhena),brachypodi),(nassella,(glycerias,melicaa)))),(nardus,lygeum)),(oryza,ehrharta))))),puelia),pharus)),flagellari));";

            // phyb_waxy
            // String s2 =
            // "(miscanthus,(zea,(pennisetum,(danthoniop,(((chusquea,(pariana,eremitis)),(((triticum,(glycerias,melicaa)),lygeum),oryza)),(anomochloa,pharus))))));";
            // String s1 =
            // "(miscanthus,(zea,(pennisetum,(danthoniop,(((pariana,eremitis),((melicaa,((triticum,lygeum),glycerias)),oryza)),(chusquea,(anomochloa,pharus)))))));";

            // rpoc_ITS
            // String s1 =
            // "(gynerium,(micraira,((((miscanthus,zea),(pennisetum,panicum)),thysanolae),((centropodi,((danthonia,(merxmuel_r,(merxmuel_m,((((spartina,eragrostis),(stipagrost,aristida)),(austrodant,karoochloa)),arundo)))),(phragmites,(molinia,(amphipogon,((anisopogon,(stipa,((nardus,lygeum),(bromus,avena)))),(oryza,ehrharta))))))),(chasmanthi,joinvillea)))));";
            // String s2 =
            // "(austrodant,(karoochloa,(danthonia,(merxmuel_m,((((((((miscanthus,zea),(pennisetum,panicum)),chasmanthi),thysanolae),(((spartina,eragrostis),(centropodi,merxmuel_r)),micraira)),((gynerium,((phragmites,molinia),(stipagrost,aristida))),arundo)),(((oryza,ehrharta),(((bromus,avena),(anisopogon,stipa)),(nardus,lygeum))),joinvillea)),amphipogon)))));";

            // cluster of phyB_rbcL
            // String s1 = "(rho,((ch,(mo,er)),(an,ar)));";
            // String s2 = "(rho,(ch,(an,(mo,(ar,er)))));";

            // rbcL_ITS
            // String s1 =
            // "(eriachne,(gynerium,((((miscanthus,zea),pennisetum),thysanolae),((centropodi,((danthonia,(merxmuel_r,(merxmuel_m,(((eragrostis,(stipagrost,aristida)),(austrodant,karoochloa)),arundo)))),(phragmites,(molinia,(amphipogon,((stipa,((triticum,bromus),avena)),((leersia,oryza),(chusquea,lithachne)))))))),(chasmanthi,joinvillea)))));";
            // String s2 =
            // "(miscanthus,((pennisetum,(gynerium,((chasmanthi,thysanolae),(((((((eragrostis,centropodi),merxmuel_r),(((danthonia,(austrodant,karoochloa)),merxmuel_m),(stipagrost,aristida))),eriachne),(phragmites,molinia)),(arundo,amphipogon)),((((triticum,bromus),avena),stipa),((lithachne,(chusquea,(leersia,oryza))),joinvillea)))))),zea));";

            // cluster of phyB_rbcL
            // String s1 =
            // "(rho:1.0,(((eragrostis:1.0,molinia':1.0)eragrostis+molinia':1.0,chasmanthi':1.0)eragrostis+molinia'+chasmanthi':1.0,(aristida':1.0,anomochloa'':1.0)aristida'+anomochloa'':1.0)eragrostis+molinia'+chasmanthi'+aristida'+anomochloa'':1.0)rho+eragrostis+molinia'+chasmanthi'+aristida'+anomochloa''";
            // String s2 =
            // "(rho:1.0,((((eragrostis:1.0,aristida':1.0)eragrostis+aristida':1.0,molinia':1.0)eragrostis+aristida'+molinia':1.0,anomochloa'':1.0)eragrostis+aristida'+molinia'+anomochloa'':1.0,chasmanthi':1.0)eragrostis+aristida'+molinia'+anomochloa''+chasmanthi':1.0)rho+eragrostis+aristida'+molinia'+anomochloa''+chasmanthi'";

            // cluster of phyB_waxy
            // String s1 =
            // "(rho:1.0,((triticum:1.0,(glycerias:1.0,melicaa:1.0)glycerias+melicaa:1.0)triticum+glycerias+melicaa:1.0,lygeum:1.0)triticum+glycerias+melicaa+lygeum:1.0)rho+triticum+glycerias+melicaa+lygeum;";
            // String s2 =
            // "(rho:1.0,(melicaa:1.0,((triticum:1.0,lygeum:1.0)triticum+lygeum:1.0,glycerias:1.0)triticum+lygeum+glycerias:1.0)melicaa+triticum+lygeum+glycerias:1.0)rho+melicaa+triticum+lygeum+glycerias;";

            // cluster of phyB_rpoC
            // String s1 =
            // "(rho:1.0,(miscanthus:1.0,(zea:1.0,((((chasmanthi:1.0,thysanolae:1.0)chasmanthi+thysanolae:1.0,(eragrostis:1.0,(molinia:1.0,phragmites:1.0)molinia+phragmites:1.0)eragrostis+molinia+phragmites:1.0)chasmanthi+thysanolae+eragrostis+molinia+phragmites:1.0,((danthonia:1.0,aristida:1.0)danthonia+aristida:1.0,anisopogon''':1.0)danthonia+aristida+anisopogon''':1.0)chasmanthi+thysanolae+eragrostis+molinia+phragmites+danthonia+aristida+anisopogon''':1.0,panicum':1.0)chasmanthi+thysanolae+eragrostis+molinia+phragmites+danthonia+aristida+anisopogon'''+panicum':1.0)zea+chasmanthi+thysanolae+eragrostis+molinia+phragmites+danthonia+aristida+anisopogon'''+panicum':1.0)miscanthus+zea+chasmanthi+thysanolae+eragrostis+molinia+phragmites+danthonia+aristida+anisopogon'''+panicum':1.0)rho+miscanthus+zea+chasmanthi+thysanolae+eragrostis+molinia+phragmites+danthonia+aristida+anisopogon'''+panicum';";
            // String s2 =
            // "(rho:1.0,(phragmites:1.0,(molinia:1.0,(aristida:1.0,((((((miscanthus:1.0,zea:1.0)miscanthus+zea:1.0,panicum':1.0)miscanthus+zea+panicum':1.0,chasmanthi:1.0)miscanthus+zea+panicum'+chasmanthi:1.0,thysanolae:1.0)miscanthus+zea+panicum'+chasmanthi+thysanolae:1.0,eragrostis:1.0)miscanthus+zea+panicum'+chasmanthi+thysanolae+eragrostis:1.0,(danthonia:1.0,anisopogon''':1.0)danthonia+anisopogon''':1.0)miscanthus+zea+panicum'+chasmanthi+thysanolae+eragrostis+danthonia+anisopogon''':1.0)aristida+miscanthus+zea+panicum'+chasmanthi+thysanolae+eragrostis+danthonia+anisopogon''':1.0)molinia+aristida+miscanthus+zea+panicum'+chasmanthi+thysanolae+eragrostis+danthonia+anisopogon''':1.0)phragmites+molinia+aristida+miscanthus+zea+panicum'+chasmanthi+thysanolae+eragrostis+danthonia+anisopogon''':1.0)rho+phragmites+molinia+aristida+miscanthus+zea+panicum'+chasmanthi+thysanolae+eragrostis+danthonia+anisopogon'''miscanthus;";

            // ndhF_ITS
            // String s1 =
            // "(spartina,(sporobolus,(eragrostis,((((((((((miscanthus,zea),(pennisetum,panicum)),gynerium),thysanolae),chasmanthi),((phragmites,molinia),(arundo,amphipogon))),((danthonia,(austrodant,karoochloa)),merxmuel_m)),((stipagrost,aristida),((((leersia,oryza),ehrharta),((chusquea,lithachne),(brachyelyt,((nardus,lygeum),((((glycerias,melicaa),(nassella,(piptatheru,(stipa,ampelodesm)))),(diarrhena,(brachypodi,((triticum,bromus),avena)))),anisopogon))))),(pharus,(streptocha,joinvillea))))),(centropodi,merxmuel_r)),micraira))));";
            // String s2 =
            // "(gynerium,(micraira,((((miscanthus,zea),(pennisetum,panicum)),thysanolae),((centropodi,((danthonia,(merxmuel_r,(merxmuel_m,(((((sporobolus,spartina),eragrostis),(stipagrost,aristida)),(austrodant,karoochloa)),arundo)))),(phragmites,(molinia,(amphipogon,((ampelodesm,(((piptatheru,anisopogon),(((nassella,stipa),((nardus,lygeum),(brachypodi,((triticum,bromus),avena)))),((glycerias,melicaa),brachyelyt))),diarrhena)),(((leersia,oryza),ehrharta),(chusquea,lithachne)))))))),(chasmanthi,(pharus,(joinvillea,streptocha)))))));";

            // //ndhF_waxy
            // String s1 =
            // "(centropodi,(merxmuel_r,(((((miscanthus,zea),pennisetum),danthoniop),((austrodant,karoochloa),merxmuel_m)),((oryza,((chusquea,(pariana,eremitis)),(lygeum,((glycerias,melicaa),triticum)))),(pharus,anomochloa)))));";
            // String s2 =
            // "(pharus,((chusquea,(merxmuel_m,(((austrodant,karoochloa),((((zea,miscanthus),pennisetum),danthoniop),((pariana,eremitis),((melicaa,((triticum,lygeum),glycerias)),oryza)))),(centropodi,merxmuel_r)))),anomochloa));";

            // cluster of rbcL_ITS
            // String s1 =
            // "(rho:1.0,(eriachne:1.0,(gynerium:1.0,((((miscanthus:1.0,zea:1.0)miscanthus+zea:1.0,pennisetum:1.0)miscanthus+zea+pennisetum:1.0,thysanolae:1.0)miscanthus+zea+pennisetum+thysanolae:1.0,((centropodi:1.0,((danthonia:1.0,(merxmuel_r:1.0,(merxmuel_m:1.0,(((eragrostis:1.0,aristida':1.0)eragrostis+aristida':1.0,austrodant':1.0)eragrostis+aristida'+austrodant':1.0,arundo:1.0)eragrostis+aristida'+austrodant'+arundo:1.0)merxmuel_m+eragrostis+aristida'+austrodant'+arundo:1.0)merxmuel_r+merxmuel_m+eragrostis+aristida'+austrodant'+arundo:1.0)danthonia+merxmuel_r+merxmuel_m+eragrostis+aristida'+austrodant'+arundo:1.0,(phragmites:1.0,(molinia:1.0,(amphipogon:1.0,(avena':1.0,chusquea':1.0)avena'+chusquea':1.0)amphipogon+avena'+chusquea':1.0)molinia+amphipogon+avena'+chusquea':1.0)phragmites+molinia+amphipogon+avena'+chusquea':1.0)danthonia+merxmuel_r+merxmuel_m+eragrostis+aristida'+austrodant'+arundo+phragmites+molinia+amphipogon+avena'+chusquea':1.0)centropodi+danthonia+merxmuel_r+merxmuel_m+eragrostis+aristida'+austrodant'+arundo+phragmites+molinia+amphipogon+avena'+chusquea':1.0,(chasmanthi:1.0,joinvillea:1.0)chasmanthi+joinvillea:1.0)centropodi+danthonia+merxmuel_r+merxmuel_m+eragrostis+aristida'+austrodant'+arundo+phragmites+molinia+amphipogon+avena'+chusquea'+chasmanthi+joinvillea:1.0)miscanthus+zea+pennisetum+thysanolae+centropodi+danthonia+merxmuel_r+merxmuel_m+eragrostis+aristida'+austrodant'+arundo+phragmites+molinia+amphipogon+avena'+chusquea'+chasmanthi+joinvillea:1.0)gynerium+miscanthus+zea+pennisetum+thysanolae+centropodi+danthonia+merxmuel_r+merxmuel_m+eragrostis+aristida'+austrodant'+arundo+phragmites+molinia+amphipogon+avena'+chusquea'+chasmanthi+joinvillea:1.0)eriachne+gynerium+miscanthus+zea+pennisetum+thysanolae+centropodi+danthonia+merxmuel_r+merxmuel_m+eragrostis+aristida'+austrodant'+arundo+phragmites+molinia+amphipogon+avena'+chusquea'+chasmanthi+joinvillea:1.0)rho+eriachne+gynerium+miscanthus+zea+pennisetum+thysanolae+centropodi+danthonia+merxmuel_r+merxmuel_m+eragrostis+aristida'+austrodant'+arundo+phragmites+molinia+amphipogon+avena'+chusquea'+chasmanthi+joinvillea";
            // String s2 =
            // "(rho:1.0,(miscanthus:1.0,((pennisetum:1.0,(gynerium:1.0,((chasmanthi:1.0,thysanolae:1.0)chasmanthi+thysanolae:1.0,(((((((eragrostis:1.0,centropodi:1.0)eragrostis+centropodi:1.0,merxmuel_r:1.0)eragrostis+centropodi+merxmuel_r:1.0,(((danthonia:1.0,austrodant':1.0)danthonia+austrodant':1.0,merxmuel_m:1.0)danthonia+austrodant'+merxmuel_m:1.0,aristida':1.0)danthonia+austrodant'+merxmuel_m+aristida':1.0)eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida':1.0,eriachne:1.0)eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne:1.0,(phragmites:1.0,molinia:1.0)phragmites+molinia:1.0)eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne+phragmites+molinia:1.0,(arundo:1.0,amphipogon:1.0)arundo+amphipogon:1.0)eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne+phragmites+molinia+arundo+amphipogon:1.0,((joinvillea:1.0,chusquea':1.0)joinvillea+chusquea':1.0,avena':1.0)joinvillea+chusquea'+avena':1.0)eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne+phragmites+molinia+arundo+amphipogon+joinvillea+chusquea'+avena':1.0)chasmanthi+thysanolae+eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne+phragmites+molinia+arundo+amphipogon+joinvillea+chusquea'+avena':1.0)gynerium+chasmanthi+thysanolae+eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne+phragmites+molinia+arundo+amphipogon+joinvillea+chusquea'+avena':1.0)pennisetum+gynerium+chasmanthi+thysanolae+eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne+phragmites+molinia+arundo+amphipogon+joinvillea+chusquea'+avena':1.0,zea:1.0)pennisetum+gynerium+chasmanthi+thysanolae+eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne+phragmites+molinia+arundo+amphipogon+joinvillea+chusquea'+avena'+zea:1.0)miscanthus+pennisetum+gynerium+chasmanthi+thysanolae+eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne+phragmites+molinia+arundo+amphipogon+joinvillea+chusquea'+avena'+zea:1.0)rho+miscanthus+pennisetum+gynerium+chasmanthi+thysanolae+eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne+phragmites+molinia+arundo+amphipogon+joinvillea+chusquea'+avena'+zea";

            // cluster of rpoC_ITS
            // String s1 =
            // "(rho:1.0,(gynerium:1.0,(micraira:1.0,((thysanolae:1.0,miscanthus':1.0)thysanolae+miscanthus':1.0,((centropodi:1.0,((danthonia:1.0,(merxmuel_r:1.0,(merxmuel_m:1.0,(((eragrostis':1.0,aristida':1.0)eragrostis'+aristida':1.0,(austrodant:1.0,karoochloa:1.0)austrodant+karoochloa:1.0)eragrostis'+aristida'+austrodant+karoochloa:1.0,arundo:1.0)eragrostis'+aristida'+austrodant+karoochloa+arundo:1.0)merxmuel_m+eragrostis'+aristida'+austrodant+karoochloa+arundo:1.0)merxmuel_r+merxmuel_m+eragrostis'+aristida'+austrodant+karoochloa+arundo:1.0)danthonia+merxmuel_r+merxmuel_m+eragrostis'+aristida'+austrodant+karoochloa+arundo:1.0,(phragmites:1.0,(molinia:1.0,(amphipogon:1.0,anisopogon'':1.0)amphipogon+anisopogon'':1.0)molinia+amphipogon+anisopogon'':1.0)phragmites+molinia+amphipogon+anisopogon'':1.0)danthonia+merxmuel_r+merxmuel_m+eragrostis'+aristida'+austrodant+karoochloa+arundo+phragmites+molinia+amphipogon+anisopogon'':1.0)centropodi+danthonia+merxmuel_r+merxmuel_m+eragrostis'+aristida'+austrodant+karoochloa+arundo+phragmites+molinia+amphipogon+anisopogon'':1.0,(chasmanthi:1.0,joinvillea:1.0)chasmanthi+joinvillea:1.0)centropodi+danthonia+merxmuel_r+merxmuel_m+eragrostis'+aristida'+austrodant+karoochloa+arundo+phragmites+molinia+amphipogon+anisopogon''+chasmanthi+joinvillea:1.0)thysanolae+miscanthus'+centropodi+danthonia+merxmuel_r+merxmuel_m+eragrostis'+aristida'+austrodant+karoochloa+arundo+phragmites+molinia+amphipogon+anisopogon''+chasmanthi+joinvillea:1.0)micraira+thysanolae+miscanthus'+centropodi+danthonia+merxmuel_r+merxmuel_m+eragrostis'+aristida'+austrodant+karoochloa+arundo+phragmites+molinia+amphipogon+anisopogon''+chasmanthi+joinvillea:1.0)gynerium+micraira+thysanolae+miscanthus'+centropodi+danthonia+merxmuel_r+merxmuel_m+eragrostis'+aristida'+austrodant+karoochloa+arundo+phragmites+molinia+amphipogon+anisopogon''+chasmanthi+joinvillea:1.0)rho+gynerium+micraira+thysanolae+miscanthus'+centropodi+danthonia+merxmuel_r+merxmuel_m+eragrostis'+aristida'+austrodant+karoochloa+arundo+phragmites+molinia+amphipogon+anisopogon''+chasmanthi+joinvillea;";
            // String s2 =
            // "(rho:1.0,(austrodant:1.0,(karoochloa:1.0,(danthonia:1.0,(merxmuel_m:1.0,((((((chasmanthi:1.0,miscanthus':1.0)chasmanthi+miscanthus':1.0,thysanolae:1.0)chasmanthi+miscanthus'+thysanolae:1.0,(((centropodi:1.0,merxmuel_r:1.0)centropodi+merxmuel_r:1.0,eragrostis':1.0)centropodi+merxmuel_r+eragrostis':1.0,micraira:1.0)centropodi+merxmuel_r+eragrostis'+micraira:1.0)chasmanthi+miscanthus'+thysanolae+centropodi+merxmuel_r+eragrostis'+micraira:1.0,((gynerium:1.0,((phragmites:1.0,molinia:1.0)phragmites+molinia:1.0,aristida':1.0)phragmites+molinia+aristida':1.0)gynerium+phragmites+molinia+aristida':1.0,arundo:1.0)gynerium+phragmites+molinia+aristida'+arundo:1.0)chasmanthi+miscanthus'+thysanolae+centropodi+merxmuel_r+eragrostis'+micraira+gynerium+phragmites+molinia+aristida'+arundo:1.0,(joinvillea:1.0,anisopogon'':1.0)joinvillea+anisopogon'':1.0)chasmanthi+miscanthus'+thysanolae+centropodi+merxmuel_r+eragrostis'+micraira+gynerium+phragmites+molinia+aristida'+arundo+joinvillea+anisopogon'':1.0,amphipogon:1.0)chasmanthi+miscanthus'+thysanolae+centropodi+merxmuel_r+eragrostis'+micraira+gynerium+phragmites+molinia+aristida'+arundo+joinvillea+anisopogon''+amphipogon:1.0)merxmuel_m+chasmanthi+miscanthus'+thysanolae+centropodi+merxmuel_r+eragrostis'+micraira+gynerium+phragmites+molinia+aristida'+arundo+joinvillea+anisopogon''+amphipogon:1.0)danthonia+merxmuel_m+chasmanthi+miscanthus'+thysanolae+centropodi+merxmuel_r+eragrostis'+micraira+gynerium+phragmites+molinia+aristida'+arundo+joinvillea+anisopogon''+amphipogon:1.0)karoochloa+danthonia+merxmuel_m+chasmanthi+miscanthus'+thysanolae+centropodi+merxmuel_r+eragrostis'+micraira+gynerium+phragmites+molinia+aristida'+arundo+joinvillea+anisopogon''+amphipogon:1.0)austrodant+karoochloa+danthonia+merxmuel_m+chasmanthi+miscanthus'+thysanolae+centropodi+merxmuel_r+eragrostis'+micraira+gynerium+phragmites+molinia+aristida'+arundo+joinvillea+anisopogon''+amphipogon:1.0)rho+austrodant+karoochloa+danthonia+merxmuel_m+chasmanthi+miscanthus'+thysanolae+centropodi+merxmuel_r+eragrostis'+micraira+gynerium+phragmites+molinia+aristida'+arundo+joinvillea+anisopogon''+amphipogon;";

            // cluster of phyB_waxy
            // String s1 =
            // "(rho:1.0,((triticum:1.0,(glycerias:1.0,melicaa:1.0)glycerias+melicaa:1.0)triticum+glycerias+melicaa:1.0,lygeum:1.0)triticum+glycerias+melicaa+lygeum:1.0)rho+triticum+glycerias+melicaa+lygeum;";
            // String s2 =
            // "(rho:1.0,(melicaa:1.0,((triticum:1.0,lygeum:1.0)triticum+lygeum:1.0,glycerias:1.0)triticum+lygeum+glycerias:1.0)melicaa+triticum+lygeum+glycerias:1.0)rho+melicaa+triticum+lygeum+glycerias;";

            // Cluster of waxy_ITS
            // String s1 =
            // "(rho:1.0,(austrodant:1.0,(karoochloa:1.0,((((miscanthus:1.0,zea:1.0)miscanthus+zea:1.0,pennisetum:1.0)miscanthus+zea+pennisetum:1.0,(oryza:1.0,glycerias':1.0)oryza+glycerias':1.0)miscanthus+zea+pennisetum+oryza+glycerias':1.0,((centropodi:1.0,merxmuel_r:1.0)centropodi+merxmuel_r:1.0,(merxmuel_m:1.0,(chusquea:1.0,pharus:1.0)chusquea+pharus:1.0)merxmuel_m+chusquea+pharus:1.0)centropodi+merxmuel_r+merxmuel_m+chusquea+pharus:1.0)miscanthus+zea+pennisetum+oryza+glycerias'+centropodi+merxmuel_r+merxmuel_m+chusquea+pharus:1.0)karoochloa+miscanthus+zea+pennisetum+oryza+glycerias'+centropodi+merxmuel_r+merxmuel_m+chusquea+pharus:1.0)austrodant+karoochloa+miscanthus+zea+pennisetum+oryza+glycerias'+centropodi+merxmuel_r+merxmuel_m+chusquea+pharus:1.0)rho+austrodant+karoochloa+miscanthus+zea+pennisetum+oryza+glycerias'+centropodi+merxmuel_r+merxmuel_m+chusquea+pharus;";
            // String s2 =
            // "(rho:1.0,(miscanthus:1.0,(zea:1.0,(pennisetum:1.0,((centropodi:1.0,((merxmuel_r:1.0,(merxmuel_m:1.0,(austrodant:1.0,karoochloa:1.0)austrodant+karoochloa:1.0)merxmuel_m+austrodant+karoochloa:1.0)merxmuel_r+merxmuel_m+austrodant+karoochloa:1.0,((oryza:1.0,chusquea:1.0)oryza+chusquea:1.0,glycerias':1.0)oryza+chusquea+glycerias':1.0)merxmuel_r+merxmuel_m+austrodant+karoochloa+oryza+chusquea+glycerias':1.0)centropodi+merxmuel_r+merxmuel_m+austrodant+karoochloa+oryza+chusquea+glycerias':1.0,pharus:1.0)centropodi+merxmuel_r+merxmuel_m+austrodant+karoochloa+oryza+chusquea+glycerias'+pharus:1.0)pennisetum+centropodi+merxmuel_r+merxmuel_m+austrodant+karoochloa+oryza+chusquea+glycerias'+pharus:1.0)zea+pennisetum+centropodi+merxmuel_r+merxmuel_m+austrodant+karoochloa+oryza+chusquea+glycerias'+pharus:1.0)miscanthus+zea+pennisetum+centropodi+merxmuel_r+merxmuel_m+austrodant+karoochloa+oryza+chusquea+glycerias'+pharus:1.0)rho+miscanthus+zea+pennisetum+centropodi+merxmuel_r+merxmuel_m+austrodant+karoochloa+oryza+chusquea+gly;";

            // //Cluster of ndhf_waxy
            // String s1 =
            // "(rho:1.0,(centropodi:1.0,(merxmuel_r:1.0,(((merxmuel_m:1.0,austrodant':1.0)merxmuel_m+austrodant':1.0,danthoniop':1.0)merxmuel_m+austrodant'+danthoniop':1.0,((oryza:1.0,((chusquea:1.0,eremitis':1.0)chusquea+eremitis':1.0,glycerias':1.0)chusquea+eremitis'+glycerias':1.0)oryza+chusquea+eremitis'+glycerias':1.0,(pharus:1.0,anomochloa:1.0)pharus+anomochloa:1.0)oryza+chusquea+eremitis'+glycerias'+pharus+anomochloa:1.0)merxmuel_m+austrodant'+danthoniop'+oryza+chusquea+eremitis'+glycerias'+pharus+anomochloa:1.0)merxmuel_r+merxmuel_m+austrodant'+danthoniop'+oryza+chusquea+eremitis'+glycerias'+pharus+anomochloa:1.0)centropodi+merxmuel_r+merxmuel_m+austrodant'+danthoniop'+oryza+chusquea+eremitis'+glycerias'+pharus+anomochloa:1.0)rho+centropodi+merxmuel_r+merxmuel_m+austrodant'+danthoniop'+oryza+chusquea+eremitis'+glycerias'+pharus+anomochloa";
            // String s2 =
            // "(rho:1.0,(pharus:1.0,((chusquea:1.0,(merxmuel_m:1.0,(((((oryza:1.0,glycerias':1.0)oryza+glycerias':1.0,eremitis':1.0)oryza+glycerias'+eremitis':1.0,danthoniop':1.0)oryza+glycerias'+eremitis'+danthoniop':1.0,austrodant':1.0)oryza+glycerias'+eremitis'+danthoniop'+austrodant':1.0,(centropodi:1.0,merxmuel_r:1.0)centropodi+merxmuel_r:1.0)oryza+glycerias'+eremitis'+danthoniop'+austrodant'+centropodi+merxmuel_r:1.0)merxmuel_m+oryza+glycerias'+eremitis'+danthoniop'+austrodant'+centropodi+merxmuel_r:1.0)chusquea+merxmuel_m+oryza+glycerias'+eremitis'+danthoniop'+austrodant'+centropodi+merxmuel_r:1.0,anomochloa:1.0)chusquea+merxmuel_m+oryza+glycerias'+eremitis'+danthoniop'+austrodant'+centropodi+merxmuel_r+anomochloa:1.0)pharus+chusquea+merxmuel_m+oryza+glycerias'+eremitis'+danthoniop'+austrodant'+centropodi+merxmuel_r+anomochloa:1.0)rho+pharus+chusquea+merxmuel_m+oryza+glycerias'+eremitis'+danthoniop'+austrodant'+centropodi+merxmuel_r+anomochloa";

            // test-100
            // String s1 =
            // "(rho:1.0,((((((19:1.0,(85:1.0,38:1.0)85+38:1.0)19+85+38:1.0,2':1.0)19+85+38+2':1.0,18':1.0)19+85+38+2'+18':1.0,((28:1.0,(4:1.0,(((55:1.0,84':1.0)55+84':1.0,90:1.0)55+84'+90:1.0,1':1.0)55+84'+90+1':1.0)4+55+84'+90+1':1.0)28+4+55+84'+90+1':1.0,((87:1.0,98:1.0)87+98:1.0,(((35:1.0,62:1.0)35+62:1.0,30':1.0)35+62+30':1.0,((63:1.0,76:1.0)63+76:1.0,((58:1.0,10:1.0)58+10:1.0,22:1.0)58+10+22:1.0)63+76+58+10+22:1.0)35+62+30'+63+76+58+10+22:1.0)87+98+35+62+30'+63+76+58+10+22:1.0)28+4+55+84'+90+1'+87+98+35+62+30'+63+76+58+10+22:1.0)19+85+38+2'+18'+28+4+55+84'+90+1'+87+98+35+62+30'+63+76+58+10+22:1.0,(((((((70:1.0,(16:1.0,83:1.0)16+83:1.0)70+16+83:1.0,17:1.0)70+16+83+17:1.0,((53:1.0,72:1.0)53+72:1.0,91:1.0)53+72+91:1.0)70+16+83+17+53+72+91:1.0,(97:1.0,(36:1.0,3:1.0)36+3:1.0)97+36+3:1.0)70+16+83+17+53+72+91+97+36+3:1.0,(((75:1.0,31':1.0)75+31':1.0,(((13:1.0,45:1.0)13+45:1.0,81:1.0)13+45+81:1.0,23:1.0)13+45+81+23:1.0)75+31'+13+45+81+23:1.0,34:1.0)75+31'+13+45+81+23+34:1.0)70+16+83+17+53+72+91+97+36+3+75+31'+13+45+81+23+34:1.0,((65:1.0,21:1.0)65+21:1.0,(60:1.0,(((((39:1.0,(50:1.0,5:1.0)50+5:1.0)39+50+5:1.0,(14:1.0,80:1.0)14+80:1.0)39+50+5+14+80:1.0,64:1.0)39+50+5+14+80+64:1.0,66':1.0)39+50+5+14+80+64+66':1.0,((41:1.0,79:1.0)41+79:1.0,88:1.0)41+79+88:1.0)39+50+5+14+80+64+66'+41+79+88:1.0)60+39+50+5+14+80+64+66'+41+79+88:1.0)65+21+60+39+50+5+14+80+64+66'+41+79+88:1.0)70+16+83+17+53+72+91+97+36+3+75+31'+13+45+81+23+34+65+21+60+39+50+5+14+80+64+66'+41+79+88:1.0,(((0:1.0,(((54:1.0,12:1.0)54+12:1.0,20':1.0)54+12+20':1.0,(96:1.0,((95:1.0,(15:1.0,86:1.0)15+86:1.0)95+15+86:1.0,61':1.0)95+15+86+61':1.0)96+95+15+86+61':1.0)54+12+20'+96+95+15+86+61':1.0)0+54+12+20'+96+95+15+86+61':1.0,((47:1.0,24:1.0)47+24:1.0,26':1.0)47+24+26':1.0)0+54+12+20'+96+95+15+86+61'+47+24+26':1.0,((78:1.0,((27:1.0,59:1.0)27+59:1.0,(25:1.0,52:1.0)25+52:1.0)27+59+25+52:1.0)78+27+59+25+52:1.0,((((11:1.0,33:1.0)11+33:1.0,8:1.0)11+33+8:1.0,56:1.0)11+33+8+56:1.0,42:1.0)11+33+8+56+42:1.0)78+27+59+25+52+11+33+8+56+42:1.0)0+54+12+20'+96+95+15+86+61'+47+24+26'+78+27+59+25+52+11+33+8+56+42:1.0)70+16+83+17+53+72+91+97+36+3+75+31'+13+45+81+23+34+65+21+60+39+50+5+14+80+64+66'+41+79+88+0+54+12+20'+96+95+15+86+61'+47+24+26'+78+27+59+25+52+11+33+8+56+42:1.0)19+85+38+2'+18'+28+4+55+84'+90+1'+87+98+35+62+30'+63+76+58+10+22+70+16+83+17+53+72+91+97+36+3+75+31'+13+45+81+23+34+65+21+60+39+50+5+14+80+64+66'+41+79+88+0+54+12+20'+96+95+15+86+61'+47+24+26'+78+27+59+25+52+11+33+8+56+42:0.5,((((7:1.0,((57:1.0,9:1.0)57+9:1.0,37:1.0)57+9+37:1.0)7+57+9+37:1.0,89':1.0)7+57+9+37+89':1.0,71:1.0)7+57+9+37+89'+71:1.0,((74:1.0,68:1.0)74+68:1.0,6':1.0)74+68+6':1.0)7+57+9+37+89'+71+74+68+6':0.5)19+85+38+2'+18'+28+4+55+84'+90+1'+87+98+35+62+30'+63+76+58+10+22+70+16+83+17+53+72+91+97+36+3+75+31'+13+45+81+23+34+65+21+60+39+50+5+14+80+64+66'+41+79+88+0+54+12+20'+96+95+15+86+61'+47+24+26'+78+27+59+25+52+11+33+8+56+42+7+57+9+37+89'+71+74+68+6':1.0)rho+19+85+38+2'+18'+28+4+55+84'+90+1'+87+98+35+62+30'+63+76+58+10+22+70+16+83+17+53+72+91+97+36+3+75+31'+13+45+81+23+34+65+21+60+39+50+5+14+80+64+66'+41+79+88+0+54+12+20'+96+95+15+86+61'+47+24+26'+78+27+59+25+52+11+33+8+56+42+7+57+9+37+89'+71+74+68+6';";
            // String s2 =
            // "(rho:1.0,(((((19:1.0,2':1.0)19+2':1.0,(85:1.0,18':1.0)85+18':1.0)19+2'+85+18':1.0,(28:1.0,4:1.0)28+4:1.0)19+2'+85+18'+28+4:1.0,((((((70:1.0,((17:1.0,65:1.0)17+65:1.0,83:1.0)17+65+83:1.0)70+17+65+83:1.0,(91:1.0,(72:1.0,16:1.0)72+16:1.0)91+72+16:1.0)70+17+65+83+91+72+16:1.0,52:1.0)70+17+65+83+91+72+16+52:1.0,34:1.0)70+17+65+83+91+72+16+52+34:1.0,(21:1.0,((88:1.0,(79:1.0,50:1.0)79+50:1.0)88+79+50:1.0,(((39:1.0,5:1.0)39+5:1.0,(80:1.0,(41:1.0,(14:1.0,53:1.0)14+53:1.0)41+14+53:1.0)80+41+14+53:1.0)39+5+80+41+14+53:1.0,64:1.0)39+5+80+41+14+53+64:1.0)88+79+50+39+5+80+41+14+53+64:1.0)21+88+79+50+39+5+80+41+14+53+64:1.0)70+17+65+83+91+72+16+52+34+21+88+79+50+39+5+80+41+14+53+64:1.0,(((24:1.0,26':1.0)24+26':1.0,0:1.0)24+26'+0:1.0,((78:1.0,(59:1.0,((((((76:1.0,(63:1.0,87:1.0)63+87:1.0)76+63+87:1.0,10:1.0)76+63+87+10:1.0,30':1.0)76+63+87+10+30':1.0,(((((22:1.0,(55:1.0,((((95:1.0,61':1.0)95+61':1.0,(86:1.0,(15:1.0,12:1.0)15+12:1.0)86+15+12:1.0)95+61'+86+15+12:1.0,20':1.0)95+61'+86+15+12+20':1.0,35:1.0)95+61'+86+15+12+20'+35:1.0)55+95+61'+86+15+12+20'+35:1.0)22+55+95+61'+86+15+12+20'+35:1.0,84':1.0)22+55+95+61'+86+15+12+20'+35+84':1.0,90:1.0)22+55+95+61'+86+15+12+20'+35+84'+90:1.0,58:1.0)22+55+95+61'+86+15+12+20'+35+84'+90+58:1.0,1':1.0)22+55+95+61'+86+15+12+20'+35+84'+90+58+1':1.0)76+63+87+10+30'+22+55+95+61'+86+15+12+20'+35+84'+90+58+1':1.0,98:1.0)76+63+87+10+30'+22+55+95+61'+86+15+12+20'+35+84'+90+58+1'+98:1.0,(54:1.0,((47:1.0,((23:1.0,((81:1.0,((75:1.0,66':1.0)75+66':1.0,((13:1.0,(45:1.0,36:1.0)45+36:1.0)13+45+36:1.0,60:1.0)13+45+36+60:1.0)75+66'+13+45+36+60:1.0)81+75+66'+13+45+36+60:1.0,62:1.0)81+75+66'+13+45+36+60+62:1.0)23+81+75+66'+13+45+36+60+62:1.0,((97:1.0,3:1.0)97+3:1.0,31':1.0)97+3+31':1.0)23+81+75+66'+13+45+36+60+62+97+3+31':1.0)47+23+81+75+66'+13+45+36+60+62+97+3+31':1.0,38:1.0)47+23+81+75+66'+13+45+36+60+62+97+3+31'+38:1.0)54+47+23+81+75+66'+13+45+36+60+62+97+3+31'+38:1.0)76+63+87+10+30'+22+55+95+61'+86+15+12+20'+35+84'+90+58+1'+98+54+47+23+81+75+66'+13+45+36+60+62+97+3+31'+38:1.0)59+76+63+87+10+30'+22+55+95+61'+86+15+12+20'+35+84'+90+58+1'+98+54+47+23+81+75+66'+13+45+36+60+62+97+3+31'+38:1.0)78+59+76+63+87+10+30'+22+55+95+61'+86+15+12+20'+35+84'+90+58+1'+98+54+47+23+81+75+66'+13+45+36+60+62+97+3+31'+38:1.0,((56:1.0,((27:1.0,(((8:1.0,33:1.0)8+33:1.0,96:1.0)8+33+96:1.0,25:1.0)8+33+96+25:1.0)27+8+33+96+25:1.0,11:1.0)27+8+33+96+25+11:1.0)56+27+8+33+96+25+11:1.0,42:1.0)56+27+8+33+96+25+11+42:1.0)78+59+76+63+87+10+30'+22+55+95+61'+86+15+12+20'+35+84'+90+58+1'+98+54+47+23+81+75+66'+13+45+36+60+62+97+3+31'+38+56+27+8+33+96+25+11+42:1.0)24+26'+0+78+59+76+63+87+10+30'+22+55+95+61'+86+15+12+20'+35+84'+90+58+1'+98+54+47+23+81+75+66'+13+45+36+60+62+97+3+31'+38+56+27+8+33+96+25+11+42:1.0)70+17+65+83+91+72+16+52+34+21+88+79+50+39+5+80+41+14+53+64+24+26'+0+78+59+76+63+87+10+30'+22+55+95+61'+86+15+12+20'+35+84'+90+58+1'+98+54+47+23+81+75+66'+13+45+36+60+62+97+3+31'+38+56+27+8+33+96+25+11+42:1.0)19+2'+85+18'+28+4+70+17+65+83+91+72+16+52+34+21+88+79+50+39+5+80+41+14+53+64+24+26'+0+78+59+76+63+87+10+30'+22+55+95+61'+86+15+12+20'+35+84'+90+58+1'+98+54+47+23+81+75+66'+13+45+36+60+62+97+3+31'+38+56+27+8+33+96+25+11+42:0.5,(((((7:1.0,(57:1.0,(37:1.0,9:1.0)37+9:1.0)57+37+9:1.0)7+57+37+9:1.0,68:1.0)7+57+37+9+68:1.0,89':1.0)7+57+37+9+68+89':1.0,71:1.0)7+57+37+9+68+89'+71:1.0,(74:1.0,6':1.0)74+6':1.0)7+57+37+9+68+89'+71+74+6':0.5)19+2'+85+18'+28+4+70+17+65+83+91+72+16+52+34+21+88+79+50+39+5+80+41+14+53+64+24+26'+0+78+59+76+63+87+10+30'+22+55+95+61'+86+15+12+20'+35+84'+90+58+1'+98+54+47+23+81+75+66'+13+45+36+60+62+97+3+31'+38+56+27+8+33+96+25+11+42+7+57+37+9+68+89'+71+74+6':1.0)rho+19+2'+85+18'+28+4+70+17+65+83+91+72+16+52+34+21+88+79+50+39+5+80+41+14+53+64+24+26'+0+78+59+76+63+87+10+30'+22+55+95+61'+86+15+12+20'+35+84'+90+58+1'+98+54+47+23+81+75+66'+13+45+36+60+62+97+3+31'+38+56+27+8+33+96+25+11+42+7+57+37+9+68+89'+71+74+6';";

            // Celine-example
            // String s1 =
            // "(((((T_urartu:100.0,T_monococcum:100.0):100.0,((Ae_tauschii:100.0,(Ae_comosa:100.0,Ae_uniaristata:100.0):92.0):48.0,(((Ae_longissima:100.0,Ae_sharonensis:100.0):90.0,Ae_bicornis:100.0):68.0,Ae_umbellulata:100.0):15.0):19.0):80.0,((Cytotype_speltoides1:100.0,T_timopheevii:100.0):61.0,T_turgidum:100.0):87.0):99.0,Hordeum:100.0):62.0,H_marinum:100.0);";
            // String s2 =
            // "(((((T_urartu:100.0,T_monococcum:100.0):100.0,((Ae_tauschii:100.0,(Ae_comosa:100.0,Ae_uniaristata:100.0):92.0):48.0,(((Ae_longissima:100.0,Ae_sharonensis:100.0):90.0,Ae_bicornis:100.0):68.0,Ae_umbellulata:100.0):15.0):19.0):80.0,((Cytotype_speltoides1:100.0,T_timopheevii:100.0):61.0,T_turgidum:100.0):87.0):99.0,Hordeum:100.0):62.0,H_marinum:100.0);";

            // bad-example
            // String s1 =
            // "(((((47,45),43),((44,18),4)),10),((((36,25),11),16),(((((((37,35),31),1),17),19),0),14)))";
            // String s2 =
            // "(((43,(((18,((36,(25,(37,(35,(45,(47,((31,1),17))))))),11)),44),4)),10),(((19,0),14),16));";

            // bad-example
            String s1 = "(rho:1.0,(miscanthus:1.0,((pennisetum:1.0,(gynerium:1.0,((chasmanthi:1.0,thysanolae:1.0)chasmanthi+thysanolae:1.0,(((((((eragrostis:1.0,centropodi:1.0)eragrostis+centropodi:1.0,merxmuel_r:1.0)eragrostis+centropodi+merxmuel_r:1.0,(((danthonia:1.0,austrodant':1.0)danthonia+austrodant':1.0,merxmuel_m:1.0)danthonia+austrodant'+merxmuel_m:1.0,aristida':1.0)danthonia+austrodant'+merxmuel_m+aristida':1.0)eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida':1.0,eriachne:1.0)eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne:1.0,(phragmites:1.0,molinia:1.0)phragmites+molinia:1.0)eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne+phragmites+molinia:1.0,(arundo:1.0,amphipogon:1.0)arundo+amphipogon:1.0)eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne+phragmites+molinia+arundo+amphipogon:1.0,((joinvillea:1.0,chusquea':1.0)joinvillea+chusquea':1.0,avena':1.0)joinvillea+chusquea'+avena':1.0)eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne+phragmites+molinia+arundo+amphipogon+joinvillea+chusquea'+avena':1.0)chasmanthi+thysanolae+eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne+phragmites+molinia+arundo+amphipogon+joinvillea+chusquea'+avena':1.0)gynerium+chasmanthi+thysanolae+eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne+phragmites+molinia+arundo+amphipogon+joinvillea+chusquea'+avena':1.0)pennisetum+gynerium+chasmanthi+thysanolae+eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne+phragmites+molinia+arundo+amphipogon+joinvillea+chusquea'+avena':1.0,zea:1.0)pennisetum+gynerium+chasmanthi+thysanolae+eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne+phragmites+molinia+arundo+amphipogon+joinvillea+chusquea'+avena'+zea:1.0)miscanthus+pennisetum+gynerium+chasmanthi+thysanolae+eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne+phragmites+molinia+arundo+amphipogon+joinvillea+chusquea'+avena'+zea:1.0)rho+miscanthus+pennisetum+gynerium+chasmanthi+thysanolae+eragrostis+centropodi+merxmuel_r+danthonia+austrodant'+merxmuel_m+aristida'+eriachne+phragmites+molinia+arundo+amphipogon+joinvillea+chusquea'+avena'+zea;";
            String s2 = "(rho:1.0,(eriachne:1.0,(gynerium:1.0,((((miscanthus:1.0,zea:1.0)miscanthus+zea:1.0,pennisetum:1.0)miscanthus+zea+pennisetum:1.0,thysanolae:1.0)miscanthus+zea+pennisetum+thysanolae:1.0,((centropodi:1.0,((danthonia:1.0,(merxmuel_r:1.0,(merxmuel_m:1.0,(((eragrostis:1.0,aristida':1.0)eragrostis+aristida':1.0,austrodant':1.0)eragrostis+aristida'+austrodant':1.0,arundo:1.0)eragrostis+aristida'+austrodant'+arundo:1.0)merxmuel_m+eragrostis+aristida'+austrodant'+arundo:1.0)merxmuel_r+merxmuel_m+eragrostis+aristida'+austrodant'+arundo:1.0)danthonia+merxmuel_r+merxmuel_m+eragrostis+aristida'+austrodant'+arundo:1.0,(phragmites:1.0,(molinia:1.0,(amphipogon:1.0,(avena':1.0,chusquea':1.0)avena'+chusquea':1.0)amphipogon+avena'+chusquea':1.0)molinia+amphipogon+avena'+chusquea':1.0)phragmites+molinia+amphipogon+avena'+chusquea':1.0)danthonia+merxmuel_r+merxmuel_m+eragrostis+aristida'+austrodant'+arundo+phragmites+molinia+amphipogon+avena'+chusquea':1.0)centropodi+danthonia+merxmuel_r+merxmuel_m+eragrostis+aristida'+austrodant'+arundo+phragmites+molinia+amphipogon+avena'+chusquea':1.0,(chasmanthi:1.0,joinvillea:1.0)chasmanthi+joinvillea:1.0)centropodi+danthonia+merxmuel_r+merxmuel_m+eragrostis+aristida'+austrodant'+arundo+phragmites+molinia+amphipogon+avena'+chusquea'+chasmanthi+joinvillea:1.0)miscanthus+zea+pennisetum+thysanolae+centropodi+danthonia+merxmuel_r+merxmuel_m+eragrostis+aristida'+austrodant'+arundo+phragmites+molinia+amphipogon+avena'+chusquea'+chasmanthi+joinvillea:1.0)gynerium+miscanthus+zea+pennisetum+thysanolae+centropodi+danthonia+merxmuel_r+merxmuel_m+eragrostis+aristida'+austrodant'+arundo+phragmites+molinia+amphipogon+avena'+chusquea'+chasmanthi+joinvillea:1.0)eriachne+gynerium+miscanthus+zea+pennisetum+thysanolae+centropodi+danthonia+merxmuel_r+merxmuel_m+eragrostis+aristida'+austrodant'+arundo+phragmites+molinia+amphipogon+avena'+chusquea'+chasmanthi+joinvillea:1.0)rho+eriachne+gynerium+miscanthus+zea+pennisetum+thysanolae+centropodi+danthonia+merxmuel_r+merxmuel_m+eragrostis+aristida'+austrodant'+arundo+phragmites+molinia+amphipogon+avena'+chusquea'+chasmanthi+joinvillea;";

            // bad-example
//			String s1 = "((a,b),(((d,e),f),((c,g),h)));";
//			String s2 = "(((a,c),((d,(f,(h,g))),e)),b);";

            PhyloTree t1 = new PhyloTree();
            t1.parseBracketNotation(s1, true);

            PhyloTree t2 = new PhyloTree();
            t2.parseBracketNotation(s2, true);

            // EasyTree eT = new EasyTree(t1);

            // Iterator<EasyNode> it = eT.computeSetOfLeaves();
            // while(it.hasNext())
            // System.out.println(it.next().getLabel());
            // it = eT.computeSetOfLeaves();
            // it.next();
            // EasyNode v = it.next();
            // System.out.println("rem label: "+v.getLabel());
            // eT.deleteLeafNode(v);

            // Iterator<EasyNode> it = eT.postOrderWalk();
            // int i = 0;
            // while(it.hasNext()){
            // EasyNode v = it.next();
            // if(v.getOutDegree() != 0){
            // v.setLabel(""+i);
            // i++;
            // }
            // }
            //
            // PhyloTree tA = eT.getPhyloTree();
            //
            // EasyNode s = null;
            // it = eT.postOrderWalk();
            // while(it.hasNext()){
            // EasyNode v = it.next();
            // if(v.getLabel().equals("3")){
            // System.out.println("Inner node: "+v.getLabel());
            // s = v;
            // break;
            // }
            // }
            //
            // EasyTree sub = eT.getSubnetwork(s);
            // PhyloTree tB = sub.getPhyloTree();
            //
            // System.out.println();
            // it = eT.computeSetOfLeaves();
            // while(it.hasNext())
            // System.out.print(it.next().getLabel()+" ");
            // System.out.println();

            // System.out.println(tA+";");
            // System.out.println(tB+";");

            HybridTree n1 = new HybridTree(t1, true, null);
            HybridTree n2 = new HybridTree(t2, true, null);
            n1.update();
            n2.update();

            // Vector<EasyTree> v = new Vector<EasyTree>();
            // v.add(new EasyTree(n2));
            // double kPrime = (new EasyForestApproximation(
            // new Hashtable<String, Vector<String>>(),
            // n1.getTaxaOrdering())).run(new EasyTree(n1), new EasyTree(
            // n2), new EasyTree(n1), v, 2);
            // System.out.println(kPrime);

            // Vector<Vector<Node>> t1Chains = (new FindMaxChains()).run(n1);
            // System.out.println("----");
            // Vector<Vector<Node>> t2Chains = (new FindMaxChains()).run(n2);
            // (new ReplaceMaxCommonChains()).run(n1, t1Chains, n2, t2Chains,
            // new ReplacementInfo());

            (new EasyFastGetAgreementForest()).run(n1, n2, 14, View.Computation.NETWORK, new ReplacementInfo());

//			EasySiblings eS = new EasySiblings();
//			eS.init(new EasyTree(n1), new Vector<EasyTree>());
//			HashSet<Vector<String>> sibs = new HashSet<Vector<String>>();
//			eS.updateSiblings(new EasyTree(n1), new Vector<EasyTree>(), false);
//			System.out.println(eS.getSiblingsOfT1());

            // (new FastGetAgreementForest()).run(n1, n2, 13, 0);
            // Vector<HybridTree> v = new Vector<HybridTree>();
            // v.add(n2);
            // (new ForestApproximation(null)).run(n1, n2, n1, v, 2);

            // HybridNetwork n = (new ComputeClusterNetwork()).run(n1, n2);
            // System.out.println(n);

            // PhyloTree t1 = new PhyloTree();
            // t1.parseBracketNotation("(((((((r,a),b),c),d),e),f),(s,t));",
            // true);
            //
            // PhyloTree t2 = new PhyloTree();
            // t2.parseBracketNotation("((((((((s,t),a),b),c),d),e),f),r);",
            // true);

            // HybridTree n1 = new HybridTree(t1, true, null);
            // HybridTree n2 = new HybridTree(t2, true, null);
            // n1.update();
            // n2.update();
            //
            // System.out.println("n1 chains:");
            // Vector<Vector<Node>> t1Chains = (new FindMaxChains()).run(n1);
            // for (Vector<Node> chain : t1Chains) {
            // System.out.print(" - ");
            // for (Node v : chain)
            // System.out.print(n1.getLabel(v) + "|");
            // }
            // System.out.print("\n");
            //
            // System.out.println("n2 chains:");
            // Vector<Vector<Node>> t2Chains = (new FindMaxChains()).run(n2);
            // for (Vector<Node> chain : t2Chains) {
            // System.out.print(" - ");
            // for (Node v : chain)
            // System.out.print(n2.getLabel(v) + "|");
            // }
            // System.out.print("\n");
            //
            // ReplacementInfo rI = new ReplacementInfo();
            // (new ReplaceMaxCommonChains()).run(n1, t1Chains, n2,
            // t2Chains,rI);
            //
            // System.out.println("n1 getEdgeToWeight: "+n1.getTaxaPairToWeight());
            // System.out.println("n2 getEdgeToWeight: "+n2.getTaxaPairToWeight());
            //
            // System.out.println("getStartLabelToChain: "+rI.getStartLabelToChain());
            // System.out.println("getStartLabelToEndLabel: "+rI.getStartLabelToEndLabel());
            //
            // System.out.println(n1);
            // System.out.println(n2);
            //
            // Vector<HybridNetwork> networks = new Vector<HybridNetwork>();
            // networks.add(n1);
            // networks.add(n2);
            // (new ReattachChains()).run(networks, rI);
            //
            // System.out.println(n1);
            // System.out.println(n2);

            // PhyloTree t1 = new PhyloTree();
            // t1
            // .parseBracketNotation(
            // "(rho:1.0,((11:1.0,12:1.0)11+12:1.0,(18':1.0,13':1.0)18'+13':1.0)11+12+18'+13':1.0)rho+11+12+18'+13';",
            // true);
            //
            // PhyloTree t2 = new PhyloTree();
            // t2
            // .parseBracketNotation(
            // "(rho:1.0,((11:1.0,(12:1.0,18':1.0)12+18':1.0)11+12+18':1.0,13':1.0)11+12+18'+13':1.0)rho+11+12+18'+13';",
            // true);
            //
            // HybridTree n1 = new HybridTree(t1, false, null);
            // HybridTree n2 = new HybridTree(t2, false, null);
            // n1.update();
            // n2.update();
            //
            // String[] s = {"13'", "18'+13'", "rho+11+12+18'+13'" };
            // Vector<Node> vec = new Vector<Node>();
            // for (Node v : n1.getNodes()) {
            // String label = n1.getLabel(v);
            // for (String l : s) {
            // if (l.equals(label))
            // vec.add(v);
            // }
            // }
            //
            // System.out.println((new FastNodeDisjointCheck()).run(n1, vec,
            // n2));
            //
            // HashSet<Vector<HybridTree>> setOfForests = (new
            // GetAgreementForest()).run(n1, n2, 4);
            //
            // System.out.println(setOfForests.size());
            // for(Vector<HybridTree> forest: setOfForests)
            // System.out.println(forest.size());

            double runtime = (System.currentTimeMillis() - time) / 1000;
            System.out.println("Runtime: " + runtime);
            System.out.println("- Test succesful - ");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR: Wrong bracket notation.");
        }
    }
}
