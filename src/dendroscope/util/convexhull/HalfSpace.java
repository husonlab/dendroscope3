/*
 * HalfSpace.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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







