package vnc;

import vnc.SessionRecording.Event;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/** Rejoue un enregistrement sur une connexion VNC, en respectant les délais d'origine. */
public final class SessionPlayer {

    private volatile boolean playing;
    private volatile boolean stopRequested;
    private Thread thread;

    public boolean isPlaying() {
        return playing;
    }

    /**
     * Lance la lecture dans un thread dédié.
     * @param onFinished appelé en fin de lecture (terminée ou interrompue) avec le nombre d'évènements rejoués
     */
    public synchronized void play(VncConnection connection, List<Event> events, IntConsumer onFinished) {
        if (playing) throw new IllegalStateException("Lecture déjà en cours");
        playing = true;
        stopRequested = false;
        thread = new Thread(() -> run(connection, events, onFinished), "vnc-replay");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        stopRequested = true;
        if (thread != null) thread.interrupt();
    }

    private void run(VncConnection c, List<Event> events, IntConsumer onFinished) {
        int played = 0;
        int mask = 0, x = 0, y = 0;
        Set<Integer> keysDown = new HashSet<>();
        long begin = System.nanoTime();
        try {
            for (Event e : events) {
                long wait = e.offsetMs() - (System.nanoTime() - begin) / 1_000_000;
                if (wait > 0) Thread.sleep(wait);
                if (stopRequested) break;
                switch (e.kind()) {
                    case POINTER -> {
                        mask = e.a(); x = e.b(); y = e.c();
                        c.sendPointer(mask, x, y);
                    }
                    case KEY -> {
                        boolean down = e.b() == 1;
                        c.sendKey(e.a(), down);
                        if (down) keysDown.add(e.a()); else keysDown.remove(e.a());
                    }
                }
                played++;
            }
        } catch (InterruptedException | IOException ignored) {
            // arrêt demandé, ou connexion perdue
        } finally {
            try { // ne laisse ni bouton ni touche enfoncé côté serveur
                if (mask != 0) c.sendPointer(0, x, y);
                for (int k : keysDown) c.sendKey(k, false);
            } catch (IOException ignored) {
            }
            playing = false;
            onFinished.accept(played);
        }
    }
}
