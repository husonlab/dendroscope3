/**
 * TreeDrawerBase.java 
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
package dendroscope.drawer;

import dendroscope.consensus.TransferVisualization;
import dendroscope.window.TreeViewer;
import jloda.graph.*;
import jloda.phylo.PhyloTree;
import jloda.phylo.PhyloTreeUtils;
import jloda.swing.graphview.*;
import jloda.swing.util.BasicSwing;
import jloda.swing.util.Geometry;
import jloda.swing.util.PolygonDouble;
import jloda.swing.util.ProgramProperties;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.*;


/**
 * base graph drawer for optimized tree drawers
 * Daniel Huson, 1.2007
 */
public class TreeDrawerBase {
    protected final TreeViewer viewer;
    protected final PhyloTree tree;
    protected final Transform trans;

    protected final NodeSet hitNodes;
    protected final NodeSet hitNodeLabels;
    protected final EdgeSet hitEdges;
    protected final EdgeSet hitEdgeLabels;

    protected final LabelOverlapAvoider labelOverlapAvoider;

    protected final NodeSet nodesWithMovedLabels;
    protected final EdgeSet edgesWithMovedLabels;
    protected final EdgeSet edgesWithMovedInternalPoints;

    protected final NodeSet visited;

    protected List<String> taxonOrder;

    protected NodeSet collapsedNodes;

    protected INodeDrawer nodeDrawer;

    protected boolean showBoundingBoxes = false;
    protected final boolean showProxyShapes = false;

    protected boolean radialLabels = false; // rotate node labels to match edge directions?

    protected final NodeArray<Rectangle2D> node2bb;   // maps node to bbox of subtree rooted at node
    protected final NodeArray<PolygonDouble> node2ProxyShape; // maps node to proxy shape use to represent subtree below node
    protected Rectangle visibleRect = null; // use this for clipping purposes

    protected final NodeArray<List<Node>> node2LSAChildren; // node to target map for computing embedding
    private boolean showLSAEdges = false;

    protected static int MIN_LEAVES_FOR_PROXY = 250; // don't compute proxy of node if has less than this number of leaves

    boolean toScale = false;

    private int auxilaryParameter = 0;

    /**
     * constructor. Call only after graph and trans have been set for GraphView
     *
     * @param viewer
     */
    public TreeDrawerBase(TreeViewer viewer, PhyloTree tree) {
        this.viewer = viewer;
        this.tree = tree;
        trans = viewer.trans;
        hitNodes = new NodeSet(tree);
        hitNodeLabels = new NodeSet(tree);
        hitEdges = new EdgeSet(tree);
        hitEdgeLabels = new EdgeSet(tree);
        labelOverlapAvoider = new LabelOverlapAvoider(viewer, 100);
        node2bb = new NodeArray<>(tree);
        node2ProxyShape = new NodeArray<>(tree);
        nodesWithMovedLabels = new NodeSet(tree);
        edgesWithMovedLabels = new EdgeSet(tree);
        edgesWithMovedInternalPoints = new EdgeSet(tree);
        node2LSAChildren = tree.getNode2GuideTreeChildren();

        nodeDrawer = new DefaultNodeDrawer(viewer);

        visited = new NodeSet(tree);
    }

    /**
     * paint the graph. If rect is non-null, only need to cover rect
     *
     * @param gc0
     * @param visibleRect
     */
    public void paint(Graphics gc0, Rectangle visibleRect) {
        final Graphics2D gc = (Graphics2D) gc0;
        this.visibleRect = visibleRect;

        Color tmpColor = gc0.getColor();
        gc0.setColor(tmpColor);
        gc.setFont(viewer.getFont());
        BasicStroke stroke = new BasicStroke(1);
        gc.setStroke(stroke);

        Node root = tree.getRoot();
        if (root == null)
            return;

        labelOverlapAvoider.resetHasNoOverlapToPreviouslyDrawnLabels();

        showLSAEdges = ProgramProperties.get("showlsa", false);

        if (false) {
            TransferVisualization.paint(gc, viewer, tree);
        }

        paintCollapsedNodes(gc);

        nodeDrawer.setup(viewer, gc);

        visited.clear();
        paintRec(root, gc);

        if (this.showBoundingBoxes) {
            for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
                if (viewer.getSelected(v)) {
                    gc.setColor(Color.RED);
                    Rectangle2D.Double bbW = (Rectangle2D.Double) node2bb.get(v);
                    gc.draw(trans.w2d(bbW));
                }
            }
        }
        if (this.showProxyShapes) {
            for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
                if (viewer.getSelected(v)) {
                    PolygonDouble shapeWC = node2ProxyShape.get(v);
                    if (shapeWC != null) {
                        gc.setColor(Color.GREEN);
                        gc.draw(trans.w2d(shapeWC));
                    } else {
                        gc.setColor(Color.GRAY);
                        gc.draw(trans.w2d(node2bb.get(v)));
                    }
                }
            }
        }
    }

    /**
     * paint all the collapsed nodes first
     *
     * @param gc
     */
    private void paintCollapsedNodes(Graphics2D gc) {
        if (tree.getRoot() != null && tree.getRoot().getOwner() != null) {
            Stack<Node> stack = new Stack<>();
            stack.push(tree.getRoot());
            while (stack.size() > 0) {
                Node v = stack.pop();
                if (viewer.getCollapsedNodes().contains(v)) {
                    CollapsedShape shape = viewer.getCollapsedShape(v);
                    if (shape != null)
                        shape.draw(trans, gc, viewer.getColor(v), viewer.getBackgroundColor(v), viewer.getSelected(v));
                } else {
                    for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                        if (PhyloTreeUtils.okToDescendDownThisEdge(tree, e, v)) {
                            stack.push(e.getTarget());
                        }
                    }
                }
            }
        }
    }

    /**
     * recursively does the work
     *
     * @param v
     * @param gc
     */
    private void paintRec(Node v, Graphics2D gc) {
        try {
            final NodeView nv = viewer.getNV(v);

            if (nv.getLabel() != null)
                nv.setLabelSize(BasicSwing.getStringSize(gc, viewer.getLabel(v), viewer.getFont(v))); // ensure label rect is set

            if (!mustVisitSubTreeBelowNode(v)) {
                if (visibleRect != null && trans.w2d(node2bb.get(v)).getBounds().intersects(visibleRect)) {
                    // draw proxy shape
                    gc.setColor(Color.BLACK);
                    PolygonDouble shapeWC = node2ProxyShape.get(v);
                    if (v.getInDegree() > 0)
                        gc.setColor(viewer.getEV(v.getFirstInEdge()).getColor());
                    if (shapeWC != null) {
                        Polygon shapeDC = trans.w2d(shapeWC);
                        gc.fillPolygon(shapeDC);
                        //gc.drawPolygon(shapeDC);
                    } else {
                        gc.fill(trans.w2d(node2bb.get(v)));
                    }
                }
            } else {
                if (!isCollapsed(v)) {
                    MagnifierUtil magnifierUtil = new MagnifierUtil(viewer);
                    for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                        final EdgeView ef = viewer.getEV(f);
                        final Node source = f.getSource();
                        final Node target = f.getTarget();
                        final NodeView sourceView = viewer.getNV(source);
                        final NodeView targetView = viewer.getNV(target);

                        Point2D nextToSource = targetView.getLocation();
                        Point2D nextToTarget = sourceView.getLocation();

                        if (!(nextToSource == null || nextToTarget == null)) {
                            if (viewer.getInternalPoints(f) != null) {
                                if (viewer.getInternalPoints(f).size() != 0) {
                                    nextToSource = viewer.getInternalPoints(f).get(0);
                                    nextToTarget = viewer.getInternalPoints(f).get(viewer.getInternalPoints(f).size() - 1);
                                }
                            }
                            magnifierUtil.addInternalPoints(f);

                            boolean arcEdge = (ef.getShape() == EdgeView.ARC_LINE_EDGE || ef.getShape() == EdgeView.QUAD_EDGE);

                            final Point sourcePt = arcEdge ? trans.w2d(sourceView.getLocation()) : sourceView.computeConnectPoint(nextToSource, trans);
                            final Point targetPt = arcEdge ? trans.w2d(targetView.getLocation()) : targetView.computeConnectPoint(nextToTarget, trans);

                            if (viewer.getEV(f).getLineWidth() != ((BasicStroke) gc.getStroke()).getLineWidth()) {
                                BasicStroke stroke = new BasicStroke(viewer.getEV(f).getLineWidth());
                                gc.setStroke(stroke);
                            }
                            viewer.getEV(f).draw(gc, sourcePt, targetPt, trans, viewer.selectedEdges.contains(f));
                            if (viewer.getLabel(f) != null) {
                                ef.setLabelReferenceLocation(nextToSource, nextToTarget, trans);
                                ef.setLabelSize(gc);
                                ef.drawLabel(gc, trans, viewer.selectedEdges.contains(f));
                            }
                        }
                        magnifierUtil.removeAddedInternalPoints(f);
                        if (PhyloTreeUtils.okToDescendDownThisEdge(tree, f, v))
                            paintRec(f.getTarget(), gc);


                        if (showLSAEdges && node2LSAChildren.get(v) != null) {
                            gc.setColor(Color.GREEN);
                            Point a = viewer.trans.w2d(viewer.getLocation(v));
                            for (Node w : node2LSAChildren.get(v)) {
                                if (v.getCommonEdge(w) == null) {
                                    Point b = viewer.trans.w2d(viewer.getLocation(w));
                                    gc.drawLine(a.x + 1, a.y + 1, b.x + 1, b.y + 1);
                                }
                            }
                        }
                    }
                }
            }
            if (nv.getLineWidth() != ((BasicStroke) gc.getStroke()).getLineWidth()) {
                BasicStroke stroke = new BasicStroke(nv.getLineWidth());
                gc.setStroke(stroke);
            }

            boolean selected = viewer.selectedNodes.contains(v);

            nodeDrawer.draw(v, selected);
            if (labelOverlapAvoider.hasNoOverlapToPreviouslyDrawnLabels(v, nv) || selected) {
                nodeDrawer.drawLabel(v, selected);
            }
        } catch (Exception ex) {
            // Basic.caught(ex);
        }
    }

    /**
     * must we visit the subtree rooted at this node when drawing or looking for a mouse click?
     * Returns true for a leaf.
     * Different algorithms should implement their own version of this!
     *
     * @param v
     * @return true, if we must look at subtree below v
     */
    protected boolean mustVisitSubTreeBelowNode(Node v) {
        if (v.getOutDegree() == 1)
            return true;
        if (node2ProxyShape.get(v) == null && isCollapsed(v))
            return false;
        Rectangle bbD = trans.w2d(node2bb.get(v)).getBounds();
        if (visibleRect != null && bbD.intersects(visibleRect) == false)
            return false; // not visible on screen
        return !(bbD.height <= 2 || bbD.width < 2);
    }

    /**
     * does the (x,y) hit the bounding box?
     *
     * @param v  node
     * @param dx x tolerance
     * @param dy y tolerance
     * @param x  x mouse
     * @param y  y mouse
     * @return true, if hits
     */
    protected boolean hitsBBox(Node v, int dx, int dy, int x, int y) {
        Rectangle bbD = trans.w2d(node2bb.get(v)).getBounds();
        if (dx != 0 || dy != 0)
            bbD.grow(dx, dy);
        return bbD.contains(x, y);

    }

    /**
     * does the rectangle intersect the bounding box?
     *
     * @param v    node
     * @param dx   x tolerance
     * @param dy   y tolerance
     * @param rect rectangle
     * @return true, if intersects
     */
    protected boolean hitsBBox(Node v, int dx, int dy, Rectangle rect) {
        Rectangle bbD = trans.w2d(node2bb.get(v)).getBounds();
        if (visibleRect != null && bbD.intersects(visibleRect) == false)
            return false; // not visible on screen
        if (bbD.height <= 2 || bbD.width < 2) // bbox too small
            return false;
        if (dx != 0 || dy != 0)
            bbD.grow(dx, dy);
        return bbD.intersects(rect);

    }

    /**
     * get all nodes hit by mouse at (x,y) with tolerance of d pixels
     *
     * @param x
     * @param y
     * @return nodes hit
     */
    public NodeSet getHitNodes(int x, int y) {
        hitNodes.clear();
        Node v = tree.getRoot();
        if (v != null)
            getHitNodesRectangleRec(v, x, y, false);
        return hitNodes;
    }

    /**
     * get all nodes hit by mouse at (x,y) with tolerance of d pixels
     *
     * @param x
     * @param y
     * @return nodes hit
     */
    public NodeSet getHitNodes(int x, int y, int d) {
        return getHitNodes(x, y);
    }

    /**
     * recursively do the work
     *
     * @param v
     * @param x
     * @param y
     * @param moreThanOne
     */
    private boolean getHitNodesRectangleRec(Node v, int x, int y, boolean moreThanOne) {
        try {
            if (viewer.getLocation(v) != null && viewer.getNV(v).contains(trans, x, y)) {
                hitNodes.add(v);
                if (!moreThanOne) return true;
            } else if (isCollapsed(v)) {
                CollapsedShape shape = viewer.getCollapsedShape(v);
                if (shape != null && shape.hit(trans, x, y))
                    hitNodes.add(v);
                if (!moreThanOne) return true;
            }
            if (!isCollapsed(v) && mustVisitSubTreeBelowNode(v) && hitsBBox(v, 5, 5, x, y)) {
                for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                    if (PhyloTreeUtils.okToDescendDownThisEdge(tree, f, v)) {
                        Node w = f.getOpposite(v);
                        Rectangle bbD = trans.w2d(node2bb.get(w)).getBounds();
                        bbD.grow(10, 10);
                        if ((viewer.getLocation(w) != null && viewer.getNV(w).contains(trans, x, y)) || bbD.contains(x, y)) {
                            if (getHitNodesRectangleRec(w, x, y, moreThanOne) && moreThanOne)
                                return true;
                        }
                    }
                }
            }
        } catch (Exception ex) {
        }
        return false;
    }

    /**
     * recursively do the work
     *
     * @param v
     * @param rect
     * @param moreThanOne
     */
    private boolean getHitNodesRectangleRec(Node v, Rectangle rect, boolean moreThanOne) {
        if (viewer.getLocation(v) != null && rect.intersects(viewer.getBox(v))) {
            hitNodes.add(v);
            if (!moreThanOne) return true;
        } else if (isCollapsed(v)) {
            CollapsedShape shape = viewer.getCollapsedShape(v);
            if (shape != null && shape.hit(trans, rect)) {
                hitNodes.add(v);
                if (!moreThanOne) return true;
            }
        }
        if (!isCollapsed(v) && mustVisitSubTreeBelowNode(v) && hitsBBox(v, 5, 5, rect)) {
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                if (PhyloTreeUtils.okToDescendDownThisEdge(tree, f, v)) {
                    Node next = f.getOpposite(v);
                    Rectangle bbD = trans.w2d(node2bb.get(next)).getBounds();
                    bbD.grow(10, 10);
                    if ((viewer.getLocation(next) != null && viewer.getNV(next).intersects(trans, rect)) || bbD.intersects(rect)) {
                        if (getHitNodesRectangleRec(next, rect, moreThanOne) && moreThanOne)
                            return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * get hit node labels
     *
     * @param x
     * @param y
     * @return hit node labels
     */
    public NodeSet getHitNodeLabels(int x, int y) {
        hitNodeLabels.clear();

        //  take care of any labels that have been moved:
        for (Node v : nodesWithMovedLabels) {
            NodeView nv = viewer.getNV(v);
            if (nv.getLabel() != null && nv.getLocation() != null
                    && nv.getLabelVisible() &&
                    (viewer.getSelected(v) || labelOverlapAvoider.isVisible(v))
                    &&
                    nv.getLabelShape(trans) != null && nv.getLabelShape(trans).contains(x, y)) {
                hitNodeLabels.add(v);
            }
        }

        Node v = tree.getRoot();
        if (v != null)
            getHitNodeLabelsRec(v, x, y);

        return hitNodeLabels;
    }

    /**
     * recursively do the work
     *
     * @param v
     * @param x
     * @param y
     */
    private void getHitNodeLabelsRec(Node v, int x, int y) {
        NodeView nv = viewer.getNV(v);
        if (nv.getLabel() != null && nv.getLocation() != null
                && nv.getLabelVisible() && labelOverlapAvoider.isVisible(v)
                &&
                nv.getLabelShape(trans) != null &&
                nv.getLabelShape(trans).contains(x, y)
                ) {
            hitNodeLabels.add(v);
        }
        if (mustVisitSubTreeBelowNode(v) && hitsBBox(v, 100, 100, x, y)) {
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                if (PhyloTreeUtils.okToDescendDownThisEdge(tree, f, v) && !isCollapsed(v)) {
                    getHitNodeLabelsRec(f.getTarget(), x, y);
                }
            }
        }
    }

    /**
     * get all nodes contained in rect
     *
     * @param rect
     * @return nodes contained in rect
     */
    public NodeSet getHitNodes(Rectangle rect) {
        hitNodes.clear();


        Node v = tree.getRoot();
        if (v != null)
            getHitNodesRectangleRec(v, rect, true);
        return hitNodes;
    }


    /**
     * get all node labels contained in rect
     *
     * @param rect
     * @return node labels contained in rect
     */
    public NodeSet getHitNodeLabels(Rectangle rect) {
        hitNodeLabels.clear();
        // first take care of any labels that have been moved:
        for (Node v : nodesWithMovedLabels) {
            NodeView nv = viewer.getNV(v);
            if (nv.getLabel() != null && nv.getLocation() != null &&
                    (viewer.getSelected(v) || labelOverlapAvoider.isVisible(v))
                    && nv.getLabelVisible() && rect.contains(nv.getLabelShape(trans).getBounds())) {
                hitNodeLabels.add(v);
            }
        }

        Node v = tree.getRoot();
        if (v != null)
            getHitNodeLabelsRectangleRec(v, rect);
        return hitNodeLabels;
    }

    /**
     * recursively do the work
     *
     * @param v
     * @param rect
     */
    private void getHitNodeLabelsRectangleRec(Node v, Rectangle rect) {
        NodeView nv = viewer.getNV(v);
        if (nv.getLabel() != null && nv.getLocation() != null && labelOverlapAvoider.isVisible(v)
                && nv.getLabelVisible() && rect.contains(nv.getLabelShape(trans).getBounds())) {
            hitNodeLabels.add(v);
        }
        if (mustVisitSubTreeBelowNode(v) && hitsBBox(v, 100, 100, rect)) {
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                if (PhyloTreeUtils.okToDescendDownThisEdge(tree, f, v) && !isCollapsed(v)) {
                    getHitNodeLabelsRectangleRec(f.getTarget(), rect);
                }
            }
        }
    }


    /**
     * get all edges hit by mouse at (x,y)
     *
     * @param x
     * @param y
     * @return edges hits
     */
    public EdgeSet getHitEdges(int x, int y) {
        hitEdges.clear();
        // first take care of any nodes use internal points that have been moved:
        MagnifierUtil magnifierUtil = new MagnifierUtil(viewer);
        for (Edge f : edgesWithMovedInternalPoints) {
            final NodeView vv = viewer.getNV(tree.getSource(f));
            final NodeView wv = viewer.getNV(tree.getTarget(f));
            if (vv.getLocation() == null || wv.getLocation() == null)
                continue;
            magnifierUtil.addInternalPoints(f);
            final Point vp = vv.computeConnectPoint(wv.getLocation(), trans);
            final Point wp = wv.computeConnectPoint(vv.getLocation(), trans);
            boolean hit = viewer.getEV(f).hitEdge(vp, wp, trans, x, y, 4);
            magnifierUtil.removeAddedInternalPoints(f);
            if (hit) {
                hitEdges.add(f);
            }
        }

        Node v = tree.getRoot();
        if (v != null)
            getHitEdgesRec(v, x, y, magnifierUtil);
        return hitEdges;
    }

    /**
     * recursively do the work
     *
     * @param v
     * @param x
     * @param y
     */
    private void getHitEdgesRec(Node v, int x, int y, MagnifierUtil magnifierUtil) {
        if (mustVisitSubTreeBelowNode(v) && hitsBBox(v, 10, 10, x, y)) {
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                final NodeView vv = viewer.getNV(tree.getSource(f));
                final NodeView wv = viewer.getNV(tree.getTarget(f));
                if (vv.getLocation() == null || wv.getLocation() == null)
                    continue;
                magnifierUtil.addInternalPoints(f);
                final Point vp = vv.computeConnectPoint(wv.getLocation(), trans);
                final Point wp = wv.computeConnectPoint(vv.getLocation(), trans);
                boolean hit = viewer.getEV(f).hitEdge(vp, wp, trans, x, y, 4);
                magnifierUtil.removeAddedInternalPoints(f);
                if (hit) {
                    hitEdges.add(f);
                }
                if (PhyloTreeUtils.okToDescendDownThisEdge(tree, f, v) && !isCollapsed(v))
                    getHitEdgesRec(f.getTarget(), x, y, magnifierUtil);
            }
        }
    }

    /**
     * get all edge labels hit by mouse at (x,y)
     *
     * @param x
     * @param y
     * @return edge labels
     */
    public EdgeSet getHitEdgeLabels(int x, int y) {
        hitEdgeLabels.clear();
        // first take care of any labels that have been moved:
        for (Edge f : edgesWithMovedLabels) {
            if (viewer.getLabel(f) != null && viewer.getLabelVisible(f)
                    && viewer.getLabelRect(f) != null &&
                    viewer.getLabelRect(f).contains(x, y)) {
                hitEdgeLabels.add(f);
                return hitEdgeLabels;
            }
        }
        Node v = tree.getRoot();
        if (v != null)
            getHitEdgeLabelsRec(v, x, y);
        return hitEdgeLabels;
    }

    /**
     * recursively do the work
     *
     * @param v
     * @param x
     * @param y
     */
    private void getHitEdgeLabelsRec(Node v, int x, int y) {
        if (mustVisitSubTreeBelowNode(v) && hitsBBox(v, 100, 100, x, y)) {
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                if (viewer.getLabel(f) != null && viewer.getLabelVisible(f)
                        && viewer.getLabelRect(f) != null &&
                        viewer.getLabelRect(f).contains(x, y)) {
                    hitEdgeLabels.add(f);
                }
                if (PhyloTreeUtils.okToDescendDownThisEdge(tree, f, v) && !isCollapsed(v))
                    getHitEdgeLabelsRec(f.getTarget(), x, y);
            }
        }
    }

    /**
     * get all edges contained in rect
     *
     * @param rect
     * @return edges contained in rect
     */
    public EdgeSet getHitEdges(Rectangle rect) {
        hitEdges.clear();
        // first take care of any labels that have been moved:
        for (Edge f : edgesWithMovedLabels) {
            if (viewer.getLocation(f.getSource()) != null && viewer.getLocation(f.getTarget()) != null &&
                    rect.contains(trans.w2d(viewer.getLocation(f.getSource())))
                    && rect.contains(trans.w2d(viewer.getLocation(f.getTarget())))) {
                hitEdges.add(f);
            }
        }
        Node v = tree.getRoot();
        if (v != null)
            getHitEdgesRectangleRec(v, rect);
        return hitEdges;
    }

    /**
     * recursively do the work
     *
     * @param v
     * @param rect
     */
    private void getHitEdgesRectangleRec(Node v, Rectangle rect) {
        if (mustVisitSubTreeBelowNode(v) && hitsBBox(v, 100, 100, rect)) {
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                if (viewer.getLocation(f.getSource()) != null && viewer.getLocation(f.getTarget()) != null &&
                        rect.contains(trans.w2d(viewer.getLocation(f.getSource())))
                        && rect.contains(trans.w2d(viewer.getLocation(f.getTarget())))) {
                    hitEdges.add(f);
                }
                if (PhyloTreeUtils.okToDescendDownThisEdge(tree, f, v) && !isCollapsed(v))
                    getHitEdgesRectangleRec(f.getTarget(), rect);
            }
        }
    }

    /**
     * get all edge labels contained in rect
     *
     * @param rect
     * @return edges contained in rect
     */
    public EdgeSet getHitEdgeLabels(Rectangle rect) {
        hitEdgeLabels.clear();
        /*
       * If a list of moved labels is available, we must traverse it here.
       *
       *
       * */
        Node v = tree.getRoot();
        if (v != null)
            getHitEdgeLabelsRectangleRec(v, rect);
        return hitEdgeLabels;
    }

    /**
     * recursively do the work
     *
     * @param v
     * @param rect
     */
    private void getHitEdgeLabelsRectangleRec(Node v, Rectangle rect) {
        if (mustVisitSubTreeBelowNode(v) && hitsBBox(v, 100, 100, rect)) {
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                if (viewer.getLabel(f) != null && viewer.getLabelVisible(f)
                        && viewer.getLabelRect(f) != null &&
                        rect.contains(viewer.getLabelRect(f))) {
                    hitEdgeLabels.add(f);
                }
                if (PhyloTreeUtils.okToDescendDownThisEdge(tree, f, v) && !isCollapsed(v))
                    getHitEdgeLabelsRectangleRec(f.getTarget(), rect);
            }
        }
    }

    /**
     * show bounding-boxes
     *
     * @param showBoundingBoxes
     */
    public void setShowBoundingBoxes(boolean showBoundingBoxes) {
        this.showBoundingBoxes = showBoundingBoxes;
    }


    /**
     * gets the label overlap avoider
     *
     * @return label overlap avoider
     */
    public LabelOverlapAvoider getLabelOverlapAvoider() {
        return labelOverlapAvoider;
    }

    /**
     * set the default label positions for nodes and edges
     *
     * @param resetAll if true, reset positions for user-placed labels, too
     */
    public void resetLabelPositions(boolean resetAll) {
        nodesWithMovedLabels.clear();
        edgesWithMovedLabels.clear();
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            NodeView nv = viewer.getNV(v);
            if (nv.getLabelVisible() && nv.getLabel() != null && nv.getLabel().length() > 0
                    && (resetAll || nv.getLabelLayout() != NodeView.USER)) {
                nv.setLabelAngle(0);
                if (v.getDegree() == 1) {
                    final Edge e = v.getFirstAdjacentEdge();
                    final Node w = e.getOpposite(v);
                    final Point pV = trans.w2d(nv.getLocation());
                    Point pW = trans.w2d(viewer.getNV(w).getLocation());

                    java.util.List internalPoints = viewer.getInternalPoints(e);
                    if (internalPoints != null &&
                            internalPoints.size() != 0) {
                        Point2D internalPoint;

                        if (v == e.getSource())
                            internalPoint = (Point2D) internalPoints.get(0);
                        else
                            internalPoint = (Point2D) internalPoints.get(internalPoints.size() - 1);
                        Point pu = trans.w2d(internalPoint);
                        if (!pu.equals(pV))
                            pW = pu;
                    }

                    double angle = Geometry.moduloTwoPI(Geometry.computeAngle(Geometry.diff(pW, pV)));
                    if (angle > 1.75 * Math.PI)
                        viewer.getNV(v).setLabelLayout(NodeView.WEST);
                    else if (angle > 1.25 * Math.PI)
                        viewer.getNV(v).setLabelLayout(NodeView.SOUTH);
                    else if (angle > 0.75 * Math.PI)
                        viewer.getNV(v).setLabelLayout(NodeView.EAST);
                    else if (angle > 0.25 * Math.PI)
                        viewer.getNV(v).setLabelLayout(NodeView.NORTH);
                    else
                        viewer.getNV(v).setLabelLayout(NodeView.WEST);
                } else
                    viewer.getNV(v).setLabelLayout(NodeView.NORTHEAST);
            }
        }
        for (Edge e = tree.getFirstEdge(); e != null; e = e.getNext()) {
            EdgeView ev = viewer.getEV(e);
            if (resetAll || ev.getLabelLayout() != EdgeView.USER) {
                ev.setLabelLayout(EdgeView.CENTRAL);
                ev.setLabelAngle(0);
            }
        }
    }

    /**
     * to support bounding-box oriented drawers, report any node whose label has been interavtively moved
     *
     * @param v
     */
    public void setNodeHasMovedLabel(Node v) {
        nodesWithMovedLabels.add(v);
    }

    /**
     * to support bounding-box oriented drawers, report any edge whose label has been interavtively moved
     * will misuse this also to keep track of any edge-middle point that has been interactively moved
     *
     * @param e
     */
    public void setEdgesHasMovedLabel(Edge e) {
        edgesWithMovedLabels.add(e);
    }

    /**
     * to support bounding-box oriented drawers, report any edge whose label has been interavtively moved
     * will misuse this also to keep track of any edge-middle point that has been interactively moved
     *
     * @param e
     */
    public void setEdgesHasMovedInternalPoints(Edge e) {
        edgesWithMovedInternalPoints.add(e);
    }


    /**
     * get the set of collapsed nodes
     *
     * @return collapsed nodes
     */
    public NodeSet getCollapsedNodes() {
        return collapsedNodes;
    }

    /**
     * set the set of collapsed nodes
     */
    public void setCollapsedNodes(NodeSet collapsedNodes) {
        this.collapsedNodes = collapsedNodes;
    }

    /**
     * gets the bounding box of the tree in world coordinates
     *
     * @return bounding box
     */
    public Rectangle2D getBBox() {
        double[] bounds = new double[]{Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE, Double.MIN_VALUE};
        if (tree.getRoot() != null) {
            computeBBoxRec(tree.getRoot(), bounds);
        }
        return new Rectangle2D.Double(bounds[0], bounds[1], bounds[2] - bounds[0], bounds[3] - bounds[1]);
    }

    /**
     * recursively does the work
     *
     * @param v
     * @param bounds
     */
    private void computeBBoxRec(Node v, double[] bounds) {
        double x = viewer.getLocation(v).getX();
        double y = viewer.getLocation(v).getY();
        bounds[0] = Math.min(bounds[0], x);
        bounds[1] = Math.min(bounds[1], y);
        bounds[2] = Math.max(bounds[2], x);
        bounds[3] = Math.max(bounds[3], y);

        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            if (PhyloTreeUtils.okToDescendDownThisEdge(tree, f, v)) {
                computeBBoxRec(f.getTarget(), bounds);
            }
        }
    }

    /**
     * has this node been collapsed?
     *
     * @param v
     * @return true, if collapsed
     */
    public boolean isCollapsed(Node v) {
        return collapsedNodes != null && collapsedNodes.contains(v);
    }

    /**
     * rotate node labels to match edge directions?
     *
     * @return rotate?
     */
    public boolean getRadialLabels() {
        return radialLabels;
    }

    /**
     * rotate node labels to match edge directions?
     *
     * @param radialLabels
     */
    public void setRadialLabels(boolean radialLabels) {
        this.radialLabels = radialLabels;
    }

    /**
     * get all children in tree, or LSA tree of network
     *
     * @param v
     * @return all children
     */
    protected List<Node> getLSAChildren(Node v) {
        List<Node> targetNodes = null;
        if (node2LSAChildren != null)
            targetNodes = node2LSAChildren.get(v);
        List<Node> list = new LinkedList<>();

        if (targetNodes == null) {
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                if (!tree.isSpecial(e))
                    list.add(e.getTarget());
            }
        } else {
            for (Node w : targetNodes) {
                list.add(w);
            }
        }
        return list;
    }


    /**
     * compute the y-coordinates for the parallel view
     *
     * @param root
     * @return y-coordinates
     */
    public NodeDoubleArray computeYCoordinates(Node root) {
        NodeDoubleArray yCoord = new NodeDoubleArray(tree);
        computeYCoordinates(root, new LinkedList<Node>(), yCoord);
        return yCoord;
    }

    /**
     * compute the levels in the tree or network (max number of edges from node to a leaf)
     *
     * @param add
     * @return levels
     */
    protected NodeIntegerArray computeLevels(int add) {
        NodeIntegerArray levels = new NodeIntegerArray(tree, -1);
        computeLevelsRec(tree.getRoot(), levels, add, new HashSet<Node>());
        return levels;
    }

    /**
     * compute node levels
     *
     * @param v
     * @param levels
     * @param add
     * @return max height
     */
    private int computeLevelsRec(Node v, NodeIntegerArray levels, int add, Set<Node> path) {
        path.add(v);
        int level = 0;
        Set<Node> below = new HashSet<>();
        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            Node w = f.getTarget();
            below.add(w);
            if (levels.getValue(w) == -1)
                computeLevelsRec(w, levels, add, path);
            level = Math.max(level, levels.getValue(w) + (isTransferEdge(f) ? 0 : add));
        }
        Collection<Node> lsaChildren = tree.getNode2GuideTreeChildren().get(v);
        if (lsaChildren != null) {
            for (Node w : lsaChildren) {
                if (!below.contains(w) && !path.contains(w)) {
                    int levelW = levels.getValue(w);
                    if (levelW == -1)
                        computeLevelsRec(w, levels, add, path);
                    level = Math.max(level, levels.getValue(w) + add);
                }
            }
        }
        levels.set(v, level);
        path.remove(v);
        return level;
    }

    /**
     * compute the y-coordinates for the parallel view
     *
     * @param root
     * @param leafOrder
     * @param yCoord
     */
    public void computeYCoordinates(Node root, List<Node> leafOrder, NodeDoubleArray yCoord) {
        computeYCoordinateOfLeavesRec(root, 0, yCoord, leafOrder);
        if (tree.getSpecialEdges().size() > 0)
            fixSpacing(leafOrder, yCoord);
        computeYCoordinateOfInternalRec(root, yCoord);
    }

    /**
     * recursively compute the y coordinate for a parallel or triangular diagram
     *
     * @param v
     * @param leafNumber rank of leaf in vertical ordering
     * @return index of last leaf
     */
    private int computeYCoordinateOfLeavesRec(Node v, int leafNumber, NodeDoubleArray yCoord, List<Node> nodeOrder) {
        List<Node> list = getLSAChildren(v);

        if (list.size() == 0) {
            // String taxonName = tree.getLabel(v);
            yCoord.set(v, ++leafNumber);
            nodeOrder.add(v);
        } else {
            for (Node w : list) {
                leafNumber = computeYCoordinateOfLeavesRec(w, leafNumber, yCoord, nodeOrder);
            }
        }
        return leafNumber;
    }


    /**
     * recursively compute the y coordinate for the internal nodes of a parallel diagram
     *
     * @param v
     * @param yCoord
     */
    private void computeYCoordinateOfInternalRec(Node v, NodeDoubleArray yCoord) {
        if (v.getOutDegree() > 0) {
            double first = Double.MIN_VALUE;
            double last = Double.MIN_VALUE;

            for (Node w : getLSAChildren(v)) {
                double y = yCoord.getValue(w);
                if (y == 0) {
                    computeYCoordinateOfInternalRec(w, yCoord);
                    y = yCoord.getValue(w);
                }
                last = y;
                if (first == Double.MIN_VALUE)
                    first = last;
            }
            yCoord.set(v, 0.5 * (last + first));
        }
    }

    /**
     * fix spacing so that space between any two true leaves is 1
     *
     * @param leafOrder
     */
    private void fixSpacing(List<Node> leafOrder, NodeDoubleArray yCoord) {
        Node[] nodes = leafOrder.toArray(new Node[leafOrder.size()]);
        double leafPos = 0;
        for (int lastLeaf = -1; lastLeaf < nodes.length; ) {
            int nextLeaf = lastLeaf + 1;
            while (nextLeaf < nodes.length && !(nodes[nextLeaf].getOutDegree() == 0 || isCollapsed(nodes[nextLeaf])))
                nextLeaf++;
            // assign fractional positions to intermediate nodes
            int count = (nextLeaf - lastLeaf) - 1;
            if (count > 0) {
                double add = 1.0 / (count + 1); // if odd, use +2 to avoid the middle
                double value = leafPos;
                for (int i = lastLeaf + 1; i < nextLeaf; i++) {
                    value += add;
                    yCoord.set(nodes[i], value);
                }
            }
            // assign whole positions to actual leaves:
            if (nextLeaf < nodes.length) {
                yCoord.set(nodes[nextLeaf], ++leafPos);
            }
            lastLeaf = nextLeaf;
        }
    }

    public int getAuxilaryParameter() {
        return auxilaryParameter;
    }

    public void setAuxilaryParameter(int auxilaryParameter) {
        this.auxilaryParameter = auxilaryParameter;
    }

    /**
     * gets the longest edge length
     *
     * @return longest edge length
     */
    protected double getLongestEdgeLength() {
        double length = 0;
        Stack<Node> stack = new Stack<>();
        stack.push(tree.getRoot());
        while (stack.size() > 0) {
            Node v = stack.pop();
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                if (!tree.isSpecial(f))
                    length = Math.max(length, tree.getWeight(f));
                if (PhyloTreeUtils.okToDescendDownThisEdge(tree, f, v)) {
                    stack.push(f.getTarget());
                }
            }
        }
        return length;
    }

    /**
     * get the minimal number of leaves that a subtree has to contain before it is represented by a proxy
     *
     * @return min
     */
    public static int getMinLeavesForProxy() {
        return MIN_LEAVES_FOR_PROXY;
    }

    /**
     * set the minimal number of leaves that a subtree has to contain before it is represented by a proxy
     *
     * @param min
     */
    public static void setMinLeavesForProxy(int min) {
        TreeDrawerBase.MIN_LEAVES_FOR_PROXY = min;
    }

    /**
     * any tree edge and any reticulate edge with  length -1 will be drawn as a transfer edge
     *
     * @param e
     * @return
     */
    protected boolean isTransferEdge(Edge e) {
        return tree.isSpecial(e) && tree.getWeight(e) == -1;
    }

    /**
     * any tree edge and any reticulate edge with length 0 will be drawn as a reticulate edge
     *
     * @param e
     * @return
     */
    protected boolean isReticulateEdge(Edge e) {
        return tree.isSpecial(e) && tree.getWeight(e) == 0;
    }

    /**
     * recursively compute the x and y min and max values for all nodes below v
     *
     * @param v
     * @param xMinMax
     * @param yMinMax
     */
    protected void computeMinMaxRec(Node v, double[] xMinMax, double[] yMinMax) {
        if (v.getOutDegree() == 0) {
            double x = viewer.getLocation(v).getX();
            double y = viewer.getLocation(v).getY();
            if (x < xMinMax[0])
                xMinMax[0] = x;
            if (x > xMinMax[1])
                xMinMax[1] = x;
            if (y < yMinMax[0])
                yMinMax[0] = y;
            if (y > yMinMax[1])
                yMinMax[1] = y;
        } else {
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                if (PhyloTreeUtils.okToDescendDownThisEdge(tree, e, v)) {
                    computeMinMaxRec(e.getTarget(), xMinMax, yMinMax);
                }
            }
        }
    }

    /**
     * recursively collection the positions of all leaves below  v
     *
     * @param v
     * @param points
     */
    protected void computePointsRec(Node v, Collection<Point2D> points) {
        if (v.getOutDegree() == 0) {
            points.add(viewer.getLocation(v));
        } else {
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                if (PhyloTreeUtils.okToDescendDownThisEdge(tree, e, v)) {
                    computePointsRec(e.getTarget(), points);
                }
            }
        }
    }

    /**
     * get the order in which leaves are supposed to appear
     *
     * @return order or null
     */
    public List<String> getTaxonOrder() {
        return taxonOrder;
    }

    /**
     * set the order that leaves are supposed to appear, or null
     *
     * @param taxonOrder
     */
    public void setTaxonOrder(List<String> taxonOrder) {
        this.taxonOrder = taxonOrder;
    }

    public INodeDrawer getNodeDrawer() {
        return nodeDrawer;
    }

    public void setNodeDrawer(INodeDrawer nodeDrawer) {
        this.nodeDrawer = nodeDrawer;
    }
}

