package echoesofluma;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A small built-in 5x7 bitmap typeface for sharp, dependency-free game dialogue. */
final class PixelBitmapFont {
    private static final Map<Character, String[]> GLYPHS = new HashMap<>();

    static {
        glyph('A', "01110", "10001", "10001", "11111", "10001", "10001", "10001");
        glyph('B', "11110", "10001", "10001", "11110", "10001", "10001", "11110");
        glyph('C', "01111", "10000", "10000", "10000", "10000", "10000", "01111");
        glyph('D', "11110", "10001", "10001", "10001", "10001", "10001", "11110");
        glyph('E', "11111", "10000", "10000", "11110", "10000", "10000", "11111");
        glyph('F', "11111", "10000", "10000", "11110", "10000", "10000", "10000");
        glyph('G', "01111", "10000", "10000", "10111", "10001", "10001", "01111");
        glyph('H', "10001", "10001", "10001", "11111", "10001", "10001", "10001");
        glyph('I', "11111", "00100", "00100", "00100", "00100", "00100", "11111");
        glyph('J', "00111", "00010", "00010", "00010", "10010", "10010", "01100");
        glyph('K', "10001", "10010", "10100", "11000", "10100", "10010", "10001");
        glyph('L', "10000", "10000", "10000", "10000", "10000", "10000", "11111");
        glyph('M', "10001", "11011", "10101", "10101", "10001", "10001", "10001");
        glyph('N', "10001", "11001", "10101", "10011", "10001", "10001", "10001");
        glyph('O', "01110", "10001", "10001", "10001", "10001", "10001", "01110");
        glyph('P', "11110", "10001", "10001", "11110", "10000", "10000", "10000");
        glyph('Q', "01110", "10001", "10001", "10001", "10101", "10010", "01101");
        glyph('R', "11110", "10001", "10001", "11110", "10100", "10010", "10001");
        glyph('S', "01111", "10000", "10000", "01110", "00001", "00001", "11110");
        glyph('T', "11111", "00100", "00100", "00100", "00100", "00100", "00100");
        glyph('U', "10001", "10001", "10001", "10001", "10001", "10001", "01110");
        glyph('V', "10001", "10001", "10001", "10001", "10001", "01010", "00100");
        glyph('W', "10001", "10001", "10001", "10101", "10101", "10101", "01010");
        glyph('X', "10001", "10001", "01010", "00100", "01010", "10001", "10001");
        glyph('Y', "10001", "10001", "01010", "00100", "00100", "00100", "00100");
        glyph('Z', "11111", "00001", "00010", "00100", "01000", "10000", "11111");

        glyph('0', "01110", "10001", "10011", "10101", "11001", "10001", "01110");
        glyph('1', "00100", "01100", "00100", "00100", "00100", "00100", "01110");
        glyph('2', "01110", "10001", "00001", "00010", "00100", "01000", "11111");
        glyph('3', "11110", "00001", "00001", "01110", "00001", "00001", "11110");
        glyph('4', "00010", "00110", "01010", "10010", "11111", "00010", "00010");
        glyph('5', "11111", "10000", "10000", "11110", "00001", "00001", "11110");
        glyph('6', "01110", "10000", "10000", "11110", "10001", "10001", "01110");
        glyph('7', "11111", "00001", "00010", "00100", "01000", "01000", "01000");
        glyph('8', "01110", "10001", "10001", "01110", "10001", "10001", "01110");
        glyph('9', "01110", "10001", "10001", "01111", "00001", "00001", "01110");

        glyph(' ', "00000", "00000", "00000", "00000", "00000", "00000", "00000");
        glyph('.', "00000", "00000", "00000", "00000", "00000", "00110", "00110");
        glyph(',', "00000", "00000", "00000", "00000", "00110", "00110", "00100");
        glyph(':', "00000", "00110", "00110", "00000", "00110", "00110", "00000");
        glyph(';', "00000", "00110", "00110", "00000", "00110", "00110", "00100");
        glyph('!', "00100", "00100", "00100", "00100", "00100", "00000", "00100");
        glyph('?', "01110", "10001", "00001", "00010", "00100", "00000", "00100");
        glyph('-', "00000", "00000", "00000", "11111", "00000", "00000", "00000");
        glyph('+', "00000", "00100", "00100", "11111", "00100", "00100", "00000");
        glyph('/', "00001", "00010", "00010", "00100", "01000", "01000", "10000");
        glyph('%', "11001", "11010", "00100", "01000", "10110", "00110", "00000");
        glyph('(', "00010", "00100", "01000", "01000", "01000", "00100", "00010");
        glyph(')', "01000", "00100", "00010", "00010", "00010", "00100", "01000");
        glyph('=', "00000", "11111", "00000", "11111", "00000", "00000", "00000");
        glyph('\'', "00100", "00100", "00010", "00000", "00000", "00000", "00000");
        glyph('"', "01010", "01010", "00100", "00000", "00000", "00000", "00000");
        glyph('&', "01100", "10010", "10100", "01000", "10101", "10010", "01101");
        glyph('<', "00010", "00100", "01000", "10000", "01000", "00100", "00010");
        glyph('>', "01000", "00100", "00010", "00001", "00010", "00100", "01000");
        glyph('#', "01010", "11111", "01010", "01010", "11111", "01010", "00000");
        glyph('_', "00000", "00000", "00000", "00000", "00000", "00000", "11111");
        glyph('*', "00000", "10101", "01110", "11111", "01110", "10101", "00000");
    }

    private PixelBitmapFont() { }

    static void drawWrapped(Graphics2D graphics, String text, int x, int y, int maxWidth,
            int maxLines, int scale, Color defaultColor) {
        if (text == null || text.isEmpty() || maxWidth <= 0 || maxLines <= 0) return;
        int pixelScale = Math.max(1, scale);
        Color normal = defaultColor == null ? Color.BLACK : defaultColor;
        List<List<Cell>> lines = wrap(tokenize(text, normal), maxWidth, pixelScale);
        boolean clipped = lines.size() > maxLines;
        if (clipped) lines = new ArrayList<>(lines.subList(0, maxLines));
        if (clipped && !lines.isEmpty()) addEllipsis(lines.get(lines.size() - 1), maxWidth, pixelScale, normal);

        Graphics2D pixels = (Graphics2D) graphics.create();
        pixels.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        pixels.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        int lineTop = y;
        for (List<Cell> line : lines) {
            int cursorX = x;
            for (Cell cell : line) {
                String[] bitmap = GLYPHS.getOrDefault(cell.character, GLYPHS.get('?'));
                pixels.setColor(cell.color);
                for (int row = 0; row < 7; row++) {
                    String bits = bitmap[row];
                    for (int column = 0; column < 5; column++) {
                        if (bits.charAt(column) == '1') {
                            pixels.fillRect(cursorX + column * pixelScale,
                                lineTop + row * pixelScale, pixelScale, pixelScale);
                        }
                    }
                }
                cursorX += advance(cell.character, pixelScale);
            }
            lineTop += pixelScale * 8;
        }
        pixels.dispose();
    }

    static void drawCentered(Graphics2D graphics, String text, int centerX, int y,
            int maxWidth, int scale, Color color) {
        int width = measure(text, Math.max(1, scale));
        drawWrapped(graphics, text, centerX - Math.min(width, maxWidth) / 2, y,
            maxWidth, 1, scale, color);
    }

    private static List<Cell> tokenize(String text, Color defaultColor) {
        List<Cell> result = new ArrayList<>();
        Color color = defaultColor;
        for (int index = 0; index < text.length();) {
            if (text.charAt(index) == '{' && text.indexOf('}', index) < 0) break;
            if (text.startsWith("{BR}", index)) {
                result.add(new Cell('\n', color));
                index += 4;
            } else if (text.startsWith("{/C}", index)) {
                color = defaultColor;
                index += 4;
            } else if (text.startsWith("{C:#", index)) {
                int close = text.indexOf('}', index);
                if (close < 0) break;
                String code = text.substring(index + 4, close);
                if (code.length() == 6) {
                    try { color = new Color(Integer.parseInt(code, 16)); }
                    catch (NumberFormatException ignored) { color = defaultColor; }
                    index = close + 1;
                } else {
                    result.add(new Cell(normalize(text.charAt(index)), color));
                    index++;
                }
            } else {
                char raw = text.charAt(index++);
                if (raw == '\r') continue;
                if (raw == '\n') result.add(new Cell('\n', color));
                else result.add(new Cell(normalize(raw), color));
            }
        }
        return result;
    }

    private static List<List<Cell>> wrap(List<Cell> cells, int maxWidth, int scale) {
        List<List<Cell>> lines = new ArrayList<>();
        List<Cell> current = new ArrayList<>();
        for (Cell cell : cells) {
            if (cell.character == '\n') {
                trimEnd(current);
                lines.add(current);
                current = new ArrayList<>();
                continue;
            }
            if (cell.character == ' ' && current.isEmpty()) continue;
            while (!current.isEmpty() && width(current, scale) + advance(cell.character, scale) > maxWidth) {
                int lastSpace = lastSpace(current);
                if (lastSpace >= 0) {
                    List<Cell> overflow = new ArrayList<>(current.subList(lastSpace + 1, current.size()));
                    List<Cell> finished = new ArrayList<>(current.subList(0, lastSpace));
                    trimEnd(finished);
                    lines.add(finished);
                    current = overflow;
                } else {
                    trimEnd(current);
                    lines.add(current);
                    current = new ArrayList<>();
                }
                if (cell.character == ' ' && current.isEmpty()) break;
            }
            if (cell.character != ' ' || !current.isEmpty()) current.add(cell);
        }
        trimEnd(current);
        if (!current.isEmpty() || lines.isEmpty()) lines.add(current);
        return lines;
    }

    private static void addEllipsis(List<Cell> line, int maxWidth, int scale, Color color) {
        while (!line.isEmpty() && width(line, scale) + advance('.', scale) * 3 > maxWidth) line.remove(line.size() - 1);
        line.add(new Cell('.', color));
        line.add(new Cell('.', color));
        line.add(new Cell('.', color));
    }

    private static int lastSpace(List<Cell> line) {
        for (int i = line.size() - 1; i >= 0; i--) if (line.get(i).character == ' ') return i;
        return -1;
    }

    private static void trimEnd(List<Cell> line) {
        while (!line.isEmpty() && line.get(line.size() - 1).character == ' ') line.remove(line.size() - 1);
    }

    private static int width(List<Cell> line, int scale) {
        int result = 0;
        for (Cell cell : line) result += advance(cell.character, scale);
        return result;
    }

    private static int measure(String text, int scale) {
        int result = 0;
        if (text == null) return result;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') break;
            result += advance(normalize(c), scale);
        }
        return result;
    }

    private static int advance(char character, int scale) {
        return (character == ' ' ? 4 : 6) * scale;
    }

    private static char normalize(char value) {
        return switch (value) {
            case '’', '‘', '`' -> '\'';
            case '“', '”' -> '"';
            case '–', '—', '−' -> '-';
            case '·' -> ' ';
            case '…' -> '.';
            default -> Character.toUpperCase(value);
        };
    }

    private static void glyph(char character, String... rows) {
        GLYPHS.put(character, rows);
    }

    private static final class Cell {
        final char character;
        final Color color;
        Cell(char character, Color color) { this.character = character; this.color = color; }
    }
}
