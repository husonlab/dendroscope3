/*
 *   NewickInputDialog.java Copyright (C) 2023 Daniel H. Huson
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
package dendroscope.util;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;


/**
 * interactive Newick parser
 * Daniel Huson and Robin Forster 1.2010
 */
public class NewickInputDialog extends JDialog implements DocumentListener, CaretListener, ItemListener {

    private static final Color colorMatch = Color.YELLOW;
    private static final Color colorError = Color.RED;

    final private JTextArea inputField;
    final private JButton okButton;
    final private JButton cancelButton;
    final private JTextField message;
    final private JCheckBox edgeLengthCBox;
    final private Highlighter highlighter;
    final private MyHighlightPainter painterMatch;
    final private MyHighlightPainter painterError;

    private int errorPosition;
    private String result = null;

    /**
     * constructor
     *
	 */
    public NewickInputDialog(JFrame parent, String programName, String defaultValue, boolean edgeLengths) {
        super(parent);
        this.setSize(400, 155);
        if (parent != null)
            setLocationRelativeTo(parent);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setModal(true);
        setTitle("Enter trees or networks - " + programName);
        errorPosition = -1;
        Container container = getContentPane();
        container.setLayout(new BorderLayout());
        JLabel label = new JLabel("Enter a tree or network in Newick format:");
        label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        container.add(label, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());

        inputField = new JTextArea();
        inputField.addCaretListener(this);
        inputField.getDocument().addDocumentListener(this);
        highlighter = inputField.getHighlighter();
        painterMatch = new MyHighlightPainter(colorMatch);
        painterError = new MyHighlightPainter(colorError);
        message = new JTextField("");
        message.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 0));
        message.setEditable(false);
        message.setBackground(container.getBackground());
        message.setForeground(Color.RED);
        centerPanel.add(new JScrollPane(inputField), BorderLayout.CENTER);
        centerPanel.add(message, BorderLayout.SOUTH);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        container.add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEtchedBorder());
        edgeLengthCBox = new JCheckBox("Edge lengths");
        edgeLengthCBox.setSelected(edgeLengths);
        edgeLengthCBox.addItemListener(this);
        buttonPanel.add(edgeLengthCBox);

        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        okButton = new JButton(getOkAction());

        getRootPane().setDefaultButton(okButton);

        cancelButton = new JButton(getCancelAction());
        okButton.setEnabled(checkNewick(inputField.getText()));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        container.add(buttonPanel, BorderLayout.SOUTH);

        if (defaultValue != null)
            inputField.setText(defaultValue.trim());
    }

    private void highlightBrackets(String s, int pos) {
        try {
            if (s.charAt(pos - 1) == ')') {
                highlighter.addHighlight(pos - 1, pos, painterMatch);
                int closed = 0;
                for (int i = pos - 2; i >= 0; i--) {
                    if (s.charAt(i) == ')') {
                        closed++;
                    } else if (s.charAt(i) == '(') {
                        if (closed == 0) {
                            highlighter.addHighlight(i, i + 1, painterMatch);
                            break;
                        } else {
                            closed--;
                        }
                    }
                }
            } else if (s.charAt(pos - 1) == '(') {
                int open = 0;
                for (int i = pos; i < s.length(); i++) {
                    if (s.charAt(i) == '(') {
                        open++;
                    } else if (s.charAt(i) == ')') {
                        if (open == 0) {
                            highlighter.addHighlight(pos - 1, pos, painterMatch);
                            highlighter.addHighlight(i, i + 1, painterMatch);
                            break;
                        } else {
                            open--;
                        }
                    }
                }
            }
        } catch (BadLocationException ignored) {
        }
    }

    private void highlightRNodes(String s, int pos) {
        if (Character.isDigit(s.charAt(pos - 1)) || Character.isLetter(s.charAt(pos - 1))
                || s.charAt(pos - 1) == '#') {
            String name_s = "";
            String name_e = "";
            String name;
            for (int i = pos - 1; i >= 0; i--) {
                if (!(Character.isDigit(s.charAt(i)) || Character.isLetter(s.charAt(i)))
                        && s.charAt(i) != '#') {
                    break;
                } else if (s.charAt(i) == '#') {
                    name_s = s.substring(i, pos);
                }
            }
            if (name_s.length() > 0) {
                for (int i = pos; i < s.length(); i++) {
                    if (!(Character.isDigit(s.charAt(i)) || Character.isLetter(s.charAt(i))
                            || s.charAt(i) == '#')) {
                        break;
                    } else {
                        name_e = s.substring(pos, i + 1);
                    }
                }
                name = name_s + name_e;
                if (name.length() < 2) {
                    return;
                }
                if (name.charAt(1) == '#') {
                    name = name.substring(1);
                }
                int lastpos = 0;
                int startpos = 0;
                int endpos = 0;
                while ((lastpos = s.indexOf(name, lastpos)) != -1) {
                    startpos = lastpos;
                    if (startpos != 0) {
                        if (s.charAt(startpos - 1) == '#') {
                            startpos = startpos - 1;
                        }
                    }
                    endpos = lastpos + name.length();
                    boolean hglt = false;
                    if (endpos == s.length()) {
                        hglt = true;
                    } else if (!Character.isDigit(s.charAt(endpos))
                            && !Character.isLetter(s.charAt(endpos))) {
                        hglt = true;
                    }
                    if (hglt) {
                        try {
                            highlighter.addHighlight(startpos, endpos, painterMatch);
                        } catch (BadLocationException ignored) {
                        }
                    }
                    lastpos = endpos;
                }
            }
        }
    }

    private void highlightErrorPosition() {
        try {
            highlighter.addHighlight(errorPosition, errorPosition + 1, painterError);
        } catch (BadLocationException ignored) {
        }
    }

    private void updateHighlighter() {
        Highlighter.Highlight[] highlights = highlighter.getHighlights();
        // remove old highlights
        for (Highlighter.Highlight highlight : highlights) {
            highlighter.removeHighlight(highlight);
        }
        String s = "";
        try {
            Document doc = inputField.getDocument();
            s = doc.getText(0, doc.getLength());
        } catch (BadLocationException ignored) {
        }
        if (s.length() == 0) {
            return;
        }
        highlightErrorPosition();
        int pos = inputField.getCaretPosition();
        if (pos - 1 < 0) {
            return;
        }
        if (pos - 1 >= s.length()) {
            return;
        }
        highlightBrackets(s, pos);
        highlightRNodes(s, pos);
    }

    private boolean containsIllegalChar(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(Character.isDigit(c) || Character.isLetter(c) || c == '#' || c == '_' || c == '-' || c == '{' || c == '}'
                    || c == '(' || c == ')' || c == ',' || c == ';' || Character.isSpaceChar(c) || c == '\n' || c == '.'
                    || (c == ':' && edgeLengthCBox.isSelected()))) {
                errorPosition = i;
                message.setText("Illegal character");
                return true;
            }
        }
        return false;
    }

    private boolean containsIllegalEdgeLabeling(String s) {
        if (s.charAt(s.length() - 1) == ':') {
            errorPosition = s.length() - 1;
            message.setText("Edge length missing");
            return true;
        }
        if (s.charAt(0) == ':') {
            errorPosition = 0;
            message.setText("Edge length missing");
            return true;
        }
        for (int i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) == ':') {
                if (!Character.isDigit(s.charAt(i + 1))) {
                    errorPosition = i;
                    message.setText("Edge length missing");
                    return true;
                }
                if (!Character.isDigit(s.charAt(i - 1)) && !Character.isLetter(s.charAt(i - 1)) && s.charAt(i - 1) != ')') {
                    errorPosition = i;
                    message.setText("Illegal number after label or bracket");
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsWrongBracketPairing(String s) {
        int open_brack = 0;
        int close_brack = 0;
        boolean finished = false;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '(') {
                if (finished) {
                    errorPosition = i;
                    message.setText("Illegal re-opening bracket");
                    return true;
                }
                open_brack++;
            } else if (s.charAt(i) == ')') {
                close_brack++;
                if (close_brack == open_brack)
                    finished = true;
            } else if (s.charAt(i) == ';') {
                if (close_brack > open_brack) {
                    errorPosition = i;
                    message.setText("Too many closing brackets");
                    return true;
                }
                if (open_brack != close_brack) {
                    message.setText("Too few closing brackets");
                    return true;
                }
                open_brack = 0;
                close_brack = 0;
                finished = false;
            }
        }
        if (close_brack > open_brack) {
            message.setText("Too many closing brackets");
            return true;
        }
        if (open_brack != close_brack) {
            message.setText("Too few closing brackets");
            return true;
        }

        return false;
    }

    private boolean containsEmptyBracketPair(String s) {
        for (int i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) == '(' && s.charAt(i + 1) == ')') {
                errorPosition = i;
                message.setText("Empty brackets");
                return true;
            }
        }
        return false;
    }

    private boolean containsEmptyNode(String s) {
        for (int i = 0; i < s.length() - 1; i++) {
            if ((s.charAt(i) == '(' && s.charAt(i + 1) == ',')
                    || (s.charAt(i) == ',' && s.charAt(i + 1) == ',')
                    || (s.charAt(i) == ',' && s.charAt(i + 1) == ')')) {
                errorPosition = i;
                message.setText("Missing node label");
                return true;
            }
        }
        return false;
    }

    private boolean containsIllegalEdgeLength(String s) {
        for (int i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) == ':') {
                if (!edgeLengthCBox.isSelected()) {
                    errorPosition = i;
                    message.setText("Edge length found, but edge lengths disabled");
                    return true;
                }
                if (i == 0) {
                    errorPosition = 0;
                    message.setText("Number not allowed here");
                    return true;
                }
                if (s.charAt(i - 1) != ')' && !Character.isLetter(s.charAt(i - 1))
                        && !Character.isDigit(s.charAt(i - 1))) {
                    errorPosition = 0;
                    message.setText("Number not allowed here");
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsLabelBeforeBracket(String s) {
        if (s.length() < 2) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) == '(') {
                if (Character.isLetter(s.charAt(i - 1)) || Character.isDigit(s.charAt(i - 1))) {
                    errorPosition = i - 1;
                    message.setText("Illegal label before opening bracket");
                    return true;
                }
                if (s.charAt(i - 1) == ')') {
                    errorPosition = i;
                    message.setText("Illegal opening bracket");
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsSingleRNodeHnLn(String s) {
        for (int i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) == '#') {
                if (s.charAt(i + 1) == 'H' || s.charAt(i + 1) == 'L') {
                    if (i - 1 >= 0) {
                        if (s.charAt(i - 1) == '#') {
                            continue;
                        }
                    }
                    StringBuilder sn = new StringBuilder("#" + s.charAt(i + 1));
                    for (int j = i + 2; j < s.length(); j++) {
                        if (Character.isDigit(s.charAt(j))) {
                            sn.append(s.charAt(j));
                        } else {
                            break;
                        }
                    }
                    int n = 0;
                    int lastpos = 0;
                    while ((lastpos = s.indexOf(sn.toString(), lastpos)) != -1) {
                        lastpos = lastpos + sn.length();
                        if (lastpos == s.length()) {
                            n++;
                        } else if (!Character.isDigit(s.charAt(lastpos))
                                && !Character.isLetter(s.charAt(lastpos))) {
                            n++;
                        }
                    }
                    if (n < 2) {
                        errorPosition = i;
                        message.setText("#Hn/#Ln label occurs only once");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean containsMultiDoubleRNodeHnLn(String s) {
        for (int i = 0; i < s.length() - 2; i++) {
            if (s.charAt(i) == '#') {
                if (s.charAt(i + 1) == '#' &&
                        (s.charAt(i + 2) == 'H' || s.charAt(i + 2) == 'L')) {
                    StringBuilder sn = new StringBuilder("##" + s.charAt(i + 2));
                    for (int j = i + 3; j < s.length(); j++) {
                        if (Character.isDigit(s.charAt(j))) {
                            sn.append(s.charAt(j));
                        } else {
                            break;
                        }
                    }
                    int n = 0;
                    int lastpos = 0;
                    while ((lastpos = s.indexOf(sn.toString(), lastpos)) != -1) {
                        lastpos = lastpos + sn.length();
                        if (lastpos == s.length()) {
                            n++;
                        } else if (!Character.isDigit(s.charAt(lastpos))) {
                            n++;
                        }
                    }
                    if (n > 1) {
                        errorPosition = i;
                        message.setText("##Hn/##Ln label occurs more than once");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean startsWithOpeningBracket(String s) {
        boolean expr_start = true;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ';')
                expr_start = true;
            else if (s.charAt(i) == '(')
                expr_start = false;
            else if (s.charAt(i) == ' ' || s.charAt(i) == '\n')
                continue;
            else if (expr_start) {
                errorPosition = i;
                message.setText("Must start with open bracket");
                return false;
            }
        }
        return true;
    }

    private boolean isTerminatedBySemicolon(String s) {
        for (int i = s.length() - 1; i >= 0; i--) {
            if (s.charAt(i) == ';')
                return true;
            else if (s.charAt(i) == ' ' || s.charAt(i) == '\n')
                continue;
            else {
                errorPosition = i;
                message.setText("String not terminated by semicolon");
                return false;
            }
        }
        return true;
    }

    private boolean isEmpty(String s) {
        boolean empty = true;
        if (s.length() == 0)
            return true;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ' ' || s.charAt(i) == '\n')
                continue;
            else if (s.charAt(i) == ';') {
                if (empty) {
                    errorPosition = i;
                    message.setText("String is empty");
                    return true;
                }
                empty = true;
            } else
                empty = false;
        }
        return false;
    }

    private boolean containsSingleRNodeHnLn_ml(String s) {
        int startpos = 0;
        int endpos = 0;
        while ((endpos = s.indexOf(";", startpos)) != -1) {
            if (containsSingleRNodeHnLn(s.substring(startpos, endpos + 1))) {
                errorPosition = errorPosition + startpos;
                return true;
            }
            startpos = endpos + 1;
        }
        if (containsSingleRNodeHnLn(s.substring(startpos))) {
            errorPosition = errorPosition + startpos;
            return true;
        } else
            return false;
    }

    private boolean containsMultiDoubleRNodeHnLn_ml(String s) {
        int startpos = 0;
        int endpos = 0;
        while ((endpos = s.indexOf(";", startpos)) != -1) {
            if (containsMultiDoubleRNodeHnLn(s.substring(startpos, endpos + 1))) {
                errorPosition = errorPosition + startpos;
                return true;
            }
            startpos = endpos + 1;
        }
        if (containsMultiDoubleRNodeHnLn(s.substring(startpos))) {
            errorPosition = errorPosition + startpos;
            return true;
        } else
            return false;
    }


    private boolean checkNewick(String s) {
        errorPosition = -1;
        message.setText(" ");

        // check if string is empty
        if (isEmpty(s)) {
            return false;
        }
        if (!startsWithOpeningBracket(s)) {
            return false;
        }
        if (containsIllegalChar(s)) {
            return false;
        }
        if (containsLabelBeforeBracket(s)) {
            return false;
        }
        if (containsIllegalEdgeLabeling(s)) {
            return false;
        }
        if (containsWrongBracketPairing(s)) {
            return false;
        }
        if (containsEmptyBracketPair(s)) {
            return false;
        }
        if (containsEmptyNode(s)) {
            return false;
        }
        if (containsIllegalEdgeLength(s)) {
            return false;
        }
        if (containsSingleRNodeHnLn_ml(s)) {
            return false;
        }
        if (containsMultiDoubleRNodeHnLn_ml(s)) {
            return false;
        }
        return isTerminatedBySemicolon(s);
    }

    public void changedUpdate(DocumentEvent e) {
        okButton.setEnabled(checkNewick(inputField.getText()));
        updateHighlighter();
    }

    public void removeUpdate(DocumentEvent e) {
        okButton.setEnabled(checkNewick(inputField.getText()));
        updateHighlighter();
    }

    public void insertUpdate(DocumentEvent e) {
        okButton.setEnabled(checkNewick(inputField.getText()));
        updateHighlighter();
    }

    public void caretUpdate(CaretEvent e) {
        updateHighlighter();
    }

    public void itemStateChanged(ItemEvent e) {
        Object source = e.getSource();
        if (source == edgeLengthCBox) {
            okButton.setEnabled(checkNewick(inputField.getText()));
            updateHighlighter();
        }
    }

    static class MyHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {
        public MyHighlightPainter(Color color) {
            super(color);
        }
    }

    private AbstractAction okAction;

    private AbstractAction getOkAction() {
        AbstractAction action = okAction;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                result = inputField.getText().replaceAll("\n", "");
                NewickInputDialog.this.setVisible(false);
            }
        };
        action.putValue(Action.NAME, "OK");
        action.putValue(Action.SHORT_DESCRIPTION, "Parse tree or network and then display");
        return okAction = action;
    }

    private AbstractAction cancelAction;

    private AbstractAction getCancelAction() {
        AbstractAction action = cancelAction;
        if (action != null)
            return action;
        action = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                result = null;
                NewickInputDialog.this.setVisible(false);
            }
        };
        action.putValue(Action.NAME, "Cancel");
        action.putValue(Action.SHORT_DESCRIPTION, "Cancel input and close window");
        return cancelAction = action;
    }

    /**
     * get the entered string or null, if canceled
     *
     * @return entrered string or null
     */
    public String getString() {
        setVisible(true);
        return result;
    }

    /**
     * test program
     *
     */
    public static void main(String[] args) {
        NewickInputDialog parser = new NewickInputDialog(null, "Dendroscope", null, false);
        String result = parser.getString();
        System.err.println("Input: " + result);
        System.exit(0);
    }
}












