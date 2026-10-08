package com.omneria.fordix.client.hud;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The look of a Voidframe 2 panel: a soft drop shadow, a dark glass body that fades downward, a one-pixel light on the top
 * edge, a dark line below, and an accent edge that glows and fades away. Only flat and gradient fills, so it draws anywhere.
 */
public final class HudStyle {
    private HudStyle() {
    }

    /** {@code bodyAlpha} 0..255 is the player's panel opacity; {@code accent} is an RGB color. */
    public static void panel(GuiGraphics g, int x, int y, int width, int height, int accent, int bodyAlpha) {
        int rgb = accent & 0x00FFFFFF;
        // Shadow: three translucent frames growing outward and downward.
        for (int i = 3; i >= 1; i--) {
            g.fill(x - i, y - i + 2, x + width + i, y + height + i + 2, HudTokens.withAlpha(0x000000, 14 + (3 - i) * 4));
        }
        int top = HudTokens.withAlpha(0x1C2834, Math.min(255, bodyAlpha + 12));
        int bottom = HudTokens.withAlpha(0x0A1016, Math.min(255, bodyAlpha + 24));
        g.fillGradient(x, y, x + width, y + height, top, bottom);
        // Light on the top edge, shade on the bottom edge.
        g.fill(x + 1, y, x + width - 1, y + 1, 0x24FFFFFF);
        g.fill(x, y + height - 1, x + width, y + height, 0x66000000);
        // Soft gloss over the upper third.
        g.fillGradient(x, y + 1, x + width, y + Math.max(2, height / 3), 0x10FFFFFF, 0x00FFFFFF);
        // Accent edge: solid at the top fading down, with a faint glow beside it.
        g.fillGradient(x, y, x + 2, y + height, 0xFF000000 | rgb, HudTokens.withAlpha(rgb, 0x30));
        g.fillGradient(x + 2, y, x + 5, y + height, HudTokens.withAlpha(rgb, 0x28), HudTokens.withAlpha(rgb, 0x00));
    }

    /** A bar track with a glossy fill: used by the small bars of the panels. */
    public static void bar(GuiGraphics g, int x, int y, int width, int height, float fraction, int color) {
        g.fill(x, y, x + width, y + height, 0x99000000);
        int filled = Math.round(width * Math.max(0.0F, Math.min(1.0F, fraction)));
        if (filled > 0) {
            g.fillGradient(x, y, x + filled, y + height, HudTokens.mix(color, 0xFFFFFFFF, 0.28F), color);
        }
        g.fill(x, y + height - 1, x + width, y + height, 0x40000000);
    }

    /** The red edge glow of the critical state, {@code strength} 0..1. */
    public static void vignette(GuiGraphics g, int width, int height, float strength) {
        if (strength <= 0.0F) {
            return;
        }
        int edge = HudTokens.withAlpha(0xE5534B, Math.round(150 * strength));
        int none = 0x00E5534B;
        int band = Math.max(24, Math.min(width, height) / 6);
        g.fillGradient(0, 0, width, band, edge, none);
        g.fillGradient(0, height - band, width, height, none, edge);
        // Side bands run horizontally, which fillGradient cannot do: stack thin columns.
        for (int i = 0; i < band; i++) {
            int a = Math.round(150 * strength * (1.0F - i / (float) band) * 0.6F);
            int c = HudTokens.withAlpha(0xE5534B, a);
            g.fill(i, band, i + 1, height - band, c);
            g.fill(width - 1 - i, band, width - i, height - band, c);
        }
    }
}
