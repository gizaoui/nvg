import com.shinyhut.vernacular.client.VernacularClient;
import com.shinyhut.vernacular.client.VernacularConfig;
import com.shinyhut.vernacular.client.rendering.ColorDepth;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class VncViewer extends JPanel {

    private volatile Image frame;
    private VernacularClient client;

    public VncViewer() {
        setFocusable(true);
        setFocusTraversalKeysEnabled(false); // pour recevoir Tab

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e)   { client.moveMouse(e.getX(), e.getY()); }
            @Override public void mouseDragged(MouseEvent e) { client.moveMouse(e.getX(), e.getY()); }
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                client.moveMouse(e.getX(), e.getY());
                client.updateMouseButton(e.getButton(), true);
            }
            @Override public void mouseReleased(MouseEvent e) {
                client.updateMouseButton(e.getButton(), false);
            }
            @Override public void mouseWheelMoved(MouseWheelEvent e) {
                int b = e.getWheelRotation() < 0 ? 4 : 5;
                client.updateMouseButton(b, true);
                client.updateMouseButton(b, false);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e)  { client.type(e); }
            @Override public void keyReleased(KeyEvent e) { client.type(e); }
        });
    }

    public void connect(String host, int port, String password) {
        VernacularConfig config = new VernacularConfig();
        config.setColorDepth(ColorDepth.BPP_24_TRUE);
        config.setPasswordSupplier(() -> password);
        config.setScreenUpdateListener(img -> {
            frame = img;
            SwingUtilities.invokeLater(() -> {
                Dimension d = new Dimension(img.getWidth(null), img.getHeight(null));
                if (!d.equals(getPreferredSize())) {
                    setPreferredSize(d);
                    SwingUtilities.getWindowAncestor(this).pack();
                }
                repaint();
            });
        });
        config.setErrorListener(e -> {
            e.printStackTrace();
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, e.getMessage(), "Erreur VNC", JOptionPane.ERROR_MESSAGE));
        });

        client = new VernacularClient(config);
        client.start(host, port);
    }

    public void disconnect() {
        if (client != null) client.stop();
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Image img = frame;
        if (img != null) g.drawImage(img, 0, 0, null);
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5900;
        String pwd = args.length > 2 ? args[2] : "";

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Client VNC - " + host + ":" + port);
            VncViewer viewer = new VncViewer();
            viewer.setPreferredSize(new Dimension(1024, 768));
            f.add(new JScrollPane(viewer));
            f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            f.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) {
                    viewer.disconnect();
                    f.dispose();
                    System.exit(0);
                }
            });
            f.pack();
            f.setVisible(true);
            viewer.connect(host, port, pwd);
        });
    }
}


private static int toKeySym(int keyCode) {
    return switch (keyCode) {
        case KeyEvent.VK_ENTER      -> 0xFF0D;
        case KeyEvent.VK_BACK_SPACE -> 0xFF08;
        case KeyEvent.VK_TAB        -> 0xFF09;
        case KeyEvent.VK_ESCAPE     -> 0xFF1B;
        case KeyEvent.VK_DELETE     -> 0xFFFF;
        case KeyEvent.VK_INSERT     -> 0xFF63;
        case KeyEvent.VK_HOME       -> 0xFF50;
        case KeyEvent.VK_LEFT       -> 0xFF51;
        case KeyEvent.VK_UP         -> 0xFF52;
        case KeyEvent.VK_RIGHT      -> 0xFF53;
        case KeyEvent.VK_DOWN       -> 0xFF54;
        case KeyEvent.VK_PAGE_UP    -> 0xFF55;
        case KeyEvent.VK_PAGE_DOWN  -> 0xFF56;
        case KeyEvent.VK_END        -> 0xFF57;
        default -> (keyCode >= KeyEvent.VK_F1 && keyCode <= KeyEvent.VK_F12)
                ? 0xFFBE + (keyCode - KeyEvent.VK_F1)
                : -1;
    };
}

addKeyListener(new KeyAdapter() {
    // Touches non imprimables : flèches, Entrée, F1..F12, etc.
    @Override public void keyPressed(KeyEvent e) {
        var c = client;
        if (c == null) return;
        int keySym = toKeySym(e.getKeyCode());
        if (keySym != -1) {
            c.type(keySym);
            e.consume();
        }
    }

    // Caractères imprimables (gère déjà Maj, AltGr, accents, etc.)
    @Override public void keyTyped(KeyEvent e) {
        var c = client;
        if (c == null) return;
        char ch = e.getKeyChar();
        if (ch != KeyEvent.CHAR_UNDEFINED && !Character.isISOControl(ch)) {
            c.type(String.valueOf(ch));
        }
    }
});