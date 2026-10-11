package vnc;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Client VNC minimal (protocole RFB 3.8).
 * Sécurité : None et VNC Authentication. Encodages : Raw et CopyRect.
 */
public final class VncConnection implements Closeable {

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private BufferedImage image;
    private String desktopName;
    private volatile boolean running;

    public BufferedImage getImage() { return image; }

    public String getDesktopName() { return desktopName; }

    // ---------------------------------------------------------------- connexion

    public void connect(String host, int port, String password) throws Exception {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 10_000);
        socket.setTcpNoDelay(true);
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream(), 1 << 16));
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

        // Version du protocole
        byte[] ver = new byte[12];
        in.readFully(ver);
        String serverVersion = new String(ver, StandardCharsets.US_ASCII);
        if (!serverVersion.startsWith("RFB ")) {
            throw new IOException("Ce n'est pas un serveur VNC : " + serverVersion.trim());
        }
        out.write("RFB 003.008\n".getBytes(StandardCharsets.US_ASCII));
        out.flush();

        // Types de sécurité
        int count = in.readUnsignedByte();
        if (count == 0) {
            throw new IOException("Connexion refusée : " + readString());
        }
        byte[] types = new byte[count];
        in.readFully(types);
        int chosen = 0;
        for (byte t : types) if (t == 1) chosen = 1;
        if (chosen == 0) for (byte t : types) if (t == 2) chosen = 2;
        if (chosen == 0) throw new IOException("Aucun type de sécurité supporté (VNC auth / None uniquement)");
        out.writeByte(chosen);
        out.flush();

        if (chosen == 2) {
            byte[] challenge = new byte[16];
            in.readFully(challenge);
            out.write(encryptChallenge(challenge, password));
            out.flush();
        }
        if (in.readInt() != 0) {
            throw new IOException("Authentification échouée : " + readString());
        }

        // ClientInit (session partagée) puis ServerInit
        out.writeByte(1);
        out.flush();
        int width = in.readUnsignedShort();
        int height = in.readUnsignedShort();
        in.readFully(new byte[16]); // format de pixel du serveur (on impose le nôtre)
        desktopName = readString();
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        sendPixelFormat();
        sendEncodings();
    }

    private static byte[] encryptChallenge(byte[] challenge, String password) throws Exception {
        byte[] key = new byte[8];
        byte[] pw = password.getBytes(StandardCharsets.ISO_8859_1);
        for (int i = 0; i < 8; i++) {
            key[i] = i < pw.length ? reverseBits(pw[i]) : 0; // VNC inverse l'ordre des bits
        }
        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DES"));
        return cipher.doFinal(challenge);
    }

    private static byte reverseBits(byte b) {
        return (byte) (Integer.reverse(b & 0xFF) >>> 24);
    }

    private String readString() throws IOException {
        byte[] b = new byte[in.readInt()];
        in.readFully(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------- messages client

    /** 32 bits/pixel, little-endian, 0x00RRGGBB : compatible avec TYPE_INT_RGB. */
    private synchronized void sendPixelFormat() throws IOException {
        out.writeByte(0);
        out.write(new byte[3]);
        out.writeByte(32);          // bits per pixel
        out.writeByte(24);          // depth
        out.writeByte(0);           // big-endian = false
        out.writeByte(1);           // true colour
        out.writeShort(255);        // red max
        out.writeShort(255);        // green max
        out.writeShort(255);        // blue max
        out.writeByte(16);          // red shift
        out.writeByte(8);           // green shift
        out.writeByte(0);           // blue shift
        out.write(new byte[3]);
        out.flush();
    }

    private synchronized void sendEncodings() throws IOException {
        out.writeByte(2);
        out.writeByte(0);
        out.writeShort(2);
        out.writeInt(1); // CopyRect
        out.writeInt(0); // Raw
        out.flush();
    }

    private synchronized void requestUpdate(boolean incremental) throws IOException {
        out.writeByte(3);
        out.writeByte(incremental ? 1 : 0);
        out.writeShort(0);
        out.writeShort(0);
        out.writeShort(image.getWidth());
        out.writeShort(image.getHeight());
        out.flush();
    }

    public synchronized void sendPointer(int buttonMask, int x, int y) throws IOException {
        out.writeByte(5);
        out.writeByte(buttonMask);
        out.writeShort(Math.max(0, x));
        out.writeShort(Math.max(0, y));
        out.flush();
    }

    public synchronized void sendKey(int keysym, boolean down) throws IOException {
        out.writeByte(4);
        out.writeByte(down ? 1 : 0);
        out.writeShort(0);
        out.writeInt(keysym);
        out.flush();
    }

    // ------------------------------------------------------- boucle de réception

    /** Boucle bloquante : à lancer dans un thread dédié. */
    public void run(Runnable onUpdate) throws IOException {
        running = true;
        requestUpdate(false);
        while (running) {
            int type = in.readUnsignedByte();
            switch (type) {
                case 0 -> {
                    readFramebufferUpdate();
                    onUpdate.run();
                    requestUpdate(true);
                }
                case 1 -> { // SetColourMapEntries (ignoré : true colour)
                    in.readUnsignedByte();
                    in.readUnsignedShort();
                    int n = in.readUnsignedShort();
                    in.readFully(new byte[n * 6]);
                }
                case 2 -> { /* Bell */ }
                case 3 -> { // ServerCutText (ignoré)
                    in.readFully(new byte[3]);
                    in.readFully(new byte[in.readInt()]);
                }
                default -> throw new IOException("Message serveur inconnu : " + type);
            }
        }
    }

    private void readFramebufferUpdate() throws IOException {
        in.readUnsignedByte(); // padding
        int rects = in.readUnsignedShort();
        for (int r = 0; r < rects; r++) {
            int x = in.readUnsignedShort();
            int y = in.readUnsignedShort();
            int w = in.readUnsignedShort();
            int h = in.readUnsignedShort();
            int encoding = in.readInt();
            switch (encoding) {
                case 0 -> { // Raw
                    byte[] buf = new byte[w * h * 4];
                    in.readFully(buf);
                    int[] px = new int[w * h];
                    for (int i = 0; i < px.length; i++) {
                        int j = i * 4; // octets : B, G, R, inutilisé
                        px[i] = (buf[j] & 0xFF) | (buf[j + 1] & 0xFF) << 8 | (buf[j + 2] & 0xFF) << 16;
                    }
                    image.setRGB(x, y, w, h, px, 0, w);
                }
                case 1 -> { // CopyRect
                    int sx = in.readUnsignedShort();
                    int sy = in.readUnsignedShort();
                    int[] px = image.getRGB(sx, sy, w, h, null, 0, w);
                    image.setRGB(x, y, w, h, px, 0, w);
                }
                default -> throw new IOException("Encodage non supporté : " + encoding);
            }
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }
}
