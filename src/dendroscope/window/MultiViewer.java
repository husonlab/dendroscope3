/*
 *   MultiViewer.java Copyright (C) 2020 Daniel H. Huson
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

import dendroscope.commands.CloseCommand;
import dendroscope.commands.SaveAsCommand;
import dendroscope.core.Director;
import dendroscope.core.Document;
import dendroscope.core.TreeData;
import dendroscope.dialogs.input.InputDialog;
import dendroscope.embed.LayoutOptimizerManager;
import dendroscope.main.DendroscopeProperties;
import dendroscope.main.Version;
import jloda.phylo.PhyloTree;
import jloda.swing.commands.CommandManager;
import jloda.swing.commands.ICommand;
import jloda.swing.director.*;
import jloda.swing.find.FindToolBar;
import jloda.swing.find.SearchManager;
import jloda.swing.format.Formatter;
import jloda.swing.graphview.INodeEdgeFormatable;
import jloda.swing.message.MessageWindow;
import jloda.swing.util.*;
import jloda.swing.window.MenuBar;
import jloda.swing.window.MenuConfiguration;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A viewer for multiple trees
 * Daniel Huson, 4.2010
 */
public class MultiViewer implements IDirectableViewer, IViewerWithFindToolBar, IMainViewer, Printable {
    private boolean locked = false;
    private boolean uptodate = true;
    final private JFrame frame;
    final private Director dir;
    final private Document doc;
    final private TreeGrid treeGrid;
    final private MenuBar menuBar;
    final private CommandManager commandManager;
    final private StatusBar statusBar;
    final private FormatterHelper formattingHelper;

    private String embedderName = LayoutOptimizerManager.ALGORITHM2010;

    static final private Set<String> previouslySelectedNodeLabels = new HashSet<>(); // keep track of latest selection

    private boolean mustRecomputeEmbedding = false;
    private boolean mustRecomputeCoordinates = false;
    public static JFrame lastActiveFrame = null;

    private final JPanel mainPanel;
    private boolean showFindToolBar = false;
    private final SearchManager searchManager;

    /**
     * construct a new multiviewer
     *
     * @param dir
     */
    public MultiViewer(final Director dir, int rows, int cols) {
        this.dir = dir;
        dir.setMainViewer(this);
        this.doc = dir.getDocument();
        treeGrid = new TreeGrid(this);
        this.commandManager = new CommandManager(dir, this, "dendroscope.commands");

        searchManager = new SearchManager(dir, this, new NodeSearcher(this), false, true);

        MenuConfiguration menuConfig = GUIConfiguration.getMenuConfiguration();
        String toolBarConfig = GUIConfiguration.getToolBarConfiguration();

        this.menuBar = new MenuBar(this, menuConfig, getCommandManager());
        DendroscopeProperties.addPropertiesListListener(menuBar.getRecentFilesListener());
        DendroscopeProperties.notifyListChange(ProgramProperties.RECENTFILES);
        ProjectManager.addAnotherWindowWithWindowMenu(dir, menuBar.getWindowMenu());

        if (rows == 0 || cols == 0) {
            String gridSize = ProgramProperties.get("GridDimensions", "1x2");
            gridSize = JOptionPane.showInputDialog("Set dimensions of grid (rows x cols):", gridSize);
            if (gridSize != null && gridSize.length() > 0) {
                String[] tokens = gridSize.split("x");
                if (tokens.length == 2 && Basic.isInteger(tokens[0].trim()) && Basic.isInteger(tokens[1].trim())) {
                    rows = Integer.parseInt(tokens[0].trim());
                    cols = Integer.parseInt(tokens[1].trim());
                    if (rows > 0 && cols > 0)
                        ProgramProperties.put("GridDimensions", rows + " x " + cols);
                } else
                    new Alert(getFrame(), "Failed to set grid, couldn't parse: " + gridSize);
            }
        }
        treeGrid.setPopupMenuConfig(GUIConfiguration.getNodePopupConfiguration(), GUIConfiguration.getEdgePopupConfiguration(),
                GUIConfiguration.getPanelPopupConfiguration(), commandManager);
        treeGrid.setGridSize(Math.max(1, rows), Math.max(1, cols));

        formattingHelper = new FormatterHelper(this);

        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        setTitle(dir);
        frame.setJMenuBar(menuBar);

        JToolBar toolBar = new ToolBar(this, toolBarConfig, commandManager);
        frame.add(toolBar, BorderLayout.NORTH);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(treeGrid, BorderLayout.CENTER);
        mainPanel.add(treeGrid.getMainScrollBar(), BorderLayout.EAST);
        frame.getContentPane().add(mainPanel, BorderLayout.CENTER);

        statusBar = new StatusBar();
        frame.getContentPane().add(statusBar, BorderLayout.SOUTH);

        // frame.getContentPane().add(new JScrollPane(treeGrid), BorderLayout.CENTER);
        frame.setIconImages(ProgramProperties.getProgramIconImages());

        int[] geometry;
        JFrame lastActiveFrame = getLastActiveFrame();
        if (lastActiveFrame != null && lastActiveFrame.isVisible()) {
            geometry = new int[]{lastActiveFrame.getLocationOnScreen().x + 20, lastActiveFrame.getLocationOnScreen().y + 20, lastActiveFrame.getWidth(), lastActiveFrame.getHeight()};
            frame.setLocation(geometry[0], geometry[1]);
        } else {
            geometry = ProgramProperties.get(ProgramProperties.MAIN_WINDOW_GEOMETRY, new int[]{100, 100, 500, 400});
            frame.setLocation(geometry[0] + (dir.getID() - 1) * 20, geometry[1] + (dir.getID() - 1) * 20);
        }
        frame.setSize(geometry[2], geometry[3]);

        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent event) {
                if (event.getID() == ComponentEvent.COMPONENT_RESIZED &&
                        (frame.getExtendedState() & JFrame.MAXIMIZED_HORIZ) == 0
                        && (frame.getExtendedState() & JFrame.MAXIMIZED_VERT) == 0) {
                    ProgramProperties.put(ProgramProperties.MAIN_WINDOW_GEOMETRY,
                            new int[]{frame.getLocation().x, frame.getLocation().y, frame.getSize().width, frame.getSize().height});
                }
                getTreeGrid().gridGeometryChanged();
                getCommandManager().updateEnableState();
            }

            public void componentMoved(java.awt.event.ComponentEvent event) {
                if (event.getID() == ComponentEvent.COMPONENT_RESIZED &&
                        (frame.getExtendedState() & JFrame.MAXIMIZED_HORIZ) == 0
                        && (frame.getExtendedState() & JFrame.MAXIMIZED_VERT) == 0) {
                    ProgramProperties.put(ProgramProperties.MAIN_WINDOW_GEOMETRY, new int[]
                            {frame.getLocation().x, frame.getLocation().y, frame.getSize().width,
                                    frame.getSize().height});
                }
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                // if(!isLocked())  
                {
                    commandManager.getCommand(CloseCommand.NAME).actionPerformed(null);
                }
            }

            public void windowOpened(WindowEvent event) {
            }

            public void windowDeactivated(WindowEvent event) {
                setLastActiveFrame(getFrame());

                Set<String> selectedLabels = treeGrid.getSelectedNodeLabelsNotInternalNumbers();
                if (selectedLabels.size() != 0) {
                    previouslySelectedNodeLabels.clear();
                    previouslySelectedNodeLabels.addAll(selectedLabels);
                }
            }

            public void windowActivated(WindowEvent event) {
                frame.requestFocusInWindow();
                if (Formatter.getInstance() != null) {
                    Formatter.getInstance().setViewer(getDir(), getFormattingHelper());
                    Formatter.getInstance().updateView(IDirector.ALL);
                    if (isLocked())
                        Formatter.getInstance().lockUserInput();
                    else
                        Formatter.getInstance().unlockUserInput();
                }
                if (InputDialog.getInstance() != null) {
                    InputDialog.getInstance().setViewer(getDir());
                }
                setLastActiveFrame(frame);
            }
        });

        setupMessageWindow();

        setCursor(Cursors.getOpenHand());
        // if (ProgramProperties.isUseGUI())
        frame.setVisible(true);
    }

    /**
     * sets up the message window
     */
    private void setupMessageWindow() {
        if (MessageWindow.getInstance() == null) {
            MessageWindow.setInstance(
                    new MessageWindow(ProgramProperties.getProgramIcon(), "Messages - Dendroscope", getFrame(), false));
            MessageWindow.getInstance().getTextArea().setFont(new Font("Monospaced", Font.PLAIN, 12));
            MessageWindow.getInstance().getFrame().addWindowListener(new WindowAdapter() {
                public void windowActivated(WindowEvent event) {
                    SearchManager searchManager = SearchManager.getInstance();
                    if (searchManager != null) {
                        searchManager.setEnabled("Messages", true);
                        searchManager.getFrame().setTitle("Find/Replace - Messages - Dendroscope");

                    }
                }

                public void windowClosing(WindowEvent event) {
                    SearchManager searchManager = SearchManager.getInstance();
                    if (searchManager != null)
                        searchManager.setEnabled("Messages", false);
                }
            });
        }
    }

    /**
     * gets the window menu
     *
     * @return window menu
     */
    public JMenu getWindowMenu() {
        return null;
    }

    /**
     * get the quit action
     *
     * @return quit action
     */
    public AbstractAction getQuit() {
        return (AbstractAction) getCommandManager().getButton("Quit").getAction();
    }

    /**
     * return the frame associated with the viewer
     *
     * @return frame
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * gets the title
     *
     * @return title
     */
    public String getTitle() {
        return frame.getTitle();
    }

    /**
     * sets the title of the window
     */
    public void setTitle(Director dir) {
        String newTitle = doc.getTitle();
        if (doc.isDocumentIsDirty())
            newTitle += "*";

        if (dir.getID() == 1)
            newTitle += " - " + DendroscopeProperties.getVersion();
        else
            newTitle += " - [" + dir.getID() + "] - " + DendroscopeProperties.getVersion();

        if (frame.getTitle().equals(newTitle) == false) {
            frame.setTitle(newTitle);
            ProjectManager.updateWindowMenus();
        }
    }


    /**
     * ask view to update itself. This is method is wrapped into a runnable object
     * and put in the swing event queue to avoid concurrent modifications.
     *
     * @param what what should be updated? Possible values: Director.ALL or Director.TITLE
     */
    public void updateView(String what) {
        setUptoDate(false);

        if (!what.equals(IDirector.TITLE)) {
            for (Iterator<TreeViewer> it = getTreeGrid().getIterator(); !doc.isDocumentIsDirty() && it.hasNext(); ) {
                if (it.next().isDirty())
                    doc.setDocumentIsDirty(true);
            }
        }

        setTitle(getDir());

        if (isMustRecomputeEmbedding() && what.equals(IDirector.ALL)) {
            recomputeEmbedding();
            setMustRecomputeEmbedding(false);
        }

        getCommandManager().updateEnableState();
        ((NodeSearcher) getSearchManager().getSearcher()).updateViewers();
        FindToolBar findToolBar = searchManager.getFindDialogAsToolBar();
        if (findToolBar.isClosing()) {
            showFindToolBar = false;
            findToolBar.setClosing(false);
        }
        if (!findToolBar.isEnabled() && showFindToolBar) {
            mainPanel.add(findToolBar, BorderLayout.NORTH);
            findToolBar.setEnabled(true);
            frame.getContentPane().validate();
            getCommandManager().updateEnableState();
        } else if (findToolBar.isEnabled() && !showFindToolBar) {
            mainPanel.remove(findToolBar);
            findToolBar.setEnabled(false);
            frame.getContentPane().validate();
            getCommandManager().updateEnableState();
        }
        if (findToolBar.isEnabled())
            findToolBar.clearMessage();

        setUptoDate(true);
        updateStatusBar();
    }

    /**
     * ask view to prevent user input
     */
    public void lockUserInput() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        locked = true;
        updateStatusBar();
        statusBar.setEnabled(true);
        getCommandManager().setEnableCritical(false);
        menuBar.setEnableRecentFileMenuItems(false);
        searchManager.getFindDialogAsToolBar().setEnableCritical(true);
    }

    /**
     * ask view to allow user input
     */
    public void unlockUserInput() {
        locked = false;
        updateStatusBar();
        statusBar.setEnabled(true);
        setCursor(Cursors.getOpenHand());
        getCommandManager().setEnableCritical(true);
        menuBar.setEnableRecentFileMenuItems(true);
        searchManager.getFindDialogAsToolBar().setEnableCritical(true);

        SearchManager searchManager = SearchManager.getInstance();
        if (searchManager != null && searchManager.isLocked() && frame.isActive()) {
            searchManager.unlockUserInput();
        }
        ProjectManager.updateWindowMenus();
    }

    /**
     * set the status line for given document
     */
    public void updateStatusBar() {
        if (isLocked()) {
            statusBar.setText2("Busy...");
        } else {
            Document doc = getDir().getDocument();

            StringBuilder buf1 = new StringBuilder();
            buf1.append("Showing ");
            BitSet current = getTreeGrid().getCurrentTrees();
            int first = current.nextSetBit(0);
            if (first >= 0) {
                buf1.append(first + 1);
                if (doc.getNumberOfTrees() > 1) {
                    buf1.append(" to ").append(first + current.cardinality()).append(" (of ").append(doc.getNumberOfTrees()).append(")");
                }
                statusBar.setText1(buf1.toString());

                int countNodes = 0;
                int countSelectedNodes = 0;
                int countEdges = 0;
                int countSelectedEdges = 0;

                for (Iterator<TreeViewer> it = getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
                    TreeViewer viewer = it.next();
                    PhyloTree tree = viewer.getPhyloTree();
                    countNodes += tree.getNumberOfNodes();
                    countSelectedNodes += viewer.getSelectedNodes().size();
                    countEdges += tree.getNumberOfEdges();
                    countSelectedEdges += viewer.getSelectedEdges().size();
                }

                StringBuilder buf = new StringBuilder();
                buf.append("Nodes=").append(countNodes);
                if (countSelectedNodes > 0) {
                    buf.append(" (selected=").append(countSelectedNodes).append(")");
                }
                buf.append(" Edges=").append(countEdges);
                if (countSelectedEdges > 0) {
                    buf.append(" (selected=").append(countSelectedEdges).append(")");
                }
                statusBar.setText2(buf.toString());
            } else
                statusBar.setText2("");
        }
    }

    /**
     * ask view to destroy itself
     */
    public void destroyView() throws CanceledException {
        try {
            if (doc.isDocumentIsDirty())
                askToSaveCurrent();
        } catch (CanceledException ex) {
            ProjectManager.setQuitting(false);
            throw ex;
        }
        try {
            if (ProjectManager.isQuitting() && ProjectManager.getNumberOfProjects() == 1) {
                if (ProgramProperties.isUseGUI() && !confirmQuit()) {
                    ProjectManager.setQuitting(false);
                }
            }
        } catch (CanceledException ex) {
            ProjectManager.setQuitting(false);
            throw ex;
        }

        ProgramProperties.put(ProgramProperties.MULTI_WINDOW_GEOMETRY, new int[]
                {frame.getLocation().x, frame.getLocation().y, frame.getSize().width,
                        frame.getSize().height});
        if (getDir().getDocument().getProgressListener() != null) {
            getDir().getDocument().getProgressListener().close();
        }
        if (getLastActiveFrame() == getFrame())
            setLastActiveFrame(null);
        searchManager.getFindDialogAsToolBar().close();
        frame.setVisible(false);
        frame.dispose();
    }

    /**
     * determine whether current data needs saving and allows the user to do so, if necessary
     */
    private void askToSaveCurrent() throws CanceledException {
        if (ProgramProperties.isUseGUI()) {
            int result = JOptionPane.showConfirmDialog(getFrame(),
                    "Document has been modified, save before " + (ProjectManager.isQuitting() ? "quitting?" : "closing?"), Version.NAME + " - Save Changes?",
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                Boolean[] canceled = new Boolean[]{Boolean.FALSE};
                ICommand saveCommand = new SaveAsCommand();
                saveCommand.setDir(getDir());
                saveCommand.setViewer(this);
                saveCommand.actionPerformed(new ActionEvent(canceled, 0, "askToSave"));
                if (canceled[0].equals(Boolean.TRUE))
                    throw new CanceledException();
                doc.setDocumentIsDirty(false);
            } else if (result == JOptionPane.NO_OPTION)
                doc.setDocumentIsDirty(false);
            else if (result == JOptionPane.CANCEL_OPTION)
                throw new CanceledException();
        }
    }

    /**
     * ask whether user wants to quit
     */
    private boolean confirmQuit() throws CanceledException {
        JFrame parent = getLastActiveFrame();
        if (parent == null)
            parent = getFrame();
        int result = JOptionPane.showConfirmDialog(parent, "Quit " + Version.NAME + "?", Version.NAME + " - Quit?", JOptionPane.YES_NO_CANCEL_OPTION);
        if (result == JOptionPane.CANCEL_OPTION) {
            throw new CanceledException();
        } else return result != JOptionPane.NO_OPTION;
    }


    /**
     * set uptodate state
     *
     * @param flag
     */
    public void setUptoDate(boolean flag) {
        uptodate = flag;
    }

    /**
     * is viewer uptodate?
     *
     * @return uptodate
     */
    public boolean isUptoDate() {
        return uptodate;
    }

    /**
     * gets the director
     *
     * @return dir
     */
    public Director getDir() {
        return dir;
    }

    /**
     * gets the tree grid
     *
     * @return tree grid
     */
    public TreeGrid getTreeGrid() {
        return treeGrid;
    }

    /**
     * move to the next tree   in the document
     */
    public void goNextTree() {
        BitSet which = treeGrid.getCurrentTrees();
        int first = which.nextSetBit(0);
        int last = getLast(which);
        if (last + 1 < doc.getNumberOfTrees()) {
            which.set(first, false);
            which.set(last + 1);
            treeGrid.loadTrees(doc, which);
        }
    }

    /**
     * go to the previous tree in the document
     */
    public void goPreviousTree() {
        BitSet which = treeGrid.getCurrentTrees();
        int first = which.nextSetBit(0);
        int last = getLast(which);
        if (first - 1 >= 0) {
            which.set(first - 1, true);
            if (last - first > getTreeGrid().getNumberOfPanels())
                which.set(last, false);
            treeGrid.loadTrees(doc, which);
        }
    }

    /**
     * go to the specified tree
     *
     * @param current number of tree between 1 and number of trees
     */
    public void goToTree(int current) {
        if (current > 0)
            current--;

        if (current > doc.getNumberOfTrees() - treeGrid.getNumberOfPanels())
            current = Math.max(0, doc.getNumberOfTrees() - treeGrid.getNumberOfPanels());
        BitSet which = new BitSet();
        for (int i = current; i < current + treeGrid.getNumberOfPanels(); i++) {
            which.set(i);
        }
        treeGrid.loadTrees(doc, which);
    }


    /**
     * gets the last element in a bit set
     *
     * @param set
     * @return last
     */
    public int getLast(BitSet set) {
        int card = set.cardinality();
        int t = set.nextSetBit(0);
        while ((--card) > 0) {
            t = set.nextSetBit(t + 1);
        }
        return t;
    }

    /**
     * load the specified trees from the document into the viewer
     *
     * @param which
     */
    public void loadTrees(BitSet which) {
        getTreeGrid().loadTrees(this.doc, which);
    }

    /**
     * load the specified trees, or all, if which==null, from a foreign document
     *
     * @param srcDoc
     */
    public void loadTreesFromDocument(Document srcDoc) {
        for (int i = 0; i < srcDoc.getNumberOfTrees(); i++) {
            doc.setCurrent(doc.appendTree(srcDoc.getName(i), (TreeData) srcDoc.getTree(i).clone(), Integer.MAX_VALUE));
        }
        chooseGridSize();
        getTreeGrid().loadTrees(doc, null);
    }

    /**
     * add a set of trees to the document and multiviewer
     *
     * @param trees
     */
    public void addTrees(TreeData[] trees) {
        if (trees.length > 0) {
            int current = doc.getCurrent();
            BitSet currentTrees = treeGrid.getCurrentTrees();
            int which;
            if (treeGrid.getNumberOfSelectedViewers() > 0)
                which = treeGrid.getNumberOfViewerInDocument(treeGrid.getLastSelected());
            else
                which = Math.max(-1, doc.getNumberOfTrees() - 1);
            doc.setCurrent(which);
            doc.appendTrees(trees, doc.getCurrent());
            if (trees.length > 1) {
                new Message(getFrame(), "Number of trees added: " + trees.length);
            }
            doc.setCurrent(current);

            if (currentTrees.cardinality() == 0) {
                chooseGridSize();
                updateView(IDirector.ALL);
            }
            int first = Math.max(0, currentTrees.nextSetBit(0));
            for (int i = first; i < doc.getNumberOfTrees(); i++) {
                currentTrees.set(i);
                if (currentTrees.cardinality() == treeGrid.getNumberOfPanels())
                    break;
            }
            loadTrees(currentTrees);
            setMustRecomputeEmbedding(true);
            // System.err.println("GOTO: " + (which + 2));
            // goToTree(which + 2);
        }
    }

    /**
     * recompute the embedding of all trees
     */
    public void recomputeEmbedding() {
        for (Iterator<TreeViewer> it = getTreeGrid().getIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            if (viewer.getPhyloTree().getRoot() != null && viewer.getPhyloTree().getRoot().getOwner() != null) {
                Boolean recomputeCoordinates = isMustRecomputeCoordinates() || !viewer.isHasCoordinates();
                viewer.recomputeEmbedding(true, recomputeCoordinates);
                if (recomputeCoordinates)
                    viewer.resetLabelPositions(true);
            }
        }
        setMustRecomputeCoordinates(false);
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * are we currently showing the first tree?
     *
     * @return first tree?
     */
    public boolean isAtFirst() {
        return doc.getNumberOfTrees() == 0 || getTreeGrid().getCurrentTrees().get(0);
    }

    /**
     * are we currently showing the last tree?
     *
     * @return
     */
    public boolean isAtLast() {
        return doc.getNumberOfTrees() == 0 || getTreeGrid().getCurrentTrees().cardinality() == 0
                || getLast(getTreeGrid().getCurrentTrees()) == doc.getNumberOfTrees() - 1;
    }

    /**
     * Print the window
     *
     * @param gc         the graphics context.
     * @param format     page format
     * @param pagenumber page index
     */
    public int print(Graphics gc, PageFormat format, int pagenumber) throws PrinterException {
        if (pagenumber == 0) {
            for (Iterator<TreeViewer> it = treeGrid.getIterator(); it.hasNext(); ) {
                TreeViewer treeViewer = it.next();
                treeViewer.inPrint = true;
            }

            try {
                JPanel componentToBePrinted = getTreeGrid();

                Graphics2D g2d = (Graphics2D) gc;

                double factorX = (format.getImageableWidth() - 50) / (componentToBePrinted.getWidth());
                double factorY = (format.getImageableHeight() - 50) / (componentToBePrinted.getHeight());
                double factor = Math.min(factorX, factorY);
                g2d.scale(factor, factor);

                g2d.translate(50 + format.getImageableX(), 50 + format.getImageableY());
                disableDoubleBuffering(componentToBePrinted);
                componentToBePrinted.paint(g2d);
                enableDoubleBuffering(componentToBePrinted);
            } finally {
                for (Iterator<TreeViewer> it = treeGrid.getIterator(); it.hasNext(); ) {
                    TreeViewer treeViewer = it.next();
                    treeViewer.inPrint = false;
                }
            }
            return Printable.PAGE_EXISTS;
        } else
            return Printable.NO_SUCH_PAGE;
    }

    public static void disableDoubleBuffering(Component c) {
        RepaintManager currentManager = RepaintManager.currentManager(c);
        currentManager.setDoubleBufferingEnabled(false);
    }

    public static void enableDoubleBuffering(Component c) {
        RepaintManager currentManager = RepaintManager.currentManager(c);
        currentManager.setDoubleBufferingEnabled(true);
    }

    /**
     * returns true, if all selected networks are trees
     *
     * @return
     */
    public boolean isAllSelectedAreTrees() {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            if (treeViewer.getPhyloTree().getSpecialEdges().size() > 0)
                return false;
        }
        return true;
    }

    public boolean isLocked() {
        return locked;
    }

    /**
     * choose best grid size based on number of trees
     */
    public void chooseGridSize() {
        if (doc.getNumberOfTrees() <= 3) {
            getTreeGrid().setGridSize(1, doc.getNumberOfTrees());
        } else if (doc.getNumberOfTrees() == 4) {
            getTreeGrid().setGridSize(2, 2);
        } else if (doc.getNumberOfTrees() <= 6) {
            getTreeGrid().setGridSize(2, 3);
        } else if (doc.getNumberOfTrees() <= 8) {
            getTreeGrid().setGridSize(2, 4);
        } else if (doc.getNumberOfTrees() <= 9) {
            getTreeGrid().setGridSize(3, 3);
        } else {
            // get good grid layout:
            int rows = Math.min(ProgramProperties.get(DendroscopeProperties.MULTIVIEWER_ROWS, 7), (int) (Math.sqrt(doc.getNumberOfTrees())));
            int cols = Math.min(ProgramProperties.get(DendroscopeProperties.MULTIVIEWER_COLS, 7), rows * rows >= doc.getNumberOfTrees() ? rows : rows + 1);
            getTreeGrid().setGridSize(Math.max(1, rows), Math.max(1, cols));
        }
    }

    /**
     * scheduled to recompute embedding on next update?
     *
     * @return true, if need to recompute embedding
     */
    public boolean isMustRecomputeEmbedding() {
        return mustRecomputeEmbedding;
    }

    /**
     * schedule to recompute embedding on next update
     *
     * @param mustRecomputeEmbedding
     */
    public void setMustRecomputeEmbedding(boolean mustRecomputeEmbedding) {
        this.mustRecomputeEmbedding = mustRecomputeEmbedding;
    }

    public boolean isMustRecomputeCoordinates() {
        return mustRecomputeCoordinates;
    }

    public void setMustRecomputeCoordinates(boolean mustRecomputeCoordinates) {
        this.mustRecomputeCoordinates = mustRecomputeCoordinates;
    }

    public String getEmbedderName() {
        return embedderName;
    }

    public void setEmbedderName(String embedderName) {
        this.embedderName = embedderName;
    }

    /**
     * sets the cursor
     *
     * @param cursor
     */
    public void setCursor(Cursor cursor) {
        if (!isLocked()) {
            treeGrid.setCursor(cursor);
            for (Iterator<TreeViewer> it = treeGrid.getIterator(); it.hasNext(); ) {
                it.next().setCursor(cursor);
            }
        }
    }

    public INodeEdgeFormatable getFormattingHelper() {
        return formattingHelper;
    }

    /**
     * gets the last main viewer frame that was active
     *
     * @return last front frame
     */
    public static JFrame getLastActiveFrame() {
        if (lastActiveFrame != null && lastActiveFrame.isVisible())
            return lastActiveFrame;
        else
            return null;
    }

    /**
     * set the last active frame
     *
     * @param frame
     */
    public static void setLastActiveFrame(JFrame frame) {
        lastActiveFrame = frame;
    }

    public boolean isShowFindToolBar() {
        return showFindToolBar;
    }

    public void setShowFindToolBar(boolean showFindToolBar) {
        this.showFindToolBar = showFindToolBar;
    }

    public SearchManager getSearchManager() {
        return searchManager;
    }

    /**
     * set of previously selected node labels
     *
     * @return previously selected node labels
     */
    public static Set getPreviouslySelectedNodeLabels() {
        return previouslySelectedNodeLabels;
    }

    /**
     * get name for this type of viewer
     *
     * @return name
     */
    public String getClassName() {
        return "MultiViewer";
    }
}
