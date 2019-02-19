/**
 * HybridManager.java 
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

import dendroscope.core.TreeData;
import jloda.phylo.PhyloTree;

import java.util.Vector;

/**
 * Manages the computation of hybrid networks.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class HybridManager {

    private final Vector<TreeData> treeData = new Vector<>();
    private final Hybrid hybrid;

    public HybridManager(PhyloTree[] trees, View view, Controller c, View.Computation compValue, boolean caMode) {
        hybrid = new Hybrid(trees, view, c, compValue, caMode);
    }

    /**
     * computes a hybrid network gram using the named method
     *
     * @return Hybrid
     */
    public void computeHybrid() {
        Vector<HybridNetwork> networks = hybrid.getNetworks();
        if (networks.size() > 0) {
            System.out.println("Networks: " + networks.size());
            for (HybridNetwork n : networks)
                treeData.add(new TreeData(n));
        }
    }

    public TreeData[] getTreeData() {
        return treeData.toArray(new TreeData[treeData.size()]);
    }

    public Hybrid getHybrid() {
        return hybrid;
    }

}
