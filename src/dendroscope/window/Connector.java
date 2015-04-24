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

package dendroscope.window;

import jloda.graph.Node;

import javax.swing.*;
import java.awt.*;

/**
 * a connector between nodes in different treeViewers
 * Daniel Huson, 7.2010
 */
public class Connector {
    private final TreeViewer treeViewer1;
    private final TreeViewer treeViewer2;
    private final Node v1;
    private final Node v2;
    private Color color;
    static final Stroke widthOneStroke = new BasicStroke(1);
    private Stroke stroke = widthOneStroke;
    private byte lineWidth = 1;

    /**
     * constuct a connector
     *
     * @param treeViewer1
     * @param v1
     * @param treeViewer2
     * @param v2
     * @param color
     */
    public Connector(TreeViewer treeViewer1, Node v1, TreeViewer treeViewer2, Node v2, Color color) {
        this.treeViewer1 = treeViewer1;
        this.treeViewer2 = treeViewer2;
        this.v1 = v1;
        this.v2 = v2;
        this.color = color;
    }

    private static final int FIX_Y_OFFSET = 20; // mapped coordinates seem to be off by this amount

    /**
     * paint the connector
     *
     * @param component
     * @param g
     */
    void paint(Component component, Graphics2D g) {
        if (v1.getOwner() != null && v2.getOwner() != null && treeViewer1.getLabel(v1) != null && treeViewer2.getLabel(v2) != null && v1.getDegree() > 0 && v2.getDegree() > 0) {
            Point p = treeViewer1.trans.w2d(treeViewer1.getLocation(v1));
            SwingUtilities.convertPointToScreen(p, treeViewer1);
            SwingUtilities.convertPointFromScreen(p, component);
            p.y -= FIX_Y_OFFSET;
            g.setColor(Color.YELLOW);
            //g.drawString(treeViewer1.getLabel(node1),p.x,p.y);
            Rectangle labelRect1 = treeViewer1.getNV(v1).getLabelRect(treeViewer1.trans);
            if (labelRect1 != null) {
                Point z = (Point) labelRect1.getLocation().clone();
                SwingUtilities.convertPointToScreen(z, treeViewer1);
                SwingUtilities.convertPointFromScreen(z, component);
                labelRect1.x = z.x;
                labelRect1.y = z.y - FIX_Y_OFFSET;

                // g.drawRect(labelRect1.x, labelRect1.y, labelRect1.width, labelRect1.height);
            }

            Rectangle rect1 = (Rectangle) treeViewer1.getScrollPane().getBounds().clone();
            SwingUtilities.convertRectangle(treeViewer1, rect1, component);
            rect1.x += 5;
            rect1.y += 5;
            rect1.width -= 10;
            rect1.height -= 10;

            //g.setColor(Color.CYAN);
            //g.drawRect(rect1.x,rect1.y,rect1.width,rect1.height);
            if (rect1.contains(p) && v2 != null) {
                Point q = treeViewer2.trans.w2d(treeViewer2.getLocation(v2));
                SwingUtilities.convertPointToScreen(q, treeViewer2);
                SwingUtilities.convertPointFromScreen(q, component);
                q.y -= FIX_Y_OFFSET;

                // g.setColor(Color.YELLOW);
                // g.drawString(treeViewer2.getLabel(node2),q.x,q.y);

                Rectangle labelRect2 = treeViewer2.getNV(v2).getLabelRect(treeViewer2.trans);
                if (labelRect2 != null) {
                    Point z = (Point) labelRect2.getLocation().clone();
                    SwingUtilities.convertPointToScreen(z, treeViewer2);
                    SwingUtilities.convertPointFromScreen(z, component);
                    labelRect2.x = z.x;
                    labelRect2.y = z.y - FIX_Y_OFFSET;

                    //g.drawRect(labelRect2.x, labelRect2.y, labelRect2.width, labelRect2.height);
                }

                Rectangle rect2 = (Rectangle) treeViewer2.getScrollPane().getBounds().clone();
                SwingUtilities.convertRectangle(treeViewer2, rect2, component);
                rect2.x += 5;
                rect2.y += 5;
                rect2.width -= 10;
                rect2.height -= 10;

                if (labelRect1 != null) {
                    p = adjust(p, labelRect1, q);
                }
                if (labelRect2 != null) {
                    q = adjust(q, labelRect2, p);
                }

                if (rect2.contains(q)) {
                    if (isSelected())
                        g.setColor(Color.PINK);
                    else
                        g.setColor(color);
                    g.setStroke(stroke);
                    g.drawLine(p.x, p.y, q.x, q.y);
                }
            }
        }
    }

    /**
     * adjust beginning and end of lines so that they start at labels rather than nodes
     *
     * @param p0
     * @param rect
     * @param q
     * @return new location for p0
     */
    private Point adjust(Point p0, Rectangle rect, Point q) {
        int left = rect.x;
        int right = rect.x + rect.width;
        int top = rect.y;
        int bot = rect.y + rect.height;

        Point p = (Point) p0.clone();

        if (p.x < right && right < q.x)
            p.x = right + 1;
        else if (p.x > left && left > q.x)
            p.x = left - 1;

        if (p.y < bot && bot < q.y)
            p.y = bot + 1;
        else if (p.y > top && top > q.y)
            p.y = top - 1;

        return p;
    }

    public boolean isSelected() {
        return v1.getOwner() != null && v2.getOwner() != null && treeViewer1.getSelected(v1) && treeViewer2.getSelected(v2);
    }

    public void setSelected(boolean select) {
        if (v1.getOwner() != null && v2.getOwner() != null) {
            treeViewer1.setSelected(v1, select);
            treeViewer2.setSelected(v2, select);
        }
    }

    public void setColor(Color a) {
        color = a;
    }

    public Color getColor() {
        return color;
    }

    public void setLineWidth(byte a) {
        lineWidth = a;
        if (a == 1)
            stroke = widthOneStroke;
        else
            stroke = new BasicStroke(a);
    }

    public byte getLineWidth() {
        return lineWidth;
    }

    public TreeViewer getTreeViewer1() {
        return treeViewer1;
    }

    public TreeViewer getTreeViewer2() {
        return treeViewer2;
    }

    public Node getV1() {
        return v1;
    }

    public Node getV2() {
        return v2;
    }

    public boolean isValid() {
        return v1 != null && v1.getOwner() != null && v2 != null && v2.getOwner() != null;
    }
}
