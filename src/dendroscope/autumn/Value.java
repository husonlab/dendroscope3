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

import jloda.util.Single;

/**
 * single mutable integer value
 * Daniel Huson, 7.2011
 */
public class Value extends Single<Integer> {
    /**
     * constructor
     */
    public Value() {
        super(0);
    }

    /**
     * constructor
     *
     * @param value
     */
    public Value(Integer value) {
        super(value);
    }

    /**
     * synchronized get
     *
     * @return value
     */
    public Integer get() {
        synchronized (this) {
            return super.get();
        }
    }

    /**
     * synchronized set
     *
     * @param value
     */
    public void set(Integer value) {
        synchronized (this) {
            super.set(value);
        }
    }

    /**
     * set to lower value. If value is not lower, does nothing
     *
     * @param value
     */
    public void lowerTo(int value) {
        synchronized (this) {
            if (super.get() > value)
                super.set(value);
        }

    }
}
