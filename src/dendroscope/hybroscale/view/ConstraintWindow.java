/*
 *   ConstraintWindow.java Copyright (C) 2020 Daniel H. Huson
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

public class ConstraintWindow extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JScrollPane scrollPane;
    private JRadioButton isTimeConsistent, addTaxa, level;
    private ButtonGroup buttonGroup;
    private JTabbedPane tabbedPane;
    private JButton addNetConstraintButton;

    private JButton undoButton;
    private JPanel textPanel;
    private JTextPane textArea;
    private String initText = "SELECT\nall networks with minimum hybridization number\nWHERE\n";

    private JPanel optConPanel;

    private int width = 600, height = 400;
    private JButton okay;

    private boolean firstConstraint = true;

    public ConstraintWindow(HybridView hView, Vector<String> taxa) {

        setResizable(false);

        String[] taxaArray = new String[taxa.size()];
        Iterator<String> it = taxa.iterator();
        Vector<String> sortedTaxa = new Vector<String>();
        for (String s : taxa)
            sortedTaxa.add(s);
        Collections.sort(sortedTaxa);
        for (int i = 0; i < sortedTaxa.size(); i++)
            taxaArray[i] = it.next();

        String[] negConstraintArray = {"Must be within a purely tree-like structure."};

        setTitle("Enter Constraints!");
        setAlwaysOnTop(true);
        setSize(new Dimension(width, height));

        GridBagLayout gbl = new GridBagLayout();
        setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        tabbedPane = new JTabbedPane();
        tabbedPane.setPreferredSize(new Dimension(width, 185));
        tabbedPane.setSize(new Dimension(width, 185));
        gbl.setConstraints(tabbedPane, gbc);
        add(tabbedPane);

        // Network constraints ***************************

        JPanel networkPanel = new JPanel();
        networkPanel.setLayout(new BoxLayout(networkPanel, BoxLayout.Y_AXIS));

        JPanel radioPanel = new JPanel(new GridLayout(0, 1));
        buttonGroup = new ButtonGroup();

        isTimeConsistent = new JRadioButton("Network has best time consistent value.");
        isTimeConsistent.setActionCommand("Network has best time consistent value.");
        isTimeConsistent.setSelected(true);
        isTimeConsistent.addActionListener(this);
        buttonGroup.add(isTimeConsistent);
        radioPanel.add(isTimeConsistent);

        addTaxa = new JRadioButton("Network has best add-taxa value.");
        addTaxa.setActionCommand("Network has best add-taxa value.");
        addTaxa.setSelected(true);
        addTaxa.addActionListener(this);
        buttonGroup.add(addTaxa);
        radioPanel.add(addTaxa);

        level = new JRadioButton("Network is best level-k network.");
        level.setActionCommand("Network is best level-k network.");
        level.setSelected(true);
        level.addActionListener(this);
        buttonGroup.add(level);
//		radioPanel.add(level);

        networkPanel.add(radioPanel);

        addNetConstraintButton = new JButton("ADD NETWORK CONSTRAINT");
        addNetConstraintButton.addActionListener(this);
        addNetConstraintButton.setMaximumSize(new Dimension(width / 2, 30));
        addNetConstraintButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        networkPanel.add(addNetConstraintButton);

        tabbedPane.add(networkPanel, "Network Constraints");

        // *************************************************

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel();

        undoButton = new JButton("UNDO");
        undoButton.addActionListener(this);
        undoButton.setMaximumSize(new Dimension(width / 2, 30));
        undoButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(undoButton);

        gbl.setConstraints(buttonPanel, gbc);
        add(buttonPanel);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        textPanel = new JPanel();
        textPanel.setBorder(BorderFactory.createTitledBorder("Added Constraints"));
        gbl.setConstraints(textPanel, gbc);
        add(textPanel);

        textArea = new JTextPane();
        scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(width, height - 205));
        scrollPane.setSize(new Dimension(width, height - 205));
        textPanel.add(scrollPane);
        textArea.setText(initText);
        textArea.setEditable(false);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        okay = new JButton("OK");
        okay.addActionListener(this);
        gbl.setConstraints(okay, gbc);
        add(okay);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                textArea.setText("");
                isTimeConsistent.setSelected(false);
            }
        });

        setVisible(false);
        pack();
        setLocationRelativeTo(hView);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(addNetConstraintButton)) {
            String text = textArea.getText();
            Enumeration<AbstractButton> it = buttonGroup.getElements();
            while (it.hasMoreElements()) {
                AbstractButton button = it.nextElement();
                if (button.isSelected()) {
                    if (firstConstraint)
                        text = text.concat("" + button.getText() + "\n");
                    else
                        text = text.concat("AND\n" + button.getText() + "\n");
                    firstConstraint = false;
                }
            }
            textArea.setText(text);
        } else if (e.getSource().equals(undoButton)) {
            String text = textArea.getText();
            String[] splitText = text.split("\n");
            if (splitText.length > 3) {
                String newText = "";
                int border = splitText.length == 4 ? 3 : splitText.length - 2;
                for (int i = 0; i < border; i++)
                    newText = newText.concat(splitText[i] + "\n");
                textArea.setText(newText);
                if (splitText.length == 4)
                    firstConstraint = true;
            }
        } else if (e.getSource().equals(okay))
            this.setVisible(false);
    }

    public String getConstraints() {
        return textArea.getText();
    }

    public void disableAdding() {
        isTimeConsistent.setEnabled(false);
        textArea.setEditable(false);
        addNetConstraintButton.setEnabled(false);
        undoButton.setEnabled(false);
        addTaxa.setEnabled(false);
    }

}
