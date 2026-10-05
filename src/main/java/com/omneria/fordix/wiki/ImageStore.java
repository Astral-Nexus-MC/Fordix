package com.omneria.fordix.wiki;

import com.omneria.fordix.guide.PageMarkup;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The pictures of guide pages: the files of {@code config/fordix/images} (put there by admins or saved by the wiki
 * importer), served to clients as small PNGs. Whatever the file format (PNG, JPEG, GIF), a picture is re-encoded to PNG
 * and scaled down to fit {@link #MAX_SIDE}, because the game only reads PNG and a network packet should stay small.
 */
public final class ImageStore {
    public static final int MAX_SIDE = 640;
    public static final int MAX_FILE_BYTES = 6_000_000;
    private static final int CACHE_BYTES = 24_000_000;

    private final Path root;
    private final Map<String, byte[]> cache = new LinkedHashMap<>(16, 0.75F, true);
    private int cached;

    public ImageStore(Path root) {
        this.root = root;
    }

    public boolean exists(String name) {
        return PageMarkup.isImageName(name) && Files.isRegularFile(root.resolve(name));
    }

    /** The picture as a PNG for the network, or null when the name is not valid, the file is missing or not an image. */
    public synchronized byte[] png(String name) {
        if (!PageMarkup.isImageName(name)) {
            return null;
        }
        byte[] known = cache.get(name);
        if (known != null) {
            return known;
        }
        Path file = root.resolve(name);
        try {
            if (!Files.isRegularFile(file) || Files.size(file) > MAX_FILE_BYTES) {
                return null;
            }
            byte[] png = toPng(Files.readAllBytes(file));
            if (png != null) {
                cache.put(name, png);
                cached += png.length;
                while (cached > CACHE_BYTES && !cache.isEmpty()) {
                    var oldest = cache.entrySet().iterator().next();
                    cached -= oldest.getValue().length;
                    cache.remove(oldest.getKey());
                }
            }
            return png;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** Saves downloaded picture bytes as {@code wiki-<hash>.png}; null when they are not a readable image. */
    public synchronized String save(byte[] raw, String key) {
        byte[] png = toPng(raw);
        if (png == null) {
            return null;
        }
        String name = nameFor(key);
        try {
            Files.createDirectories(root);
            Files.write(root.resolve(name), png);
            return name;
        } catch (IOException e) {
            return null;
        }
    }

    /** The file name a picture taken from an address is saved under. */
    public static String nameFor(String key) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(key.getBytes(StandardCharsets.UTF_8));
            return "wiki-" + HexFormat.of().formatHex(hash, 0, 8) + ".png";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** PNG bytes of any readable image, scaled to fit {@link #MAX_SIDE}; null when the bytes are not an image. */
    static byte[] toPng(byte[] raw) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(raw));
            if (image == null) {
                return null;
            }
            int width = image.getWidth();
            int height = image.getHeight();
            double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(width, height));
            BufferedImage out = image;
            if (scale < 1.0 || image.getType() == BufferedImage.TYPE_CUSTOM) {
                int w = Math.max(1, (int) Math.round(width * scale));
                int h = Math.max(1, (int) Math.round(height * scale));
                out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = out.createGraphics();
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, scale < 1.0
                        ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                graphics.drawImage(image, 0, 0, w, h, null);
                graphics.dispose();
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            return ImageIO.write(out, "png", bytes) ? bytes.toByteArray() : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
