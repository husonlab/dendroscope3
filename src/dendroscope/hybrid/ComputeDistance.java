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
