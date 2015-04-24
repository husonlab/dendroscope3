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

public class ProcWindow extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;
    private final JPanel panel = new JPanel();
    private final Controller controller;

    final JTextField number;

    public ProcWindow(Controller controller) {
        this.controller = controller;
        panel.setLayout(new FlowLayout());

        JLabel text = new JLabel("Available Cores: ");
        panel.add(text);

        number = new JTextField();
        number.setText("" + (Runtime.getRuntime().availableProcessors() - 1));
        number.setColumns(10);
        panel.add(number);

        JButton ok = new JButton("OK");
        ok.addActionListener(this);
        panel.add(ok);
        add(panel);

        setMinimumSize(new Dimension(100, 0));
        setAlwaysOnTop(true);

    }

    public void actionPerformed(ActionEvent arg0) {
        try {
            Integer num = Integer.parseInt(number.getText());
            controller.setCores(num);
            setVisible(false);
        } catch (Exception e) {
            number.setText("invalid number");
        }
    }

}
