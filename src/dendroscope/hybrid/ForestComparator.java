/**
 * ForestComparator.java 
 * Copyright (C) 2015 Daniel H. Huson
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
