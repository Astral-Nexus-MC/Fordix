package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.FordixConfig;
import com.omneria.fordix.client.gui.layout.HudAnchors;
import com.omneria.fordix.client.gui.layout.HudBounds;
import com.omneria.fordix.client.gui.layout.HudSnap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Where the HUD draws each element: the renderers keep computing their default spot and add the
 * shift returned here, which comes from the element's anchor, offset and size for the current window.
 * An element drawn bigger or smaller is scaled around its top-left corner by {@link #begin}.
 */
public final class HudPlacement {
    private HudPlacement() {
    }

    private static HudSnap.Rect placed(String element) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        HudSnap.Rect base = HudBounds.defaults(element, width, height);
        int[] offset = FordixConfig.hudOffset(element);
        return HudAnchors.place(FordixConfig.hudAnchor(element), offset[0], offset[1],
                HudAnchors.scaled(base, FordixConfig.hudScale(element), FordixConfig.hudScaleY(element)), width, height);
    }

    /** {dx, dy} from the element's default spot to where the player put it. */
    public static int[] delta(String element) {
        Minecraft minecraft = Minecraft.getInstance();
        HudSnap.Rect base = HudBounds.defaults(element, minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight());
        HudSnap.Rect placed = placed(element);
        return new int[]{placed.x() - base.x(), placed.y() - base.y()};
    }

    public static boolean hidden(String element) {
        return FordixConfig.hudHidden(element);
    }

    /**
     * Sizes what is drawn next. With {@code drawsAtDefault} the renderer draws at the element's default spot (the
     * boss bars) and the shift is applied here; otherwise it already draws at the placed spot, shifted by
     * {@link #delta}. Always pair with {@link #end}.
     */
    public static boolean begin(GuiGraphics guiGraphics, String element, boolean drawsAtDefault) {
        float scale = FordixConfig.hudScale(element);
        float scaleY = FordixConfig.hudScaleY(element);
        if (!drawsAtDefault && scale == 1.0F && scaleY == 1.0F) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        HudSnap.Rect base = HudBounds.defaults(element, minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight());
        HudSnap.Rect placed = placed(element);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(placed.x(), placed.y(), 0.0F);
        guiGraphics.pose().scale(scale, scaleY, 1.0F);
        HudSnap.Rect origin = drawsAtDefault ? base : placed;
        guiGraphics.pose().translate(-origin.x(), -origin.y(), 0.0F);
        return true;
    }

    public static void end(GuiGraphics guiGraphics, boolean begun) {
        if (begun) {
            guiGraphics.pose().popPose();
        }
    }
}
