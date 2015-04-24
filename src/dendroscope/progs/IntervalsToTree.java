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

package dendroscope.progs;

import dendroscope.algorithms.clusternet.ClusterNetwork;
import dendroscope.consensus.Cluster;
import dendroscope.consensus.Taxa;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

/**
 * convert a set of intervals to a tree
 * Daniel Huson, 9.2008
 */
public class IntervalsToTree {

    /**
     * read clusters consisting of 1-letter taxa and produces one tree per line
     *
     * @param args
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));

        Taxa taxa = new Taxa();


        System.err.flush();

        System.err.print("Enter name of file containing taxon labels or number of taxa:");
        String input = r.readLine();

        if (Basic.isInteger(input)) {
            int nTaxa = Integer.parseInt(input);
            for (int i = 1; i <= nTaxa; i++) {
                taxa.add("t" + i);
            }
        } else {
            BufferedReader reader = new BufferedReader(new FileReader(input));
            String aLine;
            int t = 0;
            while ((aLine = reader.readLine()) != null) {
                taxa.add(aLine.trim() + "[" + (++t) + "]");
            }
        }
        System.err.println("Number of taxa: " + taxa.size());

        Set<Cluster> clusters = new HashSet<Cluster>();

        for (int i = 1; i <= taxa.size(); i++) {
            BitSet bits = new BitSet();
            bits.set(i);
            clusters.add(new Cluster(bits));
        }

        System.err.println("Enter intervals as a b, or filename of entries, enter '.' to finish input");

        String aLine;
        boolean first = true;
        while ((aLine = r.readLine()) != null) {
            if (aLine.startsWith("."))
                break;
            String[] words = aLine.split(" ");
            if (first) {
                first = false;
                if (!Basic.isInteger(words[0])) {
                    System.err.println("Reading input from file: " + aLine);
                    r = new BufferedReader(new FileReader(aLine));
                    continue;
                }
            }
            if (words.length != 2) {
                System.err.println("Skipping: " + aLine);
            } else {
                BitSet bits = new BitSet();
                int bot = Integer.parseInt(words[0]);
                int top = Integer.parseInt(words[1]);
                if (bot > top) {
                    int tmp = top;
                    top = bot;
                    bot = tmp;
                }
                for (int i = bot; i <= top; i++) {
                    bits.set(i);
                }
                clusters.add(new Cluster(bits));
            }
        }

        PhyloTree tree = new PhyloTree();
        tree.setRoot(tree.newNode());
        ClusterNetwork.constructHasse(taxa, tree, tree.getRoot(), clusters.toArray(new Cluster[clusters.size()]), null, null, null, taxa.size());
        ClusterNetwork.convertHasseToClusterNetwork(tree, null, null);

        System.out.println(tree.toBracketString(false) + ";");
    }
}
