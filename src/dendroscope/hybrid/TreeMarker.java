/**
 * TreeMarker.java 
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
package dendroscope.hybrid;

import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.graph.Edge;
import jloda.graph.Graph;

import java.awt.*;
import java.util.Iterator;
import java.util.Vector;

public class TreeMarker {

    private final Vector<Edge> edgesOfclusterNetworks = new Vector<>();
    private final Vector<Edge> specialEdgesOfT1 = new Vector<>();
    private Vector<HybridNetwork> networks;

    public void init(MultiViewer mV) {

        if (edgesOfclusterNetworks.size() != 0) {

            Iterator<TreeViewer> it = mV.getTreeGrid().getIterator();

            while (it.hasNext()) {

                TreeViewer viewer = it.next();
                Graph g = viewer.getGraph();

                Iterator<Edge> itEdges = g.edgeIterator();
                viewer.selectAllEdges(false);
                while (itEdges.hasNext()) {
                    Edge e = itEdges.next();
                    if (e.getInfo().equals("-1"))
                        viewer.setSelected(e, true);
                }
                mV.getFormattingHelper()
                        .setColorSelectedEdges(Color.LIGHT_GRAY);
                viewer.repaint();
                viewer.selectAllEdges(false);

            }

        }

    }

    public void markReticulateEdges(boolean treeT1, MultiViewer mV) {

        for (Iterator<TreeViewer> it = mV.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {

            TreeViewer viewer = it.next();
            Graph g = viewer.getGraph();

            Iterator<Edge> itEdges = g.edgeIterator();
            while (itEdges.hasNext()) {
                Edge e = itEdges.next();
                if (treeT1) {
                    if (e.getInfo().equals("1"))
                        viewer.setSelected(e, true);
                    else if (e.getInfo().equals("2"))
                        viewer.setSelected(e, false);
                } else if (e.getInfo().equals("2"))
                    viewer.setSelected(e, true);
                else if (e.getInfo().equals("1"))
                    viewer.setSelected(e, false);
            }

            viewer.repaint();

        }

    }

    public void markTreeEdges(MultiViewer mV) {

        for (Iterator<TreeViewer> it = mV.getTreeGrid()
                .getSelectedOrAllIterator(); it.hasNext(); ) {

            TreeViewer viewer = it.next();
            Graph g = viewer.getGraph();

            Iterator<Edge> itEdges = g.edgeIterator();
            while (itEdges.hasNext()) {
                Edge e = itEdges.next();
                if (e.getInfo().equals("0"))
                    viewer.setSelected(e, true);
            }

            viewer.repaint();

        }

    }

    public void unmark(MultiViewer mV) {
        for (Iterator<TreeViewer> it = mV.getTreeGrid()
                .getSelectedOrAllIterator(); it.hasNext(); ) {

            TreeViewer viewer = it.next();
            Graph g = viewer.getGraph();

            Iterator<Edge> itEdges = g.edgeIterator();
            viewer.selectAllEdges(false);
            while (itEdges.hasNext()) {
                Edge e = itEdges.next();
                if (g.isSpecial(e) && !e.getInfo().equals("-1"))
                    viewer.setSelected(e, true);
            }

            mV.getFormattingHelper().setColorSelectedEdges(Color.BLUE);
            viewer.repaint();
            viewer.selectAllEdges(false);

        }
        for (Iterator<TreeViewer> it = mV.getTreeGrid()
                .getSelectedOrAllIterator(); it.hasNext(); ) {

            TreeViewer viewer = it.next();
            Graph g = viewer.getGraph();

            Iterator<Edge> itEdges = g.edgeIterator();
            viewer.selectAllEdges(false);
            while (itEdges.hasNext()) {
                Edge e = itEdges.next();
                if (!g.isSpecial(e) && !e.getInfo().equals("-1"))
                    viewer.setSelected(e, true);
            }

            mV.getFormattingHelper().setColorSelectedEdges(Color.BLACK);
            viewer.repaint();
            viewer.selectAllEdges(false);

        }

    }

    public void initT1Edges() {
        for (HybridNetwork n : networks) {
            Iterator<Edge> it = n.edgeIterator();
            while (it.hasNext()) {
                Edge e = it.next();
                if (edgesOfclusterNetworks.contains(e))
                    e.setInfo("-1");
                else if (n.isSpecial(e)) {
                    if (specialEdgesOfT1.contains(e))
                        e.setInfo("1");
                    else
                        e.setInfo("2");
                } else
                    e.setInfo("0");
            }
        }
    }

    public void insertT1Edge(Edge e) {
        if (!specialEdgesOfT1.contains(e))
            specialEdgesOfT1.add(e);
    }

    public boolean contains(Edge e) {
        return specialEdgesOfT1.contains(e);
    }

    public void printEdges() {
        System.err.println("specialEdgesOfT1.size(): "
                + specialEdgesOfT1.size());
    }

    public void setNetworks(Vector<HybridNetwork> networks) {
        this.networks = networks;
    }

    public void assignEdges(HybridNetwork n1, HybridNetwork n2) {

        // keeping track of special t1 edges
        Iterator<Edge> it = n1.edgeIterator();
        Vector<Integer> edgePos = new Vector<>();
        int i = 0;
        while (it.hasNext()) {
            Edge e = it.next();
            if (specialEdgesOfT1.contains(e))
                edgePos.add(i);
            i++;
        }
        it = n2.edgeIterator();
        i = 0;
        while (it.hasNext()) {
            Edge e = it.next();
            if (edgePos.contains(i))
                insertT1Edge(e);
            i++;
        }

        // adding edges of a cluster network
        it = n1.edgeIterator();
        edgePos.clear();
        i = 0;
        while (it.hasNext()) {
            Edge e = it.next();
            if (edgesOfclusterNetworks.contains(e))
                edgePos.add(i);
            i++;
        }
        it = n2.edgeIterator();
        i = 0;
        while (it.hasNext()) {
            Edge e = it.next();
            if (edgePos.contains(i))
                edgesOfclusterNetworks.add(e);
            i++;
        }
    }

    public void addClusterNetworkEdge(Edge e) {
        edgesOfclusterNetworks.add(e);
    }
}
