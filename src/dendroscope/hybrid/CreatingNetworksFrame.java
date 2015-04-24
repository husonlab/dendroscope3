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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class CreatingNetworksFrame extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    private final int width = 300;
    private final int height = 150;

    private final JLabel info = new JLabel("0 hybridization networks computed");
    private final JButton abort = new JButton("Abort");
    private final ReattachNetworks rN;

    public CreatingNetworksFrame(final ReattachNetworks rN) {

        this.rN = rN;

        setLayout(new BorderLayout());
        setTitle("Computing networks...");

        info.setSize(300, 300);
        info.setHorizontalAlignment(SwingUtilities.CENTER);
        info.setHorizontalTextPosition(SwingUtilities.CENTER);
        info.setVerticalTextPosition(SwingUtilities.CENTER);

        abort.addActionListener(this);
        add(info, BorderLayout.CENTER);
        add(abort, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                rN.stopThread();
            }
        });

        setLocation(
                (Toolkit.getDefaultToolkit().getScreenSize().width / 2) - (width / 2),
                (Toolkit.getDefaultToolkit().getScreenSize().height / 2) - (height / 2));
        setSize(width, height);
        setAlwaysOnTop(true);

    }

    public void setInfo(String text) {
        info.setText(text);
        repaint();
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource().equals(abort))
            rN.stopThread();
    }

}
