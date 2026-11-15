package echoesofluma;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JTextArea;
import javax.swing.Timer;
import java.awt.AlphaComposite;
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
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.RadialGradientPaint;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

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

    private enum Kind { DAMAGE, BUFF, DEBUFF }
    private enum Phase { SELECT, THROWING_CAPSULE, SUMMONING, COMMAND, MOVE_SELECT, CAPSULE_SELECT, ETHICS_QUESTION, ETHICS_FEEDBACK, ATTUNE_REVEAL, ATTUNE_INPUT, ACTION_NOTICE, ANIMATING, RECALL_PLAYER, RECALL_ENEMY, RESULT }
    private enum TrainerProfile {
        MALE("male_01", "Male", "Crimson Trailblazer", "04-male-crimson-trailblazer.jpg", new Color(191, 67, 74)),
        MALE_DARK("male_02", "Male", "Midnight Rover", "02-male-midnight-rover.png", new Color(87, 91, 119)),
        MALE_AZURE("male_03", "Male", "Azure Vanguard", "03-male-azure-vanguard.png", new Color(73, 115, 189)),
        MALE_SILVER("male_04", "Male", "Silver Scholar", "01-male-silver-scholar.png", new Color(118, 128, 153)),
        FEMALE("female_01", "Female", "Azure Scout", "07-female-azure-scout.png", new Color(73, 130, 201)),
        FEMALE_INDIGO("female_02", "Female", "Indigo Duelist", "05-female-indigo-duelist.png", new Color(92, 89, 176)),
        FEMALE_STARLIGHT("female_03", "Female", "Starlight Scout", "06-female-starlight-scout.png", new Color(135, 134, 165)),
        FEMALE_VIOLET("female_04", "Female", "Violet Ranger", "08-female-violet-ranger.jpg", new Color(132, 89, 194)),
        FEMALE_ROSE("female_05", "Female", "Rose Challenger", "09-female-rose-challenger.jpg", new Color(206, 94, 134));

        final String id;
        final String label;
        final String title;
        final Color accent;
        final BufferedImage art;

        TrainerProfile(String id, String label, String title, String artFile, Color accent) {
            this.id = id;
            this.label = label;
            this.title = title;
            this.accent = accent;
            this.art = loadTrainerArt(artFile);
        }

        static TrainerProfile fromId(String id) {
            for (TrainerProfile profile : values()) if (profile.id.equalsIgnoreCase(id)) return profile;
            if ("female".equalsIgnoreCase(id)) return FEMALE;
            return MALE;
        }
    }

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

    private enum CapsuleType {
        FIELD("Field", new Color(222, 114, 67), "Standard odds", 0.00, 0.00, -1, 0),
        GROVE("Grove", new Color(76, 166, 88), "Grass / Bug", 0.02, 0.12, 0, 3, "Grass", "Bug"),
        TIDE("Tide", new Color(50, 148, 205), "Water / Ice", 0.04, 0.12, 0, 6, "Water", "Ice"),
        EMBER("Ember", new Color(224, 93, 55), "Fire / Dragon", 0.06, 0.12, 0, 10, "Fire", "Dragon"),
        GLOAM("Gloam", new Color(131, 91, 176), "Dark / Ghost / Poison", 0.08, 0.12, 0, 15, "Dark", "Ghost", "Poison"),
        STONE("Stone", new Color(137, 132, 111), "Rock / Ground / Steel", 0.10, 0.12, 0, 20, "Rock", "Ground", "Steel"),
        SKY("Sky", new Color(80, 179, 184), "Flying / Fairy / Psychic", 0.12, 0.12, 0, 25, "Flying", "Fairy", "Psychic"),
        PRISM("Prism", new Color(200, 165, 73), "Any type", 0.28, 0.00, 0, 30);

        final String label;
        final Color color;
        final String affinityText;
        final double universalBonus;
        final double matchingBonus;
        final int unlockRegion;
        final int unlockStage;
        final Set<String> affinities;

        CapsuleType(String label, Color color, String affinityText, double universalBonus, double matchingBonus,
                int unlockRegion, int unlockStage, String... affinities) {
            this.label = label;
            this.color = color;
            this.affinityText = affinityText;
            this.universalBonus = universalBonus;
            this.matchingBonus = matchingBonus;
            this.unlockRegion = unlockRegion;
            this.unlockStage = unlockStage;
            this.affinities = Set.of(affinities);
        }

        boolean isUnlocked() {
            return unlockRegion < 0 || SAVE.completedStages[unlockRegion] >= unlockStage;
        }

        String unlockRequirement() {
            return unlockRegion < 0 ? "AVAILABLE" : "CLEAR STAGE " + (unlockRegion + 1) + "." + unlockStage;
        }

        double bonusAgainst(Beast beast) {
            if (beast == null) return universalBonus;
            boolean match = affinities.contains(beast.firstType)
                || (beast.secondType != null && affinities.contains(beast.secondType));
            return universalBonus + (match ? matchingBonus : 0.0);
        }

        boolean matches(Beast beast) {
            return beast != null && (affinities.contains(beast.firstType)
                || (beast.secondType != null && affinities.contains(beast.secondType)));
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

    private static final class EthicsQuestion {
        final String topic;
        final String scenario;
        final String[] answers;
        final int correctIndex;
        final String explanation;

        EthicsQuestion(String topic, String scenario, String a, String b, String c, int correctIndex, String explanation) {
            this.topic = topic;
            this.scenario = scenario;
            this.answers = new String[] {a, b, c};
            this.correctIndex = correctIndex;
            this.explanation = explanation;
        }
    }

    private static final List<EthicsQuestion> ETHICS_QUESTIONS = List.of(
        new EthicsQuestion("Respect & inclusion", "A classmate is being left out of a group activity. What is the most ethical response?",
            "Invite them and listen respectfully", "Ignore the situation", "Make fun of them", 0,
            "Respect, inclusion, and empathy help everyone participate."),
        new EthicsQuestion("Honesty", "You notice a mistake in your group's report that makes your result look better. What should you do?",
            "Hide it before submission", "Correct it and tell the group", "Blame another member", 1,
            "Honesty means correcting the record even when it is inconvenient."),
        new EthicsQuestion("Privacy", "A friend shares a private concern with you. What is the best response?",
            "Post it anonymously online", "Tell the whole class", "Keep it private and ask what support they want", 2,
            "Respecting privacy protects trust; share only when safety requires help."),
        new EthicsQuestion("Fairness", "A teammate did not get a chance to speak during planning. What should the group do?",
            "Make room for their ideas", "Choose without asking them", "Dismiss their ideas as unimportant", 0,
            "Fairness gives each person a real chance to contribute."),
        new EthicsQuestion("Responsibility", "You borrowed a classmate's notes and accidentally damaged them. What should you do?",
            "Return them and say nothing", "Tell them and offer to replace or repair them", "Claim they were already damaged", 1,
            "Taking responsibility means being truthful and helping repair harm."),
        new EthicsQuestion("Academic integrity", "A classmate asks you to send your completed individual assignment. What is the fairest response?",
            "Send your file so they can copy it", "Change your name and submit theirs", "Explain the ideas and help them learn without sharing your work", 2,
            "Helping someone learn is supportive; copying misrepresents authorship."),
        new EthicsQuestion("Empathy", "A student makes a mistake while presenting and looks embarrassed. What should you do?",
            "Give them time and respond kindly", "Laugh so others notice", "Interrupt and take over", 0,
            "Empathy recognizes how another person feels and avoids adding harm."),
        new EthicsQuestion("Digital citizenship", "You receive an unverified accusation about a classmate in a group chat. What should you do?",
            "Forward it to other groups", "Do not spread it; check facts and use a trusted channel", "Add a rumor of your own", 1,
            "Responsible communication checks information before sharing it."),
        new EthicsQuestion("Consent", "Your group wants to use a classmate's photo in a presentation. What should happen first?",
            "Use it because it is already online", "Edit it into a joke", "Ask for their permission", 2,
            "Consent gives people a choice about how their image is used."),
        new EthicsQuestion("Accountability", "Your group missed a deadline because you forgot your assigned task. What is the ethical next step?",
            "Pretend the task was finished", "Own the mistake and agree on a recovery plan", "Blame the group leader", 1,
            "Accountability combines honesty with a practical effort to make things right."),
        new EthicsQuestion("Respectful disagreement", "You disagree with a classmate's opinion during discussion. What is the best response?",
            "Explain your reasons and hear theirs", "Attack their character", "Stop them from speaking", 0,
            "Respectful disagreement focuses on ideas and keeps dialogue open."),
        new EthicsQuestion("Compassion", "You see a student struggling to carry project materials. What is the most considerate response?",
            "Take the materials without asking", "Walk by while mocking them", "Offer help and accept their answer", 2,
            "Compassion offers help while still respecting another person's choice.")
    );

    private static final List<EthicsQuestion> BOSS_ETHICS_QUESTIONS = List.of(
        new EthicsQuestion("Fairness & access", "Your team has one spot left in a limited workshop. One student has stronger scores; another has had fewer chances to access the equipment. What is the fairest next step?",
            "Compare the published criteria, discuss access needs transparently, and use the same process for everyone", "Quietly give the spot to your friend", "Choose the higher score without checking whether the selection rules were shared", 0,
            "Fairness combines consistent rules with attention to barriers that affect opportunity."),
        new EthicsQuestion("Privacy & safety", "A classmate privately tells you they may be unsafe at home and asks you to tell nobody. What should you do?",
            "Promise absolute secrecy", "Listen, explain you may need trusted help to keep them safe, and involve an appropriate support person", "Post the story anonymously so others can advise you", 1,
            "Respect privacy while seeking limited, trusted help when someone's safety may be at risk."),
        new EthicsQuestion("Integrity under pressure", "Your group discovers that a result in the final report came from a flawed measurement. Correcting it may lower your grade. What is the most responsible response?",
            "Keep the result because the deadline is close", "Remove the entire section and say nothing", "Disclose the limitation, correct what you can, and explain how it affects the conclusion", 2,
            "Integrity means making the evidence and its limits clear, even when the outcome is less convenient."),
        new EthicsQuestion("Conflicting duties", "You learn that a close friend copied part of a group assignment. The instructor asks whether the work is original. What should you do?",
            "Be honest about what you know, speak privately with your friend, and follow the course process", "Lie because loyalty always comes first", "Accuse them publicly before checking the facts", 0,
            "Loyalty does not require dishonesty; care and due process can be respected together."),
        new EthicsQuestion("Shared responsibility", "A team member's proposal is unpopular, but it may reduce harm to a community affected by your project. How should the team decide?",
            "Dismiss it so the team can finish quickly", "Compare evidence and impacts, hear affected perspectives where possible, and document the decision", "Let the most senior member decide alone", 1,
            "Ethical decisions consider evidence, affected people, and reasons that can be explained to the group.")
    );

    private static final String[] LESSONS = {
        "Patience: slow down and understand the whole situation before acting.",
        "Respect: listen to others and make space for different voices.",
        "Care: prevent harm and protect people who may be vulnerable.",
        "Courage: speak honestly when something unfair needs to change.",
        "Integrity: choose the truthful action even when nobody is watching.",
        "Balance: consider how your choices affect the wider community.",
        "Responsibility: own your choices and help repair their effects.",
        "Humility: value other people's knowledge and contributions.",
        "Kindness: make room for others without expecting a reward.",
        "Cooperation: share effort and credit fairly.",
        "Empathy: try to understand how the situation feels to someone else.",
        "Wisdom: pause and think through likely outcomes.",
        "Fairness: use the same reasonable standards for everyone.",
        "Trust: keep promises and handle shared information carefully.",
        "Community: work together so everyone can belong.",
        "Hope: encourage people to keep learning after a setback.",
        "Adaptability: respond thoughtfully when circumstances change.",
        "Self-control: manage frustration before it becomes harm.",
        "Service: use your strengths to support the people around you.",
        "Stewardship: take care of shared spaces and resources.",
        "Accountability: acknowledge mistakes and follow through on repair.",
        "Compassion: offer help while respecting a person's choices.",
        "Discernment: check evidence before repeating a claim.",
        "Dignity: treat each person as worthy of care and consideration.",
        "Listening: seek to understand before deciding what to say.",
        "Privacy: share personal information only with permission or for safety.",
        "Perseverance: keep working steadily toward a fair solution.",
        "Honesty: communicate clearly and do not misrepresent your work."
    };

    private static final class Beast {
        final String name;
        final BufferedImage sprite;
        final boolean facesRightInSource;
        final String firstType;
        final String secondType;
        final EchoSymbol[] echoPattern;
        final List<Move> moves = new ArrayList<>();
        int hp = 100;
        int maxHp = 100;
        int level = 1;
        int buffTurns;
        int weakenedTurns;

        Beast(String name, String file, String firstType, String secondType, String[] moveNames) {
            this.name = name;
            this.sprite = BeastIllustrations.load(file);
            this.facesRightInSource = !file.equals("02-reefclad.png")
                && !file.equals("05-citrinax.png")
                && !file.equals("06-moonwhisk.png");
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

        @Override public String toString() {
            int index = indexOf(this);
            String state = SAVE != null && index < SAVE.caught.length && SAVE.caught[index] ? "  ·  CAUGHT" : "  ·  WILD";
            return name + "  ·  " + types() + state;
        }
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
    private static final SaveData SAVE = new SaveData();

    private static BufferedImage loadTrainerArt(String fileName) {
        Path asset = Path.of("assets", "trainers", fileName).normalize();
        try {
            BufferedImage image = null;
            if (Files.isRegularFile(asset)) image = javax.imageio.ImageIO.read(asset.toFile());
            if (image == null) {
                try (var resource = BattleDemo.class.getResourceAsStream("/assets/trainers/" + fileName)) {
                    if (resource != null) image = javax.imageio.ImageIO.read(resource);
                }
            }
            if (image == null) return null;
            BufferedImage cutout = clearConnectedTrainerBackdrop(image);
            return hasVisibleTrainerPixels(cutout) ? cutout : image;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }


    /** Removes a flat light background connected to the edges while keeping enclosed clothing highlights. */
    private static BufferedImage clearConnectedTrainerBackdrop(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int length = width * height;
        int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);
        int[] corners = {pixels[0], pixels[width - 1], pixels[(height - 1) * width], pixels[length - 1]};
        int backgroundRed = 0, backgroundGreen = 0, backgroundBlue = 0;
        for (int pixel : corners) {
            backgroundRed += (pixel >>> 16) & 0xff;
            backgroundGreen += (pixel >>> 8) & 0xff;
            backgroundBlue += pixel & 0xff;
        }
        backgroundRed /= corners.length;
        backgroundGreen /= corners.length;
        backgroundBlue /= corners.length;

        // Only remove a backdrop when all four corners agree that the image has a flat border.
        // This protects light hair, sleeves, and other pale clothing in artwork with scenic edges.
        for (int corner : corners) {
            int red = (corner >>> 16) & 0xff;
            int green = (corner >>> 8) & 0xff;
            int blue = corner & 0xff;
            if (Math.abs(red - backgroundRed) > 28 || Math.abs(green - backgroundGreen) > 28
                    || Math.abs(blue - backgroundBlue) > 28) return source;
        }

        boolean[] background = new boolean[length];
        int[] queue = new int[length];
        int tail = 0;
        for (int x = 0; x < width; x++) {
            tail = enqueueTrainerBackdrop(pixels, background, queue, tail, x, backgroundRed, backgroundGreen, backgroundBlue);
            tail = enqueueTrainerBackdrop(pixels, background, queue, tail, (height - 1) * width + x, backgroundRed, backgroundGreen, backgroundBlue);
        }
        for (int y = 1; y < height - 1; y++) {
            tail = enqueueTrainerBackdrop(pixels, background, queue, tail, y * width, backgroundRed, backgroundGreen, backgroundBlue);
            tail = enqueueTrainerBackdrop(pixels, background, queue, tail, y * width + width - 1, backgroundRed, backgroundGreen, backgroundBlue);
        }

        int head = 0;
        while (head < tail) {
            int index = queue[head++];
            int x = index % width;
            if (x > 0) tail = enqueueTrainerBackdrop(pixels, background, queue, tail, index - 1, backgroundRed, backgroundGreen, backgroundBlue);
            if (x + 1 < width) tail = enqueueTrainerBackdrop(pixels, background, queue, tail, index + 1, backgroundRed, backgroundGreen, backgroundBlue);
            if (index >= width) tail = enqueueTrainerBackdrop(pixels, background, queue, tail, index - width, backgroundRed, backgroundGreen, backgroundBlue);
            if (index + width < length) tail = enqueueTrainerBackdrop(pixels, background, queue, tail, index + width, backgroundRed, backgroundGreen, backgroundBlue);
        }

        boolean changed = false;
        for (int i = 0; i < length; i++) {
            if (background[i]) {
                pixels[i] = 0x00000000;
                changed = true;
            }
        }
        if (!changed) return source;
        BufferedImage cutout = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        cutout.setRGB(0, 0, width, height, pixels, 0, width);
        return hasVisibleTrainerPixels(cutout) ? cutout : source;
    }

    private static boolean hasVisibleTrainerPixels(BufferedImage image) {
        int width = image.getWidth(), height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        int visible = 0;
        for (int pixel : pixels) {
            if (((pixel >>> 24) & 0xff) > 32) visible++;
        }
        return visible >= Math.max(1, pixels.length / 50);
    }

    private static int enqueueTrainerBackdrop(int[] pixels, boolean[] background, int[] queue, int tail,
            int index, int red, int green, int blue) {
        if (background[index]) return tail;
        int pixel = pixels[index];
        int alpha = (pixel >>> 24) & 0xff;
        int pixelRed = (pixel >>> 16) & 0xff;
        int pixelGreen = (pixel >>> 8) & 0xff;
        int pixelBlue = pixel & 0xff;
        if (alpha <= 8 || (Math.abs(pixelRed - red) <= 26
                && Math.abs(pixelGreen - green) <= 26 && Math.abs(pixelBlue - blue) <= 26)) {
            background[index] = true;
            queue[tail++] = index;
        }
        return tail;
    }
    private static Beast[] createRoster() {
        return new Beast[] {
            new Beast("Emberwren", "01-emberwren.png", "Fire", "Dragon", new String[] {"Cinder Peck", "Flare Wing", "Kindle Burst", "Hearthguard", "Ashen Feint"}),
            new Beast("Reefclad", "02-reefclad.png", "Water", "Rock", new String[] {"Reef Jab", "Tide Roll", "Coral Breaker", "Shell Shelter", "Undertow Pull"}),
            new Beast("Thornstride", "03-thornstride.png", "Grass", null, new String[] {"Briar Dash", "Leaf Talon", "Wildwood Rush", "Barkskin", "Root Snare"}),
            new Beast("Stormhorn", "04-stormhorn.png", "Electric", "Flying", new String[] {"Volt Talon", "Thunder Gale", "Skyflash", "Static Mantle", "Storm Pressure"}),
            new Beast("Citrinax", "05-citrinax.png", "Rock", "Ground", new String[] {"Amber Bash", "Quartz Claw", "Gemstone Quake", "Citrine Shell", "Faultline Grip"}),
            new Beast("Moonwhisk", "06-moonwhisk.png", "Psychic", "Fairy", new String[] {"Lunar Pounce", "Dream Pulse", "Moonlit Rush", "Calm Focus", "Hush Charm"}),
            new Beast("Fungalump", "07-fungalump.jpg", "Grass", "Poison", new String[] {"Mushroom Bonk", "Spore Puff", "Mycelium Burst", "Mossy Padding", "Sleepy Spores"}),
            new Beast("Skydrake", "08-skydrake.jpg", "Dragon", "Flying", new String[] {"Cloudcoil", "Wingbeat", "Cyclone Dive", "Updraft Guard", "Gale Bind"}),
            new Beast("Lantern Beetle", "09-lantern-beetle.jpg", "Bug", "Electric", new String[] {"Lantern Lunge", "Glowcharge", "Beacon Burst", "Carapace Glow", "Dimming Dust"}),
            new Beast("Pebblekin", "10-pebblekin.jpg", "Rock", null, new String[] {"Pebble Pop", "Boulder Roll", "Stoneheart Slam", "Granite Guard", "Dust Snare"}),
            new Beast("Inkwhorl", "11-inkwhorl.jpg", "Water", "Dark", new String[] {"Ink Flick", "Abyssal Curl", "Midnight Current", "Deepwater Veil", "Inkbind"}),
            new Beast("Frostowl", "12-frostowl.jpg", "Ice", "Flying", new String[] {"Snowfeather", "Glacier Wing", "Whiteout Dive", "Frost Plume", "Cold Front"}),
            new Beast("Thornhop", "13-thornhop.jpg", "Grass", "Fighting", new String[] {"Bramble Kick", "Vine Vault", "Thicket Crash", "Leafy Stance", "Briar Bind"}),
            new Beast("Coralclaw", "14-coralclaw.jpg", "Water", "Bug", new String[] {"Coral Pinch", "Reef Scuttle", "Tidal Claw", "Shell Brace", "Brine Trap"}),
            new Beast("Moonglint", "15-moonglint.jpg", "Fairy", "Psychic", new String[] {"Glimmer Step", "Moonbeam", "Astral Bloom", "Starlit Poise", "Dreamcatcher"}),
            new Beast("Kelpcoil", "16-kelpcoil.jpg", "Water", "Grass", new String[] {"Kelp Lash", "Current Twist", "Tidal Coil", "Seaweed Wrap", "Silt Snare"}),
            new Beast("Dunecrest", "17-dunecrest.jpg", "Ground", "Normal", new String[] {"Dune Pounce", "Sand Flick", "Mirage Rush", "Sandskin", "Quicksand Grip"}),
            new Beast("Cogfrog", "18-cogfrog.jpg", "Electric", "Steel", new String[] {"Gear Hop", "Spark Spring", "Clockwork Crash", "Copper Plating", "Magnet Lock"}),
            new Beast("Petalhart", "19-petalhart.jpg", "Grass", "Fairy", new String[] {"Petal Prance", "Bloom Beam", "Spring Antlers", "Clover Crown", "Pollen Drowse"}),
            new Beast("Basaltbat", "20-basaltbat.jpg", "Rock", "Flying", new String[] {"Basalt Wing", "Cave Gust", "Obsidian Dive", "Stoneflight Guard", "Echo Bind"}),
            new Beast("Geode Snail", "21-geode-snail.jpg", "Rock", "Psychic", new String[] {"Crystal Nudge", "Gem Flicker", "Geode Pulse", "Quartz Shell", "Prism Snare"}),
            new Beast("Thunderstoat", "22-thunderstoat.jpg", "Electric", "Normal", new String[] {"Static Pounce", "Volt Dash", "Thunder Snap", "Charged Fur", "Nerve Jolt"}),
            new Beast("Marshkite", "23-marshkite.jpg", "Flying", "Grass", new String[] {"Reed Wing", "Bog Breeze", "Marsh Dive", "Fen Veil", "Sedge Snare"}),
            new Beast("Starpuff", "24-starpuff.jpg", "Fairy", "Psychic", new String[] {"Star Hop", "Comet Pop", "Stardust Burst", "Nebula Nap", "Orbit Tangle"}),
            new Beast("Vineknuckle", "25-vineknuckle.jpg", "Grass", "Fighting", new String[] {"Vine Fist", "Bramble Punch", "Canopy Smash", "Rooted Stance", "Tangle Grip"}),
            new Beast("Honeyhide", "26-honeyhide.jpg", "Bug", "Steel", new String[] {"Honey Headbutt", "Shell Sting", "Amber Charge", "Waxen Armor", "Sticky Snare"}),
            new Beast("Crockmimic", "27-crockmimic.jpg", "Water", "Rock", new String[] {"Bog Bite", "Shell Snap", "Marsh Chomp", "Mudplate Guard", "Mire Grip"}),
            new Beast("Aurorawhale", "28-aurorawhale.jpg", "Water", "Ice", new String[] {"Aurora Splash", "Glacier Song", "Polar Current", "Northern Veil", "Cold Eddy"}),
        };
    }

    private final JFrame frame = new JFrame("Echoes of Luma — Catch. Learn. Grow.");
    private final Arena arena = new Arena();
    private final JPanel screenCards = new JPanel(new java.awt.CardLayout());
    private final JPanel gameRoot = new JPanel(new BorderLayout(0, 0));
    private final MainMenu mainMenu = new MainMenu();
    private final TrainerSelectionPanel trainerSelection = new TrainerSelectionPanel();
    private final StoryIntroPanel storyIntroPanel = new StoryIntroPanel(this::openStarterChoice);
    private final BagPanel bagPanel = new BagPanel(this::returnToMapFromBag);
    private final WorldMapPanel worldMapPanel = new WorldMapPanel(this::closeWorldMap, this::openBagScreen, this::startStage);
    private final JPanel stageResultCard = new JPanel(new BorderLayout(12, 0));
    private final PixelBitmapLabel stageResultText = new PixelBitmapLabel("STAGE COMPLETE");
    private final JButton stageMapButton = button("Return to World Map", new Color(34, 111, 116));
    private StarterChoicePanel starterChoicePanel;
    private String worldMapReturnScreen = "title";
    private final JPanel cards = new JPanel(new java.awt.CardLayout());
    private final JPanel selectCard = new JPanel();
    private final JPanel commandCard = new JPanel();
    private final JPanel moveCard = new JPanel();
    private final JComboBox<Beast> playerChoice = new JComboBox<>(ROSTER);
    private final JComboBox<Beast> enemyChoice = new JComboBox<>(ROSTER);
    private final JComboBox<String> modeChoice = new JComboBox<>(new String[] {"Wild encounter · no trainer", "Trainer battle"});
    private final JLabel hint = new JLabel("Choose a capsule, then open it to release your beast.");
    private final JLabel selectPreview = new JLabel("CAPSULE 01  ·  EMBERWREN", SwingConstants.CENTER);
    private final JButton[] moveButtons = new JButton[5];
    private final JButton captureButton = button("Capture", new Color(145, 75, 176));
    private JButton fightButton;
    private JButton bagButton;
    private JButton attuneButton;
    private JButton listenButton;
    private JButton runButton;
    private TrainerProfile activeTrainer = TrainerProfile.MALE;
    private final BufferedImage[] battleBackgrounds = {
        loadBattleBackground("starting-forest.png"),
        loadBattleBackground("luma-harbor.png"),
        loadBattleBackground("whispering-forest.png"),
        loadBattleBackground("ember-mountain.png"),
        loadBattleBackground("ethics-academy.png")
    };

    private BattleDemo() {
        SAVE.load();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 650));
        frame.setSize(1120, 760);
        frame.setLocationRelativeTo(null);

        gameRoot.setBackground(PANEL);
        gameRoot.add(arena, BorderLayout.CENTER);
        gameRoot.add(cards, BorderLayout.SOUTH);
        buildSelectionCard();
        buildCommandCard();
        buildMoveCard();
        buildStageResultCard();
        enemyChoice.setSelectedIndex(1);
        cards.add(selectCard, "select");
        cards.add(commandCard, "command");
        cards.add(moveCard, "moves");
        cards.add(stageResultCard, "stage-result");
        showCard("select");

        screenCards.add(mainMenu, "title");
        screenCards.add(trainerSelection, "trainer");
        screenCards.add(storyIntroPanel, "story");
        screenCards.add(gameRoot, "game");
        screenCards.add(bagPanel, "bag");
        screenCards.add(worldMapPanel, "world-map");
        mainMenu.newGame.addActionListener(e -> startNewGame());
        mainMenu.continueGame.addActionListener(e -> continueGame());
        mainMenu.learnButton.addActionListener(e -> showLearningJournal());
        mainMenu.settings.addActionListener(e -> showSettings());
        mainMenu.quit.addActionListener(e -> frame.dispose());
        trainerSelection.backButton.addActionListener(e -> showScreen("title"));

        playerChoice.addActionListener(e -> { updateSelectedPreview(); updateSelectionHint(); });
        enemyChoice.addActionListener(e -> arena.repaint());
        modeChoice.addActionListener(e -> updateCaptureRule());
        arena.startClock();
        frame.setContentPane(screenCards);
        showScreen("title");
        frame.setVisible(true);
    }

    private static BufferedImage loadBattleBackground(String fileName) {
        Path file = Path.of("assets", "battle-backgrounds", fileName);
        try {
            if (Files.isRegularFile(file)) return javax.imageio.ImageIO.read(file.toFile());
            try (var resource = BattleDemo.class.getResourceAsStream("/assets/battle-backgrounds/" + fileName)) {
                if (resource != null) return javax.imageio.ImageIO.read(resource);
            }
        } catch (IOException | RuntimeException ignored) { }
        return null;
    }

    private void showScreen(String name) {
        ((java.awt.CardLayout) screenCards.getLayout()).show(screenCards, name);
        mainMenu.refreshButtons();
        frame.revalidate();
        frame.repaint();
    }

    private String resolveDialogTokens(String text) {
        if (text == null || text.isEmpty()) return "";
        Beast ally = arena.player != null ? arena.player : (Beast) playerChoice.getSelectedItem();
        Beast foe = arena.enemy;
        String allyName = ally == null ? "PARTNER" : ally.name;
        String foeName = foe == null ? "WILD BEAST" : foe.name;
        String moveName = arena.move == null ? "MOVE" : arena.move.name;
        int level = ally == null ? 1 : ally.level;
        int experience = ally == null ? 0 : SAVE.nextLevelXp(ally);
        int chance = arena.captureChanceAtThrow;
        return text.replace("{PLAYER}", activeTrainer.title)
            .replace("{ALLY}", allyName)
            .replace("{BEAST}", foeName)
            .replace("{MOVE}", moveName)
            .replace("{LEVEL}", Integer.toString(level))
            .replace("{TYPE}", ally == null ? "NORMAL" : ally.types())
            .replace("{XP}", experience + "/100")
            .replace("{CAPSULE}", arena.selectedCapsule.label)
            .replace("{CHANCE}", Integer.toString(chance));
    }

    private void openWorldMap(String returnScreen) {
        worldMapReturnScreen = returnScreen;
        worldMapPanel.setTrainer(activeTrainer.art, activeTrainer.title);
        worldMapPanel.setProgress(SAVE.completedStages);
        showScreen("world-map");
    }

    private void openBagScreen() {
        bagPanel.setBag(activeTrainer.label.equalsIgnoreCase("Female"), SAVE.bagLevel, 2, false);
        showScreen("bag");
    }

    private void returnToMapFromBag() {
        openWorldMap("title");
    }

    private void closeWorldMap() {
        showScreen(worldMapReturnScreen);
    }

    private void startStage(int region, int stage) {
        if (region < 0 || region >= SAVE.completedStages.length || stage < 1 || stage > 30) return;
        if (region > 0 && SAVE.completedStages[region - 1] < 30) return;
        if (stage > SAVE.completedStages[region] + 1) return;
        List<Beast> caughtBeasts = new ArrayList<>();
        for (Beast beast : ROSTER) if (SAVE.caught[indexOf(beast)]) caughtBeasts.add(beast);
        if (caughtBeasts.isEmpty()) {
            openWorldMap("title");
            return;
        }
        Beast suggested = ROSTER[Math.max(0, Math.min(ROSTER.length - 1, SAVE.lastBeast))];
        if (!SAVE.caught[indexOf(suggested)]) suggested = caughtBeasts.get(0);
        String regionName = new String[] {"Starting Forest", "Luma Village", "Whispering Forest", "Ember Mountain", "Ethics Academy"}[region];
        String stageLabel = regionName + " · " + (region + 1) + "." + stage + (stage % 5 == 0 ? " · BOSS" : "");
        BattlePartnerPanel partnerPanel = new BattlePartnerPanel(caughtBeasts, stageLabel, suggested,
            chosen -> launchStage(region, stage, chosen), () -> openWorldMap("title"));
        partnerPanel.setName("battle-partner-choice");
        for (java.awt.Component component : screenCards.getComponents()) {
            if ("battle-partner-choice".equals(component.getName())) screenCards.remove(component);
        }
        screenCards.add(partnerPanel, "battle-partner-choice");
        showScreen("battle-partner-choice");
    }

    private void launchStage(int region, int stage, Beast player) {
        if (player == null || !SAVE.caught[indexOf(player)]) return;
        Beast enemy = pickRegionBeast(region, player);
        playerChoice.setSelectedItem(player);
        enemyChoice.setSelectedItem(enemy);
        modeChoice.setSelectedIndex(0);
        SAVE.lastBeast = indexOf(player);
        SAVE.save();
        arena.progressionBattle = true;
        arena.stageRegion = region;
        arena.stageNumber = stage;
        arena.bossBattle = stage % 5 == 0;
        showScreen("game");
        openCapsule();
    }

    private Beast pickRegionBeast(int region, Beast player) {
        String[][] habitats = {
            {"Grass", "Bug", "Normal"},
            {"Water", "Ice", "Flying"},
            {"Grass", "Ghost", "Fairy", "Psychic", "Bug"},
            {"Fire", "Rock", "Ground", "Dragon"},
            {"Steel", "Psychic", "Normal", "Fairy", "Fighting"}
        };
        List<Beast> candidates = new ArrayList<>();
        for (Beast beast : ROSTER) {
            if (beast == player) continue;
            boolean fits = false;
            for (String type : habitats[region]) {
                if (beast.firstType.equals(type) || type.equals(beast.secondType)) { fits = true; break; }
            }
            if (fits) candidates.add(beast);
        }
        if (candidates.isEmpty()) for (Beast beast : ROSTER) if (beast != player) candidates.add(beast);
        return candidates.get(arena.random.nextInt(candidates.size()));
    }

    private void startNewGame() {
        SAVE.reset();
        playerChoice.setSelectedIndex(0);
        updateSelectedPreview();
        showScreen("trainer");
    }

    private void beginWithTrainer(TrainerProfile profile) {
        activeTrainer = profile;
        SAVE.selectTrainer(profile.id);
        startStory();
    }

    private void startStory() {
        showScreen("story");
        storyIntroPanel.begin(activeTrainer.art, activeTrainer.title);
    }

    private void openStarterChoice() {
        Beast grass = ROSTER[2], fire = ROSTER[0], water = ROSTER[1];
        starterChoicePanel = new StarterChoicePanel(
            new String[] {grass.name, fire.name, water.name},
            new String[] {grass.types(), fire.types(), water.types()},
            new BufferedImage[] {grass.sprite, fire.sprite, water.sprite},
            new int[] {indexOf(grass), indexOf(fire), indexOf(water)}, this::chooseStarter);
        if (screenCards.getComponentCount() > 0) {
            for (java.awt.Component component : screenCards.getComponents()) {
                if ("starter".equals(component.getName())) screenCards.remove(component);
            }
        }
        starterChoicePanel.setName("starter");
        screenCards.add(starterChoicePanel, "starter");
        showScreen("starter");
    }

    private void buildStageResultCard() {
        stageResultCard.setBackground(PANEL);
        stageResultCard.setBorder(BorderFactory.createEmptyBorder(18, 26, 18, 26));
        stageResultCard.add(stageResultText, BorderLayout.CENTER);
        stageMapButton.addActionListener(event -> {
            arena.progressionBattle = false;
            openWorldMap("title");
        });
        stageResultCard.add(stageMapButton, BorderLayout.EAST);
    }

    private void chooseStarter(int rosterIndex) {
        Beast starter = ROSTER[Math.max(0, Math.min(ROSTER.length - 1, rosterIndex))];
        SAVE.chooseStarter(starter);
        SAVE.storyComplete = true;
        SAVE.bagLevel = 1;
        SAVE.save();
        playerChoice.setSelectedItem(starter);
        updateSelectedPreview();
        bagPanel.setBag(activeTrainer.label.equalsIgnoreCase("Female"), SAVE.bagLevel, 2, true);
        showScreen("bag");
    }

    private void continueGame() {
        activeTrainer = TrainerProfile.fromId(SAVE.trainerStyle);
        if (!SAVE.storyComplete && !SAVE.hasAnyCaught()) {
            startStory();
        } else {
            int choice = Math.max(0, Math.min(ROSTER.length - 1, SAVE.lastBeast));
            playerChoice.setSelectedIndex(choice);
            updateSelectedPreview();
            openWorldMap("title");
        }
    }

    private void showSettings() {
        JOptionPane.showMessageDialog(frame,
        "CONTROLS\n\nClick a menu choice or use the mouse.\nPress 1, 2, or 3 to answer an ethics question.\nPress 1-8 to choose a capsule; Escape returns to battle.\nPress Enter, Space, or Z to continue dialogue.\n\nEvery beast has five type-based moves. XP is awarded after a win or catch, never for quiz answers.",
            "Echoes of Luma — Settings & Controls", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showLearningJournal() {
        StringBuilder text = new StringBuilder("LEARNINGS UNLOCKED\n\n");
        int count = 0;
        for (int i = 0; i < ROSTER.length; i++) {
            if (SAVE.lessonUnlocked[i]) {
                text.append(ROSTER[i].name).append(" — ").append(LESSONS[i]).append("\n\n");
                count++;
            }
        }
        if (count == 0) text.append("Catch a wild beast to unlock its learning card.");
        JTextArea journal = new JTextArea(text.toString(), 16, 58);
        journal.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        journal.setEditable(false);
        journal.setLineWrap(true);
        journal.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(journal);
        scroll.setPreferredSize(new Dimension(620, 390));
        JOptionPane.showMessageDialog(frame, scroll, "Learning Journal", JOptionPane.INFORMATION_MESSAGE);
    }

    private static final class SaveData {
        private final Path file = Path.of(System.getProperty("user.home"), ".echoes-of-luma-save-28-beasts.properties");
        final boolean[] caught = new boolean[ROSTER.length];
        final boolean[] lessonUnlocked = new boolean[ROSTER.length];
        final int[] experience = new int[ROSTER.length];
        final int[] completedStages = new int[5];
        int lastBeast;
        int bagLevel = 1;
        boolean hasSave;
        boolean storyComplete;
        String trainerStyle = "male";

        void load() {
            if (!Files.isRegularFile(file)) return;
            Properties data = new Properties();
            try (var input = Files.newInputStream(file)) {
                data.load(input);
                hasSave = Boolean.parseBoolean(data.getProperty("started", "true"));
                lastBeast = parseInt(data.getProperty("lastBeast"), 0);
                trainerStyle = data.getProperty("trainerStyle", "male");
                storyComplete = Boolean.parseBoolean(data.getProperty("storyComplete", "false"));
                bagLevel = Math.max(1, Math.min(5, parseInt(data.getProperty("bagLevel"), 1)));
                for (int region = 0; region < completedStages.length; region++) {
                    completedStages[region] = Math.max(0, Math.min(30, parseInt(data.getProperty("region." + region + ".completed"), 0)));
                }
                for (int i = 0; i < ROSTER.length; i++) {
                    caught[i] = Boolean.parseBoolean(data.getProperty("beast." + i + ".caught", "false"));
                    lessonUnlocked[i] = Boolean.parseBoolean(data.getProperty("beast." + i + ".lesson", "false"));
                    experience[i] = Math.max(0, parseInt(data.getProperty("beast." + i + ".xp"), 0));
                }
            } catch (IOException ignored) {
                hasSave = false;
            }
        }

        void reset() {
            for (int i = 0; i < ROSTER.length; i++) {
                caught[i] = false;
                lessonUnlocked[i] = false;
                experience[i] = 0;
            }
            lastBeast = 0;
            bagLevel = 1;
            hasSave = false;
            storyComplete = false;
            java.util.Arrays.fill(completedStages, 0);
            trainerStyle = "male";
            save();
        }

        void selectTrainer(String id) {
            trainerStyle = TrainerProfile.fromId(id).id;
            hasSave = true;
            save();
        }

        boolean completeStage(int region, int stage) {
            if (region < 0 || region >= completedStages.length || stage < 1 || stage > 30) return false;
            boolean wasNew = stage > completedStages[region] && stage == completedStages[region] + 1;
            if (wasNew) completedStages[region] = stage;
            if (stage == 30 && completedStages[region] == 30) bagLevel = Math.min(5, Math.max(bagLevel, region + 2));
            hasSave = true;
            save();
            return wasNew;
        }

        int level(Beast beast) { return 1 + experience[indexOf(beast)] / 100; }
        int nextLevelXp(Beast beast) { return experience[indexOf(beast)] % 100; }
        void addExperience(Beast beast, int amount) {
            int i = indexOf(beast);
            experience[i] += Math.max(0, amount);
            lastBeast = i;
            hasSave = true;
            save();
        }
        void catchBeast(Beast beast) {
            int i = indexOf(beast);
            caught[i] = true;
            lessonUnlocked[i] = true;
            lastBeast = i;
            hasSave = true;
            save();
        }
        void chooseStarter(Beast beast) {
            int i = indexOf(beast);
            caught[i] = true;
            lessonUnlocked[i] = true;
            lastBeast = i;
            hasSave = true;
            save();
        }
        boolean hasAnyCaught() {
            for (boolean isCaught : caught) if (isCaught) return true;
            return false;
        }
        private void save() {
            Properties data = new Properties();
            data.setProperty("started", Boolean.toString(hasSave));
            data.setProperty("lastBeast", Integer.toString(lastBeast));
            data.setProperty("trainerStyle", trainerStyle);
            data.setProperty("storyComplete", Boolean.toString(storyComplete));
            data.setProperty("bagLevel", Integer.toString(bagLevel));
            for (int region = 0; region < completedStages.length; region++) {
                data.setProperty("region." + region + ".completed", Integer.toString(completedStages[region]));
            }
            for (int i = 0; i < ROSTER.length; i++) {
                data.setProperty("beast." + i + ".caught", Boolean.toString(caught[i]));
                data.setProperty("beast." + i + ".lesson", Boolean.toString(lessonUnlocked[i]));
                data.setProperty("beast." + i + ".xp", Integer.toString(experience[i]));
            }
            try (var output = Files.newOutputStream(file)) {
                data.store(output, "Echoes of Luma progress");
            } catch (IOException ignored) { }
        }
        private static int parseInt(String value, int fallback) {
            try { return Integer.parseInt(value); }
            catch (NumberFormatException ignored) { return fallback; }
        }
    }

    /** Draws a wrapped, built-in bitmap-font message in the stage result card. */
    private static final class PixelBitmapLabel extends JPanel {
        private String text;

        PixelBitmapLabel(String text) {
            this.text = text == null ? "" : text;
            setOpaque(false);
        }

        void setText(String text) {
            this.text = text == null ? "" : text;
            repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            PixelBitmapFont.drawWrapped(g, text, 8, 20, Math.max(1, getWidth() - 18),
                Math.max(1, (getHeight() - 24) / 18), 2, INK);
            g.dispose();
        }
    }

    /** Full roster picker shown before each progression encounter. */
    private final class BattlePartnerPanel extends JPanel {
        private final List<Beast> available;
        private final List<JPanel> cards = new ArrayList<>();
        private final List<JButton> chooseButtons = new ArrayList<>();
        private final JLabel selectionNote = new JLabel("Choose a caught Beast to enter the arena.");
        private final JButton beginButton = button("Choose a Beast", new Color(38, 105, 88));
        private final java.util.function.Consumer<Beast> onStart;
        private Beast selected;

        BattlePartnerPanel(List<Beast> available, String stageLabel, Beast lastPartner,
                java.util.function.Consumer<Beast> onStart, Runnable onCancel) {
            this.available = new ArrayList<>(available);
            this.onStart = onStart;
            setLayout(new BorderLayout(14, 14));
            setBackground(new Color(22, 43, 49));
            setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

            JLabel title = new JLabel("Choose Your Battle Partner", SwingConstants.CENTER);
            title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27));
            title.setForeground(new Color(251, 245, 219));
            JLabel stage = new JLabel(stageLabel, SwingConstants.CENTER);
            stage.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
            stage.setForeground(new Color(176, 216, 190));
            JPanel heading = new JPanel(new java.awt.GridLayout(2, 1, 0, 5));
            heading.setOpaque(false);
            heading.add(title);
            heading.add(stage);
            add(heading, BorderLayout.NORTH);

            JPanel rosterGrid = new JPanel(new java.awt.GridLayout(0, 4, 12, 12));
            rosterGrid.setBackground(new Color(34, 59, 62));
            rosterGrid.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            for (Beast beast : this.available) addBeastCard(rosterGrid, beast);
            JScrollPane rosterScroll = new JScrollPane(rosterGrid);
            rosterScroll.setBorder(BorderFactory.createLineBorder(new Color(103, 145, 126), 1));
            rosterScroll.getViewport().setBackground(new Color(34, 59, 62));
            rosterScroll.getVerticalScrollBar().setUnitIncrement(18);
            add(rosterScroll, BorderLayout.CENTER);

            JPanel footer = new JPanel(new BorderLayout(12, 0));
            footer.setOpaque(false);
            selectionNote.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            selectionNote.setForeground(new Color(225, 235, 219));
            if (lastPartner != null && this.available.contains(lastPartner)) {
                selectionNote.setText("Last partner: " + lastPartner.name + " · Select a Beast for this battle.");
            }
            footer.add(selectionNote, BorderLayout.CENTER);
            JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
            actions.setOpaque(false);
            JButton cancel = button("Back to Map", new Color(91, 108, 111));
            cancel.addActionListener(event -> onCancel.run());
            beginButton.setEnabled(false);
            beginButton.addActionListener(event -> {
                if (selected != null) this.onStart.accept(selected);
            });
            actions.add(cancel);
            actions.add(beginButton);
            footer.add(actions, BorderLayout.EAST);
            add(footer, BorderLayout.SOUTH);
        }

        private void addBeastCard(JPanel rosterGrid, Beast beast) {
            Color accent = typeColor(beast.firstType).darker();
            JPanel card = new JPanel(new BorderLayout(6, 6));
            card.setBackground(new Color(249, 247, 229));
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(159, 179, 149), 1, true),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
            JLabel name = new JLabel(beast.name, SwingConstants.CENTER);
            name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            name.setForeground(INK);
            JLabel details = new JLabel("Lv. " + SAVE.level(beast) + "  ·  " + beast.types().toUpperCase(Locale.ROOT), SwingConstants.CENTER);
            details.setFont(new Font(Font.MONOSPACED, Font.BOLD, 10));
            details.setForeground(accent);
            JPanel info = new JPanel(new java.awt.GridLayout(2, 1, 0, 2));
            info.setOpaque(false);
            info.add(name);
            info.add(details);

            JLabel artwork = new JLabel(new ImageIcon(containArtwork(beast.sprite, 136, 120)), SwingConstants.CENTER);
            artwork.setPreferredSize(new Dimension(150, 122));
            JButton choose = button("Choose", accent);
            choose.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            choose.addActionListener(event -> selectPartner(beast));
            cards.add(card);
            chooseButtons.add(choose);
            card.add(info, BorderLayout.NORTH);
            card.add(artwork, BorderLayout.CENTER);
            card.add(choose, BorderLayout.SOUTH);
            rosterGrid.add(card);
        }

        private java.awt.Image containArtwork(BufferedImage artwork, int maxWidth, int maxHeight) {
            if (artwork == null || artwork.getWidth() <= 0 || artwork.getHeight() <= 0) return artwork;
            double scale = Math.min(maxWidth / (double) artwork.getWidth(), maxHeight / (double) artwork.getHeight());
            int width = Math.max(1, (int) Math.round(artwork.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(artwork.getHeight() * scale));
            return artwork.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
        }

        private void selectPartner(Beast beast) {
            selected = beast;
            for (int i = 0; i < available.size(); i++) {
                boolean active = available.get(i) == selected;
                Color accent = typeColor(available.get(i).firstType).darker();
                cards.get(i).setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(active ? accent : new Color(159, 179, 149), active ? 3 : 1, true),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)));
                chooseButtons.get(i).setText(active ? "Selected" : "Choose");
            }
            selectionNote.setText("Ready to battle with " + beast.name + " · Lv. " + SAVE.level(beast) + " · " + beast.types());
            beginButton.setText("Battle with " + beast.name);
            beginButton.setEnabled(true);
        }
    }

    private final class MainMenu extends JPanel {
        final JButton newGame = menuButton("✦  New Game");
        final JButton continueGame = menuButton("▤  Continue");
        final JButton learnButton = menuButton("◈  Learnings");
        final JButton settings = menuButton("⚙  Settings");
        final JButton quit = menuButton("⇥  Quit");

        MainMenu() {
            setLayout(null);
            setFocusable(true);
            add(newGame);
            add(continueGame);
            add(learnButton);
            add(settings);
            add(quit);
        }

        void refreshButtons() {
            continueGame.setEnabled(SAVE.hasSave);
            learnButton.setEnabled(hasLessons());
        }

        private boolean hasLessons() {
            for (boolean unlocked : SAVE.lessonUnlocked) if (unlocked) return true;
            return false;
        }

        @Override public void doLayout() {
            double scale = Math.min(getWidth() / 960.0, getHeight() / 540.0);
            int ox = (int) ((getWidth() - 960 * scale) / 2);
            int oy = (int) ((getHeight() - 540 * scale) / 2);
            int x = ox + (int) (680 * scale);
            int width = (int) (230 * scale);
            int height = (int) (42 * scale);
            int gap = (int) (6 * scale);
            int y = oy + (int) (250 * scale);
            JButton[] buttons = {newGame, continueGame, learnButton, settings, quit};
            for (int i = 0; i < buttons.length; i++) {
                buttons[i].setBounds(x, y + i * (height + gap), width, height);
                buttons[i].setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(13, (int) (18 * scale))));
            }
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            double scale = Math.min(getWidth() / 960.0, getHeight() / 540.0);
            int drawW = (int) (960 * scale), drawH = (int) (540 * scale);
            int ox = (getWidth() - drawW) / 2, oy = (getHeight() - drawH) / 2;
            g.translate(ox, oy);
            g.scale(scale, scale);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            drawTitleWorld(g);
            drawTitleTrainer(g, 155, 312);
            Beast titleBeast = ROSTER[Math.max(0, Math.min(ROSTER.length - 1, SAVE.lastBeast))];
            BeastIllustrations.draw(g, titleBeast.sprite, 262, 381, 112, 112, 1f, false);

            g.setFont(new Font(Font.SERIF, Font.BOLD, 58));
            g.setColor(new Color(8, 45, 87, 190));
            g.drawString("ECHOES", 62, 105);
            g.drawString("OF LUMA", 62, 166);
            g.setColor(new Color(243, 252, 255));
            g.drawString("ECHOES", 58, 101);
            g.drawString("OF LUMA", 58, 162);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 19));
            g.setColor(new Color(11, 53, 90));
            g.drawString("BEAST TRAINER ADVENTURE", 74, 196);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 17));
            g.setColor(Color.WHITE);
            g.drawString("CATCH. LEARN. GROW.", 692, 220);
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            g.drawString("Be a better you.", 744, 243);
            g.dispose();
        }

        private void drawTitleWorld(Graphics2D g) {
            g.setPaint(new GradientPaint(0, 0, new Color(51, 157, 231), 0, 540, new Color(198, 232, 242)));
            g.fillRect(0, 0, 960, 540);
            g.setColor(new Color(255, 241, 190, 75)); g.fillOval(570, -120, 430, 330);
            drawMenuCloud(g, 530, 86, 1.12); drawMenuCloud(g, 798, 139, .82); drawMenuCloud(g, 361, 123, .7);

            Path2D backMountains = new Path2D.Double();
            backMountains.moveTo(0, 318); backMountains.lineTo(0, 277); backMountains.lineTo(122, 186);
            backMountains.lineTo(192, 236); backMountains.lineTo(307, 146); backMountains.lineTo(408, 257);
            backMountains.lineTo(520, 183); backMountains.lineTo(628, 272); backMountains.lineTo(754, 160);
            backMountains.lineTo(865, 243); backMountains.lineTo(960, 175); backMountains.lineTo(960, 366); backMountains.closePath();
            g.setPaint(new GradientPaint(0, 146, new Color(157, 207, 224), 0, 360, new Color(113, 172, 195)));
            g.fill(backMountains);
            Path2D frontMountains = new Path2D.Double();
            frontMountains.moveTo(0, 362); frontMountains.lineTo(0, 314); frontMountains.lineTo(112, 257);
            frontMountains.lineTo(210, 300); frontMountains.lineTo(331, 226); frontMountains.lineTo(448, 322);
            frontMountains.lineTo(559, 263); frontMountains.lineTo(669, 321); frontMountains.lineTo(789, 232);
            frontMountains.lineTo(899, 296); frontMountains.lineTo(960, 260); frontMountains.lineTo(960, 389); frontMountains.closePath();
            g.setPaint(new GradientPaint(0, 220, new Color(89, 156, 187), 0, 389, new Color(59, 125, 165)));
            g.fill(frontMountains);
            g.setColor(new Color(231, 247, 246, 185));
            g.drawLine(122, 186, 192, 236); g.drawLine(307, 146, 408, 257); g.drawLine(754, 160, 865, 243);

            Path2D island = new Path2D.Double();
            island.moveTo(393, 340); island.curveTo(435, 319, 477, 311, 515, 318);
            island.curveTo(562, 309, 617, 328, 657, 350); island.lineTo(696, 386);
            island.lineTo(664, 416); island.lineTo(405, 416); island.lineTo(370, 390); island.closePath();
            g.setPaint(new GradientPaint(0, 328, new Color(151, 127, 103), 0, 421, new Color(73, 88, 76)));
            g.fill(island);
            g.setColor(new Color(45, 111, 124, 150)); g.draw(new QuadCurve2D.Double(371, 397, 515, 420, 687, 392));
            drawCastleTower(g, 443, 226, 0.68);
            drawCastleTower(g, 612, 222, 0.74);
            drawCastleTower(g, 486, 174, 1.0);
            g.setPaint(new GradientPaint(463, 239, new Color(246, 230, 186), 463, 379, new Color(187, 181, 151)));
            g.fillRoundRect(454, 237, 191, 143, 10, 10);
            g.setColor(new Color(255, 244, 211)); g.fillRoundRect(448, 231, 203, 16, 9, 9);
            g.setColor(new Color(213, 199, 160)); g.fillRoundRect(470, 270, 48, 111, 5, 5); g.fillRoundRect(579, 270, 47, 111, 5, 5);
            g.setColor(new Color(30, 92, 138)); g.fillRoundRect(529, 280, 43, 101, 18, 18);
            g.setColor(new Color(176, 232, 241, 190)); g.drawRoundRect(536, 289, 29, 87, 13, 13);
            g.setColor(new Color(255, 222, 132)); g.setStroke(new BasicStroke(2));
            g.drawOval(533, 307, 35, 35); g.drawOval(541, 315, 19, 19);

            Path2D farBank = new Path2D.Double();
            farBank.moveTo(0, 400); farBank.curveTo(168, 374, 295, 407, 405, 441);
            farBank.curveTo(519, 467, 671, 418, 960, 392); farBank.lineTo(960, 540); farBank.lineTo(0, 540); farBank.closePath();
            g.setPaint(new GradientPaint(0, 395, new Color(37, 119, 110), 0, 540, new Color(13, 77, 112)));
            g.fill(farBank);
            Path2D river = new Path2D.Double();
            river.moveTo(313, 540); river.curveTo(378, 495, 487, 475, 572, 452);
            river.curveTo(666, 426, 694, 443, 771, 418); river.curveTo(828, 397, 895, 409, 960, 393);
            river.lineTo(960, 540); river.closePath();
            g.setPaint(new GradientPaint(0, 430, new Color(66, 184, 226), 0, 540, new Color(31, 125, 192)));
            g.fill(river);
            g.setColor(new Color(185, 239, 243, 155)); g.setStroke(new BasicStroke(3));
            g.draw(new QuadCurve2D.Double(513, 493, 687, 437, 831, 427));
            g.draw(new QuadCurve2D.Double(655, 524, 803, 478, 938, 451));

            Path2D meadow = new Path2D.Double();
            meadow.moveTo(0, 417); meadow.curveTo(154, 395, 272, 424, 385, 455);
            meadow.curveTo(460, 478, 503, 481, 555, 466); meadow.lineTo(427, 540); meadow.lineTo(0, 540); meadow.closePath();
            g.setPaint(new GradientPaint(0, 403, new Color(115, 179, 78), 0, 540, new Color(51, 132, 67)));
            g.fill(meadow);
            g.setColor(new Color(236, 210, 151));
            Path2D path = new Path2D.Double(); path.moveTo(173, 540); path.curveTo(245, 492, 337, 469, 400, 457);
            path.curveTo(475, 440, 521, 439, 579, 455); path.curveTo(530, 460, 490, 475, 448, 502);
            path.lineTo(360, 540); path.closePath(); g.fill(path);

            for (int i = 0; i < 18; i++) {
                int x = i < 9 ? i * 42 - 34 : 786 + (i - 9) * 42;
                int y = 253 + (i % 4) * 19;
                drawMenuTree(g, x, y, 45 + i % 3 * 8);
            }
            Random landscape = new Random(22);
            for (int i = 0; i < 70; i++) {
                int x = landscape.nextInt(960), y = 420 + landscape.nextInt(115);
                g.setColor(i % 4 == 0 ? new Color(251, 238, 184, 190) : new Color(163, 212, 113, 130));
                g.fillOval(x, y, 4 + i % 3 * 2, 2 + i % 2 * 2);
            }
        }

        private void drawMenuCloud(Graphics2D g, int x, int y, double scale) {
            Graphics2D cloud = (Graphics2D) g.create(); cloud.translate(x, y); cloud.scale(scale, scale);
            cloud.setColor(new Color(255, 255, 255, 220));
            cloud.fillOval(0, 16, 124, 34); cloud.fillOval(24, 0, 51, 44); cloud.fillOval(61, 7, 56, 40);
            cloud.setColor(new Color(255, 255, 255, 98)); cloud.drawArc(23, 8, 66, 38, 0, 175);
            cloud.dispose();
        }

        private void drawCastleTower(Graphics2D g, int x, int y, double scale) {
            Graphics2D tower = (Graphics2D) g.create(); tower.translate(x, y); tower.scale(scale, scale);
            tower.setColor(new Color(210, 209, 188)); tower.fillRoundRect(0, 24, 38, 113, 5, 5);
            tower.setColor(new Color(248, 233, 198)); tower.fillRoundRect(5, 29, 28, 107, 4, 4);
            tower.setColor(new Color(133, 104, 105));
            tower.fillPolygon(new int[] {-5, 19, 43}, new int[] {31, -17, 31}, 3);
            tower.setColor(new Color(247, 226, 183)); tower.fillPolygon(new int[] {2, 19, 36}, new int[] {27, -6, 27}, 3);
            tower.setColor(new Color(50, 112, 154)); tower.fillRoundRect(14, 63, 11, 29, 8, 8);
            tower.setColor(new Color(255, 250, 217)); tower.drawLine(19, 2, 19, -15); tower.drawLine(19, -15, 27, -8);
            tower.dispose();
        }

        private void drawMenuTree(Graphics2D g, int x, int y, int size) {
            g.setPaint(new GradientPaint(x + size / 2, y + 15, new Color(115, 86, 57), x + size / 2, y + size * 2, new Color(73, 71, 48)));
            g.fillRoundRect(x + size / 2 - 5, y + 18, 11, size * 2, 6, 6);
            Color[] greens = {new Color(27, 92, 55), new Color(47, 125, 62), new Color(82, 157, 73)};
            int[][] blobs = {{0, 16, 36}, {18, 0, 40}, {35, 17, 42}, {7, 34, 38}, {29, 39, 38}};
            for (int i = 0; i < blobs.length; i++) {
                int bx = x + blobs[i][0] * size / 58, by = y + blobs[i][1] * size / 58, d = blobs[i][2] * size / 58;
                g.setPaint(new GradientPaint(bx, by, greens[(i + 1) % greens.length], bx + d, by + d, greens[i % greens.length]));
                g.fillOval(bx, by, d, d * 3 / 4);
            }
        }

        private void drawTitleTrainer(Graphics2D g, int x, int y) {
            if (activeTrainer.art != null) {
                g.setColor(new Color(24, 68, 63, 95));
                g.fillOval(x - 4, y + 164, 92, 15);
                BeastIllustrations.draw(g, activeTrainer.art, x - 8, y - 2, 100, 170, 1f, false);
                return;
            }
            g.setColor(new Color(24, 68, 63, 95)); g.fillOval(x + 1, y + 166, 83, 16);
            g.setPaint(new GradientPaint(x + 8, y + 60, new Color(36, 48, 67), x + 69, y + 145, new Color(16, 29, 50)));
            g.fillRoundRect(x + 13, y + 111, 23, 51, 11, 11); g.fillRoundRect(x + 43, y + 111, 23, 51, 11, 11);
            g.setColor(new Color(23, 42, 67)); g.fillRoundRect(x + 9, y + 153, 30, 14, 10, 10); g.fillRoundRect(x + 40, y + 153, 30, 14, 10, 10);
            g.setColor(new Color(237, 235, 221)); g.fillRoundRect(x + 8, y + 161, 33, 9, 8, 8); g.fillRoundRect(x + 39, y + 161, 33, 9, 8, 8);
            g.setPaint(new GradientPaint(x + 5, y + 53, new Color(61, 124, 200), x + 77, y + 128, new Color(22, 72, 137)));
            g.fillRoundRect(x + 6, y + 61, 67, 63, 22, 22);
            g.setColor(new Color(91, 158, 216)); g.setStroke(new BasicStroke(3)); g.drawLine(x + 40, y + 72, x + 40, y + 116);
            g.setColor(new Color(105, 62, 43)); g.fillRoundRect(x + 46, y + 71, 29, 46, 9, 9);
            g.setColor(new Color(150, 91, 56)); g.fillRoundRect(x + 49, y + 90, 27, 30, 8, 8);
            g.setColor(new Color(232, 183, 145)); g.fillRoundRect(x + 27, y + 30, 36, 42, 15, 15);
            g.setColor(new Color(59, 42, 36));
            g.fill(new Arc2D.Double(x + 23, y + 14, 46, 47, 0, 180, Arc2D.PIE));
            g.fillRoundRect(x + 23, y + 36, 12, 20, 8, 8);
            g.setColor(new Color(193, 145, 112)); g.fillOval(x + 21, y + 43, 10, 13);
            g.setColor(new Color(255, 228, 190, 170)); g.drawArc(x + 34, y + 20, 27, 21, 15, 120);
        }
    }

    private final class TrainerSelectionPanel extends JPanel {
        private static final int PAGE_SIZE = 9;
        private static final int CARD_WIDTH = 270;
        private static final int CARD_HEIGHT = 98;
        final JButton[] choiceButtons = new JButton[PAGE_SIZE];
        final JButton backButton = menuButton("‹  Back");

        TrainerSelectionPanel() {
            setLayout(null);
            TrainerProfile[] profiles = TrainerProfile.values();
            for (int i = 0; i < choiceButtons.length; i++) {
                final int slot = i;
                choiceButtons[i] = menuButton("CHOOSE");
                choiceButtons[i].setToolTipText(profiles[slot].title);
                choiceButtons[i].addActionListener(e -> beginWithTrainer(profiles[slot]));
                add(choiceButtons[i]);
            }
            add(backButton);
        }

        @Override public void doLayout() {
            double scale = Math.min(getWidth() / 960.0, getHeight() / 540.0);
            int ox = (int) ((getWidth() - 960 * scale) / 2);
            int oy = (int) ((getHeight() - 540 * scale) / 2);
            int buttonHeight = Math.max(22, (int) (25 * scale));
            for (int slot = 0; slot < choiceButtons.length; slot++) {
                int column = slot % 3, row = slot / 3;
                int x = 64 + column * 280;
                int y = 112 + row * 108;
                choiceButtons[slot].setBounds(ox + (int) ((x + 168) * scale), oy + (int) ((y + 66) * scale),
                    Math.max(68, (int) (88 * scale)), buttonHeight);
                choiceButtons[slot].setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(9, (int) (11 * scale))));
            }
            backButton.setBounds(ox + (int) (30 * scale), oy + (int) (24 * scale),
                (int) (136 * scale), Math.max(32, (int) (38 * scale)));
            backButton.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(10, (int) (12 * scale))));
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            double scale = Math.min(getWidth() / 960.0, getHeight() / 540.0);
            int drawW = (int) (960 * scale), drawH = (int) (540 * scale);
            int ox = (getWidth() - drawW) / 2, oy = (getHeight() - drawH) / 2;
            g.translate(ox, oy);
            g.scale(scale, scale);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            g.setPaint(new GradientPaint(0, 0, new Color(34, 98, 155), 0, 540, new Color(169, 224, 199)));
            g.fillRect(0, 0, 960, 540);
            g.setColor(new Color(255, 244, 197, 115));
            g.fillOval(747, 15, 150, 150);
            g.setColor(new Color(255, 255, 255, 85));
            g.fillOval(95, 93, 34, 34);
            g.fillOval(835, 216, 22, 22);
            g.fillOval(472, 66, 16, 16);

            Path2D distantHill = new Path2D.Double();
            distantHill.moveTo(0, 339);
            distantHill.curveTo(140, 255, 255, 315, 384, 279);
            distantHill.curveTo(543, 230, 672, 321, 960, 253);
            distantHill.lineTo(960, 540);
            distantHill.lineTo(0, 540);
            distantHill.closePath();
            g.setColor(new Color(40, 120, 101, 185));
            g.fill(distantHill);

            Path2D foreground = new Path2D.Double();
            foreground.moveTo(0, 396);
            foreground.curveTo(165, 331, 324, 398, 482, 360);
            foreground.curveTo(672, 314, 814, 384, 960, 325);
            foreground.lineTo(960, 540);
            foreground.lineTo(0, 540);
            foreground.closePath();
            g.setPaint(new GradientPaint(0, 350, new Color(49, 143, 92), 0, 540, new Color(20, 92, 72)));
            g.fill(foreground);

            g.setColor(new Color(13, 48, 75, 190));
            g.fillRoundRect(54, 44, 852, 435, 34, 34);
            g.setPaint(new GradientPaint(54, 44, new Color(249, 252, 247), 906, 479, new Color(226, 240, 229)));
            g.fillRoundRect(50, 39, 852, 435, 34, 34);
            g.setStroke(new BasicStroke(2.5f));
            g.setColor(new Color(52, 112, 107));
            g.drawRoundRect(50, 39, 852, 435, 34, 34);

            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 29));
            g.setColor(new Color(22, 55, 78));
            drawCentered(g, "CHOOSE YOUR TRAINER", 480, 76);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g.setColor(new Color(76, 101, 111));
            drawCentered(g, "All 9 character designs from your reference images · full body", 480, 98);

            TrainerProfile[] profiles = TrainerProfile.values();
            for (int slot = 0; slot < profiles.length; slot++) {
                int x = 64 + (slot % 3) * 280;
                int y = 112 + (slot / 3) * 108;
                drawTrainerCard(g, profiles[slot], slot, x, y);
            }
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            g.setColor(new Color(55, 83, 90));
            drawCentered(g, "SELECT A TRAINER TO BEGIN YOUR JOURNEY", 480, 460);
            g.dispose();
        }

        private void drawTrainerCard(Graphics2D g, TrainerProfile profile, int index, int x, int y) {
            Color accent = profile.accent;
            g.setColor(new Color(22, 57, 78, 24));
            g.fillRoundRect(x + 3, y + 4, CARD_WIDTH, CARD_HEIGHT, 15, 15);
            g.setColor(Color.WHITE);
            g.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 15, 15);
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 180));
            g.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 15, 15);
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 24));
            g.fillRoundRect(x + 5, y + 5, 70, CARD_HEIGHT - 10, 12, 12);
            if (profile.art != null) BeastIllustrations.draw(g, profile.art, x + 8, y + 4, 62, 90, 1f, false);
            else drawTrainerPortraitFallback(g, profile, x + 8, y + 4, 62, 90);

            g.setColor(accent.darker());
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            g.drawString(profile.label.toUpperCase(Locale.ROOT) + "  " + String.format(Locale.ROOT, "%02d", index + 1), x + 80, y + 25);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            g.setColor(new Color(36, 52, 67));
            g.drawString(profile.title, x + 80, y + 48);
            g.setColor(new Color(94, 111, 124));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            g.drawString("Full-body trainer art", x + 80, y + 68);
        }

        private void drawTrainerPortraitFallback(Graphics2D g, TrainerProfile profile, int x, int y, int width, int height) {
            int center = x + width / 2;
            g.setColor(new Color(30, 44, 60));
            g.fillOval(center - 15, y + 3, 30, 31);
            g.setColor(new Color(230, 192, 161));
            g.fillOval(center - 11, y + 9, 22, 25);
            g.setColor(profile.label.equals("Female") ? new Color(65, 43, 56) : new Color(42, 47, 60));
            g.fillArc(center - 14, y + 2, 28, 24, 0, 180);
            g.setPaint(new GradientPaint(x + 5, y + 36, profile.accent.brighter(), x + width - 5, y + 76, profile.accent.darker()));
            g.fillRoundRect(x + 6, y + 34, width - 12, height - 36, 18, 18);
            g.setColor(new Color(34, 50, 68));
            g.fillRoundRect(x + 13, y + 66, 13, 22, 8, 8);
            g.fillRoundRect(x + width - 26, y + 66, 13, 22, 8, 8);
        }

        private void drawCentered(Graphics2D g, String text, int centerX, int baselineY) {
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, centerX - fm.stringWidth(text) / 2, baselineY);
        }
    }
    private static JButton menuButton(String label) {
        JButton button = new JButton(label);
        button.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(7, 47, 83));
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(91, 224, 249), 3),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        return button;
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
        JButton explore = button("Explore World", new Color(45, 119, 91));
        explore.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        explore.addActionListener(e -> openWorldMap("game"));
        JButton home = button("Main Menu", new Color(57, 81, 107));
        home.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        home.addActionListener(e -> showScreen("title"));
        hint.setFont(UI_SMALL);
        hint.setForeground(MUTED);
        hint.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        right.add(Box.createVerticalGlue());
        right.add(selectPreview);
        right.add(Box.createVerticalStrut(12));
        right.add(open);
        right.add(Box.createVerticalStrut(8));
        right.add(explore);
        right.add(Box.createVerticalStrut(8));
        right.add(home);
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
        if (SAVE.hasAnyCaught() && !SAVE.caught[indexOf(player)]) {
            hint.setText("That beast is not in your capsules yet. Catch a wild beast to unlock it.");
            return;
        }
        if (player == enemy) {
            enemy = ROSTER[(indexOf(enemy) + 1) % ROSTER.length];
            enemyChoice.setSelectedItem(enemy);
            hint.setText("Choose a different beast as your opponent.");
            return;
        }
        arena.player = player;
        arena.enemy = enemy;
        if (!SAVE.hasAnyCaught()) SAVE.chooseStarter(player);
        arena.player.maxHp = 100 + (SAVE.level(player) - 1) * 10;
        arena.player.level = SAVE.level(player);
        if (arena.progressionBattle) {
            arena.enemy.level = Math.max(1, SAVE.level(player) + (arena.stageNumber - 1) / 6 + (arena.bossBattle ? 2 : 0));
        } else {
            arena.enemy.level = 1 + arena.random.nextInt(3);
        }
        arena.enemy.maxHp = 100 + (arena.enemy.level - 1) * 10;
        if (arena.bossBattle) arena.enemy.maxHp = (int) Math.round(arena.enemy.maxHp * 1.35);
        arena.player.hp = arena.player.maxHp;
        arena.enemy.hp = arena.enemy.maxHp;
        arena.player.buffTurns = arena.player.weakenedTurns = 0;
        arena.enemy.buffTurns = arena.enemy.weakenedTurns = 0;
        arena.attunementBonus = false;
        arena.attunementEffect = false;
        arena.captureBreakEffect = false;
        arena.enemyRemoved = false;
        arena.enteredEcho.clear();
        arena.pendingEnemyMove = null;
        arena.enemyTurnPendingAfterCapture = false;
        arena.battleRewardGranted = false;
        arena.ethicsQuestionsAnswered = 0;
        arena.resultMessage = "";
        arena.lastQuestionIndex = -1;
        arena.lastBossQuestionIndex = -1;
        arena.pendingMove = null;
        arena.currentQuestion = null;
        arena.potions = 2;
        updateBagLabel();
        arena.trainerBattle = !arena.progressionBattle && modeChoice.getSelectedIndex() == 1;
        if (!arena.progressionBattle) arena.bossBattle = false;
        if (arena.trainerBattle) {
            TrainerProfile[] opponents = TrainerProfile.values();
            int opponentIndex = arena.random.nextInt(opponents.length);
            if (opponents[opponentIndex] == activeTrainer) opponentIndex = (opponentIndex + 1) % opponents.length;
            arena.opponentTrainer = opponents[opponentIndex];
        }
        arena.phase = Phase.THROWING_CAPSULE;
        arena.recallAfterThrow = false;
        arena.frameNo = 0;
        arena.message = "{C:#2C765B}{PLAYER}{/C} throws out {ALLY}!";
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
            captureButton.setToolTipText("Choose one of eight capsules; type affinity changes the wild catch chance.");
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
        selectPreview.setText("CAPSULE  ·  " + beast.name.toUpperCase(Locale.ROOT) + "  ·  LV " + SAVE.level(beast));
        arena.preview = beast;
        arena.repaint();
    }

    private void updateSelectionHint() {
        Beast selected = (Beast) playerChoice.getSelectedItem();
        if (selected == null) return;
        if (!SAVE.hasAnyCaught()) hint.setText("Choose your first beast. It becomes your starting companion.");
        else if (!SAVE.caught[indexOf(selected)]) hint.setText("Wild beast: catch it in battle before it can join your capsules.");
        else hint.setText("Ready to battle. Earn XP after winning a battle or catching a wild beast.");
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
        arena.pendingMove = arena.player.moves.get(slot);
        arena.currentQuestion = arena.nextEthicsQuestion();
        arena.phase = Phase.ETHICS_QUESTION;
        arena.menuFocus = 0;
        setActionsEnabled(false);
        arena.requestFocusInWindow();
        arena.repaint();
    }

    private void usePotion() {
        if (arena.phase != Phase.COMMAND) return;
        if (arena.potions <= 0) { arena.message = "Your bag is empty."; arena.repaint(); return; }
        if (arena.player.hp >= arena.player.maxHp) { arena.message = arena.player.name + " is already at full health."; arena.repaint(); return; }
        arena.potions--;
        updateBagLabel();
        arena.player.hp = Math.min(arena.player.maxHp, arena.player.hp + 32);
        arena.effectKind = Kind.BUFF; arena.effectActor = arena.player; arena.phase = Phase.ANIMATING; arena.frameNo = 0;
        arena.message = "Potion restored 32 HP. " + arena.potions + " left.";
        setActionsEnabled(false);
        arena.requestFocusInWindow();
        arena.repaint();
    }

    private void runAway() {
        if (arena.phase != Phase.COMMAND) return;
        arena.completeBattle(0, "battle", "You escaped the encounter.");
        arena.startRecallThrow();
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
        arena.phase = Phase.CAPSULE_SELECT;
        arena.frameNo = 0;
        arena.menuFocus = 0;
        arena.capsulePickerNotice = "";
        arena.message = "Choose a cylindrical capsule. Its type changes the catch chance.";
        setActionsEnabled(false);
        arena.requestFocusInWindow();
        arena.repaint();
    }

    private void throwCapsule(CapsuleType capsuleType) {
        if (arena.phase != Phase.CAPSULE_SELECT || !arena.canCapture()) return;
        if (!capsuleType.isUnlocked()) {
            arena.capsulePickerNotice = capsuleType.unlockRequirement() + " TO UNLOCK " + capsuleType.label.toUpperCase(Locale.ROOT);
            arena.repaint();
            return;
        }
        arena.capsulePickerNotice = "";
        arena.selectedCapsule = capsuleType;
        arena.phase = Phase.ANIMATING; arena.frameNo = 0; arena.capturing = true;
        double chance = arena.captureChance(capsuleType);
        arena.captureChanceAtThrow = (int) Math.round(chance * 100);
        arena.captureSuccess = arena.random.nextDouble() < chance;
        arena.attunementBonus = false;
        arena.captureBreakEffect = false;
        arena.enemyTurnPendingAfterCapture = false;
        arena.message = "{C:#2C765B}{PLAYER}{/C} throws a {C:#B56839}{CAPSULE}{/C} capsule · {CHANCE}% catch chance.";
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

    private final class Arena extends JPanel {
        Beast player;
        Beast enemy;
        TrainerProfile opponentTrainer = TrainerProfile.MALE_DARK;
        Beast preview = ROSTER[0];
        Move move;
        Kind effectKind;
        Beast effectActor;
        Phase phase = Phase.SELECT;
        String message = "Select a capsule to begin.";
        boolean trainerBattle;
        boolean capturing;
        boolean recallAfterThrow;
        boolean captureSuccess;
        int captureChanceAtThrow;
        CapsuleType selectedCapsule = CapsuleType.FIELD;
        boolean attunementBonus;
        boolean attunementEffect;
        boolean captureBreakEffect;
        boolean enemyTurnPendingAfterCapture;
        boolean enemyRemoved;
        Move pendingEnemyMove;
        Move pendingMove;
        EthicsQuestion currentQuestion;
        int lastQuestionIndex = -1;
        int lastBossQuestionIndex = -1;
        int ethicsQuestionsAnswered;
        boolean ethicsCorrect;
        boolean battleRewardGranted;
        boolean progressionBattle;
        boolean bossBattle;
        int stageRegion;
        int stageNumber;
        String resultMessage = "";
        private final List<EchoSymbol> enteredEcho = new ArrayList<>(3);
        int potions = 2;
        int frameNo;
        int tick;
        int menuFocus;
        String capsulePickerNotice = "";
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
                    if (phase == Phase.CAPSULE_SELECT) {
                        int key = e.getKeyCode();
                        if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_8) {
                            throwCapsule(CapsuleType.values()[key - KeyEvent.VK_1]);
                        } else if (key >= KeyEvent.VK_NUMPAD1 && key <= KeyEvent.VK_NUMPAD8) {
                            throwCapsule(CapsuleType.values()[key - KeyEvent.VK_NUMPAD1]);
                        } else if (key == KeyEvent.VK_ESCAPE) {
                            phase = Phase.COMMAND;
                            setActionsEnabled(true);
                            repaint();
                        }
                    } else if (phase == Phase.ETHICS_QUESTION) {
                        int key = e.getKeyCode();
                        if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_3) answerEthics(key - KeyEvent.VK_1);
                        else if (key >= KeyEvent.VK_NUMPAD1 && key <= KeyEvent.VK_NUMPAD3) answerEthics(key - KeyEvent.VK_NUMPAD1);
                    } else if (phase == Phase.ETHICS_FEEDBACK && (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_Z)) {
                        continueAfterEthics();
                    } else if (isDialogActive() && (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_Z)) {
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
            return phase == Phase.THROWING_CAPSULE || phase == Phase.SUMMONING || phase == Phase.ANIMATING || phase == Phase.RECALL_PLAYER
                || phase == Phase.RECALL_ENEMY || phase == Phase.ACTION_NOTICE || phase == Phase.RESULT;
        }

        private String dialogText() {
            if (phase == Phase.SUMMONING && player != null) return "Capsule opens. Releasing " + player.name + "...";
            return message == null ? "" : message;
        }

        private void syncDialog() {
            String text = resolveDialogTokens(dialogText());
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

        private double captureChance(CapsuleType capsuleType) {
            if (enemy == null) return 0;
            double chance = 0.20 + (100 - enemy.hp) * 0.005;
            if (enemy.weakenedTurns > 0) chance += 0.10;
            if (attunementBonus) chance += 0.25;
            chance += capsuleType.bonusAgainst(enemy);
            return Math.max(0.05, Math.min(0.95, chance));
        }

        private String moveCategory(Kind kind) {
            return switch (kind) {
                case DAMAGE -> "an ATTACK";
                case BUFF -> "a SELF-BUFF";
                case DEBUFF -> "a DEBUFF";
            };
        }

        private EthicsQuestion nextEthicsQuestion() {
            List<EthicsQuestion> questionSet = bossBattle ? BOSS_ETHICS_QUESTIONS : ETHICS_QUESTIONS;
            int previous = bossBattle ? lastBossQuestionIndex : lastQuestionIndex;
            int index = random.nextInt(questionSet.size());
            if (index == previous) index = (index + 1) % questionSet.size();
            if (bossBattle) lastBossQuestionIndex = index;
            else lastQuestionIndex = index;
            return questionSet.get(index);
        }

        private void answerEthics(int answerIndex) {
            if (phase != Phase.ETHICS_QUESTION || currentQuestion == null) return;
            ethicsCorrect = answerIndex == currentQuestion.correctIndex;
            ethicsQuestionsAnswered++;
            phase = Phase.ETHICS_FEEDBACK;
            frameNo = 0;
            menuFocus = answerIndex;
            repaint();
        }

        private void continueAfterEthics() {
            if (phase != Phase.ETHICS_FEEDBACK || pendingMove == null) return;
            if (ethicsCorrect) {
                move = pendingMove;
                pendingMove = null;
                effectActor = player;
                effectKind = null;
                phase = Phase.ANIMATING;
                frameNo = 0;
                message = "{C:#2C765B}{ALLY}{/C} used {MOVE}!";
                setActionsEnabled(false);
            } else {
                pendingMove = null;
                currentQuestion = null;
                enemyTurn();
            }
            repaint();
        }

        private void syncPlayerProgress(int oldMaxHp) {
            if (player == null) return;
            player.level = SAVE.level(player);
            player.maxHp = 100 + (player.level - 1) * 10;
            if (player.maxHp > oldMaxHp) player.hp += player.maxHp - oldMaxHp;
            player.hp = Math.min(player.maxHp, player.hp);
        }

        private void startRecallThrow() {
            recallAfterThrow = true;
            phase = Phase.THROWING_CAPSULE;
            frameNo = 0;
            message = "{C:#2C765B}{PLAYER}{/C} throws a capsule to recall {ALLY}!";
            setActionsEnabled(false);
        }

        private void completeBattle(int reward, String rewardKind, String outcome) {
            if (battleRewardGranted || player == null) return;
            battleRewardGranted = true;
            int oldLevel = SAVE.level(player);
            int oldMaxHp = player.maxHp;
            if (reward > 0) SAVE.addExperience(player, reward);
            syncPlayerProgress(oldMaxHp);
            String stageText = "";
            if (reward > 0 && progressionBattle) {
                SAVE.completeStage(stageRegion, stageNumber);
                if (stageNumber == 30 && SAVE.completedStages[stageRegion] >= 30) {
                    if (stageRegion + 1 < SAVE.completedStages.length) {
                        stageText = " Region cleared! " + new String[] {"Luma Village", "Whispering Forest", "Ember Mountain", "Ethics Academy"}[stageRegion] + " is now unlocked.";
                    } else {
                        stageText = " All regions are open. You have completed the Luma journey!";
                    }
                    SAVE.bagLevel = Math.min(5, Math.max(SAVE.bagLevel, stageRegion + 2));
                    SAVE.save();
                } else {
                    stageText = " Stage " + (stageRegion + 1) + "." + stageNumber + " cleared.";
                }
            }
            String levelText = player.level > oldLevel ? " Level up! " + player.name + " is now Lv. " + player.level + "." : "";
            String rewardText = reward > 0 ? " +" + reward + " " + rewardKind + " XP." : " No XP earned.";
            resultMessage = outcome + "{BR}" + rewardText + levelText
                + (stageText.isBlank() ? "" : "{BR}" + stageText.trim());
            message = resultMessage;
        }

        private void finishToResult() {
            if (player != null && player.hp <= 0) player.hp = player.maxHp;
            phase = Phase.RESULT;
            frameNo = 0;
            message = resultMessage;
            pendingMove = null;
            if (progressionBattle) {
                String regionName = new String[] {"Starting Forest", "Luma Village", "Whispering Forest", "Ember Mountain", "Ethics Academy"}[stageRegion];
                stageResultText.setText(resultMessage + "{BR}{BR}" + regionName + " · Stage "
                    + (stageRegion + 1) + "." + stageNumber + (bossBattle ? " · Boss" : ""));
                ((java.awt.CardLayout) cards.getLayout()).show(cards, "stage-result");
            } else {
                ((java.awt.CardLayout) cards.getLayout()).show(cards, "select");
            }
            cards.setVisible(true);
            setActionsEnabled(false);
            hint.setText(resultMessage);
            playerChoice.repaint();
            updateSelectedPreview();
            frame.revalidate();
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
            if (phase == Phase.ETHICS_QUESTION && point[0] >= 48 && point[0] <= 627 && point[1] >= 439 && point[1] <= 526) {
                menuFocus = Math.min(2, Math.max(0, (point[1] - 439) / 30));
                repaint();
            } else if (phase == Phase.CAPSULE_SELECT && point[0] >= 48 && point[0] <= 912 && point[1] >= 390 && point[1] <= 506) {
                int col = Math.min(3, Math.max(0, (point[0] - 48) / 216));
                int row = Math.min(1, Math.max(0, (point[1] - 390) / 62));
                menuFocus = row * 4 + col;
                repaint();
            } else if (phase == Phase.COMMAND && point[0] >= 20 && point[0] <= 430 && point[1] >= 396 && point[1] <= 526) {
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
            if (phase == Phase.ETHICS_QUESTION && x >= 48 && x <= 627 && y >= 439 && y <= 526) {
                int answer = Math.min(2, Math.max(0, (y - 439) / 30));
                answerEthics(answer);
            } else if (phase == Phase.ETHICS_FEEDBACK && x >= 32 && x <= 928 && y >= 350 && y <= 530) {
                continueAfterEthics();
            } else if (phase == Phase.CAPSULE_SELECT && x >= 48 && x <= 912 && y >= 390 && y <= 506) {
                int col = Math.min(3, Math.max(0, (x - 48) / 216));
                int row = Math.min(1, Math.max(0, (y - 390) / 62));
                int choice = row * 4 + col;
                menuFocus = choice;
                throwCapsule(CapsuleType.values()[choice]);
            } else if (phase == Phase.COMMAND && x >= 20 && x <= 430 && y >= 396 && y <= 526) {
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
            double scale = Math.min(getWidth() / (double) W, getHeight() / (double) H);
            double ox = (getWidth() - W * scale) / 2.0;
            double oy = (getHeight() - H * scale) / 2.0;
            int vx = (int) Math.round((mouseX - ox) / scale);
            int vy = (int) Math.round((mouseY - oy) / scale);
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
                    && phase != Phase.CAPSULE_SELECT && phase != Phase.ETHICS_QUESTION && phase != Phase.ETHICS_FEEDBACK
                        && phase != Phase.ATTUNE_INPUT && phase != Phase.RESULT) {
                    frameNo++;
                    advanceAnimation();
                }
                repaint();
            }).start();
        }

        private void advanceAnimation() {
            if (phase == Phase.THROWING_CAPSULE && frameNo >= 20 && dialogCanContinue()) {
                if (recallAfterThrow) {
                    phase = Phase.RECALL_PLAYER;
                    frameNo = 0;
                    message = player.name + " returns to its capsule in a shimmer of light.";
                } else {
                    phase = Phase.SUMMONING;
                    frameNo = 0;
                    message = "{C:#2C765B}{ALLY}{/C}'s capsule opens!";
                }
            } else if (phase == Phase.ATTUNE_REVEAL && frameNo >= 45) {
                phase = Phase.ATTUNE_INPUT;
                frameNo = 0;
                menuFocus = 0;
            } else if (phase == Phase.ACTION_NOTICE && dialogCanContinue()) {
                phase = Phase.COMMAND;
                frameNo = 0;
                setActionsEnabled(true);
            } else if (phase == Phase.SUMMONING && frameNo > 32 && dialogCanContinue()) {
                phase = Phase.COMMAND; frameNo = 0;
                message = "{C:#2C765B}{ALLY}{/C} is ready at level {LEVEL}! Choose Fight, Bag, Run, or Capture.";
                setActionsEnabled(true);
            } else if (phase == Phase.ANIMATING) {
                if (capturing && frameNo >= 28 && dialogCanContinue()) {
                    capturing = false;
                    if (captureSuccess) {
                        boolean firstCatch = !SAVE.caught[indexOf(enemy)];
                        SAVE.catchBeast(enemy);
                        String learning = firstCatch ? " New learning unlocked: " + LESSONS[indexOf(enemy)] : " Its learning is already in your journal.";
                        completeBattle(bossBattle ? 80 : 40, "capture", "Captured " + enemy.name + "!" + learning);
                        phase = Phase.RECALL_ENEMY; frameNo = 0;
                        message = "{C:#2C765B}{BEAST}{/C} joined your capsules!";
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
                            enemy.hp = 0;
                            completeBattle(bossBattle ? 100 : 50, "battle", bossBattle ? "Boss victory!" : "Victory!");
                            phase = Phase.RECALL_ENEMY; frameNo = 0;
                            message = "{C:#9A563C}{BEAST}{/C} fainted and returned to its capsule.";
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
                finishToResult();
            } else if (phase == Phase.RECALL_ENEMY && frameNo >= 30 && dialogCanContinue()) {
                captureSuccess = false;
                enemyRemoved = true;
                startRecallThrow();
            } else if (phase == Phase.RESULT && frameNo > 3) {
                // Result stays visible until the next capsule is opened.
            }
        }

        private void applyMove() {
            if (capturing || move == null) return;
            if (move.kind == Kind.DAMAGE) {
                int damage = 13 + random.nextInt(8) + (player.level - 1) * 2;
                if (player.buffTurns > 0) damage += 4;
                if (enemy.weakenedTurns > 0) damage = (int) Math.round(damage * 1.3);
                double effectiveness = trioTypeMultiplier(move.type, enemy);
                damage = (int) Math.round(damage * effectiveness);
                enemy.hp = Math.max(0, enemy.hp - damage);
                if (enemy.weakenedTurns > 0) enemy.weakenedTurns--;
                arenaMessage(move.name + " hit for " + damage + " damage!" + (effectiveness > 1.0 ? " Super effective!" : ""));
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
                player.hp = 0;
                completeBattle(0, "battle", "Defeat.");
                startRecallThrow();
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
                int damage = 8 + random.nextInt(8) + (enemy.level - 1) * 2;
                if (enemy.buffTurns > 0) damage += 4;
                if (player.weakenedTurns > 0) damage += 4;
                double effectiveness = trioTypeMultiplier(move.type, player);
                damage = (int) Math.round(damage * effectiveness);
                player.hp = Math.max(0, player.hp - damage);
                message = enemy.name + " used " + move.name + " for " + damage + " damage!" + (effectiveness > 1.0 ? " Super effective!" : "");
            } else if (move.kind == Kind.BUFF) {
                enemy.buffTurns = 3;
                message = enemy.name + " used " + move.name + " to strengthen its next attacks!";
            } else {
                player.weakenedTurns = 3;
                message = enemy.name + " used " + move.name + ". Your guard is weakened for 3 turns.";
            }
        }

        private double trioTypeMultiplier(String attackType, Beast defender) {
            if (defender == null) return 1.0;
            String targetType = defender.firstType;
            if ("Fire".equals(defender.secondType) || "Water".equals(defender.secondType) || "Grass".equals(defender.secondType)) {
                targetType = defender.secondType;
            }
            boolean advantaged = ("Fire".equals(attackType) && "Grass".equals(targetType))
                || ("Grass".equals(attackType) && "Water".equals(targetType))
                || ("Water".equals(attackType) && "Fire".equals(targetType));
            return advantaged ? 1.5 : 1.0;
        }

        private void finishEnemyTurn() {
            Move completedMove = move;
            if (enemy.buffTurns > 0 && completedMove != null && completedMove.kind != Kind.BUFF) enemy.buffTurns--;
            if (player.weakenedTurns > 0 && completedMove != null && completedMove.kind != Kind.DEBUFF) player.weakenedTurns--;
            move = null; effectActor = null; effectKind = null;
            if (player.hp <= 0) {
                player.hp = 0;
                completeBattle(0, "battle", "Defeat.");
                startRecallThrow();
            } else {
                phase = Phase.COMMAND; frameNo = 0;
                menuFocus = 0;
                setActionsEnabled(true);
            }
        }

        private void arenaMessage(String text) { message = text; }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            BufferedImage sceneFrame = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D scene = sceneFrame.createGraphics();
            scene.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            scene.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            scene.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            scene.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            scene.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            drawBackdrop(scene);
            if (phase == Phase.SELECT) drawSelectionScene(scene);
            else drawBattle(scene);
            scene.dispose();

            Graphics2D screen = (Graphics2D) graphics.create();
            double scale = Math.min(getWidth() / (double) W, getHeight() / (double) H);
            int drawW = (int) Math.round(W * scale), drawH = (int) Math.round(H * scale);
            int ox = (getWidth() - drawW) / 2, oy = (getHeight() - drawH) / 2;
            screen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            screen.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            screen.drawImage(sceneFrame, ox, oy, drawW, drawH, null);
            drawReadableOverlayText(screen, ox, oy, scale);
            screen.dispose();
        }

        private void drawReadableOverlayText(Graphics2D target, int ox, int oy, double scale) {
            Graphics2D g = (Graphics2D) target.create();
            g.translate(ox, oy);
            g.scale(scale, scale);
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
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13)); g.setColor(new Color(36, 121, 102));
            centered(g, "LEVEL " + SAVE.level(beast) + "  ·  EXP " + SAVE.nextLevelXp(beast) + "/100  ·  "
                + (SAVE.caught[indexOf(beast)] ? "CAUGHT" : "WILD"), 601, 450);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15)); g.setColor(new Color(36, 121, 102));
            centered(g, "Catch wild beasts to unlock new capsules and lessons", 337, 414);
        }

        private void drawBattleOverlay(Graphics2D g) {
            if (player != null && enemy != null) {
                drawStatusText(g, player, 102, 27, false);
                drawStatusText(g, enemy, 666, 27, true);
                if (player.buffTurns > 0 && phase != Phase.RECALL_PLAYER) drawStatusSigilText(g, 312, 188, "POWER UP");
                if (enemy.weakenedTurns > 0 && phase != Phase.RECALL_ENEMY && !enemyRemoved) drawStatusSigilText(g, 684, 188, "GUARD DOWN");
            }
            if (phase == Phase.COMMAND) drawCommandText(g);
            else if (phase == Phase.MOVE_SELECT) drawMoveText(g);
            else if (phase == Phase.CAPSULE_SELECT) drawCapsulePickerText(g);
            else if (phase == Phase.ETHICS_QUESTION || phase == Phase.ETHICS_FEEDBACK) drawEthicsText(g);
            else if (phase == Phase.ATTUNE_REVEAL || phase == Phase.ATTUNE_INPUT) drawAttunementText(g);
            else if (phase == Phase.SUMMONING) {
                PixelBitmapFont.drawCentered(g, "CAPSULE OPENING", 477, 157, 360, 2, new Color(247, 248, 224));
            } else if (phase == Phase.RESULT) {
                PixelBitmapFont.drawCentered(g, "ENCOUNTER COMPLETE", 480, 232, 368, 2, new Color(61, 82, 107));
                PixelBitmapFont.drawCentered(g, "Choose a partner and stage from the map.", 480, 266, 368, 1, MUTED);
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
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12)); g.setColor(new Color(44, 91, 75));
            if (right) rightText(g, "Lv." + beast.level, x + 180, y + 44); else g.drawString("Lv." + beast.level, x + 18, y + 44);
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13)); g.setColor(MUTED);
            if (right) rightText(g, beast.types().toUpperCase(Locale.ROOT), x + 180, y + 59);
            else g.drawString(beast.types().toUpperCase(Locale.ROOT), x + 18, y + 59);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14)); g.setColor(INK);
            String hp = beast.hp + "/" + beast.maxHp + " HP";
            if (right) rightText(g, hp, x + 180, y + 84); else g.drawString(hp, x + 18, y + 84);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 10)); g.setColor(MUTED);
            String xp = "EXP " + SAVE.nextLevelXp(beast) + "/100";
            if (right) rightText(g, xp, x + 180, y + 101); else g.drawString(xp, x + 18, y + 101);
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
            PixelBitmapFont.drawWrapped(g, text, 42, 464, 848, 3, 2, new Color(27, 37, 53));
            if (visibleDialogChars >= lastDialogText.length()) {
                PixelBitmapFont.drawWrapped(g, "SPACE / CLICK", 835, 452, 100, 1, 1,
                    new Color(27, 37, 53));
                g.setColor(new Color(27, 37, 53));
                g.fillPolygon(new int[] {913, 925, 937}, new int[] {508, 518, 508}, 3);
            }
        }

        private void drawEthicsPanel(Graphics2D g) {
            drawPixelPanel(g, 32, 350, 896, 178);
            g.setColor(new Color(113, 128, 99));
            g.fillRect(642, 365, 3, 150);
            if (phase == Phase.ETHICS_QUESTION) {
                for (int i = 0; i < 3; i++) {
                    int y = 441 + i * 30;
                    g.setColor(menuFocus == i ? new Color(204, 237, 207) : new Color(239, 241, 229));
                    g.fillRect(52, y, 570, 26);
                    g.setColor(menuFocus == i ? new Color(29, 119, 75) : new Color(82, 97, 72));
                    g.drawRect(52, y, 570, 26);
                }
            } else {
                g.setColor(new Color(235, 246, 215));
                g.fillRect(666, 392, 232, 112);
                g.setColor(new Color(89, 139, 78));
                g.drawRect(666, 392, 232, 112);
            }
        }

        private void drawEthicsText(Graphics2D g) {
            if (currentQuestion == null) return;
            PixelBitmapFont.drawWrapped(g, "ECHO CHALLENGE: COLLEGE ETHICS", 61, 362, 548, 1, 2,
                new Color(35, 100, 70));
            PixelBitmapFont.drawWrapped(g, currentQuestion.scenario, 61, 380, 548, 4, 2,
                new Color(34, 66, 53));

            if (phase == Phase.ETHICS_QUESTION) {
                for (int i = 0; i < currentQuestion.answers.length; i++) {
                    String answer = (char) ('A' + i) + ".  " + currentQuestion.answers[i];
                    PixelBitmapFont.drawWrapped(g, answer, 69, 446 + i * 30, 545, 1, 2,
                        new Color(29, 63, 52));
                }
                g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
                g.setColor(new Color(26, 56, 49));
                centered(g, "ETHICS LESSON", 782, 384);
                g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
                g.setColor(new Color(72, 87, 72));
                centered(g, currentQuestion.topic.toUpperCase(Locale.ROOT), 782, 407);
                drawEthicsProgress(g);
                g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 21));
                g.setColor(new Color(35, 132, 70));
                centered(g, "XP ON WIN / CATCH", 782, 492);
                g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
                g.setColor(MUTED);
                centered(g, "NOT FOR ANSWERS", 782, 515);
            } else {
                PixelBitmapFont.drawWrapped(g, ethicsCorrect ? "CORRECT!" : "LET'S REVIEW", 682, 398, 208, 1, 2,
                    ethicsCorrect ? new Color(27, 116, 65) : new Color(157, 82, 48));
                String correctAnswer = "BEST: " + (char) ('A' + currentQuestion.correctIndex) + ". "
                    + currentQuestion.answers[currentQuestion.correctIndex];
                PixelBitmapFont.drawWrapped(g, correctAnswer, 682, 419, 208, 2, 1,
                    new Color(34, 61, 50));
                PixelBitmapFont.drawWrapped(g, currentQuestion.explanation, 682, 438, 208, 5, 1,
                    new Color(34, 61, 50));
                g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
                g.setColor(ethicsCorrect ? new Color(35, 132, 70) : new Color(126, 75, 53));
                centered(g, "NO QUESTION XP", 782, 497);
                g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                g.setColor(MUTED);
                centered(g, "XP AFTER WIN OR CATCH", 782, 519);
            }
        }

        private void drawEthicsProgress(Graphics2D g) {
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
            g.setColor(new Color(37, 57, 46));
            centered(g, "QUESTION " + (ethicsQuestionsAnswered % 3 + 1) + " / 3", 782, 435);
            int x = 680, y = 448, w = 204;
            g.setColor(new Color(41, 56, 46)); g.fillRect(x, y, w, 18);
            g.setColor(new Color(194, 211, 178)); g.fillRect(x + 3, y + 3, w - 6, 12);
            int filled = (w - 6) * (ethicsQuestionsAnswered % 3) / 3;
            g.setColor(new Color(61, 181, 66)); g.fillRect(x + 3, y + 3, filled, 12);
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
            int region = Math.max(0, Math.min(battleBackgrounds.length - 1, stageRegion));
            BufferedImage background = battleBackgrounds[region];
            if (background != null) {
                double scale = Math.max(W / (double) background.getWidth(), H / (double) background.getHeight());
                int width = (int) Math.ceil(background.getWidth() * scale);
                int height = (int) Math.ceil(background.getHeight() * scale);
                g.drawImage(background, (W - width) / 2, (H - height) / 2, width, height, this);
                g.setColor(new Color(9, 19, 38, region == 4 ? 58 : 44));
                g.fillRect(0, 0, W, H);
                g.setPaint(new GradientPaint(0, 0, new Color(7, 19, 38, 112), 0, 188, new Color(7, 19, 38, 0)));
                g.fillRect(0, 0, W, 188);
                return;
            }
            g.setPaint(new GradientPaint(0, 0, new Color(184, 226, 239), 0, 245, new Color(205, 223, 166)));
            g.fillRect(0, 0, W, H);
            g.setColor(new Color(255, 248, 197, 85)); g.fillOval(342, -73, 284, 217);
            g.setPaint(new GradientPaint(0, 174, new Color(111, 167, 76), 0, H, new Color(31, 102, 53)));
            g.fillRect(0, 174, W, H - 174);

            Random foliage = new Random(44);
            for (int i = 0; i < 25; i++) {
                int x = -52 + i * 43;
                if (x > 470 && x < 604) continue;
                int top = 51 + foliage.nextInt(48);
                int size = 142 + foliage.nextInt(51);
                drawForestTree(g, x, top, size, i % 3 != 0);
            }
            drawEchoPortal(g);
            for (int i = 0; i < 58; i++) {
                int x = (i * 193 + 31) % W;
                int y = 188 + (i * 71) % 295;
                int radius = 3 + i % 5;
                g.setColor(i % 5 == 0 ? new Color(250, 245, 193, 194)
                    : new Color(37 + i % 4 * 7, 118 + i % 5 * 7, 47, 142));
                g.fillOval(x, y, radius * 2, Math.max(2, radius));
            }

            Ellipse2D arenaShadow = new Ellipse2D.Double(95, 207, 794, 225);
            g.setColor(new Color(24, 78, 43, 102)); g.fill(arenaShadow);
            Ellipse2D arena = new Ellipse2D.Double(92, 193, 794, 225);
            g.setPaint(new GradientPaint(0, 197, new Color(241, 221, 160), 0, 418, new Color(198, 165, 101)));
            g.fill(arena);
            g.setColor(new Color(255, 245, 205, 130)); g.setStroke(new BasicStroke(3));
            g.draw(new Ellipse2D.Double(112, 204, 754, 199));
            for (int i = 0; i < 45; i++) {
                int x = 126 + (i * 151) % 716, y = 220 + (i * 67) % 170;
                g.setColor(i % 2 == 0 ? new Color(173, 138, 86, 42) : new Color(255, 244, 210, 75));
                g.fill(new Ellipse2D.Double(x, y, 10 + i % 4 * 5, 2 + i % 3));
            }

            drawLeafyBush(g, -65, 381, 182, 150, new Color(31, 103, 52));
            drawLeafyBush(g, 819, 377, 186, 154, new Color(28, 94, 49));
            drawLeafyBush(g, -27, 464, 171, 123, new Color(20, 83, 44));
            drawLeafyBush(g, 840, 455, 169, 130, new Color(22, 88, 45));
        }

        private void drawEchoPortal(Graphics2D g) {
            int cx = 538;
            g.setColor(new Color(39, 224, 229, 30)); g.fillOval(cx - 77, 50, 154, 147);
            g.setColor(new Color(32, 197, 223, 58)); g.fillOval(cx - 58, 61, 116, 120);
            g.setPaint(new GradientPaint(cx - 47, 67, new Color(175, 196, 173), cx + 49, 181, new Color(72, 105, 79)));
            g.fillRoundRect(cx - 49, 68, 98, 120, 46, 46);
            g.setColor(new Color(39, 73, 65)); g.fillRoundRect(cx - 37, 80, 74, 104, 37, 37);
            g.setPaint(new GradientPaint(cx, 84, new Color(24, 157, 206), cx, 182, new Color(52, 238, 216)));
            g.fillRoundRect(cx - 29, 88, 58, 91, 29, 29);
            g.setColor(new Color(206, 255, 245, 210)); g.setStroke(new BasicStroke(3));
            g.drawOval(cx - 18, 110, 36, 45); g.drawOval(cx - 10, 119, 20, 27);
            g.setColor(new Color(230, 255, 236, 230)); g.fillOval(cx - 3, 129, 6, 6);
            g.setColor(new Color(105, 75, 46)); g.fillRect(cx - 62, 173, 124, 5);
            g.fillRect(cx - 55, 178, 8, 29); g.fillRect(cx + 47, 178, 8, 29);
            g.setColor(new Color(100, 67, 39)); g.fillRoundRect(cx - 72, 151, 144, 31, 5, 5);
            g.setColor(new Color(219, 189, 115)); g.drawRoundRect(cx - 72, 151, 144, 31, 5, 5);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
            g.setColor(new Color(251, 240, 193));
            centered(g, "ECHO SITE  →", cx, 172);
        }

        private void drawForestTree(Graphics2D g, int x, int y, int size, boolean bright) {
            int trunkW = Math.max(10, size / 9), trunkH = size / 2;
            g.setPaint(new GradientPaint(x, y + size / 4, new Color(104, 75, 48), x + trunkW, y + trunkH, new Color(68, 69, 47)));
            g.fillRoundRect(x + size / 2 - trunkW / 2, y + size / 3, trunkW, trunkH, trunkW / 2, trunkW / 2);
            Color base = bright ? new Color(37, 112, 62) : new Color(28, 91, 59);
            Color light = bright ? new Color(98, 165, 70) : new Color(61, 138, 69);
            int[][] leaves = {{0, 28, 62}, {33, 1, 72}, {62, 26, 72}, {20, 51, 68}, {56, 63, 62}, {-13, 59, 55}};
            for (int[] leaf : leaves) {
                int lx = x + leaf[0] * size / 100, ly = y + leaf[1] * size / 100, diameter = leaf[2] * size / 100;
                g.setPaint(new GradientPaint(lx, ly, light, lx + diameter, ly + diameter, base));
                g.fillOval(lx, ly, diameter, diameter * 3 / 4);
                g.setColor(new Color(151, 192, 91, 70));
                g.drawArc(lx + diameter / 7, ly + diameter / 8, diameter / 2, diameter / 2, 205, 100);
            }
        }

        private void drawLeafyBush(Graphics2D g, int x, int y, int w, int h, Color dark) {
            g.setColor(new Color(18, 67, 39, 86)); g.fillOval(x + 7, y + h - 21, w - 2, 42);
            g.setPaint(new GradientPaint(x, y, dark.brighter(), x, y + h, dark.darker()));
            g.fillRoundRect(x + 12, y + 24, w - 24, h - 24, 58, 58);
            Random leaves = new Random(x * 31L + y * 7L + w);
            for (int i = 0; i < 24; i++) {
                int lx = x + leaves.nextInt(Math.max(1, w - 36));
                int ly = y + leaves.nextInt(Math.max(1, h - 38));
                int d = 16 + leaves.nextInt(31);
                Color tint = i % 3 == 0 ? new Color(105, 174, 77) : i % 3 == 1 ? new Color(52, 135, 67) : new Color(34, 112, 61);
                g.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 190));
                g.fillOval(lx, ly, d, d * 3 / 4);
                g.setColor(new Color(193, 216, 123, 110));
                g.drawArc(lx + 4, ly + 3, d / 2, d / 3, 205, 95);
            }
        }

        private void drawSelectionScene(Graphics2D g) {
            g.setColor(new Color(24, 38, 53, 20)); g.fillRoundRect(45, 36, 870, 430, 34, 34);
            g.setColor(new Color(255, 255, 255, 207)); g.fillRoundRect(50, 32, 860, 426, 32, 32);
            g.setColor(new Color(214, 225, 232)); g.setStroke(new BasicStroke(2)); g.drawRoundRect(50, 32, 860, 426, 32, 32);
            Beast b = (Beast) playerChoice.getSelectedItem(); if (b == null) b = preview;
            drawSealedCapsule(g, 286, 270, 1.0, false, selectedCapsule);
            g.setColor(new Color(29, 45, 62, 32)); g.fillOval(515, 359, 170, 28);
            drawBeastFacing(g, b, 526, 173, 155, 178, 1, 0, 0, 1f, true);
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
            if (phase == Phase.THROWING_CAPSULE) drawTrainerCapsuleThrow(g);
            else if (phase == Phase.SUMMONING) drawSummoning(g);
            else if (phase == Phase.ANIMATING) drawMoveEffect(g);
            else if (phase == Phase.RECALL_PLAYER) drawRecall(g, false);
            else if (phase == Phase.RECALL_ENEMY) drawRecall(g, true);
            else if (phase == Phase.COMMAND) drawCommandMenu(g);
            else if (phase == Phase.MOVE_SELECT) drawMoveMenu(g);
            else if (phase == Phase.CAPSULE_SELECT) drawCapsulePicker(g);
            else if (phase == Phase.ETHICS_QUESTION || phase == Phase.ETHICS_FEEDBACK) drawEthicsPanel(g);
            else if (phase == Phase.ATTUNE_REVEAL || phase == Phase.ATTUNE_INPUT) drawAttunementScene(g);
            if (phase != Phase.COMMAND && phase != Phase.MOVE_SELECT && phase != Phase.CAPSULE_SELECT && phase != Phase.ATTUNE_REVEAL
                    && phase != Phase.ATTUNE_INPUT && phase != Phase.ETHICS_QUESTION && phase != Phase.ETHICS_FEEDBACK
                    && !dialogDismissed) drawMessage(g);
        }

        private void drawBattlefield(Graphics2D g, boolean sprites) {
            if (player == null || enemy == null) return;
            int shake = phase == Phase.ANIMATING && frameNo > 6 && frameNo < 12 ? (frameNo % 2 == 0 ? 5 : -5) : 0;
            g.translate(shake, 0);
            drawStatus(g, player, 102, 27, false);
            drawStatus(g, enemy, 666, 27, true);
            drawPlayerTrainer(g, 70, 230);
            if (trainerBattle) drawOpponentTrainer(g, 850, 230);
            else drawWildMark(g, 862, 246);
            Color groundShadow = switch (stageRegion) {
                case 1 -> new Color(30, 56, 79, 190);
                case 2 -> new Color(38, 39, 74, 190);
                case 3 -> new Color(80, 40, 32, 205);
                case 4 -> new Color(33, 54, 80, 195);
                default -> new Color(35, 91, 42, 190);
            };
            Color groundLight = switch (stageRegion) {
                case 1 -> new Color(137, 183, 201);
                case 2 -> new Color(117, 147, 183);
                case 3 -> new Color(241, 135, 61);
                case 4 -> new Color(113, 184, 195);
                default -> new Color(125, 175, 69);
            };
            // Soft contact shadows sit on the arena floor instead of reading as floating platforms.
            g.setColor(new Color(groundShadow.getRed(), groundShadow.getGreen(), groundShadow.getBlue(), 115));
            g.fillOval(251, 359, 140, 18); g.fillOval(608, 359, 140, 18);
            g.setColor(new Color(groundLight.getRed(), groundLight.getGreen(), groundLight.getBlue(), 65));
            g.fillOval(266, 360, 110, 7); g.fillOval(623, 360, 110, 7);
            if (progressionBattle) {
                String regionName = new String[] {"STARTING FOREST", "LUMA VILLAGE", "WHISPERING FOREST", "EMBER MOUNTAIN", "ETHICS ACADEMY"}[stageRegion];
                String label = regionName + "  ·  " + (stageRegion + 1) + "." + stageNumber + (bossBattle ? "  ·  BOSS" : "");
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, bossBattle ? 15 : 13));
                FontMetrics metrics = g.getFontMetrics();
                int labelWidth = metrics.stringWidth(label) + 28;
                g.setColor(new Color(10, 25, 42, 200)); g.fillRoundRect(W / 2 - labelWidth / 2, 111, labelWidth, 28, 14, 14);
                g.setColor(bossBattle ? new Color(255, 205, 117) : Color.WHITE);
                centered(g, label, W / 2, 130);
            }
            boolean playerBeastReleased = (phase != Phase.THROWING_CAPSULE && phase != Phase.SUMMONING) || recallAfterThrow;
            if (sprites && phase != Phase.RECALL_PLAYER && playerBeastReleased) drawTypeAura(g, player, 321, 317);
            if (sprites && !enemyRemoved && phase != Phase.RECALL_ENEMY) drawTypeAura(g, enemy, 675, 317);
            if (sprites && phase != Phase.RECALL_PLAYER && playerBeastReleased) drawBeastFacing(g, player, playerX(), 205, 150, 160, playerScaleX(), 0, 0, playerAlpha(), true);
            if (sprites && !enemyRemoved && phase != Phase.RECALL_ENEMY) drawBeastFacing(g, enemy, enemyX(), 205, 150, 160, enemyScaleX(), 0, 0, enemyAlpha(), false);
            if (player.buffTurns > 0 && phase != Phase.RECALL_PLAYER) drawStatusSigil(g, 312, 188, new Color(73, 199, 150), "POWER UP");
            if (enemy.weakenedTurns > 0 && phase != Phase.RECALL_ENEMY && !enemyRemoved) drawStatusSigil(g, 684, 188, new Color(133, 94, 159), "GUARD DOWN");
            g.translate(-shake, 0);
        }

        private void drawTypeAura(Graphics2D g, Beast beast, int cx, int cy) {
            if (beast == null) return;
            Color first = typeColor(beast.firstType);
            Color second = typeColor(beast.secondType == null ? beast.firstType : beast.secondType);
            Graphics2D aura = (Graphics2D) g.create();
            aura.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.26f));
            aura.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy + 10), 78f,
                new float[] {0f, .55f, 1f}, new Color[] {new Color(first.getRed(), first.getGreen(), first.getBlue(), 138),
                    new Color(second.getRed(), second.getGreen(), second.getBlue(), 65), new Color(255, 255, 255, 0)}));
            aura.fill(new Ellipse2D.Double(cx - 78, cy - 68, 156, 156));
            aura.dispose();
            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6.0 + tick * 0.025;
                int radiusX = 63 + (i % 3) * 5;
                int radiusY = 48 + (i % 2) * 7;
                int px = cx + (int) (Math.cos(angle) * radiusX);
                int py = cy + (int) (Math.sin(angle) * radiusY);
                Color color = (i + tick / 12) % 2 == 0 ? first : second;
                int size = i % 4 == 0 ? 9 : 5;
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 185));
                g.fillOval(px, py, size, size);
                g.setColor(new Color(255, 255, 255, 180));
                g.fillOval(px + size / 3, py + size / 3, Math.max(2, size / 3), Math.max(2, size / 3));
            }
        }

        private void drawBackTrainer(Graphics2D g, int x, int y, Color shirt, Color pack) {
            Graphics2D guy = (Graphics2D) g.create();
            guy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            guy.setColor(new Color(22, 59, 53, 78)); guy.fillOval(x + 1, y + 111, 59, 13);
            guy.setPaint(new GradientPaint(x + 5, y + 33, new Color(47, 68, 99), x + 58, y + 104, new Color(25, 39, 67)));
            guy.fillRoundRect(x + 10, y + 69, 17, 44, 8, 8); guy.fillRoundRect(x + 34, y + 69, 17, 44, 8, 8);
            guy.setColor(new Color(31, 47, 76)); guy.fillRoundRect(x + 7, y + 105, 23, 12, 8, 8); guy.fillRoundRect(x + 32, y + 105, 23, 12, 8, 8);
            guy.setColor(new Color(224, 224, 210)); guy.fillRoundRect(x + 6, y + 111, 24, 8, 7, 7); guy.fillRoundRect(x + 31, y + 111, 24, 8, 7, 7);
            guy.setPaint(new GradientPaint(x + 6, y + 36, shirt.brighter(), x + 56, y + 91, shirt.darker()));
            guy.fillRoundRect(x + 6, y + 39, 51, 48, 18, 18);
            guy.setColor(shirt.brighter()); guy.setStroke(new BasicStroke(3)); guy.drawLine(x + 31, y + 48, x + 31, y + 82);
            guy.setColor(pack.darker()); guy.fillRoundRect(x + 3, y + 46, 18, 37, 8, 8);
            guy.setColor(pack); guy.fillRoundRect(x + 4, y + 62, 20, 24, 7, 7);
            guy.setColor(new Color(228, 182, 139)); guy.fillRoundRect(x + 22, y + 13, 26, 31, 10, 10);
            guy.setColor(new Color(71, 46, 36));
            guy.fill(new Arc2D.Double(x + 18, y + 5, 35, 35, 0, 180, Arc2D.PIE));
            guy.fillRoundRect(x + 19, y + 20, 8, 17, 5, 5);
            guy.setColor(new Color(198, 150, 116)); guy.fillOval(x + 18, y + 25, 7, 10);
            guy.setColor(new Color(255, 236, 197, 160)); guy.drawArc(x + 25, y + 12, 21, 17, 20, 110);
            guy.dispose();
        }

        private void drawOpponentTrainer(Graphics2D g, int x, int y) {
            if (opponentTrainer.art == null) {
                drawBackTrainer(g, x, y + 10, new Color(176, 54, 56), new Color(60, 72, 121));
                return;
            }
            g.setColor(new Color(22, 62, 47, 66));
            g.fillOval(x - 8, y + 133, 86, 13);
            BeastIllustrations.draw(g, opponentTrainer.art, x - 14, y - 5, 98, 145, 1f, true);
        }
        private void drawPlayerTrainer(Graphics2D g, int x, int y) {
            int throwFrame = trainerThrowFrame();
            double lean = throwFrame >= 0 && throwFrame < 12
                ? Math.sin(throwFrame / 12.0 * Math.PI) * 0.075 : 0.0;
            Graphics2D trainer = (Graphics2D) g.create();
            trainer.rotate(lean, x + 42, y + 134);
            if (activeTrainer.art == null) {
                drawBackTrainer(trainer, x, y + 10, new Color(39, 77, 176), new Color(225, 112, 52));
            } else {
                trainer.setColor(new Color(22, 62, 47, 66));
                trainer.fillOval(x - 8, y + 133, 86, 13);
                BeastIllustrations.draw(trainer, activeTrainer.art, x - 14, y - 5, 98, 145, 1f, false);
            }
            trainer.dispose();

            if (throwFrame >= 0 && throwFrame < 8) {
                int pulse = 7 + throwFrame % 3;
                g.setColor(new Color(255, 246, 197, 190 - throwFrame * 16));
                g.fillOval(x + 60 - pulse / 2, y + 66 - pulse / 2, pulse, pulse);
                g.setColor(new Color(255, 255, 236, 220));
                g.fillOval(x + 61, y + 66, 4, 4);
            }
        }

        private int trainerThrowFrame() {
            if (phase == Phase.THROWING_CAPSULE) return frameNo;
            if (phase == Phase.ANIMATING && capturing && frameNo < 16) return frameNo;
            return -1;
        }

        private void drawTrainerCapsuleThrow(Graphics2D g) {
            drawCapsuleFlight(g, frameNo, 20, 313, 300, .58);
        }

        private void drawCapsuleFlight(Graphics2D g, int frame, int duration,
                int targetX, int targetY, double scale) {
            double t = Math.max(0.0, Math.min(1.0, frame / (double) duration));
            int startX = 137, startY = 300;
            int cx = (int) Math.round(startX + (targetX - startX) * t);
            int cy = (int) Math.round(startY + (targetY - startY) * t - Math.sin(t * Math.PI) * 92);

            for (int trail = 4; trail >= 1; trail--) {
                double trailT = Math.max(0.0, t - trail * .055);
                int trailX = (int) Math.round(startX + (targetX - startX) * trailT);
                int trailY = (int) Math.round(startY + (targetY - startY) * trailT - Math.sin(trailT * Math.PI) * 92);
                int size = Math.max(3, 9 - trail);
                int alpha = 45 + (4 - trail) * 30;
                Color glow = selectedCapsule.color;
                g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), alpha));
                g.fillOval(trailX - size / 2, trailY - size / 2, size, size);
            }

            Graphics2D spinningCapsule = (Graphics2D) g.create();
            spinningCapsule.rotate(t * Math.PI * 3, cx, cy);
            drawCapsule(spinningCapsule, cx, cy, scale, 0, 0, false, selectedCapsule);
            spinningCapsule.dispose();
        }

        private void drawWildMark(Graphics2D g, int x, int y) {
            g.setColor(new Color(236, 231, 191)); g.fillRect(x, y, 35, 4); g.fillRect(x + 5, y - 7, 25, 4);
            g.setColor(new Color(33, 88, 38)); g.fillRect(x + 9, y - 12, 17, 5);
        }

        private int playerX() {
            int x = 246;
            if (phase == Phase.ANIMATING && move != null && effectActor != enemy && frameNo < 9) x += frameNo * 8;
            if (phase == Phase.RECALL_PLAYER) x = (int) (259 + frameNo * 1.1);
            return x;
        }
        private int enemyX() {
            int x = 600;
            if (phase == Phase.ANIMATING && effectActor == enemy && frameNo < 9) x -= frameNo * 5;
            if (phase == Phase.RECALL_ENEMY) x = (int) (630 - frameNo * 0.7);

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
            g.setColor(new Color(12, 47, 42, 95)); g.fillRoundRect(x + 5, y + 7, 195, 110, 22, 22);
            g.setPaint(new GradientPaint(x, y, new Color(31, 81, 65), x + 8, y + 110, new Color(18, 54, 46)));
            g.fillRoundRect(x, y, 195, 110, 22, 22);
            g.setColor(new Color(244, 231, 184)); g.fillRoundRect(x + 5, y + 5, 185, 100, 17, 17);
            g.setPaint(new GradientPaint(x + 8, y + 8, new Color(255, 254, 237), x + 8, y + 103, new Color(238, 240, 211)));
            g.fillRoundRect(x + 9, y + 9, 177, 92, 14, 14);
            g.setColor(new Color(80, 112, 77, 150)); g.setStroke(new BasicStroke(1.4f));
            g.drawRoundRect(x + 9, y + 9, 177, 92, 14, 14);
            g.setColor(new Color(118, 157, 91));
            g.drawArc(x + 13, y + 13, 18, 18, 90, 90); g.drawArc(x + 164, y + 13, 18, 18, 0, 90);
            int bx = x + 49, by = y + 62, bw = 126;
            g.setColor(new Color(30, 65, 48)); g.fillRoundRect(bx - 3, by - 3, bw + 6, 16, 9, 9);
            g.setColor(new Color(194, 213, 173)); g.fillRoundRect(bx, by, bw, 10, 7, 7);
            int hpPercent = beast.maxHp == 0 ? 0 : 100 * beast.hp / beast.maxHp;
            Color hpColor = hpPercent > 45 ? new Color(54, 170, 74) : hpPercent > 20 ? new Color(222, 169, 52) : new Color(203, 74, 59);
            g.setPaint(new GradientPaint(bx, by, hpColor.brighter(), bx, by + 10, hpColor.darker()));
            g.fillRoundRect(bx, by, Math.max(0, bw * beast.hp / beast.maxHp), 10, 7, 7);
            int exp = SAVE.nextLevelXp(beast);
            g.setColor(new Color(126, 143, 113)); g.fillRoundRect(bx, y + 89, bw, 5, 5, 5);
            g.setPaint(new GradientPaint(bx, y + 88, new Color(147, 205, 113), bx, y + 95, new Color(62, 150, 83)));
            g.fillRoundRect(bx, y + 89, bw * exp / 100, 5, 5, 5);
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

        private void drawCapsulePicker(Graphics2D g) {
            drawPixelPanel(g, 32, 350, 896, 178);
            CapsuleType[] capsules = CapsuleType.values();
            for (int i = 0; i < capsules.length; i++) {
                int col = i % 4;
                int row = i / 4;
                int x = 48 + col * 216;
                int y = 390 + row * 62;
                boolean unlocked = capsules[i].isUnlocked();
                g.setColor(!unlocked ? new Color(224, 225, 215)
                    : menuFocus == i ? new Color(220, 240, 221) : new Color(247, 247, 226));
                g.fillRoundRect(x, y, 208, 54, 12, 12);
                g.setColor(!unlocked ? new Color(143, 147, 136)
                    : menuFocus == i ? capsules[i].color.darker() : new Color(129, 145, 119));
                g.setStroke(new BasicStroke(unlocked && menuFocus == i ? 2.2f : 1.2f));
                g.drawRoundRect(x, y, 208, 54, 12, 12);
                // Picker previews are always sealed. Opening is reserved for the active animation.
                drawSealedCapsule(g, x + 24, y + 27, 0.46, false, capsules[i]);
            }
        }

        private void drawCapsulePickerText(Graphics2D g) {
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
            g.setColor(new Color(35, 100, 70));
            g.drawString("CHOOSE A CYLINDER CAPSULE", 61, 378);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 10));
            g.setColor(MUTED);
            String instruction = capsulePickerNotice.isEmpty()
                ? "PRESS 1-8 TO THROW  ·  ESC TO RETURN" : capsulePickerNotice;
            g.setColor(capsulePickerNotice.isEmpty() ? MUTED : new Color(153, 82, 43));
            rightText(g, instruction, 900, 378);

            CapsuleType[] capsules = CapsuleType.values();
            for (int i = 0; i < capsules.length; i++) {
                CapsuleType capsule = capsules[i];
                int col = i % 4;
                int row = i / 4;
                int x = 48 + col * 216;
                int y = 390 + row * 62;
                boolean unlocked = capsule.isUnlocked();
                g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
                g.setColor(unlocked ? INK : new Color(104, 108, 99));
                String capsuleName = (i + 1) + ". " + capsule.label + (unlocked ? "" : " · LOCKED");
                g.drawString(ellipsize(g, capsuleName, 146), x + 46, y + 20);
                String affinity;
                if (!unlocked) affinity = capsule.unlockRequirement();
                else if (capsule == CapsuleType.FIELD) affinity = "standard odds";
                else if (capsule == CapsuleType.PRISM) affinity = "any type +28%";
                else if (capsule.matches(enemy)) {
                    affinity = "MATCH +" + (int) Math.round(capsule.bonusAgainst(enemy) * 100)
                        + "% · " + capsule.affinityText.toUpperCase(Locale.ROOT).replace(" / ", "/");
                } else {
                    affinity = "BASE +" + (int) Math.round(capsule.universalBonus * 100)
                        + "% · " + capsule.affinityText.toUpperCase(Locale.ROOT).replace(" / ", "/");
                }
                g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 8));
                g.setColor(unlocked ? new Color(71, 91, 70) : new Color(108, 111, 102));
                g.drawString(ellipsize(g, affinity, 151), x + 46, y + 36);
                g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
                g.setColor(unlocked ? capsule.color.darker() : new Color(108, 111, 102));
                rightText(g, unlocked ? (int) Math.round(captureChance(capsule) * 100) + "%" : "LOCKED", x + 198, y + 49);
            }
        }

        private void drawPixelPanel(Graphics2D g, int x, int y, int w, int h) {
            g.setColor(new Color(10, 42, 38, 105)); g.fillRoundRect(x + 4, y + 6, w, h, 24, 24);
            g.setPaint(new GradientPaint(x, y, new Color(18, 71, 57), x, y + h, new Color(12, 50, 46)));
            g.fillRoundRect(x, y, w, h, 24, 24);
            g.setColor(new Color(247, 236, 195)); g.fillRoundRect(x + 5, y + 5, w - 10, h - 10, 19, 19);
            g.setPaint(new GradientPaint(x, y + 9, new Color(255, 254, 239), x, y + h - 9, new Color(241, 244, 222)));
            g.fillRoundRect(x + 10, y + 10, w - 20, h - 20, 14, 14);
            g.setColor(new Color(79, 115, 79)); g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(x + 10, y + 10, w - 20, h - 20, 14, 14);
            g.setColor(new Color(117, 157, 100)); g.setStroke(new BasicStroke(2));
            g.drawArc(x + 12, y + 12, 23, 23, 90, 90);
            g.drawArc(x + w - 35, y + 12, 23, 23, 0, 90);
            g.drawArc(x + 12, y + h - 35, 23, 23, 180, 90);
            g.drawArc(x + w - 35, y + h - 35, 23, 23, 270, 90);
        }

        private void drawMenuArrow(Graphics2D g, int x, int y) {
            g.setColor(new Color(22, 28, 23));
            g.fillPolygon(new int[] {x, x + 13, x}, new int[] {y, y + 7, y + 14}, 3);
        }

        private void drawSummoning(Graphics2D g) {
            int f = frameNo;
            int cy = Math.min(18, f / 2);
            drawCapsule(g, 313, 300, Math.max(.65, 1 - f / 125.0), f < 7 ? f : 7, cy, true);
            if (f >= 6) {
                drawPixelSparkles(g, 368, 286, new Color(250, 221, 126), 12 + f / 2);
                float alpha = Math.min(1f, (f - 5) / 17f);
                double size = Math.min(1, (f - 5) / 17.0);
                drawBeastFacing(g, player, (int) (259 + (1 - size) * 40), (int) (245 + (1 - size) * 35), (int) (108 * size), (int) (120 * size), 1, 0, 0, alpha, true);
            }
            g.setColor(new Color(255, 255, 255, 210)); g.fillRoundRect(357, 139, 240, 47, 20, 20);
        }

        private void drawMoveEffect(Graphics2D g) {
            if (capturing) {
                if (frameNo < 15) {
                    drawCapsuleFlight(g, frameNo, 15, 675, 300, .62);
                } else {
                    drawCapsule(g, 675, 300, Math.max(.18, .75 - (frameNo - 15) / 45.0),
                        frameNo % 8 < 4 ? 2 : 0, 0, false);
                    drawPixelSparkles(g, 675, 300, new Color(255, 225, 130), 10);
                }
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
                int y = 300 + (int) (Math.sin(t * Math.PI * 3) * 26);
                drawTypeAttack(g, move.type, x, y, frameNo, enemyAttacks);
            } else if (frameNo >= 8 && frameNo <= 17) {
                int p = frameNo - 8;
                Color col = typeColor(move.type);
                int hitX = effectActor == enemy ? 313 : 684;
                drawBurst(g, hitX, 313, col, p);
                if (p < 3) { g.setColor(new Color(255, 255, 245, (3 - p) * 55)); g.fillOval(hitX - 72, 238, 144, 144); }
            }
        }

        private void drawAttunementEffect(Graphics2D g, int frame) {
            int cx = 685, cy = 305;
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
            int cx = 650, cy = 305;
            Color color = new Color(246, 196, 113);
            for (int i = 0; i < 12; i++) {
                double angle = i * (Math.PI / 6.0);
                int distance = 12 + frame * (2 + i % 3);
                int x = cx + (int) (Math.cos(angle) * distance);
                int y = cy + (int) (Math.sin(angle) * distance);
                g.setColor((i + frame / 2) % 3 == 0 ? Color.WHITE : color);
                int particle = Math.max(3, 8 - frame / 4);
                g.fill(new Ellipse2D.Double(x - particle / 2.0, y - particle / 2.0, particle, particle));
            }
            if (frame < 8) drawCapsule(g, cx, cy, .35, frame % 2 == 0 ? 3 : 0, 0, false);
        }

        private void drawBuffEffect(Graphics2D g, boolean enemyBuff, int frame) {
            int x = enemyBuff ? 684 : 313, y = 320;
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
            int x = 699, y = 311;
            Color col = new Color(136, 86, 163);
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 62));
            g.setStroke(new BasicStroke(6)); g.drawOval(x - 67 - frame, y - 38 - frame / 2, 134 + frame * 2, 76 + frame);
            g.setStroke(new BasicStroke(2)); g.drawOval(x - 48 + frame / 2, y - 26, 96 - frame, 52);
            for (int i = 0; i < 7; i++) {
                double a = (tick * .16 + i * Math.PI / 3.5);
                int px = x + (int) (Math.cos(a) * 67), py = y + (int) (Math.sin(a) * 37);
                g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 210));
                g.fill(new Ellipse2D.Double(px - 3.5, py - 3.5, 7, 7));
            }
        }

        private void drawRecall(Graphics2D g, boolean enemySide) {
            int x = enemySide ? 684 : 313, y = 313;
            Color c = new Color(129, 202, 239);
            drawCapsule(g, x, y + 28, .63, frameNo < 12 ? frameNo / 2 : 0, 0, true);
            for (int i = 0; i < 18; i++) {
                double ang = (i * .77 + tick * .06);
                double r = 28 + ((i * 13 + frameNo * 4) % 100);
                int px = x + (int) (Math.cos(ang) * r * Math.max(.12, 1 - frameNo / 37.0));
                int py = y + (int) (Math.sin(ang) * r * Math.max(.12, 1 - frameNo / 37.0));
                int alpha = Math.max(15, 210 - frameNo * 6);
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
                g.fill(new Ellipse2D.Double(px, py, 5, 5));
            }
        }

        private void drawMessage(Graphics2D g) {
            drawPixelPanel(g, 16, 447, 928, 79);
        }

        private void drawCapsule(Graphics2D g, int cx, int cy, double scale, int opening, int lift, boolean large) {
            drawCapsule(g, cx, cy, scale, opening, lift, large, selectedCapsule);
        }

        private void drawSealedCapsule(Graphics2D g, int cx, int cy, double scale, boolean large,
                CapsuleType capsuleType) {
            drawCapsule(g, cx, cy, scale, 0, 0, large, capsuleType);
        }

        private void drawCapsule(Graphics2D g, int cx, int cy, double scale, int opening, int lift,
                boolean large, CapsuleType capsuleType) {
            boolean openingAnimation = phase == Phase.SUMMONING || phase == Phase.RECALL_PLAYER
                || phase == Phase.RECALL_ENEMY
                || (phase == Phase.ANIMATING && (capturing || captureBreakEffect));
            int activeOpening = openingAnimation && capsuleType == selectedCapsule
                ? Math.max(0, Math.min(opening, 8)) : 0;
            int w = Math.max(1, (int) ((large ? 62 : 42) * scale));
            int h = Math.max(1, (int) ((large ? 112 : 76) * scale));
            int x = cx - w / 2;
            int y = cy - h / 2;
            int split = y + h / 2;
            int radius = Math.max(8, w * 3 / 4);
            int topLift = activeOpening * (large ? 2 : 1);
            Color shell = capsuleType.color;
            Color dark = new Color(26, 51, 48);
            Color base = new Color(242, 237, 211);

            g.setColor(new Color(20, 65, 45, 85));
            g.fillOval(x - w / 5, y + h - 3, w + 2 * (w / 5), Math.max(6, h / 7));
            g.setColor(dark);
            g.fillRoundRect(x - 3, y - 3 - topLift, w + 6, h + 6, radius + 5, radius + 5);
            g.setColor(base.darker());
            g.fillRoundRect(x, split + lift - 1, w, h / 2 + 1, radius, radius);
            g.setColor(base);
            g.fillRect(x + 2, split + lift, Math.max(1, w - 4), Math.max(1, h / 2 - radius / 2));
            g.setColor(shell.darker());
            g.fillRoundRect(x, y - topLift, w, h / 2 + 7, radius, radius);
            g.setColor(shell);
            g.fillRect(x + 2, y + radius / 2 - topLift, Math.max(1, w - 4), Math.max(1, h / 2 + 6 - radius / 2));
            g.setColor(new Color(255, 255, 255, 110));
            g.fillRoundRect(x + Math.max(4, w / 7), y + 8 - topLift, Math.max(2, w / 8), Math.max(1, h - 20), 8, 8);
            g.setColor(new Color(25, 48, 46));
            g.fillRoundRect(x - 1, split - 3 + lift, w + 2, Math.max(5, h / 11), 7, 7);
            g.setColor(new Color(225, 230, 206));
            g.fillRect(x + 2, split - 1 + lift, Math.max(1, w - 4), Math.max(1, h / 22));
            g.setColor(dark);
            g.setStroke(new BasicStroke(Math.max(1.4f, w / 24f)));
            g.drawRoundRect(x, y - topLift, w, h, radius, radius);

            int emblem = Math.max(8, w / 3);
            int emblemY = y + h / 3 - emblem / 2 - topLift;
            g.setColor(new Color(244, 242, 216));
            g.fillOval(cx - emblem / 2, emblemY, emblem, emblem);
            g.setColor(dark);
            g.drawOval(cx - emblem / 2, emblemY, emblem, emblem);
            g.setColor(shell.darker());
            g.fillOval(cx - Math.max(2, emblem / 5), emblemY + emblem / 3,
                Math.max(4, emblem * 2 / 5), Math.max(4, emblem / 3));
            if (activeOpening > 0) {
                g.setColor(new Color(255, 225, 141, 220));
                g.fillOval(cx - w / 3, split - h / 12 + lift, Math.max(1, 2 * w / 3), Math.max(4, h / 8));
            }
        }

        private void drawSprite(Graphics2D g, Beast beast, int x, int y, int w, int h, double scaleX, int dx, int dy, float alpha, boolean flip) {
            if (beast == null || alpha <= 0) return;
            int dw = Math.max(1, (int) (w * scaleX)), dh = h;
            int drawX = x + dx + (w - dw) / 2;
            BeastIllustrations.draw(g, beast.sprite, drawX, y + dy, dw, dh, alpha, flip);
        }

        private void drawBeastFacing(Graphics2D g, Beast beast, int x, int y, int w, int h,
                double scaleX, int dx, int dy, float alpha, boolean faceRight) {
            if (beast == null) return;
            boolean flip = beast.facesRightInSource != faceRight;
            drawSprite(g, beast, x, y, w, h, scaleX, dx, dy, alpha, flip);
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

        /** Draws a glossy, direction-correct projectile with an elemental silhouette. */
        private void drawTypeAttack(Graphics2D g, String type, int x, int y, int frame, boolean enemyAttacks) {
            Color base = typeColor(type);
            Graphics2D fx = (Graphics2D) g.create();
            fx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            fx.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            if (enemyAttacks) fx.rotate(Math.PI, x, y);
            float pulse = (float) (0.9 + 0.1 * Math.sin(frame * .82));

            fx.setPaint(new RadialGradientPaint(new Point2D.Float(x, y), 37f,
                new float[] {0f, .42f, 1f}, new Color[] {
                    new Color(255, 255, 246, 205), new Color(base.getRed(), base.getGreen(), base.getBlue(), 108),
                    new Color(base.getRed(), base.getGreen(), base.getBlue(), 0)}));
            fx.fill(new Ellipse2D.Double(x - 37, y - 37, 74, 74));

            fx.setPaint(new GradientPaint(x - 69, y, new Color(base.getRed(), base.getGreen(), base.getBlue(), 0),
                x - 9, y, new Color(base.getRed(), base.getGreen(), base.getBlue(), 172)));
            fx.setStroke(new BasicStroke(17, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            fx.drawLine(x - 68, y, x - 11, y);
            fx.setColor(new Color(255, 255, 248, 175));
            fx.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            fx.drawLine(x - 54, y, x - 13, y);
            for (int i = 0; i < 3; i++) {
                int offset = (i - 1) * 11;
                fx.setColor(new Color(255, 255, 255, 112 - i * 12));
                fx.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                fx.drawLine(x - 57, y + offset, x - 31, y + offset + (i - 1) * 3);
            }

            switch (type) {
                case "Fire" -> {
                    Path2D flame = new Path2D.Double();
                    flame.moveTo(x - 23, y + 1); flame.curveTo(x - 10, y - 13, x - 8, y - 24, x + 4, y - 20);
                    flame.curveTo(x + 7, y - 12, x + 24, y - 15, x + 27, y - 2);
                    flame.curveTo(x + 31, y + 11, x + 15, y + 21, x + 3, y + 15);
                    flame.curveTo(x - 7, y + 22, x - 19, y + 13, x - 23, y + 1); flame.closePath();
                    fx.setColor(new Color(130, 40, 38, 235)); fx.fill(flame);
                    fx.setColor(new Color(249, 105, 38, 247));
                    fx.fill(new Ellipse2D.Double(x - 10, y - 12, 31, 23));
                    fx.setColor(new Color(255, 231, 151, 250));
                    fx.fill(new Ellipse2D.Double(x - 2, y - 7, 17, 14));
                    fx.setColor(new Color(255, 255, 232, 220)); fx.draw(flame);
                }
                case "Dragon" -> {
                    Path2D crest = new Path2D.Double();
                    crest.moveTo(x - 23, y + 1); crest.lineTo(x - 7, y - 16); crest.lineTo(x + 1, y - 11);
                    crest.lineTo(x + 10, y - 24); crest.lineTo(x + 14, y - 8); crest.lineTo(x + 28, y - 3);
                    crest.lineTo(x + 15, y + 6); crest.lineTo(x + 21, y + 16); crest.lineTo(x + 2, y + 12);
                    crest.lineTo(x - 13, y + 20); crest.lineTo(x - 9, y + 7); crest.closePath();
                    fx.setColor(new Color(69, 54, 145, 235)); fx.fill(crest);
                    fx.setColor(new Color(167, 155, 255, 240)); fx.setStroke(new BasicStroke(2)); fx.draw(crest);
                    fx.setColor(new Color(255, 245, 193, 245)); fx.fill(new Ellipse2D.Double(x + 7, y - 4, 7, 7));
                    for (int i = 0; i < 3; i++) {
                        int sx = x - 21 - i * 9;
                        fx.setColor(new Color(131, 114, 237, 170 - i * 34));
                        fx.fill(new Ellipse2D.Double(sx, y - 4 + i * 3, 9, 8));
                    }
                }
                case "Water" -> {
                    Path2D droplet = new Path2D.Double();
                    droplet.moveTo(x + 1, y - 23); droplet.curveTo(x + 18, y - 4, x + 22, y + 3, x + 16, y + 14);
                    droplet.curveTo(x + 9, y + 27, x - 12, y + 21, x - 15, y + 10);
                    droplet.curveTo(x - 19, y - 1, x - 6, y - 11, x + 1, y - 23); droplet.closePath();
                    fx.setColor(new Color(20, 88, 169, 230)); fx.fill(droplet);
                    fx.setColor(new Color(80, 203, 247, 245)); fx.fill(new Ellipse2D.Double(x - 10, y - 7, 28, 22));
                    fx.setColor(new Color(235, 255, 255, 240)); fx.fill(new Ellipse2D.Double(x - 5, y - 10, 8, 7));
                    fx.setColor(new Color(183, 244, 255, 220)); fx.setStroke(new BasicStroke(2));
                    fx.draw(new Ellipse2D.Double(x - 19, y - 17, 12, 12));
                    fx.draw(new Ellipse2D.Double(x + 16, y + 9, 8, 8));
                }
                case "Ice" -> {
                    for (int i = 0; i < 3; i++) {
                        int sy = y + (i - 1) * 11;
                        Path2D shard = new Path2D.Double();
                        shard.moveTo(x - 13, sy + 2); shard.lineTo(x - 2, sy - 13 - (i == 1 ? 7 : 0));
                        shard.lineTo(x + 7, sy - 4); shard.lineTo(x + 15, sy - 9); shard.lineTo(x + 10, sy + 9);
                        shard.lineTo(x - 4, sy + 13); shard.closePath();
                        fx.setColor(i == 1 ? new Color(192, 248, 255, 245) : new Color(71, 168, 221, 235)); fx.fill(shard);
                        fx.setColor(new Color(255, 255, 255, 230)); fx.setStroke(new BasicStroke(1.5f)); fx.draw(shard);
                        fx.drawLine(x - 2, sy - 9, x + 3, sy + 7);
                    }
                }
                case "Grass" -> {
                    for (int i = 0; i < 3; i++) {
                        Graphics2D leaf = (Graphics2D) fx.create();
                        int ly = y + (i - 1) * 12;
                        leaf.rotate((i - 1) * .28, x, ly);
                        leaf.setColor(i == 1 ? new Color(135, 214, 82, 245) : new Color(47, 154, 83, 235));
                        leaf.fill(new Ellipse2D.Double(x - 23, ly - 7, 39, 14));
                        leaf.setColor(new Color(232, 255, 192, 230)); leaf.setStroke(new BasicStroke(1.5f));
                        leaf.drawLine(x - 19, ly, x + 12, ly - 1);
                        leaf.dispose();
                    }
                }
                case "Bug" -> {
                    fx.setColor(new Color(57, 107, 59, 240)); fx.fill(new Ellipse2D.Double(x - 13, y - 11, 27, 23));
                    fx.setColor(new Color(159, 209, 80, 245));
                    fx.fill(new Ellipse2D.Double(x - 20, y - 17, 16, 13)); fx.fill(new Ellipse2D.Double(x + 4, y - 17, 16, 13));
                    fx.setColor(new Color(226, 255, 197, 230)); fx.setStroke(new BasicStroke(2));
                    fx.draw(new Arc2D.Double(x - 23, y - 22, 20, 21, 5, 135, Arc2D.OPEN));
                    fx.draw(new Arc2D.Double(x + 3, y - 22, 20, 21, 40, 135, Arc2D.OPEN));
                    fx.setColor(new Color(244, 220, 130, 245)); fx.fill(new Ellipse2D.Double(x - 3, y - 4, 5, 5));
                    for (int i = -1; i <= 1; i++) { fx.drawLine(x - 8, y + i * 4, x - 17, y + i * 8); fx.drawLine(x + 8, y + i * 4, x + 17, y + i * 8); }
                }
                case "Electric" -> {
                    Path2D bolt = new Path2D.Double();
                    bolt.moveTo(x - 7, y - 20); bolt.lineTo(x + 3, y - 5); bolt.lineTo(x - 5, y - 2);
                    bolt.lineTo(x + 10, y + 18); bolt.lineTo(x + 5, y + 3); bolt.lineTo(x + 13, y);
                    bolt.closePath();
                    fx.setColor(new Color(255, 219, 59, 95)); fx.setStroke(new BasicStroke(12)); fx.draw(bolt);
                    fx.setColor(new Color(255, 234, 93, 255)); fx.fill(bolt);
                    fx.setColor(new Color(255, 255, 237, 245)); fx.setStroke(new BasicStroke(2)); fx.draw(bolt);
                    fx.setColor(new Color(255, 247, 151, 235)); fx.draw(new Arc2D.Double(x - 26, y - 21, 18, 18, 15, 100, Arc2D.OPEN));
                    fx.draw(new Arc2D.Double(x + 8, y + 8, 18, 18, 185, 100, Arc2D.OPEN));
                }
                case "Psychic" -> {
                    fx.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 205));
                    fx.fill(new Ellipse2D.Double(x - 15, y - 15, 30, 30));
                    fx.setColor(new Color(248, 228, 255, 235)); fx.setStroke(new BasicStroke(2.5f));
                    fx.draw(new Ellipse2D.Double(x - 22, y - 13, 44, 26));
                    fx.draw(new Ellipse2D.Double(x - 13, y - 22, 26, 44));
                    fx.setColor(new Color(255, 255, 255, 245)); fx.fill(new Ellipse2D.Double(x - 5, y - 6, 8, 8));
                    for (int i = 0; i < 4; i++) {
                        double angle = tick * .14 + i * Math.PI / 2;
                        int px = x + (int) (Math.cos(angle) * 27), py = y + (int) (Math.sin(angle) * 16);
                        drawPixelStar(fx, px, py, 9, base);
                    }
                }
                case "Fairy" -> {
                    fx.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 220));
                    drawPixelStar(fx, x, y, 40 + (int) (4 * pulse), base);
                    fx.setColor(new Color(255, 249, 255, 235)); fx.setStroke(new BasicStroke(2));
                    fx.draw(new Ellipse2D.Double(x - 25, y - 25, 50, 50));
                    for (int i = 0; i < 6; i++) {
                        double a = tick * .08 + i * Math.PI / 3;
                        int px = x + (int) (Math.cos(a) * 30), py = y + (int) (Math.sin(a) * 24);
                        fx.setColor(i % 2 == 0 ? new Color(255, 245, 173, 240) : Color.WHITE);
                        fx.fill(new Ellipse2D.Double(px - 3, py - 3, 6, 6));
                    }
                }
                case "Poison" -> {
                    fx.setColor(new Color(67, 34, 93, 235)); fx.fill(new Ellipse2D.Double(x - 18, y - 15, 36, 31));
                    fx.setColor(new Color(171, 86, 196, 245));
                    fx.fill(new Ellipse2D.Double(x - 24, y - 7, 17, 16)); fx.fill(new Ellipse2D.Double(x - 9, y - 18, 18, 19));
                    fx.fill(new Ellipse2D.Double(x + 7, y - 6, 16, 15));
                    fx.setColor(new Color(231, 255, 167, 245)); fx.fill(new Ellipse2D.Double(x - 3, y - 4, 7, 7));
                    fx.setColor(new Color(241, 206, 255, 220)); fx.setStroke(new BasicStroke(2));
                    fx.draw(new Ellipse2D.Double(x - 14, y - 10, 8, 7)); fx.draw(new Ellipse2D.Double(x + 8, y - 8, 6, 6));
                }
                case "Ghost" -> {
                    Path2D wisp = new Path2D.Double();
                    wisp.moveTo(x - 18, y + 15); wisp.curveTo(x - 27, y - 4, x - 9, y - 24, x + 4, y - 21);
                    wisp.curveTo(x + 24, y - 18, x + 24, y + 2, x + 17, y + 15);
                    wisp.lineTo(x + 8, y + 9); wisp.lineTo(x + 1, y + 17); wisp.lineTo(x - 7, y + 10); wisp.closePath();
                    fx.setColor(new Color(69, 59, 119, 235)); fx.fill(wisp);
                    fx.setColor(new Color(201, 191, 255, 225)); fx.setStroke(new BasicStroke(2)); fx.draw(wisp);
                    fx.setColor(Color.WHITE); fx.fill(new Ellipse2D.Double(x - 8, y - 5, 5, 7)); fx.fill(new Ellipse2D.Double(x + 4, y - 5, 5, 7));
                    fx.setColor(new Color(188, 239, 255, 225)); fx.draw(new Arc2D.Double(x - 25, y - 23, 50, 46, 35, 290, Arc2D.OPEN));
                }
                case "Dark" -> {
                    fx.setColor(new Color(34, 31, 54, 245)); fx.fill(new Ellipse2D.Double(x - 20, y - 20, 40, 40));
                    fx.setColor(new Color(144, 112, 207, 235));
                    fx.fill(new Arc2D.Double(x - 24, y - 22, 47, 45, 40, 285, Arc2D.PIE));
                    fx.setColor(new Color(38, 32, 59, 250)); fx.fill(new Ellipse2D.Double(x - 4, y - 20, 30, 37));
                    fx.setColor(new Color(215, 203, 255, 230)); fx.setStroke(new BasicStroke(2));
                    fx.draw(new Arc2D.Double(x - 28, y - 25, 56, 50, 38, 280, Arc2D.OPEN));
                    drawPixelStar(fx, x - 15, y + 15, 8, new Color(233, 213, 255));
                }
                case "Flying" -> {
                    fx.setColor(new Color(210, 245, 255, 225)); fx.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    fx.draw(new QuadCurve2D.Double(x - 24, y + 5, x - 2, y - 24, x + 24, y - 8));
                    fx.draw(new QuadCurve2D.Double(x - 20, y + 13, x + 2, y - 7, x + 22, y + 2));
                    Path2D feather = new Path2D.Double(); feather.moveTo(x - 9, y + 4); feather.lineTo(x + 20, y - 13);
                    feather.lineTo(x + 10, y + 3); feather.lineTo(x + 17, y + 7); feather.lineTo(x - 9, y + 4); feather.closePath();
                    fx.setColor(base); fx.fill(feather); fx.setColor(new Color(255, 255, 255, 220)); fx.draw(feather);
                }
                case "Rock" -> {
                    for (int i = 0; i < 3; i++) {
                        int sx = x + (i - 1) * 13, sy = y + (i % 2 == 0 ? 5 : -4);
                        Path2D stone = new Path2D.Double(); stone.moveTo(sx - 11, sy + 5); stone.lineTo(sx - 7, sy - 8);
                        stone.lineTo(sx + 2, sy - 15 - i * 2); stone.lineTo(sx + 12, sy - 3); stone.lineTo(sx + 9, sy + 8); stone.closePath();
                        fx.setColor(i == 1 ? new Color(224, 202, 159, 245) : base); fx.fill(stone);
                        fx.setColor(new Color(255, 245, 217, 230)); fx.setStroke(new BasicStroke(1.5f)); fx.draw(stone);
                        fx.drawLine(sx - 4, sy - 5, sx + 2, sy - 10);
                    }
                }
                case "Ground" -> {
                    fx.setColor(new Color(166, 116, 61, 235)); fx.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    fx.draw(new Arc2D.Double(x - 25, y - 12, 50, 26, 12, 300, Arc2D.OPEN));
                    fx.setColor(new Color(228, 183, 105, 220)); fx.setStroke(new BasicStroke(3));
                    fx.draw(new Arc2D.Double(x - 18, y - 7, 36, 15, 12, 300, Arc2D.OPEN));
                    for (int i = 0; i < 5; i++) {
                        int sx = x - 19 + i * 9, sy = y + ((i % 2 == 0) ? -10 : 10);
                        fx.setColor(i % 2 == 0 ? new Color(238, 201, 128, 230) : base);
                        fx.fill(new Ellipse2D.Double(sx - 4, sy - 3, 8, 6));
                    }
                }
                case "Steel" -> {
                    Path2D blade = new Path2D.Double(); blade.moveTo(x - 21, y + 9); blade.lineTo(x + 8, y - 14);
                    blade.lineTo(x + 26, y - 18); blade.lineTo(x + 18, y - 1); blade.lineTo(x - 10, y + 15); blade.closePath();
                    fx.setPaint(new GradientPaint(x - 10, y + 10, new Color(78, 105, 128), x + 21, y - 12, new Color(231, 250, 255)));
                    fx.fill(blade); fx.setColor(new Color(255, 255, 255, 225)); fx.setStroke(new BasicStroke(2)); fx.draw(blade);
                    fx.setColor(new Color(131, 178, 198, 235)); fx.drawLine(x - 17, y + 12, x + 15, y - 11);
                    fx.fill(new Ellipse2D.Double(x - 23, y + 8, 7, 7));
                }
                case "Fighting" -> {
                    fx.setColor(new Color(119, 48, 51, 245)); fx.fill(new java.awt.geom.RoundRectangle2D.Double(x - 18, y - 12, 36, 25, 11, 11));
                    fx.setColor(new Color(213, 96, 82, 250)); fx.fill(new java.awt.geom.RoundRectangle2D.Double(x - 11, y - 15, 29, 23, 10, 10));
                    fx.setColor(new Color(255, 220, 190, 245));
                    for (int i = 0; i < 3; i++) fx.fill(new java.awt.geom.RoundRectangle2D.Double(x + 2 + i * 5, y - 17, 5, 10, 4, 4));
                    fx.setColor(new Color(255, 232, 212, 230)); fx.setStroke(new BasicStroke(2));
                    fx.draw(new Arc2D.Double(x - 27, y - 21, 54, 42, 280, 150, Arc2D.OPEN));
                }
                default -> {
                    fx.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 230));
                    fx.fill(new Ellipse2D.Double(x - 16, y - 16, 32, 32));
                    fx.setColor(new Color(255, 255, 255, 235)); fx.fill(new Ellipse2D.Double(x - 7, y - 9, 11, 11));
                    fx.setColor(new Color(255, 255, 255, 200)); fx.setStroke(new BasicStroke(2));
                    fx.draw(new Ellipse2D.Double(x - 24, y - 18, 48, 36));
                    fx.draw(new Arc2D.Double(x - 31, y - 25, 62, 50, 190, 160, Arc2D.OPEN));
                }
            }
            fx.dispose();
        }

        private void drawTrail(Graphics2D g, String type, int x, int y, int frame) {
            Color c = typeColor(type);
            for (int i = 0; i < 5; i++) {
                int s = 5 + i * 2;
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(25, 150 - i * 25)));
                g.fill(new Ellipse2D.Double(x - i * 11 - s / 2.0, y + (i % 2 == 0 ? -7 : 7) - s / 2.0, s, s));
            }
        }

        private void drawBurst(Graphics2D g, int x, int y, Color c, int frame) {
            int radius = 12 + frame * 5;
            int alpha = Math.max(15, 205 - frame * 18);
            Graphics2D impact = (Graphics2D) g.create();
            impact.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            impact.setPaint(new RadialGradientPaint(new Point2D.Float(x, y), Math.max(18, radius * 1.4f),
                new float[] {0f, .3f, 1f}, new Color[] {
                    new Color(255, 255, 245, Math.max(45, alpha)),
                    new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(20, alpha / 2)),
                    new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}));
            impact.fill(new Ellipse2D.Double(x - radius * 1.4, y - radius * 1.4, radius * 2.8, radius * 2.8));
            impact.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
            impact.setStroke(new BasicStroke(frame < 4 ? 4.5f : 2.5f));
            impact.draw(new Ellipse2D.Double(x - radius, y - radius * .62, radius * 2.0, radius * 1.24));
            impact.setColor(new Color(255, 255, 250, Math.max(25, alpha - 20)));
            impact.setStroke(new BasicStroke(1.5f));
            impact.draw(new Ellipse2D.Double(x - radius * .68, y - radius * .39, radius * 1.36, radius * .78));
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6 + frame * .13;
                double inner = radius * (i % 2 == 0 ? .40 : .57);
                double outer = radius * (i % 2 == 0 ? 1.10 : .92);
                int x1 = x + (int) (Math.cos(a) * inner), y1 = y + (int) (Math.sin(a) * inner);
                int x2 = x + (int) (Math.cos(a) * outer), y2 = y + (int) (Math.sin(a) * outer);
                impact.setColor(i % 3 == 0 ? new Color(255, 255, 236, alpha) : new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
                impact.setStroke(new BasicStroke(i % 3 == 0 ? 2.4f : 1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                impact.drawLine(x1, y1, x2, y2);
                if (i % 2 == 0) impact.fill(new Ellipse2D.Double(x2 - 2.8, y2 - 2.8, 5.6, 5.6));
            }
            int core = Math.max(5, (18 - frame) * 2);
            impact.setColor(new Color(255, 255, 255, Math.max(20, alpha)));
            impact.fill(new Ellipse2D.Double(x - core / 2.0, y - core / 2.0, core, core));
            impact.dispose();
        }

        private void drawPixelSparkles(Graphics2D g, int x, int y, Color color, int count) {
            for (int i = 0; i < Math.min(count, 24); i++) {
                int px = x + (int) (Math.sin(i * 3.7 + tick * .04) * (22 + i % 5 * 9));
                int py = y + (int) (Math.cos(i * 2.3 + tick * .05) * (17 + i % 4 * 8));
                int size = i % 4 == 0 ? 8 : 5;
                Color glow = (i + tick / 8) % 3 == 0 ? Color.WHITE : color;
                g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 48));
                g.fill(new Ellipse2D.Double(px - size, py - size, size * 2.0, size * 2.0));
                g.setColor(glow);
                g.fill(new Ellipse2D.Double(px - size / 2.0, py - size / 2.0, size, size));
                if (i % 3 == 0) {
                    g.setStroke(new BasicStroke(1.5f));
                    g.drawLine(px - size, py, px + size, py);
                    g.drawLine(px, py - size, px, py + size);
                }
            }
        }

        private void drawPixelStar(Graphics2D g, int x, int y, int size, Color c) {
            Path2D star = new Path2D.Double();
            star.moveTo(x, y - size / 2.0); star.lineTo(x + size * .17, y - size * .16);
            star.lineTo(x + size / 2.0, y); star.lineTo(x + size * .17, y + size * .16);
            star.lineTo(x, y + size / 2.0); star.lineTo(x - size * .17, y + size * .16);
            star.lineTo(x - size / 2.0, y); star.lineTo(x - size * .17, y - size * .16); star.closePath();
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
            g.fill(new Ellipse2D.Double(x - size, y - size, size * 2.0, size * 2.0));
            g.setColor(c); g.fill(star);
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
