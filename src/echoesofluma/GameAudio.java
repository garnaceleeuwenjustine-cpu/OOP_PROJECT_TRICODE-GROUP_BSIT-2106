package echoesofluma;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.AWTEventListener;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;

/** Small original chiptune-style effects synthesized at runtime; no downloaded audio is required. */
public final class GameAudio {
    public enum Cue {
        FOCUS, CLICK, BACK, ERROR, OPEN, START,
        CAPSULE_THROW, CAPSULE_OPEN, CAPSULE_BREAK, CATCH,
        ATTACK_FIRE, ATTACK_WATER, ATTACK_GRASS, ATTACK_ELECTRIC, ATTACK_ICE,
        ATTACK_FIGHTING, ATTACK_POISON, ATTACK_GROUND, ATTACK_FLYING, ATTACK_ARCANE,
        ATTACK_BUG, ATTACK_ROCK, ATTACK_GHOST, ATTACK_DRAGON, ATTACK_DARK, ATTACK_STEEL,
        ATTACK_FAIRY, ATTACK_NORMAL, ATTACK_IMPACT,
        TEXT_TICK,
        HEAL, BUFF, ATTUNE, CORRECT, INCORRECT, VICTORY, DEFEAT, LEVEL_UP
    }

    private record Note(double fromHz, double toHz, int millis, double volume, double noise) { }
    private static final String MOVE_TYPE_PROPERTY = "echoes.luma.moveType";

    private static final float SAMPLE_RATE = 22_050f;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    private static final ExecutorService AUDIO_QUEUE = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "echoes-of-luma-audio");
        thread.setDaemon(true);
        return thread;
    });
    private static volatile boolean enabled = true;
    private static volatile boolean openingThemeRequested;
    private static SourceDataLine line;
    private static long randomState = 0x4C554D41534F554EL;

    private GameAudio() { }

    /** Adds classic short focus and confirm sounds to every Swing button in the game. */
    public static void installUiSounds() {
        AWTEventListener actions = event -> {
            if (event instanceof ActionEvent action && action.getSource() instanceof AbstractButton button
                    && button.isEnabled()) {
                Object moveType = button.getClientProperty(MOVE_TYPE_PROPERTY);
                if (moveType instanceof String type) previewMoveType(type);
                else play(Cue.CLICK);
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(actions, AWTEvent.ACTION_EVENT_MASK);

        PropertyChangeListener focus = event -> {
            Object owner = event.getNewValue();
            if (owner instanceof Component component && component.isShowing() && component.isEnabled()
                    && (component instanceof AbstractButton || component instanceof JComboBox<?>)) play(Cue.FOCUS);
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", focus);
    }

    public static void play(Cue cue) {
        if (!enabled || cue == null) return;
        try {
            AUDIO_QUEUE.execute(() -> playNow(cue));
        } catch (RuntimeException ignored) {
            // Sound is optional. A shut-down or unavailable audio device never blocks gameplay.
        }
    }

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) stopOpeningTheme();
    }
    public static void toggle() { setEnabled(!enabled); }

    /** Plays an original short adventure motif that can be stopped when the intro is skipped. */
    public static void startOpeningTheme() {
        if (!enabled) return;
        openingThemeRequested = true;
        try {
            AUDIO_QUEUE.execute(GameAudio::playOpeningThemeNow);
        } catch (RuntimeException ignored) {
            openingThemeRequested = false;
        }
    }

    public static void stopOpeningTheme() { openingThemeRequested = false; }

    private static void playOpeningThemeNow() {
        if (!enabled || !openingThemeRequested) return;
        try {
            if (line == null || !line.isOpen()) {
                line = AudioSystem.getSourceDataLine(FORMAT);
                line.open(FORMAT, 4096);
                line.start();
            }
            while (enabled && openingThemeRequested) {
                byte[] samples = synthesize(openingTheme());
                for (int offset = 0; offset < samples.length && enabled && openingThemeRequested;) {
                    int written = line.write(samples, offset, Math.min(2048, samples.length - offset));
                    if (written <= 0) break;
                    offset += written;
                }
            }
            if (!openingThemeRequested && line != null && line.isOpen()) line.flush();
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException ignored) {
            if (line != null) {
                line.close();
                line = null;
            }
        } finally {
            openingThemeRequested = false;
        }
    }

    private static Note[] openingTheme() {
        return new Note[] {
            n(293.66, 293.66, 300, .075), n(440.00, 440.00, 250, .075),
            n(587.33, 587.33, 300, .080), n(739.99, 739.99, 420, .085),
            n(659.25, 659.25, 250, .075), n(587.33, 587.33, 300, .075),
            n(440.00, 440.00, 400, .080), n(369.99, 369.99, 250, .072),
            n(440.00, 440.00, 250, .075), n(587.33, 587.33, 280, .080),
            n(783.99, 783.99, 420, .085), n(739.99, 739.99, 250, .080),
            n(659.25, 659.25, 250, .078), n(587.33, 587.33, 380, .080),
            n(493.88, 493.88, 250, .075), n(587.33, 587.33, 250, .080),
            n(783.99, 783.99, 440, .088), n(739.99, 739.99, 250, .080),
            n(659.25, 659.25, 280, .078), n(587.33, 587.33, 380, .080),
            n(440.00, 440.00, 250, .075), n(554.37, 554.37, 250, .078),
            n(587.33, 587.33, 300, .082), n(739.99, 739.99, 420, .088),
            n(880.00, 880.00, 260, .082), n(783.99, 783.99, 300, .084),
            n(739.99, 739.99, 260, .080), n(659.25, 659.25, 300, .080),
            n(587.33, 587.33, 380, .082), n(739.99, 739.99, 280, .085),
            n(880.00, 880.00, 280, .090), n(1174.66, 1396.91, 650, .095)
        };
    }

    /** Gives a move button a short type-specific preview instead of the generic click beep. */
    public static void setMoveButtonType(AbstractButton button, String type) {
        if (button != null) button.putClientProperty(MOVE_TYPE_PROPERTY, type);
    }

    private static void previewMoveType(String type) {
        if (!enabled) return;
        Note preview = switch (type == null ? "Normal" : type) {
            case "Fire" -> noisy(440, 620, 48, .09, .14);
            case "Water" -> n(740, 540, 52, .09);
            case "Grass" -> n(520, 760, 50, .09);
            case "Electric" -> n(980, 1450, 42, .085);
            case "Ice" -> n(1120, 780, 54, .085);
            case "Fighting" -> noisy(260, 170, 48, .10, .15);
            case "Poison" -> noisy(480, 340, 56, .085, .16);
            case "Ground" -> noisy(180, 300, 58, .10, .12);
            case "Flying" -> n(860, 1220, 56, .08);
            case "Psychic" -> n(780, 1160, 58, .08);
            case "Bug" -> noisy(590, 850, 46, .085, .12);
            case "Rock" -> noisy(230, 170, 55, .10, .17);
            case "Ghost" -> n(620, 420, 62, .08);
            case "Dragon" -> noisy(360, 680, 60, .095, .13);
            case "Dark" -> n(210, 140, 62, .09);
            case "Steel" -> noisy(760, 650, 48, .085, .10);
            case "Fairy" -> n(1047, 1396, 58, .08);
            default -> n(500, 440, 48, .09);
        };
        try {
            AUDIO_QUEUE.execute(() -> playNow(synthesize(new Note[] {preview})));
        } catch (RuntimeException ignored) { }
    }

    public static Cue attackCue(String type) {
        if (type == null) return Cue.ATTACK_IMPACT;
        return switch (type) {
            case "Fire" -> Cue.ATTACK_FIRE;
            case "Water" -> Cue.ATTACK_WATER;
            case "Grass" -> Cue.ATTACK_GRASS;
            case "Electric" -> Cue.ATTACK_ELECTRIC;
            case "Ice" -> Cue.ATTACK_ICE;
            case "Fighting" -> Cue.ATTACK_FIGHTING;
            case "Poison" -> Cue.ATTACK_POISON;
            case "Ground" -> Cue.ATTACK_GROUND;
            case "Flying" -> Cue.ATTACK_FLYING;
            case "Psychic" -> Cue.ATTACK_ARCANE;
            case "Bug" -> Cue.ATTACK_BUG;
            case "Rock" -> Cue.ATTACK_ROCK;
            case "Ghost" -> Cue.ATTACK_GHOST;
            case "Dragon" -> Cue.ATTACK_DRAGON;
            case "Dark" -> Cue.ATTACK_DARK;
            case "Steel" -> Cue.ATTACK_STEEL;
            case "Fairy" -> Cue.ATTACK_FAIRY;
            case "Normal" -> Cue.ATTACK_NORMAL;
            default -> Cue.ATTACK_IMPACT;
        };
    }

    private static void playNow(Cue cue) {
        if (!enabled) return;
        playNow(synthesize(pattern(cue)));
    }

    private static void playNow(byte[] samples) {
        if (!enabled) return;
        try {
            if (line == null || !line.isOpen()) {
                line = AudioSystem.getSourceDataLine(FORMAT);
                line.open(FORMAT, 4096);
                line.start();
            }
            line.write(samples, 0, samples.length);
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException ignored) {
            // Headless systems, muted machines, and missing sound devices should still run the game.
            if (line != null) {
                line.close();
                line = null;
            }
        }
    }

    private static byte[] synthesize(Note[] notes) {
        int totalSamples = 0;
        for (Note note : notes) totalSamples += SAMPLE_RATE * note.millis / 1000;
        byte[] pcm = new byte[totalSamples * 2];
        int cursor = 0;
        double phase = 0;
        for (Note note : notes) {
            int count = (int) (SAMPLE_RATE * note.millis / 1000);
            for (int i = 0; i < count; i++) {
                double progress = count <= 1 ? 1 : i / (double) (count - 1);
                double frequency = note.fromHz + (note.toHz - note.fromHz) * progress;
                phase += 2 * Math.PI * frequency / SAMPLE_RATE;
                double cycle = phase / (2 * Math.PI);
                cycle -= Math.floor(cycle);
                double square = cycle < .38 ? 1 : -1;
                double wave = square * .72 + Math.sin(phase) * .28;
                randomState ^= randomState << 13;
                randomState ^= randomState >>> 7;
                randomState ^= randomState << 17;
                double noise = ((randomState & 0xFFFF) / 32767.5) - 1;
                double edge = Math.min(1, Math.min(i / (SAMPLE_RATE * .004), (count - i) / (SAMPLE_RATE * .007)));
                double envelope = Math.max(0, edge) * note.volume;
                double mixed = (wave * (1 - note.noise) + noise * note.noise) * envelope;
                short value = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (int) (mixed * 10_500)));
                pcm[cursor++] = (byte) (value & 0xFF);
                pcm[cursor++] = (byte) ((value >>> 8) & 0xFF);
            }
        }
        return pcm;
    }

    private static Note n(double from, double to, int ms, double volume) { return new Note(from, to, ms, volume, 0); }
    private static Note noisy(double from, double to, int ms, double volume, double noise) { return new Note(from, to, ms, volume, noise); }

    private static Note[] pattern(Cue cue) {
        return switch (cue) {
            case FOCUS -> new Note[] {n(980, 1180, 30, .10)};
            case CLICK -> new Note[] {n(630, 720, 34, .16), n(850, 1040, 48, .15)};
            case BACK -> new Note[] {n(760, 560, 45, .15), n(520, 390, 58, .14)};
            case ERROR -> new Note[] {noisy(260, 185, 100, .20, .08), n(190, 160, 65, .15)};
            case OPEN -> new Note[] {n(390, 560, 55, .16), n(610, 910, 85, .18)};
            case START -> new Note[] {n(392, 392, 75, .17), n(523, 523, 75, .17), n(659, 659, 75, .18), n(784, 988, 145, .18)};
            case CAPSULE_THROW -> new Note[] {noisy(920, 290, 105, .20, .22), n(235, 175, 62, .15)};
            case CAPSULE_OPEN -> new Note[] {n(290, 490, 55, .15), n(520, 780, 60, .17), n(880, 1320, 100, .20)};
            case CAPSULE_BREAK -> new Note[] {noisy(880, 540, 52, .16, .18), noisy(420, 210, 75, .18, .30)};
            case CATCH -> new Note[] {n(660, 660, 55, .17), n(880, 880, 55, .18), n(1175, 1175, 70, .18), n(1568, 1760, 180, .19)};
            case ATTACK_FIRE -> new Note[] {noisy(190, 410, 65, .19, .28), noisy(520, 260, 88, .21, .24)};
            case ATTACK_WATER -> new Note[] {n(680, 480, 80, .16), n(480, 900, 100, .19)};
            case ATTACK_GRASS -> new Note[] {n(460, 710, 70, .17), noisy(900, 620, 90, .18, .12)};
            case ATTACK_ARCANE -> new Note[] {n(960, 430, 48, .17), noisy(320, 720, 76, .19, .12), n(980, 1350, 58, .17)};
            case ATTACK_ELECTRIC -> new Note[] {n(1440, 520, 38, .18), noisy(300, 1200, 54, .19, .15), n(1380, 1680, 42, .17)};
            case ATTACK_ICE -> new Note[] {n(1180, 720, 80, .15), n(1568, 1200, 56, .17), noisy(520, 300, 60, .16, .10)};
            case ATTACK_FIGHTING -> new Note[] {noisy(190, 120, 48, .22, .22), n(240, 170, 45, .19), noisy(330, 150, 50, .20, .25)};
            case ATTACK_POISON -> new Note[] {noisy(420, 300, 75, .17, .22), n(360, 260, 65, .16), noisy(300, 180, 55, .18, .20)};
            case ATTACK_GROUND -> new Note[] {noisy(145, 95, 110, .21, .18), n(180, 120, 75, .18)};
            case ATTACK_FLYING -> new Note[] {n(540, 1120, 72, .16), noisy(1100, 680, 80, .16, .09)};
            case ATTACK_BUG -> new Note[] {noisy(720, 1040, 48, .16, .10), n(920, 640, 55, .16), noisy(760, 1220, 45, .15, .08)};
            case ATTACK_ROCK -> new Note[] {noisy(280, 130, 70, .20, .20), noisy(160, 100, 85, .19, .27)};
            case ATTACK_GHOST -> new Note[] {n(500, 740, 65, .14), n(740, 360, 105, .16), n(430, 290, 65, .14)};
            case ATTACK_DRAGON -> new Note[] {noisy(150, 330, 95, .21, .18), n(360, 540, 75, .18), noisy(600, 250, 85, .19, .16)};
            case ATTACK_DARK -> new Note[] {n(240, 130, 110, .19), noisy(170, 220, 85, .18, .12)};
            case ATTACK_STEEL -> new Note[] {noisy(820, 610, 42, .18, .08), n(560, 390, 62, .17), noisy(720, 420, 42, .16, .10)};
            case ATTACK_FAIRY -> new Note[] {n(1175, 1175, 38, .15), n(1568, 1396, 48, .16), n(1760, 2093, 90, .17)};
            case ATTACK_NORMAL -> new Note[] {noisy(420, 240, 48, .18, .12), n(300, 190, 55, .17)};
            case ATTACK_IMPACT -> new Note[] {noisy(300, 170, 100, .20, .23)};
            case TEXT_TICK -> new Note[] {n(1340, 1120, 12, .045)};
            case HEAL -> new Note[] {n(523, 523, 55, .15), n(659, 659, 55, .16), n(784, 988, 90, .18)};
            case BUFF -> new Note[] {n(440, 660, 70, .16), n(660, 880, 110, .18)};
            case ATTUNE -> new Note[] {n(784, 784, 55, .13), n(988, 988, 55, .14), n(1175, 1396, 100, .16)};
            case CORRECT -> new Note[] {n(784, 784, 45, .16), n(1047, 1318, 100, .18)};
            case INCORRECT -> new Note[] {n(310, 250, 90, .16), n(220, 185, 105, .15)};
            case VICTORY -> new Note[] {n(523, 523, 90, .17), n(659, 659, 90, .18), n(784, 784, 90, .18), n(1047, 1318, 190, .20)};
            case DEFEAT -> new Note[] {n(494, 390, 100, .17), n(349, 294, 115, .17), n(220, 165, 180, .17)};
            case LEVEL_UP -> new Note[] {n(659, 659, 55, .15), n(784, 784, 55, .16), n(988, 988, 65, .17), n(1175, 1568, 150, .19)};
        };
    }
}
