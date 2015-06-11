/**
 * FindMaxChains.java 
 * Copyright (C) 2015 Daniel H. Huson
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
