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



