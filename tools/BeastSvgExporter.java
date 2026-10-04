import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Converts each transparent pixel-art PNG in an input folder into a separate,
 * editable SVG made from crisp vector rectangles. Run with:
 * java tools/BeastSvgExporter.java assets/beasts assets/beasts/svg
 */
public final class BeastSvgExporter {
    private BeastSvgExporter() { }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java tools/BeastSvgExporter.java <png-folder> <svg-folder>");
            System.exit(2);
        }

        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Files.createDirectories(output);

        try (Stream<Path> files = Files.list(input)) {
            for (Path png : files.filter(BeastSvgExporter::isPng).sorted().toList()) {
                BufferedImage image = ImageIO.read(png.toFile());
                if (image == null) {
                    System.err.println("Skipping unreadable image: " + png.getFileName());
                    continue;
                }
                Path svg = output.resolve(stripExtension(png.getFileName().toString()) + ".svg");
                Files.writeString(svg, toSvg(png.getFileName().toString(), image), StandardCharsets.UTF_8);
                System.out.println("Wrote " + svg.getFileName());
            }
        }
    }

    private static boolean isPng(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png");
    }

    private static String stripExtension(String filename) {
        return filename.substring(0, filename.lastIndexOf('.'));
    }

    private static String toSvg(String filename, BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Map<Integer, StringBuilder> pathsByColor = new TreeMap<>(Integer::compareUnsigned);

        for (int y = 0; y < height; y++) {
            int x = 0;
            while (x < width) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) == 0) {
                    x++;
                    continue;
                }

                int start = x++;
                while (x < width && image.getRGB(x, y) == argb) x++;
                int runWidth = x - start;
                pathsByColor.computeIfAbsent(argb, ignored -> new StringBuilder())
                    .append('M').append(start).append(' ').append(y)
                    .append('h').append(runWidth).append("v1h-").append(runWidth).append('z');
            }
        }

        StringBuilder svg = new StringBuilder(16_384);
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            .append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
            .append("\" height=\"").append(height).append("\" viewBox=\"0 0 ")
            .append(width).append(' ').append(height)
            .append("\" shape-rendering=\"crispEdges\" role=\"img\">\n")
            .append("  <title>").append(xmlEscape(stripExtension(filename))).append("</title>\n");

        int index = 0;
        for (Map.Entry<Integer, StringBuilder> entry : pathsByColor.entrySet()) {
            int argb = entry.getKey();
            int alpha = argb >>> 24;
            String fill = String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF);
            svg.append("  <path id=\"palette-").append(String.format(Locale.ROOT, "%03d", ++index))
                .append("\" fill=\"").append(fill).append('"');
            if (alpha < 255) {
                svg.append(" fill-opacity=\"")
                    .append(String.format(Locale.ROOT, "%.4f", alpha / 255.0)).append('"');
            }
            svg.append(" d=\"").append(entry.getValue()).append("\"/>\n");
        }
        return svg.append("</svg>\n").toString();
    }

    private static String xmlEscape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
