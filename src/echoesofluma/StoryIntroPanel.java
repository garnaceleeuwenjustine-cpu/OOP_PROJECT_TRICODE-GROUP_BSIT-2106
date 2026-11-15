package echoesofluma;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

/** A short, skippable story scene before the first partner choice. */
public final class StoryIntroPanel extends JPanel {
    private static final String[] LINES = {
        "You grew up in Starting Forest, where your grandmother, Lola, cared for you.",
        "You told Lola that you wanted to find your missing parents.",
        "\"If you hope to find them, first broaden your knowledge and learn from the world,\" Lola said.",
        "\"Choose a Grass, Fire, or Water partner, and I will give you supplies for the journey.\""
    };
    private final Scene scene = new Scene();
    private final Runnable onComplete;
    private BufferedImage trainer;
    private String trainerName = "Adventurer";
    private int lineIndex;
    private int visibleCharacters;
    private Timer typeTimer;

    public StoryIntroPanel(Runnable onComplete) {
        this.onComplete = onComplete;
        setLayout(new BorderLayout());
        add(scene, BorderLayout.CENTER);
        setFocusable(true);
        setToolTipText("Click or press Enter or Space to continue the story.");
        scene.setFocusable(true);
        scene.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) { advance(); }
        });
        scene.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER || event.getKeyCode() == KeyEvent.VK_SPACE || event.getKeyCode() == KeyEvent.VK_Z) advance();
            }
        });
    }

    public void begin(BufferedImage trainerArt, String name) {
        trainer = trainerArt;
        trainerName = name == null ? "Adventurer" : name;
        lineIndex = 0;
        visibleCharacters = 0;
        startTyping();
        scene.requestFocusInWindow();
        repaint();
    }

    private void advance() {
        if (visibleCharacters < LINES[lineIndex].length()) {
            visibleCharacters = LINES[lineIndex].length();
            if (typeTimer != null) typeTimer.stop();
            repaint();
            return;
        }
        if (++lineIndex >= LINES.length) {
            if (typeTimer != null) typeTimer.stop();
            onComplete.run();
            return;
        }
        visibleCharacters = 0;
        startTyping();
        repaint();
    }

    private void startTyping() {
        if (typeTimer != null) typeTimer.stop();
        typeTimer = new Timer(28, event -> {
            visibleCharacters = Math.min(LINES[lineIndex].length(), visibleCharacters + 2);
            scene.repaint();
            if (visibleCharacters >= LINES[lineIndex].length()) typeTimer.stop();
        });
        typeTimer.start();
    }

    private final class Scene extends JPanel {
        private BufferedImage background;

        Scene() {
            background = load("assets/battle-backgrounds/starting-forest.png");
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            double scale = Math.min(getWidth() / 960.0, getHeight() / 540.0);
            int drawW = (int) (960 * scale), drawH = (int) (540 * scale);
            int ox = (getWidth() - drawW) / 2, oy = (getHeight() - drawH) / 2;
            g.translate(ox, oy);
            g.scale(scale, scale);
            drawBackground(g);
            drawPeople(g);
            drawDialogue(g);
            g.dispose();
        }

        private void drawBackground(Graphics2D g) {
            if (background != null) {
                double scale = Math.max(960.0 / background.getWidth(), 540.0 / background.getHeight());
                int w = (int) (background.getWidth() * scale), h = (int) (background.getHeight() * scale);
                g.drawImage(background, (960 - w) / 2, (540 - h) / 2, w, h, this);
                g.setColor(new Color(6, 27, 39, 54));
                g.fillRect(0, 0, 960, 540);
            } else {
                g.setPaint(new GradientPaint(0, 0, new Color(61, 137, 108), 0, 540, new Color(32, 78, 76)));
                g.fillRect(0, 0, 960, 540);
            }
        }

        private void drawPeople(Graphics2D g) {
            g.setColor(new Color(13, 32, 30, 92));
            g.fillOval(222, 425, 160, 24);
            g.fillOval(615, 425, 160, 24);
            if (trainer != null) {
                drawFitted(g, trainer, 235, 100, 145, 333);
            } else {
                g.setColor(new Color(27, 42, 59)); g.fillOval(280, 147, 57, 61);
                g.setColor(new Color(221, 184, 149)); g.fillOval(289, 155, 39, 42);
                g.setColor(new Color(42, 109, 157)); g.fillRoundRect(268, 205, 83, 137, 30, 30);
                g.setColor(new Color(33, 43, 54)); g.fillRoundRect(269, 320, 34, 105, 14, 14); g.fillRoundRect(316, 320, 34, 105, 14, 14);
            }
            drawGrandmother(g, 693, 112);
            g.setColor(new Color(255, 248, 224, 226));
            g.fillRoundRect(665, 76, 112, 28, 13, 13);
            g.setColor(new Color(44, 80, 74));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            centered(g, "LOLA", 721, 95);
            g.setColor(new Color(255, 255, 255, 215));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
            g.drawString(trainerName.toUpperCase(), 86, 74);
        }

        private void drawGrandmother(Graphics2D g, int x, int y) {
            g.setColor(new Color(50, 41, 51)); g.fillOval(x + 24, y + 1, 88, 89);
            g.setColor(new Color(232, 204, 179)); g.fillOval(x + 34, y + 20, 70, 80);
            g.setColor(new Color(249, 238, 224)); g.fillOval(x + 42, y + 9, 55, 30);
            g.setColor(new Color(238, 231, 221)); g.fillOval(x + 93, y + 10, 31, 31);
            g.setColor(new Color(75, 55, 48)); g.fillOval(x + 54, y + 52, 6, 5); g.fillOval(x + 82, y + 52, 6, 5);
            g.setColor(new Color(165, 82, 93)); g.setStroke(new BasicStroke(2));
            g.drawArc(x + 63, y + 60, 20, 12, 200, 140);
            g.setPaint(new GradientPaint(x, y + 105, new Color(138, 99, 141), x + 130, y + 340, new Color(70, 72, 111)));
            g.fillRoundRect(x + 1, y + 93, 133, 250, 60, 60);
            g.setColor(new Color(230, 190, 123));
            g.fillRoundRect(x + 54, y + 112, 25, 75, 15, 15);
            g.setColor(new Color(245, 221, 182, 210));
            g.fillOval(x + 12, y + 157, 32, 33); g.fillOval(x + 93, y + 157, 32, 33);
        }

        private void drawDialogue(Graphics2D g) {
            g.setColor(new Color(7, 27, 43, 205));
            g.fillRoundRect(34, 365, 892, 143, 20, 20);
            g.setColor(new Color(214, 238, 229));
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(34, 365, 892, 143, 20, 20);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            g.setColor(new Color(143, 232, 193));
            g.drawString(lineIndex == 0 ? "STARTING FOREST" : lineIndex == LINES.length - 1 ? "LOLA'S GIFT" : "A PROMISE TO FIND YOUR FAMILY", 60, 395);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
            g.setColor(Color.WHITE);
            drawWrapped(g, LINES[lineIndex].substring(0, Math.min(visibleCharacters, LINES[lineIndex].length())), 60, 428, 816, 28);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            g.setColor(new Color(196, 218, 211));
            String prompt = visibleCharacters < LINES[lineIndex].length() ? "CLICK / ENTER TO REVEAL" : "CLICK / ENTER TO CONTINUE  ▸";
            g.drawString(prompt, 684, 486);
        }

        private void drawWrapped(Graphics2D g, String text, int x, int y, int maxWidth, int lineHeight) {
            FontMetrics metrics = g.getFontMetrics();
            StringBuilder row = new StringBuilder();
            int rowY = y;
            for (String word : text.split(" ")) {
                String candidate = row.isEmpty() ? word : row + " " + word;
                if (metrics.stringWidth(candidate) > maxWidth && !row.isEmpty()) {
                    g.drawString(row.toString(), x, rowY);
                    rowY += lineHeight;
                    row.setLength(0);
                    row.append(word);
                } else {
                    if (!row.isEmpty()) row.append(' ');
                    row.append(word);
                }
            }
            if (!row.isEmpty()) g.drawString(row.toString(), x, rowY);
        }
    }

    private static void drawFitted(Graphics2D g, BufferedImage image, int x, int y, int maxWidth, int maxHeight) {
        double scale = Math.min(maxWidth / (double) image.getWidth(), maxHeight / (double) image.getHeight());
        int width = (int) (image.getWidth() * scale), height = (int) (image.getHeight() * scale);
        g.drawImage(image, x + (maxWidth - width) / 2, y + maxHeight - height, width, height, null);
    }

    private static BufferedImage load(String file) {
        Path path = Path.of(file);
        try { if (Files.isRegularFile(path)) return ImageIO.read(path.toFile()); }
        catch (IOException | RuntimeException ignored) { }
        return null;
    }

    private static void centered(Graphics2D g, String text, int x, int y) {
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, y);
    }
}
