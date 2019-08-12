/**
 * ComputeDTLReconcilationCommand.java 
 * Copyright (C) 2019 Daniel H. Huson
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
package dendroscope.commands.compute;

import dendroscope.commands.CommandBaseMultiViewer;
import dendroscope.dtl.DTL;
import dendroscope.main.Version;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.phylo.PhyloTree;
import jloda.swing.commands.ICommand;
import jloda.swing.util.Alert;
import jloda.swing.util.Message;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * compute  dtl rec...
 * Celine Scornavacca, 8.2010
 */

//todo: check they are binary and S is dated and the cost are int
//todo: check with daniel the problem when I select/deselect STree

public class ComputeDTLReconcilationCommand extends CommandBaseMultiViewer implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {
        int dupCost = 1;
        int transCost = 1;
        int lossCost = 1;

        np.matchIgnoreCase("compute DTL_reconciliation");
        if (np.peekMatchIgnoreCase("dupcost")) {
            np.matchIgnoreCase("dupcost=");
            dupCost = np.getInt(1, Integer.MAX_VALUE);
        }
        if (np.peekMatchIgnoreCase("transcost")) {
            np.matchIgnoreCase("transcost=");
            transCost = np.getInt(1, Integer.MAX_VALUE);
        }
        if (np.peekMatchIgnoreCase("losscost")) {
            np.matchIgnoreCase("losscost=");
            lossCost = np.getInt(1, Integer.MAX_VALUE);
        }
        List<PhyloTree> trees = new LinkedList<>();
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {

            TreeViewer viewer = it.next();
            PhyloTree tree = viewer.getPhyloTree();
            trees.add(tree);
        }

        if (trees.size() == 2) {
            trees.get(0).setAllowMultiLabeledNodes(true);
            trees.get(1).setAllowMultiLabeledNodes(true);
            //System.out.println( trees.get(0).toBracketString());
            //System.out.println( trees.get(1).toBracketString());
            long time = System.currentTimeMillis();


            int DTLcost = DTL.apply(trees.get(0), trees.get(1), dupCost, transCost, lossCost);

            Long runtime = System.currentTimeMillis() - time;
            Long seconds = runtime / 1000;

            System.out.println("time " + seconds);


            if (ProgramProperties.isUseGUI()) {
                new Message(getViewer().getFrame(), "The  DTL reconciliation cost between the two trees  is " + DTLcost);
            }
            System.out.println("The DTL reconciliation cost between the two trees  is " + DTLcost);

        } else {
            new Alert(getViewer().getFrame(), "DTL reconciliation requires two binary trees\n");
        }
    }

    public String getSyntax() {
        return "compute DTL_reconciliation [dupcost=<positive-integer>] [transcost=<positive-integer>] [losscost=<positive-integer];";
    }


    public void actionPerformed(ActionEvent ev) {
        JLabel[] JLabelparam = new JLabel[3];

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints grid = new GridBagConstraints();
        //panel.setSize(300,300);
        grid.fill = GridBagConstraints.HORIZONTAL;
        grid.anchor = GridBagConstraints.PAGE_START;


        grid.weightx = 0.5;
        grid.gridx = 0;
        grid.gridy = 0;

        //panel.setLayout(grid);

        JLabelparam[0] = new JLabel("dupCost");
        JLabelparam[1] = new JLabel("transCost");
        JLabelparam[2] = new JLabel("lossCost");

        JTextField[] JTextFieldparam = new JTextField[3];

        JTextFieldparam[0] = new JTextField(12);
        JTextFieldparam[0].setText("1");
        JTextFieldparam[1] = new JTextField(12);
        JTextFieldparam[1].setText("1");
        JTextFieldparam[2] = new JTextField(12);
        JTextFieldparam[2].setText("1");


        for (int i = 0; i < 3; i++) {
            grid.gridy = i;
            panel.add(JLabelparam[i], grid);
        }

        grid.gridx = 1;
        grid.weightx = 0.1;

        for (int i = 0; i < 3; i++) {
            grid.gridy = i;
            panel.add(JTextFieldparam[i], grid);

        }

        //Create a window using JFrame with title ( Two text component in JOptionPane )
        JFrame frame = new JFrame("Parameters DTL - " + Version.NAME);

        //Set default close operation for JFrame
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        //Set JFrame size


        //Set JFrame locate at center
        frame.setLocationRelativeTo(getViewer().getFrame());

        //Make JFrame visible
        frame.setVisible(false);

        //Show JOptionPane that will ask user for parameters
        int temp = JOptionPane.showConfirmDialog(frame, panel, "Enter parameters", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        //int a=JOptionPane.showConfirmDialog(frame,panel,"Enter parameters",JOptionPane.QUESTION_MESSAGE);

        if (temp != JOptionPane.OK_OPTION)
            return;

        int dupCost = Integer.parseInt(JTextFieldparam[0].getText());
        if (dupCost <= 0) {
            new Alert(getViewer().getFrame(), "dupCost: positive integer required, got: " + dupCost);
            return;
        }
        int transCost = Integer.parseInt(JTextFieldparam[1].getText());
        if (transCost <= 0) {
            new Alert(getViewer().getFrame(), "transCost: positive integer required, got: " + transCost);
            return;
        }
        int lossCost = Integer.parseInt(JTextFieldparam[2].getText());
        if (lossCost <= 0) {
            new Alert(getViewer().getFrame(), "lossCost: positive integer required, got: " + lossCost);
            return;
        }

        execute("compute DTL_reconciliation dupcost=" + dupCost + " transcost=" + transCost + " losscost=" + lossCost + ";");
    }


    public KeyStroke getAcceleratorKey() {
        return null;
    }


    public String getDescription() {
        return "Calculate DTL reconciliation between two binary trees";
    }

    public ImageIcon getIcon() {
        return null;
    }


    public String getName() {
        return "DTL Reconciliation...";
    }


    public String getUndo() {
        return null;
    }


    public boolean isApplicable() {
        return multiViewer.getTreeGrid().getNumberSelectedOrAllViewers() == 2 && ((MultiViewer) getViewer()).getDir().getDocument().getNumberOfTrees() > 0;
    }


    public boolean isCritical() {
        return true;
    }
}
