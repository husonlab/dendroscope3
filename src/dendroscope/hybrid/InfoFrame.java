/**
 * InfoFrame.java 
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

public class InfoFrame extends JDialog {
    private static final long serialVersionUID = 1L;

    private final int width = 400;
    private final int height = 200;

    private final JTextArea text = new JTextArea();

    public InfoFrame(String title, String message) {
        super();

        setModal(true);
        setTitle(title);
        setAlwaysOnTop(true);
        setSize(width, height);
        setLocation((Toolkit.getDefaultToolkit().getScreenSize().width / 2)
                        - (width / 2),
                (Toolkit.getDefaultToolkit().getScreenSize().height / 2)
                        - (height / 2));

        Container main = getContentPane();
        main.setLayout(new BorderLayout());

        JPanel middle = new JPanel();
        middle.setLayout(new BorderLayout());
        middle.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        text.setEditable(false);
        text.setWrapStyleWord(true);
        text.setLineWrap(true);
        text.setBackground(main.getBackground());
        text.setText(message);

        middle.add(new JScrollPane(text), BorderLayout.CENTER);
        main.add(middle, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BorderLayout());

        JButton closeButton = new JButton(getCloseAction());
        bottom.add(closeButton, BorderLayout.EAST);
        rootPane.setDefaultButton(closeButton);

        main.add(bottom, BorderLayout.SOUTH);

    }

    public AbstractAction getCloseAction() {
        AbstractAction action = new AbstractAction() {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent event) {
                InfoFrame.this.dispose();
            }
        };
        action.putValue(AbstractAction.NAME, "Close");
        return action;
    }

    public void setInfo(String message) {
        text.setText(message);
    }
}
