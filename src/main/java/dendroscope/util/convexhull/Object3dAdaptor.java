/*
 * Object3dAdaptor.java Copyright (C) 2023 Daniel H. Huson
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
 * Adaptor class for Object3d - provides default implementation for all the methods
 */
public class Object3dAdaptor implements Object3d {
    protected Point3d centre;


    static int idCount; //Total no of these created
    private final int id;

    public Object3dAdaptor() {
        id = idCount++;
    }

    //name of this object for VRML purposes

    public String id() {
        return "O" + id;
    }

    public void setCentre(Point3d c) {
        centre = c;
    }

    public Point3d centre() {
        return centre;
    }

    protected int firstFrame = 0;
    protected int lastFrame = Integer.MAX_VALUE;

    public void setFirstFrame(int f) {
        firstFrame = f;
    }

    public int getFirstFrame() {
        return firstFrame;
    }

    public void setLastFrame(int f) {
        lastFrame = f;
    }

    public int getLastFrame() {
        return lastFrame;
    }

    int selectFrameNo = -1; //frame no this was selected

    /* set frameno that this was selected for processing */

    public void select(int n) {
        selectFrameNo = n;
    }

    public int getSelectFrame() {
        return selectFrameNo;
    }

    public void transform(Matrix3D T) { // do nothing
    }

}
