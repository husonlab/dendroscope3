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

package dendroscope.util.convexhull;

/**
 * author: Tim Lambert, UNSW, 2000
 */

public class Edge3d extends Object3dAdaptor {
    final Point3d start;
    final Point3d end; //end points

    public Edge3d(Point3d start, Point3d end) {
        this.start = start;
        this.end = end;
        centre = start;
    }

    public Edge3d(Point3d start, Point3d end, int frameNo) {
        this(start, end);
        this.firstFrame = frameNo;
    }


    public boolean equals(Object o) {
        if (o instanceof Edge3d) {
            Edge3d e = (Edge3d) o;
            return (start == e.end && end == e.start) ||
                    (end == e.end && start == e.start);
        } else {
            return false;
        }
    }

    public boolean inside(Point3d x) {
        HalfSpace h = new HalfSpace(start, end);
        return h.inside(x);
    }

    public String toString() {
        return "(" + start + "," + end + ")";
    }
}
