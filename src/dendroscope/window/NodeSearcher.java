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

import dendroscope.core.Director;
import dendroscope.core.Document;
import jloda.graph.Node;
import jloda.gui.find.IObjectSearcher;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Iterator;

class NodeSearcher implements IObjectSearcher {
    final private MultiViewer multiViewer;
    private TreeViewer[] viewers;
    public static final String SEARCHER_NAME = "Nodes";

    private Integer currentViewer;
    private Node currentNode;

    private TreeViewer foundViewer = null;
    private Node foundNode;

    /**
     * constructor
     *
     * @param multiViewer
     */
    public NodeSearcher(MultiViewer multiViewer) {
        this.multiViewer = multiViewer;
        updateViewers();
    }

    /**
     * update list of viewer
     */
    public void updateViewers() {
        viewers = new TreeViewer[multiViewer.getTreeGrid().getNumberOfPanels()];
        for (int i = 0; i < multiViewer.getTreeGrid().getNumberOfPanels(); i++) {
            viewers[i] = multiViewer.getTreeGrid().getViewerByRank(i);
        }

    }

    /**
     * goto the first object
     *
     * @return true, if successful
     */
    public boolean gotoFirst() {
        currentViewer = 0;
        if (isSomeViewerSelected()) {
            // find next selected viewer
            while (currentViewer < viewers.length) {
                if (multiViewer.getTreeGrid().isSelected(viewers[currentViewer]))
                    break;
                currentViewer++;
            }
        }
        if (currentViewer < viewers.length)
            currentNode = viewers[currentViewer].getGraph().getFirstNode();
        return isCurrentSet();
    }

    /**
     * goto the next object
     *
     * @return true, if successful
     */
    public boolean gotoNext() {
        if (!isCurrentSet())
            gotoFirst();
        else {
            currentNode = currentNode.getNext();
            if (currentNode == null && currentViewer < viewers.length - 1) {
                currentViewer++;
                if (isSomeViewerSelected()) {
                    // find next selected viewer
                    while (currentViewer < viewers.length) {
                        if (multiViewer.getTreeGrid().isSelected(viewers[currentViewer]))
                            break;
                        currentViewer++;
                    }
                }
                if (currentViewer < viewers.length)
                    currentNode = viewers[currentViewer].getGraph().getFirstNode();
            }
        }
        return isCurrentSet();
    }

    /**
     * goto the last object
     *
     * @return true, if successful
     */
    public boolean gotoLast() {
        currentViewer = viewers.length - 1;
        if (isSomeViewerSelected()) {
            // find next selected viewer
            while (currentViewer >= 0) {
                if (multiViewer.getTreeGrid().isSelected(viewers[currentViewer]))
                    break;
                currentViewer--;
            }
        }

        if (currentViewer >= 0)
            currentNode = viewers[currentViewer].getGraph().getLastNode();
        return isCurrentSet();
    }

    /**
     * goto the previous object
     *
     * @return true, if successful
     */
    public boolean gotoPrevious() {
        if (!isCurrentSet())
            gotoLast();
        else {
            currentNode = currentNode.getPrev();
            if (currentNode == null && currentViewer > 0) {
                currentViewer--;
                if (isSomeViewerSelected()) {
                    // find next selected viewer
                    while (currentViewer >= 0) {
                        if (multiViewer.getTreeGrid().isSelected(viewers[currentViewer]))
                            break;
                        currentViewer--;
                    }
                }
                if (currentViewer >= 0)
                    currentNode = viewers[currentViewer].getGraph().getLastNode();
            }
        }
        return isCurrentSet();
    }

    /**
     * is the current object set?
     *
     * @return true, if set
     */
    public boolean isCurrentSet() {
        return currentViewer != null && currentViewer >= 0 && currentViewer < viewers.length && currentNode != null && currentNode.getOwner() == viewers[currentViewer].getGraph();
    }

    /**
     * is the current object selected?
     *
     * @return true, if selected
     */
    public boolean isCurrentSelected() {
        return isCurrentSet() && viewers[currentViewer].getSelected(currentNode);
    }

    /**
     * set selection state of current object
     *
     * @param select
     */
    public void setCurrentSelected(boolean select) {
        if (isCurrentSet()) {
            viewers[currentViewer].setSelected(currentNode, select);
            viewers[currentViewer].repaint();
            if (select) {
                if (foundViewer != null && foundNode != null && foundViewer.getGraph() == foundNode.getOwner()) {
                    foundViewer.setFoundNode(null);
                    if (foundViewer != viewers[currentViewer])
                        foundViewer.repaint();
                }
                foundViewer = viewers[currentViewer];
                foundNode = currentNode;
                foundViewer.setFoundNode(foundNode);
            }
            viewers[currentViewer].repaint();
        }
    }

    /**
     * get the label of the current object
     *
     * @return label
     */
    public String getCurrentLabel() {
        if (isCurrentSet())
            return viewers[currentViewer].getLabel(currentNode);
        return null;
    }

    /**
     * set the label of the current object
     *
     * @param newLabel
     */
    public void setCurrentLabel(String newLabel) {
        if (isCurrentSet()) {
            TreeViewer viewer = viewers[currentViewer];
            viewer.setLabel(currentNode, newLabel);
            viewer.getPhyloTree().setLabel(currentNode, newLabel);
            Document doc = multiViewer.getDir().getDocument();
            Integer id = multiViewer.getTreeGrid().getNumberOfViewerInDocument(viewer);
            if (id != null)
                doc.getTree(id).syncViewer2Data(viewer, viewer.isDirty());

            viewer.setDirty(true);
        }
    }

    /**
     * how many objects are there?
     *
     * @return number of objects or -1
     */
    public int numberOfObjects() {
        int count = 0;
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            count += it.next().getGraph().getNumberOfNodes();
        }
        return count;
    }

    /**
     * get the name for this type of search
     *
     * @return name
     */
    public String getName() {
        return SEARCHER_NAME;
    }

    /**
     * is a global find possible?
     *
     * @return true, if there is at least one object
     */
    public boolean isGlobalFindable() {
        return true;
    }

    /**
     * is a selection find possible
     *
     * @return true, if at least one object is selected
     */
    public boolean isSelectionFindable() {
        return multiViewer.getTreeGrid().getSelectedNodesIterator().hasNext();
    }

    /**
     * something has been changed or selected, update view
     */
    public void updateView() {
        if (viewers != null && currentViewer != null) {
            TreeViewer viewer = viewers[currentViewer];
            if (viewer != null && currentNode != null) {
                final Point p = viewer.trans.w2d(viewer.getLocation(currentNode));
                viewer.scrollRectToVisible(new Rectangle(p.x - 60, p.y - 25, 120, 50));
            }
        }
        multiViewer.updateView(Director.ALL);
    }

    /**
     * does this searcher support find all?
     *
     * @return true, if find all supported
     */
    public boolean canFindAll() {
        return true;
    }

    /**
     * set select state of all objects
     *
     * @param select
     */
    public void selectAll(boolean select) {
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.selectAllNodes(select);
            treeViewer.repaint();
        }
    }

    /**
     * get the parent component
     *
     * @return parent
     */
    public Component getParent() {
        return multiViewer.getFrame();
    }

    private boolean isSomeViewerSelected() {
        return multiViewer.getTreeGrid().getNumberOfSelectedViewers() > 0;
    }

    public Collection<AbstractButton> getAdditionalButtons() {
        return null;
    }
}
