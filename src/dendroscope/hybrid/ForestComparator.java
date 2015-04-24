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
