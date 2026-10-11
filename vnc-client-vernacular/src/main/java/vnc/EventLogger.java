package vnc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Consigne les évènements de l'application (connexion, clic, molette, touche...)
 * dans un fichier texte (une ligne par évènement) et les diffuse à des écouteurs (UI).
 *
 * Format d'une ligne : {@code 2026-10-11T07:02:13.123 CLICK bouton=gauche x=120 y=45 clics=1}
 */
public final class EventLogger {

    public enum EventType {
        CONNECT, DISCONNECT, ERROR, CLICK, WHEEL, KEY,
        REC_START, REC_STOP, REC_LOAD, REPLAY_START, REPLAY_END
    }

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final Path file;
    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    public EventLogger(Path file) {
        this.file = file;
    }

    public Path getFile() {
        return file;
    }

    /** L'écouteur est appelé depuis le thread qui journalise (pas forcément l'EDT). */
    public void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    public synchronized void log(EventType type, String details) {
        String line = LocalDateTime.now().format(FORMAT) + " " + type
                + (details == null || details.isEmpty() ? "" : " " + details);
        try {
            Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Journal illisible/inaccessible (" + file + ") : " + e.getMessage());
        }
        for (Consumer<String> l : listeners) l.accept(line);
    }
}
