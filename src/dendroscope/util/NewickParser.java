/*
 * NewickParser.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.util;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class NewickParser extends JPanel implements ActionListener, DocumentListener, CaretListener {

    private final JLabel label;
    private final JTextField tf;
    private final JButton ok_button;
    private final JLabel message;


    public NewickParser() {
        setLayout(new FlowLayout());
        label = new JLabel("Please insert newick string:");
        tf = new JTextField(20);
        tf.addActionListener(this);
        tf.addCaretListener(this);
        tf.getDocument().addDocumentListener(this);
        ok_button = new JButton("OK");
        ok_button.addActionListener(this);
        message = new JLabel("");
        add(label);
        add(tf);
        add(ok_button);
        add(message);
        ok_button.setEnabled(checkNewick(tf.getText()));
        setSize(250, 200);
        setVisible(true);
    }

    public JFrame asFrame() {
        JFrame frame = new JFrame("Enter Newick");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(this, BorderLayout.CENTER);
        frame.setSize(250, 200);
        frame.setVisible(true);
        return frame;
    }


    public void checkHighlight(JTextComponent comp) {
        Highlighter highlighter = comp.getHighlighter();
        Highlighter.Highlight[] highlights = highlighter.getHighlights();
        MyHighlightPainter painter = new MyHighlightPainter(Color.red);
        // remove old highlights
        for (int i = 0; i < highlights.length; i++) {
            highlighter.removeHighlight(highlights[i]);
        }
        String s = "";
        try {
            Document doc = comp.getDocument();
            s = doc.getText(0, doc.getLength());
            if (s.length() == 0) {
                return;
            }
            int pos = comp.getCaretPosition();
            if (pos - 1 < 0) {
                System.out.println("pos-1 < 0");
                return;
            }
            System.out.println(pos);
            if (pos - 1 >= s.length()) {
                System.out.println("pos-1 >= s.length()");
                return;
            }
            if (s.charAt(pos - 1) == ')') {
                //highlight opening and closing brackets
                highlighter.addHighlight(pos - 1, pos, painter);
                int closed = 0;
                for (int i = pos - 2; i >= 0; i--) {
                    if (s.charAt(i) == ')') {
                        closed++;
                    } else if (s.charAt(i) == '(') {
                        if (closed == 0) {
                            highlighter.addHighlight(i, i + 1, painter);
                            break;
                        } else {
                            closed--;
                        }
                    }
                }
            } else if (s.charAt(pos - 1) == '(') {
                //highlight opening and closing brackets
                int open = 0;
                for (int i = pos; i < s.length(); i++) {
                    if (s.charAt(i) == '(') {
                        open++;
                    } else if (s.charAt(i) == ')') {
                        if (open == 0) {
                            highlighter.addHighlight(pos - 1, pos, painter);
                            highlighter.addHighlight(i, i + 1, painter);
                            break;
                        } else {
                            open--;
                        }
                    }
                }
            }

            //		else if (Character.isDigit(s.charAt(pos-1)) || Character.isLetter(s.charAt(pos-1))) {
            // check for reticulation node
//				String name = "";
//				for (int i=pos-1; i>=0; i--) {
//					if ( !(Character.isDigit(s.charAt(i)) || Character.isLetter(s.charAt(i)))
//							 && s.charAt(i)!='#') {
//						break;
//					} else if (s.charAt(i)=='#') {
//						name=s.substring(i,pos-1);
//					}
//				}
//				if (!name.isEmpty()) {
//					// highlight all reticulation nodes with same name
//					int firstpos = 0;
//				    int lastpos = 0;
//				    int endpos = 0;
//				    while ((lastpos = s.indexOf(name, lastpos)) != -1) {
//				      endpos = lastpos + name.length();
//				      try {
//				        highlighter.addHighlight(lastpos, endpos, painter);
//				      } catch (BadLocationException e) {
//				      }
//				      lastpos = endpos;
//				    }
//				}
//
//
//			}
        } catch (BadLocationException e) {
        }
    }


    private boolean checkNewick(String s) {
        // check if string is empty
        if (s.length() == 0) {
            message.setText("String is empty");
            return false;
        }
        // check if string contains illigal characters
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(Character.isDigit(c) || Character.isLetter(c) || c == '#' ||
                    c == '(' || c == ')' || c == ',' || c == ';')) {
                message.setText("Invalid character at position " + i);
                return false;
            }
        }
        // check if number of opening brackets
        // equals number of closing brackets
        int open_brack = 0;
        int close_brack = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '(') {
                open_brack++;
            } else if (s.charAt(i) == ')') {
                close_brack++;
            }
        }
        if (open_brack != close_brack) {
            message.setText("Number of opening brackets does not match numer of closing brackets");
            return false;
        }
        // check if the string is terminated by semicolon
        if (s.charAt(s.length() - 1) != ';') {
            message.setText("String not terminated by semicolon");
            return false;
        }
        // ok
        message.setText("");
        return true;
    }


    public static void main(String[] args) {
        final JFrame frame = new NewickParser().asFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void actionPerformed(ActionEvent e) {
        System.out.println("action");
        if (e.getSource() == ok_button) {
            //System.out.println("ok_button");
            setVisible(false);
            //dispose();
        } else if (e.getSource() == tf) {
            //System.out.println("tf");
            ok_button.setEnabled(checkNewick(tf.getText()));
        }
    }

    public void changedUpdate(DocumentEvent e) {
        //System.out.println("tf change");
        ok_button.setEnabled(checkNewick(tf.getText()));
        checkHighlight(tf);
    }

    public void removeUpdate(DocumentEvent e) {
        //System.out.println("tf remove");
        ok_button.setEnabled(checkNewick(tf.getText()));
        checkHighlight(tf);
    }

    public void insertUpdate(DocumentEvent e) {
        //System.out.println("tf insert");
        ok_button.setEnabled(checkNewick(tf.getText()));
        checkHighlight(tf);
    }

    public void caretUpdate(CaretEvent e) {
        checkHighlight(tf);
    }

    class MyHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {
        public MyHighlightPainter(Color color) {
            super(color);
        }

    }


}












