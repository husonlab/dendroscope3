package dendroscope.hybroscale.view;

import javax.swing.*;

import dendroscope.hybroscale.controller.HybroscaleController;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ProcWindow extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	private JPanel panel = new JPanel();
	private HybroscaleController controller;

	JTextField number, memory;

	public ProcWindow(HybridView hView, HybroscaleController controller) {
		this.controller = controller;
		panel.setLayout(new FlowLayout());

		JLabel textProc = new JLabel("Available Cores: ");
		panel.add(textProc);

		number = new JTextField();
		number.setText("" + (Runtime.getRuntime().availableProcessors() - 1));
		number.setColumns(10);
		number.selectAll();
		panel.add(number);
		
		JButton ok = new JButton("OK");
		ok.addActionListener(this);
		panel.add(ok);
		add(panel);

		setMinimumSize(new Dimension(100, 0));
		setAlwaysOnTop(true);
		pack();
		
		setLocationRelativeTo(hView);
		
	}

	public void actionPerformed(ActionEvent arg0) {
		try {
			Integer num = Integer.parseInt(number.getText());
			controller.setCores(num);
			setVisible(false);
		} catch (Exception e) {
			number.setText("invalid number");
		}
	}

}
