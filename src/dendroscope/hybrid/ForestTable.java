/*
 *   ForestTable.java Copyright (C) 2020 Daniel H. Huson
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

/*
 * Copyright (C) This is third party code.
 */
package dendroscope.hybrid;

import jloda.swing.util.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;

public class ForestTable extends JPanel {

    private static final long serialVersionUID = 1L;
    private final GridBagConstraints c = new GridBagConstraints();
    private final Hashtable<ForestThread, JLabel> threadToStatus = new Hashtable<>();
    private final Hashtable<ForestThread, JLabel> threadToIcon = new Hashtable<>();

    private int row = 1;
    private int id = 1;

    public ForestTable() {

        setLayout(new GridBagLayout());
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;

        JLabel name = new JLabel("Forest Thread");
        name.setHorizontalAlignment(JLabel.CENTER);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        add(name, c);

        JLabel status = new JLabel("Status");
        status.setHorizontalAlignment(JLabel.CENTER);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 0;
        add(status, c);

        setPreferredSize(new Dimension(700, 300));

    }

    public void addClusterThread(ForestThread fT) {
        JLabel name = new JLabel("Thread " + id);
        name.setHorizontalAlignment(JLabel.CENTER);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = row;
        add(name, c);

        JLabel status = new JLabel("");
        status.setHorizontalAlignment(JLabel.CENTER);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = row;
        add(status, c);
        threadToStatus.put(fT, status);

        JLabel icon = new JLabel(ResourceManager.getIcon("sun/Refresh24.gif"));
        icon.setHorizontalAlignment(JLabel.CENTER);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = row;
        add(icon, c);
        threadToIcon.put(fT, icon);

        id++;
        row++;

        repaint();
    }

    public void stopCluster(ForestThread fT) {
        JLabel icon = threadToIcon.get(fT);
        icon.setIcon(ResourceManager.getIcon("sun/Stop24.gif"));
        repaint();
    }

    public void finishCluster(ForestThread fT) {
        JLabel icon = threadToIcon.get(fT);
        icon.setIcon(ResourceManager.getIcon("sun/Properties24.gif"));
        repaint();
    }

    public void setStatus(ForestThread fT, String info) {
        JLabel status = threadToStatus.get(fT);
        status.setText(info);
        repaint();
    }

}
