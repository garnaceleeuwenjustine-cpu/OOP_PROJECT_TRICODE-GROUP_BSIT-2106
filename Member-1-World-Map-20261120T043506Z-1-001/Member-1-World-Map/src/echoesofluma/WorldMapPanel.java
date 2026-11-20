package echoesofluma;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.function.BiConsumer;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * Embedded world-map exploration adapted from the group's EchoesOfLuma.java.
 * It is a screen panel so the map shares the main game's frame and navigation.
 */
public final class WorldMapPanel extends JPanel {
    private static final double START_FOREST_X = 40;
    private static final double START_FOREST_Y = 66;
    private static final Color INK = new Color(24, 43, 48);
    private static final Color GREEN = new Color(38, 105, 78);
    private static final Color GOLD = new Color(240, 195, 92);
    private static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    private static final Font BODY_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 14);

    private static final class Location {
        final String name;
        final String description;
        final double x;
        final double y;
        final String roadNodeId;

        Location(String name, String description, double x, double y, String roadNodeId) {
            this.name = name;
            this.description = description;
            this.x = x;
            this.y = y;
            this.roadNodeId = roadNodeId;
        }
    }

    private static final class RoadNode {
        final String id;
        final double x;
        final double y;
        final List<String> neighbors = new ArrayList<>();

        RoadNode(String id, double x, double y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    private static final class Waypoint {
        final double x;
        final double y;

        Waypoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private final Map<String, Location> locations = new LinkedHashMap<>();
    private final Map<String, RoadNode> roadNetwork = new LinkedHashMap<>();
    private final MapCanvas mapCanvas = new MapCanvas();
    private final JLabel selectedLocation = new JLabel("Starting Forest", SwingConstants.LEFT);
    private final JLabel description = new JLabel("<html>Your journey begins on the forest road.</html>");
    private final JLabel trainerName = new JLabel("TRAINER  ·  ADVENTURER");
    private final List<JButton> destinationButtons = new ArrayList<>();
    private final List<JButton> stageButtons = new ArrayList<>();
    private final JPanel stagesGrid = new JPanel(new java.awt.GridLayout(6, 5, 4, 4));
    private final JLabel stageHeader = new JLabel("STARTING FOREST · LEVELS");
    private final JLabel stageProgress = new JLabel("0 / 30 complete");
    private final Runnable onBack;
    private final Runnable onOpenBag;
    private final BiConsumer<Integer, Integer> onStartStage;
    private final int[] completedStages = new int[5];
    private int selectedRegion;
    private boolean standingAtSelectedRegion = true;

    public WorldMapPanel(Runnable onBack, Runnable onOpenBag, BiConsumer<Integer, Integer> onStartStage) {
        this.onBack = onBack;
        this.onOpenBag = onOpenBag;
        this.onStartStage = onStartStage;
        setLayout(new BorderLayout(14, 0));
        setBackground(new Color(18, 57, 55));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        createRoadNetwork();
        createLocations();
        mapCanvas.setPlayerAt(locations.get("Starting Forest"));
        arriveAt(locations.get("Starting Forest"));
        add(mapCanvas, BorderLayout.CENTER);
        add(createSidebar(), BorderLayout.EAST);
        setPreferredSize(new Dimension(960, 540));
        refreshProgressionControls();
    }

    /** Supplies the current player portrait used for the moving map marker. */
    public void setTrainer(BufferedImage portrait, String name) {
        mapCanvas.setTrainerArt(portrait);
        trainerName.setText("TRAINER  ·  " + (name == null ? "ADVENTURER" : name.toUpperCase()));
        repaint();
    }

    /** Progress values are the highest cleared stage in each region, from 0 to 30. */
    public void setProgress(int[] progress) {
        Arrays.fill(completedStages, 0);
        if (progress != null) System.arraycopy(progress, 0, completedStages, 0, Math.min(progress.length, completedStages.length));
        refreshProgressionControls();
        mapCanvas.repaint();
    }

    private void createLocations() {
        // Percent coordinates are relative to the square map illustration.
        // Starting Forest is the first destination and the trainer's spawn point.
        locations.put("Starting Forest", new Location("Starting Forest",
            "Your journey begins in a quiet woodland beside the King's Road.", START_FOREST_X, START_FOREST_Y, "starting_forest"));
        locations.put("Luma Village", new Location("Luma Village",
            "A harbor village tucked among the green hills.", 25, 77, "luma_village"));
        locations.put("Whispering Forest", new Location("Whispering Forest",
            "Follow the old woodland paths and listen for hidden echoes.", 30, 21, "whispering_forest"));
        locations.put("Ember Mountain", new Location("Ember Mountain",
            "A volcanic highland with warm caves and a view across Luma.", 79, 24, "ember_mountain"));
        locations.put("Ethics Academy", new Location("Ethics Academy",
            "Visit the academy to explore lessons about choices and values.", 50, 40, "ethics_academy"));
    }

    /** Road centerline nodes traced along the paths painted on the world map. */
    private void createRoadNetwork() {
        addRoadNode("luma_village", 25, 77);
        addRoadNode("village_gate", 28, 73);
        addRoadNode("village_bend", 31, 69);
        addRoadNode("village_road", 35, 68);
        addRoadNode("starting_forest", START_FOREST_X, START_FOREST_Y);
        addRoadNode("south_road_bend", 45, 64);
        addRoadNode("south_road", 48, 61);
        addRoadNode("academy_road_1", 49, 56);
        addRoadNode("academy_road_2", 49, 50);
        addRoadNode("academy_road_3", 47, 47);
        addRoadNode("academy_road_4", 45, 44);
        addRoadNode("ethics_academy", 50, 40);
        addRoadNode("whisper_road_1", 46, 36);
        addRoadNode("whisper_road_2", 42, 33);
        addRoadNode("whisper_road_3", 37, 30);
        addRoadNode("whisper_road_4", 33, 27);
        addRoadNode("whispering_forest", 30, 21);
        addRoadNode("ember_road_1", 55, 38);
        addRoadNode("ember_road_2", 61, 35);
        addRoadNode("ember_road_3", 68, 32);
        addRoadNode("ember_road_4", 73, 28);
        addRoadNode("ember_mountain", 79, 24);

        connectRoad("luma_village", "village_gate");
        connectRoad("village_gate", "village_bend");
        connectRoad("village_bend", "village_road");
        connectRoad("village_road", "starting_forest");
        connectRoad("starting_forest", "south_road_bend");
        connectRoad("south_road_bend", "south_road");
        connectRoad("south_road", "academy_road_1");
        connectRoad("academy_road_1", "academy_road_2");
        connectRoad("academy_road_2", "academy_road_3");
        connectRoad("academy_road_3", "academy_road_4");
        connectRoad("academy_road_4", "ethics_academy");
        connectRoad("ethics_academy", "whisper_road_1");
        connectRoad("whisper_road_1", "whisper_road_2");
        connectRoad("whisper_road_2", "whisper_road_3");
        connectRoad("whisper_road_3", "whisper_road_4");
        connectRoad("whisper_road_4", "whispering_forest");
        connectRoad("ethics_academy", "ember_road_1");
        connectRoad("ember_road_1", "ember_road_2");
        connectRoad("ember_road_2", "ember_road_3");
        connectRoad("ember_road_3", "ember_road_4");
        connectRoad("ember_road_4", "ember_mountain");
    }

    private void addRoadNode(String id, double x, double y) {
        roadNetwork.put(id, new RoadNode(id, x, y));
    }

    private void connectRoad(String firstId, String secondId) {
        RoadNode first = roadNetwork.get(firstId);
        RoadNode second = roadNetwork.get(secondId);
        first.neighbors.add(secondId);
        second.neighbors.add(firstId);
    }

    private javax.swing.JScrollPane createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(244, 246, 226));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(219, 194, 126), 3),
            BorderFactory.createEmptyBorder(20, 18, 18, 18)));
        sidebar.setPreferredSize(new Dimension(292, 0));

        JLabel kicker = new JLabel("ECHOES OF LUMA");
        kicker.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        kicker.setForeground(GREEN);
        kicker.setAlignmentX(LEFT_ALIGNMENT);
        JLabel title = new JLabel("World Map");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27));
        title.setForeground(INK);
        title.setAlignmentX(LEFT_ALIGNMENT);
        trainerName.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        trainerName.setForeground(new Color(100, 111, 100));
        trainerName.setAlignmentX(LEFT_ALIGNMENT);

        sidebar.add(kicker);
        sidebar.add(Box.createVerticalStrut(3));
        sidebar.add(title);
        sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(trainerName);
        sidebar.add(Box.createVerticalStrut(20));

        JLabel destinationsLabel = new JLabel("DESTINATIONS");
        destinationsLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        destinationsLabel.setForeground(new Color(90, 112, 92));
        destinationsLabel.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(destinationsLabel);
        sidebar.add(Box.createVerticalStrut(8));

        int number = 1;
        for (Location location : locations.values()) {
            JButton destination = new JButton(String.format("%02d   %s", number++, location.name));
            destination.setFont(BODY_BOLD);
            destination.setForeground(INK);
            destination.setBackground(Color.WHITE);
            destination.setHorizontalAlignment(SwingConstants.LEFT);
            destination.setFocusPainted(false);
            destination.setOpaque(true);
            destination.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(198, 210, 182)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            destination.setAlignmentX(LEFT_ALIGNMENT);
            destination.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            destination.addActionListener(event -> travelTo(location.name));
            destinationButtons.add(destination);
            sidebar.add(destination);
            sidebar.add(Box.createVerticalStrut(6));
        }

        sidebar.add(Box.createVerticalStrut(8));
        stageHeader.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        stageHeader.setForeground(GREEN);
        stageHeader.setAlignmentX(LEFT_ALIGNMENT);
        stageProgress.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        stageProgress.setForeground(new Color(100, 111, 100));
        stageProgress.setAlignmentX(LEFT_ALIGNMENT);
        stagesGrid.setOpaque(false);
        stagesGrid.setAlignmentX(LEFT_ALIGNMENT);
        stagesGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 168));
        for (int stage = 1; stage <= 30; stage++) {
            int stageNumber = stage;
            JButton stageButton = new JButton();
            stageButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            stageButton.setMargin(new java.awt.Insets(3, 0, 3, 0));
            stageButton.setFocusPainted(false);
            stageButton.addActionListener(event -> onStartStage.accept(selectedRegion, stageNumber));
            stageButtons.add(stageButton);
            stagesGrid.add(stageButton);
        }
        sidebar.add(stageHeader);
        sidebar.add(Box.createVerticalStrut(3));
        sidebar.add(stageProgress);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(stagesGrid);

        sidebar.add(Box.createVerticalStrut(9));
        JPanel infoCard = new JPanel();
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        infoCard.setBackground(new Color(231, 239, 215));
        infoCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(185, 204, 166)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        infoCard.setAlignmentX(LEFT_ALIGNMENT);
        infoCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        JLabel here = new JLabel("CURRENT LOCATION");
        here.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        here.setForeground(GREEN);
        selectedLocation.setFont(BODY_BOLD);
        selectedLocation.setForeground(INK);
        selectedLocation.setAlignmentX(LEFT_ALIGNMENT);
        description.setFont(BODY);
        description.setForeground(new Color(69, 84, 72));
        description.setAlignmentX(LEFT_ALIGNMENT);
        infoCard.add(here);
        infoCard.add(Box.createVerticalStrut(5));
        infoCard.add(selectedLocation);
        infoCard.add(Box.createVerticalStrut(4));
        infoCard.add(description);
        sidebar.add(infoCard);

        sidebar.add(Box.createVerticalStrut(8));
        JButton bag = new JButton("🎒  Open Bag");
        bag.setFont(BODY_BOLD);
        bag.setForeground(INK);
        bag.setBackground(new Color(224, 237, 249));
        bag.setFocusPainted(false);
        bag.setAlignmentX(LEFT_ALIGNMENT);
        bag.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        bag.addActionListener(event -> onOpenBag.run());
        sidebar.add(bag);

        sidebar.add(Box.createVerticalGlue());
        JButton back = new JButton("←  Back");
        back.setFont(BODY_BOLD);
        back.setForeground(Color.WHITE);
        back.setBackground(new Color(29, 87, 76));
        back.setOpaque(true);
        back.setFocusPainted(false);
        back.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        back.setAlignmentX(LEFT_ALIGNMENT);
        back.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        back.addActionListener(event -> {
            mapCanvas.stopMovement();
            onBack.run();
        });
        sidebar.add(back);
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(sidebar);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(310, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getViewport().setBackground(new Color(244, 246, 226));
        return scroll;
    }

    private void refreshProgressionControls() {
        int unlockedRegion = 0;
        while (unlockedRegion + 1 < completedStages.length && completedStages[unlockedRegion] >= 30) unlockedRegion++;
        int regionIndex = 0;
        for (JButton button : destinationButtons) {
            boolean unlocked = regionIndex <= unlockedRegion;
            button.setEnabled(unlocked);
            button.setText(String.format("%02d   %s%s", regionIndex + 1,
                new ArrayList<>(locations.values()).get(regionIndex).name, unlocked ? "" : "   🔒"));
            regionIndex++;
        }
        Location selected = new ArrayList<>(locations.values()).get(selectedRegion);
        stageHeader.setText((selectedRegion + 1) + " · " + selected.name.toUpperCase() + " · LEVELS");
        stageProgress.setText(completedStages[selectedRegion] + " / 30 complete" + (completedStages[selectedRegion] >= 30 ? "  ·  AREA CLEARED" : ""));
        boolean regionUnlocked = selectedRegion <= unlockedRegion;
        for (int i = 0; i < stageButtons.size(); i++) {
            int stage = i + 1;
            JButton button = stageButtons.get(i);
            boolean boss = stage % 5 == 0;
            boolean available = regionUnlocked && stage <= completedStages[selectedRegion] + 1;
            button.setText((selectedRegion + 1) + "." + stage + (boss ? " ★" : ""));
            button.setEnabled(available && standingAtSelectedRegion);
            button.setBackground(boss ? new Color(255, 235, 203) : new Color(255, 255, 255));
            button.setForeground(available ? INK : new Color(140, 147, 137));
            button.setToolTipText(boss ? "Strong beast and a harder ethics dilemma" : "Stage " + (selectedRegion + 1) + "." + stage);
        }
        for (JButton button : destinationButtons) button.repaint();
    }

    private void travelTo(String destination) {
        Location target = locations.get(destination);
        if (target == null) return;
        int region = new ArrayList<>(locations.values()).indexOf(target);
        if (!isRegionUnlocked(region)) {
            selectedLocation.setText("Region locked");
            description.setText("<html>Clear all 30 levels in the previous region to unlock this road.</html>");
            return;
        }
        selectedRegion = region;
        standingAtSelectedRegion = false;
        refreshProgressionControls();
        selectedLocation.setText("Traveling to " + target.name + "…");
        description.setText("<html>Following the King's Road to " + target.name + ".</html>");
        mapCanvas.moveTo(target);
    }

    private void arriveAt(Location location) {
        selectedLocation.setText(location.name);
        description.setText("<html>" + location.description + "</html>");
        selectedRegion = new ArrayList<>(locations.values()).indexOf(location);
        standingAtSelectedRegion = true;
        refreshProgressionControls();
    }

    private boolean isRegionUnlocked(int region) {
        return region == 0 || (region > 0 && completedStages[region - 1] >= 30);
    }

    private final class MapCanvas extends JPanel {
        private BufferedImage mapImage;
        private BufferedImage trainerArt;
        private double playerX = START_FOREST_X;
        private double playerY = START_FOREST_Y;
        private List<Waypoint> path = List.of();
        private int waypointIndex;
        private double progress;
        private Timer movementTimer;

        MapCanvas() {
            setBackground(new Color(33, 67, 55));
            setPreferredSize(new Dimension(600, 600));
            setToolTipText("Choose one of the glowing map markers to travel.");
            loadMap();
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent event) {
                    Location clicked = locationAt(event.getX(), event.getY());
                    if (clicked != null) travelTo(clicked.name);
                }
            });
        }

        void setTrainerArt(BufferedImage art) {
            trainerArt = art;
            repaint();
        }

        void setPlayerAt(Location location) {
            if (location == null) return;
            playerX = location.x;
            playerY = location.y;
            repaint();
        }

        private void loadMap() {
            Path path = Path.of("assets", "maps", "luma-world-map.png");
            try {
                if (Files.isRegularFile(path)) mapImage = ImageIO.read(path.toFile());
                if (mapImage == null) {
                    try (var resource = WorldMapPanel.class.getResourceAsStream("/assets/maps/luma-world-map.png")) {
                        if (resource != null) mapImage = ImageIO.read(resource);
                    }
                }
            } catch (IOException | RuntimeException ignored) {
                mapImage = null;
            }
        }

        private Location locationAt(int mouseX, int mouseY) {
            Dimension image = imageSize();
            int side = image.width;
            int left = (getWidth() - side) / 2;
            int top = (getHeight() - side) / 2;
            double clickedX = (mouseX - left) * 100.0 / side;
            double clickedY = (mouseY - top) * 100.0 / side;
            if (clickedX < 0 || clickedY < 0 || clickedX > 100 || clickedY > 100) return null;
            for (Location location : locations.values()) {
                double dx = clickedX - location.x;
                double dy = clickedY - location.y;
                if (Math.hypot(dx, dy) <= 3.4) {
                    int region = new ArrayList<>(locations.values()).indexOf(location);
                    return isRegionUnlocked(region) ? location : null;
                }
            }
            return null;
        }

        private void moveTo(Location target) {
            if (movementTimer != null) movementTimer.stop();
            if (Math.hypot(playerX - target.x, playerY - target.y) < 0.5) {
                path = List.of();
                arriveAt(target);
                repaint();
                return;
            }
            path = buildRoadRoute(target);
            waypointIndex = 0;
            progress = 0;
            if (path.size() < 2) {
                arriveAt(target);
                return;
            }
            movementTimer = new Timer(24, event -> {
                if (waypointIndex >= path.size() - 1) {
                    movementTimer.stop();
                    playerX = target.x;
                    playerY = target.y;
                    path = List.of();
                    arriveAt(target);
                    repaint();
                    return;
                }
                Waypoint start = path.get(waypointIndex);
                Waypoint end = path.get(waypointIndex + 1);
                progress += 0.045;
                if (progress >= 1) {
                    progress = 0;
                    waypointIndex++;
                } else {
                    playerX = start.x + (end.x - start.x) * progress;
                    playerY = start.y + (end.y - start.y) * progress;
                }
                repaint();
            });
            movementTimer.start();
        }

        private void stopMovement() {
            if (movementTimer != null) movementTimer.stop();
            path = List.of();
            repaint();
        }

        private List<Waypoint> buildRoadRoute(Location target) {
            RoadNode goal = roadNetwork.get(target.roadNodeId);
            RoadNode start = nearestRoadNode(playerX, playerY);
            if (goal == null || start == null) return List.of(new Waypoint(playerX, playerY), new Waypoint(target.x, target.y));

            List<String> roadNodes = shortestRoadPath(start.id, goal.id);
            List<Waypoint> result = new ArrayList<>();
            if (Math.hypot(playerX - start.x, playerY - start.y) > 0.08) {
                result.add(new Waypoint(playerX, playerY));
            }
            for (String nodeId : roadNodes) {
                RoadNode node = roadNetwork.get(nodeId);
                if (node != null) addDistinctWaypoint(result, node.x, node.y);
            }
            addDistinctWaypoint(result, target.x, target.y);
            return result;
        }

        private RoadNode nearestRoadNode(double x, double y) {
            RoadNode closest = null;
            double bestDistance = Double.POSITIVE_INFINITY;
            for (RoadNode node : roadNetwork.values()) {
                double distance = Math.hypot(x - node.x, y - node.y);
                if (distance < bestDistance) {
                    closest = node;
                    bestDistance = distance;
                }
            }
            return closest;
        }

        private List<String> shortestRoadPath(String startId, String goalId) {
            Map<String, Double> distance = new HashMap<>();
            Map<String, String> previous = new HashMap<>();
            Set<String> visited = new HashSet<>();
            for (String id : roadNetwork.keySet()) distance.put(id, Double.POSITIVE_INFINITY);
            distance.put(startId, 0.0);

            while (visited.size() < roadNetwork.size()) {
                String current = null;
                double bestDistance = Double.POSITIVE_INFINITY;
                for (Map.Entry<String, Double> entry : distance.entrySet()) {
                    if (!visited.contains(entry.getKey()) && entry.getValue() < bestDistance) {
                        current = entry.getKey();
                        bestDistance = entry.getValue();
                    }
                }
                if (current == null) break;
                if (current.equals(goalId)) break;
                visited.add(current);

                RoadNode currentNode = roadNetwork.get(current);
                for (String neighborId : currentNode.neighbors) {
                    if (visited.contains(neighborId)) continue;
                    RoadNode neighbor = roadNetwork.get(neighborId);
                    double nextDistance = bestDistance + Math.hypot(
                        currentNode.x - neighbor.x, currentNode.y - neighbor.y);
                    if (nextDistance < distance.get(neighborId)) {
                        distance.put(neighborId, nextDistance);
                        previous.put(neighborId, current);
                    }
                }
            }

            List<String> route = new ArrayList<>();
            String step = goalId;
            while (step != null) {
                route.add(step);
                if (step.equals(startId)) break;
                step = previous.get(step);
            }
            if (route.isEmpty() || !route.get(route.size() - 1).equals(startId)) {
                return List.of(startId, goalId);
            }
            Collections.reverse(route);
            return route;
        }

        private void addDistinctWaypoint(List<Waypoint> route, double x, double y) {
            if (route.isEmpty()) {
                route.add(new Waypoint(x, y));
                return;
            }
            Waypoint last = route.get(route.size() - 1);
            if (Math.hypot(last.x - x, last.y - y) > 0.08) route.add(new Waypoint(x, y));
        }

        private Dimension imageSize() {
            int available = Math.max(1, Math.min(getWidth() - 20, getHeight() - 20));
            if (mapImage == null) return new Dimension(available, available);
            double scale = Math.min(available / (double) mapImage.getWidth(), available / (double) mapImage.getHeight());
            return new Dimension(Math.max(1, (int) (mapImage.getWidth() * scale)),
                Math.max(1, (int) (mapImage.getHeight() * scale)));
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            Dimension size = imageSize();
            int left = (getWidth() - size.width) / 2;
            int top = (getHeight() - size.height) / 2;
            g.setColor(new Color(9, 29, 34, 115));
            g.fillRoundRect(left - 5, top - 5, size.width + 10, size.height + 10, 20, 20);
            if (mapImage != null) {
                g.drawImage(mapImage, left, top, size.width, size.height, this);
            } else {
                drawFallbackMap(g, left, top, size.width, size.height);
            }
            drawLocationMarkers(g, left, top, size.width, size.height);
            drawTrainer(g, left, top, size.width, size.height);
            g.setColor(new Color(246, 239, 209));
            g.setStroke(new BasicStroke(2.5f));
            g.drawRoundRect(left, top, size.width, size.height, 16, 16);
            g.dispose();
        }

        private void drawLocationMarkers(Graphics2D g, int left, int top, int width, int height) {
            Location start = locations.get("Starting Forest");
            if (start == null) return;
            int x = left + (int) (start.x * width / 100.0);
            int y = top + (int) (start.y * height / 100.0);
            g.setColor(new Color(42, 137, 89, 145));
            g.fill(new Ellipse2D.Double(x - 16, y - 16, 32, 32));
            g.setColor(new Color(24, 65, 48, 230));
            g.fill(new Ellipse2D.Double(x - 9, y - 9, 18, 18));
            g.setColor(new Color(164, 235, 143));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(x - 9, y - 9, 18, 18));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
            g.setColor(Color.WHITE);
            g.drawString("S", x - 3, y + 3);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
            g.setColor(new Color(19, 48, 37));
            g.drawString("START", x + 12, y - 9);
            g.setColor(new Color(255, 248, 219));
            g.drawString("START", x + 11, y - 10);
        }

        private void drawTrainer(Graphics2D g, int left, int top, int width, int height) {
            int x = left + (int) (playerX * width / 100.0);
            int y = top + (int) (playerY * height / 100.0);
            g.setColor(new Color(12, 35, 32, 130));
            g.fillOval(x - 20, y + 24, 40, 12);
            if (trainerArt != null) {
                BeastIllustrations.draw(g, trainerArt, x - 31, y - 66, 62, 91, 1f, false);
            } else {
                g.setColor(new Color(30, 49, 62));
                g.fillOval(x - 12, y - 20, 24, 24);
                g.setColor(new Color(235, 198, 157));
                g.fillOval(x - 8, y - 17, 16, 17);
                g.setColor(new Color(49, 111, 145));
                g.fillRoundRect(x - 13, y + 1, 26, 26, 8, 8);
                g.setColor(new Color(26, 46, 58));
                g.fillRect(x - 10, y + 24, 8, 12);
                g.fillRect(x + 2, y + 24, 8, 12);
            }
        }

        private void drawFallbackMap(Graphics2D g, int x, int y, int width, int height) {
            g.setPaint(new GradientPaint(x, y, new Color(143, 191, 185), x, y + height, new Color(53, 113, 111)));
            g.fillRect(x, y, width, height);
            g.setColor(new Color(228, 210, 153));
            g.fillOval(x + width / 8, y + height / 5, width * 3 / 5, height * 3 / 5);
            g.setColor(new Color(79, 145, 95));
            g.fillOval(x + width / 7, y + height / 6, width / 3, height / 3);
            g.fillOval(x + width / 2, y + height / 4, width / 3, height / 3);
            g.fillOval(x + width / 3, y + height / 2, width / 3, height / 3);
            g.setColor(new Color(255, 250, 230));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            g.drawString("Luma World Map", x + 18, y + 32);
        }
    }
}
