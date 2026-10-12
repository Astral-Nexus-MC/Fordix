package com.omneria.fordix.debug;

import java.util.Locale;

/** A colour (0xRRGGBB) for an ore on the height scale: known ores keep their usual look, any other one gets a stable colour from its id. */
public final class OreColors {
    private static final String[][] KNOWN = {
            {"coal", "9A9A9A"}, {"copper", "C9A48A"}, {"iron", "D9C9B0"}, {"gold", "E8D44A"}, {"diamond", "4AD6E0"},
            {"redstone", "D94A4A"}, {"lapis", "3A5AD0"}, {"emerald", "3FD68A"}, {"zinc", "9FB3C8"}, {"quartz", "EDE6DC"},
            {"debris", "8A5A44"}, {"tin", "C8D4DC"}, {"lead", "6A6FA0"}, {"silver", "D8E0F0"}, {"nickel", "B8C8A0"},
            {"uranium", "7AD04A"}, {"osmium", "7AA0D0"}, {"aluminum", "E0E0E8"}, {"amethyst", "B07AE0"}
    };

    private OreColors() {
    }

    public static int of(String id) {
        String lower = id.toLowerCase(Locale.ROOT);
        for (String[] known : KNOWN) {
            if (lower.contains(known[0])) {
                return Integer.parseInt(known[1], 16);
            }
        }
        int hash = lower.hashCode();
        int r = 96 + ((hash >> 16) & 0x7F);
        int g = 96 + ((hash >> 8) & 0x7F);
        int b = 96 + (hash & 0x7F);
        return (r << 16) | (g << 8) | b;
    }
}
