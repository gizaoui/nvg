import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Form extends JPanel {

	private static final long serialVersionUID = 1L;
	public static  JLabel labelUsername = new JLabel("Enter username: ");
	public static JLabel labelPassword = new JLabel("Enter password: ");
	private static JTextField textUsername = new JTextField(20);
	private static JPasswordField fieldPassword = new JPasswordField(20);
	public static JButton buttonLogin = new JButton("Login");

	
	static {
		textUsername.setText("my login");
		fieldPassword.setText("my password");
	}

	public Form() {


		this.setLayout(new GridBagLayout());

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(10, 10, 10, 10);

		// add components to the panel
		constraints.gridx = 0;
		constraints.gridy = 0;
		this.add(labelUsername, constraints);

		constraints.gridx = 1;
		this.add(textUsername, constraints);

		constraints.gridx = 0;
		constraints.gridy = 1;
		this.add(labelPassword, constraints);

		constraints.gridx = 1;
		this.add(fieldPassword, constraints);

		constraints.gridx = 0;
		constraints.gridy = 2;
		constraints.gridwidth = 2;
		constraints.anchor = GridBagConstraints.CENTER;
		this.add(buttonLogin, constraints);

		// set border for the panel
		this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Login Panel"));
	}
	
	public String getLogin() {
		return textUsername.getText();
	}
	
	public String getPassword() {
		return fieldPassword.getText();
	}
	
	public static void main(String[] args) {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JFrame frm = new JFrame("JPanel Demo Program");
				frm.add(new Form());
				frm.pack();
				frm.setLocationRelativeTo(null);
				frm.setVisible(true);
			}
		});
	}
}