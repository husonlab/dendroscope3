/*
 *   Multiset.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.multnet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Multiset extends ArrayList {

    private String concatenatedElements;

    public Multiset() {
        super();
        this.concatenatedElements = null;
    }

    public void setConcatenatedElements(String el) {
        this.concatenatedElements = el;
    }

    public void setConcatenatedElements() {
        String concatenatedElements = "";
        for (Multiset tmpSet : (Iterable<Multiset>) this) {
            concatenatedElements += tmpSet.getConcatenatedElements() + tmpSet.size();
        }
        this.concatenatedElements = concatenatedElements;
    }

    public String getConcatenatedElements() {
        return this.concatenatedElements;
    }

    public void addSorted(Multiset toAdd) {
		Comparator<Multiset> comparator = (m1, m2) -> {
			String concatenatedElements1 = m1.getConcatenatedElements();
			String concatenatedElements2 = m2.getConcatenatedElements();
			return concatenatedElements1.compareTo(concatenatedElements2);
		};
		int insertionPoint = Collections.binarySearch(this, toAdd, comparator);
		if (insertionPoint < 0) insertionPoint = -1 * insertionPoint - 1;
		this.add(insertionPoint, toAdd);
	}
}
