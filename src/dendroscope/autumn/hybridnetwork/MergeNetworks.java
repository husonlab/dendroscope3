/*
 * MergeNetworks.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.autumn.hybridnetwork;

import dendroscope.autumn.PostProcess;
import dendroscope.autumn.PreProcess;
import dendroscope.autumn.Root;
import dendroscope.consensus.Taxa;
import dendroscope.core.TreeData;
import jloda.graph.Edge;
import jloda.graph.Graph;
import jloda.util.Pair;

import java.io.IOException;
import java.util.*;

/**
 * merge all networks in first list by attaching all networks in second list in all possible ways
 * <p/>
 * Daniel Huson, 5.2011
 */
public class MergeNetworks {
    private static final boolean verbose = false;

    /**
     * cluster reduce two trees, if possible
     *
     * @param tree1
     * @param tree2
     * @return subtree-reduced trees followed by all reduced subtrees
     */
    public static TreeData[] apply(TreeData tree1, TreeData tree2, TreeData[] subTrees) throws IOException {
        // setup rooted trees with nodes labeled by taxa ids
        Taxa allTaxa = new Taxa();
        Pair<Root, Root> roots = PreProcess.apply(tree1, tree2, allTaxa);

        Root root1 = roots.getFirst();
        Root root2 = roots.getSecond();

        List<Root> inputTrees = new LinkedList<Root>();
        inputTrees.add(root1);
        inputTrees.add(root2);

        List<Root> subTreesList = new LinkedList<Root>();
        for (TreeData tree : subTrees) {
            subTreesList.add(PreProcess.apply(tree, allTaxa, true));
        }

        // run the algorithm
        List<Root> results = apply(inputTrees, subTreesList);

        if (results != null && results.size() > 0) {
            List<TreeData> list = PostProcess.apply(results.toArray(new Root[results.size()]), allTaxa, true);
            return list.toArray(new TreeData[list.size()]);
        } else
            return null;
    }


    /**
     * merge all networks in first list by attaching all networks in second list in all possible ways
     *
     * @param list1
     * @param list2
     * @return list of merged networks
     */
    static public List<Root> apply(Collection<Root> list1, Collection<Root> list2) throws IOException {
        List<Root> result = new LinkedList<Root>();

        for (Root root1 : list1) {
            Root newRoot1 = root1;
            for (Root root2 : list2) {
                newRoot1 = merge(newRoot1, root2);
            }
            result.add(newRoot1);

        }
        return result;
    }


    /**
     * merge two networks by adding the second to the leaves of the first where ever taxa match
     *
     * @param root1
     * @param root2
     * @return merged tree
     */
    static public Root merge(Root root1, Root root2) throws IOException {
        if (verbose) {
            System.err.println("Merging networks:");
            System.err.println("Tree1: " + root1.toStringFullTreeX());
            System.err.println("Tree2: " + root2.toStringFullTreeX());
        }

        Root root = copySubNetwork(root1);

        int taxon = root2.getTaxa().nextSetBit(0); // original tree root1 only contains first taxon of reduced part root2

        boolean isBelow = true;
        root.getTaxa().or(root2.getTaxa());
        root.getRemovedTaxa().or(root2.getRemovedTaxa());

        Root rootTar = root;
        while (isBelow) {
            isBelow = false;
            for (Edge e = rootTar.getFirstOutEdge(); !isBelow && e != null; e = rootTar.getNextOutEdge(e)) {
                Root w = (Root) e.getTarget();
                if (w.getTaxa().get(taxon)) {
                    rootTar = w;
                    rootTar.getTaxa().or(root2.getTaxa());
                    rootTar.getRemovedTaxa().or(root2.getRemovedTaxa());
                    isBelow = true;
                }
            }
        }
        if (!rootTar.getTaxa().equals(root2.getTaxa()))
            throw new IOException("Merge: Nodes must have equal taxon sets");
        if (rootTar.getInDegree() != 1) { // rootTar is reticulation node or root, attach to it
            copySubNetwork(root2, rootTar);
        } else {  // Remove rootTar and map root2 to the node above rootTar

            Root tmp = (Root) rootTar.getFirstInEdge().getSource();
            rootTar.deleteNode();
            rootTar = tmp;
            copySubNetwork(root2, rootTar);
        }
        return root;
    }

    /**
     * produce a copy of the sub network rooted at the node
     *
     * @return sub tree or network below
     */
    private static Root copySubNetwork(Root root) {
        Root newRoot = new Root(new Graph());
        newRoot.setTaxa(root.getTaxa());
        newRoot.setRemovedTaxa(root.getRemovedTaxa());
        return copySubNetwork(root, newRoot);
    }

    /**
     * copies the network rooted at rootSrc onto rootTar
     *
     * @return rootTar
     */
    private static Root copySubNetwork(Root rootSrc, Root rootTar) {
        Map<Root, Root> old2new = new HashMap<Root, Root>();
        old2new.put(rootSrc, rootTar);
        copySubNetworkRec(rootSrc, rootTar, old2new);
        return rootTar;
    }

    /**
     * recursively does the work
     *
     * @param v1
     * @param v2
     * @param old2new
     */
    private static void copySubNetworkRec(Root v1, Root v2, Map<Root, Root> old2new) {
        for (Edge e1 = v1.getFirstOutEdge(); e1 != null; e1 = v1.getNextOutEdge(e1)) {
            Root w1 = (Root) e1.getTarget();
            Root w2 = old2new.get(w1);
            if (w2 == null) {
                w2 = v2.newNode();
                w2.setTaxa(w1.getTaxa());
                w2.setRemovedTaxa(w1.getRemovedTaxa());
                old2new.put(w1, w2);
                copySubNetworkRec(w1, w2, old2new);
            }
            Edge f = v2.newEdge(v2, w2);
            f.setInfo(e1.getInfo());
        }
    }
}
