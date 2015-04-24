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

package dendroscope.autumn.hybridnetwork;

import dendroscope.autumn.Root;
import jloda.graph.Edge;

/**
 * removes a taxon from a tree.
 * Daniel Huson, 5.2011
 */
public class RemoveTaxon {
    /**
     * remove the named taxon. Children are kept lexicographically sorted
     *
     * @param root
     * @param treeId
     * @param taxon  @return true, if changed
     */
    public static boolean apply(Root root, int treeId, int taxon) {
        if (root.getTaxa().get(taxon)) {
            root.getTaxa().set(taxon, false);
            root.getRemovedTaxa().set(taxon, true);
            boolean changed = false;
            for (Edge e = root.getFirstOutEdge(); e != null; e = root.getNextOutEdge(e)) {
                if (apply((Root) e.getTarget(), treeId, taxon))
                    changed = true;
            }
            if (changed)
                root.reorderChildren();
            if (root.getOutDegree() == 0 && root.getInDegree() > 0) {
                Edge f = root.getFirstInEdge();
                f.setInfo(treeId);
            }
            return true;
        }
        return false;
    }

    /**
     * un-remove the named taxon. Children are kept lexicographically sorted
     *
     * @param root
     * @param taxon @return true, if changed
     */
    public static boolean unapply(Root root, int taxon) {
        if (root.getRemovedTaxa().get(taxon)) {
            root.getRemovedTaxa().set(taxon, false);
            root.getTaxa().set(taxon, true);
            boolean changed = false;
            boolean isBelow = false;
            for (Edge e = root.getFirstOutEdge(); !isBelow && e != null; e = root.getNextOutEdge(e)) {
                Root w = (Root) e.getTarget();
                if (w.getTaxa().get(taxon) || w.getRemovedTaxa().get(taxon))
                    isBelow = true;
            }
            if (root.getDegree() > 0 && !isBelow) {
                Root u = root.newNode();
                u.getTaxa().set(taxon);
                root.newEdge(root, u);
                changed = true;
            } else // is nothing below, add leaf node
            {
                for (Edge e = root.getFirstOutEdge(); e != null; e = root.getNextOutEdge(e)) {
                    if (unapply((Root) e.getTarget(), taxon))
                        changed = true;
                }
            }
            if (changed)
                root.reorderChildren();
            return true;
        }
        return false;
    }

}
