package echoesofluma;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Swing preview for all 27 beasts, with an idle loop and simple world movement. */
public final class BeastDemo extends JPanel {
    private static final int DRAW_SCALE = 2;

    private static final String[] BEAST_NAMES = {
        "Pebblit", "Glowfin", "Bramblet", "Emberkit", "Chimelet",
        "Mosskip", "Copperclack", "Mistmoth", "Liltail", "Flickeray",
        "Moonhare", "Dusklynx", "Thundertuft", "Glasswing", "Songroot",
        "Solflare Stag", "Tidewisp Serpent", "Ironhowl", "Skyforge Roc", "Bloomguard",
        "Aurion", "Vesperwing", "Tempest Crown", "Lumenwhale", "Umbralynx",
        "Chronobloom", "Echoryn"
    };

    private static final String[] SPRITE_FILES = {
        "01-pebblit-idle-sheet.png", "02-glowfin-idle-sheet.png",
        "03-bramblet-idle-sheet.png", "04-emberkit-idle-sheet.png",
        "05-chimelet-idle-sheet.png", "06-mosskip-idle-sheet.png",
        "07-copperclack-idle-sheet.png", "08-mistmoth-idle-sheet.png",
        "09-liltail-idle-sheet.png", "10-flickeray-idle-sheet.png",
        "11-moonhare-idle-sheet.png", "12-dusklynx-idle-sheet.png",
        "13-thundertuft-idle-sheet.png", "14-glasswing-idle-sheet.png",
        "15-songroot-idle-sheet.png", "16-solflare-stag-idle-sheet.png",
        "17-tidewisp-serpent-idle-sheet.png", "18-ironhowl-idle-sheet.png",
        "19-skyforge-roc-idle-sheet.png", "20-bloomguard-idle-sheet.png",
        "21-aurion-idle-sheet.png", "22-vesperwing-idle-sheet.png",
        "23-tempest-crown-idle-sheet.png", "24-lumenwhale-idle-sheet.png",
        "25-umbralynx-idle-sheet.png", "26-chronobloom-idle-sheet.png",
        "27-echoryn-idle-sheet.png"
    };

    private BeastAnimator currentBeast;
    private String currentBeastName = "";
    private int drawWidth;
    private double x = 24;
    private double direction = 1;
    private long previousNanos = System.nanoTime();

    private BeastDemo() {
        setPreferredSize(new java.awt.Dimension(800, 360));
        setBackground(new Color(24, 31, 45));

        Timer gameTimer = new Timer(16, event -> updateGame());
        gameTimer.start();
    }

    private void selectBeast(int index) throws IOException {
        String spritePath = "assets/spritesheets/" + SPRITE_FILES[index];
        currentBeast = BeastAnimator.fromFile(spritePath, 8, 0.10);
        currentBeastName = BEAST_NAMES[index];
        drawWidth = currentBeast.getFrameWidth() * DRAW_SCALE;
        x = 24;
        direction = 1;
        repaint();
    }

    private void updateGame() {
        long now = System.nanoTime();
        double deltaSeconds = Math.min((now - previousNanos) / 1_000_000_000.0, 0.05);
        previousNanos = now;

        if (currentBeast != null) {
            currentBeast.update(deltaSeconds);
            x += direction * 110.0 * deltaSeconds;
            if (x < 0 || x + drawWidth > getWidth()) {
                direction *= -1;
                x = Math.max(0, Math.min(x, getWidth() - drawWidth));
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setColor(new Color(238, 240, 245));
            g.drawString(currentBeastName + " — idle animation + Java movement", 24, 32);
            if (currentBeast != null) {
                currentBeast.draw(g, (int) x, 100, DRAW_SCALE);
            }
        } finally {
            g.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Echoes of Luma — Beast Animation Preview");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            BeastDemo canvas = new BeastDemo();
            JComboBox<String> beastPicker = new JComboBox<>(BEAST_NAMES);
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
            controls.add(new JLabel("Preview beast:"));
            controls.add(beastPicker);

            beastPicker.addActionListener(event -> {
                int index = beastPicker.getSelectedIndex();
                try {
                    canvas.selectBeast(index);
                } catch (IOException error) {
                    JOptionPane.showMessageDialog(
                            window,
                            "Could not load " + BEAST_NAMES[index] + ":\n" + error.getMessage(),
                            "Sprite load error",
                            JOptionPane.ERROR_MESSAGE);
                }
            });

            window.setLayout(new BorderLayout());
            window.add(controls, BorderLayout.NORTH);
            window.add(canvas, BorderLayout.CENTER);
            try {
                canvas.selectBeast(0);
            } catch (IOException error) {
                JOptionPane.showMessageDialog(
                        window,
                        "Could not load Pebblit:\n" + error.getMessage(),
                        "Sprite load error",
                        JOptionPane.ERROR_MESSAGE);
            }

            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
