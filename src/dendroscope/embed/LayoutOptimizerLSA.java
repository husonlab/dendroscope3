/*
 *   LayoutOptimizerLSA.java Copyright (C) 2020 Daniel H. Huson
 *
 *   (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dendroscope.embed;


import jloda.phylo.PhyloTree;
import jloda.util.ProgressListener;

/**
 * computes the drawing for a given order, that already respects the LSA, suprise!: we dont have to do anything!
 * <p/>
 * Franziska Zickmann, 8.2010
 */

public class LayoutOptimizerLSA implements ILayoutOptimizer {

    public void apply(PhyloTree tree, ProgressListener progressListener) {


    }
}


