package com.omneria.fordix.client.gui.layout;

/**
 * Saved placement of an HUD element Fordix doesn't draw itself (chat, hotbar, a mod's overlay):
 * its anchor, offset and visibility, plus where and how big it sits by default. The default spot is kept
 * as fractions of the free space ({@code fx}, {@code fy}) so it still makes sense when the window or the GUI
 * scale changes. Pure Java (Gson-friendly), unit tested.
 */
public final class HudSlot {
    public String anchor = "default";
    public int offsetX;
    public int offsetY;
    public boolean hidden;
    public boolean measured;
    public int w;
    public int h;
    public float fx;
    public float fy;

    /** True when the player changed anything: otherwise the overlay is left alone. */
    public boolean customized() {
        return hidden || offsetX != 0 || offsetY != 0 || !"default".equals(anchor);
    }

    /** The default rectangle for a screen size, from the measured size and relative spot. */
    public HudSnap.Rect base(int screenWidth, int screenHeight) {
        int x = Math.round(fx * Math.max(0, screenWidth - w));
        int y = Math.round(fy * Math.max(0, screenHeight - h));
        return new HudSnap.Rect(x, y, w, h);
    }

    public void setBase(HudSnap.Rect rect, int screenWidth, int screenHeight) {
        w = rect.w();
        h = rect.h();
        int freeX = screenWidth - w;
        int freeY = screenHeight - h;
        fx = freeX > 0 ? Math.max(0F, Math.min(1F, rect.x() / (float) freeX)) : 0F;
        fy = freeY > 0 ? Math.max(0F, Math.min(1F, rect.y() / (float) freeY)) : 0F;
        measured = true;
    }

    public HudAnchors.Anchor anchorValue() {
        HudAnchors.Anchor value = HudAnchors.Anchor.byId(anchor);
        return value != null ? value : HudAnchors.Anchor.DEFAULT;
    }

    /** Back to the default spot, visible; the measured default itself is kept. */
    public void reset() {
        anchor = "default";
        offsetX = 0;
        offsetY = 0;
        hidden = false;
    }

    /** {dx, dy} from the default spot to where the element is placed on this screen. */
    public int[] delta(int screenWidth, int screenHeight) {
        return delta(base(screenWidth, screenHeight), screenWidth, screenHeight);
    }

    /** Same, from a default rectangle the caller supplies (a guess, while nothing was measured). */
    public int[] delta(HudSnap.Rect base, int screenWidth, int screenHeight) {
        HudSnap.Rect placed = HudAnchors.place(anchorValue(), offsetX, offsetY, base, screenWidth, screenHeight);
        return new int[]{placed.x() - base.x(), placed.y() - base.y()};
    }
}
