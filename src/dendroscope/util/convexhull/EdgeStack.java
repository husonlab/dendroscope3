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

package dendroscope.util.convexhull; /**
 * author: Tim Lambert, UNSW, 2000
 */

import java.util.Enumeration;
import java.util.Stack;

/**
 * This class stores the edges that still need to be processed.
 * It works like a regular stack except that putting AB on the stack when
 * BA is already there causes both edges to be eliminated.
 */
public class EdgeStack {
    private final Stack data; // contents of the stack

    public EdgeStack() {
        data = new Stack();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public Edge3d get() {
        return (Edge3d) data.pop();
    }

    public void put(Edge3d e) {
        data.push(e);
    }

    public void put(Point3d a, Point3d b) {
        put(new Edge3d(a, b));
    }

    public void putp(Edge3d e) {
        int ind = data.indexOf(e);
        if (ind == -1) {
            data.push(e);
        } else {
            data.removeElementAt(ind);
        }
    }

    public void putp(Point3d a, Point3d b) {
        putp(new Edge3d(a, b));
    }

    public void dump() {
        Enumeration e = data.elements();
        System.out.println(data.size());
        while (e.hasMoreElements()) {
            System.out.println(e.nextElement());
        }
        System.out.println();
    }

}

