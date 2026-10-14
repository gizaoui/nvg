private void ouvrirNavigateur(URI uri) throws IOException {
    // 1. Desktop, s'il sait vraiment ouvrir un navigateur
    if (Desktop.isDesktopSupported()
            && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(uri);
        return;
    }

    // 2. Navigateur imposé (propriété -Dnavigateur=firefox), sinon liste de repli
    String impose = System.getProperty("navigateur");
    List<String> candidats = impose != null
            ? List.of(impose)
            : List.of("xdg-open", "firefox", "chromium", "google-chrome", "sensible-browser");

    for (String cmd : candidats) {
        try {
            new ProcessBuilder(cmd, uri.toString())
                    .redirectErrorStream(true)
                    .start();
            this.logger.info("Navigateur lancé avec : {}", cmd);
            return;
        } catch (IOException e) {
            this.logger.debug("Commande indisponible : {}", cmd);
        }
    }
    throw new IOException("Aucun navigateur trouvé pour ouvrir " + uri);
}