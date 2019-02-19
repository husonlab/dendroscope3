/**
 * EasyIsomorphismCheck.java 
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
package dendroscope.hybrid;

import java.util.*;

/**
 * Given two rooted, bifurcating phylogenetic trees T1 and T2, this function
 * checks whether T1 and T2 are isomorphic.
 *
 * @author Benjamin Albrecht, 6.2010
 */

public class EasyIsomorphismCheck {

    public boolean run(EasyTree n1, EasyTree n2) {

        EasyTree n1Copy = new EasyTree(n1);
        EasyTree n2Copy = new EasyTree(n2);

        if (n1Copy.getNodes().size() != n2Copy.getNodes().size())
            return false;

        while (n1Copy.getNodes().size() > 3) {

            HashSet<String> t1Cherrys = new HashSet<>();
            Hashtable<String, EasyNode> t1Taxa2parent = new Hashtable<>();

            // collect all cherries in t1
            // -> a cherry is a sorted string assembled by its taxon labelings
            getCherrys(n1Copy, t1Cherrys, t1Taxa2parent);

            HashSet<String> t2Cherrys = new HashSet<>();
            Hashtable<String, EasyNode> t2Taxa2parent = new Hashtable<>();

            // collect all cherries in t2
            getCherrys(n2Copy, t2Cherrys, t2Taxa2parent);

            // compare the two cherry sets..
            if (t1Cherrys.size() != t2Cherrys.size())
                return false;

            for (String t2Cherry : t2Cherrys) {
                if (!t1Cherrys.contains(t2Cherry))
                    return false;
            }

            // generate new cherries in both trees
            if (n1Copy.getNodes().size() > 3) {
                replaceCherrys(n1Copy, t1Taxa2parent);
                replaceCherrys(n2Copy, t2Taxa2parent);
            } else
                return true;

        }

        return true;
    }

    private void replaceCherrys(EasyTree t,
                                Hashtable<String, EasyNode> taxa2parent) {
        for (String taxon : taxa2parent.keySet()) {
            EasyNode v = taxa2parent.get(taxon);
            v.setLabel(taxon);
            for (EasyNode c : v.getChildren())
                c.delete();
            for (int i = 0; i < 2; i++)
                t.deleteNode(v.getChildren().get(0));
        }
    }

    private void getCherrys(EasyTree n1Mod, HashSet<String> cherrys,
                            Hashtable<String, EasyNode> taxa2parent) {
        Iterator<EasyNode> it = n1Mod.getLeaves().iterator();
        Vector<EasyNode> parents = new Vector<>();
        while (it.hasNext()) {
            EasyNode v = it.next();
            EasyNode p = v.getParent();
            if (!parents.contains(p) && isCherry(p)) {

                Vector<String> taxa = new Vector<>();

                // collect taxa
                for (EasyNode c : p.getChildren())
                    taxa.add(c.getLabel());

                // sort taxas lexicographically
                Collections.sort(taxa);

                // generate cherry-string
                String taxaString = "";
                for (String s : taxa)
                    taxaString = taxaString.concat(s);
                cherrys.add(taxaString);

                parents.add(p);
                taxa2parent.put(taxaString, p);
            }
        }
    }

    private boolean isCherry(EasyNode p) {
        for (EasyNode c : p.getChildren()) {
            if (c.getOutDegree() != 0)
                return false;
        }
        return true;
    }
}
