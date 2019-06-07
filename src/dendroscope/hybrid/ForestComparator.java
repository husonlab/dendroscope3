/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import java.util.Comparator;
import java.util.Hashtable;

public class ForestComparator implements Comparator<EasyTree> {

    final Hashtable<EasyTree, Integer> componentToDepth;

    public ForestComparator(Hashtable<EasyTree, Integer> componentToDepth) {
        this.componentToDepth = componentToDepth;
    }

    public int compare(EasyTree f1, EasyTree f2) {
        int d1 = componentToDepth.get(f1);
        int d2 = componentToDepth.get(f2);

        // System.out.println(((PhyloGraph) v1.getOwner()).getLabel(v1)+" "+d1);
        // System.out.println(((PhyloGraph) v2.getOwner()).getLabel(v2)+" "+d2);

        if (d1 < d2)
            return 1;
        else if (d1 > d2)
            return -1;
        return 0;
    }

}
