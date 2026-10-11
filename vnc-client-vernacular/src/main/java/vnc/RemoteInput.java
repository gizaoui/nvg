package vnc;

/** Entrées envoyées au bureau distant : abstraction commune à la saisie en direct et au rejeu. */
public interface RemoteInput {

    /** @param buttonMask bits 0..2 = boutons gauche/milieu/droit, bit 3 = molette haut, bit 4 = molette bas */
    void pointer(int buttonMask, int x, int y);

    /** @param keysym code de touche X11 */
    void key(int keysym, boolean down);
}
