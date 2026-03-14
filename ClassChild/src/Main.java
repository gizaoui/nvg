import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;

public class Main implements ActionListener {

	JFrame frame;
	JButton button1, button2, button3;
	JTextField textfield;

	private static void getAllClass(Class<?> a) {

		System.out.println("\n- " + a.getSimpleName());


		for(Method method : a.getDeclaredMethods() ) {
			if (Arrays.asList("get", "set", "is").stream().filter(num -> method.getName().contains(num)).findFirst().isPresent() &&
					 Arrays.asList("String", "boolean", "Color").contains(method.getReturnType().getSimpleName()))

				System.out.println("   - " + method.getReturnType().getSimpleName() + " " + method.getName()
						+ " " + " (" + method.getParameterCount() + " param. "
						+ Stream.of(method.getParameterTypes()).collect(Collectors.toList()) + ")");
		}

		Class<?> c = a.getSuperclass();
		if (c != null) {
			getAllClass(c);
		}
	}

	public Main() {
		getAllClass(new JButton(new ImageIcon("pic/button.png")).getClass());
		
//		frame = new JFrame("Button Test");
//		button1 = new JButton("Yes");
//		button2 = new JButton("No");
//		button3 = new JButton(new ImageIcon("pic/button.png"));
//		getAllClass(button3.getClass());
//		String desc = ((ImageIcon) button3.getIcon()).getDescription();
//		System.out.println("desc : " + desc);
//		button1.setMnemonic('Y');
//		button2.setMnemonic('N');
//
//		textfield = new JTextField(20);
//		// getAllClass(textfield.getClass());
//
//		frame.setLayout(new FlowLayout());
//		frame.add(textfield);
//		frame.add(button1);
//		frame.add(button2);
//		frame.add(button3);
//
//		button1.addActionListener(this);
//		button2.addActionListener(this);
//
//		frame.pack();
//		frame.setVisible(true);
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		
	}
	

	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == button1) {
			textfield.setText("You clicked YES");
		} else {
			textfield.setText("You clicked NO");
		}
	}

	public static void main(String[] args) {

		new Main();

	}
}