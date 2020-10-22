/*
 *   HybridView.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.view;

import dendroscope.hybroscale.controller.HybroscaleController;
import dendroscope.hybroscale.model.ClusterThread;
import dendroscope.hybroscale.model.HybridManager.Computation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class HybridView extends JFrame implements Runnable {

    private boolean isVisible = true;

    private int width = 650;
    private int height = (Toolkit.getDefaultToolkit().getScreenSize().height / 4);

    private static final long serialVersionUID = 1L;
    private ButtonPanel bP;
    private ClusterTable cT;
    private StatusBar statusBar = new StatusBar();

    public HybridView(JFrame mainFrame, final HybroscaleController controller, Computation computation, int cores, Integer maxK) {

        cT = new ClusterTable(statusBar, width, this);
        setLayout(new BorderLayout());

        if (computation == Computation.EDGE_NETWORK)
            setTitle("Hybridization Networks");
        else if (computation == Computation.EDGE_NUMBER)
            setTitle("Hybridization Number");
        bP = new ButtonPanel(controller, computation, cores, maxK);
        getRootPane().setDefaultButton(bP.getRunButton());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                controller.stop(true);
            }
        });

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.ipady = height;
        c.ipadx = width - 35;
        add(new JScrollPane(cT), c);

        c.ipady = 0;
        c.ipadx = 1;
        c.gridy = 1;
        add(bP, c);

        c.ipadx = 3;
        c.gridy = 2;
        add(statusBar, c);

        setResizable(false);
        setPreferredSize(new Dimension(width, height + 130));
        setFocusable(true);
        setAlwaysOnTop(true);
        pack();
        setLocationRelativeTo(mainFrame);
        super.setVisible(isVisible);

    }

    public void run() {
        JOptionPane
                .showMessageDialog(this,
                        "Make sure if incongruence is expected to be due to reticulate\n evolution and not due to incomplete lineage sorting.");
        repaint();
    }

    public void setInfo(String s) {
        statusBar.setInfo(s);
        repaint();
    }

    public void updateTime(long time) {
        statusBar.updateTime(time);
    }

    public void reportRetNetworks(int num, int edgeNumber) {
        statusBar.reportRetNetworks(num, edgeNumber);
        bP.compFinished();
    }

    public void reportEdgeNumber(int edgeNumber) {
        statusBar.reportEdgeNumber(edgeNumber);
        bP.compFinished();
    }

    public void updateProc(int num) {
        statusBar.updateProc(num);
    }

    public void addClusterThread(ClusterThread thread) {
        cT.addClusterThread(thread);
    }

    public void stopClusterThread(ClusterThread thread) {
        cT.stopCluster(thread);
        setProgress(thread, -1);
        setProgress(thread, -1);
    }

    public void finishClusterThread(ClusterThread thread) {
        cT.finishCluster(thread);
        setProgress(thread, 100);
    }

    public void setStatus(ClusterThread thread, String info) {
        cT.setStatus(thread, info);
    }

    public void setProgress(ClusterThread thread, int i) {
        cT.setProgress(thread, i);
    }

    public void enableMarkingTrees() {
        bP.enableEditTrees();
    }

    public void createInfoFrame(String title, String text) {
        InfoFrame frame = new InfoFrame(title, text);
        frame.setLocationRelativeTo(this);
        frame.setVisible(true);
    }

    public void setDetails(ClusterThread thread, String details) {
        cT.setDetails(thread, details);
    }

    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    public void showProblematicConstraints(String badConstraints) {
        JOptionPane.showMessageDialog(this, "The following constraint(s) can not be realized:\n" + badConstraints);
        bP.compFinished();
        statusBar.setInfo("No result computed - please check constraints!");
        cT.stopAllCluster();
    }

}
