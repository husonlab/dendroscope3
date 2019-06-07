/*
 * Copyright (C) This is third party code.
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
