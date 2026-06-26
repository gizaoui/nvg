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

- https://www.baeldung.com/java-custom-annotation


## https://www.baeldung.com/java-default-annotations

```java
// 1. Définition de l'interface fonctionnelle
@FunctionalInterface
interface Adder {
    int add(int a, int b); // La signature doit correspondre à la lambda
}

public class Main {
    public static void main(String[] args) {
        
        // 2. Utilisation de la lambda pour implémenter l'interface
        Adder adder = (a, b) -> a + b;

        // 3. Appel de la méthode et stockage du résultat
        int result = adder.add(4, 5);

        // 4. Affichage du résultat
        System.out.println("Le résultat de l'ajout est : " + result);
    }
}
```


```py
# JYTHON
from java.lang import String
msg = String("Hello Java").getBytes()
print(msg)
print("Method 1 : %s"%(String(msg)))

# PYTHON
print("Method 2 : %s"%("".join(map(chr, msg))))
```


```py
# -*- coding: iso-8859-15 -*-
start=b"START"
print(start)
print(start.decode(encoding="iso-8859-15"))
print("Method 2 : %s"%("".join(map(chr, start))))
```

```py
import glob
import os
import qrcode
import qrcode.image.svg


MAXSTR = 1700
ID_SPLIT_FILE = 1
ID_SRC_FILE = 0



for outFile in sorted(glob.glob("output/*.svg")):
    if os.path.isfile(outFile):
        os.remove(outFile)


for (dir_path, dir_names, file_names) in os.walk(r"/home/gizaoui/gitlab/nvl/QRCode"):
    if not any(xs in dir_path for xs in ["target", ".settings", "logs", "output"]) :
        for javaFile in file_names:
            if javaFile.endswith(('.py','.xml','.md')) :
                FILE_PATH = f"{dir_path}/{javaFile}"
                ID_SRC_FILE += 1

                if os.path.isfile(FILE_PATH):
                    fOutput = open(FILE_PATH, "r", encoding="utf-8")
                    data = fOutput.read()
                    fOutput.close()
                    print(f"{ID_SRC_FILE} - {FILE_PATH} ({len(data)} char(s) => {int(len(data)/MAXSTR)+1} file(s))")

                    ID_FILE = 0
                    for splitStr in [data[offset - MAXSTR:offset] for offset in range(MAXSTR, len(data) + MAXSTR, MAXSTR)]:
                        qr = qrcode.QRCode(
                            version=None,
                            error_correction=qrcode.constants.ERROR_CORRECT_L,
                            image_factory=qrcode.image.svg.SvgPathImage
                        )
                        qr.clear()
                        qr.make(fit=True)
                        qr.add_data(f"{splitStr}\n@@EBLK@@")

                        ID_SPLIT_FILE += 1
                        ID_FILE += 1
                        splitSVG = f"/home/gizaoui/gitlab/nvl/QRCode/output/{format(ID_SRC_FILE, '02d')}_{javaFile}_{format(ID_FILE, '02d')}.svg"

                        try:
                            img = qr.make_image(fill_color="black", back_color="white")
                            img.save(splitSVG)
                        except Exception as err:
                            print(f"SVG FILE {splitSVG}")
                            print(f"ERR : Unexpected {err=}, {type(err)=}")
                            exit(1)


                else:
                    print(f"ERR : File {FILE_PATH} not found")
                    exit(1)

print(f"\n{ID_SPLIT_FILE} QRcode(s) generated !!!\n")

```