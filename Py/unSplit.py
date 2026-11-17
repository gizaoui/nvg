#!/usr/bin/env python3
"""Remet dans l'ordre les blocs exportés par splitfile.py.

Cas traité : les blocs (le texte de chaque QR code) ont été concaténés dans un
unique fichier, mais dans l'ordre inverse de l'export. Chaque bloc se termine
par la balise de fin ajoutée par splitfile.py ("\\n" suivi de la balise EBLK).
Le script découpe le fichier sur cette balise, inverse l'ordre des blocs et
les réécrit dans l'ordre d'origine. Par défaut, chaque bloc reste suivi de sa
balise : elle matérialise les limites des blocs (utile quand le contenu mélange
plusieurs types de fichiers : java, python, xml...). L'option --strip-markers
retire les balises pour obtenir le contenu brut.

Le lecteur de QR code peut ajouter du texte autour de chaque bloc :
  --separator        texte fixe après chaque balise (ex. '\\n')
  --separator-regex  texte variable après chaque balise (ex. date et heure du scan)
  --prefix-regex     texte avant chaque bloc (ex. un guillemet ouvrant)
  --undouble-quotes  remplace "" par " (export CSV)

Usage :
    python3 unsplitfile.py scan.txt                  # -> scan.restored.txt
    python3 unsplitfile.py scan.txt -o fichier.txt   # sortie explicite
    python3 unsplitfile.py scan.txt -o -             # sortie standard
    python3 unsplitfile.py scan.txt --strip-markers  # contenu brut, sans balises
    python3 unsplitfile.py scan.csv \\
        --separator-regex '",\\d+,\\d{2}-\\d{2}-\\d{4},\\d{2}:\\d{2}:\\d{2},,\\r?\\n?'
"""
import argparse
import re
import sys
from pathlib import Path

MARKER = "@@EBLK@@"
TERMINATOR = "\n" + MARKER  # exactement ce que splitfile.py ajoute après chaque bloc
MAXSTR = 1700  # taille maximale d'un bloc dans splitfile.py


def parse_blocks(data: str, separator: str = "", separator_regex: str | None = None,
                 prefix_regex: str | None = None, undouble_quotes: bool = False) -> list[str]:
    """Retourne les blocs (contenu seul, sans balise) dans l'ordre du fichier."""
    after = re.compile(separator_regex if separator_regex is not None else re.escape(separator))
    before = re.compile(prefix_regex) if prefix_regex else None

    tokens = data.split(TERMINATOR)
    if len(tokens) < 2:
        raise ValueError("aucune balise de fin de bloc trouvée : fichier non reconnu")

    tail = tokens.pop()  # ce qui suit la dernière balise
    match = after.match(tail)
    rest = tail[match.end():] if match else tail
    if rest.strip():
        print(f"ATTENTION : {len(rest)} caractère(s) après la dernière balise "
              "(bloc incomplet ?), ignorés", file=sys.stderr)

    blocks = []
    for i, token in enumerate(tokens, start=1):
        if i > 1:  # le texte du lecteur suit chaque balise : il précède chaque bloc sauf le 1er
            match = after.match(token)
            if match is None:
                raise ValueError(f"texte attendu après la balise précédente introuvable avant le bloc n°{i}")
            token = token[match.end():]
        if before is not None:
            match = before.match(token)
            if match is None:
                raise ValueError(f"préfixe attendu introuvable au début du bloc n°{i}")
            token = token[match.end():]
        if undouble_quotes:
            token = token.replace('""', '"')
        blocks.append(token)
    return blocks


def restore(data: str, separator: str = "", maxstr: int = MAXSTR, keep_markers: bool = True,
            separator_regex: str | None = None, prefix_regex: str | None = None,
            undouble_quotes: bool = False) -> tuple[str, int]:
    """Inverse l'ordre des blocs. Retourne (texte restauré, nombre de blocs).

    Avec keep_markers, chaque bloc est suivi de sa balise de fin (TERMINATOR),
    comme dans l'export d'origine.
    """
    blocks = parse_blocks(data, separator, separator_regex, prefix_regex, undouble_quotes)
    for i, block in enumerate(blocks, start=1):
        if len(block) > maxstr:
            print(f"ATTENTION : le bloc n°{i} du fichier fait {len(block)} caractères "
                  f"(> {maxstr}) : balise manquante entre deux blocs ?", file=sys.stderr)
    end = TERMINATOR if keep_markers else ""
    return "".join(block + end for block in reversed(blocks)), len(blocks)


def main() -> int:
    parser = argparse.ArgumentParser(description="Remet dans l'ordre les blocs exportés par splitfile.py.")
    parser.add_argument("input", type=Path, help="fichier contenant les blocs concaténés en ordre inverse")
    parser.add_argument("-o", "--output", help="fichier de sortie ('-' : sortie standard) ; "
                        "défaut : <nom>.restored<extension>")
    sep = parser.add_mutually_exclusive_group()
    sep.add_argument("--separator", default="", help="texte fixe inséré par le lecteur de QR code "
                     "après chaque balise, ex. '\\n' ou '\\r\\n'")
    sep.add_argument("--separator-regex", help="expression régulière du texte variable inséré après chaque "
                     "balise, ex. '\",\\d+,[\\d-]+,[\\d:]+,,\\r?\\n?'")
    parser.add_argument("--prefix-regex", help="expression régulière du texte inséré avant chaque bloc "
                        "(y compris le premier), ex. '\"'")
    parser.add_argument("--undouble-quotes", action="store_true",
                        help='remplace "" par " dans chaque bloc (export CSV)')
    parser.add_argument("--strip-markers", action="store_true",
                        help="retire les balises de fin de bloc (défaut : elles sont conservées)")
    parser.add_argument("--maxstr", type=int, default=MAXSTR, help=f"taille max d'un bloc (défaut {MAXSTR})")
    args = parser.parse_args()

    separator = args.separator.encode("utf-8").decode("unicode_escape")
    try:
        with args.input.open(encoding="utf-8", newline="") as f:  # newline="" : aucun octet modifié
            data = f.read()
        text, count = restore(data, separator, args.maxstr, keep_markers=not args.strip_markers,
                              separator_regex=args.separator_regex, prefix_regex=args.prefix_regex,
                              undouble_quotes=args.undouble_quotes)
    except (OSError, UnicodeDecodeError, ValueError, re.error) as err:
        print(f"ERR : {err}", file=sys.stderr)
        return 1

    if args.output == "-":
        sys.stdout.buffer.write(text.encode("utf-8"))
    else:
        out = Path(args.output) if args.output else args.input.with_name(
            f"{args.input.stem}.restored{args.input.suffix}")
        with out.open("w", encoding="utf-8", newline="") as f:
            f.write(text)
        print(f"{count} bloc(s) remis dans l'ordre -> {out}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
