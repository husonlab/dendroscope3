/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import java.util.BitSet;
import java.util.Comparator;

public class TaxaComparator implements Comparator<BitSet> {

    // todo: are you sure that you only want to compare the first element? If so, give the class a more descriptive name!

    public int compare(BitSet b1, BitSet b2) {
        if (b1.nextSetBit(0) > b2.nextSetBit(0))
            return 1;
        else if (b2.nextSetBit(0) > b1.nextSetBit(0))
            return -1;
        else
            return 0;
    }

}
