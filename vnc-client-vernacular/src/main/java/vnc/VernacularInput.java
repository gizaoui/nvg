package vnc;

import com.shinyhut.vernacular.client.VernacularClient;

/**
 * Adapte {@link RemoteInput} à l'API de Vernacular, qui sépare déplacement de la souris
 * ({@code moveMouse}) et état des boutons ({@code updateMouseButton}).
 */
final class VernacularInput implements RemoteInput {

    private final VernacularClient client;
    private int mask;
    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;

    VernacularInput(VernacularClient client) {
        this.client = client;
    }

    @Override
    public synchronized void pointer(int buttonMask, int x, int y) {
        if (x != lastX || y != lastY) {          // pas de message superflu si la souris n'a pas bougé
            client.moveMouse(x, y);
            lastX = x;
            lastY = y;
        }
        int changed = mask ^ buttonMask;
        for (int bit = 0; bit < 8; bit++) {
            if ((changed & (1 << bit)) != 0) {
                client.updateMouseButton(bit + 1, (buttonMask & (1 << bit)) != 0);
            }
        }
        mask = buttonMask;
    }

    @Override
    public void key(int keysym, boolean down) {
        client.updateKey(keysym, down);
    }
}
