/*
 *   TreeGrid.java Copyright (C) 2020 Daniel H. Huson
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
package dendroscope.window;

import dendroscope.core.Document;
import dendroscope.drawer.IOptimizedGraphDrawer;
import dendroscope.embed.LayoutOptimizerManager;
import jloda.graph.EdgeSet;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;
import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirector;
import jloda.swing.graphview.EdgeActionAdapter;
import jloda.swing.graphview.NodeActionAdapter;
import jloda.swing.graphview.PanelActionListener;
import jloda.swing.util.GraphViewPopupListener;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.NumberUtils;
import jloda.util.ProgramProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

/**
 * a grid of tree viewers
 * Daniel Huson, 4.2010
 */
public class TreeGrid extends JPanel {
    final private MultiViewer multiViewer;
    private TreeViewer[][] treeViewers;
    private final Map<TreeViewer, Integer> treeViewer2TreeId = new HashMap<>();

    private final Set<TreeViewer> selectedViewers = new HashSet<>();
    private int rows;
    private int cols;
    final private BitSet currentTrees = new BitSet();

    final private List<Connector> connectors = new LinkedList<>();

    private String nodePopupConfig = null;
    private String edgePopupConfig = null;
    private String panelPopupConfig = null;
    private CommandManager commandManager = null;

    private boolean showScrollBars = false;
    private boolean showBorders = true;
    private boolean inSelectNodes = false;

    final private JScrollBar mainScrollBar;
    private boolean avoidScrollBarBounce = false;

    /**
     * construct a 1x1 tree grid
     *
     * @param viewer0
     */
    public TreeGrid(MultiViewer viewer0) {
        this.multiViewer = viewer0;
        setGridSize(1, 1);
        // setOpaque(false);

        addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent ke) {
            }

            public void keyPressed(KeyEvent ke) {
                try {
                    if (ke.getKeyCode() == KeyEvent.VK_LEFT) {
                        if (!multiViewer.isAtFirst())
                            multiViewer.getDir().execute("go tree=prev;", multiViewer.getCommandManager());
                    } else if (ke.getKeyCode() == KeyEvent.VK_RIGHT) {
                        if (!multiViewer.isAtLast())
                            multiViewer.getDir().execute("go tree=next;", multiViewer.getCommandManager());
                    } else if (ke.getKeyCode() == KeyEvent.VK_UP) {
                        if (!multiViewer.isAtFirst())
                            multiViewer.getDir().execute("go tree=first;", multiViewer.getCommandManager());
                    } else if (ke.getKeyCode() == KeyEvent.VK_DOWN) {
                        if (!multiViewer.isAtLast())
                            multiViewer.getDir().execute("go tree=last;", multiViewer.getCommandManager());
                    }
                } catch (Exception e) {
                    Basic.caught(e);
                }
            }

            public void keyReleased(KeyEvent ke) {
            }
        });

        mainScrollBar = new JScrollBar(JScrollBar.VERTICAL);
        mainScrollBar.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent adjustmentEvent) {
                if (!avoidScrollBarBounce) {
                    if (!multiViewer.isLocked() && adjustmentEvent.getAdjustmentType() == AdjustmentEvent.TRACK
                            && !adjustmentEvent.getValueIsAdjusting()) {
                        int n = adjustmentEvent.getValue();
                        if (currentTrees.cardinality() > 0 && n != currentTrees.nextSetBit(0)) {
                            multiViewer.getDir().execute("!go tree=" + (n + 1) + ";", multiViewer.getCommandManager());
                        }
                    }
                }
            }
        });
    }

    public JScrollBar getMainScrollBar() {
        return mainScrollBar;
    }

    public void gridGeometryChanged() {
        for (Iterator<TreeViewer> it = getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            if (treeViewer.getWidth() < 300 || treeViewer.getHeight() < 200) {
                JScrollBar hScrollBar = treeViewer.getScrollPane().getHorizontalScrollBar();
                hScrollBar.setValue((hScrollBar.getMaximum() - hScrollBar.getModel().getExtent()) / 2);
                JScrollBar vScrollBar = treeViewer.getScrollPane().getVerticalScrollBar();
                vScrollBar.setValue((vScrollBar.getMaximum() - vScrollBar.getModel().getExtent()) / 2);
            }
        }

    }


    /**
     * set the size of the tree grid
     *
     * @param rows
     * @param cols
     */
    public void setGridSize(int rows, int cols) {
        if (rows < 1 || cols < 1)
            return;
        showScrollBars = (rows == 1 && cols == 1);
        showBorders = !showScrollBars;

        int first = Math.max(0, getCurrentTrees().nextSetBit(0)); // next available tree not already in the viewer

        Document doc = multiViewer.getDir().getDocument();

        this.rows = rows;
        this.cols = cols;
        treeViewers = new TreeViewer[rows][cols];

        selectedViewers.clear();
        removeAll();
        int panelWidth = Math.max(multiViewer.getFrame() != null ? multiViewer.getFrame().getSize().width / rows : 0, 400);
        int panelHeight = Math.max(multiViewer.getFrame() != null ? multiViewer.getFrame().getSize().height / cols : 0, 400);

        setLayout(new GridLayout(rows, cols));
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                final TreeViewer treeViewer = new TreeViewer(new PhyloTree(), false, multiViewer.getDir()) {
                    public void repaint() {
                        super.repaint();
                        // this ensures that connectors are repainted:
                        if (multiViewer.getTreeGrid() != null && multiViewer.getTreeGrid().getConnectors().size() > 0)
                            multiViewer.getTreeGrid().repaint();
                    }
                    /*
                   public void setSize (int width,int height) {
                        System.err.println("Setting viewer size to: "+width+", "+height);
                        super.setSize(width,height);
                    }
                    */
                };
                treeViewer.setSparseLabels(ProgramProperties.get("OpenWithSparseLabels", true));
                // this code ensures that keys work for individual panes in a tree grid:
                treeViewer.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                    }

                    public void mouseExited(MouseEvent e) {
                    }
                });

                treeViewers[i][j] = treeViewer;
                ((JButton) treeViewer.getScrollPane().getCorner(JScrollPane.LOWER_RIGHT_CORNER)).setIcon(ResourceManager.getIcon("sun/AlignCenter16.gif"));

                if (commandManager != null)
                    treeViewer.setPopupListener(new GraphViewPopupListener(treeViewer, nodePopupConfig, edgePopupConfig, panelPopupConfig, commandManager));

                treeViewer.addPanelActionListener(new PanelActionListener() {
                    public void doMouseClicked(MouseEvent mouseEvent) {
                        if (multiViewer.isLocked())
                            return;

                        final HashSet<TreeViewer> wasCurrentlySelected = new HashSet<>();
                        wasCurrentlySelected.addAll(selectedViewers);

                        if (mouseEvent.getClickCount() == 1 || mouseEvent.getClickCount() == 2) {
                            if (!mouseEvent.isShiftDown()) {
                                selectAllPanels(false);
                                setSelected(treeViewer, true);
                            } else if (!wasCurrentlySelected.contains(treeViewer))
                                setSelected(treeViewer, true);

                            if (mouseEvent.getClickCount() == 2) {
                                boolean changed = false;
                                if (wasCurrentlySelected.contains(treeViewer)) {
                                    if (treeViewer.getSelectedNodes().size() < treeViewer.getGraph().getNumberOfNodes()) {
                                        treeViewer.selectAllNodes(true);
                                        changed = true;
                                    }
                                    if (!changed) {
                                        if (treeViewer.getSelectedEdges().size() < treeViewer.getGraph().getNumberOfEdges()) {
                                            treeViewer.selectAllEdges(true);
                                            changed = true;
                                        }
                                    }
                                    if (changed) {
                                        treeViewer.repaint();
                                        multiViewer.updateView(IDirector.ENABLE_STATE);
                                    }
                                }
                                if (!changed && !mouseEvent.isShiftDown()) {
                                    for (TreeViewer aViewer : treeViewer2TreeId.keySet()) {
                                        if (aViewer != treeViewer) {
                                            if (wasCurrentlySelected.contains(aViewer)) {
                                                setSelected(aViewer, false);
                                            } else {
                                                changed = false;
                                                if (aViewer.getSelectedNodes().size() > 0) {
                                                    aViewer.selectAllNodes(false);
                                                    changed = true;
                                                }
                                                if (aViewer.getSelectedEdges().size() > 0) {
                                                    aViewer.selectAllEdges(false);
                                                    changed = true;
                                                }
                                                if (changed) {
                                                    aViewer.repaint();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            multiViewer.updateView(IDirector.ENABLE_STATE);
                        }
                    }
                });
                treeViewer.addNodeActionListener(new NodeActionAdapter() {
                    public void doSelect(NodeSet nodes) {
                        if (!isSelected(treeViewer)) {
                            // setSelected(treeViewer, true);
                        }
                        // if (!multiViewer.isLocked())
                        {
                            if (!inSelectNodes) // prevent bouncing
                            {
                                inSelectNodes = true;
                                if (getNumberOfSelectedViewers() > 1) {
                                    Set<String> labels = new HashSet<>();
                                    for (Node a : nodes) {
                                        String label = treeViewer.getLabel(a);
                                        if (label != null)
                                            labels.add(label);
                                    }
                                    for (Iterator<TreeViewer> it = getSelectedIterator(); it.hasNext(); ) {
                                        TreeViewer aViewer = it.next();
                                        if (aViewer != treeViewer) {
                                            if (aViewer.selectNodesByLabels(labels, true))
                                                aViewer.repaint();
                                        }
                                    }
                                }
                                inSelectNodes = false;
                            }
                            multiViewer.updateView(IDirector.ENABLE_STATE);
                        }
                        // reportSelected();
                    }

                    public void doDeselect(NodeSet nodes) {
                        if (!multiViewer.isLocked()) {
                            if (!inSelectNodes) // prevent bouncing
                            {
                                inSelectNodes = true;
                                if (getNumberOfSelectedViewers() > 1) {
                                    Set<String> labels = new HashSet<>();
                                    for (Node a : nodes) {
                                        String label = treeViewer.getLabel(a);
                                        if (label != null)
                                            labels.add(label);
                                    }
                                    for (Iterator<TreeViewer> it = getSelectedIterator(); it.hasNext(); ) {
                                        TreeViewer aViewer = it.next();
                                        if (aViewer != treeViewer) {
                                            if (aViewer.selectNodesByLabels(labels, false))
                                                aViewer.repaint();
                                        }
                                    }
                                }
                                inSelectNodes = false;
                            }
                            multiViewer.updateView(IDirector.ENABLE_STATE);
                        }
                        // reportSelected();
                    }

                    public void doClick(NodeSet nodes, int clicks) {
                        if (!isSelected(treeViewer)) {
                            setSelected(treeViewer, true);
                        }
                        if (!multiViewer.isLocked()) {
                            if (clicks == 1) {
                                //setSelected(nodes, true);
                                // do nothing, as this is done in GraphViewListener
                            }
                            if (clicks == 2) {
                                multiViewer.getDir().execute("select subpart;", multiViewer.getCommandManager());
                            } else if (clicks == 3) {
                                if (treeViewer.getPhyloTree().getNumberSpecialEdges() > 0)
                                    multiViewer.getDir().execute("select subnetwork;", multiViewer.getCommandManager());
                                else
                                    multiViewer.getDir().execute("select subpart;select invert;", multiViewer.getCommandManager());
                            } else if (clicks == 4) {
                                if (treeViewer.getPhyloTree().getNumberSpecialEdges() > 0)
                                    multiViewer.getDir().execute("select subnetwork;select invert;", multiViewer.getCommandManager());
                            }
                        }
                    }

                    public void doClickLabel(NodeSet nodes, int clicks) {
                        this.doClick(nodes, clicks);
                    }

                    public void doNodesMoved() {
                        ((IOptimizedGraphDrawer) treeViewer.getGraphDrawer()).recomputeOptimization(null);
                        if (!treeViewer.isDirty()) {
                            treeViewer.setDirty(true);
                            updateName(treeViewer);
                        }
                    }

                    public void doMoveLabel(NodeSet nodes) {
                        if (!treeViewer.isDirty()) {
                            treeViewer.setDirty(true);
                            updateName(treeViewer);
                        }
                    }
                });
                treeViewer.addEdgeActionListener(new EdgeActionAdapter() {
                    public void doSelect(EdgeSet edges) {
                        if (!isSelected(treeViewer)) {
                            // setSelected(treeViewer, true);
                        }
                        if (!multiViewer.isLocked()) {
                        }
                        multiViewer.updateView(IDirector.ENABLE_STATE);
                    }

                    public void doDeselect(EdgeSet edges) {
                        multiViewer.updateView(IDirector.ENABLE_STATE);
                    }

                    public void doClick(EdgeSet edges, int clicks) {
                        if (!isSelected(treeViewer)) {
                            setSelected(treeViewer, true);
                        }
                    }

                    public void doClickLabel(EdgeSet edges, int clicks) {
                        this.doClick(edges, clicks);
                    }

                    public void doLabelMoved(EdgeSet edges) {
                        if (!treeViewer.isDirty()) {
                            treeViewer.setDirty(true);
                            updateName(treeViewer);
                        }
                    }
                });

                treeViewer.setSize(panelWidth, panelHeight);

                add(treeViewer.getScrollPane());
                treeViewer.getNodeImageManager().applyImagesToNodes();

                treeViewer.getScrollPane().revalidate();
            }
        }

        BitSet which = new BitSet();
        for (int t = first; t < doc.getNumberOfTrees(); t++)
            which.set(t);

        loadTrees(doc, which);
        setShowScrollBars(showScrollBars);
        revalidate();
    }

    /**
     * get the number of rows
     *
     * @return rows
     */
    public int getRows() {
        return rows;
    }

    /**
     * get the number of cols
     *
     * @return cols
     */
    public int getCols() {
        return cols;
    }

    /**
     * get a specific tree viewer
     *
     * @param row
     * @param col
     * @return
     */
    public TreeViewer getTreeViewer(int row, int col) {
        if (row < treeViewers.length && col < treeViewers[row].length)
            return treeViewers[row][col];
        else
            return null;
    }

    /**
     * load trees from document into grid
     *
     * @param doc
     * @param which
     */
    public void loadTrees(Document doc, BitSet which) {
        if (mainScrollBar != null && doc != null) {
            multiViewer.getDir().getDocument().setInternalNodeLabelsAreEdgeLabels(doc.isInternalNodeLabelsAreEdgeLabels());

            //    mainScrollBar.setMinimum(1) ;
            if (!avoidScrollBarBounce) {
                avoidScrollBarBounce = true;
                mainScrollBar.setMaximum(doc.getNumberOfTrees());
                mainScrollBar.setVisibleAmount(getNumberOfPanels());
                mainScrollBar.setBlockIncrement(getNumberOfPanels());
                mainScrollBar.setValue(which != null ? which.nextSetBit(0) : 0);
                mainScrollBar.setEnabled(getNumberOfPanels() < doc.getNumberOfTrees());
                mainScrollBar.setVisible(getNumberOfPanels() < doc.getNumberOfTrees());
                avoidScrollBarBounce = false;
            }
        }

        selectedViewers.clear();
        currentTrees.clear();
        //connectors.clear();


        // sync current trees to document:
        syncCurrentViewers2Document(doc, false);

        treeViewer2TreeId.clear();

        if (doc != null) {
            Iterator<TreeViewer> it = getIterator();
            for (int i = 0; i < doc.getNumberOfTrees(); i++) {
                if (which == null || which.get(i)) {
                    if (it.hasNext()) {
                        TreeViewer treeViewer = it.next();
                        treeViewer.setCanvasColor(Color.WHITE);
                        treeViewer.setName(doc.getName(i));
                        treeViewer.getPhyloTree().clear();
                        treeViewer.setHasCoordinates(false);
                        currentTrees.set(i);
                        doc.setCurrent(i);
                        treeViewer2TreeId.put(treeViewer, i);

                        if (doc.getTree(i) != null) {
                            try {
                                LayoutOptimizerManager.apply(multiViewer.getEmbedderName(), doc.getTree(i));
                            } catch (Exception e) {
                                Basic.caught(e);
                            }
                            doc.getTree(i).syncData2Viewer(doc, treeViewer);
                        }
                        if (isShowBorders() && treeViewer.getWidth() >= 300 && treeViewer.getHeight() >= 200)
                            treeViewer.getScrollPane().setBorder(BorderFactory.createTitledBorder(treeViewer.getName() + (treeViewer.isDirty() ? "*" : "")));
                        else
                            treeViewer.getScrollPane().setBorder(BorderFactory.createEmptyBorder());
                        treeViewer.getScrollPane().revalidate();
                    }
                }
            }
            while (it.hasNext()) {
                TreeViewer treeViewer = it.next();
                treeViewer.setCanvasColor(Color.WHITE);
                treeViewer.getGraph().deleteAllNodes();
                treeViewer.getPhyloTree().setRoot((Node) null);
                treeViewer.setFoundNode(null);
                treeViewer.getScrollPane().setBorder(BorderFactory.createEmptyBorder());
                treeViewer.getScrollPane().revalidate();
            }

            doc.getConnectors().syncDocumentToCurrentViewers(this);
        }
        //repaint();
    }

    /**
     * update the name of the tree
     *
     * @param treeViewer
     */
    public void updateName(TreeViewer treeViewer) {
        if (isShowBorders())
            treeViewer.getScrollPane().setBorder(BorderFactory.createTitledBorder(treeViewer.getName() + (treeViewer.isDirty() ? "*" : "")));
    }

    /**
     * show or hide scroll bars
     *
     * @param showScrollBars
     */
    public void setShowScrollBars(boolean showScrollBars) {
        this.showScrollBars = showScrollBars;
        for (Iterator<TreeViewer> it = getIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            showScrollBars = (treeViewer.getPhyloTree().getNumberOfNodes() != 0 && this.showScrollBars);
            treeViewer.getScrollPane().setVerticalScrollBarPolicy(showScrollBars ? JScrollPane.VERTICAL_SCROLLBAR_ALWAYS : JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            treeViewer.getScrollPane().setHorizontalScrollBarPolicy(showScrollBars ? JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS : JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            treeViewer.getScrollPane().revalidate();
        }
    }

    /**
     * sync the current viewers to the document
     *
     * @param doc
     */
    public void syncCurrentViewers2Document(Document doc, boolean alwaysIfHasAdditional) {

        // sync current trees to document:
        for (TreeViewer treeViewer : treeViewer2TreeId.keySet()) {
            int id = treeViewer2TreeId.get(treeViewer);
            if (treeViewer.isDirty() || (alwaysIfHasAdditional && doc.getTree(id).hasAdditional())) {
                doc.getTree(id).syncViewer2Data(treeViewer, treeViewer.isDirty());
                doc.setName(treeViewer.getName(), id);
            }
        }
        doc.getConnectors().syncCurrentViewersToDocument(this);
    }

    /**
     * show or hide scroll bars
     *
     * @param showBorders
     */
    public void setShowBorders(boolean showBorders) {
        this.showBorders = showBorders;

        for (Iterator<TreeViewer> it = getIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            if (isShowBorders() && treeViewer.getPhyloTree().getNumberOfNodes() > 0)

                treeViewer.getScrollPane().setBorder(BorderFactory.createTitledBorder(treeViewer.getName()));
            else
                treeViewer.getScrollPane().setBorder(null);
            treeViewer.getScrollPane().revalidate();
        }
    }

    public boolean isShowBorders() {
        return showBorders;
    }

    /**
     * set the selection state of all panels
     *
     * @param select
     */
    public void selectAllPanels(boolean select) {
        for (Iterator<TreeViewer> it = getIterator(); it.hasNext(); )
            setSelected(it.next(), select);
    }

    /**
     * invert the selection state of all panels
     */
    public void selectPanelsInvert() {
        for (Iterator<TreeViewer> it = getIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            setSelected(treeViewer, !isSelected(treeViewer));
        }
    }

    /**
     * set the selection state of a panel
     *
     * @param treeViewer
     * @param select
     */
    public void setSelected(TreeViewer treeViewer, boolean select) {
        if (select != selectedViewers.contains(treeViewer)) {
            if (!select) {
                selectedViewers.remove(treeViewer);
                treeViewer.setCanvasColor(Color.WHITE);
            } else {
                if (treeViewer.getGraph().getNumberOfNodes() > 0) {
                    selectedViewers.add(treeViewer);
                    //treeViewer.setCanvasColor(new Color(255, 220, 230)); // pink
                    treeViewer.setCanvasColor(new Color(230, 255, 230)); // light yellow
                }
            }
            treeViewer.repaint();
        }
    }

    public boolean isSelected(TreeViewer treeViewer) {
        return selectedViewers.contains(treeViewer);
    }

    /**
     * gets an iterator over all tree views
     *
     * @return iterator
     */
    public Iterator<TreeViewer> getIterator() {
        return new TreeViewerIterator(this, false);
    }

    /**
     * gets an iterator over all selected tree views
     *
     * @return iterator
     */
    public Iterator<TreeViewer> getSelectedIterator() {
        return new TreeViewerIterator(this, true);
    }

    /**
     * gets an iterator over all selected tree views
     *
     * @return iterator
     */
    public Iterator<TreeViewer> getSelectedOrAllIterator() {
        Iterator<TreeViewer> it = getSelectedIterator();
        if (it.hasNext())
            return it;
        else
            return getIterator();
    }

    /**
     * gets an iterator over all panels with selected nodes
     *
     * @return iterator
     */
    public Iterator<TreeViewer> getSelectedNodesIterator() {
        return new TreeViewerIterator(this, true, true, false);
    }

    /**
     * gets an iterator over all panels with selected edges
     *
     * @return iterator
     */
    public Iterator<TreeViewer> getSelectedEdgesIterator() {
        return new TreeViewerIterator(this, true, false, true);
    }

    /**
     * gets the current set of trees
     *
     * @return current trees
     */
    public BitSet getCurrentTrees() {
        return (BitSet) currentTrees.clone();
    }

    public void paint(Graphics g) {
        super.paint(g);

        for (Connector connector : connectors) {
            connector.paint(multiViewer.getFrame().getContentPane(), (Graphics2D) g);
        }
    }

    /**
     * add a connector between two nodes in two different treeviewers
     *
     * @param treeViewer1
     * @param node1
     * @param treeViewer2
     * @param node2
     * @param color
     */
    public void addConnector(TreeViewer treeViewer1, Node node1, TreeViewer treeViewer2, Node node2, Color color) {
        connectors.add(new Connector(treeViewer1, node1, treeViewer2, node2, color));
    }

    /**
     * clear all connectors
     */
    public void clearConnectors() {
        connectors.clear();

    }

    /**
     * gets the list of connectors
     *
     * @return connectors
     */
    public List<Connector> getConnectors() {
        return connectors;
    }

    /**
     * gets the number of panels
     *
     * @return panels
     */
    public int getNumberOfPanels() {
        return rows * cols;
    }

    /**
     * gets the specified viewer
     *
     * @param which : must be between 0 and numberOfPanels-1
     * @return viewer
     */
    public TreeViewer getViewerByRank(int which) {
        if (which >= 0 && which < getNumberOfPanels()) {
            int row = (which / cols);
            int col = (which % cols);
            return treeViewers[row][col];
        }
        return null;
    }

    /**
     * Determines the rank 0..(nrows-1)*(ncols-1) of the given tree viewer
     *
     * @param treeViewer
     * @return number of tree viewer, or -1, if not found
     */
    public int getRankOfViewer(TreeViewer treeViewer) {
        int which = 0;
        for (int row = 0; row < getRows(); row++) {
            for (int col = 0; col < getCols(); col++) {
                if (treeViewers[row][col] == treeViewer)
                    return which;
                which++;
            }
        }
        return -1;
    }

    /**
     * gets the number that this treeViewer has in the document
     *
     * @param treeViewer
     * @return number   or null
     */
    public Integer getNumberOfViewerInDocument(TreeViewer treeViewer) {
        return treeViewer2TreeId.get(treeViewer);
    }

    /**
     * connect all nodes with the same label
     *
     * @param treeViewer1
     * @param treeViewer2
     */
    public void connectorAllTaxa(TreeViewer treeViewer1, TreeViewer treeViewer2) {
        for (Node node1 = treeViewer1.getGraph().getFirstNode(); node1 != null; node1 = node1.getNext()) {
            if (treeViewer1.getSelectedNodes().size() == 0 || treeViewer1.getSelected(node1)) {
                String label1 = treeViewer1.getLabel(node1);
                if (label1 != null) {
                    label1 = label1.trim();
                    if (label1.length() > 0) {
                        if (!multiViewer.getDir().getDocument().isInternalNodeLabelsAreEdgeLabels() || treeViewer1.getSelected(node1) || node1.getOutDegree() == 0 || !NumberUtils.isDouble(label1)) {
                            for (Node node2 = treeViewer2.getGraph().getFirstNode(); node2 != null; node2 = node2.getNext()) {
                                if (treeViewer2.getSelectedNodes().size() == 0 || treeViewer2.getSelected(node2)) {
                                    String label2 = treeViewer2.getLabel(node2);
                                    if (label2 != null) {
                                        label2 = label2.trim();
                                        if (label2.length() > 0) {
                                            if (!multiViewer.getDir().getDocument().isInternalNodeLabelsAreEdgeLabels() || treeViewer2.getSelected(node2) || node2.getOutDegree() == 0 || !NumberUtils.isDouble(label2)) {
                                                if (label1.equals(label2)) {
                                                    addConnector(treeViewer1, node1, treeViewer2, node2, Color.LIGHT_GRAY);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * get number of selected viewers
     *
     * @return
     */
    public int getNumberOfSelectedViewers() {
        return selectedViewers.size();
    }

    /**
     * gets the list of ids of selected viewers
     *
     * @return ids
     */
    public List<Integer> getSelected() {
        List<Integer> result = new LinkedList<>();
        int which = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (isSelected(treeViewers[row][col]))
                    result.add(which);
                which++;
            }
        }
        return result;
    }

    /**
     * gets the last selected and non-empty tree viewer or null
     *
     * @return last selected or null
     */
    public TreeViewer getLastSelected() {
        TreeViewer treeViewer = null;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (isSelected(treeViewers[row][col]) && treeViewers[row][col].getGraph().getNumberOfNodes() > 0)
                    treeViewer = treeViewers[row][col];
            }
        }
        return treeViewer;
    }


    /**
     * gets the number of selected viewers, if >0, else the total number of viewers
     *
     * @return number of selected or all
     */
    public int getNumberSelectedOrAllViewers() {
        if (getNumberOfSelectedViewers() > 0)
            return getNumberOfSelectedViewers();
        else
            return getNumberOfPanels();

    }

    public boolean isShowScrollBars() {
        return showScrollBars;
    }

    public void setPopupMenuConfig(String nodeConfig, String edgeConfig, String panelConfig, CommandManager commandManager) {
        this.nodePopupConfig = nodeConfig;
        this.edgePopupConfig = edgeConfig;
        this.panelPopupConfig = panelConfig;
        this.commandManager = commandManager;

    }

    /**
     * gets all selected node labels that are not internal numbers
     *
     * @return labels
     */
    public Set<String> getSelectedNodeLabelsNotInternalNumbers() {
        Set<String> labels = new HashSet<>();
        for (Iterator<TreeViewer> it = getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            labels.addAll(viewer.getSelectedNodeLabelsNotInternalNumbers());
        }
        return labels;
    }

    /**
     * get as a JPanel which can be painted in image export
     *
     * @return
     */
    public JPanel getAsJPanel() {
        JPanel panel = new JPanel() {
            public void paint(Graphics graphics) {
                setBackground(Color.WHITE);
                super.paint(graphics);
                doPaint(graphics, getSize().width, getSize().height);
                for (Connector connector : connectors) {
                    connector.paint(multiViewer.getFrame().getContentPane(), (Graphics2D) graphics);
                }
            }
        };
        panel.setSize(getSize().width + 4, getSize().height + 4);
        return panel;
    }

    private void doPaint(Graphics g0, int width, int height) {
        for (int r = 0; r < getRows(); r++) {
            int yOffset = (isShowBorders() ? 20 : 0) + Math.round((float) r * height / (float) getRows());
            for (int c = 0; c < getCols(); c++) {
                int xOffset = 1 + Math.round((float) c * width / (float) getCols());

                TreeViewer viewer = getTreeViewer(r, c);
                final Point apt = viewer.getScrollPane().getViewport().getViewPosition();
                final Dimension extent = (Dimension) viewer.getScrollPane().getViewport().getExtentSize().clone();

                g0.translate(xOffset, yOffset);

                g0.setClip(0, 0, extent.width, extent.height);

                g0.translate(-apt.x, -apt.y);

                g0.setColor(viewer.getBackground());
                g0.fillRect(apt.x, apt.y, extent.width, extent.height);

                if (isShowBorders()) {
                    g0.setColor(Color.GRAY);
                    g0.setFont(((new JButton())).getFont());
                    g0.drawString(viewer.getName(), apt.x + 10, apt.y + 12);
                    g0.drawRect(apt.x, apt.y, extent.width, extent.height);
                }

                boolean save = viewer.inPrint;
                viewer.inPrint = true;
                viewer.paint(g0);
                viewer.inPrint = save;

                g0.translate(apt.x, apt.y);
                g0.translate(-xOffset, -yOffset);
            }
        }
    }

    public boolean isInSelectNodes() {
        return inSelectNodes;
    }

    public void setInSelectNodes(boolean inSelectNodes) {
        this.inSelectNodes = inSelectNodes;
    }
}


class TreeViewerIterator implements Iterator<TreeViewer> {
    private int row = 0;
    private int col = -1;
    final private TreeGrid treeGrid;
    final private boolean selectedPanelsOnly;
    final private boolean mustHaveSelectedNodes;
    final private boolean mustHaveSelectedEdges;

    private TreeViewer next;

    public TreeViewerIterator(TreeGrid treeGrid, boolean selectedPanelsOnly) {
        this.treeGrid = treeGrid;
        this.selectedPanelsOnly = selectedPanelsOnly;
        this.mustHaveSelectedNodes = false;
        this.mustHaveSelectedEdges = false;
        next = gotoNext();
    }

    public TreeViewerIterator(TreeGrid treeGrid, boolean selectedPanelsOnly, boolean mustHaveSelectedNodes, boolean mustHaveSelectedEdges) {
        this.treeGrid = treeGrid;
        this.selectedPanelsOnly = selectedPanelsOnly;
        this.mustHaveSelectedNodes = mustHaveSelectedNodes;
        this.mustHaveSelectedEdges = mustHaveSelectedEdges;
        next = gotoNext();
    }


    public boolean hasNext() {
        return next != null;
    }

    public TreeViewer next() {
        TreeViewer result = next;
        next = gotoNext();
        return result;
    }

    public void remove() {
        System.err.println("remove(): Not implemented");
    }

    private TreeViewer gotoNext() {
        col++;
        if (col == treeGrid.getCols()) {
            row++;
            if (row < treeGrid.getRows())
                col = 0;
        }
        while (row < treeGrid.getRows() && col < treeGrid.getCols()) {
            if (treeGrid.getTreeViewer(row, col) != null && (!selectedPanelsOnly || treeGrid.getTreeViewer(row, col).getPhyloTree().getNumberOfNodes() > 0)
                    &&
                    ((!selectedPanelsOnly || treeGrid.getNumberOfSelectedViewers() == 0 || treeGrid.isSelected(treeGrid.getTreeViewer(row, col)))
                            && (!mustHaveSelectedNodes || treeGrid.getTreeViewer(row, col).getSelectedNodes().size() > 0)
                            && (!mustHaveSelectedEdges || treeGrid.getTreeViewer(row, col).getSelectedEdges().size() > 0)))
                return treeGrid.getTreeViewer(row, col);
            col++;
            if (col == treeGrid.getCols()) {
                row++;
                if (row < treeGrid.getRows())
                    col = 0;
            }
        }
        return null;
    }
}
