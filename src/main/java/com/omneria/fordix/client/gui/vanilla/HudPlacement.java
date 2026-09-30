package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.layout.HudAnchors;
import com.omneria.fordix.client.gui.layout.HudBounds;
import com.omneria.fordix.client.gui.layout.HudSnap;
import net.minecraft.client.Minecraft;

/**
 * Where the HUD draws each element: the renderers keep computing their default spot and add the
 * shift returned here, which comes from the element's anchor and offset for the current window.
 */
public final class HudPlacement {
    private HudPlacement() {
    }

    /** {dx, dy} from the element's default spot to where the player put it. */
    public static int[] delta(String element) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        HudSnap.Rect base = HudBounds.defaults(element, width, height);
        int[] offset = FordixConfig.hudOffset(element);
        HudSnap.Rect placed = HudAnchors.place(FordixConfig.hudAnchor(element), offset[0], offset[1], base, width, height);
        return new int[]{placed.x() - base.x(), placed.y() - base.y()};
    }

    public static boolean hidden(String element) {
        return FordixConfig.hudHidden(element);
    }
}
