package example.jtree;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class RightPanelThree extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private JLabel info;

	public RightPanelThree()  {
		super();
		this.setPreferredSize(new Dimension(500, 700));
		info = new JLabel("Selection shows here ...");
		this.add(info);
	}

	public JLabel getInfo() {
		return info;
	}
}
