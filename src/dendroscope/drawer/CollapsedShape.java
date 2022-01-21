/*
 * CollapsedShape.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.drawer;

import jloda.swing.graphview.Transform;
import jloda.swing.util.PolygonDouble;

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
