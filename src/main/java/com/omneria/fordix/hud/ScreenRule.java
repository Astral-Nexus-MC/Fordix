package com.omneria.fordix.hud;

import java.util.Collection;

/**
 * Which screens of other mods the server gives the Voidframe look: a list of class names or packages. An entry written
 * {@code light:<class or package>} only gives the screen the dim behind it and the glass buttons, not the panel over its background
 * (for a screen that is not laid out as a container). Pure Java, unit tested.
 */
public final class ScreenRule {
    public static final String LIGHT = "light:";

    public enum Mode {
        NONE, LIGHT, FULL
    }

    private ScreenRule() {
    }

    /** How a screen class is treated. The game's own screens and Fordix's are never matched (they have their own handling). */
    public static Mode modeOf(String className, Collection<String> entries) {
        if (className == null || entries.isEmpty() || className.startsWith("net.minecraft.") || className.startsWith("com.omneria.fordix.")) {
            return Mode.NONE;
        }
        Mode mode = Mode.NONE;
        for (String raw : entries) {
            String entry = raw == null ? "" : raw.strip();
            boolean light = entry.startsWith(LIGHT);
            if (light) {
                entry = entry.substring(LIGHT.length()).strip();
            }
            if (!entry.isEmpty() && (className.equals(entry) || className.startsWith(entry.endsWith(".") ? entry : entry + "."))) {
                if (!light) {
                    return Mode.FULL;
                }
                mode = Mode.LIGHT;
            }
        }
        return mode;
    }

    /** Whether the screen class is on the list in any way. */
    public static boolean matches(String className, Collection<String> entries) {
        return modeOf(className, entries) != Mode.NONE;
    }
}
