/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import java.util.HashSet;
import java.util.Vector;

public class ComputeDistance {

    public int run(HashSet<Vector<HybridTree>> MAFs, ReplacementInfo rI) {

        int distance = 0;

        for (Vector<HybridTree> MAF : MAFs) {
            if (MAF.size() != 0)
                distance += MAF.size() - 1;
            for (HybridNetwork component : MAF) {
                if (component.getNumberOfNodes() == 1) {
                    String label = component.getNodeLabels().iterator().next();
                    if (rI.getPrunedLabels().contains(label))
                        distance -= 1;
                }

            }
        }

        return distance;

    }
}
