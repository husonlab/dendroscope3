/**
 * Cube.java 
 * Copyright (C) 2019 Daniel H. Huson
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

public class Cube extends Object3dAdaptor {

    final Point3d[] cube = {
            new Point3d(0, 0, 0), //cube[0] is the centre of the cube
            new Point3d(-1, -1, -1), new Point3d(1, -1, -1), //corners of cube
            new Point3d(-1, 1, -1), new Point3d(1, 1, -1),
            new Point3d(-1, -1, 1), new Point3d(1, -1, 1),
            new Point3d(-1, 1, 1), new Point3d(1, 1, 1)};

    final static Point3d[] unitCube = {
            new Point3d(0, 0, 0), //cube[0] is the centre of the cube
            new Point3d(-1, -1, -1), new Point3d(1, -1, -1), //corners of cube
            new Point3d(-1, 1, -1), new Point3d(1, 1, -1),
            new Point3d(-1, -1, 1), new Point3d(1, -1, 1),
            new Point3d(-1, 1, 1), new Point3d(1, 1, 1)};

    final static int[][] faces = {
            {1, 2, 6, 5}, //This means that face 0 has corners 1 2 6 and 5
            {2, 4, 8, 6},
            {8, 7, 5, 6},
            {8, 4, 3, 7},
            {1, 3, 4, 2},
            {5, 7, 3, 1}};

    final static Point3d[] unitCubeNormal = { //normals to faces of unit cube
            new Point3d(0, -1, 0),
            new Point3d(1, 0, 0),
            new Point3d(0, 0, 1),
            new Point3d(0, 1, 0),
            new Point3d(0, 0, -1),
            new Point3d(-1, 0, 0)};

    final static int npoints = faces[0].length; //no of points on a face
    final static int nfront = 3; //no of front faces on cube
    static Color[] cols = { //colours of the faces
            Color.yellow,
            Color.blue,
            Color.red,
            Color.yellow,
            Color.red,
            Color.blue};

    static Color[] selectcols = { //colours of faces of slected cube
            Color.green,
            Color.magenta,
            Color.cyan,
            Color.green,
            Color.cyan,
            Color.magenta};

    /**
     * Create a cube with centre at specified point and with given radius
     */
    public Cube(Point3d centre, double radius) {
        Matrix3D T; //matrix that transforms unit cube into world space.
        this.centre = centre;
        lastRadius = radius;
        T = new Matrix3D();
        T.scale(radius);
        T.translate(centre);
        T.transform(cube);
    }

    static double lastRadius = 0.02;

    /**
     * Create a cube with centre at specified point and with same radius as last one
     */
    public Cube(Point3d centre) {
        this(centre, lastRadius);
    }

    /**
     * method needed by render to compute normal to a face
     */
    Point3d faceNormal(int i) {
        return cube[faces[i][1]].subtract(cube[faces[i][0]]).cross(cube[faces[i][2]].subtract(cube[faces[i][0]]));
    }

    /**
     * compute new position of cube
     */
    public void transform(Matrix3D T) {
        T.transform(cube);
        centre = cube[0];
    }

}
