import os
import shutil
import subprocess
import sys
from pathlib import Path

MAXSTR = 1700
BASE_DIR = Path("/home/gizaoui/QRCode/in/test")  # dossier contenant les packages Java
SVG_DIR = "svg"
EXTENSIONS = {".py", ".txt", ".xml", ".md", ".java"}
EXCLUDED_DIRS = {"target", ".settings", "logs", "output", SVG_DIR}
END_MARKER = "\n@@EBLK@@"

NL = "\x01"  # retour à la ligne protégé (à l'intérieur d'une chaîne multi-lignes)

# Syntaxe des commentaires par type de fichier
#   line      : marqueur de commentaire de ligne
#   block     : (début, fin) de commentaire de bloc
#   protected : (début, fin) de zones multi-lignes à conserver telles quelles
#   quotes    : délimiteurs de chaînes sur une ligne
#   escape    : le backslash échappe dans les zones protégées
#   blank     : supprimer les lignes vides (et les espaces de fin de ligne)
LANGS = {
    ".java": dict(line="//", block=("/*", "*/"), protected=[('"""', '"""')],
                  quotes="\"'", escape=True, blank=True),
    ".py": dict(line="#", block=None, protected=[('"""', '"""'), ("'''", "'''")],
                quotes="\"'", escape=True, blank=True),
    ".xml": dict(line=None, block=("<!--", "-->"), protected=[("<![CDATA[", "]]>")],
                 quotes="", escape=False, blank=True),
    ".md": dict(line=None, block=("<!--", "-->"), protected=[],
                quotes="", escape=False, blank=False),
}


def strip_comments(src: str, suffix: str) -> str:
    cfg = LANGS.get(suffix)
    if cfg is None:  # .txt : pas de syntaxe de commentaire
        return src

    line, block, esc = cfg["line"], cfg["block"], cfg["escape"]
    out = []
    i, n = 0, len(src)

    while i < n:
        for start, end in cfg["protected"]:  # chaîne multi-lignes / CDATA
            if src.startswith(start, i):
                j = i + len(start)
                while j < n and not src.startswith(end, j):
                    j += 2 if esc and src[j] == "\\" else 1
                j = min(j + len(end), n)
                out.append(src[i:j].replace("\n", NL))
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
                out.append(src[i:min(j, n)])
                i = min(j, n)
            elif line and src.startswith(line, i) and not (i == 0 and src.startswith("#!")):
                j = src.find("\n", i)  # commentaire de ligne (shebang conservé)
                i = n if j == -1 else j
            elif block and src.startswith(block[0], i):
                j = src.find(block[1], i + len(block[0]))  # commentaire de bloc
                i = n if j == -1 else j + len(block[1])
            else:
                out.append(c)
                i += 1

    text = "".join(out)
    if not cfg["blank"]:
        return text.replace(NL, "\n")
    lines = [ln.rstrip() for ln in text.split("\n") if ln.strip()]
    return "\n".join(lines).replace(NL, "\n") + "\n"


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
        # newline="" : conserve les retours à la ligne tels quels (\r\n inclus)
        with path.open(encoding="utf-8", newline="") as f:
            raw = f.read()

        data = strip_comments(raw, path.suffix)  # sources non modifiées

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
