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

package dendroscope.drawer;

import jloda.graphview.Transform;
import jloda.util.PolygonDouble;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * maintains the proxy shape for a collapsed subtree
 * Daniel Huson, 7.2010
 */
public class CollapsedShape {
    private PolygonDouble polygonWC;

    /**
     * construct from min and max coordinates in parallel view
     */
    public CollapsedShape(Point2D[] points) {
        setPointsWC(points);
    }

    /**
     * set the coordinates of the shape
     *
     * @param pointsWC
     */
    public void setPointsWC(Point2D[] pointsWC) {
        polygonWC = new PolygonDouble(pointsWC.length, pointsWC);
    }

    /**
     * draw the shape
     *
     * @param trans
     * @param g
     */
    public void draw(Transform trans, Graphics2D g, Color color, Color backgroundColor, boolean selected) {
        if (polygonWC != null) {
            Polygon polygonDC = trans.w2d(polygonWC);

            if (backgroundColor != null && !backgroundColor.equals(g.getBackground())) {
                g.setColor(backgroundColor);
                g.fill(polygonDC);
            }
            if (selected)
                g.setColor(Color.RED);
            else if (color != null)
                g.setColor(color);
            else
                g.setColor(Color.GRAY);
            g.draw(polygonDC);
        }
    }

    /**
     * does shape contain the point with device coordinates y,y?
     *
     * @param trans
     * @param x
     * @param y
     * @return true, if shape contains point
     */
    public boolean hit(Transform trans, int x, int y) {
        if (polygonWC != null) {
            Polygon polygonDC = trans.w2d(polygonWC);
            return polygonDC.contains(x, y);
        }
        return false;
    }

    /**
     * is shape contained in the given device retangle?
     *
     * @param trans
     * @param rect
     * @return true, if contained in rectangle
     */
    public boolean hit(Transform trans, Rectangle2D rect) {
        if (polygonWC != null) {

            Polygon polygonDC = trans.w2d(polygonWC);
            return rect.contains(polygonDC.getBounds());
        }
        return false;
    }
}
