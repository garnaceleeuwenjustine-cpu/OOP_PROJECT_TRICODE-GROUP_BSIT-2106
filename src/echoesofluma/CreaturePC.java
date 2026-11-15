package echoesofluma;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Member 3's reusable shop, bag, and creature-storage screen.
 * It is a JPanel so the game can show it inside its existing JFrame.
 */
public final class CreaturePC extends JPanel {
    private static final int PARTY_SIZE = 6, BOX_COUNT = 8, BOX_SIZE = 30, MAX_STACK = 99, DEX_SIZE = 28;
    private static final Path GAME_SAVE_FILE = Path.of(System.getProperty("user.home"), ".echoes-of-luma-member3-pack.properties");
    private static final Path PREVIEW_SAVE_FILE = Path.of(System.getProperty("user.home"), ".echoes-of-luma-member3-preview.properties");
    private static final Color DEEP = new Color(18, 57, 55), GREEN = new Color(44, 119, 83);
    private static final Color CREAM = new Color(247, 247, 225), PALE = new Color(232, 239, 217);
    private static final Color INK = new Color(33, 49, 47), MUTED = new Color(96, 115, 104);
    private static final Color GOLD = new Color(241, 190, 78);

    private enum Category { CAPSULE, HEALING, STATUS, TOOL }

    /** Catalog entry: the same enum ID is used for stock, buying, and using items. */
    public enum ItemId {
        FIELD_CAPSULE("Field Capsule", Category.CAPSULE, 45, "Standard wild-catch capsule.", "CAP", 0, 0, ""),
        GROVE_CAPSULE("Grove Capsule", Category.CAPSULE, 70, "Bonus odds for Grass and Bug beasts.", "CAP", 3, 0, ""),
        TIDE_CAPSULE("Tide Capsule", Category.CAPSULE, 70, "Bonus odds for Water and Ice beasts.", "CAP", 6, 0, ""),
        EMBER_CAPSULE("Ember Capsule", Category.CAPSULE, 75, "Bonus odds for Fire and Dragon beasts.", "CAP", 10, 0, ""),
        GLOAM_CAPSULE("Gloam Capsule", Category.CAPSULE, 90, "Bonus odds for Dark, Ghost, and Poison beasts.", "CAP", 15, 0, ""),
        STONE_CAPSULE("Stone Capsule", Category.CAPSULE, 90, "Bonus odds for Rock, Ground, and Steel beasts.", "CAP", 20, 0, ""),
        SKY_CAPSULE("Sky Capsule", Category.CAPSULE, 95, "Bonus odds for Flying, Fairy, and Psychic beasts.", "CAP", 25, 0, ""),
        PRISM_CAPSULE("Prism Capsule", Category.CAPSULE, 240, "Premium bonus odds against every type.", "CAP", 30, 0, ""),
        POTION("Potion", Category.HEALING, 50, "Restore 32 HP to the selected beast or lead partner.", "HP", 0, 32, ""),
        SUPER_POTION("Super Potion", Category.HEALING, 95, "Restore 64 HP to the selected beast or lead partner.", "HP+", 0, 64, ""),
        FULL_RESTORE("Full Restore", Category.HEALING, 180, "Restore all HP and clear a negative condition.", "MAX", 0, 0, "ANY"),
        REVIVAL_LEAF("Revival Leaf", Category.HEALING, 150, "Revive a fainted beast with half HP.", "REV", 0, 0, "FAINTED"),
        ANTIDOTE("Antidote", Category.STATUS, 40, "Cure Poison.", "CURE", 0, 0, "POISON"),
        BURN_SALVE("Burn Salve", Category.STATUS, 40, "Cure Burn.", "CURE", 0, 0, "BURN"),
        WAKE_HERB("Wake Herb", Category.STATUS, 45, "Wake a sleeping beast.", "CURE", 0, 0, "SLEEP"),
        STATIC_GROUNDING("Static Grounding", Category.STATUS, 45, "Cure Paralysis.", "CURE", 0, 0, "PARALYSIS"),
        CLEAR_BELL("Clear Bell", Category.STATUS, 50, "Clear Confusion.", "CURE", 0, 0, "CONFUSION"),
        GUARD_PATCH("Guard Patch", Category.STATUS, 55, "Restore a beast's lowered guard.", "CURE", 0, 0, "GUARD_DOWN"),
        CLEANSE_TONIC("Cleanse Tonic", Category.STATUS, 95, "Clear any non-fainting condition.", "CURE+", 0, 0, "ANY_STATUS"),
        SMOKE_BOMB("Smoke Bomb", Category.TOOL, 65, "A field tool for retreating from a wild encounter.", "TOOL", 0, 0, "");

        final String label, description, badge, cure;
        final Category category;
        final int price, unlockStage, heal;
        ItemId(String label, Category category, int price, String description, String badge, int unlockStage, int heal, String cure) {
            this.label = label; this.category = category; this.price = price; this.description = description;
            this.badge = badge; this.unlockStage = unlockStage; this.heal = heal; this.cure = cure;
        }
        public String label() { return label; }
        public int price() { return price; }
    }

    /** Plain data transfer object for syncing the game roster into this screen. */
    public record CreatureInfo(String name, String types, int level, int xp, int hp, int maxHp, String condition) { }

    private static final class Creature {
        String name, types, condition;
        int level, xp, hp, maxHp;
        Creature(String name, String types, int level, int xp, int hp, int maxHp, String condition) {
            this.name = name; this.types = types; this.level = Math.max(1, level); this.xp = Math.max(0, xp);
            this.maxHp = Math.max(1, maxHp); this.hp = Math.max(0, Math.min(this.maxHp, hp));
            this.condition = condition == null ? "NONE" : condition;
        }
    }

    private record Slot(boolean party, int box, int index) { }

    private final Runnable onBack;
    private final Path saveFile;
    private final Map<ItemId, Integer> stock = new EnumMap<>(ItemId.class);
    private final List<Creature> party = new ArrayList<>(Collections.nCopies(PARTY_SIZE, null));
    private final List<List<Creature>> boxes = new ArrayList<>();
    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel wallet = new JLabel();
    private final JLabel status = new JLabel("Your travel supplies are ready.");
    private final JLabel bagTitle = new JLabel("TRAIL BAG");
    private final JButton backButton = button("←  Return to World Map", GREEN);
    private final JLabel dexSummary = new JLabel("No Beasts discovered yet.");
    private final JPanel bagRows = verticalPanel(), shopRows = verticalPanel();
    private final JPanel dexRows = verticalPanel();
    private final JPanel partySlots = new JPanel(new GridLayout(2, 3, 8, 8));
    private final JPanel boxSlots = new JPanel(new GridLayout(0, 6, 6, 6));
    private final JPanel boxPicker = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
    private final JComboBox<String> filter = new JComboBox<>(new String[] {"All items", "Capsules", "Healing", "Status cures", "Tools"});
    private final JTextField search = new JTextField(14);
    private Slot selected;
    private int currentBox, coins = 550, completedForestStage, bagLevel = 1;
    private boolean femaleCharacter;
    private Predicate<ItemId> battleItemHandler;
    private Runnable afterBattleItemUse;

    public CreaturePC(Runnable onBack) { this(onBack, false); }

    /** Preview data is only for the standalone demo; the in-game constructor starts without fake beasts. */
    public CreaturePC(Runnable onBack, boolean previewData) {
        this.onBack = onBack == null ? () -> { } : onBack;
        this.saveFile = previewData ? PREVIEW_SAVE_FILE : GAME_SAVE_FILE;
        for (int i = 0; i < BOX_COUNT; i++) boxes.add(new ArrayList<>(Collections.nCopies(BOX_SIZE, null)));
        stock.put(ItemId.FIELD_CAPSULE, 5); stock.put(ItemId.POTION, 3); stock.put(ItemId.ANTIDOTE, 1);
        load();
        if (previewData && partyCount() == 0) {
            party.set(0, new Creature("Emberwren", "Fire / Dragon", 1, 0, 100, 100, "NONE"));
            boxes.get(0).set(0, new Creature("Reefclad", "Water / Rock", 1, 0, 100, 100, "NONE"));
            boxes.get(0).set(1, new Creature("Thornstride", "Grass", 1, 0, 100, 100, "NONE"));
            save();
        }
        build();
        refreshAll();
    }

    private static JPanel verticalPanel() { JPanel panel = new JPanel(); panel.setOpaque(false); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); return panel; }

    private void build() {
        setLayout(new BorderLayout(0, 10)); setBackground(CREAM); setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JPanel header = new JPanel(new BorderLayout(12, 0)); header.setBackground(DEEP);
        header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(190, 210, 160), 3), BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        JLabel title = new JLabel("LUMA FIELD SUPPLIES  ·  BAG  ·  BEAST PC"); title.setForeground(CREAM); title.setFont(new Font(Font.MONOSPACED, Font.BOLD, 19));
        wallet.setForeground(new Color(255, 223, 135)); wallet.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        header.add(title, BorderLayout.CENTER); header.add(wallet, BorderLayout.EAST); add(header, BorderLayout.NORTH);

        tabs.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        tabs.addTab("BAG & ITEMS", createBagPage()); tabs.addTab("BEAST DEX", createDexPage());
        tabs.addTab("BEAST PC", createStoragePage()); tabs.addTab("SHOP", createShopPage());
        add(tabs, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(8, 0)); footer.setOpaque(false);
        status.setForeground(INK); status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        backButton.addActionListener(event -> onBack.run());
        footer.add(status, BorderLayout.CENTER); footer.add(backButton, BorderLayout.EAST); add(footer, BorderLayout.SOUTH);
    }

    private JPanel createBagPage() {
        JPanel page = pagePanel(); JPanel top = new JPanel(new BorderLayout(8, 0)); top.setOpaque(false);
        bagTitle.setForeground(GREEN); bagTitle.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15)); top.add(bagTitle, BorderLayout.WEST);
        filter.addActionListener(event -> refreshBag()); top.add(filter, BorderLayout.EAST); page.add(top, BorderLayout.NORTH);
        page.add(scroll(bagRows), BorderLayout.CENTER); return page;
    }

    private JPanel createShopPage() {
        JPanel page = pagePanel(); JLabel help = new JLabel("Buy capture capsules, healing items, status cures, and field tools with Luma Coins.");
        help.setForeground(INK); help.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14)); page.add(help, BorderLayout.NORTH);
        page.add(scroll(shopRows), BorderLayout.CENTER); return page;
    }

    private JPanel createDexPage() {
        JPanel page = pagePanel();
        JPanel top = new JPanel(new BorderLayout(8, 0)); top.setOpaque(false);
        JLabel title = heading("FIELD GUIDE  ·  CATCH, LEARN, GROW");
        top.add(title, BorderLayout.WEST);
        dexSummary.setForeground(MUTED); dexSummary.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        top.add(dexSummary, BorderLayout.EAST);
        page.add(top, BorderLayout.NORTH);
        page.add(scroll(dexRows), BorderLayout.CENTER);
        return page;
    }

    private JPanel createStoragePage() {
        JPanel page = pagePanel(); JPanel partyBox = new JPanel(new BorderLayout(0, 5)); partyBox.setOpaque(false);
        partyBox.add(heading("YOUR PARTY  ·  MAX 6"), BorderLayout.NORTH); partySlots.setOpaque(false); partyBox.add(partySlots, BorderLayout.CENTER);
        partyBox.setPreferredSize(new Dimension(0, 200)); page.add(partyBox, BorderLayout.NORTH);
        JPanel storage = new JPanel(new BorderLayout(6, 6)); storage.setOpaque(false);
        JPanel row = new JPanel(new BorderLayout(6, 0)); row.setOpaque(false); row.add(heading("PC STORAGE  ·  8 BOXES · 30 SLOTS EACH"), BorderLayout.WEST);
        JPanel finder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)); finder.setOpaque(false); search.setToolTipText("Search beasts in this box");
        JButton find = button("Find", GREEN); find.addActionListener(event -> refreshStorage());
        JButton clear = button("Clear", MUTED); clear.addActionListener(event -> { search.setText(""); refreshStorage(); });
        finder.add(search); finder.add(find); finder.add(clear); row.add(finder, BorderLayout.EAST); storage.add(row, BorderLayout.NORTH);
        storage.add(boxPicker, BorderLayout.CENTER); boxPicker.setOpaque(false);
        JScrollPane slots = scroll(boxSlots); storage.add(slots, BorderLayout.SOUTH); page.add(storage, BorderLayout.CENTER);
        return page;
    }

    private JPanel pagePanel() { JPanel page = new JPanel(new BorderLayout(8, 10)); page.setBackground(CREAM); page.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); return page; }
    private JScrollPane scroll(JPanel panel) { JScrollPane scroll = new JScrollPane(panel); scroll.setBorder(BorderFactory.createLineBorder(new Color(190, 207, 181))); scroll.getViewport().setBackground(CREAM); return scroll; }
    private JLabel heading(String text) { JLabel label = new JLabel(text); label.setForeground(GREEN); label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13)); return label; }
    private JButton button(String text, Color color) { JButton button = new JButton(text); button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12)); button.setForeground(Color.WHITE); button.setBackground(color); button.setFocusPainted(false); button.setOpaque(true); button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color.darker()), BorderFactory.createEmptyBorder(7, 10, 7, 10))); return button; }

    private JPanel itemRow(ItemId item, boolean inShop) {
        JPanel row = new JPanel(new BorderLayout(10, 4)); row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 221, 203)), BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        JPanel description = verticalPanel(); JLabel name = new JLabel(item.badge + "   " + item.label); name.setForeground(INK); name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        JLabel details = new JLabel("<html>" + escape(item.description) + "</html>"); details.setForeground(MUTED); details.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        description.add(name); description.add(Box.createVerticalStrut(3)); description.add(details); row.add(description, BorderLayout.CENTER);
        int count = stock.getOrDefault(item, 0); String actionText; boolean enabled;
        if (inShop) { actionText = isUnlocked(item) ? item.price + " coins  ·  Buy" : "Unlock at stage " + item.unlockStage; enabled = isUnlocked(item) && coins >= item.price && count < MAX_STACK; }
        else if (item.category == Category.CAPSULE || item.category == Category.TOOL) { actionText = count + " owned"; enabled = false; }
        else { actionText = "Use  ·  " + count; enabled = count > 0; }
        JButton action = button(actionText, inShop ? new Color(88, 147, 81) : GREEN); action.setEnabled(enabled);
        if (inShop) action.addActionListener(event -> purchase(item, 1));
        else if (item.category != Category.CAPSULE && item.category != Category.TOOL) action.addActionListener(event -> use(item));
        row.add(action, BorderLayout.EAST); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70)); return row;
    }

    private void refreshAll() { wallet.setText("◈  " + coins + " LUMA COINS"); bagTitle.setText((femaleCharacter ? "FLORAL BAG" : "EXPLORER BAG") + "  ·  LV " + bagLevel + "  ·  " + stacks() + " item stacks"); refreshBag(); refreshShop(); refreshParty(); refreshBoxes(); refreshStorage(); refreshDex(); }
    private int stacks() { int result = 0; for (int amount : stock.values()) if (amount > 0) result++; return result; }
    private void refreshBag() { bagRows.removeAll(); String chosen = String.valueOf(filter.getSelectedItem()); for (ItemId item : ItemId.values()) if (stock.getOrDefault(item, 0) > 0 && matches(item, chosen)) bagRows.add(itemRow(item, false)); if (bagRows.getComponentCount() == 0) bagRows.add(new JLabel("No supplies in this category. Visit the Shop to restock.")); bagRows.revalidate(); bagRows.repaint(); }
    private boolean matches(ItemId item, String chosen) { return switch (chosen) { case "Capsules" -> item.category == Category.CAPSULE; case "Healing" -> item.category == Category.HEALING; case "Status cures" -> item.category == Category.STATUS; case "Tools" -> item.category == Category.TOOL; default -> true; }; }
    private void refreshShop() { shopRows.removeAll(); for (ItemId item : ItemId.values()) shopRows.add(itemRow(item, true)); shopRows.revalidate(); shopRows.repaint(); }
    private void refreshParty() { partySlots.removeAll(); for (int i = 0; i < PARTY_SIZE; i++) { final int slot = i; Slot ref = new Slot(true, -1, i); partySlots.add(beastSlot(party.get(i), "PARTY " + (i + 1), selected != null && selected.equals(ref), () -> click(ref))); } partySlots.revalidate(); partySlots.repaint(); }
    private void refreshBoxes() { boxPicker.removeAll(); for (int i = 0; i < BOX_COUNT; i++) { final int box = i; JButton b = new JButton("Box " + (i + 1) + "  " + boxCount(i) + "/" + BOX_SIZE); b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11)); b.setBackground(i == currentBox ? GOLD : PALE); b.addActionListener(event -> { currentBox = box; selected = null; refreshAll(); }); boxPicker.add(b); } boxPicker.revalidate(); boxPicker.repaint(); }
    private void refreshStorage() { boxSlots.removeAll(); String query = search.getText().trim().toLowerCase(Locale.ROOT); for (int i = 0; i < BOX_SIZE; i++) { final int slot = i; Creature c = boxes.get(currentBox).get(i); if (c != null && !query.isEmpty() && !c.name.toLowerCase(Locale.ROOT).contains(query) && !c.types.toLowerCase(Locale.ROOT).contains(query)) { JPanel spacer = new JPanel(); spacer.setOpaque(false); boxSlots.add(spacer); continue; } Slot ref = new Slot(false, currentBox, i); boxSlots.add(beastSlot(c, "SLOT " + (i + 1), ref.equals(selected), () -> click(ref))); } boxSlots.revalidate(); boxSlots.repaint(); }
    private void refreshDex() {
        Map<String, Creature> discovered = new java.util.LinkedHashMap<>();
        for (Creature creature : party) if (creature != null) discovered.putIfAbsent(creature.name.toLowerCase(Locale.ROOT), creature);
        for (List<Creature> box : boxes) for (Creature creature : box) if (creature != null) discovered.putIfAbsent(creature.name.toLowerCase(Locale.ROOT), creature);
        dexRows.removeAll();
        int pcCount = Math.max(0, discovered.size() - partyCount());
        dexSummary.setText("DISCOVERED  " + discovered.size() + " / " + DEX_SIZE + "   ·   PARTY " + partyCount() + "/" + PARTY_SIZE + "   ·   PC " + pcCount);
        if (discovered.isEmpty()) {
            JLabel empty = new JLabel("Catch a Beast to record it here and unlock its learning.");
            empty.setForeground(MUTED); empty.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            dexRows.add(empty);
        } else {
            for (Creature creature : discovered.values()) dexRows.add(dexEntry(creature));
        }
        dexRows.revalidate(); dexRows.repaint();
    }
    private JPanel dexEntry(Creature creature) {
        JPanel row = new JPanel(new BorderLayout(12, 4)); row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 221, 203)), BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JPanel info = verticalPanel();
        JLabel name = new JLabel(creature.name + "  ·  Lv. " + creature.level); name.setForeground(INK); name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        JLabel types = new JLabel(creature.types + "  ·  " + creature.hp + "/" + creature.maxHp + " HP"); types.setForeground(MUTED); types.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        info.add(name); info.add(types); row.add(info, BorderLayout.CENTER);
        JPanel progress = new JPanel(new BorderLayout(0, 4)); progress.setOpaque(false);
        JLabel learning = new JLabel("✦ Learning unlocked"); learning.setForeground(GREEN); learning.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        JProgressBar xp = new JProgressBar(0, 100); xp.setValue(creature.xp % 100); xp.setStringPainted(true); xp.setString("EXP " + (creature.xp % 100) + "/100"); xp.setForeground(GREEN); xp.setBackground(PALE);
        progress.add(learning, BorderLayout.NORTH); progress.add(xp, BorderLayout.SOUTH); row.add(progress, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        return row;
    }
    private JButton beastSlot(Creature c, String slot, boolean active, Runnable click) { String html = c == null ? "<html><center><b>＋</b><br>" + slot + "<br>EMPTY</center></html>" : "<html><center><b>" + escape(c.name) + "</b><br>" + escape(c.types) + "<br>Lv. " + c.level + " · " + c.hp + "/" + c.maxHp + " HP<br>" + ("NONE".equals(c.condition) ? slot : escape(c.condition)) + "</center></html>"; JButton b = new JButton(html); b.setPreferredSize(new Dimension(140, 72)); b.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11)); b.setForeground(INK); b.setBackground(active ? new Color(255, 232, 160) : c == null ? CREAM : PALE); b.setFocusPainted(false); b.setBorder(BorderFactory.createLineBorder(active ? GOLD.darker() : new Color(178, 197, 167), active ? 3 : 1)); b.addActionListener(event -> click.run()); return b; }

    private void click(Slot destination) { Creature target = get(destination); if (selected == null) { if (target == null) { message("Choose a beast first."); return; } selected = destination; message(target.name + " selected; choose a destination slot."); refreshParty(); refreshStorage(); return; } if (selected.equals(destination)) { selected = null; refreshParty(); refreshStorage(); return; } Creature moving = get(selected); if (moving == null) { selected = null; refreshAll(); return; } if (selected.party && !destination.party && partyCount() <= 1) { message("Keep at least one beast in your party."); return; } set(selected, target); set(destination, moving); selected = null; save(); refreshAll(); message(target == null ? moving.name + " moved." : moving.name + " and " + target.name + " swapped places."); }
    private Creature get(Slot slot) { return slot.party ? party.get(slot.index) : boxes.get(slot.box).get(slot.index); }
    private void set(Slot slot, Creature c) { if (slot.party) party.set(slot.index, c); else boxes.get(slot.box).set(slot.index, c); }
    private int partyCount() { int n = 0; for (Creature c : party) if (c != null) n++; return n; }
    private int boxCount(int box) { int n = 0; for (Creature c : boxes.get(box)) if (c != null) n++; return n; }

    private void purchase(ItemId item, int quantity) { int count = stock.getOrDefault(item, 0); if (!isUnlocked(item)) return; if (coins < item.price * quantity) { message("Not enough Luma Coins."); return; } if (count + quantity > MAX_STACK) { message("That item stack is full."); return; } coins -= item.price * quantity; stock.put(item, count + quantity); save(); refreshAll(); message("Bought " + quantity + " × " + item.label + "."); }
    private boolean isUnlocked(ItemId item) { return item.category != Category.CAPSULE || item.unlockStage == 0 || completedForestStage >= item.unlockStage; }
    private void use(ItemId item) {
        if (stock.getOrDefault(item, 0) <= 0) return;
        if (item.category == Category.CAPSULE) { message("Capsules are selected during wild encounters."); return; }
        if (battleItemHandler != null) {
            if (!battleItemHandler.test(item)) { message("That item cannot be used in this battle."); return; }
            stock.put(item, stock.get(item) - 1); save(); refreshAll(); message(item.label + " used in battle.");
            Runnable afterUse = afterBattleItemUse;
            if (afterUse != null) afterUse.run();
            return;
        }
        Creature target = selected == null ? firstParty() : get(selected);
        if (target == null) { message("Add a partner or select a stored beast first."); return; }
        if (!apply(item, target)) return;
        stock.put(item, stock.get(item) - 1); save(); refreshAll(); message(item.label + " used on " + target.name + ".");
    }
    private Creature firstParty() { for (Creature c : party) if (c != null) return c; return null; }
    private boolean apply(ItemId item, Creature c) { if (item.heal > 0) { if (c.hp <= 0 || c.hp >= c.maxHp) { message(c.name + " does not need HP recovery."); return false; } int recovered = Math.min(item.heal, c.maxHp - c.hp); c.hp += recovered; message(c.name + " recovered " + recovered + " HP."); return true; } if ("ANY".equals(item.cure)) { if (c.hp >= c.maxHp && "NONE".equals(c.condition)) { message(c.name + " is already fully restored."); return false; } c.hp = c.maxHp; c.condition = "NONE"; return true; } if ("FAINTED".equals(item.cure)) { if (c.hp > 0) { message("Revival Leaf is for a fainted beast."); return false; } c.hp = Math.max(1, c.maxHp / 2); c.condition = "NONE"; return true; } if ("ANY_STATUS".equals(item.cure)) { if (c.hp <= 0 || "NONE".equals(c.condition)) { message("There is no curable condition."); return false; } c.condition = "NONE"; return true; } if (item.category == Category.STATUS) { if (!item.cure.equalsIgnoreCase(c.condition)) { message(c.name + " does not have " + item.cure.replace('_', ' ') + "."); return false; } c.condition = "NONE"; return true; } message(item.label + " is used during a battle or encounter."); return false; }

    /** Adds a catch to the party first, then to the first available PC slot. */
    public boolean addCaughtBeast(String name, String types, int level, int xp) {
        if (name == null || name.isBlank()) return false;
        Creature existing = findCreature(name);
        int safeLevel = Math.max(1, level), maxHp = 100 + (safeLevel - 1) * 10;
        if (existing != null) {
            existing.types = types == null ? "Unknown" : types;
            existing.level = safeLevel;
            existing.xp = Math.max(0, xp);
            existing.maxHp = maxHp;
            existing.hp = Math.min(existing.hp, maxHp);
            save(); refreshAll();
            return true;
        }
        Creature beast = new Creature(name, types == null ? "Unknown" : types, safeLevel, xp, maxHp, maxHp, "NONE");
        for (int i = 0; i < PARTY_SIZE; i++) if (party.get(i) == null) { party.set(i, beast); save(); refreshAll(); message(name + " joined your party!"); return true; }
        for (int b = 0; b < BOX_COUNT; b++) for (int s = 0; s < BOX_SIZE; s++) if (boxes.get(b).get(s) == null) { boxes.get(b).set(s, beast); currentBox = b; save(); refreshAll(); message(name + " was sent to Box " + (b + 1) + "."); return true; }
        message("Your party and PC boxes are full."); return false;
    }

    private Creature findCreature(String name) {
        for (Creature creature : party) if (creature != null && creature.name.equalsIgnoreCase(name)) return creature;
        for (List<Creature> box : boxes) for (Creature creature : box) if (creature != null && creature.name.equalsIgnoreCase(name)) return creature;
        return null;
    }

    /** Updates capsule unlocks from first-region progress. */
    public void setCompletedForestStage(int stage) { completedForestStage = Math.max(0, Math.min(30, stage)); save(); refreshAll(); }
    /** Compatible with the existing BagPanel setter used by BattleDemo. */
    public void setBag(boolean female, int level, int potionCount, boolean gift) { femaleCharacter = female; bagLevel = Math.max(1, Math.min(5, level)); save(); refreshAll(); }
    public void selectTab(int index) { if (index >= 0 && index < tabs.getTabCount()) tabs.setSelectedIndex(index); }
    public void setBackLabel(String label) { backButton.setText("←  " + (label == null || label.isBlank() ? "Return" : label)); }
    public void setBattleItemHandler(Predicate<ItemId> handler, Runnable afterSuccessfulUse) { battleItemHandler = handler; afterBattleItemUse = afterSuccessfulUse; }
    public void resetGameData() {
        coins = 550; completedForestStage = 0; bagLevel = 1; currentBox = 0; selected = null;
        stock.clear(); stock.put(ItemId.FIELD_CAPSULE, 5); stock.put(ItemId.POTION, 3); stock.put(ItemId.ANTIDOTE, 1);
        for (int i = 0; i < PARTY_SIZE; i++) party.set(i, null);
        for (List<Creature> box : boxes) for (int i = 0; i < box.size(); i++) box.set(i, null);
        save(); refreshAll(); message("A fresh travel pack is ready.");
    }
    public int getCoins() { return coins; }
    public int getItemCount(ItemId item) { return stock.getOrDefault(item, 0); }
    public void setCoins(int amount) { coins = Math.max(0, amount); save(); refreshAll(); }
    public void addCoins(int amount) { if (amount > 0) setCoins(coins + amount); }
    public boolean grantItem(ItemId item, int amount) {
        if (item == null || amount <= 0 || stock.getOrDefault(item, 0) + amount > MAX_STACK) return false;
        stock.put(item, stock.getOrDefault(item, 0) + amount); save(); refreshAll(); return true;
    }
    public boolean consumeItem(ItemId item, int amount) {
        if (item == null || amount <= 0 || stock.getOrDefault(item, 0) < amount) return false;
        stock.put(item, stock.get(item) - amount); save(); refreshAll(); return true;
    }
    /** Use this API when an external game event asks the same shop to make a purchase. */
    public boolean buyItem(ItemId item, int quantity) {
        if (item == null || quantity < 1 || !isUnlocked(item) || coins < (long) item.price * quantity
                || stock.getOrDefault(item, 0) + quantity > MAX_STACK) return false;
        purchase(item, quantity); return true;
    }
    public List<CreatureInfo> getPartySnapshot() { return snapshot(party); }
    public List<CreatureInfo> getBoxSnapshot(int box) { return box < 0 || box >= BOX_COUNT ? List.of() : snapshot(boxes.get(box)); }
    private List<CreatureInfo> snapshot(List<Creature> source) { List<CreatureInfo> result = new ArrayList<>(); for (Creature c : source) if (c != null) result.add(new CreatureInfo(c.name, c.types, c.level, c.xp, c.hp, c.maxHp, c.condition)); return List.copyOf(result); }

    private void message(String text) { status.setText(text); }
    private static String escape(String text) { return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    private void load() { if (!Files.isRegularFile(saveFile)) return; Properties p = new Properties(); try (InputStream in = Files.newInputStream(saveFile)) { p.load(in); coins = integer(p.getProperty("coins"), coins); completedForestStage = integer(p.getProperty("forestStage"), 0); bagLevel = integer(p.getProperty("bagLevel"), 1); for (ItemId id : ItemId.values()) stock.put(id, Math.max(0, Math.min(MAX_STACK, integer(p.getProperty("item." + id.name()), 0)))); for (int i = 0; i < PARTY_SIZE; i++) party.set(i, decode(p.getProperty("party." + i))); for (int b = 0; b < BOX_COUNT; b++) for (int s = 0; s < BOX_SIZE; s++) boxes.get(b).set(s, decode(p.getProperty("box." + b + "." + s))); } catch (IOException | IllegalArgumentException ignored) { } }
    private void save() { Properties p = new Properties(); p.setProperty("coins", Integer.toString(coins)); p.setProperty("forestStage", Integer.toString(completedForestStage)); p.setProperty("bagLevel", Integer.toString(bagLevel)); for (ItemId id : ItemId.values()) p.setProperty("item." + id.name(), Integer.toString(stock.getOrDefault(id, 0))); for (int i = 0; i < PARTY_SIZE; i++) p.setProperty("party." + i, encode(party.get(i))); for (int b = 0; b < BOX_COUNT; b++) for (int s = 0; s < BOX_SIZE; s++) p.setProperty("box." + b + "." + s, encode(boxes.get(b).get(s))); try (OutputStream out = Files.newOutputStream(saveFile)) { p.store(out, "Echoes of Luma Member 3 shop and bag data"); } catch (IOException error) { message("Could not save inventory: " + error.getMessage()); } }
    private static String encode(Creature c) { if (c == null) return ""; String raw = String.join("\t", c.name, c.types, Integer.toString(c.level), Integer.toString(c.xp), Integer.toString(c.hp), Integer.toString(c.maxHp), c.condition); return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8)); }
    private static Creature decode(String value) { if (value == null || value.isBlank()) return null; String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8); String[] x = raw.split("\\t", -1); if (x.length != 7) return null; return new Creature(x[0], x[1], integer(x[2], 1), integer(x[3], 0), integer(x[4], 1), integer(x[5], 1), x[6]); }
    private static int integer(String value, int fallback) { try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return fallback; } }

    /** Standalone visual preview for Member 3; in-game code embeds this panel instead. */
    public static void main(String[] args) { SwingUtilities.invokeLater(() -> { try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { } JFrame frame = new JFrame("Echoes of Luma — Bag, Beast PC, Shop"); frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); frame.setSize(1100, 800); frame.setMinimumSize(new Dimension(950, 680)); frame.setLocationRelativeTo(null); frame.setContentPane(new CreaturePC(frame::dispose, true)); frame.setVisible(true); }); }
}
