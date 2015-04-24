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

package dendroscope.autumn;

import java.util.LinkedList;


/**
 * list of values
 * Daniel Huson, 7.2011
 */
public class ValuesList extends LinkedList<Value> {
    /**
     * make a copy that also contains the given value
     *
     * @param value
     * @return copy with value added
     */
    public ValuesList copyWithAdditionalElement(Value value) {
        ValuesList copy = new ValuesList();
        copy.addAll(this);
        copy.add(value);
        return copy;
    }

    /**
     * gets the total value
     *
     * @return total
     */
    public int sum() {
        int total = 0;
        for (Value value : this) {
            total += value.get();
        }
        return total;
    }
}
