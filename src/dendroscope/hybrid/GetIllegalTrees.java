/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import jloda.graph.Edge;
import jloda.graph.Node;

import java.util.*;

public class GetIllegalTrees {

    private int proc;

    private boolean stop = false;
    private final Hashtable<Integer, Vector<BitSet>> indexToIllegalCombi = new Hashtable<>();
    private final Hashtable<Integer, Vector<BitSet>> indexToNeedlessCombi = new Hashtable<>();

    public Hashtable<Integer, Vector<BitSet>> run(HybridTree t, View view,
                                                  ClusterThread cT) {

        // storing all edges sorted by its heights in the tree from the leaves
        // up to the root
        // -> necessary for having the right order when constructing a forest
        // out of an edge combination
        // -> necessary for having the right order when re-attaching each tree
        // of a forest to a resolved network
        Vector<Edge> sortedEdges = new Vector<>();
        Iterator<Node> itNode = t.postOrderWalk();
        while (itNode.hasNext()) {
            Node v = itNode.next();
            if (v.getInDegree() == 1) {
                Edge e = v.getFirstInEdge();
                if (!e.getSource().equals(t.getRoot()))
                    sortedEdges.add(e);
            }
        }

        // System.out.println("computeIllegalEdgeCombinations - Start");

        double k = 0;
        double size = t.getNumberOfNodes();

        if (view != null)
            view.setStatus(cT, "Initializing - Stage 1");

        Iterator<Node> it = t.postOrderWalk();
        while (it.hasNext()) {

            if (stop)
                break;

            Node v = it.next();
            k++;
            proc = (int) Math.round((k / size) * 100.0);

            if (view != null)
                view.setProgress(cT, proc);

            if (v.getInDegree() == 1 && v.getOutDegree() == 2) {

                int index1 = sortedEdges.indexOf(v.getFirstInEdge());

                if (index1 != -1) {
                    Iterator<Edge> out = v.outEdges().iterator();
                    int index2 = sortedEdges.indexOf(out.next());
                    int index3 = sortedEdges.indexOf(out.next());

                    BitSet b1 = new BitSet(sortedEdges.size());
                    b1.set(index2);
                    b1.set(index3);
                    addIllegalCombi(index1, b1);

                    BitSet b2 = new BitSet(sortedEdges.size());
                    b2.set(index1);
                    b2.set(index3);
                    addIllegalCombi(index2, b2);

                    BitSet b3 = new BitSet(sortedEdges.size());
                    b3.set(index1);
                    b3.set(index2);
                    addIllegalCombi(index3, b3);

                    if (indexToIllegalCombi.containsKey(index3)) {
                        for (BitSet b : indexToIllegalCombi.get(index3)) {
                            BitSet newB = (BitSet) b.clone();
                            newB.set(index2);
                            addIllegalCombi(index1, newB);
                        }
                    }
                    if (indexToIllegalCombi.containsKey(index2)) {
                        for (BitSet b : indexToIllegalCombi.get(index2)) {
                            BitSet newB = (BitSet) b.clone();
                            newB.set(index3);
                            addIllegalCombi(index1, newB);
                        }
                    }
                    if (indexToIllegalCombi.containsKey(index2)
                            && indexToIllegalCombi.containsKey(index3)) {
                        for (BitSet bL : indexToIllegalCombi.get(index2)) {
                            for (BitSet bR : indexToIllegalCombi.get(index3)) {
                                BitSet newB = (BitSet) bL.clone();
                                newB.or(bR);
                                addIllegalCombi(index1, newB);

                                if (stop)
                                    break;

                            }

                            if (stop)
                                break;

                        }
                    }
                }
            }
        }

        if (view != null)
            view.setStatus(cT, "Initializing - Stage 2");

        k = 0;
        it = t.postOrderWalk();
        while (it.hasNext()) {

            if (stop)
                break;

            Node v = it.next();
            k++;
            proc = (int) Math.round((k / size) * 100.0);
            if (view != null)
                view.setProgress(cT, proc);

            if (v.getInDegree() == 1 && v.getOutDegree() == 2) {

                int index1 = sortedEdges.indexOf(v.getFirstInEdge());

                if (index1 != -1) {
                    Iterator<Edge> out = v.outEdges().iterator();
                    int index2 = sortedEdges.indexOf(out.next());
                    int index3 = sortedEdges.indexOf(out.next());

                    if (indexToIllegalCombi.containsKey(index3)) {
                        for (BitSet b : indexToIllegalCombi.get(index3)) {
                            BitSet newB = (BitSet) b.clone();
                            addNeedlessCombi(index1, newB);
                        }
                    }
                    if (indexToIllegalCombi.containsKey(index2)) {
                        for (BitSet b : indexToIllegalCombi.get(index2)) {
                            BitSet newB = (BitSet) b.clone();
                            addNeedlessCombi(index1, newB);
                        }
                    }
                }

            }

        }

        if (view != null)
            view.setStatus(cT, "Initializing - Stage 3");

        k = 0;
        it = t.postOrderWalk();
        while (it.hasNext()) {

            if (stop)
                break;

            Node v = it.next();
            k++;
            proc = (int) Math.round((k / size) * 100.0);
            if (view != null)
                view.setProgress(cT, proc);

            if (v.getInDegree() == 1 && v.getOutDegree() == 2) {

                int index1 = sortedEdges.indexOf(v.getFirstInEdge());

                if (index1 != -1) {
                    Iterator<Edge> out = v.outEdges().iterator();
                    int index2 = sortedEdges.indexOf(out.next());
                    int index3 = sortedEdges.indexOf(out.next());

                    BitSet b0 = new BitSet(sortedEdges.size());
                    b0.set(index2);
                    addNeedlessCombi(index1, b0);

                    BitSet b1 = new BitSet(sortedEdges.size());
                    b1.set(index3);
                    addNeedlessCombi(index1, b1);

                    BitSet b2 = new BitSet(sortedEdges.size());
                    b2.set(index1);
                    addNeedlessCombi(index2, b2);

                    BitSet b3 = new BitSet(sortedEdges.size());
                    b3.set(index1);
                    addNeedlessCombi(index3, b3);
                }

            }

        }

        for (Integer index : indexToNeedlessCombi.keySet()) {
            for (BitSet b : indexToNeedlessCombi.get(index))
                addIllegalCombi(index, b);
        }

        for (Vector<BitSet> v : indexToIllegalCombi.values())
            Collections.sort(v, new BitComparator());

        return indexToIllegalCombi;
    }

    @SuppressWarnings("unchecked")
    private void addNeedlessCombi(int index, BitSet b) {
        if (indexToNeedlessCombi.containsKey(index)) {
            Vector<BitSet> v = (Vector<BitSet>) indexToNeedlessCombi.get(index)
                    .clone();
            indexToNeedlessCombi.remove(index);
            v.add(b);
            indexToNeedlessCombi.put(index, v);
        } else {
            Vector<BitSet> v = new Vector<>();
            v.add(b);
            indexToNeedlessCombi.put(index, v);
        }
    }

    @SuppressWarnings("unchecked")
    private void addIllegalCombi(int index, BitSet b) {
        if (indexToIllegalCombi.containsKey(index)) {
            Vector<BitSet> v = (Vector<BitSet>) indexToIllegalCombi.get(index)
                    .clone();
            indexToIllegalCombi.remove(index);
            v.add(b);
            indexToIllegalCombi.put(index, v);
        } else {
            Vector<BitSet> v = new Vector<>();
            v.add(b);
            indexToIllegalCombi.put(index, v);
        }
    }

    public void stop() {
        stop = true;
    }
}
