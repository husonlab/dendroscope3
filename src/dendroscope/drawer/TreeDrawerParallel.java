/*
 * TreeDrawerParallel.java Copyright (C) 2022 Daniel H. Huson
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

import dendroscope.window.TreeViewer;
import jloda.graph.*;
import jloda.phylo.PhyloTree;
import jloda.swing.graphview.EdgeView;
import jloda.swing.graphview.GraphView;
import jloda.swing.graphview.NodeView;
import jloda.swing.util.Geometry;
import jloda.swing.util.PolygonDouble;
import jloda.util.ProgramProperties;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.Stack;

/**
 * draws a tree using parallel edges
 * Daniel Huson 1.2007
 */
public class TreeDrawerParallel extends TreeDrawerBase implements IOptimizedGraphDrawer {
    final static public String DESCRIPTION = "Draw tree using parallel lines";

    /**
     * constructor
     *
	 */
    public TreeDrawerParallel(TreeViewer viewer, PhyloTree tree) {
        super(viewer, tree);
        setupGraphView(viewer);
        setAuxilaryParameter(5); // percent offset in phylogram view of network
    }

    /**
     * set up the graphview
     *
	 */
    public void setupGraphView(GraphView graphView) {
        graphView.setAllowInternalEdgePoints(false);

        graphView.setAllowMoveNodes(true);
        graphView.setMaintainEdgeLengths(false);

        graphView.setAllowMoveInternalEdgePoints(false);
        graphView.setKeepAspectRatio(false);
        graphView.setAllowRotationArbitraryAngle(false);
        trans.adjustAngleToNorthSouthEastWest();
        trans.getMagnifier().setInRectilinearMode(true);
    }


    /**
     * compute an embedding of the graph
     *
     * @param toScale if true, build to-scale embedding
     * @return true, if embedding was computed
     */
    public boolean computeEmbedding(boolean toScale) {
        this.toScale = toScale;
        if (tree.getNumberOfNodes() == 0)
            return true;

        viewer.removeAllInternalPoints();
        nodesWithMovedLabels.clear();
        edgesWithMovedLabels.clear();
        edgesWithMovedInternalPoints.clear();

        if (ProgramProperties.get("showids", false)) {
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                String label = viewer.getLabel(v);
                if (label != null) {
                    int p = label.indexOf("<");
                    if (p != -1)
                        label = label.substring(0, p);
                } else
                    label = "";
                viewer.setLabel(v, label + "<" + v.getId() + ">");
            }
        }

        Node root = tree.getRoot();
        if (root == null) {
            root = tree.getFirstNode();
            tree.setRoot(root);
        }
        node2bb.clear();
        node2ProxyShape.clear();
        if (root == null)
            return true;

        viewer.setLocation(root, 0, 0);

        try (NodeDoubleArray yCoord = computeYCoordinates(tree.getRoot())) {
            if (toScale) {
                setCoordinatesPhylogram(root, yCoord);
                addInternalPoints();
            } else {
                try (NodeIntArray levels = computeLevels(1)) {
                    computeCoordinatesCladogramRec(root, yCoord, levels);
                    addInternalPoints();
                }
            }
        }
        recomputeOptimization(null);
        return true;
    }

    /**
     * compute the optimization
     *
     * @param nodes if non-null, need only recompute for given nodes
     */
    public void recomputeOptimization(NodeSet nodes) {
        Node root = tree.getRoot();
        node2bb.clear();
        node2ProxyShape.clear();
        if (root != null)
            recomputeOptimizationRec(root, new NodeSet(tree));

        for (Node v : collapsedNodes) {
            viewer.setCollapsedShape(v, computeCollapsedShape(v));
        }
    }

    /**
     * recursively compute node coordinates from edge angles:
     *
     * @param v Node
     */
    private void computeCoordinatesCladogramRec(Node v, NodeDoubleArray yCoord, NodeIntArray levels) {
        viewer.setLocation(v, new Point2D.Double(-levels.get(v), yCoord.get(v)));
        for (Node w : tree.lsaChildren(v)) {
            computeCoordinatesCladogramRec(w, yCoord, levels);
        }
    }

    /**
     * add internal points to edges.
     */
    protected void addInternalPoints() {
        final Stack<Node> stack = new Stack<>();
        stack.push(tree.getRoot());
        while (stack.size() > 0) {
            final Node v = stack.pop();
            final Point2D vPt = viewer.getLocation(v);
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                Node w = f.getTarget();
                java.util.List<Point2D> pts = new LinkedList<>();
                pts.add(new Point2D.Double(vPt.getX(), viewer.getLocation(w).getY()));
                viewer.setInternalPoints(f, pts);

                if (tree.isTransferEdge(f))
					viewer.setShape(f, EdgeView.STRAIGHT_EDGE);
				else if (tree.isReticulateEdge(f) && tree.getWeight(f) <= 0)
					viewer.setShape(f, EdgeView.QUAD_EDGE);
				else // draw as curved edge
					viewer.setShape(f, EdgeView.POLY_EDGE);
				if (tree.okToDescendDownThisEdgeInTraversal(f, v) && !isCollapsed(w)) {
					stack.push(w);
				}
            }
        }
    }

    /**
     * assign rectangular phylogram coordinates. First must use cladogram code to set y coordinates!
     * This code assumes that all edges are directed away from the root.
     *
	 */
    private void setCoordinatesPhylogram(Node root, NodeDoubleArray yCoord) {
        var percentOffset = 50; //getAuxilaryParameter();

        var averageWeight = tree.edgeStream().mapToDouble(tree::getWeight).average().orElse(1);
        var smallOffsetForRecticulateEdge = (percentOffset / 100.0) * averageWeight;
        double rootHeight = yCoord.get(root);

        NodeSet assigned = new NodeSet(tree);

        // assign coordinates:
        java.util.List<Node> queue = new LinkedList<>();
        queue.add(root);
        while (queue.size() > 0) // breath-first assignment
        {
            var w = queue.remove(0); // pop

            boolean ok = true;
            if (w.getInDegree() == 1) // has regular in edge
            {
                Edge e = w.getFirstInEdge();
                Node v = e.getSource();
                Point2D location = viewer.getLocation(v);

                if (!assigned.contains(v)) // can't process yet
                {
                    ok = false;
                } else {
                    double height = yCoord.get(e.getTarget());
                    Node u = e.getTarget();
                    viewer.setLocation(u, location.getX() + tree.getWeight(e), height);
                    assigned.add(u);
                    java.util.List<Point2D> internalPoints = new LinkedList<>();
                    internalPoints.add(new Point2D.Double(location.getX(), yCoord.get(w)));
                    viewer.setInternalPoints(e, internalPoints);
                }
            } else if (w.getInDegree() > 1) // all in edges are 'blue' edges
            {
                double x = Double.NEGATIVE_INFINITY;
                for (Edge f = w.getFirstInEdge(); f != null; f = w.getNextInEdge(f)) {
                    Node u = f.getSource();
                    Point2D location = viewer.getLocation(u);
                    if (location == null) {
                        ok = false;
                    } else {
                        x = Math.max(x, location.getX());
                    }
                }
                if (ok && x > Double.NEGATIVE_INFINITY) {
                    x += smallOffsetForRecticulateEdge;
                    viewer.setLocation(w, x, yCoord.get(w));
                    assigned.add(w);
                    for (Edge f = w.getFirstInEdge(); f != null; f = w.getNextInEdge(f)) {
                        java.util.List<Point2D> internal = new LinkedList<>();
                        internal.add(new Point2D.Double(viewer.getLocation(f.getSource()).getX(), viewer.getLocation(w).getY()));
                        viewer.setInternalPoints(f, internal);
                        viewer.setShape(f, EdgeView.STRAIGHT_EDGE); // originally straight
                    }
                }
            } else  // is root node
            {
                viewer.setLocation(w, 0, rootHeight);
                assigned.add(w);
            }

            if (ok)  // add children to end of queue:
            {
                for (Edge f = w.getFirstOutEdge(); f != null; f = w.getNextOutEdge(f)) {
                    queue.add(f.getTarget());
                }
            } else  // process this node again later
                queue.add(w);
        }
    }

    /**
     * recursively compute optimization data
     *
     * @return number of leaves below the node
     */
    private int recomputeOptimizationRec(Node v, NodeSet visited) {
        final NodeView nv = viewer.getNV(v);
        final Point2D location = nv.getLocation();

        int leaves;
        if (v.getOutDegree() == 0) {
            final double d1 = 0.10;
            final double d2 = 0.05;
            Rectangle2D.Double bb = new Rectangle2D.Double(location.getX() - d2, location.getY() - d2, d1, d1);
            node2bb.put(v, bb);
            node2ProxyShape.put(v, null); // leaves have no proxies
            leaves = 1;
        } else {
            leaves = 0;
            double minX = Integer.MAX_VALUE;
            Point2D first = null;
            Point2D last;

            Rectangle2D bbox = null;
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                Node w = f.getTarget();

                if (!visited.contains(w))
                    leaves += recomputeOptimizationRec(w, visited); //todo: slight bug here, always need to increment the number of leaves
                else
                    leaves += 1;
                last = viewer.getLocation(w);
                if (first == null)
                    first = last;
                if (last.getX() < minX)
                    minX = last.getX();
                Rectangle2D bboxChild = node2bb.get(w);
                if (bboxChild != null) {
                    if (bbox == null)
                        bbox = (Rectangle2D) bboxChild.clone();
                    else
                        bbox.add(bboxChild);
                }
            }
            if (first != null) // always true
            {
                if (bbox != null)
                    bbox.add(location);
                else
                    bbox = new Rectangle2D.Double(location.getX(), location.getY(), 1, 1);
                node2bb.put(v, bbox);

                if (leaves >= MIN_LEAVES_FOR_PROXY)
                    node2ProxyShape.put(v, new PolygonDouble(bbox));
            }
        }
        return leaves;
    }

    /**
     * set the default label positions for nodes and edges
     *
	 */
    public void resetLabelPositions(boolean resetAll) {
        byte leafOr;
        byte rootOr;
        float labelAngle = 0;

        double angle = Geometry.moduloTwoPI(trans.getAngle());
        if (angle >= 0.25 * Math.PI && angle < 0.75 * Math.PI) // south
        {
            if (radialLabels) {
                leafOr = rootOr = NodeView.RADIAL;
                labelAngle = (float) Math.PI / 2;
            } else {
                leafOr = NodeView.SOUTH;
                rootOr = NodeView.NORTH;
                labelAngle = 0;
            }
        } else if (angle >= 0.75 * Math.PI && angle < 1.25 * Math.PI) // west
        {
            leafOr = (!trans.getFlipH() ? NodeView.WEST : NodeView.EAST);
            rootOr = (trans.getFlipH() ? NodeView.WEST : NodeView.EAST);
        } else if (angle >= 1.25 * Math.PI && angle < 1.75 * Math.PI) // north
        {
            if (radialLabels) {
                leafOr = rootOr = NodeView.RADIAL;
                labelAngle = (float) (1.5 * Math.PI);
            } else {
                leafOr = NodeView.NORTH;
                rootOr = NodeView.SOUTH;
                labelAngle = 0;
            }
        } else // east
        {
            leafOr = (!trans.getFlipH() ? NodeView.EAST : NodeView.WEST);
            rootOr = (trans.getFlipH() ? NodeView.EAST : NodeView.WEST);

        }

        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            NodeView nv = viewer.getNV(v);
            if (resetAll || nv.getLabelLayout() != NodeView.USER) {
                nv.setLabelPositionRelative(0, 0);
                if (v != tree.getRoot())
                    nv.setLabelLayout(leafOr);
                else
                    nv.setLabelLayout(rootOr);

                nv.setLabelAngle(labelAngle);
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
     * must we visit the subtree rooted at this node when drawing or looking for a mouse click?
     *
     * @return true, if we must look at subtree below v
     */
    protected boolean mustVisitSubTreeBelowNode(Node v) {
        if (v.getDegree() == 1) // is leaf or root
            return true;
        if (node2ProxyShape.get(v) == null)
            return true;
        if (isCollapsed(v))
            return false;
        Rectangle2D bbW = node2bb.get(v);
        Rectangle bbD = trans.w2d(bbW).getBounds();
        if (visibleRect != null && bbD.intersects(visibleRect) == false)
            return false; // not visible on screen
        if (bbD.height <= 2 && bbD.width < 2)
            return false;
        // divide height by number of children:
        int children = Math.max(v.getDegree() - 1, 1);
        return bbD.getBounds().height / children > 1;
    }

    /**
     * compute the shape used to represent a collapsed subtree
     *
	 */
    public CollapsedShape computeCollapsedShape(Node v) {
        double[] xMinMax = new double[]{Integer.MAX_VALUE, Integer.MIN_VALUE};
        double[] yMinMax = new double[]{Integer.MAX_VALUE, Integer.MIN_VALUE};
        computeMinMaxRec(v, xMinMax, yMinMax);

        Point2D[] points = new Point2D[]{new Point2D.Double(viewer.getLocation(v).getX(), yMinMax[0]),
                new Point2D.Double(xMinMax[0], yMinMax[0]),
                new Point2D.Double(xMinMax[1], yMinMax[1]),
                new Point2D.Double(viewer.getLocation(v).getX(), yMinMax[1])};
        return new CollapsedShape(points);
    }
}

