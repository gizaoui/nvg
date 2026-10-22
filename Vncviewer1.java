import com.shinyhut.vernacular.client.VernacularClient;
import com.shinyhut.vernacular.client.VernacularConfig;
import com.shinyhut.vernacular.client.rendering.ColorDepth;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

public class VncPanel extends JPanel {

    private volatile Image frame;
    private volatile VernacularClient client;

    public VncPanel() {
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e)   { move(e); }
            @Override public void mouseDragged(MouseEvent e) { move(e); }
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                move(e);
                button(e.getButton(), true);
            }
            @Override public void mouseReleased(MouseEvent e) { button(e.getButton(), false); }
            @Override public void mouseWheelMoved(MouseWheelEvent e) {
                int b = e.getWheelRotation() < 0 ? 4 : 5;
                button(b, true);
                button(b, false);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e)  { key(e); }
            @Override public void keyReleased(KeyEvent e) { key(e); }
        });
    }

    private void move(MouseEvent e) {
        var c = client;
        if (c != null) c.moveMouse(e.getX(), e.getY());
    }
    private void button(int b, boolean down) {
        var c = client;
        if (c != null) c.updateMouseButton(b, down);
    }
    private void key(KeyEvent e) {
        var c = client;
        if (c != null) c.type(e);
    }

    /** Appelable depuis n'importe quel thread ; la connexion se fait dans le thread fourni par l'appelant. */
    public void connect(String host, int port, String password, Consumer<Exception> onError) {
        VernacularConfig config = new VernacularConfig();
        config.setColorDepth(ColorDepth.BPP_24_TRUE);
        config.setShared(true);
        config.setPasswordSupplier(() -> password);
        config.setScreenUpdateListener(img -> {
            frame = img;
            SwingUtilities.invokeLater(() -> {
                Dimension d = new Dimension(img.getWidth(null), img.getHeight(null));
                if (!d.equals(getPreferredSize())) {
                    setPreferredSize(d);
                    revalidate();
                }
                repaint();
            });
        });
        config.setErrorListener(e -> SwingUtilities.invokeLater(() -> onError.accept(e)));

        VernacularClient c = new VernacularClient(config);
        client = c;
        c.start(host, port); // peut bloquer pendant la poignée de main
    }

    public void disconnect() {
        var c = client;
        client = null;
        if (c != null) c.stop();
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Image img = frame;
        if (img != null) g.drawImage(img, 0, 0, null);
    }
}



import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.*;

public class MultiVncApp {

    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "vnc-connect");
        t.setDaemon(true);
        return t;
    });

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5900;
        String pwd = args.length > 2 ? args[2] : "";

        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Multi-client VNC - " + host + ":" + port);
            JTabbedPane tabs = new JTabbedPane();
            int[] counter = {0};

            JButton add = new JButton("+ Nouvelle connexion");
            add.addActionListener(e -> addSession(tabs, ++counter[0], host, port, pwd));

            f.add(add, BorderLayout.NORTH);
            f.add(tabs, BorderLayout.CENTER);
            f.setSize(1100, 850);
            f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            f.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) {
                    for (int i = 0; i < tabs.getTabCount(); i++) {
                        Component c = ((JScrollPane) tabs.getComponentAt(i)).getViewport().getView();
                        ((VncPanel) c).disconnect();
                    }
                    f.dispose();
                    System.exit(0);
                }
            });
            f.setVisible(true);

            add.doClick(); // première session
        });
    }

    private static void addSession(JTabbedPane tabs, int n, String host, int port, String pwd) {
        VncPanel panel = new VncPanel();
        panel.setPreferredSize(new Dimension(1024, 768));
        JScrollPane scroll = new JScrollPane(panel);
        tabs.addTab("Session " + n, scroll);
        tabs.setSelectedComponent(scroll);

        // Connexion hors EDT : un thread par session
        POOL.submit(() -> panel.connect(host, port, pwd, ex ->
            JOptionPane.showMessageDialog(tabs, "Session " + n + " : " + ex.getMessage(),
                "Erreur VNC", JOptionPane.ERROR_MESSAGE)));
    }
}