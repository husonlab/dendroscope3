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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Hashtable;

public class ClusterTable extends JTable {

    private final String detailsButtonName = "Details";
    private final String abortButtonName = "Abort";

    private static final long serialVersionUID = 1L;
    private final Hashtable<ClusterThread, Integer> threadToRow = new Hashtable<>();
    private final Hashtable<Integer, ClusterThread> rowToThread = new Hashtable<>();
    private final Hashtable<ClusterThread, JButton> threadToAbortButton = new Hashtable<>();
    private final Hashtable<ClusterThread, JButton> threadToDetailsButton = new Hashtable<>();
    private final Hashtable<JButton, InfoFrame> detailButtonToFrame = new Hashtable<>();

    private final Object[] columnNames = {"Thread ID", "Progress", "Status", "", ""};
    private Object[][] data;

    private final DefaultTableModel tModel;
    private int row = 0;
    private int id = 1;

//	private int width = (int) (Toolkit.getDefaultToolkit().getScreenSize()
//			.getWidth() / 3);
//	private int height = (int) (Toolkit.getDefaultToolkit().getScreenSize()
//			.getHeight() / 6);

    private final int width;

    @SuppressWarnings("serial")
    public ClusterTable(StatusBar statusBar, int width) {

        tModel = new DefaultTableModel(data, columnNames);
        this.width = width;

        DefaultTableCellRenderer buttonRenderer = new DefaultTableCellRenderer() {

            public JButton getTableCellRendererComponent(JTable table,
                                                         Object value, boolean isSelected, boolean hasFocus,
                                                         int row, int column) {

                return (JButton) value;

            }

        };

        DefaultTableCellRenderer barRenderer = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            JProgressBar progressBar;

            public Component getTableCellRendererComponent(JTable table,
                                                           Object value, boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                int intValue = (Integer) value;

                if (progressBar == null) {
                    progressBar = new JProgressBar();
                }
                progressBar.setValue(intValue);
                progressBar.setBackground(table.getBackground());
                progressBar.setStringPainted(true);

                return progressBar;
            }
        };

        DefaultTableCellRenderer iconRenderer = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            public void setValue(Object value) {
                if (value instanceof Icon) {
                    setIcon((Icon) value);
                    setText("");
                } else {
                    setIcon(null);
                    super.setValue(value);
                }
            }
        };

        setDefaultRenderer(JButton.class, buttonRenderer);
        setDefaultRenderer(Integer.class, barRenderer);
        setDefaultRenderer(String.class, iconRenderer);
        setDefaultRenderer(Icon.class, iconRenderer);

        iconRenderer.setHorizontalAlignment(SwingUtilities.CENTER);
        addMouseListener(new JTableButtonMouseListener(this, statusBar));

        setModel(tModel);

        getColumnModel().getColumn(0).setPreferredWidth(width / 6);
        getColumnModel().getColumn(1).setPreferredWidth(width / 6);
        getColumnModel().getColumn(2).setPreferredWidth(width / 3);
        getColumnModel().getColumn(3).setPreferredWidth(width / 6);
        getColumnModel().getColumn(4).setPreferredWidth(width / 6);

        setRowHeight(30);
        setEnabled(false);

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Class getColumnClass(int columnIndex) {
        if (columnIndex == 3 || columnIndex == 4) {
            return JButton.class;
        } else if (columnIndex == 1)
            return Integer.class;
        return String.class;
    }

    public void addClusterThread(ClusterThread cT) {

        String name = "Thread " + id;
        String status = "Thread in progress.";
        JButton abort = new JButton(abortButtonName);
        JButton details = new JButton(detailsButtonName);

        threadToRow.put(cT, row);
        rowToThread.put(row, cT);
        threadToAbortButton.put(cT, abort);
        threadToDetailsButton.put(cT, details);

        InfoFrame info = new InfoFrame("Details", "No details.");
        info.setTitle("Details");
        detailButtonToFrame.put(details, info);

        Object[] dataRow = {name, 0, status, abort, details};
        tModel.insertRow(row, dataRow);
        tModel.fireTableRowsInserted(0, row);

        getColumnModel().getColumn(0).setPreferredWidth(width / 6);
        getColumnModel().getColumn(1).setPreferredWidth(width / 6);
        getColumnModel().getColumn(2).setPreferredWidth(width / 3);
        getColumnModel().getColumn(3).setPreferredWidth(width / 6);
        getColumnModel().getColumn(4).setPreferredWidth(width / 6);

        id++;
        row++;
    }

    public void stopCluster(ClusterThread cT) {
        tModel.setValueAt("Canceled.", threadToRow.get(cT), 2);
        tModel.fireTableCellUpdated(threadToRow.get(cT), 2);
    }

    public void finishCluster(ClusterThread cT) {
        tModel.setValueAt("Done.", threadToRow.get(cT), 2);
        tModel.fireTableCellUpdated(threadToRow.get(cT), 2);
        threadToAbortButton.get(cT).setEnabled(false);
        repaint();
    }

    public void setStatus(ClusterThread cT, String info) {
        tModel.setValueAt(info, threadToRow.get(cT), 2);
        tModel.fireTableCellUpdated(threadToRow.get(cT), 2);
    }

    public void setProgress(ClusterThread cT, int i) {
        tModel.setValueAt(i, threadToRow.get(cT), 1);
        tModel.fireTableCellUpdated(threadToRow.get(cT), 1);
    }

    public synchronized void setDetails(ClusterThread thread, String details) {
        (detailButtonToFrame.get(threadToDetailsButton.get(thread)))
                .setInfo(details);
    }

    public ClusterThread getClusterThread(int row) {
        return rowToThread.get(row);
    }

    public JDialog getInfoFrame(JButton b) {
        return detailButtonToFrame.get(b);
    }

    public String getDetailsButtonName() {
        return detailsButtonName;
    }

    public String getAbortButtonName() {
        return abortButtonName;
    }

}
