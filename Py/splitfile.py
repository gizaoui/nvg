import io
import os
import re
import shutil
import subprocess
import sys
import tokenize
from pathlib import Path

MAXSTR = 1700
BASE_DIR = Path("/home/gizaoui/QRCode/in/test")  # dossier contenant les packages Java
SVG_DIR = "svg"
EXTENSIONS = {".py", ".txt", ".xml", ".md", ".java"}
EXCLUDED_DIRS = {"target", ".settings", "logs", "output", SVG_DIR}
END_MARKER = "\n@@EBLK@@"


# --------------------------------------------------------------------------
# Compactage des espaces / tabulations
# --------------------------------------------------------------------------
def is_word(c: str) -> bool:
    return c.isalnum() or c in "_$"


def squeeze(ln: str) -> str:
    """Supprime les espaces inutiles d'une ligne (déjà strippée).

    Un espace n'est conservé que s'il sépare deux caractères de mot
    (ex. 'return x'), deux '+' ou deux '-' (a + +b), ou un chiffre et un
    point (1 .real).
    """
    def repl(m):
        a, b = ln[m.start() - 1], ln[m.end()]
        if (is_word(a) and is_word(b)) or (a == b and a in "+-") or (a.isdigit() and b == "."):
            return " "
        return ""
    return re.sub(r"[ \t\f\v]+", repl, ln)


def compact_java(text: str) -> str:
    lines = [squeeze(ln.strip()) for ln in text.split("\n")]
    return "\n".join(ln for ln in lines if ln) + "\n"


def py_line_depths(text: str) -> dict:
    """Retourne {n° de ligne: profondeur d'indentation} pour les débuts d'instruction."""
    rows, depth, at_start = {}, 0, True
    for tok in tokenize.generate_tokens(io.StringIO(text).readline):
        t = tok.type
        if t == tokenize.INDENT:
            depth += 1
        elif t == tokenize.DEDENT:
            depth -= 1
        elif t == tokenize.NEWLINE:
            at_start = True
        elif t == tokenize.ENDMARKER:
            break
        elif t not in (tokenize.NL, tokenize.COMMENT, tokenize.ENCODING):
            if at_start:
                rows[tok.start[0]] = depth
                at_start = False
    return rows


def compact_py(text: str) -> str:
    lines = [ln for ln in text.split("\n") if ln.strip()]
    text = "\n".join(lines)
    try:
        depths = py_line_depths(text)  # indentation = 1 espace par niveau
    except (tokenize.TokenError, SyntaxError):
        depths = None  # code non analysable : indentation d'origine conservée
    out = []
    for row, ln in enumerate(lines, start=1):
        if row == 1 and ln.startswith("#!"):  # shebang : intact
            out.append(ln.rstrip())
            continue
        body = squeeze(ln.strip())
        if len(body) > 1 and body.endswith("\\") and is_word(body[-2]):
            body = body[:-1] + " \\"  # continuation de ligne : garder la séparation
        lead = ln[:len(ln) - len(ln.lstrip())] if depths is None else " " * depths.get(row, 0)
        out.append(lead + body)
    return "\n".join(out) + "\n"


def fix_tag(m) -> str:
    parts = re.split(r'("[^"]*"|\'[^\']*\')', m.group(0))
    for k in range(0, len(parts), 2):  # hors valeurs d'attributs
        parts[k] = re.sub(r"\s*=\s*", "=", re.sub(r"\s+", " ", parts[k]))
    return re.sub(r"\s+(/?>|\?>)$", r"\1", "".join(parts))


def compact_xml(text: str) -> str:
    text = re.sub(r"<[^<>]*>", fix_tag, text, flags=re.S)
    lines = [ln.strip() for ln in text.split("\n")]
    return "\n".join(ln for ln in lines if ln) + "\n"


# --------------------------------------------------------------------------
# Suppression des commentaires
# --------------------------------------------------------------------------
#   line      : marqueur de commentaire de ligne
#   block     : (début, fin) de commentaire de bloc
#   protected : (début, fin) de zones multi-lignes à conserver telles quelles
#   quotes    : délimiteurs de chaînes sur une ligne
#   escape    : le backslash échappe dans les zones protégées
#   compact   : fonction de compactage (None : on ne touche pas aux espaces)
LANGS = {
    ".java": dict(line="//", block=("/*", "*/"), protected=[('"""', '"""')],
                  quotes="\"'", escape=True, compact=compact_java),
    ".py": dict(line="#", block=None, protected=[('"""', '"""'), ("'''", "'''")],
                quotes="\"'", escape=True, compact=compact_py),
    ".xml": dict(line=None, block=("<!--", "-->"), protected=[("<![CDATA[", "]]>")],
                 quotes="", escape=False, compact=compact_xml),
    ".md": dict(line=None, block=("<!--", "-->"), protected=[],
                quotes="", escape=False, compact=None),
}


def scan(src: str, cfg: dict, ph: str):
    """Retire les commentaires ; chaque littéral est remplacé par `ph`."""
    line, block, esc = cfg["line"], cfg["block"], cfg["escape"]
    out, lits = [], []
    i, n = 0, len(src)

    while i < n:
        for start, end in cfg["protected"]:  # chaîne multi-lignes / CDATA
            if src.startswith(start, i):
                j = i + len(start)
                while j < n and not src.startswith(end, j):
                    j += 2 if esc and src[j] == "\\" else 1
                j = min(j + len(end), n)
                lits.append(src[i:j])
                out.append(ph)
                i = j
                break
        else:
            c = src[i]
            if c in cfg["quotes"]:  # chaîne / caractère sur une ligne
                j = i + 1
                while j < n and src[j] != c and src[j] != "\n":
                    j += 2 if src[j] == "\\" else 1
                if j < n and src[j] == c:
                    j += 1
                j = min(j, n)
                lits.append(src[i:j])
                out.append(ph)
                i = j
            elif line and src.startswith(line, i) and not (i == 0 and src.startswith("#!")):
                j = src.find("\n", i)  # commentaire de ligne (shebang conservé)
                i = n if j == -1 else j
            elif block and src.startswith(block[0], i):
                j = src.find(block[1], i + len(block[0]))  # commentaire de bloc
                i = n if j == -1 else j + len(block[1])
            else:
                out.append(c)
                i += 1
    return "".join(out), lits


def clean_source(src: str, suffix: str) -> str:
    """Supprime commentaires, lignes vides et espaces superflus (sans toucher aux littéraux)."""
    cfg = LANGS.get(suffix)
    if cfg is None:  # .txt : aucune syntaxe de commentaire
        return src

    src = src.replace("\r\n", "\n")
    ph, k = '"@@L@@"', 0
    while ph in src:  # marqueur garanti absent du source
        k += 1
        ph = f'"@@L{k}@@"'

    text, lits = scan(src, cfg, ph)
    if cfg["compact"]:
        text = cfg["compact"](text)

    parts = text.split(ph)  # restauration des littéraux, dans l'ordre
    res = parts[0]
    for lit, part in zip(lits, parts[1:]):
        res += lit + part
    return res


# --------------------------------------------------------------------------
# Génération des QR codes
# --------------------------------------------------------------------------
def iter_sources(base: Path):
    for root, dirs, files in os.walk(base):
        dirs[:] = sorted(d for d in dirs if d not in EXCLUDED_DIRS)  # élagage en place
        for name in sorted(files):
            if Path(name).suffix in EXTENSIONS:
                yield Path(root) / name


def target_dir(path: Path) -> Path:
    """Sous-dossier svg du package (dossier du fichier), créé si besoin."""
    d = path.parent / SVG_DIR
    d.mkdir(exist_ok=True)
    return d


def make_qr(text: str, target: Path) -> None:
    """Génère un QR code SVG avec la commande système qrencode.

    Le texte est transmis par l'entrée standard (pas d'argument de ligne de
    commande : pas de limite de taille ni de problème d'échappement).
    """
    subprocess.run(
        ["qrencode", "-t", "SVG", "-l", "L", "-o", str(target)],
        input=text.encode("utf-8"),
        check=True,
        capture_output=True,
    )


def main() -> int:
    if shutil.which("qrencode") is None:
        print("ERR : la commande 'qrencode' est introuvable (apt install qrencode)", file=sys.stderr)
        return 1

    # Nettoyage des anciens QR codes (racine + dossiers svg)
    for svg in BASE_DIR.glob("*.svg"):
        svg.unlink()
    for svg in BASE_DIR.rglob(f"{SVG_DIR}/*.svg"):
        svg.unlink()

    total = 0
    for id_src, path in enumerate(iter_sources(BASE_DIR), start=1):
        # newline="" : conserve les retours à la ligne tels quels
        with path.open(encoding="utf-8", newline="") as f:
            raw = f.read()

        data = clean_source(raw, path.suffix)  # sources non modifiées

        chunks = [data[i:i + MAXSTR] for i in range(0, len(data), MAXSTR)]
        print(f"{id_src} - {path} ({len(raw)} -> {len(data)} char(s) => {len(chunks)} file(s))")

        out_dir = target_dir(path)
        for id_chunk, chunk in enumerate(chunks, start=1):
            target = out_dir / f"{id_src:02d}_{path.name}_{id_chunk:02d}.svg"
            try:
                make_qr(chunk + END_MARKER, target)
            except subprocess.CalledProcessError as err:
                print(f"ERR : {target} : {err.stderr.decode(errors='replace').strip()}", file=sys.stderr)
                return 1
            total += 1

    print(f"\n{total} QRcode(s) generated !!!")
    return 0


if __name__ == "__main__":
    sys.exit(main())
