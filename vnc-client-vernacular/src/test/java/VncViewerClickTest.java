import vnc.SessionRecording;
import vnc.VncViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

/**
 * Scénario : connexion -> clic sur l'écran (journalisé) -> touche -> enregistrement (clic + touche)
 * -> rejeu -> déconnexion.
 *
 * Deux modes :
 *  - sans propriété : faux serveur RFB intégré (vérifie aussi ce que reçoit le serveur) ;
 *  - avec -Dvnc.test.host=... [-Dvnc.test.port=5900] [-Dvnc.test.password=...] [-Dvnc.test.x=5 -Dvnc.test.y=5] :
 *    serveur VNC réel. Aucune frappe de caractère n'est envoyée (seulement Maj/Ctrl) ; le seul clic réel
 *    est un clic gauche en (x, y) sur le bureau distant : choisis un endroit inoffensif.
 * Un affichage graphique (DISPLAY) est nécessaire dans les deux cas.
 */
public class VncViewerClickTest {

    static volatile boolean incrementalRequestSeen;
    static volatile String serverError;
    /** Messages d'entrée reçus par le faux serveur : "P masque x y" ou "K keysym appui". */
    static final List<String> received = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        final String realHost = System.getProperty("vnc.test.host");
        final boolean real = realHost != null;
        final int cx = Integer.getInteger("vnc.test.x", real ? 5 : 40);
        final int cy = Integer.getInteger("vnc.test.y", real ? 5 : 12);

        // Touches de test : en mode réel, uniquement des modificateurs (sans effet visible côté distant)
        final int probeCode = real ? KeyEvent.VK_CONTROL : KeyEvent.VK_A;
        final char probeChar = real ? KeyEvent.CHAR_UNDEFINED : 'a';
        final String probeSym = real ? "0xffe3" : "0x61";
        final int loggedCode = real ? KeyEvent.VK_SHIFT : KeyEvent.VK_B;
        final char loggedChar = real ? KeyEvent.CHAR_UNDEFINED : 'b';
        final String loggedSym = real ? "0xffe1" : "0x62";
        final int recCode = real ? KeyEvent.VK_SHIFT : KeyEvent.VK_C;
        final char recChar = real ? KeyEvent.CHAR_UNDEFINED : 'c';
        final int recSymDec = real ? 0xffe1 : 'c';

        Path logFile = Files.createTempFile("vnc-events", ".log");
        Path recDir = Files.createTempDirectory("vncrec");
        System.setProperty("vnc.eventlog", logFile.toString());
        System.setProperty("vnc.recdir", recDir.toString());

        ServerSocket ss = null;
        if (!real) {
            ss = new ServerSocket(0);
            final ServerSocket fss = ss;
            Thread srv = new Thread(() -> fakeServer(fss), "fake-vnc-server");
            srv.setDaemon(true);
            srv.start();
        }
        final String targetHost = real ? realHost : "127.0.0.1";
        final int targetPort = real ? Integer.getInteger("vnc.test.port", 5900) : ss.getLocalPort();
        System.out.println("Mode : " + (real ? "serveur VNC réel " + targetHost + ":" + targetPort : "faux serveur intégré"));

        VncViewer[] holder = new VncViewer[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new VncViewer();
            holder[0].setVisible(true);
        });
        Container cp = holder[0].getContentPane();
        BorderLayout bl = (BorderLayout) cp.getLayout();

        List<JTextField> fields = new ArrayList<>();
        collect(cp, JTextField.class, fields);
        List<JPasswordField> pwFields = new ArrayList<>();
        collect(cp, JPasswordField.class, pwFields);
        fields.removeIf(f -> f instanceof JPasswordField);
        JTextField host = fields.get(0), port = fields.get(1);
        JPasswordField password = pwFields.get(0);
        List<JButton> buttons = new ArrayList<>();
        collect(cp, JButton.class, buttons);
        JButton button = byText(buttons, "Connexion");
        JButton recordBtn = byText(buttons, "● Enregistrer");
        JButton replayBtn = byText(buttons, "▶ Rejouer");
        List<JCheckBox> boxes = new ArrayList<>();
        collect(cp, JCheckBox.class, boxes);
        JCheckBox logKeys = boxes.get(0);
        List<JTextArea> areas = new ArrayList<>();
        collect(cp, JTextArea.class, areas);
        JTextArea logArea = areas.get(0);
        JLabel status = (JLabel) bl.getLayoutComponent(BorderLayout.SOUTH);
        JScrollPane scroll = (JScrollPane) bl.getLayoutComponent(BorderLayout.CENTER);
        JComponent screen = (JComponent) scroll.getViewport().getView();

        int failures = 0;

        // --- 0. avant connexion : enregistrement et rejeu inactifs
        failures += check("Enregistrer/Rejouer inactifs avant connexion",
                !recordBtn.isEnabled() && !replayBtn.isEnabled(), "");

        // --- 1. clic sur "Connexion"
        SwingUtilities.invokeAndWait(() -> {
            host.setText(targetHost);
            port.setText(String.valueOf(targetPort));
            password.setText(System.getProperty("vnc.test.password", ""));
            button.doClick();
        });
        failures += check("statut après connexion",
                waitFor(() -> status.getText().startsWith("Connecté : "), 8000), status.getText());
        failures += check("évènement CONNECT journalisé",
                waitFor(() -> logContains(logFile, "CONNECT hôte=" + targetHost + ":" + targetPort), 3000), "");
        failures += check("Enregistrer actif, Rejouer inactif (rien d'enregistré)",
                waitFor(recordBtn::isEnabled, 2000) && !replayBtn.isEnabled(), "");

        // --- 2. image du bureau
        failures += check("taille du bureau distant reçue (premier cadre)",
                waitFor(() -> screen.getPreferredSize().width > 10 && screen.getPreferredSize().height > 10, 5000),
                screen.getPreferredSize().width + "x" + screen.getPreferredSize().height);
        if (!real) {
            int[] pixel = new int[1];
            boolean red = waitFor(() -> {
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        BufferedImage img = new BufferedImage(64, 48, BufferedImage.TYPE_INT_RGB);
                        screen.setSize(64, 48);
                        Graphics2D g = img.createGraphics();
                        screen.paint(g);
                        g.dispose();
                        pixel[0] = img.getRGB(10, 10) & 0xFFFFFF;
                    });
                } catch (Exception e) { return false; }
                return pixel[0] == 0xFF0000;
            }, 3000);
            failures += check("pixel (10,10) rouge", red, String.format("0x%06X", pixel[0]));
            failures += check("requête incrémentale reçue par le serveur",
                    waitFor(() -> incrementalRequestSeen, 3000), "");
        } else {
            Thread.sleep(500); // laisse arriver les premières mises à jour
        }

        // --- 3. clic gauche sur l'écran en (20, 30)  [mode réel : (cx, cy)]
        final int x1 = real ? cx : 20, y1 = real ? cy : 30;
        SwingUtilities.invokeAndWait(() -> click(screen, x1, y1));
        failures += check("clic journalisé dans le fichier",
                waitFor(() -> logContains(logFile, "CLICK bouton=gauche x=" + x1 + " y=" + y1 + " clics=1"), 3000), "");
        failures += check("clic affiché dans le panneau de journal",
                waitFor(() -> logArea.getText().contains("CLICK bouton=gauche x=" + x1 + " y=" + y1), 3000), "");
        if (!real) {
            failures += check("clic transmis au serveur", waitFor(() -> count("P 1 20 30") == 1, 3000), "");
        }

        // --- 4. clavier : non journalisé par défaut, journalisé une fois la case cochée
        SwingUtilities.invokeAndWait(() -> pressAndRelease(screen, probeCode, probeChar));
        Thread.sleep(200);
        failures += check("touche test NON journalisée case décochée", !logContains(logFile, "keysym=" + probeSym), "");
        SwingUtilities.invokeAndWait(logKeys::doClick);
        SwingUtilities.invokeAndWait(() -> pressAndRelease(screen, loggedCode, loggedChar));
        failures += check("touche journalisée case cochée",
                waitFor(() -> logContains(logFile, "KEY keysym=" + loggedSym + " etat=down")
                        && logContains(logFile, "KEY keysym=" + loggedSym + " etat=up"), 3000), "");

        // --- 5. ENREGISTREMENT : clic, pause 150 ms, touche
        final int x2 = real ? cx + 1 : 40, y2 = real ? cy : 12; // x différent du 1er clic : le comptage des CLICK reste non ambigu
        SwingUtilities.invokeAndWait(recordBtn::doClick);
        failures += check("bouton = « Arrêter l'enregistrement », Rejouer inactif",
                "■ Arrêter l'enregistrement".equals(recordBtn.getText()) && !replayBtn.isEnabled(), recordBtn.getText());
        SwingUtilities.invokeAndWait(() -> click(screen, x2, y2));
        Thread.sleep(150);
        SwingUtilities.invokeAndWait(() -> pressAndRelease(screen, recCode, recChar));
        SwingUtilities.invokeAndWait(recordBtn::doClick); // arrêt
        failures += check("REC_START journalisé", logContains(logFile, "REC_START"), "");
        failures += check("REC_STOP journalisé avec 4 évènements",
                waitFor(() -> logContains(logFile, "REC_STOP evenements=4 "), 3000), "");

        Path recFile;
        try (Stream<Path> s = Files.list(recDir)) {
            List<Path> files = s.filter(p -> p.toString().endsWith(".vncrec")).toList();
            failures += check("un fichier .vncrec créé", files.size() == 1, files.toString());
            recFile = files.get(0);
        }
        SessionRecording loaded = SessionRecording.load(recFile);
        failures += check("fichier relu : 4 évènements, durée >= 140 ms",
                loaded.size() == 4 && loaded.durationMs() >= 140,
                loaded.size() + " évènements, " + loaded.durationMs() + " ms");
        failures += check("bouton redevenu « Enregistrer », Rejouer actif",
                waitFor(() -> "● Enregistrer".equals(recordBtn.getText()) && replayBtn.isEnabled(), 2000), "");

        // --- 6. REJEU
        if (!real) {
            failures += check("avant rejeu : 1 appui et 1 touche reçus",
                    waitFor(() -> count("P 1 40 12") == 1 && count("K 99 1") == 1, 3000), "");
        }
        long t0 = System.currentTimeMillis();
        SwingUtilities.invokeAndWait(replayBtn::doClick);
        failures += check("REPLAY_END journalisé (4/4)",
                waitFor(() -> logContains(logFile, "REPLAY_END evenements=4/4"), 5000), "");
        long elapsed = System.currentTimeMillis() - t0;
        failures += check("délais d'origine respectés (>= 120 ms)", elapsed >= 120, elapsed + " ms");
        if (!real) {
            failures += check("après rejeu : 2 appuis, au moins 2 relâchements, 2 touches 'c'",
                    waitFor(() -> count("P 1 40 12") == 2 && count("P 0 40 12") >= 2
                            && count("K " + recSymDec + " 1") == 2 && count("K " + recSymDec + " 0") == 2, 3000), "");
        }
        failures += check("bouton redevenu « Rejouer »",
                waitFor(() -> "▶ Rejouer".equals(replayBtn.getText()), 2000), replayBtn.getText());
        failures += check("le rejeu n'est pas journalisé comme un clic local",
                countLog(logFile, "CLICK bouton=gauche x=" + x2 + " y=" + y2) == 1, "");
        if (real) {
            Thread.sleep(300); // laisse le serveur réel traiter ; l'application ne doit signaler aucune erreur
        }

        // --- 7. enregistrement : filtrage des déplacements trop rapprochés + aller-retour fichier
        SessionRecording r = new SessionRecording();
        r.addPointer(0, 1, 1);
        r.addPointer(0, 2, 2);   // < 25 ms, mêmes boutons : ignoré
        r.addPointer(1, 2, 2);   // boutons changés : conservé
        failures += check("déplacements rapprochés filtrés (2 évènements conservés)", r.size() == 2, "" + r.size());
        Path tmp = Files.createTempFile("rt", ".vncrec");
        r.save(tmp);
        failures += check("sauvegarde/chargement identiques", SessionRecording.load(tmp).events().equals(r.events()), "");

        // --- 8. clic sur "Déconnexion"
        SwingUtilities.invokeAndWait(button::doClick);
        failures += check("statut après déconnexion",
                waitFor(() -> "Déconnecté".equals(status.getText()), 3000), status.getText());
        Thread.sleep(500);
        failures += check("statut toujours « Déconnecté » (pas d'erreur parasite)",
                "Déconnecté".equals(status.getText()), status.getText());
        failures += check("évènement DISCONNECT journalisé",
                waitFor(() -> logContains(logFile, "DISCONNECT raison=utilisateur"), 3000), "");
        failures += check("aucun évènement ERROR", !logContains(logFile, " ERROR"), "");
        failures += check("Enregistrer et Rejouer inactifs après déconnexion",
                waitFor(() -> !recordBtn.isEnabled() && !replayBtn.isEnabled(), 2000), "");

        if (serverError != null) { System.out.println("Erreur serveur simulé : " + serverError); failures++; }
        System.out.println("\n--- contenu du journal ---");
        Files.readAllLines(logFile, StandardCharsets.UTF_8).forEach(System.out::println);
        System.out.println("\n--- fichier d'enregistrement ---");
        Files.readAllLines(recFile, StandardCharsets.UTF_8).forEach(System.out::println);
        System.out.println(failures == 0 ? "\nRÉSULTAT : OK" : "\nRÉSULTAT : " + failures + " échec(s)");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------- serveur RFB simulé

    static void fakeServer(ServerSocket ss) {
        try (Socket s = ss.accept()) {
            DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
            out.write("RFB 003.008\n".getBytes()); out.flush();
            in.readFully(new byte[12]);                      // version client
            out.writeByte(1); out.writeByte(1); out.flush(); // 1 type de sécurité : None
            in.readUnsignedByte();                           // choix du client
            out.writeInt(0); out.flush();                    // SecurityResult OK
            in.readUnsignedByte();                           // ClientInit
            out.writeShort(64); out.writeShort(48);          // ServerInit : 64x48, 32 bpp little-endian RGB
            out.writeByte(32); out.writeByte(24); out.writeByte(0); out.writeByte(1);
            out.writeShort(255); out.writeShort(255); out.writeShort(255);
            out.writeByte(16); out.writeByte(8); out.writeByte(0); out.write(new byte[3]);
            byte[] name = "TestDesktop".getBytes();
            out.writeInt(name.length); out.write(name); out.flush();

            int bpp = 32; boolean bigEndian = false; int redMax = 255, redShift = 16;
            boolean updateSent = false;
            while (true) {                                   // messages client, analysés tels que Vernacular les envoie
                int type = in.readUnsignedByte();
                switch (type) {
                    case 0 -> {                              // SetPixelFormat : on mémorise le format choisi par le client
                        in.readFully(new byte[3]);
                        bpp = in.readUnsignedByte(); in.readUnsignedByte();
                        bigEndian = in.readUnsignedByte() != 0; in.readUnsignedByte();
                        redMax = in.readUnsignedShort(); in.readUnsignedShort(); in.readUnsignedShort();
                        redShift = in.readUnsignedByte(); in.readUnsignedByte(); in.readUnsignedByte();
                        in.readFully(new byte[3]);
                    }
                    case 2 -> {                              // SetEncodings
                        in.readUnsignedByte();
                        in.readFully(new byte[4 * in.readUnsignedShort()]);
                    }
                    case 3 -> {                              // FramebufferUpdateRequest
                        int incremental = in.readUnsignedByte();
                        in.readFully(new byte[8]);
                        if (incremental == 1) incrementalRequestSeen = true;
                        if (!updateSent) {
                            updateSent = true;
                            out.writeByte(0); out.writeByte(0); out.writeShort(1); // FramebufferUpdate, 1 rect
                            out.writeShort(0); out.writeShort(0); out.writeShort(64); out.writeShort(48);
                            out.writeInt(0);                 // Raw : tout en rouge, dans le format de pixel négocié
                            long red = ((long) redMax) << redShift;
                            int bytes = bpp / 8;
                            byte[] one = new byte[bytes];
                            for (int i = 0; i < bytes; i++) {
                                int shift = 8 * (bigEndian ? bytes - 1 - i : i);
                                one[i] = (byte) (red >> shift);
                            }
                            for (int i = 0; i < 64 * 48; i++) out.write(one);
                            out.flush();
                        }
                    }
                    case 4 -> {                              // KeyEvent
                        int down = in.readUnsignedByte();
                        in.readUnsignedShort();
                        int sym = in.readInt();
                        synchronized (received) { received.add("K " + sym + " " + down); }
                    }
                    case 5 -> {                              // PointerEvent
                        int mask = in.readUnsignedByte(), x = in.readUnsignedShort(), y = in.readUnsignedShort();
                        synchronized (received) { received.add("P " + mask + " " + x + " " + y); }
                    }
                    case 6 -> {                              // ClientCutText
                        in.readFully(new byte[3]);
                        in.readFully(new byte[in.readInt()]);
                    }
                    default -> throw new IOException("message client inattendu : " + type);
                }
            }
        } catch (EOFException | java.net.SocketException e) {
            // fermeture normale côté client
        } catch (IOException e) {
            serverError = e.toString();
        }
    }

    // ------------------------------------------------------------------ utilitaires

    static int count(String msg) {
        synchronized (received) { return java.util.Collections.frequency(received, msg); }
    }

    static int countLog(Path file, String text) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            int n = 0, i = 0;
            while ((i = content.indexOf(text, i)) >= 0) { n++; i += text.length(); }
            return n;
        } catch (IOException e) {
            return -1;
        }
    }

    static void click(JComponent c, int x, int y) {
        long t = System.currentTimeMillis();
        c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_PRESSED, t,
                InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1));
        c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_RELEASED, t + 10,
                0, x, y, 1, false, MouseEvent.BUTTON1));
    }

    static void pressAndRelease(JComponent c, int code, char ch) {
        long t = System.currentTimeMillis();
        c.dispatchEvent(new KeyEvent(c, KeyEvent.KEY_PRESSED, t, 0, code, ch));
        c.dispatchEvent(new KeyEvent(c, KeyEvent.KEY_RELEASED, t + 5, 0, code, ch));
    }

    static JButton byText(List<JButton> buttons, String text) {
        return buttons.stream().filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow(() -> new IllegalStateException("bouton introuvable : " + text));
    }

    static boolean logContains(Path file, String text) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8).contains(text);
        } catch (IOException e) {
            return false;
        }
    }

    static <T> void collect(Container c, Class<T> type, List<T> out) {
        for (Component k : c.getComponents()) {
            if (type.isInstance(k)) out.add(type.cast(k));
            if (k instanceof Container cc) collect(cc, type, out);
        }
    }

    static boolean waitFor(BooleanSupplier cond, long ms) throws InterruptedException {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) {
            if (cond.getAsBoolean()) return true;
            Thread.sleep(50);
        }
        return cond.getAsBoolean();
    }

    static int check(String label, boolean ok, String detail) {
        System.out.printf("[%s] %s%s%n", ok ? "OK" : "ÉCHEC", label, detail.isEmpty() ? "" : "  (" + detail + ")");
        return ok ? 0 : 1;
    }
}
