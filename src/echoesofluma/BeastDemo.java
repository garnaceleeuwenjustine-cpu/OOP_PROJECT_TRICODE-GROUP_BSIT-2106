package echoesofluma;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Preview for all 28 illustrated beasts with a gentle bob and Java movement. */
public final class BeastDemo extends JPanel {
    private static final int ART_WIDTH = 285;
    private static final int ART_HEIGHT = 300;
    private static final String[] BEAST_NAMES = {
        "Emberwren", "Reefclad", "Thornstride", "Stormhorn", "Citrinax", "Moonwhisk",
        "Fungalump", "Skydrake", "Lantern Beetle", "Pebblekin", "Inkwhorl", "Frostowl",
        "Thornhop", "Coralclaw", "Moonglint", "Kelpcoil", "Dunecrest", "Cogfrog",
        "Petalhart", "Basaltbat", "Geode Snail", "Thunderstoat", "Marshkite", "Starpuff",
        "Vineknuckle", "Honeyhide", "Crockmimic", "Aurorawhale"
    };
    private static final String[] ART_FILES = {
        "01-emberwren.png", "02-reefclad.png", "03-thornstride.png", "04-stormhorn.png",
        "05-citrinax.png", "06-moonwhisk.png", "07-fungalump.jpg", "08-skydrake.jpg",
        "09-lantern-beetle.jpg", "10-pebblekin.jpg", "11-inkwhorl.jpg", "12-frostowl.jpg",
        "13-thornhop.jpg", "14-coralclaw.jpg", "15-moonglint.jpg", "16-kelpcoil.jpg",
        "17-dunecrest.jpg", "18-cogfrog.jpg", "19-petalhart.jpg", "20-basaltbat.jpg",
        "21-geode-snail.jpg", "22-thunderstoat.jpg", "23-marshkite.jpg", "24-starpuff.jpg",
        "25-vineknuckle.jpg", "26-honeyhide.jpg", "27-crockmimic.jpg", "28-aurorawhale.jpg"
    };

    private BufferedImage currentArt;
    private String currentName = "Emberwren";
    private double x = 36;
    private double direction = 1;
    private int tick;
    private long previousNanos = System.nanoTime();

    private BeastDemo() {
        setPreferredSize(new java.awt.Dimension(960, 520));
        setBackground(new Color(209, 235, 243));
        new Timer(16, event -> updateGame()).start();
    }

    private void selectBeast(int index) {
        currentArt = BeastIllustrations.load(ART_FILES[index]);
        currentName = BEAST_NAMES[index];
        x = 36;
        direction = 1;
        repaint();
    }

    private void updateGame() {
        long now = System.nanoTime();
        double deltaSeconds = Math.min((now - previousNanos) / 1_000_000_000.0, 0.05);
        previousNanos = now;
        x += direction * 90.0 * deltaSeconds;
        if (x < 28 || x + ART_WIDTH > getWidth() - 28) {
            direction *= -1;
            x = Math.max(28, Math.min(x, getWidth() - ART_WIDTH - 28));
        }
        tick++;
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setPaint(new GradientPaint(0, 0, new Color(126, 199, 231), 0, 360, new Color(218, 239, 224)));
        g.fillRect(0, 0, getWidth(), getHeight());
        Path2D hills = new Path2D.Double();
        hills.moveTo(0, 308); hills.curveTo(180, 205, 330, 323, 500, 248);
        hills.curveTo(700, 158, 810, 316, getWidth(), 212); hills.lineTo(getWidth(), getHeight());
        hills.lineTo(0, getHeight()); hills.closePath();
        g.setColor(new Color(58, 139, 82)); g.fill(hills);
        g.setColor(new Color(37, 102, 63)); g.fillRect(0, 424, getWidth(), 96);
        g.setColor(new Color(255, 255, 255, 230)); g.fillRoundRect(24, 22, getWidth() - 48, 62, 18, 18);
        g.setColor(new Color(22, 64, 71)); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        g.drawString(currentName + "  ·  Illustrated Beast Preview", 46, 61);
        int bob = (int) Math.round(Math.sin(tick * 0.08) * 5);
        int drawX = (int) Math.round(x);
        g.setColor(new Color(20, 49, 35, 75)); g.fillOval(drawX + 42, 394, 194, 24);
        if (currentArt != null) BeastIllustrations.draw(g, currentArt, drawX, 96 + bob, ART_WIDTH, ART_HEIGHT, 1f, direction < 0);
        g.setColor(new Color(247, 247, 232, 235)); g.fillRoundRect(24, 444, getWidth() - 48, 54, 16, 16);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15)); g.setColor(new Color(35, 64, 66));
        g.drawString("28 new illustrated beasts  ·  gentle idle motion + movement", 48, 477);
        g.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Echoes of Luma — Beast Artwork Preview");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            BeastDemo canvas = new BeastDemo();
            JComboBox<String> beastPicker = new JComboBox<>(BEAST_NAMES);
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
            controls.add(new JLabel("Preview beast:"));
            controls.add(beastPicker);
            beastPicker.addActionListener(event -> canvas.selectBeast(beastPicker.getSelectedIndex()));
            window.setLayout(new BorderLayout());
            window.add(controls, BorderLayout.NORTH);
            window.add(canvas, BorderLayout.CENTER);
            canvas.selectBeast(0);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}