import glob
import os
import qrcode
import qrcode.image.svg


MAXSTR = 1700
ID_SPLIT_FILE = 1
ID_SRC_FILE = 0



for outFile in sorted(glob.glob("/home/gizaoui/QRCode/in/test/*.svg")):
    if os.path.isfile(outFile):
        os.remove(outFile)


for (dir_path, dir_names, file_names) in os.walk(r"/home/gizaoui/QRCode/in/test"):
    if not any(xs in dir_path for xs in ["target", ".settings", "logs", "output"]) :
        for javaFile in file_names:
            if javaFile.endswith(('.py','.txt','.xml','.md')) :
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
                        splitSVG = f"/home/gizaoui/QRCode/in/test/{format(ID_SRC_FILE, '02d')}_{javaFile}_{format(ID_FILE, '02d')}.svg"

                        try:
                            # qrencode -o picture.png "MY TEST STRING"
                            img = qr.make_image(fill_color="black", back_color="white")
                            img.save(splitSVG)
                            print(f"\nSAVE QRcode(s) generated !!!\n")
                        except Exception as err:
                            print(f"SVG FILE {splitSVG}")
                            print(f"ERR : Unexpected {err=}, {type(err)=}")
                            exit(1)


                else:
                    print(f"ERR : File {FILE_PATH} not found")
                    exit(1)

print(f"\n{ID_SPLIT_FILE} QRcode(s) generated !!!\n")
