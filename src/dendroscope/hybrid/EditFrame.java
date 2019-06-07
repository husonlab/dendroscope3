/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class EditFrame extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    private final Controller controller;
    private final JButton selectT1;
    private final JButton selectT2;
    private final JCheckBox nodeSuppports;

    public EditFrame(final View view, Controller controller) {
        this.controller = controller;

        setTitle("Support Window");

        JPanel edit = new JPanel();
        selectT1 = new JButton("Select Tree 1");
        edit.add(selectT1);
        selectT1.addActionListener(this);

        selectT2 = new JButton("Select Tree 2");
        edit.add(selectT2);
        selectT2.addActionListener(this);

        nodeSuppports = new JCheckBox("Show Node Supports");
        edit.add(nodeSuppports);
        nodeSuppports.addActionListener(this);

        add(edit);
        setVisible(false);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                view.setVisible(true);
            }
        });

        setAlwaysOnTop(true);
        pack();

        setLocation((Toolkit.getDefaultToolkit().getScreenSize().width / 2)
                        - (getWidth() / 2),
                90);

    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource().equals(selectT1)) {
            controller.selectCommonEdges();
            controller.markTrees(true);
        } else if (arg0.getSource().equals(selectT2)) {
            controller.selectCommonEdges();
            controller.markTrees(false);
        } else if (arg0.getSource().equals(nodeSuppports)) {
            if (nodeSuppports.isSelected())
                controller.showNodeOcc();
            else
                controller.hideNodeOcc();
        }
        repaint();
    }

}
