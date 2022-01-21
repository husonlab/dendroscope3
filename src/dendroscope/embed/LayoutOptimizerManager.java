/*
 * LayoutOptimizerManager.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.embed;

import jloda.phylo.PhyloTree;
import jloda.util.CanceledException;

/**
 * manages the embedding algorithms
 * Daniel Huson, 7.2010
 */
public class LayoutOptimizerManager {
    public static final String UNOPTIMIZED = "Unoptimized";
    public static final String ALGORITHM2008 = "Algorithm2008";
    public static final String ALGORITHM2009 = "Algorithm2009";
    public static final String ALGORITHM2010 = "Algorithm2010";
    public static final String ALGORITHM2010DIST = "Algorithm2010Dist";

    /**
     * applies the indicated embedding algorithm
     *
     * @param algorithmName
     * @param tree
     */
    public static void apply(String algorithmName, PhyloTree tree) throws CanceledException {
        ILayoutOptimizer embedder;

        if (algorithmName.equalsIgnoreCase(ALGORITHM2008))
            embedder = new LayoutOptimizer2008();
        else if (algorithmName.equalsIgnoreCase(ALGORITHM2009))
            embedder = new LayoutOptimizer2009();
        else if (algorithmName.equalsIgnoreCase(ALGORITHM2010))
            embedder = new EmbeddingOptimizerNNet();
        else if (algorithmName.equalsIgnoreCase(ALGORITHM2010DIST))
            embedder = new LayoutOptimizerDist();
        else
            embedder = new LayoutUnoptimized();
        embedder.apply(tree, null);
    }

    /**
     * gets all recognized embedding algorithm names
     *
     * @return names
     */
    public static String[] getEmbedderNames() {
        return new String[]{UNOPTIMIZED, ALGORITHM2008, ALGORITHM2009, ALGORITHM2010, ALGORITHM2010DIST};

    }
}

