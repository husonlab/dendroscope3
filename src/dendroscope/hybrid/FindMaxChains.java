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

import jloda.graph.Node;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public class FindMaxChains {

    @SuppressWarnings("unchecked")
    public Vector<Vector<Node>> run(HybridTree t) {

        Vector<Vector<Node>> chains = new Vector<>();

        Hashtable<Node, Vector<Node>> nodeToChain = new Hashtable<>();

        Iterator<Node> it = t.postOrderWalk();
        while (it.hasNext()) {
            Node v = it.next();
            if (v.getOutDegree() == 2 && !t.getRoot().equals(v)) {

                Iterator<Node> it2 = t.getSuccessors(v);
                Node l = it2.next();
                Node r = it2.next();

                if (l.getOutDegree() == 0 && r.getOutDegree() == 0) {
                    Vector<Node> chain = new Vector<>();
                    chain.add(v);
                    nodeToChain.put(v, chain);
                } else if (l.getOutDegree() == 0) {
                    if (nodeToChain.containsKey(r)) {
                        Vector<Node> chain = (Vector<Node>) nodeToChain.get(r).clone();
                        nodeToChain.remove(r);
                        chain.add(v);
                        nodeToChain.put(v, chain);
                    } else {
                        Vector<Node> chain = new Vector<>();
                        chain.add(v);
                        nodeToChain.put(v, chain);
                    }
                } else if (r.getOutDegree() == 0) {
                    if (nodeToChain.containsKey(l)) {
                        Vector<Node> chain = (Vector<Node>) nodeToChain.get(l).clone();
                        nodeToChain.remove(l);
                        chain.add(v);
                        nodeToChain.put(v, chain);
                    } else {
                        Vector<Node> chain = new Vector<>();
                        chain.add(v);
                        nodeToChain.put(v, chain);
                    }
                }
            }
        }

        for (Vector<Node> chain : nodeToChain.values()) {
            if (chain.size() >= 3)
                chains.add(chain);
        }

        return chains;
    }
}
