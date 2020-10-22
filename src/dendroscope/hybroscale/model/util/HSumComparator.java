/*
 *   HSumComparator.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.util;

import java.util.Comparator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class HSumComparator implements Comparator<Vector<Integer>> {

    private ConcurrentHashMap<Vector<Integer>, Vector<Integer>> orderingToMaxima;

    public HSumComparator(ConcurrentHashMap<Vector<Integer>, Vector<Integer>> orderingToMaxima) {
        this.orderingToMaxima = orderingToMaxima;
    }

    @Override
    public int compare(Vector<Integer> o1, Vector<Integer> o2) {
        Vector<Integer> maxVec1 = orderingToMaxima.get(o1);
        Vector<Integer> maxVec2 = orderingToMaxima.get(o2);

        for (int i = 0; i < maxVec1.size(); i++) {
            int max1 = maxVec1.get(i);
            int max2 = maxVec2.get(i);
            if (max1 > max2)
                return -1;
            if (max1 < max2)
                return 1;
        }
        return 0;
    }

}
