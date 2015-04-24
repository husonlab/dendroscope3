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

package dendroscope.util;

/**
 * a mutable integer
 * Daniel Huson, 9.2008
 */
public class IntegerVariable {
    private Integer value;

    public IntegerVariable() {
        value = new Integer(0);
    }

    public IntegerVariable(int value) {
        this.value = new Integer(value);
    }

    public int getValue() {
        return value.intValue();
    }

    public int setValue(int n) {
        value = new Integer(n);
        return n;
    }

    public String toString() {
        return value.toString();
    }

    public static IntegerVariable valueOf(String str) {
        return new IntegerVariable(Integer.parseInt(str));
    }

    public int increment() {
        return setValue(getValue() + 1);
    }

    public int decrement() {
        return setValue(getValue() - 1);
    }
}
