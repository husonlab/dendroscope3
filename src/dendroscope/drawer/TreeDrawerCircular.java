/*
 *   TreeDrawerCircular.java Copyright (C) 2023 Daniel H. Huson
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

import dendroscope.window.TreeViewer;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeIntArray;
import jloda.phylo.PhyloTree;
import jloda.swing.graphview.EdgeView;
import jloda.swing.graphview.GraphView;
import jloda.swing.graphview.NodeView;
import jloda.swing.util.Geometry;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * draws a tree using circle arc edges
 * Daniel Huson, 1.2007
 */
public class TreeDrawerCircular extends TreeDrawerRadial implements IOptimizedGraphDrawer {
    final static public String DESCRIPTION = "Draw (rooted) tree using circle segments";

    /**
     * constructor
     *
	 */
    public TreeDrawerCircular(TreeViewer viewer, PhyloTree tree) {
        super(viewer, tree);
        setupGraphView(viewer);
        setAuxilaryParameter(5); // percent offset in phylogram view of network
    }

    /**
     * setd up the graphview
     *
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
        if (toScale) {
            setCoordinatesPhylogram(root);
            addInternalPoints();
        } else {
            NodeIntArray levels = computeLevels(1);
            int maxLevel = levels.get(tree.getRoot());
            computeCoordinatesCladogramRec(tree.getRoot(), levels, maxLevel);
            addInternalPoints();
        }

        // setup optimization datastructures:
        recomputeOptimization(null);
        return true;
    }

    /**
     * assign equal angle coordinates
     *
	 */
    protected void setCoordinatesPhylogram(Node root) {
        boolean optionUseWeights = true;

        Point2D rootPt = viewer.getLocation(root);

        int percentOffset = getAuxilaryParameter();

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
                    double dist = rootPt.distance(vPt) + weight;
                    Point2D wPt = Geometry.translateByAngle(rootPt, node2AngleOfInEdge.get(w), dist);
                    viewer.setLocation(w, wPt);
                }
            } else if (w.getInDegree() > 1) // all in edges are 'blue' edges
            {
                double maxDistance = 0;
                for (Iterator it = w.inEdges().iterator(); ok && it.hasNext(); ) {
                    Node v = ((Edge) it.next()).getSource();
                    Point2D vPt = viewer.getLocation(v);
                    if (vPt == null) {
                        ok = false;
                    } else {
                        maxDistance = Math.max(maxDistance, rootPt.distance(vPt));
                    }
                }
                if (ok) {
                    double angle = w.getOutDegree() > 0 ? node2AngleOfInEdge.get(w.getFirstOutEdge().getTarget()) : 0;
                    Point2D wPt = Geometry.translateByAngle(rootPt, angle, maxDistance + smallDistance);
                    wPt = Geometry.translateByAngle(wPt, angle, smallDistance);
                    viewer.setLocation(w, wPt);
                }
            }

            if (ok)  // add childern to end of queue:
			{
				for (Edge edge : w.outEdges()) {
					queue.add(edge.getTarget());
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
    private void computeCoordinatesCladogramRec(Node v, NodeIntArray levels, int maxLevel) {
        for (Node w : tree.lsaChildren(v)) {
            computeCoordinatesCladogramRec(w, levels, maxLevel);

            int level = maxLevel - levels.get(w);
            Point2D apt = new Point2D.Double(level, 0);
            viewer.setLocation(w, Geometry.rotate(apt, node2AngleOfInEdge.get(w)));
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
        if (tree.getRoot() != null)
            resetLabelPositionsRec(tree.getRoot(), null, resetAll);
    }

    /**
     * recursively do the work
     *
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
        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            if (tree.okToDescendDownThisEdgeInTraversal(f, v)) {
                isLeaf = false;
                resetLabelPositionsRec(f.getOpposite(v), f, resetAll);
            }
        }
        NodeView nv = viewer.getNV(v);

        if (resetAll || nv.getLabelLayout() != NodeView.USER) {
            if (isLeaf) {
                Point2D location = nv.getLocation();
                Point2D refPoint = new Point2D.Double(0, 0);
                float angle = (float) (Geometry.computeAngle(Geometry.diff(location, refPoint)) + trans.getAngle());
                if (radialLabels) {
                    // angle+=0.01;
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
