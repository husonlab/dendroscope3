package dendroscope.hybroscale.view;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;


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
