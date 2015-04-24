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
