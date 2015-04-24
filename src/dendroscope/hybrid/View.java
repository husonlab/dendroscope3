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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class View extends JFrame implements Runnable {

    public enum Computation {
        NETWORK, HYBRID_NUMBER, rSPR_DISTANCE
    }

    private boolean isVisible = true;

    private final int width = 650;
    private final int height = 83;

    private static final long serialVersionUID = 1L;
    // private HybridMenuBar hMb;
    private final ButtonPanel bP;
    private final ClusterTable cT;
    private final StatusBar statusBar = new StatusBar();

    public View(JFrame mainFrame, final Controller controller, Computation compValue) {
        setLocationRelativeTo(mainFrame);

        cT = new ClusterTable(statusBar, width);
        setLayout(new BorderLayout());

        if (compValue == Computation.NETWORK)
            setTitle("Hybridization Networks");
        else if (compValue == Computation.HYBRID_NUMBER)
            setTitle("Hybridization Number");
        else
            setTitle("rSPR-Distance");
        // hMb = new HybridMenuBar(controller, compValue);
        bP = new ButtonPanel(controller, compValue);
        getRootPane().setDefaultButton(bP.getRunButton());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                controller.stop();
            }
        });

        setLocationRelativeTo(mainFrame);

    }

    public void run() {
        // setJMenuBar(hMb);

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.BOTH;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.ipady = height;
        c.ipadx = width - 35;
        add(new JScrollPane(cT), c);

        c.ipady = 0;
        c.ipadx = 1;
        c.gridy = 1;
        add(bP, c);

        c.ipadx = 3;
        c.gridy = 2;
        add(statusBar, c);

        setResizable(false);
        setPreferredSize(new Dimension(width, height + 130));
        setAlwaysOnTop(true);
        pack();
        super.setVisible(isVisible);
        bP.setView(this);
        repaint();
    }

    public void setInfo(String s) {
        statusBar.setInfo(s);
        repaint();
    }

    public void updateTime(long time) {
        statusBar.updateTime(time);
    }

    public void reportNetworks(int num, int hybrid) {
        statusBar.reportNetworks(num, hybrid);
    }

    public void reportNumber(int num) {
        statusBar.reportNumber(num);
    }

    public void reportDistance(int num) {
        statusBar.reportDistance(num);
    }

    public void updateProc(int num) {
        statusBar.updateProc(num);
    }

    public void addClusterThread(ClusterThread thread) {
        cT.addClusterThread(thread);
        // hMb.addClusterThread(thread);
    }

    public void stopClusterThread(ClusterThread thread) {
        cT.stopCluster(thread);
        setProgress(thread, -1);
        setProgress(thread, -1);
    }

    public void finishClusterThread(ClusterThread thread) {
        cT.finishCluster(thread);
        setProgress(thread, 100);
    }

    public void setStatus(ClusterThread thread, String info) {
        cT.setStatus(thread, info);
    }

    public void setProgress(ClusterThread thread, int i) {
        cT.setProgress(thread, i);
    }

    public void enableMarkingTrees() {
        // hMb.enableMarkingTrees();
        bP.enableEditTrees();
    }

    public void createInfoFrame(String title, String text) {
        InfoFrame frame = new InfoFrame(title, text);
        frame.setVisible(true);
    }

    public void setDetails(ClusterThread thread, String details) {
        cT.setDetails(thread, details);
    }

    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }
}
