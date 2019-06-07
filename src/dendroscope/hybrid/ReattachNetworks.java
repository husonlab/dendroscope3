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
