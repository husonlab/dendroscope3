/**
 * ForestThread.java 
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
package dendroscope.hybrid;

import jloda.graph.Node;

import java.util.*;

public class ForestThread extends Thread {

    private final ExhaustiveSearch eS;
    private final HybridTree t1;
    private final HybridTree t2;
    private final int edgeChoice;
    private final View.Computation compValue;
    private final ReplacementInfo rI;

    private final EasyFastGetAgreementForest fAf = new EasyFastGetAgreementForest();

    private final GetAgreementForest gAf = new GetAgreementForest();
    private final boolean caMode;
    private Vector<String> dummyLabels = new Vector<>();

    private Hashtable<Integer, Vector<BitSet>> indexToIllegalCombi = new Hashtable<>();

    public ForestThread(ExhaustiveSearch eS, HybridTree t1, HybridTree t2,
                        int edgeChoice, int numOfEdges, View.Computation compValue, ReplacementInfo rI, boolean caMode) {
        this.eS = eS;
        this.t1 = t1;
        this.t2 = t2;
        this.edgeChoice = edgeChoice;
        this.compValue = compValue;
        this.rI = rI;
        this.caMode = caMode;
    }

    public void run() {
        try {

            Hashtable<Integer, HashSet<Vector<HybridTree>>> numberToForests;

            if (!caMode)
                numberToForests = fAf.run(t1, t2, edgeChoice, compValue, rI);
            else
                numberToForests = gAf.run(t1, t2, edgeChoice, indexToIllegalCombi, compValue, dummyLabels);

//            printMAAFs(numberToForests);

            eS.reportResult(numberToForests, edgeChoice, this);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void stopThread() {
        fAf.setStop(true);
        gAf.setStopped(true);
    }

    public void setIndexToIllegalCombi(
            Hashtable<Integer, Vector<BitSet>> indexToIllegalCombi) {
        this.indexToIllegalCombi = indexToIllegalCombi;
    }

    public void setDummyLabels(Vector<String> dummyLabels) {
        this.dummyLabels = dummyLabels;
    }

    private void printMAAFs(Hashtable<Integer, HashSet<Vector<HybridTree>>> numberToForests) {
        HashSet<Vector<HybridTree>> forests = numberToForests.get(5);
        if (forests != null) {
            Vector<String> MAAFs = new Vector<>();
            for (Vector<HybridTree> maaf : forests) {
                Vector<String> MAAF = new Vector<>();
                for (HybridTree comp : maaf) {
                    Vector<String> c = new Vector<>();
                    for (Node v : comp.computeSetOfLeaves())
                        c.add(comp.getLabel(v));
                    Collections.sort(c);
                    MAAF.add(c.toString());
                }
                Collections.sort(MAAF);
                MAAFs.add(MAAF.toString());
            }
            Collections.sort(MAAFs);
            for (String s : MAAFs)
                System.out.println(MAAFs.indexOf(s) + ": " + s);
        }
    }

}
