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

import jloda.util.ResourceManager;

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

        JLabel icon = new JLabel(ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Refresh24.gif"));
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
        icon.setIcon(ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Stop24.gif"));
        repaint();
    }

    public void finishCluster(ForestThread fT) {
        JLabel icon = threadToIcon.get(fT);
        icon.setIcon(ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Properties24.gif"));
        repaint();
    }

    public void setStatus(ForestThread fT, String info) {
        JLabel status = threadToStatus.get(fT);
        status.setText(info);
        repaint();
    }

}
