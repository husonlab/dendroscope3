/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import java.util.BitSet;
import java.util.Comparator;

public class FirstBitComparator implements Comparator<BitSet> {
    public int compare(BitSet b1, BitSet b2) {
        if (b1.nextSetBit(0) < b2.nextSetBit(0))
            return 1;
        else if (b1.nextSetBit(0) > b2.nextSetBit(0))
            return -1;
        return 0;
    }

}
