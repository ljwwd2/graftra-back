package com.volcengine.imagegen.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Random;

/**
 * Captcha generator utility
 */
@Slf4j
@Component
public class CaptchaGenerator {

    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 4;
    private static final int IMAGE_WIDTH = 120;
    private static final int IMAGE_HEIGHT = 44;
    private static final int FONT_SIZE = 24;
    private static final int INTERFERENCE_LINES = 5;
    private static final int NOISE_DOTS = 40;

    private final Random random = new Random();

    /**
     * Generate captcha with image
     */
    public CaptchaResult generateCaptcha(String ipAddress) {
        // Generate code
        String code = generateCode();

        // Generate image
        String base64Image = generateImage(code);

        // Calculate expiration (5 minutes)
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        log.debug("Generated captcha for IP: {}, expires at: {}", ipAddress, expiresAt);

        return new CaptchaResult(code, base64Image, expiresAt);
    }

    /**
     * Generate random captcha code
     */
    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return code.toString();
    }

    /**
     * Generate captcha image as Base64
     */
    private String generateImage(String code) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        try {
            // Enable anti-aliasing
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw background
            g2d.setColor(randomLightColor());
            g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

            // Draw interference lines
            for (int i = 0; i < INTERFERENCE_LINES; i++) {
                g2d.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256), 100));
                g2d.drawLine(
                        random.nextInt(IMAGE_WIDTH),
                        random.nextInt(IMAGE_HEIGHT),
                        random.nextInt(IMAGE_WIDTH),
                        random.nextInt(IMAGE_HEIGHT)
                );
            }

            // Draw noise dots
            for (int i = 0; i < NOISE_DOTS; i++) {
                g2d.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256), 80));
                g2d.fillOval(random.nextInt(IMAGE_WIDTH), random.nextInt(IMAGE_HEIGHT), 2, 2);
            }

            // Draw captcha code
            g2d.setFont(new Font("Arial", Font.BOLD, FONT_SIZE));
            int startX = 20;
            int startY = 30;
            int charSpacing = 22;

            for (int i = 0; i < code.length(); i++) {
                // Save current transformation
                AffineTransform originalTransform = g2d.getTransform();

                // Random rotation (-15 to +15 degrees)
                double angle = (random.nextDouble() - 0.5) * 0.52; // ~30 degrees in radians
                g2d.rotate(angle, startX + i * charSpacing, startY);

                // Draw character
                g2d.setColor(randomColor());
                g2d.drawString(String.valueOf(code.charAt(i)), startX + i * charSpacing, startY);

                // Restore transformation
                g2d.setTransform(originalTransform);
            }

        } finally {
            g2d.dispose();
        }

        // Convert to Base64
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            log.error("Failed to generate captcha image", e);
            throw new RuntimeException("Failed to generate captcha image", e);
        }
    }

    /**
     * Generate random color for characters
     */
    private Color randomColor() {
        return new Color(
                random.nextInt(150),
                random.nextInt(150),
                random.nextInt(150)
        );
    }

    /**
     * Generate random light color for background
     */
    private Color randomLightColor() {
        return new Color(
                220 + random.nextInt(36),
                220 + random.nextInt(36),
                220 + random.nextInt(36)
        );
    }

    /**
     * Captcha generation result
     */
    public record CaptchaResult(
            String code,
            String base64Image,
            LocalDateTime expiresAt
    ) {}
}
