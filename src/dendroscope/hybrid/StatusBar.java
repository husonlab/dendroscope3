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

public class StatusBar extends JPanel {

    private static final long serialVersionUID = 1L;
    private final JLabel runtime;
    private final JLabel proc;
    final JTextField result;
    private boolean isAborted;

    public StatusBar() {
        setLayout(new BorderLayout());
        runtime = new JLabel("Runtime: " + getElapsedTime(0) + "   ");
        proc = new JLabel("   Available Cores: " + setPoolSize());
        result = new JTextField();
        result.setEditable(false);
        runtime.setHorizontalAlignment(SwingConstants.RIGHT);
        proc.setHorizontalAlignment(SwingConstants.LEFT);
        result.setHorizontalAlignment(SwingConstants.CENTER);
        add(runtime, BorderLayout.EAST);
        add(result, BorderLayout.CENTER);
        add(proc, BorderLayout.WEST);
    }

    public void updateTime(long time) {
        runtime.setText("Runtime: " + getElapsedTime(time) + "   ");
        runtime.repaint();
    }

    public void updateProc(int num) {
        proc.setText("   Available Cores: " + num);
        proc.repaint();
    }

    public void reportNetworks(int num, int hybrid) {
        if (num > 1 && hybrid > 1)
            result.setText(num + " networks with " + hybrid
                    + " reticulations computed.");
        else if (num == 1 && hybrid > 1)
            result.setText(num + " network with " + hybrid
                    + " reticulations computed.");
        else if (num > 1 && hybrid == 1)
            result.setText(num + " networks with " + hybrid
                    + " reticulation computed.");
        else
            result.setText(num + " network with " + hybrid
                    + " reticulation computed.");
        repaint();
    }

    public void reportNumber(int num) {
        if (!isAborted)
            result.setText("The computed hybridization number is " + num + ".");
        else
            result.setText("The computed hybridization number is >= " + num + ".");
        repaint();
    }

    public void reportDistance(int num) {
        if (!isAborted)
            result.setText("The computed rSPR-distance is " + num + ".");
        else
            result.setText("The computed rSPR-distance is >= " + num + ".");
        repaint();
    }

    public void setInfo(String s) {
        result.setText(s);
        repaint();
    }

    private int setPoolSize() {
        if (Runtime.getRuntime().availableProcessors() - 1 == 0)
            return 1;
        else
            return Runtime.getRuntime().availableProcessors() - 1;
    }

    public String getElapsedTime(long elapsedTime) {
        String format = String.format("%%0%dd", 2);
        elapsedTime = elapsedTime / 1000;
        String seconds = String.format(format, elapsedTime % 60);
        String minutes = String.format(format, (elapsedTime % 3600) / 60);
        String hours = String.format(format, elapsedTime / 3600);
        return hours + ":" + minutes + ":" + seconds;
    }

    public void setAborted(boolean isAborted) {
        this.isAborted = isAborted;
    }

}
