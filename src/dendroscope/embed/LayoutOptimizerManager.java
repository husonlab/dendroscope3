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
    public static final String ALGORITHMlsa = "AlgorithmLSA";

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
        else if (algorithmName.equalsIgnoreCase(ALGORITHMlsa))
            embedder = new LayoutOptimizerLSA();
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
        return new String[]{UNOPTIMIZED, ALGORITHM2008, ALGORITHM2009, ALGORITHM2010, ALGORITHM2010DIST, ALGORITHMlsa};

    }
}

