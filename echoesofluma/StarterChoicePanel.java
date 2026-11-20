package echoesofluma;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Presents the Grass, Fire, and Water partner choice after the story scene. */
public final class StarterChoicePanel extends JPanel {
    private static final Color INK = new Color(28, 47, 59);
    private final IntConsumer onChoose;

    public StarterChoicePanel(String[] names, String[] types, BufferedImage[] artwork, int[] rosterIndices, IntConsumer onChoose) {
        this.onChoose = onChoose;
        setLayout(new BorderLayout(16, 18));
        setBackground(new Color(235, 243, 239));
        setBorder(BorderFactory.createEmptyBorder(26, 38, 28, 38));
        JLabel title = new JLabel("Choose your first partner", SwingConstants.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        title.setForeground(INK);
        JLabel intro = new JLabel("Lola's gift · Grass, Fire, or Water · each type has an advantage over another", SwingConstants.CENTER);
        intro.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        intro.setForeground(new Color(87, 109, 113));
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 5));
        heading.setOpaque(false);
        heading.add(title);
        heading.add(intro);
        add(heading, BorderLayout.NORTH);

        JPanel choices = new JPanel(new GridLayout(1, 3, 18, 0));
        choices.setOpaque(false);
        Color[] accents = {new Color(70, 154, 91), new Color(220, 106, 64), new Color(67, 143, 204)};
        for (int i = 0; i < names.length; i++) {
            final int selectedIndex = rosterIndices[i];
            JPanel card = new JPanel(new BorderLayout(8, 8));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accents[i], 3), BorderFactory.createEmptyBorder(13, 13, 13, 13)));
            JLabel image = new JLabel(new ImageIcon(artwork[i].getScaledInstance(180, 190, java.awt.Image.SCALE_SMOOTH)), SwingConstants.CENTER);
            image.setPreferredSize(new Dimension(210, 205));
            JLabel name = new JLabel(names[i], SwingConstants.CENTER);
            name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 21));
            name.setForeground(INK);
            JLabel type = new JLabel(types[i].toUpperCase() + " TYPE", SwingConstants.CENTER);
            type.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            type.setForeground(accents[i].darker());
            JButton choose = new JButton("Choose " + names[i]);
            choose.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            choose.setForeground(Color.WHITE);
            choose.setBackground(accents[i]);
            choose.setOpaque(true);
            choose.setFocusPainted(false);
            choose.addActionListener(event -> this.onChoose.accept(selectedIndex));
            JPanel details = new JPanel(new GridLayout(2, 1, 0, 3));
            details.setOpaque(false);
            details.add(name);
            details.add(type);
            card.add(image, BorderLayout.CENTER);
            card.add(details, BorderLayout.NORTH);
            card.add(choose, BorderLayout.SOUTH);
            choices.add(card);
        }
        add(choices, BorderLayout.CENTER);

        JLabel rule = new JLabel("FIRE  ›  GRASS     ·     GRASS  ›  WATER     ·     WATER  ›  FIRE", SwingConstants.CENTER);
        rule.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        rule.setForeground(new Color(50, 88, 80));
        add(rule, BorderLayout.SOUTH);
    }
}
