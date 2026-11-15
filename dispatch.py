#!/usr/bin/env python3
"""Usage : eclater.py Tout.java [racine] [--dry-run]"""
import re
import sys
from pathlib import Path

PKG = re.compile(r'^[ \t]*package\s+([\w.]+)\s*;', re.M)
NOISE = re.compile(r'"(?:\\.|[^"\\])*"|//[^\n]*|/\*.*?\*/', re.S)
IMPORT = re.compile(r'^[ \t]*import\s[^;]*;', re.M)
KIND = r'(?:class|interface|enum|record|@interface)'
PUBLIC = re.compile(
    r'\bpublic\s+(?:(?:abstract|final|sealed|non-sealed|static|strictfp)\s+)*'
    + KIND + r'\s+([A-Za-z_]\w*)')
ANY = re.compile(r'(?<![\w.])' + KIND + r'\s+([A-Za-z_]\w*)')


def nom_du_type(unite: str):
    propre = IMPORT.sub('', NOISE.sub('', unite))
    m = PUBLIC.search(propre) or ANY.search(propre)
    return m.group(1) if m else None


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    dry = '--dry-run' in sys.argv
    if not args:
        sys.exit(__doc__)
    source = Path(args[0])
    racine = Path(args[1] if len(args) > 1 else 'src/main/java')

    texte = source.read_text(encoding='utf-8')
    debuts = list(PKG.finditer(texte))
    if not debuts:
        sys.exit("Aucune déclaration 'package' trouvée.")

    for i, m in enumerate(debuts):
        fin = debuts[i + 1].start() if i + 1 < len(debuts) else len(texte)
        unite = texte[m.start():fin].rstrip() + '\n'
        nom = nom_du_type(unite)
        if not nom:
            print(f"Type introuvable (package {m.group(1)})", file=sys.stderr)
            continue
        dossier = racine.joinpath(*m.group(1).split('.'))
        cible = dossier / f'{nom}.java'
        print(cible)
        if not dry:
            dossier.mkdir(parents=True, exist_ok=True)
            cible.write_text(unite, encoding='utf-8')


if __name__ == '__main__':
    main()