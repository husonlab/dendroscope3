/*
 * Copyright (C) This is third party code.
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
