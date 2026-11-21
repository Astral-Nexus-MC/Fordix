package com.omneria.fordix.hud;

import java.util.Collection;

/** Which screens of other mods the server gives the Voidframe look: a list of class names or packages. Pure Java, unit tested. */
public final class ScreenRule {
    private ScreenRule() {
    }

    /** Whether a screen class is on the list. The game's own screens and Fordix's are never matched (they have their own handling). */
    public static boolean matches(String className, Collection<String> prefixes) {
        if (className == null || prefixes.isEmpty() || className.startsWith("net.minecraft.") || className.startsWith("com.omneria.fordix.")) {
            return false;
        }
        for (String prefix : prefixes) {
            String entry = prefix == null ? "" : prefix.strip();
            if (!entry.isEmpty() && (className.equals(entry) || className.startsWith(entry.endsWith(".") ? entry : entry + "."))) {
                return true;
            }
        }
        return false;
    }
}
