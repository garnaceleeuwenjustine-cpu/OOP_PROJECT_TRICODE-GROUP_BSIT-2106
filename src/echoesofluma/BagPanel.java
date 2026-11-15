package echoesofluma;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Gender-themed inventory and bag progression screen, using the supplied bag sheets. */
public final class BagPanel extends JPanel {
    private static final Color INK = new Color(28, 47, 59);
    private final JLabel title = new JLabel("LOLA'S GIFT", SwingConstants.CENTER);
    private final JLabel subtitle = new JLabel("Your travel bag is ready.", SwingConstants.CENTER);
    private final JLabel illustration = new JLabel("", SwingConstants.CENTER);
    private final JLabel levelLabel = new JLabel("BAG LEVEL 1", SwingConstants.CENTER);
    private final JLabel capacityLabel = new JLabel("10 item slots", SwingConstants.CENTER);
    private final JLabel details = new JLabel("", SwingConstants.LEFT);
    private final JLabel inventory = new JLabel("", SwingConstants.LEFT);
    private BufferedImage maleSheet;
    private BufferedImage femaleSheet;
    private boolean female;
    private int bagLevel = 1;

    public BagPanel(Runnable onBack) {
        loadSheets();
        setLayout(new BorderLayout(18, 12));
        setBackground(new Color(237, 243, 248));
        setBorder(BorderFactory.createEmptyBorder(22, 34, 22, 34));
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 5));
        header.setOpaque(false);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27));
        title.setForeground(INK);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        subtitle.setForeground(new Color(86, 106, 117));
        header.add(title);
        header.add(subtitle);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(28, 0));
        body.setOpaque(false);
        illustration.setPreferredSize(new Dimension(340, 315));
        illustration.setOpaque(true);
        illustration.setBackground(Color.WHITE);
        illustration.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(164, 190, 208), 2), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        body.add(illustration, BorderLayout.WEST);

        JPanel info = new JPanel(new BorderLayout(0, 12));
        info.setOpaque(false);
        JPanel stats = new JPanel(new GridLayout(2, 1, 0, 4));
        stats.setOpaque(false);
        levelLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        levelLabel.setForeground(new Color(35, 96, 116));
        capacityLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        capacityLabel.setForeground(INK);
        stats.add(levelLabel);
        stats.add(capacityLabel);
        info.add(stats, BorderLayout.NORTH);

        JPanel lists = new JPanel(new GridLayout(2, 1, 0, 12));
        lists.setOpaque(false);
        details.setVerticalAlignment(SwingConstants.TOP);
        details.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        details.setForeground(INK);
        details.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 215, 222)), BorderFactory.createEmptyBorder(13, 15, 13, 15)));
        inventory.setVerticalAlignment(SwingConstants.TOP);
        inventory.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        inventory.setForeground(INK);
        inventory.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 215, 222)), BorderFactory.createEmptyBorder(13, 15, 13, 15)));
        lists.add(details);
        lists.add(inventory);
        info.add(lists, BorderLayout.CENTER);
        body.add(info, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        JButton back = new JButton("Continue to World Map  →");
        back.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        back.setForeground(Color.WHITE);
        back.setBackground(new Color(31, 105, 115));
        back.setOpaque(true);
        back.setFocusPainted(false);
        back.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        back.addActionListener(event -> onBack.run());
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(back, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    public void setBag(boolean femaleCharacter, int level, int potionCount, boolean gift) {
        female = femaleCharacter;
        bagLevel = Math.max(1, Math.min(5, level));
        title.setText(gift ? "LOLA'S GIFT · YOUR ADVENTURER BAG" : (female ? "TRAVEL BAG · FLORAL STYLE" : "TRAVEL BAG · EXPLORER STYLE"));
        subtitle.setText(gift
            ? "A useful companion for your journey. Clear regions to unlock bigger upgrades."
            : (female ? "Your pink and violet style bag." : "Your blue trailblazer style bag."));
        levelLabel.setText("BAG LEVEL " + bagLevel + "  ·  " + (female ? "FLORAL" : "EXPLORER") + " STYLE");
        capacityLabel.setText((bagLevel * 10) + " item slots  ·  " + Math.max(0, 5 - bagLevel) + " region upgrade" + (bagLevel == 4 ? "" : "s") + " remaining");
        details.setText("<html><b>UNLOCKED FEATURES</b><br>• Basic storage" + (bagLevel >= 2 ? "<br>• Expanded capacity and quick access" : "")
            + (bagLevel >= 3 ? "<br>• Sort items" : "") + (bagLevel >= 4 ? "<br>• Auto-sort and favorites" : "")
            + (bagLevel >= 5 ? "<br>• Quick-use items and special effects" : "")
            + (bagLevel < 5 ? "<br><br><b>NEXT UPGRADE</b><br>Clear the next region to improve your bag." : "<br><br><b>MAXIMUM BAG LEVEL</b>") + "</html>");
        inventory.setText("<html><b>IN YOUR BAG</b><br>• Healing potions: " + Math.max(0, potionCount)
            + "<br>• Capsule families: 8 types"
            + "<br>• Ethics learnings: saved in your journal" + "</html>");
        BufferedImage sheet = female ? femaleSheet : maleSheet;
        // Keep one recognizable bag style for each trainer gender; upgrades affect storage and features.
        BufferedImage crop = cropBag(sheet, 2);
        if (crop != null) {
            illustration.setIcon(new ImageIcon(crop.getScaledInstance(304, 270, java.awt.Image.SCALE_SMOOTH)));
            illustration.setText("");
        } else {
            illustration.setIcon(null);
            illustration.setText(female ? "FLORAL BAG" : "EXPLORER BAG");
        }
    }

    private void loadSheets() {
        maleSheet = load("assets/bags/male-bag-sheet.png");
        femaleSheet = load("assets/bags/female-bag-sheet.png");
    }

    private static BufferedImage cropBag(BufferedImage sheet, int style) {
        if (sheet == null) return null;
        double[] boundaries = {0.0, 0.221, 0.413, 0.605, 0.800, 1.0};
        int bleed = Math.max(1, sheet.getWidth() / 170);
        int x = Math.max(0, (int) Math.round(sheet.getWidth() * boundaries[style]) - bleed);
        int right = Math.min(sheet.getWidth(), (int) Math.round(sheet.getWidth() * boundaries[style + 1]) + bleed);
        int y = Math.min(sheet.getHeight() - 1, (int) Math.round(sheet.getHeight() * .093));
        int width = Math.min(right - x, sheet.getWidth() - x);
        int height = Math.min((int) Math.round(sheet.getHeight() * .315), sheet.getHeight() - y);
        if (width < 1 || height < 1) return null;
        return sheet.getSubimage(x, y, width, height);
    }

    private static BufferedImage load(String file) {
        Path path = Path.of(file);
        try { if (Files.isRegularFile(path)) return ImageIO.read(path.toFile()); }
        catch (IOException | RuntimeException ignored) { }
        return null;
    }
}
