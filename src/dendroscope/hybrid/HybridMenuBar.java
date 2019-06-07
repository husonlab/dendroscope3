/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class HybridMenuBar extends JMenuBar implements ActionListener {

    private final View.Computation compValue;
    private int id = 1;
    private static final long serialVersionUID = 1L;
    private final Controller controller;
    private final JMenuItem start;
    private final JMenuItem close;
    private final JMenuItem setProcessors;
    private final JMenuItem selectT1;
    private final JMenuItem selectT2;
    private final JMenuItem selectCommon;
    private final JMenuItem unmark;
    private final JMenuItem showOcc;
    private final JMenuItem hideOcc;

    private final JMenu selectTrees;
    private final JMenu editTree;

    public HybridMenuBar(Controller controller, View.Computation compValue) {

        this.compValue = compValue;
        this.controller = controller;

        JMenu file = new JMenu("File");
        add(file);
        start = new JMenuItem("Start");
        start.addActionListener(this);
        file.add(start);
        close = new JMenuItem("Close");
        close.addActionListener(this);
        file.add(close);

        JMenu edit = new JMenu("Edit");
        add(edit);
        setProcessors = new JMenuItem("#Cores");
        setProcessors.addActionListener(this);
        edit.add(setProcessors);

        selectTrees = new JMenu("Select Trees");
        add(selectTrees);
        selectT1 = new JMenuItem("Select Edges Of 1st Tree  ");
        selectT1.addActionListener(this);
        selectTrees.add(selectT1);
        selectT2 = new JMenuItem("Select Edges of 2nd Tree");
        selectT2.addActionListener(this);
        selectTrees.add(selectT2);
        selectCommon = new JMenuItem("Select Common Edges");
        selectCommon.addActionListener(this);
        selectTrees.add(selectCommon);
        unmark = new JMenuItem("Unmark Edges");
        unmark.addActionListener(this);
        selectTrees.add(unmark);
        selectTrees.setEnabled(false);

        editTree = new JMenu("Edit Tree");
        add(editTree);
        showOcc = new JMenuItem("Show Node Occurrences");
        showOcc.addActionListener(this);
        editTree.add(showOcc);
        hideOcc = new JMenuItem("Hide Node Occurrences");
        hideOcc.addActionListener(this);
        editTree.add(hideOcc);
        editTree.setEnabled(false);

        repaint();
    }

    public void enableMarkingTrees() {
        selectTrees.setEnabled(true);
        editTree.setEnabled(true);
    }

    public void addClusterThread(ClusterThread cT) {
        JCheckBoxMenuItem box = new JCheckBoxMenuItem("Thread " + id);
        id++;

        box.setSelected(false);
        box.addActionListener(this);

        repaint();
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource().equals(start)) {
            controller.run(compValue, false);
            setProcessors.setEnabled(false);
            start.setEnabled(false);
        } else if (arg0.getSource().equals(close))
            controller.stop();
        else if (arg0.getSource().equals(setProcessors))
            controller.showProcWindow();
        else if (arg0.getSource().equals(selectT1)) {
            controller.selectCommonEdges();
            controller.markTrees(true);
        } else if (arg0.getSource().equals(selectT2)) {
            controller.selectCommonEdges();
            controller.markTrees(false);
        } else if (arg0.getSource().equals(selectCommon))
            controller.selectCommonEdges();
        else if (arg0.getSource().equals(unmark))
            controller.unmarkTrees();
        else if (arg0.getSource().equals(showOcc))
            controller.showNodeOcc();
        else if (arg0.getSource().equals(hideOcc))
            controller.hideNodeOcc();
        repaint();
    }

}
