package echoesofluma;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

/** Plays a horizontal PNG sprite sheet as a looping animation. */
public final class BeastAnimator {
    private final BufferedImage spriteSheet;
    private final int frameCount;
    private final int frameWidth;
    private final int frameHeight;
    private final double secondsPerFrame;

    private int currentFrame;
    private double elapsedInFrame;

    public BeastAnimator(BufferedImage spriteSheet, int frameCount, double secondsPerFrame) {
        if (spriteSheet == null) {
            throw new IllegalArgumentException("spriteSheet must not be null");
        }
        if (frameCount < 1 || spriteSheet.getWidth() % frameCount != 0) {
            throw new IllegalArgumentException("Sheet width must divide evenly by frameCount");
        }
        if (secondsPerFrame <= 0) {
            throw new IllegalArgumentException("secondsPerFrame must be positive");
        }

        this.spriteSheet = spriteSheet;
        this.frameCount = frameCount;
        this.frameWidth = spriteSheet.getWidth() / frameCount;
        this.frameHeight = spriteSheet.getHeight();
        this.secondsPerFrame = secondsPerFrame;
    }

    /** Loads a sheet from the classpath, such as /spritesheets/01-pebblit-idle-sheet.png. */
    public static BeastAnimator fromResource(
            Class<?> resourceOwner,
            String resourcePath,
            int frameCount,
            double secondsPerFrame) throws IOException {
        URL resource = resourceOwner.getResource(resourcePath);
        if (resource == null) {
            throw new IOException("Sprite sheet not found on classpath: " + resourcePath);
        }
        BufferedImage image = ImageIO.read(resource);
        if (image == null) {
            throw new IOException("Could not read sprite sheet: " + resourcePath);
        }
        return new BeastAnimator(image, frameCount, secondsPerFrame);
    }

    /** Loads a sheet from a project-relative file path. */
    public static BeastAnimator fromFile(
            String filePath,
            int frameCount,
            double secondsPerFrame) throws IOException {
        BufferedImage image = ImageIO.read(new File(filePath));
        if (image == null) {
            throw new IOException("Could not read sprite sheet: " + filePath);
        }
        return new BeastAnimator(image, frameCount, secondsPerFrame);
    }

    /** Advance the animation by the elapsed time from the game's update loop. */
    public void update(double deltaSeconds) {
        elapsedInFrame += Math.max(0.0, deltaSeconds);
        while (elapsedInFrame >= secondsPerFrame) {
            elapsedInFrame -= secondsPerFrame;
            currentFrame = (currentFrame + 1) % frameCount;
        }
    }

    /** Draw at native sprite dimensions multiplied by an integer scale. */
    public void draw(Graphics2D graphics, int x, int y, int scale) {
        draw(graphics, x, y, frameWidth * scale, frameHeight * scale);
    }

    /** Draw the current frame stretched to the requested size. */
    public void draw(Graphics2D graphics, int x, int y, int width, int height) {
        int sourceX = currentFrame * frameWidth;
        graphics.drawImage(
                spriteSheet,
                x, y, x + width, y + height,
                sourceX, 0, sourceX + frameWidth, frameHeight,
                null);
    }

    public int getFrameIndex() {
        return currentFrame;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }
}
