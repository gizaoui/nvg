import os
import shutil
import subprocess
import sys
from pathlib import Path

MAXSTR = 1700
BASE_DIR = Path("/home/gizaoui/QRCode/in/test")
EXTENSIONS = {".py", ".txt", ".xml", ".md"}
EXCLUDED_DIRS = {"target", ".settings", "logs", "output"}
END_MARKER = "\n@@EBLK@@"


def iter_sources(base: Path):
    for root, dirs, files in os.walk(base):
        dirs[:] = sorted(d for d in dirs if d not in EXCLUDED_DIRS)  # élagage en place
        for name in sorted(files):
            if Path(name).suffix in EXTENSIONS:
                yield Path(root) / name


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

    for svg in BASE_DIR.glob("*.svg"):
        svg.unlink()

    total = 0
    for id_src, path in enumerate(iter_sources(BASE_DIR), start=1):
        # newline="" : conserve les retours à la ligne tels quels (\r\n inclus)
        with path.open(encoding="utf-8", newline="") as f:
            data = f.read()
        chunks = [data[i:i + MAXSTR] for i in range(0, len(data), MAXSTR)]
        print(f"{id_src} - {path} ({len(data)} char(s) => {len(chunks)} file(s))")

        for id_chunk, chunk in enumerate(chunks, start=1):
            target = BASE_DIR / f"{id_src:02d}_{path.name}_{id_chunk:02d}.svg"
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
