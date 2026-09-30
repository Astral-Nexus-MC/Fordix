package com.omneria.fordix.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A full set of Fordix colors. Presets are picked by name in the client config and
 * applied to {@link FordixPalette}. No Minecraft dependency, so parsing and preset
 * lookup are unit tested on their own.
 */
public record Theme(int panelFill, int bevelHighlight, int bevelShadow,
                    int buttonFill, int buttonFillHovered,
                    int accent, int text, int textBright,
                    int slotFill, int progressTrack, int titleBand,
                    int flame, int error) {

    public static final String DEFAULT = "voidframe";

    private static final Map<String, Theme> PRESETS = new LinkedHashMap<>();

    static {
        PRESETS.put("voidframe", new Theme(0xFF131A22, 0xFF24313D, 0xFF05070A, 0xFF16202A, 0xFF1C2830,
                0xFF3FD1C7, 0xFF9FB3C0, 0xFFC9E9E5, 0xFF0B1016, 0xFF1E2A35, 0xFF1A2530, 0xFFE39B45, 0xFFE0524A));
        PRESETS.put("ember", new Theme(0xFF1E1614, 0xFF3A2A24, 0xFF0A0706, 0xFF241A17, 0xFF2E221E,
                0xFFE8743B, 0xFFC9B3A6, 0xFFF2DCCD, 0xFF120D0B, 0xFF33251F, 0xFF281D19, 0xFFE3C045, 0xFFE0524A));
        PRESETS.put("verdant", new Theme(0xFF121C16, 0xFF22352A, 0xFF050A07, 0xFF16241B, 0xFF1C2E23,
                0xFF5ED68A, 0xFFA3BFAE, 0xFFD1EDDC, 0xFF0A120D, 0xFF1D2E24, 0xFF18261E, 0xFFE39B45, 0xFFE0524A));
        PRESETS.put("amethyst", new Theme(0xFF18142A, 0xFF2C2548, 0xFF07050E, 0xFF1D1834, 0xFF251F42,
                0xFFB08CFF, 0xFFB4ABD0, 0xFFE2DAFF, 0xFF0E0B1A, 0xFF282142, 0xFF1F1A36, 0xFFE39B45, 0xFFE0524A));
    }

    /** Null-tolerant on purpose: Forge's config validation asks contains(null) for a key missing from the file. */
    public static final List<String> NAMES = Collections.unmodifiableList(new ArrayList<>(PRESETS.keySet()));

    /** The named preset, or the default one for an unknown name. */
    public static Theme byName(String name) {
        return PRESETS.getOrDefault(name, PRESETS.get(DEFAULT));
    }

    public Theme withAccent(int accent) {
        return new Theme(panelFill, bevelHighlight, bevelShadow, buttonFill, buttonFillHovered,
                accent, text, textBright, slotFill, progressTrack, titleBand, flame, error);
    }

    /** Parses {@code #RRGGBB} (the # is optional) to an opaque ARGB color; null when malformed. */
    public static Integer parseHex(String value) {
        if (value == null) {
            return null;
        }
        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (hex.length() != 6) {
            return null;
        }
        try {
            return 0xFF000000 | Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
