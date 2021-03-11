/*
 *   TreeDrawerRadial.java Copyright (C) 2020 Daniel H. Huson
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
package dendroscope.drawer;

import dendroscope.util.convexhull.GiftWrap;
import dendroscope.window.TreeViewer;
import jloda.graph.*;
import jloda.phylo.PhyloTree;
import jloda.phylo.PhyloTreeUtils;
import jloda.swing.graphview.EdgeView;
import jloda.swing.graphview.GraphView;
import jloda.swing.graphview.NodeView;
import jloda.swing.util.Geometry;
import jloda.swing.util.PolygonDouble;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;

/**
 * draws a tree in a radial layout
 * Daniel Huson, 1.2007
 */
public class TreeDrawerRadial extends TreeDrawerBase implements IOptimizedGraphDrawer {
    final static public String DESCRIPTION = "Draw (rooted) tree using radial layout";

    final NodeIntArray node2NumberLeaves; //
    final NodeDoubleArray node2AngleOfInEdge; // maps each node to the angle of it's (auxilary) in-edge
    boolean edge2AngleHasBeenSet = false; // need to check this before recomputing optimization


    /**
     * constructor
     *
     * @param viewer
     * @param tree
     */
    public TreeDrawerRadial(TreeViewer viewer, PhyloTree tree) {
        super(viewer, tree);
        node2NumberLeaves = new NodeIntArray(tree);
        node2AngleOfInEdge = new NodeDoubleArray(tree); // maps each node to the angle of it's (auxilary) in-edge

        setAuxilaryParameter(100);

        setupGraphView(viewer);
    }

    /**
     * setd up the graphview
     *
     * @param graphView
     */
    public void setupGraphView(GraphView graphView) {
        graphView.setAllowInternalEdgePoints(false);
        graphView.setMaintainEdgeLengths(true);
        graphView.setAllowMoveNodes(true);
        graphView.setAllowMoveInternalEdgePoints(false);
        graphView.setKeepAspectRatio(true);
        graphView.setAllowRotationArbitraryAngle(true);
        trans.getMagnifier().setInRectilinearMode(false);
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
        viewer.removeAllLocations();
        nodesWithMovedLabels.clear();
        edgesWithMovedLabels.clear();
        edgesWithMovedInternalPoints.clear();

        Node root = tree.getRoot();
        if (root == null) {
            tree.setRoot(tree.getFirstNode());
            root = tree.getRoot();
        }

        // assign angles to all edges
        computeAngles(root);

        // compute coordinates
        viewer.setLocation(root, new Point(0, 0));
        if (toScale) {
            setCoordinatesPhylogram(root);
        } else {
            NodeIntArray nodeHeights = computeLevels(1);
            computeCoordinatesCladogramRec(tree.getRoot(), nodeHeights, nodeHeights.get(tree.getRoot()));
        }

        // setup optimization datastructures:
        recomputeOptimization(null);

        return true;
    }

    /**
     * compute the angles for all edges
     *
     * @param root
     * @return number of leaves
     */
    protected int computeAngles(Node root) {
        final java.util.List<Node> leafOrder = new LinkedList<>();

        computeYCoordinates(tree.getRoot(), leafOrder, node2AngleOfInEdge);

        int leaves = 0;   // number of true leaves
        if (tree.getNumberSpecialEdges() > 0) {
            for (Node v : leafOrder) {
                if (v.getOutDegree() == 0)
                    leaves++;
            }
        } else leaves = leafOrder.size();

        double value;
        if (this instanceof TreeDrawerInnerCircular || (this instanceof TreeDrawerCircular && toScale))
            value = 100;      // in this case use auxilary parameter to determine relative edge length
        else
            value = getAuxilaryParameter();

        double factor = (value / 100.0) * 2 * Math.PI / (leaves);

        double offset = 2 * Math.PI - (value / 100.0) * Math.PI;

        for (Node v : leafOrder) {
            double alpha = factor * (node2AngleOfInEdge.get(v) - 1) + offset;
            node2AngleOfInEdge.put(v, alpha);
        }

        computeAnglesRec(root, node2AngleOfInEdge);

        edge2AngleHasBeenSet = true;
        return leaves;
    }

    /**
     * recursively compute the angles for all edges
     *
     * @param v
     */
    private java.util.List<Node> computeAnglesRec(Node v, NodeDoubleArray angle) {
        java.util.List<Node> leaves = new LinkedList<>();
        java.util.List<Node> childNodes = getLSAChildren(v);
        if (childNodes.size() > 0) {
            for (Node w : childNodes) {
                {
                    leaves.addAll(computeAnglesRec(w, angle));
                }
            }
            if (leaves.size() > 0) {
                double alpha = 0;
                for (Node u : leaves) {
                    alpha += angle.get(u);
                }
                angle.put(v, alpha / leaves.size());
            }
        } else {
            leaves.add(v);
        }
        return leaves;
    }

    /**
     * assign equal angle coordinates
     *
     * @param root
     */
    protected void setCoordinatesPhylogram(Node root) {
        boolean optionUseWeights = true;

        int percentOffset = 5;

        double smallDistance = 5.0 / 100.0;
        if (optionUseWeights) {
            double largestDistance = getLongestEdgeLength();
            smallDistance = (percentOffset / 100.0) * largestDistance;
        }

        // assign coordinates:
        java.util.List<Node> queue = new LinkedList<>();
        queue.add(root);

        while (queue.size() > 0) // breath-first assignment
        {
            Node w = queue.remove(0); // pop

            boolean ok = true;
            if (w.getInDegree() == 1) // has regular in edge
            {
                Edge e = w.getFirstInEdge();
                Node v = e.getSource();
                Point2D vPt = viewer.getLocation(v);

                if (vPt == null) // can't process yet
                {
                    ok = false;
                } else {
                    double weight = (optionUseWeights ? tree.getWeight(e) : 1);
                    double angle = node2AngleOfInEdge.get(w);
                    Point2D wPt = Geometry.translateByAngle(vPt, angle, weight);
                    viewer.setLocation(e.getTarget(), wPt);
                }
            } else if (w.getInDegree() > 1) // all in edges are 'blue' edges
            {
                Point2D rootPt = viewer.getLocation(tree.getRoot());
                double maxDistance = 0;
                double x = 0;
                double y = 0;
                int count = 0;
                for (Node v : w.parents()) {
                    Point2D vPt = viewer.getLocation(v);
                    if (vPt == null) {
                        ok = false;
                    } else {
                        maxDistance = Math.max(maxDistance, rootPt.distance(vPt));
                        x += vPt.getX();
                        y += vPt.getY();
                    }
                    count++;
                }
                if (ok) {
                    Point2D wPt = new Point2D.Double(x / count, y / count);
                    double dist = maxDistance - wPt.distance(rootPt) + smallDistance;
                    double angle = w.getOutDegree() > 0 ? node2AngleOfInEdge.get(w.getFirstOutEdge().getTarget()) : 0;
                    wPt = Geometry.translateByAngle(wPt, angle, dist);
                    viewer.setLocation(w, wPt);
                }
            }

            if (ok)  // add childern to end of queue:
            {
                for (Edge e : w.outEdges()) {
                    queue.add(e.getTarget());
                }
            } else  // process this node again later
                queue.add(w);
        }
    }

    /**
     * recursively compute node coordinates from edge angles:
     *
     * @param v Node
     */
    private void computeCoordinatesCladogramRec(Node v, NodeIntArray nodeHeight, int maxHeight) {

        java.util.List<Node> childNodes = getLSAChildren(v);

        for (Node w : childNodes) {
            computeCoordinatesCladogramRec(w, nodeHeight, maxHeight);

            Point2D apt = new Point2D.Double(maxHeight - nodeHeight.get(w), 0);
            viewer.setLocation(w, Geometry.rotate(apt, node2AngleOfInEdge.get(w)));
        }
    }


    /**
     * recompute all datastructures needed for optimized drawing
     *
     * @param nodes
     */
    public void recomputeOptimization(NodeSet nodes) {
        final Node root = tree.getRoot();

        if (edge2AngleHasBeenSet == false) // if we are recomputing for read-in data, not constructed data, need to set angles
            computeAngles(root);

        node2bb.clear();
        node2ProxyShape.clear();
        node2NumberLeaves.clear();

        Rectangle2D bbox = new Rectangle2D.Double(0, 0, 0, 0);
        for (Edge f = root.getFirstOutEdge(); f != null; f = root.getNextOutEdge(f)) {
            if (PhyloTreeUtils.okToDescendDownThisEdge(tree, f, root)) {
                Node w = f.getTarget();
                recomputeOptimizationRec(w);
                bbox.add(node2bb.get(w));
            }
            node2bb.put(root, bbox);
        }


        for (Node v : collapsedNodes) {
            viewer.setCollapsedShape(v, computeCollapsedShape(v));
        }
    }


    /**
     * recursively recompute datastructures used in optimization
     *
     * @param v Node
     * @return all subtree points including and below this node
     */
    private SubTreePoints recomputeOptimizationRec(Node v) {
        final NodeView nv = viewer.getNV(v);
        final Point2D vp = nv.getLocation();

        Rectangle2D bbox = new Rectangle2D.Double(vp.getX(), vp.getY(), 0, 0);
        SubTreePoints vSTP = new SubTreePoints();
        vSTP.enter = vp;

        int leaves = 0;
        if (v == tree.getRoot() || v.getDegree() > 1) {
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                Node w = f.getOpposite(v);
                Point2D wp = viewer.getLocation(w);
                if (vSTP.left == null)
                    vSTP.left = wp;
                vSTP.right = wp;
                bbox.add(wp);
                java.util.List list = viewer.getInternalPoints(f);
                if (list != null && list.size() > 0) {

                    Point2D p = (Point2D) list.get(list.size() - 1); // only last, for circular layout!
                    bbox.add(p);
                }

                // RECURSE:
                if (PhyloTreeUtils.okToDescendDownThisEdge(tree, f, v)) {
                    SubTreePoints wSTP = recomputeOptimizationRec(w);
                    leaves += node2NumberLeaves.get(w);

                    bbox.add(node2bb.get(w));

                    vSTP.addAll(wSTP);
                }
            }
            vSTP.reduceAndSort();
        } else // node is leaf
        {
            leaves = 1;
            vSTP.add(vp);
        }
        node2NumberLeaves.set(v, leaves);
        node2bb.put(v, bbox);

        if (v.getDegree() > 1) { // no proxy of root, leaves  or collapsed nodes
            if (leaves > MIN_LEAVES_FOR_PROXY && leaves <= tree.getNumberOfNodes() / 10) {
                node2ProxyShape.put(v, new PolygonDouble(vSTP.enter, vSTP.left, vSTP, vSTP.right));
            }
        }
        return vSTP;
    }

    /**
     * setup arc edges
     */
    protected void addInternalPoints() {
        final Stack<Node> stack = new Stack<>();
        stack.push(tree.getRoot());

        Point2D originPt = new Point2D.Double(0, 0);

        while (stack.size() > 0) {
            final Node v = stack.pop();
            final Point2D vPt = viewer.getLocation(v);
            final double height = originPt.distance(vPt);
            // add internal points to edges
            final double vAngle = node2AngleOfInEdge.get(v);
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                Node w = f.getTarget();
                if (!tree.isSpecial(f) || tree.getWeight(f) == 1) {
                    viewer.getEV(f).setShape(EdgeView.ARC_LINE_EDGE);
                    double wAngle = node2AngleOfInEdge.get(w);
                    java.util.List<Point2D> list = new LinkedList<>();
                    list.add(originPt);
                    Point2D aPt = Geometry.rotate(vPt, wAngle - vAngle);
                    list.add(aPt);
                    viewer.setInternalPoints(f, list);
                } else if (tree.isSpecial(f)) {
                    viewer.getEV(f).setShape(EdgeView.QUAD_EDGE);
                    double wAngle = node2AngleOfInEdge.get(w);
                    java.util.List<Point2D> list = new LinkedList<>();
                    Point2D aPt = Geometry.rotate(vPt, wAngle - vAngle);
                    list.add(aPt);
                    viewer.setInternalPoints(f, list);
                }
                if (PhyloTreeUtils.okToDescendDownThisEdge(tree, f, v)) {
                    stack.push(f.getTarget());
                }
            }
        }
    }

    /**
     * set of sub tree points used to build proxy shape
     */
    static class SubTreePoints extends java.util.LinkedList<Point2D> {
        Point2D enter;
        Point2D left;
        Point2D right;

        /**
         * reduce set of top nodes so that we sector angles round the enter point
         * and use the most distance leaf in each sector and sort them
         * todo: this won't work well if user moves edges around so as to change ordering around node
         */
        void reduceAndSort() {
            Point2D[] buckets = new Point2D[63]; // roughly (2PI * 10) buckets
            // determine best point for each bucket
            for (Point2D apt : this) {
                Point2D acpt = Geometry.diff(apt, enter);

                double angle = Geometry.moduloTwoPI(Geometry.computeAngle(acpt));
                int index = (int) (10 * angle);
                double acptDistanceSquared = acpt.getX() * acpt.getX() + acpt.getY() * acpt.getY();

                double bcptDistanceSquared = 0;
                Point2D bpt = buckets[index];

                if (bpt != null) {
                    Point2D bcpt = Geometry.diff(bpt, enter);
                    bcptDistanceSquared = bcpt.getX() * bcpt.getX() + bcpt.getY() * bcpt.getY();
                }
                if (acptDistanceSquared > bcptDistanceSquared)
                    buckets[index] = apt;
            }

            // find largest run of empty cells to position right, enter and left:
            int offset = 0;
            int bestRun = 0;
            int run = 0;
            for (int i = 0; i < 63; i++) {
                if (buckets[i] == null) {
                    run++;
                } else {
                    if (run > bestRun) {
                        bestRun = run;
                        offset = i - 1;
                    }
                    run = 0;
                }
            }
            if (run > bestRun) {
                offset = 0;
            }

            // copy best points to list
            clear();
            for (int i = 0; i < 63; i++) {
                Point2D apt = buckets[(i + offset) % 63];
                if (apt != null)
                    add(apt);
            }
        }
    }

    /**
     * must we visit the subtree rooted at this node when drawing or looking for a mouse click?
     *
     * @param v
     * @return true, if we must look at subtree below v
     */
    protected boolean mustVisitSubTreeBelowNode(Node v) {
        if (v.getDegree() == 1 || v == tree.getRoot()) // always true for root or leaf
            return true;
        if (node2ProxyShape.get(v) == null)
            return true;
        if (v.isAdjacent(tree.getRoot()))
            return true; // always draw nodes adjacent to root
        if (isCollapsed(v))
            return false;

        Rectangle2D bbW = node2bb.get(v);
        Rectangle bbD = trans.w2d(bbW).getBounds();
        if (visibleRect != null && bbD.intersects(visibleRect) == false)
            return false; // not visible on screen

        Integer numLeaves = node2NumberLeaves.get(v);
        return !(numLeaves != null && numLeaves > 10 * (Math.min(bbD.height, bbD.width)));
    }

    /**
     * set the default label positions for nodes and edges
     *
     * @param resetAll if true, reset positions for user-placed labels, too
     */
    public void resetLabelPositions(boolean resetAll) {
        nodesWithMovedLabels.clear();
        edgesWithMovedLabels.clear();
        if (tree.getRoot() != null)
            resetLabelPositionsRec(tree.getRoot(), null, resetAll);
    }

    /**
     * recursively do the work
     *
     * @param v
     * @param e
     * @param resetAll
     */
    private void resetLabelPositionsRec(Node v, Edge e, boolean resetAll) {
        if (e != null) {
            EdgeView ev = viewer.getEV(e);
            if (resetAll || ev.getLabelLayout() != EdgeView.USER) {
                ev.setLabelAngle(0);
                ev.setLabelLayout(EdgeView.CENTRAL);
            }
        }

        boolean isLeaf = true;
        if (!collapsedNodes.contains(v)) {
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                if (PhyloTreeUtils.okToDescendDownThisEdge(tree, f, v)) {
                    isLeaf = false;
                    resetLabelPositionsRec(f.getOpposite(v), f, resetAll);
                }
            }
        }
        NodeView nv = viewer.getNV(v);

        if (resetAll || nv.getLabelLayout() != NodeView.USER) {
            if (isLeaf) {
                Point2D location = nv.getLocation();
                Point2D refPoint = null;
                if (toScale && e != null) {
                    Edge f = e;
                    do {
                        Node u = f.getSource();
                        if (viewer.getInternalPoints(f) != null)
                            refPoint = viewer.getInternalPoints(f).get(viewer.getInternalPoints(f).size() - 1);
                        if (refPoint == null || viewer.getInternalPoints(f) == null || refPoint.distanceSq(location) <= 0.00000001)
                            refPoint = viewer.getNV(u).getLocation();
                        if (u.getInDegree() >= 1)
                            f = u.getFirstInEdge();  // backup along the tree,todo: use the co-alecsent node for reticulate nodes
                        else
                            f = null;
                    }
                    while (f != null && refPoint.distanceSq(location) <= 0.00000001);
                }
                if (refPoint == null)
                    refPoint = new Point2D.Double(0, 0);
                float angle = (float) (Geometry.computeAngle(Geometry.diff(location, refPoint)) + trans.getAngle());
                if (radialLabels) {
                    nv.setLabelAngle(angle);
                    nv.setLabelLayout(NodeView.RADIAL);
                    int d = Math.max(nv.getHeight(), nv.getWidth()) / 2 + 3;
                    nv.setLabelOffset(Geometry.rotate(new Point(d, 0), nv.getLabelAngle()));
                } else {
                    nv.setLabelLayoutFromAngle(angle);
                    nv.setLabelAngle(0);
                }
            } else {
                nv.setLabelAngle(0);
                nv.setLabelOffset(new Point(0, 0));
                nv.setLabelLayout(NodeView.CENTRAL);
            }
        }
    }

    /**
     * compute the shape used to represent a collapsed subtree
     *
     * @param v
     * @return
     */
    public CollapsedShape computeCollapsedShape(Node v) {
        java.util.List<Point2D> points = new LinkedList<>();
        computePointsRec(v, points);
        Point2D[] array;
        if (points.size() > 2) {
            Set<Point2D> hull = GiftWrap.apply2D(points);
            array = new Point2D[hull.size() + 1];
            int i = 0;
            array[i++] = viewer.getLocation(v);
            for (Point2D apt : points) {
                if (hull.contains(apt))
                    array[i++] = apt;
            }
            if (i < hull.size())
                System.err.println("Convex hull calculation failed");
            // array=hull.toArray(new Point2D[hull.size()]);
        } else {
            array = new Point2D[points.size() + 1];
            int i = 0;
            array[i++] = viewer.getLocation(v);
            for (Point2D apt : points) {
                array[i++] = apt;
            }
        }
        return new CollapsedShape(array);
    }
}
