package vnc;

import com.shinyhut.vernacular.client.VernacularClient;
import com.shinyhut.vernacular.client.VernacularConfig;
import ;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;

/** Client VNC Swing basé sur la librairie Vernacular VNC. */
public final class VncViewer extends JFrame {

    private final JTextField hostField = new JTextField("localhost", 14);
    private final JTextField portField = new JTextField("5900", 5);
    private final JPasswordField passwordField = new JPasswordField(10);
    private final JButton connectButton = new JButton("Connexion");
    private final JLabel status = new JLabel("Déconnecté");
    private final ScreenPanel screen = new ScreenPanel();

    private final VernacularClient client;

    public VncViewer() {
        super("Client VNC (Vernacular)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        VernacularConfig config = new VernacularConfig();
        config.setColorDepth(ColorDepth.BPP_24_TRUE);
        config.setUseLocalMousePointer(true);
        config.setPasswordSupplier(() -> new String(passwordField.getPassword()));
        config.setUsernameSupplier(() -> "");
        config.setScreenUpdateListener(screen::setFrame);
        config.setErrorListener(e -> SwingUtilities.invokeLater(() -> {
            status.setText("Erreur : " + e.getMessage());
            resetButton();
        }));
        config.setBellListener(v -> Toolkit.getDefaultToolkit().beep());
        config.setRemoteClipboardListener(t ->
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(t), null));
        config.setMousePointerUpdateListener((image, hotspot) ->
                SwingUtilities.invokeLater(() -> screen.setCursor(
                        Toolkit.getDefaultToolkit().createCustomCursor(image, hotspot, "vnc"))));
        client = new VernacularClient(config);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.add(new JLabel("Hôte :"));
        bar.add(hostField);
        bar.add(new JLabel("Port :"));
        bar.add(portField);
        bar.add(new JLabel("Mot de passe :"));
        bar.add(passwordField);
        bar.add(connectButton);

        add(bar, BorderLayout.NORTH);
        add(new JScrollPane(screen), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        connectButton.addActionListener(e -> {
            if (client.isRunning()) disconnect(); else connect();
        });
        passwordField.addActionListener(e -> connectButton.doClick());

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (client.isRunning()) client.stop();
            }
        });

        setSize(1024, 768);
        setLocationRelativeTo(null);
    }

    private void connect() {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            status.setText("Port invalide");
            return;
        }
        if (port < 100) port += 5900; // numéro d'affichage (1 -> 5901)
        String host = hostField.getText().trim();
        screen.setFrame(null);
        client.start(host, port);
        connectButton.setText("Déconnexion");
        status.setText("Connexion à " + host + ":" + port + "...");
        screen.requestFocusInWindow();
    }

    private void disconnect() {
        client.stop();
        screen.setFrame(null);
        status.setText("Déconnecté");
        resetButton();
    }

    private void resetButton() {
        connectButton.setText("Connexion");
    }

    // ------------------------------------------------------------ écran distant

    private final class ScreenPanel extends JPanel {

        private volatile Image frame;

        ScreenPanel() {
            setFocusable(true);
            setFocusTraversalKeysEnabled(false); // Tab doit partir vers le serveur
            setBackground(Color.DARK_GRAY);

            MouseAdapter mouse = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    if (client.isRunning()) client.updateMouseButton(e.getButton(), true);
                }
                @Override public void mouseReleased(MouseEvent e) {
                    if (client.isRunning()) client.updateMouseButton(e.getButton(), false);
                }
                @Override public void mouseMoved(MouseEvent e) {
                    if (client.isRunning()) client.moveMouse(e.getX(), e.getY());
                }
                @Override public void mouseDragged(MouseEvent e) { mouseMoved(e); }
                @Override public void mouseWheelMoved(MouseWheelEvent e) {
                    if (!client.isRunning()) return;
                    if (e.getWheelRotation() < 0) client.scrollUp(); else client.scrollDown();
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);

            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if (client.isRunning()) client.handleKeyEvent(e);
                }
                @Override public void keyReleased(KeyEvent e) {
                    if (client.isRunning()) client.handleKeyEvent(e);
                }
            });
        }

        /** Appelé depuis le thread de la librairie : on repasse par l'EDT. */
        void setFrame(Image img) {
            SwingUtilities.invokeLater(() -> {
                boolean sizeChanged = img != null && (frame == null
                        || frame.getWidth(null) != img.getWidth(null)
                        || frame.getHeight(null) != img.getHeight(null));
                frame = img;
                if (sizeChanged) {
                    setPreferredSize(new Dimension(img.getWidth(null), img.getHeight(null)));
                    revalidate();
                    status.setText("Connecté (" + img.getWidth(null) + "x" + img.getHeight(null) + ")");
                }
                repaint();
            });
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Image f = frame;
            if (f != null) g.drawImage(f, 0, 0, null);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VncViewer().setVisible(true));
    }
}