package echoesofluma;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Loads and smoothly scales the supplied full-color beast illustrations. */
public final class BeastIllustrations {
    private BeastIllustrations() { }

    public static BufferedImage load(String fileName) {
        Path asset = Path.of("assets", "beasts", fileName).normalize();
        try {
            BufferedImage image = null;
            if (Files.isRegularFile(asset)) image = ImageIO.read(new File(asset.toString()));
            if (image == null) {
                String resourcePath = "/assets/beasts/" + fileName.replace('\\', '/');
                try (var resource = BeastIllustrations.class.getResourceAsStream(resourcePath)) {
                    if (resource != null) image = ImageIO.read(resource);
                }
            }
            return image == null ? null : clearConnectedWhiteBackground(image);
        } catch (IOException | RuntimeException ignored) {
            // The game remains launchable if an optional image is missing.
            return null;
        }
    }

    /** Removes only near-white pixels connected to the image edge; enclosed highlights stay intact. */
    private static BufferedImage clearConnectedWhiteBackground(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int length = width * height;
        int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);
        boolean[] background = new boolean[length];
        int[] queue = new int[length];
        int tail = 0;

        for (int x = 0; x < width; x++) {
            tail = enqueueIfBackdrop(pixels, background, queue, tail, x);
            int bottom = (height - 1) * width + x;
            tail = enqueueIfBackdrop(pixels, background, queue, tail, bottom);
        }
        for (int y = 1; y < height - 1; y++) {
            tail = enqueueIfBackdrop(pixels, background, queue, tail, y * width);
            tail = enqueueIfBackdrop(pixels, background, queue, tail, y * width + width - 1);
        }

        int head = 0;
        while (head < tail) {
            int index = queue[head++];
            int x = index % width;
            if (x > 0) tail = enqueueIfBackdrop(pixels, background, queue, tail, index - 1);
            if (x + 1 < width) tail = enqueueIfBackdrop(pixels, background, queue, tail, index + 1);
            if (index >= width) tail = enqueueIfBackdrop(pixels, background, queue, tail, index - width);
            if (index + width < length) tail = enqueueIfBackdrop(pixels, background, queue, tail, index + width);
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
        return cutout;
    }

    private static int enqueueIfBackdrop(int[] pixels, boolean[] background, int[] queue, int tail, int index) {
        if (!background[index] && isNearWhite(pixels[index])) {
            background[index] = true;
            queue[tail++] = index;
        }
        return tail;
    }

    private static boolean isNearWhite(int pixel) {
        int alpha = (pixel >>> 24) & 0xff;
        int red = (pixel >>> 16) & 0xff;
        int green = (pixel >>> 8) & 0xff;
        int blue = pixel & 0xff;
        int spread = Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue));
        return alpha > 8 && red >= 244 && green >= 244 && blue >= 244 && spread <= 16;
    }

    public static void draw(Graphics2D graphics, BufferedImage image,
            int x, int y, int width, int height, float alpha, boolean flip) {
        if (image == null || width <= 0 || height <= 0 || alpha <= 0) return;
        double fit = Math.min(width / (double) image.getWidth(), height / (double) image.getHeight());
        int drawWidth = Math.max(1, (int) Math.round(image.getWidth() * fit));
        int drawHeight = Math.max(1, (int) Math.round(image.getHeight() * fit));
        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;
        Graphics2D art = (Graphics2D) graphics.create();
        art.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        art.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
        if (flip) art.drawImage(image, drawX + drawWidth, drawY, -drawWidth, drawHeight, null);
        else art.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
        art.dispose();
    }
}