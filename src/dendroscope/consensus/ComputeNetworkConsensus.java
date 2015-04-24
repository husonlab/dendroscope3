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
                    node2taxa.set(v, set);
                    Split split = splits.getTrivial(t);
                    if (split != null) {
                        tree.setWeight(e, split.getWeight());
                        usedSplits.add(split);
                    }
                }
            }
            node2taxa.set(center, centerTaxa);

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
