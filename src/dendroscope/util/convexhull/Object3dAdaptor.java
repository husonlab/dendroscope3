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

package dendroscope.util.convexhull; /**
 * author: Tim Lambert, UNSW, 2000
 */

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
