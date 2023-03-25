/*
 *   Edge3d.java Copyright (C) 2023 Daniel H. Huson
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
