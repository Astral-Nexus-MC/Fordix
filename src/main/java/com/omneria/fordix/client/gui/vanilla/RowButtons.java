package com.omneria.fordix.client.gui.vanilla;

import com.omneria.fordix.client.gui.FordixPalette;
import com.omneria.fordix.client.gui.Glass;
import com.omneria.fordix.client.hud.HudTokens;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * Game buttons drawn as Voidframe 2 rows: an icon square (the first letter of the title), the title, under it what is currently
 * set (the "value" line), and a chevron. The menu pause and the options screen register their buttons here; the stand-in that
 * replaces each button asks {@link #isRow} and draws with {@link #draw}.
 */
public final class RowButtons {
    private record Spec(String icon, Supplier<String> detail, Supplier<String> badge) {
    }

    private static final Map<Button, Spec> ROWS = new WeakHashMap<>();

    private RowButtons() {
    }

    public static void clear() {
        ROWS.clear();
    }

    public static void register(Button button, String icon, Supplier<String> detail) {
        ROWS.put(button, new Spec(icon, detail, () -> ""));
    }

    public static void register(Button button, String icon, Supplier<String> detail, Supplier<String> badge) {
        ROWS.put(button, new Spec(icon, detail, badge));
    }

    public static boolean isRow(Button button) {
        return ROWS.containsKey(button);
    }

    /** A row: {@code hover} 0..1, drawn at (x, y) with the given size. */
    public static void draw(GuiGraphics g, Font font, Button button, String title, int x, int y, int width, int height, float hover, boolean active) {
        Spec spec = ROWS.get(button);
        Glass.button(g, x, y, width, height, Glass.Kind.NORMAL, hover, active);
        String icon = spec == null || spec.icon().isEmpty() ? (title.isEmpty() ? "?" : title.substring(0, 1).toUpperCase(java.util.Locale.ROOT)) : spec.icon();
        int box = Math.min(18, height - 6);
        int bx = x + 6;
        int by = y + (height - box) / 2;
        g.fill(bx, by, bx + box, by + box, HudTokens.withAlpha(FordixPalette.ACCENT, 0x24));
        int edge = HudTokens.withAlpha(FordixPalette.ACCENT, 0x55);
        g.fill(bx, by, bx + box, by + 1, edge);
        g.fill(bx, by + box - 1, bx + box, by + box, edge);
        g.fill(bx, by + 1, bx + 1, by + box - 1, edge);
        g.fill(bx + box - 1, by + 1, bx + box, by + box - 1, edge);
        g.drawString(font, icon, bx + (box - font.width(icon)) / 2, by + (box - 8) / 2 + 1, FordixPalette.ACCENT, false);
        int textX = bx + box + 7;
        String detail = spec == null ? "" : spec.detail().get();
        String badge = spec == null ? "" : spec.badge().get();
        int right = x + width - 12 - (badge.isEmpty() ? 0 : font.width(badge) + 14);
        int titleY = detail.isEmpty() ? y + (height - 8) / 2 : y + height / 2 - 9;
        g.drawString(font, font.plainSubstrByWidth(title, right - textX), textX, titleY, Glass.buttonText(Glass.Kind.NORMAL, active), false);
        if (!detail.isEmpty()) {
            g.drawString(font, font.plainSubstrByWidth(detail, right - textX), textX, y + height / 2 + 1, Glass.dim(), false);
        }
        if (!badge.isEmpty()) {
            Glass.pill(g, font, badge, x + width - 12 - font.width(badge) - 10, y + (height - 11) / 2, FordixPalette.ACCENT);
        } else {
            g.drawString(font, "›", x + width - 10, y + (height - 8) / 2, Glass.dim(), false);
        }
    }
}
