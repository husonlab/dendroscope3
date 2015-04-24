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
 * A Point3d that is also an Object 3d so that it can be rendered
 */
public class Point3dObject3d extends Point3d implements Object3d {

    private final Site3d site;

    public Point3dObject3d(Point3d p) {
        super();
        site = new Site3d(p);
        setCentre(p);
    }

    public Point3dObject3d(Point3d p, int frame) {
        this(p);
        setLastFrame(frame);
        setFirstFrame(frame);
    }

    //name of this object for VRML purposes

    public String id() {
        return site.id();
    }

    public void setCentre(Point3d c) {
        this.v = c.v;
    }

    public Point3d centre() {
        return this;
    }


    public void setFirstFrame(int f) {
        site.setFirstFrame(f);
    }

    public void setLastFrame(int f) {
        site.setLastFrame(f);
    }

    /* set frameno that this point was selected for processing */

    public void select(int n) {
        site.select(n);
    }

    public int getSelectFrame() {
        return site.getSelectFrame();
    }

    public int getFirstFrame() {
        return site.getFirstFrame();
    }

    public int getLastFrame() {
        return site.getLastFrame();
    }

    public void transform(Matrix3D T) {
        T.transform(this);
    }
}
