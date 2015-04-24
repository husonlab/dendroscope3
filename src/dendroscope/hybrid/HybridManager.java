/**
 * Copyright 2015, Daniel Huson
 *
 *(Some files contain contributions from other authors, who are then mentioned separately)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
