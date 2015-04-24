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

package dendroscope.drawer;

import jloda.graph.NodeSet;
import jloda.graphview.IGraphDrawer;

/**
 * interface for optimized graph drawer
 * Daniel Huson, 1.2007
 */
public interface IOptimizedGraphDrawer extends IGraphDrawer {
    /**
     * recompute additional data-structures needed for optimization
     *
     * @param nodes if null, recompute all data, otherwise recompute only for given nodes (and all dependent ones)
     */
    void recomputeOptimization(NodeSet nodes);
}
