/**
 * AntiClusterSet.java 
 * Copyright (C) 2015 Daniel H. Huson
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
package dendroscope.anticonsensus;

import java.util.Hashtable;
import java.util.Iterator;

public class AntiClusterSet {

    private final Hashtable<String, AntiCluster> antiClusterSet;

    public AntiClusterSet() {
        this.antiClusterSet = new Hashtable<>();
    }

    public void add(AntiCluster cluster) {
        this.antiClusterSet.put(cluster.getConcatenatedTaxa(), cluster);
    }

    public void remove(AntiCluster cluster) {
        this.antiClusterSet.values().remove(cluster);
    }

    public AntiCluster get(AntiCluster c) {
        return this.antiClusterSet.get(c.getConcatenatedTaxa());
    }

    public boolean contains(AntiCluster c) {
        return (this.antiClusterSet.containsKey(c.getConcatenatedTaxa()));
    }

    public Iterator<AntiCluster> getClusters() {
        return this.antiClusterSet.values().iterator();

    }

}
