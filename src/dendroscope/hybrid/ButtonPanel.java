/**
 * ButtonPanel.java 
 * Copyright (C) 2018 Daniel H. Huson
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ButtonPanel extends JPanel implements ActionListener {

    private final View.Computation compValue;
    private static final long serialVersionUID = 1L;
    private final Controller controller;
    private final JButton runButton;
    private final JButton closeButton;
    private final JButton setCoresButton;
    private JButton editModeButton;
    private JCheckBox useCA;
    private View view;
    private EditFrame editFrame;

    public ButtonPanel(Controller controller, View.Computation compValue) {

        this.controller = controller;
        this.compValue = compValue;

        setLayout(new BorderLayout());

        if (compValue == View.Computation.NETWORK) {

            JPanel edit = new JPanel();

            editModeButton = new JButton("Support Window");
            edit.add(editModeButton);
            editModeButton.addActionListener(this);
            editModeButton.setEnabled(false);

            add(edit, BorderLayout.WEST);

        }

        JPanel config = new JPanel();

        if (compValue != View.Computation.rSPR_DISTANCE) {
            useCA = new JCheckBox("Combinatorial Approach");
//			config.add(useCA);
            useCA.setSelected(false);
            useCA.addActionListener(this);
        }

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
            if (useCA != null) {
                controller.run(compValue, useCA.isSelected());
                useCA.setEnabled(false);
            } else
                controller.run(compValue, false);
            setCoresButton.setEnabled(false);
            runButton.setEnabled(false);
        } else if (arg0.getSource().equals(closeButton))
            controller.stop();
        else if (arg0.getSource().equals(setCoresButton))
            controller.showProcWindow();
        else if (arg0.getSource().equals(editModeButton)) {
            view.setVisible(false);
            editFrame.setVisible(true);
        } else if (arg0.getSource().equals(useCA)) {
            useCA.setSelected(true);
        }
        repaint();
    }

    public void enableEditTrees() {
        editModeButton.setEnabled(true);
    }

    public void setView(View view) {
        this.view = view;
        editFrame = new EditFrame(view, controller);
    }

    public JButton getRunButton() {
        return runButton;
    }
}
