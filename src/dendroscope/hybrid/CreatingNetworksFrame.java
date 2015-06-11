/**
 * CreatingNetworksFrame.java 
 * Copyright (C) 2015 Daniel H. Huson
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
