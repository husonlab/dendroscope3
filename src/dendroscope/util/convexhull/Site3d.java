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
    private static Point ip[] = new Point[Cube.unitCube.length];

    public String toString() {
        return centre.toString();
    }


}
