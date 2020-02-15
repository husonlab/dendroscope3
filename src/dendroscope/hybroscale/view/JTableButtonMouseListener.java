/*
 *   JTableButtonMouseListener.java Copyright (C) 2020 Daniel H. Huson
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

package dendroscope.hybroscale.view;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


public class JTableButtonMouseListener implements MouseListener {

	private ClusterTable clusterTable;
	private StatusBar statusBar;

	public JTableButtonMouseListener(ClusterTable table, StatusBar statusBar) {
		clusterTable = table;
		this.statusBar = statusBar;
	}

	private void forwardEventToButton(MouseEvent e) {

		TableColumnModel columnModel = clusterTable.getColumnModel();
		int column = columnModel.getColumnIndexAtX(e.getX());
		int row = e.getY() / clusterTable.getRowHeight();
		Object value;
		JButton button;
		MouseEvent buttonEvent;

		if (row >= clusterTable.getRowCount() || row < 0
				|| column >= clusterTable.getColumnCount() || column < 0)
			return;

		value = clusterTable.getValueAt(row, column);

		if (!(value instanceof JButton))
			return;

		button = (JButton) value;
		buttonEvent = (MouseEvent) SwingUtilities.convertMouseEvent(
				clusterTable, e, button);

		if (buttonEvent.getClickCount() > 0 ){
			if(button.getText().equals(clusterTable.getAbortButtonName())) {
				statusBar.setAborted(true);
				boolean b = clusterTable.getClusterThread(row).stopThread();
				button.setEnabled(!b);
			}else if(button.getText().equals(clusterTable.getDetailsButtonName())){
				clusterTable.getInfoFrame(button).setVisible(true);
			}
		}

		button.dispatchEvent(buttonEvent);
		// This is necessary so that when a button is pressed and released
		// it gets rendered properly. Otherwise, the button may still appear
		// pressed down when it has been released.
		clusterTable.repaint();

	}

	public void mouseClicked(MouseEvent e) {
		forwardEventToButton(e);
	}

	public void mouseEntered(MouseEvent e) {
		forwardEventToButton(e);
	}

	public void mouseExited(MouseEvent e) {
		forwardEventToButton(e);
	}

	public void mousePressed(MouseEvent e) {
		forwardEventToButton(e);
	}

	public void mouseReleased(MouseEvent e) {
		forwardEventToButton(e);
	}
}
