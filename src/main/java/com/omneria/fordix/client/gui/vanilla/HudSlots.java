package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.Tr;
import com.omneria.fordix.client.gui.ModNames;
import com.omneria.fordix.client.gui.layout.HudAnchors;
import com.omneria.fordix.client.gui.layout.HudBounds;
import com.omneria.fordix.client.gui.layout.HudSlot;
import com.omneria.fordix.client.gui.layout.HudSnap;
import net.minecraft.client.resources.language.I18n;

import java.util.Locale;

/**
 * One way for the layout screen to read and change any movable HUD element, whatever stores it: the six Voidframe
 * elements (client config), the other Forge overlays ({@link ExtraHud}) and JourneyMap's minimap ({@link JourneyMapHud}).
 * Elements are named by id: "health"... for the first, "namespace:path" for overlays, "journeymap".
 */
public final class HudSlots {
    private HudSlots() {
    }

    public static boolean isBuiltin(String id) {
        return HudBounds.ELEMENTS.contains(id);
    }

    public static boolean isJourneyMap(String id) {
        return id.equals(JourneyMapHud.ID);
    }

    private static boolean isOverlay(String id) {
        return !isBuiltin(id) && !isJourneyMap(id);
    }

    public static String label(String id) {
        if (isBuiltin(id)) {
            return Tr.s("hud.element." + id);
        }
        if (isJourneyMap(id)) {
            return Tr.s("hud.element.journeymap");
        }
        int colon = id.indexOf(':');
        String namespace = id.substring(0, colon);
        String path = id.substring(colon + 1);
        String key = "fordix.hud.overlay." + path;
        if (namespace.equals("minecraft") && I18n.exists(key)) {
            return I18n.get(key);
        }
        String words = path.replace('_', ' ').replace('/', ' ');
        words = words.substring(0, 1).toUpperCase(Locale.ROOT) + words.substring(1);
        return namespace.equals("minecraft") ? words : words + " (" + ModNames.of(namespace) + ")";
    }

    public static HudSnap.Rect defaults(String id, int width, int height) {
        if (isBuiltin(id)) {
            return HudBounds.defaults(id, width, height);
        }
        return isJourneyMap(id) ? JourneyMapHud.rect(width, height) : ExtraHud.defaults(id, width, height);
    }

    public static HudAnchors.Anchor anchor(String id) {
        if (isBuiltin(id)) {
            return FordixConfig.hudAnchor(id);
        }
        return isJourneyMap(id) ? HudAnchors.Anchor.DEFAULT : ExtraHud.slot(id).anchorValue();
    }

    public static int[] offset(String id) {
        if (isBuiltin(id)) {
            return FordixConfig.hudOffset(id);
        }
        if (isJourneyMap(id)) {
            return new int[]{0, 0};
        }
        HudSlot slot = ExtraHud.slot(id);
        return new int[]{slot.offsetX, slot.offsetY};
    }

    public static boolean hidden(String id) {
        if (isBuiltin(id)) {
            return FordixConfig.hudHidden(id);
        }
        return isJourneyMap(id) ? JourneyMapHud.hidden() : ExtraHud.slot(id).hidden;
    }

    public static void setHidden(String id, boolean hidden) {
        if (isBuiltin(id)) {
            FordixConfig.hudHiddenValue(id).set(hidden);
        } else if (isJourneyMap(id)) {
            JourneyMapHud.setHidden(hidden);
        } else {
            ExtraHud.slot(id).hidden = hidden;
        }
    }

    /** Where the element is now (default spot moved by its anchor and offset). */
    public static HudSnap.Rect placed(String id, int width, int height) {
        if (isJourneyMap(id)) {
            return JourneyMapHud.rect(width, height);
        }
        int[] offset = offset(id);
        return HudAnchors.place(anchor(id), offset[0], offset[1], defaults(id, width, height), width, height);
    }

    private static void setPlacement(String id, HudAnchors.Anchor anchor, int offsetX, int offsetY) {
        if (isBuiltin(id)) {
            FordixConfig.hudAnchorValue(id).set(anchor.id());
            FordixConfig.hudOffsetX(id).set(offsetX);
            FordixConfig.hudOffsetY(id).set(offsetY);
        } else if (isOverlay(id)) {
            HudSlot slot = ExtraHud.slot(id);
            slot.anchor = anchor.id();
            slot.offsetX = offsetX;
            slot.offsetY = offsetY;
        }
    }

    /** Puts the element's rectangle at (x, y), kept on screen, keeping its anchor. */
    public static void moveTo(String id, int x, int y, int width, int height) {
        HudSnap.Rect base = defaults(id, width, height);
        HudSnap.Rect target = HudBounds.place(new HudSnap.Rect(x, y, base.w(), base.h()), 0, 0, width, height);
        if (isJourneyMap(id)) {
            JourneyMapHud.moveTo(target, width, height);
            return;
        }
        int[] offset = HudAnchors.offsetFor(anchor(id), target, base, width, height);
        setPlacement(id, anchor(id), offset[0], offset[1]);
    }

    /** Pins the element to the screen point nearest its spot, so it keeps its place on any window size. */
    public static void pinToNearestAnchor(String id, int width, int height) {
        if (isJourneyMap(id)) {
            return;
        }
        HudSnap.Rect rect = placed(id, width, height);
        HudAnchors.Anchor anchor = HudAnchors.nearest(rect, width, height);
        int[] offset = HudAnchors.offsetFor(anchor, rect, defaults(id, width, height), width, height);
        setPlacement(id, anchor, offset[0], offset[1]);
    }

    public static void apply(String id, HudAnchors.Entry entry) {
        setPlacement(id, entry.anchor(), entry.offsetX(), entry.offsetY());
        setHidden(id, entry.hidden());
    }

    /** Back to the default spot, visible. */
    public static void reset(String id) {
        if (isJourneyMap(id)) {
            JourneyMapHud.reset();
            JourneyMapHud.setHidden(false);
        } else {
            apply(id, HudAnchors.Entry.standard());
        }
    }

    public static void save(String id) {
        if (isBuiltin(id)) {
            FordixConfig.hudOffsetX(id).save();
        } else if (isJourneyMap(id)) {
            JourneyMapHud.save();
        } else {
            ExtraHud.save();
        }
    }
}
