/*
 *   CountRetNumber.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.model.cmpMinNetworks;

import dendroscope.hybroscale.model.HybridManager.Computation;
import dendroscope.hybroscale.model.treeObjects.HybridNetwork;
import dendroscope.hybroscale.model.treeObjects.SparseNetNode;
import dendroscope.hybroscale.model.treeObjects.SparseNetwork;
import dendroscope.hybroscale.util.graph.MyNode;

public class CountRetNumber {

    public int getNumber(HybridNetwork n, Computation compValue) {
        if (compValue.equals(Computation.EDGE_NETWORK) || compValue.equals(Computation.EDGE_NUMBER))
            return getReticulationNumber(n);
        return getHybridizationNumber(n);
    }

    public int getNumber(SparseNetwork n, Computation compValue) {
        if (compValue.equals(Computation.EDGE_NETWORK) || compValue.equals(Computation.EDGE_NUMBER))
            return getReticulationNumber(n);
        return getHybridizationNumber(n);
    }

    public int getHybridizationNumber(SparseNetwork n) {
        int hNumber = 0;
        for (SparseNetNode v : n.getNodes()) {
            if (v.getInDegree() > 1)
                hNumber++;
        }
        return hNumber;
    }

    public int getHybridizationNumber(HybridNetwork h) {
        int hNumber = 0;
        for (MyNode v : h.getNodes()) {
            if (v.getInDegree() > 1)
                hNumber++;
        }
        return hNumber;
    }

    public int getReticulationNumber(SparseNetwork n) {
        int retNumber = computeWithModeTwo(n);
        return retNumber;
    }

    public int getReticulationNumber(HybridNetwork h) {
        int retNumber = computeWithModeTwo(new SparseNetwork(h));
        return retNumber;
    }

    private int computeWithModeTwo(SparseNetwork n) {
        int counter = 0;
        for (SparseNetNode v : n.getNodes()) {
            if (v.getInDegree() >= 2)
                counter += v.getInDegree() - 1;
        }
        return counter;
    }

}
