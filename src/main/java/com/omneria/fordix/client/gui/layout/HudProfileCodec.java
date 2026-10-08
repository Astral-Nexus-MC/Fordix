package com.omneria.fordix.client.gui.layout;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A HUD layout as one short line of text to paste in a chat or a message: {@code FXH1-} then the layout's JSON, compressed
 * and written in URL-safe base64. Pure Java, unit tested.
 */
public final class HudProfileCodec {
    public static final String PREFIX = "FXH1-";
    private static final int MAX_ELEMENTS = 200;
    private static final Gson GSON = new Gson();

    private HudProfileCodec() {
    }

    public static String encode(Map<String, HudProfiles.Placement> layout) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
                gzip.write(GSON.toJson(layout).getBytes(StandardCharsets.UTF_8));
            }
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The layout in a code, or {@code null} when the text is not a valid one (wrong prefix, damaged, absurd). */
    public static Map<String, HudProfiles.Placement> decode(String text) {
        if (text == null) {
            return null;
        }
        String code = text.strip();
        if (!code.startsWith(PREFIX) || code.length() > 60_000) {
            return null;
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(
                Base64.getUrlDecoder().decode(code.substring(PREFIX.length()))))) {
            String json = new String(gzip.readNBytes(1_000_000), StandardCharsets.UTF_8);
            Map<String, HudProfiles.Placement> layout = GSON.fromJson(json,
                    new TypeToken<LinkedHashMap<String, HudProfiles.Placement>>() { }.getType());
            if (layout == null || layout.size() > MAX_ELEMENTS) {
                return null;
            }
            for (HudProfiles.Placement placement : layout.values()) {
                if (placement == null || placement.anchor == null || !(placement.scale >= 0.2F && placement.scale <= 5.0F)
                        || Math.abs(placement.offsetX) > 10_000 || Math.abs(placement.offsetY) > 10_000) {
                    return null;
                }
            }
            return layout;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
