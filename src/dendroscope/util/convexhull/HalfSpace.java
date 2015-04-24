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

public class HalfSpace {

    /*final*/
    final Point3d normal; // normal to boundary plane
    /*final*/
    final double d; // eqn of half space is normal.x - d > 0

    /**
     * Create a half space
     */
    public HalfSpace(Point3d a, Point3d b, Point3d c) {
        normal = b.subtract(a).cross(c.subtract(a)).normalize();
        d = normal.dot(a);
    }

    /**
     * Create a half space parallel to z axis
     */
    public HalfSpace(Point3d a, Point3d b) {
        normal = b.subtract(a).cross(Point3d.k).normalize();
        d = normal.dot(a);
    }

    public boolean inside(Point3d x) {
        return normal.dot(x) > d;
    }

    /**
     * z coordinate of intersection of a vertical line through p and boundary plane
     */
    public double zint(Point3d p) {
        return (d - normal.x() * p.x() - normal.y() * p.y()) / normal.z();
    }

}







