#!/usr/bin/env python3
"""Usage: python remove_comments.py Fichier.java

Supprime tous les commentaires (javadoc inclus) et toutes les lignes vides.
Une sauvegarde Fichier.java.bak est créée avant modification.
"""
import shutil
import sys
from pathlib import Path

NL = "\x01"  # retour à la ligne protégé (à l'intérieur d'un text block)


def strip_comments(src: str) -> str:
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        nxt = src[i + 1] if i + 1 < n else ""

        if src.startswith('"""', i):  # text block
            j = i + 3
            while j < n and not (src.startswith('"""', j) and src[j - 1] != "\\"):
                j += 2 if src[j] == "\\" else 1
            out.append(src[i:j + 3].replace("\n", NL))
            i = j + 3

        elif c in ('"', "'"):  # chaîne / caractère
            j = i + 1
            while j < n and src[j] != c:
                j += 2 if src[j] == "\\" else 1
            out.append(src[i:j + 1])
            i = j + 1

        elif c == "/" and nxt == "/":  # commentaire de ligne
            j = src.find("\n", i)
            i = n if j == -1 else j

        elif c == "/" and nxt == "*":  # commentaire de bloc / javadoc
            j = src.find("*/", i + 2)
            i = n if j == -1 else j + 2

        else:
            out.append(c)
            i += 1

    lines = [line.rstrip() for line in "".join(out).split("\n") if line.strip()]
    return "\n".join(lines).replace(NL, "\n") + "\n"


def main():
    if len(sys.argv) != 2:
        sys.exit("Usage: python remove_comments.py Fichier.java")
    f = Path(sys.argv[1])
    if not f.is_file():
        sys.exit(f"Fichier introuvable : {f}")
    shutil.copy2(f, f.with_name(f.name + ".bak"))
    f.write_text(strip_comments(f.read_text(encoding="utf-8")), encoding="utf-8")
    print(f"Commentaires et lignes vides supprimés : {f}")


if __name__ == "__main__":
    main()