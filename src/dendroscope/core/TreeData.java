/**
 * TreeData.java 
 * Copyright (C) 2015 Daniel H. Huson
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
package dendroscope.core;

import dendroscope.window.TreeViewer;
import jloda.graph.*;
import jloda.graphview.EdgeView;
import jloda.graphview.NodeView;
import jloda.graphview.Transform;
import jloda.phylo.PhyloTree;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * a tree and its properties
 */
public class TreeData extends PhyloTree {
    private String name;
    private NodeArray<NodeView> node2NodeView;
    private NodeSet collapsedNodes;
    private EdgeArray<EdgeView> edge2EdgeView;
    private String drawerKind;
    private boolean toScale = false;
    private boolean sparseLabels = true;
    private boolean radialLabels = false;
    private Transform trans;
    private NodeArray<List<Node>> node2LSAChildren;

    private boolean dirty;

    private boolean disabled;

    /**
     * constructor
     */
    public TreeData() {
        node2NodeView = null;
        edge2EdgeView = null;
        node2LSAChildren = null;
        dirty = false;
        disabled = false;
        name = "Untitled";
    }

    /**
     * constructor
     *
     * @param tree
     */
    public TreeData(PhyloTree tree) {
        this();
        setName(tree.getName());
        copy(tree);
    }

    /**
     * constructor for given name and tree
     *
     * @param name
     * @param tree
     */
    public TreeData(String name, PhyloTree tree) {
        this(tree);
        setName(name);
    }


    /**
     * copy all properties of tree in main viewer to this tree
     *
     * @param treeViewer
     * @param currentViewDirty
     */
    public void syncViewer2Data(TreeViewer treeViewer, boolean currentViewDirty) {
        if (treeViewer.getPhyloTree().getNumberOfNodes() > 0 && currentViewDirty)
            dirty = true; // once dirty, can't become un-dirty

        Document doc = treeViewer.getDoc();

        PhyloTree srcTree = treeViewer.getPhyloTree();
        NodeArray<Node> oldNode2NewNode = new NodeArray<Node>(srcTree);
        EdgeArray<Edge> oldEdge2NewEdge = new EdgeArray<Edge>(srcTree);

        PhyloTree targetTree = this;
        targetTree.clear();
        targetTree.copy(srcTree, oldNode2NewNode, oldEdge2NewEdge);

        // copy lsa information
        if (srcTree.getSpecialEdges().size() > 0) {
            setupLSA();
            for (Node v = srcTree.getFirstNode(); v != null; v = v.getNext()) {
                Node w = oldNode2NewNode.get(v);
                if (srcTree.getNode2GuideTreeChildren().get(v) != null) {
                    List<Node> children = new LinkedList<Node>();
                    for (Node u : srcTree.getNode2GuideTreeChildren().get(v)) {
                        children.add(oldNode2NewNode.get(u));
                    }
                    targetTree.getNode2GuideTreeChildren().set(w, children);
                } else
                    targetTree.getNode2GuideTreeChildren().set(w, null);
            }
        } else
            clearLSA();

        if (treeViewer.hasAdditional()) {
            if (!hasAdditional())
                setupAdditional();
            setDrawerKind(treeViewer.getDrawerKind());
            setToScale(treeViewer.isToScale());
            setRadialLabels(treeViewer.isRadialLabels());
            setSparseLabels(treeViewer.isSparseLabels());
            setName(doc.getName(doc.getCurrent()));
            trans.copy(treeViewer.trans);

            collapsedNodes.clear();
            for (Node v = srcTree.getFirstNode(); v != null; v = v.getNext()) {
                Node w = oldNode2NewNode.get(v);

                setNV(w, new NodeView(treeViewer.getNV(v)));
                if (treeViewer.getCollapsedNodes().contains(v))
                    collapsedNodes.add(w);
                targetTree.setLabel(w, treeViewer.getLabel(v));
            }

            for (Edge e = srcTree.getFirstEdge(); e != null; e = e.getNext()) {
                Edge f = oldEdge2NewEdge.get(e);
                setEV(f, new EdgeView(treeViewer.getEV(e)));
                targetTree.setLabel(f, treeViewer.getLabel(e));
            }
        } else {
            clearAdditional();
        }
    }

    /**
     * copy all properties of this tree to main viewer
     *
     * @param viewer
     */
    public void

    syncData2Viewer(Document doc, TreeViewer viewer) {
        doc.setName(getName(), doc.getCurrent());
        PhyloTree targetTree = viewer.getPhyloTree();

        PhyloTree srcTree = this;
        NodeArray<Node> oldNode2NewNode = new NodeArray<Node>(srcTree);
        EdgeArray<Edge> oldEdge2NewEdge = new EdgeArray<Edge>(srcTree);

        targetTree.clear();
        targetTree.copy(srcTree, oldNode2NewNode, oldEdge2NewEdge);

        // copy lsa information
        if (srcTree.getSpecialEdges().size() > 0) {
            for (Node v = srcTree.getFirstNode(); v != null; v = v.getNext()) {
                Node w = oldNode2NewNode.get(v);
                // todo: next line just for testing:
                //  targetTree.setLabel(w, srcTree.getLabel(v));

                if (srcTree.getNode2GuideTreeChildren().get(v) != null) {
                    List<Node> children = new LinkedList<Node>();
                    for (Node u : srcTree.getNode2GuideTreeChildren().get(v)) {
                        children.add(oldNode2NewNode.get(u));
                    }
                    targetTree.getNode2GuideTreeChildren().set(w, children);
                } else
                    targetTree.getNode2GuideTreeChildren().set(w, null);
            }
        }

        if (hasAdditional()) {
            viewer.setHasAdditional(true);
            if (getDrawerKind() != null)
                viewer.setDrawerKind(getDrawerKind());
            viewer.setHasCoordinates(true);
            viewer.setToScale(isToScale());
            viewer.setRadialLabels(isRadialLabels());
            viewer.setSparseLabels(isSparseLabels());
            if (viewer.getGraphDrawer() != null && viewer.getGraphDrawer().getLabelOverlapAvoider() != null)
                viewer.getGraphDrawer().getLabelOverlapAvoider().setEnabled(isSparseLabels());
            viewer.trans.copy(getTrans());

            viewer.getCollapsedNodes().clear();
            for (Node v = srcTree.getFirstNode(); v != null; v = v.getNext()) {
                Node w = oldNode2NewNode.get(v);
                viewer.getNV(w).copy(getNV(v));
                viewer.setLabel(w, srcTree.getLabel(v));
                if (getCollapsedNodes().contains(v))
                    viewer.getCollapsedNodes().add(w);
                if (srcTree.getNode2GuideTreeChildren().get(v) != null) {
                    List<Node> children = new LinkedList<Node>();
                    for (Node u : srcTree.getNode2GuideTreeChildren().get(v)) {
                        children.add(oldNode2NewNode.get(u));
                    }
                    targetTree.getNode2GuideTreeChildren().set(w, children);
                } else
                    targetTree.getNode2GuideTreeChildren().set(w, null);
            }
            for (Edge e = srcTree.getFirstEdge(); e != null; e = e.getNext()) {
                Edge f = oldEdge2NewEdge.get(e);
                viewer.getEV(f).copy(getEV(e));
                viewer.setLabel(f, srcTree.getLabel(e));
            }
        } else // nothing additional has been saved, just make  a straight copy of the labels
        {
            for (Node v = srcTree.getFirstNode(); v != null; v = v.getNext()) {
                Node w = oldNode2NewNode.get(v);
                viewer.setLabel(w, srcTree.getLabel(v));
            }
            for (Edge e = srcTree.getFirstEdge(); e != null; e = e.getNext()) {
                Edge f = oldEdge2NewEdge.get(e);
                if (!srcTree.isSpecial(e))
                    viewer.setLabel(f, srcTree.getLabel(e));
                else
                    viewer.setLabelColor(f, null);
            }
            viewer.resetViewSpecialEdges();
        }
        viewer.setDirty(isDirty());
    }

    /**
     * Clone this tree data
     *
     * @return a clone
     */
    public Object clone() {
        TreeData target = new TreeData();

        target.setName(getName());

        PhyloTree srcTree = this;
        NodeArray<Node> oldNode2NewNode = new NodeArray<Node>(srcTree);
        EdgeArray<Edge> oldEdge2NewEdge = new EdgeArray<Edge>(srcTree);

        target.copy(srcTree, oldNode2NewNode, oldEdge2NewEdge);

        // copy lsa information
        if (srcTree.getSpecialEdges().size() > 0 && !srcTree.getNode2GuideTreeChildren().isClear()) {
            target.setupLSA();
            for (Node v = srcTree.getFirstNode(); v != null; v = v.getNext()) {
                Node w = oldNode2NewNode.get(v);
                if (srcTree.getNode2GuideTreeChildren().get(v) != null) {
                    List<Node> children = new LinkedList<Node>();
                    for (Node u : srcTree.getNode2GuideTreeChildren().get(v)) {
                        children.add(oldNode2NewNode.get(u));
                    }
                    target.getNode2GuideTreeChildren().set(w, children);
                } else
                    target.getNode2GuideTreeChildren().set(w, null);
            }
        }

        if (hasAdditional()) {
            target.dirty = true;
            target.setupAdditional();
            target.setDrawerKind(getDrawerKind());
            target.setToScale(isToScale());
            target.setRadialLabels(isRadialLabels());
            target.setSparseLabels(isSparseLabels());
            target.trans.copy(getTrans());

            if (hasAdditional()) {
                target.getCollapsedNodes().clear();
                for (Node v = srcTree.getFirstNode(); v != null; v = v.getNext()) {
                    Node w = oldNode2NewNode.get(v);
                    target.node2NodeView.set(w, new NodeView(getNV(v)));
                    target.setLabel(w, srcTree.getLabel(v));
                    if (getCollapsedNodes().contains(v))
                        target.getCollapsedNodes().add(w);
                }
                for (Edge e = srcTree.getFirstEdge(); e != null; e = e.getNext()) {
                    Edge f = oldEdge2NewEdge.get(e);
                    target.edge2EdgeView.set(f, new EdgeView(getEV(e)));
                    target.setLabel(f, srcTree.getLabel(e));
                }
            }
        } else {
            for (Node v = srcTree.getFirstNode(); v != null; v = v.getNext()) {
                Node w = oldNode2NewNode.get(v);
                target.setLabel(w, srcTree.getLabel(v));
            }
            for (Edge e = srcTree.getFirstEdge(); e != null; e = e.getNext()) {
                Edge f = oldEdge2NewEdge.get(e);
                target.setLabel(f, srcTree.getLabel(e));
            }
        }
        return target;
    }

    /**
     * setup lsa data structure
     */
    public void setupLSA() {
        node2LSAChildren = new NodeArray<List<Node>>(this);
    }

    /**
     * clear lsa data structure
     */
    public void clearLSA() {
        node2LSAChildren = null;
    }

    /**
     * setup all additional data structures needed for a dirty tree
     */
    public void setupAdditional() {
        node2NodeView = new NodeArray<NodeView>(this);
        collapsedNodes = new NodeSet(this);
        edge2EdgeView = new EdgeArray<EdgeView>(this);
        trans = new Transform();
    }

    /**
     * clear all additional stuff
     */
    public void clearAdditional() {
        node2NodeView = null;
        node2LSAChildren = null;
        collapsedNodes = null;
        edge2EdgeView = null;
        trans = null;
    }

    /**
     * das this have additional data associated with it?
     *
     * @return true, if additional data set
     */
    public boolean hasAdditional() {
        return node2NodeView != null;
    }

    /**
     * get name of tree
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * set the name of the tree
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * is tree dirty
     *
     * @return dirty
     */
    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * get a node view
     *
     * @param v
     * @return node view
     */
    public NodeView getNV(Node v) {
        return node2NodeView.get(v);
    }

    /**
     * set a node view
     *
     * @param v
     * @param nv
     */
    public void setNV(Node v, NodeView nv) {
        node2NodeView.set(v, nv);

    }

    /**
     * get an edge view
     *
     * @param e
     * @return edge view
     */
    public EdgeView getEV(Edge e) {
        return edge2EdgeView.get(e);
    }

    /**
     * set an edge view
     *
     * @param e
     * @param ev
     */
    public void setEV(Edge e, EdgeView ev) {
        edge2EdgeView.set(e, ev);
    }

    /**
     * get the drawer kind
     *
     * @return kind
     */
    public String getDrawerKind() {
        return drawerKind;
    }

    /**
     * set the drawer kind
     *
     * @param drawerKind
     */
    public void setDrawerKind(String drawerKind) {
        this.drawerKind = drawerKind;
    }

    /**
     * is to scale mode
     *
     * @return to scale
     */
    public boolean isToScale() {
        return toScale;
    }

    /**
     * set the to scale mode
     *
     * @param toScale
     */
    public void setToScale(boolean toScale) {
        this.toScale = toScale;
    }

    /**
     * is to rotate mode
     *
     * @return rotate mode
     */
    public boolean isRadialLabels() {
        return radialLabels;
    }

    /**
     * set the rotate mode
     *
     * @param radialLabels
     */
    public void setRadialLabels(boolean radialLabels) {
        this.radialLabels = radialLabels;
    }

    public boolean isSparseLabels() {
        return sparseLabels;
    }

    public void setSparseLabels(boolean sparseLabels) {
        this.sparseLabels = sparseLabels;
    }

    /**
     * get the collapsed nodes
     *
     * @return collapsed nodes
     */
    public NodeSet getCollapsedNodes() {
        return collapsedNodes;
    }

    /**
     * set the collapsed nodes
     *
     * @param collapsedNodes
     */
    public void setCollapsedNodes(NodeSet collapsedNodes) {
        this.collapsedNodes = collapsedNodes;
    }

    /**
     * get the transform
     *
     * @return transform
     */
    public Transform getTrans() {
        return trans;
    }

    /**
     * sets the transform
     *
     * @param trans
     */
    public void setTrans(Transform trans) {
        this.trans.copy(trans);
    }

    /**
     * gets the name of this tree
     *
     * @return name
     */
    public String toString() {
        if (isDirty())
            return getName() + "*";
        else
            return getName();
    }


    /**
     * is tree disabled? if so, is ignored by get next or previus tree and also by all consensus computations
     *
     * @return true, if disabled
     */
    public boolean isEnabled() {
        return disabled;
    }

    /**
     * sets the disabled state
     *
     * @param disabled
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }


    /**
     * parse a tree in Newick format, as a rooted tree, if desired.
     *
     * @param str
     * @param rooted maintain root, even if it has degree 2
     * @throws java.io.IOException
     */
    public void parseBracketNotation(String str, boolean rooted, boolean doClear) throws IOException {
        super.parseBracketNotation(str, rooted, doClear);
        // clean all single quotes from taxon labels:
        boolean changed = false;
        for (Node v = getFirstNode(); v != null; v = v.getNext()) {
            String label = getLabel(v);
            if (label != null && label.contains("\'")) {
                label = label.replaceAll("\'", "-");
                setLabel(v, label);
                changed = true;
            }
        }
        if (changed)
            System.err.println("Warning: labels in tree contain single quotes, replaced by '-'");
    }


}
