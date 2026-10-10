package com.omneria.fordix.client.gui;

import com.omneria.fordix.client.hud.HudStyle;
import com.omneria.fordix.client.hud.HudTokens;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

/**
 * The drawing primitives of Voidframe 2 for screens, the counterpart of {@link HudStyle} (HUD panels share the same look):
 * glass panels, recessed wells, buttons, text fields, pills and rules. Every Fordix screen and every redrawn vanilla
 * screen builds on these and nothing else, so a visual change is made here once. Colors come from {@link FordixPalette}
 * (the theme), geometry and motion from the Voidframe 2 specification (docs/VOIDFRAME2.md).
 */
public final class Glass {
    /** Time a hover takes to fade in or out, in milliseconds. */
    public static final long HOVER_MS = 120L;

    /** The three weights of a button: everyday, the one main action of a screen, and a destructive action. */
    public enum Kind {
        NORMAL, PRIMARY, DANGER
    }

    private Glass() {
    }

    /** A hover state that eases in and out over {@link #HOVER_MS}; one per widget, updated at every render. */
    public static final class Fade {
        private float value;
        private long last;

        public float update(boolean on) {
            long now = System.nanoTime();
            float elapsed = last == 0L ? 0.0F : (now - last) / 1_000_000.0F;
            last = now;
            float step = elapsed / HOVER_MS;
            value = on ? Math.min(1.0F, value + step) : Math.max(0.0F, value - step);
            return value;
        }
    }

    // ----- surfaces -----

    /** A container-sized glass panel with the theme's accent edge. */
    public static void panel(GuiGraphics g, int x, int y, int width, int height) {
        panel(g, x, y, width, height, FordixPalette.ACCENT);
    }

    public static void panel(GuiGraphics g, int x, int y, int width, int height, int accent) {
        HudStyle.panel(g, x, y, width, height, accent, 238, FordixPalette.PANEL_FILL);
    }

    /** A flat inner card (no accent edge): groups of rows inside a panel. */
    public static void card(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, 0x2E000000);
        g.fill(x, y, x + width, y + 1, 0x0CFFFFFF);
        g.fill(x, y + height - 1, x + width, y + height, 0x30000000);
    }

    /** The dim layer behind a full-screen Fordix screen: the world stays visible but quiet. */
    public static void backdrop(GuiGraphics g, int width, int height) {
        g.fillGradient(0, 0, width, height, 0xB0050A10, 0xD0020408);
    }

    /** A recessed rectangle (item slot, progress track, player preview): darker at the top, a light line under it. */
    public static void well(GuiGraphics g, int x, int y, int width, int height) {
        int base = FordixPalette.SLOT_FILL;
        g.fillGradient(x, y, x + width, y + height, HudTokens.mix(base, 0xFF000000, 0.45F), HudTokens.mix(base, 0xFFFFFFFF, 0.04F));
        g.fill(x, y, x + width, y + 1, 0xB0000000);
        g.fill(x, y + 1, x + 1, y + height, 0x60000000);
        g.fill(x, y + height - 1, x + width, y + height, 0x12FFFFFF);
    }

    // ----- controls -----

    /** {@code hover} is 0..1 from a {@link Fade}. */
    public static void button(GuiGraphics g, int x, int y, int width, int height, Kind kind, float hover, boolean active) {
        int accent = FordixPalette.ACCENT;
        int top;
        int bottom;
        int edge;
        switch (kind) {
            case PRIMARY -> {
                top = HudTokens.mix(accent, 0xFFFFFFFF, 0.22F + 0.12F * hover);
                bottom = HudTokens.mix(accent, 0xFF000000, 0.32F - 0.08F * hover);
                edge = HudTokens.withAlpha(HudTokens.mix(accent, 0xFFFFFFFF, 0.4F), 0xC0);
            }
            case DANGER -> {
                top = HudTokens.mix(FordixPalette.BUTTON_FILL_HOVERED, FordixPalette.ERROR, 0.12F + 0.1F * hover);
                bottom = HudTokens.mix(FordixPalette.BUTTON_FILL, FordixPalette.ERROR, 0.10F + 0.08F * hover);
                edge = HudTokens.withAlpha(FordixPalette.ERROR, 0x80 + (int) (0x50 * hover));
            }
            default -> {
                top = HudTokens.mix(FordixPalette.BUTTON_FILL_HOVERED, 0xFFFFFFFF, 0.05F + 0.07F * hover);
                bottom = HudTokens.mix(FordixPalette.BUTTON_FILL, 0xFF000000, 0.25F);
                edge = 0x1CFFFFFF;
            }
        }
        if (hover > 0.01F && active) {
            int glow = HudTokens.withAlpha(accent, Math.round(0x34 * hover));
            g.fill(x - 1, y - 1, x + width + 1, y, glow);
            g.fill(x - 1, y + height, x + width + 1, y + height + 1, glow);
            g.fill(x - 1, y, x, y + height, glow);
            g.fill(x + width, y, x + width + 1, y + height, glow);
        }
        g.fillGradient(x, y, x + width, y + height, top, bottom);
        g.fill(x, y, x + width, y + 1, edge);
        g.fill(x, y + height - 1, x + width, y + height, edge);
        g.fill(x, y + 1, x + 1, y + height - 1, edge);
        g.fill(x + width - 1, y + 1, x + width, y + height - 1, edge);
        g.fill(x + 1, y + 1, x + width - 1, y + 2, kind == Kind.PRIMARY ? 0x50FFFFFF : 0x1EFFFFFF);
        if (kind != Kind.PRIMARY && hover > 0.01F) {
            g.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, HudTokens.withAlpha(accent, Math.round(0xFF * hover)));
        }
        if (!active) {
            g.fill(x, y, x + width, y + height, 0x70070B10);
        }
    }

    public static int buttonText(Kind kind, boolean active) {
        if (!active) {
            return FordixPalette.TEXT & 0x00FFFFFF | 0x99000000;
        }
        return switch (kind) {
            case PRIMARY -> 0xFF04100F;
            case DANGER -> HudTokens.mix(FordixPalette.ERROR, 0xFFFFFFFF, 0.45F);
            default -> FordixPalette.TEXT_BRIGHT;
        };
    }

    /** A text field background; the caller draws the text. */
    public static void field(GuiGraphics g, int x, int y, int width, int height, boolean focused) {
        int accent = FordixPalette.ACCENT;
        if (focused) {
            g.fill(x - 1, y - 1, x + width + 1, y + height + 1, HudTokens.withAlpha(accent, 0x2C));
        }
        g.fill(x, y, x + width, y + height, 0x78000000);
        int edge = focused ? HudTokens.withAlpha(accent, 0xFF) : 0x20FFFFFF;
        g.fill(x, y, x + width, y + 1, edge);
        g.fill(x, y + height - 1, x + width, y + height, edge);
        g.fill(x, y + 1, x + 1, y + height - 1, edge);
        g.fill(x + width - 1, y + 1, x + width, y + height - 1, edge);
        g.fill(x + 1, y + 1, x + width - 1, y + 3, 0x40000000);
    }

    /** A list row: a faint wash on hover, an accent wash with an accent edge when selected. */
    public static void row(GuiGraphics g, int x, int y, int width, int height, boolean selected, boolean hovered) {
        if (selected) {
            g.fill(x, y, x + width, y + height, HudTokens.withAlpha(FordixPalette.ACCENT, 0x2A));
            g.fill(x, y, x + 2, y + height, 0xFF000000 | (FordixPalette.ACCENT & 0xFFFFFF));
            g.fill(x + 2, y + height - 1, x + width, y + height, HudTokens.withAlpha(FordixPalette.ACCENT, 0x40));
        } else if (hovered) {
            g.fill(x, y, x + width, y + height, 0x14FFFFFF);
        }
    }

    /** A thin vertical separator between two columns. */
    public static void divider(GuiGraphics g, int x, int top, int bottom) {
        g.fillGradient(x, top, x + 1, bottom, 0x00FFFFFF, 0x20FFFFFF);
    }

    /** An inline text button (for lists and pages): a glass button with its label centered, no widget needed. */
    public static void textButton(GuiGraphics g, Font font, String label, int x, int y, int width, boolean hovered) {
        button(g, x, y, width, 16, Kind.NORMAL, hovered ? 1.0F : 0.0F, true);
        g.drawCenteredString(font, label, x + width / 2, y + 4, FordixPalette.TEXT_BRIGHT);
    }

    // ----- text and small marks -----

    /** A screen or panel title: capitals, light accent, drawn at the given top-left. */
    public static void title(GuiGraphics g, Font font, String text, int x, int y) {
        g.drawString(font, text.toUpperCase(Locale.ROOT), x, y, HudTokens.mix(FordixPalette.ACCENT, 0xFFFFFFFF, 0.35F), false);
    }

    /** The dim tone of secondary text (captions, help lines). */
    public static int dim() {
        return HudTokens.mix(FordixPalette.TEXT, FordixPalette.PANEL_FILL, 0.35F);
    }

    /** A section label: capitals in a dim tone of the theme text, above a group. */
    public static void label(GuiGraphics g, Font font, String text, int x, int y) {
        g.drawString(font, text.toUpperCase(Locale.ROOT), x, y, dim(), false);
    }

    /** A thin accent rule that fades to the right, under a title. */
    public static void rule(GuiGraphics g, int x, int y, int width) {
        rule(g, x, y, width, FordixPalette.ACCENT);
    }

    public static void rule(GuiGraphics g, int x, int y, int width, int color) {
        int steps = 16;
        int step = Math.max(1, width / steps);
        for (int i = 0; i < steps; i++) {
            int alpha = Math.round(0x70 * (1.0F - i / (float) steps));
            int from = x + i * step;
            int to = i == steps - 1 ? x + width : from + step;
            g.fill(from, y, to, y + 1, HudTokens.withAlpha(color, alpha));
        }
    }

    /** A small rounded-looking tag with its text inside: states, counts, rarities. Returns its width. */
    public static int pill(GuiGraphics g, Font font, String text, int x, int y, int color) {
        int width = font.width(text) + 10;
        int height = 11;
        g.fill(x + 1, y, x + width - 1, y + height, 0x66000000);
        g.fill(x, y + 1, x + width, y + height - 1, 0x66000000);
        int edge = HudTokens.withAlpha(color, 0x70);
        g.fill(x + 1, y, x + width - 1, y + 1, edge);
        g.fill(x + 1, y + height - 1, x + width - 1, y + height, edge);
        g.fill(x, y + 1, x + 1, y + height - 1, edge);
        g.fill(x + width - 1, y + 1, x + width, y + height - 1, edge);
        g.drawString(font, text, x + 5, y + 2, color, false);
        return width;
    }

    /** A keyboard key cap, for hints like "G". Returns its width. */
    public static int keyCap(GuiGraphics g, Font font, String key, int x, int y) {
        int width = font.width(key) + 8;
        g.fill(x, y, x + width, y + 11, 0xFF0B1319);
        g.fill(x, y, x + width, y + 1, 0xFF2A3D49);
        g.fill(x, y + 1, x + 1, y + 10, 0xFF2A3D49);
        g.fill(x + width - 1, y + 1, x + width, y + 10, 0xFF2A3D49);
        g.fill(x, y + 10, x + width, y + 11, 0xFF3A5262);
        g.drawString(font, key, x + 4, y + 2, FordixPalette.TEXT_BRIGHT, false);
        return width;
    }
}
