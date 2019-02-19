/**
 * ComputeDistance.java 
 * Copyright (C) 2019 Daniel H. Huson
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
