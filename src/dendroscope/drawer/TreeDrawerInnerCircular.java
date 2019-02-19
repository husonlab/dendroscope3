/**
 * TreeDrawerInnerCircular.java 
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

import dendroscope.window.TreeViewer;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeIntegerArray;
import jloda.graphview.EdgeView;
import jloda.graphview.GraphView;
import jloda.graphview.NodeView;
import jloda.phylo.PhyloTree;
import jloda.util.Geometry;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * draws a tree using circle arc edges  with leaves in center
 * Daniel Huson, 1.2007
 */
public class TreeDrawerInnerCircular extends TreeDrawerRadial implements IOptimizedGraphDrawer {
    final static public String DESCRIPTION = "Draw (rooted) tree using circle segments, inside out";

    /**
     * constructor
     *
     * @param viewer
     * @param tree
     */
    public TreeDrawerInnerCircular(TreeViewer viewer, PhyloTree tree) {
        super(viewer, tree);
        setupGraphView(viewer);
        setAuxilaryParameter(50); // length of edges
    }

    /**
     * setd up the graphview
     *
     * @param graphView
     */
    public void setupGraphView(GraphView graphView) {
        graphView.setAllowInternalEdgePoints(false);
        graphView.setMaintainEdgeLengths(false);
        graphView.setAllowMoveNodes(false);
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
        NodeIntegerArray levels = computeLevels(getAuxilaryParameter());

        int maxLevel = levels.getValue(tree.getRoot());
        computeCoordinatesCladogramRec(tree.getRoot(), levels);

        // set root location:
        double angle = 0;
        for (Edge e = root.getFirstOutEdge(); e != null; e = root.getNextOutEdge(e)) {
            angle += node2AngleOfInEdge.getValue(e.getTarget());
        }
        if (root.getOutDegree() > 0)
            angle /= root.getOutDegree();
        node2AngleOfInEdge.set(root, angle);
        viewer.setLocation(root, Geometry.rotate(new Point2D.Double(maxLevel + 100, 0), angle));

        addInternalPoints();

        // setup optimization data structures:
        recomputeOptimization(null);


        // setup optimization datastructures:
        recomputeOptimization(null);
        return true;
    }

    /**
     * recursively compute node coordinates from edge angles:
     *
     * @param v Node
     */
    private void computeCoordinatesCladogramRec(Node v, NodeIntegerArray levels) {
        for (Node w : getLSAChildren(v)) {
            computeCoordinatesCladogramRec(w, levels);

            int level = levels.getValue(w) + 100;
            Point2D apt = new Point2D.Double(level, 0);
            viewer.setLocation(w, Geometry.rotate(apt, node2AngleOfInEdge.getValue(w)));
        }
    }

    /**
     * set the default label positions for nodes and edges
     *
     * @param resetAll if true, reset positions for user-placed labels, too
     */
    public void resetLabelPositions(boolean resetAll) {
        nodesWithMovedLabels.clear();
        edgesWithMovedLabels.clear();
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
                isLeaf = false;
                resetLabelPositionsRec(f.getOpposite(v), f, resetAll);
            }
        }
        NodeView nv = viewer.getNV(v);

        if (resetAll || nv.getLabelLayout() != NodeView.USER) {
            if (isLeaf) {
                Point2D location = nv.getLocation();
                Point2D refPoint = null;
                if (e != null) {
                    if (viewer.getInternalPoints(e) != null) {
                        if (v == e.getSource())
                            refPoint = viewer.getInternalPoints(e).get(0);
                        else // v==e.getTarget();
                            refPoint = viewer.getInternalPoints(e).get(viewer.getInternalPoints(e).size() - 1);
                    } else
                        refPoint = viewer.getNV(e.getOpposite(v)).getLocation();
                }
                if (refPoint == null || refPoint.distanceSq(location) < 0.000001)
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
}
