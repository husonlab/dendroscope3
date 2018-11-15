/**
 * BitComparator.java 
 * Copyright (C) 2018 Daniel H. Huson
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

import java.util.BitSet;
import java.util.Comparator;

public class BitComparator implements Comparator<BitSet> {

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
