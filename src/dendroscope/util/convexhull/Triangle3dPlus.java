/*
 *   Triangle3dPlus.java Copyright (C) 2020 Daniel H. Huson
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
package dendroscope.util.convexhull; /**
 * author: Tim Lambert, UNSW, 2000
 */

/**
 * A 3d triangle with a list of associated points
 */

public class Triangle3dPlus extends Triangle3d {
    /* the points associated with this. inside() is true for each point in here */

    final Object3dList pts;

    public Triangle3dPlus(Point3d a, Point3d b, Point3d c, int frameNo) {
        super(a, b, c, frameNo);
        pts = new Object3dList(10);
    }

    /**
     * try to add a point to association list.  Return true if succesful
     */
    public boolean add(Point3dObject3d p) {
        if (inside(p)) {
            pts.addElement(p);
            return true;
        } else {
            return false;
        }
    }

    /**
     * return list of points associated with this triangle
     */
    public Object3dList getPoints() {
        return pts;
    }

    /**
     * return point farthest from support plane of this triangle
     */
    public Point3dObject3d extreme() {
        Point3dObject3d res = null;
        double maxd = Double.MIN_VALUE;
        for (int i = 0; i < pts.size(); i++) {
            double d = h.normal.dot((Point3d) pts.elementAt(i));
            if (d > maxd) {
                res = (Point3dObject3d) pts.elementAt(i);
                maxd = d;
            }
        }
        return res;
    }
}
