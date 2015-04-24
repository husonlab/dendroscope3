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
