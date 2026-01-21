# NVG

```bash
git log --oneline -n 10 --reflog
git checkout f48839f

git tag -a v1.5 -m "my version 1.5"
git push origin --tags
git tag
git show v1.5

git push --delete origin v1.5
```


```bash
$ git pull
erreur : impossible de tirer avec un rebasage : vous avez des modifications non indexées.
erreur : de plus, votre index contient des modifications non validées.
erreur : veuillez les valider ou les remiser.

# Correctif
$ git pull --rebase --autostash
```

## Récupération des méthodes des sous-classes

```java
import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JTextField;

public class Main {

	private static void getAllClass(Class<?> a) {

		System.out.println("\n- " + a.getSimpleName());
		Method[] methods = a.getDeclaredMethods();
		for (int i = 0; i < methods.length; i++) {
			if ((methods[i].getName().contains("get") || methods[i].getName().contains("is"))
					&& methods[i].getReturnType().getSimpleName().contains("String"))
				System.out.println("   -" + methods[i].getReturnType().getSimpleName() + " " + methods[i].getName()
						+ " " + " (" + methods[i].getParameterCount() + " param. "
						+ Stream.of(methods[i].getParameterTypes()).collect(Collectors.toList()) + ")");
		}

		Class<?> c = a.getSuperclass();
		if (c != null) {
			getAllClass(c);
		}
	}

	public static void main(String[] args) {
		JTextField a = new JTextField();
		getAllClass(a.getClass());
	}
}
```
