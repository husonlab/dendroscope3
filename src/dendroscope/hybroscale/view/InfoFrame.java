package dendroscope.hybroscale.view;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class InfoFrame extends JDialog {
	private static final long serialVersionUID = 1L;

	private int width = 400;
	private int height = 200;
	
	private JTextArea text = new JTextArea();

	public InfoFrame(String title, String message) {
		super();

		setModal(true);
		setTitle(title);
		setSize(width, height);

		Container main = getContentPane();
		main.setLayout(new BorderLayout());

		JPanel middle = new JPanel();
		middle.setLayout(new BorderLayout());
		middle.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		text.setEditable(false);
		text.setWrapStyleWord(true);
		text.setLineWrap(true);
		text.setBackground(main.getBackground());
		text.setText(message);

		middle.add(new JScrollPane(text), BorderLayout.CENTER);
		main.add(middle, BorderLayout.CENTER);

		JPanel bottom = new JPanel();
		bottom.setLayout(new BorderLayout());

		JButton closeButton = new JButton(getCloseAction());
		bottom.add(closeButton, BorderLayout.EAST);
		rootPane.setDefaultButton(closeButton);

		main.add(bottom, BorderLayout.SOUTH);

	}

	public AbstractAction getCloseAction() {
		AbstractAction action = new AbstractAction() {
			
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent event) {
				InfoFrame.this.dispose();
			}
		};
		action.putValue(AbstractAction.NAME, "Close");
		return action;
	}

	public void setInfo(String message) {
		text.setText(message);
	}
}
