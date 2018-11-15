/**
 * Edge3dPlus.java 
 * Copyright (C) 2018 Daniel H. Huson
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
package dendroscope.util.convexhull; /**
 * author: Tim Lambert, UNSW, 2000
 */

/**
 * A 3d edge with a list of associated points
 * It's not a triangle but...
 */

public class Edge3dPlus extends Edge3d {
    /* the points associated with this. inside() is true for each point in here */
    final Object3dList pts;
    int selectFrameNo = -1; //frame no this tri was selected
    final HalfSpace h;

    public Edge3dPlus(Point3d a, Point3d b, int frameNo) {
        super(a, b, frameNo);
        h = new HalfSpace(a, b);

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

    /* set frameno that this tri wasselected for processing */

    public void select(int n) {
        selectFrameNo = n;
        extreme().select(n);
    }

    /**
     * return list of points associated with this triangle
     */
    public Object3dList getPoints() {
        return pts;
    }

    /**
     * return point farthest from support plane of this triangl
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
