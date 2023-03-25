/*
 *   Object3dList.java Copyright (C) 2023 Daniel H. Huson
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

public class Object3dList extends Object3dAdaptor {

    protected double[] keys;
    protected Object3d[] elementData;
    protected int elementCount;


    public Object3dList(int initial) {
        elementData = new Object3d[initial];
        elementCount = 0;
    }

    public void addElement(Object3d e) {
        if (elementData.length == elementCount) {
            Object3d[] newData = new Object3d[1 + 2 * elementData.length];
            System.arraycopy(elementData, 0, newData, 0, elementCount);
            elementData = newData;
        }
        elementData[elementCount++] = e;
        this.centre = e.centre();
    }

    public void append(Object3dList l) {
        for (int i = 0; i < l.size(); i++) {
            addElement(l.elementAt(i));
        }
    }

    public Object3d elementAt(int i) {
        return elementData[i];
    }

    public int size() {
        return elementCount;
    }

    /**
     * ensure keys array exists
     */
    void initKeys() {
        if (keys == null || keys.length != elementCount) {
            keys = new double[elementCount];
        }
    }


    public void transform(Matrix3D T) {
        for (int i = elementCount - 1; i >= 0; i--) {
            (elementData[i]).transform(T);
        }
    }
}



