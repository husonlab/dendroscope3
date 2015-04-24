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

package dendroscope.dtl;

public class Event {

    int dup;
    final int transfer;
    final int loss;

    public Event() {
        dup = 0;
        transfer = 0;
        loss = 0;
    }

    public void incrementDup() {
        this.dup = this.dup++;
    }

    public void incrementTransfer() {
        this.dup = this.dup++;
    }

    public void incrementLoss() {
        this.dup = this.dup++;
    }
}
