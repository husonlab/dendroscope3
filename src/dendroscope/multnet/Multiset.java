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
        Comparator<Multiset> comparator = new Comparator<Multiset>() {
            public int compare(Multiset m1, Multiset m2) {
                String concatenatedElements1 = m1.getConcatenatedElements();
                String concatenatedElements2 = m2.getConcatenatedElements();
                return concatenatedElements1.compareTo(concatenatedElements2);
            }
        };
        int insertionPoint = Collections.binarySearch(this, toAdd, comparator);
        if (insertionPoint < 0) insertionPoint = -1 * insertionPoint - 1;
        this.add(insertionPoint, toAdd);
    }
}
