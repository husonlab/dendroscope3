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

public class Triangle3d extends Object3dAdaptor {

    final Point3d[] tri;
    final int col;
    HalfSpace h;
    static int backFaceColor; //colour for back faces

    /**
     * Create a triangle with given colour
     */
    public Triangle3d(Point3d[] tri, int col) {
        this.tri = tri;
        this.col = col;
        computeHalfSpace();
    }

    public Triangle3d(Point3d a, Point3d b, Point3d c, int frameNo) {
        tri = new Point3d[3];
        tri[0] = a;
        tri[1] = b;
        tri[2] = c;
        col = -1;
        computeHalfSpace();
    }

    private void computeHalfSpace() {
        h = new HalfSpace(tri[0], tri[1], tri[2]);
        centre = tri[0].add(tri[1]).add(tri[2]).scale(1.0 / 3.0);
    }

    public boolean inside(Point3d x) {
        return h.inside(x);
    }


    /**
     * compute new position of tri
     */
    public void transform(Matrix3D T) {
        T.transform(tri);
        centre = tri[0].add(tri[1]).add(tri[2]).scale(1.0 / 3.0);
    }

    public String toString() {
        return "(" + tri[0] + "," + tri[1] + "," + tri[2] + ")";
    }
}
