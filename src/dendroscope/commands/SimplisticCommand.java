/*
 * SimplisticCommand.java Copyright (C) 2022 Daniel H. Huson
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
package dendroscope.commands;

import dendroscope.core.TreeData;
import dendroscope.tripletMethods.Simplistic;
import dendroscope.window.TreeViewer;
import jloda.phylo.PhyloTree;
import jloda.swing.commands.ICommand;
import jloda.swing.util.Alert;
import jloda.util.StringUtils;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;


public class SimplisticCommand extends CommandBaseMultiViewer implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("compute triplets2network method=simplistic parameters=");

        java.util.List<String> list = new LinkedList<>();

        while (!np.peekMatchIgnoreCase(";")) {
            list.add(np.getWordRespectCase());
        }

        String[] param = list.toArray(new String[list.size()]);


        //String param[] = createPanelForInput();

        if ((Boolean.parseBoolean(param[1]) == true) && (Boolean.parseBoolean(param[0]) == false)) {
            new Alert("Option 2 only works when the option 1 box is checked. Default value \"false\" is used.\n");
            param[1] = Boolean.toString(false);
        }

        /*
        if ((Boolean.parseBoolean(param[2]) == true) && (Boolean.parseBoolean(param[0]) == false)) {
            new Alert("Option 3 only works when the option 1 box is checked. Default value \"false\" is used.\n");
            param[2] = Boolean.toString(false);
        }
        */

        if (Integer.parseInt(param[3]) < 1) {
            new Alert("The starting level has to be greater than 1. Default value 1 is used.\n");
            param[3] = "1";
        }
        if (Integer.parseInt(param[4]) < 0) {
            new Alert("The subsets have to have a size greater than 0. Default value 0 is used.\n");
            param[4] = "0";
        }


        Vector<TreeData> trees = new Vector<>();
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer viewer = it.next();
            PhyloTree tree = (PhyloTree) viewer.getPhyloGraph();
            if (tree.getNumberOfNodes() != 0) {
                trees.add(new TreeData(tree));
            }
        }
        Simplistic.apply(getDir(), getDir().getDocument(), trees.toArray(new TreeData[trees.size()]), param);

    }


    public String getSyntax() {
        return "compute triplets2network method=simplistic parameters=<parameters>;";
    }


    public void actionPerformed(ActionEvent ev) {
        String[] param = createPanelForInput();
        if (param != null) {
			execute("compute triplets2network method=simplistic parameters=" + StringUtils.toString(param, " ") + ";");
        }
    }


    public KeyStroke getAcceleratorKey() {
        return null;
    }


    public String getDescription() {
        return "Compute a network using the simplistic algorithm";
    }

    public ImageIcon getIcon() {
        return null;
    }


    public String getName() {
        return "Simplistic...";
    }


    public String getUndo() {
        return null;
    }


    public boolean isApplicable() {
        boolean apply = multiViewer.getTreeGrid().getNumberSelectedOrAllViewers() > 1 && getDir().getDocument().getNumberOfTrees() > 0;
        if (apply) {
            for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
                if (it.next().getPhyloTree().getNumberReticulateEdges() > 0)
					return false;
            }
        }
        return apply;
    }


    public boolean isCritical() {
        return true;
    }

    public static String[] createPanelForInput() {

        String[] param = new String[6];

        JLabel[] JLabelparam = new JLabel[6];

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints grid = new GridBagConstraints();
        //panel.setSize(300,300);
        grid.fill = GridBagConstraints.HORIZONTAL;
        grid.anchor = GridBagConstraints.PAGE_START;


        grid.weightx = 0.5;
        grid.gridx = 0;
        grid.gridy = 0;

        //panel.setLayout(grid);


        JLabelparam[0] = new JLabel("Build only simple networks");
        JLabelparam[1] = new JLabel("For each level, builds all simple networks\n (only works for simple networks)");
        JLabelparam[2] = new JLabel("Do not try higher levels once a network has been found");
        JLabelparam[3] = new JLabel("Start at level : ");
        JLabelparam[4] = new JLabel("Split subsets of size (see documentation at http://skelk.sdf-eu.org/simplistic.html):");
        JLabelparam[5] = new JLabel("Stop at level (if -1, the default value is used, i.e., number of taxa -1):");

        JCheckBox[] JCheckBoxparam = new JCheckBox[3];
        for (int i = 0; i < 3; i++)
            JCheckBoxparam[i] = new JCheckBox();

        JTextField[] JTextFieldparam = new JTextField[3];

        JTextFieldparam[0] = new JTextField(12);
        JTextFieldparam[0].setText("1");
        JTextFieldparam[1] = new JTextField(12);
        JTextFieldparam[1].setText("0");
        JTextFieldparam[2] = new JTextField(12);
        JTextFieldparam[2].setText("-1");


        for (int i = 0; i < 6; i++) {
            grid.gridy = i;
            panel.add(JLabelparam[i], grid);
        }

        grid.gridx = 1;
        grid.weightx = 0.1;
        for (int i = 0; i < 6; i++) {
            grid.gridy = i;

            if (i < 3) {
                JCheckBoxparam[i].setSelected(false);
                panel.add(JCheckBoxparam[i], grid);
            } else
                panel.add(JTextFieldparam[i - 3], grid);

        }
        JCheckBoxparam[2].setSelected(true);

        //Create a window using JFrame with title ( Two text component in JOptionPane )
        JFrame frame = new JFrame("Parameters");

        //Set default close operation for JFrame
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        //Set JFrame size


        //Set JFrame locate at center
        frame.setLocationRelativeTo(null);

        //Make JFrame visible
        frame.setVisible(false);

        //Show JOptionPane that will ask user for parameters
        int a = JOptionPane.showConfirmDialog(frame, panel, "Enter parameters", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        //int a=JOptionPane.showConfirmDialog(frame,panel,"Enter parameters",JOptionPane.QUESTION_MESSAGE);


        //Operation that will do when user click 'OK'
        if (a == JOptionPane.OK_OPTION) {
            param[0] = Boolean.toString(JCheckBoxparam[0].isSelected());
            param[1] = Boolean.toString(JCheckBoxparam[1].isSelected());
            param[2] = Boolean.toString(JCheckBoxparam[2].isSelected());
            param[3] = JTextFieldparam[0].getText();
            param[4] = JTextFieldparam[1].getText();
            param[5] = JTextFieldparam[2].getText();
            //param[6] = "true";
            return param;
        }

        //Operation that will do when user click 'Cancel'
        return null;


    }
}
