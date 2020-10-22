/*
 *   ButtonPanel.java Copyright (C) 2020 Daniel H. Huson
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
import dendroscope.hybroscale.model.HybridManager.Computation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ButtonPanel extends JPanel implements ActionListener {

    private Computation computation;
    private int cores;
    private Integer maxK;
    private static final long serialVersionUID = 1L;
    private HybroscaleController controller;
    private JButton runButton, closeButton, setCoresButton, editModeButton, addConstraints;
    private JCheckBox upperBound;

    public ButtonPanel(HybroscaleController controller, Computation computation, int cores, Integer maxK) {

        this.controller = controller;
        this.computation = computation;
        this.cores = cores;
        this.maxK = maxK;

        setLayout(new BorderLayout());
        JPanel config = new JPanel();

        upperBound = new JCheckBox("Upper Bound");
//		config.add(upperBound);
        upperBound.addActionListener(this);

        addConstraints = new JButton("Add Constraints");
        config.add(addConstraints);
        addConstraints.addActionListener(this);

        setCoresButton = new JButton("Set Cores");
        config.add(setCoresButton);
        setCoresButton.addActionListener(this);

        closeButton = new JButton("Close");
        config.add(closeButton);
        closeButton.addActionListener(this);

        runButton = new JButton("Run");
        config.add(runButton);
        runButton.addActionListener(this);

        add(config, BorderLayout.EAST);

        repaint();

    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource().equals(runButton)) {
            controller.run(computation, cores, maxK, upperBound.isSelected());
            controller.disableConstraintWindow();
            addConstraints.setText("View Constraints");
            setCoresButton.setEnabled(false);
            runButton.setEnabled(false);
            upperBound.setEnabled(false);
            closeButton.setText("Cancel");
        } else if (arg0.getSource().equals(closeButton)) {
            controller.stop(true);
            closeButton.setText("Close");
        } else if (arg0.getSource().equals(setCoresButton))
            controller.showProcWindow();
        else if (arg0.getSource().equals(addConstraints))
            controller.showConstraintWindow();
        repaint();
    }

    public void compFinished() {
        closeButton.setText("Close");
    }

    public void enableEditTrees() {
        editModeButton.setEnabled(true);
    }

    public JButton getRunButton() {
        return runButton;
    }
}
