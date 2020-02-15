/*
 *   ReattachNetworks.java Copyright (C) 2020 Daniel H. Huson
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

import java.util.Vector;

public class ReattachNetworks extends Thread {

    private final View view;

    private final HybridNetwork n;
    private final ReplacementInfo rI;
    private final TreeMarker tM;
    private final ReattachClustersRec rC = new ReattachClustersRec();
    private final int numOfNets;

    public ReattachNetworks(HybridNetwork n, ReplacementInfo rI,
                            TreeMarker tM, int numOfNets, View view) {
        this.n = n;
        this.rI = rI;
        this.tM = tM;
        this.numOfNets = numOfNets;
        this.view = view;
    }

    @Override
    public void run() {
        rC.run(n, rI, tM, numOfNets, view);
    }

    public Vector<HybridNetwork> getNetworks() {
        return rC.getNetworks();
    }

    public void stopThread() {
        rC.setStop(true);
        interrupt();
    }

}
