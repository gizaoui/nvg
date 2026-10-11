package vnc;

import com.shinyhut.vernacular.client.VernacularClient;
import com.shinyhut.vernacular.client.VernacularConfig;
import com.shinyhut.vernacular.client.exceptions.VncException;
import com.shinyhut.vernacular.client.rendering.ColorDepth;
import vnc.EventLogger.EventType;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Client VNC Swing basé sur la librairie Vernacular VNC :
 * connexion, écran distant, journal des évènements, enregistrement et rejeu des entrées.
 */
public final class VncViewer extends JFrame {

    private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final JTextField hostField = new JTextField("localhost", 14);
    private final JTextField portField = new JTextField("5900", 5);
    private final JPasswordField passwordField = new JPasswordField(10);
    private final JButton connectButton = new JButton("Connexion");
    private final JLabel status = new JLabel("Déconnecté");
    private final ScreenPanel screen = new ScreenPanel();

    // Journalisation (fichier configurable via -Dvnc.eventlog=chemin, défaut : vnc-events.log)
    private final EventLogger events =
            new EventLogger(Path.of(System.getProperty("vnc.eventlog", "vnc-events.log")));
    private final JTextArea logArea = new JTextArea(12, 38);
    private final JCheckBox logKeys = new JCheckBox("Journaliser le clavier (codes des touches)");

    // Enregistrement / rejeu (dossier configurable via -Dvnc.recdir=dossier, défaut : dossier courant)
    private final JButton recordButton = new JButton("● Enregistrer");
    private final JButton replayButton = new JButton("▶ Rejouer");
    private final JButton loadButton = new JButton("Charger…");
    private final JLabel recInfo = new JLabel(" ");
    private final SessionPlayer player = new SessionPlayer();
    private volatile SessionRecording recording;   // non nul pendant l'enregistrement
    private SessionRecording lastRecording;        // dernier enregistrement (ou chargé) : EDT uniquement
    private javax.swing.Timer recTimer;

    private volatile Session session;

    public VncViewer() {
        super("Client VNC (Vernacular)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.add(new JLabel("Hôte :"));
        bar.add(hostField);
        bar.add(new JLabel("Port :"));
        bar.add(portField);
        bar.add(new JLabel("Mot de passe :"));
        bar.add(passwordField);
        bar.add(connectButton);

        JPanel recBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        recBar.add(recordButton);
        recBar.add(replayButton);
        recBar.add(loadButton);
        recBar.add(recInfo);
        recordButton.setToolTipText("Enregistre les clics, déplacements et touches (les touches sont stockées en clair)");

        JPanel north = new JPanel(new GridLayout(2, 1));
        north.add(bar);
        north.add(recBar);

        add(north, BorderLayout.NORTH);
        add(new JScrollPane(screen), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        add(buildLogPanel(), BorderLayout.EAST);

        connectButton.addActionListener(e -> {
            if (liveSession() == null && session == null) connect(); else disconnect();
        });
        passwordField.addActionListener(e -> connectButton.doClick());
        recordButton.addActionListener(e -> toggleRecording());
        replayButton.addActionListener(e -> toggleReplay());
        loadButton.addActionListener(e -> loadRecording());
        updateRecorderButtons();

        setSize(1280, 768);
        setLocationRelativeTo(null);
    }

    private JPanel buildLogPanel() {
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        events.addListener(line -> SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }));

        JButton clear = new JButton("Effacer l'affichage");
        clear.addActionListener(e -> logArea.setText(""));

        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(BorderFactory.createTitledBorder("Journal : " + events.getFile()));
        p.add(logKeys, BorderLayout.NORTH);
        p.add(new JScrollPane(logArea), BorderLayout.CENTER);
        p.add(clear, BorderLayout.SOUTH);
        return p;
    }

    // ------------------------------------------------------ session Vernacular

    /** Une connexion VNC : le client Vernacular, ses entrées et son état. */
    private final class Session {

        final VernacularClient client;
        final VernacularInput input;
        final String target;
        volatile boolean userStop;
        private boolean connected;
        private boolean ended;

        Session(String host, int port, String password) {
            target = host + ":" + port;
            VernacularConfig config = new VernacularConfig();
            config.setColorDepth(ColorDepth.BPP_24_TRUE);
            config.setUsernameSupplier(() -> "");
            config.setPasswordSupplier(() -> password);
            config.setScreenUpdateListener(screen::setFrame);
            config.setBellListener(v -> {
                try { Toolkit.getDefaultToolkit().beep(); } catch (RuntimeException ignored) { }
            });
            config.setRemoteClipboardListener(text -> {
                try {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
                } catch (RuntimeException ignored) { }
            });
            config.setErrorListener(this::onError);
            client = new VernacularClient(config);
            input = new VernacularInput(client);
        }

        synchronized boolean markConnected() {
            if (ended) return false;
            connected = true;
            return true;
        }

        synchronized boolean isLive() {
            return connected && !ended;
        }

        /** Erreur signalée par Vernacular (refus, authentification, coupure...). Vernacular arrête ensuite le client. */
        private void onError(VncException e) {
            if (userStop) return; // arrêt demandé : l'erreur de socket fermée est normale
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            events.log(EventType.ERROR, "message=" + msg);
            SwingUtilities.invokeLater(() -> status.setText("Erreur : " + msg));
            end("serveur ou erreur");
        }

        /** Fin de session (idempotent) : journalise, remet l'interface à zéro, arrête enregistrement et rejeu. */
        void end(String reason) {
            boolean wasConnected;
            synchronized (this) {
                if (ended) return;
                ended = true;
                wasConnected = connected;
            }
            if (wasConnected) events.log(EventType.DISCONNECT, "raison=" + reason);
            SwingUtilities.invokeLater(() -> {
                if (session == this) session = null;
                connectButton.setText("Connexion");
                connectButton.setEnabled(true);
                player.stop();      // sans effet si aucune lecture
                stopRecording();    // sans effet si aucun enregistrement
                updateRecorderButtons();
            });
        }
    }

    private Session liveSession() {
        Session s = session;
        return s != null && s.isLive() ? s : null;
    }

    // --------------------------------------------------------------- connexion

    private void connect() {
        final String host = hostField.getText().trim();
        final String password = new String(passwordField.getPassword());
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            status.setText("Port invalide");
            return;
        }
        if (port < 100) port += 5900; // numéro d'affichage (:1 -> 5901)
        final int finalPort = port;

        Session s = new Session(host, finalPort, password);
        session = s;
        screen.setFrame(null);
        connectButton.setEnabled(false);
        status.setText("Connexion à " + s.target + "...");

        // start() bloque le temps de la poignée de main : hors du thread Swing.
        // Les échecs (hôte injoignable, mot de passe refusé...) arrivent par le listener d'erreurs.
        Thread t = new Thread(() -> {
            s.client.start(host, finalPort);
            if (s.client.isRunning() && s.markConnected()) {
                events.log(EventType.CONNECT, "hôte=" + s.target);
                SwingUtilities.invokeLater(() -> {
                    if (!s.isLive()) return;
                    connectButton.setText("Déconnexion");
                    connectButton.setEnabled(true);
                    status.setText("Connecté : " + s.target);
                    updateRecorderButtons();
                    screen.requestFocusInWindow();
                });
            }
        }, "vnc-connect");
        t.setDaemon(true);
        t.start();
    }

    private void disconnect() {
        Session s = session;
        if (s == null) return;
        s.userStop = true;
        status.setText("Déconnecté");
        s.end("utilisateur");
        Thread t = new Thread(s.client::stop, "vnc-stop"); // stop() peut attendre jusqu'à 1 s
        t.setDaemon(true);
        t.start();
    }

    // ----------------------------------------------- envoi des entrées (+ capture)

    /** Point unique d'envoi souris : transmet au serveur et alimente l'enregistrement. */
    private void sendPointerEvent(int mask, int x, int y) {
        Session s = liveSession();
        if (s == null || player.isPlaying()) return; // entrées locales ignorées pendant un rejeu
        s.input.pointer(mask, x, y);
        SessionRecording r = recording;
        if (r != null) r.addPointer(mask, x, y);
    }

    private void sendKeyEvent(int keysym, boolean down) {
        Session s = liveSession();
        if (s == null || player.isPlaying()) return;
        s.input.key(keysym, down);
        SessionRecording r = recording;
        if (r != null) r.addKey(keysym, down);
    }

    // -------------------------------------------------- enregistrement et rejeu

    private void toggleRecording() {
        if (recording == null) {
            if (liveSession() == null || player.isPlaying()) return;
            recording = new SessionRecording();
            events.log(EventType.REC_START, "");
            recInfo.setText("● REC 0 évènement(s)");
            recTimer = new javax.swing.Timer(500, e -> {
                SessionRecording r = recording;
                if (r != null) recInfo.setText("● REC " + r.size() + " évènement(s)");
            });
            recTimer.start();
        } else {
            stopRecording();
        }
        updateRecorderButtons();
    }

    private void stopRecording() {
        SessionRecording r = recording;
        if (r == null) return;
        recording = null;
        if (recTimer != null) recTimer.stop();

        String details = "evenements=" + r.size() + " duree_ms=" + r.durationMs();
        if (r.size() == 0) {
            recInfo.setText("Enregistrement vide (non sauvegardé)");
        } else {
            lastRecording = r;
            try {
                Path dir = Path.of(System.getProperty("vnc.recdir", "."));
                Path file = dir.resolve("enregistrement-" + LocalDateTime.now().format(FILE_FORMAT) + ".vncrec");
                r.save(file);
                details += " fichier=" + file;
                recInfo.setText(r.size() + " évènement(s) enregistrés : " + file.getFileName());
            } catch (IOException ex) {
                details += " erreur=" + ex.getMessage();
                recInfo.setText("Sauvegarde impossible : " + ex.getMessage());
            }
        }
        events.log(EventType.REC_STOP, details);
        updateRecorderButtons();
    }

    private void toggleReplay() {
        if (player.isPlaying()) {
            player.stop();
            return;
        }
        Session s = liveSession();
        SessionRecording r = lastRecording;
        if (s == null || recording != null || r == null || r.size() == 0) return;
        int total = r.size();
        events.log(EventType.REPLAY_START, "evenements=" + total + " duree_ms=" + r.durationMs());
        player.play(s.input, r.events(), played -> {
            events.log(EventType.REPLAY_END, "evenements=" + played + "/" + total);
            SwingUtilities.invokeLater(this::updateRecorderButtons);
        });
        updateRecorderButtons();
    }

    private void loadRecording() {
        JFileChooser chooser = new JFileChooser(System.getProperty("vnc.recdir", "."));
        chooser.setFileFilter(new FileNameExtensionFilter("Enregistrements VNC (*.vncrec)", "vncrec"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path file = chooser.getSelectedFile().toPath();
        try {
            SessionRecording r = SessionRecording.load(file);
            lastRecording = r;
            events.log(EventType.REC_LOAD, "evenements=" + r.size() + " fichier=" + file);
            recInfo.setText(r.size() + " évènement(s) chargés : " + file.getFileName());
        } catch (IOException ex) {
            recInfo.setText("Chargement impossible : " + ex.getMessage());
        }
        updateRecorderButtons();
    }

    private void updateRecorderButtons() {
        boolean connected = liveSession() != null;
        boolean isRecording = recording != null;
        boolean playing = player.isPlaying();
        recordButton.setText(isRecording ? "■ Arrêter l'enregistrement" : "● Enregistrer");
        recordButton.setEnabled(isRecording || (connected && !playing));
        replayButton.setText(playing ? "■ Arrêter la lecture" : "▶ Rejouer");
        replayButton.setEnabled(playing
                || (connected && !isRecording && lastRecording != null && lastRecording.size() > 0));
        loadButton.setEnabled(!isRecording && !playing);
    }

    private static String buttonName(int awtButton) {
        return switch (awtButton) {
            case MouseEvent.BUTTON1 -> "gauche";
            case MouseEvent.BUTTON2 -> "milieu";
            case MouseEvent.BUTTON3 -> "droit";
            default -> "autre(" + awtButton + ")";
        };
    }

    // ------------------------------------------------------------ écran distant

    private final class ScreenPanel extends JPanel {

        private volatile Image frame;
        private int lastW = -1, lastH = -1;
        private int buttons;
        private final Map<Integer, Integer> pressedKeys = new HashMap<>();

        ScreenPanel() {
            setFocusable(true);
            setFocusTraversalKeysEnabled(false); // Tab doit aller au serveur distant
            setBackground(Color.DARK_GRAY);

            MouseAdapter mouse = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    buttons |= mask(e.getButton());
                    if (liveSession() != null && !player.isPlaying()) {
                        events.log(EventType.CLICK, "bouton=" + buttonName(e.getButton())
                                + " x=" + e.getX() + " y=" + e.getY() + " clics=" + e.getClickCount());
                    }
                    sendPointerEvent(buttons, e.getX(), e.getY());
                }
                @Override public void mouseReleased(MouseEvent e) {
                    buttons &= ~mask(e.getButton());
                    sendPointerEvent(buttons, e.getX(), e.getY());
                }
                @Override public void mouseMoved(MouseEvent e) {
                    sendPointerEvent(buttons, e.getX(), e.getY());
                }
                @Override public void mouseDragged(MouseEvent e) {
                    sendPointerEvent(buttons, e.getX(), e.getY());
                }
                @Override public void mouseWheelMoved(MouseWheelEvent e) {
                    if (liveSession() == null || player.isPlaying()) return;
                    boolean up = e.getWheelRotation() < 0;
                    events.log(EventType.WHEEL, "sens=" + (up ? "haut" : "bas")
                            + " x=" + e.getX() + " y=" + e.getY());
                    int wheel = up ? 8 : 16; // boutons 4 / 5 (un cran = appui puis relâchement)
                    sendPointerEvent(buttons | wheel, e.getX(), e.getY());
                    sendPointerEvent(buttons, e.getX(), e.getY());
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);

            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if (player.isPlaying()) return;
                    int sym = keysym(e);
                    if (sym == 0) return;
                    pressedKeys.put(e.getKeyCode(), sym);
                    logKey(sym, "down");
                    sendKeyEvent(sym, true);
                }
                @Override public void keyReleased(KeyEvent e) {
                    Integer sym = pressedKeys.remove(e.getKeyCode());
                    if (sym == null) return;
                    logKey(sym, "up");
                    sendKeyEvent(sym, false);
                }
            });
        }

        /** Appelé depuis un thread de Vernacular (~30 fois/s au plus). */
        void setFrame(Image img) {
            frame = img;
            if (img != null) {
                int w = img.getWidth(null), h = img.getHeight(null);
                if (w != lastW || h != lastH) {
                    lastW = w;
                    lastH = h;
                    SwingUtilities.invokeLater(() -> {
                        setPreferredSize(new Dimension(w, h));
                        revalidate();
                    });
                }
            } else {
                lastW = lastH = -1;
            }
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Image f = frame;
            if (f != null) g.drawImage(f, 0, 0, null);
        }

        private int mask(int awtButton) {
            return switch (awtButton) {
                case MouseEvent.BUTTON1 -> 1;
                case MouseEvent.BUTTON2 -> 2;
                case MouseEvent.BUTTON3 -> 4;
                default -> 0;
            };
        }

        private void logKey(int sym, String state) {
            // Désactivé par défaut : les codes de touches permettent de reconstituer une saisie (mots de passe).
            if (liveSession() != null && logKeys.isSelected()) {
                events.log(EventType.KEY, String.format("keysym=0x%x etat=%s", sym, state));
            }
        }
    }

    /** Conversion KeyEvent AWT -> keysym X11. */
    private static int keysym(KeyEvent e) {
        char c = e.getKeyChar();
        if (c != KeyEvent.CHAR_UNDEFINED && c >= 0x20 && c != 0x7f) {
            return c < 0x100 ? c : 0x01000000 | c;
        }
        int code = e.getKeyCode();
        if (code >= KeyEvent.VK_F1 && code <= KeyEvent.VK_F12) {
            return 0xffbe + (code - KeyEvent.VK_F1);
        }
        if (code >= KeyEvent.VK_A && code <= KeyEvent.VK_Z) return code + 32; // Ctrl+lettre
        if (code >= KeyEvent.VK_0 && code <= KeyEvent.VK_9) return code;
        return switch (code) {
            case KeyEvent.VK_ENTER -> 0xff0d;
            case KeyEvent.VK_BACK_SPACE -> 0xff08;
            case KeyEvent.VK_TAB -> 0xff09;
            case KeyEvent.VK_ESCAPE -> 0xff1b;
            case KeyEvent.VK_DELETE -> 0xffff;
            case KeyEvent.VK_INSERT -> 0xff63;
            case KeyEvent.VK_HOME -> 0xff50;
            case KeyEvent.VK_LEFT -> 0xff51;
            case KeyEvent.VK_UP -> 0xff52;
            case KeyEvent.VK_RIGHT -> 0xff53;
            case KeyEvent.VK_DOWN -> 0xff54;
            case KeyEvent.VK_PAGE_UP -> 0xff55;
            case KeyEvent.VK_PAGE_DOWN -> 0xff56;
            case KeyEvent.VK_END -> 0xff57;
            case KeyEvent.VK_SHIFT -> 0xffe1;
            case KeyEvent.VK_CONTROL -> 0xffe3;
            case KeyEvent.VK_CAPS_LOCK -> 0xffe5;
            case KeyEvent.VK_ALT -> 0xffe9;
            case KeyEvent.VK_ALT_GRAPH -> 0xfe03;
            case KeyEvent.VK_META, KeyEvent.VK_WINDOWS -> 0xffeb;
            default -> 0;
        };
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VncViewer().setVisible(true));
    }
}
