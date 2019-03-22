/**
 * ComputeNetworkConsensus.java 
 * Copyright (C) 2019 Daniel H. Huson
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
package dendroscope.consensus;

import dendroscope.algorithms.clusternet.ClusterNetwork;
import dendroscope.algorithms.gallnet.ComputeGalledNetwork;
import dendroscope.algorithms.levelknet.LevelKNetwork;
import dendroscope.core.Document;
import dendroscope.core.TreeData;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import jloda.util.CanceledException;

import java.util.*;

/**
 * compute a network consensus, if splits incompatible, or tree, otherwise
 * Daniel Huson, 5.2011
 */
public class ComputeNetworkConsensus implements IConsensusTreeMethod {
    public static final String LEVEL_K_NETWORK = "level-k-network";
    public static final String CLUSTER_NETWORK = "cluster-network";
    public static final String GALLED_NETWORK = "galled-network";

    private boolean computeOnlyOne;
    private boolean checkTrees;
    private String optionMethod;
    private double optionPercentThreshold;

    /**
     * constructor
     *
     * @param optionMethod
     * @param optionPercentThreshold
     */
    public ComputeNetworkConsensus(String optionMethod, double optionPercentThreshold) {
        this.optionMethod = optionMethod;
        this.optionPercentThreshold = optionPercentThreshold;
        this.computeOnlyOne = false;
        this.checkTrees = false;
    }

    /**
     * applies the consensus method to obtain a tree or network
     *
     * @return consensus
     */
    public PhyloTree apply(Document doc, TreeData[] trees) throws CanceledException {
        List<PhyloTree> result = applyAll(doc, trees);
        if (result.size() == 0)
            return new PhyloTree();
        else
            return result.get(0);
    }

    /**
     * applies the consensus method to obtain all trees or networks
     *
     * @return consensus
     */
    public List<PhyloTree> applyAll(Document doc, TreeData[] trees) throws CanceledException {
        doc.notifyTasks("Consensus " + optionMethod, "");
        ZClosure zclosure = new ZClosure();
        zclosure.setOptionFilter(ZClosure.FILTER_PERCENT_THRESHOLD);
        zclosure.setOptionPercentThreshold(optionPercentThreshold);
        System.err.println("Consensus " + optionMethod + " input trees:" + trees.length);
        SplitSystem splits = zclosure.apply(doc.getProgressListener(), trees);
        Taxa taxa = zclosure.getTaxa();
        System.err.println("Consensus " + optionMethod + " splits: " + splits.size());

        PhyloTree tree = null;
        List<PhyloTree> networks = new LinkedList<>();

        if (taxa.size() == 0)
            return new LinkedList<>();
        if (splits.containsPairWithAllFourIntersections()) {
            System.err.println("not compatible");
            switch (optionMethod) {
                case LEVEL_K_NETWORK: {
                    LevelKNetwork network = new LevelKNetwork(taxa, splits);
                    network.setComputeOnlyOne(computeOnlyOne);
                    network.setCheckTrees(checkTrees);
                    networks.addAll(network.apply(doc.getProgressListener()));
                    for (PhyloTree a : networks)
                        a.setName(LEVEL_K_NETWORK);
                    break;
                }
                case GALLED_NETWORK: {
                    ComputeGalledNetwork network = new ComputeGalledNetwork(taxa, splits);
                    tree = network.apply(doc.getProgressListener());
                    tree.setName(GALLED_NETWORK);
                    break;
                }
                default: {
                    ClusterNetwork network = new ClusterNetwork(taxa, splits);
                    tree = network.apply();
                    tree.setName(CLUSTER_NETWORK);
                    break;
                }
            }
        } else {
            tree = new PhyloTree();
            NodeArray<BitSet> node2taxa = new NodeArray<>(tree); // map each node to the taxa beyond it

            Set<Split> usedSplits = new HashSet<>();
            Node center = tree.newNode();
            tree.setRoot(center);
            BitSet centerTaxa = new BitSet();

            // setup leaf nodes
            for (Iterator it = taxa.iterator(); it.hasNext(); ) {
                String name = (String) it.next();
                int t = taxa.indexOf(name);
                if (t != taxa.maxId()) {  // this condition drops the __outgroup__
                    Node v = tree.newNode();
                    Edge e = tree.newEdge(center, v);
                    centerTaxa.set(t);
                    tree.setLabel(v, name);
                    BitSet set = new BitSet();
                    set.set(t);
                    node2taxa.put(v, set);
                    Split split = splits.getTrivial(t);
                    if (split != null) {
                        tree.setWeight(e, split.getWeight());
                        usedSplits.add(split);
                    }
                }
            }
            node2taxa.put(center, centerTaxa);

            // process all non-trivial splits
            for (Iterator it = splits.iterator(); it.hasNext(); ) {
                Split split = (Split) it.next();
                if (!usedSplits.contains(split)) {
                    if (split.getSplitSize() == 1)
                        System.err.println("problem: " + split);
                    else
                        splits.processSplit(center, node2taxa, taxa.maxId(), split, tree);
                }
            }
            tree.setName("tree");
        }
        if (tree != null)
            networks.add(tree);
        return networks;
    }

    public String getOptionMethod() {
        return optionMethod;
    }

    public void setOptionMethod(String optionMethod) {
        this.optionMethod = optionMethod;
    }

    public double getOptionPercentThreshold() {
        return optionPercentThreshold;
    }

    public void setOptionPercentThreshold(double optionPercentThreshold) {
        this.optionPercentThreshold = optionPercentThreshold;
    }

    public void setComputeOnlyOne(boolean onlyOne) {
        computeOnlyOne = onlyOne;
    }

    public void setCheckTrees(boolean ct) {
        checkTrees = ct;
    }
}
