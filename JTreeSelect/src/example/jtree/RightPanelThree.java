package example.jtree;

import java.awt.*;

import javax.swing.*;

public class RightPanelThree extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private JLabel info;

	public RightPanelThree()  {
		super();
		this.setLayout(new BorderLayout());
		JPanel centerPanel = new JPanel();
		centerPanel.setBackground(Color.WHITE);
		info = new JLabel("Selection shows here ...");
		centerPanel.add(info);

		JTextField textSouth = new JTextField();

		this.add(centerPanel, BorderLayout.CENTER);
		this.add(textSouth, BorderLayout.SOUTH);


	}

	public JLabel getInfo() {
		return info;
	}
}
