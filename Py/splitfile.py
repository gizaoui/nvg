import os
import sys
from pathlib import Path

import qrcode
import qrcode.constants
import qrcode.image.svg

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
    qr = qrcode.QRCode(
        version=None,
        error_correction=qrcode.constants.ERROR_CORRECT_L,
        image_factory=qrcode.image.svg.SvgPathImage,
    )
    qr.add_data(text)
    qr.make(fit=True)
    qr.make_image(fill_color="black", back_color="white").save(str(target))


def main() -> int:
    for svg in BASE_DIR.glob("*.svg"):
        svg.unlink()

    total = 0
    for id_src, path in enumerate(iter_sources(BASE_DIR), start=1):
        data = path.read_text(encoding="utf-8")
        chunks = [data[i:i + MAXSTR] for i in range(0, len(data), MAXSTR)]
        print(f"{id_src} - {path} ({len(data)} char(s) => {len(chunks)} file(s))")

        for id_chunk, chunk in enumerate(chunks, start=1):
            target = BASE_DIR / f"{id_src:02d}_{path.name}_{id_chunk:02d}.svg"
            try:
                make_qr(chunk + END_MARKER, target)
            except Exception as err:
                print(f"ERR : {target} : {err!r}", file=sys.stderr)
                return 1
            total += 1

    print(f"\n{total} QRcode(s) generated !!!")
    return 0


if __name__ == "__main__":
    sys.exit(main())
