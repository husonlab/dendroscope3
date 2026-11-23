/*
 * IntervalsToTree.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.progs;

import dendroscope.algorithms.clusternet.ClusterNetwork;
import dendroscope.consensus.Cluster;
import dendroscope.consensus.Taxa;
import jloda.phylo.PhyloTree;
import jloda.util.NumberUtils;

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
	 */
    public static void main(String[] args) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));

        Taxa taxa = new Taxa();


        System.err.flush();

        System.err.print("Enter name of file containing taxon labels or number of taxa:");
        String input = r.readLine();

		if (NumberUtils.isInteger(input)) {
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
				if (!NumberUtils.isInteger(words[0])) {
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
		ClusterNetwork.constructHasse(taxa, tree, tree.getRoot(), clusters.toArray(new Cluster[0]), null, null, null, taxa.size());
		ClusterNetwork.convertHasseToClusterNetwork(tree, null);

        System.out.println(tree.toBracketString(false) + ";");
    }
}
