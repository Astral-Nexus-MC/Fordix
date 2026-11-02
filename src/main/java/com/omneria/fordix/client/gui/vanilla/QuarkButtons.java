package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.Fordix;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Quark puts its buttons (sort, deposit, restock...) on the container screens of its list of allowed screens: the game's own and a
 * few of other mods. The Voidframe container screens have the class names of Fordix, so without this they get none of them. Their
 * class names are added to that list, by reflection and without a dependency on Quark, before Quark builds the buttons of the screen
 * (it does so after the screen's own setup). Nothing happens when Quark is absent or its list is not where it is expected.
 */
public final class QuarkButtons {
    private static final String CONFIG = "org.violetmoon.quark.base.config.QuarkGeneralConfig";

    private static boolean resolved;
    private static Field allowed;
    private static Field blacklist;

    private QuarkButtons() {
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            allowed = Class.forName(CONFIG).getDeclaredField("allowedScreens");
            allowed.setAccessible(true);
            blacklist = Class.forName(CONFIG).getDeclaredField("useScreenListBlacklist");
            blacklist.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            allowed = null;
        }
    }

    /** Before the screen's setup: lets Quark's buttons come to a Fordix container screen. */
    @SuppressWarnings("unchecked")
    public static void allow(Screen screen) {
        if (!(screen instanceof VoidframeScreen) || !(screen instanceof AbstractContainerScreen<?>)) {
            return;
        }
        resolve();
        if (allowed == null) {
            return;
        }
        try {
            if (blacklist.getBoolean(null)) {
                return; // The list is a list of refused screens: ours are not on it, so they are allowed already.
            }
            Object value = allowed.get(null);
            if (!(value instanceof List<?> list) || list.contains(screen.getClass().getName())) {
                return;
            }
            List<String> copy = new ArrayList<>((List<String>) list);
            copy.add(screen.getClass().getName());
            allowed.set(null, copy);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Fordix.LOGGER.warn("Fordix: cannot let Quark's buttons on {}: {}", screen.getClass().getSimpleName(), e.toString());
            allowed = null;
        }
    }
}
