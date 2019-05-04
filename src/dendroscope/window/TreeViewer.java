/**
 * TreeViewer.java 
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
package dendroscope.window;

import dendroscope.core.Director;
import dendroscope.core.Document;
import dendroscope.drawer.CollapsedShape;
import dendroscope.drawer.IOptimizedGraphDrawer;
import dendroscope.drawer.TreeDrawerAngled;
import dendroscope.drawer.TreeDrawerBase;
import jloda.graph.*;
import jloda.phylo.PhyloTree;
import jloda.phylo.PhyloTreeUtils;
import jloda.swing.director.IDirector;
import jloda.swing.find.SearchManager;
import jloda.swing.graphview.*;
import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.ProgramProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;

/**
 * Viewer for a single tree (or network)
 * Daniel Huson, 4.2010
 */
public class TreeViewer extends PhyloGraphView implements Comparable<TreeViewer> {
    final public Director dir;
    public static final String RECTANGULAR_PHYLOGRAM = "RectangularPhylogram";
    public static final String RECTANGULAR_CLADOGRAM = "RectangularCladogram";
    public static final String SLANTED_CLADOGRAM = "SlantedCladogram";
    public static final String RADIAL_PHYLOGRAM = "RadialPhylogram";
    public static final String RADIAL_CLADOGRAM = "RadialCladogram";
    public static final String CIRCULAR_PHYLOGRAM = "CircularPhylogram";
    public static final String CIRCULAR_CLADOGRAM = "CircularCladogram";
    public static final String INNERCIRCULAR_CLADOGRAM = "InnerCircularCladogram";
    final public static String LADDERIZE_LEFT = "left";
    final public static String LADDERIZE_RIGHT = "right";
    final public static String LADDERIZE_RANDOM = "random";
    final private Map<String, Integer> taxonName2Order = new HashMap<String, Integer>();

    protected final NodeSet collapsedNodes;
    protected final NodeArray<CollapsedShape> node2CollapsedShape;

    private boolean showScaleBar = false;
    private boolean toScale = false;
    private boolean radialLabels = false;
    private boolean sparseLabels = true;
    final public Document doc;
    public String drawerKind;

    private boolean dirty = false; // only relevant for multiviewer
    private boolean hasAdditional = false; // only relevant for multiviewer
    protected int radialAngle = 100;
    protected int innerCircularLength = 50;
    protected int phylogramPercentOffset = 4;
    private final boolean showEdgeWeights = false;
    private boolean unlockEdgeLengths = false;

    private boolean hasCoordinates = false;

    private List<String> taxonOrder = null;

    private final NodeImageManager nodeImageManager;

    /**
     * constructor
     *
     * @param graph
     * @param computeEmbedding
     */
    public TreeViewer(PhyloTree graph, boolean computeEmbedding, final Director dir) {
        super(graph, computeEmbedding);
        collapsedNodes = new NodeSet(getPhyloTree());
        node2CollapsedShape = new NodeArray<CollapsedShape>(graph);
        this.dir = dir;
        this.doc = dir.getDocument();

        setDefaultNodeBackgroundColor(Color.WHITE);

        setAllowEditNodeLabelsOnDoubleClick(false);
        setAllowEditEdgeLabelsOnDoubleClick(false);
        setRepaintOnGraphHasChanged(false);
        ((GraphViewListener) getGraphViewListener()).setAllowDeselectAllByMouseClick(false);
        setUseSplitSelectionModel(true);
        setDefaultNodeHeight(2);
        setDefaultNodeWidth(2);

        Font font = ProgramProperties.get(ProgramProperties.DEFAULT_FONT, (Font) null);
        if (font != null) {
            setDefaultNodeFont(font);
            setDefaultEdgeFont(font);
        }

        setDefaultNodeShape(NodeView.NONE_NODE);

        setGraphDrawer(null);
        setDrawerKind(RECTANGULAR_CLADOGRAM);

        canvasColor = Color.WHITE;

        setAllowEdit(false);
        setPOWEREDBY(null);

        getScrollPane().getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        getScrollPane().setWheelScrollingEnabled(true);
        getScrollPane().setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        addEdgeActionListener(new EdgeActionAdapter() {
            public void doNew(Edge e) {
                Node v = e.getTarget();
                if (v.getInDegree() >= 2) // if node now has two in-edges, make both special
                {
                    for (Edge f = v.getFirstInEdge(); f != null; f = v.getNextInEdge(f)) {
                        getPhyloTree().setSpecial(f, true);
                        setColor(f, Color.BLUE);
                        getPhyloTree().setWeight(f, 0);
                    }
                }
                if (getAllowEdit() && getGraphDrawer() instanceof IOptimizedGraphDrawer) {
                    IOptimizedGraphDrawer drawer = (IOptimizedGraphDrawer) getGraphDrawer();
                    drawer.recomputeOptimization(null);
                }
            }

            public void doDelete(Edge e) {
                Node v = e.getTarget();
                if (v.getInDegree() <= 2) // if node has two or less in-edges before deleting one, make not-special
                {
                    for (Edge f = v.getFirstInEdge(); f != null; f = v.getNextInEdge(f)) {
                        getPhyloTree().setSpecial(f, false);
                        getPhyloTree().setWeight(f, 1);
                        setColor(f, Color.BLACK);
                    }
                }
            }
        });
        this.nodeImageManager = new NodeImageManager(this);
    }

    /**
     * report currently selected nodes and edges
     */
    protected void reportSelected() {
        SearchManager searchManager = (SearchManager) dir.getViewerByClass(SearchManager.class);
        if (searchManager != null)
            searchManager.updateView(IDirector.ALL);

        if (getSelectedEdges().size() == 0 && getSelectedNodes().size() == 0)
            setToolTipText((String) null);
        else {
            int countLabeled = 0;
            for (Node v = getSelectedNodes().getFirstElement(); v != null; v = getSelectedNodes().getNextElement(v)) {
                if (getLabel(v) != null && getLabel(v).length() > 0)
                    countLabeled++;
            }
            setToolTipText("Selected: " + getSelectedNodes().size() + " node" + (getSelectedNodes().size() == 1 ? "" : "s")
                    + " (labeled: " + countLabeled + "), " + getSelectedEdges().size() + " edge" + (getSelectedEdges().size() == 1 ? "" : "s"));
        }
    }

    /**
     * recomputes the embedding of the graph
     *
     * @param rescale              fit graph to window?
     * @param recomputeCoordinates
     */
    public void recomputeEmbedding(boolean rescale, boolean recomputeCoordinates) {
        if (drawerKind.equals(SLANTED_CLADOGRAM)) {
            setToScale(false);
            setGraphDrawer(new TreeDrawerAngled(this, getPhyloTree()));
        } else if (drawerKind.equals(CIRCULAR_PHYLOGRAM)) {
            setToScale(true);
            setGraphDrawer(new dendroscope.drawer.TreeDrawerCircular(this, getPhyloTree()));
            getGraphDrawer().setAuxilaryParameter(phylogramPercentOffset);
        } else if (drawerKind.equals(CIRCULAR_CLADOGRAM)) {
            setToScale(false);
            setGraphDrawer(new dendroscope.drawer.TreeDrawerCircular(this, getPhyloTree()));
            getGraphDrawer().setAuxilaryParameter(radialAngle);
        } else if (drawerKind.equals(INNERCIRCULAR_CLADOGRAM)) {
            setToScale(false);
            setGraphDrawer(new dendroscope.drawer.TreeDrawerInnerCircular(this, getPhyloTree()));
            getGraphDrawer().setAuxilaryParameter(innerCircularLength);
        } else if (drawerKind.equals(RADIAL_PHYLOGRAM)) {
            setToScale(true);
            setGraphDrawer(new dendroscope.drawer.TreeDrawerRadial(this, getPhyloTree()));
            getGraphDrawer().setAuxilaryParameter(radialAngle);
        } else if (drawerKind.equals(RADIAL_CLADOGRAM)) {
            setToScale(false);
            setGraphDrawer(new dendroscope.drawer.TreeDrawerRadial(this, getPhyloTree()));
            getGraphDrawer().setAuxilaryParameter(radialAngle);
        } else if (drawerKind.equals(RECTANGULAR_PHYLOGRAM)) {
            setToScale(true);
            setGraphDrawer(new dendroscope.drawer.TreeDrawerParallel(this, getPhyloTree()));
            getGraphDrawer().setAuxilaryParameter(phylogramPercentOffset);
        } else // if (drawerKind.equals(RECTANGULAR_CLADOGRAM))
        {
            setToScale(false);
            setGraphDrawer(new dendroscope.drawer.TreeDrawerParallel(this, getPhyloTree()));
        }

        resetViews();
        ((TreeDrawerBase) getGraphDrawer()).setTaxonOrder(taxonOrder);
        getGraphDrawer().getLabelOverlapAvoider().setEnabled(isSparseLabels());
        getGraphDrawer().setCollapsedNodes(collapsedNodes);
        getGraphDrawer().setRadialLabels(radialLabels);
        getGraphDrawer().getLabelOverlapAvoider().setEnabled(isSparseLabels());

        if (recomputeCoordinates) {
            getGraphDrawer().computeEmbedding(isToScale());
            hasCoordinates = true;
        } else
            ((IOptimizedGraphDrawer) getGraphDrawer()).recomputeOptimization(null);

        trans.setCoordinateRect(getGraphDrawer().getBBox());
        resetLabelPositions(false);

        /*
        if (trans.getMagnifier().isActive()) {
            if (trans.getLockXYScale())
                trans.composeScale(5, 5);
            else
                trans.composeScale(1, 5);
        }
        */

        if (rescale)
            fitGraphToWindow();
        else
            centerGraph();

        repaint();
    }

    /**
     * paint the current tree
     *
     * @param gc
     */
    public void paint(Graphics gc) {
        if (isShowScaleBar() && (trans.getLockXYScale() || trans.getAngle() == 0 || trans.getAngle() == Math.PI))
            setDrawScaleBar(toScale);
        else
            setDrawScaleBar(false);
        super.paint(gc);
    }

    /**
     * zoom to selected nodes
     */
    public void zoomToSelection() {
        Rectangle2D worldRect = null;

        for (Node v : getSelectedNodes()) {
            if (getLocation(v) != null) {
                if (worldRect == null)
                    worldRect = new Rectangle2D.Double(getLocation(v).getX(), getLocation(v).getY(), 0, 0);
                else
                    worldRect.add(getLocation(v));
            }
        }
        if (worldRect != null) {
            worldRect.setRect(worldRect.getX(), worldRect.getY(), Math.max(150, worldRect.getWidth()),
                    Math.max(150, worldRect.getHeight()));
            trans.fitToSize(worldRect, getScrollPane().getViewport().getExtentSize());
            scrollRectToVisible(trans.w2d(worldRect).getBounds());
        }
    }

    /**
     * select all nodes below any of the currently selected nodes
     *
     * @param useSpecialEdges cross special edges
     */
    public void selectSubTree(boolean useSpecialEdges) {
        NodeSet seeds = (NodeSet) getSelectedNodes().clone();
        selectSubTreeRec(getPhyloTree().getRoot(), seeds, false, useSpecialEdges);
    }

    /**
     * recursively does the work
     *
     * @param v
     * @param seeds
     * @param select
     */
    private void selectSubTreeRec(Node v, NodeSet seeds, boolean select, boolean useSpecialEdges) {
        if (!select && seeds.contains(v))
            select = true;
        if (select)
            selectedNodes.add(v);

        if (!collapsedNodes.contains(v)) {
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                if (useSpecialEdges || !select || !getPhyloTree().isSpecial(f) || getPhyloTree().getWeight(f) > 0) {
                    if (select)
                        selectedEdges.add(f);
                    Node w = f.getTarget();
                    if (!select && getSelected(f)) {
                        selectedNodes.add(w);
                    }
                    selectSubTreeRec(w, seeds, select, useSpecialEdges);
                }
            }
        }
        fireDoSelect(selectedNodes);
        fireDoSelect(selectedEdges);
    }

    /**
     * recursively does the work
     *
     * @param v
     * @param select
     */
    public void selectSubTreeRec(Node v, boolean select, boolean useSpecialEdges) {
        if (!select)
            select = true;
        if (select)
            selectedNodes.add(v);

        if (!collapsedNodes.contains(v)) {
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                if (useSpecialEdges || !select || !getPhyloTree().isSpecial(f) || getPhyloTree().getWeight(f) > 0) {
                    if (select)
                        selectedEdges.add(f);
                    Node w = f.getTarget();
                    if (!select && getSelected(f)) {
                        selectedNodes.add(w);
                    }
                    selectSubTreeRec(w, select, useSpecialEdges);
                }
            }
        }
        fireDoSelect(selectedNodes);
        fireDoSelect(selectedEdges);
    }

    /**
     * select all nodes below any of the currently selected nodes
     */
    public void selectAllIntermediateNodes() {
        if (getPhyloTree().getRoot() != null) {
            Stack<Node> stack = new Stack<Node>();
            stack.push(getPhyloTree().getRoot());
            while (!stack.isEmpty()) {
                Node v = stack.pop();
                if (v.getOutDegree() > 0 && !collapsedNodes.contains(v)) {
                    if (v.getInDegree() == 1 && v.getOutDegree() == 1)
                        selectedNodes.add(v);
                    for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                        if (PhyloTreeUtils.okToDescendDownThisEdge(getPhyloTree(), e, v)) {
                            stack.push(e.getTarget());
                        }
                    }
                }
            }
        }
    }

    /**
     * set the selection state of all special edges
     *
     * @param select
     */
    public void selectAllSpecialEdges(boolean select) {
        if (getPhyloTree().getRoot() != null) {
            Stack<Node> stack = new Stack<Node>();
            stack.push(getPhyloTree().getRoot());
            while (stack.size() > 0) {
                Node v = stack.pop();
                if (!getCollapsedNodes().contains(v)) {
                    for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                        if (getPhyloTree().isSpecial(f) && getPhyloTree().getWeight(f) <= 0)
                            setSelected(f, select);
                        if (PhyloTreeUtils.okToDescendDownThisEdge(getPhyloTree(), f, v)) {
                            stack.push(f.getTarget());
                        }
                    }
                }
            }
        }
    }

    /**
     * select all non-terminal nodes and edges
     */
    public void selectNonTerminal() {
        Node root = getPhyloTree().getRoot();
        if (root != null && root.getDegree() > 0 && !getCollapsedNodes().contains(root)) {
            Stack<Node> stack = new Stack<Node>();
            stack.push(getPhyloTree().getRoot());
            while (stack.size() > 0) {
                Node v = stack.pop();
                setSelected(v, true);
                for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                    Node w = f.getTarget();
                    if (w.getOutDegree() > 0 && !getCollapsedNodes().contains(w)) {
                        setSelected(f, true);
                        if (PhyloTreeUtils.okToDescendDownThisEdge(getPhyloTree(), f, v)) {
                            stack.push(w);
                        }
                    }
                }
            }
        }

    }

    /**
     * swap subtree below each of the given nodes
     *
     * @param nodes
     * @return true, if a change was made
     */
    public boolean swapSubtree(NodeSet nodes) {
        Node root = ((PhyloTree) getGraph()).getRoot();
        if (root != null && getSelectedNodes().size() > 0) {
            swapSubtreeRec(root, 0, nodes);
            setDirty(true);
            return true;
        } else
            return false;
    }

    /**
     * recursively does the work
     *
     * @param v
     * @param found
     * @return number of nodes rotated
     */
    private int swapSubtreeRec(Node v, int found, NodeSet nodes) {
        if (nodes.contains(v)) {
            v.reverseOrderAdjacentEdges();
            if (getPhyloTree().getNode2GuideTreeChildren().get(v) != null) {
                getPhyloTree().getNode2GuideTreeChildren().put(v, Basic.reverseList(getPhyloTree().getNode2GuideTreeChildren().get(v)));
            }
            found++;
        }
        if (found < nodes.size()) {
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                if (PhyloTreeUtils.okToDescendDownThisEdge(getPhyloTree(), f, v)) {
                    found += swapSubtreeRec(f.getTarget(), found, nodes);
                }
            }
        }
        return found;
    }

    /**
     * rotate subtree below each of the given nodes
     *
     * @param nodes
     * @return true, if a change was made
     */
    public boolean rotateSubtree(NodeSet nodes) {
        Node root = ((PhyloTree) getGraph()).getRoot();
        if (root != null && nodes.size() > 0) {
            rotateSubtreeRec(root, 0, nodes);
            setDirty(true);
            return true;
        } else
            return false;
    }

    /**
     * recursively does the work
     *
     * @param v
     * @param found
     * @return number of nodes rotated
     */
    private int rotateSubtreeRec(Node v, int found, NodeSet nodes) {
        if (nodes.contains(v)) {
            v.rotateOrderAdjacentEdges();
            if (getPhyloTree().getNode2GuideTreeChildren().get(v) != null) {
                getPhyloTree().getNode2GuideTreeChildren().put(v, Basic.rotateList(getPhyloTree().getNode2GuideTreeChildren().get(v)));
            }

            found++;
        }
        if (found < nodes.size()) {
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                if (PhyloTreeUtils.okToDescendDownThisEdge(getPhyloTree(), f, v)) {
                    found += rotateSubtreeRec(f.getTarget(), found, nodes);
                }
            }
        }
        return found;
    }

    /**
     * collapse or uncollapse all selected nodes
     *
     * @param collapse
     * @param all      if set, uncollapses all nodes below a node
     * @return true, if change occurs
     */
    public boolean collapseSelectedNodes(boolean collapse, boolean all) {
        boolean changed = false;
        if (getSelectedNodes().size() > 0) {
            if (collapse && getPhyloTree().getSpecialEdges().size() > 0) { // check that this is collapsible: no special edges between a node below these nodes and the outside
                // label all nodes below:
                Set<Node> visited = new HashSet<Node>();
                Stack<Node> stack = new Stack<Node>();
                stack.addAll(getSelectedNodes());
                while (stack.size() > 0) {
                    Node v = stack.pop();
                    if (!visited.contains(v)) {
                        visited.add(v);
                        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                            Node w = e.getTarget();
                            if (!visited.contains(w)) {
                                stack.add(w);
                            }
                        }
                    }
                }
                for (Node v : visited) {
                    // all parent nodes must be all in or all out of the set of visited nodes:
                    boolean parentWasVisited = false;
                    boolean parentWasNotVisited = false;
                    for (Edge e = v.getFirstInEdge(); e != null; e = v.getNextInEdge(e)) {
                        if (visited.contains(e.getSource())) {
                            parentWasVisited = true;
                        } else {
                            parentWasNotVisited = true;
                        }
                    }
                    if (parentWasVisited && parentWasNotVisited) {
                        System.err.println("Can't collapse current selection because of obstructing reticulate edges");
                        return false;
                    }
                }
            }

            Stack<Node> stack = new Stack<Node>();
            stack.addAll(getSelectedNodes());
            while (stack.size() > 0) {
                Node v = stack.pop();
                if (collapsedNodes.contains(v) != collapse) {
                    if (collapse) {
                        if (collapseNode(v))
                            changed = true;
                    } else {
                        if (uncollapseNode(v))
                            changed = true;
                        if (!all) {
                            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                                if (!collapsedNodes.contains(e.getTarget())) {
                                    if (collapseNode(e.getTarget()))
                                        changed = true;
                                }
                            }
                        }
                    }
                }
                if (all) {
                    for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                        stack.push(e.getTarget());
                    }
                }
            }
        }
        if (getPhyloTree().getRoot() != null && collapsedNodes.contains(getPhyloTree().getRoot()))
            uncollapseNode(getPhyloTree().getRoot());
        return changed;
    }

    /**
     * collapse or uncollapse all  nodes
     *
     * @param collapse
     * @return true, if change occurs
     */
    public boolean collapseAllNodes(boolean collapse) {
        boolean changed = false;
        for (Node v = getGraph().getFirstNode(); v != null; v = v.getNext())
            if (collapse != collapsedNodes.contains(v)) {
                if (collapse) {
                    if (collapseNode(v))
                        changed = true;
                } else {
                    if (uncollapseNode(v))
                        changed = true;
                }
            }
        return changed;
    }

    /**
     * collapse or uncollapse all  nodes
     *
     * @return true, if change occurs
     */
    public boolean collapseNodesAtLevel(int level) {
        int oldNumber = collapsedNodes.size();
        if (getPhyloTree().getRoot() != null && level >= 0)
            collapseNodesAtLevelRec(getPhyloTree().getRoot(), 0, level);
        return collapsedNodes.size() != oldNumber;
    }

    /**
     * collapse all nodes at given level from root
     *
     * @param v
     * @param i
     * @param level
     */
    private void collapseNodesAtLevelRec(Node v, int i, int level) {
        if (i < level) {
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                if (PhyloTreeUtils.okToDescendDownThisEdge(getPhyloTree(), f, v)) {
                    collapseNodesAtLevelRec(f.getTarget(), i + 1, level);
                }
            }
        } else // must have i==level
            collapseNode(v);
    }

    /**
     * collapse complement of currently selected nodes
     *
     * @return true, if change occurs
     */
    public boolean collapseComplement() {
        NodeSet nodesToCollapse = new NodeSet(getPhyloTree());
        if (getPhyloTree().getRoot() != null) {
            EdgeSet selectedBelow = new EdgeSet(getPhyloTree());
            collapseComplementRec(getPhyloTree().getRoot(), selectedBelow, nodesToCollapse);
        }
        boolean result = nodesToCollapse.size() > 0;
        for (Node v : nodesToCollapse)
            collapseNode(v);
        return result;
    }

    /**
     * recursively do the work
     *
     * @param v
     * @return true, if selected node found below v
     */
    private boolean collapseComplementRec(Node v, EdgeSet selectedBelow, NodeSet nodesToCollapse) {
        // for each outedge, determine whether there is a selected node below:
        boolean selectedNodesBelow = false;
        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            if (PhyloTreeUtils.okToDescendDownThisEdge(getPhyloTree(), f, v)) {
                if (collapseComplementRec(f.getTarget(), selectedBelow, nodesToCollapse)) {
                    selectedNodesBelow = true;
                    selectedBelow.add(f);
                }
            }
        }
        if (selectedNodesBelow == false)  // none selected below
        {
            return getSelected(v);
        } else    // have some  selected below. collapse the others
        {
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                if (PhyloTreeUtils.okToDescendDownThisEdge(getPhyloTree(), f, v)) {
                    if (!selectedBelow.contains(f)) {
                        nodesToCollapse.add(f.getTarget());
                    }
                }
            }
            return true;
        }
    }

    /**
     * uncollapse all  nodes below the selected nodes
     *
     * @return true, if change occurs
     */
    public boolean uncollapseSelectedNodesSubtree() {
        int oldNumber = collapsedNodes.size();
        if (getPhyloTree().getRoot() != null)
            unCollapseNodesSubtreelRec(getPhyloTree().getRoot());
        return collapsedNodes.size() != oldNumber;
    }

    /**
     * collapse all nodes at given level from root
     *
     * @param v
     */
    private void unCollapseNodesSubtreelRec(Node v) {
        collapseNode(v);
        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            if (PhyloTreeUtils.okToDescendDownThisEdge(getPhyloTree(), f, v)) {
                Node w = f.getTarget();
                if (!getSelected(w)) // don't go below selected node, assume it will function as a seed later
                    unCollapseNodesSubtreelRec(f.getTarget());
            }
        }
    }

    /**
     * collapse a node
     *
     * @param v
     */
    public boolean collapseNode(Node v) {
        if (v.getInDegree() <= 1 && v.getOutDegree() > 0) // only collapse internal nodes and non-reticulate nodes
        {
            if (!collapsedNodes.contains(v)) {
                collapsedNodes.add(v);
                /*
              PhyloTree tree = getPhyloTree();

            if (tree.getLabel(v) == null || tree.getLabel(v).length() == 0) {
                tree.setLabel(v, PhyloTree.COLLAPSED_NODE_SUFFIX);
            } else
                tree.setLabel(v, tree.getLabel(v) + PhyloTree.COLLAPSED_NODE_SUFFIX);
                */
                return true;
            }
        }
        return false;
    }

    /**
     * uncollapse a node
     *
     * @param v
     */
    public boolean uncollapseNode(Node v) {
        if (collapsedNodes.contains(v)) {
            collapsedNodes.remove(v);
            /*
            PhyloTree tree = getPhyloTree();
            String label = tree.getLabel(v);
            if (label != null && label.endsWith(PhyloTree.COLLAPSED_NODE_SUFFIX)) {
                if (label.length() <= PhyloTree.COLLAPSED_NODE_SUFFIX.length())
                    tree.setLabel(v, null);
                else
                    tree.setLabel(v, label.substring(0, label.length() - PhyloTree.COLLAPSED_NODE_SUFFIX.length()));
            }
            */
            return true;
        } else
            return false;
    }

    /**
     * ladderize the tree
     *
     * @param ladderize can be left, right or none
     * @return true, if tree was changed
     */
    public boolean applyLadderize(String ladderize) {
        if (getPhyloTree().getRoot() == null)
            return false;

        boolean left;
        if (ladderize.equals(LADDERIZE_LEFT))
            left = true;
        else if (ladderize.equals(LADDERIZE_RIGHT))
            left = false;
        else if (ladderize.equals(LADDERIZE_RANDOM)) {
            ladderizeRandomRec(getPhyloTree().getRoot());
            return true;
        } else
            return false; // nothing to do
        NodeIntegerArray height = new NodeIntegerArray(getPhyloTree());
        ladderizeRec(getPhyloTree().getRoot(), left, height);
        return true;
    }

    /**
     * recursively does the work
     *
     * @param v
     * @param left
     * @param node2height
     */
    private void ladderizeRec(Node v, boolean left, NodeIntegerArray node2height) {
        if (collapsedNodes.contains(v) || v.getOutDegree() == 0) // leaf
        {
            node2height.set(v, 1);
        } else {
            int best = 0;
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                if (PhyloTreeUtils.okToDescendDownThisEdge(getPhyloTree(), f, v)) {
                    Node w = f.getTarget();
                    ladderizeRec(w, left, node2height);
                    best = Math.max(best, node2height.getValue(w));
                }
                node2height.set(v, best + 1);
            }
            List<Edge> newOrder = orderEdges(v, node2height, left);
            v.rearrangeAdjacentEdges(newOrder);
        }
    }

    /**
     * recursively does the work
     *
     * @param v
     */
    private void ladderizeRandomRec(Node v) {
        if (collapsedNodes.contains(v) || v.getOutDegree() == 1) // leaf
        {
        } else {
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                if (PhyloTreeUtils.okToDescendDownThisEdge(getPhyloTree(), f, v)) {
                    ladderizeRandomRec(f.getTarget());
                }
            }
            List<Edge> newOrder = new LinkedList<Edge>();
            for (Iterator<Edge> it = Basic.randomize(v.adjacentEdges().iterator(), v.getId()); it.hasNext(); )
                newOrder.add(it.next());

            v.rearrangeAdjacentEdges(newOrder);
        }
    }

    /**
     * order edges to ladderize
     *
     * @param v
     * @param node2height
     * @param left
     * @return ordered edges
     */
    private List<Edge> orderEdges(Node v, NodeIntegerArray node2height, boolean left) {
        SortedSet<Pair<Integer, Edge>> sorted = new TreeSet<Pair<Integer, Edge>>(new Pair<Integer, Edge>());

        for (Edge f = v.getFirstAdjacentEdge(); f != null; f = v.getNextAdjacentEdge(f)) {
            Node w = f.getTarget();
            sorted.add(new Pair<Integer, Edge>((left ? -1 : 1) * node2height.getValue(w), f));
        }
        List<Edge> result = new LinkedList<Edge>();
        for (Pair<Integer, Edge> pair : sorted) {
            result.add(pair.getSecond());
        }
        return result;
    }

    /**
     * gets the tree
     *
     * @return tree
     */
    public PhyloTree getPhyloTree() {
        return (PhyloTree) getGraph();
    }

    /**
     * gets the set of collapsed nodes
     *
     * @return collapsed nodes
     */
    public NodeSet getCollapsedNodes() {
        return collapsedNodes;
    }

    /**
     * find all uncollapsed nodes that has one of the given labels
     *
     * @param labels
     * @return nodes with one of the given labels
     */
    public NodeSet findLabeledNodes(Set labels) {
        PhyloTree tree = getPhyloTree();
        NodeSet nodes = new NodeSet(tree);
        NodeSet seen = new NodeSet(tree);
        if (getPhyloTree().getRoot() != null) {
            Stack<Node> stack = new Stack<Node>();
            stack.push(tree.getRoot());
            while (!stack.isEmpty()) {
                Node v = stack.pop();
                seen.add(v);
                String label = getLabel(v);
                if (label != null && labels.contains(label)) {
                    nodes.add(v);
                }
                if (!getCollapsedNodes().contains(v)) {
                    for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                        Node w = e.getTarget();
                        if (!seen.contains(w)) {
                            stack.push(w);
                        }
                    }
                }
            }
        }
        return nodes;
    }

    /**
     * resets the view for all special edges
     */
    public void resetViewSpecialEdges() {
        PhyloTree tree = getPhyloTree();
        for (Edge e = tree.getFirstEdge(); e != null; e = e.getNext()) {
            if (tree.isSpecial(e) && tree.getWeight(e) <= 0 && getColor(e) == GraphView.defaultEdgeView.getColor()) {
                setColor(e, Color.BLUE);
            }
            if (tree.isSpecial(e) && tree.getWeight(e) == -1) {
                setDirection(e, EdgeView.DIRECTED);
            }
            if (tree.getSpecialEdges().size() > 0 && ProgramProperties.get("scaleconfidence", false)) {
                //System.err.println("scaleconfidence e="+e+" weight: "+tree.getWeight(e));
                int width = tree.isSpecial(e) ? ((int) (10 * tree.getWeight(e))) : 10;
                setLineWidth(e, width);
                int value = (int) (255 - 25.5 * width);
                Color color = new Color(value, value, value);
                setColor(e, color);
            }
            // treeView.setDirection(e,EdgeView.DIRECTED);
        }

    }

    /**
     * select nodes by labels
     *
     * @param labels
     * @return true, if any changes made
     */
    public boolean selectNodesByLabels(Set labels, boolean select) {
        boolean changed = false;
        if (labels.size() > 0) {
            for (Node v = getGraph().getFirstNode(); v != null; v = v.getNext()) {
                if (getLabel(v) != null && getLabel(v).length() > 0) {
                    String label = getLabel(v);
                    int pos = label.indexOf(PhyloTree.COLLAPSED_NODE_SUFFIX);
                    if (pos > 0)
                        label = label.substring(0, pos);
                    if (labels.contains(label) && getSelected(v) != select) {
                        setSelected(v, select);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    /**
     * select all edges spanned by selected nodes
     */
    public void selectSpannedEdges(NodeSet nodes) {
        for (Edge e = getPhyloTree().getFirstEdge(); e != null; e = e.getNext()) {
            if (!getSelected(e) && nodes.contains(e.getSource()) && nodes.contains(e.getTarget()))
                setSelected(e, true);
        }
    }

    /**
     * gets the set of selected node labels that are not internal node labels that are numbers
     *
     * @return selected node labels
     */
    public Set<String> getSelectedNodeLabelsNotInternalNumbers() {
        Set<String> selectedLabels = new HashSet<>();
        for (Node v = getSelectedNodes().getFirstElement(); v != null; v = getSelectedNodes().getNextElement(v)) {
            String label = getPhyloTree().getLabel(v);
            if (label != null && label.length() > 0 && !(v.getOutDegree() > 0 && Basic.isDouble(label))) {
                selectedLabels.add(getPhyloGraph().getLabel(v));
            }
        }
        return selectedLabels;
    }

    /**
     * get the taxon name 2 order mapping
     *
     * @return mapping
     */
    public Map<String, Integer> getTaxonName2Order() {
        return taxonName2Order;
    }

    public boolean isShowScaleBar() {
        return showScaleBar;
    }

    public void setShowScaleBar(boolean showScaleBar) {
        this.showScaleBar = showScaleBar;
    }

    public boolean isToScale() {
        return toScale;
    }

    public void setToScale(boolean toScale) {
        this.toScale = toScale;
    }

    /**
     * rotate labels to match direction of edges?
     *
     * @param radialLabels
     */
    public void setRadialLabels(boolean radialLabels) {
        this.radialLabels = radialLabels;
    }

    /**
     * rotate labels to match direction of edges?
     *
     * @return whether to rotate labels or not
     */
    public boolean isRadialLabels() {
        return radialLabels;
    }

    public boolean isSparseLabels() {
        return sparseLabels;
    }

    public void setSparseLabels(boolean sparseLabels) {
        this.sparseLabels = sparseLabels;
    }

    /**
     * gets the document
     *
     * @return document
     */
    public Document getDoc() {
        return doc;
    }

    public String getDrawerKind() {
        return drawerKind;
    }

    public void setDrawerKind(String drawerKind) {
        this.drawerKind = drawerKind;
        ProgramProperties.put(ProgramProperties.DRAWERKIND, drawerKind);
    }

    /**
     * Fit graph to canvas.
     */
    public void fitGraphToWindow() {
        Dimension size = getScrollPane().getSize();
        if (size.getWidth() == 0 || size.getHeight() == 0) {
            try {

                Runnable runnable = new Runnable() {
                    public void run() {
                        TreeViewer.this.validate();
                        getScrollPane().validate();
                    }
                };
                if (SwingUtilities.isEventDispatchThread())
                    runnable.run(); // already in the swing thread, just run
                else
                    SwingUtilities.invokeAndWait(runnable);
            } catch (InterruptedException e) {
                Basic.caught(e);
            } catch (InvocationTargetException e) {
                Basic.caught(e);
            }
            size = getScrollPane().getSize();
        }
        if (getGraph().getNumberOfNodes() > 0) {
            // need more width than in GraphView
            trans.fitToSize(new Dimension(Math.max(100, size.width - 300), Math.max(50, size.height - 200)));
        } else
            trans.fitToSize(new Dimension(0, 0));
        centerGraph();
    }

    /**
     * tree is dirty
     *
     * @param dirty
     */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        if (!doc.isDocumentIsDirty())
            doc.setDocumentIsDirty(dirty);
        if (dirty)
            setHasAdditional(true);
    }

    /**
     * is tree dirty?
     *
     * @return true, if dirty
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * does this tree have additional attributes that were either set by the user or come from a file
     *
     * @return
     */
    public boolean hasAdditional() {
        return hasAdditional;
    }

    public void setHasAdditional(boolean hasAdditional) {
        this.hasAdditional = hasAdditional;
    }

    public int getRadialAngle() {
        return radialAngle;
    }

    public void setRadialAngle(int radialAngle) {
        this.radialAngle = radialAngle;
    }

    public int getInnerCircularLength() {
        return innerCircularLength;
    }

    public void setInnerCircularLength(int innerCircularLength) {
        this.innerCircularLength = innerCircularLength;
    }

    public int getPhylogramPercentOffset() {
        return phylogramPercentOffset;
    }

    public void setPhylogramPercentOffset(int phylogramPercentOffset) {
        this.phylogramPercentOffset = phylogramPercentOffset;
    }

    /**
     * show or hide labels of set of nodes
     *
     * @param nodes
     * @param show
     */
    public void showNodeLabels(NodeSet nodes, boolean show) {
        if (getPhyloTree().getRoot() != null) {
            Stack<Node> stack = new Stack<Node>();
            stack.push(getPhyloTree().getRoot());
            while (!stack.isEmpty()) {
                Node v = stack.pop();
                if (nodes == null || nodes.contains(v))
                    setLabelVisible(v, show);
                if (!collapsedNodes.contains(v)) {
                    for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                        if (PhyloTreeUtils.okToDescendDownThisEdge(getPhyloTree(), e, v)) {
                            stack.push(e.getTarget());
                        }
                    }
                }
            }
        }
    }

    /**
     * show or hide labels of set of edges
     *
     * @param edges
     * @param show
     */
    public void showEdgeLabels(EdgeSet edges, boolean show) {
        if (getPhyloTree().getRoot() != null) {
            Stack<Node> stack = new Stack<Node>();
            stack.push(getPhyloTree().getRoot());
            while (!stack.isEmpty()) {
                Node v = stack.pop();
                if (!collapsedNodes.contains(v)) {
                    for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                        if (edges == null || edges.contains(e))
                            setLabelVisible(e, show);

                        if (PhyloTreeUtils.okToDescendDownThisEdge(getPhyloTree(), e, v)) {
                            stack.push(e.getTarget());
                        }
                    }
                }
            }
        }
    }

    /**
     * gets the collapsed node shape
     *
     * @param v
     * @return collapsed node shape
     */
    public CollapsedShape getCollapsedShape(Node v) {
        return node2CollapsedShape.get(v);
    }

    public void setCollapsedShape(Node v, CollapsedShape collapsedShape) {
        node2CollapsedShape.put(v, collapsedShape);
    }

    public boolean getShowEdgeWeights() {
        return showEdgeWeights;
    }

    /**
     * set the label of edges to be the edge weights
     *
     * @param edges
     */
    public void setLabelsToWeights(EdgeSet edges) {
        final PhyloTree tree = getPhyloTree();
        if (tree.getRoot() != null) {
            Stack<Node> stack = new Stack<Node>();
            stack.push(tree.getRoot());
            while (!stack.isEmpty()) {
                Node v = stack.pop();
                if (!collapsedNodes.contains(v)) {
                    for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                        if (edges == null || edges.contains(e)) {
                            String label = "" + (float) tree.getWeight(e);
                            setLabel(e, label);
                            tree.setLabel(e, label);
                        }
                        if (PhyloTreeUtils.okToDescendDownThisEdge(tree, e, v)) {
                            stack.push(e.getTarget());
                        }
                    }
                }
            }
        }
    }

    /**
     * override allow move nodes
     *
     * @return true, if current selection of nodes can be moved
     */
    public boolean getAllowMoveNodes() {
        if (isUnlockEdgeLengths())
            return true;

        if (drawerKind.equals(RECTANGULAR_PHYLOGRAM) || drawerKind.equals(RECTANGULAR_CLADOGRAM)
                || drawerKind.equals(SLANTED_CLADOGRAM) || drawerKind.equals(CIRCULAR_CLADOGRAM)
                || drawerKind.equals(CIRCULAR_PHYLOGRAM) || drawerKind.equals(INNERCIRCULAR_CLADOGRAM)) {
            PhyloTree tree = getPhyloTree();
            if (tree.getSpecialEdges().size() == 0 || getSelectedNodes().size() == 0)
                return false;

            for (Iterator it = getSelectedNodes().iterator(); it.hasNext(); ) {
                Node v = (Node) it.next();
                for (Edge e = v.getFirstAdjacentEdge(); e != null; e = v.getNextAdjacentEdge(e)) {
                    if (!tree.isSpecial(e) && (!getSelected(e) || !getSelected(e.getOpposite(v))))
                        return false;
                }
            }
            return true;
        } else
            return super.getAllowMoveNodes();
    }

    public boolean isUnlockEdgeLengths() {
        return unlockEdgeLengths;
    }

    public void setUnlockEdgeLengths(boolean unlockEdgeLengths) {
        this.unlockEdgeLengths = unlockEdgeLengths;
    }

    /**
     * allow movement of the internal edge point of a special edge
     *
     * @return true, if selected edge is special
     */
    public boolean isAllowMoveInternalEdgePoints() {
        if (isUnlockEdgeLengths())
            return true;
        else return getSelectedEdges().size() == 1 &&
                (super.isAllowMoveInternalEdgePoints() || getPhyloTree().isSpecial(getSelectedEdges().getFirstElement()));
    }

    public boolean isAllowInternalEdgePoints() {
        if (isUnlockEdgeLengths())
            return true;
        else
            return super.isAllowInternalEdgePoints();
    }

    public boolean isHasCoordinates() {
        return hasCoordinates;
    }

    public void setHasCoordinates(boolean hasCoordinates) {
        this.hasCoordinates = hasCoordinates;
    }

    /**
     * remove all taxa that lie on selected nodes
     *
     * @return true, if tree has changed
     */
    public boolean removeTaxa(NodeSet toRemove) {
        boolean changed = false;
        PhyloTree tree = getPhyloTree();
        Node[] selected = toRemove.toArray(new Node[toRemove.size()]);
        List<Node> nakedLeaves = new LinkedList<Node>();

        for (Node v : selected) {
            if (v.getOwner() != null && tree.getLabel(v) != null) // not yet deleted
            {
                if (v.getOutDegree() > 0) {
                    tree.setLabel(v, null);
                } else {
                    for (Edge e = v.getFirstInEdge(); e != null; e = v.getNextInEdge(e)) {
                        Node w = e.getSource();
                        if (w.getOutDegree() == 1 && tree.getLabel(w) == null) {
                            nakedLeaves.add(w);
                        }
                    }
                    tree.deleteNode(v);
                }
                changed = true;
            }
        }

        while (nakedLeaves.size() > 0) {
            Node v = nakedLeaves.remove(0);
            for (Edge e = v.getFirstInEdge(); e != null; e = v.getNextInEdge(e)) {
                Node w = e.getSource();
                if (w.getOutDegree() == 1 && tree.getLabel(w) == null) {
                    nakedLeaves.add(w);
                }
            }
            tree.deleteNode(v);
        }

        List<Node> divertices = new LinkedList<Node>();
        for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
            if (v.getInDegree() == 1 && v.getOutDegree() == 1)
                divertices.add(v);
        }
        for (Node v : divertices) {
            tree.delDivertex(v);
        }

        if (tree.getRoot().getOwner() == null) {
            tree.setRoot((Node) null);
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                if (v.getInDegree() == 0)
                    tree.setRoot(v);
            }
        }
        return changed;
    }

    /**
     * computes a mapping of names to nodes
     *
     * @return names to node map
     */
    public Map<String, Node> computeName2NodeMap() {
        Map<String, Node> name2node = new HashMap<String, Node>();
        for (Node v = getPhyloTree().getFirstNode(); v != null; v = v.getNext()) {
            if (getPhyloTree().getLabel(v) != null)
                name2node.put(getPhyloTree().getLabel(v), v);
        }
        return name2node;
    }

    /**
     * taxon ordering to be used by embedder
     *
     * @param taxonOrdering
     */
    public void setTaxonOrdering(List<String> taxonOrdering) {
        this.taxonOrder = taxonOrdering;
    }


    /**
     * gets the node image manager
     *
     * @return node image manager
     */
    public NodeImageManager getNodeImageManager() {
        return nodeImageManager;
    }

    /**
     * compare relative to location in document
     *
     * @param treeViewer
     * @return comparison
     */
    public int compareTo(TreeViewer treeViewer) {
        int a = ((MultiViewer) dir.getMainViewer()).getTreeGrid().getNumberOfViewerInDocument(this);
        int b = ((MultiViewer) dir.getMainViewer()).getTreeGrid().getNumberOfViewerInDocument(treeViewer);
        if (a < b)
            return -1;
        else if (a > b)
            return 1;
        else return 0;
    }
}
