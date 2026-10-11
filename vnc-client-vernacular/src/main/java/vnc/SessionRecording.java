package vnc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Enregistrement d'une session : suite d'évènements d'entrée (souris, clavier) horodatés.
 * Les décalages sont relatifs au premier évènement.
 *
 * Format texte (une ligne par évènement) :
 * <pre>
 * # vncrec 1
 * 0 POINTER 1 40 12      décalage_ms POINTER masque_boutons x y
 * 150 KEY 99 1           décalage_ms KEY keysym appui(1)/relâchement(0)
 * </pre>
 */
public final class SessionRecording {

    public enum Kind { POINTER, KEY }

    /** POINTER : a=masque des boutons, b=x, c=y. KEY : a=keysym, b=1 (appui) ou 0 (relâchement), c=0. */
    public record Event(long offsetMs, Kind kind, int a, int b, int c) {

        String toLine() {
            return offsetMs + " " + kind + " " + a + " " + b + " " + c;
        }

        static Event parse(String line) {
            String[] p = line.trim().split("\\s+");
            return new Event(Long.parseLong(p[0]), Kind.valueOf(p[1]),
                    Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]));
        }
    }

    /** Les déplacements de souris plus rapprochés que cela (sans changement de boutons) sont ignorés. */
    private static final long MIN_MOVE_INTERVAL_MS = 25;

    private final List<Event> events = new ArrayList<>();
    private long startNanos;
    private int lastMask;
    private long lastPointerMs = Long.MIN_VALUE / 2;

    public synchronized void addPointer(int mask, int x, int y) {
        long now = offsetNow();
        if (mask == lastMask && now - lastPointerMs < MIN_MOVE_INTERVAL_MS) return;
        lastMask = mask;
        lastPointerMs = now;
        events.add(new Event(now, Kind.POINTER, mask, x, y));
    }

    public synchronized void addKey(int keysym, boolean down) {
        events.add(new Event(offsetNow(), Kind.KEY, keysym, down ? 1 : 0, 0));
    }

    private long offsetNow() {
        if (events.isEmpty()) startNanos = System.nanoTime();
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    public synchronized List<Event> events() {
        return List.copyOf(events);
    }

    public synchronized int size() {
        return events.size();
    }

    public synchronized long durationMs() {
        return events.isEmpty() ? 0 : events.get(events.size() - 1).offsetMs();
    }

    public synchronized void save(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# vncrec 1");
        for (Event e : events) lines.add(e.toLine());
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    public static SessionRecording load(Path file) throws IOException {
        SessionRecording r = new SessionRecording();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            try {
                r.events.add(Event.parse(line));
            } catch (RuntimeException ex) {
                throw new IOException("Ligne " + (i + 1) + " invalide : " + line, ex);
            }
        }
        return r;
    }
}
