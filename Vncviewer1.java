private static class ConnexionTask implements Runnable {
    private final VncPanel panel;
    private final String host;
    private final int port;
    private final String pwd;
    private final int n;
    private final Component parent;

    ConnexionTask(VncPanel panel, String host, int port, String pwd, int n, Component parent) {
        this.panel = panel;
        this.host = host;
        this.port = port;
        this.pwd = pwd;
        this.n = n;
        this.parent = parent;
    }

    @Override
    public void run() {
        panel.connect(host, port, pwd, ex ->
            JOptionPane.showMessageDialog(parent, "Session " + n + " : " + ex.getMessage(),
                "Erreur VNC", JOptionPane.ERROR_MESSAGE));
    }
}