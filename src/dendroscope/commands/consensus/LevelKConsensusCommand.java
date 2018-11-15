/**
 * LevelKConsensusCommand.java 
 * Copyright (C) 2018 Daniel H. Huson
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
package dendroscope.commands.consensus;

import dendroscope.consensus.ComputeNetworkConsensus;
import dendroscope.window.MultiViewer;
import dendroscope.window.TreeViewer;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.Alert;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Iterator;

public class LevelKConsensusCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    public void actionPerformed(ActionEvent ev) {
        /*
        String result = null;
        if (((MultiViewer) getViewer()).getTreeGrid().getNumberSelectedOrAllViewers() <= 2)
            result = "0";
        else {
            double threshold = ProgramProperties.get("ConsensusThreshold", 20d);
            result = JOptionPane.showInputDialog(getViewer().getFrame(), "Enter percent threshold for network consensus construction", "" + threshold);
        }
        */

        String param[] = createPanelForInput();
        if (param[2] != null) {
            float threshold = Basic.parseFloat(param[2]);

            if (threshold >= 0) {
                ProgramProperties.put("ConsensusThreshold", threshold);
                execute("compute consensus method=" + ComputeNetworkConsensus.LEVEL_K_NETWORK + " threshold='" + threshold + "' one-only=" + param[0] + " " + "check-trees=" + param[1] + ";");
            } else
                new Alert(getViewer().getFrame(), "Number >=0 expected, got: '" + param[2] + "'");
        }
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Compute a consensus level-k-network for the given trees";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getName() {
        return "Level-k Network Consensus...";
    }

    public String getUndo() {
        return null;
    }

    public static String[] createPanelForInput() {

        String param[] = new String[3];

        JLabel JLabelparam[] = new JLabel[3];

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints grid = new GridBagConstraints();
        //panel.setSize(300,300);
        grid.fill = GridBagConstraints.HORIZONTAL;
        grid.anchor = GridBagConstraints.PAGE_START;

        grid.weightx = 0.5;
        grid.gridx = 0;
        grid.gridy = 0;

        JLabelparam[0] = new JLabel("Construct only one network");
        JLabelparam[1] = new JLabel("Construct only networks that display the trees");
        JLabelparam[2] = new JLabel("Use clusters that appear in at least this percentage of the trees");

        JCheckBox JCheckBoxparam[] = new JCheckBox[2];
        for (int i = 0; i < 2; i++)
            JCheckBoxparam[i] = new JCheckBox();

        JTextField JTextFieldparam = new JTextField(2);
        JTextFieldparam.setText("0");

        for (int i = 0; i < 3; i++) {
            grid.gridy = i;
            panel.add(JLabelparam[i], grid);
        }

        grid.gridx = 1;
        grid.weightx = 0.1;
        for (int i = 0; i < 3; i++) {
            grid.gridy = i;

            if (i < 2) {
                JCheckBoxparam[i].setSelected(false);
                panel.add(JCheckBoxparam[i], grid);
            } else
                panel.add(JTextFieldparam, grid);

        }


        //Create a window using JFrame with title ( Two text component in JOptionPane )
        JFrame frame = new JFrame("Options");

        //Set default close operation for JFrame
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        //Set JFrame size

        //Set JFrame locate at center
        frame.setLocationRelativeTo(null);

        //Make JFrame visible
        frame.setVisible(false);

        //Show JOptionPane that will ask user for parameters
        int a = JOptionPane.showConfirmDialog(frame, panel, "Options", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        //Operation that will do when user click 'OK'
        if (a == JOptionPane.OK_OPTION) {
            param[0] = Boolean.toString(JCheckBoxparam[0].isSelected());
            param[1] = Boolean.toString(JCheckBoxparam[1].isSelected());
            param[2] = JTextFieldparam.getText();
            return param;
        }

        //Operation that will do when user click 'Cancel'
        return null;
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        MultiViewer multiViewer = (MultiViewer) getViewer();
        int count = 0;
        for (Iterator<TreeViewer> it = multiViewer.getTreeGrid().getSelectedOrAllIterator(); it.hasNext(); ) {
            TreeViewer treeViewer = it.next();
            if (treeViewer.getPhyloTree().getSpecialEdges().size() > 0)
                return false;
            count++;
        }
        return count > 1;
    }
}
