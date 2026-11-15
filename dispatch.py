#!/usr/bin/env python3
"""Usage : eclater.py Tout.java [racine] [--dry-run]"""
import re
import sys
from pathlib import Path

TYPE = re.compile(r'(?<![\w.])(?:class|interface|enum|record|@interface)\s+([A-Za-z_]\w*)')
PKG = re.compile(r'^\s*package\s+([\w.]+)\s*;')
IMP = re.compile(r'^\s*import\s')


def masquer(t: str) -> str:
    """Remplace commentaires, chaînes et caractères par des espaces (même longueur)."""
    out = list(t)
    n, i = len(t), 0

    def blanc(a, b):
        for k in range(a, b):
            if out[k] != '\n':
                out[k] = ' '

    while i < n:
        if t.startswith('//', i):
            j = t.find('\n', i)
            j = n if j < 0 else j
        elif t.startswith('/*', i):
            j = t.find('*/', i + 2)
            j = n if j < 0 else j + 2
        elif t.startswith('"""', i):
            j = i + 3
            while j < n and not t.startswith('"""', j):
                j += 2 if t[j] == '\\' else 1
            j = min(j + 3, n)
        elif t[i] in '"\'':
            q, j = t[i], i + 1
            while j < n and t[j] != q and t[j] != '\n':
                j += 2 if t[j] == '\\' else 1
            j = min(j + 1, n)
        else:
            i += 1
            continue
        blanc(i, j)
        i = j
    return ''.join(out)


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    dry = '--dry-run' in sys.argv
    if not args:
        sys.exit(__doc__)
    racine = Path(args[1] if len(args) > 1 else 'src/main/java')

    src = Path(args[0]).read_text(encoding='utf-8')
    m = masquer(src)

    depth, debut, pkg, header = 0, 0, None, ''
    for i, c in enumerate(m):
        if c == '{':
            depth += 1
        elif c == '}':
            depth = max(depth - 1, 0)
            if depth == 0:
                t = TYPE.search(m[debut:i + 1])
                if t:
                    dossier = racine.joinpath(*pkg.split('.')) if pkg else racine
                    cible = dossier / f'{t.group(1)}.java'
                    corps = src[debut:i + 1].strip()
                    contenu = (header + '\n' if header else '') + corps + '\n'
                    print(cible)
                    if not dry:
                        dossier.mkdir(parents=True, exist_ok=True)
                        cible.write_text(contenu, encoding='utf-8')
                    debut = i + 1
        elif c == ';' and depth == 0:
            decl = m[debut:i + 1]
            p = PKG.match(decl)
            if p:
                pkg = p.group(1)
                header = src[debut:i + 1].strip() + '\n'
            elif IMP.match(decl):
                header += src[debut:i + 1].strip() + '\n'
            debut = i + 1


if __name__ == '__main__':
    main()