/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import java.util.BitSet;
import java.util.Comparator;

public class BitCardComparator implements Comparator<BitSet> {

    public int compare(BitSet b1, BitSet b2) {
        if (b1.cardinality() > b2.cardinality())
            return 1;
        else if (b1.cardinality() < b2.cardinality())
            return -1;
        return 0;
    }

}
