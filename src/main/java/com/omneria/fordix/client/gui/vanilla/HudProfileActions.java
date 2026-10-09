package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.layout.HudLayout;
import com.omneria.fordix.client.gui.layout.HudProfileCodec;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Whole-HUD layouts: {@link #capture}/{@link #applyLayout} take and give back the placement of every element (Voidframe's own
 * and the overlays), and the share code ({@link #exportCode}, {@link #importCode}) is how a layout travels to a server's config.
 * JourneyMap's minimap keeps its own settings.
 */
public final class HudProfileActions {
    private HudProfileActions() {
    }

    /** The placement of every element as it is now. */
    private static Map<String, HudLayout.Placement> snapshotAll() {
        Map<String, HudLayout.Placement> layout = new LinkedHashMap<>();
        for (String id : FordixConfig.HUD_ELEMENTS) {
            layout.put(id, HudSlots.snapshot(id));
        }
        for (String id : ExtraHud.knownIds()) {
            layout.put(id, HudSlots.snapshot(id));
        }
        return layout;
    }

    /** Puts a layout back; an element it doesn't know returns to its default spot. */
    private static void restoreAll(Map<String, HudLayout.Placement> layout) {
        for (String id : FordixConfig.HUD_ELEMENTS) {
            apply(id, layout);
        }
        for (String id : ExtraHud.knownIds()) {
            apply(id, layout);
        }
        FordixConfig.hudOffsetX(FordixConfig.HUD_ELEMENTS.get(0)).save();
        ExtraHud.save();
    }

    private static void apply(String id, Map<String, HudLayout.Placement> layout) {
        HudLayout.Placement placement = layout.get(id);
        if (placement != null) {
            HudSlots.restore(id, placement);
        } else {
            HudSlots.reset(id);
        }
    }

    /** The placement of every element as it is now, to give back later with {@link #applyLayout}. */
    public static Map<String, HudLayout.Placement> capture() {
        return snapshotAll();
    }

    public static void applyLayout(Map<String, HudLayout.Placement> layout) {
        restoreAll(layout);
    }

    /** The share code of what is on screen now. */
    public static String exportCode() {
        return HudProfileCodec.encode(snapshotAll());
    }

    /** Puts a pasted code on screen; false when the text is not a valid code. */
    public static boolean importCode(String text) {
        Map<String, HudLayout.Placement> layout = HudProfileCodec.decode(text);
        if (layout == null) {
            return false;
        }
        restoreAll(layout);
        return true;
    }
}
