/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import jloda.graph.Edge;
import jloda.graph.Node;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * This method replaces distinct leaves of a resolved network by other resolved
 * networks producing a set of new resolved networks.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class ReattachClustersRec {

    private int numOfNets;
    private final Vector<HybridNetwork> networks = new Vector<>();
    private boolean stop = false;
    private View view;

    public Vector<HybridNetwork> run(HybridNetwork n, ReplacementInfo rI, TreeMarker tM, int numOfNets, View view) {

        this.numOfNets = numOfNets;
        this.view = view;

        //adding edges of a cluster network for special treatment
        if (rI.isClusterNetwork(n)) {
            Iterator<Edge> it = n.edgeIterator();
            while (it.hasNext())
                tM.addClusterNetworkEdge(it.next());
        }

        // generating several networks by adding each combination of all
        // networks representing a minimal common cluster replaced before
        // REMARK: a taxon can replace more than one network if several MAAFs
        // exist representing a minimal cluster
        generateNetworksRec(n, rI, tM);

        return networks;
    }

    private void generateNetworksRec(HybridNetwork n, ReplacementInfo rI, TreeMarker tM) {
        Iterator<Node> it = n.computeSetOfLeaves().iterator();

        boolean hasReplacmentLabel = false;
        while (it.hasNext()) {

            if (stop)
                break;

            Node leaf = it.next();
            BitSet b = n.getNodeToCluster().get(leaf);
            String label = n.getLabel(leaf);

            // checking if leaf replaces a minimal common clusters
            if (rI.getReplacementLabels().contains(label)) {

                hasReplacmentLabel = true;

                // getting all networks representing a minimal common
                // cluster
                Vector<HybridNetwork> clusterNetworks = rI
                        .getLabelToNetworks().get(label);

                // every so far computed network gets attached by all
                // different networks representing the minimal cluster
                // producing a bigger set of networks
                for (HybridNetwork cN : clusterNetworks) {

                    if (stop)
                        break;

                    HybridNetwork newN = new HybridNetwork(n, false, null);
                    newN.update();

                    tM.assignEdges(n, newN);

                    // attach network representing the minimal cluster
                    reattachNetwork(newN, cN, newN.getClusterToNode()
                            .get(b), tM, rI.isClusterNetwork(cN));

                    generateNetworksRec(newN, rI, tM);
                }

                break;
            }
        }

        //all cluster replaced
        if (!hasReplacmentLabel) {

            // re-attaching all max common chains
            //System.out.println("ReattachChains");
            (new ReattachChains()).run(n, rI, tM);

            // re-attaching all common subtrees...
            //System.out.println("ReattachSubtrees");
            (new ReattachSubtrees()).run(n, rI);

            rI.addLeafLabels(n);
            n.removeOutgroup();
            n.clearLabelings();

            networks.add(n);

            if (view != null)
                view.setInfo(networks.size() + " / " + numOfNets + "\n hybridization networks computed");
        }
    }

    private void reattachNetwork(HybridNetwork n, HybridNetwork toCopy, Node leaf, TreeMarker tM, boolean isClusterNetwork) {

        // attach root of the attached network
        Node vCopy = n.newNode(toCopy.getRoot());
        n.setLabel(vCopy, toCopy.getLabel(toCopy.getRoot()));

        // attaching the generated root
        for (Edge e : leaf.inEdges()) {
            boolean isSpecial = n.isSpecial(e);
            Node parent = e.getSource();
            n.deleteEdge(e);
            Edge eCopy = n.newEdge(parent, vCopy);

            if (isClusterNetwork)
                tM.addClusterNetworkEdge(eCopy);

            if (isSpecial) {
                n.setSpecial(eCopy, true);
                n.setWeight(eCopy, 0);

                if (tM.contains(e))
                    tM.insertT1Edge(eCopy);

            }
        }

        // adding attached network
        addNetworkToNetworkRec(vCopy, toCopy.getRoot(), toCopy, n,
                new Hashtable<Node, Node>(), tM, isClusterNetwork);

        //delete leaf (leaf is now replaced by a resolved network)
        n.deleteNode(leaf);

        n.initTaxaOrdering();
        n.update();

    }

    // simple recusion that adds all nodes and edges of a resolved network

    private void addNetworkToNetworkRec(Node vCopy, Node v,
                                        HybridNetwork toCopy, HybridNetwork n, Hashtable<Node, Node> created, TreeMarker tM, boolean isClusterNetwork) {
        for (Edge e : v.outEdges()) {
            Node c = e.getTarget();
            Node cCopy;
            if (!created.containsKey(c)) {
                cCopy = n.newNode(c);
                n.setLabel(cCopy, toCopy.getLabel(c));
                created.put(c, cCopy);
                Edge eCopy = n.newEdge(vCopy, cCopy);

                if (isClusterNetwork)
                    tM.addClusterNetworkEdge(eCopy);

                if (toCopy.isSpecial(e)) {
                    n.setSpecial(eCopy, true);
                    n.setWeight(eCopy, 0);

                    if (tM.contains(e))
                        tM.insertT1Edge(eCopy);

                }
                addNetworkToNetworkRec(cCopy, c, toCopy, n, created, tM, isClusterNetwork);
            } else {
                cCopy = created.get(c);
                Edge eCopy = n.newEdge(vCopy, cCopy);

                if (isClusterNetwork)
                    tM.addClusterNetworkEdge(eCopy);

                if (toCopy.isSpecial(e)) {
                    n.setSpecial(eCopy, true);
                    n.setWeight(eCopy, 0);

                    if (tM.contains(e))
                        tM.insertT1Edge(eCopy);

                }
            }
        }
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public Vector<HybridNetwork> getNetworks() {
        return networks;
    }


}
