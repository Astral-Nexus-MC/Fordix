package com.omneria.fordix.wiki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageStoreTest {
    @TempDir
    Path dir;

    private static byte[] image(String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    @Test
    void bigPicturesAreScaledDownAndConvertedToPng() throws Exception {
        ImageStore store = new ImageStore(dir);
        Files.write(dir.resolve("map.jpg"), image("jpg", 1600, 800));
        byte[] png = store.png("map.jpg");
        assertNotNull(png);
        BufferedImage read = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(640, read.getWidth());
        assertEquals(320, read.getHeight());
    }

    @Test
    void savedPicturesAreNamedAfterTheirAddressAndFound() throws Exception {
        ImageStore store = new ImageStore(dir);
        String name = store.save(image("png", 40, 40), "https://example.org/a.png");
        assertEquals(ImageStore.nameFor("https://example.org/a.png"), name);
        assertTrue(store.exists(name));
        assertNotNull(store.png(name));
        assertNull(store.save("not an image".getBytes(), "https://example.org/b.png"));
    }

    @Test
    void namesThatCouldEscapeTheFolderAreRefused() throws Exception {
        ImageStore store = new ImageStore(dir);
        Files.write(dir.resolve("ok.png"), image("png", 8, 8));
        assertFalse(store.exists("../ok.png"));
        assertNull(store.png("../ok.png"));
        assertNull(store.png("missing.png"));
        assertNotNull(store.png("ok.png"));
    }
}
