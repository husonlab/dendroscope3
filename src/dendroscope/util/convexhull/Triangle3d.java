/*
 *   Triangle3d.java Copyright (C) 2023 Daniel H. Huson
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
