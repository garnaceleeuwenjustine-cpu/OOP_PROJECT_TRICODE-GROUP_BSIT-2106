package echoesofluma;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** A self-contained Swing demo for capsule-based, type-driven beast battles. */
public final class BattleDemo {
    private static final int W = 960;
    private static final int H = 540;
    private static final Color INK = new Color(27, 37, 53);
    private static final Color PANEL = new Color(245, 248, 252);
    private static final Color MUTED = new Color(95, 109, 127);
    private static final Font UI = new Font(Font.SANS_SERIF, Font.PLAIN, 15);
    private static final Font UI_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    private static final Font UI_SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private static final Font DISPLAY = new Font(Font.SANS_SERIF, Font.BOLD, 23);

    private enum Kind { DAMAGE, BUFF, DEBUFF }
    private enum Phase { SELECT, SUMMONING, COMMAND, MOVE_SELECT, ATTUNE_REVEAL, ATTUNE_INPUT, ACTION_NOTICE, ANIMATING, RECALL_PLAYER, RECALL_ENEMY, RESULT }
    private enum EchoSymbol {
        WAVE("WAVE", new Color(48, 138, 190)),
        SPARK("SPARK", new Color(221, 153, 41)),
        LEAF("LEAF", new Color(65, 145, 83)),
        STAR("STAR", new Color(159, 91, 172)),
        MOON("MOON", new Color(76, 107, 167));

        final String label;
        final Color color;

        EchoSymbol(String label, Color color) {
            this.label = label;
            this.color = color;
        }
    }

    private static final int CAPTURE_WEAK_HP = 65;
    private static int nextEchoPatternIndex;

    private static final class Move {
        final String name;
        final String type;
        final Kind kind;
        final String effect;

        Move(String name, String type, Kind kind, String effect) {
            this.name = name;
            this.type = type;
            this.kind = kind;
            this.effect = effect;
        }
    }

    private static final class Beast {
        final String name;
        final String file;
        final String firstType;
        final String secondType;
        final EchoSymbol[] echoPattern;
        final List<Move> moves = new ArrayList<>();
        BufferedImage sprite;
        int hp = 100;
        int buffTurns;
        int weakenedTurns;

        Beast(String name, String file, String firstType, String secondType, String[] moveNames) {
            this.name = name;
            this.file = file;
            this.firstType = firstType;
            this.secondType = secondType;
            int patternCode = (nextEchoPatternIndex++ * 37 + 19) % 125;
            EchoSymbol[] symbols = EchoSymbol.values();
            this.echoPattern = new EchoSymbol[] {
                symbols[patternCode / 25],
                symbols[(patternCode / 5) % 5],
                symbols[patternCode % 5]
            };
            String secondary = secondType == null ? firstType : secondType;
            moves.add(new Move(moveNames[0], firstType, Kind.DAMAGE, "A focused " + firstType.toLowerCase(Locale.ROOT) + " strike."));
            moves.add(new Move(moveNames[1], secondary, Kind.DAMAGE, "A quick " + secondary.toLowerCase(Locale.ROOT) + " attack."));
            moves.add(new Move(moveNames[2], firstType, Kind.DAMAGE, "A heavier " + firstType.toLowerCase(Locale.ROOT) + " burst."));
            moves.add(new Move(moveNames[3], firstType, Kind.BUFF, "Raise your power for the next 3 turns."));
            moves.add(new Move(moveNames[4], secondary, Kind.DEBUFF, "Lower the enemy's guard for 3 turns."));
        }

        @Override public String toString() { return name + "  ·  " + types(); }
        String types() { return secondType == null ? firstType : firstType + " / " + secondType; }
        boolean matchesEcho(List<EchoSymbol> entered) {
            if (entered.size() != echoPattern.length) return false;
            for (int i = 0; i < echoPattern.length; i++) {
                if (entered.get(i) != echoPattern[i]) return false;
            }
            return true;
        }
    }

    private static final Beast[] ROSTER = createRoster();

    private static Beast[] createRoster() {
        return new Beast[] {
            new Beast("Pebblit", "01-pebblit.png", "Rock", "Ground", new String[] {"Pebble Shot", "Faultline Crash", "Shard Volley", "Granite Guard", "Sinkhole Snare"}),
            new Beast("Glowfin", "02-glowfin.png", "Water", null, new String[] {"Rill Dart", "Bubble Lance", "Tidal Burst", "Current Veil", "Drenching Drag"}),
            new Beast("Bramblet", "03-bramblet.png", "Grass", "Poison", new String[] {"Briar Snap", "Venom Seed", "Rootlash", "Canopy Guard", "Sporebind"}),
            new Beast("Emberkit", "04-emberkit.png", "Fire", null, new String[] {"Ember Pounce", "Cinder Rush", "Flame Arc", "Soot Shield", "Scorch Mark"}),
            new Beast("Chimelet", "05-chimelet.png", "Flying", "Psychic", new String[] {"Gale Note", "Mind Chime", "Echo Dive", "Focus Tone", "Dissonance Hex"}),
            new Beast("Mosskip", "06-mosskip.png", "Grass", "Water", new String[] {"Reed Whip", "Spring Jet", "Lily Pad Slam", "Moss Mantle", "Bog Snare"}),
            new Beast("Copperclack", "07-copperclack.png", "Bug", "Steel", new String[] {"Pinch Clamp", "Rivet Rush", "Gearspin", "Iron Carapace", "Resin Lock"}),
            new Beast("Mistmoth", "08-mistmoth.png", "Bug", "Fairy", new String[] {"Dustwing Dart", "Moonlit Dust", "Flutter Flurry", "Gossamer Glow", "Pollen Hush"}),
            new Beast("Liltail", "09-liltail.png", "Normal", "Fairy", new String[] {"Tumble Tap", "Charm Chirp", "Ribbon Rush", "Lucky Step", "Dazzle Bind"}),
            new Beast("Flickeray", "10-flickeray.png", "Water", "Flying", new String[] {"Current Slice", "Jetstream Dive", "Foam Burst", "Tailwind Lift", "Undertow Trap"}),
            new Beast("Moonhare", "11-moonhare.png", "Ice", "Fairy", new String[] {"Frost Hop", "Moonbeam Kick", "Snowflake Volley", "Winter Veil", "Chillbind"}),
            new Beast("Dusklynx", "12-dusklynx.png", "Dark", "Psychic", new String[] {"Shade Pounce", "Mind Rake", "Night Pulse", "Umbral Focus", "Gloom Snare"}),
            new Beast("Thundertuft", "13-thundertuft.png", "Electric", "Rock", new String[] {"Volt Pebble", "Thunderclap", "Static Shard", "Storm Charge", "Grounded Trap"}),
            new Beast("Glasswing", "14-glasswing.png", "Flying", "Steel", new String[] {"Razor Gale", "Wingsteel Dive", "Prism Feather", "Alloy Guard", "Wind Lock"}),
            new Beast("Songroot", "15-songroot.png", "Grass", "Ghost", new String[] {"Root Rattle", "Wisp Seed", "Hollow Vine", "Elder Bark", "Haunt Bind"}),
            new Beast("Solflare Stag", "16-solflare-stag.png", "Fire", "Fairy", new String[] {"Solar Flare", "Radiant Antler", "Sunburst Charge", "Dawn Mantle", "Gleam Snare"}),
            new Beast("Tidewisp Serpent", "17-tidewisp-serpent.png", "Water", "Dragon", new String[] {"Tide Lance", "Wyrm Current", "Deepcoil Surge", "Scale Ward", "Maelstrom Bind"}),
            new Beast("Ironhowl", "18-ironhowl.png", "Steel", "Fighting", new String[] {"Iron Jaw", "Anvil Charge", "Howling Uppercut", "Warplate", "Pressure Roar"}),
            new Beast("Skyforge Roc", "19-skyforge-roc.png", "Fire", "Flying", new String[] {"Forgefire Dive", "Ember Wing", "Meteor Talon", "Updraft Crown", "Ashen Snare"}),
            new Beast("Bloomguard", "20-bloomguard.png", "Grass", "Rock", new String[] {"Petal Slam", "Stonebloom Burst", "Rootstone Crash", "Verdant Bulwark", "Briar Quarry"}),
            new Beast("Aurion", "21-aurion.png", "Fire", "Psychic", new String[] {"Solar Lance", "Mindflare", "Sun Dial Burst", "Crown of Focus", "Afterglow Seal"}),
            new Beast("Vesperwing", "22-vesperwing.png", "Ghost", "Flying", new String[] {"Phantom Talon", "Wraith Gust", "Nightfall Dive", "Veil of Feathers", "Specter Snare"}),
            new Beast("Tempest Crown", "23-tempest-crown.png", "Electric", "Flying", new String[] {"Crownbolt", "Stormwing Dive", "Tempest Arc", "Charged Aegis", "Cloud Lock"}),
            new Beast("Lumenwhale", "24-lumenwhale.png", "Water", "Psychic", new String[] {"Lightcurrent", "Mindtide Pulse", "Abyssal Beam", "Lumen Shield", "Trenchbind"}),
            new Beast("Umbralynx", "25-umbralynx.png", "Dark", "Ghost", new String[] {"Gloomfang", "Shade Rift", "Phantom Pounce", "Dark Mantle", "Dread Snare"}),
            new Beast("Chronobloom", "26-chronobloom.png", "Bug", "Grass", new String[] {"Timepetal Dart", "Thorn Clock", "Bloom Burst", "Spore Tempo", "Stilltime Bind"}),
            new Beast("Echoryn", "27-echoryn.png", "Dragon", "Steel", new String[] {"Echo Fang", "Aegis Tail", "Chronosteel Crash", "Dragon Plate", "Soundless Lock"})
        };
    }

    private final JFrame frame = new JFrame("Echoes of Luma — Beast Arena");
    private final Arena arena = new Arena();
    private final JPanel cards = new JPanel(new java.awt.CardLayout());
    private final JPanel selectCard = new JPanel();
    private final JPanel commandCard = new JPanel();
    private final JPanel moveCard = new JPanel();
    private final JComboBox<Beast> playerChoice = new JComboBox<>(ROSTER);
    private final JComboBox<Beast> enemyChoice = new JComboBox<>(ROSTER);
    private final JComboBox<String> modeChoice = new JComboBox<>(new String[] {"Wild encounter · no trainer", "Trainer battle"});
    private final JLabel hint = new JLabel("Choose a capsule, then open it to release your beast.");
    private final JLabel selectPreview = new JLabel("CAPSULE 01  ·  PEBBLIT", SwingConstants.CENTER);
    private final JButton[] moveButtons = new JButton[5];
    private final JButton captureButton = button("Capture", new Color(145, 75, 176));
    private JButton fightButton;
    private JButton bagButton;
    private JButton attuneButton;
    private JButton listenButton;
    private JButton runButton;

    private BattleDemo() {
        loadSprites();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(860, 760));
        frame.setSize(1040, 820);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(PANEL);
        root.add(arena, BorderLayout.CENTER);
        root.add(cards, BorderLayout.SOUTH);
        buildSelectionCard();
        buildCommandCard();
        buildMoveCard();
        enemyChoice.setSelectedIndex(1);
        cards.add(selectCard, "select");
        cards.add(commandCard, "command");
        cards.add(moveCard, "moves");
        showCard("select");

        playerChoice.addActionListener(e -> updateSelectedPreview());
        enemyChoice.addActionListener(e -> arena.repaint());
        modeChoice.addActionListener(e -> updateCaptureRule());
        arena.startClock();
        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private static void loadSprites() {
        for (Beast beast : ROSTER) {
            try {
                File file = new File("assets/beasts", beast.file);
                if (file.isFile()) beast.sprite = ImageIO.read(file);
                if (beast.sprite == null) {
                    var resource = BattleDemo.class.getResourceAsStream("/assets/beasts/" + beast.file);
                    if (resource != null) beast.sprite = ImageIO.read(resource);
                }
            } catch (Exception ignored) {
                beast.sprite = null;
            }
        }
    }

    private void buildSelectionCard() {
        selectCard.setLayout(new BorderLayout(18, 8));
        selectCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(218, 225, 234)),
            BorderFactory.createEmptyBorder(14, 22, 15, 22)));
        selectCard.setBackground(PANEL);
        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.add(fieldLabel("YOUR CAPSULE"));
        fields.add(playerChoice);
        fields.add(Box.createVerticalStrut(9));
        fields.add(fieldLabel("ENEMY BEAST"));
        fields.add(enemyChoice);
        fields.add(Box.createVerticalStrut(9));
        fields.add(fieldLabel("ENCOUNTER"));
        fields.add(modeChoice);
        for (JComboBox<?> combo : new JComboBox<?>[] {playerChoice, enemyChoice, modeChoice}) {
            combo.setFont(UI);
            combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            combo.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        }
        selectCard.add(fields, BorderLayout.CENTER);
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        selectPreview.setFont(UI_BOLD);
        selectPreview.setForeground(INK);
        selectPreview.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        JButton open = button("Open capsule", new Color(36, 121, 102));
        open.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        open.addActionListener(e -> openCapsule());
        hint.setFont(UI_SMALL);
        hint.setForeground(MUTED);
        hint.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        right.add(Box.createVerticalGlue());
        right.add(selectPreview);
        right.add(Box.createVerticalStrut(12));
        right.add(open);
        right.add(Box.createVerticalStrut(8));
        right.add(hint);
        right.add(Box.createVerticalGlue());
        selectCard.add(right, BorderLayout.EAST);
        selectCard.add(fieldLabel("Choose the beast inside your capsule. Every beast has five moves that use only its assigned type or types."), BorderLayout.NORTH);
        updateSelectedPreview();
    }

    private void buildCommandCard() {
        commandCard.setLayout(new BorderLayout(10, 12));
        commandCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(218, 225, 234)),
            BorderFactory.createEmptyBorder(15, 22, 15, 22)));
        commandCard.setBackground(PANEL);
        JLabel title = new JLabel("Choose your action");
        title.setFont(UI_BOLD);
        title.setForeground(INK);
        commandCard.add(title, BorderLayout.WEST);
        JPanel actions = new JPanel(new java.awt.GridLayout(2, 3, 10, 8));
        actions.setOpaque(false);
        fightButton = button("Fight", new Color(43, 99, 174));
        bagButton = button("Bag  ·  2 potions", new Color(42, 133, 110));
        attuneButton = button("Attune", new Color(116, 83, 164));
        listenButton = button("Listen", new Color(77, 115, 139));
        runButton = button("Run", new Color(93, 107, 128));
        captureButton.addActionListener(e -> capture());
        fightButton.addActionListener(e -> { if (arena.phase == Phase.COMMAND) { refreshMoveButtons(); showCard("moves"); } });
        bagButton.addActionListener(e -> usePotion());
        attuneButton.addActionListener(e -> arena.beginAttunement());
        listenButton.addActionListener(e -> arena.listen());
        runButton.addActionListener(e -> runAway());
        actions.add(fightButton); actions.add(bagButton); actions.add(attuneButton);
        actions.add(listenButton); actions.add(runButton); actions.add(captureButton);
        commandCard.add(actions, BorderLayout.CENTER);
        JLabel legend = new JLabel("3 attacks  ·  1 self-buff  ·  1 enemy debuff");
        legend.setFont(UI_SMALL);
        legend.setForeground(MUTED);
        commandCard.add(legend, BorderLayout.SOUTH);
    }

    private void buildMoveCard() {
        moveCard.setLayout(new BorderLayout(10, 10));
        moveCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(218, 225, 234)),
            BorderFactory.createEmptyBorder(12, 18, 13, 18)));
        moveCard.setBackground(PANEL);
        JPanel grid = new JPanel(new java.awt.GridLayout(1, 5, 8, 0));
        grid.setOpaque(false);
        for (int i = 0; i < 5; i++) {
            final int slot = i;
            moveButtons[i] = button("Move " + (i + 1), new Color(47, 93, 151));
            moveButtons[i].setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            moveButtons[i].addActionListener(e -> chooseMove(slot));
            grid.add(moveButtons[i]);
        }
        moveCard.add(grid, BorderLayout.CENTER);
        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        JLabel rule = new JLabel("Move types are limited to your beast's listed types.");
        rule.setForeground(MUTED); rule.setFont(UI_SMALL);
        JButton back = button("Back", new Color(93, 107, 128));
        back.addActionListener(e -> showCard("command"));
        foot.add(rule, BorderLayout.WEST); foot.add(back, BorderLayout.EAST);
        moveCard.add(foot, BorderLayout.SOUTH);
    }

    private void openCapsule() {
        Beast player = (Beast) playerChoice.getSelectedItem();
        Beast enemy = (Beast) enemyChoice.getSelectedItem();
        if (player == enemy) {
            enemy = ROSTER[(indexOf(enemy) + 1) % ROSTER.length];
            enemyChoice.setSelectedItem(enemy);
            hint.setText("Choose a different beast as your opponent.");
            return;
        }
        arena.player = player;
        arena.enemy = enemy;
        arena.player.hp = arena.enemy.hp = 100;
        arena.player.buffTurns = arena.player.weakenedTurns = 0;
        arena.enemy.buffTurns = arena.enemy.weakenedTurns = 0;
        arena.attunementBonus = false;
        arena.attunementEffect = false;
        arena.captureBreakEffect = false;
        arena.enteredEcho.clear();
        arena.pendingEnemyMove = null;
        arena.enemyTurnPendingAfterCapture = false;
        arena.potions = 2;
        updateBagLabel();
        arena.trainerBattle = modeChoice.getSelectedIndex() == 1;
        arena.phase = Phase.SUMMONING;
        arena.frameNo = 0;
        arena.message = player.name + "'s capsule is opening!";
        setActionsEnabled(false);
        arena.requestFocusInWindow();
        cards.setVisible(false);
        frame.revalidate();
        updateCaptureRule();
        showCard("command");
        arena.repaint();
    }

    private void updateCaptureRule() {
        if (arena.player != null && arena.phase != Phase.SELECT) {
            arena.trainerBattle = modeChoice.getSelectedIndex() == 1;
        }
        captureButton.setEnabled(arena.phase == Phase.COMMAND && arena.canCapture());
        if (arena.trainerBattle) {
            captureButton.setToolTipText("Trainer-owned Beasts cannot be captured.");
        } else if (arena.enemy != null && arena.enemy.hp > CAPTURE_WEAK_HP) {
            captureButton.setToolTipText("Weaken the wild Beast to 65 HP or less first.");
        } else {
            captureButton.setToolTipText("Throw a capsule at the weakened wild Beast.");
        }
        if (attuneButton != null) attuneButton.setEnabled(arena.phase == Phase.COMMAND && arena.canAttune());
        if (listenButton != null) listenButton.setEnabled(arena.phase == Phase.COMMAND && arena.pendingEnemyMove == null);
        arena.repaint();
    }

    private void setActionsEnabled(boolean enabled) {
        if (fightButton != null) fightButton.setEnabled(enabled);
        if (bagButton != null) bagButton.setEnabled(enabled);
        if (attuneButton != null) attuneButton.setEnabled(enabled && arena.canAttune());
        if (listenButton != null) listenButton.setEnabled(enabled && arena.pendingEnemyMove == null);
        if (runButton != null) runButton.setEnabled(enabled);
        captureButton.setEnabled(enabled && arena.canCapture());
    }

    private void updateBagLabel() {
        if (bagButton != null) bagButton.setText("Bag  ·  " + arena.potions + (arena.potions == 1 ? " potion" : " potions"));
    }

    private void updateSelectedPreview() {
        Beast beast = (Beast) playerChoice.getSelectedItem();
        if (beast == null) return;
        selectPreview.setText("CAPSULE  ·  " + beast.name.toUpperCase(Locale.ROOT) + "  ·  " + beast.types().toUpperCase(Locale.ROOT));
        arena.preview = beast;
        arena.repaint();
    }

    private void refreshMoveButtons() {
        Beast player = arena.player;
        if (player == null) return;
        for (int i = 0; i < moveButtons.length; i++) {
            Move move = player.moves.get(i);
            String tag = switch (move.kind) { case DAMAGE -> "ATTACK"; case BUFF -> "SELF BUFF"; case DEBUFF -> "ENEMY DEBUFF"; };
            moveButtons[i].setText("<html><center>" + move.name + "<br><span style='font-weight:normal'>" + move.type + "  ·  " + tag + "</span></center></html>");
            moveButtons[i].setToolTipText(move.effect);
            moveButtons[i].setEnabled(arena.phase == Phase.COMMAND || arena.phase == Phase.MOVE_SELECT);
        }
    }

    private void chooseMove(int slot) {
        if (arena.phase != Phase.COMMAND && arena.phase != Phase.MOVE_SELECT) return;
        arena.move = arena.player.moves.get(slot);
        arena.effectActor = null;
        arena.effectKind = null;
        arena.menuFocus = 0;
        arena.phase = Phase.ANIMATING;
        arena.frameNo = 0;
        arena.message = arena.player.name + " used " + arena.move.name + "!";
        setActionsEnabled(false);
        arena.requestFocusInWindow();
        refreshMoveButtons();
        showCard("command");
        arena.repaint();
    }

    private void usePotion() {
        if (arena.phase != Phase.COMMAND) return;
        if (arena.potions <= 0) { arena.message = "Your bag is empty."; arena.repaint(); return; }
        if (arena.player.hp >= 100) { arena.message = arena.player.name + " is already at full health."; arena.repaint(); return; }
        arena.potions--;
        updateBagLabel();
        arena.player.hp = Math.min(100, arena.player.hp + 32);
        arena.effectKind = Kind.BUFF; arena.effectActor = arena.player; arena.phase = Phase.ANIMATING; arena.frameNo = 0;
        arena.message = "Potion restored 32 HP. " + arena.potions + " left.";
        setActionsEnabled(false);
        arena.requestFocusInWindow();
        arena.repaint();
    }

    private void runAway() {
        if (arena.phase != Phase.COMMAND) return;
        arena.message = "You returned " + arena.player.name + " to its capsule and escaped.";
        arena.phase = Phase.RECALL_PLAYER; arena.frameNo = 0; arena.afterRecall = Phase.SELECT;
        setActionsEnabled(false);
        arena.requestFocusInWindow();
        arena.repaint();
    }

    private void capture() {
        if (arena.phase != Phase.COMMAND) return;
        if (arena.trainerBattle) {
            arena.showActionNotice("A Beast with a trainer cannot be captured.");
            return;
        }
        if (!arena.canCapture()) {
            arena.showActionNotice("Weaken " + arena.enemy.name + " to 65 HP or less before throwing a capsule.");
            return;
        }
        arena.phase = Phase.ANIMATING; arena.frameNo = 0; arena.capturing = true;
        double chance = arena.captureChance();
        arena.captureSuccess = arena.random.nextDouble() < chance;
        arena.attunementBonus = false;
        arena.captureBreakEffect = false;
        arena.enemyTurnPendingAfterCapture = false;
        arena.message = String.format(Locale.ROOT, "Capsule thrown with a %d%% chance. It is not guaranteed!",
            (int) Math.round(chance * 100));
        setActionsEnabled(false);
        arena.requestFocusInWindow();
        arena.repaint();
    }

    private void showCard(String name) {
        ((java.awt.CardLayout) cards.getLayout()).show(cards, name);
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UI_SMALL); label.setForeground(MUTED); label.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JButton button(String text, Color color) {
        JButton b = new JButton(text);
        b.setFont(UI_BOLD); b.setForeground(Color.WHITE); b.setBackground(color);
        b.setOpaque(true); b.setFocusPainted(false); b.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        return b;
    }

    private static int indexOf(Beast beast) {
        for (int i = 0; i < ROSTER.length; i++) if (ROSTER[i] == beast) return i;
        return 0;
    }

    private void returnToCapsules(String text) {
        arena.message = text;
        arena.phase = Phase.SELECT;
        arena.player = null;
        arena.enemy = null;
        arena.frameNo = 0;
        updateSelectedPreview();
        showCard("select");
        cards.setVisible(true);
        frame.revalidate();
        hint.setText(text);
    }

    private final class Arena extends JPanel {
        Beast player;
        Beast enemy;
        Beast preview = ROSTER[0];
        Move move;
        Kind effectKind;
        Beast effectActor;
        Phase phase = Phase.SELECT;
        Phase afterRecall = Phase.SELECT;
        String message = "Select a capsule to begin.";
        boolean trainerBattle;
        boolean capturing;
        boolean captureSuccess;
        boolean attunementBonus;
        boolean attunementEffect;
        boolean captureBreakEffect;
        boolean enemyTurnPendingAfterCapture;
        Move pendingEnemyMove;
        private final List<EchoSymbol> enteredEcho = new ArrayList<>(3);
        int potions = 2;
        int frameNo;
        int tick;
        int playerTurns;
        int menuFocus;
        private String lastDialogText = "";
        private Phase lastDialogPhase;
        private int visibleDialogChars;
        private boolean dialogDismissed;
        private final Random random = new Random(7);

        Arena() {
            setPreferredSize(new Dimension(W, H));
            setMinimumSize(new Dimension(700, 410));
            setBackground(new Color(220, 238, 244));
            setFocusable(true);
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    int[] point = toVirtual(e.getX(), e.getY());
                    if (isDialogActive() && point[0] >= 16 && point[0] <= 944 && point[1] >= 447 && point[1] <= 526) {
                        advanceDialog();
                    } else {
                        handleMenuClick(e.getX(), e.getY());
                    }
                }
            });
            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if (isDialogActive() && (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_Z)) {
                        advanceDialog();
                    } else if (phase == Phase.ATTUNE_INPUT) {
                        int key = e.getKeyCode();
                        if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_5) {
                            enterEchoSymbol(EchoSymbol.values()[key - KeyEvent.VK_1]);
                        } else if (key >= KeyEvent.VK_NUMPAD1 && key <= KeyEvent.VK_NUMPAD5) {
                            enterEchoSymbol(EchoSymbol.values()[key - KeyEvent.VK_NUMPAD1]);
                        }
                    }
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { updateMenuFocus(e.getX(), e.getY()); }
            });
        }

        private boolean isDialogActive() {
            return phase == Phase.SUMMONING || phase == Phase.ANIMATING || phase == Phase.RECALL_PLAYER
                || phase == Phase.RECALL_ENEMY || phase == Phase.ACTION_NOTICE || phase == Phase.RESULT;
        }

        private String dialogText() {
            if (phase == Phase.SUMMONING && player != null) return "Capsule opens. Releasing " + player.name + "...";
            return message == null ? "" : message;
        }

        private void syncDialog() {
            String text = dialogText();
            if (phase != lastDialogPhase || !text.equals(lastDialogText)) {
                lastDialogPhase = phase;
                lastDialogText = text;
                visibleDialogChars = 0;
                dialogDismissed = false;
            }
        }

        private boolean dialogCanContinue() {
            return dialogDismissed;
        }

        private boolean canCapture() {
            return enemy != null && !trainerBattle && enemy.hp <= CAPTURE_WEAK_HP;
        }

        private boolean canAttune() {
            return enemy != null && !trainerBattle && !attunementBonus && enemy.hp <= CAPTURE_WEAK_HP;
        }

        private void showActionNotice(String text) {
            message = text;
            phase = Phase.ACTION_NOTICE;
            frameNo = 0;
            setActionsEnabled(false);
            requestFocusInWindow();
            repaint();
        }

        private void beginAttunement() {
            if (phase != Phase.COMMAND) return;
            if (trainerBattle) {
                showActionNotice("Echo Attunement only resonates with wild Beasts.");
                return;
            }
            if (attunementBonus) {
                showActionNotice(enemy.name + " is already attuned. Its capsule bonus is ready.");
                return;
            }
            if (enemy.hp > CAPTURE_WEAK_HP) {
                showActionNotice("Weaken " + enemy.name + " to 65 HP or less before attuning its Echo.");
                return;
            }
            enteredEcho.clear();
            phase = Phase.ATTUNE_REVEAL;
            frameNo = 0;
            message = "Watch the three-beat Echo.";
            menuFocus = 0;
            setActionsEnabled(false);
            requestFocusInWindow();
            repaint();
        }

        private void enterEchoSymbol(EchoSymbol symbol) {
            if (phase != Phase.ATTUNE_INPUT || enteredEcho.size() >= 3) return;
            enteredEcho.add(symbol);
            if (enteredEcho.size() == 3) finishAttunement(false);
            else repaint();
        }

        private void finishAttunement(boolean cancelled) {
            if (phase != Phase.ATTUNE_INPUT) return;
            attunementBonus = !cancelled && enemy.matchesEcho(enteredEcho);
            enteredEcho.clear();
            phase = Phase.ANIMATING;
            frameNo = 0;
            move = null;
            effectActor = player;
            effectKind = null;
            attunementEffect = true;
            message = attunementBonus
                ? "Echo matched! Your next capsule has an improved chance to catch " + enemy.name + "."
                : "The Echo slips away. No capture bonus this time.";
            setActionsEnabled(false);
            requestFocusInWindow();
            repaint();
        }

        private void listen() {
            if (phase != Phase.COMMAND) return;
            if (pendingEnemyMove != null) {
                showActionNotice("You have already heard the foe's next move. Use that clue to plan.");
                return;
            }
            pendingEnemyMove = enemy.moves.get(random.nextInt(enemy.moves.size()));
            phase = Phase.ANIMATING;
            frameNo = 0;
            move = null;
            effectActor = player;
            effectKind = null;
            message = "Listen used your turn. The foe's next move is " + moveCategory(pendingEnemyMove.kind) + ".";
            setActionsEnabled(false);
            requestFocusInWindow();
            repaint();
        }

        private double captureChance() {
            if (enemy == null) return 0;
            double chance = 0.20 + (100 - enemy.hp) * 0.005;
            if (enemy.weakenedTurns > 0) chance += 0.10;
            if (attunementBonus) chance += 0.25;
            return Math.min(0.90, chance);
        }

        private String moveCategory(Kind kind) {
            return switch (kind) {
                case DAMAGE -> "an ATTACK";
                case BUFF -> "a SELF-BUFF";
                case DEBUFF -> "a DEBUFF";
            };
        }

        private void advanceDialog() {
            syncDialog();
            if (!dialogDismissed && visibleDialogChars < lastDialogText.length()) {
                visibleDialogChars = lastDialogText.length();
            } else {
                dialogDismissed = true;
            }
            repaint();
        }

        private void updateMenuFocus(int mouseX, int mouseY) {
            int[] point = toVirtual(mouseX, mouseY);
            if (phase == Phase.COMMAND && point[0] >= 20 && point[0] <= 430 && point[1] >= 396 && point[1] <= 526) {
                int col = Math.min(2, Math.max(0, (point[0] - 20) / 137));
                int row = point[1] < 461 ? 0 : 1;
                menuFocus = row * 3 + col;
                repaint();
            } else if (phase == Phase.MOVE_SELECT && point[0] >= 20 && point[0] <= 430 && point[1] >= 390 && point[1] <= 526) {
                menuFocus = point[1] < 510 ? Math.min(4, Math.max(0, (point[1] - 390) / 24)) : 5;
                repaint();
            } else if (phase == Phase.ATTUNE_INPUT && point[1] >= 402 && point[1] <= 496) {
                int symbolIndex = echoSymbolAt(point[0]);
                if (symbolIndex >= 0) {
                    menuFocus = symbolIndex;
                    repaint();
                }
            }
        }

        private void handleMenuClick(int mouseX, int mouseY) {
            int[] point = toVirtual(mouseX, mouseY);
            int x = point[0], y = point[1];
            if (phase == Phase.COMMAND && x >= 20 && x <= 430 && y >= 396 && y <= 526) {
                int col = Math.min(2, Math.max(0, (x - 20) / 137));
                int row = y < 461 ? 0 : 1;
                int choice = row * 3 + col;
                menuFocus = choice;
                switch (choice) {
                    case 0 -> phase = Phase.MOVE_SELECT;
                    case 1 -> usePotion();
                    case 2 -> beginAttunement();
                    case 3 -> listen();
                    case 4 -> runAway();
                    case 5 -> capture();
                }
                repaint();
            } else if (phase == Phase.MOVE_SELECT && x >= 20 && x <= 430 && y >= 390 && y <= 526) {
                if (y >= 510) {
                    phase = Phase.COMMAND; menuFocus = 0;
                } else {
                    int slot = (y - 390) / 24;
                    if (slot >= 0 && slot < 5) chooseMove(slot);
                }
                repaint();
            } else if (phase == Phase.ATTUNE_INPUT && y >= 402 && y <= 496) {
                int symbolIndex = echoSymbolAt(x);
                if (symbolIndex >= 0) enterEchoSymbol(EchoSymbol.values()[symbolIndex]);
            }
        }

        private int echoSymbolAt(int x) {
            int[] centers = {192, 336, 480, 624, 768};
            for (int i = 0; i < centers.length; i++) {
                if (x >= centers[i] - 57 && x <= centers[i] + 57) return i;
            }
            return -1;
        }

        private int[] toVirtual(int mouseX, int mouseY) {
            double scale = Math.min(getWidth() / 320.0, getHeight() / 180.0);
            double ox = (getWidth() - 320 * scale) / 2.0;
            double oy = (getHeight() - 180 * scale) / 2.0;
            int vx = (int) Math.round((mouseX - ox) / scale * W / 320.0);
            int vy = (int) Math.round((mouseY - oy) / scale * H / 180.0);
            return new int[] {vx, vy};
        }

        void startClock() {
            new Timer(35, (ActionEvent e) -> {
                tick++;
                syncDialog();
                if (isDialogActive() && !dialogDismissed && visibleDialogChars < lastDialogText.length()) {
                    visibleDialogChars = Math.min(lastDialogText.length(), visibleDialogChars + 2);
                }
                if (phase != Phase.SELECT && phase != Phase.COMMAND && phase != Phase.MOVE_SELECT
                        && phase != Phase.ATTUNE_INPUT && phase != Phase.RESULT) {
                    frameNo++;
                    advanceAnimation();
                }
                repaint();
            }).start();
        }

        private void advanceAnimation() {
            if (phase == Phase.ATTUNE_REVEAL && frameNo >= 45) {
                phase = Phase.ATTUNE_INPUT;
                frameNo = 0;
                menuFocus = 0;
            } else if (phase == Phase.ACTION_NOTICE && dialogCanContinue()) {
                phase = Phase.COMMAND;
                frameNo = 0;
                setActionsEnabled(true);
            } else if (phase == Phase.SUMMONING && frameNo > 32 && dialogCanContinue()) {
                phase = Phase.COMMAND; frameNo = 0; message = player.name + " is ready! Choose Fight, Bag, Run, or Capture.";
                setActionsEnabled(true);
            } else if (phase == Phase.ANIMATING) {
                if (capturing && frameNo >= 28 && dialogCanContinue()) {
                    capturing = false;
                    if (captureSuccess) {
                        phase = Phase.RECALL_ENEMY; frameNo = 0; afterRecall = Phase.RESULT;
                        message = enemy.name + " joined your capsules!";
                    } else {
                        phase = Phase.ANIMATING; frameNo = 0;
                        enemyTurnPendingAfterCapture = true;
                        captureBreakEffect = true;
                        message = enemy.name + " broke free! The capsule did not hold.";
                    }
                } else if (!capturing && frameNo == 8) {
                    if (effectActor == enemy) applyEnemyMove();
                    else if (move != null) applyMove();
                } else if (!capturing && move != null && frameNo >= 20 && dialogCanContinue()) {
                    if (effectActor == enemy) finishEnemyTurn();
                    else {
                        Move completedMove = move;
                        move = null;
                        if (enemy.hp <= 0) {
                            enemy.hp = 0; phase = Phase.RECALL_ENEMY; frameNo = 0; afterRecall = Phase.RESULT;
                            message = enemy.name + " fainted and returned to its capsule.";
                        } else {
                            if (player.buffTurns > 0 && completedMove.kind != Kind.BUFF) player.buffTurns--;
                            enemyTurn();
                        }
                    }
                } else if (!capturing && move == null && frameNo >= 18 && dialogCanContinue()) {
                    boolean enemyActsAfterEffect = effectActor == player || enemyTurnPendingAfterCapture;
                    enemyTurnPendingAfterCapture = false;
                    attunementEffect = false;
                    captureBreakEffect = false;
                    phase = Phase.COMMAND; frameNo = 0; effectKind = null; effectActor = null;
                    if (enemyActsAfterEffect) enemyTurn();
                }
            } else if (phase == Phase.RECALL_PLAYER && frameNo >= 30 && dialogCanContinue()) {
                Phase destination = afterRecall;
                if (player != null && player.hp <= 0) player.hp = 100;
                returnToCapsules(destination == Phase.SELECT ? message : "Battle ended.");
            } else if (phase == Phase.RECALL_ENEMY && frameNo >= 30 && dialogCanContinue()) {
                phase = Phase.RESULT; frameNo = 0;
                if (captureSuccess) {
                    message = enemy.name + " was captured and returned to your capsules.";
                    captureSuccess = false;
                } else {
                    message = enemy.name + " returned to its capsule. You can choose another encounter.";
                }
                ((java.awt.CardLayout) cards.getLayout()).show(cards, "select");
                cards.setVisible(true);
                frame.revalidate();
                setActionsEnabled(false);
                hint.setText(message);
            } else if (phase == Phase.RESULT && frameNo > 3) {
                // Result stays visible until the next capsule is opened.
            }
        }

        private void applyMove() {
            if (capturing || move == null) return;
            if (move.kind == Kind.DAMAGE) {
                int damage = 13 + random.nextInt(8);
                if (player.buffTurns > 0) damage += 4;
                if (enemy.weakenedTurns > 0) damage = (int) Math.round(damage * 1.3);
                enemy.hp = Math.max(0, enemy.hp - damage);
                if (enemy.weakenedTurns > 0) enemy.weakenedTurns--;
                arenaMessage(move.name + " hit for " + damage + " damage!");
            } else if (move.kind == Kind.BUFF) {
                player.buffTurns = 3;
                arenaMessage(player.name + " powered up. Its attacks are stronger for 3 turns!");
            } else {
                enemy.weakenedTurns = 3;
                arenaMessage(enemy.name + "'s guard fell for 3 turns!");
            }
        }

        private void enemyTurn() {
            if (player.hp <= 0) {
                player.hp = 0; phase = Phase.RECALL_PLAYER; frameNo = 0; afterRecall = Phase.SELECT;
                message = player.name + " fainted and returned to its capsule in motes of light.";
                return;
            }
            Move counter = pendingEnemyMove != null
                ? pendingEnemyMove
                : enemy.moves.get(random.nextInt(enemy.moves.size()));
            pendingEnemyMove = null;
            move = counter;
            effectKind = Kind.DAMAGE; effectActor = enemy;
            phase = Phase.ANIMATING; frameNo = 0;
            message = enemy.name + " is preparing " + counter.name + "!";
        }

        private void applyEnemyMove() {
            if (move == null) return;
            if (move.kind == Kind.DAMAGE) {
                int damage = 8 + random.nextInt(8);
                if (enemy.buffTurns > 0) damage += 4;
                if (player.weakenedTurns > 0) damage += 4;
                player.hp = Math.max(0, player.hp - damage);
                message = enemy.name + " used " + move.name + " for " + damage + " damage!";
            } else if (move.kind == Kind.BUFF) {
                enemy.buffTurns = 3;
                message = enemy.name + " used " + move.name + " to strengthen its next attacks!";
            } else {
                player.weakenedTurns = 3;
                message = enemy.name + " used " + move.name + ". Your guard is weakened for 3 turns.";
            }
        }

        private void finishEnemyTurn() {
            Move completedMove = move;
            if (enemy.buffTurns > 0 && completedMove != null && completedMove.kind != Kind.BUFF) enemy.buffTurns--;
            if (player.weakenedTurns > 0 && completedMove != null && completedMove.kind != Kind.DEBUFF) player.weakenedTurns--;
            move = null; effectActor = null; effectKind = null;
            if (player.hp <= 0) {
                player.hp = 0; phase = Phase.RECALL_PLAYER; frameNo = 0; afterRecall = Phase.SELECT;
                message = player.name + " fainted and returned to its capsule in motes of light.";
            } else {
                phase = Phase.COMMAND; frameNo = 0;
                menuFocus = 0;
                setActionsEnabled(true);
            }
        }

        private void arenaMessage(String text) { message = text; }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            BufferedImage pixelFrame = new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB);
            Graphics2D scene = pixelFrame.createGraphics();
            scene.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            scene.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            scene.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            scene.scale(320.0 / W, 180.0 / H);
            drawBackdrop(scene);
            if (phase == Phase.SELECT) drawSelectionScene(scene);
            else drawBattle(scene);
            scene.dispose();

            Graphics2D screen = (Graphics2D) graphics.create();
            double scale = Math.min(getWidth() / 320.0, getHeight() / 180.0);
            int drawW = (int) Math.round(320 * scale), drawH = (int) Math.round(180 * scale);
            int ox = (getWidth() - drawW) / 2, oy = (getHeight() - drawH) / 2;
            screen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            screen.drawImage(pixelFrame, ox, oy, drawW, drawH, null);
            drawReadableOverlayText(screen, ox, oy, scale);
            screen.dispose();
        }

        private void drawReadableOverlayText(Graphics2D target, int ox, int oy, double scale) {
            Graphics2D g = (Graphics2D) target.create();
            g.translate(ox, oy);
            g.scale(scale / 3.0, scale / 3.0);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setColor(INK);
            if (phase == Phase.SELECT) {
                drawSelectionOverlay(g);
            } else {
                drawBattleOverlay(g);
            }
            g.dispose();
        }

        private void drawSelectionOverlay(Graphics2D g) {
            Beast beast = (Beast) playerChoice.getSelectedItem();
            if (beast == null) beast = preview;
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16)); g.setColor(MUTED);
            g.drawString("CAPSULE SELECTION", 82, 72);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 25)); g.setColor(INK);
            g.drawString("Choose your companion", 82, 107);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
            centered(g, beast.name, 601, 405);
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16)); g.setColor(MUTED);
            centered(g, beast.types(), 601, 428);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16)); g.setColor(new Color(36, 121, 102));
            centered(g, "Choose one of 27 capsules below", 337, 414);
        }

        private void drawBattleOverlay(Graphics2D g) {
            if (player != null && enemy != null) {
                drawStatusText(g, player, 102, 27, false);
                drawStatusText(g, enemy, 666, 27, true);
                if (player.buffTurns > 0 && phase != Phase.RECALL_PLAYER) drawStatusSigilText(g, 312, 300, "POWER UP");
                if (enemy.weakenedTurns > 0 && phase != Phase.RECALL_ENEMY) drawStatusSigilText(g, 684, 298, "GUARD DOWN");
            }
            if (phase == Phase.COMMAND) drawCommandText(g);
            else if (phase == Phase.MOVE_SELECT) drawMoveText(g);
            else if (phase == Phase.ATTUNE_REVEAL || phase == Phase.ATTUNE_INPUT) drawAttunementText(g);
            else if (phase == Phase.SUMMONING) {
                g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 17));
                centered(g, "CAPSULE OPENING", 477, 169);
            } else if (phase == Phase.RESULT) {
                g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
                centered(g, "ENCOUNTER COMPLETE", 480, 252);
                g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16)); g.setColor(MUTED);
                centered(g, "Choose another capsule to continue.", 480, 278);
            }
            if (isDialogActive() && !dialogDismissed) drawDialogText(g);
        }

        private void drawStatusText(Graphics2D g, Beast beast, int x, int y, boolean right) {
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18)); g.setColor(INK);
            String name = beast.name;
            if (g.getFontMetrics().stringWidth(name) > 163) {
                g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
            }
            if (right) rightText(g, name, x + 180, y + 29); else g.drawString(name, x + 18, y + 29);
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13)); g.setColor(MUTED);
            if (right) rightText(g, beast.types().toUpperCase(Locale.ROOT), x + 180, y + 48);
            else g.drawString(beast.types().toUpperCase(Locale.ROOT), x + 18, y + 48);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14)); g.setColor(INK);
            String hp = beast.hp + "/100 HP";
            if (right) rightText(g, hp, x + 180, y + 82); else g.drawString(hp, x + 18, y + 82);
        }

        private void drawStatusSigilText(Graphics2D g, int x, int y, String label) {
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
            g.setColor(new Color(27, 37, 53)); centered(g, label, x, y + 4);
        }

        private void drawCommandText(Graphics2D g) {
            String attuneLabel = trainerBattle ? "LOCKED" : attunementBonus ? "ATTUNED" : enemy.hp > CAPTURE_WEAK_HP ? "WEAKEN" : "ATTUNE";
            String captureLabel = trainerBattle ? "LOCKED" : enemy.hp > CAPTURE_WEAK_HP ? "WEAKEN" : "CAPTURE";
            String[] labels = {"FIGHT", "BAG", attuneLabel, "LISTEN", "RUN", captureLabel};
            int[] xs = {49, 182, 317, 49, 182, 317};
            int[] ys = {443, 443, 443, 491, 491, 491};
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 19));
            for (int i = 0; i < labels.length; i++) {
                boolean locked = (i == 2 && (trainerBattle || attunementBonus || enemy.hp > CAPTURE_WEAK_HP))
                    || (i == 5 && (trainerBattle || enemy.hp > CAPTURE_WEAK_HP));
                g.setColor(locked ? new Color(137, 143, 131) : INK);
                g.drawString(labels[i], xs[i], ys[i]);
            }
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
            if (pendingEnemyMove != null) {
                g.setColor(new Color(63, 91, 134));
                centered(g, "ECHO HEARD: NEXT MOVE IS " + moveCategory(pendingEnemyMove.kind), 666, 377);
            } else if (!trainerBattle && attunementBonus) {
                g.setColor(new Color(108, 73, 145));
                centered(g, "ECHO ATTUNED  ·  CAPSULE BONUS READY", 666, 377);
            } else if (!trainerBattle && enemy.hp > CAPTURE_WEAK_HP) {
                g.setColor(MUTED);
                centered(g, "WEAKEN THE WILD BEAST TO 65 HP TO ATTUNE OR CAPTURE", 666, 377);
            }
        }

        private void drawAttunementText(Graphics2D g) {
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 21));
            g.setColor(INK);
            centered(g, "ECHO ATTUNEMENT", 480, 360);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
            centered(g, enemy.name.toUpperCase(Locale.ROOT), 480, 382);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
            g.setColor(MUTED);
            if (phase == Phase.ATTUNE_REVEAL) {
                centered(g, "WATCH THE THREE-BEAT PATTERN  ·  BEAT " + (Math.min(2, frameNo / 15) + 1) + " OF 3", 480, 404);
            } else {
                centered(g, "ENTER THE SYMBOLS IN ORDER  ·  BEAT " + (enteredEcho.size() + 1) + " OF 3", 480, 405);
                g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
                g.setColor(MUTED);
                centered(g, "CLICK A SYMBOL OR PRESS 1-5 TO REPEAT THE ECHO", 480, 526);
            }
        }

        private void drawMoveText(Graphics2D g) {
            g.setColor(new Color(44, 58, 43)); g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
            g.drawString("CHOOSE A MOVE", 39, 379);
            if (player == null) return;
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
            for (int i = 0; i < player.moves.size(); i++) {
                Move option = player.moves.get(i);
                int y = 404 + i * 23;
                g.setColor(INK);
                String label = (i + 1) + "  " + option.name + "  /  " + option.type.toUpperCase(Locale.ROOT);
                g.drawString(ellipsize(g, label, 359), 61, y);
            }
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13)); g.setColor(MUTED);
            g.drawString("BACK", 47, 519);
        }

        private void drawDialogText(Graphics2D g) {
            syncDialog();
            String text = lastDialogText.substring(0, Math.min(visibleDialogChars, lastDialogText.length()));
            List<String> lines = wrapText(g, text, 848);
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18));
            g.setColor(new Color(27, 37, 53));
            int[] baselines = {481, 505};
            for (int i = 0; i < Math.min(2, lines.size()); i++) g.drawString(lines.get(i), 42, baselines[i]);
            if (visibleDialogChars >= lastDialogText.length()) {
                g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
                rightText(g, "SPACE / CLICK", 928, 469);
                g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
                centered(g, "▼", 918, 516);
            }
        }

        private List<String> wrapText(Graphics2D g, String text, int maxWidth) {
            List<String> lines = new ArrayList<>();
            if (text.isEmpty()) return lines;
            Font saved = g.getFont();
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18));
            StringBuilder line = new StringBuilder();
            for (String word : text.split(" ", -1)) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && g.getFontMetrics().stringWidth(candidate) > maxWidth) {
                    lines.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                } else {
                    if (line.length() > 0) line.append(' ');
                    line.append(word);
                }
            }
            if (line.length() > 0) lines.add(line.toString());
            g.setFont(saved);
            if (lines.size() > 2) {
                lines = new ArrayList<>(lines.subList(0, 2));
                String last = lines.get(1);
                g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18));
                while (!last.isEmpty() && g.getFontMetrics().stringWidth(last + "...") > maxWidth) last = last.substring(0, last.length() - 1);
                lines.set(1, last + "...");
                g.setFont(saved);
            }
            return lines;
        }

        private void drawBackdrop(Graphics2D g) {
            g.setColor(new Color(54, 142, 56)); g.fillRect(0, 0, W, H);
            g.setColor(new Color(20, 81, 39)); g.fillRect(0, 0, W, 164);
            Random plants = new Random(12);
            for (int x = -12; x < W + 24; x += 32) {
                int top = 8 + plants.nextInt(36);
                g.setColor(new Color(91, 58, 33)); g.fillRect(x + 9, top + 66, 13, 86);
                g.setColor(new Color(20, 94, 38)); g.fillRect(x - 9, top + 27, 48, 58);
                g.setColor(new Color(38, 137, 43)); g.fillRect(x - 5, top + 8, 42, 54);
                for (int j = 0; j < 16; j++) {
                    int lx = x - 5 + plants.nextInt(44), ly = top + 10 + plants.nextInt(58);
                    g.setColor(plants.nextBoolean() ? new Color(71, 171, 46) : new Color(31, 114, 40));
                    g.fillRect(lx, ly, 9, 8);
                }
                g.setColor(new Color(120, 183, 55));
                g.fillRect(x + 1, top + 18, 11, 6); g.fillRect(x + 23, top + 45, 9, 6);
            }
            g.setColor(new Color(115, 188, 59)); g.fillRect(0, 143, W, 30);
            g.setColor(new Color(75, 159, 48)); g.fillRect(0, 157, W, H - 157);
            for (int i = 0; i < 210; i++) {
                int x = (i * 173 + 23) % W, y = 153 + (i * 97) % 380;
                g.setColor(i % 3 == 0 ? new Color(138, 199, 62) : i % 3 == 1 ? new Color(44, 129, 47) : new Color(97, 177, 53));
                g.fillRect(x, y, 8 + i % 3 * 3, 5 + i % 2 * 3);
            }
            int[] shadowX = {270, 688, 744, 853, 791, 842, 766, 806, 680, 615, 490, 386, 268, 185, 238, 99, 194};
            int[] shadowY = {168, 168, 198, 204, 245, 278, 285, 322, 330, 370, 368, 324, 326, 291, 253, 238, 200};
            g.setColor(new Color(27, 96, 43)); g.fillPolygon(shadowX, shadowY, shadowX.length);
            int[] floorX = {270, 688, 741, 841, 785, 832, 759, 799, 675, 612, 492, 390, 270, 191, 242, 108, 199};
            int[] floorY = {158, 158, 188, 194, 235, 268, 275, 312, 320, 360, 358, 314, 316, 281, 243, 228, 190};
            g.setColor(new Color(219, 224, 199)); g.fillPolygon(floorX, floorY, floorX.length);
            g.setColor(new Color(239, 239, 213));
            g.fillRect(291, 178, 348, 21); g.fillRect(226, 209, 509, 20); g.fillRect(259, 241, 435, 18);
            g.fillRect(325, 271, 311, 18); g.fillRect(385, 300, 191, 16);
            g.setColor(new Color(177, 192, 160));
            g.fillRect(322, 201, 57, 8); g.fillRect(574, 231, 57, 8); g.fillRect(243, 260, 53, 8);
            g.fillRect(658, 278, 55, 8); g.fillRect(454, 322, 65, 8);
            drawPixelBush(g, -18, 396, new Color(31, 103, 34));
            drawPixelBush(g, 818, 389, new Color(35, 111, 38));
            drawPixelBush(g, 36, 459, new Color(21, 90, 33));
            drawPixelBush(g, 857, 450, new Color(24, 96, 36));
        }

        private void drawPixelBush(Graphics2D g, int x, int y, Color dark) {
            g.setColor(dark); g.fillRect(x, y, 154, 110);
            g.setColor(new Color(41, 135, 39)); g.fillRect(x + 13, y - 20, 119, 88);
            g.setColor(new Color(71, 171, 44)); g.fillRect(x + 31, y - 29, 77, 78);
            g.setColor(new Color(108, 196, 49));
            g.fillRect(x + 36, y - 22, 23, 13); g.fillRect(x + 79, y - 16, 18, 11); g.fillRect(x + 16, y + 20, 15, 12);
        }

        private void drawSelectionScene(Graphics2D g) {
            g.setColor(new Color(24, 38, 53, 20)); g.fillRoundRect(45, 36, 870, 430, 34, 34);
            g.setColor(new Color(255, 255, 255, 207)); g.fillRoundRect(50, 32, 860, 426, 32, 32);
            g.setColor(new Color(214, 225, 232)); g.setStroke(new BasicStroke(2)); g.drawRoundRect(50, 32, 860, 426, 32, 32);
            Beast b = (Beast) playerChoice.getSelectedItem(); if (b == null) b = preview;
            drawCapsule(g, 286, 270, 1.0, 0, 0, false);
            g.setColor(new Color(29, 45, 62, 32)); g.fillOval(515, 359, 170, 28);
            drawSprite(g, b, 526, 173, 155, 178, 1, 0, 0, 1f, false);
            drawPixelSparkles(g, 730, 190, new Color(255, 215, 104), 13);
        }

        private void drawBattle(Graphics2D g) {
            if (phase == Phase.RESULT) {
                drawBattlefield(g, false);
                if (!dialogDismissed) drawMessage(g);
                drawPixelPanel(g, 274, 208, 412, 104);
                return;
            }
            drawBattlefield(g, true);
            if (phase == Phase.SUMMONING) drawSummoning(g);
            else if (phase == Phase.ANIMATING) drawMoveEffect(g);
            else if (phase == Phase.RECALL_PLAYER) drawRecall(g, false);
            else if (phase == Phase.RECALL_ENEMY) drawRecall(g, true);
            else if (phase == Phase.COMMAND) drawCommandMenu(g);
            else if (phase == Phase.MOVE_SELECT) drawMoveMenu(g);
            else if (phase == Phase.ATTUNE_REVEAL || phase == Phase.ATTUNE_INPUT) drawAttunementScene(g);
            if (phase != Phase.COMMAND && phase != Phase.MOVE_SELECT && phase != Phase.ATTUNE_REVEAL
                    && phase != Phase.ATTUNE_INPUT && !dialogDismissed) drawMessage(g);
        }

        private void drawBattlefield(Graphics2D g, boolean sprites) {
            if (player == null || enemy == null) return;
            int shake = phase == Phase.ANIMATING && frameNo > 6 && frameNo < 12 ? (frameNo % 2 == 0 ? 5 : -5) : 0;
            g.translate(shake, 0);
            drawStatus(g, player, 102, 27, false);
            drawStatus(g, enemy, 666, 27, true);
            drawBackTrainer(g, 70, 212, new Color(39, 77, 176), new Color(225, 112, 52));
            if (trainerBattle) drawBackTrainer(g, 850, 207, new Color(176, 54, 56), new Color(60, 72, 121));
            else drawWildMark(g, 862, 218);
            g.setColor(new Color(35, 91, 42)); g.fillRect(235, 324, 150, 17); g.fillRect(584, 320, 156, 17);
            g.setColor(new Color(125, 175, 69)); g.fillRect(245, 322, 132, 7); g.fillRect(596, 318, 134, 7);
            if (sprites && phase != Phase.RECALL_PLAYER) drawSprite(g, player, playerX(), 205, 108, 120, playerScaleX(), 0, 0, playerAlpha(), false);
            if (sprites && phase != Phase.RECALL_ENEMY && !capturing) drawSprite(g, enemy, enemyX(), 197, 108, 120, enemyScaleX(), 0, 0, enemyAlpha(), true);
            if (player.buffTurns > 0 && phase != Phase.RECALL_PLAYER) drawStatusSigil(g, 312, 300, new Color(73, 199, 150), "POWER UP");
            if (enemy.weakenedTurns > 0 && phase != Phase.RECALL_ENEMY) drawStatusSigil(g, 684, 298, new Color(133, 94, 159), "GUARD DOWN");
            g.translate(-shake, 0);
        }

        private void drawBackTrainer(Graphics2D g, int x, int y, Color shirt, Color pack) {
            int p = 5;
            Color outline = new Color(25, 38, 34);
            g.setColor(outline); g.fillRect(x + p, y, 5 * p, 2 * p); g.fillRect(x, y + p, 7 * p, 5 * p);
            g.setColor(new Color(39, 31, 27)); g.fillRect(x + p, y + p, 5 * p, 4 * p);
            g.setColor(new Color(239, 179, 126)); g.fillRect(x + 2 * p, y + 3 * p, 3 * p, 3 * p);
            g.setColor(shirt); g.fillRect(x + p, y + 6 * p, 5 * p, 5 * p);
            g.setColor(pack); g.fillRect(x + 4 * p, y + 6 * p, 3 * p, 4 * p);
            g.setColor(outline); g.fillRect(x, y + 7 * p, p, 4 * p); g.fillRect(x + 6 * p, y + 7 * p, p, 4 * p);
            g.setColor(new Color(42, 54, 79)); g.fillRect(x + p, y + 11 * p, 2 * p, 4 * p); g.fillRect(x + 4 * p, y + 11 * p, 2 * p, 4 * p);
            g.setColor(new Color(230, 225, 196)); g.fillRect(x + p, y + 15 * p, 2 * p, p); g.fillRect(x + 4 * p, y + 15 * p, 2 * p, p);
        }

        private void drawWildMark(Graphics2D g, int x, int y) {
            g.setColor(new Color(236, 231, 191)); g.fillRect(x, y, 35, 4); g.fillRect(x + 5, y - 7, 25, 4);
            g.setColor(new Color(33, 88, 38)); g.fillRect(x + 9, y - 12, 17, 5);
        }

        private int playerX() {
            int x = 259;
            if (phase == Phase.ANIMATING && move != null && effectActor != enemy && frameNo < 9) x += frameNo * 8;
            if (phase == Phase.RECALL_PLAYER) x = (int) (259 + frameNo * 1.1);
            return x;
        }
        private int enemyX() {
            int x = 630;
            if (phase == Phase.ANIMATING && effectActor == enemy && frameNo < 9) x -= frameNo * 5;
            if (phase == Phase.RECALL_ENEMY) x = (int) (630 - frameNo * 0.7);
            if (capturing && phase == Phase.ANIMATING && frameNo > 13) x = 630 + (frameNo - 13) * 8;
            return x;
        }
        private double playerScaleX() {
            if (phase == Phase.RECALL_PLAYER) return Math.max(0.03, 1 - frameNo / 30.0);
            if (phase == Phase.ANIMATING && effectActor == enemy && frameNo > 6 && frameNo < 15) return frameNo % 2 == 0 ? 0.78 : 1.0;
            return 1;
        }
        private double enemyScaleX() {
            if (phase == Phase.RECALL_ENEMY) return Math.max(0.03, 1 - frameNo / 30.0);
            if (capturing && phase == Phase.ANIMATING && frameNo > 13) return Math.max(0.06, 1 - (frameNo - 13) / 16.0);
            if (phase == Phase.ANIMATING && effectActor != enemy && frameNo > 6 && frameNo < 15) return frameNo % 2 == 0 ? 0.78 : 1.0;
            return 1;
        }
        private float playerAlpha() { return phase == Phase.RECALL_PLAYER ? Math.max(0f, 1f - frameNo / 29f) : 1f; }
        private float enemyAlpha() { return phase == Phase.RECALL_ENEMY ? Math.max(0f, 1f - frameNo / 29f) : capturing && phase == Phase.ANIMATING && frameNo > 15 ? Math.max(0f, 1f - (frameNo - 15) / 15f) : 1f; }

        private void drawStatus(Graphics2D g, Beast beast, int x, int y, boolean right) {
            g.setColor(new Color(24, 31, 24)); g.fillRect(x, y, 195, 91);
            g.setColor(new Color(248, 248, 232)); g.fillRect(x + 6, y + 6, 183, 79);
            g.setColor(new Color(66, 75, 57)); g.fillRect(x + 10, y + 10, 175, 3);
            int bx = x + 49, by = y + 59, bw = 126;
            g.setColor(new Color(39, 47, 36)); g.drawRect(bx - 2, by - 2, bw + 4, 14);
            g.setColor(new Color(200, 213, 177)); g.fillRect(bx, by, bw, 10);
            g.setColor(beast.hp > 45 ? new Color(56, 163, 51) : beast.hp > 20 ? new Color(226, 173, 48) : new Color(207, 68, 48));
            g.fillRect(bx, by, Math.max(0, bw * beast.hp / 100), 10);
        }

        private void drawCommandMenu(Graphics2D g) {
            drawPixelPanel(g, 20, 396, 410, 130);
            int[] xs = {49, 182, 317, 49, 182, 317};
            int[] ys = {443, 443, 443, 491, 491, 491};
            for (int i = 0; i < xs.length; i++) {
                if (menuFocus == i) drawMenuArrow(g, xs[i] - 17, ys[i] - 7);
            }
        }

        private void drawAttunementScene(Graphics2D g) {
            drawPixelPanel(g, 132, 335, 696, 195);
            if (phase == Phase.ATTUNE_REVEAL) {
                int activeBeat = Math.min(2, frameNo / 15);
                int[] centers = {350, 480, 610};
                for (int i = 0; i <= activeBeat; i++) {
                    EchoSymbol symbol = enemy.echoPattern[i];
                    drawEchoSymbol(g, symbol, centers[i], 440, 46, i == activeBeat ? 255 : 110);
                }
            } else {
                int[] centers = {192, 336, 480, 624, 768};
                for (int i = 0; i < centers.length; i++) {
                    EchoSymbol symbol = EchoSymbol.values()[i];
                    int x = centers[i];
                    boolean focused = menuFocus == i;
                    g.setColor(focused ? new Color(230, 241, 228) : new Color(239, 241, 229));
                    g.fillRect(x - 57, 402, 114, 94);
                    g.setColor(focused ? new Color(36, 121, 102) : new Color(126, 137, 118));
                    g.drawRect(x - 57, 402, 114, 94);
                    drawEchoSymbol(g, symbol, x, 442, 36);
                    g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
                    g.setColor(INK);
                    centered(g, (i + 1) + "  " + symbol.label, x, 481);
                }
            }
        }

        private void drawEchoSymbol(Graphics2D g, EchoSymbol symbol, int centerX, int centerY, int size) {
            drawEchoSymbol(g, symbol, centerX, centerY, size, 255);
        }

        private void drawEchoSymbol(Graphics2D g, EchoSymbol symbol, int centerX, int centerY, int size, int alpha) {
            Graphics2D icon = (Graphics2D) g.create();
            icon.setComposite(java.awt.AlphaComposite.getInstance(
                java.awt.AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha / 255f))));
            icon.translate(centerX, centerY);
            icon.setColor(symbol.color);
            icon.setStroke(new BasicStroke(Math.max(3, size / 9f), BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            int half = size / 2;
            switch (symbol) {
                case WAVE -> {
                    int[] xs = {-half, -half / 2, 0, half / 2, half};
                    int[] ys = {0, -half / 2, 0, half / 2, 0};
                    icon.drawPolyline(xs, ys, xs.length);
                    icon.drawLine(-half, half / 3, half, half / 3);
                }
                case SPARK -> icon.fillPolygon(
                    new int[] {-half / 4, 0, half / 4, 0, half / 2, -half / 8, 0, -half / 3},
                    new int[] {-half, -half / 4, -half / 2, -half / 8, half, half / 4, half / 3, half / 2}, 8);
                case LEAF -> {
                    icon.fillOval(-half / 2, -half, size, size);
                    icon.setColor(new Color(238, 248, 222));
                    icon.setStroke(new BasicStroke(Math.max(2, size / 12f)));
                    icon.drawLine(-half / 3, half / 3, half / 3, -half / 3);
                }
                case STAR -> icon.fillPolygon(
                    new int[] {0, half / 4, half, half / 3, half / 3, 0, -half / 3, -half / 3, -half, -half / 4},
                    new int[] {-half, -half / 4, -half / 4, 0, half, half / 3, half, 0, -half / 4, -half / 4}, 10);
                case MOON -> {
                    icon.fillOval(-half, -half, size, size);
                    icon.setColor(new Color(252, 251, 236));
                    icon.fillOval(-half / 5, -half - 2, size, size);
                }
            }
            icon.dispose();
        }

        private void drawMoveMenu(Graphics2D g) {
            drawPixelPanel(g, 20, 354, 410, 172);
            for (int i = 0; i < player.moves.size(); i++) {
                int y = 404 + i * 23;
                if (menuFocus == i) drawMenuArrow(g, 29, y - 10);
                g.setColor(typeColor(player.moves.get(i).type)); g.fillRect(47, y - 11, 7, 7);
            }
            if (menuFocus == 5) drawMenuArrow(g, 29, 508);
        }

        private void drawPixelPanel(Graphics2D g, int x, int y, int w, int h) {
            g.setColor(new Color(22, 28, 23)); g.fillRect(x, y, w, h);
            g.setColor(new Color(252, 251, 236)); g.fillRect(x + 7, y + 7, w - 14, h - 14);
            g.setColor(new Color(74, 82, 65));
            g.fillRect(x + 11, y + 11, w - 22, 3);
            g.fillRect(x + 11, y + h - 14, w - 22, 3);
        }

        private void drawMenuArrow(Graphics2D g, int x, int y) {
            g.setColor(new Color(22, 28, 23));
            g.fillPolygon(new int[] {x, x + 13, x}, new int[] {y, y + 7, y + 14}, 3);
        }

        private void drawSummoning(Graphics2D g) {
            int f = frameNo;
            int cy = Math.min(18, f / 2);
            drawCapsule(g, 313, 264, Math.max(.65, 1 - f / 125.0), f < 7 ? f : 7, cy, true);
            if (f >= 6) {
                drawPixelSparkles(g, 368, 250, new Color(250, 221, 126), 12 + f / 2);
                float alpha = Math.min(1f, (f - 5) / 17f);
                double size = Math.min(1, (f - 5) / 17.0);
                drawSprite(g, player, (int) (259 + (1 - size) * 40), (int) (205 + (1 - size) * 35), (int) (108 * size), (int) (120 * size), 1, 0, 0, alpha, false);
            }
            g.setColor(new Color(255, 255, 255, 210)); g.fillRoundRect(357, 139, 240, 47, 20, 20);
        }

        private void drawMoveEffect(Graphics2D g) {
            if (capturing) {
                if (frameNo < 15) drawCapsule(g, 170 + frameNo * 28, 245 - (int) (Math.sin(frameNo / 2.0) * 28), 0.65, 0, 0, false);
                else { drawCapsule(g, 585, 266, Math.max(.18, .75 - (frameNo - 15) / 45.0), frameNo % 8 < 4 ? 2 : 0, 0, false); drawPixelSparkles(g, 680, 266, new Color(255, 225, 130), 10); }
                return;
            }
            if (move == null) {
                if (attunementEffect) {
                    drawAttunementEffect(g, frameNo);
                    return;
                }
                if (captureBreakEffect) {
                    drawCaptureBreakEffect(g, frameNo);
                    return;
                }
                if (effectKind == Kind.BUFF) drawBuffEffect(g, effectActor == enemy, frameNo);
                return;
            }
            if (move.kind == Kind.BUFF) { drawBuffEffect(g, effectActor == enemy, frameNo); return; }
            if (move.kind == Kind.DEBUFF) { drawDebuffEffect(g, frameNo); return; }
            if (frameNo >= 2 && frameNo <= 10) {
                double t = (frameNo - 2) / 8.0;
                boolean enemyAttacks = effectActor == enemy;
                int x = enemyAttacks ? 644 - (int) (t * 331) : 353 + (int) (t * 330);
                int y = 265 + (int) (Math.sin(t * Math.PI * 3) * 26);
                drawProjectile(g, move.type, x, y, frameNo);
                drawTrail(g, move.type, x + (enemyAttacks ? 29 : -29), y, frameNo);
            } else if (frameNo >= 8 && frameNo <= 17) {
                int p = frameNo - 8;
                Color col = typeColor(move.type);
                int hitX = effectActor == enemy ? 313 : 684;
                drawBurst(g, hitX, 278, col, p);
                if (p < 3) { g.setColor(new Color(255, 255, 245, (3 - p) * 55)); g.fillOval(hitX - 72, 203, 144, 144); }
            }
        }

        private void drawAttunementEffect(Graphics2D g, int frame) {
            int cx = 685, cy = 270;
            Color color = attunementBonus ? new Color(73, 184, 133) : new Color(184, 104, 100);
            int radius = 24 + frame * 4;
            int alpha = Math.max(18, 170 - frame * 7);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g.setStroke(new BasicStroke(4));
            g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
            for (int i = 0; i < 3; i++) {
                EchoSymbol symbol = enemy.echoPattern[i];
                double angle = tick * 0.07 + i * (Math.PI * 2 / 3);
                int x = cx + (int) (Math.cos(angle) * (44 + frame * 2));
                int y = cy + (int) (Math.sin(angle) * (28 + frame));
                drawEchoSymbol(g, symbol, x, y, 15 + (i == frame / 6 % 3 ? 8 : 0), Math.max(25, 215 - frame * 8));
            }
            if (attunementBonus) drawPixelSparkles(g, cx, cy, color, Math.max(3, 18 - frame / 2));
        }

        private void drawCaptureBreakEffect(Graphics2D g, int frame) {
            int cx = 650, cy = 269;
            Color color = new Color(246, 196, 113);
            for (int i = 0; i < 12; i++) {
                double angle = i * (Math.PI / 6.0);
                int distance = 12 + frame * (2 + i % 3);
                int x = cx + (int) (Math.cos(angle) * distance);
                int y = cy + (int) (Math.sin(angle) * distance);
                g.setColor((i + frame / 2) % 3 == 0 ? Color.WHITE : color);
                g.fillRect(x, y, Math.max(2, 7 - frame / 5), Math.max(2, 7 - frame / 5));
            }
            if (frame < 8) drawCapsule(g, cx, cy, .35, frame % 2 == 0 ? 3 : 0, 0, false);
        }

        private void drawBuffEffect(Graphics2D g, boolean enemyBuff, int frame) {
            int x = enemyBuff ? 684 : 313, y = 280;
            Color col = new Color(86, 214, 157);
            for (int i = 0; i < 7; i++) {
                int rise = (frame * 5 + i * 23) % 95;
                g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), Math.max(25, 190 - rise)));
                int px = x + (int) (Math.sin((frame + i * 11) / 9.0) * (25 + i * 2));
                drawPixelStar(g, px, y - rise, 9 + (i % 3) * 3, col);
            }
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 70));
            g.setStroke(new BasicStroke(3)); g.drawOval(x - 61, y - 19, 122, 38);
        }

        private void drawDebuffEffect(Graphics2D g, int frame) {
            int x = 699, y = 271;
            Color col = new Color(136, 86, 163);
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 62));
            g.setStroke(new BasicStroke(6)); g.drawOval(x - 67 - frame, y - 38 - frame / 2, 134 + frame * 2, 76 + frame);
            g.setStroke(new BasicStroke(2)); g.drawOval(x - 48 + frame / 2, y - 26, 96 - frame, 52);
            for (int i = 0; i < 7; i++) {
                double a = (tick * .16 + i * Math.PI / 3.5);
                int px = x + (int) (Math.cos(a) * 67), py = y + (int) (Math.sin(a) * 37);
                g.setColor(col); g.fillRect(px - 3, py - 3, 7, 7);
            }
        }

        private void drawRecall(Graphics2D g, boolean enemySide) {
            int x = enemySide ? 684 : 313, y = 273;
            Color c = new Color(129, 202, 239);
            drawCapsule(g, x, y + 28, .63, frameNo < 12 ? frameNo / 2 : 0, 0, true);
            for (int i = 0; i < 18; i++) {
                double ang = (i * .77 + tick * .06);
                double r = 28 + ((i * 13 + frameNo * 4) % 100);
                int px = x + (int) (Math.cos(ang) * r * Math.max(.12, 1 - frameNo / 37.0));
                int py = y + (int) (Math.sin(ang) * r * Math.max(.12, 1 - frameNo / 37.0));
                int alpha = Math.max(15, 210 - frameNo * 6);
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha)); g.fillRect(px, py, 5, 5);
            }
        }

        private void drawMessage(Graphics2D g) {
            drawPixelPanel(g, 16, 447, 928, 79);
        }

        private void drawCapsule(Graphics2D g, int cx, int cy, double scale, int opening, int lift, boolean large) {
            int w = large ? 94 : 65, h = large ? 112 : 77;
            w = (int) (w * scale); h = (int) (h * scale);
            int x = cx - w / 2, y = cy - h / 2;
            int split = y + h / 2;
            int topLift = Math.max(0, opening * (large ? 3 : 2));
            g.setColor(new Color(26, 39, 55)); g.fillRoundRect(x - 3, y - 3 - topLift, w + 6, h + 6, w, w);
            g.setColor(new Color(239, 99, 92)); g.fillRoundRect(x, y - topLift, w, h / 2 + 8, w, w);
            g.setColor(new Color(247, 249, 250)); g.fillRoundRect(x, split + lift, w, h / 2, w, w);
            g.setColor(new Color(31, 42, 55)); g.fillRect(x, split + lift, w, Math.max(4, h / 11));
            g.setColor(new Color(255, 255, 255)); g.fillOval(cx - Math.max(7, w / 7), split - Math.max(7, h / 14), Math.max(14, w / 3), Math.max(14, h / 3));
            g.setColor(new Color(31, 42, 55)); g.setStroke(new BasicStroke(Math.max(2, w / 28f)));
            g.drawOval(cx - Math.max(7, w / 7), split - Math.max(7, h / 14), Math.max(14, w / 3), Math.max(14, h / 3));
            if (opening > 0) { g.setColor(new Color(255, 225, 141, 210)); g.fillOval(cx - w / 3, split - h / 12, 2 * w / 3, Math.max(8, h / 6)); }
        }

        private void drawSprite(Graphics2D g, Beast beast, int x, int y, int w, int h, double scaleX, int dx, int dy, float alpha, boolean flip) {
            if (beast == null || beast.sprite == null || alpha <= 0) return;
            int dw = Math.max(1, (int) (w * scaleX)), dh = h;
            int drawX = x + dx + (w - dw) / 2;
            Graphics2D copy = (Graphics2D) g.create();
            copy.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, Math.max(0, Math.min(1, alpha))));
            if (flip) { copy.drawImage(beast.sprite, drawX + dw, y + dy, -dw, dh, null); }
            else copy.drawImage(beast.sprite, drawX, y + dy, dw, dh, null);
            copy.dispose();
        }

        private void drawStatusSigil(Graphics2D g, int x, int y, Color color, String label) {
            g.setColor(new Color(255, 255, 255, 215)); g.fillRoundRect(x - 53, y - 15, 106, 27, 12, 12);
            g.setColor(color); g.setStroke(new BasicStroke(2)); g.drawRoundRect(x - 53, y - 15, 106, 27, 12, 12);
        }

        private void drawProjectile(Graphics2D g, String type, int x, int y, int frame) {
            Color c = typeColor(type); g.setColor(c);
            switch (type) {
                case "Fire" -> { g.fillPolygon(new int[]{x, x-12, x-7, x-15, x+1, x+8, x+13, x+7}, new int[]{y-18,y-3,y+3,y+13,y+10,y+18,y+4,y-2}, 8); g.setColor(new Color(255, 237, 154)); g.fillPolygon(new int[]{x,y-7,x-4,y+6,x+5,y+5},new int[]{y-12,y-1,y+5,y+10,y+4,y-1},6); }
                case "Water" -> { g.fillOval(x-12,y-11,24,24); g.fillPolygon(new int[]{x-10,x,x+10},new int[]{y-3,y-23,y-3},3); g.setColor(new Color(207,246,255)); g.fillOval(x-5,y-8,6,6); }
                case "Electric" -> g.fillPolygon(new int[]{x-5,x+4,x-1,x+11,x-3,x+1,x-11},new int[]{y-17,y-4,y-2,y+3,y+17,y+5,x+3},7);
                case "Grass" -> { g.rotate(-.25,x,y); g.fillOval(x-14,y-7,28,14); g.setColor(new Color(224,255,181)); g.drawLine(x-10,y+1,x+11,y-2); g.rotate(.25,x,y); }
                case "Rock", "Ground" -> { g.fillRect(x-11,y-12,17,16); g.fillRect(x-1,y-6,15,14); g.setColor(new Color(255,238,195)); g.fillRect(x-6,y-8,4,4); }
                case "Psychic" -> { g.setStroke(new BasicStroke(4)); g.drawOval(x-13,y-13,26,26); g.drawOval(x-7,y-7,14,14); g.fillRect(x-3,y-3,6,6); }
                case "Poison" -> { g.fillOval(x-13,y-8,15,15); g.fillOval(x-2,y-13,14,14); g.fillOval(x+3,y+2,11,11); g.setColor(new Color(226,255,167)); g.fillRect(x-2,y-3,4,4); }
                case "Flying" -> { g.fillPolygon(new int[]{x-18,x-1,x+18,x+2,x-3,x-9},new int[]{y+2,y-7,y-17,y+2,y+7,y+4},6); g.fillPolygon(new int[]{x-1,x+12,x+17,x+2},new int[]{y+3,y+7,y+14,y+8},4); }
                case "Bug" -> { g.fillOval(x-9,y-8,18,17); g.drawLine(x-10,y-8,x-17,y-16); g.drawLine(x+10,y-8,x+17,y-16); g.drawLine(x-9,y+4,x-17,y+10); g.drawLine(x+9,y+4,x+17,y+10); }
                case "Fairy" -> { drawPixelStar(g,x,y,22,c); g.setColor(Color.WHITE); g.fillRect(x-2,y-2,4,4); }
                case "Normal" -> { g.fillOval(x-11,y-11,22,22); g.setColor(Color.WHITE); g.fillOval(x-5,y-7,6,6); }
                case "Ice" -> { g.setStroke(new BasicStroke(3)); g.drawLine(x,y-16,x,y+16); g.drawLine(x-14,y-8,x+14,y+8); g.drawLine(x-14,y+8,x+14,y-8); g.fillRect(x-3,y-3,6,6); }
                case "Steel" -> { g.fillPolygon(new int[]{x-15,x+8,x+15,x+9,x-9},new int[]{y-8,y-13,y,y+10,y+8},5); g.setColor(new Color(237,248,255)); g.drawLine(x-8,y-5,x+8,y-3); }
                case "Fighting" -> { g.fillRoundRect(x-11,y-9,23,19,6,6); g.fillRect(x-16,y-3,8,7); g.setColor(new Color(255,231,211)); g.fillRect(x+2,y-4,4,4); }
                case "Ghost" -> { g.fillOval(x-11,y-13,22,23); g.fillRect(x-11,y+1,22,10); g.setColor(new Color(230,225,255)); g.fillOval(x-5,y-5,4,4); g.fillOval(x+3,y-5,4,4); }
                case "Dark" -> { g.fillPolygon(new int[]{x-15,x-5,x,x+7,x+16,x+5,x,x-2,x-10,x-14},new int[]{y-5,y-15,y-8,y-16,y-2,y+6,y+13,y+9,y+11,y+2},10); }
                case "Dragon" -> { g.fillPolygon(new int[]{x-16,x+1,x+15,x+8,x+17,x-2,x-14},new int[]{y-3,y-13,y-6,y+1,y+8,x+11,x+7},7); g.fillRect(x+4,y-5,4,4); }
                default -> { g.fillRect(x-11,y-11,22,22); g.setColor(Color.WHITE); g.fillRect(x-4,y-4,8,8); }
            }
        }

        private void drawTrail(Graphics2D g, String type, int x, int y, int frame) {
            Color c = typeColor(type);
            for (int i = 0; i < 4; i++) { int s = 4 + i * 2; g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 140 - i * 25)); g.fillRect(x - i * 10, y + (i % 2 == 0 ? -7 : 7), s, s); }
        }

        private void drawBurst(Graphics2D g, int x, int y, Color c, int frame) {
            int radius = 12 + frame * 5;
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(15, 205 - frame * 18)));
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4 + frame * .18;
                int px = x + (int) (Math.cos(a) * radius), py = y + (int) (Math.sin(a) * radius);
                g.fillRect(px - 4, py - 4, 8, 8);
            }
            g.fillRect(x - Math.max(2, 18 - frame), y - Math.max(2, 18 - frame), Math.max(4, (18 - frame) * 2), Math.max(4, (18 - frame) * 2));
        }

        private void drawPixelSparkles(Graphics2D g, int x, int y, Color color, int count) {
            for (int i = 0; i < Math.min(count, 24); i++) {
                int px = x + (int) (Math.sin(i * 3.7 + tick * .04) * (22 + i % 5 * 9));
                int py = y + (int) (Math.cos(i * 2.3 + tick * .05) * (17 + i % 4 * 8));
                g.setColor((i + tick / 8) % 3 == 0 ? Color.WHITE : color);
                int size = i % 4 == 0 ? 7 : 4;
                g.fillRect(px, py, size, size);
            }
        }

        private void drawPixelStar(Graphics2D g, int x, int y, int size, Color c) {
            g.setColor(c); g.fillRect(x - size / 2, y - size / 6, size, size / 3); g.fillRect(x - size / 6, y - size / 2, size / 3, size);
            g.fillRect(x - size / 3, y - size / 3, size / 5, size / 5); g.fillRect(x + size / 5, y - size / 3, size / 5, size / 5);
        }

        private void drawPixelPlant(Graphics2D g, int x, int y, Color bloom) {
            g.setColor(new Color(47, 108, 76)); g.fillRect(x, y - 22, 5, 23); g.fillRect(x - 9, y - 11, 10, 5); g.fillRect(x + 4, y - 16, 10, 5);
            g.setColor(bloom); g.fillRect(x - 4, y - 31, 13, 13); g.setColor(new Color(242, 194, 92)); g.fillRect(x, y - 27, 5, 5);
        }
    }

    private static Color typeColor(String type) {
        return switch (type) {
            case "Normal" -> new Color(154, 159, 163); case "Fire" -> new Color(239, 102, 68);
            case "Water" -> new Color(57, 151, 221); case "Electric" -> new Color(242, 191, 45);
            case "Grass" -> new Color(72, 165, 92); case "Ice" -> new Color(102, 198, 214);
            case "Fighting" -> new Color(194, 82, 83); case "Poison" -> new Color(158, 94, 179);
            case "Ground" -> new Color(185, 139, 75); case "Flying" -> new Color(137, 159, 212);
            case "Psychic" -> new Color(226, 104, 153); case "Bug" -> new Color(137, 164, 68);
            case "Rock" -> new Color(166, 144, 88); case "Ghost" -> new Color(112, 103, 163);
            case "Dragon" -> new Color(108, 102, 209); case "Dark" -> new Color(91, 82, 79);
            case "Steel" -> new Color(126, 151, 168); case "Fairy" -> new Color(224, 137, 181);
            default -> new Color(77, 134, 165);
        };
    }

    private static void centered(Graphics2D g, String text, int centerX, int baselineY) {
        FontMetrics fm = g.getFontMetrics(); g.drawString(text, centerX - fm.stringWidth(text) / 2, baselineY);
    }
    private static void rightText(Graphics2D g, String text, int rightX, int baselineY) {
        FontMetrics fm = g.getFontMetrics(); g.drawString(text, rightX - fm.stringWidth(text), baselineY);
    }
    private static String ellipsize(Graphics2D g, String text, int width) {
        FontMetrics fm = g.getFontMetrics();
        while (!text.isEmpty() && fm.stringWidth(text + "…") > width) text = text.substring(0, text.length() - 1);
        return text + "…";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BattleDemo::new);
    }
}
