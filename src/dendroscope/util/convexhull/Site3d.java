/*
 * Site3d.java Copyright (C) 2022 Daniel H. Huson
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

import java.awt.*;

/**
 * A point in 3d space that can be rendered
 */

public class Site3d extends Object3dAdaptor {


    public Site3d(Point3d pt) {
        centre = pt;
    }

    public Site3d(Point3d pt, int frameNo) {
        this(pt);
        this.firstFrame = frameNo;
    }

    private static int nfront;
    private static int[][] ix = new int[Cube.nfront][Cube.npoints + 1]; // nfront visible cube faces in
    private static int[][] iy = new int[Cube.nfront][Cube.npoints + 1]; // screen space
    private static Color[] icols = new Color[Cube.nfront];
    private static Color[] iselectcols = new Color[Cube.nfront];
    private static Point[] ip = new Point[Cube.unitCube.length];

    public String toString() {
        return centre.toString();
    }


}
