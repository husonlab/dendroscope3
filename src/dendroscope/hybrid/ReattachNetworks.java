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
