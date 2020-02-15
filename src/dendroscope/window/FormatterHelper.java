/*
 *   FormatterHelper.java Copyright (C) 2020 Daniel H. Huson
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

import jloda.graph.EdgeSet;
import jloda.graph.NodeSet;
import jloda.swing.director.IDirector;
import jloda.swing.graphview.EdgeActionListener;
import jloda.swing.graphview.INodeEdgeFormatable;
import jloda.swing.graphview.NodeActionListener;

import javax.swing.*;
import java.awt.*;
import java.util.Iterator;

/**
 * provides formatting services
 * Daniel Huson, 7.2010
 */
public class FormatterHelper implements INodeEdgeFormatable {
    final MultiViewer multiViewer;
    final TreeGrid treeGrid;

    /**
     * constructor
     *
     * @param multiViewer
     */
    public FormatterHelper(MultiViewer multiViewer) {
        this.multiViewer = multiViewer;
        this.treeGrid = multiViewer.getTreeGrid();
    }

    public Color getColorSelectedNodes() {
        Color a = null;
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            Color b = treeViewer.getColorSelectedNodes();
            if (a == null)
                a = b;
            else if (b != null && !a.equals(b))
                return null;
        }
        return a;
    }

    public Color getBackgroundColorSelectedNodes() {
        Color a = null;
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            Color b = treeViewer.getBackgroundColorSelectedNodes();
            if (a == null)
                a = b;
            else if (b != null && !a.equals(b))
                return null;
        }
        return a;
    }

    public Color getLabelColorSelectedNodes() {
        Color a = null;
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            Color b = treeViewer.getLabelColorSelectedNodes();
            if (a == null)
                a = b;
            else if (b != null && !a.equals(b))
                return null;
        }
        return a;
    }

    public Color getLabelBackgroundColorSelectedNodes() {
        Color a = null;
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            Color b = treeViewer.getLabelBackgroundColorSelectedNodes();
            if (a == null)
                a = b;
            else if (b != null && !a.equals(b))
                return null;
        }
        return a;
    }

    public boolean setColorSelectedNodes(Color a) {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setColorSelectedNodes(a);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
        return true;
    }

    public boolean setBackgroundColorSelectedNodes(Color a) {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setBackgroundColorSelectedNodes(a);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
        return true;
    }

    public boolean setLabelColorSelectedNodes(Color a) {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setLabelColorSelectedNodes(a);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
        return true;
    }

    public boolean setLabelBackgroundColorSelectedNodes(Color a) {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setLabelBackgroundColorSelectedNodes(a);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
        return true;
    }

    public Font getFontSelected() {
        Font a = null;
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            Font b = treeViewer.getFontSelected();
            if (a == null)
                a = b;
            else if (b != null && !a.equals(b))
                return null;
        }
        return a;
    }

    public int getWidthSelectedNodes() {
        int a = -1;
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            int b = treeViewer.getWidthSelectedNodes();
            if (a == -1)
                a = b;
            else if (b != -1 && a != b)
                return -1;
        }
        return a;
    }

    public int getHeightSelectedNodes() {
        int a = -1;
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            int b = treeViewer.getHeightSelectedNodes();
            if (a == -1)
                a = b;
            else if (b != -1 && a != b)
                return -1;
        }
        return a;
    }

    public int getLineWidthSelectedNodes() {
        int a = -1;
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            int b = treeViewer.getLineWidthSelectedNodes();
            if (a == -1)
                a = b;
            else if (b != -1 && a != b)
                return -1;
        }
        return a;
    }

    public byte getShapeSelectedNodes() {
        byte a = -1;
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            byte b = treeViewer.getShapeSelectedNodes();
            if (a == -1)
                a = b;
            else if (b != -1 && a != b)
                return -1;
        }
        return a;
    }

    public boolean setFontSelectedEdges(String family, int bold, int italics, int size) {
        boolean result = false;
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            if (treeViewer.setFontSelectedEdges(family, bold, italics, size)) {
                result = true;
                treeViewer.repaint();
                treeViewer.setDirty(true);
            }
        }
        return result;
    }

    public boolean setFontSelectedNodes(String family, int bold, int italics, int size) {
        boolean result = false;
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            if (treeViewer.setFontSelectedNodes(family, bold, italics, size)) {
                result = true;
                treeViewer.repaint();
                treeViewer.setDirty(true);
            }
        }
        return result;
    }

    public void setWidthSelectedNodes(byte a) {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setWidthSelectedNodes(a);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
    }

    public void setHeightSelectedNodes(byte a) {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setHeightSelectedNodes(a);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
    }

    public void setLineWidthSelectedNodes(byte a) {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setLineWidthSelectedNodes(a);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
    }

    public void setShapeSelectedNodes(byte a) {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setShapeSelectedNodes(a);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
    }

    public Color getColorSelectedEdges() {
        Color a = null;
        for (Connector connector : treeGrid.getConnectors()) {
            Color b = connector.getColor();
            if (a == null)
                a = b;
            else if (b != null && !a.equals(b))
                return null;
        }
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            Color b = treeViewer.getColorSelectedEdges();
            if (a == null)
                a = b;
            else if (b != null && !a.equals(b))
                return null;
        }
        return a;
    }

    public Color getLabelColorSelectedEdges() {
        Color a = null;
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            Color b = treeViewer.getLabelColorSelectedEdges();
            if (a == null)
                a = b;
            else if (b != null && !a.equals(b))
                return null;
        }
        return a;
    }

    public Color getLabelBackgroundColorSelectedEdges() {
        Color a = null;
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            Color b = treeViewer.getLabelBackgroundColorSelectedEdges();
            if (a == null)
                a = b;
            else if (b != null && !a.equals(b))
                return null;
        }
        return a;
    }

    public boolean setColorSelectedEdges(Color a) {
        for (Connector connector : treeGrid.getConnectors()) {
            if (connector.isSelected()) {
                connector.setColor(a);
            }
        }
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setColorSelectedEdges(a);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
        return true;
    }

    public boolean setLabelBackgroundColorSelectedEdges(Color a) {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setLabelBackgroundColorSelectedEdges(a);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
        return true;
    }

    public boolean setLabelColorSelectedEdges(Color a) {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setLabelColorSelectedEdges(a);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
        return true;
    }

    public int getLineWidthSelectedEdges() {
        int a = -1;
        for (Connector connector : treeGrid.getConnectors()) {
            int b = connector.getLineWidth();
            if (a == -1)
                a = b;
            else if (b != -1 && a != b)
                return -1;
        }

        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            int b = treeViewer.getLineWidthSelectedEdges();
            if (a == -1)
                a = b;
            else if (b != -1 && a != b)
                return -1;
        }
        return a;
    }

    public int getDirectionSelectedEdges() {
        int a = -1;
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            int b = treeViewer.getDirectionSelectedEdges();
            if (a == -1)
                a = b;
            else if (b != -1 && a != b)
                return -1;
        }
        return a;
    }

    public byte getShapeSelectedEdges() {
        byte a = -1;
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            byte b = treeViewer.getShapeSelectedEdges();
            if (a == -1)
                a = b;
            else if (b != -1 && a != b)
                return -1;
        }
        return a;
    }

    public void setLineWidthSelectedEdges(byte a) {
        for (Connector connector : treeGrid.getConnectors()) {
            if (connector.isSelected()) {
                connector.setLineWidth(a);
            }
        }

        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setLineWidthSelectedEdges(a);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
    }

    public void setDirectionSelectedEdges(byte a) {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setDirectionSelectedEdges(a);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
    }

    public void setShapeSelectedEdges(byte a) {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setShapeSelectedEdges(a);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
    }

    public void addNodeActionListener(NodeActionListener nal) {
    }

    public void removeNodeActionListener(NodeActionListener nal) {
    }

    public void addEdgeActionListener(EdgeActionListener eal) {
    }

    public void removeEdgeActionListener(EdgeActionListener eal) {
    }

    public boolean hasSelectedNodes() {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            if (treeViewer.hasSelectedNodes())
                return true;
        }

        return false;
    }

    public boolean hasSelectedEdges() {

        for (Connector connector : treeGrid.getConnectors()) {
            if (connector.isSelected()) {
                return true;
            }
        }

        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            if (treeViewer.hasSelectedEdges())
                return true;
        }

        return false;
    }

    public void setLabelVisibleSelectedNodes(boolean a) {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setLabelVisibleSelectedNodes(a);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
    }

    public boolean hasLabelVisibleSelectedNodes() {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            if (treeViewer.hasLabelVisibleSelectedNodes())
                return true;
        }

        return false;
    }

    public void setLabelVisibleSelectedEdges(boolean a) {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setLabelVisibleSelectedEdges(a);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
    }

    public boolean hasLabelVisibleSelectedEdges() {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            if (treeViewer.hasLabelVisibleSelectedEdges())
                return true;
        }

        return false;
    }

    public void repaint() {
        // we already call repaint on each TreeViewer in the other methods. He we use the repaint call to update the dirty state of the document and fix the title
        boolean isDirty = multiViewer.getDir().getDocument().isDocumentIsDirty();
        if (!isDirty) {
            for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
                TreeViewer treeViewer = it.next();
                if (treeViewer.isDirty()) {
                    multiViewer.getDir().getDocument().setDocumentIsDirty(true);
                    multiViewer.updateView(IDirector.TITLE);
                }
            }
        }
    }

    public boolean getLockXYScale() {
        return false;
    }

    public void rotateLabelsSelectedNodes(int a) {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.rotateLabelsSelectedNodes(a);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
    }

    public void rotateLabelsSelectedEdges(int a) {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.rotateLabelsSelectedEdges(a);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
    }

    public JPanel getPanel() {
        return treeGrid;
    }

    public JScrollPane getScrollPane() {
        return null;
    }

    public void setRandomColorsSelectedNodes(boolean foreground, boolean background, boolean labelforeground, boolean labelbackgrond) {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setRandomColorsSelectedNodes(foreground, background, labelforeground, labelbackgrond);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
    }

    public void setRandomColorsSelectedEdges(boolean foreground, boolean labelforeground, boolean labelbackgrond) {
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            treeViewer.setRandomColorsSelectedEdges(foreground, labelforeground, labelbackgrond);
            treeViewer.repaint();
            treeViewer.setDirty(true);
        }
    }

    public EdgeSet getSelectedEdges() {
        return null;
    }

    public NodeSet getSelectedNodes() {
        return null;
    }

    public boolean selectAllNodes(boolean select) {
        boolean changed = false;
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            if (treeViewer.selectAllNodes(select) && !changed)
                changed = true;
        }
        return changed;
    }

    public boolean selectAllEdges(boolean select) {
        boolean changed = false;
        for (Iterator<TreeViewer> it = treeGrid.getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            if (treeViewer.selectAllEdges(select) && !changed)
                changed = true;
        }
        return changed;
    }

    public JFrame getFrame() {
        return multiViewer.getFrame();
    }
}
