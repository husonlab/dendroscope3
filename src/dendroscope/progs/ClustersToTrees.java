/**
 * ClustersToTrees.java 
 * Copyright (C) 2018 Daniel H. Huson
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

import dendroscope.consensus.Cluster;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * convert a set of clusters into a list of rooted trees
 * Daniel Huson, 9.2008
 */
public class ClustersToTrees {

    /**
     * read clusters consisting of 1-letter taxa and produces one tree per line
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        System.err.println("Enter clusters of single-letter taxa, enter '.' to finish input");
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));

        List lines = new LinkedList();
        String aLine;
        while ((aLine = r.readLine()) != null) {
            if (aLine.startsWith("."))
                break;
            if (aLine.length() > 0 && !aLine.trim().startsWith("#"))
                lines.add(aLine);
        }

        BitSet letters = new BitSet();

        for (Iterator it = lines.iterator(); it.hasNext(); ) {
            aLine = (String) it.next();
            for (int i = 0; i < aLine.length(); i++) {
                char ch = aLine.charAt(i);
                if (Character.isLetterOrDigit(ch))
                    letters.set((int) ch);
            }
        }

        if (lines.size() < 2)
            throw new IOException("Require at least two lines of input");

        for (Iterator it = lines.iterator(); it.hasNext(); ) {
            aLine = (String) it.next();
            BitSet seen = new BitSet();
            StringBuffer buf = new StringBuffer();
            boolean first = true;
            for (int i = 0; i < aLine.length(); i++) {
                char ch = aLine.charAt(i);
                if (Character.isLetterOrDigit(ch)) {
                    if (!seen.get(ch)) {
                        if (first) {
                            buf.append("(out,((");
                            first = false;
                        } else {
                            buf.append(",");
                        }
                        buf.append(ch);
                        seen.set(ch);
                    }
                }
            }
            if (first == true) {
                System.err.println("Warning: No letters specified in line: " + aLine);
                continue;
            }
            buf.append("),");
            if (seen.cardinality() == 1)  // only one taxon, do this properly
            {
                buf = new StringBuffer();
                buf.append("(out,(").append((char) seen.nextSetBit(0)).append(",");
            }

            if (seen.cardinality() == letters.cardinality() - 1) {
                BitSet diff = Cluster.setminus(letters, seen);
                buf.append((char) diff.nextSetBit(0)).append("));");
            } else {
                first = true;

                for (int c = letters.nextSetBit(0); c != -1; c = letters.nextSetBit(c + 1)) {
                    if (!seen.get(c)) {
                        if (first)
                            first = false;
                        else
                            buf.append(",");
                        buf.append((char) c);
                    }
                }
                if (first == true) {
                    System.err.println("No absent letters in line: " + aLine);
                    continue;
                }
                buf.append("));");
            }
            System.out.println(buf.toString());
        }
    }
}
