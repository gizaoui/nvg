import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JButton;

public class Main {

	private static String[] exclFld = { "ABORT", "ALLBITS", "BOTTOM", "BOTTOM_ALIGNMENT", "CENTER", "CENTER_ALIGNMENT",
			"DEFAULT_KEYMAP", "EAST", "ERROR", "FOCUS_ACCELERATOR_KEY", "FRAMEBITS", "HEIGHT", "HORIZONTAL", "LEADING",
			"LEFT", "LEFT_ALIGNMENT", "NEXT", "NORTH", "NORTH_EAST", "NORTH_WEST", "PREVIOUS", "PROPERTIES", "RIGHT",
			"RIGHT_ALIGNMENT", "SOMEBITS", "SOUTH", "SOUTH_EAST", "SOUTH_WEST", "TOOL_TIP_TEXT_KEY", "TOP",
			"TOP_ALIGNMENT", "TRAILING", "UNDEFINED_CONDITION", "VERTICAL", "WEST",
			"WHEN_ANCESTOR_OF_FOCUSED_COMPONENT", "WHEN_FOCUSED", "WHEN_IN_FOCUSED_WINDOW", "WIDTH" };

	// com.navalgroup.setisc.stdhelper.testgw.getAllClass

	private static void getFldAndMth(Class<?> a, Component component) {

		System.out.println("\n- " + a.getSimpleName());

		System.out.println("\n  - Field : " + a.getFields().length);
		for (Field fld : a.getFields()) {
			fld.setAccessible(true);
			if (!Arrays.asList(exclFld).stream().filter(fname -> fld.getName().contains(fname)).findFirst()
					.isPresent()) {
				
				String str =null;
				if (fld.getType().getSimpleName().equals("JButton")) {
					try {
						str = ((JButton) fld.get(component)).getText();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
				System.out.println("     - " + fld.getType().getSimpleName() + " " + fld.getName() + (str != null ? " : " + str : ""));
			}
		}

		System.out.println("\n  - Method : " + a.getDeclaredMethods().length);
		for (Method method : a.getDeclaredMethods()) {
			if (Arrays.asList("get", "set", "is").stream().filter(access -> method.getName().contains(access))
					.findFirst().isPresent()
					&& Arrays.asList("String", "boolean", "Color").stream()
							.filter(datatype -> method.getReturnType().getSimpleName().contains(datatype)).findFirst()
							.isPresent()
					&& method.getParameterCount() == 0) {

				String str = null;
				if (Arrays.asList("getLogin", "getPassword").stream()
						.filter(access -> method.getName().contains(access)).findFirst().isPresent()
						&& method.getReturnType().getSimpleName().equals("String")) {
					try {
						str = (String) method.invoke(component);
					} catch (IllegalAccessException | InvocationTargetException it) {
						it.printStackTrace();
					}
				}

				System.out.println("     - " + method.getReturnType().getSimpleName() + " " + method.getName() + " "
						+ " (" + method.getParameterCount() + " param. "
						+ Stream.of(method.getParameterTypes()).collect(Collectors.toList()) + ")  "
						+ (str != null ? " : " + str : ""));
			}
		}

		Class<?> c = a.getSuperclass();
		if (c != null) {
			getFldAndMth(c, component);
		}
	}

	public static void main(String[] args) {

		// Récurération des attribus et methodes du composant graphique 'JToggleButton'
		// getFldAndMth((new JToggleButton()).getClass());

		// Récurération des attribus et methodes d'un formulaire
		Form frm = new Form();
		getFldAndMth(frm.getClass(), frm);

	}
}